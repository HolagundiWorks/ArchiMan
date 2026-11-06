@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.holagundi.archiman.data.*
import com.holagundi.archiman.domain.*
import com.holagundi.archiman.ui.theme.ArchiCard
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ChoiceChip
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ConsultancyViewModel @Inject constructor(handle: SavedStateHandle, private val repository: ConsultancyRepository, practice: PracticeRepository) : ViewModel() {
    private val projectId: Long = checkNotNull(handle["projectId"])
    val draftProjectId get() = projectId
    val company = practice.observeCompany().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val proposals = repository.observe(projectId, false).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val agreements = repository.observe(projectId, true).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    private fun operation(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true; _notice.value = null
        viewModelScope.launch {
            try { block() } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { _notice.value = error.message ?: "Document operation failed."
            } finally { _busy.value = false }
        }
    }
    fun share(context: android.content.Context, record: ConsultancyRecord) = operation {
        val (company,style)=withContext(Dispatchers.IO) { repository.branding() }
        shareConsultancyPdf(context,record.title,record.documentText.ifBlank { "Legacy record: ${record.title}; amount ${record.amount}" },company,style)
    }
    fun newDraft(agreement: Boolean, ready: (ConsultancyInput) -> Unit) = operation {
        ready(withContext(Dispatchers.IO) { repository.newInput(projectId, agreement, ConsultancyService.ARCHITECTURE) })
    }
    fun save(agreement: Boolean, input: ConsultancyInput, expected: ConsultancyRecord?, done: (Long) -> Unit) = operation {
        val id = withContext(Dispatchers.IO) { repository.save(projectId, agreement, input, expected) }
        _notice.value = "Draft saved."; done(id)
    }
    fun issue(record: ConsultancyRecord, reviewed: Boolean, done: () -> Unit) = operation {
        withContext(Dispatchers.IO) { repository.issue(record, reviewed) }; _notice.value = "Issued for acceptance; signatures are still required."; done()
    }
    fun copy(record: ConsultancyRecord, asAgreement: Boolean, done: () -> Unit) = operation {
        withContext(Dispatchers.IO) { repository.copy(record, asAgreement) }
        _notice.value = if (asAgreement && !record.agreement) "Draft created in Agreements with the same scope and fees." else "A new draft revision was created."; done()
    }
    fun delete(record: ConsultancyRecord, done: () -> Unit) = operation { withContext(Dispatchers.IO) { repository.delete(record) }; done() }
}

@Composable
fun ConsultancyDocumentsPane(agreement: Boolean, contractors: List<com.holagundi.archiman.data.local.entity.ContractorEntity> = emptyList(), viewModel: ConsultancyViewModel = hiltViewModel()) {
    val company by viewModel.company.collectAsStateWithLifecycle()
    if (company == null) { Text("Opening practice…"); return }
    val records by (if (agreement) viewModel.agreements else viewModel.proposals).collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorJson by rememberSaveable { mutableStateOf<String?>(null) }
    var initialJson by rememberSaveable { mutableStateOf("") }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var originalJson by rememberSaveable { mutableStateOf<String?>(null) }
    var originalBody by rememberSaveable { mutableStateOf("") }
    var originalTitle by rememberSaveable { mutableStateOf("") }
    var originalAmount by rememberSaveable { mutableStateOf(0.0) }
    var discard by remember { mutableStateOf(false) }
    var issueConfirmation by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var reviewed by remember { mutableStateOf(false) }
    var issuingSnapshot by remember { mutableStateOf<ConsultancyRecord?>(null) }
    val draft = rememberLocalEditorDraft("document:${company?.practiceName}:${viewModel.draftProjectId}:$agreement")
    fun persistEditor() {
        if (editorJson != null) draft.save(org.json.JSONObject().put("editor",editorJson).put("initial",initialJson).put("editingId",editingId ?: org.json.JSONObject.NULL).put("originalJson",originalJson ?: "").put("body",originalBody).put("title",originalTitle).put("amount",originalAmount).toString())
    }
    val selected = records.firstOrNull { it.id == selectedId }
    val context = LocalContext.current
    fun edit(record: ConsultancyRecord?, input: ConsultancyInput) {
        editingId = record?.id; originalJson = record?.detailsJson; originalBody = record?.documentText.orEmpty()
        originalTitle = record?.title.orEmpty(); originalAmount = record?.amount ?: 0.0
        val json = ConsultancyCodec.encode(input); initialJson = json; editorJson = json; persistEditor()
    }
    fun closeEditor() { if (!busy) { if (editorJson != initialJson) discard = true else editorJson = null } }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Text(draft.status, style = MaterialTheme.typography.bodySmall)
            draft.recovered?.let { recovery -> TextButton(enabled = !busy && editorJson == null, onClick = {
                val json = org.json.JSONObject(recovery)
                editorJson = json.getString("editor"); initialJson = json.getString("initial")
                editingId = if (json.isNull("editingId")) null else json.getLong("editingId")
                originalJson = json.optString("originalJson"); originalBody = json.optString("body"); originalTitle = json.optString("title"); originalAmount = json.optDouble("amount",0.0)
                draft.recovered = null
            }) { Text("Resume unsaved document draft") } }
        }
        item {
            Text(if (agreement) "Client–architect agreements" else "Fee proposals", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Button(enabled = !busy, onClick = { viewModel.newDraft(agreement) { edit(null, it) } }) {
                Icon(CarbonIcons.Add, null); Spacer(Modifier.width(8.dp)); Text(if (agreement) "New agreement" else "New fee proposal")
            }
        }
        item { Text("Architecture · Interiors · Landscape · Miscellaneous", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        notice?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        if (records.isEmpty()) item { Text("Create a draft, review the complete document, then issue it for acceptance.") }
        items(records, key = { it.id }) { record ->
            ArchiCard(onClick = { selectedId = record.id }) {
                ListItem(headlineContent = { Text(record.title) },
                    overlineContent = { Text("${if (agreement) "Agreement" else "Proposal"} #${record.id} · ${record.status}") },
                    supportingContent = { Text("Base fee: ${record.amount} · ${if (record.detailsJson.isBlank()) "Legacy record" else record.input?.service?.label ?: "Stored document"}") },
                    leadingContent = { Icon(if (agreement) CarbonIcons.Handshake else CarbonIcons.RequestQuote, null, tint = MaterialTheme.colorScheme.primary) },
                    trailingContent = { Icon(CarbonIcons.ChevronRight, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent))
            }
        }
        if (agreement && contractors.isNotEmpty()) {
            item { Text("Project contractors", style = MaterialTheme.typography.titleMedium) }
            items(contractors, key = { "contractor-${it.id}" }) { contractor ->
                ArchiCard { ListItem(headlineContent = { Text(contractor.name) }, supportingContent = { Text(contractor.contractorType) }, trailingContent = { RecordActions(RecordKind.CONTRACTOR, contractor.id, contractor.name) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent)) }
            }
        }
    }
    editorJson?.let { json ->
        val input = ConsultancyCodec.decode(json)
        ConsultancyEditor(input, agreement, busy, notice, onChange = { editorJson = ConsultancyCodec.encode(it); persistEditor() }, onClose = ::closeEditor, onSave = {
            val id = editingId
            val expected = if (id == null) null else records.firstOrNull { it.id == id }?.let { live ->
                live.copy(title = originalTitle, amount = originalAmount, detailsJson = originalJson.orEmpty(), documentText = originalBody, status = "DRAFT")
            }
            if (id == null || expected != null) viewModel.save(agreement, input, expected) { saved -> draft.clear(); editorJson = null; selectedId = saved }
        })
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard unsaved changes?") },
        text = { Text("The saved document will remain unchanged.") },
        confirmButton = { TextButton(onClick = { draft.clear(); discard = false; editorJson = null }) { Text("Discard") } },
        dismissButton = { Column { TextButton(onClick = { discard = false }) { Text("Keep editing") }; TextButton(enabled = draft.status == "Local draft saved on this device", onClick = { discard = false; editorJson = null }) { Text("Keep local draft and close") } } })
    if (selected != null && editorJson == null) {
        ConsultancyDocumentView(selected, busy, notice, onClose = { selectedId = null },
            onEdit = { selected.input?.let { edit(selected, it) } },
            onShare = { viewModel.share(context, selected) },
            onIssue = { issuingSnapshot = selected; reviewed = false; issueConfirmation = true },
            onCopy = { asAgreement -> viewModel.copy(selected, asAgreement) { selectedId = null } },
            onDelete = { deleteConfirmation = true })
    }
    if (issueConfirmation && selected != null) AlertDialog(onDismissRequest = { if (!busy) issueConfirmation = false }, title = { Text("Issue for client acceptance?") },
        text = { Column { Text("Issued text is locked. This does not record a signature or client acceptance."); Row { Checkbox(reviewed, { reviewed = it }); Text("I reviewed the parties, assigned scope, fee basis, exclusions, terms and cumulative payment schedule.") }; notice?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
        confirmButton = { TextButton(enabled = reviewed && !busy, onClick = { issuingSnapshot?.let { snapshot -> viewModel.issue(snapshot, reviewed) { issueConfirmation = false } } }) { Text("Issue") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { issueConfirmation = false }) { Text("Cancel") } })
    if (deleteConfirmation && selected != null) AlertDialog(onDismissRequest = { if (!busy) deleteConfirmation = false }, title = { Text("Delete draft?") }, text = { Text(selected.title) },
        confirmButton = { TextButton(enabled = !busy, onClick = { viewModel.delete(selected) { deleteConfirmation = false; selectedId = null } }) { Text("Delete draft") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { deleteConfirmation = false }) { Text("Cancel") } })
}

@Composable
private fun ConsultancyEditor(input: ConsultancyInput, agreement: Boolean, busy: Boolean, notice: String?, onChange: (ConsultancyInput) -> Unit, onClose: () -> Unit, onSave: () -> Unit) {
    var replacing by remember { mutableStateOf<ConsultancyService?>(null) }
    var replacingBasis by remember { mutableStateOf<ConsultancyFeeBasis?>(null) }
    var showSchedule by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    val total = runCatching { ConsultancyTemplates.amount(input).toPlainString() }.getOrNull()
    val uriHandler = LocalUriHandler.current
    FullDocumentDialog("${if (agreement) "Agreement" else "Proposal"} editor", onClose, bottom = {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = { preview = true }, modifier = Modifier.weight(1f)) { Text("Preview") }
            Button(enabled = !busy && total != null, onClick = onSave, modifier = Modifier.weight(1f)) { Text(if (busy) "Saving…" else "Save draft") }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Service template", style = MaterialTheme.typography.titleMedium); FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ConsultancyService.entries.forEach { option -> ChoiceChip(option.label, input.service == option) { if (input.service != option) replacing = option } } } }
            item { ArchiField(input.title, { onChange(input.copy(title = it)) }, "Document title", Modifier.fillMaxWidth()) }
            item { ArchiField(input.parties, { onChange(input.copy(parties = it)) }, "Parties, project and date", Modifier.fillMaxWidth(), singleLine = false) }
            item { Text("Fee basis — enter the agreed fee, not an automatic tariff", style = MaterialTheme.typography.titleMedium); ConsultancyFeeBasis.entries.forEach { basis -> ChoiceChip(basis.label, input.feeBasis == basis) {
                if (input.feeBasis != basis) {
                    if (input.rate.isNotBlank() || input.quantity.isNotBlank()) replacingBasis = basis
                    else onChange(input.copy(feeBasis = basis, rate = "", quantity = "", areaDefinition = ""))
                }
            } } }
            item { ArchiField(input.rate, { onChange(input.copy(rate = it)) }, when (input.feeBasis) { ConsultancyFeeBasis.LUMP_SUM -> "Lump-sum fee (INR)"; ConsultancyFeeBasis.PER_SQ_FT -> "Rate (INR per sq ft)"; else -> "Fee rate (%)" }, Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }
            if (input.feeBasis != ConsultancyFeeBasis.LUMP_SUM) item { ArchiField(input.quantity, { onChange(input.copy(quantity = it)) }, if (input.feeBasis == ConsultancyFeeBasis.PER_SQ_FT) "Chargeable area (sq ft)" else "Assigned works cost (INR)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) }
            if (input.feeBasis == ConsultancyFeeBasis.PER_SQ_FT) item { ArchiField(input.areaDefinition, { onChange(input.copy(areaDefinition = it)) }, "Area definition and measurement rules", Modifier.fillMaxWidth(), singleLine = false) }
            item { Text("Base professional fee: INR ${total ?: "—"}", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) }
            item { Text("GST treatment", style = MaterialTheme.typography.titleMedium) }
            items(GstMode.entries.toList()) { mode ->
                TextButton(onClick = { onChange(input.copy(gstMode = mode)) }) { Text("${if(input.gstMode == mode) "Selected: " else ""}${mode.name}") }
            }
            if (input.gstMode != GstMode.NONE) item { ArchiField(input.gstin, { onChange(input.copy(gstin = it.uppercase())) }, "Supplier GSTIN") }
            if (input.gstMode == GstMode.REGULAR) {
                item { ArchiField(input.gstRate, { onChange(input.copy(gstRate = it)) }, "GST rate %") }
                item { ArchiField(input.supplierState, { onChange(input.copy(supplierState = it)) }, "Supplier state code") }
                item { ArchiField(input.placeState, { onChange(input.copy(placeState = it)) }, "Place-of-supply state code (property location)") }
            }
            item { ArchiField(input.scope, { onChange(input.copy(scope = it)) }, "Assigned services and deliverables", Modifier.fillMaxWidth(), singleLine = false) }
            item { ArchiField(input.exclusions, { onChange(input.copy(exclusions = it)) }, "Excluded or separately appointed services", Modifier.fillMaxWidth(), singleLine = false) }
            item { ArchiField(input.terms, { onChange(input.copy(terms = it)) }, if (input.service == ConsultancyService.MISCELLANEOUS) "Enter your own contract terms" else "Contract terms — resolve all bracketed entries", Modifier.fillMaxWidth(), singleLine = false) }
            item { TextButton(onClick = { showSchedule = !showSchedule }) { Icon(CarbonIcons.AccountTree, null); Spacer(Modifier.width(8.dp)); Text("Payment schedule · ${input.milestones.size} cumulative milestones") } }
            if (showSchedule) {
                items(input.milestones.indices.toList(), key = { it }) { index ->
                    val stage = input.milestones[index]
                    ArchiCard { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArchiField(stage.label, { label -> onChange(input.copy(milestones = input.milestones.toMutableList().also { it[index] = stage.copy(label = label) })) }, "Milestone ${index + 1}", Modifier.fillMaxWidth())
                        ArchiField(stage.cumulativePercent, { percent -> onChange(input.copy(milestones = input.milestones.toMutableList().also { it[index] = stage.copy(cumulativePercent = percent) })) }, "Cumulative fee (%)", Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        TextButton(onClick = { onChange(input.copy(milestones = input.milestones.filterIndexed { i, _ -> i != index })) }) { Text("Remove milestone") }
                    } }
                }
                item { OutlinedButton(onClick = { onChange(input.copy(milestones = input.milestones + ConsultancyMilestone("", "100"))) }) { Text("Add milestone") } }
            }
            if (input.sourceUrl.isNotBlank()) item {
                Text("Adapted from CoA guidance checked 5 November 2026. CoA also addresses retainers, documentation charges and reimbursements; confirm them in the terms.")
                TextButton(onClick = { uriHandler.openUri(input.sourceUrl) }) { Text("Read CoA engagement conditions") }
                TextButton(onClick = { uriHandler.openUri(ConsultancyTemplates.SCALE_URL) }) { Text("Read CoA scale of charges") }
            }
            item { Text(ConsultancyTemplates.REVIEW_NOTE, style = MaterialTheme.typography.bodySmall); notice?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }
    replacing?.let { service -> AlertDialog(onDismissRequest = { replacing = null }, title = { Text("Apply ${service.label} template?") },
        text = { Text("This replaces the scope, exclusions, terms and schedule. Party details and fee inputs are retained. Miscellaneous work starts with blank terms.") },
        confirmButton = { TextButton(onClick = {
            val template = ConsultancyTemplates.create(service, input.parties)
            onChange(template.copy(title = "${service.label} ${if (agreement) "agreement" else "fee proposal"}", feeBasis = input.feeBasis, rate = input.rate, quantity = input.quantity, areaDefinition = input.areaDefinition, proposalReference = input.proposalReference, gstMode = input.gstMode, gstRate = input.gstRate, gstin = input.gstin, supplierState = input.supplierState, placeState = input.placeState))
            replacing = null
        }) { Text("Apply template") } }, dismissButton = { TextButton(onClick = { replacing = null }) { Text("Cancel") } }) }
    replacingBasis?.let { basis -> AlertDialog(onDismissRequest = { replacingBasis = null }, title = { Text("Change fee basis?") },
        text = { Text("The rate, quantity and area definition will be cleared so values from one fee basis are not reused with different units.") },
        confirmButton = { TextButton(onClick = { onChange(input.copy(feeBasis = basis, rate = "", quantity = "", areaDefinition = "")); replacingBasis = null }) { Text("Change basis") } },
        dismissButton = { TextButton(onClick = { replacingBasis = null }) { Text("Cancel") } }) }
    if (preview) {
        val text = runCatching { ConsultancyTemplates.render(input, agreement) }.getOrElse { "Complete the fee and payment schedule to preview: ${it.message}" }
        FullDocumentDialog("Draft preview", { preview = false }) { padding -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(20.dp)) { item { SelectionContainer { Text(text) } } } }
    }
}

@Composable
private fun ConsultancyDocumentView(record: ConsultancyRecord, busy: Boolean, notice: String?, onClose: () -> Unit, onEdit: () -> Unit, onShare: () -> Unit, onIssue: () -> Unit, onCopy: (Boolean) -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    FullDocumentDialog(record.title, onClose, actions = {
        IconButton(onClick = onShare) { Icon(CarbonIcons.Share, "Share PDF") }
        Box { IconButton(onClick = { menu = true }) { Icon(CarbonIcons.MoreVert, "Document actions") }
            DropdownMenu(menu, { menu = false }) {
                if (record.input != null) {
                    DropdownMenuItem(text = { Text("Create draft revision") }, enabled = !busy, onClick = { menu = false; onCopy(record.agreement) })
                    if (!record.agreement) DropdownMenuItem(text = { Text("Create agreement from proposal") }, enabled = !busy, onClick = { menu = false; onCopy(true) })
                }
                if (record.status == "DRAFT") DropdownMenuItem(text = { Text("Delete draft") }, enabled = !busy, onClick = { menu = false; onDelete() })
            }
        }
    }, bottom = {
        if (record.status == "DRAFT" && record.input != null) Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !busy, onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit draft") }
            Button(enabled = !busy, onClick = onIssue, modifier = Modifier.weight(1f)) { Text("Review and issue") }
        }
    }) { padding -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(record.status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
        notice?.let { item { Text(it) } }
        item { SelectionContainer { Text(record.documentText.ifBlank { "Legacy record\n${record.title}\nAmount: ${record.amount}\nNo structured terms were stored with this record. Create a new proposal or agreement for the full template workflow." }) } }
        if (record.detailsJson.isBlank()) item { RecordActions(if (record.agreement) RecordKind.CONTRACT else RecordKind.PROPOSAL, record.id, record.title) }
    } }
}

@Composable
private fun FullDocumentDialog(title: String, onClose: () -> Unit, actions: @Composable () -> Unit = {}, bottom: @Composable () -> Unit = {}, content: @Composable (PaddingValues) -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(topBar = { TopAppBar(title = { Text(title, maxLines = 2, style = MaterialTheme.typography.titleMedium) }, navigationIcon = { IconButton(onClick = onClose) { Icon(CarbonIcons.ArrowBack, "Close document") } }, actions = { actions() }) }, bottomBar = { Surface { Box(Modifier.navigationBarsPadding().imePadding()) { bottom() } } }, content = content)
    }
}
