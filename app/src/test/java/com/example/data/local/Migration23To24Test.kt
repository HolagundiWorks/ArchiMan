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
class Migration23To24Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-23-24-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsDecisionsDailyReportsAndVerifiedSiteIssueClosure() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(23) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_23_24.migrate(db)

        assertEquals(setOf("daily_site_reports", "project_decisions", "site_issues", "site_issue_events"), existingTables(db))
        db.execSQL("INSERT INTO site_issue_events(siteIssueId,fromStatus,toStatus,note,actor,occurredAt) VALUES(1,'OPEN','IN_PROGRESS','Assigned','Architect',100)")
        assertTrue(runCatching { db.execSQL("UPDATE site_issue_events SET note='Changed' WHERE id=1") }.exceptionOrNull() is SQLiteException)
        assertTrue(runCatching { db.execSQL("DELETE FROM site_issue_events WHERE id=1") }.exceptionOrNull() is SQLiteException)
        helper.close()
    }

    private fun existingTables(db: SupportSQLiteDatabase): Set<String> = buildSet {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('daily_site_reports','project_decisions','site_issues','site_issue_events')").use { cursor -> while (cursor.moveToNext()) add(cursor.getString(0)) }
    }
}
