package io.pranjal.home.devices

import io.pranjal.home.lights.Brightness
import io.pranjal.home.lights.ColorTemperature
import io.pranjal.home.lights.Input
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

interface Switch {
    suspend fun turnOn(scope: CoroutineScope): Flow<Unit> = emptyFlow()
    suspend fun turnOff(scope: CoroutineScope): Flow<Unit> = emptyFlow()
    suspend fun toggle(scope: CoroutineScope): Flow<Unit> = emptyFlow()
    suspend fun increaseBrightness(scope: CoroutineScope): Flow<Unit> = emptyFlow()
    suspend fun decreaseBrightness(scope: CoroutineScope): Flow<Unit> = emptyFlow()
    suspend fun reset(scope: CoroutineScope): Flow<Unit> = emptyFlow()
    suspend fun custom(scope: CoroutineScope): Flow<Pair<ColorTemperature, Brightness>> = emptyFlow()

}


suspend fun Switch.inputFlow(scope: CoroutineScope): Flow<Input> =
    merge(
        turnOn(scope).map { Input.TurnOn },
        turnOff(scope).map { Input.TurnOff },
        toggle(scope).map { Input.Toggle },
        increaseBrightness(scope).map { Input.IncreaseBrightness },
        decreaseBrightness(scope).map { Input.DecreaseBrightness },
        reset(scope).map { Input.Reset },
        custom(scope).map { Input.Custom(it.first, it.second) }
    ).onEach {
        println("inputFlow: $it")
    }
