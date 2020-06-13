import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";

import Plotly from 'plotly.js';


class Notifications extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            notifications : [
                { count : waitingUserCount, message : "users waiting for access", badgeType : "badge-info" },
                { count : unconfirmedTailsCount, message : "tail numbers need to be confirmed", badgeType : "badge-info" },
                { count : uploadsNotImported, message : "uploads waiting to be imported", badgeType : "badge-warning" },
                { count : uploadsWithError, message : "uploads with processing errors", badgeType : "badge-danger" },
                { count : flightsWithWarning, message : "flights with import warnings", badgeType: "badge-warning" },
                { count : flightsWithError, message : "flights with import errors", badgeType : "badge-danger" }
            ]
        };
    }


    render() {
        return (
            <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)", height:"258px"}}>
                <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>Notifications</h4>
                <div className="card-body">
                    <table>
                        <tbody>
                            {
                                this.state.notifications.map((info, index) => {
                                    if (info.count == 0) {
                                        return "";
                                    } else {
                                        return (
                                            <tr key={index}>
                                                <td style={{textAlign:"right", paddingBottom:"6"}}><span className={'badge ' + info.badgeType}>{Number(info.count).toLocaleString('en')}</span></td>
                                                <td style={{paddingBottom:"6"}}>&nbsp;{info.message}</td>
                                            </tr>
                                        );
                                    }
                                })
                            }
                       </tbody>
                    </table>
                </div>
            </div>
        );
    }
}


class WelcomeCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
        };
    }

    render() {
        //console.log(systemIds);

        const numberOptions = { 
            minimumFractionDigits: 2,
            maximumFractionDigits: 2 
        };


        return (
            <div className="container-fluid">
                <div className="row">
                    <div className="col-lg-6" style={{paddingRight:"0"}}>
                        <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                            <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>Your Fleet</h4>
                            <div className="card-body">
                                <div className="row">
                                    <div className = "col-sm-4">
                                        <h3>{Number(flightHours / (60 * 60)).toLocaleString('en', numberOptions)}</h3> Flight Hours <br></br>
                                    </div>

                                    <div className = "col-sm-4">
                                        <h3>{Number(numberFlights).toLocaleString('en')}</h3> Flights <br></br>
                                    </div>

                                    <div className = "col-sm-4">
                                        <h3>{Number(numberAircraft).toLocaleString('en')}</h3> Aircraft <br></br>
                                    </div>
                                </div>

                                <hr></hr>

                                <div className="row">
                                    <div className = "col-sm-4">
                                        <h3>{Number(totalExceedences).toLocaleString('en')}</h3> Total Exceedences<br></br>
                                    </div>

                                    <div className = "col-sm-4">
                                        <h3>{Number(yearExceedences).toLocaleString('en')}</h3> Exceedences This Year<br></br>
                                    </div>

                                    <div className = "col-sm-4">
                                        <h3>{Number(monthExceedences).toLocaleString('en')}</h3> Exceedences This Month<br></br>
                                    </div>

                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="col-lg-6" style={{paddingLeft:"0"}}>
                        <Notifications />
                   </div>
                </div>

                <div className="row">
                    <div className="col-lg-12">
                        <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                            <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>Exceedences</h4>
                            <div className="card-body" style={{padding:"0"}}>
                                <div id="exceedences-plot" style={{width:"100%"}}></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

var profileCard = ReactDOM.render(
    <WelcomeCard />,
    document.querySelector('#welcome-card')
);

/*
var y = [];
for (var i = 0; i < 500; i ++) {
    y[i] = Math.random();
}

var z = [];
for (var i = 0; i < 500; i ++) {
    z[i] = Math.random();
}

var data = [
    {
        y: y,
        type: 'histogram',
        marker: {
            color: 'orange',
        },
    },
    {
        y: z,
        type: 'histogram',
        marker: {
            color: 'blue',
        },
    }
];
*/

var data = [
    {
        type: 'bar',
        name: 'Bronx Zoo',
        x: [20, 14, 23],
        y: ['giraffes', 'orangutans', 'monkeys'],
        orientation: 'h'
    },
    {
        type: 'bar',
        name: 'Rochester Zoo',
        x: [25, 11, 18],
        y: ['giraffes', 'orangutans', 'monkeys'],
        orientation: 'h'
    }
];


Plotly.newPlot('exceedences-plot', data);

