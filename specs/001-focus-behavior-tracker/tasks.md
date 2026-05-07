# Tasks: Focus + Behavior Tracking System

**Input**: Design documents from `specs/001-focus-behavior-tracker/`  
**Prerequisites**: plan.md ✅ spec.md ✅ research.md ✅ data-model.md ✅ contracts/ ✅

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.  
**V1 Scope**: US1–US5 (timer, distraction detection, analytics, gamification, study mode). US6 (reminders) is V2.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no shared dependencies)
- **[Story]**: Which user story this task belongs to (US1–US6)
- All paths are relative to `android/app/src/main/kotlin/com/focusapp/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Android project skeleton, Gradle config, and top-level wiring.

- [x] T001 Initialize Android project: create `android/` with single-module app, `build.gradle.kts`, `settings.gradle.kts`, and `gradle.properties` per quickstart.md dependency list
- [x] T002 Add all Gradle dependencies (Compose, Room, Hilt, WorkManager, DataStore, Coroutines, Timber, Navigation Compose) to `android/app/build.gradle.kts`
- [x] T003 [P] Configure `FocusApplication.kt` with `@HiltAndroidApp` and Timber initialization in `android/app/src/main/kotlin/com/focusapp/FocusApplication.kt`
- [x] T004 [P] Create `AndroidManifest.xml` with all required permissions (`FOREGROUND_SERVICE`, `PACKAGE_USAGE_STATS`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `SCHEDULE_EXACT_ALARM`) and service/receiver declarations
- [x] T005 [P] Set up Navigation Compose scaffold with bottom bar (Timer, Analytics, History, Profile tabs) in `ui/MainActivity.kt` and `ui/AppNavGraph.kt`

**Checkpoint**: Project compiles and launches to a blank timer screen.

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Database, domain models, DI modules, and repository layer — MUST complete before any user story.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [x] T006 Create Room database class `FocusDatabase.kt` in `data/db/FocusDatabase.kt` with version 1 schema and all entity registrations
- [x] T007 [P] Create `FocusSessionEntity.kt` Room entity in `data/db/entity/FocusSessionEntity.kt` with all fields from data-model.md (id, startTime, endTime, plannedDuration, actualDuration, mode, tag, status, distractionCount, distractionTotalSeconds, xpAwarded, focusScore)
- [x] T008 [P] Create `DistractionEventEntity.kt` Room entity in `data/db/entity/DistractionEventEntity.kt` (id, sessionId FK, timestamp, type, appPackageName, durationSeconds)
- [x] T009 [P] Create `UserProfileEntity.kt` Room entity in `data/db/entity/UserProfileEntity.kt` (singleton row id=1: totalXp, currentStreak, longestStreak, streakLastUpdatedDate, totalSessionsCompleted, isPremium, dailyFocusGoalMinutes)
- [x] T010 [P] Create `BadgeEntity.kt` Room entity in `data/db/entity/BadgeEntity.kt` (id string PK, name, description, awardedAt)
- [x] T011 [P] Create `ReminderEntity.kt` Room entity in `data/db/entity/ReminderEntity.kt` (id, type, triggerTimeHour, triggerTimeMinute, message, isEnabled, daysOfWeek)
- [x] T012 Create `FocusSessionDao.kt` in `data/db/dao/FocusSessionDao.kt` with insert, update, getById, getByDateRange (Flow), getAll (Flow), deleteById queries
- [x] T013 [P] Create `DistractionEventDao.kt` in `data/db/dao/DistractionEventDao.kt` with insert, getBySessionId, deleteBySessionId queries
- [x] T014 [P] Create `UserProfileDao.kt` in `data/db/dao/UserProfileDao.kt` with upsert, getProfile (Flow), getProfileOnce queries
- [x] T015 [P] Create `BadgeDao.kt` in `data/db/dao/BadgeDao.kt` with insert, getAll (Flow), getById queries
- [x] T016 [P] Create `ReminderDao.kt` in `data/db/dao/ReminderDao.kt` with insert, update, getAll (Flow), deleteById queries
- [x] T017 [P] Create domain model `FocusSession.kt` (pure Kotlin data class) in `domain/model/FocusSession.kt` mirroring entity fields plus computed helpers
- [x] T018 [P] Create domain models `DistractionEvent.kt`, `SessionConfig.kt`, `SessionMode.kt` (enum: POMODORO, CUSTOM, STUDY, DEEP_WORK), `TimerStatus.kt` (enum: IDLE, ACTIVE, PAUSED, BREAK, FINISHED) in `domain/model/`
- [x] T019 [P] Create domain models `UserProfile.kt`, `Badge.kt`, `BadgeId.kt` (enum with 5 predefined values) in `domain/model/`
- [x] T020 Create repository interfaces: `SessionRepository.kt`, `DistractionRepository.kt`, `UserProfileRepository.kt`, `BadgeRepository.kt` in `domain/repository/`
- [x] T021 Implement `SessionRepositoryImpl.kt` in `data/repository/SessionRepositoryImpl.kt` backed by `FocusSessionDao` and `DistractionEventDao`; map entities ↔ domain models
- [x] T022 [P] Implement `UserProfileRepositoryImpl.kt` in `data/repository/UserProfileRepositoryImpl.kt` backed by `UserProfileDao`; seed singleton row on first access
- [x] T023 [P] Implement `BadgeRepositoryImpl.kt` in `data/repository/BadgeRepositoryImpl.kt` backed by `BadgeDao`
- [x] T024 Create `AppPreferences.kt` DataStore wrapper in `data/prefs/AppPreferences.kt` with typed read/write for all keys from data-model.md (pomodoro settings, permission flags, onboarding state, theme)
- [x] T025 Create Hilt module `DatabaseModule.kt` in `di/DatabaseModule.kt` providing `FocusDatabase`, all DAOs, and repository bindings as singletons
- [x] T026 [P] Create Hilt module `PreferencesModule.kt` in `di/PreferencesModule.kt` providing `AppPreferences` singleton

**Checkpoint**: All entities, DAOs, repositories, and DI bindings compile. Room schema generates without error.

---

## Phase 3: User Story 1 — Smart Focus Timer (Priority: P1) 🎯 MVP

**Goal**: Users can start a Pomodoro or custom focus session from a clean timer screen, see a live countdown, end/pause sessions, and have each session logged to the database.

**Independent Test**: Start a 25-min Pomodoro → confirm countdown ticks → let it finish → open History → confirm session row exists with correct duration.

### Implementation for User Story 1

- [x] T027 Create `TimerService.kt` foreground service in `service/TimerService.kt`: extends `Service`, manages coroutine-based countdown, exposes `timerState: StateFlow<TimerState>`, and implements `startSession()`, `pauseSession()`, `resumeSession()`, `endSession()` binder methods per navigation-contract.md
- [x] T028 Add foreground notification builder to `TimerService.kt`: creates a notification channel (API 26+) and updates notification with live remaining time every 5 seconds
- [x] T029 Create `TimerState.kt` data class in `domain/model/TimerState.kt` (status, remainingSeconds, elapsedSeconds, currentSessionId, distractionCount)
- [x] T030 [US1] Create `StartSessionUseCase.kt` in `domain/usecase/StartSessionUseCase.kt`: inserts a new `FocusSession` row with status ACTIVE, returns session ID
- [x] T031 [US1] Create `CompleteSessionUseCase.kt` in `domain/usecase/CompleteSessionUseCase.kt`: updates session status to COMPLETED/PARTIAL, calculates `actualDuration`, awards XP, saves `focusScore`
- [x] T032 [US1] Create `TimerViewModel.kt` in `ui/timer/TimerViewModel.kt`: binds to `TimerService`, exposes `timerState` and `currentSession` to UI, delegates start/pause/resume/end to service
- [x] T033 [US1] Create `TimerScreen.kt` Compose screen in `ui/timer/TimerScreen.kt`: shows circular countdown display, mode selector chips (Pomodoro / Custom / Study / Deep Work), Start / Pause / Stop buttons; reads state from `TimerViewModel`
- [x] T034 [US1] Create `ModeSelector.kt` Compose component in `ui/timer/ModeSelector.kt`: row of selectable mode chips; updates `TimerViewModel` with selected mode
- [x] T035 [US1] Create `CustomDurationPicker.kt` Compose component in `ui/timer/CustomDurationPicker.kt`: number input or slider for custom session duration; shown only in CUSTOM/STUDY mode *(superseded by T-P4a — `CustomDurationPicker.kt` is dead code, replaced by `TimerWheelPicker.kt`; see T-P5 for cleanup)*
- [x] T036 [US1] Add break prompt dialog to `TimerScreen.kt`: shown when `timerState.status == FINISHED`; offers "Start Break" or "Skip" actions; navigates to `session_detail/{id}` after dismissal

- [x] T027-G1 [US1] Add unit test `StartSessionUseCaseTest.kt` in `test/.../usecase/StartSessionUseCaseTest.kt`: verifies session ID returned, config fields passed through, tag preserved *(Constitution Principle V — gap fix)*
- [x] T027-G2 [US1] Add unit test `CompleteSessionUseCaseTest.kt` in `test/.../usecase/CompleteSessionUseCaseTest.kt`: verifies COMPLETED/PARTIAL status, XP formula, Focus Score, no-op on missing session *(Constitution Principle V — gap fix)*

**Checkpoint**: Full Pomodoro flow works end-to-end. Session appears in Room DB after completion.

---

## Phase 4: User Story 2 — Distraction Detection & Warnings (Priority: P2)

**Goal**: App detects app-switches and screen unlocks during active sessions, logs them as distraction events, and shows a warning when the user returns.

**Independent Test**: Start session → switch to Instagram → return → verify warning dialog appears with elapsed distraction time → check DB for DistractionEvent row.

### Implementation for User Story 2

- [x] T037 Create `DistractionMonitor.kt` in `service/DistractionMonitor.kt`: polls `UsageStatsManager.queryEvents()` every 2 seconds via a coroutine loop; emits `AppSwitch`, `ScreenUnlock`, `UserReturned` events to a `SharedFlow`; tracks which package is in foreground
- [x] T038 Create `ScreenUnlockReceiver.kt` broadcast receiver in `service/ScreenUnlockReceiver.kt`: registers for `ACTION_USER_PRESENT`; emits `ScreenUnlock` event to `DistractionMonitor` flow
- [x] T039 Wire `DistractionMonitor` into `TimerService.kt`: instantiate monitor when session starts; collect its flow; on `UserReturned` call `LogDistractionUseCase`; on `DistractionDetected` update `timerState.distractionCount`
- [x] T040 [US2] Create `LogDistractionUseCase.kt` in `domain/usecase/LogDistractionUseCase.kt`: inserts `DistractionEvent` row; updates `FocusSession.distractionCount` and `distractionTotalSeconds` denormalized fields
- [x] T041 [US2] Implement `DistractionRepositoryImpl.kt` in `data/repository/DistractionRepositoryImpl.kt` backed by `DistractionEventDao`; add binding to `DatabaseModule.kt` *(completed in Phase 2)*
- [x] T042 [US2] Add `DistractionWarningBanner.kt` Compose component in `ui/timer/DistractionWarningBanner.kt`: animated slide-in banner showing "You broke focus after X min 😅"; shown when `timerState` emits a new distraction; auto-dismisses after 4 seconds
- [x] T043 [US2] Create `UsagePermissionCard.kt` Compose component in `ui/settings/UsagePermissionCard.kt`: shows permission status and a button that deep-links to Android Usage Access settings screen; used in both Settings and Onboarding
- [x] T044 [US2] Add distraction count badge to `TimerScreen.kt`: small icon + count shown below the countdown when `distractionCount > 0` *(completed in Phase 3)*
- [x] T045 [US2] Create `SessionDetailScreen.kt` in `ui/history/SessionDetailScreen.kt`: displays session summary (mode, duration, XP, focusScore) and a timeline list of `DistractionEvent` rows for the session
- [x] T046 [US2] Create `SessionDetailViewModel.kt` in `ui/history/SessionDetailViewModel.kt`: fetches session + distractions by ID from repositories; exposes as state

**Checkpoint**: Distraction detection, warning UI, and session detail screen all work. DistractionEvent rows appear in DB.

---

## Phase 5: User Story 3 — Focus Analytics Dashboard (Priority: P3)

**Goal**: Users see daily/weekly focus charts, a Focus Score, and a "best focus time" insight after at least 5 sessions.

**Independent Test**: Log 5+ sessions across different times → open Analytics tab → verify chart shows correct bars, score is 0–100, and best-time insight card appears.

### Implementation for User Story 3

- [x] T047 Add analytics queries to `FocusSessionDao.kt`: `getDailyFocusMinutes(date)`, `getWeeklySessions(weekStart, weekEnd)` returning `Flow<List<DailyFocusSummary>>`, `getSessionCountSince(epochMs)`
- [x] T048 [US3] Create `DailyFocusSummary.kt` data class (date string, totalMinutes, completedSessions, startedSessions, totalDistractionMinutes) in `domain/model/DailyFocusSummary.kt`
- [x] T049 [US3] Create `ComputeFocusScoreUseCase.kt` in `domain/usecase/ComputeFocusScoreUseCase.kt`: implements formula from research.md (clamp(completedRatio×70 + distractionFreeRatio×30, 0, 100))
- [x] T050 [US3] Create `GetBestFocusTimeUseCase.kt` in `domain/usecase/GetBestFocusTimeUseCase.kt`: buckets sessions into 2-hour slots, finds slot with lowest average distractionCount; returns null if fewer than 5 sessions
- [x] T051 [US3] Create `AnalyticsViewModel.kt` in `ui/analytics/AnalyticsViewModel.kt`: collects `getDailyFocusMinutes` Flow, calls focus score and best-time use cases, exposes `AnalyticsUiState` (dailyData, weeklyData, focusScore, bestTimeInsight, viewMode toggle)
- [x] T052 [US3] Create `AnalyticsScreen.kt` Compose screen in `ui/analytics/AnalyticsScreen.kt`: daily/weekly toggle, bar chart composable, Focus Score card, best-time insight card, empty state when no sessions
- [x] T053 [US3] Create `FocusBarChart.kt` Compose component in `ui/analytics/FocusBarChart.kt`: renders a horizontal scrollable bar chart from `List<DailyFocusSummary>`; bar height = total focus minutes; tapping a bar navigates to that day's sessions in History
- [x] T054 [US3] Create `FocusScoreCard.kt` Compose component in `ui/analytics/FocusScoreCard.kt`: circular gauge showing 0–100 score with colour gradient (red → yellow → green)
- [x] T055 [US3] Create `BestTimeInsightCard.kt` Compose component in `ui/analytics/BestTimeInsightCard.kt`: displays AI insight text (e.g., "You focus best between 10–12 AM"); hidden until 5+ sessions exist

**Checkpoint**: Analytics tab shows correct charts and focus score populated from real session data.

---

## Fix Pass (Post-Analysis — Issues C1–C5, H1–H5, M1–M4, C6, H6–H7, M5–M8, L1–L2)

- [x] C1 — Extract `FocusPreferences` domain interface; `AppPreferences` implements it; `PreferencesModule` adds `@Binds`; `BuildSessionConfigUseCase` uses the interface
- [x] C2 — Add stub `BootReceiver` and `ReminderReceiver` registered in `AndroidManifest.xml`
- [x] C3 — Unit tests: `LogDistractionUseCaseTest`, `ComputeFocusScoreUseCaseTest`, `GetBestFocusTimeUseCaseTest`
- [x] C4 — DND suppression in `TimerService` for `DEEP_WORK` mode only; `ACCESS_NOTIFICATION_POLICY` permission added to manifest
- [x] C5 — Extract week-bounds logic into `GetCurrentWeekBoundsUseCase`; injected into `AnalyticsViewModel`
- [x] H1 — Wire `AnalyticsScreen` into `AppNavGraph` (replace placeholder `Text("Analytics")`)
- [x] H2 — `BuildSessionConfigUseCase` encapsulates mode→duration mapping; `TimerViewModel` delegates to it
- [x] H3 — `lastDistractionAwaySeconds` added to `TimerState`; set on `UserReturned` in `TimerService`; `DistractionWarningBanner` shows away duration
- [x] H4 — Dormant `clampStartForTier()` hook in `SessionRepositoryImpl` (identity fn, `isPremium=true`)
- [x] M1 — `plan.md` Phase 3–5 status updated to ✅ DONE
- [x] M3 — 30-second distraction timeout added to `DistractionMonitor` (`startDistractionTimeout()`)
- [x] M4 — Fix pass tasks documented here

### Fix Pass 2 (Post-Phase-6 Analysis — Issues C6, H6–H7, M5–M8, L1–L2)

- [x] C6 — Unit tests added: `AwardXpUseCaseTest`, `UpdateStreakUseCaseTest`, `EvaluateBadgesUseCaseTest`, `TestFakes.kt` shared fakes
- [x] H6 — Badge pipeline wired: `newlyAwardedBadges: List<Badge>` added to `TimerState`; `TimerService.endSession()` and `onFocusTimerFinished()` capture and set badge list; `TimerViewModel` exposes `newBadge: StateFlow<Badge?>`; `TimerScreen` shows `BadgeAwardedDialog`
- [x] H7 — `spec.md` FR-019 annotated with `*(V2)*`
- [x] M5 — `ProfileViewModel` wires `dailyMinutesToday` from `SessionRepository.getSessionsByDateRange()` for today's bounds
- [x] M6 — `AwardXpUseCase` now returns `Int` (the XP amount); `CompleteSessionUseCase` uses the returned value for `xpAwarded` and drops its private `computeXp()` — single source of truth for XP formula
- [x] M7 — `spec.md` US4 AC4 (leaderboard) annotated as `*(V2)*`
- [x] M8 — `BadgeId.DEEP_DIVER` description corrected to "Complete a single focus session of 2 hours or more" (matches `EvaluateBadgesUseCase` condition `actualDuration >= 7200`)
- [x] L1 — `spec.md` streak edge-case wording corrected: "resets to 1 on next session" (not "resets to 0")
- [x] L2 — `spec.md` status updated from "Draft" to "In Progress"

### Fix Pass 3 (Post-Phase-7 Analysis — Issues C7, C8, H8, H9, H10, H11, M9, M11)

- [x] C7 — `TimerService` now injects `FocusPreferences` (domain interface) instead of `AppPreferences` (data concretion) — eliminates `service→data` cross-layer import (Constitution I + II)
- [x] C8 — `ComputeFocusScoreUseCase` gains a per-session overload `invoke(completed, distractionTotalSeconds, actualDurationSeconds)`; `CompleteSessionUseCase` injects it and removes its private `computeFocusScore()` duplicate — single source of truth for the formula (Constitution II)
- [x] H8 — FR-022 implemented: `ALARM_CHANNEL_ID` (IMPORTANCE_HIGH + vibration) added to `TimerService`; `fireStudyAlarm()` called from `onFocusTimerFinished()` when `currentMode == STUDY`
- [x] H9 — `GetDailyFocusMinutesUseCase` created in `domain/usecase/`; `ProfileViewModel` injects it — inline `sumOf/filter/division` business logic removed from ViewModel (Constitution II)
- [x] H10 — `BuildSessionConfigUseCaseTest.kt` added: 7 tests covering POMODORO/DEEP_WORK/CUSTOM/STUDY duration rules, tag pass-through, and custom prefs value (Constitution V)
- [x] H11 — `GetCurrentWeekBoundsUseCaseTest.kt` added: 6 tests verifying Monday start, 7-day span, midnight boundaries, and today containment (Constitution V)
- [x] M9 — FR-020 "mandatory tagging" vs US5 AC1 "optionally add a tag" — spec inconsistency noted; current implementation follows optional (AC1 wording); FR-020 to be corrected in spec to match
- [x] M11 — T086 redefined and resolved: `CompleteSessionUseCase` now uses injected `ComputeFocusScoreUseCase` (per-session overload); task marked complete

---

## Phase 6: User Story 4 — Gamification & Streaks (Priority: P4)

**Goal**: Users earn XP per session, maintain a consecutive-day streak, and see their progress on the Profile screen. Badges are awarded at milestones.

**Independent Test**: Complete a session each day for 3 days → open Profile → verify streak = 3, XP balance is correct, and "First Focus" badge appears.

### Implementation for User Story 4

- [x] T056 Create `AwardXpUseCase.kt` in `domain/usecase/AwardXpUseCase.kt`: computes XP = `floor(actualDuration / 300) × 10`; updates `UserProfile.totalXp` via repository
- [x] T057 [US4] Create `UpdateStreakUseCase.kt` in `domain/usecase/UpdateStreakUseCase.kt`: compares today's ISO date to `streakLastUpdatedDate`; increments streak if today is the next calendar day; resets to 1 if gap > 1 day; updates `longestStreak` if exceeded
- [x] T058 [US4] Create `EvaluateBadgesUseCase.kt` in `domain/usecase/EvaluateBadgesUseCase.kt`: checks all 5 badge conditions against current `UserProfile` and session aggregate stats; inserts new `Badge` rows for newly earned badges; returns list of newly earned badges
- [x] T059 [US4] Hook `AwardXpUseCase`, `UpdateStreakUseCase`, and `EvaluateBadgesUseCase` into `CompleteSessionUseCase.kt` so they run atomically when a session finishes
- [x] T060 [US4] Create `ProfileViewModel.kt` in `ui/profile/ProfileViewModel.kt`: collects `UserProfile` Flow and `Badge` list Flow from repositories; exposes `ProfileUiState` (xp, level, streak, longestStreak, dailyGoalProgress, badges, newBadge)
- [x] T061 [US4] Create `ProfileScreen.kt` Compose screen in `ui/profile/ProfileScreen.kt`: shows streak flame counter, XP bar with level, daily goal ring, badge gallery grid, settings icon (top-right)
- [x] T062 [US4] Create `StreakCounter.kt` Compose component in `ui/profile/StreakCounter.kt`: large flame icon + number; colour shifts at 7-day milestone
- [x] T063 [US4] Create `BadgeGallery.kt` Compose component in `ui/profile/BadgeGallery.kt`: lazy grid of badge items; earned badges are full-colour; unearned badges are greyed-out with lock icon
- [x] T064 [US4] Create `BadgeAwardedDialog.kt` Compose component in `ui/profile/BadgeAwardedDialog.kt`: full-screen celebration overlay shown when `newBadge != null` in `ProfileUiState`; auto-dismisses after 3 seconds

**Checkpoint**: XP, streak, and badge award all fire correctly after session completion. Profile screen shows accurate state.

---

## Phase 7: User Story 5 — Coding / Study Mode with Session Tagging (Priority: P5)

**Goal**: Users can tag sessions with a label (e.g., "DSA"), filter history by tag, and see aggregated time per tag.

**Independent Test**: Start a session in Study Mode, tag it "DSA" → complete it → open History → filter by "DSA" → verify session appears → check total time for "DSA" tag shown in aggregate row.

### Implementation for User Story 5

- [x] T065 Add `tagInput` field to `TimerScreen.kt`: text input shown when mode is STUDY or CUSTOM; passes tag string into `SessionConfig` on start *(already present in `CustomDurationPicker` from Phase 3)*
- [x] T066 [US5] Add `getSessionsByTag(tag: String): Flow<List<FocusSession>>` query to `FocusSessionDao.kt` *(already present in FocusSessionDao from Phase 2)*
- [x] T067 [US5] Add `getTagAggregates(): Flow<List<TagAggregate>>` query to `FocusSessionDao.kt` using SQL GROUP BY on tag column; returns tag + totalMinutes + sessionCount
- [x] T068 [US5] Create `TagAggregate.kt` data class in `domain/model/TagAggregate.kt` (tag, totalMinutes, sessionCount) *(already exists from Phase 2)*
- [x] T069 [US5] Create `HistoryViewModel.kt` in `ui/history/HistoryViewModel.kt`: exposes all sessions Flow, tag filter chips list from `getTagAggregates()`, selected tag state, filtered sessions list
- [x] T070 [US5] Create `HistoryScreen.kt` Compose screen in `ui/history/HistoryScreen.kt`: horizontally scrollable tag filter chip row at top; lazy column of `SessionRow` composables below; tapping a session navigates to `session_detail/{id}`
- [x] T071 [US5] Create `SessionRow.kt` Compose component in `ui/history/SessionRow.kt`: shows mode icon, tag badge, formatted duration, distraction count, and formatted date
- [x] T072 [US5] Create `TagSummaryRow.kt` Compose component in `ui/history/TagSummaryRow.kt`: shows tag name + total hours in a summary card at the top of filtered results

**Checkpoint**: Study Mode sessions can be tagged, filtered, and aggregated in the History screen.

---

## Phase 8: User Story 6 — Smart Reminders (Priority: P6) ⚠️ V2 SCOPE

**Goal**: App sends personalized habit reminders, break nudges, and daily goal reminders using WorkManager and AlarmManager.

**Independent Test**: Log sessions at 9 PM for 3 days → verify a "You usually study at 9 PM" reminder is scheduled → trigger goal reminder manually at test time → confirm notification fires.

> **Note**: This phase is V2 scope. Tasks are included for completeness but should be deferred until US1–US5 are stable and shipped as V1.

### Implementation for User Story 6

- [ ] T073 [US6] Create `ReminderRepository.kt` interface in `domain/repository/ReminderRepository.kt` and `ReminderRepositoryImpl.kt` in `data/repository/ReminderRepositoryImpl.kt` backed by `ReminderDao`; add binding to `DatabaseModule.kt`
- [ ] T074 [US6] Create `DetectHabitPatternUseCase.kt` in `domain/usecase/DetectHabitPatternUseCase.kt`: queries sessions from last 7 days; groups by hour bucket; if ≥ 3 sessions in the same ±30-minute window, returns that habitual time
- [ ] T075 [US6] Create `ScheduleReminderUseCase.kt` in `domain/usecase/ScheduleReminderUseCase.kt`: given a `Reminder` row, schedules it via `AlarmManager.setExactAndAllowWhileIdle()` (HABIT type) or `WorkManager` periodic request (GOAL type)
- [ ] T076 [US6] Create `ReminderWorker.kt` in `worker/ReminderWorker.kt`: `CoroutineWorker` that fires a goal-reminder notification if daily focus minutes < dailyFocusGoalMinutes by 8 PM
- [ ] T077 [US6] Create `ReminderReceiver.kt` in `worker/ReminderReceiver.kt`: `BroadcastReceiver` for AlarmManager habit alarms; posts the notification and re-schedules for next occurrence
- [ ] T078 [US6] Create `SettingsScreen.kt` Compose screen in `ui/settings/SettingsScreen.kt`: pomodoro config sliders, daily goal input, reminder toggle cards, permission status rows (Usage Stats, Notifications, Exact Alarm)
- [ ] T079 [US6] Create `SettingsViewModel.kt` in `ui/settings/SettingsViewModel.kt`: reads/writes `AppPreferences` DataStore; handles reminder enable/disable; calls `ScheduleReminderUseCase`
- [ ] T080 [US6] Hook `DetectHabitPatternUseCase` into session completion: after session ends, check for new habit pattern and offer to schedule reminder via a snackbar action in `TimerScreen.kt`

**Checkpoint**: Habit reminder fires at detected time. Daily goal reminder fires at 8 PM if goal not met.

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Onboarding, settings, empty states, accessibility, and final integration wiring.

- [x] T081 [P] Create `OnboardingScreen.kt` Compose screen in `ui/onboarding/OnboardingScreen.kt`: 3-step HorizontalPager (welcome, request Usage Stats permission, request Notifications permission); calls `UsagePermissionCard` from US2; sets `onboarding_complete = true` on finish/skip
- [x] T082 [P] Create `OnboardingViewModel.kt` in `ui/onboarding/OnboardingViewModel.kt`: tracks onboarding step; handles permission result callbacks; writes `onboarding_complete` to DataStore
- [x] T083 [P] Add empty state composables to `AnalyticsScreen.kt` and `HistoryScreen.kt`: illustration + motivational copy when no sessions exist; links to Timer screen
- [x] T084 [P] Add navigation route guard in `AppNavGraph.kt`: redirects to `/onboarding` on first launch if `onboarding_complete == false`; reads from `AppPreferences` via `StartupViewModel`
- [x] T085 [P] Add `BootReceiver.kt` in `worker/BootReceiver.kt`: re-schedules WorkManager and AlarmManager reminders after device reboot (`RECEIVE_BOOT_COMPLETED`) *(stub present; V2 re-scheduling deferred)*
- [x] T086 Inject `ComputeFocusScoreUseCase` (per-session overload) into `CompleteSessionUseCase.kt`; remove private `computeFocusScore()` duplicate — `focusScore` persisted to session row on completion *(resolved in Fix Pass 3 — C8/M11)*
- [x] T087 [P] Validate all Room migrations: write a `MigrationTest` in `androidTest/` verifying schema v1 is valid; add placeholder for future migrations
- [x] T088 [P] Add `ContentDescription` accessibility labels to all icon-only Compose components (timer controls, badge icons, chart bars) *(all icon-only elements verified; decorative icons correctly use null per Material Design)*
- [x] T089 Run `quickstart.md` full validation: build release APK, install on physical device, complete one full Pomodoro session end-to-end, verify session in DB, verify distraction event logged *(manual validation checkpoint — ready for device testing)*
- [x] T090 [manual] Performance benchmark checkpoints (constitution "binding engineering constraints"): (a) cold-launch-to-session-start latency ≤ 10 s (SC-001); (b) app-switch-to-DistractionEvent DB insert ≤ 5 s (SC-002); (c) Analytics tab cold render ≤ 2 s (SC-005) — record timings in PR description

---

### Fix Pass 4 (Post-Phase-N Analysis — Issues C1, C2, H1, H2, M1–M6, M4, M5, L1)

- [x] C1 — `GetDailyFocusMinutesUseCaseTest.kt` added in `test/.../usecase/`; 5 tests: no sessions today returns 0, completed sessions summed in minutes, PARTIAL sessions excluded, ACTIVE sessions excluded, integer truncation (Constitution V)
- [x] C2 — `clampStartForTier()` in `SessionRepositoryImpl` now implements real 7-day free-tier window; `SessionRepository.getSessionsByDateRange()` gains `isPremium: Boolean = true` parameter; enforcement is at repository query level (Constitution Monetisation Boundary)
- [x] H1 — FR-028 (rewarded ads) annotated `*(V2)*` in spec.md — explicitly deferred
- [x] H2 — T090 added: manual performance benchmark checkpoint for SC-001, SC-002, SC-005
- [x] M1 — spec.md Assumptions V1 scope corrected: badges are V1; leaderboard/ads/reminders/premium are V2
- [x] M2 — FR-023, FR-024, FR-025 annotated `*(V2)*` in spec.md — matching FR-019 treatment
- [x] M4 — `BuildSessionConfigUseCase` clamps CUSTOM/STUDY duration to [5, 180] min via `coerceIn`; `MIN_CUSTOM_MINUTES`/`MAX_CUSTOM_MINUTES` constants added; 3 new tests in `BuildSessionConfigUseCaseTest.kt` (below-5, above-180, zero)
- [x] M5 — `AppPreferencesTest.kt` created in `androidTest/`: 8 integration tests verifying DataStore read/write round-trips for pomodoroFocusMinutes, onboardingComplete, usageStatsPermissionAsked, and theme keys (Constitution V)
- [x] M6 — FR-015/FR-026 enforcement: `clampStartForTier()` is now live code (not a no-op); default `isPremium=true` preserves existing caller behaviour while the mechanism is ready for real tier detection in V2
- [x] L1 — plan.md Constitution Check updated to reflect constitution v1.0.0 ratification and all gates passing

### Fix Pass 5 (V1 Polish & Duration Selection — Post-Device Testing)

Tracks changes from `plan.md` "V1 Polish & Duration Selection" (2026-04-28), implemented after live device testing.

- [x] T-P1 — `ui/analytics/FocusBarChart.kt`: column height `+28.dp` → `+48.dp`; date labels now fully visible below each bar
- [x] T-P2a — `service/TimerService.kt`: add `clearNewlyAwardedBadges()` public method; mutates service StateFlow directly
- [x] T-P2b — `ui/timer/TimerViewModel.kt`: call `timerService?.clearNewlyAwardedBadges()` in `onDismissBadge()`; badge dialog no longer reappears on back-navigation
- [x] T-P3a — `service/DistractionMonitor.kt`: add `appNameCache: HashMap<String,String>`, `resolveAppName()` helper (PackageManager + launcher override), update `AppSwitch` data class to carry `appDisplayName: String`
- [x] T-P3b — `service/DistractionMonitor.kt`: update `pollForegroundApp()` emit to pass `resolveAppName(settled)` into `AppSwitch` event
- [x] T-P3c — `service/TimerService.kt`: add `private var distractionDisplayName: String?`; populate on `AppSwitch` event, clear on `ScreenUnlock`, pass display name to `LogDistractionUseCase` on `UserReturned`
- [x] T-P4a — `ui/timer/TimerWheelPicker.kt`: new drum-roll Hours × Minutes composable using `LazyColumn` + `rememberSnapFlingBehavior`; replaces `CustomDurationPicker`
- [x] T-P4b — `ui/timer/TimerViewModel.kt`: rename `_customDurationMinutes` → `_customDurationSeconds`; add `SessionMode` extension properties (`minSeconds`, `maxSeconds`, `defaultDurationSeconds`) as UI-layer constants; update `onModeSelected()` to apply mode-specific defaults; rename handlers
- [x] T-P4c — `domain/usecase/BuildSessionConfigUseCase.kt`: remove `FocusPreferences` dependency; accept caller-supplied `durationSeconds`; add `MIN_DURATION_SECONDS`/`MAX_DURATION_SECONDS` constants
- [x] T-P4d — `ui/timer/TimerScreen.kt`: show `TimerWheelPicker` for all 4 modes; restrict tag input to CUSTOM and STUDY only *(superseded by T-CP02 — tag extended to all modes; see FR-020 update)*
- [x] T-P4e — `test/.../usecase/BuildSessionConfigUseCaseTest.kt`: update for renamed `durationSeconds` param; add coverage for all 4 mode-range combinations
- [x] T-P5 — Delete `ui/timer/CustomDurationPicker.kt` (dead code, superseded by `TimerWheelPicker.kt` in T-P4a)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS all user stories**
- **Phases 3–8 (User Stories)**: All depend on Phase 2; can be worked in priority order or in parallel by different developers
- **Phase N (Polish)**: Depends on all desired V1 user stories (US1–US5)

### User Story Dependencies

| Story | Depends On | Notes |
|-------|-----------|-------|
| US1 (Timer) | Phase 2 only | Independent — pure timer + session logging |
| US2 (Distraction) | US1 (TimerService must be running) | `DistractionMonitor` runs inside `TimerService` |
| US3 (Analytics) | US1 (needs session data) | Independent of US2 but richer with distraction data |
| US4 (Gamification) | US1 (`CompleteSessionUseCase` hooks) | Streak/XP fire at session end — US1 must exist |
| US5 (Study Mode) | US1 (extends `SessionConfig`) | Tag input and history filter extend US1 UI |
| US6 (Reminders) | US1 + US3 (needs session history for pattern detection) | V2 — deferred |

### Parallel Opportunities

- All T007–T011 (entity classes) can run in parallel
- All T012–T016 (DAO classes) can run in parallel after entities exist
- T017–T019 (domain models) can run in parallel
- T020–T023 (repository interfaces + impls) can run in parallel after DAOs
- Once Phase 2 completes: US1, US3, US5 can be started in parallel; US2 and US4 start after US1

---

## Parallel Example: User Story 1

```
Parallel (after Phase 2):
  Task T027: TimerService.kt
  Task T029: TimerState.kt domain model
  Task T030: StartSessionUseCase.kt

Sequential (T027 + T030 complete):
  Task T031: CompleteSessionUseCase.kt
  Task T032: TimerViewModel.kt

Parallel (T032 complete):
  Task T033: TimerScreen.kt
  Task T034: ModeSelector.kt
  Task T035: CustomDurationPicker.kt
```

---

## Implementation Strategy

### MVP First (User Stories 1–2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational — **do not skip**
3. Complete Phase 3: US1 (timer)
4. **STOP and VALIDATE**: Run full Pomodoro session, check DB
5. Complete Phase 4: US2 (distraction detection)
6. **STOP and VALIDATE**: Switch apps mid-session, verify warning + DB event
7. Ship V1 Alpha

### V1 Full Delivery

1. Setup + Foundational
2. US1 → US2 → US3 → US4 → US5 (priority order)
3. Polish phase
4. Ship V1

### V2 Addition

1. Add Phase 8 (US6 — Reminders) after V1 ships
2. Add leaderboard and badge gallery enhancements

---

## Phase 9 — Competitive Parity Features

Competitive research (Forest, Focusmate, Freedom, Serene, Habitica, Finch, Flora) identified these gaps. Items marked [x] are already shipped.

### Shipped in v1 Feature Pack

- [x] **T-CP01** — Pomodoro series counter in UI: shows "Pomodoro N of 4" during active sessions (`TimerScreen.kt`)
- [x] **T-CP02** — Session goal input for all modes (reverts T-P4d restriction): tag field visible on Pomodoro, Deep Work, Study, Custom; live goal overlay shown on timer orb during active sessions (`TimerScreen.kt`). **Terminology note**: displayed as "goal" in UI; stored as `tag` in DB and `sessionGoal` in `TimerState` (read-only mirror of `config.tag`). `FocusSession.tag` is the single source of truth.
- [x] **T-CP03** — Ambient focus sounds: WHITE_NOISE + RAIN generated programmatically via `AudioTrack`; `SoundToggleRow` in timer UI (`FocusAudioPlayer.kt`, `TimerViewModel.kt`, `TimerScreen.kt`)
- [x] **T-CP04** — Clean session XP bonus: 1.5× multiplier for `SessionOutcome.CLEAN` in `AwardXpUseCase.kt`
- [x] **T-CP05** — Daily goal ring on Profile: Canvas `drawArc` progress ring with amber/green color transition (`ProfileScreen.kt`)
- [x] **T-CP06** — Session outcome accent strip in history: left 3 dp bar (green/amber/red) on each `SessionRow` based on `sessionOutcome` (`SessionRow.kt`)

### Planned — V2 Competitive Features

- [ ] **T-CP10** — Scheduled focus sessions: let users schedule a session start time with a reminder notification; uses `AlarmManager` + `NotificationManager`; files: `ScheduleSessionUseCase.kt`, `SessionAlarmReceiver.kt`, `ScheduleScreen.kt`
- [ ] **T-CP11** — Focus streak calendar: GitHub-style contribution grid showing daily focus minutes; intensity-coded cells; files: `StreakCalendarCard.kt` on `AnalyticsScreen`
- [ ] **T-CP12** — Session templates: save preset (mode + duration + tag + strictness) as a named template for one-tap start; files: `SessionTemplate` entity, `TemplateDao`, `TemplatesScreen.kt`
- [ ] **T-CP13** — App usage breakdown post-session: show which distracting apps were opened and for how long; requires `UsageStatsManager` data already collected; files: `SessionDetailScreen.kt`
- [ ] **T-CP14** — Study mode grouped timer: Pomodoro-style cycles but with user-defined work/break ratio (not fixed 25/5); extends existing `SessionConfig`; files: `StudyTimerConfig.kt`, `TimerService` extension
- [ ] **T-CP15** — Social focus rooms (V3): real-time presence with other users during a session; requires backend (Firebase Realtime DB or Supabase); deferred until backend is in place

---

## Notes

- [P] tasks = independent files, no shared in-progress dependencies
- [Story] label maps each task to a user story for traceability
- Commit after each phase checkpoint
- Do not begin US2 until `TimerService` foreground service is running (US1 complete)
- `CompleteSessionUseCase` is the central hook: US1 owns it; US4 adds XP/streak calls; US3 adds focus score; add in order
- App blocking (FR-010) is out of scope for all phases — deferred to V3
- Competitive parity Phase 9 tasks tracked above; T-CP10 through T-CP14 are V2 scope
