package com.holagundi.archiman.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "project_tasks",
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
data class ProjectTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val description: String,
    val dueDate: Long? = null,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "project_selection_items", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class ProjectSelectionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val itemName: String,
    val specification: String,
    val makeOrBrand: String,
    val quantity: Double,
    val unit: String,
    val status: String,
    val remarks: String,
    val createdAt: Long,
)

@Entity(tableName = "project_schedules", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class ProjectScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val scheduledAt: Long,
    val location: String,
    val notes: String,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "meeting_minutes", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class MeetingMinutesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val meetingAt: Long,
    val location: String,
    val attendees: String,
    val discussion: String,
    val decisions: String,
    val actionItems: String,
    val createdAt: Long,
)

@Entity(tableName = "site_inspections", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class SiteInspectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val inspectionAt: Long,
    val location: String,
    val inspector: String,
    val observation: String,
    val severity: String,
    val correctiveAction: String,
    val dueAt: Long? = null,
    val status: String,
    val photoUri: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "daily_site_reports", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class DailySiteReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val reportDate: Long,
    val weather: String,
    val manpower: String,
    val workCompleted: String,
    val materialsReceived: String,
    val delaysOrConstraints: String,
    val safetyObservations: String,
    val nextDayPlan: String,
    val preparedBy: String,
    val photoUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "project_decisions", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class ProjectDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val referenceNumber: String,
    val title: String,
    val context: String,
    val decisionRequired: String,
    val impact: String,
    val finalDecision: String,
    val requestedFrom: String,
    val owner: String,
    val dueAt: Long? = null,
    val status: String,
    val decidedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "site_issues", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class SiteIssueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val referenceNumber: String,
    val type: String,
    val title: String,
    val location: String,
    val description: String,
    val severity: String,
    val assignedTo: String,
    val correctiveAction: String,
    val dueAt: Long? = null,
    val status: String,
    val evidenceUri: String? = null,
    val afterEvidenceUri: String? = null,
    val verificationNote: String,
    val closedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "site_issue_events",
    foreignKeys = [
        ForeignKey(
            entity = SiteIssueEntity::class,
            parentColumns = ["id"],
            childColumns = ["siteIssueId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("siteIssueId")],
)
data class SiteIssueEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val siteIssueId: Long,
    val fromStatus: String,
    val toStatus: String,
    val note: String,
    val actor: String,
    val occurredAt: Long,
)

@Entity(tableName = "project_consultants", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class ProjectConsultantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val name: String,
    val organisation: String,
    val discipline: String,
    val email: String,
    val phone: String,
    val phase: String,
    val responsibility: String,
    val raciRole: String,
    val status: String,
    val createdAt: Long,
)

@Entity(tableName = "coordination_items", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class CoordinationItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val type: String,
    val referenceNumber: String,
    val subject: String,
    val discipline: String,
    val location: String,
    val raisedBy: String,
    val assignedTo: String,
    val questionOrRequirement: String,
    val response: String,
    val status: String,
    val priority: String,
    val dueAt: Long? = null,
    val linkedDrawingRevisionId: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val closedAt: Long? = null,
    val archivedAt: Long? = null,
)

@Entity(
    tableName = "coordination_events",
    foreignKeys = [
        ForeignKey(
            entity = CoordinationItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["coordinationItemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("coordinationItemId")],
)
data class CoordinationEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val coordinationItemId: Long,
    val fromStatus: String,
    val toStatus: String,
    val note: String,
    val actor: String,
    val occurredAt: Long,
)

@Entity(tableName = "project_drawings", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class ProjectDrawingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val drawingNumber: String,
    val title: String,
    val discipline: String,
    val status: String,
    val packageTag: String,
    val createdAt: Long,
    val updatedAt: Long,
    val archivedAt: Long? = null,
)

@Entity(
    tableName = "drawing_revisions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProjectDrawingEntity::class,
            parentColumns = ["id"],
            childColumns = ["drawingId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId"), Index("drawingId")],
)
data class DrawingRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val drawingId: Long,
    val revisionCode: String,
    val fileName: String,
    val mimeType: String,
    val fileUri: String,
    val fileChecksum: String,
    val issueStatus: String,
    val revisionNotes: String,
    val isAsBuilt: Boolean,
    val revisionSource: String,
    val severity: String,
    val issuedAt: Long? = null,
    val createdAt: Long,
)

@Entity(tableName = "drawing_transmittals", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class DrawingTransmittalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val transmittalNumber: String,
    val subject: String,
    val recipients: String,
    val purpose: String,
    val notes: String,
    val issuedAt: Long,
    val acknowledgedAt: Long? = null,
)

@Entity(
    tableName = "drawing_transmittal_revisions",
    primaryKeys = ["transmittalId", "revisionId"],
    foreignKeys = [
        ForeignKey(
            entity = DrawingTransmittalEntity::class,
            parentColumns = ["id"],
            childColumns = ["transmittalId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = DrawingRevisionEntity::class,
            parentColumns = ["id"],
            childColumns = ["revisionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("revisionId")],
)
data class DrawingTransmittalRevisionEntity(
    val transmittalId: Long,
    val revisionId: Long,
)

@Entity(tableName = "site_documents", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class SiteDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val category: String,
    val notes: String,
    val linkedDrawingId: Long? = null,
    val fileUri: String? = null,
    val createdAt: Long,
)

@Entity(tableName = "material_orders", foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ], indices = [Index("projectId")])
data class MaterialOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val referenceNumber: String,
    val title: String,
    val supplier: String,
    val status: String,
    val notes: String,
    val createdAt: Long,
    val issuedAt: Long? = null,
)

@Entity(
    tableName = "material_order_lines",
    foreignKeys = [
        ForeignKey(
            entity = MaterialOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("orderId")],
)
data class MaterialOrderLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val itemName: String,
    val specification: String,
    val quantity: Double,
    val unit: String,
    val remarks: String,
)
