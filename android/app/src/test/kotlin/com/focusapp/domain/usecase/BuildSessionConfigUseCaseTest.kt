package com.focusapp.domain.usecase

import com.focusapp.domain.model.SessionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class BuildSessionConfigUseCaseTest {

    private lateinit var useCase: BuildSessionConfigUseCase

    @Before
    fun setUp() {
        useCase = BuildSessionConfigUseCase()
    }

    @Test
    fun `POMODORO uses caller-supplied durationSeconds`() {
        val config = useCase(SessionMode.POMODORO, durationSeconds = 1500, tag = null)
        assertEquals(1500, config.durationSeconds)
        assertEquals(SessionMode.POMODORO, config.mode)
    }

    @Test
    fun `DEEP_WORK uses caller-supplied durationSeconds`() {
        val config = useCase(SessionMode.DEEP_WORK, durationSeconds = 5400, tag = null)
        assertEquals(5400, config.durationSeconds)
        assertEquals(SessionMode.DEEP_WORK, config.mode)
    }

    @Test
    fun `CUSTOM uses caller-supplied duration`() {
        val config = useCase(SessionMode.CUSTOM, durationSeconds = 2700, tag = null)
        assertEquals(2700, config.durationSeconds)
        assertEquals(SessionMode.CUSTOM, config.mode)
    }

    @Test
    fun `STUDY uses caller-supplied duration`() {
        val config = useCase(SessionMode.STUDY, durationSeconds = 5400, tag = null)
        assertEquals(5400, config.durationSeconds)
        assertEquals(SessionMode.STUDY, config.mode)
    }

    @Test
    fun `tag is passed through to SessionConfig`() {
        val config = useCase(SessionMode.STUDY, durationSeconds = 3600, tag = "DSA")
        assertEquals("DSA", config.tag)
    }

    @Test
    fun `null tag is preserved`() {
        val config = useCase(SessionMode.POMODORO, durationSeconds = 1500, tag = null)
        assertNull(config.tag)
    }

    @Test
    fun `duration below MIN is clamped to MIN_DURATION_SECONDS`() {
        val config = useCase(SessionMode.CUSTOM, durationSeconds = 60, tag = null)
        assertEquals(BuildSessionConfigUseCase.MIN_DURATION_SECONDS, config.durationSeconds)
    }

    @Test
    fun `duration above MAX is clamped to MAX_DURATION_SECONDS`() {
        val config = useCase(SessionMode.CUSTOM, durationSeconds = 99999, tag = null)
        assertEquals(BuildSessionConfigUseCase.MAX_DURATION_SECONDS, config.durationSeconds)
    }

    @Test
    fun `zero duration is clamped to MIN_DURATION_SECONDS`() {
        val config = useCase(SessionMode.STUDY, durationSeconds = 0, tag = "test")
        assertEquals(BuildSessionConfigUseCase.MIN_DURATION_SECONDS, config.durationSeconds)
    }

    @Test
    fun `exact MIN duration passes through unchanged`() {
        val config = useCase(SessionMode.POMODORO, durationSeconds = BuildSessionConfigUseCase.MIN_DURATION_SECONDS, tag = null)
        assertEquals(BuildSessionConfigUseCase.MIN_DURATION_SECONDS, config.durationSeconds)
    }

    @Test
    fun `exact MAX duration passes through unchanged`() {
        val config = useCase(SessionMode.DEEP_WORK, durationSeconds = BuildSessionConfigUseCase.MAX_DURATION_SECONDS, tag = null)
        assertEquals(BuildSessionConfigUseCase.MAX_DURATION_SECONDS, config.durationSeconds)
    }
}
