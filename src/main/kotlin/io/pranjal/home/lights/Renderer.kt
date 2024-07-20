package io.pranjal.home.lights

import io.pranjal.home.devices.Light
import io.pranjal.home.homeassistant.HADevice
import io.pranjal.home.homeassistant.LightsState
import io.pranjal.mqtt.MqttClient
import kotlin.time.Duration

class Renderer(
    val lights: List<Light>,
    val haDevice: HADevice<LightsState>,
    val mqttClient: MqttClient
) {
    suspend fun render(state: State, transition: Duration) {
        val brightness = state.brightness
        val colorTemperature = state.colorTemperature
        lights.forEach {
            it.setColorTemperature(colorTemperature, transition)
            it.setBrightness(brightness, transition)
        }
        haDevice.publishState(mqttClient)
    }
}