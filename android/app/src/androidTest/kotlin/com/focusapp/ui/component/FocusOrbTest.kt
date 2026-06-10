package com.focusapp.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.focusapp.domain.model.TimerStatus
import com.focusapp.ui.FocusAppTheme
import com.focusapp.ui.timer.FocusOrb
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke tests: FocusOrb renders without crashing in every TimerStatus × distraction combination.
 * The orb is pure Canvas — we assert the root node exists (i.e. no exception was thrown).
 */
@RunWith(AndroidJUnit4::class)
class FocusOrbTest {

    @get:Rule val composeRule = createComposeRule()

    private fun orbOf(status: TimerStatus, isDistracting: Boolean = false) {
        composeRule.setContent {
            FocusAppTheme {
                FocusOrb(
                    timerStatus = status,
                    isDistracting = isDistracting,
                    modifier = Modifier.size(200.dp),
                )
            }
        }
        composeRule.onRoot().fetchSemanticsNode() // throws if composition failed
    }

    @Test fun orb_renders_idle()    = orbOf(TimerStatus.IDLE)
    @Test fun orb_renders_active()  = orbOf(TimerStatus.ACTIVE)
    @Test fun orb_renders_paused()  = orbOf(TimerStatus.PAUSED)
    @Test fun orb_renders_break()   = orbOf(TimerStatus.BREAK)
    @Test fun orb_renders_finished()= orbOf(TimerStatus.FINISHED)

    @Test fun orb_renders_distraction_active() = orbOf(TimerStatus.ACTIVE, isDistracting = true)
    @Test fun orb_renders_distraction_idle()   = orbOf(TimerStatus.IDLE,   isDistracting = true)
}
