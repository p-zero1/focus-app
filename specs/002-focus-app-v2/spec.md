# Feature Specification: Focus App — V2

**Feature Branch**: `002-focus-app-v2`
**Created**: 2026-05-08
**Status**: Draft
**Depends On**: `001-focus-behavior-tracker` (V1 — all features shipped)
**Input**: Authentication, cloud sync, premium monetisation, smart reminders, social layer, compliance

---

## Strategic Context

V1 ships a polished offline focus timer with distraction detection, analytics, and gamification.
V2 transforms it into a **trusted, revenue-generating, cloud-connected** product.

Three drivers:

1. **Trust** — Anonymous local data creates no user lock-in and no reason to return after a device reset. A verified account + cloud sync turns sessions into a personal productivity record the user owns and can access anywhere. Google Sign-In adds an identity layer that signals legitimacy to new users.
2. **Revenue** — V1 has no monetisation. V2 gates advanced features behind a subscription and uses rewarded ads for the free tier, targeting ₹99–299/month with an annual plan that reduces churn.
3. **Retention** — Smart reminders re-engage lapsed users. Social features (leaderboard, sharing) create habit-reinforcing accountability. The Indian market responds strongly to WhatsApp-native sharing and visible social proof.

---

## Scope Boundaries

| In V2 | Deferred to V3 |
|-------|----------------|
| Firebase Auth (Google + email/password) | iOS port |
| Firestore cloud sync (sessions, profile, badges) | App blocking (Accessibility Service) |
| Google Play Billing subscriptions + rewarded ads | Social focus rooms (real-time presence) |
| Premium paywall gating | Custom backend API |
| Smart reminders (habit pattern, break nudge, daily goal) | AI coaching / LLM-powered insights |
| Leaderboard (global weekly focus hours) | Calendar integrations (Google Calendar, Notion) |
| Session templates | Apple Sign-In |
| Session scheduling with alarm | |
| Focus streak calendar (contribution grid) | |
| App usage breakdown per session | |
| Study mode grouped timer (custom work/break ratio) | |
| WhatsApp / social share cards | |
| Advanced analytics (full history, monthly report) | |
| Theme customisation | |
| Account management (data export, account deletion) | |
| Privacy policy in-app link | |
| Hindi + 3 regional language strings | |

---

## User Scenarios

### User Story 1 — Account Creation & Cloud Sync (Priority: P1)

A user who has been using the app for 2 weeks with a 14-day streak taps "Sign in" on the Profile screen. They tap "Continue with Google", authenticate in one tap, and see their streak and session history preserved and uploaded to the cloud. They install the app on a second device, sign in, and see the same data immediately.

**Why P1**: Without this, no other V2 feature is possible. Authentication is the foundation of leaderboard, premium verification, sync, and compliance.

**Acceptance Scenarios**:

1. **Given** a user has local session data, **When** they sign in with Google for the first time, **Then** a one-time migration uploads all existing sessions, profile, and badges to their Firestore account without blocking the UI.
2. **Given** a signed-in user completes a session on Device A, **When** they open the app on Device B within 60 seconds, **Then** the new session appears in Device B's History.
3. **Given** a signed-in user is offline, **When** they complete sessions, **Then** sessions are queued locally and sync automatically when connectivity is restored.
4. **Given** a sync conflict (same session edited on two devices), **When** the conflict is resolved, **Then** the version with the later `endTime` wins; no data is silently discarded.
5. **Given** a user signs out, **When** sign-out completes, **Then** all cloud-synced data is cleared from the device and the app reverts to anonymous local-only mode.

---

### User Story 2 — Premium Subscription & Paywall (Priority: P2)

A power user has been using the free tier for a month and hits the "last 7 days" analytics limit. A soft paywall appears explaining the premium benefits. They tap "Go Premium", see a clear pricing screen (monthly ₹99 / annual ₹699), and complete a Google Play purchase in two taps. Analytics immediately unlocks full history.

**Why P2**: Revenue. Without this V2 has no business model. Premium verification is server-side via Firebase Functions to prevent client-side spoofing.

**Acceptance Scenarios**:

1. **Given** a free user tries to view analytics beyond 7 days, **When** they tap the locked chart region, **Then** a premium upsell sheet appears with feature comparison and pricing.
2. **Given** a user completes a Play Billing purchase, **When** the receipt is verified by Firebase Function, **Then** `isPremium = true` is written to Firestore and replicated to the device within 5 seconds.
3. **Given** a premium user's subscription lapses, **When** the next session sync occurs, **Then** `isPremium` reverts to `false` on the device and gated features become inaccessible.
4. **Given** a free user watches a rewarded ad, **When** the ad completes, **Then** they unlock full analytics for 24 hours without subscribing.
5. **Given** a user taps "Restore Purchases", **When** Play Billing confirms an active subscription, **Then** premium is restored without re-payment.

---

### User Story 3 — Smart Reminders (Priority: P3)

A student logs focus sessions every evening around 9 PM for three days. On the fourth day, the app sends a notification at 8:50 PM: "You usually focus at 9 PM — ready to start?" They tap it and the timer screen opens. They also receive a break nudge after 90 minutes and a daily goal reminder at 8 PM if they haven't hit 2 hours yet.

**Why P3**: Reminders are the top re-engagement mechanism for productivity apps. Users who receive habit reminders complete 2.3× more sessions per week (Forest internal data, cited in competitive research).

**Acceptance Scenarios**:

1. **Given** the user has logged 3+ sessions within a ±30-minute time window over consecutive days, **When** the pattern is detected, **Then** the app offers to schedule a recurring reminder at that time via a non-intrusive snackbar action.
2. **Given** a scheduled habit reminder, **When** the trigger time arrives, **Then** a notification fires within 5 minutes even if the app is not in the foreground.
3. **Given** a focus session has been running for longer than the user's max session length setting, **When** the threshold is exceeded, **Then** a "Time for a break?" nudge notification is sent.
4. **Given** the user has a daily focus goal set and it is 8 PM local time, **When** the user has not met their goal, **Then** a motivational notification is sent with today's progress (e.g., "You have 45 min to go — 15 min puts you on track").
5. **Given** the user disables reminders, **When** the setting is toggled off, **Then** all scheduled alarms and WorkManager tasks for that reminder type are cancelled within 10 seconds.

---

### User Story 4 — Leaderboard (Priority: P4)

A signed-in user opens the Profile tab and sees a Leaderboard card below their streak. It shows global users ranked by weekly focus minutes. Their own rank is highlighted. They can toggle between "Global" and "Friends" (friends require a separate invite flow — deferred to V3). Tapping a user shows their public profile with badge count and longest streak.

**Why P4**: Social proof and competition are proven retention drivers for habit apps. Leaderboard requires authentication (US1) to be meaningful.

**Acceptance Scenarios**:

1. **Given** a signed-in user opens the Leaderboard, **When** it loads, **Then** they see the top 50 users ranked by total focus minutes in the current week, with their own entry always visible regardless of rank.
2. **Given** the leaderboard data is stale (>30 minutes old), **When** the user opens it, **Then** a background refresh is triggered and a loading indicator is shown until fresh data arrives.
3. **Given** an anonymous (not signed-in) user taps the Leaderboard, **When** the paywall appears, **Then** a sign-in prompt explains the social feature and shows a "Sign in to see your rank" CTA.
4. **Given** a user's weekly focus total changes after a session, **When** the leaderboard is next viewed, **Then** their rank updates to reflect the new total.

---

### User Story 5 — Session Templates (Priority: P5)

A developer opens the Timer screen and long-presses the mode chip area. They tap "Save as template", name it "Deep Work — LeetCode", which saves mode=DEEP_WORK, duration=90 min, tag="LeetCode". Next time they open the app, they see their template as a one-tap chip and start the session immediately without reconfiguring.

**Why P5**: Reduces friction for power users who run the same session type daily. Directly addresses user request for "custom presets". No new permissions required.

**Acceptance Scenarios**:

1. **Given** a user configures a session (mode + duration + optional tag), **When** they save it as a template, **Then** the template appears as a chip on the Timer screen.
2. **Given** a user taps a template chip, **When** the session starts, **Then** the timer begins immediately with the template's saved configuration.
3. **Given** a user has saved templates, **When** they are signed in, **Then** templates sync to Firestore and appear on their other devices.
4. **Given** a user long-presses a template chip, **When** a context menu appears, **Then** they can rename, edit, or delete the template.

---

### User Story 6 — Account Management & Compliance (Priority: P6)

A user in India decides to delete their account. They go to Profile > Settings > Account > Delete Account. They confirm in a dialog. Within 24 hours, all Firestore data is permanently erased by a Firebase Function. They receive an email confirmation. The app reverts to anonymous mode.

**Why P6**: Mandatory. Google Play requires account deletion for all apps with user accounts (policy enforced from May 2024). India's DPDP Act 2023 (Digital Personal Data Protection Act) requires data erasure on request. Non-compliance causes Play Store removal.

**Acceptance Scenarios**:

1. **Given** a signed-in user requests account deletion, **When** they confirm, **Then** a Firebase Function is invoked that permanently deletes all Firestore documents for that user within 24 hours.
2. **Given** a user requests data export, **When** the export is ready, **Then** they receive a downloadable JSON file containing all their session history, profile, and badges.
3. **Given** a user taps "Privacy Policy" in Settings, **When** the link is tapped, **Then** the policy opens in the in-app browser (not the system browser) so the user stays in the app.
4. **Given** a user signs in for the first time, **When** sign-up completes, **Then** they are shown a consent screen explaining what data is collected and synced, with a mandatory acknowledgement.

---

### User Story 7 — Advanced Analytics & Reports (Priority: P7)

A premium user opens Analytics and switches to "Monthly" view. They see a 30-day bar chart of daily focus minutes with a monthly summary card (total hours, average daily, best day). They tap "Export Report" to get a PDF/CSV of the month's sessions. Free users see a "Unlock full history" upsell when they try to view beyond 7 days.

**Why P7**: This is the most-requested premium feature in comparable apps and the primary upgrade motivation for power users.

**Acceptance Scenarios**:

1. **Given** a premium user opens Analytics, **When** they select "Monthly" view, **Then** they see a 30-day chart aggregated from cloud-synced data.
2. **Given** a premium user taps "Export", **When** the export completes, **Then** a CSV file of all sessions (date, mode, duration, XP, distraction count) is shared via the Android share sheet.
3. **Given** a free user views Analytics, **When** the 7-day limit is reached, **Then** data beyond 7 days is shown blurred with a premium CTA overlay.

---

### User Story 8 — Social Sharing (Priority: P8)

A user hits a 30-day streak. The app generates a shareable card: their streak flame, total focus hours, and a tag line. They tap "Share on WhatsApp" and the image is sent directly to WhatsApp without leaving the app. The image contains a Play Store link for friends to download.

**Why P8**: WhatsApp is the primary social sharing surface in India. Organic installs from streak cards are zero-cost user acquisition. The Play Store link on the card creates a referral loop.

**Acceptance Scenarios**:

1. **Given** a user completes a milestone (7/30/100 day streak or 100/500 total focus hours), **When** the milestone fires, **Then** a share card is generated in memory using Compose `Canvas` (no network call) and a share prompt appears.
2. **Given** a user taps "Share", **When** they select WhatsApp, **Then** the card image is sent to WhatsApp directly via the Android Sharesheet.
3. **Given** no internet connection, **When** the share card is generated, **Then** it generates and shares successfully (all assets are local).

---

## Requirements

### Functional Requirements

#### Authentication & Cloud Sync

- **FR-100**: System MUST support Google Sign-In via Firebase Authentication as the primary identity provider.
- **FR-101**: System MUST support email/password sign-up and sign-in as a fallback identity provider.
- **FR-102**: System MUST allow the app to be used fully without an account (anonymous mode). A signed-in account enhances but does not gate core V1 features (timer, local history, analytics).
- **FR-103**: System MUST migrate all existing local Room data (sessions, profile, badges) to Firestore on first sign-in via an idempotent, background WorkManager task that does not block the UI.
- **FR-104**: System MUST use Room as the authoritative local store. All writes go to Room first; a `SyncWorker` uploads deltas to Firestore asynchronously. Firestore is never the primary read source.
- **FR-105**: System MUST add a `syncStatus` field (`PENDING` / `SYNCED` / `CONFLICT`) to `FocusSession` to track the sync queue. A `cloudUserId` field links local rows to the Firestore user document.
- **FR-106**: System MUST resolve sync conflicts using a "latest `endTime` wins" policy for session data. `isPremium` is authoritative from Firestore only — the local device MUST NOT write this field directly.
- **FR-107**: System MUST sync session data across devices within 60 seconds of a session completing, when internet is available.
- **FR-108**: System MUST allow sign-out, which clears all cloud-synced data from the device and returns the app to anonymous local-only mode.

#### Premium Subscription & Monetisation

- **FR-109**: System MUST integrate Google Play Billing Library 6+ to offer two subscription products: `focus_premium_monthly` (₹99/month) and `focus_premium_annual` (₹699/year).
- **FR-110**: System MUST verify every Play Billing purchase receipt via a Firebase Cloud Function. The function writes `isPremium = true` to Firestore. The client MUST NOT self-grant premium status.
- **FR-111**: System MUST gate the following features behind premium: full analytics history (beyond 7 days), monthly reports, data export, advanced theme options, and session templates sync across devices.
- **FR-112**: System MUST display a premium upsell sheet when a free user attempts to access a gated feature. The sheet MUST show a feature comparison table, both pricing tiers, and a "Start Free Trial" option.
- **FR-113**: System MUST offer a 7-day free trial on first purchase of either subscription product.
- **FR-114**: System MUST support Google AdMob rewarded ads for free users as an alternative to premium. A rewarded ad completion grants 24-hour access to full analytics.
- **FR-115**: System MUST offer a streak freeze consumable purchasable for ₹29 or earnable via one rewarded ad. A streak freeze prevents streak loss when the user misses a day.
- **FR-116**: System MUST implement a "Restore Purchases" button in Settings that re-validates the user's Play Billing entitlements.

#### Smart Reminders

- **FR-117** *(formerly FR-023)*: System MUST analyse the last 7 days of session start times; if 3+ sessions fall within a ±30-minute window at similar times, the system MUST offer to schedule a daily habit reminder at that time.
- **FR-118** *(formerly FR-024)*: System MUST send a break nudge notification when an active session exceeds the user's configured max session length (default: 2 hours).
- **FR-119** *(formerly FR-025)*: System MUST send a daily goal reminder notification if the user has not met their daily focus goal by a user-configurable evening time (default: 8 PM). The notification MUST include today's actual vs. target minutes.
- **FR-120**: System MUST schedule all reminders via `AlarmManager.setExactAndAllowWhileIdle()` (habit reminders) and WorkManager periodic requests (goal reminders) to survive device restart.
- **FR-121**: System MUST provide per-reminder-type toggle switches in Settings. Disabling a type MUST cancel all associated pending alarms and WorkManager tasks immediately.

#### Leaderboard

- **FR-122** *(formerly FR-019)*: System MUST display a leaderboard showing the top 50 users ranked by total focus minutes in the current calendar week (Monday–Sunday). The signed-in user's own entry MUST always be visible regardless of rank.
- **FR-123**: System MUST refresh leaderboard data at most every 30 minutes to limit Firestore read costs. Data older than 30 minutes MUST show a "Last updated X min ago" badge.
- **FR-124**: Leaderboard is only available to signed-in users. Anonymous users MUST see a sign-in prompt with feature explanation.
- **FR-125**: System MUST display a public mini-profile (badge count, longest streak, total hours) when a leaderboard user is tapped.

#### Session Templates

- **FR-126** *(from T-CP12)*: System MUST allow users to save any session configuration (mode + duration + optional tag) as a named template.
- **FR-127**: System MUST display saved templates as one-tap chips on the Timer screen above the mode selector, limited to 5 templates visible (scroll to see more).
- **FR-128**: Templates MUST sync to Firestore for signed-in premium users. Free users get 2 templates maximum stored locally.
- **FR-129**: System MUST allow templates to be renamed, edited, or deleted via a long-press context menu.

#### Session Scheduling

- **FR-130** *(from T-CP10)*: System MUST allow users to schedule a future session start time (up to 7 days ahead). A notification fires at the scheduled time and tapping it opens the app to the pre-configured timer screen ready to start.
- **FR-131**: Scheduled sessions MUST use `AlarmManager.setExactAndAllowWhileIdle()` and survive device restart via `BootReceiver`.

#### App Usage Breakdown

- **FR-132** *(from T-CP13)*: Session Detail screen MUST show a breakdown of which apps triggered distractions during that session, how many times each, and total duration away in each app.
- **FR-133**: System MUST aggregate app distraction counts across all sessions in the Analytics screen, showing a "Top distracting apps this week" card (requires UsageStats permission).

#### Focus Streak Calendar

- **FR-134** *(from T-CP11)*: Profile screen MUST display a GitHub-style contribution calendar grid showing daily focus minutes for the last 12 weeks. Cell intensity reflects minutes (0 = empty, ≥ goal = full colour).
- **FR-135**: Tapping a calendar cell navigates to a filtered History view showing sessions from that day.

#### Study Mode Grouped Timer

- **FR-136** *(from T-CP14)*: Study Mode MUST offer a "Cycle" sub-mode allowing the user to configure a custom work/break ratio (e.g., 50 min work / 10 min break) that repeats until manually stopped, distinct from the fixed 25/5 Pomodoro cycle.

#### Advanced Analytics

- **FR-137**: Premium users MUST have access to full session history in Analytics without the 7-day limit. A "Monthly" view showing a 30-day chart MUST be available.
- **FR-138** *(formerly FR-027 partial)*: Premium users MUST be able to export their full session history as a CSV file via the Android share sheet.
- **FR-139**: Analytics MUST show a "Top distracting apps" card powered by aggregated `DistractionEvent.appPackageName` data (FR-133).

#### Theme Customisation

- **FR-140** *(formerly FR-027 partial)*: System MUST provide three themes for premium users: Default (dark purple), AMOLED (true black), and Light. The selected theme MUST be persisted to DataStore and synced to Firestore for premium users.

#### Social Sharing

- **FR-141**: System MUST generate an in-memory share card image (Compose Canvas bitmap, no network call) when a streak milestone is reached (7 / 30 / 100 days) or a focus hour milestone (100 / 500 / 1000 hours total).
- **FR-142**: The share card MUST include the user's streak or hour count, the app name, and a Play Store download link QR code or URL.
- **FR-143**: System MUST provide a "Share" button on the milestone celebration dialog that triggers the Android Sharesheet. WhatsApp MUST be surfaced as the first option when installed.

#### Account Management & Compliance

- **FR-144**: System MUST provide an "Account" section in Settings showing: signed-in email, sign-out button, delete account button, data export button, and privacy policy link.
- **FR-145**: Account deletion MUST invoke a Firebase Cloud Function that permanently erases all Firestore documents for that user within 24 hours and sends a confirmation email. The app MUST immediately revert to anonymous mode.
- **FR-146**: Privacy policy MUST be accessible via an in-app WebView link in Settings (no external browser), satisfying Google Play account policy requirements.
- **FR-147**: System MUST display a one-time data consent screen on first sign-up, explaining what data is collected, synced, and how it is used, with a mandatory acknowledgement tap before account creation completes.
- **FR-148**: System MUST provide a data export function that packages all `FocusSession`, `UserProfile`, and `Badge` records into a downloadable JSON file shared via the Android share sheet.

#### Localisation

- **FR-149**: System MUST provide full UI string translations for Hindi (`hi`), Bengali (`bn`), Tamil (`ta`), and Telugu (`te`) in addition to English. All existing `strings.xml` entries MUST be translated.
- **FR-150**: System MUST respect the device locale automatically. A manual language override option MUST be available in Settings.

---

## Key Entities — V2 Additions

### Modified Entities (additions to V1 schema)

**FocusSession** *(V2 additions)*:
- `syncStatus: SyncStatus` — enum `PENDING | SYNCED | CONFLICT`. Default `PENDING` for new sessions.
- `cloudUserId: String?` — Firebase Auth UID of the owning user. `null` for anonymous sessions.

**UserProfile** *(V2 additions)*:
- `cloudUserId: String?` — Firebase Auth UID. `null` = anonymous.
- `isPremiumExpiry: Long` — epoch ms of premium subscription expiry. `0` = not premium.
- `streakFreezeCount: Int` — number of available streak freeze consumables.
- `themePreference: String` — `DEFAULT | AMOLED | LIGHT`.

### New Entities

**SessionTemplate**:
- `id: Int` (auto-increment)
- `name: String` — user-given name (e.g., "Deep Work — LeetCode")
- `mode: SessionMode`
- `durationSeconds: Int`
- `tag: String?`
- `sortOrder: Int` — display order on timer screen
- `cloudSynced: Boolean`

**ScheduledSession**:
- `id: Int`
- `scheduledTimeMs: Long` — epoch ms of when the alarm fires
- `templateId: Int?` — linked template (optional)
- `mode: SessionMode`
- `durationSeconds: Int`
- `tag: String?`
- `alarmRequestCode: Int` — AlarmManager request code for cancellation

**CloudSyncLog** *(internal, not user-visible)*:
- `id: Int`
- `entityType: String` — `SESSION | PROFILE | BADGE | TEMPLATE`
- `entityId: String`
- `syncedAtMs: Long`
- `outcome: String` — `SUCCESS | FAILURE | CONFLICT`

---

## Architecture Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Auth provider | Firebase Authentication (Google Sign-In + email/password) | Same SDK as Firestore; offline-first persistence layer; best Indian CDN coverage |
| Cloud database | Cloud Firestore | Offline-first with local cache; real-time listeners map to Kotlin Flow; free tier sufficient at MVP scale |
| Sync direction | Room-first, Firestore as eventual replica | Preserves V1 offline-first invariant; no architectural change to domain layer |
| `isPremium` authority | Firestore only (written by Firebase Function after Play Billing verification) | Prevents client-side spoofing; standard pattern for Play Billing + Firebase |
| Session sync granularity | FocusSession as Firestore document with distractions as embedded array (not subcollection) | 1 document read per session instead of N reads; max ~20 distractions per session stays well under 1 MB limit |
| Anonymous-to-auth migration | One-shot WorkManager task on first successful sign-in | Idempotent; does not block UI; retries on failure |
| Conflict resolution | Latest `endTime` wins for sessions; server wins for `isPremium` | Simple deterministic rule with no user-visible conflict UI needed for V2 |
| Ads SDK | Google AdMob (rewarded only) | No banner/interstitial to preserve focus UX; integrates with existing Google suite |
| Leaderboard aggregation | Firestore aggregated weekly totals, refreshed by Firebase Function every 15 min | Avoids per-client aggregation queries; controlled cost |
| Reminders | AlarmManager (habit) + WorkManager (daily goal) | AlarmManager for exact-time personal alarms; WorkManager for periodic background checks |
| Share cards | Compose Canvas bitmap (in-memory, no network) | Zero latency; works offline; no server-side image rendering cost |

---

## Success Criteria

- **SC-100**: Cloud sync: a session completed on Device A appears on Device B within 60 seconds when both are online.
- **SC-101**: Premium conversion: at least 15% of active free users convert to paid within 3 months of V2 launch (matches V1 SC-007 target).
- **SC-102**: Reminder delivery: at least 90% of scheduled habit reminders are delivered within 5 minutes of the target time on a device that is not in Doze mode (matches SC-006).
- **SC-103**: Sign-in flow: account creation (Google Sign-In path) completes within 30 seconds end-to-end on a stable network.
- **SC-104**: Account deletion: all user Firestore data is permanently erased within 24 hours of a confirmed deletion request (DPDP Act compliance).
- **SC-105**: Leaderboard freshness: displayed leaderboard data is never more than 35 minutes stale when the user opens the screen with an active connection.
- **SC-106**: Billing: premium status is reflected on-device within 5 seconds of a successful Play Billing purchase on a stable network.
- **SC-107**: Offline resilience: all V1 features (timer, local history, streak, XP) function correctly with no internet connection, regardless of signed-in state.
- **SC-108**: Play Store compliance: account deletion feature achieves ≥ 95% user task-completion rate in usability testing (required by Play policy).

---

## Assumptions

- **Auth provider**: Only Google Sign-In and email/password are supported in V2. Apple Sign-In is required for iOS and deferred to V3 when the iOS port begins.
- **Leaderboard privacy**: User display names on the leaderboard are Firebase Auth display names (from Google profile). Users can change their display name. No real-name enforcement.
- **Premium gating philosophy**: Core focus functionality (timer, all modes, local history, 7-day analytics, gamification) remains free forever. Premium gates extended history, cloud sync priority, and power-user features. This preserves V1's user promise.
- **Firestore data model**: Each user has a Firestore document at `users/{uid}`. Sessions are stored as a subcollection `users/{uid}/sessions/{sessionId}`. Templates at `users/{uid}/templates/{templateId}`. Leaderboard is a separate collection `leaderboard/weekly/{weekKey}/{uid}` populated by Firebase Functions.
- **DPDP Act scope**: The Digital Personal Data Protection Act 2023 applies because the app collects behavioural data (session times, app-switch events) from Indian users. Account deletion and data export are mandatory controls.
- **Play Billing geography**: Pricing in INR (₹). Monthly: ₹99. Annual: ₹699 (saves ~40%). These are starting prices; exact pricing is a business decision and can be changed without a code release via Play Console.
- **Rewarded ads**: AdMob rewarded ads shown at natural break points only (after session ends, before analytics screen on free tier). No banner or interstitial ads. Ad content is filtered to exclude gambling, alcohol, and adult categories.
- **Streak freeze**: Maximum 3 streak freezes can be held at once. They are consumed automatically when a day is missed. Purchasing more when at maximum is blocked with an informational message.
- **Regional language scope**: Machine-translated strings are acceptable for V2 launch; professional review is a V3 quality gate. Community corrections via a feedback mechanism are preferred over delaying launch.
- **Firebase Function hosting**: Firebase Blaze (pay-as-you-go) plan required for Cloud Functions. At V2 scale (< 10,000 DAU) monthly Firebase cost is estimated at < ₹500/month.

---

## V2 Implementation Priority Order

| Priority | Feature Group | Blocking On |
|----------|--------------|-------------|
| P1 | Authentication + basic Firestore sync | Nothing — start here |
| P2 | Premium subscription + Play Billing + paywall | P1 (needs auth UID for server-side verification) |
| P3 | Smart reminders (habit + daily goal) | Nothing — independent of P1 |
| P4 | Leaderboard | P1 (requires auth) |
| P5 | Account management + compliance (deletion, export, privacy policy) | P1 |
| P6 | Advanced analytics (monthly view, export) | P2 (premium gate) |
| P7 | Session templates | P1 (for sync) |
| P8 | Social share cards (streak milestones) | Nothing — local only |
| P9 | Session scheduling | Nothing — independent |
| P10 | App usage breakdown | Nothing — uses existing DistractionEvent data |
| P11 | Focus streak calendar | Nothing — uses existing session data |
| P12 | Theme customisation | P2 (premium gate) |
| P13 | Regional languages (Hindi, Bengali, Tamil, Telugu) | Nothing — strings only |
| P14 | Study mode grouped timer | Nothing — extends existing timer |
