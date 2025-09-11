import 'bootstrap';

import React, {createRef} from "react";
import { createRoot } from 'react-dom/client';
import { showErrorModal } from './error_modal.js';
import $ from 'jquery';

import { homeNavbar } from './home_navbar.js';

window.jQuery = $;
window.$ = $;


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
            errorMessage: "",
            requires2FA: false,
            totpCode: "",
            setup2FA: false,
            storedEmail: "",
            storedPassword: "",
            isSubmitting: false,
            setupStep: 'authenticator',
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
        $("#login-modal").modal('show');
    }

    submitLogin() {
        // Prevent multiple simultaneous submissions
        if (this.state.isSubmitting) {
            console.log("Login already in progress, ignoring duplicate submission");
            return;
        }

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


        // Set submitting state to prevent duplicates
        this.setState({ isSubmitting: true });

        $("#login-modal").modal('hide');

        $("#loading").show();

        const submissionData = {
            email: this.state.requires2FA ? this.state.storedEmail : $("#loginEmail").val(),
            password: this.state.requires2FA ? this.state.storedPassword : $("#loginPassword").val()
        };

        //2FA code is required...
        if (this.state.requires2FA) {

            const code = (this.state.totpCode || "").trim();

            //...Using backup code
            if (this.state.setupStep === 'backup')
                submissionData.backupCode = code;

            //...Using authenticator app
            else
                submissionData.totpCode = code;

        }

        const submissionDataLogSafe = { ...submissionData, password: '****' };
        console.log("Submitting login data:", submissionDataLogSafe);



        $.ajax({
            type: 'POST',
            url: '/api/auth/login',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {

                console.log("Login response:", response);

                if (response.loggedOut) {
                    console.log("User was logged out...");
                    this.setState(prevState => ({
                        valid: {
                            ...prevState.valid,
                            errorMessage: true
                        },
                        errorMessage: response.message
                    }));
                    $("#login-modal").modal('show');
                    return false;
                }

                if (response.message === "2FA_CODE_REQUIRED") {
                    console.log("2FA code required");
                    this.setState({ 
                        requires2FA: true,
                        storedEmail: $("#loginEmail").val(),
                        storedPassword: $("#loginPassword").val()
                    }, ()=> {
                        $("#2fa-modal-content").show();
                        $("#login-modal").modal('show');
                    });
                    $("#loading").hide();
                    return;
                }

                if (response.message === "2FA_SETUP_REQUIRED") {
                    console.log("2FA setup required");
                    this.setState({ setup2FA: true });
                    $("#loading").hide();
                    return;
                }

                // Clear password field for successful login or errors (but not for 2FA flows)
                $("#loginPassword").val("");

                if (response.errorTitle) {
                    console.log("Displaying error modal!");
                    hideLoginModal();
                    showErrorModal(response.errorTitle, response.errorMessage);
                    return false;
                }

                if (response.waiting || response.denied) {
                    //redirect to the waiting page
                    window.location.replace("/protected/waiting");
                    return;
                } else if (response.loggedIn) {
                    //redirect to the base page (which will redirect to either welcome, waiting or the page before login)
                    window.location.replace("/");
                    return;
                } else {
                    // Handle unexpected response
                    console.log("Unexpected response:", response);
                    this.setState(prevState => ({
                        valid: {
                            ...prevState.valid,
                            errorMessage: true
                        },
                        errorMessage: "Unexpected response from server"
                    }));
                    $("#loading").hide();
                    return;
                }

            },
            error: (jqXHR, textStatus, errorThrown) => {
                hideLoginModal();
                showErrorModal("Error Submitting Account Information", errorThrown);
            },
            complete: () => {
                $("#loading").hide();
                // Reset submitting state
                this.setState({ isSubmitting: false });
            }
        });

    }

    validateEmail() {
        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/; //eslint-disable-line no-useless-escape

        const email = $("#loginEmail").val();
        console.log("Validating Email: ", email);

        if (!email) {
            console.error("Email is null or undefined! Unable to validate.");
            return;
        }

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
        console.log("Validating Password...");

        if (!password) {
            console.error("Password is null or undefined! Unable to validate.");
            return;
        }

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
        if (this.state.requires2FA) {
            return this.render2FAInput();
        }
        
        if (this.state.setup2FA) {
            return this.render2FASetup();
        }

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

    render2FAInput() {

        console.log("Rendering 2FA input step...");

        const isBackupInput = (this.state.setupStep === 'backup');

        const toggleLinkMessage = isBackupInput ? "Use authenticator app instead" : "Use backup code instead";
        const toggleLink = (
            <small className="text-muted">
                <a
                    href="#"
                    onClick={(e) => {
                        e.preventDefault();
                        this.setState({
                            setupStep: isBackupInput ? 'authenticator' : 'backup',
                            totpCode: ""
                        });
                    }}
                >
                    {toggleLinkMessage}
                </a>
            </small>
        );

        const errorMessage = (
            this.state.valid.errorMessage && (
                <div className="alert alert-danger" role="alert">
                    {this.state.errorMessage}
                </div>
            )
        );

        const formInputLength = (isBackupInput ? 8 : 6);

        const prompt = (this.state.setupStep === 'backup')
            ? "Enter your 8-digit backup code:"
            : "Enter the 6-digit code from your authenticator app:";

        return (
            <div
                className='modal-content'
                id='2fa-modal-content'
            >

                {/* Modal Header */}
                <div className='modal-header'>

                    {/* Modal Title */}
                    <h5 className='modal-title'>Two-Factor Authentication</h5>

                    {/* Close Button */}
                    <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                {/* Modal Body */}
                <div className='modal-body'>

                    {/* Shield Icon */}
                    <div className="text-center mb-3 w-full">
                        <i className="fa fa-shield fa-3x text-primary"></i>
                    </div>

                    {/* Error Message */}
                    {errorMessage}

                    {/* Enter Code Prompt */}
                    <p className="text-center">
                        {prompt}
                    </p>

                    {/* Single-use Warning */}
                    {
                        (isBackupInput)
                        &&
                        <div className="font-bold text-xs my-2 text-center">
                            Backup codes are single-use!
                        </div>
                    }

                    {/* Code Input Field */}
                    <input
                        type="text"
                        className="form-control text-center"
                        placeholder={"0".repeat(formInputLength)}
                        maxLength={formInputLength}
                        value={this.state.totpCode}
                        onChange={(e) => this.setState({ totpCode: e.target.value })}
                        onKeyPress={(e) => {
                            if (e.key === 'Enter' && this.state.totpCode.length === formInputLength) {
                                this.submitLogin();
                            }
                        }}
                        style={{ fontSize: '18px', letterSpacing: '2px' }}
                        autoFocus
                    />

                    {/* Supported Apps */}
                    <small className="text-muted mt-2 d-block text-center">
                        Supported apps: Google Authenticator, Microsoft Authenticator, Authy, 1Password, etc.
                    </small>
                    
                    {/* Code Input Mode Toggle */}
                    <div className="flex items-center justify-center">
                        {toggleLink}
                    </div>
                </div>

                {/* Modal Footer */}
                <div className='modal-footer'>

                    {/* Cancel Button */}
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>
                        Cancel
                    </button>

                    {/* Verify Button */}
                    <button
                        className='btn btn-primary'
                        onClick={() => this.submitLogin()}
                        disabled={this.state.totpCode.length !== formInputLength}
                    >
                        Verify
                    </button>
                </div>
            </div>
        );
    }

    render2FASetup() {
        return (
            <div className='modal-content'>
                <div className='modal-header'>
                    <h5 className='modal-title'>Set Up Two-Factor Authentication</h5>
                    <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div className='modal-body'>
                    <div className="text-center mb-3">
                        <i className="fas fa-shield-alt fa-3x text-warning"></i>
                    </div>
                    <div className="alert alert-info">
                        <h6><i className="fas fa-info-circle"></i> Two-Factor Authentication Required</h6>
                        <p className="mb-0">Your account requires two-factor authentication for enhanced security. Please set it up now to continue.</p>
                    </div>
                    <p className="text-center">You&apos;ll be redirected to the security settings page where you can:</p>
                    <ul className="list-unstyled">
                        <li><i className="fas fa-check text-success"></i> Scan a QR code with your authenticator app</li>
                        <li><i className="fas fa-check text-success"></i> Verify the setup with a 6-digit code</li>
                        <li><i className="fas fa-check text-success"></i> Save backup codes for emergency access</li>
                    </ul>
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>Cancel</button>
                    <button className='btn btn-primary' onClick={() => this.initiate2FASetup()}>
                        <i className="fas fa-shield-alt"></i> Set Up 2FA
                    </button>
                </div>
            </div>
        );
    }

    initiate2FASetup() {
        // Redirect to the 2FA settings page for setup
        window.location.href = "/two-factor-settings";
    }
}

const container = document.querySelector("#login-modal-content");
const root = createRoot(container);
root.render(<LoginModal ref={loginModalRef}/>);

export function showLoginModal() {

    if (loginModalRef.current)
        loginModalRef.current.show();

}

export function hideLoginModal() {

    if (loginModalRef.current)
        $("#login-modal").modal('hide');

}