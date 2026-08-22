package com.example.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Migration4To5Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "migration-4-5-test.db"

    @After fun cleanUp() = context.deleteDatabase(databaseName).let { Unit }

    @Test
    fun migrationPreservesMeasurementAndRemovesCommercialStorage() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(databaseName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE item_master (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, unit TEXT NOT NULL, calculationType TEXT NOT NULL, defaultRate REAL NOT NULL, isPredefined INTEGER NOT NULL)")
                        db.execSQL("CREATE TABLE contractor_qualified_items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contractorId INTEGER NOT NULL, itemName TEXT NOT NULL, uom TEXT NOT NULL, calculationType TEXT NOT NULL, rate REAL NOT NULL)")
                        db.execSQL("CREATE TABLE measurements (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, projectId INTEGER NOT NULL, floorId INTEGER, roomId INTEGER, componentId INTEGER, componentWorkItemId INTEGER, contractorId INTEGER NOT NULL, contractorName TEXT NOT NULL, itemId INTEGER NOT NULL, itemName TEXT NOT NULL, unit TEXT NOT NULL, calculationType TEXT NOT NULL, description TEXT NOT NULL, length REAL NOT NULL, width REAL NOT NULL, height REAL NOT NULL, nos REAL NOT NULL, deduction REAL NOT NULL, quantity REAL NOT NULL, rate REAL NOT NULL, amount REAL NOT NULL, floor TEXT NOT NULL, location TEXT NOT NULL, remarks TEXT NOT NULL, photoUri TEXT, date INTEGER NOT NULL, billId INTEGER)")
                        db.execSQL("CREATE TABLE contractor_rates (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, contractorId INTEGER NOT NULL, itemId INTEGER NOT NULL, rate REAL NOT NULL)")
                        db.execSQL("CREATE TABLE bills (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, billNumber TEXT NOT NULL)")
                    }
                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO item_master VALUES (7,'Brickwork','m²','AREA',850,1)")
        db.execSQL("INSERT INTO contractor_qualified_items VALUES (8,3,'Brickwork','m²','AREA',850)")
        db.execSQL("INSERT INTO measurements VALUES (42,1,2,NULL,NULL,NULL,3,'Contractor',7,'Brickwork','m²','AREA','North wall',4,3,0,2,1,23,850,19550,'Ground','Grid A','Checked','content://photo',123456,9)")

        AppDatabase.MIGRATION_4_5.migrate(db)

        db.query("SELECT id, description, quantity, photoUri FROM measurements").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(42L, cursor.getLong(0))
            assertEquals("North wall", cursor.getString(1))
            assertEquals(23.0, cursor.getDouble(2), 0.0)
            assertEquals("content://photo", cursor.getString(3))
        }
        assertFalse(columns(db, "measurements").containsAll(listOf("rate", "amount", "billId")))
        assertFalse(columns(db, "item_master").contains("defaultRate"))
        assertFalse(columns(db, "contractor_qualified_items").contains("rate"))
        assertFalse(tableExists(db, "contractor_rates"))
        assertFalse(tableExists(db, "bills"))
        helper.close()
    }

    private fun columns(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Set<String> {
        val result = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$table`)").use { cursor -> while (cursor.moveToNext()) result += cursor.getString(1) }
        return result
    }

    private fun tableExists(db: androidx.sqlite.db.SupportSQLiteDatabase, table: String): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }
}
