package io.pranjal.home

import jdk.jfr.Frequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

val TZ = TimeZone.of("America/New_York")

suspend fun runDaily(at: LocalTime, scope: CoroutineScope): Flow<Unit> = flow {
    scope.launch {
        while (true) {
            val now = Clock.System.now().toLocalDateTime(TZ)
            val today = Clock.System.todayIn(TZ)
            val target = today.atTime(at)
            val delay = if (now > target) {
                (target.toInstant(TZ) + 1.days) - Clock.System.now()
            } else {
                target.toInstant(TZ) - Clock.System.now()
            }
            kotlinx.coroutines.delay(delay.toLong(DurationUnit.MILLISECONDS))
            emit(Unit)
        }
    }
}

abstract class Schedule<T> {
    private var overrides = mutableListOf<OverrideSchedule<T>>()
    abstract fun valueAt(time: Instant): T
    fun now(): T = valueAt(Clock.System.now())
    open fun prune() {
        overrides = overrides.filter { !it.isExpired }.toMutableList()
    }
    fun runEvery(duration: Duration): Flow<T> = flow {
        while (true) {
            val startTime = Clock.System.now()
            try {
                emit(overrides.firstOrNull { it.isActive }?.now() ?: now())
            } catch (e: Exception) {
                println("Failed to run scheduled task")
                e.printStackTrace()
            }
            prune()
            val delay = duration - (Clock.System.now() - startTime)
            kotlinx.coroutines.delay(delay.toLong(DurationUnit.MILLISECONDS))

        }
    }

    fun override(override: Schedule<T>, startTime: Instant, endTime: Instant) {
        overrides.add(OverrideSchedule(override, startTime, endTime))
    }
    fun cancelOverrides() {
        overrides.forEach { it.cancel() }
    }
}

class OverrideSchedule<T>(val schedule: Schedule<T>, val startTime: Instant, val endTime: Instant) :
    Schedule<T>() {
    private var cancelled: Boolean = false
    override fun valueAt(time: Instant): T = schedule.valueAt(time)
    fun cancel() {
        cancelled = true
    }
    val isActive: Boolean
        get() {
            if (cancelled) return false
            val now = Clock.System.now()
            return now in startTime..endTime
        }

    val isExpired: Boolean
        get() {
            if (cancelled) return true
            val now = Clock.System.now()
            return now > endTime
        }

}

class FixedSchedule<T>(val value: T) : Schedule<T>() {
    override fun valueAt(time: Instant): T = value
}

class InterpolatingSchedule(vararg startingPoints: Pair<Instant, Double>) : Schedule<Double>() {
    private data class Point(val time: Instant, val value: Double)

    private val points = startingPoints.map { Point(it.first, it.second) }.toMutableList()
    override fun prune() {
        super.prune()
        val now = Clock.System.now()
        val mostRecentPastPoint = points.filter { it.time < now }.maxByOrNull { it.time } ?: return
        points.removeIf { it.time < mostRecentPastPoint.time }
    }

    @Synchronized
    fun reset(vararg startingPoints: Pair<Instant, Double>) {
        points.clear()
        points.addAll(startingPoints.map { Point(it.first, it.second) })
    }

    @Synchronized
    fun addPoint(time: Instant, value: Double) {
        points.removeIf { it.time == time }
        points.add(Point(time, value))
        prune()
        points.sortBy { it.time }
    }

    @Synchronized
    override fun valueAt(time: Instant): Double {
        points.binarySearchBy(time) { it.time }.let { index ->
            if (index >= 0) {
                return points[index].value
            } else {
                val insertionPoint = -index - 1
                if (insertionPoint == 0) {
                    return points.first().value
                } else if (insertionPoint == points.size) {
                    return points.last().value
                } else {
                    val before = points[insertionPoint - 1]
                    val after = points[insertionPoint]
                    val timeFraction =
                        (time - before.time).inWholeMilliseconds.toDouble() / (after.time - before.time).inWholeMilliseconds.toDouble()
                    return before.value + (after.value - before.value) * timeFraction
                }
            }
        }
    }
}

fun <T> schedule(block: (time: Instant) -> T): Schedule<T> {
    return object : Schedule<T>() {
        override fun valueAt(time: Instant): T = block(time)
    }
}