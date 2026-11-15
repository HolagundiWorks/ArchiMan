package com.holagundi.archiman.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.data.local.PracticeBoardIssue
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.domain.SiteIssueWorkflow
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.CarbonDropdown
import com.holagundi.archiman.ui.theme.CarbonDropdownOption
import com.holagundi.archiman.ui.theme.CarbonDropdownSize
import com.holagundi.archiman.ui.theme.CarbonIcons
import com.holagundi.archiman.ui.theme.CarbonModal
import com.holagundi.archiman.ui.theme.CarbonSpacing
import com.holagundi.archiman.ui.theme.CarbonTab
import com.holagundi.archiman.ui.theme.CarbonTabStyle
import com.holagundi.archiman.ui.theme.CarbonTabs
import com.holagundi.archiman.ui.theme.CarbonTextInput
import com.holagundi.archiman.ui.theme.CarbonTag
import com.holagundi.archiman.ui.theme.CarbonTile
import com.holagundi.archiman.ui.theme.CarbonTileSize
import com.holagundi.archiman.ui.theme.ScreenTitle
import com.holagundi.archiman.ui.theme.priorityAlertTone

private val IssueColumns = listOf(
    "OPEN" to "Open",
    "IN_PROGRESS" to "In progress",
    "READY_FOR_VERIFICATION" to "Ready",
    "CLOSED" to "Closed",
)
private const val AllProjectsId = "0"

@Composable
fun PracticeIssues(
    viewModel: PortfolioViewModel,
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit,
) {
    val issues by viewModel.boardIssues.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProjects = remember(projects) { projects.filter { it.status != "ARCHIVED" } }
    var projectFilterId by rememberSaveable { mutableStateOf(AllProjectsId) }
    var statusTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(activeProjects) {
        if (projectFilterId != AllProjectsId && activeProjects.none { it.id.toString() == projectFilterId }) {
            projectFilterId = AllProjectsId
        }
    }

    val scopedIssues = remember(issues, projectFilterId) {
        if (projectFilterId == AllProjectsId) issues
        else issues.filter { it.projectId.toString() == projectFilterId }
    }
    val statusCounts = IssueColumns.map { (status, _) ->
        scopedIssues.count { it.status == status }
    }
    val selectedStatus = IssueColumns[statusTab.coerceIn(0, IssueColumns.lastIndex)].first
    val selectedLabel = IssueColumns[statusTab.coerceIn(0, IssueColumns.lastIndex)].second
    val columnIssues = scopedIssues.filter { it.status == selectedStatus }
    val showProjectOnTiles = projectFilterId == AllProjectsId
    val projectOptions = remember(activeProjects) {
        buildList {
            add(CarbonDropdownOption(AllProjectsId, "All projects"))
            activeProjects.forEach { add(CarbonDropdownOption(it.id.toString(), it.name)) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CarbonSpacing.screen, vertical = CarbonSpacing.stack),
            verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
        ) {
            ScreenTitle("Issues")
            Text(
                if (activeProjects.isEmpty()) {
                    "Create a project to start tracking site issues."
                } else {
                    "Filter by project, then switch Open, In progress, Ready, and Closed. Use + to add an issue."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            if (activeProjects.isNotEmpty()) {
                CarbonDropdown(
                    label = "Project",
                    options = projectOptions,
                    selectedId = projectFilterId,
                    onSelected = {
                        projectFilterId = it
                        statusTab = 0
                    },
                    helperText = "Filter issues for one project or show all.",
                    size = CarbonDropdownSize.Large,
                )
            }
        }

        CarbonTabs(
            tabs = IssueColumns.mapIndexed { index, (_, label) ->
                CarbonTab(
                    label = label,
                    count = statusCounts.getOrElse(index) { 0 },
                    secondaryLabel = "${statusCounts.getOrElse(index) { 0 }} issues",
                )
            },
            selectedIndex = statusTab,
            onSelected = { statusTab = it },
            style = CarbonTabStyle.Contained,
        )

        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RectangleShape,
        ) {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = CarbonSpacing.screen),
                verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
                contentPadding = PaddingValues(
                    top = CarbonSpacing.stack,
                    bottom = CarbonSpacing.scrollBottom,
                ),
            ) {
                item {
                    Text(
                        selectedLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { heading() },
                    )
                }
                if (columnIssues.isEmpty()) {
                    item {
                        Text(
                            when {
                                activeProjects.isEmpty() -> "No projects yet."
                                scopedIssues.isEmpty() -> "No issues for this filter. Use + to add one."
                                else -> "No issues in $selectedLabel."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                items(columnIssues, key = { it.id }) { issue ->
                    PracticeIssueTile(
                        issue = issue,
                        showProject = showProjectOnTiles,
                        onOpen = { onOpenRecord(issue.projectId, WorkspacePage.SNAGS, issue.id) },
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePracticeIssueDialog(
    projects: List<ProjectEntity>,
    preferredProjectId: Long,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (projectId: Long, title: String, description: String, severity: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableStateOf(SiteIssueWorkflow.SEVERITY_NORMAL) }
    var projectId by rememberSaveable(preferredProjectId) {
        mutableStateOf(
            preferredProjectId.takeIf { id -> projects.any { it.id == id } }
                ?: projects.firstOrNull()?.id
                ?: 0L,
        )
    }
    val options = remember(projects) {
        projects.map { CarbonDropdownOption(it.id.toString(), it.name) }
    }
    val severityOptions = remember {
        SiteIssueWorkflow.severities.map { CarbonDropdownOption(it, SiteIssueWorkflow.severityLabel(it)) }
    }

    CarbonModal(
        title = "New site issue",
        description = "Choose a project, set priority, and open the issue for site follow-up.",
        onDismiss = onDismiss,
        primaryLabel = "Create",
        onPrimary = { onCreate(projectId, title.trim(), description.trim(), severity) },
        primaryEnabled = projectId != 0L && title.isNotBlank() && description.isNotBlank(),
        primaryBusy = busy,
    ) {
        CarbonDropdown(
            label = "Project",
            options = options,
            selectedId = projectId.takeIf { it != 0L }?.toString(),
            onSelected = { projectId = it.toLongOrNull() ?: 0L },
            placeholder = "Choose a project",
            enabled = !busy,
            size = CarbonDropdownSize.Medium,
        )
        CarbonTextInput(
            value = title,
            onValueChange = { title = it },
            label = "Title",
            placeholder = "Issue title",
            enabled = !busy,
        )
        CarbonTextInput(
            value = description,
            onValueChange = { description = it },
            label = "Description",
            placeholder = "What needs attention on site",
            enabled = !busy,
            singleLine = false,
        )
        CarbonDropdown(
            label = "Priority",
            options = severityOptions,
            selectedId = severity,
            onSelected = { severity = it },
            enabled = !busy,
            size = CarbonDropdownSize.Medium,
        )
    }
}

@Composable
private fun PracticeIssueTile(
    issue: PracticeBoardIssue,
    showProject: Boolean,
    onOpen: () -> Unit,
) {
    val alert = priorityAlertTone(issue.severity)
    CarbonTile(contentPadding = false, alert = alert, size = CarbonTileSize.MEDIUM) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(CarbonSpacing.cardDense),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
        ) {
            Icon(
                CarbonIcons.ReportProblem,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.micro)) {
                Text(
                    issue.referenceNumber,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    issue.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CarbonTag(SiteIssueWorkflow.severityLabel(issue.severity), alert = alert)
                    if (showProject) {
                        Text(
                            issue.projectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
