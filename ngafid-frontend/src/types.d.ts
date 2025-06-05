// src/types/globals.d.ts
export {};



export enum accessType {
    DENIED="DENIED",
    WAITING="WAITING",
    VIEW="VIEW",
    UPLOAD="UPLOAD",
    MANAGER="MANAGER",
}


export interface NGAFIDUser {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    fleetAccess: accessType;
    isAdmin: boolean;
    isFleetManager: boolean;
}

//TODO: Figure out the best way to reconcile this with eslint globals
declare global {
    const waitingUserCount: number;
    const fleetManager: boolean;
    const unconfirmedTailsCount: number;
    const modifyTailsAccess: boolean;
    const plotMapHidden: boolean;
}