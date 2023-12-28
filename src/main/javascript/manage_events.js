import React, {Component} from "react";
import ReactDOM from "react-dom";
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";
import SignedInNavbar from "./signed_in_navbar";
import {confirmModal} from "./confirm_modal";
import $ from "jquery";


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
            title: "",
            message: "",
            eventData: {
                // Initialize with empty values for fields
                id: "",
                fleetId: "",
                airframeNameId: "",
                name: "",
                startBuffer: "",
                stopBuffer: "",
                columnNames: [],
                severityColumnNames: [],
                severityType: "",
            },
        };
    }

    show(eventDefinition) {
        console.log(eventDefinition);
        this.setState({
            title: "Update Event Definition: " + eventDefinition.name + " (" + eventDefinition.id + ")",
            eventData: { ...eventDefinition },
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

    render() {
        const {eventData} = this.state;
        // <th>airframe_id</th>
        // <th>column_names</th>
        // <th>severity_column_names</th>
        // <th>severity_type</th>
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
                    <div id="update-event-definition-modal-body" className="modal-body">
                        <div className="form-group">
                            <label htmlFor="eventID">Event ID</label>
                            <input
                                type="number"
                                className="form-control"
                                id="eventID"
                                name="eventID"
                                value={eventData.id}
                                onChange={this.handleInputChange}
                            />
                            <br/>

                            <label htmlFor="fleetID">Fleet ID</label>
                            <input
                                type="number"
                                className="form-control"
                                id="fleetID"
                                name="fleetID"
                                value={eventData.fleetId}
                                onChange={this.handleInputChange}
                            />
                            <br/>

                            <label htmlFor="eventName">Event Name</label>
                            <input
                                type="text"
                                className="form-control"
                                id="eventName"
                                name="eventName"
                                value={eventData.name}
                                onChange={this.handleInputChange}
                            />
                            <br/>

                            <label htmlFor="startBuffer">Start Buffer</label>
                            <input
                                type="number"
                                className="form-control"
                                id="startBuffer"
                                name="startBuffer"
                                value={eventData.startBuffer}
                                onChange={this.handleInputChange}
                            />
                            <br/>

                            <label htmlFor="stopBuffer">Stop Buffer</label>
                            <input
                                type="number"
                                className="form-control"
                                id="stopBuffer"
                                name="stopBuffer"
                                value={eventData.stopBuffer}
                                onChange={this.handleInputChange}
                            />
                            <br/>

                        </div>
                    </div>

                    <div className="modal-footer">
                        <button
                            type="button"
                            className="btn btn-primary"
                            data-dismiss="modal"
                            onClick={this.modalClicked}
                        >
                            Confirm
                        </button>
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
