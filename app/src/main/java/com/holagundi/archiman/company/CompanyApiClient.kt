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
    "LOCAL_DATA_CLEAR_FAILED" -> "Company access was refused, but this phone could not clear its saved data. Check device storage and sign out."
    else -> "The company request could not be completed. Try again or contact your administrator."
}) {
    val endsSession get() = status == 401 || errorCode in setOf("MEMBERSHIP_SUSPENDED", "MEMBERSHIP_INACTIVE", "NOT_A_MEMBER")
}

data class CompanyConnection(val url: String, val authBasePath: String, val apiVersion: Int = 1)
data class CompanyIdentity(val userId: String, val email: String, val name: String, val company: String, val roles: List<String>, val permissions: List<String>, val mfaEnabled: Boolean, val organizationId: String = "")
data class CompanyProject(val id: String, val name: String, val code: String, val status: String, val version: Int)
data class CompanyProjectPage(val projects: List<CompanyProject>, val nextCursor: String?)
data class CompanyTask(val id: String, val projectId: String, val title: String, val description: String, val dueAt: String?, val status: String, val priority: String, val version: Int)
data class CompanyTaskPage(val projectId: String, val tasks: List<CompanyTask>, val nextCursor: String?)
data class CompanyLiveProjects(val revision: String, val page: CompanyProjectPage, val roles: List<String>, val permissions: List<String>, val taskPage: CompanyTaskPage? = null)
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
            !strings(info.optJSONArray("capabilities")).containsAll(listOf("identity", "sessions", "two-factor", "invitations", "projects.live"))) throw CompanyApiException("INCOMPATIBLE_API")
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
    fun liveProjects(session: CompanySession, after: String?, since: String?, taskProjectId: String? = null, taskAfter: String? = null): CompanyLiveProjects {
        if (after != null && !after.matches(Regex("[0-9a-fA-F-]{36}"))) throw CompanyApiException("INVALID_RESPONSE")
        if (since != null && !since.matches(Regex("[a-f0-9]{64}"))) throw CompanyApiException("INVALID_RESPONSE")
        if (listOfNotNull(taskProjectId, taskAfter).any { !it.matches(Regex("[0-9a-fA-F-]{36}")) } || (taskAfter != null && taskProjectId == null)) throw CompanyApiException("INVALID_RESPONSE")
        val query = listOfNotNull(after?.let { "after=$it" }, since?.let { "since=$it" }, taskProjectId?.let { "taskProjectId=$it" }, taskAfter?.let { "taskAfter=$it" }).joinToString("&")
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
            val tasks = (0 until taskRows.length()).map { taskRows.getJSONObject(it).let { row ->
                val task = CompanyTask(row.getString("id"), row.getString("projectId"), row.getString("title"), row.getString("description"),
                    if (row.isNull("dueAt")) null else row.getString("dueAt"), row.getString("status"), row.getString("priority"), row.getInt("version"))
                if (!task.id.matches(Regex("[0-9a-fA-F-]{36}")) || task.projectId != projectId || task.title.isBlank() || task.title.length > 240 ||
                    task.description.length > 10000 || task.version < 1 || task.status !in setOf("open","in_progress","completed","cancelled") ||
                    task.priority !in setOf("low","normal","high","urgent") || (task.dueAt != null && runCatching { java.time.Instant.parse(task.dueAt) }.isFailure))
                    throw CompanyApiException("INVALID_RESPONSE")
                task
            } }
            if (tasks.map { it.id }.distinct().size != tasks.size) throw CompanyApiException("INVALID_RESPONSE")
            val cursor = if (page.isNull("nextCursor")) null else page.getString("nextCursor")
            if (cursor != null && (!cursor.matches(Regex("[0-9a-fA-F-]{36}")) || cursor == taskAfter || tasks.lastOrNull()?.id != cursor))
                throw CompanyApiException("INVALID_RESPONSE")
            CompanyTaskPage(projectId, tasks, cursor)
        }
        return CompanyLiveProjects(revision, CompanyProjectPage(projects, next), strings(result.getJSONArray("roles")), strings(result.getJSONArray("permissions")), taskPage)
    }

    fun sessions(session: CompanySession): List<CompanyDeviceSession> {
        val rows = JSONArray(send(session.connection.url, "GET", "${session.connection.authBasePath}/list-sessions", token = session.token).body)
        return (0 until rows.length()).map { rows.getJSONObject(it).let { row -> CompanyDeviceSession(row.getString("token"), row.optString("createdAt"), row.optString("expiresAt"), row.getString("token") == session.token.substringBefore('.')) } }
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
