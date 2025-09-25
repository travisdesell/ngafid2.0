// src/types.d.ts



import React from "react";



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

    id: React.Key;
    flightId: string | number;
    otherFlightId: string | number;
    startTime: string | number | Date;
    endTime: string | number | Date;
    severity: number;

}

export type AirframeEventCounts = {
    airframeName: string;
    names: Array<string>;   //<-- Event names
    totalEventsCounts: Array<number>;  //<-- Total counts for each event name
};

export type ErrorResponse = {
    errorTitle: string;
    errorMessage: string;
}

export interface AirframeNameID {
    name: string;
    id: number;
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
        reactRoot: React.ReactRoot;
    }
}