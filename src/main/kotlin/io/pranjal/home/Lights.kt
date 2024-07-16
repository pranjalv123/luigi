package io.pranjal.home

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.home.devices.makes.HueBulb
import io.pranjal.home.devices.Switch
import io.pranjal.home.devices.makes.InovelliSwitch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.*
import kotlin.math.min
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes


class LightsGroup(
    scope: CoroutineScope,
    val name: String,
    val lights: List<HueBulb>,
    val switches: List<Switch>,
    val colorTemp: (Weather) -> List<Pair<Instant, Double>>,
    val brightness: (Weather) -> List<Pair<Instant, Double>>
) {
    val logger = KotlinLogging.logger("LightsGroup [$name]")
    val colorTempsSchedule = InterpolatingSchedule()
    val brightnessSchedule = InterpolatingSchedule()

    init {
        logger.info { "Setting up lights group $name" }
        runBlocking {
            switches.filter { it is InovelliSwitch }.forEach {
                (it as InovelliSwitch).setSmartbulbMode()
            }
            Weather.flow.first().let(colorTemp)
                .also { logger.info { "Setting color temp schedule on startup: $it" } }
                .forEach { (time, value) -> colorTempsSchedule.addPoint(time, value) }
            Weather.flow.first().let(brightness)
                .also { logger.info { "Setting brightness schedule on startup: $it" } }
                .forEach { (time, value) -> brightnessSchedule.addPoint(time, value) }
        }
        scope.launch {
            runDaily(LocalTime(0, 1), scope).map { Clock.System.todayIn(TZ) }.collectLatest {
                Weather.flow.first().let(colorTemp)
                    .also { logger.info { "Setting color temp schedule: $it" } }
                    .forEach { (time, value) -> colorTempsSchedule.addPoint(time, value) }
                Weather.flow.first().let(brightness)
                    .also { logger.info { "Setting brightness schedule on startup: $it" } }
                    .forEach { (time, value) -> brightnessSchedule.addPoint(time, value) }
            }
        }
    }

    fun modifyColorTemp(temp: Int) =
        when (state) {
            State.OFF -> temp
            is State.DIM -> 2300
            State.NORMAL -> temp
            is State.BRIGHT -> 4000
        }

    fun modifyBrightness(brightness: Double) =
        state.let {
            when (it) {
                State.OFF -> 0.0
                is State.DIM -> brightness - (it.level / 4.0) * brightness
                State.NORMAL -> brightness
                is State.BRIGHT -> 1 - (((3 - it.level) / 4.0) * (1 - brightness))
            }
        }

    sealed class State {
        data object OFF : State()
        data class DIM(val level: Int) : State()
        data object NORMAL : State()
        data class BRIGHT(val level: Int) : State()
    }

    var state: State = State.NORMAL


    data class Transition<T>(
        val startState: (State) -> Boolean,
        val resultState: (State) -> State,
        val action: suspend (T) -> Unit
    )

    val transitions = mutableMapOf<Flow<*>, MutableList<Transition<*>>>()

    private fun <T> Flow<T>.addTransition(
        startState: (State) -> Boolean,
        resultState: (State) -> State,
        action: suspend (T) -> Unit
    ): Flow<T> {
        transitions.getOrPut(this, { emptyList<Transition<*>>().toMutableList() })
            .add(
                Transition<T>(
                    startState,
                    resultState,
                    action
                )
            )
        return this
    }

    private fun <T> Flow<T>.handleTransitions(scope: CoroutineScope) {
        val transitions = transitions[this]?.map { it as Transition<T> } ?: return
        scope.launch {
            collectLatest { value ->
                val matchingTransitions = transitions.filter { it.startState(state) }
                when (matchingTransitions.size) {
                    0 -> return@collectLatest
                    1 -> {
                        logger.info { "Transitioning from $state to ${matchingTransitions.first().resultState(state)}" }
                        state = matchingTransitions.first().resultState(state)
                        matchingTransitions.first().action(value)
                    }

                    else -> logger.error { "Multiple transitions match for state $state" }
                }
            }
        }
    }


    init {
        scope.launch {
            colorTempsSchedule.runEvery(1.minutes)
                .collectLatest { setColorTemperature(modifyColorTemp(it.toInt()), scope) }
        }
        scope.launch {
            brightnessSchedule.runEvery(1.minutes).collectLatest { setBrightness(modifyBrightness(it), scope) }
        }

        switches.forEach { switch ->
            scope.launch {
                logger.info { "Setting up switch ${switch}" }
                switch.turnOff(scope).addTransition({ it != State.OFF }, { State.OFF }) {
                    logger.info { "Turning off lights" }
                    turnOff(scope)
                }.handleTransitions(scope)

                switch.turnOn(scope).addTransition({ it == State.OFF }, { State.NORMAL }) {
                    logger.info { "Turning on lights" }
                    turnOn(scope)
                }.handleTransitions(scope)

                switch.increaseBrightness(scope).addTransition({ it is State.DIM }, { State.NORMAL }) {
                    setBrightness(modifyBrightness(brightnessSchedule.now()), scope)
                }.addTransition({ it == State.NORMAL }, { State.BRIGHT(1) }) {
                    setBrightness(modifyBrightness(brightnessSchedule.now()), scope)
                }.addTransition(
                    { it is State.BRIGHT },
                    { State.BRIGHT(min(3, (it as State.BRIGHT).level + 1)) },
                ) {
                    setBrightness(modifyBrightness(brightnessSchedule.now()), scope)
                }.handleTransitions(scope)

                switch.decreaseBrightness(scope).addTransition({ it is State.BRIGHT }, { State.NORMAL }) {
                    setBrightness(modifyBrightness(brightnessSchedule.now()), scope)
                }.addTransition({ it == State.NORMAL }, { State.DIM(1) }) {
                    setBrightness(modifyBrightness(brightnessSchedule.now()), scope)
                }.addTransition({ it is State.DIM }, { State.DIM(min(3, (it as State.DIM).level + 1)) }) {
                    setBrightness(modifyBrightness(brightnessSchedule.now()), scope)
                }.handleTransitions(scope)
                logger.info { "Switch ${switch} setup complete" }

            }
        }
    }

    fun turnOn(scope: CoroutineScope) {
        lights.forEach { scope.launch {
            it.setColorTemperature(modifyColorTemp(colorTempsSchedule.now().toInt()))
            it.setBrightness(modifyBrightness(brightnessSchedule.now()))
            it.turnOn()
        } }
    }

    fun turnOff(scope: CoroutineScope) {
        lights.forEach { scope.launch { it.turnOff() } }
    }

    fun setColorTemperature(temp: Int, scope: CoroutineScope) {
        lights.forEach { scope.launch { it.setColorTemperature(temp) } }
    }

    fun setBrightness(brightness: Double, scope: CoroutineScope) {
        lights.forEach { scope.launch { it.setBrightness(brightness) } }
    }
}

fun standardTempSchedule(weather: Weather): List<Pair<Instant, Double>> {
    val today = Clock.System.todayIn(TZ)
    return listOf(
        today.atTime(LocalTime(0, 0)).toInstant(TZ) to 2300.0,
        weather.sunrise - 2.hours to 2300.0,
        weather.sunrise to 2900.0,
        weather.sunrise + 2.hours to 3500.0,
        (weather.sunrise + (weather.sunset - weather.sunrise) / 2) to 4800.0,
        weather.sunset - 2.hours to 3000.0,
        weather.sunset to 2700.0,
        weather.sunset + 2.hours to 2300.0,
        today.atTime(LocalTime(23, 59)).toInstant(TZ) to 2300.0,
    )
}

fun standardBrightnessSchedule(weather: Weather): List<Pair<Instant, Double>> {
    val today = Clock.System.todayIn(TZ)
    return listOf(
        today.atTime(LocalTime(0, 0)).toInstant(TZ) to 0.1,
        weather.sunrise - 2.hours to 0.1,
        weather.sunrise to 0.1,
        weather.sunrise + 3.hours to 0.2,
        (weather.sunrise + (weather.sunset - weather.sunrise) / 2) to 1.0,
        weather.sunset - 2.hours to 1.0,
        weather.sunset to 0.7,
        weather.sunset + 2.hours to 0.3,
        today.atTime(LocalTime(23, 59)).toInstant(TZ) to 0.1,
    )
}

fun bathroomBrightnessSchedule(weather: Weather): List<Pair<Instant, Double>> {
    val today = Clock.System.todayIn(TZ)
    return listOf(
        today.atTime(LocalTime(0, 0)).toInstant(TZ) to 0.03,
        weather.sunrise - 2.hours to 0.03,
        weather.sunrise to 0.03,
        weather.sunrise + 3.hours to 0.2,
        (weather.sunrise + (weather.sunset - weather.sunrise) / 2) to 1.0,
        weather.sunset - 2.hours to 1.0,
        weather.sunset to 0.7,
        weather.sunset + 2.hours to 0.3,
        today.atTime(LocalTime(23, 59)).toInstant(TZ) to 0.03,
    )
}
