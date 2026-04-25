# Data Model: Focus + Behavior Tracking System

**Phase**: 1 — Design  
**Date**: 2026-04-24

---

## Entity Overview

```
UserProfile (singleton)
    │
    ├── FocusSession (1:N)
    │       │
    │       └── DistractionEvent (1:N)
    │
    ├── Badge (1:N, awarded)
    └── Reminder (1:N)

SessionTag (standalone lookup — joined to FocusSession via tag string)
```

---

## Entities

### FocusSession

Represents one complete or partial focus interval.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | Long (PK, autoincrement) | NOT NULL | Unique session identifier |
| `startTime` | Long (epoch ms) | NOT NULL | Wall-clock start |
| `endTime` | Long (epoch ms) | NULLABLE | NULL if session is active |
| `plannedDuration` | Int (seconds) | NOT NULL | User-configured target |
| `actualDuration` | Int (seconds) | NOT NULL, ≥ 0 | Time from start to end/stop |
| `mode` | String (enum) | NOT NULL | `POMODORO`, `CUSTOM`, `STUDY`, `DEEP_WORK` |
| `tag` | String | NULLABLE | Free-form label (e.g., "DSA") |
| `status` | String (enum) | NOT NULL | `COMPLETED`, `PARTIAL`, `ACTIVE` |
| `distractionCount` | Int | NOT NULL, ≥ 0 | Denormalized count for perf |
| `distractionTotalSeconds` | Int | NOT NULL, ≥ 0 | Denormalized total for perf |
| `xpAwarded` | Int | NOT NULL, ≥ 0 | XP granted at session end |
| `focusScore` | Int | NULLABLE | Per-session score 0–100 |

**Indexes**: `startTime` (for date-range queries in analytics)

**State transitions**:
```
ACTIVE → COMPLETED  (timer reaches zero or user ends with ≥ plannedDuration)
ACTIVE → PARTIAL    (user ends early, actualDuration < plannedDuration)
```

---

### DistractionEvent

An interruption detected during an active session.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | Long (PK, autoincrement) | NOT NULL | |
| `sessionId` | Long (FK → FocusSession.id) | NOT NULL | Cascade delete |
| `timestamp` | Long (epoch ms) | NOT NULL | When distraction started |
| `type` | String (enum) | NOT NULL | `APP_SWITCH`, `SCREEN_UNLOCK` |
| `appPackageName` | String | NULLABLE | Package name if app-switch |
| `durationSeconds` | Int | NOT NULL, ≥ 0 | How long user was away |

**Indexes**: `sessionId`

---

### UserProfile

Singleton row (id = 1). Stores persistent gamification state.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | Int (PK) | NOT NULL, always 1 | Singleton |
| `totalXp` | Int | NOT NULL, ≥ 0 | Cumulative XP |
| `currentStreak` | Int | NOT NULL, ≥ 0 | Consecutive days with ≥1 session |
| `longestStreak` | Int | NOT NULL, ≥ 0 | All-time best streak |
| `streakLastUpdatedDate` | String | NOT NULL | ISO date `YYYY-MM-DD` |
| `totalSessionsCompleted` | Int | NOT NULL, ≥ 0 | Running total |
| `isPremium` | Boolean | NOT NULL, default false | Premium status |
| `dailyFocusGoalMinutes` | Int | NOT NULL, default 60 | User-configurable goal |

---

### Badge

An achievement awarded to the user.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | String (PK) | NOT NULL | Stable identifier e.g. `FIRST_FOCUS` |
| `name` | String | NOT NULL | Display name |
| `description` | String | NOT NULL | Condition description |
| `awardedAt` | Long (epoch ms) | NOT NULL | When it was earned |

**Predefined badge IDs**: `FIRST_FOCUS`, `DEEP_DIVER`, `WEEK_WARRIOR`, `DISTRACTION_FREE`, `CENTURY`

---

### Reminder

A user-configured or auto-suggested notification schedule.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | Long (PK, autoincrement) | NOT NULL | |
| `type` | String (enum) | NOT NULL | `HABIT`, `GOAL`, `BREAK` |
| `triggerTimeHour` | Int | NOT NULL | 0–23 |
| `triggerTimeMinute` | Int | NOT NULL | 0–59 |
| `message` | String | NOT NULL | Notification body |
| `isEnabled` | Boolean | NOT NULL, default true | Toggle without deleting |
| `daysOfWeek` | String | NOT NULL | Bitmask or comma-separated `1,2,3,4,5` |

---

## Derived / Computed Values

These are not stored as columns but computed at query time or in the domain layer:

| Value | Source | Formula |
|-------|--------|---------|
| Daily focus minutes | FocusSession | `SUM(actualDuration) WHERE date(startTime) = today` |
| Weekly focus hours | FocusSession | `SUM(actualDuration) / 3600 WHERE startTime ≥ week_start` |
| Focus Score (day) | FocusSession + DistractionEvent | `clamp(completedRatio×70 + distractionFreeRatio×30, 0, 100)` |
| Best focus time | FocusSession | Bucket sessions by 2-hour slots; find slot with lowest avg distractionCount |
| XP for session | FocusSession | `floor(actualDuration / 300) × 10` |

---

## DataStore (Preferences)

Non-relational settings stored in Jetpack DataStore (Proto or Preferences):

| Key | Type | Default | Notes |
|-----|------|---------|-------|
| `pomodoro_focus_minutes` | Int | 25 | |
| `pomodoro_short_break_minutes` | Int | 5 | |
| `pomodoro_long_break_minutes` | Int | 15 | |
| `pomodoro_intervals_before_long` | Int | 4 | |
| `usage_stats_permission_asked` | Boolean | false | |
| `notification_permission_asked` | Boolean | false | |
| `onboarding_complete` | Boolean | false | |
| `theme` | String | `DEFAULT` | `DEFAULT`, `DARK`, `AMOLED` (premium) |
