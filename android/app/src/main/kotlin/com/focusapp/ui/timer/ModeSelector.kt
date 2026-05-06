package com.focusapp.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.focusapp.domain.model.SessionMode

@Composable
fun ModeSelector(
    selectedMode: SessionMode,
    onModeSelected: (SessionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SessionMode.entries.forEach { mode ->
            FilterChip(
                selected = mode == selectedMode,
                onClick = { onModeSelected(mode) },
                label = { Text(mode.label, maxLines = 1) },
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "${mode.label} mode${if (mode == selectedMode) ", selected" else ""}"
                    },
            )
        }
    }
}

private val SessionMode.label: String
    get() = when (this) {
        SessionMode.POMODORO -> "Pomodoro"
        SessionMode.CUSTOM -> "Custom"
        SessionMode.STUDY -> "Study"
        SessionMode.DEEP_WORK -> "Deep Work"
    }
