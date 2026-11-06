package com.holagundi.archiman.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.holagundi.archiman.data.local.entity.ClientEntity
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.ContractorEntity
import com.holagundi.archiman.data.local.entity.ContractorQualifiedItemEntity
import com.holagundi.archiman.data.local.entity.FloorEntity
import com.holagundi.archiman.data.local.entity.ItemMasterEntity
import com.holagundi.archiman.data.local.entity.MeasurementEntity
import com.holagundi.archiman.data.local.entity.MeasurementReviewEventEntity
import com.holagundi.archiman.data.local.entity.MeasurementSheetEntity
import com.holagundi.archiman.data.local.entity.LocalUserEntity
import com.holagundi.archiman.data.local.entity.PortalAuditEventEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.data.local.entity.RoomEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PracticeDao {
    @Query("SELECT * FROM company_profile WHERE id = 1")
    fun observeCompany(): Flow<CompanyProfileEntity?>

    @Query("SELECT * FROM company_profile WHERE id = 1")
    suspend fun company(): CompanyProfileEntity?

    @Query("SELECT COUNT(*) FROM company_profile")
    suspend fun companyCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompany(profile: CompanyProfileEntity)

    @Query("SELECT * FROM clients ORDER BY name")
    fun observeClients(): Flow<List<ClientEntity>>

    @Insert
    suspend fun insertClient(client: ClientEntity): Long

    @Query("SELECT * FROM local_users ORDER BY displayName")
    fun observeUsers(): Flow<List<LocalUserEntity>>

    @Query("SELECT * FROM local_users WHERE isActive = 1 ORDER BY displayName")
    suspend fun activeUsers(): List<LocalUserEntity>

    @Query("SELECT COUNT(*) FROM local_users")
    suspend fun userCount(): Int

    @Query("SELECT * FROM local_users WHERE id = :id")
    suspend fun user(id: Long): LocalUserEntity?

    @Insert
    suspend fun insertUser(user: LocalUserEntity): Long

    @Insert
    suspend fun insertAudit(event: PortalAuditEventEntity): Long

    @Query("UPDATE local_users SET lastLoginAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun touchLogin(id: Long, at: Long)
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    suspend fun projects(): List<ProjectEntity>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun observeProject(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: Long): ProjectEntity?

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("SELECT * FROM floors WHERE projectId = :projectId ORDER BY orderIndex")
    fun observeFloors(projectId: Long): Flow<List<FloorEntity>>

    @Query("SELECT * FROM floors WHERE projectId = :projectId ORDER BY orderIndex")
    suspend fun floors(projectId: Long): List<FloorEntity>

    @Insert
    suspend fun insertFloor(floor: FloorEntity): Long

    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY orderIndex")
    fun observeRooms(projectId: Long): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE projectId = :projectId AND floorId = :floorId ORDER BY orderIndex")
    suspend fun roomsOnFloor(projectId: Long, floorId: Long): List<RoomEntity>

    @Insert
    suspend fun insertRoom(room: RoomEntity): Long
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM contractors WHERE projectId = :projectId OR projectId = 0 ORDER BY name")
    fun observeContractors(projectId: Long): Flow<List<ContractorEntity>>

    @Insert
    suspend fun insertContractor(contractor: ContractorEntity): Long

    @Query("SELECT * FROM contractor_qualified_items WHERE contractorId = :contractorId ORDER BY workType, itemName")
    fun observeQualified(contractorId: Long): Flow<List<ContractorQualifiedItemEntity>>

    @Insert
    suspend fun insertQualified(item: ContractorQualifiedItemEntity): Long

    @Query("SELECT * FROM item_master WHERE isActive = 1 ORDER BY workType, name")
    fun observeActiveItems(): Flow<List<ItemMasterEntity>>

    @Query("SELECT * FROM item_master WHERE name = :name LIMIT 1")
    suspend fun findItemByName(name: String): ItemMasterEntity?

    @Query("SELECT * FROM contractors WHERE projectId = :projectId OR projectId = 0 ORDER BY name LIMIT 1")
    suspend fun firstContractor(projectId: Long): ContractorEntity?

    @Insert
    suspend fun insertItem(item: ItemMasterEntity): Long
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurement_sheets WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeSheets(projectId: Long): Flow<List<MeasurementSheetEntity>>

    @Query("SELECT * FROM measurements WHERE projectId = :projectId ORDER BY date DESC")
    fun observeMeasurements(projectId: Long): Flow<List<MeasurementEntity>>

    @Query(
        """
        SELECT m.* FROM measurements m
        INNER JOIN measurement_sheets s ON s.id = m.sheetId
        WHERE m.projectId = :projectId AND s.status = 'APPROVED'
        ORDER BY m.itemName
        """,
    )
    fun observeApprovedMeasurements(projectId: Long): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE projectId = :projectId")
    suspend fun measurementsNow(projectId: Long): List<MeasurementEntity>

    @Query(
        """
        SELECT * FROM measurement_sheets
        WHERE projectId = :projectId
          AND contractorId = :contractorId
          AND itemNameSnapshot = :itemName
          AND floorId = :floorId
          AND status IN ('DRAFT', 'RETURNED')
          AND uomSnapshot = :unit AND formulaCode = :formula
          AND boqRevision = :boqRevision AND boqLineKey = :boqLineKey
          AND archivedAt IS NULL
        LIMIT 1
        """,
    )
    suspend fun findOpenSheet(projectId: Long, contractorId: Long, itemName: String, floorId: Long, unit: String, formula: String, boqRevision: String, boqLineKey: String): MeasurementSheetEntity?

    @Query("SELECT COUNT(*) FROM measurement_sheets WHERE projectId = :projectId")
    suspend fun sheetCount(projectId: Long): Int

    @Query("SELECT * FROM measurement_sheets WHERE id = :id")
    suspend fun getSheet(id: Long): MeasurementSheetEntity?

    @Insert
    suspend fun insertSheet(sheet: MeasurementSheetEntity): Long

    @Update
    suspend fun updateSheet(sheet: MeasurementSheetEntity)

    @Insert
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long

    @Query("SELECT * FROM measurements WHERE sheetId = :sheetId ORDER BY date")
    suspend fun measurementsForSheet(sheetId: Long): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE id = :id")
    suspend fun getMeasurement(id: Long): MeasurementEntity?

    @Query("DELETE FROM measurements WHERE id = :id")
    suspend fun deleteMeasurement(id: Long)

    @Insert
    suspend fun insertReview(event: MeasurementReviewEventEntity): Long
}

@Dao
interface FieldDao {
    @Query("SELECT COUNT(*) FROM daily_site_reports WHERE projectId = :projectId")
    fun observeDailyLogCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM material_orders WHERE projectId = :projectId")
    fun observeMaterialOrderCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM project_drawings WHERE projectId = :projectId AND archivedAt IS NULL")
    fun observeDrawingCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM site_issues WHERE projectId = :projectId")
    fun observeSiteIssueCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM site_inspections WHERE projectId = :projectId")
    fun observeInspectionCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM meeting_minutes WHERE projectId = :projectId")
    fun observeMinutesCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM coordination_items WHERE projectId = :projectId AND archivedAt IS NULL")
    fun observeCoordinationCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM project_decisions WHERE projectId = :projectId")
    fun observeDecisionCount(projectId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM project_tasks WHERE projectId = :projectId")
    fun observeTaskCount(projectId: Long): Flow<Int>
}
