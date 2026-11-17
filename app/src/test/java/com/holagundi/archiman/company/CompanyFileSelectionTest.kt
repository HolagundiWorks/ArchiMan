package com.holagundi.archiman.company

import org.junit.Assert.*
import org.junit.Test

class CompanyFileSelectionTest {
    @Test fun activeRequestSurvivesConsumerRemovalButResultIsConsumedOnce() {
        val selection = CompanyFileSelection<String>()
        selection.begin("https://company.example")
        selection.complete("fictional-local-uri")
        assertTrue(selection.hasPending())
        assertEquals("https://company.example" to "fictional-local-uri", selection.take())
        assertNull(selection.take())
        assertFalse(selection.hasPending())
    }
    @Test fun restoredOrCancelledResultsAreIgnoredAndNewRequestsClearOldResults() {
        val restored = CompanyFileSelection<String>()
        restored.complete("restored-uri")
        assertNull(restored.take())
        restored.begin("https://company.example")
        restored.complete(null)
        assertNull(restored.take())
        restored.begin("https://company.example")
        restored.complete("old-uri")
        restored.begin("https://other.example")
        assertNull(restored.take())
        restored.complete("new-uri")
        assertEquals("https://other.example" to "new-uri", restored.take())
    }
}
