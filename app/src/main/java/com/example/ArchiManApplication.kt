package com.example

import android.app.Application
import com.example.company.CompanyDatabaseRestoreCoordinator

class ArchiManApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CompanyDatabaseRestoreCoordinator.applyPendingRestore(this)
    }
}
