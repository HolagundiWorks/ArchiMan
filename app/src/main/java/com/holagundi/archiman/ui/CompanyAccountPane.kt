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
import androidx.lifecycle.withResumed
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.flow.update
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
    val fileProjectId:String?=null,val filePage:CompanyFilePage?=null,val fileStatus:CompanyFileStatus?=null,val canGoBackFiles:Boolean=false,
    val selectedFile:CompanySelectedFile?=null,val fileUpload:CompanyFileUploadState?=null,val fileDeletion:CompanyFileDeleteState?=null,val filePreview:CompanyFilePreview?=null,
    val siteProjectId:String?=null,val sitePage:CompanySitePage?=null,val siteEditor:CompanySiteEditorState?=null,val canGoBackSites:Boolean=false,
    val taskEditor: CompanyTaskEditorState? = null,
    val taskAction:CompanyTaskActionState?=null,val assigneePage:CompanyTaskCandidatePage?=null,val canGoBackAssignees:Boolean=false,
    val clientEditor:CompanyClientEditorState?=null,val clientLink:CompanyClientLinkState?=null,
    val showClients: Boolean = false, val clientPage: CompanyClientPage? = null, val canGoBackClients: Boolean = false,
    val creator:CompanyProjectCreateState?=null,val deletion:CompanyProjectDeleteState?=null,
)
data class CompanyProjectEditorState(val draft: CompanyProjectEdit, val sent: Boolean = false, val conflict: CompanyProject? = null)
data class CompanyTaskEditorState(val draft: CompanyTaskEdit, val sent: Boolean = false, val conflict: CompanyTask? = null)
data class CompanyProjectCreateState(val draft:CompanyProjectCreate=CompanyProjectCreate(),val sent:Boolean=false)
data class CompanyProjectDeleteState(val action:CompanyProjectDelete,val sent:Boolean=false,val conflict:CompanyProject?=null)

data class CompanyFileUploadState(val draft:CompanyFileUpload,val sent:Boolean=false)
data class CompanyFileDeleteState(val action:CompanyFileDelete,val sent:Boolean=false,val conflict:CompanyFile?=null)

data class CompanySiteEditorState(val draft:CompanySiteChange,val sent:Boolean=false,val conflict:CompanySiteRecord?=null)

data class CompanyTaskActionState(val draft:CompanyTaskAction,val sent:Boolean=false,val conflict:CompanyTask?=null)

data class CompanyClientEditorState(val draft:CompanyClientChange,val sent:Boolean=false,val conflict:CompanyClient?=null)
data class CompanyClientLinkState(val draft:CompanyClientLink,val sent:Boolean=false,val conflict:CompanyProjectClient?=null)

@HiltViewModel
class CompanyAccountViewModel @Inject constructor(service: CompanyAccountService,@ApplicationContext private val context:Context) : ViewModel() {
    private val account = service.account
    private val _state = MutableStateFlow(CompanyAccountState(address = account.connection()?.url.orEmpty(), hasCredential = account.hasSavedSession()))
    val state = _state.asStateFlow()
    private var liveJob: kotlinx.coroutines.Job? = null
    @Volatile private var foreground = false
    @Volatile private var epoch = 0
    private var after: String? = null
    private var revision: String? = null
    private val pages = mutableListOf<String?>()
    private var clientAfter: String? = null
    private val clientPages = mutableListOf<String?>()
    private var fileAfter:String?=null
    private val filePages=mutableListOf<String?>()
    private var siteAfter:String?=null
    private val sitePages=mutableListOf<String?>()
    private var assigneeAfter:String?=null
    private val assigneePages=mutableListOf<String?>()
    private var taskAfter: String? = null
    private val taskPages = mutableListOf<String?>()
    init { onResume() }

    private fun stopLive() { epoch++; account.cancelLive(); liveJob?.cancel(); liveJob = null; revision = null }
    private fun clearVisible(status: String) {
        fileAfter=null;filePages.clear()
        siteAfter=null;sitePages.clear()
        assigneeAfter=null;assigneePages.clear()
        clientAfter = null; clientPages.clear()
        taskAfter = null; taskPages.clear()
        _state.value = _state.value.copy(identity = null, projects = emptyList(), nextProjectCursor = null,
            clientEditor=null,clientLink=null,showClients = false, clientPage = null, canGoBackClients = false, fileProjectId=null,filePage=null,fileStatus=null,canGoBackFiles=false,selectedFile=null,fileUpload=null,fileDeletion=null,filePreview=null,siteProjectId=null,sitePage=null,siteEditor=null,canGoBackSites=false,devices = emptyList(), editor = null, taskEditor = null, taskAction=null,assigneePage=null,canGoBackAssignees=false, creator=null, deletion=null, taskProjectId = null, taskPage = null, canGoBackTasks = false, live = false, liveStatus = status, hasCredential = account.hasSavedSession())
    }
    private fun startLive() {
        if (!foreground || !account.hasSavedSession() || liveJob?.isActive == true || _state.value.busy) return
        val generation = epoch
        liveJob = viewModelScope.launch {
            while (kotlinx.coroutines.currentCoroutineContext().isActive && generation == epoch && foreground) {
                try {
                    if (account.session() == null) withContext(Dispatchers.IO) { account.refresh() }
                    val snapshot = withContext(Dispatchers.IO) { account.liveProjects(after, revision, _state.value.taskProjectId, taskAfter, _state.value.showClients, clientAfter, _state.value.taskAction?.draft?.deleting==false,assigneeAfter,_state.value.siteProjectId,siteAfter,_state.value.fileProjectId,fileAfter) }
                    if (generation != epoch || !foreground) return@launch
                    if ("clients.read" !in snapshot.permissions) { clientAfter = null; clientPages.clear() }
                    val editor = _state.value.editor?.takeIf { "projects.write" in snapshot.permissions && snapshot.page.projects.any { row -> row.id == it.draft.base.id } }
                    val taskEditor = _state.value.taskEditor?.takeIf { value ->
                        "tasks.write" in snapshot.permissions && snapshot.taskPage?.projectId == value.draft.projectId &&
                            (value.draft.base == null || snapshot.taskPage.tasks.any { it.id == value.draft.id })
                    }
                    _state.value = _state.value.copy(identity = account.session()?.identity, projects = snapshot.page.projects,
                        clientEditor=_state.value.clientEditor?.takeIf {editor->"clients.write" in snapshot.permissions &&
                            (!editor.draft.deleting || "clients.delete" in snapshot.permissions) && (editor.draft.base==null || snapshot.clientPage?.clients?.any {it.id==editor.draft.base.id}==true)},
                        clientLink=_state.value.clientLink?.takeIf {"clients.write" in snapshot.permissions && "projects.write" in snapshot.permissions && snapshot.page.projects.any {p->p.id==it.draft.base.projectId}},
                        clientPage = snapshot.clientPage, showClients = _state.value.showClients && "clients.read" in snapshot.permissions, canGoBackClients = clientPages.isNotEmpty(),
                        filePage=snapshot.filePage,fileProjectId=_state.value.fileProjectId?.takeIf {id->snapshot.page.projects.any {it.id==id}},canGoBackFiles=filePages.isNotEmpty(),
                        selectedFile=_state.value.selectedFile?.takeIf {"files.write" in snapshot.permissions},
                        fileUpload=_state.value.fileUpload?.takeIf {"files.write" in snapshot.permissions && snapshot.filePage?.projectId==it.draft.projectId},
                        fileDeletion=_state.value.fileDeletion?.takeIf {"files.delete" in snapshot.permissions && snapshot.filePage?.files?.any {f->f.id==it.action.base.id}==true},
                        filePreview=_state.value.filePreview?.takeIf {snapshot.filePage?.files?.contains(it.file)==true},
                        sitePage=snapshot.sitePage,siteProjectId=_state.value.siteProjectId?.takeIf {id->snapshot.page.projects.any {it.id==id}},canGoBackSites=sitePages.isNotEmpty(),
                        siteEditor=_state.value.siteEditor?.takeIf {editor->(if(editor.draft.deleting)"sites.delete" else "sites.write") in snapshot.permissions && snapshot.sitePage?.projectId==editor.draft.projectId && (editor.draft.base==null || snapshot.sitePage.records.any {it.id==editor.draft.base.id})},
                        taskAction=_state.value.taskAction?.takeIf {action->(if(action.draft.deleting)"tasks.delete" else "tasks.assign") in snapshot.permissions && snapshot.taskPage?.tasks?.any {it.id==action.draft.base.id}==true},
                        assigneePage=snapshot.assigneePage,canGoBackAssignees=assigneePages.isNotEmpty(),
                        taskPage = snapshot.taskPage, taskProjectId = _state.value.taskProjectId?.takeIf { id -> snapshot.page.projects.any { it.id == id } },
                        canGoBackTasks = taskPages.isNotEmpty(),
                        creator=_state.value.creator?.takeIf {"projects.create" in snapshot.permissions},
                        deletion=_state.value.deletion?.takeIf {value->"projects.delete" in snapshot.permissions && snapshot.page.projects.any {it.id==value.action.base.id}},
                        nextProjectCursor = snapshot.page.nextCursor, editor = editor, taskEditor = taskEditor, live = true,
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
        val actionEpoch=epoch
        _state.value = _state.value.copy(busy = true, notice = null)
        viewModelScope.launch {
            var notice = message
            var failed = false
            try { withContext(Dispatchers.IO) { block() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: CompanyApiException) {
                failed = true; notice = error.message ?: "Company request failed."
                if (error.status == 401 || error.status == 403 || error.errorCode in setOf("PROJECT_NOT_FOUND","TASK_NOT_FOUND")) clearVisible(notice)
            }
            catch (_: java.io.IOException) {
                failed = true; notice = "Connection lost. Reconnect to the company API."
                clearVisible(notice)
            }
            catch (error: IllegalStateException) { failed = true; notice = error.message ?: "Check the company connection." }
            catch (_: Exception) { failed = true; notice = "The company request could not be completed." }
            if(!foreground || epoch!=actionEpoch) {
                account.disconnect();clearVisible("Company data requires a live connection.")
                _state.value=_state.value.copy(busy=false)
                startLive();return@launch
            }
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
        _state.value = _state.value.copy(fileProjectId=null,filePage=null,fileStatus=null,fileUpload=null,fileDeletion=null,filePreview=null,canGoBackFiles=false,siteProjectId=null,sitePage=null,siteEditor=null,canGoBackSites=false,projects = emptyList(), editor = null, taskEditor = null, creator=null, deletion=null, taskProjectId = null, taskPage = null, canGoBackTasks = false, nextProjectCursor = null, live = false)
        startLive()
    }
    fun previousProjects() {
        if (_state.value.busy || pages.isEmpty()) return
        after = pages.removeAt(pages.lastIndex); projects()
    }
    fun showClients() {
        if (!_state.value.live || "clients.read" !in _state.value.identity?.permissions.orEmpty()) return
        stopLive(); clientAfter = null; clientPages.clear()
        _state.value = _state.value.copy(clientEditor=null,clientLink=null,showClients = !_state.value.showClients, clientPage = null, canGoBackClients = false)
        startLive()
    }
    fun nextClients(back: Boolean = false) {
        if (!_state.value.live || !_state.value.showClients) return
        if (back) {
            if (clientPages.isEmpty()) return
            clientAfter = clientPages.removeAt(clientPages.lastIndex)
        } else {
            val cursor = _state.value.clientPage?.nextCursor ?: return
            clientPages.add(clientAfter); clientAfter = cursor
        }
        stopLive(); _state.value = _state.value.copy(clientEditor=null,clientPage = null, canGoBackClients = clientPages.isNotEmpty())
        startLive()
    }
    fun startClient(client:CompanyClient?=null,deleting:Boolean=false) {
        val current=_state.value
        if(!current.live || current.busy || "clients.write" !in current.identity?.permissions.orEmpty() ||
            (deleting && (client==null || "clients.delete" !in current.identity?.permissions.orEmpty())) ||
            (client!=null && current.clientPage?.clients?.none {it.id==client.id}!=false))return
        _state.value=current.copy(clientEditor=CompanyClientEditorState(CompanyClientChange(base=client,deleting=deleting)),clientLink=null,editor=null,taskEditor=null,creator=null,deletion=null,notice=null)
    }
    fun changeClient(value:CompanyClient,confirmation:String) {
        val editor=_state.value.clientEditor ?: return
        if(!_state.value.busy && !editor.sent)_state.value=_state.value.copy(clientEditor=editor.copy(draft=editor.draft.copy(value=value,confirmation=confirmation)))
    }
    fun closeClient() {if(!_state.value.busy)_state.value=_state.value.copy(clientEditor=null)}
    fun reviewClient(keepDraft:Boolean) {
        val editor=_state.value.clientEditor ?: return;val current=editor.conflict ?: return
        if(!_state.value.live || _state.value.busy)return
        val draft=CompanyClientChange(base=current,deleting=editor.draft.deleting,value=if(keepDraft && !editor.draft.deleting)editor.draft.value.copy(version=current.version) else current)
        _state.value=_state.value.copy(clientEditor=CompanyClientEditorState(draft))
    }
    fun saveClient()=run("Company client saved.") {
        val editor=_state.value.clientEditor ?: return@run
        if(!editor.draft.valid || editor.conflict!=null)return@run
        _state.value=_state.value.copy(clientEditor=editor.copy(sent=true))
        val result=account.changeClient(editor.draft)
        if(_state.value.clientEditor?.draft?.operationId!=editor.draft.operationId)return@run
        when(result) {
            is CompanyClientChangeResult.Conflict -> {
                _state.value=_state.value.copy(clientEditor=editor.copy(sent=true,conflict=result.current))
                throw IllegalStateException("The client changed. Review the current record before saving again.")
            }
            else -> {clientAfter=null;clientPages.clear();_state.value=_state.value.copy(clientEditor=null,clientPage=null,canGoBackClients=false)}
        }
    }
    fun startClientLink(project:CompanyProject)=run("Choose the project's company client.") {
        if(!_state.value.live || "clients.write" !in _state.value.identity?.permissions.orEmpty() || "projects.write" !in _state.value.identity?.permissions.orEmpty())return@run
        val requestEpoch=epoch
        val link=account.projectClient(project.id)
        if(!foreground || epoch!=requestEpoch || _state.value.projects.none {it.id==project.id})return@run
        clientAfter=null;clientPages.clear()
        _state.value=_state.value.copy(clientLink=CompanyClientLinkState(CompanyClientLink(link)),clientEditor=null,showClients=true,clientPage=null,canGoBackClients=false,editor=null,taskEditor=null,creator=null,deletion=null)
    }
    fun chooseClientLink(id:String?) {
        val link=_state.value.clientLink ?: return
        if(!_state.value.busy && !link.sent && (id==null || _state.value.clientPage?.clients?.any {it.id==id}==true))
            _state.value=_state.value.copy(clientLink=link.copy(draft=link.draft.copy(clientId=id)))
    }
    fun closeClientLink() {if(!_state.value.busy)_state.value=_state.value.copy(clientLink=null)}
    fun reviewClientLink() {
        val current=_state.value.clientLink?.conflict ?: return
        if(_state.value.live && !_state.value.busy)_state.value=_state.value.copy(clientLink=CompanyClientLinkState(CompanyClientLink(current)))
    }
    fun saveClientLink()=run("Project client updated.") {
        val link=_state.value.clientLink ?: return@run
        if(!link.draft.valid || link.conflict!=null)return@run
        _state.value=_state.value.copy(clientLink=link.copy(sent=true))
        val result=account.linkClient(link.draft)
        if(_state.value.clientLink?.draft?.operationId!=link.draft.operationId)return@run
        when(result) {
            is CompanyClientLinkResult.Linked -> _state.value=_state.value.copy(clientLink=null)
            is CompanyClientLinkResult.Conflict -> {
                _state.value=_state.value.copy(clientLink=link.copy(sent=true,conflict=result.current))
                throw IllegalStateException("The project changed. Review its current client before linking again.")
            }
        }
    }
    fun selectLocalFile(uri:Uri,expectedOrigin:String)=run("Local file selected. Open its project and confirm the upload.") {
        if(!foreground || account.connection()?.url!=expectedOrigin || !account.hasSavedSession())return@run
        val generation=epoch
        account.refresh()
        if("files.write" !in account.session()?.identity?.permissions.orEmpty())throw CompanyApiException("PERMISSION_DENIED",403)
        val selected=readSelectedCompanyFile(context,uri)
        if(!foreground || epoch!=generation || account.connection()?.url!=expectedOrigin)return@run
        _state.update {current->if(!foreground || epoch!=generation || account.connection()?.url!=expectedOrigin)current else current.copy(selectedFile=selected,fileUpload=null)}
    }
    fun discardLocalFile() {if(!_state.value.busy)_state.value=_state.value.copy(selectedFile=null)}
    fun showFiles(projectId:String?)=run("Company attachment connection checked.") {
        if(!foreground || !_state.value.live || (projectId!=null && _state.value.projects.none {it.id==projectId}))return@run
        val generation=epoch
        val status=if(projectId==null)null else account.fileStatus()
        if(!foreground || epoch!=generation)return@run
        fileAfter=null;filePages.clear()
        _state.value=_state.value.copy(fileProjectId=projectId,filePage=null,fileStatus=status,fileUpload=null,fileDeletion=null,filePreview=null,canGoBackFiles=false)
    }
    fun nextFiles(back:Boolean=false) {
        if(_state.value.busy || !_state.value.live || _state.value.fileProjectId==null)return
        if(back){if(filePages.isEmpty())return;fileAfter=filePages.removeAt(filePages.lastIndex)}
        else {val cursor=_state.value.filePage?.nextCursor ?: return;filePages.add(fileAfter);fileAfter=cursor}
        stopLive();_state.value=_state.value.copy(filePage=null,fileUpload=null,fileDeletion=null,filePreview=null,canGoBackFiles=filePages.isNotEmpty());startLive()
    }
    fun startFileUpload() {
        val project=_state.value.fileProjectId ?: return;val selected=_state.value.selectedFile ?: return;val status=_state.value.fileStatus ?: return
        if(!_state.value.live || _state.value.busy || !status.available || "files.write" !in _state.value.identity?.permissions.orEmpty())return
        if(selected.size>status.maxBytes || selected.contentType !in status.contentTypes)return
        val upload=selected.upload(project)
        if(!upload.valid)return
        _state.value=_state.value.copy(fileUpload=CompanyFileUploadState(upload),selectedFile=null,fileDeletion=null,filePreview=null,notice=null)
    }
    fun closeFileUpload() {if(!_state.value.busy)_state.value=_state.value.copy(fileUpload=null)}
    fun uploadFile()=run("Company file uploaded.") {
        val upload=_state.value.fileUpload ?: return@run
        if(!upload.draft.valid)return@run
        _state.value=_state.value.copy(fileUpload=upload.copy(sent=true))
        account.uploadFile(upload.draft)
        if(_state.value.fileUpload?.draft?.operationId!=upload.draft.operationId)return@run
        fileAfter=null;filePages.clear();_state.value=_state.value.copy(fileUpload=null,filePage=null,canGoBackFiles=false)
    }
    fun openCompanyFile(file:CompanyFile)=run("Company file verified for this live session.") {
        if(!_state.value.live || _state.value.filePage?.files?.contains(file)!=true)return@run
        val generation=epoch;_state.value=_state.value.copy(filePreview=null)
        val content=account.fileContent(file);val preview=renderCompanyFile(context,content)
        if(!foreground || epoch!=generation || _state.value.fileProjectId!=file.projectId){preview.bitmap?.recycle();return@run}
        _state.update {current->if(!foreground || epoch!=generation || current.fileProjectId!=file.projectId)current else current.copy(filePreview=preview)}
    }
    fun companyFilePage(index:Int)=run("Company file page loaded.") {
        val previous=_state.value.filePreview ?: return@run
        if(!_state.value.live || index !in 0 until previous.pageCount || _state.value.filePage?.files?.contains(previous.file)!=true)return@run
        val generation=epoch;val preview=renderCompanyFile(context,previous.content,index)
        if(!foreground || epoch!=generation || _state.value.filePreview!==previous){preview.bitmap?.recycle();return@run}
        _state.update {current->if(!foreground || epoch!=generation || current.filePreview!==previous)current else current.copy(filePreview=preview)}
    }
    fun closeCompanyFile() {if(!_state.value.busy)_state.value=_state.value.copy(filePreview=null)}
    fun startDeleteFile(file:CompanyFile) {
        if(!_state.value.live || _state.value.busy || "files.delete" !in _state.value.identity?.permissions.orEmpty() || _state.value.filePage?.files?.contains(file)!=true)return
        _state.value=_state.value.copy(fileDeletion=CompanyFileDeleteState(CompanyFileDelete(file)),fileUpload=null,filePreview=null,notice=null)
    }
    fun confirmDeleteFile(text:String) {
        val deletion=_state.value.fileDeletion ?: return
        if(!_state.value.busy && !deletion.sent)_state.value=_state.value.copy(fileDeletion=deletion.copy(action=deletion.action.copy(confirmation=text)))
    }
    fun closeDeleteFile() {if(!_state.value.busy)_state.value=_state.value.copy(fileDeletion=null)}
    fun reviewDeleteFile() {
        val current=_state.value.fileDeletion?.conflict ?: return
        if(!_state.value.busy && _state.value.live)_state.value=_state.value.copy(fileDeletion=CompanyFileDeleteState(CompanyFileDelete(current)))
    }
    fun deleteFile()=run("Company file removed from live views.") {
        val deletion=_state.value.fileDeletion ?: return@run
        if(!deletion.action.valid || deletion.conflict!=null)return@run
        _state.value=_state.value.copy(fileDeletion=deletion.copy(sent=true))
        val result=account.deleteFile(deletion.action)
        if(_state.value.fileDeletion?.action?.operationId!=deletion.action.operationId)return@run
        when(result) {
            is CompanyFileResult.Conflict -> {
                _state.value=_state.value.copy(fileDeletion=deletion.copy(sent=true,conflict=result.current))
                throw IllegalStateException("The file changed. Review it and confirm deletion again.")
            }
            else -> {fileAfter=null;filePages.clear();_state.value=_state.value.copy(fileDeletion=null,filePreview=null,filePage=null,canGoBackFiles=false)}
        }
    }
    fun showSiteRecords(projectId:String?) {
        if(_state.value.busy || !_state.value.live || (projectId!=null && _state.value.projects.none {it.id==projectId}))return
        stopLive();siteAfter=null;sitePages.clear()
        _state.value=_state.value.copy(siteProjectId=projectId,sitePage=null,siteEditor=null,canGoBackSites=false)
        startLive()
    }
    fun nextSites(back:Boolean=false) {
        if(_state.value.busy || !_state.value.live || _state.value.siteProjectId==null)return
        if(back){if(sitePages.isEmpty())return;siteAfter=sitePages.removeAt(sitePages.lastIndex)}
        else {val cursor=_state.value.sitePage?.nextCursor ?: return;sitePages.add(siteAfter);siteAfter=cursor}
        stopLive();_state.value=_state.value.copy(sitePage=null,siteEditor=null,canGoBackSites=sitePages.isNotEmpty());startLive()
    }
    fun startSite(record:CompanySiteRecord?=null,deleting:Boolean=false) {
        val projectId=_state.value.siteProjectId ?: return
        if(!_state.value.live || _state.value.busy || (if(deleting)"sites.delete" else "sites.write") !in _state.value.identity?.permissions.orEmpty() ||
            (record!=null && _state.value.sitePage?.records?.none {it.id==record.id}!=false))return
        _state.value=_state.value.copy(siteEditor=CompanySiteEditorState(CompanySiteChange(projectId,record,deleting)),taskEditor=null,taskAction=null,editor=null,clientEditor=null,clientLink=null,creator=null,deletion=null,notice=null)
    }
    fun changeSite(value:CompanySiteRecord,confirmation:String) {
        val editor=_state.value.siteEditor ?: return
        if(!_state.value.busy && !editor.sent)_state.value=_state.value.copy(siteEditor=editor.copy(draft=editor.draft.copy(value=value,confirmation=confirmation)))
    }
    fun closeSite() {if(!_state.value.busy)_state.value=_state.value.copy(siteEditor=null)}
    fun reviewSite(keepDraft:Boolean) {
        val editor=_state.value.siteEditor ?: return;val current=editor.conflict ?: return
        if(!_state.value.live || _state.value.busy)return
        val draft=CompanySiteChange(current.projectId,current,editor.draft.deleting,value=if(keepDraft && !editor.draft.deleting)editor.draft.value.copy(version=current.version) else current)
        _state.value=_state.value.copy(siteEditor=CompanySiteEditorState(draft))
    }
    fun saveSite()=run("Company site record saved.") {
        val editor=_state.value.siteEditor ?: return@run
        if(!editor.draft.valid || editor.conflict!=null)return@run
        _state.value=_state.value.copy(siteEditor=editor.copy(sent=true))
        val result=account.changeSiteRecord(editor.draft)
        if(_state.value.siteEditor?.draft?.operationId!=editor.draft.operationId)return@run
        when(result) {
            is CompanySiteResult.Conflict -> {
                _state.value=_state.value.copy(siteEditor=editor.copy(sent=true,conflict=result.current))
                throw IllegalStateException("The site record changed. Review it before submitting again.")
            }
            else -> {siteAfter=null;sitePages.clear();_state.value=_state.value.copy(siteEditor=null,sitePage=null,canGoBackSites=false)}
        }
    }
    fun showTasks(projectId: String?) {
        if (_state.value.busy || !_state.value.live) return
        if (projectId != null && _state.value.projects.none { it.id == projectId }) return
        stopLive(); taskAfter = null; taskPages.clear()
        _state.value = _state.value.copy(taskProjectId = projectId, taskPage = null, taskEditor = null, taskAction=null,assigneePage=null,canGoBackTasks = false)
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
        stopLive(); _state.value = _state.value.copy(taskPage = null, taskEditor = null, taskAction=null,assigneePage=null,canGoBackTasks = taskPages.isNotEmpty())
        startLive()
    }
    fun startEdit(project: CompanyProject) {
        if (_state.value.live && !_state.value.busy && "projects.write" in _state.value.identity?.permissions.orEmpty())
            _state.value = _state.value.copy(editor = CompanyProjectEditorState(CompanyProjectEdit(project)), taskEditor = null, creator=null, deletion=null, clientEditor=null,clientLink=null,notice = null)
    }
    fun startTask(task: CompanyTask? = null) {
        val projectId = _state.value.taskProjectId ?: return
        if (!_state.value.live || _state.value.busy || "tasks.write" !in _state.value.identity?.permissions.orEmpty()) return
        if (task != null && (_state.value.taskPage?.tasks?.none { it.id == task.id } != false || task.projectId != projectId)) return
        _state.value = _state.value.copy(taskAction=null,assigneePage=null,taskEditor = CompanyTaskEditorState(CompanyTaskEdit(projectId,task)), editor = null, creator=null, deletion=null, notice = null)
    }
    fun startTaskAction(task:CompanyTask,deleting:Boolean=false) {
        if(!_state.value.live || _state.value.busy || (if(deleting)"tasks.delete" else "tasks.assign") !in _state.value.identity?.permissions.orEmpty() ||
            _state.value.taskPage?.tasks?.none {it.id==task.id}!=false)return
        stopLive();assigneeAfter=null;assigneePages.clear()
        _state.value=_state.value.copy(taskAction=CompanyTaskActionState(CompanyTaskAction(task,deleting)),assigneePage=null,canGoBackAssignees=false,taskEditor=null,editor=null,clientEditor=null,clientLink=null,creator=null,deletion=null,notice=null)
        startLive()
    }
    fun closeTaskAction() {
        if(_state.value.busy)return
        stopLive();assigneeAfter=null;assigneePages.clear()
        _state.value=_state.value.copy(taskAction=null,assigneePage=null,canGoBackAssignees=false);startLive()
    }
    fun confirmTaskAction(text:String) {
        val action=_state.value.taskAction ?: return
        if(!_state.value.busy && !action.sent)_state.value=_state.value.copy(taskAction=action.copy(draft=action.draft.copy(confirmation=text)))
    }
    fun toggleTaskMember(id:String) {
        val action=_state.value.taskAction ?: return
        if(_state.value.busy || action.sent || _state.value.assigneePage?.members?.none {it.membershipId==id}!=false)return
        val ids=action.draft.membershipIds
        if(id !in ids && ids.size>=20)return
        _state.value=_state.value.copy(taskAction=action.copy(draft=action.draft.copy(membershipIds=if(id in ids)ids-id else ids+id)))
    }
    fun clearTaskMembers() {
        val action=_state.value.taskAction ?: return
        if(!_state.value.busy && !action.sent)_state.value=_state.value.copy(taskAction=action.copy(draft=action.draft.copy(membershipIds=emptySet())))
    }
    fun nextAssignees(back:Boolean=false) {
        if(_state.value.busy || !_state.value.live || _state.value.taskAction?.draft?.deleting!=false)return
        if(back){if(assigneePages.isEmpty())return;assigneeAfter=assigneePages.removeAt(assigneePages.lastIndex)}
        else {val cursor=_state.value.assigneePage?.nextCursor ?: return;assigneePages.add(assigneeAfter);assigneeAfter=cursor}
        stopLive();_state.value=_state.value.copy(assigneePage=null,canGoBackAssignees=assigneePages.isNotEmpty());startLive()
    }
    fun reviewTaskAction() {
        val current=_state.value.taskAction?.conflict ?: return
        if(_state.value.busy || !_state.value.live)return
        val deleting=_state.value.taskAction!!.draft.deleting
        _state.value=_state.value.copy(taskAction=CompanyTaskActionState(CompanyTaskAction(current,deleting)))
    }
    fun saveTaskAction()=run("Company task action saved.") {
        val action=_state.value.taskAction ?: return@run
        if(!action.draft.valid || action.conflict!=null)return@run
        _state.value=_state.value.copy(taskAction=action.copy(sent=true))
        val result=account.taskAction(action.draft)
        if(_state.value.taskAction?.draft?.operationId!=action.draft.operationId)return@run
        when(result) {
            is CompanyTaskActionResult.Conflict -> {
                _state.value=_state.value.copy(taskAction=action.copy(sent=true,conflict=result.current))
                throw IllegalStateException("The task changed. Review it before changing assignees or confirming deletion again.")
            }
            else -> {taskAfter=null;taskPages.clear();assigneeAfter=null;assigneePages.clear();_state.value=_state.value.copy(taskAction=null,assigneePage=null,taskPage=null,canGoBackTasks=false,canGoBackAssignees=false)}
        }
    }
    fun changeTask(title: String, description: String, dueDate: String, status: String, priority: String) {
        val editor = _state.value.taskEditor ?: return
        if (!_state.value.busy && !editor.sent) _state.value = _state.value.copy(taskEditor = editor.copy(draft = editor.draft.copy(
            title = title, description = description, dueDate = dueDate, status = status, priority = priority)))
    }
    fun reviewTask(keepDraft: Boolean) {
        val editor = _state.value.taskEditor ?: return
        val current = editor.conflict ?: return
        if (!_state.value.busy && _state.value.live) {
            val draft = if (keepDraft) CompanyTaskEdit(current.projectId,current,title=editor.draft.title,description=editor.draft.description,
                dueDate=editor.draft.dueDate,dueAtReference=editor.draft.dueAt(),status=editor.draft.status,priority=editor.draft.priority)
            else CompanyTaskEdit(current.projectId,current)
            _state.value = _state.value.copy(taskEditor=CompanyTaskEditorState(draft))
        }
    }
    fun closeTask() { if (!_state.value.busy) _state.value = _state.value.copy(taskEditor = null) }
    fun saveTask() = run("Company task saved.") {
        val editor = _state.value.taskEditor ?: return@run
        if (editor.conflict != null || !editor.draft.valid) return@run
        _state.value = _state.value.copy(taskEditor=editor.copy(sent=true))
        val result = account.saveTask(editor.draft)
        // A completed network write must not restore company data after leaving/backgrounding.
        if (!_state.value.live || _state.value.taskProjectId != editor.draft.projectId) return@run
        when(result) {
            is CompanyTaskMutationResult.Applied -> _state.value = _state.value.copy(taskEditor=null)
            is CompanyTaskMutationResult.Conflict -> {
                _state.value = _state.value.copy(taskEditor=editor.copy(sent=true,conflict=result.current))
                throw IllegalStateException("The company task changed. Review both versions before saving again.")
            }
        }
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
    fun startCreateProject() {
        if(!_state.value.live || _state.value.busy || "projects.create" !in _state.value.identity?.permissions.orEmpty())return
        _state.value=_state.value.copy(creator=CompanyProjectCreateState(),deletion=null,editor=null,taskEditor=null,clientEditor=null,clientLink=null,notice=null)
    }
    fun changeCreateProject(name:String,code:String,status:String) {
        val creator=_state.value.creator ?: return
        if(!_state.value.busy && !creator.sent)_state.value=_state.value.copy(creator=creator.copy(draft=creator.draft.copy(name=name,code=code,status=status)))
    }
    fun closeCreateProject() {if(!_state.value.busy)_state.value=_state.value.copy(creator=null)}
    fun saveCreateProject()=run("Company project created.") {
        val creator=_state.value.creator ?: return@run
        if(!creator.draft.valid)return@run
        _state.value=_state.value.copy(creator=creator.copy(sent=true))
        account.createProject(creator.draft)
        if(_state.value.creator?.draft?.operationId!=creator.draft.operationId)return@run
        resetPages()
        _state.value=_state.value.copy(creator=null,projects=emptyList(),nextProjectCursor=null,canGoBack=false,live=false)
    }
    fun startDeleteProject(project:CompanyProject) {
        if(!_state.value.live || _state.value.busy || "projects.delete" !in _state.value.identity?.permissions.orEmpty() ||
            _state.value.projects.none {it.id==project.id})return
        _state.value=_state.value.copy(deletion=CompanyProjectDeleteState(CompanyProjectDelete(project)),creator=null,editor=null,taskEditor=null,clientEditor=null,clientLink=null,notice=null)
    }
    fun confirmDeleteProject(text:String) {
        val deletion=_state.value.deletion ?: return
        if(!_state.value.busy && !deletion.sent)_state.value=_state.value.copy(deletion=deletion.copy(action=deletion.action.copy(confirmation=text)))
    }
    fun reviewDeleteProject() {
        val current=_state.value.deletion?.conflict ?: return
        if(!_state.value.busy && _state.value.live)_state.value=_state.value.copy(deletion=CompanyProjectDeleteState(CompanyProjectDelete(current)))
    }
    fun closeDeleteProject() {if(!_state.value.busy)_state.value=_state.value.copy(deletion=null)}
    fun deleteProject()=run("Company project removed from live views.") {
        val deletion=_state.value.deletion ?: return@run
        if(deletion.conflict!=null || !deletion.action.valid)return@run
        _state.value=_state.value.copy(deletion=deletion.copy(sent=true))
        val result=account.deleteProject(deletion.action)
        if(_state.value.deletion?.action?.operationId!=deletion.action.operationId)return@run
        when(result) {
            is CompanyProjectDeleteResult.Deleted -> {
                resetPages();taskAfter=null;taskPages.clear()
                _state.value=_state.value.copy(deletion=null,projects=emptyList(),nextProjectCursor=null,canGoBack=false,live=false,
                    taskPage=null,taskProjectId=null,taskEditor=null,canGoBackTasks=false,editor=null)
            }
            is CompanyProjectDeleteResult.Conflict -> {
                _state.value=_state.value.copy(deletion=deletion.copy(sent=true,conflict=result.current))
                throw IllegalStateException("The project changed. Review its current revision before confirming deletion again.")
            }
        }
    }
    fun saveEdit() = run("Company project saved.") {
        val editor = _state.value.editor ?: return@run
        if (editor.conflict != null) return@run
        _state.value = _state.value.copy(editor = editor.copy(sent = true))
        val result = account.editProject(editor.draft)
        if(_state.value.editor?.draft?.operationId!=editor.draft.operationId)return@run
        when (result) {
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
    val filePicker = LocalCompanyFilePicker.current
    LaunchedEffect(filePicker.revision) {
        filePicker.take()?.let { (origin, uri) ->
            lifecycle.withResumed { viewModel.selectLocalFile(uri, origin) }
        }
    }

    DisposableEffect(lifecycle, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResume()
            if (event == Lifecycle.Event.ON_PAUSE) viewModel.onPause()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); viewModel.onPause() }
    }
    val view = LocalView.current
    DisposableEffect(view, state.enrollment != null || state.filePreview!=null) {
        var context = view.context
        while (context is android.content.ContextWrapper && context !is android.app.Activity) context = context.baseContext
        val window = (context as? android.app.Activity)?.window
        val secure = android.view.WindowManager.LayoutParams.FLAG_SECURE
        val wasSecure = window?.attributes?.flags?.and(secure) != 0
        if (state.enrollment != null || state.filePreview!=null) window?.addFlags(secure)
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
            if("files.write" in identity.permissions) {
                OutlinedButton(enabled=!state.busy && state.live,onClick={filePicker.choose(state.address)}) {Text("Choose local file for company upload")}
                Text("Choose a supported file up to 5 MiB, then open its project and confirm the upload.")
                state.selectedFile?.let {selected->
                    Text("Selected local file: ${selected.name} · ${selected.size} bytes")
                    OutlinedButton(enabled=!state.busy,onClick=viewModel::discardLocalFile) {Text("Discard selected local file")}
                }
            }

            if ("clients.read" in identity.permissions) {
                OutlinedButton(enabled = !state.busy && state.live, onClick = viewModel::showClients) {
                    Text(if (state.showClients) "Hide company clients" else "Show company clients")
                }
                if (state.showClients) {
                    Text("Live company client contacts", style = MaterialTheme.typography.titleMedium)
                    Text("Contacts stay available only while connected. Local practice clients remain separate.")
                    if("clients.write" in identity.permissions && state.clientEditor==null) OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.startClient()}) {Text("New company client")}
                    state.clientEditor?.takeIf {it.draft.base==null}?.let {CompanyClientForm(it,state.busy,viewModel::changeClient,viewModel::saveClient,viewModel::reviewClient,viewModel::closeClient)}
                    val contacts = state.clientPage
                    if (contacts == null) Text("Connecting to company clients…")
                    else {
                        if (contacts.clients.isEmpty()) Text("No company clients on this page.")
                        contacts.clients.forEach { client ->
                            Text(client.name, style = MaterialTheme.typography.titleSmall)
                            Text(client.kind.replace('_', ' '))
                            listOf(client.contactPerson, client.email, client.phone, client.address, client.correspondenceAddress,
                                client.preferredCommunication, client.notes).filter { it.isNotBlank() }.forEach { Text(it) }
                            if("clients.write" in identity.permissions && state.clientEditor?.draft?.base?.id!=client.id) {
                                OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.startClient(client)}) {Text("Edit company client")}
                                if("clients.delete" in identity.permissions) OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.startClient(client,true)}) {Text("Delete company client")}
                            }
                            state.clientEditor?.takeIf {it.draft.base?.id==client.id}?.let {CompanyClientForm(it,state.busy,viewModel::changeClient,viewModel::saveClient,viewModel::reviewClient,viewModel::closeClient)}
                        }
                        if (state.canGoBackClients) OutlinedButton(enabled = !state.busy, onClick = { viewModel.nextClients(true) }) { Text("Previous clients") }
                        if (contacts.nextCursor != null) OutlinedButton(enabled = !state.busy, onClick = { viewModel.nextClients() }) { Text("Next clients") }
                    }
                }
            }
            if ("projects.read" in identity.permissions) {
                if("projects.create" in identity.permissions && state.creator==null)
                    OutlinedButton(enabled=!state.busy && state.live,onClick=viewModel::startCreateProject) {Text("New company project")}
                state.creator?.let { creator ->
                    CompanyProjectCreator(creator,state.busy || !state.live,viewModel::changeCreateProject,viewModel::saveCreateProject,viewModel::closeCreateProject)
                }
                if (state.live && state.projects.isEmpty()) Text("No projects are assigned to this account.")
                state.projects.forEach { project ->
                    Text(project.name, style = MaterialTheme.typography.titleMedium)
                    Text("${project.code} · ${project.status.replace('_', ' ')} · Revision ${project.version}")
                    OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.showFiles(if(state.fileProjectId==project.id)null else project.id)}) {Text(if(state.fileProjectId==project.id)"Close company files" else "View company files")}
                    if(state.fileProjectId==project.id)CompanyFilesSection(state,state.busy || !state.live,viewModel::startFileUpload,viewModel::uploadFile,viewModel::closeFileUpload,viewModel::openCompanyFile,viewModel::companyFilePage,viewModel::closeCompanyFile,
                        viewModel::startDeleteFile,viewModel::confirmDeleteFile,viewModel::deleteFile,viewModel::reviewDeleteFile,viewModel::closeDeleteFile,viewModel::nextFiles)
                    OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.showSiteRecords(if(state.siteProjectId==project.id)null else project.id)}) {Text(if(state.siteProjectId==project.id)"Close company site records" else "View company site records")}
                    if(state.siteProjectId==project.id)CompanySiteRecordsSection(state,state.busy || !state.live,viewModel::startSite,viewModel::changeSite,viewModel::saveSite,viewModel::reviewSite,viewModel::closeSite,viewModel::nextSites)
                    if("clients.write" in identity.permissions && "projects.write" in identity.permissions) {
                        if(state.clientLink?.draft?.base?.projectId==project.id) {
                            state.clientLink?.let {CompanyClientLinkForm(it,state.clientPage,state.busy,viewModel::chooseClientLink,viewModel::saveClientLink,viewModel::reviewClientLink,viewModel::closeClientLink)}
                        } else OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.startClientLink(project)}) {Text("Manage project client")}
                    }
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
                                if(task.assignees.isNotEmpty())Text("Assigned to: ${task.assignees.joinToString {it.name}}")
                                Text(task.title, style = MaterialTheme.typography.titleSmall)
                                Text("${task.status.replace('_', ' ')} · ${task.priority} priority · Revision ${task.version}")
                                if (task.description.isNotBlank()) Text(task.description)
                                task.dueAt?.let { Text("Due: $it") }
                                if ("tasks.write" in identity.permissions && state.taskEditor == null)
                                    OutlinedButton(enabled = !state.busy && state.live, onClick = { viewModel.startTask(task) }) { Text("Edit company task") }
                            }
                            if(state.taskAction==null)taskPage.tasks.forEach {task->
                                if("tasks.assign" in identity.permissions)OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.startTaskAction(task)}) {Text("Manage assignees: ${task.title}")}
                                if("tasks.delete" in identity.permissions)OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.startTaskAction(task,true)}) {Text("Delete task: ${task.title}")}
                            }
                            state.taskAction?.let {action->CompanyTaskActionForm(action,state.assigneePage,state.busy || !state.live,state.canGoBackAssignees,
                                viewModel::toggleTaskMember,viewModel::clearTaskMembers,viewModel::confirmTaskAction,viewModel::saveTaskAction,viewModel::reviewTaskAction,viewModel::closeTaskAction,viewModel::nextAssignees)}
                            if (taskPage.nextCursor != null) OutlinedButton(enabled = !state.busy, onClick = { viewModel.nextTasks() }) { Text("Next tasks") }
                            if (state.canGoBackTasks) OutlinedButton(enabled = !state.busy, onClick = { viewModel.nextTasks(true) }) { Text("Previous tasks") }
                            if ("tasks.write" in identity.permissions && state.taskEditor == null)
                                OutlinedButton(enabled = !state.busy && state.live, onClick = { viewModel.startTask() }) { Text("New company task") }
                            state.taskEditor?.let { editor ->
                                CompanyTaskEditor(editor,state.busy || !state.live,viewModel::changeTask,viewModel::saveTask,viewModel::reviewTask,viewModel::closeTask)
                            }
                            Text(if ("tasks.write" in identity.permissions) "Task changes save online. Practice tasks on this phone remain separate." else
                                "Company tasks are read-only for this account. Practice tasks on this phone remain separate.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if ("projects.write" in identity.permissions && state.editor == null)
                        OutlinedButton(enabled = !state.busy && state.live, onClick = { viewModel.startEdit(project) }) { Text("Edit company project") }
                    if("projects.delete" in identity.permissions && state.deletion==null)
                        OutlinedButton(enabled=!state.busy && state.live,onClick={viewModel.startDeleteProject(project)}) {Text("Delete company project")}
                    state.deletion?.takeIf {it.action.base.id==project.id}?.let { deletion ->
                        CompanyProjectRemoval(deletion,state.busy || !state.live,viewModel::confirmDeleteProject,
                            viewModel::deleteProject,viewModel::reviewDeleteProject,viewModel::closeDeleteProject)
                    }
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
