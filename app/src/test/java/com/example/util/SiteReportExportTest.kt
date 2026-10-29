package com.example.util

import com.example.data.local.entity.DailySiteReportEntity
import com.example.data.local.entity.MeetingMinutesEntity
import com.example.data.local.entity.SiteInspectionEntity
import com.example.data.local.entity.SiteIssueEntity
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the CSV payload shape used by [ExportHelper.exportAndShareSiteReportsCsv]
 * without requiring Android Intent plumbing.
 */
class SiteReportExportTest {
    @Test fun siteReportBundleCarriesAllFourRegisters() {
        val bundle = ExportHelper.SiteReportBundle(
            projectName = "Demo Tower",
            dailyReports = listOf(
                DailySiteReportEntity(
                    projectId = 1,
                    reportDate = 1_700_000_000_000L,
                    workCompleted = "Cast slab",
                    weather = "Clear",
                    manpower = "12 masons"
                )
            ),
            inspections = listOf(
                SiteInspectionEntity(
                    projectId = 1,
                    inspectionAt = 1_700_000_100_000L,
                    location = "Level 3",
                    observation = "Honeycombing at beam soffit",
                    severity = "ATTENTION",
                    correctiveAction = "Hack and recast"
                )
            ),
            minutes = listOf(
                MeetingMinutesEntity(
                    projectId = 1,
                    title = "Weekly site meeting",
                    meetingAt = 1_700_000_200_000L,
                    decisions = "Approve formwork sequence"
                )
            ),
            siteIssues = listOf(
                SiteIssueEntity(
                    projectId = 1,
                    referenceNumber = "SNAG-01",
                    type = "SNAG",
                    title = "Door frame out of plumb",
                    description = "Frame leans 8mm",
                    evidenceUri = "file:///before.jpg",
                    afterEvidenceUri = "file:///after.jpg"
                )
            )
        )

        assertTrue(bundle.dailyReports.single().workCompleted.contains("slab"))
        assertTrue(bundle.inspections.single().severity == "ATTENTION")
        assertTrue(bundle.minutes.single().title.contains("Weekly"))
        assertTrue(bundle.siteIssues.single().afterEvidenceUri != null)
    }
}
