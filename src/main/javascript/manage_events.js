import React, {Component} from "react";
import ReactDOM from "react-dom";
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";
import SignedInNavbar from "./signed_in_navbar";
import {confirmModal} from "./confirm_modal";
import $ from "jquery";
import {EventDefinitionCard} from "./event_definition";


airframes.unshift("All Airframes");
airframeMap[0] = "All Airframes";

let rules = [];

class EventManager extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            eventDefinitions: []
        };


    }

    loadEventDefs() {
        fetch('/protected/manage_event_definitions')
            .then(response => response.json())
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
            <div>
                <SignedInNavbar activePage="event definitions" waitingUserCount={waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                <EventDefinitionsTable eventDefinitions={this.state.eventDefinitions} confirmDelete={this.confirmDelete}
                                       confirmUpload={this.confirmUpload}/>
            </div>
        );
    }
}

class UpdateEventDefinitionModal extends React.Component {
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
            filters : {
                type : "GROUP",
                condition : "AND",
                filters : []
            }
        }
    }

    show(eventDefinition) {
        console.log(eventDefinition);
        this.setState({
            title: "Update Event Definition: " + eventDefinition.name + " (" + eventDefinition.id + ")",
            eventName: eventDefinition.name,
            airframe: airframeMap[eventDefinition.airframeNameId],
            airframeNameId: eventDefinition.airframeNameId,
            startBuffer: eventDefinition.startBuffer,
            stopBuffer: eventDefinition.stopBuffer,
            severityType: eventDefinition.severityType,
            severityColumnNames: eventDefinition.severityColumnNames,

            eventData: eventDefinition
        });

        $("#update-event-definition-modal").modal("show");
    }

    handleInputChange = (e) => {
        const { name, value } = e.target;
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

    render() {
        const {eventData} = this.state;

        let style = {
            padding : 5
        };

        let bgStyle = {
            background : "rgba(248,259,250,0.8)",
            margin:0
        };


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
                        <div className="card mb-1" style={bgStyle}>
                                <EventDefinitionCard
                                rules={rules}
                                airframes={airframes}
                                doubleTimeSeriesNames={doubleTimeSeriesNames}

                                submitName={"Update Event"}
                                eventName={this.state.eventName}
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
                        'Content-Type': 'application/json',
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
            <div className="container-fluid" style={{backgroundColor: "white"}}>

                <div className="row">
                    <div className="col-md-12">
                        <Col>
                            <Table striped bordered hover size="sm">
                                <thead>
                                <tr>
                                    <th>id</th>
                                    <th>fleet_id</th>
                                    <th>airframe_id</th>
                                    <th>name</th>
                                    <th>start_buffer</th>
                                    <th>stop_buffer</th>
                                    <th>column_names</th>
                                    <th>severity_column_names</th>
                                    <th>severity_type</th>
                                    <th>actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                {eventDefinitions.map((eventDefinition, index) => (
                                    <tr key={index}>
                                        <td>{eventDefinition.id}</td>
                                        <td>{eventDefinition.fleetId}</td>
                                        <td>{eventDefinition.airframeNameId}</td>
                                        <td>{eventDefinition.name}</td>
                                        <td>{eventDefinition.startBuffer}</td>
                                        <td>{eventDefinition.stopBuffer}</td>
                                        <td>{arrayToString(eventDefinition.columnNames)}</td>
                                        <td>{arrayToString(eventDefinition.severityColumnNames)}</td>
                                        <td>{eventDefinition.severityType}</td>
                                        <td>
                                            <button onClick={() => updateModal.show(eventDefinition)}>
                                                Update
                                            </button>
                                            <button onClick={() => this.confirmDelete(eventDefinition)}>
                                                Delete
                                            </button>
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
    <UpdateEventDefinitionModal />,
    document.querySelector("#update-event-definition-modal-content")
);

let event_manager = ReactDOM.render(
    <EventManager/>,
    document.querySelector('#manage-events-page')
);
