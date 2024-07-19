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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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
    val clock: Clock
) {

    var state: State = State.Off(BaseState(brightnessSchedule, colorTemperatureSchedule, clock = clock))
        private set
    private val renderer = Renderer(lights)
    var initialized = false
    init {
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
                logger.trace {"Rendering lights group $name at ${clock.now()}: $state"}
                renderer.render(state, transition = 0.milliseconds)
                delay(1.minutes)
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