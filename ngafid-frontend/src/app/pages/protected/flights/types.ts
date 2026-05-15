// ngafid-frontend/src/app/pages/protected/flights/types.ts

import { TagData } from "@/components/providers/tags/tags_provider";
import { AirframeNameID } from "src/types/types";

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

export interface FlightEvent {
    endLine: number;
    endTime: string;
    eventDefinitionId: number;
    fleetId: number;
    flightdId: number;
    id: number;
    otherFlightId: number | null;
    severity: number;
    startLine: number;
    startTime: string;
}

export interface EventDefinition {
    id: number;
    fleetId: number;
    name: string;
    startBuffer: number;
    stopBuffer: number;

    airframeNameId: number;

    columnNames: Array<string>;
    severityColumnNames: Array<string>;
    severityType: string;
    filter: unknown;    // ⚠️ TODO: Define EventDefinition filter type
}

export interface Flight {
    filename: string;
    systemId: string;
    md5Hash: string;
    startDateTime: string;
    endDateTime: string;
    itinerary: Array<ItineraryEntry>;
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
    events: Array<FlightEvent> | null;
    eventCount: number;
    eventDefinitions: Array<EventDefinition> | null;
    tags: Array<TagData> | null;

    commonTraceNames: Array<string> | null;
    uncommonTraceNames: Array<string> | null;
}


/* --- Double Time Series --- */

export interface DoubleTimeSeries {
    [traceName: string]: Array<number>;
}
export const PREFERRED_TRACE_NAMES = ["AltAGL", "AltMSL", "E1 MAP", "E2 MAP", "E1 RPM", "E2 RPM", "IAS", "NormAc", "Pitch", "Roll", "VSpd", "LOC-I Index", "Stall Index"];