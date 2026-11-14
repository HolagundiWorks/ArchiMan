@file:OptIn(ExperimentalFoundationApi::class)

package com.holagundi.archiman.ui

import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.data.local.PracticeBoardTask
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.domain.WorkPriority
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
import com.holagundi.archiman.ui.theme.CarbonTag
import com.holagundi.archiman.ui.theme.CarbonTextInput
import com.holagundi.archiman.ui.theme.CarbonTile
import com.holagundi.archiman.ui.theme.CarbonTileSize
import com.holagundi.archiman.ui.theme.ScreenTitle
import com.holagundi.archiman.ui.theme.priorityAlertTone

private val BoardColumns = listOf("TO_DO" to "To do", "DOING" to "Doing", "DONE" to "Complete")
private const val TaskDragLabel = "practice-task"
private const val AllProjectsId = "0"

@Composable
fun PracticeKanban(
    viewModel: PortfolioViewModel,
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit,
) {
    val tasks by viewModel.boardTasks.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProjects = remember(projects) { projects.filter { it.status != "ARCHIVED" } }
    var projectFilterId by rememberSaveable { mutableStateOf(AllProjectsId) }
    var statusTab by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(activeProjects) {
        if (projectFilterId != AllProjectsId && activeProjects.none { it.id.toString() == projectFilterId }) {
            projectFilterId = AllProjectsId
        }
    }

    val scopedTasks = remember(tasks, projectFilterId) {
        if (projectFilterId == AllProjectsId) tasks
        else tasks.filter { it.projectId.toString() == projectFilterId }
    }
    val statusCounts = BoardColumns.map { (status, _) ->
        scopedTasks.count { matchesColumn(it.status, status) }
    }
    val selectedStatus = BoardColumns[statusTab.coerceIn(0, BoardColumns.lastIndex)].first
    val selectedLabel = BoardColumns[statusTab.coerceIn(0, BoardColumns.lastIndex)].second
    val columnTasks = scopedTasks.filter { matchesColumn(it.status, selectedStatus) }
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
            ScreenTitle("Tasks")
            Text(
                if (activeProjects.isEmpty()) {
                    "Create a project to start tracking tasks."
                } else {
                    "Filter by project, then switch To do, Doing, and Complete. Use + to add a task."
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
                    helperText = "Filter tasks for one project or show all.",
                    size = CarbonDropdownSize.Large,
                )
            }
        }

        CarbonTabs(
            tabs = BoardColumns.mapIndexed { index, (_, label) ->
                CarbonTab(
                    label = label,
                    count = statusCounts.getOrElse(index) { 0 },
                    secondaryLabel = "${statusCounts.getOrElse(index) { 0 }} tasks",
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
            PracticeBoardPanel(
                status = selectedStatus,
                label = selectedLabel,
                tasks = columnTasks,
                showProject = showProjectOnTiles,
                emptyHint = when {
                    activeProjects.isEmpty() -> "No projects yet."
                    scopedTasks.isEmpty() -> "No tasks for this filter. Use + to add one."
                    else -> "No tasks in $selectedLabel. Drop a task here or move one with the arrows."
                },
                onDropTask = { taskId -> viewModel.moveBoardTask(taskId, selectedStatus) },
                onMove = { taskId, next ->
                    viewModel.moveBoardTask(taskId, next)
                    val index = BoardColumns.indexOfFirst { it.first == next }
                    if (index >= 0) statusTab = index
                },
                onOpen = { task -> onOpenRecord(task.projectId, WorkspacePage.TASKS, task.id) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = CarbonSpacing.screen),
            )
        }
    }
}

@Composable
fun CreatePracticeTaskDialog(
    projects: List<ProjectEntity>,
    preferredProjectId: Long,
    busy: Boolean,
    onDismiss: () -> Unit,
    onCreate: (projectId: Long, title: String, priority: String) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var priority by rememberSaveable { mutableStateOf(WorkPriority.NORMAL) }
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
    val priorityOptions = remember {
        WorkPriority.all.map { CarbonDropdownOption(it, WorkPriority.label(it)) }
    }

    CarbonModal(
        title = "New task",
        description = "Choose a project, set priority, and add the task to To do.",
        onDismiss = onDismiss,
        primaryLabel = "Create",
        onPrimary = { onCreate(projectId, title.trim(), priority) },
        primaryEnabled = projectId != 0L && title.isNotBlank(),
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
            label = "Task",
            placeholder = "Task title",
            enabled = !busy,
        )
        CarbonDropdown(
            label = "Priority",
            options = priorityOptions,
            selectedId = priority,
            onSelected = { priority = it },
            enabled = !busy,
            size = CarbonDropdownSize.Medium,
        )
    }
}

@Composable
private fun PracticeBoardPanel(
    status: String,
    label: String,
    tasks: List<PracticeBoardTask>,
    showProject: Boolean,
    emptyHint: String,
    onDropTask: (Long) -> Unit,
    onMove: (Long, String) -> Unit,
    onOpen: (PracticeBoardTask) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hovering by remember { mutableStateOf(false) }
    val dropTarget = remember(status, onDropTask) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hovering = false
                val text = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString().orEmpty()
                val taskId = text.toLongOrNull() ?: return false
                onDropTask(taskId)
                return true
            }

            override fun onEntered(event: DragAndDropEvent) {
                hovering = true
            }

            override fun onExited(event: DragAndDropEvent) {
                hovering = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                hovering = false
            }
        }
    }
    Surface(
        modifier = modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
            },
            target = dropTarget,
        ),
        color = if (hovering) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RectangleShape,
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
            contentPadding = PaddingValues(
                top = CarbonSpacing.stack,
                bottom = CarbonSpacing.scrollBottom,
            ),
        ) {
            item {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() },
                )
            }
            if (tasks.isEmpty()) {
                item {
                    Text(
                        emptyHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            items(tasks, key = { it.id }) { task ->
                PracticeBoardTile(
                    task = task,
                    column = status,
                    showProject = showProject,
                    onMove = { onMove(task.id, it) },
                    onOpen = { onOpen(task) },
                )
            }
        }
    }
}

@Composable
private fun PracticeBoardTile(
    task: PracticeBoardTask,
    column: String,
    showProject: Boolean,
    onMove: (String) -> Unit,
    onOpen: () -> Unit,
) {
    val backStatus = when (column) {
        "DOING" -> "TO_DO"
        "DONE" -> "DOING"
        else -> null
    }
    val forwardStatus = when (column) {
        "TO_DO" -> "DOING"
        "DOING" -> "DONE"
        "DONE" -> "TO_DO"
        else -> "DOING"
    }
    val forwardLabel = when (forwardStatus) {
        "DOING" -> "Move to Doing"
        "DONE" -> "Mark complete"
        else -> "Reopen to To do"
    }
    val backLabel = when (backStatus) {
        "TO_DO" -> "Move to To do"
        "DOING" -> "Move to Doing"
        else -> null
    }
    val alert = priorityAlertTone(task.priority)
    CarbonTile(
        Modifier.dragAndDropSource {
            DragAndDropTransferData(
                ClipData.newPlainText(TaskDragLabel, task.id.toString()),
            )
        },
        contentPadding = false,
        alert = alert,
        size = CarbonTileSize.MEDIUM,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = CarbonSpacing.cardDense, end = CarbonSpacing.micro, top = CarbonSpacing.micro, bottom = CarbonSpacing.micro),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
        ) {
            Icon(
                CarbonIcons.Assignment,
                contentDescription = "Drag task",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = CarbonSpacing.stackTight, vertical = CarbonSpacing.stackTight)
                    .clickable(onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CarbonTag(WorkPriority.label(task.priority), alert = alert)
                    if (showProject) {
                        Text(
                            task.projectName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (backStatus != null && backLabel != null) {
                IconButton(onClick = { onMove(backStatus) }) {
                    Icon(CarbonIcons.ArrowBack, backLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = { onMove(if (task.status == "OPEN") "DOING" else forwardStatus) }) {
                Icon(
                    imageVector = when (forwardStatus) {
                        "DONE" -> CarbonIcons.Check
                        "TO_DO" -> CarbonIcons.Unarchive
                        else -> CarbonIcons.ArrowForward
                    },
                    contentDescription = forwardLabel,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun matchesColumn(status: String, column: String): Boolean =
    status == column || (column == "TO_DO" && status == "OPEN")
