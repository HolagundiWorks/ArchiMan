package com.example.ui.screens

import com.example.ui.icons.CarbonIcons

import android.content.pm.ActivityInfo
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CompanyDashboardSnapshot
import com.example.domain.RevisionRiskBand
import com.example.ui.viewmodel.SiteViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A single, non-scrolling, forced-landscape, always-on operational board —
 * built to run continuously on a phone/tablet mounted or left face-up in the
 * office. Excludes commercial and HR data per ArchiMan's product boundary
 * (see docs/ARCHITECT_PRACTICE_SCOPE.md); every figure comes from
 * SiteViewModel.companyDashboard (com.example.domain.CompanyDashboard.kt).
 */
@Composable
fun CompanyDashboardScreen(viewModel: SiteViewModel, onExit: () -> Unit) {
    val snapshot by viewModel.companyDashboard.collectAsStateWithLifecycle()
    val companyProfile by viewModel.companyProfile.collectAsStateWithLifecycle()
    LockLandscapeFullScreenAlwaysOn()

    val now by produceState(initialValue = Date()) {
        while (true) {
            value = Date()
            delay(30_000)
        }
    }
    val clockFormat = remember { SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        (companyProfile?.practiceName?.ifBlank { null } ?: "ArchiMan") + " · Company Dashboard",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(clockFormat.format(now), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onExit) { Icon(CarbonIcons.Close, contentDescription = "Exit dashboard") }
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile(Modifier.weight(1f), CarbonIcons.Apartment, "Active Projects", snapshot.activeProjects, "of ${snapshot.totalProjects} total")
                DashboardTile(Modifier.weight(1f), CarbonIcons.Assignment, "Open RFIs", snapshot.openRfis, null)
                DashboardTile(Modifier.weight(1f), CarbonIcons.RuleFolder, "Open Submittals", snapshot.openSubmittals, null)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile(Modifier.weight(1f), CarbonIcons.Gavel, "Open Site Instructions", snapshot.openSiteInstructions, null)
                DashboardTile(
                    Modifier.weight(1f), CarbonIcons.ReportProblem, "Open Snags / NCRs", snapshot.openSiteIssues,
                    if (snapshot.criticalOpenSiteIssues > 0) "${snapshot.criticalOpenSiteIssues} critical/attention" else "None critical",
                    alert = snapshot.criticalOpenSiteIssues > 0
                )
                DashboardTile(
                    Modifier.weight(1f), CarbonIcons.EventNote, "Pending Decisions", snapshot.pendingDecisions,
                    if (snapshot.overdueDecisions > 0) "${snapshot.overdueDecisions} overdue" else "None overdue",
                    alert = snapshot.overdueDecisions > 0
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardTile(
                    Modifier.weight(1f), CarbonIcons.Warning, "Overdue Tasks", snapshot.overdueTasks, null,
                    alert = snapshot.overdueTasks > 0
                )
                DashboardTile(Modifier.weight(1f), CarbonIcons.Today, "Today's Site Reports", snapshot.todaysSiteReports, null)
                DashboardTile(
                    Modifier.weight(1f), CarbonIcons.History, "Revision Risk", snapshot.revisionRisk.criticalRevisions,
                    "${snapshot.revisionRisk.riskBand.name} · ${snapshot.revisionRisk.totalRevisions} revisions",
                    alert = snapshot.revisionRisk.riskBand == RevisionRiskBand.HIGH
                )
            }
        }
    }
}

@Composable
private fun DashboardTile(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: Int,
    subtitle: String?,
    alert: Boolean = false
) {
    val tint = if (alert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier.fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = if (alert) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(28.dp))
            Text(value.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = tint)
            Column {
                Text(label, style = MaterialTheme.typography.titleSmall)
                if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

/**
 * Forces sensor-landscape, hides system bars, and keeps the screen on for as
 * long as this composable is in the composition — restores everything on
 * dispose so leaving the dashboard returns the phone to normal behaviour.
 */
@Composable
private fun LockLandscapeFullScreenAlwaysOn() {
    val view = LocalView.current
    val activity = LocalActivity.current ?: return
    DisposableEffect(Unit) {
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        val controller = WindowInsetsControllerCompat(activity.window, view)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            activity.requestedOrientation = originalOrientation
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
