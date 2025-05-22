import 'bootstrap';
import React from "react";
import ReactDOM from "react-dom";

import {errorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";
import {EmailSettingsTableManager} from "./email_settings.js";


import './index.css'          //<-- include Tailwind
import { setSourceMapsEnabled } from 'process';



class AccessCheck extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {

        const checkType = this.props.checkType;
        const lcType = checkType.toLowerCase();
        const ucType = checkType.toUpperCase();
        const slcType = lcType.charAt(0).toUpperCase() + lcType.slice(1);

        const userAccess = this.props.userAccess;
        const fleetUserRow = this.props.fleetUserRow;
        const userId = this.props.userId;

        const radioId = (lcType + "AccessRadio" + userId);

        return (
            <div className="form-check form-check-inline">
                <input className="form-check-input" type="radio" name={"accessRadios" + userId} id={radioId}
                       value={ucType} checked={ucType == userAccess} onChange={() => fleetUserRow.checkRadio(ucType)}/>
                <label className="form-check-label" htmlFor={radioId}>
                    {slcType}
                </label>
            </div>
        );
    }
}

class FleetUserRow extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            fleetUser : props.fleetUser,
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount,
            settingIndex : props.settingIndex
        };

        this.state.fleetUser.fleetAccess.originalAccess = props.fleetUser.fleetAccess.accessType;

    }

    checkRadio = (newRadio) => {

        this.setState(prev => ({
            fleetUser: {
            ...prev.fleetUser,
            fleetAccess: {
                ...prev.fleetUser.fleetAccess,
                accessType: newRadio
                }
            }
        }));
    };


    updateUserAccess = () => {

        const { fleetUser } = this.state;

        console.log("Updating user access for:", fleetUser);
        $("#loading").show();

        const submissionData = {
            fleetUserId : fleetUser.id,
            fleetId : fleetUser.fleetAccess.fleetId,
            accessType : fleetUser.fleetAccess.accessType
        };

        const thisFleetRow = this;

        $.ajax({
            type: 'PATCH',
            url: `/api/user/${fleetUser.id}/fleet-access`,
            data: submissionData,
            dataType: 'json',
            async: true,
            success: function (response) {
                $('#loading').hide();

                if (response && response.errorTitle) {
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                let previousAccess = fleetUser.fleetAccess.originalAccess;
                let newAccess = fleetUser.fleetAccess.accessType;


                const updatedFleetUser = {
                    ...fleetUser,
                    fleetAccess: {
                        ...fleetUser.fleetAccess,
                        originalAccess: fleetUser.fleetAccess.accessType
                    }
                };

                thisFleetRow.props.onFleetUserUpdated(updatedFleetUser);

                thisFleetRow.setState({ fleetUser: updatedFleetUser });

            },
            error: function (jqXHR, textStatus, errorThrown) {
                $("#loading").hide();
                errorModal.show("Error Loading Uploads", errorThrown);
            },
        });
    }

    render() {

        const fleetUser = this.state.fleetUser;
        const accessType = fleetUser.fleetAccess.accessType;

        const buttonClasses = "btn btn-outline-primary w-32 h-12 not-italic !font-sans";
        const buttonVisible = (fleetUser.fleetAccess.originalAccess != accessType);

        const rowIndex = this.props.index;
        const rowClassName = (this.props.isDenied ? `italic opacity-50` : `opacity-100 ${rowIndex%2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"}`)

        const nameExists = (fleetUser.firstName || fleetUser.lastName);
        const userNameDisplay = nameExists ? `${fleetUser.firstName} ${fleetUser.lastName}` : "Unknown Name...";
        const userNameClassName = nameExists ? `truncate whitespace-nowrap overflow-hidden` : `truncate whitespace-nowrap overflow-hidden italic`;

        return (
            <tr userid={fleetUser.id} className={rowClassName}>

                {/* User Email */}
                <td className="whitespace-wrap pl-4">
                    {fleetUser.email}
                </td>

                {/* User Name */}
                <td className={userNameClassName}>
                    {userNameDisplay}
                </td>

                {/* Update Access */}
                <td className="gap-3 flex flex-row items-center font-mono">
                    <AccessCheck checkType="MANAGER" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="UPLOAD" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="VIEW" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="WAITING" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="DENIED" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>

                    {
                        buttonVisible
                        &&
                        <button className={buttonClasses} onClick={this.updateUserAccess}>
                            Update Access
                        </button>
                    }

                </td>
            </tr>
        );
    }
}

class ManageFleetPage extends React.Component {

    constructor(props) {
        super(props);

        this.state = {
            user: this.props.user,
            fleetUsers: [],
            waitingUserCount: this.props.waitingUserCount,
            unconfirmedTailsCount: this.props.unconfirmedTailsCount,
            showDeniedUsers: false
        };
    }

    componentDidMount() {
        this.sortAndSetUsers();
    }

    sortAndSetUsers() {
        const {user} = this.state;
        if (user && user.fleet && Array.isArray(user.fleet.users)) {
            const sortedUsers = [...user.fleet.users].sort((a, b) =>
                (a.fleetAccess.accessType === "DENIED") ? 1 : -1
            );

            this.setState({ fleetUsers: sortedUsers });
        } else {
            console.warn("User data or fleet users array is missing.");
        }
    }

    setUser(user) {
        this.setState({user: user});
    }

    incrementWaiting() {
        this.setState({waitingUserCount: (this.state.waitingUserCount + 1)});
    }

    decrementWaiting() {
        this.setState({waitingUserCount: (this.state.waitingUserCount - 1)});
    }

    sendEmail = (email) => {

        const { user } = this.state;

        const submissionData = {
            email : email,
            fleetName: user.fleet.name,
            fleetId: user.fleet.id
        };

        $.ajax({
            type: 'POST',
            url: '/api/user/invite',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {
                if (response.errorTitle) {
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }
                alert('Email invite sent to ' + email + '.');
                $('#inviteEmail').val('');
            },
            error: (jqXHR, textStatus, errorThrown) => {
                errorModal.show("Error Sending Invite");
            }
        });
    };

    handleSubmit = (event) => {
        event.preventDefault();
        const email = event.target.email.value;
        const emailPattern = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;
        if (!emailPattern.test(email)) {
            alert('Please enter a valid email address.');
            return;
        }
        this.sendEmail(email);
    };

    handleFleetUserUpdated = (updated) => {

        this.setState((prev) => ({

            fleetUsers: prev.fleetUsers.map((u) => (u.id === updated.id ? updated : u)),

            //Keep the derived counts in sync, too
            waitingUserCount:
                prev.waitingUserCount
                + (updated.fleetAccess.accessType === "WAITING" && updated.fleetAccess.originalAccess !== "WAITING")
                - (updated.fleetAccess.accessType !== "WAITING" && updated.fleetAccess.originalAccess === "WAITING")

        }));

    };

    render() {
        const hidden = this.props.hidden;

        const user = this.state.user;
        const fleetName = user?.fleet?.name || "";

        return (
            <div>
                <SignedInNavbar
                    activePage="account"
                    waitingUserCount={this.state.waitingUserCount}
                    fleetManager={fleetManager}
                    unconfirmedTailsCount={this.state.unconfirmedTailsCount}
                    modifyTailsAccess={modifyTailsAccess}
                    plotMapHidden={plotMapHidden}
                />

                <div className="m-4" hidden={hidden}>

                    <div className="card mb-1">

                        <h5 className="card-header">
                            Manage {fleetName} Users
                        </h5>

                        <div className="card-body">

                            {/* Header Row (Invitation + Toggle Denied) */}
                            <div className="flex row mx-1 mb-3 invite items-center">

                                {/* User Invitation */}
                                <div className="d-flex flex-row items-center">

                                    <p style={{marginBottom: "0", marginRight: "10px"}}>
                                        Invite user to <b>{fleetName}</b>:
                                    </p>

                                    <form
                                        onSubmit={this.handleSubmit}
                                        className="d-flex align-items-center"
                                        style={{marginRight: "10px"}}
                                    >

                                        {/* Email Input Field */}
                                        <input
                                            id="inviteEmail"
                                            type="email"
                                            placeholder="Enter user email"
                                            name="email"
                                            pattern="[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"
                                            title="Please enter a valid email address"
                                            required
                                            className="form-control mr-1"
                                        />

                                        {/* Submit Invite Button */}
                                        <button className="btn btn-primary" type="submit">
                                            Invite
                                        </button>

                                    </form>
                                </div>

                                {/* Toggle Denied Users */}
                                <button
                                    className="btn btn-outline-primary ml-auto"
                                    onClick={() => this.setState({showDeniedUsers: !this.state.showDeniedUsers})}
                                    style={{
                                        lineHeight: "1.5",
                                        fontSize: "1rem",
                                    }}
                                >
                                    {this.state.showDeniedUsers ? "Hide Denied Users" : "Show Denied Users"}
                                </button>


                            </div>

                            {/* Fleet Access Levels */}
                            <div className="card-alt card mt-4">

                                <h6 className="card-header">
                                    Fleet Access Levels
                                </h6>

                                <div className="form-group my-2 px-4">
                                    <table className="table-hover rounded-lg w-full">

                                        <colgroup>
                                            <col style={{ width: "17.5%" }} />
                                            <col style={{ width: "17.5%" }} />
                                            <col style={{ width: "65.0%" }} />
                                        </colgroup>

                                        <thead className="leading-16 text-[var(--c_text)] border-b-1">
                                            <tr>
                                                <th scope="col" className="pl-4">Email</th>
                                                <th scope="col">Name</th>
                                                <th scope="col">Access Level</th>
                                            </tr>
                                        </thead>

                                        <tbody className="text-[var(--c_text)] leading-16 before:content-['\A']">

                                            {/* Empty spacer row */}
                                            <tr className="pointer-none bg-transparent">
                                                <td colSpan={3} className="h-6"/>
                                            </tr>

                                            {
                                                this.state.fleetUsers
                                                .filter(user => this.state.showDeniedUsers || user.fleetAccess.accessType !== "DENIED")
                                                .filter(user => user.id !== this.state.user.id) //<-- Exclude self
                                                .map((fleetUser, index) => {

                                                    const isDenied = fleetUser.fleetAccess.accessType === "DENIED";

                                                    return (
                                                        <FleetUserRow
                                                            onFleetUserUpdated={this.handleFleetUserUpdated}
                                                            key={fleetUser.id}
                                                            index={index}
                                                            fleetUser={fleetUser}
                                                            isDenied={isDenied}
                                                            incrementWaiting={() => this.incrementWaiting()}
                                                            decrementWaiting={() => this.decrementWaiting()}
                                                            onAccessChange={() => this.sortAndSetUsers()}
                                                        />
                                                    );

                                                })
                                            }

                                            {/* Empty spacer row */}
                                            <tr className="pointer-none bg-transparent">
                                                <td colSpan={3} className="h-6"/>
                                            </tr>

                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            {/* Fleet Email Settings */}
                            <div className="card-alt card mt-4">
                                <h6 className="card-header">
                                    Fleet Email Settings
                                </h6>
                                <div className="form-group my-2 px-4">
                                    <div className="d-flex">
                                        {
                                            (this.state.fleetUsers.length > 0)
                                            &&
                                            <EmailSettingsTableManager
                                                fleetUsers={this.state.fleetUsers}
                                                showDeniedUsers={this.state.showDeniedUsers}
                                            />
                                        }
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

var manageFleetPage = ReactDOM.render(
    <ManageFleetPage user={user} waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
    document.querySelector('#manage-fleet-page')
);
