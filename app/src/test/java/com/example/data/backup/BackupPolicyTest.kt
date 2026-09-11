package com.example.data.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
class BackupPolicyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun encryptedLegacyBackupIncludesAllMeasurementState() {
        assertEquals(
            expectedIncludes,
            includePaths(R.xml.backup_rules).toSet()
        )
    }

    @Test
    fun cloudAndDeviceTransferPoliciesIncludeAllMeasurementState() {
        val paths = includePaths(R.xml.data_extraction_rules)
        assertEquals(expectedIncludes, paths.toSet())
        assertEquals(2, paths.groupingBy { it }.eachCount().values.distinct().single())
    }

    @Test
    fun backupPolicyNeverExportsCacheOrExternalStorage() {
        val paths = includePaths(R.xml.backup_rules) + includePaths(R.xml.data_extraction_rules)
        assertTrue(paths.none { it.contains("cache", ignoreCase = true) || it.contains("external", ignoreCase = true) })
    }

    private fun includePaths(resourceId: Int): List<String> {
        val parser = context.resources.getXml(resourceId)
        val result = mutableListOf<String>()
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "include") {
                result += "${parser.getAttributeValue(null, "domain")}:${parser.getAttributeValue(null, "path")}"
            }
            parser.next()
        }
        parser.close()
        return result
    }

    private companion object {
        val expectedIncludes = setOf(
            "database:site_measurement.db",
            "file:measurement_attachments/",
            "sharedpref:measurement_drafts.xml"
        )
    }
}
