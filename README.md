# Horton (Android)

Horton is an entirely offline, privacy-first Android application designed to silently record, transcribe, and summarize your meetings and conversations without ever sending your sensitive audio data to the cloud.

Horton operates fully on-device, leveraging cutting-edge localized AI pipelines for Voice Activity Detection (VAD), Speech-to-Text (STT), and Large Language Model (LLM) reasoning. 

## Key Features

- **Offline-First Privacy:** All audio processing, transcription, and summarization happens natively on your Android device. Zero data leaves your phone.
- **Background Recording:** Horton can continuously record in the background via a persistent foreground service. A convenient notification drawer action allows you to stop the session instantly.
- **Real-Time Voice Activity Detection:** Utilizes the highly optimized Sherpa-ONNX Silero VAD engine to identify active speech segments and trim dead air automatically.
- **Edge LLM Summarization:** Integrates Google's MediaPipe Tasks GenAI to run a localized Gemma 3 INT4 LLM model to distill raw transcripts into actionable tasks and professional summaries.
- **Modern Android Architecture:** Built with Kotlin, Jetpack Compose Material 3, and Kotlin Coroutines.
- **Persistent Storage:** Backed by a robust Room Database (compiled via KSP) to organize and retrieve your past conversations and meeting notes.
- **Dynamic UI:** Features beautiful, modern UI components like the animated `PulseVisualizer` for active recording sessions and Markdown rendering for LLM-generated summaries.

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Database:** Room (via Google's KSP)
- **Local LLM Engine:** [Google MediaPipe GenAI](https://developers.google.com/mediapipe) (v0.10.27)
- **VAD/STT Engine:** [Sherpa-ONNX](https://github.com/k2-fsa/sherpa-onnx) (v1.10.37)
- **Build System:** Gradle (Kotlin DSL), Android API 36 Target

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer).
- JDK 17+
- Android emulator or physical device running Android 8.0 (API 26) or higher.
- (Recommended) A device with decent RAM for smooth on-device LLM inference.

### Installation

1. **Clone the repository:**
   ```bash
   git clone https://github.com/himanshu13ps/horton-android.git
   cd horton-android
   ```

2. **Sherpa-ONNX Binary (Already Included):**
   Due to missing artifacts on standard remote Maven repositories, the 34MB `sherpa-onnx-1.10.37.aar` file is already manually embedded in the `app/libs/` directory. You do not need to hunt for this dependency online!

3. **Open in Android Studio:**
   Open the project folder via Android Studio. Allow Gradle to sync and resolve the AndroidX Compose dependencies.

4. **Build & Run:**
   Build the project via the Android Studio interface, or use the command line:
   ```bash
   ./gradlew assembleDebug
   ```

## Architecture Documentation

Detailed documentation on the architectural decisions and build infrastructure changes made during development can be found in the [specs/adr](specs/adr/) folder.

## License
MIT License