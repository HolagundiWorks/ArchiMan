@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.ContractorEntity
import com.example.data.local.entity.ProjectEntity
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.DirectorySection
import com.example.ui.navigation.HomeTab
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: SiteViewModel,
    onNavigate: (AppScreen) -> Unit = {}
) {
    val homeTab by viewModel.selectedHomeTab.collectAsStateWithLifecycle()

    when (homeTab) {
        HomeTab.PROJECTS -> ProjectsTabContent(viewModel = viewModel, onNavigate = onNavigate)
        HomeTab.DIRECTORY -> PortfolioDirectoryScreen(viewModel = viewModel)
        HomeTab.WORK_LIBRARY -> MasterDataScreen(viewModel = viewModel)
        HomeTab.PRACTICE -> PortfolioPracticeScreen(onNavigate = onNavigate)
    }
}

@Composable
private fun PortfolioDirectoryScreen(viewModel: SiteViewModel) {
    val selectedSection by viewModel.selectedDirectorySection.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSection.ordinal, containerColor = MaterialTheme.colorScheme.surface) {
            DirectorySection.values().forEach { section ->
                Tab(
                    selected = section == selectedSection,
                    onClick = { viewModel.setDirectorySection(section) },
                    text = { Text(section.label, fontWeight = if (section == selectedSection) FontWeight.Bold else FontWeight.Medium) },
                    icon = {
                        Icon(
                            if (section == DirectorySection.CLIENTS) Icons.Default.Business else Icons.Default.Engineering,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (selectedSection) {
                DirectorySection.CLIENTS -> ClientsScreen(viewModel = viewModel)
                DirectorySection.CONTRACTORS -> ContractorsScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortfolioPracticeScreen(onNavigate: (AppScreen) -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Practice", fontWeight = FontWeight.Bold)
                        Text("Company identity, backups and connections", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("PRACTICE SETUP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp) }
            item { PortfolioToolRow("Company profile & connections", "Logo, practice identity, profile backup and Supabase setup", Icons.Default.Domain) { onNavigate(AppScreen.COMPANY_PROFILE) } }
            item { Text("LOCAL ACCESS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp, modifier = Modifier.padding(top = 8.dp)) }
            item { PortfolioToolRow("Local Wi-Fi workspace", "Secure browser access with named users and controlled editing", Icons.Default.Wifi) { onNavigate(AppScreen.LOCAL_PORTAL) } }
        }
    }
}

@Composable
private fun PortfolioToolRow(
    title: String,
    supportingText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(supportingText) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    )
    HorizontalDivider()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsTabContent(
    viewModel: SiteViewModel,
    onNavigate: (AppScreen) -> Unit = {}
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<ProjectEntity?>(null) }

    val filteredProjects = remember(projects, searchQuery) {
        if (searchQuery.isBlank()) projects
        else {
            projects.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.siteLocation.contains(searchQuery, ignoreCase = true) ||
                it.client.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text("ArchiMan")
                            Text(
                                "${projects.size} projects · ${measurements.size} measurements",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search projects, clients or sites") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("input_search_projects"),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateProjectDialog = true },
                modifier = Modifier.testTag("fab_create_project").semantics { contentDescription = "Create a new project" }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New project")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filteredProjects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No projects found" else "No matching projects",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add a project to start recording measurements.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredProjects, key = { it.id }) { project ->
                        val projectMeasurements = measurements.filter { it.projectId == project.id }
                        val projectContractorsCount = contractors.count { it.projectId == project.id }
                        
                        ProjectCardItem(
                            project = project,
                            measurementCount = projectMeasurements.size,
                            contractorCount = if (projectContractorsCount > 0) projectContractorsCount else contractors.size,
                            onClick = {
                                viewModel.selectProject(project.id)
                                viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                            },
                            onDelete = {
                                projectToDelete = project
                            }
                        )
                    }
                }
            }
        }
    }

    // Create Project Dialog
    if (showCreateProjectDialog) {
        CreateProjectWithDetailsDialog(
            clients = clients,
            onDismiss = { showCreateProjectDialog = false },
            onNavigateToClients = {
                showCreateProjectDialog = false
                viewModel.setDirectorySection(DirectorySection.CLIENTS)
                viewModel.setHomeTab(HomeTab.DIRECTORY)
            },
            onCreate = { name, projectCode, clientName, clientId, projectType, location ->
                viewModel.createProject(
                    name = name,
                    projectCode = projectCode,
                    clientName = clientName,
                    clientId = clientId,
                    projectType = projectType,
                    siteLocation = location,
                ) {
                    showCreateProjectDialog = false
                    viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Project", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to delete \"${projectToDelete?.name}\"? All floors, rooms, and recorded measurements for this project will be removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { viewModel.deleteProject(it) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { projectToDelete = null },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small
        )
    }
}

@Composable
fun ProjectCardItem(
    project: ProjectEntity,
    measurementCount: Int,
    contractorCount: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("project_card_${project.id}"),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Project Name, Location, Client & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = project.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (project.siteLocation.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = project.siteLocation,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (project.client.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Client: ${project.client}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Project",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // Metrics Strip (Recorded Measurements, Contractors, Open Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Measurement count badge
                    Surface(
                        color = if (measurementCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, if (measurementCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = if (measurementCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$measurementCount Recorded",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (measurementCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Contractor count badge
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$contractorCount Contractors",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Enter button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Open Project",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CreateProjectWithDetailsDialog(
    clients: List<ClientEntity>,
    onDismiss: () -> Unit,
    onNavigateToClients: () -> Unit,
    onCreate: (
        name: String,
        projectCode: String,
        clientName: String,
        clientId: Long,
        projectType: String,
        location: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var projectCode by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<ClientEntity?>(clients.firstOrNull()) }
    var projectType by remember { mutableStateOf("Residential") }
    var customProjectType by remember { mutableStateOf("") }
    var projectTypeExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }
    val projectTypes = listOf("Residential", "Commercial", "Healthcare", "Hospitality", "Education", "Other")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "New project",
                            style = MaterialTheme.typography.titleLarge
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(top = 6.dp))
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                item {
                    Text("Project identity", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Create the project record first. Add floors and assign contractors inside the project.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    OutlinedTextField(
                        label = { Text("Project name *") },
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        placeholder = { Text("ABC Residence") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_project_name"),
                        singleLine = true)
                }

                item {
                    OutlinedTextField(
                        label = { Text("Project code") },
                        value = projectCode,
                        onValueChange = { projectCode = it },
                        placeholder = { Text("AR-2026-001") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_project_code"),
                        singleLine = true)
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Client *",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            if (clients.isNotEmpty()) {
                                TextButton(
                                    onClick = onNavigateToClients,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Add client", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (clients.isEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ListItem(
                                    headlineContent = { Text("Add a client") },
                                    supportingContent = { Text("A project must be linked to its client.") },
                                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                                    modifier = Modifier.clickable(onClick = onNavigateToClients)
                                )
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { clientDropdownExpanded = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = selectedClient?.name ?: "Select Client",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (selectedClient != null) {
                                                Text(
                                                    text = selectedClient!!.address.ifBlank { selectedClient!!.contactNo },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                DropdownMenu(
                                    expanded = clientDropdownExpanded,
                                    onDismissRequest = { clientDropdownExpanded = false }
                                ) {
                                    clients.forEach { c ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(c.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(c.address.ifBlank { c.contactNo }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            },
                                            onClick = {
                                                selectedClient = c
                                                clientDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    ExposedDropdownMenuBox(
                        expanded = projectTypeExpanded,
                        onExpandedChange = { projectTypeExpanded = !projectTypeExpanded }
                    ) {
                        OutlinedTextField(
                            value = projectType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Project type *") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(projectTypeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = projectTypeExpanded, onDismissRequest = { projectTypeExpanded = false }) {
                            projectTypes.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { projectType = option; projectTypeExpanded = false }
                                )
                            }
                        }
                    }
                }

                if (projectType == "Other") {
                    item {
                        OutlinedTextField(
                            value = customProjectType,
                            onValueChange = { customProjectType = it },
                            label = { Text("Project type name *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        label = { Text("Site address / location *") },
                        value = location,
                        onValueChange = { location = it },
                        placeholder = { Text("Plot 42, Green Avenue, Sector 15") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().testTag("input_new_project_location"),
                        minLines = 2
                    )
                }

                item {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    errorMessage = "Enter the project name."
                                } else if (location.isBlank()) {
                                    errorMessage = "Enter the site address or location."
                                } else if (selectedClient == null) {
                                    errorMessage = "Select or create a client before creating the project."
                                } else if (projectType == "Other" && customProjectType.isBlank()) {
                                    errorMessage = "Enter the project type."
                                } else {
                                    onCreate(
                                        name.trim(),
                                        projectCode.trim(),
                                        selectedClient!!.name,
                                        selectedClient!!.id,
                                        if (projectType == "Other") customProjectType.trim() else projectType,
                                        location.trim(),
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_confirm_create_project"),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Create project", fontWeight = FontWeight.Bold)
                        }
                    }
                }
        }
    }
}
