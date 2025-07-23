import React, { useEffect, useState } from "react";
import { createRoot } from "react-dom/client";
import SignedInNavbar from "./signed_in_navbar";
import { TimeHeader } from "./time_header.js";
import GetDescription from "./get_description";
import Tooltip from "react-bootstrap/Tooltip";
import { OverlayTrigger } from "react-bootstrap";
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import XYZ from 'ol/source/XYZ';
import OSM from 'ol/source/OSM';
import DragBox from 'ol/interaction/DragBox';
import { platformModifierKeyOnly } from 'ol/events/condition';
import { fromLonLat, toLonLat } from 'ol/proj';
import 'ol/ol.css';

// Helper types
interface EventChecked {
    [eventName: string]: boolean;
}
interface EventsEmpty {
    [eventName: string]: boolean;
}

// Add HeatmapQuery interface
interface HeatmapQuery {
    date: {
        startYear: number;
        startMonth: number;
        endYear: number;
        endMonth: number;
    };
    airframe: string;
    coordinates: {
        minLat: string;
        maxLat: string;
        minLon: string;
        maxLon: string;
    };
    events: string[];
}

const azureMapsKey = process.env.AZURE_MAPS_KEY;

const mapLayerOptions = [
    { value: 'Aerial', label: 'Aerial', url: () => `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.imagery&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}` },
    { value: 'Road', label: 'Road (static)', url: () => `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}` },
    { value: 'RoadOnDemand', label: 'Road (dynamic)', url: () => `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.hybrid.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}` },
    { value: 'SectionalCharts', label: 'Sectional Charts', url: () => 'http://localhost:8187/sectional/{z}/{x}/{-y}.png' },
    { value: 'TerminalAreaCharts', label: 'Terminal Area Charts', url: () => 'http://localhost:8187/terminal-area/{z}/{x}/{-y}.png' },
    { value: 'IFREnrouteLowCharts', label: 'IFR Enroute Low Charts', url: () => 'http://localhost:8187/ifr-enroute-low/{z}/{x}/{-y}.png' },
    { value: 'IFREnrouteHighCharts', label: 'IFR Enroute High Charts', url: () => 'http://localhost:8187/ifr-enroute-high/{z}/{x}/{-y}.png' },
    { value: 'HelicopterCharts', label: 'Helicopter Charts', url: () => 'http://localhost:8187/helicopter/{z}/{x}/{-y}.png' },
];

const initialAirframes = ["PA-28-181", "Cessna 172S", "PA-44-180", "Cirrus SR20"];

const allEventNames = [
    "ANY Event",
    "Airspeed",
    "Altitude",
    "Battery Not Charging",
    "CHT Divergence",
    "CHT Sensor Divergence",
    "Cylinder Head Temperature",
    "E1 EGT Divergence",
    "EGT Sensor Divergence",
    "EGT Sensor Divergence (200 degrees)",
    "Engine Shutdown Below 3000 Ft",
    "High Altitude LOC-I",
    "High Altitude Limit Exceeded",
    "High Altitude Spin",
    "High Altitude Stall",
    "High CHT",
    "High Lateral Acceleration",
    "High Pitch",
    "High Vertical Acceleration",
    "Low Airspeed on Approach",
    "Low Airspeed on Climbout",
    "Low Altitude LOC-I",
    "Low Altitude Spin",
    "Low Altitude Stall",
    "Low Battery Level",
    "Low Ending Fuel",
    "Low Fuel",
    "Low Lateral Acceleration",
    "Low Oil Pressure",
    "Low Pitch",
    "Low Positional Accuracy",
    "Low Vertical Acceleration",
    "Proximity",
    "Roll",
    "Tail Strike",
    "VSI on Final"
];

const HeatMapPage: React.FC = () => {
    // State
    const [airframes, setAirframes] = useState<string[]>([...initialAirframes]);
    const [eventNames, setEventNames] = useState<string[]>(allEventNames);
    const [eventChecked, setEventChecked] = useState<EventChecked>(() => {
        const checked: EventChecked = {};
        for (const name of allEventNames) checked[name] = false;
        return checked;
    });
    const [eventsEmpty, setEventsEmpty] = useState<EventsEmpty>(() => {
        const empty: EventsEmpty = {};
        for (const name of allEventNames) empty[name] = false;
        return empty;
    });
    const [airframe, setAirframe] = useState<string>("All Airframes");
    const [startYear, setStartYear] = useState<number>(2020);
    const [startMonth, setStartMonth] = useState<number>(1);
    const [endYear, setEndYear] = useState<number>(new Date().getFullYear());
    const [endMonth, setEndMonth] = useState<number>(new Date().getMonth() + 1);
    const [datesChanged, setDatesChanged] = useState<boolean>(false);
    const [map, setMap] = useState<Map | null>(null);
    const [mapStyle, setMapStyle] = useState<string>('Road');
    const [boxCoords, setBoxCoords] = useState<{ minLat: string, maxLat: string, minLon: string, maxLon: string }>({ minLat: '', maxLat: '', minLon: '', maxLon: '' });
    const [minSeverity, setMinSeverity] = useState<number>(0);
    const [maxSeverity, setMaxSeverity] = useState<number>(1000);
    const mapRef = React.useRef<HTMLDivElement | null>(null);

    // Handlers for severity sliders
    const handleMinSeverityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = Math.min(Number(e.target.value), maxSeverity);
        setMinSeverity(value);
        if (value > maxSeverity) setMaxSeverity(value);
    };
    const handleMaxSeverityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const value = Math.max(Number(e.target.value), minSeverity);
        setMaxSeverity(value);
        if (value < minSeverity) setMinSeverity(value);
    };

    // Severity slider JSX (for top menu)
    const severitySlider = (
        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'flex-end', gap: 8, minWidth: 180, marginLeft: 16 }}>
            <label style={{ fontSize: 12, marginBottom: 0, display: 'flex', flexDirection: 'column', alignItems: 'flex-start' }}>
                <span style={{ marginBottom: 2 }}>Severity Range: <span style={{ color: 'red', fontWeight: 600 }}>{minSeverity} <span style={{ color: 'black' }}>-</span> {maxSeverity}</span></span>
                <div style={{ display: 'flex', alignItems: 'center', gap: 4, minWidth: 120 }}>
                    <input
                        type="range"
                        name="minSeverity"
                        min={0}
                        max={maxSeverity}
                        value={minSeverity}
                        onChange={handleMinSeverityChange}
                        style={{ width: 120, accentColor: 'red' }}
                    />
                    <input
                        type="range"
                        name="maxSeverity"
                        min={minSeverity}
                        max={1000}
                        value={maxSeverity}
                        onChange={handleMaxSeverityChange}
                        style={{ width: 120, accentColor: 'red' }}
                    />
                </div>
            </label>
        </div>
    );

    // Map initialization and layer switching
    useEffect(() => {
        if (mapRef.current && !map) {
            // Create layers for each style
            const layers = mapLayerOptions.map(opt => new TileLayer({
                visible: opt.value === mapStyle,
                source: new XYZ({ url: opt.url() })
            }));
            const olMap = new Map({
                target: mapRef.current,
                layers,
                view: new View({
                    center: fromLonLat([-95, 40]),
                    zoom: 4
                })
            });
            // DragBox for coordinate selection
            const dragBox = new DragBox({ condition: platformModifierKeyOnly });
            olMap.addInteraction(dragBox);
            dragBox.on('boxend', () => {
                const extent = dragBox.getGeometry().getExtent();
                const bottomLeft = toLonLat([extent[0], extent[1]]);
                const topRight = toLonLat([extent[2], extent[3]]);
                setBoxCoords({
                    minLat: Math.min(bottomLeft[1], topRight[1]).toFixed(6),
                    maxLat: Math.max(bottomLeft[1], topRight[1]).toFixed(6),
                    minLon: Math.min(bottomLeft[0], topRight[0]).toFixed(6),
                    maxLon: Math.max(bottomLeft[0], topRight[0]).toFixed(6)
                });
            });
            setMap(olMap);
        }
        // Layer switching
        if (map) {
            map.getLayers().forEach((layer, idx) => {
                layer.setVisible(mapLayerOptions[idx].value === mapStyle);
            });
        }
    }, [mapRef, map, mapStyle]);

    // Handlers
    const handleCheckEvent = (eventName: string) => {
        setEventChecked(prev => ({ ...prev, [eventName]: !prev[eventName] }));
    };
    const handleAirframeChange = (af: string) => {
        setAirframe(af);
    };
    const handleStartYear = (y: number) => {
        setStartYear(Number(y));
        setDatesChanged(true);
    };
    const handleStartMonth = (m: number) => {
        setStartMonth(Number(m));
        setDatesChanged(true);
    };
    const handleEndYear = (y: number) => {
        setEndYear(Number(y));
        setDatesChanged(true);
    };
    const handleEndMonth = (m: number) => {
        setEndMonth(Number(m));
        setDatesChanged(true);
    };
    // Move alert logic to handleDateChange
    const handleDateChange = () => {
        // Gather checked events
        const selectedEvents = allEventNames.filter(e => eventChecked[e]);
        const query: HeatmapQuery = {
            date: {
                startYear,
                startMonth,
                endYear,
                endMonth
            },
            airframe,
            coordinates: boxCoords,
            events: selectedEvents
        };
        alert("Heatmap Query:\n" + JSON.stringify(query, null, 2));
        setDatesChanged(false);
    };
    const handleExportCSV = () => {
        // Placeholder for export logic
    };

    // Add coordinate input boxes to the TimeHeader row
    const extraHeaderComponents = (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-start', minWidth: 320, height: 54, justifyContent: 'flex-end', marginTop: 8, marginLeft: 22, paddingLeft: 0, marginRight: 32 }}>
            <span style={{ fontSize: '11px', color: '#666', marginBottom: 2, whiteSpace: 'nowrap', minWidth: 110 }}>
                Select Area (⌘ + Drag):
            </span>
            <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'baseline', gap: 4 }}>
                <label style={{ marginRight: 2, fontSize: 13 }}>Min Lat:</label>
                <div style={{ position: 'relative', display: 'inline-block' }}>
                    <input style={{ width: 92, fontSize: 12, background: 'transparent', border: 'none', borderBottom: '1px solid #bbb', marginRight: 4, padding: '0 2px', textAlign: 'center', lineHeight: '1.8', marginTop: '-2px' }} type="text" value={boxCoords.minLat} readOnly />
                    <span style={{ opacity: 0, pointerEvents: 'none', position: 'absolute', left: 0, top: 0, width: '100%', fontSize: 12 }}>00.000000</span>
                </div>
                <label style={{ marginRight: 2, fontSize: 13 }}>Min Lon:</label>
                <div style={{ position: 'relative', display: 'inline-block' }}>
                    <input style={{ width: 92, fontSize: 12, background: 'transparent', border: 'none', borderBottom: '1px solid #bbb', marginRight: 4, padding: '0 2px', textAlign: 'center', lineHeight: '1.8', marginTop: '-2px' }} type="text" value={boxCoords.minLon} readOnly />
                    <span style={{ opacity: 0, pointerEvents: 'none', position: 'absolute', left: 0, top: 0, width: '100%', fontSize: 12 }}>00.000000</span>
                </div>
                <label style={{ marginRight: 2, fontSize: 13 }}>Max Lat:</label>
                <div style={{ position: 'relative', display: 'inline-block' }}>
                    <input style={{ width: 92, fontSize: 12, background: 'transparent', border: 'none', borderBottom: '1px solid #bbb', marginRight: 4, padding: '0 2px', textAlign: 'center', lineHeight: '1.8', marginTop: '-2px' }} type="text" value={boxCoords.maxLat} readOnly />
                    <span style={{ opacity: 0, pointerEvents: 'none', position: 'absolute', left: 0, top: 0, width: '100%', fontSize: 12 }}>00.000000</span>
                </div>
                <label style={{ marginRight: 2, fontSize: 13 }}>Max Lon:</label>
                <div style={{ position: 'relative', display: 'inline-block' }}>
                    <input style={{ width: 92, fontSize: 12, background: 'transparent', border: 'none', borderBottom: '1px solid #bbb', padding: '0 2px', textAlign: 'center', lineHeight: '1.8', marginTop: '-2px' }} type="text" value={boxCoords.maxLon} readOnly />
                    <span style={{ opacity: 0, pointerEvents: 'none', position: 'absolute', left: 0, top: 0, width: '100%', fontSize: 12 }}>00.000000</span>
                </div>
            </div>
        </div>
    );

    // Map layer dropdown for the navbar (define here, before return)
    const mapLayerDropdown = (
        <div style={{ display: 'flex', alignItems: 'center', marginLeft: '16px', marginRight: '8px', height: '56px' }}>
            <select
                className="custom-select"
                id="mapLayerSelect"
                style={{ marginLeft: "1px", height: "36px", minHeight: "36px", maxHeight: "36px", border: "1px solid rgb(108, 117, 125)", alignSelf: 'center' }}
                value={mapStyle}
                onChange={e => setMapStyle(e.target.value)}
            >
                {mapLayerOptions.map(opt => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                ))}
            </select>
        </div>
    );

    // Wrap the main content in a full-height flex container
    return (
        <div style={{ overflowX: 'hidden', display: 'flex', flexDirection: 'column', height: '100vh' }}>
            <div style={{ flex: '0 0 auto' }}>
                <SignedInNavbar activePage="heat_map" mapLayerDropdown={mapLayerDropdown} />
            </div>
            <div style={{ flex: '1 1 auto', display: 'flex', flexDirection: 'column', height: '100%' }}>
                <div className="container-fluid" style={{ flex: '1 1 auto', height: '100%' }}>
                    <div className="row" style={{ height: '100%' }}>
                        <div className="col-lg-12" style={{ height: '100%' }}>
                            <div className="card m-2" style={{ height: '100%', display: 'flex', flexDirection: 'column', flex: '1 1 auto' }}>
                                <TimeHeader
                                    name="Heat Map Event Selection"
                                    airframes={airframes}
                                    airframe={airframe}
                                    startYear={startYear}
                                    startMonth={startMonth}
                                    endYear={endYear}
                                    endMonth={endMonth}
                                    datesChanged={true}
                                    dateChange={handleDateChange}
                                    airframeChange={handleAirframeChange}
                                    updateStartYear={handleStartYear}
                                    updateStartMonth={handleStartMonth}
                                    updateEndYear={handleEndYear}
                                    updateEndMonth={handleEndMonth}
                                    extraHeaderComponents={extraHeaderComponents}
                                    severitySliderComponent={severitySlider}
                                />
                                <div className="card-body" style={{ padding: 0, background: 'transparent', flex: '1 1 auto', minHeight: 0, display: 'flex' }}>
                                    <div className="row" style={{ margin: 0, padding: 0, background: 'transparent', flex: 1, height: '100%' }}>
                                        <div className="col-lg-2" style={{ padding: 0, margin: 0, background: 'transparent', height: '100%', paddingLeft: 16, paddingTop: 16 }}>
                                            {allEventNames.map((eventName, index) => (
                                                <div key={index} className="form-check">
                                                    <input
                                                        className="form-check-input"
                                                        type="checkbox"
                                                        checked={eventChecked[eventName] || false}
                                                        onChange={() => handleCheckEvent(eventName)}
                                                    />
                                                    <label className="form-check-label">{eventName}</label>
                                                </div>
                                            ))}
                                        </div>
                                        <div className="col-lg-10" style={{ padding: 0, margin: 0, background: 'transparent', height: '100%' }}>
                                            <div ref={mapRef} style={{ width: '100%', height: '100%', background: 'transparent', margin: 0, padding: 0 }} />
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
};

const container = document.querySelector("#heat-map-page");
if (container) {
    const root = createRoot(container);
    root.render(<HeatMapPage />);
} 