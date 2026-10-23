package com.example.overlay

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class CommandConfirmOverlay(
    context: Context,
    private val commandName: String,
    private val rawSpokenText: String,
    private val onConfirm: () -> Unit,
    private val onInsertAsText: () -> Unit,
    private val onDismiss: () -> Unit
) : LinearLayout(context) {

    private val handler = Handler(Looper.getMainLooper())
    private var isResolved = false

    private val autoDismissRunnable = Runnable {
        if (!isResolved) {
            isResolved = true
            onInsertAsText()
            onDismiss()
        }
    }

    init {
        orientation = VERTICAL
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        setPadding(pad, pad, pad, pad)

        val bg = GradientDrawable().apply {
            setColor(0xF0171512.toInt())
            cornerRadius = 20f * density
            setStroke((1.5f * density).toInt(), 0xFFF2542D.toInt()) // Vermilion accent border
        }
        background = bg

        // Title Row
        val titleView = TextView(context).apply {
            text = "⚠️ Confirm Voice Action • নিশ্চিত করুন"
            setTextColor(0xFFE3A93B.toInt()) // Ochre warning
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        addView(titleView)

        // Command Description
        val cmdView = TextView(context).apply {
            text = commandName
            setTextColor(0xFFECE6DA.toInt())
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, (6 * density).toInt(), 0, (2 * density).toInt())
        }
        addView(cmdView)

        // Spoken phrase preview
        val spokenView = TextView(context).apply {
            text = "You said: “$rawSpokenText”"
            setTextColor(0xFF8C857A.toInt())
            textSize = 12f
            setPadding(0, 0, 0, (12 * density).toInt())
        }
        addView(spokenView)

        // Action Buttons Row
        val btnRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
        }

        val typeTextBtn = Button(context).apply {
            text = "Type as Text"
            setTextColor(0xFFECE6DA.toInt())
            textSize = 11f
            val btnBg = GradientDrawable().apply {
                setColor(0xFF26231E.toInt())
                cornerRadius = 8f * density
                setStroke((1f * density).toInt(), 0xFF3D3831.toInt())
            }
            background = btnBg
            setPadding((12 * density).toInt(), (4 * density).toInt(), (12 * density).toInt(), (4 * density).toInt())
            setOnClickListener {
                if (!isResolved) {
                    isResolved = true
                    handler.removeCallbacks(autoDismissRunnable)
                    onInsertAsText()
                    onDismiss()
                }
            }
        }
        btnRow.addView(typeTextBtn)

        val space = LinearLayout(context).apply {
            layoutParams = LayoutParams((8 * density).toInt(), LayoutParams.MATCH_PARENT)
        }
        btnRow.addView(space)

        val applyBtn = Button(context).apply {
            text = "Apply Action"
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 11f
            val btnBg = GradientDrawable().apply {
                setColor(0xFFF2542D.toInt()) // Vermilion
                cornerRadius = 8f * density
            }
            background = btnBg
            setPadding((14 * density).toInt(), (4 * density).toInt(), (14 * density).toInt(), (4 * density).toInt())
            setOnClickListener {
                if (!isResolved) {
                    isResolved = true
                    handler.removeCallbacks(autoDismissRunnable)
                    onConfirm()
                    onDismiss()
                }
            }
        }
        btnRow.addView(applyBtn)

        addView(btnRow)

        // Auto-dismiss after 6 seconds defaulting to inserting as text
        handler.postDelayed(autoDismissRunnable, 6000L)
    }

    fun dismiss() {
        handler.removeCallbacks(autoDismissRunnable)
        onDismiss()
    }
}
