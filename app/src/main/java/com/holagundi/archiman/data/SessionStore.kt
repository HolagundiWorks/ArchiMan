package com.holagundi.archiman.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "archiman_session")

@Singleton
class SessionStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val companyReady: Flow<Boolean> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.COMPANY_READY] == true
    }

    val signedInUserId: Flow<Long?> = context.sessionDataStore.data.map { prefs ->
        prefs[Keys.SIGNED_IN_USER_ID]?.takeIf { it > 0L }
    }

    suspend fun markCompanyReady() {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.COMPANY_READY] = true
            prefs.remove(Keys.SIGNED_IN_USER_ID)
        }
    }

    suspend fun signIn(userId: Long) {
        context.sessionDataStore.edit { prefs ->
            prefs[Keys.COMPANY_READY] = true
            prefs[Keys.SIGNED_IN_USER_ID] = userId
        }
    }

    suspend fun signOut() {
        context.sessionDataStore.edit { prefs ->
            prefs.remove(Keys.SIGNED_IN_USER_ID)
        }
    }

    private object Keys {
        val COMPANY_READY = booleanPreferencesKey("company_ready")
        val SIGNED_IN_USER_ID = longPreferencesKey("signed_in_user_id")
    }
}
