import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.jsx";
import SignedInNavbar from "./signed_in_navbar.jsx";

import { EventDefinitionCard } from './event_definition.jsx';


var navbar = ReactDOM.render(
    <SignedInNavbar activePage="create event" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);

airframes.unshift("All Airframes");

airframeMap[0] = "All Airframes";


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

class CreateEventCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            filterVisible : true,
            eventName : "",
            airframe : airframeMap[0],
            airframeNameId : 0,
            startBuffer : "",
            stopBuffer : "",
            severityType : "min",
            severityColumnNames : [],
            severityColumn : doubleTimeSeriesNames[0],
            filters : {
                type : "GROUP",
                condition : "AND",
                filters : []
            },
            
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

    setFilter(filter) {
        this.setState({
            filters : filter
        });
    }

    submitFilter() {
        console.log("Submitting filters:");
        console.log( this.state.filters );
        console.log("airframe: " + this.state.airframe);
        console.log("airframeNameId: " + this.state.airframeNameId);
        console.log(airframeMap);

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
            dataType .json',
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

        let bgStyle = {
            background : "rgba(248,259,250,0.8)",
            margin:0
        };

        return (
            <div className="card-body" style={style}>

                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header">
                        Create Event
                    </h5>

                    <EventDefinitionCard
                        rules={rules}
                        airframes={airframes}
                        doubleTimeSeriesNames={doubleTimeSeriesNames}

                        submitName={"Create Event"}
                        eventName={this.state.eventName}
                        airframe={this.state.airframe}
                        startBuffer={this.state.startBuffer}
                        stopBuffer={this.state.stopBuffer}
                        severityColumn={this.state.severityColumn}
                        severityColumnNames={this.state.severityColumnNames}
                        filters={this.state.filters}

                        getFilter={() => {return this.state.filters}}
                        setFilter={(filter) => this.setFilter(filter)}

                        submitFilter={() => this.submitFilter()}
                        validateEventName={(event) => this.validateEventName(event)}
                        validateAirframe={(event) => this.validateAirframe(event)}
                        validateSeverityType={(event) => this.validateSeverityType(event)}
                        changeSeverityColumn={(event) => this.changeSeverityColumn(event)}
                        addSeverityColumn={() => this.addSeverityColumn()}
                        removeSeverityColumn={(columnName) => this.removeSeverityColumn(columnName)}
                        validateStartBuffer={(event) => this.validateStartBuffer(event)}
                        validateStopBuffer={(event) => this.validateStopBuffer(event)}
                    />

                </div>
            </div>
        );
    }
}

let createEventCard = ReactDOM.render(
    <CreateEventCard />,
    document.querySelector('#create-event-card')
);
