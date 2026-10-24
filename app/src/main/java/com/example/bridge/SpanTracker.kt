package com.example.bridge

import kotlin.math.max
import kotlin.math.min

class SpanTracker {

    data class LiveSpan(
        val start: Int,
        val length: Int,
        val lastInserted: String
    ) {
        val end: Int get() = start + length
    }

    data class InsertionResult(
        val newFullText: String,
        val newSelection: Int,
        val updatedSpan: LiveSpan
    )

    data class CommittedSpan(
        val start: Int,
        val text: String
    )

    private val ATTACHING_PUNCTUATION = setOf('।', '.', ',', '?', '!', ';', ':')

    private var currentSpan: LiveSpan? = null
    private var lastCommitted: CommittedSpan? = null
    private var hasAppliedLeadingSpace: Boolean = false

    fun reset() {
        currentSpan = null
        hasAppliedLeadingSpace = false
    }

    fun getLastCommitted(): CommittedSpan? = lastCommitted

    private fun shouldPrependLeadingSpace(
        actualCurrentText: String,
        currentCursor: Int,
        textToInsert: String
    ): Boolean {
        if (actualCurrentText.isEmpty()) return false
        if (textToInsert.isEmpty() || textToInsert.first().isWhitespace()) return false

        // If text starts with attaching punctuation (। , . ? ! ; :), it attaches to previous word
        val firstChar = textToInsert.first()
        if (firstChar in ATTACHING_PUNCTUATION) return false

        // If cursor is unspecified (-1, common in Chrome/WebViews):
        // Since actualCurrentText is not empty, cursor is somewhere inside or at the end of existing text.
        if (currentCursor < 0) {
            return !actualCurrentText.last().isWhitespace()
        }

        // If cursor is at the very beginning of the field, no leading space needed
        if (currentCursor == 0) return false

        // Cursor is in the middle or end
        val safeIdx = clamp(currentCursor, 0, actualCurrentText.length)
        if (safeIdx > 0) {
            val charBefore = actualCurrentText[safeIdx - 1]
            if (charBefore.isWhitespace()) return false
        }

        return true
    }

    /**
     * Prepares insertion of partial text.
     * Replaces previous partial text within the live span.
     */
    fun calculatePartialInsertion(
        actualCurrentText: String,
        currentCursor: Int,
        partialText: String,
        followCursor: Boolean,
        autoLeadingSpace: Boolean = true
    ): InsertionResult {
        var span = currentSpan

        // 1. If no span exists, anchor at currentCursor and determine leading space requirement
        if (span == null) {
            val safeAnchor = clamp(currentCursor, 0, actualCurrentText.length)
            hasAppliedLeadingSpace = autoLeadingSpace && shouldPrependLeadingSpace(actualCurrentText, currentCursor, partialText)
            span = LiveSpan(start = safeAnchor, length = 0, lastInserted = "")
        } else {
            // 2. Edit-safety verification:
            // Check if actualCurrentText still contains span.lastInserted at span.start
            val expectedEnd = span.start + span.length
            val isIntact = span.length == 0 || (
                span.start <= actualCurrentText.length &&
                expectedEnd <= actualCurrentText.length &&
                actualCurrentText.substring(span.start, expectedEnd) == span.lastInserted
            )

            if (!isIntact) {
                // Try to relocate near expected position
                val relocatedIndex = findNearPosition(actualCurrentText, span.lastInserted, span.start)
                if (relocatedIndex != -1) {
                    span = span.copy(start = relocatedIndex)
                } else {
                    // Match failed or user edited inside span: freeze and re-anchor
                    val anchor = if (followCursor) clamp(currentCursor, 0, actualCurrentText.length) else actualCurrentText.length
                    hasAppliedLeadingSpace = autoLeadingSpace && shouldPrependLeadingSpace(actualCurrentText, anchor, partialText)
                    span = LiveSpan(start = anchor, length = 0, lastInserted = "")
                }
            }
        }

        // Apply leading space if determined for this active utterance
        val textToInsert = if (hasAppliedLeadingSpace && !partialText.startsWith(" ")) {
            " $partialText"
        } else {
            partialText
        }

        // 3. Rebuild text: before + textToInsert + after
        val safeStart = clamp(span.start, 0, actualCurrentText.length)
        val safeEnd = clamp(span.start + span.length, safeStart, actualCurrentText.length)

        val before = actualCurrentText.substring(0, safeStart)
        val after = actualCurrentText.substring(safeEnd)
        val newFullText = before + textToInsert + after

        val newSpan = LiveSpan(
            start = safeStart,
            length = textToInsert.length,
            lastInserted = textToInsert
        )
        currentSpan = newSpan

        val newCursor = safeStart + textToInsert.length
        return InsertionResult(newFullText, newCursor, newSpan)
    }

    /**
     * Commits final text and resets live span, saving committed span for undo.
     */
    fun calculateFinalCommit(
        actualCurrentText: String,
        currentCursor: Int,
        finalText: String,
        separator: String,
        followCursor: Boolean,
        autoLeadingSpace: Boolean = true,
        addTrailingSpace: Boolean = true
    ): InsertionResult {
        val cleanFinalText = if (!addTrailingSpace) finalText.trimEnd(' ') else finalText

        // Check leading space: if active live span already established leading space, reuse it.
        // Otherwise determine freshly against text before cursor.
        val needsLeading = autoLeadingSpace && (hasAppliedLeadingSpace || shouldPrependLeadingSpace(actualCurrentText, currentCursor, cleanFinalText))

        val textWithLeading = if (needsLeading && !cleanFinalText.startsWith(" ")) " $cleanFinalText" else cleanFinalText

        // Determine trailing separator based on user setting
        val trailing = if (addTrailingSpace && !cleanFinalText.endsWith("\n")) {
            if (separator.isNotEmpty()) separator else " "
        } else {
            ""
        }

        val fullCommittedPiece = if (!addTrailingSpace) {
            textWithLeading.trimEnd(' ')
        } else if (textWithLeading.endsWith(" ") && trailing == " ") {
            textWithLeading
        } else {
            textWithLeading + trailing
        }

        val partialRes = calculatePartialInsertion(
            actualCurrentText = actualCurrentText,
            currentCursor = currentCursor,
            partialText = fullCommittedPiece,
            followCursor = followCursor,
            autoLeadingSpace = false // leading space already included in fullCommittedPiece
        )

        lastCommitted = CommittedSpan(
            start = partialRes.updatedSpan.start,
            text = fullCommittedPiece
        )

        // Reset live span and flag after commit so next utterance anchors fresh
        currentSpan = null
        hasAppliedLeadingSpace = false

        return partialRes
    }

    /**
     * Calculates undo by removing the last committed span if still intact.
     */
    fun calculateUndo(actualCurrentText: String): Pair<String, Int>? {
        val committed = lastCommitted ?: return null
        val start = committed.start
        val end = start + committed.text.length

        if (start < 0 || end > actualCurrentText.length) return null

        val currentPiece = actualCurrentText.substring(start, end)
        if (currentPiece != committed.text) return null

        val before = actualCurrentText.substring(0, start)
        val after = actualCurrentText.substring(end)
        val undoneText = before + after

        lastCommitted = null
        currentSpan = null
        return Pair(undoneText, start)
    }

    private fun findNearPosition(text: String, target: String, expectedPos: Int): Int {
        if (target.isEmpty()) return expectedPos
        val searchWindow = 60
        val minSearch = max(0, expectedPos - searchWindow)
        val maxSearch = min(text.length, expectedPos + searchWindow + target.length)
        val windowText = text.substring(minSearch, maxSearch)

        val idxInWindow = windowText.indexOf(target)
        return if (idxInWindow != -1) minSearch + idxInWindow else -1
    }

    private fun clamp(value: Int, min: Int, max: Int): Int {
        return min(max(value, min), max)
    }
}
