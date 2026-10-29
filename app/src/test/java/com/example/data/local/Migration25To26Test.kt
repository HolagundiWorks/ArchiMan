package com.example.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration25To26Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-25-26-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsAfterEvidenceUriLeavingExistingEvidenceIntact() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(25) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE `site_issues` (
                              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                              `projectId` INTEGER NOT NULL,
                              `referenceNumber` TEXT NOT NULL,
                              `type` TEXT NOT NULL,
                              `title` TEXT NOT NULL,
                              `location` TEXT NOT NULL,
                              `description` TEXT NOT NULL,
                              `severity` TEXT NOT NULL,
                              `assignedTo` TEXT NOT NULL,
                              `correctiveAction` TEXT NOT NULL,
                              `dueAt` INTEGER,
                              `status` TEXT NOT NULL,
                              `evidenceUri` TEXT,
                              `verificationNote` TEXT NOT NULL,
                              `closedAt` INTEGER,
                              `createdAt` INTEGER NOT NULL,
                              `updatedAt` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO site_issues(
                              projectId,referenceNumber,type,title,location,description,severity,assignedTo,
                              correctiveAction,dueAt,status,evidenceUri,verificationNote,closedAt,createdAt,updatedAt
                            ) VALUES(
                              1,'SNAG-1','SNAG','Crack','Wall A','Hairline crack','ATTENTION','Civil',
                              'Repair',NULL,'OPEN','file:///before.jpg','',NULL,100,100
                            )
                            """.trimIndent()
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_25_26.migrate(db)

        db.query("PRAGMA table_info(site_issues)").use { cursor ->
            val columns = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            assertTrue(columns.contains("afterEvidenceUri"))
        }
        db.query("SELECT evidenceUri, afterEvidenceUri FROM site_issues WHERE id=1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("file:///before.jpg", cursor.getString(0))
            assertTrue(cursor.isNull(1))
        }
        helper.close()
    }
}
