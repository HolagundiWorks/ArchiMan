package com.holagundi.archiman.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_sheets",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("contractorId"), Index("floorId")],
)
data class MeasurementSheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetCode: String,
    val projectId: Long,
    val floorId: Long? = null,
    val floorNameSnapshot: String,
    val contractorId: Long,
    val contractorNameSnapshot: String,
    val itemId: Long,
    val itemNameSnapshot: String,
    val uomSnapshot: String,
    val formulaCode: String,
    val formulaVersion: Int,
    val status: String,
    val revision: Int,
    val lockedAt: Long? = null,
    val archivedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "measurement_review_events",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementSheetEntity::class,
            parentColumns = ["id"],
            childColumns = ["sheetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sheetId")],
)
data class MeasurementReviewEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val fromStatus: String,
    val toStatus: String,
    val comment: String,
    val actor: String,
    val revision: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MeasurementSheetEntity::class,
            parentColumns = ["id"],
            childColumns = ["sheetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("sheetId")],
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val projectId: Long,
    val floorId: Long? = null,
    val roomId: Long? = null,
    val componentId: Long? = null,
    val componentWorkItemId: Long? = null,
    val contractorId: Long,
    val contractorName: String,
    val itemId: Long,
    val itemName: String,
    val unit: String,
    val calculationType: String,
    val formulaCode: String,
    val formulaVersion: Int,
    val description: String,
    val length: Double,
    val width: Double,
    val height: Double,
    val nos: Double,
    val deduction: Double,
    val quantity: Double,
    val floor: String,
    val location: String,
    val remarks: String,
    val photoUri: String? = null,
    val date: Long,
)
