package io.pranjal.home.devices.makes

import io.pranjal.home.devices.Light
import io.pranjal.home.devices.LightRegistration
import io.pranjal.home.lights.Brightness
import io.pranjal.home.lights.ColorTemperature
import kotlin.time.Duration

class VirtualLight(override val name: String) : Light {
    var state: String = "off"
    var brightness: Brightness = Brightness.OFF
    var colorTemperature: ColorTemperature = ColorTemperature(0)
    override val registration = LightRegistration.register(this)
    override suspend fun turnOn(transition: Duration) {
        state = "on"
    }

    override suspend fun turnOff(transition: Duration) {
        state = "off"
    }

    override suspend fun toggle(transition: Duration) {
        if (state == "on") {
            turnOff(transition)
        } else {
            turnOn(transition)
        }
    }

    override suspend fun setBrightness(brightness: Brightness, transition: Duration) {
        this.brightness = brightness
    }

    override suspend fun moveBrightness(speed: Double) {

    }

    override suspend fun stepBrightness(step: Int) {
    }

    override suspend fun setColorTemperature(temperature: ColorTemperature, transition: Duration) {
        this.colorTemperature = temperature
    }
}
