@file:OptIn(ExperimentalFoundationApi::class)

package com.holagundi.archiman.ui

import android.content.ClipData
import android.content.ClipDescription
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.ui.theme.ArchiCard
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.CarbonIcons
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.ScreenTitle
import com.holagundi.archiman.domain.InvoiceBalance
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ReceiptEntity
import com.holagundi.archiman.data.local.entity.ProjectTaskEntity

private const val TASK_DRAG_LABEL = "archiman.task"

@Composable
fun SitePane(suite: SuiteProjectViewModel) {
    val inspections by suite.inspections.collectAsStateWithLifecycle()
    var place by rememberSaveable { mutableStateOf("") }
    var inspector by rememberSaveable { mutableStateOf("") }
    var observation by rememberSaveable { mutableStateOf("") }
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle("Site visits") }
        item {
            Text("Inspection reports. Photos come in the next pass.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        items(inspections, key = { it.id }) { item ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${item.location.ifBlank { "Site" }} · ${item.inspector}", style = MaterialTheme.typography.titleMedium)
                Text(item.observation, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RecordActions(RecordKind.VISIT, item.id, item.location.ifBlank { "Site visit" })
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArchiField(place, { place = it }, "Location")
                ArchiField(inspector, { inspector = it }, "Inspector")
                ArchiField(observation, { observation = it }, "Report", singleLine = false)
                TextButton(onClick = { val submitted = listOf(observation); suite.addInspection(place, inspector, observation).onSuccess { if (listOf(observation) == submitted) { observation = "" } } }) { Text("Save visit", color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
fun LettersPane(projectId: Long?, suite: SuiteProjectViewModel) {
    val letters by suite.letters.collectAsStateWithLifecycle()
    val company by suite.company.collectAsStateWithLifecycle()
    var subject by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    val visibleLetters = letters.filter { projectId == null || it.projectId == projectId || it.projectId == null }
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle("Letters") }
        item {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column {
                    Text(company?.practiceName ?: "Practice letterhead", style = MaterialTheme.typography.titleMedium)
                    Text(company?.let { "${it.address}, ${it.city} ${it.pinCode}" } ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(company?.let { "CoA ${it.coaRegistrationNumber}" } ?: "", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        items(visibleLetters, key = { it.id }) { letter ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(letter.subject, style = MaterialTheme.typography.titleMedium)
                Text(letter.body, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RecordActions(RecordKind.LETTER, letter.id, letter.subject)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ArchiField(subject, { subject = it }, "Subject")
                ArchiField(body, { body = it }, "Body", singleLine = false)
                TextButton(onClick = { val submitted = listOf(subject, body); suite.addLetter(subject, body).onSuccess { if (listOf(subject, body) == submitted) { subject = ""; body = "" } } }) { Text("Save letter", color = MaterialTheme.colorScheme.primary) }
            }
        }
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
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle("Tasks")
        Text(
            "Drag a task into another column to move it.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        if (focused > 0L) {
            Text("Showing the task opened from your dashboard.")
            TextButton(onClick = { focused = 0L }) { Text("Show all project tasks") }
        }
        ArchiField(title, { title = it }, "Task")
        TextButton(onClick = {
            val submitted = listOf(title)
            suite.addTask(title).onSuccess { if (listOf(title) == submitted) title = "" }
        }) { Text("Add to To do") }
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            columns.forEach { (status, label) ->
                val columnTasks = tasks.filter {
                    (focused == 0L || it.id == focused) && (it.status == status || (status == "TO_DO" && it.status == "OPEN"))
                }
                KanbanColumn(
                    status = status,
                    label = label,
                    tasks = columnTasks,
                    onDropTask = { taskId -> suite.moveTask(taskId, status) },
                    onMove = { taskId, next -> suite.moveTask(taskId, next) },
                    modifier = Modifier.width(280.dp).fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun KanbanColumn(
    status: String,
    label: String,
    tasks: List<ProjectTaskEntity>,
    onDropTask: (Long) -> Unit,
    onMove: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var hovering by remember { mutableStateOf(false) }
    val dropTarget = remember(status, onDropTask) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                hovering = false
                val text = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString().orEmpty()
                val taskId = text.toLongOrNull() ?: return false
                onDropTask(taskId)
                return true
            }

            override fun onEntered(event: DragAndDropEvent) {
                hovering = true
            }

            override fun onExited(event: DragAndDropEvent) {
                hovering = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                hovering = false
            }
        }
    }
    Surface(
        modifier = modifier
            .border(
                width = if (hovering) 2.dp else 1.dp,
                color = if (hovering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RectangleShape,
            )
            .dragAndDropTarget(
                shouldStartDragAndDrop = { event ->
                    event.mimeTypes().contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                },
                target = dropTarget,
            ),
        color = if (hovering) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        shape = RectangleShape,
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            Text(
                "${tasks.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
            )
            LazyColumn(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (tasks.isEmpty()) {
                    item {
                        Text(
                            "Drop tasks here",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                items(tasks, key = { it.id }) { task ->
                    TaskTile(
                        task = task,
                        column = status,
                        onMove = { onMove(task.id, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskTile(
    task: ProjectTaskEntity,
    column: String,
    onMove: (String) -> Unit,
) {
    val backStatus = when (column) {
        "DOING" -> "TO_DO"
        "DONE" -> "DOING"
        else -> null
    }
    val forwardStatus = when (column) {
        "TO_DO" -> "DOING"
        "DOING" -> "DONE"
        "DONE" -> "TO_DO"
        else -> "DOING"
    }
    val forwardLabel = when (forwardStatus) {
        "DOING" -> "Move to Doing"
        "DONE" -> "Mark done"
        else -> "Reopen to To do"
    }
    val backLabel = when (backStatus) {
        "TO_DO" -> "Move to To do"
        "DOING" -> "Move to Doing"
        else -> null
    }
    ArchiCard(
        Modifier
            .fillMaxWidth()
            .dragAndDropSource {
                DragAndDropTransferData(
                    ClipData.newPlainText(TASK_DRAG_LABEL, task.id.toString()),
                )
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                CarbonIcons.Assignment,
                contentDescription = "Drag task",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                task.title,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (backStatus != null && backLabel != null) {
                IconButton(onClick = { onMove(backStatus) }) {
                    Icon(CarbonIcons.ArrowBack, backLabel, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = { onMove(if (task.status == "OPEN") "DOING" else forwardStatus) }) {
                Icon(
                    imageVector = when (forwardStatus) {
                        "DONE" -> CarbonIcons.Check
                        "TO_DO" -> CarbonIcons.Unarchive
                        else -> CarbonIcons.ArrowForward
                    },
                    contentDescription = forwardLabel,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            RecordActions(RecordKind.TASK, task.id, task.title)
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
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle("Client invoice") }
        items(invoices, key = { it.id }) { invoice ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        }
        items(receipts, key = { "receipt-${it.id}" }) { receipt ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Receipt ${receipt.amount} · ${receipt.note}", style = MaterialTheme.typography.bodySmall)
                if ((receipt.amount > 0 || receipt.tdsAmount > 0) && receipts.none { it.note.startsWith("REVERSAL:${receipt.id}:") }) ReceiptReversal(receipt.id)
            }
        }
        item { TaxInvoiceForm(suite.projectIdForFinance) }
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
