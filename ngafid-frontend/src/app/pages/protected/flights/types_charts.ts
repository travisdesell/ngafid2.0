// ngafid-frontend/src/app/pages/protected/flights/types_charts.ts

export type TraceNameList = {
    commonTraceNames: string[];
    uncommonTraceNames: string[];
};

// Key to identify a series uniquely.
export type SeriesKey = `${number}:${string}`; //<-- `${flightId}:${traceName}`

export interface TraceSeries {
    flightId: number;
    name: string;
    timestamps: number[];
    values: number[];
}

/*
    For flight selection:
        - universalParams: parameters enabled for all flights.
        - perFlightParams[flightId]: extra parameters only for that flight.
*/
export interface ChartSelectionState {
    universalParams: Set<string>;
    perFlightParams: Record<number, Set<string>>;
}

/*
    For the actual data cache:
*/
export interface ChartDataState {
    seriesByFlight: Record<number, Record<string, TraceSeries>>;
    loadingSeries: Set<SeriesKey>;
}