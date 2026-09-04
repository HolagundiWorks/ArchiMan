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
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
class LocalPortalServerTest {
    @Test fun requiresNamedLoginAndCsrfBeforeEditing() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val captured = AtomicReference<PortalMutation?>()
        val server = LocalPortalServer(context, InetAddress.getLoopbackAddress(), 0, secureTransport = false)
        try {
            server.start(
                provider = {
                    PortalSnapshot(
                        companyName = "Test Practice",
                        projects = listOf(PortalProject(7, "Secret Project", "P-1", "Residential", "ACTIVE", "Client", "Site", "Architect", 3)),
                        selectedProject = "Secret Project", selectedProjectId = 7,
                        tasks = listOf("Inspect slab"), schedules = emptyList(), inspections = emptyList(), drawings = emptyList()
                    )
                },
                authenticate = { username, password ->
                    val accepted = username == "architect" && password.concatToString() == "correct-password"
                    password.fill('\u0000')
                    if (accepted) PortalPrincipal(11, "architect", "Lead Architect", "EDITOR") else null
                },
                mutate = { _, mutation -> captured.set(mutation); PortalMutationResult(true, "Saved") }
            )
            val state = withTimeout(5_000) { server.state.filter { it.isRunning }.first() }
            val uri = URI(state.url)

            val locked = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: ${uri.host}\r\nConnection: close\r\n\r\n")
            assertTrue(locked.startsWith("HTTP/1.1 401"))
            assertFalse(locked.contains("Secret Project"))

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
            val csrf = Regex("name=csrf value='([^']+)'").find(open)?.groupValues?.get(1).orEmpty()
            assertTrue(csrf.isNotBlank())

            val rejected = post(uri, "/mutate", "csrf=invalid&action=ADD_TASK&projectId=7&title=Site+visit", cookie)
            assertTrue(rejected.startsWith("HTTP/1.1 403"))
            assertEquals(null, captured.get())

            val edit = post(uri, "/mutate", "csrf=${encode(csrf)}&action=ADD_TASK&projectId=7&title=Site+visit&description=Review+slab", cookie)
            assertTrue(edit.startsWith("HTTP/1.1 303"))
            assertEquals("Site visit", captured.get()?.title)
            assertEquals(7L, captured.get()?.projectId)
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
