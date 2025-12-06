package org.verumomnis.engine

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern

/**
 * Layer 1 - Narrative Engine
 *
 * Purpose: Convert raw text evidence into clean, indexed, timestamp-aware sentences.
 * This engine normalizes evidence into a structured format suitable for contradiction analysis.
 *
 * All operations are deterministic, rule-based, and non-AI.
 */
class NarrativeEngine {

    private val sentences = mutableListOf<Sentence>()

    /**
     * Ingests raw text evidence and prepares it for tokenization.
     *
     * @param rawText The raw text evidence to be processed
     */
    fun ingest(rawText: String) {
        sentences.clear()
        val tokenized = tokenize(rawText)
        sentences.addAll(tokenized)
    }

    /**
     * Tokenizes raw text into a list of normalized sentences.
     *
     * @param rawText The raw text to tokenize
     * @return List of Sentence objects with text, index, and optional timestamp
     */
    fun tokenize(rawText: String): List<Sentence> {
        val result = mutableListOf<Sentence>()

        // Split raw text into sentences using punctuation rules
        val splitSentences = rawText.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        splitSentences.forEachIndexed { index, sentenceText ->
            val normalizedText = normalize(sentenceText)
            val timestamp = extractTimestamp(sentenceText)
            result.add(Sentence(normalizedText, index, timestamp))
        }

        return result
    }

    /**
     * Extracts timestamp from a sentence if present.
     * Supports formats: YYYY-MM-DD, MM/DD/YYYY, HH:MM, HH:MM:SS
     *
     * @param sentence The sentence to extract timestamp from
     * @return Epoch milliseconds if timestamp found, null otherwise
     */
    fun extractTimestamp(sentence: String): Long? {
        // Date patterns ordered by specificity (most specific first)
        // Note: We use MM/dd/yyyy format for US locale consistency.
        // The dd/MM/yyyy format is intentionally excluded to avoid ambiguity
        // since SimpleDateFormat cannot distinguish between the two when
        // both day and month values are <= 12.
        val datePatterns = listOf(
            "yyyy-MM-dd HH:mm:ss" to Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}"),
            "yyyy-MM-dd HH:mm" to Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}"),
            "yyyy-MM-dd" to Pattern.compile("\\d{4}-\\d{2}-\\d{2}"),
            "MM/dd/yyyy HH:mm" to Pattern.compile("\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2}"),
            "MM/dd/yyyy" to Pattern.compile("\\d{2}/\\d{2}/\\d{4}")
        )

        for ((format, pattern) in datePatterns) {
            val matcher = pattern.matcher(sentence)
            if (matcher.find()) {
                try {
                    val dateFormat = SimpleDateFormat(format, Locale.US)
                    val date = dateFormat.parse(matcher.group())
                    return date?.time
                } catch (_: Exception) {
                    // Continue to next pattern
                }
            }
        }

        return null
    }

    /**
     * Normalizes a sentence by cleaning whitespace and standardizing format.
     *
     * @param sentence The sentence to normalize
     * @return Normalized sentence text
     */
    fun normalize(sentence: String): String {
        return sentence
            .replace(Regex("\\s+"), " ")  // Collapse multiple spaces
            .trim()                        // Remove leading/trailing whitespace
    }

    /**
     * Returns the list of processed sentences.
     *
     * @return List of normalized Sentence objects
     */
    fun getSentences(): List<Sentence> {
        return sentences.toList()
    }

    /**
     * Clears all processed sentences.
     */
    fun clear() {
        sentences.clear()
    }
}
