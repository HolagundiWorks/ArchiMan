@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.attachments.SiteAttachmentStore
import com.example.data.local.entity.DailySiteReportEntity
import com.example.data.local.entity.MaterialOrderEntity
import com.example.data.local.entity.MaterialOrderLineEntity
import com.example.data.local.entity.MeasurementEntity
import com.example.data.local.entity.ProjectDrawingEntity
import com.example.data.local.entity.SiteDocumentEntity
import com.example.domain.archicalc.ArchiCalcEngine
import com.example.domain.archicalc.ArchiCalcMeasure
import com.example.ui.icons.CarbonIcons
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.ProjectSection
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SitemanPage(val labelRes: Int) {
    LOGS(R.string.siteman_logs),
    HFC(R.string.siteman_hfc),
    QUANTITIES(R.string.siteman_quantities),
    ORDERS(R.string.siteman_po),
    CALC(R.string.siteman_calc)
}

/**
 * Site-manager hub: daily logs, HFC drawings/docs, quantity abstracts,
 * material purchase orders, and the embedded Archi Calc surface.
 */
@Composable
fun SitemanScreen(
    viewModel: SiteViewModel,
    dailyReports: List<DailySiteReportEntity>,
    documents: List<SiteDocumentEntity>,
    drawings: List<ProjectDrawingEntity>,
    orders: List<MaterialOrderEntity>,
    measurements: List<MeasurementEntity>,
    projectName: String
) {
    var page by remember { mutableStateOf(SitemanPage.LOGS) }
    Column(Modifier.fillMaxSize().testTag("siteman_hub")) {
        Text(
            stringResource(R.string.siteman_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ScrollableTabRow(
            selectedTabIndex = page.ordinal,
            edgePadding = 12.dp,
            divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        ) {
            SitemanPage.entries.forEach { tab ->
                Tab(
                    selected = page == tab,
                    onClick = { page = tab },
                    text = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
        when (page) {
            SitemanPage.LOGS -> SitemanLogsTab(viewModel, dailyReports)
            SitemanPage.HFC -> SitemanHfcTab(viewModel, drawings, documents)
            SitemanPage.QUANTITIES -> SitemanQuantitiesTab(viewModel, measurements, projectName)
            SitemanPage.ORDERS -> SitemanOrdersTab(viewModel, orders, projectName)
            SitemanPage.CALC -> ArchiCalcPane()
        }
    }
}

@Composable
private fun SitemanLogsTab(viewModel: SiteViewModel, reports: List<DailySiteReportEntity>) {
    var creating by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<DailySiteReportEntity?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Daily activity for the site manager. Full snag / inspection / minutes live under Site reports.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.setProjectSection(ProjectSection.REPORTS) }) {
                    Text("All site reports")
                }
            }
            if (reports.isEmpty()) {
                SitemanEmpty(
                    "No daily site logs",
                    "Capture weather, manpower, work done, materials, delays, safety and next-day plan."
                )
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reports, key = { it.id }) { item ->
                        SitemanCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                Column(
                                    Modifier.weight(1f).clickable { editing = item },
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.reportDate)),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(item.workCompleted, style = MaterialTheme.typography.bodySmall)
                                    if (item.weather.isNotBlank()) {
                                        Text("Weather: ${item.weather}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (item.manpower.isNotBlank()) {
                                        Text("Manpower: ${item.manpower}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (item.preparedBy.isNotBlank()) {
                                        Text("Prepared by: ${item.preparedBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteDailySiteReport(item) }) {
                                    Icon(CarbonIcons.DeleteOutline, "Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("siteman_add_log"),
            shape = MaterialTheme.shapes.small
        ) { Icon(CarbonIcons.Add, "Add daily log") }
    }
    if (creating) {
        SitemanDailyLogDialog(existing = null, onDismiss = { creating = false }) { work, weather, manpower, materials, delays, safety, next, by, photo, reportDate ->
            viewModel.upsertDailySiteReport(work, weather, manpower, materials, delays, safety, next, by, photo, reportDate = reportDate)
            creating = false
        }
    }
    editing?.let { item ->
        SitemanDailyLogDialog(existing = item, onDismiss = { editing = null }) { work, weather, manpower, materials, delays, safety, next, by, photo, reportDate ->
            viewModel.upsertDailySiteReport(work, weather, manpower, materials, delays, safety, next, by, photo, reportDate = reportDate, existing = item)
            editing = null
        }
    }
}

@Composable
private fun SitemanHfcTab(
    viewModel: SiteViewModel,
    drawings: List<ProjectDrawingEntity>,
    documents: List<SiteDocumentEntity>
) {
    var addingDoc by remember { mutableStateOf(false) }
    val hfcDrawings = remember(drawings) { drawings.filter { it.packageTag.equals("HFC", ignoreCase = true) } }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(stringResource(R.string.siteman_hfc_help), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Text("Held-for-Construction drawings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (drawings.isEmpty()) {
                item { Text("Register drawings first under Drawings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(drawings, key = { "d-${it.id}" }) { drawing ->
                    val tagged = drawing.packageTag.equals("HFC", ignoreCase = true)
                    SitemanCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(drawing.title.ifBlank { drawing.drawingNumber }, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    listOf(drawing.drawingNumber, drawing.status, if (tagged) "HFC" else "Unpacked").filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tagged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilterChip(
                                selected = tagged,
                                onClick = {
                                    viewModel.setDrawingPackageTag(drawing, if (tagged) "" else "HFC")
                                },
                                label = { Text(if (tagged) "HFC" else "Tag HFC") }
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(4.dp))
                Text("Supporting documents (${documents.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("${hfcDrawings.size} drawings currently tagged HFC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (documents.isEmpty()) {
                item { Text("No attached site documents yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(documents, key = { "doc-${it.id}" }) { doc ->
                    SitemanCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(doc.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                if (doc.notes.isNotBlank()) Text(doc.notes, style = MaterialTheme.typography.bodySmall)
                                if (!doc.fileUri.isNullOrBlank()) {
                                    Text("Attachment on file", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteSiteDocument(doc) }) {
                                Icon(CarbonIcons.DeleteOutline, "Delete document")
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { addingDoc = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("siteman_add_doc"),
            shape = MaterialTheme.shapes.small
        ) { Icon(CarbonIcons.Add, "Add document") }
    }
    if (addingDoc) {
        SitemanDocumentDialog(drawings = drawings, onDismiss = { addingDoc = false }) { title, category, notes, linkedId, uri ->
            viewModel.addSiteDocument(title, category, notes, linkedId, uri)
            addingDoc = false
        }
    }
}

@Composable
private fun SitemanQuantitiesTab(
    viewModel: SiteViewModel,
    measurements: List<MeasurementEntity>,
    projectName: String
) {
    val context = LocalContext.current
    val abstractRows = remember(measurements) {
        measurements.groupBy { it.itemName to it.unit }
            .map { (key, rows) -> Triple(key.first, key.second, rows.sumOf { it.quantity }) }
            .sortedBy { it.first.lowercase(Locale.US) }
    }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.siteman_quantities_help), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) }) {
                Icon(CarbonIcons.Assignment, null, Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Open Measurement Book")
            }
            OutlinedButton(
                onClick = {
                    if (!viewModel.openCanonicalMeasurement()) {
                        viewModel.navigateTo(AppScreen.DEDICATED_MEASUREMENT)
                    }
                }
            ) {
                Icon(CarbonIcons.Straighten, null, Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text("Record measurement")
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (measurements.isEmpty()) "Nothing to export yet" else "Export quantity abstract",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                enabled = measurements.isNotEmpty(),
                onClick = { ExportHelper.exportAndShareCsv(context, projectName, measurements) }
            ) { Icon(CarbonIcons.Download, "Export CSV") }
            IconButton(
                enabled = measurements.isNotEmpty(),
                onClick = { ExportHelper.exportAndShareXls(context, projectName, measurements) }
            ) { Icon(CarbonIcons.Share, "Export spreadsheet") }
            IconButton(
                enabled = measurements.isNotEmpty(),
                onClick = { ExportHelper.exportAndSharePdf(context, projectName, measurements) }
            ) { Icon(CarbonIcons.PictureAsPdf, "Export PDF") }
        }
        if (abstractRows.isEmpty()) {
            SitemanEmpty("No measured quantities", "Record site measurements, then export the quantity abstract for valuation prep.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(abstractRows, key = { "${it.first}|${it.second}" }) { (name, unit, qty) ->
                    SitemanCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(String.format(Locale.US, "%.2f", qty), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SitemanOrdersTab(
    viewModel: SiteViewModel,
    orders: List<MaterialOrderEntity>,
    projectName: String
) {
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var inspecting by remember { mutableStateOf<MaterialOrderEntity?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Text(
                stringResource(R.string.siteman_po_help),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            if (orders.isEmpty()) {
                SitemanEmpty("No purchase orders", "Create a quantity-and-specification material order, then issue it to the supplier.")
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp, 8.dp, 12.dp, 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(orders, key = { it.id }) { order ->
                        val lines by viewModel.materialOrderLines(order.id).collectAsState(initial = emptyList())
                        SitemanCard {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).clickable { inspecting = order }) {
                                        Text("${order.referenceNumber} · ${order.title}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(
                                            listOf(order.status, order.supplier.ifBlank { "No supplier" }, "${lines.size} lines")
                                                .joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (order.status == "ISSUED") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (order.status == "DRAFT") {
                                        TextButton(onClick = { viewModel.issueMaterialOrder(order) }) { Text("Issue") }
                                    }
                                    IconButton(
                                        onClick = {
                                            ExportHelper.exportMaterialOrderCsv(context, projectName, order, lines)
                                        }
                                    ) { Icon(CarbonIcons.Download, "Export order") }
                                    IconButton(onClick = { viewModel.deleteMaterialOrder(order) }) {
                                        Icon(CarbonIcons.DeleteOutline, "Delete order")
                                    }
                                }
                                lines.take(3).forEach { line ->
                                    Text(
                                        "${line.itemName} — ${String.format(Locale.US, "%.2f", line.quantity)} ${line.unit}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                if (lines.size > 3) {
                                    Text("+${lines.size - 3} more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { creating = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).testTag("siteman_add_po"),
            shape = MaterialTheme.shapes.small
        ) { Icon(CarbonIcons.Add, "Add purchase order") }
    }
    if (creating) {
        SitemanOrderDialog(onDismiss = { creating = false }) { ref, title, supplier, notes, lines ->
            viewModel.createMaterialOrder(ref, title, supplier, notes, lines)
            creating = false
        }
    }
    inspecting?.let { order ->
        val lines by viewModel.materialOrderLines(order.id).collectAsState(initial = emptyList())
        AlertDialog(
            onDismissRequest = { inspecting = null },
            title = { Text(order.referenceNumber) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(order.title, fontWeight = FontWeight.Bold)
                    if (order.supplier.isNotBlank()) Text("Supplier: ${order.supplier}")
                    if (order.notes.isNotBlank()) Text(order.notes, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider()
                    lines.forEach { line ->
                        Text("${line.itemName} · ${line.specification}".trimEnd(' ', '·'), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("${String.format(Locale.US, "%.2f", line.quantity)} ${line.unit}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { inspecting = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun ArchiCalcPane() {
    var expression by remember { mutableStateOf("") }
    var display by remember { mutableStateOf("0.00") }
    val grid = remember(expression) { ArchiCalcMeasure.formatGrid(expression.ifBlank { "0" }) }
    Column(
        Modifier.fillMaxSize().padding(12.dp).verticalScroll(rememberScrollState()).testTag("archi_calc_pane"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(R.string.siteman_calc_help), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.End) {
                Text(expression.ifBlank { "0" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(display, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeasureStat("m", grid.getValue("m"), Modifier.weight(1f))
            MeasureStat("m²", grid.getValue("m2"), Modifier.weight(1f))
            MeasureStat("m³", grid.getValue("m3"), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeasureStat("ft", grid.getValue("ft"), Modifier.weight(1f))
            MeasureStat("ft²", grid.getValue("ft2"), Modifier.weight(1f))
            MeasureStat("ft³", grid.getValue("ft3"), Modifier.weight(1f))
        }
        val keys = listOf(
            listOf("C", "(", ")", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "x", "="),
            listOf("√", "x²", "1/x", "+/−")
        )
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { key ->
                    OutlinedButton(
                        onClick = {
                            when (key) {
                                "C" -> { expression = ""; display = "0.00" }
                                "=" -> {
                                    display = ArchiCalcEngine.evaluate(expression)
                                    if (display != "Error") expression = display
                                }
                                "√" -> {
                                    display = ArchiCalcEngine.applyFn(expression.ifBlank { display }, "sqrt")
                                    if (display != "Error") expression = display
                                }
                                "x²" -> {
                                    display = ArchiCalcEngine.applyFn(expression.ifBlank { display }, "x2")
                                    if (display != "Error") expression = display
                                }
                                "1/x" -> {
                                    display = ArchiCalcEngine.applyFn(expression.ifBlank { display }, "1/x")
                                    if (display != "Error") expression = display
                                }
                                "+/−" -> {
                                    display = ArchiCalcEngine.applyFn(expression.ifBlank { display }, "+/-")
                                    if (display != "Error") expression = display
                                }
                                else -> expression += key
                            }
                        },
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) { Text(key) }
                }
            }
        }
        Text(
            "Tip: enter lengths as 3.2 x 4.5 or 10'-6\" x 8' for area / volume conversion.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = {
                display = ArchiCalcEngine.evaluate(expression)
                if (display != "Error") {
                    // Keep expression for measure grid (LxB / LxBxH) when it contains factors.
                    if (!expression.contains('x', ignoreCase = true) && !expression.contains('*')) {
                        expression = display
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Evaluate & refresh measures") }
    }
}

@Composable
private fun MeasureStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.small
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SitemanCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) { Box(Modifier.padding(8.dp)) { content() } }
}

@Composable
private fun SitemanEmpty(title: String, supporting: String) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(CarbonIcons.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(supporting, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SitemanFormDialog(
    title: String,
    onDismiss: () -> Unit,
    enabled: Boolean,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp),
                content = content
            )
        },
        confirmButton = { TextButton(enabled = enabled, onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SitemanField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun SitemanDailyLogDialog(
    existing: DailySiteReportEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String, String?, Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { SiteAttachmentStore(context) }
    var work by remember(existing?.id) { mutableStateOf(existing?.workCompleted.orEmpty()) }
    var weather by remember(existing?.id) { mutableStateOf(existing?.weather.orEmpty()) }
    var manpower by remember(existing?.id) { mutableStateOf(existing?.manpower.orEmpty()) }
    var materials by remember(existing?.id) { mutableStateOf(existing?.materialsReceived.orEmpty()) }
    var delays by remember(existing?.id) { mutableStateOf(existing?.delaysOrConstraints.orEmpty()) }
    var safety by remember(existing?.id) { mutableStateOf(existing?.safetyObservations.orEmpty()) }
    var next by remember(existing?.id) { mutableStateOf(existing?.nextDayPlan.orEmpty()) }
    var preparedBy by remember(existing?.id) { mutableStateOf(existing?.preparedBy.orEmpty()) }
    var photo by remember(existing?.id) { mutableStateOf(existing?.photoUri) }
    var dateText by remember(existing?.id) {
        mutableStateOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(existing?.reportDate ?: System.currentTimeMillis()))
        )
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            photo = runCatching { withContext(Dispatchers.IO) { store.import(uri) } }.getOrNull() ?: photo
        }
    }
    val reportDate = remember(dateText) {
        runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }.parse(dateText)?.time }.getOrNull()
    }
    SitemanFormDialog(
        title = if (existing == null) "Daily site log" else "Edit daily site log",
        onDismiss = onDismiss,
        enabled = work.isNotBlank() && reportDate != null,
        onSave = { onSave(work, weather, manpower, materials, delays, safety, next, preparedBy, photo, reportDate ?: 0L) }
    ) {
        SitemanField(dateText, { dateText = it }, "Date YYYY-MM-DD")
        SitemanField(work, { work = it }, "Work completed", singleLine = false)
        SitemanField(weather, { weather = it }, "Weather")
        SitemanField(manpower, { manpower = it }, "Manpower")
        SitemanField(materials, { materials = it }, "Materials received", singleLine = false)
        SitemanField(delays, { delays = it }, "Delays / constraints", singleLine = false)
        SitemanField(safety, { safety = it }, "Safety observations", singleLine = false)
        SitemanField(next, { next = it }, "Next-day plan", singleLine = false)
        SitemanField(preparedBy, { preparedBy = it }, "Prepared by")
        TextButton(onClick = { picker.launch(arrayOf("image/*")) }) {
            Text(if (photo == null) "Attach photo" else "Replace photo")
        }
    }
}

@Composable
private fun SitemanDocumentDialog(
    drawings: List<ProjectDrawingEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Long?, String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = remember(context) { SiteAttachmentStore(context) }
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("HFC") }
    var notes by remember { mutableStateOf("") }
    var linkedId by remember { mutableStateOf<Long?>(null) }
    var fileUri by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            fileUri = runCatching { withContext(Dispatchers.IO) { store.import(uri) } }.getOrNull()
        }
    }
    SitemanFormDialog("Add site document", onDismiss, title.isNotBlank(), {
        onSave(title, category, notes, linkedId, fileUri)
    }) {
        SitemanField(title, { title = it }, "Title")
        SitemanField(category, { category = it }, "Category")
        SitemanField(notes, { notes = it }, "Notes", singleLine = false)
        Text("Link to drawing (optional)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        drawings.take(12).forEach { drawing ->
            FilterChip(
                selected = linkedId == drawing.id,
                onClick = { linkedId = if (linkedId == drawing.id) null else drawing.id },
                label = { Text(drawing.drawingNumber.ifBlank { drawing.title }) }
            )
        }
        TextButton(onClick = { picker.launch(arrayOf("*/*")) }) {
            Text(if (fileUri == null) "Attach file" else "Replace file")
        }
    }
}

@Composable
private fun SitemanOrderDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, List<Triple<String, String, Pair<Double, String>>>) -> Unit
) {
    var reference by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var specification by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Nos") }
    var lines by remember { mutableStateOf(listOf<Triple<String, String, Pair<Double, String>>>()) }
    SitemanFormDialog(
        title = "New purchase order",
        onDismiss = onDismiss,
        enabled = reference.isNotBlank() && title.isNotBlank() && lines.isNotEmpty(),
        onSave = { onSave(reference, title, supplier, notes, lines) }
    ) {
        SitemanField(reference, { reference = it }, "Reference (e.g. PO-001)")
        SitemanField(title, { title = it }, "Title")
        SitemanField(supplier, { supplier = it }, "Supplier")
        SitemanField(notes, { notes = it }, "Notes", singleLine = false)
        HorizontalDivider()
        Text("Order lines", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        lines.forEachIndexed { index, line ->
            Text(
                "${index + 1}. ${line.first} · ${String.format(Locale.US, "%.2f", line.third.first)} ${line.third.second}",
                style = MaterialTheme.typography.labelSmall
            )
        }
        SitemanField(itemName, { itemName = it }, "Item name")
        SitemanField(specification, { specification = it }, "Specification")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            SitemanField(quantityText, { quantityText = it }, "Qty", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
            SitemanField(unit, { unit = it }, "Unit", Modifier.weight(1f))
        }
        TextButton(
            onClick = {
                val qty = quantityText.toDoubleOrNull() ?: return@TextButton
                if (itemName.isBlank() || qty <= 0) return@TextButton
                lines = lines + Triple(itemName.trim(), specification.trim(), qty to unit.trim().ifBlank { "Nos" })
                itemName = ""
                specification = ""
                quantityText = "1"
            }
        ) { Text("Add line") }
    }
}
