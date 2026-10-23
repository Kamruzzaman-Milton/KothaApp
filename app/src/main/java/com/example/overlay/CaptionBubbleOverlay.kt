package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.graphics.ColorUtils
import com.example.engine.DictationState
import com.example.settings.CaptionBubbleSize
import com.example.settings.CaptionBubbleStyle
import com.example.settings.KothaSettings
import kotlin.math.max
import kotlin.math.min

@SuppressLint("ViewConstructor")
class CaptionBubbleOverlay(
    context: Context,
    var settings: KothaSettings
) : View(context) {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }

    private val cardRect = RectF()
    private val cornerRadiusDp = 18f

    var onLayoutChanged: (() -> Unit)? = null

    var dictationState: DictationState = DictationState.Idle
        private set

    var partialText: String = ""
        private set

    var finalUtterance: String = ""
        private set

    private var staticLayout: StaticLayout? = null
    private var fadeAnimator: ValueAnimator? = null
    private val clearFinalRunnable = Runnable {
        finalUtterance = ""
        rebuildLayout()
        requestLayout()
        postInvalidate()
        onLayoutChanged?.invoke()
    }

    // Bipping dot pulsing animation: 0.35f <-> 1.0f in 650ms reverse loop
    private var dotPulseAlpha = 1.0f
    private val dotPulseAnimator = ValueAnimator.ofFloat(0.35f, 1.0f).apply {
        duration = 650L
        repeatMode = ValueAnimator.REVERSE
        repeatCount = ValueAnimator.INFINITE
        interpolator = AccelerateDecelerateInterpolator()
        addUpdateListener {
            dotPulseAlpha = it.animatedValue as Float
            postInvalidate()
        }
    }

    init {
        visibility = INVISIBLE
        alpha = 0f
        updateStyles()
        dotPulseAnimator.start()
    }

    override fun onDetachedFromWindow() {
        dotPulseAnimator.cancel()
        fadeAnimator?.cancel()
        removeCallbacks(clearFinalRunnable)
        super.onDetachedFromWindow()
    }

    fun updateStyles() {
        val density = resources.displayMetrics.density

        val spSize = when (settings.captionBubbleSize) {
            CaptionBubbleSize.COMPACT -> 14f
            CaptionBubbleSize.MEDIUM -> 15f
            CaptionBubbleSize.LARGE -> 18f
            CaptionBubbleSize.EXTRA_LARGE -> 20f
        }
        textPaint.textSize = spSize * density
        borderPaint.strokeWidth = 1.5f * density

        rebuildLayout()
        requestLayout()
        postInvalidate()
    }

    fun setDictationState(state: DictationState) {
        val previousState = dictationState
        dictationState = state

        if (state !is DictationState.Idle) {
            fadeAnimator?.cancel()
            alpha = 1.0f
            visibility = VISIBLE
        } else {
            // Dictation ended: fade out pill
            partialText = ""
            finalUtterance = ""
            removeCallbacks(clearFinalRunnable)
            startFadeOut()
            return
        }

        rebuildLayout()
        requestLayout()
        postInvalidate()
        onLayoutChanged?.invoke()
    }

    fun setPartialText(text: String) {
        removeCallbacks(clearFinalRunnable)
        val trimmed = text.trim()
        if (partialText != trimmed) {
            partialText = trimmed
            if (trimmed.isNotBlank()) {
                finalUtterance = ""
            }
            if (dictationState !is DictationState.Idle) {
                fadeAnimator?.cancel()
                alpha = 1.0f
                visibility = VISIBLE
            }
            rebuildLayout()
            requestLayout()
            postInvalidate()
            onLayoutChanged?.invoke()
        }
    }

    fun setFinalUtterance(text: String) {
        removeCallbacks(clearFinalRunnable)
        partialText = ""
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            finalUtterance = trimmed
            if (dictationState !is DictationState.Idle) {
                fadeAnimator?.cancel()
                alpha = 1.0f
                visibility = VISIBLE
            }
            rebuildLayout()
            requestLayout()
            postInvalidate()
            onLayoutChanged?.invoke()

            // Keep final committed text visible briefly before returning to gap prompt
            val lingerMs = settings.captionLingerDurationMs.coerceIn(800L, 4000L)
            postDelayed(clearFinalRunnable, lingerMs)
        }
    }

    fun clearAll() {
        removeCallbacks(clearFinalRunnable)
        partialText = ""
        finalUtterance = ""
        visibility = INVISIBLE
        alpha = 0f
    }

    private fun startFadeOut() {
        fadeAnimator?.cancel()
        fadeAnimator = ValueAnimator.ofFloat(alpha, 0f).apply {
            duration = 250L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val a = it.animatedValue as Float
                alpha = a
                if (a <= 0.02f) {
                    visibility = INVISIBLE
                    rebuildLayout()
                }
            }
        }
        fadeAnimator?.start()
    }

    private fun getDisplayText(): Pair<String, Boolean> {
        // Returns Pair(text, isSpokenText)
        return when {
            partialText.isNotBlank() -> Pair(partialText, true)
            finalUtterance.isNotBlank() -> Pair(finalUtterance, true)
            dictationState is DictationState.Listening -> Pair("Listening... speak now", false)
            dictationState is DictationState.Starting -> Pair("Starting engine...", false)
            dictationState is DictationState.Restarting -> Pair("Restarting... hold on", false)
            dictationState is DictationState.WaitingRestart -> Pair("Restarting... hold on", false)
            dictationState is DictationState.Processing -> Pair("Processing speech...", false)
            dictationState is DictationState.Paused -> Pair("Dictation paused", false)
            dictationState is DictationState.Error -> Pair("Reconnecting...", false)
            else -> Pair("Listening...", false)
        }
    }

    private fun rebuildLayout() {
        if (dictationState is DictationState.Idle && visibility != VISIBLE) {
            staticLayout = null
            return
        }

        val (displayStr, isSpoken) = getDisplayText()

        if (isSpoken) {
            textPaint.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textPaint.color = 0xFFFFFFFF.toInt()
        } else {
            textPaint.typeface = Typeface.create("sans-serif", Typeface.ITALIC)
            textPaint.color = 0xDDECE6DA.toInt()
        }

        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val maxAvailableWidth = (screenWidth - (48 * density).toInt()).coerceAtLeast(180)
        val maxCardWidth = min(maxAvailableWidth, (360 * density).toInt())
        val horizontalPad = (48 * density).toInt() // Pad + Dot + DotGap
        val textWidth = max((80 * density).toInt(), maxCardWidth - horizontalPad)

        val maxLines = if (isSpoken) settings.captionMaxLines.coerceIn(2, 4) else 2

        staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(displayStr, 0, displayStr.length, textPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(maxLines)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(
                displayStr,
                textPaint,
                textWidth,
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0f,
                false
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val layout = staticLayout
        if (layout == null || (dictationState is DictationState.Idle && visibility != VISIBLE)) {
            setMeasuredDimension(0, 0)
            return
        }

        val padX = (14 * density)
        val padY = (10 * density)
        val dotRadius = (5 * density) // 10dp dot
        val dotGap = (10 * density)

        val contentWidth = layout.width.toFloat().coerceAtMost(maxLineWidth(layout))
        val totalWidth = contentWidth + padX * 2 + dotRadius * 2 + dotGap
        val totalHeight = layout.height + padY * 2

        val minWidth = (140 * density).toInt()
        val minHeight = (38 * density).toInt()

        setMeasuredDimension(
            totalWidth.toInt().coerceAtLeast(minWidth),
            totalHeight.toInt().coerceAtLeast(minHeight)
        )
    }

    private fun maxLineWidth(layout: StaticLayout): Float {
        var maxW = 0f
        for (i in 0 until layout.lineCount) {
            maxW = max(maxW, layout.getLineWidth(i))
        }
        return maxW
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = staticLayout ?: return
        if (dictationState is DictationState.Idle && visibility != VISIBLE) return

        val density = resources.displayMetrics.density
        val cornerRadius = cornerRadiusDp * density

        cardRect.set(1.5f, 1.5f, width.toFloat() - 1.5f, height.toFloat() - 1.5f)

        val primaryColor = settings.listeningColor.toInt()
        val hasPartial = partialText.isNotBlank()

        // 1. Dynamic border color based on dictation state & activity
        val borderColor = when {
            hasPartial || dictationState is DictationState.Listening -> ColorUtils.setAlphaComponent(primaryColor, 150)
            dictationState is DictationState.Starting || dictationState is DictationState.Restarting || dictationState is DictationState.WaitingRestart -> ColorUtils.setAlphaComponent(0xFF2196F3.toInt(), 160)
            dictationState is DictationState.Paused -> ColorUtils.setAlphaComponent(0xFFFFB300.toInt(), 160)
            dictationState is DictationState.Error -> ColorUtils.setAlphaComponent(0xFFE53935.toInt(), 160)
            else -> 0x44ECE6DA.toInt()
        }
        borderPaint.color = borderColor

        // 2. Card background: dark surface variant with 95% opacity
        when (settings.captionBubbleStyle) {
            CaptionBubbleStyle.TRANSLUCENT_DARK -> {
                bgPaint.color = 0xF21C1A18.toInt()
            }
            CaptionBubbleStyle.SOLID_AMOLED -> {
                bgPaint.color = 0xFF000000.toInt()
            }
            CaptionBubbleStyle.ACCENT_THEMED -> {
                bgPaint.color = ColorUtils.blendARGB(0xF21C1A18.toInt(), primaryColor, 0.20f)
            }
        }

        // Draw card background & border pill
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // 3. Bipping Status Dot Indicator
        val padX = 14f * density
        val dotRadius = 5f * density // 10dp diameter
        val dotCx = padX + dotRadius
        val dotCy = height / 2f

        val (dotColor, dotAlpha) = when {
            hasPartial -> primaryColor to 1.0f
            dictationState is DictationState.Listening -> primaryColor to dotPulseAlpha
            dictationState is DictationState.Starting || dictationState is DictationState.Restarting || dictationState is DictationState.WaitingRestart -> 0xFF2196F3.toInt() to dotPulseAlpha
            dictationState is DictationState.Paused -> 0xFFFFB300.toInt() to 1.0f
            dictationState is DictationState.Error -> 0xFFE53935.toInt() to 1.0f
            else -> 0xFF8C857A.toInt() to 0.4f
        }

        dotPaint.color = ColorUtils.setAlphaComponent(dotColor, (dotAlpha * 255).toInt().coerceIn(0, 255))
        canvas.drawCircle(dotCx, dotCy, dotRadius, dotPaint)

        // 4. Draw Live Text or Gap Prompt
        val textStartX = dotCx + dotRadius + (10f * density)
        val textStartY = (height - layout.height) / 2f
        canvas.save()
        canvas.translate(textStartX, textStartY)
        layout.draw(canvas)
        canvas.restore()
    }
}
