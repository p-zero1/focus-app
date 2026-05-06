# Tasks: Focus + Behavior Tracking System (V1)

**Version**: V1 — Behavior MVP
**Goal**: Build a system that **actively improves user focus**, not just tracks time

---

# 🧠 EXECUTION RULES

* Follow tasks **in order**
* Do NOT skip phases
* Do NOT add extra features
* Keep changes **minimal and localized**
* Validate each phase before moving forward

---

# 🟢 PHASE 1 — CORE TIMER SYSTEM (BASELINE)

## Goal

Stable, accurate focus timer

---

### Tasks

* [ ] T1.1 Implement `TimerService` with:

  * Coroutine-based ticker (1-second updates)
  * Foreground service lifecycle

* [ ] T1.2 Define `TimerState`:

  * status (IDLE, ACTIVE, PAUSED, FINISHED)
  * remainingSeconds
  * elapsedSeconds
  * currentSessionId

* [ ] T1.3 Implement session lifecycle:

  * startSession()
  * pauseSession()
  * resumeSession()
  * endSession()

---

### Checkpoint

* Timer continues in background
* Timer resumes correctly after pause

---

# 🟢 PHASE 2 — SESSION STORAGE

## Goal

Persist sessions for tracking

---

### Tasks

* [ ] T2.1 Create `FocusSessionEntity`

* [ ] T2.2 Implement Room DAO:

  * insertSession()
  * updateSession()
  * getSessions()

* [ ] T2.3 Create `SessionRepository`

---

### Checkpoint

* Session saved after completion
* Data persists across app restarts

---

# 🟢 PHASE 3 — DISTRACTION DETECTION

## Goal

Detect user leaving focus context

---

### Tasks

* [ ] T3.1 Implement `DistractionMonitor` using UsageStatsManager

* [ ] T3.2 Detect:

  * app switches
  * screen unlock events

* [ ] T3.3 Emit events via Flow:

  * AppSwitch
  * ScreenUnlock
  * UserReturned

* [ ] T3.4 Store `DistractionEvent` in DB

---

### Checkpoint

* App switch detected within ~3 seconds
* Distractions correctly logged

---

# 🔥 PHASE 4 — FOCUS ENFORCEMENT (CRITICAL)

## Goal

Convert detection into behavior enforcement

---

### Tasks

* [ ] T4.1 Add `FocusStrictness` enum:

  * RELAXED
  * STRICT
  * HARDCORE

* [ ] T4.2 Extend `SessionConfig`:

  * add `focusStrictness`

* [ ] T4.3 Update `TimerService`:

  * On distraction event:

    * RELAXED → log only
    * STRICT → pause session
    * HARDCORE → end session

* [ ] T4.4 Show warning event (UI hook)

---

### Checkpoint

* Distraction triggers correct behavior
* STRICT pauses
* HARDCORE ends session

---

# 🔥 PHASE 5 — SESSION OUTCOME SYSTEM

## Goal

Convert raw data into meaningful result

---

### Tasks

* [ ] T5.1 Add `SessionOutcome` enum:

  * CLEAN
  * INTERRUPTED
  * FAILED

* [ ] T5.2 Add field to `FocusSessionEntity`

* [ ] T5.3 Implement outcome logic:

  * CLEAN → no distractions
  * INTERRUPTED → few distractions
  * FAILED → excessive distractions or forced end

* [ ] T5.4 Compute outcome in `CompleteSessionUseCase`

---

### Checkpoint

* Every session has outcome
* Outcome matches behavior

---

# 🟢 PHASE 6 — BASIC FEEDBACK SYSTEM

## Goal

Provide immediate user feedback

---

### Tasks

* [ ] T6.1 Update Timer UI:

  * show distraction count
  * show warning on distraction

* [ ] T6.2 Update session completion screen:

  * show session outcome
  * show distraction summary

* [ ] T6.3 Add simple feedback messages:

  * “Clean session”
  * “You had X distractions”

---

### Checkpoint

* User understands session quality
* Feedback shown immediately

---

# 🟢 PHASE 7 — MOTIVATION SYSTEM (LIGHT)

## Goal

Encourage continued usage

---

### Tasks

* [ ] T7.1 Update XP logic:

  * reward clean sessions more

* [ ] T7.2 Implement streak tracking

* [ ] T7.3 Highlight clean sessions in UI

---

### Checkpoint

* XP updates correctly
* Streak increments daily

---

# 🟢 PHASE 8 — MINIMAL HISTORY

## Goal

Allow user to review past sessions

---

### Tasks

* [ ] T8.1 Build history screen:

  * list sessions
  * show:

    * duration
    * outcome
    * tag

* [ ] T8.2 Add session detail view:

  * show distraction breakdown

---

### Checkpoint

* Sessions visible
* Outcomes visible

---

# ❌ EXPLICITLY OUT OF SCOPE (V1)

Do NOT implement:

* Advanced analytics charts
* Smart insights engine
* Habit detection
* Reminders
* App blocking (AccessibilityService)
* Social features

---

# 📊 FINAL MVP CHECKLIST

Before marking V1 complete:

* [ ] User can start session in ≤ 2 actions
* [ ] Distraction is detected reliably
* [ ] System reacts to distraction (STRICT/HARDCORE)
* [ ] Session outcome is always computed
* [ ] User receives clear feedback
* [ ] UI remains simple

---

# 🏁 DEFINITION OF DONE

V1 is complete when:

> The app actively helps users **stay focused and understand their focus quality**

---

# 🚀 NEXT STEP (AFTER V1)

Proceed to:

* V2 (Intelligence Layer)

---
