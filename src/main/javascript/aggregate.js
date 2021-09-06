import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import  TimeHeader from "./time_header.js";

import Plotly from 'plotly.js';


airframes.unshift("All Airframes");
var index = airframes.indexOf("Garmin Flight Display");
if (index !== -1) airframes.splice(index, 1);

console.log(eventCounts);

function displayPlots(selectedAirframe) {
    console.log("displaying plots with airframe: '" + selectedAirframe + "'");

    var countData = [];
    var percentData = [];

    var fleetPercents = {
        name : 'All Fleets',
        type : 'bar',
        orientation : 'h',
        hoverinfo : 'y+text',
        hovertext : [],
        y : [],
        x : [],
        flightsWithEventCounts : [],
        totalFlightsCounts : []
    }


    for (let [key, value] of Object.entries(eventCounts)) {
        if (value.airframeName === "Garmin Flight Display") continue;
        if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes") continue;

        //console.log(key);
        //console.log(value);

        value.name = value.airframeName;
        value.y = value.names;
        value.type = 'bar';
        value.orientation = 'h';
        //value.hoverinfo = 'text';

        //don't add airframes to the count plot that the fleet doesn't have
        if (airframes.indexOf(value.airframeName) >= 0) countData.push(value);
        value.x = value.totalEventsCounts;

        for (let i = 0; i < value.names.length; i++) {
            //don't add airframes to the fleet percentage plot that the fleet doesn't have
            if (airframes.indexOf(value.airframeName) >= 0) {
                var index = fleetPercents.y.indexOf(value.names[i]);
                if (index !== -1) {
                    fleetPercents.flightsWithEventCounts[index] += value.flightsWithEventCounts[i];
                    fleetPercents.totalFlightsCounts[index] += value.totalFlightsCounts[i];
                } else {
                    let pos = fleetPercents.y.length;
                    fleetPercents.y.push(value.names[i]);
                    fleetPercents.flightsWithEventCounts[pos] = value.flightsWithEventCounts[i];
                    fleetPercents.totalFlightsCounts[pos] = value.totalFlightsCounts[i];
                }
            }
        }
    }

    percentData.push(fleetPercents);

    console.log(fleetPercents);

    for (let j = 0; j < percentData.length; j++) {
        let value = percentData[j];
        value.x = [];

        for (let i = 0; i < value.totalFlightsCounts.length; i++) {
            value.x.push( 100.0 * parseFloat(value.flightsWithEventCounts[i]) / parseFloat(value.totalFlightsCounts[i]) );

            //console.log(value.x[i]);
            var fixedText = "";
            if (value.x[i] > 0 && value.x[i] < 1) {
                //console.log("Log10 of x is " + Math.log10(value.x[i]));
                fixedText = value.x[i].toFixed(-Math.ceil(Math.log10(value.x[i])) + 2) + "%"
            } else {
                fixedText = value.x[i].toFixed(2) + "%";
            }
            value.hovertext.push(fixedText);

            //console.log("converted to " + fixedText);
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


    var config = {responsive: true}

    Plotly.newPlot('event-counts-plot', countData, countLayout, config);
    Plotly.newPlot('event-percents-plot', percentData, percentLayout, config);
}




class AggregatePage extends React.Component {
    constructor(props) {
        super(props);

        var date = new Date();
        this.state = {
            airframe : "All Airframes",
            startYear : 2020,
            startMonth : 1,
            endYear : date.getFullYear(),
            endMonth : date.getMonth() + 1,
            datesChanged : false
        };
    }

    updateStartYear(newStartYear) {
        console.log("setting new start year to: " + newStartYear);
        this.setState({startYear : newStartYear, datesChanged : true});
        console.log(this.state);
    }

    updateStartMonth(newStartMonth) {
        console.log("setting new start month to: " + newStartMonth);
        this.setState({startMonth : newStartMonth, datesChanged : true});
        console.log(this.state);
    }

    updateEndYear(newEndYear) {
        console.log("setting new end year to: " + newEndYear);
        this.setState({endYear : newEndYear, datesChanged : true});
        console.log(this.state);
    }

    updateEndMonth(newEndMonth) {
        console.log("setting new end month to: " + newEndMonth);
        this.setState({endMonth : newEndMonth, datesChanged : true});
        console.log(this.state);
    }

    dateChange() {
        console.log("[aggregatecard] notifying date change 2, startYear: '" + this.state.startYear + "', startMonth: '" + this.state.startMonth + ", endYear: '" + this.state.endYear + "', endMonth: '" + this.state.endMonth + "'"); 

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

        $('#loading').show();
        console.log("showing loading spinner!");

        let aggregatePage = this;

        $.ajax({
            type: 'POST',
            url: '/protected/all_event_counts',
            data : submission_data,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                $('#loading').hide();

                if (response.err_msg) {
                    errorModal.show(response.err_title, response.err_msg);
                    return;
                }   

                eventCounts = response;
                displayPlots(aggregatePage.state.airframe);
                aggregatePage.setState({datesChanged : false});
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            },   
            async: true 
        });  
    }

    airframeChange(airframe) {
        this.setState({airframe});
        displayPlots(airframe);
    }

    render() {
        //console.log(systemIds);

        const numberOptions = { 
            minimumFractionDigits: 2,
            maximumFractionDigits: 2 
        };

        return (
            <div>
                <SignedInNavbar activePage={"aggregate"} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="container-fluid">
                    <div className="row">
                        <div className="col-lg-6" style={{paddingRight:"0"}}>
                            <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                                <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>All Fleets</h4>
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
                            <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                                <h4 className="card-header" style={{color : "rgba(75,75,75,250)"}}>Participation</h4>
                                <div className="card-body">
                                    <div className="row">
                                        <div className = "col-sm-4">
                                            <h3>{numberFleets}</h3> Fleets <br></br>
                                        </div>
                                    </div>

                                    <hr></hr>

                                    <div className="row">
                                        <div className = "col-sm-4">
                                            <h3>{numberUsers}</h3> Users<br></br>
                                        </div>
                                    </div>
                                </div>
                            </div>
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


var aggregatePage = ReactDOM.render(
    <AggregatePage/>,
    document.querySelector('#aggregate-page')
);

displayPlots("All Airframes");


