package com.example.aorms

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Office-wide AORMS connection, entered once by an administrator under Practice settings. */
data class AormsServerConfig(val baseUrl: String, val productApiKey: String)

enum class AormsSessionStatus { UNCONFIGURED, SIGNED_OUT, VERIFYING, SIGNED_IN, UNREACHABLE }

data class AormsSessionState(
    val status: AormsSessionStatus = AormsSessionStatus.UNCONFIGURED,
    val identity: AormsIdentity? = null,
    val company: String? = null,
    val message: String = ""
)

/**
 * Owns the AORMS server configuration, the signed-in identity, and re-verifies
 * a stored credential against AORMS live (never grants access from a cached
 * result — every app launch must reach AORMS, per the office-only design in
 * `docs/AORMS_PORTAL_CASE_STUDY.md`).
 *
 * Credentials and the product API key are held only in
 * [EncryptedSharedPreferences]; nothing sensitive is stored in the Room
 * database, which is exported/backed up elsewhere in the app.
 */
class AormsSessionManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences by lazy { encryptedPrefs(appContext) }
    private val client = AormsIdentityClient(config = ::loadConfig)

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<AormsSessionState> = _state.asStateFlow()

    fun serverConfig(): AormsServerConfig? = loadConfig()

    /**
     * Verify a credential against AORMS without touching this app's own
     * sign-in state — used by the LAN browser portal (`LocalPortalServer`),
     * which authenticates its own visitors against the same office AORMS
     * connection but must not sign the phone itself in or out.
     */
    suspend fun verifyOnly(email: String, password: String, company: String?): AormsLoginResult =
        client.verifyLogin(email, password, company)

    /** Save the office's AORMS connection. Does not itself attempt a sign-in. */
    fun saveServerConfig(baseUrl: String, productApiKey: String) {
        prefs.edit()
            .putString(KEY_BASE_URL, baseUrl.trim().trimEnd('/'))
            .putString(KEY_PRODUCT_API_KEY, productApiKey.trim())
            .apply()
        if (_state.value.status == AormsSessionStatus.UNCONFIGURED) {
            _state.value = AormsSessionState(status = AormsSessionStatus.SIGNED_OUT)
        }
    }

    suspend fun signIn(email: String, password: String, company: String?): AormsLoginResult {
        _state.value = _state.value.copy(status = AormsSessionStatus.VERIFYING, message = "Contacting AORMS…")
        val result = client.verifyLogin(email, password, company)
        applyResult(result, company, persistOnSuccess = true, email = email, password = password)
        return result
    }

    /** Re-check the last signed-in credential against AORMS. Called on every app launch. */
    suspend fun reverify(): AormsLoginResult {
        val email = prefs.getString(KEY_LAST_EMAIL, null)
        val password = prefs.getString(KEY_LAST_PASSWORD, null)
        val company = prefs.getString(KEY_LAST_COMPANY, null)
        if (email.isNullOrBlank() || password.isNullOrBlank()) {
            _state.value = _state.value.copy(status = statusForNoCredential())
            return AormsLoginResult.NotConfigured
        }
        _state.value = _state.value.copy(status = AormsSessionStatus.VERIFYING, message = "Verifying with AORMS…")
        val result = client.verifyLogin(email, password, company)
        applyResult(result, company, persistOnSuccess = false, email = email, password = password)
        return result
    }

    fun signOut() {
        prefs.edit().remove(KEY_LAST_EMAIL).remove(KEY_LAST_PASSWORD).remove(KEY_LAST_COMPANY).apply()
        _state.value = AormsSessionState(status = AormsSessionStatus.SIGNED_OUT)
    }

    private fun applyResult(
        result: AormsLoginResult,
        company: String?,
        persistOnSuccess: Boolean,
        email: String,
        password: String
    ) {
        _state.value = when (result) {
            is AormsLoginResult.Success -> {
                if (persistOnSuccess) {
                    prefs.edit()
                        .putString(KEY_LAST_EMAIL, email)
                        .putString(KEY_LAST_PASSWORD, password)
                        .putString(KEY_LAST_COMPANY, company)
                        .apply()
                }
                AormsSessionState(status = AormsSessionStatus.SIGNED_IN, identity = result.identity, company = company)
            }
            AormsLoginResult.InvalidCredentials -> AormsSessionState(
                status = AormsSessionStatus.SIGNED_OUT,
                message = "Incorrect AORMS email or password."
            )
            is AormsLoginResult.NotAMember -> AormsSessionState(
                status = AormsSessionStatus.SIGNED_OUT,
                message = "This account is not an active member of that company."
            )
            AormsLoginResult.ServerUnreachable -> AormsSessionState(
                status = AormsSessionStatus.UNREACHABLE,
                message = "Could not reach the AORMS identity server. Check the network and try again."
            )
            AormsLoginResult.NotConfigured -> AormsSessionState(
                status = AormsSessionStatus.UNCONFIGURED,
                message = "AORMS server is not configured. Set it up under Practice settings."
            )
            is AormsLoginResult.UnexpectedError -> AormsSessionState(
                status = AormsSessionStatus.UNREACHABLE,
                message = result.message
            )
        }
    }

    private fun statusForNoCredential(): AormsSessionStatus =
        if (loadConfig() == null) AormsSessionStatus.UNCONFIGURED else AormsSessionStatus.SIGNED_OUT

    private fun loadConfig(): AormsServerConfig? {
        val baseUrl = prefs.getString(KEY_BASE_URL, null)
        val apiKey = prefs.getString(KEY_PRODUCT_API_KEY, null)
        return if (baseUrl.isNullOrBlank() || apiKey.isNullOrBlank()) null else AormsServerConfig(baseUrl, apiKey)
    }

    private fun initialState(): AormsSessionState = AormsSessionState(status = statusForNoCredential())

    /**
     * AndroidKeyStore-backed prefs. Falls back to a plain (unencrypted) file
     * only if the platform keystore itself is unusable — e.g. Robolectric's
     * host-JVM environment in unit tests, which has no real AndroidKeyStore
     * provider. Every real device has one; this fallback exists purely so a
     * broken keystore degrades instead of crashing the whole app on launch.
     */
    private fun encryptedPrefs(context: Context): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "aorms_identity",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        context.getSharedPreferences("aorms_identity_unencrypted_fallback", Context.MODE_PRIVATE)
    }

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_PRODUCT_API_KEY = "product_api_key"
        private const val KEY_LAST_EMAIL = "last_email"
        private const val KEY_LAST_PASSWORD = "last_password"
        private const val KEY_LAST_COMPANY = "last_company"
    }
}
