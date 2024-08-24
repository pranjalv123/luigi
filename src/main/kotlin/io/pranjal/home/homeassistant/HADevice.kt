package io.pranjal.home.homeassistant

import io.pranjal.home.lights.LightsGroup
import io.pranjal.mqtt.MqttClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

data class HaConfig<S> (
    val topic: String,
    val componentType: String,
    val componentName: String,
    val stateSerializer: KSerializer<S>,
)

interface HADevice<S> {
    val haConfig: HaConfig<S>
    fun currentHaState(): S
    suspend fun handleStateUpdate(state: S)

    suspend fun homeAssistantListener(mqttClient: MqttClient, scope: CoroutineScope) {
        sendDiscoveryMessage(mqttClient)
        val topic = "${haConfig.topic}/set"
        mqttClient.subscribe(topic, scope).filterNotNull().collect {
            val state = Json.decodeFromString(haConfig.stateSerializer, it)
            handleStateUpdate(state)
        }
    }
    suspend fun publishState(mqttClient: MqttClient) {
        println(currentHaState())
        println(Json.encodeToString(haConfig.stateSerializer, currentHaState()))
        mqttClient.publish("${haConfig.topic}/state", Json.encodeToString(haConfig.stateSerializer, currentHaState()), retained = true)
    }
    suspend fun sendDiscoveryMessage(mqttClient: MqttClient) {
        val discoveryPrefix = "homeassistant"
        val topic = "$discoveryPrefix/${haConfig.componentType}/luigi/${haConfig.componentName}/config"
        val discoveryMessage = buildJsonObject {
            put("schema", "json")
            put("command_topic", "${haConfig.topic}/set")
            put("state_topic", "${haConfig.topic}/state")
            put("name", haConfig.componentName)
            put("brightness", true)
            putJsonArray("supported_color_modes") {
                add("color_temp")
                add("hs")
            }
            put("unique_id", "luigi_${haConfig.componentType}_${haConfig.componentName}")
        }
        mqttClient.publish(topic, discoveryMessage.toString(), retained = true)
    }
}