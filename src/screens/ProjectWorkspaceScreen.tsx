import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, ProjectSection, MoreSection, FloorEntity } from '../types';
import { RecordMeasurementView } from './RecordMeasurementView';
import { MeasurementBookView } from './MeasurementBookView';
import { SitemanView } from './SitemanView';
import { DrawingsView } from './DrawingsView';
import { SiteReportsView } from './SiteReportsView';
import { CoordinationView } from './CoordinationView';
import { PlanningView } from './PlanningView';
import {
  ArrowLeft,
  Building2,
  Calendar,
  Layers,
  Ruler,
  BookOpen,
  MoreHorizontal,
  Plus,
  Compass,
  FileCode2,
  ShieldAlert,
  MessageSquare,
  Scale,
  MapPin,
  CheckCircle,
  Briefcase,
  Users,
} from 'lucide-react';

interface ProjectWorkspaceScreenProps {
  project: ProjectEntity;
  onBackToPortfolio: () => void;
}

export const ProjectWorkspaceScreen: React.FC<ProjectWorkspaceScreenProps> = ({
  project,
  onBackToPortfolio,
}) => {
  const [activeSection, setActiveSection] = useState<ProjectSection>('OVERVIEW');
  const [activeMoreSection, setActiveMoreSection] = useState<MoreSection>('SITEMAN');

  // Add Floor Modal
  const [showFloorModal, setShowFloorModal] = useState(false);
  const [newFloorName, setNewFloorName] = useState('');

  // Add Room Modal
  const [showRoomModal, setShowRoomModal] = useState<number | null>(null);
  const [newRoomName, setNewRoomName] = useState('');

  const floors = store.floors.filter((f) => f.projectId === project.id);
  const rooms = store.rooms.filter((r) => r.projectId === project.id);
  const contractors = store.contractors.filter((c) => c.projectId === project.id || c.projectId === 0);
  const sheets = store.measurementSheets.filter((s) => s.projectId === project.id);
  const measurements = store.measurements.filter((m) => m.projectId === project.id);

  const handleAddFloor = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newFloorName.trim()) return;
    store.addFloor({
      projectId: project.id,
      name: newFloorName.trim(),
      orderIndex: floors.length,
    });
    setShowFloorModal(false);
    setNewFloorName('');
  };

  const handleAddRoom = (floorId: number) => {
    if (!newRoomName.trim()) return;
    store.addRoom({
      projectId: project.id,
      floorId,
      name: newRoomName.trim(),
      orderIndex: rooms.filter((r) => r.floorId === floorId).length,
    });
    setShowRoomModal(null);
    setNewRoomName('');
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Project Top Header Bar */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={onBackToPortfolio}
                className="p-2 -ml-2 text-slate-500 hover:text-slate-800 hover:bg-slate-100 rounded-lg transition"
                title="Back to Portfolio"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>

              <div className="flex items-center gap-2">
                <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                  {project.projectCode}
                </span>
                <h1 className="text-base sm:text-lg font-bold text-slate-900 truncate max-w-xs sm:max-w-md">
                  {project.name}
                </h1>
                <span
                  className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                    project.status === 'ACTIVE'
                      ? 'bg-emerald-100 text-emerald-800'
                      : 'bg-slate-100 text-slate-600'
                  }`}
                >
                  {project.status}
                </span>
              </div>
            </div>

            <div className="hidden sm:flex items-center text-xs text-slate-500 gap-4">
              <span>Client: <strong className="text-slate-700">{project.client}</strong></span>
              <span>Architect: <strong className="text-slate-700">{project.architectInCharge}</strong></span>
            </div>
          </div>

          {/* Project Primary Persistent Tabs */}
          <div className="flex overflow-x-auto gap-2 -mb-px">
            {[
              { id: 'OVERVIEW', label: 'Project', icon: Building2 },
              { id: 'WORK', label: 'Work', icon: Layers },
              { id: 'RECORD', label: 'Record', icon: Ruler },
              { id: 'M_BOOK', label: 'M-Book', icon: BookOpen, count: sheets.length },
              { id: 'MORE', label: 'More', icon: MoreHorizontal },
            ].map((tab) => {
              const Icon = tab.icon;
              const isActive = activeSection === tab.id;
              return (
                <button
                  key={tab.id}
                  type="button"
                  onClick={() => setActiveSection(tab.id as ProjectSection)}
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

      {/* Main Workspace Area */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 w-full">
        {/* TAB 1: OVERVIEW */}
        {activeSection === 'OVERVIEW' && (
          <div className="space-y-6">
            {/* Project Summary Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                <div className="text-xs text-slate-400 font-medium uppercase tracking-wider">Area & Scale</div>
                <div className="text-xl font-mono font-bold text-slate-900 mt-1">
                  {project.builtUpArea || 0} <span className="text-xs font-normal text-slate-500">m² BUA</span>
                </div>
                <div className="text-xs text-slate-500 mt-1">Plot Area: {project.plotArea || '-'} m²</div>
              </div>

              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                <div className="text-xs text-slate-400 font-medium uppercase tracking-wider">Site Location</div>
                <div className="text-sm font-semibold text-slate-900 mt-1 line-clamp-1 flex items-center gap-1">
                  <MapPin className="w-3.5 h-3.5 text-indigo-600 shrink-0" />
                  {project.siteLocation || 'Not specified'}
                </div>
                <div className="text-xs text-slate-500 mt-1">Type: {project.projectType}</div>
              </div>

              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                <div className="text-xs text-slate-400 font-medium uppercase tracking-wider">Floors & Levels</div>
                <div className="text-xl font-bold text-slate-900 mt-1">
                  {floors.length} <span className="text-xs font-normal text-slate-500">Levels defined</span>
                </div>
                <div className="text-xs text-slate-500 mt-1">{rooms.length} individual room spaces</div>
              </div>

              <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-sm">
                <div className="text-xs text-slate-400 font-medium uppercase tracking-wider">Measurement Book</div>
                <div className="text-xl font-bold text-slate-900 mt-1">
                  {measurements.length} <span className="text-xs font-normal text-slate-500">measurements</span>
                </div>
                <div className="text-xs text-slate-500 mt-1">{sheets.length} active contractor sheets</div>
              </div>
            </div>

            {/* Description & Floor Hierarchy */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Left Column: Project Profile Details */}
              <div className="lg:col-span-1 bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
                <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider border-b pb-2">
                  Project Profile
                </h3>

                <div className="space-y-3 text-xs">
                  <div>
                    <span className="text-slate-400 block">Description:</span>
                    <p className="text-slate-700 leading-relaxed mt-0.5">{project.description || 'No description provided.'}</p>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Client:</span>
                    <p className="font-semibold text-slate-800">{project.client}</p>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Architect In Charge:</span>
                    <p className="font-semibold text-slate-800">{project.architectInCharge}</p>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Target Completion:</span>
                    <p className="text-slate-700">
                      {project.targetCompletionDate
                        ? new Date(project.targetCompletionDate).toLocaleDateString(undefined, { dateStyle: 'medium' })
                        : 'On Schedule'}
                    </p>
                  </div>
                </div>
              </div>

              {/* Right Column: Levels & Room Space Hierarchy */}
              <div className="lg:col-span-2 bg-white p-5 rounded-xl border border-slate-200 shadow-sm space-y-4">
                <div className="flex items-center justify-between border-b pb-2">
                  <div>
                    <h3 className="text-sm font-bold text-slate-900 uppercase tracking-wider">
                      Building Levels & Rooms
                    </h3>
                    <p className="text-xs text-slate-500">
                      Created with Ground Floor baseline; add levels and spaces for measurement tagging.
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => setShowFloorModal(true)}
                    className="px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-lg text-xs font-semibold flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add Level
                  </button>
                </div>

                <div className="space-y-3">
                  {floors.map((floor) => {
                    const floorRooms = rooms.filter((r) => r.floorId === floor.id);

                    return (
                      <div key={floor.id} className="p-3.5 bg-slate-50 rounded-lg border border-slate-200 space-y-2">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-xs text-slate-900 flex items-center gap-1.5">
                            <Layers className="w-3.5 h-3.5 text-indigo-600" />
                            {floor.name}
                          </span>
                          <button
                            type="button"
                            onClick={() => setShowRoomModal(floor.id)}
                            className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                          >
                            <Plus className="w-3 h-3" /> Add Room Space
                          </button>
                        </div>

                        {floorRooms.length === 0 ? (
                          <div className="text-[11px] text-slate-400 italic pl-5">
                            No specific rooms added (measurements apply to Whole Floor).
                          </div>
                        ) : (
                          <div className="flex flex-wrap gap-1.5 pl-5">
                            {floorRooms.map((room) => (
                              <span
                                key={room.id}
                                className="px-2.5 py-1 bg-white border border-slate-200 text-slate-700 rounded text-xs font-medium"
                              >
                                {room.name}
                              </span>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 2: WORK (Work Catalogue & Scope) */}
        {activeSection === 'WORK' && (
          <div className="space-y-6">
            <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm flex items-center justify-between">
              <div>
                <h2 className="text-xl font-bold text-slate-900">Project Trade Works & Contractors</h2>
                <p className="text-xs text-slate-500 mt-1">
                  Active contractor trades and qualified work items available for measurement recording.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {contractors.map((contractor) => {
                const qualified = store.contractorQualifiedItems.filter((q) => q.contractorId === contractor.id);
                return (
                  <div key={contractor.id} className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3">
                    <div className="flex items-center justify-between border-b pb-2">
                      <h3 className="font-bold text-sm text-slate-900">{contractor.name}</h3>
                      <span className="text-[10px] font-semibold px-2 py-0.5 bg-indigo-50 text-indigo-700 rounded-full">
                        {contractor.contractorType}
                      </span>
                    </div>

                    <div className="text-xs text-slate-600">
                      <div>Contact: {contractor.contactNo || contractor.phone}</div>
                    </div>

                    <div>
                      <div className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider mb-1.5">
                        Qualified Trade Items ({qualified.length}):
                      </div>
                      <div className="space-y-1">
                        {qualified.map((q) => (
                          <div
                            key={q.id}
                            className="p-1.5 bg-slate-50 rounded border border-slate-100 flex items-center justify-between text-xs"
                          >
                            <span className="text-slate-800 font-medium">{q.itemName}</span>
                            <span className="text-[10px] font-mono bg-white px-1.5 py-0.5 rounded border border-slate-200 text-slate-600">
                              {q.uom}
                            </span>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* TAB 3: RECORD MEASUREMENT */}
        {activeSection === 'RECORD' && (
          <RecordMeasurementView
            project={project}
            onDone={() => setActiveSection('M_BOOK')}
          />
        )}

        {/* TAB 4: MEASUREMENT BOOK */}
        {activeSection === 'M_BOOK' && (
          <MeasurementBookView
            project={project}
            onNavigateToRecord={() => setActiveSection('RECORD')}
          />
        )}

        {/* TAB 5: MORE (Siteman, Drawings, Reports, Coordination, Planning) */}
        {activeSection === 'MORE' && (
          <div className="space-y-6">
            {/* Sub-navigation bar under More */}
            <div className="flex overflow-x-auto gap-2 bg-white p-2 rounded-xl border border-slate-200 shadow-sm">
              {[
                { id: 'SITEMAN', label: 'Siteman Hub', icon: Briefcase },
                { id: 'DRAWINGS', label: 'Drawings & Revisions', icon: FileCode2 },
                { id: 'REPORTS', label: 'Site Reports & Snags', icon: ShieldAlert },
                { id: 'COORDINATION', label: 'Coordination (RFIs)', icon: MessageSquare },
                { id: 'PLANNING', label: 'Planning & Decisions', icon: Compass },
              ].map((sub) => {
                const Icon = sub.icon;
                const isActive = activeMoreSection === sub.id;
                return (
                  <button
                    key={sub.id}
                    type="button"
                    onClick={() => setActiveMoreSection(sub.id as MoreSection)}
                    className={`flex items-center gap-2 px-3.5 py-2 rounded-lg text-xs font-semibold transition whitespace-nowrap ${
                      isActive
                        ? 'bg-indigo-600 text-white shadow-xs'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    <span>{sub.label}</span>
                  </button>
                );
              })}
            </div>

            {/* Selected More Destination */}
            {activeMoreSection === 'SITEMAN' && (
              <SitemanView
                project={project}
                onNavigateToMBook={() => setActiveSection('M_BOOK')}
              />
            )}
            {activeMoreSection === 'DRAWINGS' && <DrawingsView project={project} />}
            {activeMoreSection === 'REPORTS' && <SiteReportsView project={project} />}
            {activeMoreSection === 'COORDINATION' && <CoordinationView project={project} />}
            {activeMoreSection === 'PLANNING' && <PlanningView project={project} />}
          </div>
        )}
      </main>

      {/* Modal: Add Floor */}
      {showFloorModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-slate-800">Add Building Level</h3>
            <form onSubmit={handleAddFloor} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Level Name</label>
                <input
                  type="text"
                  required
                  autoFocus
                  value={newFloorName}
                  onChange={(e) => setNewFloorName(e.target.value)}
                  placeholder="e.g. Second Floor or Basement 1"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowFloorModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Add Level
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Room */}
      {showRoomModal !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-xl space-y-4">
            <h3 className="text-base font-bold text-slate-800">Add Room / Space</h3>
            <div className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Room Space Name</label>
                <input
                  type="text"
                  required
                  autoFocus
                  value={newRoomName}
                  onChange={(e) => setNewRoomName(e.target.value)}
                  placeholder="e.g. Kitchen, Conference Room, Balcony"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
                />
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <button
                  type="button"
                  onClick={() => setShowRoomModal(null)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() => handleAddRoom(showRoomModal)}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Add Space
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
