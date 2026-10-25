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
                    val mergedTriggers = if (stored.triggers != null) {
                        (stored.triggers + def.triggers).distinct()
                    } else {
                        def.triggers
                    }
                    def.copy(
                        triggers = mergedTriggers,
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

    val commonBanglaInflectionSuffixes = listOf(
        "গুলোতে", "গুলোকে", "গুলোর", "গুলো",
        "গুলিতে", "গুলিকে", "গুলির", "গুলি",
        "টার", "টাতে", "টিতে", "টির", "টা", "টি",
        "দেরকে", "দের", "য়ের", "য়ের", "কে", "তে",
        "এর", "এ", "য়ে", "য়ে", "র"
    )

    val defaultSmartLoanwordsMap: Map<String, String> = mapOf(
        "মিটিং" to "meeting",
        "প্রেজেন্টেশন" to "presentation",
        "ল্যাপটপ" to "laptop",
        "মোবাইল" to "mobile",
        "ফোন" to "phone",
        "কল" to "call",
        "মেসেজ" to "message",
        "মেইল" to "mail",
        "ইমেইল" to "email",
        "ফাইল" to "file",
        "ডকুমেন্ট" to "document",
        "প্রজেক্ট" to "project",
        "রিপোর্ট" to "report",
        "আপডেট" to "update",
        "ডাউনলোড" to "download",
        "আপলোড" to "upload",
        "অনলাইন" to "online",
        "অফলাইন" to "offline",
        "ক্লাস" to "class",
        "টিম" to "team",
        "বস" to "boss",
        "ম্যানেজার" to "manager",
        "ক্লায়েন্ট" to "client",
        "কাস্টমার" to "customer",
        "কোম্পানি" to "company",
        "অফিস" to "office",
        "লিংক" to "link",
        "ভিডিও" to "video",
        "অডিও" to "audio",
        "স্ক্রিন" to "screen",
        "পাসওয়ার্ড" to "password",
        "লগইন" to "login",
        "সাইনআপ" to "signup",
        "ওয়েবসাইট" to "website",
        "অ্যাপ" to "app",
        "অ্যাপ্লিকেশন" to "application",
        "সফটওয়্যার" to "software",
        "হার্ডওয়্যার" to "hardware",
        "সার্ভার" to "server",
        "ডাটা" to "data",
        "ইন্টারনেট" to "internet",
        "ওয়াইফাই" to "wifi",
        "কানেকশন" to "connection",
        "কম্পিউটার" to "computer",
        "ডেস্কটপ" to "desktop",
        "ক্যামেরা" to "camera",
        "চার্জার" to "charger",
        "ব্যাটারি" to "battery",
        "মার্কেট" to "market",
        "শপ" to "shop",
        "বিল" to "bill",
        "পেমেন্ট" to "payment",
        "ক্যাশ" to "cash",
        "কার্ড" to "card",
        "ব্যাংক" to "bank",
        "অ্যাকাউন্ট" to "account",
        "ব্যালেন্স" to "balance",
        "নাম্বার" to "number",
        "অ্যাড্রেস" to "address",
        "লোকেশন" to "location",
        "ম্যাপ" to "map",
        "বাইক" to "bike",
        "ড্রাইভ" to "drive",
        "রাইড" to "ride",
        "টিকেট" to "ticket",
        "বুকিং" to "booking",
        "হোটেল" to "hotel",
        "রুম" to "room",
        "ডাক্তার" to "doctor",
        "হাসপাতাল" to "hospital",
        "মেডিসিন" to "medicine",
        "প্রেসক্রিপশন" to "prescription",
        "ক্লিনিক" to "clinic",
        "স্কুল" to "school",
        "কলেজ" to "college",
        "ভার্সিটি" to "varsity",
        "ইউনিভার্সিটি" to "university",
        "এক্সাম" to "exam",
        "রেজাল্ট" to "result",
        "সার্টিফিকেট" to "certificate",
        "জব" to "job",
        "ইন্টারভিউ" to "interview",
        "স্যালারি" to "salary",
        "অফার" to "offer",
        "লেটার" to "letter",
        "নোটিশ" to "notice",
        "অর্ডার" to "order",
        "ডেলিভারি" to "delivery",
        "কুরিয়ার" to "courier",
        "প্যাকেট" to "packet",
        "পার্সেল" to "parcel",
        "ডিসকাউন্ট" to "discount",
        "কুপন" to "coupon",
        "কোড" to "code",
        "ভেরিফিকেশন" to "verification",
        "ওটিপি" to "OTP",
        "পিন" to "PIN"
    )

    val defaultTechJargonMap: Map<String, String> = mapOf(
        "কুবারনেটিস" to "Kubernetes",
        "গিটহাব" to "GitHub",
        "গিট" to "Git",
        "গিটল্যাব" to "GitLab",
        "ডকার" to "Docker",
        "এপিআই" to "API",
        "রেস্ট" to "REST",
        "জেসন" to "JSON",
        "ডাটাবেস" to "Database",
        "এসকিউএল" to "SQL",
        "পোস্টগ্রেস" to "PostgreSQL",
        "মাইএসকিউএল" to "MySQL",
        "মঙ্গোডিবি" to "MongoDB",
        "রেডিস" to "Redis",
        "পাইথন" to "Python",
        "জাভাস্ক্রিপ্ট" to "JavaScript",
        "টাইপস্ক্রিপ্ট" to "TypeScript",
        "কোটলিন" to "Kotlin",
        "জাভা" to "Java",
        "সুইফট" to "Swift",
        "ফ্লাটার" to "Flutter",
        "রিঅ্যাক্ট" to "React",
        "নেক্সটজেএস" to "Next.js",
        "অ্যান্ড্রয়েড" to "Android",
        "আইওএস" to "iOS",
        "ক্লাউড" to "Cloud",
        "এডাব্লিউএস" to "AWS",
        "জিসিপি" to "GCP",
        "অ্যাজুর" to "Azure",
        "লিনাক্স" to "Linux",
        "উবুন্টু" to "Ubuntu",
        "উইন্ডোজ" to "Windows",
        "ম্যাকবুক" to "MacBook",
        "টার্মিনাল" to "Terminal",
        "কমান্ড" to "Command",
        "ফাংশন" to "Function",
        "ভ্যারিয়েবল" to "Variable",
        "বাগ" to "Bug",
        "ডিবাগ" to "Debug",
        "টেস্টিং" to "Testing",
        "ডিপ্লয়" to "Deploy",
        "ডিপ্লয়মেন্ট" to "Deployment",
        "কম্পাইল" to "Compile",
        "বিল্ড" to "Build",
        "ব্যাকএন্ড" to "Backend",
        "ফ্রন্টএন্ড" to "Frontend",
        "ফুলস্ট্যাক" to "Fullstack",
        "সার্ভারলেস" to "Serverless",
        "মাইক্রোসার্ভিস" to "Microservices",
        "আর্কিটেকচার" to "Architecture"
    )

    fun parseCustomJargonJson(jsonStr: String): Map<String, String> {
        if (jsonStr.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            val obj = org.json.JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k.trim()] = obj.optString(k).trim()
            }
        } catch (ignored: Exception) {
            // Also support comma-separated or colon-separated lines
            jsonStr.lines().forEach { line ->
                val parts = line.split(":", "=", "->")
                if (parts.size >= 2) {
                    val k = parts[0].trim()
                    val v = parts[1].trim()
                    if (k.isNotEmpty() && v.isNotEmpty()) {
                        result[k] = v
                    }
                }
            }
        }
        return result
    }

    fun applySmartLoanwordsAndJargon(
        text: String,
        enableLoanwords: Boolean,
        loanwordTargetScript: String,
        enableTechJargon: Boolean,
        customJargonJson: String
    ): String {
        if (!enableLoanwords && !enableTechJargon && customJargonJson.isBlank()) return text
        if (loanwordTargetScript.equals("BANGLA", ignoreCase = true) && !enableTechJargon) return text

        val customMap = parseCustomJargonJson(customJargonJson)

        // Split text into tokens and delimiters while preserving spaces and punctuation
        val tokens = text.split(Regex("(?<=\\s)|(?=\\s)"))
        val sb = StringBuilder()

        for (token in tokens) {
            if (token.isBlank()) {
                sb.append(token)
                continue
            }

            // Extract surrounding punctuation
            var prefixPunct = ""
            var coreWord = token
            var suffixPunct = ""

            val prefixMatch = Regex("^[।,?!;:\"'()\\[\\]{}—–\\-]+").find(coreWord)
            if (prefixMatch != null) {
                prefixPunct = prefixMatch.value
                coreWord = coreWord.substring(prefixPunct.length)
            }

            val suffixMatch = Regex("[।,?!;:\"'()\\[\\]{}—–\\-]+$").find(coreWord)
            if (suffixMatch != null) {
                suffixPunct = suffixMatch.value
                coreWord = coreWord.substring(0, coreWord.length - suffixPunct.length)
            }

            if (coreWord.isEmpty()) {
                sb.append(token)
                continue
            }

            // Try resolving word with morphological suffixes
            val replaced = resolveWordWithSuffixes(
                word = coreWord,
                enableLoanwords = enableLoanwords,
                enableTechJargon = enableTechJargon,
                customMap = customMap
            )

            sb.append(prefixPunct).append(replaced).append(suffixPunct)
        }

        return sb.toString()
    }

    private fun resolveWordWithSuffixes(
        word: String,
        enableLoanwords: Boolean,
        enableTechJargon: Boolean,
        customMap: Map<String, String>
    ): String {
        // 1. Direct match in custom jargon
        customMap[word]?.let { return it }

        // 2. Direct match in tech jargon
        if (enableTechJargon) {
            defaultTechJargonMap[word]?.let { return it }
        }

        // 3. Direct match in smart loanwords
        if (enableLoanwords) {
            defaultSmartLoanwordsMap[word]?.let { return it }
        }

        // 4. Try stripping Bengali inflectional suffixes
        // Handle words like মিটিং-এ, মিটিংয়ে, ল্যাপটপের, ফাইলটা, কুবারনেটিসে
        val cleanWord = word.trim()
        val hasHyphen = cleanWord.contains("-")

        for (suffix in commonBanglaInflectionSuffixes) {
            if (cleanWord.endsWith(suffix) && cleanWord.length > suffix.length) {
                var stem = cleanWord.substring(0, cleanWord.length - suffix.length)
                if (stem.endsWith("-")) {
                    stem = stem.dropLast(1)
                }

                // Check custom
                customMap[stem]?.let { target ->
                    val normSuffix = normalizeSuffix(suffix)
                    return "$target-$normSuffix"
                }

                // Check tech jargon
                if (enableTechJargon) {
                    defaultTechJargonMap[stem]?.let { target ->
                        val normSuffix = normalizeSuffix(suffix)
                        return "$target-$normSuffix"
                    }
                }

                // Check smart loanwords
                if (enableLoanwords) {
                    defaultSmartLoanwordsMap[stem]?.let { target ->
                        val normSuffix = normalizeSuffix(suffix)
                        return "$target-$normSuffix"
                    }
                }
            }
        }

        return word
    }

    private fun normalizeSuffix(suffix: String): String {
        return when (suffix) {
            "য়ে", "য়ে" -> "এ"
            "য়ের", "য়ের" -> "এর"
            else -> suffix
        }
    }

    fun formatFinalUtterance(
        rawText: String,
        languageCode: String,
        autoPunctuate: Boolean,
        digitMode: DigitConversionMode,
        autoCapLatin: Boolean,
        replacements: List<ReplacementRule>,
        enableLoanwords: Boolean = false,
        loanwordTargetScript: String = "ENGLISH",
        enableTechJargon: Boolean = false,
        customJargonJson: String = ""
    ): String {
        var processed = rawText
        processed = applySmartLoanwordsAndJargon(
            text = processed,
            enableLoanwords = enableLoanwords,
            loanwordTargetScript = loanwordTargetScript,
            enableTechJargon = enableTechJargon,
            customJargonJson = customJargonJson
        )
        processed = applyReplacements(processed, rules = replacements)
        processed = convertDigits(processed, digitMode)
        if (autoCapLatin) {
            processed = autoCapitalizeLatin(processed, languageCode)
        }
        processed = applyAutoPunctuation(processed, languageCode, autoPunctuate)
        return processed
    }
}
