// ngafid-frontend/src/app/pages/protected/flights/types.ts

import { TagData } from "@/components/providers/tags/tags_provider";
import { AirframeNameID } from "src/types";

export type SortingDirection = "Ascending" | "Descending";
export const isValidSortingDirection = (value: string): value is SortingDirection => {
    return (value === "Ascending" || value === "Descending");
};

export const FLIGHTS_PER_PAGE_OPTIONS = [
    10,
    25,
    50,
    100,
];
export const FILTER_RULE_NAME_NEW = "New Rule";


export interface ItineraryEntry {
    airport: string;
    endOfApproach: number;
    endOfTakeoff: number;
    finalIndex: number;
    minAirportDistance: number;
    minAltitude: number;
    minAltitudeIndex: number;
    minRunwayDistance: number;
    order: number;
    runway: string;
    runwayCounts: unknown; // ⚠️ TODO: Define type
    startOfApproach: number;
    startOfTakeoff: number;
    takeoffCounter: number;
    type: string;   // ⚠️ TODO: Define valid type names
}

export type AirframeNameIDType = { // ⚠️ TODO: Figure out what to do with this
    type: {
        id: number;
        name: string;
    }
} & AirframeNameID;


export interface Flight {
    filename: string;
    systemId: string;
    md5Hash: string;
    startDateTime: string;
    endDateTime: string;
    itinerary: ItineraryEntry[];
    id: number;
    fleetId: number;
    uploaderId: number;
    uploadId: number;
    tailNumber: string;
    airframe: AirframeNameIDType;
    status: string; // ⚠️ TODO: Define valid status names
    numberRows: number;
    doubleTimeSeries: DoubleTimeSeries;  // ⚠️ TODO: Define valid double time series types
    stringTimeSeries: object;  // ⚠️ TODO: Define valid string time series types
    events: any[];  // ⚠️ TODO: Define valid event types
    tags: TagData[] | null;

    commonTraceNames: string[] | null;
    uncommonTraceNames: string[] | null;
}


/* --- Double Time Series --- */

export type DoubleTimeSeries = {
    [traceName: string]: number[];
};
export const PREFERRED_TRACE_NAMES = ["AltAGL", "AltMSL", "E1 MAP", "E2 MAP", "E1 RPM", "E2 RPM", "IAS", "NormAc", "Pitch", "Roll", "VSpd", "LOC-I Index", "Stall Index"];