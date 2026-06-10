package com.focusapp.data.prefs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for [AppPreferences] DataStore reads and writes.
 *
 * Runs against a real DataStore instance on the device (connectedAndroidTest),
 * satisfying Constitution Principle V: "Integration tests (Room, DataStore) MUST
 * run via connectedAndroidTest against a real SQLite instance."
 *
 * Uses the instrumentation targetContext so each test class gets its own DataStore
 * file scoped to the test package, isolated from the main app's DataStore.
 */
@RunWith(AndroidJUnit4::class)
class AppPreferencesTest {

    private lateinit var prefs: AppPreferences

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = AppPreferences(context)
    }

    @Test
    fun pomodoroFocusMinutes_defaultIs25() = runTest {
        val value = prefs.pomodoroFocusMinutes.first()
        assertEquals(25, value)
    }

    @Test
    fun setPomodoroFocusMinutes_roundTrips() = runTest {
        prefs.setPomodoroFocusMinutes(45)
        assertEquals(45, prefs.pomodoroFocusMinutes.first())
    }

    @Test
    fun onboardingComplete_defaultIsFalse() = runTest {
        assertFalse(prefs.onboardingComplete.first())
    }

    @Test
    fun setOnboardingComplete_roundTrips() = runTest {
        prefs.setOnboardingComplete(true)
        assertTrue(prefs.onboardingComplete.first())
        // Reset to false for other tests
        prefs.setOnboardingComplete(false)
    }

    @Test
    fun usageStatsPermissionAsked_defaultIsFalse() = runTest {
        assertFalse(prefs.usageStatsPermissionAsked.first())
    }

    @Test
    fun setUsageStatsPermissionAsked_roundTrips() = runTest {
        prefs.setUsageStatsPermissionAsked(true)
        assertTrue(prefs.usageStatsPermissionAsked.first())
        prefs.setUsageStatsPermissionAsked(false)
    }

    @Test
    fun theme_defaultIsDefault() = runTest {
        assertEquals("DEFAULT", prefs.theme.first())
    }

    @Test
    fun setTheme_roundTrips() = runTest {
        prefs.setTheme("DARK")
        assertEquals("DARK", prefs.theme.first())
        prefs.setTheme("DEFAULT")
    }
}
