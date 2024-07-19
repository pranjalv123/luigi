package io.pranjal.home.lights

import io.pranjal.home.Interpolatable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = ColorTemperature.Serializer::class)
data class ColorTemperature(val temperature: Int) : Interpolatable<ColorTemperature> {
    class Serializer : KSerializer<ColorTemperature> {
        override val descriptor: SerialDescriptor =
            PrimitiveSerialDescriptor("ColorTemperature", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: ColorTemperature) = encoder.encodeInt(value.temperature)
        override fun deserialize(decoder: Decoder): ColorTemperature = ColorTemperature(decoder.decodeInt())
    }

    override fun plus(a: ColorTemperature): ColorTemperature = ColorTemperature(temperature + a.temperature)

    override fun minus(a: ColorTemperature): ColorTemperature = ColorTemperature(temperature - a.temperature)

    override fun times(b: Double): ColorTemperature = ColorTemperature((temperature * b).toInt())

    val temperatureReciprocalMegakelvin = 1.0 / (temperature / 1000000.0)
}