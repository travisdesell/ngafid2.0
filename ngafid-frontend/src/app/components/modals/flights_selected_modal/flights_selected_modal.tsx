// ngafid-frontend/src/app/components/modals/flights_selected_modal.tsx

"use client";

import { FlightsSelectedModalChecklist } from "@/components/modals/flights_selected_modal/flights_selected_modal_checklist";
import { FlightsSelectedModalFlights } from "@/components/modals/flights_selected_modal/flights_selected_modal_flights";
import { useModal } from "@/components/modals/modal_context";
import { ModalData, ModalProps } from "@/components/modals/types";
import { getLogger } from "@/components/providers/logger";
import { Card, CardContent } from "@/components/ui/card";
import { Flight } from "@/pages/protected/flights/types";
import { EventSelectionState } from "@/pages/protected/flights/types_events";
import { Circle } from "lucide-react";
import { motion } from "motion/react";
import { Dispatch, SetStateAction, useState, useSyncExternalStore } from "react";

const log = getLogger("FlightsSelectedModal", "black", "Modal");


type FlightsModalChartStoreSnapshot = {
    chartFlights: Flight[];
    eventSelection: EventSelectionState;
};

type FlightsModalChartStore = {
    subscribe: (listener: () => void) => () => void;
    getSnapshot: () => FlightsModalChartStoreSnapshot;
};

export type ModalDataFlightsSelected = ModalData & {
    chartStore: FlightsModalChartStore;
    setChartFlights: Dispatch<SetStateAction<Flight[]>>;
    toggleUniversalEvent: (name: string) => void;
    togglePerFlightEvent: (flightId: number, name: string) => void;
};

export function FlightsSelectedModal({ data }: ModalProps<ModalDataFlightsSelected>) {

    const { renderModalHeader } = useModal();
    // const { eventSelection, toggleUniversalEvent, togglePerFlightEvent } = useFlightsChart();

    const { chartStore, setChartFlights, toggleUniversalEvent, togglePerFlightEvent } = (data as ModalDataFlightsSelected) ?? {};
    const { chartFlights, eventSelection } = useSyncExternalStore(
        chartStore.subscribe,
        chartStore.getSnapshot,
        chartStore.getSnapshot,
    );
    
    const [localChartFlights, setLocalChartFlights] = useState<Flight[]>(chartFlights);


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

    // Log Selected Flights
    log.table("Selected Flights in Modal:", localChartFlights);

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                
                {/* Modal Header */}
                {renderModalHeader("Selected Flights", "View selected flights and change displayed events.", true)}

                <CardContent className="space-y-8 h-152 flex flex-col">

                    {/* Badge Indicator Key */}
                    <div className="flex justify-around items-center w-full border rounded min-h-12 px-4 text-xs">

                        {/* Universal Badge Color */}
                        <div className="flex items-center gap-2">
                            <Circle className="text-foreground" size={8} fill="var(--primary)" />
                            <span>Universal Event</span>
                        </div>

                        {/* Universal (Missing) Badge Color */}
                        <div className="flex items-center gap-2">
                            <Circle className="text-foreground" size={8} fill="var(--warning)" />
                            <span>Universal Event Missing</span>
                        </div>

                        {/* Flight-Specific Badge Color */}
                        <div className="flex items-center gap-2">
                            <Circle className="text-foreground" size={8}  fill="var(--muted)" />
                            <span>Flight-Specific Event</span>
                        </div>

                    </div>

                    <div className="flex gap-2 h-128">

                        {/* Universal Params Checklist */}
                        <FlightsSelectedModalChecklist
                            chartFlights={localChartFlights}
                            disableItems={false}
                            universalEvents={eventSelection.universalEvents}
                            toggleUniversalEvent={toggleUniversalEvent}
                        />

                        {/* Selected Flights List */}
                        <FlightsSelectedModalFlights
                            chartFlights={localChartFlights}
                            setChartFlights={handleSetChartFlights}
                            eventSelection={eventSelection}
                            toggleUniversalEvent={toggleUniversalEvent}
                            togglePerFlightEvent={togglePerFlightEvent}
                        />

                    </div>

                </CardContent>
            </Card>
        </motion.div>
    );

}