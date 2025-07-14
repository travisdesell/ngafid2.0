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


export interface Event {

    id: React.Key | null | undefined;
    flightId: string | number | bigint | boolean | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | React.ReactPortal | Promise<string | number | bigint | boolean | React.ReactPortal | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | null | undefined> | null | undefined;
    otherFlightId: string | number | bigint | boolean | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | React.ReactPortal | Promise<string | number | bigint | boolean | React.ReactPortal | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | null | undefined> | null | undefined;
    startTime: string | number | Date;
    endTime: string | number | Date;
    severity: number;

}


//TODO: Figure out the best way to reconcile this with eslint globals
declare global {
    const waitingUserCount: number;
    const fleetManager: boolean;
    const unconfirmedTailsCount: number;
    const modifyTailsAccess: boolean;
    const plotMapHidden: boolean;
}