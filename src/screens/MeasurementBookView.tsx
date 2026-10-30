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
} from 'lucide-react';

interface MeasurementBookViewProps {
  project: ProjectEntity;
  onNavigateToRecord: () => void;
}

export const MeasurementBookView: React.FC<MeasurementBookViewProps> = ({ project, onNavigateToRecord }) => {
  const [selectedSheetId, setSelectedSheetId] = useState<number | null>(null);
  const [returnComment, setReturnComment] = useState('');
  const [showReturnModal, setShowReturnModal] = useState<number | null>(null);

  const sheets = store.measurementSheets.filter((s) => s.projectId === project.id);
  const measurements = store.measurements.filter((m) => m.projectId === project.id);

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

  const getStatusBadge = (status: MeasurementSheetStatus) => {
    switch (status) {
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
            <CheckCircle2 className="w-3 h-3" /> Approved
          </span>
        );
      case 'CHECKED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">
            <Check className="w-3 h-3" /> Checked
          </span>
        );
      case 'SUBMITTED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-amber-100 text-amber-800">
            <Send className="w-3 h-3" /> Submitted
          </span>
        );
      case 'RETURNED':
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-rose-100 text-rose-800">
            <RotateCcw className="w-3 h-3" /> Returned
          </span>
        );
      case 'DRAFT':
      default:
        return (
          <span className="inline-flex items-center gap-1 text-[11px] font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700">
            Draft
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            <h2 className="text-xl font-bold text-slate-900">Measurement Book (M-Book)</h2>
          </div>
          <p className="text-xs text-slate-500 mt-1">
            Quantity-only verification sheets with formal check & approval audit trail. Strictly free of rates or amounts.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={onNavigateToRecord}
            className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold flex items-center gap-1.5 shadow-sm"
          >
            <Plus className="w-4 h-4" /> Record Measurement
          </button>
        </div>
      </div>

      {sheets.length === 0 ? (
        <div className="bg-white p-12 text-center rounded-xl border border-dashed border-slate-300">
          <BookOpen className="w-12 h-12 text-slate-400 mx-auto mb-2" />
          <h3 className="text-sm font-semibold text-slate-700">No Measurement Sheets Found</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Use "Record Measurement" to enter physical dimensions and generate your first contractor sheet.
          </p>
          <button
            type="button"
            onClick={onNavigateToRecord}
            className="mt-4 px-4 py-2 bg-indigo-600 text-white rounded-lg text-xs font-semibold"
          >
            Record Measurement Now
          </button>
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
                className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm transition hover:border-slate-300"
              >
                {/* Sheet Card Header */}
                <div
                  className="p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 cursor-pointer bg-gradient-to-r from-white to-slate-50/50"
                  onClick={() => setSelectedSheetId(isExpanded ? null : sheet.id)}
                >
                  <div className="flex items-start gap-3">
                    <div className="mt-1 text-slate-400">
                      {isExpanded ? <ChevronDown className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-xs font-bold text-indigo-700 bg-indigo-50 px-2 py-0.5 rounded">
                          {sheet.sheetCode}
                        </span>
                        {getStatusBadge(sheet.status)}
                        {sheet.lockedAt && (
                          <span className="text-[10px] text-slate-500 flex items-center gap-0.5 bg-slate-100 px-1.5 py-0.5 rounded">
                            <Lock className="w-2.5 h-2.5" /> Locked
                          </span>
                        )}
                      </div>

                      <h3 className="text-base font-bold text-slate-900">{sheet.itemNameSnapshot}</h3>

                      <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-slate-500">
                        <span>Contractor: <strong className="text-slate-700">{sheet.contractorNameSnapshot}</strong></span>
                        <span>Level: <strong className="text-slate-700">{sheet.floorNameSnapshot}</strong></span>
                        <span>Formula: <strong className="text-slate-700">{sheet.formulaCode}</strong></span>
                      </div>
                    </div>
                  </div>

                  <div className="flex items-center justify-between md:justify-end gap-4 border-t md:border-t-0 pt-3 md:pt-0">
                    <div className="text-right">
                      <div className="text-[11px] text-slate-400 uppercase tracking-wider">Sheet Total</div>
                      <div className="text-lg font-mono font-bold text-slate-900">
                        {total.toFixed(2)} <span className="text-xs font-normal text-slate-500">{sheet.uomSnapshot}</span>
                      </div>
                      <div className="text-[11px] text-slate-400">{list.length} rows</div>
                    </div>

                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleExportSheet(sheet);
                      }}
                      className="p-2 text-slate-500 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition"
                      title="Export CSV"
                    >
                      <Download className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Expanded Sheet Table & Workflow Actions */}
                {isExpanded && (
                  <div className="border-t border-slate-100 p-5 bg-slate-50/50 space-y-4">
                    {/* Workflow status bar */}
                    <div className="flex flex-wrap items-center justify-between gap-3 bg-white p-3.5 rounded-lg border border-slate-200">
                      <div className="text-xs text-slate-600">
                        <span className="font-semibold text-slate-800">Workflow State: </span>
                        {sheet.status}
                        {sheet.status === 'APPROVED' && ' (Read-only / Approved for record)'}
                      </div>

                      <div className="flex items-center gap-2">
                        {allowed.map((next) => (
                          <button
                            key={next}
                            type="button"
                            onClick={() => handleTransition(sheet, next)}
                            className={`px-3 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition ${
                              next === 'APPROVED'
                                ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                                : next === 'RETURNED'
                                ? 'bg-rose-50 text-rose-700 border border-rose-200 hover:bg-rose-100'
                                : 'bg-indigo-600 hover:bg-indigo-700 text-white'
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
                    <div className="bg-white rounded-lg border border-slate-200 overflow-hidden shadow-xs">
                      <div className="overflow-x-auto">
                        <table className="w-full text-left text-xs">
                          <thead className="bg-slate-100 text-slate-600 font-semibold border-b border-slate-200 text-[11px] uppercase tracking-wider">
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
                          <tbody className="divide-y divide-slate-100">
                            {list.length === 0 ? (
                              <tr>
                                <td colSpan={10} className="px-3 py-6 text-center text-slate-400">
                                  No entries in this sheet yet.
                                </td>
                              </tr>
                            ) : (
                              list.map((m, idx) => (
                                <tr key={m.id} className="hover:bg-slate-50/80">
                                  <td className="px-3 py-2 text-slate-400">{idx + 1}</td>
                                  <td className="px-3 py-2 font-medium text-slate-900">
                                    <div>{m.description || 'General item measurement'}</div>
                                    {m.location && <div className="text-[10px] text-slate-400">{m.location}</div>}
                                  </td>
                                  <td className="px-3 py-2 text-right font-mono">{m.nos}</td>
                                  <td className="px-3 py-2 text-right font-mono">{m.length || '-'}</td>
                                  <td className="px-3 py-2 text-right font-mono">{m.width || '-'}</td>
                                  <td className="px-3 py-2 text-right font-mono">{m.height || '-'}</td>
                                  <td className="px-3 py-2 text-right font-mono text-rose-600">
                                    {m.deduction ? `-${m.deduction}` : '-'}
                                  </td>
                                  <td className="px-3 py-2 text-right font-mono font-bold text-slate-900">
                                    {m.quantity.toFixed(2)}
                                  </td>
                                  <td className="px-3 py-2 text-slate-500">{m.remarks || '-'}</td>
                                  <td className="px-3 py-2 text-center">
                                    {sheet.status !== 'APPROVED' ? (
                                      <button
                                        type="button"
                                        onClick={() => store.deleteMeasurement(m.id)}
                                        className="p-1 text-slate-400 hover:text-rose-600"
                                        title="Delete measurement"
                                      >
                                        <Trash2 className="w-3.5 h-3.5" />
                                      </button>
                                    ) : (
                                      <span className="text-[10px] text-slate-400">Locked</span>
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
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-xs p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center gap-2 text-rose-600 pb-2 border-b border-slate-100">
              <AlertTriangle className="w-5 h-5" />
              <h3 className="text-base font-bold text-slate-800">Return Sheet for Rectification</h3>
            </div>
            <p className="text-xs text-slate-600">
              Per practice audit rules, returning a measurement sheet requires an explanatory comment for the contractor / site team.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Return Reason / Required Correction</label>
              <textarea
                rows={3}
                required
                value={returnComment}
                onChange={(e) => setReturnComment(e.target.value)}
                placeholder="e.g. Deductions for opening D1 missing in Wall Plaster measurement..."
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => {
                  setShowReturnModal(null);
                  setReturnComment('');
                }}
                className="px-4 py-2 border border-slate-200 rounded-lg text-xs font-medium text-slate-600 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!returnComment.trim()}
                onClick={() => handleConfirmReturn(showReturnModal)}
                className="px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-semibold disabled:opacity-50"
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
