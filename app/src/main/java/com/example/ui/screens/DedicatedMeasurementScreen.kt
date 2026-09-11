package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.MeasurementEntity
import com.example.data.attachments.MeasurementAttachmentStore
import com.example.data.draft.MeasurementDraftKey
import com.example.data.draft.MeasurementDraftRow
import com.example.data.draft.MeasurementDraftStore
import com.example.domain.MeasurementInput
import com.example.domain.MeasurementUnitConverter
import com.example.domain.MeasurementUnitSystem
import com.example.domain.QuantityCalculator
import com.example.domain.MeasurementRowStatus
import com.example.domain.MeasurementRowValidation
import com.example.domain.MeasurementRowValidationInput
import com.example.domain.MeasurementRowValidator
import com.example.ui.navigation.AppScreen
import com.example.ui.viewmodel.SiteViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class MeasurementEntryRow(
    val id: String = java.util.UUID.randomUUID().toString(),
    var description: String = "",
    var lengthText: String = "",
    var heightText: String = "",
    var widthText: String = "",
    var nosText: String = "1",
    var deductionText: String = "0",
    var remarks: String = "",
    var photoUri: String? = null
) {
    val length: Double get() = lengthText.toDoubleOrNull() ?: 0.0
    val height: Double get() = heightText.toDoubleOrNull() ?: 0.0
    val width: Double get() = widthText.toDoubleOrNull() ?: 0.0
    val nos: Double get() = nosText.toDoubleOrNull() ?: 1.0
    val deduction: Double get() = deductionText.toDoubleOrNull() ?: 0.0

    fun calculateQuantity(calcType: CalculationType): Double {
        return QuantityCalculator.calculate(
            calcType,
            MeasurementInput(no = nos, length = length, breadth = width, height = height, deduction = deduction)
        )
    }
}

private data class MeasurementEditorSnapshot(
    val rows: List<MeasurementEntryRow>,
    val unitSystem: MeasurementUnitSystem
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DedicatedMeasurementScreen(
    viewModel: SiteViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBook: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val draftStore = remember(context) { MeasurementDraftStore(context) }
    val attachmentStore = remember(context) { MeasurementAttachmentStore(context) }
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
    val itemId by viewModel.dedicatedItemId.collectAsStateWithLifecycle()

    val currentProject = remember(projects, selectedProjectId) {
        projects.firstOrNull { it.id == selectedProjectId }
    }
    val currentContractor = remember(contractors, contractorId) {
        contractors.firstOrNull { it.id == contractorId }
    }

    // Measurement entry rows
    val rows = remember {
        mutableStateListOf(
            MeasurementEntryRow(lengthText = "", heightText = "", nosText = "1", deductionText = "0")
        )
    }
    val draftKey = remember(selectedProjectId, contractorId, itemId, floorId) {
        val project = selectedProjectId
        val contractor = contractorId
        val item = itemId
        val floor = floorId
        if (project != null && contractor != null && item != null && floor != null) {
            MeasurementDraftKey(project, contractor, item, floor)
        } else null
    }
    var unitSystem by remember(draftKey, itemUom) {
        mutableStateOf(MeasurementUnitConverter.infer(itemUom))
    }
    val displayUom = remember(calcType, unitSystem, itemUom) {
        MeasurementUnitConverter.unitFor(calcType, unitSystem, itemUom)
    }
    var draftLoaded by remember(draftKey) { mutableStateOf(false) }

    LaunchedEffect(draftKey) {
        draftLoaded = false
        val restored = draftKey?.let { key -> withContext(Dispatchers.IO) { draftStore.load(key) } }
        unitSystem = restored?.unitSystem?.let(MeasurementUnitSystem::fromStored)
            ?: MeasurementUnitConverter.infer(itemUom)
        rows.clear()
        rows.addAll(restored?.rows?.map { it.toEntryRow() } ?: listOf(MeasurementEntryRow(nosText = "1", deductionText = "0")))
        draftLoaded = true
    }

    LaunchedEffect(draftKey, draftLoaded) {
        val key = draftKey ?: return@LaunchedEffect
        if (!draftLoaded) return@LaunchedEffect
        snapshotFlow { rows.toList().map { it.toDraftRow() } to unitSystem }
            .collectLatest { (snapshot, system) ->
                delay(600)
                withContext(Dispatchers.IO) { draftStore.save(key, snapshot, system.name) }
            }
    }

    // Plastering Brickwork Import Prompt State
    var showImportBrickworkPrompt by remember { mutableStateOf(false) }
    var availableBrickworkData by remember { mutableStateOf<List<MeasurementEntity>>(emptyList()) }
    var hasCheckedBrickwork by remember { mutableStateOf(false) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var savedCount by remember { mutableStateOf(0) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var undoSnapshot by remember { mutableStateOf<MeasurementEditorSnapshot?>(null) }
    val selectedRowIds = remember { mutableStateListOf<String>() }
    var duplicateCopies by remember { mutableIntStateOf(1) }
    var photoTargetRowId by remember { mutableStateOf<String?>(null) }

    fun changeUnitSystem(target: MeasurementUnitSystem) {
        if (target == unitSystem) return
        undoSnapshot = MeasurementEditorSnapshot(rows.map { it.copy() }, unitSystem)
        rows.indices.forEach { index ->
            rows[index] = rows[index].convertUnits(calcType, unitSystem, target)
        }
        unitSystem = target
        saveError = null
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val targetId = photoTargetRowId
        if (uri != null && targetId != null) {
            coroutineScope.launch {
                val managedUri = runCatching { withContext(Dispatchers.IO) { attachmentStore.import(uri) } }
                val index = rows.indexOfFirst { it.id == targetId }
                if (index >= 0) managedUri.onSuccess { stored ->
                    withContext(Dispatchers.IO) { attachmentStore.deleteIfManaged(rows[index].photoUri) }
                    rows[index] = rows[index].copy(photoUri = stored)
                }.onFailure { saveError = it.message ?: "Unable to import selected photo" }
            }
        }
        photoTargetRowId = null
    }

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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Record Measurement")
                            Text(
                                currentProject?.name ?: "Project",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_from_dedicated_measure")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                )

                // DEDICATED HEADER STRIP: Contractor | Item | Level
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${currentContractor?.name ?: "Contractor"} | ${itemName.ifBlank { "Item" }} | ${floorName.ifBlank { "Level" }}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ENTRY UNITS",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                UnitSystemToggle(
                                    selected = unitSystem,
                                    onSelect = ::changeUnitSystem
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                                ) {
                                    Text(
                                        text = displayUom,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Live Totals and Save Strip
            Surface(
                color = MaterialTheme.colorScheme.onSurface,
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
                            color = MaterialTheme.colorScheme.outline,
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
                                color = MaterialTheme.colorScheme.surface
                            )
                            Text(
                                text = displayUom,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        undoSnapshot?.let { snapshot ->
                            IconButton(
                                onClick = {
                                    rows.clear()
                                    rows.addAll(snapshot.rows)
                                    unitSystem = snapshot.unitSystem
                                    undoSnapshot = null
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last row operation", tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(19.dp))
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                rows.add(MeasurementEntryRow(nosText = "1", deductionText = "0"))
                            },
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("+ Line", color = MaterialTheme.colorScheme.surface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                if (isSaving) return@Button
                                val catalogItemId = itemId
                                if (selectedProjectId == null || contractorId == null || catalogItemId == null) {
                                    saveError = "Project, contractor, floor and a catalog work item are required before saving."
                                    return@Button
                                }
                                isSaving = true
                                saveError = null
                                val validatedRows = rows.map { it to it.validation(calcType) }
                                val incomplete = validatedRows.filter { it.second.status == MeasurementRowStatus.INCOMPLETE }
                                if (incomplete.isNotEmpty()) {
                                    isSaving = false
                                    saveError = "Complete row ${rows.indexOf(incomplete.first().first) + 1}: ${incomplete.first().second.message}"
                                    return@Button
                                }
                                val validRows = validatedRows.filter { it.second.status == MeasurementRowStatus.READY }.map { it.first }
                                if (validRows.isEmpty()) {
                                    isSaving = false
                                    saveError = "Enter at least one complete measurement row."
                                    return@Button
                                }
                                val projId = selectedProjectId ?: 0L
                                val contId = contractorId ?: 0L
                                val contName = currentContractor?.name ?: "Contractor"
                                val flrId = floorId
                                val flrName = floorName.ifBlank { "Floor" }

                                val toSave = validRows.map { r ->
                                    val qty = r.calculateQuantity(calcType)
                                    MeasurementEntity(
                                        projectId = projId,
                                        floorId = flrId,
                                        contractorId = contId,
                                        contractorName = contName,
                                        itemId = catalogItemId,
                                        itemName = itemName,
                                        unit = displayUom,
                                        calculationType = calcType,
                                        description = r.description.trim().ifBlank { "$itemName Entry" },
                                        length = r.length,
                                        height = r.height,
                                        width = r.width,
                                        nos = r.nos,
                                        deduction = r.deduction,
                                        quantity = qty,
                                        floor = flrName,
                                        location = "$flrName - $itemName",
                                        remarks = r.remarks,
                                        photoUri = r.photoUri,
                                        date = System.currentTimeMillis()
                                    )
                                }

                                viewModel.saveBatchMeasurements(toSave) { count ->
                                    isSaving = false
                                    savedCount = count
                                    draftKey?.let { key -> coroutineScope.launch(Dispatchers.IO) { draftStore.clear(key) } }
                                    showSuccessDialog = true
                                }
                            },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("btn_save_dedicated_measurements")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSaving) "Saving..." else "Save M-Book",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.surface
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
            saveError?.let { message ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.error), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                        Text(message, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            // Plastering Linked Brickwork Banner / Prompt
            if (isPlastering) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Plastering Linked to Brickwork",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                if (availableBrickworkData.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = "${availableBrickworkData.size} Brickwork rows found on $floorName",
                                            color = MaterialTheme.colorScheme.surface,
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
                                color = MaterialTheme.colorScheme.onSurface
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
                                                            lengthText = bw.dimensionText(bw.length, unitSystem),
                                                            heightText = bw.dimensionText(bw.height, unitSystem),
                                                            nosText = if (bw.nos > 0) bw.nos.toString() else "1",
                                                            deductionText = bw.deductionText(calcType, unitSystem),
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
                                    shape = MaterialTheme.shapes.small,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
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
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("Manual Entry", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }

                            if (importStatusMessage != null) {
                                Text(
                                    text = importStatusMessage ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Table Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MEASUREMENT TABLE (${rows.size} ROWS)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        TextButton(
                            onClick = {
                                if (selectedRowIds.size == rows.size) selectedRowIds.clear()
                                else {
                                    selectedRowIds.clear()
                                    selectedRowIds.addAll(rows.map { it.id })
                                }
                            }
                        ) {
                            Text(if (selectedRowIds.size == rows.size) "Clear" else "Select all")
                        }
                    }

                    if (selectedRowIds.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("${selectedRowIds.size} selected", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { if (duplicateCopies > 1) duplicateCopies-- }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Remove, contentDescription = "Fewer copies")
                                }
                                Text("$duplicateCopies", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(24.dp))
                                IconButton(onClick = { if (duplicateCopies < 20) duplicateCopies++ }, modifier = Modifier.size(30.dp)) {
                                    Icon(Icons.Default.Add, contentDescription = "More copies")
                                }
                                Button(
                                    onClick = {
                                        undoSnapshot = MeasurementEditorSnapshot(rows.toList(), unitSystem)
                                        val originals = rows.filter { it.id in selectedRowIds }
                                        repeat(duplicateCopies) {
                                            rows.addAll(originals.map { it.copy(id = java.util.UUID.randomUUID().toString()) })
                                        }
                                        selectedRowIds.clear()
                                        duplicateCopies = 1
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Duplicate")
                                }
                            }
                        }
                    }
                }
            }

            // Measurement Entry Rows (Description | Length | Height | NextLine)
            itemsIndexed(rows, key = { _, item -> item.id }) { index, row ->
                MeasurementRowCard(
                    index = index + 1,
                    row = row,
                    calcType = calcType,
                    itemName = itemName,
                    itemUom = displayUom,
                    dimensionUom = if (unitSystem == MeasurementUnitSystem.METRIC) "m" else "ft",
                    selected = row.id in selectedRowIds,
                    onToggleSelected = {
                        if (row.id in selectedRowIds) selectedRowIds.remove(row.id) else selectedRowIds.add(row.id)
                    },
                    onDuplicate = {
                        undoSnapshot = MeasurementEditorSnapshot(rows.toList(), unitSystem)
                        rows.add(index + 1, row.copy(id = java.util.UUID.randomUUID().toString()))
                    },
                    onPhotoClick = {
                        photoTargetRowId = row.id
                        photoPicker.launch("image/*")
                    },
                    onRemovePhoto = {
                        coroutineScope.launch(Dispatchers.IO) { attachmentStore.deleteIfManaged(row.photoUri) }
                        rows[index] = row.copy(photoUri = null)
                    },
                    onUpdateRow = { updated ->
                        rows[index] = updated
                    },
                    onDelete = {
                        undoSnapshot = MeasurementEditorSnapshot(rows.toList(), unitSystem)
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
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add measurement row", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Success Saved Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(40.dp))
            },
            title = {
                Text("Measurements Recorded", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$savedCount measurement lines saved successfully into the Measurement Book.")
                    Text("Total Quantity: ${String.format(Locale.getDefault(), "%.2f", totalQuantity)} $displayUom", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateToBook()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("View Measurement Book", color = MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showSuccessDialog = false
                        onNavigateBack()
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Project Hub", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small
        )
    }
}

private fun MeasurementEntryRow.toDraftRow() = MeasurementDraftRow(
    id, description, lengthText, heightText, widthText, nosText, deductionText, remarks, photoUri
)

private fun MeasurementDraftRow.toEntryRow() = MeasurementEntryRow(
    id, description, lengthText, heightText, widthText, nosText, deductionText, remarks, photoUri
)

private fun MeasurementEntryRow.convertUnits(
    type: CalculationType,
    from: MeasurementUnitSystem,
    to: MeasurementUnitSystem
) = copy(
    lengthText = MeasurementUnitConverter.convertText(lengthText, 1, from, to),
    widthText = MeasurementUnitConverter.convertText(widthText, 1, from, to),
    heightText = MeasurementUnitConverter.convertText(heightText, 1, from, to),
    deductionText = MeasurementUnitConverter.convertText(
        deductionText,
        if (type == CalculationType.WALL_PLASTER) 2 else MeasurementUnitConverter.powerFor(type),
        from,
        to
    )
)

private fun MeasurementEntity.dimensionText(value: Double, target: MeasurementUnitSystem): String {
    if (value <= 0.0) return ""
    return MeasurementUnitConverter.convertText(
        value.toString(),
        power = 1,
        from = MeasurementUnitConverter.infer(unit),
        to = target
    )
}

private fun MeasurementEntity.deductionText(type: CalculationType, target: MeasurementUnitSystem): String {
    if (deduction <= 0.0) return "0"
    return MeasurementUnitConverter.convertText(
        deduction.toString(),
        power = if (type == CalculationType.WALL_PLASTER) 2 else MeasurementUnitConverter.powerFor(type),
        from = MeasurementUnitConverter.infer(unit),
        to = target
    )
}

private fun MeasurementEntryRow.validation(type: CalculationType): MeasurementRowValidation = MeasurementRowValidator.validate(
    type,
    MeasurementRowValidationInput(description, nosText, lengthText, widthText, heightText, deductionText, photoUri != null, remarks)
)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UnitSystemToggle(
    selected: MeasurementUnitSystem,
    onSelect: (MeasurementUnitSystem) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.testTag("unit_system_toggle")
    ) {
        MeasurementUnitSystem.entries.forEachIndexed { index, system ->
            SegmentedButton(
                selected = system == selected,
                onClick = { onSelect(system) },
                shape = SegmentedButtonDefaults.itemShape(index, MeasurementUnitSystem.entries.size),
                modifier = Modifier.testTag("unit_system_${system.name.lowercase()}")
            ) {
                Text(if (system == MeasurementUnitSystem.METRIC) "Metric" else "Imperial")
            }
        }
    }
}

@Composable
fun MeasurementRowCard(
    index: Int,
    row: MeasurementEntryRow,
    calcType: CalculationType,
    itemName: String,
    itemUom: String,
    dimensionUom: String,
    selected: Boolean,
    onToggleSelected: () -> Unit,
    onDuplicate: () -> Unit,
    onPhotoClick: () -> Unit,
    onRemovePhoto: () -> Unit,
    onUpdateRow: (MeasurementEntryRow) -> Unit,
    onDelete: () -> Unit,
    onAddNextLine: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var description by remember(row.description) { mutableStateOf(row.description) }
    var lengthText by remember(row.lengthText) { mutableStateOf(row.lengthText) }
    var widthText by remember(row.widthText) { mutableStateOf(row.widthText) }
    var heightText by remember(row.heightText) { mutableStateOf(row.heightText) }
    var nosText by remember(row.nosText) { mutableStateOf(row.nosText) }
    val rowQty = row.calculateQuantity(calcType)
    val validation = row.validation(calcType)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("measurement_row_$index"),
        shape = MaterialTheme.shapes.small,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, when {
            selected -> MaterialTheme.colorScheme.primary
            validation.status == MeasurementRowStatus.READY -> MaterialTheme.colorScheme.tertiary
            validation.status == MeasurementRowStatus.INCOMPLETE -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outlineVariant
        }),
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggleSelected() },
                modifier = Modifier.size(34.dp)
            )
            Column(modifier = Modifier.width(126.dp)) {
                Text("$index. $itemName", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(itemUom, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    onUpdateRow(row.copy(description = it))
                },
                label = { Text("Member description", fontSize = 9.sp) },
                placeholder = { Text("Beam B1 / North wall", fontSize = 10.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
                textStyle = MaterialTheme.typography.labelSmall,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .width(170.dp)
                    .testTag("input_row_member_$index")
            )
            CompactNumberCell(
                label = "No.",
                value = nosText,
                tag = "input_row_nos_$index",
                imeAction = if (calcType == CalculationType.NOS) ImeAction.Done else ImeAction.Next,
                onValueChange = {
                    nosText = it
                    onUpdateRow(row.copy(nosText = it))
                }
            )
            if (calcType != CalculationType.NOS) {
                CompactNumberCell(
                    label = "Length ($dimensionUom)",
                    value = lengthText,
                    tag = "input_row_length_$index",
                    imeAction = if (calcType == CalculationType.RUNNING_LENGTH) ImeAction.Done else ImeAction.Next,
                    onValueChange = {
                        lengthText = it
                        onUpdateRow(row.copy(lengthText = it))
                    }
                )
            }
            if (calcType == CalculationType.AREA || calcType == CalculationType.VOLUME) {
                CompactNumberCell(
                    label = "Breadth ($dimensionUom)",
                    value = widthText,
                    tag = "input_row_breadth_$index",
                    imeAction = if (calcType == CalculationType.AREA) ImeAction.Done else ImeAction.Next,
                    onValueChange = {
                        widthText = it
                        onUpdateRow(row.copy(widthText = it))
                    }
                )
            }
            if (calcType == CalculationType.WALL_PLASTER || calcType == CalculationType.VOLUME) {
                CompactNumberCell(
                    label = "Height ($dimensionUom)",
                    value = heightText,
                    tag = "input_row_height_$index",
                    imeAction = ImeAction.Done,
                    onValueChange = {
                        heightText = it
                        onUpdateRow(row.copy(heightText = it))
                    }
                )
            }
            Column(
                modifier = Modifier.width(76.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Qty", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.getDefault(), "%.2f", rowQty), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                Text(
                    when (validation.status) {
                        MeasurementRowStatus.READY -> "Ready"
                        MeasurementRowStatus.INCOMPLETE -> "Incomplete"
                        MeasurementRowStatus.EMPTY -> "Empty"
                    },
                    fontSize = 8.sp,
                    color = when (validation.status) {
                        MeasurementRowStatus.READY -> MaterialTheme.colorScheme.tertiary
                        MeasurementRowStatus.INCOMPLETE -> MaterialTheme.colorScheme.error
                        MeasurementRowStatus.EMPTY -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = onPhotoClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (row.photoUri == null) Icons.Default.AddAPhoto else Icons.Default.Photo,
                    contentDescription = if (row.photoUri == null) "Add optional photo" else "Replace photo",
                    tint = if (row.photoUri == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(17.dp)
                )
            }
            if (row.photoUri != null) {
                IconButton(onClick = onRemovePhoto, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
            IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate row", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            }
            IconButton(onClick = onAddNextLine, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add row below", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete row", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun CompactNumberCell(
    label: String,
    value: String,
    tag: String,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            if (next.isEmpty() || next.matches(Regex("\\d*\\.?\\d*"))) onValueChange(next)
        },
        label = { Text(label, fontSize = 9.sp) },
        placeholder = { Text("0", style = MaterialTheme.typography.labelSmall) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() }
        ),
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.width(82.dp).testTag(tag),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline)
    )
}
