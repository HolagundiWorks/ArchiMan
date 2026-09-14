package com.example.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration24To25Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-24-25-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsRevisionSourceAndSeverityDefaultingExistingRows() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(24) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE `drawing_revisions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `drawingId` INTEGER NOT NULL, `revisionCode` TEXT NOT NULL, `fileName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `fileUri` TEXT NOT NULL, `fileChecksum` TEXT NOT NULL, `issueStatus` TEXT NOT NULL, `revisionNotes` TEXT NOT NULL, `isAsBuilt` INTEGER NOT NULL, `issuedAt` INTEGER, `createdAt` INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO drawing_revisions(projectId,drawingId,revisionCode,fileName,mimeType,fileUri,fileChecksum,issueStatus,revisionNotes,isAsBuilt,issuedAt,createdAt) VALUES(1,1,'A','d.dwg','application/acad','file:///d.dwg','','ISSUED','',0,NULL,100)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_24_25.migrate(db)

        db.query("SELECT revisionSource, severity FROM drawing_revisions WHERE id=1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
            assertEquals("NORMAL", cursor.getString(1))
        }
        helper.close()
    }
}
