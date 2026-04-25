# Research: Focus + Behavior Tracking System

**Phase**: 0 — Pre-Design Research  
**Date**: 2026-04-24  
**Branch**: `001-focus-behavior-tracker`

---

## 1. Distraction Detection on Android

**Decision**: Use `UsageStatsManager` with polling via a `Foreground Service` + `Handler` (every 3–5 seconds).

**Rationale**:
- `UsageStatsManager.queryUsageStats()` returns a list of app usage events; `UsageEvents.queryEvents()` gives per-event granularity including `ACTIVITY_RESUMED` and `ACTIVITY_PAUSED`, enabling accurate app-switch detection.
- Polling interval of 3–5 seconds keeps latency under the 5-second spec requirement while staying battery-friendly.
- The foreground service already runs for the timer, so no extra persistent process is needed.
- `ACTION_USER_PRESENT` broadcast detects screen unlocks reliably without polling.

**Alternatives considered**:
- `AccessibilityService`: More powerful but requires a scary "accessibility permission" dialog that damages trust. Rejected for V1.
- `ActivityManager.getRunningTasks()`: Deprecated since API 21. Rejected.

**Required permission**: `android.permission.PACKAGE_USAGE_STATS` — user must grant manually in Settings > Apps > Special App Access > Usage Access. The app must redirect the user with a deep-link Intent to this settings page on first run.

---

## 2. Timer Architecture

**Decision**: Android `Foreground Service` with a `CountDownTimer` / coroutine-based ticker; state exposed via `StateFlow` to the UI.

**Rationale**:
- Foreground services are not killed by the OS when the app is backgrounded, which is essential for a timer that must keep running accurately.
- Using a `Flow`-based ticker (1-second emissions from the service) with a bound service connection keeps the UI reactive without polling.
- A `Notification` with live countdown is required by Android to display a foreground service.

**Alternatives considered**:
- `WorkManager`: Designed for deferrable background work (not real-time). Cannot guarantee 1-second precision. Rejected for timer core.
- `AlarmManager` with re-schedule: More battery-efficient for single-shot alarms (used for Study Mode end alarm), but cannot serve ongoing countdown UI updates. Hybrid approach: use AlarmManager for the final alarm, Service for the live countdown.

---

## 3. Local Database

**Decision**: Room 2.6 (SQLite wrapper) with Kotlin Coroutines + Flow for reactive queries.

**Rationale**:
- Room provides compile-time SQL validation, type-safe DAOs, and first-class Flow support for reactive UI updates.
- SQLite is appropriate for the data volume (~thousands of session rows) and fully offline.
- Flow-returning DAOs (`Flow<List<FocusSession>>`) allow the analytics screen to auto-update when new sessions are inserted.

**Alternatives considered**:
- Realm: More complex, larger binary, no significant benefit at this scale. Rejected.
- DataStore alone: Only for key-value prefs (used for settings), not relational session data. Rejected as primary store.

---

## 4. UI Framework

**Decision**: Jetpack Compose with Material 3.

**Rationale**:
- Compose is the current Android-standard declarative UI toolkit; aligns with Kotlin-first development.
- Material 3 provides dynamic color theming (supports the paid theme customization feature in V3).
- Navigation Compose handles screen transitions; no XML layouts needed.

**Alternatives considered**:
- XML Views + Data Binding: Legacy approach. More boilerplate. Rejected.
- Flutter: Cross-platform but adds Dart dependency; distraction detection via UsageStats is harder to bridge. Rejected for V1.

---

## 5. Dependency Injection

**Decision**: Hilt (Dagger-based).

**Rationale**:
- Hilt is the officially recommended DI framework for Android. It generates boilerplate-free component scoping.
- `@HiltAndroidApp`, `@AndroidEntryPoint`, and `@HiltViewModel` annotations handle Activity/Fragment/Service/ViewModel injection with minimal setup.

**Alternatives considered**:
- Koin: Lighter but runtime-based (no compile-time safety). Rejected for a production-quality codebase.
- Manual DI: Viable for tiny apps; too verbose as the app grows. Rejected.

---

## 6. Reminders / Scheduled Notifications

**Decision**: `WorkManager` for daily goal reminders; `AlarmManager` (exact alarms) for habit-based reminders that need precise timing.

**Rationale**:
- WorkManager is battery-aware and respects Doze mode; appropriate for non-critical daily summaries.
- `AlarmManager.setExactAndAllowWhileIdle()` is needed for habit reminders where ±5-minute accuracy matters (per SC-006). Android 12+ requires `SCHEDULE_EXACT_ALARM` permission.
- Break nudges are handled by the foreground service directly (no scheduling needed — it's already running).

---

## 7. Focus Score Formula

**Decision**: `Score = clamp((completedSessions / startedSessions) × 70 + (1 − distractedMinutes / totalFocusMinutes) × 30, 0, 100)`

**Rationale**:
- Weighted 70/30: session completion matters more than distraction-free ratio, rewarding consistent effort.
- Clamped to 0–100 for display simplicity.
- Falls back to 100 if no sessions started (empty state).

---

## 8. XP & Gamification Logic

**Decision**:
- XP per session: `floor(sessionMinutes / 5) × 10` XP (e.g., 25 min → 50 XP, 50 min → 100 XP).
- Streak: Incremented when at least 1 session is completed on a calendar day (device local time). Checked at session end and at midnight via WorkManager.
- Badges: Evaluated after every session completion via a `BadgeEvaluator` use case that checks badge conditions against aggregate session stats.

**Badge conditions (initial set)**:

| Badge | Condition |
|-------|-----------|
| First Focus | Complete first session |
| Deep Diver | 2 hours total in one day |
| Week Warrior | 7-day streak |
| Distraction-Free | Complete a session with 0 distractions |
| Century | Accumulate 100 sessions total |

---

## 9. Permissions Strategy

| Permission | When Requested | Fallback if Denied |
|------------|---------------|-------------------|
| `PACKAGE_USAGE_STATS` | First timer start | Distraction detection disabled; shown as "unavailable" in UI |
| `POST_NOTIFICATIONS` (Android 13+) | App first launch | Reminders and session-end alerts silently disabled |
| `FOREGROUND_SERVICE` | Automatic (declared in manifest) | N/A — required for timer service |
| `RECEIVE_BOOT_COMPLETED` | Automatic | WorkManager handles re-scheduling after reboot |
| `SCHEDULE_EXACT_ALARM` (Android 12+) | Reminder setup flow | Reminders fall back to WorkManager (less precise) |

---

## 10. Minimum SDK & Target SDK

**Decision**: `minSdk = 26` (Android 8.0), `targetSdk = 35` (Android 15).

**Rationale**:
- Android 8.0 introduced notification channels (required for foreground service notification).
- API 26 covers ~95% of active Android devices as of 2026.
- Targeting API 35 ensures compliance with latest Play Store requirements.
