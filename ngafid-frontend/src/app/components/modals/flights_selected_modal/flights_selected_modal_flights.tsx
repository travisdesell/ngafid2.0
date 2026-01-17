// ngafid-frontend/src/app/components/modals/flights_selected_modal/flights_selected_modal_flights.tsx

"use client";

import { FlightsSelectedModalBadge } from "@/components/modals/flights_selected_modal/flights_selected_modal_badge";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import type { Flight } from "@/pages/protected/flights/types";
import { EventSelectionState } from "@/pages/protected/flights/types_events";
import { Check, ChevronDown, Minus, Plane, Plus } from "lucide-react";
import { Dispatch, SetStateAction } from "react";

const log = getLogger("FlightsSelectedModalFlights", "black", "Component");

type Props = {
    chartFlights: Flight[];
    setChartFlights: Dispatch<SetStateAction<Flight[]>>;

    eventSelection: EventSelectionState;
    toggleUniversalEvent: (name: string) => void;
    togglePerFlightEvent: (flightId: number, name: string) => void;
};


function getAvailableEventNamesForFlight(flight: Flight): Set<string> {

    // No events or definitions -> Empty set
    if (!flight.events || !flight.eventDefinitions)
        return new Set();

    // Map eventDefinitionIds to names
    const ids = new Set(flight.events.map(e => e.eventDefinitionId));
    const names = new Set<string>();

    for (const def of flight.eventDefinitions) {

        if (ids.has(def.id))
            names.add(def.name);

    }

    return names;

}


export function FlightsSelectedModalFlights({ chartFlights, setChartFlights, eventSelection, toggleUniversalEvent, togglePerFlightEvent }: Props) {


    const removeFlight = (flightId: number) => {

        log(`Removing flight with ID ${flightId} from chart flights.`);
        setChartFlights((prev) => prev.filter((f) => f.id !== flightId));

    };

    const renderChartFlights = () => {

        log("Rendering chart flights list: ", chartFlights);

        if (chartFlights.length === 0) {

            log("No chart flights found, rendering empty message.");

            return (
                <div className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap">
                    <span>No flights selected.</span>
                </div>
            );

        }

        const renderChartFlightRow = (flight: Flight, index: number) => {

            const hasEventsAvailable = (flight.eventCount > 0);

            const hasLoadedEvents = !!(flight.events && flight.eventDefinitions);

            // const availableNames = useMemo(
            //     () => getAvailableEventNamesForFlight(flight),
            //     [flight.events, flight.eventDefinitions],
            // );

            // const availableNamesSorted = useMemo(
            //     () => Array.from(availableNames).sort((a, b) => a.localeCompare(b)),
            //     [availableNames],
            // );

            const availableNames = getAvailableEventNamesForFlight(flight);
            const availableNamesSorted = Array.from(availableNames).sort((a, b) => a.localeCompare(b));

            const perSet = (eventSelection.perFlightEvents[flight.id] ?? new Set<string>());

            const universalList = Array.from(eventSelection.universalEvents);

            const missingUniversalCount = universalList.filter(n => !availableNames.has(n)).length;

            const selectedPresentCount = universalList.filter(n => availableNames.has(n)).length
                + Array.from(perSet).filter(n => availableNames.has(n) && !eventSelection.universalEvents.has(n)).length;

            const totalAvailableTypes = availableNames.size;

            const selectedCountLabel = (!hasEventsAvailable)
                ? "None Available"
                : (!hasLoadedEvents)
                    ? "Loading..."
                    : `${selectedPresentCount}${missingUniversalCount > 0 ? ` (+${missingUniversalCount} missing)` : ""} / ${totalAvailableTypes} Selected`;


            const renderBadges = () => {
                const anySelected =
                    eventSelection.universalEvents.size > 0
                    || (perSet && perSet.size > 0);

                if (!anySelected)
                    return null;

                return (
                    <div className="flex flex-wrap gap-1 pl-6 pr-2 pb-1">
                        {/* Universal badges first */}
                        {
                            Array.from(eventSelection.universalEvents).map((name) => {
                                const missing = !availableNames.has(name);
                                const kind = missing ? "universal-missing" : "universal";
                                return (
                                    <FlightsSelectedModalBadge
                                        key={`u:${name}`}
                                        label={name}
                                        kind={kind}
                                        onClick={() => toggleUniversalEvent(name)}
                                    />
                                );
                            })
                        }

                        {/* Per-flight badges (excluding anything also universal) */}
                        {
                            Array.from(perSet)
                                .filter(name => !eventSelection.universalEvents.has(name))
                                .map((name) => (
                                    <FlightsSelectedModalBadge
                                        key={`p:${flight.id}:${name}`}
                                        label={name}
                                        kind="per-flight"
                                        onClick={() => togglePerFlightEvent(flight.id, name)}
                                    />
                                ))
                        }
                    </div>
                );
            };

            const renderFlightEventsDropdownMenuContent = () => {
                if (!hasEventsAvailable) {
                    return (
                        <DropdownMenuContent>
                            <DropdownMenuItem className="text-xs">No Events</DropdownMenuItem>
                        </DropdownMenuContent>
                    );
                }

                if (!hasLoadedEvents) {
                    return (
                        <DropdownMenuContent>
                            <DropdownMenuItem className="text-xs">Loading...</DropdownMenuItem>
                        </DropdownMenuContent>
                    );
                }

                if (availableNamesSorted.length === 0) {
                    return (
                        <DropdownMenuContent>
                            <DropdownMenuItem className="text-xs">No Event Types</DropdownMenuItem>
                        </DropdownMenuContent>
                    );
                }

                return (
                    <DropdownMenuContent className="max-h-96 overflow-y-auto">
                        {availableNamesSorted.map((label) => {
                            const isUniversal = eventSelection.universalEvents.has(label);
                            const isPerFlight = perSet.has(label);

                            const disabled = isUniversal;

                            const handleClick = () => {
                                if (disabled)
                                    return;

                                log(`Toggling flight-specific event from dropdown: Flight ${flight.id}, Event "${label}"`);
                                togglePerFlightEvent(flight.id, label);
                            };

                            return (
                                <DropdownMenuItem
                                    className={`flex items-center ${disabled ? "opacity-50" : ""}`}
                                    key={label}
                                    onClick={handleClick}
                                    disabled={disabled}
                                >
                                    {
                                        isUniversal
                                            ? <Check className="text-muted-foreground w-3! aspect-square" />
                                            : isPerFlight
                                                ? <Minus className="text-muted-foreground w-3! aspect-square" />
                                                : <Plus className="text-muted-foreground w-3! aspect-square" />
                                    }
                                    <span className="text-xs">{label}</span>
                                </DropdownMenuItem>
                            );
                        })}

                        <DropdownMenuSeparator />

                        <DropdownMenuItem className="text-xs opacity-60 pointer-events-none">
                            Universal events are toggled on the left.
                        </DropdownMenuItem>
                    </DropdownMenuContent>
                );
            };

            return (
                <div key={flight.id} className="flex flex-col p-2 pl-4 border-b gap-2 hover:bg-background">
                    <div className="flex flex-row items-center w-full justify-between">
                        <div className="font-medium truncate select-all flex items-center gap-1">
                            <Plane size={16} className="text-muted-foreground" />
                            <span className="flex items-center gap-1 text-muted-foreground">
                                <b className="text-foreground">{flight.id}</b>
                            </span>
                        </div>

                        <div className="text-sm text-muted-foreground mr-auto ml-4">
                            {selectedCountLabel}
                        </div>

                        <div
                          onFocusCapture={(e) => {
    e.stopPropagation();
  }}
                        >
                            <Tooltip disableHoverableContent>
                                <DropdownMenu>
                                    <TooltipTrigger className="ml-auto" asChild disabled={!hasEventsAvailable || !hasLoadedEvents}>
                                        <DropdownMenuTrigger className="ml-auto p-0 cursor-pointer" asChild>
                                            <Button variant="ghost" className="w-9 flex items-center justify-center">
                                                <ChevronDown />
                                            </Button>
                                        </DropdownMenuTrigger>
                                    </TooltipTrigger>

                                    {renderFlightEventsDropdownMenuContent()}
                                </DropdownMenu>

                                <TooltipContent>Change Flight Events</TooltipContent>
                            </Tooltip>
                        </div>

                        <Tooltip disableHoverableContent>
                            <TooltipTrigger asChild>
                                <Button
                                    className="w-9 aspect-square"
                                    variant="ghostDestructive"
                                    size="icon"
                                    onClick={() => removeFlight(flight.id)}
                                >
                                    <Minus size={16} />
                                </Button>
                            </TooltipTrigger>
                            <TooltipContent>Remove Flight</TooltipContent>
                        </Tooltip>
                    </div>

                    {renderBadges()}
                </div>
            );

        };

        return (
            <div className="max-h-128 overflow-y-auto border rounded w-full">
                {
                    chartFlights.map((flight, index) =>
                        renderChartFlightRow(flight, index),
                    )
                }
            </div>
        );
    };

    return (
        <div className="flex flex-col gap-2 w-full h-full ">
            <div>Selected Flights</div>
            <div className="grid rounded-lg overflow-y-auto border w-full h-full">
                {renderChartFlights()}
            </div>
        </div>
    );

}