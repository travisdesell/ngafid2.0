import React, {Component} from "react";
import ReactDOM from "react-dom";
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";

class EventManager extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            eventDefinitions: []
        };
    }

    componentDidMount() {
        // TODO: Need a route for getting all event definitions
        // fetch('/protected/event_definitions')
        //     .then(response => response.json())
        //     .then(data => {
        //         this.setState({ eventDefinitions: data });
        //     })
        //     .catch(error => {
        //         console.error('Error fetching event definitions:', error);
        //         // Handle error
        //     });

        this.setState({
            eventDefinitions: [
                {
                    id: 1,
                    name: "Event 1",
                    description: "This is event 1"
                },
                {
                    id: 2,
                    name: "Event 2",
                    description: "This is event 2"
                },
                {
                    id: 3,
                    name: "Event 3",
                    description: "This is event 3"
                }
            ]
        });
    }


    render() {
        return (
            <div>
                <EventDefinitionsTable eventDefinitions={this.state.eventDefinitions}/>
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

        this.toggleModal = this.toggleModal.bind(this);
        this.confirmDelete = this.confirmDelete.bind(this);
    }

    toggleModal(eventDefinitionId = null) {
        console.log("Toggling modal");

        this.setState(prevState => ({
            showModal: !prevState.showModal,
            deleteItemId: eventDefinitionId,
        }));
    }

    confirmDelete() {
        const { deleteItemId } = this.state;
        console.log(`Deleting event definition with ID ${deleteItemId}.`)

        // TODO: Test this call
        // fetch(`/protected/event_definitions/${deleteItemId}`, {
        //     method: 'DELETE',
        // })
        //     .then(response => {
        //         if (response.ok) {
        //             console.log(`Event definition with ID ${deleteItemId} deleted successfully.`);
        //         } else {
        //             console.error(`Error deleting event definition with ID ${deleteItemId}.`);
        //         }
        //         this.toggleModal();
        //     })
        //     .catch(error => {
        //         console.error('Error during deletion:', error);
        //         this.toggleModal();
        //     });
    }


    render() {
        const { eventDefinitions } = this.props;
        const { showModal, deleteItemId } = this.state;

        return (<div className="container-fluid" style={{backgroundColor: "white"}}>
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
                                    <th>condition_json</th>
                                    <th>severity_column_names</th>
                                    <th>severity_type</th>
                                    <th>actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                {eventDefinitions.map((eventDefinition, index) => (
                                    <tr key={index}>
                                        <td>{eventDefinition.name}</td>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td></td>
                                        <td>
                                            <button onClick={() => this.handleUpdate(eventDefinition)}>
                                                Update
                                            </button>
                                            <button onClick={() => this.toggleModal(eventDefinition.id)}>
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
                <DeleteModal
                    showModal={showModal}
                    toggleModal={this.toggleModal}
                    confirmDelete={this.confirmDelete}
                />
            </div>
        );
    }

    handleUpdate(eventDefinition) {
        console.log("Update event definition:", eventDefinition);
    }

    handleDelete(eventDefinitionId) {
        console.log("Delete event definition with ID:", eventDefinitionId);
    }
}

class DeleteModal extends React.Component {
    render() {
        const { showModal, toggleModal, confirmDelete } = this.props;

        return (
            showModal && (
                <div className="modal">
                    <div className="modal-content">
                        <p>Are you sure you want to delete this item?</p>
                        <button onClick={confirmDelete}>Yes</button>
                        <button onClick={toggleModal}>No</button>
                    </div>
                </div>
            )
        );
    }
}


let event_manager = ReactDOM.render(
    <EventManager/>,
    document.querySelector('#manage-events-page')
);
