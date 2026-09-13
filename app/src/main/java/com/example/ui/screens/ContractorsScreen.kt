@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import com.example.ui.icons.CarbonIcons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.ContractorEntity
import com.example.data.local.entity.ContractorQualifiedItemEntity
import com.example.domain.CatalogDocumentParser
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContractorsScreen(
    viewModel: SiteViewModel
) {
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val allQualifiedItems by viewModel.allQualifiedItems.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var contractorToAddItemTo by remember { mutableStateOf<ContractorEntity?>(null) }
    var contractorToDelete by remember { mutableStateOf<ContractorEntity?>(null) }
    var contractorToEdit by remember { mutableStateOf<ContractorEntity?>(null) }

    val filteredContractors = remember(contractors, searchQuery) {
        if (searchQuery.isBlank()) contractors
        else {
            contractors.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.address.contains(searchQuery, ignoreCase = true) ||
                it.contactNo.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("Contractors")
                            Text("${contractors.size} registered", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contractors or trades") },
                    leadingIcon = { Icon(CarbonIcons.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).testTag("input_search_contractors"),
                    singleLine = true
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_contractor")
            ) {
                Icon(CarbonIcons.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add contractor")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Contractors List
            if (filteredContractors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = CarbonIcons.Engineering,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "No contractors registered" else "No matching contractors found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Register contractors along with their qualified trade items (e.g., BrickWork 230mm, Plastering, Concrete).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { showAddDialog = true },
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_empty_add_contractor")
                        ) {
                            Icon(CarbonIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create First Contractor", fontSize = 13.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(filteredContractors, key = { it.id }) { contractor ->
                        val contractorItems = allQualifiedItems.filter { it.contractorId == contractor.id }
                        ContractorCard(
                            contractor = contractor,
                            qualifiedItems = contractorItems,
                            onAddQualifiedItem = { contractorToAddItemTo = contractor },
                            onDeleteQualifiedItem = { item -> viewModel.deleteQualifiedItem(item) },
                            onEdit = { contractorToEdit = contractor },
                            onDelete = { contractorToDelete = contractor }
                        )
                    }
                }
            }
        }
    }

    // Add Contractor Dialog
    if (showAddDialog) {
        AddContractorDialog(
            existingContractors = contractors,
            allQualifiedItems = allQualifiedItems,
            onDismiss = { showAddDialog = false },
            onSave = { name, address, contactNo, contractorType, qualifiedItemsList ->
                viewModel.addContractorWithQualifiedItems(name, address, contactNo, contractorType, qualifiedItemsList)
                showAddDialog = false
            }
        )
    }

    // Edit Contractor Dialog
    if (contractorToEdit != null) {
        EditContractorDialog(
            contractor = contractorToEdit!!,
            onDismiss = { contractorToEdit = null },
            onSave = { updated ->
                viewModel.updateContractor(updated)
                contractorToEdit = null
            }
        )
    }

    // Add Single Qualified Item Dialog
    if (contractorToAddItemTo != null) {
        AddQualifiedItemDialog(
            contractor = contractorToAddItemTo!!,
            onDismiss = { contractorToAddItemTo = null },
            onSave = { itemName, uom ->
                viewModel.addQualifiedItemToContractor(contractorToAddItemTo!!.id, itemName, uom)
                contractorToAddItemTo = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (contractorToDelete != null) {
        AlertDialog(
            onDismissRequest = { contractorToDelete = null },
            title = { Text("Delete Contractor", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Are you sure you want to delete contractor \"${contractorToDelete?.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        contractorToDelete?.let { viewModel.deleteContractor(it) }
                        contractorToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { contractorToDelete = null },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContractorCard(
    contractor: ContractorEntity,
    qualifiedItems: List<ContractorQualifiedItemEntity>,
    onAddQualifiedItem: () -> Unit,
    onDeleteQualifiedItem: (ContractorQualifiedItemEntity) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contractor_card_${contractor.id}"),
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
            // Header: Name & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CarbonIcons.Engineering,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = contractor.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${qualifiedItems.size} Qualified Item${if (qualifiedItems.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(CarbonIcons.Edit, contentDescription = "Edit Contractor", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(CarbonIcons.DeleteOutline, contentDescription = "Delete Contractor", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // Address & Contact No
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (contractor.address.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = CarbonIcons.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = contractor.address,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val phoneDisplay = contractor.contactNo.ifBlank { contractor.phone }
                if (phoneDisplay.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = CarbonIcons.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Contract No / Phone: $phoneDisplay",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

            // Qualified Items Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ITEMS QUALIFIED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
                TextButton(
                    onClick = onAddQualifiedItem,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(CarbonIcons.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text("Add Item", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (qualifiedItems.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No items qualified yet. Click '+ Add Item' to qualify this contractor for masonry, plaster, concrete, etc.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    qualifiedItems.forEach { item ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Column {
                                    Text(
                                        text = item.itemName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "UOM: ${item.uom} • ${item.calculationType.displayName}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteQualifiedItem(item) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = CarbonIcons.Close,
                                        contentDescription = "Remove item",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContractorDialog(
    existingContractors: List<ContractorEntity>,
    allQualifiedItems: List<ContractorQualifiedItemEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, address: String, contactNo: String, contractorType: String, items: List<Pair<String, String>>) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contactNo by remember { mutableStateOf("") }
    val bundledCatalog = remember(context) { CatalogDocumentParser.loadBundled(context) }
    val contractorTypes = bundledCatalog.contractorTypes.map { it.name }
    val standardWorkItems = bundledCatalog.contractorTypes.associate { type ->
        type.name to type.items.map { it.name to it.uom }
    }
    var contractorType by remember { mutableStateOf("Civil") }
    var typeExpanded by remember { mutableStateOf(false) }
    var importExpanded by remember { mutableStateOf(false) }
    
    // Dynamic qualified items
    val qualifiedItems = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            addAll(standardWorkItems["Civil"].orEmpty())
        }
    }

    var newItemName by remember { mutableStateOf("") }
    var newItemUom by remember { mutableStateOf("m²") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetItems = standardWorkItems[contractorType].orEmpty()

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            text = "Add New Contractor",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(CarbonIcons.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
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
                    OutlinedTextField(
                        label = { Text("CONTRACTOR / VENDOR NAME *") },
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        placeholder = { Text("e.g. Sharma Civil Works") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().testTag("input_contractor_name"),
                        singleLine = true)
                }

                item {
                    ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = !typeExpanded }) {
                        OutlinedTextField(
                            value = contractorType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Contractor type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            contractorTypes.forEach { type ->
                                DropdownMenuItem(text = { Text(type) }, onClick = {
                                    contractorType = type
                                    qualifiedItems.clear()
                                    qualifiedItems.addAll(standardWorkItems[type].orEmpty())
                                    typeExpanded = false
                                })
                            }
                        }
                    }
                }

                if (existingContractors.isNotEmpty()) {
                    item {
                        ExposedDropdownMenuBox(expanded = importExpanded, onExpandedChange = { importExpanded = !importExpanded }) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Import qualified items") },
                                placeholder = { Text("Copy from existing contractor") },
                                leadingIcon = { Icon(CarbonIcons.ContentCopy, contentDescription = null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(importExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = importExpanded, onDismissRequest = { importExpanded = false }) {
                                existingContractors.forEach { source ->
                                    val sourceItems = allQualifiedItems.filter { it.contractorId == source.id }
                                    DropdownMenuItem(
                                        text = { Text("${source.name} (${sourceItems.size} items)") },
                                        enabled = sourceItems.isNotEmpty(),
                                        onClick = {
                                            qualifiedItems.clear()
                                            qualifiedItems.addAll(sourceItems.map { it.itemName to it.uom })
                                            contractorType = source.contractorType
                                            importExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        label = { Text("ADDRESS / BASE LOCATION") },
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text("e.g. Plot 18, Industrial Area Phase 1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth().testTag("input_contractor_address"),
                        singleLine = true)
                }

                item {
                    OutlinedTextField(
                        label = { Text("CONTRACT NO / PHONE *") },
                        value = contactNo,
                        onValueChange = { contactNo = it },
                        placeholder = { Text("e.g. +91 98765 43210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("input_contractor_contact"),
                        singleLine = true)
                }

                item {
                    Text(
                        text = "QUALIFIED TRADE ITEMS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Specify the work items this contractor is qualified to execute (item name and UOM):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick presets chips
                item {
                    Text("Quick Add Predefined Items:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(presetItems) { (pName, pUom) ->
                            val alreadyAdded = qualifiedItems.any { it.first.equals(pName, ignoreCase = true) }
                            FilterChip(
                                selected = alreadyAdded,
                                onClick = {
                                    if (!alreadyAdded) {
                                        qualifiedItems.add(pName to pUom)
                                    }
                                },
                                label = { Text("$pName ($pUom)", style = MaterialTheme.typography.labelSmall) },
                                shape = MaterialTheme.shapes.small
                            )
                        }
                    }
                }

                // Current items list in dialog
                items(qualifiedItems.size) { idx ->
                    val item = qualifiedItems[idx]
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(item.first, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("UOM: ${item.second}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(
                                onClick = { qualifiedItems.removeAt(idx) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(CarbonIcons.DeleteOutline, contentDescription = "Delete item", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Custom item addition row
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Add Custom Qualified Item", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = newItemName,
                                    onValueChange = { newItemName = it },
                                    placeholder = { Text("Item Name", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(2f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = newItemUom,
                                    onValueChange = { newItemUom = it },
                                    placeholder = { Text("UOM", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    singleLine = true
                                )
                            }
                            Button(
                                onClick = {
                                    if (newItemName.isNotBlank()) {
                                        qualifiedItems.add(newItemName.trim() to newItemUom.trim().ifBlank { "m²" })
                                        newItemName = ""
                                    }
                                },
                                shape = MaterialTheme.shapes.small,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onSurface),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("+ Add Item", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.surface)
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
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
                                    errorMessage = "Contractor name is required."
                                } else {
                                    onSave(name.trim(), address.trim(), contactNo.trim(), contractorType, qualifiedItems.toList())
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_save_contractor"),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Save Contractor", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.surface)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditContractorDialog(
    contractor: ContractorEntity,
    onDismiss: () -> Unit,
    onSave: (ContractorEntity) -> Unit
) {
    var name by remember { mutableStateOf(contractor.name) }
    var address by remember { mutableStateOf(contractor.address) }
    var contactNo by remember { mutableStateOf(contractor.contactNo.ifBlank { contractor.phone }) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Edit Contractor Details",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                OutlinedTextField(
                    label = { Text("CONTRACTOR NAME") },
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. Sharma Civil Works") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_contractor_name"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("ADDRESS") },
                    value = address,
                    onValueChange = { address = it },
                    placeholder = { Text("e.g. Plot 18, Industrial Area") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_contractor_address"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("CONTACT NO / PHONE") },
                    value = contactNo,
                    onValueChange = { contactNo = it },
                    placeholder = { Text("e.g. +91 98765 43210") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_contractor_contact"),
                    singleLine = true)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            onSave(contractor.copy(name = name, address = address, contactNo = contactNo, phone = contactNo))
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Update", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddQualifiedItemDialog(
    contractor: ContractorEntity,
    onDismiss: () -> Unit,
    onSave: (itemName: String, uom: String) -> Unit
) {
    var itemName by remember { mutableStateOf("") }
    var uom by remember { mutableStateOf("m²") }

    val presetOptions = listOf(
        Pair("BrickWork 230mm", "m²"),
        Pair("BrickWork 115mm", "m²"),
        Pair("Plaster 12mm", "m²"),
        Pair("Plaster 20mm (External)", "m²"),
        Pair("Slab Concrete", "m³"),
        Pair("Beam Concrete", "m³"),
        Pair("RCC", "m³"),
        Pair("Flooring", "m²"),
        Pair("Painting", "m²"),
        Pair("Putty", "m²")
    )

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Add Qualified Item",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Contractor: ${contractor.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)

                Text("Select from Standard Library:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetOptions) { (pName, pUom) ->
                        FilterChip(
                            selected = itemName == pName,
                            onClick = {
                                itemName = pName
                                uom = pUom
                            },
                            label = { Text(pName, style = MaterialTheme.typography.labelSmall) },
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }

                OutlinedTextField(
                    label = { Text("ITEM NAME *") },
                    value = itemName,
                    onValueChange = { itemName = it },
                    placeholder = { Text("e.g. BrickWork 230mm") },
                    modifier = Modifier.fillMaxWidth().testTag("input_qual_item_name"),
                    singleLine = true)

                OutlinedTextField(
                    label = { Text("UOM *") },
                    value = uom,
                    onValueChange = { uom = it },
                    placeholder = { Text("m², m³, m, Nos") },
                    modifier = Modifier.fillMaxWidth().testTag("input_qual_item_uom"),
                    singleLine = true)

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            if (itemName.isNotBlank()) {
                                onSave(itemName.trim(), uom.trim().ifBlank { "m²" })
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add Item", color = MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
