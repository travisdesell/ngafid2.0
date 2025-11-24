// ngafid-frontend/src/app/components/modals/charts_list_modal/charts_list_modal_flights.tsx

"use client";

import { ChartsListModalBadge } from "@/components/modals/charts_list_modal/charts_list_modal_badge";
import { NumberInput } from "@/components/number_input";
import { getLogger } from "@/components/providers/logger";
import { Button } from "@/components/ui/button";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { Separator } from "@/components/ui/separator";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import type { Flight } from "@/pages/protected/flights/types";
import { ChartSelectionState } from "@/pages/protected/flights/types_charts";
import { ChevronDown, Minus, Plane, Plus } from "lucide-react";
import { Dispatch, SetStateAction, useState } from "react";

const log = getLogger("ChartsListModalFlights", "black", "Component");

type Props = {
    chartFlights: Flight[];
    setChartFlights: Dispatch<SetStateAction<Flight[]>>;
    chartSelection: ChartSelectionState;
    togglePerFlightParam: (flightId: number, name: string) => void;
    toggleUniversalParam: (name: string) => void;
};

type FlightBadge = {
    label: string;
    isUniversal: boolean;
};

export function ChartsListModalFlights({ chartFlights, setChartFlights, chartSelection, togglePerFlightParam, toggleUniversalParam }: Props) {

    const [rangeIDLower, setRangeIDLower] = useState<number | undefined>(undefined);
    const [rangeIDUpper, setRangeIDUpper] = useState<number | undefined>(undefined);


    const removeFlight = (flightId: number) => {

        log(`Removing flight with ID ${flightId} from chart flights.`);

        setChartFlights((prev) =>
            prev.filter((f) => f.id !== flightId),
        );

    };

    const renderChartFlights = () => {

        log("Rendering chart flights list.");

        if (chartFlights.length === 0) {

            log("No chart flights found, rendering empty message.");

            return (
                <div className="w-fit mx-auto space-x-8 drop-shadow-md flex items-center *:text-nowrap">
                    <span>No flights selected.</span>
                </div>
            );

        }

        const renderChartFlightRow = (flight: Flight, index: number) => {

            const universal = chartSelection.universalParams;
            const perFlightSet = (chartSelection.perFlightParams[flight.id] ?? new Set<string>());

            // Union of universal + flight-specific for this flight
            const badgeNames = new Set<string>();
            universal.forEach((name) => badgeNames.add(name));
            perFlightSet.forEach((name) => badgeNames.add(name));

            const badgesForFlight = Array.from(badgeNames).map((name) => ({
                label: name,
                isUniversal: universal.has(name),
            }));

            const renderChartFlightRowDataBadges = () => {

                if (badgesForFlight.length === 0) {

                    return (
                        <div className="w-full mx-auto drop-shadow-md flex flex-col items-center *:text-nowrap text-xs text-muted-foreground">
                            <span>Use the dropdown menu to add parameters.</span>
                        </div>
                    );

                }

                return (
                    <div className="flex flex-row gap-2 flex-wrap my-1">
                        {
                            badgesForFlight.map((badge) => (
                                <ChartsListModalBadge
                                    label={badge.label}
                                    isUniversal={badge.isUniversal}
                                    key={`badge-${flight.id}-${badge.label}`}
                                    onClick={() => {
                                        
                                        if (badge.isUniversal) {
                                            
                                            log("Removing universal parameter via badge click:", badge.label);
                                            toggleUniversalParam(badge.label);

                                        } else {

                                            log("Removing flight-specific parameter via badge click:", flight.id, badge.label);
                                            togglePerFlightParam(
                                                flight.id,
                                                badge.label,
                                            );

                                        }

                                    }}
                                />
                            ))
                        }
                    </div>
                );

            };

            const renderFlightParametersDropdownMenuContent = () => {

                const perSet = (chartSelection.perFlightParams[flight.id] ?? new Set<string>());

                const isActive = (name: string) =>
                    universal.has(name) || perSet.has(name);

                // Combine common and uncommon trace names, filtering out active ones
                const commonUnused = (flight.commonTraceNames ?? []).filter((name) => !isActive(name));
                const uncommonUnused = (flight.uncommonTraceNames ?? []).filter((name) => !isActive(name));
                const allTraceNames = [...commonUnused, ...uncommonUnused,];

                log(`Rendering parameters dropdown for flight ${flight.id} with ${allTraceNames.length} options.`);

                // No parameters available, show empty message
                if (allTraceNames.length === 0)
                    return (
                        <DropdownMenuContent>
                            <DropdownMenuItem className="text-xs">
                                No Parameters
                            </DropdownMenuItem>
                        </DropdownMenuContent>
                    );

                // Record the index where uncommon parameters start
                const firstUncommonIndex = commonUnused.length;

                const renderFlightParameterOption = (label: string) => {

                    const handleClick = () => {

                        log("Toggling flight-specific parameter from dropdown:", flight.id, label);
                        togglePerFlightParam(flight.id, label);

                    };

                    return (
                        <DropdownMenuItem
                            className="flex items-center"
                            key={label}
                            onClick={handleClick}
                        >
                            <Plus className="text-muted-foreground w-3! aspect-square" />
                            <span className="text-xs">{label}</span>
                        </DropdownMenuItem>
                    );

                };

                return (
                    <DropdownMenuContent className="max-h-128 overflow-y-auto">
                        {
                            allTraceNames.map((param, idx) => (
                                <div key={`param-${flight.id}-${idx}`}>

                                    {
                                        (idx === firstUncommonIndex)
                                        &&
                                        <Separator className="my-1" />
                                    }

                                    {renderFlightParameterOption(param)}
                                </div>
                            ))
                        }
                    </DropdownMenuContent>
                );
            };

            return (
                <div
                    key={index}
                    className="flex flex-col p-2 pl-4 border-b gap-2 hover:bg-background"
                >

                    {/* Main Row */}
                    <div className="flex flex-row items-center w-full gap-1">

                        {/* Flight ID */}
                        <div className="font-medium truncate select-all flex items-center gap-1">
                            <Plane
                                size={16}
                                className="text-muted-foreground"
                            />
                            <span className="flex items-center gap-1 text-muted-foreground">
                                <b className="text-foreground">{flight.id}</b>
                            </span>
                        </div>

                        {/* Flight Parameters Dropdown Button */}
                        <Tooltip disableHoverableContent>
                            <DropdownMenu>
                                <TooltipTrigger className="ml-auto">
                                    <DropdownMenuTrigger className="w-9 aspect-square ml-auto p-0 *:mx-auto cursor-pointer border-none after:content-['']">
                                        <Button variant="ghost" className="w-full">
                                            <ChevronDown />
                                        </Button>
                                    </DropdownMenuTrigger>
                                </TooltipTrigger>

                                {renderFlightParametersDropdownMenuContent()}
                            </DropdownMenu>

                            <TooltipContent>
                                Change Flight Parameters
                            </TooltipContent>
                        </Tooltip>

                        {/* Flight Remove Button */}
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

                    {/* Badges Section */}
                    {renderChartFlightRowDataBadges()}

                </div>
            );
        };

        return (
            <div className="max-h-128 overflow-y-auto border rounded">
                {
                    chartFlights.map((flight, index) =>
                        renderChartFlightRow(flight, index),
                    )
                }
            </div>
        );
    };


    /*
        ⚠️ CURRENTLY UNUSED ⚠️

        Used to render a section for adding a
        range of flight IDs to the chart.

        Fit this into the UI below the list of
        selected flights if people ask for the
        ability to add flights into the list
        from inside the modal.
    */
    const renderFlightIDRangeAdder = () => {

        /*
            The Upper ID will default to the
            lower ID if only the lower ID is set.
            
            The Upper ID will also always be
            greater than or equal to the lower ID.
        */

        log("Rendering flight ID range adder.");

        const upperIDPlaceholder = (rangeIDLower !== undefined)
            ? `Upper ID (≥ ${rangeIDLower})`
            : "Upper ID";

        const allowAdd = (rangeIDLower !== undefined);

        return <div className="w-full flex items-center">

            {/* Lower Range */}
            <NumberInput
                value={rangeIDLower}
                onValueChange={(e) => {
                    setRangeIDLower(e)
                    if (rangeIDUpper !== undefined && e !== undefined && e > rangeIDUpper)
                        setRangeIDUpper(e);
                }}
                placeholder="Lower ID"
                min={0}
            />

            {/* Upper Range */}
            <NumberInput
                value={rangeIDUpper}
                onValueChange={(e) => setRangeIDUpper(e)}
                placeholder={upperIDPlaceholder}
                className="rounded-none"
                min={rangeIDLower ?? 0}
            />

            {/* Add Button */}
            <Button variant="outline" className="w-8 aspect-square ml-2" disabled={!allowAdd}>
                <Plus />
            </Button>

        </div>

    }

    return (
        <div className="flex flex-col gap-2 w-full h-full ">
            <div>Selected Flights</div>
            <div className="grid rounded-lg overflow-y-auto border w-full h-full">
                {renderChartFlights()}
            </div>

            {/* {renderFlightIDRangeAdder()} */}

        </div>
    );

}