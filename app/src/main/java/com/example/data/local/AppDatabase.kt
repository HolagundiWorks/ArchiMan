package com.example.data.local

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import com.example.domain.WorkCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val DATABASE_SCHEMA_VERSION = 18

@Database(
    entities = [
        ClientEntity::class,
        ProjectEntity::class,
        ProjectConsultancyProfileEntity::class,
        ProjectScopeItemEntity::class,
        CompanyProfileEntity::class,
        ProjectTaskEntity::class,
        ProjectSelectionItemEntity::class,
        ProjectScheduleEntity::class,
        MeetingMinutesEntity::class,
        SiteInspectionEntity::class,
        ProjectDrawingEntity::class,
        DrawingRevisionEntity::class,
        DrawingTransmittalEntity::class,
        DrawingTransmittalItemEntity::class,
        DrawingMarkupEntity::class,
        FloorEntity::class,
        RoomEntity::class,
        ComponentEntity::class,
        ComponentWorkItemEntity::class,
        ContractorEntity::class,
        ContractorQualifiedItemEntity::class,
        ProjectContractorCrossRef::class,
        ContractorRateBookEntity::class,
        ContractorRateBookItemEntity::class,
        ProjectRateBookAssignmentEntity::class,
        ItemMasterEntity::class,
        WorkItemAliasEntity::class,
        MeasurementSheetEntity::class,
        MeasurementReviewEventEntity::class,
        MeasurementEntity::class
    ],
    version = DATABASE_SCHEMA_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun projectDao(): ProjectDao
    abstract fun companyProfileDao(): CompanyProfileDao
    abstract fun projectConsultancyDao(): ProjectConsultancyDao
    abstract fun projectTaskDao(): ProjectTaskDao
    abstract fun projectSelectionItemDao(): ProjectSelectionItemDao
    abstract fun projectScheduleDao(): ProjectScheduleDao
    abstract fun meetingMinutesDao(): MeetingMinutesDao
    abstract fun siteInspectionDao(): SiteInspectionDao
    abstract fun drawingDao(): DrawingDao
    abstract fun floorDao(): FloorDao
    abstract fun roomDao(): RoomDao
    abstract fun componentDao(): ComponentDao
    abstract fun componentWorkItemDao(): ComponentWorkItemDao
    abstract fun contractorDao(): ContractorDao
    abstract fun itemMasterDao(): ItemMasterDao
    abstract fun workItemAliasDao(): WorkItemAliasDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun measurementSheetDao(): MeasurementSheetDao
    abstract fun measurementReviewEventDao(): MeasurementReviewEventDao
    abstract fun rateBookDao(): RateBookDao

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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        installSheetLockTriggers(db)
                        installDocumentControlTriggers(db)
                        // Seed predefined items & structure
                        CoroutineScope(Dispatchers.IO).launch {
                            getDatabase(context).seedPredefinedData()
                        }
                    }

                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        installPwdCatalog(db)
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        val PREDEFINED_ITEMS = listOf(
            ItemMasterEntity(name = "Brickwork", unit = "m³", calculationType = CalculationType.VOLUME, isPredefined = true),
            ItemMasterEntity(name = "BrickWork 230mm", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "BrickWork 115mm", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "PCC", unit = "m³", calculationType = CalculationType.VOLUME, isPredefined = true),
            ItemMasterEntity(name = "RCC", unit = "m³", calculationType = CalculationType.VOLUME, isPredefined = true),
            ItemMasterEntity(name = "Slab Concrete", unit = "m³", calculationType = CalculationType.VOLUME, isPredefined = true),
            ItemMasterEntity(name = "Beam Concrete", unit = "m³", calculationType = CalculationType.VOLUME, isPredefined = true),
            ItemMasterEntity(name = "Shuttering & Formwork", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "Plaster 12mm", unit = "m²", calculationType = CalculationType.WALL_PLASTER, isPredefined = true),
            ItemMasterEntity(name = "Plaster 20mm (External)", unit = "m²", calculationType = CalculationType.WALL_PLASTER, isPredefined = true),
            ItemMasterEntity(name = "Putty", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "Flooring", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "Skirting", unit = "m", calculationType = CalculationType.RUNNING_LENGTH, isPredefined = true),
            ItemMasterEntity(name = "Painting", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "Waterproofing", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "Doors/windows", unit = "Nos", calculationType = CalculationType.NOS, isPredefined = true),
            ItemMasterEntity(name = "False Ceiling", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true),
            ItemMasterEntity(name = "KPWD Pre-construction Anti-termite Treatment", unit = "m²", calculationType = CalculationType.AREA, workType = "Anti-termite Treatment", specification = "Create a continuous approved chemical barrier below and around foundations, wall trenches, plinth filling, wall-floor junctions, external perimeter, expansion joints, aprons, pipes and conduits.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "4.1", isPredefined = true),
            ItemMasterEntity(name = "KPWD Size Stone Masonry in CM 1:6", unit = "m³", calculationType = CalculationType.VOLUME, workType = "Stone Masonry", specification = "Hard size-stone masonry in foundation and plinth, laid in cement mortar 1:6 and completed to the specified line, level and workmanship.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "5.4", isPredefined = true),
            ItemMasterEntity(name = "KPWD Laterite Masonry - Foundation CM 1:6", unit = "m³", calculationType = CalculationType.VOLUME, workType = "Stone Masonry", specification = "Laterite size-stone masonry for foundations in cement mortar 1:6; stone to conform to IS 3620 and achieve at least 3.5 N/mm² compressive strength on saturated dry samples.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "5.5", isPredefined = true),
            ItemMasterEntity(name = "KPWD Laterite Masonry - Superstructure CM 1:6", unit = "m³", calculationType = CalculationType.VOLUME, workType = "Stone Masonry", specification = "Laterite size-stone masonry for superstructure in cement mortar 1:6; stone to conform to IS 3620 and achieve at least 3.5 N/mm² compressive strength on saturated dry samples.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "5.7", isPredefined = true),
            ItemMasterEntity(name = "KPWD Brick Masonry - Foundation CM 1:6", unit = "m³", calculationType = CalculationType.VOLUME, workType = "Masonry", specification = "Common burnt-clay non-modular bricks, class designation 3.5, laid in foundation and plinth in cement mortar 1:6, including scaffolding and incidental work.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.2", isPredefined = true),
            ItemMasterEntity(name = "KPWD Brick Masonry - Superstructure CM 1:6", unit = "m³", calculationType = CalculationType.VOLUME, workType = "Masonry", specification = "Common burnt-clay non-modular bricks, class designation 3.5, laid above plinth in all shapes and sizes in cement mortar 1:6, including scaffolding and incidental work.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.8", isPredefined = true),
            ItemMasterEntity(name = "KPWD Half-brick Masonry - Superstructure CM 1:4", unit = "m²", calculationType = CalculationType.AREA, workType = "Masonry", specification = "Half-brick masonry using class 3.5 common burnt-clay non-modular bricks above plinth up to first-floor level in cement mortar 1:4.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.15", isPredefined = true),
            ItemMasterEntity(name = "KPWD Fly-ash Brick Masonry - Superstructure CM 1:6", unit = "m³", calculationType = CalculationType.VOLUME, workType = "Masonry", specification = "Non-modular fly-ash bricks conforming to IS 12894, class designation 5.0, laid above plinth up to first-floor level in cement mortar 1:6.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.25", isPredefined = true),
            ItemMasterEntity(name = "KPWD AAC Block Masonry 100 mm - CM 1:4", unit = "m²", calculationType = CalculationType.AREA, workType = "Masonry", specification = "100 mm AAC block masonry conforming to IS 2185 Part III, above plinth up to first-floor level, laid in cement mortar 1:4.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.26", isPredefined = true),
            ItemMasterEntity(name = "KPWD AAC Block Masonry with RCC Bands", unit = "m²", calculationType = CalculationType.AREA, workType = "Masonry", specification = "150, 230 or 300 mm AAC blocks conforming to IS 2185 Part III, laid with approved polymer-modified adhesive mortar and RCC bands at sill and lintel levels.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.31", isPredefined = true),
            ItemMasterEntity(name = "KPWD Solid Concrete Block Wall 200 mm", unit = "m²", calculationType = CalculationType.AREA, workType = "Masonry", specification = "Load-bearing wall using 400 x 200 x 200 mm solid concrete blocks, density above 1800 kg/m³ and compressive strength at least 4 N/mm², conforming to IS 2185 Part I and laid in CM 1:4 per IS 2572.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.32", isPredefined = true),
            ItemMasterEntity(name = "KPWD Solid Concrete Block Wall 150 mm", unit = "m²", calculationType = CalculationType.AREA, workType = "Masonry", specification = "Load-bearing wall using 400 x 150 x 200 mm solid concrete blocks, density above 1800 kg/m³ and compressive strength at least 4 N/mm², conforming to IS 2185 Part I and laid in CM 1:4 per IS 2572.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.33", isPredefined = true),
            ItemMasterEntity(name = "KPWD Solid Concrete Block Wall 100 mm", unit = "m²", calculationType = CalculationType.AREA, workType = "Masonry", specification = "Wall using 400 x 100 x 200 mm solid concrete blocks, density above 1800 kg/m³ and compressive strength at least 4 N/mm², conforming to IS 2185 Part I and laid in CM 1:4 per IS 2572.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "6.34", isPredefined = true),
            ItemMasterEntity(name = "KPWD PUF Insulated Roofing 30 mm", unit = "m²", calculationType = CalculationType.AREA, workType = "Roofing", specification = "30 mm PUF insulated profiled roofing with 0.5 mm external steel sheet, 0.4 mm powder-coated bottom sheet, minimum 240 MPa steel, zinc coating and sealed waterproof overlaps fixed with suitable self-tapping fasteners.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "7.67.1", isPredefined = true),
            ItemMasterEntity(name = "KPWD Cement Plaster 15 mm - CM 1:6", unit = "m²", calculationType = CalculationType.WALL_PLASTER, workType = "Finishes", specification = "15 mm cement plaster on the rough side of brickwork in cement mortar 1:6, including corner rounding, smooth rendering, scaffolding and curing.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "8.2.2", isPredefined = true),
            ItemMasterEntity(name = "KPWD Cement Plaster 20 mm - CM 1:6", unit = "m²", calculationType = CalculationType.WALL_PLASTER, workType = "Finishes", specification = "20 mm cement plaster to brick or stone masonry in cement mortar 1:6, including corner rounding, smooth rendering, scaffolding and curing.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "8.3.2", isPredefined = true),
            ItemMasterEntity(name = "KPWD Cement Plaster 12 mm - CM 1:4 Neat Finish", unit = "m²", calculationType = CalculationType.WALL_PLASTER, workType = "Finishes", specification = "12 mm cement plaster to brick masonry in cement mortar 1:4, finished with a floating coat of neat cement, including corner rounding, smooth rendering, scaffolding and curing.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "8.4.2", isPredefined = true),
            ItemMasterEntity(name = "KPWD External Granitic Finish", unit = "m²", calculationType = CalculationType.AREA, workType = "Finishes", specification = "Prepare masonry surface, apply approved primer and two coats of granitic finish in the architect-selected shade, followed by two protective tile-guard coats.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "8.83", isPredefined = true),
            ItemMasterEntity(name = "KPWD Decorative Grooved Bamboo Wall Cladding", unit = "m²", calculationType = CalculationType.AREA, workType = "Flooring & Cladding", specification = "10 mm grooved bamboo-faced MDF panels, nominal size 1220 x 2440 mm, graphene-based PU coating, density about 1300 kg/m³, fixed with MS screws or approved adhesive; fire-, UV-, water- and sound-resistant properties required.", sourceName = "Karnataka PWD SR Buildings 2023-24 Vol. 2", sourceItemCode = "10.26", isPredefined = true)
        )

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE contractors ADD COLUMN contractorType TEXT NOT NULL DEFAULT 'Civil'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `item_master_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `calculationType` TEXT NOT NULL, `isPredefined` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `item_master_new` (`id`,`name`,`unit`,`calculationType`,`isPredefined`) SELECT `id`,`name`,`unit`,`calculationType`,`isPredefined` FROM `item_master`")
                db.execSQL("DROP TABLE `item_master`")
                db.execSQL("ALTER TABLE `item_master_new` RENAME TO `item_master`")

                db.execSQL("CREATE TABLE IF NOT EXISTS `contractor_qualified_items_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contractorId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `uom` TEXT NOT NULL, `calculationType` TEXT NOT NULL)")
                db.execSQL("INSERT INTO `contractor_qualified_items_new` (`id`,`contractorId`,`itemName`,`uom`,`calculationType`) SELECT `id`,`contractorId`,`itemName`,`uom`,`calculationType` FROM `contractor_qualified_items`")
                db.execSQL("DROP TABLE `contractor_qualified_items`")
                db.execSQL("ALTER TABLE `contractor_qualified_items_new` RENAME TO `contractor_qualified_items`")

                db.execSQL("CREATE TABLE IF NOT EXISTS `measurements_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `floorId` INTEGER, `roomId` INTEGER, `componentId` INTEGER, `componentWorkItemId` INTEGER, `contractorId` INTEGER NOT NULL, `contractorName` TEXT NOT NULL, `itemId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `unit` TEXT NOT NULL, `calculationType` TEXT NOT NULL, `description` TEXT NOT NULL, `length` REAL NOT NULL, `width` REAL NOT NULL, `height` REAL NOT NULL, `nos` REAL NOT NULL, `deduction` REAL NOT NULL, `quantity` REAL NOT NULL, `floor` TEXT NOT NULL, `location` TEXT NOT NULL, `remarks` TEXT NOT NULL, `photoUri` TEXT, `date` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `measurements_new` (`id`,`projectId`,`floorId`,`roomId`,`componentId`,`componentWorkItemId`,`contractorId`,`contractorName`,`itemId`,`itemName`,`unit`,`calculationType`,`description`,`length`,`width`,`height`,`nos`,`deduction`,`quantity`,`floor`,`location`,`remarks`,`photoUri`,`date`) SELECT `id`,`projectId`,`floorId`,`roomId`,`componentId`,`componentWorkItemId`,`contractorId`,`contractorName`,`itemId`,`itemName`,`unit`,`calculationType`,`description`,`length`,`width`,`height`,`nos`,`deduction`,`quantity`,`floor`,`location`,`remarks`,`photoUri`,`date` FROM `measurements`")
                db.execSQL("DROP TABLE `measurements`")
                db.execSQL("ALTER TABLE `measurements_new` RENAME TO `measurements`")

                db.execSQL("DROP TABLE IF EXISTS `contractor_rates`")
                db.execSQL("DROP TABLE IF EXISTS `bills`")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Point all ID-based references at the oldest canonical item before merging exact name duplicates.
                db.execSQL("UPDATE `component_work_items` SET `itemId` = (SELECT MIN(i2.`id`) FROM `item_master` i2 WHERE LOWER(TRIM(i2.`name`)) = LOWER(TRIM((SELECT i1.`name` FROM `item_master` i1 WHERE i1.`id` = `component_work_items`.`itemId`)))) WHERE EXISTS (SELECT 1 FROM `item_master` i WHERE i.`id` = `component_work_items`.`itemId`)")
                db.execSQL("UPDATE `measurements` SET `itemId` = (SELECT MIN(i2.`id`) FROM `item_master` i2 WHERE LOWER(TRIM(i2.`name`)) = LOWER(TRIM((SELECT i1.`name` FROM `item_master` i1 WHERE i1.`id` = `measurements`.`itemId`)))) WHERE EXISTS (SELECT 1 FROM `item_master` i WHERE i.`id` = `measurements`.`itemId`)")

                val workTypeCase = """CASE
                    WHEN LOWER(`name`) LIKE '%excavat%' OR LOWER(`name`) LIKE '%earth%' OR LOWER(`name`) LIKE '%soil%' THEN 'Earthwork'
                    WHEN LOWER(`name`) LIKE '%rcc%' OR LOWER(`name`) LIKE '%pcc%' OR LOWER(`name`) LIKE '%concrete%' OR LOWER(`name`) LIKE '%reinforcement%' OR LOWER(`name`) LIKE '%shuttering%' THEN 'Concrete & Structure'
                    WHEN LOWER(`name`) LIKE '%brick%' OR LOWER(`name`) LIKE '%block%' OR LOWER(`name`) LIKE '%masonry%' THEN 'Masonry'
                    WHEN LOWER(`name`) LIKE '%plaster%' OR LOWER(`name`) LIKE '%putty%' OR LOWER(`name`) LIKE '%paint%' OR LOWER(`name`) LIKE '%primer%' THEN 'Finishes'
                    WHEN LOWER(`name`) LIKE '%tile%' OR LOWER(`name`) LIKE '%floor%' OR LOWER(`name`) LIKE '%marble%' OR LOWER(`name`) LIKE '%granite%' OR LOWER(`name`) LIKE '%cladding%' OR LOWER(`name`) LIKE '%skirting%' THEN 'Flooring & Cladding'
                    WHEN LOWER(`name`) LIKE '%pipe%' OR LOWER(`name`) LIKE '%drain%' OR LOWER(`name`) LIKE '%trap%' OR LOWER(`name`) LIKE '%basin%' OR LOWER(`name`) LIKE '%fitting%' THEN 'Plumbing Works'
                    WHEN LOWER(`name`) LIKE '%wire%' OR LOWER(`name`) LIKE '%cable%' OR LOWER(`name`) LIKE '%point%' OR LOWER(`name`) LIKE '%switch%' OR LOWER(`name`) LIKE '%earthing%' OR LOWER(`name`) LIKE '%conduit%' THEN 'Electrical Works'
                    WHEN LOWER(`name`) LIKE '%door%' OR LOWER(`name`) LIKE '%window%' OR LOWER(`name`) LIKE '%wardrobe%' OR LOWER(`name`) LIKE '%wood%' THEN 'Joinery & Carpentry'
                    WHEN LOWER(`name`) LIKE '%waterproof%' THEN 'Waterproofing'
                    ELSE 'General Works' END""".replace("\n", " ")

                db.execSQL("CREATE TABLE `item_master_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workType` TEXT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `calculationType` TEXT NOT NULL, `isPredefined` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `item_master_new` (`id`,`workType`,`name`,`unit`,`calculationType`,`isPredefined`) SELECT `id`, $workTypeCase, TRIM(`name`), `unit`, `calculationType`, `isPredefined` FROM `item_master` WHERE `id` IN (SELECT MIN(`id`) FROM `item_master` GROUP BY LOWER(TRIM(`name`)))")
                db.execSQL("DROP TABLE `item_master`")
                db.execSQL("ALTER TABLE `item_master_new` RENAME TO `item_master`")
                db.execSQL("CREATE UNIQUE INDEX `index_item_master_name` ON `item_master` (`name`)")

                val qualifiedWorkTypeCase = workTypeCase.replace("`name`", "`itemName`")
                db.execSQL("CREATE TABLE `contractor_qualified_items_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contractorId` INTEGER NOT NULL, `workType` TEXT NOT NULL, `itemName` TEXT NOT NULL, `uom` TEXT NOT NULL, `calculationType` TEXT NOT NULL)")
                db.execSQL("INSERT INTO `contractor_qualified_items_new` (`id`,`contractorId`,`workType`,`itemName`,`uom`,`calculationType`) SELECT `id`,`contractorId`, $qualifiedWorkTypeCase, TRIM(`itemName`),`uom`,`calculationType` FROM `contractor_qualified_items` WHERE `id` IN (SELECT MIN(`id`) FROM `contractor_qualified_items` GROUP BY `contractorId`, LOWER(TRIM(`itemName`)))")
                db.execSQL("DROP TABLE `contractor_qualified_items`")
                db.execSQL("ALTER TABLE `contractor_qualified_items_new` RENAME TO `contractor_qualified_items`")
                db.execSQL("CREATE UNIQUE INDEX `index_contractor_qualified_items_contractorId_itemName` ON `contractor_qualified_items` (`contractorId`, `itemName`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE `item_master_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemCode` TEXT NOT NULL, `workType` TEXT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT NOT NULL, `calculationType` TEXT NOT NULL, `isPredefined` INTEGER NOT NULL, `isActive` INTEGER NOT NULL)")
                db.execSQL("INSERT INTO `item_master_new` (`id`,`itemCode`,`workType`,`name`,`unit`,`calculationType`,`isPredefined`,`isActive`) SELECT `id`, printf('WI-%06d', `id`), `workType`, `name`, `unit`, `calculationType`, `isPredefined`, 1 FROM `item_master`")
                db.execSQL("DROP TABLE `item_master`")
                db.execSQL("ALTER TABLE `item_master_new` RENAME TO `item_master`")
                db.execSQL("CREATE UNIQUE INDEX `index_item_master_name` ON `item_master` (`name`)")
                db.execSQL("CREATE UNIQUE INDEX `index_item_master_itemCode` ON `item_master` (`itemCode`)")

                db.execSQL("CREATE TABLE IF NOT EXISTS `work_item_aliases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `workItemId` INTEGER NOT NULL, `alias` TEXT NOT NULL, FOREIGN KEY(`workItemId`) REFERENCES `item_master`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX `index_work_item_aliases_workItemId` ON `work_item_aliases` (`workItemId`)")
                db.execSQL("CREATE UNIQUE INDEX `index_work_item_aliases_alias` ON `work_item_aliases` (`alias`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Repair legacy orphan IDs without deleting or rewriting measurement snapshots.
                db.execSQL("UPDATE measurements SET itemId = (SELECT id FROM item_master i WHERE LOWER(TRIM(i.name)) = LOWER(TRIM(measurements.itemName)) LIMIT 1) WHERE NOT EXISTS (SELECT 1 FROM item_master i WHERE i.id = measurements.itemId) AND EXISTS (SELECT 1 FROM item_master i WHERE LOWER(TRIM(i.name)) = LOWER(TRIM(measurements.itemName)))")
                db.execSQL("INSERT OR IGNORE INTO item_master(id,itemCode,workType,name,unit,calculationType,isPredefined,isActive) SELECT itemId, printf('REC-%06d', itemId), 'Recovered Items', itemName, unit, calculationType, 0, 1 FROM measurements m WHERE NOT EXISTS (SELECT 1 FROM item_master i WHERE i.id=m.itemId) GROUP BY itemId")
                db.execSQL("UPDATE measurements SET itemId = (SELECT id FROM item_master i WHERE LOWER(TRIM(i.name)) = LOWER(TRIM(measurements.itemName)) LIMIT 1) WHERE NOT EXISTS (SELECT 1 FROM item_master i WHERE i.id = measurements.itemId) AND EXISTS (SELECT 1 FROM item_master i WHERE LOWER(TRIM(i.name)) = LOWER(TRIM(measurements.itemName)))")
                db.execSQL("INSERT OR IGNORE INTO item_master(id,itemCode,workType,name,unit,calculationType,isPredefined,isActive) SELECT itemId, printf('REC-%06d', itemId), 'Recovered Items', itemName || ' [Recovered ' || itemId || ']', unit, calculationType, 0, 1 FROM measurements m WHERE NOT EXISTS (SELECT 1 FROM item_master i WHERE i.id=m.itemId) GROUP BY itemId")
                db.execSQL("UPDATE measurements SET contractorId = (SELECT id FROM contractors c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(measurements.contractorName)) LIMIT 1) WHERE NOT EXISTS (SELECT 1 FROM contractors c WHERE c.id=measurements.contractorId) AND EXISTS (SELECT 1 FROM contractors c WHERE LOWER(TRIM(c.name)) = LOWER(TRIM(measurements.contractorName)))")
                db.execSQL("INSERT OR IGNORE INTO contractors(id,projectId,name,address,contactNo,phone,contractorType,createdAt) SELECT contractorId, 0, contractorName, '', '', '', 'Recovered', MIN(date) FROM measurements m WHERE NOT EXISTS (SELECT 1 FROM contractors c WHERE c.id=m.contractorId) GROUP BY contractorId")
                db.execSQL("INSERT OR IGNORE INTO projects(id,name,client,clientId,siteLocation,createdAt) SELECT projectId, 'Recovered Project #' || projectId, '', 0, '', MIN(date) FROM measurements m WHERE NOT EXISTS (SELECT 1 FROM projects p WHERE p.id=m.projectId) GROUP BY projectId")
                db.execSQL("UPDATE measurements SET floorId=NULL WHERE floorId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM floors f WHERE f.id=measurements.floorId)")
                db.execSQL("UPDATE measurements SET roomId=NULL WHERE roomId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM rooms r WHERE r.id=measurements.roomId)")
                db.execSQL("UPDATE measurements SET componentId=NULL WHERE componentId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM components c WHERE c.id=measurements.componentId)")
                db.execSQL("UPDATE measurements SET componentWorkItemId=NULL WHERE componentWorkItemId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM component_work_items c WHERE c.id=measurements.componentWorkItemId)")

                db.execSQL("CREATE TABLE measurements_new (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `floorId` INTEGER, `roomId` INTEGER, `componentId` INTEGER, `componentWorkItemId` INTEGER, `contractorId` INTEGER NOT NULL, `contractorName` TEXT NOT NULL, `itemId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `unit` TEXT NOT NULL, `calculationType` TEXT NOT NULL, `description` TEXT NOT NULL, `length` REAL NOT NULL, `width` REAL NOT NULL, `height` REAL NOT NULL, `nos` REAL NOT NULL, `deduction` REAL NOT NULL, `quantity` REAL NOT NULL, `floor` TEXT NOT NULL, `location` TEXT NOT NULL, `remarks` TEXT NOT NULL, `photoUri` TEXT, `date` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`floorId`) REFERENCES `floors`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`roomId`) REFERENCES `rooms`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`componentId`) REFERENCES `components`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`componentWorkItemId`) REFERENCES `component_work_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`contractorId`) REFERENCES `contractors`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`itemId`) REFERENCES `item_master`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("INSERT INTO measurements_new SELECT * FROM measurements")
                db.execSQL("DROP TABLE measurements")
                db.execSQL("ALTER TABLE measurements_new RENAME TO measurements")
                listOf("projectId", "floorId", "roomId", "componentId", "componentWorkItemId", "contractorId", "itemId").forEach { column ->
                    db.execSQL("CREATE INDEX index_measurements_$column ON measurements($column)")
                }
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE measurement_sheets (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sheetCode` TEXT NOT NULL, `projectId` INTEGER NOT NULL, `floorId` INTEGER, `floorNameSnapshot` TEXT NOT NULL, `contractorId` INTEGER NOT NULL, `contractorNameSnapshot` TEXT NOT NULL, `itemId` INTEGER NOT NULL, `itemNameSnapshot` TEXT NOT NULL, `uomSnapshot` TEXT NOT NULL, `formulaCode` TEXT NOT NULL, `formulaVersion` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`floorId`) REFERENCES `floors`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`contractorId`) REFERENCES `contractors`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`itemId`) REFERENCES `item_master`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION)")
                db.execSQL("INSERT INTO measurement_sheets SELECT id, printf('MB-LEG-%06d', id), projectId, floorId, floor, contractorId, contractorName, itemId, itemName, unit, calculationType, 1, 'RECORDED', date, date FROM measurements")
                listOf("projectId", "floorId", "contractorId", "itemId").forEach { column ->
                    db.execSQL("CREATE INDEX index_measurement_sheets_$column ON measurement_sheets($column)")
                }
                db.execSQL("CREATE UNIQUE INDEX index_measurement_sheets_sheetCode ON measurement_sheets(sheetCode)")

                db.execSQL("CREATE TABLE measurements_new (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sheetId` INTEGER NOT NULL, `projectId` INTEGER NOT NULL, `floorId` INTEGER, `roomId` INTEGER, `componentId` INTEGER, `componentWorkItemId` INTEGER, `contractorId` INTEGER NOT NULL, `contractorName` TEXT NOT NULL, `itemId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `unit` TEXT NOT NULL, `calculationType` TEXT NOT NULL, `formulaCode` TEXT NOT NULL, `formulaVersion` INTEGER NOT NULL, `description` TEXT NOT NULL, `length` REAL NOT NULL, `width` REAL NOT NULL, `height` REAL NOT NULL, `nos` REAL NOT NULL, `deduction` REAL NOT NULL, `quantity` REAL NOT NULL, `floor` TEXT NOT NULL, `location` TEXT NOT NULL, `remarks` TEXT NOT NULL, `photoUri` TEXT, `date` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`floorId`) REFERENCES `floors`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`roomId`) REFERENCES `rooms`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`componentId`) REFERENCES `components`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`componentWorkItemId`) REFERENCES `component_work_items`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, FOREIGN KEY(`contractorId`) REFERENCES `contractors`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`itemId`) REFERENCES `item_master`(`id`) ON UPDATE NO ACTION ON DELETE NO ACTION, FOREIGN KEY(`sheetId`) REFERENCES `measurement_sheets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("INSERT INTO measurements_new (`id`,`sheetId`,`projectId`,`floorId`,`roomId`,`componentId`,`componentWorkItemId`,`contractorId`,`contractorName`,`itemId`,`itemName`,`unit`,`calculationType`,`formulaCode`,`formulaVersion`,`description`,`length`,`width`,`height`,`nos`,`deduction`,`quantity`,`floor`,`location`,`remarks`,`photoUri`,`date`) SELECT `id`,`id`,`projectId`,`floorId`,`roomId`,`componentId`,`componentWorkItemId`,`contractorId`,`contractorName`,`itemId`,`itemName`,`unit`,`calculationType`,`calculationType`,1,`description`,`length`,`width`,`height`,`nos`,`deduction`,`quantity`,`floor`,`location`,`remarks`,`photoUri`,`date` FROM measurements")
                db.execSQL("DROP TABLE measurements")
                db.execSQL("ALTER TABLE measurements_new RENAME TO measurements")
                listOf("sheetId", "projectId", "floorId", "roomId", "componentId", "componentWorkItemId", "contractorId", "itemId").forEach { column ->
                    db.execSQL("CREATE INDEX index_measurements_$column ON measurements($column)")
                }
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE measurement_sheets SET status='DRAFT' WHERE status='RECORDED'")
                db.execSQL("ALTER TABLE measurement_sheets ADD COLUMN revision INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE measurement_sheets ADD COLUMN lockedAt INTEGER")
                db.execSQL("ALTER TABLE measurement_sheets ADD COLUMN archivedAt INTEGER")
                db.execSQL("CREATE TABLE measurement_review_events (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sheetId` INTEGER NOT NULL, `fromStatus` TEXT NOT NULL, `toStatus` TEXT NOT NULL, `comment` TEXT NOT NULL, `actor` TEXT NOT NULL, `revision` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`sheetId`) REFERENCES `measurement_sheets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX index_measurement_review_events_sheetId ON measurement_review_events(sheetId)")
                installSheetLockTriggers(db)
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS project_tasks (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `dueDate` INTEGER, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_project_tasks_projectId ON project_tasks(projectId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS project_selection_items (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `itemName` TEXT NOT NULL, `specification` TEXT NOT NULL, `makeOrBrand` TEXT NOT NULL, `quantity` REAL NOT NULL, `unit` TEXT NOT NULL, `status` TEXT NOT NULL, `remarks` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_project_selection_items_projectId ON project_selection_items(projectId)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE item_master ADD COLUMN specification TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE item_master ADD COLUMN sourceName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE item_master ADD COLUMN sourceItemCode TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) = installPwdCatalog(db)
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE contractor_rate_books (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `contractorId` INTEGER NOT NULL, `name` TEXT NOT NULL, `version` INTEGER NOT NULL, `status` TEXT NOT NULL, `effectiveFrom` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_contractor_rate_books_contractorId ON contractor_rate_books(contractorId)")
                db.execSQL("CREATE TABLE contractor_rate_book_items (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rateBookId` INTEGER NOT NULL, `itemId` INTEGER NOT NULL, `itemNameSnapshot` TEXT NOT NULL, `uomSnapshot` TEXT NOT NULL, `specificationSnapshot` TEXT NOT NULL, `sourceItemCodeSnapshot` TEXT NOT NULL, `rate` REAL NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_contractor_rate_book_items_rateBookId ON contractor_rate_book_items(rateBookId)")
                db.execSQL("CREATE INDEX index_contractor_rate_book_items_itemId ON contractor_rate_book_items(itemId)")
                db.execSQL("CREATE UNIQUE INDEX index_contractor_rate_book_items_rateBookId_itemId ON contractor_rate_book_items(rateBookId,itemId)")
                db.execSQL("CREATE TABLE project_rate_book_assignments (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `contractorId` INTEGER NOT NULL, `rateBookId` INTEGER NOT NULL, `assignedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_project_rate_book_assignments_projectId ON project_rate_book_assignments(projectId)")
                db.execSQL("CREATE INDEX index_project_rate_book_assignments_contractorId ON project_rate_book_assignments(contractorId)")
                db.execSQL("CREATE INDEX index_project_rate_book_assignments_rateBookId ON project_rate_book_assignments(rateBookId)")
                db.execSQL("CREATE UNIQUE INDEX index_project_rate_book_assignments_projectId_contractorId ON project_rate_book_assignments(projectId,contractorId)")
                db.execSQL("ALTER TABLE measurements ADD COLUMN appliedRateBookId INTEGER")
                db.execSQL("ALTER TABLE measurements ADD COLUMN rateSnapshot REAL")
                db.execSQL("ALTER TABLE measurements ADD COLUMN amountSnapshot REAL")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE project_schedules (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `scheduledAt` INTEGER NOT NULL, `location` TEXT NOT NULL, `notes` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_project_schedules_projectId ON project_schedules(projectId)")
                db.execSQL("CREATE INDEX index_project_schedules_scheduledAt ON project_schedules(scheduledAt)")
                db.execSQL("CREATE TABLE meeting_minutes (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `meetingAt` INTEGER NOT NULL, `location` TEXT NOT NULL, `attendees` TEXT NOT NULL, `discussion` TEXT NOT NULL, `decisions` TEXT NOT NULL, `actionItems` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_meeting_minutes_projectId ON meeting_minutes(projectId)")
                db.execSQL("CREATE INDEX index_meeting_minutes_meetingAt ON meeting_minutes(meetingAt)")
                db.execSQL("CREATE TABLE site_inspections (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `inspectionAt` INTEGER NOT NULL, `location` TEXT NOT NULL, `inspector` TEXT NOT NULL, `observation` TEXT NOT NULL, `severity` TEXT NOT NULL, `correctiveAction` TEXT NOT NULL, `dueAt` INTEGER, `status` TEXT NOT NULL, `photoUri` TEXT, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_site_inspections_projectId ON site_inspections(projectId)")
                db.execSQL("CREATE INDEX index_site_inspections_inspectionAt ON site_inspections(inspectionAt)")
                db.execSQL("CREATE INDEX index_site_inspections_status ON site_inspections(status)")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE project_drawings (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `drawingNumber` TEXT NOT NULL, `title` TEXT NOT NULL, `discipline` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `archivedAt` INTEGER)")
                db.execSQL("CREATE INDEX index_project_drawings_projectId ON project_drawings(projectId)")
                db.execSQL("CREATE UNIQUE INDEX index_project_drawings_projectId_drawingNumber ON project_drawings(projectId,drawingNumber)")
                db.execSQL("CREATE TABLE drawing_revisions (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `drawingId` INTEGER NOT NULL, `revisionCode` TEXT NOT NULL, `fileName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `fileUri` TEXT NOT NULL, `fileChecksum` TEXT NOT NULL, `issueStatus` TEXT NOT NULL, `revisionNotes` TEXT NOT NULL, `isAsBuilt` INTEGER NOT NULL, `issuedAt` INTEGER, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_drawing_revisions_projectId ON drawing_revisions(projectId)")
                db.execSQL("CREATE INDEX index_drawing_revisions_drawingId ON drawing_revisions(drawingId)")
                db.execSQL("CREATE UNIQUE INDEX index_drawing_revisions_drawingId_revisionCode ON drawing_revisions(drawingId,revisionCode)")
                db.execSQL("CREATE TABLE drawing_transmittals (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `transmittalNumber` TEXT NOT NULL, `subject` TEXT NOT NULL, `recipients` TEXT NOT NULL, `purpose` TEXT NOT NULL, `notes` TEXT NOT NULL, `issuedAt` INTEGER NOT NULL, `acknowledgedAt` INTEGER)")
                db.execSQL("CREATE INDEX index_drawing_transmittals_projectId ON drawing_transmittals(projectId)")
                db.execSQL("CREATE UNIQUE INDEX index_drawing_transmittals_projectId_transmittalNumber ON drawing_transmittals(projectId,transmittalNumber)")
                db.execSQL("CREATE TABLE drawing_transmittal_items (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transmittalId` INTEGER NOT NULL, `drawingRevisionId` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_drawing_transmittal_items_transmittalId ON drawing_transmittal_items(transmittalId)")
                db.execSQL("CREATE INDEX index_drawing_transmittal_items_drawingRevisionId ON drawing_transmittal_items(drawingRevisionId)")
                db.execSQL("CREATE UNIQUE INDEX index_drawing_transmittal_items_transmittalId_drawingRevisionId ON drawing_transmittal_items(transmittalId,drawingRevisionId)")
                db.execSQL("CREATE TABLE drawing_markups (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `drawingRevisionId` INTEGER NOT NULL, `markupType` TEXT NOT NULL, `geometryJson` TEXT NOT NULL, `styleJson` TEXT NOT NULL, `measurementValue` REAL, `measurementUnit` TEXT, `calibrationJson` TEXT, `authorId` INTEGER, `createdAt` INTEGER NOT NULL, `deletedAt` INTEGER)")
                db.execSQL("CREATE INDEX index_drawing_markups_drawingRevisionId ON drawing_markups(drawingRevisionId)")
                db.execSQL("CREATE INDEX index_drawing_markups_createdAt ON drawing_markups(createdAt)")
                installDocumentControlTriggers(db)
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN projectCode TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN projectType TEXT NOT NULL DEFAULT 'Residential'")
                db.execSQL("ALTER TABLE projects ADD COLUMN status TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE projects ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN architectInCharge TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE projects ADD COLUMN startDate INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN targetCompletionDate INTEGER")
                db.execSQL("ALTER TABLE projects ADD COLUMN plotArea REAL")
                db.execSQL("ALTER TABLE projects ADD COLUMN builtUpArea REAL")
                db.execSQL("ALTER TABLE projects ADD COLUMN areaUnit TEXT NOT NULL DEFAULT 'm²'")
                db.execSQL("CREATE TABLE company_profile (`id` INTEGER NOT NULL, `practiceName` TEXT NOT NULL, `legalName` TEXT NOT NULL, `address` TEXT NOT NULL, `city` TEXT NOT NULL, `state` TEXT NOT NULL, `pinCode` TEXT NOT NULL, `phone` TEXT NOT NULL, `email` TEXT NOT NULL, `website` TEXT NOT NULL, `gstin` TEXT NOT NULL, `coaRegistrationNumber` TEXT NOT NULL, `logoUri` TEXT, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE project_consultancy_profiles (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `consultancyTypes` TEXT NOT NULL, `currentPhase` TEXT NOT NULL, `currentDesignStage` TEXT NOT NULL, `briefStatus` TEXT NOT NULL, `clientObjectives` TEXT NOT NULL, `projectRequirements` TEXT NOT NULL, `designPreferences` TEXT NOT NULL, `siteConstraints` TEXT NOT NULL, `clarifications` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX index_project_consultancy_profiles_projectId ON project_consultancy_profiles(projectId)")
                db.execSQL("CREATE TABLE project_scope_items (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `category` TEXT NOT NULL, `title` TEXT NOT NULL, `details` TEXT NOT NULL, `status` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX index_project_scope_items_projectId ON project_scope_items(projectId)")
                db.execSQL("CREATE INDEX index_project_scope_items_projectId_category ON project_scope_items(projectId,category)")
            }
        }

        private fun installPwdCatalog(db: SupportSQLiteDatabase) {
            PREDEFINED_ITEMS.filter { it.sourceName.isNotBlank() }.forEach { item ->
                val workType = item.workType.ifBlank { WorkCatalog.classify("Civil", item.name) }
                val values = ContentValues().apply {
                    put("itemCode", WorkCatalog.codeFor(workType, item.name))
                    put("workType", workType)
                    put("name", item.name)
                    put("unit", item.unit)
                    put("calculationType", item.calculationType.name)
                    put("specification", item.specification)
                    put("sourceName", item.sourceName)
                    put("sourceItemCode", item.sourceItemCode)
                    put("isPredefined", 1)
                    put("isActive", 1)
                }
                db.insert("item_master", SQLiteDatabase.CONFLICT_IGNORE, values)
            }
        }

        private fun installSheetLockTriggers(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TRIGGER IF NOT EXISTS lock_approved_measurement_update BEFORE UPDATE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS lock_approved_measurement_delete BEFORE DELETE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_review_event_update BEFORE UPDATE ON measurement_review_events BEGIN SELECT RAISE(ABORT, 'Review events are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_review_event_delete BEFORE DELETE ON measurement_review_events BEGIN SELECT RAISE(ABORT, 'Review events are immutable'); END")
        }

        private fun installDocumentControlTriggers(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_drawing_revision_update BEFORE UPDATE ON drawing_revisions BEGIN SELECT RAISE(ABORT, 'Drawing revisions are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_drawing_revision_delete BEFORE DELETE ON drawing_revisions BEGIN SELECT RAISE(ABORT, 'Drawing revisions are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_transmittal_item_update BEFORE UPDATE ON drawing_transmittal_items BEGIN SELECT RAISE(ABORT, 'Issued transmittal contents are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_transmittal_item_delete BEFORE DELETE ON drawing_transmittal_items BEGIN SELECT RAISE(ABORT, 'Issued transmittal contents are immutable'); END")
        }
    }

    suspend fun seedPredefinedData() {
        val preparedItems = PREDEFINED_ITEMS.map {
            val workType = if (it.workType.isNotBlank() && it.sourceName.isNotBlank()) it.workType else WorkCatalog.classify("Civil", it.name)
            it.copy(workType = workType, itemCode = WorkCatalog.codeFor(workType, it.name))
        }
        val isNewDatabase = itemMasterDao().getCount() == 0
        // Add newly bundled standards to existing databases without replacing user-edited items.
        itemMasterDao().insertAll(if (isNewDatabase) preparedItems else preparedItems.filter { it.sourceName.isNotBlank() })
        if (isNewDatabase) {
            
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
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "BrickWork 230mm", uom = "m²", calculationType = CalculationType.AREA),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "BrickWork 115mm", uom = "m²", calculationType = CalculationType.AREA),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "Plaster 12mm", uom = "m²", calculationType = CalculationType.WALL_PLASTER),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "Slab Concrete", uom = "m³", calculationType = CalculationType.VOLUME),
                    ContractorQualifiedItemEntity(contractorId = contId1, itemName = "Beam Concrete", uom = "m³", calculationType = CalculationType.VOLUME)
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
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Plaster 12mm", uom = "m²", calculationType = CalculationType.WALL_PLASTER),
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Plaster 20mm (External)", uom = "m²", calculationType = CalculationType.WALL_PLASTER),
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Putty", uom = "m²", calculationType = CalculationType.AREA),
                    ContractorQualifiedItemEntity(contractorId = contId2, itemName = "Painting", uom = "m²", calculationType = CalculationType.AREA)
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

            // Seed Floors: Ground Floor (lvl0), First Floor (lvl1), Second Floor (lvl2)
            val gfId = floorDao().insertFloor(FloorEntity(projectId = projId, name = "Level 0 (Ground Floor)", orderIndex = 0))
            val ffId = floorDao().insertFloor(FloorEntity(projectId = projId, name = "Level 1 (First Floor)", orderIndex = 1))
            val sfId = floorDao().insertFloor(FloorEntity(projectId = projId, name = "Level 2 (Second Floor)", orderIndex = 2))

            // Seed sample Brickwork measurements on Level 0 for Sharma Civil Works
            insertSeedMeasurement(
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
                    floor = "Level 0 (Ground Floor)",
                    location = "Grid A-D Exterior",
                    remarks = "230mm Red Clay Brick masonry"
                )
            )
            insertSeedMeasurement(
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
                    floor = "Level 0 (Ground Floor)",
                    location = "Living Hall",
                    remarks = "Partition wall"
                )
            )
            insertSeedMeasurement(
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

    private suspend fun insertSeedMeasurement(measurement: MeasurementEntity): Long = withTransaction {
        val sheetId = measurementSheetDao().insert(
            MeasurementSheetEntity(
                sheetCode = "MB-SEED-${measurement.date}-${java.util.UUID.randomUUID().toString().take(8).uppercase()}",
                projectId = measurement.projectId,
                floorId = measurement.floorId,
                floorNameSnapshot = measurement.floor,
                contractorId = measurement.contractorId,
                contractorNameSnapshot = measurement.contractorName,
                itemId = measurement.itemId,
                itemNameSnapshot = measurement.itemName,
                uomSnapshot = measurement.unit,
                formulaCode = measurement.calculationType.name,
                formulaVersion = 1,
                createdAt = measurement.date,
                updatedAt = measurement.date
            )
        )
        measurementDao().insertMeasurement(measurement.copy(sheetId = sheetId, formulaCode = measurement.calculationType.name, formulaVersion = 1))
    }
}
