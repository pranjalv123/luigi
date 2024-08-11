package io.pranjal.home.lights


import io.pranjal.home.*
import kotlinx.datetime.*
import kotlin.reflect.jvm.internal.impl.descriptors.Visibilities.Local
import kotlin.time.Duration.Companion.hours

fun standardTempSchedule(clock: Clock): Schedule<ColorTemperature> {
    return listOf(
        "00:00" to 2300,
        "07:00" to 2300,
        "09:00" to 3500,
        "12:00" to 4800,
        "17:00" to 4800,
        "19:00" to 3500,
        "21:00" to 2300,
        "23:59" to 2300,
    ).map { (time, temp) ->
        ColorTemperatureAtTime(
            LocalTime.parse(time), ColorTemperature(temp)
        )
    }.let { ColorTemperaturesSchedule(*it.toTypedArray(), clock = clock) }
}

fun downstairsBrightnessSchedule(clock: Clock): Schedule<Brightness> {
    return listOf(
        "00:00" to 0.05,
        "07:00" to 0.05,
        "08:00" to 0.2,
        "09:00" to 0.5,
        "10:00" to 1.0,
        "17:00" to 1.0,
        "19:00" to 0.7,
        "21:00" to 0.2,
        "23:59" to 0.05,
    ).map { (time, brightness) ->
        BrightnessAtTime(LocalTime.parse(time), Brightness(brightness))
    }.let { BrightnessSchedule(*it.toTypedArray(), clock = clock) }
}

fun bedroomBrightnessSchedule(clock: Clock): Schedule<Brightness> {
    return listOf(
        "00:00" to 0.01,
        "07:00" to 0.01,
        "08:00" to 0.2,
        "09:00" to 0.5,
        "10:00" to 1.0,
        "17:00" to 1.0,
        "19:00" to 0.7,
        "21:00" to 0.1,
        "22:00" to 0.01,
        "23:59" to 0.01,
    ).map { (time, brightness) ->
        BrightnessAtTime(LocalTime.parse(time), Brightness(brightness))
    }.let { BrightnessSchedule(*it.toTypedArray(), clock = clock) }
}

fun bathroomBrightnessSchedule(clock: Clock): Schedule<Brightness> {
    return listOf(
        "00:00" to 0.01,
        "07:00" to 0.01,
        "08:00" to 0.2,
        "09:00" to 0.5,
        "10:00" to 1.0,
        "17:00" to 1.0,
        "19:00" to 0.7,
        "21:00" to 0.1,
        "22:00" to 0.01,
        "23:59" to 0.01,
    ).map { (time, brightness) ->
        BrightnessAtTime(LocalTime.parse(time), Brightness(brightness))
    }.let { BrightnessSchedule(*it.toTypedArray(), clock = clock) }
}