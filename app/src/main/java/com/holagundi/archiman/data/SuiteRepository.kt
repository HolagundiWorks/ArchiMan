package com.holagundi.archiman.data

import androidx.room.withTransaction
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.domain.InvoiceBalance

import com.holagundi.archiman.data.local.CatalogDao
import com.holagundi.archiman.data.local.MeasurementDao
import com.holagundi.archiman.data.local.PracticeDao
import com.holagundi.archiman.data.local.ProjectDao
import com.holagundi.archiman.data.local.SuiteDao
import com.holagundi.archiman.data.local.entity.CoordinationEventEntity
import com.holagundi.archiman.data.local.entity.CoordinationItemEntity
import com.holagundi.archiman.data.local.entity.DailySiteReportEntity
import com.holagundi.archiman.data.local.entity.DrawingRevisionEntity
import com.holagundi.archiman.data.local.entity.DrawingTransmittalEntity
import com.holagundi.archiman.data.local.entity.ProjectDrawingEntity
import com.holagundi.archiman.data.local.entity.FeeProposalEntity
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ItemLinkRuleEntity
import com.holagundi.archiman.data.local.entity.LetterEntity
import com.holagundi.archiman.data.local.entity.LocalUserEntity
import com.holagundi.archiman.data.local.entity.MaterialOrderEntity
import com.holagundi.archiman.data.local.entity.MaterialOrderLineEntity
import com.holagundi.archiman.data.local.entity.MeetingMinutesEntity
import com.holagundi.archiman.data.local.entity.PermitEntity
import com.holagundi.archiman.data.local.entity.ProjectApprovalEntity
import com.holagundi.archiman.data.local.entity.ProjectConsultancyProfileEntity
import com.holagundi.archiman.data.local.entity.ProjectSettingsEntity
import com.holagundi.archiman.data.local.entity.ProjectStageEntity
import com.holagundi.archiman.data.local.entity.ProjectTaskEntity
import com.holagundi.archiman.data.local.entity.RateBookItemEntity
import com.holagundi.archiman.data.local.entity.RccMemberEntity
import com.holagundi.archiman.data.local.entity.ReceiptEntity
import com.holagundi.archiman.data.local.entity.ScheduleActivityEntity
import com.holagundi.archiman.data.local.entity.EnquiryEntity
import com.holagundi.archiman.data.local.entity.SiteInspectionEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEventEntity
import com.holagundi.archiman.data.local.entity.TimeEntryEntity
import com.holagundi.archiman.data.local.entity.WorkContractEntity
import com.holagundi.archiman.domain.CalculationTypes
import com.holagundi.archiman.domain.CoordinationWorkflow
import com.holagundi.archiman.domain.CoreOpCatalog
import com.holagundi.archiman.domain.EstimateBuilder
import com.holagundi.archiman.domain.EstimateLine
import com.holagundi.archiman.domain.MeasurementDraft
import com.holagundi.archiman.domain.ProjectPath
import com.holagundi.archiman.domain.RecordResult
import com.holagundi.archiman.domain.SiteIssueWorkflow
import com.holagundi.archiman.domain.StandardStages
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

@Singleton
class SuiteRepository @Inject constructor(
    private val database: ArchiManDatabase,
    private val suiteDao: SuiteDao,
    private val practiceDao: PracticeDao,
    private val projectDao: ProjectDao,
    private val catalogDao: CatalogDao,
    private val measurementDao: MeasurementDao,
    private val measurementRepository: MeasurementRepository,
) {
    fun observeUsers() = practiceDao.observeUsers()
    fun observeDashboardMetrics() = suiteDao.observeDashboardMetrics()
    fun observeDashboardTasks(limit: Int = 4) = suiteDao.observeDashboardTasks(limit)
    fun observeDashboardReviews(limit: Int = 3) = suiteDao.observeDashboardReviews(limit)
    fun observeDashboardIssues(limit: Int = 3) = suiteDao.observeDashboardIssues(limit)
    fun observeEnquiries() = suiteDao.observeEnquiries()
    fun observeLetters() = suiteDao.observeLetters()
    fun observeTransmittals(projectId: Long) = suiteDao.observeTransmittals(projectId)
    fun observeInspections(projectId: Long) = suiteDao.observeInspections(projectId)
    fun observeAllStages() = suiteDao.observeAllStages()
    fun observeAllInvoices() = suiteDao.observeAllInvoices()
    fun observeAllReceipts() = suiteDao.observeAllReceipts()
    fun observeAllTransmittals() = suiteDao.observeAllTransmittals()
    fun observeRates() = suiteDao.observeRates()
    fun observeLinks() = suiteDao.observeLinks()

    fun observeSettings(projectId: Long) = suiteDao.observeSettings(projectId)
    fun observeStages(projectId: Long) = suiteDao.observeStages(projectId)
    fun observeInvoices(projectId: Long) = suiteDao.observeInvoices(projectId)
    fun observeReceipts(projectId: Long) = suiteDao.observeReceipts(projectId)
    fun observePermits(projectId: Long) = suiteDao.observePermits(projectId)
    fun observeTime(projectId: Long) = suiteDao.observeTime(projectId)
    fun observeProposals(projectId: Long) = suiteDao.observeProposals(projectId)
    fun observeRcc(projectId: Long) = suiteDao.observeRcc(projectId)
    fun observeContracts(projectId: Long) = suiteDao.observeContracts(projectId)
    fun observeActivities(projectId: Long) = suiteDao.observeActivities(projectId)
    fun observeMeetings(projectId: Long) = suiteDao.observeMeetings(projectId)
    fun observeLogs(projectId: Long) = suiteDao.observeLogs(projectId)
    fun observeApprovedMeasurements(projectId: Long) = measurementDao.observeApprovedMeasurements(projectId)
    fun observeOrderLines(projectId: Long) = suiteDao.observeOrderLines(projectId)
    fun observeIssues(projectId: Long) = suiteDao.observeIssues(projectId)
    fun observeCoordination(projectId: Long) = suiteDao.observeCoordination(projectId)
    fun observeDrawings(projectId: Long) = suiteDao.observeDrawings(projectId)
    fun observeRevisions(projectId: Long) = suiteDao.observeProjectRevisions(projectId)
    fun observeTasks(projectId: Long) = suiteDao.observeTasks(projectId)
    fun observeBrief(projectId: Long) = suiteDao.observeBrief(projectId)
    fun observeApprovals(projectId: Long) = suiteDao.observeApprovals(projectId)
    fun observeOrders(projectId: Long) = suiteDao.observeOrders(projectId)

    fun observeEstimate(projectId: Long): Flow<List<EstimateLine>> = measurementDao.observeApprovedMeasurements(projectId).map { rows ->
        rows.groupBy { listOf(it.itemName,it.unit,it.boqRevision,it.rateSnapshot.toString(),it.contractBasis) }.map { (_,group) ->
            val first=group.first(); val quantity=group.sumOf { it.quantity }
            EstimateLine("${first.itemName} | ${first.boqRevision.ifBlank { "Legacy: no rate snapshot" }} | ${first.contractBasis}",first.unit,quantity,first.rateSnapshot,InvoiceBalance.money(quantity*first.rateSnapshot).toDouble())
        }
    }

    suspend fun ensureDefaults() = database.withTransaction {
        val initialized = practiceDao.userCount() > 0
        val candidates = practiceDao.activeUsers()
        val priorAdminId = database.openHelper.writableDatabase.query("SELECT id FROM local_users WHERE username = 'admin' LIMIT 1").use { if (it.moveToFirst()) it.getLong(0) else null }
        val admin = priorAdminId?.let { practiceDao.user(it) } ?: candidates.firstOrNull { it.role == "Owner" } ?: candidates.firstOrNull()
        if (admin == null) practiceDao.insertUser(LocalUserEntity(username = "admin", displayName = "Admin", role = "Admin", isActive = true, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        else {
            database.openHelper.writableDatabase.execSQL("UPDATE local_users SET isActive = 0 WHERE id <> ?", arrayOf(admin.id))
            database.openHelper.writableDatabase.execSQL("UPDATE local_users SET username = 'admin', displayName = 'Admin', role = 'Admin', isActive = 1 WHERE id = ?", arrayOf(admin.id))
        }
        if (initialized) return@withTransaction
        if (suiteDao.rateCount() == 0) {
            listOf(
                RateBookItemEntity(itemName = "BrickWork 230mm in CM 1:6", unit = "m²", rate = 842.0, sourceName = "PWD SR"),
                RateBookItemEntity(itemName = "Internal Cement Plaster 12mm", unit = "m²", rate = 286.0, sourceName = "PWD SR"),
                RateBookItemEntity(itemName = "RCC M25 Column & Beams", unit = "m³", rate = 9450.0, sourceName = "PWD SR"),
                RateBookItemEntity(itemName = "Vitrified Tile Flooring 800x800", unit = "m²", rate = 1240.0, sourceName = "PWD SR"),
                RateBookItemEntity(itemName = "Earthwork Excavation", unit = "m³", rate = 320.0, sourceName = "PWD SR"),
                RateBookItemEntity(itemName = "Masonry walls", unit = "m²", rate = 842.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Plastering", unit = "m²", rate = 286.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Painting", unit = "m²", rate = 145.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Flooring", unit = "m²", rate = 1240.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Skirting", unit = "m", rate = 210.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Columns", unit = "m³", rate = 9450.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Beams", unit = "m³", rate = 9450.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Slabs", unit = "m³", rate = 8900.0, sourceName = "Site rate"),
                RateBookItemEntity(itemName = "Footings", unit = "m³", rate = 8200.0, sourceName = "Site rate"),
            ).forEach { suiteDao.insertRate(it) }
        }
        if (suiteDao.linkCount() == 0) {
            listOf(
                ItemLinkRuleEntity(sourceTrade = "Masonry walls", targetTrade = "Plastering", factor = 2.0, note = "Both faces"),
                ItemLinkRuleEntity(sourceTrade = "BrickWork 230mm in CM 1:6", targetTrade = "Internal Cement Plaster 12mm", factor = 2.0, note = "Both faces"),
                ItemLinkRuleEntity(sourceTrade = "Plastering", targetTrade = "Painting", factor = 1.0, note = "Paint follows plaster"),
                ItemLinkRuleEntity(sourceTrade = "Internal Cement Plaster 12mm", targetTrade = "Painting", factor = 1.0, note = "Paint follows plaster"),
                ItemLinkRuleEntity(sourceTrade = "Flooring", targetTrade = "Skirting", factor = 0.4, note = "Review the derived quantity"),
                ItemLinkRuleEntity(sourceTrade = "Vitrified Tile Flooring 800x800", targetTrade = "Skirting", factor = 0.4, note = "Review the derived quantity"),
            ).forEach { suiteDao.insertLink(it) }
        }
    }

    suspend fun ensureSettings(projectId: Long) {
        if (suiteDao.settings(projectId) == null) {
            suiteDao.upsertSettings(ProjectSettingsEntity(projectId, "PMC", "Full service"))
        }
    }

    suspend fun setPersona(projectId: Long, persona: String) {
        val current = suiteDao.settings(projectId)
        suiteDao.upsertSettings(
            ProjectSettingsEntity(projectId, persona, current?.consultancyType ?: "Full service"),
        )
    }

    suspend fun assignStandardStages(projectId: Long) {
        if (suiteDao.stageCount(projectId) > 0) return
        val now = System.currentTimeMillis()
        StandardStages.TITLES.forEachIndexed { index, title ->
            suiteDao.insertStage(
                ProjectStageEntity(
                    projectId = projectId,
                    title = title,
                    status = "OPEN",
                    orderIndex = index,
                    feeAmount = 0.0,
                    createdAt = now,
                ),
            )
        }
    }

    suspend fun addEnquiry(client: String, source: String, notes: String) {
        require(client.isNotBlank()) { "Enter a client name." }
        suiteDao.insertEnquiry(
            EnquiryEntity(clientName = client.trim(), source = source.trim(), notes = notes.trim(), status = "OPEN", createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun addLetter(projectId: Long?, subject: String, body: String) {
        require(subject.isNotBlank()) { "Enter a subject." }
        suiteDao.insertLetter(
            LetterEntity(projectId = projectId, subject = subject.trim(), body = body.trim(), status = "DRAFT", createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun addRate(name: String, unit: String, rate: Double) {
        require(name.isNotBlank()) { "Enter an item name." }
        suiteDao.insertRate(RateBookItemEntity(itemName = name.trim(), unit = unit.ifBlank { "m²" }, rate = rate, sourceName = "Office"))
    }

    suspend fun billStage(projectId: Long, stage: ProjectStageEntity, amount: Double) = database.withTransaction {
        val current=database.openHelper.writableDatabase.query("SELECT status, projectId FROM project_stages WHERE id=?",arrayOf(stage.id)).use { cursor -> require(cursor.moveToFirst()) { "Stage no longer exists." }; cursor.getString(0) to cursor.getLong(1) }
        require(current.first=="OPEN" && current.second==projectId) { "This stage was already billed or belongs to another project." }
        require(InvoiceBalance.money(amount).signum()>0) { "Enter a positive stage fee." }
        val now = System.currentTimeMillis()
        suiteDao.insertInvoice(
            InvoiceEntity(
                projectId = projectId,
                stageId = stage.id,
                number = "INV-${java.util.UUID.randomUUID()}",
                title = stage.title,
                amount = amount,
                status = "DRAFT",
                createdAt = now,
            ),
        )
        suiteDao.setStageStatus(stage.id, "BILLED")
    }

    suspend fun issueInvoice(id: Long) = database.withTransaction {
        val current = requireNotNull(suiteDao.invoice(id)) { "Invoice no longer exists." }
        require(current.status == "DRAFT") { "Only draft invoices can be issued." }
        require(InvoiceBalance.money(current.amount).signum() > 0) { "Invoice amount must be greater than zero." }
        val mode = suiteDao.finance("settings")?.let { org.json.JSONObject(it.json).optString("mode", "NONE") } ?: "NONE"
        require(mode == "NONE" || current.taxJson.isNotBlank()) { "This older draft has no GST details. Recreate it from Invoices with GST details before issuing." }
        suiteDao.setInvoiceStatus(id, "ISSUED")
    }

    suspend fun recordReceipt(projectId: Long, invoice: InvoiceEntity, amount: Double, note: String, tdsAmount: Double = 0.0, tdsReference: String = "", bankKey: String = "", paymentDate: Long = System.currentTimeMillis()) = database.withTransaction {
        val current = requireNotNull(suiteDao.invoice(invoice.id)) { "Invoice no longer exists." }
        require(current.projectId == projectId) { "Invoice belongs to another project." }
        require(current.status == "ISSUED") { "Only issued invoices can receive payments." }
        require(InvoiceBalance.money(amount).signum() >= 0 && InvoiceBalance.money(tdsAmount).signum() >= 0) { "Cash and TDS cannot be negative." }
        require(tdsAmount == 0.0 || tdsReference.isNotBlank()) { "Enter the TDS deduction reference or explanation." }
        val remaining = InvoiceBalance.afterPayment(
            current.amount, suiteDao.invoiceReceipts(current.id).map { it.amount + it.tdsAmount }, amount + tdsAmount,
        )
        suiteDao.insertReceipt(
            ReceiptEntity(
                invoiceId = invoice.id,
                projectId = projectId,
                amount = InvoiceBalance.money(amount).toDouble(),
                note = note.trim(),
                tdsAmount = InvoiceBalance.money(tdsAmount).toDouble(), tdsReference = tdsReference.trim(), bankKey = bankKey,
                receivedAt = paymentDate,
            ),
        )
        suiteDao.setInvoiceStatus(current.id, if (remaining.signum() == 0) "PAID" else "ISSUED")
    }

    suspend fun addMeeting(projectId: Long, title: String, attendees: String, discussion: String) {
        require(title.isNotBlank()) { "Enter a title." }
        val now = System.currentTimeMillis()
        suiteDao.insertMeeting(
            MeetingMinutesEntity(
                projectId = projectId,
                title = title.trim(),
                meetingAt = now,
                location = "Site",
                attendees = attendees.trim(),
                discussion = discussion.trim(),
                decisions = "",
                actionItems = "",
                createdAt = now,
            ),
        )
    }

    suspend fun addLog(
        projectId: Long,
        weather: String,
        work: String,
        preparedBy: String,
        manpower: String = "",
        materials: String = "",
        delays: String = "",
        safety: String = "",
        nextDay: String = "",
    ) {
        require(work.isNotBlank()) { "Describe the work completed." }
        val now = System.currentTimeMillis()
        suiteDao.insertLog(
            DailySiteReportEntity(
                projectId = projectId,
                reportDate = now,
                weather = weather.ifBlank { "Fair" },
                manpower = manpower.trim(),
                workCompleted = work.trim(),
                materialsReceived = materials.trim(),
                delaysOrConstraints = delays.trim(),
                safetyObservations = safety.trim(),
                nextDayPlan = nextDay.trim(),
                preparedBy = preparedBy.ifBlank { "Site" },
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun addIssue(
        projectId: Long,
        type: String,
        title: String,
        location: String,
        description: String,
        severity: String,
        assignedTo: String = "",
        correctiveAction: String = "",
        evidenceUri: String = "",
    ) {
        require(title.isNotBlank() && description.isNotBlank()) { "Enter a title and description." }
        val now = System.currentTimeMillis()
        val kind = when (type.uppercase()) {
            "NCR" -> "NCR"
            "SAFETY" -> "SAFETY"
            else -> "SNAG"
        }
        val prefix = when (kind) {
            "NCR" -> "NCR"
            "SAFETY" -> "SAF"
            else -> "SNG"
        }
        suiteDao.insertIssue(
            SiteIssueEntity(
                projectId = projectId,
                referenceNumber = "$prefix-${now % 100000}",
                type = kind,
                title = title.trim(),
                location = location.trim(),
                description = description.trim(),
                severity = SiteIssueWorkflow.normalizeSeverity(severity),
                assignedTo = assignedTo.trim(),
                correctiveAction = correctiveAction.trim(),
                status = SiteIssueWorkflow.OPEN,
                evidenceUri = evidenceUri.trim().ifBlank { null },
                verificationNote = "",
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun transitionIssue(
        issueId: Long,
        toStatus: String,
        verificationNote: String = "",
        afterEvidenceUri: String = "",
        actor: String = "Architect",
    ): String? {
        val issue = suiteDao.issue(issueId) ?: return "Site issue not found."
        val check = SiteIssueWorkflow.canTransition(issue.status, toStatus, verificationNote)
        if (!check.valid) return check.error
        val now = System.currentTimeMillis()
        val note = verificationNote.trim()
        suiteDao.updateIssue(
            issue.copy(
                status = toStatus,
                verificationNote = if (toStatus == SiteIssueWorkflow.CLOSED) note else issue.verificationNote,
                afterEvidenceUri = when {
                    afterEvidenceUri.isNotBlank() -> afterEvidenceUri.trim()
                    else -> issue.afterEvidenceUri
                },
                closedAt = if (toStatus == SiteIssueWorkflow.CLOSED) now else issue.closedAt,
                updatedAt = now,
            ),
        )
        suiteDao.insertIssueEvent(
            SiteIssueEventEntity(
                siteIssueId = issueId,
                fromStatus = issue.status,
                toStatus = toStatus,
                note = if (toStatus == SiteIssueWorkflow.CLOSED) note else "",
                actor = actor.ifBlank { "Architect" },
                occurredAt = now,
            ),
        )
        return null
    }

    suspend fun addCoordination(
        projectId: Long,
        type: String,
        subject: String,
        discipline: String,
        location: String,
        question: String,
        raisedBy: String,
        assignedTo: String,
    ) {
        require(subject.isNotBlank() && question.isNotBlank()) { "Enter a subject and requirement." }
        val kind = when (type) {
            CoordinationWorkflow.SUBMITTAL, CoordinationWorkflow.SITE_INSTRUCTION -> type
            else -> CoordinationWorkflow.RFI
        }
        val now = System.currentTimeMillis()
        val prefix = when (kind) {
            CoordinationWorkflow.SUBMITTAL -> "SUB"
            CoordinationWorkflow.SITE_INSTRUCTION -> "SI"
            else -> "RFI"
        }
        suiteDao.insertCoordination(
            CoordinationItemEntity(
                projectId = projectId,
                type = kind,
                referenceNumber = "$prefix-${now % 100000}",
                subject = subject.trim(),
                discipline = discipline.trim(),
                location = location.trim(),
                raisedBy = raisedBy.trim(),
                assignedTo = assignedTo.trim(),
                questionOrRequirement = question.trim(),
                response = "",
                status = CoordinationWorkflow.initialStatus(kind),
                priority = "NORMAL",
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun transitionCoordination(
        itemId: Long,
        toStatus: String,
        note: String = "",
        actor: String = "Architect",
    ): String? {
        val item = suiteDao.coordination(itemId) ?: return "Coordination item not found."
        val error = CoordinationWorkflow.transitionError(item.type, item.status, toStatus)
        if (error != null) return error
        val now = System.currentTimeMillis()
        suiteDao.updateCoordination(
            item.copy(
                status = toStatus,
                response = note.trim().ifBlank { item.response },
                closedAt = if (CoordinationWorkflow.isClosed(toStatus)) now else item.closedAt,
                updatedAt = now,
            ),
        )
        suiteDao.insertCoordinationEvent(
            CoordinationEventEntity(
                coordinationItemId = itemId,
                fromStatus = item.status,
                toStatus = toStatus,
                note = note.trim(),
                actor = actor.ifBlank { "Architect" },
                occurredAt = now,
            ),
        )
        return null
    }

    suspend fun addDrawing(
        projectId: Long,
        number: String,
        title: String,
        discipline: String,
        packageTag: String,
    ): Long {
        require(number.isNotBlank() && title.isNotBlank()) { "Enter the drawing number and title." }
        val now = System.currentTimeMillis()
        return suiteDao.insertDrawing(
            ProjectDrawingEntity(
                projectId = projectId,
                drawingNumber = number.trim(),
                title = title.trim(),
                discipline = discipline.ifBlank { "Architectural" },
                status = "CURRENT",
                packageTag = packageTag.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun addRevision(
        projectId: Long,
        drawingId: Long,
        revisionCode: String,
        notes: String,
        asBuilt: Boolean,
        severity: String,
        fileNote: String,
    ) {
        require(revisionCode.isNotBlank()) { "Enter a revision code." }
        val now = System.currentTimeMillis()
        suiteDao.insertRevision(
            DrawingRevisionEntity(
                projectId = projectId,
                drawingId = drawingId,
                revisionCode = revisionCode.trim(),
                fileName = fileNote.trim(),
                mimeType = "",
                fileUri = fileNote.trim(),
                fileChecksum = "",
                issueStatus = "ISSUED",
                revisionNotes = notes.trim(),
                isAsBuilt = asBuilt,
                revisionSource = "PRACTICE",
                severity = SiteIssueWorkflow.normalizeSeverity(severity),
                issuedAt = now,
                createdAt = now,
            ),
        )
    }

    suspend fun addInspection(projectId: Long, location: String, inspector: String, observation: String) {
        require(observation.isNotBlank()) { "Enter the site visit observations." }
        val now = System.currentTimeMillis()
        suiteDao.insertInspection(
            SiteInspectionEntity(
                projectId = projectId,
                inspectionAt = now,
                location = location.trim(),
                inspector = inspector.ifBlank { "Architect" },
                observation = observation.trim(),
                severity = "NORMAL",
                correctiveAction = "",
                status = "OPEN",
                createdAt = now,
            ),
        )
    }

    suspend fun addTransmittal(
        projectId: Long,
        subject: String,
        recipients: String,
        purpose: String = "For information",
    ) {
        require(subject.isNotBlank()) { "Enter a subject." }
        val now = System.currentTimeMillis()
        suiteDao.insertTransmittal(
            DrawingTransmittalEntity(
                projectId = projectId,
                transmittalNumber = "TR-${now % 10000}",
                subject = subject.trim(),
                recipients = recipients.trim(),
                purpose = purpose.ifBlank { "For information" },
                notes = "",
                issuedAt = now,
            ),
        )
    }

    suspend fun receiveOrder(id: Long) {
        suiteDao.setOrderStatus(id, "RECEIVED", System.currentTimeMillis())
    }

    suspend fun decideApproval(id: Long, status: String) {
        suiteDao.setApprovalStatus(id, status, if (status == "APPROVED") System.currentTimeMillis() else null)
    }

    suspend fun advanceActivity(id: Long, progress: Int) {
        val next = progress.coerceIn(0, 100)
        suiteDao.setActivityProgress(id, next, if (next >= 100) "DONE" else "IN_PROGRESS")
    }

    suspend fun addPermit(projectId: Long, title: String, authority: String) {
        require(title.isNotBlank()) { "Enter a title." }
        suiteDao.insertPermit(
            PermitEntity(projectId = projectId, title = title.trim(), authority = authority.trim(), status = "APPLIED", createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun addTime(projectId: Long, person: String, minutes: Int, notes: String) {
        require(person.isNotBlank() && minutes > 0) { "Enter a person and time greater than zero." }
        suiteDao.insertTime(
            TimeEntryEntity(projectId = projectId, person = person.trim(), minutes = minutes, notes = notes.trim(), workDate = System.currentTimeMillis()),
        )
    }

    suspend fun addProposal(projectId: Long, title: String, amount: Double) {
        require(title.isNotBlank()) { "Enter a title." }
        suiteDao.insertProposal(
            FeeProposalEntity(projectId = projectId, title = title.trim(), amount = amount, status = "DRAFT", createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun addTask(projectId: Long, title: String) {
        require(title.isNotBlank()) { "Enter a title." }
        val now = System.currentTimeMillis()
        suiteDao.insertTask(
            ProjectTaskEntity(projectId = projectId, title = title.trim(), description = "", status = "TO_DO", createdAt = now),
        )
    }

    suspend fun moveTask(id: Long, status: String) = suiteDao.setTaskStatus(id, status)

    suspend fun saveBrief(projectId: Long, objectives: String, requirements: String, constraints: String) {
        val now = System.currentTimeMillis()
        val existing = suiteDao.brief(projectId)
        val profile = (existing ?: ProjectConsultancyProfileEntity(
            projectId = projectId,
            consultancyTypes = "Architectural",
            currentPhase = "",
            currentDesignStage = "",
            briefStatus = "RECORDED",
            clientObjectives = "",
            projectRequirements = "",
            designPreferences = "",
            siteConstraints = "",
            clarifications = "",
            siteDimensions = "",
            siteOrientation = "",
            siteAccess = "",
            existingConditions = "",
            surroundings = "",
            topography = "",
            utilities = "",
            existingStructures = "",
            vegetation = "",
            legalPlanningInformation = "",
            updatedAt = now,
        )).copy(
            briefStatus = "RECORDED",
            clientObjectives = objectives.trim(),
            projectRequirements = requirements.trim(),
            siteConstraints = constraints.trim(),
            updatedAt = now,
        )
        if (existing == null) suiteDao.insertBrief(profile) else suiteDao.updateBrief(profile)
    }

    suspend fun addFeeInvoice(projectId: Long, title: String, amount: Double) {
        require(title.isNotBlank()) { "Enter a title." }
        val now = System.currentTimeMillis()
        suiteDao.insertInvoice(
            InvoiceEntity(
                projectId = projectId,
                stageId = null,
                number = "INV-${java.util.UUID.randomUUID()}",
                title = title.trim(),
                amount = amount,
                status = "DRAFT",
                createdAt = now,
            ),
        )
    }

    suspend fun addApproval(projectId: Long, title: String, type: String) {
        require(title.isNotBlank()) { "Enter a title." }
        suiteDao.insertApproval(
            ProjectApprovalEntity(
                projectId = projectId,
                approvalType = type.ifBlank { "Drawing" },
                title = title.trim(),
                description = "",
                phase = "Construction",
                status = "SUBMITTED",
                remarks = "",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun addContract(projectId: Long, title: String, party: String, kind: String, amount: Double) {
        require(title.isNotBlank()) { "Enter a title." }
        suiteDao.insertContract(
            WorkContractEntity(
                projectId = projectId,
                title = title.trim(),
                party = party.trim(),
                kind = kind.ifBlank { "Item rate" },
                amount = amount,
                status = "DRAFT",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun addActivity(projectId: Long, title: String) {
        require(title.isNotBlank()) { "Enter a title." }
        suiteDao.insertActivity(
            ScheduleActivityEntity(projectId = projectId, title = title.trim(), progress = 0, status = "PLANNED", createdAt = System.currentTimeMillis()),
        )
    }

    suspend fun addOrder(
        projectId: Long,
        title: String,
        supplier: String,
        itemName: String = "",
        specification: String = "",
        quantity: Double = 0.0,
        unit: String = "",
    ) {
        require(title.isNotBlank()) { "Enter a title." }
        val now = System.currentTimeMillis()
        val orderId = suiteDao.insertOrder(
            MaterialOrderEntity(
                projectId = projectId,
                referenceNumber = "PO-${now % 10000}",
                title = title.trim(),
                supplier = supplier.trim(),
                status = "DRAFT",
                notes = "",
                createdAt = now,
            ),
        )
        if (itemName.isNotBlank()) {
            suiteDao.insertOrderLine(
                MaterialOrderLineEntity(
                    orderId = orderId,
                    itemName = itemName.trim(),
                    specification = specification.trim(),
                    quantity = quantity,
                    unit = unit.ifBlank { "Nos" },
                    remarks = "",
                ),
            )
        }
    }

    suspend fun addRcc(
        projectId: Long,
        projectCode: String,
        element: String,
        mark: String,
        nos: Double,
        length: Double,
        breadth: Double,
        height: Double,
    ): String {
        val quantity = com.holagundi.archiman.domain.QuantityCalculator.calculate(
            CalculationTypes.VOLUME,
            com.holagundi.archiman.domain.MeasurementInput(no = nos, length = length, breadth = breadth, height = height),
        )
        if (quantity <= 0.0) return "Quantity must be greater than zero."
        val now = System.currentTimeMillis()
        suiteDao.insertRcc(
            RccMemberEntity(
                projectId = projectId,
                elementType = element,
                mark = mark.ifBlank { element },
                nos = nos,
                length = length,
                breadth = breadth,
                height = height,
                quantity = quantity,
                remarks = "",
                createdAt = now,
            ),
        )
        return recordNamed(projectId, projectCode, element, "m³", CalculationTypes.VOLUME, nos, length, breadth, height, "RCC $mark")
    }

    suspend fun addCivil(
        projectId: Long,
        projectCode: String,
        trade: String,
        nos: Double,
        length: Double,
        breadth: Double,
        height: Double,
    ): String {
        val (unit, formula) = civilMeasure(trade)
        return recordNamed(projectId, projectCode, trade, unit, formula, nos, length, breadth, height, trade)
    }

    suspend fun applyLink(projectId: Long, projectCode: String, ruleId: Long): String {
        val rule = suiteDao.link(ruleId) ?: return "That link is no longer available."
        val source = measurementDao.measurementsNow(projectId).filter { it.itemName.equals(rule.sourceTrade, ignoreCase = true) }
        val quantity = source.sumOf { it.quantity }
        if (quantity <= 0.0) return "No ${rule.sourceTrade} quantity to link."
        val (unit, _) = civilMeasure(rule.targetTrade)
        return recordNamed(
            projectId,
            projectCode,
            rule.targetTrade,
            unit,
            CalculationTypes.NOS,
            quantity * rule.factor,
            0.0,
            0.0,
            0.0,
            "Derived from ${rule.sourceTrade} × ${rule.factor}",
        )
    }

    fun nextStep(stages: List<ProjectStageEntity>, invoices: List<InvoiceEntity>, receipts: List<ReceiptEntity>): String {
        val paid = receipts.map { it.invoiceId }.toSet()
        return ProjectPath.next(
            hasStages = stages.isNotEmpty(),
            hasOpenStage = stages.any { it.status == "OPEN" },
            hasDraftInvoice = invoices.any { it.status == "DRAFT" },
            hasUnpaidInvoice = invoices.any { it.status == "ISSUED" && it.id !in paid },
        )
    }

    private suspend fun recordNamed(
        projectId: Long,
        projectCode: String,
        itemName: String,
        unit: String,
        formula: String,
        nos: Double,
        length: Double,
        breadth: Double,
        height: Double,
        description: String,
    ): String {
        val floor = projectDao.floors(projectId).firstOrNull() ?: return "Add a level before recording quantities."
        val contractor = catalogDao.firstContractor(projectId) ?: return "Add a contractor before recording quantities."
        val item = catalogDao.findItemByName(itemName)
        val result = measurementRepository.record(
            MeasurementDraft(
                projectId = projectId,
                projectCode = projectCode,
                floorId = floor.id,
                floorName = floor.name,
                roomId = null,
                roomName = null,
                contractorId = contractor.id,
                contractorName = contractor.name,
                itemId = item?.id ?: 0L,
                itemName = itemName,
                unit = unit,
                calculationType = formula,
                description = description,
                location = floor.name,
                remarks = "",
                length = length,
                width = breadth,
                height = height,
                nos = nos,
                deduction = 0.0,
            ),
        )
        return when (result) {
            is RecordResult.Saved -> "Saved on the measurement sheet."
            is RecordResult.Rejected -> result.reason
        }
    }

    private fun civilMeasure(trade: String): Pair<String, String> = when (trade) {
        "Earthwork", "PCC bed", "VDF", "Screed", "Columns", "Beams", "Pedestals", "Lintels", "Slabs", "Footings", "Retaining walls", "Stairs" ->
            "m³" to CalculationTypes.VOLUME
        "Doors", "Windows" -> "Nos" to CalculationTypes.NOS
        "Skirting", "Coping", "DPC" -> "m" to CalculationTypes.RUNNING_LENGTH
        else -> if (CoreOpCatalog.RCC.contains(trade)) "m³" to CalculationTypes.VOLUME else "m²" to CalculationTypes.AREA
    }
}
