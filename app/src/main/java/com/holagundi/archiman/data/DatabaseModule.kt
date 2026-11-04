package com.holagundi.archiman.data

import android.content.Context
import androidx.room.Room
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.ArchiManMigrations
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ArchiManDatabase =
        Room.databaseBuilder(context, ArchiManDatabase::class.java, "archiman.db")
            .addMigrations(*ArchiManMigrations.ALL)
            .build()

    @Provides fun practiceDao(db: ArchiManDatabase) = db.practiceDao()
    @Provides fun projectDao(db: ArchiManDatabase) = db.projectDao()
    @Provides fun catalogDao(db: ArchiManDatabase) = db.catalogDao()
    @Provides fun measurementDao(db: ArchiManDatabase) = db.measurementDao()
    @Provides fun fieldDao(db: ArchiManDatabase) = db.fieldDao()
    @Provides fun suiteDao(db: ArchiManDatabase) = db.suiteDao()
}
