package com.example.portal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetAddress
import java.net.Socket
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
class LocalPortalServerTest {
    @Test fun requiresNamedLoginAndCsrfBeforeEditing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val captured = AtomicReference<PortalMutation?>()
        val providerCalls = AtomicInteger(0)
        val server = LocalPortalServer(context, InetAddress.getLoopbackAddress(), 0, secureTransport = false)
        try {
            server.start(
                provider = {
                    providerCalls.incrementAndGet()
                    PortalSnapshot(
                        companyName = "Test Practice",
                        projects = listOf(PortalProject(7, "Secret Project", "P-1", "Residential", "ACTIVE", "Client", "Site", "Architect", 3)),
                        selectedProject = "Secret Project", selectedProjectId = 7,
                        tasks = listOf("Inspect slab"), schedules = emptyList(), inspections = emptyList(), drawings = emptyList(),
                        portalUsers = listOf(PortalUserOption(11, "architect", "Lead Architect", "EDITOR", true))
                    )
                },
                authenticate = { username, password ->
                    val secret = password.concatToString()
                    password.fill('\u0000')
                    when {
                        username == "architect" && secret == "correct-password" -> PortalPrincipal(11, "architect", "Lead Architect", "EDITOR")
                        username == "admin" && secret == "correct-password" -> PortalPrincipal(10, "admin", "Practice Admin", "ADMIN")
                        username == "viewer" && secret == "correct-password" -> PortalPrincipal(12, "viewer", "Site Viewer", "VIEWER")
                        else -> null
                    }
                },
                mutate = { _, mutation -> captured.set(mutation); PortalMutationResult(true, "Saved") }
            )
            val state = withTimeout(5_000) { server.state.filter { it.isRunning }.first() }
            val uri = URI(state.url)

            val locked = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: ${uri.host}\r\nConnection: close\r\n\r\n")
            assertTrue(locked.startsWith("HTTP/1.1 401"))
            assertFalse(locked.contains("Secret Project"))
            assertTrue(locked.contains("Permissions-Policy: camera=(), microphone=(), geolocation=(), usb=()"))
            assertTrue(locked.contains("Cross-Origin-Opener-Policy: same-origin"))
            assertTrue(locked.contains("Cross-Origin-Resource-Policy: same-origin"))

            val spoofedHost = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: attacker.invalid\r\nConnection: close\r\n\r\n")
            assertTrue(spoofedHost.startsWith("HTTP/1.1 400"))
            val crossOrigin = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: ${uri.host}\r\nOrigin: http://attacker.invalid\r\nConnection: close\r\n\r\n")
            assertTrue(crossOrigin.startsWith("HTTP/1.1 403"))

            val badLogin = post(uri, "/login", "username=architect&password=wrong")
            assertTrue(badLogin.startsWith("HTTP/1.1 401"))

            val login = post(uri, "/login", "username=architect&password=correct-password")
            assertTrue(login.startsWith("HTTP/1.1 303"))
            val cookie = Regex("Set-Cookie: (ARCHIMANSESSION=[^;]+)").find(login)?.groupValues?.get(1).orEmpty()
            assertTrue(cookie.isNotBlank())

            val open = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $cookie\r\nConnection: close\r\n\r\n")
            assertTrue(open.startsWith("HTTP/1.1 200"))
            assertTrue(open.contains("Secret Project"))
            assertTrue(open.contains("Lead Architect"))
            assertTrue(open.contains("Primary navigation"))
            assertTrue(open.contains("Practice dashboard"))
            assertFalse(open.contains("Add measurement row"))
            val cachedOpen = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $cookie\r\nConnection: close\r\n\r\n")
            assertTrue(cachedOpen.startsWith("HTTP/1.1 200"))
            assertEquals(1, providerCalls.get())
            val csrf = Regex("name=csrf value='([^']+)'").find(open)?.groupValues?.get(1).orEmpty()
            assertTrue(csrf.isNotBlank())

            val measurements = request(uri.host, uri.port, "GET /?view=measurements&projectId=7 HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $cookie\r\nConnection: close\r\n\r\n")
            assertTrue(measurements.contains("MEASUREMENT BOOK"))
            assertTrue(measurements.contains("Add measurement row"))
            assertFalse(measurements.contains("Add task"))

            val planning = request(uri.host, uri.port, "GET /?view=planning&projectId=7 HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $cookie\r\nConnection: close\r\n\r\n")
            assertTrue(planning.contains("Formal decisions"))
            assertTrue(planning.contains("Add formal decision"))
            val site = request(uri.host, uri.port, "GET /?view=site&projectId=7 HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $cookie\r\nConnection: close\r\n\r\n")
            assertTrue(site.contains("Daily reports"))
            assertTrue(site.contains("Add daily site report"))
            assertTrue(site.contains("Add snag / NCR"))

            val rejected = post(uri, "/mutate", "csrf=invalid&action=ADD_TASK&projectId=7&title=Site+visit", cookie)
            assertTrue(rejected.startsWith("HTTP/1.1 403"))
            assertEquals(null, captured.get())

            val editorAdminAction = post(uri, "/mutate", "csrf=${encode(csrf)}&action=CREATE_PORTAL_USER&projectId=0&title=New+User&username=newuser&password=temporary-password&role=VIEWER", cookie)
            assertTrue(editorAdminAction.startsWith("HTTP/1.1 403"))
            assertEquals(null, captured.get())

            val edit = post(uri, "/mutate", "csrf=${encode(csrf)}&action=ADD_TASK&projectId=7&title=Site+visit&description=Review+slab&dueAt=2026-09-30", cookie)
            assertTrue(edit.startsWith("HTTP/1.1 303"))
            assertEquals("Site visit", captured.get()?.title)
            assertEquals(7L, captured.get()?.projectId)
            assertEquals("2026-09-30", captured.get()?.fields?.get("dueAt"))

            val addIssue = post(uri, "/mutate", "csrf=${encode(csrf)}&action=ADD_SITE_ISSUE&projectId=7&title=SN-014&description=Honeycomb+at+column&issueType=SNAG&issueTitle=Column+surface&severity=ATTENTION", cookie)
            assertTrue(addIssue.startsWith("HTTP/1.1 303"))
            assertEquals("ADD_SITE_ISSUE", captured.get()?.action)
            assertEquals("SN-014", captured.get()?.title)
            assertEquals("Column surface", captured.get()?.fields?.get("issueTitle"))

            captured.set(null)
            val adminLogin = post(uri, "/login", "username=admin&password=correct-password")
            val adminCookie = Regex("Set-Cookie: (ARCHIMANSESSION=[^;]+)").find(adminLogin)?.groupValues?.get(1).orEmpty()
            val adminPage = request(uri.host, uri.port, "GET /?view=admin HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $adminCookie\r\nConnection: close\r\n\r\n")
            assertTrue(adminPage.contains("Practice administration"))
            assertTrue(adminPage.contains("Manage Lead Architect"))
            val adminCsrf = Regex("name=csrf value='([^']+)'").find(adminPage)?.groupValues?.get(1).orEmpty()
            val adminAction = post(uri, "/mutate", "csrf=${encode(adminCsrf)}&action=CREATE_PORTAL_USER&projectId=0&title=New+User&username=newuser&password=temporary-password&role=VIEWER", adminCookie)
            assertTrue(adminAction.startsWith("HTTP/1.1 303"))
            assertEquals("CREATE_PORTAL_USER", captured.get()?.action)

            captured.set(null)
            val viewerLogin = post(uri, "/login", "username=viewer&password=correct-password")
            val viewerCookie = Regex("Set-Cookie: (ARCHIMANSESSION=[^;]+)").find(viewerLogin)?.groupValues?.get(1).orEmpty()
            val viewerPage = request(uri.host, uri.port, "GET /?projectId=7 HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $viewerCookie\r\nConnection: close\r\n\r\n")
            assertTrue(viewerPage.contains("Viewer access"))
            assertFalse(viewerPage.contains("Add project"))
            assertFalse(viewerPage.contains("Administration"))
            assertFalse(viewerPage.contains("Add measurement row"))
            val viewerCsrf = Regex("name=csrf value='([^']+)'").find(viewerPage)?.groupValues?.get(1).orEmpty()
            val viewerEdit = post(uri, "/mutate", "csrf=${encode(viewerCsrf)}&action=ADD_TASK&projectId=7&title=Blocked", viewerCookie)
            assertTrue(viewerEdit.startsWith("HTTP/1.1 403"))
            assertEquals(null, captured.get())
            val logout = post(uri, "/logout", "csrf=${encode(viewerCsrf)}", viewerCookie)
            assertTrue(logout.startsWith("HTTP/1.1 303"))
            assertTrue(logout.contains("Clear-Site-Data: \"cache\", \"cookies\", \"storage\""))
        } finally {
            server.close()
        }
    }

    private fun post(uri: URI, path: String, body: String, cookie: String = ""): String = request(
        uri.host, uri.port,
        "POST $path HTTP/1.1\r\nHost: ${uri.host}\r\n${if (cookie.isBlank()) "" else "Cookie: $cookie\r\n"}Content-Type: application/x-www-form-urlencoded\r\nContent-Length: ${body.toByteArray().size}\r\nConnection: close\r\n\r\n$body"
    )

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun request(host: String, port: Int, request: String): String = Socket(host, port).use { socket ->
        socket.soTimeout = 5_000
        socket.getOutputStream().write(request.toByteArray(StandardCharsets.UTF_8))
        socket.getOutputStream().flush()
        socket.getInputStream().bufferedReader().readText()
    }
}
