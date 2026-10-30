import React, { useState } from 'react';
import { store } from '../data/storage';
import {
  ProjectEntity,
  ClientEntity,
  ContractorEntity,
  HomeTab,
  ItemMasterEntity,
  CalculationType,
} from '../types';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  Building2,
  Users,
  BookOpen,
  Briefcase,
  Plus,
  Search,
  Download,
  Upload,
  CheckCircle,
  AlertCircle,
  FileText,
  MapPin,
  Compass,
  ArrowRight,
  Shield,
  Layers,
  Database,
  Activity,
  Trash2,
} from 'lucide-react';

interface PortfolioScreenProps {
  onSelectProject: (project: ProjectEntity) => void;
}

export const PortfolioScreen: React.FC<PortfolioScreenProps> = ({ onSelectProject }) => {
  const [activeTab, setActiveTab] = useState<HomeTab>('PROJECTS');
  const [directoryTab, setDirectoryTab] = useState<'CLIENTS' | 'CONTRACTORS'>('CLIENTS');

  // Modals
  const [showNewProjectModal, setShowNewProjectModal] = useState(false);
  const [showNewClientModal, setShowNewClientModal] = useState(false);
  const [showNewContractorModal, setShowNewContractorModal] = useState(false);
  const [showNewCatalogModal, setShowNewCatalogModal] = useState(false);

  // Search & Filter
  const [searchQuery, setSearchQuery] = useState('');
  const [catalogTradeFilter, setCatalogTradeFilter] = useState('ALL');

  // Forms
  const [projForm, setProjForm] = useState({
    name: '',
    projectCode: `PRJ-2026-${(store.projects.length + 1).toString().padStart(2, '0')}`,
    client: store.clients[0]?.name || '',
    clientId: store.clients[0]?.id || 1,
    siteLocation: '',
    projectType: 'Residential',
    plotArea: 400,
    builtUpArea: 550,
    description: '',
    architectInCharge: store.companyProfile.principalName || 'Ar. Vishwabhiram Holagundi',
  });

  const [clientForm, setClientForm] = useState({
    name: '',
    clientType: 'Individual',
    contactPerson: '',
    email: '',
    contactNo: '',
    address: '',
    correspondenceAddress: '',
    preferredCommunication: 'Email',
    notes: '',
  });

  const [contractorForm, setContractorForm] = useState({
    name: '',
    contractorType: 'Civil',
    contactNo: '',
    phone: '',
    address: '',
  });

  const [catalogForm, setCatalogForm] = useState({
    name: '',
    workType: 'Concrete & Structure',
    unit: 'm³',
    calculationType: 'VOLUME' as CalculationType,
    specification: '',
  });

  // Practice Profile Form
  const [profileForm, setProfileForm] = useState({ ...store.companyProfile });

  // Handle Project Creation per project creation contract
  const handleCreateProject = (e: React.FormEvent) => {
    e.preventDefault();
    if (!projForm.name.trim()) return;

    const newProj = store.addProject({
      name: projForm.name,
      projectCode: projForm.projectCode,
      client: projForm.client,
      clientId: projForm.clientId,
      siteLocation: projForm.siteLocation,
      projectType: projForm.projectType,
      status: 'ACTIVE',
      description: projForm.description,
      architectInCharge: projForm.architectInCharge,
      startDate: Date.now(),
      targetCompletionDate: Date.now() + 180 * 86400000,
      plotArea: projForm.plotArea,
      builtUpArea: projForm.builtUpArea,
      areaUnit: 'm²',
    });

    setShowNewProjectModal(false);
    onSelectProject(newProj);
  };

  const handleCreateClient = (e: React.FormEvent) => {
    e.preventDefault();
    if (!clientForm.name.trim()) return;

    store.addClient({
      name: clientForm.name,
      clientType: clientForm.clientType,
      contactPerson: clientForm.contactPerson || clientForm.name,
      email: clientForm.email,
      contactNo: clientForm.contactNo,
      address: clientForm.address,
      correspondenceAddress: clientForm.correspondenceAddress || clientForm.address,
      preferredCommunication: clientForm.preferredCommunication,
      notes: clientForm.notes,
    });

    setShowNewClientModal(false);
    setClientForm({
      name: '',
      clientType: 'Individual',
      contactPerson: '',
      email: '',
      contactNo: '',
      address: '',
      correspondenceAddress: '',
      preferredCommunication: 'Email',
      notes: '',
    });
  };

  const handleCreateContractor = (e: React.FormEvent) => {
    e.preventDefault();
    if (!contractorForm.name.trim()) return;

    const newC = store.addContractor({
      projectId: 0,
      name: contractorForm.name,
      contractorType: contractorForm.contractorType,
      contactNo: contractorForm.contactNo,
      phone: contractorForm.phone || contractorForm.contactNo,
      address: contractorForm.address,
    });

    // Seed default qualified items based on trade
    const sampleItems = store.standardWorkItems.filter((i) => i.workType.includes(contractorForm.contractorType));
    sampleItems.forEach((si) => {
      store.addContractorQualifiedItem({
        contractorId: newC.id,
        workType: si.workType,
        itemName: si.name,
        uom: si.unit,
        calculationType: si.calculationType,
      });
    });

    setShowNewContractorModal(false);
    setContractorForm({
      name: '',
      contractorType: 'Civil',
      contactNo: '',
      phone: '',
      address: '',
    });
  };

  const handleCreateCatalogItem = (e: React.FormEvent) => {
    e.preventDefault();
    if (!catalogForm.name.trim()) return;

    store.addCatalogItem({
      itemCode: `CUSTOM-${Date.now().toString(16).toUpperCase()}`,
      name: catalogForm.name,
      workType: catalogForm.workType,
      unit: catalogForm.unit,
      calculationType: catalogForm.calculationType,
      specification: catalogForm.specification,
      sourceName: 'User Master Library',
      sourceItemCode: '',
      isPredefined: false,
      isActive: true,
    });

    setShowNewCatalogModal(false);
    setCatalogForm({
      name: '',
      workType: 'Concrete & Structure',
      unit: 'm³',
      calculationType: 'VOLUME',
      specification: '',
    });
  };

  const handleSaveProfile = (e: React.FormEvent) => {
    e.preventDefault();
    store.updateCompanyProfile(profileForm);
    alert('Company profile updated successfully.');
  };

  const handleExportDB = () => {
    const data = store.exportDatabase();
    ExportHelper.downloadJSON(
      `ArchiMan_Practice_Backup_${new Date().toISOString().slice(0, 10)}.archimandb.json`,
      data
    );
  };

  const handleImportDB = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const json = JSON.parse(event.target?.result as string);
        store.importDatabase(json);
        alert('Practice database restored successfully.');
      } catch (err) {
        alert('Failed to parse database file: ' + err);
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Portfolio Top Bar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-indigo-600 text-white rounded-xl shadow-xs">
                <Compass className="w-5 h-5" />
              </div>
              <div>
                <h1 className="text-lg font-bold text-slate-900 leading-tight">
                  {store.companyProfile.practiceName || 'ArchiMan'}
                </h1>
                <div className="text-[11px] text-slate-500 flex items-center gap-1.5">
                  <span>CoA: {store.companyProfile.coaRegistrationNumber || 'Architect Practice'}</span>
                  <span>•</span>
                  <span>Schema 27 Local DB</span>
                </div>
              </div>
            </div>

            <div className="flex items-center gap-2">
              {activeTab === 'PROJECTS' && (
                <button
                  type="button"
                  onClick={() => setShowNewProjectModal(true)}
                  className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
                >
                  <Plus className="w-4 h-4" /> New Project
                </button>
              )}
              {activeTab === 'DIRECTORY' && directoryTab === 'CLIENTS' && (
                <button
                  type="button"
                  onClick={() => setShowNewClientModal(true)}
                  className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
                >
                  <Plus className="w-4 h-4" /> Add Client
                </button>
              )}
              {activeTab === 'DIRECTORY' && directoryTab === 'CONTRACTORS' && (
                <button
                  type="button"
                  onClick={() => setShowNewContractorModal(true)}
                  className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
                >
                  <Plus className="w-4 h-4" /> Add Contractor
                </button>
              )}
              {activeTab === 'WORK_LIBRARY' && (
                <button
                  type="button"
                  onClick={() => setShowNewCatalogModal(true)}
                  className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
                >
                  <Plus className="w-4 h-4" /> Add Work Item
                </button>
              )}
            </div>
          </div>

          {/* Top-Level Portfolio Navigation Tabs */}
          <div className="flex overflow-x-auto gap-4 -mb-px">
            {[
              { id: 'PROJECTS', label: 'Projects', icon: Building2, count: store.projects.length },
              { id: 'DIRECTORY', label: 'Contacts', icon: Users, count: store.clients.length + store.contractors.length },
              { id: 'WORK_LIBRARY', label: 'Library', icon: BookOpen, count: store.standardWorkItems.length },
              { id: 'PRACTICE', label: 'Practice', icon: Briefcase },
            ].map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveTab(tab.id as HomeTab)}
                  className={`flex items-center gap-2 px-4 py-3 text-xs font-semibold border-b-2 transition whitespace-nowrap ${
                    isActive
                      ? 'border-indigo-600 text-indigo-600'
                      : 'border-transparent text-slate-600 hover:text-slate-900 hover:border-slate-300'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{tab.label}</span>
                  {tab.count !== undefined && (
                    <span
                      className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                        isActive ? 'bg-indigo-100 text-indigo-800' : 'bg-slate-100 text-slate-600'
                      }`}
                    >
                      {tab.count}
                    </span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 w-full">
        {/* TAB 1: PROJECTS */}
        {activeTab === 'PROJECTS' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900">Project Portfolio</h2>
                <p className="text-xs text-slate-500">
                  Select a project to enter its workspace, measurement book, and Siteman hub.
                </p>
              </div>

              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="text"
                  placeholder="Search projects..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
              {store.projects
                .filter(
                  (p) =>
                    p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    p.projectCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
                    p.client.toLowerCase().includes(searchQuery.toLowerCase())
                )
                .map((proj) => {
                  const pFloors = store.floors.filter((f) => f.projectId === proj.id);
                  const pSheets = store.measurementSheets.filter((s) => s.projectId === proj.id);
                  const pMeasurements = store.measurements.filter((m) => m.projectId === proj.id);

                  return (
                    <div
                      key={proj.id}
                      onClick={() => onSelectProject(proj)}
                      className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm hover:border-indigo-400 hover:shadow-md transition cursor-pointer flex flex-col justify-between group"
                    >
                      <div className="space-y-3">
                        <div className="flex items-center justify-between">
                          <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                            {proj.projectCode}
                          </span>
                          <span
                            className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                              proj.status === 'ACTIVE'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-slate-100 text-slate-600'
                            }`}
                          >
                            {proj.status}
                          </span>
                        </div>

                        <div>
                          <h3 className="text-base font-bold text-slate-900 group-hover:text-indigo-600 transition">
                            {proj.name}
                          </h3>
                          <div className="text-xs text-slate-500 mt-0.5 flex items-center gap-1">
                            <MapPin className="w-3 h-3 text-slate-400 shrink-0" />
                            <span className="truncate">{proj.siteLocation || 'Location unspecified'}</span>
                          </div>
                        </div>

                        <div className="bg-slate-50 p-2.5 rounded-xl border border-slate-100 grid grid-cols-2 gap-2 text-xs">
                          <div>
                            <span className="text-slate-400 text-[10px] block">Built-up Area</span>
                            <span className="font-bold text-slate-800">{proj.builtUpArea || 0} m²</span>
                          </div>
                          <div>
                            <span className="text-slate-400 text-[10px] block">Type</span>
                            <span className="font-bold text-slate-800">{proj.projectType}</span>
                          </div>
                        </div>

                        <div className="text-xs text-slate-500">
                          Client: <strong className="text-slate-700">{proj.client}</strong>
                        </div>
                      </div>

                      <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                        <span>{pSheets.length} M-Book sheets ({pMeasurements.length} records)</span>
                        <span className="text-indigo-600 font-bold flex items-center gap-1 group-hover:translate-x-1 transition">
                          Open Hub <ArrowRight className="w-3.5 h-3.5" />
                        </span>
                      </div>
                    </div>
                  );
                })}
            </div>
          </div>
        )}

        {/* TAB 2: CONTACTS (Directory) */}
        {activeTab === 'DIRECTORY' && (
          <div className="space-y-6">
            <div className="flex border-b border-slate-200 gap-6">
              <button
                type="button"
                onClick={() => setDirectoryTab('CLIENTS')}
                className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
                  directoryTab === 'CLIENTS'
                    ? 'border-indigo-600 text-indigo-600'
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                <span>Clients Directory ({store.clients.length})</span>
              </button>

              <button
                type="button"
                onClick={() => setDirectoryTab('CONTRACTORS')}
                className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
                  directoryTab === 'CONTRACTORS'
                    ? 'border-indigo-600 text-indigo-600'
                    : 'border-transparent text-slate-500 hover:text-slate-800'
                }`}
              >
                <span>Contractors & Trades ({store.contractors.length})</span>
              </button>
            </div>

            {directoryTab === 'CLIENTS' ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {store.clients.map((c) => (
                  <div key={c.id} className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3">
                    <div className="flex items-center justify-between border-b pb-2">
                      <h3 className="font-bold text-sm text-slate-900">{c.name}</h3>
                      <span className="text-[10px] font-semibold px-2 py-0.5 bg-slate-100 text-slate-600 rounded">
                        {c.clientType}
                      </span>
                    </div>

                    <div className="space-y-1.5 text-xs text-slate-600">
                      <div>Contact Person: <strong className="text-slate-800">{c.contactPerson}</strong></div>
                      <div>Phone: {c.contactNo}</div>
                      <div>Email: {c.email}</div>
                      <div>Address: {c.address}</div>
                    </div>

                    {c.notes && (
                      <p className="text-[11px] text-slate-500 bg-slate-50 p-2 rounded italic">
                        "{c.notes}"
                      </p>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {store.contractors.map((c) => {
                  const qualified = store.contractorQualifiedItems.filter((q) => q.contractorId === c.id);
                  return (
                    <div key={c.id} className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3">
                      <div className="flex items-center justify-between border-b pb-2">
                        <h3 className="font-bold text-sm text-slate-900">{c.name}</h3>
                        <span className="text-[10px] font-bold px-2 py-0.5 bg-indigo-50 text-indigo-700 rounded-full">
                          {c.contractorType}
                        </span>
                      </div>

                      <div className="space-y-1 text-xs text-slate-600">
                        <div>Phone: {c.contactNo || c.phone}</div>
                        <div>Address: {c.address || 'Local site depot'}</div>
                      </div>

                      <div>
                        <div className="text-[11px] font-semibold text-slate-500 mb-1">
                          Qualified Items ({qualified.length}):
                        </div>
                        <div className="space-y-1 max-h-36 overflow-y-auto pr-1">
                          {qualified.map((q) => (
                            <div
                              key={q.id}
                              className="p-1 bg-slate-50 rounded border border-slate-100 flex items-center justify-between text-xs"
                            >
                              <span className="text-slate-700 truncate max-w-[180px]">{q.itemName}</span>
                              <span className="text-[10px] font-mono text-slate-500">{q.uom}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {/* TAB 3: WORK LIBRARY (PWD SR Master Data) */}
        {activeTab === 'WORK_LIBRARY' && (
          <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <div>
                <h2 className="text-xl font-bold text-slate-900">PWD SR Master Work Catalogue</h2>
                <p className="text-xs text-slate-500">
                  Standard schedule of rates items with canonical formulas (Nos, Length, Area, Wall Plaster, Volume).
                </p>
              </div>

              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                <input
                  type="text"
                  placeholder="Search work items..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="w-full pl-9 pr-3 py-1.5 bg-white border border-slate-200 rounded-lg text-xs"
                />
              </div>
            </div>

            <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider text-[11px]">
                    <tr>
                      <th className="px-4 py-3">Code</th>
                      <th className="px-4 py-3">Item Name</th>
                      <th className="px-4 py-3">Trade / Work Type</th>
                      <th className="px-4 py-3">Unit</th>
                      <th className="px-4 py-3">Formula</th>
                      <th className="px-4 py-3">Specification</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {store.standardWorkItems
                      .filter(
                        (item) =>
                          item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          item.workType.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          item.itemCode.toLowerCase().includes(searchQuery.toLowerCase())
                      )
                      .map((item) => (
                        <tr key={item.id} className="hover:bg-slate-50/70">
                          <td className="px-4 py-3 font-mono font-bold text-indigo-700">{item.itemCode}</td>
                          <td className="px-4 py-3 font-bold text-slate-900">{item.name}</td>
                          <td className="px-4 py-3 text-slate-600">{item.workType}</td>
                          <td className="px-4 py-3 font-semibold text-slate-800">{item.unit}</td>
                          <td className="px-4 py-3">
                            <span className="font-mono text-[10px] bg-slate-100 text-slate-700 px-2 py-0.5 rounded">
                              {item.calculationType}
                            </span>
                          </td>
                          <td className="px-4 py-3 text-slate-500 max-w-sm truncate">{item.specification || '-'}</td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: PRACTICE (Company Profile, Backup, Dashboard) */}
        {activeTab === 'PRACTICE' && (
          <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Practice Profile Form */}
              <div className="lg:col-span-2 bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-4">
                <div className="flex items-center justify-between border-b pb-3">
                  <div>
                    <h3 className="text-base font-bold text-slate-900">Practice Identity & CoA Registration</h3>
                    <p className="text-xs text-slate-500">
                      Official credentials displayed on measurement books, drawings, and transmittals.
                    </p>
                  </div>
                  <Shield className="w-5 h-5 text-indigo-600" />
                </div>

                <form onSubmit={handleSaveProfile} className="space-y-4 text-xs">
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Practice / Firm Name</label>
                      <input
                        type="text"
                        required
                        value={profileForm.practiceName}
                        onChange={(e) => setProfileForm({ ...profileForm, practiceName: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                      />
                    </div>
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Legal Entity Name</label>
                      <input
                        type="text"
                        value={profileForm.legalName}
                        onChange={(e) => setProfileForm({ ...profileForm, legalName: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Council of Architecture (CoA) No.</label>
                      <input
                        type="text"
                        value={profileForm.coaRegistrationNumber}
                        onChange={(e) => setProfileForm({ ...profileForm, coaRegistrationNumber: e.target.value })}
                        placeholder="e.g. CA/2014/61234"
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs font-mono"
                      />
                    </div>
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Principal Architect Name</label>
                      <input
                        type="text"
                        value={profileForm.principalName}
                        onChange={(e) => setProfileForm({ ...profileForm, principalName: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Principal Qualifications</label>
                      <input
                        type="text"
                        value={profileForm.principalQualification}
                        onChange={(e) => setProfileForm({ ...profileForm, principalQualification: e.target.value })}
                        placeholder="B.Arch, AIIA"
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                      />
                    </div>
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">PAN Number</label>
                      <input
                        type="text"
                        value={profileForm.pan}
                        onChange={(e) => setProfileForm({ ...profileForm, pan: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs font-mono uppercase"
                      />
                    </div>
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">GSTIN</label>
                      <input
                        type="text"
                        value={profileForm.gstin}
                        onChange={(e) => setProfileForm({ ...profileForm, gstin: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs font-mono uppercase"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Official Email</label>
                      <input
                        type="email"
                        value={profileForm.email}
                        onChange={(e) => setProfileForm({ ...profileForm, email: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                      />
                    </div>
                    <div>
                      <label className="block font-semibold text-slate-700 mb-1">Phone</label>
                      <input
                        type="text"
                        value={profileForm.phone}
                        onChange={(e) => setProfileForm({ ...profileForm, phone: e.target.value })}
                        className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block font-semibold text-slate-700 mb-1">Practice Studio Address</label>
                    <textarea
                      rows={2}
                      value={profileForm.address}
                      onChange={(e) => setProfileForm({ ...profileForm, address: e.target.value })}
                      className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                    />
                  </div>

                  <div className="flex justify-end pt-2">
                    <button
                      type="submit"
                      className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-bold text-xs shadow-sm"
                    >
                      Save Practice Profile
                    </button>
                  </div>
                </form>
              </div>

              {/* Database Portability & Diagnostics */}
              <div className="space-y-6">
                {/* Backup & Restore */}
                <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-4">
                  <div className="flex items-center gap-2 border-b pb-2">
                    <Database className="w-5 h-5 text-indigo-600" />
                    <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
                      Database Portability
                    </h3>
                  </div>

                  <p className="text-xs text-slate-600 leading-relaxed">
                    Export complete practice database (<code>.archimandb.json</code>) including all projects,
                    measurements, reports, and drawings for backup and transfer between devices.
                  </p>

                  <div className="space-y-2">
                    <button
                      type="button"
                      onClick={handleExportDB}
                      className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-2 shadow-xs"
                    >
                      <Download className="w-4 h-4" /> Export Company Backup
                    </button>

                    <label className="w-full py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold flex items-center justify-center gap-2 cursor-pointer transition">
                      <Upload className="w-4 h-4" /> Restore from .archimandb
                      <input
                        type="file"
                        accept=".json,.archimandb"
                        onChange={handleImportDB}
                        className="hidden"
                      />
                    </label>

                    <button
                      type="button"
                      onClick={() => {
                        if (confirm('Are you sure you want to reset the practice database to initial seed data?')) {
                          store.resetToDefault();
                          setProfileForm({ ...store.companyProfile });
                          alert('Database reset to defaults.');
                        }
                      }}
                      className="w-full py-2 text-rose-600 hover:bg-rose-50 rounded-lg text-xs font-semibold transition"
                    >
                      Reset to Default Demo Database
                    </button>
                  </div>
                </div>

                {/* Diagnostics Summary */}
                <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
                  <div className="flex items-center gap-2 border-b pb-2">
                    <Activity className="w-5 h-5 text-indigo-600" />
                    <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
                      Practice Diagnostics
                    </h3>
                  </div>

                  <div className="space-y-2 text-xs">
                    <div className="flex justify-between py-1 border-b border-slate-100">
                      <span className="text-slate-500">Database Schema:</span>
                      <span className="font-mono font-bold text-indigo-700">Room Schema 27 (Parity)</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-100">
                      <span className="text-slate-500">Total Projects:</span>
                      <span className="font-bold text-slate-800">{store.projects.length}</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-100">
                      <span className="text-slate-500">M-Book Measurements:</span>
                      <span className="font-bold text-slate-800">{store.measurements.length}</span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-100">
                      <span className="text-slate-500">Open Snags / NCRs:</span>
                      <span className="font-bold text-amber-700">
                        {store.siteIssues.filter((i) => i.status !== 'CLOSED').length}
                      </span>
                    </div>
                    <div className="flex justify-between py-1 border-b border-slate-100">
                      <span className="text-slate-500">Pending RFIs:</span>
                      <span className="font-bold text-slate-800">
                        {store.coordinationItems.filter((c) => c.status !== 'CLOSED' && c.status !== 'ANSWERED').length}
                      </span>
                    </div>
                    <div className="flex justify-between py-1">
                      <span className="text-slate-500">Product Boundary:</span>
                      <span className="text-emerald-700 font-semibold">Strict Quantity-Only Verified</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Modal: New Project */}
      {showNewProjectModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="text-base font-bold text-slate-800">Create New Project</h3>
                <p className="text-xs text-slate-500">Initializes Ground Floor baseline automatically</p>
              </div>
              <button
                type="button"
                onClick={() => setShowNewProjectModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateProject} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Project Name</label>
                  <input
                    type="text"
                    required
                    value={projForm.name}
                    onChange={(e) => setProjForm({ ...projForm, name: e.target.value })}
                    placeholder="e.g. Palm Meadows Villa"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Project Code</label>
                  <input
                    type="text"
                    required
                    value={projForm.projectCode}
                    onChange={(e) => setProjForm({ ...projForm, projectCode: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs font-mono font-bold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Client</label>
                  <select
                    value={projForm.client}
                    onChange={(e) => {
                      const c = store.clients.find((x) => x.name === e.target.value);
                      setProjForm({ ...projForm, client: e.target.value, clientId: c?.id || 1 });
                    }}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white text-xs"
                  >
                    {store.clients.map((c) => (
                      <option key={c.id} value={c.name}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Project Type</label>
                  <select
                    value={projForm.projectType}
                    onChange={(e) => setProjForm({ ...projForm, projectType: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white text-xs"
                  >
                    <option value="Residential">Residential</option>
                    <option value="Commercial">Commercial</option>
                    <option value="Institutional">Institutional</option>
                    <option value="Hospitality">Hospitality</option>
                    <option value="Industrial">Industrial</option>
                    <option value="Urban Design">Urban Design</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Site Address / Location</label>
                <input
                  type="text"
                  required
                  value={projForm.siteLocation}
                  onChange={(e) => setProjForm({ ...projForm, siteLocation: e.target.value })}
                  placeholder="Plot number, road, locality, city"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Built-Up Area (m²)</label>
                  <input
                    type="number"
                    value={projForm.builtUpArea}
                    onChange={(e) => setProjForm({ ...projForm, builtUpArea: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Plot Area (m²)</label>
                  <input
                    type="number"
                    value={projForm.plotArea}
                    onChange={(e) => setProjForm({ ...projForm, plotArea: parseFloat(e.target.value) || 0 })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Brief Description</label>
                <textarea
                  rows={2}
                  value={projForm.description}
                  onChange={(e) => setProjForm({ ...projForm, description: e.target.value })}
                  placeholder="Project scope, design intent, client preferences..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowNewProjectModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Create Project
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: New Client */}
      {showNewClientModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-slate-800">Add New Client</h3>
            <form onSubmit={handleCreateClient} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Client Name</label>
                <input
                  type="text"
                  required
                  value={clientForm.name}
                  onChange={(e) => setClientForm({ ...clientForm, name: e.target.value })}
                  placeholder="e.g. Dr. Ramesh Gupta"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Phone</label>
                  <input
                    type="text"
                    value={clientForm.contactNo}
                    onChange={(e) => setClientForm({ ...clientForm, contactNo: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Email</label>
                  <input
                    type="email"
                    value={clientForm.email}
                    onChange={(e) => setClientForm({ ...clientForm, email: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Address</label>
                <input
                  type="text"
                  value={clientForm.address}
                  onChange={(e) => setClientForm({ ...clientForm, address: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewClientModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Save Client
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: New Contractor */}
      {showNewContractorModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-slate-800">Add New Contractor</h3>
            <form onSubmit={handleCreateContractor} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Contractor / Agency Name</label>
                <input
                  type="text"
                  required
                  value={contractorForm.name}
                  onChange={(e) => setContractorForm({ ...contractorForm, name: e.target.value })}
                  placeholder="e.g. Royal Brickworks & Masonry"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Trade Specialization</label>
                <select
                  value={contractorForm.contractorType}
                  onChange={(e) => setContractorForm({ ...contractorForm, contractorType: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white text-xs"
                >
                  <option value="Civil">Civil Works</option>
                  <option value="Electrical">Electrical Works</option>
                  <option value="Plumbing">Plumbing Works</option>
                  <option value="Carpenter">Carpentry & Joinery</option>
                  <option value="Painter">Painting & Finishes</option>
                  <option value="Flooring & Cladding">Flooring & Cladding</option>
                  <option value="Waterproofing">Waterproofing</option>
                  <option value="HVAC">HVAC Works</option>
                  <option value="Fabrication">Fabrication</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Phone Number</label>
                <input
                  type="text"
                  value={contractorForm.contactNo}
                  onChange={(e) => setContractorForm({ ...contractorForm, contactNo: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewContractorModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Save Contractor
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Work Item */}
      {showNewCatalogModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-slate-800">Add Master Work Item</h3>
            <form onSubmit={handleCreateCatalogItem} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Item Name</label>
                <input
                  type="text"
                  required
                  value={catalogForm.name}
                  onChange={(e) => setCatalogForm({ ...catalogForm, name: e.target.value })}
                  placeholder="e.g. Micro-concrete pressure grouting"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Trade Category</label>
                  <input
                    type="text"
                    value={catalogForm.workType}
                    onChange={(e) => setCatalogForm({ ...catalogForm, workType: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Unit of Measure (UOM)</label>
                  <input
                    type="text"
                    value={catalogForm.unit}
                    onChange={(e) => setCatalogForm({ ...catalogForm, unit: e.target.value })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                  />
                </div>
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Calculation Formula</label>
                <select
                  value={catalogForm.calculationType}
                  onChange={(e) => setCatalogForm({ ...catalogForm, calculationType: e.target.value as any })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white text-xs"
                >
                  <option value="NOS">Nos (Count)</option>
                  <option value="RUNNING_LENGTH">Running Length (m)</option>
                  <option value="AREA">Area (m²)</option>
                  <option value="WALL_PLASTER">Wall Plaster (m² with deductions)</option>
                  <option value="VOLUME">Volume (m³)</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Technical Specification</label>
                <textarea
                  rows={2}
                  value={catalogForm.specification}
                  onChange={(e) => setCatalogForm({ ...catalogForm, specification: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowNewCatalogModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Save Item
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
