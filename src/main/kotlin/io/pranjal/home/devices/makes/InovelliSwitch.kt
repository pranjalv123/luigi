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
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class InovelliSwitch(definition: Definition, client: MqttClient) : Device(definition, client), Switch {
    /** https://www.zigbee2mqtt.io/devices/VZM31-SN.html#inovelli-vzm31-sn
     *
     * Actions: down_single, up_single, config_single, down_release, up_release, config_release, down_held, up_held,
     * config_held, down_double, up_double, config_double, down_triple, up_triple, config_triple, down_quadruple,
     * up_quadruple, config_quadruple, down_quintuple, up_quintuple, config_quintuple.
     *
     *
     *
     * */

    data class Definition(
        override val id: String,
        override val name: String,
        override val location: Device.Definition.Location
    ) : Device.Definition

    private val logger = KotlinLogging.logger("Inovelli ${definition.name} [${definition.id}]")

    suspend fun setSmartbulbMode() {
        setValue("smartBulbMode", "Smart Bulb Mode")
    }

    suspend fun setButtonDelay(duration: Duration) {
        setValue("buttonDelay", duration.inWholeMilliseconds.toString())
    }

    override suspend fun initialize() {
        setSmartbulbMode()
        setButtonDelay(0.milliseconds)
    }

    override suspend fun turnOn(scope: CoroutineScope): Flow<Unit> =
        onAction("up_single", scope).onEach { logger.info { "Got on press" } }.map { Unit }

    override suspend fun turnOff(scope: CoroutineScope): Flow<Unit> =
        onAction("down_held", scope).onEach { logger.info { "Got off press" } }.map { Unit }

    override suspend fun increaseBrightness(scope: CoroutineScope): Flow<Unit> =
        onAction("up_held", scope).onEach { logger.info { "Got increase brightness press" } }.map { Unit }

    override suspend fun decreaseBrightness(scope: CoroutineScope): Flow<Unit> =
        onAction("down_single", scope).onEach { logger.info { "Got decrease brightness press" } }.map { Unit }

    override suspend fun reset(scope: CoroutineScope): Flow<Unit> = onAction("config_single", scope).onEach {
        logger.info { "Got reset press" }
    }.map { Unit }
}
