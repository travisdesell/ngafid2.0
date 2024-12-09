import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";
import { EmailSettingsTableManager } from "./email_settings.js";

class AccessCheck extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let checkType = this.props.checkType;
        let lcType = checkType.toLowerCase();
        let ucType = checkType.toUpperCase();

        let userAccess = this.props.userAccess;
        let fleetUserRow = this.props.fleetUserRow;
        let userId = this.props.userId;

        let radioId = lcType + "AccessRadio" + userId;

        //console.log("rendering check " + checkType + " with access: " + userAccess);

        return (
            <div className="form-check form-check-inline">
                <input className="form-check-input" type="radio" name={"accessRadios" + userId} id={radioId} value={ucType} checked={ucType == userAccess} onChange={() => fleetUserRow.checkRadio(ucType)}/>
                <label className="form-check-label" htmlFor={radioId}>
                    {lcType}
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
            fleetUser : fleetUser,
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount
        };
    }

    checkRadio(newRadio) {
        //console.log("changing " + this.state.fleetUser.email + " access to " + newRadio);
        this.state.fleetUser.fleetAccess.accessType = newRadio;
        this.setState(this.state);
    }

    updateUserAccess(fleetUser) {
        console.log("updating user access for:");
        console.log(fleetUser);

        $("#loading").show();

        var submissionData = {
            fleetUserId : fleetUser.id,
            fleetId : fleetUser.fleetAccess.fleetId,
            accessType : fleetUser.fleetAccess.accessType
        };

        var fleetUserRow = this;

        $.ajax({
            type: 'POST',
            url: '/protected/update_user_access',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                $('#loading').hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                let previousAccess = fleetUser.fleetAccess.originalAccess;
                let newAccess = fleetUser.fleetAccess.accessType;

                console.log("previousAccess: " + previousAccess + ", newAccess: " + newAccess);

                if (newAccess == "WAITING" && previousAccess != "WAITING") {
                    console.log("incrementing waiting!");
                    fleetUserRow.props.incrementWaiting();
                } else if (newAccess != "WAITING" && previousAccess == "WAITING") {
                    console.log("decrementing waiting!");
                    fleetUserRow.props.decrementWaiting();
                }

                // Update the fleetUser's new original access level so that update button will be disabled unless the access is changed again.
                fleetUser.fleetAccess.originalAccess = newAccess;
                fleetUserRow.state.fleetUser = fleetUser;
                fleetUserRow.setState(fleetUser);

                // Callback to sort Users so that Denied users appear on the bottom.
                fleetUserRow.props.onAccessChange();
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                $("#loading").hide();
                errorModal.show("Error Loading Uploads", errorThrown);
            },   
            async: true
        });  

    }

    render() {
        let fleetUser = this.state.fleetUser;
        let accessType = fleetUser.fleetAccess.accessType;
        const { rowStyle } = this.props;

        console.log("rendering " + fleetUser.email + " with access " + accessType);

        let buttonClasses = "btn btn-outline-secondary";
        let buttonDisabled = fleetUser.fleetAccess.originalAccess == accessType;

        return (
            <tr userid={fleetUser.id} style={rowStyle}>
                <td scope="row" style={{padding: "15 12 15 12"}}>{fleetUser.email}</td>
                <td style={{padding: "15 12 15 12"}}>{fleetUser.firstName} {this.state.fleetUser.lastName}</td>

                <td style={{padding: "15 12 15 12"}}>
                    <AccessCheck checkType="MANAGER" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="UPLOAD" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="VIEW" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="WAITING" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                    <AccessCheck checkType="DENIED" userAccess={accessType} fleetUserRow={this} userId={fleetUser.id}/>
                </td>

                <td>
                    <button className={buttonClasses} style={{padding : "2 6 2 6"}} disabled={buttonDisabled} onClick={() => this.updateUserAccess(fleetUser)}>
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
            user : this.props.user,
            fleetUsers: [],
            waitingUserCount : this.props.waitingUserCount,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount,
            showDeniedUsers: false
        };

        console.log("constructed ManageFleetPage");
    }

    componentDidMount() {
        this.sortAndSetUsers();
    }

    /**
     * Initializes fleetUsers state from a correspondent prop
     * Sorts fleetUsers putting the ones with accessType "DENIED" to the bottom of the list
     */
    sortAndSetUsers() {
        const { user } = this.state;
        if (user && user.fleet && Array.isArray(user.fleet.users)) {
            const sortedUsers = [...user.fleet.users].sort((a, b) =>
                a.fleetAccess.accessType === "DENIED" ? 1 : -1
            );
            this.setState({ fleetUsers: sortedUsers });
        } else {
            console.warn("User data or fleet users array is missing.");
        }
    }

    setUser(user) {
        console.log("set manage fleet card user");
        console.log(user);
        this.setState({
            user : user
        });
    }

    incrementWaiting() {
        console.log("incrementing waiting on page!");
        this.setState({
            waitingUserCount : (this.state.waitingUserCount + 1)
        });
    }

    decrementWaiting() {
        console.log("decrementing waiting on page!");
        this.setState({
            waitingUserCount : (this.state.waitingUserCount - 1)
        });
    }

    sendEmail = (email) => {
        const {user} = this.state;
        var submissionData = {
            email : email,
            fleetName: user.fleet.name,
            fleetId: user.fleet.id
        };

        $.ajax({
            type: 'POST',
            url: '/protected/send_user_invite',
            data: submissionData,
            dataType: 'json',
            success: (response) => {
                console.log('Email Invite sent successfully:', response);

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }
                alert('Email invite sent to ' + email + '.');
                $('#inviteEmail').val(' ');

            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error('Failed to send email invite:');
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
        const bgStyle = { opacity: 0.8 };
        const fgStyle = { opacity: 1.0 };
        const grayoutStyle = { backgroundColor: '#d3d3d3' };

        let fleetName = "";

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
                    <div className="row ml-1 mb-2 invite" style={{ ...bgStyle, display: "flex", alignItems: "center" }}>
                        <p style={{ ...fgStyle, marginBottom: "0", marginRight: "10px" }}>
                            Invite user to {fleetName}:
                        </p>
                        <form
                            onSubmit={this.handleSubmit}
                            style={{ display: "flex", alignItems: "center", marginRight: "10px" }}
                        >
                            <input
                                id="inviteEmail"
                                type="email"
                                placeholder="Enter user email"
                                name="email"
                                pattern="[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}"
                                title="Please enter a valid email address"
                                required
                                style={{
                                    marginRight: "10px",
                                    border: "1px solid grey",
                                    borderRadius: "5px",
                                    padding: "5px",
                                    backgroundColor: "transparent",
                                    outline: "none",
                                    transition: "border-color 0.2s ease",
                                }}
                            />
                            <button className="btn btn-primary" type="submit">
                                Invite
                            </button>
                        </form>
                        <button
                            className="btn btn-outline-primary"
                            onClick={() => this.setState({showDeniedUsers: !this.state.showDeniedUsers})}
                            style={{alignSelf: "center"}}
                        >
                            {this.state.showDeniedUsers ? "Hide Denied Users" : "Show Denied Users"}
                        </button>
                    </div>


                    <div className="card mb-1" style={bgStyle}>
                        <h5 className="card-header" style={fgStyle}>
                            Manage {fleetName} Users
                        </h5>

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
                                {this.state.fleetUsers
                                    .filter(userCurrent =>
                                        this.state.showDeniedUsers || userCurrent.fleetAccess.accessType !== "DENIED"
                                    )
                                    .map((fleetUser, index) => {
                                        const rowStyle = fleetUser.fleetAccess.accessType === "DENIED" ? grayoutStyle : {};
                                        return (
                                            <FleetUserRow
                                                key={fleetUser.id}
                                                fleetUser={fleetUser}
                                                rowStyle={rowStyle}
                                                incrementWaiting={() => { this.incrementWaiting(); }}
                                                decrementWaiting={() => { this.decrementWaiting(); }}
                                                onAccessChange={() => this.sortAndSetUsers()}
                                            />
                                        );
                                    })
                                }
                                </tbody>
                            </table>

                            <h6 className="card-header" style={{ padding: "16px 12px", margin: "0px 0px" }}>
                                Fleet Email Preferences
                            </h6>
                            <div className="form-group">
                                <div className="d-flex">
                                    {this.state.fleetUsers.length > 0 && (
                                        <EmailSettingsTableManager fleetUsers={this.state.fleetUsers}></EmailSettingsTableManager>
                                    )}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <style jsx="true">
                    {`
                        .invite p {
                            margin-right: 10px;
                            margin-bottom: auto;
                            margin-top: auto;
                        }
                        .invite form {
                            margin-bottom: auto !important;
                            margin-top: auto !important;
                        }
                        .invite input {
                            border: 1px solid grey;
                            border-radius: 5px;
                            padding: 5px;
                            background-color: transparent;
                            outline: none;
                            transition: border-color 0.2s ease;
                        }
                        .invite input:focus {
                            border-color: #007bff;
                        }
                    `}
                </style>

            </div>
        );
    }
}

var manageFleetPage = ReactDOM.render(
    <ManageFleetPage user={user} waitingUserCount={waitingUserCount} unconfirmedTailsCount={unconfirmedTailsCount}/>,
    document.querySelector('#manage-fleet-page')
);