import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, CoordinationItemEntity, ProjectConsultantEntity } from '../types';
import { CoordinationWorkflow } from '../domain/SiteIssueWorkflow';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  MessageSquare,
  FileCheck,
  Send,
  Users,
  Plus,
  Download,
  AlertCircle,
  Clock,
  CheckCircle,
  FileText,
} from 'lucide-react';

interface CoordinationViewProps {
  project: ProjectEntity;
}

export const CoordinationView: React.FC<CoordinationViewProps> = ({ project }) => {
  const [activeTab, setActiveTab] = useState<'ITEMS' | 'CONSULTANTS'>('ITEMS');
  const [itemTypeFilter, setItemTypeFilter] = useState<'ALL' | 'RFI' | 'SUBMITTAL' | 'SITE_INSTRUCTION'>('ALL');
  const [showItemModal, setShowItemModal] = useState(false);
  const [showResponseModal, setShowResponseModal] = useState<CoordinationItemEntity | null>(null);
  const [responseText, setResponseText] = useState('');

  const [showConsultantModal, setShowConsultantModal] = useState(false);

  const coordinationItems = store.coordinationItems.filter((c) => c.projectId === project.id);
  const consultants = store.projectConsultants.filter((c) => c.projectId === project.id);

  const [itemForm, setItemForm] = useState({
    type: 'RFI' as 'RFI' | 'SUBMITTAL' | 'SITE_INSTRUCTION',
    subject: '',
    discipline: 'Architectural',
    location: '',
    raisedBy: '',
    assignedTo: '',
    questionOrRequirement: '',
    priority: 'NORMAL' as const,
  });

  const [consultantForm, setConsultantForm] = useState({
    name: '',
    organisation: '',
    discipline: 'Structural Engineering',
    email: '',
    phone: '',
    responsibility: '',
    raciRole: 'RESPONSIBLE' as const,
  });

  const filteredItems = coordinationItems.filter(
    (item) => itemTypeFilter === 'ALL' || item.type === itemTypeFilter
  );

  const handleCreateItem = (e: React.FormEvent) => {
    e.preventDefault();
    if (!itemForm.subject.trim()) return;

    const prefix = itemForm.type === 'RFI' ? 'RFI' : itemForm.type === 'SUBMITTAL' ? 'SUB' : 'SI';
    const count = coordinationItems.filter((i) => i.type === itemForm.type).length + 1;
    const ref = `${prefix}-${project.projectCode}-${count.toString().padStart(3, '0')}`;
    const initialStatus = CoordinationWorkflow.initialStatus(itemForm.type);

    store.addCoordinationItem({
      projectId: project.id,
      type: itemForm.type,
      referenceNumber: ref,
      subject: itemForm.subject,
      discipline: itemForm.discipline,
      location: itemForm.location,
      raisedBy: itemForm.raisedBy,
      assignedTo: itemForm.assignedTo,
      questionOrRequirement: itemForm.questionOrRequirement,
      response: '',
      status: initialStatus,
      priority: itemForm.priority,
      dueAt: Date.now() + 5 * 86400000,
      linkedDrawingRevisionId: null,
      closedAt: null,
      archivedAt: null,
    });

    setShowItemModal(false);
    setItemForm({
      type: 'RFI',
      subject: '',
      discipline: 'Architectural',
      location: '',
      raisedBy: '',
      assignedTo: '',
      questionOrRequirement: '',
      priority: 'NORMAL',
    });
  };

  const handleTransitionStatus = (item: CoordinationItemEntity, nextStatus: string) => {
    if (nextStatus === 'ANSWERED' || nextStatus === 'UNDER_REVIEW' || nextStatus === 'ACKNOWLEDGED') {
      setShowResponseModal(item);
      return;
    }
    const isClosed = CoordinationWorkflow.isClosed(nextStatus);
    store.updateCoordinationItem(item.id, {
      status: nextStatus,
      closedAt: isClosed ? Date.now() : item.closedAt,
    });
  };

  const handleSaveResponse = (item: CoordinationItemEntity) => {
    const isRFI = item.type === 'RFI';
    const nextStatus = isRFI ? 'ANSWERED' : item.type === 'SUBMITTAL' ? 'APPROVED' : 'ACKNOWLEDGED';
    store.updateCoordinationItem(item.id, {
      response: responseText,
      status: nextStatus,
    });
    setShowResponseModal(null);
    setResponseText('');
  };

  const handleCreateConsultant = (e: React.FormEvent) => {
    e.preventDefault();
    if (!consultantForm.name.trim()) return;

    store.projectConsultants.push({
      id: Date.now(),
      projectId: project.id,
      name: consultantForm.name,
      organisation: consultantForm.organisation,
      discipline: consultantForm.discipline,
      email: consultantForm.email,
      phone: consultantForm.phone,
      phase: 'All phases',
      responsibility: consultantForm.responsibility,
      raciRole: consultantForm.raciRole,
      status: 'ACTIVE',
      createdAt: Date.now(),
    });

    setShowConsultantModal(false);
    setConsultantForm({
      name: '',
      organisation: '',
      discipline: 'Structural Engineering',
      email: '',
      phone: '',
      responsibility: '',
      raciRole: 'RESPONSIBLE',
    });
  };

  const handleExportCoordination = () => {
    let csv = `COORDINATION LOG - ${project.name} (${project.projectCode})\n`;
    csv += `Reference,Type,Subject,Discipline,Status,Priority,Raised By,Assigned To,Response\n`;
    coordinationItems.forEach((c) => {
      csv += `"${c.referenceNumber}","${c.type}","${c.subject}","${c.discipline}","${c.status}","${c.priority}","${c.raisedBy}","${c.assignedTo}","${c.response}"\n`;
    });
    ExportHelper.downloadCSV(`Coordination_${project.projectCode}.csv`, csv);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <MessageSquare className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-bold text-slate-900">Coordination & Consultants Matrix</h2>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Manage Requests for Information (RFIs), Submittals, Site Instructions, and RACI Responsibility Matrix.
          </p>
        </div>

        <div className="flex items-center gap-2">
          {activeTab === 'ITEMS' && (
            <>
              <button
                type="button"
                onClick={handleExportCoordination}
                className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
              >
                <Download className="w-4 h-4" /> Export CSV
              </button>
              <button
                type="button"
                onClick={() => setShowItemModal(true)}
                className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
              >
                <Plus className="w-4 h-4" /> New RFI / Instruction
              </button>
            </>
          )}
          {activeTab === 'CONSULTANTS' && (
            <button
              type="button"
              onClick={() => setShowConsultantModal(true)}
              className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
            >
              <Plus className="w-4 h-4" /> Add Consultant
            </button>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 gap-6">
        <button
          type="button"
          onClick={() => setActiveTab('ITEMS')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'ITEMS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <FileCheck className="w-4 h-4" />
          <span>RFIs, Submittals & Site Instructions ({coordinationItems.length})</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('CONSULTANTS')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'CONSULTANTS'
              ? 'border-indigo-600 text-indigo-600'
              : 'border-transparent text-slate-500 hover:text-slate-800'
          }`}
        >
          <Users className="w-4 h-4" />
          <span>Consultants RACI Matrix ({consultants.length})</span>
        </button>
      </div>

      {/* View: Coordination Items */}
      {activeTab === 'ITEMS' && (
        <div className="space-y-4">
          {/* Subfilter */}
          <div className="flex items-center gap-2">
            {(['ALL', 'RFI', 'SUBMITTAL', 'SITE_INSTRUCTION'] as const).map((filter) => (
              <button
                key={filter}
                type="button"
                onClick={() => setItemTypeFilter(filter)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                  itemTypeFilter === filter
                    ? 'bg-indigo-600 text-white'
                    : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
                }`}
              >
                {filter === 'ALL' ? 'All Records' : filter === 'SITE_INSTRUCTION' ? 'Site Instructions' : filter}
              </button>
            ))}
          </div>

          <div className="space-y-3">
            {filteredItems.length === 0 ? (
              <div className="bg-white p-12 text-center rounded-xl border border-dashed border-slate-300">
                <MessageSquare className="w-10 h-10 text-slate-400 mx-auto mb-2" />
                <h3 className="text-sm font-semibold text-slate-700">No Coordination Records</h3>
                <p className="text-xs text-slate-500 mt-1">
                  Log RFIs, material submittals or architect site instructions to streamline multidisciplinary coordination.
                </p>
              </div>
            ) : (
              filteredItems.map((item) => {
                const allowed = CoordinationWorkflow.allowedNextStatuses(item.type, item.status);
                const isClosed = CoordinationWorkflow.isClosed(item.status);

                return (
                  <div
                    key={item.id}
                    className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3 hover:border-slate-300 transition"
                  >
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-mono text-xs font-semibold text-slate-700">
                          {item.referenceNumber}
                        </span>
                        <span className="text-slate-300">·</span>
                        <span className="text-xs font-semibold text-slate-800">
                          {item.type.replace(/_/g, ' ')}
                        </span>
                        <span className="text-slate-300">·</span>
                        <span className="text-xs text-slate-500">Discipline: {item.discipline}</span>
                      </div>

                      <div className="flex items-center gap-1.5 text-xs font-mono font-medium text-slate-700">
                        <span
                          className={`w-1.5 h-1.5 rounded-full ${
                            isClosed
                              ? 'bg-emerald-500'
                              : item.status === 'ANSWERED'
                              ? 'bg-blue-500'
                              : 'bg-amber-500'
                          }`}
                        ></span>
                        <span>{item.status}</span>
                      </div>
                    </div>

                    <div>
                      <h3 className="text-base font-bold text-slate-900">{item.subject}</h3>
                      <div className="flex flex-wrap gap-x-4 text-xs text-slate-500 mt-1">
                        <span>Raised By: <strong className="text-slate-700">{item.raisedBy || '-'}</strong></span>
                        <span>Assigned To: <strong className="text-slate-700">{item.assignedTo || '-'}</strong></span>
                        {item.location && <span>Location: <strong className="text-slate-700">{item.location}</strong></span>}
                      </div>

                      <div className="mt-2 text-xs text-slate-700 bg-slate-50 p-3 rounded-lg border border-slate-100 leading-relaxed">
                        <span className="font-semibold text-slate-900 block mb-0.5">
                          {item.type === 'RFI' ? 'Clarification Required:' : item.type === 'SITE_INSTRUCTION' ? 'Site Directive:' : 'Submittal Requirement:'}
                        </span>
                        {item.questionOrRequirement}
                      </div>

                      {item.response && (
                        <div className="mt-2 text-xs text-emerald-900 bg-emerald-50/60 p-3 rounded-lg border border-emerald-100 leading-relaxed">
                          <span className="font-semibold text-emerald-950 block mb-0.5">Formal Technical Response:</span>
                          {item.response}
                        </div>
                      )}
                    </div>

                    {/* Allowed transitions */}
                    {allowed.length > 0 && (
                      <div className="pt-2 border-t border-slate-100 flex items-center justify-end gap-2">
                        {allowed.map((next) => (
                          <button
                            key={next}
                            type="button"
                            onClick={() => handleTransitionStatus(item, next)}
                            className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold"
                          >
                            Move to {next.replace(/_/g, ' ')}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* View: Consultants RACI Matrix */}
      {activeTab === 'CONSULTANTS' && (
        <div className="space-y-4">
          <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200 uppercase tracking-wider text-[11px]">
                  <tr>
                    <th className="px-4 py-3">Consultant / Practice</th>
                    <th className="px-4 py-3">Discipline</th>
                    <th className="px-4 py-3">RACI Responsibility</th>
                    <th className="px-4 py-3">Scope of Responsibility</th>
                    <th className="px-4 py-3">Contact</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {consultants.map((c) => (
                    <tr key={c.id} className="hover:bg-slate-50/70">
                      <td className="px-4 py-3">
                        <div className="font-bold text-slate-900">{c.name}</div>
                        <div className="text-slate-500 text-[11px]">{c.organisation}</div>
                      </td>
                      <td className="px-4 py-3 font-medium text-slate-800">{c.discipline}</td>
                      <td className="px-4 py-3">
                        <span
                          className={`px-2 py-0.5 rounded font-bold text-[10px] ${
                            c.raciRole === 'RESPONSIBLE'
                              ? 'bg-blue-100 text-blue-800'
                              : c.raciRole === 'ACCOUNTABLE'
                              ? 'bg-purple-100 text-purple-800'
                              : c.raciRole === 'CONSULTED'
                              ? 'bg-amber-100 text-amber-800'
                              : 'bg-slate-100 text-slate-700'
                          }`}
                        >
                          {c.raciRole}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-600 max-w-xs">{c.responsibility}</td>
                      <td className="px-4 py-3 text-slate-500">
                        <div>{c.email}</div>
                        <div>{c.phone}</div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Add Coordination Item */}
      {showItemModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">New Coordination Item</h3>
              <button
                type="button"
                onClick={() => setShowItemModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateItem} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Item Type</label>
                  <select
                    value={itemForm.type}
                    onChange={(e) => setItemForm({ ...itemForm, type: e.target.value as any })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                  >
                    <option value="RFI">RFI (Request for Information)</option>
                    <option value="SUBMITTAL">Material / Sample Submittal</option>
                    <option value="SITE_INSTRUCTION">Architect Site Instruction (SI)</option>
                  </select>
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Priority</label>
                  <select
                    value={itemForm.priority}
                    onChange={(e) => setItemForm({ ...itemForm, priority: e.target.value as any })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                  >
                    <option value="NORMAL">Normal</option>
                    <option value="HIGH">High Priority</option>
                    <option value="CRITICAL">Critical Schedule Impact</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Subject / Query Title</label>
                <input
                  type="text"
                  required
                  value={itemForm.subject}
                  onChange={(e) => setItemForm({ ...itemForm, subject: e.target.value })}
                  placeholder="e.g. Sanitary pipe run coordination with structural beam B-12"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="grid grid-cols-3 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Discipline</label>
                  <input
                    type="text"
                    value={itemForm.discipline}
                    onChange={(e) => setItemForm({ ...itemForm, discipline: e.target.value })}
                    placeholder="e.g. Structural / MEP"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Raised By</label>
                  <input
                    type="text"
                    value={itemForm.raisedBy}
                    onChange={(e) => setItemForm({ ...itemForm, raisedBy: e.target.value })}
                    placeholder="e.g. Site Engineer"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Assigned To</label>
                  <input
                    type="text"
                    value={itemForm.assignedTo}
                    onChange={(e) => setItemForm({ ...itemForm, assignedTo: e.target.value })}
                    placeholder="e.g. Structural Consultant"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Question / Requirement Details</label>
                <textarea
                  rows={3}
                  required
                  value={itemForm.questionOrRequirement}
                  onChange={(e) => setItemForm({ ...itemForm, questionOrRequirement: e.target.value })}
                  placeholder="Detail the technical query or directive..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowItemModal(false)}
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

      {/* Modal: Enter Technical Response */}
      {showResponseModal !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">
                Provide Technical Response ({showResponseModal.referenceNumber})
              </h3>
              <button
                type="button"
                onClick={() => setShowResponseModal(null)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <div className="text-xs text-slate-600 bg-slate-50 p-2.5 rounded">
              <strong>Query:</strong> {showResponseModal.questionOrRequirement}
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Formal Technical Clarification</label>
              <textarea
                rows={4}
                required
                value={responseText}
                onChange={(e) => setResponseText(e.target.value)}
                placeholder="Enter architectural/engineering directive, reference drawings, or approvals..."
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowResponseModal(null)}
                className="px-4 py-2 border border-slate-200 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!responseText.trim()}
                onClick={() => handleSaveResponse(showResponseModal)}
                className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold disabled:opacity-50"
              >
                Submit Response
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
