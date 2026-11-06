package com.holagundi.archiman

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.os.Bundle
import androidx.room.Room
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.holagundi.archiman.data.ConsultancyRepository
import com.holagundi.archiman.data.DemoCompanyStore
import com.holagundi.archiman.data.FinanceRepository
import com.holagundi.archiman.data.MeasurementRepository
import com.holagundi.archiman.data.PracticeRepository
import com.holagundi.archiman.data.ProjectRepository
import com.holagundi.archiman.data.RecordKind
import com.holagundi.archiman.data.RecordOperations
import com.holagundi.archiman.data.SuiteRepository
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.domain.ConsultancyFeeBasis
import com.holagundi.archiman.domain.ConsultancyInput
import com.holagundi.archiman.domain.ConsultancyMilestone
import com.holagundi.archiman.domain.ConsultancyService
import com.holagundi.archiman.domain.ConsultancyTemplates
import com.holagundi.archiman.domain.FinanceMath
import com.holagundi.archiman.domain.GstMode
import com.holagundi.archiman.domain.PasswordEnvelope
import com.holagundi.archiman.domain.WorkItemLibrary
import com.holagundi.archiman.portal.PortalServer
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

/**
 * Verifies every create/edit prompt rejects blank/invalid input with a clear message
 * and accepts a well-formed value. Also checks RecordKind field labels and key UI form labels.
 *
 * Invoke: `am instrument -w -e suite inputPrompts …/BackupDeviceChecks`
 */
object InputPromptDeviceChecks {
    suspend fun run(context: Context, results: Bundle) {
        val directory = File(context.filesDir, "input-prompt-check-${UUID.randomUUID()}").apply { mkdirs() }
        val isolated = object : android.content.ContextWrapper(context) {
            override fun getFilesDir(): File = directory
            override fun getNoBackupFilesDir(): File = File(directory, "no-backup").apply { mkdirs() }
        }
        val db = Room.inMemoryDatabaseBuilder(context, ArchiManDatabase::class.java).build()
        val report = JSONArray()
        val failures = mutableListOf<String>()
        fun pass(id: String, detail: String) = report.put(JSONObject().put("id", id).put("status", "PASS").put("detail", detail))
        fun fail(id: String, detail: String) {
            failures += "$id: $detail"
            report.put(JSONObject().put("id", id).put("status", "FAIL").put("detail", detail))
        }
        suspend fun rejects(id: String, expected: String? = null, block: suspend () -> Unit) {
            val error = runCatching { block() }.exceptionOrNull()
            when {
                error == null -> fail(id, "Accepted invalid input")
                error !is IllegalArgumentException -> fail(id, "Wrong failure type: ${error::class.java.simpleName} ${error.message}")
                expected != null && error.message?.contains(expected, ignoreCase = true) != true ->
                    fail(id, "Message «${error.message}» missing «$expected»")
                else -> pass(id, error.message.orEmpty())
            }
        }
        suspend fun accepts(id: String, block: suspend () -> Unit) {
            val error = runCatching { block() }.exceptionOrNull()
            if (error == null) pass(id, "Accepted valid input")
            else fail(id, "Rejected valid input: ${error.message}")
        }

        try {
            DemoCompanyStore(isolated, db).openBuiltIn()
            val projectId = db.projectDao().projects().first().id
            val suite = SuiteRepository(db, db.suiteDao(), db.practiceDao(), db.projectDao(), db.catalogDao(), db.measurementDao(), MeasurementRepository(db, db.measurementDao(), db.catalogDao(), db.projectDao()))
            val practice = PracticeRepository(db.practiceDao())
            val projects = ProjectRepository(db, db.projectDao(), db.practiceDao())
            val finance = FinanceRepository(db, suite)
            val operations = RecordOperations(db)

            // --- RecordKind prompt formulation (labels, required flags, uniqueness) ---
            RecordKind.entries.forEach { kind ->
                val labels = kind.fields.map { it.label }
                if (labels.any { it.isBlank() }) fail("form.${kind.name}.labels", "Blank field label")
                else if (labels.size != labels.distinct().size) fail("form.${kind.name}.labels", "Duplicate labels $labels")
                else if (kind.fields.map { it.column }.size != kind.fields.map { it.column }.distinct().size)
                    fail("form.${kind.name}.columns", "Duplicate columns")
                else pass("form.${kind.name}.labels", "${kind.fields.size} fields · ${kind.fields.count { it.required }} required")
                kind.fields.filter { it.required }.forEach { field ->
                    if (field.label.isBlank()) fail("form.${kind.name}.${field.column}", "Required field has blank label")
                }
            }

            // --- Practice / project create prompts ---
            rejects("prompt.client.blank", "client name") { practice.addClient("  ", "", "") }
            rejects("prompt.client.empty", "client name") { practice.addClient("", "9", "a@b.c") }
            accepts("prompt.client.valid") { practice.addClient(" Prompt Client ", "999", "prompt@example.com") }
            check(practice.observeClients().first().any { it.name == "Prompt Client" && it.contactNo == "999" })
            pass("prompt.client.trim", "Whitespace trimmed on save")

            rejects("prompt.project.blankName", "project name") { projects.createProject(" ", "X", "Client", "Residential", "Site", "Architect") }
            rejects("prompt.project.blankClient", "client name") { projects.createProject("Name", "X", "  ", "Residential", "Site", "Architect") }
            accepts("prompt.project.valid") {
                projects.createProject("Prompt Project", "PR-IN", "Prompt Owner", "Residential", "Site A", "Architect")
            }

            val floorId = db.projectDao().floors(projectId).first().id
            rejects("prompt.floor.blank", "level name") { projects.addFloor(projectId, " ") }
            accepts("prompt.floor.valid") { projects.addFloor(projectId, " Mezzanine ") }
            rejects("prompt.room.blank", "room name") { projects.addRoom(projectId, floorId, "") }
            accepts("prompt.room.valid") { projects.addRoom(projectId, floorId, "Store") }

            rejects("prompt.company.blank", "Practice name") {
                DemoCompanyStore(isolated, db, PortalServer(db, isolated)).createBlank("  ", "Principal", "City")
            }

            // --- Suite create prompts ---
            rejects("prompt.enquiry.blank", "client name") { suite.addEnquiry(" ", "Web", "n") }
            accepts("prompt.enquiry.valid") { suite.addEnquiry("Enquiry client", "Web", "notes") }

            rejects("prompt.letter.blank", "subject") { suite.addLetter(projectId, " ", "body") }
            accepts("prompt.letter.valid") { suite.addLetter(projectId, "Prompt letter", "Body text") }

            rejects("prompt.rate.blank", "item name") { suite.addRate(" ", "m²", 10.0) }
            accepts("prompt.rate.valid") { suite.addRate("Prompt rate", "m²", 12.5) }

            rejects("prompt.meeting.blank", "title") { suite.addMeeting(projectId, "", "A", "D") }
            accepts("prompt.meeting.valid") { suite.addMeeting(projectId, "Kickoff", "Team", "Discussed") }

            rejects("prompt.log.blank", "work completed") { suite.addLog(projectId, "Fair", " ", "Site") }
            accepts("prompt.log.valid") { suite.addLog(projectId, "Fair", "Cast columns", "Site eng") }

            rejects("prompt.issue.blank", "title and description") { suite.addIssue(projectId, "SNAG", " ", "Loc", " ", "MEDIUM") }
            rejects("prompt.issue.titleOnly", "title and description") { suite.addIssue(projectId, "SNAG", "Crack", "Loc", "  ", "MEDIUM") }
            accepts("prompt.issue.valid") { suite.addIssue(projectId, "SNAG", "Crack", "GF", "Hairline crack", "MEDIUM") }

            rejects("prompt.coordination.blank", "subject and requirement") {
                suite.addCoordination(projectId, "RFI", " ", "Struct", "GF", " ", "Me", "Them")
            }
            accepts("prompt.coordination.valid") {
                suite.addCoordination(projectId, "RFI", "Beam size", "Struct", "GF", "Confirm depth", "Architect", "SE")
            }

            rejects("prompt.drawing.blank", "drawing number and title") { suite.addDrawing(projectId, "", "Plan", "Architectural", "") }
            accepts("prompt.drawing.valid") {
                suite.addDrawing(projectId, "A-101", "Ground floor plan", "Architectural", "HFC")
            }
            val drawingId = db.suiteDao().observeDrawings(projectId).first().first { it.drawingNumber == "A-101" }.id
            rejects("prompt.revision.blank", "revision code") { suite.addRevision(projectId, drawingId, " ", "notes", false, "MINOR", "") }
            accepts("prompt.revision.valid") { suite.addRevision(projectId, drawingId, "R1", "Issued for construction", false, "MINOR", "note") }

            rejects("prompt.inspection.blank", "observations") { suite.addInspection(projectId, "GF", "Me", " ") }
            accepts("prompt.inspection.valid") { suite.addInspection(projectId, "GF", "Me", "Formwork checked") }

            rejects("prompt.transmittal.blank", "subject") { suite.addTransmittal(projectId, " ", "Client", "For information") }
            accepts("prompt.transmittal.valid") { suite.addTransmittal(projectId, "IFC set", "Client", "For construction") }

            rejects("prompt.permit.blank", "title") { suite.addPermit(projectId, " ", "Authority") }
            accepts("prompt.permit.valid") { suite.addPermit(projectId, "Building permit", "Municipality") }

            rejects("prompt.time.blankPerson", "person and time") { suite.addTime(projectId, " ", 30, "notes") }
            rejects("prompt.time.zeroMinutes", "person and time") { suite.addTime(projectId, "Architect", 0, "notes") }
            accepts("prompt.time.valid") { suite.addTime(projectId, "Architect", 45, "Site") }

            rejects("prompt.proposal.blank", "title") { suite.addProposal(projectId, " ", 1000.0) }
            accepts("prompt.proposal.valid") { suite.addProposal(projectId, "Fee proposal", 25000.0) }

            rejects("prompt.task.blank", "title") { suite.addTask(projectId, " ") }
            accepts("prompt.task.valid") { suite.addTask(projectId, "Prompt task") }

            accepts("prompt.brief.valid") { suite.saveBrief(projectId, "Objectives", "Requirements", "Constraints") }

            rejects("prompt.invoice.blank", "title") { suite.addFeeInvoice(projectId, " ", 100.0) }
            accepts("prompt.invoice.valid") { suite.addFeeInvoice(projectId, "Professional fee", 5000.0) }

            rejects("prompt.approval.blank", "title") { suite.addApproval(projectId, " ", "Drawing") }
            accepts("prompt.approval.valid") { suite.addApproval(projectId, "Foundation approval", "Drawing") }

            rejects("prompt.contract.blank", "title") { suite.addContract(projectId, " ", "Party", "Item rate", 1.0) }
            accepts("prompt.contract.valid") { suite.addContract(projectId, "Civil contract", "Apex", "Item rate", 100000.0) }

            rejects("prompt.activity.blank", "title") { suite.addActivity(projectId, "") }
            accepts("prompt.activity.valid") { suite.addActivity(projectId, "Excavation") }

            rejects("prompt.order.blank", "title") { suite.addOrder(projectId, " ", "Supplier") }
            accepts("prompt.order.valid") { suite.addOrder(projectId, "Cement", "Local supplier", "OPC 53", "Bag", 10.0, "bags") }

            // --- Finance prompts ---
            rejects("prompt.finance.settings.badGstin", "GSTIN") { finance.saveSettings("REGULAR", "18", "29", "BAD") }
            accepts("prompt.finance.settings.none") { finance.saveSettings("NONE", "18", "", "") }
            rejects("prompt.finance.invoice.blank", "invoice title") { finance.createInvoice(projectId, " ", "1000", "29", "998399", "") }
            accepts("prompt.finance.invoice.valid") { finance.createInvoice(projectId, "GST invoice", "1000", "29", "", "") }
            rejects("prompt.finance.amount.junk", "plain decimal") { FinanceMath.decimal("12.34.56") }
            rejects("prompt.finance.amount.words", "plain decimal") { FinanceMath.decimal("twelve") }
            accepts("prompt.finance.amount.valid") { check(FinanceMath.decimal("12.345").toPlainString() == "12.35") }

            // --- Receipt / TDS prompts ---
            val draft = db.suiteDao().observeInvoices(projectId).first().first { it.title == "Professional fee" }
            suite.issueInvoice(draft.id)
            val issued = db.suiteDao().invoice(draft.id)!!
            rejects("prompt.receipt.tdsNoRef", "TDS deduction reference") {
                suite.recordReceipt(projectId, issued, 0.0, "partial", tdsAmount = 100.0, tdsReference = " ")
            }
            accepts("prompt.receipt.valid") {
                suite.recordReceipt(projectId, issued, 100.0, "Prompt receipt", tdsAmount = 10.0, tdsReference = "Challan-1")
            }

            // --- Record editor required-field prompts ---
            val task = db.suiteDao().observeTasks(projectId).first().first { it.title == "Prompt task" }
            val taskSnap = operations.read(RecordKind.TASK, task.id)
            rejects("prompt.editor.taskRequired", "required") {
                operations.save(taskSnap, mapOf("title" to " ", "description" to "x"))
            }
            accepts("prompt.editor.taskValid") {
                operations.save(operations.read(RecordKind.TASK, task.id), mapOf("title" to "Prompt task edited", "description" to "desc"))
            }
            rejects("prompt.editor.issuedLocked", "draft") {
                val invoice = operations.read(RecordKind.INVOICE, issued.id)
                operations.save(invoice, RecordKind.INVOICE.fields.associate { it.column to invoice.text(it.column) })
            }
            val proposal = db.suiteDao().observeProposals(projectId).first().first { it.title == "Fee proposal" }
            rejects("prompt.editor.badNumber", "number") {
                val snap = operations.read(RecordKind.PROPOSAL, proposal.id)
                operations.save(snap, RecordKind.PROPOSAL.fields.associate { it.column to if (it.column == "amount") "not-a-number" else snap.text(it.column) })
            }

            // --- Consultancy document prompts ---
            val good = ConsultancyInput(
                title = "Prompt fee",
                service = ConsultancyService.ARCHITECTURE,
                feeBasis = ConsultancyFeeBasis.PER_SQ_FT,
                rate = "50",
                quantity = "1000",
                areaDefinition = "Built-up area on drawings",
                gstMode = GstMode.NONE,
                gstRate = "18",
                gstin = "",
                supplierState = "29",
                placeState = "29",
                parties = "Client and architect",
                scope = "Full architectural services",
                exclusions = "None",
                terms = "Standard terms",
                milestones = listOf(
                    ConsultancyMilestone("Mobilisation", "30"),
                    ConsultancyMilestone("Completion", "100"),
                ),
            )
            rejects("prompt.consultancy.blankTitle", "title") { ConsultancyTemplates.validate(good.copy(title = " ")) }
            rejects("prompt.consultancy.badRate", "positive") { ConsultancyTemplates.validate(good.copy(rate = "0")) }
            rejects("prompt.consultancy.todo", "placeholder") { ConsultancyTemplates.validate(good.copy(scope = "Scope [TODO]"), forIssue = true) }
            accepts("prompt.consultancy.valid") { ConsultancyTemplates.validate(good) }
            accepts("prompt.consultancy.issue") { ConsultancyTemplates.validate(good, forIssue = true) }

            // --- Measurement / work-item prompts ---
            rejects("prompt.workItem.badFormula", "formula") { WorkItemLibrary.validate("NOPE", "m³") }
            accepts("prompt.workItem.valid") { WorkItemLibrary.validate("VOLUME", "m³") }
            rejects("prompt.password.short", "12") {
                PasswordEnvelope.encrypt("plain".toByteArray(), "short".toCharArray(), 1_000_000)
            }
            accepts("prompt.password.valid") {
                val password = "twelvechars!".toCharArray()
                try {
                    val sealed = PasswordEnvelope.encrypt("secret-bytes".toByteArray(), password, 1_000_000)
                    check(PasswordEnvelope.decrypt(sealed, password, 1_000_000).contentEquals("secret-bytes".toByteArray()))
                } finally {
                    password.fill('\u0000')
                }
            }

            // --- Snag close prompt (OPEN → IN_PROGRESS → READY_FOR_VERIFICATION → CLOSED) ---
            val issue = db.suiteDao().observeIssues(projectId).first().first { it.title == "Crack" }
            check(suite.transitionIssue(issue.id, "IN_PROGRESS") == null)
            check(suite.transitionIssue(issue.id, "READY_FOR_VERIFICATION") == null)
            val closeError = suite.transitionIssue(issue.id, "CLOSED", verificationNote = " ")
            if (closeError.isNullOrBlank()) fail("prompt.snag.closeBlank", "Closed without verification note")
            else pass("prompt.snag.closeBlank", closeError)
            val closed = suite.transitionIssue(issue.id, "CLOSED", verificationNote = "Verified on site")
            if (closed == null) pass("prompt.snag.closeValid", "Closed with verification")
            else fail("prompt.snag.closeValid", closed)

            results.putString("inputPromptReport", report.toString(2))
            results.putString(
                "inputPrompts",
                if (failures.isEmpty()) "passed: ${report.length()} prompt checks"
                else "failed ${failures.size}/${report.length()}: ${failures.take(5).joinToString("; ")}",
            )
            check(failures.isEmpty()) { failures.joinToString("\n") }
        } finally {
            db.close()
            directory.deleteRecursively()
        }
    }
}

/** UI check that key form prompts expose the formulated labels users type into. */
object InputPromptUiChecks {
    private const val PACKAGE = "com.holagundi.archiman"

    fun run(instrumentation: Instrumentation, results: Bundle) {
        val device = UiDevice.getInstance(instrumentation)
        val report = JSONArray()
        val failures = mutableListOf<String>()
        fun pass(id: String, detail: String) = report.put(JSONObject().put("id", id).put("status", "PASS").put("detail", detail))
        fun fail(id: String, detail: String) {
            failures += "$id: $detail"
            report.put(JSONObject().put("id", id).put("status", "FAIL").put("detail", detail))
        }
        fun has(text: String) = device.findObject(By.text(text)) != null || device.findObject(By.textContains(text)) != null
        fun click(text: String): Boolean {
            val node = device.wait(Until.findObject(By.text(text)), 2_500)
                ?: device.wait(Until.findObject(By.textContains(text)), 2_000)
                ?: return false
            node.click(); device.waitForIdle(400); return true
        }
        fun openTab(label: String) {
            if (!click(label)) device.findObject(By.textContains(label.take(4)))?.click()
            device.waitForIdle(700)
        }
        fun scrollClick(text: String) {
            if (click(text)) return
            repeat(5) {
                device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.7f)
                device.waitForIdle(250)
                if (click(text)) return
            }
            error("Missing $text")
        }
        fun expectLabels(id: String, labels: List<String>) {
            val missing = labels.filterNot(::has)
            if (missing.isEmpty()) pass(id, labels.joinToString(", "))
            else fail(id, "Missing labels: $missing")
        }

        try {
            val intent = instrumentation.targetContext.packageManager.getLaunchIntentForPackage(PACKAGE)?.apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK or android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: error("No launch intent")
            instrumentation.targetContext.startActivity(intent)
            device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), 15_000)
            device.waitForIdle(1_200)
            if (!has("Dashboard") && !has("Projects")) error("Practice shell not signed in for UI prompt checks")

            // New project dialog — open via Create FAB speed-dial, then "New project".
            openTab("Projects")
            if (!has("New project") || !has("Task")) {
                val fab = device.findObject(By.desc("Create"))
                    ?: device.findObject(By.descContains("Create"))
                    ?: device.findObject(By.desc("Close create menu"))
                if (fab != null) {
                    fab.click()
                    device.waitForIdle(700)
                } else {
                    device.click((device.displayWidth * 0.90).toInt(), (device.displayHeight * 0.90).toInt())
                    device.waitForIdle(700)
                }
            }
            val openedProject = run {
                device.findObject(By.desc("New project"))?.click()
                device.waitForIdle(500)
                has("Project name (required)") || click("New project")
            }
            device.waitForIdle(900)
            if (openedProject || has("Project name (required)") || has("New project") && has("Cancel")) {
                device.wait(Until.findObject(By.text("Project name (required)")), 3_000)
                expectLabels(
                    "ui.projectCreate.labels",
                    listOf("Project name (required)", "Code (optional)", "Client (required)", "Site address"),
                )
                if (has("Project name (required)")) pass("ui.projectCreate.blankGuard", "Required fields shown; Create stays gated")
                else fail("ui.projectCreate.blankGuard", "Create dialog missing required labels")
                click("Cancel") || device.pressBack()
            } else fail("ui.projectCreate.open", "Could not open New project from Create FAB")

            // Practice client form
            openTab("Practice")
            scrollClick("Clients")
            expectLabels("ui.clients.labels", listOf("Client name", "Phone", "Email", "Save client"))
            if (has("Practice menu")) click("Practice menu") else device.pressBack()

            // Project forms
            openTab("Projects")
            scrollClick("Skyline Villa Residence")
            device.wait(Until.findObject(By.textContains("Project overview")), 8_000)
            device.waitForIdle(800)

            fun openGroup(label: String) {
                click(label); device.waitForIdle(500); click(label); device.waitForIdle(500)
            }
            openGroup("Work")
            scrollClick("Brief")
            expectLabels("ui.brief.labels", listOf("Client objectives", "Requirements", "Site constraints", "Save brief"))
            device.pressBack(); device.waitForIdle(500)

            openGroup("Work")
            scrollClick("Tasks")
            expectLabels("ui.tasks.labels", listOf("Tasks", "Add to To do"))
            device.pressBack(); device.waitForIdle(500)

            openGroup("Work")
            if (has("Documents")) scrollClick("Documents")
            scrollClick("Letters")
            expectLabels("ui.letters.labels", listOf("Subject", "Body", "Save letter"))
            device.pressBack(); device.waitForIdle(400)
            if (has("Documents") || has("Letters")) device.pressBack()

            openGroup("Site")
            scrollClick("Visits")
            expectLabels("ui.visits.labels", listOf("Location", "Inspector", "Report", "Save visit"))
            device.pressBack(); device.waitForIdle(500)

            openGroup("Site")
            scrollClick("Snags")
            expectLabels("ui.snags.labels", listOf("Snags and NCRs", "New snag"))
            // Form is inline below the list — scroll until field labels appear.
            repeat(8) {
                if (has("Title") && has("Description")) return@repeat
                device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.8f)
                device.waitForIdle(250)
            }
            expectLabels("ui.snagForm.labels", listOf("Title", "Description", "Location", "Save snag"))
            device.pressBack(); device.waitForIdle(500)

            openGroup("Site")
            scrollClick("Coordination")
            expectLabels("ui.coordination.labels", listOf("Coordination", "New item"))
            if (click("New item")) {
                device.waitForIdle(600)
                expectLabels("ui.coordinationForm.labels", listOf("Subject", "Question or requirement"))
                device.pressBack(); device.waitForIdle(400)
            }
            device.pressBack(); device.waitForIdle(500)

            openGroup("Site")
            scrollClick("Daily logs & orders")
            expectLabels("ui.siteman.labels", listOf("Siteman", "Daily log"))
            device.pressBack(); device.waitForIdle(500)

            openGroup("Fees")
            scrollClick("Client invoices")
            expectLabels("ui.invoice.labels", listOf("Client invoice"))
            device.pressBack(); device.waitForIdle(500)

            openGroup("Fees")
            scrollClick("Fee proposals")
            expectLabels("ui.proposal.labels", listOf("Fee proposals"))
            device.pressBack()

            results.putString("inputPromptUiReport", report.toString(2))
            results.putString(
                "inputPromptUi",
                if (failures.isEmpty()) "passed: ${report.length()} UI prompt checks"
                else "failed ${failures.size}/${report.length()}: ${failures.take(5).joinToString("; ")}",
            )
            if (failures.isNotEmpty()) error(failures.joinToString("\n"))
        } catch (error: Throwable) {
            results.putString("inputPromptUiReport", report.toString(2))
            results.putString("inputPromptUi", "failed: ${error.message}")
            throw error
        }
    }
}

/** Instrumentation entry used when `-e suite inputPrompts` is passed to BackupDeviceChecks. */
object InputPromptSuite {
    fun run(instrumentation: Instrumentation) {
        val results = Bundle()
        try {
            runBlocking { InputPromptDeviceChecks.run(instrumentation.targetContext, results) }
            InputPromptUiChecks.run(instrumentation, results)
            results.putString(
                "stream",
                "\nInput prompt checks PASSED\n${results.getString("inputPrompts")}\n${results.getString("inputPromptUi")}\n" +
                    "${results.getString("inputPromptReport")}\n${results.getString("inputPromptUiReport")}\n",
            )
            instrumentation.finish(Activity.RESULT_OK, results)
        } catch (error: Throwable) {
            results.putString(
                "stream",
                "\nInput prompt checks FAILED: ${error.stackTraceToString()}\n" +
                    "${results.getString("inputPromptReport")}\n${results.getString("inputPromptUiReport")}\n",
            )
            instrumentation.finish(Activity.RESULT_CANCELED, results)
        }
    }
}
