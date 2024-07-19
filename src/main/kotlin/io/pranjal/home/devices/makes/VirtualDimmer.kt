package io.pranjal.home.devices.makes

import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import io.pranjal.home.devices.Switch
import io.pranjal.home.lights.Brightness
import io.pranjal.home.lights.ColorTemperature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.html.body
import kotlinx.html.div

private val virtualDimmers = mutableMapOf<String, VirtualDimmer>()

class VirtualDimmer(name: String) : Switch {


    val onFlow = MutableSharedFlow<Unit>()
    val offFlow = MutableSharedFlow<Unit>()
    val toggleFlow = MutableSharedFlow<Unit>()
    val increaseBrightnessFlow = MutableSharedFlow<Unit>()
    val decreaseBrightnessFlow = MutableSharedFlow<Unit>()
    val resetFlow = MutableSharedFlow<Unit>()
    val customFlow = MutableSharedFlow<Pair<ColorTemperature, Brightness>>()
    init {
        virtualDimmers[name.lowercase()] = this
    }
    override suspend fun turnOn(scope: CoroutineScope): Flow<Unit> = onFlow

    override suspend fun turnOff(scope: CoroutineScope): Flow<Unit> = offFlow

    override suspend fun toggle(scope: CoroutineScope): Flow<Unit> = toggleFlow

    override suspend fun increaseBrightness(scope: CoroutineScope): Flow<Unit> = increaseBrightnessFlow

    override suspend fun decreaseBrightness(scope: CoroutineScope): Flow<Unit> = decreaseBrightnessFlow

    override suspend fun reset(scope: CoroutineScope): Flow<Unit> = resetFlow

    override suspend fun custom(scope: CoroutineScope): Flow<Pair<ColorTemperature, Brightness>> = customFlow

}

fun Route.virtualDimmers() {
    route("switch/") {
        get {
            call.respond(virtualDimmers.keys)
        }
        route("{name}") {
            get {
                call.respondText("Switch")
            }
            post("on") {
                val switch = virtualDimmers[call.parameters["name"]?.lowercase()] ?: return@post call.respondHtml(
                    HttpStatusCode.NotFound
                ) {}
                switch.onFlow.emit(Unit)
                call.respondHtml(HttpStatusCode.OK) {
                    body { text("Turned on!") }
                }
            }
            post("off") {
                val switch =
                    virtualDimmers[call.parameters["name"]] ?: return@post call.respondHtml(HttpStatusCode.NotFound) {}
                switch.offFlow.emit(Unit)
                call.respondHtml(HttpStatusCode.OK) {}
            }
            post("toggle") {
                val switch =
                    virtualDimmers[call.parameters["name"]] ?: return@post call.respondHtml(HttpStatusCode.NotFound) {}
                switch.toggleFlow.emit(Unit)
                call.respondHtml(HttpStatusCode.OK) {}
            }
            post("increaseBrightness") {
                val switch =
                    virtualDimmers[call.parameters["name"]] ?: return@post call.respondHtml(HttpStatusCode.NotFound) {}
                switch.increaseBrightnessFlow.emit(Unit)
                call.respondHtml(HttpStatusCode.OK) {}
            }
            post("decreaseBrightness") {
                val switch =
                    virtualDimmers[call.parameters["name"]] ?: return@post call.respondHtml(HttpStatusCode.NotFound) {}
                switch.decreaseBrightnessFlow.emit(Unit)
                call.respondHtml(HttpStatusCode.OK) {}
            }
            post("reset") {
                val switch =
                    virtualDimmers[call.parameters["name"]] ?: return@post call.respondHtml(HttpStatusCode.NotFound) {}
                switch.resetFlow.emit(Unit)
                call.respondHtml(HttpStatusCode.OK) {}
            }
            post("custom") {
                val switch =
                    virtualDimmers[call.parameters["name"]] ?: return@post call.respondHtml(HttpStatusCode.NotFound) {}
                val colorTemperature = ColorTemperature(call.parameters["colorTemperature"]?.toInt()!!)
                val brightness = Brightness(call.parameters["brightness"]?.toDouble()!!)
                switch.customFlow.emit(colorTemperature to brightness)
                call.respondHtml(HttpStatusCode.OK) {}
            }
        }
    }
}
