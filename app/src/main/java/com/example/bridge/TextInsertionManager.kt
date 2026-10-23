package com.example.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.settings.InsertionMethod
import com.example.settings.KothaSettings
import com.example.settings.StreamingStyle
import com.example.util.DiagnosticsLog
import com.example.util.LanguageTools
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TextInsertionManager(
    private val context: Context,
    private val spanTracker: SpanTracker = SpanTracker()
) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    private var lastPartialInsertTime = 0L

    private val _liveCaptionText = MutableStateFlow("")
    val liveCaptionText: StateFlow<String> = _liveCaptionText.asStateFlow()

    private val _lastInsertedUtterance = MutableStateFlow("")
    val lastInsertedUtterance: StateFlow<String> = _lastInsertedUtterance.asStateFlow()

    fun handlePartialResult(
        partialText: String,
        settings: KothaSettings
    ) {
        _liveCaptionText.value = partialText

        if (settings.streamingStyle == StreamingStyle.ON_SENTENCE_END) {
            // Wait for final result
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastPartialInsertTime < settings.throttleIntervalMs) {
            return
        }
        lastPartialInsertTime = now

        insertTextInternal(
            text = partialText,
            isFinal = false,
            settings = settings
        )
    }

    fun handleFinalResult(
        rawFinalText: String,
        settings: KothaSettings,
        onVoiceCommand: ((LanguageTools.VoiceCommandResult) -> Unit)? = null,
        onConfirmCommand: ((actionName: String, rawText: String, onApply: () -> Unit, onInsertAsText: () -> Unit) -> Unit)? = null
    ) {
        val trimmed = rawFinalText.trim()
        if (trimmed.isEmpty()) {
            _liveCaptionText.value = ""
            return
        }

        // 1. Check Voice Commands if enabled
        if (settings.voiceCommandsEnabled) {
            val commands = LanguageTools.commandsFromJson(settings.voiceCommandsJson)
            val cmd = LanguageTools.checkVoiceCommand(trimmed, commands)
            if (cmd !is LanguageTools.VoiceCommandResult.None) {
                val executeAction = {
                    executeCommandAction(cmd, settings, onVoiceCommand)
                    _liveCaptionText.value = ""
                }
                val insertAsText = {
                    insertFormattedFinal(trimmed, settings)
                }

                if (settings.confirmSensitiveCommands && LanguageTools.isSensitiveCommand(cmd) && onConfirmCommand != null) {
                    val actionName = LanguageTools.getCommandName(cmd)
                    onConfirmCommand.invoke(actionName, trimmed, executeAction, insertAsText)
                    _liveCaptionText.value = ""
                    return
                }

                executeAction()
                return
            }
        }

        // 2. Format with Language Tools
        insertFormattedFinal(trimmed, settings)
    }

    private fun executeCommandAction(
        cmd: LanguageTools.VoiceCommandResult,
        settings: KothaSettings,
        onVoiceCommand: ((LanguageTools.VoiceCommandResult) -> Unit)?
    ) {
        DiagnosticsLog.log(DiagnosticsLog.Category.INSERTION, "Executed voice command: $cmd")
        onVoiceCommand?.invoke(cmd)
        when (cmd) {
            LanguageTools.VoiceCommandResult.NewLine -> insertTextInternal("\n", isFinal = true, settings = settings)
            LanguageTools.VoiceCommandResult.NewParagraph -> insertTextInternal("\n\n", isFinal = true, settings = settings)
            LanguageTools.VoiceCommandResult.DeleteLastWord -> TextBridgeService.instance?.deleteLastWord()
            LanguageTools.VoiceCommandResult.DeleteLastSentence -> TextBridgeService.instance?.deleteLastSentence()
            LanguageTools.VoiceCommandResult.ClearAll -> TextBridgeService.instance?.clearAll()
            LanguageTools.VoiceCommandResult.Send -> TextBridgeService.instance?.applySend()
            LanguageTools.VoiceCommandResult.Undo -> performUndo()
            LanguageTools.VoiceCommandResult.AddComma -> insertTextInternal(", ", isFinal = true, settings = settings)
            LanguageTools.VoiceCommandResult.AddQuestionMark -> insertTextInternal("? ", isFinal = true, settings = settings)
            LanguageTools.VoiceCommandResult.AddDari -> {
                val symbol = if (settings.primaryLanguage.startsWith("bn", ignoreCase = true)) "। " else ". "
                insertTextInternal(symbol, isFinal = true, settings = settings)
            }
            LanguageTools.VoiceCommandResult.Capitalize -> TextBridgeService.instance?.capitalizeLastWord()
            LanguageTools.VoiceCommandResult.SelectAll -> TextBridgeService.instance?.applySelectAll()
            LanguageTools.VoiceCommandResult.CopyText -> TextBridgeService.instance?.applyCopy(context)
            LanguageTools.VoiceCommandResult.MoveToEnd -> TextBridgeService.instance?.moveCursorToEnd()
            LanguageTools.VoiceCommandResult.MoveToStart -> TextBridgeService.instance?.moveCursorToStart()
            else -> {}
        }
    }

    private fun insertFormattedFinal(rawText: String, settings: KothaSettings) {
        val replacements = LanguageTools.rulesFromJson(settings.replacementsJson)
        val digitMode = try {
            LanguageTools.DigitConversionMode.valueOf(settings.digitConversionModeName)
        } catch (e: Exception) {
            LanguageTools.DigitConversionMode.NONE
        }

        val formattedText = LanguageTools.formatFinalUtterance(
            rawText = rawText,
            languageCode = settings.primaryLanguage,
            autoPunctuate = settings.autoPunctuate,
            digitMode = digitMode,
            autoCapLatin = settings.autoCapitalizeLatin,
            replacements = replacements
        )

        insertTextInternal(
            text = formattedText,
            isFinal = true,
            settings = settings
        )

        _lastInsertedUtterance.value = formattedText
        _liveCaptionText.value = ""
    }

    private fun insertTextInternal(
        text: String,
        isFinal: Boolean,
        settings: KothaSettings
    ) {
        val service = TextBridgeService.instance
        val snapshot = service?.getLiveFocusedSnapshot() ?: TextBridgeStateHolder.focusedSnapshot.value

        if (service == null || snapshot == null) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.INSERTION,
                "Cannot insert: Bridge not connected or no focused field"
            )
            return
        }

        if (settings.refusePasswordFields && snapshot.isPassword) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.INSERTION,
                "Refusing insertion into password field"
            )
            return
        }

        val currentText = snapshot.currentText
        val cursor = if (snapshot.selectionStart >= 0) snapshot.selectionStart else currentText.length

        val result = if (isFinal) {
            spanTracker.calculateFinalCommit(
                actualCurrentText = currentText,
                currentCursor = cursor,
                finalText = text,
                separator = settings.separator,
                followCursor = settings.followCursor,
                autoLeadingSpace = settings.autoLeadingSpace,
                addTrailingSpace = settings.addTrailingSpace
            )
        } else {
            spanTracker.calculatePartialInsertion(
                actualCurrentText = currentText,
                currentCursor = cursor,
                partialText = text,
                followCursor = settings.followCursor,
                autoLeadingSpace = settings.autoLeadingSpace
            )
        }

        applyInsertionResult(result, settings, service)
    }

    private fun applyInsertionResult(
        result: SpanTracker.InsertionResult,
        settings: KothaSettings,
        service: TextBridgeService
    ) {
        when (settings.insertionMethod) {
            InsertionMethod.ACCESSIBILITY_SET_TEXT -> {
                service.applySetText(result.newFullText)
                service.applySelection(result.newSelection, result.newSelection)
            }
            InsertionMethod.CLIPBOARD_PASTE -> {
                performClipboardPaste(result.updatedSpan.lastInserted, service)
            }
            InsertionMethod.AUTO -> {
                val success = service.applySetText(result.newFullText)
                if (success) {
                    service.applySelection(result.newSelection, result.newSelection)
                } else {
                    // Fall back to clipboard paste
                    DiagnosticsLog.log(
                        DiagnosticsLog.Category.INSERTION,
                        "SET_TEXT returned false, falling back to clipboard paste"
                    )
                    performClipboardPaste(result.updatedSpan.lastInserted, service)
                }
            }
        }
    }

    private fun performClipboardPaste(textToPaste: String, service: TextBridgeService) {
        val cm = clipboardManager ?: return
        try {
            val snapshot = TextBridgeStateHolder.focusedSnapshot.value
            if (snapshot?.isShowingHintText == true) {
                // Ensure placeholder text is cleared and selected before paste so it is never prepended
                service.applySetText("")
                service.applySelectAll()
            }

            // Save existing clipboard
            val previousClip = cm.primaryClip

            // Set new text
            val clip = ClipData.newPlainText("Kotha", textToPaste)
            cm.setPrimaryClip(clip)

            // Perform paste
            service.applyPaste()

            // Restore previous clipboard
            if (previousClip != null) {
                cm.setPrimaryClip(previousClip)
            }
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "Error during clipboard paste: ${e.message}"
            )
        }
    }

    fun performUndo(): Boolean {
        val service = TextBridgeService.instance ?: return false
        val snapshot = TextBridgeStateHolder.focusedSnapshot.value ?: return false

        val undoResult = spanTracker.calculateUndo(snapshot.currentText) ?: return false
        val (undoneText, cursor) = undoResult

        val success = service.applySetText(undoneText)
        if (success) {
            service.applySelection(cursor, cursor)
            DiagnosticsLog.log(DiagnosticsLog.Category.INSERTION, "Undo successfully applied")
            return true
        }
        return false
    }

    fun reset() {
        spanTracker.reset()
        _liveCaptionText.value = ""
    }
}
