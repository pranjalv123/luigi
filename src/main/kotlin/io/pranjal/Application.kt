package io.pranjal

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.plugins.*
import io.ktor.server.application.*
import io.ktor.server.sse.*
import io.pranjal.home.*
import io.pranjal.home.devices.Devices
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.runBlocking
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    startUpdateWeatherThread()
    thread {
        try {
            val mqttClient = MqttClient()
            val devices = Devices(mqttClient)
            runBlocking {
                mqttClient.connect()
                logger.info { "Connected to MQTT; initializing lights" }
                LightsGroup(
                    this,
                    name = "Kitchen",
                    lights = listOf(),
                    switches = devices.hueDimmers,
                    colorTemp = ::standardTempSchedule,
                    brightness = ::standardBrightnessSchedule
                LightsGroup(
                    this,
                    name = "MasterBedroom",
                    lights = devices.masterBedroomLights,
                    switches = listOf(devices.masterBedroomDimmer) + devices.hueDimmers,
                    colorTemp = ::standardTempSchedule,
                    brightness = ::standardBrightnessSchedule
                )
                LightsGroup(
                    this,
                    name = "Sunroom",
                    lights = devices.sunroomLights,
                    switches = listOf(devices.sunroomDimmer),
                    colorTemp = ::standardTempSchedule,
                    brightness = ::standardBrightnessSchedule
                )
                LightsGroup(
                    this,
                    name = "MasterBathroom",
                    lights = devices.masterBathroomLights + devices.walkInClosetLights,
                    switches = listOf(devices.walkInClosetDimmer),
                    colorTemp = ::standardTempSchedule,
                    brightness = ::standardBrightnessSchedule
                )
                logger.info { "Initialized lights" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Device management thread failure" }
            exitProcess(1)
        }
    }

    io.ktor.server.netty.EngineMain.main(args)

}

fun Application.module() {
    install(SSE)
    configureRouting()
}

