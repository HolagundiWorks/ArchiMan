@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui.shell

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.holagundi.archiman.ui.theme.CarbonNavigationBar
import com.holagundi.archiman.ui.theme.CarbonNavigationRail
import com.holagundi.archiman.ui.theme.CarbonNavigationItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.holagundi.archiman.R

data class PracticeFabAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
fun PracticeScaffold(
    destination: PracticeDestination,
    onDestinationChange: (PracticeDestination) -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
    searchOpen: Boolean = false,
    createActions: List<PracticeFabAction> = emptyList(),
    onSignOut: (() -> Unit)? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    var accountMenu by remember { mutableStateOf(false) }
    var createOpen by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                CarbonNavigationRail {
                    PracticeDestination.entries.forEach { item ->
                        CarbonNavigationItem(
                            stringResource(item.labelRes),
                            item.icon,
                            destination == item,
                            { onDestinationChange(item) },
                            rail = true,
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
                                Box {
                                    IconButton(onClick = { accountMenu = true }) {
                                        Icon(CarbonIcons.MoreVert, "Account menu")
                                    }
                                    DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.sign_out)) },
                                            leadingIcon = { Icon(CarbonIcons.Logout, null) },
                                            onClick = {
                                                accountMenu = false
                                                onSignOut()
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!wide) {
                        CarbonNavigationBar {
                            PracticeDestination.entries.forEach { item ->
                                CarbonNavigationItem(
                                    stringResource(item.labelRes),
                                    item.icon,
                                    destination == item,
                                    { onDestinationChange(item) },
                                    Modifier.weight(1f),
                                )
                            }
                        }
                    }
                },
                floatingActionButton = {
                    if (onSearch != null || createActions.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            if (createOpen) {
                                createActions.asReversed().forEach { action ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                        Text(action.label, style = MaterialTheme.typography.labelLarge)
                                        SmallFloatingActionButton(
                                            onClick = {
                                                createOpen = false
                                                action.onClick()
                                            },
                                            shape = RectangleShape,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            contentColor = MaterialTheme.colorScheme.primary,
                                        ) {
                                            Icon(action.icon, action.label)
                                        }
                                    }
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                            if (onSearch != null && !searchOpen) {
                                SmallFloatingActionButton(
                                    onClick = {
                                        createOpen = false
                                        onSearch()
                                    },
                                    shape = RectangleShape,
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                ) {
                                    Icon(CarbonIcons.Search, stringResource(R.string.practice_search))
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            if (createActions.isNotEmpty()) {
                                FloatingActionButton(
                                    onClick = { createOpen = !createOpen },
                                    shape = RectangleShape,
                                ) {
                                    Icon(
                                        if (createOpen) CarbonIcons.Close else CarbonIcons.Add,
                                        if (createOpen) stringResource(R.string.close_create_menu) else stringResource(R.string.create),
                                    )
                                }
                            }
                        }
                    }
                },
                content = content,
            )
        }
    }
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
