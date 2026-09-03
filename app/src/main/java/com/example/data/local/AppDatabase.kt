package com.example.data.local

import android.content.Context
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

const val DATABASE_SCHEMA_VERSION = 11

@Database(
    entities = [
        ClientEntity::class,
        ProjectEntity::class,
        ProjectTaskEntity::class,
        ProjectSelectionItemEntity::class,
        FloorEntity::class,
        RoomEntity::class,
        ComponentEntity::class,
        ComponentWorkItemEntity::class,
        ContractorEntity::class,
        ContractorQualifiedItemEntity::class,
        ProjectContractorCrossRef::class,
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
    abstract fun projectTaskDao(): ProjectTaskDao
    abstract fun projectSelectionItemDao(): ProjectSelectionItemDao
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
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        installSheetLockTriggers(db)
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
            ItemMasterEntity(name = "False Ceiling", unit = "m²", calculationType = CalculationType.AREA, isPredefined = true)
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

        private fun installSheetLockTriggers(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TRIGGER IF NOT EXISTS lock_approved_measurement_update BEFORE UPDATE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS lock_approved_measurement_delete BEFORE DELETE ON measurements WHEN (SELECT status FROM measurement_sheets WHERE id=OLD.sheetId)='APPROVED' BEGIN SELECT RAISE(ABORT, 'Approved measurement sheets are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_review_event_update BEFORE UPDATE ON measurement_review_events BEGIN SELECT RAISE(ABORT, 'Review events are immutable'); END")
            db.execSQL("CREATE TRIGGER IF NOT EXISTS immutable_review_event_delete BEFORE DELETE ON measurement_review_events BEGIN SELECT RAISE(ABORT, 'Review events are immutable'); END")
        }
    }

    suspend fun seedPredefinedData() {
        if (itemMasterDao().getCount() == 0) {
            itemMasterDao().insertAll(PREDEFINED_ITEMS.map {
                val workType = WorkCatalog.classify("Civil", it.name)
                it.copy(workType = workType, itemCode = WorkCatalog.codeFor(workType, it.name))
            })
            
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
