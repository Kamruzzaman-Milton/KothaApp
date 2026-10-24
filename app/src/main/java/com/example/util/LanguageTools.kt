package com.example.util

import java.util.Locale

object LanguageTools {

    data class ReplacementRule(
        val from: String,
        val to: String,
        val isRegex: Boolean = false
    )

    enum class DigitConversionMode {
        NONE,
        TO_BANGLA,
        TO_WESTERN
    }

    sealed class VoiceCommandResult {
        object None : VoiceCommandResult()
        object NewLine : VoiceCommandResult()
        object NewParagraph : VoiceCommandResult()
        object DeleteLastWord : VoiceCommandResult()
        object DeleteLastSentence : VoiceCommandResult()
        object ClearAll : VoiceCommandResult()
        object Send : VoiceCommandResult()
        object Undo : VoiceCommandResult()
        object AddComma : VoiceCommandResult()
        object AddQuestionMark : VoiceCommandResult()
        object AddDari : VoiceCommandResult()
        object Capitalize : VoiceCommandResult()
        object SelectAll : VoiceCommandResult()
        object CopyText : VoiceCommandResult()
        object MoveToEnd : VoiceCommandResult()
        object MoveToStart : VoiceCommandResult()
        object SwitchLanguage : VoiceCommandResult()
        object StopListening : VoiceCommandResult()
    }

    data class VoiceCommandConfig(
        val id: String,
        val actionName: String,
        val description: String,
        val triggers: List<String>,
        val enabled: Boolean = true,
        val requireConfirmation: Boolean = false
    )

    val defaultVoiceCommands = listOf(
        VoiceCommandConfig(
            id = "new_line",
            actionName = "New Line (নতুন লাইন)",
            description = "Inserts a single line break",
            triggers = listOf("নতুন লাইন", "পরের লাইন", "new line", "newline", "next line", "লাইন ব্রেক", "line break", "enter"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "new_paragraph",
            actionName = "New Paragraph (নতুন প্যারা)",
            description = "Inserts a paragraph break (double newline)",
            triggers = listOf("নতুন প্যারা", "new paragraph", "new para", "next paragraph", "paragraph", "নতুন অনুচ্ছেদ"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "delete_word",
            actionName = "Delete Last Word (শেষ শব্দ কাটো)",
            description = "Deletes the preceding word before cursor",
            triggers = listOf("লাস্ট শব্দ কাটো", "শব্দ মুছুন", "delete word", "delete last word", "আগের শব্দ মুছো"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "delete_sentence",
            actionName = "Delete Sentence (শেষ বাক্য কাটো)",
            description = "Deletes preceding clause or sentence up to punctuation",
            triggers = listOf("শেষ বাক্য কাটো", "বাক্য মুছো", "বাক্যটা মুছো", "delete sentence", "আগের বাক্য মুছো"),
            enabled = true,
            requireConfirmation = true
        ),
        VoiceCommandConfig(
            id = "clear_all",
            actionName = "Clear All (সব মুছো)",
            description = "Clears all text in the focused input field",
            triggers = listOf("সব মুছো", "সব ক্লিয়ার করো", "clear all", "delete all", "সব মুছে ফেলো", "সব মুছুন", "মুছে ফেলো", "clear text", "delete everything"),
            enabled = true,
            requireConfirmation = true
        ),
        VoiceCommandConfig(
            id = "send",
            actionName = "Send / Submit (পাঠাও)",
            description = "Submits or sends text across apps and messaging fields",
            triggers = listOf("সেন্ড", "সেন্ড করো", "পাঠাও", "send", "submit", "send message", "send it", "সাবমিট", "পোস্ট", "জমা দাও", "search", "খুঁজুন"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "undo",
            actionName = "Undo (আনডু)",
            description = "Reverts the last voice insertion",
            triggers = listOf("আনডু", "undo", "বাতিল করো", "পূর্বাবস্থায় ফেরান"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "add_comma",
            actionName = "Add Comma (কমা)",
            description = "Inserts a comma symbol (,)",
            triggers = listOf("কমা", "comma"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "add_question",
            actionName = "Add Question Mark (জিজ্ঞাসাচিহ্ন)",
            description = "Inserts a question mark (?)",
            triggers = listOf("জিজ্ঞাসাচিহ্ন", "প্রশ্নবোধক চিহ্ন", "question mark", "প্রশ্ন চিহ্ন"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "add_dari",
            actionName = "Add Dari / Period (দাঁড়ি)",
            description = "Inserts Bengali Dari (।) or period (.)",
            triggers = listOf("দাঁড়ি", "দাড়ি", "পূর্ণচ্ছেদ", "period", "full stop"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "capitalize",
            actionName = "Capitalize (বড় হাত)",
            description = "Capitalizes initial letter of previous word",
            triggers = listOf("ক্যাপিটালাইজ", "capitalize", "বড় হাত"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "select_all",
            actionName = "Select All (সব সিলেক্ট করো)",
            description = "Selects all content in the text box",
            triggers = listOf("সব সিলেক্ট করো", "select all"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "copy_text",
            actionName = "Copy Text (কপি করো)",
            description = "Copies text or selection to clipboard",
            triggers = listOf("সব কপি করো", "কপি", "copy text", "কপি করো"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "move_end",
            actionName = "Move to End (শেষে যাও)",
            description = "Repositions cursor to the end of text",
            triggers = listOf("শেষে যাও", "go to end", "কার্সর শেষে নাও", "move to end"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "move_start",
            actionName = "Move to Start (শুরুতে যাও)",
            description = "Repositions cursor to the beginning of text",
            triggers = listOf("শুরুতে যাও", "go to start", "কার্সর শুরুতে নাও", "move to start"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "switch_lang",
            actionName = "Switch Language (ভাষা বদলাও)",
            description = "Swaps primary and secondary speech recognition languages",
            triggers = listOf("ভাষা বদলাও", "বাংলা করো", "switch language", "switch to english"),
            enabled = true,
            requireConfirmation = false
        ),
        VoiceCommandConfig(
            id = "stop_listening",
            actionName = "Stop Listening (শোনা বন্ধ)",
            description = "Stops active speech recognition session",
            triggers = listOf("শোনা বন্ধ", "শোনা বন্ধ করুন", "stop listening"),
            enabled = true,
            requireConfirmation = false
        )
    )

    private data class StoredConfig(
        val triggers: List<String>?,
        val enabled: Boolean?,
        val requireConfirmation: Boolean?
    )

    fun commandsFromJson(json: String): List<VoiceCommandConfig> {
        if (json.isBlank()) return defaultVoiceCommands
        return try {
            val array = org.json.JSONArray(json)
            val customMap = mutableMapOf<String, StoredConfig>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val trigsArr = obj.optJSONArray("triggers")
                val trigs = if (trigsArr != null) {
                    val list = mutableListOf<String>()
                    for (j in 0 until trigsArr.length()) {
                        val t = trigsArr.optString(j, "").trim()
                        if (t.isNotEmpty()) list.add(t)
                    }
                    list
                } else null

                val enabled = if (obj.has("enabled")) obj.optBoolean("enabled") else null
                val requireConfirm = if (obj.has("requireConfirmation")) obj.optBoolean("requireConfirmation") else null

                if (id.isNotBlank()) {
                    customMap[id] = StoredConfig(trigs, enabled, requireConfirm)
                }
            }
            defaultVoiceCommands.map { def ->
                val stored = customMap[def.id]
                if (stored != null) {
                    def.copy(
                        triggers = stored.triggers ?: def.triggers,
                        enabled = stored.enabled ?: def.enabled,
                        requireConfirmation = stored.requireConfirmation ?: def.requireConfirmation
                    )
                } else {
                    def
                }
            }
        } catch (e: Exception) {
            defaultVoiceCommands
        }
    }

    fun commandsToJson(commands: List<VoiceCommandConfig>): String {
        val array = org.json.JSONArray()
        for (cmd in commands) {
            val obj = org.json.JSONObject()
            obj.put("id", cmd.id)
            val trigsArr = org.json.JSONArray()
            cmd.triggers.forEach { trigsArr.put(it) }
            obj.put("triggers", trigsArr)
            obj.put("enabled", cmd.enabled)
            obj.put("requireConfirmation", cmd.requireConfirmation)
            array.put(obj)
        }
        return array.toString()
    }

    fun cleanUtteranceForCommand(str: String): String {
        return str.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[।.,?!;:…\\-_'\"]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun checkVoiceCommandMatch(
        rawUtterance: String,
        commands: List<VoiceCommandConfig> = defaultVoiceCommands
    ): Pair<VoiceCommandResult, VoiceCommandConfig>? {
        val trimmed = cleanUtteranceForCommand(rawUtterance)
        for (cmd in commands) {
            if (!cmd.enabled) continue
            for (trig in cmd.triggers) {
                val normTrig = cleanUtteranceForCommand(trig)
                if (trimmed == normTrig) {
                    val result = when (cmd.id) {
                        "new_line" -> VoiceCommandResult.NewLine
                        "new_paragraph" -> VoiceCommandResult.NewParagraph
                        "delete_word" -> VoiceCommandResult.DeleteLastWord
                        "delete_sentence" -> VoiceCommandResult.DeleteLastSentence
                        "clear_all" -> VoiceCommandResult.ClearAll
                        "send" -> VoiceCommandResult.Send
                        "undo" -> VoiceCommandResult.Undo
                        "add_comma" -> VoiceCommandResult.AddComma
                        "add_question" -> VoiceCommandResult.AddQuestionMark
                        "add_dari" -> VoiceCommandResult.AddDari
                        "capitalize" -> VoiceCommandResult.Capitalize
                        "select_all" -> VoiceCommandResult.SelectAll
                        "copy_text" -> VoiceCommandResult.CopyText
                        "move_end" -> VoiceCommandResult.MoveToEnd
                        "move_start" -> VoiceCommandResult.MoveToStart
                        "switch_lang" -> VoiceCommandResult.SwitchLanguage
                        "stop_listening" -> VoiceCommandResult.StopListening
                        else -> VoiceCommandResult.None
                    }
                    if (result !is VoiceCommandResult.None) {
                        return Pair(result, cmd)
                    }
                }
            }
        }
        return null
    }

    fun checkVoiceCommand(
        rawUtterance: String,
        commands: List<VoiceCommandConfig> = defaultVoiceCommands
    ): VoiceCommandResult {
        return checkVoiceCommandMatch(rawUtterance, commands)?.first ?: VoiceCommandResult.None
    }

    fun isSensitiveCommand(cmd: VoiceCommandResult): Boolean {
        return when (cmd) {
            is VoiceCommandResult.DeleteLastWord,
            is VoiceCommandResult.DeleteLastSentence,
            is VoiceCommandResult.ClearAll,
            is VoiceCommandResult.Send -> true
            else -> false
        }
    }

    fun getCommandName(cmd: VoiceCommandResult): String {
        return when (cmd) {
            is VoiceCommandResult.NewLine -> "New Line (নতুন লাইন)"
            is VoiceCommandResult.NewParagraph -> "New Paragraph (নতুন প্যারা)"
            is VoiceCommandResult.DeleteLastWord -> "Delete Last Word (শেষ শব্দ কাটো)"
            is VoiceCommandResult.DeleteLastSentence -> "Delete Sentence (শেষ বাক্য কাটো)"
            is VoiceCommandResult.ClearAll -> "Clear All (সব মুছো)"
            is VoiceCommandResult.Send -> "Send / Submit (পাঠাও)"
            is VoiceCommandResult.Undo -> "Undo (আনডু)"
            is VoiceCommandResult.AddComma -> "Add Comma (কমা)"
            is VoiceCommandResult.AddQuestionMark -> "Add Question Mark (জিজ্ঞাসাচিহ্ন)"
            is VoiceCommandResult.AddDari -> "Add Dari (দাঁড়ি)"
            is VoiceCommandResult.Capitalize -> "Capitalize (বড় হাত)"
            is VoiceCommandResult.SelectAll -> "Select All (সব সিলেক্ট করো)"
            is VoiceCommandResult.CopyText -> "Copy Text (কপি করো)"
            is VoiceCommandResult.MoveToEnd -> "Move to End (শেষে যাও)"
            is VoiceCommandResult.MoveToStart -> "Move to Start (শুরুতে যাও)"
            is VoiceCommandResult.SwitchLanguage -> "Switch Language (ভাষা বদলাও)"
            is VoiceCommandResult.StopListening -> "Stop Listening (শোনা বন্ধ)"
            else -> "Voice Command"
        }
    }

    val defaultBanglaReplacements = listOf(
        ReplacementRule(from = "দাঁড়ি", to = "।"),
        ReplacementRule(from = "দাড়ি", to = "।"),
        ReplacementRule(from = "কমা", to = ","),
        ReplacementRule(from = "প্রশ্নবোধক চিহ্ন", to = "?"),
        ReplacementRule(from = "প্রশ্ন চিহ্ন", to = "?"),
        ReplacementRule(from = "বিস্ময়বোধক চিহ্ন", to = "!"),
        ReplacementRule(from = "বিস্ময় চিহ্ন", to = "!"),
        ReplacementRule(from = "হাইফেন", to = "-"),
        ReplacementRule(from = "কলন", to = ":"),
        ReplacementRule(from = "সেমিকলন", to = ";")
    )

    fun rulesFromJson(json: String): List<ReplacementRule> {
        if (json.isBlank()) return defaultBanglaReplacements
        return try {
            val array = org.json.JSONArray(json)
            val list = mutableListOf<ReplacementRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val from = obj.optString("from", "")
                val to = obj.optString("to", "")
                val isRegex = obj.optBoolean("isRegex", false)
                if (from.isNotBlank()) {
                    list.add(ReplacementRule(from, to, isRegex))
                }
            }
            list
        } catch (e: Exception) {
            defaultBanglaReplacements
        }
    }

    fun rulesToJson(rules: List<ReplacementRule>): String {
        val array = org.json.JSONArray()
        for (r in rules) {
            val obj = org.json.JSONObject()
            obj.put("from", r.from)
            obj.put("to", r.to)
            obj.put("isRegex", r.isRegex)
            array.put(obj)
        }
        return array.toString()
    }

    private val banglaToWesternDigits = mapOf(
        '০' to '0', '১' to '1', '২' to '2', '৩' to '3', '৪' to '4',
        '৫' to '5', '৬' to '6', '৭' to '7', '৮' to '8', '৯' to '9'
    )

    private val westernToBanglaDigits = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )

    fun applyReplacements(
        text: String,
        rules: List<ReplacementRule>
    ): String {
        var result = text
        for (rule in rules) {
            if (rule.from.isBlank()) continue
            try {
                result = if (rule.isRegex) {
                    result.replace(Regex(rule.from, RegexOption.IGNORE_CASE), rule.to)
                } else {
                    result.replace(rule.from, rule.to, ignoreCase = true)
                }
            } catch (e: Exception) {
                // If regex syntax was invalid, skip safely
            }
        }
        return result
    }

    fun convertDigits(text: String, mode: DigitConversionMode): String {
        return when (mode) {
            DigitConversionMode.NONE -> text
            DigitConversionMode.TO_BANGLA -> {
                val sb = java.lang.StringBuilder(text.length)
                for (ch in text) {
                    sb.append(westernToBanglaDigits[ch] ?: ch)
                }
                sb.toString()
            }
            DigitConversionMode.TO_WESTERN -> {
                val sb = java.lang.StringBuilder(text.length)
                for (ch in text) {
                    sb.append(banglaToWesternDigits[ch] ?: ch)
                }
                sb.toString()
            }
        }
    }

    fun applyAutoPunctuation(
        text: String,
        languageCode: String,
        autoPunctuate: Boolean
    ): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return text
        if (!autoPunctuate) return fixPunctuationSpacing(trimmed)

        val lastChar = trimmed.last()
        val isBangla = languageCode.startsWith("bn", ignoreCase = true)

        val hasTerminalPunctuation = when (lastChar) {
            '।', '.', '?', '!', ';', ':', '…' -> true
            else -> false
        }

        val punctText = if (!hasTerminalPunctuation) {
            if (isBangla) "$trimmed।" else "$trimmed."
        } else {
            trimmed
        }

        return fixPunctuationSpacing(punctText)
    }

    fun fixPunctuationSpacing(text: String): String {
        var formatted = text
        // Ensure no whitespace before punctuation marks
        formatted = formatted.replace(Regex("\\s+([।.,?!;:।])"), "$1")
        // Ensure single space after punctuation if followed by word character
        formatted = formatted.replace(Regex("([।.,?!;:।])(\\S)"), "$1 $2")
        return formatted
    }

    fun autoCapitalizeLatin(text: String, languageCode: String): String {
        if (languageCode.startsWith("bn", ignoreCase = true)) return text
        if (text.isEmpty()) return text

        val sb = StringBuilder()
        var capitalizeNext = true
        for (ch in text) {
            if (capitalizeNext && ch.isLetter()) {
                sb.append(ch.uppercaseChar())
                capitalizeNext = false
            } else {
                sb.append(ch)
                if (ch == '.' || ch == '?' || ch == '!') {
                    capitalizeNext = true
                }
            }
        }
        return sb.toString()
    }

    fun formatFinalUtterance(
        rawText: String,
        languageCode: String,
        autoPunctuate: Boolean,
        digitMode: DigitConversionMode,
        autoCapLatin: Boolean,
        replacements: List<ReplacementRule>
    ): String {
        var processed = rawText
        processed = applyReplacements(processed, rules = replacements)
        processed = convertDigits(processed, digitMode)
        if (autoCapLatin) {
            processed = autoCapitalizeLatin(processed, languageCode)
        }
        processed = applyAutoPunctuation(processed, languageCode, autoPunctuate)
        return processed
    }
}
