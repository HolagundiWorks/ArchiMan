package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id LIMIT 1")
    suspend fun getClientById(id: Long): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)

    @Query("SELECT COUNT(*) FROM clients")
    suspend fun getCount(): Int
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)
}

@Dao
interface FloorDao {
    @Query("SELECT * FROM floors WHERE projectId = :projectId ORDER BY orderIndex ASC, id ASC")
    fun getFloorsByProject(projectId: Long): Flow<List<FloorEntity>>

    @Query("SELECT * FROM floors WHERE projectId = :projectId ORDER BY orderIndex ASC, id ASC")
    suspend fun getFloorsByProjectSync(projectId: Long): List<FloorEntity>

    @Query("SELECT * FROM floors WHERE id = :id LIMIT 1")
    suspend fun getFloorById(id: Long): FloorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFloor(floor: FloorEntity): Long

    @Update
    suspend fun updateFloor(floor: FloorEntity)

    @Delete
    suspend fun deleteFloor(floor: FloorEntity)

    @Query("DELETE FROM floors WHERE id = :id")
    suspend fun deleteFloorById(id: Long)
}

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms WHERE floorId = :floorId ORDER BY orderIndex ASC, id ASC")
    fun getRoomsByFloor(floorId: Long): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE floorId = :floorId ORDER BY orderIndex ASC, id ASC")
    suspend fun getRoomsByFloorSync(floorId: Long): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY orderIndex ASC, id ASC")
    fun getRoomsByProject(projectId: Long): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE id = :id LIMIT 1")
    suspend fun getRoomById(id: Long): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity): Long

    @Update
    suspend fun updateRoom(room: RoomEntity)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteRoomById(id: Long)
}

@Dao
interface ComponentDao {
    @Query("SELECT * FROM components WHERE roomId = :roomId ORDER BY id ASC")
    fun getComponentsByRoom(roomId: Long): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE roomId = :roomId ORDER BY id ASC")
    suspend fun getComponentsByRoomSync(roomId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE floorId = :floorId AND (roomId = 0 OR roomId IS NULL) ORDER BY id ASC")
    fun getFloorLevelComponents(floorId: Long): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE floorId = :floorId AND (roomId = 0 OR roomId IS NULL) ORDER BY id ASC")
    suspend fun getFloorLevelComponentsSync(floorId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE floorId = :floorId ORDER BY id ASC")
    fun getComponentsByFloor(floorId: Long): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE floorId = :floorId ORDER BY id ASC")
    suspend fun getComponentsByFloorSync(floorId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE id = :id LIMIT 1")
    suspend fun getComponentById(id: Long): ComponentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComponent(component: ComponentEntity): Long

    @Update
    suspend fun updateComponent(component: ComponentEntity)

    @Delete
    suspend fun deleteComponent(component: ComponentEntity)

    @Query("DELETE FROM components WHERE id = :id")
    suspend fun deleteComponentById(id: Long)
}

@Dao
interface ComponentWorkItemDao {
    @Query("SELECT * FROM component_work_items WHERE componentId = :componentId ORDER BY id ASC")
    fun getWorkItemsForComponent(componentId: Long): Flow<List<ComponentWorkItemEntity>>

    @Query("SELECT * FROM component_work_items WHERE componentId = :componentId ORDER BY id ASC")
    suspend fun getWorkItemsForComponentSync(componentId: Long): List<ComponentWorkItemEntity>

    @Query("SELECT * FROM component_work_items WHERE id = :id LIMIT 1")
    suspend fun getWorkItemById(id: Long): ComponentWorkItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkItem(item: ComponentWorkItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkItems(items: List<ComponentWorkItemEntity>)

    @Delete
    suspend fun deleteWorkItem(item: ComponentWorkItemEntity)

    @Query("DELETE FROM component_work_items WHERE componentId = :componentId")
    suspend fun deleteByComponentId(componentId: Long)
}

@Dao
interface ContractorDao {
    @Query("SELECT * FROM contractors ORDER BY name ASC")
    fun getAllContractors(): Flow<List<ContractorEntity>>

    @Query("SELECT * FROM contractors WHERE projectId = :projectId OR projectId = 0 ORDER BY name ASC")
    fun getContractorsByProject(projectId: Long): Flow<List<ContractorEntity>>

    @Query("SELECT * FROM contractors WHERE id = :id LIMIT 1")
    suspend fun getContractorById(id: Long): ContractorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContractor(contractor: ContractorEntity): Long

    @Update
    suspend fun updateContractor(contractor: ContractorEntity)

    @Delete
    suspend fun deleteContractor(contractor: ContractorEntity)

    // Contractor Qualified Items
    @Query("SELECT * FROM contractor_qualified_items WHERE contractorId = :contractorId ORDER BY id ASC")
    fun getQualifiedItems(contractorId: Long): Flow<List<ContractorQualifiedItemEntity>>

    @Query("SELECT * FROM contractor_qualified_items WHERE contractorId = :contractorId ORDER BY id ASC")
    suspend fun getQualifiedItemsSync(contractorId: Long): List<ContractorQualifiedItemEntity>

    @Query("SELECT * FROM contractor_qualified_items ORDER BY id ASC")
    fun getAllQualifiedItems(): Flow<List<ContractorQualifiedItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQualifiedItem(item: ContractorQualifiedItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQualifiedItems(items: List<ContractorQualifiedItemEntity>)

    @Delete
    suspend fun deleteQualifiedItem(item: ContractorQualifiedItemEntity)

    @Query("DELETE FROM contractor_qualified_items WHERE contractorId = :contractorId")
    suspend fun deleteQualifiedItemsForContractor(contractorId: Long)

    // Project Contractor References
    @Query("SELECT * FROM project_contractor_refs WHERE projectId = :projectId")
    fun getProjectContractorRefs(projectId: Long): Flow<List<ProjectContractorCrossRef>>

    @Query("SELECT * FROM project_contractor_refs WHERE projectId = :projectId")
    suspend fun getProjectContractorRefsSync(projectId: Long): List<ProjectContractorCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectContractorRef(ref: ProjectContractorCrossRef): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectContractorRefs(refs: List<ProjectContractorCrossRef>)

    @Query("DELETE FROM project_contractor_refs WHERE projectId = :projectId AND contractorId = :contractorId")
    suspend fun deleteProjectContractorRef(projectId: Long, contractorId: Long)

    @Query("DELETE FROM project_contractor_refs WHERE projectId = :projectId")
    suspend fun deleteAllProjectContractorRefs(projectId: Long)
}

@Dao
interface ItemMasterDao {
    @Query("SELECT * FROM item_master WHERE isActive = 1 ORDER BY workType, name")
    fun getAllItems(): Flow<List<ItemMasterEntity>>

    @Query("SELECT * FROM item_master WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): ItemMasterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemMasterEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<ItemMasterEntity>)

    @Update
    suspend fun updateItem(item: ItemMasterEntity)

    @Delete
    suspend fun deleteItem(item: ItemMasterEntity)

    @Query("UPDATE item_master SET isActive = 0 WHERE id = :id")
    suspend fun archiveItem(id: Long)

    @Query("SELECT COUNT(*) FROM item_master")
    suspend fun getCount(): Int

    @Query("DELETE FROM component_work_items WHERE itemId = :sourceId AND componentId IN (SELECT componentId FROM component_work_items WHERE itemId = :canonicalId)")
    suspend fun removeDuplicateComponentLinks(sourceId: Long, canonicalId: Long)

    @Query("UPDATE component_work_items SET itemId = :canonicalId, itemName = :canonicalName, unit = :unit, calculationType = :calculationType WHERE itemId = :sourceId")
    suspend fun repointComponentLinks(sourceId: Long, canonicalId: Long, canonicalName: String, unit: String, calculationType: CalculationType)

    @Query("UPDATE measurements SET itemId = :canonicalId WHERE itemId = :sourceId")
    suspend fun repointMeasurements(sourceId: Long, canonicalId: Long)

    @Query("DELETE FROM contractor_qualified_items WHERE LOWER(TRIM(itemName)) = LOWER(TRIM(:sourceName)) AND contractorId IN (SELECT contractorId FROM contractor_qualified_items WHERE LOWER(TRIM(itemName)) = LOWER(TRIM(:canonicalName)))")
    suspend fun removeDuplicateQualifications(sourceName: String, canonicalName: String)

    @Query("UPDATE contractor_qualified_items SET workType = :workType, itemName = :canonicalName, uom = :unit, calculationType = :calculationType WHERE LOWER(TRIM(itemName)) = LOWER(TRIM(:sourceName))")
    suspend fun repointQualifications(sourceName: String, canonicalName: String, workType: String, unit: String, calculationType: CalculationType)

    @Query("INSERT OR IGNORE INTO work_item_aliases(workItemId, alias) VALUES(:canonicalId, :alias)")
    suspend fun preserveAlias(canonicalId: Long, alias: String)

    @Query("UPDATE work_item_aliases SET workItemId = :canonicalId WHERE workItemId = :sourceId")
    suspend fun repointAliases(sourceId: Long, canonicalId: Long)

    @Transaction
    suspend fun mergeIntoCanonical(sourceId: Long, canonicalId: Long) {
        require(sourceId != canonicalId) { "Source and canonical work item must differ" }
        val source = requireNotNull(getItemById(sourceId)) { "Source work item does not exist" }
        val canonical = requireNotNull(getItemById(canonicalId)) { "Canonical work item does not exist" }
        require(source.isActive && canonical.isActive) { "Only active work items can be merged" }
        require(source.workType.equals(canonical.workType, ignoreCase = true)) { "Work types must match" }
        require(source.calculationType == canonical.calculationType) { "Calculation formulas must match" }

        removeDuplicateComponentLinks(sourceId, canonicalId)
        repointComponentLinks(sourceId, canonicalId, canonical.name, canonical.unit, canonical.calculationType)
        repointMeasurements(sourceId, canonicalId)
        removeDuplicateQualifications(source.name, canonical.name)
        repointQualifications(source.name, canonical.name, canonical.workType, canonical.unit, canonical.calculationType)
        preserveAlias(canonicalId, source.name)
        repointAliases(sourceId, canonicalId)
        archiveItem(sourceId)
    }
}

@Dao
interface CompanyProfileDao {
    @Query("SELECT * FROM company_profile WHERE id=1 LIMIT 1")
    fun observe(): Flow<CompanyProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: CompanyProfileEntity)
}

@Dao
interface ProjectConsultancyDao {
    @Query("SELECT * FROM project_consultancy_profiles WHERE projectId=:projectId LIMIT 1")
    fun observeProfile(projectId: Long): Flow<ProjectConsultancyProfileEntity?>

    @Query("SELECT * FROM project_scope_items WHERE projectId=:projectId ORDER BY category, orderIndex, createdAt")
    fun observeScopeItems(projectId: Long): Flow<List<ProjectScopeItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProjectConsultancyProfileEntity): Long

    @Insert
    suspend fun insertScopeItem(item: ProjectScopeItemEntity): Long

    @Update
    suspend fun updateScopeItem(item: ProjectScopeItemEntity)

    @Delete
    suspend fun deleteScopeItem(item: ProjectScopeItemEntity)

    @Query("SELECT * FROM project_onboarding_responses WHERE projectId=:projectId ORDER BY questionCode")
    fun observeOnboardingResponses(projectId: Long): Flow<List<ProjectOnboardingResponseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOnboardingResponse(item: ProjectOnboardingResponseEntity): Long

    @Query("SELECT * FROM project_approvals WHERE projectId=:projectId ORDER BY status, createdAt DESC")
    fun observeApprovals(projectId: Long): Flow<List<ProjectApprovalEntity>>

    @Insert suspend fun insertApproval(item: ProjectApprovalEntity): Long
    @Update suspend fun updateApproval(item: ProjectApprovalEntity)
    @Delete suspend fun deleteApproval(item: ProjectApprovalEntity)

    @Query("SELECT * FROM project_backlog WHERE projectId=:projectId ORDER BY status, priority, createdAt DESC")
    fun observeBacklog(projectId: Long): Flow<List<ProjectBacklogEntity>>

    @Insert suspend fun insertBacklogItem(item: ProjectBacklogEntity): Long
    @Update suspend fun updateBacklogItem(item: ProjectBacklogEntity)
    @Delete suspend fun deleteBacklogItem(item: ProjectBacklogEntity)
}

@Dao
interface PortalAccessDao {
    @Query("SELECT * FROM local_users ORDER BY isActive DESC, displayName, username")
    fun observeUsers(): Flow<List<LocalUserEntity>>

    @Query("SELECT * FROM local_users WHERE lower(username)=lower(:username) LIMIT 1")
    suspend fun findUser(username: String): LocalUserEntity?

    @Query("SELECT * FROM local_users WHERE id=:id LIMIT 1")
    suspend fun getUserById(id: Long): LocalUserEntity?

    @Insert suspend fun insertUser(user: LocalUserEntity): Long
    @Update suspend fun updateUser(user: LocalUserEntity)

    @Query("SELECT * FROM portal_audit_events ORDER BY occurredAt DESC, id DESC LIMIT :limit")
    fun observeAuditEvents(limit: Int = 200): Flow<List<PortalAuditEventEntity>>

    @Insert suspend fun insertAuditEvent(event: PortalAuditEventEntity): Long
}

@Dao
interface ProjectTaskDao {
    @Query("SELECT * FROM project_tasks WHERE projectId=:projectId ORDER BY status, createdAt DESC")
    fun getByProject(projectId: Long): Flow<List<ProjectTaskEntity>>

    /** Company-wide, for the monitoring dashboard — every project's tasks, not just one. */
    @Query("SELECT * FROM project_tasks ORDER BY status, createdAt DESC")
    fun getAllTasks(): Flow<List<ProjectTaskEntity>>

    @Insert suspend fun insert(task: ProjectTaskEntity): Long
    @Update suspend fun update(task: ProjectTaskEntity)
    @Delete suspend fun delete(task: ProjectTaskEntity)
}

@Dao
interface ProjectSelectionItemDao {
    @Query("SELECT * FROM project_selection_items WHERE projectId=:projectId ORDER BY status, createdAt DESC")
    fun getByProject(projectId: Long): Flow<List<ProjectSelectionItemEntity>>
    @Insert suspend fun insert(item: ProjectSelectionItemEntity): Long
    @Update suspend fun update(item: ProjectSelectionItemEntity)
    @Delete suspend fun delete(item: ProjectSelectionItemEntity)
}

@Dao
interface ProjectScheduleDao {
    @Query("SELECT * FROM project_schedules WHERE projectId=:projectId ORDER BY scheduledAt ASC")
    fun getByProject(projectId: Long): Flow<List<ProjectScheduleEntity>>

    /** Company-wide, for the monitoring dashboard. */
    @Query("SELECT * FROM project_schedules ORDER BY scheduledAt ASC")
    fun getAllSchedules(): Flow<List<ProjectScheduleEntity>>

    @Insert suspend fun insert(item: ProjectScheduleEntity): Long
    @Update suspend fun update(item: ProjectScheduleEntity)
    @Delete suspend fun delete(item: ProjectScheduleEntity)
}

@Dao
interface MeetingMinutesDao {
    @Query("SELECT * FROM meeting_minutes WHERE projectId=:projectId ORDER BY meetingAt DESC")
    fun getByProject(projectId: Long): Flow<List<MeetingMinutesEntity>>
    @Insert suspend fun insert(item: MeetingMinutesEntity): Long
    @Delete suspend fun delete(item: MeetingMinutesEntity)
}

@Dao
interface SiteInspectionDao {
    @Query("SELECT * FROM site_inspections WHERE projectId=:projectId ORDER BY inspectionAt DESC")
    fun getByProject(projectId: Long): Flow<List<SiteInspectionEntity>>
    @Insert suspend fun insert(item: SiteInspectionEntity): Long
    @Update suspend fun update(item: SiteInspectionEntity)
    @Delete suspend fun delete(item: SiteInspectionEntity)
}

@Dao
interface SiteControlDao {
    @Query("SELECT * FROM daily_site_reports WHERE projectId=:projectId ORDER BY reportDate DESC")
    fun observeDailyReports(projectId: Long): Flow<List<DailySiteReportEntity>>

    @Query("SELECT * FROM project_decisions WHERE projectId=:projectId ORDER BY CASE status WHEN 'DECIDED' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt, createdAt DESC")
    fun observeDecisions(projectId: Long): Flow<List<ProjectDecisionEntity>>

    @Query("SELECT * FROM site_issues WHERE projectId=:projectId ORDER BY CASE status WHEN 'CLOSED' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt, createdAt DESC")
    fun observeIssues(projectId: Long): Flow<List<SiteIssueEntity>>

    @Query("SELECT * FROM site_issue_events WHERE siteIssueId=:issueId ORDER BY occurredAt, id")
    fun observeIssueEvents(issueId: Long): Flow<List<SiteIssueEventEntity>>

    /** Company-wide, for the monitoring dashboard. */
    @Query("SELECT * FROM daily_site_reports ORDER BY reportDate DESC")
    fun observeAllDailyReports(): Flow<List<DailySiteReportEntity>>

    @Query("SELECT * FROM project_decisions ORDER BY CASE status WHEN 'DECIDED' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt, createdAt DESC")
    fun observeAllDecisions(): Flow<List<ProjectDecisionEntity>>

    @Query("SELECT * FROM site_issues ORDER BY CASE status WHEN 'CLOSED' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt, createdAt DESC")
    fun observeAllIssues(): Flow<List<SiteIssueEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertDailyReport(item: DailySiteReportEntity): Long
    @Delete suspend fun deleteDailyReport(item: DailySiteReportEntity)
    @Insert suspend fun insertDecision(item: ProjectDecisionEntity): Long
    @Update suspend fun updateDecision(item: ProjectDecisionEntity)
    @Insert suspend fun insertIssue(item: SiteIssueEntity): Long
    @Update suspend fun updateIssue(item: SiteIssueEntity)
    @Insert suspend fun insertIssueEvent(item: SiteIssueEventEntity): Long
}

@Dao
interface CoordinationDao {
    @Query("SELECT * FROM project_consultants WHERE projectId=:projectId AND status='ACTIVE' ORDER BY discipline, name")
    fun getConsultants(projectId: Long): Flow<List<ProjectConsultantEntity>>

    @Query("SELECT * FROM coordination_items WHERE projectId=:projectId AND archivedAt IS NULL ORDER BY CASE status WHEN 'CLOSED' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt, createdAt DESC")
    fun getItems(projectId: Long): Flow<List<CoordinationItemEntity>>

    /** Company-wide, for the monitoring dashboard. */
    @Query("SELECT * FROM coordination_items WHERE archivedAt IS NULL ORDER BY CASE status WHEN 'CLOSED' THEN 1 ELSE 0 END, dueAt IS NULL, dueAt, createdAt DESC")
    fun getAllItems(): Flow<List<CoordinationItemEntity>>

    @Query("SELECT * FROM coordination_items WHERE id=:id LIMIT 1")
    suspend fun getItemById(id: Long): CoordinationItemEntity?

    @Query("SELECT * FROM coordination_events WHERE coordinationItemId=:itemId ORDER BY occurredAt, id")
    fun getEvents(itemId: Long): Flow<List<CoordinationEventEntity>>

    @Insert suspend fun insertConsultant(item: ProjectConsultantEntity): Long
    @Update suspend fun updateConsultant(item: ProjectConsultantEntity)
    @Insert suspend fun insertItem(item: CoordinationItemEntity): Long
    @Update suspend fun updateItem(item: CoordinationItemEntity)
    @Insert suspend fun insertEvent(item: CoordinationEventEntity): Long
}

@Dao
interface DrawingDao {
    @Query("SELECT * FROM project_drawings WHERE projectId=:projectId AND archivedAt IS NULL ORDER BY discipline, drawingNumber")
    fun getDrawings(projectId: Long): Flow<List<ProjectDrawingEntity>>

    /** Company-wide, for the monitoring dashboard. */
    @Query("SELECT * FROM project_drawings WHERE archivedAt IS NULL ORDER BY discipline, drawingNumber")
    fun getAllDrawings(): Flow<List<ProjectDrawingEntity>>

    @Query("SELECT * FROM drawing_revisions WHERE projectId=:projectId ORDER BY createdAt DESC, id DESC")
    fun getRevisions(projectId: Long): Flow<List<DrawingRevisionEntity>>

    /** Company-wide, for the monitoring dashboard's revision-risk tile. */
    @Query("SELECT * FROM drawing_revisions ORDER BY createdAt DESC, id DESC")
    fun getAllRevisions(): Flow<List<DrawingRevisionEntity>>

    @Query("SELECT * FROM drawing_transmittals WHERE projectId=:projectId ORDER BY issuedAt DESC, id DESC")
    fun getTransmittals(projectId: Long): Flow<List<DrawingTransmittalEntity>>

    @Query("SELECT * FROM drawing_transmittal_items WHERE transmittalId=:transmittalId ORDER BY id")
    fun getTransmittalItems(transmittalId: Long): Flow<List<DrawingTransmittalItemEntity>>

    @Query("SELECT * FROM drawing_markups WHERE drawingRevisionId=:revisionId AND deletedAt IS NULL ORDER BY createdAt, id")
    fun getMarkups(revisionId: Long): Flow<List<DrawingMarkupEntity>>

    @Insert suspend fun insertDrawing(item: ProjectDrawingEntity): Long
    @Update suspend fun updateDrawing(item: ProjectDrawingEntity)
    @Insert suspend fun insertRevision(item: DrawingRevisionEntity): Long
    @Insert suspend fun insertTransmittal(item: DrawingTransmittalEntity): Long
    @Insert suspend fun insertTransmittalItems(items: List<DrawingTransmittalItemEntity>)
    @Insert suspend fun insertMarkup(item: DrawingMarkupEntity): Long
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY date DESC, id DESC")
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE projectId = :projectId ORDER BY date DESC, id DESC")
    fun getMeasurementsByProject(projectId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE projectId = :projectId AND contractorId = :contractorId ORDER BY date DESC, id DESC")
    fun getMeasurementsByProjectAndContractor(projectId: Long, contractorId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE componentId = :componentId ORDER BY id ASC")
    fun getMeasurementsByComponent(componentId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE componentId = :componentId AND itemId = :itemId ORDER BY id ASC")
    fun getMeasurementsByComponentAndItem(componentId: Long, itemId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE roomId = :roomId ORDER BY id ASC")
    fun getMeasurementsByRoom(roomId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE floorId = :floorId ORDER BY id ASC")
    fun getMeasurementsByFloor(floorId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE projectId = :projectId AND (floorId = :floorId OR floor = :floorName) AND (LOWER(itemName) LIKE '%brick%' OR LOWER(description) LIKE '%brick%') ORDER BY id ASC")
    suspend fun getBrickworkMeasurementsForFloor(projectId: Long, floorId: Long?, floorName: String): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE id = :id LIMIT 1")
    suspend fun getMeasurementById(id: Long): MeasurementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurements(measurements: List<MeasurementEntity>): List<Long>

    @Update
    suspend fun updateMeasurement(measurement: MeasurementEntity)

    @Delete
    suspend fun deleteMeasurement(measurement: MeasurementEntity)

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteMeasurementById(id: Long)

}

@Dao
interface MeasurementSheetDao {
    @Query("SELECT * FROM measurement_sheets WHERE archivedAt IS NULL ORDER BY createdAt DESC, id DESC")
    fun getAllSheets(): Flow<List<MeasurementSheetEntity>>

    @Query("SELECT * FROM measurement_sheets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MeasurementSheetEntity?

    @Insert
    suspend fun insert(sheet: MeasurementSheetEntity): Long

    @Query("UPDATE measurement_sheets SET status=:status, revision=:revision, lockedAt=CASE WHEN :status='APPROVED' THEN :now ELSE lockedAt END, updatedAt=:now WHERE id=:id")
    suspend fun updateWorkflow(id: Long, status: String, revision: Int, now: Long)

    @Query("UPDATE measurement_sheets SET archivedAt=:now, updatedAt=:now WHERE id=:id AND archivedAt IS NULL")
    suspend fun archive(id: Long, now: Long): Int
}

@Dao
interface MeasurementReviewEventDao {
    @Query("SELECT * FROM measurement_review_events WHERE sheetId=:sheetId ORDER BY createdAt ASC, id ASC")
    fun getForSheet(sheetId: Long): Flow<List<MeasurementReviewEventEntity>>

    @Insert
    suspend fun insert(event: MeasurementReviewEventEntity): Long
}

@Dao
interface WorkItemAliasDao {
    @Query("SELECT * FROM work_item_aliases WHERE workItemId = :workItemId ORDER BY alias")
    fun getAliases(workItemId: Long): Flow<List<WorkItemAliasEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(alias: WorkItemAliasEntity): Long

    @Delete
    suspend fun delete(alias: WorkItemAliasEntity)
}
