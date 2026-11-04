package com.holagundi.archiman

import android.app.Application
import android.content.pm.ApplicationInfo
import com.holagundi.archiman.data.DatabaseSeeder
import com.holagundi.archiman.data.SuiteRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ArchiManApp : Application() {
    @Inject lateinit var seeder: DatabaseSeeder
    @Inject lateinit var suite: SuiteRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (debuggable) seeder.seedIfEmpty()
            suite.ensureDefaults()
        }
    }
}
