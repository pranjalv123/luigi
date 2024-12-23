package io.pranjal.home.devices

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val logger = KotlinLogging.logger {}

open class ZigbeeDevice(val definition: Definition, val client: MqttClient) {

    interface Definition {
        val id: String
        val name: String
        val location: Location

        enum class Location {
            LIVING_ROOM,
            BEDROOM,
            KITCHEN,
            SUNROOM,
            MASTER_BEDROOM,
            MASTER_BATHROOM,
            BATHROOM,
            OUTSIDE,
            DINING_ROOM,
            SMALL_BEDROOM,
            OFFICE
        }
    }

    val topicSet: String = "zigbee2mqtt/${definition.name}/set"
    val topicAction: String = "zigbee2mqtt/${definition.name}/action"

    open suspend fun initialize() {
    }

    init {
        runBlocking {
            rename()
            initialize()
        }
    }

    suspend fun rename() {
        logger.info { "Renaming device ${definition.id} to ${definition.name}" }
        client.publish(
            "zigbee2mqtt/bridge/request/device/rename",
            buildJsonObject {
                put("from", definition.id)
                put("to", definition.name)
                put("homeassistant_rename", true)
            }.toString()
    )
}

suspend fun setValue(key: String, value: String) = setValues(mapOf(key to value))

suspend fun setValues(vararg values: Pair<String, String>) = setValues(values.toMap())


suspend fun setValues(values: Map<String, String>) {
    client.publish(topicSet, values.map { (key, value) -> "\"$key\": \"$value\"" }.joinToString(",", "{", "}"))
}

suspend fun onAction(action: String, scope: CoroutineScope) =
    client.subscribe(topicAction, scope).filter { it == action }
        .onEach { logger.info { "Got action $action: $it" } }

}
