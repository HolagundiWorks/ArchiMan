import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, ProjectDrawingEntity, DrawingRevisionEntity } from '../types';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  FileCode2,
  Plus,
  Send,
  History,
  Tag,
  CheckCircle,
  AlertCircle,
  FileText,
  Download,
} from 'lucide-react';

interface DrawingsViewProps {
  project: ProjectEntity;
}

export const DrawingsView: React.FC<DrawingsViewProps> = ({ project }) => {
  const [activeTab, setActiveTab] = useState<'REGISTER' | 'TRANSMITTALS'>('REGISTER');
  const [showDrawingModal, setShowDrawingModal] = useState(false);
  const [showRevisionModal, setShowRevisionModal] = useState<number | null>(null);

  const drawings = store.drawings.filter((d) => d.projectId === project.id);
  const revisions = store.drawingRevisions.filter((r) => r.projectId === project.id);

  const [drgForm, setDrgForm] = useState({
    drawingNumber: '',
    title: '',
    discipline: 'Architectural',
    packageTag: '',
  });

  const [revForm, setRevForm] = useState({
    revisionCode: 'REV-A',
    revisionNotes: '',
    revisionSource: 'CLIENT_REQUEST',
    severity: 'NORMAL' as const,
    isAsBuilt: false,
  });

  const handleCreateDrawing = (e: React.FormEvent) => {
    e.preventDefault();
    if (!drgForm.drawingNumber.trim() || !drgForm.title.trim()) return;

    store.addDrawing({
      projectId: project.id,
      drawingNumber: drgForm.drawingNumber,
      title: drgForm.title,
      discipline: drgForm.discipline,
      status: 'WORKING',
      packageTag: drgForm.packageTag,
      archivedAt: null,
    });
    setShowDrawingModal(false);
    setDrgForm({ drawingNumber: '', title: '', discipline: 'Architectural', packageTag: '' });
  };

  const handleAddRevision = (drawingId: number) => {
    const drg = drawings.find((d) => d.id === drawingId);
    if (!drg) return;

    store.addDrawingRevision({
      projectId: project.id,
      drawingId,
      revisionCode: revForm.revisionCode,
      fileName: `${drg.drawingNumber}_${revForm.revisionCode}.pdf`,
      mimeType: 'application/pdf',
      fileUri: `drawings/${drg.drawingNumber}_${revForm.revisionCode}.pdf`,
      fileChecksum: `sha256:${Date.now().toString(16)}`,
      issueStatus: 'ISSUED',
      revisionNotes: revForm.revisionNotes,
      isAsBuilt: revForm.isAsBuilt,
      revisionSource: revForm.revisionSource,
      severity: revForm.severity,
      issuedAt: Date.now(),
    });
    setShowRevisionModal(null);
    setRevForm({
      revisionCode: 'REV-B',
      revisionNotes: '',
      revisionSource: 'CLIENT_REQUEST',
      severity: 'NORMAL',
      isAsBuilt: false,
    });
  };

  const handleToggleHFC = (drawing: ProjectDrawingEntity) => {
    const newTag = drawing.packageTag === 'HFC' ? '' : 'HFC';
    store.updateDrawing(drawing.id, { packageTag: newTag });
  };

  const handleExportTransmittal = () => {
    let csv = `DRAWING TRANSMITTAL REGISTER - ${project.name} (${project.projectCode})\n`;
    csv += `Drawing Number,Title,Discipline,Package Tag,Latest Rev,Status,Revision Source\n`;
    drawings.forEach((d) => {
      const revs = revisions.filter((r) => r.drawingId === d.id);
      const latest = revs[revs.length - 1];
      csv += `"${d.drawingNumber}","${d.title}","${d.discipline}","${d.packageTag}","${latest?.revisionCode || 'REV-0'}","${latest?.issueStatus || 'WIP'}","${latest?.revisionSource || '-'}"\n`;
    });
    ExportHelper.downloadCSV(`Transmittal_Register_${project.projectCode}.csv`, csv);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <FileCode2 className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-bold text-slate-900">Drawing Register & Transmittals</h2>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Maintain revision genealogy, HFC package tags, transmittal logs, and as-built flags.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleExportTransmittal}
            className="px-3 py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-semibold flex items-center gap-1.5"
          >
            <Download className="w-4 h-4" /> Export Transmittal CSV
          </button>
          <button
            type="button"
            onClick={() => setShowDrawingModal(true)}
            className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
          >
            <Plus className="w-4 h-4" /> Register Drawing
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {drawings.map((drg) => {
          const revs = revisions.filter((r) => r.drawingId === drg.id);
          const latest = revs[revs.length - 1];
          const isHFC = drg.packageTag === 'HFC';

          return (
            <div key={drg.id} className="bg-white rounded-xl border border-slate-200 p-5 shadow-sm space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                  {drg.drawingNumber}
                </span>

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => handleToggleHFC(drg)}
                    className={`text-[10px] font-semibold px-2 py-0.5 rounded-full border transition ${
                      isHFC
                        ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                        : 'bg-slate-50 text-slate-500 border-slate-200 hover:bg-slate-100'
                    }`}
                  >
                    {isHFC ? 'HFC Tagged' : '+ Tag HFC'}
                  </button>
                </div>
              </div>

              <div>
                <h3 className="text-base font-bold text-slate-900">{drg.title}</h3>
                <div className="text-xs text-slate-500 mt-0.5">Discipline: {drg.discipline}</div>
              </div>

              {/* Revision history pill */}
              <div className="bg-slate-50 p-2.5 rounded-lg border border-slate-100 text-xs space-y-1">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-slate-700 flex items-center gap-1">
                    <History className="w-3.5 h-3.5 text-indigo-600" />
                    Latest: {latest?.revisionCode || 'REV-0'}
                  </span>
                  <span className="text-[10px] px-1.5 py-0.5 bg-indigo-100 text-indigo-800 rounded font-medium">
                    {latest?.issueStatus || 'WORKING'}
                  </span>
                </div>
                {latest && (
                  <p className="text-[11px] text-slate-600 line-clamp-2 italic">
                    "{latest.revisionNotes || 'Initial baseline'}"
                  </p>
                )}
                {latest?.isAsBuilt && (
                  <span className="text-[10px] text-emerald-700 font-bold block">✓ As-Built Certified</span>
                )}
              </div>

              <div className="pt-2 border-t border-slate-100 flex items-center justify-between">
                <span className="text-[11px] text-slate-400">{revs.length} revisions</span>
                <button
                  type="button"
                  onClick={() => setShowRevisionModal(drg.id)}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" /> Issue Revision
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Modal: Register Drawing */}
      {showDrawingModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">Register New Drawing</h3>
              <button
                type="button"
                onClick={() => setShowDrawingModal(false)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateDrawing} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-slate-700 mb-1">Drawing Number</label>
                <input
                  type="text"
                  required
                  value={drgForm.drawingNumber}
                  onChange={(e) => setDrgForm({ ...drgForm, drawingNumber: e.target.value })}
                  placeholder="e.g. A-102 or S-301"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono"
                />
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Title</label>
                <input
                  type="text"
                  required
                  value={drgForm.title}
                  onChange={(e) => setDrgForm({ ...drgForm, title: e.target.value })}
                  placeholder="e.g. First Floor Electrical Layout"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-slate-700 mb-1">Discipline</label>
                <select
                  value={drgForm.discipline}
                  onChange={(e) => setDrgForm({ ...drgForm, discipline: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                >
                  <option value="Architectural">Architectural</option>
                  <option value="Structural">Structural</option>
                  <option value="Plumbing">Plumbing</option>
                  <option value="Electrical">Electrical</option>
                  <option value="HVAC">HVAC</option>
                  <option value="Landscape">Landscape</option>
                </select>
              </div>
              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="hfcTag"
                  checked={drgForm.packageTag === 'HFC'}
                  onChange={(e) => setDrgForm({ ...drgForm, packageTag: e.target.checked ? 'HFC' : '' })}
                  className="rounded text-indigo-600"
                />
                <label htmlFor="hfcTag" className="text-slate-700 font-medium">
                  Tag as Held-for-Construction (HFC)
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowDrawingModal(false)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Save to Register
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Issue Revision */}
      {showRevisionModal !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <h3 className="text-base font-bold text-slate-800">Issue Drawing Revision</h3>
              <button
                type="button"
                onClick={() => setShowRevisionModal(null)}
                className="text-slate-400 hover:text-slate-600 text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleAddRevision(showRevisionModal);
              }}
              className="space-y-3 text-xs"
            >
              <div>
                <label className="block font-medium text-slate-700 mb-1">Revision Code</label>
                <input
                  type="text"
                  required
                  value={revForm.revisionCode}
                  onChange={(e) => setRevForm({ ...revForm, revisionCode: e.target.value })}
                  placeholder="e.g. REV-B or REV-C"
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg font-mono font-bold"
                />
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Revision Source / Trigger</label>
                <select
                  value={revForm.revisionSource}
                  onChange={(e) => setRevForm({ ...revForm, revisionSource: e.target.value })}
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg bg-white"
                >
                  <option value="CLIENT_REQUEST">Client Request / Program Change</option>
                  <option value="SITE_CONDITION">Site Condition / Field Clarification</option>
                  <option value="ARCHITECT_REVIEW">Architect Quality Review</option>
                  <option value="COORDINATION_CLASH">MEP / Structural Clash Resolution</option>
                  <option value="STATUTORY_COMPLIANCE">Statutory / Municipal Compliance</option>
                </select>
              </div>

              <div>
                <label className="block font-medium text-slate-700 mb-1">Revision Notes & Changes</label>
                <textarea
                  rows={3}
                  required
                  value={revForm.revisionNotes}
                  onChange={(e) => setRevForm({ ...revForm, revisionNotes: e.target.value })}
                  placeholder="Describe exact architectural or structural changes introduced in this revision..."
                  className="w-full px-3 py-2 border border-slate-300 rounded-lg"
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="asBuiltCheck"
                  checked={revForm.isAsBuilt}
                  onChange={(e) => setRevForm({ ...revForm, isAsBuilt: e.target.checked })}
                  className="rounded text-indigo-600"
                />
                <label htmlFor="asBuiltCheck" className="text-slate-700 font-medium">
                  Mark as As-Built / Handover Revision
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setShowRevisionModal(null)}
                  className="px-4 py-2 border border-slate-200 rounded-lg text-slate-600 font-medium hover:bg-slate-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold"
                >
                  Issue Revision
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
