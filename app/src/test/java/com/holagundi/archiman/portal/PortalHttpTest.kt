package com.holagundi.archiman.portal

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalHttpTest {
    @Test
    fun addressPrefersWifiAndSkipsMobileData() {
        assertEquals(
            "192.168.1.20",
            PortalHttp.chooseAddress(listOf("rmnet0" to "10.1.1.1", "wlan0" to "192.168.1.20")),
        )
        assertNull(PortalHttp.chooseAddress(listOf("rmnet0" to "10.1.1.1")))
    }

    @Test
    fun hostMustBeThePhoneAddress() {
        assertTrue(PortalHttp.hostAllowed("192.168.1.20:8765", "192.168.1.20"))
        assertTrue(PortalHttp.hostAllowed("192.168.1.20", "192.168.1.20"))
        assertTrue(PortalHttp.hostAllowed("192.168.1.20.:8765", "192.168.1.20"))
        assertFalse(PortalHttp.hostAllowed("evil.example:8765", "192.168.1.20"))
        assertFalse(PortalHttp.hostAllowed(null, "192.168.1.20"))
        assertTrue(PortalHttp.originAllowed(null, "192.168.1.20"))
        assertTrue(PortalHttp.originAllowed("https://192.168.1.20:8765/", "192.168.1.20"))
        assertFalse(PortalHttp.originAllowed("https://evil.example:8765", "192.168.1.20"))
    }

    @Test
    fun codeMatchIgnoresCaseAndRejectsADifferentCode() {
        assertTrue(PortalHttp.codesMatch("AB23CD4E", "ab23cd4e"))
        assertFalse(PortalHttp.codesMatch("AB23CD4E", "AB23CD4F"))
        assertEquals(8, PortalHttp.newCode().length)
    }

    @Test
    fun readsABoundedFormPost() {
        val raw = "POST / HTTP/1.1\r\nHost: 192.168.1.20:8765\r\nContent-Type: application/x-www-form-urlencoded\r\nContent-Length: 11\r\n\r\ncode=AB23CD"
        val request = PortalHttp.read(ByteArrayInputStream(raw.toByteArray(StandardCharsets.UTF_8)))
        assertEquals("POST", request?.method)
        assertEquals("/", request?.path)
        assertEquals("AB23CD", PortalHttp.form(request?.body.orEmpty())["code"])
        assertEquals("AB23CD", PortalHttp.cookie("archiman_csrf=AB23CD; archiman_portal=zz", "archiman_csrf"))
    }

    @Test
    fun escapeKeepsMarkupOutOfThePage() {
        assertEquals("&lt;script&gt;", PortalHttp.escape("<script>"))
    }

    @Test
    fun readsLineFeedHeadersAndIgnoresABrokenEscape() {
        val raw = "GET /?q=%ZZ HTTP/1.1\nHost: 192.168.1.8:8765\nConnection: close\n\n"
        val request = PortalHttp.read(ByteArrayInputStream(raw.toByteArray(StandardCharsets.UTF_8)))
        assertEquals("GET", request?.method)
        assertEquals("/", request?.path)
        assertEquals("192.168.1.8:8765", request?.headers?.get("host"))
    }

    @Test
    fun multipartRestoreKeepsTheFileAndRejectsOtherPages() {
        val body = """
            --bound
            Content-Disposition: form-data; name="csrf"

            token
            --bound
            Content-Disposition: form-data; name="confirm"

            REPLACE
            --bound
            Content-Disposition: form-data; name="file"; filename="practice.archimanbackup"
            Content-Type: application/octet-stream

            ZIP
            --bound--

        """.trimIndent().replace("\n", "\r\n").toByteArray(StandardCharsets.ISO_8859_1)
        val head = "POST /transfer/restore HTTP/1.1\r\nHost: 192.168.1.8:8765\r\nContent-Type: multipart/form-data; boundary=bound\r\nContent-Length: ${body.size}\r\n\r\n"
        val request = PortalHttp.read(ByteArrayInputStream(head.toByteArray(StandardCharsets.ISO_8859_1) + body)) {
            body.size + 1024
        }
        val parts = PortalHttp.multipart(request?.headers?.get("content-type"), request!!.rawBody)
        assertEquals("token", parts["csrf"]?.text())
        assertEquals("REPLACE", parts["confirm"]?.text())
        assertEquals("practice.archimanbackup", parts["file"]?.filename)
        assertTrue(parts.getValue("file").bytes.contentEquals("ZIP".toByteArray(StandardCharsets.ISO_8859_1)))
        val elsewhere = "POST /workspace HTTP/1.1\r\nHost: 192.168.1.8:8765\r\nContent-Type: multipart/form-data; boundary=bound\r\nContent-Length: ${body.size}\r\n\r\n"
        assertNull(PortalHttp.read(ByteArrayInputStream(elsewhere.toByteArray(StandardCharsets.ISO_8859_1) + body)))
    }

    @Test
    fun multipartRejectsTooManyFieldsAndAnOversizedBody() {
        val fields = (1..9).joinToString("") { index ->
            "--bound\r\nContent-Disposition: form-data; name=\"f$index\"\r\n\r\n$index\r\n"
        } + "--bound--\r\n"
        var rejected = false
        try {
            PortalHttp.multipart("multipart/form-data; boundary=bound", fields.toByteArray(StandardCharsets.ISO_8859_1))
        } catch (error: IllegalArgumentException) {
            rejected = error.message == "Too many upload fields."
        }
        assertTrue(rejected)
        val huge = "POST /transfer/restore HTTP/1.1\r\nHost: 192.168.1.8:8765\r\nContent-Type: multipart/form-data; boundary=bound\r\nContent-Length: 9000\r\n\r\n"
        assertNull(PortalHttp.read(ByteArrayInputStream(huge.toByteArray(StandardCharsets.ISO_8859_1))) { 1024 })
    }

    @Test
    fun transferUnlocksWithTheAccessCodeOnlyWhenNoAdminExists() {
        assertTrue(PortalTransfer.codeOnly(codeOk = true, adminCount = 0))
        assertFalse(PortalTransfer.codeOnly(codeOk = true, adminCount = 1))
        assertFalse(PortalTransfer.codeOnly(codeOk = false, adminCount = 0))
        assertTrue(PortalTransfer.allowed("Admin"))
        assertTrue(PortalTransfer.allowed(PortalTransfer.ROLE))
        assertFalse(PortalTransfer.allowed("Staff"))
        assertFalse(PortalTransfer.allowed(null))
        var rejected = false
        try {
            PortalTransfer.open(byteArrayOf(1, 2, 3), "", "restore")
        } catch (error: IllegalArgumentException) {
            rejected = true
        }
        assertTrue(rejected)
    }

    @Test
    fun readsAnAbsoluteBrowserTarget() {
        val raw = "GET http://192.168.1.8:8765/project?id=15 HTTP/1.1\r\nHost: 192.168.1.8:8765\r\n\r\n"
        val request = PortalHttp.read(ByteArrayInputStream(raw.toByteArray(StandardCharsets.UTF_8)))
        assertEquals("/project", request?.path)
        assertEquals("15", request?.query?.get("id"))
    }
}
