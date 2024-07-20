package io.pranjal.mqtt

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.IMqttMessageListener
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.MqttSubscription
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.log

typealias Topic = String

private val logger = KotlinLogging.logger {}

interface MqttClient {
    suspend fun connect()

    suspend fun disconnect()

    suspend fun subscribe(topic: Topic, scope: CoroutineScope): SharedFlow<String>
    suspend fun subscribeState(topic: Topic, scope: CoroutineScope): StateFlow<String?> {
        return MutableStateFlow<String?>(null).apply {
            scope.launch {
                subscribe(topic, scope).collect {
                    value = it
                }
            }
        }
    }

    suspend fun publish(topic: String, message: String, qos: Int = 0, retained: Boolean = false)
}
