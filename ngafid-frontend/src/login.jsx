import 'bootstrap';

import React, {createRef} from "react";
import { createRoot } from 'react-dom/client';
import { showErrorModal } from './error_modal';
import 'bootstrap';             // ok (brings all components)
import { Modal } from 'bootstrap'; // add this import
import $ from 'jquery';
window.$ = window.jQuery = $;

import { homeNavbar } from './home_navbar';



const loginModalRef = createRef();


class LoginModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            valid: {
                email: false,
                emailEmpty: true,
                passwordEmpty: true,
                loginMessage: false
            },
            errorMessage: ""
        };
    }

    handleKeyDown(e) {
        e.preventDefault();
        if (e.keyCode == 13) {
            $("#loginSubmitButton").click();
        }
    }

    show() {
        this.validateEmail();
        this.validatePassword();
        getLoginModal().show();
    }

    submitLogin() {
        let valid = true;
        for (const property in this.state.valid) {
            console.log(property);

            if (property == false) {
                valid = false;
                break;
            }
        }

        //Got validation error, exit
        if (!valid)
            return;

        console.log("Submitting login!");

        // $("#login-modal").modal('hide');
        hideLoginModal();
        $("#loading").show();

        const submissionData = {
            email: $("#loginEmail").val(),
            password: $("#loginPassword").val()
        };



        $.ajax({
            type: 'POST',
            url: '/api/auth/login',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {

                $("#loginPassword").val("");

                if (response.loggedOut) {
                    console.log("User was logged out...");
                    this.setState(prevState => ({
                        valid: {
                            ...prevState.valid,
                            errorMessage: true
                        },
                        errorMessage: response.message
                    }));
                    homeNavbar.logOut();
                    return false;
                }

                if (response.errorTitle) {
                    console.log("Displaying error modal!");
                    hideLoginModal();
                    showErrorModal(response.errorTitle, response.errorMessage);
                    return false;
                }

                if (response.waiting || response.denied) {
                    //redirect to the waiting page
                    window.location.replace("/protected/waiting");
                } else {
                    //redirect to the base page (which will redirect to either summary, waiting or the page before login)
                    window.location.replace("/");
                }

            },
            error: (jqXHR, textStatus, errorThrown) => {
                hideLoginModal();
                showErrorModal("Error Submitting Account Information", errorThrown);
            },
            complete: () => {
                $("#loading").hide();
            }
        });

    }

    validateEmail() {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape

        const email = $("#loginEmail").val();
        const newValid = {
            ...this.state.valid,
            email: re.test(String(email).toLowerCase()),
            emailEmpty: email.length == 0,
            errorMessage: false
        };

        this.setState({
            valid: newValid,
            errorMessage: ""
        });
    }

    validatePassword() {

        const password = $("#loginPassword").val();

        //reset the error message from the server as the user has modified the email/password
        const newValid = {
            ...this.state.valid,
            passwordEmpty: (password.length == 0),
            errorMessage: false
        };

        this.setState({
            valid: newValid,
            errorMessage: ""
        });
    }

    render() {

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

        let validationMessage = "";
        let validationHidden = true;


        if (this.state.valid.errorMessage) {
            //show error messages from the server first
            validationMessage = this.state.errorMessage;
            validationHidden = false;
        } else if (this.state.valid.emailEmpty) {
            validationMessage = "Please enter your email.";
            validationHidden = false;

        } else if (!this.state.valid.email) {
            validationMessage = "Email was not valid.";
            validationHidden = false;

        } else if (this.state.valid.passwordEmpty) {
            validationMessage = "Please enter a password.";
            validationHidden = false;
        }

        const submitDisabled = !validationHidden;

        console.log(`rendering login modal with validation message: '${  validationMessage  }' and validation visible: ${  validationHidden}`);


        return (
            <form onSubmit={e => { e.preventDefault(); this.submitLogin(); }}>
                <div className='modal-content'>

                    <div className='modal-header'>
                        <h5 id='login-modal-title' className='modal-title'>Login</h5>
                        <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                            <span aria-hidden='true'>&times;</span>
                        </button>
                    </div>

                    <div id='login-modal-body' className='modal-body'>

                        <div className="form-group" style={formGroupStyle}>
                            <div className="d-flex">
                                <div className="p-2" style={formHeaderStyle}>
                                    <label htmlFor="loginEmail" style={labelStyle}>Email Address</label>
                                </div>
                                <div className="p-2 flex-fill">
                                    <input type="email" className="form-control" id="loginEmail"
                                           aria-describedby="emailHelp" placeholder="Enter email (required)" required={true}
                                           onChange={() => {
                                               this.validateEmail();
                                           }}/>
                                </div>
                            </div>
                        </div>

                        <div className="form-group" style={formGroupStyle}>
                            <div className="d-flex">
                                <div className="p-2" style={formHeaderStyle}>
                                    <label htmlFor="loginPassword" style={labelStyle}>Password</label>
                                </div>
                                <div className="p-2 flex-fill">
                                    <input type="password" className="form-control" id="loginPassword"
                                           placeholder="Password (required)" required={true} onKeyUp={(e) => {
                                        this.handleKeyDown(e);
                                    }} onChange={() => {
                                        this.validatePassword();
                                    }}/>
                                </div>
                            </div>
                        </div>

                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                            </div>
                            <div className="p-2 flex-fill">
                                <span style={validationMessageStyle} hidden={validationHidden}>{validationMessage}</span>
                            </div>
                        </div>

                        <div className="d-flex justify-content-end">
                            <div className="p-2">
                                <a href="/forgot_password">Forgot Password?</a>
                            </div>
                        </div>

                    </div>

                    <div className='modal-footer'>
                        <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>Close</button>
                        <button
                            id='loginSubmitButton'
                            type='submit'
                            className='btn btn-primary'
                            disabled={submitDisabled}
                        >
                            Submit
                        </button>
                    </div>

                </div>
            </form>
        );
    }
}

// const container = document.querySelector("#login-modal-content");
// const root = createRoot(container);
// root.render(<LoginModal ref={loginModalRef}/>);

function getLoginModal() {

    const modalElement = document.getElementById('login-modal');
    if (!modalElement)
        throw new Error('Login Modal not found!');

    return Modal.getOrCreateInstance(modalElement);

}

export function showLoginModal() {

    if (loginModalRef.current)
        getLoginModal().show();

}

export function hideLoginModal() {

    if (loginModalRef.current)
        getLoginModal().hide();

}