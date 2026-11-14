package com.holagundi.archiman.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.holagundi.archiman.sync.CloudCompany
import com.holagundi.archiman.sync.CloudException
import com.holagundi.archiman.sync.CloudSyncService
import com.holagundi.archiman.sync.SyncResult
import com.holagundi.archiman.sync.SyncSchema
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonInlineNotification
import com.holagundi.archiman.ui.theme.CarbonModal
import com.holagundi.archiman.ui.theme.CarbonNoticeKind
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

data class CloudSyncUiState(
    val configured: Boolean = true,
    val email: String? = null,
    val linkedCompany: String? = null,
    val pending: Int = 0,
    val companies: List<CloudCompany> = emptyList(),
    val counts: Map<String, Int> = emptyMap(),
    val busy: Boolean = false,
    val notice: String? = null,
    val failed: Boolean = false,
)

@HiltViewModel
class CloudSyncViewModel @Inject constructor(private val service: CloudSyncService) : ViewModel() {
    private val _state = MutableStateFlow(CloudSyncUiState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                val st = runCatching { service.status() }.getOrNull()
                CloudSyncUiState(
                    configured = service.configured,
                    email = service.signedInEmail(),
                    linkedCompany = st?.cloudCompany?.takeIf { st.enabled },
                    pending = st?.pending ?: 0,
                    counts = runCatching { service.localCounts() }.getOrDefault(emptyMap()),
                )
            }
            _state.value = snapshot.copy(companies = _state.value.companies, notice = _state.value.notice, failed = _state.value.failed)
        }
    }

    fun dismiss() {
        _state.value = _state.value.copy(notice = null)
    }

    private fun run(block: suspend () -> String?) {
        if (_state.value.busy) return
        _state.value = _state.value.copy(busy = true, notice = null, failed = false)
        viewModelScope.launch {
            var failed = false
            val message = try {
                withContext(Dispatchers.IO) { block() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: CloudException) {
                failed = true
                if (e.rejected) "The cloud refused this: ${e.message}" else e.message
            } catch (e: IOException) {
                failed = true
                "Could not reach the cloud. Your records stay on this phone and are sent next time."
            } catch (e: Exception) {
                failed = true
                e.message ?: "Cloud sync failed."
            }
            _state.value = _state.value.copy(busy = false, notice = message, failed = failed)
            refresh()
        }
    }

    fun signIn(email: String, password: String) = run {
        service.signIn(email, password)
        val companies = service.companies()
        _state.value = _state.value.copy(companies = companies)
        if (companies.isEmpty()) "Signed in. This account is not a member of any company yet; ask the owner to add you in MynaHub." else "Signed in."
    }

    fun signOut() = run {
        service.signOut()
        _state.value = _state.value.copy(companies = emptyList())
        "Signed out of the cloud. Your records are still on this phone."
    }

    fun loadCompanies() = run {
        _state.value = _state.value.copy(companies = service.companies())
        null
    }

    fun join(company: CloudCompany) = run { describe(service.join(company)) }

    fun syncNow() = run { describe(service.syncNow()) }

    private fun describe(r: SyncResult): String =
        "Synced: sent ${r.pushed + r.merged}, received ${r.pulled}" +
            (if (r.rejectedOlder > 0) ", ${r.rejectedOlder} older edit(s) kept in the cloud conflict list" else "") +
            (if (r.forbidden > 0) ", ${r.forbidden} change(s) your role may not send" else "") +
            (if (r.complete) "." else ". Some changes are still waiting; sync again.")
}

@Composable
fun CloudSyncPane(viewModel: CloudSyncViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var joining by remember { mutableStateOf<CloudCompany?>(null) }
    LaunchedEffect(state.email) { if (state.email != null && state.companies.isEmpty() && state.linkedCompany == null) viewModel.loadCompanies() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CarbonSpacing.screenPadding),
        verticalArrangement = CarbonSpacing.stackArrangement,
    ) {
        Text("Cloud sync", style = MaterialTheme.typography.titleLarge)
        Text("Keep clients, projects and tasks in step with MynaHub on your PC. Your records stay on this phone and work offline; syncing only moves changes.")
        if (!state.configured) {
            Text("This build has no cloud address configured, so sync is switched off.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (state.email == null) {
            OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Cloud account email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Cloud account password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Button(enabled = !state.busy && email.isNotBlank() && password.isNotEmpty(), onClick = { viewModel.signIn(email, password); password = "" }) { Text("Sign in to the cloud") }
        } else {
            Text("Signed in as ${state.email}", style = MaterialTheme.typography.titleMedium)
            if (state.linkedCompany != null) {
                Text("Linked to a cloud company. ${state.pending} change(s) waiting to be sent.")
                Button(enabled = !state.busy, onClick = viewModel::syncNow) { Text("Sync now") }
            } else {
                Text("Choose the company to join:")
                state.companies.forEach { c -> OutlinedButton(enabled = !state.busy, onClick = { joining = c }) { Text(c.name.ifBlank { c.id }) } }
                if (state.companies.isEmpty()) OutlinedButton(enabled = !state.busy, onClick = viewModel::loadCompanies) { Text("Check again") }
            }
            OutlinedButton(enabled = !state.busy, onClick = viewModel::signOut) { Text("Sign out of the cloud") }
        }
        if (state.busy) Text("Working…")
        state.notice?.let {
            CarbonInlineNotification(
                title = if (state.failed) "Cloud sync needs attention" else "Cloud sync",
                body = it,
                kind = if (state.failed) CarbonNoticeKind.ERROR else CarbonNoticeKind.SUCCESS,
                onDismiss = viewModel::dismiss,
            )
        }
    }
    joining?.let { company ->
        val c = state.counts
        CarbonModal(
            title = "Join ${company.name.ifBlank { company.id }}?",
            onDismiss = { joining = null },
            primaryLabel = "Upload and merge",
            onPrimary = { joining = null; viewModel.join(company) },
        ) {
            Text(
                "This phone has ${c[SyncSchema.CLIENT] ?: 0} client(s), ${c[SyncSchema.PROJECT] ?: 0} project(s) and ${c[SyncSchema.TASK] ?: 0} task(s). " +
                    "They are uploaded, and any with the same name as one already in the company are merged with it instead of duplicated. " +
                    "Then the company's records are brought to this phone.",
            )
        }
    }
}
