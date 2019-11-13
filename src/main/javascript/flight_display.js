import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";



class FlightDisplayCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
        };
    }

    render() {
        return (
            <div class="card">
                <h4>HELLO WORLD!</h4>
            </div>
        );
    }
}

var flightDisplayCard = ReactDOM.render(
    <FlightDisplayCard />,
    document.querySelector('#flight-display-card')
);
