@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui.shell

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import com.holagundi.archiman.ui.theme.CarbonNavigationBar
import com.holagundi.archiman.ui.theme.CarbonNavigationRail
import com.holagundi.archiman.ui.theme.CarbonNavigationItem
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    var accountMenu by remember { mutableStateOf(false) }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                CarbonNavigationRail {
                    PracticeDestination.entries.forEach { item ->
                        CarbonNavigationItem(stringResource(item.labelRes),item.icon,destination==item,{onDestinationChange(item)},rail=true)
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
                                    IconButton(onClick = { accountMenu = true }) { Icon(CarbonIcons.MoreVert, "Account menu") }
                                    DropdownMenu(expanded = accountMenu, onDismissRequest = { accountMenu = false }) {
                                        DropdownMenuItem(text = { Text(stringResource(R.string.sign_out)) }, leadingIcon = { Icon(CarbonIcons.Logout, null) }, onClick = { accountMenu = false; onSignOut() })
                                    }
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    if(!wide) CarbonNavigationBar {
                        PracticeDestination.entries.forEach { item ->
                            CarbonNavigationItem(stringResource(item.labelRes),item.icon,destination==item,{onDestinationChange(item)},Modifier.weight(1f))
                        }
                    }
                },
                floatingActionButton = {
                    if (onNewProject != null) {
                        ExtendedFloatingActionButton(
                            shape = androidx.compose.ui.graphics.RectangleShape,
                            onClick = onNewProject,
                            icon = { Icon(CarbonIcons.Add, contentDescription = null) },
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
