# Feature Specification: Focus + Behavior Tracking System

**Feature Branch**: `001-focus-behavior-tracker`  
**Created**: 2026-04-24  
**Status**: In Progress  
**Input**: User description: "A Focus Timer App = Timer + Distraction Detection + Analytics + Gamification"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Smart Focus Timer (Priority: P1)

A user launches the app and starts a focus session. They can choose between a Pomodoro preset (25-minute focus / 5-minute break) or configure a custom session length. During the session, notifications are silenced and they enter Deep Work Mode. When the session ends, the app suggests a break and logs the completed session.

**Why this priority**: The timer is the core feature of the app. Without a working timer that tracks sessions, nothing else (analytics, gamification, distraction detection) has data to act on. It delivers standalone value as a productivity tool.

**Independent Test**: Can be tested by starting a session, letting it run, and confirming that the session is logged and the break prompt appears.

**Acceptance Scenarios**:

1. **Given** the user opens the app, **When** they select Pomodoro mode and press Start, **Then** a 25-minute countdown begins and all non-critical notifications are suppressed.
2. **Given** an active session is running, **When** the timer reaches zero, **Then** the app plays a sound/vibration, logs the session, and prompts the user to start a break.
3. **Given** the user wants a custom session, **When** they set a custom duration (e.g., 45 minutes) and press Start, **Then** the countdown runs for exactly that duration.
4. **Given** an active session, **When** the user manually ends it early, **Then** the partial session is still logged with its actual duration.
5. **Given** a session just ended, **When** the user dismisses the break prompt, **Then** they can immediately start a new session.

---

### User Story 2 - Distraction Detection & Warnings (Priority: P2)

During an active focus session, the user switches to another app (e.g., Instagram) or unlocks their screen mid-session. The app detects this interruption, logs it as a distraction, and shows a motivational warning message when the user returns.

**Why this priority**: Distraction detection is the key differentiator from the hundreds of existing timer apps. It turns the app into an accountability tool, creating real behavior change. It depends on P1 (sessions must be running to detect distractions).

**Independent Test**: Can be tested by starting a session, switching to another app, returning, and verifying the warning message appears and the distraction is counted.

**Acceptance Scenarios**:

1. **Given** an active focus session, **When** the user switches to a different app, **Then** the system logs a distraction event with a timestamp and the name of the app switched to (where available).
2. **Given** a distraction is detected, **When** the user returns to the focus app, **Then** a warning is shown ("You broke focus after 6 min 😅") including how long they were distracted.
3. **Given** an active session, **When** the user unlocks their screen and doesn't return within 30 seconds, **Then** the distraction is recorded.
4. **Given** a completed session, **When** the user views session details, **Then** they can see total distractions, average distraction length, and which apps caused them.

---

### User Story 3 - Focus Analytics Dashboard (Priority: P3)

A user opens the Analytics tab and sees their daily and weekly focus hours visualized as charts. They see a Focus Score that reflects how distraction-free their sessions were. An AI-generated insight tells them their best focus time of day (e.g., "You focus best between 10–12 AM").

**Why this priority**: Analytics is the primary retention driver. Users keep returning to track progress over time. It requires session data from P1 and distraction data from P2 to be meaningful.

**Independent Test**: Can be tested with at least 3 logged sessions by verifying charts render correctly, the focus score calculates properly, and the best-time insight appears.

**Acceptance Scenarios**:

1. **Given** the user has logged sessions, **When** they open the Analytics tab, **Then** they see a bar/line chart of daily focus hours for the current week.
2. **Given** at least 5 sessions logged, **When** the system calculates a Focus Score, **Then** it is a value from 0–100 based on sessions completed vs. distractions incurred.
3. **Given** sessions spanning at least 3 different time slots, **When** the system identifies the best focus time, **Then** it displays a message like "You focus best between 10–12 AM" based on lowest-distraction periods.
4. **Given** the user switches between Daily and Weekly views, **When** they tap the toggle, **Then** the chart updates to show the selected time range.

---

### User Story 4 - Gamification & Streaks (Priority: P4)

A user completes a focus session every day and sees their streak counter increase. They earn XP points for sessions and receive badges for milestones ("2 Hours Deep Work", "7-Day Streak"). They can view a leaderboard to compare streaks with friends.

**Why this priority**: Gamification drives daily retention. It makes the app habit-forming. It depends on session tracking (P1) and benefits from analytics context (P3).

**Independent Test**: Can be tested by completing sessions across multiple days and verifying streak count, XP balance, and badge notifications are awarded correctly.

**Acceptance Scenarios**:

1. **Given** the user completes a focus session, **When** the session ends, **Then** they are awarded XP points proportional to session duration.
2. **Given** the user completes at least one session per day for consecutive days, **When** they open the app the next day, **Then** the streak counter reflects the correct number of consecutive days.
3. **Given** a milestone is reached (e.g., total 2 hours in one day), **When** the system detects it, **Then** a badge notification appears and the badge is saved to the user's profile.
4. *(V2)* **Given** the user has linked their account, **When** they open the Leaderboard, **Then** they see ranked weekly focus hours for themselves and friends/global users.

---

### User Story 5 - Coding / Study Mode with Session Tagging (Priority: P5)

A student preparing for exams opens the app, selects "Study Mode", sets a countdown for 2 hours, tags the session as "DSA Revision", and starts the session. After completing it, they can review all DSA-tagged sessions and their total time invested.

**Why this priority**: This niche feature makes the app highly relevant for students and developers, a large and loyal target segment. It extends P1 with labeling and mode-specific UX.

**Independent Test**: Can be tested by creating a tagged session, completing it, and verifying the tag appears in session history with correct duration.

**Acceptance Scenarios**:

1. **Given** the user selects Study Mode, **When** they configure a session, **Then** they can set a countdown target (e.g., 90 min) and optionally add a tag (e.g., "DSA", "Project X").
2. **Given** a tagged session is completed, **When** the user views session history, **Then** they can filter by tag and see total time per tag.
3. **Given** the user is in Study Mode, **When** the countdown reaches zero, **Then** an alarm-style notification fires (even if app is in background).

---

### User Story 6 - Smart Reminders (Priority: P6)

A user who usually studies around 9 PM receives a push notification at 8:50 PM saying "You usually study at 9 PM — start your focus session?". They also receive break reminders during long sessions and daily goal reminders if they haven't hit their focus target for the day.

**Why this priority**: Reminders re-engage lapsed users and build habits. Depends on session history (P1/P3) to personalize timing.

**Independent Test**: Can be tested by granting notification permissions, logging sessions at consistent times, and verifying the app sends a personalized reminder the next day at the expected time.

**Acceptance Scenarios**:

1. **Given** the user has allowed notifications and logged 3+ sessions at similar times, **When** the app detects a consistent time pattern, **Then** it schedules a reminder 10 minutes before that habitual time.
2. **Given** a focus session is active for more than the user's configured session length, **When** no break has been taken, **Then** the app sends a break nudge notification.
3. **Given** the user has a daily focus goal (e.g., 2 hours) and hasn't met it by 8 PM, **When** the system checks goal status, **Then** it sends a motivational reminder to complete the goal.

---

### Edge Cases

- What happens when the user has no session history yet — analytics and AI insights should show an empty state with onboarding guidance.
- How does the system handle a session that runs beyond midnight — it should be attributed to the day it started.
- What if the user denies notification permissions — distraction detection and smart reminders degrade gracefully; the app remains functional without them.
- What happens if the user pauses a session — paused time should not count toward focus time but the session should not be discarded.
- How does a streak handle a missed day — the streak resets to 1 on the next completed session; there is no grace period unless the user has a premium "streak freeze" feature.
- What if two friends have the same XP — leaderboard shows them tied with the same rank.

## Requirements *(mandatory)*

### Functional Requirements

**Timer & Sessions**

- **FR-001**: System MUST support Pomodoro mode with a default 25-minute focus interval and 5-minute short break, with a 15-minute long break after 4 pomodoros.
- **FR-002**: System MUST allow users to configure custom session durations from 5 minutes to 180 minutes.
- **FR-003**: System MUST suppress non-critical device notifications during an active Deep Work session (where platform permissions allow).
- **FR-004**: System MUST automatically log each completed focus session with start time, end time, duration, mode, and tag.
- **FR-005**: System MUST allow users to manually end a session early; partial sessions are logged with their actual duration.
- **FR-006**: System MUST display a break prompt with an option to accept or skip after each focus interval.

**Distraction Detection**

- **FR-007**: System MUST detect app-switching events during an active focus session and log them as distraction events with a timestamp.
- **FR-008**: System MUST detect screen unlock events during active sessions and log them as distraction events.
- **FR-009**: System MUST display a distraction warning message when the user returns to the app after a detected interruption.
- **FR-010**: App blocking (preventing users from opening specified apps during a session) is out of scope for V1 and V2. It is deferred to V3 as a premium feature. The app delivers distraction detection and warnings without blocking in all earlier versions.
- **FR-011**: Session details MUST include a distraction count and total distraction duration.

**Analytics**

- **FR-012**: System MUST display a daily and weekly view of total focus hours as a visual chart.
- **FR-013**: System MUST calculate and display a Focus Score (0–100) for each day and week, reflecting session completion rate and distraction frequency.
- **FR-014**: System MUST identify and display the user's best focus time-of-day based on historical session data once at least 5 sessions have been logged.
- **FR-015**: Free users MUST be able to access analytics for the last 7 days; premium users access full history.

**Gamification**

- **FR-016**: System MUST award XP points for each completed session: 10 XP per 5 minutes of actual focus time (e.g., 50 XP for a 25-minute session). Formula: `floor(actualDurationSeconds / 300) × 10`.
- **FR-017**: System MUST maintain a consecutive-day streak counter that increments when at least one session is completed per calendar day.
- **FR-018**: System MUST award badges when milestone conditions are met and notify the user in-app.
- **FR-019** *(V2)*: System MUST display a leaderboard showing weekly focus hours for connected friends or global users (requires account creation).

**Study/Coding Mode**

- **FR-020**: System MUST provide a Study Mode with an optional countdown target and optional session tagging (tag field shown; not required to start).
- **FR-021**: System MUST allow users to filter session history by tag and view aggregated time per tag.
- **FR-022**: System MUST fire an alarm-style notification when a Study Mode countdown reaches zero, even when the app is backgrounded.

**Reminders**

- **FR-023** *(V2)*: System MUST detect consistent session-start time patterns (3+ sessions at similar times) and offer to schedule a recurring reminder.
- **FR-024** *(V2)*: System MUST send a break nudge if an active session exceeds the configured max session length.
- **FR-025** *(V2)*: System MUST send a daily goal reminder if the user has not met their daily focus target by a user-configurable evening time (default: 8 PM).

**Monetization**

- **FR-026**: Free users MUST have access to the basic timer (all modes), session history (last 30 days), and 7-day analytics.
- **FR-027** *(V2)*: Premium users MUST have access to full analytics history, app blocking, advanced reports, and theme customization.
- **FR-028** *(V2)*: System MUST support rewarded ads as a free-tier monetization mechanism.

### Key Entities *(include if feature involves data)*

- **FocusSession**: Represents a single focus interval. Attributes: session ID, start time, end time, actual duration, mode (Pomodoro/Custom/Study/Deep Work), tag, distraction count, distraction total duration, XP awarded, status (completed/partial).
- **DistractionEvent**: A detected interruption within a session. Attributes: event ID, session ID, timestamp, type (app-switch/screen-unlock), app name (if available), duration away.
- **UserProfile**: Stores persistent user state. Attributes: user ID, total XP, current streak, streak last-updated date, badges earned, premium status, linked account (optional).
- **Badge**: An achievement awarded on a milestone. Attributes: badge ID, name, description, condition (e.g., "2 hours in one day"), awarded date.
- **Reminder**: A scheduled notification. Attributes: reminder ID, type (habit/break/goal), trigger time, message, recurrence.
- **SessionTag**: A label applied by the user to categorize sessions (e.g., "DSA", "Project X"). Tags are free-form strings.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can start a focus session within 10 seconds of opening the app.
- **SC-002**: Distraction events are detected and logged within 5 seconds of the interruption occurring.
- **SC-003**: At least 70% of users who complete 3 sessions in their first week return to use the app in week 2 (7-day retention).
- **SC-004**: Users with gamification features active complete 40% more sessions per week compared to users without gamification enabled.
- **SC-005**: Analytics dashboard loads and displays session data within 2 seconds.
- **SC-006**: Daily reminder accuracy: at least 90% of scheduled reminders are delivered within 5 minutes of the target time.
- **SC-007**: At least 15% of active free users convert to premium within 3 months of launch.
- **SC-008**: The app achieves a user-reported focus improvement rate of at least 60% among users who complete a 2-week usage streak (measured via in-app survey).

## Assumptions

- **Platform**: This is a mobile application targeting Android first. iOS support is deferred to a later version. App blocking and distraction detection rely on Android-specific accessibility and usage-stats APIs.
- **Authentication**: Account creation is optional. Users can use the app fully without an account except for the leaderboard feature, which requires sign-in.
- **Data storage**: Session data is stored locally on-device. Cloud sync is a premium feature and out of scope for V1.
- **Distraction detection scope**: The app can detect app-switching via foreground app monitoring (requires usage stats permission) and screen unlock events. It cannot detect what users do within a single app (e.g., watching YouTube without switching apps).
- **App blocking (premium)**: App blocking requires the user to grant device administrator or accessibility service permissions. This involves explicit user consent steps.
- **Monetization currency**: Pricing is in INR (₹). Approximate tiers: ₹99–₹299/month for premium. Exact pricing is a business decision outside this spec.
- **Pomodoro defaults**: 25-minute focus, 5-minute short break, 15-minute long break after 4 intervals. All configurable.
- **Focus Score formula**: `clamp(completedRatio × 70 + distractionFreeRatio × 30, 0, 100)` where `completedRatio = actualDuration / plannedDuration` and `distractionFreeRatio = 1 − (distractionTotalSeconds / actualDuration)`. Both ratios are clamped to [0, 1] before application.
- **Leaderboard**: Initially shows a global leaderboard of weekly focus hours. Friend-based leaderboard requires a social graph (deferred to V2).
- **Ads**: Rewarded ads are shown to free users at natural break points (e.g., after a session ends). Interstitial or banner ads are not used to avoid disrupting focus.
- **V1 scope**: Timer (all modes), session tracking, basic distraction detection (app-switch + screen unlock), basic analytics (7-day), streaks, XP, and badges. Leaderboard, app blocking, smart reminders, rewarded ads, and premium features are V2.
