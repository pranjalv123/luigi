package io.pranjal.home.devices

interface Light {
    suspend fun turnOn(transitionSec: Int = 0)
    suspend fun turnOff(transitionSec: Int = 0)
    suspend fun toggle(transitionSec: Int = 0)
    suspend fun setBrightness(brightness: Double, transitionSec: Int = 0)
    suspend fun moveBrightness(speed: Double)
    suspend fun stepBrightness(step: Int)
    suspend fun setColorTemperature(temperature: Int, transitionSec: Int = 0)

}