# ADR 0004: Upgrade Room to 2.7.0-alpha11

## Date
2026-08-29

## Context
Following our migration from `kapt` to `ksp` for the Room database compiler (see ADR 0001), the Kotlin compilation phase began crashing with a cryptic error: `unexpected jvm signature V`. This error is a known incompatibility between older Room versions (`2.6.1`) and bleeding-edge Kotlin compilers (version `2.3+`), specifically when KSP attempts to parse coroutine-based (`suspend`) DAOs that return `Unit` (represented by the JVM signature `V`).

## Decision
We upgraded the Room dependencies (`room-runtime`, `room-ktx`, and `room-compiler`) in `build.gradle.kts` to version `2.7.0-alpha11`. This alpha version contains the upstream fixes from Google for modern KSP coroutine signature parsing.

## Consequences
- **Positive:** Cures the `unexpected jvm signature V` compiler crash and fully unblocks the Kotlin `assembleDebug` build pipeline.
- **Negative:** Introduces an `alpha` tier library into the data layer. While acceptable for a prototype/early stage app, it may expose the project to undocumented Room compiler bugs or API shifts before reaching a stable 2.7.0 release.
