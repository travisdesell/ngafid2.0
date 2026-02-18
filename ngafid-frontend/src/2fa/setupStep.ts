// ngafid-frontend/src/2fa/setupStep.ts
export const setupStep = {
    INITIAL: 'initial',
    BACKUP: 'backup',
    QR: 'qr',
    COMPLETE: 'complete',
    DISABLE: 'disable',
};

export type SetupStep = typeof setupStep[keyof typeof setupStep];