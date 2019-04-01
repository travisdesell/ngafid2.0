import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { loginModal } from "./login.js";
import { navbar } from "./home_navbar.js";

class ResetPasswordCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            emailAddress : "",
            passphrase : "",
            newPassword : "",
            confirmPassword : ""
        };
    }

    clearPasswords() {
        this.state = {
            emailAddress : "",
            passphrase : "",
            newPassword : "",
            confirmPassword : ""
        };

        this.setState(this.state);
    }

    resetPassword(event) {
        event.preventDefault();

        var submissionData = { 
            emailAddress : this.state.emailAddress,
            passphrase : this.state.passphrase,
            newPassword : this.state.newPassword,
            confirmPassword : this.state.confirmPassword
        };


        $("#loading").show();

        $.ajax({
            type: 'POST',
            url: '/reset_password',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    resetPasswordCard.clearPasswords();
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                if (response.waiting || response.denied) {
                    //redirect to the waiting page
                    window.location.replace("/protected/waiting");
                } else {
                    //redirect to the dashboard page
                    window.location.replace("/protected/dashboard");
                }

            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Submitting Account Information", errorThrown);
            },
            async: true
        });

        console.log("submitting account!");
    }

    changeEmailAddress(event) {
        this.state.emailAddress = event.target.value;
        this.setState(this.state);
    }

    changePassphrase(event) {
        this.state.passphrase = event.target.value;
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
        const bgStyle = {background : "rgba(248,259,250,0.8)"};
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

        let emailAddress = this.state.emailAddress;
        let passphrase = this.state.passphrase;
        let newPassword = this.state.newPassword;
        let confirmPassword = this.state.confirmPassword;

        //passwords must have valid text
        //new must equal validate
        //current must not equal new/validate

        let re = /[\@\#\$\%\^\&\*\(\)\_\+\!\/\\\.,a-zA-Z0-9 ]*/;
        let emailRe = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;


        let passwordValidationMessage = "";
        let passwordValidationHidden = true;

        if (emailAddress.length == 0) {
            passwordValidationMessage = "Please enter your email address.";
            passwordValidationHidden = false;

        } else if (!emailRe.test(emailAddress)) {
            passwordValidationMessage = "Email address was not valid.";
            passwordValidationHidden = false;

        } else if (passphrase.length == 0) {
            passwordValidationMessage = "Please enter the passphrase that was emailed to you.";
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

        } else if (newPassword != confirmPassword) {
            passwordValidationMessage = "The new password and confirmation password must be the same.";
            passwordValidationHidden = false;
        }

        let resetPasswordDisabled = !passwordValidationHidden;

        console.log("rendering with password validation message: '" + passwordValidationMessage + "' and password validation visible: " + passwordValidationHidden);

        return (
            <div className="card-body" hidden={hidden}>

                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header" style={fgStyle}>
                        Reset Password
                    </h5>

                    <div className="card-body" style={fgStyle}>

                        <form onSubmit={(event) => this.resetPassword(event)} >

                            <div className="form-group" style={formGroupStyle}>
                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                        <label htmlFor="resetEmailAddress" style={labelStyle}>Email Address</label>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <input type="text" className="form-control" id="resetEmailAddress" aria-describedby="emailAddressHelp" placeholder="Enter Email Address" onChange={(event) => this.changeEmailAddress(event)}/>
                                    </div>
                                </div>
                            </div>

                            <div className="form-group" style={formGroupStyle}>
                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                        <label htmlFor="resetPassphrase" style={labelStyle}>Reset Passphrase</label>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <input type="text" className="form-control" id="resetPassphrase" aria-describedby="passphraseHelp" placeholder="Enter Reset Passphrase" onChange={(event) => this.changePassphrase(event)}/>
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
                                    <button type="submit" className="btn btn-primary float-right" disabled={resetPasswordDisabled}>Reset Password</button>
                                </div>
                            </div>

                        </form>

                    </div>
                </div>
            </div>
        );
    }
}

var resetPasswordCard = ReactDOM.render(
    <ResetPasswordCard />,
    document.querySelector('#reset-password-card')
);
