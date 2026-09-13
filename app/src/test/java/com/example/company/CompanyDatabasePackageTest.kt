package com.example.company

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_SCHEMA_VERSION
import com.example.data.local.entity.CompanyProfileEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CompanyDatabasePackageTest {
    @Test fun exportsAndPreviewsVerifiedWholeCompanyDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "company-package-test.db"
        context.deleteDatabase(databaseName)
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        database.companyProfileDao().upsert(CompanyProfileEntity(practiceName = "Portable Test Practice"))
        val manager = CompanyDatabasePackageManager(context, database)
        val archive = File(context.cacheDir, "portable-test.archimandb").apply { delete() }
        val checksum = manager.exportTo(Uri.fromFile(archive), "strong-test-password".toCharArray())

        assertTrue(archive.isFile)
        assertEquals(64, checksum.length)
        val preview = manager.previewImport(Uri.fromFile(archive), "strong-test-password".toCharArray())
        assertEquals("Portable Test Practice", preview.practiceName)
        assertEquals(DATABASE_SCHEMA_VERSION, preview.schemaVersion)
        assertEquals(checksum, preview.databaseChecksum)
        manager.discardPreview(preview)

        val error = assertThrows(IllegalArgumentException::class.java) {
            manager.previewImport(Uri.fromFile(archive), "incorrect-password".toCharArray())
        }
        assertTrue(error.message.orEmpty().contains("password is incorrect"))
        archive.delete()
        database.close()
        context.deleteDatabase(databaseName)
        Unit
    }
}
