package com.focusapp.ui.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusapp.ui.FocusAppTheme
import com.focusapp.ui.profile.StreakCounter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StreakCounterTest {

    @get:Rule val composeRule = createComposeRule()

    @Test fun shows_streak_number() {
        composeRule.setContent { FocusAppTheme { StreakCounter(streak = 5) } }
        composeRule.onNodeWithText("5").assertIsDisplayed()
    }

    @Test fun shows_day_streak_label() {
        composeRule.setContent { FocusAppTheme { StreakCounter(streak = 5) } }
        composeRule.onNodeWithText("day streak").assertIsDisplayed()
    }

    @Test fun accessibility_description_includes_streak_count() {
        composeRule.setContent { FocusAppTheme { StreakCounter(streak = 12) } }
        composeRule.onNodeWithContentDescription("12 day streak").assertIsDisplayed()
    }

    @Test fun zero_streak_renders() {
        composeRule.setContent { FocusAppTheme { StreakCounter(streak = 0) } }
        composeRule.onNodeWithText("0").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("0 day streak").assertIsDisplayed()
    }

    @Test fun large_streak_renders() {
        composeRule.setContent { FocusAppTheme { StreakCounter(streak = 100) } }
        composeRule.onNodeWithText("100").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("100 day streak").assertIsDisplayed()
    }
}
