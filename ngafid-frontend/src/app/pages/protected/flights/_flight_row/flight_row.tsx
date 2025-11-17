// ngafid-frontend/src/app/pages/protected/flights/_flight_row/flight_row.tsx
import { useModal } from "@/components/modals/modal_provider";
import TagsListModal from "@/components/modals/tags_list_modal";
import { type TagData } from "@/components/providers/tags/tags_provider";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Tooltip, TooltipContent, TooltipTrigger } from "@/components/ui/tooltip";
import FlightRowFlightIDButton from "@/pages/protected/flights/_flight_row/flight_row_flight_id_button";
import FlightRowTagBadge from "@/pages/protected/flights/_flight_row/flight_row_tag_badge";
import { useFlights } from "@/pages/protected/flights/flights";
import { Calendar, ChartArea, Clock, Dot, Download, Globe2, Map, MapPinned, Minus, MousePointerClick, PlaneTakeoff, Tag, Tags } from "lucide-react";
import { AirframeNameID } from "src/types";

export interface ItineraryEntry {
    airport: string;
    endOfApproach: number;
    endOfTakeoff: number;
    finalIndex: number;
    minAirportDistance: number;
    minAltitude: number;
    minAltitudeIndex: number;
    minRunwayDistance: number;
    order: number;
    runway: string;
    runwayCounts: unknown; // ⚠️ TODO: Define type
    startOfApproach: number;
    startOfTakeoff: number;
    takeoffCounter: number;
    type: string;   // ⚠️ TODO: Define valid type names
}

export type AirframeNameIDType = { // ⚠️ TODO: Figure out what to do with this
    type: {
        id: number;
        name: string;
    }
} & AirframeNameID;


export interface Flight {
    filename: string;
    systemId: string;
    md5Hash: string;
    startDateTime: string;
    endDateTime: string;
    itinerary: ItineraryEntry[];
    id: number;
    fleetId: number;
    uploaderId: number;
    uploadId: number;
    tailNumber: string;
    airframe: AirframeNameIDType;
    status: string; // ⚠️ TODO: Define valid status names
    numberRows: number;
    doubleTimeSeries: object;  // ⚠️ TODO: Define valid double time series types
    stringTimeSeries: object;  // ⚠️ TODO: Define valid string time series types
    events: any[];  // ⚠️ TODO: Define valid event types
    tags: TagData[] | null;
}

function FlightRowSection({children, className}: {children: React.ReactNode, className?: string}) {

    return <Card className={`
        w-full flex
        dark:bg-sidebar
        p-4
        last:rounded-l-none
        first:rounded-r-none
        not-first:not-last:rounded-l-none not-first:not-last:rounded-r-none
        [&:has(>[data-fit])]:w-fit
        overflow-clip
        ${className}
    `}>
        {children}
    </Card>;

}


export default function FlightRow({ flight }: { flight: Flight }) {

    const { setModal } = useModal();
    const { updateFlightTags, fetchFlightsWithFilter, filter, filterSearched } = useFlights();

    const renderFlightMainDetails = () => {

        const renderDetailItem = (value: string|number, fallback: string) => {

            // Valid value
            if (value !== null && value !== undefined && value !== "")
                return <span>{value}</span>;

            // Fallback value
            return <span className="opacity-50">{fallback}</span>;

        }

        return <div className="grid grid-cols-2 text-nowrap gap-2 gap-x-6 items-start">

            {/* Flight ID */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <FlightRowFlightIDButton flightID={flight.id} renderDetailItem={renderDetailItem} />
                </TooltipTrigger>
                <TooltipContent>
                    <div>Flight ID</div>
                    <div className="opacity-50 flex items-center">
                        <MousePointerClick size={16} className="mr-1"/>Add to Flight IDs filter group
                    </div>
                </TooltipContent>
            </Tooltip>

            {/* System ID */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger>
                    <Dot size={16} className="inline" />
                    {renderDetailItem(flight.systemId, "N/A")}
                </TooltipTrigger>
                <TooltipContent>
                    System ID
                </TooltipContent>
            </Tooltip>

            {/* Aircraft Type */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger>
                    <Dot size={16} className="inline" />
                    {renderDetailItem(flight.airframe.name, "N/A")}
                </TooltipTrigger>
                <TooltipContent>
                    Aircraft Type
                </TooltipContent>
            </Tooltip>

            {/* Aircraft Tail Number */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger>
                    <Dot size={16} className="inline" />
                    {renderDetailItem(flight.tailNumber, "N/A")}
                </TooltipTrigger>
                <TooltipContent>
                    Aircraft Tail Number
                </TooltipContent>
            </Tooltip>

        </div>

    }

    const renderFlightTimeDetails = () => {

        /*
            Displays the following information:

            - Start Date/Time
            - End Date/Time
            - Duration
        */

        const renderDetailItem = (value: string|number, fallback: string) => {

            // Valid value
            if (value !== null && value !== undefined && value !== "")
                return <span>{value}</span>;

            // Fallback value
            return <span className="opacity-50">{fallback}</span>;

        }


        const renderDateTime = (value: string|number, fallback: string) => {

            const date = new Date(value);

            // Invalid date -> Fallback
            if (isNaN(date.getTime()))
                return <span className="opacity-50">{fallback}</span>;

            const formattedDate = date.toLocaleDateString(undefined, {
                year: 'numeric',
                month: 'short',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
            });

            return <span className="text-nowrap">{formattedDate}</span>;

        }

        const flightDuration = (flight: Flight): string => {

            const start = new Date(flight.startDateTime);
            const end = new Date(flight.endDateTime);
            const duration = end.getTime() - start.getTime();

            const hours = Math.floor(duration / (1000 * 60 * 60));
            const minutes = Math.floor((duration % (1000 * 60 * 60)) / (1000 * 60));
            const seconds = Math.floor((duration % (1000 * 60)) / 1000);

            const minutesStr = minutes < 10 ? `0${minutes}` : `${minutes}`;
            const secondsStr = seconds < 10 ? `0${seconds}` : `${seconds}`;

            return `${hours}h ${minutesStr}m ${secondsStr}s`;

        }

        // return <div className="flex flex-col gap-2">
        return <div className="grid grid-rows-2 text-nowrap min-w-64 gap-2 items-start">

            {/* Date/Time Row */}
            <div className="flex gap-2 items-center flex-wrap">

                {/* Start Date/Time */}
                <Tooltip disableHoverableContent>
                    <TooltipTrigger>
                        <div className="flex gap-2 items-center">
                            <Calendar size={16}/>
                            {renderDateTime(flight.startDateTime, "No Start Date/Time")}
                        </div>
                    </TooltipTrigger>
                    <TooltipContent>
                        Start Date/Time
                    </TooltipContent>
                </Tooltip>

                {/* End Date/Time */}
                <Tooltip disableHoverableContent>
                    <TooltipTrigger>
                        <div className="flex gap-2 items-center">
                            {/* <div className="opacity-25 select-none">—</div> */}
                            <Minus size={16} className="opacity-25"/>
                            {renderDateTime(flight.endDateTime, "No End Date/Time")}
                        </div>
                    </TooltipTrigger>
                    <TooltipContent>
                        End Date/Time
                    </TooltipContent>
                </Tooltip>
                
            </div>

            {/* Duration Row */}
            <div className="flex gap-2 items-center">

                <Clock size={16}/>

                <Tooltip disableHoverableContent>
                    <TooltipTrigger>
                        {renderDetailItem(flightDuration(flight), "No Duration")}
                    </TooltipTrigger>
                    <TooltipContent>
                        Duration
                    </TooltipContent>
                </Tooltip>
            </div>

        </div>

    }

    const renderAirportsDetails = () => {

        /*
            Displays the following information:

            - Departure Airport
            - Arrival Airport
        */

        const noAirports = (flight.itinerary.length === 0);

        return (
            <Tooltip disableHoverableContent>
                <TooltipTrigger className={`w-full flex flex-row flex-wrap items-center mb-auto gap-2 ${noAirports ? 'opacity-50' : ''}`}>

                        <PlaneTakeoff size={16} />
                        {
                            (noAirports)
                            ?
                            <span>No Airports</span>
                            :
                            flight.itinerary.map((itineraryItem, index) => (

                                <div key={index} className="not-last:after:content-['\,'] text-xs">
                                    <b>{itineraryItem.airport}</b>
                                </div>
                            ))
                        }

                </TooltipTrigger>
                <TooltipContent>
                    Airports
                </TooltipContent>
            </Tooltip>
        );

    }

    const renderTagsRows = () => {

        /*
            Displays the tags associated
            with this flight.
        */

        const flightTags = (flight.tags || []);
        const tagsSorted = flightTags.sort((a, b) => a.name.length - b.name.length);

        const noTags = (flightTags.length === 0);

        return <div className={`flex flex-row flex-wrap gap-2 items-center w-full mb-auto ${noTags ? 'opacity-50' : ''}`}>

            {
                (noTags)
                ?
                <>
                    <Tag size={16} />
                    <span>No Tags</span>
                </>
                :
                tagsSorted.map((tag, index) => (
                    <FlightRowTagBadge key={index} tag={tag} />
                ))
            }
        </div>

    }

    const renderButtonsRow = () => {

        return <div className="grid row-span-3 grid-cols-3 min-w-32 gap-1 gap-x-2 my-auto" data-fit>

            {/* Chart Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button variant="ghost" className="w-8 h-8">
                        <ChartArea size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Chart
                </TooltipContent>
            </Tooltip>

            {/* Cesium Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button variant="ghost" className="w-8 h-8">
                        <Globe2 size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Cesium
                </TooltipContent>
            </Tooltip>

            {/* Map Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button variant="ghost" className="w-8 h-8">
                        <Map size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Map
                </TooltipContent>
            </Tooltip>



            {/* Tags Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button
                        variant="ghost"
                        className="w-8 h-8"
                        onClick={() => setModal(TagsListModal, {
                            flightTags: flight.tags || [],
                            flightId: flight.id,
                            onTagsUpdate: (updatedTags: TagData[]) => {
                                updateFlightTags(flight.id, updatedTags);
                                fetchFlightsWithFilter(filterSearched??filter, true);
                            }
                        })}
                    >
                        <Tags size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Tags
                </TooltipContent>
            </Tooltip>

            {/* Events Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button variant="ghost" className="w-8 h-8">
                        <MapPinned size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Events
                </TooltipContent>
            </Tooltip>

            {/* Download Button */}
            <Tooltip disableHoverableContent>
                <TooltipTrigger asChild>
                    <Button variant="ghost" className="w-8 h-8">
                        <Download size={16} />
                    </Button>
                </TooltipTrigger>
                <TooltipContent>
                    Download
                </TooltipContent>
            </Tooltip>

        </div>

    }

    // return <Card className="w-full flex bg-background">
    return <div className="
        w-full flex min-h-20
        *:rounded-none

        max-h-36

        text-xs
        *:@lg:*:text-sm
    ">
        <FlightRowSection className="@container min-w-54">
            {renderFlightMainDetails()}
        </FlightRowSection>

        <FlightRowSection className="@container min-w-48">
            {renderFlightTimeDetails()}
        </FlightRowSection>

        <FlightRowSection className="@container min-w-14 max-w-48">
            {renderAirportsDetails()}
        </FlightRowSection>

        {/* <FlightRowSection className="max-w-64"> */}
        <FlightRowSection className="@container max-w-96 overflow-y-auto">
            {renderTagsRows()}
        </FlightRowSection>

        <FlightRowSection className="p-3 min-w-38 w-38">
            {renderButtonsRow()}
        </FlightRowSection>

    </div>

}