package io.pranjal.mqtt

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

private val logger = KotlinLogging.logger {}

class MockMqttClient(val log : Boolean = true) : MqttClient
{
    override suspend fun connect() {
        if (log) {
            logger.info { "Connected" }
        }
    }

    override suspend fun disconnect() {
        if (log) {
            logger.info { "Disconnected" }
        }
    }

    val topicFlows = mutableMapOf<Topic, MutableSharedFlow<String>>()
    val retained = mutableMapOf<Topic, String>()

    val publishedMessages = mutableMapOf<String, MutableList<String>>()

    override suspend fun subscribe(topic: Topic, scope: CoroutineScope): SharedFlow<String> {
        if (log) {
            logger.info { "Subscribed to $topic" }
        }
        return topicFlows.getOrPut(topic) {
            MutableSharedFlow()
        }
    }
    override suspend fun unsubscribe(topic: Topic, scope: CoroutineScope) {
        if (log) {
            logger.info { "Unsubscribed from $topic" }
        }
        topicFlows.remove(topic)
    }
    override suspend fun publish(topic: String, message: String, qos: Int, retained: Boolean) {
        if (log) {
            logger.info { "Published message $message to $topic" }
        }
        topicFlows[topic]?.emit(message)
        if(retained) {
            logger.warn { "Retained messages are not supported in MockMqttClient" }
        }
        publishedMessages.getOrPut(topic, { mutableListOf() }).add(message)
    }

    fun clear() = publishedMessages.clear()
    fun allMessagesForTopic(topic: String) = publishedMessages.get(topic) ?: emptyList()
}