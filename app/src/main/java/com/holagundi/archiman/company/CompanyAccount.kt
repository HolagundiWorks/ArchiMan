package com.holagundi.archiman.company

import com.holagundi.archiman.sync.KeyValues
import com.holagundi.archiman.sync.SecretBox
import org.json.JSONObject
import java.util.Base64

/** Only the login credential is retained. Identity, permissions and business records remain in memory. */
class CompanyAccountStore(private val values: KeyValues, private val box: SecretBox) {
    private var live: CompanySession? = null
    init {
        // Scrub the retired offline snapshot/draft format before using its login credential.
        if (values.get("session") != null) {
            val old = rawPayload()
            if (old == null || old.optString("url").isBlank() || old.optString("token").isBlank()) values.remove("session")
            else if (old.keys().asSequence().any { it !in setOf("url", "token") })
                write(JSONObject().put("url", old.getString("url")).put("token", old.getString("token")))
        }
    }
    fun connection(): CompanyConnection? = runCatching {
        values.get("connection")?.let { JSONObject(it).let { value -> CompanyConnection(value.getString("url"), value.getString("authBasePath")) } }
    }.getOrNull()
    @Synchronized fun connect(connection: CompanyConnection) {
        live = null
        values.put("connection", JSONObject().put("url", connection.url).put("authBasePath", connection.authBasePath).toString())
    }
    private fun rawPayload(): JSONObject? = runCatching {
        val sealed = values.get("session") ?: return null
        JSONObject(String(box.open(Base64.getDecoder().decode(sealed)), Charsets.UTF_8))
    }.getOrNull()
    private fun payload(): JSONObject? = rawPayload()?.takeIf { it.optString("url") == connection()?.url }
    fun savedToken(): String? = payload()?.optString("token")?.takeIf { it.isNotBlank() }
    @Synchronized fun load(): CompanySession? = live?.takeIf { it.connection == connection() && it.token == savedToken() }
    @Synchronized fun save(session: CompanySession) {
        write(JSONObject().put("url", session.connection.url).put("token", session.token))
        live = session
    }
    @Synchronized fun remember(session: CompanySession) {
        check(session.connection == connection() && session.token == savedToken())
        live = session
    }
    @Synchronized fun saveIfCurrent(token: String, session: CompanySession) {
        check(savedToken() == token) { "The company session changed." }
        save(session)
    }
    @Synchronized fun clearMemory() { live = null }
    @Synchronized fun clear() { live = null; values.remove("session") }
    private fun write(value: JSONObject) {
        values.put("session", Base64.getEncoder().encodeToString(box.seal(value.toString().toByteArray(Charsets.UTF_8))))
    }
}

/** Company data is obtained from the API. No project snapshot, draft or offline queue is persisted. */
class CompanyAccount(private val store: CompanyAccountStore, private val api: CompanyApiClient, private val liveApi: CompanyApiClient = api) {
    private val liveEpoch = java.util.concurrent.atomic.AtomicLong()
    var pending: PendingCompanyMfa? = null
        private set
    var enrollment: CompanyMfaSetup? = null
        private set
    fun fileStatus()=guarded {api.fileStatus(needSession())}
    fun uploadFile(upload:CompanyFileUpload)=guarded {api.uploadFile(needSession(),upload)}
    fun fileContent(file:CompanyFile)=guarded {api.fileContent(needSession(),file)}
    fun deleteFile(action:CompanyFileDelete)=guarded {api.deleteFile(needSession(),action)}
    fun changeSiteRecord(change:CompanySiteChange)=guarded {api.changeSiteRecord(needSession(),change)}
    fun taskAction(action:CompanyTaskAction)=guarded {api.taskAction(needSession(),action)}
    fun client(id:String)=guarded {api.client(needSession(),id)}
    fun changeClient(change:CompanyClientChange)=guarded {api.changeClient(needSession(),change)}
    fun projectClient(id:String)=guarded {api.projectClient(needSession(),id)}
    fun linkClient(link:CompanyClientLink)=guarded {api.linkClient(needSession(),link)}
    fun connection() = store.connection()
    fun session() = store.load()
    fun hasSavedSession() = store.savedToken() != null
    fun disconnect() { store.clearMemory() }
    fun cancelLive() { liveEpoch.incrementAndGet(); liveApi.cancelRequests() }
    fun connect(address: String) {
        val normalized = api.normalizeAddress(address)
        if (hasSavedSession() && normalized != store.connection()?.url) throw IllegalStateException("Sign out before changing company.")
        val connected = api.connect(normalized)
        cancelChallenge()
        store.connect(connected)
    }
    fun signIn(email: String, password: String) {
        cancelChallenge(); store.clear()
        when (val result = api.signIn(needConnection(), email, password)) {
            is CompanySignIn.Challenge -> pending = result.pending
            is CompanySignIn.Complete -> accept(result.token)
        }
    }
    fun verify(code: String, backup: Boolean) {
        accept(api.verifySignIn(needConnection(), pending ?: throw IllegalStateException("Start sign-in again."), code, backup))
        pending = null
    }
    fun cancelChallenge() { pending = null; enrollment = null }
    fun refresh() = guarded {
        val token = store.savedToken() ?: throw CompanyApiException("UNAUTHENTICATED", 401)
        store.saveIfCurrent(token, api.identity(needConnection(), token))
    }
    fun projects(after: String? = null): CompanyProjectPage = guarded { api.projects(needSession(), after) }
    fun liveProjects(after: String?, since: String?, taskProjectId: String? = null, taskAfter: String? = null, clients: Boolean = false, clientAfter: String? = null, assignees:Boolean=false, assigneeAfter:String?=null,siteProjectId:String?=null,siteAfter:String?=null,fileProjectId:String?=null,fileAfter:String?=null): CompanyLiveProjects {
        val generation = liveEpoch.get()
        return guarded({ generation == liveEpoch.get() }) {
            val session = needSession()
            val result = liveApi.liveProjects(session, after, since, taskProjectId, taskAfter, clients, clientAfter,assignees,assigneeAfter,siteProjectId,siteAfter,fileProjectId,fileAfter)
            if (generation != liveEpoch.get()) throw java.util.concurrent.CancellationException("Live request closed.")
            store.remember(CompanySession(session.connection, session.token, session.identity.copy(roles = result.roles, permissions = result.permissions)))
            result
        }
    }
    fun editProject(edit: CompanyProjectEdit): CompanyProjectEditResult = guarded { api.editProject(needSession(), edit) }
    fun createProject(draft:CompanyProjectCreate):CompanyProject = guarded {api.createProject(needSession(),draft)}
    fun deleteProject(action:CompanyProjectDelete):CompanyProjectDeleteResult = guarded {api.deleteProject(needSession(),action)}
    fun saveTask(edit: CompanyTaskEdit): CompanyTaskMutationResult = guarded { api.saveTask(needSession(), edit) }
    fun devices(): List<CompanyDeviceSession> = guarded { api.sessions(needSession()) }
    fun revoke(device: CompanyDeviceSession) = guarded {
        api.revoke(needSession(), device)
        if (device.current) { store.clear(); cancelChallenge() }
    }
    fun signOut() {
        val token = store.savedToken()
        val connection = store.connection()
        store.clear(); cancelChallenge()
        if (token != null && connection != null) api.signOut(connection, token)
    }
    fun acceptInvitation(token: String, name: String, password: String) { api.acceptInvitation(needConnection(), token, name, password) }
    fun enroll(password: String) = guarded { enrollment = api.enrollMfa(needSession(), password) }
    fun verifyEnrollment(code: String) = guarded { accept(api.verifyEnrollment(needSession(), code)); enrollment = null }
    private fun accept(token: String) { store.save(api.identity(needConnection(), token)) }
    private fun needConnection() = store.connection() ?: throw IllegalStateException("Connect to your company first.")
    private fun needSession() = store.load() ?: throw CompanyApiException("UNAUTHENTICATED", 401)
    private fun <T> guarded(current: () -> Boolean = { true }, block: () -> T): T {
        val token = store.savedToken()
        return try { block() } catch (e: CompanyApiException) {
            if (!current()) throw java.util.concurrent.CancellationException("Live request closed.")
            if (store.savedToken() == token) {
                if (e.endsSession) { store.clear(); cancelChallenge() } else if (e.status == 403) store.clearMemory()
            }
            throw e
        } catch (e: java.io.IOException) {
            if (!current()) throw java.util.concurrent.CancellationException("Live request closed.")
            if (store.savedToken() == token) store.clearMemory()
            throw e
        }
    }
}
