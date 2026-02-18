import React from "react";
import { createRoot } from 'react-dom/client';

import './index.css'; //<-- include Tailwind

const REDIRECT_DELAY_MS = 10_000;
const REDIRECT_DELAY_S = (REDIRECT_DELAY_MS / 1000).toFixed(0);
const LOCALSTORAGE_KEY = "ngafid-flight-doRedirectDelay";

class FlightPage extends React.Component {

    render() {

        return (
            <div className="p-2 w-100 h-100" style={{ backgroundColor: "black", color: "white" }}>
                The Flight page has been deprecated.
                <br />
                <div className="flex gap-2 items-center mt-2">
                    <i className="fa fa-spinner fa-spin"></i>
                    Redirecting to the Flights page after {REDIRECT_DELAY_S} seconds...
                </div>
                <br />
                <span className="opacity-50">(This message will only appear once on this browser)</span>
            </div>
        );

    }

}


const doRedirectDelay = localStorage.getItem(LOCALSTORAGE_KEY) ?? "true";

//Only render the FlightPage component if the redirect delay is enabled
if (doRedirectDelay === "true") {

    console.log("FlightPage component loaded...");

    const container = document.querySelector("#flight-page");
    const root = createRoot(container);
    root.render(<FlightPage/>);

    console.log("Rendered FlightPage...");

}


/*
    The flight.js page is now obsolete.

    Immediately redirect to the flights.js
    page instead.

    Additionally, a filter corresponding to the
    flight_id(s) will be appended to the redirect
    URL.

    e.g.
    ngafid.org/protected/flight?flight_id=80&flight_id=90
    localhost:8181/protected/flight?flight_id=80&flight_id=90
*/


(function () {

    console.log("Redirecting to flights.js...");

    const REDIRECT_URL_BASE = "/protected/flights";

    //Fetch the flight_id(s) from the URL
    const params = new URLSearchParams(window.location.search);
    const flightIds = params.getAll("flight_id").filter(id => id !== "");

    let redirectURL = REDIRECT_URL_BASE;

    //Got flight IDs, translate to the new filter format
    if (flightIds.length) {

        const filterObj = {
            type: "GROUP",
            condition: "AND",
            filters: [
                {
                    type: "GROUP",
                    condition: "OR",
                    isFlightIdGroup: true,
                    filters: flightIds.map(id => ({
                        type: "RULE",
                        inputs: ["Flight ID", "=", id.toString()]
                    }))
                }
            ]
        };

        //Encode and append to the redirect URL
        const encoded = encodeURIComponent(JSON.stringify(filterObj));
        redirectURL += `?filter=${encoded}`;

    }

    const redirectDelayCurrentMS = (doRedirectDelay==="true")
        ? REDIRECT_DELAY_MS
        : 0;

    //Short delay before redirecting
    setTimeout(() => {

        console.log(`Redirecting to: ${  redirectURL}`);

        //Disable the redirect delay
        localStorage.setItem(LOCALSTORAGE_KEY, "false");
            
        //Redirect to the new URL
        window.location.replace(redirectURL);

    }, redirectDelayCurrentMS);

})();