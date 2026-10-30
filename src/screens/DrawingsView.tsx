import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, ProjectDrawingEntity, DrawingRevisionEntity } from '../types';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  FileCode2,
  Plus,
  History,
  Tag,
  CheckCircle,
  AlertCircle,
  Download,
  FileBox,
} from 'lucide-react';
import { Button } from '../components/ui';

interface DrawingsViewProps {
  project: ProjectEntity;
  onOpenODraft?: () => void;
}

export const DrawingsView: React.FC<DrawingsViewProps> = ({ project, onOpenODraft }) => {
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
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-surface-container-lowest p-5 rounded-lg border border-outline-variant elevation-1">
        <div>
          <div className="flex items-center gap-2">
            <FileCode2 className="w-5 h-5 text-primary" />
            <h2 className="text-xl font-bold text-on-surface">Drawing Register & Transmittals</h2>
          </div>
          <p className="text-xs text-on-surface-variant mt-1">
            Maintain revision genealogy, HFC package tags, transmittal logs, and as-built flags.
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {onOpenODraft && (
            <Button
              size="sm"
              variant="outlined"
              leadingIcon={<FileBox className="w-3.5 h-3.5" />}
              onClick={onOpenODraft}
            >
              Open .osp / .dxf
            </Button>
          )}
          <Button
            size="sm"
            variant="tonal"
            leadingIcon={<Download className="w-3.5 h-3.5" />}
            onClick={handleExportTransmittal}
          >
            Export CSV
          </Button>
          <Button
            size="sm"
            leadingIcon={<Plus className="w-3.5 h-3.5" />}
            onClick={() => setShowDrawingModal(true)}
          >
            Register Drawing
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {drawings.map((drg) => {
          const revs = revisions.filter((r) => r.drawingId === drg.id);
          const latest = revs[revs.length - 1];
          const isHFC = drg.packageTag === 'HFC';

          return (
            <div key={drg.id} className="bg-surface-container-lowest rounded-md border border-outline-variant p-5 elevation-1 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-mono text-xs font-semibold text-on-surface">
                  {drg.drawingNumber}
                </span>

                <div className="flex items-center gap-1.5">
                  <button
                    type="button"
                    onClick={() => handleToggleHFC(drg)}
                    className={`text-xs font-medium px-2.5 py-1 rounded-md transition ${
                      isHFC
                        ? 'bg-success-container text-emerald-800 border border-emerald-200'
                        : 'text-on-surface-variant hover:text-on-surface hover:bg-surface'
                    }`}
                  >
                    {isHFC ? '✓ HFC Tagged' : '+ Tag HFC'}
                  </button>
                </div>
              </div>

              <div>
                <h3 className="text-base font-bold text-on-surface">{drg.title}</h3>
                <div className="text-xs text-on-surface-variant mt-0.5">Discipline: {drg.discipline}</div>
              </div>

              {/* Revision history info */}
              <div className="bg-surface p-2.5 rounded-lg border border-outline-variant/60 text-xs space-y-1">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-on-surface flex items-center gap-1">
                    <History className="w-3.5 h-3.5 text-primary" />
                    Latest: {latest?.revisionCode || 'REV-0'}
                  </span>
                  <span className="text-xs font-mono font-medium text-on-surface-variant">
                    {latest?.issueStatus || 'WORKING'}
                  </span>
                </div>
                {latest && (
                  <p className="text-[11px] text-on-surface-variant line-clamp-2 italic">
                    "{latest.revisionNotes || 'Initial baseline'}"
                  </p>
                )}
                {latest?.isAsBuilt && (
                  <span className="text-[11px] text-success font-medium block">✓ As-Built Certified</span>
                )}
              </div>

              <div className="pt-2 border-t border-outline-variant/60 flex items-center justify-between">
                <span className="text-[11px] text-on-surface-variant/70">{revs.length} revisions</span>
                <button
                  type="button"
                  onClick={() => setShowRevisionModal(drg.id)}
                  className="text-xs font-semibold text-primary hover:text-primary flex items-center gap-1"
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4">
          <div className="bg-surface-container-lowest rounded-lg max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60">
              <h3 className="text-base font-bold text-on-surface">Register New Drawing</h3>
              <button
                type="button"
                onClick={() => setShowDrawingModal(false)}
                className="text-on-surface-variant/70 hover:text-on-surface-variant text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateDrawing} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-on-surface mb-1">Drawing Number</label>
                <input
                  type="text"
                  required
                  value={drgForm.drawingNumber}
                  onChange={(e) => setDrgForm({ ...drgForm, drawingNumber: e.target.value })}
                  placeholder="e.g. A-102 or S-301"
                  className="w-full px-3 py-2 border border-outline rounded-lg font-mono"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Title</label>
                <input
                  type="text"
                  required
                  value={drgForm.title}
                  onChange={(e) => setDrgForm({ ...drgForm, title: e.target.value })}
                  placeholder="e.g. First Floor Electrical Layout"
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Discipline</label>
                <select
                  value={drgForm.discipline}
                  onChange={(e) => setDrgForm({ ...drgForm, discipline: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg bg-surface-container-lowest"
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
                  className="rounded text-primary"
                />
                <label htmlFor="hfcTag" className="text-on-surface font-medium">
                  Tag as Held-for-Construction (HFC)
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => setShowDrawingModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4">
          <div className="bg-surface-container-lowest rounded-lg max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60">
              <h3 className="text-base font-bold text-on-surface">Issue Drawing Revision</h3>
              <button
                type="button"
                onClick={() => setShowRevisionModal(null)}
                className="text-on-surface-variant/70 hover:text-on-surface-variant text-sm font-semibold"
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
                <label className="block font-medium text-on-surface mb-1">Revision Code</label>
                <input
                  type="text"
                  required
                  value={revForm.revisionCode}
                  onChange={(e) => setRevForm({ ...revForm, revisionCode: e.target.value })}
                  placeholder="e.g. REV-B or REV-C"
                  className="w-full px-3 py-2 border border-outline rounded-lg font-mono font-bold"
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Revision Source / Trigger</label>
                <select
                  value={revForm.revisionSource}
                  onChange={(e) => setRevForm({ ...revForm, revisionSource: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg bg-surface-container-lowest"
                >
                  <option value="CLIENT_REQUEST">Client Request / Program Change</option>
                  <option value="SITE_CONDITION">Site Condition / Field Clarification</option>
                  <option value="ARCHITECT_REVIEW">Architect Quality Review</option>
                  <option value="COORDINATION_CLASH">MEP / Structural Clash Resolution</option>
                  <option value="STATUTORY_COMPLIANCE">Statutory / Municipal Compliance</option>
                </select>
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Revision Notes & Changes</label>
                <textarea
                  rows={3}
                  required
                  value={revForm.revisionNotes}
                  onChange={(e) => setRevForm({ ...revForm, revisionNotes: e.target.value })}
                  placeholder="Describe exact architectural or structural changes introduced in this revision..."
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                />
              </div>

              <div className="flex items-center gap-2 pt-1">
                <input
                  type="checkbox"
                  id="asBuiltCheck"
                  checked={revForm.isAsBuilt}
                  onChange={(e) => setRevForm({ ...revForm, isAsBuilt: e.target.checked })}
                  className="rounded text-primary"
                />
                <label htmlFor="asBuiltCheck" className="text-on-surface font-medium">
                  Mark as As-Built / Handover Revision
                </label>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => setShowRevisionModal(null)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
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
