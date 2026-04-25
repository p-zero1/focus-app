# Quickstart: Focus + Behavior Tracking System

**Date**: 2026-04-24

## Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK API 26–35 installed
- A physical Android device or emulator running API 26+

## Project Setup

```bash
# Clone the repo
git clone <repo-url>
cd focus-app

# Open in Android Studio
# File → Open → select android/ directory
# Let Gradle sync complete (~2–3 minutes first time)
```

## Build & Run

```bash
# From repo root or android/ directory:
./gradlew :app:assembleDebug          # Build APK
./gradlew :app:installDebug           # Install to connected device/emulator
./gradlew :app:connectedAndroidTest   # Run instrumented tests
./gradlew :app:test                   # Run unit tests (Robolectric)
```

## Required Permissions (Manual)

On first launch, the onboarding flow will guide users through:

1. **Usage Access** (`PACKAGE_USAGE_STATS`):
   - Settings → Apps → Special App Access → Usage Access → FocusApp → Enable
   - Without this: distraction detection is disabled but timer works normally.

2. **Notifications** (Android 13+):
   - Requested via standard permission dialog on first launch.
   - Without this: session-end alerts and reminders are silenced.

3. **Exact Alarms** (Android 12+):
   - Settings → Apps → FocusApp → Alarms & Reminders → Allow
   - Without this: habit reminders use WorkManager (less precise timing).

## Key Entry Points

| Class | Purpose |
|-------|---------|
| `MainActivity.kt` | Single Activity; hosts Compose NavHost |
| `TimerService.kt` | Foreground service; manages countdown + distraction monitoring |
| `FocusDatabase.kt` | Room database; single instance via Hilt |
| `TimerViewModel.kt` | Binds to TimerService; exposes timer state to Compose UI |
| `AnalyticsViewModel.kt` | Queries session aggregates; computes Focus Score + best-time insight |

## Running a Focus Session (Dev Workflow)

1. Launch app on device
2. Grant Usage Access when prompted (or skip to test timer-only)
3. Select a mode (Pomodoro / Custom / Study / Deep Work)
4. Press **Start**
5. Switch to another app → return → verify distraction warning appears
6. Timer completes → session is logged → check History tab

## Gradle Dependencies (Key)

```kotlin
// build.gradle.kts (app)
dependencies {
    // UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.7.x")

    // DI
    implementation("com.google.dagger:hilt-android:2.x")
    kapt("com.google.dagger:hilt-compiler:2.x")

    // Data
    implementation("androidx.room:room-runtime:2.6.x")
    implementation("androidx.room:room-ktx:2.6.x")
    kapt("androidx.room:room-compiler:2.6.x")
    implementation("androidx.datastore:datastore-preferences:1.0.x")

    // Background work
    implementation("androidx.work:work-runtime-ktx:2.9.x")

    // Async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.x")

    // Logging
    implementation("com.jakewharton.timber:timber:5.0.x")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.x")
    testImplementation("app.cash.turbine:turbine:1.x")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.x")
}
```
