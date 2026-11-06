package com.holagundi.archiman

import com.holagundi.archiman.data.ProjectRepository
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.data.RecordOperations
import com.holagundi.archiman.data.RecordSnapshot
import com.holagundi.archiman.data.SuiteRepository
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.InvoiceEntity

object RecordDeviceChecks {
    suspend fun run(db: ArchiManDatabase, suite: SuiteRepository) {
        val operations = RecordOperations(db)
        val sql = db.openHelper.writableDatabase
        RecordKind.entries.forEach { kind ->
            val columns = sql.query("SELECT * FROM ${kind.table} LIMIT 0").use { it.columnNames.toSet() }
            check(kind.fields.all { it.column in columns }) { "Editor has unknown columns: ${kind.name}" }
        }
        fun inputs(record: RecordSnapshot) = record.kind.fields.associate { it.column to record.text(it.column) }
        fun scalar(query: String): Long = sql.query(query).use { it.moveToFirst(); it.getLong(0) }
        suspend fun rejects(block: suspend () -> Unit) {
            var rejected = false
            try { block() } catch (_: IllegalArgumentException) { rejected = true }
            check(rejected) { "A protected operation was incorrectly allowed." }
        }

        val projects = ProjectRepository(db, db.projectDao(), db.practiceDao())
        val id = projects.createProject("Operations test", "OPS-DEVICE", "Operations client", "Residential", "Test site", "Architect")
        var project = operations.read(RecordKind.PROJECT, id)
        operations.save(project, inputs(project) + ("name" to "Renamed project"))
        check(db.projectDao().getProject(id)!!.name == "Renamed project")
        rejects { operations.save(project, inputs(project)) }
        project = operations.read(RecordKind.PROJECT, id)
        operations.archiveProject(project)
        check(db.projectDao().getProject(id)!!.status == "ARCHIVED")
        operations.archiveProject(operations.read(RecordKind.PROJECT, id))
        check(db.projectDao().getProject(id)!!.status == "ACTIVE")

        val clientId = db.projectDao().getProject(id)!!.clientId
        val client = operations.read(RecordKind.CLIENT, clientId)
        operations.save(client, inputs(client) + ("name" to "Renamed client"))
        check(db.projectDao().getProject(id)!!.client == "Renamed client")
        rejects { operations.delete(operations.read(RecordKind.CLIENT, clientId)) }

        suite.addTask(id, "Task to edit")
        val taskId = scalar("SELECT id FROM project_tasks WHERE projectId = $id LIMIT 1")
        val task = operations.read(RecordKind.TASK, taskId)
        rejects { operations.save(task, inputs(task) + ("title" to "")) }
        operations.save(task, inputs(task) + ("title" to "Edited task"))
        operations.delete(operations.read(RecordKind.TASK, taskId))
        check(scalar("SELECT COUNT(*) FROM project_tasks WHERE id = $taskId") == 0L)

        val levelId = scalar("SELECT id FROM floors WHERE projectId = $id LIMIT 1")
        projects.addRoom(id, levelId, "Protected room")
        rejects { operations.delete(operations.read(RecordKind.LEVEL, levelId)) }
        val roomId = scalar("SELECT id FROM rooms WHERE projectId = $id LIMIT 1")
        operations.delete(operations.read(RecordKind.ROOM, roomId))
        operations.delete(operations.read(RecordKind.LEVEL, levelId))

        val draftId = db.suiteDao().insertInvoice(InvoiceEntity(projectId = id, stageId = null, number = "OPS-INVOICE", title = "Draft", amount = 100.0, status = "DRAFT", createdAt = 1))
        var invoice = operations.read(RecordKind.INVOICE, draftId)
        rejects { operations.save(invoice, inputs(invoice) + ("amount" to "NaN")) }
        operations.save(invoice, inputs(invoice) + ("title" to "Edited draft"))
        suite.issueInvoice(draftId)
        invoice = operations.read(RecordKind.INVOICE, draftId)
        rejects { operations.save(invoice, inputs(invoice)) }
        rejects { operations.delete(invoice) }
        suite.recordReceipt(id, db.suiteDao().invoice(draftId)!!, 40.0, "Operations receipt")
        rejects { operations.voidInvoice(operations.read(RecordKind.INVOICE, draftId), "Correction") }
        val receiptId = db.suiteDao().invoiceReceipts(draftId).single().id
        operations.reverseReceipt(receiptId, "Entered against wrong invoice")
        check(db.suiteDao().invoiceReceipts(draftId).sumOf { it.amount } == 0.0)
        check(db.suiteDao().invoice(draftId)!!.status == "ISSUED")
        rejects { operations.reverseReceipt(receiptId, "Duplicate reversal") }
        operations.voidInvoice(operations.read(RecordKind.INVOICE, draftId), "Corrected document follows")
        check(db.suiteDao().invoice(draftId)!!.status == "VOID")

        val rowId = scalar("SELECT id FROM measurements LIMIT 1")
        val row = operations.read(RecordKind.MEASUREMENT, rowId)
        val sheetId = (row.values.getValue("sheetId") as Number).toLong()
        sql.execSQL("UPDATE measurement_sheets SET status = 'DRAFT' WHERE id = ?", arrayOf(sheetId))
        operations.save(row, inputs(row) + mapOf("nos" to "2", "length" to "2", "width" to "2", "height" to "2", "deduction" to "0"))
        val edited = operations.read(RecordKind.MEASUREMENT, rowId)
        check((edited.values.getValue("quantity") as Number).toDouble() > 0)
        sql.execSQL("UPDATE measurement_sheets SET status = 'APPROVED' WHERE id = ?", arrayOf(sheetId))
        rejects { operations.save(edited, inputs(edited)) }
        rejects { operations.delete(edited) }
        rejects { operations.delete(operations.read(RecordKind.SHEET, sheetId)) }

        // Exercise deletion of a populated demo project, including legacy non-FK rows.
        val populatedId = scalar("SELECT projectId FROM measurements LIMIT 1")
        suite.addLetter(populatedId, "Project-owned letter", "Delete with parent")
        val clientsBefore = scalar("SELECT COUNT(*) FROM clients")
        val libraryBefore = scalar("SELECT COUNT(*) FROM item_master")
        val survivor = db.projectDao().getProject(id)!!
        operations.delete(operations.read(RecordKind.PROJECT, populatedId))
        check(db.projectDao().getProject(populatedId) == null)
        sql.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)
                if (name == "portal_audit_events") continue
                val columns = sql.query("SELECT * FROM \"$name\" LIMIT 0").use { it.columnNames.toSet() }
                if ("projectId" in columns) check(scalar("SELECT COUNT(*) FROM \"$name\" WHERE projectId = $populatedId") == 0L) { "Project deletion left rows in $name" }
            }
        }
        check(scalar("SELECT COUNT(*) FROM clients") == clientsBefore)
        check(scalar("SELECT COUNT(*) FROM item_master") == libraryBefore)
        check(db.projectDao().getProject(id) == survivor)
        sql.query("PRAGMA foreign_key_check").use { check(!it.moveToFirst()) }
        suite.ensureDefaults()
        val rates = sql.query("SELECT id FROM rate_book_items").use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.getLong(0)) } }
        rates.forEach { operations.delete(operations.read(RecordKind.RATE, it)) }
        suite.ensureDefaults()
        check(scalar("SELECT COUNT(*) FROM rate_book_items") == 0L) { "Deleted rates were unexpectedly reseeded." }
    }
}
