import 'bootstrap';
import React, {Component} from "react";
import Form from 'react-bootstrap/Form';
import Dropdown from "react-bootstrap/Dropdown";
import Button from "react-bootstrap/Button";


class EmailPreferences extends React.Component {
    constructor(props) {
        super(props);

        // TOOD: Update when more props are passed in
        this.state = {
            emailOptOut: this.props.optOut,
            uploadProcessing: !this.props.optOut,
            uploadStatus: !this.props.optOut,
            criticalEvents: !this.props.optOut
        }
    }

    render() {
        let formGroupStyle = {
            marginBottom: '0px',
            padding: '0 4 0 4'
        };

        return (
            <div className="card-body">
                <div className="col" style={{padding: "0 0 0 0"}}>
                    <div className="card" style={{background: "rgba(248,259,250,0.8)"}}>
                        <h6 className="card-header">
                            Your Email Preferences:
                        </h6>
                        <div className="form-group" style={formGroupStyle}>
                            <div className="d-flex">
                                <div className="p-2">
                                    <Form>
                                        <Form.Check
                                            type="switch"
                                            id="email-opt-out"
                                            label="Opt Out of All Email Notifications"
                                            defaultChecked={this.props.optOut}
                                        />
                                        <Form.Check
                                            type="switch"
                                            id="upload-processing"
                                            label="Upload Is Being Processed"
                                            defaultChecked={this.props.uploadProcessing}
                                            disabled={this.props.optOut}
                                        />
                                        <Form.Check
                                            type="switch"
                                            id="upload-process-status"
                                            label="Upload Process Status"
                                            defaultChecked={this.props.uploadProcessStatus}
                                            disabled={this.props.optOut}
                                        />
                                        <Form.Check
                                            type="switch"
                                            id="upload-errors"
                                            label="Upload Errors"
                                            defaultChecked={this.props.uploadErrors}
                                            disabled={this.props.optOut}
                                        />
                                        <Form.Check
                                            type="switch"
                                            id="critical-events"
                                            label="Critical Events"
                                            defaultChecked={this.props.criticalEvents}
                                            disabled={this.props.optOut}
                                        />
                                        <br/>
                                        <Dropdown>
                                            <Dropdown.Toggle id="dropdown-basic">
                                                Report Frequency
                                            </Dropdown.Toggle>

                                            <Dropdown.Menu>
                                                <Dropdown.Item href="#/action-1">Daily</Dropdown.Item>
                                                <Dropdown.Item href="#/action-2">Weekly</Dropdown.Item>
                                                <Dropdown.Item href="#/action-3">Monthly</Dropdown.Item>
                                                <Dropdown.Item href="#/action-3">Quarterly</Dropdown.Item>
                                                <Dropdown.Item href="#/action-3">Yearly</Dropdown.Item>
                                            </Dropdown.Menu>

                                        </Dropdown>

                                        <br/>

                                        <Button onClick={(this.props.saveEmailPreferences())}>
                                            Save
                                        </Button>
                                    </Form>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}


export
{
    EmailPreferences
}
    ;
