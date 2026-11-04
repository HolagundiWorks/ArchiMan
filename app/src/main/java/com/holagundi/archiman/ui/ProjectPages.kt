package com.holagundi.archiman.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.ScreenTitle

@Composable
fun PathPane(projectCode: String, projectType: String, suite: SuiteProjectViewModel) {
    val stages by suite.stages.collectAsStateWithLifecycle()
    val invoices by suite.invoices.collectAsStateWithLifecycle()
    val receipts by suite.receipts.collectAsStateWithLifecycle()
    val proposals by suite.proposals.collectAsStateWithLifecycle()
    val settings by suite.settings.collectAsStateWithLifecycle()
    var fee by rememberSaveable { mutableStateOf("") }
    var proposal by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(suite.nextStep(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Text("$projectCode · $projectType · ${settings?.persona ?: "PMC"}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Row {
            TextButton(onClick = { suite.setPersona("PMC") }) { Text("PMC", color = MaterialTheme.colorScheme.primary) }
            TextButton(onClick = { suite.setPersona("Contractor") }) { Text("Contractor", color = MaterialTheme.colorScheme.primary) }
            TextButton(onClick = suite::assignStages) { Text("Assign stages", color = MaterialTheme.colorScheme.primary) }
        }
        stages.forEach { stage ->
            GlassSurface(Modifier.fillMaxWidth()) {
                Column {
                    Text("${stage.orderIndex + 1}. ${stage.title}", style = MaterialTheme.typography.titleMedium)
                    Text(stage.status, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (stage.status == "OPEN") {
                        ArchiField(fee, { fee = it }, "Stage fee")
                        TextButton(onClick = { suite.bill(stage, fee.toDoubleOrNull() ?: stage.feeAmount) }) {
                            Text("Bill this stage", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        invoices.forEach { invoice ->
            Text("${invoice.number} · ${invoice.title} · ${invoice.status} · ${invoice.amount}")
            if (invoice.status == "DRAFT") {
                TextButton(onClick = { suite.issue(invoice) }) { Text("Issue ${invoice.number}") }
            }
            if (invoice.status == "ISSUED") {
                TextButton(onClick = { suite.pay(invoice, invoice.amount, "Stage payment") }) { Text("Record payment", color = MaterialTheme.colorScheme.primary) }
            }
        }
        receipts.forEach { receipt -> Text("Receipt ${receipt.amount} · ${receipt.note}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("Fee proposals", style = MaterialTheme.typography.titleMedium)
        proposals.forEach { item -> Text("${item.title} · ${item.amount} · ${item.status}") }
        ArchiField(proposal, { proposal = it }, "Proposal title")
        TextButton(onClick = {
            suite.addProposal(proposal, fee.toDoubleOrNull() ?: 0.0)
            proposal = ""
        }) { Text("Save proposal", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun SitePane(suite: SuiteProjectViewModel) {
    val inspections by suite.inspections.collectAsStateWithLifecycle()
    var place by rememberSaveable { mutableStateOf("") }
    var inspector by rememberSaveable { mutableStateOf("") }
    var observation by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Site visits")
        Text("Inspection reports. Photos come in the next pass.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        inspections.forEach { item ->
            Text("${item.location.ifBlank { "Site" }} · ${item.inspector}", style = MaterialTheme.typography.titleMedium)
            Text(item.observation, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ArchiField(place, { place = it }, "Location")
        ArchiField(inspector, { inspector = it }, "Inspector")
        ArchiField(observation, { observation = it }, "Report", singleLine = false)
        TextButton(onClick = { suite.addInspection(place, inspector, observation); observation = "" }) { Text("Save visit", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun MeetingsPane(suite: SuiteProjectViewModel) {
    val meetings by suite.meetings.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var attendees by rememberSaveable { mutableStateOf("") }
    var discussion by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Meetings", style = MaterialTheme.typography.titleLarge)
        meetings.forEach { meeting ->
            Text(meeting.title, style = MaterialTheme.typography.titleMedium)
            Text(meeting.attendees, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(meeting.discussion)
        }
        ArchiField(title, { title = it }, "Title")
        ArchiField(attendees, { attendees = it }, "Attendees")
        ArchiField(discussion, { discussion = it }, "Discussion", singleLine = false)
        TextButton(onClick = { suite.addMeeting(title, attendees, discussion); title = ""; discussion = "" }) {
            Text("Save minutes", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun TimePane(suite: SuiteProjectViewModel) {
    val time by suite.time.collectAsStateWithLifecycle()
    val tasks by suite.tasks.collectAsStateWithLifecycle()
    var person by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("60") }
    var notes by rememberSaveable { mutableStateOf("") }
    var task by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Time", style = MaterialTheme.typography.titleLarge)
        time.forEach { entry -> Text("${entry.person} · ${entry.minutes} min · ${entry.notes}") }
        ArchiField(person, { person = it }, "Person")
        ArchiField(minutes, { minutes = it }, "Minutes")
        ArchiField(notes, { notes = it }, "Notes")
        TextButton(onClick = { suite.addTime(person, minutes.toIntOrNull() ?: 0, notes) }) { Text("Save time", color = MaterialTheme.colorScheme.primary) }
        Text("Tasks", style = MaterialTheme.typography.titleMedium)
        tasks.forEach { item -> Text("${item.title} · ${item.status}") }
        ArchiField(task, { task = it }, "Task")
        TextButton(onClick = { suite.addTask(task); task = "" }) { Text("Add task", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun LettersPane(projectId: Long?, suite: SuiteProjectViewModel) {
    val letters by suite.letters.collectAsStateWithLifecycle()
    val company by suite.company.collectAsStateWithLifecycle()
    var subject by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Letters")
        GlassSurface(Modifier.fillMaxWidth()) {
            Column {
                Text(company?.practiceName ?: "Practice letterhead", style = MaterialTheme.typography.titleMedium)
                Text(company?.let { "${it.address}, ${it.city} ${it.pinCode}" } ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(company?.let { "CoA ${it.coaRegistrationNumber}" } ?: "", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
        letters.filter { projectId == null || it.projectId == projectId || it.projectId == null }.forEach { letter ->
            Text(letter.subject, style = MaterialTheme.typography.titleMedium)
            Text(letter.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        ArchiField(subject, { subject = it }, "Subject")
        ArchiField(body, { body = it }, "Body", singleLine = false)
        TextButton(onClick = { suite.addLetter(subject, body); subject = ""; body = "" }) { Text("Save letter", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun ApprovalsPane(suite: SuiteProjectViewModel) {
    val approvals by suite.approvals.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Drawing") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Approvals", style = MaterialTheme.typography.titleLarge)
        approvals.forEach { item ->
            Text("${item.approvalType} · ${item.title} · ${item.status}")
            if (item.status == "SUBMITTED") {
                TextButton(onClick = { suite.decideApproval(item.id, "APPROVED") }) { Text("Approve", color = MaterialTheme.colorScheme.primary) }
                TextButton(onClick = { suite.decideApproval(item.id, "RETURNED") }) { Text("Return", color = MaterialTheme.colorScheme.primary) }
            }
        }
        ArchiField(title, { title = it }, "Title")
        ArchiField(type, { type = it }, "Type")
        TextButton(onClick = { suite.addApproval(title, type); title = "" }) { Text("Submit approval", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun QuantityEntryPane(title: String, trades: List<String>, projectCode: String, suite: SuiteProjectViewModel, rcc: Boolean) {
    val members by suite.rcc.collectAsStateWithLifecycle()
    var trade by rememberSaveable { mutableStateOf(trades.first()) }
    var mark by rememberSaveable { mutableStateOf("") }
    var nos by rememberSaveable { mutableStateOf("1") }
    var length by rememberSaveable { mutableStateOf("0") }
    var breadth by rememberSaveable { mutableStateOf("0") }
    var height by rememberSaveable { mutableStateOf("0") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text("Saved rows land on the measurement sheet that M-Book and the estimate read.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (rcc) {
            members.forEach { member ->
                Text("${member.elementType} ${member.mark} · ${member.quantity} m³")
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            trades.forEach { name ->
                TextButton(onClick = { trade = name }) {
                    Text(name, color = if (trade == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (rcc) ArchiField(mark, { mark = it }, "Mark")
        ArchiField(nos, { nos = it }, "Nos")
        ArchiField(length, { length = it }, "Length")
        ArchiField(breadth, { breadth = it }, "Breadth")
        ArchiField(height, { height = it }, "Height")
        TextButton(onClick = {
            val n = nos.toDoubleOrNull() ?: 0.0
            val l = length.toDoubleOrNull() ?: 0.0
            val b = breadth.toDoubleOrNull() ?: 0.0
            val h = height.toDoubleOrNull() ?: 0.0
            if (rcc) suite.addRcc(projectCode, trade, mark, n, l, b, h) else suite.addCivil(projectCode, trade, n, l, b, h)
        }) { Text("Save $trade", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun EstimatePane(projectCode: String, suite: SuiteProjectViewModel) {
    val lines by suite.estimate.collectAsStateWithLifecycle()
    val total = lines.sumOf { it.amount }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Running bill")
        Text("$projectCode · measured quantity × rate. A rate that is not in the book stays at zero.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (lines.isEmpty()) Text("Record a measurement first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        lines.forEach { line ->
            Text("${line.itemName} · ${line.quantity} ${line.unit} × ${line.rate} = ${line.amount}")
        }
        Text("This bill $total", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        var item by rememberSaveable { mutableStateOf("") }
        var unit by rememberSaveable { mutableStateOf("m²") }
        var rate by rememberSaveable { mutableStateOf("") }
        ArchiField(item, { item = it }, "Measured item")
        ArchiField(unit, { unit = it }, "Unit")
        ArchiField(rate, { rate = it }, "Rate")
        TextButton(onClick = { suite.addBillRate(item, unit, rate.toDoubleOrNull() ?: 0.0); rate = "" }) { Text("Save rate", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun SchedulePane(suite: SuiteProjectViewModel) {
    val activities by suite.activities.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Schedule", style = MaterialTheme.typography.titleLarge)
        activities.forEach { activity ->
            Text("${activity.title} · ${activity.status} · ${activity.progress}%")
            if (activity.progress < 100) {
                TextButton(onClick = { suite.advanceActivity(activity.id, (activity.progress + 25).coerceAtMost(100)) }) {
                    Text("Advance", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        ArchiField(title, { title = it }, "Activity")
        TextButton(onClick = { suite.addActivity(title); title = "" }) { Text("Add activity", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun ContractsPane(
    suite: SuiteProjectViewModel,
    contractors: List<com.holagundi.archiman.data.local.entity.ContractorEntity>,
) {
    val contracts by suite.contracts.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var party by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Contracts")
        Text("Parties on this project", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        contractors.forEach { contractor -> Text("${contractor.name} · ${contractor.contractorType}") }
        contracts.forEach { contract -> Text("${contract.title} · ${contract.party} · ${contract.kind} · ${contract.amount} · ${contract.status}") }
        ArchiField(title, { title = it }, "Contract")
        ArchiField(party, { party = it }, "Party")
        ArchiField(amount, { amount = it }, "Amount")
        TextButton(onClick = { suite.addContract(title, party, "Item rate", amount.toDoubleOrNull() ?: 0.0); title = "" }) {
            Text("Save contract", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BriefPane(suite: SuiteProjectViewModel) {
    val saved by suite.brief.collectAsStateWithLifecycle()
    var objectives by remember(saved?.updatedAt) { mutableStateOf(saved?.clientObjectives.orEmpty()) }
    var requirements by remember(saved?.updatedAt) { mutableStateOf(saved?.projectRequirements.orEmpty()) }
    var constraints by remember(saved?.updatedAt) { mutableStateOf(saved?.siteConstraints.orEmpty()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Brief")
        ArchiField(objectives, { objectives = it }, "Client objectives", singleLine = false)
        ArchiField(requirements, { requirements = it }, "Requirements", singleLine = false)
        ArchiField(constraints, { constraints = it }, "Site constraints", singleLine = false)
        TextButton(onClick = { suite.saveBrief(objectives, requirements, constraints) }) { Text("Save brief", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun KanbanPane(suite: SuiteProjectViewModel) {
    val tasks by suite.tasks.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    val columns = listOf("TO_DO" to "To do", "DOING" to "Doing", "DONE" to "Done")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Tasks")
        ArchiField(title, { title = it }, "Task")
        TextButton(onClick = { suite.addTask(title); title = "" }) { Text("Add to To do") }
        columns.forEach { (status, label) ->
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            tasks.filter { it.status == status || (status == "TO_DO" && it.status == "OPEN") }.forEach { task ->
                GlassSurface(Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    Column {
                        Text(task.title)
                        val next = when (status) {
                            "TO_DO" -> "DOING"
                            "DOING" -> "DONE"
                            else -> "TO_DO"
                        }
                        val nextLabel = when (next) {
                            "DOING" -> "Doing"
                            "DONE" -> "Done"
                            else -> "To do"
                        }
                        TextButton(onClick = { suite.moveTask(task.id, if (task.status == "OPEN") "DOING" else next) }) {
                            Text(if (status == "DONE") "Reopen ${task.title}" else "Move ${task.title} to $nextLabel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProposalPane(suite: SuiteProjectViewModel) {
    val proposals by suite.proposals.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Proposal")
        proposals.forEach { item -> Text("${item.title} · ${item.amount} · ${item.status}") }
        ArchiField(title, { title = it }, "Proposal")
        ArchiField(amount, { amount = it }, "Fee")
        TextButton(onClick = { suite.addProposal(title, amount.toDoubleOrNull() ?: 0.0); title = "" }) { Text("Save proposal", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun FeeInvoicePane(suite: SuiteProjectViewModel) {
    val invoices by suite.invoices.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Client invoice")
        invoices.forEach { invoice ->
            Text("${invoice.number} · ${invoice.title} · ${invoice.status} · ${invoice.amount}")
            if (invoice.status == "DRAFT") {
                TextButton(onClick = { suite.issue(invoice) }) { Text("Issue ${invoice.number}") }
            }
            if (invoice.status == "ISSUED") {
                TextButton(onClick = { suite.pay(invoice, invoice.amount, "Fee received") }) { Text("Record payment for ${invoice.number}") }
            }
        }
        ArchiField(title, { title = it }, "Invoice for")
        ArchiField(amount, { amount = it }, "Amount")
        TextButton(onClick = { suite.addFeeInvoice(title, amount.toDoubleOrNull() ?: 0.0); title = "" }) { Text("Save invoice", color = MaterialTheme.colorScheme.primary) }
    }
}
