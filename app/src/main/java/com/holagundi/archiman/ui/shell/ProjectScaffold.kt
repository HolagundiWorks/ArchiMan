@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui.shell

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.*
import com.holagundi.archiman.ui.theme.CarbonNavigationBar
import com.holagundi.archiman.ui.theme.CarbonNavigationRail
import com.holagundi.archiman.ui.theme.CarbonNavigationItem
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.holagundi.archiman.R
import androidx.compose.ui.unit.dp

@Composable
fun ProjectScaffold(
    projectCode: String,
    projectName: String,
    group: ProjectGroup,
    page: WorkspacePage?,
    onGroupChange: (ProjectGroup) -> Unit,
    onBack: () -> Unit,
    actions: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if(wide) CarbonNavigationRail {
                ProjectGroup.entries.forEach { item ->
                    CarbonNavigationItem(stringResource(item.labelRes),item.icon,group==item,{onGroupChange(item)},rail=true)
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(CarbonIcons.ArrowBack, stringResource(
                                    if (page == WorkspacePage.OVERVIEW) R.string.back_to_practice else R.string.back_to_section))
                            }
                        },
                        title = {
                            Column {
                                Text(projectName.ifBlank { stringResource(R.string.opening_project) },
                                    style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(listOf(projectCode, stringResource(page?.labelRes ?: group.labelRes)).filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        },
                        actions = { actions() },
                    )
                },
                bottomBar = {
                    if(!wide) CarbonNavigationBar {
                        ProjectGroup.entries.forEach { item ->
                            CarbonNavigationItem(stringResource(item.labelRes),item.icon,group==item,{onGroupChange(item)},Modifier.weight(1f))
                        }
                    }
                },
                content = content,
            )
        }
    }
}
