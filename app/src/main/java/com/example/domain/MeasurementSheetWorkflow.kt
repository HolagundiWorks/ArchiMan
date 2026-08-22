package com.example.domain

enum class MeasurementSheetStatus { DRAFT, SUBMITTED, CHECKED, APPROVED, RETURNED }

object MeasurementSheetWorkflow {
    fun requireTransition(from: MeasurementSheetStatus, to: MeasurementSheetStatus, comment: String) {
        val allowed = when (from) {
            MeasurementSheetStatus.DRAFT -> setOf(MeasurementSheetStatus.SUBMITTED)
            MeasurementSheetStatus.RETURNED -> setOf(MeasurementSheetStatus.SUBMITTED)
            MeasurementSheetStatus.SUBMITTED -> setOf(MeasurementSheetStatus.CHECKED, MeasurementSheetStatus.RETURNED)
            MeasurementSheetStatus.CHECKED -> setOf(MeasurementSheetStatus.APPROVED, MeasurementSheetStatus.RETURNED)
            MeasurementSheetStatus.APPROVED -> emptySet()
        }
        require(to in allowed) { "Invalid sheet transition: $from to $to" }
        if (to == MeasurementSheetStatus.RETURNED) require(comment.isNotBlank()) { "A return comment is required" }
    }
}
