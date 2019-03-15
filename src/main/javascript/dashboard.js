import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";

var eventStats = [
    {
        airframeId : 0,
        airframe : "Generic", /*generic events*/
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


class DashboardCard extends React.Component {
    constructor(props) {
        super(props);

    }
    render() {
        return (
            <div>
                {
                    eventStats.map((airframeStats, airframeIndex) => {
                        let marginTop = 4;
                        if (airframeIndex > 0) {
                            marginTop = 14;
                        }
                        return (
                            <div key={airframeIndex} style={{marginTop:marginTop, padding:"0 5 0 5"}}>
                                <div className="card mb-1 m-1" style={{background : "rgba(100,100,100,0.2)", padding:"10 10 10 10"}}>
                                    <h5 style={{marginBottom:0}}> 
                                        {airframeStats.airframe + " Events"}
                                    </h5>
                                </div>

                                <div className="row" style={{padding:"0 15 0 15"}}>

                                    {
                                        airframeStats.events.map((eventInfo, eventIndex) => {
                                            let processedPercentage = (100.0 * parseFloat(eventInfo.processedFlights) / parseFloat(eventInfo.totalFlights)).toFixed(2);

                                            return (
                                                <div className="col-sm-12" key={eventIndex} style={{padding:"0 0 0 0"}}>
                                                    <div className="card mb-1 m-1" style={{background : "rgba(248,259,250,0.8)"}}>
                                                        <h5 className="card-header">
                                                            <div className="d-flex">
                                                                <div style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>
                                                                    {eventInfo.name}
                                                                </div>
                                                                <div className="progress flex-fill" style={{margin:"4 0 4 0"}}>
                                                                    <div className="progress-bar" role="progressbar" style={{width: processedPercentage + "%"}} aria-valuenow={processedPercentage} aria-valuemin="0" aria-valuemax="100"> {eventInfo.processedFlights + " / " + eventInfo.totalFlights + " (" + processedPercentage + "%) flights processed"} </div>
                                                                </div>
                                                            </div>
                                                        </h5>

                                                        <div className="card-body" >

                                                            <table style={{width:"100%"}}>
                                                                <thead>
                                                                    <tr>
                                                                        <th></th>
                                                                        <th style={{textAlign:"center", paddingRight:25}} colSpan="3">Your Fleet</th>
                                                                        <th style={{textAlign:"center"}} colSpan="3">Other Fleets</th> 
                                                                    </tr>

                                                                    <tr>
                                                                        <th></th>
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>Flights With Event</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>Total Events</th> 
                                                                        <th style={{textAlign:"right", paddingRight:25, borderBottom: "1px solid grey", borderRight: "1px solid grey"}}>Avg. Duration</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>Flights With Event</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>Total Events</th> 
                                                                        <th style={{textAlign:"right", borderBottom: "1px solid grey"}}>Avg. Duration</th> 
                                                                    </tr>
                                                                </thead>

                                                                <tbody>
                                                                    {
                                                                        eventInfo.monthStats.map((stats, monthIndex) => {
                                                                            let eventPercentage = (100.0 * parseFloat(stats.flightsWithEvent) / parseFloat(stats.totalFlights)).toFixed(2);
                                                                            let flightsWithEventStr = stats.flightsWithEvent + " / " + stats.totalFlights + " (" + eventPercentage + "%)";

                                                                            let aggEventPercentage = (100.0 * parseFloat(stats.aggFlightsWithEvent) / parseFloat(stats.aggTotalFlights)).toFixed(2);
                                                                            let aggFlightsWithEventStr = stats.aggFlightsWithEvent + " / " + stats.aggTotalFlights + " (" + aggEventPercentage + "%)";

                                                                            return (
                                                                                <tr key={monthIndex}>
                                                                                    <td>{stats.name}</td>
                                                                                    <td style={{textAlign:"right"}}>{flightsWithEventStr}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.totalEvents}</td>
                                                                                    <td style={{textAlign:"right", paddingRight:25, borderRight: "1px solid grey"}}>{stats.avgDuration}</td>
                                                                                    <td style={{textAlign:"right"}}>{aggFlightsWithEventStr}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.aggTotalEvents}</td>
                                                                                    <td style={{textAlign:"right"}}>{stats.aggAvgDuration}</td>
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

var profileCard = ReactDOM.render(
    <DashboardCard />,
    document.querySelector('#dashboard-card')
);
