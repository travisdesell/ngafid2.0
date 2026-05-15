// ngafid-frontend/src/app/pages/protected/statistics/types.ts

export const EVENT_STAT_MALFORMED = null; 

export interface MonthStatsRowRaw {
    rowName?: unknown;
    flightsWithoutError?: unknown;
    flightsWithEvent?: unknown;
    totalEvents?: unknown;
    minSeverity?: unknown;
    avgSeverity?: unknown;
    maxSeverity?: unknown;
    minDuration?: unknown;
    avgDuration?: unknown;
    maxDuration?: unknown;
    aggFlightsWithoutError?: unknown;
    aggFlightsWithEvent?: unknown;
    aggTotalEvents?: unknown;
    aggMinSeverity?: unknown;
    aggAvgSeverity?: unknown;
    aggMaxSeverity?: unknown;
    aggMinDuration?: unknown;
    aggAvgDuration?: unknown;
    aggMaxDuration?: unknown;
}

export interface EventStatsRaw {
    eventName?: unknown;
    totalFlights?: unknown;
    processedFlights?: unknown;
    humanReadable?: unknown;
    eventId?: unknown;
    monthStats?: unknown;
}

export interface AirframeStatsRaw {
    airframeNameId?: unknown;
    airframeName?: unknown;
    events?: unknown;
}

export interface MonthStatsRow {
    rowName: string;
    flightsWithoutError: number;
    flightsWithEvent: number;
    totalEvents: number;
    minSeverity: number;
    avgSeverity: number;
    maxSeverity: number;
    minDuration: number;
    avgDuration: number;
    maxDuration: number;
    aggFlightsWithoutError: number;
    aggFlightsWithEvent: number;
    aggTotalEvents: number;
    aggMinSeverity: number;
    aggAvgSeverity: number;
    aggMaxSeverity: number;
    aggMinDuration: number;
    aggAvgDuration: number;
    aggMaxDuration: number;
}

export interface EventStats {
    eventName: string;
    totalFlights: number;
    processedFlights: number;
    humanReadable: string;
    eventId: number | null;
    monthStats: Array<MonthStatsRow>;
}

export interface AirframeStats {
    airframeNameId: number;
    airframeName: string;
    events: Array<EventStats>;
}

export interface AirframeCardData {
    id: number;
    name: string;
}