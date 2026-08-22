package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ComponentEntity
import com.example.data.local.entity.ComponentType
import com.example.data.local.entity.ItemMasterEntity
import com.example.ui.components.BreadcrumbBar
import com.example.ui.components.BreadcrumbItem
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.SiteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomWorkspaceScreen(
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
    val items by viewModel.items.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()

    val currentProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()
    val currentFloor = floors.firstOrNull { it.id == selectedFloorId } ?: floors.firstOrNull()
    val isFloorLevel = selectedRoomId == 0L || (selectedRoomId == null && selectedFloorId != null)
    val currentRoom = if (!isFloorLevel) rooms.firstOrNull { it.id == selectedRoomId } ?: rooms.firstOrNull() else null

    var selectedTypeFilter by remember { mutableStateOf<ComponentType?>(null) }
    var showAddComponentDialog by remember { mutableStateOf(false) }
    var showDuplicateComponentDialog by remember { mutableStateOf<ComponentEntity?>(null) }
    var showConnectWorkDialog by remember { mutableStateOf<ComponentEntity?>(null) }
    var showQuickSetup4WallsDialog by remember { mutableStateOf(false) }

    val filteredComponents = if (selectedTypeFilter != null) {
        components.filter { it.type == selectedTypeFilter }
    } else {
        components
    }

    val breadcrumbs = listOfNotNull(
        BreadcrumbItem(currentProject?.name ?: "Project") { viewModel.jumpToProject() },
        currentFloor?.let { BreadcrumbItem(it.name) { viewModel.jumpToFloor(it.id) } },
        if (isFloorLevel) {
            BreadcrumbItem("Structural (Beams/Slabs)") { viewModel.selectFloorLevel(currentFloor?.id ?: 0L) }
        } else {
            currentRoom?.let { BreadcrumbItem(it.name) { viewModel.jumpToRoom(it.id) } }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isFloorLevel) "${currentFloor?.name ?: "Floor"} - Structural Items" else (currentRoom?.name ?: "Room Components"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = if (isFloorLevel) "Whole Floor Level (Beams, Slabs, Columns) • ${components.size} items" else "${currentFloor?.name ?: "Floor"} • ${components.size} components",
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
                    if (!isFloorLevel) {
                        IconButton(onClick = { showQuickSetup4WallsDialog = true }) {
                            Icon(Icons.Default.CropFree, contentDescription = "Quick 4 Walls", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) }) {
                        Icon(Icons.Default.MenuBook, contentDescription = "Book", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddComponentDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Component") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_component_fab")
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Breadcrumbs
            BreadcrumbBar(
                items = breadcrumbs,
                onHomeClick = { viewModel.jumpToHome() }
            )

            // Category Filter Pills
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { selectedTypeFilter = null },
                        label = { Text("All (${components.size})", fontWeight = if (selectedTypeFilter == null) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(12.dp)
                    )

                    ComponentType.values().forEach { type ->
                        val count = components.count { it.type == type }
                        if (count > 0 || type == ComponentType.WALL || type == ComponentType.FLOOR || type == ComponentType.CEILING || type == ComponentType.OPENING) {
                            FilterChip(
                                selected = selectedTypeFilter == type,
                                onClick = { selectedTypeFilter = if (selectedTypeFilter == type) null else type },
                                label = { Text("${type.displayName} ($count)") },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            // Components List
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredComponents.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.SquareFoot,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "No components found",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Add walls, floors, ceilings, or doors. Connect construction activities like Plaster, Putty, Tiles to start measuring.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showQuickSetup4WallsDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.CropFree, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Quick 4 Walls")
                                    }
                                    Button(
                                        onClick = { showAddComponentDialog = true },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Component")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredComponents, key = { it.id }) { comp ->
                        ComponentCard(
                            component = comp,
                            viewModel = viewModel,
                            onDuplicateClick = { showDuplicateComponentDialog = comp },
                            onConnectWorkClick = { showConnectWorkDialog = comp }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(70.dp))
                }
            }
        }
    }

    // Add Component Dialog
    if (showAddComponentDialog) {
        var compName by remember { mutableStateOf("North Wall") }
        var compType by remember { mutableStateOf(ComponentType.WALL) }
        var lengthStr by remember { mutableStateOf("4.50") }
        var widthStr by remember { mutableStateOf("0.23") }
        var heightStr by remember { mutableStateOf("3.00") }
        var thicknessStr by remember { mutableStateOf("0.23") }
        var autoConnect by remember { mutableStateOf(true) }

        val wallSuggestions = listOf("North Wall", "South Wall", "East Wall", "West Wall", "Partition Wall", "Shaft Wall")
        val floorSuggestions = listOf("Main Floor", "Balcony Floor", "Toilet Floor", "Kitchen Floor")
        val ceilingSuggestions = listOf("Main Ceiling", "False Ceiling", "Drop Ceiling")
        val openingSuggestions = listOf("Door D1 (1.0×2.1)", "Door D2 (0.9×2.1)", "Window W1 (1.5×1.2)", "Ventilator (0.6×0.6)")
        val beamSuggestions = listOf("Main Beam (B1-B8)", "Secondary Beam (SB1-SB4)", "Tie / Lintel Beam", "Cantilever Beam")
        val slabSuggestions = listOf("Main Floor Slab S1", "Roof / Terrace Slab", "Balcony Slab", "Sunken Slab (Toilet)")
        val columnSuggestions = listOf("Columns Group (C1-C8)", "Corner Column C1", "Staircase Column")
        val waterproofingSuggestions = listOf("Terrace Waterproofing", "Toilet Sunken Waterproofing", "Podium Waterproofing")

        val currentSuggestions = when (compType) {
            ComponentType.WALL -> wallSuggestions
            ComponentType.FLOOR -> floorSuggestions
            ComponentType.CEILING -> ceilingSuggestions
            ComponentType.OPENING -> openingSuggestions
            ComponentType.BEAM -> beamSuggestions
            ComponentType.SLAB -> slabSuggestions
            ComponentType.COLUMN -> columnSuggestions
            ComponentType.WATERPROOFING -> waterproofingSuggestions
            else -> wallSuggestions
        }

        AlertDialog(
            onDismissRequest = { showAddComponentDialog = false },
            title = { Text("Add Building Component", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Type selector
                        Text("Component Type:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ComponentType.values().forEach { t ->
                                FilterChip(
                                    selected = compType == t,
                                    onClick = {
                                        compType = t
                                        compName = when (t) {
                                            ComponentType.WALL -> "North Wall"
                                            ComponentType.FLOOR -> "Main Floor"
                                            ComponentType.CEILING -> "Ceiling"
                                            ComponentType.OPENING -> "Door D1"
                                            ComponentType.BEAM -> "Main Beam Grid (B1)"
                                            ComponentType.SLAB -> "Floor Slab S-1"
                                            ComponentType.COLUMN -> "Columns Group (C1-C8)"
                                            ComponentType.WATERPROOFING -> "Floor Waterproofing"
                                            ComponentType.COLUMN_BEAM -> "Column/Beam"
                                            ComponentType.OTHER -> "Finishing Item"
                                        }
                                    },
                                    label = { Text(t.displayName, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = compName,
                            onValueChange = { compName = it },
                            label = { Text("Component Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        Text("Quick Names:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            currentSuggestions.forEach { s ->
                                SuggestionChip(
                                    onClick = { compName = s.substringBefore(" (") },
                                    label = { Text(s, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = lengthStr,
                                onValueChange = { lengthStr = it },
                                label = { Text("Length (m)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (compType == ComponentType.FLOOR || compType == ComponentType.CEILING || compType == ComponentType.COLUMN_BEAM) {
                                OutlinedTextField(
                                    value = widthStr,
                                    onValueChange = { widthStr = it },
                                    label = { Text("Width (m)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                OutlinedTextField(
                                    value = heightStr,
                                    onValueChange = { heightStr = it },
                                    label = { Text("Height (m)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (compType == ComponentType.WALL) {
                        item {
                            OutlinedTextField(
                                value = thicknessStr,
                                onValueChange = { thicknessStr = it },
                                label = { Text("Thickness (m) e.g. 0.23 / 0.115") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = autoConnect,
                                onCheckedChange = { autoConnect = it }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Auto-connect standard work items (e.g. Plaster, Putty, Paint)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val l = lengthStr.toDoubleOrNull() ?: 0.0
                        val w = widthStr.toDoubleOrNull() ?: 0.0
                        val h = heightStr.toDoubleOrNull() ?: 0.0
                        val thk = thicknessStr.toDoubleOrNull() ?: 0.0
                        viewModel.addComponent(
                            name = compName.trim(),
                            type = compType,
                            length = l,
                            width = if (w > 0) w else thk,
                            height = h,
                            thickness = thk,
                            autoConnectWork = autoConnect
                        )
                        showAddComponentDialog = false
                    },
                    enabled = compName.isNotBlank()
                ) {
                    Text("Add Component")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddComponentDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Quick Setup 4 Walls Dialog
    if (showQuickSetup4WallsDialog) {
        var roomLength by remember { mutableStateOf("4.50") }
        var roomWidth by remember { mutableStateOf("3.80") }
        var roomHeight by remember { mutableStateOf("3.00") }
        var wallThickness by remember { mutableStateOf("0.23") }

        AlertDialog(
            onDismissRequest = { showQuickSetup4WallsDialog = false },
            title = { Text("Quick 4-Wall Setup", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Automatically creates North Wall, South Wall, East Wall, West Wall, and Floor with connected Plaster, Putty, Painting, and Tile work.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roomLength,
                            onValueChange = { roomLength = it },
                            label = { Text("Length (m)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = roomWidth,
                            onValueChange = { roomWidth = it },
                            label = { Text("Width (m)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = roomHeight,
                            onValueChange = { roomHeight = it },
                            label = { Text("Height (m)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wallThickness,
                            onValueChange = { wallThickness = it },
                            label = { Text("Wall Thk (m)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val l = roomLength.toDoubleOrNull() ?: 4.5
                        val w = roomWidth.toDoubleOrNull() ?: 3.8
                        val h = roomHeight.toDoubleOrNull() ?: 3.0
                        val thk = wallThickness.toDoubleOrNull() ?: 0.23

                        viewModel.addComponent("North Wall", ComponentType.WALL, l, thk, h, thk, true)
                        viewModel.addComponent("South Wall", ComponentType.WALL, l, thk, h, thk, true)
                        viewModel.addComponent("East Wall", ComponentType.WALL, w, thk, h, thk, true)
                        viewModel.addComponent("West Wall", ComponentType.WALL, w, thk, h, thk, true)
                        viewModel.addComponent("Bedroom Floor", ComponentType.FLOOR, l, w, 0.0, 0.0, true)
                        viewModel.addComponent("Ceiling", ComponentType.CEILING, l, w, 0.0, 0.0, true)

                        showQuickSetup4WallsDialog = false
                    }
                ) {
                    Text("Generate Room Structure")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSetup4WallsDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Duplicate Component Dialog
    showDuplicateComponentDialog?.let { srcComp ->
        var newCompName by remember { mutableStateOf("${srcComp.name} (Copy)") }
        var copyWork by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showDuplicateComponentDialog = null },
            title = { Text("Duplicate Component", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Duplicates dimensions (L: ${srcComp.length}m, H: ${srcComp.height}m, W: ${srcComp.width}m).",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCompName,
                        onValueChange = { newCompName = it },
                        label = { Text("New Component Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = copyWork, onCheckedChange = { copyWork = it })
                        Spacer(Modifier.width(8.dp))
                        Text("Copy connected work activities", fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCompName.isNotBlank()) {
                            viewModel.duplicateComponent(srcComp.id, newCompName.trim(), copyWork)
                            showDuplicateComponentDialog = null
                        }
                    },
                    enabled = newCompName.isNotBlank()
                ) {
                    Text("Duplicate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicateComponentDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Connect Work Item Dialog
    showConnectWorkDialog?.let { targetComp ->
        AlertDialog(
            onDismissRequest = { showConnectWorkDialog = null },
            title = { Text("Connect Work Item", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Attach construction activities to '${targetComp.name}':",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(items) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.connectWorkItem(targetComp.id, item)
                                        showConnectWorkDialog = null
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("${item.unit} • ${item.calculationType.displayName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConnectWorkDialog = null }) { Text("Done") }
            }
        )
    }
}

@Composable
fun ComponentCard(
    component: ComponentEntity,
    viewModel: SiteViewModel,
    onDuplicateClick: () -> Unit,
    onConnectWorkClick: () -> Unit
) {
    val workItems by viewModel.repository.getWorkItemsForComponent(component.id)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (component.type) {
                                    ComponentType.WALL -> Icons.Default.ViewAgenda
                                    ComponentType.FLOOR -> Icons.Default.GridOn
                                    ComponentType.CEILING -> Icons.Default.Roofing
                                    ComponentType.OPENING -> Icons.Default.DoorFront
                                    ComponentType.BEAM -> Icons.Default.ViewAgenda
                                    ComponentType.SLAB -> Icons.Default.Layers
                                    ComponentType.COLUMN -> Icons.Default.Foundation
                                    ComponentType.WATERPROOFING -> Icons.Default.WaterDrop
                                    ComponentType.COLUMN_BEAM -> Icons.Default.ViewColumn
                                    ComponentType.OTHER -> Icons.Default.Category
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = component.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        val dimText = buildString {
                            if (component.length > 0) append("L: ${component.length}m ")
                            if (component.height > 0) append("× H: ${component.height}m ")
                            if (component.width > 0 && component.width != component.thickness) append("× W: ${component.width}m ")
                            if (component.thickness > 0) append("(${component.thickness}m thk)")
                        }
                        Text(
                            text = dimText.ifBlank { component.type.displayName },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Connect Work Item") },
                            onClick = {
                                showMenu = false
                                onConnectWorkClick()
                            },
                            leadingIcon = { Icon(Icons.Default.AddLink, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Component") },
                            onClick = {
                                showMenu = false
                                onDuplicateClick()
                            },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Component", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                viewModel.deleteComponent(component)
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Connected Work Items Section
            Text(
                text = "CONNECTED WORK ACTIVITIES",
                fontSize = 10.sp,
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
                if (workItems.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.clickable { onConnectWorkClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text("Connect Work (Plaster, Putty...)", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    workItems.forEach { wi ->
                        SuggestionChip(
                            onClick = {
                                viewModel.selectComponent(component.id)
                                viewModel.selectComponentWorkItem(wi)
                                viewModel.navigateTo(AppScreen.WORK_ITEM_MEASURE)
                            },
                            label = {
                                Text(
                                    text = "${wi.itemName} (${wi.unit})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            icon = {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.clickable { onConnectWorkClick() }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Work", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
