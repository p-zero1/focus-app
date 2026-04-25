package com.focusapp.domain.usecase

import com.focusapp.domain.model.FocusSession
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class GetBestFocusTimeUseCaseTest {

    private val useCase = GetBestFocusTimeUseCase()

    @Test
    fun `returns null when fewer than 5 sessions`() {
        val sessions = buildSessions(count = 4, hour = 10, distractionCount = 0)
        assertNull(useCase(sessions))
    }

    @Test
    fun `returns null for exactly 4 sessions`() {
        assertNull(useCase(buildSessions(4, 10, 0)))
    }

    @Test
    fun `returns slot label for exactly 5 sessions at same hour`() {
        val sessions = buildSessions(count = 5, hour = 10, distractionCount = 0)
        // hour 10 → slot 5 (10/2=5) → "10 AM–12 PM"
        assertEquals("10 AM–12 PM", useCase(sessions))
    }

    @Test
    fun `picks slot with lowest average distraction count`() {
        // 5 sessions at 8 AM with 3 distractions each
        val noisySlot = buildSessions(count = 5, hour = 8, distractionCount = 3)
        // 5 sessions at 14 (2 PM) with 0 distractions each
        val quietSlot = buildSessions(count = 5, hour = 14, distractionCount = 0)

        val result = useCase(noisySlot + quietSlot)
        // slot for 14h = 14/2 = 7 → "2 PM–4 PM"
        assertEquals("2 PM–4 PM", result)
    }

    @Test
    fun `midnight slot formats correctly`() {
        val sessions = buildSessions(count = 5, hour = 0, distractionCount = 0)
        assertEquals("12 AM–2 AM", useCase(sessions))
    }

    @Test
    fun `noon slot formats correctly`() {
        val sessions = buildSessions(count = 5, hour = 12, distractionCount = 0)
        assertEquals("12 PM–2 PM", useCase(sessions))
    }

    // ---- Helpers ----

    private fun buildSessions(count: Int, hour: Int, distractionCount: Int): List<FocusSession> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        return List(count) { index ->
            FocusSession(
                id = index.toLong(),
                startTime = startMs + index * 86_400_000L, // spread over different days
                endTime = startMs + index * 86_400_000L + 1_500_000L,
                plannedDuration = 1500,
                actualDuration = 1500,
                mode = SessionMode.POMODORO,
                tag = null,
                status = SessionStatus.COMPLETED,
                distractionCount = distractionCount,
                distractionTotalSeconds = distractionCount * 60,
                xpAwarded = 50,
                focusScore = 100,
            )
        }
    }
}
