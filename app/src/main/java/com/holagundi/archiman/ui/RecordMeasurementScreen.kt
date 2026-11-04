@file:OptIn(ExperimentalLayoutApi::class)

package com.holagundi.archiman.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
private fun MeasurementField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    onCalc: (() -> Unit)?
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.width(80.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End
            ),
            singleLine = true
        )
        if (onCalc != null) {
            IconButton(onClick = onCalc) {
                Icon(Icons.Default.Calculate, contentDescription = "Calculate $label")
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
    }
}

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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Page Header - like a physical book
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "MEASUREMENT BOOK",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "New Entry",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Sheet Info Section
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Sheet Information", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            
            Text("Contractor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChipRow(contractors.map { it.id to it.name }, contractorId.takeIf { it != -1L }) { contractorId = it }
            
            Text("Floor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ChipRow(floors.map { it.id to it.name }, floorId.takeIf { it != -1L }) { floorId = it }
            
            val floorRooms = rooms.filter { it.floorId == floorId }
            if (floorRooms.isNotEmpty()) {
                Text("Location", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ChipRow(listOf(0L to "Whole floor") + floorRooms.map { it.id to it.name }, roomId) { roomId = it }
            }
        }

        // Work Item Section
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Work Item", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("Qualified", !useCatalog) { useCatalog = false }
                ChoiceChip("Catalogue", useCatalog) { useCatalog = true }
            }
            ArchiField(query, { query = it }, "Search work items", Modifier.fillMaxWidth())
            
            Text("Selected: $itemName", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("Unit: $unit  |  Type: $calcType", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            choices.take(6).forEach { (name, type, uom) ->
                TextButton(onClick = {
                    itemName = name
                    calcType = type
                    unit = uom
                }) {
                    Text(
                        if (name == itemName) "● $name · $uom" else "$name · $uom",
                        color = if (name == itemName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Measurement Grid - like physical book columns
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            // Column Headers
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text("DIMENSIONS", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text("QUANTITY", modifier = Modifier.width(100.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            HorizontalDivider()

            // Measurement Fields in Grid
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Nos
                MeasurementField(
                    label = "Nos",
                    value = nos,
                    onValueChange = { nos = it },
                    onCalc = null
                )
                
                // Length
                if (calcType != CalculationTypes.NOS) {
                    MeasurementField(
                        label = "Length",
                        value = length,
                        onValueChange = { length = it },
                        onCalc = { calcTarget = "length"; showCalc = true }
                    )
                }
                
                // Breadth
                if (calcType == CalculationTypes.AREA || calcType == CalculationTypes.VOLUME) {
                    MeasurementField(
                        label = "Breadth",
                        value = breadth,
                        onValueChange = { breadth = it },
                        onCalc = { calcTarget = "breadth"; showCalc = true }
                    )
                }
                
                // Height
                if (calcType == CalculationTypes.WALL_PLASTER || calcType == CalculationTypes.VOLUME) {
                    MeasurementField(
                        label = "Height",
                        value = height,
                        onValueChange = { height = it },
                        onCalc = { calcTarget = "height"; showCalc = true }
                    )
                }
                
                // Deduction
                if (calcType == CalculationTypes.WALL_PLASTER) {
                    MeasurementField(
                        label = "Deduction",
                        value = deduction,
                        onValueChange = { deduction = it },
                        onCalc = null
                    )
                }
                
                HorizontalDivider()
                
                // Total Quantity Display
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL QUANTITY", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        String.format(Locale.US, "%.3f %s", animated, unit),
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Details Section
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Description & Remarks", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            HorizontalDivider()
            
            ArchiField(description, { description = it }, "Description of work", Modifier.fillMaxWidth(), singleLine = false)
            ArchiField(location, { location = it }, "Specific location", Modifier.fillMaxWidth())
            ArchiField(remarks, { remarks = it }, "Additional remarks", Modifier.fillMaxWidth())
        }

        if (error != null) {
            Text(error ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.announce())
        }

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
        ) { Text("SAVE TO MEASUREMENT BOOK") }
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
