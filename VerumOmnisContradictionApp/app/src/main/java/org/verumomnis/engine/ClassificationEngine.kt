package org.verumomnis.engine

/**
 * Layer 3 - Classification Engine
 *
 * Purpose: Map contradictions to legal subject categories defined in Verum Omnis.
 * Each contradiction may map to more than one category.
 *
 * All operations are deterministic, rule-based, and non-AI.
 * Logic MUST NEVER change based on evidence.
 */
class ClassificationEngine {

    /**
     * Classifies a list of contradictions into legal subject categories.
     *
     * @param results List of contradiction results to classify
     * @return List of LegalFinding objects grouping contradictions by legal subject
     */
    fun classify(results: List<ContradictionResult>): List<LegalFinding> {
        val findingsMap = mutableMapOf<LegalSubject, MutableList<ContradictionResult>>()

        for (result in results) {
            val subjects = classifyContradiction(result)
            for (subject in subjects) {
                findingsMap.getOrPut(subject) { mutableListOf() }.add(result)
            }
        }

        return findingsMap.map { (subject, contradictions) ->
            LegalFinding(subject, contradictions)
        }.sortedBy { it.subject.name }
    }

    /**
     * Classifies a single contradiction into applicable legal categories.
     *
     * @param result The contradiction result to classify
     * @return Set of applicable LegalSubject categories
     */
    private fun classifyContradiction(result: ContradictionResult): Set<LegalSubject> {
        val subjects = mutableSetOf<LegalSubject>()
        val textA = result.a.text.lowercase()
        val textB = result.b.text.lowercase()
        val combinedText = "$textA $textB"

        // RULE A — Corporate / Business Conflicts → ShareholderOppression
        if (checkShareholderOppression(combinedText)) {
            subjects.add(LegalSubject.ShareholderOppression)
        }

        // RULE B — Evidence Tampering → FraudulentEvidence
        if (checkFraudulentEvidence(combinedText)) {
            subjects.add(LegalSubject.FraudulentEvidence)
        }

        // RULE C — Device / Account Access → Cybercrime
        if (checkCybercrime(combinedText)) {
            subjects.add(LegalSubject.Cybercrime)
        }

        // RULE D — Trust / Duty Conflicts → BreachOfFiduciaryDuty
        if (checkBreachOfFiduciaryDuty(combinedText)) {
            subjects.add(LegalSubject.BreachOfFiduciaryDuty)
        }

        // RULE E — Manipulation / Denial → EmotionalExploitation
        if (checkEmotionalExploitation(combinedText)) {
            subjects.add(LegalSubject.EmotionalExploitation)
        }

        return subjects
    }

    /**
     * RULE A — Corporate / Business Conflicts → ShareholderOppression
     * Trigger keywords: ["profit", "agreement", "deal", "decision", "responsibility", "ownership"]
     */
    private fun checkShareholderOppression(text: String): Boolean {
        val keywords = listOf(
            "profit", "agreement", "deal", "decision", "responsibility", "ownership",
            "shareholder", "dividend", "equity", "stock", "shares", "corporate",
            "business", "company", "partnership", "investment", "board", "vote",
            "majority", "minority", "buyout", "merger", "acquisition"
        )
        return keywords.any { text.contains(it) }
    }

    /**
     * RULE B — Evidence Tampering → FraudulentEvidence
     * Trigger keywords: ["delete", "removed", "cropped", "missing", "screenshot", "edited"]
     */
    private fun checkFraudulentEvidence(text: String): Boolean {
        val keywords = listOf(
            "delete", "deleted", "removed", "cropped", "missing", "screenshot",
            "edited", "altered", "modified", "fabricated", "forged", "fake",
            "tampered", "manipulated", "doctored", "photoshopped", "redacted",
            "erased", "destroyed", "hidden", "concealed"
        )
        return keywords.any { text.contains(it) }
    }

    /**
     * RULE C — Device / Account Access → Cybercrime
     * Trigger keywords: ["access", "login", "password", "device", "breach", "unauthorized"]
     */
    private fun checkCybercrime(text: String): Boolean {
        val keywords = listOf(
            "access", "login", "password", "device", "breach", "unauthorized",
            "hacked", "hacking", "account", "computer", "phone", "email account",
            "ip address", "vpn", "encryption", "malware", "phishing", "spyware",
            "keylogger", "data theft", "identity theft", "credential"
        )
        return keywords.any { text.contains(it) }
    }

    /**
     * RULE D — Trust / Duty Conflicts → BreachOfFiduciaryDuty
     * Trigger keywords: ["managing", "accounting", "decision-making", "duty", "lied"]
     */
    private fun checkBreachOfFiduciaryDuty(text: String): Boolean {
        val keywords = listOf(
            "managing", "accounting", "decision-making", "duty", "lied",
            "fiduciary", "trustee", "executor", "guardian", "agent",
            "represent", "authority", "power of attorney", "behalf",
            "entrusted", "misappropriation", "embezzlement", "self-dealing",
            "conflict of interest", "loyalty", "negligence"
        )
        return keywords.any { text.contains(it) }
    }

    /**
     * RULE E — Manipulation / Denial → EmotionalExploitation
     * Trigger keywords: ["gaslight", "you said", "you did", "never happened", "emotional"]
     */
    private fun checkEmotionalExploitation(text: String): Boolean {
        val keywords = listOf(
            "gaslight", "gaslighting", "you said", "you did", "never happened",
            "emotional", "manipulate", "manipulation", "abuse", "abusive",
            "control", "controlling", "isolate", "isolated", "threaten",
            "intimidate", "coerce", "coercion", "exploit", "vulnerable",
            "mental health", "crazy", "imagining", "making it up"
        )
        return keywords.any { text.contains(it) }
    }
}
