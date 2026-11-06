package com.holagundi.archiman

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.holagundi.archiman.ui.LoginScreen
import com.holagundi.archiman.ui.PortfolioScreen
import com.holagundi.archiman.ui.ProjectWorkspaceScreen
import com.holagundi.archiman.ui.SessionViewModel
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.ArchiManTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        com.holagundi.archiman.widget.PracticeWidgetUpdater.requestUpdate(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArchiManTheme {
                val session: SessionViewModel = hiltViewModel()
                val signedIn = session.signedIn.collectAsStateWithLifecycle()
                val initialized = session.initialized.collectAsStateWithLifecycle()
                ReportDrawnWhen { initialized.value }
                if (signedIn.value == null) {
                    LoginScreen(viewModel = session)
                } else {
                    com.holagundi.archiman.ui.AppLockGate(
                        enabled = !android.app.ActivityManager.isRunningInTestHarness(),
                    ) {
                        val nav = rememberNavController()
                        NavHost(navController = nav, startDestination = "portfolio") {
                            composable("portfolio") {
                                PortfolioScreen(
                                    onOpenProject = { id -> nav.navigate("project/$id") },
                                    onSignOut = session::signOut,
                                    onOpenPage = { id, page -> nav.navigate("project/$id?page=${page.name}") },
                                    onOpenRecord = { id, page, record -> nav.navigate("project/$id?page=${page.name}&recordId=$record") },
                                )
                            }
                            composable(
                                route = "project/{projectId}?page={page}&recordId={recordId}",
                                arguments = listOf(navArgument("projectId") { type = NavType.LongType }, navArgument("page") { type = NavType.StringType; defaultValue = "OVERVIEW" }, navArgument("recordId") { type = NavType.LongType; defaultValue = 0L }),
                            ) { entry ->
                                val startPage = WorkspacePage.entries.firstOrNull { it.name == entry.arguments?.getString("page") } ?: WorkspacePage.OVERVIEW
                                ProjectWorkspaceScreen(onBack = { nav.popBackStack() }, startPage = startPage, focusId = entry.arguments?.getLong("recordId") ?: 0L)
                            }
                        }
                    }
                }
            }
        }
    }
}
