// src/types/types.d.ts

import type React from "react";

export {};

export type AccessType =
  "DENIED"
  | "WAITING"
  | "VIEW"
  | "UPLOAD"
  | "MANAGER";


export interface Fleet {
    id: number;
    name: string;
}

export interface FleetAccess {
    fleetName: string;
    userId: number;
    fleetId: number;
    accessType: AccessType;
}


export interface NGAFIDUser {
    id: number;
    email: string;
    firstName: string;
    lastName: string;
    fleet: Fleet | null;
    fleetAccess: Array<FleetAccess>;
    isAdmin: boolean;
    isFleetManager: boolean;
}


export interface Event {

    id: React.Key;
    flightId: string | number;
    otherFlightId: string | number;
    startTime: string | number | Date;
    endTime: string | number | Date;
    severity: number;

}

export interface MultifleetInvite {
    fleetName: string,
    inviteEmail: string,
    fleetId?: number,
}

export interface MultifleetSelectWithAccess {
    fleetName: string,
    fleetId: number,
    accessType: AccessType,
}


//TODO: Figure out the best way to reconcile this with eslint globals
export interface AirframeNameID {
    name: string;
    id: number;
}

export interface AirframeEventCounts {
    airframeName: string;
    names: Array<string>;
    flightsWithEventCounts: Array<number>;
    totalFlightsCounts: Array<number>;
    totalEventsCounts: Array<number>;
    aggregateFlightsWithEventCounts: Array<number>;
    aggregateTotalFlightsCounts: Array<number>;
    aggregateTotalEventsCounts: Array<number>;
}

declare global {
    const waitingUserCount: number;
    const fleetManager: boolean;
    const unconfirmedTailsCount: number;
    const modifyTailsAccess: boolean;
    const plotMapHidden: boolean;
    const airframes: Array<AirframeNameID>;
    const tagNames: Array<string>;
    const eventNames: Array<string>;

    interface Window {
        __APP_CONFIG__?: {
            azureMapsKey?: string;
        }
    }

}
