import React, { useState } from 'react';
import { ArchiCalcEngine, ArchiCalcMeasure } from '../domain/ArchiCalcEngine';
import { Calculator, Delete, ArrowRight, RotateCcw } from 'lucide-react';

interface ArchiCalcPadProps {
  onInsertValue?: (val: number, unit?: string) => void;
  inline?: boolean;
}

export const ArchiCalcPad: React.FC<ArchiCalcPadProps> = ({ onInsertValue, inline = false }) => {
  const [expression, setExpression] = useState('');
  const [result, setResult] = useState('0.00');

  const handleChar = (char: string) => {
    const next = expression + char;
    setExpression(next);
    const evalRes = ArchiCalcEngine.evaluate(next);
    if (evalRes !== 'Error') setResult(evalRes);
  };

  const handleBackspace = () => {
    const next = expression.slice(0, -1);
    setExpression(next);
    if (!next) {
      setResult('0.00');
    } else {
      const evalRes = ArchiCalcEngine.evaluate(next);
      if (evalRes !== 'Error') setResult(evalRes);
    }
  };

  const handleClear = () => {
    setExpression('');
    setResult('0.00');
  };

  const handleFunction = (fn: string) => {
    const current = result !== 'Error' && result !== '0.00' ? result : expression || '0';
    const res = ArchiCalcEngine.applyFn(current, fn);
    setResult(res);
    setExpression(res);
  };

  const handleEquals = () => {
    const evalRes = ArchiCalcEngine.evaluate(expression);
    setResult(evalRes);
    setExpression(evalRes);
  };

  const measureGrid = ArchiCalcMeasure.formatGrid(expression || result || '0');

  return (
    <div className={`bg-white rounded-xl shadow-sm border border-slate-200 p-4 ${inline ? 'w-full' : 'max-w-xl mx-auto'}`}>
      <div className="flex items-center justify-between pb-3 border-b border-slate-100 mb-3">
        <div className="flex items-center gap-2 text-indigo-700 font-semibold">
          <Calculator className="w-5 h-5" />
          <span>Archi Calc</span>
          <span className="text-xs px-2 py-0.5 bg-indigo-50 text-indigo-600 rounded-full font-normal">
            AcharyaOS Engine
          </span>
        </div>
        <div className="text-xs text-slate-500">Embedded Quantity & Dimensions</div>
      </div>

      {/* Display Screen */}
      <div className="bg-slate-900 rounded-lg p-3 text-right font-mono mb-4 text-white">
        <div className="text-sm text-slate-400 min-h-[1.25rem] truncate overflow-hidden">
          {expression || '0'}
        </div>
        <div className="text-3xl font-bold tracking-wider text-emerald-400 mt-1">
          {result}
        </div>
      </div>

      {/* 6-Way Real-time Length / Area / Volume Grid */}
      <div className="bg-slate-50 p-2.5 rounded-lg border border-slate-200 mb-4">
        <div className="text-xs font-semibold text-slate-600 mb-1.5 flex items-center justify-between">
          <span>Dimensional Conversions (Metric / Imperial):</span>
          {onInsertValue && (
            <span className="text-[11px] text-indigo-600 font-normal">
              Click unit chip to apply to measurement
            </span>
          )}
        </div>
        <div className="grid grid-cols-3 gap-2 text-center text-xs">
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.m), 'm')}
            className="p-1.5 bg-white rounded border border-slate-200 hover:border-indigo-400 transition"
          >
            <div className="text-slate-400 text-[10px]">Length (m)</div>
            <div className="font-semibold text-slate-800">{measureGrid.m} m</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.m2), 'm²')}
            className="p-1.5 bg-white rounded border border-slate-200 hover:border-indigo-400 transition"
          >
            <div className="text-slate-400 text-[10px]">Area (m²)</div>
            <div className="font-semibold text-slate-800">{measureGrid.m2} m²</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.m3), 'm³')}
            className="p-1.5 bg-white rounded border border-slate-200 hover:border-indigo-400 transition"
          >
            <div className="text-slate-400 text-[10px]">Volume (m³)</div>
            <div className="font-semibold text-slate-800">{measureGrid.m3} m³</div>
          </button>

          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.ft), 'ft')}
            className="p-1.5 bg-white rounded border border-slate-200 hover:border-indigo-400 transition"
          >
            <div className="text-slate-400 text-[10px]">Length (ft)</div>
            <div className="font-semibold text-slate-800">{measureGrid.ft} ft</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.ft2), 'ft²')}
            className="p-1.5 bg-white rounded border border-slate-200 hover:border-indigo-400 transition"
          >
            <div className="text-slate-400 text-[10px]">Area (ft²)</div>
            <div className="font-semibold text-slate-800">{measureGrid.ft2} ft²</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.ft3), 'ft³')}
            className="p-1.5 bg-white rounded border border-slate-200 hover:border-indigo-400 transition"
          >
            <div className="text-slate-400 text-[10px]">Volume (ft³)</div>
            <div className="font-semibold text-slate-800">{measureGrid.ft3} ft³</div>
          </button>
        </div>
      </div>

      {/* Keypad */}
      <div className="grid grid-cols-5 gap-1.5 text-sm">
        {/* Row 1: Trig / Advanced */}
        <button
          type="button"
          onClick={() => handleFunction('sin')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          sin
        </button>
        <button
          type="button"
          onClick={() => handleFunction('cos')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          cos
        </button>
        <button
          type="button"
          onClick={() => handleFunction('tan')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          tan
        </button>
        <button
          type="button"
          onClick={() => handleFunction('sqrt')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          √x
        </button>
        <button
          type="button"
          onClick={() => handleFunction('x2')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          x²
        </button>

        {/* Row 2: Secondary / Clear */}
        <button
          type="button"
          onClick={handleClear}
          className="p-2 bg-rose-50 text-rose-600 hover:bg-rose-100 rounded font-bold"
        >
          AC
        </button>
        <button
          type="button"
          onClick={handleBackspace}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700 flex items-center justify-center"
        >
          <Delete className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => handleChar('(')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          (
        </button>
        <button
          type="button"
          onClick={() => handleChar(')')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          )
        </button>
        <button
          type="button"
          onClick={() => handleChar('/')}
          className="p-2 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded font-bold"
        >
          ÷
        </button>

        {/* Row 3: 7, 8, 9, *, 1/x */}
        <button
          type="button"
          onClick={() => handleChar('7')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          7
        </button>
        <button
          type="button"
          onClick={() => handleChar('8')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          8
        </button>
        <button
          type="button"
          onClick={() => handleChar('9')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          9
        </button>
        <button
          type="button"
          onClick={() => handleChar('*')}
          className="p-2 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded font-bold"
        >
          ×
        </button>
        <button
          type="button"
          onClick={() => handleFunction('1/x')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700 text-xs"
        >
          1/x
        </button>

        {/* Row 4: 4, 5, 6, -, +/- */}
        <button
          type="button"
          onClick={() => handleChar('4')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          4
        </button>
        <button
          type="button"
          onClick={() => handleChar('5')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          5
        </button>
        <button
          type="button"
          onClick={() => handleChar('6')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          6
        </button>
        <button
          type="button"
          onClick={() => handleChar('-')}
          className="p-2 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded font-bold"
        >
          −
        </button>
        <button
          type="button"
          onClick={() => handleFunction('+/-')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700 text-xs"
        >
          ±
        </button>

        {/* Row 5: 1, 2, 3, +, ^ */}
        <button
          type="button"
          onClick={() => handleChar('1')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          1
        </button>
        <button
          type="button"
          onClick={() => handleChar('2')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          2
        </button>
        <button
          type="button"
          onClick={() => handleChar('3')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          3
        </button>
        <button
          type="button"
          onClick={() => handleChar('+')}
          className="p-2 bg-indigo-50 text-indigo-700 hover:bg-indigo-100 rounded font-bold"
        >
          +
        </button>
        <button
          type="button"
          onClick={() => handleChar('^')}
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-medium text-slate-700"
        >
          xʸ
        </button>

        {/* Row 6: 0, ., ', ", = */}
        <button
          type="button"
          onClick={() => handleChar('0')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          0
        </button>
        <button
          type="button"
          onClick={() => handleChar('.')}
          className="p-2.5 bg-white border border-slate-200 hover:bg-slate-50 rounded font-semibold text-slate-800"
        >
          .
        </button>
        <button
          type="button"
          onClick={() => handleChar("'")}
          title="Feet"
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-bold text-slate-700"
        >
          '
        </button>
        <button
          type="button"
          onClick={() => handleChar('"')}
          title="Inches"
          className="p-2 bg-slate-100 hover:bg-slate-200 rounded font-bold text-slate-700"
        >
          "
        </button>
        <button
          type="button"
          onClick={handleEquals}
          className="p-2 bg-indigo-600 text-white hover:bg-indigo-700 rounded font-bold shadow-sm"
        >
          =
        </button>
      </div>
    </div>
  );
};
