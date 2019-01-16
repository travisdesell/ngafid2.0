'use strict';

class NavLink extends React.Component {
    render() {
        const name = this.props.name;
        const hidden = this.props.hidden;
        const active = this.props.active;
        const onClick = this.props.onClick;

        console.log("rendering navlink '" + name + "', active: " + active);

        const classNames = active ? "nav-item active" : "nav-item";
        const isCurrent = active ? (<span className="sr-only">(current)</span>) : "";

        return (
            <li className={classNames}>
                <a className="nav-link" href="javascript:void(0)" hidden={hidden} onClick={() => onClick()}>{name} {isCurrent}</a>
            </li>
        );
    }
}

class DropdownLink extends React.Component {
    render() {
        const name = this.props.name;
        const hidden = this.props.hidden;
        const onClick = this.props.onClick;

        console.log("rendering dropdownlink '" + name + "'");


        return (
            <a className="dropdown-item" href="javascript:void(0)" hidden={hidden} onClick={() => onClick()}>{name}</a>
        );
    }
}




var navbar = null;
class Navbar extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            loggedIn : this.props.loggedIn,
            activeName : "Welcome"
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

    attemptLogOut() {
        console.log("attempting log out!");

        var submissionData = {};

        $.ajax({
            type: 'POST',
            url: './logout',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                //processing the response will update the navbar
                //to the logged out state
                if (!processResponse(response)) return;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                display_error_modal("Error Logging Out", errorThrown);
            },
            async: true
        });

    }

    logOut() {
        console.log("logging out!");

        this.state.loggedIn = false;
        this.state.waiting = false;
        this.state.activeName =  "Welcome";

        this.setState(this.state);

        mainContent.changeCard(this.state.activeName);
    }

    attemptLogIn() {
        console.log("showing login modal!");
        loginModal.show();
    }

    logIn(user) {
        this.state.loggedIn = true;
        this.state.waiting = false;
        this.state.activeName =  "Dashboard";

        let accessType = user.fleetAccess.accessType;
        user.waitingUsersCount = 0;

        if (accessType == "MANAGER") {
            console.log("initializing waiting count!");
            let waitingUsers = user.fleet.users;

            for (let i = 0; i < waitingUsers.length; i++) {
                if (waitingUsers[i].fleetAccess.accessType == "WAITING") {
                    user.waitingUsersCount++;
                }
            }
        }

        this.state.user = user;

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

        if (newCard == "Welcome" || newCard == "Create Account" || newCard == "Profile" || newCard == "Manage Fleet") {
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

    incrementWaiting() {
        console.log("decrementing waiting!");
        this.state.user.waitingUsersCount++;
        console.log("waiting now: " + this.state.user.waitingUsersCount);
        this.setState(this.state);
    }

    decrementWaiting() {
        console.log("decrementing waiting!");
        this.state.user.waitingUsersCount--;
        console.log("waiting now: " + this.state.user.waitingUsersCount);
        this.setState(this.state);
    }

    mapSelectChanged() {
        console.log("map select changed!");

        var select = document.getElementById('mapLayerSelect');
        var style = select.value;
        for (var i = 0, ii = layers.length; i < ii; ++i) {

            console.log("setting layer " + i + " to:" + (styles[i] === style));
            layers[i].setVisible(styles[i] === style);
        }
    }



    renderLoggedIn() {
        let activeName = this.state.activeName;

        console.log("rendering logged in navbar, activeName: " + activeName);

        let plotButtonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        let mapButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        const buttonStyle = { };

        let user = this.state.user;
        let accessType = user.fleetAccess.accessType;
        let manageHidden = accessType != "MANAGER";
        let uploadsHidden = accessType == "VIEW";
        let importsHidden = accessType == "VIEW";

        console.log("manageHidden: " + manageHidden + ", uploadsHidden: " + uploadsHidden + ", importsHidden: " + importsHidden);

        let waitingUsersCount = this.state.user.waitingUsersCount;

        let waitingUsersString = "";
        if (waitingUsersCount > 0) {
            waitingUsersString = " (" + waitingUsersCount + ")";
        }

        return (
            <div className="collapse navbar-collapse" id="navbarNavDropdown">
                <ul className="navbar-nav mr-auto">
                    <button id="plot-toggle-button" className={plotButtonClasses} data-toggle="button" title="Toggle the plot." aria-pressed="false" style={buttonStyle} onClick={() => this.togglePlot()}>
                        <i className="fa fa-area-chart p-1"></i>
                    </button>

                    <div className="input-group m-0">
                        <div className="input-group-prepend">
                            <button id="map-toggle-button" className={mapButtonClasses} data-toggle="button" title="Toggle the map." aria-pressed="false" style={buttonStyle} onClick={() => this.toggleMap()}>
                                <i className="fa fa-globe p-1"></i>
                            </button>
                        </div>
                        <select className="custom-select" defaultValue="Road" id="mapLayerSelect" onChange={() => this.mapSelectChanged()}>
                            <option value="Aerial">Aerial</option>
                            <option value="AerialWithLabels">Aerial with labels</option>
                            <option value="Road">Road (static)</option>
                            <option value="RoadOnDemand">Road (dynamic)</option>
                        </select>

                    </div>
                </ul>

                <ul className="navbar-nav">
                    <NavLink name={"Dashboard"} onClick={() => this.show("Dashboard")} active={activeName == "Dashboard"}/>
                    <NavLink name={"Flights"} onClick={() => this.show("Flights")} active={activeName == "Flights"}/>
                    <NavLink name={"Imports"} onClick={() => this.show("Imports")} hidden={importsHidden} active={activeName == "Imports"}/>
                    <NavLink name={"Uploads"} onClick={() => this.show("Uploads")} hidden={uploadsHidden} active={activeName == "Uploads"}/>

                    <li className="nav-item dropdown">
                        <a className="nav-link dropdown-toggle" href="javascript:void(0)" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            {"Account" + waitingUsersString}
                        </a>
                        <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink">
                            <DropdownLink name={"Profle"} hidden={false} onClick={() => this.show("Profile")}/>
                            <DropdownLink name={"Manage Fleet" + waitingUsersString} hidden={manageHidden} onClick={() => this.show("Manage Fleet")}/>
                            <div className="dropdown-divider"></div>
                            <DropdownLink name={"Log Out"} hidden={false} onClick={() => this.attemptLogOut()}/>
                        </div>
                    </li>

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
                    <NavLink name={"Logout"} onClick={() => this.attemptLogOut()}/>
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
            <nav id='ngafid-navbar' className="navbar navbar-expand-lg navbar-light bg-light" style={{zIndex : "999"}}>
                <a className="navbar-brand" href="javascript:void(0)" onClick={() => this.show("Welcome")}>NGAFID</a>
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

