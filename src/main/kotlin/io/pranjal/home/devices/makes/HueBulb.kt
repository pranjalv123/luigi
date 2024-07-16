package io.pranjal.home.devices.makes

import io.pranjal.home.devices.Device
import io.pranjal.home.devices.Light
import io.pranjal.mqtt.MqttClient

/** https://www.zigbee2mqtt.io/devices/579573.html */
class HueBulb(definition: Definition, client: MqttClient) : Device(definition, client), Light {

    data class Definition(
        override val id: String,
        override val name: String,
        override val location: Device.Definition.Location
    ) : Device.Definition


    override suspend fun turnOn(transitionSec: Int) = setValues("state" to "ON", "transition" to transitionSec.toString())
    override suspend fun turnOff(transitionSec: Int) = setValues("state" to "OFF", "transition" to transitionSec.toString())
    override suspend fun toggle(transitionSec: Int) =
        setValues("state" to "TOGGLE", "transition" to transitionSec.toString())

    /** Brightness is a value between 0 and 1 */
    override suspend fun setBrightness(brightness: Double, transitionSec: Int) =
        setValues("brightness" to (brightness * 254).toInt().toString(), "transition" to transitionSec.toString())

    override suspend fun moveBrightness(speed: Double) =
        setValues("brightness_move" to (speed * 254).toInt().toString())

    override suspend fun stepBrightness(step: Int) =
        setValues("brightness_step" to step.toString())


    override suspend fun setColorTemperature(temperature: Int, transitionSec: Int) {
        val temperatureReciprocalMegakelvin = 1.0 / (temperature / 1000000.0)
        setValues(
            "color_temp" to temperatureReciprocalMegakelvin.toInt().toString(),
            "transition" to transitionSec.toString()
        )
    }
}