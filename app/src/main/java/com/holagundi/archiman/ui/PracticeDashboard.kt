package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.ArchiCard
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PracticeDashboard(
    viewModel: PortfolioViewModel,
    onNewProject: () -> Unit,
    onProjects: () -> Unit,
    onInvoices: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenPage: (Long, WorkspacePage) -> Unit,
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit = { id, page, _ -> onOpenPage(id, page) },
    onAttention: (String) -> Unit = {},
) {
    val company by viewModel.company.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val tasks by viewModel.dashboardTasks.collectAsStateWithLifecycle()
    val reviews by viewModel.dashboardReviews.collectAsStateWithLifecycle()
    val issues by viewModel.dashboardIssues.collectAsStateWithLifecycle()
    val fees by viewModel.dashboardFees.collectAsStateWithLifecycle()
    androidx.activity.compose.ReportDrawnWhen { company != null && metrics != null && fees != null }
    val current = projects.filter { it.status != "ARCHIVED" }
    val number = NumberFormat.getNumberInstance().apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (tasks.isNotEmpty() || reviews.isNotEmpty() || issues.isNotEmpty()) {
            item { Text("Needs attention", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(reviews, key = { "review-${it.id}" }) { review ->
                FeatureRow(review.sheetCode, "${review.projectName} · ${if (review.status == "SUBMITTED") "Ready to check" else "Awaiting approval"}", CarbonIcons.FactCheck) { onOpenRecord(review.projectId, WorkspacePage.BOOK, review.id) }
            }
            items(tasks, key = { "task-${it.id}" }) { task ->
                FeatureRow(task.title, "${task.projectName} · ${taskDueLabel(task.dueDate)}", CarbonIcons.Assignment) { onOpenRecord(task.projectId, WorkspacePage.TASKS, task.id) }
            }
            items(issues, key = { "issue-${it.id}" }) { issue ->
                FeatureRow(issue.title, "${issue.projectName} · ${issue.severity.lowercase().replaceFirstChar { it.uppercase() }}", CarbonIcons.ReportProblem) { onOpenRecord(issue.projectId, WorkspacePage.SNAGS, issue.id) }
            }
        } else if (metrics != null && metrics!!.openTasks == 0 && metrics!!.pendingReviews == 0 && metrics!!.openIssues == 0) {
            item { FeatureRow("You're up to date", "No open tasks, reviews or site issues in current projects.", CarbonIcons.CheckCircle, onProjects) }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, contentColor = MaterialTheme.colorScheme.onSurface, shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("YOUR PRACTICE, AT A GLANCE", style = MaterialTheme.typography.labelMedium)
                    Text(company?.practiceName ?: "Your practice", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Projects, site work and fees in one place.", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onNewProject) {
                        Icon(CarbonIcons.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("New project")
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStat("Current projects", current.size.toString(), CarbonIcons.Apartment, Modifier.weight(1f), onProjects)
                    DashboardStat("Open tasks", metrics?.openTasks?.toString() ?: "—", CarbonIcons.Assignment, Modifier.weight(1f)) { onAttention("tasks") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DashboardStat("Sheets to review", metrics?.pendingReviews?.toString() ?: "—", CarbonIcons.FactCheck, Modifier.weight(1f)) { onAttention("reviews") }
                    DashboardStat("Open site issues", metrics?.openIssues?.toString() ?: "—", CarbonIcons.ReportProblem, Modifier.weight(1f)) { onAttention("issues") }
                }
            }
        }
        item { com.holagundi.archiman.ui.theme.CarbonFeeTile(fees,onInvoices) }
        item {
            val mix=projects.groupingBy{it.status}.eachCount().toSortedMap().map{(status,count)->com.holagundi.archiman.domain.CarbonChartDatum(status.lowercase().replace('_',' ').replaceFirstChar{it.uppercase()},count.toBigDecimal())}
            com.holagundi.archiman.ui.theme.CarbonDistributionTile("Project mix","Number of projects by status, including archived projects.",mix,onOpen=onProjects)
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Recent projects", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onProjects) { Text("View all") }
            }
        }
        if (current.isEmpty()) item { FeatureRow("Start a project", "Create your first project to organize your work.", CarbonIcons.Add, onNewProject) }
        items(current.take(3), key = { "project-${it.id}" }) { project -> ProjectCard(project, onOpenProject) }
    }
}

@Composable
private fun DashboardStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    ArchiCard(modifier, onClick = onClick) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = MaterialTheme.shapes.small) {
                Icon(icon, null, Modifier.padding(8.dp).size(20.dp))
            }
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun taskDueLabel(timestamp: Long?, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    if (timestamp == null) return "No due date"
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    return when {
        date < today -> "Overdue · ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
        date == today -> "Due today"
        else -> "Due ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
    }
}
