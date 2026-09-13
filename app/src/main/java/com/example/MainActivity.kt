package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.ArchiManTheme
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.HomeTab
import com.example.ui.navigation.ProjectSection
import com.example.ui.viewmodel.SiteViewModel
import com.example.aorms.AormsSessionStatus

class MainActivity : ComponentActivity() {
    private val viewModel: SiteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArchiManTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    MainAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: SiteViewModel) {
    val aormsSessionState by viewModel.aormsSessionState.collectAsStateWithLifecycle()

    // Office-only gate: nothing below this renders until AORMS has verified
    // the signed-in account live, on this launch. See AormsSessionManager.
    if (aormsSessionState.status != AormsSessionStatus.SIGNED_IN) {
        var showServerSettings by remember { mutableStateOf(false) }
        if (showServerSettings) {
            // Reachable pre-sign-in so an administrator can enter the AORMS
            // server address and product key before anyone can sign in at all.
            CompanyProfileScreen(viewModel = viewModel, onBack = { showServerSettings = false }, onOpenPortal = {})
        } else {
            when (aormsSessionState.status) {
                AormsSessionStatus.UNREACHABLE -> AormsUnreachableScreen(aormsSessionState, viewModel)
                else -> AormsLoginScreen(
                    sessionState = aormsSessionState,
                    viewModel = viewModel,
                    onOpenServerSettings = { showServerSettings = true }
                )
            }
        }
        return
    }

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val homeTab by viewModel.selectedHomeTab.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()
    val projectSection by viewModel.selectedProjectSection.collectAsStateWithLifecycle()

    var showRecordMeasurementWizard by remember { mutableStateOf(false) }

    // Smart contextual back handler
    if (currentScreen != AppScreen.HOME) {
        BackHandler {
            when (currentScreen) {
                AppScreen.PROJECT_WORKSPACE -> {
                    when (projectSection) {
                        ProjectSection.OVERVIEW -> viewModel.navigateTo(AppScreen.HOME)
                        ProjectSection.MORE, ProjectSection.BRIEF_SCOPE, ProjectSection.PLANNING -> viewModel.setProjectSection(ProjectSection.OVERVIEW)
                        else -> viewModel.setProjectSection(ProjectSection.MORE)
                    }
                }
                AppScreen.DEDICATED_MEASUREMENT -> viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                AppScreen.ROOM_WORKSPACE -> viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                AppScreen.MEASUREMENT_BOOK -> viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                AppScreen.CLIENTS, AppScreen.CONTRACTORS, AppScreen.COMPANY_PROFILE, AppScreen.LOCAL_PORTAL -> viewModel.navigateTo(AppScreen.HOME)
                AppScreen.MASTER_DATA -> viewModel.navigateTo(
                    if (homeTab == HomeTab.PRACTICE || selectedProjectId == null) AppScreen.HOME else AppScreen.PROJECT_WORKSPACE
                )
                AppScreen.REGISTER -> viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                AppScreen.PROJECTS -> viewModel.navigateTo(AppScreen.HOME)
                else -> viewModel.navigateTo(AppScreen.HOME)
            }
        }
    }

    val showPortfolioNavigation = currentScreen == AppScreen.HOME || currentScreen == AppScreen.CLIENTS || currentScreen == AppScreen.CONTRACTORS || currentScreen == AppScreen.PROJECTS
    val showProjectNavigation = currentScreen == AppScreen.PROJECT_WORKSPACE ||
        currentScreen == AppScreen.ROOM_WORKSPACE ||
        currentScreen == AppScreen.MEASUREMENT_BOOK ||
        currentScreen == AppScreen.REGISTER ||
        (currentScreen == AppScreen.MASTER_DATA && homeTab != HomeTab.PRACTICE)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            if (showPortfolioNavigation) {
                // Portfolio navigation: primary directories plus infrequent practice tools.
                NavigationBar(modifier = Modifier.testTag("home_bottom_navigation")) {
                    // 1. Projects
                    NavigationBarItem(
                        selected = homeTab == HomeTab.PROJECTS && currentScreen == AppScreen.HOME,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.PROJECTS)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.Apartment, contentDescription = "Projects") },
                        label = { Text("Projects") },
                        modifier = Modifier.testTag("home_nav_projects").semantics { contentDescription = "Projects" }
                    )

                    // 2. Shared client and contractor directory
                    NavigationBarItem(
                        selected = homeTab == HomeTab.DIRECTORY,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.DIRECTORY)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.ContactPage, contentDescription = "Contacts") },
                        label = { Text("Contacts") },
                        modifier = Modifier.testTag("home_nav_directory").semantics { contentDescription = "Contacts" }
                    )

                    // 3. Shared PWD SR work, UOM and formula library
                    NavigationBarItem(
                        selected = homeTab == HomeTab.WORK_LIBRARY,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.WORK_LIBRARY)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = "Work library") },
                        label = { Text("Library") },
                        modifier = Modifier.testTag("home_nav_work_library").semantics { contentDescription = "Work library" }
                    )

                    // 4. Infrequent practice tools and shared data
                    NavigationBarItem(
                        selected = homeTab == HomeTab.PRACTICE,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.PRACTICE)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.Domain, contentDescription = "Practice") },
                        label = { Text("Practice") },
                        modifier = Modifier.testTag("home_nav_more").semantics { contentDescription = "Practice" }
                    )
                }
            } else if (showProjectNavigation) {
                // Project navigation: persistent work actions. Portfolio exit remains in the header.
                NavigationBar(modifier = Modifier.testTag("inside_project_bottom_navigation")) {
                    val isWorkspaceActive = (currentScreen == AppScreen.PROJECT_WORKSPACE && projectSection != ProjectSection.MORE) || currentScreen == AppScreen.ROOM_WORKSPACE
                    val isMoreActive = currentScreen == AppScreen.PROJECT_WORKSPACE && projectSection == ProjectSection.MORE
                    val isMBookActive = currentScreen == AppScreen.MEASUREMENT_BOOK || currentScreen == AppScreen.REGISTER
                    val isWorkListActive = currentScreen == AppScreen.MASTER_DATA

                    // 1. Project overview
                    NavigationBarItem(
                        selected = isWorkspaceActive,
                        onClick = { viewModel.setProjectSection(ProjectSection.OVERVIEW); viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Project") },
                        label = { Text("Project") },
                        modifier = Modifier.testTag("project_nav_hub").semantics { contentDescription = "Project overview" }
                    )

                    // 2. Work-item and formula library
                    NavigationBarItem(
                        selected = isWorkListActive,
                        onClick = { viewModel.navigateTo(AppScreen.MASTER_DATA) },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = "Work List") },
                        label = { Text("Work") },
                        modifier = Modifier.testTag("project_nav_work_list").semantics { contentDescription = "Work catalogue" }
                    )

                    // 3. Record measurement — the primary field action.
                    NavigationBarItem(
                        selected = false,
                        onClick = { showRecordMeasurementWizard = true },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Record measurement") },
                        label = { Text("Record") },
                        modifier = Modifier.testTag("project_nav_record_measure").semantics { contentDescription = "Record measurement" }
                    )

                    // 4. Measurement Book
                    NavigationBarItem(
                        selected = isMBookActive,
                        onClick = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "M-Book") },
                        label = { Text("M-Book") },
                        modifier = Modifier.testTag("project_nav_mbook").semantics { contentDescription = "Measurement Book" }
                    )

                    // 5. Project setup, documents and specialist registers.
                    NavigationBarItem(
                        selected = isMoreActive,
                        onClick = { viewModel.setProjectSection(ProjectSection.MORE); viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE) },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More project tools") },
                        label = { Text("More") },
                        modifier = Modifier.testTag("project_nav_more").semantics { contentDescription = "More project tools" }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                when (screen) {
                    AppScreen.HOME, AppScreen.PROJECTS -> HomeScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    AppScreen.CLIENTS -> ClientsScreen(viewModel = viewModel)
                    AppScreen.CONTRACTORS -> ContractorsScreen(viewModel = viewModel)
                    AppScreen.COMPANY_PROFILE -> CompanyProfileScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateTo(AppScreen.HOME) },
                        onOpenPortal = { viewModel.navigateTo(AppScreen.LOCAL_PORTAL) }
                    )
                    AppScreen.LOCAL_PORTAL -> LocalPortalScreen(viewModel = viewModel, onBack = { viewModel.navigateTo(AppScreen.HOME) })
                    AppScreen.DEDICATED_MEASUREMENT -> DedicatedMeasurementScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE) },
                        onNavigateToBook = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) }
                    )
                    AppScreen.PROJECT_WORKSPACE -> ProjectWorkspaceScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                    AppScreen.ROOM_WORKSPACE -> RoomWorkspaceScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE) }
                    )
                    AppScreen.MEASUREMENT_BOOK -> MeasurementBookScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE) }
                    )
                    AppScreen.REGISTER -> MeasurementRegisterScreen(viewModel = viewModel)
                    AppScreen.MASTER_DATA -> MasterDataScreen(viewModel = viewModel)
                }
            }
        }

        // Global Record Measurement Flow Wizard Dialog (triggers anywhere inside project)
        if (showRecordMeasurementWizard) {
            RecordMeasurementFlowDialog(
                viewModel = viewModel,
                projectId = selectedProjectId ?: 0L,
                onDismiss = { showRecordMeasurementWizard = false },
                onLaunchDedicatedMeasurement = { contId, itName, itUom, itCalc, flrId, flrName ->
                    viewModel.startDedicatedMeasurementSession(
                        contractorId = contId,
                        itemName = itName,
                        uom = itUom,
                        calcType = itCalc,
                        floorId = flrId,
                        floorName = flrName
                    )
                    showRecordMeasurementWizard = false
                    viewModel.navigateTo(AppScreen.DEDICATED_MEASUREMENT)
                }
            )
        }
    }
}
