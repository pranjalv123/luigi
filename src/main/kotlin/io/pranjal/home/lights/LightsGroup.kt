package io.pranjal.home.lights

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pranjal.home.Schedule
import io.pranjal.home.devices.Light
import io.pranjal.home.devices.Switch
import io.pranjal.home.devices.inputFlow
import io.pranjal.home.homeassistant.HADevice
import io.pranjal.home.homeassistant.HaConfig
import io.pranjal.home.homeassistant.LightsState
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.math.log
import kotlin.math.round
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

private val lightsGroups = mutableListOf<LightsGroup>()
private val logger = KotlinLogging.logger {}

class LightsGroup(
    val scope: CoroutineScope,
    val name: String,
    val lights: List<Light>,
    val switches: List<Switch>,
    val brightnessSchedule: Schedule<Brightness>,
    val colorTemperatureSchedule: Schedule<ColorTemperature>,
    val clock: Clock,
    val mqttClient: MqttClient
) : HADevice<LightsState> {
    val baseState = BaseState(brightnessSchedule, colorTemperatureSchedule, clock = clock)

    private lateinit var _state: State

    private val renderer = Renderer(lights, this, mqttClient)
    var initialized = false

    val haTopic = "luigi/lightgroup/$name"
    val internalStateTopic = "luigi/lightgroup/$name/internalstate"

    private suspend fun initializeInternalState(scope: CoroutineScope) {
        val sub = mqttClient.subscribeState(internalStateTopic, scope)
        delay(10)
        val internalState = sub.value

        logger.info { "Read internal state for $name: $internalState" }
        if (internalState != null) {
            try {
                _state = Json.decodeFromString(State.serializer(), internalState)
                logger.info { "Decoded state to $_state" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to decode state from $internalState" }
                _state = State.Off(baseState)
            }
        } else {
            _state = State.Off(baseState)
        }
        mqttClient.unsubscribe(internalStateTopic, scope)
    }

    var state: State
        get() = _state
        private set(value) {
            _state = value
            runBlocking {
                mqttClient.publish(internalStateTopic, Json.encodeToString(State.serializer(), value), retained = true)
            }
        }


    override val haConfig = HaConfig<LightsState>(
        topic = haTopic,
        componentType = "light",
        componentName = name,
        stateSerializer = LightsState.serializer()
    )

    override fun currentHaState() = LightsState(
        brightness = (254 * state.brightness.value).toInt(),
        colorTemperature = state.colorTemperature.temperatureReciprocalMegakelvin.toInt(),
        state = if (state.brightness == Brightness.OFF) LightsState.StateEnum.OFF else LightsState.StateEnum.ON,
        colorMode = "color_temp"
    )

    override suspend fun handleStateUpdate(state: LightsState) {
        logger.info { "Got state update from HA: $state" }
        val brightness = state.brightness?.let { Brightness(it.toDouble() / 254) }
        val colorTemperature =
            state.colorTemperature?.let { ColorTemperature(((1.0 / it.toDouble()) * 1000000).toInt()) }
        val newState = when (state.state) {
            LightsState.StateEnum.OFF -> State.Off(baseState)
            LightsState.StateEnum.ON, null -> {
                val defaultBrightness = brightnessSchedule.now()
                when {
                    brightness == null -> State.Default(baseState)
                    brightness < defaultBrightness * 0.95 -> State.Dimmed(brightness, baseState)
                    brightness > defaultBrightness * 1.05 -> State.Brightened(brightness, baseState)
                    else -> State.Default(baseState)
                }
            }
        }
        this.state = newState
        renderer.render(newState, transition = 500.milliseconds)
    }

    init {
        scope.launch {
            initializeInternalState(this)

            scope.launch {
                logger.info { "Setting up input flow for lights group $name" }
                initialized = true
                switches.map { it.inputFlow(this) }.merge().collect { input ->
                    logger.info { "Got input $input" }
                    state = state.transition(input)
                    renderer.render(state, transition = 500.milliseconds)
                }
            }
            scope.launch {
                while (true) {
                    logger.trace { "Rendering lights group $name at ${clock.now()}: $state" }
                    renderer.render(state, transition = 0.milliseconds)
                    delay(1.minutes)
                }
            }
            scope.launch {
                homeAssistantListener(mqttClient, scope)
            }
        }
        logger.info { "Adding lights group $name" }
        lightsGroups.add(this)
        logger.info { "Lights group $name added" }
        logger.info { lightsGroups.map { it.name } }

    }
}

fun Route.lightsGroups() {
    route("lights") {
        get {
            logger.info { lightsGroups.map { it.name } }
            call.respond(lightsGroups.map { it.name })
        }

        route("/{name}") {
            get {
                val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.NotFound)
                val group = lightsGroups.find { it.name == name } ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(group.state)
            }
            route("/brightness") {
                get {
                    val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.NotFound)
                    val group =
                        lightsGroups.find { it.name == name } ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(group.state.brightness)
                }
            }
            route("/colorTemperature") {
                get {
                    val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.NotFound)
                    val group =
                        lightsGroups.find { it.name == name } ?: return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(group.state.colorTemperature)
                }
            }
        }
    }

}