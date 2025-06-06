'use strict';

class NavLink extends React.Component {
    render() {
        const name = this.props.name;
        const hidden = this.props.hidden;
        const active = this.props.active;

        let onClick = this.props.onClick;
        let href = this.props.href;

        if (typeof href == 'undefined') href = "#!";
        //make unclick an empty function if its not defined
        if (typeof onClick == 'undefined') onClick = function () {
        };


        //console.log("rendering navlink '" + name + "', active: " + active);

        const classNames = active ? "nav-item active" : "nav-item";
        const isCurrent = active ? (<span className="sr-only">(current)</span>) : "";

        return (
            <li className={classNames}>
                <a className="nav-link" href={href} hidden={hidden} onClick={() => onClick()}>{name} {isCurrent}</a>
            </li>
        );
    }
}

class DropdownLink extends React.Component {
    render() {
        const name = this.props.name;
        const hidden = this.props.hidden;

        let onClick = this.props.onClick;
        let href = this.props.href;
        if (typeof href == 'undefined') href = "#!";
        if (typeof onClick == 'undefined') onClick = function () {
        };

        console.log("rendering dropdownlink '" + name + "'");

        return (
            <a className="dropdown-item" href={href} hidden={hidden} onClick={() => onClick()}>{name}</a>
        );
    }
}


class UserNavbar extends React.Component {
    attemptLogIn() {
        console.log("showing login modal!");
        loginModal.show();
    }

    attemptLogOut() {
        console.log("attempting log out!");

        var submissionData = {};

        $.ajax({
            type: 'POST',
            url: '/api/auth/logout',
            data: submissionData,
            dataType: 'json',
            success: function (response) {
                //processing the response will update the navbar
                //to the logged out state

                //logout so they can log back in
                window.location.replace("/logout_success");
            },
            error: function (jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Logging Out", errorThrown);
            },
            async: true
        });

    }

    render() {
        let navbarBgColor = "rgba(188,203,218,0.8)";

        return (
            <nav id='ngafid-navbar' className="navbar navbar-expand-lg navbar-light"
                 style={{zIndex: "999", opacity: "1.0", backgroundColor: navbarBgColor}}>
                <a className="navbar-brand" href="../../src/main">NGAFID</a>
                <button className="navbar-toggler" type="button" data-bs-toggle="collapse"
                        data-bs-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false"
                        aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="navbar-collapse" id="navbarNavDropdown">
                    <ul className="navbar-nav mr-auto">
                    </ul>

                    <ul className="navbar-nav">
                        <li className="nav-item dropdown">
                            <a className="nav-link dropdown-toggle" href="#!" id="navbarDropdownMenuLink" role="button"
                               data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">Account</a>
                            <div className="dropdown-menu dropdown-menu-right text-right"
                                 aria-labelledby="navbarDropdownMenuLink">
                                <DropdownLink name={"Update Password"} hidden={false}
                                              href="/protected/update_password"/>
                                <DropdownLink name={"Update Profile"} hidden={false} href="/protected/update_profile"/>
                                <div className="dropdown-divider"></div>
                                <DropdownLink name={"Log Out"} hidden={false} onClick={() => this.attemptLogOut()}/>
                            </div>
                        </li>

                    </ul>
                </div>
            </nav>
        );
    }
}

var navbar = ReactDOM.render(
    <UserNavbar/>,
    document.querySelector('#navbar')
);

