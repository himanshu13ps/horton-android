# On-Device AI Note-Taker Architecture Walkthrough

The foundational architecture for the On-Device AI Note-Taker application has been fully instantiated. Following the Unidirectional Data Flow pattern and utilizing advanced edge-computing principles, the system is now capable of managing continuous background audio processing, segmentation, transcription, and synthesis.

## 1. Project Configuration & Permissions

- **Build Configuration**: The project has been configured in `app/build.gradle.kts` to target SDK 34 with a `minSdk` of 26.
- **Dependencies**: The required dependencies were added, including Jetpack Compose, Room (with KAPT compiler), Kotlin Coroutines, Markwon (for Markdown rendering), and critically, `com.google.mediapipe:tasks-genai:0.10.27`.
- **Manifest Setup**: All necessary permissions (`RECORD_AUDIO`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MICROPHONE`, `WAKE_LOCK`) have been declared in `AndroidManifest.xml`, alongside the `RecordingService` declaration.

## 2. Background Execution & Audio Capture

The continuous background listening mechanism has been implemented and fully connected:
- **[RecordingService.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/service/RecordingService.kt)**: Acts as the central orchestrator. It launches concurrent Coroutines to capture raw microphone data via `AudioCaptureManager`, streams those bytes directly into the `VADProcessor`, and simultaneously listens to the VAD's emitted file paths, routing them into the `TranscriptionEngine`.
- **[AudioCaptureManager.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/audio/AudioCaptureManager.kt)**: Responsible for tapping into the raw microphone stream, configured for `16,000 Hz`, `16-bit PCM`, and `MONO` channel audio to perfectly align with STT requirements.

## 3. The Audio Pipeline (VAD & STT)

- **[VADProcessor.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/audio/VADProcessor.kt)**: Fully integrated with the official `com.k2fsa.sherpa.onnx.Vad` component using the Silero ONNX model. It evaluates 32.5ms chunks and maintains a pre-roll buffer to prevent truncating the first spoken words, waiting for a 0.25-second trailing silence before finalizing the `.wav` file.
- **[TranscriptionEngine.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/ml/TranscriptionEngine.kt)**: Refactored to utilize the real `OfflineRecognizer` from Sherpa-ONNX. It is configured to load the INT8 Zipformer models for efficient edge processing.

## 4. The LLM Synthesis Engine

- **[NoteSynthesisEngine.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/ml/NoteSynthesisEngine.kt)**: The edge reasoning component utilizing Google's MediaPipe Tasks GenAI. It is configured to target a Gemma 3 INT4 `.task` file, with a strict factual persona via the system prompt (temperature = 0.2). It concatenates the raw transcripts of the session, asynchronously generates Markdown output, persists the result to Room, and handles the critical `.close()` native cleanup.

## 5. Persistence via Room Database

- **[NoteDatabase.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/data/NoteDatabase.kt)**: The robust relational schema defined in the blueprint has been implemented.
  - Entities created: `ConversationEntity`, `AudioSegmentEntity`, `TranscriptEntity`, `ExtractedNoteEntity`.
  - The `NoteDao` provides transactional access and reactive `Flow` wrappers for seamless UI updates.

## 6. Jetpack Compose User Interface & Permissions

- **[UIComponents.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/ui/UIComponents.kt)**: Features a dynamic Gemini-style UI with breathing gradients. Integrated the Google Accompanist permissions library to seamlessly request the mandatory `RECORD_AUDIO` permission from the user on the dashboard before starting a session.
## 7. In-App Model Downloader & Settings

- **[FileDownloader.kt](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/utils/FileDownloader.kt)**: A robust Kotlin Coroutines flow-based utility that downloads multi-megabyte machine learning models from standard HTTP/HTTPS endpoints, emitting progress percentages in real-time.
- **[UIComponents.kt (SettingsScreen)](file:///C:/workspace/github/my-projects/horton-android/app/src/main/java/com/himanshu13ps/horton/ui/UIComponents.kt)**: A dedicated Settings interface allowing users to download the required STT Encoder/Decoder, VAD, and GenAI LLM individually. It hooks into the `DownloadManager` flow to render dynamic progress bars.
- **Dashboard Integrity Check**: Upon `MainActivity` launch, the `MainViewModel` scans the private app storage (`context.filesDir/models/`) to verify all 6 required models exist. If any are missing, the user is presented with a red Material 3 warning card on the Dashboard, directing them to the Settings screen to fetch the files. The app dynamically guards against engine crashes by checking these conditions before initializing Sherpa-ONNX or MediaPipe.

## 8. ML Engine Execution Pipeline

- **WAV Parsing (`TranscriptionEngine`)**: Added custom bitwise parsing logic to convert the 16-bit PCM `.wav` files generated by the VAD into normalized `FloatArray` formats. The STT engine now securely feeds this array into `recognizer.createStream()`, extracting the recognized string and timestamp directly from the ONNX models.
- **Edge LLM Inference (`NoteSynthesisEngine`)**: The app successfully initializes Google MediaPipe's `LlmInferenceSession`. The app constructs a `systemPrompt` wrapping the concatenated session transcripts, executes `generateResponse` natively on the device hardware, and correctly triggers the `session.close()` C++ native memory free to prevent ANRs or Memory Leaks.
- **App Flow**: Pressing "Stop Recording" in the UI gracefully triggers the STT cleanup and seamlessly hands off execution to the `NoteSynthesisEngine` running safely on a background Kotlin Coroutine inside the `MainViewModel`.
## 9. UI Polish and Notification Actions
- **Markdown Rendering**: Added dev.jeziellago.compose.markdowntext library to render the LLM response beautifully on the Review Screen.
- **Visualizer Animation**: Built a custom PulseVisualizer composable utilizing InfiniteTransition and Canvas to show a dynamic expanding/contracting listening animation during active recordings.
- **Quick Stop Action**: Attached a PendingIntent directly to the foreground service notification, allowing users to stop the recording session straight from their lockscreen.
