package io.pranjal.home.lights

sealed interface Input {
    data object TurnOn : Input
    data object TurnOff : Input
    data object Toggle : Input
    data object IncreaseBrightness : Input
    data object DecreaseBrightness : Input
    data object Reset : Input

    data class Custom(val colorTemperature: ColorTemperature, val brightness: Brightness) : Input
}