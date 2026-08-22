package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CalculationType
import com.example.data.local.entity.MeasurementEntity
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.BreadcrumbItem
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItemMeasureScreen(
    viewModel: SiteViewModel,
    onNavigateBack: () -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val floors by viewModel.floors.collectAsStateWithLifecycle()
    val selectedFloorId by viewModel.selectedFloorId.collectAsStateWithLifecycle()
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val selectedRoomId by viewModel.selectedRoomId.collectAsStateWithLifecycle()
    val components by viewModel.components.collectAsStateWithLifecycle()
    val selectedComponentId by viewModel.selectedComponentId.collectAsStateWithLifecycle()
    val selectedWorkItem by viewModel.selectedComponentWorkItem.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val selectedContractorId by viewModel.selectedContractorId.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val activeMeasurements by viewModel.activeWorkItemMeasurements.collectAsStateWithLifecycle()

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()
    val currentFloor = floors.firstOrNull { it.id == selectedFloorId } ?: floors.firstOrNull()
    val currentRoom = rooms.firstOrNull { it.id == selectedRoomId } ?: rooms.firstOrNull()
    val currentComp = components.firstOrNull { it.id == selectedComponentId } ?: components.firstOrNull()
    val currentItem = items.firstOrNull { it.id == (selectedWorkItem?.itemId ?: 0L) } ?: items.firstOrNull()
    val projectContractors = contractors.filter { it.projectId == (currentProject?.id ?: 0L) }

    val calcType = currentItem?.calculationType ?: CalculationType.WALL_PLASTER
    val currentQty = viewModel.computeQuantity(calcType, formState)
    val currentAmt = viewModel.computeAmount(currentQty, formState.rate)

    val totalRowsQty = activeMeasurements.sumOf { it.quantity }
    val totalRowsAmt = activeMeasurements.sumOf { it.amount }

    var lastSavedMessage by remember { mutableStateOf<String?>(null) }

    val breadcrumbs = listOfNotNull(
        BreadcrumbItem(currentProject?.name ?: "Project") { viewModel.jumpToProject() },
        currentFloor?.let { BreadcrumbItem(it.name) { viewModel.jumpToFloor(it.id) } },
        currentRoom?.let { BreadcrumbItem(it.name) { viewModel.jumpToRoom(it.id) } },
        currentComp?.let { BreadcrumbItem(it.name) { viewModel.jumpToComponent(it.id) } },
        selectedWorkItem?.let { BreadcrumbItem(it.itemName) }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${currentComp?.name ?: "Component"} • ${selectedWorkItem?.itemName ?: currentItem?.name ?: "Work Item"}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "${currentRoom?.name ?: "Room"} • ${currentItem?.unit ?: "m²"}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Book", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            // Persistent Continuous Loop Navigation Actions
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CONTINUOUS MEASUREMENT WORKFLOW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveMeasurementRow { saved ->
                                    lastSavedMessage = "Saved: ${saved.description} (${saved.quantity} ${saved.unit})"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("save_row_btn")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Row", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.continueNextItem() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Next Item", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.continueNextComponent() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Next Component", fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.continueNextRoom() },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.MeetingRoom, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Next Room", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BreadcrumbBar(
                    items = breadcrumbs,
                    onHomeClick = { viewModel.jumpToHome() }
                )
            }

            // Success feedback banner
            item {
                AnimatedVisibility(visible = lastSavedMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(lastSavedMessage ?: "", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            IconButton(onClick = { lastSavedMessage = null }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            // Contractor Segment
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text(
                        text = "ASSIGN CONTRACTOR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        projectContractors.forEach { contractor ->
                            val isSel = contractor.id == selectedContractorId
                            FilterChip(
                                selected = isSel,
                                onClick = { viewModel.selectContractor(contractor.id) },
                                label = { Text(contractor.name, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Interactive Measurement Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Entry Row Input",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            TextButton(
                                onClick = { viewModel.repeatFormValues() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Repeat L×H", fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Description with quick chips
                        OutlinedTextField(
                            value = formState.description,
                            onValueChange = { viewModel.updateForm(description = it) },
                            label = { Text("Row Description / Part") },
                            placeholder = { Text("e.g. Main Face, Window Offset, Deduction D1") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("row_description_input")
                        )

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Main Face", "Deduction D1", "Deduction W1", "Side Return", "Beam Bottom", "Full Wall").forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { viewModel.updateForm(description = suggestion) },
                                    label = { Text(suggestion, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Dynamic Inputs based on Calculation Type
                        when (calcType) {
                            CalculationType.RUNNING_LENGTH -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = formState.length,
                                        onValueChange = { viewModel.updateForm(length = it) },
                                        label = { Text("Length (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_length")
                                    )
                                    OutlinedTextField(
                                        value = formState.nos,
                                        onValueChange = { viewModel.updateForm(nos = it) },
                                        label = { Text("Nos") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_nos")
                                    )
                                }
                            }
                            CalculationType.AREA -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = formState.length,
                                        onValueChange = { viewModel.updateForm(length = it) },
                                        label = { Text("Length (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_length")
                                    )
                                    OutlinedTextField(
                                        value = formState.width,
                                        onValueChange = { viewModel.updateForm(width = it) },
                                        label = { Text("Width (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_width")
                                    )
                                    OutlinedTextField(
                                        value = formState.nos,
                                        onValueChange = { viewModel.updateForm(nos = it) },
                                        label = { Text("Nos") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f).testTag("input_nos")
                                    )
                                }
                            }
                            CalculationType.WALL_PLASTER -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = formState.length,
                                        onValueChange = { viewModel.updateForm(length = it) },
                                        label = { Text("Length (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_length")
                                    )
                                    OutlinedTextField(
                                        value = formState.height,
                                        onValueChange = { viewModel.updateForm(height = it) },
                                        label = { Text("Height (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("input_height")
                                    )
                                    OutlinedTextField(
                                        value = formState.nos,
                                        onValueChange = { viewModel.updateForm(nos = it) },
                                        label = { Text("Nos") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f).testTag("input_nos")
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = formState.deduction,
                                    onValueChange = { viewModel.updateForm(deduction = it) },
                                    label = { Text("Deduction (m²) e.g. doors/windows") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("input_deduction")
                                )
                            }
                            CalculationType.VOLUME -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedTextField(
                                        value = formState.length,
                                        onValueChange = { viewModel.updateForm(length = it) },
                                        label = { Text("L (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = formState.width,
                                        onValueChange = { viewModel.updateForm(width = it) },
                                        label = { Text("W (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = formState.height,
                                        onValueChange = { viewModel.updateForm(height = it) },
                                        label = { Text("H (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = formState.nos,
                                        onValueChange = { viewModel.updateForm(nos = it) },
                                        label = { Text("Nos") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                }
                            }
                            CalculationType.NOS -> {
                                OutlinedTextField(
                                    value = formState.nos,
                                    onValueChange = { viewModel.updateForm(nos = it) },
                                    label = { Text("Quantity (Nos)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Rate field
                        OutlinedTextField(
                            value = formState.rate,
                            onValueChange = { viewModel.updateForm(rate = it) },
                            label = { Text("Contractor Rate (₹ / ${currentItem?.unit ?: "unit"})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_rate")
                        )

                        Spacer(Modifier.height(12.dp))

                        // Live Calculation Preview
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("ROW QUANTITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${String.format(java.util.Locale.getDefault(), "%.2f", currentQty)} ${currentItem?.unit ?: ""}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("ROW AMOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "₹${String.format(java.util.Locale.getDefault(), "%,.2f", currentAmt)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Existing Rows in this Component / Work Item
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recorded Measurement Rows (${activeMeasurements.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Total: ${String.format(java.util.Locale.getDefault(), "%.2f", totalRowsQty)} ${currentItem?.unit ?: ""} • ₹${totalRowsAmt.toInt()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (activeMeasurements.isEmpty()) {
                item {
                    Text(
                        text = "No measurement rows entered for this work item yet. Fill the fields above and tap 'Save Row'.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else {
                items(activeMeasurements, key = { it.id }) { m ->
                    MeasurementRowCard(
                        measurement = m,
                        onDelete = { viewModel.deleteMeasurement(m) }
                    )
                }
            }
        }
    }
}

@Composable
fun MeasurementRowCard(
    measurement: MeasurementEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = measurement.description.ifBlank { measurement.itemName },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                val formulaStr = when (measurement.calculationType) {
                    CalculationType.RUNNING_LENGTH -> "L: ${measurement.length} × ${measurement.nos}"
                    CalculationType.AREA -> "L: ${measurement.length} × W: ${measurement.width} × ${measurement.nos}"
                    CalculationType.WALL_PLASTER -> "L: ${measurement.length} × H: ${measurement.height} × ${measurement.nos}${if (measurement.deduction > 0) " (-${measurement.deduction})" else ""}"
                    CalculationType.VOLUME -> "L: ${measurement.length} × W: ${measurement.width} × H: ${measurement.height} × ${measurement.nos}"
                    CalculationType.NOS -> "Nos: ${measurement.nos}"
                }
                Text(
                    text = formulaStr,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${measurement.contractorName} • @ ₹${measurement.rate}/${measurement.unit}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(java.util.Locale.getDefault(), "%.2f", measurement.quantity)} ${measurement.unit}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "₹${String.format(java.util.Locale.getDefault(), "%,.0f", measurement.amount)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
