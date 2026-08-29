# Phase 3: In-App Model Downloader & Settings UI

Based on your request, we will build an integrated download manager to fetch the required ML models directly within the app, providing a seamless user experience.

## User Review Required

> [!WARNING]
> **Storage Location Constraint**
> You requested downloading the files to `/data/local/temp/horton/`. However, standard Android applications (without root access) **do not have permission** to write to the `/data/local/tmp/` directory. If we attempt to save files there, the OS will throw a `PermissionDenied` exception. 
> 
> **Proposed Solution:** I will implement the downloader to save these models to the app's private internal storage (`context.filesDir/models/`). This is the Android standard and ensures the app can read/write the models securely without requiring root or ADB. I will also update the VAD and STT engines to load the models from this internal directory. 

## Proposed Changes

### 1. App State & View Model Updates
- **`MainViewModel.kt`**:
  - Introduce an `AppState.Settings` state.
  - Implement a `checkModelsExist()` routine that verifies the presence of the model files in `context.filesDir/models/` upon initialization.
  - Expose a `missingModels` state to the UI.
  - Create a `DownloadManager` utility class utilizing Kotlin Coroutine `Flows` to download files via `HttpURLConnection` and emit progress percentages.

### 2. Dashboard UI Warning
- **`UIComponents.kt` (`DashboardScreen`)**:
  - If `missingModels` is not empty, display a prominent Material 3 `Card` at the top of the dashboard warning the user that ML models are missing.
  - Include a "Go to Settings" button inside the warning card.
  - Disable the "Start Recording" FAB if models are missing to prevent crashes.

### 3. Settings UI
- **`UIComponents.kt` (`SettingsScreen`)**:
  - A new dedicated screen listing the required models (e.g., STT Zipformer, Silero VAD, Gemma 3).
  - Each item will display its status ("Downloaded", "Missing", or "Downloading...").
  - If missing, a "Download" button will be available.
  - During download, a `LinearProgressIndicator` will display real-time progress driven by the Coroutine `Flow`.

### 4. Engine Configuration Updates
- Update `VADProcessor` and `TranscriptionEngine` to initialize using dynamic paths from `context.filesDir/models/` rather than hardcoded `/data/local/tmp/` paths.

## Open Questions

1. Do you approve changing the storage directory to the app's internal `filesDir` to avoid Android permission crashes?
2. For the model URLs, I will use direct URLs to the official Sherpa-ONNX HuggingFace repositories as defaults. Is this acceptable?
