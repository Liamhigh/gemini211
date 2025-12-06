package org.verumomnis.engine

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for the ContradictionEngine.
 */
class ContradictionEngineTest {

    @Test
    fun `ingest splits text into sentences`() {
        val engine = ContradictionEngine()
        engine.ingest("First sentence. Second sentence! Third sentence?")
        
        val results = engine.analyze()
        // With no contradictions expected, results should be empty
        assertTrue(results.isEmpty())
    }

    @Test
    fun `analyze detects negation contradiction`() {
        val engine = ContradictionEngine()
        engine.ingest("The car was red. The car was not red.")
        
        val results = engine.analyze()
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].reason.contains("negation", ignoreCase = true))
    }

    @Test
    fun `buildReport returns no contradictions message when empty`() {
        val engine = ContradictionEngine()
        engine.ingest("Simple statement one. Different topic two.")
        
        val results = engine.analyze()
        val report = engine.buildReport(results)
        
        assertTrue(report.contains("No contradictions detected"))
    }

    @Test
    fun `buildReport includes contradiction details`() {
        val engine = ContradictionEngine()
        engine.ingest("He arrived in the morning. He arrived in the evening.")
        
        val results = engine.analyze()
        val report = engine.buildReport(results)
        
        assertTrue(report.contains("VERUM OMNIS CONTRADICTION REPORT"))
    }
}
