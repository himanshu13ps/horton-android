# ADR 0001: Migrate from KAPT to KSP for Room Database

## Date
2026-08-29

## Context
During the implementation of Phase 5, the Android Gradle Plugin (AGP) version 9 compilation broke due to the legacy `kapt` plugin. We initially attempted to bypass the issue by temporarily inserting the deprecated flag `android.builtInKotlin=false` into `gradle.properties`. However, this hack caused severe side effects, completely breaking Android Studio's modern Domain Specific Language (DSL) parsing for the `jvmToolchain(17)` block in the app's `build.gradle.kts`. The compiler flagged an error indicating that the extension `java` could not be found because the standard Kotlin DSL bindings failed to resolve properly under that flag.

## Decision
We decided to completely remove the deprecated `kapt` plugin and the `android.builtInKotlin=false` flag. Instead, we migrated the Room database compiler to Google's modern `ksp` (Kotlin Symbol Processing) engine.

## Consequences
- **Positive:** Resolves the AGP 9 build conflicts and restores full functionality to the Android Studio Kotlin DSL editor. Speeds up compilation times since KSP does not require generating Java stubs like KAPT.
- **Negative:** Required strict version alignment between Kotlin (`2.3.10`) and KSP in `libs.versions.toml`. Introduced downstream edge cases with KSP coroutine parsing that required further updates to Room (documented in ADR 0004).
