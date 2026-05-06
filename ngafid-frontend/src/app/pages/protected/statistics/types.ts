// ngafid-frontend/src/app/pages/protected/statistics/types.ts

export const EVENT_STAT_MALFORMED = null; 

export type MonthStatsRowRaw = {
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
};

export type EventStatsRaw = {
    eventName?: unknown;
    totalFlights?: unknown;
    processedFlights?: unknown;
    humanReadable?: unknown;
    eventId?: unknown;
    monthStats?: unknown;
};

export type AirframeStatsRaw = {
    airframeNameId?: unknown;
    airframeName?: unknown;
    events?: unknown;
};

export type MonthStatsRow = {
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
};

export type EventStats = {
    eventName: string;
    totalFlights: number;
    processedFlights: number;
    humanReadable: string;
    eventId: number | null;
    monthStats: MonthStatsRow[];
};

export type AirframeStats = {
    airframeNameId: number;
    airframeName: string;
    events: EventStats[];
};

export type AirframeCardData = {
    id: number;
    name: string;
};