// ngafid-frontend/src/app/pages/protected/flights/_flight_row/flight_row.tsx

import { Button } from "@/components/ui/button";
import { useFlights } from "@/pages/protected/flights/flights";
import { Check, FolderSearch, Plane } from "lucide-react";
import React, { forwardRef } from "react";
import type { JSX } from "react/jsx-runtime";


type Props = {
    flightID: number;
    renderDetailItem: (value: string | number, fallback: string) => JSX.Element;
} & React.ComponentPropsWithoutRef<typeof Button>;


const FlightRowFlightIDButton = forwardRef<HTMLButtonElement, Props>(({ flightID, renderDetailItem, className, onClick, ...rest }, ref) => {

    const { addFlightIDToFilter, flightIDInSpecialGroup } = useFlights();
    const flightIDString = flightID.toString();

    return (

        <Button
            ref={ref}
            variant="link"
            className={`p-0 m-0 w-fit h-6 font-bold group ${className ?? ""}`}
            onClick={(e) => {
                onClick?.(e);
                addFlightIDToFilter(flightIDString);
            }}
            {...rest}
        >

            {/* Not Hovering -> Plane Icon */}
            <Plane size={16} className="inline group-hover:hidden!" />

            {/* Hovering... */}
            {
                // ...In Special Group -> Check Icon
                flightIDInSpecialGroup(flightIDString)
                ?
                <Check size={16} className="hidden! group-hover:inline!" />
            
                // ...Otherwise -> Folder Search Icon
                :
                <FolderSearch size={16} className="hidden! group-hover:inline!" />
            }

            {renderDetailItem(flightID, "N/A")}

        </Button>
    );

});

FlightRowFlightIDButton.displayName = "FlightRowFlightIDButton";

export default FlightRowFlightIDButton;