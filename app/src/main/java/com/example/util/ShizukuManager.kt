package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
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

    // =========================================================================
    // Safe, Non-Destructive Command Builders
    // =========================================================================

    fun getTargetAccessibilityComponent(context: Context): String {
        return "${context.packageName}/com.example.bridge.TextBridgeService"
    }

    /**
     * Safely appends Kotha's Accessibility Service to the colon-separated list
     * of enabled services without wiping out or disabling other apps' accessibility services.
     */
    fun getSafeAccessibilityEnableCommand(context: Context): String {
        val target = getTargetAccessibilityComponent(context)
        return """
curr=${'$'}(settings get secure enabled_accessibility_services)
case ":${'$'}curr:" in
  *":$target:"*) ;;
  *) if [ -z "${'$'}curr" ] || [ "${'$'}curr" = "null" ]; then
       settings put secure enabled_accessibility_services "$target"
     else
       settings put secure enabled_accessibility_services "${'$'}curr:$target"
     fi ;;
esac
settings put secure accessibility_enabled 1
        """.trimIndent()
    }

    /**
     * Safely removes only Kotha from the enabled accessibility services list,
     * keeping all other apps' accessibility services intact.
     */
    fun getSafeAccessibilityDisableCommand(context: Context): String {
        val target = getTargetAccessibilityComponent(context)
        val existing = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        } catch (e: Exception) { "" }
        val remaining = existing.split(":")
            .filter { it.isNotBlank() && !it.equals(target, ignoreCase = true) }
            .joinToString(":")
        return if (remaining.isBlank()) {
            "settings put secure enabled_accessibility_services \"\""
        } else {
            "settings put secure enabled_accessibility_services \"$remaining\""
        }
    }

    // 01. Zero-Click Setup
    fun getZeroClickSetupScript(context: Context): String {
        val pkg = context.packageName
        val enableAcc = getSafeAccessibilityEnableCommand(context)
        return """
pm grant $pkg android.permission.RECORD_AUDIO
appops set $pkg SYSTEM_ALERT_WINDOW allow
pm grant $pkg android.permission.WRITE_SECURE_SETTINGS
$enableAcc
dumpsys deviceidle whitelist +$pkg
cmd appops set $pkg RUN_IN_BACKGROUND allow
cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow
cmd appops set $pkg READ_CLIPBOARD allow
        """.trimIndent()
    }

    // 02. Self-Healing Accessibility Service
    fun getSelfHealingScript(context: Context): String {
        return getSafeAccessibilityEnableCommand(context)
    }

    // 04. Hands-Free Send Command
    fun getHandsFreeSendScript(): String {
        return "input keyevent 66"
    }

    // 05. High-Precision Cursor Navigation & Editing Macros
    fun getCursorMacroScript(): String {
        return """
# Delete previous character / word:
input keyevent 67
# Move cursor to start of line (Home):
input keyevent 122
# Move cursor to end of line (End):
input keyevent 123
        """.trimIndent()
    }

    // 07. Background Clipboard Bypass
    fun getClipboardBypassScript(context: Context): String {
        val pkg = context.packageName
        return "cmd appops set $pkg READ_CLIPBOARD allow"
    }

    // 10. Battery Saver & Doze Whitelist
    fun getDozeWhitelistScript(context: Context): String {
        val pkg = context.packageName
        return """
dumpsys deviceidle whitelist +$pkg
cmd appops set $pkg RUN_IN_BACKGROUND allow
cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow
        """.trimIndent()
    }

    // 13. Speech Recognition Engine Diagnostic & Recovery
    fun getSttRecoveryScript(): String {
        return """
am force-stop com.google.android.googlequicksearchbox
am force-stop com.google.android.tts
        """.trimIndent()
    }

    // =========================================================================
    // Execution Handlers
    // =========================================================================

    suspend fun applyZeroClickSetup(context: Context): Result<String> {
        return executeCommand(getZeroClickSetupScript(context))
    }

    suspend fun healAccessibility(context: Context): Result<String> {
        return executeCommand(getSelfHealingScript(context))
    }

    suspend fun disableAccessibility(context: Context): Result<String> {
        return executeCommand(getSafeAccessibilityDisableCommand(context))
    }

    suspend fun sendEnterKey(): Result<String> {
        return executeCommand(getHandsFreeSendScript())
    }

    suspend fun deletePreviousWord(): Result<String> {
        return executeCommand("input keyevent 67")
    }

    suspend fun moveToHome(): Result<String> {
        return executeCommand("input keyevent 122")
    }

    suspend fun moveToEnd(): Result<String> {
        return executeCommand("input keyevent 123")
    }

    suspend fun enableClipboardBypass(context: Context): Result<String> {
        return executeCommand(getClipboardBypassScript(context))
    }

    suspend fun whitelistBattery(context: Context): Result<String> {
        return executeCommand(getDozeWhitelistScript(context))
    }

    suspend fun recoverSttEngine(): Result<String> {
        return executeCommand(getSttRecoveryScript())
    }
}
