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
    <div className={`bg-surface-container-lowest rounded-md elevation-1 border border-outline-variant p-4 ${inline ? 'w-full' : 'max-w-xl mx-auto'}`}>
      <div className="flex items-center justify-between pb-3 border-b border-outline-variant/60 mb-3">
        <div className="flex items-center gap-2 text-primary font-semibold">
          <Calculator className="w-5 h-5" />
          <span>Archi Calc</span>
          <span className="text-xs px-2 py-0.5 bg-primary-container text-on-primary-container rounded-sm font-semibold">
            AcharyaOS Engine
          </span>
        </div>
        <div className="text-xs text-on-surface-variant">Embedded Quantity & Dimensions</div>
      </div>

      {/* Display Screen */}
      <div className="bg-inverse-surface rounded-md p-3 text-right font-mono mb-4 text-inverse-on-surface">
        <div className="text-sm text-inverse-on-surface/60 min-h-[1.25rem] truncate overflow-hidden">
          {expression || '0'}
        </div>
        <div className="text-3xl font-bold tracking-wider text-inverse-primary mt-1">
          {result}
        </div>
      </div>

      {/* 6-Way Real-time Length / Area / Volume Grid */}
      <div className="bg-surface p-2.5 rounded-lg border border-outline-variant mb-4">
        <div className="text-xs font-semibold text-on-surface-variant mb-1.5 flex items-center justify-between">
          <span>Dimensional Conversions (Metric / Imperial):</span>
          {onInsertValue && (
            <span className="text-[11px] text-primary font-normal">
              Click unit chip to apply to measurement
            </span>
          )}
        </div>
        <div className="grid grid-cols-3 gap-2 text-center text-xs">
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.m), 'm')}
            className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant hover:border-primary transition"
          >
            <div className="text-on-surface-variant/70 text-[10px]">Length (m)</div>
            <div className="font-semibold text-on-surface">{measureGrid.m} m</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.m2), 'm²')}
            className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant hover:border-primary transition"
          >
            <div className="text-on-surface-variant/70 text-[10px]">Area (m²)</div>
            <div className="font-semibold text-on-surface">{measureGrid.m2} m²</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.m3), 'm³')}
            className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant hover:border-primary transition"
          >
            <div className="text-on-surface-variant/70 text-[10px]">Volume (m³)</div>
            <div className="font-semibold text-on-surface">{measureGrid.m3} m³</div>
          </button>

          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.ft), 'ft')}
            className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant hover:border-primary transition"
          >
            <div className="text-on-surface-variant/70 text-[10px]">Length (ft)</div>
            <div className="font-semibold text-on-surface">{measureGrid.ft} ft</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.ft2), 'ft²')}
            className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant hover:border-primary transition"
          >
            <div className="text-on-surface-variant/70 text-[10px]">Area (ft²)</div>
            <div className="font-semibold text-on-surface">{measureGrid.ft2} ft²</div>
          </button>
          <button
            type="button"
            onClick={() => onInsertValue && onInsertValue(parseFloat(measureGrid.ft3), 'ft³')}
            className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant hover:border-primary transition"
          >
            <div className="text-on-surface-variant/70 text-[10px]">Volume (ft³)</div>
            <div className="font-semibold text-on-surface">{measureGrid.ft3} ft³</div>
          </button>
        </div>
      </div>

      {/* Keypad */}
      <div className="grid grid-cols-5 gap-1.5 text-sm">
        {/* Row 1: Trig / Advanced */}
        <button
          type="button"
          onClick={() => handleFunction('sin')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
        >
          sin
        </button>
        <button
          type="button"
          onClick={() => handleFunction('cos')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
        >
          cos
        </button>
        <button
          type="button"
          onClick={() => handleFunction('tan')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
        >
          tan
        </button>
        <button
          type="button"
          onClick={() => handleFunction('sqrt')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
        >
          √x
        </button>
        <button
          type="button"
          onClick={() => handleFunction('x2')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
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
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface flex items-center justify-center"
        >
          <Delete className="w-4 h-4" />
        </button>
        <button
          type="button"
          onClick={() => handleChar('(')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
        >
          (
        </button>
        <button
          type="button"
          onClick={() => handleChar(')')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
        >
          )
        </button>
        <button
          type="button"
          onClick={() => handleChar('/')}
          className="p-2 bg-primary-container text-primary hover:bg-primary-container rounded font-bold"
        >
          ÷
        </button>

        {/* Row 3: 7, 8, 9, *, 1/x */}
        <button
          type="button"
          onClick={() => handleChar('7')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          7
        </button>
        <button
          type="button"
          onClick={() => handleChar('8')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          8
        </button>
        <button
          type="button"
          onClick={() => handleChar('9')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          9
        </button>
        <button
          type="button"
          onClick={() => handleChar('*')}
          className="p-2 bg-primary-container text-primary hover:bg-primary-container rounded font-bold"
        >
          ×
        </button>
        <button
          type="button"
          onClick={() => handleFunction('1/x')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface text-xs"
        >
          1/x
        </button>

        {/* Row 4: 4, 5, 6, -, +/- */}
        <button
          type="button"
          onClick={() => handleChar('4')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          4
        </button>
        <button
          type="button"
          onClick={() => handleChar('5')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          5
        </button>
        <button
          type="button"
          onClick={() => handleChar('6')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          6
        </button>
        <button
          type="button"
          onClick={() => handleChar('-')}
          className="p-2 bg-primary-container text-primary hover:bg-primary-container rounded font-bold"
        >
          −
        </button>
        <button
          type="button"
          onClick={() => handleFunction('+/-')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface text-xs"
        >
          ±
        </button>

        {/* Row 5: 1, 2, 3, +, ^ */}
        <button
          type="button"
          onClick={() => handleChar('1')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          1
        </button>
        <button
          type="button"
          onClick={() => handleChar('2')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          2
        </button>
        <button
          type="button"
          onClick={() => handleChar('3')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          3
        </button>
        <button
          type="button"
          onClick={() => handleChar('+')}
          className="p-2 bg-primary-container text-primary hover:bg-primary-container rounded font-bold"
        >
          +
        </button>
        <button
          type="button"
          onClick={() => handleChar('^')}
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-medium text-on-surface"
        >
          xʸ
        </button>

        {/* Row 6: 0, ., ', ", = */}
        <button
          type="button"
          onClick={() => handleChar('0')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          0
        </button>
        <button
          type="button"
          onClick={() => handleChar('.')}
          className="p-2.5 bg-surface-container-lowest border border-outline-variant hover:bg-surface rounded font-semibold text-on-surface"
        >
          .
        </button>
        <button
          type="button"
          onClick={() => handleChar("'")}
          title="Feet"
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-bold text-on-surface"
        >
          '
        </button>
        <button
          type="button"
          onClick={() => handleChar('"')}
          title="Inches"
          className="p-2 bg-surface-container-low hover:bg-surface-container-high rounded font-bold text-on-surface"
        >
          "
        </button>
        <button
          type="button"
          onClick={handleEquals}
          className="p-2 bg-primary text-on-primary hover:bg-primary rounded font-bold elevation-1"
        >
          =
        </button>
      </div>
    </div>
  );
};
