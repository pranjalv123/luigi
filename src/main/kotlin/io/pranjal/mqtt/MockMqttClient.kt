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

    override suspend fun subscribe(topic: Topic, scope: CoroutineScope): SharedFlow<String> {
        if (log) {
            logger.info { "Subscribed to $topic" }
        }
        return topicFlows.getOrPut(topic) {
            MutableSharedFlow()
        }
    }

    override suspend fun publish(topic: String, message: String, qos: Int, retained: Boolean) {
        if (log) {
            logger.info { "Published message $message to $topic" }
        }
        topicFlows[topic]?.emit(message)
        if(retained) {
            throw NotImplementedError("Retained messages not supported in MockMqttClient")
        }
    }
}