# Phase 5: User Interface Polish & Notification Actions

With the core ML inference and data architecture rock-solid, this final phase focuses on elevating the user experience, rendering the LLM outputs beautifully, and improving system-level convenience.

## Proposed Changes

### 1. Markdown Rendering for AI Notes
- The LLM generates structured Markdown (headers, lists, bolding). Currently, the `ReviewScreen` just dumps this as raw string text.
- We will add a robust Compose Markdown library dependency to `build.gradle.kts`.
- Update the `ReviewScreen` in `UIComponents.kt` to use the `MarkdownText` composable, ensuring the "Extracted Notes" tab looks like a professional, formatted document.

### 2. Audio Visualizer Animation
- The `ActiveRecordingScreen` currently shows a static placeholder `[ Audio Visualizer ]`.
- We will implement a dynamic, multi-bar visualizer animation using Compose `InfiniteTransition` and `Animatable` to give the user active visual feedback that the app is "listening" while the recording is active.

### 3. Foreground Service Notification Stop Action
- When the app is in the background, the user sees a persistent recording notification.
- We will add a `PendingIntent` "Stop" action directly onto the `NotificationCompat.Builder` in `RecordingService.kt`.
- This allows the user to stop the recording and trigger note synthesis directly from their lock screen or notification shade, without needing to open the app.

## Open Questions

1. Do you approve the addition of a Markdown parsing library (e.g., `com.github.jeziellago:compose-markdown`) for rendering the LLM outputs?
2. For the visualizer, would you prefer a simple expanding/contracting circle (like Siri/Google Assistant) or a classic multi-bar equalizer effect?
