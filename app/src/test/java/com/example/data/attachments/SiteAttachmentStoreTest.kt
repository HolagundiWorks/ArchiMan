package com.example.data.attachments

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SiteAttachmentStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun copiesIntoSiteAttachmentsDirectoryAndDeletesOnlyManagedFiles() {
        val store = SiteAttachmentStore(context)
        val bytes = byteArrayOf(9, 8, 7, 6)
        val uri = store.copyIntoManagedStorage(ByteArrayInputStream(bytes), "jpg")
        val file = File(requireNotNull(Uri.parse(uri).path))

        assertTrue(file.exists())
        assertEquals(SiteAttachmentStore.DIRECTORY, file.parentFile?.name)
        assertArrayEquals(bytes, file.readBytes())
        store.deleteIfManaged(uri)
        assertFalse(file.exists())
    }

    private fun assertEquals(expected: String?, actual: String?) {
        org.junit.Assert.assertEquals(expected, actual)
    }
}
