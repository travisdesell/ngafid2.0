import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.jsx";
import SignedInNavbar from "./signed_in_navbar.jsx";

var navbar = ReactDOM.render(
    <SignedInNavbar activePage="flight_display" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
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

var flightDisplayCard = ReactDOM.render(
    <FlightDisplayCard />,
    document.querySelector('#flight-display-card')
);
