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
class Migration6To7Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-6-7-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun migrationPreservesItemsAndAddsCodesArchiveAndAliases() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE item_master (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, workType TEXT NOT NULL, name TEXT NOT NULL, unit TEXT NOT NULL, calculationType TEXT NOT NULL, isPredefined INTEGER NOT NULL)")
                        db.execSQL("CREATE UNIQUE INDEX index_item_master_name ON item_master(name)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO item_master VALUES (42,'Masonry','Brickwork 230mm','m²','AREA',1)")

        AppDatabase.MIGRATION_6_7.migrate(db)

        db.query("SELECT id,itemCode,workType,name,isActive FROM item_master").use {
            assertTrue(it.moveToFirst())
            assertEquals(42L, it.getLong(0))
            assertEquals("WI-000042", it.getString(1))
            assertEquals("Masonry", it.getString(2))
            assertEquals("Brickwork 230mm", it.getString(3))
            assertEquals(1, it.getInt(4))
        }
        assertTrue(db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name='work_item_aliases'").use { it.moveToFirst() })
        helper.close()
    }
}
