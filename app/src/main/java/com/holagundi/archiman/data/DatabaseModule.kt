package com.holagundi.archiman.data

import android.content.Context
import androidx.room.Room
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.ArchiManMigrations
import com.holagundi.archiman.sync.SqlDb
import com.holagundi.archiman.sync.SyncSchema
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
            // The sync tracking tables are not Room entities: create them on every open (idempotent), on fresh installs and after a restore.
            .addCallback(object : RoomDatabase.Callback() {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    SyncSchema.ensure(object : SqlDb {
                        override fun exec(sql: String, vararg args: Any?) = db.execSQL(sql)
                        override fun query(sql: String, vararg args: Any?) = emptyList<Map<String, Any?>>()
                        override fun lastInsertId() = 0L
                        override fun <T> transaction(block: () -> T) = block()
                    })
                }
            })
            .build()

    @Provides fun practiceDao(db: ArchiManDatabase) = db.practiceDao()
    @Provides fun projectDao(db: ArchiManDatabase) = db.projectDao()
    @Provides fun catalogDao(db: ArchiManDatabase) = db.catalogDao()
    @Provides fun measurementDao(db: ArchiManDatabase) = db.measurementDao()
    @Provides fun fieldDao(db: ArchiManDatabase) = db.fieldDao()
    @Provides fun suiteDao(db: ArchiManDatabase) = db.suiteDao()
}
