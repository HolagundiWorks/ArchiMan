package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class CarbonNoticeKind { INFO, SUCCESS, WARNING, ERROR }

/**
 * Carbon inline notification card: status stripe, icon, title, body lines, optional close.
 */
@Composable
fun CarbonInlineNotification(
    title: String,
    body: String,
    kind: CarbonNoticeKind = CarbonNoticeKind.INFO,
    modifier: Modifier = Modifier,
    lines: List<String> = emptyList(),
    onDismiss: (() -> Unit)? = null,
) {
    val support = carbonSupport
    val foreground = when (kind) {
        CarbonNoticeKind.INFO -> support.info
        CarbonNoticeKind.SUCCESS -> support.success
        CarbonNoticeKind.WARNING -> support.warning
        CarbonNoticeKind.ERROR -> MaterialTheme.colorScheme.error
    }
    val background = when (kind) {
        CarbonNoticeKind.INFO -> support.infoContainer
        CarbonNoticeKind.SUCCESS -> support.successContainer
        CarbonNoticeKind.WARNING -> support.warningContainer
        CarbonNoticeKind.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val icon = when (kind) {
        CarbonNoticeKind.INFO -> CarbonIcons.Info
        CarbonNoticeKind.SUCCESS -> CarbonIcons.CheckCircle
        CarbonNoticeKind.WARNING -> CarbonIcons.Warning
        CarbonNoticeKind.ERROR -> CarbonIcons.ErrorOutline
    }
    val bodyLines = when {
        lines.isNotEmpty() -> lines
        body.isBlank() -> emptyList()
        else -> body.lines().map { it.trimEnd() }.filter { it.isNotBlank() }.ifEmpty { listOf(body) }
    }

    Row(
        modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(background, RectangleShape)
            .border(1.dp, foreground.copy(alpha = 0.35f), RectangleShape)
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                contentDescription = buildString {
                    append(title)
                    bodyLines.forEach {
                        append(". ")
                        append(it)
                    }
                }
            },
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(foreground),
        )
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .padding(CarbonSpacing.cardDense)
                .size(20.dp),
            tint = foreground,
        )
        Column(
            Modifier
                .weight(1f)
                .padding(
                    top = CarbonSpacing.cardDense,
                    end = if (onDismiss == null) CarbonSpacing.card else CarbonSpacing.micro,
                    bottom = CarbonSpacing.cardDense,
                ),
            verticalArrangement = Arrangement.spacedBy(CarbonSpacing.micro),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = foreground)
            bodyLines.forEach { line ->
                Text(line, style = MaterialTheme.typography.bodyMedium, color = foreground)
            }
        }
        if (onDismiss != null) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = CarbonSpacing.micro, end = CarbonSpacing.micro),
            ) {
                Icon(CarbonIcons.Close, contentDescription = "Dismiss notification", tint = foreground)
            }
        }
    }
}
