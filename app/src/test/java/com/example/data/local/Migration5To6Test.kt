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
class Migration5To6Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-5-6-test.db"

    @After fun cleanUp() = context.deleteDatabase(name).let { Unit }

    @Test fun migrationMergesExactDuplicatesAndRepointsReferences() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE item_master (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, unit TEXT NOT NULL, calculationType TEXT NOT NULL, isPredefined INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE contractor_qualified_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contractorId INTEGER NOT NULL, itemName TEXT NOT NULL, uom TEXT NOT NULL, calculationType TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE component_work_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, itemId INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE measurements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, itemId INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO item_master VALUES (10,'Brickwork','m²','AREA',1),(11,' brickwork ','m²','AREA',0),(12,'Painting','m²','AREA',1)")
        db.execSQL("INSERT INTO contractor_qualified_items VALUES (20,5,'Painting','m²','AREA'),(21,5,' painting ','m²','AREA')")
        db.execSQL("INSERT INTO component_work_items VALUES (1,11)")
        db.execSQL("INSERT INTO measurements VALUES (1,11)")

        AppDatabase.MIGRATION_5_6.migrate(db)

        assertEquals(2, scalarInt(db, "SELECT COUNT(*) FROM item_master"))
        assertEquals(1, scalarInt(db, "SELECT COUNT(*) FROM contractor_qualified_items"))
        assertEquals(10, scalarInt(db, "SELECT itemId FROM component_work_items WHERE id=1"))
        assertEquals(10, scalarInt(db, "SELECT itemId FROM measurements WHERE id=1"))
        assertEquals("Masonry", scalarText(db, "SELECT workType FROM item_master WHERE id=10"))
        assertEquals("Finishes", scalarText(db, "SELECT workType FROM contractor_qualified_items WHERE id=20"))
        assertTrue(indexExists(db, "index_item_master_name"))
        assertTrue(indexExists(db, "index_contractor_qualified_items_contractorId_itemName"))
        helper.close()
    }

    private fun scalarInt(db: SupportSQLiteDatabase, sql: String): Int = db.query(sql).use { it.moveToFirst(); it.getInt(0) }
    private fun scalarText(db: SupportSQLiteDatabase, sql: String): String = db.query(sql).use { it.moveToFirst(); it.getString(0) }
    private fun indexExists(db: SupportSQLiteDatabase, index: String): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type='index' AND name=?", arrayOf(index)).use { it.moveToFirst() }
}
