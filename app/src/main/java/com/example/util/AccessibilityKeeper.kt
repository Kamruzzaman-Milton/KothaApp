package com.example.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.bridge.TextBridgeService
import com.example.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class AccessibilityKeeper(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val targetComponent = ComponentName(context, TextBridgeService::class.java).flattenToString()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(Dispatchers.Default)

    private val repairTimestamps = ConcurrentLinkedQueue<Long>()
    private val MAX_REPAIRS_PER_MINUTE = 5

    private val _repairEventsCount = MutableStateFlow(0)
    val repairEventsCount: StateFlow<Int> = _repairEventsCount.asStateFlow()

    private val _isAutoRepairStopped = MutableStateFlow(false)
    val isAutoRepairStopped: StateFlow<Boolean> = _isAutoRepairStopped.asStateFlow()

    private val _hasSecureSettingsPermission = MutableStateFlow(false)
    val hasSecureSettingsPermission: StateFlow<Boolean> = _hasSecureSettingsPermission.asStateFlow()

    private var contentObserver: ContentObserver? = null
    private var periodicJob: Job? = null
    private var isObserverRegistered = false

    init {
        checkPermission()
    }

    fun checkPermission(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_SECURE_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
        _hasSecureSettingsPermission.value = granted
        return granted
    }

    fun startObserving() {
        if (isObserverRegistered) return
        checkPermission()

        contentObserver = object : ContentObserver(handler) {
            private var lastTriggerTime = 0L
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                val now = System.currentTimeMillis()
                if (now - lastTriggerTime > 1000L) { // Debounce 1s
                    lastTriggerTime = now
                    scope.launch {
                        delay(500)
                        attemptRepairIfNeeded(source = "ContentObserver")
                    }
                }
            }
        }

        try {
            val resolver = context.contentResolver
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                contentObserver!!
            )
            resolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),
                false,
                contentObserver!!
            )
            isObserverRegistered = true
            DiagnosticsLog.log(
                DiagnosticsLog.Category.REPAIR,
                "AccessibilityKeeper registered observer for $targetComponent"
            )
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "Failed to register AccessibilityKeeper observer: ${e.message}"
            )
        }

        // Periodic background poll every 40 seconds
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (true) {
                delay(40_000)
                attemptRepairIfNeeded(source = "PeriodicCheck")
            }
        }
    }

    fun stopObserving() {
        periodicJob?.cancel()
        periodicJob = null
        if (isObserverRegistered && contentObserver != null) {
            try {
                context.contentResolver.unregisterContentObserver(contentObserver!!)
            } catch (e: Exception) {
                // Ignore
            }
            isObserverRegistered = false
        }
    }

    fun stopAutoRepairByUser() {
        _isAutoRepairStopped.value = true
        DiagnosticsLog.log(
            DiagnosticsLog.Category.REPAIR,
            "Auto-repair paused by user action"
        )
    }

    fun resumeAutoRepairByUser() {
        _isAutoRepairStopped.value = false
        repairTimestamps.clear()
        attemptRepairIfNeeded("UserResume")
    }

    @Synchronized
    fun attemptRepairIfNeeded(source: String): Boolean {
        if (_isAutoRepairStopped.value) return false
        val hasSecure = checkPermission()
        val hasShizuku = ShizukuManager.hasPermission.value
        if (!hasSecure && !hasShizuku) return false

        val settings = settingsRepository.cachedSettings
        if (!settings.keepAccessibilityAlive) return false

        val resolver = context.contentResolver
        val enabledServices = Settings.Secure.getString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        val isServiceInList = enabledServices.split(":").any {
            it.equals(targetComponent, ignoreCase = true)
        }

        val accessibilityEnabled = Settings.Secure.getInt(
            resolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1

        if (isServiceInList && accessibilityEnabled) {
            // Already enabled
            return true
        }

        // Loop guard: max 5 repair attempts in 60s
        val now = System.currentTimeMillis()
        while (repairTimestamps.isNotEmpty() && now - repairTimestamps.peek() > 60_000L) {
            repairTimestamps.poll()
        }

        if (repairTimestamps.size >= MAX_REPAIRS_PER_MINUTE) {
            _isAutoRepairStopped.value = true
            DiagnosticsLog.log(
                DiagnosticsLog.Category.REPAIR,
                "Loop guard triggered (5 repairs in 60s). Pausing auto-repair."
            )
            return false
        }

        repairTimestamps.add(now)

        if (!hasSecure && hasShizuku) {
            scope.launch {
                ShizukuManager.healAccessibility(context)
            }
            _repairEventsCount.value++
            DiagnosticsLog.log(
                DiagnosticsLog.Category.REPAIR,
                "Accessibility service auto-repaired via Shizuku from [$source]"
            )
            return true
        }

        try {
            val newList = if (enabledServices.isBlank()) {
                targetComponent
            } else if (!isServiceInList) {
                "$enabledServices:$targetComponent"
            } else {
                enabledServices
            }

            val putServicesSuccess = Settings.Secure.putString(
                resolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                newList
            )

            val putEnabledSuccess = Settings.Secure.putInt(
                resolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                1
            )

            _repairEventsCount.value++
            DiagnosticsLog.log(
                DiagnosticsLog.Category.REPAIR,
                "Accessibility service auto-repaired from [$source] (services: $putServicesSuccess, flag: $putEnabledSuccess)"
            )
            return true
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "Failed to auto-repair accessibility service: ${e.message}"
            )
            return false
        }
    }

    fun getAdbCommand(): String {
        return "adb shell pm grant ${context.packageName} android.permission.WRITE_SECURE_SETTINGS"
    }
}
