package com.holagundi.archiman.domain

object SheetStatus {
    const val DRAFT = "DRAFT"
    const val SUBMITTED = "SUBMITTED"
    const val CHECKED = "CHECKED"
    const val APPROVED = "APPROVED"
    const val RETURNED = "RETURNED"
    val ALL = listOf(DRAFT, SUBMITTED, CHECKED, APPROVED, RETURNED)
}

object MeasurementSheetWorkflow {
    fun allowedTransitions(from: String): List<String> = when (from) {
        SheetStatus.DRAFT, SheetStatus.RETURNED -> listOf(SheetStatus.SUBMITTED)
        SheetStatus.SUBMITTED -> listOf(SheetStatus.CHECKED, SheetStatus.RETURNED)
        SheetStatus.CHECKED -> listOf(SheetStatus.APPROVED, SheetStatus.RETURNED)
        else -> emptyList()
    }

    fun canTransition(from: String, to: String, comment: String = ""): Boolean {
        if (to !in allowedTransitions(from)) return false
        if (to == SheetStatus.RETURNED && comment.isBlank()) return false
        return true
    }
}
