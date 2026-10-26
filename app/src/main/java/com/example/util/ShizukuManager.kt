package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

object ShizukuManager {

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()

    private var permissionListener: Shizuku.OnRequestPermissionResultListener? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        _isServiceRunning.value = false
        _hasPermission.value = false
    }

    init {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            // Shizuku not active
        }
        checkStatus()
    }

    fun checkStatus() {
        val running = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
        _isServiceRunning.value = running

        if (running) {
            val granted = try {
                if (Shizuku.isPreV11()) {
                    false
                } else {
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                }
            } catch (e: Throwable) {
                false
            }
            _hasPermission.value = granted
        } else {
            _hasPermission.value = false
        }
    }

    fun registerPermissionListener(onResult: (granted: Boolean) -> Unit) {
        if (permissionListener != null) return
        permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            val granted = (grantResult == PackageManager.PERMISSION_GRANTED)
            _hasPermission.value = granted
            onResult(granted)
        }
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener!!)
        } catch (e: Throwable) {
            // Shizuku not active
        }
    }

    fun unregisterPermissionListener() {
        permissionListener?.let {
            try {
                Shizuku.removeRequestPermissionResultListener(it)
            } catch (e: Throwable) {
                // Ignore
            }
            permissionListener = null
        }
    }

    fun requestPermission(requestCode: Int = 1001): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            if (Shizuku.isPreV11()) return false
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _hasPermission.value = true
                return true
            }
            Shizuku.requestPermission(requestCode)
            true
        } catch (e: Throwable) {
            false
        }
    }

    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!_hasPermission.value && !checkPermissionDirect()) {
                return@withContext Result.failure(IllegalStateException("Shizuku permission not granted"))
            }

            val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            newProcessMethod.isAccessible = true
            val process = newProcessMethod.invoke(null, arrayOf("sh", "-c", command), null, null) as Process
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(output.trim())
            } else {
                Result.failure(RuntimeException("Command failed (exit $exitCode): $error"))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun checkPermissionDirect(): Boolean {
        return try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) {
                val granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                _hasPermission.value = granted
                granted
            } else false
        } catch (e: Throwable) {
            false
        }
    }

    // 1. Zero-Click Setup (Grants all permissions & activates accessibility)
    suspend fun applyZeroClickSetup(context: Context): Result<String> {
        val pkg = context.packageName
        val script = """
            pm grant $pkg android.permission.RECORD_AUDIO
            appops set $pkg SYSTEM_ALERT_WINDOW allow
            pm grant $pkg android.permission.WRITE_SECURE_SETTINGS
            settings put secure enabled_accessibility_services $pkg/com.example.bridge.TextBridgeService
            settings put secure accessibility_enabled 1
            dumpsys deviceidle whitelist +$pkg
            cmd appops set $pkg RUN_IN_BACKGROUND allow
            cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow
        """.trimIndent()
        return executeCommand(script)
    }

    // 2. Self-Healing Accessibility Service
    suspend fun healAccessibility(context: Context): Result<String> {
        val pkg = context.packageName
        val cmd = "settings put secure enabled_accessibility_services $pkg/com.example.bridge.TextBridgeService && settings put secure accessibility_enabled 1"
        return executeCommand(cmd)
    }

    // 4. Hands-Free Send Command
    suspend fun sendEnterKey(): Result<String> {
        return executeCommand("input keyevent 66")
    }

    // 5. High-Precision Cursor Navigation & Editing
    suspend fun deletePreviousWord(): Result<String> {
        return executeCommand("input keyevent 67")
    }

    suspend fun moveToHome(): Result<String> {
        return executeCommand("input keyevent 122")
    }

    suspend fun moveToEnd(): Result<String> {
        return executeCommand("input keyevent 123")
    }

    // 7. Background Clipboard Bypass
    suspend fun copyToClipboardPrivileged(text: String): Result<String> {
        val escaped = text.replace("'", "'\\''")
        return executeCommand("cmd clipboard set '$escaped' || echo '$escaped'")
    }

    // 10. Battery Saver & Doze Whitelist
    suspend fun whitelistBattery(context: Context): Result<String> {
        val pkg = context.packageName
        val script = """
            dumpsys deviceidle whitelist +$pkg
            cmd appops set $pkg RUN_IN_BACKGROUND allow
            cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow
        """.trimIndent()
        return executeCommand(script)
    }

    // 13. Speech Recognition Engine Diagnostic & Recovery
    suspend fun recoverSttEngine(): Result<String> {
        val script = """
            am force-stop com.google.android.googlequicksearchbox
            am force-stop com.google.android.tts
        """.trimIndent()
        return executeCommand(script)
    }
}
