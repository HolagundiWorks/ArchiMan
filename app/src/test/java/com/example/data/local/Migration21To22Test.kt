package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
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
class Migration21To22Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-21-22-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun removesCommercialSchemaAndPreservesEveryMeasurementValue() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(21) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createVersion21(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        seedVersion21(db)

        AppDatabase.MIGRATION_21_22.migrate(db)

        assertFalse(tableExists(db, "contractor_rate_books"))
        assertFalse(tableExists(db, "contractor_rate_book_items"))
        assertFalse(tableExists(db, "project_rate_book_assignments"))
        val columns = columnNames(db, "measurements")
        assertFalse(columns.contains("appliedRateBookId"))
        assertFalse(columns.contains("rateSnapshot"))
        assertFalse(columns.contains("amountSnapshot"))

        db.query("SELECT id,sheetId,projectId,floorId,roomId,componentId,componentWorkItemId,contractorId,contractorName,itemId,itemName,unit,calculationType,formulaCode,formulaVersion,description,length,width,height,nos,deduction,quantity,floor,location,remarks,photoUri,date FROM measurements WHERE id=42").use {
            assertTrue(it.moveToFirst())
            assertEquals(42L, it.getLong(0))
            assertEquals(8L, it.getLong(1))
            assertEquals(1L, it.getLong(2))
            assertEquals(2L, it.getLong(3))
            assertEquals(3L, it.getLong(4))
            assertEquals(4L, it.getLong(5))
            assertEquals(5L, it.getLong(6))
            assertEquals(6L, it.getLong(7))
            assertEquals("Civil Works", it.getString(8))
            assertEquals(7L, it.getLong(9))
            assertEquals("Brickwork", it.getString(10))
            assertEquals("m²", it.getString(11))
            assertEquals("AREA", it.getString(12))
            assertEquals("AREA", it.getString(13))
            assertEquals(2, it.getInt(14))
            assertEquals("North wall", it.getString(15))
            assertEquals(4.25, it.getDouble(16), 0.0)
            assertEquals(2.5, it.getDouble(17), 0.0)
            assertEquals(0.0, it.getDouble(18), 0.0)
            assertEquals(2.0, it.getDouble(19), 0.0)
            assertEquals(1.5, it.getDouble(20), 0.0)
            assertEquals(19.75, it.getDouble(21), 0.0)
            assertEquals("Ground Floor", it.getString(22))
            assertEquals("Grid A1", it.getString(23))
            assertEquals("Verified", it.getString(24))
            assertEquals("file:///evidence.jpg", it.getString(25))
            assertEquals(123456789L, it.getLong(26))
        }
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }

        db.execSQL("UPDATE measurement_sheets SET status='APPROVED' WHERE id=8")
        assertTrue(runCatching { db.execSQL("UPDATE measurements SET description='Tampered' WHERE id=42") }.exceptionOrNull() is SQLiteException)
        helper.close()
    }

    private fun createVersion21(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE projects(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE floors(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE rooms(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE components(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE component_work_items(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE contractors(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE item_master(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE measurement_sheets(id INTEGER PRIMARY KEY NOT NULL,status TEXT NOT NULL)")
        db.execSQL("CREATE TABLE measurement_review_events(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE measurements(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,sheetId INTEGER NOT NULL,projectId INTEGER NOT NULL,floorId INTEGER,roomId INTEGER,componentId INTEGER,componentWorkItemId INTEGER,contractorId INTEGER NOT NULL,contractorName TEXT NOT NULL,itemId INTEGER NOT NULL,itemName TEXT NOT NULL,unit TEXT NOT NULL,calculationType TEXT NOT NULL,formulaCode TEXT NOT NULL,formulaVersion INTEGER NOT NULL,description TEXT NOT NULL,length REAL NOT NULL,width REAL NOT NULL,height REAL NOT NULL,nos REAL NOT NULL,deduction REAL NOT NULL,quantity REAL NOT NULL,appliedRateBookId INTEGER,rateSnapshot REAL,amountSnapshot REAL,floor TEXT NOT NULL,location TEXT NOT NULL,remarks TEXT NOT NULL,photoUri TEXT,date INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE contractor_rate_books(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE contractor_rate_book_items(id INTEGER PRIMARY KEY NOT NULL)")
        db.execSQL("CREATE TABLE project_rate_book_assignments(id INTEGER PRIMARY KEY NOT NULL)")
    }

    private fun seedVersion21(db: SupportSQLiteDatabase) {
        listOf("projects" to 1, "floors" to 2, "rooms" to 3, "components" to 4, "component_work_items" to 5, "contractors" to 6, "item_master" to 7).forEach { (table, id) ->
            db.execSQL("INSERT INTO $table(id) VALUES($id)")
        }
        db.execSQL("INSERT INTO measurement_sheets(id,status) VALUES(8,'DRAFT')")
        db.execSQL("INSERT INTO measurements VALUES(42,8,1,2,3,4,5,6,'Civil Works',7,'Brickwork','m²','AREA','AREA',2,'North wall',4.25,2.5,0.0,2.0,1.5,19.75,9,125.0,2468.75,'Ground Floor','Grid A1','Verified','file:///evidence.jpg',123456789)")
        db.execSQL("INSERT INTO contractor_rate_books VALUES(1)")
        db.execSQL("INSERT INTO contractor_rate_book_items VALUES(1)")
        db.execSQL("INSERT INTO project_rate_book_assignments VALUES(1)")
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { it.moveToFirst() }

    private fun columnNames(db: SupportSQLiteDatabase, table: String): Set<String> = buildSet {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) add(cursor.getString(nameIndex))
        }
    }
}
