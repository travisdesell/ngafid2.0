import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Calendar, ChartArea, Clock, Download, Globe2, Map, MapPinned, Plane, Tag } from "lucide-react";
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
    airframe: AirframeNameIDType;
    tailNumber: string;
    status: string; // ⚠️ TODO: Define valid status names
    numberRows: number;
    doubleTimeSeries: object;  // ⚠️ TODO: Define valid double time series types
    stringTimeSeries: object;  // ⚠️ TODO: Define valid string time series types
    events: any[];  // ⚠️ TODO: Define valid event types
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
        ${className}
    `}>
        {children}
    </Card>;

}


export default function FlightRow({ flight }: { flight: Flight }) {



    const renderFlightMainDetails = () => {

        const renderDetailItem = (value: string|number, fallback: string) => {

            // Valid value
            if (value !== null && value !== undefined && value !== "")
                return <span>{value}</span>;

            // Fallback value
            return <span className="opacity-50">{fallback}</span>;

        }

        return <div className="grid grid-cols-2 text-nowrap min-w-64 gap-2">

            {/* Flight ID */}
            {/* {renderDetailItem(flight.id, "No Flight ID")} */}
            <Button variant="link" className="p-0 m-0 w-fit h-fit">
                <Plane size={16} className="inline" />
                {renderDetailItem(flight.id, "No Flight ID")}
            </Button>

            {/* Aircraft Tail Number */}
            {renderDetailItem(flight.tailNumber, "No Tail Number")}

            {/* Aircraft Type */}
            {renderDetailItem(flight.airframe.name, "No Aircraft Type")}

            {/* System ID */}
            {renderDetailItem(flight.systemId, "No System ID")}

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

            return <span>{formattedDate}</span>;

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

        return <div className="flex flex-col gap-2 min-w-106">

            {/* Date/Time Row */}
            <div className="flex gap-2 items-center text-nowrap">

                <Calendar size={16}/>

                {/* Start Date/Time */}
                {renderDateTime(flight.startDateTime, "No Start Date/Time")}
                <div className="opacity-25 select-none">—</div>

                {/* End Date/Time */}
                {renderDateTime(flight.endDateTime, "No End Date/Time")}
                
            </div>

            {/* Duration Row */}
            <div className="flex gap-2 items-center">

                <Clock size={16}/>

                {renderDetailItem(flightDuration(flight), "No Duration")}
            </div>

        </div>

    }

    const renderAirportsDetails = () => {

        /*
            Displays the following information:

            - Departure Airport
            - Arrival Airport
        */

        const departureAirport = flight.itinerary.find(entry => entry.type === "departure")?.airport || "Unknown";
        const arrivalAirport = flight.itinerary.find(entry => entry.type === "arrival")?.airport || "Unknown";

        return <div className="grid grid-rows-2 gap-2">

            {/* Departure Airport */}
            <div>
                <span className="font-semibold">Departure:</span> {departureAirport}
            </div>

            {/* Arrival Airport */}
            <div>
                <span className="font-semibold">Arrival:</span> {arrivalAirport}
            </div>

        </div>

    }

    const renderTagsRows = () => {

        /*
            Displays the tags associated
            with this flight.
        */

        // Placeholder
        return <div className="group w-full h-full relative">

            {/* Add Tag Button [EX] */}
            {/* <Button
                variant="ghost"
                className="
                    w-8 h-8
                    opacity-0 group-hover:opacity-100
                    absolute -top-2 -right-2
                ">
                <Plus />

            </Button> */}

            <span className="opacity-50">No Tags</span>
        </div>

    }

    const renderButtonsRow = () => {

        return <div className="grid row-span-3 grid-cols-3 min-w-32 gap-2" data-fit>

            {/* Chart Button */}
            <Button variant="ghost" className="w-8 h-8">
               <ChartArea size={16} />
            </Button>

            {/* Cesium Button */}
            <Button variant="ghost" className="w-8 h-8">
               <Globe2 size={16} />
            </Button>

            {/* Map Button */}
            <Button variant="ghost" className="w-8 h-8">
               <Map size={16} />
            </Button>



            {/* Tags Button */}
            <Button variant="ghost" className="w-8 h-8">
               <Tag size={16} />
            </Button>

            {/* Events Button */}
            <Button variant="ghost" className="w-8 h-8">
               <MapPinned size={16} />
            </Button>

            {/* Download Button */}
            <Button variant="ghost" className="w-8 h-8">
               <Download size={16} />
            </Button>

        </div>

    }

    // return <Card className="w-full flex bg-background">
    return <div className="
        w-full flex min-h-20
        *:rounded-none
    ">
        <FlightRowSection>
            {renderFlightMainDetails()}
        </FlightRowSection>

        <FlightRowSection>
            {renderFlightTimeDetails()}
        </FlightRowSection>

        <FlightRowSection>
            {renderAirportsDetails()}
        </FlightRowSection>

        <FlightRowSection>
            {renderTagsRows()}
        </FlightRowSection>

        <FlightRowSection className="p-2">
            {renderButtonsRow()}
        </FlightRowSection>

    </div>

}