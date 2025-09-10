import 'bootstrap';

import React from "react";
import { createRoot } from 'react-dom/client';

import { MetricViewerSettings } from "./metricviewer_preferences.js";
import { AirSyncSettings } from "./airsync_settings.js";

import SignedInNavbar from "./signed_in_navbar.js";

import { EmailSettingsTableUser } from "./email_settings.js";
import MultifleetInvites from './multifleet/multifleet_invites';
import MultifleetSelect from './multifleet/multifleet_select';
import { showErrorModal } from './error_modal.js';

/* 
    import { ACCESS_TYPES } from './constants/access';
    
    const TEST_FLEETS_WITH_ACCESS = [
        { fleetName: "Test Fleet Z", fleetAccess: "VIEW" },
        { fleetName: "Test Fleet X", fleetAccess: "VIEW" },
        { fleetName: "Test Fleet Denied", fleetAccess: "DENIED" },
        { fleetName: "Test Fleet Waiting", fleetAccess: "WAITING" },
        { fleetName: "Test Fleet Upload", fleetAccess: "UPLOAD" },
        { fleetName: "Test Fleet Manager", fleetAccess: "MANAGER" },
    ];

    //Order TEST_FLEETS_WITH_ACCESS by ACCESS_TYPES
    TEST_FLEETS_WITH_ACCESS.sort((a, b) => {
        return ACCESS_TYPES.indexOf(b.fleetAccess) - ACCESS_TYPES.indexOf(a.fleetAccess);
    });
*/




class PreferencesPage extends React.Component {

    constructor(props) {

        console.log("Preferences Page Props: ", props);

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
            airsyncEnabled : airsyncEnabled,
            multifleetInvites : [],
            fleetsWithAccess: [],
            fleetsWithAccessFetched: false,
        };

        console.log("This users prefs:", this.props);

    }

    componentDidMount() {

        $.ajax({
            type: 'GET',
            url: `/api/user/multifleet-invites`,
            async: true,
            success: (response) => {
                console.log("Fetched Multifleet invites:", response);
                this.setState({ multifleetInvites: response });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("There was an error fetching Multifleet invites:", errorThrown);
                showErrorModal("Error fetching Multifleet invites", errorThrown);
            }
        });

        $.ajax({
            type: 'GET',
            url: `/api/user/fleet-access`,
            async: true,
            success: (response) => {
                console.log("Fetched Fleet Access:", response);
                this.setState({
                    fleetsWithAccess: response,
                    fleetsWithAccessFetched: true
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("There was an error fetching Fleet Access:", errorThrown);
                showErrorModal("Error fetching Fleet Access", errorThrown);
            }
        });

    }

    updateSelectedFleet = (fleetIdSelected: number) => {

        console.log(`Updating selected fleet to ${fleetIdSelected}`);

        const submissionData = {
            fleetIdSelected: fleetIdSelected
        };

        $.ajax({
            type: 'PUT',
            url: `/api/user/select-fleet`,
            data: submissionData,
            async: true,
            success: (response) => {
                console.log("Successfully updated selected fleet:", response);
                //Reload page to update everything
                window.location.reload();
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("There was an error updating selected fleet:", errorThrown);
                showErrorModal("Error updating selected fleet", errorThrown);
            }
        });

    };

    removeMultifleetInviteLocally = (fleetName) => {
        const updatedInvites = this.state.multifleetInvites.filter(invite => invite.fleetName !== fleetName);
        this.setState({ multifleetInvites: updatedInvites });
        console.log(`Removed invite for fleet '${fleetName}' locally.`);
    };

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

                                {/* Multifleet Invites */}
                                <MultifleetInvites
                                    invites={this.state.multifleetInvites}
                                    removeMultifleetInviteLocally={this.removeMultifleetInviteLocally}
                                />

                                {/* Multifleet Select */}
                                {
                                    this.state.fleetsWithAccessFetched
                                    &&
                                    <MultifleetSelect
                                        fleetsWithAccess={this.state.fleetsWithAccess}
                                        fleetSelected={this.props.userFleetSelected}
                                        updateSelectedFleet={this.updateSelectedFleet}
                                    />
                                }

                                {/* Metric Viewer Settings */}
                                <MetricViewerSettings
                                    isVertical={false}
                                    selectedMetrics={this.state.selectedMetrics}
                                    decimalPrecision={this.state.decimalPrecision}>
                                </MetricViewerSettings>

                                {/* Admin Content */}
                                {adminContent}

                                {/* User Email Preferences */}
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
        userFleetSelected={userFleetSelected}
        airsyncTimeout={airsyncTimeout}
        waitingUserCount={waitingUserCount}
        unconfirmedTailsCount={unconfirmedTailsCount}
    />
);