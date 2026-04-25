package com.focusapp.ui.timer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.focusapp.domain.model.SessionMode

/**
 * Shown when mode is CUSTOM or STUDY. Lets the user choose a duration (5–180 min)
 * via a slider, and optionally enter a session tag.
 */
@Composable
fun CustomDurationPicker(
    durationMinutes: Int,
    tag: String,
    mode: SessionMode,
    onDurationChanged: (Int) -> Unit,
    onTagChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Duration:", modifier = Modifier.width(80.dp))
            Slider(
                value = durationMinutes.toFloat(),
                onValueChange = { onDurationChanged(it.toInt()) },
                valueRange = 5f..180f,
                steps = 34,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Duration slider: $durationMinutes minutes" },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "$durationMinutes min", modifier = Modifier.width(56.dp))
        }

        if (mode == SessionMode.STUDY || mode == SessionMode.CUSTOM) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = tag,
                onValueChange = onTagChanged,
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
