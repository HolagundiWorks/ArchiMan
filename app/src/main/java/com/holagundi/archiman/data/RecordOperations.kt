package com.holagundi.archiman.data

import android.database.Cursor
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.domain.InvoiceBalance
import com.holagundi.archiman.domain.MeasurementInput
import com.holagundi.archiman.domain.QuantityCalculator
import javax.inject.Inject
import javax.inject.Singleton

data class RecordField(val column: String, val label: String, val required: Boolean = false, val number: Boolean = false)

private fun field(column: String, label: String, required: Boolean = false, number: Boolean = false) = RecordField(column, label, required, number)

/** Explicit allowlist. Relationship choices are validated separately; workflow states stay out of free-text forms. */
enum class RecordKind(val table: String, val fields: List<RecordField>) {
    PROJECT("projects", listOf(field("name", "Project name", true), field("projectCode", "Project code", true), field("clientId", "Client", true), field("siteLocation", "Site location"), field("projectType", "Project type", true), field("description", "Description"), field("architectInCharge", "Architect in charge"))),
    CLIENT("clients", listOf(field("name", "Client name", true), field("contactPerson", "Contact person"), field("contactNo", "Phone"), field("email", "Email"), field("address", "Address"), field("correspondenceAddress", "Correspondence address"), field("notes", "Notes"))),
    PRACTICE("company_profile", listOf(field("practiceName", "Practice name", true), field("legalName", "Legal name"), field("principalName", "Principal name"), field("principalQualification", "Qualification"), field("address", "Address"), field("city", "City"), field("state", "State"), field("pinCode", "Postal code"), field("phone", "Phone"), field("email", "Email"), field("website", "Website"), field("coaRegistrationNumber", "CoA registration"), field("pan", "PAN"), field("gstin", "GSTIN"))),
    LEVEL("floors", listOf(field("name", "Level name", true))),
    ROOM("rooms", listOf(field("name", "Room name", true))),
    TASK("project_tasks", listOf(field("title", "Task", true), field("description", "Description"))),
    LETTER("letters", listOf(field("subject", "Subject", true), field("body", "Body", true))),
    PROPOSAL("fee_proposals", listOf(field("title", "Proposal", true), field("amount", "Fee", true, true))),
    CONTRACT("work_contracts", listOf(field("title", "Contract", true), field("party", "Party", true), field("amount", "Amount", true, true))),
    INVOICE("invoices", listOf(field("title", "Invoice for", true), field("amount", "Amount", true, true))),
    VISIT("site_inspections", listOf(field("location", "Location"), field("inspector", "Inspector"), field("observation", "Report", true), field("correctiveAction", "Corrective action"))),
    LOG("daily_site_reports", listOf(field("weather", "Weather"), field("preparedBy", "Prepared by"), field("manpower", "Manpower"), field("workCompleted", "Work completed", true), field("materialsReceived", "Materials received"), field("delaysOrConstraints", "Delays"), field("safetyObservations", "Safety notes"), field("nextDayPlan", "Next day plan"))),
    CONTRACTOR("contractors", listOf(field("name", "Contractor name", true), field("address", "Address"), field("contactNo", "Contact number"), field("phone", "Phone"), field("contractorType", "Trade", true))),
    SNAG("site_issues", listOf(field("title", "Title", true), field("location", "Location"), field("description", "Description", true), field("assignedTo", "Assigned to"), field("correctiveAction", "Corrective action"))),
    COORDINATION("coordination_items", listOf(field("subject", "Subject", true), field("discipline", "Discipline"), field("location", "Location"), field("raisedBy", "Raised by"), field("assignedTo", "Assigned to"), field("questionOrRequirement", "Question or requirement", true))),
    DRAWING("project_drawings", listOf(field("drawingNumber", "Drawing number", true), field("title", "Title", true), field("discipline", "Discipline"), field("packageTag", "Package tag"))),
    REVISION("drawing_revisions", listOf(field("revisionCode", "Revision code", true), field("revisionNotes", "Revision notes"))),
    TRANSMITTAL("drawing_transmittals", emptyList()),
    ORDER("material_orders", listOf(field("title", "Order title", true), field("supplier", "Supplier"))),
    ORDER_LINE("material_order_lines", listOf(field("itemName", "Item", true), field("specification", "Specification"), field("quantity", "Quantity", true, true), field("unit", "Unit", true))),
    MEASUREMENT("measurements", listOf(field("description", "Description"), field("location", "Location"), field("nos", "Nos", true, true), field("length", "Length", true, true), field("width", "Breadth", true, true), field("height", "Height", true, true), field("deduction", "Deduction", true, true), field("remarks", "Remarks"))),
    SHEET("measurement_sheets", emptyList()),
    RATE("rate_book_items", listOf(field("itemName", "Item", true), field("unit", "Unit", true), field("rate", "Rate", true, true))),
    MEETING("meeting_minutes", listOf(field("title", "Title", true), field("attendees", "Attendees"), field("discussion", "Discussion"), field("decisions", "Decisions"), field("actionItems", "Action items"))),
    TIME("time_entries", listOf(field("person", "Person", true), field("minutes", "Minutes", true, true), field("notes", "Notes"))),
    APPROVAL("project_approvals", listOf(field("title", "Title", true))),
    ACTIVITY("schedule_activities", listOf(field("title", "Activity", true))),
    ENQUIRY("enquiries", listOf(field("clientName", "Client", true), field("source", "Source"), field("notes", "Notes"))),
}

data class RecordSnapshot(val kind: RecordKind, val id: Long, val values: Map<String, Any?>) {
    fun text(column: String) = values[column]?.toString().orEmpty()
}

@Singleton
class RecordOperations @Inject constructor(private val db: ArchiManDatabase) {
    suspend fun saveWithSnapshot(snapshot: RecordSnapshot, fields: Map<String, String>): RecordSnapshot = db.withTransaction {
        save(snapshot, fields)
        readNow(snapshot.kind, snapshot.id)
    }
    suspend fun archiveWithSnapshot(snapshot: RecordSnapshot): RecordSnapshot = db.withTransaction {
        archiveProject(snapshot)
        readNow(snapshot.kind, snapshot.id)
    }
    private fun quote(value: String) = "\"${value.replace("\"", "\"\"")}\""
    private val sql get() = db.openHelper.writableDatabase

    suspend fun clientChoices(): List<Pair<Long, String>> = db.withTransaction {
        sql.query("SELECT id, name, contactNo FROM clients ORDER BY name").use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getLong(0) to listOf(cursor.getString(1), cursor.getString(2)).filter { it.isNotBlank() }.joinToString(" · ")) }
        }
    }

    suspend fun read(kind: RecordKind, id: Long): RecordSnapshot = db.withTransaction { readNow(kind, id) }

    suspend fun ensureAllowed(snapshot: RecordSnapshot, deleting: Boolean) = db.withTransaction {
        requireMutable(checked(snapshot), deleting)
    }

    private fun readNow(kind: RecordKind, id: Long): RecordSnapshot = sql.query("SELECT * FROM ${quote(kind.table)} WHERE id = ?", arrayOf(id)).use { cursor ->
        require(cursor.moveToFirst()) { "This record no longer exists." }
        RecordSnapshot(kind, id, cursor.columnNames.mapIndexed { index, column ->
            column to when (cursor.getType(index)) {
                Cursor.FIELD_TYPE_NULL -> null
                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(index)
                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(index)
                else -> cursor.getString(index)
            }
        }.toMap())
    }

    private fun checked(snapshot: RecordSnapshot): RecordSnapshot {
        val current = readNow(snapshot.kind, snapshot.id)
        require(current.values == snapshot.values) { "This record changed. Close and open the editor again." }
        return current
    }

    suspend fun save(snapshot: RecordSnapshot, inputs: Map<String, String>) = db.withTransaction {
        val record = checked(snapshot)
        requireMutable(record, false)
        require(inputs.keys == record.kind.fields.map { it.column }.toSet()) { "Editor fields do not match this record." }
        val values = record.kind.fields.associate { field ->
            val text = inputs.getValue(field.column).trim()
            require(!field.required || text.isNotBlank()) { "${field.label} is required." }
            val value: Any = if (field.column == "clientId") {
                requireNotNull(text.toLongOrNull()) { "Choose a client." }
            } else if (field.number) {
                val number = text.toDoubleOrNull()
                require(number != null && number.isFinite() && number >= 0) { "${field.label} must be a valid non-negative number." }
                if (field.column in setOf("amount", "rate")) InvoiceBalance.money(number).toDouble() else number
            } else text
            field.column to value
        }.toMutableMap()
        if (record.kind == RecordKind.PROJECT) {
            val clientName = sql.query("SELECT name FROM clients WHERE id = ?", arrayOf(values.getValue("clientId"))).use {
                require(it.moveToFirst()) { "The selected client no longer exists." }
                it.getString(0)
            }
            values["client"] = clientName
        }
        if (record.kind == RecordKind.TIME) {
            val minutes = values.getValue("minutes") as Double
            require(minutes % 1.0 == 0.0 && minutes <= Int.MAX_VALUE) { "Minutes must be a whole number." }
        }
        if (record.kind == RecordKind.INVOICE || record.kind == RecordKind.PROPOSAL || record.kind == RecordKind.CONTRACT) {
            require((values.getValue("amount") as Double) > 0) { "Amount must be greater than zero." }
        }
        if (record.kind == RecordKind.MEASUREMENT) {
            val quantity = QuantityCalculator.calculate(record.text("calculationType"), MeasurementInput(
                no = values.getValue("nos") as Double, length = values.getValue("length") as Double,
                breadth = values.getValue("width") as Double, height = values.getValue("height") as Double, deduction = values.getValue("deduction") as Double,
            ))
            require(quantity.isFinite() && quantity > 0) { "Dimensions must produce a positive quantity." }
            values["quantity"] = quantity
            sql.execSQL("UPDATE measurement_sheets SET updatedAt = ? WHERE id = ?", arrayOf(System.currentTimeMillis(), record.values.getValue("sheetId")))
        }
        if ("updatedAt" in record.values) values["updatedAt"] = System.currentTimeMillis()
        when (record.kind) {
            RecordKind.PROJECT -> require(count("projects", "projectCode = ? COLLATE NOCASE AND id != ?", arrayOf(values.getValue("projectCode"), record.id)) == 0L) { "Another project already uses this code." }
            RecordKind.LEVEL -> require(count("floors", "projectId = ? AND name = ? COLLATE NOCASE AND id != ?", arrayOf(record.values.getValue("projectId"), values.getValue("name"), record.id)) == 0L) { "This project already has a level with that name." }
            RecordKind.ROOM -> require(count("rooms", "floorId = ? AND name = ? COLLATE NOCASE AND id != ?", arrayOf(record.values.getValue("floorId"), values.getValue("name"), record.id)) == 0L) { "This level already has a room with that name." }
            RecordKind.DRAWING -> require(count("project_drawings", "projectId = ? AND drawingNumber = ? COLLATE NOCASE AND id != ?", arrayOf(record.values.getValue("projectId"), values.getValue("drawingNumber"), record.id)) == 0L) { "This drawing number already exists." }
            else -> Unit
        }
        update(record, values)
        if (record.kind == RecordKind.CLIENT) {
            sql.execSQL("UPDATE projects SET client = ? WHERE clientId = ?", arrayOf(values.getValue("name"), record.id))
        }
        audit(record, "EDIT", "Record details updated")
    }

    suspend fun delete(snapshot: RecordSnapshot) = db.withTransaction {
        val record = checked(snapshot)
        requireMutable(record, true)
        if (record.kind == RecordKind.PROJECT) {
            // Older imports may share a project-owned contractor with another project.
            // Preserve that contractor as a shared directory entry before deleting owners.
            sql.execSQL("""UPDATE contractors SET projectId = 0 WHERE projectId = ? AND (
                EXISTS (SELECT 1 FROM measurements m WHERE m.contractorId = contractors.id AND m.projectId != ?)
                OR EXISTS (SELECT 1 FROM measurement_sheets s WHERE s.contractorId = contractors.id AND s.projectId != ?)
                OR EXISTS (SELECT 1 FROM project_contractors p WHERE p.contractorId = contractors.id AND p.projectId != ?))""", arrayOf(record.id, record.id, record.id, record.id))
            // Some older tables (including letters and contractors) have no project FK.
            // Delete every project-owned row explicitly; normal FK cascades remove child-only rows.
            sql.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").use { cursor ->
                val tables = buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                tables.filter { it != "projects" && it != "portal_audit_events" }.forEach { table ->
                    val hasProject = sql.query("PRAGMA table_info(${quote(table)})").use { columns ->
                        var found = false
                        while (columns.moveToNext()) if (columns.getString(1) == "projectId") found = true
                        found
                    }
                    if (hasProject) sql.execSQL("DELETE FROM ${quote(table)} WHERE projectId = ?", arrayOf(record.id))
                }
            }
        }
        sql.execSQL("DELETE FROM ${quote(record.kind.table)} WHERE id = ?", arrayOf(record.id))
        audit(record, "DELETE", "Record deleted")
    }

    suspend fun archiveProject(snapshot: RecordSnapshot) = db.withTransaction {
        val record = checked(snapshot)
        require(record.kind == RecordKind.PROJECT)
        update(record, mapOf("status" to if (record.text("status") == "ARCHIVED") "ACTIVE" else "ARCHIVED"))
        audit(record, "ARCHIVE", "Project archive status changed")
    }

    suspend fun voidInvoice(snapshot: RecordSnapshot, reason: String) = db.withTransaction {
        val record = checked(snapshot)
        require(record.kind == RecordKind.INVOICE && record.text("status") == "ISSUED") { "Only an issued, unpaid invoice can be voided." }
        require(reason.isNotBlank()) { "A reason is required." }
        val paid = db.suiteDao().invoiceReceipts(record.id).map { it.amount + it.tdsAmount }
        require(InvoiceBalance.outstanding(0.0, paid).signum() == 0) { "Reverse the payments before voiding this invoice." }
        update(record, mapOf("status" to "VOID"))
        audit(record, "VOID", reason.trim())
    }

    suspend fun reverseReceipt(id: Long, reason: String) = db.withTransaction {
        require(reason.isNotBlank()) { "A reason is required." }
        val receipt = sql.query("SELECT invoiceId FROM receipts WHERE id = ?", arrayOf(id)).use {
            require(it.moveToFirst()) { "Receipt no longer exists." }; db.suiteDao().invoiceReceipts(it.getLong(0)).first { r -> r.id == id }
        }
        require(receipt.amount > 0 || receipt.tdsAmount > 0) { "A reversal cannot itself be reversed." }
        require(count("receipts", "note LIKE ?", arrayOf("REVERSAL:$id:%")) == 0L) { "This receipt has already been reversed." }
        val invoice = requireNotNull(db.suiteDao().invoice(receipt.invoiceId))
        require(invoice.status != "VOID") { "This invoice is void." }
        db.suiteDao().insertReceipt(receipt.copy(id = 0, amount = -receipt.amount, tdsAmount = -receipt.tdsAmount, tdsVerified = false, note = "REVERSAL:$id: ${reason.trim()}", receivedAt = System.currentTimeMillis()))
        if (receipt.bankKey.isNotBlank()) db.suiteDao().finance(receipt.bankKey)?.let { bank ->
            val body = org.json.JSONObject(bank.json).put("status", "UNMATCHED"); body.remove("invoiceId")
            db.suiteDao().saveFinance(bank.copy(json = body.toString()))
        }
        val remaining = InvoiceBalance.outstanding(invoice.amount, db.suiteDao().invoiceReceipts(invoice.id).map { it.amount + it.tdsAmount })
        db.suiteDao().setInvoiceStatus(invoice.id, if (remaining.signum() > 0) "ISSUED" else "PAID")
        audit(readNow(RecordKind.INVOICE, invoice.id), "REVERSE_RECEIPT", "Receipt $id: ${reason.trim()}")
    }

    private fun requireMutable(record: RecordSnapshot, deleting: Boolean) {
        if (!deleting && record.kind == RecordKind.INVOICE && record.text("taxJson").isNotBlank()) error("Taxed invoice snapshots retain their calculation. Delete this draft and create a corrected invoice.")
        if (!deleting && record.kind in listOf(RecordKind.PROPOSAL, RecordKind.CONTRACT) && record.text("detailsJson").isNotBlank()) {
            require(false) { "Use the proposal/agreement editor so scope, fee calculation and document text stay consistent." }
        }
        val status = record.text("status")
        when (record.kind) {
            RecordKind.PRACTICE -> require(!deleting) { "The practice profile cannot be deleted." }
            RecordKind.INVOICE, RecordKind.PROPOSAL, RecordKind.CONTRACT, RecordKind.LETTER -> require(status == "DRAFT") { "Only drafts can be edited or deleted. Issued records retain their history." }
            RecordKind.TRANSMITTAL -> error("Issued transmittals retain their history. Create a new transmittal to correct one.")
            RecordKind.REVISION -> require(record.text("issueStatus") == "DRAFT") { "Issued revisions retain their history. Add a new revision." }
            RecordKind.SHEET -> {
                require(deleting) { "Edit individual measurement entries instead." }
                require(status in listOf("DRAFT", "RETURNED")) { "Only draft or returned sheets can be deleted." }
            }
            RecordKind.MEASUREMENT -> {
                val sheet = readNow(RecordKind.SHEET, (record.values.getValue("sheetId") as Number).toLong())
                val sheetStatus = sheet.text("status")
                require(sheet.values["archivedAt"] == null) { "Archived measurement sheets retain their history." }
                if (deleting) {
                    require(sheetStatus != "APPROVED") { "Approved measurement entries cannot be deleted." }
                } else {
                    require(sheetStatus in listOf("DRAFT", "RETURNED")) { "Only draft or returned measurement entries can be edited." }
                }
            }
            RecordKind.SNAG -> {
                require(status != "CLOSED") { "Verified issues retain their history." }
                if (deleting) require(status == "OPEN" && count("site_issue_events", "siteIssueId = ?", arrayOf(record.id)) == 0L) { "An issue with workflow history cannot be deleted." }
            }
            RecordKind.COORDINATION -> {
                require(status in listOf("DRAFT", "OPEN", "RECEIVED")) { "Reviewed coordination records retain their history." }
                if (deleting) require(status in listOf("DRAFT", "RECEIVED") && count("coordination_events", "coordinationItemId = ?", arrayOf(record.id)) == 0L) { "Only unprocessed coordination records can be deleted." }
            }
            RecordKind.ORDER -> require(status != "RECEIVED") { "Received orders retain their history." }
            RecordKind.ORDER_LINE -> {
                val order = readNow(RecordKind.ORDER, (record.values.getValue("orderId") as Number).toLong())
                require(order.text("status") != "RECEIVED") { "Received order lines retain their history." }
            }
            RecordKind.APPROVAL -> require(status != "APPROVED") { "Approved records retain their history." }
            else -> Unit
        }
        if (!deleting) return
        if (record.kind in listOf(RecordKind.PROJECT,RecordKind.CONTRACTOR)) {
            sql.query("SELECT json FROM finance_records WHERE kind IN ('BOQ_REVISION','ESTIMATE','CONTRACTOR_BILL')").use { cursor ->
                while (cursor.moveToNext()) {
                    val json=org.json.JSONObject(cursor.getString(0))
                    val linked = if(record.kind==RecordKind.PROJECT) json.optLong("projectId")==record.id else json.optLong("contractorId")==record.id || json.optJSONArray("lines")?.let { lines -> (0 until lines.length()).any { lines.getJSONObject(it).optLong("contractorId")==record.id } } == true
                    require(!linked) { "This record has project schedule or estimate history. Keep it and archive the project instead." }
                }
            }
        }
        when (record.kind) {
            RecordKind.PROJECT -> require(count("receipts", "projectId = ? AND bankKey <> ''", arrayOf(record.id)) == 0L) { "Projects with bank reconciliation history must be archived to retain the bank ledger." }
            RecordKind.CLIENT -> require(count("projects", "clientId = ?", arrayOf(record.id)) == 0L) { "This client is linked to a project. Delete or reassign the project first." }
            RecordKind.LEVEL -> listOf("rooms", "measurements", "measurement_sheets", "components").forEach {
                require(count(it, "floorId = ?", arrayOf(record.id)) == 0L) { "This level has rooms or site records. Remove those first." }
            }
            RecordKind.ROOM -> listOf("measurements", "components").forEach {
                require(count(it, "roomId = ?", arrayOf(record.id)) == 0L) { "This room has site records. Remove those first." }
            }
            RecordKind.DRAWING -> require(count("drawing_revisions", "drawingId = ?", arrayOf(record.id)) == 0L) { "This drawing has revisions. Keep its history and add a corrected revision." }
            RecordKind.CONTRACTOR -> listOf("measurements", "measurement_sheets", "project_contractors").forEach {
                require(count(it, "contractorId = ?", arrayOf(record.id)) == 0L) { "This contractor is linked to project or measurement records." }
            }
            RecordKind.INVOICE -> require(count("receipts", "invoiceId = ?", arrayOf(record.id)) == 0L) { "Invoices with receipts cannot be deleted." }
            else -> Unit
        }
    }

    private fun count(table: String, where: String, args: Array<out Any?>): Long = sql.query("SELECT COUNT(*) FROM ${quote(table)} WHERE $where", args).use { it.moveToFirst(); it.getLong(0) }
    private fun update(record: RecordSnapshot, values: Map<String, Any>) {
        require(values.isNotEmpty()) { "This record has no editable fields." }
        sql.execSQL("UPDATE ${quote(record.kind.table)} SET ${values.keys.joinToString(",") { "${quote(it)} = ?" }} WHERE id = ?", (values.values.toList() + record.id).toTypedArray())
    }

    private suspend fun audit(record: RecordSnapshot, action: String, summary: String) {
        db.practiceDao().insertAudit(com.holagundi.archiman.data.local.entity.PortalAuditEventEntity(
            userId = 0, username = "phone", action = action, entityType = record.kind.table, entityId = record.id,
            projectId = if (record.kind == RecordKind.PROJECT) record.id else (record.values["projectId"] as? Number)?.toLong(),
            summary = summary, sourceAddress = "local", occurredAt = System.currentTimeMillis(),
        ))
    }
}
