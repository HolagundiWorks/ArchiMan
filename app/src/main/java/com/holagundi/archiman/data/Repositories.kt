package com.holagundi.archiman.data

import com.holagundi.archiman.data.local.CatalogDao
import com.holagundi.archiman.data.local.FieldDao
import com.holagundi.archiman.data.local.MeasurementDao
import com.holagundi.archiman.data.local.PracticeDao
import com.holagundi.archiman.data.local.ProjectDao
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.ClientEntity
import com.holagundi.archiman.data.local.entity.FloorEntity
import com.holagundi.archiman.data.local.entity.MeasurementEntity
import com.holagundi.archiman.data.local.entity.MeasurementReviewEventEntity
import com.holagundi.archiman.data.local.entity.MeasurementSheetEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.data.local.entity.RoomEntity
import com.holagundi.archiman.domain.InvoiceBalance
import com.holagundi.archiman.domain.MeasurementDraft
import com.holagundi.archiman.domain.MeasurementInput
import com.holagundi.archiman.domain.MeasurementSheetWorkflow
import com.holagundi.archiman.domain.ProjectModuleCounts
import com.holagundi.archiman.domain.QuantityCalculator
import com.holagundi.archiman.domain.RecordResult
import com.holagundi.archiman.domain.SheetExport
import com.holagundi.archiman.domain.SheetExportRow
import com.holagundi.archiman.domain.SheetStatus
import androidx.room.withTransaction
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeRepository @Inject constructor(
    private val practiceDao: PracticeDao,
) {
    fun observeCompany() = practiceDao.observeCompany()
    fun observeClients() = practiceDao.observeClients()

    suspend fun addClient(name: String, phone: String, email: String) {
        require(name.isNotBlank()) { "Enter a client name." }
        practiceDao.insertClient(
            ClientEntity(
                name = name.trim(),
                clientType = "Client",
                contactPerson = name.trim(),
                address = "",
                correspondenceAddress = "",
                contactNo = phone.trim(),
                email = email.trim(),
                preferredCommunication = "Phone",
                notes = "",
                createdAt = System.currentTimeMillis(),
            ),
        )
    }
}

@Singleton
class ProjectRepository @Inject constructor(
    private val db: ArchiManDatabase,
    private val projectDao: ProjectDao,
    private val practiceDao: PracticeDao,
) {
    fun observeProjects() = projectDao.observeProjects()
    fun observeProject(id: Long) = projectDao.observeProject(id)
    fun observeFloors(projectId: Long) = projectDao.observeFloors(projectId)
    fun observeRooms(projectId: Long) = projectDao.observeRooms(projectId)

    suspend fun createProject(
        name: String,
        code: String,
        clientName: String,
        projectType: String,
        siteLocation: String,
        architect: String,
    ): Long = db.withTransaction {
        val now = System.currentTimeMillis()
        val clientId = practiceDao.insertClient(
            ClientEntity(
                name = clientName.trim(),
                clientType = "Client",
                contactPerson = clientName.trim(),
                address = siteLocation.trim(),
                correspondenceAddress = siteLocation.trim(),
                contactNo = "",
                email = "",
                preferredCommunication = "Phone",
                notes = "",
                createdAt = now,
            ),
        )
        val projectCode = code.trim().ifEmpty { "PRJ-${now % 100000}" }
        val projectId = projectDao.insertProject(
            ProjectEntity(
                name = name.trim(),
                client = clientName.trim(),
                clientId = clientId,
                siteLocation = siteLocation.trim(),
                projectCode = projectCode,
                projectType = projectType.trim().ifEmpty { "Residential" },
                status = "ACTIVE",
                description = "",
                architectInCharge = architect,
                areaUnit = "m²",
                createdAt = now,
            ),
        )
        projectDao.insertFloor(FloorEntity(projectId = projectId, name = "Ground Floor", orderIndex = 0))
        projectId
    }

    suspend fun addFloor(projectId: Long, name: String) {
        val order = projectDao.floors(projectId).size
        projectDao.insertFloor(FloorEntity(projectId = projectId, name = name.trim(), orderIndex = order))
    }

    suspend fun addRoom(projectId: Long, floorId: Long, name: String) {
        val order = projectDao.roomsOnFloor(projectId, floorId).size
        projectDao.insertRoom(
            RoomEntity(projectId = projectId, floorId = floorId, name = name.trim(), orderIndex = order),
        )
    }
}

@Singleton
class CatalogRepository @Inject constructor(
    private val catalogDao: CatalogDao,
) {
    fun observeContractors(projectId: Long) = catalogDao.observeContractors(projectId)
    fun observeQualified(contractorId: Long) = catalogDao.observeQualified(contractorId)
    fun observeActiveItems() = catalogDao.observeActiveItems()
}

@Singleton
class MeasurementRepository @Inject constructor(
    private val db: ArchiManDatabase,
    private val measurementDao: MeasurementDao,
    private val catalogDao: CatalogDao,
    private val projectDao: ProjectDao,
) {
    fun observeSheets(projectId: Long) = measurementDao.observeSheets(projectId)
    fun observeMeasurements(projectId: Long) = measurementDao.observeMeasurements(projectId)

    suspend fun record(draft: MeasurementDraft): RecordResult = db.withTransaction {
        if (draft.contractorId == 0L || draft.itemName.isBlank()) {
            return@withTransaction RecordResult.Rejected("Choose a contractor and a work item.")
        }
        val schedule = ProjectScheduleRepository(db).active(draft.projectId)
            ?: return@withTransaction RecordResult.Rejected("Activate a project contractor-item revision in the web portal first.")
        val assigned = schedule.lines.firstOrNull { it.key == draft.boqLineKey && it.contractorId == draft.contractorId }
            ?: return@withTransaction RecordResult.Rejected("This item is not assigned to the selected contractor in the active revision.")
        if (draft.boqRevision != schedule.label || draft.itemId != assigned.itemId || draft.itemName != assigned.description || draft.unit != assigned.unit || draft.calculationType != assigned.formula || InvoiceBalance.money(draft.rateSnapshot).toPlainString() != assigned.rate || draft.contractBasis != assigned.basis)
            return@withTransaction RecordResult.Rejected("The project schedule changed. Reopen the entry and select the active item again.")
        val project = projectDao.getProject(draft.projectId)
        if (project == null || project.status == "ARCHIVED") return@withTransaction RecordResult.Rejected("Select an active project.")
        val floor = db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM floors WHERE id = ? AND projectId = ?", arrayOf(draft.floorId, draft.projectId)).use { it.moveToFirst(); it.getInt(0) > 0 }
        val contractor = db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM contractors WHERE id = ? AND (projectId = ? OR projectId = 0)", arrayOf(draft.contractorId, draft.projectId)).use { it.moveToFirst(); it.getInt(0) > 0 }
        if (!floor || !contractor) return@withTransaction RecordResult.Rejected("Choose a valid floor and contractor for this project.")
        if (listOf(draft.nos, draft.length, draft.width, draft.height, draft.deduction).any { !it.isFinite() || it < 0 || it > 1e9 }) return@withTransaction RecordResult.Rejected("Dimensions must be finite, non-negative numbers.")
        val quantity = QuantityCalculator.calculate(
            draft.calculationType,
            MeasurementInput(
                no = draft.nos,
                length = draft.length,
                breadth = draft.width,
                height = draft.height,
                deduction = draft.deduction,
            ),
        )
        if (quantity <= 0.0) {
            return@withTransaction RecordResult.Rejected("Quantity must be greater than zero.")
        }
        val now = System.currentTimeMillis()
        val item = catalogDao.findItemByName(draft.itemName)
        val existing = measurementDao.findOpenSheet(
            draft.projectId,
            draft.contractorId,
            draft.itemName,
            draft.floorId, draft.unit, draft.calculationType, draft.boqRevision, draft.boqLineKey,
        )
        val sheetId = existing?.id ?: measurementDao.insertSheet(
            MeasurementSheetEntity(
                sheetCode = "MS-${draft.projectCode}-${(measurementDao.sheetCount(draft.projectId) + 1).toString().padStart(3, '0')}",
                projectId = draft.projectId,
                floorId = draft.floorId,
                floorNameSnapshot = draft.floorName,
                contractorId = draft.contractorId,
                contractorNameSnapshot = draft.contractorName,
                itemId = item?.id ?: draft.itemId,
                itemNameSnapshot = draft.itemName,
                boqRevision = draft.boqRevision, boqLineKey = draft.boqLineKey, rateSnapshot = draft.rateSnapshot, contractBasis = draft.contractBasis,
                uomSnapshot = draft.unit,
                formulaCode = draft.calculationType,
                formulaVersion = 1,
                status = SheetStatus.DRAFT,
                revision = 1,
                createdAt = now,
                updatedAt = now,
            ),
        )
        measurementDao.insertMeasurement(
            MeasurementEntity(
                sheetId = sheetId,
                projectId = draft.projectId,
                floorId = draft.floorId,
                roomId = draft.roomId,
                contractorId = draft.contractorId,
                contractorName = draft.contractorName,
                itemId = item?.id ?: draft.itemId,
                itemName = draft.itemName,
                boqRevision = draft.boqRevision, boqLineKey = draft.boqLineKey, rateSnapshot = draft.rateSnapshot, contractBasis = draft.contractBasis,
                unit = draft.unit,
                calculationType = draft.calculationType,
                formulaCode = draft.calculationType,
                formulaVersion = 1,
                description = draft.description.ifBlank { "${draft.itemName} entry" },
                length = draft.length,
                width = draft.width,
                height = draft.height,
                nos = draft.nos,
                deduction = draft.deduction,
                quantity = quantity,
                floor = draft.floorName,
                location = draft.location.ifBlank { draft.roomName ?: "General" },
                remarks = draft.remarks,
                date = now,
            ),
        )
        RecordResult.Saved(sheetId)
    }

    suspend fun transition(sheetId: Long, toStatus: String, comment: String, actor: String): Boolean = db.withTransaction {
        val sheet = measurementDao.getSheet(sheetId) ?: return@withTransaction false
        if (!MeasurementSheetWorkflow.canTransition(sheet.status, toStatus, comment)) return@withTransaction false
        val now = System.currentTimeMillis()
        measurementDao.insertReview(
            MeasurementReviewEventEntity(
                sheetId = sheet.id,
                fromStatus = sheet.status,
                toStatus = toStatus,
                comment = comment.trim(),
                actor = actor,
                revision = sheet.revision,
                createdAt = now,
            ),
        )
        measurementDao.updateSheet(
            sheet.copy(
                status = toStatus,
                updatedAt = now,
                lockedAt = if (toStatus == SheetStatus.APPROVED) now else sheet.lockedAt,
            ),
        )
        true
    }

    suspend fun deleteMeasurement(id: Long): Boolean = db.withTransaction {
        val row = measurementDao.getMeasurement(id) ?: return@withTransaction false
        val sheet = measurementDao.getSheet(row.sheetId) ?: return@withTransaction false
        if (sheet.status !in listOf(SheetStatus.DRAFT, SheetStatus.RETURNED)) return@withTransaction false
        measurementDao.deleteMeasurement(id)
        true
    }

    suspend fun export(projectId: Long, sheetId: Long): SheetExport? {
        val project = projectDao.getProject(projectId) ?: return null
        val sheet = measurementDao.getSheet(sheetId) ?: return null
        if(sheet.projectId!=projectId) return null
        val rows = measurementDao.measurementsForSheet(sheetId)
        return SheetExport(
            projectName = project.name,
            projectCode = project.projectCode,
            sheetCode = sheet.sheetCode,
            floorName = sheet.floorNameSnapshot,
            contractorName = sheet.contractorNameSnapshot,
            itemName = sheet.itemNameSnapshot,
            boqRevision = sheet.boqRevision, rateSnapshot = sheet.rateSnapshot, contractBasis = sheet.contractBasis,
            formulaCode = sheet.formulaCode,
            status = sheet.status,
            unit = sheet.uomSnapshot,
            rows = rows.map { row ->
                SheetExportRow(
                    description = row.description,
                    location = row.location,
                    length = row.length,
                    width = row.width,
                    height = row.height,
                    nos = row.nos,
                    deduction = row.deduction,
                    quantity = row.quantity,
                    remarks = row.remarks,
                    dateMillis = row.date,
                )
            },
        )
    }
}

@Singleton
class FieldRepository @Inject constructor(
    private val fieldDao: FieldDao,
) {
    fun observeCounts(projectId: Long) = combine(
        combine(
            fieldDao.observeDailyLogCount(projectId),
            fieldDao.observeMaterialOrderCount(projectId),
            fieldDao.observeDrawingCount(projectId),
        ) { daily, orders, drawings -> Triple(daily, orders, drawings) },
        combine(
            fieldDao.observeSiteIssueCount(projectId),
            fieldDao.observeInspectionCount(projectId),
            fieldDao.observeMinutesCount(projectId),
        ) { issues, inspections, minutes -> Triple(issues, inspections, minutes) },
        combine(
            fieldDao.observeCoordinationCount(projectId),
            fieldDao.observeDecisionCount(projectId),
            fieldDao.observeTaskCount(projectId),
        ) { coordination, decisions, tasks -> Triple(coordination, decisions, tasks) },
    ) { site, quality, office ->
        ProjectModuleCounts(
            dailyLogs = site.first,
            materialOrders = site.second,
            drawings = site.third,
            siteIssues = quality.first,
            inspections = quality.second,
            minutes = quality.third,
            coordination = office.first,
            decisions = office.second,
            tasks = office.third,
        )
    }
}
