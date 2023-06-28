import 'bootstrap';
import React, { Component, useState } from "react";
import ReactDOM from "react-dom";
import $ from "jquery";
import { navbar } from "./home_navbar.js";

class ForgotPassword extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            validEmail : false,
            emailEmpty : true,
            registeredEmail : false,
        };
        this.submit = this.submit.bind(this);
    }

    validateEmail() {
        let re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        let email = $("#email").val();
        console.log(email);
        this.state.validEmail = re.test(String(email).toLowerCase());
        this.state.emailEmpty = email.length == 0;
        this.state.error = false;
        this.setState(this.state);
    }

    submit() {
        var submissionData = {
            email : $("#email").val(),
        };
        console.log(this.state);
        let forgotPasswordObj = this;
        $.ajax({
            type: 'POST',
            url: './forgot_password',
            data : submissionData,
            dataType : 'json',
            success : function (response) {
                console.log(response)
                if (response.registeredEmail) {
                    forgotPasswordObj.state.validEmail = true;
                    forgotPasswordObj.state.registeredEmail = true;
                }
                else {
                    forgotPasswordObj.state.registeredEmail = false;
                    forgotPasswordObj.state.validEmail = false;
                    forgotPasswordObj.state.error = true;
                }
            },
            async : false
        });
        this.setState(forgotPasswordObj);
    }
    render() {

        let validationMessage = "";
        let validationHidden = true;

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


        if (this.state.error) {
            validationMessage = "Please enter a registered email address.";
            validationHidden = false;
        }
        else if (this.state.emailEmpty) {
            validationMessage = "Please enter your email.";
            validationHidden = false;
        }
        else if (!this.state.validEmail) {
            validationMessage = "Email was not valid.";
            validationHidden = false;
        }

        let submitDisabled = !validationHidden;

        let forgotPasswordCard = (
            <div className="card mt-4">
                <h5 className="card-header">Forgot Password</h5>
                <div className="card-body">
                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="email" style={labelStyle}>Email address</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="email" className="form-control" id="email" aria-describedby="emailHelp" placeholder="Enter email (required)" required={true} onChange={() => {this.validateEmail();}}/>
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
                        <button id='forgotPasswordSubmitBtn' type='submit' className='btn btn-primary' onClick={() => {this.submit();}} disabled={submitDisabled}>Submit</button>
                    </div>
                </div>
            </div>
        );
        let resetPaaswordCard = (
            <div className="card mt-4">
                <h5 className="card-header">Reset Password</h5>
                <div className="card-body">
                    <p className="card-text">
                        A password reset link has been sent to your registered email address. Please click on it to reset your password.
                    </p>
                </div>
            </div>
        )
        return (
            <div className="container">
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

var forgotPassword = ReactDOM.render(
    <ForgotPassword/>,
    document.querySelector('#forgot_password-card')
)