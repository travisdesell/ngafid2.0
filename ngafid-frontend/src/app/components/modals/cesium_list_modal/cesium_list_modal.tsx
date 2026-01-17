// ngafid-frontend/src/app/components/modals/cesium_list_modal/cesium_list_modal.tsx

"use client";

import { FlightsSelectedModalFlights } from "@/components/modals/flights_selected_modal/flights_selected_modal_flights";
import { useModal } from "@/components/modals/modal_context";
import { ModalData, ModalProps } from "@/components/modals/types";
import { getLogger } from "@/components/providers/logger";
import { Card, CardContent } from "@/components/ui/card";
import { Flight } from "@/pages/protected/flights/types";
import { motion } from "motion/react";
import { Dispatch, SetStateAction, useState } from "react";

const log = getLogger("CesiumListModal", "black", "Modal");


export type ModalDataFlightsSelected = ModalData & {
    chartFlights: Flight[];
    setChartFlights: Dispatch<SetStateAction<Flight[]>>;
};

export function CesiumListModal({ data }: ModalProps<ModalDataFlightsSelected>) {

    const { close, renderModalHeader } = useModal();
    const { chartFlights, setChartFlights } = (data as ModalDataFlightsSelected) ?? {};

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

    return (
        <motion.div
            initial={{ scale: 0, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            exit={{ scale: 0, opacity: 0 }}
            className="w-full h-full"
        >
            <Card className="w-full max-w-xl fixed top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2">
                
                {/* Modal Header */}
                {renderModalHeader("Cesium Data", "Change what data is shown in the Cesium viewer.", true)}

                <CardContent className="space-y-8 h-152 flex flex-col">

                    <div className="flex gap-2 h-128">

                        {/* Selected Flights List */}
                        <FlightsSelectedModalFlights
                            chartFlights={localChartFlights}
                            setChartFlights={handleSetChartFlights}
                            //  eventSelection={/*...*/}
                            //  toggleUniversalEvent={/*...*/}
                            //  togglePerFlightEvent={/*...*/}
                        />

                    </div>

                </CardContent>
            </Card>
        </motion.div>
    );

}