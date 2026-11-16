package com.holagundi.archiman

import android.app.Application
import android.os.StrictMode
import com.holagundi.archiman.data.SuiteRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class ArchiManApp : Application() {
    @Inject lateinit var suite: dagger.Lazy<SuiteRepository>
    @Inject lateinit var demoStore: dagger.Lazy<com.holagundi.archiman.data.DemoCompanyStore>

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
        if (android.os.Process.myUid() != applicationInfo.uid) return
        scope.launch {
            // Sample data is opened explicitly from the welcome screen.
            suite.get().ensureDefaults()
            demoStore.get().ensureDemoContractors()
            com.holagundi.archiman.widget.PracticeWidgetUpdater.requestUpdate(this@ArchiManApp)
        }
    }
}
