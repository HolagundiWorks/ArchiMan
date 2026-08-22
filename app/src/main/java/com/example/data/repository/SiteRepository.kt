package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

class SiteRepository(private val database: AppDatabase) {
    val allProjects: Flow<List<ProjectEntity>> = database.projectDao().getAllProjects()
    val allClients: Flow<List<ClientEntity>> = database.clientDao().getAllClients()
    val allContractors: Flow<List<ContractorEntity>> = database.contractorDao().getAllContractors()
    val allQualifiedItems: Flow<List<ContractorQualifiedItemEntity>> = database.contractorDao().getAllQualifiedItems()
    val allItems: Flow<List<ItemMasterEntity>> = database.itemMasterDao().getAllItems()
    val allMeasurements: Flow<List<MeasurementEntity>> = database.measurementDao().getAllMeasurements()
    val allBills: Flow<List<BillEntity>> = database.billDao().getAllBills()

    // Client operations
    suspend fun insertClient(client: ClientEntity): Long =
        database.clientDao().insertClient(client)

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

    fun getRatesForContractor(contractorId: Long): Flow<List<ContractorRateEntity>> =
        database.contractorRateDao().getRatesForContractor(contractorId)

    suspend fun getRateForContractorAndItem(contractorId: Long, itemId: Long): Double? {
        val contractorRate = database.contractorRateDao().getRate(contractorId, itemId)
        if (contractorRate != null) {
            return contractorRate.rate
        }
        val item = database.itemMasterDao().getItemById(itemId)
        return item?.defaultRate
    }

    // Projects & Contractors
    suspend fun insertProject(project: ProjectEntity): Long =
        database.projectDao().insertProject(project)

    suspend fun updateProject(project: ProjectEntity) =
        database.projectDao().updateProject(project)

    suspend fun deleteProject(project: ProjectEntity) =
        database.projectDao().deleteProject(project)

    suspend fun insertContractor(contractor: ContractorEntity): Long =
        database.contractorDao().insertContractor(contractor)

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

    // Items & Rates
    suspend fun insertItem(item: ItemMasterEntity): Long =
        database.itemMasterDao().insertItem(item)

    suspend fun updateItem(item: ItemMasterEntity) =
        database.itemMasterDao().updateItem(item)

    suspend fun deleteItem(item: ItemMasterEntity) =
        database.itemMasterDao().deleteItem(item)

    suspend fun saveContractorRate(contractorId: Long, itemId: Long, rate: Double) =
        database.contractorRateDao().insertRate(ContractorRateEntity(contractorId = contractorId, itemId = itemId, rate = rate))

    // Measurements
    suspend fun insertMeasurement(measurement: MeasurementEntity): Long =
        database.measurementDao().insertMeasurement(measurement)

    suspend fun updateMeasurement(measurement: MeasurementEntity) =
        database.measurementDao().updateMeasurement(measurement)

    suspend fun deleteMeasurement(measurement: MeasurementEntity) =
        database.measurementDao().deleteMeasurement(measurement)

    suspend fun deleteMeasurementById(id: Long) =
        database.measurementDao().deleteMeasurementById(id)

    suspend fun getUnbilledMeasurements(projectId: Long, contractorId: Long): List<MeasurementEntity> =
        database.measurementDao().getUnbilledMeasurements(projectId, contractorId)

    suspend fun createBill(bill: BillEntity, measurementIds: List<Long>): Long {
        val billId = database.billDao().insertBill(bill)
        if (measurementIds.isNotEmpty()) {
            database.measurementDao().linkMeasurementsToBill(measurementIds, billId)
        }
        return billId
    }

    suspend fun deleteBill(bill: BillEntity) =
        database.billDao().deleteBill(bill)

    suspend fun getMeasurementsForBill(billId: Long): List<MeasurementEntity> =
        database.measurementDao().getMeasurementsForBill(billId)
}
