# V1 Test Suite

## How to run automated tests

```powershell
# Unit tests (no device needed) — 107 tests
cd android
.\gradlew :app:testDebugUnitTest

# UI + integration tests (device/emulator required)
.\gradlew :app:connectedAndroidTest
```

`connectedAndroidTest` covers:
| Suite | What it checks |
|-------|---------------|
| `AppLaunchSmokeTest` | App launches, all 4 bottom-nav tabs reachable, empty states |
| `SessionRowTest` | All 4 modes, all 4 outcome strip states, tag chip, distraction count, click |
| `DistractionBannerTest` | Hidden by default, app-name text, away-seconds text, fallback text, dismiss |
| `FocusOrbTest` | Renders without crash in every TimerStatus × distraction combination |
| `StreakCounterTest` | Streak number, label, accessibility description, zero and large values |
| `MigrationTest` | Room schema v1 is valid |
| `AppPreferencesTest` | DataStore defaults and round-trip reads/writes |

---

## Manual test checklist

Mark each row **✅ PASS** or **❌ FAIL**. Stop and fix ❌ before shipping.

### Core timer flows (must all pass for V1)

| # | What to do | What you should see |
|---|-----------|---------------------|
| T1 | Select **Pomodoro** → tap **Start** | Orb expands to ~260dp, countdown from 25:00, elapsed "0:00 elapsed" below |
| T2 | While T1 is running → tap **Pause** | Countdown freezes; orb dims slightly |
| T3 | Tap **Resume** | Countdown continues from where it stopped |
| T4 | Tap **Stop** → confirm | Session logged in History with actual duration (not 25:00) |
| T5 | Select **Custom** → set duration to **5 minutes** → Start → let it finish | Break prompt dialog appears; History shows ~300s session with XP awarded |
| T6 | Dismiss the break prompt | Timer returns to IDLE immediately, new session can start |

### Distraction detection (requires usage-stats permission granted)

| # | What to do | What you should see |
|---|-----------|---------------------|
| D1 | Start any session → switch to another app (e.g. Chrome) → return | Red warning banner slides in from top: "Switched to Chrome — stay focused!" |
| D2 | Tap the banner | Banner slides out; session continues |
| D3 | Complete the session that had D1 distraction → check History | Session row has **amber** left-side accent strip; ⚠️ 1 shown |
| D4 | Complete a session with **0** distractions → check History | Session row has **green** left-side accent strip |

### Gamification

| # | What to do | What you should see |
|---|-----------|---------------------|
| G1 | Complete a **25-min Custom session with no distractions** → Profile | XP bar gained +75 XP (25÷5 × 10 × 1.5 CLEAN bonus) |
| G2 | Complete a **10-min session with 1 distraction** → Profile | XP bar gained +20 XP (10÷5 × 10, no bonus) |
| G3 | Complete a session → Profile → **Daily Goal ring** | Ring arc shows today's minutes vs goal; turns green when goal met |

### Analytics

| # | What to do | What you should see |
|---|-----------|---------------------|
| A1 | Open **Analytics** tab with no sessions | "No sessions yet" empty state + "Start a session" button |
| A2 | After completing 2+ sessions → Analytics | Bar chart renders with bars on the correct days; Focus Score card visible |
| A3 | Tap the **Weekly / Last 7 Days** pill toggle | Chart updates to show the other time range |

### Session tagging

| # | What to do | What you should see |
|---|-----------|---------------------|
| S1 | Start session → enter tag **"DSA"** → Start → complete | History row shows purple **DSA** chip next to mode name |
| S2 | Start session → leave tag blank → complete | History row shows no chip; mode name only |

---

## Edge cases (test at least 2 before shipping)

| # | Scenario | Expected |
|---|----------|----------|
| E1 | Stop a session after only **90 seconds** | 0 XP awarded (< 5 min = no XP); session still in History with 90s duration |
| E2 | Start CUSTOM session, switch app 4+ times → complete | History row has **red** accent strip (FAILED outcome: >3 distractions) |
| E3 | Start session → lock screen → wait 35 seconds → unlock | Distraction recorded; banner appears |
| E4 | Start session → swipe app out of recents (force-kill) → relaunch | App returns to IDLE (no ghost session running); prior session not in History or shown as partial |

---

## What is NOT automatically tested (device-only, human judgment required)

- Countdown accuracy over 25 min (real-time clock)
- DND silencing (Deep Work mode — system permission)
- Audio output (ambient sounds White Noise / Rain)
- Study mode alarm notification in background
- Background service persistence through lock screen / doze
- Adaptive icon appearance on launcher
- Themed icon (Android 13+ wallpaper color tinting)
