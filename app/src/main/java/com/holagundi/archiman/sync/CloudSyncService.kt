package com.holagundi.archiman.sync

import android.content.Context
import com.holagundi.archiman.BuildConfig
import com.holagundi.archiman.data.local.ArchiManDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud login and company sync for this phone, on top of the pure pieces in this package. Every call blocks on the network or the database:
 * call it from Dispatchers.IO. The endpoints are public addresses from local.properties or the build environment (never credentials).
 */
@Singleton
class CloudSyncService @Inject constructor(
    @ApplicationContext context: Context,
    database: ArchiManDatabase,
) {
    private val store = SyncStore(RoomSqlDb(database))
    private val sessions = CloudSessionStore(PrefsKeyValues(context), KeystoreBox())
    private val http: HttpTransport = UrlConnectionTransport()
    private val auth: CloudAuth? = BuildConfig.NEON_AUTH_URL.takeIf { it.isNotBlank() }?.let { CloudAuth(it, http) }

    val configured: Boolean get() = auth != null && BuildConfig.NEON_DATA_API_URL.isNotBlank()

    fun signedInEmail(): String? = sessions.load()?.email

    fun signIn(email: String, password: String) {
        val a = auth ?: throw CloudException("Cloud sync is not configured in this build.")
        sessions.save(a.signIn(email.trim(), password).also { a.accessToken(it) }) // proves the session before it is kept
    }

    fun signOut() {
        sessions.load()?.let { auth?.signOut(it) }
        sessions.clear()
    }

    private fun api(): HubApi {
        val a = auth ?: throw CloudException("Cloud sync is not configured in this build.")
        val session = sessions.load() ?: throw CloudException("Sign in to the cloud first.", 401)
        return HubDataApi(BuildConfig.NEON_DATA_API_URL, http) { a.accessToken(session) }
    }

    fun companies(): List<CloudCompany> = api().companies()

    fun status(): SyncStatus = store.status()

    /** What joining would upload, shown before the first sync. */
    fun localCounts(): Map<String, Int> = store.localCounts()

    /** Links this phone to a cloud company the person belongs to, then runs the first sync (existing records are matched by name, never duplicated). */
    fun join(company: CloudCompany): SyncResult {
        store.enable(company.id)
        return syncNow()
    }

    fun syncNow(): SyncResult = SyncEngine(store, api()).runOnce()
}
