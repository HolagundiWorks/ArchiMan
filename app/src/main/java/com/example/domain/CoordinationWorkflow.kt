package com.example.domain

object CoordinationWorkflow {
    const val RFI = "RFI"
    const val SUBMITTAL = "SUBMITTAL"
    const val SITE_INSTRUCTION = "SITE_INSTRUCTION"

    fun initialStatus(type: String): String = when (type) {
        RFI -> "DRAFT"
        SUBMITTAL -> "RECEIVED"
        SITE_INSTRUCTION -> "DRAFT"
        else -> error("Unknown coordination type: $type")
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

    fun isClosed(status: String): Boolean = status in setOf("CLOSED", "APPROVED", "APPROVED_AS_NOTED")
}
