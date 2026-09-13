package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CoordinationItemEntity
import com.example.data.local.entity.CoordinationEventEntity
import com.example.data.local.entity.DrawingRevisionEntity
import com.example.data.local.entity.ProjectDrawingEntity
import com.example.data.local.entity.ProjectConsultantEntity
import com.example.domain.CoordinationWorkflow
import com.example.ui.viewmodel.SiteViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class CoordinationPage(val label: String, val type: String?) {
    RFI("RFIs", CoordinationWorkflow.RFI),
    SUBMITTAL("Submittals", CoordinationWorkflow.SUBMITTAL),
    INSTRUCTION("Instructions", CoordinationWorkflow.SITE_INSTRUCTION),
    TEAM("Responsibility", null)
}

@Composable
fun CoordinationScreen(
    viewModel: SiteViewModel,
    consultants: List<ProjectConsultantEntity>,
    records: List<CoordinationItemEntity>,
    drawings: List<ProjectDrawingEntity>,
    revisions: List<DrawingRevisionEntity>
) {
    var page by remember { mutableStateOf(CoordinationPage.RFI) }
    var showAdd by remember { mutableStateOf(false) }
    var transitionItem by remember { mutableStateOf<CoordinationItemEntity?>(null) }
    var detailItem by remember { mutableStateOf<CoordinationItemEntity?>(null) }
    val message by viewModel.coordinationMessage.collectAsState()
    val snackbars = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbars.showSnackbar(it)
            viewModel.clearCoordinationMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbars) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, if (page == CoordinationPage.TEAM) "Add responsibility" else "Add ${page.label}")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = page.ordinal, edgePadding = 8.dp) {
                CoordinationPage.entries.forEach { value ->
                    Tab(
                        selected = page == value,
                        onClick = { page = value },
                        text = { Text(value.label) }
                    )
                }
            }
            if (page == CoordinationPage.TEAM) {
                ResponsibilityMatrix(consultants, viewModel::archiveProjectConsultant)
            } else {
                CoordinationRegister(
                    records = records.filter { it.type == page.type },
                    drawings = drawings,
                    revisions = revisions,
                    onOpen = { detailItem = it },
                    onTransition = { transitionItem = it },
                    onArchive = viewModel::archiveCoordinationItem
                )
            }
        }
    }

    if (showAdd) {
        if (page == CoordinationPage.TEAM) {
            ConsultantDialog(
                onDismiss = { showAdd = false },
                onSave = { name, organisation, discipline, email, phone, phase, responsibility, role ->
                    viewModel.addProjectConsultant(name, organisation, discipline, email, phone, phase, responsibility, role)
                    showAdd = false
                }
            )
        } else {
            CoordinationItemDialog(
                type = requireNotNull(page.type),
                consultants = consultants,
                drawings = drawings,
                revisions = revisions,
                onDismiss = { showAdd = false },
                onSave = { reference, subject, discipline, location, raisedBy, assignedTo, requirement, priority, dueAt, linkedRevisionId ->
                    viewModel.addCoordinationItem(requireNotNull(page.type), reference, subject, discipline, location, raisedBy, assignedTo, requirement, priority, dueAt, linkedRevisionId)
                    showAdd = false
                }
            )
        }
    }

    transitionItem?.let { item ->
        TransitionDialog(
            item = item,
            onDismiss = { transitionItem = null },
            onSave = { status, note ->
                viewModel.transitionCoordinationItem(item, status, note)
                transitionItem = null
            }
        )
    }

    detailItem?.let { item ->
        val eventsFlow = remember(item.id) { viewModel.coordinationEvents(item.id) }
        val events by eventsFlow.collectAsState(initial = emptyList())
        CoordinationDetailDialog(
            item = item,
            events = events,
            drawingLabel = drawingRevisionLabel(item.linkedDrawingRevisionId, drawings, revisions),
            onDismiss = { detailItem = null },
            onUpdateStatus = {
                detailItem = null
                transitionItem = item
            }
        )
    }
}

@Composable
private fun CoordinationRegister(
    records: List<CoordinationItemEntity>,
    drawings: List<ProjectDrawingEntity>,
    revisions: List<DrawingRevisionEntity>,
    onOpen: (CoordinationItemEntity) -> Unit,
    onTransition: (CoordinationItemEntity) -> Unit,
    onArchive: (CoordinationItemEntity) -> Unit
) {
    if (records.isEmpty()) {
        CoordinationEmpty("No records", "Add the first controlled record for this project.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records, key = { it.id }) { item ->
            OutlinedCard(onClick = { onOpen(item) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("${item.referenceNumber} · ${item.subject}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                listOf(item.discipline, item.location).filter(String::isNotBlank).joinToString(" · "),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer) {
                            Text(item.status.replace('_', ' '), modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
                        }
                    }
                    Text(item.questionOrRequirement, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
                    if (item.assignedTo.isNotBlank()) Text("Ball in court: ${item.assignedTo}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    drawingRevisionLabel(item.linkedDrawingRevisionId, drawings, revisions)?.let {
                        Text("Drawing: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item.dueAt?.let {
                        val overdue = it < System.currentTimeMillis() && !CoordinationWorkflow.isClosed(item.status)
                        Text("Due ${formatCoordinationDate(it)}", style = MaterialTheme.typography.labelMedium, color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (item.response.isNotBlank()) Text("Latest response: ${item.response}", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        IconButton(onClick = { onArchive(item) }) { Icon(Icons.Default.Archive, "Archive record") }
                        if (CoordinationWorkflow.allowedNextStatuses(item.type, item.status).isNotEmpty()) {
                            TextButton(onClick = { onTransition(item) }) {
                                Icon(Icons.Default.AssignmentTurnedIn, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Update status")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponsibilityMatrix(consultants: List<ProjectConsultantEntity>, onArchive: (ProjectConsultantEntity) -> Unit) {
    if (consultants.isEmpty()) {
        CoordinationEmpty("No responsibility assignments", "Add consultants, disciplines and RACI responsibility before issuing coordination records.")
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(consultants, key = { it.id }) { item ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(item.name, fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Column {
                            Text(listOf(item.organisation, item.discipline, item.phase).filter(String::isNotBlank).joinToString(" · "))
                            Text(item.responsibility)
                            if (item.email.isNotBlank() || item.phone.isNotBlank()) Text(listOf(item.email, item.phone).filter(String::isNotBlank).joinToString(" · "))
                        }
                    },
                    leadingContent = { Icon(Icons.Default.Groups, null) },
                    trailingContent = {
                        Column(horizontalAlignment = Alignment.End) {
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                Text(item.raciRole.take(1), modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp))
                            }
                            IconButton(onClick = { onArchive(item) }) { Icon(Icons.Default.Archive, "Archive responsibility") }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CoordinationEmpty(title: String, body: String) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.AssignmentTurnedIn, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CoordinationItemDialog(
    type: String,
    consultants: List<ProjectConsultantEntity>,
    drawings: List<ProjectDrawingEntity>,
    revisions: List<DrawingRevisionEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, Long?, Long?) -> Unit
) {
    var reference by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var discipline by remember { mutableStateOf("Architectural") }
    var location by remember { mutableStateOf("") }
    var raisedBy by remember { mutableStateOf("") }
    var assignedTo by remember { mutableStateOf("") }
    var requirement by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("NORMAL") }
    var dueDate by remember { mutableStateOf("") }
    var linkedRevisionId by remember { mutableStateOf<Long?>(null) }
    val dueAt = remember(dueDate) { parseCoordinationDate(dueDate) }
    val typeLabel = type.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add $typeLabel") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { CoordinationField(reference, { reference = it }, "Reference number") }
                item { CoordinationField(subject, { subject = it }, "Subject") }
                item { CoordinationField(discipline, { discipline = it }, "Discipline") }
                item { CoordinationField(location, { location = it }, "Location") }
                item { CoordinationField(raisedBy, { raisedBy = it }, "Raised by") }
                item { CoordinationField(assignedTo, { assignedTo = it }, "Ball in court", supporting = consultants.joinToString { it.name }.takeIf(String::isNotBlank)) }
                item { CoordinationField(requirement, { requirement = it }, if (type == CoordinationWorkflow.RFI) "Question" else "Requirement / description", singleLine = false) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("LOW", "NORMAL", "HIGH").forEach { value -> FilterChip(selected = priority == value, onClick = { priority = value }, label = { Text(value) }) }
                    }
                }
                item { CoordinationField(dueDate, { dueDate = it }, "Due date (YYYY-MM-DD)") }
                item {
                    DrawingRevisionPicker(
                        selectedId = linkedRevisionId,
                        drawings = drawings,
                        revisions = revisions,
                        onSelect = { linkedRevisionId = it }
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = reference.isNotBlank() && subject.isNotBlank() && requirement.isNotBlank() && (dueDate.isBlank() || dueAt != null), onClick = { onSave(reference, subject, discipline, location, raisedBy, assignedTo, requirement, priority, dueAt, linkedRevisionId) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ConsultantDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var organisation by remember { mutableStateOf("") }
    var discipline by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf("All phases") }
    var responsibility by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("RESPONSIBLE") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add responsibility") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { CoordinationField(name, { name = it }, "Consultant / lead") }
                item { CoordinationField(organisation, { organisation = it }, "Organisation") }
                item { CoordinationField(discipline, { discipline = it }, "Discipline") }
                item { CoordinationField(phase, { phase = it }, "Project phase") }
                item { CoordinationField(responsibility, { responsibility = it }, "Deliverable / responsibility", singleLine = false) }
                item { CoordinationField(email, { email = it }, "Email") }
                item { CoordinationField(phone, { phone = it }, "Phone") }
                item {
                    Text("RACI role", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("RESPONSIBLE", "ACCOUNTABLE", "CONSULTED", "INFORMED").forEach { value -> FilterChip(selected = role == value, onClick = { role = value }, label = { Text(value.take(1)) }) }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank() && discipline.isNotBlank() && responsibility.isNotBlank(), onClick = { onSave(name, organisation, discipline, email, phone, phase, responsibility, role) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun TransitionDialog(item: CoordinationItemEntity, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    val options = CoordinationWorkflow.allowedNextStatuses(item.type, item.status)
    var selected by remember { mutableStateOf(options.first()) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update ${item.referenceNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current status: ${item.status.replace('_', ' ')}")
                options.forEach { value -> FilterChip(selected = selected == value, onClick = { selected = value }, label = { Text(value.replace('_', ' ')) }) }
                CoordinationField(note, { note = it }, "Response / transition note", singleLine = false)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(selected, note) }) { Text("Update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrawingRevisionPicker(
    selectedId: Long?,
    drawings: List<ProjectDrawingEntity>,
    revisions: List<DrawingRevisionEntity>,
    onSelect: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = drawingRevisionLabel(selectedId, drawings, revisions) ?: "No linked drawing"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Linked drawing revision (optional)") },
            leadingIcon = { Icon(Icons.Default.Link, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("No linked drawing") },
                onClick = {
                    onSelect(null)
                    expanded = false
                }
            )
            revisions.forEach { revision ->
                DropdownMenuItem(
                    text = { Text(requireNotNull(drawingRevisionLabel(revision.id, drawings, revisions))) },
                    onClick = {
                        onSelect(revision.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun CoordinationDetailDialog(
    item: CoordinationItemEntity,
    events: List<CoordinationEventEntity>,
    drawingLabel: String?,
    onDismiss: () -> Unit,
    onUpdateStatus: () -> Unit
) {
    val canUpdate = CoordinationWorkflow.allowedNextStatuses(item.type, item.status).isNotEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.History, null) },
        title = { Text("${item.referenceNumber} · ${item.subject}") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(item.status.replace('_', ' '), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }
                item { DetailValue("Type", item.type.replace('_', ' ')) }
                if (item.discipline.isNotBlank()) item { DetailValue("Discipline", item.discipline) }
                if (item.location.isNotBlank()) item { DetailValue("Location", item.location) }
                if (item.raisedBy.isNotBlank()) item { DetailValue("Raised by", item.raisedBy) }
                if (item.assignedTo.isNotBlank()) item { DetailValue("Ball in court", item.assignedTo) }
                item { DetailValue(if (item.type == CoordinationWorkflow.RFI) "Question" else "Requirement", item.questionOrRequirement) }
                if (item.response.isNotBlank()) item { DetailValue("Latest response", item.response) }
                item { DetailValue("Priority", item.priority) }
                item.dueAt?.let { due -> item { DetailValue("Due", formatCoordinationDate(due)) } }
                drawingLabel?.let { label -> item { DetailValue("Linked drawing revision", label) } }
                item {
                    HorizontalDivider()
                    Text("Activity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
                }
                if (events.isEmpty()) {
                    item { Text("No activity recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(events, key = { it.id }) { event ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(eventLabel(event), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (event.note.isNotBlank()) Text(event.note, style = MaterialTheme.typography.bodySmall)
                            Text(
                                listOf(event.actor, formatCoordinationDateTime(event.occurredAt)).filter(String::isNotBlank).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (canUpdate) TextButton(onClick = onUpdateStatus) { Text("Update status") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun DetailValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun drawingRevisionLabel(
    revisionId: Long?,
    drawings: List<ProjectDrawingEntity>,
    revisions: List<DrawingRevisionEntity>
): String? {
    val revision = revisions.firstOrNull { it.id == revisionId } ?: return null
    val drawing = drawings.firstOrNull { it.id == revision.drawingId }
    return if (drawing == null) {
        "Revision ${revision.revisionCode} · ${revision.fileName}"
    } else {
        "${drawing.drawingNumber} · Rev ${revision.revisionCode} · ${drawing.title}"
    }
}

private fun eventLabel(event: CoordinationEventEntity): String = when {
    event.toStatus == "ARCHIVED" -> "Archived"
    event.fromStatus.isBlank() -> "Created as ${event.toStatus.replace('_', ' ')}"
    else -> "${event.fromStatus.replace('_', ' ')} → ${event.toStatus.replace('_', ' ')}"
}

@Composable
private fun CoordinationField(value: String, onValueChange: (String) -> Unit, label: String, singleLine: Boolean = true, supporting: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supporting?.let { { Text(it, maxLines = 1) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3
    )
}

private fun parseCoordinationDate(value: String): Long? = if (value.isBlank()) null else runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(value)?.time
}.getOrNull()

private fun formatCoordinationDate(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))

private fun formatCoordinationDateTime(value: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))
