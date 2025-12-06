package org.verumomnis.engine

/**
 * Data class representing a legal finding from the classification engine.
 *
 * @param subject The legal subject category
 * @param contradictions List of contradictions that triggered this classification
 */
data class LegalFinding(
    val subject: LegalSubject,
    val contradictions: List<ContradictionResult>
)
