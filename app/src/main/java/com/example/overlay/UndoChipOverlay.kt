package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.graphics.ColorUtils
import com.example.settings.KothaSettings
import com.example.settings.UndoBadgeLabelMode
import com.example.settings.UndoBadgeSize
import com.example.settings.UndoBadgeStyle
import kotlin.math.max

@SuppressLint("ViewConstructor")
class UndoChipOverlay(
    context: Context,
    private val onUndoClicked: () -> Unit
) : View(context) {

    var settings: KothaSettings = KothaSettings()
        set(value) {
            field = value
            requestLayout()
            postInvalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private val chipRect = RectF()
    private var isPressedState = false
    var isDisplayed: Boolean = false
        private set

    private var scaleAnimValue = 1.0f
    private var scaleAnimator: ValueAnimator? = null

    // 6-second idle safety auto-dismiss so it stays available briefly after speaking
    private val autoDismissRunnable = Runnable {
        hideUndo()
    }

    init {
        visibility = INVISIBLE
        alpha = 0f
        isClickable = true
        isFocusable = false
    }

    fun showUndo(newSettings: KothaSettings) {
        settings = newSettings
        removeCallbacks(autoDismissRunnable)
        if (!settings.tapCaptionToUndo) {
            hideUndo()
            return
        }

        isDisplayed = true
        visibility = VISIBLE
        alpha = 1.0f

        scaleAnimator?.cancel()
        scaleAnimValue = 0.6f
        scaleAnimator = ValueAnimator.ofFloat(0.6f, 1.0f).apply {
            duration = 240L
            interpolator = OvershootInterpolator(1.4f)
            addUpdateListener {
                scaleAnimValue = it.animatedValue as Float
                postInvalidate()
            }
            start()
        }

        // Keep visible for 6 seconds unless user speaks again or taps
        postDelayed(autoDismissRunnable, 6000L)
        requestLayout()
        postInvalidate()
    }

    fun hideUndo() {
        removeCallbacks(autoDismissRunnable)
        if (!isDisplayed && visibility == INVISIBLE) return

        isDisplayed = false
        scaleAnimator?.cancel()
        scaleAnimator = ValueAnimator.ofFloat(scaleAnimValue, 0f).apply {
            duration = 180L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                scaleAnimValue = it.animatedValue as Float
                alpha = scaleAnimValue
                postInvalidate()
                if (scaleAnimValue <= 0.05f) {
                    visibility = INVISIBLE
                    alpha = 0f
                }
            }
            start()
        }
    }

    private fun getLabel(): String {
        return when (settings.undoBadgeLabelMode) {
            UndoBadgeLabelMode.ICON_AND_TEXT -> "↶ Undo"
            UndoBadgeLabelMode.ICON_ONLY -> "↶"
            UndoBadgeLabelMode.TEXT_ONLY -> "UNDO"
        }
    }

    private fun getMetrics(density: Float): Triple<Float, Float, Float> {
        // heightDp, horizPaddingDp, textSp
        return when (settings.undoBadgeSize) {
            UndoBadgeSize.COMPACT -> Triple(28f, 10f, 11.5f)
            UndoBadgeSize.STANDARD -> Triple(34f, 14f, 13f)
            UndoBadgeSize.PROMINENT -> Triple(40f, 18f, 14.5f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val label = getLabel()
        val (hDp, padDp, sp) = getMetrics(density)

        textPaint.textSize = sp * density
        val textWidth = textPaint.measureText(label)
        val padTotal = padDp * 2f * density
        val minWidth = when (settings.undoBadgeSize) {
            UndoBadgeSize.COMPACT -> 44f * density
            UndoBadgeSize.STANDARD -> 58f * density
            UndoBadgeSize.PROMINENT -> 72f * density
        }

        val measuredW = max(minWidth, textWidth + padTotal).toInt()
        val measuredH = (hDp * density).toInt()

        setMeasuredDimension(measuredW, measuredH)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visibility != VISIBLE) return

        val density = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f

        canvas.save()
        val currentScale = if (isPressedState) scaleAnimValue * 0.92f else scaleAnimValue
        canvas.scale(currentScale, currentScale, cx, cy)

        val strokeW = if (settings.undoBadgeStyle == UndoBadgeStyle.TRANSPARENT_BORDER) 2f * density else 1.5f * density
        borderPaint.strokeWidth = strokeW
        chipRect.set(strokeW, strokeW, width.toFloat() - strokeW, height.toFloat() - strokeW)
        val radius = chipRect.height() / 2f

        val customColor = settings.undoBadgeCustomColor.toInt()
        val themeAccent = if (customColor != 0) customColor else settings.listeningColor.toInt()
        val userOpacity = settings.undoButtonOpacity.coerceIn(0.15f, 1.0f)

        val (rawBgColor, borderColor, textColor) = when (settings.undoBadgeStyle) {
            UndoBadgeStyle.GLASSMORPHISM -> {
                // Frosted translucent glass background with crisp white edge
                val bg = 0x48FFFFFF.toInt()
                val border = 0x88FFFFFF.toInt()
                val textCol = Color.WHITE
                Triple(bg, border, textCol)
            }
            UndoBadgeStyle.SOLID_AMOLED_BLACK -> {
                // Pitch black background with distinct theme border
                val bg = 0xFF000000.toInt()
                val border = ColorUtils.blendARGB(themeAccent, Color.WHITE, 0.40f)
                val textCol = Color.WHITE
                Triple(bg, border, textCol)
            }
            UndoBadgeStyle.TRANSPARENT_BORDER -> {
                // Full transparent interior with prominent margin border at the edge
                val bg = Color.TRANSPARENT
                val border = themeAccent
                val textCol = if (ColorUtils.calculateLuminance(themeAccent) > 0.25f) themeAccent else Color.WHITE
                Triple(bg, border, textCol)
            }
            UndoBadgeStyle.FILLED_ACCENT -> {
                val bg = themeAccent
                val border = ColorUtils.blendARGB(bg, Color.WHITE, 0.40f)
                val isLight = ColorUtils.calculateLuminance(bg) > 0.45
                val textCol = if (isLight) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()
                Triple(bg, border, textCol)
            }
            UndoBadgeStyle.CRIMSON_DANGER -> {
                val bg = 0xFFD32F2F.toInt()
                val border = 0xFFFF5252.toInt()
                Triple(bg, border, Color.WHITE)
            }
            UndoBadgeStyle.AMBER_GLOW -> {
                val bg = 0xFFFFB300.toInt()
                val border = 0xFFFFD54F.toInt()
                Triple(bg, border, 0xFF121212.toInt())
            }
            UndoBadgeStyle.MONOCHROME_INVERSE -> {
                val bg = 0xFFFFFFFF.toInt()
                val border = 0xFFE0E0E0.toInt()
                Triple(bg, border, 0xFF121212.toInt())
            }
            UndoBadgeStyle.OUTLINED_NEON -> {
                val bg = ColorUtils.setAlphaComponent(themeAccent, 55)
                val border = themeAccent
                val textCol = if (ColorUtils.calculateLuminance(themeAccent) > 0.25) themeAccent else Color.WHITE
                Triple(bg, border, textCol)
            }
        }

        // Apply user-configured opacity
        val finalBgColor = if (rawBgColor == Color.TRANSPARENT) {
            Color.TRANSPARENT
        } else {
            val baseAlpha = Color.alpha(rawBgColor)
            ColorUtils.setAlphaComponent(rawBgColor, (baseAlpha * userOpacity).toInt().coerceIn(0, 255))
        }

        val baseBorderAlpha = Color.alpha(borderColor)
        val finalBorderColor = ColorUtils.setAlphaComponent(borderColor, (baseBorderAlpha * userOpacity).toInt().coerceIn(0, 255))

        val baseTextAlpha = Color.alpha(textColor)
        val finalTextColor = ColorUtils.setAlphaComponent(textColor, (baseTextAlpha * userOpacity.coerceAtLeast(0.6f)).toInt().coerceIn(0, 255))

        bgPaint.color = if (isPressedState && finalBgColor != Color.TRANSPARENT) {
            ColorUtils.blendARGB(finalBgColor, Color.BLACK, 0.25f)
        } else {
            finalBgColor
        }
        borderPaint.color = finalBorderColor
        textPaint.color = finalTextColor

        if (finalBgColor != Color.TRANSPARENT) {
            canvas.drawRoundRect(chipRect, radius, radius, bgPaint)
        }
        canvas.drawRoundRect(chipRect, radius, radius, borderPaint)

        val label = getLabel()
        val textY = cy - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(label, cx, textY, textPaint)

        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDisplayed || visibility != VISIBLE) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                isPressedState = true
                postInvalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (isPressedState) {
                    isPressedState = false
                    try {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } catch (ignored: Exception) {}
                    onUndoClicked()
                    hideUndo()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isPressedState = false
                postInvalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
