package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import com.holagundi.archiman.ui.theme.CarbonIcons
import com.holagundi.archiman.ui.theme.CarbonScreenHelp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.data.FeeDashboardSummary
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.CarbonAlertTone
import com.holagundi.archiman.ui.theme.CarbonSmallTileContent
import com.holagundi.archiman.ui.theme.CarbonSpacing
import com.holagundi.archiman.ui.theme.CarbonTab
import com.holagundi.archiman.ui.theme.CarbonTabStyle
import com.holagundi.archiman.ui.theme.CarbonTabs
import com.holagundi.archiman.ui.theme.CarbonTile
import com.holagundi.archiman.ui.theme.CarbonTileGrid
import com.holagundi.archiman.ui.theme.CarbonTileSize
import com.holagundi.archiman.ui.theme.DisclosureCard
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    val attentionCount = reviews.size + tasks.size + issues.size
    var overviewOpen by rememberSaveable { mutableStateOf(true) }
    var overviewTab by rememberSaveable { mutableStateOf(0) }
    var feesOpen by rememberSaveable { mutableStateOf(true) }
    var projectsOpen by rememberSaveable { mutableStateOf(true) }
    val activeCount = projects.count { it.status.equals("ACTIVE", ignoreCase = true) }
    val onHoldCount = projects.count { it.status.equals("ON_HOLD", ignoreCase = true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = CarbonSpacing.screenPaddingBottomHeavy,
        verticalArrangement = CarbonSpacing.stackArrangement,
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
            ) {
                Text(
                    company?.practiceName ?: "Your practice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                CarbonScreenHelp(
                    "Dashboard",
                    listOf("Each section expands. Tap a header to collapse or disclose details."),
                )
            }
        }

        item {
            val attentionSummary = if (attentionCount > 0) {
                listOfNotNull(
                    reviews.size.takeIf { it > 0 }?.let { "$it review${if (it == 1) "" else "s"}" },
                    tasks.size.takeIf { it > 0 }?.let { "$it task${if (it == 1) "" else "s"}" },
                    issues.size.takeIf { it > 0 }?.let { "$it issue${if (it == 1) "" else "s"}" },
                ).joinToString(" · ")
            } else {
                "Up to date"
            }
            DisclosureCard(
                title = "Overview",
                summary = "${current.size} projects · $attentionSummary",
                expanded = overviewOpen,
                onExpandedChange = { overviewOpen = it },
                leadingIcon = if (attentionCount > 0) CarbonIcons.ReportProblem else CarbonIcons.Dashboard,
                size = CarbonTileSize.LARGE,
                alert = when {
                    (metrics?.openIssues ?: 0) > 0 || issues.isNotEmpty() -> CarbonAlertTone.CRITICAL
                    attentionCount > 0 -> CarbonAlertTone.WARNING
                    else -> CarbonAlertTone.OK
                },
            ) {
                CarbonTabs(
                    tabs = listOf(
                        CarbonTab("At a glance"),
                        CarbonTab("Needs attention", attentionCount.takeIf { it > 0 }),
                    ),
                    selectedIndex = overviewTab,
                    onSelected = { overviewTab = it },
                    style = CarbonTabStyle.Line,
                )
                Spacer(Modifier.height(CarbonSpacing.stack))
                when (overviewTab) {
                    0 -> {
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
                        ) {
                            CarbonTileGrid(CarbonTileSize.SMALL) {
                                item {
                                    CompactStat(
                                        label = "Projects",
                                        value = current.size.toString(),
                                        icon = CarbonIcons.Apartment,
                                        alert = if (current.isEmpty()) CarbonAlertTone.WARNING else CarbonAlertTone.OK,
                                        onClick = onProjects,
                                    )
                                }
                                item {
                                    CompactStat(
                                        label = "Tasks",
                                        value = metrics?.openTasks?.toString() ?: "—",
                                        icon = CarbonIcons.Assignment,
                                        alert = kpiAlert(metrics?.openTasks),
                                        onClick = { onAttention("tasks") },
                                    )
                                }
                                item {
                                    CompactStat(
                                        label = "Reviews",
                                        value = metrics?.pendingReviews?.toString() ?: "—",
                                        icon = CarbonIcons.FactCheck,
                                        alert = kpiAlert(metrics?.pendingReviews),
                                        onClick = { onAttention("reviews") },
                                    )
                                }
                                item {
                                    CompactStat(
                                        label = "Issues",
                                        value = metrics?.openIssues?.toString() ?: "—",
                                        icon = CarbonIcons.ReportProblem,
                                        alert = if ((metrics?.openIssues ?: 0) > 0) CarbonAlertTone.CRITICAL else CarbonAlertTone.OK,
                                        onClick = { onAttention("issues") },
                                    )
                                }
                                item {
                                    CompactStat(
                                        label = "Active",
                                        value = activeCount.toString(),
                                        icon = CarbonIcons.Apartment,
                                        alert = if (activeCount == 0) CarbonAlertTone.WARNING else CarbonAlertTone.OK,
                                        onClick = onProjects,
                                    )
                                }
                            }
                            CarbonTileGrid(CarbonTileSize.MEDIUM) {
                                item {
                                    CarbonTile(
                                        size = CarbonTileSize.MEDIUM,
                                        alert = CarbonAlertTone.OK,
                                        onClick = onProjects,
                                        contentPadding = false,
                                    ) {
                                        CarbonSmallTileContent("Active projects", activeCount.toString(), CarbonIcons.CheckCircle)
                                    }
                                }
                                item {
                                    CarbonTile(
                                        size = CarbonTileSize.MEDIUM,
                                        alert = if (onHoldCount > 0) CarbonAlertTone.WARNING else CarbonAlertTone.OK,
                                        onClick = onProjects,
                                        contentPadding = false,
                                    ) {
                                        CarbonSmallTileContent("On hold", onHoldCount.toString(), CarbonIcons.Warning)
                                    }
                                }
                                item {
                                    CarbonTile(
                                        size = CarbonTileSize.MEDIUM,
                                        alert = kpiAlert(attentionCount),
                                        onClick = { overviewTab = 1 },
                                        contentPadding = false,
                                    ) {
                                        CarbonSmallTileContent("Needs attention", attentionCount.toString(), CarbonIcons.ReportProblem)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        if (attentionCount > 0) {
                            reviews.forEachIndexed { index, review ->
                                DenseAttentionRow(
                                    title = review.sheetCode,
                                    subtitle = "${review.projectName} · ${if (review.status == "SUBMITTED") "Ready to check" else "Awaiting approval"}",
                                    icon = CarbonIcons.FactCheck,
                                    dividerAbove = index > 0,
                                ) { onOpenRecord(review.projectId, WorkspacePage.BOOK, review.id) }
                            }
                            tasks.forEachIndexed { index, task ->
                                DenseAttentionRow(
                                    title = task.title,
                                    subtitle = "${task.projectName} · ${taskDueLabel(task.dueDate)}",
                                    icon = CarbonIcons.Assignment,
                                    dividerAbove = reviews.isNotEmpty() || index > 0,
                                ) { onOpenRecord(task.projectId, WorkspacePage.TASKS, task.id) }
                            }
                            issues.forEachIndexed { index, issue ->
                                DenseAttentionRow(
                                    title = issue.title,
                                    subtitle = "${issue.projectName} · ${issue.severity.lowercase().replaceFirstChar { it.uppercase() }}",
                                    icon = CarbonIcons.ReportProblem,
                                    dividerAbove = reviews.isNotEmpty() || tasks.isNotEmpty() || index > 0,
                                ) { onOpenRecord(issue.projectId, WorkspacePage.SNAGS, issue.id) }
                            }
                        } else {
                            DenseAttentionRow(
                                title = "You're up to date",
                                subtitle = "No open tasks, reviews or site issues.",
                                icon = CarbonIcons.CheckCircle,
                                dividerAbove = false,
                                onClick = onProjects,
                            )
                        }
                    }
                }
            }
        }

        item {
            DisclosureCard(
                title = "Fees",
                summary = fees?.let {
                    if (it.unpaidInvoices == 0) "No unpaid invoices"
                    else "${it.unpaidInvoices} unpaid · outstanding ₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(it.outstanding)}"
                } ?: "Loading fee balances…",
                expanded = feesOpen,
                onExpandedChange = { feesOpen = it },
                leadingIcon = CarbonIcons.ReceiptLong,
                size = CarbonTileSize.LARGE,
                alert = fees.let { summary ->
                    when {
                        summary == null -> CarbonAlertTone.NONE
                        summary.unpaidInvoices > 0 || summary.outstanding.signum() > 0 -> CarbonAlertTone.WARNING
                        else -> CarbonAlertTone.OK
                    }
                },
                menu = {
                    TextButton(onClick = onInvoices) { Text("Invoices") }
                },
            ) {
                CompactFeeSummary(fees, onInvoices)
            }
        }

        item {
            DisclosureCard(
                title = "Recent projects",
                summary = if (current.isEmpty()) "No current projects" else "${current.size} current · showing ${minOf(4, current.size)}",
                expanded = projectsOpen,
                onExpandedChange = { projectsOpen = it },
                leadingIcon = CarbonIcons.Apartment,
                size = CarbonTileSize.LARGE,
                menu = {
                    TextButton(onClick = onProjects) { Text("View all") }
                },
            ) {
                if (current.isEmpty()) {
                    DenseAttentionRow(
                        title = "Start a project",
                        subtitle = "Create your first project to organize work.",
                        icon = CarbonIcons.Add,
                        dividerAbove = false,
                        onClick = onNewProject,
                    )
                } else {
                    current.take(4).forEachIndexed { index, project ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        DenseProjectRow(project, onOpenProject)
                    }
                }
            }
        }

    }
}

private fun kpiAlert(count: Int?): CarbonAlertTone = when {
    count == null -> CarbonAlertTone.NONE
    count > 0 -> CarbonAlertTone.WARNING
    else -> CarbonAlertTone.OK
}

@Composable
private fun CompactStat(
    label: String,
    value: String,
    icon: ImageVector,
    alert: CarbonAlertTone = CarbonAlertTone.NONE,
    onClick: (() -> Unit)? = null,
) {
    CarbonTile(
        onClick = onClick,
        alert = alert,
        size = CarbonTileSize.SMALL,
        contentPadding = false,
    ) {
        CarbonSmallTileContent(label = label, value = value, icon = icon)
    }
}

@Composable
private fun CompactFeeSummary(summary: FeeDashboardSummary?, onOpen: () -> Unit) {
    val formatter = remember {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
    ) {
        if (summary == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text("Loading fee balances…", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(
                Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                FeeMetric(
                    label = "Outstanding",
                    value = "₹${formatter.format(summary.outstanding)}",
                    emphasize = true,
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(Modifier.padding(horizontal = CarbonSpacing.stackTight))
                FeeMetric(
                    label = "Received",
                    value = "₹${formatter.format(summary.received)}",
                    modifier = Modifier.weight(1f),
                )
                VerticalDivider(Modifier.padding(horizontal = CarbonSpacing.stackTight))
                FeeMetric(
                    label = "TDS",
                    value = "₹${formatter.format(summary.tdsWithheld)}",
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                if (summary.unpaidInvoices == 0) "No unpaid invoices"
                else "${summary.unpaidInvoices} unpaid invoice${if (summary.unpaidInvoices == 1) "" else "s"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeeMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DenseAttentionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    dividerAbove: Boolean,
    onClick: () -> Unit,
) {
    if (dividerAbove) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(CarbonSpacing.cardDense),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(CarbonIcons.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DenseProjectRow(project: ProjectEntity, onOpen: (Long) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(project.id) }
            .padding(vertical = CarbonSpacing.stackTight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
    ) {
        Icon(CarbonIcons.Apartment, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline)) {
            Row(horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    project.projectCode,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    project.status.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(project.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(project.client, project.siteLocation).filter { it.isNotBlank() }.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(CarbonIcons.ArrowForward, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal fun taskDueLabel(
    timestamp: Long?,
    today: LocalDate = LocalDate.now(),
    zone: ZoneId = ZoneId.systemDefault(),
): String {
    if (timestamp == null) return "No due date"
    val date = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
    return when {
        date < today -> "Overdue · ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
        date == today -> "Due today"
        else -> "Due ${date.format(DateTimeFormatter.ofPattern("d MMM"))}"
    }
}
