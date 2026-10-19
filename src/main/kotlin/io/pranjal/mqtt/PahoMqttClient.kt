package io.pranjal.mqtt

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient
import org.eclipse.paho.mqttv5.client.IMqttMessageListener
import org.eclipse.paho.mqttv5.client.IMqttToken
import org.eclipse.paho.mqttv5.client.MqttActionListener
import org.eclipse.paho.mqttv5.client.MqttAsyncClient
import org.eclipse.paho.mqttv5.client.MqttCallback
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse
import org.eclipse.paho.mqttv5.common.MqttException
import org.eclipse.paho.mqttv5.common.MqttMessage
import org.eclipse.paho.mqttv5.common.MqttSubscription
import org.eclipse.paho.mqttv5.common.packet.MqttProperties
import java.nio.charset.Charset
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class PahoMqttClient(serverUri: String, clientId: String) : MqttClient {
    enum class State {
        CONNECTED, CONNECTING, DISCONNECTED
    }

    val client: IMqttAsyncClient = MqttAsyncClient(serverUri, clientId)
    var state = State.DISCONNECTED

    // Set by disconnect(), so a deliberate disconnect isn't treated as a lost connection.
    @Volatile
    private var closing = false

    val subscriptions = mutableMapOf<Topic, SharedFlow<String?>>()

    init {
        // A dropped connection used to go unnoticed: no automatic reconnect,
        // every publish failed with "Client is not connected", and the process
        // stayed up, so Nomad saw a healthy task while the lights were dead
        // (mosquitto or its Connect sidecar restarting was enough). Exit
        // instead; Nomad restarts the task, which reconnects, resubscribes and
        // re-initializes every device. Paho's automaticReconnect isn't enough
        // on its own: with a clean session the subscriptions are gone after a
        // reconnect.
        client.setCallback(object : MqttCallback {
            override fun disconnected(disconnectResponse: MqttDisconnectResponse?) {
                state = State.DISCONNECTED
                if (closing) return
                logger.error { "MQTT connection lost (${disconnectResponse?.reasonString ?: disconnectResponse?.exception?.message}); exiting so Nomad restarts us" }
                exitProcess(1)
            }

            override fun mqttErrorOccurred(exception: MqttException?) {
                logger.error(exception) { "MQTT error" }
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {}
            override fun deliveryComplete(token: IMqttToken?) {}
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {}
            override fun authPacketArrived(reasonCode: Int, properties: MqttProperties?) {}
        })
    }

    override suspend fun connect() {
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
            // Rethrow: swallowing this left the client disconnected for good,
            // with every later subscribe/publish failing. launchLightsThread
            // retries on MqttException.
            throw e
        }
        while (state == State.CONNECTING) {
            logger.info { "Waiting for MQTT connection" }
            delay(50)
        }
    }


    override suspend fun disconnect() =
        suspendCoroutine { cont ->
            closing = true
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

    override suspend fun subscribe(topic: Topic, scope: CoroutineScope): SharedFlow<String?> = subscribe(topic, scope) { MutableSharedFlow() }
    override suspend fun subscribeState(topic: Topic, scope: CoroutineScope): StateFlow<String?> = subscribe(topic, scope) {
        MutableStateFlow(
            null
        )
    } as StateFlow<String?>

    suspend fun subscribe(topic: Topic, scope: CoroutineScope, getFlow: () -> MutableSharedFlow<String?>): SharedFlow<String?> =
        suspendCoroutine { cont ->
            try {
                if (subscriptions.containsKey(topic)) {
                    logger.info { "Already subscribed to $topic" }
                    cont.resume(subscriptions[topic]!!)
                    return@suspendCoroutine
                }
                logger.info { "Subscribing to $topic" }
                val flow = getFlow()
                subscriptions[topic] = flow
                client.subscribe(
                    arrayOf(MqttSubscription(topic)), null, object : MqttActionListener {
                        override fun onSuccess(asyncActionToken: IMqttToken?) {
                            logger.info { "Subscribed to $topic" }
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

    override suspend fun unsubscribe(topic: Topic, scope: CoroutineScope) {
        subscriptions.remove(topic)
        client.unsubscribe(topic)
    }

    override suspend fun publish(topic: String, message: String, qos: Int, retained: Boolean) =
        suspendCoroutine<Unit> { cont ->
            try {
                logger.debug { "Publishing to $topic: $message" }
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