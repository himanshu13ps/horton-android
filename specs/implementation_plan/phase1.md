# Phase 1: Foundation and UI Architecture

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
