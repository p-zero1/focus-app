# Navigation Contract: Focus + Behavior Tracking System

**Phase**: 1 — Design  
**Date**: 2026-04-24

This document defines the screen-level navigation contract for the Android app. It specifies what each screen exposes, what it requires as input, and what it emits as navigation events.

---

## Screen Inventory

| Screen ID | Route | Description |
|-----------|-------|-------------|
| `timer` | `/timer` | Main focus timer — default destination |
| `analytics` | `/analytics` | Daily/weekly focus charts + Focus Score |
| `history` | `/history` | Session history list with tag filter |
| `profile` | `/profile` | Streaks, XP, badges |
| `settings` | `/settings` | Timer config, reminders, permissions |
| `session_detail` | `/session/{id}` | Detail view for a single session |
| `onboarding` | `/onboarding` | First-run permissions + setup flow |

---

## Bottom Navigation Tabs

The primary navigation uses a bottom bar with 4 tabs:

```
[Timer] [Analytics] [History] [Profile]
```

Settings is accessible from the Profile screen (top-right icon).

---

## Screen Contracts

### timer (`/timer`)

**Requires**: None (always accessible)  
**Displays**: Active session state (countdown, mode, tag, distraction count), start/stop/pause controls, mode selector  
**Navigates to**:
- `session_detail/{id}` — automatically after session ends (pass completed session ID)
- `settings` — via settings icon
- `onboarding` — on first launch if `onboarding_complete = false`

**Service binding**: Binds to `TimerService` on enter, unbinds on exit. Timer continues if unbound.

---

### analytics (`/analytics`)

**Requires**: At least 1 logged session (otherwise shows empty state)  
**Displays**: Toggle (Daily / Weekly), bar/line chart, Focus Score card, best-time insight card  
**Navigates to**:
- `history` — tapping "View all sessions" link

---

### history (`/history`)

**Requires**: None (shows empty state if no sessions)  
**Displays**: Chronological session list with mode icon, duration, distraction count, tag badge; tag filter chips  
**Navigates to**:
- `session_detail/{id}` — on session row tap

**Args**: Optional `filterTag: String?` — if provided, pre-filters by tag on open

---

### session_detail (`/session/{id}`)

**Requires**: `id: Long` (valid session ID)  
**Displays**: Session summary (mode, duration, tag, XP earned, Focus Score), distraction timeline, app names if available  
**Navigates to**:
- Back (pop stack)

---

### profile (`/profile`)

**Requires**: UserProfile entity  
**Displays**: Current streak, XP total, XP level, badge gallery, daily goal progress  
**Navigates to**:
- `settings` — via settings icon (top-right)

---

### settings (`/settings`)

**Requires**: None  
**Displays**: Pomodoro config, daily goal input, reminder toggles, permission status cards (Usage Stats, Notifications, Exact Alarm)  
**Navigates to**:
- Android system settings (Usage Access, Notification) via deep-link Intent — handled by OS, not Compose nav
- Back

---

### onboarding (`/onboarding`)

**Requires**: `onboarding_complete = false`  
**Displays**: 3-step walkthrough: (1) welcome + value prop, (2) request Usage Stats permission, (3) request Notification permission  
**Navigates to**:
- `timer` — on completion or skip; sets `onboarding_complete = true`

---

## TimerService Contract

`TimerService` is a bound + started foreground service. The UI communicates via a bound `Binder` interface:

| Method / StateFlow | Direction | Description |
|--------------------|-----------|-------------|
| `timerState: StateFlow<TimerState>` | Service → UI | Current countdown value, status, elapsed time |
| `startSession(config: SessionConfig)` | UI → Service | Begin a new session |
| `pauseSession()` | UI → Service | Pause current countdown |
| `resumeSession()` | UI → Service | Resume paused countdown |
| `endSession()` | UI → Service | Stop and finalize session |

**TimerState**:
```kotlin
data class TimerState(
    val status: TimerStatus,        // IDLE, ACTIVE, PAUSED, BREAK, FINISHED
    val remainingSeconds: Int,
    val elapsedSeconds: Int,
    val currentSessionId: Long?,
    val distractionCount: Int
)
```

**SessionConfig**:
```kotlin
data class SessionConfig(
    val mode: SessionMode,          // POMODORO, CUSTOM, STUDY, DEEP_WORK
    val durationSeconds: Int,
    val tag: String? = null
)
```

---

## DistractionMonitor Contract

`DistractionMonitor` runs inside `TimerService`. It exposes events via a `SharedFlow`:

```kotlin
sealed class DistractionEvent {
    data class AppSwitch(val toPackage: String, val timestamp: Long) : DistractionEvent()
    data class ScreenUnlock(val timestamp: Long) : DistractionEvent()
    data class UserReturned(val durationSeconds: Int) : DistractionEvent()
}
```

`TimerService` subscribes to this flow and writes `DistractionEvent` rows to the database when `UserReturned` fires.
