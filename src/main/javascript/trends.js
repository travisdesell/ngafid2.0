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
const index = airframes.indexOf("Garmin Flight Display");
if (index !== -1)
    airframes.splice(index, 1);

eventNames.sort();


const countData = [];
const percentData = [];

var eventCounts = null;

let eventFleetPercents = {};
let eventNGAFIDPercents = {};

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

        const dateCurrent = new Date();
        this.state = {

            airframe : "All Airframes",

            //Start at start of calendar year
            startYear : dateCurrent.getFullYear(),
            startMonth : 1, //<-- January

            //End at current month of current year
            endYear : dateCurrent.getFullYear(),
            endMonth : dateCurrent.getMonth() + 1,

            datesChanged : false,
            aggregatePage : props.aggregate_page,
            eventChecked : eventChecked,
            eventsEmpty : eventsEmpty,

            performedSuccessfulFetch : false,

        };
        
        this.fetchMonthlyEventCounts();
    }

    fetchMonthlyEventCounts() {

        /*
            IMPORTANT:

            Dates require leading zeroes for the
            months and days, so ternary operators
            are used to append a leading zero
            if the month or day is less than 10.
        */

        const startDate = `${this.state.startYear}-${this.state.startMonth<10?'0':''}${this.state.startMonth}-01`;

        const endDate = (() => {

            //Append final day of the month to the end date
            let endDateOut = `${this.state.endYear}-${this.state.endMonth<10?'0':''}${this.state.endMonth}-`;

            let endDayOut = 0;
            switch (parseInt(this.state.endMonth)) {

                case 1: //January
                case 3: //March
                case 5: //May
                case 7: //July
                case 8: //August
                case 10: //October
                case 12: //December
                    endDayOut = 31;
                    break;

                case 4: //April
                case 6: //June
                case 9: //September
                case 11: //November
                    endDayOut = 30
                    break;

                case 2: //February
                    if ((this.state.endYear % 4 === 0 && this.state.endYear % 100 !== 0) || (this.state.endYear % 400 === 0))
                        endDayOut = 29 //<-- Leap year, February has 29 days
                    else
                        endDayOut = 28 //<-- Non-leap year, February has 28 days
                    break;

            }

            endDateOut += `${endDayOut<10?'0':''}${endDayOut}`;

            return endDateOut;

        })();

        const submission_data = {
            startDate : startDate,
            endDate : endDate,
            aggregatePage : this.props.aggregate_page
        };

        console.log("Fetching monthly event counts with data: ", submission_data);

        const trendsPage = this;
        
        return new Promise((resolve, reject) => {
            $.ajax({
                type: 'POST',
                url: '/protected/monthly_event_counts',
                data : submission_data,
                dataType : 'json',
                async: true,
                success : function(response) {

                    if (response.err_msg) {
                        errorModal.show(response.err_title, response.err_msg);
                        return;
                    }   

                    eventCounts = response;
                    
                    const countsMerged = {};
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
            
            if (!this.state.eventChecked[eventName])
                continue;

            //Ensure the eventNames array is unique names only
            if (!eventNames.includes(eventName))
                eventNames.push(eventName);

            for (let [airframe, value] of Object.entries(countsObject)) {

                if (value.airframeName === "Garmin Flight Display")
                    continue;

                if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes")
                    continue;

                let airframeName = value.airframeName;
                let valueDates = value.dates;


                //Ensure the airframeNames array is unique names only
                if (!airframeNames.includes(airframeName))
                    airframeNames.push(airframeName);

                console.log("Dates: ", value.dates);

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

                    //Ensure the dates array is unique dates only
                    if (!dates.includes(date))
                        dates.push(date);

                    if (!(eventName in csvValues))
                        csvValues[eventName] = {};
                
                    if (!(airframeName in csvValues[eventName]))
                        csvValues[eventName][airframeName] = {};

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

                if (airframeName in csvValues[eventName])
                    console.log("\t" + eventName + " - " + airframeName + " has " + Object.keys(csvValues[eventName][airframeName]).length + " entries!");
                
            }

        }

        let filetext = "";

        let needsComma = false;
        for (let eventName of eventNames) {

            for (let airframeName of airframeNames) {

                if (airframeName in csvValues[eventName]) {

                    if (needsComma)
                        filetext += ",";
                    else
                        needsComma = true;

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

                    if (needsComma)
                        filetext += ",";
                    else
                        needsComma = true;
                    
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

                    if (needsComma)
                        filetext += ",";
                    else
                        needsComma = true;
                    
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

                        if (needsComma)
                            filetext += ",";
                        else
                            needsComma = true;

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

        console.log("eventNames:", eventNames);
        console.log("airframeNames:", airframeNames);
        console.log("dates:", dates);

        const filename = "trends.csv";
        console.log("Exporting .csv: ", filename);

        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(filetext));
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    }

    async displayPlots(selectedAirframe) {

        //Show Loading Spinner
        $('#loading').show();


        console.log("Displaying plots with airframe: '", selectedAirframe, "'");

        eventFleetPercents = {};
        eventNGAFIDPercents = {};

        countData.length = 0;
        percentData.length = 0;



        let counts = (eventCounts == null ? {} : eventCounts);

        let airframeNames = [];
        for (let [eventName, countsObject] of Object.entries(counts)) {

            for (let [airframe, value] of Object.entries(countsObject)) {

                if (value.airframeName === "Garmin Flight Display")
                    continue;

                if (!airframeNames.includes(value.airframeName))
                    airframeNames.push(value.airframeName);

            }

        }

        for (let [eventName, countsObject] of Object.entries(counts)) {

            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");

            if (!this.state.eventChecked[eventName])
                continue;


            let fleetPercents = null;
            let ngafidPercents = null;

            if (eventName in eventFleetPercents) {

                console.log('Getting existing fleetPercents...');

                fleetPercents = eventFleetPercents[eventName];
                ngafidPercents = eventNGAFIDPercents[eventName];

            } else {

                console.log('Setting initial fleetPercents...');

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

                //Aggregate Page, show "All Fleets"
                if (this.state.aggregatePage)
                    ngafidPercentsName += "All Fleets";

                //Non-Aggregate Page, show "All Other Fleets"
                else
                    ngafidPercentsName += "All Other Fleets";
                
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

                //Airframe name is 'Garmin Flight Display', skip
                if (value.airframeName === "Garmin Flight Display")
                    continue;

                //Current airframe name is neither the selected airframe name or 'All Airframes', skip
                if ((selectedAirframe !== value.airframeName) && (selectedAirframe !== "All Airframes"))
                    continue;
        

                value.name = value.eventName + " - " + value.airframeName;
                value.x = value.dates;
                value.type = 'scatter';
                value.hoverinfo = 'x+text';

                //Event name is 'ANY Event'
                if (eventName === "ANY Event") {

                    value = {
                        ...value,

                        legendgroup: value.name,

                        //Dashed lines for 'ANY Event'
                        line : {
                            dash: 'dot'
                        }

                    };

                //Event is NOT 'ANY Event'
                } else {

                    value = {
                        ...value,

                        legendgroup: value.name,
                        mode : 'lines',

                        //Standard lines for non-'ANY Event'
                        line : {
                            width : 2,
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
                    let date = value.dates[i];

                    //don't add airframes to the fleet percentage plot that the fleet doesn't have
                    if (airframes.indexOf(value.airframeName) >= 0 && !this.state.aggregatePage) {

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

                for (let i = 0; i < value.dates.length; i++) {

                    let flightsWithEventCount;
                    let totalFlightsCount;

                    //Aggregate page, add to aggregate counts
                    if (this.state.aggregatePage) {
                        flightsWithEventCount = value.aggregateFlightsWithEventCounts[i];
                        totalFlightsCount = value.aggregateTotalFlightsCounts[i];

                    //Non-aggregate page, add to non-aggregate counts
                    } else {
                        flightsWithEventCount = value.flightsWithEventCounts[i];
                        totalFlightsCount = value.totalFlightsCounts[i];
                    }

                    value.hovertext.push(value.y[i] + " events in " + flightsWithEventCount + " of " + totalFlightsCount + " flights : " + value.eventName + " - " + value.airframeName);

                }
                
            }

        }

        for (const airframeName of airframeNames) {

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
                }

            };

            countData.push(airframeLegendHighlight);

        }

        for (let [eventName, fleetValue] of Object.entries(eventFleetPercents)) {

            //FLEET VALUE -- Event name is 'ANY Event'
            if (eventName === "ANY Event") {

                fleetValue = {
                    ...fleetValue,
                    legendgroup: fleetValue.name,
                    line : {
                        dash: 'dot' //<-- Dashed lines for 'ANY Event'
                    }

                };

            //FLEET VALUE -- Event is NOT 'ANY Event'
            } else {

                fleetValue = {
                    ...fleetValue,
                    legendgroup: fleetValue.name,
                    mode : 'lines',
                    line : {
                        width : 2,  //<-- Standard lines for non-'ANY Event'
                    }

                };

            }

            //Push fleet values...
            if (!this.state.aggregatePage) {

                percentData.push(fleetValue);

                fleetValue.x = [];
                fleetValue.y = [];

                for (let date of Object.keys(fleetValue.flightsWithEventCounts).sort()) {

                    fleetValue.x.push(date);

                    let v = 100.0 * (parseFloat(fleetValue.flightsWithEventCounts[date]) / parseFloat(fleetValue.totalFlightsCounts[date]));
                    if (isNaN(v))
                        v = 0.0;

                    fleetValue.y.push(v);

                    //Gives 2 significant figures (and leading 0s if it is quite small)
                    var fixedText = "";
                    if (v > 0.00 && v < 1.00)
                        fixedText = v.toFixed(-Math.ceil(Math.log10(v)) + 2) + "%";
                    else
                        fixedText = v.toFixed(2) + "%";
                    
                    fleetValue.hovertext.push(fixedText  + " (" + fleetValue.flightsWithEventCounts[date] + " of " + fleetValue.totalFlightsCounts[date] + " flights) : " + fleetValue.name);
                    
                }
            }

            //Push NGAFID data...
            let ngafidValue = eventNGAFIDPercents[eventName];

            //NGAFID VALUE -- Event name is 'ANY Event'
            if (eventName === "ANY Event") {

                ngafidValue = {
                    ...ngafidValue,
                    legendgroup: ngafidValue.name,
                    line : {
                        dash: 'dot' //<-- Dashed lines for 'ANY Event'
                    }

                };

            //NGAFID VALUE -- Event is NOT 'ANY Event'
            } else {

                ngafidValue = {
                    ...ngafidValue,
                    legendgroup: ngafidValue.name,
                    mode : 'lines',
                    line : {
                        width : 2,  //<-- Standard lines for non-'ANY Event'
                    }

                };

            }

            percentData.push(ngafidValue);
            ngafidValue.x = [];
            ngafidValue.y = [];
            for (let date of Object.keys(ngafidValue.flightsWithEventCounts).sort()) {
                ngafidValue.x.push(date);

                let v = 100.0 * parseFloat(ngafidValue.flightsWithEventCounts[date]) / parseFloat(ngafidValue.totalFlightsCounts[date]);
                if (isNaN(v))
                    v = 0.0;

                ngafidValue.y.push(v);

                //Gives 2 significant figures (and leading 0s if it is quite small)
                var fixedText = "";
                if (v > 0.00 && v < 1.00) 
                    fixedText = v.toFixed(-Math.ceil(Math.log10(v)) + 2) + "%";
                else 
                    fixedText = v.toFixed(2) + "%";
                
                ngafidValue.hovertext.push(fixedText + " (" + ngafidValue.flightsWithEventCounts[date] + " of " + ngafidValue.totalFlightsCounts[date] + " flights) : " + ngafidValue.name);

            }

        }


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

        Plotly.newPlot('count-trends-plot', countData, countLayout, config);
        Plotly.newPlot('percent-trends-plot', percentData, percentLayout, config);

        //Hide loading spinner
        $('#loading').hide();

    }


    checkEvent(eventName) {

        console.log("checking event: '" + eventName + "'");
        this.state.eventChecked[eventName] = !this.state.eventChecked[eventName];
        this.setState(this.state);

        this.displayPlots(this.state.airframe);

    }

    updateStartYear(newStartYear) {

        console.log("Setting new start year to: ", newStartYear);
        this.setState({startYear : newStartYear, datesChanged : true});
        console.log(this.state);

    }

    updateStartMonth(newStartMonth) {

        console.log("Setting new start month to: ", newStartMonth);
        this.setState({startMonth : newStartMonth, datesChanged : true});
        console.log(this.state);

    }

    updateEndYear(newEndYear) {

        console.log("Setting new end year to: ", newEndYear);
        this.setState({endYear : newEndYear, datesChanged : true});
        console.log(this.state);

    }

    updateEndMonth(newEndMonth) {

        console.log("Setting new end month to: ", newEndMonth);
        this.setState({endMonth : newEndMonth, datesChanged : true});
        console.log(this.state);

    }

    async delay() {
        return new Promise(resolve => setTimeout(resolve, 1000));
    }

    async dateChange() {

        //Show Loading Spinner
        $('#loading').show();

        //[EX] For Testing -- Wait 1 second
        //  await this.delay();

        console.log(
            "[trendscard] notifying date change 2, startYear: '", this.state.startYear,
            "', startMonth: '", this.state.startMonth,
            ", endYear: '", this.state.endYear,
            "', endMonth: '" + this.state.endMonth + "'"
        ); 

        for (let [eventName, value] of Object.entries(this.state.eventChecked)) {
            this.state.eventChecked[eventName] = false;
        }

        this.state.datesChanged = false;
        

        this.fetchMonthlyEventCounts().then((data) => {

            //Set all events to empty initially
            for (let i = 0; i < eventNames.length; i++) {
                let eventNameCur = eventNames[i];
                this.state.eventsEmpty[eventNameCur] = true;
            }

            const dataEntries = Object.entries(data);
            for (let [eventName, countsObject] of dataEntries) {
                this.state.eventsEmpty[eventName] = false;
            }

            console.log("Data Entries: ", dataEntries);
            this.state.eventsEmpty["ANY Event"] = (dataEntries.length <= 1); //<-- Data is empty/only has "ANY Event"

            this.setState(this.state);
            this.displayPlots(this.state.airframe);

        });

        //Hide loading spinner
        $('#loading').hide();        

    }

    airframeChange(airframe) {

        this.setState({airframe});
        this.displayPlots(airframe);

    }

    render() {

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
