package com.example.data.local

import android.content.Context
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
class Migration7To8Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-7-8-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun migrationRepairsOrphansAndPreservesEveryMeasurementValue() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createVersion7(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO projects VALUES(1,'Existing','','',0,100)")
        db.execSQL("INSERT INTO contractors VALUES(2,0,'Existing Contractor','','','','Civil',100)")
        db.execSQL("INSERT INTO item_master VALUES(3,'WI-3','Masonry','Brickwork','sqm','AREA',0,1)")
        db.execSQL("INSERT INTO measurements VALUES(10,99,777,888,999,666,55,'Recovered Contractor',44,'Recovered Work','sqm','AREA','North wall',4.0,3.0,0.0,2.0,0.0,24.0,'First','North','','file:///photo.jpg',12345)")
        db.execSQL("INSERT INTO measurements VALUES(11,1,NULL,NULL,NULL,NULL,2,'Existing Contractor',3,'Brickwork','sqm','AREA','South wall',2.0,3.0,0.0,1.0,0.0,6.0,'First','South','ok',NULL,12346)")

        AppDatabase.MIGRATION_7_8.migrate(db)

        db.query("SELECT COUNT(*) FROM measurements").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        db.query("SELECT projectId,floorId,roomId,componentId,componentWorkItemId,contractorId,itemId,description,quantity,photoUri FROM measurements WHERE id=10").use {
            assertTrue(it.moveToFirst())
            assertEquals(99L, it.getLong(0))
            assertTrue(it.isNull(1) && it.isNull(2) && it.isNull(3) && it.isNull(4))
            assertEquals(55L, it.getLong(5))
            assertEquals(44L, it.getLong(6))
            assertEquals("North wall", it.getString(7))
            assertEquals(24.0, it.getDouble(8), 0.0)
            assertEquals("file:///photo.jpg", it.getString(9))
        }
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        db.query("PRAGMA foreign_key_list(measurements)").use { var count = 0; while (it.moveToNext()) count++; assertEquals(7, count) }

        AppDatabase.MIGRATION_8_9.migrate(db)
        db.query("SELECT COUNT(*) FROM measurement_sheets").use { it.moveToFirst(); assertEquals(2, it.getInt(0)) }
        db.query("SELECT s.formulaCode,s.formulaVersion,s.itemNameSnapshot,m.description,m.quantity,m.photoUri FROM measurement_sheets s JOIN measurements m ON m.sheetId=s.id WHERE m.id=10").use {
            assertTrue(it.moveToFirst())
            assertEquals("AREA", it.getString(0))
            assertEquals(1, it.getInt(1))
            assertEquals("Recovered Work", it.getString(2))
            assertEquals("North wall", it.getString(3))
            assertEquals(24.0, it.getDouble(4), 0.0)
            assertEquals("file:///photo.jpg", it.getString(5))
        }
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        db.query("PRAGMA foreign_key_list(measurements)").use { var count = 0; while (it.moveToNext()) count++; assertEquals(8, count) }
        AppDatabase.MIGRATION_9_10.migrate(db)
        db.query("SELECT status,revision,lockedAt,archivedAt FROM measurement_sheets LIMIT 1").use {
            assertTrue(it.moveToFirst())
            assertEquals("DRAFT", it.getString(0))
            assertEquals(1, it.getInt(1))
            assertTrue(it.isNull(2) && it.isNull(3))
        }
        db.query("PRAGMA foreign_key_check").use { assertFalse(it.moveToFirst()) }
        helper.close()
    }

    private fun createVersion7(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE projects(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,name TEXT NOT NULL,client TEXT NOT NULL,siteLocation TEXT NOT NULL,clientId INTEGER NOT NULL,createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE contractors(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,name TEXT NOT NULL,address TEXT NOT NULL,contactNo TEXT NOT NULL,phone TEXT NOT NULL,contractorType TEXT NOT NULL,createdAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE item_master(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,itemCode TEXT NOT NULL,workType TEXT NOT NULL,name TEXT NOT NULL,unit TEXT NOT NULL,calculationType TEXT NOT NULL,isPredefined INTEGER NOT NULL,isActive INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX index_item_master_name ON item_master(name)")
        db.execSQL("CREATE UNIQUE INDEX index_item_master_itemCode ON item_master(itemCode)")
        db.execSQL("CREATE TABLE floors(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL("CREATE TABLE rooms(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL("CREATE TABLE components(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL("CREATE TABLE component_work_items(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)")
        db.execSQL("CREATE TABLE measurements(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,projectId INTEGER NOT NULL,floorId INTEGER,roomId INTEGER,componentId INTEGER,componentWorkItemId INTEGER,contractorId INTEGER NOT NULL,contractorName TEXT NOT NULL,itemId INTEGER NOT NULL,itemName TEXT NOT NULL,unit TEXT NOT NULL,calculationType TEXT NOT NULL,description TEXT NOT NULL,length REAL NOT NULL,width REAL NOT NULL,height REAL NOT NULL,nos REAL NOT NULL,deduction REAL NOT NULL,quantity REAL NOT NULL,floor TEXT NOT NULL,location TEXT NOT NULL,remarks TEXT NOT NULL,photoUri TEXT,date INTEGER NOT NULL)")
    }
}
