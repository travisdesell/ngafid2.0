import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Plotly from 'plotly.js';
import { map } from "./map.js";
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import GetDescription from "./get_description";
import {errorModal} from "./error_modal";

//Establish set of RGB values to combine
let BG_values = ["00", "55", "AA", "FF"];
let R_values = ["FF", "D6", "AB", "80"];        //<-- Heavier on the red for "warmer" colors


//Populate hashmap of event definition IDs to RGB values
const eventColorScheme = {};
const LOWEST_EVENT_ID = -7;
const HIGHEST_EVENT_ID = 70;
const ABS_EVENT_ID = Math.abs(LOWEST_EVENT_ID);

for (let d = LOWEST_EVENT_ID; d < HIGHEST_EVENT_ID; d++) {

    //Iterate through RGB permutations (up to 64)
    let green = (d + ABS_EVENT_ID) % 4;
    let blue = Math.trunc((d + ABS_EVENT_ID) / 4) % 4;
    let red = Math.trunc((d + ABS_EVENT_ID) / 16) % 4;

    eventColorScheme[(d + 1)] = "#" + R_values[red] + BG_values[green] + BG_values[blue];
}


/*
    Save the Event Definitions after the first event
    load so we can reuse them and not have to keep
    sending them from the server
*/
global.eventDefinitionsLoaded = false;
global.eventDefinitions = null;

class Events extends React.Component {

    constructor(props) {

        super(props);

        console.log("Constructing Events, props.events:", props.events);

        let definitionsPresent = [];

        for (let i = 0; i < props.events.length; i++) {

            //Present definitions don't include the current event's definition, add it to the list
            if (!definitionsPresent.includes(props.events[i].eventDefinition))
                definitionsPresent.push(props.events[i].eventDefinition);

            //Assign color scheme to events based on definition ID
            props.events[i].color = eventColorScheme[props.events[i].eventDefinitionId];

        }

        this.state = {
            events : props.events,
            definitions : definitionsPresent
        };

    }

    updateEventDisplay(index, toggle) {

        //Draw rectangles on plot
        let event = this.state.events[index];
        let shapes = global.plotlyLayout.shapes;
        console.log(`Drawing plotly rectangle from ${event.startLine} to ${event.endLine}`);

        let update = {
            id: event.id,
            type: 'rect',
            xref: 'x',      //<-- x-reference is assigned to the x-values
            yref: 'paper',  //<-- y-reference is assigned to the plot paper [0, 1]
            x0: event.startLine - 1,
            y0: 0,
            x1: event.endLine + 1,
            y1: 1,
            fillcolor: event.color,
            'opacity': 0.5,
            line: {
                'width': 0,
            }
        };

        let found = false;
        for (let i = 0; i < shapes.length; i++) {

            //Shape's ID doesn't match the event's ID, skip
            if (shapes[i].id != event.id)
                continue;

            //Mark as found
            found = true;

            //Toggling, remove the shape
            if (toggle) {

                shapes.splice(i, 1);
                
            //Otherwise, update the shape
            } else {
                shapes[i] = update;
                found = true;
                break;
            }

        }

        //Not found and toggling, add the shape
        if (!found && toggle)
            shapes.push(update);

        Plotly.relayout('plot', global.plotlyLayout);


        /* Toggle visibility of clicked event's Feature */

        //Create eventStyle & hiddenStyle
        let eventStyle = new Style({            // create style getter methods**
            stroke: new Stroke({
                color: event.color,
                width: 7
            })
        });

        let outlineStyle = new Style({          // create style getter methods**
            stroke: new Stroke({
                color: "#000000",
                width: 8
            })
        });

        let hiddenStyle = new Style({
            stroke: new Stroke({
                color: [0,0,0,0],
                width: 3
            })
        });

        //Get Event info from Flight
        let flight = this.props.parent;
        let eventMapped = flight.state.eventsMapped[index];
        let pathVisible = flight.state.pathVisible;
        let eventPoints = flight.state.eventPoints;
        let eventOutline = flight.state.eventOutlines[index];
        event = eventPoints[index];                                 //override event var w/ event Feature

        /* Toggle eventLayer style */

        //Event is hidden, show it
        if (!eventMapped) {

            event.setStyle(eventStyle);
            eventOutline.setStyle(outlineStyle);
            flight.state.eventsMapped[index] = !eventMapped;

            //Center map view on event location
            let coords = event.getGeometry().getFirstCoordinate();

            //Path is visible, center on event
            if (coords.length > 0 && pathVisible)
                map.getView().setCenter(coords);

        //Otherwise, hide it
        } else {

            event.setStyle(hiddenStyle);
            eventOutline.setStyle(hiddenStyle);
            flight.state.eventsMapped[index] = !eventMapped;

        }

    }

    changeColor(e, index) {

        this.state.events[index].color = e.target.value;
        this.setState({
            events : this.state.events
        });
        this.updateEventDisplay(index, false);

    }
    
    eventClicked(index) {

        this.updateEventDisplay(index, true);

    }

    getEventMetaData(eventId) {

        const submissionData = {
            eventId : eventId
        };

        let eventMetaData = null;

        $.ajax({
            type: 'POST',
            url: '/protected/event_metadata',
            data : submissionData,
            dataType : 'json',
            async: false,
            success : function(response) {
                eventMetaData =  response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Event Metadata ", errorThrown);
            },
        })

        console.log("Event MetaData: ", eventMetaData);

        return eventMetaData;

    }

    render() {

        const cellClasses = "d-flex flex-row p-1";
        const cellStyle = { "overflowX" : "auto" };
        const buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        const eventTypeSet = new Set();
        const eventTypeButtons = [];

        this.state.events.map((event, index) => {

            //Event definition not in set, add it to set and create button
            if (!eventTypeSet.has(event.eventDefinitionId)) {

                //Add new eventDef to types set
                eventTypeSet.add(event.eventDefinitionId);

                //Create new button for toggle
                let type = (
                    <button
                        className={buttonClasses}
                        style={{flex : "0 0 10em", "backgroundColor": eventColorScheme[event.eventDefinitionId], "color" : "#000000"}}
                        data-bs-toggle="button"
                        aria-pressed="false"
                        key={index}
                        title={GetDescription(event.eventDefinition.name)}
                        onClick={() =>
                            {
                                const flight = this.props.parent;
                                const eventsMapped = flight.state.eventsMapped;
                                const displayStatus = false;
                                const displayStatusSet = false;

                                //Update eventDisplay for every event concerned
                                for (let e = 0; e < this.state.events.length; e++) {

                                    //Event definition IDs are different, skip
                                    if (this.state.events[e].eventDefinitionId != event.eventDefinitionId)
                                        continue;

                                    //Display status not set, set it
                                    if (!displayStatusSet) {
                                        displayStatus = !eventsMapped[e];
                                        displayStatusSet = true;
                                    }
                                    
                                    //Display status is different, update
                                    if (eventsMapped[e] != displayStatus)
                                        document.getElementById("_" + flight.props.flightInfo.id + e).click();
                                    
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
                console.log("Event Color Scheme: ", eventColorScheme);
            }
        })

        return (
            <div className="m-1">
                <b className={"p-1"} style={{marginBottom:"0"}}>Events:</b>

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
                        eventTypeButtons.map( (button) => {
                            return (
                                button
                            )
                        })
                    }
                </div>

                {
                    this.state.events.map((event, index) => {

                        const buttonID = `_${this.props.parent.props.flightInfo.id}${index}`;

                        let otherFlightText = "";
                        let otherFlightURL = "";
                        let otherFlightLink = "";

                        let rateOfClosureBtn = "";
                        let rocPlot = "";

                        let eventMetaDataText = "";
                        const eventMetaData = this.getEventMetaData(event.id);

                        const EVENT_ID_PROXIMITY = -1;

                        //Event is a proximity event, get the rate of closure data
                        if (event.eventDefinitionId == EVENT_ID_PROXIMITY) {

                            const rocPlotData = this.getRateOfClosureData(event);
                            
                            otherFlightText = ", other flight id: ";
                            otherFlightURL = `./flight?flight_id=${event.flightId}&flight_id=${event.otherFlightId}`;
                            otherFlightLink = (
                                <a href={otherFlightURL}>
                                    {event.otherFlightId}
                                </a>
                            );
                            
                            //Rate of closure data is available...
                            if (rocPlotData != null) {

                                //...Create button
                                rateOfClosureBtn = (
                                    <button id="rocButton" data-bs-toggle="button" className={buttonClasses} onClick={() => this.displayRateOfClosurePlot(rocPlotData, event)}>
                                        <i className="fa fa-area-chart p-1"/>
                                    </button>
                                );

                                //...Event doesn't have a visible ROC plot, create one
                                if (!event.rocPlotVisible)
                                    rocPlot = (
                                        <div id={event.id + "-rocPlot"}></div>
                                    );

                            }

                        }

                        //Event has metadata, display it
                        if (eventMetaData != null) {

                            eventMetaDataText = " , ";
                            eventMetaData.map((item) => {
                                eventMetaDataText += item.name + ": " +  (Math.round(item.value * 100) / 100).toFixed(2) + ", ";
                            })
                            eventMetaDataText = eventMetaDataText.substring(0, eventMetaDataText.length - 2);

                        } 

                        return (
                            <div className={cellClasses} style={cellStyle} key={index}>
                                <div style={{flex: "0 0"}}>
                                    <input type="color" name="eventColor" value={event.color} onChange={(e) => {this.changeColor(e, index); }} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                                </div>

                                <button id={buttonID} className={buttonClasses} style={styleButton} data-bs-toggle="button" aria-pressed="false" onClick={() => this.eventClicked(index)}>
                                    <b>
                                        {event.eventDefinition.name}
                                    </b>
                                    {" -- " + event.startTime + " to " + event.endTime + ", severity: " + (Math.round(event.severity * 100) / 100).toFixed(2)}
                                    { eventMetaDataText }
                                    { otherFlightText }
                                    { otherFlightURL }
                                    { rateOfClosureBtn }
                                    { rocPlot }
                                </button>
                            </div>
                        );
                    })
                }

            </div>
        );

    }

    displayRateOfClosurePlot(data, event) {

        const id = `${event.id}-rocPlot`;

        //Plot is not visible, create it
        if (!event.rocPlotVisible) {

            const trace = {
                x : data.x,
                y : data.y,
                type : "scatter",
            }
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
                yaxis : {
                    title:{
                        text: "Rate of closure (distance in ft)"
                    }
                },
                xaxis : {
                    title : {
                        text : "Proximity Event (seconds)"
                    }
                }
            }

            Plotly.newPlot(id, [trace], layout)
            event.rocPlotVisible = true
            $("#"+id).show();

        //Plot is visible, hide it
        } else {

            event.rocPlotVisible = false;
            $("#"+id).hide();

        }

    };


    getRateOfClosureData(event) {

        const eventId = event.id;
        console.log("Fetching Rate of Closure Data for event: ", eventId);

        const submissionData = {
            eventId : eventId
        };

        let rocPlotData = null;

        $.ajax({
            type: 'POST',
            url: '/protected/rate_of_closure',
            data : submissionData,
            dataType : 'json',
            async: false,
            success : function(response) {
                rocPlotData = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Rate of Closure ", errorThrown);
            },
        })

        return rocPlotData;

    };

}

export { Events, eventColorScheme };