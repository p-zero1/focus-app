package com.focusapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class GetCurrentWeekBoundsUseCaseTest {

    private val useCase = GetCurrentWeekBoundsUseCase()
    private val zone: ZoneId = ZoneId.systemDefault()

    @Test
    fun `startMs is midnight of Monday this week`() {
        val bounds = useCase()
        val startDate = java.time.Instant.ofEpochMilli(bounds.startMs)
            .atZone(zone).toLocalDate()
        assertEquals(DayOfWeek.MONDAY, startDate.dayOfWeek)
    }

    @Test
    fun `endMs is midnight of Monday next week`() {
        val bounds = useCase()
        val endDate = java.time.Instant.ofEpochMilli(bounds.endMs)
            .atZone(zone).toLocalDate()
        assertEquals(DayOfWeek.MONDAY, endDate.dayOfWeek)
    }

    @Test
    fun `endMs is exactly 7 days after startMs`() {
        val bounds = useCase()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        assertEquals(sevenDaysMs, bounds.endMs - bounds.startMs)
    }

    @Test
    fun `startMs is before or equal to today midnight`() {
        val bounds = useCase()
        val todayMidnight = LocalDate.now(zone)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        assertTrue(bounds.startMs <= todayMidnight)
    }

    @Test
    fun `endMs is after today midnight`() {
        val bounds = useCase()
        val todayMidnight = LocalDate.now(zone)
            .atStartOfDay(zone).toInstant().toEpochMilli()
        assertTrue(bounds.endMs > todayMidnight)
    }

    @Test
    fun `startMs and endMs are at midnight boundary (zero time component)`() {
        val bounds = useCase()
        val startZdt = java.time.Instant.ofEpochMilli(bounds.startMs).atZone(zone)
        val endZdt = java.time.Instant.ofEpochMilli(bounds.endMs).atZone(zone)
        assertEquals(0, startZdt.hour)
        assertEquals(0, startZdt.minute)
        assertEquals(0, endZdt.hour)
        assertEquals(0, endZdt.minute)
    }
}
