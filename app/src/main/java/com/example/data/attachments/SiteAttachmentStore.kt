package com.example.data.attachments

import android.content.Context
import android.net.Uri
import java.io.InputStream

/** Managed evidence for inspections, daily logs and snag/NCR before/after photos. */
class SiteAttachmentStore(context: Context) {
    private val store = ManagedAttachmentStore(context, DIRECTORY)

    fun import(source: Uri): String = store.import(source)

    internal fun copyIntoManagedStorage(input: InputStream, extension: String): String =
        store.copyIntoManagedStorage(input, extension)

    fun deleteIfManaged(uri: String?) = store.deleteIfManaged(uri)

    companion object {
        const val DIRECTORY = "site_attachments"
    }
}
