/*
 * returns false if response wasn't success
 * will display error modal on error,
 * redirect to awaiting access page if user
 * does not have access
 */
function processResponse(response) {
    console.log("processing response:");
    console.log(response);

    $("#loading").hide();

    if (response.errorTitle) {
        console.log("displaying error modal!");
        display_error_modal(response.errorTitle, response.errorMessage);
        return false;
    }

    if (response.loggedOut) {
        console.log("user was logged out");
        loginModal.state.valid.errorMessage = true;
        loginModal.state.errorMessage = response.message;
        loginModal.setState(loginModal.state);
        navbar.logOut();
        return false;
    }

    $("#loginPassword").val("");

    return true;
}

var loginModal = null;

class LoginModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            valid : {
                email : false,
                emailEmpty : true,
                passwordEmpty : true,
                loginMessage : false
            },
            errorMessage : ""
        };

        loginModal = this;
    }

    submitLogin() {
        let valid = true;
        for (let property in this.state.valid) {
            console.log(property);

            if (property == false) {
                valid = false;
                break;
            }
        }

        if (!valid) return;
        console.log("Submitting login!");

        $("#login-modal").modal('show');
        $("#loading").show();


        var submissionData = { 
            email : $("#loginEmail").val(),
            password : $("#loginPassword").val()
        };

        $.ajax({
            type: 'POST',
            url: './login',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                if (!processResponse(response)) return;

                mainCards['manage_fleet'].setUser(response.user);
                mainCards['profile'].setUser(response.user);

                //login was successful, we can hide the modal
                $("#login-modal").modal('hide');

                if (response.waiting || response.denied) {
                    //update the navbar to the logged in items
                    //and display the welcome page
                    navbar.waiting()
                    mainContent.changeCard("Awaiting Access");
                } else {
                    //update the navbar to the logged in items
                    //and display the welcome page
                    navbar.logIn(response.user)
                    mainContent.changeCard("Dashboard");
                }

            },
            error : function(jqXHR, textStatus, errorThrown) {
                $("#loading").hide();
                display_error_modal("Error Submitting Account Information", errorThrown);
            },
            async: true
        });

    }

    validateEmail() {
        let re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;

        let email = $("#loginEmail").val();
        this.state.valid.email = re.test(String(email).toLowerCase());
        this.state.valid.emailEmpty = email.length == 0;

        //reset the error message from the server as the user has modified the email/password
        this.state.valid.errorMessage = false;
        this.state.errorMessage = "";

        this.setState(this.state);
    }

    validatePassword() {
        let password = $("#loginPassword").val();
        this.state.valid.passwordEmpty = password.length == 0;

        //reset the error message from the server as the user has modified the email/password
        this.state.valid.errorMessage = false;
        this.state.errorMessage = "";

        this.setState(this.state);
    }

    render() {
        const hidden = this.props.hidden;

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

        let submitDisabled = !validationHidden;

        console.log("rendering login modal with validation message: '" + validationMessage + "' and validation visible: " + validationHidden);


        return (
            <div className='modal-content'>

                <div className='modal-header'>
                    <h5 id='login-modal-title' className='modal-title'>Login</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='login-modal-body' className='modal-body'>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="loginEmail" style={labelStyle}>Email address</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="email" className="form-control" id="loginEmail" aria-describedby="emailHelp" placeholder="Enter email (required)" required={true} onChange={() => {this.validateEmail();}}/>
                            </div>
                        </div>
                    </div>

                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="loginPassword" style={labelStyle}>Password</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="password" className="form-control" id="loginPassword" placeholder="Password (required)" required={true} onChange={() => {this.validatePassword();}} />
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

                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                    <button id='loginSubmitButton' type='submit' className='btn btn-primary' onClick={() => {this.submitLogin();}} disabled={submitDisabled}>Submit</button>
                </div>

            </div>
        );
    }
}

$(document).ready(function() {
    ReactDOM.render(
        <LoginModal />,
        document.querySelector("#login-modal-content")
    );
});
