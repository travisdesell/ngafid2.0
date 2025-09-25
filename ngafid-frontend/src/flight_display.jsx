import 'bootstrap';

import React from "react";
import { createRoot } from 'react-dom/client';


import SignedInNavbar from "./signed_in_navbar";

const navbarContainer = document.querySelector('#navbar');
const navbarRoot = createRoot(navbarContainer);
navbarRoot.render(
    <SignedInNavbar activePage="flight_display" />
);



class FlightDisplayCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
        };
    }

    render() {
        return (
            <div className="card">
                <h4>HELLO WORLD!</h4>
            </div>
        );
    }
}



const container = document.querySelector("#flight-display-page");
const root = createRoot(container);
root.render(
    <FlightDisplayCard />
);