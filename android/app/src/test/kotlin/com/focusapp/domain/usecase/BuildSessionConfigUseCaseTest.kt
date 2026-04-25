package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.preferences.FocusPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BuildSessionConfigUseCaseTest {

    private lateinit var useCase: BuildSessionConfigUseCase

    @Before
    fun setUp() {
        useCase = BuildSessionConfigUseCase(FakeFocusPreferences(pomodoroFocusMinutes = 25))
    }

    @Test
    fun `POMODORO uses pomodoroFocusMinutes from prefs`() = runTest {
        val config = useCase(SessionMode.POMODORO, customDurationMinutes = 0, tag = null)
        assertEquals(25 * 60, config.durationSeconds)
        assertEquals(SessionMode.POMODORO, config.mode)
    }

    @Test
    fun `DEEP_WORK is double pomodoroFocusMinutes`() = runTest {
        val config = useCase(SessionMode.DEEP_WORK, customDurationMinutes = 0, tag = null)
        assertEquals(25 * 60 * 2, config.durationSeconds)
        assertEquals(SessionMode.DEEP_WORK, config.mode)
    }

    @Test
    fun `CUSTOM uses caller-supplied duration`() = runTest {
        val config = useCase(SessionMode.CUSTOM, customDurationMinutes = 45, tag = null)
        assertEquals(45 * 60, config.durationSeconds)
        assertEquals(SessionMode.CUSTOM, config.mode)
    }

    @Test
    fun `STUDY uses caller-supplied duration`() = runTest {
        val config = useCase(SessionMode.STUDY, customDurationMinutes = 90, tag = null)
        assertEquals(90 * 60, config.durationSeconds)
        assertEquals(SessionMode.STUDY, config.mode)
    }

    @Test
    fun `tag is passed through to SessionConfig`() = runTest {
        val config = useCase(SessionMode.STUDY, customDurationMinutes = 60, tag = "DSA")
        assertEquals("DSA", config.tag)
    }

    @Test
    fun `null tag is preserved`() = runTest {
        val config = useCase(SessionMode.POMODORO, customDurationMinutes = 0, tag = null)
        assertNull(config.tag)
    }

    @Test
    fun `POMODORO duration reflects custom prefs value`() = runTest {
        val customPrefs = FakeFocusPreferences(pomodoroFocusMinutes = 50)
        val result = BuildSessionConfigUseCase(customPrefs)
            .invoke(SessionMode.POMODORO, customDurationMinutes = 0, tag = null)
        assertEquals(50 * 60, result.durationSeconds)
    }

    @Test
    fun `CUSTOM duration below 5 min is clamped to 5 min`() = runTest {
        val config = useCase(SessionMode.CUSTOM, customDurationMinutes = 1, tag = null)
        assertEquals(BuildSessionConfigUseCase.MIN_CUSTOM_MINUTES * 60, config.durationSeconds)
    }

    @Test
    fun `CUSTOM duration above 180 min is clamped to 180 min`() = runTest {
        val config = useCase(SessionMode.CUSTOM, customDurationMinutes = 999, tag = null)
        assertEquals(BuildSessionConfigUseCase.MAX_CUSTOM_MINUTES * 60, config.durationSeconds)
    }

    @Test
    fun `STUDY duration of 0 is clamped to 5 min`() = runTest {
        val config = useCase(SessionMode.STUDY, customDurationMinutes = 0, tag = "test")
        assertEquals(BuildSessionConfigUseCase.MIN_CUSTOM_MINUTES * 60, config.durationSeconds)
    }
}

// ---- Fake ----

private class FakeFocusPreferences(
    private val pomodoroFocusMinutes: Int = 25,
) : FocusPreferences {
    override val pomodoroFocusMinutes: Flow<Int> = flowOf(pomodoroFocusMinutes)
    override val pomodoroShortBreakMinutes: Flow<Int> = flowOf(5)
    override val pomodoroLongBreakMinutes: Flow<Int> = flowOf(15)
    override val pomodoroIntervalsBeforeLong: Flow<Int> = flowOf(4)
    override val usageStatsPermissionAsked: Flow<Boolean> = flowOf(false)
    override val notificationPermissionAsked: Flow<Boolean> = flowOf(false)
    override val onboardingComplete: Flow<Boolean> = flowOf(false)
    override val theme: Flow<String> = flowOf("system")
}
