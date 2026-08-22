package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ClientEntity::class,
        ProjectEntity::class,
        FloorEntity::class,
        RoomEntity::class,
        ComponentEntity::class,
        ComponentWorkItemEntity::class,
        ContractorEntity::class,
        ContractorQualifiedItemEntity::class,
        ProjectContractorCrossRef::class,
        ItemMasterEntity::class,
        ContractorRateEntity::class,
        MeasurementEntity::class,
        BillEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun projectDao(): ProjectDao
    abstract fun floorDao(): FloorDao
    abstract fun roomDao(): RoomDao
    abstract fun componentDao(): ComponentDao
    abstract fun componentWorkItemDao(): ComponentWorkItemDao
    abstract fun contractorDao(): ContractorDao
    abstract fun itemMasterDao(): ItemMasterDao
    abstract fun contractorRateDao(): ContractorRateDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun billDao(): BillDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "site_measurement.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed predefined items & structure
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).seedPredefinedData()
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        val PREDEFINED_ITEMS = listOf(
            ItemMasterEntity(name = "Brickwork", unit = "m³", calculationType = CalculationType.VOLUME, defaultRate = 7200.0, isPredefined = true),
            ItemMasterEntity(name = "BrickWork 230mm", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 850.0, isPredefined = true),
            ItemMasterEntity(name = "BrickWork 115mm", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 480.0, isPredefined = true),
            ItemMasterEntity(name = "PCC", unit = "m³", calculationType = CalculationType.VOLUME, defaultRate = 5800.0, isPredefined = true),
            ItemMasterEntity(name = "RCC", unit = "m³", calculationType = CalculationType.VOLUME, defaultRate = 6800.0, isPredefined = true),
            ItemMasterEntity(name = "Slab Concrete", unit = "m³", calculationType = CalculationType.VOLUME, defaultRate = 7100.0, isPredefined = true),
            ItemMasterEntity(name = "Beam Concrete", unit = "m³", calculationType = CalculationType.VOLUME, defaultRate = 7400.0, isPredefined = true),
            ItemMasterEntity(name = "Shuttering & Formwork", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 380.0, isPredefined = true),
            ItemMasterEntity(name = "Plaster 12mm", unit = "m²", calculationType = CalculationType.WALL_PLASTER, defaultRate = 220.0, isPredefined = true),
            ItemMasterEntity(name = "Plaster 20mm (External)", unit = "m²", calculationType = CalculationType.WALL_PLASTER, defaultRate = 290.0, isPredefined = true),
            ItemMasterEntity(name = "Putty", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 45.0, isPredefined = true),
            ItemMasterEntity(name = "Flooring", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 180.0, isPredefined = true),
            ItemMasterEntity(name = "Skirting", unit = "m", calculationType = CalculationType.RUNNING_LENGTH, defaultRate = 95.0, isPredefined = true),
            ItemMasterEntity(name = "Painting", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 65.0, isPredefined = true),
            ItemMasterEntity(name = "Waterproofing", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 140.0, isPredefined = true),
            ItemMasterEntity(name = "Doors/windows", unit = "Nos", calculationType = CalculationType.NOS, defaultRate = 2500.0, isPredefined = true),
            ItemMasterEntity(name = "False Ceiling", unit = "m²", calculationType = CalculationType.AREA, defaultRate = 320.0, isPredefined = true)
        )
    }

    suspend fun seedPredefinedData() {
        if (itemMasterDao().getCount() == 0) {
            itemMasterDao().insertAll(PREDEFINED_ITEMS)
            
            // 1. Seed Clients
            val clientId1 = clientDao().insertClient(
                ClientEntity(
                    name = "Laxmi Developers & Builders",
                    address = "Suite 401, Apex Commercial Hub, MG Road",
                    contactNo = "+91 98450 12345"
                )
            )
            val clientId2 = clientDao().insertClient(
                ClientEntity(
                    name = "Prestige Estates Corp",
                    address = "Tower 2, Prestige Technology Park, Outer Ring Rd",
                    contactNo = "+91 98800 67890"
                )
            )

            // 2. Seed Contractors with Qualified Items
            val contId1 = contractorDao().insertContractor(
                ContractorEntity(
                    name = "Sharma Civil Works",
                    address = "Plot 18, Industrial Estate Phase 1",
                    contactNo = "+91 98765 43210",
                    phone = "+91 98765 43210"
                )
            )
            // Sharma's qualified items
            contractorDao().insertQualifiedItems(
                listOf(
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "BrickWork 230mm", uom = "m²", calculationType = CalculationType.AREA, rate = 850.0),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "BrickWork 115mm", uom = "m²", calculationType = CalculationType.AREA, rate = 480.0),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "Plaster 12mm", uom = "m²", calculationType = CalculationType.WALL_PLASTER, rate = 220.0),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "Slab Concrete", uom = "m³", calculationType = CalculationType.VOLUME, rate = 7100.0),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "Beam Concrete", uom = "m³", calculationType = CalculationType.VOLUME, rate = 7400.0)
                )
            )

            val contId2 = contractorDao().insertContractor(
                ContractorEntity(
                    name = "Verma Plaster & Painting",
                    address = "Shop 12, Main Market Road",
                    contactNo = "+91 98123 45678",
                    phone = "+91 98123 45678"
                )
            )
            // Verma's qualified items
            contractorDao().insertQualifiedItems(
                listOf(
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Plaster 12mm", uom = "m²", calculationType = CalculationType.WALL_PLASTER, rate = 220.0),
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Plaster 20mm (External)", uom = "m²", calculationType = CalculationType.WALL_PLASTER, rate = 290.0),
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Putty", uom = "m²", calculationType = CalculationType.AREA, rate = 45.0),
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Painting", uom = "m²", calculationType = CalculationType.AREA, rate = 65.0)
                )
            )

            // 3. Seed Project linked to Client & Contractors
            val projId = projectDao().insertProject(
                ProjectEntity(
                    name = "ABC Residence",
                    client = "Laxmi Developers & Builders",
                    clientId = clientId1,
                    siteLocation = "Plot 42, Green Avenue, Sector 15"
                )
            )

            // Link contractors to project
            contractorDao().insertProjectContractorRefs(
                listOf(
                    ProjectContractorCrossRef(projectId = projId, contractorId = contId1),
                    ProjectContractorCrossRef(projectId = projId, contractorId = contId2)
                )
            )

            // Seed rates
            contractorRateDao().insertRate(ContractorRateEntity(contractorId = contId1, itemId = 1, rate = 7200.0)) // Brickwork
            contractorRateDao().insertRate(ContractorRateEntity(contractorId = contId1, itemId = 4, rate = 220.0))  // Plaster
            contractorRateDao().insertRate(ContractorRateEntity(contractorId = contId1, itemId = 5, rate = 45.0))   // Putty
            contractorRateDao().insertRate(ContractorRateEntity(contractorId = contId1, itemId = 6, rate = 180.0))  // Flooring
            contractorRateDao().insertRate(ContractorRateEntity(contractorId = contId1, itemId = 8, rate = 65.0))   // Painting

            // Seed Floors: Ground Floor (lvl0), First Floor (lvl1), Second Floor (lvl2)
            val gfId = floorDao().insertFloor(FloorEntity(projectId = projId, name = "Level 0 (Ground Floor)", orderIndex = 0))
            val ffId = floorDao().insertFloor(FloorEntity(projectId = projId, name = "Level 1 (First Floor)", orderIndex = 1))
            val sfId = floorDao().insertFloor(FloorEntity(projectId = projId, name = "Level 2 (Second Floor)", orderIndex = 2))

            // Seed sample Brickwork measurements on Level 0 for Sharma Civil Works
            measurementDao().insertMeasurement(
                MeasurementEntity(
                    projectId = projId,
                    floorId = gfId,
                    contractorId = contId1,
                    contractorName = "Sharma Civil Works",
                    itemId = 2,
                    itemName = "BrickWork 230mm",
                    unit = "m²",
                    calculationType = CalculationType.AREA,
                    description = "External Perimeter Wall Grid A-D",
                    length = 12.50,
                    height = 3.00,
                    nos = 1.0,
                    deduction = 0.0,
                    quantity = 37.50,
                    rate = 850.0,
                    amount = 31875.0,
                    floor = "Level 0 (Ground Floor)",
                    location = "Grid A-D Exterior",
                    remarks = "230mm Red Clay Brick masonry"
                )
            )
            measurementDao().insertMeasurement(
                MeasurementEntity(
                    projectId = projId,
                    floorId = gfId,
                    contractorId = contId1,
                    contractorName = "Sharma Civil Works",
                    itemId = 2,
                    itemName = "BrickWork 230mm",
                    unit = "m²",
                    calculationType = CalculationType.AREA,
                    description = "Living Room Partition Wall",
                    length = 5.20,
                    height = 3.00,
                    nos = 1.0,
                    deduction = 0.0,
                    quantity = 15.60,
                    rate = 850.0,
                    amount = 13260.0,
                    floor = "Level 0 (Ground Floor)",
                    location = "Living Hall",
                    remarks = "Partition wall"
                )
            )
            measurementDao().insertMeasurement(
                MeasurementEntity(
                    projectId = projId,
                    floorId = gfId,
                    contractorId = contId1,
                    contractorName = "Sharma Civil Works",
                    itemId = 2,
                    itemName = "BrickWork 230mm",
                    unit = "m²",
                    calculationType = CalculationType.AREA,
                    description = "Kitchen Rear Wall",
                    length = 4.80,
                    height = 3.00,
                    nos = 1.0,
                    deduction = 0.0,
                    quantity = 14.40,
                    rate = 850.0,
                    amount = 12240.0,
                    floor = "Level 0 (Ground Floor)",
                    location = "Kitchen Area",
                    remarks = "Rear boundary"
                )
            )

            // Seed Rooms in First Floor
            val rBed1 = roomDao().insertRoom(RoomEntity(projectId = projId, floorId = ffId, name = "Bedroom 1", orderIndex = 0))
            val rBed2 = roomDao().insertRoom(RoomEntity(projectId = projId, floorId = ffId, name = "Bedroom 2", orderIndex = 1))
            val rLiving = roomDao().insertRoom(RoomEntity(projectId = projId, floorId = ffId, name = "Living", orderIndex = 2))
            val rKitchen = roomDao().insertRoom(RoomEntity(projectId = projId, floorId = ffId, name = "Kitchen", orderIndex = 3))
            val rToilet1 = roomDao().insertRoom(RoomEntity(projectId = projId, floorId = ffId, name = "Toilet 1", orderIndex = 4))

            // Seed Components in Bedroom 1
            val cNorthWall = componentDao().insertComponent(
                ComponentEntity(
                    projectId = projId,
                    floorId = ffId,
                    roomId = rBed1,
                    name = "North Wall",
                    type = ComponentType.WALL,
                    length = 4.50,
                    height = 3.00,
                    thickness = 0.23
                )
            )
            val cSouthWall = componentDao().insertComponent(
                ComponentEntity(
                    projectId = projId,
                    floorId = ffId,
                    roomId = rBed1,
                    name = "South Wall",
                    type = ComponentType.WALL,
                    length = 4.50,
                    height = 3.00,
                    thickness = 0.23
                )
            )
            val cFloor = componentDao().insertComponent(
                ComponentEntity(
                    projectId = projId,
                    floorId = ffId,
                    roomId = rBed1,
                    name = "Bedroom Floor",
                    type = ComponentType.FLOOR,
                    length = 4.50,
                    width = 3.80
                )
            )
            val cCeiling = componentDao().insertComponent(
                ComponentEntity(
                    projectId = projId,
                    floorId = ffId,
                    roomId = rBed1,
                    name = "Ceiling",
                    type = ComponentType.CEILING,
                    length = 4.50,
                    width = 3.80
                )
            )

            // Connect Work Items to North Wall
            componentWorkItemDao().insertWorkItems(
                listOf(
                    ComponentWorkItemEntity(componentId = cNorthWall, itemId = 1, itemName = "Brickwork", unit = "m³", calculationType = CalculationType.VOLUME),
                    ComponentWorkItemEntity(componentId = cNorthWall, itemId = 4, itemName = "Plaster", unit = "m²", calculationType = CalculationType.WALL_PLASTER),
                    ComponentWorkItemEntity(componentId = cNorthWall, itemId = 5, itemName = "Putty", unit = "m²", calculationType = CalculationType.AREA),
                    ComponentWorkItemEntity(componentId = cNorthWall, itemId = 8, itemName = "Painting", unit = "m²", calculationType = CalculationType.AREA),

                    // South wall
                    ComponentWorkItemEntity(componentId = cSouthWall, itemId = 1, itemName = "Brickwork", unit = "m³", calculationType = CalculationType.VOLUME),
                    ComponentWorkItemEntity(componentId = cSouthWall, itemId = 4, itemName = "Plaster", unit = "m²", calculationType = CalculationType.WALL_PLASTER),
                    ComponentWorkItemEntity(componentId = cSouthWall, itemId = 5, itemName = "Putty", unit = "m²", calculationType = CalculationType.AREA),
                    ComponentWorkItemEntity(componentId = cSouthWall, itemId = 8, itemName = "Painting", unit = "m²", calculationType = CalculationType.AREA),

                    // Floor
                    ComponentWorkItemEntity(componentId = cFloor, itemId = 9, itemName = "Waterproofing", unit = "m²", calculationType = CalculationType.AREA),
                    ComponentWorkItemEntity(componentId = cFloor, itemId = 6, itemName = "Flooring", unit = "m²", calculationType = CalculationType.AREA)
                )
            )
        }
    }
}
