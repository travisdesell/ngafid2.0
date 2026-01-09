// ngafid-frontend/src/app/pages/protected/flights/chart_data.ts
import { getLogger } from "@/components/providers/logger";
import { fetchJson } from "@/fetchJson";
import { TraceSeries } from "@/pages/protected/flights/types_charts";
import type { Dispatch, SetStateAction } from "react";
import { EventDefinition, Flight, FlightEvent, PREFERRED_TRACE_NAMES } from "./types";


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



type TraceNames = { commonTraceNames: string[]; uncommonTraceNames: string[] };
type EventInfo = { events: FlightEvent[]; definitions?: EventDefinition[] | null };
type CachedEvents = { events: FlightEvent[]; definitions: EventDefinition[] | null };

// Session caches for trace names and events
const traceNamesCache = new Map<number, TraceNames>();
const eventsCache = new Map<number, CachedEvents>();



async function fetchEventsForFlight(flightId: number): Promise<CachedEvents> {

    log("Fetching events for flight", flightId);

    const res = await fetch(`/api/flight/${flightId}/events`, {
        headers: { Accept: "application/json" },
    });

    // Bad response, throw error
    if (!res.ok) {
        const text = await res.text().catch(() => "");
        throw new Error(`Events request failed (${res.status}): ${text}`);
    }

    const text = await res.text();

    // Attempt to parse incoming text as JSON
    let data: EventInfo;
    try {
        data = JSON.parse(text) as EventInfo;
    } catch {
        throw new Error(`Failed to parse events JSON for flight ${flightId}. First 300 chars: ${text.slice(0, 300)}`);
    }

    // Failed to read any event definitions, throw error
    if (!data.definitions)
        throw new Error(`No event definitions returned for flight ${flightId}`);

    const payload: CachedEvents = {
        events: data.events ?? [],
        definitions: data.definitions,
    };

    log(`Got events response for flight ${flightId}`, payload);
    return payload;

}

export async function addFlightToChart(flight: Flight, setChartFlights: Dispatch<SetStateAction<Flight[]>>): Promise<void> {

    // Optimistically add to selection
    setChartFlights(prev => {

        // Already added, skip
        if (prev.some(f => f.id === flight.id))
            return prev;

        return [
            ...prev,
            {
                ...flight,
                commonTraceNames: flight.commonTraceNames ?? null,
                uncommonTraceNames: flight.uncommonTraceNames ?? null,
                events: flight.events ?? null,
                eventDefinitions: flight.eventDefinitions ?? null,
            },
        ];

    });

    // Apply any cached pieces immediately (no early return!)
    const cachedTrace = traceNamesCache.get(flight.id);
    const cachedEvents = eventsCache.get(flight.id);

    // Got cached trace names, apply them
    if (cachedTrace)
        setChartFlights(prev => prev.map(f => f.id === flight.id ? {
            ...f,
            commonTraceNames: cachedTrace.commonTraceNames,
            uncommonTraceNames: cachedTrace.uncommonTraceNames,
        } : f));

    // Got cached events, apply them
    if (cachedEvents)
        setChartFlights(prev => prev.map(f => f.id === flight.id ? {
            ...f,
            events: cachedEvents.events,
            eventDefinitions: cachedEvents.definitions,
        } : f));

    const haveTrace = (!!cachedTrace) || (flight.commonTraceNames != null && flight.uncommonTraceNames != null);
    const haveEvents = (!!cachedEvents) || (flight.events != null && flight.eventDefinitions != null);

    // Fetch only what's missing
    try {

        const [traceResult, eventsResult] = await Promise.all([

            // Only fetch trace names when they don't already exist
            (haveTrace)
            ?
            Promise.resolve(cachedTrace ?? {
                commonTraceNames: flight.commonTraceNames ?? [],
                uncommonTraceNames: flight.uncommonTraceNames ?? [],
            })
            :
            (async () => {

                const withNames = await ensureFlightTraceNames(flight);
                return {
                    commonTraceNames: withNames.commonTraceNames ?? [],
                    uncommonTraceNames: withNames.uncommonTraceNames ?? [],
                };
                
            })(),

            // Only fetch events when flight reports events exist
            (haveEvents)
            ?
            Promise.resolve(cachedEvents ?? {
                events: flight.events ?? [],
                definitions: flight.eventDefinitions ?? null,
            })
            : (flight.eventCount > 0)
                ? fetchEventsForFlight(flight.id)
                : Promise.resolve({ events: [], definitions: null }),

        ]);

        if (!eventsResult)
            throw new Error(`No events result for flight ${flight.id}`);

        traceNamesCache.set(flight.id, traceResult);
        eventsCache.set(flight.id, eventsResult);

        // Apply both to selected flight
        setChartFlights(prev =>
            prev.map(f => (f.id === flight.id)
                ? {
                    ...f,
                    commonTraceNames: traceResult.commonTraceNames,
                    uncommonTraceNames: traceResult.uncommonTraceNames,
                    events: eventsResult.events,
                    eventDefinitions: eventsResult.definitions,
                }
                : f,
            ),
        );

    } catch (error) {

        log.error(`Error fetching trace names and events for flight ${flight.id}`, error);
        throw error;

    }

};