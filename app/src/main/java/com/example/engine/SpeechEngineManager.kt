package com.example.engine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.settings.DictationMode
import com.example.settings.KothaSettings
import com.example.settings.SettingsRepository
import com.example.util.AudioBeepSuppressor
import com.example.util.DiagnosticsLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DictationState {
    object Idle : DictationState()
    object Starting : DictationState()
    object Listening : DictationState()
    object Processing : DictationState()
    object Restarting : DictationState()
    data class WaitingRestart(val remainingMs: Long, val totalMs: Long) : DictationState()
    object Paused : DictationState()
    data class Error(val code: Int, val message: String) : DictationState()
}

data class SpeechEngineInfo(
    val packageName: String,
    val serviceName: String,
    val label: String
)

class SpeechEngineManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onPartialText: (String) -> Unit,
    private val onFinalText: (String) -> Unit
) {

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)
    private val beepSuppressor = AudioBeepSuppressor(context)

    private var speechRecognizer: SpeechRecognizer? = null
    @Volatile
    private var isSessionActive = false
    @Volatile
    private var sessionGeneration = 0L

    @Volatile
    private var lastSpeechAtMs = 0L

    private fun markSpeech() {
        lastSpeechAtMs = SystemClock.elapsedRealtime()
    }

    private fun silenceElapsed(): Long = SystemClock.elapsedRealtime() - lastSpeechAtMs

    private var consecutiveErrors = 0
    private var restartJob: Job? = null
    private var idleAutoStopJob: Job? = null
    private var sessionTimerJob: Job? = null

    private var currentPartialText = ""
    private var lastPartialCommitted = false
    private var lastCommittedText = ""
    private var startTimeoutRunnable: Runnable? = null

    private fun cancelStartTimeout() {
        startTimeoutRunnable?.let { handler.removeCallbacks(it) }
        startTimeoutRunnable = null
    }

    private fun armStartTimeout(gen: Long) {
        cancelStartTimeout()
        startTimeoutRunnable = Runnable {
            if (_dictationState.value is DictationState.Starting && isSessionActive && gen == sessionGeneration) {
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Recognizer stuck in Starting; restarting (gen=$gen)")
                destroyRecognizer()
                scheduleRestart(delayMs = 100L, generation = gen)
            }
        }
        handler.postDelayed(startTimeoutRunnable!!, 3500L)
    }

    private fun commitPendingPartial() {
        val pending = currentPartialText.trim()
        if (pending.isNotEmpty()) {
            currentPartialText = ""
            lastPartialCommitted = true
            lastCommittedText = pending
            markSpeech()
            _utteranceCount.value++
            onFinalText(pending)
        }
    }

    private fun commitTextWithContinuationCheck(phrase: String) {
        var textToCommit = phrase.trim()
        currentPartialText = ""
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
        lastCommittedText = textToCommit
        if (textToCommit.isNotEmpty()) {
            markSpeech()
            _utteranceCount.value++
            onFinalText(textToCommit)
        }
    }

    private val _dictationState = MutableStateFlow<DictationState>(DictationState.Idle)
    val dictationState: StateFlow<DictationState> = _dictationState.asStateFlow()

    private val _smoothedRms = MutableStateFlow(0f)
    val smoothedRms: StateFlow<Float> = _smoothedRms.asStateFlow()

    private val _sessionDurationSec = MutableStateFlow(0L)
    val sessionDurationSec: StateFlow<Long> = _sessionDurationSec.asStateFlow()

    private val _utteranceCount = MutableStateFlow(0)
    val utteranceCount: StateFlow<Int> = _utteranceCount.asStateFlow()

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            val gen = sessionGeneration
            if (!isSessionActive) return

            cancelStartTimeout()
            _dictationState.value = DictationState.Listening
            consecutiveErrors = 0 // Reset error count on successful start
            resetIdleAutoStop()
            DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Ready for speech (gen=$gen)")
        }

        override fun onBeginningOfSpeech() {
            if (!isSessionActive) return
            cancelStartTimeout()
            markSpeech()
            _dictationState.value = DictationState.Listening
            resetIdleAutoStop()
            DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Beginning of speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (!isSessionActive) return
            cancelStartTimeout()
            if (rmsdB > RMS_SPEECH_THRESHOLD_DB) {
                markSpeech()
            }
            // Normalise dB (-2 to 10) to 0.0..1.0
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            val current = _smoothedRms.value
            _smoothedRms.value = current * 0.65f + normalized * 0.35f
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            if (!isSessionActive) return
            // CRITICAL: DO NOT restart or cancel here!
            // The engine is processing audio and will fire onResults or onError within 50-200ms.
            // Calling stopListening() or cancel() here will discard final words.
            val s = settingsRepository.cachedSettings
            if (silenceElapsed() >= s.silenceCompleteMs) {
                _dictationState.value = DictationState.Processing
            } else {
                _dictationState.value = DictationState.Listening
            }
            _smoothedRms.value = 0f
            DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "End of speech")
        }

        override fun onError(error: Int) {
            val gen = sessionGeneration
            cancelStartTimeout()
            _smoothedRms.value = 0f
            if (!isSessionActive) {
                beepSuppressor.restoreSessionVolumes(delayed = true)
                return
            }

            handleRecognizerError(error, gen)
        }

        override fun onResults(results: Bundle?) {
            val gen = sessionGeneration
            cancelStartTimeout()
            _smoothedRms.value = 0f
            if (!isSessionActive) {
                beepSuppressor.restoreSessionVolumes(delayed = true)
                return
            }

            consecutiveErrors = 0
            resetIdleAutoStop()

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val resultText = matches?.firstOrNull() ?: ""

            DiagnosticsLog.log(
                DiagnosticsLog.Category.RECOGNIZER,
                "Final result: [${resultText.take(40)}...] (gen=$gen)"
            )

            commitTextWithContinuationCheck(resultText)

            if (isSessionActive && gen == sessionGeneration) {
                onEngineSegmentEnded(gen)
            } else {
                _dictationState.value = DictationState.Idle
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isSessionActive) return
            cancelStartTimeout()
            resetIdleAutoStop() // User is speaking, prevent idle auto-stop

            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partial = matches?.firstOrNull() ?: ""
            if (partial.isNotBlank()) {
                markSpeech()
                currentPartialText = partial
                onPartialText(partial)
            }
        }

        override fun onSegmentResults(segmentResults: Bundle) {
            if (!isSessionActive) return
            val matches = segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val segmentText = matches?.firstOrNull() ?: ""
            if (segmentText.isNotBlank()) {
                markSpeech()
                _utteranceCount.value++
                onFinalText(segmentText)
            }
        }

        override fun onEndOfSegmentedSession() {
            val gen = sessionGeneration
            if (!isSessionActive) return
            DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "End of segmented session (gen=$gen)")
            onEngineSegmentEnded(gen)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startSession() {
        if (isSessionActive) return
        val currentGen = ++sessionGeneration
        isSessionActive = true
        consecutiveErrors = 0
        _sessionDurationSec.value = 0L
        _utteranceCount.value = 0
        markSpeech()

        val settings = settingsRepository.cachedSettings
        if (settings.beepSuppression) {
            beepSuppressor.suppressForSession()
        }

        startSessionTimer()
        resetIdleAutoStop()
        startListeningInternal(currentGen)
        DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Session started (gen=$currentGen)")
    }

    fun stopSession() {
        val stopGen = ++sessionGeneration // Invalidate any pending posted callbacks
        isSessionActive = false
        restartJob?.cancel()
        idleAutoStopJob?.cancel()
        sessionTimerJob?.cancel()
        cancelStartTimeout()

        runOnMainThread {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (ignored: Exception) {}

            // Cleanly destroy recognizer on normal stop to prevent stale listener/state
            destroyRecognizerInternal()

            // Swallow cancellation tone with 400ms delayed restore
            beepSuppressor.restoreSessionVolumes(delayed = true)
            _dictationState.value = DictationState.Idle
            _smoothedRms.value = 0f
            currentPartialText = ""
        }
        DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Session stopped (invalidated up to gen=$stopGen)")
    }

    fun pauseSession() {
        if (!isSessionActive) return
        isSessionActive = false
        restartJob?.cancel()
        idleAutoStopJob?.cancel()
        sessionTimerJob?.cancel()
        cancelStartTimeout()

        runOnMainThread {
            try {
                speechRecognizer?.cancel()
            } catch (ignored: Exception) {}
            beepSuppressor.restoreSessionVolumes(delayed = true)
            _dictationState.value = DictationState.Paused
            _smoothedRms.value = 0f
            currentPartialText = ""
        }
        DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Session paused")
    }

    fun resumeSession() {
        if (isSessionActive) return
        val currentGen = ++sessionGeneration
        isSessionActive = true
        _dictationState.value = DictationState.Starting
        markSpeech()

        startSessionTimer()
        resetIdleAutoStop()
        startListeningInternal(currentGen)
        DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Session resumed (gen=$currentGen)")
    }

    fun togglePause() {
        if (_dictationState.value is DictationState.Paused) {
            resumeSession()
        } else if (isSessionActive) {
            pauseSession()
        }
    }

    private fun startListeningInternal(generation: Long, isSilentReopen: Boolean = false) {
        if (!isSessionActive || generation != sessionGeneration) return

        runOnMainThread {
            // Guard again inside main thread queue to prevent start/stop race
            if (!isSessionActive || generation != sessionGeneration) return@runOnMainThread

            try {
                val recognizer = getOrCreateRecognizer()
                val intent = buildRecognizerIntent()

                if (!isSilentReopen) {
                    _dictationState.value = DictationState.Starting
                } else {
                    _dictationState.value = DictationState.Listening
                }

                val settings = settingsRepository.cachedSettings
                if (settings.beepSuppression) {
                    beepSuppressor.muteBeforeStart()
                }

                // Defensively cancel previous active/stale state before starting to avoid ERROR_CLIENT
                try {
                    recognizer.cancel()
                } catch (ignored: Exception) {}

                armStartTimeout(generation)
                recognizer.startListening(intent)
            } catch (e: Exception) {
                DiagnosticsLog.log(
                    DiagnosticsLog.Category.ERROR,
                    "Failed to startListening: ${e.message}"
                )
                if (isSessionActive && generation == sessionGeneration) {
                    handleRecognizerError(SpeechRecognizer.ERROR_CLIENT, generation)
                }
            }
        }
    }

    private fun getOrCreateRecognizer(): SpeechRecognizer {
        speechRecognizer?.let { return it }

        val settings = settingsRepository.cachedSettings
        val recognizer = if (settings.useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Creating on-device speech recognizer")
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            val component = findGoogleOrSelectedComponent(settings)
            if (component != null) {
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Creating speech recognizer with component: $component")
                SpeechRecognizer.createSpeechRecognizer(context, component)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        }

        recognizer.setRecognitionListener(recognitionListener)
        speechRecognizer = recognizer
        return recognizer
    }

    private fun destroyRecognizerInternal() {
        try {
            speechRecognizer?.destroy()
        } catch (ignored: Exception) {}
        speechRecognizer = null
    }

    private fun destroyRecognizer() {
        runOnMainThread {
            destroyRecognizerInternal()
        }
    }

    private fun handleRecognizerError(errorCode: Int, generation: Long) {
        if (!isSessionActive || generation != sessionGeneration) return
        cancelStartTimeout()

        val errorMessage = getErrorDescription(errorCode)
        DiagnosticsLog.log(
            DiagnosticsLog.Category.ERROR,
            "Recognizer error ($errorCode): $errorMessage (gen=$generation)"
        )

        val settings = settingsRepository.cachedSettings
        val isPauseError = (errorCode == SpeechRecognizer.ERROR_NO_MATCH || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)

        if (isPauseError && isSessionActive) {
            commitPendingPartial()
            _dictationState.value = DictationState.Restarting
            onEngineSegmentEnded(generation)
            return
        }

        commitPendingPartial()

        consecutiveErrors++
        if (consecutiveErrors >= settings.maxConsecutiveFailures) {
            _dictationState.value = DictationState.Error(errorCode, errorMessage)
            isSessionActive = false
            restartJob?.cancel()
            idleAutoStopJob?.cancel()
            sessionTimerJob?.cancel()
            beepSuppressor.restoreSessionVolumes(delayed = true)
            return
        }

        when (errorCode) {
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
            SpeechRecognizer.ERROR_CLIENT -> {
                // Reuse recognizer instance: only destroy and recreate if 2 or more consecutive critical failures
                if (consecutiveErrors >= 2) {
                    DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Recreating recognizer after $consecutiveErrors critical failures")
                    destroyRecognizer()
                }
                _dictationState.value = DictationState.Restarting
                if (isSessionActive && generation == sessionGeneration) {
                    scheduleRestart(delayMs = (settings.errorBackoffMs * consecutiveErrors).coerceAtLeast(150L), generation = generation)
                }
            }
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                _dictationState.value = DictationState.Error(errorCode, "Microphone permission required")
                isSessionActive = false
                restartJob?.cancel()
                idleAutoStopJob?.cancel()
                sessionTimerJob?.cancel()
                beepSuppressor.restoreSessionVolumes(delayed = true)
            }
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
            SpeechRecognizer.ERROR_SERVER -> {
                _dictationState.value = DictationState.Restarting
                if (isSessionActive && generation == sessionGeneration) {
                    scheduleRestart(delayMs = settings.errorBackoffMs * consecutiveErrors, generation = generation)
                }
            }
            else -> {
                _dictationState.value = DictationState.Restarting
                if (isSessionActive && generation == sessionGeneration) {
                    scheduleRestart(delayMs = settings.errorBackoffMs, generation = generation)
                }
            }
        }
    }

    private fun onEngineSegmentEnded(gen: Long) {
        if (!isSessionActive || gen != sessionGeneration) return
        val s = settingsRepository.cachedSettings
        val elapsed = silenceElapsed()
        if (elapsed < s.silenceCompleteMs) {
            // User is still inside their thinking window: reopen the mic.
            DiagnosticsLog.log(
                DiagnosticsLog.Category.RECOGNIZER,
                "Silent reopen (silence=${elapsed}ms of ${s.silenceCompleteMs}ms) (gen=$gen)"
            )
            _dictationState.value = DictationState.Restarting
            restartJob?.cancel()
            restartJob = scope.launch {
                delay(MIN_REOPEN_DELAY_MS) // min gap prevents tight loops and ERROR_RECOGNIZER_BUSY
                if (isSessionActive && gen == sessionGeneration) {
                    startListeningInternal(gen, isSilentReopen = true)
                }
            }
        } else {
            // Real silence of the full configured length: use the existing behavior
            DiagnosticsLog.log(
                DiagnosticsLog.Category.RECOGNIZER,
                "Silence window complete (silence=${elapsed}ms >= ${s.silenceCompleteMs}ms) (gen=$gen)"
            )
            _dictationState.value = DictationState.Restarting
            scheduleRestart(delayMs = getRestartDelay(), generation = gen)
        }
    }

    private fun scheduleRestart(delayMs: Long, generation: Long) {
        restartJob?.cancel()
        if (!isSessionActive || generation != sessionGeneration) return

        val settings = settingsRepository.cachedSettings
        if (settings.dictationMode == DictationMode.PAUSE_AND_RESTART && delayMs > 100) {
            // Countdown visualizer
            restartJob = scope.launch {
                val totalMs = delayMs
                var remaining = totalMs
                val step = 50L
                while (remaining > 0 && isSessionActive && generation == sessionGeneration) {
                    _dictationState.value = DictationState.WaitingRestart(remaining, totalMs)
                    delay(step)
                    remaining -= step
                }
                if (isSessionActive && generation == sessionGeneration) {
                    startListeningInternal(generation)
                }
            }
        } else {
            _dictationState.value = DictationState.Restarting
            restartJob = scope.launch {
                delay(delayMs)
                if (isSessionActive && generation == sessionGeneration) {
                    startListeningInternal(generation)
                }
            }
        }
    }

    private fun getRestartDelay(): Long {
        val settings = settingsRepository.cachedSettings
        return if (settings.dictationMode == DictationMode.CONTINUOUS) {
            settings.seamlessRestartMs
        } else {
            settings.restartDelayMs
        }
    }

    private fun buildRecognizerIntent(): Intent {
        val settings = settingsRepository.cachedSettings
        val lang = if (settings.customBcp47.isNotBlank()) settings.customBcp47 else settings.primaryLanguage

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, settings.maxResults.coerceIn(1, 5))
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

            // Only request offline recognition if the user enabled it (Bangla requires cloud recognition)
            if (settings.preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putExtra(RecognizerIntent.EXTRA_MASK_OFFENSIVE_WORDS, settings.maskOffensiveWords)
                if (settings.preferSegmentedSession) {
                    putExtra("android.speech.extra.SEGMENTED_SESSION", true)
                }
            }

            if (settings.enableFormatting) {
                putExtra("android.speech.extra.ENABLE_FORMATTING", true)
            }

            // Silence timing & dictation extras: extended 30s in continuous mode to prevent premature OS recognizer timeouts!
            val silenceMs = if (settings.dictationMode == DictationMode.CONTINUOUS) 30000L else settings.silenceCompleteMs.coerceIn(800L, 8000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMs)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceMs)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, if (settings.dictationMode == DictationMode.CONTINUOUS) 30000L else silenceMs.coerceAtLeast(1500L))

            putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", silenceMs)
            putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", silenceMs)
            putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", if (settings.dictationMode == DictationMode.CONTINUOUS) 30000L else silenceMs.coerceAtLeast(1500L))
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }
        return intent
    }

    private fun findGoogleOrSelectedComponent(settings: KothaSettings): ComponentName? {
        val pm = context.packageManager
        val services = pm.queryIntentServices(
            Intent(RecognitionService.SERVICE_INTERFACE),
            PackageManager.MATCH_ALL
        )

        // Try selected service
        val targetPkg = settings.enginePackage
        for (info in services) {
            val serviceInfo = info.serviceInfo ?: continue
            if (serviceInfo.packageName == targetPkg) {
                return ComponentName(serviceInfo.packageName, serviceInfo.name)
            }
        }

        // Try Google search box recognition service fallback
        for (info in services) {
            val serviceInfo = info.serviceInfo ?: continue
            if (serviceInfo.packageName.contains("google", ignoreCase = true)) {
                return ComponentName(serviceInfo.packageName, serviceInfo.name)
            }
        }

        return null
    }

    fun getInstalledEngines(): List<SpeechEngineInfo> {
        val pm = context.packageManager
        val services = pm.queryIntentServices(
            Intent(RecognitionService.SERVICE_INTERFACE),
            PackageManager.MATCH_ALL
        )
        return services.mapNotNull { resolveInfo ->
            val sInfo = resolveInfo.serviceInfo ?: return@mapNotNull null
            val label = sInfo.loadLabel(pm).toString()
            SpeechEngineInfo(
                packageName = sInfo.packageName,
                serviceName = sInfo.name,
                label = label
            )
        }
    }

    private fun resetIdleAutoStop() {
        idleAutoStopJob?.cancel()
        val settings = settingsRepository.cachedSettings
        if (settings.idleAutoStopSec > 0 && isSessionActive) {
            val gen = sessionGeneration
            idleAutoStopJob = scope.launch {
                delay(settings.idleAutoStopSec * 1000L)
                if (isSessionActive && gen == sessionGeneration) {
                    DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Idle auto-stop triggered after ${settings.idleAutoStopSec}s")
                    stopSession()
                }
            }
        }
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        val gen = sessionGeneration
        sessionTimerJob = scope.launch {
            while (isSessionActive && gen == sessionGeneration) {
                delay(1000L)
                if (isSessionActive && gen == sessionGeneration) {
                    _sessionDurationSec.value++
                }
            }
        }
    }

    private fun getErrorDescription(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network operation timed out"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognised (pause)"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error occurred"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timed out (pause)"
            SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "Cannot check recognition service"
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported"
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language currently unavailable"
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Server disconnected"
            else -> "Speech recognition error ($errorCode)"
        }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            handler.post(block)
        }
    }

    fun release() {
        stopSession()
        destroyRecognizer()
        beepSuppressor.release()
    }

    companion object {
        private const val RMS_SPEECH_THRESHOLD_DB = 4.0f
        private const val MIN_REOPEN_DELAY_MS = 150L
    }
}
