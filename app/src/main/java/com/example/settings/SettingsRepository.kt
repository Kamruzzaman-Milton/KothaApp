package com.example.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("kotha_permanent_prefs", Context.MODE_PRIVATE)

    @Volatile
    var cachedSettings: KothaSettings = loadSettingsFromPrefs()
        private set

    private val _settingsFlow = MutableStateFlow(cachedSettings)
    val settingsFlow: StateFlow<KothaSettings> = _settingsFlow.asStateFlow()

    private fun loadSettingsFromPrefs(): KothaSettings {
        // 1. Try restoring from master JSON backup first
        val backupJson = try { prefs.getString(KEY_SETTINGS_JSON_BACKUP, null) } catch (e: Exception) { null }
        val base = if (!backupJson.isNullOrBlank()) {
            try { KothaSettings.fromJson(backupJson) } catch (e: Exception) { KothaSettings() }
        } else {
            KothaSettings()
        }

        // 2. Safe per-key overlays: no single field exception can ever reset the other fields
        val primaryLang = try { prefs.getString(KEY_PRIMARY_LANG, base.primaryLanguage) ?: base.primaryLanguage } catch (e: Exception) { base.primaryLanguage }
        val secondaryLang = try { prefs.getString(KEY_SECONDARY_LANG, base.secondaryLanguage) ?: base.secondaryLanguage } catch (e: Exception) { base.secondaryLanguage }
        val customBcp47 = try { prefs.getString(KEY_CUSTOM_BCP47, base.customBcp47) ?: base.customBcp47 } catch (e: Exception) { base.customBcp47 }
        val preferOffline = try {
            val v = prefs.getBoolean(KEY_PREFER_OFFLINE, false)
            if (base.primaryLanguage.startsWith("bn", ignoreCase = true)) false else v
        } catch (e: Exception) { false }
        val useOnDevice = try { prefs.getBoolean(KEY_USE_ON_DEVICE, base.useOnDevice) } catch (e: Exception) { base.useOnDevice }
        val maxResults = try { prefs.getInt(KEY_MAX_RESULTS, base.maxResults) } catch (e: Exception) { base.maxResults }
        val maskOffensive = try { prefs.getBoolean(KEY_MASK_OFFENSIVE, base.maskOffensiveWords) } catch (e: Exception) { base.maskOffensiveWords }
        val enableFormatting = try { prefs.getBoolean(KEY_ENABLE_FORMATTING, base.enableFormatting) } catch (e: Exception) { base.enableFormatting }

        val dictationMode = try {
            val name = prefs.getString(KEY_DICTATION_MODE, base.dictationMode.name) ?: base.dictationMode.name
            DictationMode.valueOf(name)
        } catch (e: Exception) { base.dictationMode }

        val restartDelayMs = try { prefs.getLong(KEY_RESTART_DELAY_MS, base.restartDelayMs) } catch (e: Exception) { base.restartDelayMs }
        val seamlessRestartMs = try { prefs.getLong(KEY_SEAMLESS_RESTART_MS, base.seamlessRestartMs) } catch (e: Exception) { base.seamlessRestartMs }
        val rawSilence = try { prefs.getLong(KEY_SILENCE_COMPLETE_MS, base.silenceCompleteMs) } catch (e: Exception) { base.silenceCompleteMs }
        val silenceCompleteMs = if (rawSilence >= 15000L) base.silenceCompleteMs else rawSilence.coerceIn(500L, 8000L)
        val rawSilencePoss = try { prefs.getLong(KEY_SILENCE_POSS_MS, base.silencePossiblyCompleteMs) } catch (e: Exception) { base.silencePossiblyCompleteMs }
        val silencePossMs = if (rawSilencePoss >= 15000L) base.silencePossiblyCompleteMs else rawSilencePoss.coerceIn(400L, 6000L)
        val minUtterance = try { prefs.getInt(KEY_MIN_UTTERANCE, base.minUtteranceLength) } catch (e: Exception) { base.minUtteranceLength }
        val commitPartial = try { prefs.getBoolean(KEY_COMMIT_PARTIAL_LONG_SILENCE, base.commitPartialOnLongSilence) } catch (e: Exception) { base.commitPartialOnLongSilence }
        val maxSessionSec = try { prefs.getInt(KEY_MAX_SESSION_SEC, base.maxSessionLengthSec) } catch (e: Exception) { base.maxSessionLengthSec }
        val idleAutoStopSec = try { prefs.getInt(KEY_IDLE_AUTO_STOP_SEC, base.idleAutoStopSec) } catch (e: Exception) { base.idleAutoStopSec }
        val errorBackoffMs = try { prefs.getLong(KEY_ERROR_BACKOFF_MS, base.errorBackoffMs) } catch (e: Exception) { base.errorBackoffMs }
        val maxConsecFailures = try { prefs.getInt(KEY_MAX_CONSEC_FAILURES, base.maxConsecutiveFailures) } catch (e: Exception) { base.maxConsecutiveFailures }
        val preferSegmented = try { prefs.getBoolean(KEY_PREFER_SEGMENTED, base.preferSegmentedSession) } catch (e: Exception) { base.preferSegmentedSession }

        val streamingStyle = try {
            val name = prefs.getString(KEY_STREAMING_STYLE, base.streamingStyle.name) ?: base.streamingStyle.name
            StreamingStyle.valueOf(name)
        } catch (e: Exception) { base.streamingStyle }

        val throttleMs = try { prefs.getLong(KEY_THROTTLE_MS, base.throttleIntervalMs) } catch (e: Exception) { base.throttleIntervalMs }
        val separator = try { prefs.getString(KEY_SEPARATOR, base.separator) ?: base.separator } catch (e: Exception) { base.separator }
        val autoLeadingSpace = try { prefs.getBoolean(KEY_AUTO_LEADING_SPACE, base.autoLeadingSpace) } catch (e: Exception) { base.autoLeadingSpace }
        val addTrailingSpace = try { prefs.getBoolean(KEY_ADD_TRAILING_SPACE, base.addTrailingSpace) } catch (e: Exception) { base.addTrailingSpace }
        val filterHintPlaceholders = try { prefs.getBoolean(KEY_FILTER_HINT_PLACEHOLDERS, base.filterHintPlaceholders) } catch (e: Exception) { base.filterHintPlaceholders }
        val replaceSelection = try { prefs.getBoolean(KEY_REPLACE_SELECTION, base.replaceSelection) } catch (e: Exception) { base.replaceSelection }
        val followCursor = try { prefs.getBoolean(KEY_FOLLOW_CURSOR, base.followCursor) } catch (e: Exception) { base.followCursor }

        val insertionMethod = try {
            val name = prefs.getString(KEY_INSERTION_METHOD, base.insertionMethod.name) ?: base.insertionMethod.name
            InsertionMethod.valueOf(name)
        } catch (e: Exception) { base.insertionMethod }

        val refusePasswords = try { prefs.getBoolean(KEY_REFUSE_PASSWORDS, base.refusePasswordFields) } catch (e: Exception) { base.refusePasswordFields }
        val stopFocusLoss = try { prefs.getBoolean(KEY_STOP_FOCUS_LOSS, base.stopOnFocusLoss) } catch (e: Exception) { base.stopOnFocusLoss }
        val showCaptionBubble = try { prefs.getBoolean(KEY_SHOW_CAPTION_BUBBLE, base.showLiveCaptionBubble) } catch (e: Exception) { base.showLiveCaptionBubble }
        val captionPosition = try {
            val name = prefs.getString(KEY_CAPTION_POSITION, base.captionBubblePosition.name) ?: base.captionBubblePosition.name
            CaptionBubblePosition.valueOf(name)
        } catch (e: Exception) { base.captionBubblePosition }
        val captionSize = try {
            val name = prefs.getString(KEY_CAPTION_SIZE, base.captionBubbleSize.name) ?: base.captionBubbleSize.name
            CaptionBubbleSize.valueOf(name)
        } catch (e: Exception) { base.captionBubbleSize }
        val captionMaxLines = try { prefs.getInt(KEY_CAPTION_MAX_LINES, base.captionMaxLines).coerceIn(1, 3) } catch (e: Exception) { base.captionMaxLines }
        val captionStyle = try {
            val name = prefs.getString(KEY_CAPTION_STYLE, base.captionBubbleStyle.name) ?: base.captionBubbleStyle.name
            CaptionBubbleStyle.valueOf(name)
        } catch (e: Exception) { base.captionBubbleStyle }
        val captionLingerMs = try { prefs.getLong(KEY_CAPTION_LINGER_MS, base.captionLingerDurationMs) } catch (e: Exception) { base.captionLingerDurationMs }
        val tapCaptionToUndo = try { prefs.getBoolean(KEY_TAP_CAPTION_TO_UNDO, base.tapCaptionToUndo) } catch (e: Exception) { base.tapCaptionToUndo }
        val undoBadgeStyle = try {
            val name = prefs.getString(KEY_UNDO_BADGE_STYLE, base.undoBadgeStyle.name) ?: base.undoBadgeStyle.name
            UndoBadgeStyle.valueOf(name)
        } catch (e: Exception) { base.undoBadgeStyle }
        val undoBadgeLabelMode = try {
            val name = prefs.getString(KEY_UNDO_BADGE_LABEL_MODE, base.undoBadgeLabelMode.name) ?: base.undoBadgeLabelMode.name
            UndoBadgeLabelMode.valueOf(name)
        } catch (e: Exception) { base.undoBadgeLabelMode }
        val undoBadgeSize = try {
            val name = prefs.getString(KEY_UNDO_BADGE_SIZE, base.undoBadgeSize.name) ?: base.undoBadgeSize.name
            UndoBadgeSize.valueOf(name)
        } catch (e: Exception) { base.undoBadgeSize }
        val undoBadgePulse = try { prefs.getBoolean(KEY_UNDO_BADGE_PULSE, base.undoBadgePulse) } catch (e: Exception) { base.undoBadgePulse }
        val undoBadgeCustomColor = try { prefs.getLong(KEY_UNDO_BADGE_CUSTOM_COLOR, base.undoBadgeCustomColor) } catch (e: Exception) { base.undoBadgeCustomColor }
        val undoButtonOpacity = try { prefs.getFloat(KEY_UNDO_BUTTON_OPACITY, base.undoButtonOpacity) } catch (e: Exception) { base.undoButtonOpacity }
        val undoButtonHorizontalGapDp = try { prefs.getInt(KEY_UNDO_BUTTON_HORIZONTAL_GAP_DP, base.undoButtonHorizontalGapDp) } catch (e: Exception) { base.undoButtonHorizontalGapDp }
        val voiceBtnOffset = try { prefs.getInt(KEY_VOICE_BTN_OFFSET, base.voiceButtonVerticalOffsetDp) } catch (e: Exception) { base.voiceButtonVerticalOffsetDp }
        val transcriptionOffset = try { prefs.getInt(KEY_TRANSCRIPTION_OFFSET, base.transcriptionVerticalOffsetDp) } catch (e: Exception) { base.transcriptionVerticalOffsetDp }

        val orbSize = try {
            prefs.getInt(KEY_ORB_SIZE, base.orbSizeDp)
        } catch (e: Exception) {
            try {
                prefs.getFloat(KEY_ORB_SIZE, base.orbSizeDp.toFloat()).toInt()
            } catch (e2: Exception) {
                base.orbSizeDp
            }
        }

        val orbShape = try {
            val name = prefs.getString(KEY_ORB_SHAPE, base.orbShape.name) ?: base.orbShape.name
            OrbShape.valueOf(name)
        } catch (e: Exception) { base.orbShape }

        val idleOpacity = try {
            prefs.getFloat(KEY_IDLE_OPACITY, base.idleOpacity)
        } catch (e: Exception) {
            try {
                prefs.getInt(KEY_IDLE_OPACITY, (base.idleOpacity * 100).toInt()).toFloat() / 100f
            } catch (e2: Exception) {
                base.idleOpacity
            }
        }
        val activeOpacity = try {
            prefs.getFloat(KEY_ACTIVE_OPACITY, base.activeOpacity)
        } catch (e: Exception) {
            try {
                prefs.getInt(KEY_ACTIVE_OPACITY, (base.activeOpacity * 100).toInt()).toFloat() / 100f
            } catch (e2: Exception) {
                base.activeOpacity
            }
        }

        val ringStyle = try {
            val name = prefs.getString(KEY_RING_STYLE, base.ringStyle.name) ?: base.ringStyle.name
            RingStyle.valueOf(name)
        } catch (e: Exception) { base.ringStyle }

        val animSpeed = try { prefs.getFloat(KEY_ANIM_SPEED, base.animationSpeed) } catch (e: Exception) { base.animationSpeed }
        val outlineThickness = try { prefs.getFloat(KEY_OUTLINE_THICKNESS, base.outlineThicknessDp) } catch (e: Exception) { base.outlineThicknessDp }
        val shadowStrength = try { prefs.getFloat(KEY_SHADOW_STRENGTH, base.shadowStrength) } catch (e: Exception) { base.shadowStrength }
        val edgeMargin = try {
            prefs.getInt(KEY_EDGE_MARGIN, base.edgeMarginDp)
        } catch (e: Exception) {
            try {
                prefs.getFloat(KEY_EDGE_MARGIN, base.edgeMarginDp.toFloat()).toInt()
            } catch (e2: Exception) {
                base.edgeMarginDp
            }
        }
        val hapticStrength = try { prefs.getFloat(KEY_HAPTIC_STRENGTH, base.hapticStrength) } catch (e: Exception) { base.hapticStrength }
        val lockPosition = try { prefs.getBoolean(KEY_LOCK_POSITION, base.lockPosition) } catch (e: Exception) { base.lockPosition }
        val snapToEdge = try { prefs.getBoolean(KEY_SNAP_TO_EDGE, base.snapToEdge) } catch (e: Exception) { base.snapToEdge }
        val dockOnKeyboard = try { prefs.getBoolean(KEY_DOCK_ON_KEYBOARD, base.dockToEdgeOnKeyboard) } catch (e: Exception) { base.dockToEdgeOnKeyboard }

        val visibilityRule = try {
            val name = prefs.getString(KEY_VISIBILITY_RULE, base.visibilityRule.name) ?: base.visibilityRule.name
            VisibilityRule.valueOf(name)
        } catch (e: Exception) { base.visibilityRule }

        val hideOnLock = try { prefs.getBoolean(KEY_HIDE_ON_LOCK, base.hideOnLockScreen) } catch (e: Exception) { base.hideOnLockScreen }

        val doubleTapAction = try {
            val name = prefs.getString(KEY_DOUBLE_TAP_ACTION, base.doubleTapAction.name) ?: base.doubleTapAction.name
            GestureAction.valueOf(name)
        } catch (e: Exception) { base.doubleTapAction }

        val fixedAnchor = try { prefs.getString(KEY_FIXED_ANCHOR, base.fixedScreenAnchor) ?: base.fixedScreenAnchor } catch (e: Exception) { base.fixedScreenAnchor }

        val idleColor = try {
            prefs.getLong(KEY_COLOR_IDLE, base.idleColor)
        } catch (e: Exception) {
            try { prefs.getInt(KEY_COLOR_IDLE, base.idleColor.toInt()).toLong() } catch (e2: Exception) { base.idleColor }
        }
        val listeningColor = try {
            prefs.getLong(KEY_COLOR_LISTENING, base.listeningColor)
        } catch (e: Exception) {
            try { prefs.getInt(KEY_COLOR_LISTENING, base.listeningColor.toInt()).toLong() } catch (e2: Exception) { base.listeningColor }
        }
        val waitingColor = try {
            prefs.getLong(KEY_COLOR_WAITING, base.waitingColor)
        } catch (e: Exception) {
            try { prefs.getInt(KEY_COLOR_WAITING, base.waitingColor.toInt()).toLong() } catch (e2: Exception) { base.waitingColor }
        }
        val processingColor = try {
            prefs.getLong(KEY_COLOR_PROCESSING, base.processingColor)
        } catch (e: Exception) {
            try { prefs.getInt(KEY_COLOR_PROCESSING, base.processingColor.toInt()).toLong() } catch (e2: Exception) { base.processingColor }
        }
        val errorColor = try {
            prefs.getLong(KEY_COLOR_ERROR, base.errorColor)
        } catch (e: Exception) {
            try { prefs.getInt(KEY_COLOR_ERROR, base.errorColor.toInt()).toLong() } catch (e2: Exception) { base.errorColor }
        }
        val ringColor = try {
            prefs.getLong(KEY_COLOR_RING, base.ringColor)
        } catch (e: Exception) {
            try { prefs.getInt(KEY_COLOR_RING, base.ringColor.toInt()).toLong() } catch (e2: Exception) { base.ringColor }
        }
        val iconColor = try {
            prefs.getLong(KEY_COLOR_ICON, base.iconColor)
        } catch (e: Exception) {
            try { prefs.getInt(KEY_COLOR_ICON, base.iconColor.toInt()).toLong() } catch (e2: Exception) { base.iconColor }
        }

        val beepSuppression = try { prefs.getBoolean(KEY_BEEP_SUPPRESSION, base.beepSuppression) } catch (e: Exception) { base.beepSuppression }
        val softTones = try { prefs.getBoolean(KEY_SOFT_TONES, base.softTones) } catch (e: Exception) { base.softTones }
        val hapticsEnabled = try { prefs.getBoolean(KEY_HAPTICS_ENABLED, base.hapticsEnabled) } catch (e: Exception) { base.hapticsEnabled }
        val keepScreenAwake = try { prefs.getBoolean(KEY_KEEP_SCREEN_AWAKE, base.keepScreenAwake) } catch (e: Exception) { base.keepScreenAwake }
        val duckMedia = try { prefs.getBoolean(KEY_DUCK_MEDIA, base.duckMediaAudio) } catch (e: Exception) { base.duckMediaAudio }

        val autoPunctuate = try { prefs.getBoolean(KEY_AUTO_PUNCTUATE, base.autoPunctuate) } catch (e: Exception) { base.autoPunctuate }
        val digitConversion = try { prefs.getString(KEY_DIGIT_CONVERSION, base.digitConversionModeName) ?: base.digitConversionModeName } catch (e: Exception) { base.digitConversionModeName }
        val autoCapLatin = try { prefs.getBoolean(KEY_AUTO_CAP_LATIN, base.autoCapitalizeLatin) } catch (e: Exception) { base.autoCapitalizeLatin }
        val voiceCommands = try { prefs.getBoolean(KEY_VOICE_COMMANDS, base.voiceCommandsEnabled) } catch (e: Exception) { base.voiceCommandsEnabled }
        val voiceCommandsJson = try { prefs.getString(KEY_VOICE_COMMANDS_JSON, base.voiceCommandsJson) ?: base.voiceCommandsJson } catch (e: Exception) { base.voiceCommandsJson }
        val confirmSensitiveCommands = try { prefs.getBoolean(KEY_CONFIRM_SENSITIVE_COMMANDS, base.confirmSensitiveCommands) } catch (e: Exception) { base.confirmSensitiveCommands }
        val replacementsJson = try { prefs.getString(KEY_REPLACEMENTS_JSON, base.replacementsJson) ?: base.replacementsJson } catch (e: Exception) { base.replacementsJson }

        val keepAccessibilityAlive = try { prefs.getBoolean(KEY_KEEP_ACCESSIBILITY_ALIVE, base.keepAccessibilityAlive) } catch (e: Exception) { base.keepAccessibilityAlive }
        val showBootNotif = try { prefs.getBoolean(KEY_SHOW_BOOT_NOTIF, base.showBootNotification) } catch (e: Exception) { base.showBootNotification }

        val activeProfile = try { prefs.getString(KEY_ACTIVE_PROFILE, base.activeProfileId) ?: base.activeProfileId } catch (e: Exception) { base.activeProfileId }
        val historyEnabled = try { prefs.getBoolean(KEY_HISTORY_ENABLED, base.historyEnabled) } catch (e: Exception) { base.historyEnabled }
        val maxHistory = try { prefs.getInt(KEY_MAX_HISTORY, base.maxHistoryItems) } catch (e: Exception) { base.maxHistoryItems }

        val appTheme = try {
            val name = prefs.getString(KEY_APP_THEME, base.appTheme.name) ?: base.appTheme.name
            AppTheme.valueOf(name)
        } catch (e: Exception) { base.appTheme }

        val accentInk = try {
            val name = prefs.getString(KEY_ACCENT_INK, base.accentInk.name) ?: base.accentInk.name
            AccentInk.valueOf(name)
        } catch (e: Exception) { base.accentInk }

        return KothaSettings(
            primaryLanguage = primaryLang,
            secondaryLanguage = secondaryLang,
            customBcp47 = customBcp47,
            preferOffline = preferOffline,
            useOnDevice = useOnDevice,
            maxResults = maxResults,
            maskOffensiveWords = maskOffensive,
            enableFormatting = enableFormatting,
            dictationMode = dictationMode,
            restartDelayMs = restartDelayMs,
            seamlessRestartMs = seamlessRestartMs,
            silenceCompleteMs = silenceCompleteMs,
            silencePossiblyCompleteMs = silencePossMs,
            minUtteranceLength = minUtterance,
            commitPartialOnLongSilence = commitPartial,
            maxSessionLengthSec = maxSessionSec,
            idleAutoStopSec = idleAutoStopSec,
            errorBackoffMs = errorBackoffMs,
            maxConsecutiveFailures = maxConsecFailures,
            preferSegmentedSession = preferSegmented,
            streamingStyle = streamingStyle,
            throttleIntervalMs = throttleMs,
            separator = separator,
            autoLeadingSpace = autoLeadingSpace,
            addTrailingSpace = addTrailingSpace,
            filterHintPlaceholders = filterHintPlaceholders,
            replaceSelection = replaceSelection,
            followCursor = followCursor,
            insertionMethod = insertionMethod,
            refusePasswordFields = refusePasswords,
            stopOnFocusLoss = stopFocusLoss,
            showLiveCaptionBubble = showCaptionBubble,
            captionBubblePosition = captionPosition,
            captionBubbleSize = captionSize,
            captionMaxLines = captionMaxLines,
            captionBubbleStyle = captionStyle,
            captionLingerDurationMs = captionLingerMs,
            tapCaptionToUndo = tapCaptionToUndo,
            undoBadgeStyle = undoBadgeStyle,
            undoBadgeLabelMode = undoBadgeLabelMode,
            undoBadgeSize = undoBadgeSize,
            undoBadgePulse = undoBadgePulse,
            undoBadgeCustomColor = undoBadgeCustomColor,
            undoButtonOpacity = undoButtonOpacity,
            undoButtonHorizontalGapDp = undoButtonHorizontalGapDp,
            voiceButtonVerticalOffsetDp = voiceBtnOffset,
            transcriptionVerticalOffsetDp = transcriptionOffset,
            orbSizeDp = orbSize,
            orbShape = orbShape,
            idleOpacity = idleOpacity,
            activeOpacity = activeOpacity,
            ringStyle = ringStyle,
            animationSpeed = animSpeed,
            outlineThicknessDp = outlineThickness,
            shadowStrength = shadowStrength,
            edgeMarginDp = edgeMargin,
            hapticStrength = hapticStrength,
            lockPosition = lockPosition,
            snapToEdge = snapToEdge,
            dockToEdgeOnKeyboard = dockOnKeyboard,
            visibilityRule = visibilityRule,
            hideOnLockScreen = hideOnLock,
            doubleTapAction = doubleTapAction,
            fixedScreenAnchor = fixedAnchor,
            idleColor = idleColor,
            listeningColor = listeningColor,
            waitingColor = waitingColor,
            processingColor = processingColor,
            errorColor = errorColor,
            ringColor = ringColor,
            iconColor = iconColor,
            beepSuppression = beepSuppression,
            softTones = softTones,
            hapticsEnabled = hapticsEnabled,
            keepScreenAwake = keepScreenAwake,
            duckMediaAudio = duckMedia,
            autoPunctuate = autoPunctuate,
            digitConversionModeName = digitConversion,
            autoCapitalizeLatin = autoCapLatin,
            voiceCommandsEnabled = voiceCommands,
            voiceCommandsJson = voiceCommandsJson,
            confirmSensitiveCommands = confirmSensitiveCommands,
            replacementsJson = replacementsJson,
            keepAccessibilityAlive = keepAccessibilityAlive,
            showBootNotification = showBootNotif,
            activeProfileId = activeProfile,
            historyEnabled = historyEnabled,
            maxHistoryItems = maxHistory,
            appTheme = appTheme,
            accentInk = accentInk,
            orbPositionX = try { prefs.getFloat(KEY_ORB_X, 0.90f) } catch (e: Exception) { 0.90f },
            orbPositionY = try { prefs.getFloat(KEY_ORB_Y, 0.50f) } catch (e: Exception) { 0.50f }
        )
    }

    @Synchronized
    fun updateSettings(transform: (KothaSettings) -> KothaSettings) {
        val current = cachedSettings
        val updated = transform(current)
        cachedSettings = updated
        _settingsFlow.value = updated

        // Safely and immediately persist synchronously so nothing is ever lost or reverted
        try {
            val editor = prefs.edit()
            editor.putString(KEY_SETTINGS_JSON_BACKUP, updated.toJson())

            editor.putString(KEY_PRIMARY_LANG, updated.primaryLanguage)
            editor.putString(KEY_SECONDARY_LANG, updated.secondaryLanguage)
            editor.putString(KEY_CUSTOM_BCP47, updated.customBcp47)
            editor.putBoolean(KEY_PREFER_OFFLINE, updated.preferOffline)
            editor.putBoolean(KEY_USE_ON_DEVICE, updated.useOnDevice)
            editor.putInt(KEY_MAX_RESULTS, updated.maxResults)
            editor.putBoolean(KEY_MASK_OFFENSIVE, updated.maskOffensiveWords)
            editor.putBoolean(KEY_ENABLE_FORMATTING, updated.enableFormatting)

            editor.putString(KEY_DICTATION_MODE, updated.dictationMode.name)
            editor.putLong(KEY_RESTART_DELAY_MS, updated.restartDelayMs)
            editor.putLong(KEY_SEAMLESS_RESTART_MS, updated.seamlessRestartMs)
            editor.putLong(KEY_SILENCE_COMPLETE_MS, updated.silenceCompleteMs)
            editor.putLong(KEY_SILENCE_POSS_MS, updated.silencePossiblyCompleteMs)
            editor.putInt(KEY_MIN_UTTERANCE, updated.minUtteranceLength)
            editor.putBoolean(KEY_COMMIT_PARTIAL_LONG_SILENCE, updated.commitPartialOnLongSilence)
            editor.putInt(KEY_MAX_SESSION_SEC, updated.maxSessionLengthSec)
            editor.putInt(KEY_IDLE_AUTO_STOP_SEC, updated.idleAutoStopSec)
            editor.putLong(KEY_ERROR_BACKOFF_MS, updated.errorBackoffMs)
            editor.putInt(KEY_MAX_CONSEC_FAILURES, updated.maxConsecutiveFailures)
            editor.putBoolean(KEY_PREFER_SEGMENTED, updated.preferSegmentedSession)

            editor.putString(KEY_STREAMING_STYLE, updated.streamingStyle.name)
            editor.putLong(KEY_THROTTLE_MS, updated.throttleIntervalMs)
            editor.putString(KEY_SEPARATOR, updated.separator)
            editor.putBoolean(KEY_AUTO_LEADING_SPACE, updated.autoLeadingSpace)
            editor.putBoolean(KEY_ADD_TRAILING_SPACE, updated.addTrailingSpace)
            editor.putBoolean(KEY_FILTER_HINT_PLACEHOLDERS, updated.filterHintPlaceholders)
            editor.putBoolean(KEY_REPLACE_SELECTION, updated.replaceSelection)
            editor.putBoolean(KEY_FOLLOW_CURSOR, updated.followCursor)
            editor.putString(KEY_INSERTION_METHOD, updated.insertionMethod.name)
            editor.putBoolean(KEY_REFUSE_PASSWORDS, updated.refusePasswordFields)
            editor.putBoolean(KEY_STOP_FOCUS_LOSS, updated.stopOnFocusLoss)
            editor.putBoolean(KEY_SHOW_CAPTION_BUBBLE, updated.showLiveCaptionBubble)
            editor.putString(KEY_CAPTION_POSITION, updated.captionBubblePosition.name)
            editor.putString(KEY_CAPTION_SIZE, updated.captionBubbleSize.name)
            editor.putInt(KEY_CAPTION_MAX_LINES, updated.captionMaxLines)
            editor.putString(KEY_CAPTION_STYLE, updated.captionBubbleStyle.name)
            editor.putLong(KEY_CAPTION_LINGER_MS, updated.captionLingerDurationMs)
            editor.putBoolean(KEY_TAP_CAPTION_TO_UNDO, updated.tapCaptionToUndo)
            editor.putString(KEY_UNDO_BADGE_STYLE, updated.undoBadgeStyle.name)
            editor.putString(KEY_UNDO_BADGE_LABEL_MODE, updated.undoBadgeLabelMode.name)
            editor.putString(KEY_UNDO_BADGE_SIZE, updated.undoBadgeSize.name)
            editor.putBoolean(KEY_UNDO_BADGE_PULSE, updated.undoBadgePulse)
            editor.putLong(KEY_UNDO_BADGE_CUSTOM_COLOR, updated.undoBadgeCustomColor)
            editor.putFloat(KEY_UNDO_BUTTON_OPACITY, updated.undoButtonOpacity)
            editor.putInt(KEY_UNDO_BUTTON_HORIZONTAL_GAP_DP, updated.undoButtonHorizontalGapDp)
            editor.putInt(KEY_VOICE_BTN_OFFSET, updated.voiceButtonVerticalOffsetDp)
            editor.putInt(KEY_TRANSCRIPTION_OFFSET, updated.transcriptionVerticalOffsetDp)

            editor.putInt(KEY_ORB_SIZE, updated.orbSizeDp)
            editor.putString(KEY_ORB_SHAPE, updated.orbShape.name)
            editor.putFloat(KEY_IDLE_OPACITY, updated.idleOpacity)
            editor.putFloat(KEY_ACTIVE_OPACITY, updated.activeOpacity)
            editor.putString(KEY_RING_STYLE, updated.ringStyle.name)
            editor.putFloat(KEY_ANIM_SPEED, updated.animationSpeed)
            editor.putFloat(KEY_OUTLINE_THICKNESS, updated.outlineThicknessDp)
            editor.putFloat(KEY_SHADOW_STRENGTH, updated.shadowStrength)
            editor.putInt(KEY_EDGE_MARGIN, updated.edgeMarginDp)
            editor.putFloat(KEY_HAPTIC_STRENGTH, updated.hapticStrength)
            editor.putBoolean(KEY_LOCK_POSITION, updated.lockPosition)
            editor.putBoolean(KEY_SNAP_TO_EDGE, updated.snapToEdge)
            editor.putBoolean(KEY_DOCK_ON_KEYBOARD, updated.dockToEdgeOnKeyboard)
            editor.putString(KEY_VISIBILITY_RULE, updated.visibilityRule.name)
            editor.putBoolean(KEY_HIDE_ON_LOCK, updated.hideOnLockScreen)
            editor.putString(KEY_DOUBLE_TAP_ACTION, updated.doubleTapAction.name)
            editor.putString(KEY_FIXED_ANCHOR, updated.fixedScreenAnchor)

            editor.putLong(KEY_COLOR_IDLE, updated.idleColor)
            editor.putLong(KEY_COLOR_LISTENING, updated.listeningColor)
            editor.putLong(KEY_COLOR_WAITING, updated.waitingColor)
            editor.putLong(KEY_COLOR_PROCESSING, updated.processingColor)
            editor.putLong(KEY_COLOR_ERROR, updated.errorColor)
            editor.putLong(KEY_COLOR_RING, updated.ringColor)
            editor.putLong(KEY_COLOR_ICON, updated.iconColor)

            editor.putBoolean(KEY_BEEP_SUPPRESSION, updated.beepSuppression)
            editor.putBoolean(KEY_SOFT_TONES, updated.softTones)
            editor.putBoolean(KEY_HAPTICS_ENABLED, updated.hapticsEnabled)
            editor.putBoolean(KEY_KEEP_SCREEN_AWAKE, updated.keepScreenAwake)
            editor.putBoolean(KEY_DUCK_MEDIA, updated.duckMediaAudio)

            editor.putBoolean(KEY_AUTO_PUNCTUATE, updated.autoPunctuate)
            editor.putString(KEY_DIGIT_CONVERSION, updated.digitConversionModeName)
            editor.putBoolean(KEY_AUTO_CAP_LATIN, updated.autoCapitalizeLatin)
            editor.putBoolean(KEY_VOICE_COMMANDS, updated.voiceCommandsEnabled)
            editor.putString(KEY_VOICE_COMMANDS_JSON, updated.voiceCommandsJson)
            editor.putBoolean(KEY_CONFIRM_SENSITIVE_COMMANDS, updated.confirmSensitiveCommands)
            editor.putString(KEY_REPLACEMENTS_JSON, updated.replacementsJson)

            editor.putBoolean(KEY_KEEP_ACCESSIBILITY_ALIVE, updated.keepAccessibilityAlive)
            editor.putBoolean(KEY_SHOW_BOOT_NOTIF, updated.showBootNotification)

            editor.putString(KEY_ACTIVE_PROFILE, updated.activeProfileId)
            editor.putBoolean(KEY_HISTORY_ENABLED, updated.historyEnabled)
            editor.putInt(KEY_MAX_HISTORY, updated.maxHistoryItems)
            editor.putString(KEY_APP_THEME, updated.appTheme.name)
            editor.putString(KEY_ACCENT_INK, updated.accentInk.name)

            editor.apply()
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepo", "Failed to persist settings", e)
        }
    }

    fun getSavedOrbPosition(): Pair<Float, Float> {
        val x = try { prefs.getFloat(KEY_ORB_X, 0.90f) } catch (e: Exception) { 0.90f }
        val y = try { prefs.getFloat(KEY_ORB_Y, 0.50f) } catch (e: Exception) { 0.50f }
        return Pair(x.coerceIn(0.02f, 0.98f), y.coerceIn(0.05f, 0.95f))
    }

    fun saveOrbPosition(x: Float, y: Float) {
        val safeX = x.coerceIn(0.02f, 0.98f)
        val safeY = y.coerceIn(0.05f, 0.95f)
        try {
            prefs.edit().putFloat(KEY_ORB_X, safeX).putFloat(KEY_ORB_Y, safeY).apply()
        } catch (e: Exception) {
            try { prefs.edit().putFloat(KEY_ORB_X, safeX).putFloat(KEY_ORB_Y, safeY).commit() } catch (ignored: Exception) {}
        }
    }

    fun setScreenAnchor(anchor: String) {
        val coords = when (anchor) {
            "RIGHT_TOP" -> Pair(0.92f, 0.18f)
            "RIGHT_MIDDLE" -> Pair(0.92f, 0.50f)
            "RIGHT_BOTTOM" -> Pair(0.92f, 0.80f)
            "LEFT_TOP" -> Pair(0.08f, 0.18f)
            "LEFT_MIDDLE" -> Pair(0.08f, 0.50f)
            "LEFT_BOTTOM" -> Pair(0.08f, 0.80f)
            else -> Pair(0.92f, 0.50f)
        }
        saveOrbPosition(coords.first, coords.second)
        updateSettings { it.copy(fixedScreenAnchor = anchor) }
    }

    fun resetToDefaults() {
        updateSettings { KothaSettings() }
    }

    fun getExportJson(): String {
        val pos = getSavedOrbPosition()
        return cachedSettings.copy(
            orbPositionX = pos.first,
            orbPositionY = pos.second
        ).toJson()
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val imported = KothaSettings.fromJson(jsonString)
            saveOrbPosition(imported.orbPositionX, imported.orbPositionY)
            updateSettings { imported }
            true
        } catch (e: Exception) {
            android.util.Log.e("SettingsRepo", "Failed to import settings", e)
            false
        }
    }

    companion object {
        private const val KEY_PRIMARY_LANG = "primary_language"
        private const val KEY_SECONDARY_LANG = "secondary_language"
        private const val KEY_CUSTOM_BCP47 = "custom_bcp47"
        private const val KEY_PREFER_OFFLINE = "prefer_offline"
        private const val KEY_USE_ON_DEVICE = "use_on_device"
        private const val KEY_MAX_RESULTS = "max_results"
        private const val KEY_MASK_OFFENSIVE = "mask_offensive"
        private const val KEY_ENABLE_FORMATTING = "enable_formatting"

        private const val KEY_DICTATION_MODE = "dictation_mode"
        private const val KEY_RESTART_DELAY_MS = "restart_delay_ms"
        private const val KEY_SEAMLESS_RESTART_MS = "seamless_restart_ms"
        private const val KEY_SILENCE_COMPLETE_MS = "silence_complete_ms"
        private const val KEY_SILENCE_POSS_MS = "silence_poss_ms"
        private const val KEY_MIN_UTTERANCE = "min_utterance"
        private const val KEY_COMMIT_PARTIAL_LONG_SILENCE = "commit_partial_long_silence"
        private const val KEY_MAX_SESSION_SEC = "max_session_sec"
        private const val KEY_IDLE_AUTO_STOP_SEC = "idle_auto_stop_sec"
        private const val KEY_ERROR_BACKOFF_MS = "error_backoff_ms"
        private const val KEY_MAX_CONSEC_FAILURES = "max_consec_failures"
        private const val KEY_PREFER_SEGMENTED = "prefer_segmented"

        private const val KEY_STREAMING_STYLE = "streaming_style"
        private const val KEY_THROTTLE_MS = "throttle_ms"
        private const val KEY_SEPARATOR = "separator"
        private const val KEY_AUTO_LEADING_SPACE = "auto_leading_space"
        private const val KEY_ADD_TRAILING_SPACE = "add_trailing_space"
        private const val KEY_FILTER_HINT_PLACEHOLDERS = "filter_hint_placeholders"
        private const val KEY_REPLACE_SELECTION = "replace_selection"
        private const val KEY_FOLLOW_CURSOR = "follow_cursor"
        private const val KEY_INSERTION_METHOD = "insertion_method"
        private const val KEY_REFUSE_PASSWORDS = "refuse_passwords"
        private const val KEY_STOP_FOCUS_LOSS = "stop_focus_loss"
        private const val KEY_SHOW_CAPTION_BUBBLE = "show_caption_bubble"
        private const val KEY_CAPTION_POSITION = "caption_position"
        private const val KEY_CAPTION_SIZE = "caption_size"
        private const val KEY_CAPTION_MAX_LINES = "caption_max_lines"
        private const val KEY_CAPTION_STYLE = "caption_style"
        private const val KEY_CAPTION_LINGER_MS = "caption_linger_ms"
        private const val KEY_TAP_CAPTION_TO_UNDO = "tap_caption_to_undo"
        private const val KEY_UNDO_BADGE_STYLE = "undo_badge_style"
        private const val KEY_UNDO_BADGE_LABEL_MODE = "undo_badge_label_mode"
        private const val KEY_UNDO_BADGE_SIZE = "undo_badge_size"
        private const val KEY_UNDO_BADGE_PULSE = "undo_badge_pulse"
        private const val KEY_UNDO_BADGE_CUSTOM_COLOR = "undo_badge_custom_color"
        private const val KEY_UNDO_BUTTON_OPACITY = "undo_button_opacity"
        private const val KEY_UNDO_BUTTON_HORIZONTAL_GAP_DP = "undo_button_horizontal_gap_dp"
        private const val KEY_VOICE_BTN_OFFSET = "voice_btn_vertical_offset"
        private const val KEY_TRANSCRIPTION_OFFSET = "transcription_vertical_offset"

        private const val KEY_ORB_SIZE = "orb_size"
        private const val KEY_ORB_SHAPE = "orb_shape"
        private const val KEY_IDLE_OPACITY = "idle_opacity"
        private const val KEY_ACTIVE_OPACITY = "active_opacity"
        private const val KEY_RING_STYLE = "ring_style"
        private const val KEY_ANIM_SPEED = "anim_speed"
        private const val KEY_OUTLINE_THICKNESS = "outline_thickness"
        private const val KEY_SHADOW_STRENGTH = "shadow_strength"
        private const val KEY_EDGE_MARGIN = "edge_margin"
        private const val KEY_HAPTIC_STRENGTH = "haptic_strength"
        private const val KEY_LOCK_POSITION = "lock_position"
        private const val KEY_SNAP_TO_EDGE = "snap_to_edge"
        private const val KEY_DOCK_ON_KEYBOARD = "dock_on_keyboard"
        private const val KEY_VISIBILITY_RULE = "visibility_rule"
        private const val KEY_HIDE_ON_LOCK = "hide_on_lock"
        private const val KEY_DOUBLE_TAP_ACTION = "double_tap_action"

        private const val KEY_COLOR_IDLE = "color_idle"
        private const val KEY_COLOR_LISTENING = "color_listening"
        private const val KEY_COLOR_WAITING = "color_waiting"
        private const val KEY_COLOR_PROCESSING = "color_processing"
        private const val KEY_COLOR_ERROR = "color_error"
        private const val KEY_COLOR_RING = "color_ring"
        private const val KEY_COLOR_ICON = "color_icon"

        private const val KEY_BEEP_SUPPRESSION = "beep_suppression"
        private const val KEY_SOFT_TONES = "soft_tones"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_KEEP_SCREEN_AWAKE = "keep_screen_awake"
        private const val KEY_DUCK_MEDIA = "duck_media"

        private const val KEY_AUTO_PUNCTUATE = "auto_punctuate"
        private const val KEY_DIGIT_CONVERSION = "digit_conversion"
        private const val KEY_AUTO_CAP_LATIN = "auto_cap_latin"
        private const val KEY_VOICE_COMMANDS = "voice_commands"
        private const val KEY_VOICE_COMMANDS_JSON = "voice_commands_json"
        private const val KEY_CONFIRM_SENSITIVE_COMMANDS = "confirm_sensitive_commands"
        private const val KEY_REPLACEMENTS_JSON = "replacements_json"

        private const val KEY_KEEP_ACCESSIBILITY_ALIVE = "keep_accessibility_alive"
        private const val KEY_SHOW_BOOT_NOTIF = "show_boot_notif"

        private const val KEY_ACTIVE_PROFILE = "active_profile"
        private const val KEY_HISTORY_ENABLED = "history_enabled"
        private const val KEY_MAX_HISTORY = "max_history"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_ACCENT_INK = "accent_ink"

        private const val KEY_ORB_X = "orb_normalized_x"
        private const val KEY_ORB_Y = "orb_normalized_y"
        private const val KEY_FIXED_ANCHOR = "fixed_screen_anchor"
        private const val KEY_SETTINGS_JSON_BACKUP = "settings_json_backup"
    }
}
