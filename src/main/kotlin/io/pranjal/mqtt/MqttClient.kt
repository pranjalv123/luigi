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

val logger = KotlinLogging.logger {}

class MqttClient {
    enum class State {
        CONNECTED, CONNECTING, DISCONNECTED
    }

    val client: IMqttAsyncClient = MqttAsyncClient("tcp://localhost:1883", "luigi")
    var state = State.DISCONNECTED

    val subscriptions = mutableMapOf<Topic, SharedFlow<String>>()

    suspend fun connect() {
        try {
            if (state != State.DISCONNECTED) {
                logger.debug { "Already connected" }
                return
            }
            state = State.CONNECTING
            return suspendCoroutine { cont ->
                logger.debug { "Connecting" }
                client.connect(null, object : MqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        logger.info { "MQTT Client Connected" }
                        state = State.CONNECTED
                        cont.resume(Unit)
                    }

                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        state = State.DISCONNECTED
                        logger.error { "MQTT failed to connect" }
                        cont.resumeWithException(exception ?: RuntimeException("Failed to connect"))
                    }
                })
            }
        } catch (e: Throwable) {
            state = State.DISCONNECTED
            logger.error(e) { "Failed to connect to MQTT" }
        }
    }


    suspend fun disconnect() =
        suspendCoroutine { cont ->
            client.disconnect(null, object : MqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    state = State.DISCONNECTED
                    logger.info { "Disconnected" }
                    cont.resume(Unit)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    cont.resumeWithException(exception ?: RuntimeException("Failed to disconnect"))
                }
            })
        }

    suspend fun subscribe(topic: Topic, scope: CoroutineScope): SharedFlow<String> =
        suspendCoroutine { cont ->
            try {
                if (subscriptions.containsKey(topic)) {
                    logger.info { "Already subscribed to $topic" }
                    cont.resume(subscriptions[topic]!!)
                    return@suspendCoroutine
                }
                logger.info { "Subscribing to $topic" }
                val flow = MutableSharedFlow<String>()
                subscriptions[topic] = flow
                client.subscribe(
                    arrayOf(MqttSubscription(topic)), null, object : MqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            cont.resume(flow)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            cont.resumeWithException(exception ?: RuntimeException("Failed to subscribe"))
                        }
                    },
                    arrayOf(IMqttMessageListener { topic, message ->
                        scope.launch {
                            flow.emit(message.payload.toString(Charset.defaultCharset()))
                        }
                    }), MqttProperties()
                )
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }

    suspend fun publish(topic: String, message: String, qos: Int = 0, retained: Boolean = false) =
        suspendCoroutine<Unit> { cont ->
            try {
                logger.info { "Publishing to $topic: $message" }
                client.publish(
                    topic,
                    MqttMessage(message.toByteArray(), qos, retained, MqttProperties()),
                    null,
                    object : MqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            cont.resume(Unit)
                        }

                        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                            cont.resumeWithException(exception ?: RuntimeException("Failed to publish"))
                        }
                    })
            } catch (e: Throwable) {
                cont.resumeWithException(e)
            }
        }
}