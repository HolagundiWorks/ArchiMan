package com.example.data.local

import android.content.Context
import android.database.sqlite.SQLiteException
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
class Migration22To23Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-22-23-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsCoordinationRegistersAndImmutableEventHistory() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(22) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_22_23.migrate(db)

        assertEquals(setOf("project_consultants", "coordination_items", "coordination_events"), existingTables(db))
        db.execSQL("INSERT INTO coordination_events(coordinationItemId,fromStatus,toStatus,note,actor,occurredAt) VALUES(1,'DRAFT','OPEN','Issued','Architect',100)")
        assertTrue(runCatching { db.execSQL("UPDATE coordination_events SET note='Changed' WHERE id=1") }.exceptionOrNull() is SQLiteException)
        assertTrue(runCatching { db.execSQL("DELETE FROM coordination_events WHERE id=1") }.exceptionOrNull() is SQLiteException)
        helper.close()
    }

    private fun existingTables(db: SupportSQLiteDatabase): Set<String> = buildSet {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('project_consultants','coordination_items','coordination_events')").use { cursor ->
            while (cursor.moveToNext()) add(cursor.getString(0))
        }
    }
}
