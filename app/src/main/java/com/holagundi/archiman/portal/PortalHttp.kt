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
        val host = hostHeader?.trim()?.lowercase().orEmpty()
        if (host.isEmpty() || host.contains('@')) return false
        val expected = address.lowercase()
        return host == expected || host == "$expected:$port"
    }

    fun originAllowed(origin: String?, address: String, port: Int = PORT): Boolean {
        if (origin.isNullOrBlank()) return true
        return origin.trim() == "https://${address.lowercase()}:$port" || origin.trim() == "https://$address:$port"
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
        if(length !in 0..bodyLimit(parsed).coerceIn(0,4_000_000) || (parsed.method=="GET" && length>0)) return null
        val body=ByteArray(length); var offset=0
        while(offset<length) { val count=stream.read(body,offset,length-offset); if(count<0) return null;offset+=count }
        return parsed.copy(body=String(body,StandardCharsets.UTF_8))
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
        if (headers["content-type"]?.substringBefore(';')?.trim()?.lowercase() == "multipart/form-data") return null
        return PortalRequest(method, path, query, headers, "")
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
