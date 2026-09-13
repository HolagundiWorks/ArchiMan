package com.example.domain

import com.example.data.local.entity.CoordinationItemEntity
import com.example.data.local.entity.DailySiteReportEntity
import com.example.data.local.entity.ProjectDecisionEntity
import com.example.data.local.entity.ProjectDrawingEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectScheduleEntity
import com.example.data.local.entity.ProjectTaskEntity
import com.example.data.local.entity.SiteIssueEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class CompanyDashboardTest {
    private val now = 1_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    @Test fun countsActiveProjectsSeparatelyFromTotal() {
        val projects = listOf(
            ProjectEntity(id = 1, name = "A", status = "ACTIVE"),
            ProjectEntity(id = 2, name = "B", status = "COMPLETED")
        )
        val snapshot = computeCompanyDashboardSnapshot(
            projects, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), now
        )
        assertEquals(2, snapshot.totalProjects)
        assertEquals(1, snapshot.activeProjects)
    }

    @Test fun splitsOpenCoordinationByTypeAndExcludesClosed() {
        val items = listOf(
            CoordinationItemEntity(id = 1, projectId = 1, type = "RFI", referenceNumber = "R1", subject = "s", status = "OPEN"),
            CoordinationItemEntity(id = 2, projectId = 1, type = "RFI", referenceNumber = "R2", subject = "s", status = "CLOSED"),
            CoordinationItemEntity(id = 3, projectId = 1, type = "SUBMITTAL", referenceNumber = "R3", subject = "s", status = "UNDER_REVIEW"),
            CoordinationItemEntity(id = 4, projectId = 1, type = "SITE_INSTRUCTION", referenceNumber = "R4", subject = "s", status = "ISSUED")
        )
        val snapshot = computeCompanyDashboardSnapshot(
            emptyList(), items, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), now
        )
        assertEquals(1, snapshot.openRfis)
        assertEquals(1, snapshot.openSubmittals)
        assertEquals(1, snapshot.openSiteInstructions)
        assertEquals(3, snapshot.openCoordinationTotal)
    }

    @Test fun countsCriticalAndAttentionSiteIssuesAsCriticalOpen() {
        val issues = listOf(
            SiteIssueEntity(id = 1, projectId = 1, referenceNumber = "S1", type = "SNAG", title = "t", description = "d", severity = "CRITICAL", status = "OPEN"),
            SiteIssueEntity(id = 2, projectId = 1, referenceNumber = "S2", type = "SNAG", title = "t", description = "d", severity = "ATTENTION", status = "IN_PROGRESS"),
            SiteIssueEntity(id = 3, projectId = 1, referenceNumber = "S3", type = "SNAG", title = "t", description = "d", severity = "NORMAL", status = "OPEN"),
            SiteIssueEntity(id = 4, projectId = 1, referenceNumber = "S4", type = "SNAG", title = "t", description = "d", severity = "CRITICAL", status = "CLOSED")
        )
        val snapshot = computeCompanyDashboardSnapshot(
            emptyList(), emptyList(), issues, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), now
        )
        assertEquals(3, snapshot.openSiteIssues)
        assertEquals(2, snapshot.criticalOpenSiteIssues)
    }

    @Test fun flagsOverdueDecisionsOnlyAmongPendingOnes() {
        val decisions = listOf(
            ProjectDecisionEntity(id = 1, projectId = 1, referenceNumber = "D1", title = "t", decisionRequired = "r", status = "OPEN", dueAt = now - day),
            ProjectDecisionEntity(id = 2, projectId = 1, referenceNumber = "D2", title = "t", decisionRequired = "r", status = "OPEN", dueAt = now + day),
            ProjectDecisionEntity(id = 3, projectId = 1, referenceNumber = "D3", title = "t", decisionRequired = "r", status = "DECIDED", dueAt = now - day)
        )
        val snapshot = computeCompanyDashboardSnapshot(
            emptyList(), emptyList(), emptyList(), decisions, emptyList(), emptyList(), emptyList(), emptyList(), now
        )
        assertEquals(2, snapshot.pendingDecisions)
        assertEquals(1, snapshot.overdueDecisions)
    }

    @Test fun countsTodaysSiteReportsBySameCalendarDay() {
        val reports = listOf(
            DailySiteReportEntity(id = 1, projectId = 1, reportDate = now, workCompleted = "w"),
            DailySiteReportEntity(id = 2, projectId = 1, reportDate = now - 3 * day, workCompleted = "w")
        )
        val snapshot = computeCompanyDashboardSnapshot(
            emptyList(), emptyList(), emptyList(), emptyList(), reports, emptyList(), emptyList(), emptyList(), now
        )
        assertEquals(1, snapshot.todaysSiteReports)
    }

    @Test fun countsOverdueTasksExcludingDone() {
        val tasks = listOf(
            ProjectTaskEntity(id = 1, projectId = 1, title = "t", status = "OPEN", dueDate = now - day),
            ProjectTaskEntity(id = 2, projectId = 1, title = "t", status = "DONE", dueDate = now - day),
            ProjectTaskEntity(id = 3, projectId = 1, title = "t", status = "OPEN", dueDate = now + day)
        )
        val snapshot = computeCompanyDashboardSnapshot(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), tasks, emptyList(), emptyList(), now
        )
        assertEquals(1, snapshot.overdueTasks)
    }

    @Test fun countsDrawingsInProgressAndTotal() {
        val drawings = listOf(
            ProjectDrawingEntity(id = 1, projectId = 1, drawingNumber = "A-1", title = "t", status = "WORKING"),
            ProjectDrawingEntity(id = 2, projectId = 1, drawingNumber = "A-2", title = "t", status = "ISSUED_FOR_CONSTRUCTION")
        )
        val snapshot = computeCompanyDashboardSnapshot(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), drawings, emptyList(), now
        )
        assertEquals(1, snapshot.drawingsInProgress)
        assertEquals(2, snapshot.totalDrawings)
    }

    @Test fun countsScheduleItemsWithinTheUpcomingWindowOnly() {
        val schedules = listOf(
            ProjectScheduleEntity(id = 1, projectId = 1, title = "t", scheduledAt = now + day),
            ProjectScheduleEntity(id = 2, projectId = 1, title = "t", scheduledAt = now + 30 * day),
            ProjectScheduleEntity(id = 3, projectId = 1, title = "t", scheduledAt = now - day)
        )
        val snapshot = computeCompanyDashboardSnapshot(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), schedules, now
        )
        assertEquals(1, snapshot.upcomingScheduleItems)
    }
}
