package com.holagundi.archiman.domain

data class TransitionCheck(val valid: Boolean, val error: String? = null)

object SiteIssueWorkflow {
    const val OPEN = "OPEN"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val READY_FOR_VERIFICATION = "READY_FOR_VERIFICATION"
    const val CLOSED = "CLOSED"

    const val SEVERITY_NORMAL = "NORMAL"
    const val SEVERITY_ATTENTION = "ATTENTION"
    const val SEVERITY_CRITICAL = "CRITICAL"

    val severities = listOf(SEVERITY_NORMAL, SEVERITY_ATTENTION, SEVERITY_CRITICAL)

    fun normalizeSeverity(value: String): String = when (value.uppercase()) {
        SEVERITY_CRITICAL, "HIGH", "MAJOR" -> SEVERITY_CRITICAL
        SEVERITY_ATTENTION, "MEDIUM", "WARNING" -> SEVERITY_ATTENTION
        else -> SEVERITY_NORMAL
    }

    fun statusLabel(status: String): String = when (status) {
        OPEN -> "Open"
        IN_PROGRESS -> "In progress"
        READY_FOR_VERIFICATION -> "Ready for verification"
        CLOSED -> "Closed"
        else -> status
    }

    fun severityLabel(severity: String): String = when (normalizeSeverity(severity)) {
        SEVERITY_CRITICAL -> "Critical"
        SEVERITY_ATTENTION -> "Attention"
        else -> "Normal"
    }

    fun allowedNextStatuses(status: String): List<String> = when (status) {
        OPEN -> listOf(IN_PROGRESS)
        IN_PROGRESS -> listOf(READY_FOR_VERIFICATION, OPEN)
        READY_FOR_VERIFICATION -> listOf(CLOSED, IN_PROGRESS)
        else -> emptyList()
    }

    fun canTransition(fromStatus: String, toStatus: String, verificationNote: String = ""): TransitionCheck {
        if (toStatus !in allowedNextStatuses(fromStatus)) {
            return TransitionCheck(false, "Invalid site issue transition $fromStatus to $toStatus.")
        }
        if (toStatus == CLOSED && verificationNote.isBlank()) {
            return TransitionCheck(false, "A verification note is required before closure.")
        }
        return TransitionCheck(true)
    }
}

object CoordinationWorkflow {
    const val RFI = "RFI"
    const val SUBMITTAL = "SUBMITTAL"
    const val SITE_INSTRUCTION = "SITE_INSTRUCTION"

    fun initialStatus(type: String): String = when (type) {
        SUBMITTAL -> "RECEIVED"
        else -> "DRAFT"
    }

    fun allowedNextStatuses(type: String, status: String): List<String> = when (type) {
        RFI -> when (status) {
            "DRAFT" -> listOf("OPEN")
            "OPEN" -> listOf("ANSWERED")
            "ANSWERED" -> listOf("CLOSED", "OPEN")
            else -> emptyList()
        }
        SUBMITTAL -> when (status) {
            "RECEIVED" -> listOf("UNDER_REVIEW")
            "UNDER_REVIEW" -> listOf("APPROVED", "APPROVED_AS_NOTED", "REVISE_RESUBMIT")
            "REVISE_RESUBMIT" -> listOf("RECEIVED")
            else -> emptyList()
        }
        SITE_INSTRUCTION -> when (status) {
            "DRAFT" -> listOf("ISSUED")
            "ISSUED" -> listOf("ACKNOWLEDGED")
            "ACKNOWLEDGED" -> listOf("COMPLIED")
            "COMPLIED" -> listOf("CLOSED")
            else -> emptyList()
        }
        else -> emptyList()
    }

    fun transitionError(type: String, fromStatus: String, toStatus: String): String? {
        if (toStatus !in allowedNextStatuses(type, fromStatus)) {
            return "Invalid coordination transition $fromStatus to $toStatus for $type."
        }
        return null
    }

    fun isClosed(status: String): Boolean = status in setOf("CLOSED", "APPROVED", "APPROVED_AS_NOTED")
}
