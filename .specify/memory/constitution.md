<!--
SYNC IMPACT REPORT
==================
Version change:    (none) → 1.0.0
Type:              MINOR (initial ratification — all principles new)

Modified principles:
  - All sections newly created from template placeholders

Added sections:
  - I.   Single-Module Android Architecture
  - II.  Domain-First (Clean Architecture)
  - III. Offline-First
  - IV.  Graceful Permission Degradation
  - V.   Layer-Isolated Testing
  - VI.  Simplicity / YAGNI
  - VII. V1 Scope Gate

Template sync status:
  ✅ .specify/templates/plan-template.md     — Constitution Check gate wording unchanged; gates now derivable
  ✅ .specify/templates/spec-template.md     — No constitution-driven mandatory sections added
  ✅ .specify/templates/tasks-template.md    — No principle-driven task types added or removed
  ✅ .specify/templates/constitution-template.md — Source template; no changes needed

Deferred items:
  - None. All placeholders resolved.
-->

# Focus App Constitution

## Core Principles

### I. Single-Module Android Architecture

The app MUST remain a single Gradle module (`android/app`) for the lifetime of V1 and V2.
All source lives under `android/app/src/main/kotlin/com/focusapp/`.
New Gradle modules MUST NOT be introduced unless a clear, documented reason exists
(e.g., a separately published SDK). Adding a module purely for organisational purposes
is a violation of this principle.

Sub-package structure within the single module follows clean-architecture layers:
`data/`, `domain/`, `service/`, `ui/`, `di/`, `worker/`.
No cross-layer direct imports are allowed downward (UI MUST NOT import `data/` directly).

### II. Domain-First (Clean Architecture)

The `domain/` package MUST contain only pure Kotlin — zero Android framework imports.
Every piece of business logic MUST live in a `domain/usecase/` class, not in a
ViewModel, Service, or Repository implementation.

Rules:
- ViewModels delegate to use cases; they MUST NOT contain business logic.
- Repository interfaces live in `domain/repository/`; implementations live in `data/repository/`.
- Services (e.g., `TimerService`) coordinate use cases; they MUST NOT query the DB directly.
- Use cases are single-responsibility: one public `invoke()` method, one task.

Violations of the dependency rule (domain ← data direction) are automatically CRITICAL
in any analysis pass.

### III. Offline-First

All V1 features MUST function without any network connection.
No network call MUST block or degrade core timer or session-logging functionality.
Cloud sync is a V2 premium feature; any V1 code path that requires a network call
is a scope violation.

Local Room database is the single source of truth for all session and gamification data.
DataStore is the single source of truth for user preferences.
No in-memory-only state that is not persisted should survive an app restart.

### IV. Graceful Permission Degradation

Every Android runtime permission the app requests MUST have a defined fallback behaviour
when denied. The app MUST remain fully launchable and usable as a basic timer
regardless of which permissions are granted.

Mandatory fallback table (minimum required in any implementation):

| Permission | Feature Lost if Denied | App Remains Functional? |
|---|---|---|
| `PACKAGE_USAGE_STATS` | Distraction detection disabled | MUST YES |
| `POST_NOTIFICATIONS` (API 33+) | Session-end alerts + reminders silenced | MUST YES |
| `SCHEDULE_EXACT_ALARM` (API 31+) | Habit reminders fall back to WorkManager | MUST YES |
| `ACCESS_NOTIFICATION_POLICY` | DND suppression skipped silently | MUST YES |

Any new permission added to `AndroidManifest.xml` MUST have a corresponding row in this
table before the PR is merged.

### V. Layer-Isolated Testing

The `domain/` layer MUST be testable with JUnit 4 + Robolectric — no emulator required.
Every use case MUST have at least a unit test exercising its primary success path
before the implementing phase is marked complete.

Integration tests (Room, DataStore) MUST run via `connectedAndroidTest` against a real
SQLite instance, never a mock. The rule from research.md stands: mock/prod divergence
caused real incidents in similar projects; mocked DB tests are not a substitute.

Each user story phase MUST have an independent test scenario documented in `tasks.md`
that can be manually verified without completing other stories.

### VI. Simplicity / YAGNI

No abstraction layer, helper class, or utility function MUST be introduced for a
single call site. Three similar lines of code is preferable to a premature abstraction.

Specific prohibited patterns:
- Repository interfaces with a single implementation that will never be swapped → acceptable,
  but MUST NOT be created purely to satisfy a pattern for its own sake (exception: test doubles).
- ViewModel factories beyond `@HiltViewModel` injection.
- Abstract base classes for screens or ViewModels.
- Generic wrappers around coroutine flows unless they add measurable code reduction (>3 callsites).

When in doubt, write the direct code first. Refactor only when a second real callsite
exists.

### VII. V1 Scope Gate

V1 ships US1–US5 (Timer, Distraction Detection, Analytics, Gamification, Study Mode).
V2 adds US6 (Smart Reminders), Leaderboard, Friend graph, Cloud sync.
V3 adds App Blocking, Premium theme customisation, Advanced reports.

Code that implements a V2/V3 feature MUST NOT be merged into the main branch until
that version's development cycle begins. Placeholder stubs (empty files, TODO comments)
are acceptable; partial implementations are not.

The `tasks.md` note "V2 SCOPE" on Phase 8 is a binding scope marker.
Any PR that touches V2/V3 features while V1 is in progress is automatically blocked
pending a documented scope decision.

## Additional Constraints

### Performance Baselines

These success-criteria targets from `spec.md` are binding engineering constraints,
not aspirational metrics:

| SC | Target | Measurement Point |
|---|---|---|
| SC-001 | Session starts within 10 s of app open | From cold launch tap to countdown ticking |
| SC-002 | Distraction logged within 5 s of interruption | From app-switch to DB row insertion |
| SC-005 | Analytics screen loads within 2 s | From tab tap to chart rendered |
| SC-006 | 90% of reminders delivered within 5 min | AlarmManager / WorkManager fire time |

Any task that touches the timer hot path, distraction polling loop, or analytics query
MUST be evaluated against the relevant SC before the phase is marked complete.

### Monetisation Boundary

Free-tier access limits (FR-015, FR-026) MUST be enforced at the repository query level
(e.g., date-range clamp in `FocusSessionDao`) — not in the ViewModel or UI layer —
so premium bypass cannot be achieved by modifying UI code alone.

## Development Workflow

### Speckit Gate Order

Every feature MUST pass through these gates in order before implementation begins:

1. `/speckit-specify` — spec.md with prioritised user stories
2. `/speckit-clarify` — all ambiguities resolved, no `[NEEDS CLARIFICATION]` remaining
3. `/speckit-plan` — research.md, data-model.md, contracts/ generated
4. `/speckit-tasks` — tasks.md with phase/dependency structure
5. `/speckit-implement` — phase-by-phase implementation
6. `/speckit-analyze` — gap analysis after each phase before proceeding

Skipping a gate requires a documented reason in the PR description.

### Phase Completion Criteria

A tasks.md phase is COMPLETE only when:
- All tasks in the phase are marked `[x]`.
- The phase checkpoint (manual end-to-end test described in tasks.md) passes on a
  physical device or emulator.
- `/speckit-analyze` has been run and all CRITICAL and HIGH findings are resolved or
  explicitly accepted with written justification.

### Amendment Procedure

Amendments to this constitution require:
1. A description of the principle being changed and the motivation.
2. An update to this file with a version bump (see versioning rules below).
3. A propagation pass: re-run `/speckit-analyze` on the active feature branch to
   surface any new violations introduced by the amendment.
4. PR description MUST reference the constitution version change.

## Governance

This constitution supersedes all informal conventions and README guidance where they
conflict. Principles marked MUST are non-negotiable within the stated scope;
deviation requires an explicit amendment, not a silent exception.

**Versioning policy**:
- MAJOR: Removal or fundamental redefinition of an existing principle.
- MINOR: New principle or section added; material expansion of existing guidance.
- PATCH: Wording clarification, typo fix, non-semantic refinement.

**Compliance review**: Every `/speckit-analyze` run validates against the active
constitution version. A stale constitution (version not matching the current development
phase) MUST be updated before analysis results are treated as authoritative.

**Version**: 1.0.0 | **Ratified**: 2026-04-25 | **Last Amended**: 2026-04-25
