package com.example.aorms

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets

class AormsIdentityClientTest {
    private var server: HttpServer? = null

    @After fun tearDown() { server?.stop(0) }

    private fun startFakeAorms(status: Int, body: String, expectedAuthHeader: String? = null): String {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/platform/v1/verify-login") { exchange ->
            if (expectedAuthHeader != null) {
                assertEquals(expectedAuthHeader, exchange.requestHeaders.getFirst("Authorization"))
            }
            exchange.requestBody.readBytes() // drain
            val bytes = body.toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        s.start()
        server = s
        return "http://127.0.0.1:${s.address.port}"
    }

    @Test fun notConfiguredWhenServerMissing() = runBlocking {
        val client = AormsIdentityClient { null }
        val result = client.verifyLogin("a@b.com", "pw", null)
        assertTrue(result is AormsLoginResult.NotConfigured)
    }

    @Test fun notConfiguredWhenBaseUrlBlank() = runBlocking {
        val client = AormsIdentityClient { AormsServerConfig(baseUrl = "", productApiKey = "key") }
        val result = client.verifyLogin("a@b.com", "pw", null)
        assertTrue(result is AormsLoginResult.NotConfigured)
    }

    @Test fun successParsesIdentityAndSendsBearerKey() = runBlocking {
        val baseUrl = startFakeAorms(
            200,
            """{"ok":true,"account":{"publicId":"AORMS-U-ABC123","email":"person@firm.com","name":"Person"},"role":"OWNER"}""",
            expectedAuthHeader = "Bearer test-key"
        )
        val client = AormsIdentityClient { AormsServerConfig(baseUrl, "test-key") }
        val result = client.verifyLogin("person@firm.com", "correct-password", "firm.com")
        val success = result as AormsLoginResult.Success
        assertEquals("AORMS-U-ABC123", success.identity.publicId)
        assertEquals("person@firm.com", success.identity.email)
        assertEquals("OWNER", success.identity.role)
    }

    @Test fun invalidCredentialsMapsTo401() = runBlocking {
        val baseUrl = startFakeAorms(401, """{"error":"invalid_credentials"}""")
        val client = AormsIdentityClient { AormsServerConfig(baseUrl, "test-key") }
        val result = client.verifyLogin("person@firm.com", "wrong-password", null)
        assertTrue(result is AormsLoginResult.InvalidCredentials)
    }

    @Test fun notAMemberMapsTo403() = runBlocking {
        val baseUrl = startFakeAorms(403, """{"error":"not_a_member"}""")
        val client = AormsIdentityClient { AormsServerConfig(baseUrl, "test-key") }
        val result = client.verifyLogin("person@firm.com", "correct-password", "other-firm.com")
        assertTrue(result is AormsLoginResult.NotAMember)
    }

    @Test fun unreachableServerIsReportedDistinctly() = runBlocking {
        // Nothing listening on this loopback port.
        val client = AormsIdentityClient { AormsServerConfig("http://127.0.0.1:1", "test-key") }
        val result = client.verifyLogin("person@firm.com", "pw", null)
        assertTrue(result is AormsLoginResult.ServerUnreachable)
    }
}
