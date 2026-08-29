# ADR 0003: Upgrade compileSdk to 36

## Date
2026-08-29

## Context
During the Phase 5 integration, we upgraded several AndroidX dependencies, including `androidx.core:core-ktx:1.18.0` and Jetpack Compose runtime artifacts. Gradle builds subsequently failed because these modern bleeding-edge libraries strictly require compiling against Android API version 35 or 36 (Android 15). The project's `compileSdk` in `app/build.gradle.kts` was pinned to 34, resulting in compatibility rejections during resource resolution.

## Decision
We updated both `compileSdk` and `targetSdk` from 34 to 36 in `app/build.gradle.kts` to satisfy the prerequisites of the modern AndroidX and Compose ecosystem. 

## Consequences
- **Positive:** Resolves all Android API version mismatch errors during the Gradle build. Ensures the app takes advantage of the latest Android platform optimizations and UI paradigms.
- **Negative:** Potentially introduces subtle behavioral shifts on Android 15 devices due to targeting API 36, which requires future QA testing on Android 15 emulators to ensure backward compatibility and system-level compliance.
