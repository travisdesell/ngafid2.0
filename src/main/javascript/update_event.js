import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import { EventDefinitionCard } from './event_definition.js';


var navbar = ReactDOM.render(
    <SignedInNavbar activePage="update event" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);

airframes.unshift("All Airframes");

airframeMap[0] = "All Airframes";

//remove the 'proximity' event because this can't be modified by this interface
eventDefinitions.splice(0,1);

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

class UpdateEventCard extends React.Component {
    constructor(props) {
        super(props);

        let eventDefinition = eventDefinitions[0];

        this.state = {
            filterVisible : true,
            eventIndex : 0,
            eventName : eventDefinition.name,
            airframe : airframeMap[eventDefinition.airframeNameId],
            airframeNameId : eventDefinition.airframeNameId,
            startBuffer : eventDefinition.startBuffer,
            stopBuffer : eventDefinition.stopBuffer,
            severityColumnNames : eventDefinition.severityColumnNames,
            severityType : eventDefinition.severityType,
            filters : eventDefinition.filter
         }
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

    changeSelectedEvent(event) {
        //the key will be the index of the event definition
        let eventDefinition = eventDefinitions[event.target.value];
        console.log("new selected eventDefinition:")
        console.log(eventDefinition);
        console.log("name: " + eventDefinition.name);
        console.log("airframeNameId: " + eventDefinition.airframeNameId);

        this.setState({
            eventIndex : event.target.value,
            eventName : eventDefinition.name,
            airframe : airframeMap[eventDefinition.airframeNameId],
            airframeNameId : eventDefinition.airframeNameId,
            startBuffer : eventDefinition.startBuffer,
            stopBuffer : eventDefinition.stopBuffer,
            severityColumnNames : eventDefinition.severityColumnNames,
            severityType : eventDefinition.severityType,
            filters : eventDefinition.filter
        });
    }

    setFilter(filter) {
        this.setState({
            filters : filter
        });
    }

    submitFilter() {
        console.log("Submitting filters:");
        console.log( this.state.filters );

        $("#loading").show();

        var submissionData = {
            filterQuery : JSON.stringify(this.state.filters),
            eventName : this.state.eventName,
            startBuffer : this.state.startBuffer,
            stopBuffer : this.state.stopBuffer,
            severityColumnNames : JSON.stringify(this.state.severityColumnNames),
            severityType : this.state.severityType,
            airframe : this.state.airframe
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

        let bgStyle = {
            background : "rgba(248,259,250,0.8)",
            margin:0
        };

        let initialSelect = this.state.eventName + " - " + airframeMap[this.state.airframeNameId];
        console.log("initial select: " + initialSelect);

        return (
            <div className="card-body" style={style}>

                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header">
                        Update Event
                    </h5>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="eventSelect" style={labelStyle}>Select Event</label>
                            </div>
                            <div className="p-2 flex-fill">

                                <select id="eventSelect" className="form-control" onChange={(event) => this.changeSelectedEvent(event)} value={this.state.eventIndex}>
                                    {
                                        eventDefinitions.map((eventDefinition, index) => {
                                            let fullName = eventDefinition.name + " - " + airframeMap[eventDefinition.airframeNameId];
                                            return (
                                                <option key={index} value={index}>{fullName}</option>
                                            )
                                        })
                                    }
                                </select>
                            </div>
                        </div>
                    </div>

                    <EventDefinitionCard
                        rules={rules}
                        airframes={airframes}
                        doubleTimeSeriesNames={doubleTimeSeriesNames}

                        eventNameHidden={true}
                        submitName={"Update Event"}
                        eventName={this.state.eventName}
                        airframe={this.state.airframe}
                        startBuffer={this.state.startBuffer}
                        stopBuffer={this.state.stopBuffer}
                        severityColumn={this.state.severityColumn}
                        severityColumnNames={this.state.severityColumnNames}
                        severityType={this.state.severityType}
                        filters={this.state.filters}

                        getFilter={() => {return this.state.filters}}
                        setFilter={(filter) => this.setFilter(filter)}

                        submitFilter={() => this.submitFilter()}
                        validateEventName={(event) => this.validateEventName(event)}
                        validateAirframe={(event) => this.validateAirframe(event)}
                        validateSeverityType={(event) => this.validateSeverityType(event)}
                        changeSeverityColumn={(event) => this.changeSeverityColumn(event)}
                        addSeverityColumn={() => this.addSeverityColumn()}
                        removeSeverityColumn={(columnName) => this.addSeverityColumn(columnName)}
                        validateStartBuffer={(event) => this.validateStartBuffer(event)}
                        validateStopBuffer={(event) => this.validateStopBuffer(event)}
                    />

                </div>
            </div>
        );
    }
}

let createEventCard = ReactDOM.render(
    <UpdateEventCard />,
    document.querySelector('#update-event-card')
);
