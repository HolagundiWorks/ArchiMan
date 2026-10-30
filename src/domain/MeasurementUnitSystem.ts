import { CalculationType } from '../types';

export type MeasurementUnitSystem = 'METRIC' | 'IMPERIAL';

const FEET_PER_METRE = 3.280839895013123;

export const MeasurementUnitConverter = {
  infer(unit: string): MeasurementUnitSystem {
    const normalized = unit
      .toLowerCase()
      .replace(/²/g, '2')
      .replace(/³/g, '3')
      .replace(/\s+/g, '');
    if (
      normalized.includes('ft') ||
      normalized.includes('feet') ||
      normalized.includes('foot') ||
      normalized.includes('sqft') ||
      normalized.includes('cuft')
    ) {
      return 'IMPERIAL';
    }
    return 'METRIC';
  },

  powerFor(type: CalculationType): number {
    switch (type) {
      case 'NOS':
        return 0;
      case 'RUNNING_LENGTH':
        return 1;
      case 'AREA':
      case 'WALL_PLASTER':
        return 2;
      case 'VOLUME':
        return 3;
      default:
        return 1;
    }
  },

  convert(value: number, power: number, from: MeasurementUnitSystem, to: MeasurementUnitSystem): number {
    if (from === to || power === 0) return value;
    const factor = Math.pow(FEET_PER_METRE, power);
    return from === 'METRIC' ? value * factor : value / factor;
  },

  linear(value: number, from: MeasurementUnitSystem, to: MeasurementUnitSystem): number {
    return this.convert(value, 1, from, to);
  },

  quantity(value: number, type: CalculationType, from: MeasurementUnitSystem, to: MeasurementUnitSystem): number {
    return this.convert(value, this.powerFor(type), from, to);
  },

  unitFor(type: CalculationType, system: MeasurementUnitSystem, countUnit = 'Nos'): string {
    switch (type) {
      case 'NOS':
        return countUnit;
      case 'RUNNING_LENGTH':
        return system === 'METRIC' ? 'm' : 'ft';
      case 'AREA':
      case 'WALL_PLASTER':
        return system === 'METRIC' ? 'm²' : 'ft²';
      case 'VOLUME':
        return system === 'METRIC' ? 'm³' : 'ft³';
      default:
        return countUnit;
    }
  },
};
