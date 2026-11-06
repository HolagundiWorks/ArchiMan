package com.holagundi.archiman.data.local

data class DashboardMetrics(val openTasks: Int, val pendingReviews: Int, val openIssues: Int)
data class DashboardTask(val id: Long, val projectId: Long, val title: String, val projectName: String, val dueDate: Long?)
data class DashboardReview(val id: Long, val projectId: Long, val sheetCode: String, val projectName: String, val status: String)
data class DashboardIssue(val id: Long, val projectId: Long, val title: String, val projectName: String, val severity: String)
