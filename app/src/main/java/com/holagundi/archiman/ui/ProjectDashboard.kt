package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonTonalButton as FilledTonalButton

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.holagundi.archiman.ui.theme.ArchiCard
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.domain.InvoiceBalance
import com.holagundi.archiman.ui.shell.ProjectGroup
import com.holagundi.archiman.ui.shell.WorkspacePage
import java.math.BigDecimal

@Composable
fun FeatureRow(title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    ArchiCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(description) },
            leadingContent = {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer, shape = MaterialTheme.shapes.small) {
                    Icon(icon, null, Modifier.padding(10.dp).size(24.dp))
                }
            },
            trailingContent = { Icon(CarbonIcons.ArrowForward, null) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}

@Composable
fun ProjectSectionHub(group: ProjectGroup, parent: WorkspacePage? = null, onOpen: (WorkspacePage) -> Unit) {
    val pages = WorkspacePage.entries.filter { it.group == group && it.parent == parent && it != WorkspacePage.OVERVIEW }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item { Text(stringResource(parent?.labelRes ?: group.labelRes), style = MaterialTheme.typography.headlineSmall) }
        items(pages, key = { it.name }) { page ->
            FeatureRow(stringResource(page.labelRes), pageDescription(page), page.icon) { onOpen(page) }
        }
    }
}

private fun pageDescription(page: WorkspacePage): String = when (page) {
    WorkspacePage.BRIEF -> "Objectives, requirements and site constraints"
    WorkspacePage.TASKS -> "Plan work, track progress and reopen tasks"
    WorkspacePage.DOCUMENTS -> "Drawing register, revisions, transmittals and letters"
    WorkspacePage.LETTERS -> "Draft correspondence using your practice letterhead"
    WorkspacePage.DRAWINGS -> "Register drawings and maintain revision history"
    WorkspacePage.VISITS -> "Inspection notes and follow-up observations"
    WorkspacePage.SNAGS -> "Track defects, corrections and verification"
    WorkspacePage.COORDINATION -> "RFIs, submittals and site instructions"
    WorkspacePage.SITEMAN -> "Daily logs, material orders and site tools"
    WorkspacePage.MEASUREMENTS -> "Capture quantities, review sheets and manage spaces"
    WorkspacePage.MEASURE -> "Record dimensions with a live quantity calculation"
    WorkspacePage.BOOK -> "Review measurement sheets and export quantities"
    WorkspacePage.LEVELS -> "Organize the project's levels and rooms"
    WorkspacePage.PROPOSAL -> "Draft your architectural fee proposal"
    WorkspacePage.CONTRACT -> "Agreements and parties on this project"
    WorkspacePage.INVOICE -> "Client fees, receipts and outstanding balances"
    WorkspacePage.BILL -> "Approved quantities and project-specific contractor billing"
    WorkspacePage.SETUP -> "Contractors, assigned works, active revision and next steps"
    else -> "Project summary and next actions"
}

@Composable
fun ProjectDashboard(project: ProjectEntity, sheetCount: Int, suite: SuiteProjectViewModel, onOpen: (WorkspacePage) -> Unit) {
    val tasks by suite.tasks.collectAsStateWithLifecycle()
    val issues by suite.issues.collectAsStateWithLifecycle()
    val invoices by suite.invoices.collectAsStateWithLifecycle()
    val receipts by suite.receipts.collectAsStateWithLifecycle()
    val openTasks = tasks.count { it.status !in listOf("DONE", "COMPLETED") }
    val openIssues = issues.count { it.status != "CLOSED" }
    var feeSummary by remember(invoices,receipts){ mutableStateOf<com.holagundi.archiman.data.FeeDashboardSummary?>(null) }
    LaunchedEffect(invoices,receipts) {
        feeSummary=withContext(Dispatchers.Default){com.holagundi.archiman.data.FeeDashboardSummary.calculate(invoices,receipts)}
    }
    val outstanding=feeSummary?.outstanding
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Project overview", style = MaterialTheme.typography.labelLarge)
                    Text(project.client, style = MaterialTheme.typography.headlineSmall)
                    if (project.siteLocation.isNotBlank()) Text(project.siteLocation)
                    Text("${project.projectType} · ${project.status.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item { com.holagundi.archiman.ui.theme.CarbonFeeTile(feeSummary) { onOpen(WorkspacePage.INVOICE) } }
        item { Text("Continue your work", style = MaterialTheme.typography.titleMedium) }
        item { FeatureRow("Measurement and billing setup", "Check prerequisites and continue to capture, review or bill", WorkspacePage.SETUP.icon) { onOpen(WorkspacePage.SETUP) } }
        item { FeatureRow("Tasks", "$openTasks open tasks", WorkspacePage.TASKS.icon) { onOpen(WorkspacePage.TASKS) } }
        item { FeatureRow("Site issues", "$openIssues issues awaiting closure", WorkspacePage.SNAGS.icon) { onOpen(WorkspacePage.SNAGS) } }
        item { FeatureRow("Measurement Book", "$sheetCount sheets · quantities and reviews", WorkspacePage.BOOK.icon) { onOpen(WorkspacePage.BOOK) } }
        item { FeatureRow("Client fees", "Outstanding: ${outstanding?.toPlainString() ?: "Loading…"}", WorkspacePage.INVOICE.icon) { onOpen(WorkspacePage.INVOICE) } }
        item {
            FilledTonalButton(onClick = { onOpen(WorkspacePage.MEASURE) }, modifier = Modifier.fillMaxWidth()) {
                Icon(CarbonIcons.Straighten, null)
                Spacer(Modifier.width(8.dp))
                Text("Record measurement")
            }
        }
    }
}
