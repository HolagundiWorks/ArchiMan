@file:OptIn(ExperimentalLayoutApi::class)

package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.data.local.entity.MeasurementEntity
import com.holagundi.archiman.data.local.entity.MeasurementSheetEntity
import com.holagundi.archiman.domain.MeasurementSheetWorkflow
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.domain.SheetStatus
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ChoiceChip
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.ScreenTitle
import com.holagundi.archiman.ui.theme.announce
import java.util.Locale

@Composable
fun MeasurementBookScreen(
    onRecord: () -> Unit,
    focusId: Long = 0L,
    workspace: ProjectWorkspaceViewModel = hiltViewModel(),
    book: MeasurementBookViewModel = hiltViewModel(),
) {
    val project by workspace.project.collectAsStateWithLifecycle()
    val sheets by workspace.sheets.collectAsStateWithLifecycle()
    val measurements by workspace.measurements.collectAsStateWithLifecycle()
    val notice by book.notice.collectAsStateWithLifecycle()
    val pending by book.pending.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var query by rememberSaveable { mutableStateOf("") }
    var focused by rememberSaveable(focusId) { mutableStateOf(focusId) }
    var openId by rememberSaveable(focusId) { mutableStateOf<Long?>(focusId.takeIf { it > 0 }) }
    var returnId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comment by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val visible = sheets.filter { sheet ->
        (focused == 0L || sheet.id == focused) && (filter == "ALL" || sheet.status == filter) &&
            listOf(sheet.sheetCode, sheet.itemNameSnapshot, sheet.contractorNameSnapshot, sheet.floorNameSnapshot)
                .any { it.contains(query, true) }
    }

    val grouped=androidx.compose.runtime.remember(measurements) { measurements.groupBy { it.sheetId } }
    var sheetPage by rememberSaveable(query,filter) { mutableStateOf(0) }
    var rowPage by rememberSaveable(openId) { mutableStateOf(0) }
    val pageCount=((visible.size+24)/25).coerceAtLeast(1)
    val currentPage=sheetPage.coerceAtMost(pageCount-1)
    val pageSheets = visible.drop(currentPage*25).take(25)
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle("Measurement Book") }
        if (focused > 0) item { TextButton(onClick = { focused = 0L }) { Text("Show all project sheets") } }
        item {
            Text("Quantity-only sheets. Draft, submitted, checked, approved or returned.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
        item { TextButton(onClick = onRecord) { Text("Record measurement", color = MaterialTheme.colorScheme.primary) } }
        item { ArchiField(query, { query = it }, "Search sheets", Modifier.fillMaxWidth()) }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("ALL") + SheetStatus.ALL).forEach { status ->
                    val count = if (status == "ALL") sheets.size else sheets.count { it.status == status }
                    ChoiceChip("${sheetStatusLabel(status)} $count", filter == status) { filter = status }
                }
            }
        }
        if (notice != null) {
            item {
                notice?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification("Measurement review", it.message, it.kind) }
                TextButton(onClick = book::clearNotice) { Text("Dismiss notice") }
            }
        }
        if (visible.size>25) item {
            Row {
                TextButton(enabled=currentPage>0,onClick={sheetPage=currentPage-1}) { Text("Previous sheets") }
                Text("Page ${currentPage+1} / $pageCount")
                TextButton(enabled=currentPage+1<pageCount,onClick={sheetPage=currentPage+1}) { Text("Next sheets") }
            }
        }
        items(pageSheets, key = { it.id }) { sheet ->
            RecordActions(RecordKind.SHEET, sheet.id, sheet.sheetCode)
            val rows = grouped[sheet.id].orEmpty()
            val total = rows.sumOf { it.quantity }
            
            // Physical book style sheet
            Column(
                Modifier
                    .fillMaxWidth()
                    .border(2.dp, MaterialTheme.colorScheme.outline)
            ) {
                // Sheet Header - like a page header in physical book
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            sheet.sheetCode,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            Modifier
                                .background(statusBackground(sheet.status))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                statusLabel(sheet),
                                color = statusColor(sheet.status),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        sheet.itemNameSnapshot,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "${sheet.contractorNameSnapshot} · ${sheet.floorNameSnapshot}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "TOTAL:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            String.format(Locale.US, "%.3f %s", total, sheet.uomSnapshot),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(thickness = 2.dp)

                // Action buttons
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { openId = if (openId == sheet.id) null else sheet.id }) {
                        Text(if (openId == sheet.id) "Close" else "View entries (${rows.size})")
                    }
                    TextButton(onClick = {
                        val projectId = project?.id ?: return@TextButton
                        book.export(projectId, sheet.id) { name, csv -> shareCsv(context, name, csv) }
                    }) {
                        Text("Export CSV")
                    }
                }

                // Measurement entries table
                if (openId == sheet.id) {
                    HorizontalDivider()
                    
                    val tableScroll = rememberScrollState()
                    // Header and entry rows share one horizontal coordinate system.
                    // Table header
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(8.dp)
                            .horizontalScroll(tableScroll)
                    ) {
                        TableHeaderCell("Description", Modifier.width(120.dp))
                        TableHeaderCell("Location", Modifier.width(100.dp))
                        TableHeaderCell("Nos", Modifier.width(60.dp))
                        TableHeaderCell("Length", Modifier.width(70.dp))
                        TableHeaderCell("Breadth", Modifier.width(70.dp))
                        TableHeaderCell("Height", Modifier.width(70.dp))
                        TableHeaderCell("Deduction", Modifier.width(70.dp))
                        TableHeaderCell("Quantity (${sheet.uomSnapshot})", Modifier.width(100.dp))
                    }

                    HorizontalDivider()

                    // Table rows
                    val rowPages=((rows.size+99)/100).coerceAtLeast(1)
                    val currentRowPage=rowPage.coerceAtMost(rowPages-1)
                    if(rows.size>100) Row {
                        TextButton(enabled=currentRowPage>0,onClick={rowPage=currentRowPage-1}) { Text("Previous entries") }
                        Text("${currentRowPage+1} / $rowPages")
                        TextButton(enabled=currentRowPage+1<rowPages,onClick={rowPage=currentRowPage+1}) { Text("Next entries") }
                    }
                    rows.drop(currentRowPage*100).take(100).forEachIndexed { offset, row ->
                        val index=currentRowPage*100+offset
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(if (index % 2 == 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(8.dp)
                                .horizontalScroll(tableScroll)
                                .clearAndSetSemantics { contentDescription = "Entry ${index + 1}. ${row.description}. Location ${row.location}. Count ${row.nos}. Length ${row.length}, breadth ${row.width}, height ${row.height}. Quantity ${row.quantity} ${sheet.uomSnapshot}." },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell(row.description, Modifier.width(120.dp))
                            TableCell(row.location, Modifier.width(100.dp))
                            TableCell(String.format(Locale.US, "%.0f", row.nos), Modifier.width(60.dp), true)
                            TableCell(String.format(Locale.US, "%.2f", row.length), Modifier.width(70.dp), true)
                            TableCell(String.format(Locale.US, "%.2f", row.width), Modifier.width(70.dp), true)
                            TableCell(String.format(Locale.US, "%.2f", row.height), Modifier.width(70.dp), true)
                            TableCell(String.format(Locale.US, "%.2f", row.deduction), Modifier.width(70.dp), true)
                            TableCell(
                                String.format(Locale.US, "%.3f", row.quantity),
                                Modifier.width(100.dp),
                                true,
                                MaterialTheme.colorScheme.primary,
                                FontWeight.Bold
                            )
                        }
                        if (sheet.status != SheetStatus.APPROVED && sheet.archivedAt == null) {
                            RecordActions(RecordKind.MEASUREMENT, row.id, row.description.ifBlank { "Measurement ${index + 1}" })
                        }
                        HorizontalDivider()
                    }

                    // Sheet actions (status transitions)
                    if (MeasurementSheetWorkflow.allowedTransitions(sheet.status).isNotEmpty()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                                .padding(12.dp)
                        ) {
                            Text(
                                "Sheet Actions",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MeasurementSheetWorkflow.allowedTransitions(sheet.status).forEach { next ->
                                    TextButton(enabled = "sheet:${sheet.id}" !in pending, onClick = {
                                        if (next == SheetStatus.RETURNED) returnId = sheet.id else book.transition(sheet.id, next, "")
                                    }) {
                                        Text("→ $next")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
    if (returnId != null) {
        Dialog(onDismissRequest = { returnId = null }) {
            GlassSurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Return sheet", style = MaterialTheme.typography.titleMedium)
                    ArchiField(comment, { comment = it }, "Comment", singleLine = false)
                    TextButton(onClick = {
                        val id = returnId ?: return@TextButton
                        if (comment.isBlank()) return@TextButton
                        book.transition(id, SheetStatus.RETURNED, comment)
                        comment = ""
                        returnId = null
                    }) { Text("Return ${sheets.firstOrNull { it.id == returnId }?.sheetCode ?: "sheet"}") }
                }
            }
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(4.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Start,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    fontWeight: FontWeight = FontWeight.Normal
) {
    Text(
        text,
        modifier = modifier.padding(4.dp),
        style = MaterialTheme.typography.bodySmall,
        fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
        textAlign = if (monospace) TextAlign.End else TextAlign.Start,
        color = color,
        fontWeight = fontWeight,
        maxLines = 2
    )
}

private fun sheetStatusLabel(status: String) = when (status) {
    "ALL" -> "All"; "DRAFT" -> "Draft"; "SUBMITTED" -> "Ready to check"; "CHECKED" -> "Awaiting approval"; "APPROVED" -> "Approved"; "RETURNED" -> "Returned with comments"; else -> status.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}
private fun statusLabel(sheet: MeasurementSheetEntity) = sheetStatusLabel(sheet.status)

@Composable
private fun statusColor(status: String) = when (status) {
    SheetStatus.APPROVED -> com.holagundi.archiman.ui.theme.carbonSupport.success
    SheetStatus.RETURNED -> MaterialTheme.colorScheme.error
    SheetStatus.CHECKED -> com.holagundi.archiman.ui.theme.carbonSupport.warning
    SheetStatus.SUBMITTED -> com.holagundi.archiman.ui.theme.carbonSupport.info
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun statusBackground(status: String) = when(status) {
    SheetStatus.APPROVED -> com.holagundi.archiman.ui.theme.carbonSupport.successContainer
    SheetStatus.RETURNED -> MaterialTheme.colorScheme.errorContainer
    SheetStatus.CHECKED -> com.holagundi.archiman.ui.theme.carbonSupport.warningContainer
    SheetStatus.SUBMITTED -> com.holagundi.archiman.ui.theme.carbonSupport.infoContainer
    else -> MaterialTheme.colorScheme.surfaceContainerHigh
}
