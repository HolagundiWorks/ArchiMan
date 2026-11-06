package com.holagundi.archiman.domain

import com.holagundi.archiman.data.ConsultancyCodec
import org.junit.Assert.*
import org.junit.Test

class ConsultancyTemplatesTest {
    private fun draft(service: ConsultancyService = ConsultancyService.ARCHITECTURE) = ConsultancyTemplates.create(service, "Client and architect details").copy(rate = "100000")

    @Test fun supportsAllThreeFeeBasesWithDecimalArithmetic() {
        assertEquals("100000.00", ConsultancyTemplates.amount(draft()).toPlainString())
        assertEquals("87500.00", ConsultancyTemplates.amount(draft().copy(feeBasis = ConsultancyFeeBasis.PER_SQ_FT, rate = "35", quantity = "2500", areaDefinition = "Agreed measured built-up area")).toPlainString())
        assertEquals("75000.00", ConsultancyTemplates.amount(draft().copy(feeBasis = ConsultancyFeeBasis.PERCENT_OF_COST, rate = "7.5", quantity = "1000000")).toPlainString())
    }

    @Test fun areaBasisNeedsAnExplicitMeasurementDefinition() {
        assertThrows(IllegalArgumentException::class.java) { ConsultancyTemplates.validate(draft().copy(feeBasis = ConsultancyFeeBasis.PER_SQ_FT, quantity = "100", rate = "25")) }
        listOf("-1", "0", "NaN", "Infinity", "10000000000000000000000").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { ConsultancyTemplates.amount(draft().copy(rate = value)) }
        }
    }

    @Test fun miscellaneousTermsAreEntirelyManual() {
        val input = draft(ConsultancyService.MISCELLANEOUS)
        assertTrue(input.scope.isBlank()); assertTrue(input.terms.isBlank()); assertTrue(input.sourceUrl.isBlank())
        assertThrows(IllegalArgumentException::class.java) { ConsultancyTemplates.validate(input, true) }
        ConsultancyTemplates.validate(input.copy(scope = "Measured drawing of existing space", terms = "Client-provided terms agreed in writing"), true)
    }

    @Test fun standardTemplatesRetainDifferentScopesAndSourceReferences() {
        val architecture = draft()
        val interior = draft(ConsultancyService.INTERIORS)
        val landscape = draft(ConsultancyService.LANDSCAPE)
        assertTrue(interior.scope.contains("furniture")); assertTrue(landscape.scope.contains("planting"))
        assertNotEquals(architecture.sourceUrl, interior.sourceUrl)
        assertEquals(listOf("10", "20", "30", "35"), architecture.milestones.take(4).map { it.cumulativePercent })
        assertEquals(listOf("10", "20", "35"), interior.milestones.take(3).map { it.cumulativePercent })
    }

    @Test fun unresolvedDraftsCannotBeIssued() {
        assertThrows(IllegalArgumentException::class.java) { ConsultancyTemplates.validate(draft(), true) }
        val complete = draft().let { it.copy(scope = it.scope.replace(Regex("\\[[^]]*]"), "Responsibilities agreed"), terms = it.terms.replace(Regex("\\[[^]]*]"), "Agreed project particulars")) }
        ConsultancyTemplates.validate(complete, true)
    }

    @Test fun cumulativeScheduleCannotBeMistakenForAdditiveOrIncompleteStages() {
        val input = draft().copy(milestones = listOf(ConsultancyMilestone("Start", "60"), ConsultancyMilestone("Finish", "50")))
        assertThrows(IllegalArgumentException::class.java) { ConsultancyTemplates.validate(input) }
        assertThrows(IllegalArgumentException::class.java) { ConsultancyTemplates.validate(draft().copy(milestones = listOf(ConsultancyMilestone("Finish", "90")))) }
        val text = ConsultancyTemplates.render(draft(), true)
        assertTrue(text.contains("100% to date = 100000.00"))
        assertTrue(text.contains("do not add cumulative targets together"))
        assertTrue(text.contains("Client name/signature")); assertTrue(text.contains(ConsultancyTemplates.VERSION))
    }

    @Test fun documentInputRoundTripsWithoutLosingManualTermsOrArea() {
        val input = draft(ConsultancyService.INTERIORS).copy(feeBasis = ConsultancyFeeBasis.PER_SQ_FT, quantity = "1587.25", rate = "45.50", areaDefinition = "Carpet area; excludes shafts", terms = "Custom terms\nSecond clause")
        assertEquals(input, ConsultancyCodec.decode(ConsultancyCodec.encode(input)))
    }
}
