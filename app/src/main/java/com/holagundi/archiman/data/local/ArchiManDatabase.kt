package com.holagundi.archiman.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.holagundi.archiman.data.local.entity.ClientEntity
import com.holagundi.archiman.data.local.entity.CompanyProfileEntity
import com.holagundi.archiman.data.local.entity.ComponentEntity
import com.holagundi.archiman.data.local.entity.ComponentWorkItemEntity
import com.holagundi.archiman.data.local.entity.ContractorEntity
import com.holagundi.archiman.data.local.entity.ContractorQualifiedItemEntity
import com.holagundi.archiman.data.local.entity.CoordinationEventEntity
import com.holagundi.archiman.data.local.entity.CoordinationItemEntity
import com.holagundi.archiman.data.local.entity.DailySiteReportEntity
import com.holagundi.archiman.data.local.entity.DrawingRevisionEntity
import com.holagundi.archiman.data.local.entity.DrawingTransmittalEntity
import com.holagundi.archiman.data.local.entity.DrawingTransmittalRevisionEntity
import com.holagundi.archiman.data.local.entity.EnquiryEntity
import com.holagundi.archiman.data.local.entity.FeeProposalEntity
import com.holagundi.archiman.data.local.entity.FloorEntity
import com.holagundi.archiman.data.local.entity.InvoiceEntity
import com.holagundi.archiman.data.local.entity.ItemLinkRuleEntity
import com.holagundi.archiman.data.local.entity.ItemMasterEntity
import com.holagundi.archiman.data.local.entity.LetterEntity
import com.holagundi.archiman.data.local.entity.LocalUserEntity
import com.holagundi.archiman.data.local.entity.MaterialOrderEntity
import com.holagundi.archiman.data.local.entity.MaterialOrderLineEntity
import com.holagundi.archiman.data.local.entity.MeasurementEntity
import com.holagundi.archiman.data.local.entity.MeasurementReviewEventEntity
import com.holagundi.archiman.data.local.entity.MeasurementSheetEntity
import com.holagundi.archiman.data.local.entity.MeetingMinutesEntity
import com.holagundi.archiman.data.local.entity.PermitEntity
import com.holagundi.archiman.data.local.entity.PortalAuditEventEntity
import com.holagundi.archiman.data.local.entity.ProjectApprovalEntity
import com.holagundi.archiman.data.local.entity.ProjectBacklogEntity
import com.holagundi.archiman.data.local.entity.ProjectConsultancyProfileEntity
import com.holagundi.archiman.data.local.entity.ProjectConsultantEntity
import com.holagundi.archiman.data.local.entity.ProjectContractorCrossRef
import com.holagundi.archiman.data.local.entity.ProjectDecisionEntity
import com.holagundi.archiman.data.local.entity.ProjectDrawingEntity
import com.holagundi.archiman.data.local.entity.ProjectEntity
import com.holagundi.archiman.data.local.entity.ProjectOnboardingResponseEntity
import com.holagundi.archiman.data.local.entity.ProjectScheduleEntity
import com.holagundi.archiman.data.local.entity.ProjectSettingsEntity
import com.holagundi.archiman.data.local.entity.ProjectStageEntity
import com.holagundi.archiman.data.local.entity.ProjectScopeItemEntity
import com.holagundi.archiman.data.local.entity.RateBookItemEntity
import com.holagundi.archiman.data.local.entity.RccMemberEntity
import com.holagundi.archiman.data.local.entity.ReceiptEntity
import com.holagundi.archiman.data.local.entity.ProjectTaskEntity
import com.holagundi.archiman.data.local.entity.ProjectSelectionItemEntity
import com.holagundi.archiman.data.local.entity.ScheduleActivityEntity
import com.holagundi.archiman.data.local.entity.RoomEntity
import com.holagundi.archiman.data.local.entity.TimeEntryEntity
import com.holagundi.archiman.data.local.entity.SiteDocumentEntity
import com.holagundi.archiman.data.local.entity.SiteInspectionEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEntity
import com.holagundi.archiman.data.local.entity.SiteIssueEventEntity
import com.holagundi.archiman.data.local.entity.WorkContractEntity
import com.holagundi.archiman.data.local.entity.WorkItemAliasEntity
import com.holagundi.archiman.domain.SchemaContract

/**
 * Schema 28 is created in full for a new install.
 * [ArchiManMigrations.ALL] must contain a preserving migration from 27.
 * Do not call fallbackToDestructiveMigration.
 */
object ArchiManMigrations {
    const val VERSION = SchemaContract.VERSION
    private val MIGRATION_27_28 = object : Migration(27, 28) {
        override fun migrate(db: SupportSQLiteDatabase) {
            statements.forEach(db::execSQL)
        }
    }
    val MIGRATION_28_29 = object : Migration(28, 29) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf("fee_proposals", "work_contracts").forEach { table ->
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `detailsJson` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `documentText` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
    val MIGRATION_29_30 = object : Migration(29, 30) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE invoices ADD COLUMN taxJson TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE receipts ADD COLUMN tdsAmount REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE receipts ADD COLUMN tdsReference TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE receipts ADD COLUMN tdsVerified INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE receipts ADD COLUMN bankKey TEXT NOT NULL DEFAULT ''")
            db.execSQL("CREATE TABLE IF NOT EXISTS finance_records (`key` TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, json TEXT NOT NULL)")
        }
    }
    val MIGRATION_30_31 = object : Migration(30, 31) {
        override fun migrate(db: SupportSQLiteDatabase) {
            listOf("clients", "contractors").forEach { db.execSQL("ALTER TABLE `$it` ADD COLUMN detailsJson TEXT NOT NULL DEFAULT ''") }
            listOf("measurement_sheets", "measurements").forEach {
                db.execSQL("ALTER TABLE `$it` ADD COLUMN boqRevision TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `$it` ADD COLUMN boqLineKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `$it` ADD COLUMN rateSnapshot REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `$it` ADD COLUMN contractBasis TEXT NOT NULL DEFAULT ''")
            }
        }
    }
    val MIGRATION_31_32 = object : Migration(31, 32) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_projects_status` ON `projects` (`status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_project_tasks_projectId_status` ON `project_tasks` (`projectId`, `status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_site_issues_projectId_status` ON `site_issues` (`projectId`, `status`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_measurement_sheets_projectId_status` ON `measurement_sheets` (`projectId`, `status`)")
        }
    }
    val ALL: Array<Migration> = arrayOf(MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32)

    private val statements = listOf(
        "CREATE TABLE IF NOT EXISTS `project_settings` (`projectId` INTEGER NOT NULL, `persona` TEXT NOT NULL, `consultancyType` TEXT NOT NULL, PRIMARY KEY(`projectId`), FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE TABLE IF NOT EXISTS `project_stages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `feeAmount` REAL NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_project_stages_projectId` ON `project_stages` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `invoices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `stageId` INTEGER, `number` TEXT NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_invoices_projectId` ON `invoices` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `receipts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceId` INTEGER NOT NULL, `projectId` INTEGER NOT NULL, `amount` REAL NOT NULL, `note` TEXT NOT NULL, `receivedAt` INTEGER NOT NULL, FOREIGN KEY(`invoiceId`) REFERENCES `invoices`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_receipts_invoiceId` ON `receipts` (`invoiceId`)",
        "CREATE INDEX IF NOT EXISTS `index_receipts_projectId` ON `receipts` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `enquiries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `clientName` TEXT NOT NULL, `source` TEXT NOT NULL, `notes` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `permits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `authority` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_permits_projectId` ON `permits` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `time_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `person` TEXT NOT NULL, `minutes` INTEGER NOT NULL, `notes` TEXT NOT NULL, `workDate` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_time_entries_projectId` ON `time_entries` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `letters` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER, `subject` TEXT NOT NULL, `body` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS `index_letters_projectId` ON `letters` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `fee_proposals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `amount` REAL NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_fee_proposals_projectId` ON `fee_proposals` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `rcc_members` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `elementType` TEXT NOT NULL, `mark` TEXT NOT NULL, `nos` REAL NOT NULL, `length` REAL NOT NULL, `breadth` REAL NOT NULL, `height` REAL NOT NULL, `quantity` REAL NOT NULL, `remarks` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_rcc_members_projectId` ON `rcc_members` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `rate_book_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `itemName` TEXT NOT NULL, `unit` TEXT NOT NULL, `rate` REAL NOT NULL, `sourceName` TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `work_contracts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `party` TEXT NOT NULL, `kind` TEXT NOT NULL, `amount` REAL NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_work_contracts_projectId` ON `work_contracts` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `schedule_activities` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `title` TEXT NOT NULL, `progress` INTEGER NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE INDEX IF NOT EXISTS `index_schedule_activities_projectId` ON `schedule_activities` (`projectId`)",
        "CREATE TABLE IF NOT EXISTS `item_link_rules` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sourceTrade` TEXT NOT NULL, `targetTrade` TEXT NOT NULL, `factor` REAL NOT NULL, `note` TEXT NOT NULL)",
    )
}

@Database(
    entities = [
        CompanyProfileEntity::class,
        ClientEntity::class,
        LocalUserEntity::class,
        PortalAuditEventEntity::class,
        ProjectEntity::class,
        ProjectConsultancyProfileEntity::class,
        ProjectScopeItemEntity::class,
        ProjectOnboardingResponseEntity::class,
        ProjectApprovalEntity::class,
        ProjectBacklogEntity::class,
        FloorEntity::class,
        RoomEntity::class,
        ComponentEntity::class,
        ComponentWorkItemEntity::class,
        ContractorEntity::class,
        ContractorQualifiedItemEntity::class,
        ProjectContractorCrossRef::class,
        ItemMasterEntity::class,
        WorkItemAliasEntity::class,
        ProjectTaskEntity::class,
        ProjectSelectionItemEntity::class,
        ProjectScheduleEntity::class,
        MeetingMinutesEntity::class,
        SiteInspectionEntity::class,
        DailySiteReportEntity::class,
        ProjectDecisionEntity::class,
        SiteIssueEntity::class,
        SiteIssueEventEntity::class,
        ProjectConsultantEntity::class,
        CoordinationItemEntity::class,
        CoordinationEventEntity::class,
        ProjectDrawingEntity::class,
        DrawingRevisionEntity::class,
        DrawingTransmittalEntity::class,
        DrawingTransmittalRevisionEntity::class,
        SiteDocumentEntity::class,
        MaterialOrderEntity::class,
        MaterialOrderLineEntity::class,
        MeasurementSheetEntity::class,
        MeasurementReviewEventEntity::class,
        MeasurementEntity::class,
        ProjectSettingsEntity::class,
        ProjectStageEntity::class,
        InvoiceEntity::class,
        ReceiptEntity::class,
        com.holagundi.archiman.data.local.entity.FinanceRecordEntity::class,
        EnquiryEntity::class,
        PermitEntity::class,
        TimeEntryEntity::class,
        LetterEntity::class,
        FeeProposalEntity::class,
        RccMemberEntity::class,
        RateBookItemEntity::class,
        WorkContractEntity::class,
        ScheduleActivityEntity::class,
        ItemLinkRuleEntity::class,
    ],
    version = ArchiManMigrations.VERSION,
    exportSchema = true,
)
abstract class ArchiManDatabase : RoomDatabase() {
    abstract fun practiceDao(): PracticeDao
    abstract fun projectDao(): ProjectDao
    abstract fun catalogDao(): CatalogDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun fieldDao(): FieldDao
    abstract fun suiteDao(): SuiteDao
}
