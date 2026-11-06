@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.domain.*
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ArchiCard
import com.holagundi.archiman.ui.theme.CarbonIcons
import java.util.Locale

@Composable
fun <T> MeasurementSelect(label: String, value: String, choices: List<Pair<T, String>>, enabled: Boolean = true, choose: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Box {
            OutlinedButton(enabled = enabled, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(value.ifBlank { "Select $label" }, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(8.dp))
                Icon(CarbonIcons.ChevronDown, null, Modifier.size(20.dp))
            }
            if (expanded && choices.size > 8) MeasurementChoiceDialog(label, choices, { expanded = false }) { key -> choose(key); expanded = false }
            DropdownMenu(expanded && choices.size <= 8, { expanded = false }, modifier = Modifier.heightIn(max = 320.dp)) {
                if (choices.isEmpty()) Text("No available ${label.lowercase()} options", Modifier.padding(16.dp))
                choices.forEach { (key, name) -> DropdownMenuItem(text = { Text(name) }, onClick = { choose(key); expanded = false }) }
            }
        }
    }
}

@Composable
fun RecordMeasurementScreen(onDone: () -> Unit, onSetup: () -> Unit = {}, onCancel: () -> Unit = onDone, workspace: ProjectWorkspaceViewModel = hiltViewModel(), record: RecordMeasurementViewModel = hiltViewModel()) {
    MeasurementInputBoundary { RecordMeasurementContent(onDone, onSetup, onCancel, workspace, record) }
}

@Composable
private fun RecordMeasurementContent(onDone: () -> Unit, onSetup: () -> Unit, onCancel: () -> Unit, workspace: ProjectWorkspaceViewModel, record: RecordMeasurementViewModel) {
    val project by workspace.project.collectAsStateWithLifecycle()
    val capture by workspace.captureContext.collectAsStateWithLifecycle()
    if (capture == null) { Text("Loading contractor, floor and revision choices…"); return }
    val floors = capture!!.floors
    val rooms by workspace.rooms.collectAsStateWithLifecycle()
    val contractors = capture!!.contractors
    val schedules = capture!!.schedules
    val error by record.error.collectAsStateWithLifecycle()
    val busy by record.busy.collectAsStateWithLifecycle()
    val saved by record.saved.collectAsStateWithLifecycle()
    val selectedEntry by record.selected.collectAsStateWithLifecycle()
    val editing by record.editing.collectAsStateWithLifecycle()
    val measurements by workspace.measurements.collectAsStateWithLifecycle()
    val sheets by workspace.sheets.collectAsStateWithLifecycle()
    var initialized by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!initialized) { record.beginEntry(); initialized = true } }
    var contractorId by rememberSaveable { mutableStateOf(-1L) }; var floorId by rememberSaveable { mutableStateOf(-1L) }
    var contractorPicker by rememberSaveable { mutableStateOf(true) }
    var roomId by rememberSaveable { mutableStateOf(0L) }; var itemName by rememberSaveable { mutableStateOf("") }
    var lineKey by rememberSaveable { mutableStateOf("") }
    var calcType by rememberSaveable { mutableStateOf(CalculationTypes.VOLUME) }
    var system by rememberSaveable { mutableStateOf(MeasurementUnitSystem.METRIC) }
    var nos by rememberSaveable { mutableStateOf("1") }; var length by rememberSaveable { mutableStateOf("") }
    var breadth by rememberSaveable { mutableStateOf("") }; var height by rememberSaveable { mutableStateOf("") }
    var deduction by rememberSaveable { mutableStateOf("") }; var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }; var remarks by rememberSaveable { mutableStateOf("") }
    var descriptionEdited by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var more by remember { mutableStateOf(false) }
    var activeField by rememberSaveable { mutableStateOf("description") }
    val softwareKeyboard = LocalSoftwareKeyboardController.current
    val selection = remember { mutableStateMapOf<String, TextRange>() }
    val focusRequests = remember { listOf("description", "nos", "length", "breadth", "height", "deduction", "location", "remarks").associateWith { FocusRequester() } }
    var pendingLeave by remember { mutableStateOf<(() -> Unit)?>(null) }
    var editBaseline by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val feedback = remember { SnackbarHostState() }
    LaunchedEffect(saved, error) {
        val message = error ?: saved
        if (!message.isNullOrBlank()) feedback.showSnackbar(message, withDismissAction = true, duration = if (error != null) SnackbarDuration.Long else SnackbarDuration.Short)
    }
    fun hasDimensions() = listOf(length, breadth, height, deduction).any { it.isNotBlank() } || nos != "1"
    fun editorValues() = listOf(nos, length, breadth, height, deduction, description, location, remarks)
    fun dirty() = if (selectedEntry == null) hasDimensions() || descriptionEdited else editing && editorValues() != editBaseline
    fun requestLeave(leave: () -> Unit) {
        if (busy) return
        if (dirty()) pendingLeave = leave else leave()
    }
    BackHandler { requestLeave(onCancel) }
    var pendingContext by remember { mutableStateOf<(() -> Unit)?>(null) }
    fun changeContext(change: () -> Unit) {
        if (hasDimensions()) pendingContext = change else change()
    }
    fun clearDimensions() { nos = "1"; length = ""; breadth = ""; height = ""; deduction = "" }

    val unit = MeasurementUnitConverter.unitFor(calcType, system)
    LaunchedEffect(contractors) { if (contractors.none { it.id == contractorId }) contractorId = -1 }
    LaunchedEffect(floors) { if (floors.none { it.id == floorId }) floorId = floors.firstOrNull()?.id ?: -1 }
    val active = schedules.firstOrNull { it.status == "ACTIVE" }
    val assigned = active?.lines.orEmpty().filter { it.contractorId == contractorId }
    val selectedLine = assigned.firstOrNull { it.key == lineKey }
    val entries = remember(measurements, contractorId) { measurements.filter { it.contractorId == contractorId }.sortedWith(compareBy({ it.date }, { it.id })) }
    val entryIndex = remember(entries, selectedEntry?.id) { entries.indexOfFirst { it.id == selectedEntry?.id } }
    val sheetById = remember(sheets) { sheets.associateBy { it.id } }
    val selectedSheet = sheetById[selectedEntry?.text("sheetId")?.toLongOrNull()]
    val canEditSelected = selectedEntry != null && selectedSheet?.status in listOf("DRAFT", "RETURNED") && selectedSheet?.archivedAt == null
    LaunchedEffect(assigned) {
        if (selectedLine == null && selectedEntry == null) { lineKey = ""; itemName = ""; if (!descriptionEdited) description = "" }
    }
    val targetSystem = selectedEntry?.let { MeasurementUnitConverter.infer(it.text("unit")) } ?: selectedLine?.let { MeasurementUnitConverter.infer(it.unit) } ?: system
    fun dimensionValue(text: String, power: Int = 1) = MeasurementUnitConverter.convert(text.toDoubleOrNull() ?: 0.0, power, system, targetSystem)

    val values = listOf(nos, length, breadth, height, deduction)
    val validNumbers = values.filter(String::isNotBlank).all { it.toDoubleOrNull()?.let { n -> n.isFinite() && n >= 0 } == true }
    val quantity = QuantityCalculator.calculate(calcType, MeasurementInput(nos.toDoubleOrNull() ?: 0.0, length.toDoubleOrNull() ?: 0.0, breadth.toDoubleOrNull() ?: 0.0, height.toDoubleOrNull() ?: 0.0, deduction.toDoubleOrNull() ?: 0.0))
    val dimension = if (system == MeasurementUnitSystem.METRIC) "m" else "ft"
    val canSave = !busy && editing && validNumbers && quantity > 0 && (if (selectedEntry != null) canEditSelected else selectedLine != null && contractorId > 0 && floorId > 0 && itemName.isNotBlank())
    fun fieldValue(id: String) = when (id) {
        "description" -> description; "nos" -> nos; "length" -> length; "breadth" -> breadth; "height" -> height; "deduction" -> deduction; "location" -> location; else -> remarks
    }
    fun updateField(id: String, value: String) { when (id) {
        "description" -> if (description != value) { description = value; descriptionEdited = true }
        "nos" -> nos = value; "length" -> length = value; "breadth" -> breadth = value; "height" -> height = value; "deduction" -> deduction = value; "location" -> location = value; "remarks" -> remarks = value
    } }
    fun numericField(id: String) = id in setOf("nos", "length", "breadth", "height", "deduction")
    val fieldLabels = mapOf("description" to "Description", "nos" to "Nos.", "length" to "Length ($dimension)", "breadth" to "Breadth ($dimension)", "height" to "Height ($dimension)", "deduction" to "Opening deduction ($unit)", "location" to "Specific location", "remarks" to "Remarks")
    val fieldOrder = buildList {
        add("description"); add("nos")
        if (calcType != CalculationTypes.NOS) add("length")
        if (calcType in listOf(CalculationTypes.AREA, CalculationTypes.VOLUME)) add("breadth")
        if (calcType in listOf(CalculationTypes.WALL_PLASTER, CalculationTypes.VOLUME)) add("height")
        if (calcType == CalculationTypes.WALL_PLASTER) add("deduction")
        if (details) { add("location"); add("remarks") }
    }
    fun focusField(id: String) { activeField = id; focusRequests[id]?.requestFocus() }
    fun nextField(offset: Int) {
        val index = fieldOrder.indexOf(activeField).coerceAtLeast(0)
        focusField(fieldOrder[(index + offset + fieldOrder.size) % fieldOrder.size])
    }
    fun keyInput(input: String?, backspace: Boolean = false) {
        if (busy || !editing) return
        val text = fieldValue(activeField)
        val cursor = selection[activeField] ?: TextRange(text.length)
        val edited = if (backspace) MeasurementTextEditing.backspace(text, cursor.start, cursor.end) else MeasurementTextEditing.insert(text, cursor.start, cursor.end, input.orEmpty(), numericField(activeField))
        updateField(activeField, edited.text); selection[activeField] = TextRange(edited.cursor)
        focusRequests[activeField]?.requestFocus()
    }
    @Composable fun EntryField(id: String, modifier: Modifier = Modifier) {
        val value = fieldValue(id)
        val cursor = selection[id] ?: TextRange(value.length)
        Column(modifier) {
            Text(fieldLabels.getValue(id), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextField(
                value = TextFieldValue(value, TextRange(cursor.start.coerceIn(0, value.length), cursor.end.coerceIn(0, value.length))),
                onValueChange = { entered ->
                    val text = if (numericField(id)) entered.text.replace(',', '.') else entered.text
                    if (text.length <= (if (numericField(id)) 40 else 8000) && (!numericField(id) || text.matches(Regex("[0-9]*(\\.[0-9]*)?")))) {
                        updateField(id, text); selection[id] = entered.selection
                    }
                },
                enabled = !busy,
                readOnly = !editing,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequests.getValue(id)).onFocusChanged { state ->
                    if (state.isFocused) { activeField = id; softwareKeyboard?.hide() }
                }.semantics { contentDescription = fieldLabels.getValue(id) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = if (numericField(id)) KeyboardType.Decimal else KeyboardType.Text, imeAction = ImeAction.Next, showKeyboardOnFocus = false),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { nextField(1) }),
                shape = androidx.compose.ui.graphics.RectangleShape,
                colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            )
        }
    }
    LaunchedEffect(contractorPicker, details, activeField, selectedLine?.key, editing) {
        softwareKeyboard?.hide()
        if (!contractorPicker && editing && (selectedLine != null || selectedEntry != null) && activeField in fieldOrder) {
            withFrameNanos { }; focusRequests.getValue(activeField).requestFocus()
        }
    }
    fun selectItem(key: String) {
        if (selectedEntry != null) return
        val line = assigned.firstOrNull { it.key == key } ?: return
        if (line.key != lineKey) changeContext {
            lineKey = line.key; itemName = line.description; calcType = line.formula
            if (!descriptionEdited) description = line.description
            clearDimensions(); activeField = "description"
        }
    }
    fun changeUnits(target: MeasurementUnitSystem) {
        if (target == system) return
        val wasDirty = dirty()
        fun convert(text: String, power: Int): String = text.toDoubleOrNull()?.let { String.format(Locale.US, "%.6f", MeasurementUnitConverter.convert(it, power, system, target)).trimEnd('0').trimEnd('.') } ?: text
        length = convert(length, 1); breadth = convert(breadth, 1); height = convert(height, 1); deduction = convert(deduction, 2); system = target
        if (selectedEntry != null && !wasDirty) editBaseline = editorValues()
    }
    fun loadEntry(value: com.holagundi.archiman.data.RecordSnapshot) {
        fun number(key: String) = value.text(key).toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: value.text(key)
        contractorId = value.text("contractorId").toLong(); floorId = value.text("floorId").toLongOrNull() ?: -1L; roomId = value.text("roomId").toLongOrNull() ?: 0L
        itemName = value.text("itemName"); lineKey = value.text("boqLineKey"); calcType = value.text("calculationType"); system = MeasurementUnitConverter.infer(value.text("unit"))
        nos = number("nos"); length = number("length"); breadth = number("width"); height = number("height"); deduction = number("deduction")
        description = value.text("description"); location = value.text("location"); remarks = value.text("remarks"); descriptionEdited = false
        editBaseline = editorValues(); selection.clear(); activeField = "description"; contractorPicker = false
    }
    fun navigateEntry(index: Int) {
        val entry = entries.getOrNull(index) ?: return
        requestLeave { record.selectEntry(entry.id, ::loadEntry) }
    }
    fun newEntry(pickContractor: Boolean = false) {
        requestLeave {
            record.beginEntry(); clearDimensions(); description = selectedLine?.description.orEmpty(); descriptionEdited = false; location = ""; remarks = ""; editBaseline = emptyList(); activeField = "description"
            if (selectedLine == null || pickContractor) { lineKey = ""; itemName = ""; description = "" }
            if (pickContractor) contractorPicker = true
        }
    }
    fun saveEntry(addAnother: Boolean) {
            if (selectedEntry != null) {
                record.saveSelected(mapOf("description" to description, "location" to location, "nos" to nos, "length" to dimensionValue(length).toString(), "width" to dimensionValue(breadth).toString(), "height" to dimensionValue(height).toString(), "deduction" to dimensionValue(deduction, 2).toString(), "remarks" to remarks)) { committed -> loadEntry(committed); if (!addAnother) onDone() }
                return
            }
            val submitted = listOf(contractorId, floorId, roomId, lineKey, system, nos, length, breadth, height, deduction, description, location, remarks)
            val current = project ?: return
            val contractor = contractors.firstOrNull { it.id == contractorId } ?: return
            val floor = floors.firstOrNull { it.id == floorId } ?: return
            val room = rooms.firstOrNull { it.id == roomId }
            record.clearError()
                record.save(
                    MeasurementDraft(
                        projectId = current.id,
                        projectCode = current.projectCode,
                        floorId = floor.id,
                        floorName = floor.name,
                        roomId = room?.id,
                        roomName = room?.name,
                        contractorId = contractor.id,
                        contractorName = contractor.name,
                        itemId = selectedLine!!.itemId,
                        boqRevision = active!!.label, boqLineKey = selectedLine.key, rateSnapshot = selectedLine.rate.toDouble(), contractBasis = selectedLine.basis,
                        itemName = itemName,
                        unit = selectedLine!!.unit,
                        calculationType = calcType,
                        description = description,
                        location = location,
                        remarks = remarks,
                        length = dimensionValue(length),
                        width = dimensionValue(breadth),
                        height = dimensionValue(height),
                        nos = nos.toDoubleOrNull() ?: 0.0,
                        deduction = dimensionValue(deduction, 2),
                    ),
                    { _ ->
                        if (submitted == listOf(contractorId, floorId, roomId, lineKey, system, nos, length, breadth, height, deduction, description, location, remarks)) {
                            if (addAnother) { clearDimensions(); description = selectedLine.description; descriptionEdited = false; location = ""; remarks = "" }
                            else onDone()
                        }
                    },
                )
    }
    fun chooseContractor(id: Long) {
        contractorPicker = false
        if (id == contractorId) return
        if (selectedEntry != null) requestLeave {
            record.beginEntry(); contractorId = id; lineKey = ""; itemName = ""; clearDimensions(); description = ""; descriptionEdited = false; location = ""; remarks = ""; selection.clear(); activeField = "description"
        } else changeContext { contractorId = id; lineKey = ""; itemName = ""; clearDimensions(); activeField = "description" }
    }
    if (contractorPicker && contractors.size > 8) {
        MeasurementChoiceDialog("contractor", contractors.map { it.id to it.name }, { contractorPicker = false; if (contractorId <= 0) onCancel() }) { id ->
            chooseContractor(id)
        }
    }
    if (contractorPicker && contractors.size <= 8) {
        fun closePicker() { contractorPicker = false; if (contractorId <= 0) onCancel() }
        AlertDialog(
            onDismissRequest = ::closePicker,
            properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false),
            modifier = Modifier.imePadding().safeDrawingPadding(),
            title = { Text("Select contractor") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose whose work you are measuring. Only their assigned items will be shown.")
                    Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        val matches = contractors
                        if (matches.isEmpty()) Text(if (contractors.isEmpty()) "No contractors have been added to this project." else "No matching contractors.")
                        matches.forEach { contractor ->
                            val count = active?.lines.orEmpty().count { it.contractorId == contractor.id }
                            DropdownMenuItem(
                                text = { Column { Text(contractor.name); Text("$count assigned works", style = MaterialTheme.typography.bodySmall) } },
                                leadingIcon = { Icon(if (contractorId == contractor.id) CarbonIcons.Check else CarbonIcons.People, null) },
                                onClick = {
                                    chooseContractor(contractor.id)
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = { if (contractors.isEmpty() || active == null) TextButton(onClick = { contractorPicker = false; requestLeave(onSetup) }) { Text("Open setup") } },
            dismissButton = { TextButton(onClick = ::closePicker) { Text(if (contractorId > 0) "Keep current contractor" else "Cancel recording") } },
        )
    }
    if (pendingContext != null) AlertDialog(onDismissRequest = { pendingContext = null }, title = { Text("Change measurement context?") },
        text = { Text("Changing contractor or item clears the current dimensions to prevent using them with the wrong formula. Your custom description is retained.") },
        confirmButton = { TextButton(onClick = { val change = pendingContext; pendingContext = null; change?.invoke() }) { Text("Change and clear dimensions") } },
        dismissButton = { TextButton(onClick = { pendingContext = null }) { Text("Keep current entry") } })
    if (pendingLeave != null) AlertDialog(onDismissRequest = { pendingLeave = null }, title = { Text("Leave without saving?") },
        text = { Text("This entry has unsaved values. Save it before leaving, or discard these values.") },
        confirmButton = { TextButton(onClick = { val leave = pendingLeave; pendingLeave = null; leave?.invoke() }) { Text("Discard and leave") } },
        dismissButton = { TextButton(onClick = { pendingLeave = null }) { Text("Keep editing") } })
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        snackbarHost = { SnackbarHost(feedback) },
        topBar = {
            TopAppBar(
                title = { Column { Text(project?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("Record entry", style = MaterialTheme.typography.labelMedium) } },
                navigationIcon = { IconButton(enabled = !busy, onClick = { requestLeave(onCancel) }) { Icon(CarbonIcons.ArrowBack, "Back to measurements") } },
                actions = {
                    IconButton(enabled = !busy && (selectedEntry == null || canEditSelected), onClick = {
                        if (selectedEntry == null) focusField("description") else record.editSelected { editBaseline = editorValues(); activeField = "description" }
                    }) { Icon(CarbonIcons.Edit, if (selectedEntry == null) "Edit description" else "Edit recorded entry") }
                    IconButton(enabled = canSave, onClick = { saveEntry(true) }) { if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(CarbonIcons.Save, if (selectedEntry == null) "Save entry" else "Save changes") }
                    com.holagundi.archiman.ui.theme.CarbonScreenHelp("Record measurements", listOf(
                        "Choose a floor and an assigned work item for a new entry. Previous/next arrows browse recorded measurements for this contractor, including entries for different work items.",
                        "Tap a field to edit it. The fixed keyboard switches between text and numbers; its arrows move between fields.",
                        "For a new entry, Save records it in a draft sheet. For a selected draft/returned entry, Edit unlocks its values and Save updates that same entry. Approved entries are read-only.",
                        "Use the three-dot menu to change contractor, metric/imperial units, or extra details. The system keyboard is blocked on this screen.",
                    ))
                    Box {
                        IconButton(enabled = !busy, onClick = { more = true }) { Icon(CarbonIcons.MoreVert, "Measurement options") }
                        DropdownMenu(more, { more = false }) {
                            DropdownMenuItem(text = { Text("Change contractor") }, leadingIcon = { Icon(CarbonIcons.People, null) }, onClick = { more = false; contractorPicker = true })
                            MeasurementUnitSystem.entries.reversed().forEach { target ->
                                DropdownMenuItem(text = { Text(if (target == MeasurementUnitSystem.METRIC) "Metric (m)" else "Imperial (ft)") }, leadingIcon = { if (target == system) Icon(CarbonIcons.Check, null) }, onClick = { more = false; changeUnits(target) })
                            }
                            DropdownMenuItem(text = { Text(if (details) "Hide location and remarks" else "Room, location and remarks") }, onClick = { more = false; details = !details; activeField = if (details) "location" else "description" })
                            if (selectedEntry != null) DropdownMenuItem(text = { Text("Reload recorded entry") }, onClick = { more = false; requestLeave { record.selectEntry(selectedEntry!!.id, ::loadEntry) } })
                            DropdownMenuItem(text = { Text("New measurement") }, leadingIcon = { Icon(CarbonIcons.Add, null) }, onClick = { more = false; newEntry() })
                            DropdownMenuItem(text = { Text("Save and view sheet") }, enabled = canSave, onClick = { more = false; saveEntry(false) })
                            DropdownMenuItem(text = { Text("View measurement sheets") }, onClick = { more = false; requestLeave(onDone) })
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface { Box(Modifier.navigationBarsPadding()) {
                MeasurementKeyboard(if (editing) fieldLabels[activeField].orEmpty() else "Read-only entry", numericField(activeField), !busy && editing, { keyInput(it) }, { keyInput(null, true) }, { nextField(-1) }, { nextField(1) })
            } }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(selectedEntry?.text("contractorName") ?: contractors.firstOrNull { it.id == contractorId }?.name ?: "Select contractor in measurement options", style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val status = when {
                    selectedSheet?.archivedAt != null -> "Archived — read-only"
                    selectedSheet?.status == "SUBMITTED" -> "Ready to check"
                    selectedSheet?.status == "CHECKED" -> "Awaiting approval"
                    selectedSheet?.status == "RETURNED" -> "Returned with comments"
                    else -> selectedSheet?.status?.lowercase()?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }.orEmpty()
                }
                Text(if (selectedEntry == null) "New entry · ${entries.size} recorded" else "Entry ${entryIndex + 1} of ${entries.size} · $status", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(enabled = !busy && entries.isNotEmpty() && (selectedEntry == null || entryIndex > 0), onClick = { navigateEntry(if (selectedEntry == null) entries.lastIndex else entryIndex - 1) }) { Icon(CarbonIcons.ArrowBack, "Previous recorded measurement") }
                IconButton(enabled = !busy && selectedEntry != null && entryIndex >= 0 && entryIndex < entries.lastIndex, onClick = { navigateEntry(entryIndex + 1) }) { Icon(CarbonIcons.ArrowForward, "Next recorded measurement") }
            }
            MeasurementSelect("Floor", floors.firstOrNull { it.id == floorId }?.name ?: selectedEntry?.text("floor").orEmpty(), floors.map { it.id to it.name }, enabled = !busy && selectedEntry == null) { floorId = it; roomId = 0 }
            MeasurementSelect("Assigned item", itemName, assigned.map { it.key to "${it.code} | ${it.description}" }, enabled = !busy && selectedEntry == null, choose = ::selectItem)
            if (selectedEntry == null && (active == null || assigned.isEmpty())) {
                Text("No assigned works in an active revision for this contractor.", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { requestLeave(onSetup) }) { Text("Open measurement setup") }
            }
            EntryField("description", Modifier.fillMaxWidth())
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val availableWidth = maxWidth
                val dimensions = fieldOrder.filter { numericField(it) && it != "deduction" }
                val minFieldWidth = 80.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
                val fieldWidth = ((availableWidth - 6.dp * (dimensions.size - 1)) / dimensions.size).coerceAtLeast(minFieldWidth)
                Column {
                    if (minFieldWidth * dimensions.size + 6.dp * (dimensions.size - 1) > availableWidth) Text("Swipe to see all dimensions", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { dimensions.forEach { EntryField(it, Modifier.width(fieldWidth)) } }
                }
            }
            if (calcType == CalculationTypes.WALL_PLASTER) EntryField("deduction", Modifier.fillMaxWidth())
            ArchiCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                val viewing = selectedEntry != null && !editing
                val displayUnit = if (viewing) selectedEntry!!.text("unit") else unit
                val displayQuantity = if (viewing) selectedEntry!!.text("quantity").toDoubleOrNull() ?: quantity else quantity
                Text("${if (viewing) "Recorded" else "Calculated"} quantity · $displayUnit", style = MaterialTheme.typography.labelSmall)
                Text(String.format(Locale.US, "%.2f %s", displayQuantity, displayUnit), style = MaterialTheme.typography.titleLarge)
                if (selectedLine != null && targetSystem != system) Text("Saved unit: ${selectedLine.unit}; dimensions are converted when saving.", style = MaterialTheme.typography.bodySmall)
                Text(WorkItemLibrary.formulas[calcType].orEmpty(), style = MaterialTheme.typography.bodySmall)
            } }
            error?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification("Check measurement", it, com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR) }
            if (details) {
                MeasurementSelect("Room", rooms.firstOrNull { it.id == roomId }?.name ?: "Whole floor", listOf(0L to "Whole floor") + rooms.filter { it.floorId == floorId }.map { it.id to it.name }, enabled = !busy && selectedEntry == null) { roomId = it }
                EntryField("location", Modifier.fillMaxWidth()); EntryField("remarks", Modifier.fillMaxWidth())
            }
        }
    }
}
