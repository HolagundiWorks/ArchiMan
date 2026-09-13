package com.example.ui.screens

import com.example.ui.icons.CarbonIcons

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
private enum class ReportsPage(val label: String) { DAILY("Daily"), ISSUES("Snag / NCR"), INSPECTIONS("Inspections"), MINUTES("Minutes") }

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
    inspections: List<SiteInspectionEntity>,
    dailyReports: List<DailySiteReportEntity>,
    siteIssues: List<SiteIssueEntity>
) {
    var page by remember { mutableStateOf(ReportsPage.DAILY) }
    Column(Modifier.fillMaxSize()) {
        OperationPageSelector(ReportsPage.values().map { it.label }, page.ordinal) { page = ReportsPage.values()[it] }
        when (page) {
            ReportsPage.DAILY -> DailyReportsTab(viewModel, dailyReports)
            ReportsPage.ISSUES -> SiteIssuesTab(viewModel, siteIssues)
            ReportsPage.MINUTES -> MeetingMinutesTab(viewModel, minutes)
            ReportsPage.INSPECTIONS -> SiteInspectionTab(viewModel, inspections)
        }
    }
}

@Composable
private fun OperationPageSelector(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selected,
        edgePadding = 12.dp,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
    ) {
        labels.forEachIndexed { index, label ->
            Tab(
                selected = selected == index,
                onClick = { onSelect(index) },
                text = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
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
                        IconButton(onClick = { viewModel.toggleProjectSchedule(item) }) { Icon(if (item.status == "DONE") CarbonIcons.CheckCircle else CarbonIcons.Event, "Change status", tint = if (item.status == "DONE") MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary) }
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(formatDateTime(item.scheduledAt) + if (item.location.isBlank()) "" else " • ${item.location}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            if (item.notes.isNotBlank()) Text(item.notes, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                        }
                        IconButton(onClick = { viewModel.deleteProjectSchedule(item) }) { Icon(CarbonIcons.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(CarbonIcons.Add, "Add schedule") }
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
                        IconButton(onClick = { viewModel.deleteMeetingMinutes(item) }) { Icon(CarbonIcons.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(CarbonIcons.Add, "Add minutes") }
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
                        IconButton(onClick = { viewModel.closeSiteInspection(item) }) { Icon(if (item.status == "CLOSED") CarbonIcons.CheckCircle else CarbonIcons.ReportProblem, "Change status", tint = severityColor(item.severity)) }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.location, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${formatDateTime(item.inspectionAt)} • ${item.severity} • ${item.status}", color = severityColor(item.severity), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(item.observation, style = MaterialTheme.typography.labelSmall)
                            if (item.correctiveAction.isNotBlank()) Text("Corrective action: ${item.correctiveAction}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            if (item.photoUri != null) Text("Photo attached", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(onClick = { viewModel.deleteSiteInspection(item) }) { Icon(CarbonIcons.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(CarbonIcons.Add, "Add inspection") }
    }
    if (add) InspectionDialog(onDismiss = { add = false }) { location, inspector, observation, severity, corrective, photo ->
        viewModel.addSiteInspection(location, inspector, observation, severity, corrective, photo); add = false
    }
}

@Composable
private fun DailyReportsTab(viewModel: SiteViewModel, reports: List<DailySiteReportEntity>) {
    var add by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (reports.isEmpty()) OperationEmpty("No daily reports", "Capture work completed, labour, materials, delays, safety and tomorrow's plan.")
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reports, key = { it.id }) { item ->
                OperationCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.reportDate)), fontWeight = FontWeight.Bold)
                            Text(item.workCompleted, style = MaterialTheme.typography.bodySmall)
                            if (item.manpower.isNotBlank()) Text("Manpower: ${item.manpower}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.materialsReceived.isNotBlank()) Text("Materials: ${item.materialsReceived}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (item.delaysOrConstraints.isNotBlank()) Text("Constraints: ${item.delaysOrConstraints}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            if (item.photoUri != null) Text("Photo attached", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.deleteDailySiteReport(item) }) { Icon(CarbonIcons.DeleteOutline, "Delete") }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(CarbonIcons.Add, "Add daily report") }
    }
    if (add) DailyReportDialog(onDismiss = { add = false }) { work, weather, manpower, materials, delays, safety, next, by, photo ->
        viewModel.addDailySiteReport(work, weather, manpower, materials, delays, safety, next, by, photo); add = false
    }
}

@Composable
private fun SiteIssuesTab(viewModel: SiteViewModel, issues: List<SiteIssueEntity>) {
    var add by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf<SiteIssueEntity?>(null) }
    Box(Modifier.fillMaxSize()) {
        if (issues.isEmpty()) OperationEmpty("No snag or NCR records", "Record defects and non-conformances, then verify corrective closure.")
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(issues, key = { it.id }) { item ->
                OperationCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                        IconButton(onClick = { if (item.status == "READY_FOR_VERIFICATION") closing = item else viewModel.advanceSiteIssue(item) }, enabled = item.status != "CLOSED") {
                            Icon(if (item.status == "CLOSED") CarbonIcons.Verified else CarbonIcons.BuildCircle, "Advance status", tint = severityColor(item.severity))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("${item.referenceNumber} · ${item.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${item.type} · ${item.severity} · ${item.status.replace('_', ' ')}", color = severityColor(item.severity), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            if (item.location.isNotBlank()) Text(item.location, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.description, style = MaterialTheme.typography.bodySmall)
                            if (item.assignedTo.isNotBlank()) Text("Assigned to: ${item.assignedTo}", style = MaterialTheme.typography.labelSmall)
                            if (item.verificationNote.isNotBlank()) Text("Verified: ${item.verificationNote}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(CarbonIcons.Add, "Add site issue") }
    }
    if (add) SiteIssueDialog(onDismiss = { add = false }) { reference, type, title, location, description, severity, assigned, corrective, evidence ->
        viewModel.addSiteIssue(reference, type, title, location, description, severity, assigned, corrective, evidence); add = false
    }
    closing?.let { issue ->
        var note by remember(issue.id) { mutableStateOf("") }
        FormDialog("Verify ${issue.referenceNumber}", { closing = null }, note.isNotBlank(), { viewModel.advanceSiteIssue(issue, note); closing = null }) {
            Text("Closure is permanent. Record what was checked and the evidence accepted.", style = MaterialTheme.typography.bodySmall)
            FormField(note, { note = it }, "Verification note", singleLine = false)
        }
    }
}

@Composable
fun ProjectDecisionsScreen(viewModel: SiteViewModel, decisions: List<ProjectDecisionEntity>) {
    var add by remember { mutableStateOf(false) }
    var deciding by remember { mutableStateOf<ProjectDecisionEntity?>(null) }
    Box(Modifier.fillMaxSize()) {
        if (decisions.isEmpty()) OperationEmpty("No formal decisions", "Track context, required decision, impact, owner and final outcome.")
        else LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(decisions, key = { it.id }) { item ->
                OperationCard {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${item.referenceNumber} · ${item.title}", fontWeight = FontWeight.Bold)
                                Text("${item.status} · Owner: ${item.owner.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                            if (item.status != "DECIDED") TextButton(onClick = { deciding = item }) { Text("Record decision") }
                        }
                        Text(item.decisionRequired, style = MaterialTheme.typography.bodySmall)
                        if (item.impact.isNotBlank()) Text("Impact: ${item.impact}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.finalDecision.isNotBlank()) Text("Decision: ${item.finalDecision}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        FloatingActionButton(onClick = { add = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp), shape = MaterialTheme.shapes.small) { Icon(CarbonIcons.Add, "Add decision") }
    }
    if (add) DecisionDialog(onDismiss = { add = false }) { ref, title, context, required, impact, requestedFrom, owner -> viewModel.addProjectDecision(ref, title, context, required, impact, requestedFrom, owner); add = false }
    deciding?.let { item ->
        var finalDecision by remember(item.id) { mutableStateOf("") }
        FormDialog("Record final decision", { deciding = null }, finalDecision.isNotBlank(), { viewModel.decideProjectDecision(item, finalDecision); deciding = null }) { FormField(finalDecision, { finalDecision = it }, "Final decision", singleLine = false) }
    }
}

@Composable private fun OperationCard(content: @Composable () -> Unit) { Surface(color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) { Box(Modifier.padding(8.dp)) { content() } } }
@Composable private fun OperationEmpty(title: String, supporting: String) { Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(CarbonIcons.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp)); Spacer(Modifier.height(8.dp)); Text(title, fontWeight = FontWeight.Bold); Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }

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
    FormDialog("Site inspection", onDismiss, location.isNotBlank() && observation.isNotBlank(), { onSave(location, inspector, observation, severity, corrective, photoUri) }) { FormField(location, { location = it }, "Location / member"); FormField(inspector, { inspector = it }, "Inspector"); FormField(observation, { observation = it }, "Observation", singleLine = false); Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("NORMAL", "ATTENTION", "CRITICAL").forEach { value -> FilterChip(selected = severity == value, onClick = { severity = value }, label = { Text(value, fontSize = 9.sp) }) } }; FormField(corrective, { corrective = it }, "Corrective action", singleLine = false); OutlinedButton(onClick = { photoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(CarbonIcons.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text(if (photoUri == null) "Attach optional photo" else "Photo attached") } }
}

@Composable
private fun DailyReportDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String, String, String, String, String?) -> Unit) {
    val context = LocalContext.current
    var work by remember { mutableStateOf("") }; var weather by remember { mutableStateOf("") }; var manpower by remember { mutableStateOf("") }; var materials by remember { mutableStateOf("") }; var delays by remember { mutableStateOf("") }; var safety by remember { mutableStateOf("") }; var next by remember { mutableStateOf("") }; var preparedBy by remember { mutableStateOf("") }; var photo by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }; photo = uri.toString() } }
    FormDialog("Daily site report", onDismiss, work.isNotBlank(), { onSave(work, weather, manpower, materials, delays, safety, next, preparedBy, photo) }) {
        FormField(work, { work = it }, "Work completed", singleLine = false); FormField(weather, { weather = it }, "Weather"); FormField(manpower, { manpower = it }, "Manpower / trades"); FormField(materials, { materials = it }, "Materials received", singleLine = false); FormField(delays, { delays = it }, "Delays or constraints", singleLine = false); FormField(safety, { safety = it }, "Safety observations", singleLine = false); FormField(next, { next = it }, "Next-day plan", singleLine = false); FormField(preparedBy, { preparedBy = it }, "Prepared by"); OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Icon(CarbonIcons.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text(if (photo == null) "Attach optional photo" else "Photo attached") }
    }
}

@Composable
private fun SiteIssueDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String, String, String, String, String?) -> Unit) {
    val context = LocalContext.current
    var reference by remember { mutableStateOf("") }; var type by remember { mutableStateOf("SNAG") }; var title by remember { mutableStateOf("") }; var location by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var severity by remember { mutableStateOf("NORMAL") }; var assigned by remember { mutableStateOf("") }; var corrective by remember { mutableStateOf("") }; var evidence by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { runCatching { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }; evidence = uri.toString() } }
    FormDialog("Add snag / NCR", onDismiss, reference.isNotBlank() && title.isNotBlank() && description.isNotBlank(), { onSave(reference, type, title, location, description, severity, assigned, corrective, evidence) }) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("SNAG", "NCR").forEach { value -> FilterChip(selected = type == value, onClick = { type = value }, label = { Text(value) }) } }
        FormField(reference, { reference = it }, "Reference number"); FormField(title, { title = it }, "Title"); FormField(location, { location = it }, "Location"); FormField(description, { description = it }, "Description", singleLine = false)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) { listOf("NORMAL", "ATTENTION", "CRITICAL").forEach { value -> FilterChip(selected = severity == value, onClick = { severity = value }, label = { Text(value, fontSize = 9.sp) }) } }
        FormField(assigned, { assigned = it }, "Assigned to"); FormField(corrective, { corrective = it }, "Corrective action required", singleLine = false); OutlinedButton(onClick = { picker.launch(arrayOf("image/*", "application/pdf")) }, modifier = Modifier.fillMaxWidth()) { Icon(CarbonIcons.AttachFile, null); Spacer(Modifier.width(6.dp)); Text(if (evidence == null) "Attach optional evidence" else "Evidence attached") }
    }
}

@Composable
private fun DecisionDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String, String, String) -> Unit) {
    var reference by remember { mutableStateOf("") }; var title by remember { mutableStateOf("") }; var context by remember { mutableStateOf("") }; var required by remember { mutableStateOf("") }; var impact by remember { mutableStateOf("") }; var requestedFrom by remember { mutableStateOf("") }; var owner by remember { mutableStateOf("") }
    FormDialog("Add formal decision", onDismiss, reference.isNotBlank() && title.isNotBlank() && required.isNotBlank(), { onSave(reference, title, context, required, impact, requestedFrom, owner) }) {
        FormField(reference, { reference = it }, "Reference number"); FormField(title, { title = it }, "Title"); FormField(context, { context = it }, "Context", singleLine = false); FormField(required, { required = it }, "Decision required", singleLine = false); FormField(impact, { impact = it }, "Impact if delayed", singleLine = false); FormField(requestedFrom, { requestedFrom = it }, "Requested from"); FormField(owner, { owner = it }, "Internal owner")
    }
}

@Composable private fun FormDialog(title: String, onDismiss: () -> Unit, enabled: Boolean, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp), content = content) }, confirmButton = { TextButton(enabled = enabled, onClick = onSave) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }
@Composable private fun FormField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier = Modifier.fillMaxWidth(), singleLine: Boolean = true) { OutlinedTextField(value, onValue, label = { Text(label) }, modifier = modifier, singleLine = singleLine, minLines = if (singleLine) 1 else 2) }

private fun formatDateTime(value: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))
@Composable
private fun severityColor(value: String) = when (value) { "CRITICAL" -> MaterialTheme.colorScheme.error; "ATTENTION" -> MaterialTheme.colorScheme.secondary; else -> MaterialTheme.colorScheme.primary }
