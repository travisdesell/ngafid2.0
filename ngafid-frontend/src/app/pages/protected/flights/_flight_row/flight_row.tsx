import { Card } from "@/components/ui/card";
import { AirframeNameID } from "src/types";

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
    airframe: AirframeNameIDType;
    tailNumber: string;
    status: string; // ⚠️ TODO: Define valid status names
    numberRows: number;
    doubleTimeSeries: object;  // ⚠️ TODO: Define valid double time series types
    stringTimeSeries: object;  // ⚠️ TODO: Define valid string time series types
    events: any[];  // ⚠️ TODO: Define valid event types
}

export default function FlightRow(flight: Flight) {

    return <Card>
        Flight Row!
    </Card>;

}