package com.example.domain

import com.example.data.local.entity.CoordinationItemEntity
import com.example.data.local.entity.DailySiteReportEntity
import com.example.data.local.entity.DrawingRevisionEntity
import com.example.data.local.entity.ProjectDecisionEntity
import com.example.data.local.entity.ProjectDrawingEntity
import com.example.data.local.entity.ProjectEntity
import com.example.data.local.entity.ProjectScheduleEntity
import com.example.data.local.entity.ProjectTaskEntity
import com.example.data.local.entity.SiteIssueEntity
import java.util.Calendar

/**
 * Company-wide operational snapshot for the monitoring dashboard — counts
 * only. Deliberately holds no commercial or payroll figures, per ArchiMan's
 * product boundary (see docs/ARCHITECT_PRACTICE_SCOPE.md).
 */
data class CompanyDashboardSnapshot(
    val totalProjects: Int = 0,
    val activeProjects: Int = 0,
    val openRfis: Int = 0,
    val openSubmittals: Int = 0,
    val openSiteInstructions: Int = 0,
    val openSiteIssues: Int = 0,
    val criticalOpenSiteIssues: Int = 0,
    val pendingDecisions: Int = 0,
    val overdueDecisions: Int = 0,
    val todaysSiteReports: Int = 0,
    val overdueTasks: Int = 0,
    val drawingsInProgress: Int = 0,
    val totalDrawings: Int = 0,
    val upcomingScheduleItems: Int = 0,
    val revisionRisk: RevisionRiskSummary = computeRevisionRisk(emptyList())
) {
    val openCoordinationTotal: Int get() = openRfis + openSubmittals + openSiteInstructions
}

private fun isSameCalendarDay(a: Long, b: Long): Boolean {
    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

/** Pure aggregation — unit-testable without Room, a clock, or coroutines. */
fun computeCompanyDashboardSnapshot(
    projects: List<ProjectEntity>,
    coordinationItems: List<CoordinationItemEntity>,
    siteIssues: List<SiteIssueEntity>,
    decisions: List<ProjectDecisionEntity>,
    dailyReports: List<DailySiteReportEntity>,
    tasks: List<ProjectTaskEntity>,
    drawings: List<ProjectDrawingEntity>,
    schedules: List<ProjectScheduleEntity>,
    now: Long = System.currentTimeMillis(),
    upcomingWindowMs: Long = 7L * 24 * 60 * 60 * 1000,
    drawingRevisions: List<DrawingRevisionEntity> = emptyList()
): CompanyDashboardSnapshot {
    fun openCoordinationOfType(type: String) =
        coordinationItems.count { it.type == type && !CoordinationWorkflow.isClosed(it.status) }

    val openIssues = siteIssues.filter { it.status != SiteIssueWorkflow.CLOSED }
    val pendingDecisions = decisions.filter { it.status != "DECIDED" }

    return CompanyDashboardSnapshot(
        totalProjects = projects.size,
        activeProjects = projects.count { it.status == "ACTIVE" },
        openRfis = openCoordinationOfType(CoordinationWorkflow.RFI),
        openSubmittals = openCoordinationOfType(CoordinationWorkflow.SUBMITTAL),
        openSiteInstructions = openCoordinationOfType(CoordinationWorkflow.SITE_INSTRUCTION),
        openSiteIssues = openIssues.size,
        criticalOpenSiteIssues = openIssues.count { it.severity == "CRITICAL" || it.severity == "ATTENTION" },
        pendingDecisions = pendingDecisions.size,
        overdueDecisions = pendingDecisions.count { (it.dueAt ?: Long.MAX_VALUE) < now },
        todaysSiteReports = dailyReports.count { isSameCalendarDay(it.reportDate, now) },
        overdueTasks = tasks.count { it.status != "DONE" && (it.dueDate ?: Long.MAX_VALUE) < now },
        drawingsInProgress = drawings.count { it.status == "WORKING" },
        totalDrawings = drawings.size,
        upcomingScheduleItems = schedules.count { it.scheduledAt in now..(now + upcomingWindowMs) },
        revisionRisk = computeRevisionRisk(drawingRevisions.map { it.revisionSource to it.severity })
    )
}
