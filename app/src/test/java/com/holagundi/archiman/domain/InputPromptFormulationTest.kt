package com.holagundi.archiman.domain

import com.holagundi.archiman.data.RecordKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** JVM coverage for prompt labels and domain validators used by every create/edit form. */
class InputPromptFormulationTest {
    @Test fun everyRecordEditorFieldHasAClearLabelAndUniqueColumn() {
        RecordKind.entries.forEach { kind ->
            val columns = kind.fields.map { it.column }
            val labels = kind.fields.map { it.label }
            assertEquals(kind.name, columns.distinct().size, columns.size)
            assertTrue(kind.name, labels.none { it.isBlank() })
            assertEquals(kind.name, labels.distinct().size, labels.size)
            kind.fields.filter { it.required }.forEach { field ->
                assertTrue("${kind.name}.${field.column}", field.label.isNotBlank())
            }
        }
    }

    @Test fun financeAmountPromptRejectsJunkAndAcceptsPlainDecimals() {
        assertTrue(runCatching { FinanceMath.decimal("twelve") }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { FinanceMath.decimal("12.34.56") }.exceptionOrNull() is IllegalArgumentException)
        assertEquals("12.35", FinanceMath.decimal("12.345").toPlainString())
    }

    @Test fun consultancyPromptsRequireTitlePositiveFeeAndResolvedPlaceholders() {
        val good = ConsultancyInput(
            title = "Fee",
            service = ConsultancyService.ARCHITECTURE,
            feeBasis = ConsultancyFeeBasis.LUMP_SUM,
            rate = "10000",
            parties = "Client and architect",
            scope = "Full services",
            exclusions = "None",
            terms = "Standard",
            milestones = listOf(ConsultancyMilestone("Complete", "100")),
        )
        ConsultancyTemplates.validate(good)
        ConsultancyTemplates.validate(good, forIssue = true)
        assertTrue(runCatching { ConsultancyTemplates.validate(good.copy(title = " ")) }.isFailure)
        assertTrue(runCatching { ConsultancyTemplates.validate(good.copy(rate = "0")) }.isFailure)
        assertTrue(runCatching { ConsultancyTemplates.validate(good.copy(scope = "Do [TODO]"), forIssue = true) }.isFailure)
    }

    @Test fun workItemAndPasswordPromptsAreFormulated() {
        WorkItemLibrary.validate("VOLUME", "m³")
        assertTrue(runCatching { WorkItemLibrary.validate("NOPE", "m³") }.isFailure)
        val short = "short".toCharArray()
        assertTrue(runCatching { PasswordEnvelope.encrypt("x".toByteArray(), short, 1000) }.isFailure)
        short.fill('\u0000')
        val password = "twelvechars!".toCharArray()
        try {
            val sealed = PasswordEnvelope.encrypt("payload".toByteArray(), password, 1_000_000)
            assertEquals("payload", PasswordEnvelope.decrypt(sealed, password, 1_000_000).toString(Charsets.UTF_8))
        } finally {
            password.fill('\u0000')
        }
    }

    @Test fun snagClosePromptRequiresVerificationNote() {
        assertTrue(!SiteIssueWorkflow.canTransition(SiteIssueWorkflow.OPEN, SiteIssueWorkflow.CLOSED, "Verified").valid)
        val blank = SiteIssueWorkflow.canTransition(SiteIssueWorkflow.READY_FOR_VERIFICATION, SiteIssueWorkflow.CLOSED, " ")
        assertTrue(!blank.valid)
        assertTrue(blank.error.orEmpty().contains("verification", ignoreCase = true))
        assertTrue(SiteIssueWorkflow.canTransition(SiteIssueWorkflow.READY_FOR_VERIFICATION, SiteIssueWorkflow.CLOSED, "Verified on site").valid)
    }
}
