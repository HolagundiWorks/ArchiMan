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
import com.holagundi.archiman.ui.theme.CarbonBreadcrumb
import com.holagundi.archiman.ui.theme.CarbonBreadcrumbs
import com.holagundi.archiman.ui.theme.CarbonNavigationBar
import com.holagundi.archiman.ui.theme.CarbonNavigationRail
import com.holagundi.archiman.ui.theme.CarbonNavigationItem
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.holagundi.archiman.ui.theme.CarbonOverflowAction
import com.holagundi.archiman.ui.theme.CarbonOverflowMenu
import com.holagundi.archiman.ui.theme.CarbonSpacing

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
    breadcrumbs: List<CarbonBreadcrumb> = emptyList(),
    onSearch: (() -> Unit)? = null,
    searchOpen: Boolean = false,
    createActions: List<PracticeFabAction> = emptyList(),
    onSignOut: (() -> Unit)? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
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
                            if (breadcrumbs.isNotEmpty()) {
                                CarbonBreadcrumbs(
                                    breadcrumbs,
                                    modifier = Modifier.semantics { heading() },
                                )
                            } else {
                                Text(
                                    text = stringResource(destination.labelRes),
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.semantics { heading() },
                                )
                            }
                        },
                        actions = {
                            if (onSignOut != null) {
                                CarbonOverflowMenu(
                                    contentDescription = "Account menu",
                                    actions = listOf(
                                        CarbonOverflowAction(
                                            label = stringResource(R.string.sign_out),
                                            icon = CarbonIcons.Logout,
                                            onClick = onSignOut,
                                        ),
                                    ),
                                )
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
                                        horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
                                    ) {
                                        // A solid chip keeps the label readable over the page content behind the menu.
                                        androidx.compose.material3.Surface(
                                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                            shape = RectangleShape,
                                            shadowElevation = 6.dp,
                                        ) {
                                            Text(
                                                action.label,
                                                Modifier.padding(horizontal = CarbonSpacing.card, vertical = CarbonSpacing.stackTight),
                                                style = MaterialTheme.typography.labelLarge,
                                            )
                                        }
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
                                    Spacer(Modifier.height(CarbonSpacing.stack))
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
                                Spacer(Modifier.height(CarbonSpacing.stack))
                            }
                            if (createActions.isNotEmpty()) {
                                FloatingActionButton(
                                    onClick = {
                                        if (createActions.size == 1) {
                                            createActions.first().onClick()
                                        } else {
                                            createOpen = !createOpen
                                        }
                                    },
                                    shape = RectangleShape,
                                ) {
                                    Icon(
                                        if (createOpen) CarbonIcons.Close else CarbonIcons.Add,
                                        if (createOpen) {
                                            stringResource(R.string.close_create_menu)
                                        } else {
                                            stringResource(R.string.create)
                                        },
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
