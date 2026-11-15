package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

data class CarbonOverflowAction(
    val label: String,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * IBM Carbon Overflow menu — actions behind a ⋮ trigger.
 * @see https://carbondesignsystem.com/components/overflow-menu/usage/
 */
@Composable
fun CarbonOverflowMenu(
    actions: List<CarbonOverflowAction>,
    modifier: Modifier = Modifier,
    contentDescription: String = "More actions",
) {
    if (actions.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(CarbonIcons.MoreVert, contentDescription)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    enabled = action.enabled,
                    leadingIcon = action.icon?.let { icon ->
                        { Icon(icon, contentDescription = null) }
                    },
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}
