package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DrawingRevisionSourceTest {
    @Test fun noRevisionsIsPerfectHealth() {
        val summary = computeRevisionRisk(emptyList())
        assertEquals(100, summary.healthScore)
        assertEquals(RevisionRiskBand.LOW, summary.riskBand)
        assertEquals(0, summary.totalRevisions)
    }

    @Test fun allClientRequestedNormalSeverityStaysLowRisk() {
        val revisions = List(6) { DrawingRevisionSource.CLIENT_REQUEST to "NORMAL" }
        val summary = computeRevisionRisk(revisions)
        assertEquals(100, summary.healthScore)
        assertEquals(RevisionRiskBand.LOW, summary.riskBand)
        assertEquals(0, summary.criticalRevisions)
    }

    @Test fun criticalSeverityWeighsMoreThanInternalCorrection() {
        val allInternal = List(4) { DrawingRevisionSource.INTERNAL_CORRECTION to "NORMAL" }
        val allCritical = List(4) { DrawingRevisionSource.CLIENT_REQUEST to "CRITICAL" }
        val internalScore = computeRevisionRisk(allInternal).healthScore
        val criticalScore = computeRevisionRisk(allCritical).healthScore
        assert(criticalScore < internalScore) { "critical severity ($criticalScore) should score worse than internal correction ($internalScore)" }
    }

    @Test fun mostlyInternalCorrectionsAndCriticalSeverityIsHighRisk() {
        val revisions = List(10) { DrawingRevisionSource.INTERNAL_CORRECTION to "CRITICAL" }
        val summary = computeRevisionRisk(revisions)
        assertEquals(RevisionRiskBand.HIGH, summary.riskBand)
        assertEquals(10, summary.criticalRevisions)
        assertEquals(10, summary.internalCorrections)
    }

    @Test fun countsScopeChangesSeparatelyFromHealthScore() {
        val revisions = listOf(
            DrawingRevisionSource.SCOPE_CHANGE to "NORMAL",
            DrawingRevisionSource.SCOPE_CHANGE to "NORMAL",
            DrawingRevisionSource.CLIENT_REQUEST to "NORMAL"
        )
        val summary = computeRevisionRisk(revisions)
        assertEquals(2, summary.scopeChanges)
        assertEquals(100, summary.healthScore)
    }
}
