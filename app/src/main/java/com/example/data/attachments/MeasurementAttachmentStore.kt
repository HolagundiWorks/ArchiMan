package com.example.data.attachments

import android.content.Context
import android.net.Uri
import java.io.InputStream

class MeasurementAttachmentStore(context: Context) {
    private val store = ManagedAttachmentStore(context, DIRECTORY)

    fun import(source: Uri): String = store.import(source)

    internal fun copyIntoManagedStorage(input: InputStream, extension: String): String =
        store.copyIntoManagedStorage(input, extension)

    fun deleteIfManaged(uri: String?) = store.deleteIfManaged(uri)

    companion object {
        const val DIRECTORY = "measurement_attachments"
    }
}
