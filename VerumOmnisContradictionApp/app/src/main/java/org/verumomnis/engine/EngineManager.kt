package org.verumomnis.engine

/**
 * Engine Manager - Orchestrates all four layers of the Verum Omnis Forensic Engine.
 *
 * The four engines ALWAYS run in this order:
 * 1. Narrative → 2. Contradiction → 3. Classification → 4. Report
 *
 * ENGINE IMMUTABILITY RULE:
 * Logic MUST NEVER change based on evidence.
 *
 * All operations are deterministic, rule-based, and non-AI.
 * Results are repeatable and suitable for legal proceedings.
 */
class EngineManager {

    private val narrativeEngine = NarrativeEngine()
    private val contradictionEngine = ContradictionEngine()
    private val classificationEngine = ClassificationEngine()
    private val reportEngine = ReportEngine()

    // Cached results from each layer
    private var sentences: List<Sentence> = emptyList()
    private var contradictions: List<ContradictionResult> = emptyList()
    private var legalFindings: List<LegalFinding> = emptyList()
    private var finalReport: String = ""

    /**
     * Runs the complete forensic analysis pipeline.
     *
     * Pipeline order (immutable):
     * 1. NarrativeEngine.ingest() → tokenize and normalize
     * 2. ContradictionEngine.analyze() → detect contradictions
     * 3. ClassificationEngine.classify() → map to legal categories
     * 4. ReportEngine.build() → generate final report
     *
     * @param rawText The raw evidence text to analyze
     * @return The complete forensic report
     */
    fun runAnalysis(rawText: String): String {
        // Layer 1: Narrative Engine - Normalize evidence into structured sentences
        narrativeEngine.ingest(rawText)
        sentences = narrativeEngine.getSentences()

        // Layer 2: Contradiction Engine - Detect conflicting statements
        contradictions = contradictionEngine.analyze(sentences)

        // Layer 3: Classification Engine - Map to legal categories
        legalFindings = classificationEngine.classify(contradictions)

        // Layer 4: Report Engine - Build final structured output
        finalReport = reportEngine.build(sentences, contradictions, legalFindings)

        return finalReport
    }

    /**
     * Returns the list of normalized sentences from the last analysis.
     */
    fun getSentences(): List<Sentence> = sentences

    /**
     * Returns the list of detected contradictions from the last analysis.
     */
    fun getContradictions(): List<ContradictionResult> = contradictions

    /**
     * Returns the list of legal findings from the last analysis.
     */
    fun getLegalFindings(): List<LegalFinding> = legalFindings

    /**
     * Returns the final report from the last analysis.
     */
    fun getReport(): String = finalReport

    /**
     * Clears all cached results and resets the engine.
     */
    fun clear() {
        narrativeEngine.clear()
        sentences = emptyList()
        contradictions = emptyList()
        legalFindings = emptyList()
        finalReport = ""
    }
}
