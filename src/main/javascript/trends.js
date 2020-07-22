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

eventNames.sort();

console.log(eventNames);

/*
var trace1 = {
    name: 'test1',
    x: [1, 2, 3, 4],
    y: [10, 15, 13, 17],
    type: 'scatter'
};

var trace2 = {
    name: 'test2',
    x: [1, 2, 3, 4],
    y: [16, 5, 11, 9],
    type: 'scatter'
};
*/

var countData = [];
var percentData = [];

var eventCounts = {};

var eventFleetPercents = {};
var eventNGAFIDPercents = {};

class TrendsCard extends React.Component {
    constructor(props) {
        super(props);

        let eventChecked = {};
        for (let i = 0; i < eventNames.length; i++) {
            eventChecked[eventNames[i]] = false;
        }

        var date = new Date();
        this.state = {
            airframe : "All Airframes",
            startYear : 2000,
            startMonth : 1,
            endYear : date.getFullYear(),
            endMonth : date.getMonth() + 1,
            datesChanged : false,

            eventChecked : eventChecked
        };
    }

    displayPlots(selectedAirframe) {
        console.log("displaying plots with airframe: '" + selectedAirframe + "'");

        eventFleetPercents = {};
        eventNGAFIDPercents = {};

        countData = [];
        percentData = [];

        for (let [eventName, countsObject] of Object.entries(eventCounts)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            var fleetPercents = null;
            var ngafidPercents = null;

            if (eventName in eventFleetPercents) {
                fleetPercents = eventFleetPercents[eventName];
                ngafidPercents = eventNGAFIDPercents[eventName];

            } else {
                fleetPercents = { 
                    name : eventName + ' - Your Fleet',
                    type : 'scatter',
                    hoverinfo : 'x+text',
                    hovertext : [], 
                    y : [], 
                    x : [], 
                    flightsWithEventCounts : {},
                    totalFlightsCounts : {}
                }   

                ngafidPercents = { 
                    name : eventName + ' - All Other Fleets',
                    type : 'scatter',
                    hoverinfo : 'x+text',
                    hovertext : [], 
                    y : [], 
                    x : [], 
                    flightsWithEventCounts : {}, 
                    totalFlightsCounts :{} 
                }   

                eventFleetPercents[eventName] = fleetPercents;
                eventNGAFIDPercents[eventName] = ngafidPercents;
            }


            for (let [airframe, value] of Object.entries(countsObject)) {
                if (value.airframeName === "Garmin Flight Display") continue;
                if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes") continue;

                /*
                console.log("airframes, airframeName, value:"); 
                console.log(airframes);
                console.log(airframe);
                console.log(value);
                */

                value.name = value.eventName + " - " + value.airframeName;
                value.x = value.dates;
                value.type = 'scatter';

                //don't add airframes to the count plot that the fleet doesn't have
                if (airframes.indexOf(value.airframeName) >= 0) countData.push(value);
                value.y = value.totalEventsCounts;

                for (let i = 0; i < value.dates.length; i++) {
                    let date = value.dates[i];

                    //don't add airframes to the fleet percentage plot that the fleet doesn't have
                    if (airframes.indexOf(value.airframeName) >= 0) {
                        if (date in fleetPercents.flightsWithEventCounts) {
                            fleetPercents.flightsWithEventCounts[date] += value.flightsWithEventCounts[i];
                            fleetPercents.totalFlightsCounts[date] += value.totalFlightsCounts[i];
                        } else {
                            fleetPercents.flightsWithEventCounts[date] = value.flightsWithEventCounts[i];
                            fleetPercents.totalFlightsCounts[date] = value.totalFlightsCounts[i];
                        }
                    }

                    if (date in ngafidPercents.flightsWithEventCounts) {
                        ngafidPercents.flightsWithEventCounts[date] += value.aggregateFlightsWithEventCounts[i];
                        ngafidPercents.totalFlightsCounts[date] += value.aggregateTotalFlightsCounts[i];
                    } else {
                        ngafidPercents.flightsWithEventCounts[date] = value.aggregateFlightsWithEventCounts[i];
                        ngafidPercents.totalFlightsCounts[date] = value.aggregateTotalFlightsCounts[i];
                    }
                }
            }
        }

        /*
        console.log("eventFleetPercents:");
        console.log(eventFleetPercents);
        console.log("eventNGAFIDPercents:");
        console.log(eventNGAFIDPercents);
        */

        for (let [eventName, fleetValue] of Object.entries(eventFleetPercents)) {
            let ngafidValue = eventNGAFIDPercents[eventName];
            percentData.push(fleetValue);
            percentData.push(ngafidValue);

            fleetValue.x = [];
            fleetValue.y = [];
            for (let date of Object.keys(fleetValue.flightsWithEventCounts).sort()) {
                fleetValue.x.push(date);

                let v = 100.0 * parseFloat(fleetValue.flightsWithEventCounts[date]) / parseFloat(fleetValue.totalFlightsCounts[date]);
                fleetValue.y.push(v);

                //console.log(date + " :: " + fleetValue.flightsWithEventCounts[date]  + " / " + fleetValue.totalFlightsCounts[date] + " : " + v);

                var fixedText = "";
                if (v > 0 && v < 1) {
                    //console.log("Log10 of y is " + Math.log10(v);
                    fixedText = v.toFixed(-Math.ceil(Math.log10(v)) + 2) + "%";
                } else {
                    fixedText = v.toFixed(2) + "%";
                }
                fleetValue.hovertext.push(fixedText);
            }

            //console.log(fleetValue);

            ngafidValue.x = [];
            ngafidValue.y = [];
            for (let date of Object.keys(ngafidValue.flightsWithEventCounts).sort()) {
                ngafidValue.x.push(date);

                let v = 100.0 * parseFloat(ngafidValue.flightsWithEventCounts[date]) / parseFloat(ngafidValue.totalFlightsCounts[date]);
                ngafidValue.y.push(v);

                //console.log(date + " :: " + ngafidValue.flightsWithEventCounts[date]  + " / " + ngafidValue.totalFlightsCounts[date] + " : " + v);

                var fixedText = "";
                if (v > 0 && v < 1) {
                    //console.log("Log10 of y is " + Math.log10(v);
                    fixedText = v.toFixed(-Math.ceil(Math.log10(v)) + 2) + "%";
                } else {
                    fixedText = v.toFixed(2) + "%";
                }
                ngafidValue.hovertext.push(fixedText);
            }

            //console.log(ngafidValue);
        }

        /*
        console.log("percentData:");
        console.log(percentData);
        */

        var countLayout = {
            title : 'Event Counts Over Time',
            //autosize: false,
            //width: 500,
            //height: 500,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            }
        };

        var percentLayout = {
            title : 'Percentage of Flights With Event Over Time',
            //autosize: false,
            //width: 500,
            //height: 500,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            }
        };

        var config = {responsive: true}

        Plotly.newPlot('count-trends-plot', countData, countLayout, config);
        Plotly.newPlot('percent-trends-plot', percentData, percentLayout, config);
    }


    checkEvent(eventName) {
        console.log("checking event: '" + eventName + "'");
        this.state.eventChecked[eventName] = !this.state.eventChecked[eventName];
        this.setState(this.state);

        let startDate = this.state.startYear + "-";
        let endDate = this.state.endYear + "-";

        //0 pad the months on the front
        if (parseInt(this.state.startMonth) < 10) startDate += "0" + parseInt(this.state.startMonth);
        else startDate += this.state.startMonth;
        if (parseInt(this.state.endMonth) < 10) endDate += "0" + parseInt(this.state.endMonth);
        else endDate += this.state.endMonth;

        var submission_data = {
            startDate : startDate + "-01",
            endDate : endDate + "-28",
            eventName : eventName
        };

        if (eventName in eventCounts) {
            console.log("already loaded counts for event: '" + eventName + "'");
            trendsCard.displayPlots(trendsCard.state.airframe);

        } else {
            $('#loading').show();
            console.log("showing loading spinner!");

            let trendsCard = this;

            $.ajax({
                type: 'POST',
                url: '/protected/monthly_event_counts',
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

                    eventCounts[eventName] = response;
                    trendsCard.displayPlots(trendsCard.state.airframe);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Uploads", errorThrown);
                },   
                async: true 
            });
        }
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
        console.log("[trendscard] notifying date change 2, startYear: '" + this.state.startYear + "', startMonth: '" + this.state.startMonth + ", endYear: '" + this.state.endYear + "', endMonth: '" + this.state.endMonth + "'"); 

        for (let [eventName, value] of Object.entries(this.state.eventChecked)) {
            this.state.eventChecked[eventName] = false;
        }
        this.state.datesChanged = false;
        this.setState(this.state);

        eventCounts = {};
        this.displayPlots(this.state.airframe);
    }

    airframeChange(airframe) {
        this.setState({airframe});
        this.displayPlots(airframe);
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
                    <div className="col-lg-12">
                        <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
                            <TimeHeader
                                name="Event Trends"
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
                                    <div className="col-lg-2" style={{padding:"8 8 8 8"}}>

                                        {
                                            eventNames.map((eventName, index) => {
                                                return (
                                                    <div key={index} className="form-check">
                                                        <input className="form-check-input" type="checkbox" value="" id={"event-check-" + index} checked={this.state.eventChecked[eventName]} onChange={() => this.checkEvent(eventName)}></input>
                                                        <label className="form-check-label" htmlFor={"event-check-" + index}>
                                                            {eventName}
                                                        </label>
                                                    </div>
                                                );
                                            })
                                        }

                                    </div>

                                    <div className="col-lg-10" style={{padding:"0 0 0 8"}}>
                                        <div id="count-trends-plot"></div>
                                        <div id="percent-trends-plot"></div>
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


var trendsCard = ReactDOM.render(
    <TrendsCard />,
    document.querySelector('#trends-card')
);

trendsCard.displayPlots("All Airframes");

var navbar = ReactDOM.render(
    <SignedInNavbar activePage={"trends"} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);

