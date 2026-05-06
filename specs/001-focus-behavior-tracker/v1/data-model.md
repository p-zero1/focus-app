# 🗄️ Data Model: Focus + Behavior Tracking System (V1)

**Version**: V1 — Behavior MVP
**Database**: Room (SQLite)
**Scope**: Minimal schema required for behavior-driven focus system

---

# 🧠 DESIGN PRINCIPLES

* Keep schema minimal and extensible
* Store only necessary data for V1
* Enable behavior tracking (not just time tracking)
* Avoid premature optimization

---

# 🧩 ENTITIES

---

## 1. FocusSession

### Description

Represents a single focus session.

---

### Fields

| Field             | Type   | Constraints | Description                           |
| ----------------- | ------ | ----------- | ------------------------------------- |
| `id`              | Long   | PK, auto    | Unique session ID                     |
| `mode`            | String | NOT NULL    | Session mode (POMODORO, CUSTOM, etc.) |
| `durationSeconds` | Int    | NOT NULL    | Planned session duration              |
| `startTime`       | Long   | NOT NULL    | Start timestamp                       |
| `endTime`         | Long   | NULLABLE    | End timestamp                         |
| `status`          | String | NOT NULL    | ACTIVE, COMPLETED, PAUSED             |
| `tag`             | String | NULLABLE    | Optional session tag                  |

---

### 🔥 Behavior Fields (V1 Core)

| Field                     | Type   | Constraints | Description                |
| ------------------------- | ------ | ----------- | -------------------------- |
| `distractionCount`        | Int    | DEFAULT 0   | Number of distractions     |
| `distractionTotalSeconds` | Int    | DEFAULT 0   | Total distraction time     |
| `focusStrictness`         | String | NOT NULL    | RELAXED, STRICT, HARDCORE  |
| `sessionOutcome`          | String | NOT NULL    | CLEAN, INTERRUPTED, FAILED |

---

### 🎮 Motivation Fields

| Field      | Type | Constraints | Description            |
| ---------- | ---- | ----------- | ---------------------- |
| `xpEarned` | Int  | DEFAULT 0   | XP earned from session |

---

### Indexes

* INDEX on `startTime`
* INDEX on `tag`

---

## 2. DistractionEvent

### Description

Represents a distraction during a session.

---

### Fields

| Field             | Type   | Constraints          | Description             |
| ----------------- | ------ | -------------------- | ----------------------- |
| `id`              | Long   | PK, auto             | Unique event ID         |
| `sessionId`       | Long   | FK → FocusSession.id | Associated session      |
| `timestamp`       | Long   | NOT NULL             | Event time              |
| `durationSeconds` | Int    | DEFAULT 0            | Time spent away         |
| `appPackageName`  | String | NULLABLE             | App causing distraction |
| `appDisplayName`  | String | NULLABLE             | Human-readable app name |

---

### Indexes

* INDEX on `sessionId`

---

## 3. UserProfile

### Description

Stores aggregated user progress.

---

### Fields

| Field           | Type | Constraints     | Description    |
| --------------- | ---- | --------------- | -------------- |
| `id`            | Int  | PK (single row) | Always 1       |
| `totalXp`       | Int  | DEFAULT 0       | Total XP       |
| `currentStreak` | Int  | DEFAULT 0       | Current streak |
| `longestStreak` | Int  | DEFAULT 0       | Longest streak |

---

---

# 🧠 ENUM DEFINITIONS

---

## SessionMode

```kotlin
POMODORO
CUSTOM
DEEP_WORK
STUDY
```

---

## SessionStatus

```kotlin
ACTIVE
PAUSED
COMPLETED
```

---

## 🔥 FocusStrictness (V1 CORE)

```kotlin
RELAXED
STRICT
HARDCORE
```

---

## 🔥 SessionOutcome (V1 CORE)

```kotlin
CLEAN
INTERRUPTED
FAILED
```

---

# 🧩 RELATIONSHIPS

* One `FocusSession` → many `DistractionEvent`
* One `UserProfile` → global singleton

---

# ⚠️ CONSTRAINTS

* `durationSeconds > 0`
* `distractionCount ≥ 0`
* `distractionTotalSeconds ≥ 0`
* `focusStrictness` must be valid enum
* `sessionOutcome` must always be set before session completion

---

# 🧠 DERIVED VALUES (NOT STORED)

These should be computed in use cases:

* Focus score
* Session quality interpretation
* Best focus time

---

# ❌ OUT OF SCOPE (V1)

Do NOT include:

* Advanced analytics tables
* Habit tracking tables
* Reminder schedules
* Sync metadata

---

# 🏁 SUMMARY

This data model supports:

* Focus session tracking
* Distraction detection
* Behavior enforcement
* Session quality feedback
* Basic motivation system

---

## Key Strength

> Enables behavior-driven insights without adding unnecessary complexity

---
