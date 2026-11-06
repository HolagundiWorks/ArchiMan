package com.holagundi.archiman.data

import androidx.room.withTransaction
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.FeeProposalEntity
import com.holagundi.archiman.data.local.entity.WorkContractEntity
import com.holagundi.archiman.domain.*
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ConsultancyRecord(
    val id: Long, val projectId: Long, val agreement: Boolean, val title: String,
    val amount: Double, val status: String, val detailsJson: String, val documentText: String, val createdAt: Long,
) {
    val input: ConsultancyInput? get() = detailsJson.takeIf { it.isNotBlank() }?.let { runCatching { ConsultancyCodec.decode(it) }.getOrNull() }
}

@Singleton
class ConsultancyRepository @Inject constructor(private val db: ArchiManDatabase) {
    suspend fun branding() = db.practiceDao().company() to (db.suiteDao().finance("letterhead")?.let { org.json.JSONObject(it.json) } ?: org.json.JSONObject())
    private fun table(agreement: Boolean) = if (agreement) "work_contracts" else "fee_proposals"

    fun observe(projectId: Long, agreement: Boolean): Flow<List<ConsultancyRecord>> = if (agreement) {
        db.suiteDao().observeContracts(projectId).map { records -> records.map { ConsultancyRecord(it.id, it.projectId, true, it.title, it.amount, it.status, it.detailsJson, it.documentText, it.createdAt) } }
    } else {
        db.suiteDao().observeProposals(projectId).map { records -> records.map { ConsultancyRecord(it.id, it.projectId, false, it.title, it.amount, it.status, it.detailsJson, it.documentText, it.createdAt) } }
    }

    suspend fun read(projectId: Long, agreement: Boolean, id: Long): ConsultancyRecord = db.withTransaction {
        db.openHelper.writableDatabase.query("SELECT * FROM ${table(agreement)} WHERE id = ? AND projectId = ?", arrayOf(id, projectId)).use {
            require(it.moveToFirst()) { "Document no longer exists in this project." }
            ConsultancyRecord(id, projectId, agreement, it.getString(it.getColumnIndexOrThrow("title")), it.getDouble(it.getColumnIndexOrThrow("amount")),
                it.getString(it.getColumnIndexOrThrow("status")), it.getString(it.getColumnIndexOrThrow("detailsJson")), it.getString(it.getColumnIndexOrThrow("documentText")), it.getLong(it.getColumnIndexOrThrow("createdAt")))
        }
    }

    suspend fun newInput(projectId: Long, agreement: Boolean, service: ConsultancyService): ConsultancyInput = db.withTransaction {
        val project = requireNotNull(db.projectDao().getProject(projectId)) { "Project no longer exists." }
        val practice = db.practiceDao().company()
        val clientAddress = db.openHelper.writableDatabase.query("SELECT address FROM clients WHERE id = ?", arrayOf(project.clientId)).use { if (it.moveToFirst()) it.getString(0) else "" }
        fun complete(value: String?, missing: String) = value?.takeIf { it.isNotBlank() } ?: "[$missing]"
        val parties = """
            Project: ${project.name} (${project.projectCode})
            Site: ${complete(project.siteLocation, "enter site address")}
            Client: ${project.client}
            Client address: ${complete(clientAddress, "enter client address")}
            Practice: ${complete(practice?.legalName?.ifBlank { practice.practiceName }, "enter legal practice name")}
            Principal: ${complete(practice?.principalName, "enter architect name")}
            CoA registration: ${complete(practice?.coaRegistrationNumber, "enter CoA registration")}
            Practice address: ${complete(practice?.let { listOf(it.address, it.city, it.state, it.pinCode).filter(String::isNotBlank).joinToString(", ") }, "enter practice address")}
            Date: ${LocalDate.now()}
        """.trimIndent()
        val gst = db.suiteDao().finance("settings")?.let { org.json.JSONObject(it.json) }
        ConsultancyTemplates.create(service, parties).let { it.copy(gstMode = GstMode.valueOf(gst?.optString("mode") ?: "NONE"), gstRate = gst?.optString("rate") ?: "18", gstin = gst?.optString("gstin").orEmpty(), supplierState = gst?.optString("state").orEmpty(), title = "${service.label} ${if (agreement) "agreement" else "fee proposal"}") }
    }

    private suspend fun unchanged(expected: ConsultancyRecord): ConsultancyRecord {
        val current = read(expected.projectId, expected.agreement, expected.id)
        require(current == expected) { "This document changed. Reopen it before saving." }
        return current
    }

    suspend fun save(projectId: Long, agreement: Boolean, input: ConsultancyInput, expected: ConsultancyRecord? = null): Long = db.withTransaction {
        require(db.projectDao().getProject(projectId) != null) { "Project no longer exists." }
        ConsultancyTemplates.validate(input)
        val body = ConsultancyTemplates.render(input, agreement)
        val json = ConsultancyCodec.encode(input)
        val amount = ConsultancyTemplates.amount(input).toDouble()
        if (expected != null) {
            require(expected.projectId == projectId && expected.agreement == agreement) { "Document context does not match." }
            require(unchanged(expected).status == "DRAFT") { "Issued documents are immutable. Create a revision." }
            db.openHelper.writableDatabase.execSQL("UPDATE ${table(agreement)} SET title = ?, amount = ?, detailsJson = ?, documentText = ? WHERE id = ?", arrayOf(input.title.trim(), amount, json, body, expected.id))
            expected.id
        } else if (agreement) {
            val project = requireNotNull(db.projectDao().getProject(projectId))
            db.suiteDao().insertContract(WorkContractEntity(projectId = projectId, title = input.title.trim(), party = project.client, kind = input.service.label, amount = amount, status = "DRAFT", createdAt = System.currentTimeMillis(), detailsJson = json, documentText = body))
        } else {
            db.suiteDao().insertProposal(FeeProposalEntity(projectId = projectId, title = input.title.trim(), amount = amount, status = "DRAFT", createdAt = System.currentTimeMillis(), detailsJson = json, documentText = body))
        }
    }

    suspend fun issue(expected: ConsultancyRecord, reviewed: Boolean) = db.withTransaction {
        require(reviewed) { "Review scope, fees, terms and the payment schedule before issue." }
        val current = unchanged(expected)
        require(current.status == "DRAFT") { "Only a draft may be issued." }
        val input = requireNotNull(current.input) { "Prepare the document in the editor before issue." }
        ConsultancyTemplates.validate(input, forIssue = true)
        val body = ConsultancyTemplates.render(input, current.agreement, "ISSUED")
        db.openHelper.writableDatabase.execSQL("UPDATE ${table(current.agreement)} SET status = 'ISSUED', documentText = ? WHERE id = ?", arrayOf(body, current.id))
        db.practiceDao().insertAudit(com.holagundi.archiman.data.local.entity.PortalAuditEventEntity(
            userId = 0, username = "phone", action = "ISSUE_CONSULTANCY_DOCUMENT", entityType = table(current.agreement),
            entityId = current.id, projectId = current.projectId, summary = "Issued for acceptance after document review; not signed.", sourceAddress = "local", occurredAt = System.currentTimeMillis(),
        ))
    }

    suspend fun copy(expected: ConsultancyRecord, asAgreement: Boolean): Long = db.withTransaction {
        val current = unchanged(expected)
        val input = requireNotNull(current.input) { "Prepare this legacy document in the editor first." }
        val revised = input.copy(
            title = if (asAgreement && !current.agreement) "${input.service.label} agreement" else "${input.title} (revision)",
            proposalReference = if (asAgreement && !current.agreement) "#${current.id} · ${current.title}" else input.proposalReference,
        )
        save(current.projectId, asAgreement, revised)
    }

    suspend fun delete(expected: ConsultancyRecord) = db.withTransaction {
        unchanged(expected)
        val operations = RecordOperations(db)
        val kind = if (expected.agreement) RecordKind.CONTRACT else RecordKind.PROPOSAL
        operations.delete(operations.read(kind, expected.id))
    }
}
