package com.holagundi.archiman.widget

import com.holagundi.archiman.data.local.ProjectDao
import com.holagundi.archiman.data.local.SuiteDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PracticeWidgetEntryPoint {
    fun suiteDao(): SuiteDao
    fun projectDao(): ProjectDao
}
