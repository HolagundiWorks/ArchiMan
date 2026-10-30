import {
  ClientEntity,
  ProjectEntity,
  FloorEntity,
  RoomEntity,
  ContractorEntity,
  ContractorQualifiedItemEntity,
  ItemMasterEntity,
  MeasurementSheetEntity,
  MeasurementEntity,
  DailySiteReportEntity,
  SiteIssueEntity,
  CoordinationItemEntity,
  ProjectDrawingEntity,
  DrawingRevisionEntity,
  SiteDocumentEntity,
  MaterialOrderEntity,
  MaterialOrderLineEntity,
  CompanyProfileEntity,
  ProjectDecisionEntity,
  ProjectTaskEntity,
  ProjectConsultantEntity,
} from '../types';
import {
  initialCompanyProfile,
  initialClients,
  initialProjects,
  initialFloors,
  initialRooms,
  initialContractors,
  initialContractorQualifiedItems,
  initialStandardWorkItems,
  initialMeasurementSheets,
  initialMeasurements,
  initialDailyReports,
  initialSiteIssues,
  initialCoordinationItems,
  initialDrawings,
  initialDrawingRevisions,
  initialSiteDocuments,
  initialMaterialOrders,
  initialMaterialOrderLines,
  initialProjectDecisions,
  initialTasks,
  initialConsultants,
} from './seedData';

const STORAGE_PREFIX = 'archiman_v27_';

function getOrInit<T>(key: string, initial: T): T {
  try {
    const raw = localStorage.getItem(STORAGE_PREFIX + key);
    if (!raw) {
      localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(initial));
      return initial;
    }
    return JSON.parse(raw);
  } catch {
    return initial;
  }
}

function save<T>(key: string, value: T): void {
  try {
    localStorage.setItem(STORAGE_PREFIX + key, JSON.stringify(value));
  } catch (err) {
    console.error('Storage save error:', err);
  }
}

export class PracticeStore {
  // Store collections
  companyProfile: CompanyProfileEntity = getOrInit('companyProfile', initialCompanyProfile);
  clients: ClientEntity[] = getOrInit('clients', initialClients);
  projects: ProjectEntity[] = getOrInit('projects', initialProjects);
  floors: FloorEntity[] = getOrInit('floors', initialFloors);
  rooms: RoomEntity[] = getOrInit('rooms', initialRooms);
  contractors: ContractorEntity[] = getOrInit('contractors', initialContractors);
  contractorQualifiedItems: ContractorQualifiedItemEntity[] = getOrInit(
    'contractorQualifiedItems',
    initialContractorQualifiedItems
  );
  standardWorkItems: ItemMasterEntity[] = getOrInit('standardWorkItems', initialStandardWorkItems);
  measurementSheets: MeasurementSheetEntity[] = getOrInit('measurementSheets', initialMeasurementSheets);
  measurements: MeasurementEntity[] = getOrInit('measurements', initialMeasurements);
  dailyReports: DailySiteReportEntity[] = getOrInit('dailyReports', initialDailyReports);
  siteIssues: SiteIssueEntity[] = getOrInit('siteIssues', initialSiteIssues);
  coordinationItems: CoordinationItemEntity[] = getOrInit('coordinationItems', initialCoordinationItems);
  drawings: ProjectDrawingEntity[] = getOrInit('drawings', initialDrawings);
  drawingRevisions: DrawingRevisionEntity[] = getOrInit('drawingRevisions', initialDrawingRevisions);
  siteDocuments: SiteDocumentEntity[] = getOrInit('siteDocuments', initialSiteDocuments);
  materialOrders: MaterialOrderEntity[] = getOrInit('materialOrders', initialMaterialOrders);
  materialOrderLines: MaterialOrderLineEntity[] = getOrInit('materialOrderLines', initialMaterialOrderLines);
  projectDecisions: ProjectDecisionEntity[] = getOrInit('projectDecisions', initialProjectDecisions);
  projectTasks: ProjectTaskEntity[] = getOrInit('projectTasks', initialTasks);
  projectConsultants: ProjectConsultantEntity[] = getOrInit('projectConsultants', initialConsultants);

  private listeners: (() => void)[] = [];

  subscribe(listener: () => void) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter((l) => l !== listener);
    };
  }

  private notify() {
    this.listeners.forEach((l) => l());
  }

  // --- Company Profile ---
  updateCompanyProfile(profile: Partial<CompanyProfileEntity>) {
    this.companyProfile = { ...this.companyProfile, ...profile, updatedAt: Date.now() };
    save('companyProfile', this.companyProfile);
    this.notify();
  }

  // --- Projects ---
  addProject(data: Omit<ProjectEntity, 'id' | 'createdAt'>) {
    const id = Date.now();
    const newProj: ProjectEntity = { ...data, id, createdAt: id };
    this.projects.push(newProj);
    save('projects', this.projects);

    // Default baseline Ground Floor per project creation contract
    this.addFloor({ projectId: id, name: 'Ground Floor', orderIndex: 0 });
    this.notify();
    return newProj;
  }

  updateProject(id: number, data: Partial<ProjectEntity>) {
    this.projects = this.projects.map((p) => (p.id === id ? { ...p, ...data } : p));
    save('projects', this.projects);
    this.notify();
  }

  deleteProject(id: number) {
    this.projects = this.projects.filter((p) => p.id !== id);
    save('projects', this.projects);
    this.notify();
  }

  // --- Clients ---
  addClient(data: Omit<ClientEntity, 'id' | 'createdAt'>) {
    const id = Date.now();
    const newClient: ClientEntity = { ...data, id, createdAt: id };
    this.clients.push(newClient);
    save('clients', this.clients);
    this.notify();
    return newClient;
  }

  updateClient(id: number, data: Partial<ClientEntity>) {
    this.clients = this.clients.map((c) => (c.id === id ? { ...c, ...data } : c));
    save('clients', this.clients);
    this.notify();
  }

  deleteClient(id: number) {
    this.clients = this.clients.filter((c) => c.id !== id);
    save('clients', this.clients);
    this.notify();
  }

  // --- Floors & Rooms ---
  addFloor(data: Omit<FloorEntity, 'id'>) {
    const id = Date.now() + Math.floor(Math.random() * 100);
    const item: FloorEntity = { ...data, id };
    this.floors.push(item);
    save('floors', this.floors);
    this.notify();
    return item;
  }

  addRoom(data: Omit<RoomEntity, 'id'>) {
    const id = Date.now() + Math.floor(Math.random() * 100);
    const item: RoomEntity = { ...data, id };
    this.rooms.push(item);
    save('rooms', this.rooms);
    this.notify();
    return item;
  }

  // --- Contractors ---
  addContractor(data: Omit<ContractorEntity, 'id' | 'createdAt'>) {
    const id = Date.now();
    const item: ContractorEntity = { ...data, id, createdAt: id };
    this.contractors.push(item);
    save('contractors', this.contractors);
    this.notify();
    return item;
  }

  updateContractor(id: number, data: Partial<ContractorEntity>) {
    this.contractors = this.contractors.map((c) => (c.id === id ? { ...c, ...data } : c));
    save('contractors', this.contractors);
    this.notify();
  }

  addContractorQualifiedItem(data: Omit<ContractorQualifiedItemEntity, 'id'>) {
    const id = Date.now() + Math.floor(Math.random() * 100);
    const item: ContractorQualifiedItemEntity = { ...data, id };
    this.contractorQualifiedItems.push(item);
    save('contractorQualifiedItems', this.contractorQualifiedItems);
    this.notify();
    return item;
  }

  removeContractorQualifiedItem(id: number) {
    this.contractorQualifiedItems = this.contractorQualifiedItems.filter((i) => i.id !== id);
    save('contractorQualifiedItems', this.contractorQualifiedItems);
    this.notify();
  }

  // --- Master Catalog ---
  addCatalogItem(data: Omit<ItemMasterEntity, 'id'>) {
    const id = Date.now();
    const item: ItemMasterEntity = { ...data, id };
    this.standardWorkItems.push(item);
    save('standardWorkItems', this.standardWorkItems);
    this.notify();
    return item;
  }

  // --- Measurement Sheets & Measurements ---
  addMeasurementSheet(data: Omit<MeasurementSheetEntity, 'id' | 'createdAt' | 'updatedAt'>) {
    const id = Date.now();
    const sheet: MeasurementSheetEntity = { ...data, id, createdAt: id, updatedAt: id };
    this.measurementSheets.push(sheet);
    save('measurementSheets', this.measurementSheets);
    this.notify();
    return sheet;
  }

  updateSheetStatus(id: number, status: MeasurementSheetEntity['status']) {
    this.measurementSheets = this.measurementSheets.map((s) =>
      s.id === id
        ? {
            ...s,
            status,
            updatedAt: Date.now(),
            lockedAt: status === 'APPROVED' ? Date.now() : s.lockedAt,
          }
        : s
    );
    save('measurementSheets', this.measurementSheets);
    this.notify();
  }

  addMeasurement(data: Omit<MeasurementEntity, 'id'>) {
    const id = Date.now();
    const item: MeasurementEntity = { ...data, id };
    this.measurements.push(item);
    save('measurements', this.measurements);
    this.notify();
    return item;
  }

  deleteMeasurement(id: number) {
    this.measurements = this.measurements.filter((m) => m.id !== id);
    save('measurements', this.measurements);
    this.notify();
  }

  // --- Site Reports ---
  addDailyReport(data: Omit<DailySiteReportEntity, 'id' | 'createdAt' | 'updatedAt'>) {
    const id = Date.now();
    const item: DailySiteReportEntity = { ...data, id, createdAt: id, updatedAt: id };
    this.dailyReports.push(item);
    save('dailyReports', this.dailyReports);
    this.notify();
    return item;
  }

  // --- Site Issues (Snags / NCRs) ---
  addSiteIssue(data: Omit<SiteIssueEntity, 'id' | 'createdAt' | 'updatedAt'>) {
    const id = Date.now();
    const item: SiteIssueEntity = { ...data, id, createdAt: id, updatedAt: id };
    this.siteIssues.push(item);
    save('siteIssues', this.siteIssues);
    this.notify();
    return item;
  }

  updateSiteIssue(id: number, data: Partial<SiteIssueEntity>) {
    this.siteIssues = this.siteIssues.map((i) =>
      i.id === id ? { ...i, ...data, updatedAt: Date.now() } : i
    );
    save('siteIssues', this.siteIssues);
    this.notify();
  }

  // --- Coordination (RFIs, Submittals, Site Instructions) ---
  addCoordinationItem(data: Omit<CoordinationItemEntity, 'id' | 'createdAt' | 'updatedAt'>) {
    const id = Date.now();
    const item: CoordinationItemEntity = { ...data, id, createdAt: id, updatedAt: id };
    this.coordinationItems.push(item);
    save('coordinationItems', this.coordinationItems);
    this.notify();
    return item;
  }

  updateCoordinationItem(id: number, data: Partial<CoordinationItemEntity>) {
    this.coordinationItems = this.coordinationItems.map((c) =>
      c.id === id ? { ...c, ...data, updatedAt: Date.now() } : c
    );
    save('coordinationItems', this.coordinationItems);
    this.notify();
  }

  // --- Drawings & Revisions ---
  addDrawing(data: Omit<ProjectDrawingEntity, 'id' | 'createdAt' | 'updatedAt'>) {
    const id = Date.now();
    const item: ProjectDrawingEntity = { ...data, id, createdAt: id, updatedAt: id };
    this.drawings.push(item);
    save('drawings', this.drawings);
    this.notify();
    return item;
  }

  updateDrawing(id: number, data: Partial<ProjectDrawingEntity>) {
    this.drawings = this.drawings.map((d) => (d.id === id ? { ...d, ...data, updatedAt: Date.now() } : d));
    save('drawings', this.drawings);
    this.notify();
  }

  addDrawingRevision(data: Omit<DrawingRevisionEntity, 'id' | 'createdAt'>) {
    const id = Date.now();
    const item: DrawingRevisionEntity = { ...data, id, createdAt: id };
    this.drawingRevisions.push(item);
    save('drawingRevisions', this.drawingRevisions);
    this.notify();
    return item;
  }

  // --- Siteman: Site Documents ---
  addSiteDocument(data: Omit<SiteDocumentEntity, 'id' | 'createdAt'>) {
    const id = Date.now();
    const item: SiteDocumentEntity = { ...data, id, createdAt: id };
    this.siteDocuments.push(item);
    save('siteDocuments', this.siteDocuments);
    this.notify();
    return item;
  }

  // --- Siteman: Purchase Orders (Quantity & Specification only) ---
  addMaterialOrder(
    data: Omit<MaterialOrderEntity, 'id' | 'createdAt'>,
    lines: Omit<MaterialOrderLineEntity, 'id' | 'orderId'>[]
  ) {
    const id = Date.now();
    const order: MaterialOrderEntity = { ...data, id, createdAt: id };
    this.materialOrders.push(order);
    save('materialOrders', this.materialOrders);

    lines.forEach((l, idx) => {
      this.materialOrderLines.push({ ...l, id: id + idx + 1, orderId: id });
    });
    save('materialOrderLines', this.materialOrderLines);

    this.notify();
    return order;
  }

  issueMaterialOrder(id: number) {
    this.materialOrders = this.materialOrders.map((o) =>
      o.id === id ? { ...o, status: 'ISSUED', issuedAt: Date.now() } : o
    );
    save('materialOrders', this.materialOrders);
    this.notify();
  }

  deleteMaterialOrder(id: number) {
    this.materialOrders = this.materialOrders.filter((o) => o.id !== id);
    this.materialOrderLines = this.materialOrderLines.filter((l) => l.orderId !== id);
    save('materialOrders', this.materialOrders);
    save('materialOrderLines', this.materialOrderLines);
    this.notify();
  }

  // --- Project Decisions & Tasks ---
  addDecision(data: Omit<ProjectDecisionEntity, 'id' | 'createdAt' | 'updatedAt'>) {
    const id = Date.now();
    const item: ProjectDecisionEntity = { ...data, id, createdAt: id, updatedAt: id };
    this.projectDecisions.push(item);
    save('projectDecisions', this.projectDecisions);
    this.notify();
    return item;
  }

  addTask(data: Omit<ProjectTaskEntity, 'id' | 'createdAt'>) {
    const id = Date.now();
    const item: ProjectTaskEntity = { ...data, id, createdAt: id };
    this.projectTasks.push(item);
    save('projectTasks', this.projectTasks);
    this.notify();
    return item;
  }

  toggleTaskStatus(id: number) {
    this.projectTasks = this.projectTasks.map((t) =>
      t.id === id ? { ...t, status: t.status === 'COMPLETED' ? 'OPEN' : 'COMPLETED' } : t
    );
    save('projectTasks', this.projectTasks);
    this.notify();
  }

  // --- Database Backup & Restore ---
  exportDatabase() {
    return {
      version: 27,
      exportedAt: Date.now(),
      companyProfile: this.companyProfile,
      clients: this.clients,
      projects: this.projects,
      floors: this.floors,
      rooms: this.rooms,
      contractors: this.contractors,
      contractorQualifiedItems: this.contractorQualifiedItems,
      standardWorkItems: this.standardWorkItems,
      measurementSheets: this.measurementSheets,
      measurements: this.measurements,
      dailyReports: this.dailyReports,
      siteIssues: this.siteIssues,
      coordinationItems: this.coordinationItems,
      drawings: this.drawings,
      drawingRevisions: this.drawingRevisions,
      siteDocuments: this.siteDocuments,
      materialOrders: this.materialOrders,
      materialOrderLines: this.materialOrderLines,
      projectDecisions: this.projectDecisions,
      projectTasks: this.projectTasks,
      projectConsultants: this.projectConsultants,
    };
  }

  importDatabase(data: any) {
    if (!data || typeof data !== 'object') throw new Error('Invalid database format');
    if (data.companyProfile) {
      this.companyProfile = data.companyProfile;
      save('companyProfile', this.companyProfile);
    }
    if (Array.isArray(data.clients)) {
      this.clients = data.clients;
      save('clients', this.clients);
    }
    if (Array.isArray(data.projects)) {
      this.projects = data.projects;
      save('projects', this.projects);
    }
    if (Array.isArray(data.floors)) {
      this.floors = data.floors;
      save('floors', this.floors);
    }
    if (Array.isArray(data.rooms)) {
      this.rooms = data.rooms;
      save('rooms', this.rooms);
    }
    if (Array.isArray(data.contractors)) {
      this.contractors = data.contractors;
      save('contractors', this.contractors);
    }
    if (Array.isArray(data.contractorQualifiedItems)) {
      this.contractorQualifiedItems = data.contractorQualifiedItems;
      save('contractorQualifiedItems', this.contractorQualifiedItems);
    }
    if (Array.isArray(data.standardWorkItems)) {
      this.standardWorkItems = data.standardWorkItems;
      save('standardWorkItems', this.standardWorkItems);
    }
    if (Array.isArray(data.measurementSheets)) {
      this.measurementSheets = data.measurementSheets;
      save('measurementSheets', this.measurementSheets);
    }
    if (Array.isArray(data.measurements)) {
      this.measurements = data.measurements;
      save('measurements', this.measurements);
    }
    if (Array.isArray(data.dailyReports)) {
      this.dailyReports = data.dailyReports;
      save('dailyReports', this.dailyReports);
    }
    if (Array.isArray(data.siteIssues)) {
      this.siteIssues = data.siteIssues;
      save('siteIssues', this.siteIssues);
    }
    if (Array.isArray(data.coordinationItems)) {
      this.coordinationItems = data.coordinationItems;
      save('coordinationItems', this.coordinationItems);
    }
    if (Array.isArray(data.drawings)) {
      this.drawings = data.drawings;
      save('drawings', this.drawings);
    }
    if (Array.isArray(data.drawingRevisions)) {
      this.drawingRevisions = data.drawingRevisions;
      save('drawingRevisions', this.drawingRevisions);
    }
    if (Array.isArray(data.siteDocuments)) {
      this.siteDocuments = data.siteDocuments;
      save('siteDocuments', this.siteDocuments);
    }
    if (Array.isArray(data.materialOrders)) {
      this.materialOrders = data.materialOrders;
      save('materialOrders', this.materialOrders);
    }
    if (Array.isArray(data.materialOrderLines)) {
      this.materialOrderLines = data.materialOrderLines;
      save('materialOrderLines', this.materialOrderLines);
    }
    this.notify();
  }

  resetToDefault() {
    localStorage.clear();
    this.companyProfile = initialCompanyProfile;
    this.clients = initialClients;
    this.projects = initialProjects;
    this.floors = initialFloors;
    this.rooms = initialRooms;
    this.contractors = initialContractors;
    this.contractorQualifiedItems = initialContractorQualifiedItems;
    this.standardWorkItems = initialStandardWorkItems;
    this.measurementSheets = initialMeasurementSheets;
    this.measurements = initialMeasurements;
    this.dailyReports = initialDailyReports;
    this.siteIssues = initialSiteIssues;
    this.coordinationItems = initialCoordinationItems;
    this.drawings = initialDrawings;
    this.drawingRevisions = initialDrawingRevisions;
    this.siteDocuments = initialSiteDocuments;
    this.materialOrders = initialMaterialOrders;
    this.materialOrderLines = initialMaterialOrderLines;
    this.projectDecisions = initialProjectDecisions;
    this.projectTasks = initialTasks;
    this.projectConsultants = initialConsultants;
    this.notify();
  }
}

export const store = new PracticeStore();
