package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.KothaApplication
import com.example.bridge.TextBridgeService
import com.example.bridge.TextBridgeStateHolder
import com.example.engine.DictationService
import com.example.settings.KothaSettings
import com.example.settings.SettingsRepository
import com.example.settings.VisibilityRule
import com.example.ui.theme.DisplayFont
import com.example.ui.theme.LocalKothaColors
import com.example.ui.theme.MonoFont
import com.example.util.ShizukuManager
import kotlinx.coroutines.launch

data class StationIndexItem(
    val number: String,
    val title: String,
    val subtitle: String,
    val tags: List<String>
)

val stationIndexList = listOf(
    StationIndexItem("01", "Voice", "Primary (Bangla) & secondary (English) speech language selection & recognizer settings", listOf("language", "voice", "bangla", "english", "bcp47", "engine", "google", "offline", "offensive")),
    StationIndexItem("02", "Mode", "Continuous dictation vs Pause-and-restart timing parameters", listOf("mode", "continuous", "pause", "restart", "silence", "delay", "timing")),
    StationIndexItem("03", "Insertion", "Live partials, Accessibility set-text, clipboard, and cursor following", listOf("insertion", "partial", "clipboard", "cursor", "paste", "password", "caption")),
    StationIndexItem("04", "Orb", "Floating button sizing, shapes, button & transcription position offsets, colors, and triggers", listOf("orb", "button", "size", "shape", "color", "ring", "opacity", "gesture", "snap", "keyboard", "appear", "trigger", "visibility", "offset", "position")),
    StationIndexItem("05", "Sound & Feel", "Beep suppression, haptic feedback, and audio ducking", listOf("sound", "beep", "mute", "haptic", "vibrate", "screen", "media")),
    StationIndexItem("06", "Language Tools", "Auto-punctuation, Bangla danda '।', digit conversion, and voice commands", listOf("punctuation", "danda", "digits", "numbers", "replacement", "commands", "voice")),
    StationIndexItem("07", "System", "Permission ledger, Self-healing AccessibilityKeeper, ADB command, and MIUI guide", listOf("system", "permission", "accessibility", "adb", "keeper", "repair", "xiaomi", "miui", "hyperos")),
    StationIndexItem("08", "Data & Backup", "Export & import all settings to restore on a new phone, themes, event logs", listOf("data", "backup", "restore", "transfer", "phone", "settings", "theme", "json", "export", "import", "log", "diagnostics", "privacy", "about")),
    StationIndexItem("09", "Voice Commands", "Customize trigger words for 15 voice editing & cursor commands", listOf("voice", "commands", "triggers", "editing", "cursor", "undo", "delete", "newline", "select", "send", "copy", "comma", "danda", "customize")),
    StationIndexItem("10", "Shizuku Supported Features", "Privileged ADB/Shell powers: zero-click setup, self-healing, native send, cursor navigation, background clipboard, doze whitelist, and STT recovery", listOf("shizuku", "shizoku", "adb", "shell", "privileged", "zero-click", "self-healing", "send", "cursor", "clipboard", "doze", "whitelist", "recovery", "stt"))
)

private fun startDictationOverlayService(context: Context) {
    val startIntent = Intent(context, DictationService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(startIntent)
    } else {
        context.startService(startIntent)
    }
}

@Composable
fun HomeScreen(
    settings: KothaSettings,
    settingsRepo: SettingsRepository,
    onNavigateStation: (String) -> Unit,
    onNavigateTestPad: () -> Unit
) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isOrbRunning by DictationService.isRunningState.collectAsState()
    val isShizukuRunning by ShizukuManager.isServiceRunning.collectAsState()
    val hasShizukuPermission by ShizukuManager.hasPermission.collectAsState()
    val isBridgeConnected by TextBridgeStateHolder.isConnected.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var isSettingUp by remember { mutableStateOf(false) }
    var pendingStartOrbAfterPermissions by remember { mutableStateOf(false) }

    val runtimePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val overlayGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true

        if (pendingStartOrbAfterPermissions && micGranted && overlayGranted) {
            pendingStartOrbAfterPermissions = false
            startDictationOverlayService(context)
        } else if (pendingStartOrbAfterPermissions && !overlayGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingStartOrbAfterPermissions = false
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } else if (!micGranted) {
            pendingStartOrbAfterPermissions = false
            Toast.makeText(context, "Microphone permission is required to float & dictate", Toast.LENGTH_SHORT).show()
        }
    }

    // If user granted Shizuku permission while trying to float the orb, auto-finish setup and launch
    LaunchedEffect(hasShizukuPermission) {
        if (hasShizukuPermission && pendingStartOrbAfterPermissions && !isSettingUp) {
            isSettingUp = true
            val result = ShizukuManager.applyZeroClickSetup(context)
            (context.applicationContext as? KothaApplication)?.accessibilityKeeper?.checkPermission()
            TextBridgeService.instance?.checkWindowsAndKeyboard()
            TextBridgeService.instance?.refreshFocusedNode()
            isSettingUp = false
            pendingStartOrbAfterPermissions = false

            val canDraw = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
            val hasMicNow = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (canDraw && hasMicNow) {
                settingsRepo.updateSettings { it.copy(shizukuAutoSetupEnabled = true) }
                startDictationOverlayService(context)
                DictationService.instance?.refreshOrb()
                Toast.makeText(context, "⚡ Shizuku setup complete & Orb started!", Toast.LENGTH_SHORT).show()
            } else if (result.isFailure) {
                Toast.makeText(context, "Setup partial: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredStations = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            stationIndexList
        } else {
            stationIndexList.filter { item ->
                item.title.contains(searchQuery, ignoreCase = true) ||
                        item.subtitle.contains(searchQuery, ignoreCase = true) ||
                        item.tags.any { it.contains(searchQuery, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        // Top Header with Oversized Bangla Wordmark
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "KOTHA",
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "কথা",
                        fontFamily = DisplayFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 44.sp,
                        lineHeight = 48.sp,
                        color = colors.accent
                    )
                }

                Button(
                    onClick = onNavigateTestPad,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = "Test Pad", tint = colors.accent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "TEST PAD", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                }
            }
        }


        // Full-Width Typographic Master Control to Float / Lower Orb (1-Tap Setup & Launch)
        item {
            val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
            val hasMic = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }

            val runningSubtitle = when {
                !hasOverlay -> "Overlay permission needed • Tap to grant"
                !isBridgeConnected -> "Accessibility connecting • Orb visible for tap-to-repair"
                settings.visibilityRule == VisibilityRule.KEYBOARD_VISIBLE -> "Active • Appears automatically when keyboard opens (tap to turn off)"
                settings.visibilityRule == VisibilityRule.EDITABLE_FOCUSED -> "Active • Appears when text field is focused (tap to turn off)"
                else -> "Tap to lower & dismiss floating button"
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, if (isOrbRunning) colors.signalVerdigris else colors.accent, RoundedCornerShape(2.dp))
                        .background(if (isOrbRunning) colors.surface else colors.accent)
                        .clickable(enabled = !isSettingUp) {
                            if (isOrbRunning) {
                                val stopIntent = Intent(context, DictationService::class.java).apply {
                                    action = DictationService.ACTION_HIDE_ORB
                                }
                                context.startService(stopIntent)
                                return@clickable
                            }

                            // Automatic 1-tap Shizuku setup if any permission is missing and Shizuku is running
                            val needsSetup = !hasOverlay || !hasMic || !hasNotif || !isBridgeConnected
                            if (needsSetup && isShizukuRunning) {
                                if (!hasShizukuPermission) {
                                    pendingStartOrbAfterPermissions = true
                                    ShizukuManager.requestPermission()
                                    return@clickable
                                }
                                scope.launch {
                                    isSettingUp = true
                                    ShizukuManager.applyZeroClickSetup(context)
                                    (context.applicationContext as? KothaApplication)?.accessibilityKeeper?.checkPermission()
                                    TextBridgeService.instance?.checkWindowsAndKeyboard()
                                    TextBridgeService.instance?.refreshFocusedNode()
                                    settingsRepo.updateSettings { it.copy(shizukuAutoSetupEnabled = true) }
                                    isSettingUp = false

                                    val overlayNow = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)
                                    val micNow = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (overlayNow && micNow) {
                                        startDictationOverlayService(context)
                                        DictationService.instance?.refreshOrb()
                                    } else if (!micNow) {
                                        pendingStartOrbAfterPermissions = true
                                        val toReq = buildList {
                                            add(Manifest.permission.RECORD_AUDIO)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                add(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }.toTypedArray()
                                        runtimePermLauncher.launch(toReq)
                                    } else if (!overlayNow && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                                return@clickable
                            }

                            // Fallback without Shizuku: request missing runtime permissions cleanly
                            if (!hasMic || !hasNotif) {
                                pendingStartOrbAfterPermissions = true
                                val toReq = buildList {
                                    if (!hasMic) add(Manifest.permission.RECORD_AUDIO)
                                    if (!hasNotif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                                runtimePermLauncher.launch(toReq)
                                return@clickable
                            }

                            if (!hasOverlay) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                }
                                return@clickable
                            }

                            startDictationOverlayService(context)
                        }
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = when {
                                    isSettingUp -> "SETTING UP VIA SHIZUKU…"
                                    isOrbRunning -> "ORB IS FLOATING • বাটন চালু আছে"
                                    else -> "FLOAT THE ORB • বাটন চালু করুন"
                                },
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isOrbRunning) colors.signalVerdigris else colors.background
                            )
                            Text(
                                text = if (isOrbRunning) runningSubtitle else "1-tap permission setup & floating orb across all apps",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = if (isOrbRunning) colors.textMuted else colors.background.copy(alpha = 0.85f)
                            )
                        }

                        Text(
                            text = if (isOrbRunning) "[ ON ]" else "[ OFF ]",
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isOrbRunning) colors.signalVerdigris else colors.background
                        )
                    }
                }

                // 1-Tap Shizuku Auto-Setup Strip when Shizuku is active and any permission is still incomplete
                if (isShizukuRunning && (!hasShizukuPermission || !hasOverlay || !hasMic || !hasNotif || !isBridgeConnected)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                            .background(colors.surface)
                            .clickable(enabled = !isSettingUp) {
                                if (!hasShizukuPermission) {
                                    ShizukuManager.requestPermission()
                                } else {
                                    scope.launch {
                                        isSettingUp = true
                                        val res = ShizukuManager.applyZeroClickSetup(context)
                                        (context.applicationContext as? KothaApplication)?.accessibilityKeeper?.checkPermission()
                                        TextBridgeService.instance?.checkWindowsAndKeyboard()
                                        TextBridgeService.instance?.refreshFocusedNode()
                                        DictationService.instance?.refreshOrb()
                                        isSettingUp = false
                                        if (res.isSuccess) {
                                            settingsRepo.updateSettings { it.copy(shizukuAutoSetupEnabled = true) }
                                            Toast.makeText(context, "⚡ All permissions & Accessibility enabled!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Setup partial: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (!hasShizukuPermission) {
                                    "⚡ Shizuku Ready • Tap to grant & auto-enable all permissions"
                                } else {
                                    "⚡ Complete 1-Tap Shizuku Permission Setup"
                                },
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = colors.accent,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "[ RUN ]",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        }

        // Typewriter Settings Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search stations (e.g. 'bangla', 'timing', 'adb', 'shape')…",
                        fontFamily = MonoFont,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = colors.textMuted)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.hairline,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                ),
                shape = RoundedCornerShape(2.dp)
            )
        }

        // Table of Contents / Index Rows (01 through 08)
        item {
            Text(
                text = "INDEX • স্টেশন সূচিপত্র",
                fontFamily = MonoFont,
                fontSize = 11.sp,
                color = colors.textMuted
            )
        }

        items(filteredStations) { item ->
            StationIndexCard(
                number = item.number,
                title = item.title,
                subtitle = item.subtitle,
                onClick = { onNavigateStation(item.number) }
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun StationIndexCard(
    number: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalKothaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
            .background(colors.surface)
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large section marker numeral (01-08)
        Text(
            text = number,
            fontFamily = MonoFont,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = colors.accent,
            modifier = Modifier.width(38.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontFamily = MonoFont,
                fontSize = 11.sp,
                color = colors.textMuted,
                lineHeight = 15.sp
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open",
            tint = colors.textMuted
        )
    }
}
