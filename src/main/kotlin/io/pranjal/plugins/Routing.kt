package io.pranjal.plugins


import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.pranjal.home.devices.makes.VirtualDimmer
import io.pranjal.home.devices.makes.virtualDimmers
import io.pranjal.home.getWeather
import io.pranjal.home.lights.lightsGroups
import io.pranjal.mqttClient
import kotlinx.coroutines.runBlocking

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        sse("/mqtt_stream") {
             runBlocking {
                val flow = mqttClient.subscribe("#", this)
                flow.collect {
                    println("Got message: ${it}")
                    send(it)
                }

            }
        }

        get("/weather") {
            call.respondText(getWeather().toString())
        }

        virtualDimmers()
        lightsGroups()
    }
}
