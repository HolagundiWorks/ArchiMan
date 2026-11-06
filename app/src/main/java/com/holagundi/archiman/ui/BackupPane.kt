package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.holagundi.archiman.R
import com.holagundi.archiman.data.BackupRepository
import com.holagundi.archiman.data.PreparedBackup
import com.holagundi.archiman.data.BackupArchive
import com.holagundi.archiman.domain.PasswordEnvelope
import java.io.ByteArrayOutputStream
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backups: BackupRepository,
) : ViewModel() {
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _notice = MutableStateFlow<String?>(null)
    val notice = _notice.asStateFlow()
    private val _prepared = MutableStateFlow<PreparedBackup?>(null)
    val prepared = _prepared.asStateFlow()
    private val _restored = MutableStateFlow(false)
    val restored = _restored.asStateFlow()

    fun export(uri: Uri, password: String, recovery: Boolean = false) = operation {
        val raw = ByteArrayOutputStream().also { if (recovery) backups.exportRecovery(it) else backups.export(it) }.toByteArray()
        val secret = password.toCharArray()
        val protected = try { PasswordEnvelope.encrypt(raw, secret, BackupArchive.safeByteLimit()) } finally { secret.fill('\u0000'); raw.fill(0) }
        context.contentResolver.openOutputStream(uri, "wt")?.use {
            it.write(protected)
        } ?: error("Could not open the destination.")
        _notice.value = context.getString(R.string.backup_saved)
    }

    fun prepare(uri: Uri, password: String) = operation {
        _prepared.value = null
        val bytes = context.contentResolver.openInputStream(uri)?.use { PasswordEnvelope.readBounded(it, BackupArchive.safeByteLimit() + 128) } ?: error("Could not read the backup.")
        val secret = password.toCharArray()
        val plain = try { if (PasswordEnvelope.isEncrypted(bytes)) PasswordEnvelope.decrypt(bytes, secret, BackupArchive.safeByteLimit()) else bytes } finally { secret.fill('\u0000') }
        try { _prepared.value = backups.prepare(plain.inputStream()) } finally { plain.fill(0) }
    }

    fun dismiss() { if (!_busy.value) _prepared.value = null }
    fun consumeRestored() { _restored.value = false }

    fun restore() {
        val candidate = _prepared.value ?: return
        operation {
            backups.restore(candidate)
            _prepared.value = null
            _notice.value = context.getString(R.string.backup_restored)
            _restored.value = true
        }
    }

    private fun operation(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _notice.value = null
        _failed.value = false
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { block() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _failed.value = true
                _notice.value = error.message ?: context.getString(R.string.backup_failed)
            } finally {
                _busy.value = false
            }
        }
    }
}

@Composable
fun BackupPane(onRestored: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val prepared by viewModel.prepared.collectAsStateWithLifecycle()
    val restored by viewModel.restored.collectAsStateWithLifecycle()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var action by remember { mutableStateOf<String?>(null) }
    var source by remember { mutableStateOf<Uri?>(null) }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.export(it, password) }; password = ""; confirmation = ""
    }
    val recovery = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { viewModel.export(it, password, recovery = true) }; password = ""; confirmation = ""
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { source = uri; action = "restore" }
    }
    LaunchedEffect(restored) {
        if (restored) {
            viewModel.consumeRestored()
            onRestored()
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.backup_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.backup_description))
        Text(stringResource(R.string.backup_privacy), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(enabled = !busy, onClick = { action = "export" }) {
            Text(stringResource(R.string.backup_export))
        }
        OutlinedButton(enabled = !busy, onClick = { restore.launch(arrayOf("*/*")) }) {
            Text(stringResource(R.string.backup_choose))
        }
        OutlinedButton(enabled = !busy, onClick = { action = "recovery" }) {
            Text(stringResource(R.string.backup_recovery))
        }
        if (busy) Text(stringResource(R.string.backup_working))
        notice?.let { com.holagundi.archiman.ui.theme.CarbonInlineNotification(if(failed) "Backup needs attention" else "Backup updated",it,if(failed) com.holagundi.archiman.ui.theme.CarbonNoticeKind.ERROR else com.holagundi.archiman.ui.theme.CarbonNoticeKind.SUCCESS) }
        ProtectedFilesPane()
    }
    action?.let { current ->
        val restoring = current == "restore"
        AlertDialog(onDismissRequest = { action = null; password = ""; confirmation = ""; source = null },
            title = { Text(if (restoring) "Open backup" else "Protect backup") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (restoring) "Enter its password. Leave blank only for an older unencrypted backup." else "Choose a unique password of at least 12 characters. Store it separately; a forgotten password cannot be recovered.")
                OutlinedTextField(password, { if (it.length <= 256) password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), singleLine = true)
                if (!restoring) OutlinedTextField(confirmation, { if (it.length <= 256) confirmation = it }, label = { Text("Confirm password") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false), singleLine = true)
            } },
            confirmButton = { TextButton(enabled = restoring || (password.length >= 12 && password == confirmation), onClick = {
                action = null
                when (current) {
                    "restore" -> { source?.let { viewModel.prepare(it, password) }; source = null; password = ""; confirmation = "" }
                    "recovery" -> recovery.launch("ArchiMan-recovery.archimanbackup")
                    else -> export.launch("ArchiMan-${System.currentTimeMillis()}.archimanbackup")
                }
            }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { action = null; password = ""; confirmation = ""; source = null }) { Text("Cancel") } })
    }
    prepared?.preview?.let { preview ->
        AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.backup_preview)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(preview.practice)
                    preview.upgradedFrom?.let { Text("This schema-$it backup will be upgraded while restoring.") }
                    Text(DateFormat.getDateTimeInstance().format(Date(preview.createdAt)))
                    Text(stringResource(R.string.backup_counts, preview.projects, preview.records, preview.attachments))
                    if (preview.missingAttachments > 0) Text(pluralStringResource(R.plurals.backup_missing, preview.missingAttachments, preview.missingAttachments), color = MaterialTheme.colorScheme.error)
                    Text(stringResource(R.string.backup_replace_warning))
                }
            },
            confirmButton = { TextButton(enabled = !busy, onClick = viewModel::restore) { Text(stringResource(R.string.backup_replace)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = viewModel::dismiss) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
