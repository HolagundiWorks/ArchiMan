@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.holagundi.archiman.R

@Composable
fun ProjectScaffold(
    projectCode: String,
    projectName: String,
    status: String,
    page: WorkspacePage,
    sheetCount: Int,
    onPageChange: (WorkspacePage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                ProjectSectionRail(
                    page = page,
                    sheetCount = sheetCount,
                    onPageChange = onPageChange,
                    modifier = Modifier
                        .width(220.dp)
                        .fillMaxHeight(),
                )
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = {
                        ProjectTopBar(
                            projectCode = projectCode,
                            projectName = projectName,
                            status = status,
                            onBack = onBack,
                        )
                    },
                    content = content,
                )
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    Column {
                        ProjectTopBar(
                            projectCode = projectCode,
                            projectName = projectName,
                            status = status,
                            onBack = onBack,
                        )
                        ProjectSectionTabs(
                            page = page,
                            sheetCount = sheetCount,
                            onPageChange = onPageChange,
                        )
                    }
                },
                content = content,
            )
        }
    }
}

@Composable
private fun ProjectTopBar(
    projectCode: String,
    projectName: String,
    status: String,
    onBack: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_practice),
                )
            }
        },
        title = {
            Column {
                Text(
                    text = projectCode.ifBlank { stringResource(R.string.opening_project) },
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
                Text(
                    text = projectName.ifBlank { stringResource(R.string.opening_project) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
            }
        },
        actions = {
            if (status.isNotBlank()) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        },
    )
}

@Composable
private fun ProjectSectionRail(
    page: WorkspacePage,
    sheetCount: Int,
    onPageChange: (WorkspacePage) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ProjectGroup.entries.forEach { group ->
            Text(
                text = stringResource(group.labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
            WorkspacePage.entries.filter { it.group == group }.forEach { item ->
                NavigationDrawerItem(
                    selected = page == item,
                    onClick = { onPageChange(item) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    label = { Text(pageLabel(item, sheetCount)) },
                )
            }
            if (group != ProjectGroup.entries.last()) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun ProjectSectionTabs(
    page: WorkspacePage,
    sheetCount: Int,
    onPageChange: (WorkspacePage) -> Unit,
) {
    val selectedIndex = WorkspacePage.entries.indexOf(page).coerceAtLeast(0)
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 8.dp,
        divider = {},
    ) {
        WorkspacePage.entries.forEach { item ->
            Tab(
                selected = page == item,
                onClick = { onPageChange(item) },
                text = {
                    Text(
                        text = pageLabel(item, sheetCount),
                        maxLines = 1,
                    )
                },
                icon = { Icon(item.icon, contentDescription = null) },
            )
        }
    }
}

@Composable
private fun pageLabel(page: WorkspacePage, sheetCount: Int): String {
    return if (page == WorkspacePage.BOOK) {
        stringResource(R.string.nav_book_count, sheetCount)
    } else {
        stringResource(page.labelRes)
    }
}
