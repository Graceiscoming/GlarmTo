# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository layout

The git root (`D:\GlarmTo`) holds docs (`README.md`, `idea.md`, `functionsum.md`, `update.md` — mostly Thai-language project notes/progress logs) and `figma(wireframe)`. The actual Android project is the nested `GlarmTo/` directory — run all Gradle commands from there.

GlarmTo is a single-module (`:app`) Android fitness tracker: Kotlin + Jetpack Compose (Material3), MVVM, Room, fully offline/local (no backend). Package `com.example.glarmto`, applicationId `com.graceiscoming.glarmto`, minSdk 24 / compileSdk 36, Java 11.

## Commands (run from `GlarmTo/`)

```
./gradlew assembleDebug                 # build debug APK
./gradlew assembleRelease               # minified + resource-shrunk (see app/proguard-rules.pro)
./gradlew testDebugUnitTest             # JVM unit tests (incl. Robolectric)
./gradlew testDebugUnitTest --tests "com.example.glarmto.GlarmToRepositoryTest"   # single class
./gradlew testDebugUnitTest --tests "*HealthCalculatorGoalsTest.someMethod"       # single test
./gradlew connectedDebugAndroidTest     # instrumented tests (needs device/emulator)
```

No lint/format tooling is configured beyond Android defaults. `clean_for_release.bat` deletes build caches/`.idea` for zipping/submission.

## Architecture

- **Entry / wiring**: `GlarmToApplication` is the manual DI container — lazily exposes `database`, `sessionManager`, `themeManager`, and `repository`. There is no Hilt/Koin; ViewModels are built through hand-written `*ViewModelFactory` classes (e.g. `CalculatorViewModelFactory`) that pull from the Application.
- **Navigation**: `MainActivity.kt` holds everything — `Screen` sealed class, `MainScreen` with `NavHost`, bottom bar (hidden on `welcome`/`register`/`login`/`onboarding` routes). Start destination is chosen from session state: no user → `welcome`; user without profile → `onboarding`; else `dashboard`. The Aura theme draws `AnimatedAuraBackground` behind the whole UI. `MainActivity.onUserLeaveHintCallback` is used for the PiP rest timer.
- **Data layer** (`data/`):
  - `local/` Room: single `AppDatabase` (entities: Workout, Nutrition, User, Routine, WorkoutSession, Water) with one DAO `GlarmToDao`. The DAO is deliberately non-`suspend` (avoids a Kotlin generic name-clash bug); the repository moves work to `Dispatchers.IO`.
  - **Schema changes require a hand-written `Migration` in `AppDatabase.kt`** and a version bump (currently version 12, `exportSchema = false`) — existing migrations are chained there.
  - `preferences/`: `SessionManager` (current user, local auth/multi-user on one device) and `ThemeManager` (5 themes), both SharedPreferences-backed.
  - `repository/GlarmToRepository.kt`: the single access point used by all ViewModels; also contains XP/level/streak logic.
  - `util/`: pure-Kotlin domain logic, which is where unit tests concentrate — `HealthCalculator` (TDEE/macros), `PlateCalculator`, `RecoveryCalculator` (48h muscle fatigue), `PoseAngleMath`, `WorkoutGenerator`, `ExerciseLibrary/Presets`, `CalorieBurnModel`, `GlarmToExport` (JSON/CSV), `ThaiProductDatabase` + `OpenFoodFactsApi` + `NutritionOcrParser` (barcode/label nutrition lookup), `InstagramShareHelper`.
- **UI layer** (`ui/<feature>/`): each feature has a `*Screen.kt` composable and usually a `*ViewModel.kt` exposing `StateFlow`s. Features: login/onboarding, dashboard (heatmap, streak), workout (voice logging, AI pose tracker via ML Kit, generator sheet, recovery), routines, nutrition (barcode scanner, water animation), calculator (also the "Profile" tab), history, camera, `widget/` (Glance app widget), `theme/`.
- **ML**: on-device only. ML Kit (pose detection, barcode, text recognition) + CameraX. `CalorieBurnModel` is a 5-feature linear regression with coefficients baked in as constants; they are produced offline by `ml/train_calorie_model.py` (numpy/pandas, reads `ml/calories.csv`) — re-run it and paste new constants if the model changes.

## Testing notes

Unit tests live in `app/src/test/java/com/example/glarmto/`. ViewModel/repository tests use Robolectric (`*RobolectricTest`) plus `kotlinx-coroutines-test`; `unitTests.isIncludeAndroidResources = true` is set for this. Dependency versions are in `gradle/libs.versions.toml` (CameraX, ML Kit, and accompanist versions are hardcoded in `app/build.gradle.kts`).
