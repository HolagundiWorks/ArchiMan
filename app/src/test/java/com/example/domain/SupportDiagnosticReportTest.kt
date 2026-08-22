package com.example.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportDiagnosticReportTest {
    @Test
    fun `report contains operational counts in stable order`() {
        val report = SupportDiagnosticReport.render(
            SupportDiagnosticSnapshot(
                appVersion = "1.0\"debug",
                versionCode = 1,
                schemaVersion = 10,
                androidSdk = 36,
                generatedAtEpochMs = 1234,
                projectCount = 2,
                contractorCount = 3,
                workItemCount = 4,
                measurementCount = 6,
                sheetCount = 5,
                archivedSheetCount = 1,
                rowsWithoutKnownSheet = 0,
                rowsWithoutKnownWorkItem = 0,
                sheetStatusCounts = mapOf("SUBMITTED" to 1, "DRAFT" to 4)
            )
        )

        assertTrue(report.contains("\"databaseSchema\": 10"))
        assertTrue(report.contains("\"measurements\": 6"))
        assertTrue(report.indexOf("\"DRAFT\"") < report.indexOf("\"SUBMITTED\""))
        assertTrue(report.contains("1.0\\\"debug"))
    }

    @Test
    fun `report contract excludes measurement content fields`() {
        val report = SupportDiagnosticReport.render(
            SupportDiagnosticSnapshot(
                appVersion = "1", versionCode = 1, schemaVersion = 10, androidSdk = 30,
                generatedAtEpochMs = 0, projectCount = 0, contractorCount = 0,
                workItemCount = 0, measurementCount = 0, sheetCount = 0,
                archivedSheetCount = 0, rowsWithoutKnownSheet = 0,
                rowsWithoutKnownWorkItem = 0, sheetStatusCounts = emptyMap()
            )
        )

        listOf("description", "dimension", "quantity", "photo", "comment", "path", "contractorName")
            .forEach { forbidden -> assertFalse(report.contains(forbidden, ignoreCase = true)) }
    }
}
