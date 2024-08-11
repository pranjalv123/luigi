package io.pranjal.home.devices

import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.pranjal.home.lights.Brightness
import io.pranjal.home.lights.ColorTemperature
import kotlin.time.Duration

private val lights = mutableListOf<Light>()

class LightRegistration private constructor() {
    companion object {
        fun register(light: Light): LightRegistration {
            lights.add(light)
            return LightRegistration()
        }
    }
}


interface Light {
    val name: String
    val registration: LightRegistration
    suspend fun turnOn(transition: Duration = Duration.ZERO)
    suspend fun turnOff(transition: Duration = Duration.ZERO)
    suspend fun toggle(transition: Duration = Duration.ZERO)
    suspend fun setBrightness(brightness: Brightness, transition: Duration = Duration.ZERO)
    suspend fun moveBrightness(speed: Double)
    suspend fun stepBrightness(step: Int)
    suspend fun setColorTemperature(temperature: ColorTemperature, transition: Duration = Duration.ZERO)

    suspend fun setColorTemperatureAndBrightness(
        temperature: ColorTemperature,
        brightness: Brightness,
        transition: Duration = Duration.ZERO
    ) {
        setColorTemperature(temperature, transition)
        setBrightness(brightness, transition)
    }
}
