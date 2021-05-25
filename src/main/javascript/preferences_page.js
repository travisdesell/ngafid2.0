import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import ListGroup from 'react-bootstrap/ListGroup';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

let defSeriesNames = ["TAS",
                        "TRK",
                        "E1 CHT4",
                        "E1 CHT3",
                        "VSpdG",
                        "E1 EGT4",
                        "MagVar",
                        "E1 CHT2",
                        "E1 EGT2",
                        "E2 EGT Variance",
                        "E1 CHT1",
                        "E1 EGT3",
                        "VPLwas",
                        "E1 EGT1",
                        "HPLfd",
                        "WndSpd",
                        "WptBrg",
                        "BaroA",
                        "NAV1",
                        "NAV2",
                        "AltMSL",
                        "AltAGL",
                        "Pitch",
                        "IAS",
                        "Roll",
                        "E1 RPM",
                        "FQtyL",
                        "amp1",
                        "OAT",
                        "amp2",
                        "FQtyR",
                        "AltMSL Lag Diff",
                        "CRS",
                        "AltB",
                        "WndDr",
                        "HDG",
                        "HPLwas",
                        "GndSpd",
                        "E1 CHT Variance",
                        "VSpd",
                        "E1 FFlow",
                        "LatAc",
                        "E1 EGT Variance",
                        "E1 OilP",
                        "HCDI",
                        "volt1",
                        "volt2",
                        "E2 MAP",
                        "E1 OilT",
                        "E1 MAP",
                        "E2 EGT4",
                        "E2 CHT1",
                        "E2 EGT3",
                        "E2 FFlow",
                        "E2 RPM",
                        "AirportDistance",
                        "Total Fuel",
                        "COM2",
                        "COM1",
                        "RunwayDistance",
                        "HAL",
                        "AltGPS",
                        "NormAc",
                        "WptDst",
                        "Yaw Rate",
                        "True Airspeed(ft/min)",
                        "AOASimple",
                        "Coordination Index"]

class PreferencesPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            decimalPrecision : userPreferences.decimalPrecision,
            selectedMetrics : userPreferences.flightMetrics,
            fullName : userName,
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount
        };

        console.log("this users prefs:");
        console.log(this.state.preferences);
    }

    updatePreferences() {
        console.log("updating preferences");
        console.log(this.state.selectedMetrics);
        var submissionData = {
            flight_metrics : JSON.stringify(this.state.selectedMetrics),
            decimal_precision : this.state.decimalPrecision
        };

        let prefsPage = this;

        $.ajax({
            type: 'POST',
            url: '/protected/preferences',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                prefsPage.setState({
                    selectedMetrics : response.flightMetrics,
                    decimalPrecision : response.decimalPrecision
                });
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Updating User Preferences", errorThrown);
            },   
            async: true 
        });  
    }

    addMetric() {
        let name = event.target.value;

        console.log("adding " + name + " to the users metrics");
        this.state.selectedMetrics.push(name);

        this.updatePreferences();
    }

    removeMetric(index) {
        console.log("removing " + this.state.selectedMetrics[index] + " from metric list.");

        var newSelectedMetrics = new Array();

        for (let i = 0; i < this.state.selectedMetrics.length; i++) {
            if (i != index) {
                newSelectedMetrics.push(this.state.selectedMetrics[i]);
            }
        }

        console.log("sel metrics:");
        console.log(newSelectedMetrics);
        this.state.selectedMetrics = newSelectedMetrics;
        this.updatePreferences();
    }
    
    changePrecision(precision) {
        this.state.decimalPrecision = event.target.value;

        this.updatePreferences();
    }

    render() {
        let selectedMetrics = this.state.selectedMetrics;

        //serverMetrics = (All Series) - (User Selected Series)
        let serverMetrics = defSeriesNames.filter((e) => !selectedMetrics.includes(e));

        let styleButtonSq = {
            flex : "right",
            float : "auto"
        };

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left'
        };

        let listStyle = {
            maxHeight: "400px",
            overflowY: "scroll"
        };

        let formGroupStyle = {
            marginBottom: '0px',
            padding: '0 4 0 4'
        };

        let formHeaderStyle = {
            width: '200px',
            flex: '0 0 200px'
        };

        //let listStyle = {
            //maxHeight: "400px",
            //overflowX: "scroll",
            //flexDirection: "row"
        //}

        var selectedMetricsHTML;

        if (this.state.selectedMetrics != null && this.state.selectedMetrics.length > 0) {
            selectedMetricsHTML = (
                selectedMetrics.map((metric, key) => {
                    return (
                        <ListGroup.Item key={key} size="sm">
                            <Container>
                                <Row className="justify-content-md-center">
                                    <Col xs>
                                        {metric}
                                    </Col>
                                    <Col xs>
                                        <button className="m-1 btn btn-outline-secondary align-right" style={styleButtonSq} onClick={() => this.removeMetric(key)} title="Permanently delete this cached aircraft">
                                            <i className="fa fa-times" aria-hidden="true"></i>
                                        </button>
                                    </Col>
                                  </Row>
                            </Container>
                        </ListGroup.Item>
                    );
                })
            );
        } else {
            selectedMetricsHTML = (
                <ListGroup.Item size="sm">
                    <Container>
                        No metrics here yet! Use the dropdown to add some.
                    </Container>
                </ListGroup.Item>
            );
        }

 

        return (
            <div>
                <SignedInNavbar activePage="account" waitingUserCount={this.state.waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={this.state.unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="container-fluid">
                    <div className="row">
                        <div className="col" style={{paddingRight:"16"}}>
                             <div style={{marginTop:"4", padding:"0 0 0 0"}}>
                                    <div className="col" style={{padding:"0 0 0 0"}}>
                                        <div className="card" style={{background : "rgba(248,259,250,0.8)"}}>
                                            <h5 className="card-header">
                                                {this.state.fullName}'s Preferences:
                                            </h5>
                                            <div className="card-body">
                                                <div className="col" style={{padding:"0 0 0 0"}}>
                                                    <div className="card" style={{background : "rgba(248,259,250,0.8)"}}>
                                                        <h6 className="card-header">
                                                            Your Flight Metric Viewer Preferences:
                                                        </h6>
                                                        <div className="form-group" style={formGroupStyle}>
                                                            <div className="d-flex">
                                                                <div className="p-2" style={formHeaderStyle}>
                                                                    <label htmlFor="severityColumnNames" style={labelStyle}>Selected Metrics</label>
                                                                </div>
                                                                <div className="p-2">

                                                                    <select id="severityColumnNames" className="form-control" onChange={(event) => this.props.changeSeverityColumn(event)} value={this.props.severityColumn}>
                                                                        {
                                                                            serverMetrics.map((seriesName, index) => {
                                                                                return (
                                                                                    <option key={index} value={seriesName}>{seriesName}</option>
                                                                                )
                                                                            })
                                                                        }
                                                                    </select>               
                                                                </div>
                                                                <div className="p-2 flex-fill">

                                                                    {
                                                                        selectedMetrics.map((columnName, index) => {
                                                                            return (<button type="button" key={columnName} className="btn btn-primary mr-1" onClick={() => this.props.removeSeverityColumn(columnName)}>{columnName} <i className="fa fa-times p-1"></i></button>)
                                                                        })
                                                                    }

                                                                </div>
                                                                <div className="p-2">
                                                                    <button type="button" className="btn btn-primary" onClick={() => this.props.addSeverityColumn()}>Add Column</button>
                                                                </div>
                                                            </div>
                                                        </div>
                                                        <div className="card-body">
                                                            <div className="form-row align-items-left justify-content-left">
                                                                <Form.Group controlId="exampleForm.ControlInput1">
                                                                    <Form.Label>Decimal Precision:</Form.Label>
                                                                    <Form.Control as="select" defaultValue={this.state.decimalPrecision} onChange={this.changePrecision.bind(this)}>
                                                                        <option key='0'>0</option>
                                                                        <option key='1'>1</option>
                                                                        <option key='2'>2</option>
                                                                        <option key='3'>3</option>
                                                                        <option key='4'>4</option>
                                                                        <option key='5'>5</option>
                                                                        <option key='6'>6</option>
                                                                    </Form.Control>    
                                                                </Form.Group>
                                                            </div>
                                                        <hr style={{padding:"0", margin:"0 0 0 0"}}></hr>
                                                    </div>
                                                </div>
                                            </div>
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

console.log("setting preferences page with react!");

var preferencesPage = ReactDOM.render(
    <PreferencesPage waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
   document.querySelector('#preferences-page')
)
