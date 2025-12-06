package org.verumomnis.engine

/**
 * Legal subject categories for classification of contradictions.
 * Each category represents a distinct legal area that may be relevant
 * in forensic analysis of evidence.
 */
enum class LegalSubject {
    /**
     * Shareholder Oppression - Corporate/business conflicts involving
     * profit, agreement, deal, decision, responsibility, ownership disputes.
     */
    ShareholderOppression,

    /**
     * Breach of Fiduciary Duty - Trust/duty conflicts involving
     * managing, accounting, decision-making, duty violations, lies.
     */
    BreachOfFiduciaryDuty,

    /**
     * Cybercrime - Device/account access conflicts involving
     * access, login, password, device, breach, unauthorized actions.
     */
    Cybercrime,

    /**
     * Fraudulent Evidence - Evidence tampering involving
     * delete, removed, cropped, missing, screenshot, edited content.
     */
    FraudulentEvidence,

    /**
     * Emotional Exploitation - Manipulation/denial involving
     * gaslighting, contradictory personal statements, emotional abuse.
     */
    EmotionalExploitation
}
