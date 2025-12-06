package org.verumomnis.engine

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Layer 4 - Report Engine
 *
 * Purpose: Generate final forensic report containing:
 * 1. Narrative summary
 * 2. Contradictions list
 * 3. Legal classification
 * 4. Summary of findings
 *
 * All operations are deterministic, rule-based, and non-AI.
 * Logic MUST NEVER change based on evidence.
 */
class ReportEngine {

    companion object {
        /** Maximum length for truncated sentence display in the narrative table */
        private const val MAX_SENTENCE_DISPLAY_LENGTH = 50
        /** Suffix added to truncated sentences */
        private const val TRUNCATION_SUFFIX = "..."
        /** Effective text length when truncated (MAX_SENTENCE_DISPLAY_LENGTH - TRUNCATION_SUFFIX.length) */
        private const val TRUNCATED_TEXT_LENGTH = MAX_SENTENCE_DISPLAY_LENGTH - 3
    }

    /**
     * Builds the final structured forensic report.
     *
     * Report Sections (in order):
     * 1. PRE-ANALYSIS DECLARATION
     * 2. NARRATIVE STRUCTURE
     * 3. CONTRADICTIONS DETECTED
     * 4. LEGAL CLASSIFICATION
     * 5. SUMMARY FINDINGS
     * 6. POST-ANALYSIS DECLARATION
     *
     * @param sentences List of normalized sentences from NarrativeEngine
     * @param contradictions List of detected contradictions from ContradictionEngine
     * @param legal List of legal findings from ClassificationEngine
     * @return Complete formatted report string
     */
    fun build(
        sentences: List<Sentence>,
        contradictions: List<ContradictionResult>,
        legal: List<LegalFinding>
    ): String {
        val sb = StringBuilder()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val timestamp = dateFormat.format(Date())

        // ═══════════════════════════════════════════════════════════════════════
        // SECTION 1: PRE-ANALYSIS DECLARATION
        // ═══════════════════════════════════════════════════════════════════════
        sb.appendLine("╔══════════════════════════════════════════════════════════════════════════════╗")
        sb.appendLine("║                    VERUM OMNIS FORENSIC ANALYSIS REPORT                     ║")
        sb.appendLine("╚══════════════════════════════════════════════════════════════════════════════╝")
        sb.appendLine()
        sb.appendLine("Report Generated: $timestamp")
        sb.appendLine()
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine("SECTION 1: PRE-ANALYSIS DECLARATION")
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("This is a deterministic forensic report. No AI interpretation included.")
        sb.appendLine("All analysis is based on rule-based pattern matching and keyword detection.")
        sb.appendLine("Results are repeatable and suitable for legal proceedings.")
        sb.appendLine()

        // ═══════════════════════════════════════════════════════════════════════
        // SECTION 2: NARRATIVE STRUCTURE
        // ═══════════════════════════════════════════════════════════════════════
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine("SECTION 2: NARRATIVE STRUCTURE")
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("Total Sentences Analyzed: ${sentences.size}")
        sb.appendLine()
        sb.appendLine(String.format("%-6s | %-20s | %s", "Index", "Timestamp", "Sentence"))
        sb.appendLine("-".repeat(80))

        for (sentence in sentences) {
            val timestampStr = if (sentence.timestamp != null) {
                dateFormat.format(Date(sentence.timestamp))
            } else {
                "N/A"
            }
            val truncatedText = if (sentence.text.length > MAX_SENTENCE_DISPLAY_LENGTH) {
                sentence.text.substring(0, TRUNCATED_TEXT_LENGTH) + TRUNCATION_SUFFIX
            } else {
                sentence.text
            }
            sb.appendLine(String.format("%-6d | %-20s | %s", sentence.index, timestampStr, truncatedText))
        }
        sb.appendLine()

        // ═══════════════════════════════════════════════════════════════════════
        // SECTION 3: CONTRADICTIONS DETECTED
        // ═══════════════════════════════════════════════════════════════════════
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine("SECTION 3: CONTRADICTIONS DETECTED")
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine()

        if (contradictions.isEmpty()) {
            sb.appendLine("No contradictions detected in the provided evidence.")
        } else {
            sb.appendLine("Total Contradictions Found: ${contradictions.size}")
            sb.appendLine()

            contradictions.forEachIndexed { index, result ->
                sb.appendLine("--- Contradiction #${index + 1} ---")
                sb.appendLine("Statement A (Index ${result.a.index}):")
                sb.appendLine("  \"${result.a.text}\"")
                sb.appendLine("Statement B (Index ${result.b.index}):")
                sb.appendLine("  \"${result.b.text}\"")
                sb.appendLine("Reason: ${result.reason}")
                sb.appendLine()
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECTION 4: LEGAL CLASSIFICATION
        // ═══════════════════════════════════════════════════════════════════════
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine("SECTION 4: LEGAL CLASSIFICATION")
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine()

        if (legal.isEmpty()) {
            sb.appendLine("No legal classifications triggered.")
        } else {
            for (finding in legal) {
                sb.appendLine("Subject: ${formatLegalSubject(finding.subject)}")
                sb.appendLine("Evidence:")
                finding.contradictions.forEachIndexed { index, contradiction ->
                    val contradictionIndex = contradictions.indexOf(contradiction) + 1
                    sb.appendLine("  - Contradiction #$contradictionIndex")
                }
                sb.appendLine()
            }
        }

        // ═══════════════════════════════════════════════════════════════════════
        // SECTION 5: SUMMARY FINDINGS
        // ═══════════════════════════════════════════════════════════════════════
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine("SECTION 5: SUMMARY FINDINGS")
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("Total Sentences: ${sentences.size}")
        sb.appendLine("Total Contradictions: ${contradictions.size}")
        sb.appendLine("Categories Triggered: ${legal.size}")

        if (legal.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Legal Categories:")
            for (finding in legal) {
                sb.appendLine("  - ${formatLegalSubject(finding.subject)} (${finding.contradictions.size} contradiction(s))")
            }
        }

        if (contradictions.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Most Severe Contradiction Pair:")
            val mostSevere = findMostSevereContradiction(contradictions, legal)
            sb.appendLine("  Statement A: \"${mostSevere.a.text}\"")
            sb.appendLine("  Statement B: \"${mostSevere.b.text}\"")
            sb.appendLine("  Reason: ${mostSevere.reason}")
        }

        sb.appendLine()

        // ═══════════════════════════════════════════════════════════════════════
        // SECTION 6: POST-ANALYSIS DECLARATION
        // ═══════════════════════════════════════════════════════════════════════
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine("SECTION 6: POST-ANALYSIS DECLARATION")
        sb.appendLine("═══════════════════════════════════════════════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("End of deterministic evaluation.")
        sb.appendLine("This report was generated using the Verum Omnis Forensic Engine.")
        sb.appendLine("All findings are based on pattern matching rules and are reproducible.")
        sb.appendLine()
        sb.appendLine("╔══════════════════════════════════════════════════════════════════════════════╗")
        sb.appendLine("║                              END OF REPORT                                   ║")
        sb.appendLine("╚══════════════════════════════════════════════════════════════════════════════╝")

        return sb.toString()
    }

    /**
     * Formats a LegalSubject enum value into a human-readable string.
     */
    private fun formatLegalSubject(subject: LegalSubject): String {
        return when (subject) {
            LegalSubject.ShareholderOppression -> "Shareholder Oppression"
            LegalSubject.BreachOfFiduciaryDuty -> "Breach of Fiduciary Duty"
            LegalSubject.Cybercrime -> "Cybercrime"
            LegalSubject.FraudulentEvidence -> "Fraudulent Evidence"
            LegalSubject.EmotionalExploitation -> "Emotional Exploitation"
        }
    }

    /**
     * Finds the most severe contradiction based on legal classification count.
     */
    private fun findMostSevereContradiction(
        contradictions: List<ContradictionResult>,
        legal: List<LegalFinding>
    ): ContradictionResult {
        // Count how many legal categories each contradiction appears in
        val severityMap = mutableMapOf<ContradictionResult, Int>()

        for (contradiction in contradictions) {
            var count = 0
            for (finding in legal) {
                if (finding.contradictions.contains(contradiction)) {
                    count++
                }
            }
            severityMap[contradiction] = count
        }

        // Return the contradiction with the highest severity (most categories)
        // or the first one if all have equal severity
        return severityMap.maxByOrNull { it.value }?.key ?: contradictions.first()
    }
}
