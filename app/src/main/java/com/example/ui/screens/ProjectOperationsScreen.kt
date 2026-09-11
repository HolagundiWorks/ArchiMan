package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.*
import com.example.ui.viewmodel.SiteViewModel
import java.text.SimpleDateFormat
import java.util.*

private enum class PlanningPage(val label: String) { TASKS("Tasks"), SCHEDULE("Schedule"), SELECTIONS("Selections / PO") }
private enum class ReportsPage(val label: String) { MINUTES("Minutes"), INSPECTIONS("Inspections") }

@Composable
fun ProjectPlanningScreen(
    viewModel: SiteViewModel,
    tasks: List<ProjectTaskEntity>,
    schedules: List<ProjectScheduleEntity>,
    selections: List<ProjectSelectionItemEntity>,
    project: ProjectEntity?
) {
    var page by remember { mutableStateOf(PlanningPage.TASKS) }
    Column(Modifier.fillMaxSize()) {
        OperationPageSelector(PlanningPage.values().map { it.label }, page.ordinal) { page = PlanningPage.values()[it] }
        when (page) {
            PlanningPage.TASKS -> ProjectTasksTab(viewModel, tasks)
            PlanningPage.SCHEDULE -> ProjectScheduleTab(viewModel, schedules)
            PlanningPage.SELECTIONS -> ProjectSelectionsTab(viewModel, project, selections)
        }
    }
}

@Composable
fun ProjectReportsScreen(
    viewModel: SiteViewModel,
    minutes: List<MeetingMinutesEntity>,
    inspections: List<SiteInspectionEntity>
) {
    var page by remember { mutableStateOf(ReportsPage.MINUTES) }
    Column(Modifier.fillMaxSize()) {
        OperationPageSelector(ReportsPage.values().map { it.label }, page.ordinal) { page = ReportsPage.values()[it] }
        when (page) {
            ReportsPage.MINUTES -> MeetingMinutesTab(viewModel, minutes)
            ReportsPage.INSPECTIONS -> SiteInspectionTab(viewModel, inspections)
        }
    }
}

@Composable
private fun OperationPageSelector(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            FilterChip(selected = selected == index, onClick = { onSelect(index) }, label = { Text(label, style = MaterialTheme.typography.labelSmall) })
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun ProjectScheduleTab(viewModel: SiteViewModel, schedules: List<ProjectScheduleEntity>) {
    var add by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (schedules.isEmpty()) OperationEmpty("No scheduled site activity", "Plan meetings, inspections, deliveries and milestones.")
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(schedules, key = { it.id }) { item ->
                OperationCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleProjectSchedule(item) }) { Icon(if (item.status == "DONE") Icons.Default.CheckCircle else Icons.Default.Event, "Change status", tint = if (item.status == "DONE") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary) }
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(formatDateTime(item.scheduledAt) + if (item.location.isBlank()) "" else " • ${item.location}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            if (item.notes.isNotBlank()) Text(item.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                        IconButton(onClick = { viewModel.deleteProjectSchedule(item) }) { Icon(Icons.Default.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(Icons.Default.Add, "Add schedule") }
    }
    if (add) ScheduleDialog(onDismiss = { add = false }) { title, at, location, notes -> viewModel.addProjectSchedule(title, at, location, notes); add = false }
}

@Composable
private fun MeetingMinutesTab(viewModel: SiteViewModel, minutes: List<MeetingMinutesEntity>) {
    var add by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (minutes.isEmpty()) OperationEmpty("No meeting minutes", "Record attendance, discussion, decisions and action items.")
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(minutes, key = { it.id }) { item ->
                OperationCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(formatDateTime(item.meetingAt) + if (item.location.isBlank()) "" else " • ${item.location}", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                            if (item.attendees.isNotBlank()) Text("Attendees: ${item.attendees}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            if (item.decisions.isNotBlank()) Text("Decisions: ${item.decisions}", style = MaterialTheme.typography.labelSmall, maxLines = 2)
                            if (item.actionItems.isNotBlank()) Text("Actions: ${item.actionItems}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                        IconButton(onClick = { viewModel.deleteMeetingMinutes(item) }) { Icon(Icons.Default.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(Icons.Default.Add, "Add minutes") }
    }
    if (add) MinutesDialog(onDismiss = { add = false }) { title, location, attendees, discussion, decisions, actions ->
        viewModel.addMeetingMinutes(title, System.currentTimeMillis(), location, attendees, discussion, decisions, actions); add = false
    }
}

@Composable
private fun SiteInspectionTab(viewModel: SiteViewModel, inspections: List<SiteInspectionEntity>) {
    var add by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (inspections.isEmpty()) OperationEmpty("No site inspection reports", "Record observations, severity and corrective action.")
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(inspections, key = { it.id }) { item ->
                OperationCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        IconButton(onClick = { viewModel.closeSiteInspection(item) }) { Icon(if (item.status == "CLOSED") Icons.Default.CheckCircle else Icons.Default.ReportProblem, "Change status", tint = severityColor(item.severity)) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.location, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${formatDateTime(item.inspectionAt)} • ${item.severity} • ${item.status}", color = severityColor(item.severity), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(item.observation, style = MaterialTheme.typography.labelSmall)
                            if (item.correctiveAction.isNotBlank()) Text("Corrective action: ${item.correctiveAction}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            if (item.photoUri != null) Text("Photo attached", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { viewModel.deleteSiteInspection(item) }) { Icon(Icons.Default.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(Icons.Default.Add, "Add inspection") }
    }
    if (add) InspectionDialog(onDismiss = { add = false }) { location, inspector, observation, severity, corrective, photo ->
        viewModel.addSiteInspection(location, inspector, observation, severity, corrective, photo); add = false
    }
}

@Composable private fun OperationCard(content: @Composable () -> Unit) { Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) { Box(Modifier.padding(8.dp)) { content() } } }
@Composable private fun OperationEmpty(title: String, supporting: String) { Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold); Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }

@Composable
private fun ScheduleDialog(onDismiss: () -> Unit, onSave: (String, Long, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }; var time by remember { mutableStateOf("09:00") }; var location by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    val at = remember(date, time) { runCatching { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }.parse("$date $time")?.time }.getOrNull() }
    FormDialog("Add schedule", onDismiss, title.isNotBlank() && at != null, { onSave(title, at ?: 0L, location, notes) }) {
        FormField(title, { title = it }, "Title"); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { FormField(date, { date = it }, "Date YYYY-MM-DD", Modifier.weight(1f)); FormField(time, { time = it }, "Time HH:mm", Modifier.weight(0.7f)) }; FormField(location, { location = it }, "Location"); FormField(notes, { notes = it }, "Notes", singleLine = false)
    }
}

@Composable
private fun MinutesDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String, String) -> Unit) {
    var title by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }; var attendees by remember { mutableStateOf("") }; var discussion by remember { mutableStateOf("") }; var decisions by remember { mutableStateOf("") }; var actions by remember { mutableStateOf("") }
    FormDialog("Meeting minutes", onDismiss, title.isNotBlank(), { onSave(title, location, attendees, discussion, decisions, actions) }) { FormField(title, { title = it }, "Meeting title"); FormField(location, { location = it }, "Location"); FormField(attendees, { attendees = it }, "Attendees"); FormField(discussion, { discussion = it }, "Discussion", singleLine = false); FormField(decisions, { decisions = it }, "Decisions", singleLine = false); FormField(actions, { actions = it }, "Action items", singleLine = false) }
}

@Composable
private fun InspectionDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String, String?) -> Unit) {
    val context = LocalContext.current
    var location by remember { mutableStateOf("") }; var inspector by remember { mutableStateOf("") }; var observation by remember { mutableStateOf("") }; var severity by remember { mutableStateOf("NORMAL") }; var corrective by remember { mutableStateOf("") }; var photoUri by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            photoUri = uri.toString()
        }
    }
    FormDialog("Site inspection", onDismiss, location.isNotBlank() && observation.isNotBlank(), { onSave(location, inspector, observation, severity, corrective, photoUri) }) { FormField(location, { location = it }, "Location / member"); FormField(inspector, { inspector = it }, "Inspector"); FormField(observation, { observation = it }, "Observation", singleLine = false); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("NORMAL", "ATTENTION", "CRITICAL").forEach { value -> FilterChip(selected = severity == value, onClick = { severity = value }, label = { Text(value, fontSize = 9.sp) }) } }; FormField(corrective, { corrective = it }, "Corrective action", singleLine = false); OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text(if (photoUri == null) "Attach optional photo" else "Photo attached") } }
}

@Composable private fun FormDialog(title: String, onDismiss: () -> Unit, enabled: Boolean, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content) }, confirmButton = { TextButton(enabled = enabled, onClick = onSave) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }
@Composable private fun FormField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth(), singleLine: Boolean = true) { OutlinedTextField(value, onValue, label = { Text(label) }, modifier = modifier, singleLine = singleLine, minLines = if (singleLine) 1 else 2) }

private fun formatDateTime(value: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))
@Composable
private fun severityColor(value: String) = when (value) { "CRITICAL" -> MaterialTheme.colorScheme.error; "ATTENTION" -> MaterialTheme.colorScheme.secondary; else -> MaterialTheme.colorScheme.primary }
