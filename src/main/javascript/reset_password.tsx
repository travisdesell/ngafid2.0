/* ResetPasswordPage.tsx */


/* Imports */
import 'bootstrap';
import React from 'react';
import { createRoot } from 'react-dom/client';
import { errorModal } from './error_modal';
import $ from 'jquery';


/* Declarations */
//...


/* Components */
interface ResetPasswordCardProps {
    hidden?: boolean;
}

interface ResetPasswordCardState {
    emailAddress: string;
    passphrase: string;
    newPassword: string;
    confirmPassword: string;
}

class ResetPasswordCard extends React.Component<ResetPasswordCardProps, ResetPasswordCardState> {

    constructor(props: ResetPasswordCardProps) {

        super(props);

        //Fetch resetPhrase from the URL
        const urlParams = new URLSearchParams(window.location.search);
        const passPhrase = urlParams.get('resetPhrase') ?? '';

        this.state = {
            emailAddress: '',
            passphrase: passPhrase,
            newPassword: '',
            confirmPassword: '',
        };
        
    }

    clearPasswords(): void {

        this.setState({
            emailAddress: '',
            newPassword: '',
            confirmPassword: '',
        });

    }

    resetPassword(event: React.FormEvent<HTMLFormElement>): void {

        event.preventDefault();

        const { emailAddress, passphrase, newPassword, confirmPassword } = this.state;

        const submissionData = {
            emailAddress,
            passphrase,
            newPassword,
            confirmPassword,
        };

        $('#loading').show();

        $.ajax({
            type: 'POST',
            url: '/reset_password',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response: any) => {

                console.log('Received Response:', response);
                $('#loading').hide();

                //Got an error, display Error Modal
                if (response.errorTitle) {
                    console.log('displaying error modal!');
                    this.clearPasswords();
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                //Account denied or waiting for approval, redirect to waiting page
                if (response.waiting || response.denied)
                    window.location.replace('/protected/waiting');

                //Otherwise, redirect to welcome page
                else
                    window.location.replace('/protected/welcome');
                
            },
            error: (jqXHR: any, textStatus: string, errorThrown: string) => {
                errorModal.show('Error Submitting Account Information', errorThrown);
            }
        });

        console.log('Submitting reset password request!');
    }

    changeEmailAddress(event: React.ChangeEvent<HTMLInputElement>): void {
        this.setState({ emailAddress: event.target.value });
    }

    changeNewPassword(event: React.ChangeEvent<HTMLInputElement>): void {
        this.setState({ newPassword: event.target.value });
    }

    changeConfirmPassword(event: React.ChangeEvent<HTMLInputElement>): void {
        this.setState({ confirmPassword: event.target.value });
    }

    render() {
        const { hidden } = this.props;
        const { emailAddress, newPassword, confirmPassword } = this.state;

        const bgStyle = { background: 'rgba(248,249,250,0.8)' }; // corrected the values
        const fgStyle = { opacity: 1.0 };

        const formGroupStyle = { marginBottom: '8px' };
        const formHeaderStyle = { width: '150px', flex: '0 0 150px' };
        const labelStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'right' as const
        };
        const validationMessageStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'left' as const,
            color: 'red',
        };

        /*
            Passwords must meet the following criteria:

            - Be at least <PASSWORD_LENGTH_MIN> characters long
            - Have valid text
            - New must equal Validate
            - Current must not equal New/Validate
        */

        const PASSWORD_LENGTH_MIN = 10;

        const re = /[\@\#\$\%\^\&\*\(\)\_\+\!\/\\\.,a-zA-Z0-9 ]*/;

        let passwordValidationMessage = "";
        let passwordValidationHidden = true;

        /*
            Failure cases now defined as lambda functions;
            returns the validation message if a failure occurs.
        */
        const passwordValidationFailures = [
        
            //Failure: New Password is empty
            () => {
                if (newPassword.length === 0) {
                    passwordValidationMessage = 'Please enter a new password.';
                    return false;
                }
                return true;
            },

            //Failure: New Password is not valid
            () => {
                if (re.test(newPassword) && newPassword.length < PASSWORD_LENGTH_MIN) {
                    passwordValidationMessage =
                        `New password was not valid. Must be minimum ${PASSWORD_LENGTH_MIN} characters long and consist of letters, numbers, spaces and any of the following special characters: @#$%^&*()_+!\\/'`;
                    return false;
                }
                return true;
            },

            //Failure: Confirm Password is empty
            () => {
                if (confirmPassword.length === 0) {
                    passwordValidationMessage = 'Please re-enter your new password.';
                    return false;
                }
                return true;
            },

            //Failure: Confirm Password is not valid
            () => {
                if (re.test(confirmPassword) && confirmPassword.length < PASSWORD_LENGTH_MIN) {
                    passwordValidationMessage =
                        `Confirmation password was not valid. Must be minimum ${PASSWORD_LENGTH_MIN} characters long and consist of letters, numbers, spaces and any of the following special characters: @#$%^&*()_+!\\/'`;
                    return false;
                }
                return true;
            },

            //Failure: New Password does not match Confirm Password
            () => {
                if (newPassword !== confirmPassword) {
                    passwordValidationMessage = 'The new password and confirmation password must be the same.';
                    return false;
                }
                return true;
            }

        ];

        /*
            Iterate over the password validation failure cases.

            If any are, update the password validation message
            and set the password validation hidden to false.
        */
        passwordValidationHidden = passwordValidationFailures.every((failure) => failure());

        const resetPasswordDisabled = !passwordValidationHidden;

        console.log(
            `rendering with password validation message: '${passwordValidationMessage}' and password validation visible: ${passwordValidationHidden}`
        );

        return (
            <div className="card-body" style={{backgroundColor : "transparent"}} hidden={hidden}>
                <div className="card mb-1" style={bgStyle}>
                    <h5 className="card-header" style={fgStyle}>
                        Reset Password
                    </h5>
                    <div className="card-body" style={fgStyle}>
                        <form onSubmit={(event) => this.resetPassword(event)}>
                            <div className="form-group" style={formGroupStyle}>
                                <div className="d-flex">
                                    <div className="p-2" style={formHeaderStyle}>
                                        <label htmlFor="resetEmailAddress" style={labelStyle}>
                                            Email Address
                                        </label>
                                    </div>
                                    <div className="p-2 flex-fill">
                                        <input
                                            type="text"
                                            className="form-control"
                                            id="resetEmailAddress"
                                            placeholder="Enter Email Address"
                                            onChange={(event) => this.changeEmailAddress(event)}
                                        />
                                    </div>
                                </div>
                            </div>

                            <div className="d-flex">
                                <div className="p-2" style={formHeaderStyle}>
                                    <label htmlFor="createPassword" style={labelStyle}>
                                        New Password
                                    </label>
                                </div>
                                <div className="p-2 flex-fill">
                                    <input
                                        type="password"
                                        className="form-control"
                                        id="newPassword"
                                        placeholder="Password (required)"
                                        required
                                        onChange={(event) => this.changeNewPassword(event)}
                                        value={newPassword}
                                    />
                                </div>
                                <div className="p-2 flex-fill">
                                    <input
                                        type="password"
                                        className="form-control"
                                        id="confirmPassword"
                                        placeholder="Confirm password (required)"
                                        required
                                        onChange={(event) => this.changeConfirmPassword(event)}
                                        value={confirmPassword}
                                    />
                                </div>
                            </div>

                            <div className="d-flex">
                                <div className="p-2" style={formHeaderStyle} />
                                <div className="p-2 flex-fill">
                                    <span style={validationMessageStyle} hidden={passwordValidationHidden}>
                                        {passwordValidationMessage}
                                    </span>
                                </div>
                                <div className="p-2">
                                    <button type="submit" className="btn btn-primary float-right" disabled={resetPasswordDisabled}>
                                        Reset Password
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

/* Render the component to the DOM */
const container = document.querySelector('#reset-password-card');
const root = createRoot(container!);
root.render(<ResetPasswordCard/>);