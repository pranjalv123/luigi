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
)  {

    enum class StateEnum {
        ON,
        OFF
    }

}