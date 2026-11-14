package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.data.local.entity.SiteIssueEntity
import com.holagundi.archiman.domain.SiteIssueWorkflow
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.CarbonDropdown
import com.holagundi.archiman.ui.theme.CarbonDropdownOption
import com.holagundi.archiman.ui.theme.CarbonTag
import com.holagundi.archiman.ui.theme.CarbonTile
import com.holagundi.archiman.ui.theme.ScreenTitle
import com.holagundi.archiman.ui.theme.priorityAlertTone
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SiteIssuesPane(suite: SuiteProjectViewModel, focusId: Long = 0L) {
    val issues by suite.issues.collectAsStateWithLifecycle()
    val operationNotice by suite.operationNotice.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable(focusId) { mutableStateOf(if (focusId > 0) focusId else -1L) }
    var type by rememberSaveable { mutableStateOf("SNAG") }
    var title by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var severity by rememberSaveable { mutableStateOf(SiteIssueWorkflow.SEVERITY_NORMAL) }
    var assignedTo by rememberSaveable { mutableStateOf("") }
    var corrective by rememberSaveable { mutableStateOf("") }
    var evidence by rememberSaveable { mutableStateOf("") }
    var verificationNote by rememberSaveable { mutableStateOf("") }
    var afterEvidence by rememberSaveable { mutableStateOf("") }

    val selected = issues.firstOrNull { it.id == selectedId }

    LaunchedEffect(operationNotice) {
        val notice = operationNotice ?: return@LaunchedEffect
        if (
            selectedId > 0L &&
            notice.kind == com.holagundi.archiman.ui.theme.CarbonNoticeKind.SUCCESS &&
            notice.message.startsWith("Site issue moved to")
        ) {
            verificationNote = ""
            afterEvidence = ""
            selectedId = -1L
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(com.holagundi.archiman.ui.theme.CarbonSpacing.stack),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.snags_heading)) }
        item {
            Text(
                stringResource(R.string.snags_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        if (issues.isEmpty()) {
            item {
                Text(
                    stringResource(R.string.snags_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(issues, key = { it.id }) { issue ->
            val open = selectedId == issue.id
            val alert = priorityAlertTone(issue.severity)
            CarbonTile(Modifier.fillMaxWidth(), alert = alert) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecordActions(RecordKind.SNAG, issue.id, issue.title)
                    TextButton(onClick = { selectedId = if (open) -1L else issue.id }) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                            Text(
                                issue.referenceNumber,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                issue.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                    }
                    CarbonTag(SiteIssueWorkflow.severityLabel(issue.severity), alert = alert)
                    Text(
                        "${issue.type} · ${SiteIssueWorkflow.statusLabel(issue.status)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(issue.location.ifBlank { "Location not set" }, style = MaterialTheme.typography.bodyMedium)
                    if (open) {
                        IssueDetail(
                            issue = issue,
                            verificationNote = verificationNote,
                            onVerificationNote = { verificationNote = it },
                            afterEvidence = afterEvidence,
                            onAfterEvidence = { afterEvidence = it },
                            onTransition = { next ->
                                suite.transitionIssue(
                                    issue.id,
                                    next,
                                    verificationNote,
                                    afterEvidence,
                                )
                            },
                        )
                    }
                }
            }
        }

        item {
            CarbonTile(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.snag_new),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    CarbonDropdown(
                        label = "Type",
                        options = listOf("SNAG", "NCR", "SAFETY").map { CarbonDropdownOption(it, it) },
                        selectedId = type,
                        onSelected = { type = it },
                    )
                    CarbonDropdown(
                        label = stringResource(R.string.snag_severity),
                        options = SiteIssueWorkflow.severities.map {
                            CarbonDropdownOption(it, SiteIssueWorkflow.severityLabel(it))
                        },
                        selectedId = severity,
                        onSelected = { severity = it },
                    )
                    ArchiField(title, { title = it }, stringResource(R.string.snag_title))
                    ArchiField(location, { location = it }, stringResource(R.string.snag_location))
                    ArchiField(
                        description,
                        { description = it },
                        stringResource(R.string.snag_description),
                        singleLine = false,
                    )
                    ArchiField(assignedTo, { assignedTo = it }, stringResource(R.string.snag_assigned))
                    ArchiField(
                        corrective,
                        { corrective = it },
                        stringResource(R.string.snag_corrective),
                        singleLine = false,
                    )
                    ArchiField(evidence, { evidence = it }, stringResource(R.string.snag_evidence))
                    TextButton(
                        onClick = {
                            val submitted = listOf(title, location, description, corrective, evidence, assignedTo); suite.addIssue(
                                type,
                                title,
                                location,
                                description,
                                severity,
                                assignedTo,
                                corrective,
                                evidence,
                            ).onSuccess { if (listOf(title, location, description, corrective, evidence, assignedTo) == submitted) { title = ""; location = ""; description = ""; corrective = ""; evidence = ""; assignedTo = "" } }
                            severity = SiteIssueWorkflow.SEVERITY_NORMAL
                            type = "SNAG"
                        },
                    ) {
                        Text(stringResource(R.string.snag_save), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueDetail(
    issue: SiteIssueEntity,
    verificationNote: String,
    onVerificationNote: (String) -> Unit,
    afterEvidence: String,
    onAfterEvidence: (String) -> Unit,
    onTransition: (String) -> Unit,
) {
    Text(issue.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (issue.assignedTo.isNotBlank()) {
        Text("Assigned: ${issue.assignedTo}", style = MaterialTheme.typography.bodySmall)
    }
    if (issue.correctiveAction.isNotBlank()) {
        Text("Corrective action: ${issue.correctiveAction}", style = MaterialTheme.typography.bodySmall)
    }
    if (!issue.evidenceUri.isNullOrBlank()) {
        Text("Evidence: ${issue.evidenceUri}", style = MaterialTheme.typography.bodySmall)
    }
    if (issue.verificationNote.isNotBlank()) {
        Text("Verification: ${issue.verificationNote}", style = MaterialTheme.typography.bodySmall)
    }
    if (!issue.afterEvidenceUri.isNullOrBlank()) {
        Text("After evidence: ${issue.afterEvidenceUri}", style = MaterialTheme.typography.bodySmall)
    }

    val next = SiteIssueWorkflow.allowedNextStatuses(issue.status)
    if (next.isEmpty()) return

    val closing = SiteIssueWorkflow.CLOSED in next
    if (closing) {
        Text(
            stringResource(R.string.snag_verification_required),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        ArchiField(
            verificationNote,
            onVerificationNote,
            stringResource(R.string.snag_verification),
            singleLine = false,
        )
        ArchiField(afterEvidence, onAfterEvidence, stringResource(R.string.snag_after_evidence))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        next.forEach { status ->
            val label = if (status == SiteIssueWorkflow.CLOSED) {
                stringResource(R.string.snag_close)
            } else {
                SiteIssueWorkflow.statusLabel(status)
            }
            val enabled =
                status != SiteIssueWorkflow.CLOSED || verificationNote.isNotBlank()
            if (status == SiteIssueWorkflow.CLOSED) {
                Button(
                    onClick = { onTransition(status) },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(label)
                }
            } else {
                OutlinedButton(
                    onClick = { onTransition(status) },
                    enabled = enabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Text(label)
                }
            }
        }
    }
}
