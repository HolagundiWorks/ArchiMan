package com.holagundi.archiman.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BackupArchiveTest {
    private fun archive(): ByteArray = ByteArrayOutputStream().also {
        BackupArchive.write(mapOf("database.json" to "{\"tables\":{}}".toByteArray()), it)
    }.toByteArray()

    @Test fun roundTripsDatabaseAndAttachmentBytes() {
        val attachment = byteArrayOf(0, 1, -1, 34)
        val entries = mapOf("database.json" to "{}".toByteArray(), "attachments/${BackupArchive.digest(attachment)}.bin" to attachment)
        val output = ByteArrayOutputStream()
        BackupArchive.write(entries, output)
        val restored = BackupArchive.read(ByteArrayInputStream(output.toByteArray()))
        entries.forEach { (name, bytes) -> assertArrayEquals(bytes, restored.getValue(name)) }
    }

    @Test fun detectsPayloadTamperingEvenWithValidZipCrc() {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { target ->
            ZipInputStream(ByteArrayInputStream(archive())).use { source ->
                while (true) {
                    val entry = source.nextEntry ?: break
                    val bytes = source.readBytes()
                    target.putNextEntry(ZipEntry(entry.name))
                    target.write(if (entry.name == "database.json") "tampered".toByteArray() else bytes)
                    target.closeEntry()
                }
            }
        }
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(ByteArrayInputStream(output.toByteArray())) }
    }

    @Test fun rejectsTraversalAndMissingManifest() {
        listOf("../database.json", "database.json").forEach { name ->
            val output = ByteArrayOutputStream()
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(name))
                zip.write("{}".toByteArray())
                zip.closeEntry()
            }
            assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(ByteArrayInputStream(output.toByteArray())) }
        }
    }

    @Test fun rejectsDuplicateEntriesAndUnsupportedVersions() {
        val duplicate = String(archive(), Charsets.ISO_8859_1).replace("manifest.json", "database.json").toByteArray(Charsets.ISO_8859_1)
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(ByteArrayInputStream(duplicate)) }
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json"))
            zip.write("{\"format\":\"archiman-backup\",\"version\":99,\"checksums\":{}}".toByteArray())
            zip.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) { BackupArchive.read(ByteArrayInputStream(output.toByteArray())) }
    }
}
