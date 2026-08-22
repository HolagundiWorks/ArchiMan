package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val address: String = "",
    val contactNo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val client: String = "",
    val clientId: Long = 0L,
    val siteLocation: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "floors")
data class FloorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val name: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "rooms")
data class RoomEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val floorId: Long,
    val name: String,
    val orderIndex: Int = 0
)

enum class ComponentType(val displayName: String) {
    WALL("Wall"),
    FLOOR("Floor / Screed"),
    CEILING("Ceiling"),
    OPENING("Opening"),
    SLAB("Slab / Deck"),
    BEAM("Beam (Main/Secondary/Plinth)"),
    COLUMN("Column"),
    COLUMN_BEAM("Column / Beam"),
    WATERPROOFING("Waterproofing"),
    OTHER("Other Structural / Common")
}

@Entity(tableName = "components")
data class ComponentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val floorId: Long,
    val roomId: Long = 0L, // 0L means Whole Floor Level (not tied to any room)
    val name: String,
    val type: ComponentType = ComponentType.WALL,
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val thickness: Double = 0.0
)

@Entity(tableName = "component_work_items")
data class ComponentWorkItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val componentId: Long,
    val itemId: Long,
    val itemName: String,
    val unit: String,
    val calculationType: CalculationType
)

@Entity(tableName = "contractors")
data class ContractorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long = 0L,
    val name: String,
    val address: String = "",
    val contactNo: String = "",
    val phone: String = "",
    val contractorType: String = "Civil",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contractor_qualified_items",
    indices = [Index(value = ["contractorId", "itemName"], unique = true)]
)
data class ContractorQualifiedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contractorId: Long,
    val workType: String = "General Works",
    val itemName: String,
    val uom: String = "m²",
    val calculationType: CalculationType = CalculationType.AREA
)

@Entity(tableName = "project_contractor_refs")
data class ProjectContractorCrossRef(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val contractorId: Long
)

enum class CalculationType(val displayName: String, val defaultUnit: String) {
    RUNNING_LENGTH("Running Length (m)", "m"),
    AREA("Area (m²)", "m²"),
    WALL_PLASTER("Wall Plaster (m²)", "m²"),
    VOLUME("Volume (m³)", "m³"),
    NOS("Numbers (Nos)", "Nos")
}

@Entity(
    tableName = "item_master",
    indices = [Index(value = ["name"], unique = true), Index(value = ["itemCode"], unique = true)]
)
data class ItemMasterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemCode: String = "",
    val workType: String = "General Works",
    val name: String,
    val unit: String,
    val calculationType: CalculationType,
    val isPredefined: Boolean = false,
    val isActive: Boolean = true
)

@Entity(
    tableName = "work_item_aliases",
    foreignKeys = [ForeignKey(
        entity = ItemMasterEntity::class,
        parentColumns = ["id"],
        childColumns = ["workItemId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["workItemId"]), Index(value = ["alias"], unique = true)]
)
data class WorkItemAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workItemId: Long,
    val alias: String
)

@Entity(
    tableName = "measurement_sheets",
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = FloorEntity::class, parentColumns = ["id"], childColumns = ["floorId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ContractorEntity::class, parentColumns = ["id"], childColumns = ["contractorId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = ItemMasterEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.NO_ACTION)
    ],
    indices = [Index("projectId"), Index("floorId"), Index("contractorId"), Index("itemId"), Index(value = ["sheetCode"], unique = true)]
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
    val formulaVersion: Int = 1,
    val status: String = "DRAFT",
    @ColumnInfo(defaultValue = "1") val revision: Int = 1,
    val lockedAt: Long? = null,
    val archivedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt
)

@Entity(
    tableName = "measurement_review_events",
    foreignKeys = [ForeignKey(entity = MeasurementSheetEntity::class, parentColumns = ["id"], childColumns = ["sheetId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sheetId")]
)
data class MeasurementReviewEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sheetId: Long,
    val fromStatus: String,
    val toStatus: String,
    val comment: String,
    val actor: String,
    val revision: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "measurements",
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = FloorEntity::class, parentColumns = ["id"], childColumns = ["floorId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = RoomEntity::class, parentColumns = ["id"], childColumns = ["roomId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ComponentEntity::class, parentColumns = ["id"], childColumns = ["componentId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ComponentWorkItemEntity::class, parentColumns = ["id"], childColumns = ["componentWorkItemId"], onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = ContractorEntity::class, parentColumns = ["id"], childColumns = ["contractorId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = ItemMasterEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.NO_ACTION),
        ForeignKey(entity = MeasurementSheetEntity::class, parentColumns = ["id"], childColumns = ["sheetId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index("projectId"), Index("floorId"), Index("roomId"), Index("componentId"),
        Index("componentWorkItemId"), Index("contractorId"), Index("itemId"), Index("sheetId")
    ]
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sheetId: Long = 0,
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
    val calculationType: CalculationType,
    val formulaCode: String = calculationType.name,
    val formulaVersion: Int = 1,
    val description: String = "",
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val nos: Double = 1.0,
    val deduction: Double = 0.0,
    val quantity: Double = 0.0,
    val floor: String = "",
    val location: String = "",
    val remarks: String = "",
    val photoUri: String? = null,
    val date: Long = System.currentTimeMillis()
)
