package com.example.aorms

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * The AORMS-U portable identity plus the person's role in the named company,
 * as returned by `POST /platform/v1/verify-login`.
 */
data class AormsIdentity(
    val publicId: String?,
    val email: String,
    val name: String?,
    /** The person's ACTIVE role in the company that was verified against, or null. */
    val role: String?
)

sealed class AormsLoginResult {
    data class Success(val identity: AormsIdentity) : AormsLoginResult()
    object InvalidCredentials : AormsLoginResult()
    data class NotAMember(val reason: String) : AormsLoginResult()
    object ServerUnreachable : AormsLoginResult()
    object NotConfigured : AormsLoginResult()
    data class UnexpectedError(val message: String) : AormsLoginResult()
}

@JsonClass(generateAdapter = true)
internal data class VerifyLoginRequest(val email: String, val password: String, val company: String?)

@JsonClass(generateAdapter = true)
internal data class VerifyLoginAccount(val publicId: String?, val email: String, val name: String?)

@JsonClass(generateAdapter = true)
internal data class VerifyLoginResponse(
    val ok: Boolean?,
    val account: VerifyLoginAccount?,
    val role: String?,
    val error: String?
)

/**
 * Machine client for AORMS's Product License API (paths under `/platform/v1/`) — the
 * "product node delegates firm auth to the platform" contract documented at
 * `backend/src/licensing-platform/routes/v1.ts`. Every call is authenticated
 * with a per-product API key issued to ArchiMan in AORMS's licensing console
 * (see `docs/AORMS_PORTAL_CASE_STUDY.md`); it is never bundled into the app —
 * an office administrator enters it once under Practice settings.
 */
class AormsIdentityClient(
    private val config: () -> AormsServerConfig?
) {
    private val moshi = Moshi.Builder().build()
    private val requestAdapter = moshi.adapter(VerifyLoginRequest::class.java)
    private val responseAdapter = moshi.adapter(VerifyLoginResponse::class.java)

    /**
     * Verify [email]/[password] against AORMS, optionally scoped to [company]
     * (an AORMS-C handle, login domain, or slug — free text, exactly what a
     * person would type). Never throws; every failure mode is a result.
     */
    suspend fun verifyLogin(email: String, password: String, company: String?): AormsLoginResult {
        val cfg = config() ?: return AormsLoginResult.NotConfigured
        if (cfg.baseUrl.isBlank() || cfg.productApiKey.isBlank()) return AormsLoginResult.NotConfigured

        val requestBody = requestAdapter.toJson(
            VerifyLoginRequest(email = email.trim(), password = password, company = company?.trim()?.ifBlank { null })
        )

        return try {
            val (status, body) = post("${cfg.baseUrl.trimEnd('/')}/platform/v1/verify-login", cfg.productApiKey, requestBody)
            val parsed = runCatching { responseAdapter.fromJson(body) }.getOrNull()
                ?: return AormsLoginResult.UnexpectedError("AORMS returned an unexpected response.")
            when {
                status == 200 && parsed.ok == true && parsed.account != null -> AormsLoginResult.Success(
                    AormsIdentity(
                        publicId = parsed.account.publicId,
                        email = parsed.account.email,
                        name = parsed.account.name,
                        role = parsed.role
                    )
                )
                status == 401 && parsed.error == "invalid_credentials" -> AormsLoginResult.InvalidCredentials
                status == 401 -> AormsLoginResult.UnexpectedError("AORMS rejected the product API key. Check the key under Practice settings.")
                status == 403 || status == 404 -> AormsLoginResult.NotAMember(parsed.error ?: "not_a_member")
                else -> AormsLoginResult.UnexpectedError(parsed.error ?: "AORMS returned HTTP $status.")
            }
        } catch (e: SocketTimeoutException) {
            AormsLoginResult.ServerUnreachable
        } catch (e: IOException) {
            AormsLoginResult.ServerUnreachable
        } catch (e: Exception) {
            AormsLoginResult.UnexpectedError(e.message ?: "Unknown error contacting AORMS.")
        }
    }

    private fun post(url: String, apiKey: String, jsonBody: String): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.outputStream.use { it.write(jsonBody.toByteArray(StandardCharsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: "{}"
            return status to text
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }
}
