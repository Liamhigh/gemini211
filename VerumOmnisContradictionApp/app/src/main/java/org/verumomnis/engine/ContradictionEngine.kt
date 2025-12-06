package org.verumomnis.engine

/**
 * Layer 2 - Contradiction Engine
 *
 * Purpose: Detect contradictions between any two sentences.
 * Implements all seven contradiction rules for forensic analysis.
 *
 * All operations are deterministic, rule-based, and non-AI.
 * Logic MUST NEVER change based on evidence.
 */
class ContradictionEngine {

    companion object {
        /** Pre-compiled regex pattern for standalone 'no' word boundary matching */
        private val NO_PATTERN = Regex("\\bno\\b")

        /** Pre-compiled regex patterns for number words (word boundary matching) */
        private val NUMBER_WORD_PATTERNS = mapOf(
            Regex("\\bone\\b") to 1,
            Regex("\\btwo\\b") to 2,
            Regex("\\bthree\\b") to 3,
            Regex("\\bfour\\b") to 4,
            Regex("\\bfive\\b") to 5,
            Regex("\\bsix\\b") to 6,
            Regex("\\bseven\\b") to 7,
            Regex("\\beight\\b") to 8,
            Regex("\\bnine\\b") to 9,
            Regex("\\bten\\b") to 10,
            Regex("\\bzero\\b") to 0,
            Regex("\\bnone\\b") to 0
        )

        /** Pre-compiled regex for extracting digit numbers */
        private val DIGIT_PATTERN = Regex("\\d+")
    }

    /**
     * Analyzes all pairs of sentences to find potential contradictions.
     * Implements Rules 1-7 as specified in the Verum Omnis specification.
     *
     * @param sentences List of sentences to analyze
     * @return List of ContradictionResult objects representing detected contradictions
     */
    fun analyze(sentences: List<Sentence>): List<ContradictionResult> {
        val results = mutableListOf<ContradictionResult>()

        for (i in sentences.indices) {
            for (j in (i + 1) until sentences.size) {
                val a = sentences[i]
                val b = sentences[j]

                val reason = contradictionRule(a, b)
                if (reason != null) {
                    results.add(ContradictionResult(a, b, reason))
                }
            }
        }

        return results
    }

    /**
     * Applies all contradiction rules to a pair of sentences.
     *
     * @param a First sentence
     * @param b Second sentence
     * @return Reason string if contradiction detected, null otherwise
     */
    fun contradictionRule(a: Sentence, b: Sentence): String? {
        val textA = a.text.lowercase()
        val textB = b.text.lowercase()

        // Extract key words for topic comparison
        val keyWordsA = extractKeyWords(textA)
        val keyWordsB = extractKeyWords(textB)
        val commonWords = keyWordsA.intersect(keyWordsB)

        // RULE 1 — Direct Negation
        val rule1 = checkDirectNegation(textA, textB, commonWords)
        if (rule1 != null) return rule1

        // RULE 2 — Denial vs Evidence
        val rule2 = checkDenialVsEvidence(textA, textB, commonWords)
        if (rule2 != null) return rule2

        // RULE 3 — Timeline Conflicts
        val rule3 = checkTimelineConflicts(textA, textB, commonWords)
        if (rule3 != null) return rule3

        // RULE 4 — Quantity Conflicts
        val rule4 = checkQuantityConflicts(textA, textB, commonWords)
        if (rule4 != null) return rule4

        // RULE 5 — Admission vs Later Denial
        val rule5 = checkAdmissionVsDenial(textA, textB, commonWords)
        if (rule5 != null) return rule5

        // RULE 6 — Action vs Outcome Conflict
        val rule6 = checkActionVsOutcome(textA, textB, commonWords)
        if (rule6 != null) return rule6

        // RULE 7 — Data Access Claim Conflicts
        val rule7 = checkDataAccessConflicts(textA, textB, commonWords)
        if (rule7 != null) return rule7

        return null
    }

    /**
     * RULE 1 — Direct Negation
     * If one sentence contains 'never', 'did not', 'no', while the other affirms same event.
     */
    private fun checkDirectNegation(textA: String, textB: String, commonWords: Set<String>): String? {
        if (commonWords.isEmpty()) return null

        val negationPatterns = listOf(
            "never", "did not", "didn't", "do not", "don't", "does not", "doesn't",
            "was not", "wasn't", "were not", "weren't", "is not", "isn't",
            "are not", "aren't", "will not", "won't", "would not", "wouldn't",
            "could not", "couldn't", "should not", "shouldn't", "can not",
            "cannot", "can't"
        )

        // Use pre-compiled word boundary regex for standalone 'no' to avoid false positives
        val aHasNegation = negationPatterns.any { textA.contains(it) } || NO_PATTERN.containsMatchIn(textA)
        val bHasNegation = negationPatterns.any { textB.contains(it) } || NO_PATTERN.containsMatchIn(textB)

        if (commonWords.isNotEmpty() && aHasNegation != bHasNegation) {
            return "RULE 1 - Direct Negation: One statement negates while other affirms regarding: ${commonWords.joinToString(", ")}"
        }

        return null
    }

    /**
     * RULE 2 — Denial vs Evidence
     * "no payment" vs "payment made", "I never met him" vs "we met Monday"
     */
    private fun checkDenialVsEvidence(textA: String, textB: String, commonWords: Set<String>): String? {
        val denialEvidencePairs = listOf(
            Pair("no payment", "payment"),
            Pair("never met", "met"),
            Pair("never spoke", "spoke"),
            Pair("never sent", "sent"),
            Pair("never received", "received"),
            Pair("no contact", "contact"),
            Pair("no communication", "communicated"),
            Pair("never agreed", "agreed"),
            Pair("never signed", "signed"),
            Pair("no record", "record")
        )

        for ((denial, evidence) in denialEvidencePairs) {
            val aHasDenial = textA.contains(denial)
            val bHasDenial = textB.contains(denial)
            val aHasEvidence = textA.contains(evidence) && !textA.contains(denial)
            val bHasEvidence = textB.contains(evidence) && !textB.contains(denial)

            if ((aHasDenial && bHasEvidence) || (bHasDenial && aHasEvidence)) {
                return "RULE 2 - Denial vs Evidence: '$denial' contradicts evidence of '$evidence'"
            }
        }

        return null
    }

    /**
     * RULE 3 — Timeline Conflicts
     * Different months/dates for same event topic: (met, invoice, payment, call, meeting)
     */
    private fun checkTimelineConflicts(textA: String, textB: String, commonWords: Set<String>): String? {
        if (commonWords.isEmpty()) return null

        val eventTopics = setOf("met", "meeting", "invoice", "payment", "call", "email", "contract", "signed")
        val hasEventTopic = commonWords.any { topic -> eventTopics.any { it in topic || topic in it } }

        if (!hasEventTopic) return null

        // Check for month conflicts
        val months = listOf(
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
        )

        val monthsInA = months.filter { textA.contains(it) }
        val monthsInB = months.filter { textB.contains(it) }

        if (monthsInA.isNotEmpty() && monthsInB.isNotEmpty() && monthsInA.intersect(monthsInB.toSet()).isEmpty()) {
            return "RULE 3 - Timeline Conflict: Different months (${monthsInA.first()} vs ${monthsInB.first()}) for same event: ${commonWords.joinToString(", ")}"
        }

        // Check for day conflicts (Monday vs Friday, etc.)
        val days = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        val daysInA = days.filter { textA.contains(it) }
        val daysInB = days.filter { textB.contains(it) }

        if (daysInA.isNotEmpty() && daysInB.isNotEmpty() && daysInA.intersect(daysInB.toSet()).isEmpty()) {
            return "RULE 3 - Timeline Conflict: Different days (${daysInA.first()} vs ${daysInB.first()}) for same event: ${commonWords.joinToString(", ")}"
        }

        // Check for time of day conflicts
        val morningTerms = setOf("morning", "am", "early", "sunrise", "dawn")
        val eveningTerms = setOf("evening", "pm", "night", "sunset", "dusk", "late")

        val aHasMorning = morningTerms.any { textA.contains(it) }
        val aHasEvening = eveningTerms.any { textA.contains(it) }
        val bHasMorning = morningTerms.any { textB.contains(it) }
        val bHasEvening = eveningTerms.any { textB.contains(it) }

        if ((aHasMorning && bHasEvening) || (aHasEvening && bHasMorning)) {
            return "RULE 3 - Timeline Conflict: Morning vs evening contradiction for: ${commonWords.joinToString(", ")}"
        }

        return null
    }

    /**
     * RULE 4 — Quantity Conflicts
     * Different numbers for same subject: ("one meeting" vs "three meetings")
     */
    private fun checkQuantityConflicts(textA: String, textB: String, commonWords: Set<String>): String? {
        if (commonWords.isEmpty()) return null

        val numbersA = mutableListOf<Int>()
        val numbersB = mutableListOf<Int>()

        // Extract digit numbers using pre-compiled pattern
        numbersA.addAll(DIGIT_PATTERN.findAll(textA).map { it.value.toInt() })
        numbersB.addAll(DIGIT_PATTERN.findAll(textB).map { it.value.toInt() })

        // Extract word numbers using pre-compiled word boundary patterns
        for ((pattern, num) in NUMBER_WORD_PATTERNS) {
            if (pattern.containsMatchIn(textA)) numbersA.add(num)
            if (pattern.containsMatchIn(textB)) numbersB.add(num)
        }

        if (numbersA.isNotEmpty() && numbersB.isNotEmpty()) {
            val maxA = numbersA.maxOrNull() ?: 0
            val maxB = numbersB.maxOrNull() ?: 0

            if (maxA != maxB) {
                return "RULE 4 - Quantity Conflict: Different quantities ($maxA vs $maxB) regarding: ${commonWords.joinToString(", ")}"
            }
        }

        return null
    }

    /**
     * RULE 5 — Admission vs Later Denial
     * "I agreed" vs "I never agreed"
     */
    private fun checkAdmissionVsDenial(textA: String, textB: String, commonWords: Set<String>): String? {
        val admissionPatterns = listOf(
            "i agreed", "i accepted", "i confirmed", "i approved", "i authorized",
            "i admitted", "i acknowledged", "i consented", "yes i did", "i did it",
            "i was there", "i took", "i received", "i sent"
        )

        val denialPatterns = listOf(
            "never agreed", "never accepted", "never confirmed", "never approved",
            "never authorized", "never admitted", "never acknowledged", "never consented",
            "i did not", "i didn't", "i wasn't there", "never took", "never received", "never sent"
        )

        val aHasAdmission = admissionPatterns.any { textA.contains(it) }
        val bHasAdmission = admissionPatterns.any { textB.contains(it) }
        val aHasDenial = denialPatterns.any { textA.contains(it) }
        val bHasDenial = denialPatterns.any { textB.contains(it) }

        if ((aHasAdmission && bHasDenial) || (bHasAdmission && aHasDenial)) {
            return "RULE 5 - Admission vs Denial: Prior admission contradicts later denial"
        }

        return null
    }

    /**
     * RULE 6 — Action vs Outcome Conflict
     * "I sent nothing" vs "email attached"
     */
    private fun checkActionVsOutcome(textA: String, textB: String, commonWords: Set<String>): String? {
        val actionOutcomePairs = listOf(
            Pair(listOf("sent nothing", "never sent", "did not send"), listOf("email attached", "attachment", "sent email", "message received")),
            Pair(listOf("never called", "no call", "did not call"), listOf("call log", "phone record", "spoke on phone")),
            Pair(listOf("never paid", "no payment", "did not pay"), listOf("payment received", "transaction", "bank transfer")),
            Pair(listOf("never signed", "did not sign"), listOf("signature", "signed document", "contract signed")),
            Pair(listOf("never accessed", "did not access"), listOf("login record", "access log", "logged in"))
        )

        for ((noAction, outcomes) in actionOutcomePairs) {
            val aHasNoAction = noAction.any { textA.contains(it) }
            val bHasNoAction = noAction.any { textB.contains(it) }
            val aHasOutcome = outcomes.any { textA.contains(it) }
            val bHasOutcome = outcomes.any { textB.contains(it) }

            if ((aHasNoAction && bHasOutcome) || (bHasNoAction && aHasOutcome)) {
                return "RULE 6 - Action vs Outcome: Claimed inaction contradicts evidence of outcome"
            }
        }

        return null
    }

    /**
     * RULE 7 — Data Access Claim Conflicts
     * "I did not access" vs evidence of access attempt
     */
    private fun checkDataAccessConflicts(textA: String, textB: String, commonWords: Set<String>): String? {
        val accessDenials = listOf(
            "did not access", "didn't access", "never accessed", "no access",
            "did not login", "didn't login", "never logged in", "did not log in",
            "never opened", "did not open", "never viewed", "did not view"
        )

        val accessEvidence = listOf(
            "access log", "login record", "login attempt", "logged in", "accessed",
            "opened file", "viewed document", "download record", "browsing history",
            "ip address", "device log", "authentication", "session record"
        )

        val aHasDenial = accessDenials.any { textA.contains(it) }
        val bHasDenial = accessDenials.any { textB.contains(it) }
        val aHasEvidence = accessEvidence.any { textA.contains(it) }
        val bHasEvidence = accessEvidence.any { textB.contains(it) }

        if ((aHasDenial && bHasEvidence) || (bHasDenial && aHasEvidence)) {
            return "RULE 7 - Data Access Conflict: Access denial contradicts access evidence"
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
}
