package io.pranjal.home.lights

import io.pranjal.home.devices.Light
import kotlin.time.Duration

class Renderer(
    val lights: List<Light>,
) {
    suspend fun render(state: State, transition: Duration) {
        val brightness = state.brightness
        val colorTemperature = state.colorTemperature
        lights.forEach {
            it.setColorTemperature(colorTemperature, transition)
            it.setBrightness(brightness, transition)
        }
    }
}