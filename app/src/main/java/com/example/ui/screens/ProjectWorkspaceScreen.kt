package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.theme.*
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.ProjectSection
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    viewModel: SiteViewModel,
    onNavigateBack: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val projectContractorRefs by viewModel.projectContractorRefs.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val allMeasurements by viewModel.measurements.collectAsStateWithLifecycle()
    val projectTasks by viewModel.projectTasks.collectAsStateWithLifecycle()
    val selectionItems by viewModel.projectSelectionItems.collectAsStateWithLifecycle()
    val schedules by viewModel.projectSchedules.collectAsStateWithLifecycle()
    val meetingMinutes by viewModel.meetingMinutes.collectAsStateWithLifecycle()
    val siteInspections by viewModel.siteInspections.collectAsStateWithLifecycle()
    val projectDrawings by viewModel.projectDrawings.collectAsStateWithLifecycle()
    val drawingRevisions by viewModel.drawingRevisions.collectAsStateWithLifecycle()
    val drawingTransmittals by viewModel.drawingTransmittals.collectAsStateWithLifecycle()
    val rateBooks by viewModel.rateBooks.collectAsStateWithLifecycle()
    val rateBookAssignments by viewModel.projectRateBookAssignments.collectAsStateWithLifecycle()
    val consultancyProfile by viewModel.projectConsultancyProfile.collectAsStateWithLifecycle()
    val scopeItems by viewModel.projectScopeItems.collectAsStateWithLifecycle()
    val onboardingResponses by viewModel.projectOnboardingResponses.collectAsStateWithLifecycle()
    val approvals by viewModel.projectApprovals.collectAsStateWithLifecycle()
    val backlog by viewModel.projectBacklog.collectAsStateWithLifecycle()
    val selectedSection by viewModel.selectedProjectSection.collectAsStateWithLifecycle()

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()
    val projectMeasurements = remember(allMeasurements, currentProject) {
        if (currentProject != null) allMeasurements.filter { it.projectId == currentProject.id }
        else emptyList()
    }
    val projectContractors = remember(contractors, projectContractorRefs, currentProject) {
        val assignedIds = projectContractorRefs.map { it.contractorId }.toSet()
        if (currentProject != null) contractors.filter { it.id in assignedIds || it.projectId == currentProject.id }
        else emptyList()
    }

    var showSwitchProjectDialog by remember { mutableStateOf(false) }
    var showProjectProfileDialog by remember { mutableStateOf(false) }
    val sectionTitle = when (selectedSection) {
        ProjectSection.OVERVIEW -> "Project overview"
        ProjectSection.BRIEF_SCOPE -> "Brief & scope"
        ProjectSection.PLANNING -> "Planning"
        ProjectSection.MORE -> "Project tools"
        ProjectSection.DRAWINGS -> "Drawings"
        ProjectSection.REPORTS -> "Site reports"
        ProjectSection.CONTROLS -> "Onboarding & controls"
        ProjectSection.CONTRACTORS -> "Project team"
        ProjectSection.RATE_BOOKS -> "Rate books"
    }
    val navigateUp: () -> Unit = {
        when (selectedSection) {
            ProjectSection.OVERVIEW -> onNavigateBack()
            ProjectSection.MORE, ProjectSection.BRIEF_SCOPE, ProjectSection.PLANNING -> viewModel.setProjectSection(ProjectSection.OVERVIEW)
            else -> viewModel.setProjectSection(ProjectSection.MORE)
        }
    }

    Scaffold(
        containerColor = CarbonWhite,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonWhite)
                    .statusBarsPadding()
                    .drawBehind {
                        drawLine(
                            color = CarbonGray20,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            ) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = navigateUp,
                            modifier = Modifier
                                .size(36.dp)
                                .background(CarbonGray10, shape = RoundedCornerShape(2.dp))
                                .testTag("btn_back_home")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (selectedSection == ProjectSection.OVERVIEW) "Back to projects" else "Back in project",
                                tint = CarbonGray100,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = currentProject?.name ?: "Select Project",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray100,
                                maxLines = 1
                            )
                            Text(
                                text = listOf(sectionTitle, currentProject?.siteLocation.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
                                fontSize = 11.sp,
                                color = CarbonGray70
                            )
                        }
                    }

                    IconButton(
                        onClick = { showSwitchProjectDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(CarbonGray10, shape = RoundedCornerShape(2.dp))
                            .testTag("btn_switch_project")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Switch Project",
                            tint = CarbonBlue60,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Project Overview Stat Header Strip
                Surface(
                    color = CarbonGray10,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${projectMeasurements.size} entries  ·  ${projectContractors.size} team  ·  ${floors.size} levels",
                            modifier = Modifier.weight(1f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarbonGray80,
                            maxLines = 1
                        )

                        Surface(
                            color = CarbonBlue10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonBlue60)
                        ) {
                            Text(
                                text = currentProject?.status?.replace('_', ' ') ?: "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonBlue60,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedSection) {
                ProjectSection.OVERVIEW -> ProjectHubOverview(
                    project = currentProject,
                    taskCount = projectTasks.size,
                    selectionCount = selectionItems.size,
                    scheduleCount = schedules.size,
                    reportCount = meetingMinutes.size + siteInspections.size,
                    drawingCount = projectDrawings.size,
                    measurementCount = projectMeasurements.size,
                    briefStatus = consultancyProfile?.briefStatus ?: "NOT_STARTED",
                    onEditProfile = { showProjectProfileDialog = true },
                    onOpenBrief = { viewModel.setProjectSection(ProjectSection.BRIEF_SCOPE) },
                    onOpenPlanning = { viewModel.setProjectSection(ProjectSection.PLANNING) },
                    onOpenMore = { viewModel.setProjectSection(ProjectSection.MORE) }
                )
                ProjectSection.BRIEF_SCOPE -> ProjectSecondarySection("Project brief & scope") {
                    ProjectBriefScopeScreen(viewModel, currentProject, consultancyProfile, scopeItems)
                }
                ProjectSection.PLANNING -> ProjectSecondarySection("Planning") {
                    ProjectPlanningScreen(viewModel, projectTasks, schedules, selectionItems, currentProject)
                }
                ProjectSection.MORE -> ProjectMoreMenu(
                    drawingCount = projectDrawings.size,
                    reportCount = meetingMinutes.size + siteInspections.size,
                    contractorCount = projectContractors.size,
                    rateBookCount = rateBookAssignments.size,
                    controlCount = approvals.count { it.status != "APPROVED" } + backlog.count { it.status == "OPEN" },
                    onSelect = viewModel::setProjectSection
                )
                ProjectSection.DRAWINGS -> ProjectSecondarySection("Drawings") {
                    DrawingRegisterScreen(viewModel, projectDrawings, drawingRevisions, drawingTransmittals)
                }
                ProjectSection.REPORTS -> ProjectSecondarySection("Site reports") {
                    ProjectReportsScreen(viewModel, meetingMinutes, siteInspections)
                }
                ProjectSection.CONTROLS -> ProjectSecondarySection("Onboarding & controls") {
                    ProjectControlsScreen(viewModel, currentProject, onboardingResponses, approvals, backlog)
                }
                ProjectSection.CONTRACTORS -> ProjectSecondarySection("Project team") {
                    ProjectContractorsTab(
                        viewModel = viewModel,
                        currentProject = currentProject,
                        contractors = projectContractors,
                        allContractors = contractors,
                        measurements = projectMeasurements,
                        onViewContractorMeasurements = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) }
                    )
                }
                ProjectSection.RATE_BOOKS -> ProjectSecondarySection("Rate books") {
                    ProjectRateBooksTab(
                        viewModel = viewModel,
                        project = currentProject,
                        contractors = projectContractors,
                        pwdItems = items.filter { it.sourceName.isNotBlank() },
                        rateBooks = rateBooks,
                        assignments = rateBookAssignments
                    )
                }
            }
        }
    }

    // Switch Project Dialog
    if (showSwitchProjectDialog) {
        Dialog(onDismissRequest = { showSwitchProjectDialog = false }) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = CarbonWhite,
                border = BorderStroke(1.dp, CarbonGray30),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Switch Project",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray100
                    )
                    HorizontalDivider(color = CarbonGray20)
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(projects) { proj ->
                            val isSelected = proj.id == currentProject?.id
                            Surface(
                                color = if (isSelected) CarbonBlue10 else CarbonGray10,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, if (isSelected) CarbonBlue60 else CarbonGray30),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectProject(proj.id)
                                        showSwitchProjectDialog = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(proj.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CarbonGray100)
                                        Text(proj.siteLocation, fontSize = 11.sp, color = CarbonGray70)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = CarbonBlue60, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { showSwitchProjectDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CarbonGray100)
                    ) {
                        Text("Close", color = CarbonWhite)
                    }
                }
            }
        }
    }

    if (showProjectProfileDialog && currentProject != null) {
        ProjectProfileDialog(
            project = currentProject,
            onDismiss = { showProjectProfileDialog = false },
            onSave = {
                viewModel.updateProjectProfile(it)
                showProjectProfileDialog = false
            }
        )
    }
}

@Composable
private fun ProjectMoreMenu(
    drawingCount: Int,
    reportCount: Int,
    contractorCount: Int,
    rateBookCount: Int,
    controlCount: Int,
    onSelect: (ProjectSection) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Project tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Documents, field records and project setup", style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
        }
        item { Text("CORE WORKFLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray60, letterSpacing = 0.6.sp) }
        item { ProjectMoreRow("Project brief & scope", "Requirements, site data, deliverables and responsibilities", Icons.AutoMirrored.Filled.FactCheck) { onSelect(ProjectSection.BRIEF_SCOPE) } }
        item { ProjectMoreRow("Planning", "Tasks, schedules and material selections", Icons.AutoMirrored.Filled.EventNote) { onSelect(ProjectSection.PLANNING) } }
        item { ProjectMoreRow("Onboarding & controls", "$controlCount open approvals and backlog actions", Icons.AutoMirrored.Filled.Rule) { onSelect(ProjectSection.CONTROLS) } }
        item { Text("DOCUMENTS & SITE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray60, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 8.dp)) }
        item { ProjectMoreRow("Drawings", "$drawingCount registered drawings, revisions and transmittals", Icons.Default.Architecture) { onSelect(ProjectSection.DRAWINGS) } }
        item { ProjectMoreRow("Site reports", "$reportCount meeting minutes and inspection reports", Icons.AutoMirrored.Filled.Assignment) { onSelect(ProjectSection.REPORTS) } }
        item { Text("TEAM & RATE BOOKS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray60, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 8.dp)) }
        item { ProjectMoreRow("Project team", "$contractorCount assigned contractors", Icons.Default.Engineering) { onSelect(ProjectSection.CONTRACTORS) } }
        item { ProjectMoreRow("Rate books", "$rateBookCount rate books applied to this project", Icons.Default.PriceChange) { onSelect(ProjectSection.RATE_BOOKS) } }
    }
}

@Composable
private fun ProjectMoreRow(
    title: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        color = CarbonWhite,
        border = BorderStroke(1.dp, CarbonGray20),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(CarbonBlue10, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = CarbonBlue60, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = CarbonGray100)
                Text(supportingText, style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CarbonGray60)
        }
    }
}

@Composable
private fun ProjectSecondarySection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Surface(color = CarbonGray10, modifier = Modifier.fillMaxWidth()) {
            Text(title, color = CarbonGray100, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectBriefScopeScreen(
    viewModel: SiteViewModel,
    project: ProjectEntity?,
    savedProfile: ProjectConsultancyProfileEntity?,
    scopeItems: List<ProjectScopeItemEntity>
) {
    if (project == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Select a project first") }
        return
    }

    val consultancyOptions = listOf("Architectural", "Landscaping", "Part consultancy", "Miscellaneous")
    val phaseOptions = listOf("DESIGN", "EXECUTION", "HANDOVER")
    val designStageOptions = listOf("CONCEPT", "PRELIMINARY", "DESIGN_DEVELOPMENT", "APPROVAL_DRAWINGS", "WORKING_DRAWINGS", "DETAILED_DRAWINGS", "ISSUED_FOR_CONSTRUCTION")
    val briefStatusOptions = listOf("NOT_STARTED", "IN_PROGRESS", "UNDER_REVIEW", "CLARIFICATION_REQUIRED", "APPROVED")

    var selectedConsultancies by remember(project.id, savedProfile) {
        mutableStateOf(savedProfile?.consultancyTypes?.split(',')?.map(String::trim)?.filter(String::isNotBlank)?.toSet() ?: setOf("Architectural"))
    }
    var phase by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.currentPhase ?: "DESIGN") }
    var designStage by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.currentDesignStage ?: "CONCEPT") }
    var briefStatus by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.briefStatus ?: "NOT_STARTED") }
    var objectives by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.clientObjectives.orEmpty()) }
    var requirements by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.projectRequirements.orEmpty()) }
    var preferences by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.designPreferences.orEmpty()) }
    var constraints by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.siteConstraints.orEmpty()) }
    var clarifications by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.clarifications.orEmpty()) }
    var siteDimensions by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.siteDimensions.orEmpty()) }
    var siteOrientation by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.siteOrientation.orEmpty()) }
    var siteAccess by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.siteAccess.orEmpty()) }
    var existingConditions by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.existingConditions.orEmpty()) }
    var surroundings by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.surroundings.orEmpty()) }
    var topography by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.topography.orEmpty()) }
    var utilities by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.utilities.orEmpty()) }
    var existingStructures by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.existingStructures.orEmpty()) }
    var vegetation by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.vegetation.orEmpty()) }
    var legalPlanningInformation by remember(project.id, savedProfile) { mutableStateOf(savedProfile?.legalPlanningInformation.orEmpty()) }
    var siteDataExpanded by remember(project.id) { mutableStateOf(false) }
    var showAddScope by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Project brief", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Capture the agreed design intent before drawings and site work proceed.", style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
        }
        item {
            Surface(color = CarbonGray10, border = BorderStroke(1.dp, CarbonGray20), shape = RoundedCornerShape(4.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Consultancy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        consultancyOptions.forEach { option ->
                            FilterChip(
                                selected = option in selectedConsultancies,
                                onClick = {
                                    selectedConsultancies = if (option in selectedConsultancies) selectedConsultancies - option else selectedConsultancies + option
                                },
                                label = { Text(option) }
                            )
                        }
                    }
                    BriefDropdown("Current phase", phase, phaseOptions) { phase = it }
                    if (phase == "DESIGN") BriefDropdown("Design stage", designStage, designStageOptions) { designStage = it }
                    BriefDropdown("Brief status", briefStatus, briefStatusOptions) { briefStatus = it }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(objectives, { objectives = it }, label = { Text("Client objectives") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(requirements, { requirements = it }, label = { Text("Spaces and project requirements") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                OutlinedTextField(preferences, { preferences = it }, label = { Text("Design preferences") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(constraints, { constraints = it }, label = { Text("Known site / programme constraints") }, supportingText = { Text("Record known information only; this is not an automatic compliance assessment.") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(clarifications, { clarifications = it }, label = { Text("Open clarifications") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedButton(onClick = { siteDataExpanded = !siteDataExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Icon(if (siteDataExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (siteDataExpanded) "Hide site data" else "Add / review site data")
                }
                if (siteDataExpanded) {
                    Surface(color = CarbonGray10, border = BorderStroke(1.dp, CarbonGray20), shape = RoundedCornerShape(4.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Site data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Record supplied or observed information. ArchiMan does not certify legal or planning compliance.", style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
                            OutlinedTextField(siteDimensions, { siteDimensions = it }, label = { Text("Site dimensions") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(siteOrientation, { siteOrientation = it }, label = { Text("Orientation") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(siteAccess, { siteAccess = it }, label = { Text("Access and approach") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(existingConditions, { existingConditions = it }, label = { Text("Existing conditions") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(surroundings, { surroundings = it }, label = { Text("Surroundings") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(topography, { topography = it }, label = { Text("Topography") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(utilities, { utilities = it }, label = { Text("Utilities / services") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(existingStructures, { existingStructures = it }, label = { Text("Existing structures") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(vegetation, { vegetation = it }, label = { Text("Vegetation") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(legalPlanningInformation, { legalPlanningInformation = it }, label = { Text("Legal / planning information supplied") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        }
                    }
                }
                Button(
                    enabled = selectedConsultancies.isNotEmpty(),
                    onClick = {
                        viewModel.saveProjectConsultancyProfile(
                            ProjectConsultancyProfileEntity(
                                id = savedProfile?.id ?: 0,
                                projectId = project.id,
                                consultancyTypes = selectedConsultancies.sorted().joinToString(", "),
                                currentPhase = phase,
                                currentDesignStage = designStage,
                                briefStatus = briefStatus,
                                clientObjectives = objectives.trim(),
                                projectRequirements = requirements.trim(),
                                designPreferences = preferences.trim(),
                                siteConstraints = constraints.trim(),
                                clarifications = clarifications.trim(),
                                siteDimensions = siteDimensions.trim(),
                                siteOrientation = siteOrientation.trim(),
                                siteAccess = siteAccess.trim(),
                                existingConditions = existingConditions.trim(),
                                surroundings = surroundings.trim(),
                                topography = topography.trim(),
                                utilities = utilities.trim(),
                                existingStructures = existingStructures.trim(),
                                vegetation = vegetation.trim(),
                                legalPlanningInformation = legalPlanningInformation.trim()
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save project brief") }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Scope register", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Scope, deliverables, exclusions and responsibilities", style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
                }
                FilledTonalButton(onClick = { showAddScope = true }) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
        if (scopeItems.isEmpty()) {
            item {
                Surface(color = CarbonGray10, modifier = Modifier.fillMaxWidth()) {
                    Text("No scope items yet. Add the agreed inclusions before work begins.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(scopeItems, key = { it.id }) { item ->
                Surface(border = BorderStroke(1.dp, CarbonGray20), shape = RoundedCornerShape(4.dp), color = CarbonWhite) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.status == "COMPLETE", onCheckedChange = { viewModel.toggleProjectScopeItem(item) })
                        Column(Modifier.weight(1f)) {
                            Text(item.category.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = CarbonBlue60)
                            Text(item.title, fontWeight = FontWeight.SemiBold)
                            if (item.details.isNotBlank()) Text(item.details, style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
                            Text(item.status.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = CarbonGray60)
                        }
                        IconButton(onClick = { viewModel.deleteProjectScopeItem(item) }) { Icon(Icons.Default.DeleteOutline, "Delete scope item") }
                    }
                }
            }
        }
    }

    if (showAddScope) {
        AddScopeItemDialog(
            onDismiss = { showAddScope = false },
            onAdd = { category, title, details, status ->
                viewModel.addProjectScopeItem(category, title, details, status)
                showAddScope = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BriefDropdown(label: String, value: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }) },
                    onClick = { onValueChange(option); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScopeItemDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    val categories = listOf("SCOPE", "DELIVERABLE", "EXCLUSION", "RESPONSIBILITY")
    var category by remember { mutableStateOf("SCOPE") }
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    val initialStatus = if (category == "EXCLUSION") "EXCLUDED" else "INCLUDED"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add scope item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BriefDropdown("Category", category, categories) { category = it }
                OutlinedTextField(title, { title = it }, label = { Text("Title*") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(details, { details = it }, label = { Text("Details") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        },
        confirmButton = { Button(enabled = title.isNotBlank(), onClick = { onAdd(category, title, details, initialStatus) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProjectHubOverview(
    project: ProjectEntity?,
    taskCount: Int,
    selectionCount: Int,
    scheduleCount: Int,
    reportCount: Int,
    drawingCount: Int,
    measurementCount: Int,
    briefStatus: String,
    onEditProfile: () -> Unit,
    onOpenBrief: () -> Unit,
    onOpenPlanning: () -> Unit,
    onOpenMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("PROJECT HUB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonBlue60, letterSpacing = 0.7.sp)
                Text(project?.name ?: "Project overview", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CarbonGray100)
                Text("Brief, plan and coordinate the selected project.", fontSize = 12.sp, color = CarbonGray70)
            }
        }
        item {
            Surface(
                color = CarbonGray10,
                border = BorderStroke(1.dp, CarbonGray20),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onEditProfile)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = CarbonBlue60)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Project profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            listOfNotNull(
                                project?.projectCode?.takeIf(String::isNotBlank),
                                project?.projectType?.takeIf(String::isNotBlank),
                                project?.status?.takeIf(String::isNotBlank)
                            ).joinToString(" • ").ifBlank { "Add project code, type, dates and architect" },
                            color = CarbonGray70,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.Edit, contentDescription = "Edit project profile", tint = CarbonGray60)
                }
            }
        }
        item {
            Text("CONTINUE PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray60, letterSpacing = 0.6.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectHubActionCard("Brief", briefStatus.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }, Icons.AutoMirrored.Filled.FactCheck, onOpenBrief, Modifier.weight(1f))
                ProjectHubActionCard("Planning", "$taskCount tasks • $scheduleCount events • $selectionCount selections", Icons.AutoMirrored.Filled.EventNote, onOpenPlanning, Modifier.weight(1f))
            }
        }
        item {
            Text("AT A GLANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGray60, letterSpacing = 0.6.sp)
        }
        item {
            Surface(color = CarbonGray10, border = BorderStroke(1.dp, CarbonGray20), shape = RoundedCornerShape(4.dp)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    ProjectMetric("MEASUREMENTS", measurementCount.toString())
                    ProjectMetric("DRAWINGS", drawingCount.toString())
                    ProjectMetric("SITE REPORTS", reportCount.toString())
                }
            }
        }
        item {
            Surface(
                color = CarbonGray10,
                border = BorderStroke(1.dp, CarbonGray20),
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMore)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = CarbonBlue60)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("All project tools", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Drawings, site reports, team and rate books", color = CarbonGray70, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CarbonGray60)
                }
            }
        }
    }
}

@Composable
private fun ProjectMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CarbonGray100)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CarbonGray60)
    }
}

@Composable
private fun ProjectHubActionCard(
    title: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CarbonWhite,
        border = BorderStroke(1.dp, CarbonGray20),
        shape = RoundedCornerShape(2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = CarbonBlue60, modifier = Modifier.size(22.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CarbonGray100)
            Text(supportingText, fontSize = 10.sp, color = CarbonGray70)
        }
    }
}

private fun MeasurementEntity.linearUnitLabel(): String =
    if (unit.contains("ft", ignoreCase = true)) " ft" else " m"

@Composable
fun ProjectTasksTab(viewModel: SiteViewModel, tasks: List<ProjectTaskEntity>) {
    var showAdd by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (tasks.isEmpty()) {
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.TaskAlt, null, Modifier.size(40.dp), tint = CarbonGray50)
                Spacer(Modifier.height(8.dp))
                Text("No project tasks", fontWeight = FontWeight.Bold)
                Text("Add site actions, decisions, or follow-ups.", color = CarbonGray70, fontSize = 12.sp)
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = CarbonWhite), border = BorderStroke(1.dp, CarbonGray20)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.status == "DONE", onCheckedChange = { viewModel.toggleProjectTask(task) })
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold, color = CarbonGray100)
                            if (task.description.isNotBlank()) Text(task.description, fontSize = 12.sp, color = CarbonGray70)
                        }
                        IconButton(onClick = { viewModel.deleteProjectTask(task) }) { Icon(Icons.Default.DeleteOutline, "Delete task") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Default.Add, "Add task")
        }
    }
    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showAdd = false }, title = { Text("Add task") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Task") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, minLines = 2)
            }
        }, confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { viewModel.addProjectTask(title, description); showAdd = false }) { Text("Add") } },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } })
    }
}

@Composable
fun ProjectSelectionsTab(
    viewModel: SiteViewModel,
    project: ProjectEntity?,
    selectionItems: List<ProjectSelectionItemEntity>
) {
    val context = LocalContext.current
    var showAdd by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Specification / Selection List", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Quantity-only procurement schedule", color = CarbonGray70, fontSize = 11.sp)
            }
            TextButton(enabled = selectionItems.isNotEmpty(), onClick = {
                ExportHelper.exportSelectionListAsPurchaseOrder(context, project?.name ?: "Project", selectionItems)
            }) { Icon(Icons.Default.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Export PO") }
        }
        HorizontalDivider(color = CarbonGray20)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (selectionItems.isEmpty()) Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Checklist, null, Modifier.size(40.dp), tint = CarbonGray50)
                Spacer(Modifier.height(8.dp)); Text("No selection items", fontWeight = FontWeight.Bold)
                Text("Add materials, fixtures, finishes, or equipment.", color = CarbonGray70, fontSize = 12.sp, textAlign = TextAlign.Center)
            } else LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectionItems, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = CarbonWhite), border = BorderStroke(1.dp, CarbonGray20)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
                            Checkbox(checked = item.status == "SELECTED", onCheckedChange = { viewModel.toggleProjectSelectionItem(item) })
                            Column(Modifier.weight(1f)) {
                                Text(item.itemName, fontWeight = FontWeight.Bold)
                                Text("${item.quantity} ${item.unit}${item.makeOrBrand.takeIf(String::isNotBlank)?.let { " • $it" } ?: ""}", color = CarbonBlue60, fontSize = 12.sp)
                                if (item.specification.isNotBlank()) Text(item.specification, fontSize = 12.sp, color = CarbonGray70)
                            }
                            IconButton(onClick = { viewModel.deleteProjectSelectionItem(item) }) { Icon(Icons.Default.DeleteOutline, "Delete selection item") }
                        }
                    }
                }
            }
            FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, "Add selection item") }
        }
    }
    if (showAdd) SelectionItemDialog(onDismiss = { showAdd = false }) { n, s, b, q, u, r ->
        viewModel.addProjectSelectionItem(n, s, b, q, u, r); showAdd = false
    }
}

@Composable
private fun SelectionItemDialog(onDismiss: () -> Unit, onAdd: (String, String, String, Double, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }; var spec by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }; var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Nos") }; var remarks by remember { mutableStateOf("") }
    val qty = quantity.toDoubleOrNull()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add selection item") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Item / material") }, singleLine = true)
            OutlinedTextField(spec, { spec = it }, label = { Text("Specification") }, minLines = 2)
            OutlinedTextField(brand, { brand = it }, label = { Text("Make / brand (optional)") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(quantity, { quantity = it }, Modifier.weight(1f), label = { Text("Quantity") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(unit, { unit = it }, Modifier.weight(1f), label = { Text("Unit") }, singleLine = true)
            }
            OutlinedTextField(remarks, { remarks = it }, label = { Text("Remarks (optional)") })
        }
    }, confirmButton = { TextButton(enabled = name.isNotBlank() && qty != null && qty > 0, onClick = { onAdd(name, spec, brand, qty ?: 0.0, unit, remarks) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun CanonicalMeasurementLauncher(viewModel: SiteViewModel) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopCenter) {
        Surface(color = CarbonGray10, border = BorderStroke(1.dp, CarbonGray30), shape = RoundedCornerShape(2.dp)) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.TableRows, contentDescription = null, tint = CarbonBlue60, modifier = Modifier.size(28.dp))
                Text("Measurement Sheet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CarbonGray100)
                Text(
                    "All project, room, and component shortcuts now use the same spreadsheet-style measurement editor.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = CarbonGray70
                )
                Button(
                    onClick = { viewModel.openCanonicalMeasurement() },
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60),
                    shape = RoundedCornerShape(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Straighten, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Measurement Sheet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ====================================================================
// TAB 1: CONTRACTORS LIST
// ====================================================================

@Composable
fun ProjectContractorsTab(
    viewModel: SiteViewModel,
    currentProject: ProjectEntity?,
    contractors: List<ContractorEntity>,
    allContractors: List<ContractorEntity>,
    measurements: List<MeasurementEntity>,
    onViewContractorMeasurements: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAddContractorDialog by remember { mutableStateOf(false) }
    var contractorToDelete by remember { mutableStateOf<ContractorEntity?>(null) }
    val selectedExistingIds = remember { mutableStateListOf<Long>() }
    val availableExisting = remember(allContractors, contractors) {
        val assignedIds = contractors.map { it.id }.toSet()
        allContractors.filter { it.id !in assignedIds }
    }

    Scaffold(
        containerColor = CarbonWhite,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddContractorDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Contractor", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                containerColor = CarbonBlue60,
                contentColor = CarbonWhite,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.testTag("fab_add_contractor")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSIGNED CONTRACTORS (${contractors.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray70,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            if (contractors.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 30.dp),
                        color = CarbonGray10,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray30)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                tint = CarbonGray60,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No contractors added yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray100
                            )
                            Text(
                                text = "Add civil, RCC, masonry, plaster, or finishing contractors to assign them to measurements.",
                                fontSize = 12.sp,
                                color = CarbonGray70,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddContractorDialog = true },
                                shape = RoundedCornerShape(2.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add First Contractor", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(contractors, key = { it.id }) { contractor ->
                    val contractorEntries = measurements.filter { it.contractorId == contractor.id }
                    val totalQty = contractorEntries.sumOf { it.quantity }

                    Surface(
                        color = CarbonWhite,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray30),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(CarbonBlue10, shape = RoundedCornerShape(2.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = CarbonBlue60,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = contractor.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CarbonGray100
                                        )
                                        if (contractor.phone.isNotBlank()) {
                                            Text(
                                                text = "📞 ${contractor.phone}",
                                                fontSize = 11.sp,
                                                color = CarbonGray70
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { contractorToDelete = contractor },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Contractor",
                                        tint = CarbonGray60,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = CarbonGray20, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${contractorEntries.size} Recorded Entries • ${"%.2f".format(totalQty)} Total Qty",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CarbonGray80
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (contractor.phone.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                try {
                                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contractor.phone}"))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    // Fallback
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Call",
                                                tint = CarbonBlue60,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onViewContractorMeasurements(contractor.id) },
                                        shape = RoundedCornerShape(2.dp),
                                        border = BorderStroke(1.dp, CarbonBlue60),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("View M-Book", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonBlue60)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Contractor Dialog
    if (showAddContractorDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showAddContractorDialog = false }) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = CarbonWhite,
                border = BorderStroke(1.dp, CarbonGray30),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Contractor to Project",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray100
                    )
                    HorizontalDivider(color = CarbonGray20)

                    Text("ASSIGN EXISTING CONTRACTORS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarbonGray70)
                    if (availableExisting.isEmpty()) {
                        Text("All registered contractors are already assigned to this site.", fontSize = 12.sp, color = CarbonGray60)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(availableExisting, key = { it.id }) { existing ->
                                val selected = existing.id in selectedExistingIds
                                Surface(
                                    color = if (selected) CarbonBlue10 else CarbonGray10,
                                    border = BorderStroke(1.dp, if (selected) CarbonBlue60 else CarbonGray30),
                                    shape = RoundedCornerShape(2.dp),
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (selected) selectedExistingIds.remove(existing.id) else selectedExistingIds.add(existing.id)
                                    }
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = selected, onCheckedChange = {
                                            if (it) selectedExistingIds.add(existing.id) else selectedExistingIds.remove(existing.id)
                                        })
                                        Column {
                                            Text(existing.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(existing.contactNo.ifBlank { existing.phone }, fontSize = 10.sp, color = CarbonGray60)
                                        }
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = {
                                currentProject?.let { project ->
                                    selectedExistingIds.distinct().forEach { viewModel.assignContractorToProject(project.id, it) }
                                    selectedExistingIds.clear()
                                    showAddContractorDialog = false
                                }
                            },
                            enabled = selectedExistingIds.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Assign ${selectedExistingIds.size} Selected")
                        }
                    }

                    HorizontalDivider(color = CarbonGray20)
                    Text("OR CREATE A NEW CONTRACTOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CarbonGray70)

                    if (error != null) {
                        Text(text = error ?: "", color = CarbonRed60, fontSize = 11.sp)
                    }

                    CarbonInputField(
                        label = "CONTRACTOR / AGENCY NAME *",
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = "e.g. Ramesh Civil Works",
                        keyboardType = KeyboardType.Text,
                        testTag = "input_contractor_name"
                    )

                    CarbonInputField(
                        label = "PHONE / CONTACT NUMBER",
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = "e.g. +91 98765 43210",
                        keyboardType = KeyboardType.Phone,
                        testTag = "input_contractor_phone"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddContractorDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Cancel", color = CarbonGray80, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    error = "Contractor name is required."
                                } else if (currentProject != null) {
                                    viewModel.addContractor(
                                        projectId = currentProject.id,
                                        name = name.trim(),
                                        phone = phone.trim()
                                    ) {
                                        showAddContractorDialog = false
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CarbonWhite)
                        }
                    }
                }
            }
        }
    }

    // Delete Contractor Dialog
    if (contractorToDelete != null) {
        AlertDialog(
            onDismissRequest = { contractorToDelete = null },
            title = { Text("Delete Contractor?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to remove '${contractorToDelete?.name}' from this project?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        contractorToDelete?.let { contractor ->
                            currentProject?.let { project -> viewModel.removeContractorFromProject(project.id, contractor.id) }
                        }
                        contractorToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonRed60),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contractorToDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(2.dp)
        )
    }
}

// ====================================================================
// TAB 2: MEASUREMENT BOOK (RECORDED MEASUREMENTS)
// ====================================================================

@Composable
fun ProjectMeasurementBookTab(
    viewModel: SiteViewModel,
    currentProject: ProjectEntity?,
    floors: List<FloorEntity>,
    contractors: List<ContractorEntity>,
    items: List<ItemMasterEntity>,
    measurements: List<MeasurementEntity>,
    preselectedContractorId: Long? = null,
    onClearContractorFilter: () -> Unit = {},
    onNavigateToRecord: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedFloorFilter by remember { mutableStateOf<Long?>(null) }
    var selectedItemFilter by remember { mutableStateOf<Long?>(null) }
    var selectedContractorFilter by remember(preselectedContractorId) { mutableStateOf(preselectedContractorId) }
    var searchQuery by remember { mutableStateOf("") }
    var measurementToDelete by remember { mutableStateOf<MeasurementEntity?>(null) }

    val filteredMeasurements = remember(
        measurements,
        selectedFloorFilter,
        selectedItemFilter,
        selectedContractorFilter,
        searchQuery
    ) {
        measurements.filter { m ->
            (selectedFloorFilter == null || m.floorId == selectedFloorFilter) &&
            (selectedItemFilter == null || m.itemId == selectedItemFilter) &&
            (selectedContractorFilter == null || m.contractorId == selectedContractorFilter) &&
            (searchQuery.isBlank() ||
             m.itemName.contains(searchQuery, ignoreCase = true) ||
             m.description.contains(searchQuery, ignoreCase = true) ||
             m.floor.contains(searchQuery, ignoreCase = true) ||
             m.location.contains(searchQuery, ignoreCase = true) ||
             m.contractorName.contains(searchQuery, ignoreCase = true))
        }
    }

    val totalCalculatedQuantity = remember(filteredMeasurements) {
        filteredMeasurements.sumOf { it.quantity }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp)
    ) {
        // Export Actions & Search Bar
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Quick Export Toolbar
                Surface(
                    color = CarbonGray10,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "M-BOOK EXPORT:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray70
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // PDF
                            Surface(
                                color = CarbonRed10,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, CarbonRed60),
                                modifier = Modifier.clickable {
                                    ExportHelper.printMeasurementSheetPdf(
                                        context = context,
                                        projectName = currentProject?.name ?: "Project",
                                        measurements = filteredMeasurements
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = CarbonRed60, modifier = Modifier.size(12.dp))
                                    Text("PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonRed60)
                                }
                            }

                            // Excel XLS
                            Surface(
                                color = CarbonGreen10,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, CarbonGreen60),
                                modifier = Modifier.clickable {
                                    ExportHelper.exportAndShareXls(
                                        context = context,
                                        projectName = currentProject?.name ?: "Project",
                                        measurements = filteredMeasurements
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = CarbonGreen60, modifier = Modifier.size(12.dp))
                                    Text("Excel XLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonGreen60)
                                }
                            }

                            // CSV
                            Surface(
                                color = CarbonBlue10,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, CarbonBlue60),
                                modifier = Modifier.clickable {
                                    ExportHelper.exportAndShareCsv(
                                        context = context,
                                        projectName = currentProject?.name ?: "Project",
                                        measurements = filteredMeasurements
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = CarbonBlue60, modifier = Modifier.size(12.dp))
                                    Text("CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CarbonBlue60)
                                }
                            }
                        }
                    }
                }

                // Search & Filter Row
                Surface(
                    color = CarbonGray10,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = CarbonGray60, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(fontSize = 13.sp, color = CarbonGray100),
                            singleLine = true,
                            cursorBrush = SolidColor(CarbonBlue60),
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (searchQuery.isEmpty()) Text("Filter entries by member, item, floor...", fontSize = 12.sp, color = CarbonGray50)
                                    inner()
                                }
                            }
                        )
                        if (searchQuery.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = CarbonGray70,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { searchQuery = "" }
                            )
                        }
                    }
                }

                // Filter Chips (Floors & Contractors)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // All floors
                    item {
                        FilterChip(
                            selected = selectedFloorFilter == null,
                            onClick = { selectedFloorFilter = null },
                            label = { Text("All Floors", fontSize = 11.sp) },
                            shape = RoundedCornerShape(2.dp)
                        )
                    }

                    // Specific floors
                    items(floors) { f ->
                        FilterChip(
                            selected = selectedFloorFilter == f.id,
                            onClick = { selectedFloorFilter = if (selectedFloorFilter == f.id) null else f.id },
                            label = { Text(f.name, fontSize = 11.sp) },
                            shape = RoundedCornerShape(2.dp)
                        )
                    }

                    // Contractor active filter chip
                    if (selectedContractorFilter != null) {
                        val cont = contractors.firstOrNull { it.id == selectedContractorFilter }
                        item {
                            InputChip(
                                selected = true,
                                onClick = {
                                    selectedContractorFilter = null
                                    onClearContractorFilter()
                                },
                                label = { Text("👷 ${cont?.name ?: "Contractor"}", fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                shape = RoundedCornerShape(2.dp)
                            )
                        }
                    }
                }
            }
        }

        // Summary Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SHOWING ${filteredMeasurements.size} ENTRIES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray70,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Total Qty: ${"%.3f".format(totalCalculatedQuantity)}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonBlue60
                )
            }
        }

        // Measurement Rows
        if (filteredMeasurements.isEmpty()) {
            item {
                Surface(
                    color = CarbonGray10,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = CarbonGray60, modifier = Modifier.size(32.dp))
                        Text("No recorded measurements found", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CarbonGray100)
                        Text("Switch to 'Record Measurement' tab to enter dimensions and calculate quantities.", fontSize = 12.sp, color = CarbonGray70, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Button(
                            onClick = onNavigateToRecord,
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                        ) {
                            Text("+ Record New Measurement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(filteredMeasurements, key = { it.id }) { m ->
                val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
                val formattedDate = remember(m.date) { dateFormat.format(Date(m.date)) }

                Surface(
                    color = CarbonWhite,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonGray30),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Top row: Item Name + Formula badge + Delete
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = m.itemName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonGray100
                                )

                                Surface(
                                    color = CarbonBlue10,
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, CarbonBlue60)
                                ) {
                                    Text(
                                        text = m.unit,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CarbonBlue60,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { measurementToDelete = m },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = CarbonGray60, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Description / Location
                        Text(
                            text = listOfNotNull(m.floor.ifBlank { null }, m.location.ifBlank { null }, m.description.ifBlank { null }).joinToString(" • "),
                            fontSize = 12.sp,
                            color = CarbonGray70,
                            fontWeight = FontWeight.Medium
                        )

                        // Dimension Calculation Box
                        Surface(
                            color = CarbonGray10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonGray20),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val dimensionBreakdown = buildString {
                                    append("${m.nos.toInt()} nos × ")
                                    if (m.length > 0) append("${m.length}${m.linearUnitLabel()} ")
                                    if (m.width > 0) append("× ${m.width}m ")
                                    if (m.height > 0) append("× ${m.height}m")
                                    if (m.deduction > 0) append(" - Ded (${m.deduction})")
                                }
                                Text(
                                    text = dimensionBreakdown,
                                    fontSize = 12.sp,
                                    color = CarbonGray80,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "= ${"%.3f".format(m.quantity)} ${m.unit}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonBlue60
                                )
                            }
                        }
                        if (m.rateSnapshot != null) {
                            Text(
                                text = "Rate ₹ ${"%.2f".format(m.rateSnapshot)} / ${m.unit}  •  Amount ₹ ${"%.2f".format(m.amountSnapshot ?: 0.0)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarbonGray80
                            )
                        }

                        // Footer: Contractor tag + Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (m.contractorName.isNotBlank()) {
                                Text(
                                    text = "👷 ${m.contractorName}",
                                    fontSize = 11.sp,
                                    color = CarbonGray70
                                )
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }
                            Text(
                                text = formattedDate,
                                fontSize = 10.sp,
                                color = CarbonGray60
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (measurementToDelete != null) {
        AlertDialog(
            onDismissRequest = { measurementToDelete = null },
            title = { Text("Archive Measurement Sheet?", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("Archive this measurement sheet without deleting its recorded rows (${measurementToDelete?.itemName} - ${"%.3f".format(measurementToDelete?.quantity)} ${measurementToDelete?.unit})?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        measurementToDelete?.let { viewModel.deleteMeasurement(it) }
                        measurementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonRed60),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Archive", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { measurementToDelete = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(2.dp)
        )
    }
}

// ====================================================================
// TAB 3: RECORD MEASUREMENT (FAST 6-STEP DIMENSION ENTRY)
// ====================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectRecordMeasurementTab(
    viewModel: SiteViewModel,
    currentProject: ProjectEntity?,
    floors: List<FloorEntity>,
    contractors: List<ContractorEntity>,
    items: List<ItemMasterEntity>,
    onMeasurementSaved: (MeasurementEntity) -> Unit = {}
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val selectedFloorId by viewModel.selectedFloorId.collectAsStateWithLifecycle()
    val selectedItemId by viewModel.selectedItemId.collectAsStateWithLifecycle()
    val selectedContractorId by viewModel.selectedContractorId.collectAsStateWithLifecycle()
    val allQualifiedItems by viewModel.allQualifiedItems.collectAsStateWithLifecycle()

    val currentItem = items.firstOrNull { it.id == selectedItemId } ?: items.firstOrNull()
    val currentFloor = floors.firstOrNull { it.id == selectedFloorId } ?: floors.firstOrNull()
    val currentContractor = contractors.firstOrNull { it.id == selectedContractorId }
    val contractorItems = remember(selectedContractorId, allQualifiedItems) {
        if (selectedContractorId == null || selectedContractorId == 0L) emptyList()
        else allQualifiedItems.filter { it.contractorId == selectedContractorId }
    }

    // Multi-floor duplication selection
    val selectedDuplicationFloors = remember { mutableStateListOf<Long>() }
    var showAddFloorDialog by remember { mutableStateOf(false) }
    var workItemMenuExpanded by remember { mutableStateOf(false) }
    var floorMenuExpanded by remember { mutableStateOf(false) }
    var contractorMenuExpanded by remember { mutableStateOf(false) }

    val liveQuantity = remember(currentItem, formState) {
        if (currentItem != null) viewModel.computeQuantity(currentItem.calculationType, formState)
        else 0.0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
    ) {
        // Last Saved Feedback Banner
        if (formState.lastSavedSummary != null) {
            item {
                Surface(
                    color = CarbonGreen10,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonGreen60),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CarbonGreen60, modifier = Modifier.size(16.dp))
                        Text(
                            text = formState.lastSavedSummary ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGreen60,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // STEP 1: SELECT CONTRACTOR
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CarbonStepHeader(stepNumber = "1", title = "SELECT CONTRACTOR")
                ExposedDropdownMenuBox(
                    expanded = contractorMenuExpanded,
                    onExpandedChange = { if (contractors.isNotEmpty()) contractorMenuExpanded = !contractorMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = currentContractor?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Contractor") },
                        placeholder = { Text(if (contractors.isEmpty()) "No contractors assigned" else "Choose contractor first") },
                        leadingIcon = { Icon(Icons.Default.Engineering, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(contractorMenuExpanded) },
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = contractors.isNotEmpty())
                            .fillMaxWidth()
                            .testTag("project_dropdown_contractor")
                    )
                    ExposedDropdownMenu(expanded = contractorMenuExpanded, onDismissRequest = { contractorMenuExpanded = false }) {
                        contractors.forEach { contractor ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(contractor.name, fontWeight = FontWeight.SemiBold)
                                        val count = allQualifiedItems.count { it.contractorId == contractor.id }
                                        Text("$count qualified items", fontSize = 11.sp, color = CarbonGray70)
                                    }
                                },
                                onClick = {
                                    viewModel.selectContractor(contractor.id)
                                    contractorMenuExpanded = false
                                    workItemMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // STEP 2: SELECT WORK ITEM
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CarbonStepHeader(stepNumber = "2", title = "SELECT QUALIFIED WORK ITEM")

                ExposedDropdownMenuBox(
                    expanded = workItemMenuExpanded,
                    onExpandedChange = { if (contractorItems.isNotEmpty()) workItemMenuExpanded = !workItemMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Work item") },
                        placeholder = { Text(if (currentContractor == null) "Select contractor first" else if (contractorItems.isEmpty()) "No qualified items" else "Choose qualified item") },
                        leadingIcon = { Icon(Icons.Default.Construction, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(workItemMenuExpanded) },
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = contractorItems.isNotEmpty())
                            .fillMaxWidth()
                            .testTag("project_dropdown_work_item")
                    )
                    ExposedDropdownMenu(
                        expanded = workItemMenuExpanded,
                        onDismissRequest = { workItemMenuExpanded = false }
                    ) {
                        contractorItems.forEach { qualifiedItem ->
                            val masterItem = items.firstOrNull { it.name.equals(qualifiedItem.itemName, ignoreCase = true) }
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(qualifiedItem.itemName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "${qualifiedItem.uom} • ${qualifiedItem.calculationType.displayName}",
                                            fontSize = 11.sp,
                                            color = CarbonGray70
                                        )
                                    }
                                },
                                leadingIcon = if (masterItem?.id == selectedItemId) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = CarbonBlue60) }
                                } else null,
                                onClick = {
                                    masterItem?.let { viewModel.selectItem(it.id) }
                                    workItemMenuExpanded = false
                                    val project = currentProject
                                    val floor = currentFloor
                                    if (project != null && floor != null) {
                                        viewModel.startDedicatedMeasurement(
                                            projectId = project.id,
                                            contractorId = currentContractor?.id ?: 0L,
                                            itemName = qualifiedItem.itemName,
                                            uom = qualifiedItem.uom,
                                            calcType = qualifiedItem.calculationType,
                                            floorId = floor.id,
                                            floorName = floor.name,
                                            itemId = masterItem?.id
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // STEP 2: SELECT FLOOR LEVEL
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CarbonStepHeader(stepNumber = "3", title = "SELECT FLOOR LEVEL")
                    Text(
                        text = "+ Add Floor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonBlue60,
                        modifier = Modifier.clickable { showAddFloorDialog = true }
                    )
                }

                ExposedDropdownMenuBox(
                    expanded = floorMenuExpanded,
                    onExpandedChange = { if (floors.isNotEmpty()) floorMenuExpanded = !floorMenuExpanded }
                ) {
                    OutlinedTextField(
                        value = currentFloor?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Floor / level") },
                        placeholder = { Text(if (floors.isEmpty()) "No floors available" else "Choose a floor") },
                        leadingIcon = { Icon(Icons.Default.Layers, contentDescription = null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(floorMenuExpanded) },
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = floors.isNotEmpty())
                            .fillMaxWidth()
                            .testTag("project_dropdown_floor")
                    )
                    ExposedDropdownMenu(
                        expanded = floorMenuExpanded,
                        onDismissRequest = { floorMenuExpanded = false }
                    ) {
                        floors.forEach { floor ->
                            DropdownMenuItem(
                                text = { Text(floor.name, fontWeight = FontWeight.SemiBold) },
                                leadingIcon = if (floor.id == selectedFloorId) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = CarbonBlue60) }
                                } else null,
                                onClick = {
                                    viewModel.selectFloor(floor.id)
                                    floorMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // STEP 3: CONTRACTOR
        if (false) { item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CarbonStepHeader(stepNumber = "3", title = "SELECT CONTRACTOR")

                if (contractors.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ASSIGN CONTRACTOR (OPTIONAL)", fontSize = 11.sp, color = CarbonGray70, fontWeight = FontWeight.Medium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                Surface(
                                    color = if (selectedContractorId == null) CarbonBlue10 else CarbonGray10,
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, if (selectedContractorId == null) CarbonBlue60 else CarbonGray30),
                                    modifier = Modifier.clickable { viewModel.selectContractor(0L) }
                                ) {
                                    Text("General / Direct", fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            items(contractors) { c ->
                                val isSelected = c.id == selectedContractorId
                                Surface(
                                    color = if (isSelected) CarbonBlue10 else CarbonGray10,
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, if (isSelected) CarbonBlue60 else CarbonGray30),
                                    modifier = Modifier.clickable { viewModel.selectContractor(c.id) }
                                ) {
                                    Text(
                                        text = "👷 ${c.name}",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) CarbonBlue60 else CarbonGray100,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        }

        // Legacy inline entry controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CarbonStepHeader(stepNumber = "4", title = "ENTER MEASUREMENTS & DIMENSIONS")

                // Numbers (Nos)
                CarbonInputField(
                    label = "NUMBERS / MULTIPLIER (NOS)",
                    value = formState.nos,
                    onValueChange = { viewModel.updateFormNos(it) },
                    placeholder = "1",
                    keyboardType = KeyboardType.Decimal,
                    testTag = "input_dim_nos"
                )

                // 3-Column Dimensions: Length x Width x Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CarbonDimensionBox(
                        label = "LENGTH (L)",
                        value = formState.length,
                        onValueChange = { viewModel.updateFormLength(it) },
                        placeholder = "0.00",
                        unit = "m",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        testTag = "input_dim_length"
                    )

                    CarbonDimensionBox(
                        label = "WIDTH (W)",
                        value = formState.width,
                        onValueChange = { viewModel.updateFormWidth(it) },
                        placeholder = "0.00",
                        unit = "m",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        testTag = "input_dim_width"
                    )

                    CarbonDimensionBox(
                        label = "HEIGHT (H)",
                        value = formState.height,
                        onValueChange = { viewModel.updateFormHeight(it) },
                        placeholder = "0.00",
                        unit = "m",
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        testTag = "input_dim_height"
                    )
                }

                // Deduction field
                CarbonDimensionBox(
                    label = "DEDUCTION / OPENING VOIDS (-)",
                    value = formState.deduction,
                    onValueChange = { viewModel.updateFormDeduction(it) },
                    placeholder = "0.00",
                    unit = currentItem?.unit ?: "m³",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "input_dim_deduction"
                )
            }
        }

        // STEP 5: LIVE CALCULATED QUANTITY TILE
        item {
            Surface(
                color = CarbonBlue10,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, CarbonBlue60),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALCULATED QUANTITY:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonBlue60
                        )
                        Text(
                            text = "${"%.3f".format(liveQuantity)} ${currentItem?.unit ?: ""}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = CarbonBlue60
                        )
                    }

                    // Equation breakdown
                    val l = formState.length.toDoubleOrNull() ?: 0.0
                    val w = formState.width.toDoubleOrNull() ?: 0.0
                    val h = formState.height.toDoubleOrNull() ?: 0.0
                    val n = formState.nos.toDoubleOrNull() ?: 1.0
                    val ded = formState.deduction.toDoubleOrNull() ?: 0.0

                    val formulaText = buildString {
                        append("${n.toInt()} nos")
                        if (l > 0) append(" × ${l}m")
                        if (w > 0) append(" × ${w}m")
                        if (h > 0) append(" × ${h}m")
                        if (ded > 0) append(" - ${ded} ded")
                        append(" = ${"%.3f".format(liveQuantity)} ${currentItem?.unit ?: ""}")
                    }

                    Text(
                        text = formulaText,
                        fontSize = 11.sp,
                        color = CarbonGray80,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // STEP 6: DUPLICATE TO OTHER FLOORS
        if (floors.size > 1) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CarbonStepHeader(stepNumber = "5", title = "DUPLICATE ENTRY TO OTHER FLOORS")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(floors.filter { it.id != selectedFloorId }) { f ->
                            val isChecked = selectedDuplicationFloors.contains(f.id)
                            FilterChip(
                                selected = isChecked,
                                onClick = {
                                    if (isChecked) selectedDuplicationFloors.remove(f.id)
                                    else selectedDuplicationFloors.add(f.id)
                                },
                                label = { Text(f.name, fontSize = 11.sp) },
                                leadingIcon = if (isChecked) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                } else null,
                                shape = RoundedCornerShape(2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ACTION BUTTONS
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (selectedDuplicationFloors.isNotEmpty()) {
                            viewModel.saveMeasurementWithFloorDuplication(selectedDuplicationFloors.toList()) {
                                selectedDuplicationFloors.clear()
                            }
                        } else {
                            viewModel.saveMeasurementRow { saved ->
                                onMeasurementSaved(saved)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_measurement"),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedDuplicationFloors.isNotEmpty())
                            "SAVE & DUPLICATE ACROSS ${selectedDuplicationFloors.size + 1} FLOORS"
                        else
                            "SAVE MEASUREMENT RECORD",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                        color = CarbonWhite
                    )
                }
            }
        }
    }

    // Add Floor Dialog
    if (showAddFloorDialog) {
        var floorName by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showAddFloorDialog = false }) {
            Surface(
                shape = RoundedCornerShape(2.dp),
                color = CarbonWhite,
                border = BorderStroke(1.dp, CarbonGray30),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Add Floor Level", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    CarbonInputField(
                        label = "FLOOR LEVEL NAME",
                        value = floorName,
                        onValueChange = { floorName = it },
                        placeholder = "e.g. 3rd Floor, Basement 1, Podium",
                        keyboardType = KeyboardType.Text
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddFloorDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                if (floorName.isNotBlank() && currentProject != null) {
                                    viewModel.addFloor(floorName.trim())
                                    showAddFloorDialog = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                        ) {
                            Text("Add Floor", color = CarbonWhite)
                        }
                    }
                }
            }
        }
    }
}
