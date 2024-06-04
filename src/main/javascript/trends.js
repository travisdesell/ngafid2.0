import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import TimeHeader from "./time_header.js";
import GetDescription from "./get_description";

import Plotly from 'plotly.js';
import Tooltip from "react-bootstrap/Tooltip";
import {OverlayTrigger} from "react-bootstrap";

airframes.unshift("All Airframes");
var index = airframes.indexOf("Garmin Flight Display");
if (index !== -1) airframes.splice(index, 1);

eventNames.sort();

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

var eventCounts = null;

var eventFleetPercents = {};
var eventNGAFIDPercents = {};

class TrendsPage extends React.Component {
    constructor(props) {
        super(props);
        let eventChecked = {};
        for (let i = 0; i < eventNames.length; i++) {
            eventChecked[eventNames[i]] = false;
        }

        var date = new Date();
        this.state = {
            airframe : "All Airframes",
            startYear : 2020,
            startMonth : 1,
            endYear : date.getFullYear(),
            endMonth : date.getMonth() + 1,
            datesChanged : false,
            aggregatePage : props.aggregate_page,
            eventChecked : eventChecked
        };
        
        this.fetchMonthlyEventCounts();
    }
    startDate() {
        let startDate = this.state.startYear + "-";

        if (parseInt(this.state.startMonth) < 10) startDate += "0" + parseInt(this.state.startMonth);
        else startDate += this.state.startMonth;

        return startDate;
    }

    endDate() {
        let endDate = this.state.endYear + "-";

        if (parseInt(this.state.endMonth) < 10) endDate += "0" + parseInt(this.state.endMonth);
        else endDate += this.state.endMonth;

        return endDate;
    }

    fetchMonthlyEventCounts() {
        var submission_data = {
            startDate : this.startDate() + "-01",
            endDate : this.endDate() + "-28",
            aggregatePage : this.props.aggregate_page
        };

        let trendsPage = this;
        $.ajax({
            type: 'POST',
            url: '/protected/monthly_event_counts',
            data : submission_data,
            dataType : 'json',
            success : function(response) {
                $('#loading').hide();

                if (response.err_msg) {
                    errorModal.show(response.err_title, response.err_msg);
                    return;
                }   

                eventCounts = response;
                trendsPage.displayPlots(trendsPage.state.airframe);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            },   
            async: true 
        });

    }

    exportCSV() {
        let selectedAirframe = this.state.airframe;

        let eventNames = [];
        let airframeNames = [];
        let dates = [];
        let csvValues = {};


        for (let [eventName, countsObject] of Object.entries(eventCounts)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            //make sure the eventNames array is unique names only
            if (!eventNames.includes(eventName)) {
                eventNames.push(eventName);
            }

            for (let [airframe, value] of Object.entries(countsObject)) {
                if (value.airframeName === "Garmin Flight Display") continue;
                if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes") continue;

                let airframeName = value.airframeName;
                let valueDates = value.dates;


                //make sure the airframeNames array is unique names only
                if (!airframeNames.includes(airframeName)) {
                    airframeNames.push(airframeName);
                }


                console.log(value.dates);

                for (let i = 0; i < value.dates.length; i++) {
                    let date = value.dates[i];
                    let eventCount = value.y[i];
                    let flightsWithEventCount;
                    let totalFlights;
                    if (this.state.aggregatePage) {
                        flightsWithEventCount = value.aggregateFlightsWithEventCounts[i];
                        totalFlights = value.aggregateTotalFlightsCounts[i];
                    }
                    else {
                        flightsWithEventCount = value.flightsWithEventCounts[i];
                        totalFlights = value.totalFlightsCounts[i];
                    }

                    //make sure the dates array is unique dates only
                    if (!dates.includes(date)) {
                        dates.push(date);
                    }

                    if (!(eventName in csvValues)) {
                        csvValues[eventName] = {};
                    }

                    if (!(airframeName in csvValues[eventName])) {
                        csvValues[eventName][airframeName] = {};
                    }

                    csvValues[eventName][airframeName][date] = {};
                    csvValues[eventName][airframeName][date].eventCount = eventCount;
                    csvValues[eventName][airframeName][date].flightsWithEventCount = flightsWithEventCount;
                    csvValues[eventName][airframeName][date].totalFlights = totalFlights;
                }
            }
        }
        eventNames.sort();
        airframeNames.sort();
        dates.sort();

        for (let eventName of eventNames) {
            console.log(csvValues[eventName]);

            for (let airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    console.log("\t" + eventName + " - " + airframeName + " has " + Object.keys(csvValues[eventName][airframeName]).length + " entries!");
                }
            }
        }

        let filetext = "";

        let needsComma = false;
        for (let eventName of eventNames) {
            for (let airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }

                    filetext += eventName;
                    filetext += "," + eventName;
                    filetext += "," + eventName;
                }
            }
        }
        filetext += "\n";

        needsComma = false;
        for (let eventName of eventNames) {
            for (let airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }
                    filetext += airframeName;
                    filetext += "," + airframeName;
                    filetext += "," + airframeName;
                }
            }
        }
        filetext += "\n";

        needsComma = false;
        for (let eventName of eventNames) {
            for (let airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }
                    filetext += "Events";
                    filetext += ",Flights With Event";
                    filetext += ",Total Flights";
                }
            }
        }
        filetext += "\n";

        for (let i = 0; i < dates.length; i++) {
            let date = dates[i];

            needsComma = false;
            for (let eventName of eventNames) {
                for (let airframeName of airframeNames) {
                    if (airframeName in csvValues[eventName]) {
                        if (needsComma) {
                            filetext += ",";
                        } else {
                            needsComma = true;
                        }

                        if (date in csvValues[eventName][airframeName]) {
                            filetext += csvValues[eventName][airframeName][date].eventCount;
                            filetext += "," + csvValues[eventName][airframeName][date].flightsWithEventCount;
                            filetext += "," + csvValues[eventName][airframeName][date].totalFlights;
                        } else {
                            filetext += ",,";
                        }
                    }
                }
            }
            filetext += "\n";
        }

        console.log("eventNames:");
        console.log(eventNames);
        console.log("airframeNames:");
        console.log(airframeNames);
        console.log("dates:");
        console.log(dates);


        let filename = "trends.csv";

        console.log("exporting csv!");

        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(filetext));
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    }

    displayPlots(selectedAirframe) {
        console.log("displaying plots with airframe: '" + selectedAirframe + "'");

        eventFleetPercents = {};
        eventNGAFIDPercents = {};

        countData = [];
        percentData = [];

        let counts = eventCounts == null ? {} : eventCounts;

        for (let [eventName, countsObject] of Object.entries(counts)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            let fleetPercents = null;
            let ngafidPercents = null;

            if (eventName in eventFleetPercents) {
                console.log('getting existing fleetPercents!');

                fleetPercents = eventFleetPercents[eventName];
                ngafidPercents = eventNGAFIDPercents[eventName];

            } else {
                console.log('setting initial fleetPercents!');

                fleetPercents = {
                    name : eventName + ' - Your Fleet',
                    type : 'scatter',
                    hoverinfo : 'x+text',
                    hovertext : [],
                    y : [],
                    x : [],
                    flightsWithEventCounts : {},
                    totalFlightsCounts : {},
                }
                let ngafidPercentsName = eventName + " - ";
                if (this.state.aggregatePage) {
                    ngafidPercentsName += "All Fleets";
                }
                else {
                    ngafidPercentsName += "All Other Fleets";
                }
                ngafidPercents = {
                    name : ngafidPercentsName,
                    type : 'scatter',
                    hoverinfo : 'x+text',
                    hovertext : [],
                    y : [],
                    x : [],
                    flightsWithEventCounts : {},
                    totalFlightsCounts :{},
                }

                eventFleetPercents[eventName] = fleetPercents;
                eventNGAFIDPercents[eventName] = ngafidPercents;
            }


            for (let [airframe, value] of Object.entries(countsObject)) {
                if (value.airframeName === "Garmin Flight Display") continue;
                if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes") continue;

                console.log("airframes:");
                console.log(airframes);
                console.log("airframe name:")
                console.log(airframe);
                console.log("value:")
                console.log(value);

                let new_value_name = value.eventName + " - " + value.airframeName;
                console.log("setting value name: " + value.name + " to " + new_value_name);

                value.name = new_value_name;
                value.x = value.dates;
                value.type = 'scatter';
                value.hoverinfo = 'x+text';

                //don't add airframes to the count plot that the fleet doesn't have
                if (airframes.indexOf(value.airframeName) >= 0) countData.push(value);
                if (this.state.aggregatePage) {
                    value.y = value.aggregateTotalEventsCounts;
                }
                else {
                    value.y = value.totalEventsCounts;
                }
                value.hovertext = [];

                for (let i = 0; i < value.dates.length; i++) {
                    let date = value.dates[i];

                    //don't add airframes to the fleet percentage plot that the fleet doesn't have
                    if (airframes.indexOf(value.airframeName) >= 0 && !this.state.aggregatePage) {
                        if (date in fleetPercents.flightsWithEventCounts) {
                            //console.log("incremented fleetPercents.flightsWithEventCounts for date: " + date + " and airframe: " + value.airframeName + " initially " + fleetPercents.flightsWithEventCounts[date] + " by " + value.flightsWithEventCounts[i]);
                            //console.log("incremented fleetPercents.totalFlightsCounts for date: " + date + " and airframe: " + value.airframeName + " initially " + fleetPercents.totalFlightsCounts[date] + " by " + value.totalFlightsCounts[i]);

                            fleetPercents.flightsWithEventCounts[date] += value.flightsWithEventCounts[i];
                            fleetPercents.totalFlightsCounts[date] += value.totalFlightsCounts[i];

                            //console.log("incremented fleetPercents.flightsWithEventCounts for date: " + date + " and airframe: " + value.airframeName + " to " + fleetPercents.flightsWithEventCounts[date] + " was incremented by " + value.flightsWithEventCounts[i]);
                            //console.log("incremented fleetPercents.totalFlightsCounts for date: " + date + " and airframe: " + value.airframeName + " to " + fleetPercents.totalFlightsCounts[date] + " was incremented by " + value.totalFlightsCounts[i]);
                        } else {
                            fleetPercents.flightsWithEventCounts[date] = value.flightsWithEventCounts[i];
                            fleetPercents.totalFlightsCounts[date] = value.totalFlightsCounts[i];

                            //console.log("resetting fleetPercents for date: " + date + " and airframe: " + value.airframeName + " to " + fleetPercents.flightsWithEventCounts[date]);
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

                for (let i = 0; i < value.dates.length; i++) {
                    let date = value.dates[i];
                    let flightsWithEventCount;
                    let totalFlightsCount;
                    if (this.state.aggregatePage) {
                        flightsWithEventCount = value.aggregateFlightsWithEventCounts[i];
                        totalFlightsCount = value.aggregateTotalFlightsCounts[i];
                    }
                    else {
                        flightsWithEventCount = value.flightsWithEventCounts[i];
                        totalFlightsCount = value.totalFlightsCounts[i];
                    }
                    value.hovertext.push(value.y[i] + " events in " + flightsWithEventCount + " of " + totalFlightsCount + " flights : " + value.eventName + " - " + value.airframeName);
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
            if (!this.state.aggregatePage) {
                percentData.push(fleetValue);
                fleetValue.x = [];
                fleetValue.y = [];

                for (let date of Object.keys(fleetValue.flightsWithEventCounts).sort()) {
                    fleetValue.x.push(date);

                    let v = 100.0 * (parseFloat(fleetValue.flightsWithEventCounts[date]) / parseFloat(fleetValue.totalFlightsCounts[date]));
                    if (isNaN(v)) v = 0.0;
                    fleetValue.y.push(v);

                    // console.log(date + " :: " + fleetValue.flightsWithEventCounts[date]  + " / " + fleetValue.totalFlightsCounts[date] + " : " + v);

                    //this will give 2 significant figures (and leading 0s if it is quite small)
                    var fixedText = "";
                    if (v > 0 && v < 1) {
                        //console.log("Log10 of y is " + Math.log10(v);
                        fixedText = v.toFixed(-Math.ceil(Math.log10(v)) + 2) + "%";
                    } else {
                        fixedText = v.toFixed(2) + "%";
                    }
                    fleetValue.hovertext.push(fixedText  + " (" + fleetValue.flightsWithEventCounts[date] + " of " + fleetValue.totalFlightsCounts[date] + " flights) : " + fleetValue.name);
                }
            }
            percentData.push(ngafidValue);
            ngafidValue.x = [];
            ngafidValue.y = [];
            for (let date of Object.keys(ngafidValue.flightsWithEventCounts).sort()) {
                ngafidValue.x.push(date);

                let v = 100.0 * parseFloat(ngafidValue.flightsWithEventCounts[date]) / parseFloat(ngafidValue.totalFlightsCounts[date]);
                if (isNaN(v)) v = 0.0;

                ngafidValue.y.push(v);

                //console.log(date + " :: " + ngafidValue.flightsWithEventCounts[date]  + " / " + ngafidValue.totalFlightsCounts[date] + " : " + v);

                //this will give 2 significant figures (and leading 0s if it is quite small)
                var fixedText = "";
                if (v > 0 && v < 1) {
                    //console.log("Log10 of y is " + Math.log10(v);
                    fixedText = v.toFixed(-Math.ceil(Math.log10(v)) + 2) + "%";
                } else {
                    fixedText = v.toFixed(2) + "%";
                }
                ngafidValue.hovertext.push(fixedText + " (" + ngafidValue.flightsWithEventCounts[date] + " of " + ngafidValue.totalFlightsCounts[date] + " flights) : " + ngafidValue.name);
            }

            //console.log(ngafidValue);
        }

        /*
        console.log("percentData:");
        console.log(percentData);
        */

        var countLayout = {
            title : 'Event Counts Over Time',
            hovermode : "x unified",
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
            hovermode : "x unified",
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

        var config = {responsive: true};

        /*
        console.log("countData, percentData:");
        console.log(countData);
        console.log(percentData);
        */

        Plotly.newPlot('count-trends-plot', countData, countLayout, config);
        Plotly.newPlot('percent-trends-plot', percentData, percentLayout, config);


    }


    checkEvent(eventName) {
        console.log("checking event: '" + eventName + "'");
        console.log("Changing event state : " + eventName + " from " + this.state.eventChecked[eventName] + " to " + !this.state.eventChecked[eventName]);
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
            eventName : eventName,
            aggregatePage : this.props.aggregate_page
        };
        console.log(eventCounts);
        if (eventCounts != null) {
            console.log("already loaded counts for event: '" + eventName + "'");
            this.displayPlots(this.state.airframe);
        } else {
            $('#loading').show();
            console.log("showing loading spinner!");
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

        eventCounts = null;
        this.fetchMonthlyEventCounts();
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
            <div>
                <SignedInNavbar activePage={"trends"} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

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
                                    exportCSV={() => this.exportCSV()}


                                />

                            <div className="card-body" style={{padding:"0"}}>
                                <div className="row" style={{margin:"0"}}>
                                    <div className="col-lg-2" style={{padding:"8 8 8 8"}}>

                                        {
                                            eventNames.map((eventName, index) => {
                                                return (
                                                    <div key={index} className="form-check">
                                                        <input  className="form-check-input" 
                                                                type="checkbox" 
                                                                value="" 
                                                                id={"event-check-" + index}
                                                                checked={this.state.eventChecked[eventName]} 
                                                                onChange={() => this.checkEvent(eventName)}></input>

                                                        <OverlayTrigger overlay={(props) => (
                                                            <Tooltip {...props}>{GetDescription(eventName)}</Tooltip>)}
                                                                        placement="bottom">
                                                            <label className="form-check-label" onclick={() => this.checkEvent(eventName)}>
                                                                {eventName}
                                                            </label>
                                                        </OverlayTrigger>

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
        </div>
        );
    }
}




export default TrendsPage
