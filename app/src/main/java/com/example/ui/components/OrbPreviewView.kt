package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.settings.CaptionBubbleSize
import com.example.settings.CaptionBubbleStyle
import com.example.settings.KothaSettings
import com.example.settings.OrbShape
import com.example.settings.RingStyle
import com.example.settings.UndoBadgeLabelMode
import com.example.settings.UndoBadgeSize
import com.example.settings.UndoBadgeStyle
import com.example.ui.theme.DisplayFont
import com.example.ui.theme.LocalKothaColors
import com.example.ui.theme.MonoFont
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class PreviewOrbState {
    IDLE,
    LISTENING,
    WAITING,
    ERROR
}

@Composable
fun OrbPreviewView(
    settings: KothaSettings,
    modifier: Modifier = Modifier
) {
    val colors = LocalKothaColors.current
    val haptic = LocalHapticFeedback.current
    var previewState by remember { mutableStateOf(PreviewOrbState.LISTENING) }
    var isSecondaryLanguage by remember { mutableStateOf(settings.isSecondaryLanguageActive) }
    var samplePhraseIndex by remember { mutableIntStateOf(0) }
    var displayedScriptText by remember { mutableStateOf("") }
    var showCursor by remember { mutableStateOf(true) }
    var undoTappedCount by remember { mutableIntStateOf(0) }

    // Sample phrases reflecting authentic Bangla & English live speech transcription
    val banglaSamples = remember {
        listOf(
            "কথা বলে সহজেই দ্রুত টাইপ করুন...",
            "আমার সোনার বাংলা আমি তোমায় ভালোবাসি।",
            "আজকের আবহাওয়া কেমন থাকবে এবং বৃষ্টি হবে কি না?",
            "বাংলা কণ্ঠস্বর শনাক্তকরণ খুব নির্ভুল কাজ করে।"
        )
    }

    val englishSamples = remember {
        listOf(
            "Live continuous speech transcription in real time...",
            "Speak naturally and words insert instantly at the cursor.",
            "Switch languages on the fly with a single tap.",
            "Self-healing voice engine with zero dropped words."
        )
    }

    val activeSamples = if (isSecondaryLanguage) englishSamples else banglaSamples

    // Infinite pulse animations for Orb breathing and Bipping Status Dot
    val transition = rememberInfiniteTransition(label = "orbStagePulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Dot pulse animation (650ms reverse loop matching CaptionBubbleOverlay)
    val dotPulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulseAlpha"
    )

    // Blinking cursor effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            showCursor = !showCursor
        }
    }

    // Typewriter / Live streaming script animation in listening state
    LaunchedEffect(previewState, isSecondaryLanguage, samplePhraseIndex, undoTappedCount) {
        if (previewState == PreviewOrbState.LISTENING) {
            val fullText = activeSamples[samplePhraseIndex % activeSamples.size]
            displayedScriptText = ""
            for (i in 1..fullText.length) {
                displayedScriptText = fullText.substring(0, i)
                val char = fullText[i - 1]
                val delayTime = when {
                    char == ' ' -> 70L
                    char == '।' || char == '.' || char == '?' -> 320L
                    char == ',' -> 160L
                    else -> 42L
                }
                delay(delayTime)
            }
            // Pause on completed sentence
            delay(2800)
            // Cycle to next sample phrase
            samplePhraseIndex = (samplePhraseIndex + 1) % activeSamples.size
        } else {
            displayedScriptText = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.hairline, RoundedCornerShape(2.dp))
            .background(colors.surface)
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Specimen Metadata Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (previewState == PreviewOrbState.LISTENING) colors.signalVerdigris else colors.accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STAGE • LIVE INTERFACE",
                    fontFamily = MonoFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = colors.textPrimary
                )
            }
            Text(
                text = "${settings.orbSizeDp} DP • ${settings.orbShape.name} • ${if (isSecondaryLanguage) "EN" else "বাং"}",
                fontFamily = MonoFont,
                fontSize = 10.5.sp,
                color = colors.accent
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Stage Workbench Canvas (Displaying Caption Bubble + Language Indicator + Orb Button)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(colors.background)
                .border(1.dp, colors.hairline.copy(alpha = 0.5f), RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Technical millimeter grid and center coordinate marks
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridSpacing = 20.dp.toPx()
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = colors.hairline.copy(alpha = 0.28f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 0.7f
                    )
                    x += gridSpacing
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = colors.hairline.copy(alpha = 0.28f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 0.7f
                    )
                    y += gridSpacing
                }

                // Stage alignment reticle crosshairs
                val cx = size.width / 2f
                val cy = size.height * 0.65f
                drawLine(
                    color = colors.hairline.copy(alpha = 0.5f),
                    start = Offset(cx - 24f, cy),
                    end = Offset(cx + 24f, cy),
                    strokeWidth = 1f
                )
                drawLine(
                    color = colors.hairline.copy(alpha = 0.5f),
                    start = Offset(cx, cy - 24f),
                    end = Offset(cx, cy + 24f),
                    strokeWidth = 1f
                )
            }

            // Live Composables: Caption Bubble Overlay Pill + Dictation Cluster
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. ACTIVE LIVE TRANSCRIPTION CAPTION BUBBLE PILL
                ActiveCaptionBubblePreview(
                    settings = settings,
                    previewState = previewState,
                    displayedText = displayedScriptText,
                    showCursor = showCursor,
                    dotPulseAlpha = dotPulseAlpha
                )

                Spacer(modifier = Modifier.weight(1f))

                // 2. ORB + LANGUAGE SWITCHER + HORIZONTAL UNDO BUTTON OVERLAY CLUSTER
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Language-Switching Button Floating Above Orb (Exactly like LanguageIndicatorOverlay)
                        LanguageSwitchButton(
                            isSecondary = isSecondaryLanguage,
                            primaryCode = settings.primaryLanguage,
                            secondaryCode = settings.secondaryLanguage,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isSecondaryLanguage = !isSecondaryLanguage
                            }
                        )

                        Spacer(modifier = Modifier.height((settings.voiceButtonVerticalOffsetDp.coerceIn(4, 16)).dp))

                        // Orb and Undo Button placed on the exact same horizontal "X" axis (side by side)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Dedicated Floating Undo Button beside the Orb
                            androidx.compose.animation.AnimatedVisibility(
                                visible = settings.tapCaptionToUndo && (previewState == PreviewOrbState.LISTENING || previewState == PreviewOrbState.WAITING),
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    UndoBadgePreview(
                                        settings = settings,
                                        dotPulseAlpha = dotPulseAlpha,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            undoTappedCount++
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(settings.undoButtonHorizontalGapDp.coerceIn(4, 32).dp))
                                }
                            }

                            // Live Interactive Floating Orb Button
                            InteractiveOrbCanvas(
                                settings = settings,
                                previewState = previewState,
                                pulse = pulse,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    previewState = if (previewState == PreviewOrbState.LISTENING) {
                                        PreviewOrbState.IDLE
                                    } else {
                                        PreviewOrbState.LISTENING
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State Switcher Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreviewOrbState.values().forEach { state ->
                val isSelected = state == previewState
                val stateColor = when (state) {
                    PreviewOrbState.IDLE -> colors.textMuted
                    PreviewOrbState.LISTENING -> colors.signalVerdigris
                    PreviewOrbState.WAITING -> Color(0xFF2196F3)
                    PreviewOrbState.ERROR -> Color(0xFFE53935)
                }

                Box(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = if (isSelected) stateColor else colors.hairline,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .background(if (isSelected) stateColor.copy(alpha = 0.16f) else colors.surface)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            previewState = state
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(stateColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = when (state) {
                                PreviewOrbState.IDLE -> "idle"
                                PreviewOrbState.LISTENING -> "listening"
                                PreviewOrbState.WAITING -> "restarting"
                                PreviewOrbState.ERROR -> "error"
                            },
                            fontFamily = MonoFont,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) stateColor else colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Preview component of the Floating Live Caption Bubble with dynamic Bipping dot,
 * live speech text streaming animation, and configurable Undo badge.
 */
@Composable
private fun ActiveCaptionBubblePreview(
    settings: KothaSettings,
    previewState: PreviewOrbState,
    displayedText: String,
    showCursor: Boolean,
    dotPulseAlpha: Float
) {
    val primaryColor = Color(settings.listeningColor)

    // Dynamic border color depending on current speech state
    val borderColor = when (previewState) {
        PreviewOrbState.LISTENING -> primaryColor.copy(alpha = 0.65f)
        PreviewOrbState.WAITING -> Color(0xFF2196F3).copy(alpha = 0.65f)
        PreviewOrbState.ERROR -> Color(0xFFE53935).copy(alpha = 0.65f)
        PreviewOrbState.IDLE -> Color(0xFFECE6DA).copy(alpha = 0.22f)
    }

    // Pill background matching settings
    val pillBgColor = when (settings.captionBubbleStyle) {
        CaptionBubbleStyle.TRANSLUCENT_DARK -> Color(0xF21C1A18)
        CaptionBubbleStyle.SOLID_AMOLED -> Color(0xFF000000)
        CaptionBubbleStyle.ACCENT_THEMED -> Color(0xF21C1A18).copy(alpha = 0.90f)
    }

    // Bipping dot color & alpha
    val (dotColor, dotAlpha) = when (previewState) {
        PreviewOrbState.LISTENING -> primaryColor to (if (displayedText.isNotBlank()) 1.0f else dotPulseAlpha)
        PreviewOrbState.WAITING -> Color(0xFF2196F3) to dotPulseAlpha
        PreviewOrbState.ERROR -> Color(0xFFE53935) to 1.0f
        PreviewOrbState.IDLE -> Color(0xFF8C857A) to 0.45f
    }

    // Caption status prompt / text
    val isSpokenText = displayedText.isNotBlank() && previewState == PreviewOrbState.LISTENING
    val captionDisplay = when {
        isSpokenText -> displayedText + (if (showCursor) " ▎" else "")
        previewState == PreviewOrbState.LISTENING -> "Listening... speak now"
        previewState == PreviewOrbState.WAITING -> "Restarting engine... hold on"
        previewState == PreviewOrbState.ERROR -> "Reconnecting audio stream..."
        else -> "Tap orb to begin dictating"
    }

    val textSizeSp = when (settings.captionBubbleSize) {
        CaptionBubbleSize.COMPACT -> 12.5.sp
        CaptionBubbleSize.MEDIUM -> 13.5.sp
        CaptionBubbleSize.LARGE -> 14.5.sp
        CaptionBubbleSize.EXTRA_LARGE -> 16.sp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(min = 180.dp, max = 500.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
            .background(pillBgColor, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Bipping Status Indicator Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = dotAlpha))
                )

                // Live Script Text
                Text(
                    text = captionDisplay,
                    fontSize = textSizeSp,
                    fontStyle = if (isSpokenText) FontStyle.Normal else FontStyle.Italic,
                    fontWeight = if (isSpokenText) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSpokenText) Color.White else Color(0xDDECE6DA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Renders the Undo Badge exactly as styled in KothaSettings.
 */
@Composable
private fun UndoBadgePreview(
    settings: KothaSettings,
    dotPulseAlpha: Float,
    onClick: () -> Unit
) {
    val themeAccent = if (settings.undoBadgeCustomColor != 0L) {
        Color(settings.undoBadgeCustomColor)
    } else {
        Color(settings.listeningColor)
    }

    val label = when (settings.undoBadgeLabelMode) {
        UndoBadgeLabelMode.ICON_AND_TEXT -> "↶ Undo"
        UndoBadgeLabelMode.ICON_ONLY -> "↶"
        UndoBadgeLabelMode.TEXT_ONLY -> "UNDO"
    }

    val (heightDp, padHDp, textSp) = when (settings.undoBadgeSize) {
        UndoBadgeSize.COMPACT -> Triple(20.dp, 6.dp, 10.sp)
        UndoBadgeSize.STANDARD -> Triple(23.dp, 8.dp, 11.sp)
        UndoBadgeSize.PROMINENT -> Triple(26.dp, 10.dp, 12.sp)
    }

    val (bgColor, borderColor, textColor) = when (settings.undoBadgeStyle) {
        UndoBadgeStyle.FILLED_ACCENT -> Triple(themeAccent, themeAccent.copy(alpha = 0.8f), Color.White)
        UndoBadgeStyle.CRIMSON_DANGER -> Triple(Color(0xFFD32F2F), Color(0xFFFF5252), Color.White)
        UndoBadgeStyle.AMBER_GLOW -> Triple(Color(0xFFFFB300), Color(0xFFFFD54F), Color(0xFF121212))
        UndoBadgeStyle.MONOCHROME_INVERSE -> Triple(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFF121212))
        UndoBadgeStyle.OUTLINED_NEON -> Triple(themeAccent.copy(alpha = 0.22f), themeAccent, themeAccent)
        UndoBadgeStyle.GLASSMORPHISM -> Triple(Color(0x38FFFFFF), Color(0x66FFFFFF), Color.White)
        UndoBadgeStyle.SOLID_AMOLED_BLACK -> Triple(Color(0xFF000000), Color(0xFF2A2A2A), Color(0xFFE0E0E0))
        UndoBadgeStyle.TRANSPARENT_BORDER -> Triple(Color.Transparent, themeAccent, themeAccent)
    }

    val finalAlpha = (if (settings.undoBadgePulse) (0.85f + 0.15f * dotPulseAlpha) else 1.0f) * settings.undoButtonOpacity

    Box(
        modifier = Modifier
            .height(heightDp)
            .clip(RoundedCornerShape(heightDp / 2))
            .background(bgColor.copy(alpha = finalAlpha))
            .border(1.2.dp, borderColor, RoundedCornerShape(heightDp / 2))
            .clickable(onClick = onClick)
            .padding(horizontal = padHDp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = textSp,
            fontFamily = MonoFont,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

/**
 * Floating Language Switcher button replicating LanguageIndicatorOverlay.
 */
@Composable
private fun LanguageSwitchButton(
    isSecondary: Boolean,
    primaryCode: String,
    secondaryCode: String,
    onClick: () -> Unit
) {
    val activeCode = if (isSecondary) secondaryCode else primaryCode
    val label = when {
        activeCode.lowercase().startsWith("bn") -> "বাং"
        activeCode.lowercase().startsWith("en") -> "EN"
        activeCode.lowercase().startsWith("hi") -> "HI"
        activeCode.lowercase().startsWith("ur") -> "UR"
        activeCode.lowercase().startsWith("ar") -> "AR"
        else -> activeCode.substringBefore("-").uppercase()
    }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0x55121212))
            .border(1.3.dp, Color(0x66FFFFFF), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (label == "বাং") 11.sp else 10.sp,
            fontFamily = if (label == "বাং") DisplayFont else MonoFont,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

/**
 * Interactive canvas rendering the configured floating Orb with shape, rings, and glyph.
 */
@Composable
private fun InteractiveOrbCanvas(
    settings: KothaSettings,
    previewState: PreviewOrbState,
    pulse: Float,
    onClick: () -> Unit
) {
    // Sized with scaled bounds for compact, responsive stage box display
    val displaySizeDp = (settings.orbSizeDp * 0.85f).coerceIn(44f, 68f).dp

    Box(
        modifier = Modifier
            .size(displaySizeDp + 16.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(displaySizeDp + 14.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val baseRadius = (displaySizeDp.toPx() / 2f) * 0.82f
            val activeRadius = if (previewState == PreviewOrbState.IDLE) {
                baseRadius + (pulse * 2.5f)
            } else {
                baseRadius
            }

            val orbColor = when (previewState) {
                PreviewOrbState.IDLE -> Color(settings.idleColor)
                PreviewOrbState.LISTENING -> Color(settings.listeningColor)
                PreviewOrbState.WAITING -> Color(settings.waitingColor)
                PreviewOrbState.ERROR -> Color(settings.errorColor)
            }

            // Render configured Orb shape
            when (settings.orbShape) {
                OrbShape.CIRCLE -> {
                    drawCircle(color = orbColor, radius = activeRadius, center = Offset(cx, cy))
                }
                OrbShape.SQUIRCLE -> {
                    drawRoundRect(
                        color = orbColor,
                        topLeft = Offset(cx - activeRadius, cy - activeRadius),
                        size = Size(activeRadius * 2, activeRadius * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(activeRadius * 0.6f)
                    )
                }
                OrbShape.ROUNDED_SQUARE -> {
                    drawRoundRect(
                        color = orbColor,
                        topLeft = Offset(cx - activeRadius, cy - activeRadius),
                        size = Size(activeRadius * 2, activeRadius * 2),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(activeRadius * 0.28f)
                    )
                }
                OrbShape.SEAL -> {
                    val path = Path()
                    val points = 12
                    val innerR = activeRadius * 0.88f
                    for (i in 0 until points * 2) {
                        val r = if (i % 2 == 0) activeRadius else innerR
                        val angle = Math.toRadians((i * 360.0 / (points * 2)))
                        val px = cx + (r * cos(angle)).toFloat()
                        val py = cy + (r * sin(angle)).toFloat()
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, orbColor)
                }
            }

            // Ring styling
            val ringRadius = activeRadius + 5f
            val ringColor = Color(settings.ringColor)
            val strokeW = (settings.outlineThicknessDp * 1.8f).coerceAtLeast(1.5f)

            if (previewState == PreviewOrbState.LISTENING) {
                val tickCount = 20
                val spread = 360f / tickCount
                for (i in 0 until tickCount) {
                    val deg = i * spread
                    if (deg in 285f..320f) continue // 1 o'clock gap
                    val rad = Math.toRadians(deg.toDouble())
                    val tickLen = 3.5f + (pulse * 7f)
                    val x1 = cx + (ringRadius * cos(rad)).toFloat()
                    val y1 = cy + (ringRadius * sin(rad)).toFloat()
                    val x2 = cx + ((ringRadius + tickLen) * cos(rad)).toFloat()
                    val y2 = cy + ((ringRadius + tickLen) * sin(rad)).toFloat()
                    drawLine(
                        color = ringColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            } else if (previewState == PreviewOrbState.WAITING) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 270f * pulse,
                    useCenter = false,
                    topLeft = Offset(cx - ringRadius, cy - ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = strokeW)
                )
            } else if (previewState == PreviewOrbState.ERROR) {
                drawArc(
                    color = ringColor,
                    startAngle = 30f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(cx - ringRadius, cy - ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = strokeW)
                )
            } else if (settings.ringStyle != RingStyle.NONE) {
                drawCircle(
                    color = ringColor,
                    radius = ringRadius,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeW)
                )
            }

            // Center Microphone / recording glyph
            val iconColor = Color(settings.iconColor)
            if (previewState == PreviewOrbState.LISTENING) {
                drawCircle(color = iconColor, radius = activeRadius * 0.24f, center = Offset(cx, cy))
            } else {
                drawCircle(color = iconColor, radius = activeRadius * 0.17f, center = Offset(cx, cy))
            }
        }
    }
}
