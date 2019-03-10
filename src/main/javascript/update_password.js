import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";

class UpdatePasswordCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            currentPassword : "",
            newPassword : "",
            confirmPassword : ""
        };
    }

    clearPasswords() {
        this.state = {
            currentPassword : "",
            newPassword : "",
            confirmPassword : ""
        };

        this.setState(this.state);
    }

    updatePassword(event) {
        event.preventDefault();

        var submissionData = { 
            currentPassword : this.state.currentPassword,
            newPassword : this.state.newPassword,
            confirmPassword : this.state.confirmPassword
        };


        $("#loading").show();

        $.ajax({
            type: 'POST',
            url: '/protected/update_password',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                profileCard.clearPasswords();

            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Submitting Account Information", errorThrown);
            },
            async: true
        });

        console.log("submitting account!");
    }

    changeCurrentPassword(event) {
        this.state.currentPassword = event.target.value;
        this.setState(this.state);
    }

    changeNewPassword(event) {
        this.state.newPassword = event.target.value;
        this.setState(this.state);
    }

    changeConfirmPassword(event) {
        this.state.confirmPassword = event.target.value;
        this.setState(this.state);
    }

    render() {
        const hidden = this.props.hidden;
        const bgStyle = {opacity : 0.8};
        const fgStyle = {opacity : 1.0};

        let formGroupStyle = {
            marginBottom: '8px'
        };

        let formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'right'
        };

        let validationMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        let currentPassword = this.state.currentPassword;
        let newPassword = this.state.newPassword;
        let confirmPassword = this.state.confirmPassword;

        //passwords must have valid text
        //new must equal validate
        //current must not equal new/validate

        let re = /[\@\#\$\%\^\&\*\(\)\_\+\!\/\\\.,a-zA-Z0-9 ]*/;

        let passwordValidationMessage = "";
        let passwordValidationHidden = true;

        if (currentPassword.length == 0) {
            passwordValidationMessage = "Please enter your current password.";
            passwordValidationHidden = false;

        } else if (re.test(currentPassword) && currentPassword.length < 10) {
            passwordValidationMessage = "Current password was not valid. Must be minimum 10 characters long and consist of letters, numbers, spaces and any of the following special characters: @#$%^&*()_+!/\\";
            passwordValidationHidden = false;

        } else if (newPassword.length == 0) {
            passwordValidationMessage = "Please enter a new password.";
            passwordValidationHidden = false;

        } else if (re.test(newPassword) && newPassword.length < 10) {
            passwordValidationMessage = "New password was not valid. Must be minimum 10 characters long and consist of letters, numbers, spaces and any of the following special characters: @#$%^&*()_+!/\\";
            passwordValidationHidden = false;

        } else if (confirmPassword.length == 0) {
            passwordValidationMessage = "Please re-enter your new password.";
            passwordValidationHidden = false;

        } else if (re.test(confirmPassword) && confirmPassword.length < 10) {
            passwordValidationMessage = "Confirmation password was not valid. Must be minimum 10 characters long and consist of letters, numbers, spaces and any of the following special characters: @#$%^&*()_+!/\\";
            passwordValidationHidden = false;

        } else if (currentPassword == newPassword) {
            passwordValidationMessage = "The new password cannot be the same as your current password.";
            passwordValidationHidden = false;

        } else if (newPassword != confirmPassword) {
            passwordValidationMessage = "The new password and confirmation password must be the same.";
            passwordValidationHidden = false;
        }

        let updatePasswordDisabled = !passwordValidationHidden;

        console.log("rendering with password validation message: '" + passwordValidationMessage + "' and password validation visible: " + passwordValidationHidden);

        return (
            <div className="card-body" hidden={hidden}>

                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header" style={fgStyle}>
                        Update Password
                    </h5>

                    <div className="card-body" style={fgStyle}>

                        <form onSubmit={(event) => this.updatePassword(event)} >

                            <div className="form-group" style={formGroupStyle}>

                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                        <label htmlFor="createPassword" style={labelStyle}>Current Password</label>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <input type="password" className="form-control" id="currentPassword" placeholder="Password (required)" required={true} onChange={(event) => {this.changeCurrentPassword(event)}} value={this.state.currentPassword}/>
                                    </div>
                                </div>
                            </div>

                            <div className="d-flex">
                                <div className="p-2" style={formHeaderStyle}>
                                    <label htmlFor="createPassword" style={labelStyle}>New Password</label>
                                </div>
                                <div className="p-2 flex-fill">
                                    <input type="password" className="form-control" id="newPassword" placeholder="Password (required)" required={true} onChange={(event) => {this.changeNewPassword(event)}} value={this.state.newPassword}/>
                                </div>
                                <div className="p-2 flex-fill">
                                    <input type="password" className="form-control" id="confirmPassword" placeholder="Confirm password (required)" required={true} onChange={(event) => {this.changeConfirmPassword(event)}} value={this.state.confirmPassword}/>
                                </div>
                            </div>

                            <div className="d-flex">
                                <div className="p-2" style={formHeaderStyle}>
                                </div>
                                <div className="p-2 flex-fill">
                                    <span style={validationMessageStyle} hidden={passwordValidationHidden}>{passwordValidationMessage}</span>
                                </div>
                                <div className="p-2">
                                    <button type="submit" className="btn btn-primary float-right" disabled={updatePasswordDisabled}>Update Password</button>
                                </div>
                            </div>


                        </form>

                    </div>
                </div>
            </div>
        );
    }
}

var profileCard = ReactDOM.render(
    <UpdatePasswordCard />,
    document.querySelector('#password-card')
);
