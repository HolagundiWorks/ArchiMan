import React, { useState } from 'react';
import { store } from '../data/storage';
import { ProjectEntity, MaterialOrderEntity, DailySiteReportEntity } from '../types';
import { ArchiCalcPad } from '../components/ArchiCalcPad';
import { ExportHelper } from '../domain/WorkCatalog';
import {
  ClipboardList,
  FileCheck2,
  Package,
  ShoppingCart,
  Calculator,
  Plus,
  Trash2,
  Download,
  Send,
  Calendar,
  Layers,
  FileText,
  AlertCircle,
  FileBox,
} from 'lucide-react';
import {
  Button,
  EmptyState,
  PageHeader,
  SegmentedTabs,
  Surface,
} from '../components/ui';


interface SitemanViewProps {
  project: ProjectEntity;
  onNavigateToMBook: () => void;
  onOpenODraft?: () => void;
}

export const SitemanView: React.FC<SitemanViewProps> = ({
  project,
  onNavigateToMBook,
  onOpenODraft,
}) => {
  const [activeTab, setActiveTab] = useState<'LOGS' | 'HFC' | 'ABSTRACT' | 'POS' | 'CALC'>('LOGS');

  // Logs state
  const [showLogModal, setShowLogModal] = useState(false);
  const [logForm, setLogForm] = useState({
    weather: 'Sunny, 30°C',
    manpower: '',
    workCompleted: '',
    materialsReceived: '',
    delaysOrConstraints: '',
    safetyObservations: '',
    nextDayPlan: '',
    preparedBy: '',
  });

  // Material PO state
  const [showPOModal, setShowPOModal] = useState(false);
  const [poForm, setPOForm] = useState({
    title: '',
    supplier: '',
    referenceNumber: `PO-${project.projectCode}-${Date.now().toString().slice(-4)}`,
    notes: '',
  });
  const [poLines, setPOLines] = useState<{ itemName: string; specification: string; quantity: number; unit: string; remarks: string }[]>([
    { itemName: '', specification: '', quantity: 1, unit: 'Nos', remarks: '' },
  ]);

  // HFC site document state
  const [showDocModal, setShowDocModal] = useState(false);
  const [docForm, setDocForm] = useState({
    title: '',
    category: 'HFC' as const,
    notes: '',
  });

  const dailyLogs = store.dailyReports.filter((r) => r.projectId === project.id);
  const hfcDrawings = store.drawings.filter((d) => d.projectId === project.id && d.packageTag === 'HFC');
  const siteDocuments = store.siteDocuments.filter((d) => d.projectId === project.id);
  const projectPOs = store.materialOrders.filter((o) => o.projectId === project.id);
  const measurements = store.measurements.filter((m) => m.projectId === project.id);

  // Compute Quantity Abstract
  const quantityAbstract = React.useMemo(() => {
    const map = new Map<string, { itemName: string; unit: string; totalQty: number; entriesCount: number }>();
    measurements.forEach((m) => {
      const existing = map.get(m.itemName) || { itemName: m.itemName, unit: m.unit, totalQty: 0, entriesCount: 0 };
      existing.totalQty += m.quantity;
      existing.entriesCount += 1;
      map.set(m.itemName, existing);
    });
    return Array.from(map.values()).sort((a, b) => b.totalQty - a.totalQty);
  }, [measurements]);

  const handleExportAbstract = () => {
    let csv = `Quantity Abstract - ${project.name} (${project.projectCode})\n`;
    csv += `Work Item,Unit of Measure,Total Quantity,Recorded Measurements\n`;
    quantityAbstract.forEach((item) => {
      csv += `"${item.itemName}","${item.unit}",${item.totalQty.toFixed(2)},${item.entriesCount}\n`;
    });
    ExportHelper.downloadCSV(`Quantity_Abstract_${project.projectCode}.csv`, csv);
  };

  const handleCreateLog = (e: React.FormEvent) => {
    e.preventDefault();
    if (!logForm.workCompleted.trim()) return;
    store.addDailyReport({
      projectId: project.id,
      reportDate: Date.now(),
      weather: logForm.weather,
      manpower: logForm.manpower,
      workCompleted: logForm.workCompleted,
      materialsReceived: logForm.materialsReceived,
      delaysOrConstraints: logForm.delaysOrConstraints,
      safetyObservations: logForm.safetyObservations,
      nextDayPlan: logForm.nextDayPlan,
      preparedBy: logForm.preparedBy || 'Site Manager',
      photoUri: null,
    });
    setShowLogModal(false);
    setLogForm({
      weather: 'Sunny, 30°C',
      manpower: '',
      workCompleted: '',
      materialsReceived: '',
      delaysOrConstraints: '',
      safetyObservations: '',
      nextDayPlan: '',
      preparedBy: '',
    });
  };

  const handleCreatePO = (e: React.FormEvent) => {
    e.preventDefault();
    if (!poForm.title.trim()) return;
    store.addMaterialOrder(
      {
        projectId: project.id,
        referenceNumber: poForm.referenceNumber,
        title: poForm.title,
        supplier: poForm.supplier,
        status: 'DRAFT',
        notes: poForm.notes,
        issuedAt: null,
      },
      poLines.filter((l) => l.itemName.trim())
    );
    setShowPOModal(false);
    setPOForm({
      title: '',
      supplier: '',
      referenceNumber: `PO-${project.projectCode}-${Date.now().toString().slice(-4)}`,
      notes: '',
    });
    setPOLines([{ itemName: '', specification: '', quantity: 1, unit: 'Nos', remarks: '' }]);
  };

  const handleExportPO = (order: MaterialOrderEntity) => {
    const lines = store.materialOrderLines.filter((l) => l.orderId === order.id);
    let csv = `PURCHASE ORDER (QUANTITY & SPECIFICATION ONLY)\n`;
    csv += `Project:,"${project.name} (${project.projectCode})"\n`;
    csv += `PO Number:,"${order.referenceNumber}"\n`;
    csv += `Title:,"${order.title}"\n`;
    csv += `Supplier:,"${order.supplier}"\n`;
    csv += `Status:,"${order.status}"\n\n`;
    csv += `Item Name,Specification,Quantity,Unit,Remarks\n`;
    lines.forEach((l) => {
      csv += `"${l.itemName}","${l.specification}",${l.quantity},"${l.unit}","${l.remarks}"\n`;
    });
    ExportHelper.downloadCSV(`PO_${order.referenceNumber}.csv`, csv);
  };

  const handleCreateDoc = (e: React.FormEvent) => {
    e.preventDefault();
    if (!docForm.title.trim()) return;
    store.addSiteDocument({
      projectId: project.id,
      title: docForm.title,
      category: docForm.category,
      notes: docForm.notes,
      linkedDrawingId: null,
      fileUri: null,
    });
    setShowDocModal(false);
    setDocForm({ title: '', category: 'HFC', notes: '' });
  };

  const primaryAction =
    activeTab === 'LOGS' ? (
      <Button leadingIcon={<Plus className="w-4 h-4" />} onClick={() => setShowLogModal(true)}>
        Add daily log
      </Button>
    ) : activeTab === 'HFC' ? (
      <Button leadingIcon={<Plus className="w-4 h-4" />} onClick={() => setShowDocModal(true)}>
        Add site document
      </Button>
    ) : activeTab === 'ABSTRACT' ? (
      <Button variant="tonal" leadingIcon={<Download className="w-4 h-4" />} onClick={handleExportAbstract}>
        Export abstract CSV
      </Button>
    ) : activeTab === 'POS' ? (
      <Button leadingIcon={<Plus className="w-4 h-4" />} onClick={() => setShowPOModal(true)}>
        Create material PO
      </Button>
    ) : null;

  return (
    <div className="space-y-5">
      <Surface tone="lowest" elevation={1} padding="lg" className="space-y-5">
        <PageHeader
          eyebrow="Site operations · quantity-only"
          title="Siteman"
          description="Daily logs, Held-for-Construction drawings and documents, quantity abstracts, material purchase orders, and Archi Calc."
          actions={
            <div className="flex items-center gap-2 flex-wrap justify-end">
              {onOpenODraft && (
                <Button
                  size="sm"
                  variant="outlined"
                  leadingIcon={<FileBox className="w-3.5 h-3.5" />}
                  onClick={onOpenODraft}
                >
                  Read .osp
                </Button>
              )}
              {primaryAction}
            </div>
          }
        />
        <SegmentedTabs
          value={activeTab}
          onChange={setActiveTab}
          items={[
            { id: 'LOGS', label: 'Site logs', icon: <ClipboardList className="w-4 h-4" />, count: dailyLogs.length },
            {
              id: 'HFC',
              label: 'HFC docs',
              icon: <FileCheck2 className="w-4 h-4" />,
              count: hfcDrawings.length + siteDocuments.length,
            },
            {
              id: 'ABSTRACT',
              label: 'Quantity abstract',
              icon: <Package className="w-4 h-4" />,
              count: quantityAbstract.length,
            },
            { id: 'POS', label: 'Purchase orders', icon: <ShoppingCart className="w-4 h-4" />, count: projectPOs.length },
            { id: 'CALC', label: 'Archi Calc', icon: <Calculator className="w-4 h-4" /> },
          ]}
        />
      </Surface>

      {/* Tab 1: Daily Site Logs */}
      {activeTab === 'LOGS' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="type-title-md">Site daily activity logs</h3>
            <span className="type-body-sm">{dailyLogs.length} recorded</span>
          </div>

          {dailyLogs.length === 0 ? (
            <EmptyState
              icon={<ClipboardList />}
              title="No site logs yet"
              description="Record weather, manpower, work completed, materials, delays and the next-day plan."
              actionLabel="Add first daily log"
              onAction={() => setShowLogModal(true)}
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {dailyLogs.map((log) => (
                <div key={log.id} className="bg-surface-container-lowest rounded-md p-5 border border-outline-variant elevation-1 space-y-3">
                  <div className="flex items-center justify-between border-b border-outline-variant/60 pb-2.5">
                    <div className="flex items-center gap-2 text-xs font-semibold text-on-surface">
                      <Calendar className="w-4 h-4 text-primary" />
                      <span>{new Date(log.reportDate).toLocaleDateString(undefined, { dateStyle: 'medium' })}</span>
                    </div>
                    <span className="text-xs px-2 py-0.5 bg-surface-container-low text-on-surface-variant rounded">
                      {log.weather}
                    </span>
                  </div>

                  <div>
                    <div className="text-xs font-medium text-on-surface-variant">Work Completed:</div>
                    <div className="text-sm text-on-surface mt-0.5 leading-relaxed">{log.workCompleted}</div>
                  </div>

                  {log.manpower && (
                    <div>
                      <div className="text-xs font-medium text-on-surface-variant">Manpower:</div>
                      <div className="text-xs text-on-surface mt-0.5">{log.manpower}</div>
                    </div>
                  )}

                  {log.materialsReceived && (
                    <div className="p-2.5 bg-warning-container/60 rounded border border-amber-100/80">
                      <div className="text-xs font-medium text-amber-800">Materials Received:</div>
                      <div className="text-xs text-amber-900 mt-0.5">{log.materialsReceived}</div>
                    </div>
                  )}

                  {log.nextDayPlan && (
                    <div className="text-xs text-on-surface-variant">
                      <span className="font-medium text-on-surface">Next Day Plan: </span>
                      {log.nextDayPlan}
                    </div>
                  )}

                  <div className="text-[11px] text-on-surface-variant/70 pt-2 border-t border-outline-variant/60 flex justify-between">
                    <span>Prepared by: {log.preparedBy}</span>
                    <span>{new Date(log.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 2: HFC Drawings & Supporting Docs */}
      {activeTab === 'HFC' && (
        <div className="space-y-6">
          {/* HFC Drawings */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <div>
                <h3 className="text-base font-semibold text-on-surface">Held-for-Construction (HFC) Drawings</h3>
                <p className="text-xs text-on-surface-variant">Drawings stamped and tagged for site execution</p>
              </div>
            </div>

            {hfcDrawings.length === 0 ? (
              <div className="bg-surface-container-lowest p-6 text-center rounded-md border border-outline-variant text-xs text-on-surface-variant">
                No drawings currently tagged as HFC. You can tag drawing registers as HFC under Drawings.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                {hfcDrawings.map((drg) => {
                  const revs = store.drawingRevisions.filter((r) => r.drawingId === drg.id);
                  const latestRev = revs[revs.length - 1];
                  return (
                    <div key={drg.id} className="bg-surface-container-lowest p-4 rounded-md border border-outline-variant elevation-1 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-primary bg-primary-container px-2 py-0.5 rounded">
                          {drg.drawingNumber}
                        </span>
                        <span className="text-[10px] font-semibold px-2 py-0.5 bg-emerald-100 text-emerald-800 rounded-full">
                          HFC Active
                        </span>
                      </div>
                      <div className="text-sm font-semibold text-on-surface">{drg.title}</div>
                      <div className="text-xs text-on-surface-variant flex items-center justify-between">
                        <span>Discipline: {drg.discipline}</span>
                        <span>Latest: {latestRev?.revisionCode || 'REV-0'}</span>
                      </div>
                      {latestRev && (
                        <div className="text-xs text-on-surface-variant bg-surface p-2 rounded border border-outline-variant/60">
                          {latestRev.revisionNotes}
                        </div>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Site Supporting Documents */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <div>
                <h3 className="text-base font-semibold text-on-surface">Site Supporting Documents & Packs</h3>
                <p className="text-xs text-on-surface-variant">Method statements, QC checklists, and package notes</p>
              </div>
            </div>

            {siteDocuments.length === 0 ? (
              <div className="bg-surface-container-lowest p-6 text-center rounded-md border border-outline-variant text-xs text-on-surface-variant">
                No site documents uploaded yet.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {siteDocuments.map((doc) => (
                  <div key={doc.id} className="bg-surface-container-lowest p-4 rounded-md border border-outline-variant elevation-1 flex items-start gap-3">
                    <FileText className="w-6 h-6 text-primary shrink-0 mt-0.5" />
                    <div className="space-y-1 flex-1">
                      <div className="flex items-center justify-between">
                        <h4 className="text-sm font-semibold text-on-surface">{doc.title}</h4>
                        <span className="text-[10px] font-medium px-2 py-0.5 bg-surface-container-low text-on-surface-variant rounded">
                          {doc.category}
                        </span>
                      </div>
                      <p className="text-xs text-on-surface-variant">{doc.notes}</p>
                      <div className="text-[11px] text-on-surface-variant/70">
                        Added: {new Date(doc.createdAt).toLocaleDateString()}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 3: Quantity Abstract */}
      {activeTab === 'ABSTRACT' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-semibold text-on-surface">Aggregated Quantity Abstract</h3>
              <p className="text-xs text-on-surface-variant">
                Auto-computed totals from approved and draft measurement book sheets
              </p>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onNavigateToMBook}
                className="px-3 py-1.5 bg-surface-container-low hover:bg-surface-container-high text-on-surface rounded-lg text-xs font-semibold flex items-center gap-1.5"
              >
                <Layers className="w-3.5 h-3.5" /> Open Measurement Book
              </button>
              <button
                type="button"
                onClick={handleExportAbstract}
                className="px-3 py-1.5 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold flex items-center gap-1.5 elevation-1"
              >
                <Download className="w-3.5 h-3.5" /> Export CSV
              </button>
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-md border border-outline-variant overflow-hidden elevation-1">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-surface text-on-surface-variant font-semibold border-b border-outline-variant uppercase tracking-wider text-[11px]">
                  <tr>
                    <th className="px-4 py-3">Work Item</th>
                    <th className="px-4 py-3">Unit (UOM)</th>
                    <th className="px-4 py-3 text-right">Total Executed Qty</th>
                    <th className="px-4 py-3 text-center">Entries</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/60 text-on-surface">
                  {quantityAbstract.length === 0 ? (
                    <tr>
                      <td colSpan={4} className="px-4 py-8 text-center text-on-surface-variant/70">
                        No measurement records found for this project yet.
                      </td>
                    </tr>
                  ) : (
                    quantityAbstract.map((row, i) => (
                      <tr key={i} className="hover:bg-surface/60">
                        <td className="px-4 py-3 font-medium text-on-surface">{row.itemName}</td>
                        <td className="px-4 py-3">{row.unit}</td>
                        <td className="px-4 py-3 text-right font-mono font-bold text-on-surface">
                          {row.totalQty.toFixed(2)}
                        </td>
                        <td className="px-4 py-3 text-center text-on-surface-variant">{row.entriesCount}</td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Tab 4: Material Purchase Orders (Strictly Quantity & Specification Only) */}
      {activeTab === 'POS' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-base font-semibold text-on-surface">Material Purchase Orders</h3>
              <p className="text-xs text-on-surface-variant">
                Quantity-only and technical specification orders. No commercial rates, amounts, or invoices.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setShowPOModal(true)}
              className="px-3.5 py-1.5 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-semibold flex items-center gap-1.5 elevation-1"
            >
              <Plus className="w-3.5 h-3.5" /> Create Order
            </button>
          </div>

          <div className="bg-warning-container border border-amber-200 p-3 rounded-lg flex items-start gap-2 text-xs text-amber-800">
            <AlertCircle className="w-4 h-4 text-warning shrink-0 mt-0.5" />
            <div>
              <span className="font-semibold">ArchiMan Product Boundary: </span>
              In strict compliance with architectural practice independence, material orders retain quantities and
              engineering specs only. Drafts support create, issue, CSV export, and deletion.
            </div>
          </div>

          {projectPOs.length === 0 ? (
            <div className="bg-surface-container-lowest p-12 text-center rounded-md border border-dashed border-outline">
              <ShoppingCart className="w-10 h-10 text-on-surface-variant/70 mx-auto mb-2" />
              <p className="text-sm font-medium text-on-surface">No material orders created yet</p>
              <p className="text-xs text-on-surface-variant mt-1">
                Generate site material requisition orders with exact specs and quantities.
              </p>
              <button
                type="button"
                onClick={() => setShowPOModal(true)}
                className="mt-4 px-4 py-2 bg-primary text-on-primary rounded-lg text-xs font-semibold"
              >
                Create Purchase Order
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {projectPOs.map((po) => {
                const lines = store.materialOrderLines.filter((l) => l.orderId === po.id);
                return (
                  <div key={po.id} className="bg-surface-container-lowest rounded-md border border-outline-variant p-5 elevation-1 space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-outline-variant/60 pb-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="text-xs font-mono font-bold text-primary bg-primary-container px-2 py-0.5 rounded">
                            {po.referenceNumber}
                          </span>
                          <span
                            className={`text-[10px] font-semibold px-2 py-0.5 rounded-full ${
                              po.status === 'ISSUED'
                                ? 'bg-emerald-100 text-emerald-800'
                                : 'bg-surface-container-low text-on-surface'
                            }`}
                          >
                            {po.status}
                          </span>
                        </div>
                        <h4 className="text-base font-semibold text-on-surface mt-1">{po.title}</h4>
                        <div className="text-xs text-on-surface-variant">
                          Supplier: <span className="font-medium text-on-surface">{po.supplier || 'Unassigned'}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        {po.status === 'DRAFT' && (
                          <button
                            type="button"
                            onClick={() => store.issueMaterialOrder(po.id)}
                            className="px-3 py-1.5 bg-success hover:brightness-95 text-on-success rounded-lg text-xs font-semibold flex items-center gap-1 elevation-1"
                          >
                            <Send className="w-3.5 h-3.5" /> Issue PO
                          </button>
                        )}
                        <button
                          type="button"
                          onClick={() => handleExportPO(po)}
                          className="px-3 py-1.5 bg-surface-container-low hover:bg-surface-container-high text-on-surface rounded-lg text-xs font-semibold flex items-center gap-1"
                        >
                          <Download className="w-3.5 h-3.5" /> CSV
                        </button>
                        <button
                          type="button"
                          onClick={() => store.deleteMaterialOrder(po.id)}
                          className="p-1.5 text-on-surface-variant/70 hover:text-rose-600 hover:bg-rose-50 rounded"
                          title="Delete PO"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>

                    {/* PO Line Items */}
                    <div className="overflow-x-auto">
                      <table className="w-full text-left text-xs">
                        <thead className="bg-surface text-on-surface-variant border-b border-outline-variant">
                          <tr>
                            <th className="px-3 py-2">Item Name</th>
                            <th className="px-3 py-2">Specification</th>
                            <th className="px-3 py-2 text-right">Quantity</th>
                            <th className="px-3 py-2">Unit</th>
                            <th className="px-3 py-2">Remarks</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-outline-variant/60">
                          {lines.map((line) => (
                            <tr key={line.id}>
                              <td className="px-3 py-2 font-medium text-on-surface">{line.itemName}</td>
                              <td className="px-3 py-2 text-on-surface-variant">{line.specification}</td>
                              <td className="px-3 py-2 text-right font-mono font-semibold">{line.quantity}</td>
                              <td className="px-3 py-2 text-on-surface-variant">{line.unit}</td>
                              <td className="px-3 py-2 text-on-surface-variant">{line.remarks || '-'}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>

                    {po.notes && (
                      <div className="text-xs text-on-surface-variant italic bg-surface p-2.5 rounded">
                        Notes: {po.notes}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Tab 5: Embedded Archi Calc */}
      {activeTab === 'CALC' && (
        <div className="space-y-4">
          <div className="max-w-xl mx-auto">
            <ArchiCalcPad />
          </div>
        </div>
      )}

      {/* Modal: Add Daily Log */}
      {showLogModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-surface-container-lowest rounded-lg max-w-lg w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60">
              <h3 className="text-base font-bold text-on-surface">Add Daily Site Log</h3>
              <button
                type="button"
                onClick={() => setShowLogModal(false)}
                className="text-on-surface-variant/70 hover:text-on-surface-variant text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateLog} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-on-surface mb-1">Weather</label>
                <input
                  type="text"
                  required
                  value={logForm.weather}
                  onChange={(e) => setLogForm({ ...logForm, weather: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="e.g. Sunny, 32°C"
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Work Completed (Detailed)</label>
                <textarea
                  required
                  rows={3}
                  value={logForm.workCompleted}
                  onChange={(e) => setLogForm({ ...logForm, workCompleted: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="Concrete poured for Column C1-C4, brickwork started on 1st floor..."
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Manpower on Site</label>
                <input
                  type="text"
                  value={logForm.manpower}
                  onChange={(e) => setLogForm({ ...logForm, manpower: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="e.g. 1 Mason, 6 Laborers, 2 Bar Benders"
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Materials Received</label>
                <input
                  type="text"
                  value={logForm.materialsReceived}
                  onChange={(e) => setLogForm({ ...logForm, materialsReceived: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="e.g. 50 bags OPC Cement, 1 truck River Sand"
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Next Day Plan</label>
                <input
                  type="text"
                  value={logForm.nextDayPlan}
                  onChange={(e) => setLogForm({ ...logForm, nextDayPlan: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="e.g. Deshuttering and water curing"
                />
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Prepared By</label>
                <input
                  type="text"
                  value={logForm.preparedBy}
                  onChange={(e) => setLogForm({ ...logForm, preparedBy: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="Site Engineer / Architect"
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => setShowLogModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
                >
                  Save Daily Log
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Create Material PO */}
      {showPOModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4 overflow-y-auto">
          <div className="bg-surface-container-lowest rounded-lg max-w-2xl w-full p-6 shadow-xl space-y-4 my-8">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60">
              <div>
                <h3 className="text-base font-bold text-on-surface">New Material Purchase Order</h3>
                <p className="text-xs text-on-surface-variant">Quantity & Specification only</p>
              </div>
              <button
                type="button"
                onClick={() => setShowPOModal(false)}
                className="text-on-surface-variant/70 hover:text-on-surface-variant text-sm font-semibold"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreatePO} className="space-y-4 text-xs">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="block font-medium text-on-surface mb-1">PO Reference Number</label>
                  <input
                    type="text"
                    required
                    value={poForm.referenceNumber}
                    onChange={(e) => setPOForm({ ...poForm, referenceNumber: e.target.value })}
                    className="w-full px-3 py-2 border border-outline rounded-lg font-mono"
                  />
                </div>
                <div>
                  <label className="block font-medium text-on-surface mb-1">Supplier / Vendor</label>
                  <input
                    type="text"
                    required
                    value={poForm.supplier}
                    onChange={(e) => setPOForm({ ...poForm, supplier: e.target.value })}
                    className="w-full px-3 py-2 border border-outline rounded-lg"
                    placeholder="e.g. Apex Building Supplies"
                  />
                </div>
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Order Title</label>
                <input
                  type="text"
                  required
                  value={poForm.title}
                  onChange={(e) => setPOForm({ ...poForm, title: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="e.g. Structural Rebar & Binding Wire Lot 2"
                />
              </div>

              {/* Line Items */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="font-semibold text-on-surface">Items & Specifications</span>
                  <button
                    type="button"
                    onClick={() =>
                      setPOLines([...poLines, { itemName: '', specification: '', quantity: 1, unit: 'Nos', remarks: '' }])
                    }
                    className="text-primary hover:text-primary font-semibold flex items-center gap-1"
                  >
                    <Plus className="w-3.5 h-3.5" /> Add Item Line
                  </button>
                </div>

                <div className="space-y-2">
                  {poLines.map((line, idx) => (
                    <div key={idx} className="p-3 bg-surface rounded-lg border border-outline-variant grid grid-cols-12 gap-2">
                      <div className="col-span-12 sm:col-span-4">
                        <input
                          type="text"
                          required
                          placeholder="Item Name (e.g. TMT Rebar 12mm)"
                          value={line.itemName}
                          onChange={(e) => {
                            const updated = [...poLines];
                            updated[idx].itemName = e.target.value;
                            setPOLines(updated);
                          }}
                          className="w-full px-2.5 py-1.5 bg-surface-container-lowest border border-outline rounded text-xs"
                        />
                      </div>
                      <div className="col-span-12 sm:col-span-4">
                        <input
                          type="text"
                          placeholder="Technical Specification"
                          value={line.specification}
                          onChange={(e) => {
                            const updated = [...poLines];
                            updated[idx].specification = e.target.value;
                            setPOLines(updated);
                          }}
                          className="w-full px-2.5 py-1.5 bg-surface-container-lowest border border-outline rounded text-xs"
                        />
                      </div>
                      <div className="col-span-6 sm:col-span-2">
                        <input
                          type="number"
                          step="0.01"
                          required
                          placeholder="Quantity"
                          value={line.quantity}
                          onChange={(e) => {
                            const updated = [...poLines];
                            updated[idx].quantity = parseFloat(e.target.value) || 0;
                            setPOLines(updated);
                          }}
                          className="w-full px-2.5 py-1.5 bg-surface-container-lowest border border-outline rounded text-xs text-right"
                        />
                      </div>
                      <div className="col-span-4 sm:col-span-1">
                        <input
                          type="text"
                          placeholder="Unit"
                          value={line.unit}
                          onChange={(e) => {
                            const updated = [...poLines];
                            updated[idx].unit = e.target.value;
                            setPOLines(updated);
                          }}
                          className="w-full px-2.5 py-1.5 bg-surface-container-lowest border border-outline rounded text-xs text-center"
                        />
                      </div>
                      <div className="col-span-2 sm:col-span-1 flex items-center justify-center">
                        {poLines.length > 1 && (
                          <button
                            type="button"
                            onClick={() => setPOLines(poLines.filter((_, i) => i !== idx))}
                            className="p-1 text-on-surface-variant/70 hover:text-rose-600"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div>
                <label className="block font-medium text-on-surface mb-1">Delivery / Site Notes</label>
                <textarea
                  rows={2}
                  value={poForm.notes}
                  onChange={(e) => setPOForm({ ...poForm, notes: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="Delivery terms, storage guidelines, batch certificates required..."
                />
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => setShowPOModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
                >
                  Create Purchase Order
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Add Site Doc */}
      {showDocModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-inverse-surface/50 backdrop-blur-xs p-4">
          <div className="bg-surface-container-lowest rounded-lg max-w-md w-full p-6 shadow-xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60">
              <h3 className="text-base font-bold text-on-surface">Add Site Document Pack</h3>
              <button
                type="button"
                onClick={() => setShowDocModal(false)}
                className="text-on-surface-variant/70 hover:text-on-surface-variant text-sm font-semibold"
              >
                ✕
              </button>
            </div>
            <form onSubmit={handleCreateDoc} className="space-y-3 text-xs">
              <div>
                <label className="block font-medium text-on-surface mb-1">Document Title</label>
                <input
                  type="text"
                  required
                  value={docForm.title}
                  onChange={(e) => setDocForm({ ...docForm, title: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="e.g. HFC Pack 02 - Structural First Floor Slab"
                />
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Category</label>
                <select
                  value={docForm.category}
                  onChange={(e) => setDocForm({ ...docForm, category: e.target.value as any })}
                  className="w-full px-3 py-2 border border-outline rounded-lg bg-surface-container-lowest"
                >
                  <option value="HFC">HFC Package</option>
                  <option value="METHOD_STATEMENT">Method Statement</option>
                  <option value="SITE_MEMO">Site Memo</option>
                  <option value="QUALITY">Quality Checklist</option>
                  <option value="SAFETY">Safety Protocol</option>
                </select>
              </div>
              <div>
                <label className="block font-medium text-on-surface mb-1">Notes / Description</label>
                <textarea
                  rows={3}
                  value={docForm.notes}
                  onChange={(e) => setDocForm({ ...docForm, notes: e.target.value })}
                  className="w-full px-3 py-2 border border-outline rounded-lg"
                  placeholder="Details regarding revision signoff, distribution list, or site execution instructions..."
                />
              </div>
              <div className="flex justify-end gap-2 pt-3 border-t border-outline-variant/60">
                <button
                  type="button"
                  onClick={() => setShowDocModal(false)}
                  className="px-4 py-2 border border-outline-variant rounded-lg text-on-surface-variant font-medium hover:bg-surface"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg font-semibold"
                >
                  Save Document
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
