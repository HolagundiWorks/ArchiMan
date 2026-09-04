package com.example.portal

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

data class PortalProject(
    val name: String,
    val code: String,
    val type: String,
    val status: String,
    val client: String,
    val location: String,
    val architect: String,
    val measurementCount: Int
)

data class PortalSnapshot(
    val companyName: String,
    val projects: List<PortalProject>,
    val selectedProject: String?,
    val tasks: List<String>,
    val schedules: List<String>,
    val inspections: List<String>,
    val drawings: List<String>
)

data class LocalPortalState(
    val isRunning: Boolean = false,
    val url: String = "",
    val pin: String = "",
    val expiresAt: Long? = null,
    val error: String? = null
)

class LocalPortalServer(
    private val context: Context,
    private val bindAddressOverride: InetAddress? = null,
    private val port: Int = PORT
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessions = Collections.synchronizedSet(mutableSetOf<String>())
    private val random = SecureRandom()
    private val _state = MutableStateFlow(LocalPortalState())
    val state: StateFlow<LocalPortalState> = _state.asStateFlow()
    private var serverSocket: ServerSocket? = null
    private var expiryJob: Job? = null
    private var snapshotProvider: (() -> PortalSnapshot)? = null

    fun start(provider: () -> PortalSnapshot) {
        if (_state.value.isRunning) return
        val address = bindAddressOverride ?: wifiAddress()
        if (address == null) {
            _state.value = LocalPortalState(error = "Connect this phone to a local Wi-Fi network first.")
            return
        }
        val pin = (random.nextInt(900_000) + 100_000).toString()
        snapshotProvider = provider
        scope.launch {
            try {
                val socket = ServerSocket(port, 24, address)
                serverSocket = socket
                val expiresAt = System.currentTimeMillis() + SESSION_DURATION_MS
                _state.value = LocalPortalState(true, "http://${address.hostAddress}:${socket.localPort}", pin, expiresAt)
                expiryJob = scope.launch {
                    delay(SESSION_DURATION_MS)
                    stop()
                }
                while (!socket.isClosed) {
                    val client = socket.accept()
                    launch { handle(client, pin) }
                }
            } catch (_: SocketException) {
                // Expected when the user stops sharing.
            } catch (error: Exception) {
                _state.value = LocalPortalState(error = error.message ?: "Could not start the local portal.")
            } finally {
                runCatching { serverSocket?.close() }
                serverSocket = null
                sessions.clear()
                if (_state.value.isRunning) _state.value = LocalPortalState()
            }
        }
    }

    fun stop() {
        runCatching { serverSocket?.close() }
        serverSocket = null
        expiryJob?.cancel()
        expiryJob = null
        sessions.clear()
        snapshotProvider = null
        _state.value = LocalPortalState()
    }

    fun close() {
        stop()
        scope.cancel()
    }

    private fun wifiAddress(): InetAddress? {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = manager.activeNetwork ?: return null
        val capabilities = manager.getNetworkCapabilities(network) ?: return null
        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
        return manager.getLinkProperties(network)?.linkAddresses
            ?.map { it.address }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress && !it.isLinkLocalAddress }
    }

    private fun handle(socket: Socket, pin: String) = socket.use { client ->
        client.soTimeout = 5_000
        val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
        val requestLine = reader.readLine() ?: return
        val parts = requestLine.split(' ')
        if (parts.size < 2) return
        val method = parts[0]
        val path = parts[1]
        var contentLength = 0
        var cookie = ""
        var lineCount = 0
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isBlank()) break
            lineCount++
            if (lineCount > 64) return
            if (line.startsWith("Content-Length:", true)) contentLength = line.substringAfter(':').trim().toIntOrNull()?.coerceIn(0, 1024) ?: 0
            if (line.startsWith("Cookie:", true)) cookie = line.substringAfter(':').trim()
        }
        val authenticated = cookie.split(';').map(String::trim).any { entry ->
            entry.startsWith("AMBSESSION=") && sessions.contains(entry.substringAfter('='))
        }
        when {
            method == "GET" && path == "/health" -> respond(client, 200, "text/plain; charset=utf-8", "AMB local portal")
            method == "POST" && path == "/login" -> {
                val bodyChars = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val count = reader.read(bodyChars, read, contentLength - read)
                    if (count < 0) break
                    read += count
                }
                val suppliedPin = bodyChars.concatToString(0, read).split('&')
                    .firstOrNull { it.startsWith("pin=") }
                    ?.substringAfter('=')
                    ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
                if (suppliedPin == pin) {
                    val token = ByteArray(18).also(random::nextBytes).joinToString("") { "%02x".format(it) }
                    sessions += token
                    respond(client, 303, "text/plain", "Open AMB", listOf("Location: /", "Set-Cookie: AMBSESSION=$token; HttpOnly; SameSite=Strict; Path=/"))
                } else respond(client, 401, "text/html; charset=utf-8", loginPage("Incorrect PIN"))
            }
            method != "GET" -> respond(client, 405, "text/plain", "Method not allowed")
            !authenticated -> respond(client, 401, "text/html; charset=utf-8", loginPage())
            path == "/" -> respond(client, 200, "text/html; charset=utf-8", portalPage(snapshotProvider?.invoke()))
            else -> respond(client, 404, "text/plain", "Not found")
        }
    }

    private fun respond(socket: Socket, status: Int, type: String, body: String, extraHeaders: List<String> = emptyList()) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        val reason = when (status) { 200 -> "OK"; 303 -> "See Other"; 401 -> "Unauthorized"; 404 -> "Not Found"; 405 -> "Method Not Allowed"; else -> "Error" }
        val headers = buildString {
            append("HTTP/1.1 $status $reason\r\n")
            append("Content-Type: $type\r\nContent-Length: ${bytes.size}\r\n")
            append("Cache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nX-Frame-Options: DENY\r\n")
            append("Content-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; base-uri 'none'\r\n")
            extraHeaders.forEach { append(it).append("\r\n") }
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.UTF_8)
        socket.getOutputStream().apply { write(headers); write(bytes); flush() }
    }

    private fun loginPage(error: String = "") = page("AMB Local Portal", """
        <main class="login"><h1>AMB Local Portal</h1><p>Enter the six-digit PIN shown on the phone.</p>
        ${if (error.isBlank()) "" else "<p class=error>${escape(error)}</p>"}
        <form method="post" action="/login"><label>Access PIN<input name="pin" inputmode="numeric" maxlength="6" required autofocus></label><button>Open portal</button></form></main>
    """.trimIndent())

    private fun portalPage(snapshot: PortalSnapshot?): String {
        if (snapshot == null) return page("AMB Local Portal", "<main><h1>AMB</h1><p>Project data is not ready.</p></main>")
        val projectRows = snapshot.projects.joinToString("") { project ->
            "<tr><td><strong>${escape(project.name)}</strong><small>${escape(project.code)}</small></td><td>${escape(project.type)}</td><td>${escape(project.client)}</td><td>${escape(project.location)}</td><td>${project.measurementCount}</td><td><span class=tag>${escape(project.status)}</span></td></tr>"
        }
        fun list(title: String, values: List<String>) = "<section><h2>${escape(title)} <span>${values.size}</span></h2>" +
            if (values.isEmpty()) "<p class=muted>No records</p></section>" else "<ul>${values.joinToString("") { "<li>${escape(it)}</li>" }}</ul></section>"
        return page("AMB · ${snapshot.companyName}", """
            <header><div><b>AMB</b><span>Local read-only portal</span></div><div>${escape(snapshot.companyName)}</div></header>
            <main><h1>Projects</h1><p class=muted>Live view from the connected phone · ${formatTime(System.currentTimeMillis())}</p>
            <div class=table><table><thead><tr><th>Project</th><th>Type</th><th>Client</th><th>Location</th><th>Measurements</th><th>Status</th></tr></thead><tbody>$projectRows</tbody></table></div>
            <h1>${escape(snapshot.selectedProject ?: "Selected project")}</h1><div class=grid>
            ${list("Tasks", snapshot.tasks)}${list("Schedule", snapshot.schedules)}${list("Inspections", snapshot.inspections)}${list("Drawings", snapshot.drawings)}
            </div></main><footer>Available only while Local Wi-Fi Portal is active on the phone.</footer>
        """.trimIndent())
    }

    private fun page(title: String, content: String) = """<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(title)}</title><style>
        :root{font-family:Inter,system-ui,sans-serif;color:#161616;background:#f4f4f4}*{box-sizing:border-box}body{margin:0}header{background:#161616;color:white;padding:16px 5%;display:flex;justify-content:space-between}header span{display:block;color:#c6c6c6;font-size:12px}main{max-width:1180px;margin:auto;padding:28px 5%}h1{font-size:22px;margin:8px 0}h2{font-size:15px;margin:0 0 12px}h2 span,.tag{font-size:11px;background:#e0e0e0;padding:3px 7px}.table{overflow:auto;background:white;border:1px solid #ddd;margin:18px 0 30px}table{border-collapse:collapse;width:100%;min-width:760px}th,td{text-align:left;padding:11px;border-bottom:1px solid #e0e0e0;font-size:13px}th{font-size:11px;background:#f4f4f4}td small{display:block;color:#6f6f6f}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:12px}section{background:white;border:1px solid #ddd;padding:16px}ul{margin:0;padding-left:18px}li{margin:7px 0;font-size:13px}.muted{color:#6f6f6f;font-size:13px}.login{max-width:420px;margin:10vh auto;background:white;border:1px solid #ddd;padding:28px}label{display:block;font-size:13px}input{width:100%;font-size:22px;letter-spacing:6px;padding:12px;margin:8px 0 14px}button{width:100%;padding:12px;background:#0f62fe;color:white;border:0;font-weight:700}.error{color:#da1e28}footer{text-align:center;padding:20px;color:#6f6f6f;font-size:11px}</style></head><body>$content</body></html>"""

    private fun escape(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;")
    private fun formatTime(value: Long) = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(value))

    companion object {
        const val PORT = 8765
        const val SESSION_DURATION_MS = 60L * 60L * 1000L
    }
}
