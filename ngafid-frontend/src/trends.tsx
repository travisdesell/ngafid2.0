import 'bootstrap';

import React, { useCallback, useEffect, useState } from "react";

import { showErrorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import { TimeHeader } from "./time_header.js";
import GetDescription from "./get_description.js";

import Plotly from 'plotly.js';
import Tooltip from "react-bootstrap/Tooltip";
import { OverlayTrigger } from "react-bootstrap";
import type { AirframeNameID } from "./types";


const allAirframes = { name: "All Airframes", id: -1 } as AirframeNameID;
airframes.unshift(allAirframes);
const garminIndex = airframes.findIndex(af => af.name === "Garmin Flight Display");
if (garminIndex !== -1)
    airframes.splice(garminIndex, 1);


const AIRFRAME_NAMES_SKIP = ['Garmin Flight Display'];


//Sort incoming event names
eventNames.sort();


export type CSVValues = {
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
    aggregate_page: boolean;
};


export function TrendsPage({ aggregate_page }: TrendsPageProps) {

    // Initialize eventChecked and eventsEmpty
    const initialEventChecked: { [key: string]: boolean } = {};
    const initialEventsEmpty: { [key: string]: boolean } = {};

    if (!eventNames.includes("ANY Event")) {
        eventNames.unshift("ANY Event");
    }
    for (let i = 0; i < eventNames.length; i++) {
        const eventNameCur = eventNames[i];
        initialEventChecked[eventNameCur] = false;
        initialEventsEmpty[eventNameCur] = true;
    }
    initialEventsEmpty["ANY Event"] = true;

    const date = new Date();

    // State hooks
    const [eventCounts, setEventCounts] = useState<{ [eventName: string]: { [airframeName: string]: TrendsData } }>({});
    // const [countData, setCountData] = useState<Plotly.Data[]>([]);
    // const [percentData, setPercentData] = useState<Plotly.Data[]>([]);
    // const [eventFleetPercents, setEventFleetPercents] = useState<{ [key: string]: CountsData }>({});
    // const [eventNGAFIDPercents, setEventNGAFIDPercents] = useState<{ [key: string]: CountsData }>({});
    const [airframe, setAirframe] = useState<AirframeNameID>(allAirframes);
    const [startYear, setStartYear] = useState<number>(2020);
    const [startMonth, setStartMonth] = useState<number>(1);
    const [endYear, setEndYear] = useState<number>(date.getFullYear());
    const [endMonth, setEndMonth] = useState<number>(date.getMonth() + 1);
    const [datesChanged, setDatesChanged] = useState<boolean>(false);
    const [aggregatePage] = useState<boolean>(aggregate_page);
    const [eventChecked, setEventChecked] = useState<{ [key: string]: boolean }>(initialEventChecked);
    const [eventsEmpty, setEventsEmpty] = useState<{ [key: string]: boolean }>(initialEventsEmpty);
    const [datesOrAirframeChanged, setDatesOrAirframeChanged] = useState<boolean>(false);

    //Effect to update datesOrAirframeChanged when dependencies change
    useEffect(() => {
        setDatesOrAirframeChanged(true);
    }, [startYear, startMonth, endYear, endMonth, airframe]);


    useEffect(() => {

        //Fetch the monthly event counts
        fetchMonthlyEventCounts();

        //Display the plots for the initial state
        displayPlots("All Airframes");
        
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const startDate = () => {
        let startDate = `${startYear}-`;

        const startMonthNumeric = Number(startMonth);

        //Start month is less than 10, add leading zero
        if (startMonthNumeric < 10)
            startDate += `0${startMonthNumeric}`;

        //Otherwise, just append the month
        else
            startDate += startMonth;

        return startDate;
    };

    const endDate = () => {
        let endDate = `${endYear}-`;

        const endMonthNumeric = Number(endMonth);

        //End month is less than 10, add leading zero
        if (endMonthNumeric < 10)
            endDate += `0${endMonthNumeric}`;

        //Otherwise, just append the month
        else
            endDate += endMonth;

        return endDate;
    };

    const fetchMonthlyEventCounts = () => {
        const submissionData = {
            startDate: `${startDate()}-01`,
            endDate: `${endDate()}-28`,
            aggregatePage: aggregatePage
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

                    setEventCounts({
                        ...responseObj,
                        ["ANY Event"]: countsMerged
                    });

                    resolve(response);
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    showErrorModal("Error Loading Uploads", errorThrown);
                    reject(errorThrown);
                },
            });
        });

    };

    const exportCSV = () => {

        const selectedAirframe = airframe;

        const eventNames: string[] = [];
        const airframeNames: string[] = [];
        const dates: string[] = [];
        const csvValues: CSVValues = {};

        for (const [eventName, countsObject] of Object.entries(eventCounts)) {

            //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
            if (!eventChecked[eventName])
                continue;

            //Ensure the eventNames array contains unique names only
            if (!eventNames.includes(eventName))
                eventNames.push(eventName);

            for (const [/*...*/, value] of Object.entries(countsObject as unknown as { [key: string]: CountsData })) {

                //Airframe is Garmin Flight Display, skip
                if (airframe.name === "Garmin Flight Display")
                    continue;

                //Airframe is not selected, skip
                if (selectedAirframe.name !== value.airframeName && selectedAirframe.name !== "All Airframes")
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
                    if (aggregatePage) {
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
                    console.log(`\t${eventName} - ${airframeName} has ${Object.keys(csvValues[eventName][airframeName]).length} entries!`);
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
                    filetext += `,${eventName}`;
                    filetext += `,${eventName}`;
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
                    filetext += `,${airframeName}`;
                    filetext += `,${airframeName}`;
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
                            filetext += `,${csvValues[eventName][airframeName][date].flightsWithEventCount}`;
                            filetext += `,${csvValues[eventName][airframeName][date].totalFlights}`;
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
        element.setAttribute('href', `data:text/plain;charset=utf-8,${encodeURIComponent(filetext)}`);
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    };


    const displayPlots = useCallback((selectedAirframe: string) => {

        const countTraces: Plotly.Data[] = [];
        const percentTraces: Plotly.Data[] = [];

        //Aggregators for percent chart
        type Aggregator = {
            name: string;
            flightsWithEventCounts: Record<string, number>;
            totalFlightsCounts: Record<string, number>;
            style: { line?: { width?: number; dash?: string }, mode?: 'lines' };
        };

        const fleetAgg: Record<string, Aggregator> = {};   // eventName -> your fleet (non-aggregate only)
        const ngafidAgg: Record<string, Aggregator> = {};  // eventName -> all fleets / other fleets

        const counts = eventCounts ?? {};
        const fleetAirframeNames = new Set(airframes.map(a => a.name));

        //Build count traces, fill aggregators for percent data
        for (const [eventName, countsObject] of Object.entries(counts)) {

            //Event is unchecked, skip
            if (!eventChecked[eventName])
                continue;

            //Style per event
            const isAnyEvent = (eventName === 'ANY Event');
            const line = isAnyEvent
                ? { dash: 'dot' as Plotly.Dash, width: 2 }
                : { width: 2 };
            const mode = isAnyEvent
                ? undefined
                : 'lines';

            //Initialize percent aggregators for this event
            const ngafidName = `${eventName} - ${aggregatePage ? 'All Fleets' : 'All Other Fleets'}`;

            //NGAFID aggregate data doesn't exist yet, create it
            if (!ngafidAgg[eventName]) {

                ngafidAgg[eventName] = {
                    name: ngafidName,
                    flightsWithEventCounts: {},
                    totalFlightsCounts: {},
                    style: { line, mode },
                };

            }

            //Fleet aggregate data doesn't exist yet, create it
            if (!aggregatePage && !fleetAgg[eventName]) {

                fleetAgg[eventName] = {
                    name: `${eventName} - Your Fleet`,
                    flightsWithEventCounts: {},
                    totalFlightsCounts: {},
                    style: { line, mode },
                };

            }

            //Iterate airframes within the event
            for (const value of Object.values(countsObject as unknown as { [k: string]: TrendsData })) {

                //Got skippable airframe, skip
                if (AIRFRAME_NAMES_SKIP.includes(value.airframeName))
                    continue;

                //Current airframe isn't selected, skip
                if (selectedAirframe !== 'All Airframes' && selectedAirframe !== value.airframeName)
                    continue;

                //Create a fresh count series
                const yCounts = (aggregatePage ? value.aggregateTotalEventsCounts : value.totalEventsCounts).map(Number);
                const series: Plotly.Data = {
                    name: `${value.eventName} - ${value.airframeName}`,
                    type: 'scatter',
                    hoverinfo: 'x+text',
                    legendgroup: `${value.eventName} - ${value.airframeName}`,
                    line,
                    mode,
                    x: value.dates.slice(),
                    y: yCounts,
                    hovertext: value.dates.map((d, i) => {
                        const flightsWithEvent = (aggregatePage ? value.aggregateFlightsWithEventCounts[i] : value.flightsWithEventCounts[i]);
                        const totalFlights = (aggregatePage ? value.aggregateTotalFlightsCounts[i] : value.totalFlightsCounts[i]);
                        return `${yCounts[i]} events in ${flightsWithEvent} of ${totalFlights} flights : ${value.eventName} - ${value.airframeName}`;
                    }),
                };

                //Airframe name is in fleet, add to count traces
                if (fleetAirframeNames.has(value.airframeName)) {

                    if (isAnyEvent)
                        countTraces.push(series);
                    else
                        countTraces.unshift(series);

                }

                //Fill aggregators for percent data
                for (let i = 0; i < value.dates.length; i++) {

                    const date = value.dates[i];

                    //Fleet-only line (non-aggregate page)
                    if (!aggregatePage && fleetAirframeNames.has(value.airframeName)) {
                        const f = fleetAgg[eventName]!;
                        f.flightsWithEventCounts[date] = (f.flightsWithEventCounts[date] ?? 0) + value.flightsWithEventCounts[i];
                        f.totalFlightsCounts[date] = (f.totalFlightsCounts[date] ?? 0) + value.totalFlightsCounts[i];
                    }

                    //NGAFID line (uses aggregate totals)
                    const g = ngafidAgg[eventName]!;
                    g.flightsWithEventCounts[date] = (g.flightsWithEventCounts[date] ?? 0) + Number(value.aggregateFlightsWithEventCounts[i]);
                    g.totalFlightsCounts[date] = (g.totalFlightsCounts[date] ?? 0) + Number(value.aggregateTotalFlightsCounts[i]);

                }

            }

        }

        //Add legend highlight markers for each airframe (count chart)
        const airframeNames: string[] = [];
        for (const countsObject of Object.values(counts)) {
            for (const v of Object.values(countsObject as { [k: string]: TrendsData })) {

                //Airframe not in airframe names list, add it
                if (!airframeNames.includes(v.airframeName))
                    airframeNames.push(v.airframeName);

            }
        }

        //Add a count trace for each airframe
        for (const airframeName of airframeNames) {
            countTraces.push({
                name: airframeName,
                x: [0],
                y: [0],
                visible: false,
                showlegend: true,
                mode: 'markers',
                marker: { width: 2.0, opacity: 1.0 },
                type: 'scatter',
            } as Plotly.Data);
        }

        //Build percent traces from aggregators
        const buildPercentSeries = (agg: Aggregator) => {

            const dates = Object.keys(agg.flightsWithEventCounts).sort();
            const x: string[] = [];
            const y: number[] = [];
            const hovertext: string[] = [];

            for (const d of dates) {
                const fw = agg.flightsWithEventCounts[d] ?? 0;
                const tf = agg.totalFlightsCounts[d] ?? 0;
                let v = 100 * (tf ? fw / tf : 0);
                if (isNaN(v))
                    v = 0;

                x.push(d);
                y.push(v);

                let fixed = '';
                if (v > 0 && v < 1)
                    fixed = `${v.toFixed(-Math.ceil(Math.log10(v)) + 2)}%`;
                else
                    fixed = `${v.toFixed(2)}%`;

                hovertext.push(`${fixed} (${fw} of ${tf} flights) : ${agg.name}`);
            }

            const style = { ...agg.style };

            //Plotly Dash is a string, cast it
            if (style.line && style.line.dash)
                style.line.dash = style.line.dash as Plotly.Dash;
            
            const trace: Plotly.Data = {
                name: agg.name,
                type: 'scatter',
                hoverinfo: 'x+text',
                legendgroup: agg.name,
                ...(style.line ? { line: { ...style.line, dash: style.line.dash as Plotly.Dash } } : {}),
                ...(style.mode ? { mode: style.mode } : {}),
                x,
                y,
                hovertext,
            };
            return trace;
        };

        //Non-aggregate page, build percent traces for each event
        if (!aggregatePage) {
            for (const ev of Object.keys(fleetAgg)) {
                percentTraces.push(buildPercentSeries(fleetAgg[ev]));
            }
        }

        for (const ev of Object.keys(ngafidAgg)) {
            percentTraces.push(buildPercentSeries(ngafidAgg[ev]));
        }

        
        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        const countLayout: Plotly.Layout = {
            title: { text: 'Event Counts Over Time' },
            hovermode: "x unified",
            autosize: true,
            margin: { l: 50, r: 50, b: 50, t: 50, pad: 4 },
            legend: { traceorder: "normal" },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: { color: plotTextColor },
            xaxis: { gridcolor: plotGridColor },
            yaxis: { gridcolor: plotGridColor },
        } as Plotly.Layout;

        const percentLayout: Plotly.Layout = {
            title: { text: 'Percentage of Flights With Event Over Time' },
            hovermode: "x unified",
            autosize: true,
            margin: { l: 50, r: 50, b: 50, t: 50, pad: 4 },
            legend: { traceorder: "normal" },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: { color: plotTextColor },
            xaxis: { gridcolor: plotGridColor },
            yaxis: { gridcolor: plotGridColor },
        } as Plotly.Layout;

        const config = { responsive: true };

        Plotly.newPlot('count-trends-plot', countTraces, countLayout, config);
        Plotly.newPlot('percent-trends-plot', percentTraces, percentLayout, config);

        $('#loading').hide();

    }, [eventCounts, eventChecked, aggregatePage]);



    useEffect(() => {
        displayPlots(airframe.name);

        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [airframe.name, eventCounts, eventChecked, aggregatePage]);

    const checkEvent = (eventName: string) => {

        console.log("Checking event: '", eventName, "'");
        setEventChecked(prevEventChecked => {
            const newEventChecked = { ...prevEventChecked };
            newEventChecked[eventName] = !newEventChecked[eventName];
            return newEventChecked;
        });

    };

    const updateStartYear = (newStartYear: number) => {

        console.log("Setting new start year to: ", newStartYear);
        setStartYear(newStartYear);
        setDatesChanged(true);

    };

    const updateStartMonth = (newStartMonth: number) => {

        console.log("Setting new start month to: ", newStartMonth);
        setStartMonth(newStartMonth);
        setDatesChanged(true);

    };

    const updateEndYear = (newEndYear: number) => {

        console.log("Setting new end year to: ", newEndYear);
        setEndYear(newEndYear);
        setDatesChanged(true);

    };

    const updateEndMonth = (newEndMonth: number) => {

        console.log("Setting new end month to: ", newEndMonth);
        setEndMonth(newEndMonth);
        setDatesChanged(true);

    };

    const dateChange = () => {

        //Clear Time Header update flag
        setDatesOrAirframeChanged(false);

        console.log(`[trendscard] notifying date change 2, startYear: '${startYear}', startMonth: '${startMonth}, endYear: '${endYear}', endMonth: '${endMonth}'`);

        const newEventChecked = { ...eventChecked };
        for (const [eventName] of Object.entries(newEventChecked)) {
            newEventChecked[eventName] = false;
        }
        setEventChecked(newEventChecked);
        setDatesChanged(false);

        $('#loading').hide();

        fetchMonthlyEventCounts().then((data) => {

            /*
                Set all events to empty initially.
                
                Create a copy of eventsEmpty to avoid direct state mutation.
            */
            const updatedEventsEmpty = { ...eventsEmpty };
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

            setEventsEmpty(updatedEventsEmpty);

        });

    };

    const airframeChangeFromName = (airframeName: string) => {

        //Find airframe data in list corresponding to the name
        const airframe = airframes.find(a => a.name === airframeName);

        //Got an airframe from the name, change the state
        if (airframe)
            airframeChange(airframe);

    };
    
    const airframeChange = (airframe: AirframeNameID) => {
        setAirframe(airframe);
    };

    const render = () => {

        const activePageName = (aggregatePage ? "aggregate_trends" : "trends");
        const timeHeaderTitle = (aggregatePage ? "Event Trends (Aggregate)" : "Event Trends");

        return (
            <div style={{ overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh" }}>

                <div style={{ flex: "0 0 auto" }}>
                    <SignedInNavbar
                        activePage={activePageName}
                        waitingUserCount={waitingUserCount}
                        fleetManager={fleetManager}
                        unconfirmedTailsCount={unconfirmedTailsCount}
                        modifyTailsAccess={modifyTailsAccess}
                        plotMapHidden={plotMapHidden}
                    />
                </div>

                <div className="container-fluid" style={{ overflowY: "auto", flex: "1 1 auto" }}>

                    <div className="row">
                        <div className="col-lg-12" style={{ paddingBottom: "64" }}>
                            <div className="card m-2">
                                <TimeHeader
                                    name={timeHeaderTitle}
                                    airframes={airframes.map((airframe: AirframeNameID) => airframe.name)}
                                    airframe={airframe.name}
                                    startYear={startYear}
                                    startMonth={startMonth}
                                    endYear={endYear}
                                    endMonth={endMonth}
                                    datesChanged={datesChanged}
                                    dateChange={dateChange}
                                    airframeChange={(airframe: string) => airframeChangeFromName(airframe)} 
                                    updateStartYear={updateStartYear}
                                    updateStartMonth={updateStartMonth}
                                    updateEndYear={updateEndYear}
                                    updateEndMonth={updateEndMonth}
                                    exportCSV={exportCSV}
                                    datesOrAirframeChanged={datesOrAirframeChanged}
                                    requireManualInitialUpdate
                                />
                                <div className="card-body" style={{ padding: "0" }}>
                                    <div className="row" style={{ margin: "0", display: "flex", height: "100%" }}>
                                        <div className="col-lg-2" style={{ padding: "8 8 8 8" }}>

                                            {
                                                eventNames.map((eventName, index) => {

                                                    //Don't show a description for the "ANY Event" event
                                                    if (eventName === "ANY Event") return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input"
                                                                disabled={eventsEmpty[eventName]}
                                                                type="checkbox" value="" id={`event-check-${index}`}
                                                                checked={eventChecked[eventName]}
                                                                onChange={() => checkEvent(eventName)}></input>
                                                            <label className="form-check-label">
                                                                {eventName}
                                                            </label>
                                                        </div>
                                                    );

                                                    return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input"
                                                                disabled={eventsEmpty[eventName]}
                                                                type="checkbox"
                                                                value=""
                                                                id={`event-check-${index}`}
                                                                checked={eventChecked[eventName]}
                                                                onChange={() => checkEvent(eventName)}>
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
                                            <hr style={{ margin: "0", borderTop: "8px solid var(--c_card_bg)" }}></hr>
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
    };

    return render();

};