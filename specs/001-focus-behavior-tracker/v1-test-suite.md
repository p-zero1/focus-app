# V1 Test Suite — Focus Behavior Tracker

**Branch**: `001-focus-behavior-tracker`  
**Date**: 2026-05-07  
**Unit test count**: 107 passing, 0 failing  

---

## Part 1 — Automated Unit Tests (107 tests, all passing)

These run with `.\gradlew :app:testDebugUnitTest` from `/android/`.

| Test file | Tests | What it covers |
|-----------|-------|----------------|
| `AwardXpUseCaseTest` | 14 | Base XP formula, CLEAN 1.5× bonus, INTERRUPTED/FAILED no-bonus, zero-XP short sessions |
| `CompleteSessionUseCaseTest` | 7 | Session finalisation: status, XP, focus score, not-found no-op |
| `BuildSessionConfigUseCaseTest` | — | Session config construction for all 4 modes |
| `ComputeFocusScoreUseCaseTest` | — | 0–100 score formula: completion + distraction ratio |
| `EvaluateBadgesUseCaseTest` | — | Badge trigger conditions for each `BadgeId` |
| `GetBestFocusTimeUseCaseTest` | — | Best-hour ranking across sessions |
| `GetCurrentWeekBoundsUseCaseTest` | — | Week boundary calculation |
| `GetDailyFocusMinutesUseCaseTest` | — | Daily minute aggregation |
| `LogDistractionUseCaseTest` | — | Distraction event persistence |
| `StartSessionUseCaseTest` | — | Session creation and ID return |
| `UpdateStreakUseCaseTest` | — | Streak increment and reset logic |
| `TimerStateNewFieldTest` | — | `sessionGoal` and `sessionOutcome` default-null on IDLE state |
| `DistractionBannerTextTest` | — | Banner text for appName / awaySeconds / fallback |
| `FocusAudioPlayerTest` | 13 | SoundMode enum, label mapping, goalPlaceholder for all modes, idempotency |
| `TopFocusBreakersAggregationTest` | — | Tag/app distraction aggregation for SessionDetailScreen |

---

## Part 2 — Manual Device Tests

Run on a physical Android device (API 26+). Mark each row ✅ PASS / ❌ FAIL / ⚠️ SKIP.

### US1 — Smart Focus Timer

| # | Scenario | Steps | Expected | Result |
|---|----------|-------|----------|--------|
| 1.1 | Pomodoro start | Select Pomodoro → Start | 25-min countdown begins; DND enabled (Deep Work only) | |
| 1.2 | Timer finishes naturally | Let Pomodoro run to zero | Vibration/sound; session logged; Break prompt appears | |
| 1.3 | Custom duration | Select Custom → set 45 min → Start | Countdown shows 45:00 and counts down correctly | |
| 1.4 | Manual stop | Start any session → Stop | Dialog confirms; partial session logged with actual duration | |
| 1.5 | Dismiss break → new session | Complete session → skip break | Timer returns to IDLE; new session can be started immediately | |
| 1.6 | Orb size animation | Session transitions IDLE → ACTIVE | Orb animates from 200dp to 260dp over ~500ms | |
| 1.7 | Background countdown | Start session → home screen → return | Countdown continues; notification shows remaining time | |
| 1.8 | Deep Work DND | Select Deep Work → Start | DND enabled (INTERRUPTION_FILTER_NONE) while active; disabled on end | |
| 1.9 | Study alarm | Select Study mode → let timer finish | Alarm-style notification fires with vibration even in background | |

### US2 — Distraction Detection & Warnings

| # | Scenario | Steps | Expected | Result |
|---|----------|-------|----------|--------|
| 2.1 | App-switch detection | Start session → switch to browser → return | Warning banner slides in: "Switched to [app] — stay focused!" | |
| 2.2 | Screen-unlock detection | Start session → lock screen → unlock → wait 30s | Distraction recorded; warning shown on return | |
| 2.3 | Banner dismiss | Banner visible → tap it | Banner slides out; session continues | |
| 2.4 | Distraction count in history | Complete session with 2 distractions → History | Session row shows ⚠️ 2 count | |
| 2.5 | Distraction detail | Tap session in History → detail screen | Shows distraction events with timestamps and app names | |
| 2.6 | STRICT mode pause | STRICT + session active + switch app | Session pauses; warning shown; resumes on return | |
| 2.7 | HARDCORE mode force-end | HARDCORE + session active + switch app | Session ends immediately; outcome = FAILED | |

### US3 — Analytics Dashboard

| # | Scenario | Steps | Expected | Result |
|---|----------|-------|----------|--------|
| 3.1 | Empty state | Fresh install → Analytics tab | "No sessions yet" message + "Start a session" button | |
| 3.2 | Weekly bar chart | Complete 3 sessions → Analytics | Bar chart renders with bars for session days | |
| 3.3 | Focus Score display | Complete sessions with/without distractions | Score card shows 0–100 value | |
| 3.4 | Best time insight | Sessions at different times of day | BestTimeInsightCard shows correct peak hour | |
| 3.5 | Weekly ↔ Daily toggle | Analytics → tap toggle | PillToggle switches between "This Week" and "Last 7 Days" views; chart updates | |

### US4 — Gamification & Streaks

| # | Scenario | Steps | Expected | Result |
|---|----------|-------|----------|--------|
| 4.1 | XP on clean session | Complete 25-min session with 0 distractions | Profile shows +75 XP (25min × 10/5min × 1.5 CLEAN) | |
| 4.2 | XP on interrupted session | Complete 25-min session with 1 distraction | Profile shows +50 XP (no CLEAN bonus) | |
| 4.3 | XP proportional | Complete 10-min session (CLEAN) | Profile shows +15 XP (10min ÷ 5min × 10 × 1.5 = 15) | |
| 4.4 | Streak increment | Complete session day 1; complete session day 2 | Streak shows 2 on day 2 | |
| 4.5 | Badge notification | Trigger badge condition (e.g., first session) | BadgeAwardedDialog appears; badge saved to Profile | |
| 4.6 | Daily goal ring | Profile → goal ring | Ring shows today's minutes / goal minutes; turns green when complete | |
| 4.7 | XP level bar | Earn XP → Profile | Amber gradient XP bar fills; level updates at 500 XP threshold | |

### US5 — Session Tagging / Study Mode

| # | Scenario | Steps | Expected | Result |
|---|----------|-------|----------|--------|
| 5.1 | Tag a session | Any mode → tag field → type "DSA" → Start | Session starts with tag "DSA" | |
| 5.2 | Tag in history | Complete tagged session → History | Session row shows purple "DSA" chip | |
| 5.3 | Tag in goal overlay | Session active with tag → timer orb area | Goal overlay shows tag text on orb | |
| 5.4 | Study mode countdown | Study mode → set 90 min → Start | Countdown from 90:00 | |

### Phase 9 Competitive Features

| # | Scenario | Steps | Expected | Result |
|---|----------|-------|----------|--------|
| 9.1 | Pomodoro counter | Complete 3 Pomodoro intervals | Counter on orb shows "🍅 3" | |
| 9.2 | Pomodoro long break | Complete 4th interval (default setting) | Long break (15 min) offered instead of short (5 min) | |
| 9.3 | Ambient sounds — White Noise | Session active → Sound toggle → White Noise | Continuous white noise plays via AudioTrack | |
| 9.4 | Ambient sounds — Rain | Session active → Sound toggle → Rain | Rain sound plays | |
| 9.5 | Ambient sounds — Off | Sound playing → toggle Off | Audio stops immediately | |
| 9.6 | Session outcome strip — CLEAN | Complete session with 0 distractions → History | Session row has green left-side accent strip | |
| 9.7 | Session outcome strip — INTERRUPTED | Complete session with 1–3 distractions → History | Session row has amber accent strip | |
| 9.8 | Session outcome strip — FAILED | HARDCORE + forced end → History | Session row has red accent strip | |

---

## Part 3 — Icon & Branding Tests

| # | Scenario | Expected | Result |
|---|----------|----------|--------|
| I.1 | Launcher icon — standard | Long-press app → icon is purple gradient (top-left #9D94FF → bottom-right #3A2F8F) with white F monogram | |
| I.2 | Launcher icon — round | Devices with round icon masks → icon is correct round variant | |
| I.3 | Adaptive icon safe zone | Icon in a launcher that clips to circle/squircle | F monogram is fully visible, not clipped | |
| I.4 | Themed icon (Android 13+) | System "Themed icons" setting ON → Focus icon | Monochrome F takes wallpaper accent color | |
| I.5 | In-app logo | Any screen that uses `ic_app_logo` | Self-contained rounded square with gradient bg + white F | |

---

## Part 4 — Regression Tests (Known Bug Areas)

| # | Regression | Test | Expected | Result |
|---|-----------|------|----------|--------|
| R.1 | Duplicate height import (TimerScreen.kt line 68 removed) | Build compiles without warnings | No "duplicate import" Kotlin warning | |
| R.2 | sessionGoal unification | Start Pomodoro session with tag → check `TimerState.sessionGoal` | Equals `config.tag` value, not a separate field | |
| R.3 | `SessionOutcome` not persisted before session ends | DB inspection mid-session | `sessionOutcome` column is null until `CompleteSessionUseCase` writes it | |
| R.4 | FocusAudioPlayer idempotency | Call `play(WHITE_NOISE)` twice | Second call is a no-op; no double AudioTrack | |
| R.5 | CLEAN XP test regression | Run `AwardXpUseCaseTest` | 75 XP for 25-min CLEAN; 50 XP for 25-min INTERRUPTED | |

---

## Part 5 — Edge Cases

| # | Edge case | Steps | Expected | Result |
|---|-----------|-------|----------|--------|
| E.1 | Very short session (<5 min) | Start + immediately stop (2 min) | 0 XP awarded; session still logged with 120s actual duration | |
| E.2 | 0-minute goal | Goal set to 0 in preferences | Daily ring doesn't divide by zero; shows 0/0 or default | |
| E.3 | Session with no tag | Start without entering tag | Goal overlay and history row omit the tag chip entirely | |
| E.4 | Multiple distractions quickly | Switch apps 5 times in 10s | `distractionCount` increments correctly; outcome = FAILED (>3) | |
| E.5 | App killed mid-session | Force-stop app during active session | Session NOT auto-completed; on next launch, session marked PARTIAL or left as ACTIVE depending on DB state | |
| E.6 | Analytics with single session | Complete exactly 1 session | Chart renders single bar; focus score shows; best-time shows | |

---

## How to Run

**Automated tests:**
```powershell
cd android
.\gradlew :app:testDebugUnitTest
# Report: android/app/build/reports/tests/testDebugUnitTest/index.html
```

**Device tests:**
1. Build debug APK: `.\gradlew :app:assembleDebug`
2. Install: `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Work through Part 2–5 manually, marking each row Pass/Fail

**Pass criteria for V1 release:**
- All 107 automated tests pass
- All P1 (US1) manual tests pass
- All P2 (US2) manual tests pass  
- No CRITICAL bugs in P3–P5
- Icon tests I.1–I.3 pass
- Regression tests R.1–R.5 pass
