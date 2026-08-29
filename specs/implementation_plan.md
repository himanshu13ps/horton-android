# Project Implementation Plan

## Phase 1: Foundation and UI Architecture

### 1. Project Scaffolding
- Refactor the project structure to separate concerns into specific packages: `ui`, `audio`, `ml`, `data`, `service`, and `utils`.
- Update `build.gradle.kts` with required Jetpack Compose, Room, and Coroutines dependencies.

### 2. Room Database Setup
- **`NoteDatabase.kt`**: Configure the Room Database with standard best practices.
- **`NoteDao.kt`**: Implement DAO for complex relational queries (`getConversationWithDetails`) and standard CRUD operations.
- **Entities**: Define `ConversationEntity`, `AudioSegmentEntity`, `TranscriptEntity`, and `ExtractedNoteEntity`.

### 3. ML & Audio Stubs
- Create foundational classes to define the API boundaries before real SDK integration:
  - **`RecordingService.kt`**: Foreground service orchestrator.
  - **`AudioCaptureManager.kt`**: Microphone tap.
  - **`VADProcessor.kt`**: Chunk evaluation and segmentation.
  - **`TranscriptionEngine.kt`**: STT inference.
  - **`NoteSynthesisEngine.kt`**: LLM interaction.

### 4. Jetpack Compose UI
- **`MainViewModel.kt`**: Establish strict Unidirectional Data Flow using `StateFlow` and defined `AppState` (Dashboard, ActiveRecording, Review).
- **`UIComponents.kt`**: Build out the dynamic Material 3 views, including a custom `GeminiBackground` for aesthetic flair, and horizontal paging for the review screen.

---

## Phase 2: Background Execution & Audio Pipeline

### 1. Build & Permissions Integration
- Add the `sherpa-onnx` library and Google Accompanist permissions to `build.gradle.kts`.
- Implement `RECORD_AUDIO` runtime permission checks seamlessly in the `DashboardScreen` before allowing the service to start.

### 2. Audio Pipeline Wiring
- Update **`RecordingService.kt`** to execute concurrent coroutines:
  - Route the raw `ByteArray` output from `AudioCaptureManager` directly into `VADProcessor`.
  - Listen to the `completedFilesFlow` from the `VADProcessor` and pass the emitted `.wav` files directly to the `TranscriptionEngine`.
- Hook up the `MainActivity` start/stop callbacks to emit explicit `Intents` that manage the foreground service lifecycle.

### 3. ML Engine Initialization
- Refactor **`VADProcessor.kt`** to construct and utilize the official `com.k2fsa.sherpa.onnx.Vad` bindings targeting a Silero model.
- Refactor **`TranscriptionEngine.kt`** to initialize the real `OfflineRecognizer` leveraging Zipformer INT8 configurations.

---

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
