package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.settings.KothaSettings
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.StationScreen
import com.example.ui.screens.TestPadScreen
import com.example.ui.screens.stationIndexList
import com.example.ui.theme.KothaTheme

import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.bridge.TextBridgeService
import com.example.engine.DictationService
import com.example.util.ShizukuManager
import kotlinx.coroutines.launch

sealed class Screen {
    object Home : Screen()
    data class Station(val number: String) : Screen()
    object TestPad : Screen()
    object Onboarding : Screen()
}

class MainActivity : ComponentActivity() {

    private var isApplyingAutoSetup = false

    private fun runAutoSetupIfNeeded(
        app: KothaApplication,
        settingsRepo: com.example.settings.SettingsRepository,
        showToast: Boolean
    ) {
        if (isApplyingAutoSetup) return
        val currentSettings = settingsRepo.cachedSettings
        val needsSetup = !currentSettings.shizukuAutoSetupEnabled ||
                !ShizukuManager.areAllCorePermissionsGranted(this)
        if (!needsSetup) return

        isApplyingAutoSetup = true
        lifecycleScope.launch {
            try {
                val result = ShizukuManager.applyZeroClickSetup(this@MainActivity)
                app.accessibilityKeeper.checkPermission()
                TextBridgeService.instance?.checkWindowsAndKeyboard()
                TextBridgeService.instance?.refreshFocusedNode()
                DictationService.instance?.refreshOrb()

                if (result.isSuccess) {
                    settingsRepo.updateSettings { s ->
                        s.copy(
                            shizukuAutoSetupEnabled = true,
                            shizukuSelfHealingEnabled = true,
                            shizukuHandsFreeSendEnabled = true,
                            shizukuCursorNavEnabled = true,
                            shizukuClipboardBypassEnabled = true,
                            shizukuDozeWhitelistEnabled = true,
                            shizukuSttRecoveryEnabled = true
                        )
                    }
                    if (showToast) {
                        Toast.makeText(
                            this@MainActivity,
                            "⚡ Shizuku Connected & Permitted! All permissions & background features activated.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (showToast) {
                    Toast.makeText(
                        this@MainActivity,
                        "⚡ Shizuku setup partial: ${result.exceptionOrNull()?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                isApplyingAutoSetup = false
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as KothaApplication
        val settingsRepo = app.settingsRepository

        // Initial Shizuku status check
        ShizukuManager.checkStatus()

        // Observe Shizuku permission state (triggers whether pre-granted in Shizuku app or newly granted)
        lifecycleScope.launch {
            ShizukuManager.hasPermission.collect { granted ->
                if (granted) {
                    runAutoSetupIfNeeded(app, settingsRepo, showToast = true)
                }
            }
        }

        // Register Shizuku permission request result listener
        ShizukuManager.registerPermissionListener { granted ->
            if (granted) {
                runAutoSetupIfNeeded(app, settingsRepo, showToast = true)
            }
        }

        setContent {
            val settings by settingsRepo.settingsFlow.collectAsState()
            var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

            val isShizukuRunning by ShizukuManager.isServiceRunning.collectAsState()
            val hasShizukuPermission by ShizukuManager.hasPermission.collectAsState()
            var showShizukuPrompt by remember { mutableStateOf(false) }

            // Trigger prompt on first detection if Shizuku is active but not yet granted
            androidx.compose.runtime.LaunchedEffect(isShizukuRunning, hasShizukuPermission) {
                if (isShizukuRunning && !hasShizukuPermission) {
                    showShizukuPrompt = true
                }
            }

            KothaTheme(
                appTheme = settings.appTheme,
                accentInk = settings.accentInk
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (val screen = currentScreen) {
                            is Screen.Home -> {
                                HomeScreen(
                                    settings = settings,
                                    settingsRepo = settingsRepo,
                                    onNavigateStation = { num ->
                                        currentScreen = Screen.Station(num)
                                    },
                                    onNavigateTestPad = {
                                        currentScreen = Screen.TestPad
                                    }
                                )
                            }

                            is Screen.Station -> {
                                BackHandler {
                                    currentScreen = Screen.Home
                                }
                                val stationMeta = stationIndexList.find { it.number == screen.number }
                                StationScreen(
                                    stationNumber = screen.number,
                                    stationTitle = stationMeta?.title ?: "Station",
                                    stationSubtitle = stationMeta?.subtitle ?: "",
                                    settings = settings,
                                    settingsRepo = settingsRepo,
                                    onBack = { currentScreen = Screen.Home }
                                )
                            }

                            is Screen.TestPad -> {
                                BackHandler {
                                    currentScreen = Screen.Home
                                }
                                TestPadScreen(
                                    settings = settings,
                                    onBack = { currentScreen = Screen.Home }
                                )
                            }

                            is Screen.Onboarding -> {
                                BackHandler {
                                    currentScreen = Screen.Home
                                }
                                OnboardingScreen(
                                    onFinished = { currentScreen = Screen.Home }
                                )
                            }
                        }

                        // Shizuku Detection Prompt Dialog
                        if (showShizukuPrompt) {
                            androidx.compose.material3.AlertDialog(
                                onDismissRequest = { showShizukuPrompt = false },
                                title = {
                                    androidx.compose.material3.Text(
                                        text = "⚡ Shizuku Detected",
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                    )
                                },
                                text = {
                                    androidx.compose.material3.Text(
                                        text = "Shizuku is running on your phone.\n\nGranting permission allows Kotha to automatically enable Zero-Click setup (microphone, overlay, notifications, accessibility bridge), background self-healing, hands-free send, and battery exemptions."
                                    )
                                },
                                confirmButton = {
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            showShizukuPrompt = false
                                            ShizukuManager.requestPermission()
                                        }
                                    ) {
                                        androidx.compose.material3.Text("Grant Shizuku Permission")
                                    }
                                },
                                dismissButton = {
                                    androidx.compose.material3.TextButton(
                                        onClick = { showShizukuPrompt = false }
                                    ) {
                                        androidx.compose.material3.Text("Later")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ShizukuManager.checkStatus()
        (application as KothaApplication).accessibilityKeeper.checkPermission()
        TextBridgeService.instance?.checkWindowsAndKeyboard()
        TextBridgeService.instance?.refreshFocusedNode()
        DictationService.instance?.refreshOrb()
    }

    override fun onDestroy() {
        super.onDestroy()
        ShizukuManager.unregisterPermissionListener()
    }
}
