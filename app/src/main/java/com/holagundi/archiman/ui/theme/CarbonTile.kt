package com.holagundi.archiman.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Top alert stripe on tiles/KPIs — green OK, yellow attention, red critical. */
enum class CarbonAlertTone { NONE, OK, WARNING, CRITICAL }

/**
 * Only three tile footprints:
 * - [SMALL] — 5 across, compact KPI band
 * - [MEDIUM] — 3 across, same collapsed height as large
 * - [LARGE] — 1 across, same collapsed height as medium; both expand for overflow
 */
enum class CarbonTileSize(val columns: Int) {
    SMALL(5),
    MEDIUM(3),
    LARGE(1),
}

object CarbonTileMetrics {
    /** Collapsed band shared by medium and large. */
    val bandHeight: Dp = 152.dp

    /** Compact KPI / small tile height. */
    val smallHeight: Dp = 96.dp

    val gridGap: Dp = CarbonSpacing.stackTight
}

@Composable
fun CarbonAlertTone.color(): Color = when (this) {
    CarbonAlertTone.NONE -> Color.Transparent
    CarbonAlertTone.OK -> carbonSupport.success
    CarbonAlertTone.WARNING -> Color(0xFFF1C21B)
    CarbonAlertTone.CRITICAL -> MaterialTheme.colorScheme.error
}

/** Maps task priority / issue severity to the tile top alert stripe. */
fun priorityAlertTone(priority: String): CarbonAlertTone =
    when (com.holagundi.archiman.domain.WorkPriority.normalize(priority)) {
        com.holagundi.archiman.domain.WorkPriority.CRITICAL -> CarbonAlertTone.CRITICAL
        com.holagundi.archiman.domain.WorkPriority.ATTENTION -> CarbonAlertTone.WARNING
        else -> CarbonAlertTone.OK
    }

/**
 * IBM Carbon Tile — base / clickable container (no elevation).
 * Sized via [CarbonTileSize]; medium/large share [CarbonTileMetrics.bandHeight].
 */
@Composable
fun CarbonTile(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Boolean = true,
    alert: CarbonAlertTone = CarbonAlertTone.NONE,
    size: CarbonTileSize = CarbonTileSize.MEDIUM,
    content: @Composable ColumnScope.() -> Unit,
) {
    val padding = if (contentPadding) Modifier.padding(CarbonSpacing.cardDense) else Modifier
    val sized = when (size) {
        CarbonTileSize.SMALL -> Modifier.height(CarbonTileMetrics.smallHeight)
        CarbonTileSize.MEDIUM, CarbonTileSize.LARGE -> Modifier.heightIn(min = CarbonTileMetrics.bandHeight)
    }
    val body: @Composable ColumnScope.() -> Unit = {
        if (alert != CarbonAlertTone.NONE) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(alert.color()),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (size == CarbonTileSize.SMALL) Modifier.fillMaxHeight() else Modifier)
                .then(padding),
            content = content,
        )
    }
    if (onClick == null) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .then(sized),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RectangleShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = { Column(Modifier.fillMaxSize(), content = body) },
        )
    } else {
        Surface(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .then(sized),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = RectangleShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            content = { Column(Modifier.fillMaxSize(), content = body) },
        )
    }
}

/**
 * Medium or large expandable tile. Collapsed height matches [CarbonTileMetrics.bandHeight];
 * expands when more content must be shown.
 */
@Composable
fun CarbonExpandableTile(
    title: String,
    summary: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    menu: @Composable (() -> Unit)? = null,
    alert: CarbonAlertTone = CarbonAlertTone.NONE,
    size: CarbonTileSize = CarbonTileSize.LARGE,
    content: @Composable ColumnScope.() -> Unit,
) {
    require(size != CarbonTileSize.SMALL) { "Expandable tiles are medium or large only." }
    val band = Modifier
        .fillMaxWidth()
        .then(
            if (expanded) Modifier.heightIn(min = CarbonTileMetrics.bandHeight)
            else Modifier.height(CarbonTileMetrics.bandHeight),
        )
    Surface(
        modifier = modifier.then(band).clipToBounds(),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RectangleShape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(Modifier.fillMaxSize()) {
            if (alert != CarbonAlertTone.NONE) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(alert.color()),
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = CarbonSpacing.cardDense,
                        end = CarbonSpacing.micro,
                        top = CarbonSpacing.micro,
                        bottom = CarbonSpacing.micro,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier
                        .weight(1f)
                        .clickable { onExpandedChange(!expanded) }
                        .padding(vertical = CarbonSpacing.stackTight, horizontal = CarbonSpacing.micro),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
                ) {
                    if (leadingIcon != null) {
                        Icon(leadingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CarbonSpacing.hairline)) {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (summary.isNotBlank()) {
                            Text(
                                summary,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = if (expanded) 3 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(
                        CarbonIcons.ChevronDown,
                        contentDescription = if (expanded) "Hide details" else "Show details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.graphicsLayer { rotationZ = if (expanded) 180f else 0f },
                    )
                }
                menu?.invoke()
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = CarbonSpacing.card,
                            end = CarbonSpacing.card,
                            bottom = CarbonSpacing.card,
                        ),
                    verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
                    content = content,
                )
            }
        }
    }
}

/**
 * Places children in a Small (5), Medium (3), or Large (1) column grid.
 * Medium/large cells share [CarbonTileMetrics.bandHeight] unless a child expands.
 */
@Composable
fun CarbonTileGrid(
    size: CarbonTileSize,
    modifier: Modifier = Modifier,
    content: CarbonTileGridScope.() -> Unit,
) {
    val scope = CarbonTileGridScope().apply(content)
    val columns = size.columns
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CarbonTileMetrics.gridGap),
    ) {
        scope.items.chunked(columns).forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .then(
                        when (size) {
                            CarbonTileSize.SMALL -> Modifier.height(CarbonTileMetrics.smallHeight)
                            CarbonTileSize.MEDIUM, CarbonTileSize.LARGE -> Modifier.heightIn(min = CarbonTileMetrics.bandHeight)
                        },
                    ),
                horizontalArrangement = Arrangement.spacedBy(CarbonTileMetrics.gridGap),
            ) {
                row.forEach { item ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    ) { item() }
                }
                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

class CarbonTileGridScope internal constructor() {
    internal val items = mutableListOf<@Composable () -> Unit>()
    fun item(content: @Composable () -> Unit) {
        items += content
    }
}

/** Compact KPI content for [CarbonTileSize.SMALL] cells. */
@Composable
fun CarbonSmallTileContent(
    label: String,
    value: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = CarbonSpacing.stackTight, vertical = CarbonSpacing.stackTight),
        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
        horizontalAlignment = Alignment.Start,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.height(16.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Medium/large body that stays within the shared band until [expanded], then grows.
 * Use inside a sized [CarbonTile] when you need overflow without the disclosure header.
 */
@Composable
fun CarbonTileExpandableBody(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    collapsedLabel: String = "Show more",
    expandedLabel: String = "Show less",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (expanded) Modifier else Modifier.heightIn(max = CarbonTileMetrics.bandHeight - 40.dp))
                .clipToBounds()
                .then(if (expanded) Modifier else Modifier),
            verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
            content = content,
        )
        Text(
            if (expanded) expandedLabel else collapsedLabel,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .clickable { onExpandedChange(!expanded) }
                .padding(top = CarbonSpacing.stackTight),
        )
    }
}

@Composable
fun rememberTileExpanded(key: String, initial: Boolean = false): Pair<Boolean, (Boolean) -> Unit> {
    var expanded by rememberSaveable(key) { mutableStateOf(initial) }
    return expanded to { expanded = it }
}
