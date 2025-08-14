import "bootstrap";

import React from "react";

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import {TimeHeader} from "./time_header.js";

import Plotly from "plotly.js";


import "./index.css";
import * as $ from "jquery";
import type JQuery from "jquery";
import type { AirframeNameID } from "./types";


const ALL_AIRFRAMES_PAIR = {
    name: "All Airframes",
    id: -1
};
airframes.unshift(ALL_AIRFRAMES_PAIR);
const garminIndex = airframes.findIndex(airframe => airframe.name === "Garmin Flight Display");
if (garminIndex !== -1)
    airframes.splice(garminIndex, 1);



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




const date = new Date();

let startYear: number = date.getFullYear();
let startMonth: number = 1;
let endYear: number = date.getFullYear();
let endMonth: number = date.getMonth() + 1;


export function buildStartDate(year: number, month: number): string {

    //Force year and month values to be numbers
    year = +year;
    month = +month;

    console.log(`Building start date for year=${year}, month=${month}...`);

    let startDate = `${year}-`;

    //Pad the months on the front
    if (month < 10) startDate += `0${month}`;
        else startDate += month;

    startDate = `${startDate}-01`;

    console.log(`Built start date for year=${year}, month=${month}: ${startDate}`);
    return startDate;

}

export function buildEndDate(year: number, month: number): string {

    //Force year and month values to be numbers
    year = +year;
    month = +month;

    const monthName = new Intl.DateTimeFormat("en-US", { month: "long" }).format(new Date(year, month - 1));
    console.log(`Building end date for year=${year}, month=${month} (${monthName})...`);

    let endDate = `${year}-`;

    //Pad the months on the front
    if (month < 10) endDate += `0${month}`;
        else endDate += month;

    //February, check for leap year
    if (month == 2) {

        //Leap year, append '29'
        if ((year % 4 === 0 && year % 100 !== 0) || (year % 400 === 0)) {
            endDate = `${endDate}-29`;

        //Otherwise, append '28'
        } else {
            endDate = `${endDate}-28`;
        }

    
    //April, June, September, November -> 30 days
    } else if ([4, 6, 9, 11].includes(month)) {
        endDate = `${endDate}-30`;
    }
    
    //All other months -> 31 days
    else {
        endDate = `${endDate}-31`;
    }

    console.log(`Built end date for year=${year}, month=${month} (${monthName}): ${endDate}`);
    return endDate;

}

async function fetchStatistic<ResponseType>(
    stat: string,
    route: string,
    aggregate: boolean,
    airframe: AirframeNameID = ALL_AIRFRAMES_PAIR,
    startYear: number,
    startMonth: number,
    endYear: number,
    endMonth: number,
    successResponseHandler: SuccessResponseHandlerType<ResponseType>
) {

    console.log(`Fetching statistic '${stat}' from route '${route}' with aggregate=${aggregate}...`);

    if (aggregate)
        route = `${route}/aggregate`;

    const errorResponseHandler = function (jqXHR: JQuery.jqXHR<unknown>, textStatus: string, errorThrown: string) {
        console.log(jqXHR);
        console.log(textStatus);
        console.log(errorThrown);
        showErrorModal(`Error Loading Statistic: ${stat} (aggregate = ${aggregate})`, errorThrown);
    };

    const startDate = buildStartDate(startYear, startMonth);
    const endDate = buildEndDate(endYear, endMonth);

    const submissionData = {
        startDate: startDate,
        endDate: endDate,
        airframeID: airframe.id
    };


    console.log(`Fetching statistic '${stat}' from route '${route}' with date data: `, submissionData);

    $.ajax({
        type: 'GET',
        url: route,
        data: submissionData,
        success: successResponseHandler,
        error: errorResponseHandler,
        complete: (jqXHR) => {
            console.log(`Finished fetching statistic '${stat}' from route '${route}' with aggregate=${aggregate}. (Status: ${jqXHR.status})`);
        }
    });

}

type SuccessResponseHandlerType<ResponseType> = (response: ResponseType) => void;




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

        this.fetchNotificationStatistics();

    }

    fetchNotificationStatistics() {


        console.log("Notifications -- Fetching Statistics...");

        for (const [i, notif] of this.state.notifications.entries()) {

            type NotificationType = {
                count: number;
                message: string;
                badgeType: string;
                name: string | undefined;
                err_msg?: string;
                err_title?: string;
            };

            const successResponseHandler = (response:NotificationType) => {

                console.log(`Got successful response for fetched stat: ${response}`);

                //Response has an error, exit
                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                //Update the notification count
                this.setState(prev => ({
                    notifications: prev.notifications.map((n, j) =>
                        j === i ? { ...n, count: response.count } : n
                    )
                }));

            };

            //Notification has a 'name' property, fetch the statistic
            if (Object.hasOwn(notif, "name")
                && notif.name
                && targetValues[notif.name as keyof typeof targetValues]
            ) {
                fetchStatistic<NotificationType>(
                    notif.name,
                    targetValues[notif.name as keyof typeof targetValues],
                    false,
                    undefined,
                    startYear,
                    startMonth,
                    endYear,
                    endMonth,
                    successResponseHandler
                );
            }

        }
    }

    render() {

        //All notification counts are 0, don't display the card
        if (this.state.notifications.every(info => info.count === 0))
            return null;

        return (


            <div className="card w-full flex-1">

                <h4 className="card-header w-full flex` flex-row justify-between items-center">
                    Notifications
                </h4>

                <div className="card-body px-12! w-full">
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
                </div>
            </div>
        );
    }
}


type FlightHoursByAirframe = {
    airframe: string;
    airframe_id: number;
    num_flights: number;
    total_flight_hours: number;
};

type SummaryPageState = {
    airframe: AirframeNameID;
    datesOrAirframeChanged: boolean;
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
    flightHoursByAirframe: FlightHoursByAirframe[];
    aggregateFlightHoursByAirframe: FlightHoursByAirframe[];
    eventCounts: object;
    notifications: React.ReactElement;
}

export type SummaryPageProps = {
    aggregate: boolean;
};

export default class SummaryPage extends React.Component<SummaryPageProps, SummaryPageState> {
    constructor(props: SummaryPageProps) {
        super(props);

        this.state = {
            airframe: airframes[0],
            datesOrAirframeChanged: false,
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
            flightHoursByAirframe: [],
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

        //Otherwise, fetch fleet flight hours by airframe
        else
            this.fetchFlightHoursByAirframe();
    }

    displayPlots(selectedAirframe: AirframeNameID) {

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
            if ((selectedAirframe.name !== value.airframeName) && (selectedAirframe.name !== "All Airframes"))
                continue;

            value.name = value.airframeName;
            value.y = value.names;
            value.type = "bar";
            value.orientation = "h";

            //Don't add airframes to the count plot that the fleet doesn't have
            if (airframes.some(airframe => airframe.name === value.airframeName))
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

                const fleetAirframeNames = new Set(airframes.map(a => a.name));
                if (fleetAirframeNames.has(value.airframeName)) {
                    
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
        // Plotly.newPlot("event-percents-plot", percentData as unknown as Plotly.Data[], percentLayout, config);
        Plotly.newPlot("event-percents-plot", percentData as unknown as Plotly.Data[], percentLayout, config);
    }

    updateStartYear(newStartYear: number) {
        this.setState({datesOrAirframeChanged: true});
        startYear = newStartYear;
    }

    updateStartMonth(newStartMonth: number) {
        this.setState({datesOrAirframeChanged: true});
        startMonth = newStartMonth;
    }

    updateEndYear(newEndYear: number) {
        this.setState({datesOrAirframeChanged: true});
        endYear = newEndYear;
    }

    updateEndMonth(newEndMonth: number) {
        this.setState({datesOrAirframeChanged: true});
        endMonth = newEndMonth;
    }

    airframeChangeFromName(airframeName: string) {

        //Find airframe data in list corresponding to the name
        const airframe = airframes.find(a => a.name === airframeName);

        //Got an airframe from the name, change the state
        if (airframe)
            this.airframeChange(airframe);
        
    }

    airframeChange(airframe: AirframeNameID) {
        this.setState({airframe, datesOrAirframeChanged: true});
    }

    dateChange() {

        $("#loading").show();

        //...
        this.fetchEventCountByAirframe();

        //In aggregate view, re-fetch aggregate flight hours by airframe
        if (this.props.aggregate) 
            this.fetchAggregateFlightHoursByAirframe();
        //Otherwise, re-fetch fleet flight hours by airframe
        else
            this.fetchFlightHoursByAirframe();

        //Re-fetch statistics   [TODO: Only re-fetch the date-dependent ones ?]
        this.fetchStatistics();

        //Re-fetch notifications statistics
        this.setState({notifications: <Notifications/>});
        
        $("#loading").show();

    }

    async fetchEventCountByAirframe() {


        const startDate = buildStartDate(startYear, startMonth);
        const endDate = buildEndDate(endYear, endMonth);

        const submissionData = {
            startDate: startDate,
            endDate: endDate
        };

        const route = `/api/event/count/by-airframe${this.props.aggregate ? "/aggregate" : ""}`;
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

                this.setState({ eventCounts: response, datesOrAirframeChanged: false }, () => {
                    this.displayPlots(this.state.airframe);
                });

            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Getting Event Counts by Airframe", errorThrown);
            },
            complete: () => {
                $("#loading").hide();
            } 
        });

    }

    async fetchStatistics() {

        console.log("SummaryPage -- Fetching Statistics...");

        for await (const [stat, route] of Object.entries(targetValues)) {

            const successResponseHandler = (response: { err_msg: string; err_title: string; }) => {

                console.log("Got response for statistic: ", stat, response);

                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                const result: { [key: string]: string } = {};
                result[stat] = (typeof response === "string" ? response : JSON.stringify(response));
                this.setState(prev => ({
                    statistics: {...prev.statistics, ...result}
                }));

            };

            fetchStatistic(
                stat,
                route,
                this.props.aggregate,
                this.state.airframe,
                startYear,
                startMonth,
                endYear,
                endMonth,
                successResponseHandler
            );

        }

    }

    fetchFlightHoursByAirframe() {

        console.log("SummaryPage -- Fetching Fleet Flight Hours by Airframe...");

        const route = '/api/flight/flight_hours_by_airframe';

        const startDate = buildStartDate(startYear, startMonth);
        const endDate = buildEndDate(endYear, endMonth);

        const submissionData = {
            startDate: startDate,
            endDate: endDate,
            airframeID: this.state.airframe.id
        };

        $.ajax({
            type: 'GET',
            url: route,
            data: submissionData,
            async: true,
            success: (response) => {

                console.log(`Got Fleet Flight Hours by Airframe: `, response);

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

                this.setState({flightHoursByAirframe: response});

            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
                showErrorModal("Error Getting Fleet Flight Hours by Airframe", errorThrown);
            },
        });

        console.log("Finished fetching Fleet Flight Hours by Airframe.");

    }

    fetchAggregateFlightHoursByAirframe() {

        console.log("SummaryPage -- Fetching Aggregate Flight Hours by Airframe...");

        const route = '/api/flight/aggregate/flight_hours_by_airframe';

        const startDate = buildStartDate(startYear, startMonth);
        const endDate = buildEndDate(endYear, endMonth);

        const submissionData = {
            startDate: startDate,
            endDate: endDate,
            airframeID: this.state.airframe.id
        };

        $.ajax({
            type: 'GET',
            url: route,
            data: submissionData,
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
                showErrorModal("Error Getting Aggregate Flight Hours by Airframe", errorThrown);
            },
        });

        console.log("Finished fetching Aggregate Flight Hours by Airframe.");

    }

    AggregateParticipationSummary() {
        return (
            <div className="card flex flex-col flex-1 h-full">
                <h4 className="card-header">Participation</h4>
                <div className="card-body px-12!">
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
            <div className="card flex flex-col flex-1 h-full">
                <h4 className="card-header">Uploads (Aggregate)</h4>
                <div className="card-body px-12! flex flex-col h-full">
                    <table className="row">
                        <tbody className="col-sm-6 px-0">

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

                        <tbody className="col-sm-6 px-0">

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
            <div className="card flex flex-col h-full flex-1">
                <h4 className="card-header">Uploads</h4>
                <div className="card-body px-12! flex-1 flex flex-col h-full">
                    <table className="row w-full h-full">
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

        const TABLE_DATA = [
            {
                name: "Flight Count",
                value: formatNumberAsync(this.state.statistics.numberFlights, integerOptions),
                yearValue: formatNumberAsync(this.state.statistics.yearNumberFlights, integerOptions),
                monthValue: formatNumberAsync(this.state.statistics.monthNumberFlights, integerOptions)
            },
            {
                name: "Flight Hours",
                value: formatDurationAsync(this.state.statistics.flightTime),
                yearValue: formatDurationAsync(this.state.statistics.yearFlightTime),
                monthValue: formatDurationAsync(this.state.statistics.monthFlightTime)
            },
            {
                name: "Event Count",
                value: formatNumberAsync(this.state.statistics.totalEvents, integerOptions),
                yearValue: formatNumberAsync(this.state.statistics.yearEvents, integerOptions),
                monthValue: formatNumberAsync(this.state.statistics.monthEvents, integerOptions)
            },
            {
                name: "Aircraft",
                value: formatNumberAsync(this.state.statistics.numberAircraft, integerOptions),
                yearValue: undefined,
                monthValue: undefined
            }
        ];

        const CELL_NOT_AVAILABLE = (
            <i className="opacity-50 text-sm">N/A</i>
        );

        const flightHoursByAirframeView = () => {

            const HAS_NO_DATA = (this.state.flightHoursByAirframe.length === 0);

            return (
                <div className="card flex-1 h-full">


                    {/* Table Header */}
                    <div className="text-2xl card-header opacity-100">
                        Flight Hours by Airframe
                    </div>


                    <div className="card-body px-12! opacity-100! text-center text-sm">

                        {
                            HAS_NO_DATA
                            &&
                            <div className="truncate whitespace-nowrap overflow-hidden italic text-yellow-500 text-base">

                                {/* Status Icon */}
                                <i className={`mr-2 scale-100 fa fa-warning`}/>

                                {/* Status Name */}
                                <span>No data available for the selected airframe(s) and date range!</span>

                            </div>
                        }
                        

                        {/* Flight Hours by Airframe Table */}
                        <table className="table-hover table-fixed rounded-lg w-full">

                            <colgroup>
                                <col style={{width: "40%"}}/>
                                <col style={{width: "30%"}}/>
                                <col style={{width: "30%"}}/>
                            </colgroup>


                            <thead className="leading-16 text-[var(--c_text)] border-b-1">
                                <tr>
                                    <th>Airframe</th>
                                    <th className="text-right">Flights</th>
                                    <th className="text-right">Hours</th>
                                </tr>
                            </thead>


                            <tbody className="text-[var(--c_text)] leading-8 before:content-['\A']">

                                {/* Empty spacer row */}
                                <tr className="pointer-none bg-transparent">
                                    <td colSpan={3} className="h-6" />
                                </tr>

                                {
                                    Object.entries(this.state.flightHoursByAirframe).map(([index, data]) => (
                                        <tr
                                            key={index}
                                            className={`${parseInt(index) % 2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"}`}
                                        >
                                            <td className="truncate whitespace-nowrap overflow-hidden">
                                                {data.airframe}
                                            </td>
                                            <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                {data.num_flights}
                                            </td>
                                            <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                {data.total_flight_hours.toFixed(2)}
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

        const aggregateFlightHoursByAirframeView = () => {

            const HAS_NO_DATA = (this.state.aggregateFlightHoursByAirframe.length === 0);

            return (
                <div className="card flex-1 h-full">

                    {/* Table Header */}
                    <div className="text-2xl card-header opacity-100">
                        Aggregate Flight Hours by Airframe
                    </div>


                    <div className="card-body px-12! opacity-100! text-center text-sm">

                        {
                            HAS_NO_DATA
                            &&
                            <div className="truncate whitespace-nowrap overflow-hidden italic text-yellow-500 text-base">

                                {/* Status Icon */}
                                <i className={`mr-2 scale-100 fa fa-warning`}/>

                                {/* Status Name */}
                                <span>No data available for the selected airframe(s) and date range!</span>

                            </div>
                        }
                    

                        {/* Aggregate Flight Hours by Airframe Table */}
                        <table className="table-hover table-fixed rounded-lg w-full">

                            <colgroup>
                                <col style={{width: "40%"}}/>
                                <col style={{width: "30%"}}/>
                                <col style={{width: "30%"}}/>
                            </colgroup>


                            <thead className="leading-16 text-[var(--c_text)] border-b-1">
                                <tr>
                                    <th>Airframe</th>
                                    <th className="text-right">Flights</th>
                                    <th className="text-right">Hours</th>
                                </tr>
                            </thead>


                            <tbody className="text-[var(--c_text)] leading-8 before:content-['\A']">

                                {/* Empty spacer row */}
                                <tr className="pointer-none bg-transparent">
                                    <td colSpan={3} className="h-6" />
                                </tr>

                                {
                                    Object.entries(this.state.aggregateFlightHoursByAirframe).map(([index, data]) => (
                                        <tr
                                            key={index}
                                            className={`${parseInt(index) % 2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"}`}
                                        >
                                            <td className="truncate whitespace-nowrap overflow-hidden">
                                                {data.airframe}
                                            </td>
                                            <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                {data.num_flights}
                                            </td>
                                            <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                {data.total_flight_hours.toFixed(2)}
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

        const DISPLAY_YEAR_MONTH_COLUMNS = false;

        const newSummaryTable = (
            <div className="card flex-1 h-full">
                <div className="text-2xl card-header opacity-100">
                    Statistics Table
                </div>

                <div className="card-body px-12! opacity-100! text-center text-sm">

                    {/* Summary Table */}
                    <table className="table-hover table-fixed rounded-lg w-full">

                        <colgroup>
                            <col style={{ width: "25%" }} />
                            <col style={{ width: `${DISPLAY_YEAR_MONTH_COLUMNS ? '25' : '75'}%` }} />
                            {
                                DISPLAY_YEAR_MONTH_COLUMNS
                                ?<>
                                    <col style={{ width: "25%" }} />
                                    <col style={{ width: "25%" }} />
                                </>
                                : null
                            }
                        </colgroup>

                        <thead className="leading-16 text-[var(--c_text)] border-b-1">
                            <tr>
                                <th>Name</th>
                                <th className="text-right">Total</th>
                                {
                                    DISPLAY_YEAR_MONTH_COLUMNS
                                    ?<>
                                        <th className="text-right">This Year</th>
                                        <th className="text-right">This Month</th>
                                    </>
                                    : null
                                }
                            </tr>
                        </thead>


                        <tbody className="text-[var(--c_text)] leading-8 before:content-['\A']">

                            {/* Empty spacer row */}
                            <tr className="pointer-none bg-transparent">
                                <td colSpan={3} className="h-6" />
                            </tr>

                            {TABLE_DATA.map((entry, index) => {
                                return (
                                    <React.Fragment key={entry.name}>
                                        <tr
                                            className={`${index % 2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"}`}
                                        >

                                            <td className="truncate whitespace-nowrap overflow-hidden">
                                                {entry.name}
                                            </td>
                                            <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                {
                                                    entry.value
                                                    ?
                                                    (<h3>{entry.value}</h3>)
                                                    :
                                                    CELL_NOT_AVAILABLE
                                                }
                                            </td>

                                            {
                                                DISPLAY_YEAR_MONTH_COLUMNS
                                                ?
                                                <>
                                                    <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                        {
                                                            entry.yearValue
                                                            ?
                                                            (<h3>{entry.yearValue}</h3>)
                                                            :
                                                            CELL_NOT_AVAILABLE
                                                        }
                                                    </td>
                                                    <td className="truncate whitespace-nowrap overflow-hidden text-right">
                                                        {
                                                            entry.monthValue
                                                            ?
                                                            (<h3>{entry.monthValue}</h3>)
                                                            :
                                                            CELL_NOT_AVAILABLE
                                                        }
                                                    </td>
                                                </>
                                                : null
                                            }

                                        </tr>
                                    </React.Fragment>
                                    );
                                })}
                        </tbody>
                    </table>

                </div>
            </div>
        );

        const navbarArea = (
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
        );

        const timeHeader = (
            <div className="m-2 py-4 px-3">
                <TimeHeader
                    className="rounded-lg! bg-(--c_card_header_bg_opaque)! border-(--c_border_alt)! border-1!"
                    name={`Event Statistics Summary ${this.props.aggregate ? "(Aggregate)" : ""}`}
                    airframes={airframes.map((airframe: AirframeNameID) => airframe.name)}
                    airframe={this.state.airframe.name}
                    startYear={startYear}
                    startMonth={startMonth}
                    endYear={endYear}
                    endMonth={endMonth}
                    datesOrAirframeChanged={this.state.datesOrAirframeChanged}
                    dateChange={() => this.dateChange()}
                    airframeChange={(airframe: string) => this.airframeChangeFromName(airframe)}
                    updateStartYear={(newStartYear: number) => this.updateStartYear(newStartYear)}
                    updateStartMonth={(newStartMonth: number) => this.updateStartMonth(newStartMonth)}
                    updateEndYear={(newEndYear: number) => this.updateEndYear(newEndYear)}
                    updateEndMonth={(newEndMonth: number) => this.updateEndMonth(newEndMonth)}
                />
            </div>
        );

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                {/* Navbar */}
                {navbarArea}

                {/* Page Content Area */}
                <div className="flex flex-col px-4 flex-1 min-h-0 overflow-y-auto">

                    {/* Time Header */}
                    {timeHeader}

                    {/* Tables Row */}
                    <div className="
                        flex flex-row items-stretch justify-between
                        mx-4
                        gap-4
                    ">

                        {/* Tables Row -- Summary Table (& Aggregate Flight Hours by Airframe Table) */}
                        <div className="grid grid-cols-2 gap-4 w-full">

                            {/* Summary Table */}
                            {newSummaryTable}

                            {/* Uploads Summary */}
                            {this.props.aggregate ? this.UploadsSummaryAggregate() : this.UploadsSummary()}

                            {/* Aggregate Flight Hours by Airframe Table */}
                            {
                                this.props.aggregate
                                ? aggregateFlightHoursByAirframeView()
                                : flightHoursByAirframeView()
                            }
                            
                            {/* Notifications / Participation */}
                            <div className="flex flex-col h-full flex-1">
                                {
                                    //Aggregate --> Participation
                                    this.props.aggregate
                                    ? this.AggregateParticipationSummary()

                                    //Non-Aggregate --> Notifications
                                    :
                                    this.state.notifications
                                }
                            </div>


                        </div>

                        {/* Tables Row -- Uploads Table & (Notifications or Participation) */}
                        {/* <div className="flex flex-col flex-1 h-full gap-4">


                        </div> */}

                    </div>

                    <div className="container-fluid">

                        <div className="row">
                            <div className="col-lg-12">
                                <div className="card mx-2 mb-4 mt-8">

                                    <h4 className="card-header">Event Counts and Percentages</h4>

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
