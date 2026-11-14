package com.holagundi.archiman.portal

import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import org.json.JSONObject

/** Practice identity shown in the portal chrome (header, sign-in, accent). */
data class PortalBrand(
    val practiceName: String = "",
    val legalName: String = "",
    val logoDataUrl: String = "",
    val accent: String = DEFAULT_ACCENT,
) {
    val displayName: String
        get() = practiceName.ifBlank { legalName }.ifBlank { "Practice" }

    val hasLogo: Boolean
        get() = logoDataUrl.startsWith("data:image/")

    val safeAccent: String
        get() = accent.takeIf { it.matches(Regex("#[0-9a-fA-F]{6}")) } ?: DEFAULT_ACCENT

    companion object {
        const val DEFAULT_ACCENT = "#B83E16"
        val FALLBACK = PortalBrand()

        fun from(company: CompanyProfileEntity?, letterheadJson: String? = null): PortalBrand {
            val style = letterheadJson?.takeIf(String::isNotBlank)?.let(::JSONObject) ?: JSONObject()
            return PortalBrand(
                practiceName = company?.practiceName.orEmpty(),
                legalName = company?.legalName.orEmpty(),
                logoDataUrl = style.optString("logo"),
                accent = style.optString("accent", DEFAULT_ACCENT),
            )
        }

        suspend fun load(db: ArchiManDatabase): PortalBrand {
            val company = db.practiceDao().company()
            val letterhead = db.suiteDao().finance("letterhead")?.json
            return from(company, letterhead)
        }
    }
}
