import 'bootstrap';

import React from "react";
import ReactDOM from "react-dom";

import SignedInNavbar from "./signed_in_navbar.js";

const navbarContainer = document.querySelector('#navbar');
const navbarRoot = ReactDOM.createRoot(navbarContainer);
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
const root = ReactDOM.createRoot(container);
root.render(
    <FlightDisplayCard />
);