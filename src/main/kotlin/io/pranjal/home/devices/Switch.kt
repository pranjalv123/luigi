package io.pranjal.home.devices

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

interface Switch {
    suspend fun turnOn(scope: CoroutineScope) : Flow<Unit> = emptyFlow()
    suspend fun turnOff(scope: CoroutineScope) : Flow<Unit> = emptyFlow()
    suspend fun toggle(scope: CoroutineScope) : Flow<Unit> = emptyFlow()
    suspend fun increaseBrightness(scope: CoroutineScope) : Flow<Unit> = emptyFlow()
    suspend fun decreaseBrightness(scope: CoroutineScope) : Flow<Unit> = emptyFlow()
}