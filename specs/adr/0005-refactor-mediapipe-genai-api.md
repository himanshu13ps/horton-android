# ADR 0005: Refactor MediaPipe GenAI API Usage

## Date
2026-08-29

## Context
During the Phase 5 build stabilization, Kotlin compilation failed in `NoteSynthesisEngine.kt` due to unresolved references when calling MediaPipe's `LlmInferenceSession`, `setTemperature()`, and `setTopK()`. It became apparent that the MediaPipe Tasks GenAI API version `0.10.27` introduced breaking changes that drastically simplified the interface, stripping out the session-based architecture and inference parameters in favor of a direct, state-free generation methodology.

## Decision
We refactored `NoteSynthesisEngine.kt` to align with the new `0.10.27` API standard. We removed all imports and instantiations of `LlmInferenceSession` and deleted the unresolved `setTemperature(0.2f)` and `setTopK(40)` builder methods. The synchronous LLM execution was reduced to a direct call: `llmInference!!.generateResponse(systemPrompt)`.

## Consequences
- **Positive:** Fixes compilation errors and modernizes the inference pipeline to match the newest SDK. Simplifies native memory management since we no longer have to explicitly `.close()` a short-lived `LlmInferenceSession` for every query.
- **Negative:** We lost granular control over temperature and Top-K sampling parameters for the LLM output at runtime, as those configurations were removed from the `LlmInferenceOptions` builder in this SDK version.
