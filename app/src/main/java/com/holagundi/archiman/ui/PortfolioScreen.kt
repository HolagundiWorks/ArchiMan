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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.ui.shell.PracticeDestination
import com.holagundi.archiman.ui.shell.PracticeScaffold
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.ArchiCard
import com.holagundi.archiman.ui.theme.CarbonBreadcrumb
import com.holagundi.archiman.ui.theme.CarbonContentSwitcher
import com.holagundi.archiman.ui.theme.CarbonDropdown
import com.holagundi.archiman.ui.theme.CarbonDropdownOption
import com.holagundi.archiman.ui.theme.CarbonModal
import com.holagundi.archiman.ui.theme.CarbonSearch
import com.holagundi.archiman.ui.theme.CarbonSpacing
import com.holagundi.archiman.ui.theme.CarbonTag
import com.holagundi.archiman.ui.theme.CarbonTextInput
import com.holagundi.archiman.ui.theme.CarbonTile
import com.holagundi.archiman.data.DemoCompanyPackage
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.announce

private enum class PracticeRegistry(val labelRes: Int, val icon: ImageVector, val description: String, val category: String) {
    PROFILE(R.string.practice_profile, CarbonIcons.Business, "Practice identity and letterhead", "Your practice"),
    CLIENTS(R.string.nav_clients, CarbonIcons.People, "Client contacts and project relationships", "Your practice"),
    INVOICES(R.string.nav_invoices, CarbonIcons.ReceiptLong, "Client invoices across all projects", "Finance"),
    FINANCE(R.string.finance_title, CarbonIcons.ReceiptLong, "GST settings, bank imports and TDS reconciliation", "Finance"),
    BOOKS(R.string.books_title, CarbonIcons.MenuBook, "Trial balance, journals and manual entries", "Finance"),
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
    val demoCatalog by viewModel.demoCatalog.collectAsStateWithLifecycle()
    val demoNotice by viewModel.demoNotice.collectAsStateWithLifecycle()
    val openingDemo by viewModel.openingDemo.collectAsStateWithLifecycle()
    var inbox by rememberSaveable { mutableStateOf<String?>(null) }
    var destination by rememberSaveable { mutableStateOf(PracticeDestination.HOME) }
    var confirmDemo by rememberSaveable { mutableStateOf(false) }
    var showCreate by rememberSaveable { mutableStateOf(false) }
    var showCreateTask by rememberSaveable { mutableStateOf(false) }
    var showCreateIssue by rememberSaveable { mutableStateOf(false) }
    var showCreateInvoice by rememberSaveable { mutableStateOf(false) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var practicePage by rememberSaveable { mutableStateOf<PracticeRegistry?>(null) }
    val currentProjects = projects.filter { it.status != "ARCHIVED" }
    val openCreateTask = {
        showCreateTask = true
    }
    val openCreateInvoice = {
        when {
            currentProjects.size == 1 -> onOpenPage(currentProjects.first().id, WorkspacePage.INVOICE)
            currentProjects.isNotEmpty() -> showCreateInvoice = true
            else -> {
                practicePage = PracticeRegistry.INVOICES
                destination = PracticeDestination.PRACTICE
            }
        }
    }

    if (showSearch) {
        BackHandler {
            showSearch = false
            searchQuery = ""
        }
    }

    val goHome = {
        inbox = null
        showSearch = false
        searchQuery = ""
        practicePage = null
        destination = PracticeDestination.HOME
    }
    val goPractice = {
        inbox = null
        showSearch = false
        searchQuery = ""
        practicePage = null
        destination = PracticeDestination.PRACTICE
    }
    val breadcrumbs = when {
        showSearch -> listOf(
            CarbonBreadcrumb(stringResource(R.string.nav_home), goHome),
            CarbonBreadcrumb(stringResource(R.string.practice_search)),
        )
        inbox != null -> listOf(
            CarbonBreadcrumb(stringResource(R.string.nav_home), goHome),
            CarbonBreadcrumb(
                when (inbox) {
                    "tasks" -> "Open tasks"
                    "reviews" -> "Sheets to review"
                    else -> "Open site issues"
                },
            ),
        )
        destination == PracticeDestination.HOME -> listOf(
            CarbonBreadcrumb("Practice", goPractice),
            CarbonBreadcrumb(stringResource(R.string.nav_home)),
        )
        destination == PracticeDestination.PROJECTS -> listOf(
            CarbonBreadcrumb("Practice", goPractice),
            CarbonBreadcrumb(stringResource(R.string.nav_projects)),
        )
        destination == PracticeDestination.TASKS -> listOf(
            CarbonBreadcrumb("Practice", goPractice),
            CarbonBreadcrumb(stringResource(R.string.nav_tasks)),
        )
        destination == PracticeDestination.ISSUES -> listOf(
            CarbonBreadcrumb("Practice", goPractice),
            CarbonBreadcrumb(stringResource(R.string.nav_issues)),
        )
        destination == PracticeDestination.PRACTICE && practicePage != null -> listOf(
            CarbonBreadcrumb("Practice", goPractice),
            CarbonBreadcrumb(stringResource(practicePage!!.labelRes)),
        )
        else -> listOf(
            CarbonBreadcrumb(stringResource(R.string.nav_practice)),
        )
    }

    PracticeScaffold(
        destination = destination,
        onDestinationChange = {
            if (it == destination && it == PracticeDestination.PRACTICE) practicePage = null
            inbox = null
            showSearch = false
            searchQuery = ""
            destination = it
        },
        breadcrumbs = breadcrumbs,
        onSearch = { showSearch = true },
        searchOpen = showSearch,
        createActions = when (destination) {
            PracticeDestination.TASKS -> listOf(
                com.holagundi.archiman.ui.shell.PracticeFabAction(
                    stringResource(R.string.create_task),
                    CarbonIcons.Assignment,
                    openCreateTask,
                ),
            )
            PracticeDestination.ISSUES -> listOf(
                com.holagundi.archiman.ui.shell.PracticeFabAction(
                    stringResource(R.string.create_site_issue),
                    CarbonIcons.ReportProblem,
                ) { showCreateIssue = true },
            )
            else -> listOf(
                com.holagundi.archiman.ui.shell.PracticeFabAction(
                    stringResource(R.string.new_project),
                    CarbonIcons.Apartment,
                ) { showCreate = true },
                com.holagundi.archiman.ui.shell.PracticeFabAction(
                    stringResource(R.string.create_task),
                    CarbonIcons.Assignment,
                    openCreateTask,
                ),
                com.holagundi.archiman.ui.shell.PracticeFabAction(
                    stringResource(R.string.create_site_issue),
                    CarbonIcons.ReportProblem,
                ) { showCreateIssue = true },
                com.holagundi.archiman.ui.shell.PracticeFabAction(
                    stringResource(R.string.create_invoice),
                    CarbonIcons.ReceiptLong,
                    openCreateInvoice,
                ),
            )
        },
        onSignOut = onSignOut,
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Column(Modifier.fillMaxSize()) {
                operationNotice?.let {
                    com.holagundi.archiman.ui.theme.CarbonInlineNotification(
                        title = "Practice update",
                        body = it.message,
                        kind = it.kind,
                        onDismiss = viewModel.operations::dismiss,
                    )
                }
                if (pending.isNotEmpty()) Text("Saving…")
                Box(Modifier.weight(1f)) {
                    if (showSearch && searchQuery.isNotBlank()) {
                        PracticeSearchResults(
                            viewModel = viewModel,
                            onOpenProject = {
                                showSearch = false
                                searchQuery = ""
                                viewModel.setPracticeSearchQuery("")
                                onOpenProject(it)
                            },
                            onOpenRecord = { projectId, page, recordId ->
                                showSearch = false
                                searchQuery = ""
                                viewModel.setPracticeSearchQuery("")
                                onOpenRecord(projectId, page, recordId)
                            },
                        )
                    } else if (inbox != null) {
                        AttentionInbox(viewModel, inbox!!, { inbox = null }, onOpenRecord)
                    } else when (destination) {
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
                        PracticeDestination.TASKS -> PracticeKanban(
                            viewModel = viewModel,
                            onOpenRecord = onOpenRecord,
                        )
                        PracticeDestination.ISSUES -> PracticeIssues(
                            viewModel = viewModel,
                            onOpenRecord = onOpenRecord,
                        )
                        PracticeDestination.PRACTICE -> PracticeRegistries(
                            company, projects, viewModel, demoCatalog, demoNotice, openingDemo,
                            onOpenDemo = { confirmDemo = true },
                            onRestored = onSignOut,
                            page = practicePage,
                            onPageChange = { practicePage = it },
                            onOpenRecord = onOpenRecord,
                            onCreateInvoice = openCreateInvoice,
                        )
                    }
                }
            }
            if (showSearch) {
                PracticeFloatingSearchBar(
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                        viewModel.setPracticeSearchQuery(it)
                    },
                    onClose = {
                        showSearch = false
                        searchQuery = ""
                        viewModel.setPracticeSearchQuery("")
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 88.dp, bottom = 16.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                )
            }
        }
    }

    if (confirmDemo) {
        CarbonModal(
            title = stringResource(R.string.demo_confirm_title),
            description = stringResource(R.string.demo_confirm_body),
            onDismiss = { confirmDemo = false },
            primaryLabel = stringResource(R.string.demo_open),
            onPrimary = {
                confirmDemo = false
                viewModel.openBuiltInDemo()
            },
            secondaryLabel = stringResource(R.string.cancel),
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

    if (showCreateTask) {
        CreatePracticeTaskDialog(
            projects = currentProjects,
            preferredProjectId = currentProjects.firstOrNull()?.id ?: 0L,
            busy = "addTask" in pending,
            onDismiss = { showCreateTask = false },
            onCreate = { projectId, title, priority ->
                viewModel.addBoardTask(projectId, title, priority).onSuccess {
                    showCreateTask = false
                    destination = PracticeDestination.TASKS
                }
            },
        )
    }

    if (showCreateIssue) {
        CreatePracticeIssueDialog(
            projects = currentProjects,
            preferredProjectId = currentProjects.firstOrNull()?.id ?: 0L,
            busy = "addIssue" in pending,
            onDismiss = { showCreateIssue = false },
            onCreate = { projectId, title, description, severity ->
                viewModel.addBoardIssue(projectId, title, description, severity).onSuccess {
                    showCreateIssue = false
                    destination = PracticeDestination.ISSUES
                }
            },
        )
    }

    if (showCreateInvoice) {
        CreatePracticeInvoiceDialog(
            projects = currentProjects,
            preferredProjectId = currentProjects.firstOrNull()?.id ?: 0L,
            onDismiss = { showCreateInvoice = false },
            onOpen = { projectId ->
                showCreateInvoice = false
                onOpenPage(projectId, WorkspacePage.INVOICE)
            },
        )
    }
}

@Composable
private fun ProjectList(projects: List<ProjectEntity>, onOpen: (Long) -> Unit) {
    val filters = listOf("Active", "Archived", "All")
    var filterIndex by rememberSaveable { mutableStateOf(0) }
    val filter = filters[filterIndex.coerceIn(0, filters.lastIndex)]
    val visible = projects.filter { project ->
        filter == "All" || (project.status == "ARCHIVED") == (filter == "Archived")
    }
    LazyColumn(Modifier.fillMaxSize().padding(CarbonSpacing.screen), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack)) {
        item {
            CarbonContentSwitcher(
                options = filters,
                selectedIndex = filterIndex,
                onSelected = { filterIndex = it },
            )
        }
        if (visible.isEmpty()) {
            item { Text(stringResource(R.string.no_projects), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item {
            com.holagundi.archiman.ui.theme.CarbonTileGrid(com.holagundi.archiman.ui.theme.CarbonTileSize.LARGE) {
                visible.forEach { project ->
                    item { ProjectCard(project, onOpen) }
                }
            }
        }
    }
}

@Composable
internal fun ProjectCard(project: ProjectEntity, onOpen: (Long) -> Unit) {
    val alert = when (project.status.uppercase()) {
        "ACTIVE" -> com.holagundi.archiman.ui.theme.CarbonAlertTone.OK
        "ON_HOLD" -> com.holagundi.archiman.ui.theme.CarbonAlertTone.WARNING
        "COMPLETED" -> com.holagundi.archiman.ui.theme.CarbonAlertTone.OK
        else -> com.holagundi.archiman.ui.theme.CarbonAlertTone.NONE
    }
    CarbonTile(
        onClick = { onOpen(project.id) },
        modifier = Modifier.fillMaxWidth(),
        contentPadding = false,
        alert = alert,
        size = com.holagundi.archiman.ui.theme.CarbonTileSize.LARGE,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = CarbonSpacing.card, end = CarbonSpacing.micro, top = CarbonSpacing.stackTight, bottom = CarbonSpacing.stackTight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                CarbonIcons.Apartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = CarbonSpacing.stack),
            )
            Column(Modifier.weight(1f).padding(vertical = CarbonSpacing.micro), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline)) {
                Text(project.projectCode, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(project.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    listOf(project.client, project.siteLocation).filter { it.isNotBlank() }.joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
                CarbonTag(project.status.lowercase().replaceFirstChar { it.uppercase() }, alert = alert)
            }
            RecordActions(RecordKind.PROJECT, project.id, project.name)
        }
    }
}

@Composable
private fun PracticeRegistries(
    company: CompanyProfileEntity?,
    projects: List<ProjectEntity>,
    viewModel: PortfolioViewModel,
    demoCatalog: DemoCompanyPackage?,
    demoNotice: String?,
    openingDemo: Boolean,
    onOpenDemo: () -> Unit,
    onRestored: () -> Unit,
    page: PracticeRegistry?,
    onPageChange: (PracticeRegistry?) -> Unit,
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit,
    onCreateInvoice: () -> Unit,
) {
    BackHandler(enabled = page != null) { onPageChange(null) }
    Column(Modifier.fillMaxSize()) {
        if (page != null) TextButton(onClick = { onPageChange(null) }) {
            Icon(CarbonIcons.ArrowBack, null)
            Spacer(Modifier.width(CarbonSpacing.stackTight))
            Text("Practice menu")
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (page) {
                null -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = CarbonSpacing.screen), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = CarbonSpacing.scrollBottom)) {
                    item { com.holagundi.archiman.ui.theme.AppearanceCard() }
                    item {
                        Text("Company", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = CarbonSpacing.micro))
                        Text(
                            "Cards with ⋮ actions. Tap a section to disclose details.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = CarbonSpacing.micro),
                        )
                    }
                    item { CompanyProfilePreviewCard(company) { onPageChange(PracticeRegistry.PROFILE) } }
                    item {
                        FeatureRow(stringResource(R.string.nav_clients), "Client contacts — cards, ⋮ menu and disclosures", CarbonIcons.People) {
                            onPageChange(PracticeRegistry.CLIENTS)
                        }
                    }
                    PracticeRegistry.entries
                        .filter { it != PracticeRegistry.PROFILE && it != PracticeRegistry.CLIENTS }
                        .groupBy { it.category }
                        .forEach { (category, registries) ->
                            item { Text(category, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = CarbonSpacing.stack)) }
                            items(registries, key = { it.name }) { registry ->
                                FeatureRow(stringResource(registry.labelRes), registry.description, registry.icon) { onPageChange(registry) }
                            }
                        }
                }
                PracticeRegistry.PROFILE -> OfficePane(company)
                PracticeRegistry.CLIENTS -> ClientsPane(viewModel)
                PracticeRegistry.INVOICES -> AccountsPane(viewModel, projects, onOpenRecord, onCreateInvoice)
                PracticeRegistry.FINANCE -> FinancePane()
                PracticeRegistry.BOOKS -> LedgerPane()
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CarbonSpacing.screen), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight)) {
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
private fun ClientsPane(viewModel: PortfolioViewModel) {
    val clients by viewModel.clients.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var openIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = CarbonSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = CarbonSpacing.screen, bottom = CarbonSpacing.scrollBottom),
    ) {
        item {
            Text("Clients", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(
                "Tap a card to disclose company details. Use ⋮ to edit or delete.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = CarbonSpacing.micro),
            )
        }
        if (clients.isEmpty()) {
            item { Text("No clients yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(clients, key = { it.id }) { client ->
            val open = client.id in openIds
            val extras = remember(client.detailsJson) {
                runCatching { org.json.JSONObject(client.detailsJson.ifBlank { "{}" }) }.getOrElse { org.json.JSONObject() }
            }
            com.holagundi.archiman.ui.theme.DisclosureCard(
                title = client.name,
                summary = listOf(
                    client.clientType,
                    client.contactPerson,
                    client.contactNo.ifBlank { client.email },
                ).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "No contact details" },
                expanded = open,
                onExpandedChange = { next ->
                    openIds = if (next) openIds + client.id else openIds - client.id
                },
                leadingIcon = CarbonIcons.People,
                menu = { RecordActions(RecordKind.CLIENT, client.id, client.name) },
            ) {
                com.holagundi.archiman.ui.theme.DetailRow("Type", client.clientType)
                com.holagundi.archiman.ui.theme.DetailRow("Primary contact", client.contactPerson)
                com.holagundi.archiman.ui.theme.DetailRow("Phone", client.contactNo)
                com.holagundi.archiman.ui.theme.DetailRow("Email", client.email)
                com.holagundi.archiman.ui.theme.DetailRow("Preferred communication", client.preferredCommunication)
                com.holagundi.archiman.ui.theme.DetailRow("Billing address", client.address)
                com.holagundi.archiman.ui.theme.DetailRow("Correspondence address", client.correspondenceAddress)
                com.holagundi.archiman.ui.theme.DetailRow("Legal name", extras.optString("legalName"))
                com.holagundi.archiman.ui.theme.DetailRow("GSTIN", extras.optString("gstin"), monospace = true)
                com.holagundi.archiman.ui.theme.DetailRow("PAN", extras.optString("pan"), monospace = true)
                com.holagundi.archiman.ui.theme.DetailRow("TAN", extras.optString("tan"), monospace = true)
                com.holagundi.archiman.ui.theme.DetailRow("Notes", client.notes)
            }
        }
        item {
            ArchiCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(CarbonSpacing.card), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight)) {
                    Text("Add client", style = MaterialTheme.typography.titleMedium)
                    ArchiField(name, { name = it }, "Client name")
                    ArchiField(phone, { phone = it }, "Phone")
                    ArchiField(email, { email = it }, "Email")
                    TextButton(onClick = {
                        val submitted = listOf(name, phone, email)
                        viewModel.addClient(name, phone, email).onSuccess {
                            if (listOf(name, phone, email) == submitted) {
                                name = ""
                                phone = ""
                                email = ""
                            }
                        }
                    }) { Text("Save client", color = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
private fun AccountsPane(
    viewModel: PortfolioViewModel,
    projects: List<ProjectEntity>,
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit,
    onCreateInvoice: () -> Unit,
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().padding(CarbonSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = CarbonSpacing.scrollBottom),
    ) {
        item { Text("Invoices", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() }) }
        item {
            Text(
                "Tap an invoice to open it on the project. Use Create invoice to draft a new fee.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        item {
            Button(onClick = onCreateInvoice) { Text(stringResource(R.string.create_invoice)) }
        }
        if (invoices.isEmpty()) {
            item { Text("No invoices yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(invoices, key = { it.id }) { invoice ->
            val project = projects.find { it.id == invoice.projectId }?.name ?: "Project"
            CarbonTile(
                onClick = { onOpenRecord(invoice.projectId, WorkspacePage.INVOICE, invoice.id) },
                contentPadding = false,
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(CarbonSpacing.cardDense),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${invoice.number} · $project", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${invoice.title} · ${invoice.status} · ${invoice.amount}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        InvoiceTaxSummary(invoice.taxJson)
                    }
                    RecordActions(RecordKind.INVOICE, invoice.id, invoice.number)
                }
            }
        }
    }
}

@Composable
private fun CreatePracticeInvoiceDialog(
    projects: List<ProjectEntity>,
    preferredProjectId: Long,
    onDismiss: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    var projectId by rememberSaveable(preferredProjectId) {
        mutableStateOf(
            preferredProjectId.takeIf { id -> projects.any { it.id == id } }
                ?: projects.firstOrNull()?.id
                ?: 0L,
        )
    }
    val options = remember(projects) {
        projects.map { CarbonDropdownOption(it.id.toString(), it.name) }
    }
    CarbonModal(
        title = "New invoice",
        description = "Choose a project. You will draft the fee invoice with GST details there.",
        onDismiss = onDismiss,
        primaryLabel = "Open invoices",
        onPrimary = { onOpen(projectId) },
        primaryEnabled = projectId != 0L,
    ) {
        CarbonDropdown(
            label = "Project",
            options = options,
            selectedId = projectId.takeIf { it != 0L }?.toString(),
            onSelected = { projectId = it.toLongOrNull() ?: 0L },
            placeholder = "Choose a project",
            size = com.holagundi.archiman.ui.theme.CarbonDropdownSize.Medium,
        )
    }
}

@Composable
private fun CompanyProfilePreviewCard(company: CompanyProfileEntity?, onOpenFull: () -> Unit) {
    if (company == null) {
        ArchiCard(Modifier.fillMaxWidth()) {
            Text("Opening the practice…", Modifier.padding(CarbonSpacing.card), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    var open by rememberSaveable { mutableStateOf(true) }
    com.holagundi.archiman.ui.theme.DisclosureCard(
        title = company.practiceName,
        summary = listOf(company.legalName, company.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN $it" })
            .filter { !it.isNullOrBlank() }
            .joinToString(" · ")
            .ifBlank { "Tap to disclose company details" },
        expanded = open,
        onExpandedChange = { open = it },
        leadingIcon = CarbonIcons.Business,
        menu = { RecordActions(RecordKind.PRACTICE, company.id, company.practiceName) },
    ) {
        com.holagundi.archiman.ui.theme.DetailRow("Legal name", company.legalName)
        com.holagundi.archiman.ui.theme.DetailRow("Principal", company.principalName)
        com.holagundi.archiman.ui.theme.DetailRow("Phone", company.phone)
        com.holagundi.archiman.ui.theme.DetailRow("Email", company.email)
        com.holagundi.archiman.ui.theme.DetailRow("GSTIN", company.gstin, monospace = true)
        TextButton(onClick = onOpenFull) {
            Text("Open full company profile", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun OfficePane(
    company: CompanyProfileEntity?,
) {
    if (company == null) {
        Column(Modifier.fillMaxSize().padding(CarbonSpacing.screen)) {
            Text("Opening the practice…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    var identityOpen by rememberSaveable { mutableStateOf(true) }
    var addressOpen by rememberSaveable { mutableStateOf(true) }
    var registrationOpen by rememberSaveable { mutableStateOf(true) }
    var principalOpen by rememberSaveable { mutableStateOf(true) }
    val place = listOf(company.city, company.state, company.pinCode).filter { it.isNotBlank() }.joinToString(" ")
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = CarbonSpacing.screen),
        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = CarbonSpacing.screen, bottom = CarbonSpacing.scrollBottom),
    ) {
        item {
            Text("Company profile", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(
                "Letterhead identity used on letters, invoices and the Wi‑Fi portal.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = CarbonSpacing.micro),
            )
        }
        item {
            ArchiCard(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(start = CarbonSpacing.card, end = CarbonSpacing.micro, top = CarbonSpacing.stackTight, bottom = CarbonSpacing.stackTight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(CarbonIcons.Business, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = CarbonSpacing.stack))
                    Column(Modifier.weight(1f).padding(vertical = CarbonSpacing.micro), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline)) {
                        Text(company.practiceName, style = MaterialTheme.typography.titleLarge)
                        if (company.legalName.isNotBlank() && company.legalName != company.practiceName) {
                            Text(company.legalName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (company.companyType.isNotBlank()) {
                            Text(company.companyType, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    RecordActions(RecordKind.PRACTICE, company.id, company.practiceName)
                }
            }
        }
        item {
            com.holagundi.archiman.ui.theme.DisclosureCard(
                title = "Identity",
                summary = listOf(company.practiceName, company.companyType).filter { it.isNotBlank() }.joinToString(" · "),
                expanded = identityOpen,
                onExpandedChange = { identityOpen = it },
                leadingIcon = CarbonIcons.Business,
            ) {
                com.holagundi.archiman.ui.theme.DetailRow("Practice name", company.practiceName)
                com.holagundi.archiman.ui.theme.DetailRow("Legal name", company.legalName)
                com.holagundi.archiman.ui.theme.DetailRow("Company type", company.companyType)
                com.holagundi.archiman.ui.theme.DetailRow("Website", company.website)
            }
        }
        item {
            com.holagundi.archiman.ui.theme.DisclosureCard(
                title = "Address & contact",
                summary = listOf(place, company.phone, company.email).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "No contact details yet" },
                expanded = addressOpen,
                onExpandedChange = { addressOpen = it },
                leadingIcon = CarbonIcons.Place,
            ) {
                com.holagundi.archiman.ui.theme.DetailRow("Address", company.address)
                com.holagundi.archiman.ui.theme.DetailRow("City / state / PIN", place)
                com.holagundi.archiman.ui.theme.DetailRow("Country", company.country)
                com.holagundi.archiman.ui.theme.DetailRow("Phone", company.phone)
                com.holagundi.archiman.ui.theme.DetailRow("Email", company.email)
            }
        }
        item {
            com.holagundi.archiman.ui.theme.DisclosureCard(
                title = "Registration & tax",
                summary = listOf(
                    company.coaRegistrationNumber.takeIf { it.isNotBlank() }?.let { "CoA $it" },
                    company.gstin.takeIf { it.isNotBlank() }?.let { "GSTIN $it" },
                    company.pan.takeIf { it.isNotBlank() }?.let { "PAN $it" },
                ).filterNotNull().joinToString(" · ").ifBlank { "No registrations yet" },
                expanded = registrationOpen,
                onExpandedChange = { registrationOpen = it },
                leadingIcon = CarbonIcons.Description,
            ) {
                com.holagundi.archiman.ui.theme.DetailRow("CoA registration", company.coaRegistrationNumber, monospace = true)
                com.holagundi.archiman.ui.theme.DetailRow("PAN", company.pan, monospace = true)
                com.holagundi.archiman.ui.theme.DetailRow("GSTIN", company.gstin, monospace = true)
                com.holagundi.archiman.ui.theme.DetailRow("Practice registration", company.practiceRegistrationDetails)
            }
        }
        item {
            com.holagundi.archiman.ui.theme.DisclosureCard(
                title = "Principal",
                summary = listOf(company.principalName, company.principalQualification).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "No principal recorded" },
                expanded = principalOpen,
                onExpandedChange = { principalOpen = it },
                leadingIcon = CarbonIcons.People,
            ) {
                com.holagundi.archiman.ui.theme.DetailRow("Name", company.principalName)
                com.holagundi.archiman.ui.theme.DetailRow("Qualification", company.principalQualification)
            }
        }
        item {
            Text(
                "Edit any section with the ⋮ menu on the company card. Changes apply to letterhead exports and the portal.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
        }
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
    val types = listOf("Residential", "Commercial", "Institutional", "Interior")
    CarbonModal(
        title = "New project",
        description = "A Ground Floor is added so measurement can start. Architect: ${architect.ifBlank { "Practice principal" }}",
        onDismiss = onDismiss,
        primaryLabel = "Create",
        onPrimary = { if (name.isNotBlank() && client.isNotBlank()) onCreate(name, code, client, type, site) },
        primaryEnabled = !busy && name.isNotBlank() && client.isNotBlank(),
        primaryBusy = busy,
    ) {
        saveNotice?.takeIf { it.kind == com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR }?.let {
            com.holagundi.archiman.ui.theme.CarbonInlineNotification("Could not create project", it.message, it.kind)
        }
        CarbonTextInput(name, { name = it }, "Project name", enabled = !busy)
        CarbonTextInput(code, { code = it }, "Code", helperText = "Optional", enabled = !busy)
        CarbonTextInput(client, { client = it }, "Client", enabled = !busy)
        CarbonTextInput(site, { site = it }, "Site address", enabled = !busy)
        CarbonDropdown(
            label = "Project type",
            options = types.map { CarbonDropdownOption(it, it) },
            selectedId = type,
            onSelected = { type = it },
            enabled = !busy,
        )
    }
}

@Composable
private fun PracticeFloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CarbonSearch(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = stringResource(R.string.practice_search_hint),
        onClose = onClose,
        requestFocus = true,
    )
}

@Composable
private fun PracticeSearchResults(
    viewModel: PortfolioViewModel,
    onOpenProject: (Long) -> Unit,
    onOpenRecord: (Long, WorkspacePage, Long) -> Unit,
) {
    val hits by viewModel.practiceSearchHits.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = CarbonSpacing.screen).padding(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
    ) {
        if (hits.projects.isNotEmpty()) {
            item { Text("Projects", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(hits.projects, key = { "p-${it.id}" }) { project ->
                FeatureRow(project.name, listOf(project.projectCode, project.client).filter { it.isNotBlank() }.joinToString(" · "), CarbonIcons.Apartment) {
                    onOpenProject(project.id)
                }
            }
        }
        if (hits.tasks.isNotEmpty()) {
            item { Text("Tasks", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(hits.tasks, key = { "t-${it.id}" }) { task ->
                FeatureRow(task.title, task.projectName, CarbonIcons.Assignment) {
                    onOpenRecord(task.projectId, WorkspacePage.TASKS, task.id)
                }
            }
        }
        if (hits.reviews.isNotEmpty()) {
            item { Text("Sheets to review", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(hits.reviews, key = { "r-${it.id}" }) { review ->
                FeatureRow(review.sheetCode, review.projectName, CarbonIcons.FactCheck) {
                    onOpenRecord(review.projectId, WorkspacePage.BOOK, review.id)
                }
            }
        }
        if (hits.issues.isNotEmpty()) {
            item { Text("Site issues", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(hits.issues, key = { "i-${it.id}" }) { issue ->
                FeatureRow(issue.title, issue.projectName, CarbonIcons.ReportProblem) {
                    onOpenRecord(issue.projectId, WorkspacePage.SNAGS, issue.id)
                }
            }
        }
        if (hits.invoices.isNotEmpty()) {
            item { Text("Invoices", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp)) }
            items(hits.invoices, key = { "inv-${it.id}" }) { invoice ->
                FeatureRow(invoice.number, invoice.title, CarbonIcons.ReceiptLong) {
                    onOpenRecord(invoice.projectId, WorkspacePage.INVOICE, invoice.id)
                }
            }
        }
        if (hits.projects.isEmpty() && hits.tasks.isEmpty() && hits.reviews.isEmpty() && hits.issues.isEmpty() && hits.invoices.isEmpty()) {
            item { Text("No matches.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp)) }
        }
    }
}
