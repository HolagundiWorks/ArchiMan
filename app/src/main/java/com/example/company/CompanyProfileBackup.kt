package com.example.company

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.local.entity.CompanyProfileEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

@JsonClass(generateAdapter = true)
data class CompanyProfileBackupPayload(
    val format: String = "archiman-company-profile",
    val formatVersion: Int = 1,
    val exportedAt: Long,
    val profile: CompanyProfileBackupRecord,
    val logoMimeType: String? = null,
    val logoBase64: String? = null
)

@JsonClass(generateAdapter = true)
data class CompanyProfileBackupRecord(
    val practiceName: String,
    val legalName: String,
    val companyType: String,
    val address: String,
    val city: String,
    val state: String,
    val country: String,
    val pinCode: String,
    val phone: String,
    val email: String,
    val website: String,
    val pan: String,
    val gstin: String,
    val coaRegistrationNumber: String,
    val principalName: String,
    val principalQualification: String,
    val practiceRegistrationDetails: String
) {
    fun toEntity(logoUri: String? = null) = CompanyProfileEntity(
        practiceName = practiceName,
        legalName = legalName,
        companyType = companyType,
        address = address,
        city = city,
        state = state,
        country = country,
        pinCode = pinCode,
        phone = phone,
        email = email,
        website = website,
        pan = pan,
        gstin = gstin,
        coaRegistrationNumber = coaRegistrationNumber,
        principalName = principalName,
        principalQualification = principalQualification,
        practiceRegistrationDetails = practiceRegistrationDetails,
        logoUri = logoUri
    )

    companion object {
        fun from(profile: CompanyProfileEntity) = CompanyProfileBackupRecord(
            profile.practiceName, profile.legalName, profile.companyType, profile.address,
            profile.city, profile.state, profile.country, profile.pinCode, profile.phone,
            profile.email, profile.website, profile.pan, profile.gstin,
            profile.coaRegistrationNumber, profile.principalName,
            profile.principalQualification, profile.practiceRegistrationDetails
        )
    }
}

class CompanyProfileBackupManager(private val context: Context) {
    private val adapter = Moshi.Builder().build().adapter(CompanyProfileBackupPayload::class.java).indent("  ")
    private val logoDirectory = File(context.filesDir, "company").apply { mkdirs() }

    fun storeLogo(source: Uri): String {
        val mime = context.contentResolver.getType(source) ?: "image/png"
        require(mime.startsWith("image/")) { "Select a PNG, JPG or other image file." }
        val extension = when (mime) { "image/jpeg" -> "jpg"; "image/svg+xml" -> "svg"; else -> "png" }
        val target = File(logoDirectory, "profile-logo.$extension")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Could not open the selected logo." }
            target.outputStream().use { output -> input.copyTo(output) }
        }
        return Uri.fromFile(target).toString()
    }

    fun exportTo(destination: Uri, profile: CompanyProfileEntity) {
        val logoBytes = profile.logoUri?.let(::readLogoBytes)
        require(logoBytes == null || logoBytes.size <= MAX_LOGO_BYTES) { "Logo is too large for a company-profile backup (maximum 5 MB)." }
        val payload = CompanyProfileBackupPayload(
            exportedAt = System.currentTimeMillis(),
            profile = CompanyProfileBackupRecord.from(profile),
            logoMimeType = profile.logoUri?.let(::logoMimeType),
            logoBase64 = logoBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
        )
        context.contentResolver.openOutputStream(destination, "wt").use { output ->
            requireNotNull(output) { "Could not create the backup file." }
            output.writer(Charsets.UTF_8).use { it.write(adapter.toJson(payload)) }
        }
    }

    fun importFrom(source: Uri): CompanyProfileEntity {
        val json = context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Could not open the selected backup." }
            input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
        require(json.length <= MAX_JSON_CHARS) { "Backup is too large." }
        val payload = requireNotNull(adapter.fromJson(json)) { "Backup is empty or invalid." }
        require(payload.format == "archiman-company-profile" && payload.formatVersion == 1) { "This is not a supported ArchiMan company-profile backup." }
        require(payload.profile.practiceName.isNotBlank()) { "Backup does not contain a company name." }
        val logoUri = payload.logoBase64?.let { encoded ->
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            require(bytes.size <= MAX_LOGO_BYTES) { "Backup logo is too large." }
            val extension = if (payload.logoMimeType == "image/jpeg") "jpg" else "png"
            val target = File(logoDirectory, "profile-logo-import-${System.currentTimeMillis()}.$extension")
            target.writeBytes(bytes)
            Uri.fromFile(target).toString()
        }
        return payload.profile.toEntity(logoUri)
    }

    fun discardImportedLogo(value: String?) {
        val uri = value?.let(Uri::parse) ?: return
        val file = uri.path?.let(::File) ?: return
        if (uri.scheme == "file" && file.parentFile == logoDirectory && file.name.startsWith("profile-logo-import-")) file.delete()
    }

    private fun readLogoBytes(value: String): ByteArray? {
        val uri = Uri.parse(value)
        return if (uri.scheme == "file") File(requireNotNull(uri.path)).takeIf(File::exists)?.readBytes()
        else context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }

    private fun logoMimeType(value: String): String = when (Uri.parse(value).path?.substringAfterLast('.', "")?.lowercase()) {
        "jpg", "jpeg" -> "image/jpeg"
        "svg" -> "image/svg+xml"
        else -> "image/png"
    }

    companion object {
        private const val MAX_LOGO_BYTES = 5 * 1024 * 1024
        private const val MAX_JSON_CHARS = 8 * 1024 * 1024
    }
}
