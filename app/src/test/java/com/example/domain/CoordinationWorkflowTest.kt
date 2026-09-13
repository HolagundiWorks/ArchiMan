package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoordinationWorkflowTest {
    @Test fun rfiRequiresAnswerBeforeClosure() {
        assertEquals(listOf("OPEN"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.RFI, "DRAFT"))
        assertEquals(listOf("ANSWERED"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.RFI, "OPEN"))
        assertTrue("CLOSED" in CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.RFI, "ANSWERED"))
    }

    @Test fun submittalSupportsReviewOutcomesAndResubmission() {
        assertEquals(listOf("UNDER_REVIEW"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.SUBMITTAL, "RECEIVED"))
        assertEquals(setOf("APPROVED", "APPROVED_AS_NOTED", "REVISE_RESUBMIT"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.SUBMITTAL, "UNDER_REVIEW").toSet())
        assertEquals(listOf("RECEIVED"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.SUBMITTAL, "REVISE_RESUBMIT"))
    }

    @Test fun siteInstructionRequiresAcknowledgementAndCompliance() {
        assertEquals(listOf("ISSUED"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.SITE_INSTRUCTION, "DRAFT"))
        assertEquals(listOf("ACKNOWLEDGED"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.SITE_INSTRUCTION, "ISSUED"))
        assertEquals(listOf("COMPLIED"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.SITE_INSTRUCTION, "ACKNOWLEDGED"))
        assertEquals(listOf("CLOSED"), CoordinationWorkflow.allowedNextStatuses(CoordinationWorkflow.SITE_INSTRUCTION, "COMPLIED"))
    }
}
