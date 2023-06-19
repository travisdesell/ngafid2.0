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
            submitted : false
        };
        this.submit = this.submit.bind(this);
    }

    validateEmail() {
        let re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        let email = $("#email").val();
        console.log(email);
        this.state.validEmail = re.test(String(email).toLowerCase());
        this.state.emailEmpty = email.length == 0;
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

                console.log(response == "Verification Link Sent.")
                if (response) {
                    forgotPasswordObj.state.submitted = true;
                    forgotPasswordObj.state.validEmail = true;
                    forgotPasswordObj.state.verificationLinkSent = true;
                    console.log(forgotPasswordObj.state);
                    window.location.replace("/reset_password");

                }
                else {
                    console.log("False");
                    this.state.validEmail = false;
                }

            },
            async : true
        });
        this.setState(forgotPasswordObj);

    }
    render() {
        const hidden = this.props.hidden;
        const bgStyle = {opacity : 0.8};
        const fgStyle = {opacity : 1.0};

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

        let submitDisabled = !validationHidden;

        if (!this.state.validEmail) {
            validationMessage = "Email was not valid.";
            validationHidden = false;
        }
        if (this.state.emailEmpty) {
            validationMessage = "Please enter your email.";
            validationHidden = false;
        }
        let obj = this;
        return (
            <div className="container">
                <div className="row justify-content-center">
                    <div className="col-md-6">
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