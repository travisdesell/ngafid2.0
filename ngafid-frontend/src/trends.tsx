import 'bootstrap';

import React from "react";

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import { TimeHeader } from "./time_header.js";
import GetDescription from "./get_description.js";

import Plotly from 'plotly.js';
import Tooltip from "react-bootstrap/Tooltip";
import {OverlayTrigger} from "react-bootstrap";


airframes.unshift("All Airframes");
const index = airframes.indexOf("Garmin Flight Display");
if (index !== -1)
    airframes.splice(index, 1);


//Sort incoming event names
eventNames.sort();

/*
var trace1 = {
    name: 'test1',
    x: [1, 2, 3, 4],
    y: [10, 15, 13, 17],
    type: 'scatter'
};

var trace2 = {
    name: 'test2',
    x: [1, 2, 3, 4],
    y: [16, 5, 11, 9],
    type: 'scatter'
};
*/



type CSVValues = {
    [eventName: string]: {
        [airframeName: string]: {
            [date: string]: {
                eventCount: number;
                flightsWithEventCount: number;
                totalFlights: number;
            };
        };
    };
}

type CountsData = {
    airframeName: string;
    name: string;
    eventName: string;
    x: string[];
    y: number[];
    dates: string[];
    type: string;
    hoverinfo: string;
    hovertext: string[];
    legendgroup: string;
    line: {
        dash?: string;
        width?: number;
    };
    mode?: string;
    aggregateFlightsWithEventCounts: string[];
    aggregateTotalFlightsCounts: string[];
    aggregateTotalEventsCounts: string[];
    flightsWithEventCounts: { [date: string]: number };
    totalFlightsCounts: { [date: string]: number };
    totalEventsCounts: string[];
}

type TrendsData = {
    airframeName: string;
    eventName: string;
    dates: string[];
    aggregateFlightsWithEventCounts: number[];
    aggregateTotalEventsCounts: number[];
    aggregateTotalFlightsCounts: number[];
    flightsWithEventCounts: number[];
    totalEventsCounts: number[];
    totalFlightsCounts: number[];
}


type TrendsPageProps = {
    aggregate_page: boolean
};


type TrendsPageState = {
    eventCounts: { [eventName:string]: { [airframeName: string]: TrendsData } },
    countData: Plotly.Data[],
    percentData: Plotly.Data[],
    eventFleetPercents: { [key: string]: CountsData },
    eventNGAFIDPercents: { [key: string]: CountsData },
    airframe: string,
    startYear: number,
    startMonth: number,
    endYear: number,
    endMonth: number,
    datesChanged: boolean,
    aggregatePage: boolean,
    eventChecked: { [key: string]: boolean },
    eventsEmpty: { [key: string]: boolean }
};

class TrendsPage extends React.Component<TrendsPageProps, TrendsPageState> {

    constructor(props: TrendsPageProps) {
        
        super(props);
        const eventChecked: { [key: string]: boolean } = {};
        const eventsEmpty: { [key: string]: boolean } = {};

        eventNames.unshift("ANY Event");
        for (let i = 0; i < eventNames.length; i++) {

            const eventNameCur = eventNames[i];
            eventChecked[eventNameCur] = false;
            eventsEmpty[eventNameCur] = true;
        }
        eventsEmpty["ANY Event"] = true;

        const date = new Date();
        this.state = {
            eventCounts: {},
            countData: [],
            percentData: [],
            eventFleetPercents: {},
            eventNGAFIDPercents: {},
            airframe: "All Airframes",
            startYear: 2020,
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            datesChanged: false,
            aggregatePage: props.aggregate_page,
            eventChecked: eventChecked,
            eventsEmpty: eventsEmpty
        };

        this.fetchMonthlyEventCounts();
    }

    componentDidMount() {

        //Display the plots for the initial state
        this.displayPlots("All Airframes");

    }

    startDate() {
        let startDate = `${this.state.startYear}-`;

        const startMonthNumeric = Number(this.state.startMonth);

        //Start month is less than 10, add leading zero
        if (startMonthNumeric < 10)
            startDate += `0${startMonthNumeric}`;

        //Otherwise, just append the month
        else
            startDate += this.state.startMonth;

        return startDate;
    }

    endDate() {
        let endDate = `${this.state.endYear}-`;

        const endMonthNumeric = Number(this.state.endMonth);

        //End month is less than 10, add leading zero
        if (endMonthNumeric < 10)
            endDate += `0${endMonthNumeric}`;

        //Otherwise, just append the month
        else
            endDate += this.state.endMonth;

        return endDate;
    }

    fetchMonthlyEventCounts() {
        const submissionData = {
            startDate: `${this.startDate()  }-01`,
            endDate: `${this.endDate()  }-28`,
            aggregatePage: this.props.aggregate_page
        };

        $('#loading').hide();

        return new Promise((resolve, reject) => {
            $.ajax({
                type: 'GET',
                url: '/api/event/count/monthly/by-name',
                data: submissionData,
                dataType: 'json',
                async: true,
                success: (response) => {

                    if (response.err_msg) {
                        showErrorModal(response.err_title, response.err_msg);
                        return;
                    }

                    const responseObj = response as { [eventName: string]: { [airframeName: string]: TrendsData } };

                    const countsMerged: { [airframeName: string]: TrendsData } = {};
                    for (const countsObject of Object.values(responseObj)) {

                        for (const airframeName of Object.keys(countsObject)) {

                            if (airframeName === "Garmin Flight Display")
                                continue;

                            const countsAirframe = countsObject[airframeName];

                            //Airframe name is not in the merged counts object yet, add it
                            if (!(airframeName in countsMerged)) {

                                countsMerged[airframeName] = {
                                    airframeName: airframeName,
                                    eventName: "ANY Event",
                                    dates: [...countsAirframe.dates],
                                    aggregateFlightsWithEventCounts: [...countsAirframe.aggregateFlightsWithEventCounts],
                                    aggregateTotalEventsCounts: [...countsAirframe.aggregateTotalEventsCounts],
                                    aggregateTotalFlightsCounts: [...countsAirframe.aggregateTotalFlightsCounts],
                                    flightsWithEventCounts: [...countsAirframe.flightsWithEventCounts],
                                    totalEventsCounts: [...countsAirframe.totalEventsCounts],
                                    totalFlightsCounts: [...countsAirframe.totalFlightsCounts]
                                };

                            //Airframe name is already in the merged counts object, add the counts
                            } else {

                                for (let i = 0; i < countsAirframe.dates.length; i++) {

                                    if (countsAirframe.totalEventsCounts[i] === 0)
                                        continue;

                                    countsMerged[airframeName].aggregateFlightsWithEventCounts[i] += countsAirframe.aggregateFlightsWithEventCounts[i];
                                    countsMerged[airframeName].aggregateTotalEventsCounts[i] += countsAirframe.aggregateTotalEventsCounts[i];
                                    countsMerged[airframeName].aggregateTotalFlightsCounts[i] += countsAirframe.aggregateTotalFlightsCounts[i];
                                    countsMerged[airframeName].flightsWithEventCounts[i] += countsAirframe.flightsWithEventCounts[i];
                                    countsMerged[airframeName].totalEventsCounts[i] += countsAirframe.totalEventsCounts[i];
                                    countsMerged[airframeName].totalFlightsCounts[i] += countsAirframe.totalFlightsCounts[i];
                                }

                            }

                        }

                    }

                    this.setState(() => ({
                        eventCounts: {
                            ...responseObj,
                            ["ANY Event"]: countsMerged
                        }
                    }));

                    this.displayPlots(this.state.airframe);

                    resolve(response);
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    showErrorModal("Error Loading Uploads", errorThrown);
                    reject(errorThrown);
                },
            });
        });

    }

    exportCSV() {

        const selectedAirframe = this.state.airframe;

        const eventNames: string[] = [];
        const airframeNames: string[] = [];
        const dates: string[] = [];
        const csvValues : CSVValues = {};

        const { eventCounts } = this.state;

        for (const [eventName, countsObject] of Object.entries(eventCounts)) {

            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName])
                continue;

            //Ensure the eventNames array contains unique names only
            if (!eventNames.includes(eventName))
                eventNames.push(eventName);

            for (const [/*...*/, value] of Object.entries(countsObject as unknown as { [key: string]: CountsData })) {

                if (value.airframeName === "Garmin Flight Display")
                    continue;

                if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes")
                    continue;

                const airframeName = value.airframeName;

                //Make sure the airframeNames array is unique names only
                if (!airframeNames.includes(airframeName))
                        airframeNames.push(airframeName);

                console.log(value.dates);

                for (let i = 0; i < value.dates.length; i++) {
                    const date = value.dates[i];
                    const eventCount = value.y[i];
                    let flightsWithEventCount;
                    let totalFlights;
                    if (this.state.aggregatePage) {
                        flightsWithEventCount = value.aggregateFlightsWithEventCounts[i];
                        totalFlights = value.aggregateTotalFlightsCounts[i];
                    } else {
                        flightsWithEventCount = value.flightsWithEventCounts[i];
                        totalFlights = value.totalFlightsCounts[i];
                    }

                    //make sure the dates array is unique dates only
                    if (!dates.includes(date)) {
                        dates.push(date);
                    }

                    if (!(eventName in csvValues)) {
                        csvValues[eventName] = {};
                    }

                    if (!(airframeName in csvValues[eventName])) {
                        csvValues[eventName][airframeName] = {};
                    }

                    csvValues[eventName][airframeName][date] = {
                        eventCount: eventCount,
                        flightsWithEventCount: Number(flightsWithEventCount),
                        totalFlights: Number(totalFlights)
                    };
                }
            }
        }
        eventNames.sort();
        airframeNames.sort();
        dates.sort();

        for (const eventName of eventNames) {
            console.log(csvValues[eventName]);

            for (const airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    console.log(`\t${  eventName  } - ${  airframeName  } has ${  Object.keys(csvValues[eventName][airframeName]).length  } entries!`);
                }
            }
        }

        let filetext = "";

        let needsComma = false;
        for (const eventName of eventNames) {
            for (const airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }

                    filetext += eventName;
                    filetext += `,${  eventName}`;
                    filetext += `,${  eventName}`;
                }
            }
        }
        filetext += "\n";

        needsComma = false;
        for (const eventName of eventNames) {
            for (const airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }
                    filetext += airframeName;
                    filetext += `,${  airframeName}`;
                    filetext += `,${  airframeName}`;
                }
            }
        }
        filetext += "\n";

        needsComma = false;
        for (const eventName of eventNames) {
            for (const airframeName of airframeNames) {
                if (airframeName in csvValues[eventName]) {
                    if (needsComma) {
                        filetext += ",";
                    } else {
                        needsComma = true;
                    }
                    filetext += "Events";
                    filetext += ",Flights With Event";
                    filetext += ",Total Flights";
                }
            }
        }
        filetext += "\n";

        for (let i = 0; i < dates.length; i++) {
            const date = dates[i];

            needsComma = false;
            for (const eventName of eventNames) {
                for (const airframeName of airframeNames) {
                    if (airframeName in csvValues[eventName]) {
                        if (needsComma) {
                            filetext += ",";
                        } else {
                            needsComma = true;
                        }

                        if (date in csvValues[eventName][airframeName]) {
                            filetext += csvValues[eventName][airframeName][date].eventCount;
                            filetext += `,${  csvValues[eventName][airframeName][date].flightsWithEventCount}`;
                            filetext += `,${  csvValues[eventName][airframeName][date].totalFlights}`;
                        } else {
                            filetext += ",,";
                        }
                    }
                }
            }
            filetext += "\n";
        }

        console.log("eventNames:");
        console.log(eventNames);
        console.log("airframeNames:");
        console.log(airframeNames);
        console.log("dates:");
        console.log(dates);


        const filename = "trends.csv";

        console.log("exporting csv!");

        const element = document.createElement('a');
        element.setAttribute('href', `data:text/plain;charset=utf-8,${  encodeURIComponent(filetext)}`);
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    }

    displayPlots(selectedAirframe: string) {
        console.log(`Displaying plots with airframe: '${  selectedAirframe  }'`);

        this.setState({
            eventFleetPercents: {},
            eventNGAFIDPercents: {},
            countData: [],
            percentData: [],
        });

        const newEventFleetPercents: { [key: string]: CountsData } = {};
        const newEventNGAFIDPercents: { [key: string]: CountsData } = {};


        const counts = (this.state.eventCounts == null ? {} : this.state.eventCounts);

        const airframeNames:string[] = [];
        for (const countsObject of Object.values(counts)) {

            for (const [/*...*/, value] of Object.entries(countsObject as { [key: string]:TrendsData })) {

                const airframeName = value.airframeName;
                if (!airframeNames.includes(airframeName))
                    airframeNames.push(airframeName);

            }

        }

        for (const [eventName, countsObject] of Object.entries(counts)) {

            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!this.state.eventChecked[eventName])
                continue;

            let fleetPercents = null;
            let ngafidPercents = null;

            if (eventName in newEventFleetPercents) {

                console.log('Getting existing fleetPercents!');

                fleetPercents = newEventFleetPercents[eventName];
                ngafidPercents = newEventNGAFIDPercents[eventName];

            } else {

                console.log('Setting initial fleetPercents!');

                if (!fleetPercents) {
                    fleetPercents = {
                        name: `${eventName} - Your Fleet`,
                        type: 'scatter',
                        hoverinfo: 'x+text',
                        hovertext: [],
                        y: [],
                        x: [],
                        flightsWithEventCounts: {},
                        totalFlightsCounts: {},
                    } as unknown as CountsData;

                    newEventFleetPercents[eventName] = fleetPercents;
                }

                if (!ngafidPercents) {
                    const ngafidPercentsName = `${eventName} - ${this.state.aggregatePage ? 'All Fleets' : 'All Other Fleets'}`;
                    ngafidPercents = {
                        name: ngafidPercentsName,
                        type: 'scatter',
                        hoverinfo: 'x+text',
                        hovertext: [],
                        y: [],
                        x: [],
                        flightsWithEventCounts: {},
                        totalFlightsCounts: {},
                    } as unknown as CountsData;

                    newEventNGAFIDPercents[eventName] = ngafidPercents;
                }

            }

            for (let [/*...*/, value] of Object.entries(countsObject as unknown as { [key: string]: CountsData })) {

                //Airframe name is 'Garmin Flight Display', skip
                if (value.airframeName === "Garmin Flight Display")
                    continue;

                //Current airframe name is neither the selected airframe name or 'All Airframes', skip
                if ((selectedAirframe !== value.airframeName) && (selectedAirframe !== "All Airframes"))
                    continue;

                value.name = `${value.eventName  } - ${  value.airframeName}`;
                value.x = value.dates;
                value.type = 'scatter';
                value.hoverinfo = 'x+text';

                //Event name is 'ANY Event'
                if (eventName === "ANY Event") {

                    value = {
                        ...value,

                        legendgroup: value.name,

                        //Dashed lines for 'ANY Event'
                        line: {
                            dash: 'dot'
                        }

                    };

                //Event is NOT 'ANY Event'
                } else {

                    value = {
                        ...value,

                        legendgroup: value.name,
                        mode: 'lines',

                        //Standard lines for non-'ANY Event'
                        line: {
                            width: 2,
                        }

                    };

                }

                //Only add airframes to the count plot that the fleet has
                if (airframes.indexOf(value.airframeName) >= 0) {

                    //Display the "ANY Event" lines under the other ones
                    if (eventName === "ANY Event") {
                        this.state.countData.push(value as Plotly.Data);
                    } else {
                        this.state.countData.unshift(value as Plotly.Data);
                    }

                }

                if (this.state.aggregatePage) {
                    value.y = value.aggregateTotalEventsCounts.map(Number);
                } else {
                    value.y = value.totalEventsCounts.map(Number);
                }
                value.hovertext = [];

                for (let i = 0; i < value.dates.length; i++) {

                    const date:string = value.dates[i];

                    //Only add airframes to the fleet percentage plot that the fleet has
                    if (airframes.indexOf(value.airframeName) >= 0 && !this.state.aggregatePage) {

                        if (date in fleetPercents.flightsWithEventCounts) {
                            fleetPercents.flightsWithEventCounts[date] += value.flightsWithEventCounts[i];
                            fleetPercents.totalFlightsCounts[date] += value.totalFlightsCounts[i];
                        } else {
                            fleetPercents.flightsWithEventCounts[date] = value.flightsWithEventCounts[i];
                            fleetPercents.totalFlightsCounts[date] = value.totalFlightsCounts[i];
                        }

                    }

                    if (date in ngafidPercents.flightsWithEventCounts) {
                        ngafidPercents.flightsWithEventCounts[date] += Number(value.aggregateFlightsWithEventCounts[i]);
                        ngafidPercents.totalFlightsCounts[date] += Number(value.aggregateTotalFlightsCounts[i]);
                    } else {
                        ngafidPercents.flightsWithEventCounts[date] = Number(value.aggregateFlightsWithEventCounts[i]);
                        ngafidPercents.totalFlightsCounts[date] = Number(value.aggregateTotalFlightsCounts[i]);
                    }
                }

                for (let i = 0; i < value.dates.length; i++) {
                    
                    let flightsWithEventCount;
                    let totalFlightsCount;
                    if (this.state.aggregatePage) {
                        flightsWithEventCount = value.aggregateFlightsWithEventCounts[i];
                        totalFlightsCount = value.aggregateTotalFlightsCounts[i];
                    } else {
                        flightsWithEventCount = value.flightsWithEventCounts[i];
                        totalFlightsCount = value.totalFlightsCounts[i];
                    }

                    value.hovertext.push(`${value.y[i]  } events in ${  flightsWithEventCount  } of ${  totalFlightsCount  } flights : ${  value.eventName  } - ${  value.airframeName}`);

                }

            }

        }

        for (const airframeName of airframeNames) {

            const airframeLegendHighlight = {

                name: airframeName,

                x: [0],
                y: [0],

                //visible: 'legendonly',
                visible: false,
                showlegend: true,

                mode: 'markers',
                marker: {
                    width: 2.0,
                    opacity: 1.0,
                }

            };

            this.state.countData.push(airframeLegendHighlight);

        }

        for (const [eventName, fleetValueOrig] of Object.entries(newEventFleetPercents)) {

            console.log("Event Name: ", eventName);

            let fleetValue = fleetValueOrig;

            //FLEET VALUE -- Event name is 'ANY Event'
            if (eventName === "ANY Event") {

                fleetValue = {
                    ...fleetValue,

                    legendgroup: fleetValue.name,

                    //Dashed lines for 'ANY Event'
                    line: {
                        dash: 'dot'
                    }

                };

            //FLEET VALUE -- Event is NOT 'ANY Event'
            } else {

                fleetValue = {
                    ...fleetValue,

                    legendgroup: fleetValue.name,
                    mode: 'lines',

                    //Standard lines for non-'ANY Event'
                    line: {
                        width: 2,
                    }

                };

            }

            //Push fleet values...
            if (!this.state.aggregatePage) {
                this.state.percentData.push(fleetValue as Plotly.Data);
                fleetValue.x = [];
                fleetValue.y = [];

                for (const date of Object.keys(fleetValue.flightsWithEventCounts).sort()) {
                    fleetValue.x.push(date);

                    let v = 100.0 * (fleetValue.flightsWithEventCounts[date] / fleetValue.totalFlightsCounts[date]);
                    if (isNaN(v)) v = 0.0;
                    fleetValue.y.push(v);

                    // console.log(date + " :: " + fleetValue.flightsWithEventCounts[date]  + " / " + fleetValue.totalFlightsCounts[date] + " : " + v);

                    //this will give 2 significant figures (and leading 0s if it is quite small)
                    let fixedText = "";
                    if (v > 0 && v < 1) {
                        //console.log("Log10 of y is " + Math.log10(v);
                        fixedText = `${v.toFixed(-Math.ceil(Math.log10(v)) + 2)  }%`;
                    } else {
                        fixedText = `${v.toFixed(2)  }%`;
                    }
                    fleetValue.hovertext.push(`${fixedText  } (${  fleetValue.flightsWithEventCounts[date]  } of ${  fleetValue.totalFlightsCounts[date]  } flights) : ${  fleetValue.name}`);
                }
            }

            //Push NGAFID data...
            let ngafidValue = newEventNGAFIDPercents[eventName];


            //NGAFID VALUE -- Event name is 'ANY Event'
            if (eventName === "ANY Event") {

                //  console.log("[EX] NAME (AE): ", ngafidValue.name);

                ngafidValue = {
                    ...ngafidValue,

                    legendgroup: ngafidValue.name,

                    //Dashed lines for 'ANY Event'
                    line: {
                        dash: 'dot'
                    }

                };

                //NGAFID VALUE -- Event is NOT 'ANY Event'
            } else {

                //  console.log("[EX] NAME: ", ngafidValue.name);

                ngafidValue = {
                    ...ngafidValue,

                    legendgroup: ngafidValue.name,
                    mode: 'lines',

                    //Standard lines for non-'ANY Event'
                    line: {
                        width: 2,
                    }

                };

            }

            this.state.percentData.push(ngafidValue as Plotly.Data);
            ngafidValue.x = [];
            ngafidValue.y = [];
            for (const date of Object.keys(ngafidValue.flightsWithEventCounts).sort()) {
                ngafidValue.x.push(date);

                let v = 100.0 * ngafidValue.flightsWithEventCounts[date] / ngafidValue.totalFlightsCounts[date];
                if (isNaN(v)) v = 0.0;

                ngafidValue.y.push(v);


                //console.log(date + " :: " + ngafidValue.flightsWithEventCounts[date]  + " / " + ngafidValue.totalFlightsCounts[date] + " : " + v);

                //this will give 2 significant figures (and leading 0s if it is quite small)
                let fixedText = "";
                if (v > 0 && v < 1) {
                    //console.log("Log10 of y is " + Math.log10(v);
                    fixedText = `${v.toFixed(-Math.ceil(Math.log10(v)) + 2)  }%`;
                } else {
                    fixedText = `${v.toFixed(2)  }%`;
                }
                ngafidValue.hovertext.push(`${fixedText  } (${  ngafidValue.flightsWithEventCounts[date]  } of ${  ngafidValue.totalFlightsCounts[date]  } flights) : ${  ngafidValue.name}`);
            }

        }

        /*
            console.log("percentData:");
            console.log(percentData);
        */

        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        const countLayout = {
            title: {text: 'Event Counts Over Time'},
            hovermode: "x unified" as const,
            autosize: true,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            },
            legend: {
                traceorder: "normal"
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
        } as Plotly.Layout;

        const percentLayout = {
            title: {text: 'Percentage of Flights With Event Over Time'},
            hovermode: "x unified",
            autosize: true,
            margin: {
                l: 50,
                r: 50,
                b: 50,
                t: 50,
                pad: 4
            },
            legend: {
                traceorder: "normal"
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
        } as Plotly.Layout;

        const config = {responsive: true};

        /*
        console.log("countData, percentData:");
        console.log(countData);
        console.log(percentData);
        */

        Plotly.newPlot('count-trends-plot', this.state.countData, countLayout, config);
        Plotly.newPlot('percent-trends-plot', this.state.percentData, percentLayout, config);

        console.log("Hiding loading spinner");
        $('#loading').hide();

    }


    checkEvent(eventName: string) {

        console.log("Checking event: '", eventName, "'");
        this.setState(prevState => {
            const eventChecked = { ...prevState.eventChecked };
            eventChecked[eventName] = !eventChecked[eventName];
            return { eventChecked };
        }, () => {
            this.displayPlots(this.state.airframe);
        });

    }

    updateStartYear(newStartYear: number) {

        console.log("Setting new start year to: ", newStartYear);
        this.setState({startYear: newStartYear, datesChanged: true});
        console.log(this.state);

    }

    updateStartMonth(newStartMonth: number) {

        console.log("Setting new start month to: ", newStartMonth);
        this.setState({startMonth: newStartMonth, datesChanged: true});
        console.log(this.state);

    }

    updateEndYear(newEndYear: number) {

        console.log("Setting new end year to: ", newEndYear);
        this.setState({endYear: newEndYear, datesChanged: true});
        console.log(this.state);

    }

    updateEndMonth(newEndMonth: number) {

        console.log("Setting new end month to: ", newEndMonth);
        this.setState({endMonth: newEndMonth, datesChanged: true});
        console.log(this.state);

    }

    dateChange() {

        console.log(`[trendscard] notifying date change 2, startYear: '${  this.state.startYear  }', startMonth: '${  this.state.startMonth  }, endYear: '${  this.state.endYear  }', endMonth: '${  this.state.endMonth  }'`);

        const newEventChecked = { ...this.state.eventChecked };
        for (const [eventName] of Object.entries(newEventChecked)) {
            newEventChecked[eventName] = false;
        }
        this.setState({
            eventChecked: newEventChecked,
            datesChanged: false
        });
        
        $('#loading').hide();

        this.fetchMonthlyEventCounts().then((data) => {

            /*
                Set all events to empty initially.
                
                Create a copy of eventsEmpty to avoid direct state mutation.
            */
            const updatedEventsEmpty = { ...this.state.eventsEmpty };
            for (let i = 0; i < eventNames.length; i++) {
                const eventNameCur = eventNames[i];
                updatedEventsEmpty[eventNameCur] = true;
            }

            const dataTyped = data as { [eventName: string]: { [airframeName: string]: TrendsData } };
            for (const [eventName] of Object.entries(dataTyped)) {
                updatedEventsEmpty[eventName] = false;
            }

            //Has at least one event data, mark "ANY Event" as non-empty
            if (Object.keys(dataTyped).length > 0)
                updatedEventsEmpty["ANY Event"] = false;

            this.setState({ eventsEmpty: updatedEventsEmpty }, () => {
                this.displayPlots(this.state.airframe);
            });

        });

    }

    airframeChange(airframe: string) {
        this.setState({airframe});
        this.displayPlots(airframe);
    }

    render() {

        const activePageName = (this.state.aggregatePage ? "aggregate_trends" : "trends");
        const timeHeaderTitle = (this.state.aggregatePage ? "Event Trends (Aggregate)" : "Event Trends");

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar activePage={activePageName} darkModeOnClickAlt={() => {
                        this.displayPlots(this.state.airframe);
                    }} waitingUserCount={waitingUserCount} fleetManager={fleetManager}
                                    unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess}
                                    plotMapHidden={plotMapHidden}/>
                </div>

                <div className="container-fluid" style={{overflowY: "auto", flex: "1 1 auto"}}>

                    <div className="row">
                        <div className="col-lg-12" style={{paddingBottom: "64"}}>
                            <div className="card m-2">
                                <TimeHeader
                                    name={timeHeaderTitle}
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
                                    exportCSV={() => this.exportCSV()}
                                />
                                <div className="card-body" style={{padding: "0"}}>
                                    <div className="row" style={{margin: "0", display: "flex", height: "100%"}}>
                                        <div className="col-lg-2" style={{padding: "8 8 8 8"}}>

                                            {
                                                eventNames.map((eventName, index) => {

                                                    //Don't show a description for the "ANY Event" event
                                                    if (eventName === "ANY Event") return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input"
                                                                   disabled={this.state.eventsEmpty[eventName]}
                                                                   type="checkbox" value="" id={`event-check-${  index}`}
                                                                   checked={this.state.eventChecked[eventName]}
                                                                   onChange={() => this.checkEvent(eventName)}></input>
                                                            <label className="form-check-label">
                                                                {eventName}
                                                            </label>
                                                        </div>
                                                    );

                                                    return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input"
                                                                   disabled={this.state.eventsEmpty[eventName]}
                                                                   type="checkbox"
                                                                   value=""
                                                                   id={`event-check-${  index}`}
                                                                   checked={this.state.eventChecked[eventName]}
                                                                   onChange={() => this.checkEvent(eventName)}>
                                                            </input>
                                                            <OverlayTrigger overlay={(props) => (
                                                                <Tooltip {...props}>{GetDescription(eventName)}</Tooltip>)}
                                                                            placement="bottom">
                                                                <label className="form-check-label">
                                                                    {eventName}
                                                                </label>
                                                            </OverlayTrigger>

                                                        </div>
                                                    );
                                                })
                                            }

                                        </div>

                                        <div className="col-lg-10" style={{
                                            padding: "0 0 0 8",
                                            opacity: "0.80",
                                            display: "flex",
                                            flexDirection: "column",
                                            minHeight: "85vh",
                                            flex: "1 1 auto"
                                        }}>
                                            <div id="count-trends-plot" className="flex-fill" style={{
                                                flex: "1 1 auto",
                                                minHeight: "0",
                                                height: "100%",
                                                width: "100%"
                                            }}></div>
                                            <hr style={{margin: "0", borderTop: "8px solid var(--c_card_bg)"}}></hr>
                                            <div id="percent-trends-plot" className="flex-fill" style={{
                                                flex: "1 1 auto",
                                                minHeight: "0",
                                                height: "100%",
                                                width: "100%"
                                            }}></div>
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


export default TrendsPage;