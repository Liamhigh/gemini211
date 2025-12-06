package org.verumomnis.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the Verum Omnis Forensic Engine (all four layers).
 */
class ContradictionEngineTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // NARRATIVE ENGINE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `NarrativeEngine tokenizes text into sentences`() {
        val engine = NarrativeEngine()
        engine.ingest("First sentence. Second sentence! Third sentence?")

        val sentences = engine.getSentences()
        assertEquals(3, sentences.size)
        assertEquals("First sentence", sentences[0].text)
        assertEquals("Second sentence", sentences[1].text)
        assertEquals("Third sentence", sentences[2].text)
    }

    @Test
    fun `NarrativeEngine preserves sentence order with index`() {
        val engine = NarrativeEngine()
        engine.ingest("Statement A. Statement B. Statement C.")

        val sentences = engine.getSentences()
        assertEquals(0, sentences[0].index)
        assertEquals(1, sentences[1].index)
        assertEquals(2, sentences[2].index)
    }

    @Test
    fun `NarrativeEngine normalizes whitespace`() {
        val engine = NarrativeEngine()
        val normalized = engine.normalize("  Multiple   spaces   here  ")
        assertEquals("Multiple spaces here", normalized)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CONTRADICTION ENGINE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `ContradictionEngine Rule 1 - Direct Negation`() {
        val narrativeEngine = NarrativeEngine()
        narrativeEngine.ingest("The car was red. The car was not red.")
        val sentences = narrativeEngine.getSentences()

        val engine = ContradictionEngine()
        val results = engine.analyze(sentences)

        assertTrue(results.isNotEmpty())
        assertTrue(results[0].reason.contains("RULE 1", ignoreCase = true))
    }

    @Test
    fun `ContradictionEngine Rule 2 - Denial vs Evidence`() {
        val narrativeEngine = NarrativeEngine()
        narrativeEngine.ingest("I never met him. We met on Monday.")
        val sentences = narrativeEngine.getSentences()

        val engine = ContradictionEngine()
        val results = engine.analyze(sentences)

        assertTrue(results.isNotEmpty())
        assertTrue(results[0].reason.contains("RULE 2", ignoreCase = true))
    }

    @Test
    fun `ContradictionEngine Rule 3 - Timeline Conflicts`() {
        val narrativeEngine = NarrativeEngine()
        narrativeEngine.ingest("The meeting was in January. The meeting was in March.")
        val sentences = narrativeEngine.getSentences()

        val engine = ContradictionEngine()
        val results = engine.analyze(sentences)

        assertTrue(results.isNotEmpty())
        assertTrue(results[0].reason.contains("RULE 3", ignoreCase = true))
    }

    @Test
    fun `ContradictionEngine Rule 4 - Quantity Conflicts`() {
        val narrativeEngine = NarrativeEngine()
        narrativeEngine.ingest("There was one meeting. There were three meetings.")
        val sentences = narrativeEngine.getSentences()

        val engine = ContradictionEngine()
        val results = engine.analyze(sentences)

        assertTrue(results.isNotEmpty())
        assertTrue(results[0].reason.contains("RULE 4", ignoreCase = true))
    }

    @Test
    fun `ContradictionEngine Rule 5 - Admission vs Denial`() {
        val narrativeEngine = NarrativeEngine()
        narrativeEngine.ingest("I agreed to the terms. I never agreed to anything.")
        val sentences = narrativeEngine.getSentences()

        val engine = ContradictionEngine()
        val results = engine.analyze(sentences)

        assertTrue(results.isNotEmpty())
        assertTrue(results[0].reason.contains("RULE 5", ignoreCase = true))
    }

    @Test
    fun `ContradictionEngine returns empty for non-contradicting sentences`() {
        val narrativeEngine = NarrativeEngine()
        narrativeEngine.ingest("The sky is blue. The grass is green.")
        val sentences = narrativeEngine.getSentences()

        val engine = ContradictionEngine()
        val results = engine.analyze(sentences)

        assertTrue(results.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CLASSIFICATION ENGINE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `ClassificationEngine classifies ShareholderOppression`() {
        val sentence1 = Sentence("The profit was shared equally.", 0)
        val sentence2 = Sentence("The profit was not shared at all.", 1)
        val contradiction = ContradictionResult(sentence1, sentence2, "Test reason")

        val engine = ClassificationEngine()
        val findings = engine.classify(listOf(contradiction))

        assertTrue(findings.any { it.subject == LegalSubject.ShareholderOppression })
    }

    @Test
    fun `ClassificationEngine classifies Cybercrime`() {
        val sentence1 = Sentence("I did not access the account.", 0)
        val sentence2 = Sentence("Login records show access was made.", 1)
        val contradiction = ContradictionResult(sentence1, sentence2, "Test reason")

        val engine = ClassificationEngine()
        val findings = engine.classify(listOf(contradiction))

        assertTrue(findings.any { it.subject == LegalSubject.Cybercrime })
    }

    @Test
    fun `ClassificationEngine returns empty for unclassifiable contradictions`() {
        val sentence1 = Sentence("Hello there.", 0)
        val sentence2 = Sentence("Goodbye now.", 1)
        val contradiction = ContradictionResult(sentence1, sentence2, "Test reason")

        val engine = ClassificationEngine()
        val findings = engine.classify(listOf(contradiction))

        assertTrue(findings.isEmpty())
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REPORT ENGINE TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `ReportEngine builds complete report with all sections`() {
        val sentences = listOf(
            Sentence("Statement one.", 0),
            Sentence("Statement two.", 1)
        )
        val contradiction = ContradictionResult(sentences[0], sentences[1], "Test contradiction")
        val finding = LegalFinding(LegalSubject.Cybercrime, listOf(contradiction))

        val engine = ReportEngine()
        val report = engine.build(sentences, listOf(contradiction), listOf(finding))

        assertTrue(report.contains("VERUM OMNIS FORENSIC ANALYSIS REPORT"))
        assertTrue(report.contains("PRE-ANALYSIS DECLARATION"))
        assertTrue(report.contains("NARRATIVE STRUCTURE"))
        assertTrue(report.contains("CONTRADICTIONS DETECTED"))
        assertTrue(report.contains("LEGAL CLASSIFICATION"))
        assertTrue(report.contains("SUMMARY FINDINGS"))
        assertTrue(report.contains("POST-ANALYSIS DECLARATION"))
        assertTrue(report.contains("End of deterministic evaluation"))
    }

    @Test
    fun `ReportEngine handles empty contradictions`() {
        val sentences = listOf(
            Sentence("Statement one.", 0),
            Sentence("Statement two.", 1)
        )

        val engine = ReportEngine()
        val report = engine.build(sentences, emptyList(), emptyList())

        assertTrue(report.contains("No contradictions detected"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ENGINE MANAGER INTEGRATION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun `EngineManager runs complete analysis pipeline`() {
        val manager = EngineManager()
        val rawText = "I never met the client. We met the client on Monday at the office."

        val report = manager.runAnalysis(rawText)

        assertTrue(report.isNotEmpty())
        assertTrue(report.contains("VERUM OMNIS FORENSIC ANALYSIS REPORT"))
        assertEquals(2, manager.getSentences().size)
        assertTrue(manager.getContradictions().isNotEmpty())
    }

    @Test
    fun `EngineManager clear resets all state`() {
        val manager = EngineManager()
        manager.runAnalysis("Test sentence one. Test sentence two.")

        manager.clear()

        assertTrue(manager.getSentences().isEmpty())
        assertTrue(manager.getContradictions().isEmpty())
        assertTrue(manager.getLegalFindings().isEmpty())
        assertTrue(manager.getReport().isEmpty())
    }
}
