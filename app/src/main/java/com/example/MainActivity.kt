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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.navigation.AppScreen
import com.example.ui.navigation.HomeTab
import com.example.ui.viewmodel.SiteViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SiteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CarbonWhite
                ) {
                    MainAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: SiteViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val homeTab by viewModel.selectedHomeTab.collectAsStateWithLifecycle()
    val selectedProjectId by viewModel.selectedProjectId.collectAsStateWithLifecycle()

    var showRecordMeasurementWizard by remember { mutableStateOf(false) }

    // Smart contextual back handler
    if (currentScreen != AppScreen.HOME) {
        BackHandler {
            when (currentScreen) {
                AppScreen.PROJECT_WORKSPACE -> viewModel.navigateTo(AppScreen.HOME)
                AppScreen.DEDICATED_MEASUREMENT -> viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                AppScreen.ROOM_WORKSPACE -> viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                AppScreen.MEASUREMENT_BOOK -> viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE)
                AppScreen.CLIENTS, AppScreen.CONTRACTORS, AppScreen.COMPANY_PROFILE, AppScreen.LOCAL_PORTAL -> viewModel.navigateTo(AppScreen.HOME)
                AppScreen.EXPORT -> viewModel.navigateTo(AppScreen.HOME)
                AppScreen.MASTER_DATA -> viewModel.navigateTo(
                    if (homeTab == HomeTab.MORE || selectedProjectId == null) AppScreen.HOME else AppScreen.PROJECT_WORKSPACE
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
        (currentScreen == AppScreen.MASTER_DATA && homeTab != HomeTab.MORE)

    Scaffold(
        containerColor = CarbonWhite,
        bottomBar = {
            if (showPortfolioNavigation) {
                // Portfolio navigation: primary directories plus infrequent practice tools.
                NavigationBar(
                    containerColor = CarbonWhite,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .drawBehind {
                            drawLine(
                                color = CarbonGray20,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .testTag("home_bottom_navigation")
                ) {
                    // 1. Projects
                    NavigationBarItem(
                        selected = homeTab == HomeTab.PROJECTS && currentScreen == AppScreen.HOME,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.PROJECTS)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.Apartment, contentDescription = "Projects") },
                        label = {
                            Text(
                                "Projects",
                                fontSize = 11.sp,
                                fontWeight = if (homeTab == HomeTab.PROJECTS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonGray70,
                            unselectedTextColor = CarbonGray70
                        ),
                        modifier = Modifier.testTag("home_nav_projects")
                    )

                    // 2. Shared client and contractor directory
                    NavigationBarItem(
                        selected = homeTab == HomeTab.DIRECTORY,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.DIRECTORY)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.ContactPage, contentDescription = "Directory") },
                        label = {
                            Text(
                                "Directory",
                                fontSize = 11.sp,
                                fontWeight = if (homeTab == HomeTab.DIRECTORY) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonGray70,
                            unselectedTextColor = CarbonGray70
                        ),
                        modifier = Modifier.testTag("home_nav_directory")
                    )

                    // 3. Shared PWD SR work, UOM and formula library
                    NavigationBarItem(
                        selected = homeTab == HomeTab.WORK_LIBRARY,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.WORK_LIBRARY)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = "Work library") },
                        label = {
                            Text(
                                "Work List",
                                fontSize = 11.sp,
                                fontWeight = if (homeTab == HomeTab.WORK_LIBRARY) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonGray70,
                            unselectedTextColor = CarbonGray70
                        ),
                        modifier = Modifier.testTag("home_nav_work_library")
                    )

                    // 4. Infrequent practice tools and shared data
                    NavigationBarItem(
                        selected = homeTab == HomeTab.MORE,
                        onClick = {
                            viewModel.setHomeTab(HomeTab.MORE)
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                        label = {
                            Text(
                                "More",
                                fontSize = 11.sp,
                                fontWeight = if (homeTab == HomeTab.MORE) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonGray70,
                            unselectedTextColor = CarbonGray70
                        ),
                        modifier = Modifier.testTag("home_nav_more")
                    )
                }
            } else if (showProjectNavigation) {
                // Project navigation: persistent work actions. Portfolio exit remains in the header.
                NavigationBar(
                    containerColor = CarbonWhite,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .drawBehind {
                            drawLine(
                                color = CarbonGray20,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .testTag("inside_project_bottom_navigation")
                ) {
                    val isWorkspaceActive = currentScreen == AppScreen.PROJECT_WORKSPACE || currentScreen == AppScreen.ROOM_WORKSPACE
                    val isMBookActive = currentScreen == AppScreen.MEASUREMENT_BOOK || currentScreen == AppScreen.REGISTER
                    val isWorkListActive = currentScreen == AppScreen.MASTER_DATA

                    // 1. Project overview
                    NavigationBarItem(
                        selected = isWorkspaceActive,
                        onClick = { viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE) },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Project") },
                        label = {
                            Text(
                                "Project",
                                fontSize = 11.sp,
                                fontWeight = if (isWorkspaceActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonGray70,
                            unselectedTextColor = CarbonGray70
                        ),
                        modifier = Modifier.testTag("project_nav_hub")
                    )

                    // 2. Work-item and formula library
                    NavigationBarItem(
                        selected = isWorkListActive,
                        onClick = { viewModel.navigateTo(AppScreen.MASTER_DATA) },
                        icon = { Icon(Icons.Default.AccountTree, contentDescription = "Work List") },
                        label = {
                            Text(
                                "Work",
                                fontSize = 11.sp,
                                fontWeight = if (isWorkListActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonGray70,
                            unselectedTextColor = CarbonGray70
                        ),
                        modifier = Modifier.testTag("project_nav_work_list")
                    )

                    // 3. Record measurement — the primary field action.
                    NavigationBarItem(
                        selected = false,
                        onClick = { showRecordMeasurementWizard = true },
                        icon = { Icon(Icons.Default.AddCircle, contentDescription = "Record measurement") },
                        label = {
                            Text(
                                "Record",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonBlue60,
                            unselectedTextColor = CarbonBlue60
                        ),
                        modifier = Modifier.testTag("project_nav_record_measure")
                    )

                    // 4. Measurement Book
                    NavigationBarItem(
                        selected = isMBookActive,
                        onClick = { viewModel.navigateTo(AppScreen.MEASUREMENT_BOOK) },
                        icon = { Icon(Icons.Default.MenuBook, contentDescription = "M-Book") },
                        label = {
                            Text(
                                "M-Book",
                                fontSize = 11.sp,
                                fontWeight = if (isMBookActive) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CarbonBlue60,
                            selectedTextColor = CarbonBlue60,
                            indicatorColor = CarbonBlue10,
                            unselectedIconColor = CarbonGray70,
                            unselectedTextColor = CarbonGray70
                        ),
                        modifier = Modifier.testTag("project_nav_mbook")
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
                    AppScreen.EXPORT -> ExportScreen(
                        viewModel = viewModel,
                        onNavigateBack = { viewModel.navigateTo(AppScreen.PROJECT_WORKSPACE) }
                    )
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
