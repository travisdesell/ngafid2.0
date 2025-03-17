/* ForgotPasswordPage.tsx */


/* Imports */
import 'bootstrap';
import React from 'react';
import { createRoot } from 'react-dom/client';
import { errorModal } from './error_modal.js';
import $ from 'jquery';


/* Declarations */
//...


/* Components */
interface ForgotPasswordProps { }

interface ForgotPasswordState {
    validEmail: boolean;
    emailEmpty: boolean;
    registeredEmail: boolean;
    error: boolean;
}

class ForgotPassword extends React.Component<ForgotPasswordProps, ForgotPasswordState> {

    constructor(props: ForgotPasswordProps) {

        super(props);
        this.state = {
            validEmail: false,
            emailEmpty: true,
            registeredEmail: false,
            error: false,
        };

        this.submit = this.submit.bind(this);
        this.validateEmail = this.validateEmail.bind(this);
        
    }

    validateEmail(): void {

        const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
        const email = $('#email').val() as string;

        this.setState({
            validEmail: re.test(String(email).toLowerCase()),
            emailEmpty: (email.length === 0),
            error: false,
        });

    }

    submit(): void {

        $('#loading').show();

        const email = $('#email').val() as string;

        console.log(`Sending password reset email to address: ${email}`);

        $.ajax({
            type: 'POST',
            url: './forgot_password',
            data: { email },
            dataType: 'json',
            async: true,
            success: (response: any) => {
                $('#loading').hide();

                //Email was valid and registered, show success message
                if (response.registeredEmail) {

                    this.setState({
                        validEmail: true,
                        registeredEmail: true,
                        error: false,
                        emailEmpty: false,
                    });

                    return true;
                
                //Email was invalid / unregisterd, show error message
                } else {
                
                    this.setState({
                        validEmail: false,
                        registeredEmail: false,
                        error: true,
                    });

                    return false;
                }
            },
            error: (jqXHR: any, textStatus: string, errorThrown: string) => {
                $('#loading').hide();
                errorModal.show('Error Submitting Account Information', errorThrown);
            },
        });
    }

    render() {

        const { validEmail, emailEmpty, registeredEmail, error } = this.state;

        let validationMessage = '';
        let validationHidden = true;

        if (error) {
            validationMessage = 'Please enter a registered email address.';
            validationHidden = false;
        } else if (emailEmpty) {
            validationMessage = 'Please enter your email.';
            validationHidden = false;
        } else if (!validEmail) {
            validationMessage = 'Email was not valid.';
            validationHidden = false;
        }

        const submitDisabled = !validationHidden;

        const formGroupStyle = {
            marginBottom: '8px',
        };

        const formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px',
        };

        const labelStyle = {
            padding: '7 0 7 0',
            margin: 0,
            display: 'block',
            textAlign: 'right' as const,
        };

        const validationMessageStyle = {
            padding: '7 0 7 0',
            margin: 0,
            display: 'block',
            textAlign: 'left' as const,
            color: 'red',
        };

        const forgotPasswordCard = (
            <div className="card mt-4">
                <h5 className="card-header">Forgot Password</h5>
                <div className="card-body">
                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="email" style={labelStyle}>
                                    Email Address
                                </label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input
                                    type="email"
                                    className="form-control"
                                    id="email"
                                    placeholder="Enter email (required)"
                                    required
                                    onChange={this.validateEmail}
                                />
                            </div>
                        </div>
                    </div>

                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle} />
                        <div className="p-2 flex-fill">
                            <span style={validationMessageStyle} hidden={validationHidden}>
                                {validationMessage}
                            </span>
                        </div>
                    </div>

                    <div className="modal-footer">
                        <button
                            id="forgotPasswordSubmitBtn"
                            type="submit"
                            className="btn btn-primary"
                            onClick={this.submit}
                            disabled={submitDisabled}
                        >
                            Submit
                        </button>
                    </div>
                </div>
            </div>
        );

        const resetPasswordCard = (
            <div className="card mt-4">
                <h5 className="card-header">Reset Password</h5>
                <div className="card-body">
                    <p className="card-text">
                        A password reset link has been sent to your registered email address. Please click on it to reset your
                        password.
                    </p>
                </div>
            </div>
        );

        return (
            <div className="container">
                <div className="row justify-content-center">
                    <div className="col-md-6">
                        {!registeredEmail ? forgotPasswordCard : resetPasswordCard}
                    </div>
                </div>
            </div>
        );
    }
}

/* Render the component to the DOM */
const container = document.querySelector('#forgot-password-card');
const root = createRoot(container!);
root.render(<ForgotPassword/>);