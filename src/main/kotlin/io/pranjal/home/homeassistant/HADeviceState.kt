package io.pranjal.home.homeassistant

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LightsState(
    val brightness: Int? = null,
    @SerialName("color_temp")
    val colorTemperature: Int? = null,
    val state: StateEnum? = null,
    @SerialName("color_mode")
    val colorMode: String? = null,
    val transition: Int? = null,
    val color: Color? = null,
    val effect: String? = null,
)  {

    enum class StateEnum {
        ON,
        OFF
    }

}

@Serializable
data class Color(
    val r: Int?,
    val g: Int?,
    val b: Int?,
    val c: Int?,
    val w: Int?,
    val x: Float?,
    val y: Float?,
    val h: Float?,
    val s: Float?,
)