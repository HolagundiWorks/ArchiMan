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
class Migration15To16Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-15-16-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun createsControlledDocumentTablesAndLocksRevisions() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(15) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_15_16.migrate(db)
        db.execSQL("INSERT INTO project_drawings VALUES(1,7,'A-101','Ground floor plan','Architectural','ISSUED',1,1,NULL)")
        db.execSQL("INSERT INTO drawing_revisions VALUES(2,7,1,'R0','A-101.dwg','application/acad','content://drawing','abc','ISSUED','First issue',0,1,1)")

        db.query("SELECT drawingNumber,title FROM project_drawings WHERE projectId=7").use {
            assertTrue(it.moveToFirst())
            assertEquals("A-101", it.getString(0))
            assertEquals("Ground floor plan", it.getString(1))
        }
        var immutable = false
        try {
            db.execSQL("UPDATE drawing_revisions SET revisionNotes='changed' WHERE id=2")
        } catch (_: SQLiteException) {
            immutable = true
        }
        assertTrue("Drawing revision updates must be rejected", immutable)
        helper.close()
    }
}
