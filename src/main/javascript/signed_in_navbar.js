import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";

class NavLink extends React.Component {
    render() {
        const name = this.props.name;
        const hidden = this.props.hidden;
        const active = this.props.active;

        let onClick = this.props.onClick;
        let href = this.props.href;

        if (typeof href == 'undefined') href = "#!";
        //make unclick an empty function if its not defined
        if (typeof onClick == 'undefined') onClick = function(){};

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
        //make unclick an empty function if its not defined
        if (typeof onClick == 'undefined') onClick = function(){};

        console.log("rendering dropdownlink '" + name + "'");

        return (
            <a className="dropdown-item" href={href} hidden={hidden} onClick={() => onClick()}>{name}</a>
        );
    }
}



class SignedInNavbar extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            waitingUserCount : this.props.waitingUserCount,
            fleetManager : this.props.fleetManager ,
            unconfirmedTailsCount : this.props.unconfirmedTailsCount,
            modifyTailsAccess : this.props.modifyTailsAccess
        };

        navbar = this;
    }

    setFlightsCard(flightsCard) {
        this.state.flightsCard = flightsCard;
    }

    toggleMap() {
        this.state.flightsCard.toggleMap();
    }

    togglePlot() {
        this.state.flightsCard.togglePlot();
    }

    toggleFilter() {
        this.state.flightsCard.toggleFilter();
    }

    mapSelectChanged() {
        console.log("map select changed!");

        var select = document.getElementById('mapLayerSelect');
        var style = select.value;

        this.state.flightsCard.mapSelectChanged(style);
    }

    setWaiting(waitingUserCount) {
        console.log("setting waiting to: " + waitingUserCount + "!");
        this.state.waitingUserCount = waitingUserCount;
        console.log("waiting now: " + this.state.waitingUserCount);
        this.setState(this.state);
    }


    incrementWaiting() {
        console.log("decrementing waiting!");
        this.state.waitingUserCount++;
        console.log("waiting now: " + this.state.waitingUserCount);
        this.setState(this.state);
    }

    decrementWaiting() {
        console.log("decrementing waiting!");
        this.state.waitingUserCount--;
        console.log("waiting now: " + this.state.waitingUserCount);
        this.setState(this.state);
    }

    attemptLogIn() {
        console.log("showing login modal!");
        loginModal.show();
    }

    attemptLogOut() {
        console.log("attempting log out!");

        var submissionData = {};

        $.ajax({
            type: 'POST',
            url: '../logout',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                //processing the response will update the navbar
                //to the logged out state

                //redirect to the dashboard page
                window.location.replace("/logout_success");
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Logging Out", errorThrown);
            },
            async: true
        });

    }

    render() {
        let waitingUsersString = "";
        if (this.state.waitingUserCount > 0) waitingUsersString = " (" + this.state.waitingUserCount + ")";
        let manageHidden = !this.state.fleetManager;

        let tailsHidden = !this.state.modifyTailsAccess;

        let unconfirmedTailsString = "";
        if (this.state.unconfirmedTailsCount > 0) unconfirmedTailsString = " (" + this.state.unconfirmedTailsCount + ")";

        let accountNotifications = " (" + (this.state.waitingUserCount + this.state.unconfirmedTailsCount) + ")";

        let filterButtonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary active";
        let plotButtonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        let mapButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";

        let navbarBgColor = "rgba(188,203,218,0.8)";
        let selectBgColor = "rgba(203,210,218,0.8)";
        //const buttonStyle = { backgroundColor : selectBgColor };
        const buttonStyle = { };

        return (
            <nav id='ngafid-navbar' className="navbar navbar-expand-lg navbar-light" style={{zIndex: "999", opacity: "1.0", backgroundColor:navbarBgColor}}>
                <a className="navbar-brand" href="../">NGAFID</a>
                <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="navbarNavDropdown">
                    <ul className="navbar-nav mr-auto">

                        <ul className="navbar-nav mr-auto" hidden={this.props.plotMapHidden}>
                            <button id="filter-toggle-button" className={filterButtonClasses} data-toggle="button" title="Toggle the filter." aria-pressed="false" style={buttonStyle} onClick={() => this.toggleFilter()}>
                                <i className="fa fa-search p-1"></i>
                            </button>

                            <button id="plot-toggle-button" className={plotButtonClasses} data-toggle="button" title="Toggle the plot." aria-pressed="false" style={buttonStyle} onClick={() => this.togglePlot()}>
                                <i className="fa fa-area-chart p-1"></i>
                            </button>

                            <div className="input-group m-0">
                                <div className="input-group-prepend">
                                    <button id="map-toggle-button" className={mapButtonClasses} data-toggle="button" title="Toggle the map." aria-pressed="false" style={buttonStyle} onClick={() => this.toggleMap()}>
                                        <i className="fa fa-map-o p-1"></i>
                                    </button>
                                </div>
                                <select className="custom-select" defaultValue="Road" id="mapLayerSelect" style={{backgroundColor:selectBgColor}} onChange={() => this.mapSelectChanged()}>
                                    <option value="Aerial">Aerial</option>
                                    <option value="AerialWithLabels">Aerial with labels</option>
                                    <option value="Road">Road (static)</option>
                                    <option value="RoadOnDemand">Road (dynamic)</option>
                                    <option value="SectionalCharts">Sectional Charts</option>
                                    <option value="TerminalAreaCharts">Terminal Area Charts</option>
                                    <option value="IFREnrouteLowCharts">IFR Enroute Low Charts</option>
                                    <option value="IFREnrouteHighCharts">IFR Enroute High Charts</option>
                                </select>

                            </div>
                        </ul>

                    </ul>

                    <ul className="navbar-nav">
                        <NavLink name={"Dashboard"} href="/protected/dashboard"/>
                        <NavLink name={"Flights"} href="/protected/flights"/>
                        <NavLink name={"Imports"} href="/protected/imports"/>
                        <NavLink name={"Uploads"} href="/protected/uploads"/>

                        <li className="nav-item dropdown">
                            <a className="nav-link dropdown-toggle" href="#!" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                {"Account" + accountNotifications}
                            </a>
                            <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink">
                                <DropdownLink name={"Manage Fleet" + waitingUsersString} hidden={manageHidden} href="/protected/manage_fleet"/>
                                <DropdownLink name={"Manage Tail Numbers" + unconfirmedTailsString} hidden={tailsHidden} href="/protected/system_ids"/>
                                <div className="dropdown-divider" hidden={manageHidden}></div>
                                <DropdownLink name={"Update Password"} hidden={false} href="/protected/update_password"/>
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
    <SignedInNavbar waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);

export { navbar };
