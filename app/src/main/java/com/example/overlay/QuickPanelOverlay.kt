package com.example.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.settings.DictationMode
import com.example.settings.KothaSettings

class QuickPanelOverlay(
    context: Context,
    private val settings: KothaSettings,
    private val transcriptPeek: String,
    private val onSwitchLanguage: (String) -> Unit,
    private val onToggleMode: () -> Unit,
    private val onUndo: () -> Unit,
    private val onPause: () -> Unit,
    private val onHideOrb: () -> Unit,
    private val onOpenApp: () -> Unit,
    private val onDismiss: () -> Unit
) : LinearLayout(context) {

    init {
        orientation = VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)

        val bg = GradientDrawable().apply {
            setColor(0xF0171512.toInt())
            cornerRadius = 24f
            setStroke(2, 0xFF2A2722.toInt())
        }
        background = bg

        // Title Row
        val title = TextView(context).apply {
            text = "Kotha Quick Controls • কথা"
            setTextColor(0xFFECE6DA.toInt())
            textSize = 14f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        addView(title)

        // Transcript Peek
        if (transcriptPeek.isNotBlank()) {
            val transcriptView = TextView(context).apply {
                text = "“${transcriptPeek.take(60)}”"
                setTextColor(0xFF8C857A.toInt())
                textSize = 12f
                setPadding(0, 8, 0, 16)
            }
            addView(transcriptView)
        } else {
            val divider = View(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 2).apply {
                    setMargins(0, 12, 0, 12)
                }
                setBackgroundColor(0xFF2A2722.toInt())
            }
            addView(divider)
        }

        // Language Switch Row
        val langRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 4, 0, 8)
        }
        val btnPrimaryLang = createChipButton(
            label = "Primary: ${settings.primaryLanguage}",
            isSelected = !settings.isSecondaryLanguageActive
        ) {
            if (settings.isSecondaryLanguageActive) {
                onSwitchLanguage(settings.primaryLanguage)
            }
            onDismiss()
        }
        val btnSecondaryLang = createChipButton(
            label = "Secondary: ${settings.secondaryLanguage}",
            isSelected = settings.isSecondaryLanguageActive
        ) {
            if (!settings.isSecondaryLanguageActive) {
                onSwitchLanguage(settings.secondaryLanguage)
            }
            onDismiss()
        }
        langRow.addView(btnPrimaryLang)
        langRow.addView(btnSecondaryLang)
        addView(langRow)

        // Action Buttons Row 1: Mode Switch & Undo
        val actionRow1 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 4, 0, 8)
        }
        val modeLabel = if (settings.dictationMode == DictationMode.CONTINUOUS) "Mode: Continuous" else "Mode: Pause & Restart"
        val btnMode = createActionButton(modeLabel) {
            onToggleMode()
            onDismiss()
        }
        val btnUndo = createActionButton("Undo Last Utterance") {
            onUndo()
            onDismiss()
        }
        actionRow1.addView(btnMode)
        actionRow1.addView(btnUndo)
        addView(actionRow1)

        // Action Buttons Row 2: Pause, Hide, Open App
        val actionRow2 = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 4, 0, 4)
        }
        val btnPause = createActionButton("Pause") {
            onPause()
            onDismiss()
        }
        val btnHide = createActionButton("Hide Orb") {
            onHideOrb()
            onDismiss()
        }
        val btnOpen = createActionButton("Open Kotha") {
            onOpenApp()
            onDismiss()
        }
        actionRow2.addView(btnPause)
        actionRow2.addView(btnHide)
        actionRow2.addView(btnOpen)
        addView(actionRow2)

        // Dismiss Bar
        val closeBtn = TextView(context).apply {
            text = "[ Dismiss ]"
            gravity = Gravity.CENTER
            setTextColor(0xFF8C857A.toInt())
            textSize = 12f
            setPadding(0, 16, 0, 0)
            setOnClickListener { onDismiss() }
        }
        addView(closeBtn)
    }

    private fun createChipButton(label: String, isSelected: Boolean, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = label
            isAllCaps = false
            textSize = 12f
            setTextColor(if (isSelected) 0xFF0F0E0C.toInt() else 0xFFECE6DA.toInt())
            background = GradientDrawable().apply {
                setColor(if (isSelected) 0xFFF2542D.toInt() else 0xFF2A2722.toInt())
                cornerRadius = 16f
            }
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createActionButton(label: String, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = label
            isAllCaps = false
            textSize = 11f
            setTextColor(0xFFECE6DA.toInt())
            background = GradientDrawable().apply {
                setColor(0xFF221F1B.toInt())
                cornerRadius = 12f
                setStroke(1, 0xFF3D3830.toInt())
            }
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
            setOnClickListener { onClick() }
        }
    }
}
