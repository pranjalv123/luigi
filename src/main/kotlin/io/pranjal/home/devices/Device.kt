package io.pranjal.home.devices

import io.github.oshai.kotlinlogging.KotlinLogging
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

private val logger = KotlinLogging.logger {}

open class Device(val definition: Definition, val client: MqttClient) {

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
            OUTSIDE
        }
    }

    val topicSet: String = "zigbee2mqtt/${definition.id}/set"
    val topicLuigi: String = "luigi/${definition.id}"

    suspend fun setValue(key: String, value: String) = setValues(mapOf(key to value))

    suspend fun setValues(vararg values: Pair<String, String>) = setValues(values.toMap())

    suspend fun saveState(value: String) {
        client.publish(topicLuigi, value, retained = true)
    }
    suspend fun getState(scope: CoroutineScope): String {
        return client.subscribe(topicLuigi, scope).first()
    }

    suspend fun setValues(values: Map<String, String>) {
        client.publish(topicSet, values.map { (key, value) -> "\"$key\": \"$value\"" }.joinToString(",", "{", "}"))
    }

    suspend fun onAction(action: String, scope: CoroutineScope) =
        client.subscribe("zigbee2mqtt/${definition.id}/action", scope).filter { it == action }
            .onEach { logger.info { "Got action $action: $it" } }

}
