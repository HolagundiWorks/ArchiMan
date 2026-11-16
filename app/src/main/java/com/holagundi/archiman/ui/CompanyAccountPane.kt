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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.holagundi.archiman.company.*
import com.holagundi.archiman.ui.theme.CarbonButton as Button
import com.holagundi.archiman.ui.theme.CarbonOutlinedButton as OutlinedButton
import com.holagundi.archiman.ui.theme.CarbonInlineNotification
import com.holagundi.archiman.ui.theme.CarbonNoticeKind
import com.holagundi.archiman.ui.theme.CarbonSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive

data class CompanyAccountState(
    val address: String = "", val identity: CompanyIdentity? = null, val pendingMfa: Boolean = false,
    val devices: List<CompanyDeviceSession> = emptyList(), val enrollment: CompanyMfaSetup? = null,
    val projects: List<CompanyProject> = emptyList(), val nextProjectCursor: String? = null,
    val busy: Boolean = false, val notice: String? = null, val failed: Boolean = false,
    val editor: CompanyProjectEditorState? = null, val live: Boolean = false, val liveStatus: String = "Connect to the company API.",
    val canGoBack: Boolean = false,
    val hasCredential: Boolean = false,
    val taskProjectId: String? = null, val taskPage: CompanyTaskPage? = null, val canGoBackTasks: Boolean = false,
)
data class CompanyProjectEditorState(val draft: CompanyProjectEdit, val sent: Boolean = false, val conflict: CompanyProject? = null)

@HiltViewModel
class CompanyAccountViewModel @Inject constructor(service: CompanyAccountService) : ViewModel() {
    private val account = service.account
    private val _state = MutableStateFlow(CompanyAccountState(address = account.connection()?.url.orEmpty(), hasCredential = account.hasSavedSession()))
    val state = _state.asStateFlow()
    private var liveJob: kotlinx.coroutines.Job? = null
    private var foreground = false
    private var epoch = 0
    private var after: String? = null
    private var revision: String? = null
    private val pages = mutableListOf<String?>()
    private var taskAfter: String? = null
    private val taskPages = mutableListOf<String?>()
    init { onResume() }

    private fun stopLive() { epoch++; account.cancelLive(); liveJob?.cancel(); liveJob = null; revision = null }
    private fun clearVisible(status: String) {
        taskAfter = null; taskPages.clear()
        _state.value = _state.value.copy(identity = null, projects = emptyList(), nextProjectCursor = null,
            devices = emptyList(), editor = null, taskProjectId = null, taskPage = null, canGoBackTasks = false, live = false, liveStatus = status, hasCredential = account.hasSavedSession())
    }
    private fun startLive() {
        if (!foreground || !account.hasSavedSession() || liveJob?.isActive == true || _state.value.busy) return
        val generation = epoch
        liveJob = viewModelScope.launch {
            while (kotlinx.coroutines.currentCoroutineContext().isActive && generation == epoch && foreground) {
                try {
                    if (account.session() == null) withContext(Dispatchers.IO) { account.refresh() }
                    val snapshot = withContext(Dispatchers.IO) { account.liveProjects(after, revision, _state.value.taskProjectId, taskAfter) }
                    if (generation != epoch || !foreground) return@launch
                    val editor = _state.value.editor?.takeIf { "projects.write" in snapshot.permissions && snapshot.page.projects.any { row -> row.id == it.draft.base.id } }
                    _state.value = _state.value.copy(identity = account.session()?.identity, projects = snapshot.page.projects,
                        taskPage = snapshot.taskPage, taskProjectId = _state.value.taskProjectId?.takeIf { id -> snapshot.page.projects.any { it.id == id } },
                        canGoBackTasks = taskPages.isNotEmpty(),
                        nextProjectCursor = snapshot.page.nextCursor, editor = editor, live = true,
                        liveStatus = if ("projects.read" in snapshot.permissions) "Live company projects" else "Live connection. This account has no project access.", canGoBack = pages.isNotEmpty())
                    revision = snapshot.revision
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: CompanyApiException) {
                    if (generation != epoch) return@launch
                    account.disconnect()
                    clearVisible(error.message ?: "Live updates stopped.")
                    revision = null
                    if (error.endsSession || error.status == 403) break
                    kotlinx.coroutines.delay(2000)
                } catch (_: Exception) {
                    if (generation != epoch) return@launch
                    account.disconnect()
                    clearVisible("Connection lost. Reconnecting; company data is unavailable.")
                    revision = null
                    kotlinx.coroutines.delay(2000)
                }
            }
        }
    }
    private fun run(message: String, block: () -> Unit) {
        if (_state.value.busy) return
        stopLive()
        _state.value = _state.value.copy(busy = true, notice = null)
        viewModelScope.launch {
            var notice = message
            var failed = false
            try { withContext(Dispatchers.IO) { block() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: CompanyApiException) {
                failed = true; notice = error.message ?: "Company request failed."
                if (error.status == 401 || error.status == 403 || error.errorCode == "PROJECT_NOT_FOUND") clearVisible(notice)
            }
            catch (_: java.io.IOException) {
                failed = true; notice = "Connection lost. Reconnect to the company API."
                clearVisible(notice)
            }
            catch (error: IllegalStateException) { failed = true; notice = error.message ?: "Check the company connection." }
            catch (_: Exception) { failed = true; notice = "The company request could not be completed." }
            _state.value = _state.value.copy(address = account.connection()?.url.orEmpty(), identity = account.session()?.identity,
                pendingMfa = account.pending != null, enrollment = account.enrollment, hasCredential = account.hasSavedSession(), busy = false, notice = notice, failed = failed)
            startLive()
        }
    }
    fun onResume() { foreground = true; startLive() }
    fun onPause() { foreground = false; stopLive(); account.disconnect(); clearVisible("Company data requires a live connection.") }
    override fun onCleared() { onPause(); account.cancelChallenge(); super.onCleared() }
    private fun resetPages() { after = null; pages.clear(); revision = null }
    fun connect(address: String) = run("Company API connected.") { resetPages(); account.connect(address) }
    fun signIn(email: String, password: String) = run("Sign-in processed. Enter your code if requested.") { resetPages(); account.signIn(email, password) }
    fun verify(code: String, backup: Boolean) = run("Signed in.") { account.verify(code, backup) }
    fun cancel() = run("Verification cancelled.") { account.cancelChallenge() }
    fun refresh() = run("Company connection checked.") { clearVisible("Connecting to company API."); account.refresh() }
    fun signOut() = run("Signed out of the company.") { resetPages(); clearVisible("Signed out."); account.signOut() }
    fun projects(more: Boolean = false) {
        if (_state.value.busy) return
        if (more) {
            val cursor = _state.value.nextProjectCursor ?: return
            pages.add(after); after = cursor
        }
        stopLive(); taskAfter = null; taskPages.clear()
        _state.value = _state.value.copy(projects = emptyList(), editor = null, taskProjectId = null, taskPage = null, canGoBackTasks = false, nextProjectCursor = null, live = false)
        startLive()
    }
    fun previousProjects() {
        if (_state.value.busy || pages.isEmpty()) return
        after = pages.removeAt(pages.lastIndex); projects()
    }
    fun showTasks(projectId: String?) {
        if (_state.value.busy || !_state.value.live) return
        if (projectId != null && _state.value.projects.none { it.id == projectId }) return
        stopLive(); taskAfter = null; taskPages.clear()
        _state.value = _state.value.copy(taskProjectId = projectId, taskPage = null, canGoBackTasks = false)
        startLive()
    }
    fun nextTasks(previous: Boolean = false) {
        if (_state.value.busy || !_state.value.live || _state.value.taskProjectId == null) return
        if (previous) {
            if (taskPages.isEmpty()) return
            taskAfter = taskPages.removeAt(taskPages.lastIndex)
        } else {
            val cursor = _state.value.taskPage?.nextCursor ?: return
            taskPages.add(taskAfter); taskAfter = cursor
        }
        stopLive(); _state.value = _state.value.copy(taskPage = null, canGoBackTasks = taskPages.isNotEmpty())
        startLive()
    }
    fun startEdit(project: CompanyProject) {
        if (_state.value.live && !_state.value.busy && "projects.write" in _state.value.identity?.permissions.orEmpty())
            _state.value = _state.value.copy(editor = CompanyProjectEditorState(CompanyProjectEdit(project)), notice = null)
    }
    fun changeEdit(name: String, code: String, status: String) {
        val editor = _state.value.editor ?: return
        if (!_state.value.busy && !editor.sent) _state.value = _state.value.copy(editor = editor.copy(draft = editor.draft.copy(name = name, code = code, status = status)))
    }
    fun reviewConflict(keepDraft: Boolean) {
        val editor = _state.value.editor ?: return
        val current = editor.conflict ?: return
        if (!_state.value.busy && _state.value.live) _state.value = _state.value.copy(editor = CompanyProjectEditorState(
            if (keepDraft) CompanyProjectEdit(current, editor.draft.name, editor.draft.code, editor.draft.status) else CompanyProjectEdit(current)))
    }
    fun closeEdit() { if (!_state.value.busy) _state.value = _state.value.copy(editor = null) }
    fun saveEdit() = run("Company project saved.") {
        val editor = _state.value.editor ?: return@run
        if (editor.conflict != null) return@run
        _state.value = _state.value.copy(editor = editor.copy(sent = true))
        when (val result = account.editProject(editor.draft)) {
            is CompanyProjectEditResult.Applied -> _state.value = _state.value.copy(editor = null)
            is CompanyProjectEditResult.Conflict -> {
                _state.value = _state.value.copy(editor = editor.copy(sent = true, conflict = result.current))
                throw IllegalStateException("The company project changed. Review both versions before saving again.")
            }
        }
    }
    fun devices() = run("Sessions loaded.") { _state.value = _state.value.copy(devices = account.devices()) }
    fun revoke(device: CompanyDeviceSession) = run("Session revoked.") {
        account.revoke(device)
        _state.value = _state.value.copy(devices = if (account.session() == null) emptyList() else account.devices())
    }
    fun invite(token: String, name: String, password: String) = run("Invitation accepted. Sign in to continue.") { account.acceptInvitation(token, name, password) }
    fun enroll(password: String) = run("Save your authenticator key and backup codes privately, then verify.") { account.enroll(password) }
    fun verifyEnrollment(code: String) = run("Two-factor authentication enabled.") { account.verifyEnrollment(code) }
    fun dismiss() { _state.value = _state.value.copy(notice = null) }
}

@Composable
fun CloudSyncPane(viewModel: CompanyAccountViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.onPause()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); viewModel.onPause() }
    }
    val view = LocalView.current
    DisposableEffect(view, state.enrollment != null) {
        var context = view.context
        while (context is android.content.ContextWrapper && context !is android.app.Activity) context = context.baseContext
        val window = (context as? android.app.Activity)?.window
        val secure = android.view.WindowManager.LayoutParams.FLAG_SECURE
        val wasSecure = window?.attributes?.flags?.and(secure) != 0
        if (state.enrollment != null) window?.addFlags(secure)
        onDispose { if (!wasSecure) window?.clearFlags(secure) }
    }
    var address by remember { mutableStateOf(state.address) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var backup by remember { mutableStateOf(false) }
    var invitation by remember { mutableStateOf(false) }
    var invitationToken by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var enrolling by remember { mutableStateOf(false) }
    LaunchedEffect(state.address) { if (state.address.isNotBlank()) address = state.address }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(CarbonSpacing.screenPadding), verticalArrangement = CarbonSpacing.stackArrangement) {
        Text("Company connection", style = MaterialTheme.typography.titleLarge)
        Text("Connect to your company's Myna API for live project updates. Company data requires a connection.")
        Text(state.liveStatus)
        OutlinedTextField(address, { address = it }, Modifier.fillMaxWidth(), label = { Text("Company API address") }, singleLine = true,
            enabled = !state.busy && !state.hasCredential && !state.pendingMfa, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
        if (state.identity == null && state.hasCredential && !state.pendingMfa) {
            OutlinedButton(enabled = !state.busy, onClick = viewModel::refresh) { Text("Reconnect to company") }
            OutlinedButton(enabled = !state.busy, onClick = viewModel::signOut) { Text("Sign out of company") }
        }
        if (state.identity == null && !state.hasCredential && !state.pendingMfa) {
            Button(enabled = !state.busy && address.isNotBlank(), onClick = { address = address.trim().trimEnd('/'); viewModel.connect(address) }) { Text("Connect to company") }
        }
        if (state.address.isNotBlank() && address.trim().trimEnd('/') != state.address && state.identity == null && !state.pendingMfa) {
            Text("Connect to the address above before signing in.")
        }
        if (state.address.isNotBlank() && address.trim().trimEnd('/') == state.address && state.identity == null && !state.hasCredential && !state.pendingMfa) {
            if (invitation) {
                OutlinedTextField(invitationToken, { invitationToken = it }, Modifier.fillMaxWidth(), label = { Text("Invitation token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Your name") }, singleLine = true)
            } else {
                OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("Company account email") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
            }
            OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text(if (invitation) "New password (12 or more characters)" else "Company account password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
            Button(enabled = !state.busy && password.isNotBlank() && (if (invitation) invitationToken.isNotBlank() && name.isNotBlank() && password.length >= 12 else email.isNotBlank()), onClick = {
                if (invitation) viewModel.invite(invitationToken, name, password) else viewModel.signIn(email, password)
                password = ""; invitationToken = ""
            }) { Text(if (invitation) "Accept invitation" else "Sign in to company") }
            OutlinedButton(enabled = !state.busy, onClick = { invitation = !invitation; password = ""; invitationToken = "" }) { Text(if (invitation) "Back to sign in" else "I have an invitation") }
        }
        if (state.pendingMfa) {
            Text(if (backup) "Enter one of your backup codes." else "Enter the current code from your authenticator.")
            OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text(if (backup) "Backup code" else "Verification code") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = if (backup) KeyboardType.Password else KeyboardType.NumberPassword))
            Button(enabled = !state.busy && code.isNotBlank(), onClick = { viewModel.verify(code, backup); code = "" }) { Text("Verify sign-in") }
            OutlinedButton(enabled = !state.busy, onClick = { backup = !backup; code = "" }) { Text(if (backup) "Use authenticator" else "Use backup code") }
            OutlinedButton(enabled = !state.busy, onClick = { code = ""; viewModel.cancel() }) { Text("Cancel sign-in") }
        }
        state.identity?.let { identity ->
            Text(identity.company, style = MaterialTheme.typography.titleMedium)
            Text("Signed in as ${identity.email}")
            Text("Roles: ${identity.roles.joinToString()}")
            Text("Permissions: ${identity.permissions.joinToString()}", style = MaterialTheme.typography.bodySmall)
            Text("Projects update live while this screen is open. A connection is required.")
            if ("projects.read" in identity.permissions) {
                if (state.live && state.projects.isEmpty()) Text("No projects are assigned to this account.")
                state.projects.forEach { project ->
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    Text("${project.code} · ${project.status.replace('_', ' ')} · Revision ${project.version}")
                    OutlinedButton(enabled = !state.busy && state.live, onClick = { viewModel.showTasks(if (state.taskProjectId == project.id) null else project.id) }) {
                        Text(if (state.taskProjectId == project.id) "Close company tasks" else "View company tasks")
                    }
                    if (state.taskProjectId == project.id) {
                        Text("Live company tasks", style = MaterialTheme.typography.titleSmall)
                        val taskPage = state.taskPage
                        if (taskPage == null) Text("Connecting to company tasks…")
                        else {
                            if (taskPage.tasks.isEmpty()) Text("No company tasks on this page.")
                            taskPage.tasks.forEach { task ->
                                Text(task.title, style = MaterialTheme.typography.titleSmall)
                                Text("${task.status.replace('_', ' ')} · ${task.priority} priority · Revision ${task.version}")
                                if (task.description.isNotBlank()) Text(task.description)
                                task.dueAt?.let { Text("Due: $it") }
                            }
                            if (taskPage.nextCursor != null) OutlinedButton(enabled = !state.busy, onClick = { viewModel.nextTasks() }) { Text("Next tasks") }
                            if (state.canGoBackTasks) OutlinedButton(enabled = !state.busy, onClick = { viewModel.nextTasks(true) }) { Text("Previous tasks") }
                            Text("Company tasks are read-only. Practice tasks on this phone remain separate.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if ("projects.write" in identity.permissions && state.editor == null)
                        OutlinedButton(enabled = !state.busy && state.live, onClick = { viewModel.startEdit(project) }) { Text("Edit company project") }
                }
                state.editor?.let { editor ->
                    CompanyProjectEditor(editor, state.busy || !state.live, viewModel::changeEdit, viewModel::saveEdit, viewModel::reviewConflict, viewModel::closeEdit)
                }
                if (state.nextProjectCursor != null) OutlinedButton(enabled = !state.busy, onClick = { viewModel.projects(true) }) { Text("Next projects") }
                if (state.canGoBack) OutlinedButton(enabled = !state.busy, onClick = viewModel::previousProjects) { Text("Previous projects") }
            }
            OutlinedButton(enabled = !state.busy, onClick = viewModel::refresh) { Text("Check account") }
            OutlinedButton(enabled = !state.busy, onClick = viewModel::devices) { Text("Show active sessions") }
            state.devices.forEach { device ->
                Text("${if (device.current) "This session" else "Other session"} · Created ${device.createdAt}")
                OutlinedButton(enabled = !state.busy, onClick = { viewModel.revoke(device) }) { Text(if (device.current) "Revoke this session" else "Revoke session") }
            }
            if (!identity.mfaEnabled) {
                OutlinedButton(enabled = !state.busy, onClick = { enrolling = !enrolling; password = "" }) { Text("Set up two-factor authentication") }
                if (enrolling && state.enrollment == null) {
                    OutlinedTextField(password, { password = it }, Modifier.fillMaxWidth(), label = { Text("Confirm password") }, singleLine = true, visualTransformation = PasswordVisualTransformation())
                    Button(enabled = !state.busy && password.isNotBlank(), onClick = { viewModel.enroll(password); password = "" }) { Text("Create authenticator setup") }
                }
            }
            state.enrollment?.let { setup ->
                val key = runCatching { android.net.Uri.parse(setup.uri).getQueryParameter("secret") }.getOrNull().orEmpty()
                Text("Authenticator setup key: $key")
                Text("Save these backup codes privately: ${setup.backupCodes.joinToString()}")
                OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("Authenticator code") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                Button(enabled = !state.busy && code.isNotBlank(), onClick = { viewModel.verifyEnrollment(code); code = "" }) { Text("Finish two-factor setup") }
            }
            OutlinedButton(enabled = !state.busy, onClick = { password = ""; code = ""; enrolling = false; viewModel.signOut() }) { Text("Sign out of company") }
        }
        if (state.busy) Text("Working…")
        state.notice?.let { notice -> CarbonInlineNotification(title = if (state.failed) "Company connection needs attention" else "Company connection", body = notice,
            kind = if (state.failed) CarbonNoticeKind.ERROR else CarbonNoticeKind.SUCCESS, onDismiss = viewModel::dismiss) }
    }
}
