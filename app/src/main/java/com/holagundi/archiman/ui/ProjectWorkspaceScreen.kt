package com.holagundi.archiman.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.ui.shell.ProjectScaffold
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.announce

@Composable
fun ProjectWorkspaceScreen(
    onBack: () -> Unit,
    viewModel: ProjectWorkspaceViewModel = hiltViewModel(),
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val sheets by viewModel.sheets.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val suite: SuiteProjectViewModel = hiltViewModel()
    val notice by suite.notice.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf(WorkspacePage.BRIEF) }
    val current = project

    ProjectScaffold(
        projectCode = current?.projectCode.orEmpty(),
        projectName = current?.name.orEmpty(),
        status = current?.status.orEmpty(),
        page = page,
        sheetCount = sheets.size,
        onPageChange = { page = it },
        onBack = onBack,
    ) { innerPadding ->
        AnimatedContent(
            targetState = page,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it / 10 }) togetherWith (fadeOut() + slideOutHorizontally { -it / 10 })
            },
            label = "project-section",
        ) { selected ->
            Column(Modifier.fillMaxSize()) {
                if (notice != null) {
                    Text(notice ?: "", color = MaterialTheme.colorScheme.primary, modifier = Modifier.announce())
                    TextButton(onClick = suite::clearNotice) { Text("Dismiss notice") }
                }
                when (selected) {
                    WorkspacePage.BRIEF -> BriefPane(suite)
                    WorkspacePage.TASKS -> KanbanPane(suite)
                    WorkspacePage.LETTERS -> LettersPane(current?.id, suite)
                    WorkspacePage.PROPOSAL -> ProposalPane(suite)
                    WorkspacePage.CONTRACT -> ContractsPane(suite, contractors)
                    WorkspacePage.VISITS -> SitePane(suite)
                    WorkspacePage.SNAGS -> SiteIssuesPane(suite)
                    WorkspacePage.COORDINATION -> CoordinationPane(suite)
                    WorkspacePage.DRAWINGS -> DrawingsPane(suite)
                    WorkspacePage.SITEMAN -> SitemanPane(
                        suite,
                        projectName = current?.name.orEmpty(),
                        projectCode = current?.projectCode.orEmpty(),
                    )
                    WorkspacePage.LEVELS -> if (current != null) {
                        OverviewPane(current, floors, rooms, measurements.size, sheets.size, viewModel)
                    } else {
                        Opening()
                    }
                    WorkspacePage.MEASURE -> RecordMeasurementScreen(onDone = { page = WorkspacePage.BOOK })
                    WorkspacePage.BOOK -> MeasurementBookScreen(onRecord = { page = WorkspacePage.MEASURE })
                    WorkspacePage.INVOICE -> FeeInvoicePane(suite)
                    WorkspacePage.BILL -> EstimatePane(current?.projectCode.orEmpty(), suite)
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(project.client, style = MaterialTheme.typography.titleMedium)
                Text(project.siteLocation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${project.projectType} · ${project.builtUpArea} ${project.areaUnit}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$sheetCount sheets · $measurementCount measurements", color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { showFloor = true }) { Text("Add level") }
        }
        floors.forEach { floor ->
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(floor.name, style = MaterialTheme.typography.titleSmall, fontFamily = FontFamily.Monospace)
                    rooms.filter { it.floorId == floor.id }.forEach { room ->
                        Text("· ${room.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
