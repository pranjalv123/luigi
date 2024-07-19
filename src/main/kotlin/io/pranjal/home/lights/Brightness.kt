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
            value < 0 -> OFF
            value > 1 -> MAX
            else -> this
        }
    }

    fun dim(levels: Int): Brightness = Brightness(floor(((value - 0.01)) * levels)/levels).constrain()
    fun brighten(levels: Int): Brightness = Brightness(ceil(((value + 0.01)) * levels) /levels).constrain()

    fun toRange(min: Int, max: Int): Int = (value * (max - min) + min).roundToInt()
}