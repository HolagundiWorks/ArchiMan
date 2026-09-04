package com.example.cloud

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class SupabaseConnectionState(
    val projectUrl: String = "",
    val publishableKey: String = "",
    val isTesting: Boolean = false,
    val isConnected: Boolean = false,
    val message: String = "Not configured"
)

class SupabaseConnectionManager(context: Context) {
    private val preferences = context.getSharedPreferences("supabase_connection", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(load())
    val state: StateFlow<SupabaseConnectionState> = _state.asStateFlow()

    fun clear() {
        preferences.edit().clear().apply()
        _state.value = SupabaseConnectionState()
    }

    suspend fun saveAndTest(rawUrl: String, rawKey: String) {
        val projectUrl = normalizeUrl(rawUrl)
        val key = rawKey.trim()
        val validation = validate(projectUrl, key)
        if (validation != null) {
            _state.value = SupabaseConnectionState(projectUrl, key, message = validation)
            return
        }
        _state.value = SupabaseConnectionState(projectUrl, key, isTesting = true, message = "Testing connection…")
        val result = runCatching { testGateway(projectUrl, key) }
        if (result.isSuccess) {
            preferences.edit().putString("project_url", projectUrl).putString("publishable_key", key).apply()
            _state.value = SupabaseConnectionState(projectUrl, key, isConnected = true, message = "Connected to the Supabase API gateway")
        } else {
            _state.value = SupabaseConnectionState(projectUrl, key, message = result.exceptionOrNull()?.message ?: "Connection failed")
        }
    }

    internal fun validate(projectUrl: String, key: String): String? {
        val uri = runCatching { URI(projectUrl) }.getOrNull()
        if (uri?.scheme != "https" || uri.host.isNullOrBlank()) return "Enter a valid HTTPS Supabase project URL."
        if (key.startsWith("sb_secret_") || key.contains("service_role", ignoreCase = true)) return "Secret and service-role keys are not allowed on a phone. Use a publishable key."
        if (!key.startsWith("sb_publishable_")) return "Use the sb_publishable_ key from the Supabase Connect dialog."
        return null
    }

    private fun testGateway(projectUrl: String, key: String) {
        val connection = URL("$projectUrl/rest/v1/").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("apikey", key)
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            require(code in 200..299) { if (code == 401 || code == 403) "Supabase rejected the publishable key." else "Supabase connection failed (HTTP $code)." }
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizeUrl(value: String) = value.trim().trimEnd('/')

    private fun load(): SupabaseConnectionState {
        val url = preferences.getString("project_url", "").orEmpty()
        val key = preferences.getString("publishable_key", "").orEmpty()
        return if (url.isBlank() || key.isBlank()) SupabaseConnectionState()
        else SupabaseConnectionState(url, key, isConnected = false, message = "Saved configuration—test before use")
    }
}
