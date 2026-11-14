package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * IBM Carbon transactional Modal — header, body, footer with Cancel + primary.
 * @see https://carbondesignsystem.com/components/modal/usage/
 */
@Composable
fun CarbonModal(
    title: String,
    onDismiss: () -> Unit,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    description: String? = null,
    primaryEnabled: Boolean = true,
    primaryBusy: Boolean = false,
    secondaryLabel: String = "Cancel",
    danger: Boolean = false,
    showFooterActions: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Dialog(
        onDismissRequest = { if (!primaryBusy) onDismiss() },
        properties = DialogProperties(
            decorFitsSystemWindows = false,
            usePlatformDefaultWidth = true,
            dismissOnBackPress = !primaryBusy,
            dismissOnClickOutside = !primaryBusy,
        ),
    ) {
        Box(
            Modifier
                .imePadding()
                .safeDrawingPadding()
                .padding(horizontal = CarbonSpacing.screen),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RectangleShape,
                shadowElevation = 8.dp,
                tonalElevation = 0.dp,
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = CarbonSpacing.card, end = CarbonSpacing.micro, top = CarbonSpacing.stack),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(top = CarbonSpacing.stackTight, end = CarbonSpacing.stackTight),
                            verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stackTight),
                        ) {
                            if (label != null) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                title,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics { heading() },
                            )
                            if (description != null) {
                                Text(
                                    description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(enabled = !primaryBusy, onClick = onDismiss) {
                            Icon(CarbonIcons.Close, contentDescription = "Close")
                        }
                    }
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = CarbonSpacing.card, vertical = CarbonSpacing.stack),
                        verticalArrangement = Arrangement.spacedBy(CarbonSpacing.stack),
                        content = content,
                    )
                    if (showFooterActions) {
                        Row(Modifier.fillMaxWidth()) {
                            CarbonButton(
                                onClick = onDismiss,
                                enabled = !primaryBusy,
                                modifier = Modifier.weight(1f),
                                shape = RectangleShape,
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            ) { Text(secondaryLabel) }
                            CarbonButton(
                                onClick = onPrimary,
                                enabled = primaryEnabled && !primaryBusy,
                                modifier = Modifier.weight(1f),
                                shape = RectangleShape,
                                colors = if (danger) {
                                    androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError,
                                        disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f),
                                        disabledContentColor = MaterialTheme.colorScheme.onError.copy(alpha = 0.38f),
                                    )
                                } else {
                                    androidx.compose.material3.ButtonDefaults.buttonColors()
                                },
                            ) { Text(if (primaryBusy) "Creating…" else primaryLabel) }
                        }
                    }
                }
            }
        }
    }
}
