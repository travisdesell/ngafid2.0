import React, {Component} from "react";
import ReactDOM from "react-dom";
import Table from "react-bootstrap/Table";
import Col from "react-bootstrap/Col";

class EventManager extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            eventDefinitions: [], // Store fetched event definitions here
            // Other state properties
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
    render() {
        const {eventDefinitions} = this.props;

        return (
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
                                <button onClick={() => this.handleDelete(eventDefinition.id)}>
                                    Delete
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </Table>
            </Col>
        );
    }

    handleUpdate(eventDefinition) {
        console.log("Update event definition:", eventDefinition);
    }

    handleDelete(eventDefinitionId) {
        console.log("Delete event definition with ID:", eventDefinitionId);
    }
}

let event_manager = ReactDOM.render(
    <EventManager/>,
    document.querySelector('#manage-events-page')
);
