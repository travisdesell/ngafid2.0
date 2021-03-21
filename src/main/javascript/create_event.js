import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import { Filter } from './filter.js';


var navbar = ReactDOM.render(
    <SignedInNavbar activePage="account" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);

airframes.unshift("All Airframes (Generic)");


var rules = [];

for (let i = 0; i < doubleTimeSeriesNames.length; i++) {
    rules.push({
        name : doubleTimeSeriesNames[i],
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "<=", "<", ">", ">=" ]
            },
            { 
                type : "number",
                name : "number",
            }
        ]
    });
}

console.log("filter rules: ");
console.log(rules);

class CreateEventCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            mapVisible : false,
            plotVisible : false,
            filterVisible : true,
            eventName : "",
            airframe : 0,
            startBuffer : "",
            stopBuffer : "",
            severityColumnNames : []
        }

        this.exceedenceFilter = React.createRef();
        this.severityFilter = React.createRef();
    }

    validateEventName(event) {
        let eventName = event.target.value;
        console.log("new event name: " + eventName);
        this.setState({
            eventName : eventName
        });
    }

    validateAirframe(event) {
        let airframe = event.target.value;
        console.log("new airframe: " + airframe);
        this.setState({
            airframe : airframe
        });
    }

    validateSeverityType(event) {
        let severityType = event.target.value;
        console.log("new severity type: " + severityType);
        this.setState({
            severityType : severityType
        });
    }

    changeSeverityColumn(event) {
        let severityColumn = event.target.value;
        console.log("new severity column: " + severityColumn);
        this.setState({
            severityColumn : severityColumn
        });
    }

    addSeverityColumn() {
        console.log("adding severity column: " + this.state.severityColumn);
        let newSeverityColumns = this.state.severityColumnNames;

        if (newSeverityColumns.indexOf(this.state.severityColumn) === -1) {
            newSeverityColumns.push(this.state.severityColumn);
        }

        console.log("new severity columns array:");
        console.log(newSeverityColumns);
        this.setState({
            severityColumnNames : newSeverityColumns
        });
    }

    removeSeverityColumn(columnName) {
        let newSeverityColumns = this.state.severityColumnNames;

        let columnIndex = newSeverityColumns.indexOf(columnName);
        newSeverityColumns.splice(columnIndex, 1);

        console.log("new severity columns array:");
        console.log(newSeverityColumns);
        this.setState({
            severityColumnNames : newSeverityColumns
        });
    }

    validateStartBuffer(event) {
        let startBuffer = event.target.value;
        console.log("new startBuffer: " + startBuffer);
        this.setState({
            startBuffer : startBuffer
        });
    }

    validateStopBuffer(event) {
        let stopBuffer = event.target.value;
        console.log("new stopBuffer: " + stopBuffer);
        this.setState({
            stopBuffer : stopBuffer
        });
    }

    submitFilter() {
        //console.log( this.state.filters );

        let query = this.exceedenceFilter.current.getQuery();

        console.log("Submitting filters:");
        console.log( query );

        $("#loading").show();

        var submissionData = {
            filterQuery : JSON.stringify(query),
            eventName : this.state.eventName,
            startBuffer : this.state.startBuffer,
            stopBuffer : this.state.stopBuffer,
            severityColumnNames : JSON.stringify(this.state.severityColumnNames),
            severityType : this.state.severityType,
            airframe : airframes[this.state.airframe]
        };   
        console.log(submissionData);

        $.ajax({
            type: 'POST',
            url: '/protected/create_event',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                //createEventCard.setEvents(response);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
        });  

    }

    render() {
        console.log("rendering events!");

        let style = {
            padding : 5
        };

        let formGroupStyle = {
            marginBottom: '0px',
            padding: '0 4 0 4'
        };

        let formHeaderStyle = {
            width: '200px',
            flex: '0 0 200px'
        };

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'right'
        };

        let bgStyle={
            background : "rgba(248,259,250,0.8)",
            margin:0
        };

        let validationMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        let validationMessage = "";

        if (this.state.eventName == "") {
            validationMessage = "Please enter an event name.";
        } else if (this.state.startBuffer == "") {
            validationMessage = "Please enter a start buffer time.";
        } else if (parseInt(this.state.startBuffer) < 1) {
            validationMessage = "Start buffer time must be greater than 1 second.";
        } else if (this.state.stopBuffer == "") {
            validationMessage = "Please enter a stop buffer time.";
        } else if (parseInt(this.state.startBuffer) < 1) {
            validationMessage = "Stop buffer time must be greater than 1 second.";

            //first time rendering this component exceedenceFilter will not be defined
        } else if (typeof this.exceedenceFilter != 'undefined') {
            console.log("checking exceedenceFilter isValid");

            if ( !this.exceedenceFilter.current.isValid()) {
                validationMessage = "Correct the incomplete filter.";
            }
        }

        let validationHidden = (validationMessage == "");
        let createEventDisabled = !validationHidden;

        console.log("airframes:");
        console.log(airframes);

        console.log("rendering with new severityColumnNames: " + this.state.severityColumnNames);

        return (
            <div className="card-body" style={style}>

                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header">
                        Create Event
                    </h5>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="eventName" style={labelStyle}>Event name</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="text" className="form-control" id="eventName" aria-describedby="eventName" placeholder="Enter event name" onChange={(event) => this.validateEventName(event)} value={this.state.eventName}/>
                            </div>
                        </div>
                    </div>


                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="airframeSelect" style={labelStyle}>Event for</label>
                            </div>
                            <div className="p-2 flex-fill">

                                <select id="airframeSelect" className="form-control" onChange={(event) => this.validateAirframe(event)} value={this.state.airframe}>
                                    {
                                        airframes.map((airframeInfo, index) => {
                                            return (
                                                <option key={index} value={index}>{airframeInfo}</option>
                                            )
                                        })
                                    }
                                </select>               
                            </div>
                        </div>
                    </div>


                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="startBuffer" style={labelStyle}>Start buffer (seconds)</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="number" className="form-control" id="eventName" aria-describedby="startBuffer" placeholder="Enter seconds" min="1" onChange={(event) => this.validateStartBuffer(event)} value={this.state.startBuffer} />
                            </div>
                        </div>
                    </div>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="stopBuffer" style={labelStyle}>Stop buffer (seconds)</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="number" className="form-control" id="eventName" aria-describedby="stopBuffer" placeholder="Enter seconds" min="1" onChange={(event) => this.validateStopBuffer(event)} value={this.state.stopBuffer}/>
                            </div>
                        </div>
                    </div>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="severityColumnNames" style={labelStyle}>Severity Columns</label>
                            </div>
                            <div className="p-2">

                                <select id="severityColumnNames" className="form-control" onChange={(event) => this.changeSeverityColumn(event)} value={this.state.severityColumn}>
                                    {
                                        doubleTimeSeriesNames.map((seriesName, index) => {
                                            return (
                                                <option key={index} value={seriesName}>{seriesName}</option>
                                            )
                                        })
                                    }
                                </select>               
                            </div>
                            <div className="p-2 flex-fill">

                                {
                                    this.state.severityColumnNames.map((columnName, index) => {
                                        return (<button type="button" key={columnName} className="btn btn-primary mr-1" onClick={() => this.removeSeverityColumn(columnName)}>{columnName} <i className="fa fa-times p-1"></i></button>)
                                    })
                                }

                            </div>
                            <div className="p-2">
                                <button type="button" className="btn btn-primary" onClick={() => this.addSeverityColumn()}>Add Column</button>
                            </div>
                        </div>
                    </div>


                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="severityTypeSelect" style={labelStyle}>Severity Type</label>
                            </div>
                            <div className="p-2 flex-fill">

                                <select id="severityTypeSelect" className="form-control" onChange={(event) => this.validateSeverityType(event)} value={this.state.severityType}>
                                    <option key="min" value="min">Minimum</option>
                                    <option key="max" value="max">Maximum</option>
                                    <option key="min abs" value="abs">Minimum Absolute Value</option>
                                    <option key="max abs" value="abs">Maximum Absolute Value</option>
                                </select>               
                            </div>
                        </div>
                    </div>


                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label style={labelStyle}>Exceedence Definition</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <Filter 
                                    ref={this.exceedenceFilter} 
                                    externalSubmit={true}
                                    submitFilter={(resetCurrentPage) => {this.submitFilter(resetCurrentPage);}}

                                    filterVisible={true}
                                    depth={0} 
                                    baseIndex="[0-0]" 
                                    key="[0-0]" 
                                    parent={null} 
                                    type="GROUP" 
                                    parentRerender={() => {this.forceUpdate();}} 
                                    rules={rules} 
                                    submitButtonName="Create Event" 
                                />
                            </div>
                        </div>
                    </div>

                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                        </div>
                        <div className="p-2 flex-fill">
                            <span style={validationMessageStyle} hidden={validationHidden}>{validationMessage}</span>
                        </div>
                        <div className="p-2">
                            <button className="btn btn-primary float-right" onClick={() => {this.submitFilter()}} disabled={createEventDisabled}>Create Event</button>
                        </div>
                    </div>

                </div>
            </div>
        );
    }
}

let createEventCard = ReactDOM.render(
    <CreateEventCard />,
    document.querySelector('#create-event-card')
);
