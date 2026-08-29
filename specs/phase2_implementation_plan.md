# Phase 2: Background Execution & Audio Pipeline

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
