package io.pranjal.home.devices.makes

import io.pranjal.home.devices.*
import io.pranjal.home.lights.Brightness
import io.pranjal.home.lights.ColorTemperature
import io.pranjal.mqtt.MqttClient
import kotlin.time.Duration
import kotlin.time.DurationUnit

/** https://www.zigbee2mqtt.io/devices/579573.html */
class HueBulb(definition: Definition, client: MqttClient) : Device(definition, client), Light {
    override val name = definition.name
    override val registration = LightRegistration.register(this)

    data class Definition(
        override val id: String,
        override val name: String,
        override val location: Device.Definition.Location
    ) : Device.Definition


    override suspend fun turnOn(transition: Duration) = setValues("state" to "ON", "transition" to transition.toString(DurationUnit.SECONDS, 3))
    override suspend fun turnOff(transition: Duration) = setValues("state" to "OFF", "transition" to transition.toString(DurationUnit.SECONDS, 3))
    override suspend fun toggle(transition: Duration) =
        setValues("state" to "TOGGLE", "transition" to transition.toString(DurationUnit.SECONDS, 3))

    /** Brightness is a value between 0 and 1 */
    override suspend fun setBrightness(brightness: Brightness, transition: Duration) =
        setValues("brightness" to (brightness.toRange(0, 254)).toString(), "transition" to transition.toString(DurationUnit.SECONDS, 3))

    override suspend fun moveBrightness(speed: Double) =
        setValues("brightness_move" to (speed * 254).toInt().toString())

    override suspend fun stepBrightness(step: Int) =
        setValues("brightness_step" to step.toString())


    override suspend fun setColorTemperature(temperature: ColorTemperature, transition: Duration) {

        setValues(
            "color_temp" to temperature.temperatureReciprocalMegakelvin.toInt().toString(),
            "transition" to transition.toString(DurationUnit.SECONDS, 3)
        )
    }

    override suspend fun setColorTemperatureAndBrightness(
        temperature: ColorTemperature,
        brightness: Brightness,
        transition: Duration
    ) {
        setValues(
            "color_temp" to temperature.temperatureReciprocalMegakelvin.toInt().toString(),
            "brightness" to (brightness.toRange(0, 254)).toString(),
            "transition" to transition.toString(DurationUnit.SECONDS, 3)
        )
    }
}