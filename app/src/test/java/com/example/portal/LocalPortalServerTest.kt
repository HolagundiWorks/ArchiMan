package com.example.portal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetAddress
import java.net.Socket
import java.net.URI
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class LocalPortalServerTest {
    @Test fun requiresPinBeforeReturningProjectData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val server = LocalPortalServer(context, InetAddress.getLoopbackAddress(), 0)
        try {
            server.start {
                PortalSnapshot(
                    companyName = "Test Practice",
                    projects = listOf(PortalProject("Secret Project", "P-1", "Residential", "ACTIVE", "Client", "Site", "Architect", 3)),
                    selectedProject = "Secret Project",
                    tasks = listOf("Inspect slab"), schedules = emptyList(), inspections = emptyList(), drawings = emptyList()
                )
            }
            val state = withTimeout(5_000) { server.state.filter { it.isRunning }.first() }
            val uri = URI(state.url)

            val locked = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: ${uri.host}\r\nConnection: close\r\n\r\n")
            assertTrue(locked.startsWith("HTTP/1.1 401"))
            assertFalse(locked.contains("Secret Project"))

            val loginBody = "pin=${state.pin}"
            val login = request(uri.host, uri.port, "POST /login HTTP/1.1\r\nHost: ${uri.host}\r\nContent-Length: ${loginBody.length}\r\nConnection: close\r\n\r\n$loginBody")
            assertTrue(login.startsWith("HTTP/1.1 303"))
            val cookie = Regex("Set-Cookie: (AMBSESSION=[^;]+)").find(login)?.groupValues?.get(1).orEmpty()
            assertTrue(cookie.isNotBlank())

            val open = request(uri.host, uri.port, "GET / HTTP/1.1\r\nHost: ${uri.host}\r\nCookie: $cookie\r\nConnection: close\r\n\r\n")
            assertTrue(open.startsWith("HTTP/1.1 200"))
            assertTrue(open.contains("Secret Project"))
            assertTrue(open.contains("Local read-only portal"))
        } finally {
            server.close()
        }
    }

    private fun request(host: String, port: Int, request: String): String = Socket(host, port).use { socket ->
        socket.soTimeout = 5_000
        socket.getOutputStream().write(request.toByteArray(StandardCharsets.UTF_8))
        socket.getOutputStream().flush()
        socket.getInputStream().bufferedReader().readText()
    }
}
