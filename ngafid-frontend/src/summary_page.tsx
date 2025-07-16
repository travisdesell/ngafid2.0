import "bootstrap";

import React from "react";

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import {TimeHeader} from "./time_header.js";

import Plotly from "plotly.js";


import "./index.css";
import * as $ from "jquery";
import type JQuery from "jquery";


airframes.unshift("All Airframes");
const index = airframes.indexOf("Garmin Flight Display");
if (index !== -1)
    airframes.splice(index, 1);


const targetValues = {
    flightTime: "/api/flight/time",
    yearFlightTime: "/api/flight/time/past-year",
    monthFlightTime: "/api/flight/time/past-month",
    numberFlights: "/api/flight/count",
    numberAircraft: "/api/aircraft/count",
    yearNumberFlights: "/api/flight/count/past-year",
    monthNumberFlights: "/api/flight/count/past-month",
    totalEvents: "/api/event/count",
    yearEvents: "/api/event/count/past-year",
    monthEvents: "/api/event/count/past-month",
    numberFleets: "/api/fleet/count",
    numberUsers: "/api/user/count",
    uploads: "/api/upload/count",
    uploadsOK: "/api/upload/count/success",
    uploadsNotImported: "/api/upload/count/pending",
    uploadsWithError: "/api/upload/count/error",
    flightsWithWarning: "/api/flight/count/with-warning",
    flightsWithError: "/api/flight/count/with-error",
};

const LOADING_STRING = "...";

const floatOptions = {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
};

const integerOptions = {};

function formatNumberAsync(value: number|string, formattingOptions: Intl.NumberFormatOptions | undefined) {
    if (value || typeof value === "number") {
        return Number(value).toLocaleString("en", formattingOptions);
    } else {
        return LOADING_STRING;
    }
}

function formatDurationAsync(seconds: number) {
    if (seconds || typeof seconds == "number") {
        return Number(seconds / (60 * 60)).toLocaleString("en", floatOptions);
    } else {
        return LOADING_STRING;
    }
}


type SuccessResponseHandlerType = (response: { err_msg: string; err_title: string; }) => void;

function fetchStatistic(
    stat: string,
    route: string,
    aggregate: boolean,
    successResponseHandler: SuccessResponseHandlerType
) {

    if (aggregate)
        route = `${route}/aggregate`;


    const errorResponseHandler = function (jqXHR: JQuery.jqXHR<unknown>, textStatus: string, errorThrown: string) {
        console.log(jqXHR);
        console.log(textStatus);
        console.log(errorThrown);
        showErrorModal("Error Loading Statistic", errorThrown);
    };

    $.ajax({
        type: 'GET',
        url: route,
        success: successResponseHandler,
        error: errorResponseHandler,
    });

}


type NotificationsState = {
    notifications: Array<{
        count: number;
        message: string;
        badgeType: string;
        name: string|undefined;
    }>;
}

class Notifications extends React.Component<object, NotificationsState> {
    constructor(props: object) {
        super(props);

        this.state = {
            notifications: [
                {
                    count: waitingUserCount,
                    message: "User(s) awaiting Access Privileges",
                    badgeType: "badge-info",
                    name: "waitingUserCount"
                },
                {
                    count: unconfirmedTailsCount,
                    message: "Tail Number(s) awaiting Confirmation",
                    badgeType: "badge-info",
                    name: "unconfirmedTailsCount"
                }
            ]
        };

        this.fetchStatistics();

    }

    fetchStatistics() {

        const successResponseHandler = (response: { err_msg: string; err_title: string; }) => {

            console.log(`Got successful response for fetched stat: ${response}`);

            //Response has an error, exit
            if (response.err_msg) {
                showErrorModal(response.err_title, response.err_msg);
                return;
            }

            //Update the notification count
            const updatedState = {
                ...this.state,
                notifications: this.state.notifications.map((notif, idx) =>
                    idx === idx ? { ...notif, count: response } : notif
                )
            } as NotificationsState;

            this.setState(updatedState);

        };

        console.log("Notifications -- Fetching Statistics...");

        for (const [/*...*/, notif] of this.state.notifications.entries()) {

            //Notification has a 'name' property, fetch the statistic
            if (Object.hasOwn(notif, "name")
                && notif.name
                && targetValues[notif.name as keyof typeof targetValues]
            ) {
                fetchStatistic(notif.name, targetValues[notif.name as keyof typeof targetValues], false, successResponseHandler);
            }

        }
    }

    render() {
        return (
            <table>
                <tbody>
                {
                    this.state.notifications.map((info, index) => {

                        //No notifications, don't display counter
                        if (info.count == 0)
                            return;

                        //Has notifications, display counter
                        else
                            return (
                                <tr key={index}>
                                    <td style={{textAlign: "right", paddingBottom: "6"}}>
                                                <span className={`badge ${  info.badgeType}`}>
                                                    <i className="fa fa-fw fa-bell" aria-hidden="true"/>
                                                    &nbsp;{Number(info.count).toLocaleString('en')}
                                                </span>
                                    </td>
                                    <td style={{paddingBottom: "6", color: "var(--c_text)"}}>&nbsp;{info.message}</td>
                                </tr>
                            );

                    })
                }
                </tbody>
            </table>
        );
    }
}


type AggregateFlightHoursByAirframe = {
    airframe: string;
    airframe_id: number;
    num_flights: number;
    total_flight_hours: number;
};

type SummaryPageState = {
    airframe: string;
    startYear: number | string;
    startMonth: number | string;
    endYear: number | string;
    endMonth: number | string;
    datesChanged: boolean;
    statistics: {
        flightTime: number;
        yearFlightTime: number;
        monthFlightTime: number;
        numberFlights: number;
        numberAircraft: number;
        yearNumberFlights: number;
        monthNumberFlights: number;
        totalEvents: number;
        yearEvents: number;
        monthEvents: number;
        numberFleets: number;
        numberUsers: number;
        uploads: number;
        uploadsOK: number;
        uploadsNotImported: number;
        uploadsWithError: number;
        flightsWithWarning: number;
        flightsWithError: number;
    };
    aggregateFlightHoursByAirframe: AggregateFlightHoursByAirframe[];
    eventCounts: object;
    notifications: React.ReactElement;
}

export type SummaryPageProps = {
    aggregate: boolean;
};

export default class SummaryPage extends React.Component<SummaryPageProps, SummaryPageState> {
    constructor(props: SummaryPageProps) {
        super(props);

        const date = new Date();
        this.state = {
            airframe: "All Airframes",
            startYear: date.getFullYear(),
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            datesChanged: false,
            statistics: Object.keys(targetValues).reduce((o, key) => ({...o, [key]: ""}), {} as {
                flightTime: number;
                yearFlightTime: number;
                monthFlightTime: number;
                numberFlights: number;
                numberAircraft: number;
                yearNumberFlights: number;
                monthNumberFlights: number;
                totalEvents: number;
                yearEvents: number;
                monthEvents: number;
                numberFleets: number;
                numberUsers: number;
                uploads: number;
                uploadsOK: number;
                uploadsNotImported: number;
                uploadsWithError: number;
                flightsWithWarning: number;
                flightsWithError: number;
            }),
            aggregateFlightHoursByAirframe: [],
            eventCounts: {},
            notifications: <Notifications/>
        };

        this.dateChange();
        this.fetchStatistics();

    }

    componentDidMount() {
        this.displayPlots(this.state.airframe);

        //In aggregate view, fetch aggregate flight hours by airframe
        if (this.props.aggregate)
            this.fetchAggregateFlightHoursByAirframe();
    }

    displayPlots(selectedAirframe: string) {

        const countData = [];
        const percentData = [];

        const fleetPercents = {
            name: this.props.aggregate ? "All Fleets" : "Your Fleet",
            type: "bar",
            orientation: "h",
            hoverinfo: "y+text",
            hovertext: [] as string[],
            y: [] as string[],
            x: [] as number[],
            flightsWithEventCounts: [] as number[],
            totalFlightsCounts: [] as number[]
        };

        const ngafidPercents = {
            name: this.props.aggregate ? "All Fleets" : 'All Other Fleets',
            type: 'bar',
            orientation: 'h',
            hoverinfo: 'y+text',
            hovertext: [] as string[],
            y: [] as string[],
            x: [] as number[],
            flightsWithEventCounts: [] as number[],
            totalFlightsCounts: [] as number[]
        };

        for (const [/*...*/, value] of Object.entries(this.state.eventCounts)) {

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

                const index = ngafidPercents.y.indexOf(value.names[i]);
                if (index !== -1) {
                    ngafidPercents.flightsWithEventCounts[index] += value.aggregateFlightsWithEventCounts[i];
                    ngafidPercents.totalFlightsCounts[index] += value.aggregateTotalFlightsCounts[i];
                } else {
                    const pos = ngafidPercents.y.length;
                    ngafidPercents.y.push(value.names[i]);
                    ngafidPercents.flightsWithEventCounts[pos] = value.aggregateFlightsWithEventCounts[i];
                    ngafidPercents.totalFlightsCounts[pos] = value.aggregateTotalFlightsCounts[i];
                }

                if (airframes.indexOf(value.airframeName) >= 0) {
                    
                    const index = fleetPercents.y.indexOf(value.names[i]);
                    if (index !== -1) {
                        fleetPercents.flightsWithEventCounts[index] += value.flightsWithEventCounts[i];
                        fleetPercents.totalFlightsCounts[index] += value.totalFlightsCounts[i];
                    } else {
                        const pos = fleetPercents.y.length;
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
        for (let j = percentData.length - 1; j >= 0; j--) {

            const value = percentData[j];
            value.x = [];

            for (let i = 0; i < value.flightsWithEventCounts.length; i++) {

                value.x.push(100.0 * value.flightsWithEventCounts[i] / value.totalFlightsCounts[i]);


                let fixedText = "";
                if (value.x[i] > 0 && value.x[i] < 1) {
                    fixedText = `${value.x[i].toFixed(-Math.ceil(Math.log10(value.x[i])) + 2)  }%`;
                } else {
                    fixedText = `${value.x[i].toFixed(2)  }%`;
                }
                value.hovertext.push(fixedText);

            }
        }

        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        const countLayout = {
            title: { text: "Event Counts" },
            barmode: "stack" as const,
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
                traceorder: "normal" as const
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                gridcolor: plotGridColor
            },
            yaxis: {
                gridcolor: plotGridColor
            }
        };

        const percentLayout = {
            title: { text: "Percentage of Flights With Event" },
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
                traceorder: "normal" as const,
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                gridcolor: plotGridColor
            },
            yaxis: {
                gridcolor: plotGridColor,
                autorange: "reversed" as const
            }
        };

        console.log("Plot bg color: ", plotBgColor);
        console.log("Plot text color: ", plotTextColor);

        const config = {responsive: true};

        Plotly.newPlot("event-counts-plot", countData, countLayout, config);
        Plotly.newPlot("event-percents-plot", percentData as unknown as Plotly.Data[], percentLayout, config);
    }

    updateStartYear(newStartYear: number) {
        this.setState({startYear: newStartYear, datesChanged: true});
    }

    updateStartMonth(newStartMonth: number) {
        this.setState({startMonth: newStartMonth, datesChanged: true});
    }

    updateEndYear(newEndYear: number) {
        this.setState({endYear: newEndYear, datesChanged: true});
    }

    updateEndMonth(newEndMonth: number) {
        this.setState({endMonth: newEndMonth, datesChanged: true});
    }

    dateChange() {
        let startDate = `${this.state.startYear  }-`;
        let endDate = `${this.state.endYear  }-`;

        const {startMonth, endMonth} = this.state as {startMonth: string, endMonth: string};

        //0 pad the months on the front
        if (parseInt(startMonth) < 10) startDate += `0${  parseInt(startMonth)}`;
        else startDate += startMonth;
        if (parseInt(endMonth) < 10) endDate += `0${  parseInt(endMonth)}`;
        else endDate += this.state.endMonth;

        const submissionData = {
            startDate: `${startDate  }-01`,
            endDate: `${endDate  }-28`
        };

        $("#loading").show();

        let route = "/api/event/count/by-airframe";
        if (this.props.aggregate)
            route = `${route}/aggregate`;

        console.log(`Got date change, fetching event counts from '${route}' with date data: `, submissionData);

        $.ajax({
            type: 'GET',
            url: route,
            data: submissionData,
            async: true,
            success: (response) => {

                //Response has an error, exit
                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                this.setState({ eventCounts: response, datesChanged: false }, () => {
                    this.displayPlots(this.state.airframe);
                });

            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Uploads", errorThrown);
            },
            complete: () => {
                $("#loading").hide();
            } 
        });
    }

    fetchStatistics() {

        console.log("SummaryPage -- Fetching Statistics...");

        for (const [stat, route] of Object.entries(targetValues)) {

            const successResponseHandler = (response: { err_msg: string; err_title: string; }) => {

                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                const result: { [key: string]: string } = {};
                result[stat] = (typeof response === "string" ? response : JSON.stringify(response));
                this.setState({statistics: {...this.state.statistics, ...result}});

            };

            fetchStatistic(stat, route, this.props.aggregate, successResponseHandler);

        }

    }

    airframeChange(airframe: string) {
        this.setState({airframe});
        this.displayPlots(airframe);
    }



    fetchAggregateFlightHoursByAirframe() {

        console.log("SummaryPage -- Fetching Aggregate Flight Hours by Airframe...");

        const route = '/api/flight/aggregate/flight_hours_by_airframe';

        $.ajax({
            type: 'GET',
            url: route,
            async: true,
            success: (response) => {

                console.log(`Got Aggregate Flight Hours by Airframe: `, response);

                //Response has an error, exit
                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                /*  
                    airframe -> airframeName
                    airframe_id -> airframeID
                    num_flights -> numFlights
                    total_flight_hours -> totalFlightHours
                */  

                this.setState({aggregateFlightHoursByAirframe: response});

            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
                showErrorModal("Error Loading Uploads", errorThrown);
            },
        });

        console.log("Finished fetching Aggregate Flight Hours by Airframe.");

    }


    FlightSummary() {

        const aggregateFlightHoursByAirframeView = () => {
            
            //Not in aggregate view, don't display
            if (!this.props.aggregate)
                return null;

            return (
                <div className="
                    fa fa-fw fa-info-circle group relative cursor-pointer
                ">

                    <div className="
                        font-sans
                        cursor-default
                        pointer-events-none group-hover:pointer-events-auto hover:pointer-events-auto
                        group-hover:block!
                        z-100 w-128 h-fit
                        absolute top-4 left-4
                        bg-[var(--c_card_tab)]
                        rounded-lg shadow-lg p-2
                        opacity-0! group-hover:opacity-100! transform-[opacity] duration-200
                    ">
                        <div className="font-bold text-lg mb-4 text-center">
                            <i className="fa fa-fw fa-clock-o my-auto mr-2"/>
                            Aggregate Flight Hours by Airframe
                        </div>
                        <table className="table-hover table-fixed w-full">

                            <colgroup>
                                <col style={{width: "40%"}}/>
                                <col style={{width: "30%"}}/>
                                <col style={{width: "30%"}}/>
                            </colgroup>


                            <thead className="leading-16 text-[var(--c_text)] border-b-1">
                                <tr>
                                    <th>Airframe</th>
                                    <th>Flights</th>
                                    <th>Hours</th>
                                </tr>
                            </thead>


                            <tbody className="text-[var(--c_text)] leading-8 before:content-['\A']">

                                {
                                    Object.entries(this.state.aggregateFlightHoursByAirframe).map(([index, data]) => (
                                        <tr key={index}
                                            className={`${parseInt(index) % 2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"} text-[var(--c_text_alt)]`}>
                                            <td className="truncate whitespace-nowrap overflow-hidden">
                                                {data.airframe}
                                            </td>
                                            <td className="font-mono truncate whitespace-nowrap overflow-hidden">
                                                {data.num_flights}
                                            </td>
                                            <td className="font-mono truncate whitespace-nowrap overflow-hidden">
                                                {Math.floor(data.total_flight_hours*10)/10}
                                            </td>
                                        </tr>
                                    ))
                                }

                            </tbody>
                            
                        </table>
                    </div>

                </div>
            );  

        };

        let title;
        if (this.props.aggregate)
            title = "All Fleets";
        else
            title = "Your Fleet";

        return (
            <div className="card mb-2 m-2">
                <h4 className="card-header flex flex-row justify-between items-center">
                    {title}
                    {aggregateFlightHoursByAirframeView()}
                </h4>
                <div className="card-body">
                    {!this.props.aggregate && this.state.notifications}
                    {!this.props.aggregate && (<hr></hr>)}
                    <div className="row">
                        <div className="col-sm-4">
                            <h3>{formatDurationAsync(this.state.statistics.flightTime)}</h3> Flight Hours <br></br>
                        </div>

                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.numberFlights, integerOptions)}</h3> Flights
                            Without Errors <br></br>
                        </div>

                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.numberAircraft, integerOptions)}</h3> Aircraft <br></br>
                        </div>
                    </div>

                    <hr></hr>
                    <div className="row">
                        <div className="col-sm-4">
                            <h3>{formatDurationAsync(this.state.statistics.yearFlightTime)}</h3> Flight Hours This
                            Year<br></br>
                        </div>

                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.yearNumberFlights, integerOptions)}</h3> Flights
                            This Year<br></br>
                        </div>
                    </div>

                    <hr></hr>
                    <div className="row">
                        <div className="col-sm-4">
                            <h3>{formatDurationAsync(this.state.statistics.monthFlightTime)}</h3> Flight
                            Hours (Last 30 Days)<br></br>
                        </div>

                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.monthNumberFlights, integerOptions)}</h3> Flights
                            (Last 30 Days)<br></br>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    EventSummary() {
        return (
            <div className="card mb-2 m-2" style={{display: "flex", flexFlow: "column nowrap", height: "50%"}}>
                <h4 className="card-header">Events</h4>
                <div className="card-body">
                    <div className="row">
                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.totalEvents, integerOptions)}</h3> Total Events<br></br>
                        </div>

                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.yearEvents, integerOptions)}</h3> Events This
                            Year<br></br>
                        </div>

                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.monthEvents, integerOptions)}</h3> Events This
                            Month<br></br>
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
                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.numberFleets, integerOptions)}</h3> Fleets <br></br>
                        </div>

                        <div className="col-sm-4">
                            <h3>{formatNumberAsync(this.state.statistics.numberUsers, integerOptions)}</h3> Users<br></br>
                        </div>
                    </div>
                </div>
            </div>
        );
    }

    UploadsSummaryAggregate() {

        console.log("Rendering Uploads Summary Aggregate...");

        //Modifes a string to be plural if the supplied is not 1
        const pluralize = (count: string | number, stringIn: string) => {
            return (count === 1 ? stringIn : `${stringIn  }s`);
        };

        return (
            <div className="card mb-2 m-2" style={{display: "flex", flexFlow: "column nowrap", height: "50%"}}>
                <h4 className="card-header">Uploads (Aggregate)</h4>
                <div className="card-body">
                    <table className="row">
                        <tbody className="col-sm-6">

                        {/* (Total) Uploads */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_info)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-upload" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.uploads, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.uploads, "Upload")}
                            </td>
                        </tr>

                        {/* Uploads Processed */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_valid)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-check" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.uploadsOK, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.uploadsOK, "Upload")} Processed
                            </td>
                        </tr>

                        {/* ⚠ Empty Row for Formatting (Helps align the bottom of the widget with the All Fleets widget) */}
                        <tr style={{opacity: 0.00, userSelect: "none"}}>
                            <td>
                                    <span className="badge">
                                        <i className="fa fa-fw fa-upload" aria-hidden="true"/>
                                        &nbsp;
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                PLACEHOLDER ROW
                            </td>
                        </tr>

                        </tbody>

                        <tbody className="col-sm-6">

                        {/* Uploads Waiting */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_warning)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-hourglass" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.uploadsNotImported, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.uploadsNotImported, "Upload")} awaiting Import
                            </td>
                        </tr>

                        {/* Uploads with Errors */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_danger)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-exclamation-circle" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.uploadsWithError, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.uploadsWithError, "Upload")} with Errors
                            </td>
                        </tr>

                        </tbody>
                    </table>
                </div>
            </div>
        );

    }

    UploadsSummary() {

        const totalFlights = (this.state.statistics.numberFlights + this.state.statistics.flightsWithError);
        const hasWarnings = (this.state.statistics.flightsWithWarning > 0);

        //Modifes a string to be plural if the supplied is not 1
        const pluralize = (count: string | number, stringIn: string) => {
            return (count === 1 ? stringIn : `${stringIn  }s`);
        };

        return (
            <div className="card mb-2 m-2" style={{display: "flex", flexFlow: "column nowrap", height: "50%"}}>
                <h4 className="card-header">Uploads</h4>
                <div className="card-body h-full">
                    <table className="row">
                        <tbody className="col-sm-6">

                        {/* (Total) Uploads */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_info)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-upload" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.uploads, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.uploads, "Upload")}
                            </td>
                        </tr>

                        {/* Uploads Waiting */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_warning)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-hourglass" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.uploadsNotImported, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.uploadsNotImported, "Upload")} awaiting Import
                            </td>
                        </tr>

                        {/* Uploads with Errors */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_danger)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-exclamation-circle" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.uploadsWithError, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.uploadsWithError, "Upload")} with Errors
                            </td>
                        </tr>

                        </tbody>

                        <tbody className="col-sm-6">

                        {/* Flights Valid */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_valid)", color: "white"}}
                                    >
                                        {
                                            (hasWarnings)
                                                ? <i className="fa fa-fw fa-check"
                                                     style={{alignContent: "center", color: "var(--c_warning)"}}
                                                     title="Flights with non-critical Warnings are included as Valid flights."/>
                                                : <i className="fa fa-fw fa-check"
                                                     style={{alignContent: "center", color: "white"}}
                                                     title="No Flights in this Fleet have Warnings."/>
                                        }
                                        &nbsp;{formatNumberAsync(this.state.statistics.numberFlights, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>&nbsp;{pluralize(this.state.statistics.numberFlights, "Flight")}
                                &nbsp;Valid
                            </td>
                        </tr>

                        {/* Flights with Warnings */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_warning)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-exclamation-triangle" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.flightsWithWarning, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.flightsWithWarning, "Flight")} with Warnings
                            </td>
                        </tr>

                        {/* Flights with Errors */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_danger)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-exclamation-circle" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(this.state.statistics.flightsWithError, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(this.state.statistics.flightsWithError, "Flight")} with Errors
                            </td>
                        </tr>

                        {/* Flights Imported */}
                        <tr>
                            <td style={{textAlign: "right"}}>
                                    <span
                                        className="badge"
                                        style={{backgroundColor: "var(--c_info)", color: "white"}}
                                    >
                                        <i className="fa fa-fw fa-cloud-download" aria-hidden="true"/>
                                        &nbsp;{formatNumberAsync(totalFlights, integerOptions)}
                                    </span>
                            </td>
                            <td style={{paddingBottom: "6"}}>
                                &nbsp;{pluralize(totalFlights, "Flight")} Imported
                            </td>
                        </tr>

                        </tbody>
                    </table>
                </div>
            </div>
        );
    }

    render() {
        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar
                        activePage={this.props.aggregate ? "aggregate" : "welcome"}
                        darkModeOnClickAlt={() => {
                            this.displayPlots(this.state.airframe);
                        }}
                        waitingUserCount={waitingUserCount}
                        fleetManager={fleetManager}
                        unconfirmedTailsCount={unconfirmedTailsCount}
                        modifyTailsAccess={modifyTailsAccess}
                        plotMapHidden={plotMapHidden}
                    />
                </div>

                <div style={{overflowY: "auto", flex: "1 1 auto"}}>
                    <div className="container-fluid">
                        <div className="row">
                            <div className="col-6">{this.FlightSummary()}</div>
                            <div className="col-6" style={{display: "flex", flexDirection: "column"}}>
                                {this.EventSummary()}
                                {this.props.aggregate ? this.UploadsSummaryAggregate() : this.UploadsSummary()}
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
                                        airframeChange={(airframe: string) => this.airframeChange(airframe)}
                                        updateStartYear={(newStartYear: number) => this.updateStartYear(newStartYear)}
                                        updateStartMonth={(newStartMonth: number) => this.updateStartMonth(newStartMonth)}
                                        updateEndYear={(newEndYear: number) => this.updateEndYear(newEndYear)}
                                        updateEndMonth={(newEndMonth: number) => this.updateEndMonth(newEndMonth)}
                                    />

                                    <div className="card-body" style={{padding: "0", backgroundColor: "transparent"}}>
                                        <div className="row" style={{margin: "0"}}>
                                            <div className="col-lg-6" style={{padding: "0 0 0 0"}}>
                                                <div id="event-counts-plot"></div>
                                            </div>
                                            <div className="col-lg-6" style={{padding: "0 0 0 0"}}>
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

export {SummaryPage};
