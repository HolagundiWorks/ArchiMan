package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class CarbonTab(
    val label: String,
    val count: Int? = null,
    /** Contained tabs only — secondary label under the primary label. */
    val secondaryLabel: String? = null,
)

enum class CarbonTabStyle {
    /** Default Carbon line tabs — primary underline under the selected label. */
    Line,

    /** Contained tabs — filled cells with primary accent on the top edge of the selected tab. */
    Contained,
}

/** IBM Carbon tabs. Use [CarbonTabStyle.Contained] for project/tag switching over a content panel. */
@Composable
fun CarbonTabs(
    tabs: List<CarbonTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: CarbonTabStyle = CarbonTabStyle.Line,
) {
    when (style) {
        CarbonTabStyle.Line -> CarbonLineTabs(tabs, selectedIndex, onSelected, modifier)
        CarbonTabStyle.Contained -> CarbonContainedTabs(tabs, selectedIndex, onSelected, modifier)
    }
}

@Composable
private fun CarbonLineTabs(
    tabs: List<CarbonTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.Bottom,
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                Column(
                    Modifier
                        .semantics {
                            this.selected = selected
                            role = Role.Tab
                        }
                        .clickable { onSelected(index) }
                        .padding(horizontal = CarbonSpacing.card, vertical = CarbonSpacing.stackTight),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
                ) {
                    TabLabel(tab = tab, selected = selected, emphasized = true)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f),
                            ),
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
private fun CarbonContainedTabs(
    tabs: List<CarbonTab>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tabs.isEmpty()) return
    Row(
        modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.Bottom,
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Column(
                Modifier
                    .widthIn(min = 96.dp)
                    .heightIn(min = 48.dp)
                    .semantics {
                        this.selected = selected
                        role = Role.Tab
                    }
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                    )
                    .clickable { onSelected(index) },
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                        ),
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = CarbonSpacing.card, vertical = CarbonSpacing.stackTight),
                    verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline),
                ) {
                    TabLabel(tab = tab, selected = selected, emphasized = false, showCountInline = false)
                    val secondary = tab.secondaryLabel ?: tab.count?.toString()
                    if (secondary != null) {
                        Text(
                            secondary,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabLabel(
    tab: CarbonTab,
    selected: Boolean,
    emphasized: Boolean,
    showCountInline: Boolean = true,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tab.label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = when {
                selected && emphasized -> FontWeight.SemiBold
                selected -> FontWeight.SemiBold
                else -> FontWeight.Medium
            },
            color = when {
                selected && emphasized -> MaterialTheme.colorScheme.primary
                selected -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (showCountInline && tab.count != null) {
            Text(
                tab.count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
