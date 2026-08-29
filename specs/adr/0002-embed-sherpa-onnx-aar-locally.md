# ADR 0002: Embed Sherpa-ONNX AAR Locally

## Date
2026-08-29

## Context
Our offline Speech-To-Text processing relies heavily on `Sherpa-ONNX` (version 1.10.37). During the build process, Gradle failed to resolve the dependency `com.k2fsa.sherpa.onnx:sherpa-onnx:1.10.37` from Maven Central. Although Sherpa-ONNX documentation suggests the artifact is available on standard repositories, the `1.10.37` Android package was inexplicably missing from remote endpoints, causing hard build failures.

## Decision
Instead of relying on unstable or non-existent remote snapshots, we decided to manually download the `sherpa-onnx-1.10.37.aar` file (34MB) directly from the official Sherpa-ONNX GitHub Releases page. We placed the binary artifact into the local `app/libs/` directory and linked it in `build.gradle.kts` via `implementation(files("libs/sherpa-onnx-1.10.37.aar"))`. The artifact was subsequently committed to the git repository.

## Consequences
- **Positive:** Guarantees a fully reproducible, offline-capable build without being at the mercy of remote repository outages or undocumented artifact name changes.
- **Negative:** Bloats the git repository by 34MB. However, this is well within the 100MB GitHub limit. Upgrading to a newer version of Sherpa-ONNX in the future will require manual downloading and file replacement.
