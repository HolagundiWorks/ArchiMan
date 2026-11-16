package com.holagundi.archiman.company

import org.json.JSONObject

data class CompanyClient(val id: String, val name: String, val kind: String, val contactPerson: String,
    val email: String, val phone: String, val address: String, val correspondenceAddress: String,
    val preferredCommunication: String, val notes: String, val version: Int)
data class CompanyClientPage(val clients: List<CompanyClient>, val nextCursor: String?)

internal fun readCompanyClients(page: JSONObject, after: String?): CompanyClientPage {
    val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    fun invalid(): Nothing = throw CompanyApiException("INVALID_RESPONSE")
    val rows = page.getJSONArray("clients")
    if (rows.length() > 25) invalid()
    val clients = (0 until rows.length()).map { index ->
        val row = rows.getJSONObject(index)
        fun text(key: String, max: Int): String = row.getString(key).also { if (it.length > max) invalid() }
        val id = row.getString("id"); if (!uuid.matches(id)) invalid()
        val name = text("name", 240); if (name.isBlank()) invalid()
        val kind = row.getString("kind"); if (kind !in setOf("individual", "company", "architect_firm")) invalid()
        val version = row.getLong("version"); if (version !in 1..Int.MAX_VALUE.toLong()) invalid()
        CompanyClient(id, name, kind, text("contactPerson",240), text("email",254), text("phone",80),
            text("address",2000), text("correspondenceAddress",2000), text("preferredCommunication",120), text("notes",4000), version.toInt())
    }
    if (clients.map { it.id }.distinct().size != clients.size) invalid()
    val cursor = if (page.isNull("nextCursor")) null else page.getString("nextCursor")
    if (cursor != null && (!uuid.matches(cursor) || cursor == after || clients.lastOrNull()?.id != cursor || clients.size != 25)) invalid()
    return CompanyClientPage(clients,cursor)
}
