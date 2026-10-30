import { CalculationType } from '../types';

export interface MeasurementInput {
  no?: number;
  length?: number;
  breadth?: number;
  height?: number;
  deduction?: number;
}

export const QuantityCalculator = {
  calculate(type: CalculationType, input: MeasurementInput): number {
    const no = Math.max(0, input.no ?? 1);
    const length = Math.max(0, input.length ?? 0);
    const breadth = Math.max(0, input.breadth ?? 0);
    const height = Math.max(0, input.height ?? 0);
    const deduction = Math.max(0, input.deduction ?? 0);

    let raw = 0;
    switch (type) {
      case 'NOS':
        raw = no;
        break;
      case 'RUNNING_LENGTH':
        raw = no * length;
        break;
      case 'AREA':
        raw = no * length * breadth;
        break;
      case 'WALL_PLASTER':
        raw = no * length * height - deduction;
        break;
      case 'VOLUME':
        raw = no * length * breadth * height;
        break;
      default:
        raw = no * length * breadth;
    }

    return Math.round(Math.max(0, raw) * 100) / 100;
  },
};
