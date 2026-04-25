# Implementation Plan: Focus + Behavior Tracking System

**Branch**: `001-focus-behavior-tracker` | **Date**: 2026-04-24 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-focus-behavior-tracker/spec.md`

## Summary

An Android-first Focus Timer application combining a foreground-service countdown timer, `UsageStatsManager`-based distraction detection, local Room database analytics, and gamification (XP + streaks + badges). Built with Jetpack Compose + Material 3, Hilt DI, Room 2.6, and WorkManager. V1 ships User Stories 1–5; V2 adds smart reminders and leaderboard.

## Technical Context

**Language/Version**: Kotlin 1.9 / JVM 17  
**Primary Dependencies**: Jetpack Compose (BOM), Material 3, Navigation Compose, Hilt 2.x, Room 2.6, WorkManager 2.9, DataStore Preferences 1.0, Coroutines 1.7, Timber 5.0  
**Storage**: Room (SQLite) for sessions/distractions/gamification; DataStore Preferences for app settings  
**Testing**: JUnit 4 + Robolectric (unit), Turbine (Flow testing), Espresso (instrumented)  
**Target Platform**: Android, minSdk 26 (Android 8.0), targetSdk 35 (Android 15)  
**Project Type**: Mobile app (single-module Android project under `android/`)  
**Performance Goals**: Session start ≤ 10 s from app open (SC-001); distraction logged ≤ 5 s of interruption (SC-002); analytics screen load ≤ 2 s (SC-005)  
**Constraints**: Offline-capable (local DB only in V1); no cloud sync; free tier limited to 7-day analytics; permissions degraded gracefully when denied  
**Scale/Scope**: Single user, local device; thousands of session rows; ~10 Compose screens; V1 = US1–US5, V2 = US6 + leaderboard

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Status**: Constitution v1.0.0 ratified 2026-04-25. All gates pass.

| Gate | Status | Notes |
|------|--------|-------|
| No unnecessary abstractions (§VI) | ✅ PASS | Repository interfaces defined once; no layer duplication |
| Permissions degraded gracefully (§IV) | ✅ PASS | All 4 permissions have explicit fallback behavior (research.md §9) |
| App blocking out of scope V1/V2 (§VII) | ✅ PASS | FR-010 explicitly deferred to V3 |
| Offline-first (§III) | ✅ PASS | Room local DB; no cloud dependency in V1 |
| Single module (§I) | ✅ PASS | `android/app` is a single-module project |
| Domain-first / no cross-layer imports (§II) | ✅ PASS | TimerService uses FocusPreferences interface (C7 fix); domain has zero Android imports |
| V1 scope gate (§VII) | ✅ PASS | Phase 8 (US6) correctly marked V2; all V2/V3 items blocked |
| Free-tier enforcement at DAO layer | ✅ PASS | `clampStartForTier()` implemented in `SessionRepositoryImpl` (Fix Pass 4 — C2) |

## Project Structure

### Documentation (this feature)

```text
specs/001-focus-behavior-tracker/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output — all decisions resolved
├── data-model.md        # Phase 1 output — entities, DAOs, DataStore keys
├── quickstart.md        # Phase 1 output — setup, build, key entry points
├── contracts/
│   └── navigation-contract.md   # Screen routes, TimerService + DistractionMonitor contracts
└── tasks.md             # Phase 2 output — all tasks T001–T089 organized by phase
```

### Source Code (repository root)

```text
android/
├── app/
│   └── src/
│       ├── main/
│       │   ├── kotlin/com/focusapp/
│       │   │   ├── FocusApplication.kt          # @HiltAndroidApp + Timber init
│       │   │   ├── data/
│       │   │   │   ├── db/
│       │   │   │   │   ├── FocusDatabase.kt
│       │   │   │   │   ├── entity/              # Room entities (FocusSession, DistractionEvent, UserProfile, Badge, Reminder)
│       │   │   │   │   └── dao/                 # DAOs (FocusSessionDao, DistractionEventDao, UserProfileDao, BadgeDao, ReminderDao)
│       │   │   │   ├── repository/              # Impl classes (SessionRepositoryImpl, UserProfileRepositoryImpl, etc.)
│       │   │   │   └── prefs/
│       │   │   │       └── AppPreferences.kt    # DataStore wrapper
│       │   │   ├── domain/
│       │   │   │   ├── model/                   # Pure Kotlin data classes (FocusSession, DistractionEvent, SessionConfig, etc.)
│       │   │   │   ├── repository/              # Repository interfaces
│       │   │   │   └── usecase/                 # Use cases (StartSession, CompleteSession, LogDistraction, AwardXp, etc.)
│       │   │   ├── di/
│       │   │   │   ├── DatabaseModule.kt        # Hilt: DB, DAOs, repository bindings
│       │   │   │   └── PreferencesModule.kt     # Hilt: DataStore singleton
│       │   │   ├── service/
│       │   │   │   ├── TimerService.kt          # Foreground service: countdown + distraction monitoring
│       │   │   │   ├── DistractionMonitor.kt    # UsageStatsManager polling (every 4 s)
│       │   │   │   └── ScreenUnlockReceiver.kt  # BroadcastReceiver for ACTION_USER_PRESENT
│       │   │   ├── worker/
│       │   │   │   ├── ReminderWorker.kt        # V2: daily goal reminder
│       │   │   │   ├── ReminderReceiver.kt      # V2: habit alarm receiver
│       │   │   │   └── BootReceiver.kt          # Re-schedules reminders after reboot
│       │   │   └── ui/
│       │   │       ├── MainActivity.kt          # Single Activity, NavHost host
│       │   │       ├── AppNavGraph.kt           # Bottom nav + route definitions
│       │   │       ├── timer/                   # TimerScreen, TimerViewModel, ModeSelector, CustomDurationPicker
│       │   │       ├── analytics/               # AnalyticsScreen, AnalyticsViewModel, FocusBarChart, FocusScoreCard, BestTimeInsightCard
│       │   │       ├── history/                 # HistoryScreen, HistoryViewModel, SessionRow, SessionDetailScreen, SessionDetailViewModel
│       │   │       ├── profile/                 # ProfileScreen, ProfileViewModel, StreakCounter, BadgeGallery, BadgeAwardedDialog
│       │   │       ├── settings/                # SettingsScreen, SettingsViewModel, UsagePermissionCard
│       │   │       └── onboarding/              # OnboardingScreen, OnboardingViewModel
│       │   ├── res/                             # drawables, strings, themes
│       │   └── AndroidManifest.xml
│       ├── test/                                # JUnit + Robolectric unit tests
│       └── androidTest/                         # Espresso + Room migration tests
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

**Structure Decision**: Option 3 (Mobile app) — single Android module. All source lives under `android/app/src/main/kotlin/com/focusapp/`. Domain layer is pure Kotlin (no Android dependencies) enabling Robolectric unit testing without an emulator.

## Complexity Tracking

> No constitution violations to justify.

---

## Phase Execution Summary

### Phase 0: Research ✅ COMPLETE

See `research.md`. All decisions resolved:

| Topic | Decision |
|-------|----------|
| Distraction detection | `UsageStatsManager.queryEvents()` + 4-second polling in foreground service |
| Timer architecture | `Foreground Service` + coroutine ticker + `StateFlow` to UI |
| Database | Room 2.6 + Kotlin Coroutines + Flow |
| UI framework | Jetpack Compose + Material 3 |
| DI | Hilt (Dagger) |
| Reminders | WorkManager (goal) + AlarmManager exact (habit) |
| Focus Score | `clamp(completedRatio×70 + distractionFreeRatio×30, 0, 100)` |
| XP | `floor(sessionMinutes / 5) × 10` |
| SDK targets | minSdk 26, targetSdk 35 |

### Phase 1: Design & Contracts ✅ COMPLETE

| Artifact | Path | Status |
|----------|------|--------|
| Data model | `specs/001-focus-behavior-tracker/data-model.md` | ✅ |
| Navigation contract | `specs/001-focus-behavior-tracker/contracts/navigation-contract.md` | ✅ |
| Quickstart | `specs/001-focus-behavior-tracker/quickstart.md` | ✅ |
| Tasks | `specs/001-focus-behavior-tracker/tasks.md` | ✅ |

### Phase 2: Implementation Tasks

All tasks defined in `tasks.md`. Implementation phases:

| Phase | Scope | Status |
|-------|-------|--------|
| Phase 1 (tasks.md) | Setup — project skeleton, Gradle, Manifest, Nav scaffold (T001–T005) | ✅ DONE |
| Phase 2 (tasks.md) | Foundational — DB, entities, DAOs, repositories, DI (T006–T026) | ✅ DONE |
| Phase 3 (tasks.md) | US1 Smart Focus Timer — TimerService, TimerViewModel, TimerScreen (T027–T036) | ✅ DONE |
| Phase 4 (tasks.md) | US2 Distraction Detection — DistractionMonitor, ScreenUnlockReceiver, warning UI (T037–T046) | ✅ DONE |
| Phase 5 (tasks.md) | US3 Analytics Dashboard — charts, Focus Score, best-time insight (T047–T055) | ✅ DONE |
| Phase 6 (tasks.md) | US4 Gamification — XP, streaks, badges, ProfileScreen (T056–T064) | ✅ DONE |
| Phase 7 (tasks.md) | US5 Study Mode + Tagging — tag input, history filter (T065–T072) | ✅ DONE |
| Phase 8 (tasks.md) | US6 Smart Reminders — WorkManager, AlarmManager, SettingsScreen (T073–T080) | ⬜ V2 |
| Phase N (tasks.md) | Polish — onboarding, empty states, accessibility, boot receiver (T081–T089) | ✅ DONE |

#### Phase 4 Detail: US2 Distraction Detection (T037–T046)

**Prerequisite**: Phase 3 (TimerService running) must be complete first.

**Goal**: App detects app-switches and screen unlocks during active sessions, logs them as distraction events, and shows a warning when the user returns.

**Key implementation decisions (from research.md)**:
- `UsageStatsManager.queryEvents()` polled every **4 seconds** inside `DistractionMonitor.kt`
- `ACTION_USER_PRESENT` broadcast via `ScreenUnlockReceiver.kt` for screen unlock detection
- `DistractionMonitor` is instantiated and torn down by `TimerService` — no separate process
- `LogDistractionUseCase` writes `DistractionEvent` row + updates denormalized `FocusSession.distractionCount` and `distractionTotalSeconds`
- Warning banner (`DistractionWarningBanner.kt`) is a slide-in Compose animation, auto-dismisses after 4 s

**Tasks**:

| Task | Class | Description |
|------|-------|-------------|
| T037 | `service/DistractionMonitor.kt` | UsageStats polling; emits `DistractionDetected`, `UserReturned` via SharedFlow |
| T038 | `service/ScreenUnlockReceiver.kt` | BroadcastReceiver for `ACTION_USER_PRESENT` → emit `ScreenUnlock` |
| T039 | `service/TimerService.kt` | Wire DistractionMonitor; collect flow; call LogDistractionUseCase on UserReturned |
| T040 | `domain/usecase/LogDistractionUseCase.kt` | Insert DistractionEvent + update session denormalized fields |
| T041 | `data/repository/DistractionRepositoryImpl.kt` | Backed by DistractionEventDao; add to DatabaseModule |
| T042 | `ui/timer/DistractionWarningBanner.kt` | Animated slide-in banner; auto-dismiss 4 s |
| T043 | `ui/settings/UsagePermissionCard.kt` | Permission status card; deep-link to system Usage Access settings |
| T044 | `ui/timer/TimerScreen.kt` | Add distraction count badge below countdown |
| T045 | `ui/history/SessionDetailScreen.kt` | Session summary + distraction event timeline |
| T046 | `ui/history/SessionDetailViewModel.kt` | Fetch session + distractions by ID |

**Checkpoint**: Start session → switch to Instagram → return → warning dialog appears with elapsed distraction time → DistractionEvent row confirmed in Room DB.
