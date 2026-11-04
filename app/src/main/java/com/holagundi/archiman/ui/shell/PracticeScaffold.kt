@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui.shell

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.holagundi.archiman.R

@Composable
fun PracticeScaffold(
    destination: PracticeDestination,
    onDestinationChange: (PracticeDestination) -> Unit,
    onNewProject: (() -> Unit)?,
    onSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                NavigationRail {
                    PracticeDestination.entries.forEach { item ->
                        NavigationRailItem(
                            selected = destination == item,
                            onClick = { onDestinationChange(item) },
                            icon = { DestinationIcon(item, destination == item) },
                            label = { Text(stringResource(item.labelRes)) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(destination.labelRes),
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.semantics { heading() },
                            )
                        },
                        actions = {
                            if (onSignOut != null) {
                                IconButton(onClick = onSignOut) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Logout,
                                        contentDescription = stringResource(R.string.sign_out),
                                    )
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!wide) {
                        Column(Modifier.navigationBarsPadding()) {
                            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                                PracticeDestination.entries.forEach { item ->
                                    NavigationBarItem(
                                        selected = destination == item,
                                        onClick = { onDestinationChange(item) },
                                        icon = { DestinationIcon(item, destination == item) },
                                        label = { Text(stringResource(item.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        alwaysShowLabel = true,
                                    )
                                }
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (onNewProject != null) {
                        ExtendedFloatingActionButton(
                            onClick = onNewProject,
                            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.new_project)) },
                        )
                    }
                },
                content = content,
            )
        }
    }
}

@Composable
private fun DestinationIcon(item: PracticeDestination, selected: Boolean) {
    Icon(
        imageVector = if (selected) item.selectedIcon else item.icon,
        contentDescription = null,
    )
}

@Composable
fun EmptyDestination(message: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(24.dp)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
