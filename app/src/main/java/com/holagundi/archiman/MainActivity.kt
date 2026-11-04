package com.holagundi.archiman

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import com.holagundi.archiman.ui.theme.ArchiManTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArchiManTheme {
                val session: SessionViewModel = hiltViewModel()
                val signedIn = session.signedIn.collectAsStateWithLifecycle()
                if (signedIn.value == null) {
                    LoginScreen(viewModel = session)
                } else {
                    val nav = rememberNavController()
                    NavHost(navController = nav, startDestination = "portfolio") {
                        composable("portfolio") {
                            PortfolioScreen(
                                onOpenProject = { id -> nav.navigate("project/$id") },
                                onSignOut = session::signOut,
                            )
                        }
                        composable(
                            route = "project/{projectId}",
                            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                        ) {
                            ProjectWorkspaceScreen(onBack = { nav.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
