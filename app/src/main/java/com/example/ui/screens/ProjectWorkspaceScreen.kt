@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import com.example.ui.icons.CarbonIcons

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
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
    val dailySiteReports by viewModel.dailySiteReports.collectAsStateWithLifecycle()
    val projectDecisions by viewModel.projectDecisions.collectAsStateWithLifecycle()
    val siteIssues by viewModel.siteIssues.collectAsStateWithLifecycle()
    val projectConsultants by viewModel.projectConsultants.collectAsStateWithLifecycle()
    val coordinationItems by viewModel.coordinationItems.collectAsStateWithLifecycle()
    val projectDrawings by viewModel.projectDrawings.collectAsStateWithLifecycle()
    val drawingRevisions by viewModel.drawingRevisions.collectAsStateWithLifecycle()
    val drawingTransmittals by viewModel.drawingTransmittals.collectAsStateWithLifecycle()
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
        ProjectSection.DECISIONS -> "Decisions"
        ProjectSection.COORDINATION -> "Coordination"
        ProjectSection.CONTROLS -> "Onboarding & controls"
        ProjectSection.CONTRACTORS -> "Project team"
    }
    val navigateUp: () -> Unit = {
        when (selectedSection) {
            ProjectSection.OVERVIEW -> onNavigateBack()
            ProjectSection.MORE, ProjectSection.BRIEF_SCOPE, ProjectSection.PLANNING -> viewModel.setProjectSection(ProjectSection.OVERVIEW)
            else -> viewModel.setProjectSection(ProjectSection.MORE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentProject?.name ?: "Select project", maxLines = 1)
                        Text(
                            listOf(sectionTitle, currentProject?.siteLocation.orEmpty()).filter(String::isNotBlank).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp, modifier = Modifier.testTag("btn_back_home")) {
                        Icon(CarbonIcons.ArrowBack, contentDescription = if (selectedSection == ProjectSection.OVERVIEW) "Back to projects" else "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSwitchProjectDialog = true }, modifier = Modifier.testTag("btn_switch_project")) {
                        Icon(CarbonIcons.SwapHoriz, contentDescription = "Switch project")
                    }
                }
            )
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
                    reportCount = meetingMinutes.size + siteInspections.size + dailySiteReports.size + siteIssues.size,
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
                    reportCount = meetingMinutes.size + siteInspections.size + dailySiteReports.size + siteIssues.size,
                    decisionCount = projectDecisions.count { it.status != "DECIDED" },
                    coordinationCount = coordinationItems.count { !com.example.domain.CoordinationWorkflow.isClosed(it.status) },
                    contractorCount = projectContractors.size,
                    controlCount = approvals.count { it.status != "APPROVED" } + backlog.count { it.status == "OPEN" },
                    onSelect = viewModel::setProjectSection
                )
                ProjectSection.DRAWINGS -> ProjectSecondarySection("Drawings") {
                    DrawingRegisterScreen(viewModel, projectDrawings, drawingRevisions, drawingTransmittals)
                }
                ProjectSection.REPORTS -> ProjectSecondarySection("Site reports") {
                    ProjectReportsScreen(viewModel, meetingMinutes, siteInspections, dailySiteReports, siteIssues)
                }
                ProjectSection.DECISIONS -> ProjectSecondarySection("Decisions") {
                    ProjectDecisionsScreen(viewModel, projectDecisions)
                }
                ProjectSection.COORDINATION -> ProjectSecondarySection("Coordination") {
                    CoordinationScreen(
                        viewModel = viewModel,
                        consultants = projectConsultants,
                        records = coordinationItems,
                        drawings = projectDrawings,
                        revisions = drawingRevisions
                    )
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
            }
        }
    }

    // Switch Project Dialog
    if (showSwitchProjectDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchProjectDialog = false },
            title = { Text("Switch project") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(projects) { project ->
                        ListItem(
                            headlineContent = { Text(project.name) },
                            supportingContent = { Text(project.siteLocation) },
                            trailingContent = { if (project.id == currentProject?.id) Icon(CarbonIcons.Check, contentDescription = "Selected") },
                            modifier = Modifier.clickable {
                                viewModel.selectProject(project.id)
                                showSwitchProjectDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSwitchProjectDialog = false }) { Text("Close") }
            }
        )
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
    decisionCount: Int,
    coordinationCount: Int,
    contractorCount: Int,
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
            Text("Documents, field records and project setup", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Text("CORE WORKFLOW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp) }
        item { ProjectMoreRow("Project brief & scope", "Requirements, site data, deliverables and responsibilities", CarbonIcons.FactCheck) { onSelect(ProjectSection.BRIEF_SCOPE) } }
        item { ProjectMoreRow("Planning", "Tasks, schedules and material selections", CarbonIcons.EventNote) { onSelect(ProjectSection.PLANNING) } }
        item { ProjectMoreRow("Onboarding & controls", "$controlCount open approvals and backlog actions", CarbonIcons.Rule) { onSelect(ProjectSection.CONTROLS) } }
        item { Text("DOCUMENTS & SITE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 8.dp)) }
        item { ProjectMoreRow("Drawings", "$drawingCount registered drawings, revisions and transmittals", CarbonIcons.Architecture) { onSelect(ProjectSection.DRAWINGS) } }
        item { ProjectMoreRow("Site reports", "$reportCount daily, snag, NCR, inspection and meeting records", CarbonIcons.Assignment) { onSelect(ProjectSection.REPORTS) } }
        item { ProjectMoreRow("Decisions", "$decisionCount decisions awaiting closure", CarbonIcons.Gavel) { onSelect(ProjectSection.DECISIONS) } }
        item { ProjectMoreRow("Coordination", "$coordinationCount open RFIs, submittals or instructions", CarbonIcons.SyncAlt) { onSelect(ProjectSection.COORDINATION) } }
        item { Text("PROJECT TEAM", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
        item { ProjectMoreRow("Project team", "$contractorCount assigned contractors", CarbonIcons.Engineering) { onSelect(ProjectSection.CONTRACTORS) } }
    }
}

@Composable
private fun ProjectMoreRow(
    title: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supportingText) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(CarbonIcons.ChevronRight, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@Composable
private fun ProjectSecondarySection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        HorizontalDivider()
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
            Text("Capture the agreed design intent before drawings and site work proceed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small) {
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
                    Icon(if (siteDataExpanded) CarbonIcons.ExpandLess else CarbonIcons.ExpandMore, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (siteDataExpanded) "Hide site data" else "Add / review site data")
                }
                if (siteDataExpanded) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Site data", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("Record supplied or observed information. ArchiMan does not certify legal or planning compliance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Text("Scope, deliverables, exclusions and responsibilities", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = { showAddScope = true }) {
                    Icon(CarbonIcons.Add, null)
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
        if (scopeItems.isEmpty()) {
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Text("No scope items yet. Add the agreed inclusions before work begins.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            items(scopeItems, key = { it.id }) { item ->
                Surface(border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surface) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = item.status == "COMPLETE", onCheckedChange = { viewModel.toggleProjectScopeItem(item) })
                        Column(Modifier.weight(1f)) {
                            Text(item.category.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(item.title, fontWeight = FontWeight.SemiBold)
                            if (item.details.isNotBlank()) Text(item.details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.status.replace('_', ' '), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.deleteProjectScopeItem(item) }) { Icon(CarbonIcons.DeleteOutline, "Delete scope item") }
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
                Text("PROJECT HUB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.7.sp)
                Text(project?.name ?: "Project overview", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("Brief, plan and coordinate the selected project.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onEditProfile)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(CarbonIcons.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Project profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            listOfNotNull(
                                project?.projectCode?.takeIf(String::isNotBlank),
                                project?.projectType?.takeIf(String::isNotBlank),
                                project?.status?.takeIf(String::isNotBlank)
                            ).joinToString(" • ").ifBlank { "Add project code, type, dates and architect" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Icon(CarbonIcons.Edit, contentDescription = "Edit project profile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text("CONTINUE PROJECT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProjectHubActionCard("Brief", briefStatus.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }, CarbonIcons.FactCheck, onOpenBrief, Modifier.weight(1f))
                ProjectHubActionCard("Planning", "$taskCount tasks • $scheduleCount events • $selectionCount selections", CarbonIcons.EventNote, onOpenPlanning, Modifier.weight(1f))
            }
        }
        item {
            Text("AT A GLANCE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceAround) {
                    ProjectMetric("MEASUREMENTS", measurementCount.toString())
                    ProjectMetric("DRAWINGS", drawingCount.toString())
                    ProjectMetric("SITE REPORTS", reportCount.toString())
                }
            }
        }
        item {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenMore)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(CarbonIcons.MoreHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("All project tools", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Drawings, site reports and project team", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    }
                    Icon(CarbonIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ProjectMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(supportingText, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Icon(CarbonIcons.TaskAlt, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("No project tasks", fontWeight = FontWeight.Bold)
                Text("Add site actions, decisions, or follow-ups.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        } else LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = task.status == "DONE", onCheckedChange = { viewModel.toggleProjectTask(task) })
                        Column(Modifier.weight(1f)) {
                            Text(task.title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            if (task.description.isNotBlank()) Text(task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.deleteProjectTask(task) }) { Icon(CarbonIcons.DeleteOutline, "Delete task") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(CarbonIcons.Add, "Add task")
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
                Text("Quantity-only procurement schedule", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            TextButton(enabled = selectionItems.isNotEmpty(), onClick = {
                ExportHelper.exportSelectionListAsPurchaseOrder(context, project?.name ?: "Project", selectionItems)
            }) { Icon(CarbonIcons.FileDownload, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Export PO") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (selectionItems.isEmpty()) Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(CarbonIcons.Checklist, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp)); Text("No selection items", fontWeight = FontWeight.Bold)
                Text("Add materials, fixtures, finishes, or equipment.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
            } else LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(selectionItems, key = { it.id }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.Top) {
                            Checkbox(checked = item.status == "SELECTED", onCheckedChange = { viewModel.toggleProjectSelectionItem(item) })
                            Column(Modifier.weight(1f)) {
                                Text(item.itemName, fontWeight = FontWeight.Bold)
                                Text("${item.quantity} ${item.unit}${item.makeOrBrand.takeIf(String::isNotBlank)?.let { " • $it" } ?: ""}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                if (item.specification.isNotBlank()) Text(item.specification, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteProjectSelectionItem(item) }) { Icon(CarbonIcons.DeleteOutline, "Delete selection item") }
                        }
                    }
                }
            }
            FloatingActionButton(onClick = { showAdd = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(CarbonIcons.Add, "Add selection item") }
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
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small) {
            Column(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(CarbonIcons.TableRows, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Text("Measurement Sheet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "All project, room, and component shortcuts now use the same spreadsheet-style measurement editor.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { viewModel.openCanonicalMeasurement() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(CarbonIcons.Straighten, contentDescription = null)
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
        containerColor = MaterialTheme.colorScheme.surface,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddContractorDialog = true },
                icon = { Icon(CarbonIcons.Add, contentDescription = null) },
                text = { Text("Add Contractor", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = MaterialTheme.shapes.small,
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
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = CarbonIcons.Engineering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No contractors added yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Add civil, RCC, masonry, plaster, or finishing contractors to assign them to measurements.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddContractorDialog = true },
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(CarbonIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
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
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                            .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CarbonIcons.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = contractor.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (contractor.phone.isNotBlank()) {
                                            Text(
                                                text = "📞 ${contractor.phone}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { contractorToDelete = contractor },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = CarbonIcons.DeleteOutline,
                                        contentDescription = "Delete Contractor",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${contractorEntries.size} Recorded Entries • ${"%.2f".format(totalQty)} Total Qty",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                                imageVector = CarbonIcons.Phone,
                                                contentDescription = "Call",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    OutlinedButton(
                                        onClick = { onViewContractorMeasurements(contractor.id) },
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("View M-Book", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

        BasicAlertDialog(onDismissRequest = { showAddContractorDialog = false }) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text("ASSIGN EXISTING CONTRACTORS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (availableExisting.isEmpty()) {
                        Text("All registered contractors are already assigned to this site.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(availableExisting, key = { it.id }) { existing ->
                                val selected = existing.id in selectedExistingIds
                                Surface(
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                                    shape = MaterialTheme.shapes.small,
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
                                            Text(existing.contactNo.ifBlank { existing.phone }, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Assign ${selectedExistingIds.size} Selected")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text("OR CREATE A NEW CONTRACTOR", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (error != null) {
                        Text(text = error ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedTextField(
                        label = { Text("CONTRACTOR / AGENCY NAME *") },
                        value = name,
                        onValueChange = { name = it; error = null },
                        placeholder = { Text("e.g. Ramesh Civil Works") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().testTag("input_contractor_name"),
                        singleLine = true)

                    OutlinedTextField(
                        label = { Text("PHONE / CONTACT NUMBER") },
                        value = phone,
                        onValueChange = { phone = it },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("input_contractor_phone"),
                        singleLine = true)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddContractorDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
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
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.surface)
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { contractorToDelete = null }) { Text("Cancel") }
            },
            shape = MaterialTheme.shapes.small
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
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // PDF
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
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
                                    Icon(CarbonIcons.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                    Text("PDF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            // Excel XLS
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary),
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
                                    Icon(CarbonIcons.TableChart, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
                                    Text("Excel XLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }

                            // CSV
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
                                    Icon(CarbonIcons.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                    Text("CSV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Search & Filter Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search measurements") },
                    placeholder = { Text("Member, item or floor") },
                    leadingIcon = { Icon(CarbonIcons.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) {
                            Icon(CarbonIcons.Close, contentDescription = "Clear search")
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                            label = { Text("All Floors", style = MaterialTheme.typography.labelSmall) },
                            shape = MaterialTheme.shapes.small
                        )
                    }

                    // Specific floors
                    items(floors) { f ->
                        FilterChip(
                            selected = selectedFloorFilter == f.id,
                            onClick = { selectedFloorFilter = if (selectedFloorFilter == f.id) null else f.id },
                            label = { Text(f.name, style = MaterialTheme.typography.labelSmall) },
                            shape = MaterialTheme.shapes.small
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
                                label = { Text("👷 ${cont?.name ?: "Contractor"}", style = MaterialTheme.typography.labelSmall) },
                                trailingIcon = { Icon(CarbonIcons.Close, contentDescription = null, modifier = Modifier.size(12.dp)) },
                                shape = MaterialTheme.shapes.small
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
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "Total Qty: ${"%.3f".format(totalCalculatedQuantity)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Measurement Rows
        if (filteredMeasurements.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                        Icon(CarbonIcons.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Text("No recorded measurements found", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("Switch to 'Record Measurement' tab to enter dimensions and calculate quantities.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Button(
                            onClick = onNavigateToRecord,
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(
                                        text = m.unit,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { measurementToDelete = m },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(CarbonIcons.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Description / Location
                        Text(
                            text = listOfNotNull(m.floor.ifBlank { null }, m.location.ifBlank { null }, m.description.ifBlank { null }).joinToString(" • "),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )

                        // Dimension Calculation Box
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "= ${"%.3f".format(m.quantity)} ${m.unit}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
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
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }
                            Text(
                                text = formattedDate,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Archive", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { measurementToDelete = null }) { Text("Cancel") }
            },
            shape = MaterialTheme.shapes.small
        )
    }
}
