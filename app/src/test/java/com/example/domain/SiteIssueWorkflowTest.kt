package com.example.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SiteIssueWorkflowTest {
    @Test fun issueRequiresCorrectionThenIndependentVerification() {
        assertEquals(listOf("IN_PROGRESS"), SiteIssueWorkflow.allowedNextStatuses("OPEN"))
        assertEquals(
            setOf("READY_FOR_VERIFICATION", "OPEN"),
            SiteIssueWorkflow.allowedNextStatuses("IN_PROGRESS").toSet()
        )
        assertEquals(
            setOf("CLOSED", "IN_PROGRESS"),
            SiteIssueWorkflow.allowedNextStatuses("READY_FOR_VERIFICATION").toSet()
        )
    }

    @Test fun closedIssueIsTerminal() {
        assertEquals(emptyList<String>(), SiteIssueWorkflow.allowedNextStatuses("CLOSED"))
        assertThrows(IllegalArgumentException::class.java) {
            SiteIssueWorkflow.requireTransition("CLOSED", "OPEN", "")
        }
    }

    @Test fun closureRequiresVerificationNote() {
        assertThrows(IllegalArgumentException::class.java) {
            SiteIssueWorkflow.requireTransition("READY_FOR_VERIFICATION", "CLOSED", "")
        }
        SiteIssueWorkflow.requireTransition("READY_FOR_VERIFICATION", "CLOSED", "Checked on site")
    }
}
