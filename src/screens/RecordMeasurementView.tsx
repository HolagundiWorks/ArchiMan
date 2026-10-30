import React, { useState, useEffect } from 'react';
import { store } from '../data/storage';
import {
  ProjectEntity,
  CalculationType,
} from '../types';
import { QuantityCalculator } from '../domain/QuantityCalculator';
import { ArchiCalcPad } from '../components/ArchiCalcPad';
import {
  Ruler,
  Check,
  Building,
  User,
  Calculator,
  Layers,
  Sparkles,
  Search,
} from 'lucide-react';

interface RecordMeasurementViewProps {
  project: ProjectEntity;
  onDone: () => void;
}

export const RecordMeasurementView: React.FC<RecordMeasurementViewProps> = ({ project, onDone }) => {
  const contractors = store.contractors.filter((c) => c.projectId === project.id || c.projectId === 0);
  const floors = store.floors.filter((f) => f.projectId === project.id);

  // Form State
  const [selectedContractorId, setSelectedContractorId] = useState<number>(contractors[0]?.id || 0);
  const [selectedFloorId, setSelectedFloorId] = useState<number>(floors[0]?.id || 0);
  const [selectedRoomId, setSelectedRoomId] = useState<number>(0); // 0 means Whole Floor
  const [selectedItemName, setSelectedItemName] = useState<string>('');
  const [calculationType, setCalculationType] = useState<CalculationType>('AREA');
  const [unit, setUnit] = useState<string>('m²');

  // Work item catalogue view mode & search
  const [itemMode, setItemMode] = useState<'QUALIFIED' | 'CATALOG'>('QUALIFIED');
  const [itemSearchQuery, setItemSearchQuery] = useState<string>('');

  // Dimension string fields for clean input experience (no 012.5 leading-zero glitches)
  const [nos, setNos] = useState<string>('1');
  const [length, setLength] = useState<string>('');
  const [breadth, setBreadth] = useState<string>('');
  const [height, setHeight] = useState<string>('');
  const [deduction, setDeduction] = useState<string>('');

  // Metadata
  const [description, setDescription] = useState<string>('');
  const [location, setLocation] = useState<string>('');
  const [remarks, setRemarks] = useState<string>('');

  const [showCalcInline, setShowCalcInline] = useState(false);
  const [activeCalcTarget, setActiveCalcTarget] = useState<'length' | 'breadth' | 'height' | null>(null);

  // Qualified items for selected contractor
  const qualifiedItems = store.contractorQualifiedItems.filter((q) => q.contractorId === selectedContractorId);
  const allMasterItems = store.standardWorkItems;

  const currentRooms = store.rooms.filter((r) => r.projectId === project.id && r.floorId === selectedFloorId);

  // Sync selected work item when contractor changes
  useEffect(() => {
    if (qualifiedItems.length > 0) {
      setSelectedItemName(qualifiedItems[0].itemName);
      setCalculationType(qualifiedItems[0].calculationType);
      setUnit(qualifiedItems[0].uom);
    } else if (allMasterItems.length > 0) {
      setSelectedItemName(allMasterItems[0].name);
      setCalculationType(allMasterItems[0].calculationType);
      setUnit(allMasterItems[0].unit);
    }
  }, [selectedContractorId]);

  const numNos = parseFloat(nos) || 0;
  const numLength = parseFloat(length) || 0;
  const numBreadth = parseFloat(breadth) || 0;
  const numHeight = parseFloat(height) || 0;
  const numDeduction = parseFloat(deduction) || 0;

  // Compute live quantity
  const computedQuantity = QuantityCalculator.calculate(calculationType, {
    no: numNos,
    length: numLength,
    breadth: numBreadth,
    height: numHeight,
    deduction: numDeduction,
  });

  const handleSelectItem = (name: string) => {
    setSelectedItemName(name);
    const qItem = qualifiedItems.find((q) => q.itemName === name);
    if (qItem) {
      setCalculationType(qItem.calculationType);
      setUnit(qItem.uom);
      return;
    }
    const mItem = allMasterItems.find((m) => m.name === name);
    if (mItem) {
      setCalculationType(mItem.calculationType);
      setUnit(mItem.unit);
    }
  };

  const handleInsertFromCalc = (val: number) => {
    const formatted = val.toFixed(3).replace(/\.?0+$/, '');
    if (activeCalcTarget === 'length') setLength(formatted);
    else if (activeCalcTarget === 'breadth') setBreadth(formatted);
    else if (activeCalcTarget === 'height') setHeight(formatted);
    setShowCalcInline(false);
    setActiveCalcTarget(null);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedContractorId || !selectedItemName || computedQuantity <= 0) return;

    const contractor = store.contractors.find((c) => c.id === selectedContractorId);
    const floor = store.floors.find((f) => f.id === selectedFloorId);
    const room = store.rooms.find((r) => r.id === selectedRoomId);

    // Find or create Measurement Sheet for this contractor + work item + floor
    let sheet = store.measurementSheets.find(
      (s) =>
        s.projectId === project.id &&
        s.contractorId === selectedContractorId &&
        s.itemNameSnapshot === selectedItemName &&
        s.floorId === selectedFloorId &&
        s.status !== 'APPROVED'
    );

    if (!sheet) {
      const sheetSeq = store.measurementSheets.filter((s) => s.projectId === project.id).length + 1;
      const code = `MS-${project.projectCode}-${sheetSeq.toString().padStart(3, '0')}`;
      sheet = store.addMeasurementSheet({
        sheetCode: code,
        projectId: project.id,
        floorId: selectedFloorId || null,
        floorNameSnapshot: floor?.name || 'Ground Floor',
        contractorId: selectedContractorId,
        contractorNameSnapshot: contractor?.name || 'Contractor',
        itemId: 0,
        itemNameSnapshot: selectedItemName,
        uomSnapshot: unit,
        formulaCode: calculationType,
        formulaVersion: 1,
        status: 'DRAFT',
        revision: 1,
        lockedAt: null,
        archivedAt: null,
      });
    }

    // Save individual measurement row
    store.addMeasurement({
      sheetId: sheet.id,
      projectId: project.id,
      floorId: selectedFloorId || null,
      roomId: selectedRoomId || null,
      componentId: null,
      componentWorkItemId: null,
      contractorId: selectedContractorId,
      contractorName: contractor?.name || 'Contractor',
      itemId: 0,
      itemName: selectedItemName,
      unit,
      calculationType,
      formulaCode: calculationType,
      formulaVersion: 1,
      description: description || `${selectedItemName} entry`,
      length: numLength,
      width: numBreadth,
      height: numHeight,
      nos: numNos,
      deduction: numDeduction,
      quantity: computedQuantity,
      floor: floor?.name || 'Ground Floor',
      location: location || (room ? room.name : 'General'),
      remarks,
      photoUri: null,
      date: Date.now(),
    });

    onDone();
  };

  // Filter items based on mode and search
  const visibleItems =
    itemMode === 'QUALIFIED' && qualifiedItems.length > 0
      ? qualifiedItems
          .filter((q) => q.itemName.toLowerCase().includes(itemSearchQuery.toLowerCase()))
          .map((q) => ({ name: q.itemName, calculationType: q.calculationType, unit: q.uom }))
      : allMasterItems
          .filter(
            (m) =>
              m.name.toLowerCase().includes(itemSearchQuery.toLowerCase()) ||
              m.itemCode.toLowerCase().includes(itemSearchQuery.toLowerCase()) ||
              m.trade.toLowerCase().includes(itemSearchQuery.toLowerCase())
          )
          .map((m) => ({ name: m.name, calculationType: m.calculationType, unit: m.unit, trade: m.trade }));

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="bg-surface-container-lowest p-5 sm:p-6 rounded-lg border border-outline-variant elevation-1">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-outline-variant/60 mb-6 gap-2">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-primary-container text-primary rounded-md">
              <Ruler className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-on-surface">Record Site Measurement</h2>
              <p className="text-xs text-on-surface-variant">
                Contractor-first entry workflow with standard PWD dimension formulas.
              </p>
            </div>
          </div>
          <div className="text-xs font-mono font-semibold text-on-surface-variant self-start sm:self-auto">
            {project.projectCode} · {project.name}
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Step 1: Contractor & Location */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1.5 flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-primary" /> 1. Contractor (First)
              </label>
              <select
                value={selectedContractorId}
                onChange={(e) => setSelectedContractorId(parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-surface border border-outline rounded-lg text-xs font-medium text-on-surface focus:bg-surface-container-lowest focus:outline-none focus:ring-1 focus:ring-primary"
              >
                {contractors.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.contractorType})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1.5 flex items-center gap-1">
                <Building className="w-3.5 h-3.5 text-primary" /> 2. Floor Level
              </label>
              <select
                value={selectedFloorId}
                onChange={(e) => setSelectedFloorId(parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-surface border border-outline rounded-lg text-xs font-medium text-on-surface focus:bg-surface-container-lowest focus:outline-none focus:ring-1 focus:ring-primary"
              >
                {floors.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1.5 flex items-center gap-1">
                <Layers className="w-3.5 h-3.5 text-primary" /> 3. Room / Area
              </label>
              <select
                value={selectedRoomId}
                onChange={(e) => setSelectedRoomId(parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-surface border border-outline rounded-lg text-xs font-medium text-on-surface focus:bg-surface-container-lowest focus:outline-none focus:ring-1 focus:ring-primary"
              >
                <option value={0}>Whole Floor Level (Common)</option>
                {currentRooms.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Step 2: Work Item Selection with Catalogue Search & Mode Toggle */}
          <div className="space-y-2">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <label className="block text-xs font-semibold text-on-surface">
                4. Select Work Item:
              </label>
              <div className="flex items-center gap-2">
                <div className="flex items-center gap-1 p-0.5 bg-surface-container-low rounded-lg">
                  <button
                    type="button"
                    onClick={() => {
                      setItemMode('QUALIFIED');
                      setItemSearchQuery('');
                    }}
                    className={`px-2.5 py-1 text-xs font-medium rounded-md transition ${
                      itemMode === 'QUALIFIED'
                        ? 'bg-surface-container-lowest text-on-surface elevation-1'
                        : 'text-on-surface-variant hover:text-on-surface'
                    }`}
                  >
                    Contractor Qualified ({qualifiedItems.length})
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setItemMode('CATALOG');
                      setItemSearchQuery('');
                    }}
                    className={`px-2.5 py-1 text-xs font-medium rounded-md transition ${
                      itemMode === 'CATALOG'
                        ? 'bg-surface-container-lowest text-on-surface elevation-1'
                        : 'text-on-surface-variant hover:text-on-surface'
                    }`}
                  >
                    Full PWD Catalogue ({allMasterItems.length})
                  </button>
                </div>
              </div>
            </div>

            {/* Quick search input */}
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-on-surface-variant/70 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Search work item, specification, trade..."
                value={itemSearchQuery}
                onChange={(e) => setItemSearchQuery(e.target.value)}
                className="w-full pl-8 pr-3 py-1.5 bg-surface border border-outline-variant rounded-lg text-xs focus:bg-surface-container-lowest focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-2 max-h-48 overflow-y-auto p-1">
              {visibleItems.length === 0 ? (
                <div className="col-span-full text-center py-6 text-xs text-on-surface-variant/70">
                  No matching work items found. Switch to Full PWD Catalogue or refine search.
                </div>
              ) : (
                visibleItems.map((item, idx) => {
                  const isSelected = selectedItemName === item.name;
                  return (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => handleSelectItem(item.name)}
                      className={`p-3 rounded-md border text-left transition flex flex-col justify-between ${
                        isSelected
                          ? 'border-primary bg-primary-container/70 ring-1 ring-primary'
                          : 'border-outline-variant bg-surface-container-lowest hover:border-outline'
                      }`}
                    >
                      <div className="font-semibold text-xs text-on-surface leading-snug">{item.name}</div>
                      <div className="flex items-center justify-between text-[11px] text-on-surface-variant mt-2 font-mono">
                        <span className="text-on-surface-variant">{item.calculationType}</span>
                        <span className="font-semibold text-on-surface">{item.unit}</span>
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          {/* Step 3: Dimension Inputs & Live Formula Calculator */}
          <div className="bg-surface p-4 sm:p-5 rounded-md border border-outline-variant space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div className="flex items-center gap-2">
                <Calculator className="w-4 h-4 text-primary" />
                <span className="text-xs font-bold text-on-surface tracking-wide">
                  Dimension Inputs ({calculationType})
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowCalcInline(!showCalcInline)}
                className="text-xs text-primary hover:text-primary font-semibold flex items-center gap-1 self-start sm:self-auto"
              >
                <Sparkles className="w-3.5 h-3.5" />
                {showCalcInline ? 'Hide Calculator' : 'Use Archi Calc to compute dimension'}
              </button>
            </div>

            {/* Inline Archi Calc Pad */}
            {showCalcInline && (
              <div className="p-4 bg-surface-container-lowest rounded-md border border-primary-container elevation-1">
                <div className="text-xs text-on-surface-variant mb-2 font-medium">
                  Select which input to insert calculated value into:
                  <div className="flex gap-2 mt-1.5">
                    {(['length', 'breadth', 'height'] as const).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setActiveCalcTarget(t)}
                        className={`px-3 py-1 rounded text-xs uppercase font-bold transition ${
                          activeCalcTarget === t
                            ? 'bg-primary text-on-primary'
                            : 'bg-surface-container-low text-on-surface-variant hover:bg-surface-container-high'
                        }`}
                      >
                        Target: {t}
                      </button>
                    ))}
                  </div>
                </div>
                <ArchiCalcPad onInsertValue={(val) => handleInsertFromCalc(val)} inline />
              </div>
            )}

            <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-on-surface-variant mb-1">Nos (Multiplier)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  required
                  value={nos}
                  placeholder="1"
                  onChange={(e) => setNos(e.target.value)}
                  className="w-full px-3 py-2 bg-surface-container-lowest border border-outline rounded-lg text-xs font-mono font-semibold focus:outline-none focus:ring-1 focus:ring-primary"
                />
              </div>

              {['RUNNING_LENGTH', 'AREA', 'WALL_PLASTER', 'VOLUME'].includes(calculationType) && (
                <div>
                  <label className="block text-[11px] font-semibold text-on-surface-variant mb-1">Length (m)</label>
                  <input
                    type="number"
                    step="0.001"
                    min="0"
                    required
                    value={length}
                    placeholder="0.000"
                    onChange={(e) => setLength(e.target.value)}
                    className="w-full px-3 py-2 bg-surface-container-lowest border border-outline rounded-lg text-xs font-mono font-semibold focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              )}

              {['AREA', 'VOLUME'].includes(calculationType) && (
                <div>
                  <label className="block text-[11px] font-semibold text-on-surface-variant mb-1">Breadth / Width (m)</label>
                  <input
                    type="number"
                    step="0.001"
                    min="0"
                    required
                    value={breadth}
                    placeholder="0.000"
                    onChange={(e) => setBreadth(e.target.value)}
                    className="w-full px-3 py-2 bg-surface-container-lowest border border-outline rounded-lg text-xs font-mono font-semibold focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              )}

              {['WALL_PLASTER', 'VOLUME'].includes(calculationType) && (
                <div>
                  <label className="block text-[11px] font-semibold text-on-surface-variant mb-1">Height / Depth (m)</label>
                  <input
                    type="number"
                    step="0.001"
                    min="0"
                    required
                    value={height}
                    placeholder="0.000"
                    onChange={(e) => setHeight(e.target.value)}
                    className="w-full px-3 py-2 bg-surface-container-lowest border border-outline rounded-lg text-xs font-mono font-semibold focus:outline-none focus:ring-1 focus:ring-primary"
                  />
                </div>
              )}

              {calculationType === 'WALL_PLASTER' && (
                <div>
                  <label className="block text-[11px] font-semibold text-on-surface-variant mb-1">Deduction (m²)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={deduction}
                    placeholder="0.00"
                    onChange={(e) => setDeduction(e.target.value)}
                    className="w-full px-3 py-2 bg-surface-container-lowest border border-outline rounded-lg text-xs font-mono font-semibold text-rose-600 focus:outline-none focus:ring-1 focus:ring-rose-500"
                  />
                </div>
              )}
            </div>

            {/* Live Result Banner */}
            <div className="bg-surface-container-lowest p-3.5 rounded-lg border border-outline-variant flex flex-col sm:flex-row sm:items-center justify-between gap-2">
              <div>
                <span className="text-xs text-on-surface-variant font-medium">Computed Executed Quantity:</span>
                <div className="text-2xl font-mono font-extrabold text-primary tabular-nums">
                  {computedQuantity.toFixed(2)} <span className="text-sm font-semibold">{unit}</span>
                </div>
              </div>
              <div className="text-left sm:text-right text-[11px] text-on-surface-variant font-mono">
                {calculationType === 'VOLUME' && 'Formula: Nos × L × B × H'}
                {calculationType === 'AREA' && 'Formula: Nos × L × B'}
                {calculationType === 'WALL_PLASTER' && 'Formula: (Nos × L × H) - Deduction'}
                {calculationType === 'RUNNING_LENGTH' && 'Formula: Nos × L'}
                {calculationType === 'NOS' && 'Formula: Nos (Count)'}
              </div>
            </div>
          </div>

          {/* Step 4: Descriptive Details */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1">Item Description / Specifics</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="e.g. Beam B-02 bottom and sides shuttering"
                className="w-full px-3 py-2 border border-outline rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-on-surface mb-1">Location Details / Grid Ref</label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g. Grid 2-3 / North Balcony"
                className="w-full px-3 py-2 border border-outline rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-on-surface mb-1">Site Remarks / Quality Notes</label>
            <input
              type="text"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              placeholder="e.g. Verified with joint measurement tape; deshuttered after 7 days"
              className="w-full px-3 py-2 border border-outline rounded-lg text-xs focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-outline-variant">
            <button
              type="button"
              onClick={onDone}
              className="px-4 py-2 border border-outline rounded-lg text-xs font-semibold text-on-surface-variant hover:bg-surface transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={computedQuantity <= 0}
              className="px-5 py-2 bg-primary hover:bg-primary text-on-primary rounded-lg text-xs font-bold elevation-1 flex items-center gap-2 disabled:opacity-50 transition"
            >
              <Check className="w-4 h-4" /> Save to Measurement Book
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
