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
    val clientType: String = "Individual",
    val contactPerson: String = "",
    val address: String = "",
    val correspondenceAddress: String = "",
    val contactNo: String = "",
    val email: String = "",
    val preferredCommunication: String = "Phone",
    val notes: String = "",
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
    val projectCode: String = "",
    val projectType: String = "Residential",
    val status: String = "ACTIVE",
    val description: String = "",
    val architectInCharge: String = "",
    val startDate: Long? = null,
    val targetCompletionDate: Long? = null,
    val plotArea: Double? = null,
    val builtUpArea: Double? = null,
    val areaUnit: String = "m²",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_consultancy_profiles", indices = [Index(value = ["projectId"], unique = true)])
data class ProjectConsultancyProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val consultancyTypes: String = "Architectural",
    val currentPhase: String = "DESIGN",
    val currentDesignStage: String = "CONCEPT",
    val briefStatus: String = "NOT_STARTED",
    val clientObjectives: String = "",
    val projectRequirements: String = "",
    val designPreferences: String = "",
    val siteConstraints: String = "",
    val clarifications: String = "",
    val siteDimensions: String = "",
    val siteOrientation: String = "",
    val siteAccess: String = "",
    val existingConditions: String = "",
    val surroundings: String = "",
    val topography: String = "",
    val utilities: String = "",
    val existingStructures: String = "",
    val vegetation: String = "",
    val legalPlanningInformation: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_scope_items", indices = [Index("projectId"), Index(value = ["projectId", "category"])])
data class ProjectScopeItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val category: String = "SCOPE",
    val title: String,
    val details: String = "",
    val status: String = "INCLUDED",
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "project_onboarding_responses",
    indices = [Index("projectId"), Index(value = ["projectId", "questionCode"], unique = true)]
)
data class ProjectOnboardingResponseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val templateVersion: Int = 1,
    val questionCode: String,
    val answer: String = "",
    val clarification: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_approvals", indices = [Index("projectId"), Index("status")])
data class ProjectApprovalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val approvalType: String = "CLIENT",
    val title: String,
    val description: String = "",
    val phase: String = "DESIGN",
    val status: String = "PENDING",
    val submittedAt: Long? = null,
    val approvedAt: Long? = null,
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_backlog", indices = [Index("projectId"), Index("status"), Index("category")])
data class ProjectBacklogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val category: String = "GENERAL",
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM",
    val status: String = "OPEN",
    val phase: String = "DESIGN",
    val dueAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "company_profile")
data class CompanyProfileEntity(
    @PrimaryKey val id: Int = 1,
    val practiceName: String = "",
    val legalName: String = "",
    val companyType: String = "Architecture practice",
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "India",
    val pinCode: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val pan: String = "",
    val gstin: String = "",
    val coaRegistrationNumber: String = "",
    val principalName: String = "",
    val principalQualification: String = "",
    val practiceRegistrationDetails: String = "",
    val logoUri: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
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

@Entity(tableName = "project_tasks", indices = [Index("projectId")])
data class ProjectTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,
    val status: String = "OPEN",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_selection_items", indices = [Index("projectId")])
data class ProjectSelectionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val itemName: String,
    val specification: String = "",
    val makeOrBrand: String = "",
    val quantity: Double = 1.0,
    val unit: String = "Nos",
    val status: String = "PENDING",
    val remarks: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "project_schedules", indices = [Index("projectId"), Index("scheduledAt")])
data class ProjectScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val scheduledAt: Long,
    val location: String = "",
    val notes: String = "",
    val status: String = "SCHEDULED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meeting_minutes", indices = [Index("projectId"), Index("meetingAt")])
data class MeetingMinutesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val meetingAt: Long,
    val location: String = "",
    val attendees: String = "",
    val discussion: String = "",
    val decisions: String = "",
    val actionItems: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "site_inspections", indices = [Index("projectId"), Index("inspectionAt"), Index("status")])
data class SiteInspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val inspectionAt: Long,
    val location: String,
    val inspector: String = "",
    val observation: String,
    val severity: String = "NORMAL",
    val correctiveAction: String = "",
    val dueAt: Long? = null,
    val status: String = "OPEN",
    val photoUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "project_drawings",
    indices = [Index("projectId"), Index(value = ["projectId", "drawingNumber"], unique = true)]
)
data class ProjectDrawingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val drawingNumber: String,
    val title: String,
    val discipline: String = "Architectural",
    val status: String = "WORKING",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val archivedAt: Long? = null
)

@Entity(
    tableName = "drawing_revisions",
    indices = [Index("projectId"), Index("drawingId"), Index(value = ["drawingId", "revisionCode"], unique = true)]
)
data class DrawingRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val drawingId: Long,
    val revisionCode: String,
    val fileName: String,
    val mimeType: String = "application/acad",
    val fileUri: String,
    val fileChecksum: String = "",
    val issueStatus: String = "WIP",
    val revisionNotes: String = "",
    val isAsBuilt: Boolean = false,
    val issuedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "drawing_transmittals", indices = [Index("projectId"), Index(value = ["projectId", "transmittalNumber"], unique = true)])
data class DrawingTransmittalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val transmittalNumber: String,
    val subject: String,
    val recipients: String,
    val purpose: String = "FOR_INFORMATION",
    val notes: String = "",
    val issuedAt: Long = System.currentTimeMillis(),
    val acknowledgedAt: Long? = null
)

@Entity(tableName = "drawing_transmittal_items", indices = [Index("transmittalId"), Index("drawingRevisionId"), Index(value = ["transmittalId", "drawingRevisionId"], unique = true)])
data class DrawingTransmittalItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transmittalId: Long,
    val drawingRevisionId: Long
)

@Entity(tableName = "drawing_markups", indices = [Index("drawingRevisionId"), Index("createdAt")])
data class DrawingMarkupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val drawingRevisionId: Long,
    val markupType: String,
    val geometryJson: String,
    val styleJson: String = "{}",
    val measurementValue: Double? = null,
    val measurementUnit: String? = null,
    val calibrationJson: String? = null,
    val authorId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
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

@Entity(tableName = "contractor_rate_books", indices = [Index("contractorId")])
data class ContractorRateBookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contractorId: Long,
    val name: String,
    val version: Int = 1,
    val status: String = "DRAFT",
    val effectiveFrom: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "contractor_rate_book_items",
    indices = [Index("rateBookId"), Index("itemId"), Index(value = ["rateBookId", "itemId"], unique = true)]
)
data class ContractorRateBookItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rateBookId: Long,
    val itemId: Long,
    val itemNameSnapshot: String,
    val uomSnapshot: String,
    val specificationSnapshot: String = "",
    val sourceItemCodeSnapshot: String = "",
    val rate: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "project_rate_book_assignments",
    indices = [Index("projectId"), Index("contractorId"), Index("rateBookId"), Index(value = ["projectId", "contractorId"], unique = true)]
)
data class ProjectRateBookAssignmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val contractorId: Long,
    val rateBookId: Long,
    val assignedAt: Long = System.currentTimeMillis()
)
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
    val specification: String = "",
    val sourceName: String = "",
    val sourceItemCode: String = "",
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
    val appliedRateBookId: Long? = null,
    val rateSnapshot: Double? = null,
    val amountSnapshot: Double? = null,
    val floor: String = "",
    val location: String = "",
    val remarks: String = "",
    val photoUri: String? = null,
    val date: Long = System.currentTimeMillis()
)
