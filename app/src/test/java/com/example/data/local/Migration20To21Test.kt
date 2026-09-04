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
class Migration20To21Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-20-21-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsPortalUsersAndImmutableAuditLog() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(20) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_20_21.migrate(db)

        db.execSQL("INSERT INTO local_users(username,displayName,passwordHash,passwordSalt,passwordIterations,role,isActive,createdAt,updatedAt,lastLoginAt) VALUES ('architect','Lead Architect','hash','salt',600000,'ADMIN',1,1,1,NULL)")
        db.execSQL("INSERT INTO portal_audit_events(userId,username,action,entityType,entityId,projectId,summary,sourceAddress,occurredAt) VALUES (1,'architect','ADD_TASK','TASK',9,7,'Site visit','192.168.1.2',2)")

        db.query("SELECT role FROM local_users WHERE username='architect'").use { assertTrue(it.moveToFirst()); assertEquals("ADMIN", it.getString(0)) }
        db.query("SELECT summary FROM portal_audit_events WHERE projectId=7").use { assertTrue(it.moveToFirst()); assertEquals("Site visit", it.getString(0)) }
        assertTrue(runCatching { db.execSQL("UPDATE portal_audit_events SET summary='Changed' WHERE id=1") }.isFailure)
        assertTrue(runCatching { db.execSQL("DELETE FROM portal_audit_events WHERE id=1") }.isFailure)
        helper.close()
    }
}
