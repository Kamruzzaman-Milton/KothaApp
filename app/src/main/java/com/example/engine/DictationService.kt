package com.example.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.KothaApplication
import com.example.MainActivity
import com.example.R
import com.example.bridge.TextBridgeService
import com.example.bridge.TextBridgeStateHolder
import com.example.bridge.TextInsertionManager
import com.example.overlay.OrbWindowManager
import com.example.settings.DictationMode
import com.example.settings.KothaSettings
import com.example.settings.SettingsRepository
import com.example.settings.VisibilityRule
import com.example.util.AccessibilityKeeper
import com.example.util.DiagnosticsLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DictationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var speechEngineManager: SpeechEngineManager
    private lateinit var textInsertionManager: TextInsertionManager
    private lateinit var orbWindowManager: OrbWindowManager
    private lateinit var accessibilityKeeper: AccessibilityKeeper

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Crash Safety: Restore audio stream volumes if a previous crash occurred
        com.example.util.AudioBeepSuppressor.restoreCrashSafetyIfNeeded(this)

        settingsRepository = (application as KothaApplication).settingsRepository
        accessibilityKeeper = (application as KothaApplication).accessibilityKeeper

        textInsertionManager = TextInsertionManager(this)

        speechEngineManager = SpeechEngineManager(
            context = this,
            settingsRepository = settingsRepository,
            onPartialText = { partial ->
                textInsertionManager.handlePartialResult(partial, settingsRepository.cachedSettings)
            },
            onFinalText = { final ->
                textInsertionManager.handleFinalResult(
                    rawFinalText = final,
                    settings = settingsRepository.cachedSettings,
                    onVoiceCommand = { cmd ->
                        // Handle voice command callback
                    }
                )
            }
        )

        orbWindowManager = OrbWindowManager(
            context = this,
            settingsRepository = settingsRepository,
            onSingleTap = {
                handleOrbSingleTap()
            },
            onDoubleTap = {
                handleOrbDoubleTap()
            },
            onUndo = {
                textInsertionManager.performUndo()
            },
            onTogglePause = {
                speechEngineManager.togglePause()
            },
            onHideOrb = {
                stopSelf()
            }
        )

        // Observe settings changes
        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                orbWindowManager.updateSettings(settings)
                val isKeyboardVisible = TextBridgeStateHolder.isKeyboardVisible.value
                val isEditable = TextBridgeStateHolder.focusedSnapshot.value?.isEditable == true
                orbWindowManager.updateVisibility(isKeyboardVisible, isEditable, settings)
            }
        }

        // Observe Dictation Engine State
        serviceScope.launch {
            speechEngineManager.dictationState.collectLatest { state ->
                orbWindowManager.updateState(state)
                updateNotification(state)
            }
        }

        // Observe RMS Audio level
        serviceScope.launch {
            speechEngineManager.smoothedRms.collectLatest { rms ->
                orbWindowManager.updateAudioLevel(rms)
            }
        }

        // Observe Caption Text (live streaming)
        serviceScope.launch {
            textInsertionManager.liveCaptionText.collectLatest { caption ->
                orbWindowManager.updateCaption(caption, isStreaming = true)
            }
        }

        // Observe Final Inserted Utterance (lingers clearly above orb so user can review what they said)
        serviceScope.launch {
            textInsertionManager.lastInsertedUtterance.collectLatest { utterance ->
                if (utterance.isNotBlank()) {
                    orbWindowManager.updateCaption(utterance, isStreaming = false)
                }
            }
        }

        // Observe Bridge Connection
        serviceScope.launch {
            TextBridgeStateHolder.isConnected.collectLatest { connected ->
                orbWindowManager.updateBridgeConnected(connected)
            }
        }

        // Observe Keyboard Visible & Height
        serviceScope.launch {
            TextBridgeStateHolder.isKeyboardVisible.collectLatest { visible ->
                val height = TextBridgeStateHolder.keyboardHeightPx.value
                val settings = settingsRepository.cachedSettings
                orbWindowManager.handleKeyboardVisibility(visible, height, settings)

                val isEditable = TextBridgeStateHolder.focusedSnapshot.value?.isEditable == true
                orbWindowManager.updateVisibility(visible, isEditable, settings)

                // Voice typing lifecycle: in KEYBOARD_VISIBLE mode, automatically shut down dictation when keyboard closes
                if (!visible && settings.visibilityRule == VisibilityRule.KEYBOARD_VISIBLE) {
                    if (speechEngineManager.dictationState.value !is DictationState.Idle) {
                        speechEngineManager.stopSession()
                    }
                }
            }
        }

        // Observe Focused Node
        serviceScope.launch {
            TextBridgeStateHolder.focusedSnapshot.collectLatest { snapshot ->
                val isKeyboardVisible = TextBridgeStateHolder.isKeyboardVisible.value
                val isEditable = snapshot?.isEditable == true
                orbWindowManager.updateVisibility(isKeyboardVisible, isEditable, settingsRepository.cachedSettings)
            }
        }

        // Start observing accessibility health
        accessibilityKeeper.startObserving()

        // Show floating Orb
        val currentSettings = settingsRepository.cachedSettings
        orbWindowManager.showOrb(currentSettings)
        val initialKb = TextBridgeStateHolder.isKeyboardVisible.value
        val initialEditable = TextBridgeStateHolder.focusedSnapshot.value?.isEditable == true
        orbWindowManager.updateVisibility(initialKb, initialEditable, currentSettings)

        // Start Foreground Notification
        startForegroundNotification()

        DiagnosticsLog.log(DiagnosticsLog.Category.SYSTEM, "DictationService created and running")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE_LISTEN -> {
                handleOrbSingleTap()
            }
            ACTION_START -> {
                speechEngineManager.startSession()
            }
            ACTION_STOP -> {
                speechEngineManager.stopSession()
            }
            ACTION_PAUSE -> {
                speechEngineManager.pauseSession()
            }
            ACTION_RESUME -> {
                speechEngineManager.resumeSession()
            }
            ACTION_UNDO -> {
                textInsertionManager.performUndo()
            }
            ACTION_HIDE_ORB -> {
                stopSelf()
            }
            ACTION_STOP_REPAIR -> {
                accessibilityKeeper.stopAutoRepairByUser()
            }
        }
        return START_STICKY
    }

    private fun handleOrbSingleTap() {
        if (!TextBridgeStateHolder.isConnected.value) {
            // Bridge is down -> attempt immediate repair or prompt accessibility settings
            val repaired = accessibilityKeeper.attemptRepairIfNeeded("OrbTap")
            if (!repaired) {
                val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            }
            return
        }

        val currentState = speechEngineManager.dictationState.value
        if (currentState is DictationState.Listening || currentState is DictationState.Starting || currentState is DictationState.WaitingRestart || currentState is DictationState.Restarting) {
            speechEngineManager.stopSession()
        } else if (currentState is DictationState.Paused) {
            speechEngineManager.resumeSession()
        } else {
            speechEngineManager.startSession()
        }
    }

    private fun handleOrbDoubleTap() {
        val settings = settingsRepository.cachedSettings
        // Toggle Dictation Mode (Continuous <-> Pause-and-Restart)
        val newMode = if (settings.dictationMode == DictationMode.CONTINUOUS) {
            DictationMode.PAUSE_AND_RESTART
        } else {
            DictationMode.CONTINUOUS
        }
        settingsRepository.updateSettings { it.copy(dictationMode = newMode) }
    }

    private fun startForegroundNotification() {
        val channelId = "kotha_dictation_channel"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val notification = buildNotification(channelId, speechEngineManager.dictationState.value)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(state: DictationState) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification("kotha_dictation_channel", state)
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(channelId: String, state: DictationState): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pOpenApp = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pStart = PendingIntent.getService(
            this,
            2,
            Intent(this, DictationService::class.java).apply { action = ACTION_START },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pStop = PendingIntent.getService(
            this,
            3,
            Intent(this, DictationService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pPause = PendingIntent.getService(
            this,
            4,
            Intent(this, DictationService::class.java).apply { action = ACTION_PAUSE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pResume = PendingIntent.getService(
            this,
            5,
            Intent(this, DictationService::class.java).apply { action = ACTION_RESUME },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pUndo = PendingIntent.getService(
            this,
            6,
            Intent(this, DictationService::class.java).apply { action = ACTION_UNDO },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pHide = PendingIntent.getService(
            this,
            7,
            Intent(this, DictationService::class.java).apply { action = ACTION_HIDE_ORB },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pOpenApp)
            .setOngoing(true)

        when (state) {
            is DictationState.Listening -> {
                builder.setContentTitle(getString(R.string.notification_title_listening))
                builder.setContentText("Kotha • কথা (${settingsRepository.cachedSettings.primaryLanguage})")
                builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_stop), pStop)
                builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_pause), pPause)
            }
            is DictationState.Restarting,
            is DictationState.WaitingRestart -> {
                builder.setContentTitle(getString(R.string.notification_title_waiting))
                builder.setContentText("Kotha • কথা (${settingsRepository.cachedSettings.primaryLanguage})")
                builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_stop), pStop)
                builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_pause), pPause)
            }
            is DictationState.Paused -> {
                builder.setContentTitle(getString(R.string.notification_title_paused))
                builder.setContentText("Dictation paused. Tap Resume to continue.")
                builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_resume), pResume)
                builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_stop), pStop)
            }
            else -> {
                builder.setContentTitle(getString(R.string.notification_title_active))
                builder.setContentText("Kotha • কথা (${settingsRepository.cachedSettings.primaryLanguage})")
                builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_start), pStart)
            }
        }

        builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_undo), pUndo)
        builder.addAction(R.drawable.ic_notification, getString(R.string.notification_action_hide), pHide)

        return builder.build()
    }

    override fun onDestroy() {
        speechEngineManager.release()
        orbWindowManager.hideOrb()
        accessibilityKeeper.stopObserving()
        serviceScope.cancel()
        instance = null
        DiagnosticsLog.log(DiagnosticsLog.Category.SYSTEM, "DictationService destroyed and cleaned up")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_TOGGLE_LISTEN = "com.example.action.TOGGLE_LISTEN"
        const val ACTION_UNDO = "com.example.action.UNDO"
        const val ACTION_HIDE_ORB = "com.example.action.HIDE_ORB"
        const val ACTION_STOP_REPAIR = "com.example.action.STOP_REPAIR"

        var instance: DictationService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
