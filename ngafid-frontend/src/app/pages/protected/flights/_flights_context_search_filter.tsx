// ngafid-frontend/src/app/pages/protected/flights/_flights_context_search_filter.tsx

import { TagData } from "@/components/providers/tags/tags_provider";
import { Filter, FilterGroup } from "@/pages/protected/flights/_filters/types";
import { type SortingDirection } from "@/pages/protected/flights/types";
import { createContext, useContext } from "react";

export interface FlightsFilterState {
    filter: Filter;
}

export interface FlightsFilterContextValue extends FlightsFilterState {
    filterSearched: Filter | null;

    setFilter: (updater: (prev: Filter) => Filter) => void;
    setFilterFromJSON: (json: string) => void;
    filterIsEmpty: (filter: Filter) => boolean;
    filterIsValid: (filter: Filter) => boolean;
    revertFilter: () => void;

    copyFilterURL: (filterTarget: Filter) => void;

    addFlightIDToFilter: (flightID: string) => void;
    flightIDInSpecialGroup: (flightID: string) => boolean;

    newID: () => string;
}

export type FlightsSearchFilterState = {
    isFilterSearchLoading: boolean;
    isFilterSearchLoadingManual: boolean;

    sortingColumn?: string | null;
    sortingDirection?: SortingDirection;    
    pageSize?: number;
    currentPage: number;
};

export interface FlightsSearchFilterContextValue extends FlightsSearchFilterState {

    setSortingColumn: (column: string | null) => void;
    setSortingDirection: (direction: SortingDirection) => void;
    setPageSize: (size: number) => void;
    setCurrentPage: (page: number) => void;

    fetchFlightsWithFilter: (filter: FilterGroup, isTriggeredManually: boolean) => Promise<any>;

    updateFlightTags: (flightId: number, tags: TagData[] | null) => void;
}


export const FlightsSearchFilterContext = createContext<FlightsSearchFilterContextValue | null>(null);

export function useFlightsSearchFilter() {

    const context = useContext(FlightsSearchFilterContext);
    if (!context)
        throw new Error("useFlightsSearchFilter must be used within a FlightsSearchFilterContext.Provider");

    return context;

}


export const FlightsFilterContext = createContext<FlightsFilterContextValue | null>(null);

export function useFlightsFilter() {
    
    const context = useContext(FlightsFilterContext);
    if (!context)

        throw new Error("useFlightsFilter must be used within a FlightsFilterContext.Provider");
    return context;

}
