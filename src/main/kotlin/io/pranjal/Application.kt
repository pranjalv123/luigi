package io.pranjal

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.pranjal.plugins.*
import io.ktor.server.application.*
import io.ktor.server.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.pranjal.home.*
import io.pranjal.home.devices.Devices
import io.pranjal.home.devices.makes.VirtualDimmer
import io.pranjal.home.devices.makes.VirtualLight
import io.pranjal.home.lights.*
import io.pranjal.mqtt.MockMqttClient
import io.pranjal.mqtt.MqttClient
import io.pranjal.mqtt.PahoMqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlin.concurrent.thread
import kotlin.system.exitProcess


private val logger = KotlinLogging.logger {}

enum class Environment {
    PRODUCTION,
    DEVELOPMENT
}


val environment = when (System.getenv("LUIGI_ENVIRONMENT") ?: "dev") {
    "production" -> Environment.PRODUCTION
    "development" -> Environment.DEVELOPMENT
    else -> Environment.DEVELOPMENT
}


fun CoroutineScope.prodLights(devices: Devices, clock: Clock, mqttClient: MqttClient) = listOf(
    LightsGroup(
        this,
        name = "Kitchen",
        lights = devices.kitchenLights,
        switches = listOf(),
        brightnessSchedule = downstairsBrightnessSchedule(clock),
        colorTemperatureSchedule = standardTempSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "LivingRoom",
        lights = devices.livingRoomLights,
        colorTemperatureSchedule = standardTempSchedule(clock),
        brightnessSchedule = downstairsBrightnessSchedule(clock),
        switches = listOf(),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "DiningRoom",
        lights = devices.diningRoomLights,
        colorTemperatureSchedule = standardTempSchedule(clock),
        brightnessSchedule = downstairsBrightnessSchedule(clock),
        switches = listOf(),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "MasterBedroom",
        lights = devices.masterBedroomLights,
        switches = listOf(devices.masterBedroomDimmer) + devices.hueDimmers,
        colorTemperatureSchedule = standardTempSchedule(clock),
        brightnessSchedule = bedroomBrightnessSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "Sunroom",
        lights = devices.sunroomLights,
        switches = listOf(devices.sunroomDimmer),
        colorTemperatureSchedule = standardTempSchedule(clock),
        brightnessSchedule = bedroomBrightnessSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "MasterBathroom",
        lights = devices.masterBathroomLights + devices.walkInClosetLights,
        switches = listOf(devices.walkInClosetDimmer),
        colorTemperatureSchedule = standardTempSchedule(clock),
        brightnessSchedule = bedroomBrightnessSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "SmallBedroom",
        lights = devices.smallBedroomLights,
        switches = listOf(devices.smallBedroomDimmer),
        colorTemperatureSchedule = standardTempSchedule(clock),
        brightnessSchedule = bedroomBrightnessSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "Office",
        lights = devices.officeLights,
        switches = listOf(devices.officeDimmer),
        colorTemperatureSchedule = standardTempSchedule(clock),
        brightnessSchedule = bedroomBrightnessSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    )
)

fun CoroutineScope.devLights(devices: Devices, clock: Clock, mqttClient: MqttClient) = listOf(
    LightsGroup(
        this,
        name = "Group1",
        lights = listOf(VirtualLight("light1"), VirtualLight("light2")),
        switches = listOf(VirtualDimmer("dimmer1")),
        brightnessSchedule = downstairsBrightnessSchedule(clock),
        colorTemperatureSchedule = standardTempSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    ),
    LightsGroup(
        this,
        name = "Group2",
        lights = listOf(VirtualLight("light3"), VirtualLight("light4")),
        switches = listOf(VirtualDimmer("dimmer2")),
        brightnessSchedule = bathroomBrightnessSchedule(clock),
        colorTemperatureSchedule = standardTempSchedule(clock),
        clock = clock,
        mqttClient = mqttClient
    )
)

fun launchLightsThread(clock: Clock) = thread {
    try {
        val mqttClient: MqttClient = when (environment) {
            Environment.PRODUCTION -> PahoMqttClient("tcp://localhost:1883", "luigi")
            Environment.DEVELOPMENT -> MockMqttClient(log = true)
        }
        runBlocking {
            mqttClient.connect()
            logger.info { "Connected to MQTT; initializing lights" }
            val devices = Devices(mqttClient)
            when (environment) {
                Environment.PRODUCTION -> prodLights(devices, clock, mqttClient)
                Environment.DEVELOPMENT -> devLights(devices, clock, mqttClient)
            }
            logger.info { "Initialized lights" }
        }
    } catch (e: Exception) {
        logger.error(e) { "Device management thread failure" }
        exitProcess(1)
    }
}

fun main(args: Array<String>) {
    startUpdateWeatherThread()
    io.ktor.server.netty.EngineMain.main(args)

}

fun Application.module() {
    install(SSE)
    install(ContentNegotiation) {
        json(
            json = kotlinx.serialization.json.Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
//            serializersModule = Interpolatable.serializersModule
            }
        )
    }
    launchLightsThread(Clock.System)
    configureRouting()
}

