package com.holagundi.archiman.data
import com.holagundi.archiman.data.local.entity.FinanceRecordEntity
import org.junit.Assert.*
import org.junit.Test
class FinanceRecordValidationTest {
    @Test fun malformedAndUnsafeSettingsFailClosed() {
        listOf("not json", "{\"mode\":\"UNKNOWN\",\"rate\":\"18\",\"state\":\"29\",\"gstin\":\"\"}", "{\"mode\":\"NONE\",\"rate\":\"1e999999999\",\"state\":\"\",\"gstin\":\"\"}").forEach { assertTrue(runCatching { FinanceRecordValidation.validate(FinanceRecordEntity("settings","SETTINGS",it)) }.isFailure) }
    }
    @Test fun validBankAndSettingsAccepted() {
        FinanceRecordValidation.validate(FinanceRecordEntity("settings","SETTINGS","{\"mode\":\"NONE\",\"rate\":\"18\",\"state\":\"\",\"gstin\":\"\"}"))
        FinanceRecordValidation.validate(FinanceRecordEntity("bank","BANK","{\"date\":\"2026-11-05\",\"amount\":\"-100.00\",\"status\":\"UNMATCHED\",\"description\":\"Charge\",\"reference\":\"ref\",\"account\":\"Business\"}"))
    }
    @Test fun archiveDatabaseMetadataIsBounded() {
        val data=ByteArray(BackupArchive.MAX_DATABASE_BYTES+1)
        assertTrue(runCatching { BackupArchive.write(mapOf("database.json" to data),java.io.ByteArrayOutputStream()) }.isFailure)
    }
    @Test fun archiveManifestIsBoundedBeforeJsonParsing() {
        val bytes=java.io.ByteArrayOutputStream()
        java.util.zip.ZipOutputStream(bytes).use { zip -> zip.putNextEntry(java.util.zip.ZipEntry("manifest.json"));zip.write(ByteArray(1024*1024+1){32});zip.closeEntry() }
        val error=runCatching { BackupArchive.read(java.io.ByteArrayInputStream(bytes.toByteArray())) }.exceptionOrNull()
        assertTrue(error?.message?.contains("manifest exceeds")==true)
    }
}
