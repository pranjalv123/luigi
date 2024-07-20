package io.pranjal.home.lights

import io.pranjal.home.Schedule
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Serializable
data class BaseState(
    val brightness: Schedule<Brightness>,
    val colorTemperature: Schedule<ColorTemperature>,
    val brightnessLevels: List<Double> = Levels.renard10,
    val dimmingLifetime: Duration = 4.hours,
    val clock: Clock
)


sealed class State {
    abstract val baseState: BaseState
    abstract fun transition(input: Input): State
    open val brightness: Brightness
        get() = baseState.brightness.now()
    open val colorTemperature: ColorTemperature
        get() = baseState.colorTemperature.now()

    @Serializable
    data class Off(override val baseState: BaseState) : State() {
        override fun transition(input: Input) =
            when (input) {
                is Input.TurnOn -> Default(baseState)
                is Input.Toggle -> Default(baseState)
                is Input.IncreaseBrightness ->
                    Brightened(Brightness.MAX, baseState)

                Input.DecreaseBrightness -> Dimmed(Brightness.MIN, baseState)
                Input.Reset -> this
                Input.TurnOff -> this
                is Input.Custom -> Custom(input.colorTemperature, input.brightness, baseState)
            }

        override val brightness = Brightness.OFF

    }

    @Serializable
    data class Default(override val baseState: BaseState) : State() {
        override fun transition(input: Input) =
            when (input) {
                Input.DecreaseBrightness -> Dimmed(brightness.dim(baseState.brightnessLevels), baseState)
                Input.IncreaseBrightness, Input.TurnOn -> Brightened(brightness.brighten(baseState.brightnessLevels), baseState)
                Input.Reset -> this
                Input.Toggle -> Off(baseState)
                Input.TurnOff -> Off(baseState)
                is Input.Custom -> Custom(input.colorTemperature, input.brightness, baseState)
            }
    }

    @Serializable
    data class Brightened(override val brightness: Brightness, override val baseState: BaseState) : State() {
        override fun transition(input: Input) =
            when (input) {
                Input.DecreaseBrightness -> {
                    val newBrightness = brightness.dim(baseState.brightnessLevels)
                    if (newBrightness < baseState.brightness.now()) {
                        Default(baseState)
                    } else {
                        Brightened(newBrightness, baseState)
                    }
                }

                Input.IncreaseBrightness, Input.TurnOn -> Brightened(brightness.brighten(baseState.brightnessLevels), baseState)
                Input.Reset -> Default(baseState)
                Input.Toggle -> Off(baseState)
                Input.TurnOff -> Off(baseState)
                is Input.Custom -> Custom(input.colorTemperature, input.brightness, baseState)
            }

    }

    @Serializable
    data class Dimmed(override val brightness: Brightness, override val baseState: BaseState) : State() {
        override fun transition(input: Input) =
            when (input) {
                Input.DecreaseBrightness -> Dimmed(brightness.dim(baseState.brightnessLevels), baseState)
                Input.IncreaseBrightness, Input.TurnOn -> {
                    val newBrightness = brightness.brighten(baseState.brightnessLevels)
                    if (newBrightness > baseState.brightness.now()) {
                        Default(baseState)
                    } else {
                        Dimmed(newBrightness, baseState)
                    }
                }

                Input.Reset -> Default(baseState)
                Input.Toggle -> DimmedOff(this, baseState)
                Input.TurnOff -> DimmedOff(this, baseState)

                is Input.Custom -> Custom(input.colorTemperature, input.brightness, baseState)
            }

    }

    @Serializable
    data class DimmedOff(
        val dimmed: Dimmed,
        override val baseState: BaseState,
        val startTime: Instant = baseState.clock.now()
    ) :
        State() {

        override fun transition(input: Input) =
            when (input) {
                is Input.TurnOn, is Input.Toggle -> {
                    if ((baseState.clock.now() - startTime) > baseState.dimmingLifetime) {
                        Default(baseState)
                    } else {
                        dimmed
                    }
                }

                is Input.IncreaseBrightness ->
                    Brightened(Brightness.MAX, baseState)

                Input.DecreaseBrightness -> this
                Input.Reset -> Off(baseState)
                Input.TurnOff -> this
                is Input.Custom -> Custom(input.colorTemperature, input.brightness, baseState)
            }

        override val brightness = Brightness.OFF
    }

    @Serializable
    data class Custom(
        override val colorTemperature: ColorTemperature,
        override val brightness: Brightness,
        override val baseState: BaseState
    ) : State() {
        override fun transition(input: Input) =
            when (input) {
                is Input.TurnOn, is Input.Toggle -> this
                is Input.IncreaseBrightness -> this
                is Input.DecreaseBrightness -> this
                is Input.Reset -> Default(baseState)
                is Input.TurnOff -> Off(baseState)
                is Input.Custom -> Custom(input.colorTemperature, input.brightness, baseState)
            }

    }
}