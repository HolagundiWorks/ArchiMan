package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton
import com.holagundi.archiman.ui.theme.ArchiCard
import com.holagundi.archiman.ui.theme.CarbonInlineNotification
import com.holagundi.archiman.ui.theme.CarbonNoticeKind

/** Explicit, project-scoped handoff; the access code is never embedded in a URL. */
@Composable
fun ProjectPortalLink(projectId: Long, section: String, label: String, portal: PortalViewModel = hiltViewModel()) {
    val state by portal.state.collectAsStateWithLifecycle()
    val uri = LocalUriHandler.current
    var pairing by rememberSaveable { mutableStateOf(false) }
    val address = state.address?.trimEnd('/')?.let { "$it/workspace?section=$section&id=$projectId" }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Use the web portal for desktop setup, imports and contractor bill preparation. Your selected project is included in this link.", style = MaterialTheme.typography.bodyMedium)
        if (state.running && address != null) {
            SelectionContainer { Text(address) }
            Button(onClick = { uri.openUri(address) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
            Text("On another device, open this address and pair using the phone's access code. Verify the certificate fingerprint first.", style = MaterialTheme.typography.bodySmall)
        }
        TextButton(onClick = { pairing = !pairing }) { Text(if (pairing) "Hide portal connection details" else "Set up / view portal connection") }
        if (pairing) Box(Modifier.fillMaxWidth().heightIn(min = 400.dp, max = 650.dp)) { PortalPane(portal) }
    }
}

@Composable
fun RunningBillsPane(suite: SuiteProjectViewModel, onReview: () -> Unit, onSetup: () -> Unit) {
    val rows by suite.approvedMeasurements.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Contractor running bills", style = MaterialTheme.typography.headlineSmall)
        CarbonInlineNotification("Excluding taxes", "Contractor bills use approved quantities and each entry's negotiated revision/rate snapshot. GST and TDS for your professional fees are managed separately.", CarbonNoticeKind.INFO)
        Text("${rows.size} approved measurement entries", style = MaterialTheme.typography.titleMedium)
        Text("This summary includes previously billed quantities. The portal checks issued bills and selects only eligible unbilled entries before preparing a draft.")
        rows.groupBy { it.contractorName }.forEach { (name, entries) ->
            ArchiCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text("${entries.size} approved entries · ${entries.map { it.boqRevision.ifBlank { "Legacy" } }.distinct().joinToString()}")
                if (entries.any { it.boqRevision.isBlank() || it.rateSnapshot <= 0 }) Text("Some legacy entries lack agreed rate snapshots and cannot be inferred into a bill.")
            } }
        }
        TextButton(onClick = onReview) { Text("Review measurement sheets") }
        TextButton(onClick = onSetup) { Text("Check contractor and revision setup") }
        ProjectPortalLink(suite.projectIdForFinance, "bills", "Open this project's running bills")
    }
}

@Composable
fun ProjectSetupPane(workspace: ProjectWorkspaceViewModel, suite: SuiteProjectViewModel, onMeasure: () -> Unit, onReview: () -> Unit, onBill: () -> Unit) {
    val project by workspace.project.collectAsStateWithLifecycle()
    val contractors by workspace.contractors.collectAsStateWithLifecycle()
    val floors by workspace.floors.collectAsStateWithLifecycle()
    val schedules by workspace.schedules.collectAsStateWithLifecycle()
    val active = schedules.firstOrNull { it.status == "ACTIVE" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Measurement and billing setup", style = MaterialTheme.typography.headlineSmall)
        listOf(
            "Client and site details" to (project?.client?.isNotBlank() == true && project?.siteLocation?.isNotBlank() == true),
            "At least one floor" to floors.isNotEmpty(),
            "Project contractors" to contractors.isNotEmpty(),
            "Active project item revision with assigned works" to (active?.lines?.isNotEmpty() == true),
        ).forEach { (label, complete) -> Text("${if (complete) "Ready" else "Required"}: $label") }
        Text("1. Confirm project/client details. 2. Add contractors. 3. Choose reference items and agree labour-only or material-inclusive rates. 4. Activate the project-wide revision. 5. Record, review and approve quantities. 6. Prepare the contractor running bill.")
        ProjectPortalLink(suite.projectIdForFinance, "schedule", "Open this project's item revisions")
        TextButton(enabled = active != null && contractors.isNotEmpty() && floors.isNotEmpty(), onClick = onMeasure) { Text("Record measurement") }
        TextButton(onClick = onReview) { Text("Review and approve sheets") }
        TextButton(onClick = onBill) { Text("Prepare contractor bill") }
    }
}
