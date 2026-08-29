# Architectural Blueprint and Implementation Strategy for an On-Device AI Note-Taker Application

## Overview
The development of an intelligent, fully autonomous note-taking application that continuously listens to extended conversations, segments audio streams, transcribes speech, and synthesizes structured notes entirely on-device. Such an architecture guarantees absolute user privacy, zero latency associated with network round-trips, and uninterrupted functionality in offline environments.

## Core Technologies
1. **Platform**: Android (Target API 34, MinSdk 26)
2. **Language**: Kotlin
3. **UI Framework**: Jetpack Compose (Material 3)
4. **Database**: Room (SQLite)
5. **VAD & STT**: Sherpa-ONNX (Silero VAD + Zipformer INT8)
6. **LLM Inference**: MediaPipe Tasks GenAI (Gemma 3 1B INT4)
7. **Concurrency**: Kotlin Coroutines & Flow

## Architecture Pipeline

### 1. Foreground Audio Capture
- **Service**: `RecordingService` running as a foreground service with `FOREGROUND_SERVICE_TYPE_MICROPHONE`.
- **Power Management**: Uses `PowerManager.PARTIAL_WAKE_LOCK` to prevent the device from sleeping during long meetings when the screen is off.
- **Audio Spec**: `AudioRecord` configured to capture 16kHz, 16-bit PCM, Mono audio.

### 2. Voice Activity Detection (VAD)
- **Engine**: Official `sherpa-onnx` Silero VAD.
- **Chunking**: Evaluates 520-sample chunks (32.5ms).
- **State Machine**:
  - `PASSIVE`: Listening for speech above probability threshold. Maintains a 5-chunk pre-roll buffer to prevent truncation.
  - `ACTIVE`: Actively writing raw bytes to a uniquely generated `.wav` file in `context.cacheDir`.
  - `TRAILING`: Speech has stopped, but waits for a minimum 0.25-second trailing silence before finalizing the file and emitting the file path.

### 3. Speech-to-Text (STT)
- **Engine**: `sherpa-onnx` OfflineRecognizer using Zipformer INT8 models.
- **Execution**: A background coroutine consumes the `.wav` paths emitted by the VAD processor, runs offline greedy-search inference, and inserts the resulting raw text chunks sequentially into the Room database.

### 4. Note Synthesis (LLM)
- **Engine**: Google MediaPipe `tasks-genai` running a quantized Gemma 3 INT4 `.task` model.
- **Execution**: When a recording session ends, all transcript chunks for the session are concatenated. The LLM evaluates the full context using a strict system prompt to extract key decisions and action items, generating Markdown-formatted notes.

### 5. Persistence
- **Database**: Room
- **Entities**:
  - `ConversationEntity`: Metadata about the recording session.
  - `AudioSegmentEntity`: Tracks temporary `.wav` files.
  - `TranscriptEntity`: Stores sequential text chunks.
  - `ExtractedNoteEntity`: Stores the final LLM-generated Markdown.
  
### 6. Dynamic Downloader
- **Mechanism**: Due to Android storage constraints and large model sizes, ML models (STT Encoder/Decoder, VAD, LLM) are downloaded in-app via `FileDownloader` to the private `context.filesDir/models/` directory.
- **Verification**: The app dynamically verifies the existence of required models on boot and directs users to a Settings UI for downloading missing components.
