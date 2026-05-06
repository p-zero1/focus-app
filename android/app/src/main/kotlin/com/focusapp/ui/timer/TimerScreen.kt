package com.focusapp.ui.timer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusapp.domain.model.FocusStrictness
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.SessionOutcome
import com.focusapp.domain.model.TimerStatus
import com.focusapp.ui.profile.BadgeAwardedDialog

@Composable
fun TimerScreen(
    onSessionCompleted: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val timerState by viewModel.timerState.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val selectedStrictness by viewModel.selectedStrictness.collectAsState()
    val customDurationSeconds by viewModel.customDurationSeconds.collectAsState()
    val customTag by viewModel.customTag.collectAsState()
    val showBreakPrompt by viewModel.showBreakPrompt.collectAsState()
    val showDistractionWarning by viewModel.showDistractionWarning.collectAsState()
    val distractionAwaySeconds by viewModel.distractionAwaySeconds.collectAsState()
    val distractionAppName by viewModel.distractionAppName.collectAsState()
    val newBadge by viewModel.newBadge.collectAsState()

    DisposableEffect(Unit) {
        viewModel.bindService()
        onDispose { viewModel.unbindService() }
    }

    newBadge?.let { badge ->
        BadgeAwardedDialog(badge = badge, onDismiss = viewModel::onDismissBadge)
    }

    val finishedSessionId = timerState.currentSessionId
    if (showBreakPrompt && finishedSessionId != null) {
        BreakPromptDialog(
            outcome = timerState.sessionOutcome,
            onStartBreak = { viewModel.onStartBreak() },
            onSkip = {
                viewModel.onDismissBreakPrompt()
                onSessionCompleted(finishedSessionId)
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        DistractionWarningBanner(
            visible = showDistractionWarning,
            onDismiss = viewModel::onDismissDistractionWarning,
            awaySeconds = distractionAwaySeconds,
            appName = distractionAppName,
            modifier = Modifier.fillMaxWidth(),
        )

        CountdownDisplay(
            timerState = timerState,
            modifier = Modifier.size(220.dp),
        )

        AnimatedVisibility(
            visible = timerState.distractionCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${timerState.distractionCount} distraction${if (timerState.distractionCount != 1) "s" else ""}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        // Mode selector + duration picker — only while idle
        AnimatedVisibility(visible = timerState.status == TimerStatus.IDLE) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = viewModel::onModeSelected,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                StrictnessSelector(
                    selected = selectedStrictness,
                    onSelected = viewModel::onStrictnessSelected,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = selectedMode.pickerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(4.dp))

                // key(selectedMode) resets the LazyListState when mode changes, snapping back to default
                key(selectedMode) {
                    TimerWheelPicker(
                        durationSeconds = customDurationSeconds,
                        minSeconds = selectedMode.minSeconds,
                        maxSeconds = selectedMode.maxSeconds,
                        onDurationChanged = viewModel::onCustomDurationSecondsChanged,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Tag field only for CUSTOM and STUDY
                if (selectedMode == SessionMode.CUSTOM || selectedMode == SessionMode.STUDY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customTag,
                        onValueChange = viewModel::onTagChanged,
                        label = { Text("Tag (optional)") },
                        placeholder = { Text("e.g. DSA, Project X") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    )
                }
            }
        }

        TimerControls(
            status = timerState.status,
            onStart = viewModel::onStartSession,
            onPause = viewModel::onPause,
            onResume = viewModel::onResume,
            onStop = viewModel::onStop,
            onSkipBreak = viewModel::onSkipBreak,
        )
    }
}

// ---- Circular countdown composable ----

@Composable
private fun CountdownDisplay(
    timerState: com.focusapp.domain.model.TimerState,
    modifier: Modifier = Modifier,
) {
    val progress = if (timerState.remainingSeconds > 0 && timerState.status != TimerStatus.IDLE) {
        val total = timerState.remainingSeconds + timerState.elapsedSeconds
        if (total > 0) timerState.remainingSeconds.toFloat() / total else 1f
    } else {
        1f
    }

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 8.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatSeconds(timerState.remainingSeconds),
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics {
                    contentDescription = "Remaining time: ${formatSeconds(timerState.remainingSeconds)}"
                },
            )
            Text(
                text = timerState.status.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val TimerStatus.displayName: String
    get() = when (this) {
        TimerStatus.IDLE -> "Ready"
        TimerStatus.ACTIVE -> "Focusing"
        TimerStatus.PAUSED -> "Paused"
        TimerStatus.BREAK -> "Break"
        TimerStatus.FINISHED -> "Done!"
    }

private val SessionMode.pickerLabel: String
    get() = when (this) {
        SessionMode.POMODORO -> "Focus interval (5 – 90 min)"
        SessionMode.DEEP_WORK -> "Deep work duration (30 min – 3 h)"
        SessionMode.CUSTOM -> "Session duration"
        SessionMode.STUDY -> "Study duration"
    }

// ---- Controls row ----

@Composable
private fun TimerControls(
    status: TimerStatus,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkipBreak: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (status) {
            TimerStatus.IDLE, TimerStatus.FINISHED -> {
                Button(
                    onClick = onStart,
                    modifier = Modifier.semantics { contentDescription = "Start session" },
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Start")
                }
            }
            TimerStatus.ACTIVE -> {
                FilledTonalButton(
                    onClick = onPause,
                    modifier = Modifier.semantics { contentDescription = "Pause session" },
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pause")
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.semantics { contentDescription = "Stop session" },
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
            TimerStatus.PAUSED -> {
                Button(
                    onClick = onResume,
                    modifier = Modifier.semantics { contentDescription = "Resume session" },
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Resume")
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.semantics { contentDescription = "Stop session" },
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop")
                }
            }
            TimerStatus.BREAK -> {
                OutlinedButton(
                    onClick = onSkipBreak,
                    modifier = Modifier.semantics { contentDescription = "Skip break" },
                ) {
                    Text("Skip Break")
                }
            }
        }
    }
}

// ---- Strictness selector ----

@Composable
private fun StrictnessSelector(
    selected: FocusStrictness,
    onSelected: (FocusStrictness) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Focus mode",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocusStrictness.entries.forEach { strictness ->
                androidx.compose.material3.FilterChip(
                    selected = strictness == selected,
                    onClick = { onSelected(strictness) },
                    label = { Text(strictness.label, maxLines = 1) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = selected.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private val FocusStrictness.label: String
    get() = when (this) {
        FocusStrictness.RELAXED  -> "Relaxed"
        FocusStrictness.STRICT   -> "Strict"
        FocusStrictness.HARDCORE -> "Hardcore"
    }

private val FocusStrictness.description: String
    get() = when (this) {
        FocusStrictness.RELAXED  -> "Distractions are logged only"
        FocusStrictness.STRICT   -> "Session pauses on distraction"
        FocusStrictness.HARDCORE -> "Session ends on first distraction"
    }

// ---- Break prompt dialog ----

@Composable
private fun BreakPromptDialog(
    outcome: SessionOutcome?,
    onStartBreak: () -> Unit,
    onSkip: () -> Unit,
) {
    val title = when (outcome) {
        SessionOutcome.CLEAN       -> "Clean session! 🎯"
        SessionOutcome.INTERRUPTED -> "Session complete"
        SessionOutcome.FAILED      -> "Session complete"
        null                       -> "Session Complete!"
    }
    val message = when (outcome) {
        SessionOutcome.CLEAN       -> "No distractions — excellent focus! Take a break to recharge."
        SessionOutcome.INTERRUPTED -> "You had a few distractions. Take a break and aim for clean next time!"
        SessionOutcome.FAILED      -> "Lots of distractions this time. Rest up and try again!"
        null                       -> "Great work! Take a short break to recharge before your next session."
    }
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onStartBreak) { Text("Take a Break") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Skip Break") }
        },
    )
}

private fun formatSeconds(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
