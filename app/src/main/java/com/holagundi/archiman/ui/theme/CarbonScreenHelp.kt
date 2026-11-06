@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.holagundi.archiman.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Trailing info control; explanation opens in a bottom sheet until dismissed. */
@Composable
fun CarbonScreenHelp(title: String, paragraphs: List<String>) {
    if (paragraphs.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }) {
        Icon(CarbonIcons.Info, contentDescription = "$title help", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { open = false }, sheetState = sheetState) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                paragraphs.forEach {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                CarbonButton(onClick = { open = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Got it")
                }
            }
        }
    }
}
