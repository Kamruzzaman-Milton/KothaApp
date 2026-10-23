package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import com.example.engine.DictationState
import com.example.settings.KothaSettings
import com.example.settings.OrbShape
import com.example.settings.RingStyle
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@SuppressLint("ViewConstructor")
class OrbFloatingView(
    context: Context,
    var onSingleTap: () -> Unit,
    var onDoubleTap: () -> Unit,
    var onLongPress: () -> Unit,
    var onDragPositionChanged: (dx: Float, dy: Float) -> Unit,
    var onDragFinished: (velocityX: Float, velocityY: Float) -> Unit,
    var onDismissOrb: () -> Unit
) : View(context) {

    var settings: KothaSettings = KothaSettings()
        set(value) {
            field = value
            postInvalidate()
        }

    var dictationState: DictationState = DictationState.Idle
        set(value) {
            field = value
            lastStateChangeTime = SystemClock.uptimeMillis()
            postInvalidate()
        }

    var audioLevel: Float = 0f
        set(value) {
            field = value
            postInvalidate()
        }

    var isBridgeConnected: Boolean = true
        set(value) {
            field = value
            postInvalidate()
        }

    var captionText: String = ""
        set(value) {
            field = value
            postInvalidate()
        }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val captionBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xDD0F0E0C.toInt()
        style = Paint.Style.FILL
    }

    private val clipPath = Path()
    private val rectF = RectF()

    private var breathPhase = 0f
    private var rotateAngle = 0f
    private var lastStateChangeTime = SystemClock.uptimeMillis()

    // Touch tracking
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false
    private val touchSlop = 16f

    private val continuousAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 3000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val v = it.animatedValue as Float
            breathPhase = (sin(Math.toRadians(v.toDouble())) * 0.5 + 0.5).toFloat()
            rotateAngle = (rotateAngle + 3f) % 360f
            invalidate()
        }
    }

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            performHaptic()
            onSingleTap()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            performHaptic()
            onDoubleTap()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            performHaptic()
            onLongPress()
        }
    })

    init {
        continuousAnimator.start()
    }

    fun performHaptic() {
        if (settings.hapticsEnabled) {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    override fun onDetachedFromWindow() {
        continuousAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val cx = width / 2f
        val cy = height / 2f
        val density = resources.displayMetrics.density
        val targetRadius = (settings.orbSizeDp * density) / 2f
        val maxAllowedRadius = (size / 2f) - (6f * density)
        val baseRadius = targetRadius.coerceAtMost(maxAllowedRadius.coerceAtLeast(10f))

        // Determine State Color
        val currentColor = when {
            !isBridgeConnected -> 0xFF8C857A.toInt() // Bridge down / disconnected
            dictationState is DictationState.Listening -> settings.listeningColor.toInt()
            dictationState is DictationState.WaitingRestart -> settings.waitingColor.toInt()
            dictationState is DictationState.Restarting -> 0xFF2196F3.toInt()
            dictationState is DictationState.Processing -> settings.processingColor.toInt()
            dictationState is DictationState.Error -> settings.errorColor.toInt()
            dictationState is DictationState.Paused -> 0xFF8C857A.toInt()
            else -> settings.idleColor.toInt() // Idle
        }

        // Subtle Breathing expansion in Idle
        val currentRadius = if (dictationState is DictationState.Idle) {
            baseRadius + (breathPhase * 2f)
        } else {
            baseRadius
        }

        // 1. Draw Orb Base Shape
        bgPaint.color = currentColor
        bgPaint.alpha = (settings.activeOpacity * 255).toInt().coerceIn(40, 255)

        drawShapedBody(canvas, cx, cy, currentRadius, settings.orbShape)

        // 2. Draw State Ring
        drawStateRing(canvas, cx, cy, currentRadius)

        // 3. Draw Center Icon / Glyphs
        drawCenterGlyph(canvas, cx, cy, currentRadius)
    }

    private fun drawShapedBody(canvas: Canvas, cx: Float, cy: Float, radius: Float, shape: OrbShape) {
        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)
        when (shape) {
            OrbShape.CIRCLE -> {
                canvas.drawCircle(cx, cy, radius, bgPaint)
            }
            OrbShape.SQUIRCLE -> {
                canvas.drawRoundRect(rectF, radius * 0.60f, radius * 0.60f, bgPaint)
            }
            OrbShape.ROUNDED_SQUARE -> {
                canvas.drawRoundRect(rectF, radius * 0.28f, radius * 0.28f, bgPaint)
            }
            OrbShape.SEAL -> {
                // Polygon/seal with notched edges
                clipPath.reset()
                val points = 12
                val innerR = radius * 0.88f
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) radius else innerR
                    val angle = Math.toRadians((i * 360.0 / (points * 2)))
                    val px = cx + (r * cos(angle)).toFloat()
                    val py = cy + (r * sin(angle)).toFloat()
                    if (i == 0) clipPath.moveTo(px, py) else clipPath.lineTo(px, py)
                }
                clipPath.close()
                canvas.drawPath(clipPath, bgPaint)
            }
        }
    }

    private fun drawStateRing(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val outerR = radius + 6f
        ringPaint.strokeWidth = settings.outlineThicknessDp * 2f
        ringPaint.color = settings.ringColor.toInt()

        when (val state = dictationState) {
            is DictationState.Listening -> {
                // Radial ticks reacting dynamically to RMS audio level
                val tickCount = 20
                val tickSpread = 360f / tickCount
                for (i in 0 until tickCount) {
                    // Deliberate 1 o'clock gap (around 300 degrees)
                    val deg = i * tickSpread
                    if (deg in 285f..320f) continue

                    val rad = Math.toRadians(deg.toDouble())
                    val tickLen = 4f + (audioLevel * 10f)
                    val x1 = cx + (outerR * cos(rad)).toFloat()
                    val y1 = cy + (outerR * sin(rad)).toFloat()
                    val x2 = cx + ((outerR + tickLen) * cos(rad)).toFloat()
                    val y2 = cy + ((outerR + tickLen) * sin(rad)).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, ringPaint)
                }
            }

            is DictationState.WaitingRestart -> {
                // Dashed countdown ring
                val sweep = ((state.remainingMs.toFloat() / state.totalMs.coerceAtLeast(1)) * 360f)
                rectF.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
                canvas.drawArc(rectF, -90f, sweep, false, ringPaint)
            }

            is DictationState.Restarting -> {
                ringPaint.color = 0xFF2196F3.toInt()
                rectF.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
                canvas.drawArc(rectF, rotateAngle, 140f, false, ringPaint)
            }

            is DictationState.Processing -> {
                // Rotating arc
                rectF.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
                canvas.drawArc(rectF, rotateAngle, 120f, false, ringPaint)
            }

            is DictationState.Error -> {
                // Broken ring (arc with gap)
                rectF.set(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
                canvas.drawArc(rectF, 30f, 120f, false, ringPaint)
                canvas.drawArc(rectF, 180f, 120f, false, ringPaint)
            }

            else -> {
                // Subtle static outline in idle
                if (settings.ringStyle != RingStyle.NONE) {
                    canvas.drawCircle(cx, cy, outerR, ringPaint)
                }
            }
        }
    }

    private fun drawCenterGlyph(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        iconPaint.color = settings.iconColor.toInt()
        iconPaint.strokeWidth = 3f

        if (!isBridgeConnected) {
            // Small broken link mark indicating accessibility service is down
            val linkR = radius * 0.35f
            canvas.drawLine(cx - linkR, cy - linkR, cx - 2f, cy - 2f, iconPaint)
            canvas.drawLine(cx + 2f, cy + 2f, cx + linkR, cy + linkR, iconPaint)
            canvas.drawCircle(cx - linkR, cy - linkR, 4f, iconPaint)
            canvas.drawCircle(cx + linkR, cy + linkR, 4f, iconPaint)
            return
        }

        when (dictationState) {
            is DictationState.Listening -> {
                // Active solid core dot
                val dotR = radius * 0.22f + (audioLevel * 4f)
                iconPaint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, dotR, iconPaint)
                iconPaint.style = Paint.Style.STROKE
            }
            is DictationState.Paused -> {
                // Two vertical pause bars
                val barH = radius * 0.40f
                val barW = 3f
                canvas.drawLine(cx - 6f, cy - barH, cx - 6f, cy + barH, iconPaint)
                canvas.drawLine(cx + 6f, cy - barH, cx + 6f, cy + barH, iconPaint)
            }
            is DictationState.Error -> {
                // Exclamation mark
                canvas.drawLine(cx, cy - radius * 0.35f, cx, cy + radius * 0.1f, iconPaint)
                canvas.drawPoint(cx, cy + radius * 0.3f, iconPaint)
            }
            else -> {
                // Quiet center mark (small dot or voice glyph)
                iconPaint.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, radius * 0.18f, iconPaint)
                iconPaint.style = Paint.Style.STROKE
            }
        }
    }

    private fun drawCaptionBubble(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val bubbleText = if (captionText.length > 25) captionText.take(22) + "…" else captionText
        val textWidth = textPaint.measureText(bubbleText)
        val padX = 16f
        val padY = 12f
        val bubbleW = textWidth + padX * 2
        val bubbleH = textPaint.textSize + padY * 2

        val bubbleLeft = cx - bubbleW / 2
        val bubbleTop = cy - radius - bubbleH - 12f
        rectF.set(bubbleLeft, bubbleTop, bubbleLeft + bubbleW, bubbleTop + bubbleH)

        canvas.drawRoundRect(rectF, 12f, 12f, captionBgPaint)
        canvas.drawText(bubbleText, bubbleLeft + padX, bubbleTop + bubbleH - padY - 2f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY

                if (!isDragging && (dx * dx + dy * dy > touchSlop * touchSlop)) {
                    if (!settings.lockPosition) {
                        isDragging = true
                    }
                }

                if (isDragging) {
                    onDragPositionChanged(dx, dy)
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    onDragFinished(0f, 0f)
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
