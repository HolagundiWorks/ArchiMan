package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.ui.theme.GlassSurface

/**
 * Reports and exports screen
 * Following PlanSwift and Procore patterns for report generation
 */
@Composable
fun ReportsScreen(
    onNavigateToProjects: () -> Unit,
    session: SessionViewModel = hiltViewModel(),
    portfolio: PortfolioViewModel = hiltViewModel(),
) {
    val user by session.signedIn.collectAsStateWithLifecycle()
    val role = user?.role
    val projects by portfolio.projects.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Reports & Exports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Generate measurement books, abstracts, and analytics",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Report types
        Text(
            "Available Reports",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        QuickActionCard(
            title = "Measurement Book",
            description = "Export sheets in MB format (PDF/Excel)",
            icon = CarbonIcons.Description,
            onClick = onNavigateToProjects
        )

        QuickActionCard(
            title = "Quantity Abstract",
            description = "Approved quantities summary (CSV)",
            icon = CarbonIcons.TableChart,
            onClick = onNavigateToProjects
        )

        QuickActionCard(
            title = "Running Account Bill",
            description = "RA bill with rates and quantities",
            icon = CarbonIcons.Calculate,
            onClick = onNavigateToProjects
        )

        QuickActionCard(
            title = "Daily Log Summary",
            description = "Site progress and manpower report",
            icon = CarbonIcons.Assessment,
            onClick = onNavigateToProjects
        )

        // Instructions
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "How to generate reports",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "1. Open a project from the Projects tab\n" +
                    "2. Navigate to the relevant section (Measurement Book, Siteman, etc.)\n" +
                    "3. Use the export/share buttons to generate reports\n" +
                    "4. Choose format (PDF, Excel, CSV) and share via WhatsApp, email, or save to device",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Project-specific reports
        if (projects.isNotEmpty()) {
            Text(
                "Recent Projects",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            projects.take(5).forEach { project ->
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            project.projectCode,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            project.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Tap to view project reports",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
