package io.pranjal.home.devices.makes

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.home.devices.Device
import io.pranjal.home.devices.Switch
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration

@OptIn(FlowPreview::class)
class InovelliSwitch(definition: Definition, client: MqttClient) : Device(definition, client), Switch {
    /** https://www.zigbee2mqtt.io/devices/929002398602.html#philips-929002398602
     *
     * Actions: on_press, on_hold, on_press_release, on_hold_release, off_press, off_hold,
     * off_press_release, off_hold_release, up_press, up_hold, up_press_release, up_hold_release,
     * down_press, down_hold, down_press_release, down_hold_release, recall_0, recall_1.
     *
     * */

    data class Definition(
        override val id: String,
        override val name: String,
        override val location: Device.Definition.Location
    ) : Device.Definition

    private val logger = KotlinLogging.logger("Inovelli ${definition.name} [${definition.id}]")

    fun setSmartbulbMode() {
        runBlocking { setValue("smartBulbMode", "Smart Bulb Mode") }
    }
    fun setButtonDelay(duration: Duration) {
        runBlocking { setValue("buttonDelay", duration.inWholeMilliseconds.toString()) }
    }

    override suspend fun turnOn(scope: CoroutineScope): Flow<Unit> =
        onAction("up_single", scope).onEach { logger.info { "Got on press" } }.map { Unit }

    override suspend fun turnOff(scope: CoroutineScope): Flow<Unit> =
        onAction("down_held", scope).onEach { logger.info { "Got off press" } }.map { Unit }

    override suspend fun increaseBrightness(scope: CoroutineScope): Flow<Unit> =
        onAction("up_held", scope).onEach { logger.info { "Got increase brightness press" } }.map { Unit }

    override suspend fun decreaseBrightness(scope: CoroutineScope): Flow<Unit> =
        onAction("down_single", scope).onEach { logger.info { "Got decrease brightness press" } }.map { Unit }
    override suspend fun toggle(scope: CoroutineScope): Flow<Unit> =
        onAction("up_down", scope).onEach { logger.info { "Got toggle press" } }.map { Unit }

    override suspend fun reset(scope: CoroutineScope): Flow<Unit> {
        return super.reset(scope)
    }
}
