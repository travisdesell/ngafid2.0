import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";

import Plotly from 'plotly.js';

class WelcomeCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
        };
    }

    render() {
        //console.log(systemIds);

        return (
            <div className="container-fluid">
                <div className="row">
                    <h2>Welcome!</h2>
                    <div className="col-lg-6" style={{paddingRight:"0"}}>
                    </div>
                    <div className="col-lg-6" style={{paddingLeft:"0"}}>
                    </div>
                </div>
                <div className="row">
                    <div id="exceedences-plot" style={{width:"100%"}}></div>
                </div>
            </div>
        );
    }
}

var profileCard = ReactDOM.render(
    <WelcomeCard />,
    document.querySelector('#welcome-card')
);

var data = [{
    type: 'bar',
    x: [20, 14, 23],
    y: ['giraffes', 'orangutans', 'monkeys'],
    orientation: 'h'
}];

Plotly.newPlot('exceedences-plot', data);

