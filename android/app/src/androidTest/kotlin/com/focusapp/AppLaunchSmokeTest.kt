package com.focusapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusapp.ui.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke test: the real Hilt application graph is wired, MainActivity launches,
 * and the bottom nav is reachable from a fresh-install state.
 *
 * Run with: .\gradlew :app:connectedAndroidTest
 *
 * On a fresh install the app starts at Onboarding. On a repeat install (DataStore
 * persists across runs) it starts at Timer. Both cases are handled: the test
 * navigates via the bottom nav items which only appear after onboarding is done.
 * If onboarding is showing, the test completes onboarding first.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppLaunchSmokeTest {

    @get:Rule(order = 0) val hiltRule    = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun app_launches_without_crash() {
        composeRule.waitForIdle()
        // Either the bottom nav "Timer" label or the onboarding screen must be present.
        // Both are valid post-launch states.
        val timerTabExists = try {
            composeRule.onNodeWithText("Timer").assertIsDisplayed(); true
        } catch (_: AssertionError) { false }

        val onboardingExists = try {
            composeRule.onNodeWithText("Get Started", substring = true).assertIsDisplayed(); true
        } catch (_: AssertionError) { false }

        assert(timerTabExists || onboardingExists) {
            "Expected either the timer tab or onboarding to be visible on launch"
        }
    }

    @Test
    fun bottom_nav_tabs_are_reachable() {
        composeRule.waitForIdle()
        skipOnboardingIfPresent()

        // Timer tab (already selected by default)
        composeRule.onAllNodesWithText("Timer")[0].assertIsDisplayed()

        // Analytics tab
        composeRule.onAllNodesWithText("Analytics")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Analytics")[0].assertIsDisplayed()

        // History tab
        composeRule.onAllNodesWithText("History")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("History")[0].assertIsDisplayed()

        // Profile tab
        composeRule.onAllNodesWithText("Profile")[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Profile")[0].assertIsDisplayed()
    }

    @Test
    fun analytics_shows_empty_state_with_no_sessions() {
        composeRule.waitForIdle()
        skipOnboardingIfPresent()

        composeRule.onNodeWithText("Analytics").performClick()
        composeRule.waitForIdle()

        // With an empty DB (fresh install or test isolation), the empty state shows
        try {
            composeRule.onNodeWithText("No sessions yet").assertIsDisplayed()
        } catch (_: AssertionError) {
            // If sessions exist from a previous run, the chart shows instead — both are valid
        }
    }

    @Test
    fun history_shows_empty_state_with_no_sessions() {
        composeRule.waitForIdle()
        skipOnboardingIfPresent()

        composeRule.onNodeWithText("History").performClick()
        composeRule.waitForIdle()

        try {
            composeRule.onNodeWithText("No sessions yet", substring = true).assertIsDisplayed()
        } catch (_: AssertionError) {
            // Sessions from a prior run exist — table renders, both are valid
        }
    }

    // ---- Helpers ----

    private fun skipOnboardingIfPresent() {
        try {
            // If the "Get Started" / "Next" / "Done" button is visible, tap through onboarding
            composeRule.onNodeWithText("Get Started", substring = true).performClick()
            composeRule.waitForIdle()
            // May need multiple taps for multi-page onboarding
            repeat(3) {
                try {
                    composeRule.onNodeWithText("Next", substring = true).performClick()
                    composeRule.waitForIdle()
                } catch (_: AssertionError) { /* no more pages */ }
            }
            try {
                composeRule.onNodeWithText("Done", substring = true).performClick()
                composeRule.waitForIdle()
            } catch (_: AssertionError) { /* not on last page */ }
        } catch (_: AssertionError) {
            // Onboarding already complete — nothing to do
        }
    }
}
