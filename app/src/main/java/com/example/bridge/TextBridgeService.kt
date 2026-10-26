package com.example.bridge

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.example.util.DiagnosticsLog
import kotlinx.coroutines.launch
import java.util.Locale

class TextBridgeService : AccessibilityService() {

    override fun onServiceConnected() {
        try {
            instance = this
            TextBridgeStateHolder.updateConnected(true)
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ACCESSIBILITY,
                "TextBridgeService connected"
            )
            refreshFocusedNode()
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "Error in onServiceConnected: ${e.message}"
            )
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        try {
            when (event.eventType) {
                AccessibilityEvent.TYPE_VIEW_FOCUSED,
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                    refreshFocusedNode()
                }
                AccessibilityEvent.TYPE_WINDOWS_CHANGED,
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                    checkWindowsAndKeyboard()
                    refreshFocusedNode()
                }
            }
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "Error in onAccessibilityEvent: ${e.message}"
            )
        }
    }

    override fun onInterrupt() {
        try {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ACCESSIBILITY,
                "TextBridgeService onInterrupt called"
            )
        } catch (e: Exception) {
            // Safe
        }
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        try {
            TextBridgeStateHolder.updateConnected(false)
            instance = null
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ACCESSIBILITY,
                "TextBridgeService unbind"
            )
        } catch (e: Exception) {
            // Safe
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        try {
            TextBridgeStateHolder.updateConnected(false)
            instance = null
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ACCESSIBILITY,
                "TextBridgeService destroyed"
            )
        } catch (e: Exception) {
            // Safe
        }
        super.onDestroy()
    }

    private fun checkWindowsAndKeyboard() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val windows = windows ?: return
                var keyboardVisible = false
                var keyboardHeight = 0

                for (window in windows) {
                    if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
                        keyboardVisible = true
                        val bounds = Rect()
                        window.getBoundsInScreen(bounds)
                        keyboardHeight = bounds.height()
                    } else if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION && window.isFocused) {
                        val root = window.root
                        if (root != null) {
                            val pkg = root.packageName?.toString() ?: ""
                            TextBridgeStateHolder.updateForegroundPackage(pkg)
                            try { root.recycle() } catch (ignored: Exception) {}
                        }
                    }
                }
                TextBridgeStateHolder.updateKeyboard(keyboardVisible, keyboardHeight)
            }
        } catch (e: Exception) {
            // Ignore window query errors
        }
    }

    fun findFocusedInputNode(): AccessibilityNodeInfo? {
        try {
            // 1. Try findFocus on input
            val focused = findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (focused != null && (focused.isEditable || focused.isFocused)) {
                return focused
            }

            // 2. Fall back to search root in active window
            val root = rootInActiveWindow ?: return null
            val result = findEditableChild(root)
            if (result != null) {
                return result
            }
            try { root.recycle() } catch (ignored: Exception) {}
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "findFocusedInputNode error: ${e.message}"
            )
        }
        return null
    }

    private fun findEditableChild(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        try {
            if (node.isFocused && node.isEditable) {
                return node
            }
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i) ?: continue
                val found = findEditableChild(child)
                if (found != null) {
                    return found
                }
                try { child.recycle() } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            // Safe
        }
        return null
    }

    private val KNOWN_PLACEHOLDERS = setOf(
        "message", "type a message", "type message", "send a message", "type a message...",
        "write a message", "message...", "type a reply...", "reply...", "write a reply...",
        "text message", "new message", "say something", "say something...", "start typing...",
        "write something", "type something", "chat", "chat with meta ai", "ask meta ai",
        "ask meta ai or search", "ask anything", "what's on your mind", "whats on your mind",
        "বার্তা", "মেসেজ", "একটি বার্তা লিখুন", "বার্তা লিখুন", "কিছু লিখুন", "বলুন...",
        "search", "search...", "search or type url", "search or type web address",
        "search or type a web address", "search google or type a url", "search google or type url",
        "search or enter address", "search or type address", "type a url", "search here", "find",
        "query", "অনুসন্ধান", "খুঁজুন", "অনুসন্ধান করুন বা ওয়েব ঠিকানা লিখুন", "অনুসন্ধান করুন",
        "comment", "add a comment", "add a comment...", "comment...", "write a comment...",
        "leave a comment", "post a comment", "মন্তব্য", "মন্তব্য করুন", "মন্তব্য লিখুন"
    )

    private fun cleanForComparison(str: String): String {
        return str.replace(Regex("[\\u200B-\\u200F\\u202A-\\u202E\\u2066-\\u2069\\uFEFF\\u00A0\\s]+"), " ")
            .trim()
            .trimEnd('.', '…', ':', ' ')
            .lowercase()
    }

    private fun isPlaceholderMatch(cleanedRaw: String, cleanedHint: String?, cleanedDesc: String?): Boolean {
        if (cleanedRaw.isEmpty()) return true

        // 1. Exact or prefix match with hint attribute
        if (!cleanedHint.isNullOrBlank()) {
            if (cleanedRaw == cleanedHint || cleanedRaw.startsWith(cleanedHint) || cleanedHint.startsWith(cleanedRaw)) {
                return true
            }
        }

        // 2. Exact or prefix match with contentDescription (e.g. WhatsApp / Meta AI input box)
        if (!cleanedDesc.isNullOrBlank()) {
            if (cleanedRaw == cleanedDesc || cleanedRaw.startsWith(cleanedDesc) || cleanedDesc.startsWith(cleanedRaw)) {
                return true
            }
        }

        // 3. Known placeholders set
        if (cleanedRaw in KNOWN_PLACEHOLDERS) {
            return true
        }

        // 4. Common conversational & browser search prefixes
        if (cleanedRaw.startsWith("message") ||
            cleanedRaw.startsWith("type a message") ||
            cleanedRaw.startsWith("write a message") ||
            cleanedRaw.startsWith("search") ||
            cleanedRaw.startsWith("ask meta ai") ||
            cleanedRaw.startsWith("ask google") ||
            cleanedRaw.startsWith("type a url") ||
            cleanedRaw.startsWith("বার্তা") ||
            cleanedRaw.startsWith("মেসেজ") ||
            cleanedRaw.startsWith("অনুসন্ধান")
        ) {
            return true
        }

        return false
    }

    private fun extractActualFieldText(node: AccessibilityNodeInfo): Pair<String, Boolean> {
        val rawText = node.text?.toString() ?: ""
        if (rawText.isEmpty()) return Pair("", false)

        // 1. Explicit platform flag (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && node.isShowingHintText) {
            return Pair("", true)
        }

        val shouldFilter = try {
            (applicationContext as? com.example.KothaApplication)?.settingsRepository?.cachedSettings?.filterHintPlaceholders != false
        } catch (e: Exception) { true }

        if (!shouldFilter) {
            return Pair(rawText, false)
        }

        val cleanedRaw = cleanForComparison(rawText)
        val hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) node.hintText?.toString() else null
        val cleanedHint = hint?.let { cleanForComparison(it) }
        val contentDesc = node.contentDescription?.toString()
        val cleanedDesc = contentDesc?.let { cleanForComparison(it) }

        if (isPlaceholderMatch(cleanedRaw, cleanedHint, cleanedDesc)) {
            return Pair("", true)
        }

        return Pair(rawText, false)
    }

    fun getLiveFocusedSnapshot(): FocusedFieldSnapshot? {
        refreshFocusedNode()
        return TextBridgeStateHolder.focusedSnapshot.value
    }

    fun refreshFocusedNode() {
        var node: AccessibilityNodeInfo? = null
        try {
            node = findFocusedInputNode()
            if (node == null) {
                TextBridgeStateHolder.updateFocusedSnapshot(null)
                return
            }

            val (text, isHint) = extractActualFieldText(node)

            val snapshot = FocusedFieldSnapshot(
                currentText = text,
                selectionStart = if (isHint || text.isEmpty()) 0 else node.textSelectionStart,
                selectionEnd = if (isHint || text.isEmpty()) 0 else node.textSelectionEnd,
                isEditable = node.isEditable,
                isPassword = node.isPassword,
                isShowingHintText = isHint,
                packageName = node.packageName?.toString() ?: "",
                className = node.className?.toString() ?: ""
            )

            TextBridgeStateHolder.updateFocusedSnapshot(snapshot)
            TextBridgeStateHolder.updateForegroundPackage(snapshot.packageName)
        } catch (e: Exception) {
            // Safe
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun applySetText(text: String): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            refreshFocusedNode()
            success
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "applySetText failed: ${e.message}"
            )
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun applySelection(start: Int, end: Int): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val args = Bundle()
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
            args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
        } catch (e: Exception) {
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun applySelectAll(): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val len = node.text?.length ?: 0
            if (len > 0) {
                val args = Bundle()
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                args.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, len)
                node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun applyPaste(): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val success = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
            refreshFocusedNode()
            success
        } catch (e: Exception) {
            DiagnosticsLog.log(
                DiagnosticsLog.Category.ERROR,
                "applyPaste failed: ${e.message}"
            )
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun deleteLastWord(): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val (text, isHint) = extractActualFieldText(node)
            if (isHint || text.isEmpty()) return false
            val selStart = if (node.textSelectionStart >= 0) node.textSelectionStart else text.length
            val before = text.substring(0, selStart)
            val after = text.substring(selStart)
            val trimmedBefore = before.trimEnd()
            val lastDelim = trimmedBefore.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '।', '.', ',', '?', '!', ';', ':'))
            val newBefore = if (lastDelim == -1) "" else before.substring(0, lastDelim + 1)
            val newText = newBefore + after
            val newCursor = newBefore.length
            applySetText(newText)
            applySelection(newCursor, newCursor)
            true
        } catch (e: Exception) {
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun deleteLastSentence(): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val (text, isHint) = extractActualFieldText(node)
            if (isHint || text.isEmpty()) return false
            val selStart = if (node.textSelectionStart >= 0) node.textSelectionStart else text.length
            val before = text.substring(0, selStart)
            val after = text.substring(selStart)
            val trimmedBefore = before.trimEnd()
            val lastPunct = trimmedBefore.dropLast(1).lastIndexOfAny(charArrayOf('।', '.', '?', '!', '\n'))
            val newBefore = if (lastPunct == -1) "" else before.substring(0, lastPunct + 1).trimStart()
            val newText = if (newBefore.isNotEmpty() && !newBefore.endsWith(" ")) "$newBefore $after" else "$newBefore$after"
            val newCursor = newBefore.length
            applySetText(newText)
            applySelection(newCursor, newCursor)
            true
        } catch (e: Exception) {
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun insertNewline(doubleNewline: Boolean = false): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val (text, isHint) = extractActualFieldText(node)
            val currentText = if (isHint) "" else text
            val cursor = if (isHint || node.textSelectionStart < 0) currentText.length else node.textSelectionStart
            val safeCursor = cursor.coerceIn(0, currentText.length)
            val breakStr = if (doubleNewline) "\n\n" else "\n"
            val before = currentText.substring(0, safeCursor)
            val after = currentText.substring(safeCursor)
            val newText = before + breakStr + after
            val newCursor = safeCursor + breakStr.length

            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            }
            val success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (success) {
                applySelection(newCursor, newCursor)
            }
            refreshFocusedNode()
            success
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "insertNewline failed: ${e.message}")
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun clearAll(): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            
            // Check if text is still present (some apps reject SET_TEXT with empty string)
            val (currentText, isHint) = extractActualFieldText(node)
            if (!isHint && currentText.isNotEmpty()) {
                val len = node.text?.length ?: 0
                if (len > 0) {
                    val selArgs = Bundle().apply {
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                        putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, len)
                    }
                    node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)
                    node.performAction(AccessibilityNodeInfo.ACTION_CUT)
                }
            }
            applySelection(0, 0)
            refreshFocusedNode()
            true
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "clearAll failed: ${e.message}")
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun applyDeleteRange(start: Int, end: Int, replacementFullText: String): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val nodeTextLen = node.text?.length ?: 0

            // If the entire field is being cleared or replacement is empty
            if (replacementFullText.isEmpty() || (start <= 0 && end >= nodeTextLen)) {
                return clearAll()
            }

            val safeStart = start.coerceIn(0, nodeTextLen)
            val safeEnd = end.coerceIn(safeStart, nodeTextLen)

            // 1. Try standard ACTION_SET_TEXT with replacementFullText
            val setTextArgs = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, replacementFullText)
            }
            val setSuccess = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setTextArgs)

            // Check if the text actually updated
            val (textAfterSet, _) = extractActualFieldText(node)
            if (setSuccess && textAfterSet == replacementFullText) {
                applySelection(safeStart, safeStart)
                refreshFocusedNode()
                return true
            }

            // 2. Fallback for apps where ACTION_SET_TEXT fails or is ignored (e.g. WhatsApp):
            // Select the span to be deleted and perform ACTION_CUT
            val selArgs = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, safeStart)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, safeEnd)
            }
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, selArgs)

            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val prevClip = cm?.primaryClip

            node.performAction(AccessibilityNodeInfo.ACTION_CUT)

            // Restore clipboard to preserve user's copied item
            if (cm != null && prevClip != null) {
                try {
                    cm.setPrimaryClip(prevClip)
                } catch (ignored: Exception) {}
            }

            applySelection(safeStart, safeStart)
            refreshFocusedNode()
            true
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "applyDeleteRange failed: ${e.message}")
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun applySend(): Boolean {
        // Priority 0: Shizuku privileged hardware Enter key injection
        try {
            val app = applicationContext as? com.example.KothaApplication
            val settings = app?.settingsRepository?.cachedSettings
            if (settings?.shizukuHandsFreeSendEnabled == true && com.example.util.ShizukuManager.hasPermission.value) {
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    com.example.util.ShizukuManager.sendEnterKey()
                }
                DiagnosticsLog.log(DiagnosticsLog.Category.INSERTION, "applySend: dispatched KEYCODE_ENTER via Shizuku")
                return true
            }
        } catch (e: Exception) {
            // Fallback to accessibility search
        }

        val root = rootInActiveWindow ?: return false
        return try {
            // Priority 1: Search the window for a Send/Submit/Post button (Messaging & social apps)
            val sendKeywords = listOf(
                "send", "submit", "post", "done", "পাঠাও", "প্রেরণ", "সেন্ড", "সাবমিট",
                "send_button", "sendbutton", "send_btn", "btn_send", "btn_submit", "button_send"
            )
            var found = findActionNode(root, sendKeywords)

            // Priority 2: Search for search / go buttons (Browsers & search fields)
            if (found == null) {
                val searchKeywords = listOf("search", "search_button", "search_btn", "search_go_btn", "অনুসন্ধান", "খুঁজুন")
                found = findActionNode(root, searchKeywords)
            }

            if (found != null) {
                val clicked = found.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                try { found.recycle() } catch (ignored: Exception) {}
                DiagnosticsLog.log(DiagnosticsLog.Category.INSERTION, "applySend: clicked send/submit button (success=$clicked)")
                if (clicked) return true
            }

            // Priority 3: Fallback for single-line search fields or forms with no on-screen button
            var focused: AccessibilityNodeInfo? = null
            try {
                focused = findFocusedInputNode()
                if (focused != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val imeEnter = AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER
                    val imeSuccess = focused.performAction(imeEnter.id)
                    DiagnosticsLog.log(DiagnosticsLog.Category.INSERTION, "applySend: IME Enter fallback (success=$imeSuccess)")
                    if (imeSuccess) return true
                }
            } finally {
                try { focused?.recycle() } catch (ignored: Exception) {}
            }

            false
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "applySend error: ${e.message}")
            false
        } finally {
            try { root.recycle() } catch (ignored: Exception) {}
        }
    }

    private fun findActionNode(node: AccessibilityNodeInfo, keywords: List<String>): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""
        val text = node.text?.toString()?.trim()?.lowercase(Locale.ROOT) ?: ""
        val resName = node.viewIdResourceName?.lowercase(Locale.ROOT) ?: ""

        val isMatch = keywords.any { kw ->
            val kwLower = kw.lowercase(Locale.ROOT)
            desc == kwLower || text == kwLower ||
                desc.startsWith("$kwLower ") || desc.endsWith(" $kwLower") ||
                resName.contains(kwLower)
        }

        val isNegative = desc.contains("back") || desc.contains("close") || desc.contains("navigate") ||
                (node.isEditable && desc.isEmpty())

        if (isMatch && !isNegative) {
            if (node.isClickable) return node
            val parent = node.parent
            if (parent != null && parent.isClickable) {
                return parent
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findActionNode(child, keywords)
            if (match != null) return match
            try { child.recycle() } catch (ignored: Exception) {}
        }
        return null
    }


    fun applyCopy(context: Context): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            if (node.performAction(AccessibilityNodeInfo.ACTION_COPY)) {
                android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                return true
            }
            val (text, isHint) = extractActualFieldText(node)
            if (!isHint && text.isNotEmpty()) {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("Kotha", text))
                android.widget.Toast.makeText(context, "Copied text to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun moveCursorToEnd(): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val len = node.text?.length ?: 0
            applySelection(len, len)
        } catch (e: Exception) {
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    fun moveCursorToStart(): Boolean {
        return applySelection(0, 0)
    }

    fun capitalizeLastWord(): Boolean {
        var node: AccessibilityNodeInfo? = null
        return try {
            node = findFocusedInputNode() ?: return false
            val (text, isHint) = extractActualFieldText(node)
            if (isHint || text.isEmpty()) return false
            val cursor = if (node.textSelectionStart >= 0) node.textSelectionStart else text.length
            if (cursor <= 0) return false
            val before = text.substring(0, cursor)
            val after = text.substring(cursor)
            val trimmed = before.trimEnd()
            val lastDelim = trimmed.lastIndexOfAny(charArrayOf(' ', '\n', '\t', '।', '.', ',', '?', '!', ';', ':'))
            val wordStart = if (lastDelim == -1) 0 else lastDelim + 1
            if (wordStart < trimmed.length) {
                val word = trimmed.substring(wordStart)
                val capWord = word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                val newBefore = before.substring(0, wordStart) + capWord + before.substring(trimmed.length)
                val newText = newBefore + after
                applySetText(newText)
                applySelection(cursor, cursor)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            try { node?.recycle() } catch (ignored: Exception) {}
        }
    }

    companion object {
        var instance: TextBridgeService? = null
            private set
    }
}
