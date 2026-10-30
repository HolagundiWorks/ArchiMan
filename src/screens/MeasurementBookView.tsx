import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, MeasurementSheetEntity, MeasurementEntity, MeasurementSheetStatus } from '../types';
import { MeasurementSheetWorkflow } from '../domain/MeasurementSheetWorkflow';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  BookOpen,
  Plus,
  Download,
  ChevronDown,
  ChevronRight,
  CheckCircle2,
  AlertTriangle,
  RotateCcw,
  Check,
  Send,
  Layers,
  Lock,
  Trash2,
  Calendar,
  Search,
  Filter,
} from 'lucide-react';

interface MeasurementBookViewProps {
  project: ProjectEntity;
  onNavigateToRecord: () => void;
}

export const MeasurementBookView: React.FC<MeasurementBookViewProps> = ({ project, onNavigateToRecord }) => {
  const [selectedSheetId, setSelectedSheetId] = useState<number | null>(null);
  const [statusFilter, setStatusFilter] = useState<'ALL' | MeasurementSheetStatus>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [returnComment, setReturnComment] = useState('');
  const [showReturnModal, setShowReturnModal] = useState<number | null>(null);

  const allSheets = store.measurementSheets.filter((s) => s.projectId === project.id);
  const measurements = store.measurements.filter((m) => m.projectId === project.id);

  const sheets = allSheets.filter((s) => {
    const matchesStatus = statusFilter === 'ALL' || s.status === statusFilter;
    const matchesSearch =
      s.sheetCode.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.itemNameSnapshot.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.contractorNameSnapshot.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.floorNameSnapshot.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesStatus && matchesSearch;
  });

  const getSheetMeasurements = (sheetId: number) => {
    return measurements.filter((m) => m.sheetId === sheetId);
  };

  const getSheetTotal = (sheetId: number) => {
    const list = getSheetMeasurements(sheetId);
    return list.reduce((sum, m) => sum + m.quantity, 0);
  };

  const handleTransition = (sheet: MeasurementSheetEntity, nextStatus: MeasurementSheetStatus) => {
    if (nextStatus === 'RETURNED') {
      setShowReturnModal(sheet.id);
      return;
    }
    store.updateSheetStatus(sheet.id, nextStatus);
  };

  const handleConfirmReturn = (sheetId: number) => {
    if (!returnComment.trim()) return;
    store.updateSheetStatus(sheetId, 'RETURNED');
    setShowReturnModal(null);
    setReturnComment('');
  };

  const handleExportSheet = (sheet: MeasurementSheetEntity) => {
    const list = getSheetMeasurements(sheet.id);
    let csv = `MEASUREMENT BOOK (QUANTITY ONLY)\n`;
    csv += `Project:,"${project.name} (${project.projectCode})"\n`;
    csv += `Sheet Code:,"${sheet.sheetCode}"\n`;
    csv += `Floor:,"${sheet.floorNameSnapshot}"\n`;
    csv += `Contractor:,"${sheet.contractorNameSnapshot}"\n`;
    csv += `Work Item:,"${sheet.itemNameSnapshot}"\n`;
    csv += `Formula:,"${sheet.formulaCode}"\n`;
    csv += `Status:,"${sheet.status}"\n\n`;
    csv += `No.,Description,Location,Length,Width,Height,Nos,Deduction,Quantity (${sheet.uomSnapshot}),Remarks,Date\n`;

    list.forEach((m, idx) => {
      csv += `${idx + 1},"${m.description}","${m.location}",${m.length},${m.width},${m.height},${m.nos},${m.deduction},${m.quantity.toFixed(2)},"${m.remarks}","${new Date(m.date).toLocaleDateString()}"\n`;
    });

    csv += `\nTotal Executed Quantity:,,, ,,,,${getSheetTotal(sheet.id).toFixed(2)} ${sheet.uomSnapshot}\n`;
    ExportHelper.downloadCSV(`MBook_${sheet.sheetCode}.csv`, csv);
  };

  const renderStatus = (status: MeasurementSheetStatus) => {
    switch (status) {
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-success">
            <CheckCircle2 className="w-3.5 h-3.5" /> Approved
          </span>
        );
      case 'CHECKED':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-blue-700">
            <Check className="w-3.5 h-3.5" /> Checked
          </span>
        );
      case 'SUBMITTED':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-warning">
            <Send className="w-3.5 h-3.5" /> Submitted
          </span>
        );
      case 'RETURNED':
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-rose-700">
            <RotateCcw className="w-3.5 h-3.5" /> Returned
          </span>
        );
      case 'DRAFT':
      default:
        return (
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-on-surface-variant">
            Draft
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-surface-container-lowest p-5 rounded-lg border border-outline-variant elevation-1">
        <div>
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-primary" />
            <h2 className="text-xl font-bold text-on-surface">Measurement Book (M-Book)</h2>
          </div>
          <p className="text-xs text-on-surface-variant mt-1">
            Quantity-only audit sheets with formal verification lifecycle. Strict architectural boundary: no rates or commercial invoices.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onNavigateToRecord}
            className="px-3.5 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold flex items-center gap-1.5 elevation-1"
          >
            <Plus className="w-4 h-4" /> Record Measurement
          </button>
        </div>
      </div>

      {/* Filter & Search Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-surface-container-lowest p-3 rounded-md border border-outline-variant elevation-1">
        <div className="flex overflow-x-auto gap-1 p-0.5 bg-surface-container-low rounded-lg">
          {(['ALL', 'DRAFT', 'SUBMITTED', 'CHECKED', 'APPROVED', 'RETURNED'] as const).map((status) => {
            const count = status === 'ALL' ? allSheets.length : allSheets.filter((s) => s.status === status).length;
            const isActive = statusFilter === status;
            return (
              <button
                key={status}
                type="button"
                onClick={() => setStatusFilter(status)}
                className={`px-3 py-1.5 text-xs font-medium rounded-md transition whitespace-nowrap flex items-center gap-1.5 ${
                  isActive
                    ? 'bg-surface-container-lowest text-on-surface elevation-1 font-semibold'
                    : 'text-on-surface-variant hover:text-on-surface'
                }`}
              >
                <span>{status === 'ALL' ? 'All Sheets' : status}</span>
                <span className="text-[11px] font-mono font-medium text-on-surface-variant/70">
                  ({count})
                </span>
              </button>
            );
          })}
        </div>

        <div className="relative w-full sm:w-64">
          <Search className="w-3.5 h-3.5 text-on-surface-variant/70 absolute left-3 top-2.5" />
          <input
            type="text"
            placeholder="Search sheets, items, trades..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-8 pr-3 py-1.5 bg-surface border border-outline-variant rounded-lg text-xs focus:bg-surface-container-lowest"
          />
        </div>
      </div>

      {sheets.length === 0 ? (
        <div className="bg-surface-container-lowest p-12 text-center rounded-md border border-dashed border-outline">
          <BookOpen className="w-12 h-12 text-on-surface-variant/70 mx-auto mb-2" />
          <h3 className="text-sm font-semibold text-on-surface">No Measurement Sheets Found</h3>
          <p className="text-xs text-on-surface-variant mt-1 max-w-sm mx-auto">
            {allSheets.length === 0
              ? 'Use "Record Measurement" to enter physical dimensions and generate your first contractor sheet.'
              : 'No sheets matched your current filter criteria.'}
          </p>
          {allSheets.length === 0 && (
            <button
              type="button"
              onClick={onNavigateToRecord}
              className="mt-4 px-4 py-2 bg-primary text-on-primary rounded-lg text-xs font-semibold"
            >
              Record Measurement Now
            </button>
          )}
        </div>
      ) : (
        <div className="space-y-4">
          {sheets.map((sheet) => {
            const list = getSheetMeasurements(sheet.id);
            const total = getSheetTotal(sheet.id);
            const isExpanded = selectedSheetId === sheet.id;
            const allowed = MeasurementSheetWorkflow.allowedTransitions(sheet.status);

            return (
              <div
                key={sheet.id}
                className="bg-surface-container-lowest rounded-md border border-outline-variant overflow-hidden elevation-1 transition hover:border-outline"
              >
                {/* Sheet Card Header */}
                <div
                  className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 cursor-pointer bg-gradient-to-r from-surface-container-lowest to-surface-container-low"
                  onClick={() => setSelectedSheetId(isExpanded ? null : sheet.id)}
                >
                  <div className="flex items-start gap-3">
                    <div className="mt-1 text-on-surface-variant/70">
                      {isExpanded ? <ChevronDown className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2.5 text-xs text-on-surface-variant">
                        <span className="font-mono font-bold text-primary bg-primary-container px-2 py-0.5 rounded">
                          {sheet.sheetCode}
                        </span>
                        <span aria-hidden="true">·</span>
                        {renderStatus(sheet.status)}
                        {sheet.lockedAt && (
                          <>
                            <span aria-hidden="true">·</span>
                            <span className="text-[11px] text-on-surface-variant flex items-center gap-1">
                              <Lock className="w-3 h-3" /> Locked
                            </span>
                          </>
                        )}
                      </div>

                      <h3 className="text-base font-bold text-on-surface">{sheet.itemNameSnapshot}</h3>

                      <div className="flex flex-wrap items-center gap-x-3 text-xs text-on-surface-variant">
                        <span>Contractor: <strong className="text-on-surface">{sheet.contractorNameSnapshot}</strong></span>
                        <span aria-hidden="true">·</span>
                        <span>Level: <strong className="text-on-surface">{sheet.floorNameSnapshot}</strong></span>
                        <span aria-hidden="true">·</span>
                        <span>Formula: <span className="font-mono text-on-surface">{sheet.formulaCode}</span></span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between md:justify-end gap-4 border-t md:border-t-0 pt-3 md:pt-0">
                    <div className="text-right">
                      <div className="text-[10px] text-on-surface-variant/70 font-medium uppercase tracking-wider">Executed Quantity</div>
                      <div className="text-lg font-mono font-bold text-on-surface tabular-nums">
                        {total.toFixed(2)} <span className="text-xs font-normal text-on-surface-variant">{sheet.uomSnapshot}</span>
                      </div>
                      <div className="text-[11px] text-on-surface-variant/70">{list.length} entries</div>
                    </div>

                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleExportSheet(sheet);
                      }}
                      className="p-2 text-on-surface-variant hover:text-primary hover:bg-primary-container rounded-lg transition"
                      title="Export CSV"
                    >
                      <Download className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Expanded Sheet Table & Workflow Actions */}
                {isExpanded && (
                  <div className="border-t border-outline-variant/60 p-5 bg-surface/50 space-y-4">
                    {/* Workflow status bar */}
                    <div className="flex flex-wrap items-center justify-between gap-3 bg-surface-container-lowest p-3.5 rounded-lg border border-outline-variant">
                      <div className="text-xs text-on-surface-variant">
                        <span className="font-semibold text-on-surface">Review Status: </span>
                        <span className="font-medium text-on-surface">{sheet.status}</span>
                        {sheet.status === 'APPROVED' && ' (Verified & locked for record)'}
                      </div>

                      <div className="flex items-center gap-2">
                        {allowed.map((next) => (
                          <button
                            key={next}
                            type="button"
                            onClick={() => handleTransition(sheet, next)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition ${
                              next === 'APPROVED'
                                ? 'bg-success hover:brightness-95 text-on-success'
                                : next === 'RETURNED'
                                ? 'bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100'
                                : 'bg-primary hover:bg-primary text-on-primary'
                            }`}
                          >
                            {next === 'SUBMITTED' && <Send className="w-3 h-3" />}
                            {next === 'CHECKED' && <Check className="w-3 h-3" />}
                            {next === 'APPROVED' && <CheckCircle2 className="w-3 h-3" />}
                            {next === 'RETURNED' && <RotateCcw className="w-3 h-3" />}
                            {next === 'SUBMITTED' ? 'Submit Sheet' : next === 'CHECKED' ? 'Mark Checked' : next === 'APPROVED' ? 'Approve' : 'Return Sheet'}
                          </button>
                        ))}
                      </div>
                    </div>

                    {/* Table of measurements */}
                    <div className="bg-surface-container-lowest rounded-lg border border-outline-variant overflow-hidden elevation-1">
                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs">
                          <thead className="bg-surface-container-low text-on-surface-variant font-semibold border-b border-outline-variant text-[11px] uppercase tracking-wider">
                            <tr>
                              <th className="px-3 py-2.5">#</th>
                              <th className="px-3 py-2.5">Description & Location</th>
                              <th className="px-3 py-2.5 text-right">Nos</th>
                              <th className="px-3 py-2.5 text-right">Length</th>
                              <th className="px-3 py-2.5 text-right">Breadth</th>
                              <th className="px-3 py-2.5 text-right">Height</th>
                              <th className="px-3 py-2.5 text-right">Ded.</th>
                              <th className="px-3 py-2.5 text-right">Quantity ({sheet.uomSnapshot})</th>
                              <th className="px-3 py-2.5">Remarks</th>
                              <th className="px-3 py-2.5 text-center">Actions</th>
                            </tr>
                          </thead>
                          <tbody className="divide-y divide-outline-variant/60">
                            {list.length === 0 ? (
                              <tr>
                                <td colSpan={10} className="px-3 py-6 text-center text-on-surface-variant/70">
                                  No entries in this sheet yet.
                                </td>
                              </tr>
                            ) : (
                              list.map((m, idx) => (
                                <tr key={m.id} className="hover:bg-surface/80">
                                  <td className="px-3 py-2 text-on-surface-variant/70 font-mono">{idx + 1}</td>
                                  <td className="px-3 py-2 font-medium text-on-surface">
                                    <div>{m.description || 'General measurement entry'}</div>
                                    {m.location && <div className="text-[11px] text-on-surface-variant/70 font-normal">{m.location}</div>}
                                  </td>
                                  <td className="px-3 py-2 text-right font-mono tabular-nums">{m.nos}</td>
                                  <td className="px-3 py-2 text-right font-mono tabular-nums">{m.length || '-'}</td>
                                  <td className="px-3 py-2 text-right font-mono tabular-nums">{m.width || '-'}</td>
                                  <td className="px-3 py-2 text-right font-mono tabular-nums">{m.height || '-'}</td>
                                  <td className="px-3 py-2 text-right font-mono tabular-nums text-rose-600">
                                    {m.deduction ? `-${m.deduction}` : '-'}
                                  </td>
                                  <td className="px-3 py-2 text-right font-mono font-bold text-on-surface tabular-nums">
                                    {m.quantity.toFixed(2)}
                                  </td>
                                  <td className="px-3 py-2 text-on-surface-variant">{m.remarks || '-'}</td>
                                  <td className="px-3 py-2 text-center">
                                    {sheet.status !== 'APPROVED' ? (
                                      <button
                                        type="button"
                                        onClick={() => store.deleteMeasurement(m.id)}
                                        className="p-1 text-on-surface-variant/70 hover:text-rose-600"
                                        title="Delete entry"
                                      >
                                        <Trash2 className="w-3.5 h-3.5" />
                                      </button>
                                    ) : (
                                      <span className="text-[10px] text-on-surface-variant/70">Locked</span>
                                    )}
                                  </td>
                                </tr>
                              ))
                            )}
                          </tbody>
                        </table>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>
      )}

      {/* Return Sheet Modal */}
      {showReturnModal !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4">
          <div className="bg-surface-container-lowest rounded-lg max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center gap-2 text-rose-600 pb-2 border-b border-outline-variant/60">
              <AlertTriangle className="w-5 h-5" />
              <h3 className="text-base font-bold text-on-surface">Return Sheet for Rectification</h3>
            </div>
            <p className="text-xs text-on-surface-variant">
              Per practice audit rules, returning a measurement sheet requires an explanatory comment for the contractor / site team.
            </p>
            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1">Return Reason / Required Correction</label>
              <textarea
                rows={3}
                required
                value={returnComment}
                onChange={(e) => setReturnComment(e.target.value)}
                placeholder="e.g. Deductions for opening D1 missing in Wall Plaster measurement..."
                className="w-full px-3 py-2 border border-outline rounded-lg text-xs"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  setShowReturnModal(null);
                  setReturnComment('');
                }}
                className="px-4 py-2 border border-outline-variant rounded-lg text-xs font-medium text-on-surface-variant hover:bg-surface"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!returnComment.trim()}
                onClick={() => handleConfirmReturn(showReturnModal)}
                className="px-4 py-2 bg-error hover:brightness-95 text-on-error rounded-lg text-xs font-semibold disabled:opacity-50"
              >
                Return Sheet
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
