import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import TimeHeader from "./time_header.js";
import GetDescription from "./get_description.js";

import Plotly from 'plotly.js';
import {OverlayTrigger} from "react-bootstrap";
import Tooltip from "react-bootstrap/Tooltip";


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

var eventSeverities = {};

var eventFleetPercents = {};
var eventNGAFIDPercents = {};

class SeveritiesPage extends React.Component {
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

            eventChecked : eventChecked
        };
    }

    exportCSV() {
        let selectedAirframe = this.state.airframe;

        console.log("selected airframe: '" + selectedAirframe + "'");

        console.log(eventSeverities);


        let filetext = "Event Name,Airframe,Flight ID,Start Time,End Time,Start Line,End Line,Severity\n";

        let needsComma = false;

        for (let [eventName, countsMap] of Object.entries(eventSeverities)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            for (let [airframe, counts] of Object.entries(countsMap)) {
                if (airframe === "Garmin Flight Display") continue;
                if (selectedAirframe !== airframe && selectedAirframe !== "All Airframes") continue;


                for (let i = 0; i < counts.length; i++) {
                    let count = counts[i];
                    filetext += eventName + "," + airframe + "," + count.flightId + "," + count.startTime +  "," + count.endTime + "," + count.startLine + "," + count.endLine + "," + count.severity + "\n";
                }
            }
        }


        let filename = "event_severities.csv";

        console.log("exporting csv!");

        var element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(filetext));
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
        console.log("Event MetaData : ");
        console.log(eventMetaData);

        return eventMetaData;

    }

    displayPlot(selectedAirframe) {
        console.log("displaying plots with airframe: '" + selectedAirframe + "'");

        let severityTraces = [];

        for (let [eventName, countsMap] of Object.entries(eventSeverities)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            for (let [airframe, counts] of Object.entries(countsMap)) {

                if (airframe === "Garmin Flight Display") continue;
                if (selectedAirframe !== airframe && selectedAirframe !== "All Airframes") continue;

                let severityTrace = {
                    name : eventName + ' - ' + airframe,
                    type : 'scatter',
                    mode : 'markers',
                    hoverinfo : 'x+text',
                    hovertext : [], 
                    y : [], 
                    x : [],
                    flightIds : [],
                };

                //console.log("events:");
                for (let i = 0; i < counts.length; i++) {
                    var eventMetaData = this.getEventMetaData(counts[i].id);
                    var eventMetaDataStr = "";
                    if (eventMetaData != null) {
                        eventMetaData.map((item) => {
                            eventMetaDataStr += item.name + ": " +  (Math.round(item.value * 100) / 100).toFixed(2) + ", ";
                        })
                    }
                    severityTrace.y.push( counts[i].severity );
                    severityTrace.x.push( counts[i].startTime );

                    if (counts[i].eventDefinitionId == -1) {
                        severityTrace.flightIds.push( counts[i].flightId + " " + counts[i].otherFlightId);
                    } else {
                        severityTrace.flightIds.push( counts[i].flightId);
                    }

                    let hovertext = "Flight #" + counts[i].flightId +  ", System ID: " + counts[i].systemId +  ", Tail: " + counts[i].tail + ", severity: " + (Math.round(counts[i].severity * 100) / 100).toFixed(2) + ", event start time: " + counts[i].startTime + ", event end time: " + counts[i].endTime;
                    if (counts[i].eventDefinitionId == -1) hovertext += ", Proximity Flight #" + counts[i].otherFlightId;

                    if (eventMetaDataStr != "") hovertext += ", " + eventMetaDataStr;

                    severityTrace.hovertext.push(hovertext);
                    //+ ", severity: " + counts[i].severity);
                }

                severityTraces.push(severityTrace);
            }
        }

        var severityLayout = {
            title : 'Severity of Events',
            hovermode : "closest",
            //autosize: false,
            //width: 500,
            height: 700,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
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

        if (eventName in eventSeverities) {
            console.log("already loaded counts for event: '" + eventName + "'");
            severitiesPage.displayPlot(severitiesPage.state.airframe);

        } else {
            $('#loading').show();
            console.log("showing loading spinner!");

            let severitiesPage = this;

            $.ajax({
                type: 'POST',
                url: '/protected/severities',
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

                    eventSeverities[eventName] = response;
                    severitiesPage.displayPlot(severitiesPage.state.airframe);
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
        console.log("[severitiescard] notifying date change 2, startYear: '" + this.state.startYear + "', startMonth: '" + this.state.startMonth + ", endYear: '" + this.state.endYear + "', endMonth: '" + this.state.endMonth + "'"); 

        for (let [eventName, value] of Object.entries(this.state.eventChecked)) {
            this.state.eventChecked[eventName] = false;
        }
        this.state.datesChanged = false;
        this.setState(this.state);

        eventSeverities = {};
        this.displayPlot(this.state.airframe);
    }

    airframeChange(airframe) {
        this.setState({airframe});
        this.displayPlot(airframe);
    }

    render() {
        //console.log(systemIds);

        const numberOptions = { 
            minimumFractionDigits: 2,
            maximumFractionDigits: 2 
        };

        return (
            <div>
                <SignedInNavbar activePage={"severities"} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="container-fluid">

                    <div className="row">
                        <div className="col-lg-12">
                            <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
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
                                />

                            <div className="card-body" style={{padding:"0"}}>
                                <div className="row" style={{margin:"0"}}>
                                    <div className="col-lg-2" style={{padding:"8 8 8 8"}}>

                                        {
                                            eventNames.map((eventName, index) => {
                                                return (
                                                    <div key={index} className="form-check">
                                                        <input className="form-check-input" type="checkbox" value="" id={"event-check-" + index} checked={this.state.eventChecked[eventName]} onChange={() => this.checkEvent(eventName)}></input>

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

                                    <div className="col-lg-10" style={{padding:"0 0 0 8"}}>
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
