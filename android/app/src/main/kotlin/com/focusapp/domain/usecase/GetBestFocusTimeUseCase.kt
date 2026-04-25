package com.focusapp.domain.usecase

import com.focusapp.domain.model.FocusSession
import javax.inject.Inject
import java.util.Calendar

/**
 * Identifies the user's best focus time-of-day from historical sessions.
 *
 * Algorithm (from tasks.md T050 / research.md §7):
 * 1. If fewer than 5 sessions provided, returns null (insight not yet meaningful).
 * 2. Buckets sessions into 2-hour slots (0–2, 2–4, …, 22–24) by their start hour.
 * 3. For each slot that has at least one session, computes average distractionCount.
 * 4. Returns the slot label (e.g. "10–12 AM") with the lowest average distraction count.
 */
class GetBestFocusTimeUseCase @Inject constructor() {

    /** Returns a human-readable time range string, or null if data is insufficient. */
    operator fun invoke(sessions: List<FocusSession>): String? {
        if (sessions.size < 5) return null

        // Map slotIndex (0–11) → list of distractionCounts
        val slotDistractions = mutableMapOf<Int, MutableList<Int>>()

        sessions.forEach { session ->
            val hour = Calendar.getInstance().apply {
                timeInMillis = session.startTime
            }.get(Calendar.HOUR_OF_DAY)
            val slot = hour / 2
            slotDistractions.getOrPut(slot) { mutableListOf() }.add(session.distractionCount)
        }

        val bestSlot = slotDistractions.minByOrNull { (_, counts) ->
            counts.average()
        }?.key ?: return null

        return formatSlot(bestSlot)
    }

    private fun formatSlot(slot: Int): String {
        val startHour = slot * 2
        val endHour = startHour + 2
        return "${formatHour(startHour)}–${formatHour(endHour)}"
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12 AM"
            hour < 12 -> "$hour AM"
            hour == 12 -> "12 PM"
            else -> "${hour - 12} PM"
        }
    }
}
