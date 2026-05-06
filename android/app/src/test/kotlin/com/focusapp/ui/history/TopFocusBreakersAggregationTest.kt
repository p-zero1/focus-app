package com.focusapp.ui.history

import com.focusapp.domain.model.DistractionEvent
import com.focusapp.domain.model.DistractionType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the aggregation and ranking logic used by TopFocusBreakersCard.
 * Mirrors the exact groupBy/map/sortedByDescending pipeline in the composable.
 */
class TopFocusBreakersAggregationTest {

    // Mirrors the ranked pipeline in TopFocusBreakersCard
    private fun aggregate(distractions: List<DistractionEvent>): List<Triple<String, Int, Int>> =
        distractions
            .groupBy { it.appPackageName ?: "Screen unlock" }
            .map { (name, events) -> Triple(name, events.size, events.sumOf { it.durationSeconds }) }
            .sortedByDescending { it.third }

    @Test
    fun `multiple events from same app are grouped into one row`() {
        val events = listOf(
            event(app = "Instagram", duration = 120),
            event(app = "Instagram", duration = 60),
        )
        val result = aggregate(events)
        assertEquals(1, result.size)
        assertEquals("Instagram", result[0].first)
        assertEquals(2, result[0].second)   // 2 occurrences
        assertEquals(180, result[0].third)  // 120 + 60 seconds
    }

    @Test
    fun `rows sorted by total time descending`() {
        val events = listOf(
            event(app = "WhatsApp", duration = 30),
            event(app = "Instagram", duration = 300),
            event(app = "WhatsApp", duration = 30),
        )
        val result = aggregate(events)
        assertEquals(2, result.size)
        assertEquals("Instagram", result[0].first)  // 300s — worst offender first
        assertEquals("WhatsApp", result[1].first)   // 60s total
    }

    @Test
    fun `null packageName grouped as Screen unlock`() {
        val events = listOf(
            event(app = null, duration = 45),
            event(app = null, duration = 15),
        )
        val result = aggregate(events)
        assertEquals(1, result.size)
        assertEquals("Screen unlock", result[0].first)
        assertEquals(2, result[0].second)
        assertEquals(60, result[0].third)
    }

    @Test
    fun `screen unlock and app switch are separate rows`() {
        val events = listOf(
            event(app = "YouTube", duration = 90),
            event(app = null, duration = 20),
        )
        val result = aggregate(events)
        assertEquals(2, result.size)
        assertEquals("YouTube", result[0].first)      // 90s > 20s
        assertEquals("Screen unlock", result[1].first)
    }

    @Test
    fun `single event produces count of 1`() {
        val events = listOf(event(app = "YouTube", duration = 90))
        val result = aggregate(events)
        assertEquals(1, result.size)
        assertEquals(1, result[0].second)
        assertEquals(90, result[0].third)
    }

    @Test
    fun `empty input produces empty result`() {
        assertEquals(emptyList<Triple<String, Int, Int>>(), aggregate(emptyList()))
    }

    @Test
    fun `three apps ranked correctly by total seconds`() {
        val events = listOf(
            event(app = "Twitter", duration = 10),
            event(app = "Reddit", duration = 500),
            event(app = "Twitter", duration = 20),
            event(app = "Spotify", duration = 200),
        )
        val result = aggregate(events)
        assertEquals("Reddit", result[0].first)    // 500s
        assertEquals("Spotify", result[1].first)   // 200s
        assertEquals("Twitter", result[2].first)   // 30s
    }

    // ---- Helper ----

    private fun event(app: String?, duration: Int) = DistractionEvent(
        id = 0L,
        sessionId = 1L,
        timestamp = 0L,
        type = if (app != null) DistractionType.APP_SWITCH else DistractionType.SCREEN_UNLOCK,
        appPackageName = app,
        durationSeconds = duration,
    )
}
