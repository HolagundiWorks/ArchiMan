@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.ItemMasterEntity
import com.example.domain.WorkCatalog
import com.example.domain.WorkItemDuplicateDetector
import com.example.domain.WorkItemDuplicateCandidate
import com.example.ui.navigation.AppScreen
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataScreen(
    viewModel: SiteViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.items.collectAsStateWithLifecycle()
    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemMasterEntity?>(null) }
    var showDuplicateReport by remember { mutableStateOf(false) }
    var candidateToMerge by remember { mutableStateOf<WorkItemDuplicateCandidate?>(null) }
    var mergeError by remember { mutableStateOf<String?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val catalogImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Unable to read catalog") }
                .onSuccess { json ->
                    viewModel.importWorkCatalog(json) { result ->
                        importMessage = result.fold(
                            onSuccess = { count -> if (count == 0) "Catalog valid; all items already exist." else "Imported $count work items." },
                            onFailure = { error -> error.message ?: "Catalog import failed" }
                        )
                    }
                }
                .onFailure { importMessage = it.message ?: "Catalog import failed" }
        }
    }
    val expandedWorkTypes = remember { mutableStateListOf<String>() }
    val duplicateCandidates = remember(items) { WorkItemDuplicateDetector.find(items) }
    val pwdItemCount = remember(items) { items.count { it.sourceName.isNotBlank() } }
    val customItemCount = items.size - pwdItemCount

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("PWD SR Work Catalogue", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Specifications, custom items and formulas",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                modifier = Modifier.testTag("fab_add_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Work Item")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Context bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PWD SR $pwdItemCount  •  CUSTOM $customItemCount",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { catalogImporter.launch(arrayOf("application/json", "text/plain")) }) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("Import", style = MaterialTheme.typography.labelSmall)
                        }
                        if (duplicateCandidates.isEmpty()) {
                            Text("Verified", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        } else {
                            TextButton(onClick = { showDuplicateReport = true }) {
                                Text("Review ${duplicateCandidates.size} duplicate${if (duplicateCandidates.size == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            val groupedItems = items
                .sortedWith(compareBy<ItemMasterEntity> { it.sourceName.isBlank() }.thenBy { it.sourceItemCode }.thenBy { it.name })
                .groupBy { it.workType.ifBlank { "General Works" } }
                .toSortedMap()
            importMessage?.let { message ->
                Surface(color = MaterialTheme.colorScheme.primaryContainer, border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = { importMessage = null }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            LaunchedEffect(groupedItems.keys) {
                if (expandedWorkTypes.isEmpty()) expandedWorkTypes.addAll(groupedItems.keys)
            }

            // Work Type -> Work Item -> Formula hierarchy
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                groupedItems.forEach { (workType, workItems) ->
                    item(key = "work_type_$workType") {
                        val expanded = workType in expandedWorkTypes
                        Surface(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (expanded) expandedWorkTypes.remove(workType) else expandedWorkTypes.add(workType)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(workType, color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${workItems.size} work item${if (workItems.size == 1) "" else "s"}", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                                }
                                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.surface)
                            }
                        }
                    }
                    if (workType in expandedWorkTypes) items(workItems, key = { it.id }) { item ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { itemToEdit = item }
                            .testTag("item_master_row_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(item.itemCode, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                if (item.sourceName.isNotBlank()) {
                                    Text(
                                        text = "${item.sourceName} • Item ${item.sourceItemCode}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (item.specification.isNotBlank()) {
                                    Text(
                                        text = item.specification,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Formula Tag
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                    ) {
                                        Text(
                                            text = item.calculationType.displayName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Unit Tag
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Text(
                                            text = item.unit,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { itemToEdit = item },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { viewModel.deleteItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    // Add Item Dialog
    if (showAddItemDialog) {
        ItemMasterEditorDialog(
            initialItem = null,
            onDismiss = { showAddItemDialog = false },
            onSave = { workType, name, unit, calcType ->
                viewModel.addItem(
                    ItemMasterEntity(
                        itemCode = WorkCatalog.codeFor(workType, name),
                        workType = workType,
                        name = name,
                        unit = unit,
                        calculationType = calcType
                    )
                )
                showAddItemDialog = false
            }
        )
    }

    // Edit Item Dialog
    itemToEdit?.let { item ->
        ItemMasterEditorDialog(
            initialItem = item,
            onDismiss = { itemToEdit = null },
            onSave = { workType, name, unit, calcType ->
                viewModel.updateItem(
                    item.copy(
                        workType = workType,
                        name = name,
                        unit = unit,
                        calculationType = calcType
                    )
                )
                itemToEdit = null
            }
        )
    }

    if (showDuplicateReport) {
        AlertDialog(
            onDismissRequest = { showDuplicateReport = false },
            title = { Text("POSSIBLE DUPLICATE ITEMS", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                    items(duplicateCandidates, key = { "${it.first.id}_${it.second.id}" }) { candidate ->
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("${candidate.similarity}% similar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Text(candidate.first.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(candidate.second.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("${candidate.first.workType} • ${candidate.first.unit} • ${candidate.first.calculationType.displayName}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                TextButton(
                                    onClick = {
                                        candidateToMerge = candidate
                                        mergeError = null
                                        showDuplicateReport = false
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("Preview merge") }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showDuplicateReport = false }) { Text("Close") } }
        )
    }

    candidateToMerge?.let { candidate ->
        var canonicalId by remember(candidate) { mutableLongStateOf(candidate.first.id) }
        val canonical = if (canonicalId == candidate.first.id) candidate.first else candidate.second
        val source = if (canonicalId == candidate.first.id) candidate.second else candidate.first
        AlertDialog(
            onDismissRequest = { candidateToMerge = null; mergeError = null },
            title = { Text("MERGE WORK ITEMS", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select the canonical item to keep. Measurements remain unchanged; their item reference is moved safely.", fontSize = 12.sp)
                    listOf(candidate.first, candidate.second).forEach { option ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { canonicalId = option.id },
                            color = if (canonicalId == option.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (canonicalId == option.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = canonicalId == option.id, onClick = { canonicalId = option.id })
                                Column {
                                    Text(option.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(option.itemCode, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("MERGE PREVIEW", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text("Keep: ${canonical.name}", style = MaterialTheme.typography.labelSmall)
                            Text("Archive: ${source.name}", style = MaterialTheme.typography.labelSmall)
                            Text("Preserve “${source.name}” as an alias", style = MaterialTheme.typography.labelSmall)
                            Text("Repoint measurements, component links and contractor qualifications", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    mergeError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall) }
                }
            },
            dismissButton = { TextButton(onClick = { candidateToMerge = null; mergeError = null }) { Text("Cancel") } },
            confirmButton = {
                Button(onClick = {
                    viewModel.mergeWorkItems(source.id, canonical.id) { result ->
                        result.onSuccess { candidateToMerge = null; mergeError = null }
                            .onFailure { mergeError = it.message ?: "Merge failed" }
                    }
                }) { Text("Merge safely") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemMasterEditorDialog(
    initialItem: ItemMasterEntity?,
    onDismiss: () -> Unit,
    onSave: (workType: String, name: String, unit: String, calcType: CalculationType) -> Unit
) {
    var name by remember { mutableStateOf(initialItem?.name ?: "") }
    var workType by remember { mutableStateOf(initialItem?.workType ?: "General Works") }
    var workTypeExpanded by remember { mutableStateOf(false) }
    var unit by remember { mutableStateOf(initialItem?.unit ?: "cum") }
    var selectedCalcType by remember { mutableStateOf(initialItem?.calculationType ?: CalculationType.VOLUME) }

    val commonUnits = listOf("cum", "sqm", "rmt", "nos", "kg", "ton", "bags", "litres")
    val workTypes = listOf("Earthwork", "Concrete & Structure", "Masonry", "Finishes", "Flooring & Cladding", "Plumbing Works", "Electrical Works", "Joinery & Carpentry", "Waterproofing", "HVAC Works", "Fire Protection", "Fabrication", "Landscaping", "General Works")

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (initialItem == null) "ADD WORK ITEM" else "EDIT WORK ITEM",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 0.5.sp
                )

                ExposedDropdownMenuBox(expanded = workTypeExpanded, onExpandedChange = { workTypeExpanded = !workTypeExpanded }) {
                    OutlinedTextField(
                        value = workType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("WORK TYPE") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(workTypeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = workTypeExpanded, onDismissRequest = { workTypeExpanded = false }) {
                        workTypes.forEach { option ->
                            DropdownMenuItem(text = { Text(option) }, onClick = { workType = option; workTypeExpanded = false })
                        }
                    }
                }

                // Item Name
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ITEM NAME / DESCRIPTION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("e.g. RCC M25 Beam Concrete") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }

                // Calculation Formula Type
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CALCULATION FORMULA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CalculationType.values().forEach { calcType ->
                        val isSelected = selectedCalcType == calcType
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCalcType = calcType
                                    if (unit == "cum" || unit == "sqm" || unit == "rmt" || unit == "nos") {
                                        unit = when (calcType) {
                                            CalculationType.VOLUME -> "cum"
                                            CalculationType.AREA, CalculationType.WALL_PLASTER -> "sqm"
                                            CalculationType.RUNNING_LENGTH -> "rmt"
                                            CalculationType.NOS -> "nos"
                                        }
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = calcType.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = when (calcType) {
                                            CalculationType.VOLUME -> "Nos × Length × Width × Height/Depth"
                                            CalculationType.AREA -> "Nos × Length × Width"
                                            CalculationType.WALL_PLASTER -> "Nos × Length × Height (- Deductions)"
                                            CalculationType.RUNNING_LENGTH -> "Nos × Length"
                                            CalculationType.NOS -> "Direct item count (Nos)"
                                        },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Measurement Unit
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "UNIT OF MEASUREMENT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonUnits.take(4).forEach { u ->
                            val isSel = unit.equals(u, ignoreCase = true)
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { unit = u }
                            ) {
                                Text(
                                    text = u,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(workType, name.trim(), unit.trim(), selectedCalcType)
                            }
                        },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Save Item", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
