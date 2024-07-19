package io.pranjal.home.lights

import io.pranjal.home.BrightnessAtTime
import io.pranjal.home.BrightnessSchedule
import io.pranjal.home.ColorTemperatureAtTime
import io.pranjal.home.ColorTemperaturesSchedule
import io.pranjal.home.devices.inputFlow
import io.pranjal.home.devices.makes.VirtualDimmer
import io.pranjal.home.devices.makes.VirtualLight
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class LightsGroupTest {

    fun lightsGroup(
        coroutineScope: CoroutineScope,
        brightnessSchedule: BrightnessSchedule,
        colorTemperaturesSchedule: ColorTemperaturesSchedule,
        clock: Clock
    ) = LightsGroup(
        coroutineScope,
        name = "TestGroup",
        lights = listOf(VirtualLight("TestLight")),
        switches = listOf(VirtualDimmer("TestDimmer")),
        brightnessSchedule = brightnessSchedule,
        colorTemperatureSchedule = colorTemperaturesSchedule,
        clock = clock
    )

    class TestClock : Clock {

        var time = Instant.fromEpochSeconds(0)
        override fun now(): Instant {
            return time
        }

        fun set(time: Instant) {
            this.time = time
        }

        fun advance(duration: Duration) {
            time = time + duration
        }
    }

    val tz = TimeZone.of("America/New_York")
    var lgJob: Job? = null
    var lg: LightsGroup? = null
    var clock: TestClock = TestClock()
    lateinit var light: VirtualLight
    lateinit var switch: VirtualDimmer


    suspend fun setUp(scope: TestScope) {
        clock.set(LocalDateTime(2021, 1, 1, 0, 0).toInstant(tz))
        lgJob =
            scope.launch {
                lg = lightsGroup(
                    this, BrightnessSchedule(
                        BrightnessAtTime(LocalTime(0, 0), Brightness(0.1)),
                        BrightnessAtTime(LocalTime(8, 0), Brightness(0.1)),
                        BrightnessAtTime(LocalTime(12, 0), Brightness(1.0)),
                        BrightnessAtTime(LocalTime(20, 0), Brightness(0.1)),
                        BrightnessAtTime(
                            LocalTime(23, 59), Brightness(0.1)
                        ),
                        clock = clock
                    ), ColorTemperaturesSchedule(
                        ColorTemperatureAtTime(LocalTime(0, 0), ColorTemperature(2300)),
                        ColorTemperatureAtTime(LocalTime(23, 59), ColorTemperature(2300)),
                        clock = clock
                    ), clock
                )
            }
        while (lg == null || !lg!!.initialized) {
            delay(1)
        }

        light = lg!!.lights[0] as VirtualLight
        switch = lg!!.switches[0] as VirtualDimmer

        while (switch.onFlow.subscriptionCount.value == 0) {
            delay(1)
        }


    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.advance(duration: Duration) {
        clock.advance(duration)
        advanceTimeBy(duration)
    }


    @Test
    fun testClock() = runTest {
        setUp(this)
        assert(clock.now() == LocalDateTime(2021, 1, 1, 0, 0).toInstant(tz))
        clock.advance(1.hours)
        assert(clock.now() == LocalDateTime(2021, 1, 1, 1, 0).toInstant(tz))
        advance(1.hours)
        assert(clock.now() == LocalDateTime(2021, 1, 1, 2, 0).toInstant(tz))
        lgJob?.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testLightsGroup() =
        runTest {
            setUp(this)

            assertEquals(Brightness(0.0), light.brightness)

            switch.onFlow.emit(Unit)
            delay(1)
            assertEquals(Brightness(0.1), light.brightness)


            while (clock.now().toLocalDateTime(tz).time < LocalTime(8, 0)) {
                advance(1.minutes)
                assertEquals(Brightness(0.1), light.brightness)
            }

            while (clock.now().toLocalDateTime(tz).time < LocalTime(8, 10)) {
                advance(1.minutes)
                assert(0.1 < light.brightness.value)
            }
            switch.offFlow.emit(Unit)
            delay(1)
            assertEquals(Brightness.OFF, light.brightness)

            lgJob?.cancel()
        }

    @Test
    fun testBrightnessMaintained() = runTest {
        setUp(this)
        clock.set(LocalDateTime(2021, 1, 1, 8, 10).toInstant(tz))
        switch.onFlow.emit(Unit)
        delay(5)
        assertEquals(Brightness(0.1375), light.brightness)

        switch.offFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness.OFF, light.brightness)

        switch.onFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.1375), light.brightness)
        lgJob?.cancel()

    }

    @Test
    fun testIncreaseBrightness() = runTest {
        setUp(this)
        clock.set(LocalDateTime(2021, 1, 1, 8, 10).toInstant(tz))

        switch.onFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.1375), light.brightness)


        switch.increaseBrightnessFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.2), light.brightness)
        advance(4.hours)
        assertEquals(Brightness(0.2), light.brightness)

        switch.offFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness.OFF, light.brightness)

        switch.onFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.98125), light.brightness)
        lgJob?.cancel()
    }


    @Test
    fun testDecreaseBrightness() = runTest {
        setUp(this)
        clock.set(LocalDateTime(2021, 1, 1, 8, 10).toInstant(tz))

        switch.onFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.1375), light.brightness)


        switch.decreaseBrightnessFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.1), light.brightness)
        advance(1.hours)
        assertEquals(Brightness(0.1), light.brightness)

        switch.offFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness.OFF, light.brightness)

        switch.onFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.1), light.brightness)


        switch.offFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness.OFF, light.brightness)

        advance(5.hours)

        assertEquals(Brightness.OFF, light.brightness)



        switch.onFlow.emit(Unit)
        delay(1)
        assertEquals(Brightness(0.75625), light.brightness)

        lgJob?.cancel()
    }

}