package com.example.settings

import org.json.JSONArray
import org.json.JSONObject

enum class DictationMode {
    CONTINUOUS,
    PAUSE_AND_RESTART
}

enum class StreamingStyle {
    LIVE,
    ON_SENTENCE_END
}

enum class InsertionMethod {
    AUTO,
    ACCESSIBILITY_SET_TEXT,
    CLIPBOARD_PASTE
}

enum class CaptionBubblePosition {
    ABOVE_ORB,
    BELOW_ORB,
    TOP_BANNER
}

enum class CaptionBubbleSize {
    COMPACT,
    MEDIUM,
    LARGE,
    EXTRA_LARGE
}

enum class CaptionBubbleStyle {
    TRANSLUCENT_DARK,
    SOLID_AMOLED,
    ACCENT_THEMED
}

enum class OrbShape {
    CIRCLE,
    SQUIRCLE,
    ROUNDED_SQUARE,
    SEAL
}

enum class RingStyle {
    RADIAL_TICKS,
    SOLID_ARC,
    DOTTED,
    NONE
}

enum class VisibilityRule {
    ALWAYS,
    EDITABLE_FOCUSED,
    KEYBOARD_VISIBLE
}

enum class GestureAction {
    SWITCH_MODE,
    SWITCH_LANGUAGE,
    UNDO,
    NONE
}

enum class AppTheme {
    DARK,
    PAPER,
    AMOLED
}

enum class AccentInk(val hexColor: Long, val displayName: String) {
    VERMILION(0xFFF2542D, "Vermilion (সিঁদুর)"),
    OCHRE(0xFFE3A93B, "Ochre (গেরুয়া)"),
    VERDIGRIS(0xFF45A596, "Verdigris (তামাটে)"),
    INDIGO(0xFF4D62CD, "Indigo (নীল)")
}

data class Profile(
    val id: String,
    val name: String,
    val primaryLanguage: String,
    val mode: DictationMode,
    val autoPunctuate: Boolean
)

data class KothaSettings(
    // 01 Voice
    val enginePackage: String = "com.google.android.googlequicksearchbox",
    val engineService: String = "com.google.android.voicesearch.serviceapi.GoogleRecognitionService",
    val primaryLanguage: String = "bn-BD",
    val secondaryLanguage: String = "en-US",
    val isSecondaryLanguageActive: Boolean = false,
    val customBcp47: String = "",
    val preferOffline: Boolean = false,
    val useOnDevice: Boolean = false,
    val maxResults: Int = 1,
    val maskOffensiveWords: Boolean = false,
    val enableFormatting: Boolean = true,

    // 02 Mode
    val dictationMode: DictationMode = DictationMode.CONTINUOUS,
    val restartDelayMs: Long = 400L,
    val seamlessRestartMs: Long = 237L,
    val silenceCompleteMs: Long = 4665L,
    val silencePossiblyCompleteMs: Long = 4665L,
    val minUtteranceLength: Int = 1,
    val commitPartialOnLongSilence: Boolean = true,
    val maxSessionLengthSec: Int = 300,
    val idleAutoStopSec: Int = 30,
    val errorBackoffMs: Long = 500L,
    val maxConsecutiveFailures: Int = 4,
    val preferSegmentedSession: Boolean = true,

    // 03 Insertion
    val streamingStyle: StreamingStyle = StreamingStyle.ON_SENTENCE_END,
    val throttleIntervalMs: Long = 51L,
    val separator: String = " ",
    val autoLeadingSpace: Boolean = true,
    val addTrailingSpace: Boolean = false,
    val filterHintPlaceholders: Boolean = true,
    val replaceSelection: Boolean = true,
    val followCursor: Boolean = true,
    val insertionMethod: InsertionMethod = InsertionMethod.AUTO,
    val refusePasswordFields: Boolean = true,
    val stopOnFocusLoss: Boolean = false,
    val showLiveCaptionBubble: Boolean = true,
    val captionBubblePosition: CaptionBubblePosition = CaptionBubblePosition.ABOVE_ORB,
    val captionBubbleSize: CaptionBubbleSize = CaptionBubbleSize.COMPACT,
    val captionMaxLines: Int = 2,
    val captionBubbleStyle: CaptionBubbleStyle = CaptionBubbleStyle.TRANSLUCENT_DARK,
    val captionLingerDurationMs: Long = 1000L,

    // 04 Orb
    val orbSizeDp: Int = 28,
    val orbShape: OrbShape = OrbShape.CIRCLE,
    val idleOpacity: Float = 0.85f,
    val activeOpacity: Float = 1.0f,
    val ringStyle: RingStyle = RingStyle.RADIAL_TICKS,
    val animationSpeed: Float = 1.0f,
    val outlineThicknessDp: Float = 1.5f,
    val shadowStrength: Float = 0.4f,
    val edgeMarginDp: Int = 8,
    val hapticStrength: Float = 1.0f,
    val lockPosition: Boolean = true,
    val snapToEdge: Boolean = true,
    val dockToEdgeOnKeyboard: Boolean = false,
    val visibilityRule: VisibilityRule = VisibilityRule.KEYBOARD_VISIBLE,
    val hideOnLockScreen: Boolean = true,
    val doubleTapAction: GestureAction = GestureAction.SWITCH_LANGUAGE,
    val fixedScreenAnchor: String = "RIGHT_MIDDLE",
    val orbPositionX: Float = 0.90f,
    val orbPositionY: Float = 0.50f,

    // Colors
    val idleColor: Long = 4279702802L,
    val listeningColor: Long = 4282754454L,
    val waitingColor: Long = 4293110075L,
    val processingColor: Long = 4282754454L,
    val errorColor: Long = 4291310140L,
    val ringColor: Long = 4293715674L,
    val iconColor: Long = 4293715674L,

    // 05 Sound & Feel
    val beepSuppression: Boolean = true,
    val softTones: Boolean = true,
    val hapticsEnabled: Boolean = false,
    val keepScreenAwake: Boolean = false,
    val duckMediaAudio: Boolean = false,

    // 06 Language tools
    val autoPunctuate: Boolean = false,
    val digitConversionModeName: String = "NONE",
    val autoCapitalizeLatin: Boolean = false,
    val voiceCommandsEnabled: Boolean = true,
    val voiceCommandsJson: String = "",
    val confirmSensitiveCommands: Boolean = false,
    val replacementsJson: String = "",

    // 07 System
    val keepAccessibilityAlive: Boolean = true,
    val showBootNotification: Boolean = true,

    // 08 Data
    val activeProfileId: String = "default_bangla",
    val historyEnabled: Boolean = false,
    val maxHistoryItems: Int = 50,
    val appTheme: AppTheme = AppTheme.DARK,
    val accentInk: AccentInk = AccentInk.VERMILION
) {
    fun getActiveDictationLanguage(): String {
        return if (isSecondaryLanguageActive) {
            secondaryLanguage.ifBlank { "en-US" }
        } else {
            if (customBcp47.isNotBlank()) customBcp47 else primaryLanguage.ifBlank { "bn-BD" }
        }
    }

    fun toJson(): String {
        val root = JSONObject()
        root.put("primaryLanguage", primaryLanguage)
        root.put("secondaryLanguage", secondaryLanguage)
        root.put("isSecondaryLanguageActive", isSecondaryLanguageActive)
        root.put("customBcp47", customBcp47)
        root.put("preferOffline", preferOffline)
        root.put("useOnDevice", useOnDevice)
        root.put("maxResults", maxResults)
        root.put("maskOffensiveWords", maskOffensiveWords)
        root.put("enableFormatting", enableFormatting)

        root.put("dictationMode", dictationMode.name)
        root.put("restartDelayMs", restartDelayMs)
        root.put("seamlessRestartMs", seamlessRestartMs)
        root.put("silenceCompleteMs", silenceCompleteMs)
        root.put("silencePossiblyCompleteMs", silencePossiblyCompleteMs)
        root.put("minUtteranceLength", minUtteranceLength)
        root.put("commitPartialOnLongSilence", commitPartialOnLongSilence)
        root.put("maxSessionLengthSec", maxSessionLengthSec)
        root.put("idleAutoStopSec", idleAutoStopSec)
        root.put("errorBackoffMs", errorBackoffMs)
        root.put("maxConsecutiveFailures", maxConsecutiveFailures)
        root.put("preferSegmentedSession", preferSegmentedSession)

        root.put("streamingStyle", streamingStyle.name)
        root.put("throttleIntervalMs", throttleIntervalMs)
        root.put("separator", separator)
        root.put("autoLeadingSpace", autoLeadingSpace)
        root.put("addTrailingSpace", addTrailingSpace)
        root.put("filterHintPlaceholders", filterHintPlaceholders)
        root.put("replaceSelection", replaceSelection)
        root.put("followCursor", followCursor)
        root.put("insertionMethod", insertionMethod.name)
        root.put("refusePasswordFields", refusePasswordFields)
        root.put("stopOnFocusLoss", stopOnFocusLoss)
        root.put("showLiveCaptionBubble", showLiveCaptionBubble)
        root.put("captionBubblePosition", captionBubblePosition.name)
        root.put("captionBubbleSize", captionBubbleSize.name)
        root.put("captionMaxLines", captionMaxLines)
        root.put("captionBubbleStyle", captionBubbleStyle.name)
        root.put("captionLingerDurationMs", captionLingerDurationMs)

        root.put("orbSizeDp", orbSizeDp)
        root.put("orbShape", orbShape.name)
        root.put("idleOpacity", idleOpacity.toDouble())
        root.put("activeOpacity", activeOpacity.toDouble())
        root.put("ringStyle", ringStyle.name)
        root.put("animationSpeed", animationSpeed.toDouble())
        root.put("outlineThicknessDp", outlineThicknessDp.toDouble())
        root.put("shadowStrength", shadowStrength.toDouble())
        root.put("edgeMarginDp", edgeMarginDp)
        root.put("hapticStrength", hapticStrength.toDouble())
        root.put("lockPosition", lockPosition)
        root.put("snapToEdge", snapToEdge)
        root.put("dockToEdgeOnKeyboard", dockToEdgeOnKeyboard)
        root.put("visibilityRule", visibilityRule.name)
        root.put("hideOnLockScreen", hideOnLockScreen)
        root.put("doubleTapAction", doubleTapAction.name)
        root.put("fixedScreenAnchor", fixedScreenAnchor)
        root.put("orbPositionX", orbPositionX.toDouble())
        root.put("orbPositionY", orbPositionY.toDouble())

        root.put("idleColor", idleColor)
        root.put("listeningColor", listeningColor)
        root.put("waitingColor", waitingColor)
        root.put("processingColor", processingColor)
        root.put("errorColor", errorColor)
        root.put("ringColor", ringColor)
        root.put("iconColor", iconColor)

        root.put("beepSuppression", beepSuppression)
        root.put("softTones", softTones)
        root.put("hapticsEnabled", hapticsEnabled)
        root.put("keepScreenAwake", keepScreenAwake)
        root.put("duckMediaAudio", duckMediaAudio)

        root.put("autoPunctuate", autoPunctuate)
        root.put("digitConversionModeName", digitConversionModeName)
        root.put("autoCapitalizeLatin", autoCapitalizeLatin)
        root.put("voiceCommandsEnabled", voiceCommandsEnabled)
        root.put("voiceCommandsJson", voiceCommandsJson)
        root.put("confirmSensitiveCommands", confirmSensitiveCommands)
        root.put("replacementsJson", replacementsJson)

        root.put("keepAccessibilityAlive", keepAccessibilityAlive)
        root.put("showBootNotification", showBootNotification)
        root.put("activeProfileId", activeProfileId)
        root.put("historyEnabled", historyEnabled)
        root.put("maxHistoryItems", maxHistoryItems)
        root.put("appTheme", appTheme.name)
        root.put("accentInk", accentInk.name)
        return root.toString(2)
    }

    companion object {
        fun fromJson(jsonStr: String): KothaSettings {
            val root = JSONObject(jsonStr)
            val base = KothaSettings()
            return base.copy(
                primaryLanguage = root.optString("primaryLanguage", base.primaryLanguage),
                secondaryLanguage = root.optString("secondaryLanguage", base.secondaryLanguage),
                isSecondaryLanguageActive = root.optBoolean("isSecondaryLanguageActive", base.isSecondaryLanguageActive),
                customBcp47 = root.optString("customBcp47", base.customBcp47),
                preferOffline = root.optBoolean("preferOffline", base.preferOffline),
                useOnDevice = root.optBoolean("useOnDevice", base.useOnDevice),
                maxResults = root.optInt("maxResults", base.maxResults),
                maskOffensiveWords = root.optBoolean("maskOffensiveWords", base.maskOffensiveWords),
                enableFormatting = root.optBoolean("enableFormatting", base.enableFormatting),

                dictationMode = try {
                    DictationMode.valueOf(root.optString("dictationMode", base.dictationMode.name))
                } catch (e: Exception) { base.dictationMode },
                restartDelayMs = root.optLong("restartDelayMs", base.restartDelayMs),
                seamlessRestartMs = root.optLong("seamlessRestartMs", base.seamlessRestartMs),
                silenceCompleteMs = root.optLong("silenceCompleteMs", base.silenceCompleteMs),
                silencePossiblyCompleteMs = root.optLong("silencePossiblyCompleteMs", base.silencePossiblyCompleteMs),
                minUtteranceLength = root.optInt("minUtteranceLength", base.minUtteranceLength),
                commitPartialOnLongSilence = root.optBoolean("commitPartialOnLongSilence", base.commitPartialOnLongSilence),
                maxSessionLengthSec = root.optInt("maxSessionLengthSec", base.maxSessionLengthSec),
                idleAutoStopSec = root.optInt("idleAutoStopSec", base.idleAutoStopSec),
                errorBackoffMs = root.optLong("errorBackoffMs", base.errorBackoffMs),
                maxConsecutiveFailures = root.optInt("maxConsecutiveFailures", base.maxConsecutiveFailures),
                preferSegmentedSession = root.optBoolean("preferSegmentedSession", base.preferSegmentedSession),

                streamingStyle = try {
                    StreamingStyle.valueOf(root.optString("streamingStyle", base.streamingStyle.name))
                } catch (e: Exception) { base.streamingStyle },
                throttleIntervalMs = root.optLong("throttleIntervalMs", base.throttleIntervalMs),
                separator = root.optString("separator", base.separator),
                autoLeadingSpace = root.optBoolean("autoLeadingSpace", base.autoLeadingSpace),
                addTrailingSpace = root.optBoolean("addTrailingSpace", base.addTrailingSpace),
                filterHintPlaceholders = root.optBoolean("filterHintPlaceholders", base.filterHintPlaceholders),
                replaceSelection = root.optBoolean("replaceSelection", base.replaceSelection),
                followCursor = root.optBoolean("followCursor", base.followCursor),
                insertionMethod = try {
                    InsertionMethod.valueOf(root.optString("insertionMethod", base.insertionMethod.name))
                } catch (e: Exception) { base.insertionMethod },
                refusePasswordFields = root.optBoolean("refusePasswordFields", base.refusePasswordFields),
                stopOnFocusLoss = root.optBoolean("stopOnFocusLoss", base.stopOnFocusLoss),
                showLiveCaptionBubble = root.optBoolean("showLiveCaptionBubble", base.showLiveCaptionBubble),
                captionBubblePosition = try {
                    CaptionBubblePosition.valueOf(root.optString("captionBubblePosition", base.captionBubblePosition.name))
                } catch (e: Exception) { base.captionBubblePosition },
                captionBubbleSize = try {
                    CaptionBubbleSize.valueOf(root.optString("captionBubbleSize", base.captionBubbleSize.name))
                } catch (e: Exception) { base.captionBubbleSize },
                captionMaxLines = root.optInt("captionMaxLines", base.captionMaxLines).coerceIn(1, 3),
                captionBubbleStyle = try {
                    CaptionBubbleStyle.valueOf(root.optString("captionBubbleStyle", base.captionBubbleStyle.name))
                } catch (e: Exception) { base.captionBubbleStyle },
                captionLingerDurationMs = root.optLong("captionLingerDurationMs", base.captionLingerDurationMs).coerceIn(500L, 5000L),

                orbSizeDp = root.optInt("orbSizeDp", base.orbSizeDp),
                orbShape = try {
                    OrbShape.valueOf(root.optString("orbShape", base.orbShape.name))
                } catch (e: Exception) { base.orbShape },
                idleOpacity = root.optDouble("idleOpacity", base.idleOpacity.toDouble()).toFloat(),
                activeOpacity = root.optDouble("activeOpacity", base.activeOpacity.toDouble()).toFloat(),
                ringStyle = try {
                    RingStyle.valueOf(root.optString("ringStyle", base.ringStyle.name))
                } catch (e: Exception) { base.ringStyle },
                animationSpeed = root.optDouble("animationSpeed", base.animationSpeed.toDouble()).toFloat(),
                outlineThicknessDp = root.optDouble("outlineThicknessDp", base.outlineThicknessDp.toDouble()).toFloat(),
                shadowStrength = root.optDouble("shadowStrength", base.shadowStrength.toDouble()).toFloat(),
                edgeMarginDp = root.optInt("edgeMarginDp", base.edgeMarginDp),
                hapticStrength = root.optDouble("hapticStrength", base.hapticStrength.toDouble()).toFloat(),
                lockPosition = root.optBoolean("lockPosition", base.lockPosition),
                snapToEdge = root.optBoolean("snapToEdge", base.snapToEdge),
                dockToEdgeOnKeyboard = root.optBoolean("dockToEdgeOnKeyboard", base.dockToEdgeOnKeyboard),
                visibilityRule = try {
                    VisibilityRule.valueOf(root.optString("visibilityRule", base.visibilityRule.name))
                } catch (e: Exception) { base.visibilityRule },
                hideOnLockScreen = root.optBoolean("hideOnLockScreen", base.hideOnLockScreen),
                doubleTapAction = try {
                    GestureAction.valueOf(root.optString("doubleTapAction", base.doubleTapAction.name))
                } catch (e: Exception) { base.doubleTapAction },
                fixedScreenAnchor = root.optString("fixedScreenAnchor", base.fixedScreenAnchor),
                orbPositionX = root.optDouble("orbPositionX", base.orbPositionX.toDouble()).toFloat(),
                orbPositionY = root.optDouble("orbPositionY", base.orbPositionY.toDouble()).toFloat(),

                idleColor = root.optLong("idleColor", base.idleColor),
                listeningColor = root.optLong("listeningColor", base.listeningColor),
                waitingColor = root.optLong("waitingColor", base.waitingColor),
                processingColor = root.optLong("processingColor", base.processingColor),
                errorColor = root.optLong("errorColor", base.errorColor),
                ringColor = root.optLong("ringColor", base.ringColor),
                iconColor = root.optLong("iconColor", base.iconColor),

                beepSuppression = root.optBoolean("beepSuppression", base.beepSuppression),
                softTones = root.optBoolean("softTones", base.softTones),
                hapticsEnabled = root.optBoolean("hapticsEnabled", base.hapticsEnabled),
                keepScreenAwake = root.optBoolean("keepScreenAwake", base.keepScreenAwake),
                duckMediaAudio = root.optBoolean("duckMediaAudio", base.duckMediaAudio),

                autoPunctuate = root.optBoolean("autoPunctuate", base.autoPunctuate),
                digitConversionModeName = root.optString("digitConversionModeName", base.digitConversionModeName),
                autoCapitalizeLatin = root.optBoolean("autoCapitalizeLatin", base.autoCapitalizeLatin),
                voiceCommandsEnabled = root.optBoolean("voiceCommandsEnabled", base.voiceCommandsEnabled),
                voiceCommandsJson = root.optString("voiceCommandsJson", base.voiceCommandsJson),
                confirmSensitiveCommands = root.optBoolean("confirmSensitiveCommands", base.confirmSensitiveCommands),
                replacementsJson = root.optString("replacementsJson", base.replacementsJson),

                keepAccessibilityAlive = root.optBoolean("keepAccessibilityAlive", base.keepAccessibilityAlive),
                showBootNotification = root.optBoolean("showBootNotification", base.showBootNotification),
                activeProfileId = root.optString("activeProfileId", base.activeProfileId),
                historyEnabled = root.optBoolean("historyEnabled", base.historyEnabled),
                maxHistoryItems = root.optInt("maxHistoryItems", base.maxHistoryItems),
                appTheme = try {
                    AppTheme.valueOf(root.optString("appTheme", base.appTheme.name))
                } catch (e: Exception) { base.appTheme },
                accentInk = try {
                    AccentInk.valueOf(root.optString("accentInk", base.accentInk.name))
                } catch (e: Exception) { base.accentInk }
            )
        }
    }
}
