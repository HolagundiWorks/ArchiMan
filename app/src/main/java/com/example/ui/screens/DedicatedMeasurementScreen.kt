package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.MeasurementEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import kotlinx.coroutines.launch
import java.util.Locale

data class MeasurementEntryRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    var description: String = "",
    var lengthText: String = "",
    var heightText: String = "",
    var widthText: String = "",
    var nosText: String = "1",
    var deductionText: String = "0",
    var remarks: String = ""
) {
    val length: Double get() = lengthText.toDoubleOrNull() ?: 0.0
    val height: Double get() = heightText.toDoubleOrNull() ?: 0.0
    val width: Double get() = widthText.toDoubleOrNull() ?: 0.0
    val nos: Double get() = nosText.toDoubleOrNull() ?: 1.0
    val deduction: Double get() = deductionText.toDoubleOrNull() ?: 0.0

    fun calculateQuantity(calcType: CalculationType): Double {
        val qty = when (calcType) {
            CalculationType.VOLUME -> (length * (if (width > 0) width else 1.0) * height * nos) - deduction
            CalculationType.AREA, CalculationType.WALL_PLASTER -> (length * (if (height > 0) height else (if (width > 0) width else 1.0)) * nos) - deduction
            CalculationType.RUNNING_LENGTH -> (length * nos) - deduction
            CalculationType.NOS -> nos - deduction
        }
        return if (qty > 0) Math.round(qty * 100.0) / 100.0 else 0.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedicatedMeasurementScreen(
    viewModel: SiteViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBook: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val allQualifiedItems by viewModel.allQualifiedItems.collectAsStateWithLifecycle()

    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val contractorId by viewModel.dedicatedContractorId.collectAsStateWithLifecycle()
    val itemName by viewModel.dedicatedItemName.collectAsStateWithLifecycle()
    val itemUom by viewModel.dedicatedItemUom.collectAsStateWithLifecycle()
    val calcType by viewModel.dedicatedCalculationType.collectAsStateWithLifecycle()
    val floorId by viewModel.dedicatedFloorId.collectAsStateWithLifecycle()
    val floorName by viewModel.dedicatedFloorName.collectAsStateWithLifecycle()

    val currentProject = remember(projects, selectedProjectId) {
        projects.firstOrNull { it.id == selectedProjectId }
    }
    val currentContractor = remember(contractors, contractorId) {
        contractors.firstOrNull { it.id == contractorId }
    }

    // Rate lookup
    val unitRate = remember(contractorId, itemName, allQualifiedItems) {
        val qual = allQualifiedItems.firstOrNull { it.contractorId == contractorId && it.itemName.equals(itemName, ignoreCase = true) }
        qual?.rate ?: 0.0
    }

    // Measurement entry rows
    val rows = remember {
        mutableStateListOf(
            MeasurementEntryRow(description = "External Wall Grid A-B", lengthText = "", heightText = "", nosText = "1", deductionText = "0")
        )
    }

    // Plastering Brickwork Import Prompt State
    var showImportBrickworkPrompt by remember { mutableStateOf(false) }
    var availableBrickworkData by remember { mutableStateOf<List<MeasurementEntity>>(emptyList()) }
    var hasCheckedBrickwork by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }

    val isPlastering = remember(itemName) {
        itemName.contains("plaster", ignoreCase = true)
    }

    // Auto-check for Brickwork data on this floor if item is Plastering
    LaunchedEffect(isPlastering, selectedProjectId, floorId, floorName) {
        if (isPlastering && selectedProjectId != null && !hasCheckedBrickwork) {
            hasCheckedBrickwork = true
            val bwList = viewModel.findBrickworkMeasurements(selectedProjectId!!, floorId, floorName)
            if (bwList.isNotEmpty()) {
                availableBrickworkData = bwList
                showImportBrickworkPrompt = true
            }
        }
    }

    // Totals calculations
    val totalQuantity = remember(rows.toList(), calcType) {
        rows.sumOf { it.calculateQuantity(calcType) }
    }
    val totalAmount = remember(totalQuantity, unitRate) {
        totalQuantity * unitRate
    }

    Scaffold(
        containerColor = CarbonWhite,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CarbonGray100)
                    .statusBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_from_dedicated_measure")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CarbonWhite
                            )
                        }
                        Text(
                            text = "Record Measurement",
                            color = CarbonWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Project Tag
                    Surface(
                        color = CarbonBlue80,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = currentProject?.name ?: "Project",
                            color = CarbonWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // DEDICATED HEADER STRIP: Contractor | Item | Level
                Surface(
                    color = CarbonGray90,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Title Format: ContractorA | BrickWork230mm | lvl0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currentContractor?.name ?: "Contractor",
                                color = CarbonCyan30,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(text = "|", color = CarbonGray60, fontWeight = FontWeight.Bold)
                            Text(
                                text = itemName.ifBlank { "Item" },
                                color = CarbonYellow30,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                            Text(text = "|", color = CarbonGray60, fontWeight = FontWeight.Bold)
                            Text(
                                text = floorName.ifBlank { "Level 0" },
                                color = CarbonWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        // UOM & Rate Tag
                        Surface(
                            color = CarbonGray80,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonGray70)
                        ) {
                            Text(
                                text = if (unitRate > 0) "$itemUom • ₹${unitRate.toInt()}" else itemUom,
                                color = CarbonGray20,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Live Totals and Save Strip
            Surface(
                color = CarbonGray100,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "TOTAL QUANTITY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CarbonGray40,
                            letterSpacing = 0.5.sp
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f", totalQuantity),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = CarbonWhite
                            )
                            Text(
                                text = itemUom,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = CarbonCyan30
                            )
                            if (unitRate > 0) {
                                Text(
                                    text = "• ₹${String.format(Locale.getDefault(), "%,.0f", totalAmount)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CarbonYellow30
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                rows.add(MeasurementEntryRow(nosText = "1", deductionText = "0"))
                            },
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonGray60),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = CarbonWhite, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ Line", color = CarbonWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (isSaving) return@Button
                                isSaving = true
                                val validRows = rows.filter { it.description.isNotBlank() || it.length > 0 || it.height > 0 }
                                val projId = selectedProjectId ?: 0L
                                val contId = contractorId ?: 0L
                                val contName = currentContractor?.name ?: "Contractor"
                                val flrId = floorId
                                val flrName = floorName.ifBlank { "Floor" }

                                val toSave = validRows.map { r ->
                                    val qty = r.calculateQuantity(calcType)
                                    val amt = qty * unitRate
                                    MeasurementEntity(
                                        projectId = projId,
                                        floorId = flrId,
                                        contractorId = contId,
                                        contractorName = contName,
                                        itemId = 0L,
                                        itemName = itemName,
                                        unit = itemUom,
                                        calculationType = calcType,
                                        description = r.description.ifBlank { "$itemName Entry" },
                                        length = r.length,
                                        height = r.height,
                                        width = r.width,
                                        nos = r.nos,
                                        deduction = r.deduction,
                                        quantity = qty,
                                        rate = unitRate,
                                        amount = amt,
                                        floor = flrName,
                                        location = "$flrName - $itemName",
                                        remarks = r.remarks,
                                        date = System.currentTimeMillis()
                                    )
                                }

                                viewModel.saveBatchMeasurements(toSave) { count ->
                                    isSaving = false
                                    savedCount = count
                                    showSuccessDialog = true
                                }
                            },
                            shape = RoundedCornerShape(2.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_save_dedicated_measurements")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSaving) "Saving..." else "Save M-Book",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = CarbonWhite
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plastering Linked Brickwork Banner / Prompt
            if (isPlastering) {
                item {
                    Surface(
                        color = CarbonBlue10,
                        shape = RoundedCornerShape(2.dp),
                        border = BorderStroke(1.dp, CarbonBlue60),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoFixHigh,
                                        contentDescription = null,
                                        tint = CarbonBlue60,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Plastering Linked to Brickwork",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = CarbonBlue80
                                    )
                                }

                                if (availableBrickworkData.isNotEmpty()) {
                                    Surface(
                                        color = CarbonBlue60,
                                        shape = RoundedCornerShape(2.dp)
                                    ) {
                                        Text(
                                            text = "${availableBrickworkData.size} Brickwork rows found on $floorName",
                                            color = CarbonWhite,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Plaster surface measurements correspond to brick masonry walls on this floor. Would you like to import brickwork dimensions?",
                                fontSize = 12.sp,
                                color = CarbonGray90
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val bwList = if (availableBrickworkData.isNotEmpty()) {
                                                availableBrickworkData
                                            } else {
                                                viewModel.findBrickworkMeasurements(selectedProjectId ?: 0L, floorId, floorName)
                                            }

                                            if (bwList.isNotEmpty()) {
                                                rows.clear()
                                                bwList.forEach { bw ->
                                                    rows.add(
                                                        MeasurementEntryRow(
                                                            description = bw.description.ifBlank { "Plaster over ${bw.itemName}" },
                                                            lengthText = if (bw.length > 0) bw.length.toString() else "",
                                                            heightText = if (bw.height > 0) bw.height.toString() else "",
                                                            nosText = if (bw.nos > 0) bw.nos.toString() else "1",
                                                            deductionText = if (bw.deduction > 0) bw.deduction.toString() else "0",
                                                            remarks = "Imported from ${bw.itemName} (${bw.contractorName})"
                                                        )
                                                    )
                                                }
                                                importStatusMessage = "Successfully imported ${bwList.size} brickwork measurements into plastering. You can adjust lines as needed."
                                            } else {
                                                importStatusMessage = "No brickwork records found on $floorName. You can enter plastering dimensions manually."
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(2.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_import_brickwork_data")
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Import Brickwork Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (availableBrickworkData.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = {
                                            // Manual entry without import
                                            showImportBrickworkPrompt = false
                                        },
                                        shape = RoundedCornerShape(2.dp),
                                        border = BorderStroke(1.dp, CarbonBlue60),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Manual Entry", fontSize = 12.sp, color = CarbonBlue60)
                                    }
                                }
                            }

                            if (importStatusMessage != null) {
                                Text(
                                    text = importStatusMessage ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = CarbonGreen80
                                )
                            }
                        }
                    }
                }
            }

            // Table Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MEASUREMENT ENTRIES (${rows.size} LINES)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CarbonGray80,
                        letterSpacing = 0.5.sp
                    )

                    TextButton(
                        onClick = {
                            rows.add(MeasurementEntryRow(nosText = "1", deductionText = "0"))
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = CarbonBlue60)
                        Spacer(Modifier.width(4.dp))
                        Text("Add Line", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CarbonBlue60)
                    }
                }
            }

            // Measurement Entry Rows (Description | Length | Height | NextLine)
            itemsIndexed(rows, key = { _, item -> item.id }) { index, row ->
                MeasurementRowCard(
                    index = index + 1,
                    row = row,
                    calcType = calcType,
                    itemUom = itemUom,
                    onUpdateRow = { updated ->
                        rows[index] = updated
                    },
                    onDelete = {
                        if (rows.size > 1) {
                            rows.removeAt(index)
                        } else {
                            rows[0] = MeasurementEntryRow(nosText = "1", deductionText = "0")
                        }
                    },
                    onAddNextLine = {
                        rows.add(index + 1, MeasurementEntryRow(nosText = "1", deductionText = "0"))
                    }
                )
            }

            item {
                Button(
                    onClick = {
                        rows.add(MeasurementEntryRow(nosText = "1", deductionText = "0"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonGray10),
                    border = BorderStroke(1.dp, CarbonGray30)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = CarbonGray90, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("+ Add Next Line (Description, Length, Height)", color = CarbonGray90, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Success Saved Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CarbonGreen60, modifier = Modifier.size(40.dp))
            },
            title = {
                Text("Measurements Recorded", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$savedCount measurement lines saved successfully into the Measurement Book.")
                    Text("Total Quantity: ${String.format(Locale.getDefault(), "%.2f", totalQuantity)} $itemUom", fontWeight = FontWeight.Bold)
                    if (unitRate > 0) {
                        Text("Total Amount: ₹${String.format(Locale.getDefault(), "%,.0f", totalAmount)}", fontWeight = FontWeight.Bold, color = CarbonBlue60)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToBook()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CarbonBlue60),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("View Measurement Book", color = CarbonWhite)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    },
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text("Project Hub", color = CarbonGray100)
                }
            },
            containerColor = CarbonWhite,
            shape = RoundedCornerShape(4.dp)
        )
    }
}

@Composable
fun MeasurementRowCard(
    index: Int,
    row: MeasurementEntryRow,
    calcType: CalculationType,
    itemUom: String,
    onUpdateRow: (MeasurementEntryRow) -> Unit,
    onDelete: () -> Unit,
    onAddNextLine: () -> Unit
) {
    var description by remember(row.description) { mutableStateOf(row.description) }
    var lengthText by remember(row.lengthText) { mutableStateOf(row.lengthText) }
    var heightText by remember(row.heightText) { mutableStateOf(row.heightText) }
    var nosText by remember(row.nosText) { mutableStateOf(row.nosText) }
    var deductionText by remember(row.deductionText) { mutableStateOf(row.deductionText) }
    var showAdvanced by remember { mutableStateOf(row.deduction > 0 || (row.nos != 1.0 && row.nos > 0)) }

    val rowQty = row.calculateQuantity(calcType)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("measurement_row_$index"),
        shape = RoundedCornerShape(2.dp),
        colors = CardDefaults.cardColors(containerColor = CarbonWhite),
        border = BorderStroke(1.dp, if (rowQty > 0) CarbonBlue60 else CarbonGray30),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Line Number & Row Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = CarbonGray90,
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = "Line #$index",
                            color = CarbonWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (rowQty > 0) {
                        Surface(
                            color = CarbonGreen10,
                            shape = RoundedCornerShape(2.dp),
                            border = BorderStroke(1.dp, CarbonGreen60)
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.2f", rowQty)} $itemUom",
                                color = CarbonGreen80,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = { showAdvanced = !showAdvanced },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text(if (showAdvanced) "Hide Nos/Ded" else "+ Nos/Ded", fontSize = 11.sp, color = CarbonBlue60)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete line", tint = CarbonRed60, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    onUpdateRow(row.copy(description = it))
                },
                placeholder = { Text("Description (e.g. North Wall / Bedroom 1)", fontSize = 12.sp) },
                label = { Text("DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_row_desc_$index"),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CarbonBlue60,
                    unfocusedBorderColor = CarbonGray40
                )
            )

            // Flow inputs: Length | Height | (Nos / Deduction if expanded)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Length
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = {
                        lengthText = it
                        onUpdateRow(row.copy(lengthText = it))
                    },
                    placeholder = { Text("0.00", fontSize = 12.sp) },
                    label = { Text("LENGTH (m)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_row_length_$index"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CarbonBlue60,
                        unfocusedBorderColor = CarbonGray40
                    )
                )

                // Height
                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        heightText = it
                        onUpdateRow(row.copy(heightText = it))
                    },
                    placeholder = { Text("0.00", fontSize = 12.sp) },
                    label = { Text("HEIGHT (m)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_row_height_$index"),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CarbonBlue60,
                        unfocusedBorderColor = CarbonGray40
                    )
                )

                if (!showAdvanced) {
                    // Quick next line button on the right
                    IconButton(
                        onClick = onAddNextLine,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .size(36.dp)
                            .background(CarbonGray10, shape = RoundedCornerShape(2.dp)),
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardReturn,
                            contentDescription = "Next Line",
                            tint = CarbonBlue60,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Advanced Nos & Deduction inputs if toggled
            if (showAdvanced) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = nosText,
                        onValueChange = {
                            nosText = it
                            onUpdateRow(row.copy(nosText = it))
                        },
                        placeholder = { Text("1", fontSize = 12.sp) },
                        label = { Text("NOS (Multiplier)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deductionText,
                        onValueChange = {
                            deductionText = it
                            onUpdateRow(row.copy(deductionText = it))
                        },
                        placeholder = { Text("0.00", fontSize = 12.sp) },
                        label = { Text("DEDUCTION ($itemUom)", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }
    }
}
