# Phase 4: ML Engine Execution & Application Flow Finalization

While the architecture and models are wired up dynamically, the internal logic of the machine learning engines is currently stubbed to prevent crashes during initial development. Now that the app can download and store the real `.onnx` and `.task` models securely, we must complete the execution logic.

## User Review Required

> [!WARNING]
> **MediaPipe GenAI Initialization**
> In `NoteSynthesisEngine`, initializing the `LlmInference` session takes significant CPU and RAM, especially for a 1B+ parameter model. Initializing this on the main thread will cause an ANR (Application Not Responding) crash. I will ensure this initialization and inference runs inside a `Dispatchers.IO` coroutine.

## Proposed Changes

### 1. Finalize STT Inference (`TranscriptionEngine.kt`)
- Remove the stubbed string `Transcribed text for segment $sequenceIndex`.
- Implement a WAV parsing mechanism (or use Sherpa-ONNX's `WaveReader` if available in the JNI bindings) to read the raw `.wav` files emitted by the VAD processor into a `FloatArray`.
- Call the Sherpa-ONNX engine's `createStream()`, feed the float array, and `decode()` the result.
- Extract the real transcribed `text` from the ONNX engine stream.

### 2. Finalize LLM Synthesis (`NoteSynthesisEngine.kt`)
- Remove the stubbed Markdown output.
- Instantiate `LlmInference.createFromOptions()` safely inside a try-catch block when the models exist.
- Replace the stub generation with a real call to `llmInference.generateResponse(systemPrompt)`.
- Ensure native memory `.close()` is called correctly on the model after inference to prevent catastrophic memory leaks.

### 3. Application Flow Integration
- **`RecordingService.kt`**: Ensure it gracefully shuts down the `TranscriptionEngine` upon `ACTION_STOP`. 
- **`MainViewModel.kt`**: When the user presses "Stop Recording" (triggering `endSession()`), the ViewModel should instantiate or call `NoteSynthesisEngine` to generate the notes based on the database transcripts, rather than just jumping to the Review screen empty-handed. We will add a simple background job in the ViewModel to manage this synthesis state seamlessly.

## Verification Plan
1. Ensure the code compiles cleanly.
2. Confirm the exact methods (`generateResponse`, `createStream`, etc.) match the official Sherpa-ONNX and MediaPipe Kotlin SDKs.
