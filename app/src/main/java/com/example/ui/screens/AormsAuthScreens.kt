package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aorms.AormsSessionState
import com.example.aorms.AormsSessionStatus
import com.example.ui.viewmodel.SiteViewModel

/**
 * Full-screen sign-in gate: no part of the app is reachable until this
 * verifies against the AORMS identity server. See AormsSessionManager.
 */
@Composable
fun AormsLoginScreen(sessionState: AormsSessionState, viewModel: SiteViewModel, onOpenServerSettings: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val verifying = sessionState.status == AormsSessionStatus.VERIFYING
    val unconfigured = sessionState.status == AormsSessionStatus.UNCONFIGURED

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Sign in with your AORMS account", style = MaterialTheme.typography.headlineSmall)
            Text(
                "ArchiMan is an office-only app. Use the same email and password you use for AORMS.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            if (unconfigured) {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("AORMS server is not configured", style = MaterialTheme.typography.titleSmall)
                        Text(
                            sessionState.message.ifBlank { "An administrator must enter the office AORMS server address and product key first." },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onOpenServerSettings) { Text("Open server settings") }
                    }
                }
                return@Column
            }

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("AORMS email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = { TextButton(onClick = { showPassword = !showPassword }) { Text(if (showPassword) "Hide" else "Show") } },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = company, onValueChange = { company = it },
                label = { Text("Company (optional)") },
                placeholder = { Text("AORMS-C-XXXX, your login domain, or leave blank") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.signInToAorms(email.trim(), password, company.trim().ifBlank { null }) },
                enabled = !verifying && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp))
                Text(if (verifying) "Verifying…" else "Sign in")
            }
            if (sessionState.message.isNotBlank() && !verifying) {
                Spacer(Modifier.height(12.dp))
                Text(sessionState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenServerSettings) { Text("Server settings") }
        }
    }
}

/**
 * Practice-settings section: the office's AORMS server address and product
 * API key (entered once by an administrator, held only in encrypted local
 * storage — see AormsSessionManager), plus the currently signed-in identity.
 */
@Composable
fun AormsIdentitySection(viewModel: SiteViewModel) {
    val sessionState by viewModel.aormsSessionState.collectAsStateWithLifecycle()
    var baseUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.aormsServerConfig()?.let { baseUrl = it.baseUrl; apiKey = it.productApiKey }
    }

    Text("AORMS identity server", style = MaterialTheme.typography.titleSmall)
    Text(
        "Office-only sign-in. Every person who opens ArchiMan must have an AORMS account; enter the office AORMS server here once.",
        style = MaterialTheme.typography.bodySmall
    )
    OutlinedTextField(
        baseUrl, { baseUrl = it }, label = { Text("AORMS server URL") },
        placeholder = { Text("https://your-office.aorms.in") },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
    OutlinedTextField(
        apiKey, { apiKey = it }, label = { Text("Product API key") }, placeholder = { Text("hlp_sk_…") },
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { TextButton(onClick = { showKey = !showKey }) { Text(if (showKey) "Hide" else "Show") } }
    )
    Button(
        enabled = baseUrl.isNotBlank() && apiKey.isNotBlank(),
        onClick = { viewModel.saveAormsServerConfig(baseUrl, apiKey) },
        modifier = Modifier.fillMaxWidth()
    ) { Text("Save AORMS server") }

    if (sessionState.status == AormsSessionStatus.SIGNED_IN && sessionState.identity != null) {
        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.padding(12.dp)) {
                Text("Signed in as", style = MaterialTheme.typography.labelMedium)
                Text(sessionState.identity!!.name?.ifBlank { null } ?: sessionState.identity!!.email, style = MaterialTheme.typography.bodyMedium)
                Text(sessionState.identity!!.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                sessionState.identity!!.publicId?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                sessionState.company?.let { Text("Company: $it", style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.signOutOfAorms() }) { Text("Sign out") }
            }
        }
    }
}

/** Shown when a previously verified account can no longer reach AORMS. Access is never granted from a cache. */
@Composable
fun AormsUnreachableScreen(sessionState: AormsSessionState, viewModel: SiteViewModel) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Text("Can't reach AORMS", style = MaterialTheme.typography.headlineSmall)
            Text(
                sessionState.message.ifBlank { "ArchiMan needs to verify your AORMS account before opening. Check the network and try again." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))
            Button(onClick = { viewModel.retryAormsConnection() }) { Text("Retry") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.signOutOfAorms() }) { Text("Sign in with a different account") }
        }
    }
}
