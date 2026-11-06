package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

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
import com.holagundi.archiman.domain.InvoiceBalance
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ReceiptEntity

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
            InvoiceTaxSummary(invoice.taxJson)
            RecordActions(RecordKind.INVOICE, invoice.id, invoice.number)
            if (invoice.status == "DRAFT") {
                TextButton(onClick = { suite.issue(invoice) }) { Text("Issue ${invoice.number}") }
            }
            if (invoice.status == "ISSUED") {
                InvoicePayment(invoice, receipts, suite)
            }
        }
        receipts.forEach { receipt -> Text("Receipt ${receipt.amount} · ${receipt.note}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("Fee proposals", style = MaterialTheme.typography.titleMedium)
        proposals.forEach { item -> Text("${item.title} · ${item.amount} · ${item.status}") }
        ArchiField(proposal, { proposal = it }, "Proposal title")
        TextButton(onClick = {
            val submitted = listOf(proposal); suite.addProposal(proposal, fee.toDoubleOrNull() ?: 0.0).onSuccess { if (listOf(proposal) == submitted) { proposal = "" } }
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
            RecordActions(RecordKind.VISIT, item.id, item.location.ifBlank { "Site visit" })
        }
        ArchiField(place, { place = it }, "Location")
        ArchiField(inspector, { inspector = it }, "Inspector")
        ArchiField(observation, { observation = it }, "Report", singleLine = false)
        TextButton(onClick = { val submitted = listOf(observation); suite.addInspection(place, inspector, observation).onSuccess { if (listOf(observation) == submitted) { observation = "" } } }) { Text("Save visit", color = MaterialTheme.colorScheme.primary) }
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
            RecordActions(RecordKind.MEETING, meeting.id, meeting.title)
        }
        ArchiField(title, { title = it }, "Title")
        ArchiField(attendees, { attendees = it }, "Attendees")
        ArchiField(discussion, { discussion = it }, "Discussion", singleLine = false)
        TextButton(onClick = { val submitted = listOf(title, discussion); suite.addMeeting(title, attendees, discussion).onSuccess { if (listOf(title, discussion) == submitted) { title = ""; discussion = "" } } }) {
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
        TextButton(onClick = { val submitted = listOf(task); suite.addTask(task).onSuccess { if (listOf(task) == submitted) { task = "" } } }) { Text("Add task", color = MaterialTheme.colorScheme.primary) }
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
            RecordActions(RecordKind.LETTER, letter.id, letter.subject)
        }
        ArchiField(subject, { subject = it }, "Subject")
        ArchiField(body, { body = it }, "Body", singleLine = false)
        TextButton(onClick = { val submitted = listOf(subject, body); suite.addLetter(subject, body).onSuccess { if (listOf(subject, body) == submitted) { subject = ""; body = "" } } }) { Text("Save letter", color = MaterialTheme.colorScheme.primary) }
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
            RecordActions(RecordKind.APPROVAL, item.id, item.title)
            if (item.status == "SUBMITTED") {
                TextButton(onClick = { suite.decideApproval(item.id, "APPROVED") }) { Text("Approve", color = MaterialTheme.colorScheme.primary) }
                TextButton(onClick = { suite.decideApproval(item.id, "RETURNED") }) { Text("Return", color = MaterialTheme.colorScheme.primary) }
            }
        }
        ArchiField(title, { title = it }, "Title")
        ArchiField(type, { type = it }, "Type")
        TextButton(onClick = { val submitted = listOf(title); suite.addApproval(title, type).onSuccess { if (listOf(title) == submitted) { title = "" } } }) { Text("Submit approval", color = MaterialTheme.colorScheme.primary) }
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
    val rates by suite.rates.collectAsStateWithLifecycle()
    val total = lines.sumOf { it.amount }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Running bill")
        Text("$projectCode · approved quantity × stored contractor rate. Values exclude taxes; legacy rows have no inferred rate.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (lines.isEmpty()) Text("Record a measurement first.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        lines.forEach { line ->
            Text("${line.itemName} · ${line.quantity} ${line.unit} × ${line.rate} = ${line.amount}")
        }
        Text("This bill $total", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Text("Rate book", style = MaterialTheme.typography.titleMedium)
        rates.forEach { rate ->
            Text("${rate.itemName} · ${rate.rate} / ${rate.unit}")
            RecordActions(RecordKind.RATE, rate.id, rate.itemName)
        }
        var item by rememberSaveable { mutableStateOf("") }
        var unit by rememberSaveable { mutableStateOf("m²") }
        var rate by rememberSaveable { mutableStateOf("") }
        ArchiField(item, { item = it }, "Measured item")
        ArchiField(unit, { unit = it }, "Unit")
        ArchiField(rate, { rate = it }, "Rate")
        TextButton(onClick = { val submitted = listOf(rate); suite.addBillRate(item, unit, rate.toDoubleOrNull() ?: 0.0).onSuccess { if (listOf(rate) == submitted) { rate = "" } } }) { Text("Save rate", color = MaterialTheme.colorScheme.primary) }
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
            RecordActions(RecordKind.ACTIVITY, activity.id, activity.title)
            if (activity.progress < 100) {
                TextButton(onClick = { suite.advanceActivity(activity.id, (activity.progress + 25).coerceAtMost(100)) }) {
                    Text("Advance", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        ArchiField(title, { title = it }, "Activity")
        TextButton(onClick = { val submitted = listOf(title); suite.addActivity(title).onSuccess { if (listOf(title) == submitted) { title = "" } } }) { Text("Add activity", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
fun ContractsPane(
    suite: SuiteProjectViewModel,
    contractors: List<com.holagundi.archiman.data.local.entity.ContractorEntity>,
) {
    ConsultancyDocumentsPane(agreement = true, contractors = contractors)
}

@Composable
fun BriefPane(suite: SuiteProjectViewModel) {
    val pending by suite.pending.collectAsStateWithLifecycle()
    val saving = "saveBrief" in pending
    val saved by suite.brief.collectAsStateWithLifecycle()
    val company by suite.company.collectAsStateWithLifecycle()
    if (company == null) { Text("Opening practice…"); return }
    var objectives by rememberSaveable(saved?.updatedAt) { mutableStateOf(saved?.clientObjectives.orEmpty()) }
    var requirements by rememberSaveable(saved?.updatedAt) { mutableStateOf(saved?.projectRequirements.orEmpty()) }
    var constraints by rememberSaveable(saved?.updatedAt) { mutableStateOf(saved?.siteConstraints.orEmpty()) }
    val draft = rememberLocalEditorDraft("brief:${company?.practiceName}:${suite.projectIdForFinance}:${saved?.updatedAt}")
    fun persist() { draft.save(org.json.JSONObject().put("objectives",objectives).put("requirements",requirements).put("constraints",constraints).toString()) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Brief")
        Text(draft.status, style = MaterialTheme.typography.bodySmall)
        draft.recovered?.let { recovery -> TextButton(onClick = {
            val json = org.json.JSONObject(recovery); objectives = json.optString("objectives"); requirements = json.optString("requirements"); constraints = json.optString("constraints"); draft.recovered = null
        }) { Text("Resume local draft") } }
        Text("Unsaved edits are kept on this device; Save brief updates the project record.", style = MaterialTheme.typography.bodySmall)
        ArchiField(objectives, { objectives = it; persist() }, "Client objectives", singleLine = false, enabled = !saving)
        ArchiField(requirements, { requirements = it; persist() }, "Requirements", singleLine = false, enabled = !saving)
        ArchiField(constraints, { constraints = it; persist() }, "Site constraints", singleLine = false, enabled = !saving)
        TextButton(enabled = !saving, onClick = { suite.saveBrief(objectives, requirements, constraints).onSuccess { draft.clear() } }) { Text("Save brief") }
        TextButton(enabled = !saving, onClick = { objectives = saved?.clientObjectives.orEmpty(); requirements = saved?.projectRequirements.orEmpty(); constraints = saved?.siteConstraints.orEmpty(); draft.clear() }) { Text("Discard local edits") }
    }
}

@Composable
fun KanbanPane(suite: SuiteProjectViewModel, focusId: Long = 0L) {
    val tasks by suite.tasks.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var focused by rememberSaveable(focusId) { mutableStateOf(focusId) }
    val columns = listOf("TO_DO" to "To do", "DOING" to "Doing", "DONE" to "Done")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Tasks")
        if (focused > 0L) {
            Text("Showing the task opened from your dashboard.")
            TextButton(onClick = { focused = 0L }) { Text("Show all project tasks") }
        }
        ArchiField(title, { title = it }, "Task")
        TextButton(onClick = { val submitted = listOf(title); suite.addTask(title).onSuccess { if (listOf(title) == submitted) { title = "" } } }) { Text("Add to To do") }
        columns.forEach { (status, label) ->
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            tasks.filter { (focused == 0L || it.id == focused) && (it.status == status || (status == "TO_DO" && it.status == "OPEN")) }.forEach { task ->
                GlassSurface(Modifier.fillMaxWidth(), cornerRadius = 12.dp) {
                    Column {
                        Text(task.title)
                        RecordActions(RecordKind.TASK, task.id, task.title)
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
    ConsultancyDocumentsPane(agreement = false)
}

@Composable
fun FeeInvoicePane(suite: SuiteProjectViewModel) {
    val invoices by suite.invoices.collectAsStateWithLifecycle()
    val receipts by suite.receipts.collectAsStateWithLifecycle()
    var title by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Client invoice")
        invoices.forEach { invoice ->
            Text("${invoice.number} · ${invoice.title} · ${invoice.status} · ${invoice.amount}")
            InvoiceTaxSummary(invoice.taxJson)
            RecordActions(RecordKind.INVOICE, invoice.id, invoice.number)
            if (invoice.status == "DRAFT") {
                TextButton(onClick = { suite.issue(invoice) }) { Text("Issue ${invoice.number}") }
            }
            if (invoice.status == "ISSUED") {
                InvoicePayment(invoice, receipts, suite)
            }
        }
        receipts.forEach { receipt ->
            Text("Receipt ${receipt.amount} · ${receipt.note}", style = MaterialTheme.typography.bodySmall)
            if ((receipt.amount > 0 || receipt.tdsAmount > 0) && receipts.none { it.note.startsWith("REVERSAL:${receipt.id}:") }) ReceiptReversal(receipt.id)
        }
        TaxInvoiceForm(suite.projectIdForFinance)
    }
}

@Composable
private fun InvoicePayment(invoice: InvoiceEntity, receipts: List<ReceiptEntity>, suite: SuiteProjectViewModel) {
    val remaining = InvoiceBalance.outstanding(invoice.amount, receipts.filter { it.invoiceId == invoice.id }.map { it.amount + it.tdsAmount })
    var payment by rememberSaveable(invoice.id, remaining.toPlainString()) { mutableStateOf("") }
    var tds by rememberSaveable(invoice.id) { mutableStateOf("") }
    var tdsRef by rememberSaveable(invoice.id) { mutableStateOf("") }
    var note by rememberSaveable(invoice.id) { mutableStateOf("") }
    val parsed = payment.ifBlank { "0" }.toDoubleOrNull()
    val withheld = tds.ifBlank { "0" }.toDoubleOrNull()
    val valid = parsed != null && withheld != null && parsed >= 0 && withheld >= 0 && (withheld == 0.0 || tdsRef.isNotBlank()) && runCatching {
        InvoiceBalance.afterPayment(invoice.amount, receipts.filter { it.invoiceId == invoice.id }.map { it.amount + it.tdsAmount }, parsed + withheld!!)
    }.isSuccess
    Text("Outstanding: ${remaining.toPlainString()}")
    ArchiField(payment, { payment = it }, "Cash received (INR)", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
    ArchiField(tds, { tds = it }, "Actual TDS withheld (INR, optional)", keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
    ArchiField(tdsRef, { tdsRef = it }, "TDS reference / deduction advice")
    ArchiField(note, { note = it }, "Payment note")
    if (payment.isNotBlank() && !valid) {
        Text("Enter a positive amount within the outstanding balance.", color = MaterialTheme.colorScheme.error)
    }
    TextButton(enabled = valid, onClick = {
        val submitted = listOf(payment, tds, tdsRef, note)
        suite.pay(invoice, parsed!!, note, withheld!!, tdsRef).onSuccess { if (listOf(payment, tds, tdsRef, note) == submitted) { payment = ""; tds = ""; tdsRef = ""; note = "" } }
    }) { Text("Record payment for ${invoice.number}") }
}
