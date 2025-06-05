import 'bootstrap';
import React from "react";
import ReactDOM from "react-dom";

import {errorModal} from "./error_modal.js";

class ResetPasswordCard extends React.Component {
    
    constructor(props) {
        super(props);
        const urlParams = new URLSearchParams(window.location.search);
        const passPhrase = urlParams.get("resetPhrase");
        this.state = {
            emailAddress: "",
            passphrase: passPhrase,
            newPassword: "",
            confirmPassword: ""
        };
    }

    clearPasswords() {
        this.setState({
            emailAddress: "",
            newPassword: "",
            confirmPassword: ""
        });
    }

    resetPassword(event) {
        event.preventDefault();

        const submissionData = {
            emailAddress: this.state.emailAddress,
            passphrase: this.state.passphrase,
            newPassword: this.state.newPassword,
            confirmPassword: this.state.confirmPassword
        };


        $("#loading").show();

        $.ajax({
            type: 'POST',
            url: '/api/auth/reset-password',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {
                
                console.log("Received response: ", response);

                $("#loading").hide();

                if (response.errorTitle) {
                    this.clearPasswords();
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                if (response.waiting || response.denied) {
                    //redirect to the waiting page
                    window.location.replace("/protected/waiting");
                } else {
                    //redirect to the welcome page
                    window.location.replace("/protected/welcome");
                }

            },
            error: (jqXHR, textStatus, errorThrown) => {
                errorModal.show("Error Submitting Account Information", errorThrown);
            },
        });

        console.log("submitting account!");
    }

    changeEmailAddress(event) {
        this.setState({ emailAddress: event.target.value });
    }


    changeNewPassword(event) {
        this.setState({ newPassword: event.target.value });
    }

    changeConfirmPassword(event) {
        this.setState({ confirmPassword: event.target.value });
    }

    render() {
        const hidden = this.props.hidden;
        const bgStyle = {background: "rgba(248,259,250,0.8)"};
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

        const emailAddress = this.state.emailAddress;
        const newPassword = this.state.newPassword;
        const confirmPassword = this.state.confirmPassword;

        //passwords must have valid text
        //new must equal validate
        //current must not equal new/validate

        const re = /[\@\#\$\%\^\&\*\(\)\_\+\!\/\\\.,a-zA-Z0-9 ]*/; //eslint-disable-line no-useless-escape
        const emailRe = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape


        let passwordValidationMessage = "";
        let passwordValidationHidden = true;

        if (emailAddress.length == 0) {
            passwordValidationMessage = "Please enter your email address.";
            passwordValidationHidden = false;

        } else if (!emailRe.test(emailAddress)) {
            passwordValidationMessage = "Email address was not valid.";
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

        const resetPasswordDisabled = !passwordValidationHidden;

        console.log(`rendering with password validation message: '${  passwordValidationMessage  }' and password validation visible: ${  passwordValidationHidden}`);

        return (
            <div className="card-body" hidden={hidden}>

                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header" style={fgStyle}>
                        Reset Password
                    </h5>

                    <div className="card-body" style={fgStyle}>

                        <form onSubmit={(event) => this.resetPassword(event)}>

                            <div className="form-group" style={formGroupStyle}>
                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                        <label htmlFor="resetEmailAddress" style={labelStyle}>Email Address</label>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <input type="text" className="form-control" id="resetEmailAddress"
                                               aria-describedby="emailAddressHelp" placeholder="Enter Email Address"
                                               onChange={(event) => this.changeEmailAddress(event)}/>
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
                                            disabled={resetPasswordDisabled}>Reset Password
                                    </button>
                                </div>
                            </div>

                        </form>

                    </div>
                </div>
            </div>
        );
    }
}

const container = document.querySelector("#reset-password-card");
const root = ReactDOM.createRoot(container);
root.render(<ResetPasswordCard hidden={false}/>);