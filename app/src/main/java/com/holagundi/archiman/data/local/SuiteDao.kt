package com.holagundi.archiman.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.holagundi.archiman.data.local.entity.CoordinationEventEntity
import com.holagundi.archiman.data.local.entity.CoordinationItemEntity
import com.holagundi.archiman.data.local.entity.DailySiteReportEntity
import com.holagundi.archiman.data.local.entity.DrawingRevisionEntity
import com.holagundi.archiman.data.local.entity.DrawingTransmittalEntity
import com.holagundi.archiman.data.local.entity.ProjectDrawingEntity
import com.holagundi.archiman.data.local.entity.EnquiryEntity
import com.holagundi.archiman.data.local.entity.FeeProposalEntity
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ItemLinkRuleEntity
import com.holagundi.archiman.data.local.entity.LetterEntity
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
import com.holagundi.archiman.data.local.entity.SiteInspectionEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEventEntity
import com.holagundi.archiman.data.local.entity.TimeEntryEntity
import com.holagundi.archiman.data.local.entity.WorkContractEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuiteDao {
    @Query("SELECT * FROM project_settings WHERE projectId = :projectId")
    fun observeSettings(projectId: Long): Flow<ProjectSettingsEntity?>

    @Query("SELECT * FROM project_settings WHERE projectId = :projectId")
    suspend fun settings(projectId: Long): ProjectSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: ProjectSettingsEntity)

    @Query("SELECT * FROM project_stages WHERE projectId = :projectId ORDER BY orderIndex")
    fun observeStages(projectId: Long): Flow<List<ProjectStageEntity>>

    @Query("SELECT * FROM project_stages")
    fun observeAllStages(): Flow<List<ProjectStageEntity>>

    @Insert
    suspend fun insertStage(stage: ProjectStageEntity): Long

    @Query("UPDATE project_stages SET status = :status WHERE id = :id")
    suspend fun setStageStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM project_stages WHERE projectId = :projectId")
    suspend fun stageCount(projectId: Long): Int

    @Query("SELECT * FROM invoices ORDER BY createdAt DESC")
    fun observeAllInvoices(): Flow<List<InvoiceEntity>>

    @Query("SELECT * FROM invoices WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeInvoices(projectId: Long): Flow<List<InvoiceEntity>>

    @Insert
    suspend fun insertInvoice(invoice: InvoiceEntity): Long

    @Query("UPDATE invoices SET status = :status WHERE id = :id")
    suspend fun setInvoiceStatus(id: Long, status: String)

    @Query("SELECT * FROM receipts ORDER BY receivedAt DESC")
    fun observeAllReceipts(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE projectId = :projectId ORDER BY receivedAt DESC")
    fun observeReceipts(projectId: Long): Flow<List<ReceiptEntity>>

    @Insert
    suspend fun insertReceipt(receipt: ReceiptEntity): Long

    @Query("SELECT * FROM enquiries ORDER BY createdAt DESC")
    fun observeEnquiries(): Flow<List<EnquiryEntity>>

    @Insert
    suspend fun insertEnquiry(enquiry: EnquiryEntity): Long

    @Query("SELECT * FROM permits WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observePermits(projectId: Long): Flow<List<PermitEntity>>

    @Insert
    suspend fun insertPermit(permit: PermitEntity): Long

    @Query("SELECT * FROM time_entries WHERE projectId = :projectId ORDER BY workDate DESC")
    fun observeTime(projectId: Long): Flow<List<TimeEntryEntity>>

    @Insert
    suspend fun insertTime(entry: TimeEntryEntity): Long

    @Query("SELECT * FROM letters ORDER BY createdAt DESC")
    fun observeLetters(): Flow<List<LetterEntity>>

    @Insert
    suspend fun insertLetter(letter: LetterEntity): Long

    @Query("SELECT * FROM fee_proposals WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeProposals(projectId: Long): Flow<List<FeeProposalEntity>>

    @Insert
    suspend fun insertProposal(proposal: FeeProposalEntity): Long

    @Query("SELECT * FROM rcc_members WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeRcc(projectId: Long): Flow<List<RccMemberEntity>>

    @Insert
    suspend fun insertRcc(member: RccMemberEntity): Long

    @Query("SELECT * FROM rate_book_items ORDER BY itemName")
    fun observeRates(): Flow<List<RateBookItemEntity>>

    @Insert
    suspend fun insertRate(item: RateBookItemEntity): Long

    @Query("SELECT * FROM work_contracts WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeContracts(projectId: Long): Flow<List<WorkContractEntity>>

    @Insert
    suspend fun insertContract(contract: WorkContractEntity): Long

    @Query("SELECT * FROM schedule_activities WHERE projectId = :projectId ORDER BY createdAt")
    fun observeActivities(projectId: Long): Flow<List<ScheduleActivityEntity>>

    @Insert
    suspend fun insertActivity(activity: ScheduleActivityEntity): Long

    @Query("SELECT * FROM item_link_rules ORDER BY sourceTrade")
    fun observeLinks(): Flow<List<ItemLinkRuleEntity>>

    @Query("SELECT COUNT(*) FROM item_link_rules")
    suspend fun linkCount(): Int

    @Query("SELECT COUNT(*) FROM rate_book_items")
    suspend fun rateCount(): Int

    @Query("SELECT * FROM item_link_rules WHERE id = :id")
    suspend fun link(id: Long): ItemLinkRuleEntity?

    @Insert
    suspend fun insertLink(rule: ItemLinkRuleEntity): Long

    @Query("SELECT * FROM meeting_minutes WHERE projectId = :projectId ORDER BY meetingAt DESC")
    fun observeMeetings(projectId: Long): Flow<List<MeetingMinutesEntity>>

    @Insert
    suspend fun insertMeeting(meeting: MeetingMinutesEntity): Long

    @Query("SELECT * FROM daily_site_reports WHERE projectId = :projectId ORDER BY reportDate DESC")
    fun observeLogs(projectId: Long): Flow<List<DailySiteReportEntity>>

    @Query("SELECT * FROM daily_site_reports WHERE projectId = :projectId ORDER BY reportDate DESC")
    suspend fun logs(projectId: Long): List<DailySiteReportEntity>

    @Insert
    suspend fun insertLog(report: DailySiteReportEntity): Long

    @Query("SELECT * FROM site_issues WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeIssues(projectId: Long): Flow<List<SiteIssueEntity>>

    @Query("SELECT * FROM site_issues WHERE projectId = :projectId ORDER BY updatedAt DESC")
    suspend fun issues(projectId: Long): List<SiteIssueEntity>

    @Query("SELECT * FROM site_issues WHERE id = :id")
    suspend fun issue(id: Long): SiteIssueEntity?

    @Insert
    suspend fun insertIssue(issue: SiteIssueEntity): Long

    @Update
    suspend fun updateIssue(issue: SiteIssueEntity)

    @Insert
    suspend fun insertIssueEvent(event: SiteIssueEventEntity): Long

    @Query("SELECT * FROM project_tasks WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeTasks(projectId: Long): Flow<List<ProjectTaskEntity>>

    @Insert
    suspend fun insertTask(task: ProjectTaskEntity): Long

    @Query("UPDATE project_tasks SET status = :status WHERE id = :id")
    suspend fun setTaskStatus(id: Long, status: String)

    @Query("SELECT * FROM project_consultancy_profiles WHERE projectId = :projectId LIMIT 1")
    fun observeBrief(projectId: Long): Flow<ProjectConsultancyProfileEntity?>

    @Query("SELECT * FROM project_consultancy_profiles WHERE projectId = :projectId LIMIT 1")
    suspend fun brief(projectId: Long): ProjectConsultancyProfileEntity?

    @Insert
    suspend fun insertBrief(profile: ProjectConsultancyProfileEntity): Long

    @Update
    suspend fun updateBrief(profile: ProjectConsultancyProfileEntity)

    @Query("SELECT * FROM project_approvals WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeApprovals(projectId: Long): Flow<List<ProjectApprovalEntity>>

    @Insert
    suspend fun insertApproval(approval: ProjectApprovalEntity): Long

    @Query("SELECT * FROM material_orders WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeOrders(projectId: Long): Flow<List<MaterialOrderEntity>>

    @Insert
    suspend fun insertOrder(order: MaterialOrderEntity): Long

    @Query(
        """
        SELECT l.* FROM material_order_lines l
        INNER JOIN material_orders o ON o.id = l.orderId
        WHERE o.projectId = :projectId
        ORDER BY l.id
        """,
    )
    fun observeOrderLines(projectId: Long): Flow<List<MaterialOrderLineEntity>>

    @Insert
    suspend fun insertOrderLine(line: MaterialOrderLineEntity): Long

    @Query("UPDATE material_orders SET status = :status, issuedAt = :issuedAt WHERE id = :id")
    suspend fun setOrderStatus(id: Long, status: String, issuedAt: Long?)

    @Query("UPDATE project_approvals SET status = :status, approvedAt = :approvedAt WHERE id = :id")
    suspend fun setApprovalStatus(id: Long, status: String, approvedAt: Long?)

    @Query("UPDATE schedule_activities SET progress = :progress, status = :status WHERE id = :id")
    suspend fun setActivityProgress(id: Long, progress: Int, status: String)

    @Query("SELECT * FROM drawing_transmittals ORDER BY issuedAt DESC")
    fun observeAllTransmittals(): Flow<List<DrawingTransmittalEntity>>

    @Query("SELECT * FROM drawing_transmittals WHERE projectId = :projectId ORDER BY issuedAt DESC")
    fun observeTransmittals(projectId: Long): Flow<List<DrawingTransmittalEntity>>

    @Insert
    suspend fun insertTransmittal(transmittal: DrawingTransmittalEntity): Long

    @Query("SELECT * FROM site_inspections WHERE projectId = :projectId ORDER BY inspectionAt DESC")
    fun observeInspections(projectId: Long): Flow<List<SiteInspectionEntity>>

    @Insert
    suspend fun insertInspection(inspection: SiteInspectionEntity): Long

    @Query("SELECT * FROM coordination_items WHERE projectId = :projectId AND archivedAt IS NULL ORDER BY updatedAt DESC")
    fun observeCoordination(projectId: Long): Flow<List<CoordinationItemEntity>>

    @Query("SELECT * FROM coordination_items WHERE projectId = :projectId AND archivedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun coordinationItems(projectId: Long): List<CoordinationItemEntity>

    @Query("SELECT * FROM coordination_items WHERE id = :id")
    suspend fun coordination(id: Long): CoordinationItemEntity?

    @Insert
    suspend fun insertCoordination(item: CoordinationItemEntity): Long

    @Update
    suspend fun updateCoordination(item: CoordinationItemEntity)

    @Insert
    suspend fun insertCoordinationEvent(event: CoordinationEventEntity): Long

    @Query("SELECT * FROM project_drawings WHERE projectId = :projectId AND archivedAt IS NULL ORDER BY drawingNumber")
    fun observeDrawings(projectId: Long): Flow<List<ProjectDrawingEntity>>

    @Query("SELECT * FROM project_drawings WHERE projectId = :projectId AND archivedAt IS NULL ORDER BY drawingNumber")
    suspend fun drawings(projectId: Long): List<ProjectDrawingEntity>

    @Insert
    suspend fun insertDrawing(drawing: ProjectDrawingEntity): Long

    @Query("SELECT * FROM drawing_revisions WHERE drawingId = :drawingId ORDER BY createdAt DESC")
    fun observeRevisions(drawingId: Long): Flow<List<DrawingRevisionEntity>>

    @Query("SELECT * FROM drawing_revisions WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun observeProjectRevisions(projectId: Long): Flow<List<DrawingRevisionEntity>>

    @Insert
    suspend fun insertRevision(revision: DrawingRevisionEntity): Long
}
