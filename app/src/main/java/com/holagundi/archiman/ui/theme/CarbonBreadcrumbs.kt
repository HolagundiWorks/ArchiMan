package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

data class CarbonBreadcrumb(
    val label: String,
    val onClick: (() -> Unit)? = null,
)

/** IBM Carbon-style breadcrumb trail. The last item is the current page. */
@Composable
fun CarbonBreadcrumbs(
    items: List<CarbonBreadcrumb>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val description = items.joinToString(" / ") { it.label }
    Row(
        modifier
            .horizontalScroll(rememberScrollState())
            .semantics { contentDescription = "Breadcrumb $description" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
    ) {
        items.forEachIndexed { index, crumb ->
            val current = index == items.lastIndex
            if (index > 0) {
                Text(
                    "/",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val clickable = crumb.onClick != null && !current
            Text(
                crumb.label,
                style = if (current) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    current -> MaterialTheme.colorScheme.onSurface
                    clickable -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (clickable) {
                    Modifier
                        .clickable(onClick = crumb.onClick!!)
                        .padding(vertical = CarbonSpacing.hairline)
                } else {
                    Modifier.padding(vertical = CarbonSpacing.hairline)
                },
            )
        }
    }
}
