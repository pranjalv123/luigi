package io.pranjal.home.lights

import io.pranjal.home.Interpolatable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

object Levels {
    val renard10 = listOf(0.01, 0.016, 0.025, 0.04, 0.063, 0.1, 0.16, 0.25, 0.4, 0.63, 1.0)
    val linear10 = (0..10).map { it / 10.0 }
}

@Serializable(with = Brightness.Serializer::class)
data class Brightness(val value: Double) : Interpolatable<Brightness> {
    class Serializer : KSerializer<Brightness> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Brightness", PrimitiveKind.DOUBLE)
        override fun serialize(encoder: Encoder, value: Brightness) = encoder.encodeDouble(value.value)
        override fun deserialize(decoder: Decoder): Brightness = Brightness(decoder.decodeDouble())
    }

    companion object {
        val OFF = Brightness(0.0)
        val MIN = Brightness(0.01)
        val MAX = Brightness(1.0)
    }

    override fun plus(a: Brightness): Brightness = Brightness(value + a.value)

    override fun minus(a: Brightness): Brightness = Brightness(value - a.value)

    override fun times(b: Double): Brightness = Brightness(value * b)

    operator fun compareTo(other: Brightness): Int = value.compareTo(other.value)

    fun constrain(): Brightness {
        return when {
            value <= 0 -> MIN
            value > 1 -> MAX
            else -> this
        }
    }


    fun dim(levels: List<Double>): Brightness = levels.filter { it < value }.maxOrNull()?.let { Brightness(it) } ?: MIN

    fun brighten(levels: List<Double>): Brightness =
        levels.filter { it > value }.minOrNull()?.let { Brightness(it) } ?: MAX

    fun toRange(min: Int, max: Int): Int = (value * (max - min) + min).roundToInt()
}