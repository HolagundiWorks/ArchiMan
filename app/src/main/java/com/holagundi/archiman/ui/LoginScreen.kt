package com.holagundi.archiman.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.announce

private enum class LoginStep { COMPANY, CREATE, ROLE }

@Composable
fun LoginScreen(viewModel: SessionViewModel = hiltViewModel()) {
    val company by viewModel.company.collectAsStateWithLifecycle()
    val users by viewModel.users.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val notice by viewModel.notice.collectAsStateWithLifecycle()
    val opening by viewModel.opening.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableStateOf(LoginStep.COMPANY) }
    var confirmBuiltIn by rememberSaveable { mutableStateOf(false) }
    var confirmCreate by rememberSaveable { mutableStateOf(false) }
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceName by rememberSaveable { mutableStateOf("") }
    var principalName by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingUri = uri.toString()
    }

    LaunchedEffect(notice) {
        if (notice == SessionViewModel.OPENED || notice == SessionViewModel.CREATED) step = LoginStep.ROLE
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.login_brand),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(
                    when (step) {
                        LoginStep.COMPANY -> R.string.login_company_title
                        LoginStep.CREATE -> R.string.login_create_title
                        LoginStep.ROLE -> R.string.login_role_title
                    },
                ),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(
                    when (step) {
                        LoginStep.COMPANY -> R.string.login_company_body
                        LoginStep.CREATE -> R.string.login_create_body
                        LoginStep.ROLE -> R.string.login_role_body
                    },
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (notice != null) {
                Text(notice ?: "", color = MaterialTheme.colorScheme.primary, modifier = Modifier.announce())
            }

            if (step == LoginStep.COMPANY) {
                if (company != null) {
                    Text(company?.practiceName.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = { step = LoginStep.ROLE },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.login_continue))
                    }
                }
                Text(stringResource(R.string.login_builtin), style = MaterialTheme.typography.titleMedium)
                catalog?.index?.take(6)?.forEach { entry ->
                    Text("${entry.label} · ${entry.count}", fontFamily = FontFamily.Monospace)
                }
                Button(
                    onClick = { confirmBuiltIn = true },
                    enabled = catalog != null && !opening,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (opening) R.string.demo_opening else R.string.login_open_builtin))
                }
                OutlinedButton(
                    onClick = { picker.launch(arrayOf("application/json", "*/*")) },
                    enabled = !opening,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.login_pick_file))
                }
                OutlinedButton(
                    onClick = { step = LoginStep.CREATE },
                    enabled = !opening,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.login_create))
                }
            } else if (step == LoginStep.CREATE) {
                ArchiField(
                    practiceName,
                    { practiceName = it },
                    stringResource(R.string.login_create_name),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                ArchiField(
                    principalName,
                    { principalName = it },
                    stringResource(R.string.login_create_principal),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                ArchiField(
                    city,
                    { city = it },
                    stringResource(R.string.login_create_city),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                Button(
                    onClick = {
                        if (company != null) confirmCreate = true else viewModel.createCompany(practiceName, principalName, city)
                    },
                    enabled = practiceName.isNotBlank() && !opening,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text(stringResource(if (opening) R.string.demo_opening else R.string.login_create_action))
                }
                TextButton(onClick = { step = LoginStep.COMPANY }) {
                    Text(stringResource(R.string.cancel))
                }
            } else {
                if (users.isEmpty()) {
                    Text(stringResource(R.string.login_roles_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                users.filter { it.isActive }.forEach { user ->
                    OutlinedButton(
                        onClick = { viewModel.signIn(user) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    ) {
                        Text("${user.displayName} · ${user.role}")
                    }
                }
                TextButton(onClick = { step = LoginStep.COMPANY }) {
                    Text(stringResource(R.string.login_change_company))
                }
            }
        }
    }

    if (confirmBuiltIn) {
        AlertDialog(
            onDismissRequest = { confirmBuiltIn = false },
            title = { Text(stringResource(R.string.demo_confirm_title)) },
            text = { Text(stringResource(R.string.demo_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmBuiltIn = false
                    viewModel.openBuiltIn()
                }) { Text(stringResource(R.string.login_open_builtin)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmBuiltIn = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (confirmCreate) {
        AlertDialog(
            onDismissRequest = { confirmCreate = false },
            title = { Text(stringResource(R.string.login_create_confirm_title)) },
            text = { Text(stringResource(R.string.login_create_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmCreate = false
                    viewModel.createCompany(practiceName, principalName, city)
                }) { Text(stringResource(R.string.login_create_action)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCreate = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    val uriText = pendingUri
    if (uriText != null) {
        AlertDialog(
            onDismissRequest = { pendingUri = null },
            title = { Text(stringResource(R.string.login_file_confirm_title)) },
            text = { Text(stringResource(R.string.login_file_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    val uri = Uri.parse(uriText)
                    pendingUri = null
                    viewModel.openFromUri(uri)
                }) { Text(stringResource(R.string.login_open_file)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUri = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
