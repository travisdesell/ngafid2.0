import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import ListGroup from 'react-bootstrap/ListGroup';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';

import { errorModal } from "./error_modal.jsx";
import { MetricViewerSettings } from "./metricviewer_preferences.jsx";
import { AirSyncSettings } from "./airsync_settings.jsx";

import SignedInNavbar from "./signed_in_navbar.jsx";

import { EmailSettingsTableUser } from "./email_settings.jsx";

class PreferencesPage extends React.Component {
    constructor(props) {
        super(props);

        let airsyncEnabled = false;
        if (props.airsyncTimeout != -1) {
            airsyncEnabled = true;
        }

        this.state = {
            fullName : userName,
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount,
            selectedMetrics : userPreferences.flightMetrics,
            decimalPrecision : userPreferences.decimalPrecision,
            airsyncEnabled : airsyncEnabled
        };

        console.log("this users prefs:");
        console.log(this.props);
    }


    render() {
        let adminContent = "";
        let userName = this.state.fullName + "'s Preferences";

        if (this.props.isAdmin) {
            if (this.state.airsyncEnabled) {
                console.log("timeout is: " + this.props.airsyncTimeout);
                adminContent = (
                    <AirSyncSettings
                        isVertical={false}
                        selectedMetrics={this.state.selectedMetrics}
                        decimalPrecision={this.state.decimalPrecision}
                        timeout={this.props.airsyncTimeout}>
                    </AirSyncSettings>
                );
            }
        }

        return (
            <div style={{overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar activePage="account" waitingUserCount={this.state.waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={this.state.unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY:"auto", flex:"1 1 auto"}}>
                    <div className="card-body-rounded m-2">
                    <div style={{marginTop:"4", padding:"0 0 0 0"}}>
                        <div className="col" style={{padding:"0 0 0 0"}}>
                            <div className="card">
                                <h5 className="card-header">
                                    {this.state.fullName}'s Preferences:
                                </h5>
                                <MetricViewerSettings
                                    isVertical={false}
                                    selectedMetrics={this.state.selectedMetrics}
                                    decimalPrecision={this.state.decimalPrecision}>
                                </MetricViewerSettings>

                                {adminContent}

                                <div className="card-body">
                                    <div className="col" style={{padding:"0 0 0 0"}}>
                                        <div className="card-alt card">
                                            <h6 className="card-header">
                                                Your Email Preferences:
                                            </h6>
                                            <div className="form-group">
                                                <div className="d-flex">
                                                    <EmailSettingsTableUser isAdmin={isAdmin}></EmailSettingsTableUser>
                                                <hr style={{padding:"0", margin:"0 0 0 0"}}></hr>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
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
    <PreferencesPage userPreferences={userPreferences} isAdmin={isAdmin} airsyncTimeout={airsync_timeout} waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
   document.querySelector('#preferences-page')
);