package com.example.data.repository

import androidx.room.withTransaction
import com.example.domain.MeasurementSheetStatus
import com.example.domain.MeasurementSheetWorkflow
import com.example.domain.CoordinationWorkflow
import com.example.domain.SiteIssueWorkflow
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import com.example.portal.PortalPasswordHasher
import com.example.portal.PortalPrincipal

class SiteRepository(private val database: AppDatabase) {
    val allProjects: Flow<List<ProjectEntity>> = database.projectDao().getAllProjects()
    val allClients: Flow<List<ClientEntity>> = database.clientDao().getAllClients()
    val allContractors: Flow<List<ContractorEntity>> = database.contractorDao().getAllContractors()
    val allQualifiedItems: Flow<List<ContractorQualifiedItemEntity>> = database.contractorDao().getAllQualifiedItems()
    val allItems: Flow<List<ItemMasterEntity>> = database.itemMasterDao().getAllItems()
    val allMeasurements: Flow<List<MeasurementEntity>> = database.measurementDao().getAllMeasurements()
    val companyProfile: Flow<CompanyProfileEntity?> = database.companyProfileDao().observe()
    val localUsers: Flow<List<LocalUserEntity>> = database.portalAccessDao().observeUsers()
    val portalAuditEvents: Flow<List<PortalAuditEventEntity>> = database.portalAccessDao().observeAuditEvents()

    // Company-wide streams (every project, not one) — feed the monitoring dashboard.
    val allProjectTasks: Flow<List<ProjectTaskEntity>> = database.projectTaskDao().getAllTasks()
    val allProjectSchedules: Flow<List<ProjectScheduleEntity>> = database.projectScheduleDao().getAllSchedules()
    val allDailyReports: Flow<List<DailySiteReportEntity>> = database.siteControlDao().observeAllDailyReports()
    val allDecisions: Flow<List<ProjectDecisionEntity>> = database.siteControlDao().observeAllDecisions()
    val allSiteIssues: Flow<List<SiteIssueEntity>> = database.siteControlDao().observeAllIssues()
    val allCoordinationItems: Flow<List<CoordinationItemEntity>> = database.coordinationDao().getAllItems()
    val allDrawings: Flow<List<ProjectDrawingEntity>> = database.drawingDao().getAllDrawings()
    val allDrawingRevisions: Flow<List<DrawingRevisionEntity>> = database.drawingDao().getAllRevisions()

    suspend fun upsertCompanyProfile(profile: CompanyProfileEntity) = database.companyProfileDao().upsert(profile)

    suspend fun createLocalUser(username: String, displayName: String, password: CharArray, role: String): Long {
        val normalized = username.trim().lowercase()
        require(normalized.matches(Regex("[a-z0-9._-]{3,40}"))) { "Username must be 3–40 characters using letters, numbers, dot, dash or underscore." }
        require(database.portalAccessDao().findUser(normalized) == null) { "That username already exists." }
        require(role in setOf("ADMIN", "EDITOR", "VIEWER")) { "Unknown portal role." }
        val digest = PortalPasswordHasher.hash(password)
        return database.portalAccessDao().insertUser(LocalUserEntity(username = normalized, displayName = displayName.trim().ifBlank { normalized }, passwordHash = digest.hash, passwordSalt = digest.salt, passwordIterations = digest.iterations, role = role))
    }

    suspend fun authenticateLocalUser(username: String, password: CharArray): PortalPrincipal? {
        val user = database.portalAccessDao().findUser(username.trim()) ?: return null
        if (!user.isActive || !PortalPasswordHasher.verify(password, user.passwordHash, user.passwordSalt, user.passwordIterations)) return null
        database.portalAccessDao().updateUser(user.copy(lastLoginAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        return PortalPrincipal(user.id, user.username, user.displayName, user.role)
    }

    suspend fun setLocalUserActive(user: LocalUserEntity, active: Boolean) = database.portalAccessDao().updateUser(user.copy(isActive = active, updatedAt = System.currentTimeMillis()))
    suspend fun getLocalUserById(id: Long) = database.portalAccessDao().getUserById(id)

    suspend fun updateLocalUser(user: LocalUserEntity, displayName: String, role: String, active: Boolean, newPassword: CharArray?) {
        require(role in setOf("ADMIN", "EDITOR", "VIEWER")) { "Unknown portal role." }
        val currentUsers = localUsers.first()
        val wouldRemoveLastAdmin = user.role == "ADMIN" && user.isActive && (role != "ADMIN" || !active) &&
            currentUsers.count { it.role == "ADMIN" && it.isActive } <= 1
        require(!wouldRemoveLastAdmin) { "At least one active administrator is required." }
        val password = newPassword?.takeIf { it.isNotEmpty() }
        val digest = password?.let {
            require(it.size >= 10) { "The new password must contain at least 10 characters." }
            PortalPasswordHasher.hash(it)
        }
        database.portalAccessDao().updateUser(
            user.copy(
                displayName = displayName.trim().ifBlank { user.username },
                role = role,
                isActive = active,
                passwordHash = digest?.hash ?: user.passwordHash,
                passwordSalt = digest?.salt ?: user.passwordSalt,
                passwordIterations = digest?.iterations ?: user.passwordIterations,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
    suspend fun recordPortalAudit(event: PortalAuditEventEntity) = database.portalAccessDao().insertAuditEvent(event)
    fun getProjectConsultancyProfile(projectId: Long) = database.projectConsultancyDao().observeProfile(projectId)
    fun getProjectScopeItems(projectId: Long) = database.projectConsultancyDao().observeScopeItems(projectId)
    suspend fun upsertProjectConsultancyProfile(profile: ProjectConsultancyProfileEntity) = database.projectConsultancyDao().upsertProfile(profile)
    suspend fun insertProjectScopeItem(item: ProjectScopeItemEntity) = database.projectConsultancyDao().insertScopeItem(item)
    suspend fun updateProjectScopeItem(item: ProjectScopeItemEntity) = database.projectConsultancyDao().updateScopeItem(item)
    suspend fun deleteProjectScopeItem(item: ProjectScopeItemEntity) = database.projectConsultancyDao().deleteScopeItem(item)
    fun getProjectOnboardingResponses(projectId: Long) = database.projectConsultancyDao().observeOnboardingResponses(projectId)
    suspend fun upsertProjectOnboardingResponse(item: ProjectOnboardingResponseEntity) = database.projectConsultancyDao().upsertOnboardingResponse(item)
    fun getProjectApprovals(projectId: Long) = database.projectConsultancyDao().observeApprovals(projectId)
    suspend fun insertProjectApproval(item: ProjectApprovalEntity) = database.projectConsultancyDao().insertApproval(item)
    suspend fun updateProjectApproval(item: ProjectApprovalEntity) = database.projectConsultancyDao().updateApproval(item)
    suspend fun deleteProjectApproval(item: ProjectApprovalEntity) = database.projectConsultancyDao().deleteApproval(item)
    fun getProjectBacklog(projectId: Long) = database.projectConsultancyDao().observeBacklog(projectId)
    suspend fun insertProjectBacklogItem(item: ProjectBacklogEntity) = database.projectConsultancyDao().insertBacklogItem(item)
    suspend fun updateProjectBacklogItem(item: ProjectBacklogEntity) = database.projectConsultancyDao().updateBacklogItem(item)
    suspend fun deleteProjectBacklogItem(item: ProjectBacklogEntity) = database.projectConsultancyDao().deleteBacklogItem(item)

    fun getProjectTasks(projectId: Long) = database.projectTaskDao().getByProject(projectId)
    suspend fun insertProjectTask(task: ProjectTaskEntity) = database.projectTaskDao().insert(task)
    suspend fun updateProjectTask(task: ProjectTaskEntity) = database.projectTaskDao().update(task)
    suspend fun deleteProjectTask(task: ProjectTaskEntity) = database.projectTaskDao().delete(task)

    fun getProjectSelectionItems(projectId: Long) = database.projectSelectionItemDao().getByProject(projectId)
    suspend fun insertProjectSelectionItem(item: ProjectSelectionItemEntity) = database.projectSelectionItemDao().insert(item)
    suspend fun updateProjectSelectionItem(item: ProjectSelectionItemEntity) = database.projectSelectionItemDao().update(item)
    suspend fun deleteProjectSelectionItem(item: ProjectSelectionItemEntity) = database.projectSelectionItemDao().delete(item)

    fun getProjectSchedules(projectId: Long) = database.projectScheduleDao().getByProject(projectId)
    suspend fun insertProjectSchedule(item: ProjectScheduleEntity) = database.projectScheduleDao().insert(item)
    suspend fun updateProjectSchedule(item: ProjectScheduleEntity) = database.projectScheduleDao().update(item)
    suspend fun deleteProjectSchedule(item: ProjectScheduleEntity) = database.projectScheduleDao().delete(item)

    fun getMeetingMinutes(projectId: Long) = database.meetingMinutesDao().getByProject(projectId)
    suspend fun insertMeetingMinutes(item: MeetingMinutesEntity) = database.meetingMinutesDao().insert(item)
    suspend fun deleteMeetingMinutes(item: MeetingMinutesEntity) = database.meetingMinutesDao().delete(item)

    fun getSiteInspections(projectId: Long) = database.siteInspectionDao().getByProject(projectId)
    suspend fun insertSiteInspection(item: SiteInspectionEntity) = database.siteInspectionDao().insert(item)
    suspend fun updateSiteInspection(item: SiteInspectionEntity) = database.siteInspectionDao().update(item)
    suspend fun deleteSiteInspection(item: SiteInspectionEntity) = database.siteInspectionDao().delete(item)

    fun getDailySiteReports(projectId: Long) = database.siteControlDao().observeDailyReports(projectId)
    fun getProjectDecisions(projectId: Long) = database.siteControlDao().observeDecisions(projectId)
    fun getSiteIssues(projectId: Long) = database.siteControlDao().observeIssues(projectId)
    fun getSiteIssueEvents(issueId: Long) = database.siteControlDao().observeIssueEvents(issueId)
    suspend fun upsertDailySiteReport(item: DailySiteReportEntity) = database.siteControlDao().upsertDailyReport(item)
    suspend fun deleteDailySiteReport(item: DailySiteReportEntity) = database.siteControlDao().deleteDailyReport(item)
    suspend fun insertProjectDecision(item: ProjectDecisionEntity) = database.siteControlDao().insertDecision(item)
    suspend fun updateProjectDecision(item: ProjectDecisionEntity) = database.siteControlDao().updateDecision(item)

    suspend fun createSiteIssue(item: SiteIssueEntity, actor: String = "Local user"): Long = database.withTransaction {
        require(item.type in setOf("SNAG", "NCR")) { "Site issue type must be SNAG or NCR." }
        val id = database.siteControlDao().insertIssue(item.copy(status = "OPEN"))
        database.siteControlDao().insertIssueEvent(SiteIssueEventEntity(siteIssueId = id, fromStatus = "", toStatus = "OPEN", note = "Issue recorded", actor = actor))
        id
    }

    suspend fun transitionSiteIssue(item: SiteIssueEntity, toStatus: String, note: String, actor: String = "Local user") = database.withTransaction {
        SiteIssueWorkflow.requireTransition(item.status, toStatus, note)
        val now = System.currentTimeMillis()
        database.siteControlDao().updateIssue(item.copy(status = toStatus, verificationNote = if (toStatus == "CLOSED") note.trim() else item.verificationNote, closedAt = if (toStatus == "CLOSED") now else null, updatedAt = now))
        database.siteControlDao().insertIssueEvent(SiteIssueEventEntity(siteIssueId = item.id, fromStatus = item.status, toStatus = toStatus, note = note.trim(), actor = actor))
    }

    fun getProjectConsultants(projectId: Long) = database.coordinationDao().getConsultants(projectId)
    fun getCoordinationItems(projectId: Long) = database.coordinationDao().getItems(projectId)
    suspend fun getCoordinationItemById(id: Long) = database.coordinationDao().getItemById(id)
    fun getCoordinationEvents(itemId: Long) = database.coordinationDao().getEvents(itemId)
    suspend fun insertProjectConsultant(item: ProjectConsultantEntity) = database.coordinationDao().insertConsultant(item)
    suspend fun updateProjectConsultant(item: ProjectConsultantEntity) = database.coordinationDao().updateConsultant(item)

    suspend fun createCoordinationItem(item: CoordinationItemEntity, actor: String = "Local user"): Long =
        database.withTransaction {
            val initial = CoordinationWorkflow.initialStatus(item.type)
            val id = database.coordinationDao().insertItem(item.copy(status = initial))
            database.coordinationDao().insertEvent(
                CoordinationEventEntity(
                    coordinationItemId = id,
                    fromStatus = "",
                    toStatus = initial,
                    note = "Record created",
                    actor = actor
                )
            )
            id
        }

    suspend fun transitionCoordinationItem(item: CoordinationItemEntity, toStatus: String, note: String, actor: String = "Local user") =
        database.withTransaction {
            require(toStatus in CoordinationWorkflow.allowedNextStatuses(item.type, item.status)) {
                "Invalid ${item.type} transition from ${item.status} to $toStatus"
            }
            val now = System.currentTimeMillis()
            database.coordinationDao().updateItem(
                item.copy(
                    status = toStatus,
                    response = note.trim().ifBlank { item.response },
                    updatedAt = now,
                    closedAt = if (CoordinationWorkflow.isClosed(toStatus)) now else null
                )
            )
            database.coordinationDao().insertEvent(
                CoordinationEventEntity(
                    coordinationItemId = item.id,
                    fromStatus = item.status,
                    toStatus = toStatus,
                    note = note.trim(),
                    actor = actor,
                    occurredAt = now
                )
            )
        }

    suspend fun archiveCoordinationItem(item: CoordinationItemEntity, actor: String = "Local user") =
        database.withTransaction {
            val now = System.currentTimeMillis()
            database.coordinationDao().updateItem(item.copy(archivedAt = now, updatedAt = now))
            database.coordinationDao().insertEvent(
                CoordinationEventEntity(
                    coordinationItemId = item.id,
                    fromStatus = item.status,
                    toStatus = "ARCHIVED",
                    note = "Record archived",
                    actor = actor,
                    occurredAt = now
                )
            )
        }

    fun getProjectDrawings(projectId: Long) = database.drawingDao().getDrawings(projectId)
    fun getDrawingRevisions(projectId: Long) = database.drawingDao().getRevisions(projectId)
    fun getDrawingTransmittals(projectId: Long) = database.drawingDao().getTransmittals(projectId)
    suspend fun insertProjectDrawing(item: ProjectDrawingEntity) = database.drawingDao().insertDrawing(item)
    suspend fun updateProjectDrawing(item: ProjectDrawingEntity) = database.drawingDao().updateDrawing(item)
    suspend fun insertDrawingRevision(item: DrawingRevisionEntity) = database.drawingDao().insertRevision(item)
    suspend fun insertDrawingTransmittal(item: DrawingTransmittalEntity) = database.drawingDao().insertTransmittal(item)
    suspend fun insertDrawingTransmittalItems(items: List<DrawingTransmittalItemEntity>) = database.drawingDao().insertTransmittalItems(items)

    suspend fun createDrawingWithRevision(drawing: ProjectDrawingEntity, revision: DrawingRevisionEntity): Long =
        database.withTransaction {
            val drawingId = database.drawingDao().insertDrawing(drawing)
            database.drawingDao().insertRevision(revision.copy(drawingId = drawingId))
            drawingId
        }

    // Client operations
    suspend fun insertClient(client: ClientEntity): Long =
        database.clientDao().insertClient(client)

    suspend fun getClientById(id: Long): ClientEntity? = database.clientDao().getClientById(id)

    suspend fun updateClient(client: ClientEntity) =
        database.clientDao().updateClient(client)

    suspend fun deleteClient(client: ClientEntity) =
        database.clientDao().deleteClient(client)

    // Contractor Qualified Items
    fun getQualifiedItemsForContractor(contractorId: Long): Flow<List<ContractorQualifiedItemEntity>> =
        database.contractorDao().getQualifiedItems(contractorId)

    suspend fun getQualifiedItemsForContractorSync(contractorId: Long): List<ContractorQualifiedItemEntity> =
        database.contractorDao().getQualifiedItemsSync(contractorId)

    suspend fun insertQualifiedItem(item: ContractorQualifiedItemEntity): Long =
        database.contractorDao().insertQualifiedItem(item)

    suspend fun insertQualifiedItems(items: List<ContractorQualifiedItemEntity>) =
        database.contractorDao().insertQualifiedItems(items)

    suspend fun deleteQualifiedItem(item: ContractorQualifiedItemEntity) =
        database.contractorDao().deleteQualifiedItem(item)

    suspend fun deleteQualifiedItemsForContractor(contractorId: Long) =
        database.contractorDao().deleteQualifiedItemsForContractor(contractorId)

    // Project Contractor Cross Refs
    fun getProjectContractorRefs(projectId: Long): Flow<List<ProjectContractorCrossRef>> =
        database.contractorDao().getProjectContractorRefs(projectId)

    suspend fun getProjectContractorRefsSync(projectId: Long): List<ProjectContractorCrossRef> =
        database.contractorDao().getProjectContractorRefsSync(projectId)

    suspend fun insertProjectContractorRef(ref: ProjectContractorCrossRef): Long =
        database.contractorDao().insertProjectContractorRef(ref)

    suspend fun insertProjectContractorRefs(refs: List<ProjectContractorCrossRef>) =
        database.contractorDao().insertProjectContractorRefs(refs)

    suspend fun deleteProjectContractorRef(projectId: Long, contractorId: Long) =
        database.contractorDao().deleteProjectContractorRef(projectId, contractorId)

    suspend fun getBrickworkMeasurementsForFloor(projectId: Long, floorId: Long?, floorName: String): List<MeasurementEntity> =
        database.measurementDao().getBrickworkMeasurementsForFloor(projectId, floorId, floorName)

    // Floor, Room, Component queries
    fun getFloorsByProject(projectId: Long): Flow<List<FloorEntity>> =
        database.floorDao().getFloorsByProject(projectId)

    fun getRoomsByFloor(floorId: Long): Flow<List<RoomEntity>> =
        database.roomDao().getRoomsByFloor(floorId)

    fun getRoomsByProject(projectId: Long): Flow<List<RoomEntity>> =
        database.roomDao().getRoomsByProject(projectId)

    fun getComponentsByRoom(roomId: Long): Flow<List<ComponentEntity>> =
        database.componentDao().getComponentsByRoom(roomId)

    fun getFloorLevelComponents(floorId: Long): Flow<List<ComponentEntity>> =
        database.componentDao().getFloorLevelComponents(floorId)

    fun getComponentsByFloor(floorId: Long): Flow<List<ComponentEntity>> =
        database.componentDao().getComponentsByFloor(floorId)

    fun getWorkItemsForComponent(componentId: Long): Flow<List<ComponentWorkItemEntity>> =
        database.componentWorkItemDao().getWorkItemsForComponent(componentId)

    fun getMeasurementsByComponent(componentId: Long): Flow<List<MeasurementEntity>> =
        database.measurementDao().getMeasurementsByComponent(componentId)

    fun getMeasurementsByComponentAndItem(componentId: Long, itemId: Long): Flow<List<MeasurementEntity>> =
        database.measurementDao().getMeasurementsByComponentAndItem(componentId, itemId)

    fun getMeasurementsByRoom(roomId: Long): Flow<List<MeasurementEntity>> =
        database.measurementDao().getMeasurementsByRoom(roomId)

    fun getMeasurementsByFloor(floorId: Long): Flow<List<MeasurementEntity>> =
        database.measurementDao().getMeasurementsByFloor(floorId)

    fun getContractorsByProject(projectId: Long): Flow<List<ContractorEntity>> =
        database.contractorDao().getContractorsByProject(projectId)

    fun getMeasurementsByProject(projectId: Long): Flow<List<MeasurementEntity>> =
        database.measurementDao().getMeasurementsByProject(projectId)

    fun getMeasurementsByProjectAndContractor(projectId: Long, contractorId: Long): Flow<List<MeasurementEntity>> =
        database.measurementDao().getMeasurementsByProjectAndContractor(projectId, contractorId)

    // Projects & Contractors
    suspend fun insertProject(project: ProjectEntity): Long =
        database.projectDao().insertProject(project)

    suspend fun getProjectById(id: Long): ProjectEntity? = database.projectDao().getProjectById(id)

    suspend fun updateProject(project: ProjectEntity) =
        database.projectDao().updateProject(project)

    suspend fun deleteProject(project: ProjectEntity) =
        database.projectDao().deleteProject(project)

    suspend fun insertContractor(contractor: ContractorEntity): Long =
        database.contractorDao().insertContractor(contractor)

    suspend fun getContractorById(id: Long): ContractorEntity? = database.contractorDao().getContractorById(id)

    suspend fun getItemById(id: Long): ItemMasterEntity? = database.itemMasterDao().getItemById(id)

    suspend fun getFloorById(id: Long): FloorEntity? = database.floorDao().getFloorById(id)

    suspend fun updateContractor(contractor: ContractorEntity) =
        database.contractorDao().updateContractor(contractor)

    suspend fun deleteContractor(contractor: ContractorEntity) =
        database.contractorDao().deleteContractor(contractor)

    // Floor Operations
    suspend fun insertFloor(floor: FloorEntity): Long =
        database.floorDao().insertFloor(floor)

    suspend fun updateFloor(floor: FloorEntity) =
        database.floorDao().updateFloor(floor)

    suspend fun deleteFloor(floor: FloorEntity) =
        database.floorDao().deleteFloor(floor)

    suspend fun duplicateFloor(sourceFloorId: Long, newFloorName: String): Long {
        val sourceFloor = database.floorDao().getFloorById(sourceFloorId) ?: return 0
        val newFloorId = database.floorDao().insertFloor(
            FloorEntity(
                projectId = sourceFloor.projectId,
                name = newFloorName,
                orderIndex = sourceFloor.orderIndex + 1
            )
        )

        val rooms = database.roomDao().getRoomsByFloorSync(sourceFloorId)
        for (room in rooms) {
            val newRoomId = database.roomDao().insertRoom(
                RoomEntity(
                    projectId = sourceFloor.projectId,
                    floorId = newFloorId,
                    name = room.name,
                    orderIndex = room.orderIndex
                )
            )

            val components = database.componentDao().getComponentsByRoomSync(room.id)
            for (comp in components) {
                val newCompId = database.componentDao().insertComponent(
                    ComponentEntity(
                        projectId = sourceFloor.projectId,
                        floorId = newFloorId,
                        roomId = newRoomId,
                        name = comp.name,
                        type = comp.type,
                        length = comp.length,
                        width = comp.width,
                        height = comp.height,
                        thickness = comp.thickness
                    )
                )

                val workItems = database.componentWorkItemDao().getWorkItemsForComponentSync(comp.id)
                if (workItems.isNotEmpty()) {
                    val clonedWorkItems = workItems.map { wi ->
                        ComponentWorkItemEntity(
                            componentId = newCompId,
                            itemId = wi.itemId,
                            itemName = wi.itemName,
                            unit = wi.unit,
                            calculationType = wi.calculationType
                        )
                    }
                    database.componentWorkItemDao().insertWorkItems(clonedWorkItems)
                }
            }
        }
        // Also copy Floor-Level Components (Beams, Slabs, Structural items not tied to rooms)
        val floorComponents = database.componentDao().getFloorLevelComponentsSync(sourceFloorId)
        for (comp in floorComponents) {
            val newCompId = database.componentDao().insertComponent(
                ComponentEntity(
                    projectId = sourceFloor.projectId,
                    floorId = newFloorId,
                    roomId = 0L,
                    name = comp.name,
                    type = comp.type,
                    length = comp.length,
                    width = comp.width,
                    height = comp.height,
                    thickness = comp.thickness
                )
            )

            val workItems = database.componentWorkItemDao().getWorkItemsForComponentSync(comp.id)
            if (workItems.isNotEmpty()) {
                val clonedWorkItems = workItems.map { wi ->
                    ComponentWorkItemEntity(
                        componentId = newCompId,
                        itemId = wi.itemId,
                        itemName = wi.itemName,
                        unit = wi.unit,
                        calculationType = wi.calculationType
                    )
                }
                database.componentWorkItemDao().insertWorkItems(clonedWorkItems)
            }
        }

        return newFloorId
    }

    // Room Operations
    suspend fun insertRoom(room: RoomEntity): Long =
        database.roomDao().insertRoom(room)

    suspend fun updateRoom(room: RoomEntity) =
        database.roomDao().updateRoom(room)

    suspend fun deleteRoom(room: RoomEntity) =
        database.roomDao().deleteRoom(room)

    suspend fun duplicateRoom(sourceRoomId: Long, newRoomName: String): Long {
        val sourceRoom = database.roomDao().getRoomById(sourceRoomId) ?: return 0
        val newRoomId = database.roomDao().insertRoom(
            RoomEntity(
                projectId = sourceRoom.projectId,
                floorId = sourceRoom.floorId,
                name = newRoomName,
                orderIndex = sourceRoom.orderIndex + 1
            )
        )

        val components = database.componentDao().getComponentsByRoomSync(sourceRoomId)
        for (comp in components) {
            val newCompId = database.componentDao().insertComponent(
                ComponentEntity(
                    projectId = sourceRoom.projectId,
                    floorId = sourceRoom.floorId,
                    roomId = newRoomId,
                    name = comp.name,
                    type = comp.type,
                    length = comp.length,
                    width = comp.width,
                    height = comp.height,
                    thickness = comp.thickness
                )
            )

            val workItems = database.componentWorkItemDao().getWorkItemsForComponentSync(comp.id)
            if (workItems.isNotEmpty()) {
                val clonedWorkItems = workItems.map { wi ->
                    ComponentWorkItemEntity(
                        componentId = newCompId,
                        itemId = wi.itemId,
                        itemName = wi.itemName,
                        unit = wi.unit,
                        calculationType = wi.calculationType
                    )
                }
                database.componentWorkItemDao().insertWorkItems(clonedWorkItems)
            }
        }
        return newRoomId
    }

    suspend fun copyRoomStructure(targetRoomId: Long, sourceRoomId: Long) {
        val targetRoom = database.roomDao().getRoomById(targetRoomId) ?: return
        val components = database.componentDao().getComponentsByRoomSync(sourceRoomId)
        for (comp in components) {
            val newCompId = database.componentDao().insertComponent(
                ComponentEntity(
                    projectId = targetRoom.projectId,
                    floorId = targetRoom.floorId,
                    roomId = targetRoomId,
                    name = comp.name,
                    type = comp.type,
                    length = comp.length,
                    width = comp.width,
                    height = comp.height,
                    thickness = comp.thickness
                )
            )

            val workItems = database.componentWorkItemDao().getWorkItemsForComponentSync(comp.id)
            if (workItems.isNotEmpty()) {
                val clonedWorkItems = workItems.map { wi ->
                    ComponentWorkItemEntity(
                        componentId = newCompId,
                        itemId = wi.itemId,
                        itemName = wi.itemName,
                        unit = wi.unit,
                        calculationType = wi.calculationType
                    )
                }
                database.componentWorkItemDao().insertWorkItems(clonedWorkItems)
            }
        }
    }

    // Component Operations
    suspend fun insertComponent(component: ComponentEntity): Long =
        database.componentDao().insertComponent(component)

    suspend fun updateComponent(component: ComponentEntity) =
        database.componentDao().updateComponent(component)

    suspend fun deleteComponent(component: ComponentEntity) {
        database.componentWorkItemDao().deleteByComponentId(component.id)
        database.componentDao().deleteComponent(component)
    }

    suspend fun duplicateComponent(sourceComponentId: Long, newComponentName: String, copyConnectedWork: Boolean = true): Long {
        val sourceComp = database.componentDao().getComponentById(sourceComponentId) ?: return 0
        val newCompId = database.componentDao().insertComponent(
            ComponentEntity(
                projectId = sourceComp.projectId,
                floorId = sourceComp.floorId,
                roomId = sourceComp.roomId,
                name = newComponentName,
                type = sourceComp.type,
                length = sourceComp.length,
                width = sourceComp.width,
                height = sourceComp.height,
                thickness = sourceComp.thickness
            )
        )

        if (copyConnectedWork) {
            val workItems = database.componentWorkItemDao().getWorkItemsForComponentSync(sourceComponentId)
            if (workItems.isNotEmpty()) {
                val cloned = workItems.map { wi ->
                    ComponentWorkItemEntity(
                        componentId = newCompId,
                        itemId = wi.itemId,
                        itemName = wi.itemName,
                        unit = wi.unit,
                        calculationType = wi.calculationType
                    )
                }
                database.componentWorkItemDao().insertWorkItems(cloned)
            }
        }
        return newCompId
    }

    // Connected Work Items
    suspend fun insertConnectedWorkItem(item: ComponentWorkItemEntity): Long =
        database.componentWorkItemDao().insertWorkItem(item)

    suspend fun insertConnectedWorkItems(items: List<ComponentWorkItemEntity>) =
        database.componentWorkItemDao().insertWorkItems(items)

    suspend fun deleteConnectedWorkItem(item: ComponentWorkItemEntity) =
        database.componentWorkItemDao().deleteWorkItem(item)

    // Work items
    suspend fun insertItem(item: ItemMasterEntity): Long =
        database.itemMasterDao().insertItem(item)

    suspend fun updateItem(item: ItemMasterEntity) =
        database.itemMasterDao().updateItem(item)

    suspend fun deleteItem(item: ItemMasterEntity) =
        database.itemMasterDao().archiveItem(item.id)

    fun getAliasesForWorkItem(workItemId: Long): Flow<List<WorkItemAliasEntity>> =
        database.workItemAliasDao().getAliases(workItemId)

    suspend fun addWorkItemAlias(workItemId: Long, alias: String): Long =
        database.workItemAliasDao().insert(WorkItemAliasEntity(workItemId = workItemId, alias = alias.trim()))

    suspend fun mergeWorkItems(sourceId: Long, canonicalId: Long) =
        database.itemMasterDao().mergeIntoCanonical(sourceId, canonicalId)

    // Measurements
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long = database.withTransaction {
        if (measurement.sheetId > 0) database.measurementDao().insertMeasurement(measurement)
        else {
            val sheetId = database.measurementSheetDao().insert(measurement.toSheet())
            database.measurementDao().insertMeasurement(measurement.withSheet(sheetId))
        }
    }

    suspend fun insertMeasurementBatch(measurements: List<MeasurementEntity>): Int = database.withTransaction {
        if (measurements.isEmpty()) return@withTransaction 0
        val first = measurements.first()
        val sheetId = database.measurementSheetDao().insert(first.toSheet())
        database.measurementDao().insertMeasurements(measurements.map { it.withSheet(sheetId) }).size
    }

    fun getMeasurementSheets(): Flow<List<MeasurementSheetEntity>> = database.measurementSheetDao().getAllSheets()

    fun getReviewEvents(sheetId: Long): Flow<List<MeasurementReviewEventEntity>> =
        database.measurementReviewEventDao().getForSheet(sheetId)

    suspend fun transitionMeasurementSheet(sheetId: Long, to: MeasurementSheetStatus, actor: String, comment: String = "") =
        database.withTransaction {
            require(actor.isNotBlank()) { "Actor is required" }
            val sheet = requireNotNull(database.measurementSheetDao().getById(sheetId)) { "Measurement sheet does not exist" }
            require(sheet.archivedAt == null) { "Archived sheets cannot change status" }
            val from = MeasurementSheetStatus.valueOf(sheet.status)
            MeasurementSheetWorkflow.requireTransition(from, to, comment)
            val revision = if (from == MeasurementSheetStatus.RETURNED && to == MeasurementSheetStatus.SUBMITTED) sheet.revision + 1 else sheet.revision
            val now = System.currentTimeMillis()
            database.measurementSheetDao().updateWorkflow(sheetId, to.name, revision, now)
            database.measurementReviewEventDao().insert(
                MeasurementReviewEventEntity(
                    sheetId = sheetId,
                    fromStatus = from.name,
                    toStatus = to.name,
                    comment = comment.trim(),
                    actor = actor.trim(),
                    revision = revision,
                    createdAt = now
                )
            )
            Unit
        }

    suspend fun updateMeasurement(measurement: MeasurementEntity) = database.withTransaction {
        val sheet = requireNotNull(database.measurementSheetDao().getById(measurement.sheetId)) { "Measurement sheet does not exist" }
        require(sheet.status == "DRAFT" || sheet.status == "RETURNED") { "Only draft or returned sheets can be edited" }
        database.measurementDao().updateMeasurement(measurement)
    }

    suspend fun deleteMeasurement(measurement: MeasurementEntity) = archiveMeasurementSheet(measurement.sheetId, "Local User")

    suspend fun deleteMeasurementById(id: Long) {
        val measurement = database.measurementDao().getMeasurementById(id) ?: return
        archiveMeasurementSheet(measurement.sheetId, "Local User")
    }

    suspend fun archiveMeasurementSheet(sheetId: Long, actor: String) = database.withTransaction {
        val sheet = requireNotNull(database.measurementSheetDao().getById(sheetId)) { "Measurement sheet does not exist" }
        if (sheet.archivedAt != null) return@withTransaction
        val now = System.currentTimeMillis()
        database.measurementSheetDao().archive(sheetId, now)
        database.measurementReviewEventDao().insert(
            MeasurementReviewEventEntity(sheetId = sheetId, fromStatus = sheet.status, toStatus = "ARCHIVED", comment = "Archived without deleting measurement rows", actor = actor.ifBlank { "Local User" }, revision = sheet.revision, createdAt = now)
        )
    }

}

private fun MeasurementEntity.toSheet() = MeasurementSheetEntity(
    sheetCode = "MB-${date}-${java.util.UUID.randomUUID().toString().take(8).uppercase()}",
    projectId = projectId,
    floorId = floorId,
    floorNameSnapshot = floor,
    contractorId = contractorId,
    contractorNameSnapshot = contractorName,
    itemId = itemId,
    itemNameSnapshot = itemName,
    uomSnapshot = unit,
    formulaCode = calculationType.name,
    formulaVersion = 1,
    createdAt = date,
    updatedAt = date
)

private fun MeasurementEntity.withSheet(id: Long) = copy(
    sheetId = id,
    formulaCode = calculationType.name,
    formulaVersion = 1
)
