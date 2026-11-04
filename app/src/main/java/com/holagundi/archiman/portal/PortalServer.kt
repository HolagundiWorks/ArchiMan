package com.holagundi.archiman.portal

import android.util.Log
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.PortalAuditEventEntity
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

data class PortalUiState(
    val running: Boolean = false,
    val address: String? = null,
    val code: String? = null,
    val detail: String? = null,
)

@Singleton
class PortalServer @Inject constructor(
    private val db: ArchiManDatabase,
) {
    private val _state = MutableStateFlow(PortalUiState())
    val state = _state.asStateFlow()
    var onStopped: (() -> Unit)? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val clients = Executors.newFixedThreadPool(8) { runnable ->
        Thread(runnable, "archiman-portal-client").apply { isDaemon = true }
    }
    private val sessions = ConcurrentHashMap<String, PortalSession>()
    private val failures = ConcurrentHashMap<String, Failure>()
    private val active = AtomicInteger()
    private val random = SecureRandom()
    private var server: ServerSocket? = null
    private var acceptThread: Thread? = null
    private var limit: Job? = null
    private var boundAddress: String? = null
    @Volatile private var accessCode: String = ""

    fun isRunning(): Boolean = _state.value.running

    fun fail(detail: String) {
        _state.value = PortalUiState(detail = detail)
    }

    @Synchronized
    fun start(address: String): Boolean {
        if (server?.isClosed == false) return true
        return try {
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(address, PortalHttp.PORT), PortalHttp.MAX_CLIENTS)
            server = socket
            boundAddress = address
            accessCode = PortalHttp.newCode()
            sessions.clear()
            failures.clear()
            _state.value = PortalUiState(
                running = true,
                address = "http://$address:${PortalHttp.PORT}",
                code = accessCode,
            )
            limit?.cancel()
            limit = scope.launch {
                delay(60 * 60 * 1000L)
                stop(notify = true)
            }
            acceptThread = Thread({
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        if (active.get() >= PortalHttp.MAX_CLIENTS) {
                            client.close()
                        } else {
                            clients.execute { handle(client) }
                        }
                    } catch (_: Exception) {
                        if (socket.isClosed) break
                    }
                }
            }, "archiman-portal").apply {
                isDaemon = true
                start()
            }
            true
        } catch (error: Exception) {
            _state.value = PortalUiState(detail = error.message ?: "Could not open the portal.")
            false
        }
    }

    @Synchronized
    fun stop(notify: Boolean) {
        limit?.cancel()
        val socket = server
        server = null
        boundAddress = null
        accessCode = ""
        sessions.clear()
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        _state.value = PortalUiState()
        if (notify) onStopped?.invoke()
    }

    private fun handle(client: Socket) {
        active.incrementAndGet()
        try {
            client.soTimeout = 4_000
            val remote = client.inetAddress?.hostAddress.orEmpty()
            val request = PortalHttp.read(client.getInputStream())
            if (request == null) {
                write(client, "400 Bad Request", PortalPages.message("Request rejected", "That request was too large or incomplete."))
                return
            }
            val address = boundAddress
            if (address == null || !PortalHttp.hostAllowed(request.headers["host"], address) || !PortalHttp.originAllowed(request.headers["origin"], address)) {
                write(client, "403 Forbidden", PortalPages.message("Wrong host", "Open the address shown on the phone."))
                return
            }
            if (request.method == "POST" && request.headers["content-type"]?.substringBefore(';')?.trim()?.lowercase() != "application/x-www-form-urlencoded") {
                write(client, "400 Bad Request", PortalPages.message("Request rejected", "Use the form on this page."))
                return
            }
            route(client, request, remote, address)
        } catch (error: Exception) {
            Log.e("ArchiManPortal", "portal request failed", error)
            try {
                write(client, "400 Bad Request", PortalPages.message("Request rejected", "The portal could not read that request."))
            } catch (_: Exception) {
            }
        } finally {
            active.decrementAndGet()
            try {
                client.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun route(client: Socket, request: PortalRequest, remote: String, address: String) {
        val session = session(request)
        when {
            request.method == "POST" && request.path == "/logout" -> {
                if (session != null && csrfMatches(request, session)) sessions.remove(cookie(request))
                write(client, "303 See Other", PortalPages.message("Signed out", "The portal session is closed."), listOf("Location: http://$address:${PortalHttp.PORT}/", clearCookie()))
            }
            request.method == "POST" && request.path == "/" -> signIn(client, request, remote, address)
            request.method == "GET" && request.path == "/" && session != null -> {
                val page = runBlocking(Dispatchers.IO) {
                    PortalPages.home(db.practiceDao().company(), db.projectDao().projects(), session.role, session.csrf)
                }
                write(client, "200 OK", page)
            }
            request.method == "GET" && request.path == "/project" && session != null -> openProject(client, request, session)
            request.method == "GET" && request.path == "/favicon.ico" -> write(client, "204 No Content", "")
            request.method == "GET" && (request.path == "/" || request.path == "/project") -> {
                val csrf = newToken()
                val page = runBlocking(Dispatchers.IO) {
                    val company = db.practiceDao().company()
                    PortalPages.login(company?.practiceName.orEmpty(), db.practiceDao().activeUsers(), csrf, null)
                }
                write(client, "200 OK", page, listOf(csrfCookie(csrf)))
            }
            else -> write(client, "404 Not Found", PortalPages.message("Not found", "That page is not on the portal."))
        }
    }

    private fun signIn(client: Socket, request: PortalRequest, remote: String, address: String) {
        val failure = failures[remote]
        val now = System.currentTimeMillis()
        if (failure != null && failure.count >= 8 && now - failure.at < 60_000) {
            write(client, "429 Too Many Requests", PortalPages.message("Try again shortly", "Too many access codes were rejected."))
            return
        }
        val fields = PortalHttp.form(request.body)
        val csrf = PortalHttp.cookie(request.headers["cookie"], "archiman_csrf").orEmpty()
        val codeOk = PortalHttp.codesMatch(accessCode, fields["code"].orEmpty()) && csrf.isNotBlank() && csrf == fields["csrf"]
        val userId = fields["user"]?.toLongOrNull()
        val user = if (codeOk && userId != null) {
            runBlocking(Dispatchers.IO) { db.practiceDao().user(userId) }?.takeIf { it.isActive }
        } else {
            null
        }
        if (user == null) {
            failures[remote] = Failure((failure?.count ?: 0) + 1, now)
            val fresh = newToken()
            val page = runBlocking(Dispatchers.IO) {
                val company = db.practiceDao().company()
                PortalPages.login(company?.practiceName.orEmpty(), db.practiceDao().activeUsers(), fresh, "That code does not match the phone.")
            }
            write(client, "401 Unauthorized", page, listOf(csrfCookie(fresh)))
            return
        }
        failures.remove(remote)
        val token = newToken()
        val sessionCsrf = newToken()
        sessions[token] = PortalSession(user.id, user.username, user.role, sessionCsrf, now, now)
        runBlocking(Dispatchers.IO) {
            db.practiceDao().touchLogin(user.id, now)
            db.practiceDao().insertAudit(
                PortalAuditEventEntity(
                    userId = user.id,
                    username = user.username,
                    action = "PORTAL_SIGN_IN",
                    entityType = "portal",
                    summary = "Wi-Fi portal sign-in",
                    sourceAddress = remote,
                    occurredAt = now,
                ),
            )
        }
        write(
            client,
            "303 See Other",
            PortalPages.message("Signed in", "Opening the practice."),
            listOf(
                "Location: http://$address:${PortalHttp.PORT}/",
                "archiman_portal=$token; HttpOnly; SameSite=Strict; Path=/; Max-Age=${PortalHttp.SESSION_SECONDS}",
                clearCsrf(),
            ),
        )
    }

    private fun openProject(client: Socket, request: PortalRequest, session: PortalSession) {
        val id = request.query["id"]?.toLongOrNull()
        if (id == null) {
            write(client, "404 Not Found", PortalPages.message("Not found", "That project is not on this phone."))
            return
        }
        val page = runBlocking(Dispatchers.IO) {
            val project = db.projectDao().getProject(id) ?: return@runBlocking null
            val brief = db.suiteDao().brief(id)
            PortalPages.project(
                project,
                brief?.clientObjectives.orEmpty(),
                db.suiteDao().logs(id),
                db.suiteDao().issues(id),
                db.suiteDao().coordinationItems(id),
                db.suiteDao().drawings(id),
                session.csrf,
            )
        }
        if (page == null) {
            write(client, "404 Not Found", PortalPages.message("Not found", "That project is not on this phone."))
        } else {
            write(client, "200 OK", page)
        }
    }

    private fun session(request: PortalRequest): PortalSession? {
        val token = cookie(request) ?: return null
        val current = sessions[token] ?: return null
        val now = System.currentTimeMillis()
        if (now - current.seenAt > PortalHttp.SESSION_SECONDS * 1000L || now - current.createdAt > 60 * 60 * 1000L) {
            sessions.remove(token)
            return null
        }
        current.seenAt = now
        return current
    }

    private fun cookie(request: PortalRequest) = PortalHttp.cookie(request.headers["cookie"], "archiman_portal")

    private fun csrfMatches(request: PortalRequest, session: PortalSession): Boolean {
        return PortalHttp.form(request.body)["csrf"] == session.csrf
    }

    private fun newToken(): String {
        val bytes = ByteArray(18)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun csrfCookie(token: String) = "archiman_csrf=$token; HttpOnly; SameSite=Strict; Path=/; Max-Age=600"

    private fun clearCookie() = "archiman_portal=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0"

    private fun clearCsrf() = "archiman_csrf=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0"

    private fun write(socket: Socket, status: String, html: String, cookies: List<String> = emptyList()) {
        val bytes = html.toByteArray(StandardCharsets.UTF_8)
        val head = buildString {
            append("HTTP/1.1 ").append(status).append("\r\n")
            append("Content-Type: text/html; charset=utf-8\r\n")
            append("Content-Length: ").append(bytes.size).append("\r\n")
            append("Cache-Control: no-store\r\n")
            append("X-Content-Type-Options: nosniff\r\n")
            append("X-Frame-Options: DENY\r\n")
            append("Referrer-Policy: no-referrer\r\n")
            append("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'\r\n")
            cookies.forEach { value ->
                if (value.startsWith("Location:")) append(value).append("\r\n") else append("Set-Cookie: ").append(value).append("\r\n")
            }
            append("Connection: close\r\n\r\n")
        }
        val out = socket.getOutputStream()
        out.write(head.toByteArray(StandardCharsets.UTF_8))
        out.write(bytes)
        out.flush()
    }

    private data class PortalSession(
        val userId: Long,
        val username: String,
        val role: String,
        val csrf: String,
        val createdAt: Long,
        var seenAt: Long,
    )

    private data class Failure(val count: Int, val at: Long)
}
