package com.holagundi.archiman

import android.app.Application
import android.os.StrictMode
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
    @Inject lateinit var demoStore: com.holagundi.archiman.data.DemoCompanyStore

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build(),
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build(),
            )
        }
        super.onCreate()
        scope.launch {
            // Sample data is opened explicitly from the welcome screen.
            suite.ensureDefaults()
            demoStore.ensureDemoContractors()
            com.holagundi.archiman.widget.PracticeWidgetUpdater.requestUpdate(this@ArchiManApp)
        }
    }
}
