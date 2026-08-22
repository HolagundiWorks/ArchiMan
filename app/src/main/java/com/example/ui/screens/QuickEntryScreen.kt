package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickEntryScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val components by viewModel.components.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val selectedItemId by viewModel.selectedItemId.collectAsStateWithLifecycle()
    val selectedFloorId by viewModel.selectedFloorId.collectAsStateWithLifecycle()
    val selectedComponentId by viewModel.selectedComponentId.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    var showProjectDialog by remember { mutableStateOf(false) }
    var showNewFloorDialog by remember { mutableStateOf(false) }
    var newFloorName by remember { mutableStateOf("") }

    // Multi-floor duplication selection state
    val targetFloorIds = remember { mutableStateListOf<Long>() }

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()
    val currentItem = items.firstOrNull { it.id == selectedItemId } ?: items.firstOrNull()
    val currentFloor = floors.firstOrNull { it.id == selectedFloorId } ?: floors.firstOrNull()

    // Auto select defaults if needed
    LaunchedEffect(currentProject?.id) {
        if (currentProject != null && selectedProjectId == null) {
            viewModel.selectProject(currentProject.id)
        }
    }
    LaunchedEffect(currentFloor?.id) {
        if (currentFloor != null && selectedFloorId == null) {
            viewModel.selectFloor(currentFloor.id)
        }
    }

    val currentQuantity = viewModel.computeQuantity(currentItem, formState)

    // Camera / Photo launcher
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.updateForm(photoUri = tempPhotoUri.toString())
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.updateForm(photoUri = uri.toString())
        }
    }

    fun launchCamera() {
        try {
            val file = File(context.cacheDir, "measure_photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            photoPickerLauncher.launch("image/*")
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CarbonWhite)
    ) {
        // Carbon Header Bar
        Surface(
            color = CarbonWhite,
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = CarbonGray20,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
        ) {
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(CarbonGray100, shape = RoundedCornerShape(2.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "AMB",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = CarbonWhite
                        )
                    }
                    Column {
                        Text(
                            text = "Accelerated Measurement Book",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray100,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Fast Structural Quantity Calculator",
                            fontSize = 11.sp,
                            color = CarbonBlue60,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // M-Book Button
                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray30),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("btn_goto_mbook")
                    ) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = CarbonBlue60,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "M-Book",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarbonGray100
                        )
                    }

                    // Camera Photo
                    IconButton(
                        onClick = { launchCamera() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (formState.photoUri != null) CarbonGreen10 else CarbonGray10,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .border(
                                1.dp,
                                if (formState.photoUri != null) CarbonGreen60 else CarbonGray30,
                                RoundedCornerShape(2.dp)
                            )
                            .testTag("btn_capture_photo")
                    ) {
                        Icon(
                            imageVector = if (formState.photoUri != null) Icons.Default.Check else Icons.Default.CameraAlt,
                            contentDescription = "Photo",
                            tint = if (formState.photoUri != null) CarbonGreen60 else CarbonGray80,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Scrollable Form Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Saved Notification Banner
            AnimatedVisibility(
                visible = formState.lastSavedSummary != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = CarbonGreen10,
                    shape = RoundedCornerShape(2.dp),
                    border = BorderStroke(1.dp, CarbonGreen60),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CarbonGreen60,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Recorded: ${formState.lastSavedSummary}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = CarbonGreen70,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.updateForm() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = CarbonGreen70, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // ==========================================
            // STEP 1: SELECT PROJECT
            // ==========================================
            CarbonStepHeader(stepNumber = "1", title = "Select Project")

            Surface(
                color = CarbonGray10,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, CarbonGray20),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProjectDialog = true }
                    .testTag("step1_project_selector")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ACTIVE PROJECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray70,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentProject?.name ?: "Tap to select project",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray100
                        )
                        if (!currentProject?.siteLocation.isNullOrBlank()) {
                            Text(
                                text = "Site: ${currentProject?.siteLocation} • Client: ${currentProject?.client ?: "N/A"}",
                                fontSize = 12.sp,
                                color = CarbonGray70
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(CarbonBlue10, RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CarbonBlue60
                        )
                    }
                }
            }

            // ==========================================
            // STEP 2: SELECT WORK ITEM
            // ==========================================
            CarbonStepHeader(stepNumber = "2", title = "Select Work Item")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items.forEach { item ->
                        val isSelected = item.id == selectedItemId
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) CarbonBlue60 else CarbonGray10,
                            border = BorderStroke(1.dp, if (isSelected) CarbonBlue70 else CarbonGray30),
                            modifier = Modifier
                                .clickable { viewModel.selectItem(item.id) }
                                .testTag("chip_item_${item.name.replace(" ", "_")}")
                        ) {
                            Text(
                                text = "${item.name} (${item.unit})",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CarbonWhite else CarbonGray100,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Active item formula hint
                currentItem?.let { item ->
                    Surface(
                        color = CarbonBlue10,
                        shape = RoundedCornerShape(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Formula: ${item.calculationType.displayName}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonBlue70
                            )
                            Text(
                                text = "Unit: ${item.unit}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarbonBlue70
                            )
                        }
                    }
                }
            }

            // ==========================================
            // STEP 3: SELECT FLOOR LEVEL
            // ==========================================
            CarbonStepHeader(stepNumber = "3", title = "Select Floor Level")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    floors.forEach { floor ->
                        val isSelected = floor.id == selectedFloorId
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (isSelected) CarbonGray100 else CarbonGray10,
                            border = BorderStroke(1.dp, if (isSelected) CarbonBlack else CarbonGray30),
                            modifier = Modifier
                                .clickable { viewModel.selectFloor(floor.id) }
                                .testTag("chip_floor_${floor.name.replace(" ", "_")}")
                        ) {
                            Text(
                                text = floor.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) CarbonWhite else CarbonGray100,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    // + Add New Floor Button
                    Surface(
                        shape = RoundedCornerShape(2.dp),
                        color = CarbonBlue10,
                        border = BorderStroke(1.dp, CarbonBlue60),
                        modifier = Modifier
                            .clickable { showNewFloorDialog = true }
                            .testTag("btn_add_floor_quick")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CarbonBlue60, modifier = Modifier.size(14.dp))
                            Text("+ New Floor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarbonBlue60)
                        }
                    }
                }
            }

            // ==========================================
            // STEP 4: SELECT COMPONENT / MEMBER
            // ==========================================
            CarbonStepHeader(stepNumber = "4", title = "Select Component / Member")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (components.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        components.forEach { comp ->
                            val isSelected = comp.id == selectedComponentId
                            Surface(
                                shape = RoundedCornerShape(2.dp),
                                color = if (isSelected) CarbonBlue10 else CarbonGray10,
                                border = BorderStroke(1.dp, if (isSelected) CarbonBlue60 else CarbonGray30),
                                modifier = Modifier
                                    .clickable {
                                        viewModel.selectComponent(comp.id)
                                        viewModel.updateForm(
                                            description = comp.name,
                                            length = if (comp.length > 0) comp.length.toString() else formState.length,
                                            width = if (comp.width > 0) comp.width.toString() else formState.width,
                                            height = if (comp.height > 0) comp.height.toString() else formState.height
                                        )
                                    }
                                    .testTag("chip_comp_${comp.name.replace(" ", "_")}")
                            ) {
                                Text(
                                    text = comp.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CarbonBlue70 else CarbonGray100,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Quick component presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Beams Grid (B1-B8)", "Floor Slab S1", "Outer Wall 230mm", "Internal Wall 115mm", "Columns (C1-C8)", "Floor Screed & Tiles").forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = CarbonWhite,
                            border = BorderStroke(1.dp, CarbonGray30),
                            modifier = Modifier.clickable {
                                viewModel.updateForm(description = preset)
                            }
                        ) {
                            Text(
                                text = "+ $preset",
                                fontSize = 11.sp,
                                color = CarbonGray70,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // STEP 5: ENTER DESCRIPTION & MEASUREMENTS
            // ==========================================
            CarbonStepHeader(stepNumber = "5", title = "Enter Description & Measurements")

            Surface(
                color = CarbonWhite,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, CarbonGray20),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Description field
                    CarbonInputField(
                        label = "Description / Location Details",
                        value = formState.description,
                        onValueChange = { viewModel.updateForm(description = it) },
                        placeholder = "e.g. Beam B1-B4 between Col 1-6 / Living Room North Wall",
                        keyboardType = KeyboardType.Text,
                        testTag = "input_measurement_desc"
                    )

                    // Dimension Input Grid (Nos x L x W x D)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CarbonDimensionBox(
                            label = "Nos (N)",
                            value = formState.nos,
                            onValueChange = { viewModel.updateForm(nos = it) },
                            placeholder = "1",
                            unit = "nos",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.weight(1f),
                            testTag = "input_nos"
                        )
                        CarbonDimensionBox(
                            label = "Length (L)",
                            value = formState.length,
                            onValueChange = { viewModel.updateForm(length = it) },
                            placeholder = "0.00",
                            unit = "m",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            testTag = "input_length"
                        )
                        CarbonDimensionBox(
                            label = "Width (W/B)",
                            value = formState.width,
                            onValueChange = { viewModel.updateForm(width = it) },
                            placeholder = "0.00",
                            unit = "m",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            testTag = "input_width"
                        )
                        CarbonDimensionBox(
                            label = "Height (H/D)",
                            value = formState.height,
                            onValueChange = { viewModel.updateForm(height = it) },
                            placeholder = "0.00",
                            unit = "m",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            testTag = "input_height"
                        )
                    }

                    // Deduction & Remarks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CarbonDimensionBox(
                            label = "Deductions (D)",
                            value = formState.deduction,
                            onValueChange = { viewModel.updateForm(deduction = it) },
                            placeholder = "0.00",
                            unit = currentItem?.unit ?: "unit",
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f),
                            testTag = "input_deduction"
                        )
                        CarbonInputField(
                            label = "Remarks (Optional)",
                            value = formState.remarks,
                            onValueChange = { viewModel.updateForm(remarks = it) },
                            placeholder = "e.g. Drawing Rev 2",
                            keyboardType = KeyboardType.Text,
                            modifier = Modifier.weight(1f),
                            testTag = "input_remarks"
                        )
                    }

                    // Live Carbon Quantity Calculation Tile
                    Surface(
                        color = CarbonBlue10,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonBlue60),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CALCULATED QUANTITY",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonBlue70,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = when (currentItem?.calculationType) {
                                        CalculationType.RUNNING_LENGTH -> "Nos (${formState.nos}) × Length (${formState.length.ifBlank { "0" }})"
                                        CalculationType.AREA -> "Nos × L (${formState.length.ifBlank { "0" }}) × W (${formState.width.ifBlank { "0" }})"
                                        CalculationType.WALL_PLASTER -> "Nos × L (${formState.length.ifBlank { "0" }}) × H (${formState.height.ifBlank { "0" }})" + if (formState.deduction.isNotBlank()) " - Ded" else ""
                                        CalculationType.VOLUME, null -> "Nos × L × W × D (${formState.length.ifBlank { "0" }} × ${formState.width.ifBlank { "0" }} × ${formState.height.ifBlank { "0" }})"
                                        CalculationType.NOS -> "Nos count"
                                    },
                                    fontSize = 11.sp,
                                    color = CarbonGray70
                                )
                            }

                            Text(
                                text = "${String.format(Locale.getDefault(), "%.3f", currentQuantity)} ${currentItem?.unit ?: ""}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = CarbonBlue60
                            )
                        }
                    }
                }
            }

            // =======================================================
            // STEP 6: OPTION TO DUPLICATE THE ENTRY FOR OTHER FLOORS
            // =======================================================
            CarbonStepHeader(stepNumber = "6", title = "Duplicate to Other Floors (Optional)")

            Surface(
                color = if (targetFloorIds.isNotEmpty()) CarbonBlue10 else CarbonGray10,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, if (targetFloorIds.isNotEmpty()) CarbonBlue60 else CarbonGray20),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = if (targetFloorIds.isNotEmpty()) CarbonBlue60 else CarbonGray70,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Duplicate Entry to Additional Floors:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CarbonGray100
                            )
                        }

                        // Select All / Clear Toggle
                        TextButton(
                            onClick = {
                                val otherFloors = floors.filter { it.id != selectedFloorId }.map { it.id }
                                if (targetFloorIds.containsAll(otherFloors)) {
                                    targetFloorIds.clear()
                                } else {
                                    targetFloorIds.clear()
                                    targetFloorIds.addAll(otherFloors)
                                }
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                if (targetFloorIds.isNotEmpty()) "Clear All" else "Select All Floors",
                                fontSize = 11.sp,
                                color = CarbonBlue60,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val otherFloors = floors.filter { it.id != selectedFloorId }
                    if (otherFloors.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            otherFloors.forEach { f ->
                                val isChecked = targetFloorIds.contains(f.id)
                                Surface(
                                    shape = RoundedCornerShape(2.dp),
                                    color = if (isChecked) CarbonBlue60 else CarbonWhite,
                                    border = BorderStroke(1.dp, if (isChecked) CarbonBlue70 else CarbonGray30),
                                    modifier = Modifier
                                        .clickable {
                                            if (isChecked) targetFloorIds.remove(f.id) else targetFloorIds.add(f.id)
                                        }
                                        .testTag("checkbox_duplicate_floor_${f.name.replace(" ", "_")}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isChecked) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = CarbonWhite, modifier = Modifier.size(14.dp))
                                        }
                                        Text(
                                            text = f.name,
                                            fontSize = 12.sp,
                                            fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isChecked) CarbonWhite else CarbonGray100
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Add more floors to your project to duplicate typical floor measurements.",
                            fontSize = 11.sp,
                            color = CarbonGray70
                        )
                    }

                    if (targetFloorIds.isNotEmpty()) {
                        Text(
                            text = "💡 Will record on ${currentFloor?.name ?: "Selected Floor"} + duplicate to ${targetFloorIds.size} additional floor(s) (Total ${targetFloorIds.size + 1} records)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = CarbonBlue70
                        )
                    }
                }
            }

            // ==========================================
            // ACTION BUTTONS (Pure Quantity Workflow)
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (targetFloorIds.isNotEmpty()) {
                    Button(
                        onClick = {
                            viewModel.saveMeasurementWithFloorDuplication(
                                targetFloorIds = targetFloorIds.toList(),
                                onComplete = {
                                    targetFloorIds.clear()
                                }
                            )
                        },
                        shape = RoundedCornerShape(2.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CarbonBlue60,
                            contentColor = CarbonWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_save_duplicate_floors")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Save & Duplicate to ${targetFloorIds.size + 1} Floors (${String.format(Locale.getDefault(), "%.3f", currentQuantity * (targetFloorIds.size + 1))} ${currentItem?.unit ?: ""})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Standard Single Save Button
                Button(
                    onClick = { viewModel.saveMeasurementRow() },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (targetFloorIds.isEmpty()) CarbonBlue60 else CarbonGray100,
                        contentColor = CarbonWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_save_measurement")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Save Measurement (${String.format(Locale.getDefault(), "%.3f", currentQuantity)} ${currentItem?.unit ?: ""})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.saveMeasurementRow {
                                viewModel.repeatFormValues()
                            }
                        },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray30),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_save_add_next")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = CarbonGray100, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save & Add Next", fontSize = 12.sp, color = CarbonGray100, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.updateForm(
                                description = "",
                                length = "",
                                width = "",
                                height = "",
                                nos = "1",
                                deduction = "",
                                remarks = ""
                            )
                            targetFloorIds.clear()
                        },
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonGray30),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_clear_form")
                    ) {
                        Text("Clear Inputs", fontSize = 12.sp, color = CarbonGray70)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Project Selection Dialog
    if (showProjectDialog) {
        AlertDialog(
            onDismissRequest = { showProjectDialog = false },
            shape = RoundedCornerShape(2.dp),
            containerColor = CarbonWhite,
            title = { Text("Select Project", fontWeight = FontWeight.Bold, color = CarbonGray100) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    projects.forEach { p ->
                        Surface(
                            shape = RoundedCornerShape(2.dp),
                            color = if (p.id == selectedProjectId) CarbonBlue10 else CarbonGray10,
                            border = BorderStroke(1.dp, if (p.id == selectedProjectId) CarbonBlue60 else CarbonGray20),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectProject(p.id)
                                    showProjectDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = p.name,
                                        fontWeight = if (p.id == selectedProjectId) FontWeight.Bold else FontWeight.Normal,
                                        color = if (p.id == selectedProjectId) CarbonBlue70 else CarbonGray100
                                    )
                                    if (p.siteLocation.isNotBlank()) {
                                        Text(p.siteLocation, fontSize = 11.sp, color = CarbonGray70)
                                    }
                                }
                                if (p.id == selectedProjectId) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = CarbonBlue60)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProjectDialog = false }) {
                    Text("Close", color = CarbonBlue60, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Add Floor Dialog
    if (showNewFloorDialog) {
        AlertDialog(
            onDismissRequest = { showNewFloorDialog = false },
            shape = RoundedCornerShape(2.dp),
            containerColor = CarbonWhite,
            title = { Text("Add Floor Level", fontWeight = FontWeight.Bold, color = CarbonGray100) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter name for the new floor / level:", fontSize = 12.sp, color = CarbonGray70)
                    OutlinedTextField(
                        value = newFloorName,
                        onValueChange = { newFloorName = it },
                        placeholder = { Text("e.g. 2nd Floor / Terrace Floor") },
                        shape = RoundedCornerShape(2.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFloorName.isNotBlank()) {
                            viewModel.addFloor(newFloorName)
                            newFloorName = ""
                            showNewFloorDialog = false
                        }
                    },
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60)
                ) {
                    Text("Add Floor", color = CarbonWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFloorDialog = false }) {
                    Text("Cancel", color = CarbonGray70)
                }
            }
        )
    }
}

// ====================================================================
// Pure Carbon Design System Helper Composables
// ====================================================================

@Composable
fun CarbonStepHeader(stepNumber: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(CarbonBlue60, shape = RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = CarbonWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CarbonGray100,
            letterSpacing = 0.2.sp
        )
    }
}

@Composable
fun CarbonInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, fontSize = 11.sp, color = CarbonGray70, fontWeight = FontWeight.Medium)
        Surface(
            color = CarbonGray10,
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, CarbonGray30),
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    color = CarbonGray100,
                    fontWeight = FontWeight.Medium
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = SolidColor(CarbonBlue60),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp)
                    .testTag(testTag),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, fontSize = 13.sp, color = CarbonGray50)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun CarbonDimensionBox(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    unit: String,
    keyboardType: KeyboardType,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        color = CarbonGray10,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, CarbonGray30),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label, fontSize = 10.sp, color = CarbonGray70, fontWeight = FontWeight.Bold)
                Text(text = unit, fontSize = 9.sp, color = CarbonGray50)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CarbonGray100
                ),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                singleLine = true,
                cursorBrush = SolidColor(CarbonBlue60),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(testTag),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, fontSize = 16.sp, color = CarbonGray50)
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
