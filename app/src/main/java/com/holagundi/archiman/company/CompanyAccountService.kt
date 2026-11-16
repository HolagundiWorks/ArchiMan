package com.holagundi.archiman.company

import android.content.Context
import com.holagundi.archiman.BuildConfig
import com.holagundi.archiman.sync.KeystoreBox
import com.holagundi.archiman.sync.KeyValues
import com.holagundi.archiman.sync.UrlConnectionTransport
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Credential-only storage and separate cancellable live reads. No company endpoint is compiled in. */
@Singleton
class CompanyAccountService @Inject constructor(@ApplicationContext context: Context) {
    val account = CompanyAccount(
        CompanyAccountStore(CompanyAccountPreferences(context), KeystoreBox("mynahub_company_api_session")),
        CompanyApiClient(UrlConnectionTransport(allowDevelopmentLoopback = BuildConfig.DEBUG,maxResponseBytes=8*1024*1024), BuildConfig.DEBUG, BuildConfig.VERSION_CODE),
        CompanyApiClient(UrlConnectionTransport(allowDevelopmentLoopback = BuildConfig.DEBUG,maxResponseBytes=8*1024*1024), BuildConfig.DEBUG, BuildConfig.VERSION_CODE),
    )
}

/** Auth success is reported only after the sealed token is durably saved; calls run on Dispatchers.IO. */
private class CompanyAccountPreferences(context: Context) : KeyValues {
    private val prefs = context.getSharedPreferences("company_api_account", Context.MODE_PRIVATE)
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String) {
        if (!prefs.edit().putString(key, value).commit()) throw java.io.IOException("Could not save the company account.")
    }
    override fun remove(key: String) {
        if (!prefs.edit().remove(key).commit()) throw java.io.IOException("Could not clear the company account.")
    }
}
