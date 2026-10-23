package com.example.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import com.example.MainActivity
import com.example.engine.DictationState
import com.example.settings.CaptionBubblePosition
import com.example.settings.KothaSettings
import com.example.settings.SettingsRepository
import com.example.settings.VisibilityRule
import com.example.util.DiagnosticsLog

class OrbWindowManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit,
    private val onUndo: () -> Unit,
    private val onTogglePause: () -> Unit,
    private val onHideOrb: () -> Unit
) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var floatingView: OrbFloatingView? = null
    private var captionBubbleView: CaptionBubbleOverlay? = null
    private var quickPanelView: QuickPanelOverlay? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var captionLayoutParams: WindowManager.LayoutParams? = null

    private var normalizedX = 0.85f
    private var normalizedY = 0.50f

    private var screenWidth = 1080
    private var screenHeight = 2400

    private var isViewAttached = false
    private var isCaptionAttached = false

    init {
        // Load permanently saved position on initialization
        val savedPos = settingsRepository.getSavedOrbPosition()
        normalizedX = savedPos.first
        normalizedY = savedPos.second
    }

    fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun showOrb(settings: KothaSettings) {
        if (!canDrawOverlays()) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "Cannot show Orb: SYSTEM_ALERT_WINDOW not granted")
            return
        }

        if (isViewAttached) {
            updateSettings(settings)
            return
        }

        updateDisplayMetrics()

        val savedPos = settingsRepository.getSavedOrbPosition()
        normalizedX = savedPos.first
        normalizedY = savedPos.second

        val density = context.resources.displayMetrics.density
        val orbPx = (settings.orbSizeDp * density).toInt()
        val extraPad = (36 * density).toInt()
        val totalDimension = orbPx + extraPad

        val params = WindowManager.LayoutParams(
            totalDimension,
            totalDimension,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (normalizedX * (screenWidth - totalDimension)).toInt().coerceIn(0, (screenWidth - totalDimension).coerceAtLeast(0))
            y = (normalizedY * (screenHeight - totalDimension)).toInt().coerceIn(0, (screenHeight - totalDimension).coerceAtLeast(0))
        }

        layoutParams = params

        floatingView = OrbFloatingView(
            context = context,
            onSingleTap = { onSingleTap() },
            onDoubleTap = { onDoubleTap() },
            onLongPress = { showQuickPanel(settingsRepository.cachedSettings) },
            onDragPositionChanged = { dx, dy ->
                params.x += dx.toInt()
                params.y += dy.toInt()
                try {
                    windowManager.updateViewLayout(floatingView, params)
                } catch (ignored: Exception) {}
                updateCaptionPosition()
            },
            onDragFinished = { _, _ ->
                snapToEdge(settingsRepository.cachedSettings)
            },
            onDismissOrb = {
                onHideOrb()
            }
        ).apply {
            this.settings = settings
        }

        try {
            windowManager.addView(floatingView, params)
            isViewAttached = true
            setupCaptionBubble(settings)
            DiagnosticsLog.log(DiagnosticsLog.Category.SYSTEM, "Floating Orb added to WindowManager (size=${settings.orbSizeDp}dp)")
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "Failed to add floating Orb: ${e.message}")
        }
    }

    private fun setupCaptionBubble(settings: KothaSettings) {
        if (captionBubbleView != null) return
        val capView = CaptionBubbleOverlay(context, settings)
        capView.onLayoutChanged = {
            updateCaptionPosition()
        }
        captionBubbleView = capView

        val capParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        captionLayoutParams = capParams

        try {
            windowManager.addView(capView, capParams)
            isCaptionAttached = true
            DiagnosticsLog.log(DiagnosticsLog.Category.SYSTEM, "CaptionBubbleOverlay added to WindowManager")
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "Failed to add CaptionBubbleOverlay: ${e.message}")
        }
    }

    private fun updateCaptionPosition() {
        val capView = captionBubbleView ?: return
        val capParams = captionLayoutParams ?: return
        val orbParams = layoutParams ?: return
        val settings = settingsRepository.cachedSettings
        if (!isCaptionAttached) return

        val density = context.resources.displayMetrics.density
        val margin = (12 * density).toInt()

        capView.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
        )
        val capW = capView.measuredWidth
        val capH = capView.measuredHeight
        val orbW = orbParams.width
        val orbH = orbParams.height

        when (settings.captionBubblePosition) {
            CaptionBubblePosition.TOP_BANNER -> {
                capParams.x = ((screenWidth - capW) / 2).coerceAtLeast(margin)
                capParams.y = (36 * density).toInt()
            }
            CaptionBubblePosition.ABOVE_ORB -> {
                val idealX = orbParams.x + (orbW / 2) - (capW / 2)
                capParams.x = idealX.coerceIn(margin, (screenWidth - capW - margin).coerceAtLeast(margin))
                val idealY = orbParams.y - capH - (10 * density).toInt()
                capParams.y = if (idealY < (30 * density).toInt()) {
                    // Too close to top screen edge, flip to below orb
                    orbParams.y + orbH + (10 * density).toInt()
                } else {
                    idealY
                }
            }
            CaptionBubblePosition.BELOW_ORB -> {
                val idealX = orbParams.x + (orbW / 2) - (capW / 2)
                capParams.x = idealX.coerceIn(margin, (screenWidth - capW - margin).coerceAtLeast(margin))
                val idealY = orbParams.y + orbH + (10 * density).toInt()
                capParams.y = if (idealY + capH > screenHeight - (60 * density).toInt()) {
                    // Too close to bottom screen edge, flip to above orb
                    orbParams.y - capH - (10 * density).toInt()
                } else {
                    idealY
                }
            }
        }

        try {
            windowManager.updateViewLayout(capView, capParams)
        } catch (ignored: Exception) {}
    }

    fun updateSettings(settings: KothaSettings) {
        val view = floatingView ?: return
        view.settings = settings
        captionBubbleView?.settings = settings
        captionBubbleView?.updateStyles()
        val params = layoutParams ?: return
        if (isViewAttached) {
            updateDisplayMetrics()
            val density = context.resources.displayMetrics.density
            val orbPx = (settings.orbSizeDp * density).toInt()
            val extraPad = (36 * density).toInt()
            val totalDimension = orbPx + extraPad
            params.width = totalDimension
            params.height = totalDimension

            // If position is locked, reposition according to saved coordinates
            if (settings.lockPosition) {
                val savedPos = settingsRepository.getSavedOrbPosition()
                normalizedX = savedPos.first
                normalizedY = savedPos.second
                params.x = (normalizedX * (screenWidth - totalDimension)).toInt().coerceIn(0, (screenWidth - totalDimension).coerceAtLeast(0))
                params.y = (normalizedY * (screenHeight - totalDimension)).toInt().coerceIn(0, (screenHeight - totalDimension).coerceAtLeast(0))
            }

            try {
                windowManager.updateViewLayout(view, params)
            } catch (ignored: Exception) {}
            updateCaptionPosition()
        }
    }

    fun updateState(state: DictationState) {
        floatingView?.dictationState = state
        captionBubbleView?.setDictationState(state)
        updateCaptionPosition()
    }

    fun updateAudioLevel(level: Float) {
        floatingView?.audioLevel = level
    }

    fun updateBridgeConnected(connected: Boolean) {
        floatingView?.isBridgeConnected = connected
    }

    fun updateCaption(caption: String, isStreaming: Boolean = true) {
        val settings = settingsRepository.cachedSettings
        floatingView?.captionText = caption

        if (!settings.showLiveCaptionBubble) {
            captionBubbleView?.clearAll()
            return
        }

        if (isStreaming) {
            captionBubbleView?.setPartialText(caption)
        } else {
            captionBubbleView?.setFinalUtterance(caption)
        }
        updateCaptionPosition()
    }

    fun updateVisibility(isKeyboardVisible: Boolean, isEditableFocused: Boolean, settings: KothaSettings) {
        val view = floatingView ?: return
        if (!isViewAttached) return

        val shouldBeVisible = when (settings.visibilityRule) {
            VisibilityRule.KEYBOARD_VISIBLE -> isKeyboardVisible
            VisibilityRule.EDITABLE_FOCUSED -> isEditableFocused
            VisibilityRule.ALWAYS -> true
        }

        // CRITICAL FIX: Overlay views attached to WindowManager must NEVER use View.GONE!
        // View.GONE causes ViewRootImpl to collapse to 0x0 and disassociates surface,
        // which throws fatal crash when toggled repeatedly. Use View.INVISIBLE and safe try/catch.
        try {
            view.visibility = if (shouldBeVisible) android.view.View.VISIBLE else android.view.View.INVISIBLE
        } catch (ignored: Exception) {}
    }

    fun handleKeyboardVisibility(visible: Boolean, keyboardHeightPx: Int, settings: KothaSettings) {
        val params = layoutParams ?: return
        val view = floatingView ?: return
        if (!isViewAttached) return

        // Fixed screen position: vertical position must not shift upward or downward automatically
        if (visible && settings.dockToEdgeOnKeyboard) {
            params.x = if (params.x > screenWidth / 2) screenWidth - (view.width / 2) else -(view.width / 2)
            try { windowManager.updateViewLayout(view, params) } catch (ignored: Exception) {}
            updateCaptionPosition()
        }
    }

    private fun snapToEdge(settings: KothaSettings) {
        val params = layoutParams ?: return
        val view = floatingView ?: return
        val viewWidth = view.width
        val viewHeight = view.height

        if (settings.snapToEdge) {
            val margin = (settings.edgeMarginDp * context.resources.displayMetrics.density).toInt()
            val targetX = if (params.x + viewWidth / 2 > screenWidth / 2) {
                screenWidth - viewWidth - margin
            } else {
                margin
            }

            val animator = ValueAnimator.ofInt(params.x, targetX).apply {
                duration = 250L
                interpolator = OvershootInterpolator(1.1f)
                addUpdateListener {
                    params.x = it.animatedValue as Int
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (ignored: Exception) {}
                    updateCaptionPosition()
                }
            }
            animator.start()

            normalizedX = targetX.toFloat() / (screenWidth - viewWidth).coerceAtLeast(1)
            normalizedY = params.y.toFloat() / (screenHeight - viewHeight).coerceAtLeast(1)
        } else {
            normalizedX = params.x.toFloat() / (screenWidth - viewWidth).coerceAtLeast(1)
            normalizedY = params.y.toFloat() / (screenHeight - viewHeight).coerceAtLeast(1)
        }

        // Permanently persist the position so it is never reset
        settingsRepository.saveOrbPosition(normalizedX, normalizedY)
    }

    private fun showQuickPanel(settings: KothaSettings) {
        if (quickPanelView != null) return

        val width = (300 * context.resources.displayMetrics.density).toInt()
        val params = WindowManager.LayoutParams(
            width,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val panel = QuickPanelOverlay(
            context = context,
            settings = settings,
            transcriptPeek = floatingView?.captionText ?: "",
            onSwitchLanguage = { /* Handled in service */ },
            onToggleMode = { onDoubleTap() },
            onUndo = { onUndo() },
            onPause = { onTogglePause() },
            onHideOrb = {
                hideQuickPanel()
                onHideOrb()
            },
            onOpenApp = {
                hideQuickPanel()
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(intent)
            },
            onDismiss = { hideQuickPanel() }
        )

        quickPanelView = panel
        try {
            windowManager.addView(panel, params)
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "Failed to show quick panel: ${e.message}")
        }
    }

    private fun hideQuickPanel() {
        quickPanelView?.let {
            try {
                windowManager.removeView(it)
            } catch (ignored: Exception) {}
            quickPanelView = null
        }
    }

    fun hideOrb() {
        hideQuickPanel()
        captionBubbleView?.let {
            try {
                windowManager.removeView(it)
            } catch (ignored: Exception) {}
            captionBubbleView = null
            isCaptionAttached = false
        }
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (ignored: Exception) {}
            floatingView = null
            isViewAttached = false
            DiagnosticsLog.log(DiagnosticsLog.Category.SYSTEM, "Floating Orb removed from WindowManager")
        }
    }

    private fun updateDisplayMetrics() {
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(dm)
        screenWidth = dm.widthPixels
        screenHeight = dm.heightPixels
    }
}
