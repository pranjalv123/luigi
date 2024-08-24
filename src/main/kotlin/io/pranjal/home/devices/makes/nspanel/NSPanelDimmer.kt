package io.pranjal.home.devices.makes.nspanel

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.home.devices.ZigbeeDevice
import io.pranjal.home.devices.Switch
import io.pranjal.home.devices.makes.HueDimmer
import io.pranjal.mqtt.MqttClient

class NSPanelDimmer(definition: HueDimmer.Definition, client: MqttClient) : Switch {

    data class Definition(
        override val id: String,
        override val name: String,
        override val location: ZigbeeDevice.Definition.Location
    ) : ZigbeeDevice.Definition

    private val logger = KotlinLogging.logger("NSPanel ${definition.name} [${definition.id}]")

}