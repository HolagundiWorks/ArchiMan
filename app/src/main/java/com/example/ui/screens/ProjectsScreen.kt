package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ContractorEntity
import com.example.data.local.entity.ProjectEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    var showAddProjectDialog by remember { mutableStateOf(false) }
    var showAddContractorDialogForProject by remember { mutableStateOf<ProjectEntity?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SleekBgLight,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SleekOutline)
                            .clickable { viewModel.navigateTo(AppScreen.HOME) }
                            .testTag("btn_back_home"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = SleekTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Projects & Contractors",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(SleekPrimaryContainer)
                        .clickable { showAddProjectDialog = true }
                        .testTag("btn_add_project_top"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Project", tint = SleekPrimaryBlue, modifier = Modifier.size(20.dp))
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddProjectDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Project", fontWeight = FontWeight.Bold) },
                containerColor = SleekPrimaryBlue,
                contentColor = Color.White,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.testTag("fab_add_project")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            if (projects.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SleekSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = SleekTextTertiary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "No projects created yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = SleekTextPrimary
                            )
                            Button(
                                onClick = { showAddProjectDialog = true },
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryBlue),
                                modifier = Modifier.testTag("btn_create_first_project")
                            ) {
                                Text("+ Create First Project")
                            }
                        }
                    }
                }
            } else {
                items(projects, key = { it.id }) { project ->
                    val isSelected = project.id == selectedProjectId
                    val projectContractors = contractors.filter { it.projectId == project.id }
                    val projectMeasurements = measurements.filter { it.projectId == project.id }
                    val projectTotalAmount = projectMeasurements.sumOf { it.amount }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SleekSurfaceLight,
                        shadowElevation = if (isSelected) 2.dp else 1.dp,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) SleekPrimaryBlue else SleekOutline
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("card_project_${project.id}")
                    ) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = project.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = SleekTextPrimary
                                        )
                                        if (isSelected) {
                                            Surface(
                                                color = SleekPrimaryContainer,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "ACTIVE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = SleekOnPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (project.client.isNotBlank()) {
                                        Text(
                                            text = "Client: ${project.client}",
                                            fontSize = 12.sp,
                                            color = SleekTextSecondary
                                        )
                                    }
                                    if (project.siteLocation.isNotBlank()) {
                                        Text(
                                            text = "Site: ${project.siteLocation}",
                                            fontSize = 12.sp,
                                            color = SleekTextSecondary
                                        )
                                    }
                                }

                                if (!isSelected) {
                                    FilledTonalButton(
                                        onClick = { viewModel.selectProject(project.id) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = SleekPrimaryContainer,
                                            contentColor = SleekOnPrimaryContainer
                                        ),
                                        modifier = Modifier.testTag("btn_select_project_${project.id}")
                                    ) {
                                        Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Divider(color = SleekDivider, thickness = 1.dp)

                            // Contractor list under this project
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ASSIGNED CONTRACTORS (${projectContractors.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekTextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                TextButton(
                                    onClick = { showAddContractorDialogForProject = project },
                                    modifier = Modifier.testTag("btn_add_contractor_proj_${project.id}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = SleekPrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Contractor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SleekPrimaryBlue)
                                }
                            }

                            if (projectContractors.isEmpty()) {
                                Text(
                                    text = "No contractors assigned. Tap '+ Add Contractor' above.",
                                    fontSize = 12.sp,
                                    color = SleekTextTertiary,
                                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    projectContractors.forEach { contractor ->
                                        Surface(
                                            color = SleekSurfaceVariant,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, SleekOutlineVariant),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Icon(
                                                        imageVector = Icons.Default.Engineering,
                                                        contentDescription = null,
                                                        tint = SleekPrimaryBlue,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Column {
                                                        Text(
                                                            text = contractor.name,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp,
                                                            color = SleekTextPrimary
                                                        )
                                                        if (contractor.phone.isNotBlank()) {
                                                            Text(
                                                                text = "📞 ${contractor.phone}",
                                                                fontSize = 11.sp,
                                                                color = SleekTextSecondary
                                                            )
                                                        }
                                                    }
                                                }

                                                IconButton(
                                                    onClick = { viewModel.deleteContractor(contractor) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete contractor",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Summary row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${projectMeasurements.size} Measurements Logged",
                                    fontSize = 11.sp,
                                    color = SleekTextSecondary
                                )
                                Text(
                                    text = "Total: ₹${String.format(java.util.Locale.getDefault(), "%,.2f", projectTotalAmount)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SleekPrimaryBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Project Dialog
    if (showAddProjectDialog) {
        var projName by remember { mutableStateOf("") }
        var clientName by remember { mutableStateOf("") }
        var siteLoc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddProjectDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurfaceLight,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "New Project Setup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    OutlinedTextField(
                        value = projName,
                        onValueChange = { projName = it },
                        label = { Text("Project Name *") },
                        placeholder = { Text("e.g. Greenfield Heights") },
                        modifier = Modifier.fillMaxWidth().testTag("input_project_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Client Name") },
                        placeholder = { Text("e.g. Apex Corp") },
                        modifier = Modifier.fillMaxWidth().testTag("input_client_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = siteLoc,
                        onValueChange = { siteLoc = it },
                        label = { Text("Site Location") },
                        placeholder = { Text("e.g. Sector 12, Plot 4") },
                        modifier = Modifier.fillMaxWidth().testTag("input_site_location"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddProjectDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (projName.isNotBlank()) {
                                    viewModel.addProject(projName.trim(), clientName.trim(), siteLoc.trim())
                                    showAddProjectDialog = false
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryBlue),
                            modifier = Modifier.testTag("btn_confirm_add_project")
                        ) {
                            Text("Create Project")
                        }
                    }
                }
            }
        }
    }

    // Add Contractor Dialog
    if (showAddContractorDialogForProject != null) {
        val targetProject = showAddContractorDialogForProject!!
        var contractorName by remember { mutableStateOf("") }
        var contractorPhone by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddContractorDialogForProject = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SleekSurfaceLight,
                border = BorderStroke(1.dp, SleekOutline),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Add Contractor to ${targetProject.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SleekTextPrimary
                    )
                    OutlinedTextField(
                        value = contractorName,
                        onValueChange = { contractorName = it },
                        label = { Text("Contractor / Agency Name *") },
                        placeholder = { Text("e.g. Sharma Constructions") },
                        modifier = Modifier.fillMaxWidth().testTag("input_contractor_name"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = contractorPhone,
                        onValueChange = { contractorPhone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. 9876543210") },
                        modifier = Modifier.fillMaxWidth().testTag("input_contractor_phone"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddContractorDialogForProject = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (contractorName.isNotBlank()) {
                                    viewModel.addContractor(
                                        projectId = targetProject.id,
                                        name = contractorName.trim(),
                                        phone = contractorPhone.trim()
                                    )
                                    showAddContractorDialogForProject = null
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SleekPrimaryBlue),
                            modifier = Modifier.testTag("btn_confirm_add_contractor")
                        ) {
                            Text("Add Contractor")
                        }
                    }
                }
            }
        }
    }
}
