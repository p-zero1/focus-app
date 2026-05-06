package com.focusapp.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimerStateNewFieldTest {

    @Test
    fun `lastDistractionAppName defaults to null on IDLE`() {
        assertNull(TimerState.IDLE.lastDistractionAppName)
    }

    @Test
    fun `lastDistractionAppName defaults to null on fresh instance`() {
        assertNull(TimerState().lastDistractionAppName)
    }

    @Test
    fun `lastDistractionAppName can be set via copy`() {
        val state = TimerState.IDLE.copy(lastDistractionAppName = "Instagram")
        assertEquals("Instagram", state.lastDistractionAppName)
    }

    @Test
    fun `lastDistractionAppName is preserved when other fields change`() {
        val state = TimerState.IDLE.copy(lastDistractionAppName = "WhatsApp", distractionCount = 1)
        val updated = state.copy(distractionCount = 2)
        assertEquals("WhatsApp", updated.lastDistractionAppName)
    }

    @Test
    fun `copy with null clears lastDistractionAppName`() {
        val state = TimerState.IDLE.copy(lastDistractionAppName = "YouTube")
        val cleared = state.copy(lastDistractionAppName = null)
        assertNull(cleared.lastDistractionAppName)
    }
}
