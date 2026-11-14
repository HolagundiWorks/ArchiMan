package com.holagundi.archiman.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudClientTest {
    private class Fake(val reply: (String, String, Map<String, String>, String?) -> HttpResponse) : HttpTransport {
        val seen = ArrayList<Triple<String, String, Map<String, String>>>()
        val bodies = ArrayList<String?>()
        override fun request(method: String, url: String, headers: Map<String, String>, body: String?): HttpResponse {
            seen += Triple(method, url, headers); bodies += body
            return reply(method, url, headers, body)
        }
    }

    @Test fun sign_in_returns_the_user_and_collects_every_cookie() {
        val http = Fake { _, _, _, _ ->
            HttpResponse(200, mapOf("Set-Cookie" to listOf("neon-auth.session_token=abc; Path=/; HttpOnly", "neon-auth.other=; Path=/", "x.data=1; Path=/")), """{"user":{"id":"u1","email":"a@example.test"}}""")
        }
        val s = CloudAuth("https://auth.example.test/db/auth", http).signIn("a@example.test", "pw")
        assertEquals("u1", s.userId)
        assertEquals("neon-auth.session_token=abc; x.data=1", s.cookies)
        assertEquals("https://auth.example.test/db/auth/sign-in/email", http.seen[0].second)
        assertTrue(http.seen[0].third.containsKey("Origin"))
    }

    @Test fun a_wrong_password_is_rejected_without_echoing_it() {
        val http = Fake { _, _, _, _ -> HttpResponse(401, emptyMap(), "{}") }
        val e = assertThrows(CloudException::class.java) { CloudAuth("https://a.example.test/x", http).signIn("a@example.test", "secret-pw") }
        assertTrue(e.rejected)
        assertFalse(e.message!!.contains("secret-pw"))
    }

    @Test fun the_access_token_comes_from_the_set_auth_jwt_header_and_sends_the_cookie() {
        val http = Fake { _, _, _, _ -> HttpResponse(200, mapOf("set-auth-jwt" to listOf("jwt-value")), "{}") }
        assertEquals("jwt-value", CloudAuth("https://a.example.test/x", http).accessToken(CloudSession("u", "e", "k=v")))
        assertEquals("k=v", http.seen[0].third["Cookie"])
    }

    @Test fun an_ended_session_asks_for_a_new_sign_in() {
        val http = Fake { _, _, _, _ -> HttpResponse(200, emptyMap(), "null") }
        val e = assertThrows(CloudException::class.java) { CloudAuth("https://a.example.test/x", http).accessToken(CloudSession("u", "e", "k=v")) }
        assertTrue(e.rejected)
    }

    @Test fun plain_http_addresses_are_refused() {
        assertThrows(IllegalArgumentException::class.java) { CloudAuth("http://a.example.test/x", Fake { _, _, _, _ -> HttpResponse(200, emptyMap(), "") }) }
        assertThrows(IllegalArgumentException::class.java) { HubDataApi("http://a.example.test/x", Fake { _, _, _, _ -> HttpResponse(200, emptyMap(), "") }) { "t" } }
    }

    @Test fun the_data_api_sends_the_token_and_schema_headers_and_reads_the_error_message() {
        val http = Fake { _, url, _, _ ->
            if (url.endsWith("push_records")) HttpResponse(403, emptyMap(), """{"message":"not a member of this company"}""") else HttpResponse(200, emptyMap(), "[]")
        }
        val api = HubDataApi("https://api.example.test/db/rest/v1", http) { "tok" }
        api.pull("co 1", 5, 500)
        assertEquals("Bearer tok", http.seen[0].third["Authorization"])
        assertEquals("hub", http.seen[0].third["Accept-Profile"])
        assertTrue(http.seen[0].second.contains("company_id=eq.co+1") && http.seen[0].second.contains("seq=gt.5"))
        val e = assertThrows(CloudException::class.java) { api.push("co-1", emptyList()) }
        assertEquals("not a member of this company", e.message)
        assertEquals("hub", http.seen[1].third["Content-Profile"])
    }

    @Test fun companies_are_listed_from_the_membership_filtered_table() {
        val http = Fake { _, _, _, _ -> HttpResponse(200, emptyMap(), """[{"id":"co-1","name":"Studio"}]""") }
        val list = HubDataApi("https://api.example.test/rest/v1", http) { "t" }.companies()
        assertEquals(listOf(CloudCompany("co-1", "Studio")), list)
    }

    private class MapValues : KeyValues {
        val m = HashMap<String, String>()
        override fun get(key: String) = m[key]
        override fun put(key: String, value: String) { m[key] = value }
        override fun remove(key: String) { m.remove(key) }
    }

    private object Reverse : SecretBox {
        override fun seal(plain: ByteArray) = plain.reversedArray()
        override fun open(sealed: ByteArray) = sealed.reversedArray()
    }

    @Test fun the_session_is_stored_sealed_and_cleared_on_sign_out() {
        val values = MapValues()
        val store = CloudSessionStore(values, Reverse)
        store.save(CloudSession("u1", "a@example.test", "k=SECRETCOOKIE"))
        assertFalse(values.m.values.any { it.contains("SECRETCOOKIE") })
        assertEquals("k=SECRETCOOKIE", store.load()!!.cookies)
        store.clear()
        assertNull(store.load())
    }

    @Test fun a_session_that_cannot_be_opened_is_treated_as_signed_out() {
        val values = MapValues()
        CloudSessionStore(values, Reverse).save(CloudSession("u1", "e", "k=v"))
        val broken = object : SecretBox {
            override fun seal(plain: ByteArray) = plain
            override fun open(sealed: ByteArray): ByteArray = throw java.security.GeneralSecurityException("key gone")
        }
        assertNull(CloudSessionStore(values, broken).load())
        assertEquals(JSONObject("""{"a":1}""").getInt("a"), 1)
    }
}
