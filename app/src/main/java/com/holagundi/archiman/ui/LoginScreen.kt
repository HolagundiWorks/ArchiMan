package com.holagundi.archiman.ui

import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonTextButton as TextButton

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.holagundi.archiman.ui.theme.CarbonModal
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.holagundi.archiman.R
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.ui.theme.ArchiField
import com.holagundi.archiman.ui.theme.CarbonIcons
import com.holagundi.archiman.ui.theme.CarbonSpacing
import com.holagundi.archiman.ui.theme.DetailRow
import com.holagundi.archiman.ui.theme.DisclosureCard
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
    var showBackup by rememberSaveable { mutableStateOf(false) }
    var showPortal by rememberSaveable { mutableStateOf(false) }
    var pendingUri by rememberSaveable { mutableStateOf<String?>(null) }
    var practiceName by rememberSaveable { mutableStateOf("") }
    var principalName by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var companyOpen by rememberSaveable { mutableStateOf(true) }
    var demoOpen by rememberSaveable { mutableStateOf(true) }
    var moreOpen by rememberSaveable { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingUri = uri.toString()
    }

    LaunchedEffect(notice) {
        if (notice == SessionViewModel.OPENED || notice == SessionViewModel.CREATED) {
            users.firstOrNull { it.isActive }?.let(viewModel::signIn)
            step = LoginStep.ROLE
        }
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(CarbonSpacing.screenPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = CarbonSpacing.stackArrangement,
        ) {
            Text(
                stringResource(R.string.login_brand),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(
                    when (step) {
                        LoginStep.COMPANY -> R.string.login_company_title
                        LoginStep.CREATE -> R.string.login_create_title
                        LoginStep.ROLE -> R.string.login_role_title
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
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
                style = MaterialTheme.typography.bodyMedium,
            )
            if (notice != null) {
                Text(notice ?: "", color = MaterialTheme.colorScheme.primary, modifier = Modifier.announce())
            }

            when (step) {
                LoginStep.COMPANY -> {
                    company?.let { profile ->
                        OnDeviceCompanyCard(
                            company = profile,
                            expanded = companyOpen,
                            onExpandedChange = { companyOpen = it },
                            onOpen = { users.firstOrNull { it.isActive }?.let(viewModel::signIn) },
                        )
                    }

                    DisclosureCard(
                        title = stringResource(R.string.login_builtin),
                        summary = "Fictional sample practice for testing",
                        expanded = demoOpen,
                        onExpandedChange = { demoOpen = it },
                        leadingIcon = CarbonIcons.Description,
                    ) {
                        Button(
                            onClick = { confirmBuiltIn = true },
                            enabled = catalog != null && !opening,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(if (opening) R.string.demo_opening else R.string.login_open_builtin))
                        }
                    }

                    DisclosureCard(
                        title = "More ways in",
                        summary = "File, new company, backup, Wi‑Fi portal",
                        expanded = moreOpen,
                        onExpandedChange = { moreOpen = it },
                        leadingIcon = CarbonIcons.FolderOpen,
                    ) {
                        LoginActionRow(
                            icon = CarbonIcons.FolderOpen,
                            label = stringResource(R.string.login_pick_file),
                            enabled = !opening,
                            onClick = { picker.launch(arrayOf("application/json", "*/*")) },
                        )
                        LoginActionRow(
                            icon = CarbonIcons.Add,
                            label = stringResource(R.string.login_create),
                            enabled = !opening,
                            onClick = { step = LoginStep.CREATE },
                        )
                        LoginActionRow(
                            icon = CarbonIcons.Backup,
                            label = stringResource(R.string.backup_title),
                            enabled = !opening,
                            onClick = { showBackup = true },
                        )
                        LoginActionRow(
                            icon = CarbonIcons.Wifi,
                            label = stringResource(R.string.portal_heading),
                            enabled = !opening,
                            onClick = { showPortal = true },
                        )
                    }
                }

                LoginStep.CREATE -> {
                    DisclosureCard(
                        title = stringResource(R.string.login_create_title),
                        summary = "Empty practice on this phone",
                        expanded = true,
                        onExpandedChange = {},
                        leadingIcon = CarbonIcons.Add,
                    ) {
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
                    }
                    TextButton(onClick = { step = LoginStep.COMPANY }) {
                        Text(stringResource(R.string.cancel))
                    }
                }

                LoginStep.ROLE -> {
                    DisclosureCard(
                        title = company?.practiceName ?: stringResource(R.string.login_role_title),
                        summary = "Open as admin on this phone",
                        expanded = true,
                        onExpandedChange = {},
                        leadingIcon = CarbonIcons.Business,
                    ) {
                        if (users.isEmpty()) {
                            Text(stringResource(R.string.login_roles_loading), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        users.firstOrNull { it.isActive }?.let { admin ->
                            DetailRow("Role", admin.displayName.ifBlank { "Admin" })
                            Button(onClick = { viewModel.signIn(admin) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                                Text("Open admin workspace")
                            }
                        }
                    }
                    TextButton(onClick = { step = LoginStep.COMPANY }) {
                        Text(stringResource(R.string.login_change_company))
                    }
                }
            }
        }
    }

    if (showPortal) {
        CarbonModal(
            title = stringResource(R.string.portal_heading),
            description = stringResource(R.string.login_portal_help),
            onDismiss = { showPortal = false },
            primaryLabel = stringResource(R.string.cancel),
            onPrimary = { showPortal = false },
            showFooterActions = false,
        ) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().heightIn(min = 320.dp, max = 400.dp)) {
                PortalPane()
            }
        }
    }

    if (showBackup) {
        CarbonModal(
            title = stringResource(R.string.backup_title),
            onDismiss = { showBackup = false },
            primaryLabel = stringResource(R.string.cancel),
            onPrimary = { showBackup = false },
            showFooterActions = false,
        ) {
            BackupPane(onRestored = {
                viewModel.signOut()
                showBackup = false
                step = LoginStep.ROLE
            })
        }
    }

    if (confirmBuiltIn) {
        CarbonModal(
            title = stringResource(R.string.demo_confirm_title),
            description = stringResource(R.string.demo_confirm_body),
            onDismiss = { confirmBuiltIn = false },
            primaryLabel = stringResource(R.string.login_open_builtin),
            onPrimary = {
                confirmBuiltIn = false
                viewModel.openBuiltIn()
            },
            secondaryLabel = stringResource(R.string.cancel),
        )
    }

    if (confirmCreate) {
        CarbonModal(
            title = stringResource(R.string.login_create_confirm_title),
            description = stringResource(R.string.login_create_confirm_body),
            onDismiss = { confirmCreate = false },
            primaryLabel = stringResource(R.string.login_create_action),
            onPrimary = {
                confirmCreate = false
                viewModel.createCompany(practiceName, principalName, city)
            },
            secondaryLabel = stringResource(R.string.cancel),
        )
    }

    val uriText = pendingUri
    if (uriText != null) {
        CarbonModal(
            title = stringResource(R.string.login_file_confirm_title),
            description = stringResource(R.string.login_file_confirm_body),
            onDismiss = { pendingUri = null },
            primaryLabel = stringResource(R.string.login_open_file),
            onPrimary = {
                val uri = Uri.parse(uriText)
                pendingUri = null
                viewModel.openFromUri(uri)
            },
            secondaryLabel = stringResource(R.string.cancel),
        )
    }
}

@Composable
private fun OnDeviceCompanyCard(
    company: CompanyProfileEntity,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    DisclosureCard(
        title = company.practiceName,
        summary = "Practice on this phone",
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        leadingIcon = CarbonIcons.Business,
    ) {
        Button(onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text("Open admin workspace")
        }
    }
}

@Composable
private fun LoginActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null)
            Text(label)
        }
    }
}
