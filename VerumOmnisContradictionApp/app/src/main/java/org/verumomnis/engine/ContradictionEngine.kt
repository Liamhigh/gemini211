package org.verumomnis.engine

/**
 * ContradictionEngine - Core engine for detecting contradictions in text evidence.
 *
 * This engine ingests text, splits it into sentences, and analyzes pairs of sentences
 * to detect potential contradictions based on keyword analysis and negation patterns.
 */
class ContradictionEngine {

    private val sentences = mutableListOf<Sentence>()

    /**
     * Ingests raw text evidence and splits it into individual sentences.
     *
     * @param text The raw text to be analyzed
     */
    fun ingest(text: String) {
        sentences.clear()
        val splitSentences = text.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        splitSentences.forEachIndexed { index, sentenceText ->
            sentences.add(Sentence(sentenceText, index))
        }
    }

    /**
     * Analyzes all pairs of sentences to find potential contradictions.
     *
     * @return List of ContradictionResult objects representing detected contradictions
     */
    fun analyze(): List<ContradictionResult> {
        val results = mutableListOf<ContradictionResult>()

        for (i in sentences.indices) {
            for (j in (i + 1) until sentences.size) {
                val a = sentences[i]
                val b = sentences[j]

                val reason = detectContradiction(a.text, b.text)
                if (reason != null) {
                    results.add(ContradictionResult(a, b, reason))
                }
            }
        }

        return results
    }

    /**
     * Detects if two sentences contain a contradiction.
     *
     * @param textA First sentence text
     * @param textB Second sentence text
     * @return Reason for contradiction, or null if no contradiction detected
     */
    private fun detectContradiction(textA: String, textB: String): String? {
        val lowerA = textA.lowercase()
        val lowerB = textB.lowercase()

        // Check for negation patterns
        val negationPatterns = listOf(
            "not", "never", "no ", "don't", "doesn't", "didn't", "wasn't",
            "weren't", "isn't", "aren't", "won't", "wouldn't", "couldn't",
            "shouldn't", "can't", "cannot"
        )

        val aHasNegation = negationPatterns.any { lowerA.contains(it) }
        val bHasNegation = negationPatterns.any { lowerB.contains(it) }

        // Extract key words (nouns/verbs) for comparison
        val keyWordsA = extractKeyWords(lowerA)
        val keyWordsB = extractKeyWords(lowerB)

        // Find common key words
        val commonWords = keyWordsA.intersect(keyWordsB)

        // If there are common key words and one has negation while the other doesn't
        if (commonWords.isNotEmpty() && aHasNegation != bHasNegation) {
            return "Potential negation contradiction on topic: ${commonWords.joinToString(", ")}"
        }

        // Check for opposite time references
        val timeContradiction = detectTimeContradiction(lowerA, lowerB, commonWords)
        if (timeContradiction != null) {
            return timeContradiction
        }

        // Check for quantity contradictions
        val quantityContradiction = detectQuantityContradiction(lowerA, lowerB, commonWords)
        if (quantityContradiction != null) {
            return quantityContradiction
        }

        return null
    }

    /**
     * Extracts key words from a sentence, filtering out common stop words.
     */
    private fun extractKeyWords(text: String): Set<String> {
        val stopWords = setOf(
            "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "could",
            "should", "may", "might", "must", "shall", "can", "need", "dare",
            "to", "of", "in", "for", "on", "with", "at", "by", "from", "as",
            "into", "through", "during", "before", "after", "above", "below",
            "between", "under", "again", "further", "then", "once", "here",
            "there", "when", "where", "why", "how", "all", "each", "few",
            "more", "most", "other", "some", "such", "only", "own", "same",
            "so", "than", "too", "very", "just", "and", "but", "if", "or",
            "because", "until", "while", "although", "though", "i", "you",
            "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
            "my", "your", "his", "its", "our", "their", "this", "that", "these",
            "those", "what", "which", "who", "whom", "whose"
        )

        return text.split(Regex("\\W+"))
            .filter { it.length > 2 && it !in stopWords }
            .toSet()
    }

    /**
     * Detects time-based contradictions between two sentences.
     */
    private fun detectTimeContradiction(
        textA: String,
        textB: String,
        commonWords: Set<String>
    ): String? {
        if (commonWords.isEmpty()) return null

        val morningTerms = setOf("morning", "am", "early", "sunrise", "dawn")
        val eveningTerms = setOf("evening", "pm", "night", "sunset", "dusk", "late")

        val aHasMorning = morningTerms.any { textA.contains(it) }
        val aHasEvening = eveningTerms.any { textA.contains(it) }
        val bHasMorning = morningTerms.any { textB.contains(it) }
        val bHasEvening = eveningTerms.any { textB.contains(it) }

        if ((aHasMorning && bHasEvening) || (aHasEvening && bHasMorning)) {
            return "Time contradiction detected regarding: ${commonWords.joinToString(", ")}"
        }

        return null
    }

    /**
     * Detects quantity contradictions between two sentences.
     */
    private fun detectQuantityContradiction(
        textA: String,
        textB: String,
        commonWords: Set<String>
    ): String? {
        if (commonWords.isEmpty()) return null

        // Extract numbers from both texts
        val numbersA = Regex("\\d+").findAll(textA).map { it.value.toInt() }.toList()
        val numbersB = Regex("\\d+").findAll(textB).map { it.value.toInt() }.toList()

        // If both have numbers and they differ significantly
        if (numbersA.isNotEmpty() && numbersB.isNotEmpty()) {
            val maxA = numbersA.maxOrNull() ?: 0
            val maxB = numbersB.maxOrNull() ?: 0

            if (maxA != maxB && (maxA == 0 || maxB == 0 || maxA.toDouble() / maxB > 2 || maxB.toDouble() / maxA > 2)) {
                return "Quantity discrepancy detected ($maxA vs $maxB) regarding: ${commonWords.joinToString(", ")}"
            }
        }

        return null
    }

    /**
     * Builds a formatted report from the analysis results.
     *
     * @param results List of contradiction results to include in the report
     * @return Formatted string report
     */
    fun buildReport(results: List<ContradictionResult>): String {
        if (results.isEmpty()) {
            return "=== VERUM OMNIS CONTRADICTION REPORT ===\n\nNo contradictions detected in the provided evidence.\n"
        }

        val sb = StringBuilder()
        sb.appendLine("=== VERUM OMNIS CONTRADICTION REPORT ===")
        sb.appendLine()
        sb.appendLine("Total contradictions found: ${results.size}")
        sb.appendLine()

        results.forEachIndexed { index, result ->
            sb.appendLine("--- Contradiction #${index + 1} ---")
            sb.appendLine("Statement A (Index ${result.a.index}): \"${result.a.text}\"")
            sb.appendLine("Statement B (Index ${result.b.index}): \"${result.b.text}\"")
            sb.appendLine("Reason: ${result.reason}")
            sb.appendLine()
        }

        sb.appendLine("=== END OF REPORT ===")
        return sb.toString()
    }
}
