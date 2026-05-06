# Plan: Focus + Behavior Tracking System (V1)

**Version**: V1 — Behavior MVP
**Branch**: `001-focus-behavior-tracker`
**Goal**: Build a system that **actively improves user focus behavior**, not just tracks time

---

# 🧠 PRODUCT GOAL

## Problem

Users struggle to stay focused due to:

* Frequent distractions
* Lack of accountability
* Ineffective existing tools (passive timers)

---

## Solution

Build a system that:

> Detects distraction → reacts to it → provides feedback → improves behavior

---

## Core Differentiator

> Focus Enforcement System

Unlike existing apps:

* Not just tracking
* Not just timing
* Actively enforces focus behavior

---

# 🧩 SYSTEM OVERVIEW

## Architecture

* **UI Layer** → Jetpack Compose screens
* **Domain Layer** → Use cases (business logic)
* **Data Layer** → Room DB + DataStore
* **Service Layer** → `TimerService` (foreground)

---

## Key Components

### 1. TimerService

* Manages session lifecycle
* Emits timer state
* Integrates distraction events
* Applies enforcement logic

---

### 2. DistractionMonitor

* Detects:

  * App switches
  * Screen unlocks
* Emits events via Flow

---

### 3. FocusSession System

* Stores:

  * duration
  * distractions
  * outcome
  * XP

---

### 4. Behavior Layer (NEW)

Adds:

* FocusStrictness
* SessionOutcome
* Enforcement logic

---

# 🔥 V1 FEATURE SCOPE

---

## 🟢 1. Focus Session System

* Timer (all modes supported)
* Wheel picker duration
* Quick start (≤ 2 actions)

---

## 🟢 2. Distraction Detection

* UsageStats-based detection
* Event logging

---

## 🔥 3. Focus Enforcement (CRITICAL)

### Modes

| Mode     | Behavior      |
| -------- | ------------- |
| RELAXED  | Log only      |
| STRICT   | Pause session |
| HARDCORE | End session   |

---

## 🔥 4. Session Outcome System

| Outcome     | Meaning           |
| ----------- | ----------------- |
| CLEAN       | No distractions   |
| INTERRUPTED | Some distractions |
| FAILED      | Heavy distraction |

---

## 🟢 5. Basic Feedback

* Distraction count
* Session outcome
* Simple messages

---

## 🟢 6. Motivation System

* XP (existing logic)
* Streak tracking
* Badge system

---

## 🟢 7. Minimal History

* Session list
* Session detail view

---

# ❌ OUT OF SCOPE (V1)

Do NOT implement:

* Advanced analytics
* Smart insights engine
* Habit detection
* Reminders system
* Accessibility-based app blocking
* Social features
* Complex UI customization

---

# 🧱 IMPLEMENTATION STRATEGY

---

## Phase Order (Strict)

1. Core timer system
2. Session persistence
3. Distraction detection
4. Focus enforcement
5. Session outcome
6. UI feedback
7. Motivation system
8. History

---

## Development Rules

* Keep changes minimal
* Avoid unnecessary abstractions
* Do not break existing architecture
* Implement one phase at a time

---

# 🧠 BEHAVIOR DESIGN

---

## Key Principle

> Behavior > Features

---

## User Flow

```text
Start Session
   ↓
Focus
   ↓
Distraction detected?
   ↓
Yes → Enforcement triggered
   ↓
Session ends
   ↓
Outcome shown (Clean / Interrupted / Failed)
   ↓
User gets feedback
   ↓
User improves next session
```

---

## Expected Impact

* Increased focus awareness
* Reduced distraction frequency
* Improved session quality over time

---

# 📊 SUCCESS CRITERIA

---

## Functional

* Timer runs reliably
* Distractions detected correctly
* Enforcement works as expected
* Session outcome always computed

---

## Behavioral

* User understands focus quality
* User experiences consequence for distraction
* User is motivated to improve

---

# ⚠️ RISKS & MITIGATION

---

## Risk 1 — Over-complexity

**Mitigation**:

* Keep UI simple
* Hide advanced logic

---

## Risk 2 — Weak enforcement

**Mitigation**:

* Ensure STRICT/HARDCORE modes work reliably

---

## Risk 3 — User friction

**Mitigation**:

* Maintain quick start (≤ 2 taps)

---

# 🏁 DEFINITION OF DONE

V1 is complete when:

* Core system works reliably
* Enforcement is functional
* Feedback is clear
* UI remains simple
* No unnecessary features added

---

# 🚀 NEXT STEP

After V1:

→ Move to V2 (Intelligence Layer)

---
