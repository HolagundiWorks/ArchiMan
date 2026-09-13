package com.example.company

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_SCHEMA_VERSION
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class CompanyDatabaseImportPreview(
    internal val stagingDirectory: File,
    val practiceName: String,
    val schemaVersion: Int,
    val projectCount: Int,
    val measurementCount: Int,
    val attachmentCount: Int,
    val databaseChecksum: String
)

/**
 * Portable whole-company package modelled after LEOS' verified .leosdb flow.
 * The package contains one SQLite snapshot plus only ArchiMan-managed files.
 */
class CompanyDatabasePackageManager(
    private val context: Context,
    private val database: AppDatabase
) {
    fun exportTo(destination: Uri, password: CharArray): String {
        require(password.size >= MIN_PASSWORD_LENGTH) { "Use at least $MIN_PASSWORD_LENGTH characters for the company database password." }
        val work = File(context.cacheDir, "company-export-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val snapshot = File(work, DATABASE_ENTRY)
            createSnapshot(snapshot)
            val databaseChecksum = sha256(snapshot)
            val managedFiles = managedFiles().map { (entry, file) ->
                PackageFile(entry, file.length(), sha256(file), file)
            }
            val manifest = JSONObject()
                .put("format", FORMAT)
                .put("formatVersion", FORMAT_VERSION)
                .put("app", "ArchiMan")
                .put("schemaVersion", readSchemaVersion(snapshot))
                .put("createdAt", System.currentTimeMillis())
                .put("databaseSha256", databaseChecksum)
                .put("files", JSONArray().apply {
                    put(JSONObject().put("path", DATABASE_ENTRY).put("size", snapshot.length()).put("sha256", databaseChecksum))
                    managedFiles.forEach { item -> put(JSONObject().put("path", item.entry).put("size", item.size).put("sha256", item.checksum)) }
                })

            val archive = File(work, "ArchiMan-company.archimandb")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(archive))).use { zip ->
                writeEntry(zip, MANIFEST_ENTRY, manifest.toString(2).toByteArray(Charsets.UTF_8))
                writeFileEntry(zip, DATABASE_ENTRY, snapshot)
                managedFiles.forEach { writeFileEntry(zip, it.entry, it.file) }
            }
            require(archive.length() <= MAX_ARCHIVE_BYTES) { "Company database package is too large." }
            encryptArchive(archive, destination, password)
            return databaseChecksum
        } finally {
            password.fill('\u0000')
            work.deleteRecursively()
        }
    }

    fun previewImport(source: Uri, password: CharArray): CompanyDatabaseImportPreview {
        require(password.isNotEmpty()) { "Enter the company database password." }
        val staging = File(context.cacheDir, "company-import-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            decryptAndExtract(source, staging, password)
            val manifestFile = File(staging, MANIFEST_ENTRY)
            val databaseFile = File(staging, DATABASE_ENTRY)
            require(manifestFile.isFile && databaseFile.isFile) { "Package must contain manifest.json and company.sqlite." }
            val manifest = JSONObject(manifestFile.readText(Charsets.UTF_8))
            require(manifest.optString("format") == FORMAT && manifest.optInt("formatVersion") == FORMAT_VERSION) {
                "This is not a supported ArchiMan company database."
            }
            val packageFiles = manifest.getJSONArray("files").let { files ->
                buildMap {
                    for (index in 0 until files.length()) {
                        val item = files.getJSONObject(index)
                        put(item.getString("path"), ManifestFile(item.getLong("size"), item.getString("sha256")))
                    }
                }
            }
            val extracted = staging.walkTopDown().filter(File::isFile).map { it.relativeTo(staging).invariantSeparatorsPath }.toSet()
            require(extracted == packageFiles.keys + MANIFEST_ENTRY) { "Package file list does not match its manifest." }
            packageFiles.forEach { (path, expected) ->
                val file = File(staging, path)
                require(file.length() == expected.size) { "Size validation failed for $path." }
                require(sha256(file) == expected.checksum) { "Checksum validation failed for $path." }
            }
            require(manifest.getString("databaseSha256") == packageFiles.getValue(DATABASE_ENTRY).checksum) { "Database checksum metadata does not match." }
            val inspection = inspectDatabase(databaseFile)
            require(inspection.schemaVersion in MIN_IMPORT_SCHEMA..DATABASE_SCHEMA_VERSION) {
                "Database schema ${inspection.schemaVersion} cannot be opened by this ArchiMan version."
            }
            return CompanyDatabaseImportPreview(
                stagingDirectory = staging,
                practiceName = inspection.practiceName,
                schemaVersion = inspection.schemaVersion,
                projectCount = inspection.projectCount,
                measurementCount = inspection.measurementCount,
                attachmentCount = packageFiles.keys.count { it.startsWith("files/") },
                databaseChecksum = packageFiles.getValue(DATABASE_ENTRY).checksum
            )
        } catch (error: Exception) {
            staging.deleteRecursively()
            throw IllegalArgumentException("The password is incorrect or the company database is damaged.", error)
        } finally {
            password.fill('\u0000')
        }
    }

    fun discardPreview(preview: CompanyDatabaseImportPreview?) {
        preview?.stagingDirectory?.takeIf { it.parentFile == context.cacheDir }?.deleteRecursively()
    }

    fun stageRestore(preview: CompanyDatabaseImportPreview) {
        val restoreRoot = restoreRoot(context)
        val candidate = File(restoreRoot, "pending-${UUID.randomUUID()}")
        require(preview.stagingDirectory.copyRecursively(candidate, overwrite = false)) { "Could not stage the company database." }
        val pending = File(restoreRoot, PENDING_DIRECTORY)
        pending.deleteRecursively()
        require(candidate.renameTo(pending)) { "Could not activate the staged company database." }
        preview.stagingDirectory.deleteRecursively()
    }

    fun consumeRestoreMessage(): String? {
        val result = File(restoreRoot(context), RESULT_FILE)
        return result.takeIf(File::isFile)?.readText()?.also { result.delete() }
    }

    private fun createSnapshot(target: File) {
        val sqlite = database.openHelper.writableDatabase
        val source = File(requireNotNull(sqlite.path) { "ArchiMan database path is unavailable." })
        val onlineSnapshot = runCatching {
            sqlite.execSQL("VACUUM INTO ${DatabaseUtils.sqlEscapeString(target.path)}")
            target.isFile
        }.getOrDefault(false)
        if (!onlineSnapshot) {
            target.delete()
            require(source.isFile) { "ArchiMan database is not available." }
            sqlite.query("PRAGMA wal_checkpoint(FULL)").use { cursor -> while (cursor.moveToNext()) Unit }
            source.copyTo(target, overwrite = false)
        }
        val inspection = inspectDatabase(target)
        require(inspection.schemaVersion == DATABASE_SCHEMA_VERSION) { "Database snapshot is not on the current schema." }
    }

    private fun managedFiles(): List<Pair<String, File>> = MANAGED_DIRECTORIES.flatMap { directoryName ->
        val directory = File(context.filesDir, directoryName)
        if (!directory.isDirectory) return@flatMap emptyList()
        directory.walkTopDown().filter(File::isFile).map { managed ->
            "files/$directoryName/${managed.relativeTo(directory).invariantSeparatorsPath}" to managed
        }.toList()
    }

    private fun decryptAndExtract(source: Uri, staging: File, password: CharArray) {
        val raw = requireNotNull(openInput(source)) { "Could not open the selected company database." }
        DataInputStream(BufferedInputStream(raw)).use { input ->
            val magic = ByteArray(FILE_MAGIC.size).also(input::readFully)
            require(magic.contentEquals(FILE_MAGIC)) { "This is not an encrypted ArchiMan company database." }
            val iterations = input.readInt()
            require(iterations == KEY_ITERATIONS) { "Unsupported company database encryption settings." }
            val salt = ByteArray(SALT_BYTES).also(input::readFully)
            val iv = ByteArray(IV_BYTES).also(input::readFully)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, deriveKey(password, salt, iterations), GCMParameterSpec(128, iv)) }
            CipherInputStream(input, cipher).use { encrypted -> extractPackage(encrypted, staging) }
        }
    }

    private fun extractPackage(raw: InputStream, staging: File) {
        var total = 0L
        var entries = 0
        ZipInputStream(BufferedInputStream(raw)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    require(++entries <= MAX_ENTRIES) { "Company database contains too many files." }
                    val name = entry.name.replace('\\', '/')
                    require(name == MANIFEST_ENTRY || name == DATABASE_ENTRY || MANAGED_DIRECTORIES.any { name.startsWith("files/$it/") }) {
                        "Unsupported package entry: $name"
                    }
                    require(!name.startsWith('/') && name.split('/').none { it == ".." || it.isBlank() }) { "Unsafe package path." }
                    val output = File(staging, name).canonicalFile
                    require(output.path.startsWith(staging.canonicalPath + File.separator)) { "Unsafe package path." }
                    require(!output.exists()) { "Duplicate package entry: $name" }
                    output.parentFile?.mkdirs()
                    FileOutputStream(output).use { fileOutput ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            total += read
                            require(total <= MAX_UNCOMPRESSED_BYTES) { "Company database is too large after extraction." }
                            fileOutput.write(buffer, 0, read)
                        }
                        fileOutput.fd.sync()
                    }
                }
            }
    }

    private fun encryptArchive(archive: File, destination: Uri, password: CharArray) {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, deriveKey(password, salt, KEY_ITERATIONS), GCMParameterSpec(128, iv)) }
        val output = requireNotNull(openOutput(destination)) { "Could not create the company database file." }
        DataOutputStream(BufferedOutputStream(output)).use { header ->
            header.write(FILE_MAGIC)
            header.writeInt(KEY_ITERATIONS)
            header.write(salt)
            header.write(iv)
            CipherOutputStream(header, cipher).use { encrypted -> FileInputStream(archive).use { it.copyTo(encrypted) } }
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        return try {
            val factory = runCatching { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256") }
                .getOrElse { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") }
            SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun openInput(uri: Uri): InputStream? =
        if (uri.scheme == "file") uri.path?.let { FileInputStream(it) } else context.contentResolver.openInputStream(uri)

    private fun openOutput(uri: Uri): OutputStream? =
        if (uri.scheme == "file") uri.path?.let { FileOutputStream(it) } else context.contentResolver.openOutputStream(uri, "w")

    private fun inspectDatabase(file: File): DatabaseInspection {
        val sqlite = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        return try {
            val integrity = sqlite.rawQuery("PRAGMA integrity_check", null).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else "failed" }
            require(integrity == "ok") { "Company database integrity check failed." }
            val foreignKeys = sqlite.rawQuery("PRAGMA foreign_key_check", null).use { cursor -> cursor.count }
            require(foreignKeys == 0) { "Company database contains broken references." }
            val schema = sqlite.rawQuery("PRAGMA user_version", null).use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }
            val tables = sqlite.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) }
            }
            require(setOf("projects", "measurements", "item_master").all(tables::contains)) { "Required ArchiMan tables are missing." }
            fun count(table: String) = sqlite.rawQuery("SELECT COUNT(*) FROM $table", null).use { cursor -> cursor.moveToFirst(); cursor.getInt(0) }
            val practice = if ("company_profile" in tables) sqlite.rawQuery("SELECT practiceName FROM company_profile WHERE id=1", null).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else "" } else ""
            DatabaseInspection(schema, practice.ifBlank { "Unnamed company" }, count("projects"), count("measurements"))
        } finally {
            sqlite.close()
        }
    }

    private fun readSchemaVersion(file: File): Int = inspectDatabase(file).schemaVersion

    private fun writeEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name).apply { time = 0L })
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun writeFileEntry(zip: ZipOutputStream, name: String, file: File) {
        zip.putNextEntry(ZipEntry(name).apply { time = 0L })
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private data class PackageFile(val entry: String, val size: Long, val checksum: String, val file: File)
    private data class ManifestFile(val size: Long, val checksum: String)
    private data class DatabaseInspection(val schemaVersion: Int, val practiceName: String, val projectCount: Int, val measurementCount: Int)

    companion object {
        internal const val DATABASE_NAME = "site_measurement.db"
        private const val FORMAT = "archiman-company-database"
        private const val FORMAT_VERSION = 1
        private const val MIN_IMPORT_SCHEMA = 3
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val DATABASE_ENTRY = "company.sqlite"
        private const val PENDING_DIRECTORY = "pending"
        private const val RESULT_FILE = "last-result.txt"
        private const val MAX_ARCHIVE_BYTES = 1_073_741_824L
        private const val MAX_UNCOMPRESSED_BYTES = 1_073_741_824L
        private const val MAX_ENTRIES = 10_000
        private const val MIN_PASSWORD_LENGTH = 12
        private const val KEY_ITERATIONS = 210_000
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private val FILE_MAGIC = "ARCHIMANDB1".toByteArray(Charsets.US_ASCII)
        internal val MANAGED_DIRECTORIES = listOf("company", "measurement_attachments")
        internal fun restoreRoot(context: Context) = File(context.filesDir, "company_restore").apply { mkdirs() }
    }
}

/** Applies a previously validated package before Room opens the active database. */
object CompanyDatabaseRestoreCoordinator {
    fun applyPendingRestore(context: Context) {
        val root = CompanyDatabasePackageManager.restoreRoot(context)
        val pending = File(root, "pending")
        if (!pending.isDirectory) return
        val result = File(root, "last-result.txt")
        val previous = File(root, "previous-${System.currentTimeMillis()}").apply { mkdirs() }
        val active = context.getDatabasePath(CompanyDatabasePackageManager.DATABASE_NAME)
        try {
            backupActive(context, active, previous)
            replaceFile(File(pending, "company.sqlite"), active)
            File(active.path + "-wal").delete()
            File(active.path + "-shm").delete()
            CompanyDatabasePackageManager.MANAGED_DIRECTORIES.forEach { name ->
                val target = File(context.filesDir, name)
                target.deleteRecursively()
                val staged = File(pending, "files/$name")
                if (staged.isDirectory) require(staged.copyRecursively(target, overwrite = false)) { "Could not restore $name." }
            }
            rewriteManagedUris(context, active)
            pending.deleteRecursively()
            result.writeText("Company database restored successfully. The previous local database is retained in private recovery storage.")
            prunePrevious(root)
        } catch (error: Exception) {
            restorePrevious(context, active, previous)
            result.writeText("Company database restore failed; the previous local data was recovered. ${error.message.orEmpty()}")
        }
    }

    private fun backupActive(context: Context, active: File, destination: File) {
        listOf(active, File(active.path + "-wal"), File(active.path + "-shm")).filter(File::isFile).forEach { file -> file.copyTo(File(destination, file.name), overwrite = false) }
        CompanyDatabasePackageManager.MANAGED_DIRECTORIES.forEach { name ->
            val source = File(context.filesDir, name)
            if (source.isDirectory) require(source.copyRecursively(File(destination, "files/$name"), overwrite = false)) { "Could not create the safety backup." }
        }
    }

    private fun restorePrevious(context: Context, active: File, previous: File) {
        listOf(active, File(active.path + "-wal"), File(active.path + "-shm")).forEach(File::delete)
        listOf(active.name, active.name + "-wal", active.name + "-shm").forEach { name ->
            File(previous, name).takeIf(File::isFile)?.copyTo(File(active.parentFile, name), overwrite = true)
        }
        CompanyDatabasePackageManager.MANAGED_DIRECTORIES.forEach { name ->
            val target = File(context.filesDir, name)
            target.deleteRecursively()
            File(previous, "files/$name").takeIf(File::isDirectory)?.copyRecursively(target, overwrite = false)
        }
    }

    private fun replaceFile(source: File, target: File) {
        require(source.isFile) { "Staged database is missing." }
        target.parentFile?.mkdirs()
        val candidate = File(target.parentFile, target.name + ".restore")
        candidate.delete()
        source.copyTo(candidate, overwrite = false)
        FileOutputStream(candidate, true).use { it.fd.sync() }
        target.delete()
        require(candidate.renameTo(target)) { "Could not replace the active database." }
    }

    private fun rewriteManagedUris(context: Context, databaseFile: File) {
        val sqlite = SQLiteDatabase.openDatabase(databaseFile.path, null, SQLiteDatabase.OPEN_READWRITE)
        try {
            fun rewrite(table: String, idColumn: String, uriColumn: String, directory: String) {
                sqlite.rawQuery("SELECT $idColumn,$uriColumn FROM $table WHERE $uriColumn IS NOT NULL AND $uriColumn LIKE 'file:%'", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(0)
                        val name = Uri.parse(cursor.getString(1)).lastPathSegment ?: continue
                        val target = File(context.filesDir, "$directory/$name")
                        if (target.isFile) sqlite.execSQL("UPDATE $table SET $uriColumn=? WHERE $idColumn=?", arrayOf<Any>(Uri.fromFile(target).toString(), id))
                    }
                }
            }
            val tables = sqlite.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor -> buildSet { while (cursor.moveToNext()) add(cursor.getString(0)) } }
            if ("company_profile" in tables) rewrite("company_profile", "id", "logoUri", "company")
            if ("measurements" in tables) rewrite("measurements", "id", "photoUri", "measurement_attachments")
        } finally {
            sqlite.close()
        }
    }

    private fun prunePrevious(root: File) {
        root.listFiles { file -> file.isDirectory && file.name.startsWith("previous-") }
            ?.sortedByDescending(File::lastModified)?.drop(3)?.forEach(File::deleteRecursively)
    }
}
