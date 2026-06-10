package com.focusapp.ui.timer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val ITEM_HEIGHT = 48.dp
private const val VISIBLE_ITEMS = 5
private const val PAD = 2  // padding items at each end so first/last real value can be centered

/**
 * Drum-roll hour × minute wheel picker.
 *
 * Minute range adjusts dynamically based on the selected hour:
 * - hour == 0           → minutes start at minSeconds/60 (enforces lower bound)
 * - hour == maxHours    → minutes end at (maxSeconds%3600)/60 (enforces upper bound)
 * - otherwise           → full 0–59 minute range
 *
 * [key(selectedHour)] causes the minute column to be recreated whenever the hour
 * changes, ensuring the minute scroll state reflects the new valid range.
 */
@Composable
fun TimerWheelPicker(
    durationSeconds: Int,
    minSeconds: Int,
    maxSeconds: Int,
    onDurationChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxHours = maxSeconds / 3600
    val initHours = (durationSeconds / 3600).coerceIn(0, maxHours)
    val initMinutes = (durationSeconds % 3600) / 60

    val hourItems: List<Int?> = List(PAD) { null } + (0..maxHours).toList() + List(PAD) { null }
    val hoursState = rememberLazyListState(initialFirstVisibleItemIndex = initHours)
    val selectedHour = remember { derivedStateOf { hoursState.firstVisibleItemIndex.coerceIn(0, maxHours) } }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WheelColumn(
            items = hourItems,
            state = hoursState,
            label = { if (it == 1) "1 h" else "$it h" },
            itemWidth = 72.dp,
        )
        Text(
            text = ":",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        // Recreate the minute column whenever the hour changes so its item range is correct.
        key(selectedHour.value) {
            val hourNow = selectedHour.value
            val startMin = if (hourNow == 0) (minSeconds / 60).coerceIn(0, 59) else 0
            val endMin = if (hourNow >= maxHours) ((maxSeconds % 3600) / 60).coerceIn(0, 59) else 59
            val initMin = if (hourNow == initHours) initMinutes.coerceIn(startMin, endMin) else startMin

            val minuteItems: List<Int?> = List(PAD) { null } + (startMin..endMin).toList() + List(PAD) { null }
            val minutesState = rememberLazyListState(
                initialFirstVisibleItemIndex = (initMin - startMin).coerceAtLeast(0),
            )
            // selectedMinute = startMin + scroll offset, since minute items begin at startMin
            val selectedMinute = remember {
                derivedStateOf { (startMin + minutesState.firstVisibleItemIndex).coerceIn(startMin, endMin) }
            }

            LaunchedEffect(hourNow, selectedMinute.value) {
                val raw = hourNow * 3600 + selectedMinute.value * 60
                onDurationChanged(raw.coerceIn(minSeconds, maxSeconds))
            }

            WheelColumn(
                items = minuteItems,
                state = minutesState,
                label = { "%02d m".format(it) },
                itemWidth = 88.dp,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<Int?>,
    state: LazyListState,
    label: (Int) -> String,
    itemWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val selectedIndex = remember { derivedStateOf { state.firstVisibleItemIndex + PAD } }

    Box(modifier = modifier.height(ITEM_HEIGHT * VISIBLE_ITEMS).width(itemWidth)) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.matchParentSize(),
        ) {
            items(items.size) { index ->
                val value = items[index]
                val isSelected = index == selectedIndex.value
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.height(ITEM_HEIGHT).width(itemWidth),
                ) {
                    if (value != null) {
                        Text(
                            text = label(value),
                            style = if (isSelected) MaterialTheme.typography.titleLarge
                                    else MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        // Horizontal lines framing the selected (center) row
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = ITEM_HEIGHT * PAD),
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = ITEM_HEIGHT * (PAD + 1)),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
