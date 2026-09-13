package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import coil.compose.AsyncImage
import com.example.company.CompanyProfileBackupManager
import com.example.company.CompanyDatabaseImportPreview
import com.example.data.local.DATABASE_SCHEMA_VERSION
import com.example.data.local.entity.CompanyProfileEntity
import com.example.data.local.entity.ProjectEntity
import com.example.domain.SupportDiagnosticReport
import com.example.domain.SupportDiagnosticSnapshot
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(viewModel: SiteViewModel, onBack: () -> Unit, onOpenPortal: () -> Unit) {
    val saved by viewModel.companyProfile.collectAsStateWithLifecycle()
    val supabaseState by viewModel.supabaseConnectionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val backupManager = remember(context) { CompanyProfileBackupManager(context) }
    var practiceName by remember { mutableStateOf("") }
    var legalName by remember { mutableStateOf("") }
    var companyType by remember { mutableStateOf("Architecture practice") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("India") }
    var pinCode by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var coaNumber by remember { mutableStateOf("") }
    var principalName by remember { mutableStateOf("") }
    var principalQualification by remember { mutableStateOf("") }
    var practiceRegistrationDetails by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<CompanyProfileEntity?>(null) }
    var pendingDatabaseImport by remember { mutableStateOf<CompanyDatabaseImportPreview?>(null) }
    var restoreStaged by remember { mutableStateOf(false) }
    var databasePassword by remember { mutableStateOf("") }
    var databasePasswordConfirmation by remember { mutableStateOf("") }
    var showDatabasePassword by remember { mutableStateOf(false) }
    var supabaseUrl by remember { mutableStateOf("") }
    var supabaseKey by remember { mutableStateOf("") }
    var showSupabaseKey by remember { mutableStateOf(false) }

    LaunchedEffect(saved) {
        saved?.let {
            practiceName = it.practiceName
            legalName = it.legalName
            companyType = it.companyType
            address = it.address
            city = it.city
            state = it.state
            country = it.country
            pinCode = it.pinCode
            phone = it.phone
            email = it.email
            website = it.website
            pan = it.pan
            gstin = it.gstin
            coaNumber = it.coaRegistrationNumber
            principalName = it.principalName
            principalQualification = it.principalQualification
            practiceRegistrationDetails = it.practiceRegistrationDetails
            logoUri = it.logoUri
        }
    }

    LaunchedEffect(Unit) {
        viewModel.consumeCompanyDatabaseRestoreMessage()?.let { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(supabaseState.projectUrl, supabaseState.publishableKey) {
        supabaseUrl = supabaseState.projectUrl
        supabaseKey = supabaseState.publishableKey
    }

    val logoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.storeLogo(uri) } }
                .onSuccess { logoUri = it; snackbar.showSnackbar("Logo added. Save the profile to keep it.") }
                .onFailure { snackbar.showSnackbar(it.message ?: "Could not add the logo.") }
        }
    }
    val backupExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val profile = saved
        if (uri != null && profile != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.exportTo(uri, profile) } }
                .onSuccess { snackbar.showSnackbar("Company profile backup exported.") }
                .onFailure { snackbar.showSnackbar(it.message ?: "Could not export the backup.") }
        }
    }
    val backupImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { withContext(Dispatchers.IO) { backupManager.importFrom(uri) } }
                .onSuccess { pendingImport = it }
                .onFailure { snackbar.showSnackbar(it.message ?: "Could not import the backup.") }
        }
    }
    val databaseExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) scope.launch {
            val password = databasePassword
            runCatching { viewModel.exportCompanyDatabase(uri, password) }
                .onSuccess { checksum -> databasePassword = ""; databasePasswordConfirmation = ""; snackbar.showSnackbar("Company database exported · ${checksum.take(12)}…") }
                .onFailure { snackbar.showSnackbar(it.message ?: "Could not export the company database.") }
        }
    }
    val databaseImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            pendingDatabaseImport?.let(viewModel::discardCompanyDatabasePreview)
            val password = databasePassword
            runCatching { viewModel.previewCompanyDatabase(uri, password) }
                .onSuccess { databasePassword = ""; databasePasswordConfirmation = ""; pendingDatabaseImport = it }
                .onFailure { snackbar.showSnackbar(it.message ?: "Could not validate the company database.") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Company profile") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(96.dp)) {
                    if (logoUri.isNullOrBlank()) Icon(Icons.Default.Business, "Company logo", modifier = Modifier.padding(24.dp))
                    else AsyncImage(model = logoUri, contentDescription = "Company logo", modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { logoPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.AddAPhoto, null); Spacer(Modifier.width(6.dp)); Text(if (logoUri == null) "Add logo" else "Change logo")
                    }
                    if (logoUri != null) TextButton(onClick = { logoUri = null }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DeleteOutline, null); Spacer(Modifier.width(6.dp)); Text("Remove")
                    }
                }
            }
            Text("Practice identity", style = MaterialTheme.typography.titleMedium)
            Text("Used on drawing registers, transmittals, reports and handover documents.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onOpenPortal, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Wifi, null)
                Spacer(Modifier.width(8.dp))
                Text("Local Wi-Fi workspace")
            }
            OutlinedTextField(practiceName, { practiceName = it }, label = { Text("Practice / company name*") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(legalName, { legalName = it }, label = { Text("Legal name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(companyType, { companyType = it }, label = { Text("Company type") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(address, { address = it }, label = { Text("Registered address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(city, { city = it }, label = { Text("City") }, modifier = Modifier.weight(1f), singleLine = true)
                OutlinedTextField(state, { state = it }, label = { Text("State") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            OutlinedTextField(country, { country = it }, label = { Text("Country") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(pinCode, { pinCode = it.filter(Char::isDigit).take(6) }, label = { Text("PIN code") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
            OutlinedTextField(email, { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true)
            OutlinedTextField(website, { website = it }, label = { Text("Website") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true)
            Text("Professional identity", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(principalName, { principalName = it }, label = { Text("Principal architect / proprietor") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(principalQualification, { principalQualification = it }, label = { Text("Principal qualification") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(coaNumber, { coaNumber = it }, label = { Text("COA registration number") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(practiceRegistrationDetails, { practiceRegistrationDetails = it }, label = { Text("Practice registration details") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Text("Tax identifiers", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(pan, { pan = it.uppercase(Locale.ROOT).take(10) }, label = { Text("PAN (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(gstin, { gstin = it.uppercase(Locale.ROOT).take(15) }, label = { Text("GSTIN (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            HorizontalDivider()
            Text("Full company database", style = MaterialTheme.typography.titleSmall)
            Text("Password-encrypted .archimandb package containing the verified SQLite database, portal users, company logo and managed measurement photos. Import is previewed and keeps the previous local database in private recovery storage.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                databasePassword,
                { databasePassword = it.take(128) },
                label = { Text("Database password") },
                supportingText = { Text("At least 12 characters for export; enter the existing password for import") },
                visualTransformation = if (showDatabasePassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showDatabasePassword = !showDatabasePassword }) { Text(if (showDatabasePassword) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                databasePasswordConfirmation,
                { databasePasswordConfirmation = it.take(128) },
                label = { Text("Confirm new export password") },
                supportingText = { Text("Required only when exporting") },
                isError = databasePasswordConfirmation.isNotBlank() && databasePasswordConfirmation != databasePassword,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = databasePassword.length >= 12 && databasePassword == databasePasswordConfirmation, onClick = { databaseExporter.launch("ArchiMan-company.archimandb") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(4.dp)); Text("Export")
                }
                OutlinedButton(enabled = databasePassword.isNotEmpty(), onClick = { databaseImporter.launch(arrayOf("application/octet-stream", "application/zip", "*/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(4.dp)); Text("Import")
                }
            }
            Text("Profile-only transfer", style = MaterialTheme.typography.titleSmall)
            Text("JSON transfer for only the practice identity and logo. Projects and measurements are not included.", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = saved != null, onClick = { backupExporter.launch("ArchiMan-company-profile.json") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.UploadFile, null); Spacer(Modifier.width(4.dp)); Text("Export profile")
                }
                OutlinedButton(onClick = { backupImporter.launch(arrayOf("application/json", "text/*")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, null); Spacer(Modifier.width(4.dp)); Text("Import profile")
                }
            }
            HorizontalDivider()
            Text("Supabase connection", style = MaterialTheme.typography.titleSmall)
            Text("Optional platform connection. Connecting does not upload or synchronise data. Use only a publishable key; Row Level Security must be configured in Supabase.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(supabaseUrl, { supabaseUrl = it }, label = { Text("Project URL") }, placeholder = { Text("https://your-project.supabase.co") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
            OutlinedTextField(
                supabaseKey, { supabaseKey = it }, label = { Text("Publishable key") }, placeholder = { Text("sb_publishable_…") },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = if (showSupabaseKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showSupabaseKey = !showSupabaseKey }) { Text(if (showSupabaseKey) "Hide" else "Show") } }
            )
            Text(supabaseState.message, style = MaterialTheme.typography.bodySmall, color = if (supabaseState.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(enabled = !supabaseState.isTesting && supabaseUrl.isNotBlank() && supabaseKey.isNotBlank(), onClick = { viewModel.configureSupabase(supabaseUrl, supabaseKey) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Cloud, null); Spacer(Modifier.width(4.dp)); Text(if (supabaseState.isTesting) "Testing…" else "Save & test")
                }
                if (supabaseState.projectUrl.isNotBlank()) OutlinedButton(onClick = viewModel::clearSupabaseConnection) { Text("Clear") }
            }
            HorizontalDivider()
            AormsIdentitySection(viewModel)
            Button(
                enabled = practiceName.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    viewModel.saveCompanyProfile(
                        CompanyProfileEntity(
                            practiceName = practiceName.trim(), legalName = legalName.trim(), companyType = companyType.trim(), address = address.trim(),
                            city = city.trim(), state = state.trim(), country = country.trim(), pinCode = pinCode.trim(), phone = phone.trim(),
                            email = email.trim(), website = website.trim(), pan = pan.trim(), gstin = gstin.trim(),
                            coaRegistrationNumber = coaNumber.trim(), principalName = principalName.trim(),
                            principalQualification = principalQualification.trim(), practiceRegistrationDetails = practiceRegistrationDetails.trim(),
                            logoUri = logoUri
                        )
                    )
                    onBack()
                }
            ) { Text("Save company profile") }
        }
    }

    pendingImport?.let { imported ->
        AlertDialog(
            onDismissRequest = { backupManager.discardImportedLogo(imported.logoUri); pendingImport = null },
            title = { Text("Restore company profile?") },
            text = { Text("This will replace the current company profile with “${imported.practiceName}”. Projects, measurements and Supabase settings will not be changed.") },
            confirmButton = { Button(onClick = { viewModel.saveCompanyProfile(imported); pendingImport = null; scope.launch { snackbar.showSnackbar("Company profile restored.") } }) { Text("Restore") } },
            dismissButton = { TextButton(onClick = { backupManager.discardImportedLogo(imported.logoUri); pendingImport = null }) { Text("Cancel") } }
        )
    }

    pendingDatabaseImport?.let { preview ->
        AlertDialog(
            onDismissRequest = { viewModel.discardCompanyDatabasePreview(preview); pendingDatabaseImport = null },
            title = { Text("Replace this company database?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(preview.practiceName, style = MaterialTheme.typography.titleMedium)
                    Text("Schema ${preview.schemaVersion} · ${preview.projectCount} projects · ${preview.measurementCount} measurements · ${preview.attachmentCount} managed files")
                    Text("SHA-256 ${preview.databaseChecksum}", style = MaterialTheme.typography.labelSmall)
                    Text("The current database will be retained in private recovery storage. The replacement is applied only after ArchiMan restarts.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        runCatching { viewModel.stageCompanyDatabaseRestore(preview) }
                            .onSuccess { pendingDatabaseImport = null; restoreStaged = true }
                            .onFailure { snackbar.showSnackbar(it.message ?: "Could not stage the company database.") }
                    }
                }) { Text("Import and restart") }
            },
            dismissButton = { TextButton(onClick = { viewModel.discardCompanyDatabasePreview(preview); pendingDatabaseImport = null }) { Text("Cancel") } }
        )
    }

    if (restoreStaged) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Restart required") },
            text = { Text("Close ArchiMan now, then open it again. The validated company database will be applied before the app opens.") },
            confirmButton = {
                Button(onClick = { android.os.Process.killProcess(android.os.Process.myPid()) }) { Text("Close ArchiMan") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPortalScreen(viewModel: SiteViewModel, onBack: () -> Unit) {
    val state by viewModel.localPortalState.collectAsStateWithLifecycle()
    val users by viewModel.localUsers.collectAsStateWithLifecycle()
    val auditEvents by viewModel.portalAuditEvents.collectAsStateWithLifecycle()
    val userMessage by viewModel.portalUserMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val measurementSheets by viewModel.measurementSheets.collectAsStateWithLifecycle()
    var showAddUser by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Wi-Fi workspace") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(if (state.isRunning || state.isStarting) Icons.Default.Wifi else Icons.Default.WifiOff, null, modifier = Modifier.size(42.dp))
            Text(
                when {
                    state.isRunning -> "Workspace is available"
                    state.isStarting -> "Finding the Wi-Fi address…"
                    else -> "Use ArchiMan from a browser"
                },
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "People on the same trusted Wi-Fi can sign in with their own account. Admins and editors can enter projects, contacts, contractor work lists, planning, reports, coordination and measurement rows; viewers have read-only access.",
                style = MaterialTheme.typography.bodyMedium
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            userMessage?.let {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(it, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = viewModel::clearPortalUserMessage) { Text("Dismiss") }
                    }
                }
            }
            if (state.isRunning) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SECURE WEB ADDRESS", style = MaterialTheme.typography.labelSmall)
                        Text(state.url, style = MaterialTheme.typography.titleMedium)
                        Text("CERTIFICATE FINGERPRINT", style = MaterialTheme.typography.labelSmall)
                        Text(state.certificateFingerprint, style = MaterialTheme.typography.bodySmall)
                        Text("The workspace stops after one hour. Each browser login lasts up to 30 minutes.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString("${state.url}\nCertificate SHA-256: ${state.certificateFingerprint}")) }) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Copy connection details")
                        }
                    }
                }
                Text("A browser may show a one-time certificate warning because the phone creates its own local certificate. Compare its SHA-256 fingerprint with the value above before continuing. Use only on a trusted Wi-Fi network.", style = MaterialTheme.typography.bodySmall)
                Text("If another device times out before showing the certificate warning, the Wi-Fi router is blocking device-to-device traffic. Disable AP/client isolation (sometimes called WLAN partition) or use a trusted hotspot where connected devices may communicate.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = viewModel::stopLocalPortal, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Stop workspace")
                }
            } else {
                Button(enabled = users.any { it.isActive } && !state.isStarting, onClick = viewModel::startLocalPortal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Wifi, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isStarting) "Finding address…" else "Start secure workspace")
                }
                Text("The Wi-Fi router assigns the phone's IP address; ArchiMan detects and displays it. Nothing is uploaded to the internet, and the address works only from devices that can reach this phone on the same Wi-Fi network.", style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Portal users", style = MaterialTheme.typography.titleMedium)
                    Text("Admin manages access · Editor can enter records · Viewer can only view", style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { showAddUser = true }) { Text("Add user") }
            }
            if (users.isEmpty()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Create an administrator before starting the workspace.")
                        Button(onClick = { showAddUser = true }) { Text("Create administrator") }
                    }
                }
            } else users.forEach { user ->
                Surface(tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleSmall)
                            Text("@${user.username} · ${user.role.lowercase().replaceFirstChar(Char::uppercase)}", style = MaterialTheme.typography.bodySmall)
                            user.lastLoginAt?.let { Text("Last sign-in ${SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(it)}", style = MaterialTheme.typography.labelSmall) }
                        }
                        Switch(checked = user.isActive, onCheckedChange = { viewModel.setLocalUserActive(user, it) })
                    }
                }
            }
            if (auditEvents.isNotEmpty()) {
                Text("Recent web changes", style = MaterialTheme.typography.titleMedium)
                auditEvents.take(5).forEach { event ->
                    Text("${event.username} · ${event.action.removePrefix("ADD_").lowercase()} · ${event.summary}", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            OutlinedButton(
                onClick = {
                    val knownSheetIds = measurementSheets.mapTo(hashSetOf()) { it.id }
                    val knownItemIds = items.mapTo(hashSetOf()) { it.id }
                    val report = SupportDiagnosticReport.render(
                        SupportDiagnosticSnapshot(
                            appVersion = BuildConfig.VERSION_NAME,
                            versionCode = BuildConfig.VERSION_CODE.toLong(),
                            schemaVersion = DATABASE_SCHEMA_VERSION,
                            androidSdk = Build.VERSION.SDK_INT,
                            generatedAtEpochMs = System.currentTimeMillis(),
                            projectCount = projects.size,
                            contractorCount = contractors.size,
                            workItemCount = items.size,
                            measurementCount = measurements.size,
                            sheetCount = measurementSheets.size,
                            archivedSheetCount = measurementSheets.count { it.archivedAt != null },
                            rowsWithoutKnownSheet = measurements.count { it.sheetId !in knownSheetIds },
                            rowsWithoutKnownWorkItem = measurements.count { it.itemId !in knownItemIds },
                            sheetStatusCounts = measurementSheets.groupingBy { it.status }.eachCount()
                        )
                    )
                    ExportHelper.shareSupportDiagnostics(context, report)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Share support diagnostics") }
        }
    }

    if (showAddUser) {
        PortalUserDialog(
            firstUser = users.isEmpty(),
            onDismiss = { showAddUser = false },
            onCreate = { username, displayName, password, role ->
                viewModel.createLocalUser(username, displayName, password, role)
                showAddUser = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortalUserDialog(firstUser: Boolean, onDismiss: () -> Unit, onCreate: (String, String, String, String) -> Unit) {
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(if (firstUser) "ADMIN" else "EDITOR") }
    var roleMenu by remember { mutableStateOf(false) }
    val valid = username.matches(Regex("[A-Za-z0-9._-]{3,40}")) && displayName.isNotBlank() && password.length in 10..128 && password == confirmation
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (firstUser) "Create portal administrator" else "Add portal user") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(displayName, { displayName = it }, label = { Text("Display name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(username, { username = it.lowercase(Locale.ROOT).filter { char -> char.isLetterOrDigit() || char in "._-" } }, label = { Text("Username") }, supportingText = { Text("3–40 letters, numbers, dot, dash or underscore") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                ExposedDropdownMenuBox(expanded = roleMenu, onExpandedChange = { if (!firstUser) roleMenu = it }) {
                    OutlinedTextField(role.lowercase().replaceFirstChar(Char::uppercase), {}, readOnly = true, enabled = !firstUser, label = { Text("Role") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(roleMenu) }, modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth())
                    ExposedDropdownMenu(expanded = roleMenu, onDismissRequest = { roleMenu = false }) {
                        listOf("ADMIN", "EDITOR", "VIEWER").forEach { option -> DropdownMenuItem(text = { Text(option.lowercase().replaceFirstChar(Char::uppercase)) }, onClick = { role = option; roleMenu = false }) }
                    }
                }
                OutlinedTextField(password, { password = it.take(128) }, label = { Text("Password") }, supportingText = { Text("At least 10 characters") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(confirmation, { confirmation = it.take(128) }, label = { Text("Confirm password") }, isError = confirmation.isNotEmpty() && confirmation != password, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (firstUser) Text("The first account is always an administrator so access cannot be locked out.", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { Button(enabled = valid, onClick = { onCreate(username, displayName.trim(), password, role) }) { Text("Create user") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProjectProfileDialog(project: ProjectEntity, onDismiss: () -> Unit, onSave: (ProjectEntity) -> Unit) {
    var name by remember { mutableStateOf(project.name) }
    var code by remember { mutableStateOf(project.projectCode) }
    var type by remember { mutableStateOf(project.projectType) }
    var status by remember { mutableStateOf(project.status) }
    var client by remember { mutableStateOf(project.client) }
    var location by remember { mutableStateOf(project.siteLocation) }
    var description by remember { mutableStateOf(project.description) }
    var architect by remember { mutableStateOf(project.architectInCharge) }
    var startDate by remember { mutableStateOf(formatProfileDate(project.startDate)) }
    var targetDate by remember { mutableStateOf(formatProfileDate(project.targetCompletionDate)) }
    var plotArea by remember { mutableStateOf(project.plotArea?.toString().orEmpty()) }
    var builtUpArea by remember { mutableStateOf(project.builtUpArea?.toString().orEmpty()) }
    var areaUnit by remember { mutableStateOf(project.areaUnit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project profile") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Project name*") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(code, { code = it }, label = { Text("Project code") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(type, { type = it }, label = { Text("Project type") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(status, { status = it.uppercase(Locale.ROOT) }, label = { Text("Status") }, singleLine = true)
                OutlinedTextField(client, { client = it }, label = { Text("Client") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(location, { location = it }, label = { Text("Site address") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                OutlinedTextField(architect, { architect = it }, label = { Text("Architect in charge") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Project description") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(startDate, { startDate = it }, label = { Text("Start (DD-MM-YYYY)") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(targetDate, { targetDate = it }, label = { Text("Target (DD-MM-YYYY)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(plotArea, { plotArea = it }, label = { Text("Plot area") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(builtUpArea, { builtUpArea = it }, label = { Text("Built-up area") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                    OutlinedTextField(areaUnit, { areaUnit = it }, label = { Text("Unit") }, modifier = Modifier.width(80.dp), singleLine = true)
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                onSave(project.copy(
                    name = name.trim(), projectCode = code.trim(), projectType = type.trim(), status = status.trim(),
                    client = client.trim(), siteLocation = location.trim(), description = description.trim(),
                    architectInCharge = architect.trim(), startDate = parseProfileDate(startDate),
                    targetCompletionDate = parseProfileDate(targetDate), plotArea = plotArea.toDoubleOrNull(),
                    builtUpArea = builtUpArea.toDoubleOrNull(), areaUnit = areaUnit.trim().ifBlank { "m²" }
                ))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatProfileDate(value: Long?): String = value?.let {
    SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(it)
}.orEmpty()

private fun parseProfileDate(value: String): Long? = runCatching {
    if (value.isBlank()) null else SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).apply { isLenient = false }.parse(value)?.time
}.getOrNull()
