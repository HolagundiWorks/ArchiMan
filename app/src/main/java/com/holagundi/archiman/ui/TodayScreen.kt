package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Straighten
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Today view for quick daily entries
 * Inspired by Raken and Fieldwire's "Today" screens
 */
@Composable
fun TodayScreen(
    onNavigateToProjects: () -> Unit,
    session: SessionViewModel = hiltViewModel(),
    portfolio: PortfolioViewModel = hiltViewModel(),
) {
    val role by session.currentRole.collectAsStateWithLifecycle()
    val projects by portfolio.projects.collectAsStateWithLifecycle()
    val today = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Date header
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Today",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    today,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Role: ${role ?: "Not signed in"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Quick action message
        Text(
            "Quick daily actions will appear here once you select a project.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Role-specific quick actions
        Text(
            "Available Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        when (role) {
            "Site", "Site Engineer", "Siteman" -> {
                QuickActionCard(
                    title = "Daily Site Log",
                    description = "Record today's manpower, materials, and progress",
                    icon = Icons.Default.Engineering,
                    onClick = onNavigateToProjects
                )
                QuickActionCard(
                    title = "Quick Measurement",
                    description = "Add measurement entry to open sheet",
                    icon = Icons.Default.Straighten,
                    onClick = onNavigateToProjects
                )
                QuickActionCard(
                    title = "Report Snag",
                    description = "Add new snag or NCR",
                    icon = Icons.Default.Report,
                    onClick = onNavigateToProjects
                )
            }
            "Office", "Office Engineer" -> {
                QuickActionCard(
                    title = "Review Measurements",
                    description = "Check and approve submitted sheets",
                    icon = Icons.Default.CheckCircle,
                    onClick = onNavigateToProjects
                )
                QuickActionCard(
                    title = "View Projects",
                    description = "${projects.size} active projects",
                    icon = Icons.Default.AddCircle,
                    onClick = onNavigateToProjects
                )
            }
            else -> {
                QuickActionCard(
                    title = "Select Project",
                    description = "Choose a project to see today's actions",
                    icon = Icons.Default.AddCircle,
                    onClick = onNavigateToProjects
                )
            }
        }

        // Recent activity placeholder
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Your recent measurements, logs, and issues will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tips section
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "💡 Tip",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    when (role) {
                        "Site", "Site Engineer" -> "Use 'Copy previous entry' in measurement book to speed up repetitive entries."
                        "Office", "Office Engineer" -> "You can approve multiple sheets at once using batch actions."
                        else -> "Select a project from Projects tab to see context-specific quick actions."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
