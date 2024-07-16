package io.pranjal.plugins


import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.pranjal.home.getWeather
import kotlinx.coroutines.runBlocking

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        sse("/mqtt_stream") {
             runBlocking {
                val client = io.pranjal.mqtt.MqttClient()
                client.connect()
                val flow = client.subscribe("#", this)
                flow.collect {
                    println("Got message: ${it}")
                    send(it)
                }

            }
        }

        get("/weather") {
            call.respondText(getWeather().toString())
        }
    }
}
