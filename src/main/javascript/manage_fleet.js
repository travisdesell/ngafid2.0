import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";

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
            fleetUser : fleetUser
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

                if (newAccess == "WAITING" && previousAccess != "WAITING") {
                    console.log("incrementing waiting!");
                    navbar.incrementWaiting();
                } else if (newAccess != "WAITING" && previousAccess == "WAITING") {
                    console.log("decrementing waiting!");
                    navbar.decrementWaiting();
                }

                //update the fleetUser's new original access (so the update button will be disabled unless the access is changed again)
                fleetUser.fleetAccess.originalAccess = newAccess;
                fleetUserRow.state.fleetUser = fleetUser;
                fleetUserRow.setState(fleetUser);
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

        console.log("rendering " + fleetUser.email + " with access " + accessType);

        let buttonClasses = "btn btn-outline-secondary";
        let buttonDisabled = fleetUser.fleetAccess.originalAccess == accessType;

        return (
            <tr userid={fleetUser.id}>
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

class ManageFleetCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            user : this.props.user
        };

        console.log("constructed ManageFleetCard");
    }

    setUser(user) {
        console.log("set manage fleet card user");
        console.log(user);
        this.state.user = user;
        this.setState(this.state);
    }

    render() {
        const hidden = this.props.hidden;
        const bgStyle = {opacity : 0.8};
        const fgStyle = {opacity : 1.0};

        let user = "";
        let fleetName = "";
        let fleetUsers = []
        if (typeof this.state.user != 'undefined') {
            user = this.state.user;
            fleetName = user.fleet.name;

            if (typeof user.fleet.users != 'undefined') {
                fleetUsers = user.fleet.users;
            }
        }

        return (
            <div className="card-body" hidden={hidden}>
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
                                {
                                    fleetUsers.map((fleetUser, index) => {
                                        return <FleetUserRow key={fleetUser.id} fleetUser={fleetUser} />
                                    })
                                }
                            </tbody>
                        </table>

                    </div>

                </div>
            </div>
        );
    }
}

var manageFleetCard = ReactDOM.render(
    <ManageFleetCard user={user} />,
    document.querySelector('#manage-fleet-card')
);
