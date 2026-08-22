package com.example.data.local.entity

import androidx.room.Entity
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
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "contractor_qualified_items")
data class ContractorQualifiedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contractorId: Long,
    val itemName: String,
    val uom: String = "m²",
    val calculationType: CalculationType = CalculationType.AREA,
    val rate: Double = 0.0
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

@Entity(tableName = "item_master")
data class ItemMasterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val unit: String,
    val calculationType: CalculationType,
    val defaultRate: Double = 0.0,
    val isPredefined: Boolean = false
)

@Entity(tableName = "contractor_rates")
data class ContractorRateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contractorId: Long,
    val itemId: Long,
    val rate: Double
)

@Entity(tableName = "measurements")
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    val description: String = "",
    val length: Double = 0.0,
    val width: Double = 0.0,
    val height: Double = 0.0,
    val nos: Double = 1.0,
    val deduction: Double = 0.0,
    val quantity: Double = 0.0,
    val rate: Double = 0.0,
    val amount: Double = 0.0,
    val floor: String = "",
    val location: String = "",
    val remarks: String = "",
    val photoUri: String? = null,
    val date: Long = System.currentTimeMillis(),
    val billId: Long? = null
)

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val billNumber: String,
    val projectId: Long,
    val projectName: String,
    val contractorId: Long,
    val contractorName: String,
    val date: Long = System.currentTimeMillis(),
    val totalQuantity: Double = 0.0,
    val totalAmount: Double = 0.0,
    val retentionPercent: Double = 0.0,
    val netAmount: Double = 0.0,
    val notes: String = ""
)
