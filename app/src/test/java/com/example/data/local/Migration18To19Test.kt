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
class Migration18To19Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-18-19-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun preservesRecordsAndAddsPracticeClientAndSiteData() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(18) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        db.execSQL("CREATE TABLE clients (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `address` TEXT NOT NULL, `contactNo` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE company_profile (`id` INTEGER NOT NULL, `practiceName` TEXT NOT NULL, `legalName` TEXT NOT NULL, `address` TEXT NOT NULL, `city` TEXT NOT NULL, `state` TEXT NOT NULL, `pinCode` TEXT NOT NULL, `phone` TEXT NOT NULL, `email` TEXT NOT NULL, `website` TEXT NOT NULL, `gstin` TEXT NOT NULL, `coaRegistrationNumber` TEXT NOT NULL, `logoUri` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE project_consultancy_profiles (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `consultancyTypes` TEXT NOT NULL, `currentPhase` TEXT NOT NULL, `currentDesignStage` TEXT NOT NULL, `briefStatus` TEXT NOT NULL, `clientObjectives` TEXT NOT NULL, `projectRequirements` TEXT NOT NULL, `designPreferences` TEXT NOT NULL, `siteConstraints` TEXT NOT NULL, `clarifications` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("INSERT INTO clients(name,address,contactNo,createdAt) VALUES ('Existing Client','Old address','123',1)")
        db.execSQL("INSERT INTO company_profile(id,practiceName,legalName,address,city,state,pinCode,phone,email,website,gstin,coaRegistrationNumber,logoUri,updatedAt) VALUES (1,'Existing Practice','','','','','','','','','','',NULL,1)")
        db.execSQL("INSERT INTO project_consultancy_profiles(projectId,consultancyTypes,currentPhase,currentDesignStage,briefStatus,clientObjectives,projectRequirements,designPreferences,siteConstraints,clarifications,updatedAt) VALUES (4,'Architectural','DESIGN','CONCEPT','NOT_STARTED','','','','','',1)")

        AppDatabase.MIGRATION_18_19.migrate(db)

        db.query("SELECT name,clientType,preferredCommunication,email FROM clients").use {
            assertTrue(it.moveToFirst())
            assertEquals("Existing Client", it.getString(0))
            assertEquals("Individual", it.getString(1))
            assertEquals("Phone", it.getString(2))
            assertEquals("", it.getString(3))
        }
        db.query("SELECT practiceName,companyType,country,pan FROM company_profile WHERE id=1").use {
            assertTrue(it.moveToFirst())
            assertEquals("Existing Practice", it.getString(0))
            assertEquals("Architecture practice", it.getString(1))
            assertEquals("India", it.getString(2))
            assertEquals("", it.getString(3))
        }
        db.query("SELECT projectId,siteDimensions,legalPlanningInformation FROM project_consultancy_profiles WHERE projectId=4").use {
            assertTrue(it.moveToFirst())
            assertEquals(4L, it.getLong(0))
            assertEquals("", it.getString(1))
            assertEquals("", it.getString(2))
        }
        helper.close()
    }
}
