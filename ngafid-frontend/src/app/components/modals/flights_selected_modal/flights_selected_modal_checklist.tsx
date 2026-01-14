// ngafid-frontend/src/app/components/modals/flights_selected_modal/flights_selected_modal_checklist.tsx

"use client";

import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { CheckboxStatic } from "@/components/ui/checkbox-static";
import { Separator } from "@/components/ui/separator";
import { Flight } from "@/pages/protected/flights/types";

const log = getLogger("FlightsSelectedModalChecklist", "black", "Component");

type Props = {
    chartFlights: Flight[];
    disableItems: boolean;
    universalEvents: Set<string>;
    toggleUniversalEvent: (name: string) => void;
};

function getAvailableEventNamesForFlight(flight: Flight): Set<string> {

    if (!flight.events || !flight.eventDefinitions)
        return new Set();

    const ids = new Set(flight.events.map(e => e.eventDefinitionId));
    const names = new Set<string>();
    for (const def of flight.eventDefinitions) {
        if (ids.has(def.id)) names.add(def.name);
    }

    return names;

}


export function FlightsSelectedModalChecklist({ chartFlights, disableItems, universalEvents, toggleUniversalEvent }: Props) {

    /*
        Universal events list:

            - Union of all event names that occur on any selected flight
            - Matching FlightEvent.eventDefinitionId -> EventDefinition.id
            - De-duplicated by name
            - Includes any currently-selected universal events (so they can be untoggled)
    */
    const unionNamesSorted = (() => {

        const all = new Set<string>();
        for (const flight of chartFlights) {

            for (const name of getAvailableEventNamesForFlight(flight)) {
                all.add(name);
            }

        }

        return Array.from(all).sort((a, b) => a.localeCompare(b));

    })();


    const renderChecklistItem = (item: string, index: number) => {

        const checked = universalEvents.has(item);
        const handleClick = () => {

            // Disabled, do nothing
            if (disableItems)
                return;

            log("Toggling universal event:", item);
            toggleUniversalEvent(item);

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
                    <CheckboxStatic checked={checked} />
                </Button>
                <Separator />
            </div>
        );
    };


    return (
        <div className={`flex flex-col gap-2 h-full ${disableItems ? "opacity-25 select-none pointer-events-none" : ""}`}>
            <div>Universal Events</div>
            <div className="flex flex-col rounded-lg overflow-y-auto border w-52 h-full">
                {
                    (unionNamesSorted.length === 0)
                        ? <span className="text-muted-foreground mx-auto my-auto select-none">None Available.</span>
                        : unionNamesSorted.map(renderChecklistItem)
                }
            </div>
        </div>
    );

}