import React, { useState, useEffect } from 'react';
import { store } from '../data/storage';
import {
  ProjectEntity,
  ContractorEntity,
  FloorEntity,
  RoomEntity,
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
  ArrowRight,
  Sparkles,
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

  // Dimension fields
  const [nos, setNos] = useState<number>(1);
  const [length, setLength] = useState<number>(0);
  const [breadth, setBreadth] = useState<number>(0);
  const [height, setHeight] = useState<number>(0);
  const [deduction, setDeduction] = useState<number>(0);

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

  // Compute live quantity
  const computedQuantity = QuantityCalculator.calculate(calculationType, {
    no: nos,
    length,
    breadth,
    height,
    deduction,
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
    if (activeCalcTarget === 'length') setLength(val);
    else if (activeCalcTarget === 'breadth') setBreadth(val);
    else if (activeCalcTarget === 'height') setHeight(val);
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
      length,
      width: breadth,
      height,
      nos,
      deduction,
      quantity: computedQuantity,
      floor: floor?.name || 'Ground Floor',
      location: location || (room ? room.name : 'General'),
      remarks,
      photoUri: null,
      date: Date.now(),
    });

    onDone();
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 mb-6">
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-50 text-indigo-600 rounded-xl">
              <Ruler className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-slate-900">Record Site Measurement</h2>
              <p className="text-xs text-slate-500">
                Follows contractor-first entry contract with automated quantity formula calculation.
              </p>
            </div>
          </div>
          <div className="text-right">
            <span className="text-xs px-2.5 py-1 bg-slate-100 text-slate-700 rounded-full font-mono font-semibold">
              {project.projectCode}
            </span>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Step 1: Contractor & Location */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                <User className="w-3.5 h-3.5 text-indigo-600" /> 1. Contractor (First)
              </label>
              <select
                value={selectedContractorId}
                onChange={(e) => setSelectedContractorId(parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-medium text-slate-800 focus:bg-white"
              >
                {contractors.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.contractorType})
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                <Building className="w-3.5 h-3.5 text-indigo-600" /> 2. Floor Level
              </label>
              <select
                value={selectedFloorId}
                onChange={(e) => setSelectedFloorId(parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-medium text-slate-800 focus:bg-white"
              >
                {floors.map((f) => (
                  <option key={f.id} value={f.id}>
                    {f.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1.5 flex items-center gap-1">
                <Layers className="w-3.5 h-3.5 text-indigo-600" /> 3. Room / Area
              </label>
              <select
                value={selectedRoomId}
                onChange={(e) => setSelectedRoomId(parseInt(e.target.value) || 0)}
                className="w-full px-3 py-2 bg-slate-50 border border-slate-300 rounded-lg text-xs font-medium text-slate-800 focus:bg-white"
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

          {/* Step 2: Work Item Selection */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-2">
              4. Work Item (from Contractor Trade Qualifications):
            </label>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
              {(qualifiedItems.length > 0 ? qualifiedItems : allMasterItems.slice(0, 6)).map((item, idx) => {
                const isSelected = selectedItemName === item.itemName || selectedItemName === (item as any).name;
                const name = item.itemName || (item as any).name;
                const formula = item.calculationType;
                const uom = (item as any).uom || (item as any).unit;

                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => handleSelectItem(name)}
                    className={`p-3 rounded-xl border text-left transition flex flex-col justify-between ${
                      isSelected
                        ? 'border-indigo-600 bg-indigo-50/70 ring-1 ring-indigo-600'
                        : 'border-slate-200 bg-white hover:border-slate-300'
                    }`}
                  >
                    <div className="font-semibold text-xs text-slate-900 leading-snug">{name}</div>
                    <div className="flex items-center justify-between text-[10px] text-slate-500 mt-2">
                      <span className="font-mono bg-white px-1.5 py-0.5 rounded border border-slate-200">
                        {formula}
                      </span>
                      <span className="font-bold text-slate-700">{uom}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Step 3: Dimension Inputs & Live Formula Calculator */}
          <div className="bg-slate-50 p-5 rounded-xl border border-slate-200 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Calculator className="w-4 h-4 text-indigo-600" />
                <span className="text-xs font-bold text-slate-800 uppercase tracking-wider">
                  Dimension Inputs ({calculationType})
                </span>
              </div>
              <button
                type="button"
                onClick={() => setShowCalcInline(!showCalcInline)}
                className="text-xs text-indigo-600 hover:text-indigo-800 font-semibold flex items-center gap-1"
              >
                <Sparkles className="w-3.5 h-3.5" />
                {showCalcInline ? 'Hide Calculator' : 'Use Archi Calc to compute dimension'}
              </button>
            </div>

            {/* Inline Archi Calc Pad */}
            {showCalcInline && (
              <div className="p-4 bg-white rounded-xl border border-indigo-200 shadow-sm">
                <div className="text-xs text-slate-600 mb-2 font-medium">
                  Select which input to apply computed result into:
                  <div className="flex gap-2 mt-1">
                    {(['length', 'breadth', 'height'] as const).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setActiveCalcTarget(t)}
                        className={`px-2.5 py-1 rounded text-xs uppercase font-bold ${
                          activeCalcTarget === t
                            ? 'bg-indigo-600 text-white'
                            : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
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
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">Nos (Multiplier)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  required
                  value={nos}
                  onChange={(e) => setNos(parseFloat(e.target.value) || 0)}
                  className="w-full px-3 py-2 bg-white border border-slate-300 rounded-lg text-xs font-mono font-semibold"
                />
              </div>

              {['RUNNING_LENGTH', 'AREA', 'WALL_PLASTER', 'VOLUME'].includes(calculationType) && (
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Length (m)</label>
                  <input
                    type="number"
                    step="0.001"
                    min="0"
                    required
                    value={length}
                    onChange={(e) => setLength(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-white border border-slate-300 rounded-lg text-xs font-mono font-semibold"
                  />
                </div>
              )}

              {['AREA', 'VOLUME'].includes(calculationType) && (
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Breadth / Width (m)</label>
                  <input
                    type="number"
                    step="0.001"
                    min="0"
                    required
                    value={breadth}
                    onChange={(e) => setBreadth(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-white border border-slate-300 rounded-lg text-xs font-mono font-semibold"
                  />
                </div>
              )}

              {['WALL_PLASTER', 'VOLUME'].includes(calculationType) && (
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Height / Depth (m)</label>
                  <input
                    type="number"
                    step="0.001"
                    min="0"
                    required
                    value={height}
                    onChange={(e) => setHeight(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-white border border-slate-300 rounded-lg text-xs font-mono font-semibold"
                  />
                </div>
              )}

              {calculationType === 'WALL_PLASTER' && (
                <div>
                  <label className="block text-[11px] font-semibold text-slate-600 mb-1">Deduction (m²)</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={deduction}
                    onChange={(e) => setDeduction(parseFloat(e.target.value) || 0)}
                    className="w-full px-3 py-2 bg-white border border-slate-300 rounded-lg text-xs font-mono font-semibold text-rose-600"
                  />
                </div>
              )}
            </div>

            {/* Live Result Banner */}
            <div className="bg-white p-3.5 rounded-lg border border-indigo-100 flex items-center justify-between">
              <div>
                <span className="text-xs text-slate-500 font-medium">Computed Executed Quantity:</span>
                <div className="text-2xl font-mono font-extrabold text-indigo-700">
                  {computedQuantity.toFixed(2)} <span className="text-sm font-semibold">{unit}</span>
                </div>
              </div>
              <div className="text-right text-[11px] text-slate-500 max-w-xs">
                Formula: {calculationType === 'VOLUME' ? 'Nos × L × B × H' : calculationType === 'AREA' ? 'Nos × L × B' : calculationType === 'WALL_PLASTER' ? '(Nos × L × H) - Ded.' : calculationType === 'RUNNING_LENGTH' ? 'Nos × L' : 'Nos (Count)'}
              </div>
            </div>
          </div>

          {/* Step 4: Descriptive Details */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Item Description / Specifics</label>
              <input
                type="text"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="e.g. Beam B-02 bottom and sides shuttering"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Location Details / Grid Ref</label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g. Grid 2-3 / North Balcony"
                className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">Site Remarks / Quality Notes</label>
            <input
              type="text"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
              placeholder="e.g. Verified with joint measurement tape; deshutered after 7 days"
              className="w-full px-3 py-2 border border-slate-300 rounded-lg text-xs"
            />
          </div>

          {/* Form Actions */}
          <div className="flex items-center justify-end gap-3 pt-4 border-t border-slate-200">
            <button
              type="button"
              onClick={onDone}
              className="px-4 py-2 border border-slate-300 rounded-lg text-xs font-semibold text-slate-600 hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={computedQuantity <= 0}
              className="px-6 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-bold shadow-sm flex items-center gap-2 disabled:opacity-50"
            >
              <Check className="w-4 h-4" /> Save to Measurement Book
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
