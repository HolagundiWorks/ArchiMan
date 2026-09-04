package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SiteViewModel
import java.util.Locale

private data class BriefQuestion(val code: String, val section: String, val label: String, val required: Boolean = true)

private fun onboardingQuestions(projectType: String): List<BriefQuestion> {
    val common = listOf(
        BriefQuestion("project_purpose", "Intent", "What is the primary purpose and desired outcome?"),
        BriefQuestion("users", "Users", "Who will use the project, and what are their key needs?"),
        BriefQuestion("spaces", "Programme", "List the required spaces, capacities and relationships."),
        BriefQuestion("quality", "Design", "Describe the expected character, quality and performance."),
        BriefQuestion("resource_constraint", "Constraints", "Record client-declared resource constraints affecting scope and material choices.", false),
        BriefQuestion("programme_constraint", "Constraints", "Record key dates, dependencies and programme constraints.", false)
    )
    val specific = when (projectType.lowercase(Locale.getDefault())) {
        "residential" -> listOf(
            BriefQuestion("residents", "Residential", "Household composition and future growth?"),
            BriefQuestion("privacy", "Residential", "Privacy, accessibility and domestic staff requirements?", false)
        )
        "commercial" -> listOf(
            BriefQuestion("business_model", "Commercial", "Business model, occupancy and customer flow?"),
            BriefQuestion("tenant_flexibility", "Commercial", "Tenant, subdivision and expansion flexibility?", false)
        )
        "healthcare" -> listOf(
            BriefQuestion("clinical_flow", "Healthcare", "Clinical flows, infection control and departmental adjacencies?"),
            BriefQuestion("medical_equipment", "Healthcare", "Special equipment and engineering dependencies?", false)
        )
        "hospitality" -> listOf(
            BriefQuestion("guest_profile", "Hospitality", "Guest profile, keys/covers and service standard?"),
            BriefQuestion("boh_flow", "Hospitality", "Back-of-house, service and guest-flow separation?", false)
        )
        "education" -> listOf(
            BriefQuestion("student_capacity", "Education", "Student capacity, age groups and teaching model?"),
            BriefQuestion("shared_facilities", "Education", "Shared, community and after-hours facilities?", false)
        )
        else -> listOf(BriefQuestion("special_requirements", "Project-specific", "What specialist requirements define this project?", false))
    }
    return common + specific
}

@Composable
fun ProjectControlsScreen(
    viewModel: SiteViewModel,
    project: ProjectEntity?,
    responses: List<ProjectOnboardingResponseEntity>,
    approvals: List<ProjectApprovalEntity>,
    backlog: List<ProjectBacklogEntity>
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = page) {
            listOf("Onboarding", "Approvals", "Backlog").forEachIndexed { index, title ->
                Tab(selected = page == index, onClick = { page = index }, text = { Text(title) })
            }
        }
        when (page) {
            0 -> OnboardingRegister(viewModel, project, responses)
            1 -> ApprovalRegister(viewModel, approvals)
            else -> BacklogRegister(viewModel, backlog)
        }
    }
}

@Composable
private fun OnboardingRegister(viewModel: SiteViewModel, project: ProjectEntity?, responses: List<ProjectOnboardingResponseEntity>) {
    val questions = remember(project?.projectType) { onboardingQuestions(project?.projectType ?: "Other") }
    val answers = responses.associateBy { it.questionCode }
    val completed = questions.count { !answers[it.code]?.answer.isNullOrBlank() }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("${project?.projectType ?: "Project"} onboarding", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Template v1 · $completed of ${questions.size} answered", style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
            LinearProgressIndicator(progress = { if (questions.isEmpty()) 0f else completed.toFloat() / questions.size }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        }
        items(questions, key = { it.code }) { question ->
            var answer by remember(question.code, answers[question.code]?.answer) { mutableStateOf(answers[question.code]?.answer.orEmpty()) }
            var clarification by remember(question.code, answers[question.code]?.clarification) { mutableStateOf(answers[question.code]?.clarification.orEmpty()) }
            Surface(border = BorderStroke(1.dp, CarbonGray20), color = CarbonWhite, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(question.section.uppercase(), style = MaterialTheme.typography.labelSmall, color = CarbonBlue60)
                    Text(question.label, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(answer, { answer = it }, label = { Text(if (question.required) "Response*" else "Response") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    OutlinedTextField(clarification, { clarification = it }, label = { Text("Clarification / follow-up") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { viewModel.saveOnboardingResponse(question.code, answer, clarification) }, enabled = !question.required || answer.isNotBlank(), modifier = Modifier.align(Alignment.End)) { Text("Save") }
                }
            }
        }
    }
}

@Composable
private fun ApprovalRegister(viewModel: SiteViewModel, approvals: List<ProjectApprovalEntity>) {
    var adding by remember { mutableStateOf(false) }
    RegisterHeader("Approval register", "Client and technical decisions with a simple audit trail", approvals.count { it.status != "APPROVED" }) { adding = true }
    if (adding) ApprovalDialog(onDismiss = { adding = false }) { type, title, description, phase ->
        viewModel.addProjectApproval(type, title, description, phase); adding = false
    }
    RegisterList(emptyText = "No approvals recorded.", isEmpty = approvals.isEmpty()) {
        items(approvals, key = { it.id }) { item ->
            RegisterCard(item.title, "${item.approvalType} · ${item.phase} · ${item.status}", item.description,
                onToggle = { viewModel.advanceProjectApproval(item) }, toggleLabel = when (item.status) { "PENDING" -> "Submit"; "SUBMITTED" -> "Approve"; else -> "Reopen" },
                onDelete = { viewModel.deleteProjectApproval(item) })
        }
    }
}

@Composable
private fun BacklogRegister(viewModel: SiteViewModel, backlog: List<ProjectBacklogEntity>) {
    var adding by remember { mutableStateOf(false) }
    RegisterHeader("Project backlog", "Pending drawings, decisions, coordination and general actions", backlog.count { it.status == "OPEN" }) { adding = true }
    if (adding) BacklogDialog(onDismiss = { adding = false }) { category, title, description, priority, phase ->
        viewModel.addProjectBacklogItem(category, title, description, priority, phase); adding = false
    }
    RegisterList(emptyText = "No backlog items recorded.", isEmpty = backlog.isEmpty()) {
        items(backlog, key = { it.id }) { item ->
            RegisterCard(item.title, "${item.category.replace('_', ' ')} · ${item.priority} · ${item.status}", item.description,
                onToggle = { viewModel.toggleProjectBacklogItem(item) }, toggleLabel = if (item.status == "CLOSED") "Reopen" else "Close",
                onDelete = { viewModel.deleteProjectBacklogItem(item) })
        }
    }
}

@Composable
private fun RegisterHeader(title: String, subtitle: String, openCount: Int, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("$openCount open · $subtitle", style = MaterialTheme.typography.bodySmall, color = CarbonGray70) }
        FilledTonalButton(onClick = onAdd) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Add") }
    }
}

@Composable
private fun RegisterList(emptyText: String, isEmpty: Boolean, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isEmpty) item { Text(emptyText, modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), color = CarbonGray70) }
        content()
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
private fun RegisterCard(title: String, meta: String, description: String, onToggle: () -> Unit, toggleLabel: String, onDelete: () -> Unit) {
    Surface(border = BorderStroke(1.dp, CarbonGray20), color = CarbonWhite, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(meta, style = MaterialTheme.typography.labelSmall, color = CarbonBlue60)
            if (description.isNotBlank()) Text(description, style = MaterialTheme.typography.bodySmall, color = CarbonGray70)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggle) { Text(toggleLabel) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Delete") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
        OutlinedTextField(value.replace('_', ' '), {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true))
        ExposedDropdownMenu(expanded, { expanded = false }) { options.forEach { DropdownMenuItem({ Text(it.replace('_', ' ')) }, { onChange(it); expanded = false }) } }
    }
}

@Composable
private fun ApprovalDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String) -> Unit) {
    var type by remember { mutableStateOf("CLIENT") }; var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var phase by remember { mutableStateOf("DESIGN") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add approval") }, text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { ChoiceField("Type", type, listOf("CLIENT", "TECHNICAL")) { type = it }; ChoiceField("Phase", phase, listOf("DESIGN", "EXECUTION", "HANDOVER")) { phase = it }; OutlinedTextField(title, { title = it }, label = { Text("Decision / deliverable*") }); OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2) } }, confirmButton = { Button(enabled = title.isNotBlank(), onClick = { onAdd(type, title, description, phase) }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun BacklogDialog(onDismiss: () -> Unit, onAdd: (String, String, String, String, String) -> Unit) {
    var category by remember { mutableStateOf("GENERAL") }; var title by remember { mutableStateOf("") }; var description by remember { mutableStateOf("") }; var priority by remember { mutableStateOf("MEDIUM") }; var phase by remember { mutableStateOf("DESIGN") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add backlog item") }, text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) { ChoiceField("Category", category, listOf("DRAWINGS", "CLIENT_APPROVALS", "TECHNICAL_APPROVALS", "COORDINATION", "GENERAL")) { category = it }; ChoiceField("Priority", priority, listOf("HIGH", "MEDIUM", "LOW")) { priority = it }; ChoiceField("Phase", phase, listOf("DESIGN", "EXECUTION", "HANDOVER")) { phase = it }; OutlinedTextField(title, { title = it }, label = { Text("Action*") }); OutlinedTextField(description, { description = it }, label = { Text("Description") }, minLines = 2) } }, confirmButton = { Button(enabled = title.isNotBlank(), onClick = { onAdd(category, title, description, priority, phase) }) { Text("Add") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
