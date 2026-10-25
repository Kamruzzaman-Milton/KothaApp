package com.example

import com.example.bridge.SpanTracker
import com.example.util.LanguageTools
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSpanTrackerPartialCumulativeReplacement() {
        val tracker = SpanTracker()
        val initialText = "আমি "
        val cursor = 4

        // First partial: "বাংলা"
        val r1 = tracker.calculatePartialInsertion(
            actualCurrentText = initialText,
            currentCursor = cursor,
            partialText = "বাংলা",
            followCursor = true
        )
        assertEquals("আমি বাংলা", r1.newFullText)
        assertEquals(9, r1.newSelection)

        // Second cumulative partial: "বাংলা কথা"
        val r2 = tracker.calculatePartialInsertion(
            actualCurrentText = r1.newFullText,
            currentCursor = r1.newSelection,
            partialText = "বাংলা কথা",
            followCursor = true
        )
        assertEquals("আমি বাংলা কথা", r2.newFullText)
        assertEquals(13, r2.newSelection)
    }

    @Test
    fun testSpanTrackerFinalCommitAndUndo() {
        val tracker = SpanTracker()
        val initialText = "হ্যালো "

        val finalRes = tracker.calculateFinalCommit(
            actualCurrentText = initialText,
            currentCursor = initialText.length,
            finalText = "বন্ধু",
            separator = " ",
            followCursor = true
        )
        assertEquals("হ্যালো বন্ধু ", finalRes.newFullText)

        // Undo
        val undoRes = tracker.calculateUndo(finalRes.newFullText)
        assertNotNull(undoRes)
        assertEquals("হ্যালো ", undoRes?.first)
    }

    @Test
    fun testAutoPunctuationBangla() {
        val text = "কেমন আছেন"
        val punctuated = LanguageTools.applyAutoPunctuation(text, "bn-BD", autoPunctuate = true)
        assertEquals("কেমন আছেন।", punctuated)

        val alreadyPunctuated = "ভালো আছি।"
        val result = LanguageTools.applyAutoPunctuation(alreadyPunctuated, "bn-BD", autoPunctuate = true)
        assertEquals("ভালো আছি।", result)
    }

    @Test
    fun testDigitConversion() {
        val western = "১২৩৪৫"
        val converted = LanguageTools.convertDigits(western, LanguageTools.DigitConversionMode.TO_WESTERN)
        assertEquals("12345", converted)

        val toBangla = LanguageTools.convertDigits("67890", LanguageTools.DigitConversionMode.TO_BANGLA)
        assertEquals("৬৭৮৯০", toBangla)
    }

    @Test
    fun testSpokenReplacements() {
        val spoken = "আজকে সুন্দর দিন দাঁড়ি"
        val replaced = LanguageTools.applyReplacements(spoken, LanguageTools.defaultBanglaReplacements)
        assertEquals("আজকে সুন্দর দিন ।", replaced)
    }

    @Test
    fun testVoiceCommands() {
        val cmd = LanguageTools.checkVoiceCommand("নতুন লাইন")
        assertTrue(cmd is LanguageTools.VoiceCommandResult.NewLine)

        val undoCmd = LanguageTools.checkVoiceCommand("undo")
        assertTrue(undoCmd is LanguageTools.VoiceCommandResult.Undo)
    }
}
