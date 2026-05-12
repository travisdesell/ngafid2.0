// src/types.d.ts

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

export type FleetAccess = {
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
    fleetAccess: FleetAccess[];
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

export type MultifleetInvite = {
    fleetName: string,
    inviteEmail: string,
    fleetId?: number,
}

export type MultifleetSelectWithAccess = {
    fleetName: string,
    fleetId: number,
    accessType: AccessType,
}


//TODO: Figure out the best way to reconcile this with eslint globals
export interface AirframeNameID {
    name: string;
    id: number;
}

export type AirframeEventCounts = {
    airframeName: string;
    names: string[];
    flightsWithEventCounts: number[];
    totalFlightsCounts: number[];
    totalEventsCounts: number[];
    aggregateFlightsWithEventCounts: number[];
    aggregateTotalFlightsCounts: number[];
    aggregateTotalEventsCounts: number[];
}

declare global {
    const waitingUserCount: number;
    const fleetManager: boolean;
    const unconfirmedTailsCount: number;
    const modifyTailsAccess: boolean;
    const plotMapHidden: boolean;
    const airframes: AirframeNameID[];
    const tagNames: string[];
    const eventNames: string[];

    interface Window {
        __APP_CONFIG__?: {
            azureMapsKey?: string;
        }
    }

}
