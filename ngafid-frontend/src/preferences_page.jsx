import 'bootstrap';

import React from "react";
import { createRoot } from 'react-dom/client';

import { MetricViewerSettings } from "./metricviewer_preferences";
import { AirSyncSettings } from "./airsync_settings";

import SignedInNavbar from "./signed_in_navbar";

import { EmailSettingsTableUser } from "./email_settings";


class PreferencesPage extends React.Component {

    constructor(props) {

        super(props);

        let airsyncEnabled = false;
        if (props.airsyncTimeout != -1)
            airsyncEnabled = true;

        this.state = {
            fullName : userName,
            airsyncTimeout : this.props.airsyncTimeout,
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount,
            selectedMetrics : userPreferences.flightMetrics,
            decimalPrecision : userPreferences.decimalPrecision,
            airsyncEnabled : airsyncEnabled
        };

        console.log("This users prefs:", this.props);

    }


    render() {

        const userNameDisplay = (
            this.state.fullName.replace(/\s/g, '').length > 0
            ? `${this.state.fullName}'s Preferences`
            : "Your Preferences"
        );

        console.log(`FULL NAME: '${this.state.fullName}'`);

        let adminContent = "";
        if (this.props.isAdmin && this.state.airsyncEnabled) {

            console.log(`Timeout is: ${  this.props.airsyncTimeout}`);
            adminContent = (
                <AirSyncSettings
                    isVertical={false}
                    selectedMetrics={this.state.selectedMetrics}
                    decimalPrecision={this.state.decimalPrecision}
                    timeout={this.props.airsyncTimeout}>
                </AirSyncSettings>
            );

        }

        return (
            <div style={{overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar
                        activePage="account"
                        waitingUserCount={this.state.waitingUserCount}
                        fleetManager={fleetManager}
                        unconfirmedTailsCount={this.state.unconfirmedTailsCount}
                        modifyTailsAccess={modifyTailsAccess}
                        plotMapHidden={plotMapHidden}
                    />
                </div>

                <div style={{overflowY:"auto", flex:"1 1 auto"}}>
                    <div className="card-body-rounded m-2">
                    <div style={{marginTop:"4", padding:"0 0 0 0"}}>
                        <div className="col" style={{padding:"0 0 0 0"}}>
                            <div className="card">
                                <h5 className="card-header">
                                    {userNameDisplay}
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
                                                Your Email Preferences
                                            </h6>
                                            <div className="form-group my-4 px-4">
                                                <div className="d-flex">
                                                    <EmailSettingsTableUser isAdmin={isAdmin}/>
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

const container = document.querySelector("#preferences-page");
const root = createRoot(container);
root.render(
    <PreferencesPage
        userPreferences={userPreferences}
        isAdmin={isAdmin}
        airsyncTimeout={airsyncTimeout}
        waitingUserCount={waitingUserCount}
        unconfirmedTailsCount={unconfirmedTailsCount}
    />
);