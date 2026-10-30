export const SiteIssueWorkflow = {
  OPEN: 'OPEN',
  IN_PROGRESS: 'IN_PROGRESS',
  READY_FOR_VERIFICATION: 'READY_FOR_VERIFICATION',
  CLOSED: 'CLOSED',

  allowedNextStatuses(status: string): string[] {
    switch (status) {
      case 'OPEN':
        return ['IN_PROGRESS'];
      case 'IN_PROGRESS':
        return ['READY_FOR_VERIFICATION', 'OPEN'];
      case 'READY_FOR_VERIFICATION':
        return ['CLOSED', 'IN_PROGRESS'];
      default:
        return [];
    }
  },

  canTransition(fromStatus: string, toStatus: string, verificationNote = ''): { valid: boolean; error?: string } {
    const allowed = this.allowedNextStatuses(fromStatus);
    if (!allowed.includes(toStatus)) {
      return { valid: false, error: `Invalid site issue transition ${fromStatus} to ${toStatus}.` };
    }
    if (toStatus === 'CLOSED' && !verificationNote.trim()) {
      return { valid: false, error: 'A verification note is required before closure.' };
    }
    return { valid: true };
  },
};

export const CoordinationWorkflow = {
  RFI: 'RFI',
  SUBMITTAL: 'SUBMITTAL',
  SITE_INSTRUCTION: 'SITE_INSTRUCTION',

  initialStatus(type: string): string {
    switch (type) {
      case 'RFI':
        return 'DRAFT';
      case 'SUBMITTAL':
        return 'RECEIVED';
      case 'SITE_INSTRUCTION':
        return 'DRAFT';
      default:
        return 'DRAFT';
    }
  },

  allowedNextStatuses(type: string, status: string): string[] {
    switch (type) {
      case 'RFI':
        switch (status) {
          case 'DRAFT':
            return ['OPEN'];
          case 'OPEN':
            return ['ANSWERED'];
          case 'ANSWERED':
            return ['CLOSED', 'OPEN'];
          default:
            return [];
        }
      case 'SUBMITTAL':
        switch (status) {
          case 'RECEIVED':
            return ['UNDER_REVIEW'];
          case 'UNDER_REVIEW':
            return ['APPROVED', 'APPROVED_AS_NOTED', 'REVISE_RESUBMIT'];
          case 'REVISE_RESUBMIT':
            return ['RECEIVED'];
          default:
            return [];
        }
      case 'SITE_INSTRUCTION':
        switch (status) {
          case 'DRAFT':
            return ['ISSUED'];
          case 'ISSUED':
            return ['ACKNOWLEDGED'];
          case 'ACKNOWLEDGED':
            return ['COMPLIED'];
          case 'COMPLIED':
            return ['CLOSED'];
          default:
            return [];
        }
      default:
        return [];
    }
  },

  isClosed(status: string): boolean {
    return ['CLOSED', 'APPROVED', 'APPROVED_AS_NOTED'].includes(status);
  },
};
