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
class Migration17To18Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-17-18-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun addsProjectBriefAndScopeFoundation() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(17) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase

        AppDatabase.MIGRATION_17_18.migrate(db)

        db.execSQL("INSERT INTO project_consultancy_profiles (`projectId`,`consultancyTypes`,`currentPhase`,`currentDesignStage`,`briefStatus`,`clientObjectives`,`projectRequirements`,`designPreferences`,`siteConstraints`,`clarifications`,`updatedAt`) VALUES (7,'Architectural, Landscaping','DESIGN','CONCEPT','IN_PROGRESS','Comfortable home','Three bedrooms','Natural materials','Narrow access','Confirm parking',123)")
        db.query("SELECT consultancyTypes,briefStatus,clientObjectives FROM project_consultancy_profiles WHERE projectId=7").use {
            assertTrue(it.moveToFirst())
            assertEquals("Architectural, Landscaping", it.getString(0))
            assertEquals("IN_PROGRESS", it.getString(1))
            assertEquals("Comfortable home", it.getString(2))
        }

        db.execSQL("INSERT INTO project_scope_items (`projectId`,`category`,`title`,`details`,`status`,`orderIndex`,`createdAt`) VALUES (7,'DELIVERABLE','Concept plans','Two options','INCLUDED',0,123)")
        db.query("SELECT category,title,status FROM project_scope_items WHERE projectId=7").use {
            assertTrue(it.moveToFirst())
            assertEquals("DELIVERABLE", it.getString(0))
            assertEquals("Concept plans", it.getString(1))
            assertEquals("INCLUDED", it.getString(2))
        }
        helper.close()
    }
}
