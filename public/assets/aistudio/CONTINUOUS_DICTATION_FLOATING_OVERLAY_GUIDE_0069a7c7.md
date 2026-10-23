# Continuous Dictation & Floating Live Transcription Guide

A technical reference and implementation guide for Android apps using Jetpack Compose and `android.speech.SpeechRecognizer`.

---

## 1. System Overview & The Interruption Problem

In continuous dictation, Android's `SpeechRecognizer` naturally stops listening when speech pauses or when the OS speech buffer expires, firing `onEndOfSpeech()`, followed by `onResults()` or `onError()`. During the transition before `onReadyForSpeech()` is called again, there is a **dead gap (150–500 ms)** where any spoken words are lost.

### Key Solutions Applied in This Architecture
1. **Explicit Gap State Machine**: A 6-state sealed interface (`Idle`, `Starting`, `Listening`, `Restarting`, `Paused`, `Error`) exposes engine restarts to the UI in real time.
2. **Visual Gap Indicator ("Bipping Dot")**: A dynamic status dot that changes color and pulse mode based on whether voice is actively being captured or if the engine is restarting.
3. **Floating Transcription Pill**: A floating overlay that renders live partial results when speaking, and clear gap prompts (`"Listening... speak now"`, `"Restarting engine..."`, `"Dictation paused"`) when in a gap.
4. **Zero-Word-Loss Commit Logic**: Any uncommitted partial text is safely flushed (`commitPendingPartial()`) before restarting, with prefix de-duplication on subsequent results.
5. **Extended Silence Intent Extras**: Speech timeouts are configured up to 30 seconds to minimize unnecessary OS restarts.

---

## 2. State Machine: Surfacing Restarts to UI

Instead of a binary `isListening: Boolean`, use an explicit state machine:

```kotlin
sealed interface DictationState {
    object Idle : DictationState
    object Starting : DictationState
    object Listening : DictationState
    object Restarting : DictationState
    object Paused : DictationState
    data class Error(val message: String, val isRecoverable: Boolean = true) : DictationState
}
```

### State Flow in Engine Supervisor:
- `_state`: `StateFlow<DictationState>`
- `_partialText`: `StateFlow<String>` (live interim transcription)
- `_finalCommittedText`: `SharedFlow<String>` (committed phrases)
- `_audioLevelRms`: `StateFlow<Float>` (normalized 0.0 to 1.0 from `onRmsChanged`)

---

## 3. The Visual Gap Indicator & Floating Transcription Pill

### Compose Implementation

```kotlin
@Composable
fun FloatingLiveTranscriptionOverlay(
    dictationState: DictationState,
    partialText: String,
    modifier: Modifier = Modifier
) {
    val isSessionActive = dictationState !is DictationState.Idle

    AnimatedVisibility(
        visible = isSessionActive,
        enter = fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 3 },
        exit = fadeOut(tween(180)) + slideOutVertically(tween(180)) { it / 3 },
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
            border = BorderStroke(
                width = 1.5.dp,
                color = when (dictationState) {
                    is DictationState.Listening -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    is DictationState.Starting, is DictationState.Restarting -> Color(0xFF2196F3).copy(alpha = 0.6f)
                    is DictationState.Paused -> Color(0xFFFFB300).copy(alpha = 0.6f)
                    is DictationState.Error -> Color(0xFFE53935).copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.outline
                }
            ),
            shadowElevation = 8.dp,
            modifier = Modifier.widthIn(min = 140.dp, max = 560.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bipping / Pulsing Dot
                BippingStatusDot(
                    dictationState = dictationState,
                    hasPartialText = partialText.isNotBlank()
                )

                // Live Transcription Text or Gap Status
                val statusText = when {
                    partialText.isNotBlank() -> partialText
                    dictationState is DictationState.Listening -> "Listening... speak now"
                    dictationState is DictationState.Starting -> "Starting engine..."
                    dictationState is DictationState.Restarting -> "Restarting... hold on"
                    dictationState is DictationState.Paused -> "Dictation paused"
                    dictationState is DictationState.Error -> "Reconnecting..."
                    else -> "Listening..."
                }

                Text(
                    text = statusText,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontStyle = if (partialText.isBlank()) FontStyle.Italic else FontStyle.Normal,
                        fontWeight = if (partialText.isNotBlank()) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (partialText.isNotBlank()) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        }
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun BippingStatusDot(
    dictationState: DictationState,
    hasPartialText: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speech_dot_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_pulse_alpha"
    )

    val (dotColor, dotAlpha) = when {
        hasPartialText -> MaterialTheme.colorScheme.primary to 1.0f
        dictationState is DictationState.Listening -> MaterialTheme.colorScheme.primary to pulseAlpha
        dictationState is DictationState.Starting || dictationState is DictationState.Restarting -> Color(0xFF2196F3) to pulseAlpha
        dictationState is DictationState.Paused -> Color(0xFFFFB300) to 1.0f
        dictationState is DictationState.Error -> Color(0xFFE53935) to 1.0f
        else -> MaterialTheme.colorScheme.onSurfaceVariant to 0.4f
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(dotColor.copy(alpha = dotAlpha))
    )
}
```

### Visual State Meaning for the User:
| State | Dot Color | Dot Behavior | Overlay Text | User Guidance |
|---|---|---|---|---|
| **Actively Capturing** | Primary (Green/Theme) | Solid (1.0 Alpha) | *User's spoken words in real time* | Keep speaking. |
| **Ready for Voice** | Primary | Breathing Pulse (0.35 ↔ 1.0) | `"Listening... speak now"` | Start speaking. |
| **Restart Gap** | Blue | Rapid Flash/Pulse | `"Restarting... hold on"` | **Hold speaking until dot turns solid/primary.** |
| **Engine Starting** | Blue | Pulse | `"Starting engine..."` | Wait a fraction of a second. |
| **Paused** | Amber | Solid | `"Dictation paused"` | Tap Mic to resume. |
| **Transient Error** | Red | Solid | `"Reconnecting..."` | Recovering connection automatically. |

---

## 4. Speech Supervisor Restart Logic (Fixing Dropped Words)

### 1. Extending Silence Durations via RecognizerIntent
Prevent the OS recognizer from terminating too quickly (default is often 1–2 seconds):

```kotlin
val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    
    // Extend silence tolerances up to 30 seconds
    val silenceMs = 30000L
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMs)
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMs)
    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, silenceMs)
}
```

### 2. Never Restart in `onEndOfSpeech()`
```kotlin
override fun onEndOfSpeech() {
    _audioLevelRms.value = 0f
    // CRITICAL: DO NOT restart or cancel here!
    // The engine is processing audio and will fire onResults or onError within 50-200ms.
    // Calling stopListening() or cancel() here will discard the final words!
}
```

### 3. Preserving Partial Text Before Restart
When speech times out or disconnects (`ERROR_SPEECH_TIMEOUT` / `ERROR_NO_MATCH`), immediately commit whatever was visible in the floating overlay:

```kotlin
private fun commitPendingPartial() {
    val pending = _partialText.value.trim()
    if (pending.isNotEmpty()) {
        _partialText.value = ""
        lastPartialCommitted = true
        lastCommittedText = pending
        _finalCommittedText.tryEmit(pending)
    }
}

override fun onError(error: Int) {
    cancelStartTimeout()
    _audioLevelRms.value = 0f
    
    when (error) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
            commitPendingPartial()
            // Mark state as Restarting so UI shows the gap
            _state.value = DictationState.Restarting
            mainHandler.post {
                scheduleRestart(immediate = true)
            }
        }
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
        SpeechRecognizer.ERROR_CLIENT -> {
            commitPendingPartial()
            try { speechRecognizer?.cancel() } catch (_: Exception) {}
            _state.value = DictationState.Restarting
            scheduleRestart(immediate = false, delayMs = 150)
        }
        else -> {
            commitPendingPartial()
            _state.value = DictationState.Error("Reconnecting...", isRecoverable = true)
            scheduleRestart(immediate = false, delayMs = 500)
        }
    }
}
```

### 4. Seamless Result Continuation & De-duplication
When `onResults` fires after an interim partial commit, strip out already committed prefixes:

```kotlin
private fun commitTextWithContinuationCheck(phrase: String) {
    var textToCommit = phrase.trim()
    if (textToCommit.isEmpty()) return

    if (lastPartialCommitted && lastCommittedText.isNotEmpty()) {
        if (textToCommit.equals(lastCommittedText, ignoreCase = true)) {
            lastPartialCommitted = false
            return
        } else if (textToCommit.startsWith(lastCommittedText, ignoreCase = true)) {
            textToCommit = textToCommit.substring(lastCommittedText.length).trimStart()
        }
    }

    lastPartialCommitted = false
    _partialText.value = ""
    lastCommittedText = textToCommit
    if (textToCommit.isNotEmpty()) {
        _finalCommittedText.tryEmit(textToCommit)
    }
}
```

### 5. Watchdog for Stuck "Starting" Recognizers
If `SpeechRecognizer` gets stuck during binding:
```kotlin
cancelStartTimeout()
startTimeoutRunnable = Runnable {
    if (_state.value is DictationState.Starting && isSessionActive) {
        destroyRecognizer()
        scheduleRestart(immediate = true)
    }
}
mainHandler.postDelayed(startTimeoutRunnable!!, 3500L)
// Cancel startTimeout in onReadyForSpeech(), onBeginningOfSpeech(), onRmsChanged(), or onPartialResults()
```

### 6. Suppressing Periodic Android Beep Sounds
To prevent the Android OS chime from beeping on every loop:
- Mute `AudioManager.STREAM_MUSIC` and `AudioManager.STREAM_SYSTEM` **once** when continuous dictation starts.
- Keep them muted through restarts.
- Restore stream volumes **once** after a 400ms delay when the user explicitly stops dictation.

---

## 5. Summary Checklist for Porting to Another App
1. **Model DictationState as a sealed interface** with `Starting` and `Restarting`.
2. **Bind the floating pill UI directly to `DictationState` and `partialText`**.
3. **Animate the Bipping Dot**: Solid on active speech, breathing pulse when listening, distinct color/flash during `Restarting`.
4. **Commit partial text in `onError` and `onResults`** before triggering restart.
5. **Never cancel inside `onEndOfSpeech`**.
6. **Pass 30-second silence extras** in the `RecognizerIntent`.
