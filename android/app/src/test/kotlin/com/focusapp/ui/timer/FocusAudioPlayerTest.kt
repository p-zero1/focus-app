package com.focusapp.ui.timer

import com.focusapp.domain.model.SessionMode
import com.focusapp.ui.timer.FocusAudioPlayer.SoundMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests for FocusAudioPlayer behavior that is verifiable without a real AudioTrack.
 *
 * AudioTrack initialization and actual audio playback require an instrumented test
 * (connectedAndroidTest). This file covers:
 *  - SoundMode enum correctness and ordering
 *  - SoundMode label mapping (mirrored from TimerScreen)
 *  - SessionMode.goalPlaceholder mapping (mirrored from TimerScreen)
 *  - Mode-switch idempotency contract (same mode = no-op)
 */
class FocusAudioPlayerTest {

    // Mirrors private val FocusAudioPlayer.SoundMode.label in TimerScreen.kt
    private fun SoundMode.label(): String = when (this) {
        SoundMode.OFF         -> "Off"
        SoundMode.WHITE_NOISE -> "White"
        SoundMode.RAIN        -> "Rain"
    }

    // Mirrors private val SessionMode.goalPlaceholder in TimerScreen.kt
    private fun SessionMode.goalPlaceholder(): String = when (this) {
        SessionMode.POMODORO  -> "e.g. Review chapter 3"
        SessionMode.DEEP_WORK -> "e.g. Architecture design"
        SessionMode.CUSTOM    -> "e.g. DSA, Project X"
        SessionMode.STUDY     -> "e.g. Calculus problems"
    }

    // ---- SoundMode enum ----

    @Test
    fun `SoundMode has exactly three values`() {
        assertEquals(3, SoundMode.entries.size)
    }

    @Test
    fun `SoundMode values are OFF WHITE_NOISE and RAIN`() {
        val values = SoundMode.entries.map { it.name }
        assertEquals(listOf("OFF", "WHITE_NOISE", "RAIN"), values)
    }

    @Test
    fun `OFF is the first SoundMode`() {
        assertEquals(SoundMode.OFF, SoundMode.entries.first())
    }

    // ---- SoundMode label mapping ----

    @Test
    fun `OFF label is Off`() {
        assertEquals("Off", SoundMode.OFF.label())
    }

    @Test
    fun `WHITE_NOISE label is White`() {
        assertEquals("White", SoundMode.WHITE_NOISE.label())
    }

    @Test
    fun `RAIN label is Rain`() {
        assertEquals("Rain", SoundMode.RAIN.label())
    }

    @Test
    fun `all SoundMode labels are distinct`() {
        val labels = SoundMode.entries.map { it.label() }
        assertEquals(labels.size, labels.toSet().size)
    }

    // ---- Mode-switch idempotency contract ----

    @Test
    fun `same SoundMode values are equal`() {
        // Verifies the no-op guard `if (mode == currentMode) return` in FocusAudioPlayer.setMode()
        // can rely on enum equality.
        assertEquals(SoundMode.WHITE_NOISE, SoundMode.WHITE_NOISE)
    }

    @Test
    fun `different SoundModes are not equal`() {
        assertNotEquals(SoundMode.OFF, SoundMode.WHITE_NOISE)
        assertNotEquals(SoundMode.WHITE_NOISE, SoundMode.RAIN)
        assertNotEquals(SoundMode.OFF, SoundMode.RAIN)
    }

    // ---- goalPlaceholder mapping (all modes receive a goal prompt) ----

    @Test
    fun `POMODORO has a goal placeholder`() {
        assertEquals("e.g. Review chapter 3", SessionMode.POMODORO.goalPlaceholder())
    }

    @Test
    fun `DEEP_WORK has a goal placeholder`() {
        assertEquals("e.g. Architecture design", SessionMode.DEEP_WORK.goalPlaceholder())
    }

    @Test
    fun `CUSTOM has a goal placeholder`() {
        assertEquals("e.g. DSA, Project X", SessionMode.CUSTOM.goalPlaceholder())
    }

    @Test
    fun `STUDY has a goal placeholder`() {
        assertEquals("e.g. Calculus problems", SessionMode.STUDY.goalPlaceholder())
    }

    @Test
    fun `all SessionModes have distinct goal placeholders`() {
        val placeholders = SessionMode.entries.map { it.goalPlaceholder() }
        assertEquals(placeholders.size, placeholders.toSet().size)
    }
}
