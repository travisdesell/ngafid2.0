// ngafid-frontend/src/app/pages/protected/flights/_flights_context_chart.tsx

import type { Flight } from "@/pages/protected/flights/types";
import { ChartDataState, ChartSelectionState, TraceSeries } from "@/pages/protected/flights/types_charts";
import { EventSelectionState } from "@/pages/protected/flights/types_events";
import { createContext, Dispatch, SetStateAction, useContext } from "react";


export type FlightsChartState = {
    chartFlights: Flight[];
    chartSelection: ChartSelectionState;
    chartData: ChartDataState;

    eventSelection: EventSelectionState;

    labelingEnabledFlightIds: number[];
    selectedLabelingParamByFlight: Record<number, string>;
    pendingLabelStartXByFlight: Record<number, number | null>;
    labelSectionsByFlight: Record<number, FlightLabelSection[]>;

    selectedIds: Set<number>;
}

export type FlightLabelSection = {
    id: number | null;
    startIndex: number;
    endIndex: number;
    startTime: number;
    endTime: number;
    startValue: number;
    endValue: number;
    labelText: string;
    parameterNames: string[];
    visibleOnChart?: boolean;
};

export type EnsureSeriesFn = (flightId: number, paramName: string) => Promise<TraceSeries>;

export interface FlightsChartContextValue extends FlightsChartState {
    setChartFlights: Dispatch<SetStateAction<Flight[]>>;
    toggleUniversalParam: (name: string) => void;
    togglePerFlightParam: (flightId: number, name: string) => void;
    toggleUniversalEvent: (name: string) => void;
    togglePerFlightEvent: (flightId: number, name: string) => void;
    setLabelingEnabledForFlight: (flightId: number, enabled: boolean) => void;
    setSelectedLabelingParam: (flightId: number, paramName: string) => void;
    setPendingLabelStartX: (flightId: number, x: number | null) => void;
    setFlightLabelSections: (flightId: number, sections: FlightLabelSection[]) => void;
    ensureSeries: EnsureSeriesFn;
}

export const FlightsChartContext = createContext<FlightsChartContextValue | null>(null);

export function useFlightsChart() {

    const context = useContext(FlightsChartContext);
    if (!context)
        throw new Error("useFlightsChart must be used within a FlightsChartContext.Provider");

    return context;

}