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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.focusapp.domain.model.SessionMode
import com.focusapp.domain.model.TimerStatus

@Composable
fun TimerScreen(
    onSessionCompleted: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TimerViewModel = hiltViewModel(),
) {
    val timerState by viewModel.timerState.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val customDuration by viewModel.customDurationMinutes.collectAsState()
    val customTag by viewModel.customTag.collectAsState()
    val showBreakPrompt by viewModel.showBreakPrompt.collectAsState()
    val showDistractionWarning by viewModel.showDistractionWarning.collectAsState()

    // Bind / unbind service with the composable lifecycle
    DisposableEffect(Unit) {
        viewModel.bindService()
        onDispose { viewModel.unbindService() }
    }

    // Navigate to session detail when a session just finished.
    // Capture the ID before onDismissBreakPrompt() resets timerState to IDLE
    // (currentSessionId would be null after the reset — C1 crash fix).
    val finishedSessionId = timerState.currentSessionId
    if (showBreakPrompt && finishedSessionId != null) {
        BreakPromptDialog(
            sessionId = finishedSessionId,
            onStartBreak = {
                viewModel.onDismissBreakPrompt()
                onSessionCompleted(finishedSessionId)
            },
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

        // ---- Distraction warning banner (slides in from top) ----
        DistractionWarningBanner(
            visible = showDistractionWarning,
            onDismiss = viewModel::onDismissDistractionWarning,
            modifier = Modifier.fillMaxWidth(),
        )

        // ---- Circular countdown ----
        CountdownDisplay(
            timerState = timerState,
            modifier = Modifier.size(220.dp),
        )

        // ---- Distraction badge (shown when distractions > 0 during active session) ----
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

        // ---- Mode selector (only when idle) ----
        AnimatedVisibility(visible = timerState.status == TimerStatus.IDLE) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = viewModel::onModeSelected,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (selectedMode == SessionMode.CUSTOM || selectedMode == SessionMode.STUDY) {
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomDurationPicker(
                        durationMinutes = customDuration,
                        tag = customTag,
                        mode = selectedMode,
                        onDurationChanged = viewModel::onCustomDurationChanged,
                        onTagChanged = viewModel::onTagChanged,
                    )
                }
            }
        }

        // ---- Controls ----
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

// ---- Break prompt dialog (T036) ----

@Composable
private fun BreakPromptDialog(
    sessionId: Long,
    onStartBreak: () -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Session Complete!") },
        text = { Text("Great work! Take a short break to recharge before your next session.") },
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
