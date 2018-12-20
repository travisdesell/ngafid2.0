'use strict';

class NavLink extends React.Component {
    render() {
        const name = this.props.name;
        const active = this.props.active;
        const onClick = this.props.onClick;

        console.log("rendering navlink '" + name + "', active: " + active);

        const classNames = active ? "nav-item active" : "nav-item";
        const isCurrent = active ? (<span className="sr-only">(current)</span>) : "";

        return (
            <li className={classNames}>
                <a className="nav-link" href="javascript:void(0)" onClick={() => onClick()}>{name} {isCurrent}</a>
            </li>
        );
    }
}


var navbar = null;
class Navbar extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loggedIn : this.props.loggedIn,
            activeName : "Home"
        };

        navbar = this;
    }

    createAccount() {
        console.log("Creating account!");

        this.state = {
            loggedIn : false,
            activeName : "Create Account"
        };
        this.setState(this.state);

        console.log("changing card to: " + this.state.activeName);

        mainContent.changeCard(this.state.activeName);

    }

    waiting() {
        console.log("setting navbar state to waiting!");

        this.state.loggedIn = false;
        this.state.waiting = true;
        this.state.activeName =  "Awaiting Access";

        this.setState(this.state);

        mainContent.changeCard(this.state.activeName);
    }


    logOut() {
        console.log("logging out!");

        this.state.loggedIn = false;
        this.state.waiting = false;
        this.state.activeName =  "Home";

        this.setState(this.state);

        mainContent.changeCard(this.state.activeName);
    }

    attemptLogIn() {
        console.log("showing login modal!");
        $("#login-modal").modal('show');
    }

    logIn() {
        this.state.loggedIn = true;
        this.state.waiting = false;
        this.state.activeName =  "Welcome";

        this.setState(this.state);

        mainContent.changeCard(this.state.activeName);
    }

    isMapVisible() {
        return mainContent.state.mapVisible;
    }

    toggleMap() {
        mainContent.toggleMap();
    }

    togglePlot() {
        mainContent.togglePlot();
    }

    show(newCard) {
        console.log("changing card to: " + newCard);
        mainContent.changeCard(newCard);

        let homeActive = false;
        let flightsActive = false;
        let importsActive = false;
        let uploadsActive = false;

        if (newCard == "Home" || newCard == "Create Account") {
            homeActive = true;
            mainContent.hidePlot();
            mainContent.hideMap();
        }

        console.log("changing log in to: " + newCard);

        this.state = {
            loggedIn : this.state.loggedIn,
            activeName : newCard
        }

        this.setState(this.state);
     }

    renderLoggedIn() {
        let activeName = this.state.activeName;

        console.log("rendering logged in navbar, activeName: " + activeName);

        let buttonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        const buttonStyle = { };

        return (
            <div className="collapse navbar-collapse" id="navbarNavDropdown">
                <ul className="navbar-nav mr-auto">

                    <button id="map-toggle-button" className={buttonClasses} data-toggle="button" title="Toggle the map." aria-pressed="false" style={buttonStyle} onClick={() => this.toggleMap()}>
                        <i className="fa fa-globe p-1"></i>
                    </button>

                    <button id="plot-toggle-button" className={buttonClasses} data-toggle="button" title="Toggle the plot." aria-pressed="false" style={buttonStyle} onClick={() => this.togglePlot()}>
                        <i className="fa fa-area-chart p-1"></i>
                    </button>


                </ul>

                <ul className="navbar-nav">
                    <NavLink name={"Flights"} onClick={() => this.show("Flights")} active={activeName == "Flights"}/>
                    <NavLink name={"Imports"} onClick={() => this.show("Imports")} active={activeName == "Imports"}/>
                    <NavLink name={"Uploads"} onClick={() => this.show("Uploads")} active={activeName == "Uploads"}/>

                    <li className="nav-item dropdown">
                        <a className="nav-link dropdown-toggle" href="javascript:void(0)" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            Account
                        </a>
                        <div className="dropdown-menu" aria-labelledby="navbarDropdownMenuLink">
                            <a className="dropdown-item" href="javascript:void(0)">Profile</a>
                            <a className="dropdown-item" href="javascript:void(0)">Log Out</a>
                        </div>
                    </li>

                    <NavLink name={"Logout"} onClick={() => this.logOut()}/>
                </ul>
            </div>
        );
    }

    renderWaiting() {
        let activeName = this.state.activeName;

        return (
            <div className="collapse navbar-collapse" id="navbarNavDropdown">
                <ul className="navbar-nav mr-auto">
                </ul>

                <ul className="navbar-nav">
                    <NavLink name={"Logout"} onClick={() => this.logOut()}/>
                </ul>
            </div>
        );
    }

    renderLoggedOut() {
        let activeName = this.state.activeName;

        return (
            <div className="collapse navbar-collapse" id="navbarNavDropdown">
                <ul className="navbar-nav mr-auto">
                </ul>

                <ul className="navbar-nav">
                    <NavLink name={"Login"} onClick={() => this.attemptLogIn()} />
                    <NavLink name={"Create Account"} onClick={() => this.createAccount()} active={activeName == "Create Account"}/>
                </ul>
            </div>
        );
    }

    render() {
        const isLoggedIn = this.state.loggedIn;
        let navbar_content;

        if (this.state.waiting) {
            navbar_content = this.renderWaiting();

        } else if (isLoggedIn) {
            navbar_content = this.renderLoggedIn();
        } else {
            navbar_content = this.renderLoggedOut();
        }

        return (
            <nav id='ngafid-navbar' className="navbar navbar-expand-lg navbar-light bg-light">
                <a className="navbar-brand" href="javascript:void(0)" onClick={() => this.show("Home")}>NGAFID</a>
                <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"></span>
                </button>
                {navbar_content}
            </nav>
        );
    }
}

var navbar = ReactDOM.render(
    <Navbar loggedIn={false} />,
    document.querySelector('#navbar')
);

