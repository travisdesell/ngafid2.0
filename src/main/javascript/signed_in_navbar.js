import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Overlay from 'react-bootstrap/Overlay';
import { errorModal } from "./error_modal.js";

var activePage = "";

class NavLink extends React.Component {
    render() {
        const name = this.props.name;
        const hidden = this.props.hidden;
        let active = this.props.active;

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

        this.infoTarget = React.createRef();
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

                //redirect to the welcome page
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
        if (this.props.waitingUserCount > 0) waitingUsersString = " (" + this.props.waitingUserCount + ")";
        let manageHidden = !this.props.fleetManager;

        let tailsHidden = !this.props.modifyTailsAccess;

        let unconfirmedTailsString = "";
        if (this.props.unconfirmedTailsCount > 0) unconfirmedTailsString = " (" + this.props.unconfirmedTailsCount + ")";

        let accountNotifications = " (" + (this.props.waitingUserCount + this.props.unconfirmedTailsCount) + ")";

        let filterButtonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";

        if (this.props.filterVisible) filterButtonClasses += " active";

        let plotButtonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        let mapButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";

        let navbarBgColor = "rgba(188,203,218,0.8)";
        let selectBgColor = "rgba(203,210,218,0.8)";
        //const buttonStyle = { backgroundColor : selectBgColor };
        const buttonStyle = { };
        //const [show, setShow] = React.useState(false);
        var uploadsButton = "";
        var importsButton = "";
            if (airSyncEnabled) {
                uploadsButton = (
                    <li className="nav-item dropdown">
                        <a className={"nav-link dropdown-toggle" + (this.props.activePage === "uploads" ? " active" : "")} href="#!" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            {"Uploads"}
                        </a>
                        <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink">
                            <DropdownLink name={"Manual Uploads"} href="/protected/uploads"/>
                            <DropdownLink name={"AirSync Uploads"} href="/protected/airsync_uploads"/>
                        </div>
                    </li>

                );

                importsButton = (
                    <li className="nav-item dropdown">
                        <a className={"nav-link dropdown-toggle" + (this.props.activePage === "imports" ? " active" : "")} href="#!" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            {"Imports"}
                        </a>
                        <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink">
                            <DropdownLink name={"Manual Imports"} href="/protected/imports"/>
                            <DropdownLink name={"AirSync Imports"} href="/protected/airsync_imports"/>
                        </div>
                    </li>

                );
            } else {
                importsButton = (
                    <NavLink name={"Imports"} active={this.props.activePage === "imports"} href="/protected/imports"/>
                )
                uploadsButton = (
                    <NavLink name={"Uploads"} active={this.props.activePage === "uploads"} href="/protected/uploads"/>
                );
            }

        console.log("[signed in navbar] this.props.filterVisible: " + this.props.filterVisible);

        let eventsActive = this.props.activePage === "trends" || this.props.activePage === "event statistics" || this.props.activePage === "create event" || this.props.activePage === "update event" || this.props.activePage === "severities";
        let analysisActive = this.props.activePage === "ttf";

        return (
            <nav id='ngafid-navbar' className="navbar navbar-expand-lg navbar-light" style={{zIndex: "999", opacity: "1.0", backgroundColor:navbarBgColor}}>
                <a className="navbar-brand" href="../">NGAFID</a>
                <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarNavDropdown" aria-controls="navbarNavDropdown" aria-expanded="false" aria-label="Toggle navigation">
                    <span className="navbar-toggler-icon"></span>
                </button>

                <div className="collapse navbar-collapse" id="navbarNavDropdown">
                    <ul className="navbar-nav mr-auto">

                        <ul className="navbar-nav mr-auto" hidden={this.props.plotMapHidden}>
                            { 
                                //only display the filter icon on the navbar if it's being used
                                this.props.filterVisible ? (
                                    <button id="filter-toggle-button" className={filterButtonClasses} data-toggle="button" title="Toggle the filter." aria-pressed={this.props.filterSelected} style={buttonStyle} onClick={() => this.props.toggleFilter()}>
                                        <i className="fa fa-search p-1"></i>
                                    </button>
                                ) : ( "" )
                            }

                            <button id="plot-toggle-button" className={plotButtonClasses} data-toggle="button" title="Toggle the plot." aria-pressed="false" style={buttonStyle} onClick={() => this.props.togglePlot()}>
                                <i className="fa fa-area-chart p-1"></i>
                            </button>

                            <div className="input-group m-0">
                                <div className="input-group-prepend">
                                    <button id="map-toggle-button" className={mapButtonClasses} data-toggle="button" title="Toggle the map." aria-pressed="false" style={buttonStyle} onClick={() => this.props.toggleMap()}>
                                        <i className="fa fa-map-o p-1"></i>
                                    </button>
                                </div>
                                <select className="custom-select" id="mapLayerSelect" ref={this.infoTarget} style={{backgroundColor:selectBgColor}} 
                                    value={this.props.mapStyle}
                                    onChange={event => this.props.mapSelectChanged(event.target.value)}>

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
                        <NavLink name={"Home"} active={this.props.activePage === "welcome"} href="/protected/welcome"/>

                        {aggregateView ?
                            <li className="nav-item dropdown">
                                <a className={"nav-link dropdown-toggle" + (eventsActive ? " active" : "")} href="#!" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                    Aggregate View
                                </a>
                                <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink" >
                                    <DropdownLink name={"Aggregate Dashboard"} hidden={false} href="/protected/aggregate"/>
                                    <DropdownLink name={"Aggregate Trends"} hidden={false} href="/protected/aggregate_trends"/>
                                </div>
                            </li> : ""
                        }

                        <li className="nav-item dropdown">
                            <a className={"nav-link dropdown-toggle" + (eventsActive ? " active" : "")} href="#!" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                Events{eventsActive ? (<span className="sr-only">(current)</span>) : ""}
                            </a>
                            <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink" >
                                <DropdownLink name={"Trends"} hidden={false} href="/protected/trends"/>
                                <DropdownLink name={"Severity"} hidden={false} href="/protected/severities"/>
                                <DropdownLink name={"Statistics"} hidden={false} href="/protected/event_statistics"/>
                                <DropdownLink name={"Definitions"} hidden={false} href="/protected/event_definitions"/>

                                {admin
                                    ? <div className="dropdown-divider"></div>
                                    : ""
                                }
                                {admin
                                    ? <DropdownLink name={"Manage Events"} hidden={false} href="/protected/manage_events"/>
                                    : ""
                                }
                            </div>
                        </li>
                        
                        <li className="nav-item dropdown">
                            <a className={"nav-link dropdown-toggle" + (analysisActive ? " active" : "")} href="#!" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                Analysis{analysisActive ? (<span className="sr-only">(current)</span>) : ""}
                            </a>
                            <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink" >
                                <DropdownLink name={"Turn to Final Tool"} hidden={false} active={this.props.activePage === "ttf"} href="/protected/ttf"/>
                            </div>
                        </li>

                        <NavLink name={"Flights"} active={this.props.activePage === "flights"} href="/protected/flights"/>
                        {importsButton}
                        {uploadsButton}

                        <li className="nav-item dropdown">
                            <a className={"nav-link dropdown-toggle" + (this.props.activePage === "account" ? " active" : "")} href="#!" id="navbarDropdownMenuLink" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                {"Account" + accountNotifications}{this.props.activePage === "account" ? (<span className="sr-only">(current)</span>) : ""}
                            </a>
                            <div className="dropdown-menu dropdown-menu-right text-right" aria-labelledby="navbarDropdownMenuLink">
                                <DropdownLink name={"Manage Fleet" + waitingUsersString} hidden={manageHidden} href="/protected/manage_fleet"/>
                                <DropdownLink name={"Manage Tail Numbers" + unconfirmedTailsString} hidden={tailsHidden} href="/protected/system_ids"/>
                                <div className="dropdown-divider" hidden={manageHidden}></div>
                                <DropdownLink name={"Update Password"} hidden={false} href="/protected/update_password"/>
                                <DropdownLink name={"Update Profile"} hidden={false} href="/protected/update_profile"/>
                                <div className="dropdown-divider"></div>
                                <DropdownLink name={"My Preferences"} hidden={false} href="/protected/preferences"/>
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

export default SignedInNavbar;
