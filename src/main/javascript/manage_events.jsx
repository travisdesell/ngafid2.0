import React, {Component} from "react";
import ReactDOM from "react-dom";
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";
import SignedInNavbar from "./signed_in_navbar";
import {confirmModal} from "./confirm_modal";
import $ from "jquery";
import {EventDefinitionCard} from "./event_definition";
import {errorModal} from "./error_modal";


airframes.unshift("All Airframes");
airframeMap[0] = "All Airframes";

let rules = [];

for (let i = 0; i < doubleTimeSeriesNames.length; i++) {
    rules.push({
        name: doubleTimeSeriesNames[i],
        conditions: [
            {
                type: "select",
                name: "condition",
                options: ["<=", "<", ">", ">="]
            },
            {
                type: "number",
                name: "number",
            }
        ]
    });
}

class EventManager extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            eventDefinitions: []
        };
    }

    loadEventDefs() {
        fetch('/protected/manage_event_definitions')
            .then(response => responsejson())
            .then(data => {
                this.setState({eventDefinitions: data});
            })
            .catch(error => {
                console.error('Error fetching event definitions:', error);
                // Handle error
            });
    }

    componentDidMount() {
        this.loadEventDefs();
    }

    render() {
        for (let eventDefinition of this.state.eventDefinitions) {
            console.log(eventDefinition);
        }

        return (
            <div style={{overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                    fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                    modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY:"scroll", flex:"1 1 auto"}}>
                    <CreateEventCard/>
                    <EventDefinitionsTable eventDefinitions={this.state.eventDefinitions} confirmDelete={this.confirmDelete}
                                        confirmUpload={this.confirmUpload}/>
                </div>

            </div>
        );
    }
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
            }
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

                // Passing loadEventDefs in as prop doesn't seem to work, so doing a hard reload instead
                window.location.reload();
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },
            async: true
        });

    }

    render() {

        let bgStyle = {
            margin:0
        };

        return (
                <div className="card mb-1 m-2" style={bgStyle}>
                    <h5 className="card-header">
                        Create Event
                    </h5>

                    <EventDefinitionCard
                        rules={rules}
                        airframes={airframes}
                        doubleTimeSeriesNames={doubleTimeSeriesNames}

                        eventID={0} // 0 is a placeholder for a new event. Can't be negative because that's used for custom events

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
        );
    }
}

class UpdateEventDefinitionModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            filterVisible: true,
            eventName: "",
            airframe: airframeMap[0],
            airframeNameId: 0,
            startBuffer: "",
            stopBuffer: "",
            severityType: "min",
            severityColumnNames: [],
            severityColumn: doubleTimeSeriesNames[0],
            filters: {
                type: "GROUP",
                condition: "AND",
                filters: []
            }
        }
    }

    show(eventDefinition) {
        console.log(eventDefinition);
        this.setState({
            title: "Update Event Definition: " + eventDefinition.name + " (" + eventDefinition.id + ")",
            eventDefinitionID: eventDefinition.id,
            eventName: eventDefinition.name,
            airframe: airframeMap[eventDefinition.airframeNameId],
            airframeNameId: eventDefinition.airframeNameId,
            startBuffer: eventDefinition.startBuffer,
            stopBuffer: eventDefinition.stopBuffer,
            severityType: eventDefinition.severityType,
            severityColumnNames: eventDefinition.severityColumnNames,
            filters: eventDefinition.filter,
            eventData: eventDefinition
        });

        $("#update-event-definition-modal").modal("show");
    }

    handleInputChange = (e) => {
        const {name, value} = e.target;
        this.setState((prevState) => ({
            eventData: {
                ...prevState.eventData,
                [name]: value,
            },
        }));
    };

    modalClicked = () => {
        console.log("Update Submitted: " + this.state.eventData.id);
    };

    validateEventName(event) {
        let eventName = event.target.value;
        console.log("new event name: " + eventName);
        this.setState({
            eventName: eventName
        });
    }

    validateAirframe(event) {
        let airframe = event.target.value;
        console.log("new airframe: " + airframe);
        this.setState({
            airframe: airframe
        });
    }

    validateSeverityType(event) {
        let severityType = event.target.value;
        console.log("new severity type: " + severityType);
        this.setState({
            severityType: severityType
        });
    }

    changeSeverityColumn(event) {
        let severityColumn = event.target.value;
        console.log("new severity column: " + severityColumn);
        this.setState({
            severityColumn: severityColumn
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
            severityColumnNames: newSeverityColumns
        });
    }

    removeSeverityColumn(columnName) {
        let newSeverityColumns = this.state.severityColumnNames;

        let columnIndex = newSeverityColumns.indexOf(columnName);
        newSeverityColumns.splice(columnIndex, 1);

        console.log("new severity columns array:");
        console.log(newSeverityColumns);
        this.setState({
            severityColumnNames: newSeverityColumns
        });
    }

    validateStartBuffer(event) {
        let startBuffer = event.target.value;
        console.log("new startBuffer: " + startBuffer);
        this.setState({
            startBuffer: startBuffer
        });
    }

    validateStopBuffer(event) {
        let stopBuffer = event.target.value;
        console.log("new stopBuffer: " + stopBuffer);
        this.setState({
            stopBuffer: stopBuffer
        });
    }

    setFilter(filter) {
        this.setState({
            filters: filter
        });
    }

    submit() {
        console.log("submitting event definition update");
        console.log("Result: " + this.state.eventData);

        let eventDefinition = this.state.eventData;
        eventDefinition.name = this.state.eventName;
        for (let i = 0; i < airframeMap.length; i++) {
            if (airframeMap[i] === this.state.airframe) {
                eventDefinition.airframeNameId = i;
                break;
            }
        }
        eventDefinition.startBuffer = this.state.startBuffer;
        eventDefinition.stopBuffer = this.state.stopBuffer;
        eventDefinition.severityType = this.state.severityType;
        eventDefinition.severityColumnNames = this.state.severityColumnNames;
        eventDefinition.filter = this.state.filters;

        console.log("event definition:");
        console.log(eventDefinition);

        fetch('/protected/manage_event_definitions', {
            method: 'PUT',
            headers: {
                'Content-Type': 'applicationjson',
            },
            body: JSON.stringify(eventDefinition),
        })
            .then(response => {
                if (response.ok) {
                    console.log(`Event definition with ID ${eventDefinition.id} updated successfully.`);
                } else {
                    console.error(`Error updating event definition with ID ${eventDefinition.id}.`);
                    console.error(response);
                }
            })
            .catch(error => {
                console.error('Error during update:', error);
            });
    }

    render() {
        const {eventData} = this.state;

        let style = {
            padding: 5
        };

        let bgStyle = {
            margin: 0
        };

        console.log("Filter: " + JSON.stringify(this.state.filters));


        return (
            <div className="modal-content">
                <div className="modal-header">
                    <h5 id="update-event-definition-modal-title" className="modal-title">
                        {this.state.title}
                    </h5>
                    <button type="button" className="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>

                <form>
                    <div className="card-body" style={style}>
                        <EventDefinitionCard
                            rules={rules}
                            airframes={airframes}
                            doubleTimeSeriesNames={doubleTimeSeriesNames}

                            submitName={"Update Event"}
                            eventName={this.state.eventName}
                            eventID={this.state.eventDefinitionID}
                            airframe={this.state.airframe}
                            startBuffer={this.state.startBuffer}
                            stopBuffer={this.state.stopBuffer}
                            severityType={this.state.severityType}
                            severityColumn={this.state.severityColumn}
                            severityColumnNames={this.state.severityColumnNames}
                            filters={this.state.filters}

                            getFilter={() => {
                                return this.state.filters
                            }}

                            setFilter={(filter) => this.setFilter(filter)}

                            submitFilter={() => this.submit()}
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

                    <div className="modal-footer">
                        <button type="button" className="btn btn-secondary" data-dismiss="modal">
                            Close
                        </button>
                    </div>
                </form>
            </div>
        );
    }
}


class EventDefinitionsTable extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            showModal: false,
            deleteItemId: null,
        };
    }


    confirmDelete(eventDefinition) {
        confirmModal.show("Confirm Delete: " + eventDefinition.name + " (" + eventDefinition.id + ")",
            "Are you sure you wish to delete this event definition?\n\n" +
            "This will not delete if there are any events associated with it.\n",
            () => {
                console.log(`Deleting event definition with ID ${eventDefinition.id}.`)

                const params = {
                    eventDefinitionID: eventDefinition.id,
                };


                fetch(`/protected/manage_event_definitions?${new URLSearchParams(params)}`, {
                    method: 'DELETE',
                    headers: {
                        'Content-Type': 'applicationjson',
                    },

                })
                    .then(response => {
                        if (response.ok) {
                            console.log(`Event definition with ID ${eventDefinition.id} deleted successfully.`);
                            for (let i = 0; i < this.props.eventDefinitions.length; i++) {
                                if (this.props.eventDefinitions[i].id === eventDefinition.id) {
                                    this.props.eventDefinitions.splice(i, 1);
                                    this.forceUpdate();
                                    break;
                                }
                            }
                        } else {
                            console.error(`Error deleting event definition with ID ${eventDefinition.id}.`);
                            console.error(response);
                        }
                    })
                    .catch(error => {
                        console.error('Error during deletion:', error);
                    });
            }
        );
    }

    render() {
        const {eventDefinitions} = this.props;
        const {showModal: showDeleteModal, deleteItemId} = this.state;

        function arrayToString(arr) {

            return "[" + arr.join(', ') + "]";
        }


        return (
            <div className="card-body" style={{margin:10, padding:10, borderRadius:5}}>
                <div className="row">
                    <div className="col-md-12">
                        <Col>
                            <Table striped bordered hover size="sm">
                                <thead style={{color:"var(--c_text)", backgroundColor:"var(--c_bg)"}}>
                                <tr>
                                    <th>id</th>
                                    <th>fleet_id</th>
                                    <th>airframe_id</th>
                                    <th>airframe_name</th>
                                    <th>name</th>
                                    <th>start_buffer</th>
                                    <th>stop_buffer</th>
                                    <th>column_names</th>
                                    <th>severity_column_names</th>
                                    <th>severity_type</th>
                                    <th>actions</th>
                                </tr>
                                </thead>
                                <tbody style={{color:"var(--c_text_alt)"}}>
                                {eventDefinitions.map((eventDefinition, index) => (
                                    <tr key={index} style={{backgroundColor:(index%2 ? "var(--c_row_bg_solid)" : "var(--c_row_bg_alt_solid")}}>
                                        <td>{eventDefinition.id}</td>
                                        <td>{eventDefinition.fleetId}</td>
                                        <td>{eventDefinition.airframeNameId}</td>
                                        <td>{airframeMap[eventDefinition.airframeNameId]}</td>
                                        <td>{eventDefinition.name}</td>
                                        <td>{eventDefinition.startBuffer}</td>
                                        <td>{eventDefinition.stopBuffer}</td>
                                        <td>{arrayToString(eventDefinition.columnNames)}</td>
                                        <td>{arrayToString(eventDefinition.severityColumnNames)}</td>
                                        <td>{eventDefinition.severityType}</td>
                                        <td visibility="hidden">
                                            <div style={{display:"flex", flexDirection:"row"}}>
                                                <button className="btn btn-outline-primary" onClick={() => updateModal.show(eventDefinition)}>
                                                    <i className="fa fa-gear mr-1" aria-hidden="true"/>
                                                    Update
                                                </button>
                                                <div style={{margin:"0 5px"}}/>
                                                <button className="btn btn-danger" onClick={() => this.confirmDelete(eventDefinition)}>
                                                    <i className="fa fa-times" aria-hidden="true"/>
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </Table>
                        </Col>
                    </div>
                </div>
            </div>
        );
    }
}

let updateModal = ReactDOM.render(
    <UpdateEventDefinitionModal/>,
    document.querySelector("#update-event-definition-modal-content")
);

let event_manager = ReactDOM.render(
    <EventManager/>,
    document.querySelector('#manage-events-page')
);
