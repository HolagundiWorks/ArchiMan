// Data models mirroring Room entities (Schema 27) and domain types

export type CalculationType = 'RUNNING_LENGTH' | 'AREA' | 'WALL_PLASTER' | 'VOLUME' | 'NOS';

export interface ClientEntity {
  id: number;
  name: string;
  clientType: string;
  contactPerson: string;
  address: string;
  correspondenceAddress: string;
  contactNo: string;
  email: string;
  preferredCommunication: string;
  notes: string;
  createdAt: number;
}

export interface ProjectEntity {
  id: number;
  name: string;
  client: string;
  clientId: number;
  siteLocation: string;
  projectCode: string;
  projectType: string;
  status: 'ACTIVE' | 'ARCHIVED' | 'COMPLETED' | 'ON_HOLD';
  description: string;
  architectInCharge: string;
  startDate: number | null;
  targetCompletionDate: number | null;
  plotArea: number | null;
  builtUpArea: number | null;
  areaUnit: string;
  createdAt: number;
}

export interface ProjectConsultancyProfileEntity {
  id: number;
  projectId: number;
  consultancyTypes: string;
  currentPhase: string;
  currentDesignStage: string;
  briefStatus: string;
  clientObjectives: string;
  projectRequirements: string;
  designPreferences: string;
  siteConstraints: string;
  clarifications: string;
  siteDimensions: string;
  siteOrientation: string;
  siteAccess: string;
  existingConditions: string;
  surroundings: string;
  topography: string;
  utilities: string;
  existingStructures: string;
  vegetation: string;
  legalPlanningInformation: string;
  updatedAt: number;
}

export interface ProjectScopeItemEntity {
  id: number;
  projectId: number;
  category: string;
  title: string;
  details: string;
  status: 'INCLUDED' | 'EXCLUDED' | 'CONDITIONAL';
  orderIndex: number;
  createdAt: number;
}

export interface ProjectOnboardingResponseEntity {
  id: number;
  projectId: number;
  templateVersion: number;
  questionCode: string;
  answer: string;
  clarification: string;
  updatedAt: number;
}

export interface ProjectApprovalEntity {
  id: number;
  projectId: number;
  approvalType: string;
  title: string;
  description: string;
  phase: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED';
  submittedAt: number | null;
  approvedAt: number | null;
  remarks: string;
  createdAt: number;
}

export interface ProjectBacklogEntity {
  id: number;
  projectId: number;
  category: string;
  title: string;
  description: string;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  status: 'OPEN' | 'IN_PROGRESS' | 'DONE';
  phase: string;
  dueAt: number | null;
  createdAt: number;
}

export interface LocalUserEntity {
  id: number;
  username: string;
  displayName: string;
  role: 'ADMIN' | 'ARCHITECT' | 'SITE_ENGINEER' | 'VIEWER';
  isActive: boolean;
  createdAt: number;
  updatedAt: number;
  lastLoginAt: number | null;
}

export interface PortalAuditEventEntity {
  id: number;
  userId: number;
  username: string;
  action: string;
  entityType: string;
  entityId: number | null;
  projectId: number | null;
  summary: string;
  sourceAddress: string;
  occurredAt: number;
}

export interface CompanyProfileEntity {
  id: number;
  practiceName: string;
  legalName: string;
  companyType: string;
  address: string;
  city: string;
  state: string;
  country: string;
  pinCode: string;
  phone: string;
  email: string;
  website: string;
  pan: string;
  gstin: string;
  coaRegistrationNumber: string;
  principalName: string;
  principalQualification: string;
  practiceRegistrationDetails: string;
  logoUri: string | null;
  updatedAt: number;
}

export interface FloorEntity {
  id: number;
  projectId: number;
  name: string;
  orderIndex: number;
}

export interface RoomEntity {
  id: number;
  projectId: number;
  floorId: number;
  name: string;
  orderIndex: number;
}

export type ComponentType =
  | 'WALL'
  | 'FLOOR'
  | 'CEILING'
  | 'OPENING'
  | 'SLAB'
  | 'BEAM'
  | 'COLUMN'
  | 'COLUMN_BEAM'
  | 'WATERPROOFING'
  | 'OTHER';

export interface ComponentEntity {
  id: number;
  projectId: number;
  floorId: number;
  roomId: number; // 0 means Whole Floor Level
  name: string;
  type: ComponentType;
  length: number;
  width: number;
  height: number;
  thickness: number;
}

export interface ComponentWorkItemEntity {
  id: number;
  componentId: number;
  itemId: number;
  itemName: string;
  unit: string;
  calculationType: CalculationType;
}

export interface ContractorEntity {
  id: number;
  projectId: number;
  name: string;
  address: string;
  contactNo: string;
  phone: string;
  contractorType: string;
  createdAt: number;
}

export interface ContractorQualifiedItemEntity {
  id: number;
  contractorId: number;
  workType: string;
  itemName: string;
  uom: string;
  calculationType: CalculationType;
}

export interface ProjectContractorCrossRef {
  id: number;
  projectId: number;
  contractorId: number;
}

export interface ProjectTaskEntity {
  id: number;
  projectId: number;
  title: string;
  description: string;
  dueDate: number | null;
  status: 'OPEN' | 'IN_PROGRESS' | 'COMPLETED';
  createdAt: number;
}

export interface ProjectSelectionItemEntity {
  id: number;
  projectId: number;
  itemName: string;
  specification: string;
  makeOrBrand: string;
  quantity: number;
  unit: string;
  status: 'PENDING' | 'APPROVED' | 'ORDERED' | 'INSTALLED';
  remarks: string;
  createdAt: number;
}

export interface ProjectScheduleEntity {
  id: number;
  projectId: number;
  title: string;
  scheduledAt: number;
  location: string;
  notes: string;
  status: 'SCHEDULED' | 'COMPLETED' | 'CANCELLED';
  createdAt: number;
}

export interface MeetingMinutesEntity {
  id: number;
  projectId: number;
  title: string;
  meetingAt: number;
  location: string;
  attendees: string;
  discussion: string;
  decisions: string;
  actionItems: string;
  createdAt: number;
}

export interface SiteInspectionEntity {
  id: number;
  projectId: number;
  inspectionAt: number;
  location: string;
  inspector: string;
  observation: string;
  severity: 'NORMAL' | 'ATTENTION' | 'CRITICAL';
  correctiveAction: string;
  dueAt: number | null;
  status: 'OPEN' | 'RESOLVED';
  photoUri: string | null;
  createdAt: number;
}

export interface DailySiteReportEntity {
  id: number;
  projectId: number;
  reportDate: number;
  weather: string;
  manpower: string;
  workCompleted: string;
  materialsReceived: string;
  delaysOrConstraints: string;
  safetyObservations: string;
  nextDayPlan: string;
  preparedBy: string;
  photoUri: string | null;
  createdAt: number;
  updatedAt: number;
}

export interface ProjectDecisionEntity {
  id: number;
  projectId: number;
  referenceNumber: string;
  title: string;
  context: string;
  decisionRequired: string;
  impact: string;
  finalDecision: string;
  requestedFrom: string;
  owner: string;
  dueAt: number | null;
  status: 'OPEN' | 'DECIDED';
  decidedAt: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface SiteIssueEntity {
  id: number;
  projectId: number;
  referenceNumber: string;
  type: 'SNAG' | 'NCR' | 'SAFETY';
  title: string;
  location: string;
  description: string;
  severity: 'NORMAL' | 'ATTENTION' | 'CRITICAL';
  assignedTo: string;
  correctiveAction: string;
  dueAt: number | null;
  status: 'OPEN' | 'IN_PROGRESS' | 'READY_FOR_VERIFICATION' | 'CLOSED';
  evidenceUri: string | null;
  afterEvidenceUri: string | null;
  verificationNote: string;
  closedAt: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface SiteIssueEventEntity {
  id: number;
  siteIssueId: number;
  fromStatus: string;
  toStatus: string;
  note: string;
  actor: string;
  occurredAt: number;
}

export interface ProjectConsultantEntity {
  id: number;
  projectId: number;
  name: string;
  organisation: string;
  discipline: string;
  email: string;
  phone: string;
  phase: string;
  responsibility: string;
  raciRole: 'RESPONSIBLE' | 'ACCOUNTABLE' | 'CONSULTED' | 'INFORMED';
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: number;
}

export interface CoordinationItemEntity {
  id: number;
  projectId: number;
  type: 'RFI' | 'SUBMITTAL' | 'SITE_INSTRUCTION';
  referenceNumber: string;
  subject: string;
  discipline: string;
  location: string;
  raisedBy: string;
  assignedTo: string;
  questionOrRequirement: string;
  response: string;
  status: string;
  priority: 'NORMAL' | 'HIGH' | 'CRITICAL';
  dueAt: number | null;
  linkedDrawingRevisionId: number | null;
  createdAt: number;
  updatedAt: number;
  closedAt: number | null;
  archivedAt: number | null;
}

export interface CoordinationEventEntity {
  id: number;
  coordinationItemId: number;
  fromStatus: string;
  toStatus: string;
  note: string;
  actor: string;
  occurredAt: number;
}

export interface ProjectDrawingEntity {
  id: number;
  projectId: number;
  drawingNumber: string;
  title: string;
  discipline: string;
  status: string;
  packageTag: string; // e.g. "HFC" or ""
  createdAt: number;
  updatedAt: number;
  archivedAt: number | null;
}

export interface DrawingRevisionEntity {
  id: number;
  projectId: number;
  drawingId: number;
  revisionCode: string;
  fileName: string;
  mimeType: string;
  fileUri: string;
  fileChecksum: string;
  issueStatus: 'WIP' | 'ISSUED' | 'ARCHIVED';
  revisionNotes: string;
  isAsBuilt: boolean;
  revisionSource: string;
  severity: 'NORMAL' | 'ATTENTION' | 'CRITICAL';
  issuedAt: number | null;
  createdAt: number;
}

export interface DrawingTransmittalEntity {
  id: number;
  projectId: number;
  transmittalNumber: string;
  subject: string;
  recipients: string;
  purpose: string;
  notes: string;
  issuedAt: number;
  acknowledgedAt: number | null;
  revisionIds?: number[];
}

export interface SiteDocumentEntity {
  id: number;
  projectId: number;
  title: string;
  category: 'HFC' | 'METHOD_STATEMENT' | 'SITE_MEMO' | 'QUALITY' | 'SAFETY';
  notes: string;
  linkedDrawingId: number | null;
  fileUri: string | null;
  createdAt: number;
}

export interface MaterialOrderEntity {
  id: number;
  projectId: number;
  referenceNumber: string;
  title: string;
  supplier: string;
  status: 'DRAFT' | 'ISSUED' | 'FULFILLED' | 'CANCELLED';
  notes: string;
  createdAt: number;
  issuedAt: number | null;
}

export interface MaterialOrderLineEntity {
  id: number;
  orderId: number;
  itemName: string;
  specification: string;
  quantity: number;
  unit: string;
  remarks: string;
}

export interface ItemMasterEntity {
  id: number;
  itemCode: string;
  workType: string;
  name: string;
  unit: string;
  calculationType: CalculationType;
  specification: string;
  sourceName: string;
  sourceItemCode: string;
  isPredefined: boolean;
  isActive: boolean;
}

export interface WorkItemAliasEntity {
  id: number;
  workItemId: number;
  alias: string;
}

export type MeasurementSheetStatus = 'DRAFT' | 'SUBMITTED' | 'CHECKED' | 'APPROVED' | 'RETURNED';

export interface MeasurementSheetEntity {
  id: number;
  sheetCode: string;
  projectId: number;
  floorId: number | null;
  floorNameSnapshot: string;
  contractorId: number;
  contractorNameSnapshot: string;
  itemId: number;
  itemNameSnapshot: string;
  uomSnapshot: string;
  formulaCode: string;
  formulaVersion: number;
  status: MeasurementSheetStatus;
  revision: number;
  lockedAt: number | null;
  archivedAt: number | null;
  createdAt: number;
  updatedAt: number;
}

export interface MeasurementReviewEventEntity {
  id: number;
  sheetId: number;
  fromStatus: string;
  toStatus: string;
  comment: string;
  actor: string;
  revision: number;
  createdAt: number;
}

export interface MeasurementEntity {
  id: number;
  sheetId: number;
  projectId: number;
  floorId: number | null;
  roomId: number | null;
  componentId: number | null;
  componentWorkItemId: number | null;
  contractorId: number;
  contractorName: string;
  itemId: number;
  itemName: string;
  unit: string;
  calculationType: CalculationType;
  formulaCode: string;
  formulaVersion: number;
  description: string;
  length: number;
  width: number;
  height: number;
  nos: number;
  deduction: number;
  quantity: number;
  floor: string;
  location: string;
  remarks: string;
  photoUri: string | null;
  date: number;
}

// Navigation Enums & Hierarchy
export type AppScreen =
  | 'HOME'
  | 'PROJECT_WORKSPACE'
  | 'DEDICATED_MEASUREMENT'
  | 'MEASUREMENT_BOOK'
  | 'CLIENTS'
  | 'CONTRACTORS'
  | 'MASTER_DATA'
  | 'COMPANY_PROFILE'
  | 'COMPANY_DASHBOARD';

export type HomeTab = 'PROJECTS' | 'DIRECTORY' | 'WORK_LIBRARY' | 'PRACTICE';

export type ProjectSection =
  | 'OVERVIEW'
  | 'WORK'
  | 'RECORD'
  | 'M_BOOK'
  | 'MORE';

export type MoreSection =
  | 'SITEMAN'
  | 'DRAWINGS'
  | 'REPORTS'
  | 'DECISIONS'
  | 'COORDINATION'
  | 'PLANNING'
  | 'BRIEF_SCOPE'
  | 'CONTRACTORS';

export type DirectorySection = 'CLIENTS' | 'CONTRACTORS';
