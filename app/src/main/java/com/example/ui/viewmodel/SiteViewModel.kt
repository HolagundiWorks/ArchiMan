package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.SiteRepository
import com.example.domain.MeasurementInput
import com.example.domain.QuantityCalculator
import com.example.domain.WorkCatalog
import com.example.domain.CatalogDocumentParser
import com.example.domain.MeasurementSheetStatus
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.DirectorySection
import com.example.ui.navigation.HomeTab
import com.example.portal.LocalPortalServer
import com.example.portal.PortalProject
import com.example.portal.PortalSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class QuickEntryFormState(
    val description: String = "",
    val length: String = "",
    val width: String = "",
    val height: String = "",
    val nos: String = "1",
    val deduction: String = "",
    val floor: String = "",
    val location: String = "",
    val remarks: String = "",
    val photoUri: String? = null,
    val consecutiveSavedCount: Int = 0,
    val lastSavedSummary: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SiteViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    val repository = SiteRepository(database)
    private val localPortalServer = LocalPortalServer(application)
    val localPortalState = localPortalServer.state

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedHomeTab = MutableStateFlow(HomeTab.PROJECTS)
    val selectedHomeTab: StateFlow<HomeTab> = _selectedHomeTab.asStateFlow()

    private val _selectedDirectorySection = MutableStateFlow(DirectorySection.CLIENTS)
    val selectedDirectorySection: StateFlow<DirectorySection> = _selectedDirectorySection.asStateFlow()

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
    val rateBooks: StateFlow<List<ContractorRateBookEntity>> = repository.allRateBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val companyProfile: StateFlow<CompanyProfileEntity?> = repository.companyProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    val projectDrawings: StateFlow<List<ProjectDrawingEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectDrawings) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drawingRevisions: StateFlow<List<DrawingRevisionEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getDrawingRevisions) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val drawingTransmittals: StateFlow<List<DrawingTransmittalEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getDrawingTransmittals) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectRateBookAssignments: StateFlow<List<ProjectRateBookAssignmentEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectRateBookAssignments) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectConsultancyProfile: StateFlow<ProjectConsultancyProfileEntity?> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectConsultancyProfile) ?: flowOf(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val projectScopeItems: StateFlow<List<ProjectScopeItemEntity>> = selectedProjectId
        .flatMapLatest { it?.let(repository::getProjectScopeItems) ?: flowOf(emptyList()) }
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

    private val _formState = MutableStateFlow(QuickEntryFormState())
    val formState: StateFlow<QuickEntryFormState> = _formState.asStateFlow()

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
        // Ensure predefined data is seeded
        viewModelScope.launch(Dispatchers.IO) {
            database.seedPredefinedData()
        }

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
        // PIN-authenticated, read-only session on the current Wi-Fi network.
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

    fun startLocalPortal() {
        localPortalServer.start {
            val measurementCounts = measurements.value.groupingBy { it.projectId }.eachCount()
            val selectedId = selectedProjectId.value
            PortalSnapshot(
                companyName = companyProfile.value?.practiceName?.ifBlank { "ArchiMan" } ?: "ArchiMan",
                projects = projects.value.map { project ->
                    PortalProject(
                        name = project.name,
                        code = project.projectCode,
                        type = project.projectType,
                        status = project.status,
                        client = project.client,
                        location = project.siteLocation,
                        architect = project.architectInCharge,
                        measurementCount = measurementCounts[project.id] ?: 0
                    )
                },
                selectedProject = projects.value.firstOrNull { it.id == selectedId }?.name,
                tasks = projectTasks.value.map { "${it.title} · ${it.status}" },
                schedules = projectSchedules.value.map { "${it.title} · ${it.status}" },
                inspections = siteInspections.value.map { "${it.location} · ${it.observation} · ${it.status}" },
                drawings = projectDrawings.value.map { "${it.drawingNumber} · ${it.title} · ${it.status}" }
            )
        }
    }

    fun stopLocalPortal() = localPortalServer.stop()

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
        isAsBuilt: Boolean
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
        isAsBuilt: Boolean
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
                    issuedAt = if (issueStatus == "WIP") null else System.currentTimeMillis()
                )
            )
            repository.updateProjectDrawing(drawing.copy(status = issueStatus, updatedAt = System.currentTimeMillis()))
        }
    }

    fun createRateBook(contractorId: Long, name: String, version: Int = 1, onComplete: (Long) -> Unit = {}) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.createRateBook(ContractorRateBookEntity(contractorId = contractorId, name = name.trim(), version = version.coerceAtLeast(1)))
            withContext(Dispatchers.Main) { onComplete(id) }
        }
    }

    fun setRateBookItem(rateBookId: Long, item: ItemMasterEntity, rate: Double) {
        if (rate < 0.0) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertRateBookItem(ContractorRateBookItemEntity(
                rateBookId = rateBookId,
                itemId = item.id,
                itemNameSnapshot = item.name,
                uomSnapshot = item.unit,
                specificationSnapshot = item.specification,
                sourceItemCodeSnapshot = item.sourceItemCode,
                rate = rate
            ))
        }
    }

    fun deleteRateBookItem(item: ContractorRateBookItemEntity) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteRateBookItem(item) }
    }

    fun assignRateBook(projectId: Long, contractorId: Long, rateBookId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.assignRateBook(ProjectRateBookAssignmentEntity(projectId = projectId, contractorId = contractorId, rateBookId = rateBookId))
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

        // Prefill form dimensions from component if available
        val comp = components.value.firstOrNull { it.id == _selectedComponentId.value }
        if (comp != null) {
            _formState.update { current ->
                current.copy(
                    description = comp.name,
                    length = if (comp.length > 0) comp.length.toString() else current.length,
                    width = if (comp.width > 0) comp.width.toString() else current.width,
                    height = if (comp.height > 0) comp.height.toString() else current.height,
                    nos = "1",
                    deduction = "0"
                )
            }
        }
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
        val flr = floors.value.firstOrNull { it.id == floorId }
        _formState.update { current ->
            current.copy(
                floor = flr?.name ?: "Floor Level",
                location = "${flr?.name ?: "Floor"} - Structural (Beams/Slabs)"
            )
        }
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

    // Quick Entry / Form state
    fun updateForm(
        description: String? = null,
        length: String? = null,
        width: String? = null,
        height: String? = null,
        nos: String? = null,
        deduction: String? = null,
        floor: String? = null,
        location: String? = null,
        remarks: String? = null,
        photoUri: String? = null
    ) {
        _formState.update { current ->
            current.copy(
                description = description ?: current.description,
                length = length ?: current.length,
                width = width ?: current.width,
                height = height ?: current.height,
                nos = nos ?: current.nos,
                deduction = deduction ?: current.deduction,
                floor = floor ?: current.floor,
                location = location ?: current.location,
                remarks = remarks ?: current.remarks,
                photoUri = if (photoUri != null && photoUri == "CLEAR") null else (photoUri ?: current.photoUri)
            )
        }
    }

    fun updateFormDescription(value: String) = updateForm(description = value)
    fun updateFormNos(value: String) = updateForm(nos = value)
    fun updateFormLength(value: String) = updateForm(length = value)
    fun updateFormWidth(value: String) = updateForm(width = value)
    fun updateFormHeight(value: String) = updateForm(height = value)
    fun updateFormDeduction(value: String) = updateForm(deduction = value)

    fun repeatFormValues() {
        // Keeps dimension values while letting the user change the description.
        _formState.update { current ->
            current.copy(
                description = if (current.description.isNotBlank()) "${current.description} (Copy)" else ""
            )
        }
    }

    fun computeQuantity(calcType: CalculationType, state: QuickEntryFormState): Double {
        val l = state.length.toDoubleOrNull() ?: 0.0
        val w = state.width.toDoubleOrNull() ?: 0.0
        val h = state.height.toDoubleOrNull() ?: 0.0
        val n = state.nos.toDoubleOrNull() ?: 1.0
        val d = state.deduction.toDoubleOrNull() ?: 0.0

        return QuantityCalculator.calculate(
            calcType,
            MeasurementInput(no = n, length = l, breadth = w, height = h, deduction = d)
        )
    }

    fun computeQuantity(item: ItemMasterEntity?, state: QuickEntryFormState): Double {
        val type = item?.calculationType ?: CalculationType.WALL_PLASTER
        return computeQuantity(type, state)
    }

    fun saveMeasurement(andNext: Boolean = false, onSaved: () -> Unit = {}) {
        saveMeasurementRow {
            onSaved()
        }
    }

    // Save Row in Work Item Measurement or Quick Entry
    fun saveMeasurementRow(
        onSaved: (MeasurementEntity) -> Unit = {}
    ) {
        val projId = _selectedProjectId.value ?: return
        val contId = _selectedContractorId.value ?: return
        val itemId = _selectedItemId.value ?: return

        val proj = projects.value.firstOrNull { it.id == projId } ?: return
        val cont = contractors.value.firstOrNull { it.id == contId } ?: return
        val item = items.value.firstOrNull { it.id == itemId } ?: return

        val currentFloor = floors.value.firstOrNull { it.id == _selectedFloorId.value }
        val currentRoom = rooms.value.firstOrNull { it.id == _selectedRoomId.value }
        val currentComp = components.value.firstOrNull { it.id == _selectedComponentId.value }

        val form = _formState.value
        val qty = computeQuantity(item.calculationType, form)

        val floorName = form.floor.ifBlank { currentFloor?.name ?: "" }
        val locationName = form.location.ifBlank {
            listOfNotNull(currentRoom?.name, currentComp?.name).joinToString(" - ")
        }
        val desc = form.description.ifBlank { currentComp?.name ?: item.name }

        val measurement = MeasurementEntity(
            projectId = projId,
            floorId = _selectedFloorId.value,
            roomId = _selectedRoomId.value,
            componentId = _selectedComponentId.value,
            componentWorkItemId = _selectedComponentWorkItem.value?.id,
            contractorId = contId,
            contractorName = cont.name,
            itemId = itemId,
            itemName = item.name,
            unit = item.unit,
            calculationType = item.calculationType,
            description = desc,
            length = form.length.toDoubleOrNull() ?: 0.0,
            width = form.width.toDoubleOrNull() ?: 0.0,
            height = form.height.toDoubleOrNull() ?: 0.0,
            nos = form.nos.toDoubleOrNull() ?: 1.0,
            deduction = form.deduction.toDoubleOrNull() ?: 0.0,
            quantity = qty,
            floor = floorName,
            location = locationName,
            remarks = form.remarks,
            photoUri = form.photoUri,
            date = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            val insertedId = repository.insertMeasurement(measurement)
            val savedEntity = measurement.copy(id = insertedId)

            val summary = "$desc: $qty ${item.unit}"

            _formState.update { current ->
                current.copy(
                    description = "",
                    length = "",
                    width = "",
                    height = "",
                    nos = "1",
                    deduction = "",
                    photoUri = null,
                    consecutiveSavedCount = current.consecutiveSavedCount + 1,
                    lastSavedSummary = summary
                )
            }

            onSaved(savedEntity)
        }
    }

    // Save and duplicate entry across multiple selected floors
    fun saveMeasurementWithFloorDuplication(
        targetFloorIds: List<Long>,
        onComplete: (Int) -> Unit = {}
    ) {
        val projId = _selectedProjectId.value ?: return
        val contId = _selectedContractorId.value ?: return
        val itemId = _selectedItemId.value ?: return

        val proj = projects.value.firstOrNull { it.id == projId } ?: return
        val cont = contractors.value.firstOrNull { it.id == contId } ?: return
        val item = items.value.firstOrNull { it.id == itemId } ?: return

        val currentFloor = floors.value.firstOrNull { it.id == _selectedFloorId.value }
        val currentRoom = rooms.value.firstOrNull { it.id == _selectedRoomId.value }
        val currentComp = components.value.firstOrNull { it.id == _selectedComponentId.value }

        val form = _formState.value
        val qty = computeQuantity(item.calculationType, form)

        val desc = form.description.ifBlank { currentComp?.name ?: item.name }
        val locationName = form.location.ifBlank {
            listOfNotNull(currentRoom?.name, currentComp?.name).joinToString(" - ")
        }

        val allFloorList = floors.value

        viewModelScope.launch(Dispatchers.IO) {
            var count = 0
            // 1. Primary selected floor
            val baseFloorName = form.floor.ifBlank { currentFloor?.name ?: "" }
            val baseMeasurement = MeasurementEntity(
                projectId = projId,
                floorId = _selectedFloorId.value,
                roomId = _selectedRoomId.value,
                componentId = _selectedComponentId.value,
                componentWorkItemId = _selectedComponentWorkItem.value?.id,
                contractorId = contId,
                contractorName = cont.name,
                itemId = itemId,
                itemName = item.name,
                unit = item.unit,
                calculationType = item.calculationType,
                description = desc,
                length = form.length.toDoubleOrNull() ?: 0.0,
                width = form.width.toDoubleOrNull() ?: 0.0,
                height = form.height.toDoubleOrNull() ?: 0.0,
                nos = form.nos.toDoubleOrNull() ?: 1.0,
                deduction = form.deduction.toDoubleOrNull() ?: 0.0,
                quantity = qty,
                floor = baseFloorName,
                location = locationName,
                remarks = form.remarks,
                photoUri = form.photoUri,
                date = System.currentTimeMillis()
            )
            repository.insertMeasurement(baseMeasurement)
            count++

            // 2. Additional target floors
            for (fId in targetFloorIds) {
                if (fId == _selectedFloorId.value) continue
                val targetFloor = allFloorList.firstOrNull { it.id == fId } ?: continue
                val dupMeasurement = baseMeasurement.copy(
                    id = 0L,
                    floorId = fId,
                    floor = targetFloor.name,
                    roomId = null,
                    componentId = null,
                    componentWorkItemId = null,
                    date = System.currentTimeMillis()
                )
                repository.insertMeasurement(dupMeasurement)
                count++
            }

            val summary = "$desc: $qty ${item.unit} duplicated across $count floor(s)"
            _formState.update { current ->
                current.copy(
                    description = "",
                    length = "",
                    width = "",
                    height = "",
                    nos = "1",
                    deduction = "",
                    photoUri = null,
                    consecutiveSavedCount = current.consecutiveSavedCount + count,
                    lastSavedSummary = summary
                )
            }
            onComplete(count)
        }
    }

    // Continue Navigation Loops
    fun continueNextItem() {
        val currentWorkList = componentWorkItems.value
        val currentIndex = currentWorkList.indexOfFirst { it.id == _selectedComponentWorkItem.value?.id }
        if (currentIndex >= 0 && currentIndex < currentWorkList.size - 1) {
            val nextItem = currentWorkList[currentIndex + 1]
            selectComponentWorkItem(nextItem)
        }
    }

    fun continueNextComponent() {
        val currentCompList = components.value
        val currentIndex = currentCompList.indexOfFirst { it.id == _selectedComponentId.value }
        if (currentIndex >= 0 && currentIndex < currentCompList.size - 1) {
            val nextComp = currentCompList[currentIndex + 1]
            selectComponent(nextComp.id)
            _currentScreen.value = AppScreen.ROOM_WORKSPACE
        }
    }

    fun continueNextRoom() {
        val currentRoomList = rooms.value
        val currentIndex = currentRoomList.indexOfFirst { it.id == _selectedRoomId.value }
        if (currentIndex >= 0 && currentIndex < currentRoomList.size - 1) {
            val nextRoom = currentRoomList[currentIndex + 1]
            selectRoom(nextRoom.id)
            _currentScreen.value = AppScreen.ROOM_WORKSPACE
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

    // Client Management
    fun addClient(name: String, address: String, contactNo: String, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertClient(
                ClientEntity(
                    name = name.trim(),
                    address = address.trim(),
                    contactNo = contactNo.trim()
                )
            )
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

    // Projects with Client & Contractors Multi-Select
    fun addProjectWithClientAndContractors(
        name: String,
        clientName: String,
        clientId: Long,
        siteLocation: String,
        floorNames: List<String>,
        contractorIds: List<Long>,
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val projId = repository.insertProject(
                ProjectEntity(
                    name = name.trim(),
                    client = clientName.trim(),
                    clientId = clientId,
                    siteLocation = siteLocation.trim()
                )
            )
            _selectedProjectId.value = projId

            // Insert Floor Levels
            var firstFloorId: Long? = null
            floorNames.filter { it.isNotBlank() }.forEachIndexed { idx, fName ->
                val fId = repository.insertFloor(FloorEntity(projectId = projId, name = fName.trim(), orderIndex = idx))
                if (firstFloorId == null) firstFloorId = fId
            }
            if (firstFloorId == null) {
                firstFloorId = repository.insertFloor(FloorEntity(projectId = projId, name = "Ground Floor", orderIndex = 0))
            }
            _selectedFloorId.value = firstFloorId

            // Link selected contractors to this project
            if (contractorIds.isNotEmpty()) {
                val refs = contractorIds.map { cId ->
                    ProjectContractorCrossRef(projectId = projId, contractorId = cId)
                }
                repository.insertProjectContractorRefs(refs)
            }
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
