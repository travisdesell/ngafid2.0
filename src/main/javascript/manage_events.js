import React, {Component} from "react";
import ReactDOM from "react-dom";
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";
import SignedInNavbar from "./signed_in_navbar";
import {confirmModal} from "./confirm_modal";


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

    handleUpdate(eventDefinition) {
        console.log("Update event definition:", eventDefinition);
    }

    confirmUpload(eventDefinition) {
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
                                            <button onClick={() => this.props.confirmUpload(eventDefinition)}>
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


let event_manager = ReactDOM.render(
    <EventManager/>,
    document.querySelector('#manage-events-page')
);
