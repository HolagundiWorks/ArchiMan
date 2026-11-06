package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.holagundi.archiman.data.BackupArchive
import com.holagundi.archiman.domain.PasswordEnvelope
import kotlinx.coroutines.*

/** A portable encrypted wrapper keeps standard PDF/XLSX/CSV interoperability explicit. */
@Composable fun ProtectedFilesPane() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var source by remember { mutableStateOf<Uri?>(null) }
    var decrypt by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var dialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf<ByteArray?>(null) }
    var suggestedName by remember { mutableStateOf("document.archimansecure") }
    DisposableEffect(Unit) { onDispose { ready?.fill(0) } }
    val destination = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes = ready; ready = null
        if (bytes != null) scope.launch {
            busy = true
            try {
                if (uri != null) withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri,"wt")?.use { it.write(bytes) } ?: error("Could not open destination.") }
                notice = if (uri == null) "Cancelled." else if (decrypt) "Decrypted file saved. This copy is readable without a password." else "Protected file saved. Keep its password separately."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { notice = error.message }
            finally { bytes.fill(0); busy = false }
        }
    }
    val select = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) { source = uri; dialog = true } }
    HorizontalDivider()
    Text("Protect document files", style = MaterialTheme.typography.titleMedium)
    Text("Wrap exported PDFs, spreadsheets or CSV files in a password-protected .archimansecure file before sharing. The recipient uses this tool to unlock it. Existing unprotected copies remain readable.")
    OutlinedButton(enabled = !busy && ready == null, onClick = { decrypt = false; select.launch(arrayOf("*/*")) }) { Text("Encrypt a file") }
    OutlinedButton(enabled = !busy && ready == null, onClick = { decrypt = true; select.launch(arrayOf("*/*")) }) { Text("Decrypt a file") }
    if (busy) Text("Processing protected file…")
    notice?.let { Text(it) }
    if (dialog) AlertDialog(onDismissRequest = { dialog = false; source = null; password = ""; confirmation = "" },
        title = { Text(if (decrypt) "Unlock file" else "Protect file") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (decrypt) "Enter the password used to protect this file." else "Use at least 12 characters. A forgotten password cannot be recovered.")
            OutlinedTextField(password,{ if (it.length <= 256) password = it },label = { Text("Password") },visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),singleLine = true)
            if (!decrypt) OutlinedTextField(confirmation,{ if (it.length <= 256) confirmation = it },label = { Text("Confirm password") },visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),singleLine = true)
        } }, confirmButton = { TextButton(enabled = if (decrypt) password.isNotEmpty() else password.length >= 12 && password == confirmation,onClick = {
            val uri = source!!; val secret = password.toCharArray(); val opening = decrypt
            dialog = false; source = null; password = ""; confirmation = ""; busy = true; notice = null
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        val limit = BackupArchive.safeByteLimit()
                        val input = context.contentResolver.openInputStream(uri)?.use { PasswordEnvelope.readBounded(it, limit + 128) } ?: error("Could not read file.")
                        try {
                            if (opening) {
                                val plain = PasswordEnvelope.decrypt(input, secret, limit)
                                val stream = java.io.DataInputStream(plain.inputStream())
                                try { val name = stream.readUTF(); require(name.length in 1..180 && name.none { it == '/' || it == '\\' || it.code < 32 }) { "Invalid protected filename." }; name to stream.readBytes() } finally { plain.fill(0) }
                            } else {
                                val name = context.contentResolver.query(uri,arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),null,null,null)?.use { cursor -> if(cursor.moveToFirst()) cursor.getString(0) else null }.orEmpty().substringAfterLast('/').substringAfterLast('\\').filter { it.code >= 32 }.take(180).ifBlank { "document" }
                                val payload = java.io.ByteArrayOutputStream().also { bytes -> java.io.DataOutputStream(bytes).use { it.writeUTF(name); it.write(input) } }.toByteArray()
                                try { "$name.archimansecure" to PasswordEnvelope.encrypt(payload,secret,limit) } finally { payload.fill(0) }
                            }
                        } finally { input.fill(0) }
                    }
                    suggestedName = result.first; ready = result.second; destination.launch(suggestedName)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { notice = error.message ?: "Unable to process file." }
                finally { secret.fill('\u0000'); busy = false }
            }
        }) { Text("Continue") } }, dismissButton = { TextButton(onClick = { dialog = false; source = null; password = ""; confirmation = "" }) { Text("Cancel") } })
}
