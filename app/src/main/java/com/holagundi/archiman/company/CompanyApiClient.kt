package com.holagundi.archiman.company

import com.holagundi.archiman.sync.HttpResponse
import com.holagundi.archiman.sync.HttpTransport
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI

class CompanyApiException(val errorCode: String, val status: Int = 0) : Exception(when (errorCode) {
    "UNAUTHENTICATED" -> "Your company session has ended. Sign in again."
    "MEMBERSHIP_SUSPENDED", "MEMBERSHIP_INACTIVE", "NOT_A_MEMBER" -> "This account no longer has access to the company."
    "PERMISSION_DENIED" -> "Your account cannot perform this action."
    "MFA_REQUIRED" -> "Two-factor authentication is required for this action."
    "REAUTH_REQUIRED" -> "Sign in again before performing this action."
    "RATE_LIMITED" -> "Too many attempts. Wait a minute and try again."
    "INCOMPATIBLE_API" -> "This company API is not compatible with this app. Update the app or check the company address."
    "INVALID_ADDRESS" -> "Enter the company's HTTPS API address, without a path, query or password."
    "INVALID_CREDENTIALS" -> "Check the email, password or verification code and try again."
    "INVITATION_INVALID" -> "The invitation is invalid, expired or already used. Ask for a new invitation."
    "PROJECT_CODE_IN_USE" -> "That project code is already in use. Close this draft and reload before editing again."
    "OPERATION_ID_REUSED" -> "This edit cannot be retried with changed values. Close the draft and reload the project."
    "PROJECT_NOT_FOUND" -> "This project is no longer available to this account."
    "PROJECT_ID_IN_USE" -> "Check the live project list, then open a new project form."
    "INVALID_PROJECT_ACTION" -> "Check the project details or type DELETE to confirm removal."
    "CLIENT_IN_USE" -> "Unlink this client from active projects before deleting it."
    "CLIENT_NOT_FOUND" -> "This client is no longer available."
    "CLIENT_ID_IN_USE" -> "This client ID is reserved. Check live contacts and open a new form."
    "INVALID_CLIENT_ACTION" -> "Check client details or type DELETE to confirm removal."
    "INVALID_TASK_ACTION" -> "Check task assignees or type DELETE to confirm removal."
    "INVALID_ASSIGNEES" -> "Choose active company members who can read this project. Reload the assignee list."
    "INVALID_SITE_ACTION" -> "Check the site record dates, details or DELETE confirmation."
    "SITE_RECORD_NOT_FOUND" -> "This site record is no longer available."
    "SITE_RECORD_ID_IN_USE" -> "This site record ID is reserved. Open a new record form."
    "INVALID_FILE", "INVALID_FILE_ACTION" -> "Choose a supported file up to 5 MiB, or type DELETE to confirm removal."
    "FILE_NOT_FOUND", "FILE_CHANGED" -> "This file or its access changed. Reload the live file list."
    "FILE_ID_IN_USE" -> "This file ID is reserved. Check live files before starting another upload."
    "FILES_NOT_CONFIGURED", "FILE_STORAGE_UNAVAILABLE" -> "Private attachment storage is unavailable. Contact your company administrator."
    "FILE_INTEGRITY_ERROR" -> "The file could not be verified. Close it and contact your administrator."
    "INVALID_FILE_PREVIEW" -> "This file could not be previewed on this phone."
    "TASK_NOT_FOUND" -> "This task is no longer available to this account."
    "TASK_ID_IN_USE" -> "This task could not be created. Check the live task list, then open a new task."
    "TASK_CHANGED" -> "Task access or details changed. Close the editor and check the live task list."
    "INVALID_TASK" -> "Check the title, due date and task details before saving."
    "LOCAL_DATA_CLEAR_FAILED" -> "Company access was refused, but this phone could not clear its saved data. Check device storage and sign out."
    else -> "The company request could not be completed. Try again or contact your administrator."
}) {
    val endsSession get() = status == 401 || errorCode in setOf("MEMBERSHIP_SUSPENDED", "MEMBERSHIP_INACTIVE", "NOT_A_MEMBER")
}

data class CompanyConnection(val url: String, val authBasePath: String, val apiVersion: Int = 1)
data class CompanyIdentity(val userId: String, val email: String, val name: String, val company: String, val roles: List<String>, val permissions: List<String>, val mfaEnabled: Boolean, val organizationId: String = "")
data class CompanyProject(val id: String, val name: String, val code: String, val status: String, val version: Int)
data class CompanyProjectPage(val projects: List<CompanyProject>, val nextCursor: String?)
data class CompanyTask(val id: String, val projectId: String, val title: String, val description: String, val dueAt: String?, val status: String, val priority: String, val version: Int, val assignees:List<CompanyTaskAssignee> = emptyList())
data class CompanyTaskPage(val projectId: String, val tasks: List<CompanyTask>, val nextCursor: String?)
data class CompanyLiveProjects(val revision: String, val page: CompanyProjectPage, val roles: List<String>, val permissions: List<String>, val taskPage: CompanyTaskPage? = null, val clientPage: CompanyClientPage? = null, val assigneePage:CompanyTaskCandidatePage? = null,val sitePage:CompanySitePage?=null,val filePage:CompanyFilePage?=null)
data class CompanyProjectEdit(val base: CompanyProject, val name: String = base.name, val code: String = base.code,
    val status: String = base.status, val operationId: String = java.util.UUID.randomUUID().toString())
sealed interface CompanyProjectEditResult {
    data class Applied(val project: CompanyProject) : CompanyProjectEditResult
    data class Conflict(val current: CompanyProject) : CompanyProjectEditResult
}
class CompanySession(val connection: CompanyConnection, val token: String, val identity: CompanyIdentity) {
    override fun toString() = "CompanySession(redacted)"
}
class PendingCompanyMfa(val cookies: String) { override fun toString() = "PendingCompanyMfa(redacted)" }
sealed interface CompanySignIn {
    class Complete(val token: String) : CompanySignIn { override fun toString() = "Complete(redacted)" }
    class Challenge(val pending: PendingCompanyMfa) : CompanySignIn
}
class CompanyDeviceSession(val token: String, val createdAt: String, val expiresAt: String, val current: Boolean) {
    override fun toString() = "CompanyDeviceSession(redacted)"
}
class CompanyMfaSetup(val uri: String, val backupCodes: List<String>) { override fun toString() = "CompanyMfaSetup(redacted)" }

/** Only URLs supplied by the company's user are used. No Neon endpoint or database credential belongs here. */
class CompanyApiClient(private val http: HttpTransport, private val developmentLoopback: Boolean = false, private val versionCode: Int = 8) {
    fun cancelRequests() { http.cancel() }
    fun normalizeAddress(input: String): String {
        try {
            val uri = URI(input.trim())
            val loopback = developmentLoopback && uri.scheme == "http" && uri.host in setOf("localhost", "127.0.0.1", "10.0.2.2")
            if ((uri.scheme != "https" && !loopback) || uri.host.isNullOrEmpty() || uri.rawUserInfo != null || uri.rawQuery != null ||
                uri.rawFragment != null || uri.rawPath !in listOf("", "/") || uri.port < -1 || uri.port == 0 || uri.port > 65535) throw IllegalArgumentException()
            return uri.toString().trimEnd('/')
        } catch (_: Exception) { throw CompanyApiException("INVALID_ADDRESS") }
    }

    fun connect(address: String): CompanyConnection {
        val base = normalizeAddress(address)
        val health = json(send(base, "GET", "/health"))
        if (health.optString("service") != "myna-api" || health.optString("status") != "ok") throw CompanyApiException("INCOMPATIBLE_API")
        val info = json(send(base, "GET", "/v1/mobile/connection"))
        val auth = info.optString("authBasePath")
        if (info.optString("service") != "myna-api" || info.optInt("apiVersion", -1) != 1 ||
            info.optInt("minimumVersionCode", Int.MAX_VALUE) > versionCode || auth != "/api/auth" ||
            !strings(info.optJSONArray("capabilities")).containsAll(listOf("identity", "sessions", "two-factor", "invitations", "projects.live", "tasks.edit", "projects.create", "projects.delete", "clients.live", "clients.edit", "tasks.actions", "sites.live", "sites.edit", "files.live", "files.upload"))) throw CompanyApiException("INCOMPATIBLE_API")
        send(base, "GET", "/health/ready")
        return CompanyConnection(base, auth)
    }

    fun signIn(connection: CompanyConnection, email: String, password: String): CompanySignIn {
        val response = send(connection.url, "POST", "${connection.authBasePath}/sign-in/email", JSONObject().put("email", email.trim()).put("password", password))
        if (json(response).optBoolean("twoFactorRedirect")) {
            val cookies = response.headerAll("Set-Cookie").map { it.substringBefore(';').trim() }.filter { it.contains('=') && !it.endsWith('=') }.joinToString("; ")
            if (cookies.isBlank() || cookies.length > 8192 || cookies.any { it == '\r' || it == '\n' }) throw CompanyApiException("INVALID_RESPONSE")
            // A set-auth-token header in this response is a pending challenge, never a session.
            return CompanySignIn.Challenge(PendingCompanyMfa(cookies))
        }
        return CompanySignIn.Complete(sessionToken(response))
    }

    fun verifySignIn(connection: CompanyConnection, pending: PendingCompanyMfa, code: String, backup: Boolean): String =
        sessionToken(send(connection.url, "POST", "${connection.authBasePath}/two-factor/${if (backup) "verify-backup-code" else "verify-totp"}",
            JSONObject().put("code", code.trim()), cookies = pending.cookies))

    fun identity(connection: CompanyConnection, token: String): CompanySession {
        val me = json(send(connection.url, "GET", "/v1/me", token = token))
        val user = me.getJSONObject("user")
        val permissions = json(send(connection.url, "GET", "/v1/me/permissions", token = token))
        val identity = CompanyIdentity(user.getString("id"), user.getString("email"), user.getString("name"), me.getJSONObject("organization").getString("name"),
            strings(me.optJSONArray("roles")), strings(permissions.optJSONArray("permissions")), user.optBoolean("twoFactorEnabled"), me.getJSONObject("organization").getString("id"))
        return CompanySession(connection, token, identity)
    }

    fun acceptInvitation(connection: CompanyConnection, invitation: String, name: String, password: String) {
        send(connection.url, "POST", "/v1/invitations/accept", JSONObject().put("token", invitation.trim()).put("name", name.trim()).put("password", password))
    }

    fun projects(session: CompanySession, after: String? = null): CompanyProjectPage {
        if (after != null && (after.length > 100 || after.any { it == '\r' || it == '\n' })) throw CompanyApiException("INVALID_RESPONSE")
        val path = "/v1/projects?limit=25" + if (after == null) "" else "&after=${java.net.URLEncoder.encode(after, "UTF-8")}"
        val body = json(send(session.connection.url, "GET", path, token = session.token))
        val rows = body.getJSONArray("projects")
        if (rows.length() > 25) throw CompanyApiException("INVALID_RESPONSE")
        val projects = (0 until rows.length()).map { rows.getJSONObject(it).let { row ->
            CompanyProject(row.getString("id"), row.getString("name"), row.getString("code"), row.getString("status"), row.getInt("version"))
        } }
        val next = if (body.isNull("nextCursor")) null else body.getString("nextCursor")
        if (next != null && (next.length > 100 || next == after)) throw CompanyApiException("INVALID_RESPONSE")
        return CompanyProjectPage(projects, next)
    }
    fun project(session: CompanySession, id: String): CompanyProject {
        if (!id.matches(Regex("[0-9a-fA-F-]{36}"))) throw CompanyApiException("INVALID_RESPONSE")
        val value = readProject(json(send(session.connection.url, "GET", "/v1/projects/$id", token = session.token)))
        if (value.id != id) throw CompanyApiException("INVALID_RESPONSE")
        return value
    }
    fun liveProjects(session: CompanySession, after: String?, since: String?, taskProjectId: String? = null, taskAfter: String? = null, clients: Boolean = false, clientAfter: String? = null, assignees:Boolean=false, assigneeAfter:String?=null,siteProjectId:String?=null,siteAfter:String?=null,fileProjectId:String?=null,fileAfter:String?=null): CompanyLiveProjects {
        if (after != null && !after.matches(Regex("[0-9a-fA-F-]{36}"))) throw CompanyApiException("INVALID_RESPONSE")
        if (since != null && !since.matches(Regex("[a-f0-9]{64}"))) throw CompanyApiException("INVALID_RESPONSE")
        if (listOfNotNull(taskProjectId, taskAfter).any { !it.matches(Regex("[0-9a-fA-F-]{36}")) } || (taskAfter != null && taskProjectId == null)) throw CompanyApiException("INVALID_RESPONSE")
        if (clientAfter != null && (!clients || !clientAfter.matches(Regex("[0-9a-fA-F-]{36}")))) throw CompanyApiException("INVALID_RESPONSE")
        if((assignees && taskProjectId==null) || (assigneeAfter!=null && (!assignees || !validClientId(assigneeAfter))))throw CompanyApiException("INVALID_RESPONSE")
        if((siteProjectId!=null && !validClientId(siteProjectId)) || (siteAfter!=null && (siteProjectId==null || !validClientId(siteAfter))))throw CompanyApiException("INVALID_RESPONSE")
        if((fileProjectId!=null && !validClientId(fileProjectId)) || (fileAfter!=null && (fileProjectId==null || !validClientId(fileAfter))))throw CompanyApiException("INVALID_RESPONSE")
        val query = listOfNotNull(fileProjectId?.let {"fileProjectId=$it"},fileAfter?.let {"fileAfter=$it"},siteProjectId?.let {"siteProjectId=$it"},siteAfter?.let {"siteAfter=$it"},if(assignees)"assignees=true" else null,assigneeAfter?.let {"assigneeAfter=$it"},if (clients) "clients=true" else null, clientAfter?.let { "clientAfter=$it" }, after?.let { "after=$it" }, since?.let { "since=$it" }, taskProjectId?.let { "taskProjectId=$it" }, taskAfter?.let { "taskAfter=$it" }).joinToString("&")
        val result = json(send(session.connection.url, "GET", "/v1/mobile/projects/live" + if (query.isEmpty()) "" else "?$query", token = session.token))
        val revision = result.getString("revision")
        if (!revision.matches(Regex("[a-f0-9]{64}"))) throw CompanyApiException("INVALID_RESPONSE")
        val rows = result.getJSONArray("projects")
        if (rows.length() > 25) throw CompanyApiException("INVALID_RESPONSE")
        val projects = (0 until rows.length()).map { readProject(rows.getJSONObject(it)) }
        val next = if (result.isNull("nextCursor")) null else result.getString("nextCursor")
        if (next != null && (!next.matches(Regex("[0-9a-fA-F-]{36}")) || next == after)) throw CompanyApiException("INVALID_RESPONSE")
        val taskPage = if (result.isNull("taskPage")) null else result.getJSONObject("taskPage").let { page ->
            val projectId = page.getString("projectId")
            if (projectId != taskProjectId || projects.none { it.id == projectId }) throw CompanyApiException("INVALID_RESPONSE")
            val taskRows = page.getJSONArray("tasks")
            if (taskRows.length() > 25) throw CompanyApiException("INVALID_RESPONSE")
            val tasks = (0 until taskRows.length()).map { readCompanyTask(taskRows.getJSONObject(it), projectId) }
            if (tasks.map { it.id }.distinct().size != tasks.size) throw CompanyApiException("INVALID_RESPONSE")
            val cursor = if (page.isNull("nextCursor")) null else page.getString("nextCursor")
            if (cursor != null && (!cursor.matches(Regex("[0-9a-fA-F-]{36}")) || cursor == taskAfter || tasks.lastOrNull()?.id != cursor))
                throw CompanyApiException("INVALID_RESPONSE")
            CompanyTaskPage(projectId, tasks, cursor)
        }
        val contactPage = if (result.isNull("clientPage")) null else {
            if (!clients || "clients.read" !in strings(result.getJSONArray("permissions"))) throw CompanyApiException("INVALID_RESPONSE")
            readCompanyClients(result.getJSONObject("clientPage"), clientAfter)
        }
        val candidates=if(result.isNull("assigneePage"))null else {
            if(!assignees || taskProjectId==null || "tasks.assign" !in strings(result.getJSONArray("permissions")))throw CompanyApiException("INVALID_RESPONSE")
            readTaskCandidates(result.getJSONObject("assigneePage"),assigneeAfter)
        }
        val sites=if(result.isNull("sitePage"))null else {
            if(siteProjectId==null || projects.none {it.id==siteProjectId})throw CompanyApiException("INVALID_RESPONSE")
            readSitePage(result.getJSONObject("sitePage"),siteProjectId,siteAfter)
        }
        val files=if(result.isNull("filePage"))null else {
            if(fileProjectId==null || projects.none {it.id==fileProjectId})throw CompanyApiException("INVALID_RESPONSE")
            readCompanyFilePage(result.getJSONObject("filePage"),fileProjectId,fileAfter)
        }
        return CompanyLiveProjects(revision, CompanyProjectPage(projects, next), strings(result.getJSONArray("roles")), strings(result.getJSONArray("permissions")), taskPage, contactPage,candidates,sites,files)
    }

    fun sessions(session: CompanySession): List<CompanyDeviceSession> {
        val rows = JSONArray(send(session.connection.url, "GET", "${session.connection.authBasePath}/list-sessions", token = session.token).body)
        return (0 until rows.length()).map { rows.getJSONObject(it).let { row -> CompanyDeviceSession(row.getString("token"), row.optString("createdAt"), row.optString("expiresAt"), row.getString("token") == session.token.substringBefore('.')) } }
    }

    fun saveTask(session: CompanySession, edit: CompanyTaskEdit): CompanyTaskMutationResult {
        val fields = edit.fields()
        val payload = JSONObject(fields.toString()).put("operationId",edit.operationId)
        if (edit.base == null) payload.put("id",edit.id) else payload.put("expectedVersion",edit.base.version)
        val path = "/v1/projects/${edit.projectId}/tasks" + if (edit.base == null) "" else "/${edit.id}"
        val response = send(session.connection.url,if (edit.base == null) "POST" else "PATCH",path,payload,session.token,allowVersionConflict = true)
        val body = json(response)
        val result = if (response.code == 409) body.getJSONObject("conflict") else body
        if (result.getString("operationId") != edit.operationId) throw CompanyApiException("INVALID_RESPONSE")
        fun matchingDate(value: JSONObject) = if (value.isNull("dueAt")) edit.dueAt() == null else
            edit.dueAt()?.let { runCatching { java.time.Instant.parse(it) == java.time.Instant.parse(value.getString("dueAt")) }.getOrDefault(false) } == true
        fun matchingFields(value: JSONObject) = value.getString("title") == edit.title.trim() && value.getString("description") == edit.description &&
            value.getString("status") == edit.status && value.getString("priority") == edit.priority && matchingDate(value)
        return if (response.code == 409) {
            if (edit.base == null || result.getString("outcome") != "conflict" || result.getInt("expectedVersion") != edit.base.version ||
                !matchingFields(result.getJSONObject("submitted"))) throw CompanyApiException("INVALID_RESPONSE")
            val current = readCompanyTask(result.getJSONObject("current"),edit.projectId)
            if (current.id != edit.id || current.version == edit.base.version) throw CompanyApiException("INVALID_RESPONSE")
            CompanyTaskMutationResult.Conflict(current)
        } else {
            if (result.getString("outcome") != "applied" || !matchingFields(result.getJSONObject("task"))) throw CompanyApiException("INVALID_RESPONSE")
            val saved = readCompanyTask(result.getJSONObject("task"),edit.projectId)
            if (saved.id != edit.id || saved.version != (edit.base?.version ?: 0) + 1) throw CompanyApiException("INVALID_RESPONSE")
            CompanyTaskMutationResult.Applied(saved)
        }
    }

    fun editProject(session: CompanySession, edit: CompanyProjectEdit): CompanyProjectEditResult {
        validateProjectEdit(edit)
        val response = send(session.connection.url, "PATCH", "/v1/projects/${edit.base.id}",
            JSONObject().put("operationId", edit.operationId).put("expectedVersion", edit.base.version)
                .put("name", edit.name.trim()).put("code", edit.code.trim()).put("status", edit.status),
            session.token, allowVersionConflict = true)
        val body = json(response)
        val result = if (response.code == 409) body.getJSONObject("conflict") else body
        if (result.getString("operationId") != edit.operationId) throw CompanyApiException("INVALID_RESPONSE")
        fun project(row: JSONObject): CompanyProject {
            val value = CompanyProject(row.getString("id"), row.getString("name"), row.getString("code"), row.getString("status"), row.getInt("version"))
            if (value.id != edit.base.id || value.version < 1 || value.status !in setOf("active", "on_hold", "completed", "archived"))
                throw CompanyApiException("INVALID_RESPONSE")
            return value
        }
        return if (response.code == 409) {
            val submitted = result.getJSONObject("submitted")
            if (result.getString("outcome") != "conflict" || result.getInt("expectedVersion") != edit.base.version ||
                submitted.getString("name") != edit.name.trim() || submitted.getString("code") != edit.code.trim() || submitted.getString("status") != edit.status)
                throw CompanyApiException("INVALID_RESPONSE")
            CompanyProjectEditResult.Conflict(project(result.getJSONObject("current")))
        } else {
            if (result.getString("outcome") != "applied") throw CompanyApiException("INVALID_RESPONSE")
            val saved = project(result.getJSONObject("project"))
            if (saved.version != edit.base.version + 1) throw CompanyApiException("INVALID_RESPONSE")
            CompanyProjectEditResult.Applied(saved)
        }
    }

    fun createProject(session:CompanySession,draft:CompanyProjectCreate):CompanyProject {
        if(!draft.valid)throw CompanyApiException("INVALID_PROJECT_ACTION")
        val body=json(send(session.connection.url,"POST","/v1/projects",JSONObject().put("id",draft.id).put("operationId",draft.operationId)
            .put("name",draft.name.trim()).put("code",draft.code.trim()).put("status",draft.status),session.token))
        if(body.getString("outcome")!="created" || body.getString("operationId")!=draft.operationId)throw CompanyApiException("INVALID_RESPONSE")
        val project=readLifecycleProject(body.getJSONObject("project"),draft.id)
        if(project.version!=1 || project.name!=draft.name.trim() || project.code!=draft.code.trim() || project.status!=draft.status)
            throw CompanyApiException("INVALID_RESPONSE")
        return project
    }
    fun deleteProject(session:CompanySession,action:CompanyProjectDelete):CompanyProjectDeleteResult {
        if(!action.valid)throw CompanyApiException("INVALID_PROJECT_ACTION")
        val response=send(session.connection.url,"DELETE","/v1/projects/${action.base.id}",JSONObject().put("operationId",action.operationId)
            .put("expectedVersion",action.base.version).put("confirmation",action.confirmation),session.token,allowVersionConflict=true)
        val body=json(response)
        val result=if(response.code==409)body.getJSONObject("conflict") else body
        if(result.getString("operationId")!=action.operationId)throw CompanyApiException("INVALID_RESPONSE")
        return if(response.code==409) {
            if(result.getString("outcome")!="conflict" || result.getInt("expectedVersion")!=action.base.version)throw CompanyApiException("INVALID_RESPONSE")
            val current=readLifecycleProject(result.getJSONObject("current"),action.base.id)
            if(current.version==action.base.version)throw CompanyApiException("INVALID_RESPONSE")
            CompanyProjectDeleteResult.Conflict(current)
        }else{
            if(result.getString("outcome")!="deleted" || result.getString("id")!=action.base.id || result.getInt("version")!=action.base.version+1 ||
                !validDeletionTime(result.getString("deletedAt")))throw CompanyApiException("INVALID_RESPONSE")
            CompanyProjectDeleteResult.Deleted(action.base.id,result.getInt("version"),result.getString("deletedAt"))
        }
    }

    fun fileStatus(session:CompanySession):CompanyFileStatus {
        val body=json(send(session.connection.url,"GET","/v1/files/status",token=session.token))
        val max=body.getInt("maxBytes");val types=strings(body.getJSONArray("contentTypes"))
        if(max !in 1..COMPANY_MAX_FILE_BYTES || types.isEmpty() || types.size!=types.distinct().size || types.any {it !in companyFileTypes})throw CompanyApiException("INVALID_RESPONSE")
        return CompanyFileStatus(body.getBoolean("available"),max,types.toSet())
    }
    fun uploadFile(session:CompanySession,upload:CompanyFileUpload):CompanyFile {
        if(!upload.valid)throw CompanyApiException("INVALID_FILE")
        val body=json(send(session.connection.url,"POST","/v1/projects/${upload.projectId}/files",JSONObject().put("id",upload.id).put("operationId",upload.operationId)
            .put("name",upload.name).put("contentType",upload.contentType).put("contentBase64",upload.encoded()),session.token))
        if(body.getString("outcome")!="uploaded" || body.getString("operationId")!=upload.operationId)throw CompanyApiException("INVALID_RESPONSE")
        val file=readCompanyFile(body.getJSONObject("file"),upload.projectId)
        if(file.id!=upload.id || file.name!=upload.name || file.contentType!=upload.contentType || file.size!=upload.size || file.sha256!=upload.sha256 || file.version!=1)throw CompanyApiException("INVALID_RESPONSE")
        return file
    }
    fun fileContent(session:CompanySession,file:CompanyFile):CompanyFileContent {
        if(!validClientId(file.id) || !validClientId(file.projectId))throw CompanyApiException("INVALID_RESPONSE")
        val body=json(send(session.connection.url,"GET","/v1/projects/${file.projectId}/files/${file.id}/content",token=session.token))
        if(readCompanyFile(body.getJSONObject("file"),file.projectId)!=file)throw CompanyApiException("FILE_CHANGED")
        val encoded=body.getString("contentBase64")
        if(encoded.length>((COMPANY_MAX_FILE_BYTES+2)/3)*4)throw CompanyApiException("INVALID_RESPONSE")
        val bytes=runCatching {java.util.Base64.getDecoder().decode(encoded)}.getOrElse {throw CompanyApiException("INVALID_RESPONSE")}
        if(bytes.size!=file.size || fileHash(bytes)!=file.sha256)throw CompanyApiException("FILE_INTEGRITY_ERROR")
        return CompanyFileContent(file,bytes)
    }
    fun deleteFile(session:CompanySession,action:CompanyFileDelete):CompanyFileResult {
        if(!action.valid)throw CompanyApiException("INVALID_FILE_ACTION")
        val response=send(session.connection.url,"DELETE","/v1/projects/${action.base.projectId}/files/${action.base.id}",JSONObject().put("operationId",action.operationId)
            .put("expectedVersion",action.base.version).put("confirmation",action.confirmation),session.token,allowVersionConflict=true)
        val body=json(response);val result=if(response.code==409)body.getJSONObject("conflict") else body
        if(result.getString("operationId")!=action.operationId)throw CompanyApiException("INVALID_RESPONSE")
        if(response.code==409) {
            val current=readCompanyFile(result.getJSONObject("current"),action.base.projectId)
            if(result.getString("outcome")!="conflict" || result.getInt("expectedVersion")!=action.base.version || current.id!=action.base.id || current.version==action.base.version)throw CompanyApiException("INVALID_RESPONSE")
            return CompanyFileResult.Conflict(current)
        }
        if(result.getString("outcome")!="deleted" || result.getString("id")!=action.base.id || result.getString("projectId")!=action.base.projectId || result.getInt("version")!=action.base.version+1 || !validDeletionTime(result.getString("deletedAt")))throw CompanyApiException("INVALID_RESPONSE")
        return CompanyFileResult.Deleted(action.base.id)
    }

    fun changeSiteRecord(session:CompanySession,change:CompanySiteChange):CompanySiteResult {
        if(!change.valid)throw CompanyApiException("INVALID_SITE_ACTION")
        val payload=if(change.deleting)JSONObject().put("confirmation",change.confirmation) else change.fields()
        payload.put("operationId",change.operationId)
        if(change.base==null)payload.put("id",change.value.id) else payload.put("expectedVersion",change.base.version)
        val response=send(session.connection.url,if(change.deleting)"DELETE" else if(change.base==null)"POST" else "PATCH",
            "/v1/projects/${change.projectId}/site-records"+if(change.base==null)"" else "/${change.base.id}",payload,session.token,allowVersionConflict=true)
        val body=json(response);val result=if(response.code==409)body.getJSONObject("conflict") else body
        if(result.getString("operationId")!=change.operationId)throw CompanyApiException("INVALID_RESPONSE")
        if(response.code==409) {
            val current=readSiteRecord(result.getJSONObject("current"),change.projectId)
            if(change.base==null || result.getString("outcome")!="conflict" || result.getInt("expectedVersion")!=change.base.version || current.id!=change.base.id || current.version==change.base.version)throw CompanyApiException("INVALID_RESPONSE")
            return CompanySiteResult.Conflict(current)
        }
        if(change.deleting) {
            if(result.getString("outcome")!="deleted" || result.getString("id")!=change.value.id || result.getString("projectId")!=change.projectId || result.getInt("version")!=change.base!!.version+1 || !validDeletionTime(result.getString("deletedAt")))throw CompanyApiException("INVALID_RESPONSE")
            return CompanySiteResult.Deleted(change.value.id)
        }
        val record=readSiteRecord(result.getJSONObject("record"),change.projectId)
        if(result.getString("outcome")!=(if(change.base==null)"created" else "applied") || record.id!=change.value.id || record.version!=(change.base?.version ?: 0)+1)throw CompanyApiException("INVALID_RESPONSE")
        val actual=CompanySiteChange(record.projectId,record).fields();val expected=change.fields()
        if(expected.keys().asSequence().any {actual.get(it).toString()!=expected.get(it).toString()})throw CompanyApiException("INVALID_RESPONSE")
        return CompanySiteResult.Saved(record)
    }

    fun taskAction(session:CompanySession,action:CompanyTaskAction):CompanyTaskActionResult {
        if(!action.valid)throw CompanyApiException("INVALID_TASK_ACTION")
        val payload=JSONObject().put("operationId",action.operationId).put("expectedVersion",action.base.version)
        if(action.deleting)payload.put("confirmation",action.confirmation) else payload.put("membershipIds",JSONArray(action.membershipIds.sorted()))
        val response=send(session.connection.url,if(action.deleting)"DELETE" else "PUT","/v1/projects/${action.base.projectId}/tasks/${action.base.id}"+if(action.deleting)"" else "/assignees",payload,session.token,allowVersionConflict=true)
        val body=json(response);val result=if(response.code==409)body.getJSONObject("conflict") else body
        if(result.getString("operationId")!=action.operationId)throw CompanyApiException("INVALID_RESPONSE")
        if(response.code==409) {
            val current=readCompanyTask(result.getJSONObject("current"),action.base.projectId)
            if(result.getString("outcome")!="conflict" || result.getInt("expectedVersion")!=action.base.version || current.id!=action.base.id || current.version==action.base.version)throw CompanyApiException("INVALID_RESPONSE")
            return CompanyTaskActionResult.Conflict(current)
        }
        if(action.deleting) {
            if(result.getString("outcome")!="deleted" || result.getString("id")!=action.base.id || result.getString("projectId")!=action.base.projectId || result.getInt("version")!=action.base.version+1 || !validDeletionTime(result.getString("deletedAt")))throw CompanyApiException("INVALID_RESPONSE")
            return CompanyTaskActionResult.Deleted(action.base.id)
        }
        val task=readCompanyTask(result.getJSONObject("task"),action.base.projectId)
        if(result.getString("outcome")!="assigned" || task.id!=action.base.id || task.version!=action.base.version+1 || task.assignees.map {it.membershipId}.toSet()!=action.membershipIds)throw CompanyApiException("INVALID_RESPONSE")
        return CompanyTaskActionResult.Assigned(task)
    }

    fun client(session:CompanySession,id:String):CompanyClient {
        if(!validClientId(id))throw CompanyApiException("INVALID_RESPONSE")
        val value=readMutationClient(json(send(session.connection.url,"GET","/v1/clients/$id",token=session.token)))
        if(value.id!=id)throw CompanyApiException("INVALID_RESPONSE")
        return value
    }
    fun changeClient(session:CompanySession,change:CompanyClientChange):CompanyClientChangeResult {
        if(!change.valid)throw CompanyApiException("INVALID_CLIENT_ACTION")
        val payload=if(change.deleting)JSONObject().put("confirmation",change.confirmation) else change.fields()
        payload.put("operationId",change.operationId)
        if(change.base==null)payload.put("id",change.value.id) else payload.put("expectedVersion",change.base.version)
        val response=send(session.connection.url,if(change.deleting)"DELETE" else if(change.base==null)"POST" else "PATCH",
            "/v1/clients"+if(change.base==null)"" else "/${change.base.id}",payload,session.token,allowVersionConflict=true)
        val body=json(response); val result=if(response.code==409)body.getJSONObject("conflict") else body
        if(result.getString("operationId")!=change.operationId)throw CompanyApiException("INVALID_RESPONSE")
        if(response.code==409) {
            val current=readMutationClient(result.getJSONObject("current"))
            if(change.base==null || result.getString("outcome")!="conflict" || result.getInt("expectedVersion")!=change.base.version || current.id!=change.base.id || current.version==change.base.version)throw CompanyApiException("INVALID_RESPONSE")
            return CompanyClientChangeResult.Conflict(current)
        }
        if(change.deleting) {
            if(result.getString("outcome")!="deleted" || result.getString("id")!=change.value.id || result.getInt("version")!=change.base!!.version+1 || !validDeletionTime(result.getString("deletedAt")))throw CompanyApiException("INVALID_RESPONSE")
            return CompanyClientChangeResult.Deleted(change.value.id)
        }
        val client=readMutationClient(result.getJSONObject("client"))
        if(result.getString("outcome")!=(if(change.base==null)"created" else "applied") || client.id!=change.value.id || client.version!=(change.base?.version ?: 0)+1)throw CompanyApiException("INVALID_RESPONSE")
        val actual=CompanyClientChange(base=client).fields();val expected=change.fields()
        if(expected.keys().asSequence().any {actual.getString(it)!=expected.getString(it)})throw CompanyApiException("INVALID_RESPONSE")
        return CompanyClientChangeResult.Saved(client)
    }
    fun projectClient(session:CompanySession,id:String):CompanyProjectClient {
        if(!validClientId(id))throw CompanyApiException("INVALID_RESPONSE")
        return readProjectClient(json(send(session.connection.url,"GET","/v1/projects/$id/client",token=session.token)),id)
    }
    fun linkClient(session:CompanySession,link:CompanyClientLink):CompanyClientLinkResult {
        if(!link.valid)throw CompanyApiException("INVALID_CLIENT_ACTION")
        val response=send(session.connection.url,"PUT","/v1/projects/${link.base.projectId}/client",JSONObject().put("clientId",link.clientId ?: JSONObject.NULL)
            .put("operationId",link.operationId).put("expectedVersion",link.base.version),session.token,allowVersionConflict=true)
        val body=json(response);val result=if(response.code==409)body.getJSONObject("conflict") else body
        if(result.getString("operationId")!=link.operationId)throw CompanyApiException("INVALID_RESPONSE")
        val current=readProjectClient(result.getJSONObject(if(response.code==409)"current" else "project"),link.base.projectId)
        return if(response.code==409) {
            if(result.getString("outcome")!="conflict" || result.getInt("expectedVersion")!=link.base.version || current.version==link.base.version)throw CompanyApiException("INVALID_RESPONSE")
            CompanyClientLinkResult.Conflict(current)
        } else {
            if(result.getString("outcome")!="linked" || current.version!=link.base.version+1 || current.clientId!=link.clientId)throw CompanyApiException("INVALID_RESPONSE")
            CompanyClientLinkResult.Linked(current)
        }
    }

    fun revoke(session: CompanySession, device: CompanyDeviceSession) {
        send(session.connection.url, "POST", "${session.connection.authBasePath}/revoke-session", JSONObject().put("token", device.token), session.token)
    }

    fun signOut(session: CompanySession) = signOut(session.connection, session.token)
    fun signOut(connection: CompanyConnection, token: String) { send(connection.url, "POST", "${connection.authBasePath}/sign-out", JSONObject(), token) }

    fun enrollMfa(session: CompanySession, password: String): CompanyMfaSetup {
        val result = json(send(session.connection.url, "POST", "${session.connection.authBasePath}/two-factor/enable", JSONObject().put("password", password), session.token))
        return CompanyMfaSetup(result.getString("totpURI"), strings(result.optJSONArray("backupCodes")))
    }

    fun verifyEnrollment(session: CompanySession, code: String): String = sessionToken(send(session.connection.url, "POST", "${session.connection.authBasePath}/two-factor/verify-totp", JSONObject().put("code", code.trim()), session.token))

    private fun sessionToken(response: HttpResponse): String = response.header("set-auth-token")?.takeIf { it.isNotBlank() && it.length <= 8192 && !it.contains('\r') && !it.contains('\n') }
        ?: throw CompanyApiException("INVALID_RESPONSE")

    private fun json(response: HttpResponse): JSONObject = try { JSONObject(response.body) } catch (_: Exception) { throw CompanyApiException("INVALID_RESPONSE") }

    private fun send(base: String, method: String, path: String, body: JSONObject? = null, token: String? = null, cookies: String? = null, allowVersionConflict: Boolean = false): HttpResponse {
        val headers = mutableMapOf("Origin" to base, "Accept" to "application/json")
        if (body != null) headers["Content-Type"] = "application/json"
        if (token != null) headers["Authorization"] = "Bearer $token"
        if (cookies != null) headers["Cookie"] = cookies
        val response = http.request(method, "$base$path", headers, body?.toString())
        if (response.code !in 200..299) {
            val payload = runCatching { JSONObject(response.body) }.getOrNull()
            val code = payload?.optJSONObject("error")?.optString("code")?.takeIf { it.isNotBlank() } ?: payload?.optString("code")
            if (allowVersionConflict && response.code == 409 && code == "VERSION_CONFLICT") return response
            val safe = when {
                response.code == 429 -> "RATE_LIMITED"
                code?.matches(Regex("[A-Z_]{1,80}")) == true -> code
                response.code == 401 -> "UNAUTHENTICATED"
                response.code == 400 -> "INVALID_CREDENTIALS"
                else -> "REQUEST_FAILED"
            }
            throw CompanyApiException(safe, response.code)
        }
        return response
    }

    companion object {
        internal fun validateProjectEdit(edit: CompanyProjectEdit) {
            if (!edit.base.id.matches(Regex("[0-9a-fA-F-]{36}")) || !edit.operationId.matches(Regex("[0-9a-fA-F-]{36}")) ||
                edit.name.trim().isEmpty() || edit.name.trim().length > 240 || edit.code.trim().length > 80 ||
                edit.status !in setOf("active", "on_hold", "completed", "archived") || edit.base.version !in 1..2147483646)
                throw CompanyApiException("INVALID_PROJECT_EDIT")
        }
        fun strings(array: JSONArray?): List<String> = if (array == null) emptyList() else (0 until array.length()).map { array.getString(it) }
    }
}
