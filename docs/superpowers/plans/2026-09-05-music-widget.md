# Android 16-17 Music Widget & Immersive Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Android 16-17 compatible media home widget system (4x2, 2x2 standard, 2x2 minimal) with 5 circular progress ring styles, Glance UI, widget configuration screen, and landscape immersive StandBy player.

**Architecture:** A `NotificationListenerService` reads active `MediaSession`s (Apple Music, YouTube Music, Samsung Music) into a reactive `MediaPlaybackRepository`. Canvas/RenderEffect generates blurred backgrounds and 5 custom circular progress ring styles on bitmaps. Glance renders these to home widgets with battery-conscious smart updates. An `ImmersivePlayerActivity` provides a full-screen landscape StandBy player.

**Tech Stack:** Kotlin, Android SDK 36-37, Jetpack Glance (`glance-appwidget`), Jetpack Compose (Material 3), AndroidX Media / MediaSessionManager, Coroutines & Flow, SharedPreferences / DataStore.

**Spec:** `docs/superpowers/specs/2026-09-05-music-widget-design.md`

## Global Constraints
- Target platform: Android 16 (API 36) ~ Android 17 (API 37), minSdk = 36, compileSdk = 37.
- Framework: Jetpack Glance for home widgets, Jetpack Compose for Configuration Activity and Immersive StandBy Player.
- Media sources: Apple Music, YouTube Music, Samsung Music, Spotify via standard `MediaSessionManager` / `NotificationListenerService`.
- RemoteViews Safety: All bitmaps passed to Glance widgets must be bounded and downsampled to widget dp dimensions to strictly prevent `TransactionTooLargeException`.
- Battery policy: Periodic ticker (1-1.5s) only runs when screen is ON and audio is playing (`isPlaying == true`).

---

### Task 1: Dependencies & Build Configuration

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: Existing Gradle setup
- Produces: Jetpack Glance, Jetpack Compose, Material 3, and Media libraries ready for compilation

- [ ] **Step 1: Update libs.versions.toml with Glance, Compose, and Media dependencies**
Add Glance (`1.1.1`), Compose BOM, Material 3, and Media3/Media compat libraries to version catalog.

- [ ] **Step 2: Update app/build.gradle.kts with Compose & Glance configs**
Enable `buildFeatures { compose = true }`, configure Kotlin compiler options, and declare dependencies.

- [ ] **Step 3: Run Gradle sync / build dry-run**
Run: `./gradlew assembleDebug` to verify dependency resolution and build integrity.

- [ ] **Step 4: Commit build configuration**
```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add Jetpack Glance, Compose, and Media dependencies"
```

---

### Task 2: Core Domain Model & Media Playback Repository

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/domain/model/MediaPlaybackState.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/domain/repository/MediaPlaybackRepository.kt`
- Create: `app/src/test/java/com/github/windsekirun/musicwidget/domain/model/MediaPlaybackStateTest.kt`

**Interfaces:**
- Consumes: None
- Produces: `MediaPlaybackState`, `MediaPlaybackRepository` interface and singleton implementation

- [ ] **Step 1: Write unit tests for MediaPlaybackState**
Test progress ratio calculation: duration 0 returns 0f, 50s of 100s returns 0.5f, overflow clamped to 1f.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.github.windsekirun.musicwidget.domain.model.MediaPlaybackStateTest`

- [ ] **Step 3: Implement MediaPlaybackState and MediaPlaybackRepository**
Create `MediaPlaybackState` data class and `MediaPlaybackRepository` singleton with `playbackState: StateFlow<MediaPlaybackState>` and control action methods (`playPause()`, `skipToNext()`, `skipToPrevious()`, `seekTo(ms)`).

- [ ] **Step 4: Run unit tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests com.github.windsekirun.musicwidget.domain.model.MediaPlaybackStateTest`

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/domain/ app/src/test/java/com/github/windsekirun/musicwidget/domain/
git commit -m "feat: add MediaPlaybackState and MediaPlaybackRepository"
```

---

### Task 3: MediaNotificationListenerService & Smart Ticker

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/service/MediaNotificationListenerService.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/receiver/ScreenStateReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `MediaPlaybackRepository`
- Produces: System `MediaSession` integration and screen-aware smart ticker

- [ ] **Step 1: Implement MediaNotificationListenerService**
Hook into `MediaSessionManager.OnActiveSessionsChangedListener`, bind to active `MediaController`, register `MediaController.Callback`, extract metadata & playback states, and feed `MediaPlaybackRepository`.

- [ ] **Step 2: Implement ScreenStateReceiver & Smart Ticker**
Listen for `ACTION_SCREEN_ON` and `ACTION_SCREEN_OFF`. Launch a coroutine ticker updating `positionMs` every 1s only when screen is on and playback is playing. Cancel immediately on pause or screen off.

- [ ] **Step 3: Declare Service and Receivers in AndroidManifest.xml**
Add `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` intent-filter for the service.

- [ ] **Step 4: Verify build and manifest merge**
Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/service/ app/src/main/java/com/github/windsekirun/musicwidget/receiver/ app/src/main/AndroidManifest.xml
git commit -m "feat: implement MediaNotificationListenerService and ScreenStateReceiver"
```

---

### Task 4: Graphic Renderer with 5 Circular Progress Ring Styles & Blur

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/graphics/RingStyle.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/graphics/WidgetBitmapRenderer.kt`
- Create: `app/src/test/java/com/github/windsekirun/musicwidget/ui/graphics/WidgetBitmapRendererTest.kt`

**Interfaces:**
- Consumes: `MediaPlaybackState`, `RingStyle`
- Produces: `renderBlurredBackground(context, bitmap, width, height)`, `renderArtworkWithRing(context, bitmap, progress, isPlaying, ringStyle, borderColor, size)`

- [ ] **Step 1: Write unit tests for ring math and styling calculations**
Test sweep angle calculation for 0%, 50%, 100%, and sine wave generation parameters.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.github.windsekirun.musicwidget.ui.graphics.WidgetBitmapRendererTest`

- [ ] **Step 3: Implement WidgetBitmapRenderer**
1. `renderBlurredBackground`: centerCrop bitmap to destination size, apply RenderEffect/in-memory blur, overlay 35% dark dimming tint. If null bitmap, render transparent background with rounded border.
2. `renderArtworkWithRing`: circular crop album art with PorterDuff.Mode.SRC_IN. Render one of 5 styles:
   - `SQUIGGLY_WAVE`: sine wave modulated arc along circle radius when playing, straight circle when paused.
   - `FLOATING_CLEAN`: inner slim border + gap + outer round cap arc.
   - `SEGMENTED_MINIMAL`: dash/dot ticks around circumference.
   - `GLOW_THUMB`: active progress arc with glowing head dot.
   - `SOLID_CLASSIC`: single direct circular progress arc.
   If no artwork, draw default music note icon.

- [ ] **Step 4: Run unit tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests com.github.windsekirun.musicwidget.ui.graphics.WidgetBitmapRendererTest`

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/ui/graphics/ app/src/test/java/com/github/windsekirun/musicwidget/ui/graphics/
git commit -m "feat: implement WidgetBitmapRenderer with 5 circular progress ring styles and blur"
```

---

### Task 5: Widget Preferences Repository

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/data/WidgetConfig.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/data/WidgetPreferencesRepository.kt`
- Create: `app/src/test/java/com/github/windsekirun/musicwidget/data/WidgetPreferencesRepositoryTest.kt`

**Interfaces:**
- Consumes: `RingStyle`
- Produces: `loadConfig(appWidgetId)`, `saveConfig(appWidgetId, config)`

- [ ] **Step 1: Write unit test for WidgetConfig serialization/deserialization**
Test default values and storing custom colors, styles, and flags.

- [ ] **Step 2: Run test to verify it fails**
Run: `./gradlew testDebugUnitTest --tests com.github.windsekirun.musicwidget.data.WidgetPreferencesRepositoryTest`

- [ ] **Step 3: Implement WidgetPreferencesRepository**
Support per-appWidgetId storage using SharedPreferences with fallback defaults.

- [ ] **Step 4: Run unit tests to verify they pass**
Run: `./gradlew testDebugUnitTest --tests com.github.windsekirun.musicwidget.data.WidgetPreferencesRepositoryTest`

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/data/ app/src/test/java/com/github/windsekirun/musicwidget/data/
git commit -m "feat: implement WidgetPreferencesRepository for per-widget settings"
```

---

### Task 6: Glance Widgets (4x2, 2x2 Standard, 2x2 Minimal)

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/widget/MusicWidget4x2.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/widget/MusicWidget2x2Standard.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/widget/MusicWidget2x2Minimal.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/widget/MusicWidgetActionCallback.kt`
- Create: `app/src/main/res/xml/music_widget_4x2_info.xml`
- Create: `app/src/main/res/xml/music_widget_2x2_standard_info.xml`
- Create: `app/src/main/res/xml/music_widget_2x2_minimal_info.xml`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `MediaPlaybackRepository`, `WidgetBitmapRenderer`, `WidgetPreferencesRepository`
- Produces: 3 registerable Glance widgets in Android launcher widget picker

- [ ] **Step 1: Create XML AppWidget provider info files**
Define size, minWidth/minHeight, targetCellWidth/targetCellHeight, configuration activity, and preview layouts.

- [ ] **Step 2: Implement GlanceActionCallback and click handlers**
Handle `ACTION_PLAY_PAUSE`, `ACTION_PREV`, `ACTION_NEXT`, and launch player session activity on artwork/title click.

- [ ] **Step 3: Implement MusicWidget4x2, MusicWidget2x2Standard, and MusicWidget2x2Minimal**
Implement Glance composable layouts utilizing Glance `Image`, `Text`, `Row`, `Column`, `Box`, and action modifiers.

- [ ] **Step 4: Register widgets and receivers in AndroidManifest.xml**
Register receivers with `android.appwidget.action.APPWIDGET_UPDATE`.

- [ ] **Step 5: Verify build**
Run: `./gradlew assembleDebug`

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/widget/ app/src/main/res/xml/ app/src/main/AndroidManifest.xml
git commit -m "feat: implement 4x2 and 2x2 Glance music widgets"
```

---

### Task 7: Widget Configuration Activity (Compose M3)

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/config/WidgetConfigurationActivity.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/config/ColorPickerComponents.kt`

**Interfaces:**
- Consumes: `WidgetPreferencesRepository`, `WidgetBitmapRenderer`, `RingStyle`
- Produces: Complete Configuration UI with live preview and color customization

- [ ] **Step 1: Implement ColorPickerComponents**
Create Compose components for preset color chips, Material You dynamic color toggle, and custom HEX input/slider.

- [ ] **Step 2: Implement WidgetConfigurationActivity**
Add live preview card, ring style selector (horizontal scroll cards showing the 5 styles), color customizer, permission check banner, and save button that updates Glance widget and returns `RESULT_OK`.

- [ ] **Step 3: Register WidgetConfigurationActivity in AndroidManifest.xml**
Add `ACTION_APPWIDGET_CONFIGURE` intent filter.

- [ ] **Step 4: Verify build**
Run: `./gradlew assembleDebug`

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/ui/config/ app/src/main/AndroidManifest.xml
git commit -m "feat: implement WidgetConfigurationActivity with live preview and color picker"
```

---

### Task 8: Responsive Immersive StandBy Player (Portrait & Landscape with Sensor Override)

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/immersive/ImmersivePlayerActivity.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/immersive/SquigglySeekBar.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/immersive/ArtisticAlbumMasks.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/receiver/PowerConnectionReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `MediaPlaybackRepository`
- Produces: Fullscreen responsive (Landscape & Portrait) StandBy player activity with sensor rotation override

- [ ] **Step 1: Implement SquigglySeekBar and ArtisticAlbumMasks Compose components**
Render interactive sine-wave progress seekbar and artistic album mask shapes (Figure-8, Morphing Pebble, Vinyl, Squircle).

- [ ] **Step 2: Implement ImmersivePlayerActivity with Full Sensor Rotation**
Configure `requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR` so the activity unlocks and follows the physical device orientation even if system auto-rotate is locked. Adapt layout smoothly between Landscape (side-by-side) and Portrait (vertical stack).

- [ ] **Step 3: Implement PowerConnectionReceiver for StandBy auto-trigger**
Listen for `ACTION_POWER_CONNECTED` and orientation changes; if music is playing and device is placed horizontally, automatically launch `ImmersivePlayerActivity`.

- [ ] **Step 4: Register Activity and Receiver in AndroidManifest.xml**
Configure `android:screenOrientation="fullSensor"` and window flags.

- [ ] **Step 5: Verify build**
Run: `./gradlew assembleDebug`

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/ui/immersive/ app/src/main/java/com/github/windsekirun/musicwidget/receiver/PowerConnectionReceiver.kt app/src/main/AndroidManifest.xml
git commit -m "feat: implement responsive ImmersivePlayerActivity with full-sensor rotation override"
```

**Files:**
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/immersive/ImmersivePlayerActivity.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/ui/immersive/SquigglySeekBar.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/receiver/PowerConnectionReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `MediaPlaybackRepository`
- Produces: Fullscreen landscape StandBy player activity & auto-launch receiver

- [ ] **Step 1: Implement SquigglySeekBar Compose component**
Render interactive sine-wave progress seekbar matching Android 13+ / One UI aesthetic.

- [ ] **Step 2: Implement ImmersivePlayerActivity**
Fullscreen landscape layout: Left side organic/artistic mask album art; Right side large Title/Artist, M3 bold pill Play button, scallop Prev/Next buttons, Squiggly seekbar, Output chip. Include `FLAG_KEEP_SCREEN_ON`.

- [ ] **Step 3: Implement PowerConnectionReceiver for StandBy auto-trigger**
Listen for `ACTION_POWER_CONNECTED` and orientation changes; if music is playing and device is placed horizontally, automatically launch `ImmersivePlayerActivity`.

- [ ] **Step 4: Register Activity and Receiver in AndroidManifest.xml**
Configure `screenOrientation="sensorLandscape"` and window flags.

- [ ] **Step 5: Verify build**
Run: `./gradlew assembleDebug`

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/github/windsekirun/musicwidget/ui/immersive/ app/src/main/java/com/github/windsekirun/musicwidget/receiver/PowerConnectionReceiver.kt app/src/main/AndroidManifest.xml
git commit -m "feat: implement landscape ImmersivePlayerActivity and StandBy auto-launch"
```

---

### Task 9: Main App UI, Permissions Helper & Final End-to-End Verification

**Files:**
- Modify: `app/src/main/java/com/github/windsekirun/musicwidget/MainActivity.kt`
- Create: `app/src/main/java/com/github/windsekirun/musicwidget/util/PermissionUtils.kt`
- Modify: `app/src/main/res/layout/activity_main.xml`

**Interfaces:**
- Consumes: All components
- Produces: Fully functional application with setup guidance and test suite

- [ ] **Step 1: Implement PermissionUtils & MainActivity setup flow**
Check if Notification Access is granted (`NotificationManagerCompat.getEnabledListenerPackages`). If not, show clear setup guide and button opening `ACTION_NOTIFICATION_LISTENER_SETTINGS`.

- [ ] **Step 2: Run all unit tests**
Run: `./gradlew testDebugUnitTest`
Verify all tests pass.

- [ ] **Step 3: Run assembleDebug and lint/check**
Run: `./gradlew assembleDebug`

- [ ] **Step 4: Final commit**
```bash
git add app/src/main/
git commit -m "feat: complete MainActivity permission setup and end-to-end integration"
```
