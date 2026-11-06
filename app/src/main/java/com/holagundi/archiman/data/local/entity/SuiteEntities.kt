package com.holagundi.archiman.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_settings",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ProjectSettingsEntity(
    @PrimaryKey val projectId: Long,
    val persona: String,
    val consultancyType: String,
)

@Entity(
    tableName = "project_stages",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class ProjectStageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val status: String,
    val orderIndex: Int,
    val feeAmount: Double,
    val createdAt: Long,
)

@Entity(
    tableName = "invoices",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class InvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val stageId: Long?,
    val number: String,
    val title: String,
    val amount: Double,
    val status: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "''") val taxJson: String = "",
)

@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = InvoiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["invoiceId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("invoiceId"), Index("projectId")],
)
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val projectId: Long,
    val amount: Double,
    val note: String,
    val receivedAt: Long,
    @ColumnInfo(defaultValue = "0") val tdsAmount: Double = 0.0,
    @ColumnInfo(defaultValue = "''") val tdsReference: String = "",
    @ColumnInfo(defaultValue = "0") val tdsVerified: Boolean = false,
    @ColumnInfo(defaultValue = "''") val bankKey: String = "",

)

@Entity(tableName = "enquiries")
data class EnquiryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val source: String,
    val notes: String,
    val status: String,
    val createdAt: Long,
)

@Entity(
    tableName = "permits",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class PermitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val authority: String,
    val status: String,
    val createdAt: Long,
)

@Entity(
    tableName = "time_entries",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class TimeEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val person: String,
    val minutes: Int,
    val notes: String,
    val workDate: Long,
)

@Entity(tableName = "letters", indices = [Index("projectId")])
data class LetterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long?,
    val subject: String,
    val body: String,
    val status: String,
    val createdAt: Long,
)

@Entity(
    tableName = "fee_proposals",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class FeeProposalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val amount: Double,
    val status: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "''") val detailsJson: String = "",
    @ColumnInfo(defaultValue = "''") val documentText: String = "",
)

@Entity(
    tableName = "rcc_members",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class RccMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val elementType: String,
    val mark: String,
    val nos: Double,
    val length: Double,
    val breadth: Double,
    val height: Double,
    val quantity: Double,
    val remarks: String,
    val createdAt: Long,
)

@Entity(tableName = "rate_book_items")
data class RateBookItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val unit: String,
    val rate: Double,
    val sourceName: String,
)

@Entity(
    tableName = "work_contracts",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class WorkContractEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val party: String,
    val kind: String,
    val amount: Double,
    val status: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "''") val detailsJson: String = "",
    @ColumnInfo(defaultValue = "''") val documentText: String = "",
)

@Entity(
    tableName = "schedule_activities",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class ScheduleActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val progress: Int,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "item_link_rules")
data class ItemLinkRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceTrade: String,
    val targetTrade: String,
    val factor: Double,
    val note: String,
)

@Entity(tableName = "finance_records")
data class FinanceRecordEntity(@PrimaryKey val key: String, val kind: String, val json: String)
