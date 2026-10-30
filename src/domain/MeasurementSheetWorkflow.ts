import { MeasurementSheetStatus } from '../types';

export const MeasurementSheetWorkflow = {
  allowedTransitions(from: MeasurementSheetStatus): MeasurementSheetStatus[] {
    switch (from) {
      case 'DRAFT':
        return ['SUBMITTED'];
      case 'RETURNED':
        return ['SUBMITTED'];
      case 'SUBMITTED':
        return ['CHECKED', 'RETURNED'];
      case 'CHECKED':
        return ['APPROVED', 'RETURNED'];
      case 'APPROVED':
        return [];
      default:
        return [];
    }
  },

  canTransition(from: MeasurementSheetStatus, to: MeasurementSheetStatus, comment = ''): boolean {
    const allowed = this.allowedTransitions(from);
    if (!allowed.includes(to)) return false;
    if (to === 'RETURNED' && !comment.trim()) return false;
    return true;
  },
};
