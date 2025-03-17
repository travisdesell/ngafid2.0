// UpdatePasswordPage.tsx


/* Imports */
import 'bootstrap';
import React from 'react';
import ReactDOM from 'react-dom';
import { createRoot } from 'react-dom/client';
import { errorModal } from './error_modal';
import SignedInNavbar from './signed_in_navbar';
import $ from 'jquery';


/* Declarations */
declare var waitingUserCount: any;
declare var fleetManager: any;
declare var unconfirmedTailsCount: any;
declare var modifyTailsAccess: any;
declare var plotMapHidden: any;
declare var profileCard: any;


/* Components */
interface UpdatePasswordPageProps {
    hidden?: boolean;
}

interface UpdatePasswordPageState {
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
}

class UpdatePasswordPage extends React.Component<UpdatePasswordPageProps, UpdatePasswordPageState> {

    constructor(props: UpdatePasswordPageProps) {

        super(props);
        this.state = {
            currentPassword: "",
            newPassword: "",
            confirmPassword: "",
        };

    }

    clearPasswords(): void {

        this.setState({
            currentPassword: "",
            newPassword: "",
            confirmPassword: ""
        });

    }

    updatePassword(event: React.FormEvent<HTMLFormElement>): void {

        event.preventDefault();

        const submissionData = {
            currentPassword: this.state.currentPassword,
            newPassword: this.state.newPassword,
            confirmPassword: this.state.confirmPassword
        };

        const thisRef = this;

        $('#loading').show();

        $.ajax({
            type: 'POST',
            url: '/protected/update_password',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response: any) => {

                console.log('Received Response:', response);
                $('#loading').hide();

                //Got an error, display Error Modal
                if (response.errorTitle) {
                    console.log('displaying error modal!');
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                //Clear all of the password fields
                thisRef.clearPasswords();

            },
            error: (jqXHR: any, textStatus: string, errorThrown: string) => {
                errorModal.show('Error Submitting Account Information', errorThrown);
            }
        });

        console.log("Submitted Password Update!");

    }

    changeCurrentPassword(event: React.ChangeEvent<HTMLInputElement>): void {
        this.setState({ currentPassword: event.target.value });
    }

    changeNewPassword(event: React.ChangeEvent<HTMLInputElement>): void {
        this.setState({ newPassword: event.target.value });
    }

    changeConfirmPassword(event: React.ChangeEvent<HTMLInputElement>): void {
        this.setState({ confirmPassword: event.target.value });
    }

    render() {
        
        const { hidden } = this.props;
        const fgStyle = { opacity: 1.0 };

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
            textAlign: 'right' as const
        };

        const validationMessageStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'left' as const,
            color: 'red'
        };

        const { currentPassword, newPassword, confirmPassword } = this.state;

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
            
            //Failure: Current Password is empty
            () => {
                if (currentPassword.length === 0) {
                    passwordValidationMessage = 'Please enter your current password.';
                    return false;
                }
                return true;
            },

            //Failure: Current Password is not valid
            () => {
                if (re.test(currentPassword) && currentPassword.length < PASSWORD_LENGTH_MIN) {
                    passwordValidationMessage =
                        `Current password was not valid. Must be minimum ${PASSWORD_LENGTH_MIN} characters long and consist of letters, numbers, spaces and any of the following special characters: @#$%^&*()_+!\\/'`;
                    return false;
                }
                return true;
            },

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

            //Failure: Current Password is the same as New Password
            () => {
                if (currentPassword === newPassword) {
                    passwordValidationMessage = 'The new password cannot be the same as your current password.';
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

        const updatePasswordDisabled = !passwordValidationHidden;

        console.log(
            `rendering with password validation message: '${passwordValidationMessage}' and password validation visible: ${passwordValidationHidden}`
        );

        return (
            <div style={{ overflowX: 'hidden', display: hidden ? 'none' : 'flex', flexDirection: 'column', height: '100vh' }}>
                <div style={{ flex: '0 0 auto' }}>
                    <SignedInNavbar
                        activePage="account"
                        waitingUserCount={waitingUserCount}
                        fleetManager={fleetManager}
                        unconfirmedTailsCount={unconfirmedTailsCount}
                        modifyTailsAccess={modifyTailsAccess}
                        plotMapHidden={plotMapHidden}
                    />
                </div>

                <div style={{ overflowY: 'auto', flex: '1 1 auto' }}>
                    <div className="card mb-1 m-2">
                        <h5 className="card-header" style={fgStyle}>
                            Update Password
                        </h5>

                        <div className="card-body" style={fgStyle}>
                            <form onSubmit={(event) => this.updatePassword(event)}>
                                <div className="form-group" style={formGroupStyle}>
                                    <div className="d-flex">
                                        <div className="p-2" style={formHeaderStyle}>
                                            <label htmlFor="createPassword" style={labelStyle}>
                                                Current Password
                                            </label>
                                        </div>
                                        <div className="p-2 flex-fill">
                                            <input
                                                type="password"
                                                className="form-control"
                                                id="currentPassword"
                                                placeholder="Password (required)"
                                                required
                                                onChange={(event) => this.changeCurrentPassword(event)}
                                                value={currentPassword}
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
                                        <button type="submit" className="btn btn-primary float-right" disabled={updatePasswordDisabled}>
                                            Update Password
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

/* Render the component to the DOM */
const container = document.querySelector('#password-page');
const root = createRoot(container!);
root.render(<UpdatePasswordPage />);