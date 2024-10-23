import "bootstrap";

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import  TimeHeader from "./time_header.js";

import Plotly, { redraw } from "plotly.js";


airframes.unshift("All Airframes");
var index = airframes.indexOf("Garmin Flight Display");
if (index !== -1) airframes.splice(index, 1);




let targetValues = [
    "flightTime",
    "yearFlightTime",
    "monthFlightTime",
                           
    "numberFlights",
    "numberAircraft",
    "yearNumberFlights",
    "monthNumberFlights",
    "totalEvents",
    "yearEvents",
    "monthEvents",
    "numberFleets",
    "numberUsers",
    "uploads",
    "uploadsNotImported",
    "uploadsWithError",
    "flightsWithWarning",
    "flightsWithError"
];

const LOADING_STRING = "...";

const floatOptions = { 
    minimumFractionDigits: 2,
    maximumFractionDigits: 2 
};

const integerOptions = {};

function formatNumberAsync(value, formattingOptions) {
    if (value || typeof value === "number") {
        return Number(value).toLocaleString("en", formattingOptions);
    } else {
        return LOADING_STRING;
    }
}

function formatDurationAsync(seconds) {
    if (seconds || typeof seconds == "number") {
        return Number(seconds / (60 * 60)).toLocaleString("en", floatOptions);
    } else {
        return LOADING_STRING;
    }
}

function fetchStatistic(stat, aggregate, success) {
    let route;
    if (aggregate)
        route = "/protected/statistics/aggregate";
    else
        route = "/protected/statistics";

    $.ajax({
        type: "POST",
        url: `${route}/${stat}`,
        dataType: "json",
        success: success
    });
}

class Notifications extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            notifications: [
                {   count: waitingUserCount,
                    message: "User(s) awaiting Access Privileges", 
                    badgeType: "badge-info" 
                },
                {   count: unconfirmedTailsCount, 
                    message: "Tail Number(s) awaiting Confirmation",
                    badgeType: "badge-info"
                }
            ]
        };
    
        this.fetchStatistics();
        
    }

    fetchStatistics() {
        let notifications = this;
        for (let [i, notif] of this.state.notifications.entries()) {
            if (Object.hasOwn(notif, "name")) {
                fetchStatistic(notif.name, false, 
                    function (response) {
                        if (response.err_msg) {
                            errorModal.show(response.err_title, response.err_msg);
                            return;
                        }

                        notifications.state.notifications[i].count = response[notif.name];

                        notifications.setState(notifications.state);
                    }
                );
            }
        }
    }

    render() {
        return (
                <table>
                    <tbody>
                        {
                            this.state.notifications.map((info, index) => {
                                if (info.count == 0) {
                                    return;
                                } else {
                                    return (
                                        <tr key={index}>
                                            <td style={{textAlign:"right", paddingBottom:"6"}}>
                                                <span className={'badge ' + info.badgeType}>
                                                    <i className="fa fa-fw fa-bell" aria-hidden="true"/>
                                                    &nbsp;{Number(info.count).toLocaleString('en')}
                                                </span>
                                            </td>
                                            <td style={{paddingBottom:"6", color:"var(--c_text)"}}>&nbsp;{info.message}</td>
                                        </tr>
                                    );
                                }
                            })
                        }
                   </tbody>
                </table>
        );
    }
}

export default class SummaryPage extends React.Component {
    constructor(props) {
        super(props);

        var date = new Date();
        this.state = {
            airframe : "All Airframes",
            startYear : date.getFullYear(),
            startMonth : 1,
            endYear : date.getFullYear(),
            endMonth : date.getMonth() + 1,
            datesChanged : false,
            statistics: targetValues.reduce((o, key) => ({...o, [key]: ""}), {}),
            eventCounts: {},
            notifications: <Notifications />
        };
        
        this.dateChange();
        this.fetchStatistics();
        
    }
    
    componentDidMount() {
        this.displayPlots(this.state.airframe);
    }

    displayPlots(selectedAirframe) {
        
        var countData = [];
        var percentData = [];
    
        var fleetPercents = {
            name : this.props.aggregate ? "All Fleets" : "Your Fleet",
            type : "bar",
            orientation : "h",
            hoverinfo : "y+text",
            hovertext : [],
            y : [],
            x : [],
            flightsWithEventCounts : [],
            totalFlightsCounts : []
        };
   
        var ngafidPercents = {
            name : this.props.aggregate ? "All Fleets" : 'All Other Fleets',
            type : 'bar',
            orientation : 'h',
            hoverinfo : 'y+text',
            hovertext : [],
            y : [],
            x : [],
            flightsWithEventCounts : [],
            totalFlightsCounts : []
        };
    
        for (let [key, value] of Object.entries(this.state.eventCounts)) {

            //Airframe name is 'Garmin Flight Display', skip
            if (value.airframeName === "Garmin Flight Display")
                continue;

            //Current airframe name is neither the selected airframe name or 'All Airframes', skip
            if ((selectedAirframe !== value.airframeName) && (selectedAirframe !== "All Airframes"))
                continue;
    
            value.name = value.airframeName;
            value.y = value.names;
            value.type = "bar";
            value.orientation = "h";
    
            //don"t add airframes to the count plot that the fleet doesn"t have
            if (airframes.indexOf(value.airframeName) >= 0)
                countData.push(value);

            value.x = value.aggregateTotalEventsCounts;
            
            //  let percents = (this.props.aggregate ? fleetPercents : ngafidPercents);

            for (let i = 0; i < value.names.length; i++) {

                /*
                    Don't add airframes to the fleet percentage
                    plot that the fleet doesn't have.
                */

                var index = ngafidPercents.y.indexOf(value.names[i]);
                if (index !== -1) {
                    ngafidPercents.flightsWithEventCounts[index] += value.aggregateFlightsWithEventCounts[i];
                    ngafidPercents.totalFlightsCounts[index] += value.aggregateTotalFlightsCounts[i];
                } else {
                    let pos = ngafidPercents.y.length;
                    ngafidPercents.y.push(value.names[i]);
                    ngafidPercents.flightsWithEventCounts[pos] = value.aggregateFlightsWithEventCounts[i];
                    ngafidPercents.totalFlightsCounts[pos] = value.aggregateTotalFlightsCounts[i];
                }

                if (airframes.indexOf(value.airframeName) >= 0) {
                    var index = fleetPercents.y.indexOf(value.names[i]);
                    if (index !== -1) {
                        fleetPercents.flightsWithEventCounts[index] += value.flightsWithEventCounts[i];
                        fleetPercents.totalFlightsCounts[index] += value.totalFlightsCounts[i];
                    } else {
                        let pos = fleetPercents.y.length;
                        fleetPercents.y.push(value.names[i]);
                        fleetPercents.flightsWithEventCounts[pos] = value.flightsWithEventCounts[i];
                        fleetPercents.totalFlightsCounts[pos] = value.totalFlightsCounts[i];
                    }
                }

            }

        }
   
        
        //Push fleetPercents data ('Your Fleet')
        if (!this.props.aggregate)
            percentData.push(fleetPercents);


        //Push ngafidPercents data ('All Other Fleets')
        percentData.push(ngafidPercents);

    
        //for (let j = 0; j < percentData.length; j++) {
        for (let j = percentData.length-1 ; j >= 0 ; j--) {

            let value = percentData[j];
            value.x = [];
    
            for (let i = 0; i < value.flightsWithEventCounts.length; i++) {
            
                value.x.push( 100.0 * parseFloat(value.flightsWithEventCounts[i]) / parseFloat(value.totalFlightsCounts[i]) );

    
                var fixedText = "";
                if (value.x[i] > 0 && value.x[i] < 1) {
                    fixedText = value.x[i].toFixed(-Math.ceil(Math.log10(value.x[i])) + 2) + "%"
                } else {
                    fixedText = value.x[i].toFixed(2) + "%";
                }
                value.hovertext.push(fixedText);
    
            }
        }
    
        let styles = getComputedStyle(document.documentElement);
        let plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        let plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        let plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        var countLayout = {
            title : "Event Counts",
            barmode: "stack",
            //autosize: false,
            //width: 500,
            height: 750,
            margin: {
                l: 250,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            },
            legend: { 
                traceorder: "normal"
            },
            plot_bgcolor : "transparent",
            paper_bgcolor : plotBgColor,
            font : {
                color : plotTextColor
            },
            xaxis : {
                gridcolor : plotGridColor
            },
            yaxis : {
                gridcolor : plotGridColor
            }
        };

        var percentLayout = {
            title : "Percentage of Flights With Event",
            //autosize: false,
            //width: 500,
            height: 750,
            margin: {
                l: 250,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            },
            plot_bgcolor : "transparent",
            paper_bgcolor : plotBgColor,
            font : {
                color : plotTextColor
            },
            xaxis : {
                gridcolor : plotGridColor
            },
            yaxis : {
                gridcolor : plotGridColor
            }
        };
    
        console.log("Plot bg color: ", plotBgColor);
        console.log("Plot text color: ", plotTextColor);
    
        var config = {responsive: true}

        Plotly.newPlot("event-counts-plot", countData, countLayout, config);
        Plotly.newPlot("event-percents-plot", percentData, percentLayout, config);
    }
    updateStartYear(newStartYear) {
        this.setState({startYear : newStartYear, datesChanged : true});
    }

    updateStartMonth(newStartMonth) {
        this.setState({startMonth : newStartMonth, datesChanged : true});
    }

    updateEndYear(newEndYear) {
        this.setState({endYear : newEndYear, datesChanged : true});
    }

    updateEndMonth(newEndMonth) {
        this.setState({endMonth : newEndMonth, datesChanged : true});
    }

    dateChange() {
        let startDate = this.state.startYear + "-";
        let endDate = this.state.endYear + "-";

        //0 pad the months on the front
        if (parseInt(this.state.startMonth) < 10) startDate += "0" + parseInt(this.state.startMonth);
        else startDate += this.state.startMonth;
        if (parseInt(this.state.endMonth) < 10) endDate += "0" + parseInt(this.state.endMonth);
        else endDate += this.state.endMonth;

        var submission_data = {
            startDate : startDate + "-01",
            endDate : endDate + "-28"
        };

        $("#loading").show();

        let page = this;

        let route;
        if (this.props.aggregate)
            route = "/protected/all_event_counts";
        else
            route = "/protected/event_counts";

        $.ajax({
            type: "POST",
            url: route,
            data : submission_data,
            dataType: "json",
            success: function(response) {
                $("#loading").hide();

                if (response.err_msg) {
                    errorModal.show(response.err_title, response.err_msg);
                    return;
                }   

                page.state.eventCounts = response;
                page.displayPlots(page.state.airframe);
                page.setState({datesChanged : false});
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Uploads", errorThrown);
            },   
            async: true 
        });  
    }
    
    fetchStatistics() {
        let page = this;
        
        for (var stat of targetValues) {
            fetchStatistic(stat, this.props.aggregate, 
                function(response) {
                    if (response.err_msg) {
                        errorModal.show(response.err_title, response.err_msg);
                        return;
                    }   

                    page.setState({statistics: {...page.state.statistics, ...response}});
                }
            );
        }
    }

    airframeChange(airframe) {
        this.setState({airframe});
        this.displayPlots(airframe);
    }

    FlightSummary() {
        let title;
        if (this.props.aggregate)
            title = "All Fleets";
        else
            title = "Your Fleet";

        return (
                <div className="card mb-2 m-2">
                    <h4 className="card-header">{title}</h4>
                    <div className="card-body">
                        {!this.props.aggregate && this.state.notifications}
                        {!this.props.aggregate && (<hr></hr>)}
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatDurationAsync(this.state.statistics.flightTime)}</h3> Flight Hours <br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.numberFlights, integerOptions)}</h3> Flights <br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.numberAircraft, integerOptions)}</h3> Aircraft <br></br>
                            </div>
                        </div>

                        <hr></hr>
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatDurationAsync(this.state.statistics.yearFlightTime)}</h3> Flight Hours This Year<br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.yearNumberFlights, integerOptions)}</h3> Flights This Year<br></br>
                            </div>
                        </div>

                        <hr></hr>
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatDurationAsync(this.state.statistics.monthFlightTime, integerOptions)}</h3> Flight Hours (Last 30 Days)<br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.monthNumberFlights, integerOptions)}</h3> Flights (Last 30 Days)<br></br>
                            </div>
                        </div>
                    </div>
                </div>
        );
    }

    EventSummary() {
        return (
                <div className="card mb-2 m-2">
                    <h4 className="card-header">Events</h4>
                    <div className="card-body">
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.totalEvents, integerOptions)}</h3> Total Events<br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.yearEvents, integerOptions)}</h3> Events This Year<br></br>
                            </div>

                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.monthEvents, integerOptions)}</h3> Events This Month<br></br>
                            </div>

                        </div>
                    </div>
                </div>
        );
    }

    ParticipationSummary() {
        return (
                <div className="card mb-2 m-2">
                    <h4 className="card-header">Participation</h4>
                    <div className="card-body">
                        <div className="row">
                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.numberFleets, integerOptions)}</h3> Fleets <br></br>
                            </div>
                
                            <div className = "col-sm-4">
                                <h3>{formatNumberAsync(this.state.statistics.numberUsers, integerOptions)}</h3> Users<br></br>
                            </div>
                        </div>
                    </div>
                </div>
        );
    }

    UploadsSummary() {

        let totalFlights = (this.state.statistics.numberFlights + this.state.statistics.flightsWithWarning + this.state.statistics.flightsWithError);

        return (
                <div className="card mb-2 m-2">
                    <h4 className="card-header">Uploads</h4>
                    <div className="card-body">
                        <table className="row">
                            <tbody className="col-sm-6">


                                <tr>
                                    <td style={{textAlign: "right"}}>
                                        <span className="badge" style={{backgroundColor:"var(--c_info)", color:"white"}}>
                                            <i className="fa fa-fw fa-upload" aria-hidden="true"/>
                                            &nbsp;{formatNumberAsync(this.state.statistics.uploads, integerOptions)}
                                        </span>
                                    </td> 
                                    <td style={{paddingBottom: "6"}}>&nbsp;Upload(s)</td>
                                </tr>

                                <tr>
                                    <td style={{textAlign: "right"}}>
                                        <span className="badge" style={{backgroundColor:"var(--c_warning)", color:"white"}}>
                                            <i className="fa fa-fw fa-exclamation-triangle" aria-hidden="true"/>
                                            &nbsp;{formatNumberAsync(this.state.statistics.uploadsNotImported, integerOptions)}
                                        </span>
                                    </td> 
                                    <td style={{paddingBottom: "6"}}>&nbsp;Upload(s) awaiting Import</td>
                                </tr>
 
                                <tr>
                                    <td style={{textAlign: "right"}}>
                                        <span className="badge" style={{backgroundColor:"var(--c_danger)", color:"white"}}>
                                            <i className="fa fa-fw fa-exclamation-circle" aria-hidden="true"/>
                                            &nbsp;{formatNumberAsync(this.state.statistics.uploadsWithError, integerOptions)}
                                        </span>
                                    </td> 
                                    <td style={{paddingBottom: "6"}}>&nbsp;Upload(s) with Errors</td>
                                </tr>  

                            </tbody>

                            <tbody className="col-sm-6">

                                <tr>
                                    <td style={{textAlign: "right"}}>
                                        <span className="badge" style={{backgroundColor:"var(--c_valid)", color:"white"}}>
                                            <i className="fa fa-fw fa-check" aria-hidden="true"/>
                                            &nbsp;{formatNumberAsync(this.state.statistics.numberFlights, integerOptions)}
                                        </span>
                                    </td> 
                                    <td style={{paddingBottom: "6"}}>&nbsp;Flight(s) Valid</td>
                                </tr>

                                <tr>
                                    <td style={{textAlign: "right"}}>
                                        <span className="badge" style={{backgroundColor:"var(--c_warning)", color:"white"}}>
                                            <i className="fa fa-fw fa-exclamation-triangle" aria-hidden="true"/>
                                            &nbsp;{formatNumberAsync(this.state.statistics.flightsWithWarning, integerOptions)}
                                        </span>
                                    </td> 
                                    <td style={{paddingBottom: "6"}}>&nbsp;Flight(s) with Warnings</td>
                                </tr>
 
                                <tr>
                                    <td style={{textAlign: "right"}}>
                                    <span className="badge" style={{backgroundColor:"var(--c_danger)", color:"white"}}>
                                            <i className="fa fa-fw fa-exclamation-circle" aria-hidden="true"/>
                                            &nbsp;{formatNumberAsync(this.state.statistics.flightsWithError, integerOptions)}
                                        </span>
                                    </td> 
                                    <td style={{paddingBottom: "6"}}>&nbsp;Flight(s) with Errors</td>
                                </tr>     

                                <tr>
                                    <td style={{textAlign: "right"}}>
                                        <span className="badge" style={{backgroundColor:"var(--c_info)", color:"white"}}>
                                            <i className="fa fa-fw fa-cloud-download" aria-hidden="true"/>
                                            &nbsp;{formatNumberAsync(totalFlights, integerOptions)}
                                        </span>
                                    </td> 
                                    <td style={{paddingBottom: "6"}}>&nbsp;Flight(s) Imported</td>
                                </tr>

                            </tbody>
                        </table>
                    </div>
                </div>
        );
    }

    render() {
        return (
            <div style={{overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

                <div style={{flex:"0 0 auto"}}>
                    <SignedInNavbar activePage={"aggregate"} darkModeOnClickAlt={()=>{this.displayPlots(this.state.airframe);}} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
                </div>

                <div style={{overflowY:"auto", flex:"1 1 auto"}}>
                    <div className="container-fluid">
                        <div className="row">
                            <div className="col-6">{this.FlightSummary()}</div>
                            <div className="col-6">
                                {this.EventSummary()}
                                {!this.props.aggregate && this.UploadsSummary()}
                                {this.props.aggregate && this.ParticipationSummary()}
                            </div>
                        </div>

                        <div className="row">
                            <div className="col-lg-12">
                                <div className="card mb-2 m-2">
                                    <TimeHeader
                                        name="Event Statistics"
                                        airframes={airframes}
                                        airframe={this.state.airframe}
                                        startYear={this.state.startYear} 
                                        startMonth={this.state.startMonth} 
                                        endYear={this.state.endYear} 
                                        endMonth={this.state.endMonth} 
                                        datesChanged={this.state.datesChanged}
                                        dateChange={() => this.dateChange()}
                                        airframeChange={(airframe) => this.airframeChange(airframe)}
                                        updateStartYear={(newStartYear) => this.updateStartYear(newStartYear)}
                                        updateStartMonth={(newStartMonth) => this.updateStartMonth(newStartMonth)}
                                        updateEndYear={(newEndYear) => this.updateEndYear(newEndYear)}
                                        updateEndMonth={(newEndMonth) => this.updateEndMonth(newEndMonth)}
                                    />

                                <div className="card-body" style={{padding:"0" ,backgroundColor:"transparent"}}>
                                    <div className="row" style={{margin:"0"}}>
                                        <div className="col-lg-6" style={{padding:"0 0 0 0"}}>
                                            <div id="event-counts-plot"></div>
                                        </div>
                                        <div className="col-lg-6" style={{padding:"0 0 0 0"}}>
                                            <div id="event-percents-plot"></div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

            </div>
        </div>
        );
    }
}

export { SummaryPage }
