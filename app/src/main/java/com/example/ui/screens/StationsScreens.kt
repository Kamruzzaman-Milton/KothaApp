package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.KothaApplication
import com.example.bridge.TextBridgeService
import com.example.bridge.TextBridgeStateHolder
import com.example.engine.LanguageData
import com.example.engine.SpeechEngineManager
import com.example.settings.AccentInk
import com.example.settings.AppTheme
import com.example.settings.CaptionBubblePosition
import com.example.settings.CaptionBubbleSize
import com.example.settings.CaptionBubbleStyle
import com.example.settings.DictationMode
import com.example.settings.GestureAction
import com.example.settings.InsertionMethod
import com.example.settings.KothaSettings
import com.example.settings.OrbShape
import com.example.settings.RingStyle
import com.example.settings.SettingsRepository
import com.example.settings.StreamingStyle
import com.example.settings.UndoBadgeLabelMode
import com.example.settings.UndoBadgeSize
import com.example.settings.UndoBadgeStyle
import com.example.settings.VisibilityRule
import com.example.ui.components.InkBracketToggle
import com.example.ui.components.InkColorPicker
import com.example.ui.components.InkRulerSlider
import com.example.ui.components.InkSegmentedControl
import com.example.ui.components.InkStepper
import com.example.ui.theme.DisplayFont
import com.example.ui.theme.LocalKothaColors
import com.example.ui.theme.MonoFont
import com.example.util.DiagnosticsLog
import com.example.util.LanguageTools

@Composable
fun StationScreen(
    stationNumber: String,
    stationTitle: String,
    stationSubtitle: String,
    settings: KothaSettings,
    settingsRepo: SettingsRepository,
    onBack: () -> Unit
) {
    val colors = LocalKothaColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Station Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "$stationNumber • $stationTitle",
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = stationSubtitle,
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairline)
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (stationNumber) {
                "01" -> item { Station01Voice(settings, settingsRepo) }
                "02" -> item { Station02Mode(settings, settingsRepo) }
                "03" -> item { Station03Insertion(settings, settingsRepo) }
                "04" -> item { Station04Orb(settings, settingsRepo) }
                "05" -> item { Station05SoundFeel(settings, settingsRepo) }
                "06" -> item { Station06LanguageTools(settings, settingsRepo) }
                "07" -> item { Station07System(settings, settingsRepo) }
                "08" -> item { Station08Data(settings, settingsRepo) }
                "09" -> item { Station09VoiceCommands(settings, settingsRepo) }
                "10" -> item { Station10Shizoku(settings, settingsRepo) }
            }
            item { Spacer(modifier = Modifier.height(48.dp)) }
        }
    }
}

@Composable
private fun Station01Voice(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Quick Active Language Toggle Card
        SettingCard(
            title = "Active Language • সক্রিয় উচ্চারণ ভাষা",
            description = "বর্তমানে কোন ভাষায় ভয়েস ইনপুট নেওয়া হচ্ছে। ডিক্টেশন চলাকালীন ফ্লোটিং বাটনের উপরের ছোট বাটনটি ট্যাপ করেও সরাসরি অদলবদল করতে পারবেন।"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isSecondary = settings.isSecondaryLanguageActive
                // Primary Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.5.dp, if (!isSecondary) colors.accent else colors.hairline, RoundedCornerShape(2.dp))
                        .background(if (!isSecondary) colors.accent.copy(alpha = 0.15f) else colors.surface)
                        .clickable { repo.updateSettings { it.copy(isSecondaryLanguageActive = false) } }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (!isSecondary) "● ACTIVE PRIMARY" else "PRIMARY",
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (!isSecondary) colors.accent else colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "বাংলা (Bengali)",
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = settings.primaryLanguage,
                            fontFamily = MonoFont,
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }

                // Secondary Option
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.5.dp, if (isSecondary) colors.accent else colors.hairline, RoundedCornerShape(2.dp))
                        .background(if (isSecondary) colors.accent.copy(alpha = 0.15f) else colors.surface)
                        .clickable { repo.updateSettings { it.copy(isSecondaryLanguageActive = true) } }
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isSecondary) "● ACTIVE SECONDARY" else "SECONDARY",
                            fontFamily = MonoFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = if (isSecondary) colors.accent else colors.textMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "English",
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = settings.secondaryLanguage,
                            fontFamily = MonoFont,
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }

        // Section 1: Primary Speech Language
        SettingCard(
            title = "1. Primary Speech Language • মূল উচ্চারণ ভাষা",
            description = "আপনার প্রধান কথ্য ভাষা নির্বাচন করুন (ডিফল্ট: বাংলা)।"
        ) {
            val languages = LanguageData.curatedLanguages.take(12)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                languages.forEach { lang ->
                    val isSelected = settings.primaryLanguage.equals(lang.bcp47, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp))
                            .background(if (isSelected) colors.accent.copy(alpha = 0.1f) else colors.surface)
                            .clickable { repo.updateSettings { it.copy(primaryLanguage = lang.bcp47) } }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = lang.displayName, fontFamily = DisplayFont, fontSize = 14.sp, color = colors.textPrimary)
                            Text(text = lang.nativeName, fontFamily = MonoFont, fontSize = 11.sp, color = colors.textMuted)
                        }
                        Text(text = lang.bcp47, fontFamily = MonoFont, fontSize = 12.sp, color = if (isSelected) colors.accent else colors.textMuted)
                    }
                }
            }
        }

        // Section 2: Secondary Speech Language
        SettingCard(
            title = "2. Secondary Speech Language • দ্বিতীয় উচ্চারণ ভাষা",
            description = "আপনার দ্বিতীয় কথ্য ভাষা নির্বাচন করুন (ডিফল্ট: English)। দ্রুত অদলবদল করার জন্য ইংরেজি ভ্যারিয়েন্টগুলো শীর্ষে পিন করা রয়েছে।"
        ) {
            val secondaryLanguages = remember {
                val englishVariants = LanguageData.curatedLanguages.filter { it.bcp47.startsWith("en") }
                val others = LanguageData.curatedLanguages.filter { !it.bcp47.startsWith("en") }
                englishVariants + others.take(8)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                secondaryLanguages.forEach { lang ->
                    val isSelected = settings.secondaryLanguage.equals(lang.bcp47, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp))
                            .background(if (isSelected) colors.accent.copy(alpha = 0.1f) else colors.surface)
                            .clickable { repo.updateSettings { it.copy(secondaryLanguage = lang.bcp47) } }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = lang.displayName, fontFamily = DisplayFont, fontSize = 14.sp, color = colors.textPrimary)
                            Text(text = lang.nativeName, fontFamily = MonoFont, fontSize = 11.sp, color = colors.textMuted)
                        }
                        Text(text = lang.bcp47, fontFamily = MonoFont, fontSize = 12.sp, color = if (isSelected) colors.accent else colors.textMuted)
                    }
                }
            }
        }

        // Section 3: Custom BCP-47 Tag
        SettingCard(
            title = "3. Custom BCP-47 Tag • কাস্টম ভাষা কোড",
            description = "অন্য কোনো নির্দিষ্ট আঞ্চলিক ভাষা কোড ম্যানুয়ালি দিতে চাইলে এখানে লিখুন।"
        ) {
            var textInput by remember { mutableStateOf(settings.customBcp47) }
            OutlinedTextField(
                value = textInput,
                onValueChange = {
                    textInput = it
                    repo.updateSettings { s -> s.copy(customBcp47 = it) }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Leave empty to use primary language (e.g. bn-BD)", fontFamily = MonoFont, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.hairline,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                shape = RoundedCornerShape(2.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFont, fontSize = 11.sp)
            )
        }

        // Section: Engine Options & Toggles
        SettingToggleRow(
            title = "Prefer Offline Recognition (অফলাইন স্বীকৃতি)",
            description = "ফোনে গুগল অফলাইন স্পিচ প্যাক ডাউনলোড করা থাকলে অগ্রাধিকার দেবে।",
            checked = settings.preferOffline,
            onCheckedChange = { repo.updateSettings { s -> s.copy(preferOffline = it) } }
        )

        SettingToggleRow(
            title = "On-Device Recognizer (API 31+)",
            description = "অ্যান্ড্রয়েড ১২+ ডিভাইসে অফিশিয়াল অন-ডিভাইস স্পিচ রেকগনিশন এপিআই ব্যবহার করে।",
            checked = settings.useOnDevice,
            onCheckedChange = { repo.updateSettings { s -> s.copy(useOnDevice = it) } }
        )

        SettingToggleRow(
            title = "Mask Offensive Words (আপত্তিকর শব্দ ফিল্টার)",
            description = "গুগল স্পিচ থেকে আপত্তিকর শব্দের জায়গায় অ্যাস্ট্যারিস্ক (*) চিহ্ন বসায় (সাধারণত বন্ধ রাখা ভালো)।",
            checked = settings.maskOffensiveWords,
            onCheckedChange = { repo.updateSettings { s -> s.copy(maskOffensiveWords = it) } }
        )
    }
}

@Composable
private fun Station02Mode(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Detailed explanation card for Dictation Mode
        SettingCard(
            title = "1. Dictation Mode • ডিক্টেশন পরিচালনার মোড",
            description = "আপনার কথা বলার ধরন ও পরিবেশ অনুযায়ী মাইক্রোফোন কীভাবে আচরণ করবে তা বেছে নিন।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InkSegmentedControl(
                    items = listOf(DictationMode.CONTINUOUS, DictationMode.PAUSE_AND_RESTART),
                    selectedItem = settings.dictationMode,
                    onItemSelected = { repo.updateSettings { s -> s.copy(dictationMode = it) } },
                    labelProvider = { if (it == DictationMode.CONTINUOUS) "Mode A: Continuous" else "Mode B: Pause & Restart" }
                )

                // Deep practical explanation of currently chosen mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
                        .padding(14.dp)
                ) {
                    if (settings.dictationMode == DictationMode.CONTINUOUS) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "MODE A: CONTINUOUS DICTATION (একটানা ডিক্টেশন মোড - সবচেয়ে জনপ্রিয় ও প্রস্তাবিত)",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "• কী করে (What it actually does):\n" +
                                        "মাইক্রোফোন অবিরাম সক্রিয় থাকে। আপনি কথা বলতে বলতে কোনো বাক্যের মাঝে চিন্তা করার জন্য থামলে বা দম নিলেও রেকর্ডিং বন্ধ হয় না। প্রতিটি বাক্য শেষ হওয়ার সাথে সাথে ব্যাকগ্রাউন্ডে তাৎক্ষণিক (১৫০ মিলি.সেকেন্ডের মধ্যে) পরবর্তী বাক্য শোনার জন্য লুপ চালু হয়। আপনাকে বারবার স্ক্রিনের বাটনে হাত দিতে হয় না।\n\n" +
                                        "• কখন ব্যবহার করবেন (When to use this):\n" +
                                        "১. লম্বা ফেসবুক পোস্ট, ইমেইল, স্টোরি বা প্রবন্ধ লেখার সময়।\n" +
                                        "২. হোয়াটসঅ্যাপ বা মেসেঞ্জারে লম্বা বার্তা একটানা মুখে বলে লেখার সময়।\n" +
                                        "৩. সম্পূর্ণ হ্যান্ডস-ফ্রি টাইপিংয়ের জন্য, যেখানে আপনি একনাগাড়ে কথা বলে যেতে চান।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = colors.textPrimary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "MODE B: PAUSE & RESTART (বিরতি ও পুনরায় চালু মোড)",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.signalOchre
                            )
                            Text(
                                text = "• কী করে (What it actually does):\n" +
                                        "আপনি একটি সম্পূর্ণ বাক্য বলে থামার পর বাটনটি চারদিকে একটি কাউন্টডাউন রিং প্রদর্শন করে এবং নির্দিষ্ট সময় (যেমন ৪০০ মিলি.সেকেন্ড) সাময়িক বিরতি নেয়। আপনি আবার নতুন কথা শুরু করলে রিংটি আবার সক্রিয় হয়।\n\n" +
                                        "• কখন ব্যবহার করবেন (When to use this):\n" +
                                        "১. ছোট মেসেজ বা এক লাইনের চ্যাট রিপ্লাই দেওয়ার সময়।\n" +
                                        "২. কোলাহলপূর্ণ পরিবেশ, অফিস বা গাড়িতে—যেখানে আশেপাশের মানুষ কথা বলছে এবং আপনি চান না তাদের কথা আপনার টেক্সটে ঢুকে পড়ুক।\n" +
                                        "৩. আপনি যদি প্রতিটি বাক্যের মাঝে ভেবেচিন্তে কথা বলতে পছন্দ করেন।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        if (settings.dictationMode == DictationMode.PAUSE_AND_RESTART) {
            SettingCard(
                title = "2. Waiting Restart Delay • পুনরায় চালুর বিরতি",
                description = "একটি বাক্য শেষ হওয়ার পর বাটনটি কত মিলি.সেকেন্ড বিরতি নিয়ে পরবর্তী বাক্যের জন্য প্রস্তুত হবে।\n\n" +
                        "• ব্যবহারবিধি (When to adjust): ডিফল্ট ৪০০ms অধিকাংশের জন্য উপযুক্ত। আপনি যদি দ্রুত কথা বলেন তবে ২০০ms রাখুন। যদি ভেবেচিন্তে সময় নিয়ে কথা বলতে চান তবে ৬০০-৮০০ms রাখুন।"
            ) {
                InkRulerSlider(
                    value = settings.restartDelayMs.toFloat(),
                    onValueChange = { repo.updateSettings { s -> s.copy(restartDelayMs = it.toLong()) } },
                    valueRange = 100f..1500f,
                    unitLabel = "ms"
                )
            }
        } else {
            SettingCard(
                title = "2. Seamless Restart Interval • নিরবচ্ছিন্ন লুপের বিরতি",
                description = "একটানা মোডে একটি বাক্য টাইপ হওয়ার পর পরবর্তী বাক্য শোনার লুপ কত দ্রুত পুনরায় শুরু হবে।\n\n" +
                        "• ব্যবহারবিধি (When to adjust): ১০০ms থেকে ২০০ms রাখা সবচেয়ে উত্তম। এটি রাখলে কথা বলার মাঝে কোনো বাধা অনুভূত হয় না এবং বাক্যগুলো মসৃণভাবে পরপর টাইপ হতে থাকে।"
            ) {
                InkRulerSlider(
                    value = settings.seamlessRestartMs.toFloat(),
                    onValueChange = { repo.updateSettings { s -> s.copy(seamlessRestartMs = it.toLong()) } },
                    valueRange = 50f..300f,
                    unitLabel = "ms"
                )
            }
        }

        SettingCard(
            title = "3. Silence Complete Length • নীরবতা শনাক্তকরণ সময়কাল (অ্যাপ-নিয়ন্ত্রিত)",
            description = "কথা বলার মাঝে বিরতি নিলে অ্যাপ নিজে এই নির্ধারিত সময় পর্যন্ত অপেক্ষা করবে—ইঞ্জিনের অকাল বিরতি হলেও অ্যাপ তাৎক্ষণিকভাবে মাইক্রোফোন চালু রেখে আপনার চিন্তা করার পূর্ণ সময় নিশ্চিত করবে।\n\n" +
                    "This extends speech recognizer tolerances so thinking pauses are never prematurely cut short by Google Speech Services.\n\n" +
                    "• ১০০০–১৮০০ ms (১–১.৮ সেকেন্ড): দ্রুত একটানা সাবলীল কথা বলার জন্য আদর্শ।\n" +
                    "• ২০০০–৪০০০ ms (২–৪ সেকেন্ড): কথা বলার মাঝে চিন্তা করার দীর্ঘ বিরতি নেওয়ার জন্য উপযুক্ত।"
        ) {
            InkRulerSlider(
                value = settings.silenceCompleteMs.toFloat(),
                onValueChange = {
                    val newVal = it.toLong()
                    repo.updateSettings { s -> s.copy(silenceCompleteMs = newVal, silencePossiblyCompleteMs = newVal) }
                },
                valueRange = 800f..8000f,
                unitLabel = "ms"
            )
        }

        SettingCard(
            title = "4. Idle Auto-Stop Timer • অব্যবহৃত অবস্থায় স্বয়ংক্রিয় বন্ধ",
            description = "মাইক্রোফোন চালু রেখে কথা বলা ভুলে গেলে কত সেকেন্ড পর স্বয়ংক্রিয়ভাবে মাইক্রোফোন বন্ধ হয়ে ব্যাটারি বাঁচাবে।\n\n" +
                    "• ব্যবহারবিধি: ৬০ সেকেন্ড বা ১২০ সেকেন্ড রাখা উত্তম। ০ সিলেক্ট করলে এটি নিজে থেকে কখনোই বন্ধ হবে না।"
        ) {
            InkRulerSlider(
                value = settings.idleAutoStopSec.toFloat(),
                onValueChange = { repo.updateSettings { s -> s.copy(idleAutoStopSec = it.toInt()) } },
                valueRange = 0f..300f,
                unitLabel = "sec"
            )
        }

        SettingCard(
            title = "5. Max Consecutive Failures • ত্রুটি সহনশীলতা",
            description = "ইন্টারনেট সাময়িক দুর্বল হলে বা কথা স্পষ্ট না হলে একটানা কতবার পুনরায় চেষ্টা করার পর বাটনটি লাল রঙের ত্রুটি দেখাবে। ডিফল্ট ৪ বার রাখা সবচেয়ে নিরাপদ।"
        ) {
            InkStepper(
                value = settings.maxConsecutiveFailures,
                onValueChange = { repo.updateSettings { s -> s.copy(maxConsecutiveFailures = it) } },
                range = 1..10,
                label = "retries"
            )
        }
    }
}

@Composable
private fun Station03Insertion(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Detailed explanation card for Streaming Style
        SettingCard(
            title = "1. Streaming Style • লেখা প্রবেশের ধরন",
            description = "কথা চলাকালীন টেক্সট বক্সে কীভাবে লেখা যুক্ত হবে তা নির্ধারণ করুন।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InkSegmentedControl(
                    items = listOf(StreamingStyle.LIVE, StreamingStyle.ON_SENTENCE_END),
                    selectedItem = settings.streamingStyle,
                    onItemSelected = { repo.updateSettings { s -> s.copy(streamingStyle = it) } },
                    labelProvider = { if (it == StreamingStyle.LIVE) "Live Partial Words" else "On Sentence End" }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
                        .padding(14.dp)
                ) {
                    if (settings.streamingStyle == StreamingStyle.LIVE) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "LIVE STREAM (কথা বলার সাথে সাথে সরাসরি লাইভ টাইপিং - প্রস্তাবিত)",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "• কী করে (What it actually does):\n" +
                                        "আপনি মুখ দিয়ে উচ্চারণ করার সাথে সাথেই প্রতিটা শব্দ রিয়েল-টাইমে আপনার যেকোনো অ্যাপের টেক্সট বক্সে দেখা দেয়। বাক্য শেষ হলে অটোমেটিক দাড়ি বা কমা বসে যায়।\n\n" +
                                        "• কখন ব্যবহার করবেন (When to use this):\n" +
                                        "স্বাভাবিক মেসেজিং, চ্যাটিং, ব্রাউজারে সার্চ করার সময় এটি ব্যবহার করুন। আপনি কী বলছেন তা গুগল সঠিকভাবে শুনছে কিনা তা তাৎক্ষণিক স্ক্রিনে দেখতে পাওয়ার দারুণ আনন্দ দেয়।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = colors.textPrimary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "ON SENTENCE END (বাক্য শেষ হলে একবারে পরিচ্ছন্ন প্রবেশ)",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.signalVerdigris
                            )
                            Text(
                                text = "• কী করে (What it actually does):\n" +
                                        "কথা চলাকালীন টেক্সট বক্সে কোনো কাঁচা খসড়া শব্দ আসে না। যখন আপনি কথা বলা শেষ করবেন এবং নীরব হবেন, তখন সম্পূর্ণ নির্ভুল, ব্যাকরণগতভাবে সংশোধিত ও যতিচিহ্নযুক্ত বাক্যটি একবারে প্রবেশ করবে।\n\n" +
                                        "• কখন ব্যবহার করবেন (When to use this):\n" +
                                        "কিছু অ্যাপে (যেমন কিছু ব্যাংকিং বা কাস্টম ওয়েবভিউ ফর্ম) দ্রুত লাইভ শব্দ আসার কারণে কার্সার নড়ে যেতে পারে বা টেক্সট বাফার কাঁপতে পারে। সেইসব ক্ষেত্রে এই অপশনটি ব্যবহার করলে পরিচ্ছন্ন ও নিখুঁত লেখা পাওয়া যায়।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                lineHeight = 17.sp,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Detailed explanation card for Insertion Method
        SettingCard(
            title = "2. Insertion Method • লেখা বসানোর প্রযুক্তিগত পদ্ধতি",
            description = "অ্যান্ড্রয়েডের অন্য অ্যাপগুলোতে 'কথা' কীভাবে টেক্সট প্রবেশ করাবে সেই মেকানিজম।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InkSegmentedControl(
                    items = listOf(InsertionMethod.AUTO, InsertionMethod.ACCESSIBILITY_SET_TEXT, InsertionMethod.CLIPBOARD_PASTE),
                    selectedItem = settings.insertionMethod,
                    onItemSelected = { repo.updateSettings { s -> s.copy(insertionMethod = it) } },
                    labelProvider = {
                        when (it) {
                            InsertionMethod.AUTO -> "Auto Fallback"
                            InsertionMethod.ACCESSIBILITY_SET_TEXT -> "Accessibility Direct"
                            InsertionMethod.CLIPBOARD_PASTE -> "Clipboard Paste"
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.background)
                        .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
                        .padding(14.dp)
                ) {
                    when (settings.insertionMethod) {
                        InsertionMethod.AUTO -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "AUTO FALLBACK (স্বয়ংক্রিয় সমন্বয় - সবচেয়ে আধুনিক ও নিরাপদ)",
                                    fontFamily = MonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.accent
                                )
                                Text(
                                    text = "• কী করে (What it actually does):\n" +
                                            "প্রথমে এটি পরিচ্ছন্নভাবে অ্যান্ড্রয়েড অ্যাক্সেসিবিলিটি দিয়ে লেখা বসায়। যদি কোনো বিশেষ অ্যাপ অ্যাক্সেসিবিলিটি ব্লক করে, তবে 'কথা' নিজে থেকেই বিকল্প ক্লিপবোর্ড পেস্ট পদ্ধতিতে লেখাটি ঠিক জায়গায় বসিয়ে দেয়।\n\n" +
                                            "• কখন ব্যবহার করবেন:\n" +
                                            "৯৯% ক্ষেত্রে এটিই ব্যবহার করুন। এটি সব অ্যাপেই স্বয়ংক্রিয়ভাবে সেরা অভিজ্ঞতা দেয়।",
                                    fontFamily = MonoFont,
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp,
                                    color = colors.textPrimary
                                )
                            }
                        }
                        InsertionMethod.ACCESSIBILITY_SET_TEXT -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "ACCESSIBILITY DIRECT (অ্যাক্সেসিবিলিটি সরাসরি প্রবেশ)",
                                    fontFamily = MonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "• কী করে (What it actually does):\n" +
                                            "আপনার ফোনের ক্লিপবোর্ডে কপি করা কোনো টেক্সট স্পর্শ না করে সরাসরি কাঙ্ক্ষিত অ্যাপের টেক্সট বাফারে লেখা প্রবেশ করায়। এর ফলে আপনার পূর্বে কপি করা কোনো টেক্সট বা পাসওয়ার্ড নষ্ট হয় না।\n\n" +
                                            "• কখন ব্যবহার করবেন:\n" +
                                            "হোয়াটসঅ্যাপ, মেসেঞ্জার, টেলিগ্রাম, ক্রোম ব্রাউজার, গুগল কিপ এবং স্ট্যান্ডার্ড নোটস অ্যাপে।",
                                    fontFamily = MonoFont,
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp,
                                    color = colors.textPrimary
                                )
                            }
                        }
                        InsertionMethod.CLIPBOARD_PASTE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "CLIPBOARD PASTE (ক্লিপবোর্ড পেস্ট পদ্ধতি)",
                                    fontFamily = MonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.signalOchre
                                )
                                Text(
                                    text = "• কী করে (What it actually does):\n" +
                                            "প্রতিটি বাক্য ক্লিপবোর্ডে কপি করে কার্সার পজিশনে পেস্ট কমান্ড দেয়।\n\n" +
                                            "• কখন ব্যবহার করবেন:\n" +
                                            "যদি কোনো বিশেষ গেমের চ্যাট, রিঅ্যাক্ট-নেটিভ/ফ্লাটার অ্যাপ বা জটিল ব্যাংকিং অ্যাপ অ্যাক্সেসিবিলিটি সরাসরি লেখা আটকে দেয়, তবে এটি ব্যবহার করবেন।",
                                    fontFamily = MonoFont,
                                    fontSize = 11.sp,
                                    lineHeight = 17.sp,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        SettingCard(
            title = "3. Partial Throttle Interval • স্ক্রিন রিফ্রেশ গতি",
            description = "কথা বলার সময় প্রতি কত মিলি.সেকেন্ড পর পর স্ক্রিনের লেখা রিফ্রেশ হবে। ৫০ms রাখলে লেখা কোনো প্রকার কাঁপাকাঁপি ছাড়াই মাখনের মতো মসৃণ দেখায়।"
        ) {
            InkRulerSlider(
                value = settings.throttleIntervalMs.toFloat(),
                onValueChange = { repo.updateSettings { s -> s.copy(throttleIntervalMs = it.toLong()) } },
                valueRange = 20f..250f,
                unitLabel = "ms"
            )
        }

        SettingToggleRow(
            title = "Auto-Add Leading Space • পূর্বে স্বয়ংক্রিয় স্পেস যোগ",
            description = "কার্সার লেখার মাঝে বা শেষে থাকলে এবং আগের শব্দের পর স্পেস না থাকলে, ভয়েস লেখার শুরুতে স্বয়ংক্রিয়ভাবে একটি স্পেস যুক্ত করবে।\n\n" +
                    "• কী করে (What it does):\n" +
                    "কার্সার যখন কোনো লেখার মাঝে বা শেষে থাকে (যেমন: 'দেখি তো ,' এর পর) এবং ঠিক আগের অক্ষরের পর কোনো স্পেস না থাকে, তখন নতুন রূপান্তরিত কথার শুরুতে এটি নিজে থেকে একটি স্পেস বসিয়ে দেয়।\n\n" +
                    "• ব্যবহারবিধি (When to use):\n" +
                    "ব্যাকরণগতভাবে সঠিক লেখার জন্য এটি চালু রাখা অত্যন্ত প্রয়োজনীয়। এর ফলে দুটি শব্দ বা কমা/দাড়ির পর কথা কখনোই জোড়া লেগে যাবে না (যেমন: 'দেখি তো , ঠিক আছে' হবে, 'দেখি তো ,ঠিক আছে' হবে না)।",
            checked = settings.autoLeadingSpace,
            onCheckedChange = { repo.updateSettings { s -> s.copy(autoLeadingSpace = it) } }
        )

        SettingToggleRow(
            title = "Add Trailing Space • বাক্য শেষে অতিরিক্ত স্পেস যোগ",
            description = "কথা বলা শেষ হলে লেখার শেষে একটি অতিরিক্ত স্পেস যোগ করবে কি না তা নিয়ন্ত্রণ করুন।\n\n" +
                    "• কী করে (What it does):\n" +
                    "কথা বলা শেষ হলে বাক্যের শেষে একটি স্পেস যোগ করবে কি না তা ঠিক করে। বন্ধ রাখলে ঠিক শেষ উচ্চারিত শব্দের পরই লেখা থেমে যাবে (যেমন: 'The weather is nice'), কোনো অতিরিক্ত স্পেস যোগ হবে না। চালু রাখলে পরবর্তী বাক্য সহজে টাইপ করার জন্য শেষে একটি স্পেস যোগ হবে (যেমন: 'The weather is nice ')।\n\n" +
                    "• ব্যবহারবিধি (When to use):\n" +
                    "সার্চ বারে সার্চ করার সময় বা বাক্যের শেষে বাড়তি স্পেস অপছন্দ করলে এটি বন্ধ রাখুন। আর স্বাভাবিক লম্বা চ্যাটিংয়ের ক্ষেত্রে চালু রাখতে পারেন।",
            checked = settings.addTrailingSpace,
            onCheckedChange = { repo.updateSettings { s -> s.copy(addTrailingSpace = it) } }
        )

        SettingToggleRow(
            title = "Filter Subtle Placeholders • প্লেসহোল্ডার টেক্সট ছাঁকন",
            description = "হোয়াটসঅ্যাপ, ক্রোম বা অন্যান্য অ্যাপের খালি বক্সে লেখা হালকা ধূসর নির্দেশিকা (যেমন 'Message', 'Search or type URL') যাতে ভয়েস লেখার সাথে জুড়ে না যায় তা স্বয়ংক্রিয়ভাবে দূর করে।\nEliminates subtle hint/placeholder text (like 'Message' in WhatsApp) from accidentally attaching before your converted speech.",
            checked = settings.filterHintPlaceholders,
            onCheckedChange = { repo.updateSettings { s -> s.copy(filterHintPlaceholders = it) } }
        )

        SettingToggleRow(
            title = "Follow My Cursor • কার্সার অবস্থান অনুসরণ",
            description = "কথা বলার মাঝে কিবোর্ড দিয়ে লেখার মাঝখানে কার্সার সরালে বা কিছু টাইপ করলে, নতুন কথা সেখানেই গিয়ে যুক্ত হবে—আগের লেখা মুছে যাবে না।\nAlways writes wherever you tap your regular keyboard cursor.",
            checked = settings.followCursor,
            onCheckedChange = { repo.updateSettings { s -> s.copy(followCursor = it) } }
        )

        SettingToggleRow(
            title = "Refuse Password Fields • পাসওয়ার্ড সুরক্ষা",
            description = "পিন বা পাসওয়ার্ডের ঘরে ভয়েস লেখা প্রবেশ বন্ধ রাখে যাতে কোনো সংবেদনশীল তথ্য ভুল করে টাইপ না হয়।\nPrevents accidental voice entry in sensitive secure fields.",
            checked = settings.refusePasswordFields,
            onCheckedChange = { repo.updateSettings { s -> s.copy(refusePasswordFields = it) } }
        )

        SettingCard(
            title = "Live Caption Bubble • ভাসমান শব্দ বাবল কাস্টমাইজেশন",
            description = "কথা বলার সময় ভাসমান বাটনের কাছে সম্পূর্ণ ও স্পষ্ট আকারে আপনার বলা বাক্যটি দেখায়, যাতে টেক্সট বক্সে না তাকিয়ে সরাসরি বাটনের দিকে তাকিয়েই সম্পূর্ণ কথা পরিষ্কার বোঝা যায়।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingToggleRow(
                    title = "Enable Live Caption Bubble • ভাসমান বাবল সক্রিয় রাখুন",
                    description = "কথা বলার সময় রিয়েল-টাইমে উচ্চারিত শব্দ এবং পূর্ণ বাক্য ভাসমান পিল-এ প্রদর্শন করে।\nShows a clean floating text pill displaying what you are speaking in real time.",
                    checked = settings.showLiveCaptionBubble,
                    onCheckedChange = { repo.updateSettings { s -> s.copy(showLiveCaptionBubble = it) } }
                )

                AnimatedVisibility(visible = settings.showLiveCaptionBubble) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // 1. Position
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "বাবলের অবস্থান • Bubble Position",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "বাটনের ঠিক উপরে, নিচে অথবা স্ক্রিনের একদম শীর্ষে ডায়নামিক বার আকারে দেখতে পছন্দ অনুযায়ী নির্ধারণ করুন।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                            InkSegmentedControl(
                                items = listOf(
                                    CaptionBubblePosition.ABOVE_ORB,
                                    CaptionBubblePosition.BELOW_ORB,
                                    CaptionBubblePosition.TOP_BANNER
                                ),
                                selectedItem = settings.captionBubblePosition,
                                onItemSelected = { repo.updateSettings { s -> s.copy(captionBubblePosition = it) } },
                                labelProvider = {
                                    when (it) {
                                        CaptionBubblePosition.ABOVE_ORB -> "Above Orb • উপরে"
                                        CaptionBubblePosition.BELOW_ORB -> "Below Orb • নিচে"
                                        CaptionBubblePosition.TOP_BANNER -> "Top Banner • শীর্ষে"
                                    }
                                }
                            )
                        }

                        // 2. Text Size
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "লেখার আকার ও স্পষ্টতা • Text Size & Readability",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "দূরে থাকা বা দ্রুত নজর বুলিয়ে বোঝার জন্য ফন্ট সাইজ বড় করতে পারেন।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                            InkSegmentedControl(
                                items = listOf(
                                    CaptionBubbleSize.COMPACT,
                                    CaptionBubbleSize.MEDIUM,
                                    CaptionBubbleSize.LARGE,
                                    CaptionBubbleSize.EXTRA_LARGE
                                ),
                                selectedItem = settings.captionBubbleSize,
                                onItemSelected = { repo.updateSettings { s -> s.copy(captionBubbleSize = it) } },
                                labelProvider = {
                                    when (it) {
                                        CaptionBubbleSize.COMPACT -> "১৩sp ছোট"
                                        CaptionBubbleSize.MEDIUM -> "১৬sp মাঝারি"
                                        CaptionBubbleSize.LARGE -> "১৯sp বড়"
                                        CaptionBubbleSize.EXTRA_LARGE -> "২২sp সর্বোচ্চ"
                                    }
                                }
                            )
                        }

                        // 3. Max Lines
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "সর্বোচ্চ লাইন সংখ্যা • Max Display Lines",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "লম্বা বক্তব্য বা পূর্ণ বাক্য যাতে কোনো অংশ কাটা না পড়ে সেজন্য ২ বা ৩ লাইন নির্বাচন করুন।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                            InkSegmentedControl(
                                items = listOf(1, 2, 3),
                                selectedItem = settings.captionMaxLines,
                                onItemSelected = { repo.updateSettings { s -> s.copy(captionMaxLines = it) } },
                                labelProvider = {
                                    when (it) {
                                        1 -> "১ লাইন (সংক্ষিপ্ত)"
                                        2 -> "২ লাইন (স্বাভাবিক)"
                                        3 -> "৩ লাইন (পূর্ণ বাক্য)"
                                        else -> "$it লাইন"
                                    }
                                }
                            )
                        }

                        // 4. Bubble Style
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "বাবলের দৃশ্যমান থিম • Bubble Visual Theme",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "অর্ধস্বচ্ছ কাঁচ, গাঢ় কালো বা বাটন কালারের রঙিন আভা নির্ধারণ করুন।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                            InkSegmentedControl(
                                items = listOf(
                                    CaptionBubbleStyle.TRANSLUCENT_DARK,
                                    CaptionBubbleStyle.SOLID_AMOLED,
                                    CaptionBubbleStyle.ACCENT_THEMED
                                ),
                                selectedItem = settings.captionBubbleStyle,
                                onItemSelected = { repo.updateSettings { s -> s.copy(captionBubbleStyle = it) } },
                                labelProvider = {
                                    when (it) {
                                        CaptionBubbleStyle.TRANSLUCENT_DARK -> "Dark Glass"
                                        CaptionBubbleStyle.SOLID_AMOLED -> "AMOLED Black"
                                        CaptionBubbleStyle.ACCENT_THEMED -> "Accent Glow"
                                    }
                                }
                            )
                        }

                        // 5. Review Linger Duration
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "পড়ার জন্য প্রদর্শনের সময়কাল • Review Linger Time",
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "কথা বলা শেষ হওয়ার পর বাক্যটি স্ক্রিনে কতক্ষণ দৃশ্যমান থাকবে (${String.format("%.1f", settings.captionLingerDurationMs / 1000f)}s), যাতে টেক্সট বক্সে না তাকিয়ে সরাসরি পুরো বাক্যটি পড়ে নেওয়া যায়।",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                            InkRulerSlider(
                                value = settings.captionLingerDurationMs.toFloat(),
                                onValueChange = { repo.updateSettings { s -> s.copy(captionLingerDurationMs = it.toLong()) } },
                                valueRange = 1000f..3500f,
                                unitLabel = "ms"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Station04Orb(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Lock Position Toggle
        SettingToggleRow(
            title = "Lock Position • নির্দিষ্ট স্থানে বাটন লক রাখুন",
            description = "বাটনটিকে স্ক্রিনের এক কোণে স্থায়ীভাবে আটকে রাখে যাতে হাত লেগে বা ড্র্যাগ করে নড়ে না যায়। Locks the Orb firmly to your chosen screen anchor, preventing accidental moves.",
            checked = settings.lockPosition,
            onCheckedChange = { locked ->
                repo.updateSettings { s -> s.copy(lockPosition = locked) }
            }
        )

        // Fixed Screen Position Anchor Selector
        SettingCard(
            title = "Fixed Screen Anchor • স্ক্রিনে নির্দিষ্ট অবস্থান",
            description = "বাটনটি স্ক্রিনের কোন পাশে বা কোণে স্থায়ীভাবে ভেসে থাকবে তা নির্বাচন করুন। ট্যাপ করলেই বাটনটি তাৎক্ষণিকভাবে সেই স্থানে চলে যাবে এবং স্থায়ীভাবে সংরক্ষিত হবে।"
        ) {
            val anchors = listOf(
                "RIGHT_MIDDLE" to "Right Mid • ডান মাঝে",
                "RIGHT_TOP" to "Right Top • ডান উপরে",
                "RIGHT_BOTTOM" to "Right Btm • ডান নিচে",
                "LEFT_MIDDLE" to "Left Mid • বাম মাঝে",
                "LEFT_TOP" to "Left Top • বাম উপরে",
                "LEFT_BOTTOM" to "Left Btm • বাম নিচে"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Top row: Right side anchors
                Text(
                    text = "ডান পাশ (Right Edge):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    anchors.take(3).forEach { (anchorId, label) ->
                        val isSelected = settings.fixedScreenAnchor == anchorId
                        Button(
                            onClick = { repo.setScreenAnchor(anchorId) },
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface
                            ),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.textPrimary
                            )
                        }
                    }
                }

                // Bottom row: Left side anchors
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "বাম পাশ (Left Edge):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    anchors.drop(3).forEach { (anchorId, label) ->
                        val isSelected = settings.fixedScreenAnchor == anchorId
                        Button(
                            onClick = { repo.setScreenAnchor(anchorId) },
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface
                            ),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Orb Size Card with Quick Presets and Slider
        SettingCard(
            title = "Orb Size • বাটনের আকার",
            description = "ভাসমান বাটনের ব্যাস (২৮ থেকে ১২০ DP)। পছন্দমতো আকার বেছে নিন বা স্লাইডার দিয়ে নিখুঁতভাবে নির্ধারণ করুন।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Quick Size Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(
                        48 to "48 DP\nছোট",
                        56 to "56 DP\nআদর্শ",
                        64 to "64 DP\nমাঝারি",
                        76 to "76 DP\nবড়",
                        92 to "92 DP\nজাম্বো"
                    )
                    presets.forEach { (sizeDp, label) ->
                        val isSelected = settings.orbSizeDp == sizeDp
                        Button(
                            onClick = { repo.updateSettings { s -> s.copy(orbSizeDp = sizeDp) } },
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface
                            ),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = label,
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.textPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                InkRulerSlider(
                    value = settings.orbSizeDp.toFloat(),
                    onValueChange = { repo.updateSettings { s -> s.copy(orbSizeDp = it.toInt()) } },
                    valueRange = 28f..120f,
                    unitLabel = "dp"
                )
            }
        }

        // Voice Button & Transcription Vertical Spacing / Offset Card
        SettingCard(
            title = "Voice Button & Transcription Position • অবস্থান সমন্বয়",
            description = "মূল ফ্লোটিং বাটন (Orb) থেকে ভাষা পরিবর্তন বাটন এবং লাইভ টেক্সট ট্রানস্ক্রিপশন স্ট্রিপের উল্লম্ব অবস্থান (কতটুকু উপরে বা নিচে থাকবে) নিয়ন্ত্রণ করুন।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text(
                        text = "Voice Change Button Distance (ভাষা বাটনের দূরত্ব): ${settings.voiceButtonVerticalOffsetDp} dp",
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "মূল বাটন থেকে ভাষা পরিবর্তন বাটনের উল্লম্ব ব্যবধান (DP)। মান বৃদ্ধি করলে আরও উপরে উঠবে।",
                        fontFamily = MonoFont,
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InkRulerSlider(
                        value = settings.voiceButtonVerticalOffsetDp.toFloat(),
                        onValueChange = { repo.updateSettings { s -> s.copy(voiceButtonVerticalOffsetDp = it.toInt()) } },
                        valueRange = -20f..80f,
                        unitLabel = "dp"
                    )
                }

                Column {
                    Text(
                        text = "Transcription Strip Distance (ট্রানস্ক্রিপশন স্ট্রিপের দূরত্ব): ${settings.transcriptionVerticalOffsetDp} dp",
                        fontFamily = MonoFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "মূল বাটন থেকে লাইভ টেক্সট ট্রানস্ক্রিপশন স্ট্রিপের উল্লম্ব ব্যবধান (DP)। মান বৃদ্ধি করলে আরও উপরে উঠবে।",
                        fontFamily = MonoFont,
                        fontSize = 10.sp,
                        color = colors.textMuted
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    InkRulerSlider(
                        value = settings.transcriptionVerticalOffsetDp.toFloat(),
                        onValueChange = { repo.updateSettings { s -> s.copy(transcriptionVerticalOffsetDp = it.toInt()) } },
                        valueRange = -20f..80f,
                        unitLabel = "dp"
                    )
                }
            }
        }

        // Tap-to-Dismiss Caption to Undo
        SettingToggleRow(
            title = "Tap Bubble to Dismiss & Undo • বাবলে ট্যাপ করে মুছে ফেলা (Undo)",
            description = "ভাসমান প্রিভিউ বাবলে ট্যাপ করলেই শেষ বলা কথা সাথে সাথে মুছে যাবে (Undo) এবং বাবলটি মিলিয়ে যাবে। Tap the live caption bubble anytime to immediately revert the last dictated phrase and dismiss the bubble.",
            checked = settings.tapCaptionToUndo,
            onCheckedChange = { checked ->
                repo.updateSettings { s -> s.copy(tapCaptionToUndo = checked) }
            }
        )

        // Undo Badge Button Customization
        SettingCard(
            title = "Undo Badge Appearance & Style • আনডু বাটন কাস্টমাইজেশন",
            description = "কথা বলা শেষ হওয়ার পর বাবলে দৃশ্যমান 'আনডু' বাটনের রঙ, শৈলী, আকার ও লেবেল আপনার পছন্দমতো নির্ধারণ করুন যাতে এটি সহজে চোখে পড়ে।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // 1. Live Interactive Preview of the Undo Badge
                Text(
                    text = "Live Preview (প্রিভিউ):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                val previewCustomColor = settings.undoBadgeCustomColor.toInt()
                val previewAccent = if (previewCustomColor != 0) Color(previewCustomColor) else colors.accent
                val lum = (0.299f * previewAccent.red + 0.587f * previewAccent.green + 0.114f * previewAccent.blue)

                val (previewBg, previewBorder, previewText) = when (settings.undoBadgeStyle) {
                    UndoBadgeStyle.FILLED_ACCENT -> Triple(previewAccent, previewAccent.copy(alpha = 0.9f), if (lum > 0.45f) Color(0xFF121212) else Color.White)
                    UndoBadgeStyle.CRIMSON_DANGER -> Triple(Color(0xFFD32F2F), Color(0xFFFF5252), Color.White)
                    UndoBadgeStyle.AMBER_GLOW -> Triple(Color(0xFFFFB300), Color(0xFFFFD54F), Color(0xFF121212))
                    UndoBadgeStyle.MONOCHROME_INVERSE -> Triple(Color.White, Color(0xFFE0E0E0), Color(0xFF121212))
                    UndoBadgeStyle.OUTLINED_NEON -> Triple(previewAccent.copy(alpha = 0.22f), previewAccent, if (lum > 0.25f) previewAccent else Color.White)
                }

                val previewLabel = when (settings.undoBadgeLabelMode) {
                    UndoBadgeLabelMode.ICON_AND_TEXT -> "↶ Undo"
                    UndoBadgeLabelMode.ICON_ONLY -> "↶"
                    UndoBadgeLabelMode.TEXT_ONLY -> "UNDO"
                }

                val (badgeHeightDp, badgePadHDp, badgeFontSp) = when (settings.undoBadgeSize) {
                    UndoBadgeSize.COMPACT -> Triple(22.dp, 8.dp, 10.5.sp)
                    UndoBadgeSize.STANDARD -> Triple(26.dp, 10.dp, 12.sp)
                    UndoBadgeSize.PROMINENT -> Triple(30.dp, 14.dp, 13.5.sp)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xF21C1A18), RoundedCornerShape(18.dp))
                        .border(1.5.dp, colors.accent.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(colors.accent, RoundedCornerShape(5.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "আমি কথা বলেছি...",
                                fontFamily = DisplayFont,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }

                        // The Undo Badge Button Preview
                        Box(
                            modifier = Modifier
                                .height(badgeHeightDp)
                                .background(previewBg, RoundedCornerShape(badgeHeightDp / 2))
                                .border(1.5.dp, previewBorder, RoundedCornerShape(badgeHeightDp / 2))
                                .clickable {
                                    Toast.makeText(context, "Undo action preview tapped!", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = badgePadHDp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = previewLabel,
                                fontFamily = MonoFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = badgeFontSp,
                                color = previewText
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Badge Style Selector
                Text(
                    text = "Badge Visual Style (বাটনের শৈলী):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                val styles = listOf(
                    UndoBadgeStyle.FILLED_ACCENT to "Filled Accent • উজ্জ্বল অ্যাকসেন্ট",
                    UndoBadgeStyle.CRIMSON_DANGER to "Crimson Alert • লাল সংকেত",
                    UndoBadgeStyle.AMBER_GLOW to "Amber Glow • সোনালী সতর্কতা",
                    UndoBadgeStyle.MONOCHROME_INVERSE to "Inverse White • সাদা বৈসাদৃশ্য",
                    UndoBadgeStyle.OUTLINED_NEON to "Outlined Neon • বর্ডার নিয়ন"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    styles.forEach { (style, label) ->
                        val isSelected = settings.undoBadgeStyle == style
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.15f) else colors.surface)
                                .clickable { repo.updateSettings { s -> s.copy(undoBadgeStyle = style) } }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.textPrimary
                            )
                            if (isSelected) {
                                Text(
                                    text = "SELECTED",
                                    fontFamily = MonoFont,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 3. Label Mode (Icon + Text, Icon Only, Text Only)
                Text(
                    text = "Badge Content (লেবেল ও আইকন):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                InkSegmentedControl(
                    items = listOf(
                        UndoBadgeLabelMode.ICON_AND_TEXT,
                        UndoBadgeLabelMode.ICON_ONLY,
                        UndoBadgeLabelMode.TEXT_ONLY
                    ),
                    selectedItem = settings.undoBadgeLabelMode,
                    onItemSelected = { repo.updateSettings { s -> s.copy(undoBadgeLabelMode = it) } },
                    labelProvider = {
                        when (it) {
                            UndoBadgeLabelMode.ICON_AND_TEXT -> "↶ Undo"
                            UndoBadgeLabelMode.ICON_ONLY -> "↶ Only"
                            UndoBadgeLabelMode.TEXT_ONLY -> "UNDO Text"
                        }
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 4. Badge Size (Compact, Standard, Prominent)
                Text(
                    text = "Badge Size (বাটনের আকার):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                InkSegmentedControl(
                    items = listOf(
                        UndoBadgeSize.COMPACT,
                        UndoBadgeSize.STANDARD,
                        UndoBadgeSize.PROMINENT
                    ),
                    selectedItem = settings.undoBadgeSize,
                    onItemSelected = { repo.updateSettings { s -> s.copy(undoBadgeSize = it) } },
                    labelProvider = {
                        when (it) {
                            UndoBadgeSize.COMPACT -> "Compact (ছোট)"
                            UndoBadgeSize.STANDARD -> "Standard (আদর্শ)"
                            UndoBadgeSize.PROMINENT -> "Prominent (বড়)"
                        }
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 5. Pulse Animation Toggle
                SettingToggleRow(
                    title = "Breathing Glow Pulse • স্পন্দন অ্যানিমেশন",
                    description = "কথা বলা সম্পন্ন হওয়ার পর আনডু বাটনটি মৃদু স্পন্দিত হয়ে দৃষ্টি আকর্ষণ করবে।",
                    checked = settings.undoBadgePulse,
                    onCheckedChange = { checked ->
                        repo.updateSettings { s -> s.copy(undoBadgePulse = checked) }
                    }
                )

                // 6. Custom Badge Tint Color Palette
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Custom Badge Color Tint • নির্দিষ্ট রঙ নির্বাচন:",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                val colorOptions = listOf(
                    0L to "Theme Default (থিম)",
                    0xFFE53935L to "Crimson Red (লাল)",
                    0xFFF2542DL to "Vermilion (সিঁদুর)",
                    0xFFFFB300L to "Amber Gold (সোনালী)",
                    0xFF45A596L to "Verdigris (পান্না)",
                    0xFF4D62CDL to "Indigo Blue (নীল)",
                    0xFF9C27B0L to "Purple (বেগুনী)",
                    0xFFFFFFFFL to "Pure White (সাদা)"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorOptions.take(4).forEach { (colorVal, label) ->
                        val isSelected = settings.undoBadgeCustomColor == colorVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                .clickable { repo.updateSettings { s -> s.copy(undoBadgeCustomColor = colorVal) } }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.split(" ").first(),
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.textPrimary
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colorOptions.drop(4).forEach { (colorVal, label) ->
                        val isSelected = settings.undoBadgeCustomColor == colorVal
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.5.dp, if (isSelected) colors.accent else colors.hairline, RoundedCornerShape(2.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.2f) else colors.surface)
                                .clickable { repo.updateSettings { s -> s.copy(undoBadgeCustomColor = colorVal) } }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label.split(" ").first(),
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.accent else colors.textPrimary
                            )
                        }
                    }
                }
            }
        }

        // Caption Bubble Position and Style Customization in Section 04
        SettingCard(
            title = "Caption Bubble Position & Style • প্রিভিউ বাবলের অবস্থান ও শৈলী",
            description = "কথা বলার সময় স্ক্রিনে ভেসে ওঠা টেক্সট বাবলের অবস্থান, আকৃতি ও রঙ পছন্দমতো নির্ধারণ করুন।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Bubble Position (বাবলের অবস্থান):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                InkSegmentedControl(
                    items = listOf(
                        CaptionBubblePosition.ABOVE_ORB,
                        CaptionBubblePosition.BELOW_ORB,
                        CaptionBubblePosition.TOP_BANNER
                    ),
                    selectedItem = settings.captionBubblePosition,
                    onItemSelected = { repo.updateSettings { s -> s.copy(captionBubblePosition = it) } },
                    labelProvider = {
                        when (it) {
                            CaptionBubblePosition.ABOVE_ORB -> "Above Orb • উপরে"
                            CaptionBubblePosition.BELOW_ORB -> "Below Orb • নিচে"
                            CaptionBubblePosition.TOP_BANNER -> "Top Banner • শীর্ষে"
                        }
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bubble Style (বাবলের শৈলী):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                InkSegmentedControl(
                    items = listOf(
                        CaptionBubbleStyle.TRANSLUCENT_DARK,
                        CaptionBubbleStyle.SOLID_AMOLED,
                        CaptionBubbleStyle.ACCENT_THEMED
                    ),
                    selectedItem = settings.captionBubbleStyle,
                    onItemSelected = { repo.updateSettings { s -> s.copy(captionBubbleStyle = it) } },
                    labelProvider = {
                        when (it) {
                            CaptionBubbleStyle.TRANSLUCENT_DARK -> "Dark Glass"
                            CaptionBubbleStyle.SOLID_AMOLED -> "AMOLED Black"
                            CaptionBubbleStyle.ACCENT_THEMED -> "Accent Tint"
                        }
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Bubble Text Size (বাবলের অক্ষরের আকার):",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                InkSegmentedControl(
                    items = listOf(
                        CaptionBubbleSize.COMPACT,
                        CaptionBubbleSize.MEDIUM,
                        CaptionBubbleSize.LARGE,
                        CaptionBubbleSize.EXTRA_LARGE
                    ),
                    selectedItem = settings.captionBubbleSize,
                    onItemSelected = { repo.updateSettings { s -> s.copy(captionBubbleSize = it) } },
                    labelProvider = {
                        when (it) {
                            CaptionBubbleSize.COMPACT -> "Small (14sp)"
                            CaptionBubbleSize.MEDIUM -> "Medium (15sp)"
                            CaptionBubbleSize.LARGE -> "Large (18sp)"
                            CaptionBubbleSize.EXTRA_LARGE -> "XL (20sp)"
                        }
                    }
                )
            }
        }

        SettingToggleRow(
            title = "Show Orb Only When Keyboard Appears",
            description = "কিবোর্ড আসলেই বাটন ভেসে উঠবে। The Orb automatically appears whenever the keyboard opens in any text field, and disappears when the keyboard closes.",
            checked = settings.visibilityRule == VisibilityRule.KEYBOARD_VISIBLE,
            onCheckedChange = { checked ->
                repo.updateSettings { s ->
                    s.copy(visibilityRule = if (checked) VisibilityRule.KEYBOARD_VISIBLE else VisibilityRule.ALWAYS)
                }
            }
        )

        SettingCard(
            title = "Orb Visibility Rule",
            description = "Control when the floating button is displayed on screen."
        ) {
            InkSegmentedControl(
                items = listOf(VisibilityRule.ALWAYS, VisibilityRule.KEYBOARD_VISIBLE, VisibilityRule.EDITABLE_FOCUSED),
                selectedItem = settings.visibilityRule,
                onItemSelected = { repo.updateSettings { s -> s.copy(visibilityRule = it) } },
                labelProvider = {
                    when (it) {
                        VisibilityRule.ALWAYS -> "Always"
                        VisibilityRule.KEYBOARD_VISIBLE -> "Keyboard Only"
                        VisibilityRule.EDITABLE_FOCUSED -> "Text Fields"
                    }
                }
            )
        }

        SettingCard(
            title = "Orb Shape",
            description = "Geometric silhouette drawn on Canvas."
        ) {
            InkSegmentedControl(
                items = listOf(OrbShape.CIRCLE, OrbShape.SQUIRCLE, OrbShape.ROUNDED_SQUARE, OrbShape.SEAL),
                selectedItem = settings.orbShape,
                onItemSelected = { repo.updateSettings { s -> s.copy(orbShape = it) } },
                labelProvider = { it.name.lowercase().capitalize() }
            )
        }

        SettingCard(
            title = "Ring Style",
            description = "Visual ring signature around the Orb."
        ) {
            InkSegmentedControl(
                items = listOf(RingStyle.RADIAL_TICKS, RingStyle.SOLID_ARC, RingStyle.DOTTED, RingStyle.NONE),
                selectedItem = settings.ringStyle,
                onItemSelected = { repo.updateSettings { s -> s.copy(ringStyle = it) } },
                labelProvider = { it.name.lowercase().replace("_", " ") }
            )
        }

        SettingCard(
            title = "Idle Opacity",
            description = "Transparency of the Orb when idle."
        ) {
            InkRulerSlider(
                value = settings.idleOpacity * 100f,
                onValueChange = { repo.updateSettings { s -> s.copy(idleOpacity = it / 100f) } },
                valueRange = 20f..100f,
                unitLabel = "%"
            )
        }

        SettingCard(
            title = "Double Tap Gesture Action",
            description = "Action when double-tapping the Orb."
        ) {
            InkSegmentedControl(
                items = listOf(GestureAction.SWITCH_MODE, GestureAction.SWITCH_LANGUAGE, GestureAction.UNDO),
                selectedItem = settings.doubleTapAction,
                onItemSelected = { repo.updateSettings { s -> s.copy(doubleTapAction = it) } },
                labelProvider = {
                    when (it) {
                        GestureAction.SWITCH_MODE -> "Switch Mode"
                        GestureAction.SWITCH_LANGUAGE -> "Switch Lang"
                        GestureAction.UNDO -> "Undo Last"
                        else -> "None"
                    }
                }
            )
        }

        SettingToggleRow(
            title = "Snap to Nearest Edge",
            description = "Spring snaps to left or right margin when drag is released.",
            checked = settings.snapToEdge,
            onCheckedChange = { repo.updateSettings { s -> s.copy(snapToEdge = it) } }
        )

        SettingToggleRow(
            title = "Dock on Keyboard Open",
            description = "Peeks out half-hidden at edge when on-screen keyboard appears.",
            checked = settings.dockToEdgeOnKeyboard,
            onCheckedChange = { repo.updateSettings { s -> s.copy(dockToEdgeOnKeyboard = it) } }
        )

        SettingCard(title = "Custom Palette per State", description = "Hex and swatches for Orb states.") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InkColorPicker(
                    label = "Listening State",
                    selectedColor = settings.listeningColor,
                    onColorSelected = { repo.updateSettings { s -> s.copy(listeningColor = it) } }
                )
                InkColorPicker(
                    label = "Waiting Restart State",
                    selectedColor = settings.waitingColor,
                    onColorSelected = { repo.updateSettings { s -> s.copy(waitingColor = it) } }
                )
                InkColorPicker(
                    label = "Error State",
                    selectedColor = settings.errorColor,
                    onColorSelected = { repo.updateSettings { s -> s.copy(errorColor = it) } }
                )
            }
        }
    }
}

@Composable
private fun Station05SoundFeel(settings: KothaSettings, repo: SettingsRepository) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingToggleRow(
            title = "Beep Suppression (On by default)",
            description = "Selectively mutes media and system tone streams (leaving phone calls, ringtones, and notification alerts intact) to completely eliminate annoying Google Speech beep sounds.",
            checked = settings.beepSuppression,
            onCheckedChange = { repo.updateSettings { s -> s.copy(beepSuppression = it) } }
        )

        SettingToggleRow(
            title = "Haptic Feedback",
            description = "Tactile feedback on taps, drags, detents, and mode changes.",
            checked = settings.hapticsEnabled,
            onCheckedChange = { repo.updateSettings { s -> s.copy(hapticsEnabled = it) } }
        )

        SettingToggleRow(
            title = "Keep Screen Awake",
            description = "Prevent display sleep while actively dictating.",
            checked = settings.keepScreenAwake,
            onCheckedChange = { repo.updateSettings { s -> s.copy(keepScreenAwake = it) } }
        )

        SettingToggleRow(
            title = "Duck Media Audio",
            description = "Lowers background media volume while microphone is listening.",
            checked = settings.duckMediaAudio,
            onCheckedChange = { repo.updateSettings { s -> s.copy(duckMediaAudio = it) } }
        )
    }
}

@Composable
private fun Station06LanguageTools(settings: KothaSettings, repo: SettingsRepository) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingToggleRow(
            title = "Auto-Punctuation",
            description = "Appends Bangla danda '।' or Latin '.' at the end of sentences with proper spacing rules.",
            checked = settings.autoPunctuate,
            onCheckedChange = { repo.updateSettings { s -> s.copy(autoPunctuate = it) } }
        )

        SettingCard(
            title = "Digit Conversion",
            description = "Convert digits between Bengali and Western numerals."
        ) {
            InkSegmentedControl(
                items = listOf("NONE", "TO_BANGLA", "TO_WESTERN"),
                selectedItem = settings.digitConversionModeName,
                onItemSelected = { repo.updateSettings { s -> s.copy(digitConversionModeName = it) } },
                labelProvider = {
                    when (it) {
                        "TO_BANGLA" -> "০-৯ (Bangla)"
                        "TO_WESTERN" -> "0-9 (Western)"
                        else -> "As Spoken"
                    }
                }
            )
        }

        SettingToggleRow(
            title = "Voice Commands",
            description = "Recognises spoken editing and punctuation commands (customizable in Station 09).",
            checked = settings.voiceCommandsEnabled,
            onCheckedChange = { repo.updateSettings { s -> s.copy(voiceCommandsEnabled = it) } }
        )

        SettingToggleRow(
            title = "Auto-Capitalize Latin Sentences",
            description = "Capitalizes initial letter for English / Latin script.",
            checked = settings.autoCapitalizeLatin,
            onCheckedChange = { repo.updateSettings { s -> s.copy(autoCapitalizeLatin = it) } }
        )
    }
}

@Composable
private fun Station07System(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current
    val keeper = (context.applicationContext as KothaApplication).accessibilityKeeper
    val isKeeperPermitted by keeper.hasSecureSettingsPermission.collectAsState()
    val repairEvents by keeper.repairEventsCount.collectAsState()
    val isAutoRepairStopped by keeper.isAutoRepairStopped.collectAsState()
    val isBridgeConnected by TextBridgeStateHolder.isConnected.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Permission Ledger
        SettingCard(
            title = "Live Permission Ledger",
            description = "Status of required platform permissions. Tap to open system settings."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PermissionStatusItem(
                    name = "Microphone (RECORD_AUDIO)",
                    granted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
                PermissionStatusItem(
                    name = "Display Over Other Apps (SYSTEM_ALERT_WINDOW)",
                    granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Settings.canDrawOverlays(context) else true,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    }
                )
                PermissionStatusItem(
                    name = "Kotha Accessibility Bridge",
                    granted = isBridgeConnected,
                    onClick = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                )
                PermissionStatusItem(
                    name = "Auto-Repair (WRITE_SECURE_SETTINGS)",
                    granted = isKeeperPermitted,
                    onClick = {}
                )
            }
        }

        // Accessibility Keeper Card & One-time ADB Grant
        SettingCard(
            title = "Self-Healing Accessibility Keeper",
            description = "Android / HyperOS frequently kills accessibility services. Keeper revives it automatically."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingToggleRow(
                    title = "Keep Accessibility Service Alive",
                    description = "Observer detects disable events and re-enables Kotha.",
                    checked = settings.keepAccessibilityAlive,
                    onCheckedChange = { repo.updateSettings { s -> s.copy(keepAccessibilityAlive = it) } }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Repairs executed: $repairEvents",
                        fontFamily = MonoFont,
                        fontSize = 12.sp,
                        color = colors.textPrimary
                    )
                    if (isAutoRepairStopped) {
                        Text(
                            text = "[ Auto-Repair Paused ]",
                            fontFamily = MonoFont,
                            fontSize = 12.sp,
                            color = colors.signalCrimson
                        )
                    }
                }

                if (!isKeeperPermitted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
                            .background(colors.background)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "এককালীন অনুদান / One-Time ADB Grant",
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.accent
                            )
                            Text(
                                text = "কম্পিউটার থেকে ইউএসবি দিয়ে একবার এই কমান্ডটি চালান, ফোন রিবুট হলেও চালু থাকবে।\nRun this adb shell command once from a computer. It persists across reboots.",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = colors.textMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val adbCommand = keeper.getAdbCommand()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.surface)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = adbCommand,
                                    fontFamily = MonoFont,
                                    fontSize = 11.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("ADB Command", adbCommand))
                                        Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.accent)
                                }
                            }
                        }
                    }
                }
            }
        }

        // MIUI / HyperOS Guide Card
        SettingCard(
            title = "Xiaomi MIUI / HyperOS Guide",
            description = "Special instructions for Xiaomi / Redmi / POCO devices running MIUI or HyperOS."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "• Restricted Settings: On sideloaded APKs, open App info → ⋮ → 'Allow restricted settings' if Accessibility is greyed out.\n" +
                            "• Battery Saver: Set to 'No restrictions'.\n" +
                            "• Pop-up Windows: Enable 'Display pop-up windows while running in the background'.\n" +
                            "• Autostart: Allow autostart in app info.\n" +
                            "• Recents: Lock Kotha in recent apps.",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = colors.textMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        try {
                            val intent = Intent().apply {
                                setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
                ) {
                    Text(text = "Open App Settings", fontFamily = MonoFont, fontSize = 12.sp, color = colors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun Station08Data(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current
    val logs by DiagnosticsLog.entriesFlow.collectAsState()

    var showManualImportDialog by remember { mutableStateOf(false) }
    var manualJsonText by remember { mutableStateOf("") }
    var manualImportError by remember { mutableStateOf<String?>(null) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = repo.getExportJson()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "Settings backup exported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to export settings: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader(Charsets.UTF_8).readText()
                }
                if (!json.isNullOrBlank()) {
                    val success = repo.importFromJson(json)
                    if (success) {
                        Toast.makeText(context, "All settings restored successfully!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "Error: Invalid settings file format", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "Selected file is empty", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showManualImportDialog) {
        AlertDialog(
            onDismissRequest = { showManualImportDialog = false },
            title = {
                Text(
                    text = "Restore Settings from JSON",
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste the JSON backup configuration from your previous phone:",
                        fontFamily = MonoFont,
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )

                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipText = cm.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!clipText.isNullOrBlank()) {
                                manualJsonText = clipText
                                manualImportError = null
                            } else {
                                Toast.makeText(context, "Clipboard is empty", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Paste", tint = colors.accent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Paste from Clipboard", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                    }

                    OutlinedTextField(
                        value = manualJsonText,
                        onValueChange = {
                            manualJsonText = it
                            manualImportError = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = {
                            Text(text = "{\n  \"primaryLanguage\": \"bn-BD\",\n  ...\n}", fontFamily = MonoFont, fontSize = 10.sp, color = colors.textMuted)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.hairline,
                            focusedContainerColor = colors.background,
                            unfocusedContainerColor = colors.background
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )

                    manualImportError?.let { err ->
                        Text(
                            text = err,
                            fontFamily = MonoFont,
                            fontSize = 11.sp,
                            color = colors.signalCrimson
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (manualJsonText.isBlank()) {
                            manualImportError = "Please enter or paste JSON settings text"
                            return@Button
                        }
                        val success = repo.importFromJson(manualJsonText)
                        if (success) {
                            showManualImportDialog = false
                            Toast.makeText(context, "All settings restored successfully!", Toast.LENGTH_LONG).show()
                        } else {
                            manualImportError = "Failed to parse JSON. Please check file format."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(text = "Restore Settings", fontFamily = MonoFont, fontSize = 11.sp, color = colors.background)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualImportDialog = false }) {
                    Text(text = "Cancel", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(4.dp)
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = "Reset All Settings?",
                    fontFamily = DisplayFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = colors.signalCrimson
                )
            },
            text = {
                Text(
                    text = "This will restore all voice dictation parameters, orb styling, insertion timings, gestures, and language rules to initial factory defaults.",
                    fontFamily = MonoFont,
                    fontSize = 12.sp,
                    color = colors.textPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repo.resetToDefaults()
                        showResetConfirmDialog = false
                        Toast.makeText(context, "Reset to factory defaults", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.signalCrimson),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(text = "Reset", fontFamily = MonoFont, fontSize = 11.sp, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(text = "Cancel", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textMuted)
                }
            },
            containerColor = colors.surface,
            shape = RoundedCornerShape(4.dp)
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingCard(
            title = "Visual Appearance",
            description = "App theme and accent ink."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InkSegmentedControl(
                    items = listOf(AppTheme.DARK, AppTheme.PAPER, AppTheme.AMOLED),
                    selectedItem = settings.appTheme,
                    onItemSelected = { repo.updateSettings { s -> s.copy(appTheme = it) } },
                    labelProvider = { it.name.lowercase().capitalize() }
                )
                InkSegmentedControl(
                    items = listOf(AccentInk.VERMILION, AccentInk.OCHRE, AccentInk.VERDIGRIS, AccentInk.INDIGO),
                    selectedItem = settings.accentInk,
                    onItemSelected = { repo.updateSettings { s -> s.copy(accentInk = it) } },
                    labelProvider = { it.displayName.split(" ")[0] }
                )
            }
        }

        SettingCard(
            title = "Export & Import Settings",
            description = "Backup your entire app configuration (voice parameters, floating orb styling, gestures, colors, coordinate positions, and custom vocabulary) into a JSON file to restore on a new phone as like your previous app."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Section: Export
                Text(
                    text = "BACKUP SETTINGS (CURRENT PHONE)",
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportFileLauncher.launch("kotha_settings_backup.json")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export File", tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Save File", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                    }

                    Button(
                        onClick = {
                            val json = repo.getExportJson()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "Kotha Settings Backup")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Settings Backup"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Share", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                    }

                    Button(
                        onClick = {
                            val json = repo.getExportJson()
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Kotha Settings Backup", json))
                            Toast.makeText(context, "Settings JSON copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy", tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Copy", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section: Import
                Text(
                    text = "RESTORE SETTINGS (NEW PHONE)",
                    fontFamily = MonoFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = "Restore File", tint = colors.signalVerdigris, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Select File", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                    }

                    Button(
                        onClick = {
                            manualJsonText = ""
                            manualImportError = null
                            showManualImportDialog = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Paste JSON", tint = colors.signalVerdigris, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Paste JSON", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section: Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reset all configuration to factory defaults",
                        fontFamily = MonoFont,
                        fontSize = 11.sp,
                        color = colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showResetConfirmDialog = true },
                        modifier = Modifier.border(1.dp, colors.signalCrimson.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.RestartAlt, contentDescription = "Reset", tint = colors.signalCrimson, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Reset All", fontFamily = MonoFont, fontSize = 11.sp, color = colors.signalCrimson)
                    }
                }
            }
        }

        // About & Privacy Statement
        SettingCard(
            title = "About Kotha • কথা",
            description = "Zero-cloud privacy commitment and architecture records."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "কথা (Kotha) v1.0 • Client-side voice dictation instrument.\n\n" +
                            "Privacy Statement: Kotha contains zero advertising SDKs, zero telemetry, zero analytics, and performs zero custom network requests. Spoken audio is sent only to the on-device Google speech engine chosen by your operating system. Dictated text is never logged or stored unless you manually enable local history.",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusItem(
    name: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKothaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
            .background(colors.surface)
            .clickable { onClick() }
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontFamily = MonoFont, fontSize = 12.sp, color = colors.textPrimary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (granted) "[ GRANTED ]" else "[ NOT GRANTED ]",
            fontFamily = MonoFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (granted) colors.signalVerdigris else colors.signalCrimson
        )
    }
}

@Composable
private fun SettingCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    val colors = LocalKothaColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
            .background(colors.surface)
            .padding(14.dp)
    ) {
        Text(text = title, fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
        Text(text = description, fontFamily = MonoFont, fontSize = 11.sp, color = colors.textMuted)
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = LocalKothaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
            .background(colors.surface)
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = colors.textPrimary)
            Text(text = description, fontFamily = MonoFont, fontSize = 11.sp, color = colors.textMuted)
        }
        Spacer(modifier = Modifier.width(12.dp))
        InkBracketToggle(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Station09VoiceCommands(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current
    val commands = remember(settings.voiceCommandsJson) {
        LanguageTools.commandsFromJson(settings.voiceCommandsJson)
    }
    var expandedCommandId by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingToggleRow(
            title = "Voice Commands Master Switch • ভয়েস কমান্ড চালু/বন্ধ",
            description = "কথা বলার সময় নির্দিষ্ট ভয়েস কমান্ড বললে টেক্সট লেখার বদলে কমান্ডটি কার্যকর হবে (যেমন নতুন লাইন, আনডু, মোছা, পাঠানো ইত্যাদি)।",
            checked = settings.voiceCommandsEnabled,
            onCheckedChange = { repo.updateSettings { s -> s.copy(voiceCommandsEnabled = it) } }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "15 SMART COMMANDS (${commands.size} ACTIONS)",
                fontFamily = MonoFont,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = colors.textMuted
            )

            Button(
                onClick = {
                    repo.updateSettings { it.copy(voiceCommandsJson = "") }
                    Toast.makeText(context, "All commands reset to defaults", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset all",
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Reset All",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    color = colors.textPrimary
                )
            }
        }

        commands.forEach { cmd ->
            val isExpanded = expandedCommandId == cmd.id
            var newTriggerInput by remember { mutableStateOf("") }
            var isBatchEditOpen by remember { mutableStateOf(false) }
            var batchEditText by remember(cmd.triggers) { mutableStateOf(cmd.triggers.joinToString(", ")) }

            val defaultCmd = LanguageTools.defaultVoiceCommands.find { it.id == cmd.id }
            val isCustomized = defaultCmd != null && (cmd.triggers != defaultCmd.triggers || cmd.enabled != defaultCmd.enabled || cmd.requireConfirmation != defaultCmd.requireConfirmation)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (!cmd.enabled) colors.hairline.copy(alpha = 0.5f) else if (isCustomized) colors.accent.copy(alpha = 0.5f) else colors.hairline,
                        RoundedCornerShape(2.dp)
                    )
                    .background(if (cmd.enabled) colors.surface else colors.surface.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                // Header Row with Title, Master Enable/Disable, and Accordion Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedCommandId = if (isExpanded) null else cmd.id
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = cmd.actionName,
                                fontFamily = DisplayFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (cmd.enabled) colors.textPrimary else colors.textMuted
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${cmd.triggers.size})",
                                fontFamily = MonoFont,
                                fontSize = 11.sp,
                                color = if (cmd.enabled) colors.accent else colors.textMuted
                            )
                        }
                        Text(
                            text = cmd.description,
                            fontFamily = MonoFont,
                            fontSize = 11.sp,
                            color = colors.textMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCustomized) {
                            IconButton(
                                onClick = {
                                    if (defaultCmd != null) {
                                        val updated = commands.map { if (it.id == cmd.id) defaultCmd else it }
                                        repo.updateSettings {
                                             it.copy(voiceCommandsJson = LanguageTools.commandsToJson(updated))
                                        }
                                        Toast.makeText(context, "Reset ${cmd.actionName}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset to default",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Turn On/Off Toggle for this command
                        InkBracketToggle(
                            checked = cmd.enabled,
                            onCheckedChange = { isEnabled ->
                                val updated = commands.map { if (it.id == cmd.id) it.copy(enabled = isEnabled) else it }
                                repo.updateSettings {
                                    it.copy(voiceCommandsJson = LanguageTools.commandsToJson(updated))
                                }
                            }
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = if (isExpanded) colors.accent else colors.textMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Per-Command Confirmation Toggle Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, colors.hairline.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                .background(colors.background.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Require Confirmation Popup • নিশ্চিতকরণ পপআপ",
                                    fontFamily = MonoFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (cmd.requireConfirmation) colors.accent else colors.textPrimary
                                )
                                Text(
                                    text = if (cmd.requireConfirmation) "সরাসরি প্রয়োগ না করে নিশ্চিতকরণ কার্ড দেখাবে" else "সরাসরি প্রয়োগ করবে (পপআপ ছাড়া)",
                                    fontFamily = MonoFont,
                                    fontSize = 10.sp,
                                    color = colors.textMuted
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            InkBracketToggle(
                                checked = cmd.requireConfirmation,
                                onCheckedChange = { isReq ->
                                    val updated = commands.map { if (it.id == cmd.id) it.copy(requireConfirmation = isReq) else it }
                                    repo.updateSettings {
                                        it.copy(voiceCommandsJson = LanguageTools.commandsToJson(updated))
                                    }
                                }
                            )
                        }

                        if (!cmd.enabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ এই কমান্ডটি বন্ধ রাখা হয়েছে — মুখে বললে সাধারণ টেক্সট হিসেবে লেখা হবে।",
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Active Triggers (Unlimited Chips)
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            cmd.triggers.forEach { trig ->
                                Row(
                                    modifier = Modifier
                                        .border(1.dp, colors.hairline, RoundedCornerShape(14.dp))
                                        .background(colors.background)
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = trig,
                                        fontFamily = MonoFont,
                                        fontSize = 11.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove trigger",
                                        tint = colors.textMuted,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable {
                                                val updatedTriggers = cmd.triggers.filter { it != trig }
                                                val updated = commands.map { if (it.id == cmd.id) it.copy(triggers = updatedTriggers) else it }
                                                repo.updateSettings {
                                                    it.copy(voiceCommandsJson = LanguageTools.commandsToJson(updated))
                                                }
                                            }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Add Trigger Input Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newTriggerInput,
                                onValueChange = { newTriggerInput = it },
                                placeholder = {
                                    Text(
                                        "Add trigger(s), e.g. বাক্য কাটো...",
                                        fontFamily = MonoFont,
                                        fontSize = 11.sp,
                                        color = colors.textMuted
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.hairline,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary,
                                    cursorColor = colors.accent
                                ),
                                shape = RoundedCornerShape(2.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = MonoFont,
                                    fontSize = 12.sp
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (newTriggerInput.isNotBlank()) {
                                        val additions = newTriggerInput
                                            .split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                        val merged = (cmd.triggers + additions).distinct()
                                        val updated = commands.map {
                                            if (it.id == cmd.id) it.copy(triggers = merged) else it
                                        }
                                        repo.updateSettings {
                                            it.copy(voiceCommandsJson = LanguageTools.commandsToJson(updated))
                                        }
                                        newTriggerInput = ""
                                        Toast.makeText(context, "Added triggers for ${cmd.actionName}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add",
                                    fontFamily = MonoFont,
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Collapsible Batch Edit
                        Row(
                            modifier = Modifier
                                .clickable { isBatchEditOpen = !isBatchEditOpen }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBatchEditOpen) "Hide batch text editor ▲" else "Batch edit comma-separated text ▼",
                                fontFamily = MonoFont,
                                fontSize = 10.sp,
                                color = colors.textMuted
                            )
                        }

                        if (isBatchEditOpen) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = batchEditText,
                                onValueChange = { batchEditText = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = colors.accent,
                                    unfocusedBorderColor = colors.hairline,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                shape = RoundedCornerShape(2.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = MonoFont, fontSize = 11.sp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = {
                                    val items = batchEditText
                                        .split(",")
                                        .map { it.trim() }
                                        .filter { it.isNotEmpty() }
                                    val updated = commands.map {
                                        if (it.id == cmd.id) it.copy(triggers = items) else it
                                    }
                                    repo.updateSettings {
                                        it.copy(voiceCommandsJson = LanguageTools.commandsToJson(updated))
                                    }
                                    Toast.makeText(context, "Saved all triggers", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(text = "Save Batch Text", fontFamily = MonoFont, fontSize = 11.sp, color = colors.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Station10Shizoku(settings: KothaSettings, repo: SettingsRepository) {
    val colors = LocalKothaColors.current
    val context = LocalContext.current
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    val isRunning by com.example.util.ShizukuManager.isServiceRunning.collectAsState()
    val hasPermission by com.example.util.ShizukuManager.hasPermission.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.example.util.ShizukuManager.checkStatus()
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Status Card
        SettingCard(
            title = "⚡ Shizoku & ADB Privileged Automation Hub",
            description = "Shizuku ফ্রেমওয়ার্কের মাধ্যমে Kotha কোনো ম্যানুয়াল কমান্ড টাইপ ছাড়াই সিস্টেম-লেভেল সুবিধায় সরাসরি রান করে। নিচের টগলগুলো অন করলে অ্যাপ নিজেই ব্যাকগ্রাউন্ডে ফিচারগুলো সক্রিয় করে নেয়।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Live Shizuku Status Banner
                val (badgeColor, statusTitle, statusSubtitle) = when {
                    isRunning && hasPermission -> Triple(
                        colors.accent,
                        "🟢 Shizuku Status: Active & Connected (UID 2000)",
                        "পারমিশন সক্রিয় রয়েছে। যেকোনো ফিচার টগল করলে সরাসরি ব্যাকগ্রাউন্ডে কাজ করবে।"
                    )
                    isRunning && !hasPermission -> Triple(
                        Color(0xFFE3A93B),
                        "🟡 Shizuku Status: Active (Permission Required)",
                        "Shizuku সার্ভিস চলছে কিন্তু পারমিশন দেওয়া হয়নি। নিচে বাটন চেপে পারমিশন দিন।"
                    )
                    else -> Triple(
                        colors.signalCrimson,
                        "🔴 Shizuku Status: Not Running / Not Detected",
                        "ডিভাইসে Shizuku বা Wireless ADB সার্ভিস চালু নেই। Shizuku অ্যাপ চালু থাকলে অটো-অ্যাক্টিভেশন কাজ করবে।"
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = statusTitle,
                            fontFamily = DisplayFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = badgeColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusSubtitle,
                            fontFamily = MonoFont,
                            fontSize = 11.sp,
                            color = colors.textPrimary,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRunning && !hasPermission) {
                                Button(
                                    onClick = {
                                        val requested = com.example.util.ShizukuManager.requestPermission()
                                        if (!requested) {
                                            Toast.makeText(context, "Shizuku প্রম্পট চালু করা যায়নি", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                                    shape = RoundedCornerShape(2.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Grant Shizuku Permission Now",
                                        fontFamily = MonoFont,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    com.example.util.ShizukuManager.checkStatus()
                                    Toast.makeText(context, "Shizuku স্ট্যাটাস যাচাই করা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "🔄 Check / Refresh Status",
                                    fontFamily = MonoFont,
                                    fontSize = 11.sp,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Feature 1: Zero-Click Setup
        SettingCard(
            title = "#01 Zero-Click Setup • এক ক্লিকে সম্পূর্ণ সেটআপ",
            description = "ম্যানুয়ালি কোনো সেটিংসে না গিয়ে Shizuku দিয়ে মাইক্রোফোন, ফ্লোটিং উইন্ডো এবং অ্যাক্সেসিবিলিটি সার্ভিস স্বয়ংক্রিয়ভাবে চালু করুন।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingToggleRow(
                    title = "Auto-Grant All Permissions (স্বয়ংক্রিয় অ্যাক্টিভেশন)",
                    description = "টগল অন করলে সঙ্গে সঙ্গে সবগুলো প্ল্যাটফর্ম পারমিশন প্রদান করে ব্রিজ সচল করবে।",
                    checked = settings.shizukuAutoSetupEnabled,
                    onCheckedChange = { isChecked ->
                        repo.updateSettings { it.copy(shizukuAutoSetupEnabled = isChecked) }
                        if (isChecked) {
                            coroutineScope.launch {
                                val result = com.example.util.ShizukuManager.applyZeroClickSetup(context)
                                if (result.isSuccess) {
                                    Toast.makeText(context, "সব পারমিশন ও অ্যাক্সেসিবিলিটি সার্ভিস স্বয়ংক্রিয়ভাবে অন হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Shizuku ত্রুটি: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val result = com.example.util.ShizukuManager.applyZeroClickSetup(context)
                            if (result.isSuccess) {
                                repo.updateSettings { it.copy(shizukuAutoSetupEnabled = true) }
                                Toast.makeText(context, "Zero-Click Setup সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Shizuku পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ Run Complete Zero-Click Setup Now",
                        fontFamily = MonoFont,
                        fontSize = 11.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }

        // Feature 2: Self-Healing Accessibility Service
        SettingToggleRow(
            title = "#02 Self-Healing Accessibility • স্বয়ংক্রিয় অ্যাক্সেসিবিলিটি রিস্টোর",
            description = "MIUI, HyperOS বা OneUI ব্যাকগ্রাউন্ডে অ্যাক্সেসিবিলিটি সার্ভিস বন্ধ করে দিলে Shizuku কোনো ম্যানুয়াল ইন্টারভেনশন ছাড়াই নিঃশব্দে সার্ভিস পুনরায় চালু করবে।",
            checked = settings.shizukuSelfHealingEnabled,
            onCheckedChange = { isChecked ->
                repo.updateSettings { it.copy(shizukuSelfHealingEnabled = isChecked) }
                if (isChecked) {
                    coroutineScope.launch {
                        com.example.util.ShizukuManager.healAccessibility(context)
                        Toast.makeText(context, "স্বয়ংক্রিয় অ্যাক্সেসিবিলিটি রিস্টোর সক্রিয়", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )

        // Feature 4: Hands-Free Send Command
        SettingToggleRow(
            title = "#04 Hands-Free \"Send\" • ভয়েস কমান্ডে সরাসরি মেসেজ প্রেরণ",
            description = "মুখে 'পাঠাও' বা 'Send' বললে সাধারণ অ্যাক্সেসিবিলিটি বাটন না পেলে Shizuku সরাসরি হার্ডওয়্যার KEYCODE_ENTER (৬৬) পাঠিয়ে যেকোনো চ্যাট ও মেসেজিং অ্যাপে মেসেজ সেন্ড করে।",
            checked = settings.shizukuHandsFreeSendEnabled,
            onCheckedChange = { isChecked ->
                repo.updateSettings { it.copy(shizukuHandsFreeSendEnabled = isChecked) }
                Toast.makeText(context, if (isChecked) "হ্যান্ডস-ফ্রি সেন্ড সক্রিয়" else "হ্যান্ডস-ফ্রি সেন্ড নিষ্ক্রিয়", Toast.LENGTH_SHORT).show()
            }
        )

        // Feature 5: High-Precision Cursor Navigation & Macros
        SettingToggleRow(
            title = "#05 Cursor Navigation & Macros • হার্ডওয়্যার কিবোর্ড ম্যাক্রো",
            description = "টার্মিনাল (Termux), ওয়েবভিউ বা কোড এডিটরে সরাসরি হার্ডওয়্যার কী ইভেন্ট (Home, End, Ctrl+Del) পাঠিয়ে দ্রুতগতিতে কার্সার মুভ এবং আগের শব্দ মুছে ফেলে।",
            checked = settings.shizukuCursorNavEnabled,
            onCheckedChange = { isChecked ->
                repo.updateSettings { it.copy(shizukuCursorNavEnabled = isChecked) }
                Toast.makeText(context, if (isChecked) "কার্সার ম্যাক্রো সক্রিয়" else "কার্সার ম্যাক্রো নিষ্ক্রিয়", Toast.LENGTH_SHORT).show()
            }
        )

        // Feature 7: Background Clipboard Bypass
        SettingToggleRow(
            title = "#07 Background Clipboard Bypass • ব্যাকগ্রাউন্ড ক্লিপবোর্ড বাইপাস",
            description = "অ্যান্ড্রয়েড ১০+ ব্যাকগ্রাউন্ড ক্লিপবোর্ড সীমাবদ্ধতা বাইপাস করে Shizuku-এর মাধ্যমে হার্ডেন্ড বা সুরক্ষিত ইনপুট ফিল্ডে সরাসরি পেস্ট সুবিধা নিশ্চিত করে।",
            checked = settings.shizukuClipboardBypassEnabled,
            onCheckedChange = { isChecked ->
                repo.updateSettings { it.copy(shizukuClipboardBypassEnabled = isChecked) }
                Toast.makeText(context, if (isChecked) "ক্লিপবোর্ড বাইপাস সক্রিয়" else "ক্লিপবোর্ড বাইপাস নিষ্ক্রিয়", Toast.LENGTH_SHORT).show()
            }
        )

        // Feature 10: Battery Saver & Doze Whitelist
        SettingToggleRow(
            title = "#10 Battery Saver & Doze Whitelist • একটানা ব্যাকগ্রাউন্ড ডিক্টেশন",
            description = "স্ক্রিন অফ হলেও বা ফোন ডিপ স্লিপে গেলেও ডিক্টেশন যাতে ড্রপ না করে, সেজন্য Kotha-কে সিস্টেমের ডুয়াল ব্যাটারি ও ব্যাকগ্রাউন্ড রান হোয়াইটলিস্টে যুক্ত করে।",
            checked = settings.shizukuDozeWhitelistEnabled,
            onCheckedChange = { isChecked ->
                repo.updateSettings { it.copy(shizukuDozeWhitelistEnabled = isChecked) }
                if (isChecked) {
                    coroutineScope.launch {
                        val res = com.example.util.ShizukuManager.whitelistBattery(context)
                        if (res.isSuccess) {
                            Toast.makeText(context, "ব্যাটারি অপ্টিমাইজেশন ছাড় সফলভাবে প্রয়োগ হয়েছে!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )

        // Feature 13: Speech Recognition Engine Recovery
        SettingCard(
            title = "#13 Speech Engine Recovery • স্পিচ ইঞ্জিন রিস্টার্ট ও ক্যাশ রিকভারি",
            description = "গুগল স্পিচ সার্ভিস বা অ্যান্ড্রয়েড ভয়েস রিকগনাইজার হ্যাং বা 'ERROR_CLIENT' লুপে আটকে গেলে Shizuku সার্ভিসটি রিস্টার্ট করে ফ্রেশ সেশন চালু করে।"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingToggleRow(
                    title = "Auto-Restart on Freeze (হ্যাং হলে অটো রিস্টার্ট)",
                    description = "স্পিচ ইঞ্জিন বারবার ফেইল করলে অটোমেটিক ব্যাকগ্রাউন্ড কিল ও রিস্টার্ট চালাবে।",
                    checked = settings.shizukuSttRecoveryEnabled,
                    onCheckedChange = { isChecked ->
                        repo.updateSettings { it.copy(shizukuSttRecoveryEnabled = isChecked) }
                    }
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            val res = com.example.util.ShizukuManager.recoverSttEngine()
                            if (res.isSuccess) {
                                Toast.makeText(context, "গুগল স্পিচ ইঞ্জিন রিস্টার্ট সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "রিকভারি ব্যর্থ: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.hairline, RoundedCornerShape(2.dp)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🔄 Force-Restart & Recover STT Engine Now",
                        fontFamily = MonoFont,
                        fontSize = 11.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}
