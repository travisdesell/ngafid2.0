// ngafid-frontend/src/app/pages/protected/flights/_flights_context.tsx

import { TagData } from "@/components/providers/tags/tags_provider";
import { Filter, FilterGroup } from "@/pages/protected/flights/_filters/types";
import type { Flight, SortingDirection } from "@/pages/protected/flights/types";
import { ChartDataState, ChartSelectionState, TraceSeries } from "@/pages/protected/flights/types_charts";
import { createContext, Dispatch, SetStateAction, useContext } from "react";



export type FlightsResponse = {
    flights: Flight[];
    totalFlights: number;
    numberPages: number;
}

export type FlightsState = {
    flights: Flight[];
    totalFlights: number;
    numberPages: number;

    filter: Filter;

    isFilterSearchLoading: boolean;
    isFilterSearchLoadingManual: boolean;

    filterSearched: Filter | null;
    sortingColumn?: string | null;
    sortingDirection?: SortingDirection;    
    pageSize?: number;
    currentPage: number;

    // Chart Data
    chartFlights: Flight[];

    chartSelection: ChartSelectionState;
    chartData: ChartDataState;

};

export type EnsureSeriesFn = (flightId: number, paramName: string) => Promise<TraceSeries>;

export interface FlightsContextValue extends FlightsState {
    setFilter: (updater: (prev: Filter) => Filter) => void;
    setFilterFromJSON: (json: string) => void;
    filterIsEmpty: (filter: Filter) => boolean;
    filterIsValid: (filter: Filter) => boolean;
    revertFilter: () => void;

    copyFilterURL: (filterTarget: Filter) => void;

    addFlightIDToFilter: (flightID: string) => void;
    flightIDInSpecialGroup: (flightID: string) => boolean;

    newID: () => string;

    setSortingColumn: (column: string | null) => void;
    setSortingDirection: (direction: SortingDirection) => void;
    setPageSize: (size: number) => void;
    setCurrentPage: (page: number) => void;

    fetchFlightsWithFilter: (filter: FilterGroup, isTriggeredManually: boolean) => Promise<any>;

    // Flight Tags
    updateFlightTags: (flightId: number, tags: TagData[] | null) => void;

    // Chart Data
    setChartFlights: Dispatch<SetStateAction<Flight[]>>;
    toggleUniversalParam: (name: string) => void;
    togglePerFlightParam: (flightId: number, name: string) => void;
    ensureSeries: EnsureSeriesFn;
};


export const FlightsContext = createContext<FlightsContextValue | null>(null);

export function useFlights() {

    const context = useContext(FlightsContext);
    if (!context)
        throw new Error("useFlights must be used within a FlightsContext.Provider");

    return context;

}