package com.holagundi.archiman.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.holagundi.archiman.data.CatalogRepository
import com.holagundi.archiman.data.DemoCompanyPackage
import com.holagundi.archiman.data.DemoCompanyStore
import com.holagundi.archiman.data.FieldRepository
import com.holagundi.archiman.data.MeasurementRepository
import com.holagundi.archiman.data.PracticeRepository
import com.holagundi.archiman.data.ProjectRepository
import com.holagundi.archiman.data.SuiteRepository
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.LocalUserEntity
import com.holagundi.archiman.data.local.entity.ProjectStageEntity
import com.holagundi.archiman.domain.MeasurementDraft
import com.holagundi.archiman.domain.MeasurementSheetCsv
import com.holagundi.archiman.domain.ProjectModuleCounts
import com.holagundi.archiman.domain.RecordResult
import com.holagundi.archiman.domain.SiteIssueWorkflow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

private val subscribed = SharingStarted.WhileSubscribed(5_000)

@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val practiceRepository: PracticeRepository,
    private val projectRepository: ProjectRepository,
    private val suiteRepository: SuiteRepository,
    private val demoCompanyStore: DemoCompanyStore,
    catalogRepository: CatalogRepository,
) : ViewModel() {
    private val attentionLimit = MutableStateFlow(50)
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val inboxTasks = attentionLimit.flatMapLatest { suiteRepository.observeDashboardTasks(it) }.stateIn(viewModelScope, subscribed, emptyList())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val inboxReviews = attentionLimit.flatMapLatest { suiteRepository.observeDashboardReviews(it) }.stateIn(viewModelScope, subscribed, emptyList())
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val inboxIssues = attentionLimit.flatMapLatest { suiteRepository.observeDashboardIssues(it) }.stateIn(viewModelScope, subscribed, emptyList())
    fun loadMoreAttention() { attentionLimit.value += 50 }
    val dashboardMetrics = suiteRepository.observeDashboardMetrics().stateIn(viewModelScope, subscribed, null)
    val dashboardTasks = suiteRepository.observeDashboardTasks().stateIn(viewModelScope, subscribed, emptyList())
    val dashboardReviews = suiteRepository.observeDashboardReviews().stateIn(viewModelScope, subscribed, emptyList())
    val dashboardIssues = suiteRepository.observeDashboardIssues().stateIn(viewModelScope, subscribed, emptyList())
    val dashboardFees = combine(suiteRepository.observeAllInvoices(), suiteRepository.observeAllReceipts()) { invoices, receipts ->
        com.holagundi.archiman.data.FeeDashboardSummary.calculate(invoices, receipts)
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, subscribed, null)
    val company = practiceRepository.observeCompany().stateIn(viewModelScope, subscribed, null)
    val projects = projectRepository.observeProjects().stateIn(viewModelScope, subscribed, emptyList())
    val clients = practiceRepository.observeClients().stateIn(viewModelScope, subscribed, emptyList())
    val workItems = catalogRepository.observeActiveItems().stateIn(viewModelScope, subscribed, emptyList())


    val users = suiteRepository.observeUsers().stateIn(viewModelScope, subscribed, emptyList())
    val enquiries = suiteRepository.observeEnquiries().stateIn(viewModelScope, subscribed, emptyList())
    val letters = suiteRepository.observeLetters().stateIn(viewModelScope, subscribed, emptyList())
    val transmittals = suiteRepository.observeAllTransmittals().stateIn(viewModelScope, subscribed, emptyList())
    val stages = suiteRepository.observeAllStages().stateIn(viewModelScope, subscribed, emptyList())
    val invoices = suiteRepository.observeAllInvoices().stateIn(viewModelScope, subscribed, emptyList())
    val receipts = suiteRepository.observeAllReceipts().stateIn(viewModelScope, subscribed, emptyList())
    val rates = suiteRepository.observeRates().stateIn(viewModelScope, subscribed, emptyList())
    private val _signedIn = MutableStateFlow<LocalUserEntity?>(null)
    val signedIn = _signedIn.asStateFlow()
    private val _demoCatalog = MutableStateFlow<DemoCompanyPackage?>(null)
    val demoCatalog = _demoCatalog.asStateFlow()
    private val _demoNotice = MutableStateFlow<String?>(null)
    val demoNotice = _demoNotice.asStateFlow()
    private val _openingDemo = MutableStateFlow(false)
    val openingDemo = _openingDemo.asStateFlow()

    init {
        viewModelScope.launch {
            _demoCatalog.value = withContext(Dispatchers.IO) { runCatching { demoCompanyStore.readPackage() }.getOrNull() }
        }
    }

    fun openBuiltInDemo() {
        if (_openingDemo.value) return
        viewModelScope.launch {
            _openingDemo.value = true
            _demoNotice.value = null
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    demoCompanyStore.openBuiltIn()
                    suiteRepository.ensureDefaults()
                }
            }
            _demoNotice.value = result.exceptionOrNull()?.message ?: "Demo company opened."
            _openingDemo.value = false
        }
    }

    fun signIn(user: LocalUserEntity) {
        _signedIn.value = user
    }

    fun signOut() {
        _signedIn.value = null
    }

    val operations = UiOperations(viewModelScope)
    fun createProject(name: String, code: String, client: String, type: String, site: String) = operations.run("createProject", "Project $name created with a Ground Floor.") {
        projectRepository.createProject(name = name, code = code, clientName = client, projectType = type, siteLocation = site, architect = company.value?.principalName ?: "")
    }
    fun addClient(name: String, phone: String, email: String) = operations.run("addClient", "Client $name saved.") { practiceRepository.addClient(name, phone, email) }
    fun addEnquiry(client: String, source: String, notes: String) = operations.run("addEnquiry", "Enquiry saved for $client.") { suiteRepository.addEnquiry(client, source, notes) }
    fun addLetter(subject: String, body: String) = operations.run("addLetter", "Letter $subject saved.") { suiteRepository.addLetter(null, subject, body) }
    fun addRate(name: String, unit: String, rate: Double) = operations.run("addRate", "Rate saved for $name.") { suiteRepository.addRate(name, unit, rate) }

}

@HiltViewModel
class ProjectWorkspaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val catalogRepository: CatalogRepository,
    measurementRepository: MeasurementRepository,
    fieldRepository: FieldRepository,
    scheduleRepository: com.holagundi.archiman.data.ProjectScheduleRepository,
) : ViewModel() {
    private val projectId: Long = checkNotNull(savedStateHandle.get<Long>("projectId"))

    val schedules = scheduleRepository.observe(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val project = projectRepository.observeProject(projectId).stateIn(viewModelScope, subscribed, null)
    val floors = projectRepository.observeFloors(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val rooms = projectRepository.observeRooms(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val contractors = catalogRepository.observeContractors(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val sheets = measurementRepository.observeSheets(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val measurements = measurementRepository.observeMeasurements(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val counts = fieldRepository.observeCounts(projectId).stateIn(viewModelScope, subscribed, ProjectModuleCounts())
    val workItems = catalogRepository.observeActiveItems().stateIn(viewModelScope, subscribed, emptyList())
    data class CaptureContext(
        val schedules: List<com.holagundi.archiman.data.ScheduleRevision>,
        val contractors: List<com.holagundi.archiman.data.local.entity.ContractorEntity>,
        val floors: List<com.holagundi.archiman.data.local.entity.FloorEntity>,
    )
    // A null initial value distinguishes loading from a genuinely empty setup. This prevents
    // lookup placeholders from erasing restored contractor/item selections after recreation.
    val captureContext = combine(scheduleRepository.observe(projectId), catalogRepository.observeContractors(projectId), projectRepository.observeFloors(projectId)) { schedules, contractors, floors ->
        CaptureContext(schedules, contractors, floors)
    }.stateIn(viewModelScope, subscribed, null)

    fun qualified(contractorId: Long) = catalogRepository.observeQualified(contractorId)

    fun addFloor(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { projectRepository.addFloor(projectId, name) }
    }

    fun addRoom(floorId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { projectRepository.addRoom(projectId, floorId, name) }
    }
}

@HiltViewModel
class RecordMeasurementViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
    private val recordOperations: com.holagundi.archiman.data.RecordOperations,
    private val captureState: SavedStateHandle,
) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _saved = MutableStateFlow<String?>(null)
    val saved = _saved.asStateFlow()
    private val _selected = MutableStateFlow(captureState.get<HashMap<String, Any?>>("entryValues")?.takeIf { it.isNotEmpty() }?.let {
        com.holagundi.archiman.data.RecordSnapshot(com.holagundi.archiman.data.RecordKind.MEASUREMENT, captureState.get<Long>("entryId") ?: 0L, it)
    })
    val selected = _selected.asStateFlow()
    private val _editing = MutableStateFlow(captureState.get<Boolean>("entryEditing") ?: true)
    val editing = _editing.asStateFlow()

    fun beginEntry() {
        if (!_busy.value) { _error.value = null; _saved.value = null; selectSnapshot(null); setEditing(true) }
    }
    private fun selectSnapshot(value: com.holagundi.archiman.data.RecordSnapshot?) {
        _selected.value = value
        captureState["entryId"] = value?.id ?: 0L
        captureState["entryValues"] = HashMap(value?.values.orEmpty())
    }
    private fun setEditing(value: Boolean) { _editing.value = value; captureState["entryEditing"] = value }

    private fun operation(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true; _error.value = null; _saved.value = null
        viewModelScope.launch {
            try { block() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (failure: Exception) { _error.value = failure.message ?: "Could not complete this entry operation. Your edits are retained." }
            finally { _busy.value = false }
        }
    }

    fun selectEntry(id: Long, ready: (com.holagundi.archiman.data.RecordSnapshot) -> Unit) = operation {
        val loaded = withContext(Dispatchers.IO) { recordOperations.read(com.holagundi.archiman.data.RecordKind.MEASUREMENT, id) }
        selectSnapshot(loaded); setEditing(false); ready(loaded)
    }

    fun editSelected(ready: () -> Unit) = operation {
        val selected = requireNotNull(_selected.value) { "Choose a recorded entry first." }
        withContext(Dispatchers.IO) { recordOperations.ensureAllowed(selected, false) }
        setEditing(true); ready()
    }

    fun saveSelected(fields: Map<String, String>, ready: (com.holagundi.archiman.data.RecordSnapshot) -> Unit) = operation {
        require(_editing.value) { "Use Edit before saving a recorded entry." }
        val selected = requireNotNull(_selected.value)
        val committed = withContext(Dispatchers.IO) { recordOperations.saveWithSnapshot(selected, fields) }
        selectSnapshot(committed); setEditing(false)
        _saved.value = "Entry ${committed.id} updated in its existing sheet. No new entry was created."
        ready(committed)
    }

    fun clearError() {
        _error.value = null
    }

    fun save(draft: MeasurementDraft, onDone: (Long) -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        _saved.value = null
        viewModelScope.launch {
            try {
                when (val result = measurementRepository.record(draft)) {
                    is RecordResult.Saved -> {
                        _saved.value = "Entry saved to draft sheet ${result.sheetId}: ${draft.itemName}. Unit: ${draft.unit}."
                        onDone(result.sheetId)
                    }
                    is RecordResult.Rejected -> _error.value = result.reason
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: Exception) { _error.value = error.message ?: "Could not save this measurement. Your entries are retained; please retry."
            } finally { _busy.value = false }
        }
    }
}

@HiltViewModel
class MeasurementBookViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {
    val operations = UiOperations(viewModelScope)
    val notice = operations.notice
    val pending = operations.pending
    fun transition(sheetId: Long, status: String, comment: String) = operations.run("sheet:$sheetId", "Sheet $sheetId review status updated.") {
        require(measurementRepository.transition(sheetId, status, comment, "Architect")) { "Review could not be completed. Check the current sheet status and required comment." }
    }
    fun deleteRow(id: Long) = operations.run("entry:$id", "Measurement entry deleted.") { measurementRepository.deleteMeasurement(id) }

    fun export(projectId: Long, sheetId: Long, onReady: (fileName: String, csv: String) -> Unit) {
        viewModelScope.launch {
            val sheet = measurementRepository.export(projectId, sheetId) ?: return@launch
            onReady("MBook_${sheet.sheetCode}.csv", MeasurementSheetCsv.render(sheet))
        }
    }

    fun clearNotice() {
        operations.dismiss()
    }
}

@HiltViewModel
class SuiteProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val suiteRepository: SuiteRepository,
    practiceRepository: PracticeRepository,
) : ViewModel() {
    private val projectId: Long = checkNotNull(savedStateHandle.get<Long>("projectId"))
    val projectIdForFinance get() = projectId

    val settings = suiteRepository.observeSettings(projectId).stateIn(viewModelScope, subscribed, null)
    val rates = suiteRepository.observeRates().stateIn(viewModelScope, subscribed, emptyList())
    val stages = suiteRepository.observeStages(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val invoices = suiteRepository.observeInvoices(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val receipts = suiteRepository.observeReceipts(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val permits = suiteRepository.observePermits(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val time = suiteRepository.observeTime(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val letters = suiteRepository.observeLetters().stateIn(viewModelScope, subscribed, emptyList())
    val transmittals = suiteRepository.observeTransmittals(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val inspections = suiteRepository.observeInspections(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val proposals = suiteRepository.observeProposals(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val rcc = suiteRepository.observeRcc(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val contracts = suiteRepository.observeContracts(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val activities = suiteRepository.observeActivities(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val links = suiteRepository.observeLinks().stateIn(viewModelScope, subscribed, emptyList())
    val meetings = suiteRepository.observeMeetings(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val logs = suiteRepository.observeLogs(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val approvedMeasurements = suiteRepository.observeApprovedMeasurements(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val orderLines = suiteRepository.observeOrderLines(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val issues = suiteRepository.observeIssues(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val coordination = suiteRepository.observeCoordination(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val drawings = suiteRepository.observeDrawings(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val revisions = suiteRepository.observeRevisions(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val tasks = suiteRepository.observeTasks(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val brief = suiteRepository.observeBrief(projectId).stateIn(viewModelScope, subscribed, null)
    val company = practiceRepository.observeCompany().stateIn(viewModelScope, subscribed, null)
    val approvals = suiteRepository.observeApprovals(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val orders = suiteRepository.observeOrders(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val estimate = suiteRepository.observeEstimate(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val operations = UiOperations(viewModelScope)
    val operationNotice = operations.notice
    val pending = operations.pending
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    init {
        viewModelScope.launch { suiteRepository.ensureSettings(projectId) }
    }

    fun nextStep(): String = suiteRepository.nextStep(stages.value, invoices.value, receipts.value)

    fun setPersona(persona: String) = launch("setPersona", "Project workflow updated.") { suiteRepository.setPersona(projectId, persona) }
    fun assignStages() = launch("assignStages", "Assign stages completed.") { suiteRepository.assignStandardStages(projectId) }
    fun bill(stage: ProjectStageEntity, amount: Double) = launch("bill", "Bill completed.") { suiteRepository.billStage(projectId, stage, amount) }
    fun issue(invoice: InvoiceEntity) = operations.run("invoice:${invoice.id}", "Invoice ${invoice.number} issued.") { suiteRepository.issueInvoice(invoice.id) }
    fun pay(invoice: InvoiceEntity, amount: Double, note: String, tds: Double = 0.0, reference: String = "") = operations.run("invoice:${invoice.id}", "Receipt posted to ${invoice.number}: cash $amount, TDS $tds. Invoice balance updated.") { suiteRepository.recordReceipt(projectId, invoice, amount, note, tds, reference) }

    fun addMeeting(title: String, attendees: String, discussion: String) = launch("addMeeting", "Meeting saved.") { suiteRepository.addMeeting(projectId, title, attendees, discussion) }
    fun addLog(
        weather: String,
        work: String,
        by: String,
        manpower: String = "",
        materials: String = "",
        delays: String = "",
        safety: String = "",
        nextDay: String = "",
    ) = launch("addLog", "Daily site log saved.") { suiteRepository.addLog(projectId, weather, work, by, manpower, materials, delays, safety, nextDay) }
    fun addIssue(
        type: String,
        title: String,
        location: String,
        description: String,
        severity: String,
        assignedTo: String = "",
        correctiveAction: String = "",
        evidenceUri: String = "",
    ) = launch("addIssue", "Site issue saved.") {
        suiteRepository.addIssue(
            projectId,
            type,
            title,
            location,
            description,
            severity,
            assignedTo,
            correctiveAction,
            evidenceUri,
        )
    }

    fun transitionIssue(
        issueId: Long,
        toStatus: String,
        verificationNote: String = "",
        afterEvidenceUri: String = "",
    ) {
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) {
                suiteRepository.transitionIssue(
                    issueId,
                    toStatus,
                    verificationNote,
                    afterEvidenceUri,
                )
            }
            operations.report(error ?: "Site issue moved to ${SiteIssueWorkflow.statusLabel(toStatus)}.", if (error == null) com.holagundi.archiman.ui.theme.CarbonNoticeKind.SUCCESS else com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR)
        }
    }
    fun addPermit(title: String, authority: String) = launch("addPermit", "Permit saved.") { suiteRepository.addPermit(projectId, title, authority) }
    fun addTime(person: String, minutes: Int, notes: String) = launch("addTime", "Add time completed.") { suiteRepository.addTime(projectId, person, minutes, notes) }
    fun addInspection(location: String, inspector: String, observation: String) = launch("addInspection", "Site visit saved.") { suiteRepository.addInspection(projectId, location, inspector, observation) }
    fun addTransmittal(subject: String, recipients: String, purpose: String = "For information") =
        launch("addTransmittal", "Add transmittal completed.") { suiteRepository.addTransmittal(projectId, subject, recipients, purpose) }

    fun addCoordination(
        type: String,
        subject: String,
        discipline: String,
        location: String,
        question: String,
        raisedBy: String,
        assignedTo: String,
    ) = launch("addCoordination", "Coordination record saved.") {
        suiteRepository.addCoordination(projectId, type, subject, discipline, location, question, raisedBy, assignedTo)
    }

    fun transitionCoordination(itemId: Long, toStatus: String, note: String = "") {
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) {
                suiteRepository.transitionCoordination(itemId, toStatus, note)
            }
            operations.report(error ?: "Coordination status updated.", if (error == null) com.holagundi.archiman.ui.theme.CarbonNoticeKind.SUCCESS else com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR)
        }
    }

    fun addDrawing(number: String, title: String, discipline: String, packageTag: String) =
        launch("addDrawing", "Drawing added to the register.") { suiteRepository.addDrawing(projectId, number, title, discipline, packageTag) }

    fun addRevision(
        drawingId: Long,
        revisionCode: String,
        notes: String,
        asBuilt: Boolean,
        severity: String,
        fileNote: String,
    ) = launch("addRevision", "Drawing revision saved.") {
        suiteRepository.addRevision(projectId, drawingId, revisionCode, notes, asBuilt, severity, fileNote)
    }
    fun receiveOrder(id: Long) = launch("receiveOrder", "Receive order completed.") { suiteRepository.receiveOrder(id) }
    fun decideApproval(id: Long, status: String) = launch("decideApproval", "Decide approval completed.") { suiteRepository.decideApproval(id, status) }
    fun advanceActivity(id: Long, progress: Int) = launch("advanceActivity", "Advance activity completed.") { suiteRepository.advanceActivity(id, progress) }
    fun addLetter(subject: String, body: String) = launch("addLetter", "Project letter saved.") { suiteRepository.addLetter(projectId, subject, body) }
    fun addProposal(title: String, amount: Double) = launch("addProposal", "Add proposal completed.") { suiteRepository.addProposal(projectId, title, amount) }
    fun addTask(title: String) = launch("addTask", "Task added to To do.") { suiteRepository.addTask(projectId, title) }
    fun moveTask(id: Long, status: String) = launch("moveTask", "Task status updated.") { suiteRepository.moveTask(id, status) }
    fun saveBrief(objectives: String, requirements: String, constraints: String) = launch("saveBrief", "Project brief saved.") { suiteRepository.saveBrief(projectId, objectives, requirements, constraints) }
    fun addFeeInvoice(title: String, amount: Double) = launch("addFeeInvoice", "Add fee invoice completed.") { suiteRepository.addFeeInvoice(projectId, title, amount) }
    fun addBillRate(name: String, unit: String, rate: Double) = launch("addBillRate", "Add bill rate completed.") { suiteRepository.addRate(name, unit, rate) }
    fun addApproval(title: String, type: String) = launch("addApproval", "Add approval completed.") { suiteRepository.addApproval(projectId, title, type) }
    fun addContract(title: String, party: String, kind: String, amount: Double) = launch("addContract", "Add contract completed.") { suiteRepository.addContract(projectId, title, party, kind, amount) }
    fun addActivity(title: String) = launch("addActivity", "Add activity completed.") { suiteRepository.addActivity(projectId, title) }
    fun addOrder(
        title: String,
        supplier: String,
        itemName: String = "",
        specification: String = "",
        quantity: Double = 0.0,
        unit: String = "",
    ) = launch("addOrder", "Add order completed.") { suiteRepository.addOrder(projectId, title, supplier, itemName, specification, quantity, unit) }

    fun addRcc(projectCode: String, element: String, mark: String, nos: Double, length: Double, breadth: Double, height: Double) {
        viewModelScope.launch {
            _notice.value = suiteRepository.addRcc(projectId, projectCode, element, mark, nos, length, breadth, height)
        }
    }

    fun addCivil(projectCode: String, trade: String, nos: Double, length: Double, breadth: Double, height: Double) {
        viewModelScope.launch {
            _notice.value = suiteRepository.addCivil(projectId, projectCode, trade, nos, length, breadth, height)
        }
    }

    fun applyLink(projectCode: String, ruleId: Long) {
        viewModelScope.launch {
            _notice.value = suiteRepository.applyLink(projectId, projectCode, ruleId)
        }
    }

    fun clearNotice() {
        _notice.value = null
    }

    private fun launch(key: String, success: String, block: suspend () -> Unit): SaveAction = operations.run(key, success, block)
}
