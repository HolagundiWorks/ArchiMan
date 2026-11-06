package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.ui.theme.GlassSurface
import com.holagundi.archiman.ui.theme.ScreenTitle

/**
 * Home dashboard with role-based quick actions and summary cards
 * Follows case study patterns from Procore, Fieldwire, and Raken
 */
@Composable
fun HomeScreen(
    onNavigateToProjects: () -> Unit,
    onNavigateToToday: () -> Unit,
    onOpenProject: (Long) -> Unit,
    portfolio: PortfolioViewModel = hiltViewModel(),
    session: SessionViewModel = hiltViewModel(),
) {
    val user by session.signedIn.collectAsStateWithLifecycle()
    val role = user?.role
    val company by session.company.collectAsStateWithLifecycle()
    val projects by portfolio.projects.collectAsStateWithLifecycle()
    val recentProjects = projects.take(5)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome header
        GlassSurface(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Welcome back",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    company?.practiceName ?: "ArchiMan",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Role: ${role ?: "Not signed in"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Role-based quick actions
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        when (role) {
            "Principal", "Architect" -> {
                QuickActionCard(
                    title = "Record Today's Work",
                    description = "Add daily log, measurements, or issues",
                    icon = CarbonIcons.Edit,
                    onClick = onNavigateToToday
                )
                QuickActionCard(
                    title = "View Projects",
                    description = "${projects.size} active projects",
                    icon = CarbonIcons.CheckCircle,
                    onClick = onNavigateToProjects
                )
            }
            "Site", "Site Engineer" -> {
                QuickActionCard(
                    title = "Daily Log",
                    description = "Record today's site progress",
                    icon = CarbonIcons.Engineering,
                    onClick = onNavigateToToday
                )
                QuickActionCard(
                    title = "Quick Measurement",
                    description = "Add measurement entry",
                    icon = CarbonIcons.Edit,
                    onClick = onNavigateToToday
                )
                QuickActionCard(
                    title = "Report Issue",
                    description = "Add snag or NCR",
                    icon = CarbonIcons.Warning,
                    onClick = onNavigateToToday
                )
            }
            "Office", "Office Engineer" -> {
                QuickActionCard(
                    title = "Pending Approvals",
                    description = "Review and approve measurements",
                    icon = CarbonIcons.CheckCircle,
                    onClick = onNavigateToProjects
                )
                QuickActionCard(
                    title = "View Projects",
                    description = "${projects.size} active projects",
                    icon = CarbonIcons.Add,
                    onClick = onNavigateToProjects
                )
            }
            else -> {
                QuickActionCard(
                    title = "View Projects",
                    description = "${projects.size} projects",
                    icon = CarbonIcons.Add,
                    onClick = onNavigateToProjects
                )
            }
        }

        // Recent projects section
        if (recentProjects.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Recent Projects",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            recentProjects.forEach { project ->
                ProjectCard(
                    code = project.projectCode,
                    name = project.name,
                    location = project.siteLocation,
                    onClick = { onOpenProject(project.id) }
                )
            }
        }

        // Summary metrics
        Spacer(Modifier.height(8.dp))
        Text(
            "Overview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                value = projects.size.toString(),
                label = "Projects",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                value = projects.count { it.status == "Active" }.toString(),
                label = "Active",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.shapes.medium
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ProjectCard(
    code: String,
    name: String,
    location: String,
    onClick: () -> Unit
) {
    GlassSurface(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                code,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
