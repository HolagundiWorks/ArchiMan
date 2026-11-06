package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import com.holagundi.archiman.ui.theme.CarbonIcons
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.holagundi.archiman.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.data.RecordOperations
import com.holagundi.archiman.data.RecordSnapshot
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.ChoiceChip
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class RecordOperationsViewModel @Inject constructor(val operations: RecordOperations) : ViewModel()

private data class RecordMenuChoice(val action: String, val label: String, val icon: ImageVector, val restriction: String? = null)

/** Shared controls keep edit/save/cancel and destructive confirmations consistent on every list. */
@Composable
fun RecordActions(kind: RecordKind, id: Long, title: String, onDeleted: () -> Unit = {}) = key(kind, id) {
    val repository = hiltViewModel<RecordOperationsViewModel>().operations
    val scope = rememberCoroutineScope()
    var action by remember { mutableStateOf<String?>(null) }
    var snapshot by remember { mutableStateOf<RecordSnapshot?>(null) }
    var fields by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reason by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var clients by remember { mutableStateOf<List<Pair<Long, String>>>(emptyList()) }
    var menu by remember { mutableStateOf(false) }
    var choices by remember { mutableStateOf<List<RecordMenuChoice>>(emptyList()) }
    var result by remember { mutableStateOf<String?>(null) }
    var undo by remember { mutableStateOf<(suspend () -> Unit)?>(null) }
    var leaveEditor by remember { mutableStateOf(false) }
    fun open(next: String) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                val loaded = withContext(Dispatchers.IO) { repository.read(kind, id) }
                if (next == "Edit" || next == "Delete") withContext(Dispatchers.IO) { repository.ensureAllowed(loaded, next == "Delete") }
                if (kind == RecordKind.PROJECT) clients = withContext(Dispatchers.IO) { repository.clientChoices() }
                snapshot = loaded
                fields = kind.fields.associate { it.column to loaded.text(it.column) }
                reason = ""
                confirmation = ""
                error = null
                action = next
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (failure: Exception) { error = failure.message ?: "Could not open this record."
            } finally { busy = false }
        }
    }
    Box {
        IconButton(enabled = !busy, onClick = {
            busy = true
            menu = true
            choices = emptyList()
            scope.launch {
                try {
                    val loaded = withContext(Dispatchers.IO) { repository.read(kind, id) }
                    val options = mutableListOf<RecordMenuChoice>()
                    if (kind.fields.isNotEmpty()) options.add(RecordMenuChoice("Edit", "Edit details", CarbonIcons.Edit))
                    if (kind == RecordKind.PROJECT) options.add(RecordMenuChoice("Archive or reopen", if (loaded.text("status") == "ARCHIVED") "Reopen project" else "Archive project", if (loaded.text("status") == "ARCHIVED") CarbonIcons.Unarchive else CarbonIcons.Archive))
                    if (kind == RecordKind.INVOICE && loaded.text("status") == "ISSUED") options.add(RecordMenuChoice("Void invoice", "Void invoice", CarbonIcons.Block))
                    if (kind != RecordKind.PRACTICE && kind != RecordKind.TRANSMITTAL) options.add(RecordMenuChoice("Delete", "Delete", CarbonIcons.DeleteOutline))
                    choices = options.map { option ->
                        if (option.action == "Edit" || option.action == "Delete") {
                            val restriction = withContext(Dispatchers.IO) {
                                try { repository.ensureAllowed(loaded, option.action == "Delete"); null }
                                catch (failure: IllegalArgumentException) { failure.message }
                                catch (failure: IllegalStateException) { failure.message }
                            }
                            option.copy(restriction = restriction)
                        } else option
                    }
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) { error = failure.message; menu = false
                } finally { busy = false }
            }
        }) { Icon(CarbonIcons.MoreVert, stringResource(R.string.more_actions, title)) }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (busy) DropdownMenuItem(text = { Text("Loading actions…") }, onClick = {}, enabled = false)
            if (!busy && choices.isEmpty()) DropdownMenuItem(text = { Text("Issued records retain their history") }, onClick = {}, enabled = false)
            choices.forEach { choice ->
                DropdownMenuItem(
                    text = { Column { Text(choice.label); choice.restriction?.let { Text(it, style = MaterialTheme.typography.bodySmall) } } },
                    leadingIcon = { Icon(choice.icon, null) },
                    enabled = !busy && choice.restriction == null,
                    onClick = { menu = false; open(choice.action) },
                )
            }
        }
    }
    if (error != null && action == null) com.holagundi.archiman.ui.theme.CarbonInlineNotification("Action unavailable",error!!,com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR)
    result?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification("Record updated", it, com.holagundi.archiman.ui.theme.CarbonNoticeKind.SUCCESS) }
    undo?.let { reverse -> TextButton(enabled = !busy, onClick = {
        busy = true
        scope.launch {
            try { withContext(Dispatchers.IO) { reverse() }; result = "$title: previous details restored."; undo = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "Could not undo. The record may have changed."; undo = null }
            finally { busy = false }
        }
    }) { Text("Undo last change") } }
    val current = snapshot
    val selected = action
    if (selected != null && current != null) {
        val draft = rememberLocalEditorDraft("record:${kind.name}:$id:${current.values.toSortedMap()}")
        fun close() {
            if (!busy) {
                if (selected == "Edit" && fields != kind.fields.associate { it.column to current.text(it.column) }) leaveEditor = true
                else action = null
            }
        }
        val archived = current.text("status") == "ARCHIVED"
        val heading = if (selected == "Archive or reopen") { if (archived) "Reopen" else "Archive" } else selected
        AlertDialog(
            onDismissRequest = ::close,
            title = { Text("$heading: $title") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (selected) {
                        "Edit" -> {
                            Text(draft.status, style = MaterialTheme.typography.bodySmall)
                            draft.recovered?.let { recovered -> TextButton(onClick = {
                                val json = org.json.JSONObject(recovered)
                                fields = kind.fields.associate { it.column to json.optString(it.column, current.text(it.column)) }
                                draft.recovered = null
                            }) { Text("Resume local draft") } }
                            kind.fields.forEach { field ->
                            if (field.column == "clientId") {
                                Text("Client")
                                clients.forEach { (clientId, label) ->
                                    ChoiceChip(label, fields[field.column] == clientId.toString()) { fields = fields + (field.column to clientId.toString()); draft.save(org.json.JSONObject(fields).toString()) }
                                }
                            } else ArchiField(fields[field.column].orEmpty(), { fields = fields + (field.column to it); draft.save(org.json.JSONObject(fields).toString()) }, field.label + if (field.required) " (required)" else "", enabled = !busy, errorText = if (field.number && fields[field.column].orEmpty().isNotBlank() && fields[field.column]?.toDoubleOrNull()?.let { it.isFinite() && it >= 0 } != true) "Enter a valid non-negative number." else null, singleLine = field.column !in setOf("body", "description", "observation", "discussion", "decisions", "actionItems", "notes", "questionOrRequirement", "workCompleted", "remarks", "correctiveAction", "address"), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = when {
                                field.number -> androidx.compose.ui.text.input.KeyboardType.Decimal
                                field.column.contains("phone", true) || field.column == "contactNo" -> androidx.compose.ui.text.input.KeyboardType.Phone
                                field.column.contains("email", true) -> androidx.compose.ui.text.input.KeyboardType.Email
                                else -> androidx.compose.ui.text.input.KeyboardType.Text
                            }))
                        }
                        }
                        "Delete" -> {
                            Text(if (kind == RecordKind.PROJECT) "This permanently deletes the project and all its tasks, documents, site records, measurements, invoices and receipts. Shared clients and library items remain. Save a backup first." else if (kind == RecordKind.SHEET) "This deletes the sheet, all its entries and review comments. Only draft or returned sheets can be deleted." else "Permanently delete this record? Linked or approved records may prevent deletion.")
                            if (kind == RecordKind.PROJECT) ArchiField(confirmation, { confirmation = it }, "Type DELETE to confirm")
                        }
                        "Void invoice" -> {
                            Text("Voiding retains the invoice and its history. Payments must be reversed first.")
                            ArchiField(reason, { reason = it }, "Reason", singleLine = false)
                        }
                        else -> Text(if (archived) "Reopen this project as active? Its records will be retained." else "Archive this project? Its records will be retained and it can be reopened.")
                    }
                    error?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification("Check your entries",it,com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR) }
                    if (busy) Text("Saving…")
                }
            },
            confirmButton = {
                TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = if (selected in listOf("Delete", "Void invoice")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary), enabled = !busy && (selected != "Edit" || kind.fields.all { (!it.required || !fields[it.column].isNullOrBlank()) && (!it.number || fields[it.column]?.toDoubleOrNull()?.let { n -> n.isFinite() && n >= 0 } == true) }) && (selected != "Delete" || kind != RecordKind.PROJECT || confirmation == "DELETE") && (selected != "Void invoice" || reason.isNotBlank()), onClick = {
                    busy = true
                    scope.launch {
                        try {
                            val committed = withContext(Dispatchers.IO) {
                                when (selected) {
                                    "Edit" -> repository.saveWithSnapshot(current, fields)
                                    "Delete" -> { repository.delete(current); null }
                                    "Void invoice" -> { repository.voidInvoice(current, reason); null }
                                    else -> repository.archiveWithSnapshot(current)
                                }
                            }
                            undo = when (selected) {
                                "Edit" -> {
                                    val savedSnapshot = requireNotNull(committed)
                                    val previous = kind.fields.associate { it.column to current.text(it.column) }
                                    suspend { repository.save(savedSnapshot, previous) }
                                }
                                "Archive or reopen" -> {
                                    val savedSnapshot = requireNotNull(committed)
                                    suspend { repository.archiveProject(savedSnapshot) }
                                }
                                else -> null
                            }
                            action = null
                            error = null
                            draft.clear()
                            result = when (selected) { "Edit" -> "$title: details saved."; "Delete" -> "$title deleted."; "Void invoice" -> "$title voided; its history is retained."; else -> "$title ${if (archived) "reopened" else "archived"}." }
                            if (selected == "Delete") onDeleted()
                        } catch (cancelled: CancellationException) { throw cancelled
                        } catch (failure: Exception) { error = failure.message ?: "Could not save this change."
                        } finally { busy = false }
                    }
                }) { Text(if (selected == "Edit") "Save changes" else heading) }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = ::close) { Text("Cancel") } },
        )
        if (leaveEditor) AlertDialog(onDismissRequest = { leaveEditor = false }, title = { Text("Keep your changes?") }, text = { Column { Text("A local draft can be resumed by reopening Edit details. Saved record details stay unchanged until you save."); Text(draft.status) } },
            confirmButton = { TextButton(enabled = draft.status == "Local draft saved on this device", onClick = { leaveEditor = false; action = null }) { Text("Keep draft and close") } },
            dismissButton = { Column {
                TextButton(onClick = { leaveEditor = false }) { Text("Continue editing") }
                TextButton(onClick = { draft.clear(); leaveEditor = false; action = null }) { Text("Discard changes") }
            } })
    }
}

@Composable
fun ReceiptReversal(id: Long) = key(id) {
    val repository = hiltViewModel<RecordOperationsViewModel>().operations
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), onClick = { open = true }) { Text("Reverse receipt") }
    if (open) AlertDialog(
        onDismissRequest = { if (!busy) open = false },
        title = { Text("Reverse receipt?") },
        text = { Column { Text("The original receipt is retained. A reversal reopens the invoice balance."); ArchiField(reason, { reason = it }, "Reason", singleLine = false); error?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification("Check your entries",it,com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR) } } },
        confirmButton = { TextButton(colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error), enabled = !busy && reason.isNotBlank(), onClick = {
            busy = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { repository.reverseReceipt(id, reason) }
                    open = false
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (failure: Exception) { error = failure.message ?: "Could not reverse receipt."
                } finally { busy = false }
            }
        }) { Text("Reverse receipt") } },
        dismissButton = { TextButton(enabled = !busy, onClick = { open = false }) { Text("Cancel") } },
    )
}
