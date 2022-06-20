import 'bootstrap';

import React, { Component } from "react";
import Dropdown from 'react-bootstrap/Dropdown';
import Popover from 'react-bootstrap/Popover';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';
import Table from 'react-bootstrap/Table';
import OverlayTrigger from 'react-bootstrap/OverlayTrigger';

import { errorModal } from './error_modal.js';
import { confirmModal } from './confirm_modal.js';

class EventAnnotation extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            annotationTypes : this.getAnnotationTypes(),
        };
    }

    getAnnotationTypes() {
        var thisAnnotation = this;
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

    getAnnotations() {
        var thisAnnotation = this;
        let annotations = [];

        let submissionData = {
            eventId : this.props.event.id,
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
        var thisAnnotation = this;
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
                    confirmModal.show("Error", "You have already assigned a class to this event, are you sure you would like to change it to: " + name + "?", () => thisAnnotation.setEventAnnotation(name, eventId, true));
                } else if (response == "INVALID_PERMISSION") {
                    errorModal.show("Error", "You do not have permission to annotate this flight. Please contact the site admin for more information.")
                } else if (response == "OK") {
                    thisAnnotation.setState(thisAnnotation.state);
                }

            },
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: false
        });

    }

    updateAnnotationNotes(notes) {
        var thisAnnotation = this;
        let submissionData = {
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
                    confirmModal.show("Error", "You have already assigned a class to this event, are you sure you would like to change it to: " + name + "?", () => thisAnnotation.setEventAnnotation(name, eventId, true));
                } else if (response == "INVALID_PERMISSION") {
                    errorModal.show("Error", "You do not have permission to annotate this flight. Please contact the site admin for more information.")
                } else if (response == "OK") {
                    thisAnnotation.setState(thisAnnotation.state);
                }

            },
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: false
        });
    }

    render() {
        const cellClasses = "d-flex flex-row p-1";
        const cellStyle = { "overflowX" : "auto", "overflowY" : "visible" };

        const annotations = this.getAnnotations();
        let lociAnnotationNames = Array.from(this.state.annotationTypes.values());
        let hasCompletedAnnotation = false;

        const event = this.props.event;

        annotations.forEach(element => {
            if (element.eventId != -1) {
                hasCompletedAnnotation = true;
            }
        });

        let log = (
            <OverlayTrigger trigger="click" placement="right-end" overlay={lociAnnotationPopover}>
                <Button className="m-1" data-toggle="button" variant="outline-danger" title="No log available." disabled>
                    <i className="fa fa-users" aria-hidden="true"></i> Nobody has annotated this event yet!
                </Button>
            </OverlayTrigger>
        );
        
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
                                        status = this.state.annotationTypes.get(eventAnnotation.classId.toString());
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
                        <Col sm={9} style={{ display: "flex" }}>Annotator's Notes:</Col>
                        <Col style={{ display: "flex" }}>
                            <Button className="" data-toggle="button" variant="outline-success" title="Submit" onClick={() => this.updateAnnotationNotes()}>
                                <i className="fa fa-check" aria-hidden="true"></i>
                            </Button>
                        </Col>
                    </Row>

                </Popover.Title>
                <Popover.Content> 
                    <div className="input-group">
                        <textarea className="form-control" aria-label="textarea"></textarea>
                    </div>
                </Popover.Content>
            </Popover>
        );

        
        if (annotations.length > 0) {
            log = (
                <OverlayTrigger trigger="click" placement="right-end" overlay={lociAnnotationPopover}>
                    <Button className="m-1" data-toggle="button" variant="outline-warning" title="Click to see the annotation log.">
                        <i className="fa fa-users" aria-hidden="true"></i> You have not yet rated this event.
                    </Button>
                </OverlayTrigger>
            );
        }

        if (hasCompletedAnnotation) {
            log = (
                <OverlayTrigger trigger="click" placement="right-end" overlay={lociAnnotationPopover}>
                    <Button className="m-1" data-toggle="button" variant="outline-success" title="Click to see the annotation log.">
                        <i className="fa fa-users" aria-hidden="true"></i> You have rated this event!
                    </Button>
                </OverlayTrigger>
            )
        }

        return (
            <div className="row ml-1" role="group" aria-label="Basic example">
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

                {log}

                <OverlayTrigger trigger="click" placement="right-end" overlay={additionalNotesPopover}>
                    <Button className="m-1" data-toggle="button" variant="outline-info" title="Click to comment this annotation">
                        <i className="fa fa-commenting" aria-hidden="true"></i>
                    </Button>
                </OverlayTrigger>
            </div>
        
        );
    }
}

export { EventAnnotation };
