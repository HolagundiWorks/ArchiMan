package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedDataPolicyTest {
    private lateinit var database: AppDatabase

    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    AppDatabase.installReferenceCatalog(db)
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS lock_approved_measurement_update BEFORE UPDATE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
                    db.execSQL("CREATE TRIGGER IF NOT EXISTS lock_approved_measurement_delete BEFORE DELETE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
                }
            })
            .allowMainThreadQueries()
            .build()
    }

    @After fun cleanup() = database.close()

    @Test fun seedingAddsOnlyReferenceCatalogueAndNeverInventsUserRecords() {
        assertTrue(scalar("SELECT COUNT(*) FROM item_master") > 0)
        listOf(
            "clients",
            "projects",
            "contractors",
            "project_contractor_refs",
            "floors",
            "rooms",
            "components",
            "component_work_items",
            "measurement_sheets",
            "measurements"
        ).forEach { table ->
            assertEquals("$table must start empty", 0, scalar("SELECT COUNT(*) FROM `$table`"))
        }
    }

    private fun scalar(sql: String): Int = database.openHelper.readableDatabase.query(sql).use {
        it.moveToFirst()
        it.getInt(0)
    }
}
