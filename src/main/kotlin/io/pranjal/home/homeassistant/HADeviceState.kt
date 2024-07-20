package io.pranjal.home.homeassistant

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
class LightsState(
    val brightness: Int? = null,
    val colorTemperature: Int? = null,
    val state: StateEnum? = null
)  {

    enum class StateEnum {
        ON,
        OFF
    }

}