package com.holagundi.archiman.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.ui.shell.WorkspacePage
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

@Composable
fun AttentionInbox(vm: PortfolioViewModel, filter: String, onBack: () -> Unit, onOpen: (Long, WorkspacePage, Long) -> Unit) {
    val tasks by vm.inboxTasks.collectAsStateWithLifecycle()
    val reviews by vm.inboxReviews.collectAsStateWithLifecycle()
    val issues by vm.inboxIssues.collectAsStateWithLifecycle()
    val metrics by vm.dashboardMetrics.collectAsStateWithLifecycle()
    var query by rememberSaveable(filter) { mutableStateOf("") }
    val rows = when (filter) {
        "tasks" -> tasks.map { AttentionRow(it.id, it.projectId, WorkspacePage.TASKS, it.title, "${it.projectName} · ${taskDueLabel(it.dueDate)}") }
        "reviews" -> reviews.map { AttentionRow(it.id, it.projectId, WorkspacePage.BOOK, it.sheetCode, "${it.projectName} · ${if (it.status == "SUBMITTED") "Ready to check" else "Awaiting approval"}") }
        else -> issues.map { AttentionRow(it.id, it.projectId, WorkspacePage.SNAGS, it.title, "${it.projectName} · ${it.severity.lowercase().replaceFirstChar { c -> c.uppercase() }}") }
    }
    val total = when (filter) { "tasks" -> metrics?.openTasks; "reviews" -> metrics?.pendingReviews; else -> metrics?.openIssues }
    BackHandler(onBack = onBack)
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("Back to dashboard") }
            Text(when (filter) { "tasks" -> "Open tasks"; "reviews" -> "Sheets to review"; else -> "Open site issues" }, style = MaterialTheme.typography.headlineSmall)
            Text("${rows.size} loaded of ${total ?: "…"} across current projects")
            ArchiField(query, { query = it }, "Search loaded items", Modifier.fillMaxWidth())
        }
        val visible = rows.filter { (it.title + " " + it.detail).contains(query, true) }
        if (visible.isEmpty()) item { Text(if (total == null) "Loading work…" else "No matching loaded items.") }
        items(visible, key = { it.id }) { row -> FeatureRow(row.title, row.detail, row.page.icon) { onOpen(row.projectId, row.page, row.id) } }
        if (total != null && rows.size < total!!) item { TextButton(onClick = vm::loadMoreAttention) { Text("Load 50 more items") } }
    }
}
private data class AttentionRow(val id: Long, val projectId: Long, val page: WorkspacePage, val title: String, val detail: String)
