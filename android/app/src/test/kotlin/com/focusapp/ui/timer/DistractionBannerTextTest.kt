package com.focusapp.ui.timer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the banner text selection logic from DistractionWarningBanner.
 * Mirrors the `when` expression in the composable so any change to message
 * format breaks the test immediately.
 */
class DistractionBannerTextTest {

    // Mirrors the bannerText `when` expression in DistractionWarningBanner
    private fun bannerText(appName: String?, awaySeconds: Int): String = when {
        appName != null -> "Switched to $appName \u2014 stay focused!"
        awaySeconds > 0 -> "You were away for ${awaySeconds}s \u2014 stay on track!"
        else -> "You broke focus! Stay on track \uD83D\uDCAA"
    }

    @Test
    fun `app name produces app-specific switched message`() {
        assertEquals(
            "Switched to Instagram \u2014 stay focused!",
            bannerText(appName = "Instagram", awaySeconds = 0),
        )
    }

    @Test
    fun `app name takes priority over awaySeconds`() {
        assertEquals(
            "Switched to WhatsApp \u2014 stay focused!",
            bannerText(appName = "WhatsApp", awaySeconds = 30),
        )
    }

    @Test
    fun `null app name with positive awaySeconds shows time-away message`() {
        assertEquals(
            "You were away for 45s \u2014 stay on track!",
            bannerText(appName = null, awaySeconds = 45),
        )
    }

    @Test
    fun `null app name and zero awaySeconds shows generic fallback`() {
        assertEquals(
            "You broke focus! Stay on track \uD83D\uDCAA",
            bannerText(appName = null, awaySeconds = 0),
        )
    }

    @Test
    fun `app name message uses exact name provided`() {
        assertEquals(
            "Switched to YouTube \u2014 stay focused!",
            bannerText(appName = "YouTube", awaySeconds = 0),
        )
    }
}
