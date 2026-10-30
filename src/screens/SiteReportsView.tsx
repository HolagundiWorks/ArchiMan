import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, SiteIssueEntity } from '../types';
import { SiteIssueWorkflow } from '../domain/SiteIssueWorkflow';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  Plus,
  FileText,
  Download,
  Calendar,
  ShieldAlert,
  ChevronRight,
  Send,
  Camera,
} from 'lucide-react';

interface SiteReportsViewProps {
  project: ProjectEntity;
}

export const SiteReportsView: React.FC<SiteReportsViewProps> = ({ project }) => {
  const [activeTab, setActiveTab] = useState<'SNAGS' | 'DAILY' | 'MEETINGS'>('SNAGS');
  const [showIssueModal, setShowIssueModal] = useState(false);
  const [showClosureModal, setShowClosureModal] = useState<SiteIssueEntity | null>(null);
  const [verificationNote, setVerificationNote] = useState('');
  const [afterEvidence, setAfterEvidence] = useState('');

  const issues = store.siteIssues.filter((i) => i.projectId === project.id);
  const dailyLogs = store.dailyReports.filter((r) => r.projectId === project.id);

  const [issueForm, setIssueForm] = useState({
    type: 'SNAG' as 'SNAG' | 'NCR' | 'SAFETY',
    title: '',
    location: '',
    description: '',
    severity: 'NORMAL' as const,
    assignedTo: '',
    correctiveAction: '',
    evidenceUri: '',
  });

  const handleCreateIssue = (e: React.FormEvent) => {
    e.preventDefault();
    if (!issueForm.title.trim() || !issueForm.description.trim()) return;

    const count = issues.length + 1;
    const ref = `${issueForm.type === 'NCR' ? 'NCR' : issueForm.type === 'SAFETY' ? 'SAF' : 'SNG'}-${project.projectCode}-${count.toString().padStart(3, '0')}`;

    store.addSiteIssue({
      projectId: project.id,
      referenceNumber: ref,
      type: issueForm.type,
      title: issueForm.title,
      location: issueForm.location,
      description: issueForm.description,
      severity: issueForm.severity,
      assignedTo: issueForm.assignedTo,
      correctiveAction: issueForm.correctiveAction,
      dueAt: Date.now() + 3 * 86400000,
      status: 'OPEN',
      evidenceUri: issueForm.evidenceUri || 'Discovered during architect walk-through',
      afterEvidenceUri: null,
      verificationNote: '',
      closedAt: null,
    });

    setShowIssueModal(false);
    setIssueForm({
      type: 'SNAG',
      title: '',
      location: '',
      description: '',
      severity: 'NORMAL',
      assignedTo: '',
      correctiveAction: '',
      evidenceUri: '',
    });
  };

  const handleStatusTransition = (issue: SiteIssueEntity, nextStatus: string) => {
    if (nextStatus === 'CLOSED') {
      setShowClosureModal(issue);
      return;
    }
    store.updateSiteIssue(issue.id, { status: nextStatus as any });
  };

  const handleConfirmClosure = (issue: SiteIssueEntity) => {
    if (!verificationNote.trim()) return;
    store.updateSiteIssue(issue.id, {
      status: 'CLOSED',
      verificationNote,
      afterEvidenceUri: afterEvidence || 'Rectification verified on site by Architect',
      closedAt: Date.now(),
    });
    setShowClosureModal(null);
    setVerificationNote('');
    setAfterEvidence('');
  };

  const handleExportIssues = () => {
    let csv = `SITE ISSUES & SNAG / NCR REGISTER - ${project.name} (${project.projectCode})\n`;
    csv += `Ref Number,Type,Title,Severity,Status,Assigned To,Location,Corrective Action,Verification Note,Closed At\n`;
    issues.forEach((i) => {
      csv += `"${i.referenceNumber}","${i.type}","${i.title}","${i.severity}","${i.status}","${i.assignedTo}","${i.location}","${i.correctiveAction}","${i.verificationNote}","${i.closedAt ? new Date(i.closedAt).toLocaleDateString() : '-'}"\n`;
    });
    ExportHelper.downloadCSV(`Snag_NCR_Register_${project.projectCode}.csv`, csv);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <ShieldAlert className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-bold text-slate-900">Site Quality & Reports</h2>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Snag / NCR closure tracking with before & after evidence and formal architect verification notes.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleExportIssues}
            className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
          >
            <Download className="w-4 h-4" /> Export CSV
          </button>
          <button
            type="button"
            onClick={() => setShowIssueModal(true)}
            className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
          >
            <Plus className="w-4 h-4" /> Log Snag / NCR
          </button>
        </div>
      </div>

      {/* Issues List */}
      <div className="space-y-3">
        {issues.length === 0 ? (
          <div className="bg-white p-12 text-center rounded-xl border border-dashed border-slate-300">
            <CheckCircle2 className="w-10 h-10 text-emerald-500 mx-auto mb-2" />
            <h3 className="text-sm font-semibold text-slate-700">No Open Snags or NCRs</h3>
            <p className="text-xs text-slate-500 mt-1">
              Quality standards are meeting specifications. Log an observation whenever defects are identified on site.
            </p>
          </div>
        ) : (
          issues.map((issue) => {
            const nextStatuses = SiteIssueWorkflow.allowedNextStatuses(issue.status);

            return (
              <div
                key={issue.id}
                className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-4 hover:border-slate-300 transition"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-100 pb-3">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                      {issue.referenceNumber}
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wider ${
                        issue.type === 'NCR'
                          ? 'bg-rose-100 text-rose-800'
                          : issue.type === 'SAFETY'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-indigo-100 text-indigo-800'
                      }`}
                    >
                      {issue.type}
                    </span>
                    <span
                      className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                        issue.severity === 'CRITICAL'
                          ? 'bg-rose-100 text-rose-800'
                          : issue.severity === 'ATTENTION'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-slate-100 text-slate-600'
                      }`}
                    >
                      {issue.severity}
                    </span>
                  </div>

                  <div className="flex items-center gap-2">
                    <span
                      className={`text-xs font-bold px-2.5 py-1 rounded-full ${
                        issue.status === 'CLOSED'
                          ? 'bg-emerald-100 text-emerald-800'
                          : issue.status === 'READY_FOR_VERIFICATION'
                          ? 'bg-blue-100 text-blue-800'
                          : issue.status === 'IN_PROGRESS'
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-slate-100 text-slate-700'
                      }`}
                    >
                      {issue.status}
                    </span>
                  </div>
                </div>

                <div>
                  <h3 className="text-base font-bold text-slate-900">{issue.title}</h3>
                  <div className="text-xs text-slate-500 mt-1 flex flex-wrap gap-x-4">
                    <span>Location: <strong className="text-slate-700">{issue.location || 'General'}</strong></span>
                    <span>Assigned To: <strong className="text-slate-700">{issue.assignedTo || 'Civil Contractor'}</strong></span>
                  </div>
                  <p className="text-xs text-slate-700 mt-2 bg-slate-50 p-3 rounded-lg border border-slate-100 leading-relaxed">
                    {issue.description}
                  </p>
                </div>

                {/* Evidence & Closure Details */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                  <div className="p-3 bg-amber-50/50 rounded-lg border border-amber-100">
                    <div className="font-semibold text-amber-800 flex items-center gap-1.5">
                      <Camera className="w-3.5 h-3.5" /> Discovery (Before) Evidence:
                    </div>
                    <div className="text-slate-700 mt-1">{issue.evidenceUri || 'Recorded during site walk'}</div>
                  </div>

                  {issue.status === 'CLOSED' ? (
                    <div className="p-3 bg-emerald-50/60 rounded-lg border border-emerald-100">
                      <div className="font-semibold text-emerald-800 flex items-center gap-1.5">
                        <CheckCircle2 className="w-3.5 h-3.5" /> Rectification Verified (After):
                      </div>
                      <div className="text-slate-700 mt-1">{issue.afterEvidenceUri || 'Verified by Architect'}</div>
                      <div className="text-[11px] text-emerald-900 font-medium mt-1">
                        Note: {issue.verificationNote}
                      </div>
                    </div>
                  ) : (
                    <div className="p-3 bg-slate-50 rounded-lg border border-slate-200">
                      <div className="font-semibold text-slate-700">Corrective Action Required:</div>
                      <div className="text-slate-600 mt-1">{issue.correctiveAction || 'Pending contractor proposal'}</div>
                    </div>
                  )}
                </div>

                {/* Workflow Transitions */}
                {nextStatuses.length > 0 && (
                  <div className="pt-3 border-t border-slate-100 flex items-center justify-end gap-2">
                    {nextStatuses.map((next) => (
                      <button
                        key={next}
                        type="button"
                        onClick={() => handleStatusTransition(issue, next)}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition ${
                          next === 'CLOSED'
                            ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm'
                            : 'bg-indigo-600 hover:bg-indigo-700 text-white'
                        }`}
                      >
                        {next === 'CLOSED' && <CheckCircle2 className="w-3.5 h-3.5" />}
                        {next === 'READY_FOR_VERIFICATION' && <CheckCircle2 className="w-3.5 h-3.5" />}
                        {next === 'IN_PROGRESS' && <Clock className="w-3.5 h-3.5" />}
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

      {/* Modal: Log Snag/NCR */}
      {showIssueModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">Log Site Snag / NCR</h3>
              <button
                type="button"
                onClick={() => setShowIssueModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateIssue} className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Issue Type</label>
                  <select
                    value={issueForm.type}
                    onChange={(e) => setIssueForm({ ...issueForm, type: e.target.value as any })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                  >
                    <option value="SNAG">Snag (Finish / Workmanship)</option>
                    <option value="NCR">NCR (Non-Conformance Report)</option>
                    <option value="SAFETY">Safety Hazard Observation</option>
                  </select>
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Severity Scale</label>
                  <select
                    value={issueForm.severity}
                    onChange={(e) => setIssueForm({ ...issueForm, severity: e.target.value as any })}
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                  >
                    <option value="NORMAL">Normal</option>
                    <option value="ATTENTION">Attention Required</option>
                    <option value="CRITICAL">Critical Structural / Safety</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Issue Title</label>
                <input
                  type="text"
                  required
                  value={issueForm.title}
                  onChange={(e) => setIssueForm({ ...issueForm, title: e.target.value })}
                  placeholder="e.g. Inadequate rebar cover in ground floor slab beam"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Location Details</label>
                  <input
                    type="text"
                    value={issueForm.location}
                    onChange={(e) => setIssueForm({ ...issueForm, location: e.target.value })}
                    placeholder="e.g. GF Kitchen Shaft Grid B"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
                <div>
                  <label className="block font-medium text-slate-700 mb-1">Assigned Contractor</label>
                  <input
                    type="text"
                    value={issueForm.assignedTo}
                    onChange={(e) => setIssueForm({ ...issueForm, assignedTo: e.target.value })}
                    placeholder="e.g. Apex Civil Infrastructure"
                    className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Detailed Observation & Defect</label>
                <textarea
                  rows={3}
                  required
                  value={issueForm.description}
                  onChange={(e) => setIssueForm({ ...issueForm, description: e.target.value })}
                  placeholder="Detail exact non-conformance with drawing or specification standards..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Corrective Action Required</label>
                <input
                  type="text"
                  value={issueForm.correctiveAction}
                  onChange={(e) => setIssueForm({ ...issueForm, correctiveAction: e.target.value })}
                  placeholder="e.g. Tie 25mm PVC cover blocks before casting"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Discovery Evidence Note</label>
                <input
                  type="text"
                  value={issueForm.evidenceUri}
                  onChange={(e) => setIssueForm({ ...issueForm, evidenceUri: e.target.value })}
                  placeholder="e.g. Photo taken during pre-pour inspection"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowIssueModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Record Snag / NCR
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Confirm Closure (Requires Verification Note) */}
      {showClosureModal !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center gap-2 text-emerald-600 pb-2 border-b border-slate-100">
              <CheckCircle2 className="w-5 h-5" />
              <h3 className="text-base font-bold text-slate-800">Verify & Close Issue</h3>
            </div>
            <p className="text-xs text-slate-600">
              Per practice QA protocol (SiteIssueWorkflow), a verification note is mandatory before closing any snag or NCR.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Verification Note (Architect Signoff)
              </label>
              <textarea
                rows={3}
                required
                value={verificationNote}
                onChange={(e) => setVerificationNote(e.target.value)}
                placeholder="e.g. Inspected on site on 30 Oct; micro-concrete grouting completed cleanly without cracks."
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                After Rectification Evidence Note
              </label>
              <input
                type="text"
                value={afterEvidence}
                onChange={(e) => setAfterEvidence(e.target.value)}
                placeholder="e.g. Post-repair inspection photo recorded"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  setShowClosureModal(null);
                  setVerificationNote('');
                  setAfterEvidence('');
                }}
                className="px-4 py-2 border border-slate-200 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!verificationNote.trim()}
                onClick={() => handleConfirmClosure(showClosureModal)}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-semibold disabled:opacity-50"
              >
                Confirm Verification & Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
