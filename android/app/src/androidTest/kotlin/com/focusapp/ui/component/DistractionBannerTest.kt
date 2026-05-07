package com.focusapp.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusapp.ui.FocusAppTheme
import com.focusapp.ui.timer.DistractionWarningBanner
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DistractionBannerTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun banner_hidden_when_visible_false() {
        composeRule.setContent {
            FocusAppTheme {
                DistractionWarningBanner(
                    visible = false,
                    onDismiss = {},
                )
            }
        }
        // Content is never composed when visible starts as false
        composeRule.onNodeWithText("stay focused", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("stay on track", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("broke focus", substring = true).assertDoesNotExist()
    }

    @Test fun banner_shows_app_name_text() {
        composeRule.setContent {
            FocusAppTheme {
                DistractionWarningBanner(
                    visible = true,
                    onDismiss = {},
                    appName = "Instagram",
                )
            }
        }
        composeRule.onNodeWithText("Switched to Instagram — stay focused!", substring = false)
            .assertIsDisplayed()
    }

    @Test fun banner_shows_away_seconds_when_no_app_name() {
        composeRule.setContent {
            FocusAppTheme {
                DistractionWarningBanner(
                    visible = true,
                    onDismiss = {},
                    awaySeconds = 45,
                )
            }
        }
        composeRule.onNodeWithText("You were away for 45s — stay on track!", substring = false)
            .assertIsDisplayed()
    }

    @Test fun banner_shows_fallback_text_when_no_context() {
        composeRule.setContent {
            FocusAppTheme {
                DistractionWarningBanner(
                    visible = true,
                    onDismiss = {},
                    awaySeconds = 0,
                    appName = null,
                )
            }
        }
        composeRule.onNodeWithText("You broke focus!", substring = true).assertIsDisplayed()
    }

    @Test fun banner_dismiss_invokes_callback() {
        var dismissed = false
        composeRule.setContent {
            FocusAppTheme {
                DistractionWarningBanner(
                    visible = true,
                    onDismiss = { dismissed = true },
                    appName = "YouTube",
                )
            }
        }
        composeRule.onNodeWithText("Switched to YouTube", substring = true).performClick()
        assertTrue(dismissed)
    }
}
