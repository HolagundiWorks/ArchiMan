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
class Migration16To17Test {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val name = "migration-16-17-test.db"

    @After fun cleanup() = context.deleteDatabase(name).let { Unit }

    @Test fun preservesProjectIdentityAndAddsProfiles() {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(16) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE projects (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `client` TEXT NOT NULL, `clientId` INTEGER NOT NULL, `siteLocation` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        val db = helper.writableDatabase
        db.execSQL("INSERT INTO projects VALUES(5,'Retained Project','Retained Client',3,'Bengaluru',123)")

        AppDatabase.MIGRATION_16_17.migrate(db)

        db.query("SELECT name,client,siteLocation,projectType,status,areaUnit FROM projects WHERE id=5").use {
            assertTrue(it.moveToFirst())
            assertEquals("Retained Project", it.getString(0))
            assertEquals("Retained Client", it.getString(1))
            assertEquals("Bengaluru", it.getString(2))
            assertEquals("Residential", it.getString(3))
            assertEquals("ACTIVE", it.getString(4))
            assertEquals("m²", it.getString(5))
        }
        db.execSQL("INSERT INTO company_profile VALUES(1,'Studio','Studio Legal','Address','City','Karnataka','560001','1','a@b.in','example.in','','CA/1',NULL,1)")
        db.query("SELECT practiceName,coaRegistrationNumber FROM company_profile WHERE id=1").use {
            assertTrue(it.moveToFirst())
            assertEquals("Studio", it.getString(0))
            assertEquals("CA/1", it.getString(1))
        }
        helper.close()
    }
}
