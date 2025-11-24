// ngafid-frontend/src/app/components/modals/charts_list_modal.tsx

"use client";

import { ChartsListModalChecklist } from "@/components/modals/charts_list_modal/charts_list_modal_checklist";
import { ChartsListModalFlights } from "@/components/modals/charts_list_modal/charts_list_modal_flights";
import { useModal } from "@/components/modals/modal_context";
import { ModalData, ModalProps } from "@/components/modals/types";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { Card, CardAction, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Flight } from "@/pages/protected/flights/types";
import { ChartSelectionState } from "@/pages/protected/flights/types_charts";
import { Circle, X } from "lucide-react";
import { motion } from "motion/react";
import { Dispatch, SetStateAction, useEffect, useState } from "react";

const log = getLogger("ChartsListModal", "black", "Modal");


export type ModalDataChartsList = ModalData & {
    chartFlights: Flight[];
    setChartFlights: Dispatch<SetStateAction<Flight[]>>;
    chartSelection: ChartSelectionState;
    toggleUniversalParam: (name: string) => void;
    togglePerFlightParam: (flightId: number, name: string) => void;
};

export function ChartsListModal({ data }: ModalProps<ModalDataChartsList>) {

    const { close } = useModal();
    const { chartFlights, setChartFlights, chartSelection, toggleUniversalParam, togglePerFlightParam } = (data as ModalDataChartsList) ?? {};

    const [localChartFlights, setLocalChartFlights] = useState<Flight[]>(chartFlights);
    const [localSelection, setLocalSelection] = useState<ChartSelectionState>(chartSelection);


    const handleSetChartFlights: Dispatch<SetStateAction<Flight[]>> = (updater) => {

        setLocalChartFlights((prevLocal) => {

            const nextLocal = (typeof updater === "function")
                ? (updater as (prev: Flight[]) => Flight[])(prevLocal)
                : updater;

            // Keep FlightsContext in sync
            setChartFlights(nextLocal);

            return nextLocal;

        });

    };

    const handleToggleUniversalParam = (name: string) => {

        // Global context
        toggleUniversalParam(name);

        // Modal-local selection
        setLocalSelection((prev) => {

            const universal = new Set(prev.universalParams);

            if (universal.has(name))
                universal.delete(name);
            else
                universal.add(name);

            return {
                ...prev,
                universalParams: universal,
            };

        });

    };

    const handleTogglePerFlightParam = (flightId: number, name: string) => {

        // Global context
        togglePerFlightParam(flightId, name);

        // Modal-local selection
        setLocalSelection((prev) => {

            const perFlight = { ...prev.perFlightParams };
            const forFlight = new Set(perFlight[flightId] ?? new Set<string>());

            if (forFlight.has(name))
                forFlight.delete(name);
            else
                forFlight.add(name);

            if (forFlight.size === 0)
                delete perFlight[flightId];
            else
                perFlight[flightId] = forFlight;

            return {
                ...prev,
                perFlightParams: perFlight,
            };

        });

    };
    

    const disableChecklistItems = (localChartFlights.length === 0);

    
    useEffect(() => {
        log("Rendering ChartsListModal with chartFlights:", localChartFlights);
    }, []);

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                <CardHeader className="grid gap-2">

                    <div className="grid gap-2">
                        <CardTitle>Chart Data</CardTitle>
                        <CardDescription>
                            Change what data is shown in the chart.
                        </CardDescription>
                    </div>

                    <CardAction>
                        <Button variant="link" onClick={close}>
                            <X/>
                        </Button>
                    </CardAction>

                </CardHeader>
                <CardContent className="space-y-8 h-152 flex flex-col">

                    {/* Badge Indicator Key */}
                    <div className="flex justify-around items-center w-full border rounded min-h-12 px-4 text-xs">

                        {/* Universal Badge Color */}
                        <div className="flex items-center gap-2">
                            <Circle className="text-foreground" size={8} fill="var(--primary)" />
                            <span>Universal Parameter</span>
                        </div>

                        {/* Flight-Specific Badge Color */}
                        <div className="flex items-center gap-2">
                            <Circle className="text-foreground" size={8}  fill="var(--muted)" />
                            <span>Flight-Specific Parameter</span>
                        </div>

                    </div>

                    <div className="flex gap-2 h-128">

                        {/* Universal Params Checklist */}
                        <ChartsListModalChecklist
                            chartFlights={localChartFlights}
                            disableItems={disableChecklistItems}
                            universalParams={localSelection.universalParams}
                            toggleUniversalParam={handleToggleUniversalParam}
                        />

                        {/* Selected Flights List */}
                        <ChartsListModalFlights
                            chartFlights={localChartFlights}
                            setChartFlights={handleSetChartFlights}
                            chartSelection={localSelection}
                            togglePerFlightParam={handleTogglePerFlightParam}
                            toggleUniversalParam={handleToggleUniversalParam}
                        />

                    </div>

                </CardContent>
            </Card>
        </motion.div>
    );
    
}