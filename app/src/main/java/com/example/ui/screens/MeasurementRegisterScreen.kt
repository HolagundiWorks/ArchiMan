@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import com.example.ui.icons.CarbonIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.MeasurementEntity
import com.example.ui.navigation.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementRegisterScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val editingMeasurement by viewModel.editingMeasurement.collectAsStateWithLifecycle()

    var filterContractorId by remember { mutableStateOf<Long?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPhotoUrl by remember { mutableStateOf<String?>(null) }

    val currentProject = projects.firstOrNull { it.id == selectedProjectId }
    val sdf = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    val filteredMeasurements = measurements.filter { m ->
        (selectedProjectId == null || m.projectId == selectedProjectId) &&
        (filterContractorId == null || m.contractorId == filterContractorId) &&
        (searchQuery.isBlank() || m.itemName.contains(searchQuery, ignoreCase = true) ||
                m.contractorName.contains(searchQuery, ignoreCase = true) ||
                m.floor.contains(searchQuery, ignoreCase = true) ||
                m.location.contains(searchQuery, ignoreCase = true) ||
                m.remarks.contains(searchQuery, ignoreCase = true))
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Material 3 screen header
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
                            .background(MaterialTheme.colorScheme.outline)
                            .clickable { viewModel.navigateTo(AppScreen.HOME) }
                            .testTag("btn_back_home"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CarbonIcons.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Measurement Register",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "${filteredMeasurements.size} measurement entries",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline)
                            .clickable {
                                ExportHelper.exportAndShareCsv(
                                    context = context,
                                    projectName = currentProject?.name ?: "All Projects",
                                    measurements = filteredMeasurements
                                )
                            }
                            .testTag("btn_export_csv"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(CarbonIcons.Share, contentDescription = "Share CSV", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.outline)
                            .clickable {
                                ExportHelper.printMeasurementSheetPdf(
                                    context = context,
                                    projectName = currentProject?.name ?: "All Projects",
                                    measurements = filteredMeasurements
                                )
                            }
                            .testTag("btn_print_register"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(CarbonIcons.Print, contentDescription = "Print PDF", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCanonicalMeasurement() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("fab_new_measurement")
            ) {
                Icon(CarbonIcons.Add, contentDescription = "New Measurement")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search item, floor, location, contractor...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(CarbonIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(CarbonIcons.Clear, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("input_search_register"),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            // Contractor Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAllSelected = filterContractorId == null
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isAllSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { filterContractorId = null }
                ) {
                    Text(
                        text = "All Contractors",
                        fontSize = 12.sp,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                contractors.forEach { c ->
                    val isSelected = filterContractorId == c.id
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .clickable { filterContractorId = c.id }
                            .testTag("filter_chip_contractor_${c.id}")
                    ) {
                        Text(
                            text = c.name,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Measurement List
            if (filteredMeasurements.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CarbonIcons.SquareFoot,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "No measurements found",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.openCanonicalMeasurement() },
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_empty_add")
                        ) {
                            Text("+ Add First Measurement")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
                ) {
                    items(filteredMeasurements, key = { it.id }) { item ->
                        MeasurementCard(
                            measurement = item,
                            dateFormat = sdf,
                            onClick = { viewModel.startEditingMeasurement(item) },
                            onPhotoClick = { if (item.photoUri != null) selectedPhotoUrl = item.photoUri }
                        )
                    }
                }
            }
        }
    }

    // Edit Measurement Dialog
    if (editingMeasurement != null) {
        EditMeasurementDialog(
            measurement = editingMeasurement!!,
            onDismiss = { viewModel.stopEditingMeasurement() },
            onSave = { updated -> viewModel.updateMeasurement(updated) },
            onDelete = { viewModel.deleteMeasurement(editingMeasurement!!) }
        )
    }

    // Fullscreen Photo Viewer Dialog
    if (selectedPhotoUrl != null) {
        BasicAlertDialog(onDismissRequest = { selectedPhotoUrl = null }) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.Black,
                modifier = Modifier.fillMaxWidth().height(400.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = selectedPhotoUrl,
                        contentDescription = "Measurement site photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { selectedPhotoUrl = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(CarbonIcons.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementCard(
    measurement: MeasurementEntity,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onPhotoClick: () -> Unit
) {
    val dimString = when (measurement.calculationType) {
        CalculationType.RUNNING_LENGTH -> "L: ${measurement.length} × ${measurement.nos}"
        CalculationType.AREA -> "L: ${measurement.length} × W: ${measurement.width} × ${measurement.nos}"
        CalculationType.WALL_PLASTER -> "L: ${measurement.length} × H: ${measurement.height} × ${measurement.nos}" + (if (measurement.deduction > 0) " (-${measurement.deduction})" else "")
        CalculationType.VOLUME -> "L: ${measurement.length} × W: ${measurement.width} × H: ${measurement.height} × ${measurement.nos}"
        CalculationType.NOS -> "Nos: ${measurement.nos}"
    }
    val locationStr = listOf(measurement.floor, measurement.location).filter { it.isNotBlank() }.joinToString(" - ")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("measurement_row_${measurement.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Badge
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.width(48.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dateFormat.format(Date(measurement.date)),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Main Info
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = measurement.itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• ${measurement.contractorName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = dimString,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (locationStr.isNotBlank() || measurement.remarks.isNotBlank()) {
                    Text(
                        text = listOf(locationStr, measurement.remarks).filter { it.isNotBlank() }.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Measured quantity
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.2f", measurement.quantity)} ${measurement.unit}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Photo indicator
            if (measurement.photoUri != null) {
                IconButton(
                    onClick = onPhotoClick,
                    modifier = Modifier.size(28.dp).testTag("btn_view_photo_${measurement.id}")
                ) {
                    Icon(
                        imageVector = CarbonIcons.PhotoCamera,
                        contentDescription = "Photo attached",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeasurementDialog(
    measurement: MeasurementEntity,
    onDismiss: () -> Unit,
    onSave: (MeasurementEntity) -> Unit,
    onDelete: () -> Unit
) {
    var length by remember { mutableStateOf(measurement.length.toString()) }
    var width by remember { mutableStateOf(measurement.width.toString()) }
    var height by remember { mutableStateOf(measurement.height.toString()) }
    var nos by remember { mutableStateOf(measurement.nos.toString()) }
    var deduction by remember { mutableStateOf(measurement.deduction.toString()) }
    var floor by remember { mutableStateOf(measurement.floor) }
    var location by remember { mutableStateOf(measurement.location) }
    var remarks by remember { mutableStateOf(measurement.remarks) }

    val l = length.toDoubleOrNull() ?: 0.0
    val w = width.toDoubleOrNull() ?: 0.0
    val h = height.toDoubleOrNull() ?: 0.0
    val n = nos.toDoubleOrNull() ?: 1.0
    val d = deduction.toDoubleOrNull() ?: 0.0

    val calcQty = when (measurement.calculationType) {
        CalculationType.RUNNING_LENGTH -> l * n
        CalculationType.AREA -> l * w * n
        CalculationType.WALL_PLASTER -> (l * h * n) - d
        CalculationType.VOLUME -> l * w * h * n
        CalculationType.NOS -> n
    }
    val roundedQty = Math.round(calcQty * 100.0) / 100.0

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Edit Measurement",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(CarbonIcons.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text(
                    text = "${measurement.itemName} • ${measurement.contractorName}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                // Editable dimensions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (measurement.calculationType != CalculationType.NOS) {
                        OutlinedTextField(
                            value = length,
                            onValueChange = { length = it },
                            label = { Text("Length") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                    }
                    if (measurement.calculationType == CalculationType.AREA || measurement.calculationType == CalculationType.VOLUME) {
                        OutlinedTextField(
                            value = width,
                            onValueChange = { width = it },
                            label = { Text("Width") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                    }
                    if (measurement.calculationType == CalculationType.WALL_PLASTER || measurement.calculationType == CalculationType.VOLUME) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = { height = it },
                            label = { Text("Height") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = nos,
                        onValueChange = { nos = it },
                        label = { Text("Nos") },
                        modifier = Modifier.width(65.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }

                if (measurement.calculationType == CalculationType.WALL_PLASTER) {
                    OutlinedTextField(
                        value = deduction,
                        onValueChange = { deduction = it },
                        label = { Text("Deduction (m²)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }

                // Live total preview
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Quantity: $roundedQty ${measurement.unit}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = floor,
                        onValueChange = { floor = it },
                        label = { Text("Floor") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(CarbonIcons.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Archive sheet")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = MaterialTheme.shapes.large
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                onSave(
                                    measurement.copy(
                                        length = l,
                                        width = w,
                                        height = h,
                                        nos = n,
                                        deduction = d,
                                        quantity = roundedQty,
                                        floor = floor,
                                        location = location,
                                        remarks = remarks
                                    )
                                )
                            },
                            shape = MaterialTheme.shapes.large,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_save_edit_measurement")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
