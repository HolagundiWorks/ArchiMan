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
class Migration19To20Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-19-20-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsOnboardingApprovalAndBacklogRegisters() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(19) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        AppDatabase.MIGRATION_19_20.migrate(db)

        db.execSQL("INSERT INTO project_onboarding_responses(projectId,templateVersion,questionCode,answer,clarification,updatedAt) VALUES (7,1,'users','Family of four','',1)")
        db.execSQL("INSERT INTO project_approvals(projectId,approvalType,title,description,phase,status,submittedAt,approvedAt,remarks,createdAt) VALUES (7,'CLIENT','Concept plan','','DESIGN','PENDING',NULL,NULL,'',1)")
        db.execSQL("INSERT INTO project_backlog(projectId,category,title,description,priority,status,phase,dueAt,createdAt) VALUES (7,'DRAWINGS','Door schedule','','HIGH','OPEN','DESIGN',NULL,1)")

        db.query("SELECT answer FROM project_onboarding_responses WHERE projectId=7").use { assertTrue(it.moveToFirst()); assertEquals("Family of four", it.getString(0)) }
        db.query("SELECT status FROM project_approvals WHERE projectId=7").use { assertTrue(it.moveToFirst()); assertEquals("PENDING", it.getString(0)) }
        db.query("SELECT priority FROM project_backlog WHERE projectId=7").use { assertTrue(it.moveToFirst()); assertEquals("HIGH", it.getString(0)) }
        helper.close()
    }
}
