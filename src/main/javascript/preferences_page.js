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

import { errorModal } from "./error_modal.js";
import { MetricViewerSettings } from "./metricviewer_preferences.js";
import { AirSyncSettings } from "./airsync_settings.js";

import SignedInNavbar from "./signed_in_navbar.js";

import { EmailSettingsTableUser } from "./email_settings.js";

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
        	<div>
            	<SignedInNavbar activePage="account" waitingUserCount={this.state.waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={this.state.unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

            	<div className="container-fluid">
                	<div className="row">
                    	<div className="col" style={{paddingRight:"16"}}>
                         	<div style={{marginTop:"4", padding:"0 0 0 0"}}>
                                	<div className="col" style={{padding:"0 0 0 0"}}>
                                    	<div className="card" style={{background : "rgba(248,259,250,0.8)"}}>
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
                                           		 <div className="card" style={{background : "rgba(248,259,250,0.8)"}}>
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
        	</div>
    	);
	}
}

console.log("setting preferences page with react!");

var preferencesPage = ReactDOM.render(
	<PreferencesPage userPreferences={userPreferences} isAdmin={isAdmin} airsyncTimeout={airsync_timeout} waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
   document.querySelector('#preferences-page')
);