@file:OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.activity.compose.BackHandler

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.local.entity.ClientEntity
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.ui.shell.PracticeDestination
import com.holagundi.archiman.ui.shell.PracticeScaffold
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.ArchiCard
import com.holagundi.archiman.data.DemoCompanyPackage
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.announce
import com.holagundi.archiman.ui.theme.ChoiceChip
import com.holagundi.archiman.ui.theme.GlassSurface

private enum class PracticeRegistry(val labelRes: Int, val icon: ImageVector, val description: String, val category: String) {
    PROFILE(R.string.practice_profile, CarbonIcons.Business, "Practice identity and letterhead", "Your practice"),
    CLIENTS(R.string.nav_clients, CarbonIcons.People, "Client contacts and project relationships", "Your practice"),
    INVOICES(R.string.nav_invoices, CarbonIcons.ReceiptLong, "Client invoices across all projects", "Finance"),
    FINANCE(R.string.finance_title, CarbonIcons.ReceiptLong, "GST settings, bank imports and TDS reconciliation", "Finance"),
    BACKUP(R.string.backup_title, CarbonIcons.Backup, "Save your records or recover a practice", "Data and access"),
    PORTAL(R.string.practice_portal, CarbonIcons.Wifi, "Company letterhead, item revisions and documents on local Wi-Fi", "Data and access"),
    DEMO(R.string.nav_demo, CarbonIcons.HelpOutline, "Explore the sample company", "Help and sample data"),
}

@Composable
fun PortfolioScreen(
    onOpenProject: (Long) -> Unit,
    onSignOut: () -> Unit,
    onOpenPage: (Long, WorkspacePage) -> Unit = { id, _ -> onOpenProject(id) },
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit = { id, page, _ -> onOpenPage(id, page) },
    viewModel: PortfolioViewModel = hiltViewModel(),
) {
    val operationNotice by viewModel.operations.notice.collectAsStateWithLifecycle()
    val pending by viewModel.operations.pending.collectAsStateWithLifecycle()
    val company by viewModel.company.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val demoCatalog by viewModel.demoCatalog.collectAsStateWithLifecycle()
    val demoNotice by viewModel.demoNotice.collectAsStateWithLifecycle()
    val openingDemo by viewModel.openingDemo.collectAsStateWithLifecycle()
    var inbox by rememberSaveable { mutableStateOf<String?>(null) }
    var destination by rememberSaveable { mutableStateOf(PracticeDestination.HOME) }
    var confirmDemo by rememberSaveable { mutableStateOf(false) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var pickProjectFor by rememberSaveable { mutableStateOf<String?>(null) }
    var practicePage by rememberSaveable { mutableStateOf<PracticeRegistry?>(null) }
    val currentProjects = projects.filter { it.status != "ARCHIVED" }

    PracticeScaffold(
        destination = destination,
        onDestinationChange = {
            if (it == destination && it == PracticeDestination.PRACTICE) practicePage = null
            inbox = null
            destination = it
        },
        onSearch = { showSearch = true },
        createActions = listOf(
            com.holagundi.archiman.ui.shell.PracticeFabAction(
                stringResource(R.string.new_project),
                CarbonIcons.Apartment,
            ) { showCreate = true },
            com.holagundi.archiman.ui.shell.PracticeFabAction(
                stringResource(R.string.create_task),
                CarbonIcons.Assignment,
            ) { pickProjectFor = "tasks" },
            com.holagundi.archiman.ui.shell.PracticeFabAction(
                stringResource(R.string.create_site_issue),
                CarbonIcons.ReportProblem,
            ) { pickProjectFor = "issues" },
            com.holagundi.archiman.ui.shell.PracticeFabAction(
                stringResource(R.string.create_invoice),
                CarbonIcons.ReceiptLong,
            ) {
                practicePage = PracticeRegistry.INVOICES
                destination = PracticeDestination.PRACTICE
            },
        ),
        onSignOut = onSignOut,
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            operationNotice?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification("Practice update", it.message, it.kind); TextButton(onClick = viewModel.operations::dismiss) { Text("Dismiss update") } }
            if (pending.isNotEmpty()) Text("Saving…")
            Box(Modifier.weight(1f)) {
            if (inbox != null) AttentionInbox(viewModel, inbox!!, { inbox = null }, onOpenRecord)
            else when (destination) {
                PracticeDestination.HOME -> PracticeDashboard(
                    viewModel = viewModel,
                    onNewProject = { showCreate = true },
                    onProjects = { destination = PracticeDestination.PROJECTS },
                    onInvoices = { practicePage = PracticeRegistry.INVOICES; destination = PracticeDestination.PRACTICE },
                    onOpenProject = onOpenProject,
                    onOpenPage = onOpenPage,
                    onOpenRecord = onOpenRecord,
                    onAttention = { inbox = it },
                )
                PracticeDestination.PROJECTS -> ProjectList(projects, onOpenProject)
                PracticeDestination.PRACTICE -> PracticeRegistries(
                    company, clients, invoices, projects, viewModel, demoCatalog, demoNotice, openingDemo,
                    onOpenDemo = { confirmDemo = true },
                    onRestored = onSignOut,
                    page = practicePage,
                    onPageChange = { practicePage = it },
                )
            }
        }
    }

    }

    if (confirmDemo) {
        Dialog(onDismissRequest = { confirmDemo = false }) {
            GlassSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.demo_confirm_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(stringResource(R.string.demo_confirm_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { confirmDemo = false }) { Text(stringResource(R.string.cancel)) }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            confirmDemo = false
                            viewModel.openBuiltInDemo()
                        }) { Text(stringResource(R.string.demo_open)) }
                    }
                }
            }
        }
    }

    if (showSearch) {
        PracticeSearchSheet(
            viewModel = viewModel,
            onDismiss = { showSearch = false },
            onOpenProject = {
                showSearch = false
                onOpenProject(it)
            },
            onOpenRecord = { projectId, page, recordId ->
                showSearch = false
                onOpenRecord(projectId, page, recordId)
            },
            onOpenPracticeInvoices = {
                showSearch = false
                practicePage = PracticeRegistry.INVOICES
                destination = PracticeDestination.PRACTICE
            },
        )
    }

    if (pickProjectFor != null) {
        ProjectPickSheet(
            projects = currentProjects,
            title = if (pickProjectFor == "issues") stringResource(R.string.pick_project_for_issue) else stringResource(R.string.pick_project_for_task),
            onDismiss = { pickProjectFor = null },
            onPick = { projectId ->
                val page = if (pickProjectFor == "issues") WorkspacePage.SNAGS else WorkspacePage.TASKS
                pickProjectFor = null
                onOpenPage(projectId, page)
            },
        )
    }

    if (showCreate) {
        CreateProjectDialog(
            busy = "createProject" in pending,
            saveNotice = operationNotice,
            architect = company?.principalName.orEmpty(),
            onDismiss = { showCreate = false },
            onCreate = { name, code, client, type, site ->
                viewModel.createProject(name, code, client, type, site).onSuccess {
                    showCreate = false
                    destination = PracticeDestination.PROJECTS
                }
            },
        )
    }
}

@Composable
private fun ProjectList(projects: List<ProjectEntity>, onOpen: (Long) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("Active") }
    val visible = projects.filter { project ->
        filter == "All" || (project.status == "ARCHIVED") == (filter == "Archived")
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Active", "Archived", "All").forEach { option -> ChoiceChip(option, filter == option) { filter = option } }
            }
        }
        if (visible.isEmpty()) {
            item { Text(stringResource(R.string.no_projects), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(visible, key = { it.id }) { project ->
            ProjectCard(project, onOpen)
        }
    }
}

@Composable
internal fun ProjectCard(project: ProjectEntity, onOpen: (Long) -> Unit) {
    ArchiCard(onClick = { onOpen(project.id) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                CarbonIcons.Apartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(Modifier.weight(1f).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(project.projectCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOf(project.client, project.siteLocation).filter { it.isNotBlank() }.joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    project.status.lowercase().replaceFirstChar { it.uppercase() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            RecordActions(RecordKind.PROJECT, project.id, project.name)
        }
    }
}

@Composable
private fun PracticeRegistries(
    company: CompanyProfileEntity?,
    clients: List<ClientEntity>,
    invoices: List<InvoiceEntity>,
    projects: List<ProjectEntity>,
    viewModel: PortfolioViewModel,
    demoCatalog: DemoCompanyPackage?,
    demoNotice: String?,
    openingDemo: Boolean,
    onOpenDemo: () -> Unit,
    onRestored: () -> Unit,
    page: PracticeRegistry?,
    onPageChange: (PracticeRegistry?) -> Unit,
) {
    BackHandler(enabled = page != null) { onPageChange(null) }
    Column(Modifier.fillMaxSize()) {
        if (page != null) TextButton(onClick = { onPageChange(null) }) {
            Icon(CarbonIcons.ArrowBack, null)
            Spacer(Modifier.width(8.dp))
            Text("Practice menu")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (page) {
                null -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)) {
                    item { com.holagundi.archiman.ui.theme.AppearanceCard() }
                    PracticeRegistry.entries.groupBy { it.category }.forEach { (category, registries) ->
                        item { Text(category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
                        items(registries, key = { it.name }) { registry ->
                            FeatureRow(stringResource(registry.labelRes), registry.description, registry.icon) { onPageChange(registry) }
                        }
                    }
                }
                PracticeRegistry.PROFILE -> OfficePane(company)
                PracticeRegistry.CLIENTS -> ClientsPane(clients, viewModel)
                PracticeRegistry.INVOICES -> AccountsPane(invoices, projects)
                PracticeRegistry.FINANCE -> FinancePane()
                PracticeRegistry.DEMO -> DemoPane(demoCatalog, demoNotice, openingDemo, onOpenDemo)
                PracticeRegistry.PORTAL -> PortalPane()
                PracticeRegistry.BACKUP -> BackupPane(onRestored)
            }
        }
    }
}

@Composable
private fun DemoPane(
    catalog: DemoCompanyPackage?,
    notice: String?,
    opening: Boolean,
    onOpen: () -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.demo_heading), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(stringResource(R.string.demo_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (catalog == null) {
            Text(stringResource(R.string.demo_missing), color = MaterialTheme.colorScheme.error, modifier = Modifier.announce())
        } else {
            Text(catalog.practiceName, style = MaterialTheme.typography.titleMedium)
            Text("Schema ${catalog.schemaVersion}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            catalog.index.forEach { entry ->
                ListItem(
                    headlineContent = { Text(entry.label) },
                    supportingContent = { Text(entry.count.toString()) },
                    leadingContent = { Icon(CarbonIcons.FolderOpen, contentDescription = null) },
                )
            }
            Button(onClick = onOpen, enabled = !opening) {
                Text(stringResource(if (opening) R.string.demo_opening else R.string.demo_open))
            }
        }
        if (notice != null) {
            Text(notice, color = MaterialTheme.colorScheme.primary, modifier = Modifier.announce())
        }
    }
}

@Composable
private fun ClientsPane(
    clients: List<com.holagundi.archiman.data.local.entity.ClientEntity>,
    viewModel: PortfolioViewModel,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Clients", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        clients.forEach { client ->
            Text(client.name, style = MaterialTheme.typography.titleMedium)
            Text("${client.clientType} · ${client.contactNo.ifBlank { "No phone" }} · ${client.email}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            RecordActions(RecordKind.CLIENT, client.id, client.name)
        }
        ArchiField(name, { name = it }, "Client name")
        ArchiField(phone, { phone = it }, "Phone")
        ArchiField(email, { email = it }, "Email")
        TextButton(onClick = {
            val submitted = listOf(name, phone, email); viewModel.addClient(name, phone, email).onSuccess { if (listOf(name, phone, email) == submitted) { name = ""; phone = ""; email = "" } }
        }) { Text("Save client", color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun AccountsPane(
    invoices: List<com.holagundi.archiman.data.local.entity.InvoiceEntity>,
    projects: List<ProjectEntity>,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Invoices", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text("Fee invoices raised from a project. Open the project to issue one.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (invoices.isEmpty()) Text("No invoices yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        invoices.forEach { invoice ->
            val project = projects.find { it.id == invoice.projectId }?.name ?: "Project"
            Text("${invoice.number} · $project", style = MaterialTheme.typography.titleMedium)
            Text("${invoice.title} · ${invoice.status} · ${invoice.amount}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            RecordActions(RecordKind.INVOICE, invoice.id, invoice.number)
        }
    }
}

@Composable
private fun OfficePane(
    company: com.holagundi.archiman.data.local.entity.CompanyProfileEntity?,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (company == null) {
            Text("Opening the practice…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        Text(company.practiceName, style = MaterialTheme.typography.headlineMedium)
        RecordActions(RecordKind.PRACTICE, company.id, company.practiceName)
        Text(company.legalName, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${company.address}, ${company.city} ${company.pinCode}")
        Text("CoA ${company.coaRegistrationNumber}", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
        Text(company.principalName, style = MaterialTheme.typography.titleMedium)
        Text(company.principalQualification, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("GSTIN ${company.gstin}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text("This block is the letterhead used on letters and invoices.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CreateProjectDialog(
    busy: Boolean = false,
    saveNotice: UiNotice? = null,
    architect: String,
    onDismiss: () -> Unit,
    onCreate: (name: String, code: String, client: String, type: String, site: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var client by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf("Residential") }
    var site by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = androidx.compose.ui.window.DialogProperties(decorFitsSystemWindows = false)) {
        androidx.compose.foundation.layout.Box(Modifier.imePadding().safeDrawingPadding()) {
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                saveNotice?.takeIf { it.kind == com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR }?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification("Could not create project", it.message, it.kind) }
                Text("New project", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                Text("A Ground Floor is added so measurement can start.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                ArchiField(name, { name = it }, "Project name (required)", Modifier.fillMaxWidth(), enabled = !busy)
                ArchiField(code, { code = it }, "Code (optional)", Modifier.fillMaxWidth(), enabled = !busy)
                ArchiField(client, { client = it }, "Client (required)", Modifier.fillMaxWidth(), enabled = !busy)
                ArchiField(site, { site = it }, "Site address", Modifier.fillMaxWidth(), enabled = !busy)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Residential", "Commercial", "Institutional", "Interior").forEach { option ->
                        ChoiceChip(option, type == option) { type = option }
                    }
                }
                Text("Architect: ${architect.ifBlank { "Practice principal" }}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !busy && name.isNotBlank() && client.isNotBlank(),
                        onClick = { if (name.isNotBlank() && client.isNotBlank()) onCreate(name, code, client, type, site) },
                    ) { Text(if (busy) "Creating…" else "Create") }
                }
            }
        }
        }
    }
}

@Composable
private fun PracticeSearchSheet(
    viewModel: PortfolioViewModel,
    onDismiss: () -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit,
    onOpenPracticeInvoices: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val tasks by viewModel.dashboardTasks.collectAsStateWithLifecycle()
    val reviews by viewModel.dashboardReviews.collectAsStateWithLifecycle()
    val issues by viewModel.dashboardIssues.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val q = query.trim()
    val projectHits = projects.filter { project ->
        q.isBlank() || listOf(project.name, project.projectCode, project.client, project.siteLocation).any { it.contains(q, true) }
    }.take(8)
    val taskHits = tasks.filter { q.isBlank() || it.title.contains(q, true) || it.projectName.contains(q, true) }.take(6)
    val reviewHits = reviews.filter { q.isBlank() || it.sheetCode.contains(q, true) || it.projectName.contains(q, true) }.take(6)
    val issueHits = issues.filter { q.isBlank() || it.title.contains(q, true) || it.projectName.contains(q, true) }.take(6)
    val invoiceHits = invoices.filter { q.isBlank() || it.number.contains(q, true) || it.title.contains(q, true) }.take(6)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.practice_search), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            ArchiField(query, { query = it }, stringResource(R.string.practice_search_hint), Modifier.fillMaxWidth())
            LazyColumn(Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (projectHits.isNotEmpty()) {
                    item { Text("Projects", style = MaterialTheme.typography.titleMedium) }
                    items(projectHits, key = { "p-${it.id}" }) { project ->
                        FeatureRow(project.name, listOf(project.projectCode, project.client).filter { it.isNotBlank() }.joinToString(" · "), CarbonIcons.Apartment) {
                            onOpenProject(project.id)
                        }
                    }
                }
                if (taskHits.isNotEmpty()) {
                    item { Text("Tasks", style = MaterialTheme.typography.titleMedium) }
                    items(taskHits, key = { "t-${it.id}" }) { task ->
                        FeatureRow(task.title, task.projectName, CarbonIcons.Assignment) {
                            onOpenRecord(task.projectId, WorkspacePage.TASKS, task.id)
                        }
                    }
                }
                if (reviewHits.isNotEmpty()) {
                    item { Text("Sheets to review", style = MaterialTheme.typography.titleMedium) }
                    items(reviewHits, key = { "r-${it.id}" }) { review ->
                        FeatureRow(review.sheetCode, review.projectName, CarbonIcons.FactCheck) {
                            onOpenRecord(review.projectId, WorkspacePage.BOOK, review.id)
                        }
                    }
                }
                if (issueHits.isNotEmpty()) {
                    item { Text("Site issues", style = MaterialTheme.typography.titleMedium) }
                    items(issueHits, key = { "i-${it.id}" }) { issue ->
                        FeatureRow(issue.title, issue.projectName, CarbonIcons.ReportProblem) {
                            onOpenRecord(issue.projectId, WorkspacePage.SNAGS, issue.id)
                        }
                    }
                }
                if (invoiceHits.isNotEmpty()) {
                    item { Text("Invoices", style = MaterialTheme.typography.titleMedium) }
                    items(invoiceHits, key = { "inv-${it.id}" }) { invoice ->
                        FeatureRow(invoice.number, invoice.title, CarbonIcons.ReceiptLong, onOpenPracticeInvoices)
                    }
                }
                if (projectHits.isEmpty() && taskHits.isEmpty() && reviewHits.isEmpty() && issueHits.isEmpty() && invoiceHits.isEmpty()) {
                    item { Text("No matches.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
    }
}

@Composable
private fun ProjectPickSheet(
    projects: List<ProjectEntity>,
    title: String,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            if (projects.isEmpty()) {
                Text(stringResource(R.string.no_projects), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(projects, key = { it.id }) { project ->
                        FeatureRow(project.name, project.client, CarbonIcons.Apartment) { onPick(project.id) }
                    }
                }
            }
        }
    }
}
