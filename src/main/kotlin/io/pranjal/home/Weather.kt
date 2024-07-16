package io.pranjal.home

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

import kotlinx.serialization.json.Json
import java.lang.System.Logger
import kotlin.concurrent.thread

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

val API_KEY = System.getenv()["OPENWEATHER_API_KEY"] ?: throw Exception("OPENWEATHER_API_KEY not set")

val logger = KotlinLogging.logger {}

val lat = "42.402853507187295"
val long = "-71.10932974603533"

private var weatherTtl: Duration = 5.minutes

private var weatherFlow = MutableSharedFlow<Weather>(replay = 1)

fun startUpdateWeatherThread() {
    runBlocking {
        weatherFlow.emit(fetchWeather())
    }
    thread {
        while (true) {
            runBlocking {
                weatherFlow.emit(fetchWeather())
            }
            Thread.sleep(weatherTtl.inWholeMilliseconds)
        }
    }
}

fun getWeather(): Weather {
    return weatherFlow.replayCache.last()
}

private class InstantSerializer : kotlinx.serialization.KSerializer<Instant> {
    override val descriptor =
        kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("Instant", kind = PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochSeconds(decoder.decodeLong())
    }
}

@Serializable
data class Weather(
    @Serializable(with = InstantSerializer::class)
    val sunrise: Instant,
    @Serializable(with = InstantSerializer::class)
    val sunset: Instant,
    val temp: Double,
    val feels_like: Double,
    val pressure: Int,
    val humidity: Int,
    val dew_point: Double,
    val clouds: Int, /* 0-100 */
    val visibility: Int, /* meters */
    val uvi: Double,
    val wind_speed: Double,
) {
    val windSpeed = wind_speed
    val feelsLike = feels_like
    val dewPoint = dew_point

    companion object {
        val flow = weatherFlow.asSharedFlow()
    }
}


private val weatherClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(json = Json { ignoreUnknownKeys = true })
    }
}

@Serializable
data class WeatherResponse(
    val current: Weather
)

private suspend fun fetchWeather(): Weather {
    try {
        val weather =
            weatherClient.get("https://api.openweathermap.org/data/3.0/onecall?lat=$lat&lon=$long&exclude=minutely,hourly,daily&appid=$API_KEY")
        try {
            return weather.body<WeatherResponse>().current
        } catch (e: Exception) {
            logger.error { "Failed to parse weather" }
            logger.error { weather }
            throw e
        }

    } catch (e: Exception) {
        logger.error { "Failed to fetch weather" }
        throw e
    }
}


