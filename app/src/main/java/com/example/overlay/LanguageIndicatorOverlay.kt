package com.example.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.widget.TextView
import com.example.settings.KothaSettings

@SuppressLint("ViewConstructor")
class LanguageIndicatorOverlay(
    context: Context,
    private val onToggle: () -> Unit
) : TextView(context) {

    private var activeLanguageCode: String = "bn-BD"
    private var isSecondary: Boolean = false

    init {
        gravity = Gravity.CENTER
        textSize = 12f
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        setTextColor(0xFFFFFFFF.toInt())
        includeFontPadding = false

        val density = resources.displayMetrics.density
        val circleDim = (36 * density).toInt()
        minWidth = circleDim
        minHeight = circleDim
        width = circleDim
        height = circleDim

        // Circular button matching the sketch
        val normalBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x40121212.toInt()) // Soft translucent dark background
            setStroke((1.5f * density).toInt().coerceAtLeast(1), 0x40FFFFFF.toInt()) // Crisp subtle hairline border
        }
        val pressedBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(0x80121212.toInt())
            setStroke((1.5f * density).toInt().coerceAtLeast(1), 0x70FFFFFF.toInt())
        }
        val stateList = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressedBg)
            addState(intArrayOf(), normalBg)
        }
        background = stateList

        isClickable = true
        isFocusable = false

        setOnClickListener {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            onToggle()
        }

        updateBadgeText()
    }

    fun updateSettings(settings: KothaSettings) {
        isSecondary = settings.isSecondaryLanguageActive
        activeLanguageCode = if (isSecondary) {
            settings.secondaryLanguage
        } else {
            if (settings.customBcp47.isNotBlank()) settings.customBcp47 else settings.primaryLanguage
        }
        updateBadgeText()
    }

    private fun updateBadgeText() {
        val lower = activeLanguageCode.lowercase()
        val label = when {
            lower.startsWith("bn") -> "বাং"
            lower.startsWith("en") -> "EN"
            lower.startsWith("hi") -> "HI"
            lower.startsWith("ur") -> "UR"
            lower.startsWith("ar") -> "AR"
            lower.startsWith("es") -> "ES"
            lower.startsWith("fr") -> "FR"
            lower.startsWith("de") -> "DE"
            lower.startsWith("ja") -> "JA"
            lower.startsWith("zh") -> "ZH"
            lower.startsWith("ko") -> "KO"
            else -> activeLanguageCode.substringBefore("-").uppercase()
        }
        text = label
    }
}
