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
import { DarkModeToggle } from './dark_mode_toggle.js';

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
        let eventsEmpty = {};

        eventNames.unshift("ANY Event");
        for (let i = 0; i < eventNames.length; i++) {

            let eventNameCur = eventNames[i];
            eventChecked[eventNameCur] = false;
            eventsEmpty[eventNameCur] = true;
        }
        eventsEmpty["ANY Event"] = false;

        var date = new Date();
        this.state = {
            airframe : "All Airframes",
            startYear : 2020,
            startMonth : 1,
            endYear : date.getFullYear(),
            endMonth : date.getMonth() + 1,
            datesChanged : false,
            aggregatePage : props.aggregate_page,
            eventChecked : eventChecked,
            eventsEmpty : eventsEmpty
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

        $('#loading').hide();

        return new Promise((resolve, reject) => {
            $.ajax({
                type: 'POST',
                url: '/protected/monthly_event_counts',
                data : submission_data,
                dataType : 'json',
                success : function(response) {

                    if (response.err_msg) {
                        errorModal.show(response.err_title, response.err_msg);
                        return;
                    }   

                    eventCounts = response;
                    
                    let countsMerged = {};
                    for(let [eventName, countsObject] of Object.entries(eventCounts)) {

                        for(let [airframeName] of Object.entries(countsObject)) {

                            if (airframeName === "Garmin Flight Display") {
                                continue;
                            }

                            let countsAirframe = countsObject[airframeName];

                            //Airframe name is not in the merged counts object yet, add it
                            if (!(airframeName in countsMerged)) {

                                countsMerged[airframeName] = {
                                    airframeName: airframeName,
                                    eventName: "ANY Event",
                                    dates: [...countsAirframe.dates],
                                    aggregateFlightsWithEventCounts: [...countsAirframe.aggregateFlightsWithEventCounts],
                                    aggregateTotalEventsCounts: [...countsAirframe.aggregateTotalEventsCounts],
                                    aggregateTotalFlightsCounts: [...countsAirframe.aggregateTotalFlightsCounts],
                                    flightsWithEventCounts: [...countsAirframe.flightsWithEventCounts],
                                    totalEventsCounts: [...countsAirframe.totalEventsCounts],
                                    totalFlightsCounts: [...countsAirframe.totalFlightsCounts]
                                };

                            //Airframe name is already in the merged counts object, add the counts
                            } else {

                                for (let i = 0 ; i < countsAirframe.dates.length ; i++) {
                                    
                                    if (countsAirframe.totalEventsCounts[i] === 0)
                                        continue;
    
                                    countsMerged[airframeName].aggregateFlightsWithEventCounts[i] += countsAirframe.aggregateFlightsWithEventCounts[i];
                                    countsMerged[airframeName].aggregateTotalEventsCounts[i] += countsAirframe.aggregateTotalEventsCounts[i];
                                    countsMerged[airframeName].aggregateTotalFlightsCounts[i] += countsAirframe.aggregateTotalFlightsCounts[i];
                                    countsMerged[airframeName].flightsWithEventCounts[i] += countsAirframe.flightsWithEventCounts[i];
                                    countsMerged[airframeName].totalEventsCounts[i] += countsAirframe.totalEventsCounts[i];
                                    countsMerged[airframeName].totalFlightsCounts[i] += countsAirframe.totalFlightsCounts[i];
                                }

                            }

                        }

                    }

                    eventCounts["ANY Event"] = countsMerged;

                    trendsPage.displayPlots(trendsPage.state.airframe);

                    resolve(response);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Uploads", errorThrown);
                    reject(errorThrown);
                },   
                async: true 
            });
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
                    } else {
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

        let airframeNames = [];
        for (let [eventName, countsObject] of Object.entries(counts)) {
            for (let [airframe, value] of Object.entries(countsObject)) {
                if (value.airframeName === "Garmin Flight Display") continue;
                if (!airframeNames.includes(value.airframeName)) {
                    airframeNames.push(value.airframeName);
                }
            }
        }

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
                } else {
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

                /*
                console.log("airframes, airframeName, value:");
                console.log(airframes);
                console.log(airframe);
                console.log(value);
                */

                value.name = value.eventName + " - " + value.airframeName;
                value.x = value.dates;
                value.type = 'scatter';
                value.hoverinfo = 'x+text';

                    
                let airframeIndex = airframes.indexOf(value.airframeName);
                let eventNameIndex = eventNames.indexOf(eventName);

                let indexCur = (airframeIndex + eventNameIndex);
                let indicesMax = (airframes.length + eventNames.length);

                //Dashed lines for ANY Event
                if (eventName === "ANY Event") {

                    value = {
                        ...value,
                        legendgroup: value.name,

                        //Consistent rainbow colors for each airframe
                        line : {
                            width: 1.0,
                            dash: 'dot',
                            color : 'hsl(' + parseInt(360.0 * airframeIndex / airframeNames.length) + ', 50%, 50%)'
                        }

                    };

                //'Glowing' rainbow lines for other events
                } else {

                    value = {
                        ...value,
                        legendgroup: value.name,
                        //showlegend: false,
                        
                        mode : 'lines',
                        line : {
                            width : 2,
                            // color : 'hsl('
                            //     + parseInt(360.0 * indexCur / indicesMax) + ','
                            //     + parseInt(50.0 + 50.0 * airframeIndex / airframeNames.length) + '%,'
                            //     + parseInt(25.0 + 25.0) + '%)'
                        }

                    };
                }

                //don't add airframes to the count plot that the fleet doesn't have
                if (airframes.indexOf(value.airframeName) >= 0) {
                    
                    //Display the "ANY Event" lines under the other ones
                    if (eventName === "ANY Event") {
                        countData.unshift(value);
                    } else {
                        countData.push(value);
                    }
                    
                }

                if (this.state.aggregatePage) {
                    value.y = value.aggregateTotalEventsCounts;
                } else {
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
                    } else {
                        flightsWithEventCount = value.flightsWithEventCounts[i];
                        totalFlightsCount = value.totalFlightsCounts[i];
                    }
                    value.hovertext.push(value.y[i] + " events in " + flightsWithEventCount + " of " + totalFlightsCount + " flights : " + value.eventName + " - " + value.airframeName);
                }
                
            }

        }

        for (const airframeName of airframeNames) {

            let airframeIndex = airframeNames.indexOf(airframeName);
            let airframeLegendHighlight = {
                
                name : airframeName,

                x : [0],
                y : [0],

                //visible: 'legendonly',
                visible: false,
                showlegend: true,

                mode : 'markers',
                marker : {
                    width : 2.0,
                    opacity: 1.0,
                    // color : 'hsl('
                    //     + parseInt(360.0 * airframeIndex / airframeNames.length) + ','
                    //     + parseInt(50.0) + '%,'
                    //     + parseInt(50.0) + '%)'
                }

            };

            countData.push(airframeLegendHighlight);

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



            let airframeIndex = airframes.indexOf(ngafidValue.airframeName);
            let eventNameIndex = eventNames.indexOf(eventName);

            let indexCur = (airframeIndex + eventNameIndex);
            let indicesMax = (airframes.length + eventNames.length);

            //...
            if (eventName === "ANY Event") {

                ngafidValue = {
                    ...ngafidValue,

                    legendgroup: ngafidValue.name,

                    //Consistent rainbow colors for each event
                    line : {
                        width: 1,
                        dash: 'dot',
                        color : 'hsl(' + parseInt(360.0 * airframeIndex / airframeNames.length) + ', 50%, 50%)'
                    }

                };

            //...
            } else {

                ngafidValue = {
                    ...ngafidValue,

                    legendgroup: ngafidValue.name,
                    mode : 'lines',

                    //Consistent rainbow colors for each event
                    line : {
                        width : 2,
                        // color : 'hsl('
                        //     + parseInt(360.0 * eventNameIndex / eventNames.length)
                        //     + parseInt(50.0 + 50.0 * airframeIndex / airframeNames.length) + '%,'
                        //     + parseInt(25.0 + 25.0 * airframeIndex / airframeNames.length) + '%)'
                        color : 'hsl('
                            + parseInt(360.0 * indexCur / indicesMax) + ','
                            + parseInt(50.0 + 50.0 * airframeIndex / airframeNames.length) + '%,'
                            + parseInt(25.0 + 25.0) + '%)'
//                                + parseInt(25.0 + 25.0 * (indexCur%2)) + '%)'
                    }

                };
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

        let styles = getComputedStyle(document.documentElement);
        let plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        let plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        let plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

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
            },
            plot_bgcolor : "transparent",
            paper_bgcolor : plotBgColor,
            font : {
                color : plotTextColor
            },
            xaxis : {
                gridcolor : plotGridColor
            },
            yaxis : {
                gridcolor : plotGridColor
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
            },
            plot_bgcolor : "transparent",
            paper_bgcolor : plotBgColor,
            font : {
                color : plotTextColor
            },
            xaxis : {
                gridcolor : plotGridColor
            },
            yaxis : {
                gridcolor : plotGridColor
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

        console.log("Hiding loading spinner");
        $('#loading').hide();

    }


    checkEvent(eventName) {

        console.log("checking event: '" + eventName + "'");
        this.state.eventChecked[eventName] = !this.state.eventChecked[eventName];
        this.setState(this.state);

        this.displayPlots(this.state.airframe);

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
        $('#loading').hide();

        this.fetchMonthlyEventCounts().then((data) => {

            //Set all events to empty initially
            for (let i = 0; i < eventNames.length; i++) {
                let eventNameCur = eventNames[i];
                this.state.eventsEmpty[eventNameCur] = true;
            }

            for (let [eventName, countsObject] of Object.entries(data)) {
                this.state.eventsEmpty[eventName] = false;
            }

            this.setState(this.state);
            this.displayPlots(this.state.airframe);

        });

    }

    airframeChange(airframe) {
        this.setState({airframe});
        this.displayPlots(airframe);
    }

    render() {

        const numberOptions = { 
            minimumFractionDigits: 2,
            maximumFractionDigits: 2 
        };

        return (
            <div style={{overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar activePage={"trends"} darkModeOnClickAlt={()=>{this.displayPlots(this.state.airframe);}} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div className="container-fluid" style={{overflowY:"auto", flex:"1 1 auto"}}>

                    <div className="row">
                        <div className="col-lg-12">
                            <div className="card mb-2 m-2">
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

                                                //Don't show a description for the "ANY Event" event
                                                if (eventName === "ANY Event") return (
                                                    <div key={index} className="form-check">
                                                        <input className="form-check-input" disabled={this.state.eventsEmpty[eventName]} type="checkbox" value="" id={"event-check-" + index} checked={this.state.eventChecked[eventName]} onChange={() => this.checkEvent(eventName)}></input>
                                                        <label className="form-check-label">
                                                            {eventName}
                                                        </label>
                                                    </div>
                                                );

                                                return (
                                                    <div key={index} className="form-check">
                                                        <input  className="form-check-input"
                                                                disabled={this.state.eventsEmpty[eventName]} 
                                                                type="checkbox" 
                                                                value="" 
                                                                id={"event-check-" + index}
                                                                checked={this.state.eventChecked[eventName]} 
                                                                onChange={() => this.checkEvent(eventName)}>
                                                        </input>
                                                        <OverlayTrigger overlay={(props) => (
                                                            <Tooltip {...props}>{GetDescription(eventName)}</Tooltip>)}
                                                                        placement="bottom">
                                                            <label className="form-check-label">
                                                                {eventName}
                                                            </label>
                                                        </OverlayTrigger>

                                                    </div>
                                                );
                                            })
                                        }

                                    </div>

                                    <div className="col-lg-10" style={{padding:"0 0 0 8", opacity:"0.80"}}>
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
