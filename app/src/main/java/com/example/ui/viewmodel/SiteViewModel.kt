package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.SiteRepository
import com.example.server.LocalLanServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreen(val title: String) {
    HOME("Home"),
    PROJECT_WORKSPACE("Project Workspace"),
    DEDICATED_MEASUREMENT("Record Measurement"),
    ROOM_WORKSPACE("Room Components"),
    WORK_ITEM_MEASURE("Work Item Measurement"),
    MEASUREMENT_BOOK("Measurement Book"),
    QUICK_ENTRY("Quick Measurement"),
    REGISTER("Measurements Register"),
    PROJECTS("Projects"),
    CLIENTS("Clients"),
    CONTRACTORS("Contractors"),
    BILLS("Bills & Invoicing"),
    REPORTS("Reports & Summary"),
    LAN_SERVER("Local LAN Web Server"),
    MASTER_DATA("Items & Master Library"),
    EXPORT("Export & Quantities")
}

enum class HomeTab(val label: String) {
    PROJECTS("Projects"),
    CLIENTS("Clients"),
    CONTRACTORS("Contractors")
}

data class QuickEntryFormState(
    val description: String = "",
    val length: String = "",
    val width: String = "",
    val height: String = "",
    val nos: String = "1",
    val deduction: String = "",
    val rate: String = "",
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
    val lanServer = LocalLanServer(application, repository)

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedHomeTab = MutableStateFlow(HomeTab.PROJECTS)
    val selectedHomeTab: StateFlow<HomeTab> = _selectedHomeTab.asStateFlow()

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

    val bills: StateFlow<List<BillEntity>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // LAN Server State
    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

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

        // Start LAN server on startup
        startLanServer()
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
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
        updateRateForCurrentSelection()

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
        updateRateForCurrentSelection()
    }

    fun selectItem(itemId: Long) {
        _selectedItemId.value = itemId
        updateRateForCurrentSelection()
    }

    private fun updateRateForCurrentSelection() {
        val contractorId = _selectedContractorId.value
        val itemId = _selectedItemId.value
        if (contractorId != null && itemId != null) {
            viewModelScope.launch {
                val rate = repository.getRateForContractorAndItem(contractorId, itemId) ?: 0.0
                _formState.update { it.copy(rate = if (rate > 0) rate.toString() else "") }
            }
        } else if (itemId != null) {
            val item = items.value.firstOrNull { it.id == itemId }
            val rate = item?.defaultRate ?: 0.0
            _formState.update { it.copy(rate = if (rate > 0) rate.toString() else "") }
        }
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
        rate: String? = null,
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
                rate = rate ?: current.rate,
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
        // Keeps length, width, height, nos, rate but lets user change description
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

        val q = when (calcType) {
            CalculationType.RUNNING_LENGTH -> l * n
            CalculationType.AREA -> l * w * n
            CalculationType.WALL_PLASTER -> (l * h * n) - d
            CalculationType.VOLUME -> l * w * h * n
            CalculationType.NOS -> n
        }
        return Math.round(q * 100.0) / 100.0
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

    fun computeAmount(qty: Double, rateStr: String): Double {
        val r = rateStr.toDoubleOrNull() ?: 0.0
        return Math.round(qty * r * 100.0) / 100.0
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
        val rate = form.rate.toDoubleOrNull() ?: item.defaultRate
        val amount = computeAmount(qty, rate.toString())

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
            rate = rate,
            amount = amount,
            floor = floorName,
            location = locationName,
            remarks = form.remarks,
            photoUri = form.photoUri,
            date = System.currentTimeMillis()
        )

        viewModelScope.launch(Dispatchers.IO) {
            val insertedId = repository.insertMeasurement(measurement)
            val savedEntity = measurement.copy(id = insertedId)

            val summary = "$desc: ${qty} ${item.unit} @ ₹$rate = ₹${amount.toInt()}"

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
        val rate = form.rate.toDoubleOrNull() ?: item.defaultRate
        val amount = computeAmount(qty, rate.toString())

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
                rate = rate,
                amount = amount,
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

            val summary = "$desc: ${qty} ${item.unit} duplicated across $count floor(s) (Total ₹${(amount * count).toInt()})"
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
            repository.updateMeasurement(measurement)
            _editingMeasurement.value = null
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
        qualifiedItemsList: List<Triple<String, String, Double>>, // itemName, uom, rate
        onComplete: (Long) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val contractorId = repository.insertContractor(
                ContractorEntity(
                    name = name.trim(),
                    address = address.trim(),
                    contactNo = contactNo.trim(),
                    phone = contactNo.trim()
                )
            )
            if (qualifiedItemsList.isNotEmpty()) {
                val entities = qualifiedItemsList.map { (iName, uom, rate) ->
                    val calcType = when (uom.lowercase().trim()) {
                        "m³", "cu.m", "cum" -> CalculationType.VOLUME
                        "m", "rmt", "r.m." -> CalculationType.RUNNING_LENGTH
                        "nos", "no", "unit" -> CalculationType.NOS
                        else -> if (iName.contains("plaster", ignoreCase = true)) CalculationType.WALL_PLASTER else CalculationType.AREA
                    }
                    ContractorQualifiedItemEntity(
                        contractorId = contractorId,
                        itemName = iName.trim(),
                        uom = uom.trim(),
                        calculationType = calcType,
                        rate = rate
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
        uom: String,
        rate: Double = 0.0
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
                    itemName = itemName.trim(),
                    uom = uom.trim(),
                    calculationType = calcType,
                    rate = rate
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
            var count = 0
            for (m in measurementsList) {
                if (m.description.isNotBlank() || m.length > 0 || m.height > 0 || m.quantity > 0) {
                    repository.insertMeasurement(m)
                    count++
                }
            }
            onComplete(count)
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

    fun addItem(name: String, unit: String, calcType: CalculationType, defaultRate: Double = 0.0) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertItem(
                ItemMasterEntity(
                    name = name,
                    unit = unit,
                    calculationType = calcType,
                    defaultRate = defaultRate,
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

    fun saveContractorRate(contractorId: Long, itemId: Long, rate: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveContractorRate(contractorId, itemId, rate)
        }
    }

    // Billing
    fun generateBill(
        projectId: Long,
        contractorId: Long,
        retentionPercent: Double = 0.0,
        notes: String = "",
        onComplete: (BillEntity) -> Unit = {}
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val unbilled = repository.getUnbilledMeasurements(projectId, contractorId)
            if (unbilled.isEmpty()) return@launch

            val proj = projects.value.firstOrNull { it.id == projectId }
            val cont = contractors.value.firstOrNull { it.id == contractorId }

            val totalQty = unbilled.sumOf { it.quantity }
            val totalAmt = unbilled.sumOf { it.amount }
            val retentionAmt = totalAmt * (retentionPercent / 100.0)
            val netAmt = totalAmt - retentionAmt

            val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            val billNo = "BILL-${sdf.format(Date())}-${(100..999).random()}"

            val bill = BillEntity(
                billNumber = billNo,
                projectId = projectId,
                projectName = proj?.name ?: "Site Project",
                contractorId = contractorId,
                contractorName = cont?.name ?: "Contractor",
                date = System.currentTimeMillis(),
                totalQuantity = totalQty,
                totalAmount = totalAmt,
                retentionPercent = retentionPercent,
                netAmount = netAmt,
                notes = notes
            )

            val billId = repository.createBill(bill, unbilled.map { it.id })
            onComplete(bill.copy(id = billId))
        }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBill(bill)
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
        floorName: String
    ) {
        _dedicatedContractorId.value = contractorId
        _dedicatedItemName.value = itemName
        _dedicatedItemUom.value = uom
        _dedicatedCalculationType.value = calcType
        _dedicatedFloorId.value = floorId
        _dedicatedFloorName.value = floorName
    }

    // Filter controls
    fun setFilterProject(id: Long?) { _filterProjectId.value = id }
    fun setFilterContractor(id: Long?) { _filterContractorId.value = id }
    fun setFilterSearchQuery(query: String) { _filterSearchQuery.value = query }

    // LAN Server
    fun startLanServer() {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = lanServer.start()
            _isServerRunning.value = ok
            if (ok) {
                _serverUrl.value = lanServer.getServerUrl()
            }
        }
    }

    fun stopLanServer() {
        lanServer.stop()
        _isServerRunning.value = false
        _serverUrl.value = ""
    }

    override fun onCleared() {
        super.onCleared()
        lanServer.stop()
    }
}
