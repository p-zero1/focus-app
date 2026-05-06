# Implementation Plan: Focus App — V1 Polish & Duration Selection

**Branch**: `001-focus-behavior-tracker` | **Date**: 2026-04-28 | **Spec**: [spec.md](spec.md)
**Input**: Four defects + two UX enhancements discovered during live device testing.

## Summary

Six targeted improvements to V1 from real device testing. Two are single-line bugs (chart
clipping, badge re-show). One is a data-layer fix (distraction name resolution). Three are
UX enhancements: replacing the Custom slider with a drum-roll wheel picker, and extending
that same picker to Pomodoro, Deep Work, and Study modes so every mode lets the user choose
their own duration. No new DB entities, no new permissions, no Room migrations.

## Technical Context

**Language/Version**: Kotlin 2.0 / JVM 17
**Primary Dependencies**: Jetpack Compose (BOM 2024.02.02), Material 3, Hilt 2.50, Room 2.6, DataStore 1.0
**Storage**: Room (existing); no schema changes
**Testing**: JUnit 4 + Robolectric; existing `BuildSessionConfigUseCaseTest` requires updates
**Target Platform**: Android minSdk 26 / targetSdk 35
**Project Type**: Single-module Android app under `android/`
**Performance Goals**: No new perf-critical paths
**Constraints**: Offline-first; no new permissions; no new DB migrations
**Scale/Scope**: 4 bugs + 2 UX changes across 7 existing files + 1 new composable

## Constitution Check

| Gate | Status | Notes |
|------|--------|-------|
| No unnecessary abstractions (§VI) | ✅ PASS | `TimerWheelPicker` has 4 call sites (one per mode) — justified new file |
| Permissions degraded gracefully (§IV) | ✅ PASS | PM label lookup has safe fallback; no new permissions |
| Offline-first (§III) | ✅ PASS | All changes are local; zero network calls |
| Single module (§I) | ✅ PASS | No new Gradle modules |
| Domain-first / no cross-layer imports (§II) | ✅ PASS | PM resolution in service layer (DistractionMonitor), not domain |
| V1 scope gate (§VII) | ✅ PASS | All changes are V1 scope |

## Project Structure — Files Touched

```text
android/app/src/main/kotlin/com/focusapp/
├── service/
│   ├── DistractionMonitor.kt          # Fix 3: resolve pkg name → display name via PackageManager
│   └── TimerService.kt                # Fix 2: add clearNewlyAwardedBadges(); Fix 3: thread appDisplayName
├── domain/
│   └── usecase/
│       └── BuildSessionConfigUseCase.kt  # Enhancement 2: all modes use caller-supplied durationSeconds
├── ui/
│   ├── analytics/
│   │   └── FocusBarChart.kt           # Fix 1: column height +28dp → +48dp
│   └── timer/
│       ├── TimerScreen.kt             # Enhancement 1+2: show picker for all modes; show tag only for Custom/Study
│       ├── TimerViewModel.kt          # Fix 2: clearNewlyAwardedBadges on dismiss; Enhancement 1+2: seconds-based duration + smart defaults per mode
│       └── TimerWheelPicker.kt        # New: drum-roll Hours × Minutes composable
```

---

## Phase 0: Research — Decisions

All technology decisions inherit from original research.md. New decisions for this plan:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| App name resolution | `PackageManager.getApplicationLabel()` in `DistractionMonitor`, cached in `HashMap` | Keeps domain layer pure; resolved once per unique package per service lifetime |
| Launcher name | Override to "Home Screen" when package contains "launcher" | Covers AOSP + all major OEM launchers reliably |
| Badge clear | `TimerService.clearNewlyAwardedBadges()` mutates service's own `_timerState` | Only the service owns its StateFlow; ViewModel must not write to it directly |
| Wheel picker | `LazyColumn` + `rememberSnapFlingBehavior` — no new dependencies | Already in compose-foundation BOM; no extra dependency |
| Picker columns | Hours (0–2) + Minutes (0–59) | Covers 0–179 min; seconds add complexity for negligible benefit |
| Duration unit | Seconds throughout (`durationSeconds: Int`) | Eliminates the `× 60` conversion scattered across ViewModel + UseCase |
| All-mode picker | Same `TimerWheelPicker` composable for all 4 modes with mode-specific `minSeconds`/`maxSeconds` | DRY; avoids per-mode composables; mode behavior (DND, breaks, alarm) is independent of duration |
| Default on mode switch | ViewModel reads pref for POMODORO; hardcoded for others | Pomodoro preference is the source of truth for that mode's default interval |
| POMODORO range | 5–90 min | Focus intervals beyond 90 min are clinically not "Pomodoro"; keeps UI sensible |
| DEEP_WORK range | 30–180 min | Minimum 30 min is the accepted lower bound for deep work; max = spec FR-002 max |
| CUSTOM/STUDY range | 5–180 min | Matches FR-002 exactly |
| DEEP_WORK default | 90 min | Canonical deep work session length; decouples from Pomodoro pref |

---

## Phase 1: Design & Fix Contracts

### Fix 1 — Analytics Bar Chart Labels Clipped (`FocusBarChart.kt`)

**Root cause:** `BarColumn` height = `MAX_BAR_HEIGHT + 28dp = 168dp`. At max bar height,
content = top label (≈16dp) + 2dp spacer + bar (140dp) + 4dp spacer + date label (≈16dp) = **178dp**.
Date label overflows 10dp and is clipped by the parent.

**Change — one line in `BarColumn`:**
```kotlin
// line 75: before
.height(MAX_BAR_HEIGHT + 28.dp)

// after
.height(MAX_BAR_HEIGHT + 48.dp)
```

---

### Fix 2 — Badge Dialog Reappears on Every Visit (`TimerService.kt` + `TimerViewModel.kt`)

**Root cause:** `TimerService._timerState` retains `newlyAwardedBadges = [badge]` after
the session ends. `TimerViewModel.onDismissBadge()` only copies its local snapshot of
`_timerState` — the service's own StateFlow is never cleared. On next bind (navigate
away → back), the service replays its state and the badge fires again.

**`TimerService.kt` — add public method (after `skipBreak()`):**
```kotlin
fun clearNewlyAwardedBadges() {
    _timerState.value = _timerState.value.copy(newlyAwardedBadges = emptyList())
}
```

**`TimerViewModel.kt` — update `onDismissBadge()`:**
```kotlin
fun onDismissBadge() {
    _newBadge.value = null
    timerService?.clearNewlyAwardedBadges()
    // Remove the old local-copy line — it only masked the bug
}
```

---

### Fix 3 — Wrong Distraction App Names (`DistractionMonitor.kt` + `TimerService.kt`)

**Root cause:** `pollForegroundApp()` emits the raw Android package name
(e.g., `com.android.launcher3`, `com.whatsapp`). `LogDistractionUseCase` stores it
as-is. Session detail displays whatever is in the DB.

**`DistractionMonitor.kt` — add name resolution:**

```kotlin
// 1. Add cache field
private val appNameCache = HashMap<String, String>()

// 2. Add resolver helper
private fun resolveAppName(packageName: String): String {
    appNameCache[packageName]?.let { return it }
    val label = try {
        val pm = context.packageManager
        val info = pm.getApplicationInfo(packageName, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        packageName.substringAfterLast('.').replaceFirstChar { it.uppercaseChar() }
    }
    val display = if (packageName.contains("launcher", ignoreCase = true)) "Home Screen" else label
    appNameCache[packageName] = display
    return display
}
```

**Update `Event.AppSwitch` to carry display name:**
```kotlin
data class AppSwitch(
    val packageName: String,
    val appDisplayName: String,   // new field
    val timestampMs: Long,
) : Event()
```

**Update emit in `pollForegroundApp()`:**
```kotlin
// before:
_events.tryEmit(Event.AppSwitch(latestForeground, now))

// after:
_events.tryEmit(Event.AppSwitch(latestForeground, resolveAppName(latestForeground), now))
```

**`TimerService.collectDistractionEvents()` — thread display name through:**
```kotlin
is DistractionMonitor.Event.UserReturned -> {
    // pass event.appDisplayName (not raw packageName) to use case
    logDistractionUseCase(
        sessionId = sessionId,
        timestampMs = event.timestampMs,
        type = type,
        appPackageName = event.packageName?.let { resolveFrom(distractionMonitor, it) }
                         ?: event.packageName,
        awayDurationMs = event.awayDurationMs,
    )
}
```

> **Simpler path**: store the `distractionPackage` display name alongside the raw package name
> in `TimerService`'s private fields, populated when `AppSwitch` fires. Then pass the display
> name to `LogDistractionUseCase` in `UserReturned`. No additional call needed.

**Concrete implementation plan for TimerService:**
- Add `private var distractionDisplayName: String? = null`
- On `AppSwitch` event: `distractionDisplayName = event.appDisplayName`
- On `UserReturned` event: pass `distractionDisplayName` as `appPackageName` to use case
- On `ScreenUnlock` event: `distractionDisplayName = null` (no app name for screen unlock)

---

### Enhancement 1 + 2 — Drum-Roll Picker for All Modes

#### New file: `TimerWheelPicker.kt`

```kotlin
@Composable
fun TimerWheelPicker(
    totalSeconds: Int,
    onSecondsChanged: (Int) -> Unit,
    minSeconds: Int = 300,
    maxSeconds: Int = 10800,
    label: String = "Duration",
    modifier: Modifier = Modifier,
)
```

Internal structure:
- Two side-by-side `WheelColumn` composables: **Hours (0–2)** and **Minutes (0–59)**
- `WheelColumn` uses `LazyColumn` + `rememberSnapFlingBehavior`; shows 5 items, selected = middle
- On settle: reads `firstVisibleItemIndex + 2` → selected value
- Decode input: `hours = totalSeconds / 3600`, `minutes = (totalSeconds % 3600) / 60`
- Emit: `onSecondsChanged(hours * 3600 + minutes * 60)` when either wheel settles
- Validation: coerce emitted value to `minSeconds..maxSeconds` before calling callback

#### Mode-specific ranges and defaults

| Mode | `minSeconds` | `maxSeconds` | Default shown | Label |
|------|-------------|-------------|---------------|-------|
| POMODORO | 300 (5 min) | 5400 (90 min) | `pomodoroFocusMinutes × 60` | "Focus interval" |
| DEEP_WORK | 1800 (30 min) | 10800 (180 min) | 5400 (90 min) | "Duration" |
| CUSTOM | 300 (5 min) | 10800 (180 min) | 1500 (25 min) | "Duration" |
| STUDY | 300 (5 min) | 10800 (180 min) | 1500 (25 min) | "Duration" |

Tag field (optional text input): **only shown for CUSTOM and STUDY** modes.

#### `TimerScreen.kt` — show picker for all modes

**Before (line 150):**
```kotlin
if (selectedMode == SessionMode.CUSTOM || selectedMode == SessionMode.STUDY) {
    Spacer(modifier = Modifier.height(12.dp))
    CustomDurationPicker(...)
}
```

**After:**
```kotlin
Spacer(modifier = Modifier.height(12.dp))
TimerWheelPicker(
    totalSeconds = customDurationSeconds,
    onSecondsChanged = viewModel::onCustomDurationSecondsChanged,
    minSeconds = selectedMode.minSeconds,
    maxSeconds = selectedMode.maxSeconds,
    label = selectedMode.pickerLabel,
    modifier = Modifier.fillMaxWidth(),
)
if (selectedMode == SessionMode.CUSTOM || selectedMode == SessionMode.STUDY) {
    Spacer(modifier = Modifier.height(8.dp))
    TagInput(tag = customTag, onTagChanged = viewModel::onTagChanged)
}
```

Add extension properties on `SessionMode` (in `TimerScreen.kt`, not domain layer):
```kotlin
private val SessionMode.minSeconds: Int get() = when (this) {
    SessionMode.POMODORO -> 300
    SessionMode.DEEP_WORK -> 1800
    SessionMode.CUSTOM, SessionMode.STUDY -> 300
}
private val SessionMode.maxSeconds: Int get() = when (this) {
    SessionMode.POMODORO -> 5400
    SessionMode.DEEP_WORK, SessionMode.CUSTOM, SessionMode.STUDY -> 10800
}
private val SessionMode.pickerLabel: String get() = when (this) {
    SessionMode.POMODORO -> "Focus interval"
    else -> "Duration"
}
```

#### `TimerViewModel.kt` — seconds-based duration + smart defaults

```kotlin
// Replace
private val _customDurationMinutes = MutableStateFlow(25)
val customDurationMinutes: StateFlow<Int> = _customDurationMinutes.asStateFlow()

// With
private val _customDurationSeconds = MutableStateFlow(25 * 60)
val customDurationSeconds: StateFlow<Int> = _customDurationSeconds.asStateFlow()
```

Update `onModeSelected()` to apply mode default:
```kotlin
fun onModeSelected(mode: SessionMode) {
    _selectedMode.value = mode
    viewModelScope.launch {
        _customDurationSeconds.value = when (mode) {
            SessionMode.POMODORO ->
                buildSessionConfigUseCase.pomodoroDefaultSeconds(prefs)
            SessionMode.DEEP_WORK -> 90 * 60
            SessionMode.CUSTOM, SessionMode.STUDY -> 25 * 60
        }
    }
}
```

> `pomodoroDefaultSeconds` is a helper method on `BuildSessionConfigUseCase` that reads the
> pref flow. Alternatively, inject `FocusPreferences` directly into `TimerViewModel` for this
> one read — simpler and avoids adding state to the use case.

**Simplest approach**: inject `FocusPreferences` into `TimerViewModel` (it's already a domain
interface, no Constitution violation):
```kotlin
// In onModeSelected:
SessionMode.POMODORO -> prefs.pomodoroFocusMinutes.first() * 60
```

Rename handlers:
```kotlin
fun onCustomDurationSecondsChanged(seconds: Int) {
    _customDurationSeconds.value = seconds
}
```

Update `buildSessionConfig()`:
```kotlin
private suspend fun buildSessionConfig(): SessionConfig =
    buildSessionConfigUseCase(
        mode = _selectedMode.value,
        durationSeconds = _customDurationSeconds.value,
        tag = _customTag.value.trim().ifEmpty { null },
    )
```

#### `BuildSessionConfigUseCase.kt` — all modes use caller-supplied seconds

```kotlin
// New signature — remove FocusPreferences dependency (no longer needed)
suspend operator fun invoke(
    mode: SessionMode,
    durationSeconds: Int,
    tag: String?,
): SessionConfig {
    val clamped = durationSeconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
    return SessionConfig(mode = mode, durationSeconds = clamped, tag = tag)
}

companion object {
    const val MIN_DURATION_SECONDS = 300    // 5 min
    const val MAX_DURATION_SECONDS = 10800  // 180 min
}
```

Since `FocusPreferences` is no longer needed in the use case, remove it from the
constructor — simplifying the class (Constitution §VI).

---

## Phase 2: Task List

| ID | File | Change | Size |
|----|------|--------|------|
| T-P1 | `FocusBarChart.kt:75` | `+28.dp` → `+48.dp` | XS |
| T-P2a | `TimerService.kt` | Add `clearNewlyAwardedBadges()` | XS |
| T-P2b | `TimerViewModel.kt` | Call `clearNewlyAwardedBadges()` on dismiss | XS |
| T-P3a | `DistractionMonitor.kt` | Add `appNameCache`, `resolveAppName()`, update `AppSwitch` data class | S |
| T-P3b | `DistractionMonitor.kt` | Update `pollForegroundApp()` emit with resolved name | XS |
| T-P3c | `TimerService.kt` | Add `distractionDisplayName` field; thread through to use case | S |
| T-P4a | `TimerWheelPicker.kt` | New drum-roll composable (Hours + Minutes columns) | M |
| T-P4b | `TimerViewModel.kt` | Rename to `_customDurationSeconds`; inject `FocusPreferences`; update `onModeSelected()` defaults; rename handlers | S |
| T-P4c | `BuildSessionConfigUseCase.kt` | Remove prefs; rename param to `durationSeconds`; update constants | S |
| T-P4d | `TimerScreen.kt` | Remove old `CustomDurationPicker` condition; show `TimerWheelPicker` always; add `SessionMode` extensions; keep tag only for CUSTOM/STUDY | S |
| T-P4e | `BuildSessionConfigUseCaseTest.kt` | Update test calls for renamed param + all-mode coverage | S |

**Estimated effort**: ~4–5 hours total. No DB migrations. No new permissions.

---

## Manual Verification Checkpoint

After all tasks complete, verify on device:

| # | Steps | Expected |
|---|-------|----------|
| 1 | Analytics tab → bar chart | Date labels (MM-DD) fully visible below each bar |
| 2 | Complete session → dismiss badge → nav away → return to Timer | Badge dialog does NOT reappear |
| 3 | Start session → switch to WhatsApp → return → Session Detail | Shows "WhatsApp", not "whatsapp" |
| 4 | Press Home during session → Session Detail | Shows "Home Screen", not "launcher" |
| 5 | Select **Pomodoro** → wheel shows 0–90 min, default matches pref | Picker visible; Start works |
| 6 | Select **Deep Work** → wheel shows 30–180 min, default 90 min | Picker visible; Start works |
| 7 | Select **Study** → wheel shows 5–180 min + tag field | Same as Custom; tag optional |
| 8 | Select **Custom** → wheel shows 5–180 min + tag field | Picker replaces old slider |
| 9 | Scroll Custom to 1h 30m → Start | Countdown begins at 90:00 |
