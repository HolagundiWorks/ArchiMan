@file:OptIn(ExperimentalLayoutApi::class)

package com.holagundi.archiman.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.data.local.entity.MeasurementEntity
import com.holagundi.archiman.data.local.entity.MeasurementSheetEntity
import com.holagundi.archiman.domain.MeasurementSheetWorkflow
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
    workspace: ProjectWorkspaceViewModel = hiltViewModel(),
    book: MeasurementBookViewModel = hiltViewModel(),
) {
    val project by workspace.project.collectAsStateWithLifecycle()
    val sheets by workspace.sheets.collectAsStateWithLifecycle()
    val measurements by workspace.measurements.collectAsStateWithLifecycle()
    val notice by book.notice.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf("ALL") }
    var query by rememberSaveable { mutableStateOf("") }
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    var returnId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comment by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val visible = sheets.filter { sheet ->
        (filter == "ALL" || sheet.status == filter) &&
            listOf(sheet.sheetCode, sheet.itemNameSnapshot, sheet.contractorNameSnapshot, sheet.floorNameSnapshot)
                .any { it.contains(query, true) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ScreenTitle("Measurement Book")
        Text("Quantity-only sheets. Draft, submitted, checked, approved or returned.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onRecord) { Text("Record measurement", color = MaterialTheme.colorScheme.primary) }
        ArchiField(query, { query = it }, "Search sheets", Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("ALL") + SheetStatus.ALL).forEach { status ->
                val count = if (status == "ALL") sheets.size else sheets.count { it.status == status }
                ChoiceChip("$status $count", filter == status) { filter = status }
            }
        }
        if (notice != null) {
            Text(notice ?: "", color = MaterialTheme.colorScheme.error, modifier = Modifier.announce())
            TextButton(onClick = book::clearNotice) { Text("Dismiss notice") }
        }
        visible.forEach { sheet ->
            val rows = measurements.filter { it.sheetId == sheet.id }
            val total = rows.sumOf { it.quantity }
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sheet.sheetCode, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                    Text(sheet.itemNameSnapshot, style = MaterialTheme.typography.titleMedium)
                    Text("${sheet.contractorNameSnapshot} · ${sheet.floorNameSnapshot}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    Text(statusLabel(sheet), color = statusColor(sheet.status), style = MaterialTheme.typography.labelLarge)
                    Text(String.format(Locale.US, "%.2f %s", total, sheet.uomSnapshot), fontFamily = FontFamily.Monospace)
                    Row {
                        TextButton(onClick = { openId = if (openId == sheet.id) null else sheet.id }) {
                            Text(if (openId == sheet.id) "Hide rows of ${sheet.sheetCode}" else "Rows of ${sheet.sheetCode}")
                        }
                        TextButton(onClick = {
                            val projectId = project?.id ?: return@TextButton
                            book.export(projectId, sheet.id) { name, csv -> shareCsv(context, name, csv) }
                        }) { Text("Share CSV of ${sheet.sheetCode}") }
                    }
                    if (openId == sheet.id) {
                        rows.forEach { row -> RowLine(row, sheet, book) }
                        MeasurementSheetWorkflow.allowedTransitions(sheet.status).forEach { next ->
                            TextButton(onClick = {
                                if (next == SheetStatus.RETURNED) returnId = sheet.id else book.transition(sheet.id, next, "")
                            }) { Text("Set ${sheet.sheetCode} to $next") }
                        }
                    }
                }
            }
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
private fun RowLine(row: MeasurementEntity, sheet: MeasurementSheetEntity, book: MeasurementBookViewModel) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(row.description, style = MaterialTheme.typography.bodyMedium)
        Text(
            "${row.location} · L ${row.length} · B ${row.width} · H ${row.height} · Nos ${row.nos} · Ded ${row.deduction}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(String.format(Locale.US, "%.2f %s", row.quantity, row.unit), fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        if (sheet.status == SheetStatus.DRAFT || sheet.status == SheetStatus.RETURNED) {
            TextButton(onClick = { book.deleteRow(row.id) }) { Text("Remove ${row.description}") }
        }
    }
}

private fun statusLabel(sheet: MeasurementSheetEntity) = sheet.status

@Composable
private fun statusColor(status: String) = when (status) {
    SheetStatus.APPROVED -> MaterialTheme.colorScheme.tertiary
    SheetStatus.RETURNED -> MaterialTheme.colorScheme.error
    SheetStatus.SUBMITTED, SheetStatus.CHECKED -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
