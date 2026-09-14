package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.SiteRepository
import com.example.domain.WorkCatalog
import com.example.domain.CatalogDocumentParser
import com.example.domain.MeasurementSheetStatus
import com.example.domain.MeasurementInput
import com.example.domain.QuantityCalculator
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.DirectorySection
import com.example.ui.navigation.HomeTab
import com.example.ui.navigation.ProjectSection
import com.example.portal.LocalPortalServer
import com.example.domain.CompanyDashboardSnapshot
import com.example.domain.computeCompanyDashboardSnapshot
import com.example.company.CompanyDatabaseImportPreview
import com.example.company.CompanyDatabasePackageManager
import com.example.portal.PortalProject
import com.example.portal.PortalSnapshot
import com.example.portal.PortalMutation
import com.example.portal.PortalMutationResult
import com.example.portal.PortalPrincipal
import com.example.portal.PortalOption
import com.example.portal.PortalUserOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class SiteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = SiteRepository(database)
    private val localPortalServer = LocalPortalServer(application)
    val localPortalState = localPortalServer.state
    private val companyDatabasePackageManager = CompanyDatabasePackageManager(application, database)

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedHomeTab = MutableStateFlow(HomeTab.PROJECTS)
    val selectedHomeTab: StateFlow<HomeTab> = _selectedHomeTab.asStateFlow()

    private val _selectedDirectorySection = MutableStateFlow(DirectorySection.CLIENTS)
    val selectedDirectorySection: StateFlow<DirectorySection> = _selectedDirectorySection.asStateFlow()

    private val _selectedProjectSection = MutableStateFlow(ProjectSection.OVERVIEW)
    val selectedProjectSection: StateFlow<ProjectSection> = _selectedProjectSection.asStateFlow()

    // Top Level Streams
    val projects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clients: StateFlow<List<ClientEntity>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contractors: StateFlow<List<ContractorEntity>> = repository.allContractors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQualifiedItems: StateFlow<List<ContractorQualifiedItemEntity>> = repository.allQualifiedItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val items: StateFlow<List<ItemMasterEntity>> = repository.allItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val measurements: StateFlow<List<MeasurementEntity>> = repository.allMeasurements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val measurementSheets: StateFlow<List<MeasurementSheetEntity>> = repository.getMeasurementSheets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Company-wide monitoring dashboard: two 5-flow/3-flow stages, then combined,
    // since kotlinx.coroutines.flow.combine tops out at 5 typed flows per call.
    private data class DashboardPartA(
        val projects: List<ProjectEntity>,
        val coordination: List<CoordinationItemEntity>,
        val issues: List<SiteIssueEntity>,
        val decisions: List<ProjectDecisionEntity>,
        val dailyReports: List<DailySiteReportEntity>
    )
    private data class DashboardPartB(
        val tasks: List<ProjectTaskEntity>,
        val drawings: List<ProjectDrawingEntity>,
        val schedules: List<ProjectScheduleEntity>,
        val revisions: List<DrawingRevisionEntity>
    )
    private val dashboardPartA = combine(
        repository.allProjects, repository.allCoordinationItems, repository.allSiteIssues,
        repository.allDecisions, repository.allDailyReports
    ) { projects, coordination, issues, decisions, dailyReports ->
        DashboardPartA(projects, coordination, issues, decisions, dailyReports)
    }
    private val dashboardPartB = combine(
        repository.allProjectTasks, repository.allDrawings, repository.allProjectSchedules, repository.allDrawingRevisions
    ) { tasks, drawings, schedules, revisions -> DashboardPartB(tasks, drawings, schedules, revisions) }

    val companyDashboard: StateFlow<CompanyDashboardSnapshot> = combine(dashboardPartA, dashboardPartB) { a, b ->
        computeCompanyDashboardSnapshot(
            projects = a.projects, coordinationItems = a.coordination, siteIssues = a.issues,
            decisions = a.decisions, dailyReports = a.dailyReports,
            tasks = b.tasks, drawings = b.drawings, schedules = b.schedules,
            drawingRevisions = b.revisions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompanyDashboardSnapshot())
    val companyProfile: StateFlow<CompanyProfileEntity?> = repository.companyProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val localUsers: StateFlow<List<LocalUserEntity>> = repository.localUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val portalAuditEvents: StateFlow<List<PortalAuditEventEntity>> = repository.portalAuditEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val _portalUserMessage = MutableStateFlow<String?>(null)
    val portalUserMessage: StateFlow<String?> = _portalUserMessage.asStateFlow()

    // Dedicated Measurement Session State
    private val _dedicatedContractorId = MutableStateFlow<Long?>(null)
    val dedicatedContractorId: StateFlow<Long?> = _dedicatedContractorId.asStateFlow()

    private val _dedicatedItemName = MutableStateFlow<String>("")
    val dedicatedItemName: StateFlow<String> = _dedicatedItemName.asStateFlow()

    private val _dedicatedItemUom = MutableStateFlow<String>("m²")
    val dedicatedItemUom: StateFlow<String> = _dedicatedItemUom.asStateFlow()

    private val _dedicatedCalculationType = MutableStateFlow<CalculationType>(CalculationType.AREA)
    val dedicatedCalculationType: StateFlow<CalculationType> = _dedicatedCalculationType.asStateFlow()

    private val _dedicatedFloorId = MutableStateFlow<Long?>(null)
    val dedicatedFloorId: StateFlow<Long?> = _dedicatedFloorId.asStateFlow()

    private val _dedicatedFloorName = MutableStateFlow<String>("")
    val dedicatedFloorName: StateFlow<String> = _dedicatedFloorName.asStateFlow()

    private val _dedicatedItemId = MutableStateFlow<Long?>(null)
    val dedicatedItemId: StateFlow<Long?> = _dedicatedItemId.asStateFlow()

    // Selection Hierarchy (Digital Measurement Book Context)
    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    val projectTasks: StateFlow<List<ProjectTaskEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectTasks) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectSelectionItems: StateFlow<List<ProjectSelectionItemEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectSelectionItems) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectSchedules: StateFlow<List<ProjectScheduleEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectSchedules) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val meetingMinutes: StateFlow<List<MeetingMinutesEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getMeetingMinutes) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val siteInspections: StateFlow<List<SiteInspectionEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getSiteInspections) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailySiteReports: StateFlow<List<DailySiteReportEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getDailySiteReports) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectDecisions: StateFlow<List<ProjectDecisionEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectDecisions) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val siteIssues: StateFlow<List<SiteIssueEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getSiteIssues) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectConsultants: StateFlow<List<ProjectConsultantEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectConsultants) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val coordinationItems: StateFlow<List<CoordinationItemEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getCoordinationItems) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _coordinationMessage = MutableStateFlow<String?>(null)
    val coordinationMessage: StateFlow<String?> = _coordinationMessage.asStateFlow()

    val projectDrawings: StateFlow<List<ProjectDrawingEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectDrawings) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drawingRevisions: StateFlow<List<DrawingRevisionEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getDrawingRevisions) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drawingTransmittals: StateFlow<List<DrawingTransmittalEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getDrawingTransmittals) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val projectConsultancyProfile: StateFlow<ProjectConsultancyProfileEntity?> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectConsultancyProfile) ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val projectScopeItems: StateFlow<List<ProjectScopeItemEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectScopeItems) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectOnboardingResponses: StateFlow<List<ProjectOnboardingResponseEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectOnboardingResponses) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectApprovals: StateFlow<List<ProjectApprovalEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectApprovals) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectBacklog: StateFlow<List<ProjectBacklogEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectBacklog) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectContractorRefs: StateFlow<List<ProjectContractorCrossRef>> = selectedProjectId
        .flatMapLatest { projectId ->
            if (projectId == null) flowOf(emptyList())
            else repository.getProjectContractorRefs(projectId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFloorId = MutableStateFlow<Long?>(null)
    val selectedFloorId: StateFlow<Long?> = _selectedFloorId.asStateFlow()

    private val _selectedRoomId = MutableStateFlow<Long?>(null)
    val selectedRoomId: StateFlow<Long?> = _selectedRoomId.asStateFlow()

    private val _selectedComponentId = MutableStateFlow<Long?>(null)
    val selectedComponentId: StateFlow<Long?> = _selectedComponentId.asStateFlow()

    private val _selectedComponentWorkItem = MutableStateFlow<ComponentWorkItemEntity?>(null)
    val selectedComponentWorkItem: StateFlow<ComponentWorkItemEntity?> = _selectedComponentWorkItem.asStateFlow()

    private val _selectedContractorId = MutableStateFlow<Long?>(null)
    val selectedContractorId: StateFlow<Long?> = _selectedContractorId.asStateFlow()

    private val _selectedItemId = MutableStateFlow<Long?>(null)
    val selectedItemId: StateFlow<Long?> = _selectedItemId.asStateFlow()

    // Reactive Hierarchy Flows
    val floors: StateFlow<List<FloorEntity>> = _selectedProjectId.flatMapLatest { pId ->
        if (pId != null) repository.getFloorsByProject(pId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rooms: StateFlow<List<RoomEntity>> = _selectedFloorId.flatMapLatest { fId ->
        if (fId != null) repository.getRoomsByFloor(fId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjectRooms: StateFlow<List<RoomEntity>> = _selectedProjectId.flatMapLatest { pId ->
        if (pId != null) repository.getRoomsByProject(pId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val floorLevelComponents: StateFlow<List<ComponentEntity>> = _selectedFloorId.flatMapLatest { fId ->
        if (fId != null) repository.getFloorLevelComponents(fId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val components: StateFlow<List<ComponentEntity>> = combine(
        _selectedFloorId,
        _selectedRoomId
    ) { fId, rId ->
        Pair(fId, rId)
    }.flatMapLatest { (fId, rId) ->
        if (rId != null && rId > 0L) {
            repository.getComponentsByRoom(rId)
        } else if (fId != null) {
            repository.getFloorLevelComponents(fId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val componentWorkItems: StateFlow<List<ComponentWorkItemEntity>> = _selectedComponentId.flatMapLatest { cId ->
        if (cId != null) repository.getWorkItemsForComponent(cId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeWorkItemMeasurements: StateFlow<List<MeasurementEntity>> = combine(
        _selectedComponentId,
        _selectedItemId
    ) { cId, iId ->
        Pair(cId, iId)
    }.flatMapLatest { (cId, iId) ->
        if (cId != null && iId != null) {
            repository.getMeasurementsByComponentAndItem(cId, iId)
        } else if (cId != null) {
            repository.getMeasurementsByComponent(cId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Register filters
    private val _filterProjectId = MutableStateFlow<Long?>(null)
    val filterProjectId: StateFlow<Long?> = _filterProjectId.asStateFlow()

    private val _filterContractorId = MutableStateFlow<Long?>(null)
    val filterContractorId: StateFlow<Long?> = _filterContractorId.asStateFlow()

    private val _filterSearchQuery = MutableStateFlow("")
    val filterSearchQuery: StateFlow<String> = _filterSearchQuery.asStateFlow()

    // Editing measurement state
    private val _editingMeasurement = MutableStateFlow<MeasurementEntity?>(null)
    val editingMeasurement: StateFlow<MeasurementEntity?> = _editingMeasurement.asStateFlow()

    init {
        // Auto-select initial context
        viewModelScope.launch {
            projects.collect { list ->
                if (_selectedProjectId.value == null && list.isNotEmpty()) {
                    _selectedProjectId.value = list.first().id
                }
            }
        }
        viewModelScope.launch {
            floors.collect { list ->
                if (_selectedFloorId.value == null && list.isNotEmpty()) {
                    _selectedFloorId.value = list.first().id
                }
            }
        }
        viewModelScope.launch {
            rooms.collect { list ->
                if (_selectedRoomId.value == null && list.isNotEmpty()) {
                    _selectedRoomId.value = list.first().id
                }
            }
        }
        viewModelScope.launch {
            contractors.collect { list ->
                if (_selectedContractorId.value == null && list.isNotEmpty()) {
                    _selectedContractorId.value = list.first().id
                }
            }
        }
        viewModelScope.launch {
            items.collect { list ->
                if (_selectedItemId.value == null && list.isNotEmpty()) {
                    val defaultItem = list.firstOrNull { it.name.equals("Plaster", ignoreCase = true) } ?: list.first()
                    selectItem(defaultItem.id)
                }
            }
        }

        // Local sharing never auto-starts. The user must explicitly start a new
        // named-user, role-controlled HTTPS session on the current Wi-Fi network.
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun saveCompanyProfile(profile: CompanyProfileEntity) {
        if (profile.practiceName.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertCompanyProfile(profile.copy(id = 1, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun exportCompanyDatabase(destination: android.net.Uri, password: String): String =
        withContext(Dispatchers.IO) { companyDatabasePackageManager.exportTo(destination, password.toCharArray()) }

    suspend fun previewCompanyDatabase(source: android.net.Uri, password: String): CompanyDatabaseImportPreview =
        withContext(Dispatchers.IO) { companyDatabasePackageManager.previewImport(source, password.toCharArray()) }

    suspend fun stageCompanyDatabaseRestore(preview: CompanyDatabaseImportPreview) =
        withContext(Dispatchers.IO) { companyDatabasePackageManager.stageRestore(preview) }

    fun discardCompanyDatabasePreview(preview: CompanyDatabaseImportPreview?) =
        companyDatabasePackageManager.discardPreview(preview)

    fun consumeCompanyDatabaseRestoreMessage(): String? =
        companyDatabasePackageManager.consumeRestoreMessage()

    fun updateProjectProfile(project: ProjectEntity) {
        if (project.name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { repository.updateProject(project) }
    }

    fun saveProjectConsultancyProfile(profile: ProjectConsultancyProfileEntity) {
        val projectId = selectedProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertProjectConsultancyProfile(
                profile.copy(projectId = projectId, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun addProjectScopeItem(category: String, title: String, details: String, status: String) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectScopeItem(
                ProjectScopeItemEntity(
                    projectId = projectId,
                    category = category,
                    title = title.trim(),
                    details = details.trim(),
                    status = status
                )
            )
        }
    }

    fun toggleProjectScopeItem(item: ProjectScopeItemEntity) {
        val nextStatus = if (item.status == "COMPLETE") {
            if (item.category == "EXCLUSION") "EXCLUDED" else "INCLUDED"
        } else {
            "COMPLETE"
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProjectScopeItem(item.copy(status = nextStatus))
        }
    }

    fun deleteProjectScopeItem(item: ProjectScopeItemEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProjectScopeItem(item) }
    }

    fun saveOnboardingResponse(questionCode: String, answer: String, clarification: String = "", templateVersion: Int = 1) {
        val projectId = selectedProjectId.value ?: return
        val existing = projectOnboardingResponses.value.firstOrNull { it.questionCode == questionCode }
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertProjectOnboardingResponse(
                ProjectOnboardingResponseEntity(
                    id = existing?.id ?: 0,
                    projectId = projectId,
                    templateVersion = templateVersion,
                    questionCode = questionCode,
                    answer = answer.trim(),
                    clarification = clarification.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addProjectApproval(type: String, title: String, description: String, phase: String) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectApproval(ProjectApprovalEntity(projectId = projectId, approvalType = type, title = title.trim(), description = description.trim(), phase = phase))
        }
    }

    fun advanceProjectApproval(item: ProjectApprovalEntity) {
        val now = System.currentTimeMillis()
        val next = when (item.status) {
            "PENDING" -> item.copy(status = "SUBMITTED", submittedAt = now)
            "SUBMITTED" -> item.copy(status = "APPROVED", approvedAt = now)
            else -> item.copy(status = "PENDING", submittedAt = null, approvedAt = null)
        }
        viewModelScope.launch(Dispatchers.IO) { repository.updateProjectApproval(next) }
    }

    fun deleteProjectApproval(item: ProjectApprovalEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProjectApproval(item) }
    }

    fun addProjectBacklogItem(category: String, title: String, description: String, priority: String, phase: String) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectBacklogItem(ProjectBacklogEntity(projectId = projectId, category = category, title = title.trim(), description = description.trim(), priority = priority, phase = phase))
        }
    }

    fun toggleProjectBacklogItem(item: ProjectBacklogEntity) {
        val next = if (item.status == "CLOSED") "OPEN" else "CLOSED"
        viewModelScope.launch(Dispatchers.IO) { repository.updateProjectBacklogItem(item.copy(status = next)) }
    }

    fun deleteProjectBacklogItem(item: ProjectBacklogEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProjectBacklogItem(item) }
    }

    fun startLocalPortal() {
        if (localUsers.value.none { it.isActive }) { _portalUserMessage.value = "Create at least one active portal user first."; return }
        localPortalServer.start(
            provider = ::buildPortalSnapshot,
            authenticate = repository::authenticateLocalUser,
            mutate = ::applyPortalMutation
        )
    }

    private suspend fun buildPortalSnapshot(requestedProjectId: Long?): PortalSnapshot {
        val projectRows = repository.allProjects.first()
        val clientRows = repository.allClients.first()
        val contractorRows = repository.allContractors.first()
        val workItemRows = repository.allItems.first()
        val allMeasurementRows = repository.allMeasurements.first()
        val allSheetRows = repository.getMeasurementSheets().first()
        val profile = repository.companyProfile.first()
        val portalUserRows = repository.localUsers.first()
        val auditRows = repository.portalAuditEvents.first()
        val selected = projectRows.firstOrNull { it.id == requestedProjectId }
            ?: projectRows.firstOrNull { it.id == selectedProjectId.value }
            ?: projectRows.firstOrNull()
        val projectId = selected?.id
        val taskRows = projectId?.let { repository.getProjectTasks(it).first() }.orEmpty()
        val selectionRows = projectId?.let { repository.getProjectSelectionItems(it).first() }.orEmpty()
        val scheduleRows = projectId?.let { repository.getProjectSchedules(it).first() }.orEmpty()
        val meetingRows = projectId?.let { repository.getMeetingMinutes(it).first() }.orEmpty()
        val inspectionRows = projectId?.let { repository.getSiteInspections(it).first() }.orEmpty()
        val dailyReportRows = projectId?.let { repository.getDailySiteReports(it).first() }.orEmpty()
        val decisionRows = projectId?.let { repository.getProjectDecisions(it).first() }.orEmpty()
        val siteIssueRows = projectId?.let { repository.getSiteIssues(it).first() }.orEmpty()
        val drawingRows = projectId?.let { repository.getProjectDrawings(it).first() }.orEmpty()
        val consultantRows = projectId?.let { repository.getProjectConsultants(it).first() }.orEmpty()
        val coordinationRows = projectId?.let { repository.getCoordinationItems(it).first() }.orEmpty()
        val floorRows = projectId?.let { repository.getFloorsByProject(it).first() }.orEmpty()
        val measurementRows = projectId?.let { repository.getMeasurementsByProject(it).first() }.orEmpty()
        val sheetRows = allSheetRows.filter { it.projectId == projectId && it.archivedAt == null }
        val measurementCounts = allMeasurementRows.groupingBy { it.projectId }.eachCount()
        return PortalSnapshot(
            companyName = profile?.practiceName?.ifBlank { "ArchiMan" } ?: "ArchiMan",
            projects = projectRows.map { project -> PortalProject(project.id, project.name, project.projectCode, project.projectType, project.status, project.client, project.siteLocation, project.architectInCharge, measurementCounts[project.id] ?: 0) },
            selectedProject = selected?.name,
            selectedProjectId = projectId,
            clients = clientRows.map { PortalOption(it.id, it.name, listOf(it.clientType, it.contactPerson).filter(String::isNotBlank).joinToString(" · ")) },
            contractors = contractorRows.map { PortalOption(it.id, it.name, it.contractorType) },
            workItems = workItemRows.filter { it.isActive }.map { PortalOption(it.id, it.name, "${it.workType} · ${it.calculationType.displayName} · ${it.unit}") },
            floors = floorRows.map { PortalOption(it.id, it.name) },
            tasks = taskRows.map { "${it.title} · ${it.status}" },
            selections = selectionRows.map { "${it.itemName} · ${it.quantity} ${it.unit} · ${it.status}" },
            schedules = scheduleRows.map { "${it.title} · ${formatPortalDate(it.scheduledAt)} · ${it.status}" },
            meetings = meetingRows.map { "${it.title} · ${formatPortalDate(it.meetingAt)}" },
            inspections = inspectionRows.map { "${it.location} · ${it.observation} · ${it.status}" },
            dailyReports = dailyReportRows.map { "${formatPortalDate(it.reportDate)} · ${it.workCompleted}" },
            siteIssues = siteIssueRows.map { "${it.referenceNumber} · ${it.type} · ${it.title} · ${it.status}" },
            decisions = decisionRows.map { "${it.referenceNumber} · ${it.title} · ${it.status}" },
            drawings = drawingRows.map { "${it.drawingNumber} · ${it.title} · ${it.status}" },
            responsibilities = consultantRows.map { "${it.name} · ${it.discipline} · ${it.raciRole.take(1)}" },
            coordination = coordinationRows.map { "${it.referenceNumber} · ${it.subject} · ${it.status}" },
            measurements = measurementRows.takeLast(100).map { "${it.itemName} · ${it.description} · ${it.quantity} ${it.unit}" },
            measurementSheets = sheetRows.map { PortalOption(it.id, it.sheetCode, "${it.itemNameSnapshot} · ${it.status} · Rev ${it.revision}") },
            coordinationRecords = coordinationRows.map { PortalOption(it.id, it.referenceNumber, "${it.type} · ${it.subject} · ${it.status}") },
            siteIssueRecords = siteIssueRows.map { PortalOption(it.id, it.referenceNumber, "${it.type} · ${it.title} · ${it.status}") },
            portalUsers = portalUserRows.map { PortalUserOption(it.id, it.username, it.displayName, it.role, it.isActive) },
            auditEvents = auditRows.take(50).map { "${formatPortalDate(it.occurredAt)} · ${it.username} · ${it.action} · ${it.entityType} · ${it.summary}" },
            companyLegalName = profile?.legalName.orEmpty(),
            companyType = profile?.companyType.orEmpty(),
            companyAddress = profile?.address.orEmpty(),
            companyPhone = profile?.phone.orEmpty(),
            companyEmail = profile?.email.orEmpty()
        )
    }

    private suspend fun applyPortalMutation(principal: PortalPrincipal, mutation: PortalMutation): PortalMutationResult = runCatching {
        fun field(name: String) = mutation.fields[name].orEmpty()
        fun required(name: String) = field(name).takeIf(String::isNotBlank) ?: error("$name is required.")
        fun long(name: String) = field(name).toLongOrNull() ?: 0L
        fun number(name: String, default: Double = 0.0) = field(name).toDoubleOrNull() ?: default
        fun date(name: String): Long? = field(name).takeIf(String::isNotBlank)?.let(::parsePortalDate)
        val projectRequired = mutation.action !in setOf("UPDATE_COMPANY", "ADD_PROJECT", "ADD_CLIENT", "ADD_CONTRACTOR", "QUALIFY_CONTRACTOR", "CREATE_PORTAL_USER", "UPDATE_PORTAL_USER", "SET_PROJECT_STATUS", "ARCHIVE_WORK_ITEM")
        if (projectRequired) requireNotNull(repository.getProjectById(mutation.projectId)) { "Project was not found." }
        var auditProjectId: Long? = mutation.projectId.takeIf { it > 0 }
        val (entityType, entityId) = when (mutation.action) {
            "UPDATE_COMPANY" -> {
                val current = repository.companyProfile.first() ?: CompanyProfileEntity()
                repository.upsertCompanyProfile(current.copy(id = 1, practiceName = mutation.title, legalName = field("legalName"), companyType = field("companyType").ifBlank { "Architecture practice" }, address = field("address"), phone = field("phone"), email = field("email"), updatedAt = System.currentTimeMillis()))
                auditProjectId = null
                "COMPANY_PROFILE" to 1L
            }
            "ADD_PROJECT" -> {
                val clientId = long("clientId")
                val client = clientId.takeIf { it > 0 }?.let { repository.getClientById(it) }
                val id = repository.insertProject(ProjectEntity(name = mutation.title, projectCode = field("code"), projectType = field("type").ifBlank { "Residential" }, clientId = client?.id ?: 0, client = client?.name.orEmpty(), siteLocation = field("location"), architectInCharge = field("architect"), description = mutation.description))
                auditProjectId = id
                "PROJECT" to id
            }
            "ADD_CLIENT" -> "CLIENT" to repository.insertClient(ClientEntity(name = mutation.title, clientType = field("clientType").ifBlank { "Individual" }, contactPerson = field("contactPerson"), contactNo = field("phone"), email = field("email"), address = field("address")))
            "ADD_CONTRACTOR" -> "CONTRACTOR" to repository.insertContractor(ContractorEntity(name = mutation.title, contractorType = field("contractorType").ifBlank { "Civil" }, phone = field("phone"), contactNo = field("phone"), address = field("address")))
            "QUALIFY_CONTRACTOR" -> {
                val contractor = requireNotNull(repository.getContractorById(long("contractorId"))) { "Contractor was not found." }
                val item = requireNotNull(repository.getItemById(long("itemId"))) { "Work item was not found." }
                "CONTRACTOR_QUALIFICATION" to repository.insertQualifiedItem(ContractorQualifiedItemEntity(contractorId = contractor.id, workType = item.workType, itemName = item.name, uom = item.unit, calculationType = item.calculationType))
            }
            "CREATE_PORTAL_USER" -> {
                val password = required("password").toCharArray()
                val id = try {
                    repository.createLocalUser(required("username"), mutation.title, password, field("role").ifBlank { "VIEWER" })
                } finally {
                    password.fill('\u0000')
                }
                auditProjectId = null
                "PORTAL_USER" to id
            }
            "UPDATE_PORTAL_USER" -> {
                val user = requireNotNull(repository.getLocalUserById(long("userId"))) { "Portal user was not found." }
                val newPassword = field("newPassword").takeIf(String::isNotBlank)?.toCharArray()
                try {
                    repository.updateLocalUser(user, field("displayName"), required("role"), required("active").toBooleanStrict(), newPassword)
                } finally {
                    newPassword?.fill('\u0000')
                }
                auditProjectId = null
                "PORTAL_USER" to user.id
            }
            "SET_PROJECT_STATUS" -> {
                val project = requireNotNull(repository.getProjectById(long("targetProjectId"))) { "Project was not found." }
                repository.updateProject(project.copy(status = required("status")))
                auditProjectId = project.id
                "PROJECT" to project.id
            }
            "ARCHIVE_WORK_ITEM" -> {
                val item = requireNotNull(repository.getItemById(long("itemId"))) { "Work item was not found." }
                repository.deleteItem(item)
                auditProjectId = null
                "WORK_ITEM" to item.id
            }
            "ASSIGN_CONTRACTOR" -> {
                val contractor = requireNotNull(repository.getContractorById(long("contractorId"))) { "Contractor was not found." }
                "PROJECT_CONTRACTOR" to repository.insertProjectContractorRef(ProjectContractorCrossRef(projectId = mutation.projectId, contractorId = contractor.id))
            }
            "ADD_FLOOR" -> "FLOOR" to repository.insertFloor(FloorEntity(projectId = mutation.projectId, name = mutation.title, orderIndex = long("order").toInt()))
            "ADD_TASK" -> "TASK" to repository.insertProjectTask(ProjectTaskEntity(projectId = mutation.projectId, title = mutation.title, description = mutation.description, dueDate = date("dueAt"), status = field("status").ifBlank { "OPEN" }))
            "ADD_APPROVAL" -> "APPROVAL" to repository.insertProjectApproval(ProjectApprovalEntity(projectId = mutation.projectId, approvalType = field("approvalType").ifBlank { "CLIENT" }, title = mutation.title, description = mutation.description, phase = field("phase").ifBlank { "DESIGN" }))
            "ADD_BACKLOG" -> "BACKLOG" to repository.insertProjectBacklogItem(ProjectBacklogEntity(projectId = mutation.projectId, title = mutation.title, description = mutation.description, category = field("category").ifBlank { "GENERAL" }, priority = field("priority").ifBlank { "MEDIUM" }, phase = field("phase").ifBlank { "DESIGN" }, dueAt = date("dueAt")))
            "ADD_SCOPE" -> "SCOPE" to repository.insertProjectScopeItem(ProjectScopeItemEntity(projectId = mutation.projectId, title = mutation.title, details = mutation.description, category = field("category").ifBlank { "SCOPE" }, status = field("status").ifBlank { "INCLUDED" }))
            "ADD_SELECTION" -> "SELECTION" to repository.insertProjectSelectionItem(ProjectSelectionItemEntity(projectId = mutation.projectId, itemName = mutation.title, specification = field("specification"), makeOrBrand = field("brand"), quantity = number("quantity", 1.0).also { require(it > 0) { "Quantity must be greater than zero." } }, unit = field("unit").ifBlank { "Nos" }, remarks = field("remarks")))
            "ADD_SCHEDULE" -> "SCHEDULE" to repository.insertProjectSchedule(ProjectScheduleEntity(projectId = mutation.projectId, title = mutation.title, scheduledAt = requireNotNull(date("scheduledAt")) { "Schedule date is required." }, location = field("location"), notes = mutation.description))
            "ADD_DECISION" -> "DECISION" to repository.insertProjectDecision(ProjectDecisionEntity(projectId = mutation.projectId, referenceNumber = mutation.title, title = required("decisionTitle"), context = field("context"), decisionRequired = mutation.description, impact = field("impact"), requestedFrom = field("requestedFrom"), owner = field("owner")))
            "ADD_DAILY_REPORT" -> "DAILY_SITE_REPORT" to repository.upsertDailySiteReport(DailySiteReportEntity(projectId = mutation.projectId, reportDate = requireNotNull(date("reportDate")) { "Report date is required." }, weather = field("weather"), manpower = field("manpower"), workCompleted = mutation.title, materialsReceived = field("materials"), delaysOrConstraints = field("delays"), safetyObservations = field("safety"), nextDayPlan = field("nextPlan"), preparedBy = field("preparedBy")))
            "ADD_SITE_ISSUE" -> "SITE_ISSUE" to repository.createSiteIssue(SiteIssueEntity(projectId = mutation.projectId, referenceNumber = mutation.title, type = field("issueType").ifBlank { "SNAG" }, title = required("issueTitle"), location = field("location"), description = mutation.description, severity = field("severity").ifBlank { "NORMAL" }, assignedTo = field("assignedTo"), correctiveAction = field("corrective")), principal.displayName)
            "ADD_MEETING" -> "MEETING" to repository.insertMeetingMinutes(MeetingMinutesEntity(projectId = mutation.projectId, title = mutation.title, meetingAt = requireNotNull(date("meetingAt")) { "Meeting date is required." }, location = field("location"), attendees = field("attendees"), discussion = mutation.description, decisions = field("decisions"), actionItems = field("actions")))
            "ADD_INSPECTION" -> "INSPECTION" to repository.insertSiteInspection(SiteInspectionEntity(projectId = mutation.projectId, inspectionAt = System.currentTimeMillis(), location = required("location"), inspector = field("inspector"), observation = mutation.title, severity = field("severity").ifBlank { "NORMAL" }, correctiveAction = field("corrective")))
            "ADD_RESPONSIBILITY" -> "RESPONSIBILITY" to repository.insertProjectConsultant(ProjectConsultantEntity(projectId = mutation.projectId, name = mutation.title, organisation = field("organisation"), discipline = required("discipline"), email = field("email"), phone = field("phone"), phase = field("phase").ifBlank { "All phases" }, responsibility = mutation.description, raciRole = field("raciRole").ifBlank { "RESPONSIBLE" }))
            "ADD_COORDINATION" -> "COORDINATION" to repository.createCoordinationItem(CoordinationItemEntity(projectId = mutation.projectId, type = field("recordType").ifBlank { "RFI" }, referenceNumber = mutation.title, subject = required("subject"), discipline = field("discipline").ifBlank { "Architectural" }, location = field("location"), raisedBy = field("raisedBy"), assignedTo = field("assignedTo"), questionOrRequirement = mutation.description, priority = field("priority").ifBlank { "NORMAL" }, dueAt = date("dueAt")), principal.displayName)
            "ADD_MEASUREMENT" -> {
                val contractor = requireNotNull(repository.getContractorById(long("contractorId"))) { "Contractor was not found." }
                require(repository.getProjectContractorRefsSync(mutation.projectId).any { it.contractorId == contractor.id }) { "Assign this contractor to the project first." }
                val item = requireNotNull(repository.getItemById(long("itemId"))) { "Work item was not found." }
                require(repository.getQualifiedItemsForContractorSync(contractor.id).any { it.itemName.equals(item.name, true) }) { "Import this work item into the contractor's work list first." }
                val floor = long("floorId").takeIf { it > 0 }?.let { requireNotNull(repository.getFloorById(it)) { "Floor was not found." }.also { floor -> require(floor.projectId == mutation.projectId) { "Floor belongs to another project." } } }
                val nos = number("nos", 1.0); val length = number("length"); val width = number("width"); val height = number("height"); val deduction = number("deduction")
                require(nos > 0 && deduction >= 0) { "Number must be positive and deduction cannot be negative." }
                val quantity = QuantityCalculator.calculate(item.calculationType, MeasurementInput(no = nos, length = length, breadth = width, height = height, deduction = deduction))
                require(quantity > 0) { "Enter the dimensions required by ${item.calculationType.displayName}." }
                val id = repository.insertMeasurement(MeasurementEntity(projectId = mutation.projectId, floorId = floor?.id, contractorId = contractor.id, contractorName = contractor.name, itemId = item.id, itemName = item.name, unit = item.unit, calculationType = item.calculationType, formulaCode = item.calculationType.name, description = mutation.title, length = length, width = width, height = height, nos = nos, deduction = deduction, quantity = quantity, floor = floor?.name.orEmpty(), remarks = field("remarks")))
                "MEASUREMENT" to id
            }
            "TRANSITION_SHEET" -> {
                val sheetId = long("sheetId")
                val sheet = requireNotNull(repository.getMeasurementSheets().first().firstOrNull { it.id == sheetId && it.projectId == mutation.projectId }) { "Measurement sheet was not found." }
                val target = runCatching { MeasurementSheetStatus.valueOf(required("toStatus")) }.getOrElse { error("Review status is invalid.") }
                repository.transitionMeasurementSheet(sheet.id, target, principal.displayName, field("comment"))
                "MEASUREMENT_SHEET" to sheet.id
            }
            "ARCHIVE_COORDINATION" -> {
                val item = requireNotNull(repository.getCoordinationItemById(long("coordinationId"))) { "Coordination record was not found." }
                require(item.projectId == mutation.projectId) { "Coordination record belongs to another project." }
                repository.archiveCoordinationItem(item, principal.displayName)
                "COORDINATION" to item.id
            }
            "TRANSITION_SITE_ISSUE" -> {
                val issueId = long("issueId")
                val issue = requireNotNull(repository.getSiteIssues(mutation.projectId).first().firstOrNull { it.id == issueId }) { "Snag / NCR was not found." }
                repository.transitionSiteIssue(issue, required("toStatus"), field("note"), principal.displayName)
                "SITE_ISSUE" to issue.id
            }
            "TRANSITION_COORDINATION" -> {
                val item = requireNotNull(repository.getCoordinationItemById(long("coordinationId"))) { "Coordination record was not found." }
                require(item.projectId == mutation.projectId) { "Coordination record belongs to another project." }
                repository.transitionCoordinationItem(item, required("toStatus"), field("note"), principal.displayName)
                "COORDINATION" to item.id
            }
            "ARCHIVE_SHEET" -> {
                val sheetId = long("sheetId")
                val sheet = requireNotNull(repository.getMeasurementSheets().first().firstOrNull { it.id == sheetId && it.projectId == mutation.projectId }) { "Measurement sheet was not found." }
                repository.archiveMeasurementSheet(sheet.id, principal.displayName)
                "MEASUREMENT_SHEET" to sheet.id
            }
            else -> error("Unsupported edit.")
        }
        repository.recordPortalAudit(PortalAuditEventEntity(userId = principal.userId, username = principal.username, action = mutation.action, entityType = entityType, entityId = entityId, projectId = auditProjectId, summary = mutation.title.take(160), sourceAddress = mutation.sourceAddress))
        PortalMutationResult(true, "Saved ${mutation.title}.")
    }.getOrElse { PortalMutationResult(false, it.message ?: "Could not save this record.") }

    private fun parsePortalDate(value: String): Long = requireNotNull(
        listOf("yyyy-MM-dd'T'HH:mm", "yyyy-MM-dd").firstNotNullOfOrNull { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)?.time }.getOrNull()
        }
    ) { "Date is invalid." }

    private fun formatPortalDate(value: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))

    fun stopLocalPortal() = localPortalServer.stop()

    fun createLocalUser(username: String, displayName: String, password: String, role: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.createLocalUser(username, displayName, password.toCharArray(), role) }
                .onSuccess { _portalUserMessage.value = "Portal user created." }
                .onFailure { _portalUserMessage.value = it.message ?: "Could not create portal user." }
        }
    }

    fun setLocalUserActive(user: LocalUserEntity, active: Boolean) {
        if (user.role == "ADMIN" && user.isActive && !active && localUsers.value.count { it.role == "ADMIN" && it.isActive } <= 1) {
            _portalUserMessage.value = "At least one active administrator is required."
            return
        }
        viewModelScope.launch(Dispatchers.IO) { repository.setLocalUserActive(user, active) }
    }

    fun clearPortalUserMessage() { _portalUserMessage.value = null }

    override fun onCleared() {
        localPortalServer.close()
        super.onCleared()
    }

    fun addProjectTask(title: String, description: String = "") {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectTask(ProjectTaskEntity(projectId = projectId, title = title.trim(), description = description.trim()))
        }
    }

    fun toggleProjectTask(task: ProjectTaskEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProjectTask(task.copy(status = if (task.status == "DONE") "OPEN" else "DONE"))
        }
    }

    fun deleteProjectTask(task: ProjectTaskEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProjectTask(task) }
    }

    fun addProjectSelectionItem(name: String, specification: String, brand: String, quantity: Double, unit: String, remarks: String) {
        val projectId = selectedProjectId.value ?: return
        if (name.isBlank() || quantity <= 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectSelectionItem(ProjectSelectionItemEntity(
                projectId = projectId, itemName = name.trim(), specification = specification.trim(),
                makeOrBrand = brand.trim(), quantity = quantity, unit = unit.trim().ifBlank { "Nos" }, remarks = remarks.trim()
            ))
        }
    }

    fun toggleProjectSelectionItem(item: ProjectSelectionItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProjectSelectionItem(item.copy(status = if (item.status == "SELECTED") "PENDING" else "SELECTED"))
        }
    }

    fun deleteProjectSelectionItem(item: ProjectSelectionItemEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProjectSelectionItem(item) }
    }

    fun addProjectSchedule(title: String, scheduledAt: Long, location: String, notes: String) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectSchedule(ProjectScheduleEntity(projectId = projectId, title = title.trim(), scheduledAt = scheduledAt, location = location.trim(), notes = notes.trim()))
        }
    }

    fun toggleProjectSchedule(item: ProjectScheduleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProjectSchedule(item.copy(status = if (item.status == "DONE") "SCHEDULED" else "DONE"))
        }
    }

    fun deleteProjectSchedule(item: ProjectScheduleEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteProjectSchedule(item) }
    }

    fun addMeetingMinutes(title: String, meetingAt: Long, location: String, attendees: String, discussion: String, decisions: String, actionItems: String) {
        val projectId = selectedProjectId.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMeetingMinutes(MeetingMinutesEntity(projectId = projectId, title = title.trim(), meetingAt = meetingAt, location = location.trim(), attendees = attendees.trim(), discussion = discussion.trim(), decisions = decisions.trim(), actionItems = actionItems.trim()))
        }
    }

    fun deleteMeetingMinutes(item: MeetingMinutesEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteMeetingMinutes(item) }
    }

    fun addSiteInspection(location: String, inspector: String, observation: String, severity: String, correctiveAction: String, photoUri: String? = null) {
        val projectId = selectedProjectId.value ?: return
        if (location.isBlank() || observation.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSiteInspection(SiteInspectionEntity(projectId = projectId, inspectionAt = System.currentTimeMillis(), location = location.trim(), inspector = inspector.trim(), observation = observation.trim(), severity = severity, correctiveAction = correctiveAction.trim(), photoUri = photoUri))
        }
    }

    fun closeSiteInspection(item: SiteInspectionEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateSiteInspection(item.copy(status = if (item.status == "CLOSED") "OPEN" else "CLOSED")) }
    }

    fun deleteSiteInspection(item: SiteInspectionEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteSiteInspection(item) }
    }

    fun addDailySiteReport(workCompleted: String, weather: String, manpower: String, materials: String, delays: String, safety: String, nextPlan: String, preparedBy: String, photoUri: String?) {
        val projectId = selectedProjectId.value ?: return
        if (workCompleted.isBlank()) return
        val day = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).run { parse(format(java.util.Date()))?.time ?: System.currentTimeMillis() }
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertDailySiteReport(DailySiteReportEntity(projectId = projectId, reportDate = day, weather = weather.trim(), manpower = manpower.trim(), workCompleted = workCompleted.trim(), materialsReceived = materials.trim(), delaysOrConstraints = delays.trim(), safetyObservations = safety.trim(), nextDayPlan = nextPlan.trim(), preparedBy = preparedBy.trim(), photoUri = photoUri))
        }
    }

    fun deleteDailySiteReport(item: DailySiteReportEntity) = viewModelScope.launch(Dispatchers.IO) { repository.deleteDailySiteReport(item) }

    fun addProjectDecision(reference: String, title: String, context: String, required: String, impact: String, requestedFrom: String, owner: String) {
        val projectId = selectedProjectId.value ?: return
        if (reference.isBlank() || title.isBlank() || required.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectDecision(ProjectDecisionEntity(projectId = projectId, referenceNumber = reference.trim(), title = title.trim(), context = context.trim(), decisionRequired = required.trim(), impact = impact.trim(), requestedFrom = requestedFrom.trim(), owner = owner.trim()))
        }
    }

    fun decideProjectDecision(item: ProjectDecisionEntity, finalDecision: String) {
        if (finalDecision.isBlank() || item.status == "DECIDED") return
        viewModelScope.launch(Dispatchers.IO) { repository.updateProjectDecision(item.copy(finalDecision = finalDecision.trim(), status = "DECIDED", decidedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())) }
    }

    fun addSiteIssue(reference: String, type: String, title: String, location: String, description: String, severity: String, assignedTo: String, correctiveAction: String, evidenceUri: String?) {
        val projectId = selectedProjectId.value ?: return
        if (reference.isBlank() || title.isBlank() || description.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.createSiteIssue(SiteIssueEntity(projectId = projectId, referenceNumber = reference.trim(), type = type, title = title.trim(), location = location.trim(), description = description.trim(), severity = severity, assignedTo = assignedTo.trim(), correctiveAction = correctiveAction.trim(), evidenceUri = evidenceUri))
        }
    }

    fun advanceSiteIssue(item: SiteIssueEntity, verificationNote: String = "") {
        val next = when (item.status) { "OPEN" -> "IN_PROGRESS"; "IN_PROGRESS" -> "READY_FOR_VERIFICATION"; "READY_FOR_VERIFICATION" -> "CLOSED"; else -> return }
        viewModelScope.launch(Dispatchers.IO) { runCatching { repository.transitionSiteIssue(item, next, verificationNote) } }
    }

    fun addProjectConsultant(name: String, organisation: String, discipline: String, email: String, phone: String, phase: String, responsibility: String, raciRole: String) {
        val projectId = selectedProjectId.value ?: return
        if (name.isBlank() || discipline.isBlank() || responsibility.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.insertProjectConsultant(
                    ProjectConsultantEntity(
                        projectId = projectId,
                        name = name.trim(),
                        organisation = organisation.trim(),
                        discipline = discipline.trim(),
                        email = email.trim(),
                        phone = phone.trim(),
                        phase = phase.trim().ifBlank { "All phases" },
                        responsibility = responsibility.trim(),
                        raciRole = raciRole
                    )
                )
            }.onFailure { _coordinationMessage.value = "That consultant and discipline already exist." }
        }
    }

    fun archiveProjectConsultant(item: ProjectConsultantEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateProjectConsultant(item.copy(status = "ARCHIVED")) }
    }

    fun coordinationEvents(itemId: Long): Flow<List<CoordinationEventEntity>> =
        repository.getCoordinationEvents(itemId)

    fun addCoordinationItem(type: String, referenceNumber: String, subject: String, discipline: String, location: String, raisedBy: String, assignedTo: String, requirement: String, priority: String, dueAt: Long?, linkedDrawingRevisionId: Long?) {
        val projectId = selectedProjectId.value ?: return
        if (referenceNumber.isBlank() || subject.isBlank() || requirement.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.createCoordinationItem(
                    CoordinationItemEntity(
                        projectId = projectId,
                        type = type,
                        referenceNumber = referenceNumber.trim(),
                        subject = subject.trim(),
                        discipline = discipline.trim().ifBlank { "Architectural" },
                        location = location.trim(),
                        raisedBy = raisedBy.trim(),
                        assignedTo = assignedTo.trim(),
                        questionOrRequirement = requirement.trim(),
                        priority = priority,
                        dueAt = dueAt,
                        linkedDrawingRevisionId = linkedDrawingRevisionId
                    )
                )
            }.onFailure { _coordinationMessage.value = it.message ?: "Could not create coordination record." }
        }
    }

    fun transitionCoordinationItem(item: CoordinationItemEntity, status: String, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.transitionCoordinationItem(item, status, note) }
                .onFailure { _coordinationMessage.value = it.message ?: "Could not update coordination record." }
        }
    }

    fun archiveCoordinationItem(item: CoordinationItemEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.archiveCoordinationItem(item) }
    }

    fun clearCoordinationMessage() { _coordinationMessage.value = null }

    fun registerDrawing(
        drawingNumber: String,
        title: String,
        discipline: String,
        revisionCode: String,
        fileName: String,
        mimeType: String,
        fileUri: String,
        issueStatus: String,
        revisionNotes: String,
        isAsBuilt: Boolean,
        revisionSource: String = "",
        severity: String = "NORMAL"
    ) {
        val projectId = selectedProjectId.value ?: return
        if (drawingNumber.isBlank() || title.isBlank() || revisionCode.isBlank() || fileUri.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.createDrawingWithRevision(
                ProjectDrawingEntity(
                    projectId = projectId,
                    drawingNumber = drawingNumber.trim(),
                    title = title.trim(),
                    discipline = discipline.trim().ifBlank { "Architectural" },
                    status = issueStatus
                ),
                DrawingRevisionEntity(
                    projectId = projectId,
                    drawingId = 0,
                    revisionCode = revisionCode.trim(),
                    fileName = fileName,
                    mimeType = mimeType,
                    fileUri = fileUri,
                    issueStatus = issueStatus,
                    revisionNotes = revisionNotes.trim(),
                    isAsBuilt = isAsBuilt,
                    revisionSource = revisionSource,
                    severity = severity.ifBlank { "NORMAL" },
                    issuedAt = if (issueStatus == "WIP") null else System.currentTimeMillis()
                )
            )
        }
    }

    fun addDrawingRevision(
        drawing: ProjectDrawingEntity,
        revisionCode: String,
        fileName: String,
        mimeType: String,
        fileUri: String,
        issueStatus: String,
        revisionNotes: String,
        isAsBuilt: Boolean,
        revisionSource: String = "",
        severity: String = "NORMAL"
    ) {
        if (revisionCode.isBlank() || fileUri.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDrawingRevision(
                DrawingRevisionEntity(
                    projectId = drawing.projectId,
                    drawingId = drawing.id,
                    revisionCode = revisionCode.trim(),
                    fileName = fileName,
                    mimeType = mimeType,
                    fileUri = fileUri,
                    issueStatus = issueStatus,
                    revisionNotes = revisionNotes.trim(),
                    isAsBuilt = isAsBuilt,
                    revisionSource = revisionSource,
                    severity = severity.ifBlank { "NORMAL" },
                    issuedAt = if (issueStatus == "WIP") null else System.currentTimeMillis()
                )
            )
            repository.updateProjectDrawing(drawing.copy(status = issueStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    /** Opens the single authoritative measurement editor using current project context. */
    fun openCanonicalMeasurement(): Boolean {
        val contractorId = _selectedContractorId.value ?: contractors.value.firstOrNull()?.id ?: return false
        val floor = floors.value.firstOrNull { it.id == _selectedFloorId.value } ?: floors.value.firstOrNull() ?: return false
        val connected = _selectedComponentWorkItem.value
        val master = items.value.firstOrNull { it.id == (connected?.itemId ?: _selectedItemId.value) }
        val itemName = connected?.itemName ?: master?.name ?: return false
        val unit = connected?.unit ?: master?.unit ?: return false
        val formula = connected?.calculationType ?: master?.calculationType ?: return false
        startDedicatedMeasurementSession(contractorId, itemName, unit, formula, floor.id, floor.name, connected?.itemId ?: master?.id)
        _currentScreen.value = AppScreen.DEDICATED_MEASUREMENT
        return true
    }

    // Context selection methods
    fun selectProject(projectId: Long) {
        _selectedProjectId.value = projectId
        _selectedHomeTab.value = HomeTab.PROJECTS
        _selectedProjectSection.value = ProjectSection.OVERVIEW
        _selectedFloorId.value = null
        _selectedRoomId.value = null
        _selectedComponentId.value = null
        _selectedComponentWorkItem.value = null

        val projectContractors = contractors.value.filter { it.projectId == projectId }
        if (projectContractors.isNotEmpty() && projectContractors.none { it.id == _selectedContractorId.value }) {
            _selectedContractorId.value = projectContractors.first().id
        }
    }

    fun selectFloor(floorId: Long) {
        _selectedFloorId.value = floorId
        _selectedRoomId.value = null
        _selectedComponentId.value = null
        _selectedComponentWorkItem.value = null
    }

    fun selectRoom(roomId: Long) {
        _selectedRoomId.value = roomId
        _selectedComponentId.value = null
        _selectedComponentWorkItem.value = null
    }

    fun selectComponent(componentId: Long) {
        _selectedComponentId.value = componentId
        _selectedComponentWorkItem.value = null
    }

    fun selectComponentWorkItem(item: ComponentWorkItemEntity) {
        _selectedComponentWorkItem.value = item
        _selectedItemId.value = item.itemId
    }

    fun selectContractor(contractorId: Long) {
        _selectedContractorId.value = contractorId
    }

    fun selectItem(itemId: Long) {
        _selectedItemId.value = itemId
    }

    // Breadcrumb navigation jumps
    fun jumpToHome() {
        _currentScreen.value = AppScreen.HOME
    }

    fun jumpToProject() {
        _currentScreen.value = AppScreen.PROJECT_WORKSPACE
    }

    fun jumpToFloor(floorId: Long) {
        _selectedFloorId.value = floorId
        _currentScreen.value = AppScreen.PROJECT_WORKSPACE
    }

    fun jumpToFloorLevel(floorId: Long) {
        _selectedFloorId.value = floorId
        _selectedRoomId.value = 0L
        _currentScreen.value = AppScreen.ROOM_WORKSPACE
    }

    fun selectFloorLevel(floorId: Long) {
        _selectedFloorId.value = floorId
        _selectedRoomId.value = 0L
        _selectedComponentId.value = null
        _selectedComponentWorkItem.value = null
    }

    fun jumpToRoom(roomId: Long) {
        _selectedRoomId.value = roomId
        _currentScreen.value = AppScreen.ROOM_WORKSPACE
    }

    fun jumpToComponent(componentId: Long) {
        _selectedComponentId.value = componentId
        _currentScreen.value = AppScreen.ROOM_WORKSPACE
    }

    // Floor Actions
    fun addFloor(name: String, onComplete: (Long) -> Unit = {}) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertFloor(FloorEntity(projectId = projId, name = name, orderIndex = floors.value.size))
            _selectedFloorId.value = id
            onComplete(id)
        }
    }

    fun duplicateFloor(sourceFloorId: Long, newFloorName: String, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val newFloorId = repository.duplicateFloor(sourceFloorId, newFloorName)
            if (newFloorId > 0) {
                _selectedFloorId.value = newFloorId
                onComplete(newFloorId)
            }
        }
    }

    fun deleteFloor(floor: FloorEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteFloor(floor)
            if (_selectedFloorId.value == floor.id) {
                _selectedFloorId.value = null
            }
        }
    }

    // Room Actions
    fun addRoom(name: String, onComplete: (Long) -> Unit = {}) {
        val projId = _selectedProjectId.value ?: return
        val floorId = _selectedFloorId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertRoom(RoomEntity(projectId = projId, floorId = floorId, name = name, orderIndex = rooms.value.size))
            _selectedRoomId.value = id
            onComplete(id)
        }
    }

    fun duplicateRoom(sourceRoomId: Long, newRoomName: String, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val newRoomId = repository.duplicateRoom(sourceRoomId, newRoomName)
            if (newRoomId > 0) {
                _selectedRoomId.value = newRoomId
                onComplete(newRoomId)
            }
        }
    }

    fun copyRoomStructure(targetRoomId: Long, sourceRoomId: Long, onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.copyRoomStructure(targetRoomId, sourceRoomId)
            onComplete()
        }
    }

    fun deleteRoom(room: RoomEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteRoom(room)
            if (_selectedRoomId.value == room.id) {
                _selectedRoomId.value = null
            }
        }
    }

    // Component Actions
    fun addComponent(
        name: String,
        type: ComponentType,
        length: Double,
        width: Double,
        height: Double,
        thickness: Double,
        autoConnectWork: Boolean = true,
        onComplete: (Long) -> Unit = {}
    ) {
        val projId = _selectedProjectId.value ?: return
        val floorId = _selectedFloorId.value ?: return
        val roomId = _selectedRoomId.value ?: 0L // 0L means Whole Floor Level

        viewModelScope.launch(Dispatchers.IO) {
            val compId = repository.insertComponent(
                ComponentEntity(
                    projectId = projId,
                    floorId = floorId,
                    roomId = roomId,
                    name = name,
                    type = type,
                    length = length,
                    width = width,
                    height = height,
                    thickness = thickness
                )
            )

            if (autoConnectWork) {
                val suggestedItems = getSuggestedWorkItemsForType(type)
                val allMaster = items.value
                val toConnect = mutableListOf<ComponentWorkItemEntity>()
                for (sName in suggestedItems) {
                    val mItem = allMaster.firstOrNull { it.name.equals(sName, ignoreCase = true) }
                    if (mItem != null) {
                        toConnect.add(
                            ComponentWorkItemEntity(
                                componentId = compId,
                                itemId = mItem.id,
                                itemName = mItem.name,
                                unit = mItem.unit,
                                calculationType = mItem.calculationType
                            )
                        )
                    }
                }
                if (toConnect.isNotEmpty()) {
                    repository.insertConnectedWorkItems(toConnect)
                }
            }

            _selectedComponentId.value = compId
            onComplete(compId)
        }
    }

    fun addFloorBeam(
        floorId: Long,
        name: String,
        length: Double,
        width: Double,
        depth: Double,
        onComplete: (Long) -> Unit = {}
    ) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val compId = repository.insertComponent(
                ComponentEntity(
                    projectId = projId,
                    floorId = floorId,
                    roomId = 0L,
                    name = name,
                    type = ComponentType.BEAM,
                    length = length,
                    width = width,
                    height = depth,
                    thickness = width
                )
            )

            val allMaster = items.value
            val toConnect = mutableListOf<ComponentWorkItemEntity>()
            listOf("Beam Concrete", "Shuttering & Formwork", "RCC", "Plaster").forEach { sName ->
                allMaster.firstOrNull { it.name.equals(sName, ignoreCase = true) }?.let { mItem ->
                    toConnect.add(
                        ComponentWorkItemEntity(
                            componentId = compId,
                            itemId = mItem.id,
                            itemName = mItem.name,
                            unit = mItem.unit,
                            calculationType = mItem.calculationType
                        )
                    )
                }
            }
            if (toConnect.isNotEmpty()) {
                repository.insertConnectedWorkItems(toConnect)
            }
            _selectedFloorId.value = floorId
            _selectedRoomId.value = 0L
            _selectedComponentId.value = compId
            onComplete(compId)
        }
    }

    fun addFloorSlab(
        floorId: Long,
        name: String,
        length: Double,
        width: Double,
        thickness: Double,
        onComplete: (Long) -> Unit = {}
    ) {
        val projId = _selectedProjectId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val compId = repository.insertComponent(
                ComponentEntity(
                    projectId = projId,
                    floorId = floorId,
                    roomId = 0L,
                    name = name,
                    type = ComponentType.SLAB,
                    length = length,
                    width = width,
                    height = 0.0,
                    thickness = thickness
                )
            )

            val allMaster = items.value
            val toConnect = mutableListOf<ComponentWorkItemEntity>()
            listOf("Slab Concrete", "Shuttering & Formwork", "RCC", "Waterproofing").forEach { sName ->
                allMaster.firstOrNull { it.name.equals(sName, ignoreCase = true) }?.let { mItem ->
                    toConnect.add(
                        ComponentWorkItemEntity(
                            componentId = compId,
                            itemId = mItem.id,
                            itemName = mItem.name,
                            unit = mItem.unit,
                            calculationType = mItem.calculationType
                        )
                    )
                }
            }
            if (toConnect.isNotEmpty()) {
                repository.insertConnectedWorkItems(toConnect)
            }
            _selectedFloorId.value = floorId
            _selectedRoomId.value = 0L
            _selectedComponentId.value = compId
            onComplete(compId)
        }
    }

    fun duplicateComponent(sourceComponentId: Long, newName: String, copyWork: Boolean = true, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.duplicateComponent(sourceComponentId, newName, copyWork)
            if (newId > 0) {
                _selectedComponentId.value = newId
                onComplete(newId)
            }
        }
    }

    fun deleteComponent(component: ComponentEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComponent(component)
            if (_selectedComponentId.value == component.id) {
                _selectedComponentId.value = null
            }
        }
    }

    fun connectWorkItem(componentId: Long, item: ItemMasterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertConnectedWorkItem(
                ComponentWorkItemEntity(
                    componentId = componentId,
                    itemId = item.id,
                    itemName = item.name,
                    unit = item.unit,
                    calculationType = item.calculationType
                )
            )
        }
    }

    fun removeConnectedWorkItem(item: ComponentWorkItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConnectedWorkItem(item)
        }
    }

    fun getSuggestedWorkItemsForType(type: ComponentType): List<String> {
        return when (type) {
            ComponentType.WALL -> listOf("Brickwork", "Plaster", "Putty", "Painting")
            ComponentType.FLOOR -> listOf("Flooring", "PCC", "Waterproofing", "Skirting")
            ComponentType.CEILING -> listOf("False Ceiling", "Putty", "Painting")
            ComponentType.OPENING -> listOf("Doors/windows", "Painting")
            ComponentType.SLAB -> listOf("Slab Concrete", "Shuttering & Formwork", "RCC", "Waterproofing")
            ComponentType.BEAM -> listOf("Beam Concrete", "Shuttering & Formwork", "RCC", "Plaster")
            ComponentType.COLUMN -> listOf("RCC", "Shuttering & Formwork", "Plaster", "Painting")
            ComponentType.COLUMN_BEAM -> listOf("Beam Concrete", "RCC", "Shuttering & Formwork", "Plaster")
            ComponentType.WATERPROOFING -> listOf("Waterproofing", "PCC")
            ComponentType.OTHER -> listOf("RCC", "Plaster", "Painting")
        }
    }

    fun startEditingMeasurement(measurement: MeasurementEntity) {
        _editingMeasurement.value = measurement
    }

    fun stopEditingMeasurement() {
        _editingMeasurement.value = null
    }

    fun updateMeasurement(measurement: MeasurementEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.updateMeasurement(measurement) }
                .onSuccess { _editingMeasurement.value = null }
        }
    }

    fun deleteMeasurement(measurement: MeasurementEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMeasurement(measurement)
        }
    }

    // Home Tab Switching
    fun setHomeTab(tab: HomeTab) {
        _selectedHomeTab.value = tab
    }

    fun setDirectorySection(section: DirectorySection) {
        _selectedDirectorySection.value = section
    }

    fun setProjectSection(section: ProjectSection) {
        _selectedProjectSection.value = section
    }

    // Client Management
    fun addClient(name: String, address: String, contactNo: String, onComplete: (Long) -> Unit = {}) {
        addClient(ClientEntity(name = name.trim(), address = address.trim(), contactNo = contactNo.trim()), onComplete)
    }

    fun addClient(client: ClientEntity, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertClient(client)
            onComplete(id)
        }
    }

    fun updateClient(client: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateClient(client)
        }
    }

    fun deleteClient(client: ClientEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteClient(client)
        }
    }

    // Contractor Management with Qualified Items
    fun addContractorWithQualifiedItems(
        name: String,
        address: String,
        contactNo: String,
        contractorType: String = "Civil",
        qualifiedItemsList: List<Pair<String, String>>, // itemName, uom
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val contractorId = repository.insertContractor(
                ContractorEntity(
                    name = name.trim(),
                    address = address.trim(),
                    contactNo = contactNo.trim(),
                    phone = contactNo.trim(),
                    contractorType = contractorType
                )
            )
            if (qualifiedItemsList.isNotEmpty()) {
                val entities = qualifiedItemsList.map { (iName, uom) ->
                    val calcType = when (uom.lowercase().trim()) {
                        "m³", "cu.m", "cum" -> CalculationType.VOLUME
                        "m", "rmt", "r.m." -> CalculationType.RUNNING_LENGTH
                        "nos", "no", "unit" -> CalculationType.NOS
                        else -> if (iName.contains("plaster", ignoreCase = true)) CalculationType.WALL_PLASTER else CalculationType.AREA
                    }
                    ContractorQualifiedItemEntity(
                        contractorId = contractorId,
                        workType = WorkCatalog.classify(contractorType, iName),
                        itemName = iName.trim(),
                        uom = uom.trim(),
                        calculationType = calcType,
                    )
                }
                repository.insertQualifiedItems(entities)
            }
            onComplete(contractorId)
        }
    }

    fun addQualifiedItemToContractor(
        contractorId: Long,
        itemName: String,
        uom: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val calcType = when (uom.lowercase().trim()) {
                "m³", "cu.m", "cum" -> CalculationType.VOLUME
                "m", "rmt", "r.m." -> CalculationType.RUNNING_LENGTH
                "nos", "no", "unit" -> CalculationType.NOS
                else -> if (itemName.contains("plaster", ignoreCase = true)) CalculationType.WALL_PLASTER else CalculationType.AREA
            }
            repository.insertQualifiedItem(
                ContractorQualifiedItemEntity(
                    contractorId = contractorId,
                    workType = WorkCatalog.classify(
                        contractors.value.firstOrNull { it.id == contractorId }?.contractorType.orEmpty(),
                        itemName
                    ),
                    itemName = itemName.trim(),
                    uom = uom.trim(),
                    calculationType = calcType,
                )
            )
        }
    }

    fun deleteQualifiedItem(item: ContractorQualifiedItemEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteQualifiedItem(item)
        }
    }

    // Create only the project identity. Team assignment and measurement levels
    // are explicit project setup actions after creation.
    fun createProject(
        name: String,
        projectCode: String,
        clientName: String,
        clientId: Long,
        projectType: String,
        siteLocation: String,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val projId = repository.insertProject(
                ProjectEntity(
                    name = name.trim(),
                    projectCode = projectCode.trim(),
                    client = clientName.trim(),
                    clientId = clientId,
                    projectType = projectType.trim(),
                    siteLocation = siteLocation.trim()
                )
            )
            _selectedProjectId.value = projId
            // Keep measurement entry ready without asking users to design the
            // building hierarchy during project creation. More levels are added
            // from the project's measurement setup.
            _selectedFloorId.value = repository.insertFloor(
                FloorEntity(projectId = projId, name = "Ground Floor", orderIndex = 0)
            )
            onComplete(projId)
        }
    }

    fun assignContractorToProject(projectId: Long, contractorId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertProjectContractorRef(ProjectContractorCrossRef(projectId = projectId, contractorId = contractorId))
        }
    }

    fun removeContractorFromProject(projectId: Long, contractorId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProjectContractorRef(projectId, contractorId)
        }
    }

    // Dedicated Measurement Flow Methods
    fun startDedicatedMeasurement(
        projectId: Long,
        contractorId: Long,
        itemName: String,
        uom: String,
        calcType: CalculationType,
        floorId: Long,
        floorName: String,
        itemId: Long? = null
    ) {
        _selectedProjectId.value = projectId
        _dedicatedContractorId.value = contractorId
        _dedicatedItemName.value = itemName
        _dedicatedItemUom.value = uom
        _dedicatedCalculationType.value = calcType
        _dedicatedFloorId.value = floorId
        _dedicatedFloorName.value = floorName
        _dedicatedItemId.value = itemId
        _currentScreen.value = AppScreen.DEDICATED_MEASUREMENT
    }

    suspend fun findBrickworkMeasurements(projectId: Long, floorId: Long?, floorName: String): List<MeasurementEntity> {
        return repository.getBrickworkMeasurementsForFloor(projectId, floorId, floorName)
    }

    fun saveBatchMeasurements(
        measurementsList: List<MeasurementEntity>,
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val valid = measurementsList.filter { it.description.isNotBlank() || it.length > 0 || it.height > 0 || it.quantity > 0 }
            val count = repository.insertMeasurementBatch(valid)
            withContext(Dispatchers.Main) { onComplete(count) }
        }
    }

    // Projects & Contractors
    fun addProject(name: String, client: String, siteLocation: String, onComplete: (Long) -> Unit = {}) {
        addProjectWithFloors(name, client, siteLocation, listOf("Ground Floor", "1st Floor"), onComplete)
    }

    fun addProjectWithFloors(
        name: String,
        client: String,
        siteLocation: String,
        floorNames: List<String>,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertProject(ProjectEntity(name = name, client = client, siteLocation = siteLocation))
            _selectedProjectId.value = id
            var firstFloorId: Long? = null
            floorNames.filter { it.isNotBlank() }.forEachIndexed { idx, fName ->
                val fId = repository.insertFloor(FloorEntity(projectId = id, name = fName.trim(), orderIndex = idx))
                if (firstFloorId == null) firstFloorId = fId
            }
            if (firstFloorId == null) {
                firstFloorId = repository.insertFloor(FloorEntity(projectId = id, name = "Ground Floor", orderIndex = 0))
            }
            _selectedFloorId.value = firstFloorId
            onComplete(id)
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProject(project)
        }
    }

    fun addContractor(projectId: Long, name: String, phone: String, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertContractor(ContractorEntity(projectId = projectId, name = name, phone = phone))
            _selectedContractorId.value = id
            onComplete(id)
        }
    }

    fun deleteContractor(contractor: ContractorEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteContractor(contractor)
        }
    }

    // Item Master & Formulas
    fun addItem(item: ItemMasterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertItem(item)
        }
    }

    fun transitionMeasurementSheet(sheetId: Long, status: MeasurementSheetStatus, actor: String, comment: String = "", onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { repository.transitionMeasurementSheet(sheetId, status, actor, comment) }
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }

    fun reviewEvents(sheetId: Long): Flow<List<MeasurementReviewEventEntity>> = repository.getReviewEvents(sheetId)

    fun importWorkCatalog(json: String, onComplete: (Result<Int>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val imported = CatalogDocumentParser.toMasterItems(CatalogDocumentParser.parse(json))
                val existing = items.value.map { WorkCatalog.normalizeName(it.name) }.toMutableSet()
                var added = 0
                imported.forEach { item ->
                    if (existing.add(WorkCatalog.normalizeName(item.name))) {
                        repository.insertItem(item)
                        added++
                    }
                }
                added
            }
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }

    fun addItem(name: String, unit: String, calcType: CalculationType) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertItem(
                ItemMasterEntity(
                    itemCode = WorkCatalog.codeFor("General Works", name),
                    name = name,
                    unit = unit,
                    calculationType = calcType,
                    isPredefined = false
                )
            )
        }
    }

    fun updateItem(item: ItemMasterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: ItemMasterEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteItem(item)
        }
    }

    fun mergeWorkItems(sourceId: Long, canonicalId: Long, onComplete: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { repository.mergeWorkItems(sourceId, canonicalId) }
            withContext(Dispatchers.Main) { onComplete(result) }
        }
    }

    // Contractor Update
    fun updateContractor(contractor: ContractorEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateContractor(contractor)
        }
    }

    // Dedicated Measurement Session Helper
    fun startDedicatedMeasurementSession(
        contractorId: Long,
        itemName: String,
        uom: String,
        calcType: CalculationType,
        floorId: Long,
        floorName: String,
        itemId: Long? = null
    ) {
        _dedicatedContractorId.value = contractorId
        _dedicatedItemName.value = itemName
        _dedicatedItemUom.value = uom
        _dedicatedCalculationType.value = calcType
        _dedicatedFloorId.value = floorId
        _dedicatedFloorName.value = floorName
        _dedicatedItemId.value = itemId ?: items.value.firstOrNull {
            WorkCatalog.normalizeName(it.name) == WorkCatalog.normalizeName(itemName)
        }?.id
    }

    // Filter controls
    fun setFilterProject(id: Long?) { _filterProjectId.value = id }
    fun setFilterContractor(id: Long?) { _filterContractorId.value = id }
    fun setFilterSearchQuery(query: String) { _filterSearchQuery.value = query }

}
