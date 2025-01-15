import 'bootstrap';

import React, { Component, useEffect } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import TimeHeader from "./time_header.js";
import GetDescription from "./get_description.js";

import Plotly from 'plotly.js';
import {OverlayTrigger} from "react-bootstrap";
import Tooltip from "react-bootstrap/Tooltip";
import { set } from 'ol/transform.js';


airframes.unshift("All Airframes");
var index = airframes.indexOf("Garmin Flight Display");
if (index !== -1) airframes.splice(index, 1);

tagNames.unshift("All Tags");
var tagIndex = tagNames.indexOf("Garmin Flight Display");
if (tagIndex !== -1) tagNames.splice(tagIndex, 1);

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

var eventCounts = null;
var eventSeverities = {};

var eventFleetPercents = {};
var eventNGAFIDPercents = {};

class SeveritiesPage extends React.Component {

    constructor(props) {

        super(props);

        let eventChecked = {};
        let eventsEmpty = {};

        //  eventNames.unshift("ANY Event");
        for (let i = 0; i < eventNames.length; i++) {

            let eventNameCur = eventNames[i];
            eventChecked[eventNameCur] = false;
            eventsEmpty[eventNameCur] = false;
        }

        var date = new Date();
        this.state = {
            airframe : "All Airframes",
            tagName: "All Tags",
            startYear : 2025,
            startMonth : 1,
            endYear : date.getFullYear(),
            endMonth : date.getMonth() + 1,
            datesChanged : false,
            eventMetaData : {},
            eventChecked : eventChecked,
            eventsEmpty : eventsEmpty,
            metaDataChecked: false,
            // severityTraces: [],
        };

        // //Fetch all event severities
        // this.fetchAllEventSeverities();

        //Fetch monthly event counts
        this.fetchMonthlyEventCounts();

    }

    componentDidMount() {
        this.displayPlot(this.state.airframe);
    }

    exportCSV() {
        let selectedAirframe = this.state.airframe;

        console.log("selected airframe: '" + selectedAirframe + "'");
        console.log(eventSeverities);
        let fileHeaders = "Event Name,Airframe,Flight ID,Start Time,End Time,Start Line,End Line,Severity";

        let fileContent = [fileHeaders];
        let uniqueMetaDataNames = [];

        for (let [eventName, countsMap] of Object.entries(eventSeverities)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            for (let [airframe, counts] of Object.entries(countsMap)) {
                if (airframe === "Garmin Flight Display") continue;
                if (selectedAirframe !== airframe && selectedAirframe !== "All Airframes") continue;
                console.log("Counts severities");
                console.log(counts);

                for (let i = 0; i < counts.length; i++) {
                    let line = "";
                    var eventMetaData = this.state.eventMetaData[counts[i].id];
                    console.log(eventMetaData);
                    var eventMetaDataText = [];
                    if (eventMetaData != null) {
                        eventMetaData.map((item) => {
                            if (!uniqueMetaDataNames.includes(item.name)) {
                                uniqueMetaDataNames.push(item.name);
                            }
                            eventMetaDataText.push(item.value);
                        })
                    }
                    let count = counts[i];
                    line = eventName + "," + airframe + "," + count.flightId + "," + count.startTime +  "," + count.endTime + "," + count.startLine + "," + count.endLine + "," + count.severity;
                    if (eventMetaDataText != "")
                        line += "," + eventMetaDataText.join(",");
                    fileContent.push(line);
                }
            }
        }

        if (uniqueMetaDataNames.length != 0)
            fileContent[0] = fileHeaders + "," +uniqueMetaDataNames.join(","); 
        let filename = "event_severities.csv";
        console.log("exporting csv!");

        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(fileContent.join("\n")));
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    }

    getEventMetaData(eventId) {
        var eventMetaData = null;
        var submissionData = {
            eventId : eventId
        };
        $.ajax({
            type: 'POST',
            url: '/protected/event_metadata',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                eventMetaData =  response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Event Metadata ", errorThrown);
            },
            async: false
        })
        //console.log("Event MetaData : ");
        //console.log(eventMetaData);

        return eventMetaData;

    }

    displayPlot(selectedAirframe) {

        console.log("Displaying plots with airframe: '" + selectedAirframe + "'");
        // console.log("Event Severities: ", eventSeverities);

        let severityTraces = [];
        var airframeNames = {};

        
        for (let [eventName, countsMap] of Object.entries(eventSeverities)) {
            
            if (!this.state.eventChecked[eventName])
                continue;

            console.log("Displaying checked event: '" + eventName + "'");

            for (let [airframe, counts] of Object.entries(countsMap)) {

                if (airframe === "Garmin Flight Display")
                    continue;

                if (selectedAirframe !== airframe && selectedAirframe !== "All Airframes")
                    continue;

                //Map airframe names to a consistent marker symbol
                const markerSymbolList = ["circle", "diamond", "square", "x", "pentagon", "hexagon", "octagon"];

                airframeNames[airframe] ??= Object.keys(airframeNames).length;
                let markerSymbol = markerSymbolList[airframeNames[airframe] % markerSymbolList.length];
                // console.log("Marker Symbol: ", markerSymbol);
                let markerSymbolAny = (markerSymbol + "-open-dot");

                let severityTrace = {
                    name : eventName + ' - ' + airframe,
                    type : 'scatter',
                    mode : 'markers',
                    hoverinfo : 'x+text',
                    hovertext : [], 
                    y : [], 
                    x : [],
                    z: [],
                    flightIds : [],
                    id: [],
                    systemId: [],
                    tail: [],
                    eventDefinitionId: [],
                    tagName: []
                };

                //Use a hollow circle for the "ANY Event" event
                if (eventName === "ANY Event") {

                    severityTrace.marker = {
                        color: 'gray',
                        size: 14,
                        symbol: markerSymbolAny,
                        opacity: 0.65
                    };

                } else {

                    let eventNameIndex = eventNames.indexOf(eventName);
                    severityTrace.marker = {
                        
                        //Consistent rainbow colors for each event
                        color:
                            'hsl('
                            + parseInt(360.0 * eventNameIndex / eventNames.length)
                            + ',100%'
                            + ',75%)',
                        symbol: markerSymbol,
                        size: 8,
                        line: {
                            color:'black',
                            width:1
                        }
                    };
                }

                for (let i = 0; i < counts.length; i++) {
                    severityTrace.id.push( counts[i].id );
                    severityTrace.y.push( counts[i].severity );
                    severityTrace.x.push( counts[i].startTime );
                    severityTrace.z.push( counts[i].endTime );
                    severityTrace.systemId.push( counts[i].systemId );
                    severityTrace.tail.push( counts[i].tail );
                    severityTrace.eventDefinitionId.push( counts[i].eventDefinitionId );
                    severityTrace.tagName.push( counts[i].tagName );

                    if (counts[i].eventDefinitionId === -1) {
                        severityTrace.flightIds.push( counts[i].flightId + " " + counts[i].otherFlightId);
                    } else {
                        severityTrace.flightIds.push( counts[i].flightId);
                    }

                  let hovertext = "Flight #" + counts[i].flightId +  ", System ID: " + counts[i].systemId +  ", Tail: " + counts[i].tail + ", severity: " + (Math.round(counts[i].severity * 100) / 100).toFixed(2) + ", event name: " + eventName + ", event start time: " + counts[i].startTime + ", event end time: " + counts[i].endTime;
                    if(counts[i].tagName !== ""){
                        hovertext += ", Tag: " + counts[i].tagName;
                    }

                    if (counts[i].eventDefinitionId == -1) hovertext += ", Proximity Flight #" + counts[i].otherFlightId;

                    //if (eventMetaDataText.length != 0) hovertext += ", " + eventMetaDataText.join(", ");

                    severityTrace.hovertext.push(hovertext);
                    //+ ", severity: " + counts[i].severity);
                }
                
                //Display the "ANY Event" markers under the other ones
                if (eventName === "ANY Event") {
                    severityTraces.unshift(severityTrace);
                } else {
                    severityTraces.push(severityTrace);
                }
                this.setState(this.state);
            }
        }

        let styles = getComputedStyle(document.documentElement);
        let plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        let plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        let plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        var severityLayout = {
            title : 'Severity of Events',
            hovermode : "closest",
            height: 700,
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

        Plotly.newPlot('severities-plot', severityTraces, severityLayout, config);

        let severitiesPlot = document.getElementById('severities-plot');
        severitiesPlot.on('plotly_click', function(data) {
            console.log("clicked on plot near point!");
            console.log(data);

            for (var i = 0; i < data.points.length; i++) {
                let index = data.points[i].pointIndex;
                let flightId = data.points[i].data.flightIds[index];
                let otherFlightId = null;
                if (typeof flightId === 'string' && flightId.indexOf(' ') >= 0) {
                    let parts = flightId.split(' ');
                    flightId = parts[0];
                    otherFlightId = parts[1];
                }

                console.log("point index of clicked point is: " + index + " with a flight id of: " + flightId);
                if (otherFlightId == null) {
                    window.open("/protected/flight?flight_id=" + flightId);
                } else {
                    window.open("/protected/flight?flight_id=" + otherFlightId + "&flight_id=" + flightId);
                }

                //might want to only open one tab if for some reason multiple points overlap (which shouldn't but might happen)
                //break;
            }
        });

        severitiesPlot.on('plotly_hover', (data) => {
            var point = data.points[0];
            var idIndex = point.pointIndex;
            var severityData =  point.data;
            var id = severityData.id[idIndex];
            var severity = severityData.y[idIndex];
            var startTime = severityData.x[idIndex];
            var tail = severityData.tail[idIndex];
            var eventDefinitionId = severityData.eventDefinitionId[idIndex];
            var endTime = severityData.z[idIndex];
            var systemId = severityData.systemId[idIndex];
            var tagName = severityData.tagName[idIndex];

            let flightId = severityData.flightIds[idIndex];
            let otherFlightId = null;
            if (typeof flightId === 'string' && flightId.indexOf(' ') >= 0) {
                let parts = flightId.split(' ');
                flightId = parts[0];
                otherFlightId = parts[1];
            }
            var eventMetaDataText = [];
            if (!this.state.eventMetaData[id]) {
                var eventMetaData = this.getEventMetaData(id);
                if (eventMetaData != null) {
                    eventMetaData.map((item) => {
                        eventMetaDataText.push(item.name + ": " + (Math.round(item.value * 100) / 100).toFixed(2));
                    });
                    this.state.eventMetaData[id] = eventMetaData;

                    eventMetaData.forEach((item) => {
                        eventMetaDataText.push(item.name + ": " + (Math.round(item.value * 100) / 100).toFixed(2));
                    });
                }
            }

            let hovertext = "Flight #" + flightId +  ", System ID: " + systemId +  ", Tail: " + tail + ", severity: " + (Math.round(severity * 100) / 100).toFixed(2) + ", event start time: " + startTime + ", event end time: " + endTime;

            if(tagName !== ""){
                hovertext += ", Tag: " + tagName;
            }
          
            if (eventDefinitionId === -1) hovertext += ", Proximity Flight #" + otherFlightId;

            if (eventMetaDataText.length !== 0) hovertext += ", " + eventMetaDataText.join(", ");
            severityData.hovertext = hovertext;
            this.setState(this.state);
            let index = severityTraces.findIndex(hash => hash.name === severityData.name);
            Plotly.restyle(severitiesPlot, {"hovertext": hovertext}, [index])
        });

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
                    let eventTypesDetected = Object.keys(eventCounts);
                    console.log("[EX] Events Detected: ", eventTypesDetected);
                    
                    //Mark all events as checked if they are detected and unchecked if they are not detected
                    for (let i = 0; i < eventNames.length; i++) {
                        let eventNameCur = eventNames[i];
                        severitiesPage.state.eventsEmpty[eventNameCur] = !eventTypesDetected.includes(eventNameCur);
                    }

                    severitiesPage.setState(severitiesPage.state);
                    severitiesPage.displayPlot(severitiesPage.state.airframe);

                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Uploads", errorThrown);
                    reject(errorThrown);
                },
            });
        });

    }

    /*
    fetchAllEventSeverities() {

        $('#loading').show();
        console.log("showing loading spinner!");


        let startDate = this.state.startYear + "-";
        let endDate = this.state.endYear + "-";

        //0 pad the months on the front
        if (parseInt(this.state.startMonth) < 10) startDate += "0" + parseInt(this.state.startMonth);
        else startDate += this.state.startMonth;
        if (parseInt(this.state.endMonth) < 10) endDate += "0" + parseInt(this.state.endMonth);
        else endDate += this.state.endMonth;

        let severitiesPage = this;

        var submission_data = {
            startDate : startDate + "-01",
            endDate : endDate + "-28",
            eventNames : JSON.stringify(eventNames),
            tagName: this.state.tagName
        };

        $.ajax({
            type: 'POST',
            url: '/protected/all_severities',
            data : submission_data,
            dataType : 'json',
            success : function(response) {
                console.log("Received response <all_severities>: ", this.data, response);

                $('#loading').hide();

                if (response.err_msg) {
                    errorModal.show(response.err_title, response.err_msg);
                    return;
                }

                //Check if the response is empty for each event
                for (let [eventName, countsMap] of Object.entries(response)) {

                    let eventSeverityCounts = response[eventName];
                    
                    let isEmpty = true;
                    for(let [airframe, counts] of Object.entries(eventSeverityCounts)) {

                        if (counts.length > 0) {
                            isEmpty = false;
                            break;
                        }
                    }
                    if (isEmpty) {
                        severitiesPage.state.eventsEmpty[eventName] = true;
                        eventSeverities[eventName] = {};
                    } else {

                        //Mark severities for event
                        severitiesPage.state.eventsEmpty[eventName] = false;
                        eventSeverities[eventName] = eventSeverityCounts;

                        // //Concatenate the counts for the "ANY Event" event
                        // if (eventSeverities["ANY Event"] == null)
                        //     eventSeverities["ANY Event"] = {};                        
                        // console.log("Merging counts for event: '" + eventName + "'");
                        // for(let [airframe, counts] of Object.entries(eventSeverityCounts)) {

                        //     if (eventSeverities["ANY Event"][airframe] == null)
                        //         eventSeverities["ANY Event"][airframe] = eventSeverityCounts[airframe];
                        //     else
                        //         eventSeverities["ANY Event"][airframe] = eventSeverities["ANY Event"][airframe].concat(eventSeverityCounts[airframe]);
                            
                        // }

                    }
                    
                }

                // severitiesPage.displayPlot(severitiesPage.state.airframe);
                severitiesPage.setState(severitiesPage.state);
                severitiesPage.displayPlot(severitiesPage.state.airframe);

            },

            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            }

        });

    }
    */

    fetchEventSeverities(eventName) {

        $('#loading').show();
        console.log("showing loading spinner!");


        let startDate = this.state.startYear + "-";
        let endDate = this.state.endYear + "-";

        //0 pad the months on the front
        if (parseInt(this.state.startMonth) < 10) startDate += "0" + parseInt(this.state.startMonth);
        else startDate += this.state.startMonth;
        if (parseInt(this.state.endMonth) < 10) endDate += "0" + parseInt(this.state.endMonth);
        else endDate += this.state.endMonth;


        //let severitiesPage = this;


        var submission_data = {
            startDate : startDate + "-01",
            endDate : endDate + "-28",
            eventName : eventName,
            tagName: this.state.tagName
        };

        $.ajax({
            type: 'POST',
            url: '/protected/severities',
            data : submission_data,
            dataType : 'json',
            async: true,
            success : function(response) {

                console.log("Received response <severities>: ", this.data, response);

                $('#loading').hide();

                if (response.err_msg) {
                    errorModal.show(response.err_title, response.err_msg);
                    return;
                }   

                // //Check if the response is empty
                // for(let [airframe, counts] of Object.entries(response)) {
                    
                //     if (counts.length != 0)
                //         continue;
                
                //     console.log("No counts for event: '" + eventName + "' and airframe: '" + airframe + "'");

                //     severitiesPage.state.eventsEmpty[eventName] = true;
                //     eventSeverities[eventName] = {};
                //     severitiesPage.setState(severitiesPage.state);
                //     return;
                // }

                eventSeverities[eventName] = response;
                severitiesPage.setState(severitiesPage.state);
                severitiesPage.displayPlot(severitiesPage.state.airframe);

            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            } 
        });

    }

    checkEvent(eventName) {

        console.log("Checking event: '" + eventName + "'");
        this.state.eventChecked[eventName] = !this.state.eventChecked[eventName];
        

        if (eventName in eventSeverities) {
            console.log("already loaded counts for event: '" + eventName + "'");
            severitiesPage.displayPlot(severitiesPage.state.airframe);
        } else {
            this.fetchEventSeverities(eventName);
        }

       // this.fetchEventSeverities(eventName);

        this.setState(this.state);

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
        console.log("[severitiescard] notifying date change 2, startYear: '" + this.state.startYear + "', startMonth: '" + this.state.startMonth + ", endYear: '" + this.state.endYear + "', endMonth: '" + this.state.endMonth + "'"); 

        for (let [eventName, value] of Object.entries(this.state.eventChecked)) {
            this.state.eventChecked[eventName] = false;
        }
        this.state.datesChanged = false;
        this.setState(this.state);

        eventSeverities = {};
        this.displayPlot(this.state.airframe);

        //this.fetchAllEventSeverities();
        this.fetchMonthlyEventCounts();
    }

    airframeChange(airframe) {
        this.setState({airframe}, () => this.displayPlot(airframe));
    }

    updateTags(tagName){
        this.setState({tagName, datesChanged : true});

    }
    tagNameChange(tagName) {
        this.setState({tagName}, ()=> this.displayPlot(this.state.airframe));

    }

    updateTags(tagName){
        this.setState({tagName, datesChanged : true});

    }
    tagNameChange(tagName) {
        this.setState({tagName}, ()=> this.displayPlot(this.state.airframe));

    }

    render() {
        //console.log(systemIds);

        const numberOptions = { 
            minimumFractionDigits: 2,
            maximumFractionDigits: 2 
        };

        return (
            <div style={{overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar activePage={"severities"} darkModeOnClickAlt={()=>{this.displayPlot(this.state.airframe);}} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div className="container-fluid" style={{overflowY:"auto", flex:"1 1 auto"}}>

                    <div className="row">
                        <div className="col-lg-12">
                            <div className="card mb-2 m-2">
                                <TimeHeader
                                    name="Event Severities"
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
                                    tagNames={tagNames}
                                    tagName={this.state.tagName}
                                    tagNameChange={(tagName) => this.tagNameChange(tagName)}
                                />

                            <div className="card-body" style={{padding:"0"}}>
                                <div className="row" style={{margin:"0"}}>
                                    <div className="col-lg-2" style={{padding:"8 8 8 8"}}>

                                        {
                                            eventNames.map((eventName, index) => {

                                                //Don't show a description for the "ANY Event" event
                                                if (eventName === "ANY Event") return (
                                                    <div key={index} className="form-check">
                                                        <input className="form-check-input" type="checkbox" value="" id={"event-check-" + index} checked={this.state.eventChecked[eventName]} onChange={() => this.checkEvent(eventName)} style={{border:"1px solid red"}}/>
                                                        <label className="form-check-label">
                                                            {eventName}
                                                        </label>
                                                    </div>
                                                );

                                                return (
                                                    <div key={index} className="form-check">
                                                        <input disabled={this.state.eventsEmpty[eventName]} className="form-check-input" type="checkbox" value="" id={"event-check-" + index} checked={this.state.eventChecked[eventName]} onChange={() => this.checkEvent(eventName)}></input>

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
                                        <div id="severities-plot"></div>
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


var severitiesPage = ReactDOM.render(
    <SeveritiesPage />,
    document.querySelector('#severities-page')
);

severitiesPage.displayPlot("All Airframes");