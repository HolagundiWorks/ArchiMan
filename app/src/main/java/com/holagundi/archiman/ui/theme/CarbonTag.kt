package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * IBM Carbon Tag — labels for status and filterable selections.
 * @see https://carbondesignsystem.com/components/tag/usage/
 */
@Composable
fun CarbonTag(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    alert: CarbonAlertTone = CarbonAlertTone.NONE,
    onClick: (() -> Unit)? = null,
) {
    val colors = when {
        selected -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        alert == CarbonAlertTone.CRITICAL -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        alert == CarbonAlertTone.WARNING -> carbonSupport.warningContainer to carbonSupport.warning
        alert == CarbonAlertTone.OK -> carbonSupport.successContainer to carbonSupport.success
        else -> MaterialTheme.colorScheme.surfaceContainerHigh to MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .heightIn(min = 24.dp)
            .semantics {
                this.selected = selected
                if (onClick != null) role = Role.Checkbox
            }
            .background(colors.first, RectangleShape)
            .then(
                if (onClick != null) {
                    Modifier
                        .border(
                            width = 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RectangleShape,
                        )
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = CarbonSpacing.stackTight, vertical = CarbonSpacing.micro),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
    ) {
        if (selected && onClick != null) {
            Icon(
                CarbonIcons.Check,
                contentDescription = null,
                tint = colors.second,
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.second)
    }
}
