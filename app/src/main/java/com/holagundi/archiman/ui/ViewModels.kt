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
            _demoCatalog.value = runCatching { demoCompanyStore.readPackage() }.getOrNull()
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

    fun createProject(name: String, code: String, client: String, type: String, site: String) {
        viewModelScope.launch {
            projectRepository.createProject(
                name = name,
                code = code,
                clientName = client,
                projectType = type,
                siteLocation = site,
                architect = company.value?.principalName ?: "",
            )
        }
    }

    fun addClient(name: String, phone: String, email: String) {
        viewModelScope.launch { practiceRepository.addClient(name, phone, email) }
    }

    fun addEnquiry(client: String, source: String, notes: String) {
        viewModelScope.launch { suiteRepository.addEnquiry(client, source, notes) }
    }

    fun addLetter(subject: String, body: String) {
        viewModelScope.launch { suiteRepository.addLetter(null, subject, body) }
    }

    fun addRate(name: String, unit: String, rate: Double) {
        viewModelScope.launch { suiteRepository.addRate(name, unit, rate) }
    }
}

@HiltViewModel
class ProjectWorkspaceViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val catalogRepository: CatalogRepository,
    measurementRepository: MeasurementRepository,
    fieldRepository: FieldRepository,
) : ViewModel() {
    private val projectId: Long = checkNotNull(savedStateHandle.get<Long>("projectId"))

    val project = projectRepository.observeProject(projectId).stateIn(viewModelScope, subscribed, null)
    val floors = projectRepository.observeFloors(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val rooms = projectRepository.observeRooms(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val contractors = catalogRepository.observeContractors(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val sheets = measurementRepository.observeSheets(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val measurements = measurementRepository.observeMeasurements(projectId).stateIn(viewModelScope, subscribed, emptyList())
    val counts = fieldRepository.observeCounts(projectId).stateIn(viewModelScope, subscribed, ProjectModuleCounts())
    val workItems = catalogRepository.observeActiveItems().stateIn(viewModelScope, subscribed, emptyList())

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
) : ViewModel() {
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun save(draft: MeasurementDraft, onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = measurementRepository.record(draft)) {
                is RecordResult.Saved -> onDone()
                is RecordResult.Rejected -> _error.value = result.reason
            }
        }
    }
}

@HiltViewModel
class MeasurementBookViewModel @Inject constructor(
    private val measurementRepository: MeasurementRepository,
) : ViewModel() {
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    fun transition(sheetId: Long, status: String, comment: String) {
        viewModelScope.launch {
            val ok = measurementRepository.transition(sheetId, status, comment, "Architect")
            if (!ok) _notice.value = "That review step needs a valid comment."
        }
    }

    fun deleteRow(id: Long) {
        viewModelScope.launch { measurementRepository.deleteMeasurement(id) }
    }

    fun export(projectId: Long, sheetId: Long, onReady: (fileName: String, csv: String) -> Unit) {
        viewModelScope.launch {
            val sheet = measurementRepository.export(projectId, sheetId) ?: return@launch
            onReady("MBook_${sheet.sheetCode}.csv", MeasurementSheetCsv.render(sheet))
        }
    }

    fun clearNotice() {
        _notice.value = null
    }
}

@HiltViewModel
class SuiteProjectViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val suiteRepository: SuiteRepository,
    practiceRepository: PracticeRepository,
) : ViewModel() {
    private val projectId: Long = checkNotNull(savedStateHandle.get<Long>("projectId"))

    val settings = suiteRepository.observeSettings(projectId).stateIn(viewModelScope, subscribed, null)
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
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()

    init {
        viewModelScope.launch { suiteRepository.ensureSettings(projectId) }
    }

    fun nextStep(): String = suiteRepository.nextStep(stages.value, invoices.value, receipts.value)

    fun setPersona(persona: String) = launch { suiteRepository.setPersona(projectId, persona) }
    fun assignStages() = launch { suiteRepository.assignStandardStages(projectId) }
    fun bill(stage: ProjectStageEntity, amount: Double) = launch { suiteRepository.billStage(projectId, stage, amount) }
    fun issue(invoice: InvoiceEntity) = launch { suiteRepository.issueInvoice(invoice.id) }
    fun pay(invoice: InvoiceEntity, amount: Double, note: String) = launch { suiteRepository.recordReceipt(projectId, invoice, amount, note) }
    fun addMeeting(title: String, attendees: String, discussion: String) = launch { suiteRepository.addMeeting(projectId, title, attendees, discussion) }
    fun addLog(
        weather: String,
        work: String,
        by: String,
        manpower: String = "",
        materials: String = "",
        delays: String = "",
        safety: String = "",
        nextDay: String = "",
    ) = launch { suiteRepository.addLog(projectId, weather, work, by, manpower, materials, delays, safety, nextDay) }
    fun addIssue(
        type: String,
        title: String,
        location: String,
        description: String,
        severity: String,
        assignedTo: String = "",
        correctiveAction: String = "",
        evidenceUri: String = "",
    ) = launch {
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
            _notice.value = error ?: "Site issue moved to ${SiteIssueWorkflow.statusLabel(toStatus)}."
        }
    }
    fun addPermit(title: String, authority: String) = launch { suiteRepository.addPermit(projectId, title, authority) }
    fun addTime(person: String, minutes: Int, notes: String) = launch { suiteRepository.addTime(projectId, person, minutes, notes) }
    fun addInspection(location: String, inspector: String, observation: String) = launch { suiteRepository.addInspection(projectId, location, inspector, observation) }
    fun addTransmittal(subject: String, recipients: String, purpose: String = "For information") =
        launch { suiteRepository.addTransmittal(projectId, subject, recipients, purpose) }

    fun addCoordination(
        type: String,
        subject: String,
        discipline: String,
        location: String,
        question: String,
        raisedBy: String,
        assignedTo: String,
    ) = launch {
        suiteRepository.addCoordination(projectId, type, subject, discipline, location, question, raisedBy, assignedTo)
    }

    fun transitionCoordination(itemId: Long, toStatus: String, note: String = "") {
        viewModelScope.launch {
            val error = withContext(Dispatchers.IO) {
                suiteRepository.transitionCoordination(itemId, toStatus, note)
            }
            _notice.value = error ?: "Coordination moved to $toStatus."
        }
    }

    fun addDrawing(number: String, title: String, discipline: String, packageTag: String) =
        launch { suiteRepository.addDrawing(projectId, number, title, discipline, packageTag) }

    fun addRevision(
        drawingId: Long,
        revisionCode: String,
        notes: String,
        asBuilt: Boolean,
        severity: String,
        fileNote: String,
    ) = launch {
        suiteRepository.addRevision(projectId, drawingId, revisionCode, notes, asBuilt, severity, fileNote)
    }
    fun receiveOrder(id: Long) = launch { suiteRepository.receiveOrder(id) }
    fun decideApproval(id: Long, status: String) = launch { suiteRepository.decideApproval(id, status) }
    fun advanceActivity(id: Long, progress: Int) = launch { suiteRepository.advanceActivity(id, progress) }
    fun addLetter(subject: String, body: String) = launch { suiteRepository.addLetter(projectId, subject, body) }
    fun addProposal(title: String, amount: Double) = launch { suiteRepository.addProposal(projectId, title, amount) }
    fun addTask(title: String) = launch { suiteRepository.addTask(projectId, title) }
    fun moveTask(id: Long, status: String) = launch { suiteRepository.moveTask(id, status) }
    fun saveBrief(objectives: String, requirements: String, constraints: String) = launch { suiteRepository.saveBrief(projectId, objectives, requirements, constraints) }
    fun addFeeInvoice(title: String, amount: Double) = launch { suiteRepository.addFeeInvoice(projectId, title, amount) }
    fun addBillRate(name: String, unit: String, rate: Double) = launch { suiteRepository.addRate(name, unit, rate) }
    fun addApproval(title: String, type: String) = launch { suiteRepository.addApproval(projectId, title, type) }
    fun addContract(title: String, party: String, kind: String, amount: Double) = launch { suiteRepository.addContract(projectId, title, party, kind, amount) }
    fun addActivity(title: String) = launch { suiteRepository.addActivity(projectId, title) }
    fun addOrder(
        title: String,
        supplier: String,
        itemName: String = "",
        specification: String = "",
        quantity: Double = 0.0,
        unit: String = "",
    ) = launch { suiteRepository.addOrder(projectId, title, supplier, itemName, specification, quantity, unit) }

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

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
