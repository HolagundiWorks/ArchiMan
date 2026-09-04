package com.example.portal

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.security.auth.x500.X500Principal

data class PortalProject(val id: Long, val name: String, val code: String, val type: String, val status: String, val client: String, val location: String, val architect: String, val measurementCount: Int)
data class PortalSnapshot(val companyName: String, val projects: List<PortalProject>, val selectedProject: String?, val selectedProjectId: Long?, val tasks: List<String>, val schedules: List<String>, val inspections: List<String>, val drawings: List<String>)
data class PortalMutation(val action: String, val projectId: Long, val title: String, val description: String, val sourceAddress: String)
data class PortalMutationResult(val success: Boolean, val message: String)
data class LocalPortalState(val isRunning: Boolean = false, val url: String = "", val certificateFingerprint: String = "", val expiresAt: Long? = null, val error: String? = null)
private data class PortalSession(val principal: PortalPrincipal, val csrfToken: String, val expiresAt: Long)
private data class FailedLogin(var count: Int, var blockedUntil: Long)

class LocalPortalServer(private val context: Context, private val bindAddressOverride: InetAddress? = null, private val port: Int = PORT, private val secureTransport: Boolean = true) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = Collections.synchronizedMap(mutableMapOf<String, PortalSession>())
    private val failedLogins = Collections.synchronizedMap(mutableMapOf<String, FailedLogin>())
    private val random = SecureRandom()
    private val _state = MutableStateFlow(LocalPortalState())
    val state: StateFlow<LocalPortalState> = _state.asStateFlow()
    private var serverSocket: java.net.ServerSocket? = null
    private var expiryJob: Job? = null
    private var snapshotProvider: (() -> PortalSnapshot)? = null
    private var authenticator: (suspend (String, CharArray) -> PortalPrincipal?)? = null
    private var mutationHandler: (suspend (PortalPrincipal, PortalMutation) -> PortalMutationResult)? = null

    fun start(provider: () -> PortalSnapshot, authenticate: suspend (String, CharArray) -> PortalPrincipal?, mutate: suspend (PortalPrincipal, PortalMutation) -> PortalMutationResult) {
        if (_state.value.isRunning) return
        val address = bindAddressOverride ?: wifiAddress()
        if (address == null) { _state.value = LocalPortalState(error = "Connect this phone to a local Wi-Fi network first."); return }
        snapshotProvider = provider; authenticator = authenticate; mutationHandler = mutate
        scope.launch {
            try {
                val (socket, fingerprint) = if (secureTransport) secureSocket(address) else Pair(java.net.ServerSocket(port, 24, address), "TEST")
                serverSocket = socket
                val expiresAt = System.currentTimeMillis() + PORTAL_DURATION_MS
                _state.value = LocalPortalState(true, "${if (secureTransport) "https" else "http"}://${address.hostAddress}:${socket.localPort}", fingerprint, expiresAt)
                expiryJob = launch { delay(PORTAL_DURATION_MS); stop() }
                while (!socket.isClosed) {
                    val client = socket.accept()
                    launch { handle(client) }
                }
            } catch (_: SocketException) {
            } catch (error: Exception) {
                _state.value = LocalPortalState(error = error.message ?: "Could not start the local portal.")
            } finally {
                runCatching { serverSocket?.close() }; serverSocket = null; sessions.clear()
                if (_state.value.isRunning) _state.value = LocalPortalState()
            }
        }
    }

    fun stop() { runCatching { serverSocket?.close() }; serverSocket = null; expiryJob?.cancel(); expiryJob = null; sessions.clear(); failedLogins.clear(); snapshotProvider = null; authenticator = null; mutationHandler = null; _state.value = LocalPortalState() }
    fun close() { stop(); scope.cancel() }

    private fun wifiAddress(): InetAddress? {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = manager.activeNetwork ?: return null
        val capabilities = manager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        return manager.getLinkProperties(network)?.linkAddresses?.map { it.address }?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress && !it.isLinkLocalAddress }
    }

    private suspend fun handle(socket: Socket) = socket.use { client ->
        client.soTimeout = 8_000
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
        val requestLine = reader.readLine() ?: return
        val parts = requestLine.split(' '); if (parts.size < 2) return
        val method = parts[0]; val target = parts[1]
        var contentLength = 0; var cookie = ""; var lineCount = 0
        while (true) {
            val line = reader.readLine() ?: break; if (line.isBlank()) break
            if (++lineCount > 64) return
            if (line.startsWith("Content-Length:", true)) contentLength = line.substringAfter(':').trim().toIntOrNull()?.coerceIn(0, 8_192) ?: 0
            if (line.startsWith("Cookie:", true)) cookie = line.substringAfter(':').trim()
        }
        val body = if (contentLength > 0) CharArray(contentLength).let { chars -> var read = 0; while (read < chars.size) { val n = reader.read(chars, read, chars.size - read); if (n < 0) break; read += n }; chars.concatToString(0, read) } else ""
        val form = parseForm(body)
        val token = cookie.split(';').map(String::trim).firstNotNullOfOrNull { entry -> if (entry.startsWith("$cookieName=")) entry.substringAfter('=') else null }
        val session = token?.let(sessions::get)?.takeIf { it.expiresAt > System.currentTimeMillis() }
        val path = target.substringBefore('?')
        when {
            method == "GET" && path == "/health" -> respond(client, 200, "text/plain; charset=utf-8", "ArchiMan local portal")
            method == "POST" && path == "/login" -> login(client, form, client.inetAddress.hostAddress.orEmpty())
            method == "POST" && path == "/logout" && session != null -> {
                if (!secureTokenMatches(form["csrf"], session.csrfToken)) respond(client, 403, "text/plain", "Invalid request token.")
                else { token?.let(sessions::remove); respond(client, 303, "text/plain", "Signed out", listOf("Location: /", clearCookieHeader())) }
            }
            session == null -> respond(client, 401, "text/html; charset=utf-8", loginPage())
            method == "POST" && path == "/mutate" -> mutate(client, session, form, client.inetAddress.hostAddress.orEmpty())
            method != "GET" -> respond(client, 405, "text/plain", "Method not allowed")
            path == "/" -> respond(client, 200, "text/html; charset=utf-8", portalPage(snapshotProvider?.invoke(), session))
            else -> respond(client, 404, "text/plain", "Not found")
        }
    }

    private suspend fun login(client: Socket, form: Map<String, String>, address: String) {
        val failure = failedLogins[address]
        if (failure != null && failure.blockedUntil > System.currentTimeMillis()) { respond(client, 429, "text/html; charset=utf-8", loginPage("Too many attempts. Try again later.")); return }
        val principal = runCatching { authenticator?.invoke(form["username"].orEmpty(), form["password"].orEmpty().toCharArray()) }.getOrNull()
        if (principal == null) {
            val next = (failure?.count ?: 0) + 1; failedLogins[address] = FailedLogin(next, if (next >= 5) System.currentTimeMillis() + LOGIN_BLOCK_MS else 0)
            respond(client, 401, "text/html; charset=utf-8", loginPage("Incorrect username or password")); return
        }
        failedLogins.remove(address)
        val token = randomToken(32); val csrf = randomToken(24)
        sessions[token] = PortalSession(principal, csrf, System.currentTimeMillis() + SESSION_DURATION_MS)
        respond(client, 303, "text/plain", "Open ArchiMan", listOf("Location: /", setCookieHeader(token)))
    }

    private suspend fun mutate(client: Socket, session: PortalSession, form: Map<String, String>, address: String) {
        if (session.principal.role !in setOf("ADMIN", "EDITOR")) { respond(client, 403, "text/plain", "This account cannot edit records."); return }
        if (!secureTokenMatches(form["csrf"], session.csrfToken)) { respond(client, 403, "text/plain", "Invalid request token."); return }
        val action = form["action"].orEmpty(); val projectId = form["projectId"]?.toLongOrNull() ?: 0L; val title = form["title"].orEmpty().trim(); val description = form["description"].orEmpty().trim()
        if (projectId <= 0 || title.isBlank() || title.length > 160 || description.length > 2_000 || action !in setOf("ADD_TASK", "ADD_APPROVAL", "ADD_BACKLOG")) { respond(client, 400, "text/plain", "Invalid or incomplete edit."); return }
        val result = mutationHandler?.invoke(session.principal, PortalMutation(action, projectId, title, description, address)) ?: PortalMutationResult(false, "Editing is unavailable.")
        respond(client, if (result.success) 303 else 400, "text/plain", result.message, if (result.success) listOf("Location: /") else emptyList())
    }

    private fun parseForm(body: String): Map<String, String> = body.split('&').mapNotNull { part -> val key = part.substringBefore('=', ""); if (key.isBlank()) null else URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(part.substringAfter('=', ""), "UTF-8") }.toMap()

    private fun respond(socket: Socket, status: Int, type: String, body: String, extraHeaders: List<String> = emptyList()) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val reason = mapOf(200 to "OK", 303 to "See Other", 400 to "Bad Request", 401 to "Unauthorized", 403 to "Forbidden", 404 to "Not Found", 405 to "Method Not Allowed", 429 to "Too Many Requests")[status] ?: "Error"
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\nReferrer-Policy: no-referrer\r\n")
            append("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'\r\n")
            extraHeaders.forEach { append(it).append("\r\n") }; append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        socket.getOutputStream().apply { write(headers); write(bytes); flush() }
    }

    private fun loginPage(error: String = "") = page("ArchiMan Login", """<main class="login"><h1>ArchiMan</h1><p>Sign in with a local portal account created on the phone.</p>${if (error.isBlank()) "" else "<p class=error>${escape(error)}</p>"}<form method="post" action="/login"><label>Username<input name="username" maxlength="40" autocomplete="username" required autofocus></label><label>Password<input name="password" type="password" maxlength="128" autocomplete="current-password" required></label><button>Sign in</button></form></main>""")

    private fun portalPage(snapshot: PortalSnapshot?, session: PortalSession): String {
        if (snapshot == null) return page("ArchiMan", "<main><h1>ArchiMan</h1><p>Project data is not ready.</p></main>")
        val rows = snapshot.projects.joinToString("") { p -> "<tr><td><strong>${escape(p.name)}</strong><small>${escape(p.code)}</small></td><td>${escape(p.type)}</td><td>${escape(p.client)}</td><td>${escape(p.location)}</td><td>${p.measurementCount}</td><td><span class=tag>${escape(p.status)}</span></td></tr>" }
        fun list(title: String, values: List<String>) = "<section><h2>${escape(title)} <span>${values.size}</span></h2>" + if (values.isEmpty()) "<p class=muted>No records</p></section>" else "<ul>${values.joinToString("") { "<li>${escape(it)}</li>" }}</ul></section>"
        val edit = if (session.principal.role == "VIEWER" || snapshot.selectedProjectId == null) "<p class=muted>Viewer access: records cannot be changed.</p>" else listOf("ADD_TASK" to "Task", "ADD_APPROVAL" to "Approval", "ADD_BACKLOG" to "Backlog action").joinToString("") { (action, label) -> "<form class=edit method=post action=/mutate><h2>Add $label</h2><input type=hidden name=csrf value='${session.csrfToken}'><input type=hidden name=action value='$action'><input type=hidden name=projectId value='${snapshot.selectedProjectId}'><label>Title<input name=title maxlength=160 required></label><label>Details<textarea name=description maxlength=2000></textarea></label><button>Add</button></form>" }
        return page("ArchiMan · ${snapshot.companyName}", """<header><div><b>ArchiMan</b><span>${escape(snapshot.companyName)}</span></div><div>${escape(session.principal.displayName)} · ${escape(session.principal.role)}<form method=post action=/logout><input type=hidden name=csrf value='${session.csrfToken}'><button class=link>Sign out</button></form></div></header><main><h1>Projects</h1><p class=muted>Secure live workspace from the connected phone · ${formatTime(System.currentTimeMillis())}</p><div class=table><table><thead><tr><th>Project</th><th>Type</th><th>Client</th><th>Location</th><th>Measurements</th><th>Status</th></tr></thead><tbody>$rows</tbody></table></div><h1>${escape(snapshot.selectedProject ?: "Selected project")}</h1><div class=grid>${list("Tasks", snapshot.tasks)}${list("Schedule", snapshot.schedules)}${list("Inspections", snapshot.inspections)}${list("Drawings", snapshot.drawings)}</div><h1>Quick entry</h1><div class=grid>$edit</div></main>""")
    }

    private fun page(title: String, content: String) = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(title)}</title><style>:root{font-family:system-ui,sans-serif;color:#161616;background:#f4f4f4}*{box-sizing:border-box}body{margin:0}header{background:#161616;color:#fff;padding:14px 5%;display:flex;justify-content:space-between;gap:16px}header span{display:block;color:#c6c6c6;font-size:12px}header form{display:inline;margin-left:8px}main{max-width:1180px;margin:auto;padding:24px 5%}h1{font-size:22px;margin:12px 0}h2{font-size:15px;margin:0 0 10px}h2 span,.tag{font-size:11px;background:#e0e0e0;padding:3px 7px}.table{overflow:auto;background:#fff;border:1px solid #ddd;margin:14px 0 26px}table{border-collapse:collapse;width:100%;min-width:760px}th,td{text-align:left;padding:11px;border-bottom:1px solid #e0e0e0;font-size:13px}th{font-size:11px;background:#f4f4f4}td small{display:block;color:#6f6f6f}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:12px}section,.edit{background:#fff;border:1px solid #ddd;padding:16px}ul{margin:0;padding-left:18px}li{margin:7px 0;font-size:13px}.muted{color:#6f6f6f;font-size:13px}.login{max-width:420px;margin:8vh auto;background:#fff;border:1px solid #ddd;padding:28px}label{display:block;font-size:13px;margin:8px 0}input,textarea{width:100%;font-size:16px;padding:10px;margin-top:5px}textarea{min-height:72px}button{width:100%;padding:11px;background:#0f62fe;color:#fff;border:0;font-weight:700}.link{width:auto;padding:3px 6px;background:transparent;text-decoration:underline}.error{color:#da1e28}</style></head><body>$content</body></html>"""

    private fun secureSocket(address: InetAddress): Pair<java.net.ServerSocket, String> {
        val androidStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!androidStore.containsAlias(TLS_ALIAS)) {
            val now = Calendar.getInstance(); val until = Calendar.getInstance().apply { add(Calendar.YEAR, 10) }
            KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore").apply { initialize(KeyGenParameterSpec.Builder(TLS_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT).setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256).setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1).setCertificateSubject(X500Principal("CN=ArchiMan Local Portal")).setCertificateSerialNumber(BigInteger(64, random)).setCertificateNotBefore(now.time).setCertificateNotAfter(until.time).build()) }.generateKeyPair()
        }
        val key = androidStore.getKey(TLS_ALIAS, null) as PrivateKey; val certificate = androidStore.getCertificate(TLS_ALIAS) as X509Certificate
        val memory = KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null); setKeyEntry("server", key, charArrayOf(), arrayOf(certificate)) }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply { init(memory, charArrayOf()) }
        val ssl = SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, null, random) }
        val socket = ssl.serverSocketFactory.createServerSocket(port, 24, address) as SSLServerSocket
        socket.enabledProtocols = socket.enabledProtocols.filter { it == "TLSv1.2" || it == "TLSv1.3" }.toTypedArray()
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString(":") { "%02X".format(it) }
        return socket to fingerprint
    }

    private val cookieName get() = if (secureTransport) "__Host-ARCHIMANSESSION" else "ARCHIMANSESSION"
    private fun secureTokenMatches(candidate: String?, expected: String) = MessageDigest.isEqual(candidate.orEmpty().toByteArray(StandardCharsets.UTF_8), expected.toByteArray(StandardCharsets.UTF_8))
    private fun setCookieHeader(token: String) = "Set-Cookie: $cookieName=$token; ${if (secureTransport) "Secure; " else ""}HttpOnly; SameSite=Strict; Path=/; Max-Age=${SESSION_DURATION_MS / 1000}"
    private fun clearCookieHeader() = "Set-Cookie: $cookieName=; ${if (secureTransport) "Secure; " else ""}HttpOnly; SameSite=Strict; Path=/; Max-Age=0"
    private fun randomToken(bytes: Int) = ByteArray(bytes).also(random::nextBytes).joinToString("") { "%02x".format(it) }
    private fun escape(value: String) = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
    private fun formatTime(value: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))

    companion object { const val PORT = 8765; const val SESSION_DURATION_MS = 30L * 60L * 1000L; const val PORTAL_DURATION_MS = 60L * 60L * 1000L; private const val LOGIN_BLOCK_MS = 5L * 60L * 1000L; private const val TLS_ALIAS = "archiman_local_portal_tls" }
}
