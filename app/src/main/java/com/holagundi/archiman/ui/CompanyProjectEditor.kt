package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonSpacing

@Composable
fun CompanyProjectEditor(state: CompanyProjectEditorState, busy: Boolean,
    onChange: (String, String, String) -> Unit, onSave: () -> Unit,
    onReview: (Boolean) -> Unit, onClose: () -> Unit) {
    val draft = state.draft
    var statuses by remember { mutableStateOf(false) }
    Column(verticalArrangement = CarbonSpacing.stackArrangement) {
        Text("Edit company project", style = MaterialTheme.typography.titleMedium)
        Text("Based on revision ${draft.base.version}. This changes the company record; local practice projects are kept separate.")
        Text("This edit is held only while the live screen is open. Save it to the company before leaving.")
        OutlinedTextField(draft.name, { onChange(it, draft.code, draft.status) }, Modifier.fillMaxWidth(),
            label = { Text("Company project name") }, enabled = !busy && !state.sent, singleLine = true)
        OutlinedTextField(draft.code, { onChange(draft.name, it, draft.status) }, Modifier.fillMaxWidth(),
            label = { Text("Company project code") }, enabled = !busy && !state.sent, singleLine = true)
        Box {
            OutlinedButton(enabled = !busy && !state.sent, onClick = { statuses = true }) { Text("Status: ${draft.status.replace('_', ' ')}") }
            DropdownMenu(expanded = statuses, onDismissRequest = { statuses = false }) {
                listOf("active", "on_hold", "completed", "archived").forEach { status ->
                    DropdownMenuItem(text = { Text(status.replace('_', ' ')) }, onClick = { statuses = false; onChange(draft.name, draft.code, status) })
                }
            }
        }
        state.conflict?.let { current ->
            Text("Current company version · Revision ${current.version}", style = MaterialTheme.typography.titleMedium)
            Text("${current.name}\nCode: ${current.code}\nStatus: ${current.status.replace('_', ' ')}")
            Text("Your submitted values remain above. Nothing from this conflicting edit was saved.")
            OutlinedButton(enabled = !busy, onClick = { onReview(true) }) { Text("Review my draft on current revision") }
            OutlinedButton(enabled = !busy, onClick = { onReview(false) }) { Text("Use company values") }
        } ?: run {
            if (state.sent) Text("An interrupted request may already be saved by the company. Reload live data to confirm.")
            Button(enabled = !busy && draft.name.trim().isNotEmpty() && draft.name.trim().length <= 240 && draft.code.trim().length <= 80,
                onClick = onSave) { Text(if (state.sent) "Retry same edit" else "Save company project") }
        }
        OutlinedButton(enabled = !busy, onClick = onClose) { Text("Close editor") }
    }
}
