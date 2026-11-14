package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class CarbonDropdownOption(
    val id: String,
    val label: String,
)

enum class CarbonDropdownSize(val heightDp: Int) {
    Small(32),
    Medium(40),
    Large(48),
}

/**
 * IBM Carbon Dropdown (default style) — single selection for filters and forms.
 * @see https://carbondesignsystem.com/components/dropdown/usage/
 */
@Composable
fun CarbonDropdown(
    label: String,
    options: List<CarbonDropdownOption>,
    selectedId: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose an option",
    helperText: String? = null,
    enabled: Boolean = true,
    size: CarbonDropdownSize = CarbonDropdownSize.Medium,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    val fieldHeight = size.heightDp.dp

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (helperText != null) {
            Text(
                helperText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(fieldHeight)
                    .semantics { contentDescription = label }
                    .background(
                        if (enabled) MaterialTheme.colorScheme.surfaceContainerHigh
                        else MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        RectangleShape,
                    )
                    .then(
                        if (expanded) {
                            Modifier.border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RectangleShape,
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = CarbonSpacing.card),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
            ) {
                Text(
                    text = selected?.label ?: placeholder,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected != null) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    CarbonIcons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = fieldHeight * 6.5f)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            ) {
                // DropdownMenu already scrolls its content; a nested verticalScroll here is measured
                // with infinite height and crashes the app the moment the menu opens.
                options.forEach { option ->
                    val isSelected = option.id == selectedId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(fieldHeight)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerLowest
                                },
                            )
                            .clickable {
                                onSelected(option.id)
                                expanded = false
                            }
                            .padding(horizontal = CarbonSpacing.card),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
                    ) {
                        Text(
                            option.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isSelected) {
                            Icon(
                                CarbonIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
