// ngafid-frontend/src/app/pages/protected/flights/chart_data.ts
import { getLogger } from "@/components/providers/logger";
import { fetchJson } from "@/fetchJson";
import { TraceSeries } from "@/pages/protected/flights/types_charts";
import type { Dispatch, SetStateAction } from "react";
import { Flight, PREFERRED_TRACE_NAMES } from "./types";


const log = getLogger("ChartData", "black", "Utility");


export async function ensureFlightTraceNames(flight: Flight): Promise<Flight> {

    // Flight already has trace names defined, exit
    if (flight.commonTraceNames && flight.uncommonTraceNames)
        return flight;

    log("Fetching trace names for flight", flight.id);

    const { names } = await fetchJson.get<{ names: string[] }>(
        `/api/flight/${flight.id}/double-series`
    );

    const common: string[] = [];
    const uncommon: string[] = [];

    for (const name of names) {

        if (PREFERRED_TRACE_NAMES.includes(name))
            common.push(name);
        else
            uncommon.push(name);
        
    }

    log("Fetched trace names for flight", flight.id, "common:", common, "uncommon:", uncommon);

    return {
        ...flight,
        commonTraceNames: common,
        uncommonTraceNames: uncommon,
    };
    
}

export async function fetchSeries(flightId: number, traceName: string): Promise<TraceSeries> {

    const encodedName = encodeURIComponent(traceName);

    const response = await fetchJson.get<{ x: string[]; y: number[] }>(
        `/api/flight/${flightId}/double-series/${encodedName}`
    );

    const timestamps = response.x.map((s) => Number(s));

    return {
        flightId,
        name: traceName,
        timestamps,
        values: response.y,
    };

}

/**
 * Helper to add a flight to the chart list to ensure
 * that commonTraceNames and uncommonTraceNames are
 * populated.
*/
export async function addFlightToChart(flight: Flight, setChartFlights: Dispatch<SetStateAction<Flight[]>>): Promise<void> {

    const withNames = await ensureFlightTraceNames(flight);

    setChartFlights((prev: Flight[]) => {

        // Already present -> no-op
        if (prev.some((f: Flight) => f.id === withNames.id))
            return prev;

        return [...prev, withNames];

    });
}
