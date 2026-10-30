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
class Migration26To27Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-26-27-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsPackageTagAndSitemanTables() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(26) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE `project_drawings` (
                              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                              `projectId` INTEGER NOT NULL,
                              `drawingNumber` TEXT NOT NULL,
                              `title` TEXT NOT NULL,
                              `discipline` TEXT NOT NULL,
                              `status` TEXT NOT NULL,
                              `createdAt` INTEGER NOT NULL,
                              `updatedAt` INTEGER NOT NULL,
                              `archivedAt` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO project_drawings(
                              projectId,drawingNumber,title,discipline,status,createdAt,updatedAt,archivedAt
                            ) VALUES (1,'A-101','Ground floor','Architectural','WORKING',100,100,NULL)
                            """.trimIndent()
                        )
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_26_27.migrate(db)

        db.query("PRAGMA table_info(project_drawings)").use { cursor ->
            val columns = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
            }
            assertTrue(columns.contains("packageTag"))
        }
        db.query("SELECT packageTag FROM project_drawings WHERE id=1").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
        }
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('site_documents','material_orders','material_order_lines')").use { cursor ->
            val tables = buildSet {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
            assertTrue(tables.containsAll(listOf("site_documents", "material_orders", "material_order_lines")))
        }
        helper.close()
    }
}
