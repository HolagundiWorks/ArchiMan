package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.domain.SiteIssueWorkflow
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.CarbonContentSwitcher
import com.holagundi.archiman.ui.theme.CarbonDropdown
import com.holagundi.archiman.ui.theme.CarbonDropdownOption
import com.holagundi.archiman.ui.theme.CarbonTag
import com.holagundi.archiman.ui.theme.CarbonTile
import com.holagundi.archiman.ui.theme.ScreenTitle

private enum class DrawingPanel { REGISTER, REVISIONS, TRANSMITTALS }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawingsPane(suite: SuiteProjectViewModel) {
    val drawings by suite.drawings.collectAsStateWithLifecycle()
    val revisions by suite.revisions.collectAsStateWithLifecycle()
    val transmittals by suite.transmittals.collectAsStateWithLifecycle()
    var panel by rememberSaveable { mutableStateOf(DrawingPanel.REGISTER) }
    var number by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var discipline by rememberSaveable { mutableStateOf("Architectural") }
    var packageTag by rememberSaveable { mutableStateOf("HFC") }
    var drawingId by rememberSaveable { mutableStateOf(-1L) }
    var revisionCode by rememberSaveable { mutableStateOf("A") }
    var notes by rememberSaveable { mutableStateOf("") }
    var asBuilt by rememberSaveable { mutableStateOf(false) }
    var severity by rememberSaveable { mutableStateOf(SiteIssueWorkflow.SEVERITY_NORMAL) }
    var fileNote by rememberSaveable { mutableStateOf("") }
    var subject by rememberSaveable { mutableStateOf("") }
    var recipients by rememberSaveable { mutableStateOf("") }
    var purpose by rememberSaveable { mutableStateOf("For construction") }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(com.holagundi.archiman.ui.theme.CarbonSpacing.stack),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.drawings_heading)) }
        item {
            Text(stringResource(R.string.drawings_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            val panels = DrawingPanel.entries
            CarbonContentSwitcher(
                options = listOf(
                    stringResource(R.string.drawings_register),
                    stringResource(R.string.drawings_revisions),
                    stringResource(R.string.drawings_transmittals),
                ),
                selectedIndex = panels.indexOf(panel).coerceAtLeast(0),
                onSelected = { panel = panels[it] },
            )
        }

        when (panel) {
            DrawingPanel.REGISTER -> {
                items(drawings, key = { "d-${it.id}" }) { drawing ->
                    CarbonTile(Modifier.fillMaxWidth()) {
                        Text(drawing.drawingNumber, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text(drawing.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text("${drawing.discipline} · ${drawing.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Package: ${drawing.packageTag.ifBlank { "—" }}")
                        RecordActions(RecordKind.DRAWING, drawing.id, drawing.drawingNumber)
                    }
                }
                item {
                    CarbonTile(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArchiField(number, { number = it }, stringResource(R.string.drawing_number))
                            ArchiField(title, { title = it }, stringResource(R.string.drawing_title))
                            ArchiField(discipline, { discipline = it }, stringResource(R.string.drawing_discipline))
                            ArchiField(packageTag, { packageTag = it }, stringResource(R.string.drawing_package))
                            Button(
                                onClick = {
                                    val submitted = listOf(number, title); suite.addDrawing(number, title, discipline, packageTag).onSuccess { if (listOf(number, title) == submitted) { number = ""; title = "" } }
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.drawing_save)) }
                        }
                    }
                }
            }
            DrawingPanel.REVISIONS -> {
                items(revisions, key = { "r-${it.id}" }) { revision ->
                    val drawing = drawings.firstOrNull { it.id == revision.drawingId }
                    CarbonTile(Modifier.fillMaxWidth()) {
                        Text("${drawing?.drawingNumber ?: "Drawing"} · ${revision.revisionCode}", style = MaterialTheme.typography.titleMedium)
                        Text(revision.revisionNotes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RecordActions(RecordKind.REVISION, revision.id, revision.revisionCode)
                        Text(
                            buildString {
                                append(revision.issueStatus)
                                if (revision.isAsBuilt) append(" · As-built")
                                append(" · ${SiteIssueWorkflow.severityLabel(revision.severity)}")
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                item {
                    CarbonTile(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CarbonDropdown(
                                label = "Drawing",
                                options = drawings.map { CarbonDropdownOption(it.id.toString(), it.drawingNumber) },
                                selectedId = if (drawingId > 0L) drawingId.toString() else null,
                                onSelected = { drawingId = it.toLongOrNull() ?: -1L },
                            )
                            ArchiField(revisionCode, { revisionCode = it }, stringResource(R.string.revision_code))
                            ArchiField(notes, { notes = it }, stringResource(R.string.revision_notes), singleLine = false)
                            ArchiField(fileNote, { fileNote = it }, stringResource(R.string.snag_evidence))
                            CarbonTag(
                                label = stringResource(R.string.revision_as_built),
                                selected = asBuilt,
                                onClick = { asBuilt = !asBuilt },
                            )
                            CarbonDropdown(
                                label = stringResource(R.string.snag_severity),
                                options = SiteIssueWorkflow.severities.map {
                                    CarbonDropdownOption(it, SiteIssueWorkflow.severityLabel(it))
                                },
                                selectedId = severity,
                                onSelected = { severity = it },
                            )
                            Button(
                                onClick = {
                                    if (drawingId > 0L) {
                                        val submitted = listOf(notes); suite.addRevision(drawingId, revisionCode, notes, asBuilt, severity, fileNote).onSuccess { if (listOf(notes) == submitted) { notes = "" } }
                                    }
                                },
                                enabled = drawingId > 0L,
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.revision_save)) }
                        }
                    }
                }
            }
            DrawingPanel.TRANSMITTALS -> {
                items(transmittals, key = { "t-${it.id}" }) { item ->
                    CarbonTile(Modifier.fillMaxWidth()) {
                        Text(item.transmittalNumber, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text(item.subject, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        Text("${item.purpose} · ${item.recipients}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        RecordActions(RecordKind.TRANSMITTAL, item.id, item.transmittalNumber)
                    }
                }
                item {
                    CarbonTile(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArchiField(subject, { subject = it }, stringResource(R.string.transmittal_subject))
                            ArchiField(recipients, { recipients = it }, stringResource(R.string.transmittal_recipients))
                            ArchiField(purpose, { purpose = it }, stringResource(R.string.transmittal_purpose))
                            Button(
                                onClick = {
                                    val submitted = listOf(subject); suite.addTransmittal(subject, recipients, purpose).onSuccess { if (listOf(subject) == submitted) { subject = "" } }
                                },
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) { Text(stringResource(R.string.transmittal_save)) }
                        }
                    }
                }
            }
        }
    }
}
