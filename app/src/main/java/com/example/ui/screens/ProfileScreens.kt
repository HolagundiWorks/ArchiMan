package com.example.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.data.local.DATABASE_SCHEMA_VERSION
import com.example.data.local.entity.CompanyProfileEntity
import com.example.data.local.entity.ProjectEntity
import com.example.domain.SupportDiagnosticReport
import com.example.domain.SupportDiagnosticSnapshot
import com.example.ui.viewmodel.SiteViewModel
import com.example.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanyProfileScreen(viewModel: SiteViewModel, onBack: () -> Unit, onOpenPortal: () -> Unit) {
    val saved by viewModel.companyProfile.collectAsStateWithLifecycle()
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
        }
    }

    Scaffold(
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
            Icon(Icons.Default.Business, null, modifier = Modifier.size(32.dp))
            Text("Practice identity", style = MaterialTheme.typography.titleMedium)
            Text("Used on drawing registers, transmittals, reports and handover documents.", style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onOpenPortal, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Wifi, null)
                Spacer(Modifier.width(8.dp))
                Text("Local Wi-Fi web portal")
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
                            logoUri = saved?.logoUri
                        )
                    )
                    onBack()
                }
            ) { Text("Save company profile") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalPortalScreen(viewModel: SiteViewModel, onBack: () -> Unit) {
    val state by viewModel.localPortalState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val contractors by viewModel.contractors.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()
    val measurementSheets by viewModel.measurementSheets.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Local Wi-Fi portal") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(if (state.isRunning) Icons.Default.Wifi else Icons.Default.WifiOff, null, modifier = Modifier.size(42.dp))
            Text(if (state.isRunning) "Portal is available" else "Share ArchiMan on this Wi-Fi", style = MaterialTheme.typography.titleLarge)
            Text(
                "A browser on the same local Wi-Fi can view projects and the selected project's tasks, schedule, inspections and drawings. Access is read-only and requires the PIN shown here.",
                style = MaterialTheme.typography.bodyMedium
            )
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.isRunning) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("WEB ADDRESS", style = MaterialTheme.typography.labelSmall)
                        Text(state.url, style = MaterialTheme.typography.titleMedium)
                        Text("ACCESS PIN", style = MaterialTheme.typography.labelSmall)
                        Text(state.pin, style = MaterialTheme.typography.headlineMedium)
                        Text("Session automatically stops after one hour.", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString("${state.url}\nPIN: ${state.pin}")) }) {
                            Icon(Icons.Default.ContentCopy, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Copy connection details")
                        }
                    }
                }
                Text("Keep this screen or ArchiMan open. Stop sharing when finished. Use only on a trusted Wi-Fi network.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = viewModel::stopLocalPortal, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Stop portal")
                }
            } else {
                Button(onClick = viewModel::startLocalPortal, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Wifi, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start read-only portal")
                }
                Text("Nothing is uploaded to the internet. The address works only from devices that can reach this phone on the same Wi-Fi network.", style = MaterialTheme.typography.bodySmall)
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
