package com.holagundi.archiman.sync

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class HttpResponse(val code: Int, val headers: Map<String, List<String>>, val body: String) {
    fun header(name: String): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()
    fun headerAll(name: String): List<String> = headers.entries.filter { it.key.equals(name, ignoreCase = true) }.flatMap { it.value }
}

/** The one place the network is touched, so the tests can stand in for it. */
interface HttpTransport {
    fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse
}

class UrlConnectionTransport(private val timeoutMs: Int = 30_000) : HttpTransport {
    override fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse {
        val u = URL(url)
        require(u.protocol == "https") { "Only https addresses are used." }
        val c = u.openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = timeoutMs
            c.readTimeout = timeoutMs
            c.instanceFollowRedirects = false
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (body != null) {
                c.doOutput = true
                c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = c.responseCode
            val stream = if (code >= 400) c.errorStream else c.inputStream
            val text = stream?.use { it.readBytes().toString(Charsets.UTF_8) } ?: ""
            val hs = c.headerFields.filterKeys { it != null }.mapKeys { it.key!! }
            return HttpResponse(code, hs, text)
        } finally {
            c.disconnect()
        }
    }
}

class CloudException(message: String, val code: Int = 0) : Exception(message) {
    /** The server refused the credentials or the session (as opposed to the network failing). */
    val rejected get() = code == 401 || code == 403 || code == 400
}

/** A cloud sign-in. [cookies] is the long-lived secret: store it sealed, never log it. */
class CloudSession(val userId: String, val email: String, val cookies: String)

/** Neon Auth (Managed Better Auth) over HTTPS: email and password, then a short-lived access token. Public endpoints only; no database credential. */
class CloudAuth(baseUrl: String, private val http: HttpTransport, private val origin: String = "http://localhost:3000") {
    private val base = baseUrl.trimEnd('/')

    init {
        require(base.startsWith("https://")) { "The cloud address must be an https URL." }
    }

    fun signIn(email: String, password: String): CloudSession {
        val res = http.request(
            "POST", "$base/sign-in/email",
            mapOf("Origin" to origin, "Content-Type" to "application/json"),
            JSONObject().put("email", email).put("password", password).toString(),
        )
        if (res.code !in 200..299) {
            throw CloudException(if (res.code == 401 || res.code == 400) "The cloud account email or password is wrong." else "Cloud sign-in failed (${res.code}).", res.code)
        }
        val cookies = res.headerAll("Set-Cookie").map { it.substringBefore(';').trim() }.filter { it.contains('=') && !it.endsWith("=") }.joinToString("; ")
        if (cookies.isEmpty()) throw CloudException("The cloud service did not return a session.", res.code)
        val user = JSONObject(res.body).optJSONObject("user")
        return CloudSession(user?.optString("id").orEmpty(), user?.optString("email").orEmpty(), cookies)
    }

    /** A fresh access token (JWT). Throws a rejected [CloudException] when the session has ended. */
    fun accessToken(session: CloudSession): String {
        val res = http.request("GET", "$base/get-session", mapOf("Origin" to origin, "Cookie" to session.cookies), null)
        if (res.code !in 200..299) throw CloudException("Cloud session check failed (${res.code}).", res.code)
        res.header("set-auth-jwt")?.takeIf { it.isNotEmpty() }?.let { return it }
        if (res.body.isBlank() || res.body.trim() == "null") throw CloudException("The cloud session has ended; sign in again.", 401)
        throw CloudException("The cloud service returned no access token.")
    }

    fun signOut(session: CloudSession) {
        runCatching { http.request("POST", "$base/sign-out", mapOf("Origin" to origin, "Cookie" to session.cookies, "Content-Type" to "application/json"), "{}") }
    }
}

data class CloudCompany(val id: String, val name: String)

/** The cloud side of the company sync (the same API MynaHub uses). Faked in tests. */
interface HubApi {
    fun companies(): List<CloudCompany>

    /** One answer {sync_id, kind, status, rev, seq} per pushed record. */
    fun push(company: String, records: List<PushRecord>): JSONArray

    fun pull(company: String, fromSeq: Long, limit: Int): JSONArray
}

/** Neon Data API over HTTPS with a Neon Auth access token. Row level security in the database decides what this person may see and change. */
class HubDataApi(
    baseUrl: String,
    private val http: HttpTransport,
    private val retryDelayMs: Long = 1_500,
    private val token: () -> String,
) : HubApi {
    private val base = baseUrl.trimEnd('/')

    init {
        require(base.startsWith("https://")) { "The data address must be an https URL." }
    }

    override fun companies(): List<CloudCompany> {
        val rows = JSONArray(send("GET", "company?select=id,name&order=name.asc", null))
        return (0 until rows.length()).map { rows.getJSONObject(it).let { o -> CloudCompany(o.getString("id"), o.optString("name")) } }
    }

    override fun push(company: String, records: List<PushRecord>): JSONArray {
        val body = JSONObject().put("p_company", company).put("p_records", JSONArray(records.map { it.toCloudJson() }))
        return JSONArray(send("POST", "rpc/push_records", body.toString()))
    }

    override fun pull(company: String, fromSeq: Long, limit: Int): JSONArray {
        val c = URLEncoder.encode(company, "UTF-8")
        return JSONArray(send("GET", "record?company_id=eq.$c&seq=gt.$fromSeq&order=seq.asc&limit=$limit&select=kind,sync_id,rev,seq,deleted,body", null))
    }

    // Observed on dev: a token is sometimes accepted before the database can tell who it belongs to ("not signed in", or 401). A fresh token and a
    // short wait fixes it, so a request is tried up to three times for exactly that and nothing else.
    private fun send(method: String, path: String, body: String?): String {
        var attempt = 1
        while (true) {
            try {
                return sendOnce(method, path, body)
            } catch (e: CloudException) {
                val transient = e.code == 401 || e.message.orEmpty().contains("not signed in", ignoreCase = true)
                if (!transient || attempt >= 3) throw e
                Thread.sleep(retryDelayMs * attempt)
                attempt++
            }
        }
    }

    private fun sendOnce(method: String, path: String, body: String?): String {
        val headers = mutableMapOf("Authorization" to "Bearer ${token()}", "Accept-Profile" to "hub")
        if (body != null) {
            headers["Content-Profile"] = "hub"
            headers["Content-Type"] = "application/json"
        }
        val res = http.request(method, "$base/$path", headers, body)
        if (res.code !in 200..299) {
            val message = runCatching { JSONObject(res.body).optString("message") }.getOrNull().orEmpty()
            throw CloudException(message.ifEmpty { "The cloud service refused the request (${res.code})." }, res.code)
        }
        return res.body.ifBlank { "[]" }
    }
}
