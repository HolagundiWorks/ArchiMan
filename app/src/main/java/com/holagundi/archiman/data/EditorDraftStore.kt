package com.holagundi.archiman.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest

/** Device-bound drafts are not portable backups and never leave app-private no-backup storage. */
class EditorDraftStore(context: Context, key: String) {
    private val name = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
    private val directory = File(context.noBackupFilesDir, "editor-drafts")
    private val file = AtomicFile(File(directory, "$name.bin"))
    fun read(): String? = synchronized(lock) {
        if (!file.baseFile.exists()) return@synchronized null
        require(file.baseFile.length() <= MAX_BYTES + 128) { "Local draft exceeds its size limit." }
        RecoveryCipher.decrypt(file.readFully()).toString(Charsets.UTF_8)
    }
    fun write(text: String) = synchronized(lock) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_BYTES) { "Local draft exceeds 2 MB. Save the document before continuing." }
        check(directory.exists() || directory.mkdirs()) { "Could not create local draft storage." }
        val encrypted = RecoveryCipher.encrypt(bytes)
        val stream = file.startWrite()
        try { stream.write(encrypted); file.finishWrite(stream) }
        catch (failure: Exception) { file.failWrite(stream); throw failure }
    }
    fun delete() = synchronized(lock) { file.delete() }
    companion object { private val lock = Any(); private const val MAX_BYTES = 2_000_000 }
}
