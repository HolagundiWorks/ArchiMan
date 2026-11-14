package com.holagundi.archiman.ui.theme

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/** Trailing info control; explanation opens in a Carbon passive/acknowledgment modal. */
@Composable
fun CarbonScreenHelp(title: String, paragraphs: List<String>) {
    if (paragraphs.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(CarbonIcons.Info, contentDescription = "$title help", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        CarbonModal(
            title = title,
            onDismiss = { open = false },
            primaryLabel = "Got it",
            onPrimary = { open = false },
            showFooterActions = true,
            secondaryLabel = "Close",
        ) {
            paragraphs.forEach {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
