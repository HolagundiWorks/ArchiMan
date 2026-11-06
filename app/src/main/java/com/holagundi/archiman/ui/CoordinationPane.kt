package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.domain.CoordinationWorkflow
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ChoiceChip
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.ScreenTitle
import com.holagundi.archiman.ui.theme.announce

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoordinationPane(suite: SuiteProjectViewModel) {
    val items by suite.coordination.collectAsStateWithLifecycle()
    val notice by suite.notice.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf(-1L) }
    var type by rememberSaveable { mutableStateOf(CoordinationWorkflow.RFI) }
    var subject by rememberSaveable { mutableStateOf("") }
    var discipline by rememberSaveable { mutableStateOf("Architectural") }
    var location by rememberSaveable { mutableStateOf("") }
    var question by rememberSaveable { mutableStateOf("") }
    var raisedBy by rememberSaveable { mutableStateOf("") }
    var assignedTo by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ScreenTitle(stringResource(R.string.coordination_heading))
        Text(stringResource(R.string.coordination_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (notice != null) {
            Text(notice ?: "", color = MaterialTheme.colorScheme.primary, modifier = Modifier.announce())
            TextButton(onClick = suite::clearNotice) { Text("Dismiss") }
        }
        if (items.isEmpty()) {
            Text(stringResource(R.string.coordination_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items.forEach { item ->
            val open = selectedId == item.id
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { selectedId = if (open) -1L else item.id }) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(item.referenceNumber, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            Text(item.subject, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                        }
                    }
                    Text("${item.type} · ${item.status}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    RecordActions(RecordKind.COORDINATION, item.id, item.subject)
                    if (open) {
                        Text(item.questionOrRequirement)
                        if (item.response.isNotBlank()) Text("Response: ${item.response}")
                        Text("${item.discipline} · ${item.location}", style = MaterialTheme.typography.bodySmall)
                        val next = CoordinationWorkflow.allowedNextStatuses(item.type, item.status)
                        if (next.isNotEmpty()) {
                            ArchiField(note, { note = it }, stringResource(R.string.coordination_response), singleLine = false)
                            next.forEach { status ->
                                OutlinedButton(
                                    onClick = { suite.transitionCoordination(item.id, status, note) },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                ) { Text(status) }
                            }
                        }
                    }
                }
            }
        }
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.coordination_new), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(CoordinationWorkflow.RFI, CoordinationWorkflow.SUBMITTAL, CoordinationWorkflow.SITE_INSTRUCTION).forEach { option ->
                        ChoiceChip(option, type == option) { type = option }
                    }
                }
                ArchiField(subject, { subject = it }, stringResource(R.string.coordination_subject))
                ArchiField(question, { question = it }, stringResource(R.string.coordination_question), singleLine = false)
                ArchiField(discipline, { discipline = it }, stringResource(R.string.coordination_discipline))
                ArchiField(location, { location = it }, stringResource(R.string.snag_location))
                ArchiField(raisedBy, { raisedBy = it }, "Raised by")
                ArchiField(assignedTo, { assignedTo = it }, stringResource(R.string.snag_assigned))
                Button(
                    onClick = {
                        val submitted = listOf(subject, question); suite.addCoordination(type, subject, discipline, location, question, raisedBy, assignedTo).onSuccess { if (listOf(subject, question) == submitted) { subject = ""; question = "" } }
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.coordination_save)) }
            }
        }
    }
}
