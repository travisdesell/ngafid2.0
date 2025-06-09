import 'bootstrap';
import React from "react";

import Plotly from 'plotly.js';
import {map} from "./map.js";
import {Stroke, Style} from 'ol/style.js';
import GetDescription from "./get_description";
import {showErrorModal} from "./error_modal";
import { plotlyLayoutGlobal } from "./flights";

// establish set of RGB values to combine //
const BG_values = ["00", "55", "AA", "FF"];
const R_values = ["FF", "D6", "AB", "80"];                            // heavier on the red for "warmer" colors


// populate hashmap of event definition IDs to RGB values
const eventColorScheme = {};
const LOWEST_EVENT_ID = -7;
const HIGHEST_EVENT_ID = 70;
const ABS_EVENT_ID = Math.abs(LOWEST_EVENT_ID);

for (let d = LOWEST_EVENT_ID; d < HIGHEST_EVENT_ID; d++) {
    // iterate through RGB permutations (up to 64)
    const green = (d + ABS_EVENT_ID) % 4;
    const blue = Math.trunc((d + ABS_EVENT_ID) / 4) % 4;
    const red = Math.trunc((d + ABS_EVENT_ID) / 16) % 4;

    eventColorScheme[(d + 1)] = `#${  R_values[red]  }${BG_values[green]  }${BG_values[blue]}`;
}


//save the event definitions after the first event load so we can reuse them and not
//have to keep sending them from the server

const eventDefinitions = {
    loaded: false,
    content: null,
};


class Events extends React.Component {
    constructor(props) {
        super(props);

        console.log("Constructing Events, props.events:", props.events);

        const definitionsPresent = [];

        for (let i = 0; i < props.events.length; i++) {
            if (!definitionsPresent.includes(props.events[i].eventDefinition)) {
                definitionsPresent.push(props.events[i].eventDefinition);
            }

            // assign color scheme to events, based on definition ID
            props.events[i].color = eventColorScheme[props.events[i].eventDefinitionId];
        }


        this.state = {
            events: props.events,
            definitions: definitionsPresent
        };
    }

    updateEventDisplay(index, toggle) {

        //Draw rectangles on plot
        let event = this.state.events[index];
        console.log("Updating event display for event: ", event);

        //Cesium flight is enabled, add event to cesium
        if (this.props.parent.state.cesiumFlightEnabled)
            this.props.parent.addCesiumEventEntity(event);

        console.log("Drawing plotly rectangle from ", event.startLine, " to ", event.endLine);
        const shapes = plotlyLayoutGlobal.shapes;
        const update = {
            id: event.id,
            type: 'rect',
            xref: 'x',      //<-- x-reference is assigned to the x-values
            yref: 'paper',  //<-- y-reference is assigned to the plot paper [0,1]
            x0: event.startLine - 1,
            y0: 0,
            x1: event.endLine + 1,
            y1: 1,
            fillcolor: event.color,
            opacity: 0.5,
            line: {
                'width': 0,
            }
        };

        let found = false;
        for (let i = 0; i < shapes.length; i++) {

            //Shape ID matches the event ID...
            if (shapes[i].id == event.id) {

                //...Toggling, remove the shape
                if (toggle) {
                    shapes.splice(i, 1);
                    found = true;

                    //...Otherwise, just update the shape
                } else {

                    shapes[i] = update;
                    found = true;
                    break;

                }

            }

        }

        //Didn't find the shape and is toggling, add it
        if (!found && toggle)
            shapes.push(update);

        Plotly.relayout('plot', plotlyLayoutGlobal);


        //Toggle visibility of clicked event's Feature

        // create eventStyle & hiddenStyle
        const eventStyle = new Style({
            stroke: new Stroke({
                color: event.color,
                width: 7
            })
        });

        const outlineStyle = new Style({
            stroke: new Stroke({
                color: "#000000",
                width: 8
            })
        });

        const hiddenStyle = new Style({
            stroke: new Stroke({
                color: [0, 0, 0, 0],
                width: 3
            })
        });

        //Get event info from flight
        const flight = this.props.parent;
        const eventMapped = flight.state.eventsMapped[index];
        const pathVisible = flight.state.pathVisible;
        const eventPoints = flight.state.eventPoints;
        const eventOutline = flight.state.eventOutlines[index];
        event = eventPoints[index];

        //Event is not mapped, add it to the map
        if (!eventMapped) {

            event.setStyle(eventStyle);
            eventOutline.setStyle(outlineStyle);
            flight.state.eventsMapped[index] = true;

            //Center map view on event location
            const coords = event.getGeometry().getFirstCoordinate();

            //Path is visible, center on the event
            if (coords.length > 0 && pathVisible)
                map.getView().setCenter(coords);

            //Otherwise, hide it
        } else {

            event.setStyle(hiddenStyle);
            eventOutline.setStyle(hiddenStyle);
            flight.state.eventsMapped[index] = false;

        }

    }

    changeColor(e, index) {
        
        const updatedEvents = this.state.events.slice();
        updatedEvents[index] = { ...updatedEvents[index], color: e.target.value };
        this.setState({
            events: updatedEvents
        });
        this.updateEventDisplay(index, false);
    }


    eventClicked(index) {
        this.updateEventDisplay(index, true);
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
        console.log("Event MetaData: ", eventMetaData);

        return eventMetaData;

    }


    render() {

        const cellClasses = "d-flex flex-row p-1 mx-1";
        const cellStyle = {"overflowX": "auto"};
        const buttonClasses = "m-1 btn btn-outline-secondary";

        const cesiumZoomButtonClasses = "m-1 btn btn-primary";

        const styleButton = {
            flex: "0 0 10em"
        };

        const eventTypeSet = new Set();
        const eventTypeButtons = [];
        const thisFlight = this.props.parent;

        const EVENT_TYPE_PROXIMITY = -1;

        this.state.events.map((event, index) => {

            if (!eventTypeSet.has(event.eventDefinitionId)) {

                //Add new eventDef to types set
                eventTypeSet.add(event.eventDefinitionId);

                //Create new button for toggle
                const type = (
                    <button
                        id={`eventToggleButton-${thisFlight.props.flightInfo.id}-${event.eventDefinitionId}`}
                        className={buttonClasses}
                        style={{
                            flex: "0 0 10em",
                            "backgroundColor": eventColorScheme[event.eventDefinitionId],
                            "color": "#000000"
                        }}
                        data-bs-toggle="button"
                        aria-pressed={thisFlight.state.eventsMapped[index] && thisFlight.state.pathVisible}
                        key={index}
                        title={GetDescription(event.eventDefinition.name)}
                        onClick={() => {

                            const flight = this.props.parent;
                            const eventsMapped = flight.state.eventsMapped;
                            let displayStatus = false;
                            let displayStatusSet = false;

                            // update eventDisplay for every event concerned
                            for (let e = 0; e < this.state.events.length; e++) {
                                if (this.state.events[e].eventDefinitionId == event.eventDefinitionId) {
                                    // ensure unified display
                                    if (!displayStatusSet) {
                                        displayStatus = !eventsMapped[e];
                                        displayStatusSet = true;
                                    }
                                    // eventsMapped[e] = displayStatus;
                                    // this.updateEventDisplay(e);

                                    if (eventsMapped[e] != displayStatus) {
                                        document.getElementById(`_${  flight.props.flightInfo.id  }${e}`).click();
                                    }
                                }
                            }
                        }
                        }
                    >
                        <b>
                            {event.eventDefinition.name}
                        </b>
                    </button>
                );
                eventTypeButtons.push(type);

                console.log(eventColorScheme);
            }
        });

        return (
            <div className="w-100">

                <b className={"p-1 d-flex flex-row justify-content-start align-items-center"}
                   style={{marginBottom: "0"}}>
                    <div className="d-flex flex-column mr-3"
                         style={{width: "16px", minWidth: "16px", maxWidth: "16px", height: "16px"}}>
                        <i className='fa fa-exclamation ml-2'
                           style={{fontSize: "12px", marginTop: "3px", opacity: "0.50"}}/>
                    </div>
                    <div style={{fontSize: "0.75em"}}>
                        Events
                    </div>
                </b>

                {
                    (this.state.events.length == 0)
                    &&
                    <div className="row m-1">
                        <div className="flex-basis m-1 p-3 card">
                            No events were found for this flight.
                        </div>
                    </div>
                }

                <div className={"eventTypes"}>
                    {
                        eventTypeButtons.map((button) => {
                            return (
                                button
                            );
                        })
                    }
                </div>

                {
                    this.state.events.map((event, index) => {
                        const buttonID = `_${  this.props.parent.props.flightInfo.id  }${index}`;
                        let otherFlightText = "";
                        let otherFlightURL = "";
                        let rateOfClosureBtn = "";
                        let rocPlot = "";
                        let zoomToCesiumEntityBtn = "";
                        let eventMetaDataText = "";
                        const eventMetaData = this.getEventMetaData(event.id);

                        //Got proximity event, show rate of closure button
                        if (event.eventDefinitionId == EVENT_TYPE_PROXIMITY) {

                            const rocPlotData = this.getRateOfClosureData(event);

                            otherFlightText = ", other flight id: ";
                            otherFlightURL = (
                                <a href={`./flight?flight_id=${  event.flightId  }&flight_id=${  event.otherFlightId}`}> {event.otherFlightId} </a>);

                            //Got rate of closure data, show button
                            if (rocPlotData != null) {

                                rateOfClosureBtn = (
                                    <button id="rocButton" data-bs-toggle="button" className={buttonClasses}
                                            onClick={() => this.displayRateOfClosurePlot(rocPlotData, event)}>
                                        <i className="fa fa-area-chart p-1"/>
                                    </button>
                                );

                                //Rate of closure plot is not visible, show it
                                if (!event.rocPlotVisible)
                                    rocPlot = (<div id={`${event.id  }-rocPlot`}></div>);

                            }

                        }

                        console.log("Event mapped:", thisFlight.state.eventsMapped[index]);
                        console.log("Flight ID:", event.flightId);

                        //Cesium is enabled, show event zoom button
                        if (this.props.parent.state.cesiumFlightEnabled) {

                            zoomToCesiumEntityBtn = (
                                <button
                                    id="zoomCesium"
                                    data-bs-toggle="button"
                                    className={cesiumZoomButtonClasses}
                                    style={{height: "100%", width: "100%", padding: "0"}}
                                    onClick={() => this.props.parent.zoomToEventEntity(event.id, event.flightId)}
                                    aria-pressed={this.props.parent.state.eventsMapped[index] && this.props.parent.state.pathVisible}
                                >
                                    <i className="fa fa-search-plus" style={{lineHeight: "36px"}}/>
                                </button>
                            );

                        }

                        if (eventMetaData != null) {
                            eventMetaDataText = " , ";
                            eventMetaData.map((item) => {
                                eventMetaDataText += `${item.name  }: ${  (Math.round(item.value * 100) / 100).toFixed(2)  }, `;
                            });
                            eventMetaDataText = eventMetaDataText.substring(0, eventMetaDataText.length - 2);
                        }

                        return (
                            <div className={cellClasses} style={cellStyle} key={index}>
                                <div style={{flex: "0 0"}}>
                                    <input type="color" name="eventColor" value={event.color} onChange={(e) => {
                                        this.changeColor(e, index);
                                    }} style={{
                                        padding: "3 2 3 2",
                                        border: "1",
                                        margin: "5 4 4 0",
                                        height: "36px",
                                        width: "36px"
                                    }}/>
                                </div>


                                <button id={buttonID} className={buttonClasses} style={styleButton}
                                        data-bs-toggle="button" aria-pressed="false"
                                        onClick={() => this.eventClicked(index)}>
                                    <b>
                                        {event.eventDefinition.name}
                                    </b>
                                    {` — ${  event.startTime  } to ${  event.endTime  }, severity: ${  (Math.round(event.severity * 100) / 100).toFixed(2)}`} {eventMetaDataText} {otherFlightText} {otherFlightURL} {rateOfClosureBtn}
                                    {rocPlot}
                                </button>
                                <div style={{
                                    height: "36px",
                                    width: "36px"
                                }}>
                                    {zoomToCesiumEntityBtn}
                                </div>
                            </div>

                        );
                    })
                }

            </div>
        );

    }

    displayRateOfClosurePlot(data, event) {
        const id = `${event.id  }-rocPlot`;
        if (!event.rocPlotVisible) {
            const trace = {
                x: data.x,
                y: data.y,
                type: "scatter",
            };
            const layout = {
                shapes: [
                    {
                        type: 'rect',
                        x0: 0,
                        y0: Math.min(...data.y),
                        x1: data.x.length - 10,
                        y1: Math.max(...data.y),
                        fillcolor: '#d3d3d3',
                        opacity: 0.3,
                        line: {
                            width: 0
                        }
                    }],
                yaxis: {
                    title: {
                        text: "Rate of closure (distance in ft)"
                    }
                },
                xaxis: {
                    title: {
                        text: "Proximity Event (seconds)"
                    }
                }
            };
            Plotly.newPlot(id, [trace], layout);
            event.rocPlotVisible = true;
            $(`#${  id}`).show();
        } else {
            event.rocPlotVisible = false;
            $(`#${  id}`).hide();
        }
    };


    getRateOfClosureData(event) {
        const eventId = event.id;
        let rocPlotData = null;
        console.log("Calculating Rate of Closure");
        $.ajax({
            type: 'GET',
            url: `/api/event/${eventId}/rate-of-closure`,
            async: false,
            success: (response) => {
                rocPlotData = response;
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Rate of closure ", errorThrown);
            },
        });
        return rocPlotData;
    };

}

export {Events, eventColorScheme, eventDefinitions};
