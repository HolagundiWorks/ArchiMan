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
    @Query("SELECT * FROM item_master ORDER BY id ASC")
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

    @Query("SELECT COUNT(*) FROM item_master")
    suspend fun getCount(): Int
}

@Dao
interface ContractorRateDao {
    @Query("SELECT * FROM contractor_rates WHERE contractorId = :contractorId")
    fun getRatesForContractor(contractorId: Long): Flow<List<ContractorRateEntity>>

    @Query("SELECT * FROM contractor_rates WHERE contractorId = :contractorId AND itemId = :itemId LIMIT 1")
    suspend fun getRate(contractorId: Long, itemId: Long): ContractorRateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: ContractorRateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRates(rates: List<ContractorRateEntity>)

    @Query("DELETE FROM contractor_rates WHERE contractorId = :contractorId AND itemId = :itemId")
    suspend fun deleteRate(contractorId: Long, itemId: Long)
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

    @Query("SELECT * FROM measurements WHERE projectId = :projectId AND contractorId = :contractorId AND (billId IS NULL OR billId = 0)")
    suspend fun getUnbilledMeasurements(projectId: Long, contractorId: Long): List<MeasurementEntity>

    @Query("SELECT * FROM measurements WHERE billId = :billId")
    suspend fun getMeasurementsForBill(billId: Long): List<MeasurementEntity>

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

    @Query("UPDATE measurements SET billId = :billId WHERE id IN (:ids)")
    suspend fun linkMeasurementsToBill(ids: List<Long>, billId: Long)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY date DESC, id DESC")
    fun getAllBills(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE projectId = :projectId ORDER BY date DESC")
    fun getBillsByProject(projectId: Long): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id LIMIT 1")
    suspend fun getBillById(id: Long): BillEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: BillEntity): Long

    @Delete
    suspend fun deleteBill(bill: BillEntity)
}
