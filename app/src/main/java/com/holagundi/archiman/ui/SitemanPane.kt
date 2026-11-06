package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.domain.AbstractRow
import com.holagundi.archiman.domain.QuantityAbstract
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ChoiceChip
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.ScreenTitle

private enum class SitemanPanel { LOGS, HFC, ABSTRACT, ORDERS, CALC }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SitemanPane(
    suite: SuiteProjectViewModel,
    projectName: String,
    projectCode: String,
) {
    val logs by suite.logs.collectAsStateWithLifecycle()
    val drawings by suite.drawings.collectAsStateWithLifecycle()
    val approved by suite.approvedMeasurements.collectAsStateWithLifecycle()
    val orders by suite.orders.collectAsStateWithLifecycle()
    val lines by suite.orderLines.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var panel by rememberSaveable { mutableStateOf(SitemanPanel.LOGS) }
    var weather by rememberSaveable { mutableStateOf("Fair") }
    var work by rememberSaveable { mutableStateOf("") }
    var manpower by rememberSaveable { mutableStateOf("") }
    var materials by rememberSaveable { mutableStateOf("") }
    var delays by rememberSaveable { mutableStateOf("") }
    var safety by rememberSaveable { mutableStateOf("") }
    var nextDay by rememberSaveable { mutableStateOf("") }
    var preparedBy by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var supplier by rememberSaveable { mutableStateOf("") }
    var itemName by rememberSaveable { mutableStateOf("") }
    var specification by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var unit by rememberSaveable { mutableStateOf("Nos") }
    var calcNote by rememberSaveable { mutableStateOf("") }

    val hfc = drawings.filter { it.packageTag.equals("HFC", ignoreCase = true) }
    val abstract = QuantityAbstract.summarize(
        approved.map { row ->
            AbstractRow(row.itemName, row.unit, row.quantity, approved = true)
        },
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(R.string.siteman_heading))
        Text(stringResource(R.string.siteman_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(stringResource(R.string.siteman_logs), panel == SitemanPanel.LOGS) { panel = SitemanPanel.LOGS }
            ChoiceChip(stringResource(R.string.siteman_hfc), panel == SitemanPanel.HFC) { panel = SitemanPanel.HFC }
            ChoiceChip(stringResource(R.string.siteman_abstract), panel == SitemanPanel.ABSTRACT) { panel = SitemanPanel.ABSTRACT }
            ChoiceChip(stringResource(R.string.siteman_orders), panel == SitemanPanel.ORDERS) { panel = SitemanPanel.ORDERS }
            ChoiceChip(stringResource(R.string.siteman_calc), panel == SitemanPanel.CALC) { panel = SitemanPanel.CALC }
        }

        when (panel) {
            SitemanPanel.LOGS -> {
                if (logs.isEmpty()) Text(stringResource(R.string.siteman_empty_logs), color = MaterialTheme.colorScheme.onSurfaceVariant)
                logs.forEach { log ->
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column {
                            Text(log.preparedBy.ifBlank { "Site" }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                            Text(log.weather, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(log.workCompleted)
                            RecordActions(RecordKind.LOG, log.id, "Daily site log")
                            if (log.manpower.isNotBlank()) Text("Manpower: ${log.manpower}", style = MaterialTheme.typography.bodySmall)
                            if (log.nextDayPlan.isNotBlank()) Text("Next: ${log.nextDayPlan}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArchiField(weather, { weather = it }, stringResource(R.string.siteman_weather))
                        ArchiField(work, { work = it }, stringResource(R.string.siteman_work), singleLine = false)
                        ArchiField(manpower, { manpower = it }, stringResource(R.string.siteman_manpower))
                        ArchiField(materials, { materials = it }, stringResource(R.string.siteman_materials))
                        ArchiField(delays, { delays = it }, stringResource(R.string.siteman_delays))
                        ArchiField(safety, { safety = it }, stringResource(R.string.siteman_safety))
                        ArchiField(nextDay, { nextDay = it }, stringResource(R.string.siteman_next))
                        ArchiField(preparedBy, { preparedBy = it }, stringResource(R.string.siteman_by))
                        Button(
                            onClick = {
                                val submitted = listOf(work); suite.addLog(weather, work, preparedBy, manpower, materials, delays, safety, nextDay).onSuccess { if (listOf(work) == submitted) { work = "" } }
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.siteman_save_log)) }
                    }
                }
            }
            SitemanPanel.HFC -> {
                if (hfc.isEmpty()) Text(stringResource(R.string.siteman_empty_hfc), color = MaterialTheme.colorScheme.onSurfaceVariant)
                hfc.forEach { drawing ->
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column {
                            Text(drawing.drawingNumber, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            Text(drawing.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                            Text("HFC · ${drawing.discipline}")
                        }
                    }
                }
            }
            SitemanPanel.ABSTRACT -> {
                if (abstract.isEmpty()) {
                    Text(stringResource(R.string.siteman_empty_abstract), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                abstract.forEach { line ->
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column {
                            Text(line.itemName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                            Text(
                                "${"%.2f".format(line.totalQuantity)} ${line.unit} · ${line.rowCount} rows",
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
                Button(
                    onClick = {
                        shareCsv(
                            context,
                            "Quantity_Abstract_${projectCode.ifBlank { "project" }}.csv",
                            QuantityAbstract.csv(projectName, projectCode, abstract),
                        )
                    },
                    enabled = abstract.isNotEmpty(),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.siteman_share_abstract)) }
            }
            SitemanPanel.ORDERS -> {
                if (orders.isEmpty()) Text(stringResource(R.string.siteman_empty_orders), color = MaterialTheme.colorScheme.onSurfaceVariant)
                orders.forEach { order ->
                    val orderLines = lines.filter { it.orderId == order.id }
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(order.referenceNumber, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            Text(order.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                            Text("${order.supplier} · ${order.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            RecordActions(RecordKind.ORDER, order.id, order.referenceNumber)
                            orderLines.forEach { line ->
                                Text("${line.itemName} · ${line.quantity} ${line.unit}")
                                RecordActions(RecordKind.ORDER_LINE, line.id, line.itemName)
                                if (line.specification.isNotBlank()) {
                                    Text(line.specification, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (order.status != "RECEIVED") {
                                TextButton(onClick = { suite.receiveOrder(order.id) }) { Text("Receive") }
                            }
                        }
                    }
                }
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ArchiField(title, { title = it }, stringResource(R.string.drawing_title))
                        ArchiField(supplier, { supplier = it }, stringResource(R.string.siteman_supplier))
                        ArchiField(itemName, { itemName = it }, stringResource(R.string.siteman_item))
                        ArchiField(specification, { specification = it }, stringResource(R.string.siteman_spec), singleLine = false)
                        ArchiField(quantity, { quantity = it }, stringResource(R.string.siteman_qty))
                        ArchiField(unit, { unit = it }, stringResource(R.string.siteman_unit))
                        Button(
                            onClick = {
                                val submitted = listOf(title, itemName, specification, quantity); suite.addOrder(
                                    title,
                                    supplier,
                                    itemName,
                                    specification,
                                    quantity.toDoubleOrNull() ?: 0.0,
                                    unit,
                                ).onSuccess { if (listOf(title, itemName, specification, quantity) == submitted) { title = ""; itemName = ""; specification = ""; quantity = "" } }
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.siteman_save_order)) }
                    }
                }
            }
            SitemanPanel.CALC -> {
                if (calcNote.isNotBlank()) {
                    Text(calcNote, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                }
                ArchiCalcPad { value -> calcNote = value.toString() }
            }
        }
    }
}
