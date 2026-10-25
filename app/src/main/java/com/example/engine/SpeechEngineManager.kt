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

    private enum class RecognizerSlot { SLOT_A, SLOT_B }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Main)
    private val beepSuppressor = AudioBeepSuppressor(context)

    @Volatile
    private var activeSlot = RecognizerSlot.SLOT_A
    private var recognizerA: SpeechRecognizer? = null
    private var recognizerB: SpeechRecognizer? = null

    private val listenerA: RecognitionListener = createRecognitionListener(RecognizerSlot.SLOT_A)
    private val listenerB: RecognitionListener = createRecognitionListener(RecognizerSlot.SLOT_B)

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
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Recognizer stuck in Starting; handoff retry (gen=$gen)")
                destroyRecognizerSlot(activeSlot)
                triggerPingPongHandoff(fromSlot = activeSlot, gen = gen)
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

    private fun createRecognitionListener(slot: RecognizerSlot): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (!isSessionActive || activeSlot != slot) return

                cancelStartTimeout()
                _dictationState.value = DictationState.Listening
                consecutiveErrors = 0
                resetIdleAutoStop()
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Ready for speech ($slot, gen=$sessionGeneration)")
            }

            override fun onBeginningOfSpeech() {
                if (!isSessionActive || activeSlot != slot) return
                cancelStartTimeout()
                markSpeech()
                _dictationState.value = DictationState.Listening
                resetIdleAutoStop()
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Beginning of speech ($slot)")
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (!isSessionActive || activeSlot != slot) return
                cancelStartTimeout()
                if (rmsdB > RMS_SPEECH_THRESHOLD_DB) {
                    markSpeech()
                }
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                val current = _smoothedRms.value
                _smoothedRms.value = current * 0.65f + normalized * 0.35f
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (!isSessionActive || activeSlot != slot) return
                _smoothedRms.value = 0f
                val s = settingsRepository.cachedSettings
                if (silenceElapsed() >= s.silenceCompleteMs) {
                    _dictationState.value = DictationState.Processing
                } else {
                    _dictationState.value = DictationState.Listening
                }
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "End of speech ($slot)")
            }

            override fun onError(error: Int) {
                if (!isSessionActive || activeSlot != slot) return
                val gen = sessionGeneration
                cancelStartTimeout()
                _smoothedRms.value = 0f
                if (!isSessionActive) {
                    beepSuppressor.restoreSessionVolumes(delayed = true)
                    return
                }

                handleRecognizerError(error, slot, gen)
            }

            override fun onResults(results: Bundle?) {
                if (!isSessionActive || activeSlot != slot) return
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
                    "Final result ($slot): [${resultText.take(40)}...] (gen=$gen)"
                )

                commitTextWithContinuationCheck(resultText)

                if (isSessionActive && gen == sessionGeneration) {
                    triggerPingPongHandoff(fromSlot = slot, gen = gen)
                } else {
                    _dictationState.value = DictationState.Idle
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!isSessionActive || activeSlot != slot) return
                cancelStartTimeout()
                resetIdleAutoStop()

                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull() ?: ""
                if (partial.isNotBlank()) {
                    markSpeech()
                    currentPartialText = partial
                    onPartialText(partial)
                }
            }

            override fun onSegmentResults(segmentResults: Bundle) {
                if (!isSessionActive || activeSlot != slot) return
                val matches = segmentResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val segmentText = matches?.firstOrNull() ?: ""
                if (segmentText.isNotBlank()) {
                    markSpeech()
                    _utteranceCount.value++
                    onFinalText(segmentText)
                }
            }

            override fun onEndOfSegmentedSession() {
                if (!isSessionActive || activeSlot != slot) return
                val gen = sessionGeneration
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "End of segmented session ($slot, gen=$gen)")
                triggerPingPongHandoff(fromSlot = slot, gen = gen)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startSession() {
        if (isSessionActive) return
        val currentGen = ++sessionGeneration
        isSessionActive = true
        activeSlot = RecognizerSlot.SLOT_A
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
        startListeningSlot(RecognizerSlot.SLOT_A, currentGen)
        DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Session started on SLOT_A (gen=$currentGen)")
    }

    fun stopSession() {
        val stopGen = ++sessionGeneration
        isSessionActive = false
        restartJob?.cancel()
        idleAutoStopJob?.cancel()
        sessionTimerJob?.cancel()
        cancelStartTimeout()

        runOnMainThread {
            try { recognizerA?.stopListening() } catch (ignored: Exception) {}
            try { recognizerA?.cancel() } catch (ignored: Exception) {}
            try { recognizerB?.stopListening() } catch (ignored: Exception) {}
            try { recognizerB?.cancel() } catch (ignored: Exception) {}

            destroyRecognizersInternal()

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
            try { recognizerA?.cancel() } catch (ignored: Exception) {}
            try { recognizerB?.cancel() } catch (ignored: Exception) {}
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
        startListeningSlot(activeSlot, currentGen)
        DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Session resumed on $activeSlot (gen=$currentGen)")
    }

    fun togglePause() {
        if (_dictationState.value is DictationState.Paused) {
            resumeSession()
        } else if (isSessionActive) {
            pauseSession()
        }
    }

    fun switchLanguageAndRestart() {
        if (!isSessionActive) return
        commitPendingPartial()

        runOnMainThread {
            try { recognizerA?.stopListening() } catch (ignored: Exception) {}
            try { recognizerA?.cancel() } catch (ignored: Exception) {}
            try { recognizerB?.stopListening() } catch (ignored: Exception) {}
            try { recognizerB?.cancel() } catch (ignored: Exception) {}
            destroyRecognizersInternal()

            val gen = ++sessionGeneration
            isSessionActive = true
            _dictationState.value = DictationState.Restarting

            restartJob?.cancel()
            restartJob = scope.launch {
                delay(280L)
                if (isSessionActive && gen == sessionGeneration) {
                    startListeningSlot(RecognizerSlot.SLOT_A, gen, isSilentReopen = false)
                }
            }
        }
    }

    private fun startListeningSlot(slot: RecognizerSlot, generation: Long, isSilentReopen: Boolean = false) {
        if (!isSessionActive || generation != sessionGeneration) return

        runOnMainThread {
            if (!isSessionActive || generation != sessionGeneration) return@runOnMainThread

            try {
                activeSlot = slot
                val recognizer = getOrCreateRecognizer(slot)
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

                try {
                    recognizer.cancel()
                } catch (ignored: Exception) {}

                armStartTimeout(generation)
                recognizer.startListening(intent)
            } catch (e: Exception) {
                DiagnosticsLog.log(
                    DiagnosticsLog.Category.ERROR,
                    "Failed to startListeningSlot ($slot): ${e.message}"
                )
                if (isSessionActive && generation == sessionGeneration) {
                    handleRecognizerError(SpeechRecognizer.ERROR_CLIENT, slot, generation)
                }
            }
        }
    }

    private fun triggerPingPongHandoff(fromSlot: RecognizerSlot, gen: Long) {
        if (!isSessionActive || gen != sessionGeneration) return

        val s = settingsRepository.cachedSettings
        val nextSlot = if (fromSlot == RecognizerSlot.SLOT_A) RecognizerSlot.SLOT_B else RecognizerSlot.SLOT_A

        // 1. Immediately cancel outgoing slot to release AudioRecord on Xiaomi MIUI 14
        try {
            if (fromSlot == RecognizerSlot.SLOT_A) recognizerA?.cancel() else recognizerB?.cancel()
        } catch (ignored: Exception) {}

        if (s.dictationMode == DictationMode.CONTINUOUS) {
            // Xiaomi MIUI 14 calibrated micro-handoff: 40ms to guarantee AudioPolicyManager audio release
            _dictationState.value = DictationState.Restarting
            restartJob?.cancel()
            restartJob = scope.launch {
                delay(XIAOMI_MIUI_HANDOFF_DELAY_MS)
                if (isSessionActive && gen == sessionGeneration) {
                    startListeningSlot(nextSlot, gen, isSilentReopen = true)
                }
            }
        } else {
            // Pause-and-restart mode with visual countdown
            scheduleRestart(delayMs = getRestartDelay(), generation = gen, targetSlot = nextSlot)
        }
    }

    private fun getOrCreateRecognizer(slot: RecognizerSlot): SpeechRecognizer {
        val existing = if (slot == RecognizerSlot.SLOT_A) recognizerA else recognizerB
        if (existing != null) return existing

        val settings = settingsRepository.cachedSettings
        val recognizer = if (settings.useOnDevice && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Creating on-device speech recognizer for $slot")
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            val component = findGoogleOrSelectedComponent(settings)
            if (component != null) {
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Creating speech recognizer for $slot with component: $component")
                SpeechRecognizer.createSpeechRecognizer(context, component)
            } else {
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        }

        val listener = if (slot == RecognizerSlot.SLOT_A) listenerA else listenerB
        recognizer.setRecognitionListener(listener)

        if (slot == RecognizerSlot.SLOT_A) {
            recognizerA = recognizer
        } else {
            recognizerB = recognizer
        }
        return recognizer
    }

    private fun destroyRecognizerSlot(slot: RecognizerSlot) {
        try {
            if (slot == RecognizerSlot.SLOT_A) {
                recognizerA?.destroy()
                recognizerA = null
            } else {
                recognizerB?.destroy()
                recognizerB = null
            }
        } catch (ignored: Exception) {}
    }

    private fun destroyRecognizersInternal() {
        try { recognizerA?.destroy() } catch (ignored: Exception) {}
        try { recognizerB?.destroy() } catch (ignored: Exception) {}
        recognizerA = null
        recognizerB = null
    }

    private fun handleRecognizerError(errorCode: Int, slot: RecognizerSlot, generation: Long) {
        if (!isSessionActive || generation != sessionGeneration || activeSlot != slot) return
        cancelStartTimeout()

        val errorMessage = getErrorDescription(errorCode)
        DiagnosticsLog.log(
            DiagnosticsLog.Category.ERROR,
            "Recognizer error ($errorCode) on $slot: $errorMessage (gen=$generation)"
        )

        val settings = settingsRepository.cachedSettings
        val isPauseError = (errorCode == SpeechRecognizer.ERROR_NO_MATCH || errorCode == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)

        if (isPauseError && isSessionActive) {
            commitPendingPartial()
            triggerPingPongHandoff(fromSlot = slot, gen = generation)
            return
        }

        // Xiaomi MIUI 14 specific handling:
        // If audio bus reported busy during handoff, do a fast 35ms micro-retry instead of long backoff
        if (errorCode == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "MIUI 14 audio bus busy on $slot; fast 35ms micro-retry")
            restartJob?.cancel()
            restartJob = scope.launch {
                delay(35L)
                if (isSessionActive && generation == sessionGeneration) {
                    startListeningSlot(slot, generation, isSilentReopen = true)
                }
            }
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

        val nextSlot = if (slot == RecognizerSlot.SLOT_A) RecognizerSlot.SLOT_B else RecognizerSlot.SLOT_A

        when (errorCode) {
            SpeechRecognizer.ERROR_CLIENT -> {
                if (consecutiveErrors >= 2) {
                    DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Recreating recognizer for $slot after $consecutiveErrors critical failures")
                    destroyRecognizerSlot(slot)
                }
                _dictationState.value = DictationState.Restarting
                if (isSessionActive && generation == sessionGeneration) {
                    scheduleRestart(delayMs = (settings.errorBackoffMs * consecutiveErrors).coerceAtLeast(100L), generation = generation, targetSlot = nextSlot)
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
                    scheduleRestart(delayMs = settings.errorBackoffMs * consecutiveErrors, generation = generation, targetSlot = nextSlot)
                }
            }
            else -> {
                _dictationState.value = DictationState.Restarting
                if (isSessionActive && generation == sessionGeneration) {
                    scheduleRestart(delayMs = settings.errorBackoffMs, generation = generation, targetSlot = nextSlot)
                }
            }
        }
    }

    private fun scheduleRestart(delayMs: Long, generation: Long, targetSlot: RecognizerSlot = activeSlot) {
        restartJob?.cancel()
        if (!isSessionActive || generation != sessionGeneration) return

        val settings = settingsRepository.cachedSettings
        if (settings.dictationMode == DictationMode.PAUSE_AND_RESTART && delayMs > 100) {
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
                    startListeningSlot(targetSlot, generation)
                }
            }
        } else {
            _dictationState.value = DictationState.Restarting
            restartJob = scope.launch {
                delay(delayMs)
                if (isSessionActive && generation == sessionGeneration) {
                    startListeningSlot(targetSlot, generation)
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
        val lang = settings.getActiveDictationLanguage()

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, settings.maxResults.coerceIn(1, 5))
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

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

            // Extended 30s continuous silence tolerances for uninterrupted dictation
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

        val targetPkg = settings.enginePackage
        for (info in services) {
            val serviceInfo = info.serviceInfo ?: continue
            if (serviceInfo.packageName == targetPkg) {
                return ComponentName(serviceInfo.packageName, serviceInfo.name)
            }
        }

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
        destroyRecognizersInternal()
        beepSuppressor.release()
    }

    companion object {
        private const val RMS_SPEECH_THRESHOLD_DB = 4.0f
        private const val XIAOMI_MIUI_HANDOFF_DELAY_MS = 40L
    }
}
