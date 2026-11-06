package com.holagundi.archiman.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.holagundi.archiman.data.local.ArchiManDatabase
import com.holagundi.archiman.data.local.entity.ClientEntity
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.ContractorEntity
import com.holagundi.archiman.data.local.entity.ContractorQualifiedItemEntity
import com.holagundi.archiman.data.local.entity.CoordinationItemEntity
import com.holagundi.archiman.data.local.entity.DrawingRevisionEntity
import com.holagundi.archiman.data.local.entity.DrawingTransmittalEntity
import com.holagundi.archiman.data.local.entity.FeeProposalEntity
import com.holagundi.archiman.data.local.entity.DailySiteReportEntity
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.MaterialOrderEntity
import com.holagundi.archiman.data.local.entity.MaterialOrderLineEntity
import com.holagundi.archiman.data.local.entity.ItemMasterEntity
import com.holagundi.archiman.data.local.entity.LetterEntity
import com.holagundi.archiman.data.local.entity.MeasurementEntity
import com.holagundi.archiman.data.local.entity.MeasurementReviewEventEntity
import com.holagundi.archiman.data.local.entity.MeasurementSheetEntity
import com.holagundi.archiman.data.local.entity.ProjectConsultancyProfileEntity
import com.holagundi.archiman.data.local.entity.ProjectDrawingEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.data.local.entity.ProjectTaskEntity
import com.holagundi.archiman.data.local.entity.FloorEntity
import com.holagundi.archiman.data.local.entity.RoomEntity
import com.holagundi.archiman.data.local.entity.SiteInspectionEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEntity
import com.holagundi.archiman.data.local.entity.WorkContractEntity
import com.holagundi.archiman.domain.CoordinationWorkflow
import com.holagundi.archiman.domain.MeasurementInput
import com.holagundi.archiman.domain.QuantityCalculator
import com.holagundi.archiman.domain.SheetStatus
import com.holagundi.archiman.domain.SiteIssueWorkflow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class DemoCompanyStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: ArchiManDatabase,
    private val portal: com.holagundi.archiman.portal.PortalServer? = null,
) {
    private fun readBoundedPackage(input: java.io.InputStream): String {
        val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
        while(true) { val count=input.read(buffer); if(count<0) break; require(output.size()+count<=5*1024*1024) { "Company files must be 5 MB or smaller." }; output.write(buffer,0,count) }
        return output.toString("UTF-8")
    }

    private fun clearPracticeRows() {
        val sql=db.openHelper.writableDatabase
        check(sql.inTransaction()) { "Practice replacement must be transactional." }
        sql.execSQL("PRAGMA defer_foreign_keys = ON")
        val tables=sql.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT IN ('android_metadata','room_master_table')").use { cursor -> buildList { while(cursor.moveToNext()) add(cursor.getString(0)) } }
        tables.forEach { table -> sql.execSQL("DELETE FROM \"${table.replace("\"","\"\"")}\"") }
    }

    fun readPackage(): DemoCompanyPackage {
        val text = context.assets.open(DemoCompanyPackage.ASSET).use(::readBoundedPackage)
        return DemoCompanyPackage.parse(text).also { it.requireOpenable() }
    }

    suspend fun openBuiltIn() {
        openPackage(readPackage())
    }

    /** Additive sample upgrade; never opens/replaces a practice or changes real projects. */
    suspend fun ensureDemoContractors() = ensureDemoContractors(readPackage())

    private suspend fun ensureDemoContractors(pack: DemoCompanyPackage) = db.withTransaction {
        val profile = db.practiceDao().company() ?: return@withTransaction
        val company = pack.source.getJSONObject("company")
        if (!pack.fictional || profile.practiceName != company.getString("practiceName") || profile.legalName != company.getString("legalName")) return@withTransaction
        val projects = db.projectDao().projects()
        val itemSpecs = pack.source.getJSONArray("workItems").objects().associateBy { it.getString("ref") }
        val schedules = ProjectScheduleRepository(db)
        pack.source.getJSONArray("projects").objects().forEach { spec ->
            val project = projects.firstOrNull { it.projectCode == spec.getString("code") && it.name == spec.getString("name") } ?: return@forEach
            val marker = "demo-contractor-works-v1-${project.id}"
            if (db.suiteDao().finance(marker) != null || schedules.revisions(project.id).any { it.status == "DRAFT" }) return@forEach
            val contractorSpecs = spec.optJSONArray("contractors").objects().filter { it.optJSONArray("scheduleItems") != null }
            if (contractorSpecs.isEmpty()) return@forEach
            val existingItems = db.catalogDao().observeActiveItems().first().associateBy { it.itemCode }.toMutableMap()
            val existingContractors = db.catalogDao().observeContractors(project.id).first().associateBy { it.name }.toMutableMap()
            val active = schedules.active(project.id)
            val additions = mutableListOf<Triple<Long, com.holagundi.archiman.data.local.entity.ItemMasterEntity, JSONObject>>()
            contractorSpecs.forEach { contractorSpec ->
                val name = contractorSpec.getString("name")
                val contractor = existingContractors[name] ?: ContractorEntity(projectId=project.id,name=name,address="Fictional demo address",contactNo="",phone="",contractorType=contractorSpec.getString("type"),createdAt=System.currentTimeMillis(),detailsJson=JSONObject().put("notes","Fictional demo contractor. Example rates exclude taxes and are not market quotations.").put("email","demo@example.test").toString()).let { it.copy(id=db.catalogDao().insertContractor(it)) }.also { existingContractors[name]=it }
                contractorSpec.getJSONArray("scheduleItems").objects().forEach { work ->
                    val itemSpec = itemSpecs.getValue(work.getString("itemRef"))
                    val code = itemSpec.getString("itemCode")
                    val item = existingItems[code] ?: ItemMasterEntity(itemCode=code,workType=itemSpec.getString("workType"),name=itemSpec.getString("name"),unit=itemSpec.getString("unit"),calculationType=itemSpec.getString("calculationType"),specification=itemSpec.getString("specification"),sourceName="Fictional demo item",sourceItemCode=itemSpec.getString("sourceItemCode"),isPredefined=true,isActive=true).let { it.copy(id=db.catalogDao().insertItem(it)) }.also { existingItems[code]=it }
                    if (db.catalogDao().observeQualified(contractor.id).first().none { it.itemName == item.name }) db.catalogDao().insertQualified(ContractorQualifiedItemEntity(contractorId=contractor.id,workType=item.workType,itemName=item.name,uom=work.getString("unit"),calculationType=item.calculationType))
                    if (active?.lines.orEmpty().none { it.contractorId==contractor.id && it.itemId==item.id }) additions.add(Triple(contractor.id,item,work))
                }
            }
            if (additions.isNotEmpty()) {
                val revision=schedules.create(project.id)
                additions.forEach { (contractor,item,work) -> schedules.line(revision.key,contractor,item.id,item.name,work.getString("unit"),work.getString("rate"),work.getString("basis")) }
                schedules.activate(revision.key)
            }
            db.suiteDao().saveFinance(com.holagundi.archiman.data.local.entity.FinanceRecordEntity(marker,"DEMO_SEED",JSONObject().put("projectId",project.id).put("fictional",true).toString()))
        }
    }

    suspend fun openFromUri(uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use(::readBoundedPackage)
            ?: error("Could not read the company file.")
        openPackage(DemoCompanyPackage.parse(text).also { it.requireOpenable() })
    }

    suspend fun createBlank(practiceName: String, principalName: String, city: String) {
        val name = practiceName.trim()
        require(name.isNotEmpty()) { "Practice name is required." }
        val now = System.currentTimeMillis()
        portal?.stop(notify = true)
        db.withTransaction {
            clearPracticeRows()
            db.practiceDao().upsertCompany(
                CompanyProfileEntity(
                    practiceName = name,
                    legalName = name,
                    companyType = "Practice",
                    address = "",
                    city = city.trim(),
                    state = "",
                    country = "India",
                    pinCode = "",
                    phone = "",
                    email = "",
                    website = "",
                    pan = "",
                    gstin = "",
                    coaRegistrationNumber = "",
                    principalName = principalName.trim(),
                    principalQualification = "",
                    practiceRegistrationDetails = "",
                    updatedAt = now,
                ),
            )
        }
    }

    private suspend fun openPackage(pack: DemoCompanyPackage) {
        val json = pack.source
        val now = json.optLong("createdAt", 1735689600000L)
        portal?.stop(notify = true)
        db.withTransaction {
            clearPracticeRows()
            val company = json.getJSONObject("company")
            db.practiceDao().upsertCompany(
                CompanyProfileEntity(
                    practiceName = company.getString("practiceName"),
                    legalName = company.getString("legalName"),
                    companyType = company.getString("companyType"),
                    address = company.getString("address"),
                    city = company.getString("city"),
                    state = company.getString("state"),
                    country = company.getString("country"),
                    pinCode = company.getString("pinCode"),
                    phone = company.getString("phone"),
                    email = company.getString("email"),
                    website = company.getString("website"),
                    pan = company.getString("pan"),
                    gstin = company.getString("gstin"),
                    coaRegistrationNumber = company.getString("coaRegistrationNumber"),
                    principalName = company.getString("principalName"),
                    principalQualification = company.getString("principalQualification"),
                    practiceRegistrationDetails = company.getString("practiceRegistrationDetails"),
                    updatedAt = now,
                ),
            )
            val clientNames = json.getJSONArray("clients").objects().associate {
                it.getString("ref") to it.getString("name")
            }
            val clients = mutableMapOf<String, Long>()
            json.getJSONArray("clients").objects().forEach { client ->
                val id = db.practiceDao().insertClient(
                    ClientEntity(
                        name = client.getString("name"),
                        clientType = client.getString("clientType"),
                        contactPerson = client.getString("contactPerson"),
                        address = client.getString("address"),
                        correspondenceAddress = client.getString("correspondenceAddress"),
                        contactNo = client.getString("contactNo"),
                        email = client.getString("email"),
                        preferredCommunication = client.getString("preferredCommunication"),
                        notes = client.getString("notes"),
                        createdAt = now,
                    ),
                )
                clients[client.getString("ref")] = id
            }
            val items = mutableMapOf<String, ItemMasterEntity>()
            json.getJSONArray("workItems").objects().forEach { item ->
                val entity = ItemMasterEntity(
                    itemCode = item.getString("itemCode"),
                    workType = item.getString("workType"),
                    name = item.getString("name"),
                    unit = item.getString("unit"),
                    calculationType = item.getString("calculationType"),
                    specification = item.getString("specification"),
                    sourceName = "PWD SR",
                    sourceItemCode = item.getString("sourceItemCode"),
                    isPredefined = true,
                    isActive = true,
                )
                val id = db.catalogDao().insertItem(entity)
                items[item.getString("ref")] = entity.copy(id = id)
            }
            json.getJSONArray("projects").objects().forEach { project ->
                writeProject(project, clients, clientNames, items, now)
            }
            ensureDemoContractors(pack)
            db.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { require(!it.moveToFirst()) { "The company file has invalid record relationships." } }
        }
    }

    private suspend fun writeProject(
        project: JSONObject,
        clients: Map<String, Long>,
        clientNames: Map<String, String>,
        items: Map<String, ItemMasterEntity>,
        now: Long,
    ) {
        val clientRef = project.getString("clientRef")
        val clientId = clients[clientRef] ?: error("Project ${project.getString("code")} has no client $clientRef.")
        val clientName = clientNames.getValue(clientRef)
        val projectId = db.projectDao().insertProject(
            ProjectEntity(
                name = project.getString("name"),
                client = clientName,
                clientId = clientId,
                siteLocation = project.getString("site"),
                projectCode = project.getString("code"),
                projectType = project.getString("type"),
                status = project.getString("status"),
                description = project.getString("description"),
                architectInCharge = project.getString("architect"),
                plotArea = project.optDouble("plotArea"),
                builtUpArea = project.optDouble("builtUpArea"),
                areaUnit = project.getString("areaUnit"),
                createdAt = project.optLong("createdAt", now),
            ),
        )
        val floors = mutableMapOf<String, Long>()
        project.optJSONArray("floors").objects().forEachIndexed { index, floor ->
            val floorId = db.projectDao().insertFloor(
                FloorEntity(projectId = projectId, name = floor.getString("name"), orderIndex = index),
            )
            floors[floor.getString("name")] = floorId
            floor.optJSONArray("rooms").strings().forEachIndexed { roomIndex, room ->
                db.projectDao().insertRoom(
                    RoomEntity(projectId = projectId, floorId = floorId, name = room, orderIndex = roomIndex),
                )
            }
        }
        if (project.has("brief")) {
            val brief = project.getJSONObject("brief")
            db.suiteDao().insertBrief(
                ProjectConsultancyProfileEntity(
                    projectId = projectId,
                    consultancyTypes = "Architecture",
                    currentPhase = "Design",
                    currentDesignStage = "Brief",
                    briefStatus = "RECORDED",
                    clientObjectives = brief.optString("objectives"),
                    projectRequirements = brief.optString("requirements"),
                    designPreferences = "",
                    siteConstraints = brief.optString("constraints"),
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
                ),
            )
        }
        project.optJSONArray("tasks").objects().forEach { task ->
            db.suiteDao().insertTask(
                ProjectTaskEntity(
                    projectId = projectId,
                    title = task.getString("title"),
                    description = "",
                    status = task.getString("status"),
                    createdAt = now,
                ),
            )
        }
        project.optJSONArray("letters").objects().forEach { letter ->
            db.suiteDao().insertLetter(
                LetterEntity(
                    projectId = projectId,
                    subject = letter.getString("subject"),
                    body = letter.getString("body"),
                    status = "ISSUED",
                    createdAt = now,
                ),
            )
        }
        if (project.has("proposal")) {
            val proposal = project.getJSONObject("proposal")
            db.suiteDao().insertProposal(
                FeeProposalEntity(
                    projectId = projectId,
                    title = proposal.getString("title"),
                    amount = proposal.getDouble("amount"),
                    status = proposal.getString("status"),
                    createdAt = now,
                ),
            )
        }
        if (project.has("contract")) {
            val contract = project.getJSONObject("contract")
            db.suiteDao().insertContract(
                WorkContractEntity(
                    projectId = projectId,
                    title = contract.getString("title"),
                    party = contract.getString("party"),
                    kind = contract.getString("kind"),
                    amount = contract.getDouble("amount"),
                    status = contract.getString("status"),
                    createdAt = now,
                ),
            )
        }
        if (project.has("visit")) {
            val visit = project.getJSONObject("visit")
            db.suiteDao().insertInspection(
                SiteInspectionEntity(
                    projectId = projectId,
                    inspectionAt = now,
                    location = visit.getString("location"),
                    inspector = visit.getString("inspector"),
                    observation = visit.getString("observation"),
                    severity = visit.getString("severity"),
                    correctiveAction = "",
                    status = visit.getString("status"),
                    createdAt = now,
                ),
            )
        }
        project.optJSONArray("snags").objects().forEach { snag ->
            db.suiteDao().insertIssue(
                SiteIssueEntity(
                    projectId = projectId,
                    referenceNumber = snag.getString("referenceNumber"),
                    type = snag.getString("type"),
                    title = snag.getString("title"),
                    location = snag.getString("location"),
                    description = snag.getString("description"),
                    severity = SiteIssueWorkflow.normalizeSeverity(snag.getString("severity")),
                    assignedTo = snag.optString("assignedTo"),
                    correctiveAction = snag.optString("correctiveAction"),
                    status = snag.getString("status"),
                    evidenceUri = snag.optString("evidenceUri").ifBlank { null },
                    verificationNote = "",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        project.optJSONArray("coordination").objects().forEach { item ->
            val type = item.getString("type")
            db.suiteDao().insertCoordination(
                CoordinationItemEntity(
                    projectId = projectId,
                    type = type,
                    referenceNumber = item.getString("referenceNumber"),
                    subject = item.getString("subject"),
                    discipline = item.optString("discipline"),
                    location = item.optString("location"),
                    raisedBy = item.optString("raisedBy"),
                    assignedTo = item.optString("assignedTo"),
                    questionOrRequirement = item.optString("question"),
                    response = item.optString("response"),
                    status = item.optString("status", CoordinationWorkflow.initialStatus(type)),
                    priority = "NORMAL",
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        project.optJSONArray("drawings").objects().forEach { drawing ->
            val drawingId = db.suiteDao().insertDrawing(
                ProjectDrawingEntity(
                    projectId = projectId,
                    drawingNumber = drawing.getString("number"),
                    title = drawing.getString("title"),
                    discipline = drawing.optString("discipline", "Architectural"),
                    status = drawing.optString("status", "CURRENT"),
                    packageTag = drawing.optString("packageTag"),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            drawing.optJSONArray("revisions").objects().forEach { revision ->
                db.suiteDao().insertRevision(
                    DrawingRevisionEntity(
                        projectId = projectId,
                        drawingId = drawingId,
                        revisionCode = revision.getString("code"),
                        fileName = revision.optString("fileNote"),
                        mimeType = "",
                        fileUri = revision.optString("fileNote"),
                        fileChecksum = "",
                        issueStatus = "ISSUED",
                        revisionNotes = revision.optString("notes"),
                        isAsBuilt = revision.optBoolean("asBuilt", false),
                        revisionSource = "PRACTICE",
                        severity = SiteIssueWorkflow.normalizeSeverity(revision.optString("severity", "NORMAL")),
                        issuedAt = now,
                        createdAt = now,
                    ),
                )
            }
        }
        project.optJSONArray("logs").objects().forEach { log ->
            db.suiteDao().insertLog(
                DailySiteReportEntity(
                    projectId = projectId,
                    reportDate = now,
                    weather = log.optString("weather", "Fair"),
                    manpower = log.optString("manpower"),
                    workCompleted = log.getString("work"),
                    materialsReceived = log.optString("materials"),
                    delaysOrConstraints = log.optString("delays"),
                    safetyObservations = log.optString("safety"),
                    nextDayPlan = log.optString("nextDay"),
                    preparedBy = log.optString("preparedBy", "Site"),
                    createdAt = now,
                    updatedAt = now,
                ),
            )
        }
        project.optJSONArray("orders").objects().forEach { order ->
            val orderId = db.suiteDao().insertOrder(
                MaterialOrderEntity(
                    projectId = projectId,
                    referenceNumber = order.getString("number"),
                    title = order.getString("title"),
                    supplier = order.optString("supplier"),
                    status = "DRAFT",
                    notes = "",
                    createdAt = now,
                ),
            )
            if (order.optString("item").isNotBlank()) {
                db.suiteDao().insertOrderLine(
                    MaterialOrderLineEntity(
                        orderId = orderId,
                        itemName = order.getString("item"),
                        specification = order.optString("specification"),
                        quantity = order.optDouble("quantity", 0.0),
                        unit = order.optString("unit", "Nos"),
                        remarks = "",
                    ),
                )
            }
        }
        project.optJSONArray("transmittals").objects().forEach { item ->
            db.suiteDao().insertTransmittal(
                DrawingTransmittalEntity(
                    projectId = projectId,
                    transmittalNumber = item.getString("number"),
                    subject = item.getString("subject"),
                    recipients = item.optString("recipients"),
                    purpose = item.optString("purpose", "For information"),
                    notes = "",
                    issuedAt = now,
                ),
            )
        }
        if (project.has("invoice")) {
            val invoice = project.getJSONObject("invoice")
            db.suiteDao().insertInvoice(
                InvoiceEntity(
                    projectId = projectId,
                    stageId = null,
                    number = invoice.getString("number"),
                    title = invoice.getString("title"),
                    amount = invoice.getDouble("amount"),
                    status = invoice.getString("status"),
                    createdAt = now,
                ),
            )
        }
        val contractors = mutableMapOf<String, Long>()
        project.optJSONArray("contractors").objects().forEach { contractor ->
            val contractorId = db.catalogDao().insertContractor(
                ContractorEntity(
                    projectId = projectId,
                    name = contractor.getString("name"),
                    address = "",
                    contactNo = contractor.optString("phone"),
                    phone = contractor.optString("phone"),
                    contractorType = contractor.getString("type"),
                    createdAt = now,
                ),
            )
            contractors[contractor.getString("name")] = contractorId
            contractor.optJSONArray("qualified").objects().forEach { qualified ->
                val item = items.getValue(qualified.getString("itemRef"))
                db.catalogDao().insertQualified(
                    ContractorQualifiedItemEntity(
                        contractorId = contractorId,
                        workType = qualified.getString("workType"),
                        itemName = item.name,
                        uom = item.unit,
                        calculationType = item.calculationType,
                    ),
                )
            }
        }
        project.optJSONArray("sheets").objects().forEach { sheet ->
            val item = items.getValue(sheet.getString("itemRef"))
            val floorName = sheet.getString("floor")
            val contractorName = sheet.getString("contractor")
            val status = sheet.getString("status")
            val sheetId = db.measurementDao().insertSheet(
                MeasurementSheetEntity(
                    sheetCode = sheet.getString("code"),
                    projectId = projectId,
                    floorId = floors[floorName],
                    floorNameSnapshot = floorName,
                    contractorId = contractors.getValue(contractorName),
                    contractorNameSnapshot = contractorName,
                    itemId = item.id,
                    itemNameSnapshot = item.name,
                    uomSnapshot = item.unit,
                    formulaCode = item.calculationType,
                    formulaVersion = 1,
                    status = status,
                    revision = 1,
                    lockedAt = if (status == SheetStatus.APPROVED) now else null,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            sheet.optJSONArray("rows").objects().forEach { row ->
                val quantity = QuantityCalculator.calculate(
                    item.calculationType,
                    MeasurementInput(
                        no = row.optDouble("nos", 1.0),
                        length = row.optDouble("length", 0.0),
                        breadth = row.optDouble("breadth", 0.0),
                        height = row.optDouble("height", 0.0),
                        deduction = row.optDouble("deduction", 0.0),
                    ),
                )
                db.measurementDao().insertMeasurement(
                    MeasurementEntity(
                        sheetId = sheetId,
                        projectId = projectId,
                        floorId = floors[floorName],
                        contractorId = contractors.getValue(contractorName),
                        contractorName = contractorName,
                        itemId = item.id,
                        itemName = item.name,
                        unit = item.unit,
                        calculationType = item.calculationType,
                        formulaCode = item.calculationType,
                        formulaVersion = 1,
                        description = row.getString("description"),
                        length = row.optDouble("length", 0.0),
                        width = row.optDouble("breadth", 0.0),
                        height = row.optDouble("height", 0.0),
                        nos = row.optDouble("nos", 1.0),
                        deduction = row.optDouble("deduction", 0.0),
                        quantity = quantity,
                        floor = floorName,
                        location = row.optString("location"),
                        remarks = row.optString("remarks"),
                        date = now,
                    ),
                )
            }
            if (sheet.has("review")) {
                val review = sheet.getJSONObject("review")
                db.measurementDao().insertReview(
                    MeasurementReviewEventEntity(
                        sheetId = sheetId,
                        fromStatus = review.getString("from"),
                        toStatus = review.getString("to"),
                        comment = review.getString("comment"),
                        actor = review.getString("actor"),
                        revision = 1,
                        createdAt = now,
                    ),
                )
            }
        }
    }
}

private fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    return List(length()) { getJSONObject(it) }
}

private fun JSONArray?.strings(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { getString(it) }
}
