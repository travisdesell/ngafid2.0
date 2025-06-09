import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';
import $ from "jquery";
import { showErrorModal } from "./error_modal";

window.jQuery = $;
window.$ = $;


import './index.css';


class ForgotPassword extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            validEmail: false,
            emailEmpty: true,
            registeredEmail: false,
        };
        this.submit = this.submit.bind(this);
    }

    validateEmail() {

        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape
        const email = $("#email").val();
        console.log(email);

        this.setState({
            validEmail: re.test(String(email).toLowerCase()),
            emailEmpty: email.length === 0,
            error: false
        });
    }

    submit() {

        $("#loading").show();
        const submissionData = {
            email: $("#email").val(),
        };
        console.log(this.state);

        $.ajax({
            type: 'POST',
            url: '/api/auth/forgot-password',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {

                $("#loading").hide();
                if (response.registeredEmail) {

                    this.setState({
                        validEmail: true,
                        registeredEmail: true,
                        error: false
                    });
                    return true;

                } else {

                    this.setState({
                        registeredEmail: false,
                        validEmail: false,
                        error: true
                    });
                    return false;

                }

            },
            error: (jqXHR, textStatus, errorThrown) => {
                $("#loading").hide();
                showErrorModal("Error Submitting Account Information", errorThrown);
            },
        });
        this.setState(this);
    }

    render() {

        let validationMessage = "";
        let validationHidden = true;

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
        if (this.state.error) {
            validationMessage = "Please enter a registered email address.";
            validationHidden = false;
        } else if (this.state.emailEmpty) {
            validationMessage = "Please enter your email.";
            validationHidden = false;
        } else if (!this.state.validEmail) {
            validationMessage = "Email was not valid.";
            validationHidden = false;
        }

        const submitDisabled = !validationHidden;

        const forgotPasswordCard = (
            <div className="card">
                <h5 className="card-header">Forgot Password</h5>
                <div className="card-body">
                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="email" style={labelStyle}>Email Address</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="email" className="form-control" id="email" aria-describedby="emailHelp"
                                       placeholder="Enter email (required)" required={true} onChange={() => {
                                    this.validateEmail();
                                }}/>
                            </div>
                        </div>
                    </div>
                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}></div>
                        <div className="p-2 flex-fill">
                            <span style={validationMessageStyle} hidden={validationHidden}>{validationMessage}</span>
                        </div>
                    </div>
                    <div className='modal-footer'>
                        <button id='forgotPasswordSubmitBtn' type='submit' className='btn btn-primary' onClick={() => {
                            this.submit();
                        }} disabled={submitDisabled}>Submit
                        </button>
                    </div>
                </div>
            </div>
        );

        const resetPaaswordCard = (
            <div className="card mt-4">
                <h5 className="card-header">Reset Password</h5>
                <div className="card-body">
                    <p className="card-text">
                        A password reset link has been sent to your registered email address. Please click on it to
                        reset your password.
                    </p>
                </div>
            </div>
        );

        return (
            <div className="container my-auto pb-24">
                <div className="row justify-content-center">
                    <div className="col-md-6">
                        {
                            !this.state.registeredEmail ? forgotPasswordCard : resetPaaswordCard
                        }

                    </div>
                </div>
            </div>
        );
    }
}

const container = document.querySelector("#forgot_password-card");
const root = createRoot(container);
root.render(<ForgotPassword />);