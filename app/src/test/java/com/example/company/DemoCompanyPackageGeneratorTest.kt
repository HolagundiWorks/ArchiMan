package com.example.company

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DATABASE_SCHEMA_VERSION
import com.example.data.local.entity.*
import com.example.data.repository.SiteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.Instant
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
class DemoCompanyPackageGeneratorTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val at: (String) -> Long = { Instant.parse(it).toEpochMilli() }

    @Test fun createsCompleteImportableDemoCompany() = runBlocking {
        val databaseName = "archiman-complete-demo.db"
        context.deleteDatabase(databaseName)
        CompanyDatabasePackageManager.MANAGED_DIRECTORIES.forEach { File(context.filesDir, it).deleteRecursively() }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .allowMainThreadQueries()
            .build()
        try {
            AppDatabase.initializeNewDatabase(database.openHelper.writableDatabase)
            val logo = managedPng("company", "atelier-nila-logo.png")
            val measurementPhoto = managedPng("measurement_attachments", "demo-site-evidence.png")
            val repository = SiteRepository(database)
            val adminId = repository.createLocalUser("admin", "Ananya Rao · Practice Admin", "DemoAdmin#2026".toCharArray(), "ADMIN")
            val architectId = repository.createLocalUser("architect", "Vikram Desai · Project Architect", "DemoEditor#2026".toCharArray(), "EDITOR")
            repository.createLocalUser("siteviewer", "Meera Nair · Site Reviewer", "DemoViewer#2026".toCharArray(), "VIEWER")

            database.withTransaction {
                database.companyProfileDao().upsert(
                    CompanyProfileEntity(
                        practiceName = "Atelier Nila Architects · Demo",
                        legalName = "Atelier Nila Design Studio Private Limited",
                        companyType = "Architecture and urban design practice",
                        address = "42, 2nd Cross, Indiranagar",
                        city = "Bengaluru",
                        state = "Karnataka",
                        country = "India",
                        pinCode = "560038",
                        phone = "+91 80 4000 2026",
                        email = "studio@atelier-nila.example",
                        website = "https://atelier-nila.example",
                        pan = "AABCA1234D",
                        gstin = "29AABCA1234D1Z5",
                        coaRegistrationNumber = "CA/DEMO/2026/001",
                        principalName = "Ar. Ananya Rao",
                        principalQualification = "B.Arch, M.Arch (Urban Design)",
                        practiceRegistrationDetails = "Fictional demonstration practice. All identifiers and contacts are test data.",
                        logoUri = Uri.fromFile(logo).toString(),
                        updatedAt = at("2026-09-13T08:30:00Z")
                    )
                )

                val clients = database.clientDao()
                val clientIds = listOf(
                    ClientEntity(name = "Rohan and Kavya Iyer", clientType = "Individual", contactPerson = "Kavya Iyer", address = "Jayanagar, Bengaluru", correspondenceAddress = "Jayanagar, Bengaluru 560041", contactNo = "+91 90000 10001", email = "kavya.iyer@example.test", preferredCommunication = "Email", notes = "Joint residential client; decisions require both owners."),
                    ClientEntity(name = "Saffron Loop Technologies Pvt Ltd", clientType = "Company", contactPerson = "Neel Shah", address = "Whitefield, Bengaluru", correspondenceAddress = "EPIP Zone, Whitefield 560066", contactNo = "+91 90000 10002", email = "facilities@saffronloop.example", preferredCommunication = "Email", notes = "Corporate workplace demonstration client."),
                    ClientEntity(name = "Aarohan Education Trust", clientType = "Trust", contactPerson = "Dr Lakshmi Menon", address = "Mysuru, Karnataka", correspondenceAddress = "Vijayanagar, Mysuru 570017", contactNo = "+91 90000 10003", email = "projects@aarohan.example", preferredCommunication = "Phone", notes = "Institutional project with consultant coordination."),
                    ClientEntity(name = "Indus Heritage Collective", clientType = "Society", contactPerson = "Farah Khan", address = "Basavanagudi, Bengaluru", correspondenceAddress = "Basavanagudi 560004", contactNo = "+91 90000 10004", email = "committee@indusheritage.example", preferredCommunication = "WhatsApp", notes = "Committee approvals are minuted.")
                ).map { clients.insertClient(it) }

                val projectSpecs = listOf(
                    ProjectEntity(name = "Iyer Courtyard Residence", client = "Rohan and Kavya Iyer", clientId = clientIds[0], siteLocation = "Jayanagar, Bengaluru", projectCode = "AN-RES-026", projectType = "Residential", status = "ACTIVE", description = "G+2 climate-responsive courtyard home on an east-facing urban plot.", architectInCharge = "Vikram Desai", startDate = at("2026-01-12T00:00:00Z"), targetCompletionDate = at("2027-03-31T00:00:00Z"), plotArea = 334.45, builtUpArea = 612.0),
                    ProjectEntity(name = "Saffron Loop Workplace", client = "Saffron Loop Technologies Pvt Ltd", clientId = clientIds[1], siteLocation = "Whitefield, Bengaluru", projectCode = "AN-COM-014", projectType = "Commercial", status = "ACTIVE", description = "Two-floor workplace fit-out for 180 staff with phased occupation.", architectInCharge = "Ananya Rao", startDate = at("2026-06-01T00:00:00Z"), targetCompletionDate = at("2026-12-15T00:00:00Z"), plotArea = 0.0, builtUpArea = 2415.0),
                    ProjectEntity(name = "Aarohan Learning Centre", client = "Aarohan Education Trust", clientId = clientIds[2], siteLocation = "Mysuru, Karnataka", projectCode = "AN-INS-008", projectType = "Institutional", status = "ACTIVE", description = "Inclusive learning centre with classrooms, library and multipurpose hall.", architectInCharge = "Meera Nair", startDate = at("2025-11-20T00:00:00Z"), targetCompletionDate = at("2027-06-30T00:00:00Z"), plotArea = 4850.0, builtUpArea = 3280.0),
                    ProjectEntity(name = "Indus House Conservation", client = "Indus Heritage Collective", clientId = clientIds[3], siteLocation = "Basavanagudi, Bengaluru", projectCode = "AN-CON-003", projectType = "Conservation", status = "ON_HOLD", description = "Measured documentation and sensitive adaptive reuse of a 1930s residence.", architectInCharge = "Ananya Rao", startDate = at("2026-02-10T00:00:00Z"), targetCompletionDate = at("2027-01-31T00:00:00Z"), plotArea = 510.0, builtUpArea = 440.0)
                )
                val projectIds = projectSpecs.map { database.projectDao().insertProject(it) }

                projectIds.forEachIndexed { index, projectId ->
                    database.projectConsultancyDao().upsertProfile(
                        ProjectConsultancyProfileEntity(
                            projectId = projectId,
                            consultancyTypes = if (index == 1) "Architecture, Interiors, Coordination" else "Architecture, Site review, Coordination",
                            currentPhase = listOf("EXECUTION", "EXECUTION", "DESIGN", "DOCUMENTATION")[index],
                            currentDesignStage = listOf("GFC", "FIT_OUT", "DESIGN_DEVELOPMENT", "MEASURED_SURVEY")[index],
                            briefStatus = if (index == 3) "IN_PROGRESS" else "COMPLETE",
                            clientObjectives = listOf("Daylit family home with privacy and passive cooling.", "Collaborative workplace delivered without interrupting operations.", "Accessible, durable and low-maintenance learning environment.", "Conserve significant fabric while enabling community use.")[index],
                            projectRequirements = "Maintain drawing control, decision register, consultant coordination and quantity-only site records.",
                            designPreferences = "Natural materials, restrained palette and clear wayfinding.",
                            siteConstraints = listOf("Narrow access; occupied neighbours; monsoon sequencing.", "Live office below; restricted delivery hours.", "Expansive soil and staged funding.", "Fragile lime masonry; committee approvals.")[index],
                            clarifications = "Demo record: verify dimensions and approvals before issue.",
                            siteDimensions = listOf("12.2 m × 27.4 m", "Two leased floors · 1,207.5 m² each", "Approx. 62 m × 78 m", "17 m × 30 m")[index],
                            siteOrientation = listOf("East-facing", "Internal commercial floorplate", "North-east entry", "South-facing street")[index],
                            siteAccess = "Access recorded with delivery and emergency constraints.",
                            existingConditions = "Surveyed; photographs and drawing references logged.",
                            surroundings = "Mixed urban context with active adjoining properties.",
                            topography = if (index == 2) "Gentle 1:24 fall to south-west" else "Generally level",
                            utilities = "Power, water, drainage and data points identified.",
                            existingStructures = if (index == 3) "Load-bearing brick and lime structure retained" else "As documented in survey",
                            vegetation = if (index == 0) "Existing mango tree retained" else "Landscape inventory recorded",
                            legalPlanningInformation = "Demonstration only; no statutory-compliance claim.",
                            updatedAt = at("2026-09-13T09:00:00Z") + index * 60_000L
                        )
                    )
                    listOf(
                        ProjectScopeItemEntity(projectId = projectId, category = "SCOPE", title = "Architectural design", details = "Brief through construction documentation and periodic site review.", status = "INCLUDED", orderIndex = 1),
                        ProjectScopeItemEntity(projectId = projectId, category = "DELIVERABLE", title = "Controlled drawing issues", details = "Revision register and transmittals maintained in ArchiMan.", status = "INCLUDED", orderIndex = 2),
                        ProjectScopeItemEntity(projectId = projectId, category = "EXCLUSION", title = "Rates and billing", details = "Commercial valuation, invoices and payments are outside ArchiMan.", status = "EXCLUDED", orderIndex = 3)
                    ).forEach { database.projectConsultancyDao().insertScopeItem(it) }
                    listOf(
                        "PROJECT_VISION" to "A context-responsive, durable and inclusive project.",
                        "OCCUPANCY" to listOf("Five family members", "180 staff", "240 learners and 35 staff", "Community events up to 80 people")[index],
                        "PRIORITIES" to "Safety, daylight, maintainability and clear project records."
                    ).forEach { (code, answer) -> database.projectConsultancyDao().upsertOnboardingResponse(ProjectOnboardingResponseEntity(projectId = projectId, templateVersion = 1, questionCode = code, answer = answer)) }
                }

                val mainProject = projectIds[0]
                val workplace = projectIds[1]
                val school = projectIds[2]
                val conservation = projectIds[3]

                listOf(
                    ProjectApprovalEntity(projectId = mainProject, approvalType = "CLIENT", title = "Courtyard massing", description = "Approve revised courtyard proportion.", phase = "DESIGN", status = "APPROVED", submittedAt = at("2026-07-12T10:00:00Z"), approvedAt = at("2026-07-15T11:30:00Z"), remarks = "Approved in client review."),
                    ProjectApprovalEntity(projectId = mainProject, approvalType = "TECHNICAL", title = "Stair reinforcement detail", description = "Structural consultant review required.", phase = "EXECUTION", status = "PENDING", submittedAt = at("2026-09-10T09:00:00Z")),
                    ProjectApprovalEntity(projectId = workplace, approvalType = "CLIENT", title = "Workstation mock-up", description = "Confirm finish and cable-management details.", phase = "EXECUTION", status = "PENDING")
                ).forEach { database.projectConsultancyDao().insertApproval(it) }

                listOf(
                    ProjectBacklogEntity(projectId = mainProject, category = "CLIENT_INPUT", title = "Confirm puja-room joinery", description = "Obtain final deity shelf dimensions.", priority = "HIGH", phase = "EXECUTION", dueAt = at("2026-09-18T00:00:00Z")),
                    ProjectBacklogEntity(projectId = mainProject, category = "COORDINATION", title = "Resolve terrace rainwater outlet", description = "Align architecture, structure and plumbing sleeves.", priority = "HIGH", phase = "EXECUTION", dueAt = at("2026-09-14T00:00:00Z")),
                    ProjectBacklogEntity(projectId = workplace, category = "DESIGN", title = "Issue acoustic ceiling layout", description = "Incorporate services coordination comments.", priority = "MEDIUM", phase = "EXECUTION", status = "CLOSED"),
                    ProjectBacklogEntity(projectId = conservation, category = "GENERAL", title = "Committee funding confirmation", description = "Resume measured documentation after written confirmation.", priority = "MEDIUM", phase = "DOCUMENTATION")
                ).forEach { database.projectConsultancyDao().insertBacklogItem(it) }

                listOf(
                    ProjectTaskEntity(projectId = mainProject, title = "Verify first-floor sill levels", description = "Cross-check against A-204 R2 before blockwork continues.", dueDate = at("2026-09-12T00:00:00Z"), status = "OPEN"),
                    ProjectTaskEntity(projectId = mainProject, title = "Close staircase shuttering inspection", description = "Upload observations and corrective action.", dueDate = at("2026-09-16T00:00:00Z"), status = "IN_PROGRESS"),
                    ProjectTaskEntity(projectId = workplace, title = "Review lighting control samples", description = "Coordinate electrical consultant and client FM.", dueDate = at("2026-09-20T00:00:00Z"), status = "OPEN"),
                    ProjectTaskEntity(projectId = school, title = "Complete accessibility review", description = "Review ramp, toilet and tactile-wayfinding drawings.", dueDate = at("2026-09-08T00:00:00Z"), status = "DONE")
                ).forEach { database.projectTaskDao().insert(it) }

                listOf(
                    ProjectSelectionItemEntity(projectId = mainProject, itemName = "Exterior clay jaali", specification = "High-fired modular clay jaali, approved sample, natural terracotta finish.", makeOrBrand = "Approved equivalent", quantity = 86.0, unit = "m²", status = "APPROVED", remarks = "Coordinate module with structural frame."),
                    ProjectSelectionItemEntity(projectId = mainProject, itemName = "Flooring stone", specification = "20 mm honed Kota stone with calibrated thickness.", makeOrBrand = "Local quarry sample QS-02", quantity = 214.0, unit = "m²", status = "SELECTED"),
                    ProjectSelectionItemEntity(projectId = workplace, itemName = "Acoustic ceiling tile", specification = "NRC ≥ 0.80 mineral fibre tile, 600 × 600 mm.", makeOrBrand = "Approved equivalent", quantity = 1180.0, unit = "m²", status = "PENDING")
                ).forEach { database.projectSelectionItemDao().insert(it) }

                listOf(
                    ProjectScheduleEntity(projectId = mainProject, title = "Weekly site review", scheduledAt = at("2026-09-15T04:30:00Z"), location = "Iyer Residence site", notes = "Review masonry, sleeves and waterproofing mock-up."),
                    ProjectScheduleEntity(projectId = workplace, title = "Services coordination workshop", scheduledAt = at("2026-09-17T05:30:00Z"), location = "Site meeting room", notes = "Close reflected-ceiling clashes."),
                    ProjectScheduleEntity(projectId = school, title = "Client design presentation", scheduledAt = at("2026-09-22T06:00:00Z"), location = "Trust office", notes = "Present developed-design package."),
                    ProjectScheduleEntity(projectId = conservation, title = "Measured survey", scheduledAt = at("2026-08-10T04:00:00Z"), location = "Indus House", notes = "Completed before project hold.", status = "DONE")
                ).forEach { database.projectScheduleDao().insert(it) }

                listOf(
                    MeetingMinutesEntity(projectId = mainProject, title = "Site coordination meeting 18", meetingAt = at("2026-09-11T05:00:00Z"), location = "Site office", attendees = "Client, architect, civil contractor, structural consultant", discussion = "First-floor masonry sequence, terrace drainage and staircase shuttering.", decisions = "Retain east-window width; shift rainwater outlet 150 mm.", actionItems = "Contractor to revise sleeve location; architect to issue sketch."),
                    MeetingMinutesEntity(projectId = workplace, title = "Interior coordination meeting 07", meetingAt = at("2026-09-09T06:30:00Z"), location = "Online / site", attendees = "Client FM, architect, MEP consultant, contractor", discussion = "Ceiling zones and phased handover.", decisions = "Use accessible service corridor above collaboration zone.", actionItems = "MEP consultant to issue coordinated mark-up.")
                ).forEach { database.meetingMinutesDao().insert(it) }

                listOf(
                    SiteInspectionEntity(projectId = mainProject, inspectionAt = at("2026-09-12T04:45:00Z"), location = "First-floor south bedroom", inspector = "Vikram Desai", observation = "Window sill is 18 mm above drawing level.", severity = "ATTENTION", correctiveAction = "Reset sill course before casting lintel.", dueAt = at("2026-09-14T00:00:00Z"), status = "OPEN"),
                    SiteInspectionEntity(projectId = mainProject, inspectionAt = at("2026-09-05T04:45:00Z"), location = "Ground-floor toilet", inspector = "Meera Nair", observation = "Waterproofing upturn and pipe collars checked.", severity = "NORMAL", correctiveAction = "None.", status = "CLOSED"),
                    SiteInspectionEntity(projectId = workplace, inspectionAt = at("2026-09-10T06:00:00Z"), location = "Level 5 open office", inspector = "Ananya Rao", observation = "Cable tray conflicts with ceiling suspension grid.", severity = "CRITICAL", correctiveAction = "MEP contractor to reroute before ceiling closure.", status = "OPEN")
                ).forEach { database.siteInspectionDao().insert(it) }

                listOf(
                    DailySiteReportEntity(projectId = mainProject, reportDate = at("2026-09-13T00:00:00Z"), weather = "Overcast; light afternoon rain", manpower = "Masons 8, helpers 10, carpenters 4", workCompleted = "First-floor internal blockwork and staircase waist-slab shuttering.", materialsReceived = "AAC blocks 420 nos; reinforcement steel 1.2 t", delaysOrConstraints = "Rain interrupted terrace work for 75 minutes.", safetyObservations = "Edge protection checked; one access route cleared.", nextDayPlan = "Complete bedroom walls and inspect stair reinforcement.", preparedBy = "Vikram Desai"),
                    DailySiteReportEntity(projectId = mainProject, reportDate = at("2026-09-12T00:00:00Z"), weather = "Dry", manpower = "Masons 7, helpers 9", workCompleted = "Completed north bedroom blockwork to lintel level.", materialsReceived = "Cement 60 bags", nextDayPlan = "South bedroom and stair shuttering.", preparedBy = "Vikram Desai"),
                    DailySiteReportEntity(projectId = workplace, reportDate = at("2026-09-13T00:00:00Z"), weather = "Indoor work", manpower = "Ceiling 12, electrical 8, HVAC 6", workCompleted = "Ceiling grid and lighting conduits in north work zone.", materialsReceived = "Ceiling grid 480 m; luminaires 36 nos", delaysOrConstraints = "Awaiting revised sprinkler coordination.", safetyObservations = "Night-shift permit verified.", nextDayPlan = "Close coordinated grid in meeting rooms.", preparedBy = "Meera Nair")
                ).forEach { database.siteControlDao().upsertDailyReport(it) }

                listOf(
                    ProjectDecisionEntity(projectId = mainProject, referenceNumber = "DEC-026-014", title = "Terrace pergola finish", context = "Mock-up reviewed against maintenance requirements.", decisionRequired = "Choose between exposed concrete and powder-coated steel.", impact = "Fabrication release delayed if open.", finalDecision = "Powder-coated steel in charcoal grey.", requestedFrom = "Clients", owner = "Vikram Desai", dueAt = at("2026-09-08T00:00:00Z"), status = "DECIDED", decidedAt = at("2026-09-07T10:00:00Z")),
                    ProjectDecisionEntity(projectId = mainProject, referenceNumber = "DEC-026-015", title = "Kitchen counter stone", context = "Two samples reviewed under site lighting.", decisionRequired = "Confirm honed black granite or engineered quartz.", impact = "Joinery shop drawings cannot be frozen.", requestedFrom = "Clients", owner = "Vikram Desai", dueAt = at("2026-09-12T00:00:00Z"), status = "OPEN"),
                    ProjectDecisionEntity(projectId = workplace, referenceNumber = "DEC-014-009", title = "Collaboration-zone acoustic finish", context = "NRC and maintenance comparison issued.", decisionRequired = "Approve ceiling baffle colour and spacing.", impact = "Procurement lead time is four weeks.", requestedFrom = "Client facilities", owner = "Ananya Rao", dueAt = at("2026-09-18T00:00:00Z"), status = "OPEN")
                ).forEach { database.siteControlDao().insertDecision(it) }

                val issueOpen = database.siteControlDao().insertIssue(SiteIssueEntity(projectId = mainProject, referenceNumber = "SNAG-026-021", type = "SNAG", title = "Honeycombing at stair landing edge", location = "First-floor stair landing", description = "Localized voids visible after de-shuttering.", severity = "ATTENTION", assignedTo = "Pragati Civil Works", correctiveAction = "Submit repair method and complete approved polymer repair.", dueAt = at("2026-09-15T00:00:00Z"), status = "IN_PROGRESS"))
                val issueReady = database.siteControlDao().insertIssue(SiteIssueEntity(projectId = workplace, referenceNumber = "NCR-014-004", type = "NCR", title = "Unapproved ceiling suspension spacing", location = "Level 5 north zone", description = "Installed spacing differs from approved shop drawing.", severity = "CRITICAL", assignedTo = "Astra Interiors", correctiveAction = "Rework suspension grid and request verification.", dueAt = at("2026-09-13T00:00:00Z"), status = "READY_FOR_VERIFICATION"))
                val issueClosed = database.siteControlDao().insertIssue(SiteIssueEntity(projectId = mainProject, referenceNumber = "SNAG-026-018", type = "SNAG", title = "Incomplete toilet pipe collar", location = "Ground-floor powder room", description = "Waterproofing collar incomplete around waste pipe.", severity = "NORMAL", assignedTo = "Flowline Plumbing", correctiveAction = "Complete collar and flood test.", status = "CLOSED", verificationNote = "24-hour flood test passed; collar photographed.", closedAt = at("2026-09-06T09:00:00Z")))
                listOf(
                    SiteIssueEventEntity(siteIssueId = issueOpen, fromStatus = "", toStatus = "OPEN", note = "Issue recorded", actor = "Vikram Desai", occurredAt = at("2026-09-12T05:10:00Z")),
                    SiteIssueEventEntity(siteIssueId = issueOpen, fromStatus = "OPEN", toStatus = "IN_PROGRESS", note = "Repair method accepted and work assigned.", actor = "Vikram Desai", occurredAt = at("2026-09-12T10:00:00Z")),
                    SiteIssueEventEntity(siteIssueId = issueReady, fromStatus = "", toStatus = "OPEN", note = "NCR recorded", actor = "Ananya Rao", occurredAt = at("2026-09-10T07:00:00Z")),
                    SiteIssueEventEntity(siteIssueId = issueReady, fromStatus = "OPEN", toStatus = "IN_PROGRESS", note = "Rework started", actor = "Ananya Rao", occurredAt = at("2026-09-11T07:00:00Z")),
                    SiteIssueEventEntity(siteIssueId = issueReady, fromStatus = "IN_PROGRESS", toStatus = "READY_FOR_VERIFICATION", note = "Contractor reports rework complete", actor = "Meera Nair", occurredAt = at("2026-09-13T05:00:00Z")),
                    SiteIssueEventEntity(siteIssueId = issueClosed, fromStatus = "READY_FOR_VERIFICATION", toStatus = "CLOSED", note = "24-hour flood test passed; collar photographed.", actor = "Meera Nair", occurredAt = at("2026-09-06T09:00:00Z"))
                ).forEach { database.siteControlDao().insertIssueEvent(it) }

                listOf(
                    ProjectConsultantEntity(projectId = mainProject, name = "Arjun Kulkarni", organisation = "Gridline Structures", discipline = "Structural", email = "arjun@gridline.example", phone = "+91 90000 20001", phase = "Execution", responsibility = "Structural design, GFC details and reinforcement inspections", raciRole = "RESPONSIBLE"),
                    ProjectConsultantEntity(projectId = mainProject, name = "Sara Joseph", organisation = "Flow Matrix Consultants", discipline = "Plumbing", email = "sara@flowmatrix.example", phone = "+91 90000 20002", phase = "Execution", responsibility = "Water, drainage and rainwater coordination", raciRole = "CONSULTED"),
                    ProjectConsultantEntity(projectId = workplace, name = "Ritesh Bhat", organisation = "Axis MEP", discipline = "MEP", email = "ritesh@axis-mep.example", phone = "+91 90000 20003", phase = "Fit-out", responsibility = "Coordinated services and site review", raciRole = "RESPONSIBLE"),
                    ProjectConsultantEntity(projectId = school, name = "Nandita Sen", organisation = "Access First", discipline = "Accessibility", email = "nandita@accessfirst.example", phone = "+91 90000 20004", phase = "Design", responsibility = "Universal-access review", raciRole = "ACCOUNTABLE")
                ).forEach { database.coordinationDao().insertConsultant(it) }

                suspend fun coordination(item: CoordinationItemEntity, events: List<Pair<String, String>>) {
                    val id = database.coordinationDao().insertItem(item)
                    events.forEachIndexed { index, (from, to) -> database.coordinationDao().insertEvent(CoordinationEventEntity(coordinationItemId = id, fromStatus = from, toStatus = to, note = if (from.isBlank()) "Record created" else "Workflow update for demonstration", actor = "Vikram Desai", occurredAt = item.createdAt + index * 3_600_000L)) }
                }
                coordination(CoordinationItemEntity(projectId = mainProject, type = "RFI", referenceNumber = "RFI-026-011", subject = "Terrace rainwater outlet sleeve", discipline = "Plumbing", location = "Terrace grid C4", raisedBy = "Pragati Civil Works", assignedTo = "Flow Matrix Consultants", questionOrRequirement = "Confirm sleeve diameter and invert before slab pour.", response = "Use 110 mm sleeve at marked invert.", status = "ANSWERED", priority = "HIGH", dueAt = at("2026-09-13T00:00:00Z"), createdAt = at("2026-09-10T04:00:00Z")), listOf("" to "DRAFT", "DRAFT" to "OPEN", "OPEN" to "ANSWERED"))
                coordination(CoordinationItemEntity(projectId = workplace, type = "SUBMITTAL", referenceNumber = "SUB-014-023", subject = "Acoustic ceiling tile", discipline = "Interiors", location = "Level 5", raisedBy = "Astra Interiors", assignedTo = "Atelier Nila", questionOrRequirement = "Review technical data and physical sample.", status = "UNDER_REVIEW", priority = "NORMAL", dueAt = at("2026-09-16T00:00:00Z"), createdAt = at("2026-09-11T04:00:00Z")), listOf("" to "RECEIVED", "RECEIVED" to "UNDER_REVIEW"))
                coordination(CoordinationItemEntity(projectId = mainProject, type = "SITE_INSTRUCTION", referenceNumber = "SI-026-006", subject = "Protect retained mango tree", discipline = "Architectural", location = "Front setback", raisedBy = "Atelier Nila", assignedTo = "Pragati Civil Works", questionOrRequirement = "Install protective barricade and prohibit material storage inside drip line.", status = "ACKNOWLEDGED", priority = "HIGH", createdAt = at("2026-09-08T04:00:00Z")), listOf("" to "DRAFT", "DRAFT" to "ISSUED", "ISSUED" to "ACKNOWLEDGED"))
                coordination(CoordinationItemEntity(projectId = school, type = "RFI", referenceNumber = "RFI-008-004", subject = "Ramp landing geometry", discipline = "Accessibility", location = "Main entrance", raisedBy = "Architect", assignedTo = "Access First", questionOrRequirement = "Confirm intermediate landing requirement.", response = "Provide landing as marked on A-102 R1.", status = "CLOSED", priority = "NORMAL", closedAt = at("2026-08-20T08:00:00Z"), createdAt = at("2026-08-17T04:00:00Z")), listOf("" to "DRAFT", "DRAFT" to "OPEN", "OPEN" to "ANSWERED", "ANSWERED" to "CLOSED"))

                val drawingId = database.drawingDao().insertDrawing(ProjectDrawingEntity(projectId = mainProject, drawingNumber = "A-204", title = "First-floor plan", discipline = "Architectural", status = "ISSUED", createdAt = at("2026-06-01T00:00:00Z"), updatedAt = at("2026-09-09T00:00:00Z")))
                val revision1 = database.drawingDao().insertRevision(DrawingRevisionEntity(projectId = mainProject, drawingId = drawingId, revisionCode = "R1", fileName = "AN-RES-026-A-204-R1.dwg", fileUri = "content://com.archiman.demo/drawings/A-204-R1.dwg", fileChecksum = "demo-checksum-r1", issueStatus = "SUPERSEDED", revisionNotes = "Initial GFC issue.", issuedAt = at("2026-07-01T00:00:00Z"), createdAt = at("2026-07-01T00:00:00Z")))
                val revision2 = database.drawingDao().insertRevision(DrawingRevisionEntity(projectId = mainProject, drawingId = drawingId, revisionCode = "R2", fileName = "AN-RES-026-A-204-R2.dwg", fileUri = "content://com.archiman.demo/drawings/A-204-R2.dwg", fileChecksum = "demo-checksum-r2", issueStatus = "GFC", revisionNotes = "Window and stair coordination updated.", issuedAt = at("2026-09-09T00:00:00Z"), createdAt = at("2026-09-09T00:00:00Z")))
                val transmittal = database.drawingDao().insertTransmittal(DrawingTransmittalEntity(projectId = mainProject, transmittalNumber = "TR-026-012", subject = "First-floor coordinated GFC drawings", recipients = "Client; Pragati Civil Works; Gridline Structures", purpose = "FOR_CONSTRUCTION", notes = "R2 supersedes all earlier first-floor plans.", issuedAt = at("2026-09-09T06:00:00Z"), acknowledgedAt = at("2026-09-09T10:00:00Z")))
                database.drawingDao().insertTransmittalItems(listOf(DrawingTransmittalItemEntity(transmittalId = transmittal, drawingRevisionId = revision2)))
                database.drawingDao().insertMarkup(DrawingMarkupEntity(drawingRevisionId = revision2, markupType = "MEASUREMENT", geometryJson = "{\"type\":\"line\",\"points\":[[0.15,0.22],[0.68,0.22]]}", styleJson = "{\"color\":\"#0F62FE\",\"width\":2}", measurementValue = 3.6, measurementUnit = "m", calibrationJson = "{\"drawingUnits\":1000,\"realUnits\":1}", authorId = architectId, createdAt = at("2026-09-10T06:00:00Z")))
                database.drawingDao().insertDrawing(ProjectDrawingEntity(projectId = workplace, drawingNumber = "ID-RCP-501", title = "Reflected ceiling plan · Level 5", discipline = "Interiors", status = "WORKING"))
                database.drawingDao().insertDrawing(ProjectDrawingEntity(projectId = school, drawingNumber = "A-102", title = "Ground-floor plan", discipline = "Architectural", status = "ISSUED"))

                val contractorSpecs = listOf(
                    "Pragati Civil Works" to "Civil", "VoltEdge Systems" to "Electrical", "Flowline Plumbing" to "Plumbing", "CraftGrid Joinery" to "Carpenter", "Hue & Lime Finishes" to "Painter", "StoneWeave Surfaces" to "Flooring and cladding", "AeroComfort Services" to "HVAC", "Astra Interiors" to "General"
                )
                val contractorIds = contractorSpecs.mapIndexed { index, (name, type) -> database.contractorDao().insertContractor(ContractorEntity(name = name, contractorType = type, address = "Demo contractor address ${index + 1}, Bengaluru", contactNo = "+91 91000 3000${index + 1}", phone = "+91 91000 3000${index + 1}")) }
                listOf(mainProject, workplace, school).forEachIndexed { projectIndex, projectId ->
                    contractorIds.filterIndexed { contractorIndex, _ -> contractorIndex % 3 == projectIndex % 3 || contractorIndex == 0 }.forEach { contractorId -> database.contractorDao().insertProjectContractorRef(ProjectContractorCrossRef(projectId = projectId, contractorId = contractorId)) }
                }

                val items = database.itemMasterDao().getAllItems().first()
                contractorIds.forEachIndexed { index, contractorId ->
                    items.filter { item -> when (index) { 0 -> item.workType in setOf("Masonry", "Stone Masonry", "Anti-termite Treatment"); 1 -> item.name.contains("Electrical", true); 2 -> item.name.contains("Water", true); 3 -> item.name.contains("Door", true); 4 -> item.workType == "Finishes"; 5 -> item.workType.contains("Flooring", true) || item.name.contains("Flooring", true); else -> item.calculationType == CalculationType.NOS } }.take(4).ifEmpty { listOf(items[index % items.size]) }.forEach { item -> database.contractorDao().insertQualifiedItem(ContractorQualifiedItemEntity(contractorId = contractorId, workType = item.workType, itemName = item.name, uom = item.unit, calculationType = item.calculationType)) }
                }
                val aliasItem = items.first { it.name.contains("AAC Block Masonry 100", true) }
                database.workItemAliasDao().insert(WorkItemAliasEntity(workItemId = aliasItem.id, alias = "100 mm AAC partition"))

                val ground = database.floorDao().insertFloor(FloorEntity(projectId = mainProject, name = "Ground Floor", orderIndex = 0))
                val first = database.floorDao().insertFloor(FloorEntity(projectId = mainProject, name = "First Floor", orderIndex = 1))
                database.floorDao().insertFloor(FloorEntity(projectId = mainProject, name = "Terrace", orderIndex = 2))
                val living = database.roomDao().insertRoom(RoomEntity(projectId = mainProject, floorId = ground, name = "Living / Courtyard", orderIndex = 1))
                val bedroom = database.roomDao().insertRoom(RoomEntity(projectId = mainProject, floorId = first, name = "South Bedroom", orderIndex = 1))
                projectIds.drop(1).forEach { projectId -> val floor = database.floorDao().insertFloor(FloorEntity(projectId = projectId, name = "Primary Level", orderIndex = 0)); database.roomDao().insertRoom(RoomEntity(projectId = projectId, floorId = floor, name = "Main Space", orderIndex = 1)) }
                val wall = database.componentDao().insertComponent(ComponentEntity(projectId = mainProject, floorId = first, roomId = bedroom, name = "South external wall", type = ComponentType.WALL, length = 4.8, height = 3.0, thickness = 0.2))
                val floorComponent = database.componentDao().insertComponent(ComponentEntity(projectId = mainProject, floorId = ground, roomId = living, name = "Living floor finish", type = ComponentType.FLOOR, length = 6.2, width = 4.5))

                val measurementItems = CalculationType.values().associateWith { type -> items.first { it.calculationType == type } }
                val civilId = contractorIds[0]
                measurementItems.values.forEach { item -> database.contractorDao().insertQualifiedItem(ContractorQualifiedItemEntity(contractorId = civilId, workType = item.workType, itemName = item.name, uom = item.unit, calculationType = item.calculationType)) }
                val wallWorkItem = database.componentWorkItemDao().insertWorkItem(ComponentWorkItemEntity(componentId = wall, itemId = measurementItems.getValue(CalculationType.WALL_PLASTER).id, itemName = measurementItems.getValue(CalculationType.WALL_PLASTER).name, unit = measurementItems.getValue(CalculationType.WALL_PLASTER).unit, calculationType = CalculationType.WALL_PLASTER))
                val floorWorkItem = database.componentWorkItemDao().insertWorkItem(ComponentWorkItemEntity(componentId = floorComponent, itemId = measurementItems.getValue(CalculationType.AREA).id, itemName = measurementItems.getValue(CalculationType.AREA).name, unit = measurementItems.getValue(CalculationType.AREA).unit, calculationType = CalculationType.AREA))

                val statuses = listOf("DRAFT", "SUBMITTED", "RETURNED", "CHECKED", "APPROVED")
                measurementItems.entries.forEachIndexed { index, (type, item) ->
                    val status = statuses[index]
                    val floorId = if (index % 2 == 0) ground else first
                    val floorName = if (floorId == ground) "Ground Floor" else "First Floor"
                    val sheetId = database.measurementSheetDao().insert(MeasurementSheetEntity(sheetCode = "MB-AN026-${(index + 1).toString().padStart(3, '0')}", projectId = mainProject, floorId = floorId, floorNameSnapshot = floorName, contractorId = civilId, contractorNameSnapshot = "Pragati Civil Works", itemId = item.id, itemNameSnapshot = item.name, uomSnapshot = item.unit, formulaCode = type.name, status = status, revision = if (status == "RETURNED") 2 else 1, lockedAt = if (status == "APPROVED") at("2026-09-12T11:00:00Z") else null, createdAt = at("2026-09-01T04:00:00Z") + index * 86_400_000L, updatedAt = at("2026-09-12T11:00:00Z")))
                    val dimensions = when (type) {
                        CalculationType.RUNNING_LENGTH -> doubleArrayOf(5.4, 0.0, 0.0, 3.0, 0.2, 16.0)
                        CalculationType.AREA -> doubleArrayOf(6.2, 4.5, 0.0, 1.0, 1.1, 26.8)
                        CalculationType.WALL_PLASTER -> doubleArrayOf(4.8, 0.0, 3.0, 2.0, 3.78, 25.02)
                        CalculationType.VOLUME -> doubleArrayOf(4.2, 0.23, 3.0, 2.0, 0.42, 5.376)
                        CalculationType.NOS -> doubleArrayOf(0.0, 0.0, 0.0, 12.0, 1.0, 11.0)
                    }
                    database.measurementDao().insertMeasurement(MeasurementEntity(sheetId = sheetId, projectId = mainProject, floorId = floorId, roomId = if (floorId == ground) living else bedroom, componentId = when (type) { CalculationType.WALL_PLASTER -> wall; CalculationType.AREA -> floorComponent; else -> null }, componentWorkItemId = when (type) { CalculationType.WALL_PLASTER -> wallWorkItem; CalculationType.AREA -> floorWorkItem; else -> null }, contractorId = civilId, contractorName = "Pragati Civil Works", itemId = item.id, itemName = item.name, unit = item.unit, calculationType = type, description = "Demo ${type.displayName.lowercase()} row", length = dimensions[0], width = dimensions[1], height = dimensions[2], nos = dimensions[3], deduction = dimensions[4], quantity = dimensions[5], floor = floorName, location = if (floorId == ground) "Living / Courtyard" else "South Bedroom", remarks = "Checked against current drawing revision.", photoUri = if (index == 0) Uri.fromFile(measurementPhoto).toString() else null, date = at("2026-09-12T06:00:00Z") + index * 3_600_000L))
                    database.measurementReviewEventDao().insert(MeasurementReviewEventEntity(sheetId = sheetId, fromStatus = "", toStatus = "DRAFT", comment = "Measurement sheet created", actor = "Vikram Desai", revision = 1, createdAt = at("2026-09-01T04:00:00Z") + index * 86_400_000L))
                    if (status != "DRAFT") database.measurementReviewEventDao().insert(MeasurementReviewEventEntity(sheetId = sheetId, fromStatus = "DRAFT", toStatus = status, comment = "Demonstration workflow state", actor = "Ananya Rao", revision = if (status == "RETURNED") 2 else 1, createdAt = at("2026-09-12T10:00:00Z") + index * 60_000L))
                }

                listOf(
                    PortalAuditEventEntity(userId = adminId, username = "admin", action = "UPDATE_COMPANY", entityType = "COMPANY_PROFILE", entityId = 1, summary = "Updated demonstration practice profile", sourceAddress = "192.168.1.20", occurredAt = at("2026-09-12T08:00:00Z")),
                    PortalAuditEventEntity(userId = architectId, username = "architect", action = "ADD_TASK", entityType = "TASK", entityId = 1, projectId = mainProject, summary = "Added sill-level verification task", sourceAddress = "192.168.1.21", occurredAt = at("2026-09-12T08:30:00Z"))
                ).forEach { database.portalAccessDao().insertAuditEvent(it) }
            }

            assertDatabaseCoverage(database)
            val output = System.getenv("ARCHIMAN_DEMO_OUTPUT")?.let(::File)
                ?: File(context.cacheDir, "ArchiMan-Demo-Company.archimandb")
            output.parentFile?.mkdirs()
            output.delete()
            val password = (System.getenv("ARCHIMAN_DEMO_PASSWORD") ?: "ArchiMan-Demo-2026!").toCharArray()
            val checksum = CompanyDatabasePackageManager(context, database).exportTo(Uri.fromFile(output), password)
            assertTrue(output.isFile && output.length() > 1_000)
            val preview = CompanyDatabasePackageManager(context, database).previewImport(Uri.fromFile(output), (System.getenv("ARCHIMAN_DEMO_PASSWORD") ?: "ArchiMan-Demo-2026!").toCharArray())
            assertEquals("Atelier Nila Architects · Demo", preview.practiceName)
            assertEquals(DATABASE_SCHEMA_VERSION, preview.schemaVersion)
            assertEquals(4, preview.projectCount)
            assertEquals(5, preview.measurementCount)
            assertEquals(2, preview.attachmentCount)
            assertEquals(checksum, preview.databaseChecksum)
            CompanyDatabasePackageManager(context, database).discardPreview(preview)
            if (System.getenv("ARCHIMAN_DEMO_OUTPUT") == null) output.delete()
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
            CompanyDatabasePackageManager.MANAGED_DIRECTORIES.forEach { File(context.filesDir, it).deleteRecursively() }
        }
    }

    private fun managedPng(directory: String, name: String): File {
        val file = File(context.filesDir, "$directory/$name")
        file.parentFile?.mkdirs()
        file.writeBytes(Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9ZpN8AAAAASUVORK5CYII="))
        return file
    }

    private fun assertDatabaseCoverage(database: AppDatabase) {
        val sqlite = database.openHelper.writableDatabase
        val expectedTables = listOf(
            "company_profile", "clients", "projects", "project_consultancy_profiles", "project_scope_items",
            "project_onboarding_responses", "project_approvals", "project_backlog", "local_users", "portal_audit_events",
            "floors", "rooms", "components", "component_work_items", "contractors", "contractor_qualified_items",
            "project_contractor_refs", "item_master", "work_item_aliases", "project_tasks", "project_selection_items",
            "project_schedules", "meeting_minutes", "site_inspections", "daily_site_reports", "project_decisions",
            "site_issues", "site_issue_events", "project_consultants", "coordination_items", "coordination_events",
            "project_drawings", "drawing_revisions", "drawing_transmittals", "drawing_transmittal_items", "drawing_markups",
            "measurement_sheets", "measurement_review_events", "measurements"
        )
        expectedTables.forEach { table ->
            sqlite.query("SELECT COUNT(*) FROM `$table`").use { cursor -> cursor.moveToFirst(); assertTrue("Expected demo rows in $table", cursor.getInt(0) > 0) }
        }
        sqlite.query("PRAGMA integrity_check").use { cursor -> cursor.moveToFirst(); assertEquals("ok", cursor.getString(0)) }
        sqlite.query("PRAGMA foreign_key_check").use { cursor -> assertEquals(0, cursor.count) }
        sqlite.query("PRAGMA user_version").use { cursor -> cursor.moveToFirst(); assertEquals(DATABASE_SCHEMA_VERSION, cursor.getInt(0)) }
    }
}
