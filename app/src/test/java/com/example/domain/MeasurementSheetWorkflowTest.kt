package com.example.domain

import org.junit.Test

class MeasurementSheetWorkflowTest {
    @Test fun validLifecycleAndReturnPath() {
        MeasurementSheetWorkflow.requireTransition(MeasurementSheetStatus.DRAFT, MeasurementSheetStatus.SUBMITTED, "")
        MeasurementSheetWorkflow.requireTransition(MeasurementSheetStatus.SUBMITTED, MeasurementSheetStatus.CHECKED, "")
        MeasurementSheetWorkflow.requireTransition(MeasurementSheetStatus.CHECKED, MeasurementSheetStatus.RETURNED, "Correct dimensions")
        MeasurementSheetWorkflow.requireTransition(MeasurementSheetStatus.RETURNED, MeasurementSheetStatus.SUBMITTED, "")
        MeasurementSheetWorkflow.requireTransition(MeasurementSheetStatus.CHECKED, MeasurementSheetStatus.APPROVED, "")
    }

    @Test(expected = IllegalArgumentException::class)
    fun cannotApproveDraftDirectly() = MeasurementSheetWorkflow.requireTransition(MeasurementSheetStatus.DRAFT, MeasurementSheetStatus.APPROVED, "")

    @Test(expected = IllegalArgumentException::class)
    fun returnRequiresComment() = MeasurementSheetWorkflow.requireTransition(MeasurementSheetStatus.SUBMITTED, MeasurementSheetStatus.RETURNED, "")
}
