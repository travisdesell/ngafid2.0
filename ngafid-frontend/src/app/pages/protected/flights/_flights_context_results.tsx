// ngafid-frontend/src/app/pages/protected/flights/_flights_context_results.tsx

import { type Flight } from "@/pages/protected/flights/types";
import { createContext, useContext } from "react";


export interface FlightsResponse {
    flights: Array<Flight>;
    totalFlights: number;
    numberPages: number;
}

export interface FlightsResultsState {
    flights: Array<Flight>;
    totalFlights: number;
    numberPages: number;
}

export interface FlightsResultsContextValue extends FlightsResultsState {
    /* ... */
}

export const FlightsResultsContext = createContext<FlightsResultsContextValue | null>(null);

export function useFlightsResults() {

    const context = useContext(FlightsResultsContext);
    if (!context)
        throw new Error("useFlightsResults must be used within a FlightsResultsContext.Provider");

    return context;

}