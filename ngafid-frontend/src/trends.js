import 'bootstrap';

import React from "react";

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import { TimeHeader } from "./time_header.js";
import GetDescription from "./get_description";

import Plotly from 'plotly.js';
import Tooltip from "react-bootstrap/Tooltip";
import {OverlayTrigger} from "react-bootstrap";


airframes.unshift("All Airframes");
const index = airframes.indexOf("Garmin Flight Display");
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

let countData = [];
let percentData = [];

let eventCounts = null;

let eventFleetPercents = {};
let eventNGAFIDPercents = {};

class TrendsPage extends React.Component {
    constructor(props) {
        super(props);
        const eventChecked = {};
        const eventsEmpty = {};

        eventNames.unshift("ANY Event");
        for (let i = 0; i < eventNames.length; i++) {

            const eventNameCur = eventNames[i];
            eventChecked[eventNameCur] = false;
            eventsEmpty[eventNameCur] = true;
        }
        eventsEmpty["ANY Event"] = false;

        const date = new Date();
        this.state = {
            airframe: "All Airframes",
            startYear: 2020,
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            datesChanged: false,
            aggregatePage: props.aggregate_page,
            eventChecked: eventChecked,
            eventsEmpty: eventsEmpty
        };

        this.fetchMonthlyEventCounts();
    }

    componentDidMount() {

        this.displayPlots("All Airframes");

    }

    startDate() {
        let startDate = `${this.state.startYear  }-`;

        if (parseInt(this.state.startMonth) < 10) startDate += `0${  parseInt(this.state.startMonth)}`;
        else startDate += this.state.startMonth;

        return startDate;
    }

    endDate() {
        let endDate = `${this.state.endYear  }-`;

        if (parseInt(this.state.endMonth) < 10) endDate += `0${  parseInt(this.state.endMonth)}`;
        else endDate += this.state.endMonth;

        return endDate;
    }

    fetchMonthlyEventCounts() {
        const submissionData = {
            startDate: `${this.startDate()  }-01`,
            endDate: `${this.endDate()  }-28`,
            aggregatePage: this.props.aggregate_page
        };

        $('#loading').hide();

        return new Promise((resolve, reject) => {
            $.ajax({
                type: 'GET',
                url: '/api/event/count/monthly/by-name',
                data: submissionData,
                dataType: 'json',
                async: true,
                success: (response) => {

                    if (response.err_msg) {
                        showErrorModal(response.err_title, response.err_msg);
                        return;
                    }

                    eventCounts = response;

                    const countsMerged = {};
                    for (const [countsObject] of Object.entries(eventCounts)) {

                        for (const [airframeName] of Object.entries(countsObject)) {

                            if (airframeName === "Garmin Flight Display")
                                continue;

                            const countsAirframe = countsObject[airframeName];

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

                                for (let i = 0; i < countsAirframe.dates.length; i++) {

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

                    this.displayPlots(this.state.airframe);

                    resolve(response);
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    showErrorModal("Error Loading Uploads", errorThrown);
                    reject(errorThrown);
                },
            });
        });

    }

    exportCSV() {
        const selectedAirframe = this.state.airframe;

        const eventNames = [];
        const airframeNames = [];
        const dates = [];
        const csvValues = {};


        for (const [eventName, countsObject] of Object.entries(eventCounts)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            //make sure the eventNames array is unique names only
            if (!eventNames.includes(eventName)) {
                eventNames.push(eventName);
            }

            for (const [value] of Object.entries(countsObject)) {

                if (value.airframeName === "Garmin Flight Display")
                    continue;

                if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes")
                    continue;

                const airframeName = value.airframeName;

                //Make sure the airframeNames array is unique names only
                if (!airframeNames.includes(airframeName))
                    airframeNames.push(airframeName);

                console.log(value.dates);

                for (let i = 0; i < value.dates.length; i++) {
                    const date = value.dates[i];
                    const eventCount = value.y[i];
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

        for (const eventName of eventNames) {
            console.log(csvValues[eventName]);

            for (const airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    console.log(`\t${  eventName  } - ${  airframeName  } has ${  Object.keys(csvValues[eventName][airframeName]).length  } entries!`);
                }
            }
        }

        let filetext = "";

        let needsComma = false;
        for (const eventName of eventNames) {
            for (const airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }

                    filetext += eventName;
                    filetext += `,${  eventName}`;
                    filetext += `,${  eventName}`;
                }
            }
        }
        filetext += "\n";

        needsComma = false;
        for (const eventName of eventNames) {
            for (const airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }
                    filetext += airframeName;
                    filetext += `,${  airframeName}`;
                    filetext += `,${  airframeName}`;
                }
            }
        }
        filetext += "\n";

        needsComma = false;
        for (const eventName of eventNames) {
            for (const airframeName of airframeNames) {
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
            const date = dates[i];

            needsComma = false;
            for (const eventName of eventNames) {
                for (const airframeName of airframeNames) {
                    if (airframeName in csvValues[eventName]) {
                        if (needsComma) {
                            filetext += ",";
                        } else {
                            needsComma = true;
                        }

                        if (date in csvValues[eventName][airframeName]) {
                            filetext += csvValues[eventName][airframeName][date].eventCount;
                            filetext += `,${  csvValues[eventName][airframeName][date].flightsWithEventCount}`;
                            filetext += `,${  csvValues[eventName][airframeName][date].totalFlights}`;
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


        const filename = "trends.csv";

        console.log("exporting csv!");

        const element = document.createElement('a');
        element.setAttribute('href', `data:text/plain;charset=utf-8,${  encodeURIComponent(filetext)}`);
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    }

    displayPlots(selectedAirframe) {
        console.log(`displaying plots with airframe: '${  selectedAirframe  }'`);

        eventFleetPercents = {};
        eventNGAFIDPercents = {};

        countData = [];
        percentData = [];


        const counts = eventCounts == null ? {} : eventCounts;

        const airframeNames = [];
        for (const [countsObject] of Object.entries(counts)) {

            for (const [value] of Object.entries(countsObject)) {
                
                if (value.airframeName === "Garmin Flight Display")
                    continue;

                if (!airframeNames.includes(value.airframeName))
                    airframeNames.push(value.airframeName);
                
            }

        }

        for (const [eventName, countsObject] of Object.entries(counts)) {

            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName])
                continue;

            let fleetPercents = null;
            let ngafidPercents = null;

            if (eventName in eventFleetPercents) {

                console.log('getting existing fleetPercents!');

                fleetPercents = eventFleetPercents[eventName];
                ngafidPercents = eventNGAFIDPercents[eventName];

            } else {

                console.log('setting initial fleetPercents!');

                fleetPercents = {
                    name: `${eventName  } - Your Fleet`,
                    type: 'scatter',
                    hoverinfo: 'x+text',
                    hovertext: [],
                    y: [],
                    x: [],
                    flightsWithEventCounts: {},
                    totalFlightsCounts: {},
                };
                let ngafidPercentsName = `${eventName  } - `;

                if (this.state.aggregatePage)
                    ngafidPercentsName += "All Fleets";
                else
                    ngafidPercentsName += "All Other Fleets";

                ngafidPercents = {
                    name: ngafidPercentsName,
                    type: 'scatter',
                    hoverinfo: 'x+text',
                    hovertext: [],
                    y: [],
                    x: [],
                    flightsWithEventCounts: {},
                    totalFlightsCounts: {},
                };

                eventFleetPercents[eventName] = fleetPercents;
                eventNGAFIDPercents[eventName] = ngafidPercents;
            }


            for (let [value] of Object.entries(countsObject)) {

                //Airframe name is 'Garmin Flight Display', skip
                if (value.airframeName === "Garmin Flight Display")
                    continue;

                //Current airframe name is neither the selected airframe name or 'All Airframes', skip
                if ((selectedAirframe !== value.airframeName) && (selectedAirframe !== "All Airframes"))
                    continue;

                value.name = `${value.eventName  } - ${  value.airframeName}`;
                value.x = value.dates;
                value.type = 'scatter';
                value.hoverinfo = 'x+text';

                //Event name is 'ANY Event'
                if (eventName === "ANY Event") {

                    value = {
                        ...value,

                        legendgroup: value.name,

                        //Dashed lines for 'ANY Event'
                        line: {
                            dash: 'dot'
                        }

                    };

                    //Event is NOT 'ANY Event'
                } else {

                    value = {
                        ...value,

                        legendgroup: value.name,
                        mode: 'lines',

                        //Standard lines for non-'ANY Event'
                        line: {
                            width: 2,
                        }

                    };

                }

                //don't add airframes to the count plot that the fleet doesn't have
                if (airframes.indexOf(value.airframeName) >= 0) {

                    //Display the "ANY Event" lines under the other ones
                    if (eventName === "ANY Event") {
                        countData.push(value);
                    } else {
                        countData.unshift(value);
                    }

                }

                if (this.state.aggregatePage) {
                    value.y = value.aggregateTotalEventsCounts;
                } else {
                    value.y = value.totalEventsCounts;
                }
                value.hovertext = [];

                for (let i = 0; i < value.dates.length; i++) {
                    const date = value.dates[i];

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
                    
                    let flightsWithEventCount;
                    let totalFlightsCount;
                    if (this.state.aggregatePage) {
                        flightsWithEventCount = value.aggregateFlightsWithEventCounts[i];
                        totalFlightsCount = value.aggregateTotalFlightsCounts[i];
                    } else {
                        flightsWithEventCount = value.flightsWithEventCounts[i];
                        totalFlightsCount = value.totalFlightsCounts[i];
                    }

                    value.hovertext.push(`${value.y[i]  } events in ${  flightsWithEventCount  } of ${  totalFlightsCount  } flights : ${  value.eventName  } - ${  value.airframeName}`);

                }

            }

        }

        for (const airframeName of airframeNames) {

            const airframeLegendHighlight = {

                name: airframeName,

                x: [0],
                y: [0],

                //visible: 'legendonly',
                visible: false,
                showlegend: true,

                mode: 'markers',
                marker: {
                    width: 2.0,
                    opacity: 1.0,
                }

            };

            countData.push(airframeLegendHighlight);

        }

        for (const [eventName, fleetValueOrig] of Object.entries(eventFleetPercents)) {

            console.log("Event Name: ", eventName);

            let fleetValue = fleetValueOrig;

            //FLEET VALUE -- Event name is 'ANY Event'
            if (eventName === "ANY Event") {

                fleetValue = {
                    ...fleetValue,

                    legendgroup: fleetValue.name,

                    //Dashed lines for 'ANY Event'
                    line: {
                        dash: 'dot'
                    }

                };

            //FLEET VALUE -- Event is NOT 'ANY Event'
            } else {

                fleetValue = {
                    ...fleetValue,

                    legendgroup: fleetValue.name,
                    mode: 'lines',

                    //Standard lines for non-'ANY Event'
                    line: {
                        width: 2,
                    }

                };

            }

            //Push fleet values...
            if (!this.state.aggregatePage) {
                percentData.push(fleetValue);
                fleetValue.x = [];
                fleetValue.y = [];

                for (const date of Object.keys(fleetValue.flightsWithEventCounts).sort()) {
                    fleetValue.x.push(date);

                    let v = 100.0 * (parseFloat(fleetValue.flightsWithEventCounts[date]) / parseFloat(fleetValue.totalFlightsCounts[date]));
                    if (isNaN(v)) v = 0.0;
                    fleetValue.y.push(v);

                    // console.log(date + " :: " + fleetValue.flightsWithEventCounts[date]  + " / " + fleetValue.totalFlightsCounts[date] + " : " + v);

                    //this will give 2 significant figures (and leading 0s if it is quite small)
                    let fixedText = "";
                    if (v > 0 && v < 1) {
                        //console.log("Log10 of y is " + Math.log10(v);
                        fixedText = `${v.toFixed(-Math.ceil(Math.log10(v)) + 2)  }%`;
                    } else {
                        fixedText = `${v.toFixed(2)  }%`;
                    }
                    fleetValue.hovertext.push(`${fixedText  } (${  fleetValue.flightsWithEventCounts[date]  } of ${  fleetValue.totalFlightsCounts[date]  } flights) : ${  fleetValue.name}`);
                }
            }

            //Push NGAFID data...
            let ngafidValue = eventNGAFIDPercents[eventName];


            //NGAFID VALUE -- Event name is 'ANY Event'
            if (eventName === "ANY Event") {

                //  console.log("[EX] NAME (AE): ", ngafidValue.name);

                ngafidValue = {
                    ...ngafidValue,

                    legendgroup: ngafidValue.name,

                    //Dashed lines for 'ANY Event'
                    line: {
                        dash: 'dot'
                    }

                };

                //NGAFID VALUE -- Event is NOT 'ANY Event'
            } else {

                //  console.log("[EX] NAME: ", ngafidValue.name);

                ngafidValue = {
                    ...ngafidValue,

                    legendgroup: ngafidValue.name,
                    mode: 'lines',

                    //Standard lines for non-'ANY Event'
                    line: {
                        width: 2,
                    }

                };

            }

            percentData.push(ngafidValue);
            ngafidValue.x = [];
            ngafidValue.y = [];
            for (const date of Object.keys(ngafidValue.flightsWithEventCounts).sort()) {
                ngafidValue.x.push(date);

                let v = 100.0 * parseFloat(ngafidValue.flightsWithEventCounts[date]) / parseFloat(ngafidValue.totalFlightsCounts[date]);
                if (isNaN(v)) v = 0.0;

                ngafidValue.y.push(v);


                //console.log(date + " :: " + ngafidValue.flightsWithEventCounts[date]  + " / " + ngafidValue.totalFlightsCounts[date] + " : " + v);

                //this will give 2 significant figures (and leading 0s if it is quite small)
                let fixedText = "";
                if (v > 0 && v < 1) {
                    //console.log("Log10 of y is " + Math.log10(v);
                    fixedText = `${v.toFixed(-Math.ceil(Math.log10(v)) + 2)  }%`;
                } else {
                    fixedText = `${v.toFixed(2)  }%`;
                }
                ngafidValue.hovertext.push(`${fixedText  } (${  ngafidValue.flightsWithEventCounts[date]  } of ${  ngafidValue.totalFlightsCounts[date]  } flights) : ${  ngafidValue.name}`);
            }

        }

        /*
            console.log("percentData:");
            console.log(percentData);
        */

        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        const countLayout = {
            title: {text: 'Event Counts Over Time'},
            hovermode: "x unified",
            autosize: true,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            },
            legend: {
                traceorder: "normal"
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                gridcolor: plotGridColor
            },
            yaxis: {
                gridcolor: plotGridColor
            }
        };

        const percentLayout = {
            title: {text: 'Percentage of Flights With Event Over Time'},
            hovermode: "x unified",
            autosize: true,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            },
            legend: {
                traceorder: "normal"
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                gridcolor: plotGridColor
            },
            yaxis: {
                gridcolor: plotGridColor
            }
        };

        const config = {responsive: true};

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

        console.log("Checking event: '", eventName, "'");
        this.setState(prevState => {
            const eventChecked = { ...prevState.eventChecked };
            eventChecked[eventName] = !eventChecked[eventName];
            return { eventChecked };
        }, () => {
            this.displayPlots(this.state.airframe);
        });

    }

    updateStartYear(newStartYear) {

        console.log("Setting new start year to: ", newStartYear);
        this.setState({startYear: newStartYear, datesChanged: true});
        console.log(this.state);

    }

    updateStartMonth(newStartMonth) {

        console.log("Setting new start month to: ", newStartMonth);
        this.setState({startMonth: newStartMonth, datesChanged: true});
        console.log(this.state);

    }

    updateEndYear(newEndYear) {

        console.log("Setting new end year to: ", newEndYear);
        this.setState({endYear: newEndYear, datesChanged: true});
        console.log(this.state);

    }

    updateEndMonth(newEndMonth) {

        console.log("Setting new end month to: ", newEndMonth);
        this.setState({endMonth: newEndMonth, datesChanged: true});
        console.log(this.state);

    }

    dateChange() {

        console.log(`[trendscard] notifying date change 2, startYear: '${  this.state.startYear  }', startMonth: '${  this.state.startMonth  }, endYear: '${  this.state.endYear  }', endMonth: '${  this.state.endMonth  }'`);

        const newEventChecked = { ...this.state.eventChecked };
        for (const [eventName] of Object.entries(newEventChecked)) {
            newEventChecked[eventName] = false;
        }
        this.setState({
            eventChecked: newEventChecked,
            datesChanged: false
        });
        
        $('#loading').hide();

        this.fetchMonthlyEventCounts().then((data) => {

            /*
                Set all events to empty initially.
                
                Create a copy of eventsEmpty to avoid direct state mutation.
            */
            const updatedEventsEmpty = { ...this.state.eventsEmpty };
            for (let i = 0; i < eventNames.length; i++) {
                const eventNameCur = eventNames[i];
                updatedEventsEmpty[eventNameCur] = true;
            }

            for (const [eventName] of Object.entries(data)) {
                updatedEventsEmpty[eventName] = false;
            }

            this.setState({ eventsEmpty: updatedEventsEmpty }, () => {
                this.displayPlots(this.state.airframe);
            });

        });

    }

    airframeChange(airframe) {
        this.setState({airframe});
        this.displayPlots(airframe);
    }

    render() {

        const activePageName = (this.state.aggregatePage ? "aggregate_trends" : "trends");
        const timeHeaderTitle = (this.state.aggregatePage ? "Aggregate Event Trends" : "Event Trends");

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage={activePageName} darkModeOnClickAlt={() => {
                        this.displayPlots(this.state.airframe);
                    }} waitingUserCount={waitingUserCount} fleetManager={fleetManager}
                                    unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess}
                                    plotMapHidden={plotMapHidden}/>
                </div>

                <div className="container-fluid" style={{overflowY: "auto", flex: "1 1 auto"}}>

                    <div className="row">
                        <div className="col-lg-12" style={{paddingBottom: "64"}}>
                            <div className="card m-2">
                                <TimeHeader
                                    name={timeHeaderTitle}
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
                                <div className="card-body" style={{padding: "0"}}>
                                    <div className="row" style={{margin: "0", display: "flex", height: "100%"}}>
                                        <div className="col-lg-2" style={{padding: "8 8 8 8"}}>

                                            {
                                                eventNames.map((eventName, index) => {

                                                    //Don't show a description for the "ANY Event" event
                                                    if (eventName === "ANY Event") return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input"
                                                                   disabled={this.state.eventsEmpty[eventName]}
                                                                   type="checkbox" value="" id={`event-check-${  index}`}
                                                                   checked={this.state.eventChecked[eventName]}
                                                                   onChange={() => this.checkEvent(eventName)}></input>
                                                            <label className="form-check-label">
                                                                {eventName}
                                                            </label>
                                                        </div>
                                                    );

                                                    return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input"
                                                                   disabled={this.state.eventsEmpty[eventName]}
                                                                   type="checkbox"
                                                                   value=""
                                                                   id={`event-check-${  index}`}
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

                                        <div className="col-lg-10" style={{
                                            padding: "0 0 0 8",
                                            opacity: "0.80",
                                            display: "flex",
                                            flexDirection: "column",
                                            minHeight: "85vh",
                                            flex: "1 1 auto"
                                        }}>
                                            <div id="count-trends-plot" className="flex-fill" style={{
                                                flex: "1 1 auto",
                                                minHeight: "0",
                                                height: "100%",
                                                widhth: "100%"
                                            }}></div>
                                            <hr style={{margin: "0", borderTop: "8px solid var(--c_card_bg)"}}></hr>
                                            <div id="percent-trends-plot" className="flex-fill" style={{
                                                flex: "1 1 auto",
                                                minHeight: "0",
                                                height: "100%",
                                                widhth: "100%"
                                            }}></div>
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


export default TrendsPage;
