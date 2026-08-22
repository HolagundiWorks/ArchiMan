package com.example.data.attachments

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.util.UUID

class MeasurementAttachmentStore(private val context: Context) {
    private val directory: File get() = File(context.filesDir, "measurement_attachments").apply { mkdirs() }

    fun import(source: Uri): String {
        val mimeType = context.contentResolver.getType(source)
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val input = requireNotNull(context.contentResolver.openInputStream(source)) { "Unable to read selected photo" }
        return input.use { copyIntoManagedStorage(it, extension) }
    }

    internal fun copyIntoManagedStorage(input: InputStream, extension: String): String {
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        target.outputStream().use { output -> input.copyTo(output) }
        return Uri.fromFile(target).toString()
    }

    fun deleteIfManaged(uri: String?) {
        val path = uri?.let { Uri.parse(it).path } ?: return
        val file = File(path).canonicalFile
        val root = directory.canonicalFile
        if (file.parentFile == root && file.exists()) file.delete()
    }
}
