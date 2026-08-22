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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.*

enum class ProjectTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    CONTRACTORS("Contractors", Icons.Default.Engineering),
    MEASUREMENT_BOOK("Measurement Book", Icons.Default.MenuBook),
    RECORD_MEASURE("Record Measurement", Icons.Default.Straighten)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    viewModel: SiteViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val allMeasurements by viewModel.measurements.collectAsStateWithLifecycle()

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()
    val projectMeasurements = remember(allMeasurements, currentProject) {
        if (currentProject != null) allMeasurements.filter { it.projectId == currentProject.id }
        else emptyList()
    }
    val projectContractors = remember(contractors, currentProject) {
        if (currentProject != null) contractors.filter { it.projectId == currentProject.id }
        else emptyList()
    }

    var selectedTab by remember { mutableStateOf(ProjectTab.RECORD_MEASURE) }
    var showSwitchProjectDialog by remember { mutableStateOf(false) }
    var filterContractorForMBook by remember { mutableStateOf<Long?>(null) }

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
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(36.dp)
                                .background(CarbonGray10, shape = RoundedCornerShape(2.dp))
                                .testTag("btn_back_home")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to Projects",
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
                                text = currentProject?.siteLocation?.ifBlank { "Project Hub" } ?: "Construction Site",
                                fontSize = 11.sp,
                                color = CarbonGray70
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                        IconButton(
                            onClick = {
                                if (projectMeasurements.isNotEmpty()) {
                                    ExportHelper.printMeasurementSheetPdf(
                                        context = context,
                                        projectName = currentProject?.name ?: "Project",
                                        measurements = projectMeasurements
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(CarbonGray10, shape = RoundedCornerShape(2.dp))
                                .testTag("btn_quick_pdf_print")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Print M-Book",
                                tint = CarbonGray80,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("ENTRIES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CarbonGray60)
                                Text("${projectMeasurements.size} Recorded", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarbonGray100)
                            }
                            Column {
                                Text("CONTRACTORS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CarbonGray60)
                                Text("${projectContractors.size} Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarbonGray100)
                            }
                            Column {
                                Text("FLOORS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CarbonGray60)
                                Text("${floors.size} Levels", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarbonGray100)
                            }
                        }

                        Surface(
                            color = CarbonBlue10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonBlue60)
                        ) {
                            Text(
                                text = "ACTIVE SITE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonBlue60,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // 3 Segmented Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = CarbonWhite,
                    contentColor = CarbonBlue60,
                    divider = {
                        Divider(color = CarbonGray20, thickness = 1.dp)
                    }
                ) {
                    ProjectTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) CarbonBlue60 else CarbonGray70
                                    )
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) CarbonBlue60 else CarbonGray70
                                    )
                                }
                            },
                            modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                        )
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
            when (selectedTab) {
                ProjectTab.RECORD_MEASURE -> {
                    ProjectRecordMeasurementTab(
                        viewModel = viewModel,
                        currentProject = currentProject,
                        floors = floors,
                        contractors = projectContractors,
                        items = items,
                        onMeasurementSaved = {
                            // Optionally switch or stay on entry
                        }
                    )
                }
                ProjectTab.MEASUREMENT_BOOK -> {
                    ProjectMeasurementBookTab(
                        viewModel = viewModel,
                        currentProject = currentProject,
                        floors = floors,
                        contractors = projectContractors,
                        items = items,
                        measurements = projectMeasurements,
                        preselectedContractorId = filterContractorForMBook,
                        onClearContractorFilter = { filterContractorForMBook = null },
                        onNavigateToRecord = { selectedTab = ProjectTab.RECORD_MEASURE }
                    )
                }
                ProjectTab.CONTRACTORS -> {
                    ProjectContractorsTab(
                        viewModel = viewModel,
                        currentProject = currentProject,
                        contractors = projectContractors,
                        measurements = projectMeasurements,
                        onViewContractorMeasurements = { contractorId ->
                            filterContractorForMBook = contractorId
                            selectedTab = ProjectTab.MEASUREMENT_BOOK
                        }
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
                    Divider(color = CarbonGray20)
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
}

// ====================================================================
// TAB 1: CONTRACTORS LIST
// ====================================================================

@Composable
fun ProjectContractorsTab(
    viewModel: SiteViewModel,
    currentProject: ProjectEntity?,
    contractors: List<ContractorEntity>,
    measurements: List<MeasurementEntity>,
    onViewContractorMeasurements: (Long) -> Unit
) {
    val context = LocalContext.current
    var showAddContractorDialog by remember { mutableStateOf(false) }
    var contractorToDelete by remember { mutableStateOf<ContractorEntity?>(null) }

    Scaffold(
        containerColor = CarbonWhite,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddContractorDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("+ Add Contractor", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
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

                            Divider(color = CarbonGray20, thickness = 1.dp)

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
                    Divider(color = CarbonGray20)

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
                        contractorToDelete?.let { viewModel.deleteContractor(it) }
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
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = CarbonGray60, modifier = Modifier.size(32.dp))
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
                                    if (m.length > 0) append("${m.length}m ")
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
            title = { Text("Delete Measurement Entry?", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("Are you sure you want to delete this recorded measurement (${measurementToDelete?.itemName} - ${"%.3f".format(measurementToDelete?.quantity)} ${measurementToDelete?.unit})?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        measurementToDelete?.let { viewModel.deleteMeasurement(it) }
                        measurementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonRed60),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
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

    val currentItem = items.firstOrNull { it.id == selectedItemId } ?: items.firstOrNull()
    val currentFloor = floors.firstOrNull { it.id == selectedFloorId } ?: floors.firstOrNull()
    val currentContractor = contractors.firstOrNull { it.id == selectedContractorId }

    // Multi-floor duplication selection
    val selectedDuplicationFloors = remember { mutableStateListOf<Long>() }
    var showAddFloorDialog by remember { mutableStateOf(false) }

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

        // STEP 1: SELECT WORK ITEM
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CarbonStepHeader(stepNumber = "1", title = "SELECT WORK ITEM")

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(items) { itemEntity ->
                        val isSelected = itemEntity.id == selectedItemId
                        Surface(
                            color = if (isSelected) CarbonBlue10 else CarbonGray10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, if (isSelected) CarbonBlue60 else CarbonGray30),
                            modifier = Modifier.clickable { viewModel.selectItem(itemEntity.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = itemEntity.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CarbonBlue60 else CarbonGray100
                                )
                                Text(
                                    text = "(${itemEntity.unit})",
                                    fontSize = 10.sp,
                                    color = if (isSelected) CarbonBlue60 else CarbonGray60
                                )
                            }
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
                    CarbonStepHeader(stepNumber = "2", title = "SELECT FLOOR LEVEL")
                    Text(
                        text = "+ Add Floor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonBlue60,
                        modifier = Modifier.clickable { showAddFloorDialog = true }
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(floors) { f ->
                        val isSelected = f.id == selectedFloorId
                        Surface(
                            color = if (isSelected) CarbonBlue60 else CarbonGray10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, if (isSelected) CarbonBlue60 else CarbonGray30),
                            modifier = Modifier.clickable { viewModel.selectFloor(f.id) }
                        ) {
                            Text(
                                text = f.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) CarbonWhite else CarbonGray100,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // STEP 3: COMPONENT / LOCATION & CONTRACTOR
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CarbonStepHeader(stepNumber = "3", title = "MEMBER DESCRIPTION & CONTRACTOR")

                CarbonInputField(
                    label = "STRUCTURAL MEMBER / DESCRIPTION",
                    value = formState.description,
                    onValueChange = { viewModel.updateFormDescription(it) },
                    placeholder = "e.g. Beam B1 (Grid A-C), Column C4, Living Room Wall",
                    keyboardType = KeyboardType.Text,
                    testTag = "input_dimension_description"
                )

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

        // STEP 4: ENTER DIMENSIONS GRID
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
