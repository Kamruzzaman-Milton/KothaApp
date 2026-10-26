package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.DictationService
import com.example.settings.KothaSettings
import com.example.settings.SettingsRepository
import com.example.ui.components.OrbPreviewView
import com.example.ui.theme.DisplayFont
import com.example.ui.theme.LocalKothaColors
import com.example.ui.theme.MonoFont

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
    StationIndexItem("10", "Shizoku Supported Features", "Privileged ADB/Shell powers: zero-click setup, self-healing, native send, cursor navigation, background clipboard, doze whitelist, and STT recovery", listOf("shizoku", "shizuku", "adb", "shell", "privileged", "zero-click", "self-healing", "send", "cursor", "clipboard", "doze", "whitelist", "recovery", "stt"))
)

@Composable
fun HomeScreen(
    settings: KothaSettings,
    settingsRepo: SettingsRepository,
    onNavigateStation: (String) -> Unit,
    onNavigateTestPad: () -> Unit
) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current
    var isOrbRunning by remember { mutableStateOf(DictationService.isRunning()) }
    var searchQuery by remember { mutableStateOf("") }

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

        // Stage: Live interactive Orb Preview
        item {
            OrbPreviewView(settings = settings)
        }

        // Full-Width Typographic Master Control to Float / Lower Orb
        item {
            val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, if (isOrbRunning) colors.signalVerdigris else colors.accent, RoundedCornerShape(2.dp))
                    .background(if (isOrbRunning) colors.surface else colors.accent)
                    .clickable {
                        if (!hasOverlay) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                            return@clickable
                        }

                        if (isOrbRunning) {
                            val stopIntent = Intent(context, DictationService::class.java).apply {
                                action = DictationService.ACTION_HIDE_ORB
                            }
                            context.startService(stopIntent)
                            isOrbRunning = false
                        } else {
                            val startIntent = Intent(context, DictationService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(startIntent)
                            } else {
                                context.startService(startIntent)
                            }
                            isOrbRunning = true
                        }
                    }
                    .padding(vertical = 16.dp, horizontal = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isOrbRunning) "ORB IS FLOATING • বাটন চালু আছে" else "FLOAT THE ORB • বাটন চালু করুন",
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isOrbRunning) colors.signalVerdigris else colors.background
                        )
                        Text(
                            text = if (isOrbRunning) "Tap to lower & dismiss floating button" else "Hovers over your keyboard across all apps",
                            fontFamily = MonoFont,
                            fontSize = 11.sp,
                            color = if (isOrbRunning) colors.textMuted else colors.background.copy(alpha = 0.8f)
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
