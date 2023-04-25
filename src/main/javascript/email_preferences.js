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
                                            onChange={(e) => {
                                                this.props.setOptOut(e)
                                            }}
                                        />
                                        <Form.Check
                                            type="switch"
                                            id="upload-processing"
                                            label="Upload Is Being Processed"
                                            disabled={this.props.optOut}
                                            defaultChecked={this.props.uploadProcessing}
                                            onChange={(e) => {
                                                this.props.setUploadProcessing(e)
                                            }}

                                        />
                                        <Form.Check
                                            type="switch"
                                            id="upload-process-status"
                                            label="Upload Process Status"
                                            defaultChecked={this.props.uploadProcessStatus}
                                            disabled={this.props.optOut}
                                            onChange={(e) => {
                                                this.props.setUploadProcessStatus(e)
                                            }}

                                        />
                                        <Form.Check
                                            type="switch"
                                            id="upload-errors"
                                            label="Upload Errors"
                                            defaultChecked={this.props.uploadError}
                                            disabled={this.props.optOut}
                                            onChange={(e) => {
                                                this.props.setUploadError(e)
                                            }}

                                        />
                                        <Form.Check
                                            type="switch"
                                            id="critical-events"
                                            label="Critical Events"
                                            defaultChecked={this.props.criticalEvents}
                                            disabled={this.props.optOut}
                                            onChange={(e) => {
                                                this.props.setCriticalEvents(e)
                                            }}

                                        />
                                        <br/>
                                        <Form.Group>
                                            <Form.Label>Report Frequency</Form.Label>
                                            <Form.Control as="select" onChange={(e) => this.props.setEmailFrequency(e)} value={this.props.emailFrequency}>
                                                <option value="NEVER">Never</option>
                                                <option value="DAILY">Daily</option>
                                                <option value="WEEKLY">Weekly</option>
                                                <option value="MONTHLY">Monthly</option>
                                                <option value="QUARTERLY">Quarterly</option>
                                                <option value="YEARLY">Yearly</option>
                                            </Form.Control>
                                        </Form.Group>

                                        <br/>

                                        <Button onClick={(this.props.saveEmailPreferences)}>
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
