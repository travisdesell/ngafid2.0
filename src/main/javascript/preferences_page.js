import 'bootstrap';

import React, {Component} from "react";
import ReactDOM from "react-dom";

import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import ListGroup from 'react-bootstrap/ListGroup';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';

import {errorModal} from "./error_modal.js";
import {MetricViewerSettings} from "./metricviewer_preferences.js";
import SignedInNavbar from "./signed_in_navbar.js";
import {EmailPreferences} from "./email_preferences.js";

class PreferencesPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            fullName: userName,
            waitingUserCount: this.props.waitingUserCount,
            unconfirmedTailsCount: this.props.unconfirmedTailsCount,
            selectedMetrics: userPreferences.flightMetrics,
            decimalPrecision: userPreferences.decimalPrecision,

            emailOptOut: userPreferences.emailOptOut,
            emailUploadProcessing: userPreferences.emailUploadProcessing,
            emailUploadStatus: userPreferences.emailUploadStatus,
            emailCriticalEvents: userPreferences.emailCriticalEvents,
            emailUploadError: userPreferences.emailUploadError,
            emailFrequency: userPreferences.emailFrequency,

        };

        console.log("this users prefs:");
        console.log(this.state);
    }

    saveEmailPreferences() {
        console.log("saving email prefs");
    }


    render() {
        return (
            <div>
                <SignedInNavbar activePage="account" waitingUserCount={this.state.waitingUserCount}
                                fleetManager={fleetManager} unconfirmedTailsCount={this.state.unconfirmedTailsCount}
                                modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                <div className="container-fluid">
                    <div className="row">
                        <div className="col" style={{paddingRight: "16"}}>
                            <div style={{marginTop: "4", padding: "0 0 0 0"}}>
                                <div className="col" style={{padding: "0 0 0 0"}}>
                                    <div className="card" style={{background: "rgba(248,259,250,0.8)"}}>
                                        <h5 className="card-header">
                                            {this.state.fullName}'s Preferences:
                                        </h5>
                                        <MetricViewerSettings
                                            isVertical={false}
                                            selectedMetrics={this.state.selectedMetrics}
                                            decimalPrecision={this.state.decimalPrecision}>
                                        </MetricViewerSettings>

                                        <EmailPreferences optOut={this.state.emailOptOut}
                                                          uploadProcessing={this.state.emailUploadProcessing}
                                                          uploadProcessStatus={this.state.emailUploadProcessing}
                                                          criticalEvents={this.state.emailCriticalEvents}
                                                          uploadError={this.state.emailUploadError}
                                                          emailFrequency={this.state.emailFrequency}
                                                          saveEmailPreferences={() => this.saveEmailPreferences}>
                                        </EmailPreferences>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

console.log("setting preferences page with react!");

var preferencesPage = ReactDOM.render(
    <PreferencesPage userPreferences={userPreferences} waitingUserCount={waitingUserCount}
                     unconfirmedTailsCount={unconfirmedTailsCount}/>,
    document.querySelector('#preferences-page')
)
