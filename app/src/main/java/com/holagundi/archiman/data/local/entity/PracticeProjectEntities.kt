package com.holagundi.archiman.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey val id: Long = 1,
    val practiceName: String,
    val legalName: String,
    val companyType: String,
    val address: String,
    val city: String,
    val state: String,
    val country: String,
    val pinCode: String,
    val phone: String,
    val email: String,
    val website: String,
    val pan: String,
    val gstin: String,
    val coaRegistrationNumber: String,
    val principalName: String,
    val principalQualification: String,
    val practiceRegistrationDetails: String,
    val logoUri: String? = null,
    val updatedAt: Long,
)

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val clientType: String,
    val contactPerson: String,
    val address: String,
    val correspondenceAddress: String,
    val contactNo: String,
    val email: String,
    val preferredCommunication: String,
    val notes: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "''") val detailsJson: String = "",
)

@Entity(tableName = "local_users")
data class LocalUserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val role: String,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastLoginAt: Long? = null,
)

@Entity(
    tableName = "portal_audit_events",
    indices = [Index("projectId"), Index("userId")],
)
data class PortalAuditEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val username: String,
    val action: String,
    val entityType: String,
    val entityId: Long? = null,
    val projectId: Long? = null,
    val summary: String,
    val sourceAddress: String,
    val occurredAt: Long,
)

@Entity(tableName = "projects", indices = [Index("clientId")])
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val client: String,
    val clientId: Long,
    val siteLocation: String,
    val projectCode: String,
    val projectType: String,
    val status: String,
    val description: String,
    val architectInCharge: String,
    val startDate: Long? = null,
    val targetCompletionDate: Long? = null,
    val plotArea: Double? = null,
    val builtUpArea: Double? = null,
    val areaUnit: String,
    val createdAt: Long,
)

@Entity(
    tableName = "project_consultancy_profiles",
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
data class ProjectConsultancyProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val consultancyTypes: String,
    val currentPhase: String,
    val currentDesignStage: String,
    val briefStatus: String,
    val clientObjectives: String,
    val projectRequirements: String,
    val designPreferences: String,
    val siteConstraints: String,
    val clarifications: String,
    val siteDimensions: String,
    val siteOrientation: String,
    val siteAccess: String,
    val existingConditions: String,
    val surroundings: String,
    val topography: String,
    val utilities: String,
    val existingStructures: String,
    val vegetation: String,
    val legalPlanningInformation: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "project_scope_items",
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
data class ProjectScopeItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val category: String,
    val title: String,
    val details: String,
    val status: String,
    val orderIndex: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "project_onboarding_responses",
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
data class ProjectOnboardingResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val templateVersion: Int,
    val questionCode: String,
    val answer: String,
    val clarification: String,
    val updatedAt: Long,
)

@Entity(
    tableName = "project_approvals",
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
data class ProjectApprovalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val approvalType: String,
    val title: String,
    val description: String,
    val phase: String,
    val status: String,
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val remarks: String,
    val createdAt: Long,
)

@Entity(
    tableName = "project_backlog",
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
data class ProjectBacklogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val category: String,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val phase: String,
    val dueAt: Long? = null,
    val createdAt: Long,
)

@Entity(
    tableName = "floors",
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
data class FloorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val orderIndex: Int,
)

@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FloorEntity::class,
            parentColumns = ["id"],
            childColumns = ["floorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("floorId")],
)
data class RoomEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val floorId: Long,
    val name: String,
    val orderIndex: Int,
)

@Entity(
    tableName = "components",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = FloorEntity::class,
            parentColumns = ["id"],
            childColumns = ["floorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("floorId")],
)
data class ComponentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val floorId: Long,
    val roomId: Long,
    val name: String,
    val type: String,
    val length: Double,
    val width: Double,
    val height: Double,
    val thickness: Double,
)

@Entity(
    tableName = "component_work_items",
    foreignKeys = [
        ForeignKey(
            entity = ComponentEntity::class,
            parentColumns = ["id"],
            childColumns = ["componentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("componentId")],
)
data class ComponentWorkItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val componentId: Long,
    val itemId: Long,
    val itemName: String,
    val unit: String,
    val calculationType: String,
)

@Entity(tableName = "contractors", indices = [Index("projectId")])
data class ContractorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val address: String,
    val contactNo: String,
    val phone: String,
    val contractorType: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "''") val detailsJson: String = "",
)

@Entity(
    tableName = "contractor_qualified_items",
    foreignKeys = [
        ForeignKey(
            entity = ContractorEntity::class,
            parentColumns = ["id"],
            childColumns = ["contractorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("contractorId")],
)
data class ContractorQualifiedItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contractorId: Long,
    val workType: String,
    val itemName: String,
    val uom: String,
    val calculationType: String,
)

@Entity(
    tableName = "project_contractors",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContractorEntity::class,
            parentColumns = ["id"],
            childColumns = ["contractorId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("contractorId")],
)
data class ProjectContractorCrossRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val contractorId: Long,
)

@Entity(tableName = "item_master", indices = [Index(value = ["itemCode"], unique = true)])
data class ItemMasterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemCode: String,
    val workType: String,
    val name: String,
    val unit: String,
    val calculationType: String,
    val specification: String,
    val sourceName: String,
    val sourceItemCode: String,
    val isPredefined: Boolean,
    val isActive: Boolean,
)

@Entity(
    tableName = "work_item_aliases",
    foreignKeys = [
        ForeignKey(
            entity = ItemMasterEntity::class,
            parentColumns = ["id"],
            childColumns = ["workItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workItemId")],
)
data class WorkItemAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workItemId: Long,
    val alias: String,
)
