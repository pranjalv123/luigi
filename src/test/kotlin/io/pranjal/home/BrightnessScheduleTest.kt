package io.pranjal.home

import io.pranjal.home.lights.Brightness
import kotlinx.datetime.*
import org.junit.jupiter.api.Assertions.*
import kotlin.test.Test

class BrightnessScheduleTest {
    private val schedule = BrightnessSchedule(
        BrightnessAtTime(LocalTime(0, 0), Brightness(0.1)),
        BrightnessAtTime(LocalTime(8, 0), Brightness(0.1)),
        BrightnessAtTime(LocalTime(12, 0), Brightness(1.0)),
        BrightnessAtTime(LocalTime(20, 0), Brightness(0.1)),
        BrightnessAtTime(
            LocalTime(23, 59), Brightness(0.1)
        ),
        clock = Clock.System
    )

    private val tz = TimeZone.of("America/New_York")

    fun time(hour: Int, minute: Int) = LocalDateTime(2020, 1, 1, hour, minute).toInstant(tz)

    @Test
    fun testSchedule() {
        assertEquals(
            0.1,
            schedule.valueAt(time(0, 0)).value
        )
        assertEquals(
            0.1,
            schedule.valueAt(time(8, 0)).value
        )
        assertEquals(
            0.1375,
            schedule.valueAt(time(8, 10)).value
        )
        assertEquals(
            0.55,
            schedule.valueAt(time(10, 0)).value
        )
        assertEquals(
            1.0,
            schedule.valueAt(time(12, 0)).value
        )
        assertEquals(
            0.1,
            schedule.valueAt(time(20, 0)).value
        )
        assertEquals(
            0.1,
            schedule.valueAt(time(23, 59)).value
        )
    }
}