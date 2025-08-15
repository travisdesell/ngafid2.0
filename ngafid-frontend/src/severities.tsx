import 'bootstrap';

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { createRoot } from "react-dom/client";

import {showErrorModal} from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import {TimeHeader} from "./time_header.js";
import GetDescription from "./get_description.js";

import Plotly from 'plotly.js';
import {OverlayTrigger} from "react-bootstrap";
import Tooltip from "react-bootstrap/Tooltip";


import "./index.css";
import type { AirframeNameID } from "./types";
import { buildEndDate, buildStartDate } from './summary_page';


const allAirframes = { name: "All Airframes", id: -1 } as AirframeNameID;


tagNames.unshift("All Tags");
const tagIndex = tagNames.indexOf("Garmin Flight Display");
if (tagIndex !== -1)
    tagNames.splice(tagIndex, 1);


eventNames.sort();
console.log(eventNames);



type EventMetaDataItem = {
  name: string;
  value: number | string;
};

type EventCount = {
    id: number;
    severity: number;
    startTime: string;
    endTime: string;
    startLine?: number;
    endLine?: number;
    systemId: number | string;
    tail: string;
    eventDefinitionId: number;
    tagName: string;
    flightId: number | string;
    otherFlightId?: number | string;
};


type EventSeverityByAirframe = Record<string, EventCount[]>;
type EventSeverities = Record<string, EventSeverityByAirframe>;


const eventSeverities: EventSeverities = {};


export function SeveritiesPage() {

    const airframesForUI = useMemo(() => {

        //Remove GFD
        const list = airframes.filter(a => a.name !== "Garmin Flight Display");

        //All Airframes not present, add it
        return list.some(a => a.name === "All Airframes" || a.id === -1)
            ? list
            : [allAirframes, ...list];

    }, []);


    const iniitalEventFlags = useMemo(() => {
        const checked: Record<string, boolean> = {};
        const empty:   Record<string, boolean> = {};
        for (const name of eventNames) {
            checked[name] = false;
            empty[name]   = true;
        }
        empty["ANY Event"] = true;
        return { checked, empty };
    }, []);


    const [airframe, setAirframe] = useState<AirframeNameID>(allAirframes);
    const [tagName, setTagName] = useState("All Tags");
    const [startYear, setStartYear] = useState(2020);
    const [startMonth, setStartMonth] = useState(1);
    const [endYear, setEndYear] = useState(new Date().getFullYear());
    const [endMonth, setEndMonth] = useState(new Date().getMonth() + 1);
    const [datesChanged, setDatesChanged] = useState(false);
    const [eventMetaData, setEventMetaData] = useState<Record<number, EventMetaDataItem[]>>({});
    const [eventChecked, setEventChecked] = useState<{ [key: string]: boolean }>(iniitalEventFlags.checked);
    const [eventsEmpty, setEventsEmpty] = useState<{ [key: string]: boolean }>(iniitalEventFlags.empty);
    const [eventSeveritiesState, setEventSeveritiesState] = useState<EventSeverities>({});
    const [datesOrAirframeChanged, setDatesOrAirframeChanged] = useState<boolean>(false);


    //Effect to update datesOrAirframeChanged when dependencies change
    useEffect(() => {
        setDatesOrAirframeChanged(true);
    }, [startYear, startMonth, endYear, endMonth, tagName]);
    
    useEffect(() => {

        displayPlot(airframe.name);

    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [airframe.name, eventChecked, eventMetaData, eventSeveritiesState]);


    const exportCSV = () => {

        const selectedAirframe = airframe;

        console.log(`selected airframe: '${  selectedAirframe  }'`);
        console.log(eventSeverities);
        const fileHeaders = "Event Name,Airframe,Flight ID,Start Time,End Time,Start Line,End Line,Severity";

        const fileContent = [fileHeaders];
        const uniqueMetaDataNames: string[] = [];

        for (const [eventName, countsMap] of Object.entries(eventSeverities)) {

            //Event is unchecked, skip
            if (!eventChecked[eventName])
                continue;

            for (const [airframeName, counts] of Object.entries(countsMap)) {

                //Airframe is Garmin Flight Display, skip
                if (airframeName === "Garmin Flight Display")
                    continue;

                //Airframe is not selected, skip
                if (selectedAirframe.name !== airframeName && selectedAirframe.name !== "All Airframes")
                    continue;

                console.log("Counts severities:", counts);

                for (let i = 0; i < counts.length; i++) {

                    let line = "";
                    const metaData: EventMetaDataItem[] = eventMetaData[counts[i].id];
                    console.log(metaData);
                    const eventMetaDataText:(string|number)[] = [];
                    if (metaData != null) {
                        metaData.map((item:EventMetaDataItem) => {

                            //...
                            if (!uniqueMetaDataNames.includes(item.name))
                                uniqueMetaDataNames.push(item.name);

                            eventMetaDataText.push(
                                typeof item.value === "number"
                                    ? `${item.name}: ${(Math.round(item.value * 100) / 100).toFixed(2)}`
                                    : `${item.name}: ${item.value}`
                            );
                        });

                    }
                    const count = counts[i];
                    line = `${eventName  },${  airframeName  },${  count.flightId  },${  count.startTime  },${  count.endTime  },${  count.startLine  },${  count.endLine  },${  count.severity}`;

                    //...
                    if (eventMetaDataText.length !== 0)
                        line += `,${  eventMetaDataText.join(",")}`;

                    fileContent.push(line);

                }

            }

        }

        if (uniqueMetaDataNames.length != 0)
            fileContent[0] = `${fileHeaders},${uniqueMetaDataNames.join(",")}`;

        const filename = "event_severities.csv";
        console.log("Exporting CSV!");

        const element = document.createElement('a');
        element.setAttribute('href', `data:text/plain;charset=utf-8,${  encodeURIComponent(fileContent.join("\n"))}`);
        element.setAttribute('download', filename);

        element.style.display = 'none';
        document.body.appendChild(element);

        element.click();

        document.body.removeChild(element);

    };

    const getEventMetaData = (eventId:number) => {

        let eventMetaData: EventMetaDataItem[] | null = null;
        $.ajax({
            type: 'GET',
            url: `/api/event/${eventId}/meta`,
            dataType: 'json',
            async: false,
            success: (response) => {
                eventMetaData = response;
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Event Metadata ", errorThrown);
            },
        });

        return eventMetaData;

    };


    const displayPlot = useCallback((selectedAirframe: string) => {
        
        const toEpochMs = (s: string | number) => {
        
            //Already a number, return it
            if (typeof s === 'number') return s;
            
            //Numeric string; detect seconds vs ms
            if (/^\d+$/.test(s))
                return (s.length <= 10)
                    ? Number(s) * 1_000
                    : Number(s);

            const t = Date.parse(s);
            return Number.isNaN(t) ? undefined : t;
        };

        const colorForEvent = (name: string) => {
            const idx = eventNames.indexOf(name);
            const n = Math.max(eventNames.length, 1);
            const hue = Math.round((360 * (idx >= 0 ? idx : 0)) / n);
            return `hsl(${hue},100%,75%)`;
        };

        const severityTraces: Plotly.Data[] = [];
        const airframeNames: Record<string, number> = {};
        const markerSymbolList = ["circle", "diamond", "square", "x", "pentagon", "hexagon", "octagon"];

        for (const [eventName, countsMap] of Object.entries(eventSeveritiesState)) {

            //Event is unchecked, skip
            if (!eventChecked[eventName])
                continue;

            for (const [airframeName, counts] of Object.entries(countsMap)) {

                //Got GFD, skip
                if (airframeName === "Garmin Flight Display")
                    continue;

                //Airframe is unselected, skip
                if (selectedAirframe !== airframeName && selectedAirframe !== "All Airframes")
                    continue;

                //Airframe is undefined, assign it a unique index
                if (airframeNames[airframeName] === undefined)
                    airframeNames[airframeName] = Object.keys(airframeNames).length;
                
                const markerSymbol = markerSymbolList[airframeNames[airframeName] % markerSymbolList.length];
                const markerSymbolAny = `${markerSymbol}-open-dot`;

                const x: number[] = [];
                const y: number[] = [];
                const z: (string | number)[] = [];
                const customdata: Array<
                    [
                    string,          //0. flightId
                    string | null,   //1. otherFlightId
                    string,          //2. systemId
                    string,          //3. tail
                    number,          //4. eventDefinitionId
                    string,          //5. tagName
                    number,          //6. severity
                    string | number, //7. startTime (raw)
                    string | number  //8. endTime (raw)
                    ]
                > = [];

                for (const c of counts) {

                    const ms = toEpochMs(c.startTime);
                    if (ms !== undefined)
                        x.push(ms);
                    y.push(c.severity);
                    z.push(c.endTime);

                    const primary = String(c.flightId);
                    const other = (c.eventDefinitionId === -1)
                        ? String(c.otherFlightId ?? "")
                        : null;

                    customdata.push([
                        primary,
                        other,
                        String(c.systemId),
                        String(c.tail),
                        Number(c.eventDefinitionId),
                        String(c.tagName ?? ""),
                        Number(c.severity),
                        c.startTime,
                        c.endTime
                    ]);
                }

                const trace: Partial<Plotly.ScatterData> = {
                    name: `${eventName} - ${airframeName}`,
                    type: 'scatter',
                    mode: 'markers',
                    hoverinfo: 'skip', //<-- Lets hovertemplate control the tooltip
                    x,
                    y,
                    z,
                    customdata,
                    marker: (eventName === "ANY Event")
                        ? {
                            color: 'gray',
                            size: 14,
                            symbol: markerSymbolAny,
                            opacity: 0.65
                        }
                        : {
                            color: colorForEvent(eventName),
                            size: 8,
                            symbol: markerSymbol,
                            line: { color: 'black', width: 1 }
                        },

                    hovertemplate:
                        'Flight #: %{customdata[0]} (Other #: %{customdata[1]})<br>' +
                        'System ID: %{customdata[2]}<br>' +
                        'Tail: %{customdata[3]}<br>' +
                        'Severity: %{customdata[6]:.2f}<br>' +
                        'Tag: %{customdata[5]}<br>' +
                        'Event Start: %{x|%Y-%m-%d %H:%M:%S}<br>' +
                        'Event End: %{customdata[8]}<br>' +
                        '<extra></extra>'
                };

                //Put ANY Event underneath other points
                if (eventName === "ANY Event")
                    severityTraces.unshift(trace as Plotly.Data);

                else severityTraces.push(trace as Plotly.Data);

            }
        }

        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();

        const severityLayout: Plotly.Layout = {
            title: { text: 'Severity of Events' },
            hovermode: "closest",
            autosize: true,
            margin: { l: 50, r: 50, b: 50, t: 50, pad: 4 },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: { color: plotTextColor },
            xaxis: { gridcolor: plotGridColor, type: 'date' },
            yaxis: { gridcolor: plotGridColor }
        } as Plotly.Layout;

        const config = { responsive: true as const };

        Plotly.react('severities-plot', severityTraces, severityLayout, config);

        const severitiesPlot = document.getElementById('severities-plot') as Plotly.PlotlyHTMLElement | null;
        if (!severitiesPlot) {
            console.error('Severities plot not found');
            return;
        }


        //Clean up old listeners to avoid duplicates
        severitiesPlot.removeAllListeners?.('plotly_click');

        severitiesPlot.on('plotly_click', (event) => {

            //Event has no points data, exit
            if (!event?.points?.length)
                return;

            for (const point of event.points) {
            const customData = point.data.customdata?.[point.pointIndex] as undefined | [
                string, string | null, string, string, number, string, number, string | number, string | number
            ];

            if (!customData)
                continue;

            const flightId = customData[0];
            const other = customData[1];
            const url = other
                ? `/protected/flight?flight_id=${encodeURIComponent(other)}&flight_id=${encodeURIComponent(flightId)}`
                : `/protected/flight?flight_id=${encodeURIComponent(flightId)}`;

            window.open(url, '_blank', 'noopener');
            }

        });

    }, [eventChecked, eventSeveritiesState]);


    const fetchAllEventSeverities = useCallback(() => {

        $('#loading').show();
        console.log("Showing loading spinner!");


        const startDate = buildStartDate(startYear, startMonth);
        const endDate = buildEndDate(endYear, endMonth);

        const submissionData = {
            startDate: startDate,
            endDate: endDate,
            eventNames: JSON.stringify(eventNames),
            tagName: tagName
        };

        $.ajax({
            type: 'GET',
            url: '/api/event/severities',
            data: submissionData,
            success: (response: EventSeverities) => {
                $('#loading').hide();
                if (response.err_msg) {
                    showErrorModal(response.err_title, response.err_msg);
                    return;
                }

                const next: EventSeverities = {};
                const newEventsEmpty: Record<string, boolean> = {};

                for (const [eventName, countsByAirframe] of Object.entries(response)) {
                    const hasAnyData = Object.values(countsByAirframe).some(arr => Array.isArray(arr) && arr.length > 0);
                    newEventsEmpty[eventName] = !hasAnyData;
                    next[eventName] = hasAnyData ? countsByAirframe : {};
                }

                //Build "ANY Event"
                const anyEvent: EventSeverityByAirframe = {};
                for (const countsByAirframe of Object.values(next)) {

                    for (const [airframeName, eventCountArray] of Object.entries(countsByAirframe)) {

                        if (!anyEvent[airframeName])
                            anyEvent[airframeName] = [];

                        anyEvent[airframeName] = anyEvent[airframeName].concat(eventCountArray);
                    }

                }

                if (Object.keys(anyEvent).length)
                    next['ANY Event'] = anyEvent;

                setEventsEmpty(newEventsEmpty);
                setEventSeveritiesState(next);

            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Uploads", errorThrown);
            }

        });

    }, [startMonth, startYear, endMonth, endYear, tagName]);

    const fetchEventSeverities = (eventName:string) => {

        $('#loading').show();
        console.log("Showing loading spinner!");

        const startDate = buildStartDate(startYear, startMonth);
        const endDate = buildEndDate(endYear, endMonth);

        const submissionData = {
            startDate: startDate,
            endDate: endDate,
            tagName: tagName
        };

        return new Promise(() => {

            $.ajax({
                type: 'GET',
                url: `/api/event/severities/${encodeURIComponent(eventName)}`,
                data: submissionData,
                async: true,
                success: (response: EventSeverityByAirframe) => {
                    $('#loading').hide();

                    if ((response).err_msg) {
                        showErrorModal(response.err_title, response.err_msg);
                        return;
                    }

                    const hasAnyData = Object.values(response).some(
                        (counts) => Array.isArray(counts) && counts.length > 0
                    );

                    setEventsEmpty((prev) => ({ ...prev, [eventName]: !hasAnyData }));
                    eventSeverities[eventName] = hasAnyData ? response : {};
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    showErrorModal("Error Loading Uploads", errorThrown);
                },
            });
        });

    };

    const checkEvent = (eventName: string) => {

        console.log("Checking event: '", eventName, "'");
        setEventChecked(prevEventChecked => {
            const newEventChecked = { ...prevEventChecked };
            newEventChecked[eventName] = !newEventChecked[eventName];
            return newEventChecked;
        });

        fetchEventSeverities(eventName);

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


    const dateChange = useCallback(async () => {

        //Clear Time Header update flag
        setDatesOrAirframeChanged(false);

        const cleared: Record<string, boolean> = {};

        for (const eventName of eventNames)
            cleared[eventName] = false;

        setEventChecked(cleared);
        setDatesChanged(false);

        await fetchAllEventSeverities();

        displayPlot(airframe.name);

    }, [airframe.name, fetchAllEventSeverities, displayPlot]);


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

    const tagNameChange = (tagName:string) => {
        setTagName(tagName);
    };

    const render = () => {

        console.log("Rendering Severities Page...");

        return (
            <div style={{overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh"}}>

                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar
                        activePage={"severities"}
                        darkModeOnClickAlt={() => { displayPlot(airframe.name); }}
                        waitingUserCount={waitingUserCount}
                        fleetManager={fleetManager}
                        unconfirmedTailsCount={unconfirmedTailsCount}
                        modifyTailsAccess={modifyTailsAccess}
                        plotMapHidden={plotMapHidden}
                    />
                </div>

                <div className="container-fluid" style={{overflowY: "auto", flex: "1 1 auto"}}>

                    <div className="row">
                        <div className="col-lg-12" style={{paddingBottom: "128px"}}>
                            <div className="card mb-2 m-2">
                                <TimeHeader
                                    name="Event Severities"
                                    airframes={airframesForUI.map(a => a.name)}
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
                                    tagNames={tagNames}
                                    tagName={tagName}
                                    tagNameChange={tagNameChange}
                                    datesOrAirframeChanged={datesOrAirframeChanged}
                                    requireManualInitialUpdate
                                />

                                <div className="card-body" style={{padding: "0"}}>
                                    <div className="row" style={{margin: "0"}}>
                                        <div className="col-lg-2" style={{padding: "8 8 8 8"}}>

                                            {
                                                eventNames.map((eventName, index) => {

                                                    //Don't show a description for the "ANY Event" event
                                                    if (eventName === "ANY Event") return (
                                                        <div key={index} className="form-check">
                                                            <input className="form-check-input" type="checkbox" value=""
                                                                   id={`event-check-${  index}`}
                                                                   checked={eventChecked[eventName]}
                                                                   onChange={() => checkEvent(eventName)}
                                                                   style={{border: "1px solid red"}}/>
                                                            <label className="form-check-label">
                                                                {eventName}
                                                            </label>
                                                        </div>
                                                    );

                                                    return (
                                                        <div key={index} className="form-check">
                                                            <input disabled={eventsEmpty[eventName]}
                                                                   className="form-check-input" type="checkbox" value=""
                                                                   id={`event-check-${  index}`}
                                                                   checked={eventChecked[eventName]}
                                                                   onChange={() => checkEvent(eventName)}></input>

                                                            <OverlayTrigger
                                                                overlay={(props) => (
                                                                    <Tooltip {...props}>
                                                                        {GetDescription(eventName)}
                                                                    </Tooltip>
                                                                )}
                                                                placement="bottom"
                                                            >
                                                                <label className="form-check-label">
                                                                    {eventName}
                                                                </label>
                                                            </OverlayTrigger>


                                                        </div>
                                                    );
                                                })
                                            }


                                        </div>

                                    <div className="col-lg-10 p-0!" style={{padding:"0 0 0 8", opacity:"0.80"}}>
                                        <div id="severities-plot" className="h-full"/>
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

}


const container = document.getElementById('severities-page');
if (container) {
    const root = createRoot(container);
    root.render(<SeveritiesPage/>);
}