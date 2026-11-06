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
    val rawBody: ByteArray = ByteArray(0),
)

data class PortalPart(val filename: String?, val bytes: ByteArray) {
    fun text(): String = bytes.toString(StandardCharsets.UTF_8)
}

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
        val host = normalizeHost(hostHeader) ?: return false
        val expected = normalizeHost(address) ?: return false
        return host == expected || host == "$expected:$port"
    }

    fun originAllowed(origin: String?, address: String, port: Int = PORT): Boolean {
        if (origin.isNullOrBlank() || origin.equals("null", ignoreCase = true)) return true
        val expected = "https://${normalizeHost(address)}:$port"
        return origin.trim().trimEnd('/').equals(expected, ignoreCase = true)
    }

    /** Strip whitespace, trailing DNS dots, and IPv6 brackets so browser Host headers match the bound Wi‑Fi IP. */
    fun normalizeHost(value: String?): String? {
        val raw = value?.trim()?.lowercase().orEmpty()
        if (raw.isEmpty() || raw.contains('@') || raw.contains(',')) return null
        val withoutBrackets = if (raw.startsWith('[')) {
            val end = raw.indexOf(']')
            if (end <= 1) return null
            val ip = raw.substring(1, end).trimEnd('.')
            val rest = raw.substring(end + 1)
            if (rest.isEmpty()) ip else if (rest.startsWith(':')) "$ip$rest" else return null
        } else raw
        val hostPart = withoutBrackets.substringBefore(':').trimEnd('.')
        val portPart = withoutBrackets.substringAfter(':', missingDelimiterValue = "")
        return if (portPart.isEmpty()) hostPart else "$hostPart:$portPart"
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

    fun read(input: InputStream, bodyLimit: (PortalRequest) -> Int = { 4_000_000 }): PortalRequest? {
        val stream=java.io.BufferedInputStream(input,8192)
        val header=ByteArrayOutputStream(); var previous=0; var beforePrevious=0; var thirdPrevious=0; var separator=0
        while(header.size()<32_768) {
            val byte=stream.read(); if(byte<0) return null
            if(header.size()==0 && byte==0x16) return null
            header.write(byte)
            if(thirdPrevious==13 && beforePrevious==10 && previous==13 && byte==10) { separator=4;break }
            if(previous==10 && byte==10) { separator=2;break }
            thirdPrevious=beforePrevious; beforePrevious=previous; previous=byte
        }
        if(separator==0) return null
        val bytes=header.toByteArray()
        val parsed=parseHead(String(bytes,0,bytes.size-separator,StandardCharsets.UTF_8)) ?: return null
        val lengthText=parsed.headers["content-length"]
        if(lengthText!=null && !lengthText.matches(Regex("[0-9]{1,9}"))) return null
        val length=lengthText?.toIntOrNull() ?: 0
        val cap = if (parsed.path == "/transfer/restore") 65 * 1024 * 1024 else 4_000_000
        if(length !in 0..bodyLimit(parsed).coerceIn(0, cap) || (parsed.method=="GET" && length>0)) return null
        val raw=ByteArray(length); var offset=0
        while(offset<length) { val count=stream.read(raw,offset,length-offset); if(count<0) return null;offset+=count }
        val multipart = parsed.headers["content-type"]?.substringBefore(';')?.trim()?.equals("multipart/form-data", true) == true
        return if (multipart) parsed.copy(body = "", rawBody = raw) else parsed.copy(body = String(raw, StandardCharsets.UTF_8))
    }

    fun parseHead(head: String): PortalRequest? {
        val lines = head.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        if (lines.isEmpty() || lines.size > 80) return null
        val request = lines.first().trim().split(Regex("\\s+"))
        if (request.size != 3 || request[2] !in listOf("HTTP/1.0","HTTP/1.1")) return null
        val method = request[0].uppercase()
        if (method != "GET" && method != "POST") return null
        var target = request[1]
        var authority: String? = null
        if (target.length > 2048 || target.contains("..") || target.contains('\\')) return null
        if (target.startsWith("http://") || target.startsWith("https://")) {
            val uri=runCatching { java.net.URI(target) }.getOrNull() ?: return null
            authority=uri.rawAuthority
            if(uri.userInfo!=null || uri.fragment!=null) return null
            target=uri.rawPath.orEmpty().ifBlank { "/" } + (uri.rawQuery?.let { "?$it" } ?: "")
        }
        val path = target.substringBefore('?').ifBlank { "/" }
        if (!path.startsWith("/")) return null
        val query = runCatching { decodePairs(if ('?' in target) target.substringAfter('?') else "") }.getOrNull() ?: return null
        val headers = mutableMapOf<String, String>()
        for(line in lines.drop(1)) {
            if(line.isBlank()) continue
            val split=line.split(':',limit=2)
            if(split.size!=2 || !split[0].matches(Regex("[!#$%&'*+.^_`|~0-9A-Za-z-]{1,128}")) || split[1].length>4096) return null
            val name=split[0].lowercase(); val value=split[1].trim()
            if(value.any { it.code<32 && it!='\t' } || name in headers) return null
            headers[name]=value
        }
        if(headers["host"].isNullOrBlank() || headers["host"]!!.contains(',') || headers.containsKey("transfer-encoding")) return null
        if(authority!=null && !authority.equals(headers["host"],ignoreCase=true)) return null
        val multipart = headers["content-type"]?.substringBefore(';')?.trim()?.equals("multipart/form-data", true) == true
        if (multipart && path != "/transfer/restore") return null
        return PortalRequest(method, path, query, headers, "")
    }

    fun multipart(contentType: String?, raw: ByteArray): Map<String, PortalPart> {
        val boundary = Regex("""boundary=(?:"([^"]+)"|([^;\s]+))""", RegexOption.IGNORE_CASE)
            .find(contentType.orEmpty())?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }
            ?: throw IllegalArgumentException("Choose the backup file again.")
        require(boundary.length in 1..70 && boundary.none { it.code < 32 }) { "Invalid upload." }
        val marker = "--$boundary".toByteArray(StandardCharsets.ISO_8859_1)
        val parts = linkedMapOf<String, PortalPart>()
        var cursor = indexOf(raw, marker, 0)
        require(cursor >= 0) { "The upload was incomplete." }
        var count = 0
        while (cursor >= 0) {
            var start = cursor + marker.size
            if (start + 1 < raw.size && raw[start] == '-'.code.toByte() && raw[start + 1] == '-'.code.toByte()) break
            if (count >= 8) throw IllegalArgumentException("Too many upload fields.")
            if (start < raw.size && raw[start] == '\r'.code.toByte()) start++
            if (start < raw.size && raw[start] == '\n'.code.toByte()) start++
            val next = indexOf(raw, marker, start)
            val end = if (next < 0) raw.size else {
                var cut = next
                if (cut >= 2 && raw[cut - 2] == '\r'.code.toByte() && raw[cut - 1] == '\n'.code.toByte()) cut -= 2
                else if (cut >= 1 && raw[cut - 1] == '\n'.code.toByte()) cut -= 1
                cut
            }
            val headerEnd = indexOf(raw, "\r\n\r\n".toByteArray(StandardCharsets.ISO_8859_1), start).let { crlf ->
                if (crlf in start until end) crlf to 4 else indexOf(raw, "\n\n".toByteArray(StandardCharsets.ISO_8859_1), start).let { lf ->
                    if (lf in start until end) lf to 2 else -1 to 0
                }
            }
            require(headerEnd.first >= start) { "The upload was incomplete." }
            val header = String(raw, start, headerEnd.first - start, StandardCharsets.ISO_8859_1)
            val disposition = header.lineSequence().firstOrNull { it.startsWith("Content-Disposition:", true) }.orEmpty()
            val name = Regex("""name="([^"]{1,128})"""").find(disposition)?.groupValues?.get(1)
                ?: throw IllegalArgumentException("Invalid upload field.")
            require(name !in parts) { "Duplicate upload field." }
            val filename = Regex("""filename="([^"]*)"""").find(disposition)?.groupValues?.get(1)
            val dataStart = headerEnd.first + headerEnd.second
            parts[name] = PortalPart(filename, raw.copyOfRange(dataStart, end))
            count++
            cursor = next
        }
        require(parts.isNotEmpty()) { "The upload was empty." }
        return parts
    }

    private fun indexOf(data: ByteArray, needle: ByteArray, from: Int): Int {
        if (needle.isEmpty() || from > data.size - needle.size) return -1
        for (index in from..data.size - needle.size) {
            var match = true
            for (offset in needle.indices) if (data[index + offset] != needle[offset]) { match = false; break }
            if (match) return index
        }
        return -1
    }

    private fun decodePairs(text: String): Map<String, String> {
        if (text.isBlank()) return emptyMap()
        val parts=text.split('&',limit=130)
        require(parts.size<=128) { "Too many form parameters." }
        val result=linkedMapOf<String,String>()
        parts.forEach { part ->
            if(part.isEmpty()) return@forEach
            val split=part.split('=',limit=2); val key=urlDecode(split[0])
            require(key.length in 1..128 && key !in result) { "Duplicate or invalid form field." }
            result[key]=urlDecode(split.getOrElse(1) { "" })
        }
        return result
    }

    private fun urlDecode(value: String): String = try {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    } catch (_: IllegalArgumentException) {
        value
    }

}
