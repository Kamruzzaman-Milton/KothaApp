package com.example.engine

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.example.util.DiagnosticsLog
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import org.json.JSONObject

object BilingualLexiconManager {

    private const val DB_NAME = "bilingual_lexicon.db"
    private const val DB_VERSION = 1

    private var database: SQLiteDatabase? = null
    private var phrasesList: List<Pair<Regex, String>> = emptyList()
    private var isInitialized = false

    private val commonBanglaInflectionSuffixes = listOf(
        "গুলোতে", "গুলোকে", "গুলোর", "গুলো",
        "গুলিতে", "গুলিকে", "গুলির", "গুলি",
        "টার", "টাতে", "টিতে", "টির", "টা", "টি",
        "দেরকে", "দের", "য়ের", "য়ের", "কে", "তে",
        "এর", "এ", "য়ে", "য়ে", "র"
    )

    fun initialize(context: Context) {
        if (isInitialized && database?.isOpen == true) return

        synchronized(this) {
            if (isInitialized && database?.isOpen == true) return

            try {
                val dbFile = File(context.filesDir, DB_NAME)
                var needsCopy = !dbFile.exists()

                // Check if assets db exists and size is different
                if (dbFile.exists()) {
                    try {
                        val assetFd = context.assets.openFd(DB_NAME)
                        if (assetFd.length != dbFile.length()) {
                            needsCopy = true
                        }
                        assetFd.close()
                    } catch (ignored: Exception) {
                        // In case compressed asset length is not available
                    }
                }

                if (needsCopy) {
                    DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Copying native bilingual database from assets...")
                    context.assets.open(DB_NAME).use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "Bilingual database ready: ${dbFile.length() / 1024} KB")
                }

                database = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                loadPhrases()
                isInitialized = true
                DiagnosticsLog.log(DiagnosticsLog.Category.RECOGNIZER, "BilingualLexiconManager initialized successfully with ${phrasesList.size} phrases")
            } catch (e: Exception) {
                DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "Failed to initialize BilingualLexiconManager: ${e.message}")
            }
        }
    }

    private fun loadPhrases() {
        val db = database ?: return
        val list = mutableListOf<Pair<Regex, String>>()
        try {
            db.rawQuery("SELECT bn_phrase, en_phrase FROM phrases", null).use { cursor ->
                val bnIdx = cursor.getColumnIndex("bn_phrase")
                val enIdx = cursor.getColumnIndex("en_phrase")
                while (cursor.moveToNext()) {
                    val bn = cursor.getString(bnIdx)
                    val en = cursor.getString(enIdx)
                    if (!bn.isNullOrBlank() && !en.isNullOrBlank()) {
                        // Match multi-word with flexible whitespace regex
                        val pattern = Regex(bn.trim().split(Regex("\\s+")).joinToString("\\s+"), RegexOption.IGNORE_CASE)
                        list.add(pattern to en.trim())
                    }
                }
            }
            phrasesList = list
        } catch (e: Exception) {
            DiagnosticsLog.log(DiagnosticsLog.Category.ERROR, "Error loading phrases: ${e.message}")
        }
    }

    fun lookupWord(bnWord: String): String? {
        val db = database ?: return null
        if (!db.isOpen || bnWord.isBlank()) return null

        return try {
            db.rawQuery("SELECT en_word FROM lexicon WHERE bn_word = ? LIMIT 1", arrayOf(bnWord.trim())).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun processBilingualText(
        text: String,
        enableLoanwords: Boolean,
        loanwordTargetScript: String,
        enableTechJargon: Boolean,
        customJargonJson: String
    ): String {
        if (!enableLoanwords && !enableTechJargon && customJargonJson.isBlank()) return text
        if (loanwordTargetScript.equals("BANGLA", ignoreCase = true) && !enableTechJargon) return text

        val customMap = parseCustomJargon(customJargonJson)

        // 1. Process multi-word phrases first
        var phraseProcessed = text
        if (enableLoanwords) {
            for ((regex, replacement) in phrasesList) {
                phraseProcessed = phraseProcessed.replace(regex, replacement)
            }
        }

        // 2. Tokenize preserving whitespaces and delimiters
        val tokens = phraseProcessed.split(Regex("(?<=\\s)|(?=\\s)"))
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

            // Resolve word using native SQLite database and suffixes
            val replaced = resolveWord(
                word = coreWord,
                enableLoanwords = enableLoanwords,
                customMap = customMap
            )

            sb.append(prefixPunct).append(replaced).append(suffixPunct)
        }

        return sb.toString()
    }

    private fun resolveWord(
        word: String,
        enableLoanwords: Boolean,
        customMap: Map<String, String>
    ): String {
        // 1. Check custom jargon
        customMap[word]?.let { return it }

        if (!enableLoanwords) return word

        // 2. Direct lookup in offline SQLite database
        lookupWord(word)?.let { return it }

        // 3. Morphological inflection suffix stripping
        val cleanWord = word.trim()
        for (suffix in commonBanglaInflectionSuffixes) {
            if (cleanWord.endsWith(suffix) && cleanWord.length > suffix.length) {
                var stem = cleanWord.substring(0, cleanWord.length - suffix.length)
                if (stem.endsWith("-")) {
                    stem = stem.dropLast(1)
                }

                // Check custom for stem
                customMap[stem]?.let { target ->
                    val normSuffix = normalizeSuffix(suffix)
                    return "$target-$normSuffix"
                }

                // Check SQLite database for stem
                lookupWord(stem)?.let { target ->
                    val normSuffix = normalizeSuffix(suffix)
                    return "$target-$normSuffix"
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

    private fun parseCustomJargon(jsonStr: String): Map<String, String> {
        if (jsonStr.isBlank()) return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            val obj = JSONObject(jsonStr)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                result[k.trim()] = obj.optString(k).trim()
            }
        } catch (ignored: Exception) {
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

    fun getLexiconCount(): Int {
        val db = database ?: return 0
        return try {
            db.rawQuery("SELECT COUNT(*) FROM lexicon", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
        } catch (e: Exception) {
            0
        }
    }
}
