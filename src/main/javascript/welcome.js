import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import  TimeHeader from "./time_header.js";

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
                                        return;
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

        var date = new Date();
        this.state = {
            startDate : "2000-01-01",
            endDate :  date.getFullYear() + "-" + (date.getMonth() + 1) + "-" + date.getDate()
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
                                        <h3>{Number(totalEvents).toLocaleString('en')}</h3> Total Events<br></br>
                                    </div>

                                    <div className = "col-sm-4">
                                        <h3>{Number(yearEvents).toLocaleString('en')}</h3> Events This Year<br></br>
                                    </div>

                                    <div className = "col-sm-4">
                                        <h3>{Number(monthEvents).toLocaleString('en')}</h3> Events This Month<br></br>
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
                            {this.state.timeHeader}

                            <div className="card-body" style={{padding:"0"}}>
                                <div className="row" style={{margin:"0"}}>
                                    <div className="col-lg-6" style={{padding:"0 8 0 0"}}>
                                        <div id="event-counts-plot"></div>
                                    </div>
                                    <div className="col-lg-6" style={{padding:"0 0 0 8"}}>
                                        <div id="event-percents-plot"></div>
                                    </div>
                                </div>
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

/*
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
*/


//console.log(eventCounts);

var countData = [];
var percentData = [];

for (let [key, value] of Object.entries(eventCounts)) {
    console.log(key);
    console.log(value);

    value.name = value.airframeName;
    value.y = value.names;
    value.type = 'bar';
    value.orientation = 'h';
    //value.hoverinfo = 'text';
    if (value.name === "all" || value.name === "fleet") {
        if (value.name === "all") value.name = "NGAFID Aggregate";
        if (value.name === "fleet") value.name = "Your Fleet";

        percentData.push(value);
        value.x = [];

        value.hoverinfo = 'y+text';
        value.hovertext = [];

        for (let i = 0; i < value.totalFlightsCounts.length; i++) {
            value.x.push( 100.0 * parseFloat(value.flightsWithEventCounts[i]) / parseFloat(value.totalFlightsCounts[i]) );

            console.log(value.x[i]);
            var fixedText = "";
            if (value.x[i] > 0 && value.x[i] < 1) {
                console.log("Log10 of x is " + Math.log10(value.x[i]));
                fixedText = value.x[i].toFixed(-Math.ceil(Math.log10(value.x[i])) + 2) + "%"
            } else {
                fixedText = value.x[i].toFixed(2) + "%";
            }
            value.hovertext.push(fixedText);

            console.log("converted to " + fixedText);
        }
    } else {
        countData.push(value);
        value.x = value.totalEventsCounts;
    }
}

var countLayout = {
    title : 'Event Counts',
    barmode: 'stack',
    //autosize: false,
    //width: 500,
    //height: 500,
      margin: {
              l: 250,
              r: 50,
              b: 50,
              t: 50,
              pad: 4
            }
};

var percentLayout = {
    title : 'Percentage of Flights With Event',
    //autosize: false,
    //width: 500,
    //height: 500,
      margin: {
              l: 250,
              r: 50,
              b: 50,
              t: 50,
              pad: 4
            }
};



import Plotly from 'plotly.js';

var config = {responsive: true}

Plotly.newPlot('event-counts-plot', countData, countLayout, config);
Plotly.newPlot('event-percents-plot', percentData, percentLayout, config);


var navbar = ReactDOM.render(
    <SignedInNavbar activePage={"welcome"} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);

