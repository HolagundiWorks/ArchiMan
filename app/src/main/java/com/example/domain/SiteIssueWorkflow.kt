package com.example.domain

object SiteIssueWorkflow {
    const val OPEN = "OPEN"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val READY_FOR_VERIFICATION = "READY_FOR_VERIFICATION"
    const val CLOSED = "CLOSED"

    fun allowedNextStatuses(status: String): List<String> = when (status) {
        OPEN -> listOf(IN_PROGRESS)
        IN_PROGRESS -> listOf(READY_FOR_VERIFICATION, OPEN)
        READY_FOR_VERIFICATION -> listOf(CLOSED, IN_PROGRESS)
        else -> emptyList()
    }

    fun requireTransition(fromStatus: String, toStatus: String, verificationNote: String) {
        require(toStatus in allowedNextStatuses(fromStatus)) {
            "Invalid site issue transition $fromStatus to $toStatus."
        }
        require(toStatus != CLOSED || verificationNote.isNotBlank()) {
            "A verification note is required before closure."
        }
    }
}
