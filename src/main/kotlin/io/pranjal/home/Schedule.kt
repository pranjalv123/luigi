package io.pranjal.home

import io.pranjal.home.lights.Brightness
import io.pranjal.home.lights.ColorTemperature
import jdk.jfr.Frequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

val TZ = TimeZone.of("America/New_York")

@Serializable
sealed interface Schedule<T> {
    val clock: Clock
    fun valueAt(time: Instant): T
    fun now(): T = valueAt(clock.now())
    fun prune() {}

    fun runEvery(duration: Duration, scope: CoroutineScope): StateFlow<T> {
        val flow = MutableStateFlow(now())
        scope.launch {
            while (true) {
                val startTime = clock.now()
                try {
                    flow.value = now()
                } catch (e: Exception) {
                    println("Failed to run scheduled task")
                    e.printStackTrace()
                }
                prune()
                val delay = duration - (clock.now() - startTime)
                kotlinx.coroutines.delay(delay.toLong(DurationUnit.MILLISECONDS))

            }
        }
        return flow
    }
}


interface Interpolatable<T> {
    //    companion object {
//        val serializersModule = SerializersModule {
//            polymorphic(Interpolatable::class) {
//                subclass(Brightness::class, Brightness.serializer())
//                subclass(ColorTemperature::class, ColorTemperature.serializer())
//            }
//        }
//    }
    operator fun plus(a: T): T
    operator fun minus(a: T): T
    operator fun times(b: Double): T
}


private class DailyInterpolatingSchedule<T : Interpolatable<T>>(
    vararg val points: Pair<LocalTime, T>,
    override val clock: Clock
) : Schedule<T> {
    override fun valueAt(time: Instant): T {
        val localTime = time.toLocalDateTime(TZ).time
        val (firstTime, firstValue) = points.lastOrNull { it.first <= localTime } ?: points.first()
        val (secondTime, secondValue) = points.firstOrNull { it.first > localTime } ?: points.last()
        val timeInterval = secondTime.toSecondOfDay() - firstTime.toSecondOfDay()
        val timePassed = localTime.toSecondOfDay() - firstTime.toSecondOfDay()
        if (timeInterval == 0) return firstValue
        val timeFraction = timePassed.toDouble() / timeInterval
        return firstValue + (secondValue - firstValue) * timeFraction
    }
}

@Serializable
data class BrightnessAtTime(val time: LocalTime, val brightness: Brightness) {
    fun toPair() = time to brightness
}

@Serializable
class BrightnessSchedule(
    vararg val brightnesses: BrightnessAtTime,
    @kotlinx.serialization.Transient
    override val clock: Clock = Clock.System
) : Schedule<Brightness> by DailyInterpolatingSchedule(
    *(brightnesses.map { it.toPair() }.toTypedArray()),
    clock = clock
) {
}

@Serializable
data class ColorTemperatureAtTime(val time: LocalTime, val colorTemperature: ColorTemperature) {
    fun toPair() = time to colorTemperature
}

@Serializable
class ColorTemperaturesSchedule(
    vararg val colorTemperatures: ColorTemperatureAtTime,
    override val clock: Clock
) : Schedule<ColorTemperature> by DailyInterpolatingSchedule(*(colorTemperatures.map { it.toPair() }
    .toTypedArray()), clock = clock)