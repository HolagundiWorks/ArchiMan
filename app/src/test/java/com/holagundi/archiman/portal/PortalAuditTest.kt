package com.holagundi.archiman.portal
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.util.Random
class PortalAuditTest {
    private fun read(raw:String)=PortalHttp.read(ByteArrayInputStream(raw.toByteArray()))
    @Test fun duplicateAuthorityAndFramingRejected() {
        listOf("Host: 127.0.0.1\r\nHost: evil.test", "Host: 127.0.0.1\r\nContent-Length: 0\r\nContent-Length: 99", "Host: 127.0.0.1\r\nTransfer-Encoding: chunked", "Host: 127.0.0.1,evil.test", "Host: 127.0.0.1\r\n Content-Length: 9").forEach { assertNull(read("POST / HTTP/1.1\r\n$it\r\n\r\n")) }
    }
    @Test fun malformedLengthsAndVersionsRejected() {
        listOf("-1","+1","abc","999999999999999","4000001").forEach { assertNull(read("POST / HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: $it\r\n\r\n")) }
        assertNull(read("GET / INVALID\r\nHost: 127.0.0.1\r\n\r\n"))
        assertNull(read("GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 1\r\n\r\nx"))
    }
    @Test fun absoluteTargetMustMatchHost() { assertNull(read("GET http://evil.test/workspace HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n")) }
    @Test fun customBodyBudgetCheckedBeforeAllocation() {
        val request="POST /workspace HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 10000\r\n\r\n"
        assertNull(PortalHttp.read(ByteArrayInputStream(request.toByteArray())) {8192})
    }
    @Test fun truncatedBodyAndOversizeHeadersRejected() {
        assertNull(read("POST / HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 5\r\n\r\nx"))
        assertNull(read("GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nX-Long: "+"a".repeat(40000)+"\r\n\r\n"))
    }
    @Test fun byteFragmentedHeadersRemainValid() {
        val bytes=("GET / HTTP/1.1\r\nHost: 127.0.0.1\r\nX-Fill: "+"x".repeat(4000)+"\r\n\r\n").toByteArray()
        val input=object:FilterInputStream(ByteArrayInputStream(bytes)){override fun read(b:ByteArray,off:Int,len:Int)=super.read(b,off,minOf(1,len))}
        assertEquals("/",PortalHttp.read(input)?.path)
    }
    @Test fun randomMalformedInputsFailClosed() { val random=Random(42);repeat(1000){val data=ByteArray(random.nextInt(2048));random.nextBytes(data);assertNull(PortalHttp.read(ByteArrayInputStream(data)))} }
    @Test fun hostAndOriginChecksRejectForeignAndOpaqueOrigins() {
        assertFalse(PortalHttp.hostAllowed("127.0.0.1,evil.test","127.0.0.1"))
        assertFalse(PortalHttp.originAllowed("null","127.0.0.1"));assertFalse(PortalHttp.originAllowed("https://evil.test","127.0.0.1"))
    }
    @Test fun parameterPollutionAndParameterBombRejected() {
        assertTrue(runCatching { PortalHttp.form("action=one&action=two") }.isFailure)
        assertTrue(runCatching { PortalHttp.form((1..10000).joinToString("&"){"k$it=v"}) }.isFailure)
        assertNull(read("GET /?"+(1..200).joinToString("&"){"k$it=v"}+" HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"))
    }
    @Test fun secureOriginOnly() {
        assertTrue(PortalHttp.originAllowed("https://127.0.0.1:8765","127.0.0.1"))
        assertFalse(PortalHttp.originAllowed("http://127.0.0.1:8765","127.0.0.1"))
    }
}
