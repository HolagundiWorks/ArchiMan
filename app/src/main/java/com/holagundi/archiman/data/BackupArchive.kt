package com.holagundi.archiman.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.json.JSONObject

/** Bounded, checksummed archive. Entry names never become filesystem paths. */
object BackupArchive {
    const val MAX_BYTES = 64 * 1024 * 1024
    const val MAX_DATABASE_BYTES = 8 * 1024 * 1024
    fun safeByteLimit(): Int = minOf(MAX_BYTES.toLong(),Runtime.getRuntime().maxMemory()/8).coerceAtLeast(8L*1024*1024).toInt()
    private const val MAX_ENTRIES = 2000
    private val attachmentName = Regex("attachments/[a-f0-9]{64}\\.bin")

    fun digest(bytes: ByteArray): String = com.holagundi.archiman.domain.Hex.encode(MessageDigest.getInstance("SHA-256").digest(bytes))

    fun write(entries: Map<String, ByteArray>, output: OutputStream) {
        require(entries.containsKey("database.json")) { "Database is missing." }
        require(entries.getValue("database.json").size<=MAX_DATABASE_BYTES) { "Database metadata exceeds the 8 MB safety limit." }
        com.holagundi.archiman.domain.JsonSafety.check(entries.getValue("database.json").toString(Charsets.UTF_8))
        require(entries.size <= MAX_ENTRIES && entries.values.sumOf { it.size.toLong() } <= safeByteLimit()) { "Backup is too large." }
        require(entries.keys.all { it == "database.json" || attachmentName.matches(it) }) { "Invalid backup entry." }
        val checksums = JSONObject()
        entries.forEach { (name, bytes) -> checksums.put(name, digest(bytes)) }
        val manifest = JSONObject().put("format", "archiman-backup").put("version", 1).put("checksums", checksums)
        ZipOutputStream(output).use { zip ->
            (entries + ("manifest.json" to manifest.toString().toByteArray(Charsets.UTF_8))).forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }

    fun read(input: InputStream): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0L
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && (entry.name == "manifest.json" || entry.name == "database.json" || attachmentName.matches(entry.name))) { "Unexpected backup entry." }
                require(entry.name !in entries && entries.size < MAX_ENTRIES + 1) { "Duplicate or excessive backup entries." }
                val bytes = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= safeByteLimit() + 1024 * 1024L) { "Backup exceeds this device's safe memory limit (${safeByteLimit()/1024/1024} MB)." }
                    require(entry.name != "manifest.json" || bytes.size()+count<=1024*1024) { "Backup manifest exceeds the 1 MB safety limit." }
                    require(entry.name != "database.json" || bytes.size()+count<=MAX_DATABASE_BYTES) { "Database metadata exceeds the 8 MB safety limit." }
                    bytes.write(buffer, 0, count)
                }
                entries[entry.name] = bytes.toByteArray()
            }
        }
        val manifestText = String(requireNotNull(entries.remove("manifest.json")) { "Backup manifest is missing." }, Charsets.UTF_8)
        com.holagundi.archiman.domain.JsonSafety.check(manifestText)
        val manifest = JSONObject(manifestText)
        require(manifest.getString("format") == "archiman-backup" && manifest.getInt("version") == 1) { "Unsupported backup format." }
        val checksums = manifest.getJSONObject("checksums")
        require(checksums.keys().asSequence().toSet() == entries.keys && "database.json" in entries) { "Backup files are missing." }
        entries.forEach { (name, bytes) -> require(checksums.getString(name) == digest(bytes)) { "Backup integrity check failed." } }
        return entries
    }
}
