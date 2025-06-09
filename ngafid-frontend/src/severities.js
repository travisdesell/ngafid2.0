import 'bootstrap';

import React from "react";
import ReactDOM from "react-dom";

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import {TimeHeader} from "./time_header.js";
import GetDescription from "./get_description.js";

import Plotly from 'plotly.js';
import {OverlayTrigger} from "react-bootstrap";
import Tooltip from "react-bootstrap/Tooltip";


import "./index.css";


airframes.unshift("All Airframes");
const index = airframes.indexOf("Garmin Flight Display");
if (index !== -1)
    airframes.splice(index, 1);

tagNames.unshift("All Tags");
const tagIndex = tagNames.indexOf("Garmin Flight Display");
if (tagIndex !== -1)
    tagNames.splice(tagIndex, 1);

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

let eventSeverities = {};

class SeveritiesPage extends React.Component {

    constructor(props) {

        super(props);

        const eventChecked = {};
        const eventsEmpty = {};

        //  eventNames.unshift("ANY Event");    (⚠ ANY Event disabled on the severities page for now)
        for (let i = 0; i < eventNames.length; i++) {

            const eventNameCur = eventNames[i];
            eventChecked[eventNameCur] = false;
            eventsEmpty[eventNameCur] = false;
        }

        const date = new Date();
        this.state = {
            airframe: "All Airframes",
            tagName: "All Tags",
            startYear: 2020,
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            datesChanged: false,
            eventMetaData: {},
            eventChecked: eventChecked,
            eventsEmpty: eventsEmpty,
            metaDataChecked: false,
            // severityTraces: [],
        };

        //Fetch all event severities
        this.fetchAllEventSeverities();

    }

    componentDidMount() {

        this.displayPlot(this.state.airframe);
        this.displayPlot("All Airframes");

    }

    exportCSV() {
        const selectedAirframe = this.state.airframe;

        console.log(`selected airframe: '${  selectedAirframe  }'`);
        console.log(eventSeverities);
        const fileHeaders = "Event Name,Airframe,Flight ID,Start Time,End Time,Start Line,End Line,Severity";

        const fileContent = [fileHeaders];
        const uniqueMetaDataNames = [];

        for (const [eventName, countsMap] of Object.entries(eventSeverities)) {
            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName]) continue;

            for (const [airframe, counts] of Object.entries(countsMap)) {
                if (airframe === "Garmin Flight Display") continue;
                if (selectedAirframe !== airframe && selectedAirframe !== "All Airframes") continue;
                console.log("Counts severities");
                console.log(counts);

                for (let i = 0; i < counts.length; i++) {
                    let line = "";
                    const eventMetaData = this.state.eventMetaData[counts[i].id];
                    console.log(eventMetaData);
                    const eventMetaDataText = [];
                    if (eventMetaData != null) {
                        eventMetaData.map((item) => {
                            if (!uniqueMetaDataNames.includes(item.name)) {
                                uniqueMetaDataNames.push(item.name);
                            }
                            eventMetaDataText.push(item.value);
                        });
                    }
                    const count = counts[i];
                    line = `${eventName  },${  airframe  },${  count.flightId  },${  count.startTime  },${  count.endTime  },${  count.startLine  },${  count.endLine  },${  count.severity}`;
                    if (eventMetaDataText != "")
                        line += `,${  eventMetaDataText.join(",")}`;
                    fileContent.push(line);
                }
            }
        }

        if (uniqueMetaDataNames.length != 0)
            fileContent[0] = `${fileHeaders  },${  uniqueMetaDataNames.join(",")}`;
        const filename = "event_severities.csv";
        console.log("exporting csv!");

        const element = document.createElement('a');
        element.setAttribute('href', `data:text/plain;charset=utf-8,${  encodeURIComponent(fileContent.join("\n"))}`);
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    }

    getEventMetaData(eventId) {
        let eventMetaData = null;
        $.ajax({
            type: 'GET',
            url: `/api/event/${eventId}/meta`,
            dataType: 'json',
            async: false,
            success: (response) => {
                eventMetaData = response;
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Event Metadata ", errorThrown);
            },
        });
        

        return eventMetaData;

    }

    displayPlot(selectedAirframe) {

        console.log(`Displaying plots with airframe: '${  selectedAirframe  }'`);
        // console.log("Event Severities: ", eventSeverities);

        const severityTraces = [];
        const airframeNames = {};


        for (const [eventName, countsMap] of Object.entries(eventSeverities)) {

            if (!this.state.eventChecked[eventName])
                continue;

            for (const [airframe, counts] of Object.entries(countsMap)) {

                if (airframe === "Garmin Flight Display")
                    continue;

                if (selectedAirframe !== airframe && selectedAirframe !== "All Airframes")
                    continue;

                //Map airframe names to a consistent marker symbol
                const markerSymbolList = ["circle", "diamond", "square", "x", "pentagon", "hexagon", "octagon"];

                airframeNames[airframe] ??= Object.keys(airframeNames).length;
                const markerSymbol = markerSymbolList[airframeNames[airframe] % markerSymbolList.length];
                // console.log("Marker Symbol: ", markerSymbol);
                const markerSymbolAny = (`${markerSymbol  }-open-dot`);

                const severityTrace = {
                    name: `${eventName  } - ${  airframe}`,
                    type: 'scatter',
                    mode: 'markers',
                    hoverinfo: 'x+text',
                    hovertext: [],
                    y: [],
                    x: [],
                    z: [],
                    flightIds: [],
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

                    const eventNameIndex = eventNames.indexOf(eventName);
                    severityTrace.marker = {

                        //Consistent rainbow colors for each event
                        color:
                            `hsl(${
                             parseInt(360.0 * eventNameIndex / eventNames.length)
                             },100%`
                            + `,75%)`,
                        symbol: markerSymbol,
                        size: 8,
                        line: {
                            color: 'black',
                            width: 1
                        }
                    };
                }

                for (let i = 0; i < counts.length; i++) {
                    severityTrace.id.push(counts[i].id);
                    severityTrace.y.push(counts[i].severity);
                    severityTrace.x.push(counts[i].startTime);
                    severityTrace.z.push(counts[i].endTime);
                    severityTrace.systemId.push(counts[i].systemId);
                    severityTrace.tail.push(counts[i].tail);
                    severityTrace.eventDefinitionId.push(counts[i].eventDefinitionId);
                    severityTrace.tagName.push(counts[i].tagName);

                    if (counts[i].eventDefinitionId === -1) {
                        severityTrace.flightIds.push(`${counts[i].flightId  } ${  counts[i].otherFlightId}`);
                    } else {
                        severityTrace.flightIds.push(counts[i].flightId);
                    }

                    let hovertext = `Flight #${  counts[i].flightId  }, System ID: ${  counts[i].systemId  }, Tail: ${  counts[i].tail  }, severity: ${  (Math.round(counts[i].severity * 100) / 100).toFixed(2)  }, event name: ${  eventName  }, event start time: ${  counts[i].startTime  }, event end time: ${  counts[i].endTime}`;
                    if (counts[i].tagName !== "") {
                        hovertext += `, Tag: ${  counts[i].tagName}`;
                    }

                    if (counts[i].eventDefinitionId == -1) hovertext += `, Proximity Flight #${  counts[i].otherFlightId}`;

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

        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        const severityLayout = {
            title : {text: 'Severity of Events'},
            hovermode : "closest",
            autosize: true,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
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

        Plotly.newPlot('severities-plot', severityTraces, severityLayout, config);

        const severitiesPlot = document.getElementById('severities-plot');
        severitiesPlot.on('plotly_click', function (data) {
            console.log("clicked on plot near point!");
            console.log(data);

            for (let i = 0; i < data.points.length; i++) {
                const index = data.points[i].pointIndex;
                let flightId = data.points[i].data.flightIds[index];
                let otherFlightId = null;
                if (typeof flightId === 'string' && flightId.indexOf(' ') >= 0) {
                    const parts = flightId.split(' ');
                    flightId = parts[0];
                    otherFlightId = parts[1];
                }

                console.log(`point index of clicked point is: ${  index  } with a flight id of: ${  flightId}`);
                if (otherFlightId == null) {
                    window.open(`/protected/flight?flight_id=${  flightId}`);
                } else {
                    window.open(`/protected/flight?flight_id=${  otherFlightId  }&flight_id=${  flightId}`);
                }

                //might want to only open one tab if for some reason multiple points overlap (which shouldn't but might happen)
                //break;
            }
        });

        severitiesPlot.on('plotly_hover', (data) => {
            const point = data.points[0];
            const idIndex = point.pointIndex;
            const severityData = point.data;
            const id = severityData.id[idIndex];
            const severity = severityData.y[idIndex];
            const startTime = severityData.x[idIndex];
            const tail = severityData.tail[idIndex];
            const eventDefinitionId = severityData.eventDefinitionId[idIndex];
            const endTime = severityData.z[idIndex];
            const systemId = severityData.systemId[idIndex];
            const tagName = severityData.tagName[idIndex];

            let flightId = severityData.flightIds[idIndex];
            let otherFlightId = null;
            if (typeof flightId === 'string' && flightId.indexOf(' ') >= 0) {
                const parts = flightId.split(' ');
                flightId = parts[0];
                otherFlightId = parts[1];
            }
            const eventMetaDataText = [];
            if (!this.state.eventMetaData[id]) {
                const eventMetaData = this.getEventMetaData(id);
                if (eventMetaData != null) {
                    eventMetaData.map((item) => {
                        eventMetaDataText.push(`${item.name  }: ${  (Math.round(item.value * 100) / 100).toFixed(2)}`);
                    });
                    this.setState(prevState => {
                        const updatedEventMetaData = { ...prevState.eventMetaData, [id]: eventMetaData };
                        return { eventMetaData: updatedEventMetaData };
                    });

                    eventMetaData.forEach((item) => {
                        eventMetaDataText.push(`${item.name  }: ${  (Math.round(item.value * 100) / 100).toFixed(2)}`);
                    });
                }
            }

            let hovertext = `Flight #${  flightId  }, System ID: ${  systemId  }, Tail: ${  tail  }, severity: ${  (Math.round(severity * 100) / 100).toFixed(2)  }, event start time: ${  startTime  }, event end time: ${  endTime}`;

            if (tagName !== "") {
                hovertext += `, Tag: ${  tagName}`;
            }

            if (eventDefinitionId === -1) hovertext += `, Proximity Flight #${  otherFlightId}`;

            if (eventMetaDataText.length !== 0) hovertext += `, ${  eventMetaDataText.join(", ")}`;
            severityData.hovertext = hovertext;
            this.setState(this.state);
            const index = severityTraces.findIndex(hash => hash.name === severityData.name);
            Plotly.restyle(severitiesPlot, {"hovertext": hovertext}, [index]);
        });

    }

    fetchAllEventSeverities() {

        $('#loading').show();
        console.log("showing loading spinner!");


        let startDate = `${this.state.startYear  }-`;
        let endDate = `${this.state.endYear  }-`;

        //0 pad the months on the front
        if (parseInt(this.state.startMonth) < 10) startDate += `0${  parseInt(this.state.startMonth)}`;
        else startDate += this.state.startMonth;
        if (parseInt(this.state.endMonth) < 10) endDate += `0${  parseInt(this.state.endMonth)}`;
        else endDate += this.state.endMonth;

        const submissionData = {
            startDate: `${startDate  }-01`,
            endDate: `${endDate  }-28`,
            eventNames: JSON.stringify(eventNames),
            tagName: this.state.tagName
        };

        $.ajax({
            type: 'GET',
            url: '/api/event/severities',
            data: submissionData,
            success: (response) => {

                console.log("Received response <all_severities>: ", this.data, response);

                $('#loading').hide();

                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                //Check if the response is empty for each event
                for (const [eventName] of Object.entries(response)) {

                    const eventSeverityCounts = response[eventName];

                    let isEmpty = true;
                    for (const [counts] of Object.entries(eventSeverityCounts)) {

                        if (counts.length > 0) {
                            isEmpty = false;
                            break;
                        }

                    }

                    if (isEmpty) {

                        this.setState(prevState => ({
                            eventsEmpty: { ...prevState.eventsEmpty, [eventName]: true }
                        }));

                        eventSeverities[eventName] = {};

                    } else {

                        //Mark severities for event
                        this.setState(prevState => ({
                            eventsEmpty: { ...prevState.eventsEmpty, [eventName]: false }
                        }));

                        eventSeverities[eventName] = eventSeverityCounts;

                        //Concatenate the counts for the "ANY Event" event
                        if (eventSeverities["ANY Event"] == null)
                            eventSeverities["ANY Event"] = {};

                        console.log(`Merging counts for event: '${  eventName  }'`);
                        for (const [airframe] of Object.entries(eventSeverityCounts)) {

                            if (eventSeverities["ANY Event"][airframe] == null)
                                eventSeverities["ANY Event"][airframe] = eventSeverityCounts[airframe];
                            else
                                eventSeverities["ANY Event"][airframe] = eventSeverities["ANY Event"][airframe].concat(eventSeverityCounts[airframe]);

                        }

                    }

                }

                // severitiesPage.displayPlot(severitiesPage.state.airframe);
                this.setState(this.state);
                this.displayPlot(this.state.airframe);

            },

            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Uploads", errorThrown);
            }

        });

    }

    fetchEventSeverities(eventName) {

        $('#loading').show();
        console.log("showing loading spinner!");


        let startDate = `${this.state.startYear  }-`;
        let endDate = `${this.state.endYear  }-`;

        //0 pad the months on the front
        if (parseInt(this.state.startMonth) < 10) startDate += `0${  parseInt(this.state.startMonth)}`;
        else startDate += this.state.startMonth;
        if (parseInt(this.state.endMonth) < 10) endDate += `0${  parseInt(this.state.endMonth)}`;
        else endDate += this.state.endMonth;

        const submissionData = {
            startDate: `${startDate  }-01`,
            endDate: `${endDate  }-28`,
            tagName: this.state.tagName
        };

        $.ajax({
            type: 'GET',
            url: `/api/event/severities/${encodeURIComponent(eventName)}`,
            data: submissionData,
            async: true,
            success: (response) => {
                console.log("Received response <severities>: ", this.data, response);

                $('#loading').hide();

                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                //Check if the response is empty
                for (const [airframe, counts] of Object.entries(response)) {

                    if (counts.length != 0)
                        continue;

                    console.log(`No counts for event: '${  eventName  }' and airframe: '${  airframe  }'`);

                    this.setState(prevState => ({
                        eventsEmpty: { ...prevState.eventsEmpty, [eventName]: true }
                    }));
                    eventSeverities[eventName] = {};
                    return;
                }

                eventSeverities[eventName] = response;
                this.setState(this.state);
                this.displayPlot(this.state.airframe);

            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Uploads", errorThrown);
            },
        });

    }

    checkEvent(eventName) {
        
        console.log("Checking event: '", eventName, "'");
        this.setState(prevState => {
            const updatedEventChecked = { ...prevState.eventChecked };
            updatedEventChecked[eventName] = !updatedEventChecked[eventName];
            return { eventChecked: updatedEventChecked };
        }, () => {
            if (eventName in eventSeverities) {
                
                console.log("Already loaded counts for event: '", eventName, "'");
                this.displayPlot(this.state.airframe);
            } else {
                this.fetchEventSeverities(eventName);
            }
        });
    }

    updateStartYear(newStartYear) {
        console.log(`setting new start year to: ${  newStartYear}`);
        this.setState({startYear: newStartYear, datesChanged: true});
        console.log(this.state);
    }

    updateStartMonth(newStartMonth) {
        console.log(`setting new start month to: ${  newStartMonth}`);
        this.setState({startMonth: newStartMonth, datesChanged: true});
        console.log(this.state);
    }

    updateEndYear(newEndYear) {
        console.log(`setting new end year to: ${  newEndYear}`);
        this.setState({endYear: newEndYear, datesChanged: true});
        console.log(this.state);
    }

    updateEndMonth(newEndMonth) {
        console.log(`setting new end month to: ${  newEndMonth}`);
        this.setState({endMonth: newEndMonth, datesChanged: true});
        console.log(this.state);
    }

    dateChange() {
        console.log(`[severitiescard] notifying date change 2, startYear: '${  this.state.startYear  }', startMonth: '${  this.state.startMonth  }, endYear: '${  this.state.endYear  }', endMonth: '${  this.state.endMonth  }'`);

        const updatedEventChecked = { ...this.state.eventChecked };
        for (const [eventName] of Object.entries(updatedEventChecked)) {
            updatedEventChecked[eventName] = false;
        }
        this.setState({
            eventChecked: updatedEventChecked,
            datesChanged: false
        });

        eventSeverities = {};
        this.displayPlot(this.state.airframe);

        this.fetchAllEventSeverities();
    }

    airframeChange(airframe) {
        this.setState({airframe}, () => this.displayPlot(airframe));
    }

    updateTags(tagName) {
        this.setState({tagName, datesChanged: true});

    }

    tagNameChange(tagName) {
        this.setState({tagName}, () => this.displayPlot(this.state.airframe));

    }

    render() {

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage={"severities"} darkModeOnClickAlt={() => {
                        this.displayPlot(this.state.airframe);
                    }} waitingUserCount={waitingUserCount} fleetManager={fleetManager}
                                    unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess}
                                    plotMapHidden={plotMapHidden}/>
                </div>

                <div className="container-fluid" style={{overflowY: "auto", flex: "1 1 auto"}}>

                    <div className="row">
                        <div className="col-lg-12" style={{paddingBottom: "128"}}>
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

                                <div className="card-body" style={{padding: "0"}}>
                                    <div className="row" style={{margin: "0"}}>
                                        <div className="col-lg-2" style={{padding: "8 8 8 8"}}>

                                            {
                                                eventNames.map((eventName, index) => {

                                                    //Don't show a description for the "ANY Event" event
                                                    if (eventName === "ANY Event") return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input" type="checkbox" value=""
                                                                   id={`event-check-${  index}`}
                                                                   checked={this.state.eventChecked[eventName]}
                                                                   onChange={() => this.checkEvent(eventName)}
                                                                   style={{border: "1px solid red"}}/>
                                                            <label className="form-check-label">
                                                                {eventName}
                                                            </label>
                                                        </div>
                                                    );

                                                    return (
                                                        <div key={index} className="form-check">
                                                            <input disabled={this.state.eventsEmpty[eventName]}
                                                                   className="form-check-input" type="checkbox" value=""
                                                                   id={`event-check-${  index}`}
                                                                   checked={this.state.eventChecked[eventName]}
                                                                   onChange={() => this.checkEvent(eventName)}></input>

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
                                        <div id="severities-plot"/>
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


const container = document.getElementById('severities-page');
const root = ReactDOM.createRoot(container);
root.render(<SeveritiesPage/>);