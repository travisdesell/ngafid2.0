import 'bootstrap';
import React from "react";
import ReactDOM from "react-dom";

import {errorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";
import {EmailSettingsTableManager} from "./email_settings.js";

class AccessCheck extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let checkType = this.props.checkType;
        let lcType = checkType.toLowerCase();
        let ucType = checkType.toUpperCase();
        let slcType = lcType.charAt(0).toUpperCase() + lcType.slice(1);

        let userAccess = this.props.userAccess;
        let fleetUserRow = this.props.fleetUserRow;
        let userId = this.props.userId;

        let radioId = lcType + "AccessRadio" + userId;

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

        let fleetUser = props.fleetUser;
        fleetUser.fleetAccess.originalAccess = fleetUser.fleetAccess.accessType;

        this.state = {
            fleetUser: fleetUser,
            waitingUserCount: this.props.waitingUserCount,
            unconfirmedTailsCount: this.props.unconfirmedTailsCount,
            settingIndex: props.settingIndex
        };
    }

    checkRadio(newRadio) {
        this.state.fleetUser.fleetAccess.accessType = newRadio;
        this.setState(this.state);
    }

    updateUserAccess(fleetUser) {
        console.log("updating user access for:", fleetUser);
        $("#loading").show();

        var submissionData = {
            fleetUserId: fleetUser.id,
            fleetId: fleetUser.fleetAccess.fleetId,
            accessType: fleetUser.fleetAccess.accessType
        };

        var fleetUserRow = this;

        $.ajax({
            type: 'PATCH',
            url: `/api/user/${fleetUser.id}/fleet-access`,
            data: submissionData,
            dataType: 'json',
            success: function (response) {
                $('#loading').hide();

                if (response.errorTitle) {
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                let previousAccess = fleetUser.fleetAccess.originalAccess;
                let newAccess = fleetUser.fleetAccess.accessType;

                if (newAccess == "WAITING" && previousAccess != "WAITING") {
                    fleetUserRow.props.incrementWaiting();
                } else if (newAccess != "WAITING" && previousAccess == "WAITING") {
                    fleetUserRow.props.decrementWaiting();
                }

                fleetUser.fleetAccess.originalAccess = newAccess;
                fleetUserRow.state.fleetUser = fleetUser;
                fleetUserRow.setState(fleetUser);

                fleetUserRow.props.onAccessChange();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                $("#loading").hide();
                errorModal.show("Error Loading Uploads", errorThrown);
            },
            async: true
        });
    }

    render() {
        let fleetUser = this.state.fleetUser;
        let accessType = fleetUser.fleetAccess.accessType;
        const {rowStyle} = this.props;

        let buttonClasses = "btn btn-outline-secondary";
        let buttonDisabled = fleetUser.fleetAccess.originalAccess == accessType;

        return (
            <tr userid={fleetUser.id} style={rowStyle}>
                <td scope="row" style={{padding: "15 12 15 12"}}>{fleetUser.email}</td>
                <td style={{padding: "15 12 15 12"}}>{fleetUser.firstName} {fleetUser.lastName}</td>
                <td style={{padding: "15 12 15 12"}}>
                    <AccessCheck checkType="MANAGER" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="UPLOAD" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="VIEW" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="WAITING" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="DENIED" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                </td>
                <td>
                    <button className={buttonClasses} style={{padding: "2 6 2 6"}} disabled={buttonDisabled}
                            onClick={() => this.updateUserAccess(fleetUser)}>
                        Update
                    </button>
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
                a.fleetAccess.accessType === "DENIED" ? 1 : -1
            );
            this.setState({fleetUsers: sortedUsers});
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
        const {user} = this.state;
        var submissionData = {
            email: email,
            fleetName: user.fleet.name,
            fleetId: user.fleet.id
        };

        $.ajax({
            type: 'POST',
            url: '/api/user/invite',
            data: submissionData,
            dataType: 'json',
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
            },
            async: true
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

    render() {
        const hidden = this.props.hidden;
        const bgStyle = {opacity: 0.8};
        const fgStyle = {opacity: 1.0};
        const grayoutStyle = {backgroundColor: '#d3d3d3'};

        let user = this.state.user;
        let fleetName = user?.fleet?.name || "";

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

                <div className="card-body" hidden={hidden}>
                    <div className="row ml-1 mb-2 invite align-items-center" style={{...bgStyle}}>
                        <p style={{...fgStyle, marginBottom: "0", marginRight: "10px"}}>
                            Invite user to {fleetName}:
                        </p>

                        <form
                            onSubmit={this.handleSubmit}
                            className="d-flex align-items-center"
                            style={{marginRight: "10px"}}
                        >
                            <input
                                id="inviteEmail"
                                type="email"
                                placeholder="Enter user email"
                                name="email"
                                pattern="[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"
                                title="Please enter a valid email address"
                                required
                                className="form-control"
                                style={{marginRight: "10px"}}
                            />
                            <button className="btn btn-primary" type="submit">
                                Invite
                            </button>
                        </form>

                        <div className="d-flex align-items-center">
                            <button
                                className="btn btn-outline-primary"
                                onClick={() => this.setState({showDeniedUsers: !this.state.showDeniedUsers})}
                                style={{

                                    lineHeight: "1.5",
                                    fontSize: "1rem",
                                    transform: "translateY(-8px)"
                                }}
                            >
                                {this.state.showDeniedUsers ? "Hide Denied Users" : "Show Denied Users"}
                            </button>


                        </div>
                    </div>


                    <div className="card mb-1" style={bgStyle}>
                        <h5 className="card-header" style={fgStyle}>Manage {fleetName} Users</h5>

                        <div className="card-body" style={fgStyle}>
                            <table className="table">
                                <thead>
                                <tr>
                                    <th scope="col">Email</th>
                                    <th scope="col">Name</th>
                                    <th scope="col">Access Level</th>
                                </tr>
                                </thead>
                                <tbody>
                                {
                                    this.state.fleetUsers
                                        .filter(user => this.state.showDeniedUsers || user.fleetAccess.accessType !== "DENIED")
                                        .map((fleetUser, index) => {
                                            const rowStyle = fleetUser.fleetAccess.accessType === "DENIED" ? grayoutStyle : {};
                                            return (
                                                <FleetUserRow
                                                    key={fleetUser.id}
                                                    fleetUser={fleetUser}
                                                    rowStyle={rowStyle}
                                                    incrementWaiting={() => this.incrementWaiting()}
                                                    decrementWaiting={() => this.decrementWaiting()}
                                                    onAccessChange={() => this.sortAndSetUsers()}
                                                />
                                            );
                                        })
                                }
                                </tbody>
                            </table>

                            <h6 className="card-header" style={{padding: "16px 12px", margin: "0px 0px"}}>Fleet Email
                                Preferences</h6>
                            <div className="form-group">
                                <div className="d-flex">
                                    {this.state.fleetUsers.length > 0 && (
                                        <EmailSettingsTableManager fleetUsers={this.state.fleetUsers}/>
                                    )}
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
