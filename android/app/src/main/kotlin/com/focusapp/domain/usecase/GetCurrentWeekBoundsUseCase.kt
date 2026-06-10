package com.focusapp.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * Returns the epoch-millisecond bounds [startMs, endMs) for the current ISO week
 * (Monday–Sunday) in the device's local timezone.
 *
 * Extracted from [com.focusapp.ui.analytics.AnalyticsViewModel] to satisfy
 * Constitution Principle II — date-range computation is domain business logic.
 */
class GetCurrentWeekBoundsUseCase @Inject constructor() {

    data class WeekBounds(val startMs: Long, val endMs: Long)

    operator fun invoke(): WeekBounds {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusWeeks(1)
        return WeekBounds(
            startMs = weekStart.atStartOfDay(zone).toInstant().toEpochMilli(),
            endMs = weekEnd.atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }
}
