package com.holagundi.archiman.data.local

data class DashboardMetrics(val openTasks: Int, val pendingReviews: Int, val openIssues: Int)
data class DashboardTask(val id: Long, val projectId: Long, val title: String, val projectName: String, val dueDate: Long?)
data class DashboardReview(val id: Long, val projectId: Long, val sheetCode: String, val projectName: String, val status: String)
data class DashboardIssue(val id: Long, val projectId: Long, val title: String, val projectName: String, val severity: String)
/** SQL aggregate for fee tiles — avoids loading full invoice/receipt tables on dashboards. */
data class FeeDashboardTotals(
    val outstanding: Double,
    val received: Double,
    val unpaidInvoices: Int,
    val tdsWithheld: Double,
)
data class ProjectOverviewCounts(val openTasks: Int, val openIssues: Int)
data class ProjectModuleCountRow(
    val dailyLogs: Int,
    val materialOrders: Int,
    val drawings: Int,
    val siteIssues: Int,
    val inspections: Int,
    val minutes: Int,
    val coordination: Int,
    val decisions: Int,
    val tasks: Int,
)
