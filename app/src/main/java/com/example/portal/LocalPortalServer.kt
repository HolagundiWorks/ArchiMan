package com.example.portal

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigInteger
import java.net.*
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509KeyManager
import javax.security.auth.x500.X500Principal

data class PortalProject(val id: Long, val name: String, val code: String, val type: String, val status: String, val client: String, val location: String, val architect: String, val measurementCount: Int)
data class PortalOption(val id: Long, val label: String, val meta: String = "")
data class PortalUserOption(val id: Long, val username: String, val displayName: String, val role: String, val isActive: Boolean)
data class PortalSnapshot(
    val companyName: String,
    val projects: List<PortalProject>,
    val selectedProject: String?,
    val selectedProjectId: Long?,
    val tasks: List<String>,
    val schedules: List<String>,
    val inspections: List<String>,
    val drawings: List<String>,
    val dailyReports: List<String> = emptyList(),
    val siteIssues: List<String> = emptyList(),
    val decisions: List<String> = emptyList(),
    val clients: List<PortalOption> = emptyList(),
    val contractors: List<PortalOption> = emptyList(),
    val workItems: List<PortalOption> = emptyList(),
    val floors: List<PortalOption> = emptyList(),
    val selections: List<String> = emptyList(),
    val meetings: List<String> = emptyList(),
    val responsibilities: List<String> = emptyList(),
    val coordination: List<String> = emptyList(),
    val measurements: List<String> = emptyList(),
    val measurementSheets: List<PortalOption> = emptyList(),
    val coordinationRecords: List<PortalOption> = emptyList(),
    val siteIssueRecords: List<PortalOption> = emptyList(),
    val portalUsers: List<PortalUserOption> = emptyList(),
    val auditEvents: List<String> = emptyList(),
    val companyLegalName: String = "",
    val companyType: String = "",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = ""
)
data class PortalMutation(val action: String, val projectId: Long, val title: String, val description: String, val fields: Map<String, String>, val sourceAddress: String)
data class PortalMutationResult(val success: Boolean, val message: String)
data class LocalPortalState(
    val isRunning: Boolean = false,
    val url: String = "",
    val certificateFingerprint: String = "",
    val expiresAt: Long? = null,
    val error: String? = null,
    val isStarting: Boolean = false
)
private data class PortalSession(val principal: PortalPrincipal, val csrfToken: String, val absoluteExpiresAt: Long, val lastSeenAt: Long)
private data class FailedLogin(var count: Int, var blockedUntil: Long)
private data class CachedSnapshot(val value: PortalSnapshot, val expiresAt: Long)

class LocalPortalServer(private val context: Context, private val bindAddressOverride: InetAddress? = null, private val port: Int = PORT, private val secureTransport: Boolean = true) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = Collections.synchronizedMap(mutableMapOf<String, PortalSession>())
    private val failedLogins = Collections.synchronizedMap(mutableMapOf<String, FailedLogin>())
    private val snapshotCache = Collections.synchronizedMap(mutableMapOf<String, CachedSnapshot>())
    private val activeClients = AtomicInteger(0)
    private val random = SecureRandom()
    private val _state = MutableStateFlow(LocalPortalState())
    val state: StateFlow<LocalPortalState> = _state.asStateFlow()
    private var serverSocket: java.net.ServerSocket? = null
    private var expiryJob: Job? = null
    private var snapshotProvider: (suspend (Long?) -> PortalSnapshot)? = null
    private var authenticator: (suspend (String, CharArray) -> PortalPrincipal?)? = null
    private var mutationHandler: (suspend (PortalPrincipal, PortalMutation) -> PortalMutationResult)? = null

    fun start(provider: suspend (Long?) -> PortalSnapshot, authenticate: suspend (String, CharArray) -> PortalPrincipal?, mutate: suspend (PortalPrincipal, PortalMutation) -> PortalMutationResult) {
        if (_state.value.isRunning || _state.value.isStarting) return
        snapshotProvider = provider; authenticator = authenticate; mutationHandler = mutate
        _state.value = LocalPortalState(isStarting = true)
        scope.launch {
            val address = bindAddressOverride ?: awaitWifiAddress()
            if (address == null) {
                _state.value = LocalPortalState(error = "Wi-Fi is not ready or has no IPv4 address. Reconnect to Wi-Fi, wait a few seconds, then try again.")
                return@launch
            }
            try {
                val (socket, fingerprint) = if (secureTransport) secureSocket(address) else Pair(java.net.ServerSocket(port, 24, address), "TEST")
                serverSocket = socket
                val expiresAt = System.currentTimeMillis() + PORTAL_DURATION_MS
                _state.value = LocalPortalState(true, "${if (secureTransport) "https" else "http"}://${address.hostAddress}:${socket.localPort}", fingerprint, expiresAt)
                expiryJob = launch { delay(PORTAL_DURATION_MS); stop() }
                while (!socket.isClosed) {
                    val client = socket.accept()
                    if (activeClients.incrementAndGet() > MAX_CONCURRENT_CLIENTS) {
                        activeClients.decrementAndGet()
                        runCatching { client.close() }
                    } else {
                        launch {
                            try { handle(client) } finally { activeClients.decrementAndGet() }
                        }
                    }
                }
            } catch (_: SocketException) {
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Could not start local workspace", error)
                _state.value = LocalPortalState(error = "Could not start the secure workspace. ${error.message ?: error.javaClass.simpleName}")
            } finally {
                runCatching { serverSocket?.close() }; serverSocket = null; sessions.clear()
                if (_state.value.isRunning) _state.value = LocalPortalState()
            }
        }
    }

    fun stop() { runCatching { serverSocket?.close() }; serverSocket = null; expiryJob?.cancel(); expiryJob = null; sessions.clear(); failedLogins.clear(); snapshotCache.clear(); snapshotProvider = null; authenticator = null; mutationHandler = null; _state.value = LocalPortalState() }
    fun close() { stop(); scope.cancel() }

    private suspend fun awaitWifiAddress(): InetAddress? {
        repeat(WIFI_ADDRESS_ATTEMPTS) {
            wifiAddress()?.let { return it }
            delay(WIFI_ADDRESS_RETRY_MS)
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun wifiAddress(): InetAddress? {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val networks = buildList {
            manager.activeNetwork?.let(::add)
            manager.allNetworks.forEach { if (it !in this) add(it) }
        }
        networks.forEach { network ->
            val capabilities = manager.getNetworkCapabilities(network) ?: return@forEach
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return@forEach
            manager.getLinkProperties(network)?.linkAddresses?.asSequence()?.map { it.address }
                ?.firstOrNull(::usableIpv4Address)?.let { return it }
        }

        // Some Android builds briefly omit Wi-Fi from ConnectivityManager while
        // wlan0 already has its DHCP address. Restrict this fallback to known
        // Wi-Fi names so USB, VPN and mobile addresses are never displayed.
        val interfaces = runCatching { Collections.list(NetworkInterface.getNetworkInterfaces()) }.getOrDefault(emptyList())
        return interfaces.asSequence()
            .filter { network -> runCatching { network.isUp && !network.isLoopback }.getOrDefault(false) }
            .filter { network -> network.name.lowercase(Locale.ROOT).let { it.startsWith("wlan") || it.startsWith("wifi") || it.startsWith("swlan") } }
            .flatMap { network -> Collections.list(network.inetAddresses).asSequence() }
            .firstOrNull(::usableIpv4Address)
    }

    private fun usableIpv4Address(address: InetAddress): Boolean =
        address is Inet4Address && !address.isAnyLocalAddress && !address.isLoopbackAddress && !address.isLinkLocalAddress

    private suspend fun handle(socket: Socket) = socket.use { client ->
        client.soTimeout = 8_000
        client.tcpNoDelay = true
        val reader = try {
            BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
        } catch (error: Exception) {
            Log.w(LOG_TAG, "TLS handshake failed for ${client.inetAddress.hostAddress}", error)
            return
        }
        purgeExpiredSecurityState()
        val requestLine = reader.readLine() ?: return
        if (requestLine.length > MAX_REQUEST_LINE_LENGTH) { respond(client, 414, "text/plain", "Request target is too long."); return }
        val parts = requestLine.split(' '); if (parts.size < 2) return
        val method = parts[0]; val target = parts[1]
        var contentLength = 0; var invalidLength = false; var lineCount = 0
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break; if (line.isBlank()) break
            if (++lineCount > MAX_HEADER_COUNT || line.length > MAX_HEADER_LINE_LENGTH) { respond(client, 431, "text/plain", "Request headers are too large."); return }
            val name = line.substringBefore(':', "").trim().lowercase(Locale.ROOT)
            if (name.isNotBlank()) headers[name] = line.substringAfter(':', "").trim()
            if (name == "content-length") {
                val declared = headers[name]?.toIntOrNull()
                invalidLength = declared == null || declared !in 0..MAX_BODY_BYTES
                contentLength = declared?.takeIf { it in 0..MAX_BODY_BYTES } ?: 0
            }
        }
        if (invalidLength) { respond(client, 413, "text/plain", "Request body is too large."); return }
        Log.d(LOG_TAG, "DIAG method=$method path=$target host='${headers["host"]}' origin='${headers["origin"]}' boundAddress='${serverSocket?.inetAddress?.hostAddress}' boundPort='${serverSocket?.localPort}'")
        if (!allowedHost(headers["host"])) { respond(client, 400, "text/plain", "Invalid host."); return }
        if (!allowedOrigin(headers["origin"], headers["host"])) { respond(client, 403, "text/plain", "Cross-origin requests are not allowed."); return }
        if (method == "POST" && !headers["content-type"].orEmpty().substringBefore(';').trim().equals("application/x-www-form-urlencoded", true)) { respond(client, 415, "text/plain", "Unsupported content type."); return }
        val body = if (contentLength > 0) CharArray(contentLength).let { chars -> var read = 0; while (read < chars.size) { val n = reader.read(chars, read, chars.size - read); if (n < 0) break; read += n }; chars.concatToString(0, read) } else ""
        val form = runCatching { parseForm(body) }.getOrElse { respond(client, 400, "text/plain", "Malformed form data."); return }
        val token = headers["cookie"].orEmpty().split(';').map(String::trim).firstNotNullOfOrNull { entry -> if (entry.startsWith("$cookieName=")) entry.substringAfter('=') else null }
        val session = token?.let(::activeSession)
        val path = target.substringBefore('?')
        val query = parseForm(target.substringAfter('?', ""))
        when {
            method == "GET" && path == "/health" -> respond(client, 200, "text/plain; charset=utf-8", "ArchiMan local portal")
            method == "POST" && path == "/login" -> login(client, form, client.inetAddress.hostAddress.orEmpty())
            method == "POST" && path == "/logout" && session != null -> {
                if (!secureTokenMatches(form["csrf"], session.csrfToken)) respond(client, 403, "text/plain", "Invalid request token.")
                else { token?.let(sessions::remove); respond(client, 303, "text/plain", "Signed out", listOf("Location: /", clearCookieHeader(), "Clear-Site-Data: \"cache\", \"cookies\", \"storage\"")) }
            }
            session == null -> respond(client, 401, "text/html; charset=utf-8", loginPage(), if (token == null) emptyList() else listOf(clearCookieHeader()))
            method == "POST" && path == "/mutate" -> mutate(client, session, form, client.inetAddress.hostAddress.orEmpty())
            method != "GET" -> respond(client, 405, "text/plain", "Method not allowed")
            path == "/" -> respond(client, 200, "text/html; charset=utf-8", portalPage(cachedSnapshot(query["projectId"]?.toLongOrNull()), session, query["view"], query["q"]))
            else -> respond(client, 404, "text/plain", "Not found")
        }
    }

    private suspend fun login(client: Socket, form: Map<String, String>, address: String) {
        val failure = failedLogins[address]
        if (failure != null && failure.blockedUntil > System.currentTimeMillis()) { respond(client, 429, "text/html; charset=utf-8", loginPage("Too many attempts. Try again later.")); return }
        val suppliedPassword = form["password"].orEmpty().toCharArray()
        val principal = try { runCatching { authenticator?.invoke(form["username"].orEmpty(), suppliedPassword) }.getOrNull() } finally { suppliedPassword.fill('\u0000') }
        if (principal == null) {
            val next = (failure?.count ?: 0) + 1; failedLogins[address] = FailedLogin(next, if (next >= 5) System.currentTimeMillis() + LOGIN_BLOCK_MS else 0)
            respond(client, 401, "text/html; charset=utf-8", loginPage("Incorrect username or password")); return
        }
        failedLogins.remove(address)
        val token = randomToken(32); val csrf = randomToken(24)
        val now = System.currentTimeMillis()
        synchronized(sessions) {
            if (sessions.size >= MAX_SESSIONS) sessions.entries.minByOrNull { it.value.lastSeenAt }?.key?.let(sessions::remove)
            sessions[token] = PortalSession(principal, csrf, now + SESSION_DURATION_MS, now)
        }
        respond(client, 303, "text/plain", "Open ArchiMan", listOf("Location: /", setCookieHeader(token)))
    }

    private suspend fun mutate(client: Socket, session: PortalSession, form: Map<String, String>, address: String) {
        if (session.principal.role !in setOf("ADMIN", "EDITOR")) { respond(client, 403, "text/plain", "This account cannot edit records."); return }
        if (!secureTokenMatches(form["csrf"], session.csrfToken)) { respond(client, 403, "text/plain", "Invalid request token."); return }
        val action = form["action"].orEmpty(); val projectId = form["projectId"]?.toLongOrNull() ?: 0L; val title = form["title"].orEmpty().trim(); val description = form["description"].orEmpty().trim()
        val fields = form.filterKeys { it !in setOf("csrf", "action", "projectId", "title", "description") }.mapValues { it.value.trim() }
        if (action in ADMIN_ACTIONS && session.principal.role != "ADMIN") { respond(client, 403, "text/plain", "Administrator access is required."); return }
        val projectOptional = action in setOf("UPDATE_COMPANY", "ADD_PROJECT", "ADD_CLIENT", "ADD_CONTRACTOR", "QUALIFY_CONTRACTOR", "CREATE_PORTAL_USER", "UPDATE_PORTAL_USER", "SET_PROJECT_STATUS", "ARCHIVE_WORK_ITEM")
        if ((!projectOptional && projectId <= 0) || title.isBlank() || title.length > 160 || description.length > 4_000 || fields.size > 32 || fields.any { (key, value) -> key.length > 50 || value.length > 4_000 } || action !in EDIT_ACTIONS) { respond(client, 400, "text/plain", "Invalid or incomplete edit."); return }
        val result = mutationHandler?.invoke(session.principal, PortalMutation(action, projectId, title, description, fields, address)) ?: PortalMutationResult(false, "Editing is unavailable.")
        if (result.success) snapshotCache.clear()
        respond(client, if (result.success) 303 else 400, "text/plain", result.message, if (result.success) listOf("Location: /${if (projectId > 0) "?projectId=$projectId" else ""}") else emptyList())
    }

    private suspend fun cachedSnapshot(projectId: Long?): PortalSnapshot? {
        val key = projectId?.toString() ?: "all"
        val now = System.currentTimeMillis()
        snapshotCache[key]?.takeIf { it.expiresAt > now }?.let { return it.value }
        val snapshot = snapshotProvider?.invoke(projectId) ?: return null
        snapshotCache[key] = CachedSnapshot(snapshot, now + SNAPSHOT_CACHE_MS)
        return snapshot
    }

    private fun activeSession(token: String): PortalSession? = synchronized(sessions) {
        val now = System.currentTimeMillis()
        val session = sessions[token] ?: return@synchronized null
        if (session.absoluteExpiresAt <= now || now - session.lastSeenAt > SESSION_IDLE_TIMEOUT_MS) {
            sessions.remove(token)
            null
        } else session.copy(lastSeenAt = now).also { sessions[token] = it }
    }

    private fun purgeExpiredSecurityState() {
        val now = System.currentTimeMillis()
        synchronized(sessions) { sessions.entries.removeAll { it.value.absoluteExpiresAt <= now || now - it.value.lastSeenAt > SESSION_IDLE_TIMEOUT_MS } }
        synchronized(failedLogins) {
            failedLogins.entries.removeAll { it.value.blockedUntil in 1..now }
            while (failedLogins.size > MAX_FAILED_LOGIN_SOURCES) failedLogins.entries.firstOrNull()?.let { failedLogins.remove(it.key) }
        }
        synchronized(snapshotCache) { snapshotCache.entries.removeAll { it.value.expiresAt <= now } }
    }

    private fun allowedHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        val address = serverSocket?.inetAddress?.hostAddress ?: return false
        val localPort = serverSocket?.localPort ?: return false
        return host.equals(address, true) || host.equals("$address:$localPort", true)
    }

    private fun allowedOrigin(origin: String?, host: String?): Boolean {
        if (origin.isNullOrBlank()) return true
        val scheme = if (secureTransport) "https" else "http"
        return origin.equals("$scheme://$host", true)
    }

    private fun parseForm(body: String): Map<String, String> = body.split('&').mapNotNull { part -> val key = part.substringBefore('=', ""); if (key.isBlank()) null else URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(part.substringAfter('=', ""), "UTF-8") }.toMap()

    private fun respond(socket: Socket, status: Int, type: String, body: String, extraHeaders: List<String> = emptyList()) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val reason = mapOf(200 to "OK", 303 to "See Other", 400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden", 404 to "Not Found", 405 to "Method Not Allowed", 413 to "Content Too Large", 414 to "URI Too Long", 415 to "Unsupported Media Type", 429 to "Too Many Requests", 431 to "Request Header Fields Too Large", 503 to "Service Unavailable")[status] ?: "Error"
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\n")
            append("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'\r\n")
            append("Permissions-Policy: camera=(), microphone=(), geolocation=(), usb=()\r\nCross-Origin-Opener-Policy: same-origin\r\nCross-Origin-Resource-Policy: same-origin\r\nX-Permitted-Cross-Domain-Policies: none\r\n")
            extraHeaders.forEach { append(it).append("\r\n") }; append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        socket.getOutputStream().apply { write(headers); write(bytes); flush() }
    }

    private fun loginPage(error: String = "") = carbonPage(
        "ArchiMan Login",
        """<main class="login-layout"><section class="login-brand"><div><p class="eyebrow">ARCHITECTURAL CONSULTANCY MANAGEMENT</p><h1>ArchiMan</h1><p>Secure, phone-hosted access to projects, coordination and measurement books.</p></div><p class="login-note">The phone remains the only database. Nothing is uploaded to the internet.</p></section><section class="login-panel"><div class="login-form"><p class="eyebrow">LOCAL WORKSPACE</p><h2>Sign in</h2><p class="supporting">Use a portal account created by the administrator on the phone.</p>${if (error.isBlank()) "" else "<div class='notification error' role=alert><strong>Sign-in failed</strong><span>${escape(error)}</span></div>"}<form method="post" action="/login"><label>Username<input name="username" maxlength="40" autocomplete="username" required autofocus></label><label>Password<input name="password" type="password" maxlength="128" autocomplete="current-password" required></label><button type=submit>Sign in</button></form><p class="helper">Use only on the same trusted Wi-Fi network as the ArchiMan phone.</p></div></section></main>"""
    )

    private fun carbonIcon(name: String): String {
        val paths = when (name) {
            "dashboard" -> listOf(
                "M24 21H26V26H24z",
                "M20 16H22V26H20z",
                "M11,26a5.0059,5.0059,0,0,1-5-5H8a3,3,0,1,0,3-3V16a5,5,0,0,1,0,10Z",
                "M28,2H4A2.002,2.002,0,0,0,2,4V28a2.0023,2.0023,0,0,0,2,2H28a2.0027,2.0027,0,0,0,2-2V4A2.0023,2.0023,0,0,0,28,2Zm0,9H14V4H28ZM12,4v7H4V4ZM4,28V13H28.0007l.0013,15Z"
            )
            "projects" -> listOf(
                "M28,2H16a2.002,2.002,0,0,0-2,2V14H4a2.002,2.002,0,0,0-2,2V30H30V4A2.0023,2.0023,0,0,0,28,2ZM9,28V21h4v7Zm19,0H15V20a1,1,0,0,0-1-1H8a1,1,0,0,0-1,1v8H4V16H16V4H28Z",
                "M18 8H20V10H18z", "M24 8H26V10H24z", "M18 14H20V16H18z", "M24 14H26V16H24z", "M18 20H20V22H18z", "M24 20H26V22H24z"
            )
            "planning" -> listOf("M26,4h-4V2h-2v2h-8V2h-2v2H6C4.9,4,4,4.9,4,6v20c0,1.1,0.9,2,2,2h20c1.1,0,2-0.9,2-2V6C28,4.9,27.1,4,26,4z M26,26H6V12h20 V26z M26,10H6V6h4v2h2V6h8v2h2V6h4V10z")
            "site" -> listOf(
                "M29.34,16.06a1.0007,1.0007,0,0,0-1.1084.3L24.46,20.8857l-5.4355-.9882-3.602-8.9512A3.014,3.014,0,0,0,12.6138,9h-4.06A3.0018,3.0018,0,0,0,7.01,9.4277L2,12.4336v6.4009l5,.9092V30H9V20.1074l3.5652.648L14,24.2V30h2V23.8l-1.0911-2.6182L22.99,22.6509,18.2319,28.36A1,1,0,0,0,19,30H29a1,1,0,0,0,1-1V17A1,1,0,0,0,29.34,16.06ZM4,17.1655V13.5664l3-1.8v5.9448Zm5,.9092V11h3.6138a1.0141,1.0141,0,0,1,.9453.6709l3.14,7.8037ZM28,28H21.1353L28,19.7617Z",
                "M12.5,8A3.5,3.5,0,1,1,16,4.5,3.5042,3.5042,0,0,1,12.5,8Zm0-5A1.5,1.5,0,1,0,14,4.5,1.5017,1.5017,0,0,0,12.5,3Z"
            )
            "coordination" -> listOf(
                "M11.41 26.59 7.83 23 28 23 28 21 7.83 21 11.41 17.41 10 16 4 22 10 28 11.41 26.59z",
                "M28 10 22 4 20.59 5.41 24.17 9 4 9 4 11 24.17 11 20.59 14.59 22 16 28 10z"
            )
            "measurements" -> listOf("M29,10H3a1,1,0,0,0-1,1V21a1,1,0,0,0,1,1H29a1,1,0,0,0,1-1V11A1,1,0,0,0,29,10ZM28,20H4V12H8v4h2V12h5v4h2V12h5v4h2V12h4Z")
            "directory" -> listOf(
                "M19 10H26V12H19z", "M19 15H26V17H19z", "M19 20H26V22H19z", "M6 10H13V12H6z", "M6 15H13V17H6z", "M6 20H13V22H6z",
                "M28,5H4A2.002,2.002,0,0,0,2,7V25a2.002,2.002,0,0,0,2,2H28a2.002,2.002,0,0,0,2-2V7A2.002,2.002,0,0,0,28,5ZM4,7H15V25H4ZM17,25V7H28V25Z"
            )
            "admin" -> listOf(
                "M27,16.76c0-.25,0-.5,0-.76s0-.51,0-.77l1.92-1.68A2,2,0,0,0,29.3,11L26.94,7a2,2,0,0,0-1.73-1,2,2,0,0,0-.64.1l-2.43.82a11.35,11.35,0,0,0-1.31-.75l-.51-2.52a2,2,0,0,0-2-1.61H13.64a2,2,0,0,0-2,1.61l-.51,2.52a11.48,11.48,0,0,0-1.32.75L7.43,6.06A2,2,0,0,0,6.79,6,2,2,0,0,0,5.06,7L2.7,11a2,2,0,0,0,.41,2.51L5,15.24c0,.25,0,.5,0,.76s0,.51,0,.77L3.11,18.45A2,2,0,0,0,2.7,21L5.06,25a2,2,0,0,0,1.73,1,2,2,0,0,0,.64-.1l2.43-.82a11.35,11.35,0,0,0,1.31.75l.51,2.52a2,2,0,0,0,2,1.61h4.72a2,2,0,0,0,2-1.61l.51-2.52a11.48,11.48,0,0,0,1.32-.75l2.42.82a2,2,0,0,0,.64.1,2,2,0,0,0,1.73-1L29.3,21a2,2,0,0,0-.41-2.51ZM25.21,24l-3.43-1.16a8.86,8.86,0,0,1-2.71,1.57L18.36,28H13.64l-.71-3.55a9.36,9.36,0,0,1-2.7-1.57L6.79,24,4.43,20l2.72-2.4a8.9,8.9,0,0,1,0-3.13L4.43,12,6.79,8l3.43,1.16a8.86,8.86,0,0,1,2.71-1.57L13.64,4h4.72l.71,3.55a9.36,9.36,0,0,1,2.7,1.57L25.21,8,27.57,12l-2.72,2.4a8.9,8.9,0,0,1,0,3.13L27.57,20Z",
                "M16,22a6,6,0,1,1,6-6A5.94,5.94,0,0,1,16,22Zm0-10a3.91,3.91,0,0,0-4,4,3.91,3.91,0,0,0,4,4,3.91,3.91,0,0,0,4-4A3.91,3.91,0,0,0,16,12Z"
            )
            else -> error("Unknown Carbon portal icon: $name")
        }
        return "<svg data-carbon-icon='${escape(name)}' focusable='false' viewBox='0 0 32 32' aria-hidden='true'>${paths.joinToString("") { "<path d='${escape(it)}'></path>" }}</svg>"
    }

    private fun portalPage(snapshot: PortalSnapshot?, session: PortalSession, requestedView: String?, requestedSearch: String?): String {
        if (snapshot == null) return carbonPage("ArchiMan", "<main class=standalone><div class='notification error' role=alert><strong>Project data is not ready</strong><span>Keep ArchiMan open on the phone and reload this page.</span></div></main>")
        val allowedViews = setOf("dashboard", "projects", "planning", "site", "coordination", "measurements", "directory", "admin")
        val view = requestedView?.takeIf(allowedViews::contains) ?: "dashboard"
        val projectSuffix = snapshot.selectedProjectId?.let { "&projectId=$it" }.orEmpty()
        fun href(target: String) = "/?view=$target$projectSuffix"
        fun nav(target: String, label: String) = "<a class='nav-item ${if (view == target) "active" else ""}' href='${href(target)}' ${if (view == target) "aria-current=page" else ""}><span>${carbonIcon(target)}</span>${escape(label)}</a>"
        fun empty(title: String, body: String) = "<div class=empty-state><strong>${escape(title)}</strong><p>${escape(body)}</p></div>"
        fun list(title: String, values: List<String>, emptyMessage: String = "No records in this project") = "<section class=module><div class=module-heading><h2>${escape(title)}</h2><span class=count>${values.size}</span></div>" + (if (values.isEmpty()) empty(title, emptyMessage) else "<ul class=record-list>${values.joinToString("") { "<li>${escape(it)}</li>" }}</ul>") + "</section>"
        fun options(title: String, values: List<PortalOption>, emptyMessage: String = "No records available") = list(title, values.map { it.label + if (it.meta.isBlank()) "" else " · ${it.meta}" }, emptyMessage)
        val search = requestedSearch.orEmpty().trim().take(80)
        val visibleProjects = if (search.isBlank()) snapshot.projects else snapshot.projects.filter { project -> listOf(project.name, project.code, project.client, project.location, project.type, project.status).any { it.contains(search, ignoreCase = true) } }
        val projectRows = visibleProjects.joinToString("") { p -> "<tr><td><a href='/?view=projects&projectId=${p.id}'>${escape(p.name)}</a><span class=secondary>${escape(p.code.ifBlank { "No project code" })}</span></td><td>${escape(p.type)}</td><td>${escape(p.client)}</td><td>${escape(p.location)}</td><td class=numeric>${p.measurementCount}</td><td><span class='status ${statusClass(p.status)}'>${escape(p.status.replace('_', ' '))}</span></td></tr>" }
        val searchForm = "<form class=table-search method=get action=/><input type=hidden name=view value='$view'>${snapshot.selectedProjectId?.let { "<input type=hidden name=projectId value='$it'>" }.orEmpty()}<label><span class=visually-hidden>Search projects</span><input type=search name=q maxlength=80 value='${escape(search)}' placeholder='Search projects'></label><button type=submit>Search</button></form>"
        val projectEmpty = if (snapshot.projects.isEmpty()) empty("No projects yet", "Add the first project to start planning and recording measurements.") else empty("No matching projects", "Clear or change the search term to see other projects.")
        val addProject = if (session.principal.role in setOf("ADMIN", "EDITOR")) "<a class='button secondary-button' href='/?view=projects$projectSuffix#actions'>Add project</a>" else ""
        val projectTable = "<section class='module data-module'><div class=table-toolbar><div><h2>Project register</h2><p>${visibleProjects.size} of ${snapshot.projects.size} projects</p></div><div class=toolbar-actions>$searchForm$addProject</div></div>" + if (visibleProjects.isEmpty()) projectEmpty else "<div class=table-wrap><table><thead><tr><th>Project</th><th>Type</th><th>Client</th><th>Location</th><th class=numeric>Measurements</th><th>Status</th></tr></thead><tbody>$projectRows</tbody></table></div>" + "</section>"
        val selectedContext = snapshot.selectedProject?.let { "<span class=context-label>Current project</span><strong>${escape(it)}</strong>" } ?: "<span class=context-label>Project context</span><strong>All projects</strong>"
        val viewerNotice = if (session.principal.role == "VIEWER") "<div class='notification info'><strong>Viewer access</strong><span>Records cannot be changed with this account.</span></div>" else ""
        val pageContent = when (view) {
            "dashboard" -> """<div class=page-title><p class=eyebrow>OVERVIEW</p><h1>Practice dashboard</h1><p>Live operational view from the phone · ${formatTime(System.currentTimeMillis())}</p></div><div class=kpi-grid><article class=kpi><span>Projects</span><strong>${snapshot.projects.size}</strong><small>Company register</small></article><article class=kpi><span>Measurement rows</span><strong>${snapshot.projects.sumOf { it.measurementCount }}</strong><small>Across all projects</small></article><article class=kpi><span>Open work</span><strong>${snapshot.tasks.size}</strong><small>Current project tasks</small></article><article class=kpi><span>Coordination</span><strong>${snapshot.coordination.size}</strong><small>Current project records</small></article></div>$projectTable<div class=module-grid>${list("Upcoming schedule", snapshot.schedules)}${list("Site attention", snapshot.inspections)}${list("Recent coordination", snapshot.coordination)}</div>"""
            "projects" -> """<div class=page-title><p class=eyebrow>PORTFOLIO</p><h1>Projects</h1><p>Project register, profile and scope.</p></div>$projectTable${if (snapshot.selectedProject == null) empty("Select a project", "Open a project from the register to see its working context.") else "<div class=section-title><h2>${escape(snapshot.selectedProject)}</h2><p>Project workspace summary</p></div><div class=module-grid>${list("Scope and deliverables", snapshot.tasks)}${options("Floors and locations", snapshot.floors)}${options("Assigned contractors", snapshot.contractors)}</div>"}${actions(snapshot, session, "projects")}"""
            "planning" -> """<div class=page-title><p class=eyebrow>PROJECT CONTROLS</p><h1>Planning and programme</h1><p>Tasks, decisions, selections, meetings and scheduled commitments.</p></div><div class=module-grid>${list("Tasks", snapshot.tasks)}${list("Programme", snapshot.schedules)}${list("Formal decisions", snapshot.decisions)}${list("Material selections", snapshot.selections)}${list("Meetings", snapshot.meetings)}</div>${actions(snapshot, session, "planning")}"""
            "site" -> """<div class=page-title><p class=eyebrow>FIELD OPERATIONS</p><h1>Site management</h1><p>Daily progress, snag/NCR closure, inspections and drawing references.</p></div><div class=module-grid>${list("Daily reports", snapshot.dailyReports)}${list("Snag and NCR register", snapshot.siteIssues)}${list("Site inspections", snapshot.inspections)}${list("Drawing register", snapshot.drawings)}${list("Site meetings", snapshot.meetings)}</div>${actions(snapshot, session, "site")}"""
            "coordination" -> """<div class=page-title><p class=eyebrow>PROJECT COMMUNICATION</p><h1>Coordination</h1><p>RFI, submittal, site-instruction and responsibility tracking.</p></div><div class=module-grid>${list("Coordination register", snapshot.coordination)}${list("Responsibility matrix", snapshot.responsibilities)}${list("Meeting decisions", snapshot.meetings)}</div>${actions(snapshot, session, "coordination")}"""
            "measurements" -> """<div class=page-title><p class=eyebrow>MEASUREMENT BOOK</p><h1>Measurements</h1><p>Quantity-only site measurements with controlled review.</p></div><div class=module-grid>${list("Measurement rows", snapshot.measurements)}${options("Measurement books", snapshot.measurementSheets)}${options("Work items", snapshot.workItems)}${options("Floors", snapshot.floors)}</div>${actions(snapshot, session, "measurements")}"""
            "directory" -> """<div class=page-title><p class=eyebrow>MASTER DATA</p><h1>Directory and work library</h1><p>Reusable clients, contractors and standard work items.</p></div><div class=module-grid>${options("Clients", snapshot.clients)}${options("Contractors", snapshot.contractors)}${options("Work library", snapshot.workItems)}</div>${actions(snapshot, session, "directory")}"""
            else -> """<div class=page-title><p class=eyebrow>GOVERNANCE</p><h1>Practice administration</h1><p>Company identity, access control, lifecycle controls and immutable web audit.</p></div>${if (session.principal.role == "ADMIN") "<div class=module-grid>${list("Portal users", snapshot.portalUsers.map { "${it.displayName} · @${it.username} · ${it.role} · ${if (it.isActive) "Active" else "Disabled"}" })}${list("Recent web audit", snapshot.auditEvents)}${list("Company profile", listOfNotNull(snapshot.companyLegalName.takeIf(String::isNotBlank), snapshot.companyType.takeIf(String::isNotBlank), snapshot.companyEmail.takeIf(String::isNotBlank), snapshot.companyPhone.takeIf(String::isNotBlank)))}</div>${actions(snapshot, session, "admin")}" else "<div class='notification error'><strong>Administrator access required</strong><span>Your ${escape(session.principal.role.lowercase())} account cannot manage practice settings.</span></div>"}"""
        }
        val adminNavigation = if (session.principal.role == "ADMIN") nav("admin", "Administration") else ""
        val shell = """<a class=skip-link href=#main-content>Skip to main content</a><header class=top-header><a class=brand href='/?view=dashboard$projectSuffix'><strong>ArchiMan</strong><span>${escape(snapshot.companyName)}</span></a><div class=project-context>$selectedContext</div><div class=user-menu><span><strong>${escape(session.principal.displayName)}</strong><small>${escape(session.principal.role)}</small></span><form method=post action=/logout><input type=hidden name=csrf value='${session.csrfToken}'><button class=ghost-button type=submit>Sign out</button></form></div></header><aside class=side-nav><nav aria-label="Primary navigation">${nav("dashboard", "Dashboard")}${nav("projects", "Projects")}<p class=nav-group>PROJECT DELIVERY</p>${nav("planning", "Planning")}${nav("site", "Site management")}${nav("coordination", "Coordination")}${nav("measurements", "Measurements")}<p class=nav-group>PRACTICE</p>${nav("directory", "Directory & library")}$adminNavigation</nav><div class=server-note><span class=live-dot></span><div><strong>Phone connected</strong><small>Local workspace</small></div></div></aside><main id=main-content class=app-content>$viewerNotice$pageContent</main>"""
        return carbonPage("${view.replaceFirstChar { it.uppercase() }} · ArchiMan", shell)
    }

    private fun statusClass(status: String) = when (status.uppercase(Locale.ROOT)) {
        "ACTIVE", "APPROVED", "DONE", "COMPLETED" -> "success"
        "ON_HOLD", "RETURNED", "ATTENTION" -> "warning"
        "ARCHIVED", "DISABLED" -> "neutral"
        else -> "info"
    }

    private fun actions(snapshot: PortalSnapshot, session: PortalSession, area: String): String {
        if (session.principal.role == "VIEWER") return ""
        val csrf = session.csrfToken
        fun input(label: String, name: String, type: String = "text", required: Boolean = false, value: String = "") = "<label>${escape(label)}<input name='$name' type='$type' maxlength=160 value='${escape(value)}' ${if (type == "number") "step='any'" else ""} ${if (required) "required" else ""}></label>"
        fun area(label: String, name: String = "description", value: String = "") = "<label>${escape(label)}<textarea name='$name' maxlength=4000>${escape(value)}</textarea></label>"
        fun select(label: String, name: String, options: List<PortalOption>, required: Boolean = false) = "<label>${escape(label)}<select name='$name' ${if (required) "required" else ""}>${if (required) "<option value=''>Select</option>" else "<option value='0'>None</option>"}${options.joinToString("") { "<option value='${it.id}'>${escape(it.label)}${if (it.meta.isBlank()) "" else " · ${escape(it.meta)}"}</option>" }}</select></label>"
        fun choices(label: String, name: String, values: List<String>) = "<label>${escape(label)}<select name='$name'>${values.joinToString("") { "<option value='${escape(it)}'>${escape(it.replace('_', ' '))}</option>" }}</select></label>"
        fun form(action: String, label: String, fields: String, projectId: Long = snapshot.selectedProjectId ?: 0L, titleLabel: String = "Title", descriptionLabel: String? = "Details") = "<details class=edit><summary><span>Add ${escape(label)}</span><span aria-hidden=true>+</span></summary><form method=post action=/mutate><input type=hidden name=csrf value='$csrf'><input type=hidden name=action value='$action'><input type=hidden name=projectId value='$projectId'>${input(titleLabel, "title", required = true)}${descriptionLabel?.let { area(it) }.orEmpty()}$fields<button type=submit>Add ${escape(label)}</button></form></details>"
        fun wrap(title: String, description: String, body: String) = if (body.isBlank()) "" else "<section id=actions class=actions><div class=section-title><h2>${escape(title)}</h2><p>${escape(description)}</p></div><div class=action-grid>$body</div></section>"
        val projectId = snapshot.selectedProjectId
        val selectProject = "<div class='notification info'><strong>Select a project first</strong><span>Open a project from the project register before adding project-specific records.</span></div>"
        val body = when (area) {
            "projects" -> buildString {
                append(form("ADD_PROJECT", "project", input("Project code", "code") + choices("Project type", "type", listOf("Residential", "Commercial", "Institutional", "Industrial", "Interior", "Landscape")) + select("Client", "clientId", snapshot.clients) + input("Site address", "location") + input("Architect in charge", "architect"), projectId = 0, titleLabel = "Project name", descriptionLabel = "Project description"))
                if (projectId != null) {
                    append(form("ASSIGN_CONTRACTOR", "project contractor", select("Contractor", "contractorId", snapshot.contractors, true), projectId, titleLabel = "Assignment note", descriptionLabel = null))
                    append(form("ADD_FLOOR", "floor", input("Order", "order", "number", value = "0"), projectId, titleLabel = "Floor name", descriptionLabel = null))
                    append(form("ADD_SCOPE", "scope item", choices("Category", "category", listOf("SCOPE", "DELIVERABLE", "EXCLUSION", "ASSUMPTION")) + choices("Status", "status", listOf("INCLUDED", "EXCLUDED", "OPTIONAL")), projectId))
                }
            }
            "planning" -> if (projectId == null) "" else buildString {
                append(form("ADD_TASK", "task", input("Due date", "dueAt", "date") + choices("Status", "status", listOf("OPEN", "IN_PROGRESS", "DONE")), projectId))
                append(form("ADD_APPROVAL", "approval", choices("Approval type", "approvalType", listOf("CLIENT", "TECHNICAL")) + input("Phase", "phase", value = "DESIGN"), projectId))
                append(form("ADD_BACKLOG", "backlog item", choices("Category", "category", listOf("GENERAL", "CLIENT_INPUT", "DESIGN", "COORDINATION", "SITE")) + choices("Priority", "priority", listOf("LOW", "MEDIUM", "HIGH")) + input("Phase", "phase", value = "DESIGN") + input("Due date", "dueAt", "date"), projectId))
                append(form("ADD_SELECTION", "selection item", input("Specification", "specification") + input("Make / brand", "brand") + input("Quantity", "quantity", "number", value = "1") + input("Unit", "unit", value = "Nos") + input("Remarks", "remarks"), projectId, titleLabel = "Item / material", descriptionLabel = null))
                append(form("ADD_SCHEDULE", "schedule entry", input("Date and time", "scheduledAt", "datetime-local", true) + input("Location", "location"), projectId, descriptionLabel = "Notes"))
                append(form("ADD_DECISION", "formal decision", input("Decision title", "decisionTitle", required = true) + area("Context", "context") + area("Impact if delayed", "impact") + input("Requested from", "requestedFrom") + input("Internal owner", "owner"), projectId, titleLabel = "Reference number", descriptionLabel = "Decision required"))
            }
            "site" -> if (projectId == null) "" else form("ADD_DAILY_REPORT", "daily site report", input("Report date", "reportDate", "date", true) + input("Weather", "weather") + input("Manpower / trades", "manpower") + area("Materials received", "materials") + area("Delays or constraints", "delays") + area("Safety observations", "safety") + area("Next-day plan", "nextPlan") + input("Prepared by", "preparedBy"), projectId, titleLabel = "Work completed", descriptionLabel = null) + form("ADD_SITE_ISSUE", "snag / NCR", choices("Record type", "issueType", listOf("SNAG", "NCR")) + input("Issue title", "issueTitle", required = true) + input("Location", "location") + choices("Severity", "severity", listOf("NORMAL", "ATTENTION", "CRITICAL")) + input("Assigned to", "assignedTo") + area("Corrective action", "corrective"), projectId, titleLabel = "Reference number", descriptionLabel = "Description") + form("TRANSITION_SITE_ISSUE", "snag / NCR transition", select("Snag / NCR", "issueId", snapshot.siteIssueRecords, true) + choices("Move to", "toStatus", listOf("OPEN", "IN_PROGRESS", "READY_FOR_VERIFICATION", "CLOSED")) + area("Verification / transition note", "note"), projectId, titleLabel = "Audit note", descriptionLabel = null) + form("ADD_MEETING", "meeting minutes", input("Meeting date and time", "meetingAt", "datetime-local", true) + input("Location", "location") + area("Attendees", "attendees") + area("Decisions", "decisions") + area("Action items", "actions"), projectId, descriptionLabel = "Discussion") + form("ADD_INSPECTION", "site inspection", input("Location", "location", required = true) + input("Inspector", "inspector") + choices("Severity", "severity", listOf("NORMAL", "ATTENTION", "CRITICAL")) + area("Corrective action", "corrective"), projectId, titleLabel = "Observation", descriptionLabel = null)
            "coordination" -> if (projectId == null) "" else form("ADD_RESPONSIBILITY", "responsibility", input("Organisation", "organisation") + input("Discipline", "discipline", required = true) + input("Phase", "phase", value = "All phases") + choices("RACI role", "raciRole", listOf("RESPONSIBLE", "ACCOUNTABLE", "CONSULTED", "INFORMED")) + input("Email", "email", "email") + input("Phone", "phone", "tel"), projectId, titleLabel = "Consultant / lead", descriptionLabel = "Deliverable / responsibility") + form("ADD_COORDINATION", "coordination record", choices("Record type", "recordType", listOf("RFI", "SUBMITTAL", "SITE_INSTRUCTION")) + input("Subject", "subject", required = true) + input("Discipline", "discipline", value = "Architectural") + input("Location", "location") + input("Raised by", "raisedBy") + input("Ball in court", "assignedTo") + choices("Priority", "priority", listOf("LOW", "NORMAL", "HIGH")) + input("Due date", "dueAt", "date"), projectId, titleLabel = "Reference number", descriptionLabel = "Question / requirement") + form("TRANSITION_COORDINATION", "coordination transition", select("Coordination record", "coordinationId", snapshot.coordinationRecords, true) + choices("Move to", "toStatus", listOf("DRAFT", "OPEN", "ANSWERED", "RECEIVED", "UNDER_REVIEW", "APPROVED", "APPROVED_AS_NOTED", "REVISE_RESUBMIT", "ISSUED", "ACKNOWLEDGED", "COMPLIED", "CLOSED")) + area("Response / transition note", "note"), projectId, titleLabel = "Audit note", descriptionLabel = null) + if (session.principal.role == "ADMIN") form("ARCHIVE_COORDINATION", "coordination archive", select("Coordination record", "coordinationId", snapshot.coordinationRecords, true), projectId, titleLabel = "Archive note", descriptionLabel = null) else ""
            "measurements" -> if (projectId == null) "" else form("ADD_MEASUREMENT", "measurement row", select("Contractor", "contractorId", snapshot.contractors, true) + select("Work item", "itemId", snapshot.workItems, true) + select("Floor", "floorId", snapshot.floors) + input("Number", "nos", "number", true, "1") + input("Length", "length", "number") + input("Breadth", "width", "number") + input("Height", "height", "number") + input("Deduction", "deduction", "number", value = "0") + input("Remarks", "remarks"), projectId, titleLabel = "Member description", descriptionLabel = null) + form("TRANSITION_SHEET", "M-Book review action", select("Measurement sheet", "sheetId", snapshot.measurementSheets, true) + choices("Move to", "toStatus", listOf("SUBMITTED", "RETURNED", "CHECKED", "APPROVED")) + area("Review comment", "comment"), projectId, titleLabel = "Audit note", descriptionLabel = null) + if (session.principal.role == "ADMIN") form("ARCHIVE_SHEET", "M-Book archive", select("Measurement sheet", "sheetId", snapshot.measurementSheets, true), projectId, titleLabel = "Archive note", descriptionLabel = null) else ""
            "directory" -> form("ADD_CLIENT", "client", choices("Client type", "clientType", listOf("Individual", "Company", "Government", "Trust", "Society")) + input("Contact person", "contactPerson") + input("Phone", "phone", "tel") + input("Email", "email", "email") + area("Address", "address"), projectId = 0, titleLabel = "Client name", descriptionLabel = null) + form("ADD_CONTRACTOR", "contractor", choices("Contractor type", "contractorType", listOf("Civil", "Electrical", "Plumbing", "Carpenter", "Painter", "Flooring and cladding", "HVAC", "Fire protection", "Landscape", "General")) + input("Phone", "phone", "tel") + area("Address", "address"), projectId = 0, titleLabel = "Contractor name", descriptionLabel = null) + form("QUALIFY_CONTRACTOR", "contractor work item", select("Contractor", "contractorId", snapshot.contractors, true) + select("Work item", "itemId", snapshot.workItems, true), projectId = 0, titleLabel = "Qualification note", descriptionLabel = null)
            "admin" -> if (session.principal.role != "ADMIN") "" else buildString {
                append(form("UPDATE_COMPANY", "company profile", input("Legal name", "legalName", value = snapshot.companyLegalName) + input("Company type", "companyType", value = snapshot.companyType) + area("Registered address", "address", snapshot.companyAddress) + input("Phone", "phone", "tel", value = snapshot.companyPhone) + input("Email", "email", "email", value = snapshot.companyEmail), projectId = 0, titleLabel = "Practice / company name", descriptionLabel = null))
                append(form("CREATE_PORTAL_USER", "portal user", input("Username", "username", required = true) + input("Temporary password", "password", "password", true) + choices("Role", "role", listOf("ADMIN", "EDITOR", "VIEWER")), projectId = 0, titleLabel = "Display name", descriptionLabel = null))
                snapshot.portalUsers.forEach { user -> append("<details class=edit><summary><span>Manage ${escape(user.displayName)}</span><span aria-hidden=true>+</span></summary><form method=post action=/mutate><input type=hidden name=csrf value='$csrf'><input type=hidden name=action value='UPDATE_PORTAL_USER'><input type=hidden name=projectId value=0><input type=hidden name=userId value='${user.id}'><input type=hidden name=title value='${escape(user.displayName)}'>${input("Display name", "displayName", value = user.displayName)}${choices("Role", "role", listOf(user.role) + listOf("ADMIN", "EDITOR", "VIEWER").filter { it != user.role })}${choices("Account state", "active", listOf(user.isActive.toString(), (!user.isActive).toString()))}${input("New password (leave blank to keep)", "newPassword", "password")}<button type=submit>Update user</button></form></details>") }
                append(form("SET_PROJECT_STATUS", "project status", select("Project", "targetProjectId", snapshot.projects.map { PortalOption(it.id, it.name, it.status) }, true) + choices("Status", "status", listOf("PLANNING", "ACTIVE", "ON_HOLD", "COMPLETED", "ARCHIVED")), projectId = 0, titleLabel = "Change note", descriptionLabel = null))
                append(form("ARCHIVE_WORK_ITEM", "work item archive", select("Work item", "itemId", snapshot.workItems, true), projectId = 0, titleLabel = "Archive note", descriptionLabel = null))
            }
            else -> ""
        }
        return if (body.isBlank() && projectId == null && area in setOf("planning", "site", "coordination", "measurements")) selectProject else wrap("Available actions", "Open an action only when you are ready to enter or change a record.", body)
    }

    private fun carbonPage(title: String, content: String) = """
        <!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="color-scheme" content="light"><title>${escape(title)}</title><style>
        :root{--blue:#0f62fe;--blue-hover:#0353e9;--g100:#161616;--g90:#262626;--g80:#393939;--g70:#525252;--g50:#8d8d8d;--g30:#c6c6c6;--g20:#e0e0e0;--g10:#f4f4f4;--red:#da1e28;--header:48px;--nav:256px;font-family:"IBM Plex Sans","Segoe UI",Arial,sans-serif;color:var(--g100);background:var(--g10)}*{box-sizing:border-box}html{scroll-behavior:smooth}body{margin:0;background:var(--g10);line-height:1.4}a{color:var(--blue);text-decoration:none}a:hover{text-decoration:underline}button,input,select,textarea{font:inherit}button,.button{min-height:48px;border:0;border-radius:0;padding:0 16px;background:var(--blue);color:#fff;font-size:14px;font-weight:600;cursor:pointer;display:inline-flex;align-items:center;justify-content:center}button:hover,.button:hover{background:var(--blue-hover);text-decoration:none}button:focus-visible,a:focus-visible,input:focus-visible,select:focus-visible,textarea:focus-visible,summary:focus-visible{outline:2px solid var(--blue);outline-offset:2px}.skip-link{position:fixed;z-index:100;top:-60px;left:16px;background:#fff;padding:12px 16px}.skip-link:focus{top:4px}
        .top-header{height:var(--header);position:fixed;z-index:20;inset:0 0 auto;background:var(--g100);color:#fff;display:grid;grid-template-columns:var(--nav) minmax(220px,1fr) auto;border-bottom:1px solid var(--g80)}.brand{display:flex;align-items:center;height:48px;color:#fff;padding:0 16px;gap:8px}.brand:hover{text-decoration:none;background:var(--g90)}.brand span{font-size:14px;color:var(--g30);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.project-context{display:flex;align-items:center;gap:12px;padding:0 16px;border-left:1px solid var(--g80);min-width:0}.project-context strong{font-size:14px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.context-label{font-size:12px;color:var(--g30)}.user-menu{display:flex;align-items:center;height:48px}.user-menu>span{display:flex;flex-direction:column;padding:0 16px;line-height:1.15}.user-menu strong{font-size:13px}.user-menu small{font-size:11px;color:var(--g30)}.user-menu form{height:48px}.ghost-button{height:48px;background:transparent;border-left:1px solid var(--g80)}.ghost-button:hover{background:var(--g80)}
        .side-nav{position:fixed;z-index:10;top:var(--header);bottom:0;left:0;width:var(--nav);background:var(--g100);color:#fff;display:flex;flex-direction:column}.side-nav nav{padding:8px 0;overflow:auto;flex:1}.nav-item{height:48px;display:flex;align-items:center;gap:16px;padding:0 16px;color:var(--g20);font-size:14px;border-left:4px solid transparent}.nav-item>span{width:20px;height:20px;display:inline-flex;align-items:center;justify-content:center}.nav-item svg{width:20px;height:20px;display:block;fill:currentColor}.nav-item:hover{background:var(--g80);color:#fff;text-decoration:none}.nav-item.active{background:var(--g80);border-left-color:#78a9ff;color:#fff;font-weight:600}.nav-group{font-size:11px;font-weight:600;letter-spacing:.08em;color:var(--g50);margin:24px 16px 8px}.server-note{height:64px;border-top:1px solid var(--g80);display:flex;align-items:center;gap:12px;padding:0 16px}.server-note div{display:flex;flex-direction:column}.server-note strong{font-size:12px}.server-note small{font-size:11px;color:var(--g30)}.live-dot{width:8px;height:8px;background:#42be65;border-radius:50%}
        .app-content{margin-left:var(--nav);padding:calc(var(--header) + 32px) 32px 64px;max-width:1600px;min-height:100vh}.page-title{margin-bottom:32px;max-width:800px}.eyebrow{font-size:12px;font-weight:600;letter-spacing:.08em;color:var(--g70);margin:0 0 8px}.page-title h1{font-size:32px;font-weight:400;line-height:1.2;margin:0 0 8px}.page-title>p:last-child,.section-title p,.table-toolbar p,.supporting,.helper{color:var(--g70);margin:0;font-size:14px}.section-title{margin:40px 0 16px}.section-title h2{font-size:20px;font-weight:400;margin:0 0 4px}.kpi-grid,.module-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:1px;background:var(--g20);margin-bottom:32px}.kpi{background:#fff;min-height:144px;padding:16px;display:flex;flex-direction:column}.kpi span{font-size:14px}.kpi strong{font-size:36px;font-weight:300;margin:auto 0 8px}.kpi small{color:var(--g70)}.module-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.module{background:#fff;min-height:180px}.module-heading{height:48px;padding:0 16px;display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid var(--g20)}.module-heading h2,.table-toolbar h2{font-size:16px;font-weight:600;margin:0}.count{min-width:24px;height:20px;background:var(--g20);font-size:12px;display:inline-flex;align-items:center;justify-content:center}.record-list{list-style:none;margin:0;padding:0}.record-list li{padding:12px 16px;border-bottom:1px solid var(--g20);font-size:14px}.empty-state{padding:24px 16px;color:var(--g70)}.empty-state strong{color:var(--g100);font-size:14px}.empty-state p{font-size:13px;margin:8px 0 0;max-width:480px}
        .data-module{margin-bottom:32px;min-height:0}.table-toolbar{min-height:64px;padding:8px 0 8px 16px;display:flex;align-items:center;justify-content:space-between}.secondary-button{background:var(--g80);align-self:stretch}.secondary-button:hover{background:var(--g70)}.table-wrap{overflow:auto;border-top:1px solid var(--g20)}table{border-collapse:collapse;width:100%;min-width:800px;background:#fff}th,td{text-align:left;border-bottom:1px solid var(--g20);padding:12px 16px;font-size:14px;vertical-align:top}th{background:var(--g20);font-size:12px;font-weight:600;color:var(--g70)}tbody tr:hover{background:#e8f1ff}td a{font-weight:600}.secondary{display:block;color:var(--g70);font-size:12px;margin-top:2px}.numeric{text-align:right}.status{display:inline-block;padding:3px 8px;font-size:11px;font-weight:600;text-transform:capitalize;background:#d0e2ff;color:#0043ce}.status.success{background:#a7f0ba;color:#0e6027}.status.warning{background:#fddc69;color:#684e00}.status.neutral{background:var(--g20);color:var(--g70)}
        .notification{border-left:4px solid var(--blue);background:#edf5ff;padding:12px 16px;margin:0 0 24px;display:flex;flex-direction:column;font-size:14px}.notification.error{border-color:var(--red);background:#fff1f1}.notification span{color:var(--g70)}.actions{margin-top:40px}.action-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:1px;background:var(--g20)}.edit{background:#fff}.edit summary{list-style:none;min-height:56px;padding:0 16px;display:flex;align-items:center;justify-content:space-between;font-size:14px;font-weight:600;cursor:pointer}.edit summary::-webkit-details-marker{display:none}.edit[open] summary{border-bottom:1px solid var(--g20)}.edit[open] summary span:last-child{transform:rotate(45deg)}.edit form{padding:8px 16px 24px}.edit label,.login-form label{display:block;font-size:12px;font-weight:600;margin-top:16px}.edit input,.edit textarea,.edit select,.login-form input{width:100%;min-height:48px;margin-top:6px;padding:10px 16px;border:0;border-bottom:1px solid var(--g50);border-radius:0;background:var(--g10);color:var(--g100)}.edit textarea{min-height:96px;resize:vertical}.edit input:hover,.edit textarea:hover,.edit select:hover,.login-form input:hover{background:var(--g20)}.edit form button{margin-top:24px;min-width:160px}.standalone{padding:64px;max-width:720px}
        .login-layout{min-height:100vh;display:grid;grid-template-columns:minmax(320px,42%) 1fr}.login-brand{background:var(--g100);color:#fff;padding:64px;display:flex;flex-direction:column;justify-content:space-between}.login-brand .eyebrow{color:#78a9ff}.login-brand h1{font-size:54px;font-weight:300;margin:8px 0 16px}.login-brand p{max-width:460px}.login-note{font-size:13px;color:var(--g30)}.login-panel{background:#fff;display:flex;align-items:center;padding:64px}.login-form{width:100%;max-width:440px}.login-form h2{font-size:32px;font-weight:400;margin:0 0 8px}.login-form form{margin-top:32px}.login-form button{width:100%;margin-top:32px}.login-form .helper{margin-top:24px;font-size:12px}
        @media(max-width:1100px){.kpi-grid{grid-template-columns:repeat(2,1fr)}.module-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:800px){:root{--nav:0px}.top-header{grid-template-columns:1fr auto}.project-context,.user-menu>span{display:none}.side-nav{top:48px;bottom:auto;width:100%;height:56px;display:block;overflow-x:auto}.side-nav nav{display:flex;padding:0;overflow:visible;width:max-content}.nav-item{height:56px;min-width:max-content;padding:0 14px;border-left:0;border-bottom:3px solid transparent}.nav-item.active{border-left:0;border-bottom-color:var(--blue)}.nav-group,.server-note{display:none}.app-content{margin-left:0;padding:136px 16px 48px}.module-grid,.action-grid{grid-template-columns:1fr}.login-layout{grid-template-columns:1fr}.login-brand{min-height:260px;padding:40px 24px}.login-brand h1{font-size:42px}.login-panel{padding:40px 24px}}@media(max-width:520px){.brand span{display:none}.page-title h1{font-size:28px}.kpi-grid{grid-template-columns:1fr}.kpi{min-height:112px}.standalone{padding:32px 16px}}
        .toolbar-actions,.table-search{display:flex;align-items:stretch}.table-search input{height:48px;width:220px;border:0;border-bottom:1px solid var(--g50);background:var(--g10);padding:0 16px}.table-search button{background:var(--g80)}.visually-hidden{position:absolute!important;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}@media(max-width:680px){.table-toolbar{align-items:stretch;flex-direction:column;padding:16px 0 0}.table-toolbar>div:first-child{padding:0 16px 12px}.toolbar-actions{display:grid;grid-template-columns:1fr}.table-search input{width:100%}.secondary-button{height:48px}}
        </style></head><body>$content</body></html>
    """.trimIndent()

    private fun secureSocket(address: InetAddress): Pair<java.net.ServerSocket, String> {
        val androidStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!androidStore.containsAlias(TLS_ALIAS)) {
            val now = Calendar.getInstance(); val until = Calendar.getInstance().apply { add(Calendar.YEAR, 10) }
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply {
                initialize(
                    KeyGenParameterSpec.Builder(TLS_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(2048)
                        // Android 12 Conscrypt may use a raw RSA operation for
                        // TLS 1.2 signatures. Samsung KeyMaster rejects it unless
                        // DIGEST_NONE was authorised when the key was created.
                        .setDigests(
                            KeyProperties.DIGEST_NONE,
                            KeyProperties.DIGEST_SHA256,
                            KeyProperties.DIGEST_SHA384,
                            KeyProperties.DIGEST_SHA512
                        )
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_NONE,
                            KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1
                        )
                        .setCertificateSubject(X500Principal("CN=ArchiMan Local Portal"))
                        .setCertificateSerialNumber(BigInteger(64, random))
                        .setCertificateNotBefore(now.time)
                        .setCertificateNotAfter(until.time)
                        .build()
                )
            }.generateKeyPair()
        }
        val certificate = androidStore.getCertificate(TLS_ALIAS) as X509Certificate
        // AndroidKeyStore private keys are hardware-backed handles and are not
        // exportable. Initialising a BKS memory store with that handle fails on
        // Samsung/Bouncy Castle because the key has no encoded format.
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(androidStore, null) }
        val platformKeyManager = kmf.keyManagers.filterIsInstance<X509KeyManager>().first()
        val ssl = SSLContext.getInstance("TLS").apply {
            // Browsers connect to the phone by IPv4 address and therefore may
            // omit SNI. Force this server's private-key alias instead of relying
            // on vendor-specific Android key-manager alias selection.
            init(arrayOf(FixedServerAliasKeyManager(platformKeyManager, TLS_ALIAS)), null, random)
        }
        val socket = ssl.serverSocketFactory.createServerSocket(port, 24, address) as SSLServerSocket
        socket.enabledProtocols = socket.enabledProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }.toTypedArray()
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString(":") { "%02X".format(it) }
        return socket to fingerprint
    }

    private class FixedServerAliasKeyManager(
        private val delegate: X509KeyManager,
        private val serverAlias: String
    ) : X509ExtendedKeyManager() {
        override fun chooseClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, socket: Socket?): String? =
            delegate.chooseClientAlias(keyType, issuers, socket)
        override fun chooseEngineClientAlias(keyType: Array<out String>?, issuers: Array<out Principal>?, engine: SSLEngine?): String? =
            delegate.chooseClientAlias(keyType, issuers, null)
        override fun chooseServerAlias(keyType: String?, issuers: Array<out Principal>?, socket: Socket?): String = serverAlias
        override fun chooseEngineServerAlias(keyType: String?, issuers: Array<out Principal>?, engine: SSLEngine?): String = serverAlias
        override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = delegate.getClientAliases(keyType, issuers)
        override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> = arrayOf(serverAlias)
        override fun getCertificateChain(alias: String?): Array<X509Certificate>? = delegate.getCertificateChain(alias)
        override fun getPrivateKey(alias: String?): PrivateKey? = delegate.getPrivateKey(alias)
    }

    private val cookieName get() = if (secureTransport) "__Host-ARCHIMANSESSION" else "ARCHIMANSESSION"
    private fun secureTokenMatches(candidate: String?, expected: String) = MessageDigest.isEqual(candidate.orEmpty().toByteArray(StandardCharsets.UTF_8), expected.toByteArray(StandardCharsets.UTF_8))
    private fun setCookieHeader(token: String) = "Set-Cookie: $cookieName=$token; ${if (secureTransport) "Secure; " else ""}HttpOnly; SameSite=Strict; Path=/; Max-Age=${SESSION_DURATION_MS / 1000}"
    private fun clearCookieHeader() = "Set-Cookie: $cookieName=; ${if (secureTransport) "Secure; " else ""}HttpOnly; SameSite=Strict; Path=/; Max-Age=0"
    private fun randomToken(bytes: Int) = ByteArray(bytes).also(random::nextBytes).joinToString("") { "%02x".format(it) }
    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
    private fun formatTime(value: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))

    companion object {
        const val PORT = 8765
        const val SESSION_DURATION_MS = 30L * 60L * 1000L
        private const val SESSION_IDLE_TIMEOUT_MS = 15L * 60L * 1000L
        const val PORTAL_DURATION_MS = 60L * 60L * 1000L
        private const val LOGIN_BLOCK_MS = 5L * 60L * 1000L
        private const val SNAPSHOT_CACHE_MS = 2_000L
        private const val MAX_CONCURRENT_CLIENTS = 12
        private const val MAX_SESSIONS = 64
        private const val MAX_FAILED_LOGIN_SOURCES = 128
        private const val MAX_REQUEST_LINE_LENGTH = 4_096
        private const val MAX_HEADER_LINE_LENGTH = 8_192
        private const val MAX_HEADER_COUNT = 64
        private const val MAX_BODY_BYTES = 8_192
        private const val WIFI_ADDRESS_ATTEMPTS = 12
        private const val WIFI_ADDRESS_RETRY_MS = 250L
        // Version the alias whenever key authorisations change; AndroidKeyStore
        // cannot modify the policy of an existing hardware-backed key.
        private const val TLS_ALIAS = "archiman_local_portal_tls_v3"
        private const val LOG_TAG = "ArchiManPortal"
        private val ADMIN_ACTIONS = setOf("CREATE_PORTAL_USER", "UPDATE_PORTAL_USER", "SET_PROJECT_STATUS", "ARCHIVE_WORK_ITEM", "ARCHIVE_COORDINATION", "ARCHIVE_SHEET")
        private val EDIT_ACTIONS = setOf("UPDATE_COMPANY", "ADD_PROJECT", "ADD_CLIENT", "ADD_CONTRACTOR", "QUALIFY_CONTRACTOR", "ASSIGN_CONTRACTOR", "ADD_FLOOR", "ADD_TASK", "ADD_APPROVAL", "ADD_BACKLOG", "ADD_SCOPE", "ADD_SELECTION", "ADD_SCHEDULE", "ADD_DECISION", "ADD_DAILY_REPORT", "ADD_SITE_ISSUE", "ADD_MEETING", "ADD_INSPECTION", "ADD_RESPONSIBILITY", "ADD_COORDINATION", "ADD_MEASUREMENT", "TRANSITION_SHEET", "TRANSITION_SITE_ISSUE", "TRANSITION_COORDINATION") + ADMIN_ACTIONS
    }
}
