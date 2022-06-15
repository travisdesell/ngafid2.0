import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown';
import Popover from 'react-bootstrap/Popover';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';
import Table from 'react-bootstrap/Table';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';

import Plotly from 'plotly.js';
import { map } from "./map.js";
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import GetDescription from "./get_description";
import { errorModal } from './error_modal.js';
import { confirmModal } from './confirm_modal.js';


// establish set of RGB values to combine //
let BG_values = ["00", "55", "AA", "FF"];
let R_values = ["FF", "D6", "AB", "80"];                            // heavier on the red for "warmer" colors


// populate hashmap of event definition IDs to RGB values
var eventColorScheme = {};
for (let d = -3; d < 70; d++){
    // iterate through RGB permutations (up to 64)
    let green = (d + 3) % 4;
    let blue = Math.trunc((d + 3)/4) % 4;
    let red = Math.trunc((d + 3)/16) % 4;

    eventColorScheme[(d + 1)] = "#" + R_values[red] + BG_values[green] + BG_values[blue];
}


//save the event definitions after the first event load so we can reuse them and not
//have to keep sending them from the server
global.eventDefinitionsLoaded = false;
global.eventDefinitions = null;

class Events extends React.Component {
    constructor(props) {
        super(props);

        console.log("constructing Events, props.events:");
        console.log(props.events);

        let definitionsPresent = [];

        for (let i = 0; i < props.events.length; i++) {
            if (!definitionsPresent.includes(props.events[i].eventDefinition)) {
                definitionsPresent.push(props.events[i].eventDefinition);
            }

            // assign color scheme to events, based on definition ID
            props.events[i].color = eventColorScheme[props.events[i].eventDefinitionId];
        }

        this.state = {
            events : props.events,
            definitions : definitionsPresent,
            lociClasses : this.getAnnotationTypes(),
        };
    }

    updateEventDisplay(index, toggle) {
            // Draw rectangles on plot
        var event = this.state.events[index];
        console.log("drawing plotly rectangle from " + event.startLine + " to " + event.endLine);
        let shapes = global.plotlyLayout.shapes;

        let update = {
            id: event.id,
            type: 'rect',
            // x-reference is assigned to the x-values
            xref: 'x',
            // y-reference is assigned to the plot paper [0,1]
            yref: 'paper',
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
            if (shapes[i].id == event.id) {
                if (toggle) {
                    shapes.splice(i, 1);
                    found = true;
                } else {
                    shapes[i] = update;
                    found = true;
                    break;
                }
            }
        }

        if (!found && toggle) {
            shapes.push(update);
        }

        Plotly.relayout('plot', global.plotlyLayout);


        // Toggle visibility of clicked event's Feature //

        // create eventStyle & hiddenStyle
        var eventStyle = new Style({                                                   // create style getter methods**
            stroke: new Stroke({
                color: event.color,
                width: 7
            })
        });

        var outlineStyle = new Style({                                                   // create style getter methods**
            stroke: new Stroke({
                color: "#000000",
                width: 8
            })
        });

        var hiddenStyle = new Style({
            stroke: new Stroke({
                color: [0,0,0,0],
                width: 3
            })
        });

        // get event info from flight
        let flight = this.props.parent;
        let eventMapped = flight.state.eventsMapped[index];
        let pathVisible = flight.state.pathVisible;
        let eventPoints = flight.state.eventPoints;
        let eventOutline = flight.state.eventOutlines[index];
        event = eventPoints[index];                                 //override event var w/ event Feature

        //toggle eventLayer style
        if (!eventMapped) {                             // if event hidden
            event.setStyle(eventStyle);
            eventOutline.setStyle(outlineStyle);
            flight.state.eventsMapped[index] = !eventMapped;

            // center map view on event location
            let coords = event.getGeometry().getFirstCoordinate();
            if (coords.length > 0 && pathVisible) {
                map.getView().setCenter(coords);
            }

        } else {                                        // if event displayed
            event.setStyle(hiddenStyle);
            eventOutline.setStyle(hiddenStyle);
            flight.state.eventsMapped[index] = !eventMapped;
        }
    }

    getAnnotationTypes() {
        var thisFlight = this;
        let types = [];

        $.ajax({
            type: 'GET',
            url: '/protected/event_classes',
            dataType : 'json',
            success : function(response) {
                types = new Map(Object.entries(response));
            },
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: false
        });

        return types;
    }

    getAnnotations(eventId) {
        var thisFlight = this;
        let annotations = [];

        let submissionData = {
            eventId : eventId,
        }

        $.ajax({
            type: 'GET',
            url: '/protected/event_annotations',
            dataType : 'json',
            data : submissionData,
            success : function(response) {
                annotations = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: false
        });

        return annotations;
    }

    setEventAnnotation(name, eventId, override = false) {
        console.log("Setting annotation for event " + eventId + " using: " + name);
        var thisFlight = this;
        let submissionData = {
            className: name,
            eventId : eventId,
            override : override,
        };

        $.ajax({
            type: 'POST',
            url: '/protected/create_annotation',
            data: submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("create annotation response:");
                console.log(response);

                if (response == "ALREADY_EXISTS") {
                    confirmModal.show("Error", "You have already assigned a class to this event, are you sure you would like to change it to: " + name + "?", () => thisFlight.setEventAnnotation(name, eventId, true));
                } else if (response == "INVALID_PERMISSION") {
                    errorModal.show("Error", "You do not have permission to annotate this flight. Please contact the site admin for more information.")
                } else if (response == "OK") {
                    thisFlight.setState(thisFlight.state);
                }

            },
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: false
        });

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

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto", "overflowY" : "visible" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let eventType = "type";

        let eventTypeSet = new Set();
        let eventTypeButtons = [];
        let thisFlight = this.props.parent;

        this.state.events.map((event, index) => {
            if (!eventTypeSet.has(event.eventDefinitionId)) {
                // add new eventDef to types set
                eventTypeSet.add(event.eventDefinitionId);

                // create new button for toggle
                let type =
                        (
                            <button className={buttonClasses} style={{flex : "0 0 10em", "backgroundColor": eventColorScheme[event.eventDefinitionId], "color" : "#000000"}} data-toggle="button" aria-pressed="false" key={index}
                                        onClick={() =>
                                            {
                                                let flight = this.props.parent;
                                                let eventsMapped = flight.state.eventsMapped;
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
                                                            document.getElementById("_" + flight.props.flightInfo.id + e).click();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    title={GetDescription(event.eventDefinition.name)}>

                                <b>{event.eventDefinition.name}</b>
                            </button>
                        );
                eventTypeButtons.push(type);
            }
        })

        return (
            <div>
                <b className={"p-1"} style={{marginBottom:"0"}}>Events:</b>

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
                        let buttonID = "_" + this.props.parent.props.flightInfo.id + index;
                        let otherFlightText = "";
                        let otherFlightURL = "";
                        let lociLabel = "";
                        let lociLabelStatus = "";
                        let lociLabelNotes = "";
                        let lociAnnotationNames = Array.from(this.state.lociClasses.values());
                        let hasCompletedAnnotation = false;


                        if (event.eventDefinitionId == -1) { 
                            otherFlightText = ", other flight id: ";
                            otherFlightURL = ( <a href={"./flight?flight_id=" + event.flightId + "&flight_id=" + event.otherFlightId}> {event.otherFlightId} </a> );
                        }

                        if (event.eventDefinitionId >= 50 && event.eventDefinitionId <= 53) {
                            let annotations = this.getAnnotations(event.id);

                            annotations.forEach(element => {
                                if (element.eventId != -1) {
                                    hasCompletedAnnotation = true;
                                }
                            });

                            const lociAnnotationPopover = (
                                <Popover
                                    id="popover-basic"
                                    style={{maxWidth: '1200px'}}
                                >
                                    <Popover.Title> 
                                        <Row>
                                            <Col style={{ display: "flex" }}>Annotation Log</Col>
                                        </Row>

                                    </Popover.Title>
                                    <Popover.Content> 
                                        <table className="table-striped table-bordered table-sm">
                                            <thead>
                                                <tr>
                                                    <th colSpan={3}>
                                                        <span className='m-1'> Event {event.id}: </span>
                                                        <span className="badge m-1" style={{backgroundColor: event.color, color: 'white'}}>{event.eventDefinition.name}</span>
                                                    </th>
                                                </tr>
                                            </thead>
                            
                                            <tbody>
                                                {
                                                    annotations.map((eventAnnotation, index) => {
                                                        let timestamp = eventAnnotation.timestamp;
                                                        let status = (<i className="fa fa-check" aria-hidden="true" style={{color : 'green'}}></i>);
                                                        
                                                        if (eventAnnotation.classId != -1) {
                                                            status = this.state.lociClasses.get(eventAnnotation.classId.toString());
                                                        }

                                                        let dateTime = timestamp.date.month + "/" + timestamp.date.day + "/" + timestamp.date.year;
                                                        dateTime = dateTime + " " + timestamp.time.hour + ":" + timestamp.time.minute + "." + timestamp.time.second;
                                                        return (
                                                            <tr key={index}>
                                                                <td>{status}</td>
                                                                <td>{eventAnnotation.user.firstName + " " + eventAnnotation.user.lastName}</td>
                                                                <td>{dateTime}</td>
                                                            </tr>

                                                        )}
                                                    )
                                                }
                                            </tbody>

                                        </table>
                                    </Popover.Content>
                                </Popover>
                            );

                            const additionalNotesPopover = (
                                <Popover
                                    id="popover-basic"
                                    style={{maxWidth: '1200px'}}
                                >
                                    <Popover.Title> 
                                        <Row>
                                            <Col style={{ display: "flex" }}>Annotator's Notes:</Col>
                                        </Row>

                                    </Popover.Title>
                                    <Popover.Content> 
                                        <div className="input-group">
                                            <textarea className="form-control" aria-label="textarea"></textarea>
                                        </div>
                                    </Popover.Content>
                                </Popover>
                            );

                            lociLabel = (
                                <div>
                                    <button className="m-1 btn btn-outline-primary dropdown-toggle" type="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                        <i className="fa fa-object-group p-1"></i>
                                        LOC-I/Stall Class
                                    </button>
                                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                                    {
                                        lociAnnotationNames.map((name, index) => {
                                            return (
                                                <button key={index} className="dropdown-item" type="button" onClick={() => this.setEventAnnotation(name, event.id)}>{name}</button>
                                            );
                                        })
                                    }
                                    </div>
                                </div>
                            );

                            lociLabelStatus = (
                                <OverlayTrigger trigger="click" placement="right-end" overlay={lociAnnotationPopover}>
                                    <Button className="m-1" data-toggle="button" variant="outline-danger" title="No log available." disabled>
                                        <i className="fa fa-users" aria-hidden="true"></i> Nobody has annotated this event yet!
                                    </Button>
                                </OverlayTrigger>
                            );

                            lociLabelNotes = (
                                <OverlayTrigger trigger="click" placement="right-end" overlay={additionalNotesPopover}>
                                    <Button className="m-1" data-toggle="button" variant="outline-info" title="Click to comment this annotation">
                                        <i className="fa fa-commenting" aria-hidden="true"></i>
                                    </Button>
                                </OverlayTrigger>
                            )

                            if (annotations.length > 0) {
                                lociLabelStatus = (
                                    <OverlayTrigger trigger="click" placement="right-end" overlay={lociAnnotationPopover}>
                                        <Button className="m-1" data-toggle="button" variant="outline-warning" title="Click to see the annotation log.">
                                            <i className="fa fa-users" aria-hidden="true"></i> You have not yet rated this event.
                                        </Button>
                                    </OverlayTrigger>
                                );
                            }

                            if (hasCompletedAnnotation) {
                                lociLabelStatus = (
                                    <OverlayTrigger trigger="click" placement="right-end" overlay={lociAnnotationPopover}>
                                        <Button className="m-1" data-toggle="button" variant="outline-success" title="Click to see the annotation log.">
                                            <i className="fa fa-users" aria-hidden="true"></i> You have rated this event!
                                        </Button>
                                    </OverlayTrigger>
                                )
                            }

                        }

                        return (
                            <div className={cellClasses} style={cellStyle} key={index}>
                                <div style={{flex: "0 0"}}>
                                    <input type="color" name="eventColor" value={event.color} onChange={(e) => {this.changeColor(e, index); }} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                                </div>

                                <button id={buttonID} className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.eventClicked(index)}>
                                    <b>{event.eventDefinition.name}</b> {" -- " + event.startTime + " to " + event.endTime + ", severity: " + (Math.round(event.severity * 100) / 100).toFixed(2)} { otherFlightText } { otherFlightURL }

                                </button>

                                {lociLabel}

                                {lociLabelStatus}

                                {lociLabelNotes}

                            </div>
                        );
                    })
                }

            </div>
        );

    }
}

export { Events, eventColorScheme };
