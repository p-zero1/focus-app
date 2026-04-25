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
- [x] T035 [US1] Create `CustomDurationPicker.kt` Compose component in `ui/timer/CustomDurationPicker.kt`: number input or slider for custom session duration; shown only in CUSTOM/STUDY mode
- [x] T036 [US1] Add break prompt dialog to `TimerScreen.kt`: shown when `timerState.status == FINISHED`; offers "Start Break" or "Skip" actions; navigates to `session_detail/{id}` after dismissal

- [x] T027-G1 [US1] Add unit test `StartSessionUseCaseTest.kt` in `test/.../usecase/StartSessionUseCaseTest.kt`: verifies session ID returned, config fields passed through, tag preserved *(Constitution Principle V — gap fix)*
- [x] T027-G2 [US1] Add unit test `CompleteSessionUseCaseTest.kt` in `test/.../usecase/CompleteSessionUseCaseTest.kt`: verifies COMPLETED/PARTIAL status, XP formula, Focus Score, no-op on missing session *(Constitution Principle V — gap fix)*

**Checkpoint**: Full Pomodoro flow works end-to-end. Session appears in Room DB after completion.

---

## Phase 4: User Story 2 — Distraction Detection & Warnings (Priority: P2)

**Goal**: App detects app-switches and screen unlocks during active sessions, logs them as distraction events, and shows a warning when the user returns.

**Independent Test**: Start session → switch to Instagram → return → verify warning dialog appears with elapsed distraction time → check DB for DistractionEvent row.

### Implementation for User Story 2

- [x] T037 Create `DistractionMonitor.kt` in `service/DistractionMonitor.kt`: polls `UsageStatsManager.queryEvents()` every 4 seconds via a coroutine loop; emits `DistractionDetected`, `UserReturned` events to a `SharedFlow`; tracks which package is in foreground
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

- [ ] T047 Add analytics queries to `FocusSessionDao.kt`: `getDailyFocusMinutes(date)`, `getWeeklySessions(weekStart, weekEnd)` returning `Flow<List<DailyFocusSummary>>`, `getSessionCountSince(epochMs)`
- [ ] T048 [US3] Create `DailyFocusSummary.kt` data class (date string, totalMinutes, completedSessions, startedSessions, totalDistractionMinutes) in `domain/model/DailyFocusSummary.kt`
- [ ] T049 [US3] Create `ComputeFocusScoreUseCase.kt` in `domain/usecase/ComputeFocusScoreUseCase.kt`: implements formula from research.md (clamp(completedRatio×70 + distractionFreeRatio×30, 0, 100))
- [ ] T050 [US3] Create `GetBestFocusTimeUseCase.kt` in `domain/usecase/GetBestFocusTimeUseCase.kt`: buckets sessions into 2-hour slots, finds slot with lowest average distractionCount; returns null if fewer than 5 sessions
- [ ] T051 [US3] Create `AnalyticsViewModel.kt` in `ui/analytics/AnalyticsViewModel.kt`: collects `getDailyFocusMinutes` Flow, calls focus score and best-time use cases, exposes `AnalyticsUiState` (dailyData, weeklyData, focusScore, bestTimeInsight, viewMode toggle)
- [ ] T052 [US3] Create `AnalyticsScreen.kt` Compose screen in `ui/analytics/AnalyticsScreen.kt`: daily/weekly toggle, bar chart composable, Focus Score card, best-time insight card, empty state when no sessions
- [ ] T053 [US3] Create `FocusBarChart.kt` Compose component in `ui/analytics/FocusBarChart.kt`: renders a horizontal scrollable bar chart from `List<DailyFocusSummary>`; bar height = total focus minutes; tapping a bar navigates to that day's sessions in History
- [ ] T054 [US3] Create `FocusScoreCard.kt` Compose component in `ui/analytics/FocusScoreCard.kt`: circular gauge showing 0–100 score with colour gradient (red → yellow → green)
- [ ] T055 [US3] Create `BestTimeInsightCard.kt` Compose component in `ui/analytics/BestTimeInsightCard.kt`: displays AI insight text (e.g., "You focus best between 10–12 AM"); hidden until 5+ sessions exist

**Checkpoint**: Analytics tab shows correct charts and focus score populated from real session data.

---

## Phase 6: User Story 4 — Gamification & Streaks (Priority: P4)

**Goal**: Users earn XP per session, maintain a consecutive-day streak, and see their progress on the Profile screen. Badges are awarded at milestones.

**Independent Test**: Complete a session each day for 3 days → open Profile → verify streak = 3, XP balance is correct, and "First Focus" badge appears.

### Implementation for User Story 4

- [ ] T056 Create `AwardXpUseCase.kt` in `domain/usecase/AwardXpUseCase.kt`: computes XP = `floor(actualDuration / 300) × 10`; updates `UserProfile.totalXp` via repository
- [ ] T057 [US4] Create `UpdateStreakUseCase.kt` in `domain/usecase/UpdateStreakUseCase.kt`: compares today's ISO date to `streakLastUpdatedDate`; increments streak if today is the next calendar day; resets to 1 if gap > 1 day; updates `longestStreak` if exceeded
- [ ] T058 [US4] Create `EvaluateBadgesUseCase.kt` in `domain/usecase/EvaluateBadgesUseCase.kt`: checks all 5 badge conditions against current `UserProfile` and session aggregate stats; inserts new `Badge` rows for newly earned badges; returns list of newly earned badges
- [ ] T059 [US4] Hook `AwardXpUseCase`, `UpdateStreakUseCase`, and `EvaluateBadgesUseCase` into `CompleteSessionUseCase.kt` so they run atomically when a session finishes
- [ ] T060 [US4] Create `ProfileViewModel.kt` in `ui/profile/ProfileViewModel.kt`: collects `UserProfile` Flow and `Badge` list Flow from repositories; exposes `ProfileUiState` (xp, level, streak, longestStreak, dailyGoalProgress, badges, newBadge)
- [ ] T061 [US4] Create `ProfileScreen.kt` Compose screen in `ui/profile/ProfileScreen.kt`: shows streak flame counter, XP bar with level, daily goal ring, badge gallery grid, settings icon (top-right)
- [ ] T062 [US4] Create `StreakCounter.kt` Compose component in `ui/profile/StreakCounter.kt`: large flame icon + number; colour shifts at 7-day milestone
- [ ] T063 [US4] Create `BadgeGallery.kt` Compose component in `ui/profile/BadgeGallery.kt`: lazy grid of badge items; earned badges are full-colour; unearned badges are greyed-out with lock icon
- [ ] T064 [US4] Create `BadgeAwardedDialog.kt` Compose component in `ui/profile/BadgeAwardedDialog.kt`: full-screen celebration overlay shown when `newBadge != null` in `ProfileUiState`; auto-dismisses after 3 seconds

**Checkpoint**: XP, streak, and badge award all fire correctly after session completion. Profile screen shows accurate state.

---

## Phase 7: User Story 5 — Coding / Study Mode with Session Tagging (Priority: P5)

**Goal**: Users can tag sessions with a label (e.g., "DSA"), filter history by tag, and see aggregated time per tag.

**Independent Test**: Start a session in Study Mode, tag it "DSA" → complete it → open History → filter by "DSA" → verify session appears → check total time for "DSA" tag shown in aggregate row.

### Implementation for User Story 5

- [ ] T065 Add `tagInput` field to `TimerScreen.kt`: text input shown when mode is STUDY or CUSTOM; passes tag string into `SessionConfig` on start
- [x] T066 [US5] Add `getSessionsByTag(tag: String): Flow<List<FocusSession>>` query to `FocusSessionDao.kt` *(already present in FocusSessionDao from Phase 2)*
- [ ] T067 [US5] Add `getTagAggregates(): Flow<List<TagAggregate>>` query to `FocusSessionDao.kt` using SQL GROUP BY on tag column; returns tag + totalMinutes + sessionCount
- [x] T068 [US5] Create `TagAggregate.kt` data class in `domain/model/TagAggregate.kt` (tag, totalMinutes, sessionCount) *(already exists from Phase 2)*
- [ ] T069 [US5] Create `HistoryViewModel.kt` in `ui/history/HistoryViewModel.kt`: exposes all sessions Flow, tag filter chips list from `getTagAggregates()`, selected tag state, filtered sessions list
- [ ] T070 [US5] Create `HistoryScreen.kt` Compose screen in `ui/history/HistoryScreen.kt`: horizontally scrollable tag filter chip row at top; lazy column of `SessionRow` composables below; tapping a session navigates to `session_detail/{id}`
- [ ] T071 [US5] Create `SessionRow.kt` Compose component in `ui/history/SessionRow.kt`: shows mode icon, tag badge, formatted duration, distraction count, and formatted date
- [ ] T072 [US5] Create `TagSummaryRow.kt` Compose component in `ui/history/TagSummaryRow.kt`: shows tag name + total hours in a summary card at the top of filtered results

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

- [ ] T081 [P] Create `OnboardingScreen.kt` Compose screen in `ui/onboarding/OnboardingScreen.kt`: 3-step HorizontalPager (welcome, request Usage Stats permission, request Notifications permission); calls `UsagePermissionCard` from US2; sets `onboarding_complete = true` on finish/skip
- [ ] T082 [P] Create `OnboardingViewModel.kt` in `ui/onboarding/OnboardingViewModel.kt`: tracks onboarding step; handles permission result callbacks; writes `onboarding_complete` to DataStore
- [ ] T083 [P] Add empty state composables to `AnalyticsScreen.kt` and `HistoryScreen.kt`: illustration + motivational copy when no sessions exist; links to Timer screen
- [ ] T084 [P] Add navigation route guard in `AppNavGraph.kt`: redirects to `/onboarding` on first launch if `onboarding_complete == false`; reads from `AppPreferences` via a startup ViewModel
- [ ] T085 [P] Add `BootReceiver.kt` in `worker/BootReceiver.kt`: re-schedules WorkManager and AlarmManager reminders after device reboot (`RECEIVE_BOOT_COMPLETED`)
- [ ] T086 Add `FocusScoreUseCase` result to `CompleteSessionUseCase.kt`: compute and persist per-session `focusScore` to `FocusSession` row on completion
- [ ] T087 [P] Validate all Room migrations: write a `MigrationTest` in `androidTest/` verifying schema v1 is valid; add placeholder for future migrations
- [ ] T088 [P] Add `ContentDescription` accessibility labels to all icon-only Compose components (timer controls, badge icons, chart bars)
- [ ] T089 Run `quickstart.md` full validation: build release APK, install on physical device, complete one full Pomodoro session end-to-end, verify session in DB, verify distraction event logged

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

## Notes

- [P] tasks = independent files, no shared in-progress dependencies
- [Story] label maps each task to a user story for traceability
- Commit after each phase checkpoint
- Do not begin US2 until `TimerService` foreground service is running (US1 complete)
- `CompleteSessionUseCase` is the central hook: US1 owns it; US4 adds XP/streak calls; US3 adds focus score; add in order
- App blocking (FR-010) is out of scope for all phases — deferred to V3
