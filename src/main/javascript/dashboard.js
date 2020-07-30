import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";



/*
var eventStats = [
    {
        airframeId : 0,
        airframe : "Generic", 
        events : [
            {
                id : 0,
                name : "Pitch",
                totalFlights : 9039,
                processedFlights : 7023,
                monthStats : [
                    { 
                        name : "Current Month",
                        totalFlights : 1039,
                        flightsWithEvent : 823,
                        totalEvents : 132,
                        avgDuration : 10.32,

                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    },

                    { 
                        name : "Previous Month",
                        totalFlights : 1387,
                        flightsWithEvent : 753,
                        totalEvents : 192,
                        avgDuration : 5.32,
                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    },

                    { 
                        name : "Last 6 Months",
                        totalFlights : 2072,
                        flightsWithEvent : 1123,
                        totalEvents : 32,
                        avgDuration : 15.12,
                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    },

                    { 
                        name : "All Previous",
                        totalFlights : 23072,
                        flightsWithEvent : 9123,
                        totalEvents : 3322,
                        avgDuration : 25.12,
                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    }

                ]
            },

            {
                id : 1,
                name : "Roll",
                totalFlights : 9039,
                processedFlights : 7023,
                monthStats : [
                    { 
                        name : "Current Month",
                        totalFlights : 1039,
                        flightsWithEvent : 823,
                        totalEvents : 132,
                        avgDuration : 10.32,
                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    },

                    { 
                        name : "Previous Month",
                        totalFlights : 1387,
                        flightsWithEvent : 753,
                        totalEvents : 192,
                        avgDuration : 5.32,
                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    },

                    { 
                        name : "Last 6 Months",
                        totalFlights : 2072,
                        flightsWithEvent : 1123,
                        totalEvents : 32,
                        avgDuration : 15.12,
                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    },

                    { 
                        name : "All Previous",
                        totalFlights : 23072,
                        flightsWithEvent : 9123,
                        totalEvents : 3322,
                        avgDuration : 25.12,
                        aggTotalFlights : 1039,
                        aggFlightsWithEvent : 823,
                        aggTotalEvents : 132,
                        aggAvgDuration : 10.32
                    }

                ]
            }
        ]

    },

    {
        airframe_id : 1,
        airframe : "Cessna 172S", 
        events : [
            {
                id : 0,
                name : "Pitch",
                totalFlights : 9039,
                processedFlights : 5723,
                monthStats : []
            }
        ]


    },

    {
        airframe_id : 1,
        airframe : "PA-28-181", 
        events : [
            {
                id : 0,
                name : "Pitch",
                totalFlights : 9039,
                processedFlights : 5723,
                monthStats : []
            }
        ]

    }

];
*/


class DashboardCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            eventStats : eventStats
        };
    }

    toggleEventInfo(eventInfo) {
        console.log("eventInfo.infoHidden is: " + eventInfo.infoHidden);
        eventInfo.infoHidden = !eventInfo.infoHidden;
        console.log("eventInfo.infoHidden changed to: " + eventInfo.infoHidden);

        this.setState(this.state);
    }


    render() {
        console.log(eventStats);

        return (
            <div>
                <SignedInNavbar activePage="dashboard" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                {
                    this.state.eventStats.map((airframeStats, airframeIndex) => {
                        let marginTop = 4;
                        if (airframeIndex > 0) {
                            marginTop = 14;
                        }
                        return (
                            <div key={airframeIndex} style={{marginTop:marginTop, padding:"0 5 0 5"}}>
                                <div className="card mb-1 m-1" style={{background : "rgba(100,100,100,0.2)", padding:"10 10 10 10"}}>
                                    <h5 style={{marginBottom:0}}> 
                                        {airframeStats.airframeName + " Events"}
                                    </h5>
                                </div>

                                <div className="row" style={{padding:"0 15 0 15"}}>

                                    {
                                        airframeStats.events.map((eventInfo, eventIndex) => {
                                            let processedPercentage = (100.0 * parseFloat(eventInfo.processedFlights) / parseFloat(eventInfo.totalFlights)).toFixed(2);
                                            if (typeof eventInfo.infoHidden == 'undefined') eventInfo.infoHidden = true;

                                            return (
                                                <div className="col-sm-12" key={eventIndex} style={{padding:"0 0 0 0"}}>
                                                    <div className="card mb-1 m-1" style={{background : "rgba(248,259,250,0.8)"}}>
                                                        <h5 className="card-header">
                                                            <div className="d-flex">
                                                                <div style={{flexBasis:"30%", flexShrink:0, flexGrow:0}}>
                                                                    {eventInfo.eventName}
                                                                </div>
                                                                <button type="button" className="btn btn-outline-secondary" style={{padding:"3 8 3 8", marginRight:"5"}} onClick={() => {this.toggleEventInfo(eventInfo)}}>
                                                                    <i className='fa fa-info'></i>
                                                                </button>
                                                                <div className="progress flex-fill" style={{height:"24px", background:"rgba(183,186,199,1.0)"}}>
                                                                    <div className="progress-bar" role="progressbar" style={{width: processedPercentage + "%"}} aria-valuenow={processedPercentage} aria-valuemin="0" aria-valuemax="100"> &nbsp; {eventInfo.processedFlights + " / " + eventInfo.totalFlights + " (" + processedPercentage + "%) flights processed"} </div>
                                                                </div>
                                                            </div>
                                                        </h5>

                                                        <div className="card-body" >
                                                            <p hidden={eventInfo.infoHidden}>
                                                                {eventInfo.humanReadable}
                                                            </p>

                                                            <table style={{width:"100%"}}>
                                                                <thead>
                                                                    <tr>
                                                                        <th></th>
                                                                        <th style={{textAlign:"center", paddingRight:25, borderBottom: "1px solid grey", borderRight: "1px solid grey"}} colSpan="4">Your Fleet</th>
                                                                        <th style={{textAlign:"center", borderBottom: "1px solid grey"}} colSpan="4">Other Fleets</th> 
                                                                    </tr>

                                                                    <tr>
                                                                        <th></th>
                                                                        <th style={{textAlign:"right"}}>Flights </th> 
                                                                        <th style={{textAlign:"right"}}>Total</th> 
                                                                        <th style={{textAlign:"right"}}>Severity</th> 
                                                                        <th style={{textAlign:"right", paddingRight:25, borderRight: "1px solid grey"}}>Duration (s)</th> 
                                                                        <th style={{textAlign:"right"}}>Flights </th> 
                                                                        <th style={{textAlign:"right"}}>Total </th> 
                                                                        <th style={{textAlign:"right"}}>Severity</th> 
                                                                        <th style={{textAlign:"right"}}>Duration (s)</th> 
                                                                    </tr>

                                                                    <tr>
                                                                        <th></th>
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>With Event</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>Events</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>(Min/Avg/Max)</th> 
                                                                        <th style={{textAlign:"right", paddingRight:25, borderBottom: "1px solid grey", borderRight: "1px solid grey"}}>(Min/Avg/Max)</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>With Event</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>Events</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>(Min/Avg/Max)</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>(Min/avg/Max)</th> 
                                                                    </tr>
                                                                </thead>

                                                                <tbody>
                                                                    {
                                                                        eventInfo.monthStats.map((stats, monthIndex) => {
                                                                            let eventPercentage = (100.0 * parseFloat(stats.flightsWithEvent) / parseFloat(stats.flightsWithoutError)).toFixed(2);
                                                                            let flightsWithEventStr = stats.flightsWithEvent + " / " + stats.flightsWithoutError + " (" + eventPercentage + "%)";

                                                                            let aggEventPercentage = (100.0 * parseFloat(stats.aggFlightsWithEvent) / parseFloat(stats.aggFlightsWithoutError)).toFixed(2);
                                                                            let aggFlightsWithEventStr = stats.aggFlightsWithEvent + " / " + stats.aggFlightsWithoutError + " (" + aggEventPercentage + "%)";

                                                                            return (
                                                                                <tr key={monthIndex}>
                                                                                    <td>{stats.rowName}</td>
                                                                                    <td style={{textAlign:"right"}}>{flightsWithEventStr}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.totalEvents}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.minSeverity.toFixed(2) + " / " + stats.avgSeverity.toFixed(2) + " / " + stats.maxSeverity.toFixed(2)}</td>
                                                                                    <td style={{textAlign:"right", paddingRight:25, borderRight: "1px solid grey"}}>{stats.minDuration.toFixed(2) + " / " + stats.avgDuration.toFixed(2) + " / " + stats.maxDuration.toFixed(2)}</td>
                                                                                    <td style={{textAlign:"right"}}>{aggFlightsWithEventStr}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.aggTotalEvents}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.aggMinSeverity.toFixed(2) + " / " + stats.aggAvgSeverity.toFixed(2) + " / " + stats.aggMaxSeverity.toFixed(2)}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.aggMinDuration.toFixed(2) + " / " + stats.aggAvgDuration.toFixed(2) + " / " + stats.aggMaxDuration.toFixed(2)}</td>
                                                                                </tr>
                                                                            );
                                                                        })
                                                                    }
                                                                </tbody>

                                                            </table>

                                                        </div>
                                                    </div>
                                                </div>
                                            );
                                        })
                                    }

                                </div>


                            </div>
                        );
                    })
                }

            </div>
        );
    }
}

var profilePage = ReactDOM.render(
    <DashboardCard />,
    document.querySelector('#dashboard-page')
);
