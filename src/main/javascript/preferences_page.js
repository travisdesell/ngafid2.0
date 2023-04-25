import 'bootstrap';

import React, {Component} from "react";
import ReactDOM from "react-dom";

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
    }


    saveEmailPreferences() {
        console.log("saving email prefs");
        console.log(this.state);

        let emailPreferences = {
            emailOptOut: this.state.emailOptOut,
            emailUploadProcessing: this.state.emailUploadProcessing,
            emailUploadStatus: this.state.emailUploadStatus,
            emailCriticalEvents: this.state.emailCriticalEvents,
            emailUploadError: this.state.emailUploadError,
            emailFrequency: this.state.emailFrequency,
        }

        $.ajax({
            url: "email_preferences",
            type: "PUT",
            data: emailPreferences,
            dataType: "json",
            success: (response) => {
                console.log("received response: ");
                console.log(response);
                this.setState({
                    emailOptOut: response.emailOptOut,
                    emailUploadProcessing: response.emailUploadProcessing,
                    emailUploadStatus: response.emailUploadStatus,
                    emailCriticalEvents: response.emailCriticalEvents,
                    emailUploadError: response.emailUploadError,
                    emailFrequency: response.emailFrequency,
                });
            }
        });
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
                                                          setOptOut={(e) => this.setState({emailOptOut: e.target.checked})}

                                                          uploadProcessing={this.state.emailUploadProcessing}
                                                          setUploadProcessing={(e) => this.setState({emailUploadProcessing: e.target.checked})}

                                                          uploadProcessStatus={this.state.emailUploadStatus}
                                                          setUploadProcessStatus={(e) => this.setState({emailUploadStatus: e.target.checked})}

                                                          criticalEvents={this.state.emailCriticalEvents}
                                                          setCriticalEvents={(e) => this.setState({emailCriticalEvents: e.target.checked})}

                                                          uploadError={this.state.emailUploadError}
                                                          setUploadError={(e) => this.setState({emailUploadError: e.target.checked})}

                                                          emailFrequency={this.state.emailFrequency}
                                                          setEmailFrequency={(e) => this.setState({emailFrequency: e.target.value})}

                                                          saveEmailPreferences={() => this.saveEmailPreferences()}
                                        />
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
