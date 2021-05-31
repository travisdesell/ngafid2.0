import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";
import Popover from 'react-bootstrap/Popover';
import Accordion from 'react-bootstrap/Accordion';
import Card from 'react-bootstrap/Card';
import Table from 'react-bootstrap/Table';
import Badge from 'react-bootstrap/Badge';
import Container from 'react-bootstrap/Container';
import Alert from 'react-bootstrap/Alert';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';
import Button from 'react-bootstrap/Button';
import ButtonGroup from 'react-bootstrap/ButtonGroup';
import ListGroup from 'react-bootstrap/ListGroup';
import {eventColorScheme} from './events_component.js';

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class MapPopup extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            navbarWidth : 40,
            status : '',
            info : "",
            eventRowHidden : true,
            placement : []
        };

    }

    show() {
        this.setState({status : ""});
    }

    close() {
        console.log("closing the popup!");
        this.setState({status : 'none'});
    }

    isPinned() {
        return this.state.status === 'pinned';
    }

    toggleEventRow() {
        this.setState({
            eventRowHidden : !this.state.eventRowHidden
        });
    }

    pin() {
        if (this.state.status == 'pinned') {
            console.log("unpinning the popup!")
            this.setState({status : 'none'});
        } else {
            console.log("pinning the popup!");
            this.setState({status : 'pinned'});
        }
    }

    getRelevantEvents() {
        let timeIndex = this.props.lociData[0];

        var events = new Array();
        console.log("events props");
        console.log(events);

        for (let i = 0; i < this.props.events.length; i++) {
            let event = this.props.events[i];

            if (timeIndex >= event.startLine && timeIndex <= event.endLine) {
                console.log("event pushed!!");
                events.push(event);
            }
        }

        if (events.length > 0) {
            this.state.events = events;
        } else { 
            this.state.events = null;
        }

    }

    render() {
        console.log("rendering a map popup with info:");
        console.log(this.props.info);
        this.getRelevantEvents();

        var style = {
            top : this.props.placement[1] + this.state.navbarWidth,
            left : this.props.placement[0],
            display : this.props.on,
            minWidth: 320
        }

        if(this.state.status !== "none"){
            let lociInfo = new Array();

            lociInfo[0] = this.props.lociData[0];
            for (let i = 1; i < this.props.lociData.length; i++) {
                let lPrecision = (this.props.precision < 2) ? 2 : this.props.precision;
                console.log(lPrecision);
                if (this.props.lociData[i] == null) {
                    lociInfo[i] = "Not Available";
                } else if (i < 3) {
                    //show 2 S.Fs for probabilities and display as a decimal value
                    lociInfo[i] = this.props.lociData[i].toFixed(lPrecision);
                } else {
                    //only show 1 S.F. for all other params
                    lociInfo[i] = this.props.lociData[i].toFixed(1);
                }
            }

            let info = this.props.info;
            let precision = this.props.precision;
            console.log("users precision: "+precision);

            let lgStyle = {
                backgroundColor : '#dee2e6'
            };

            let lgiStyle = {
                backgroundColor : '#dee2e6',
                overflowX : 'auto'
            }

            let eventRow = "";

            if (this.state.events != null) {
                if (this.state.eventRowHidden) {
                    eventRow = "";
                } else {
                    eventRow = (
                        <ListGroup style={lgiStyle} className="my-2" horizontal='sm'>
                        {
                            this.state.events.map((event, key) => {
                                let eventColor = eventColorScheme[event.eventDefinitionId];
                                let descriptionString = event.startTime + " to " + event.endTime + " severity: " + event.severity;

                                let badgeStyle = {
                                    backgroundColor : eventColor
                                };

                                return (
                                    <ListGroup.Item style={lgStyle} key={key}>
                                        <h6>
                                            <Badge style={badgeStyle} key={key} title={descriptionString}>{event.eventDefinition.name}</Badge>
                                        </h6>
                                    </ListGroup.Item>
                                );
                            })
                        }
                        </ListGroup>
                    );
                }
            }

            return (
                <div style={{ height: 120}}>
                    <Popover
                        id="popover-basic"
                        style={style}
                    >
                        <Popover.Title as="h2"> 
                            <Row>
                                <Col sm={5}>Flight Metrics</Col>
                                <Col style={{ display: "flex" }}>
                                    <ButtonGroup className="text-right" aria-label="toolbar" style={{marginLeft: "auto"}}>
                                        {this.state.events != null &&
                                            <Button onClick={() => this.toggleEventRow()} data-toggle="button" variant="outline-secondary" size="sm">
                                                <i className="fa fa-exclamation p-1"></i>
                                            </Button>
                                        }
                                        <Button onClick={() => this.pin()} data-toggle="button" variant="outline-secondary" size="sm">
                                            <i className="fa fa fa-cog p-1"></i>
                                        </Button>
                                        <Button onClick={() => this.pin()} data-toggle="button" variant="outline-secondary" size="sm">
                                            <i className="fa fa-thumb-tack p-1"></i>
                                        </Button>
                                        <Button onClick={() => this.close()} variant="outline-secondary" size="sm">
                                            <i className="fa fa-times p-1"></i>
                                        </Button>
                                    </ButtonGroup>
                                </Col>
                            </Row>
                        </Popover.Title>
                        <Popover.Content> 

                            {eventRow}
                      
                            <Table striped bordered hover size="sm">

                                <thead>
                                    <tr>
                                        <th>Metric Name</th>
                                        <th>Data</th>
                                    </tr>
                                </thead>
                
                                <tbody>
                                    <tr>
                                        <td>Time Index (s):</td>
                                        <td>{lociInfo[0]}</td>
                                    </tr>
                                    <tr>
                                        <td>Stall Index:</td>
                                        <td>{lociInfo[1]}</td>
                                    </tr>
                                    <tr>
                                        <td>LOC-I Index:</td>
                                        <td>{lociInfo[2]}</td>
                                    </tr>
                                    {
                                        info.map((metric, key) => {
                                            console.log(metric.value);
                                            let displayValue = "";
                                            if (metric.value === "null") {
                                                console.log("a metric isnt available");
                                                displayValue = "Not Available";
                                            } else {
                                                let val = parseFloat(metric.value);
                                                displayValue = val.toFixed(precision);
                                            }

                                            return(
                                                <tr key={key}>
                                                    <td>{metric.name}</td>
                                                    <td>{displayValue}</td>
                                                </tr>
                                            );
                                        })
                                    }
                                </tbody>

                            </Table>
                        </Popover.Content>
                    </Popover>
                </div>
            );
        } else {
            return null;
        }
    }
}

export { MapPopup };
