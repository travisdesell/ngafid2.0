import "bootstrap";

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import  TimeHeader from "./time_header.js";

import Plotly from "plotly.js";


airframes.unshift("All Airframes");
var index = airframes.indexOf("Garmin Flight Display");
if (index !== -1) airframes.splice(index, 1);




let STATISTICS_ROUTE = "/protected/statistics/aggregate";
let statisticNameMap = {
    "summaryStatistics": "summary",
    "eventCounts": "event_counts"
};

let statisticsDefault = {
    numberFlights: "",
    flightTime: "",
    numberAircraft: "",
    yearNumberFlights: "",
    monthNumberFlights: "",
    monthFlightTime: "",
    totalEvents: "",
    yearEvents: "",
    monthEvents: "",
    airframes: "",
    numberFleets: "",
    numberUsers: "",
};

const LOADING_STRING = "...";

const floatOptions = { 
    minimumFractionDigits: 2,
    maximumFractionDigits: 2 
};

const integerOptions = {};

function formatNumberAsync(value, formattingOptions) {
    if (value || typeof value === "number") {
        return Number(value).toLocaleString("en", formattingOptions);
    } else {
        return LOADING_STRING;
    }
}

function formatDurationAsync(seconds) {
    if (seconds || typeof seconds == "number") {
        new Date(seconds * 1000).toLocaleTimeString("en-GB", {
            timeZone:"Etc/UTC",
            hour12: false,
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit"
        });
    } else {
        return LOADING_STRING;
    }
}

export default class SummaryPage extends React.Component {
    constructor(props) {
        super(props);

        var date = new Date();
        this.state = {
            airframe : "All Airframes",
            startYear : date.getFullYear(),
            startMonth : 1,
            endYear : date.getFullYear(),
            endMonth : date.getMonth() + 1,
            datesChanged : false,
            statistics: statisticsDefault,
            eventCounts: {}
        };
        
        this.dateChange();
        this.fetchStatistics();
    }
    
    displayPlots(selectedAirframe) {
        var countData = [];
        var percentData = [];
    
        var fleetPercents = {
            name : "All Fleets",
            type : "bar",
            orientation : "h",
            hoverinfo : "y+text",
            hovertext : [],
            y : [],
            x : [],
            aggregateFlightsWithEventCounts : [],
            aggregateTotalFlightsCounts : []
        }
    
    
        for (let [key, value] of Object.entries(this.state.eventCounts)) {
            if (value.airframeName === "Garmin Flight Display") continue;
            if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes") continue;
    
            value.name = value.airframeName;
            value.y = value.names;
            value.type = "bar";
            value.orientation = "h";
            //value.hoverinfo = "text";
    
            //don"t add airframes to the count plot that the fleet doesn"t have
            if (airframes.indexOf(value.airframeName) >= 0) countData.push(value);
            value.x = value.aggregateTotalEventsCounts;
    
            for (let i = 0; i < value.names.length; i++) {
                //don"t add airframes to the fleet percentage plot that the fleet doesn"t have
                if (airframes.indexOf(value.airframeName) >= 0) {
                    var index = fleetPercents.y.indexOf(value.names[i]);
                    if (index !== -1) {
                        fleetPercents.aggregateFlightsWithEventCounts[index] += value.aggregateFlightsWithEventCounts[i];
                        fleetPercents.aggregateTotalFlightsCounts[index] += value.aggregateTotalFlightsCounts[i];
                    } else {
                        let pos = fleetPercents.y.length;
                        fleetPercents.y.push(value.names[i]);
                        fleetPercents.aggregateFlightsWithEventCounts[pos] = value.aggregateFlightsWithEventCounts[i];
                        fleetPercents.aggregateTotalFlightsCounts[pos] = value.aggregateTotalFlightsCounts[i];
                    }
                }
            }
        }
    
        percentData.push(fleetPercents);
    
        for (let j = 0; j < percentData.length; j++) {
            let value = percentData[j];
            value.x = [];
    
            for (let i = 0; i < value.aggregateTotalFlightsCounts.length; i++) {
                value.x.push( 100.0 * parseFloat(value.aggregateFlightsWithEventCounts[i]) / parseFloat(value.aggregateTotalFlightsCounts[i]) );
    
                var fixedText = "";
                if (value.x[i] > 0 && value.x[i] < 1) {
                    fixedText = value.x[i].toFixed(-Math.ceil(Math.log10(value.x[i])) + 2) + "%"
                } else {
                    fixedText = value.x[i].toFixed(2) + "%";
                }
                value.hovertext.push(fixedText);
    
            }
        }
    
        var countLayout = {
            title : "Event Counts",
            barmode: "stack",
            //autosize: false,
            //width: 500,
            height: 750,
            margin: {
                l: 250,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            }
        };
    
        var percentLayout = {
            title : "Percentage of Flights With Event",
            //autosize: false,
            //width: 500,
            height: 750,
            margin: {
                l: 250,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            }
        };
    
    
        var config = {responsive: true}
    
        Plotly.newPlot("event-counts-plot", countData, countLayout, config);
        Plotly.newPlot("event-percents-plot", percentData, percentLayout, config);
    }
    updateStartYear(newStartYear) {
        this.setState({startYear : newStartYear, datesChanged : true});
    }

    updateStartMonth(newStartMonth) {
        this.setState({startMonth : newStartMonth, datesChanged : true});
    }

    updateEndYear(newEndYear) {
        this.setState({endYear : newEndYear, datesChanged : true});
    }

    updateEndMonth(newEndMonth) {
        this.setState({endMonth : newEndMonth, datesChanged : true});
    }

    dateChange() {
        let startDate = this.state.startYear + "-";
        let endDate = this.state.endYear + "-";

        //0 pad the months on the front
        if (parseInt(this.state.startMonth) < 10) startDate += "0" + parseInt(this.state.startMonth);
        else startDate += this.state.startMonth;
        if (parseInt(this.state.endMonth) < 10) endDate += "0" + parseInt(this.state.endMonth);
        else endDate += this.state.endMonth;

        var submission_data = {
            startDate : startDate + "-01",
            endDate : endDate + "-28"
        };

        $("#loading").show();

        let page = this;

        let route;
        if (this.props.aggregate)
            route = "/protected/all_event_counts";
        else
            route = "/protected/event_counts";

        $.ajax({
            type: "POST",
            url: route,
            data : submission_data,
            dataType: "json",
            success: function(response) {
                $("#loading").hide();

                if (response.err_msg) {
                    errorModal.show(response.err_title, response.err_msg);
                    return;
                }   

                page.state.eventCounts = response;
                console.log("Got response:")
                console.log(response);
                page.displayPlots(page.state.airframe);
                page.setState({datesChanged : false});
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            },   
            async: true 
        });  
    }
    
    fetchStatistics() {
        let page = this;

        let route;
        if (this.props.aggregate)
            route = "/protected/statistics/aggregate/summary";
        else
            route = "/protected/statistics/summary";

        $.ajax({
            type: "POST",
            url: route,
            dataType: "json",
            success: function(response) {
                if (response.err_msg) {
                    errorModal.show(response.err_title, response.err_msg);
                    return;
                }   

                console.log("Got response");
                console.log(response);
                page.setState({statistics: response});
            }
        });
    }

    airframeChange(airframe) {
        this.setState({airframe});
        this.displayPlots(airframe);
    }

    FlightSummary() {
        return (
            <div className="col-lg-6" style={{paddingRight:"0"}}>
                <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                    <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>All Fleets</h4>
                    <div className="card-body">
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatDurationAsync(this.state.statistics.flightTime)}</h3> Flight Hours <br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.numberFlights, integerOptions)}</h3> Flights <br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.numberAircraft, integerOptions)}</h3> Aircraft <br></br>
                            </div>
                        </div>

                        <hr></hr>
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatDurationAsync(this.state.statistics.yearFlightTime)}</h3> Flight Hours This Year<br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.yearNumberFlights, integerOptions)}</h3> Flights This Year<br></br>
                            </div>
                        </div>

                        <hr></hr>
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatDurationAsync(this.state.statistics.monthFlightTime, integerOptions)}</h3> Flight Hours (Last 30 Days)<br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.monthNumberFlights, integerOptions)}</h3> Flights (Last 30 Days)<br></br>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    EventSummary() {
        return (
            <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>Events</h4>
                <div className="card-body">
                    <div className="row">
                        <div className = "col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.totalEvents, integerOptions)}</h3> Total Events<br></br>
                        </div>

                        <div className = "col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.yearEvents, integerOptions)}</h3> Events This Year<br></br>
                        </div>

                        <div className = "col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.monthEvents, integerOptions)}</h3> Events This Month<br></br>
                        </div>

                    </div>
                </div>
            </div>
        );
    }

    ParticipationSummary() {
        return (
            <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>Participation</h4>
                <div className="card-body">
                    <div className="row">
                        <div className = "col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.numberFleets, integerOptions)}</h3> Fleets <br></br>
                        </div>
            
                        <div className = "col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.numberUsers, integerOptions)}</h3> Users<br></br>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    render() {
        return (
            <div>
                <SignedInNavbar activePage={"aggregate"} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="container-fluid">
                    <div className="row">
                        {this.FlightSummary()}
                        <div className="col-lg-6" style={{paddingLeft:"0"}}>
                            {this.EventSummary()}
                            {this.props.aggregate && this.ParticipationSummary()}
                        </div>
                    </div>

                    <div className="row">
                        <div className="col-lg-12">
                            <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                                <TimeHeader
                                    name="Events"
                                    airframes={airframes}
                                    airframe={this.state.airframe}
                                    startYear={this.state.startYear} 
                                    startMonth={this.state.startMonth} 
                                    endYear={this.state.endYear} 
                                    endMonth={this.state.endMonth} 
                                    datesChanged={this.state.datesChanged}
                                    dateChange={() => this.dateChange()}
                                    airframeChange={(airframe) => this.airframeChange(airframe)}
                                    updateStartYear={(newStartYear) => this.updateStartYear(newStartYear)}
                                    updateStartMonth={(newStartMonth) => this.updateStartMonth(newStartMonth)}
                                    updateEndYear={(newEndYear) => this.updateEndYear(newEndYear)}
                                    updateEndMonth={(newEndMonth) => this.updateEndMonth(newEndMonth)}
                                />

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
        </div>
        );
    }
}

export { SummaryPage }
