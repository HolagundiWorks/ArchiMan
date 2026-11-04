package com.holagundi.archiman.portal

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

data class PortalRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
)

object PortalHttp {
    const val PORT = 8765
    const val MAX_CLIENTS = 12
    const val SESSION_SECONDS = 30 * 60
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray()
    private val random = SecureRandom()

    fun newCode(): String = buildString(8) { repeat(8) { append(alphabet[random.nextInt(alphabet.size)]) } }

    fun codesMatch(expected: String, given: String): Boolean {
        val left = expected.trim().uppercase().toByteArray(StandardCharsets.UTF_8)
        val right = given.trim().uppercase().toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(left, right)
    }

    fun chooseAddress(candidates: List<Pair<String, String>>): String? {
        val wifi = candidates.firstOrNull { (name, _) ->
            val key = name.lowercase()
            key.contains("wlan") || key.contains("wifi") || key.startsWith("ap")
        }
        if (wifi != null) return wifi.second
        return candidates.firstOrNull { (name, _) ->
            val key = name.lowercase()
            !key.contains("rmnet") && !key.contains("ccmni") && !key.contains("pdp") && !key.contains("tun")
        }?.second
    }

    fun hostAllowed(hostHeader: String?, address: String, port: Int = PORT): Boolean {
        val host = hostHeader?.substringBefore(',')?.trim()?.lowercase().orEmpty()
        if (host.isEmpty() || host.contains('@')) return false
        val expected = address.lowercase()
        return host == expected || host == "$expected:$port"
    }

    fun originAllowed(origin: String?, address: String, port: Int = PORT): Boolean {
        if (origin.isNullOrBlank()) return true
        return origin.trim() == "http://${address.lowercase()}:$port" || origin.trim() == "http://$address:$port"
    }

    fun escape(value: String): String = buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    fun form(body: String): Map<String, String> = decodePairs(body)

    fun cookie(header: String?, name: String): String? {
        if (header.isNullOrBlank()) return null
        return header.split(';').map { it.trim() }.firstNotNullOfOrNull { part ->
            val split = part.split('=', limit = 2)
            if (split.size == 2 && split[0] == name) urlDecode(split[1]) else null
        }
    }

    fun read(input: InputStream): PortalRequest? {
        val header = ByteArrayOutputStream()
        val chunk = ByteArray(1024)
        while (header.size() < 32_768) {
            val count = input.read(chunk)
            if (count < 0) return null
            if (header.size() == 0 && count > 0 && chunk[0] == 0x16.toByte()) return null
            header.write(chunk, 0, count)
            val raw = header.toByteArray()
            val end = headerEnd(raw) ?: continue
            val head = String(raw, 0, end.index, StandardCharsets.UTF_8)
            val parsed = parseHead(head) ?: return null
            val length = parsed.headers["content-length"]?.toIntOrNull() ?: 0
            if (length !in 0..4096) return null
            val already = raw.copyOfRange((end.index + end.separator).coerceAtMost(raw.size), raw.size)
            val body = ByteArray(length)
            val copied = minOf(already.size, length)
            if (copied > 0) System.arraycopy(already, 0, body, 0, copied)
            var filled = copied
            while (filled < length) {
                val more = input.read(body, filled, length - filled)
                if (more < 0) return null
                filled += more
            }
            return parsed.copy(body = String(body, StandardCharsets.UTF_8))
        }
        return null
    }

    fun parseHead(head: String): PortalRequest? {
        val lines = head.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        if (lines.isEmpty() || lines.size > 80) return null
        val request = lines.first().trim().split(Regex("\\s+"))
        if (request.size < 2) return null
        val method = request[0].uppercase()
        if (method != "GET" && method != "POST") return null
        var target = request[1]
        if (target.length > 2048 || target.contains("..") || target.contains('\\')) return null
        if (target.startsWith("http://") || target.startsWith("https://")) {
            val scheme = target.indexOf("://")
            val slash = target.indexOf('/', scheme + 3)
            target = if (slash < 0) "/" else target.substring(slash)
        }
        val path = target.substringBefore('?').ifBlank { "/" }
        if (!path.startsWith("/")) return null
        val query = decodePairs(if ('?' in target) target.substringAfter('?') else "")
        val headers = mutableMapOf<String, String>()
        lines.drop(1).forEach { line ->
            if (line.isBlank()) return@forEach
            val split = line.split(':', limit = 2)
            if (split.size != 2 || split[0].length > 128 || split[1].length > 4096) return@forEach
            headers.putIfAbsent(split[0].trim().lowercase(), split[1].trim())
        }
        if (headers["content-type"]?.substringBefore(';')?.trim()?.lowercase() == "multipart/form-data") return null
        return PortalRequest(method, path, query, headers, "")
    }

    private fun decodePairs(text: String): Map<String, String> {
        if (text.isBlank()) return emptyMap()
        return text.split('&').mapNotNull { part ->
            if (part.isEmpty()) return@mapNotNull null
            val split = part.split('=', limit = 2)
            urlDecode(split[0]) to urlDecode(split.getOrElse(1) { "" })
        }.toMap()
    }

    private fun urlDecode(value: String): String = try {
        URLDecoder.decode(value, StandardCharsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        value
    }

    private data class HeaderEnd(val index: Int, val separator: Int)

    private fun headerEnd(raw: ByteArray): HeaderEnd? {
        for (index in 0 until raw.size - 1) {
            if (raw[index] == 13.toByte() && index + 3 < raw.size && raw[index + 1] == 10.toByte() && raw[index + 2] == 13.toByte() && raw[index + 3] == 10.toByte()) {
                return HeaderEnd(index, 4)
            }
            if (raw[index] == 10.toByte() && raw[index + 1] == 10.toByte()) {
                return HeaderEnd(index, 2)
            }
        }
        return null
    }
}
