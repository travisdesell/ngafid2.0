import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";

import { Filter } from './filter.js';

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
            stopBuffer : ""
        }

        this.filterRef = React.createRef();
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

        let query = this.filterRef.current.getQuery();

        console.log("Submitting filters:");
        console.log( query );

        $("#loading").show();

        var submissionData = {
            filterQuery : JSON.stringify(query),
            eventName : this.state.eventName,
            startBuffer : this.state.startBuffer,
            stopBuffer : this.state.stopBuffer,
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

            //first time rendering this component filterRef will not be defined
        } else if (typeof this.filterRef != 'undefined') {
            console.log("checking filterRef isValid");

            if ( !this.filterRef.current.isValid()) {
                validationMessage = "Correct the incomplete filter.";
            }
        }

        let validationHidden = (validationMessage == "");
        let updateProfileDisabled = !validationHidden;

        console.log("airframes:");
        console.log(airframes);

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


                    <Filter ref={this.filterRef} depth={0} baseIndex="[0-0]" key="[0-0]" parent={null} type="GROUP" parentRerender={() => {this.forceUpdate();}} rules={rules} submitButtonName="Create Event" externalSubmit={true} />

                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                        </div>
                        <div className="p-2 flex-fill">
                            <span style={validationMessageStyle} hidden={validationHidden}>{validationMessage}</span>
                        </div>
                        <div className="p-2">
                            <button className="btn btn-primary float-right" onClick={() => {this.submitFilter()}} disabled={updateProfileDisabled}>Create Event</button>
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
