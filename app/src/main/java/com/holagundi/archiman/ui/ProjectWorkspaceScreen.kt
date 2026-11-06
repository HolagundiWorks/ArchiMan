package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import androidx.activity.compose.BackHandler

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.ui.shell.ProjectScaffold
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.shell.ProjectGroup
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.announce

@Composable
fun ProjectWorkspaceScreen(
    onBack: () -> Unit,
    startPage: WorkspacePage = WorkspacePage.OVERVIEW,
    focusId: Long = 0L,
    viewModel: ProjectWorkspaceViewModel = hiltViewModel(),
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val suite: SuiteProjectViewModel = hiltViewModel()
    val notice by suite.notice.collectAsStateWithLifecycle()
    val operationNotice by suite.operationNotice.collectAsStateWithLifecycle()
    val pending by suite.pending.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf<WorkspacePage?>(startPage) }
    var captureSession by rememberSaveable { mutableStateOf(0L) }
    var group by rememberSaveable { mutableStateOf(startPage.group) }
    var lastWork by rememberSaveable { mutableStateOf(startPage.takeIf { it.group == ProjectGroup.WORK }) }
    var lastSite by rememberSaveable { mutableStateOf(startPage.takeIf { it.group == ProjectGroup.SITE }) }
    var lastFees by rememberSaveable { mutableStateOf(startPage.takeIf { it.group == ProjectGroup.FEES }) }
    val pageState = rememberSaveableStateHolder()
    fun selectPage(next: WorkspacePage?) {
        if (next == WorkspacePage.MEASURE && page != WorkspacePage.MEASURE) captureSession++
        if (next != null) group = next.group
        page = next
        when (group) {
            ProjectGroup.WORK -> lastWork = next
            ProjectGroup.SITE -> lastSite = next
            ProjectGroup.FEES -> lastFees = next
            else -> Unit
        }
    }
    fun back() {
        if (page == WorkspacePage.OVERVIEW) onBack()
        else if (page != null) selectPage(page!!.parent)
        else { group = ProjectGroup.OVERVIEW; page = WorkspacePage.OVERVIEW }
    }
    BackHandler(enabled = page != WorkspacePage.OVERVIEW) { back() }
    val current = project
    var opened by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(current?.id) {
        if (current != null) opened = true else if (opened) onBack()
    }

    if (page == WorkspacePage.MEASURE) {
        pageState.SaveableStateProvider("capture-$captureSession") {
            RecordMeasurementScreen(onDone = { selectPage(WorkspacePage.BOOK) }, onSetup = { selectPage(WorkspacePage.SETUP) }, onCancel = { selectPage(WorkspacePage.MEASUREMENTS) })
        }
        return
    }

    ProjectScaffold(
        projectCode = current?.projectCode.orEmpty(),
        projectName = current?.name.orEmpty(),
        group = group,
        page = page,
        onGroupChange = { next ->
            val repeated = next == group
            group = next
            page = if (next == ProjectGroup.OVERVIEW) WorkspacePage.OVERVIEW else if (repeated) null else when (next) {
                ProjectGroup.WORK -> lastWork
                ProjectGroup.SITE -> lastSite
                ProjectGroup.FEES -> lastFees
                else -> WorkspacePage.OVERVIEW
            }
            if (repeated && next != ProjectGroup.OVERVIEW) selectPage(null)
        },
        onBack = ::back,
        actions = { if (current != null) RecordActions(RecordKind.PROJECT, current.id, current.name) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            val selected = page
            pageState.SaveableStateProvider(selected?.name ?: "hub-${group.name}") {
            Column(Modifier.fillMaxSize()) {
                if (pending.isNotEmpty()) Text("Saving… Your entries will clear only after saving.", modifier = Modifier.announce())
                operationNotice?.let {
                    com.holagundi.archiman.ui.theme.CarbonInlineNotification(if (it.kind == com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR) "Could not save" else "Saved", it.message, it.kind)
                    TextButton(onClick = suite.operations::dismiss) { Text("Dismiss update") }
                }
                if (notice != null) {
                    com.holagundi.archiman.ui.theme.CarbonInlineNotification("Project update", notice ?: "", com.holagundi.archiman.ui.theme.CarbonNoticeKind.INFO)
                    TextButton(onClick = suite::clearNotice) { Text("Dismiss notice") }
                }
                when (selected) {
                    null -> ProjectSectionHub(group, onOpen = ::selectPage)
                    WorkspacePage.DOCUMENTS, WorkspacePage.MEASUREMENTS -> ProjectSectionHub(group, selected, ::selectPage)
                    WorkspacePage.OVERVIEW -> if (current != null) {
                        ProjectOverviewPage(current, viewModel, suite, ::selectPage)
                    } else Opening()
                    WorkspacePage.BRIEF -> BriefPane(suite)
                    WorkspacePage.TASKS -> KanbanPane(suite, if (startPage == WorkspacePage.TASKS) focusId else 0L)
                    WorkspacePage.LETTERS -> LettersPane(current?.id, suite)
                    WorkspacePage.PROPOSAL -> ProposalPane(suite)
                    WorkspacePage.CONTRACT -> ContractsPane(suite, contractors)
                    WorkspacePage.VISITS -> SitePane(suite)
                    WorkspacePage.SNAGS -> SiteIssuesPane(suite, if (startPage == WorkspacePage.SNAGS) focusId else 0L)
                    WorkspacePage.COORDINATION -> CoordinationPane(suite)
                    WorkspacePage.DRAWINGS -> DrawingsPane(suite)
                    WorkspacePage.SITEMAN -> SitemanPane(
                        suite,
                        projectName = current?.name.orEmpty(),
                        projectCode = current?.projectCode.orEmpty(),
                    )
                    WorkspacePage.LEVELS -> if (current != null) {
                        ProjectLevelsPage(current, floors, rooms, viewModel)
                    } else {
                        Opening()
                    }
                    WorkspacePage.SETUP -> ProjectSetupPane(viewModel, suite, { selectPage(WorkspacePage.MEASURE) }, { selectPage(WorkspacePage.BOOK) }, { selectPage(WorkspacePage.BILL) })
                    WorkspacePage.MEASURE -> Unit // Capture uses the dedicated viewport above.
                    WorkspacePage.BOOK -> MeasurementBookScreen(onRecord = { selectPage(WorkspacePage.MEASURE) }, focusId = if (startPage == WorkspacePage.BOOK) focusId else 0L)
                    WorkspacePage.INVOICE -> FeeInvoicePane(suite)
                    WorkspacePage.BILL -> RunningBillsPane(suite, { selectPage(WorkspacePage.BOOK) }, { selectPage(WorkspacePage.SETUP) })
                }
            }
            }
        }
    }
}

@Composable
private fun Opening() {
    Text(
        stringResource(R.string.opening_project),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(8.dp),
    )
}

@Composable
private fun ProjectOverviewPage(
    project: com.holagundi.archiman.data.local.entity.ProjectEntity,
    workspace: ProjectWorkspaceViewModel,
    suite: SuiteProjectViewModel,
    onOpen: (WorkspacePage) -> Unit,
) {
    val sheets by workspace.sheets.collectAsStateWithLifecycle()
    ProjectDashboard(project, sheets.size, suite, onOpen)
}

@Composable
private fun ProjectLevelsPage(
    project: com.holagundi.archiman.data.local.entity.ProjectEntity,
    floors: List<com.holagundi.archiman.data.local.entity.FloorEntity>,
    rooms: List<com.holagundi.archiman.data.local.entity.RoomEntity>,
    viewModel: ProjectWorkspaceViewModel,
) {
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    OverviewPane(project, floors, rooms, measurements.size, sheets.size, viewModel)
}

@Composable
private fun OverviewPane(
    project: com.holagundi.archiman.data.local.entity.ProjectEntity,
    floors: List<com.holagundi.archiman.data.local.entity.FloorEntity>,
    rooms: List<com.holagundi.archiman.data.local.entity.RoomEntity>,
    measurementCount: Int,
    sheetCount: Int,
    viewModel: ProjectWorkspaceViewModel,
) {
    var floorName by rememberSaveable { mutableStateOf("") }
    var roomFloorId by remember { mutableStateOf(-1L) }
    var roomName by rememberSaveable { mutableStateOf("") }
    var showFloor by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(project.client, style = MaterialTheme.typography.titleMedium)
                    Text(project.siteLocation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${project.projectType} · ${project.builtUpArea} ${project.areaUnit}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$sheetCount sheets · $measurementCount measurements", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showFloor = true }) { Text("Add level") }
            }
        }
        items(floors, key = { it.id }) { floor ->
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(floor.name, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                    RecordActions(RecordKind.LEVEL, floor.id, floor.name)
                    rooms.filter { it.floorId == floor.id }.forEach { room ->
                        Text("· ${room.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RecordActions(RecordKind.ROOM, room.id, room.name)
                    }
                    TextButton(onClick = { roomFloorId = floor.id; roomName = "" }) { Text("Add room on ${floor.name}") }
                }
            }
        }
    }
    if (showFloor) {
        Dialog(onDismissRequest = { showFloor = false }) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("New level", style = MaterialTheme.typography.titleMedium)
                    ArchiField(floorName, { floorName = it }, "Level name")
                    TextButton(onClick = {
                        if (floorName.isNotBlank()) {
                            viewModel.addFloor(floorName.trim())
                            floorName = ""
                            showFloor = false
                        }
                    }) { Text("Save level") }
                }
            }
        }
    }
    if (roomFloorId >= 0L) {
        Dialog(onDismissRequest = { roomFloorId = -1L }) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("New room", style = MaterialTheme.typography.titleMedium)
                    ArchiField(roomName, { roomName = it }, "Room name")
                    TextButton(onClick = {
                        if (roomName.isNotBlank()) {
                            viewModel.addRoom(roomFloorId, roomName.trim())
                            roomFloorId = -1L
                            roomName = ""
                        }
                    }) { Text("Save room") }
                }
            }
        }
    }
}
