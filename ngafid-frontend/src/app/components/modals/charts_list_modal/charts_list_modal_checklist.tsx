// ngafid-frontend/src/app/components/modals/charts_list_modal/charts_list_modal_checklist.tsx

"use client";

import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { CheckboxStatic } from "@/components/ui/checkbox-static";
import { Separator } from "@/components/ui/separator";
import { Flight } from "@/pages/protected/flights/types";

const log = getLogger("ChartsListModalChecklist", "black", "Component");


type Props = {
    chartFlights: Flight[];
    disableItems: boolean;
    universalParams: Set<string>;
    toggleUniversalParam: (name: string) => void;
};


export function ChartsListModalChecklist({ chartFlights, disableItems, universalParams, toggleUniversalParam }: Props) {
 
    /*
        Create a list of trace names which are
        shared between all of the selected flights.

        Universal trace names from the flights'
        'commonTraceNames' arrays will come firs,
        and the 'uncommonTraceNames' will come after.
    */
    const sharedTraceNamesList = chartFlights.reduce((shared, flight) => {

        if (!flight.commonTraceNames || !flight.uncommonTraceNames)
            return shared;

        const flightTraceNames = [...flight.commonTraceNames, ...flight.uncommonTraceNames];

        if (shared === null)
            return flightTraceNames;

        return shared.filter(name => flightTraceNames.includes(name));

    }, null as string[] | null) ?? [];



    const renderChecklistItem = (item: string, index: number) => {

        const checked = universalParams.has(item);

        const handleClick = () => {

            // Disabled, do nothing
            if (disableItems)
                return;

            log("Toggling universal parameter:", item);
            toggleUniversalParam(item);

        };

        return (
            <div key={index}>
                <Button
                    variant="outline"
                    className="w-full rounded-none border-none flex items-center justify-between"
                    disabled={disableItems}
                    onClick={handleClick}
                >
                    <span className="truncate">{item}</span>
                    <CheckboxStatic
                        className="pointer-events-none"
                        checked={checked}
                    />
                </Button>
                <Separator />
            </div>
        );
    };


    return (
        <div className={`flex flex-col gap-2 h-full ${disableItems ? "opacity-25 select-none pointer-events-none" : ""}`}>
            <div >Universal Parameters</div>
            <div className={`grid rounded-lg overflow-y-auto border w-52 h-full`}>
                {sharedTraceNamesList.map(renderChecklistItem)}
            </div>
        </div>
    );

}