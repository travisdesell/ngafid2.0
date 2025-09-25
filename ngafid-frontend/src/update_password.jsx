import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';

import {showErrorModal} from "./error_modal";
import SignedInNavbar from "./signed_in_navbar";


class UpdatePasswordPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            currentPassword: "",
            newPassword: "",
            confirmPassword: ""
        };
    }

    clearPasswords() {
        this.setState({
            currentPassword: "",
            newPassword: "",
            confirmPassword: ""
        });
    }

    updatePassword(event) {
        
        event.preventDefault();

        const submissionData = {
            currentPassword: this.state.currentPassword,
            newPassword: this.state.newPassword,
            confirmPassword: this.state.confirmPassword
        };


        $("#loading").show();

        $.ajax({
            type: 'PATCH',
            url: '/api/auth/change-password',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {

                console.log("Received response: ", response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    showErrorModal(response.errorTitle, response.errorMessage);
                    return false;
                }

            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Submitting Account Information", errorThrown);
            },
        });

        console.log("Submitting account!");
    }

    changeCurrentPassword(event) {
        this.setState({ currentPassword: event.target.value });
    }

    changeNewPassword(event) {
        this.setState({ newPassword: event.target.value });
    }

    changeConfirmPassword(event) {
        this.setState({ confirmPassword: event.target.value });
    }

    render() {

        const fgStyle = {opacity: 1.0};

        const formGroupStyle = {
            marginBottom: '8px'
        };

        const formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        const labelStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'right'
        };

        const validationMessageStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        const currentPassword = this.state.currentPassword;
        const newPassword = this.state.newPassword;
        const confirmPassword = this.state.confirmPassword;

        //passwords must have valid text
        //new must equal validate
        //current must not equal new/validate

        const re = /[\@\#\$\%\^\&\*\(\)\_\+\!\/\\\.,a-zA-Z0-9 ]*/; //eslint-disable-line no-useless-escape

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

        const updatePasswordDisabled = !passwordValidationHidden;

        console.log(`rendering with password validation message: '${  passwordValidationMessage  }' and password validation visible: ${  passwordValidationHidden}`);

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage="account" waitingUserCount={waitingUserCount} fleetManager={fleetManager}
                                    unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess}
                                    plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY: "auto", flex: "1 1 auto"}}>
                    <div className="card mb-1 m-2">
                        <h5 className="card-header" style={fgStyle}>
                            Update Password
                        </h5>

                        <div className="card-body" style={fgStyle}>

                            <form onSubmit={(event) => this.updatePassword(event)}>

                                <div className="form-group" style={formGroupStyle}>

                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createPassword" style={labelStyle}>Current Password</label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input type="password" className="form-control" id="currentPassword"
                                                   placeholder="Password (required)" required={true}
                                                   onChange={(event) => {
                                                       this.changeCurrentPassword(event);
                                                   }} value={this.state.currentPassword}/>
                                        </div>
                                    </div>
                                </div>

                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                        <label htmlFor="createPassword" style={labelStyle}>New Password</label>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <input type="password" className="form-control" id="newPassword"
                                               placeholder="Password (required)" required={true} onChange={(event) => {
                                            this.changeNewPassword(event);
                                        }} value={this.state.newPassword}/>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <input type="password" className="form-control" id="confirmPassword"
                                               placeholder="Confirm password (required)" required={true}
                                               onChange={(event) => {
                                                   this.changeConfirmPassword(event);
                                               }} value={this.state.confirmPassword}/>
                                    </div>
                                </div>

                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <span style={validationMessageStyle}
                                              hidden={passwordValidationHidden}>{passwordValidationMessage}</span>
                                    </div>
                                    <div className="p-2">
                                        <button type="submit" className="btn btn-primary float-right"
                                                disabled={updatePasswordDisabled}>Update Password
                                        </button>
                                    </div>
                                </div>


                            </form>

                        </div>
                    </div>
                </div>

            </div>
        );
    }
}

const container = document.querySelector("#password-page");
const root = createRoot(container);
root.render(<UpdatePasswordPage/>);