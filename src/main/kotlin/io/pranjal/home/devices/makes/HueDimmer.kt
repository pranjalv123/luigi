package io.pranjal.home.devices.makes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.home.devices.Device
import io.pranjal.home.devices.Switch
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

/** https://www.zigbee2mqtt.io/devices/929002398602.html#philips-929002398602
 *
 * Actions: on_press, on_hold, on_press_release, on_hold_release, off_press, off_hold,
 * off_press_release, off_hold_release, up_press, up_hold, up_press_release, up_hold_release,
 * down_press, down_hold, down_press_release, down_hold_release, recall_0, recall_1.
 *
 * */
@OptIn(FlowPreview::class)
class HueDimmer(definition: Definition, client: MqttClient) : Device(definition, client), Switch {

    data class Definition(
        override val id: String,
        override val name: String,
        override val location: Device.Definition.Location
    ) : Device.Definition

    private val logger = KotlinLogging.logger("HueDimmer ${definition.name} [${definition.id}]")

    override suspend fun turnOn(scope: CoroutineScope): Flow<Unit> =
        onAction("on_press", scope).onEach { logger.info { "Got on press" } }.map { Unit }

    override suspend fun turnOff(scope: CoroutineScope): Flow<Unit> =
        onAction("off_press", scope).onEach { logger.info { "Got off press" } }.map { Unit }

    override suspend fun increaseBrightness(scope: CoroutineScope): Flow<Unit> =
        onAction("up_press", scope).onEach { logger.info { "Got increase brightness press" } }.map { Unit }

    override suspend fun decreaseBrightness(scope: CoroutineScope): Flow<Unit> =
        onAction("down_press", scope).onEach { logger.info { "Got decrease brightness press" } }.map { Unit }
}