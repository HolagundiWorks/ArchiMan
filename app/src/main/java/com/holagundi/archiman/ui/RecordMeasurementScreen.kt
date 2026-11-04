@file:OptIn(ExperimentalLayoutApi::class)

package com.holagundi.archiman.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.domain.CalculationTypes
import com.holagundi.archiman.domain.MeasurementDraft
import com.holagundi.archiman.domain.MeasurementInput
import com.holagundi.archiman.domain.QuantityCalculator
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ChoiceChip
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.ScreenTitle
import com.holagundi.archiman.ui.theme.announce
import java.util.Locale

@Composable
fun RecordMeasurementScreen(
    onDone: () -> Unit,
    workspace: ProjectWorkspaceViewModel = hiltViewModel(),
    record: RecordMeasurementViewModel = hiltViewModel(),
) {
    val project by workspace.project.collectAsStateWithLifecycle()
    val floors by workspace.floors.collectAsStateWithLifecycle()
    val rooms by workspace.rooms.collectAsStateWithLifecycle()
    val contractors by workspace.contractors.collectAsStateWithLifecycle()
    val catalog by workspace.workItems.collectAsStateWithLifecycle()
    val error by record.error.collectAsStateWithLifecycle()

    var contractorId by rememberSaveable { mutableStateOf(-1L) }
    var floorId by rememberSaveable { mutableStateOf(-1L) }
    var roomId by rememberSaveable { mutableStateOf(0L) }
    var itemName by rememberSaveable { mutableStateOf("") }
    var calcType by rememberSaveable { mutableStateOf(CalculationTypes.AREA) }
    var unit by rememberSaveable { mutableStateOf("m²") }
    var useCatalog by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var nos by rememberSaveable { mutableStateOf("1") }
    var length by rememberSaveable { mutableStateOf("") }
    var breadth by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var deduction by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var remarks by rememberSaveable { mutableStateOf("") }
    var showCalc by rememberSaveable { mutableStateOf(false) }
    var calcTarget by rememberSaveable { mutableStateOf("length") }

    val qualifiedFlow = remember(contractorId) { workspace.qualified(contractorId) }
    val qualified by qualifiedFlow.collectAsStateWithLifecycle(emptyList())

    LaunchedEffect(contractors) {
        if (contractorId == -1L || contractors.none { it.id == contractorId }) contractorId = contractors.firstOrNull()?.id ?: -1L
    }
    LaunchedEffect(floors) {
        if (floorId == -1L || floors.none { it.id == floorId }) floorId = floors.firstOrNull()?.id ?: -1L
    }
    LaunchedEffect(qualified, catalog, useCatalog) {
        val names = visibleNames(useCatalog, qualified.map { it.itemName }, catalog.map { it.name }, query)
        if (itemName !in names) {
            val firstQualified = qualified.firstOrNull()
            val firstCatalog = catalog.firstOrNull()
            if (!useCatalog && firstQualified != null) {
                itemName = firstQualified.itemName
                calcType = firstQualified.calculationType
                unit = firstQualified.uom
            } else if (firstCatalog != null) {
                itemName = firstCatalog.name
                calcType = firstCatalog.calculationType
                unit = firstCatalog.unit
            }
        }
    }

    val quantity = QuantityCalculator.calculate(
        calcType,
        MeasurementInput(
            no = nos.toDoubleOrNull() ?: 0.0,
            length = length.toDoubleOrNull() ?: 0.0,
            breadth = breadth.toDoubleOrNull() ?: 0.0,
            height = height.toDoubleOrNull() ?: 0.0,
            deduction = deduction.toDoubleOrNull() ?: 0.0,
        ),
    )
    val animated by animateFloatAsState(quantity.toFloat(), tween(280), label = "quantity")
    val numberKeys = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    val choices = if (!useCatalog && qualified.isNotEmpty()) {
        qualified.filter { it.itemName.contains(query, true) }.map { Triple(it.itemName, it.calculationType, it.uom) }
    } else {
        catalog.filter { it.name.contains(query, true) || it.itemCode.contains(query, true) || it.workType.contains(query, true) }
            .map { Triple(it.name, it.calculationType, it.unit) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScreenTitle("Record measurement")
        Text("Quantity only. The sheet stays in draft until it is submitted.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        ChipRow(contractors.map { it.id to it.name }, contractorId.takeIf { it != -1L }) { contractorId = it }
        ChipRow(floors.map { it.id to it.name }, floorId.takeIf { it != -1L }) { floorId = it }
        val floorRooms = rooms.filter { it.floorId == floorId }
        ChipRow(listOf(0L to "Whole floor") + floorRooms.map { it.id to it.name }, roomId) { roomId = it }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip("Qualified", !useCatalog) { useCatalog = false }
            ChoiceChip("Catalogue", useCatalog) { useCatalog = true }
        }
        ArchiField(query, { query = it }, "Search work items", Modifier.fillMaxWidth())
        choices.take(8).forEach { (name, type, uom) ->
            TextButton(onClick = {
                itemName = name
                calcType = type
                unit = uom
            }) {
                Text(if (name == itemName) "● $name · $uom" else "$name · $uom", color = if (name == itemName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
        }
        ArchiField(nos, { nos = it }, "Nos", Modifier.fillMaxWidth(), keyboardOptions = numberKeys)
        if (calcType != CalculationTypes.NOS) {
            ArchiField(length, { length = it }, "Length", Modifier.fillMaxWidth(), keyboardOptions = numberKeys)
            TextButton(onClick = { calcTarget = "length"; showCalc = true }) { Text("Calc length", color = MaterialTheme.colorScheme.primary) }
        }
        if (calcType == CalculationTypes.AREA || calcType == CalculationTypes.VOLUME) {
            ArchiField(breadth, { breadth = it }, "Breadth", Modifier.fillMaxWidth(), keyboardOptions = numberKeys)
            TextButton(onClick = { calcTarget = "breadth"; showCalc = true }) { Text("Calc breadth", color = MaterialTheme.colorScheme.primary) }
        }
        if (calcType == CalculationTypes.WALL_PLASTER || calcType == CalculationTypes.VOLUME) {
            ArchiField(height, { height = it }, "Height", Modifier.fillMaxWidth(), keyboardOptions = numberKeys)
            TextButton(onClick = { calcTarget = "height"; showCalc = true }) { Text("Calc height", color = MaterialTheme.colorScheme.primary) }
        }
        if (calcType == CalculationTypes.WALL_PLASTER) {
            ArchiField(deduction, { deduction = it }, "Opening deduction", Modifier.fillMaxWidth(), keyboardOptions = numberKeys)
        }
        GlassSurface(Modifier.fillMaxWidth()) {
            Column {
                Text("Quantity", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(
                    String.format(Locale.US, "%.2f %s", animated, unit),
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(calcType, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
        ArchiField(description, { description = it }, "Description", Modifier.fillMaxWidth(), singleLine = false)
        ArchiField(location, { location = it }, "Location", Modifier.fillMaxWidth())
        ArchiField(remarks, { remarks = it }, "Remarks", Modifier.fillMaxWidth())
        if (error != null) Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.announce())
        Button(
            onClick = {
                val current = project ?: return@Button
                val contractor = contractors.firstOrNull { it.id == contractorId } ?: return@Button
                val floor = floors.firstOrNull { it.id == floorId } ?: return@Button
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
                        itemId = catalog.firstOrNull { it.name == itemName }?.id ?: 0L,
                        itemName = itemName,
                        unit = unit,
                        calculationType = calcType,
                        description = description,
                        location = location,
                        remarks = remarks,
                        length = length.toDoubleOrNull() ?: 0.0,
                        width = breadth.toDoubleOrNull() ?: 0.0,
                        height = height.toDoubleOrNull() ?: 0.0,
                        nos = nos.toDoubleOrNull() ?: 0.0,
                        deduction = deduction.toDoubleOrNull() ?: 0.0,
                    ),
                    onDone,
                )
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        ) { Text("Save measurement") }
    }

    if (showCalc) {
        Dialog(onDismissRequest = { showCalc = false }) {
            GlassSurface(Modifier.fillMaxWidth()) {
                ArchiCalcPad { value ->
                    val formatted = String.format(Locale.US, "%.3f", value).trimEnd('0').trimEnd('.')
                    when (calcTarget) {
                        "breadth" -> breadth = formatted
                        "height" -> height = formatted
                        else -> length = formatted
                    }
                    showCalc = false
                }
            }
        }
    }
}

private fun visibleNames(useCatalog: Boolean, qualified: List<String>, catalog: List<String>, query: String): Set<String> {
    val source = if (!useCatalog && qualified.isNotEmpty()) qualified else catalog
    return source.filter { it.contains(query, true) }.toSet()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipRow(options: List<Pair<Long, String>>, selected: Long?, onSelect: (Long) -> Unit) {
    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) ->
            ChoiceChip(label, id == selected) { onSelect(id) }
        }
    }
}
