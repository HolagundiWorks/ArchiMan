package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ClientEntity
import com.example.data.local.entity.ContractorEntity
import com.example.data.local.entity.ProjectEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HomeTab
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
        HomeTab.CLIENTS -> ClientsScreen(viewModel = viewModel)
        HomeTab.CONTRACTORS -> ContractorsScreen(viewModel = viewModel)
    }
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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(CarbonGray100, shape = RoundedCornerShape(2.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "AMB",
                                color = CarbonWhite,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Column {
                            Text(
                                text = "Accelerated Measurement Book",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray100
                            )
                            Text(
                                text = "${projects.size} Active Project${if (projects.size != 1) "s" else ""} • ${measurements.size} Recorded Entries",
                                fontSize = 11.sp,
                                color = CarbonBlue60,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Quick Actions
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.MASTER_DATA) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(CarbonGray10, shape = RoundedCornerShape(2.dp))
                                .testTag("btn_top_items")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Items Library",
                                tint = CarbonGray80,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.EXPORT) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(CarbonGray10, shape = RoundedCornerShape(2.dp))
                                .testTag("btn_top_export")
                        ) {
                            Icon(
                                imageVector = Icons.Default.IosShare,
                                contentDescription = "Export & Quantities",
                                tint = CarbonGray80,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Search Box
                CarbonSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Search projects by name, client, or site address...",
                    testTag = "input_search_projects"
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateProjectDialog = true },
                containerColor = CarbonBlue60,
                contentColor = CarbonWhite,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.testTag("fab_create_project")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Project", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                            tint = CarbonGray40,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No projects found" else "No matching projects",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarbonGray80
                        )
                        Text(
                            text = "Create a new project with client & qualified contractors to start digital measurement booking.",
                            fontSize = 12.sp,
                            color = CarbonGray60,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { showCreateProjectDialog = true },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60),
                            modifier = Modifier.testTag("btn_empty_create_project")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Project", fontSize = 13.sp)
                        }
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
            contractors = contractors,
            onDismiss = { showCreateProjectDialog = false },
            onNavigateToClients = {
                showCreateProjectDialog = false
                viewModel.setHomeTab(HomeTab.CLIENTS)
            },
            onNavigateToContractors = {
                showCreateProjectDialog = false
                viewModel.setHomeTab(HomeTab.CONTRACTORS)
            },
            onCreate = { name, clientName, clientId, location, floorsList, selectedContractorIds ->
                viewModel.addProjectWithClientAndContractors(
                    name = name,
                    clientName = clientName,
                    clientId = clientId,
                    siteLocation = location,
                    floorNames = floorsList,
                    contractorIds = selectedContractorIds
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
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonRed60),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Delete", color = CarbonWhite)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { projectToDelete = null },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Cancel", color = CarbonGray100)
                }
            },
            containerColor = CarbonWhite,
            shape = RoundedCornerShape(4.dp)
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
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonWhite),
        border = BorderStroke(1.dp, CarbonGray30),
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
                            .background(CarbonBlue10, shape = RoundedCornerShape(2.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apartment,
                            contentDescription = null,
                            tint = CarbonBlue60,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = project.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray100
                        )

                        if (project.siteLocation.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = CarbonGray60,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = project.siteLocation,
                                    fontSize = 12.sp,
                                    color = CarbonGray70
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
                                    tint = CarbonGray60,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Client: ${project.client}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = CarbonGray80
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
                        tint = CarbonGray60,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Divider(color = CarbonGray20, thickness = 1.dp)

            // Metrics Strip (Recorded Measurements, Contractors, Open Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Measurement count badge
                    Surface(
                        color = if (measurementCount > 0) CarbonBlue10 else CarbonGray10,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, if (measurementCount > 0) CarbonBlue60 else CarbonGray30)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (measurementCount > 0) CarbonBlue60 else CarbonGray70,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$measurementCount Recorded",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (measurementCount > 0) CarbonBlue60 else CarbonGray70
                            )
                        }
                    }

                    // Contractor count badge
                    Surface(
                        color = CarbonGray10,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray30)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Engineering,
                                contentDescription = null,
                                tint = CarbonGray70,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "$contractorCount Contractors",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = CarbonGray80
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
                        color = CarbonBlue60
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = CarbonBlue60,
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
    contractors: List<ContractorEntity>,
    onDismiss: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToContractors: () -> Unit,
    onCreate: (name: String, clientName: String, clientId: Long, location: String, floors: List<String>, selectedContractorIds: List<Long>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<ClientEntity?>(clients.firstOrNull()) }
    val selectedContractorIds = remember { mutableStateListOf<Long>().apply { addAll(contractors.map { it.id }) } }
    var floorsText by remember { mutableStateOf("Level 0 (Ground Floor), Level 1 (1st Floor), Level 2 (2nd Floor)") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var clientDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = CarbonWhite,
            border = BorderStroke(1.dp, CarbonGray30),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Create New Project",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray100
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = CarbonGray70)
                        }
                    }
                    Divider(color = CarbonGray20, thickness = 1.dp, modifier = Modifier.padding(top = 6.dp))
                }

                if (errorMessage != null) {
                    item {
                        Surface(
                            color = CarbonRed10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonRed60),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = CarbonRed60,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }

                // Project Name
                item {
                    CarbonInputField(
                        label = "PROJECT NAME *",
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        placeholder = "e.g. ABC Residence / Emerald Heights",
                        keyboardType = KeyboardType.Text,
                        testTag = "input_new_project_name"
                    )
                }

                // Site Address
                item {
                    CarbonInputField(
                        label = "SITE ADDRESS / LOCATION *",
                        value = location,
                        onValueChange = { location = it },
                        placeholder = "e.g. Plot 42, Green Avenue, Sector 15",
                        keyboardType = KeyboardType.Text,
                        testTag = "input_new_project_location"
                    )
                }

                // Client Selector
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CLIENT / DEVELOPER *",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray70,
                                letterSpacing = 0.5.sp
                            )
                            TextButton(
                                onClick = onNavigateToClients,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("+ Create Client First", fontSize = 11.sp, color = CarbonBlue60, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (clients.isEmpty()) {
                            Surface(
                                color = CarbonYellow10,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, CarbonYellow30),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("No clients found. Always create client first.", fontSize = 11.sp, color = CarbonGray90)
                                    TextButton(onClick = onNavigateToClients) {
                                        Text("Add Client", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    border = BorderStroke(1.dp, CarbonGray40),
                                    color = CarbonWhite,
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
                                                color = CarbonGray100
                                            )
                                            if (selectedClient != null) {
                                                Text(
                                                    text = selectedClient!!.address.ifBlank { selectedClient!!.contactNo },
                                                    fontSize = 11.sp,
                                                    color = CarbonGray60
                                                )
                                            }
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = CarbonGray70)
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
                                                    Text(c.address.ifBlank { c.contactNo }, fontSize = 11.sp, color = CarbonGray60)
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

                // Contractors Multiple Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ASSIGN CONTRACTORS (Multiple Selection)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonGray70,
                                letterSpacing = 0.5.sp
                            )
                            TextButton(
                                onClick = onNavigateToContractors,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("+ New Contractor", fontSize = 11.sp, color = CarbonBlue60, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (contractors.isEmpty()) {
                            Surface(
                                color = CarbonYellow10,
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, CarbonYellow30),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("No contractors registered.", fontSize = 11.sp, color = CarbonGray90)
                                    TextButton(onClick = onNavigateToContractors) {
                                        Text("Add Contractor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                border = BorderStroke(1.dp, CarbonGray30),
                                color = CarbonGray10,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    contractors.forEach { cont ->
                                        val isSelected = selectedContractorIds.contains(cont.id)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isSelected) selectedContractorIds.remove(cont.id)
                                                    else selectedContractorIds.add(cont.id)
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    if (checked) selectedContractorIds.add(cont.id)
                                                    else selectedContractorIds.remove(cont.id)
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = CarbonBlue60)
                                            )
                                            Column {
                                                Text(cont.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CarbonGray100)
                                                Text(cont.address.ifBlank { cont.contactNo.ifBlank { cont.phone } }, fontSize = 10.sp, color = CarbonGray60)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Floor Levels
                item {
                    CarbonInputField(
                        label = "FLOOR / LEVEL NAMES (Comma-separated)",
                        value = floorsText,
                        onValueChange = { floorsText = it },
                        placeholder = "Level 0, Level 1, Level 2, Terrace",
                        keyboardType = KeyboardType.Text,
                        testTag = "input_new_project_floors"
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
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonGray40)
                        ) {
                            Text("Cancel", color = CarbonGray100, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    errorMessage = "Please enter a valid project name."
                                } else {
                                    val clientName = selectedClient?.name ?: "Client"
                                    val clientId = selectedClient?.id ?: 0L
                                    val floorsList = floorsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    onCreate(
                                        name.trim(),
                                        clientName,
                                        clientId,
                                        location.trim(),
                                        floorsList,
                                        selectedContractorIds.toList()
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_confirm_create_project"),
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                        ) {
                            Text("Create Project", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CarbonWhite)
                        }
                    }
                }
            }
        }
    }
}
