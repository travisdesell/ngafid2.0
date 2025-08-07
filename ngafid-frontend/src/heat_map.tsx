// =======================
// HEAT MAP PAGE COMPONENT
// =======================
//
// This file implements the Heat Map page for the NGAFID frontend. It provides:
//  - Event selection and filtering UI
//  - OpenLayers map with multiple base layers (Azure Maps, Sectional Charts, etc.)
//  - Proximity event heatmap and grid overlays
//  - Draggable popups for event details and distance calculations
//  - State management for all of the above
//
// =======================
// SECTION: Imports
// =======================
import React, { useEffect, useState, useRef, useCallback } from "react";
import { createRoot } from "react-dom/client";
import SignedInNavbar from "./signed_in_navbar";
import { TimeHeader } from "./time_header.js";

// OpenLayers imports
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import XYZ from 'ol/source/XYZ';
import DragBox from 'ol/interaction/DragBox';
import { platformModifierKeyOnly } from 'ol/events/condition';
import { fromLonLat, toLonLat, getTransform } from 'ol/proj';
import 'ol/ol.css';

// Vector and styling imports
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Feature from 'ol/Feature';
import Polygon from 'ol/geom/Polygon';
import Point from 'ol/geom/Point';
import Style from 'ol/style/Style';
import Fill from 'ol/style/Fill';
import Icon from 'ol/style/Icon';
import Circle from 'ol/style/Circle';
import Stroke from 'ol/style/Stroke';

// Heatmap and WebGL imports
import Heatmap from 'ol/layer/Heatmap';
import WebGLVectorLayer from 'ol/layer/WebGLVector';

// =======================
// SECTION: Type Definitions
// =======================

interface EventChecked {
    [eventName: string]: boolean;
}

interface PopupContentData {
    time: string | null;
    latitude: number | null;
    longitude: number | null;
    altitude: number | null;
    flightId: string | null;
    flightAirframe: string | null;
    otherFlightId: string | number | null;
    otherFlightAirframe: string | null;
    severity: string | null;
    eventId: number | null;
    eventType: string | null;
    eventTypes?: string[] | null;
    columnValues?: { [key: string]: any } | null;
}

interface ProximityEventPoints {
    eventId: number;
    eventDefinitionId: number;
    mainFlightId: number;
    otherFlightId: number | null;
    mainFlightPoints: any[];
    otherFlightPoints: any[];
    severity: number;
    airframe: string;
    otherAirframe: string;
}

interface CoordinateEventGroup {
    coord: number[];
    events: Array<{
        eventId: number;
        eventDefinitionId: number;
        flightId: number;
        otherFlightId: number | null;
        time: string;
        flightAirframe: string;
        otherFlightAirframe: string;
        severity: number;
        altitudeAgl: number;
        latitude: number;
        longitude: number;
    }>;
}

interface EventStatistics {
    totalEvents: number;
    eventsByType: { [eventType: string]: number };
}

// =======================
// SECTION: Constants and Configuration
// =======================

// Map layer configuration
const mapLayerOptions = [
    { value: 'Aerial', label: 'Aerial', url: () => azureMapsKey ? `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.imagery&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}` : undefined },
    { value: 'Road', label: 'Road (static)', url: () => azureMapsKey ? `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}` : undefined },
    { value: 'RoadOnDemand', label: 'Road (dynamic)', url: () => azureMapsKey ? `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.hybrid.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}` : undefined },
    { value: 'SectionalCharts', label: 'Sectional Charts', url: () => 'http://localhost:8187/sectional/{z}/{x}/{-y}.png' },
    { value: 'TerminalAreaCharts', label: 'Terminal Area Charts', url: () => 'http://localhost:8187/terminal-area/{z}/{x}/{-y}.png' },
    { value: 'IFREnrouteLowCharts', label: 'IFR Enroute Low Charts', url: () => 'http://localhost:8187/ifr-enroute-low/{z}/{x}/{-y}.png' },
    { value: 'IFREnrouteHighCharts', label: 'IFR Enroute High Charts', url: () => 'http://localhost:8187/ifr-enroute-high/{z}/{x}/{-y}.png' },
    { value: 'HelicopterCharts', label: 'Helicopter Charts', url: () => 'http://localhost:8187/helicopter/{z}/{x}/{-y}.png' },
];

// Event definitions mapping
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

// Event definition ID mappings
const eventNameToDefinitionIds: { [eventName: string]: number[] } = {
    "ANY Event": [], // Will be filled with all IDs below
    "Airspeed": [9, 11, 12, 13, 63],
    "Altitude": [14, 15, 16, 17, 64],
    "Battery Not Charging": [57],
    "CHT Divergence": [65],
    "CHT Sensor Divergence": [39, 40, 41],
    "Cylinder Head Temperature": [21, 22, 23, 62],
    "E1 EGT Divergence": [67],
    "EGT Sensor Divergence": [42, 43, 44, 45],
    "EGT Sensor Divergence (200 degrees)": [72],
    "Engine Shutdown Below 3000 Ft": [46, 47, 48, 49],
    "High Altitude LOC-I": [50],
    "High Altitude Limit Exceeded": [59, 60, 61],
    "High Altitude Spin": [-3],
    "High Altitude Stall": [51],
    "High CHT": [66],
    "High Lateral Acceleration": [4],
    "High Pitch": [2],
    "High Vertical Acceleration": [6],
    "Low Airspeed on Approach": [31, 32, 33, 34, 69],
    "Low Airspeed on Climbout": [35, 36, 37, 38, 68],
    "Low Altitude LOC-I": [53],
    "Low Altitude Spin": [-2],
    "Low Altitude Stall": [52],
    "Low Battery Level": [56],
    "Low Ending Fuel": [-7, -6, -5, -4],
    "Low Fuel": [28, 29, 30],
    "Low Lateral Acceleration": [3],
    "Low Oil Pressure": [24, 25, 26, 27, 70],
    "Low Pitch": [1],
    "Low Positional Accuracy": [58],
    "Low Vertical Acceleration": [5],
    "Proximity": [-1],
    "Roll": [7],
    "Tail Strike": [55, 71],
    "VSI on Final": [8],
};

// Fill ANY Event with all unique IDs
const allDefinitionIds = Array.from(new Set(Object.values(eventNameToDefinitionIds).flat().filter(id => id !== undefined)));
eventNameToDefinitionIds["ANY Event"] = allDefinitionIds;

// Map styling constants
const ICON_IMAGE_RED = new Icon({
    src: '/images/red-point.png',
    scale: 0.05,
    anchor: [0.5, 0.5],
});

const RED_POINT_STYLE = new Style({
    image: ICON_IMAGE_RED
});

const BLUE_POINT_STYLE = new Style({
    image: ICON_IMAGE_RED
});

const MARKER_VISIBILITY_ZOOM_THRESHOLD = 15;

// Azure Maps configuration
let azureMapsKey = process.env.AZURE_MAPS_KEY;

// Airframes configuration - define airframes if not already defined
declare const airframes: string[] | undefined;
let airframesList = (typeof airframes !== 'undefined' && Array.isArray(airframes)) ? [...airframes] : [];
if (!airframesList.includes('All Airframes')) {
    airframesList.unshift('All Airframes');
}
const gfdIndex = airframesList.indexOf('Garmin Flight Display');
if (gfdIndex !== -1) airframesList.splice(gfdIndex, 1);

// =======================
// SECTION: Utility Functions
// =======================

/**
 * Interpolates color from green to red based on intensity value
 */
function interpolateColor(value: number): string {
    const r = Math.round(255 * value);
    const g = Math.round(255 * (1 - value));
    return `rgba(${r},${g},0,0.6)`;
}

/**
 * Converts degrees to radians
 */
function toRadians(degrees: number): number {
    return degrees * (Math.PI / 180);
}

/**
 * Creates a coordinate key for grouping events by location
 */
function createCoordinateKey(lat: number, lon: number, precision: number = 6): string {
    return `${lat.toFixed(precision)},${lon.toFixed(precision)}`;
}

/**
 * Calculates the lateral (surface) and euclidean (3D) distance between two points
 */
function calculateDistanceBetweenPoints(
    lat1: number, lon1: number, alt1: number = 0,
    lat2: number, lon2: number, alt2: number = 0
): { lateral: number, euclidean: number } {
    const earthRadius = 6371000; // meters

    const lat1Rad = toRadians(lat1);
    const lat2Rad = toRadians(lat2);
    const deltaLatRad = toRadians(lat2 - lat1);
    const deltaLonRad = toRadians(lon2 - lon1);

    // Haversine formula for lateral distance (in meters)
    const a = Math.sin(deltaLatRad / 2) ** 2 +
        Math.cos(lat1Rad) * Math.cos(lat2Rad) *
        Math.sin(deltaLonRad / 2) ** 2;
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    const lateralMeters = earthRadius * c;

    // Convert lateral distance to feet
    const lateral = lateralMeters * 3.28084;

    // 3D distance
    const deltaAlt = (alt2 || 0) - (alt1 || 0);
    const euclidean = Math.sqrt(lateral ** 2 + deltaAlt ** 2);

    return { lateral, euclidean };
}

/**
 * Gets the event type name from event definition ID
 */
function getEventTypeName(eventDefinitionId: number): string {
    for (const [eventName, definitionIds] of Object.entries(eventNameToDefinitionIds)) {
        if (eventName === "ANY Event") continue;
        if (definitionIds.includes(eventDefinitionId)) {
            return eventName;
        }
    }
    return `Event ${eventDefinitionId}`;
}

/**
 * Detects the user's operating system.
 * Used to display OS-specific instructions.
 * 
 * (Invoked immediately on load to set userOS constant)
 * 
 * @returns The user's operating system as a string
 */
function getOS():string {

    //Try to detect via user-agent first
    if ('userAgentData' in navigator) {

        const { platform } = (navigator as any).userAgentData;
        switch (platform) {
            case 'Windows':          return 'Windows';
            case 'macOS':            return 'Mac OS';
            case 'Android':          return 'Android';
            case 'Chrome OS':        return 'Chrome OS';
            case 'iOS':              return 'iOS';
            case 'Linux':            return 'Linux';
            default:                 return 'Unknown';
        }

    }

    //If the above approach fails, use the legacy version
    const
        platform = window.navigator.platform,
        macosPlatforms = ['macOS', 'Macintosh', 'MacIntel', 'MacPPC', 'Mac68K'],
        windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE']
    ;
        
    const OS_FALLBACK_DEFAULT = 'Linux';

    let os = OS_FALLBACK_DEFAULT;

    if (macosPlatforms.indexOf(platform) !== -1) {
        os = 'Mac OS';
    } else if (windowsPlatforms.indexOf(platform) !== -1) {
        os = 'Windows';
    } else if (/Linux/.test(platform)) {
        os = OS_FALLBACK_DEFAULT;
    }

    return os;

}

const userOS = getOS();


// =======================
// SECTION: Main HeatMapPage Component
// =======================

const HeatMapPage: React.FC = () => {
    // =======================
    // SECTION: State Management
    // =======================

    // UI State
    const [airframes, setAirframes] = useState<string[]>(airframesList);
    const [eventChecked, setEventChecked] = useState<EventChecked>(() => {
        const checked: EventChecked = {};
        for (const name of allEventNames) checked[name] = false;
        return checked;
    });
    const [airframe, setAirframe] = useState<string>("All Airframes");
    const [startYear, setStartYear] = useState<number>(1990);
    const [startMonth, setStartMonth] = useState<number>(1);
    const [endYear, setEndYear] = useState<number>(new Date().getFullYear());
    const [endMonth, setEndMonth] = useState<number>(new Date().getMonth() + 1);
    const [datesChanged, setDatesChanged] = useState<boolean>(false);
    const [error, setError] = useState<string | null>(null);
    const [loading, setLoading] = useState<boolean>(false);
    const [datesOrAirframeChanged, setDatesOrAirframeChanged] = useState<boolean>(false);
    useEffect(() => {
        setDatesOrAirframeChanged(true);
    }, [datesChanged, airframe]);

    // Map State
    const [map, setMap] = useState<Map | null>(null);
    const [mapStyle, setMapStyle] = useState<string>('Road');
    const [boxCoords, setBoxCoords] = useState<{ minLat: string, maxLat: string, minLon: string, maxLon: string }>({ minLat: '', maxLat: '', minLon: '', maxLon: '' });
    const [overlayLayer, setOverlayLayer] = useState<VectorLayer<VectorSource> | null>(null);
    const [overlayFeature, setOverlayFeature] = useState<Feature<Polygon> | null>(null);

    // Severity Filter State
    const [minSeverity, setMinSeverity] = useState<number>(-10000);
    const [maxSeverity, setMaxSeverity] = useState<number>(1000);
    const [displayMinSeverity, setDisplayMinSeverity] = useState<number>(0);
    const [displayMaxSeverity, setDisplayMaxSeverity] = useState<number>(1000);

    // Visualization State
    const [showGrid, setShowGrid] = useState<boolean>(false);
    const [heatmapLayer1, setHeatmapLayer1] = useState<Heatmap | null>(null);
    const [heatmapLayer2, setHeatmapLayer2] = useState<Heatmap | null>(null);
    const [markerSource, setMarkerSource] = useState<VectorSource | null>(null);
    const [markerLayer, setMarkerLayer] = useState<VectorLayer | null>(null);
    const [gridLayer, setGridLayer] = useState<WebGLVectorLayer | null>(null);
    const [gridSource, setGridSource] = useState<VectorSource | null>(null);

    // Event Data State
    const [proximityEventPoints, setProximityEventPoints] = useState<ProximityEventPoints[]>([]);
    const [coordinateRegistry, setCoordinateRegistry] = useState<{[key: string]: CoordinateEventGroup}>({});

    // Event Statistics State
    const [eventStatistics, setEventStatistics] = useState<EventStatistics>({
        totalEvents: 0,
        eventsByType: {}
    });

    // Navigation Tips State
    const [navigationTipsExpanded, setNavigationTipsExpanded] = useState<boolean>(true);

    // Popup and Distance Calculation State
    const [openPopups, setOpenPopups] = useState<Array<{
        id: string,
        coord: number[],
        data: PopupContentData,
        position?: { left: number, top: number }
    }>>([]);
    const [selectedPoints, setSelectedPoints] = useState<Array<{
        id: string;
        latitude: number;
        longitude: number;
        altitude: number;
    }>>([]);
    const [distances, setDistances] = useState<{
        lateral: number | null;
        euclidean: number | null;
    }>({ lateral: null, euclidean: null });

    // =======================
    // SECTION: Popup Dragging Functionality
    // =======================
    /**
     * Implements draggable popups for event details.
     * Uses refs to track drag state and ensure global event handlers always have the latest values.
     */

    const dragStart = useRef<{ x: number, y: number }>({ x: 0, y: 0 });
    const popupStart = useRef<{ left: number, top: number }>({ left: 0, top: 0 });
    const [draggedPopupId, setDraggedPopupId] = useState<string | null>(null);
    const draggedPopupIdRef = useRef<string | null>(null);
    const [recentPopupId, setRecentPopupId] = useState<string | null>(null);

    // Map ref must be defined before using in handlers
    const mapRef = React.useRef<HTMLDivElement | null>(null);

    // Called on popup header mousedown: starts drag and attaches listeners
    const handlePopupMouseDown = (e: React.MouseEvent, popupId: string) => {
        e.preventDefault();
        dragStart.current = { x: e.clientX, y: e.clientY };
        
        // Get the popup element and its current position
        const popupElem = (e.currentTarget.parentElement as HTMLElement);
        popupStart.current = {
            left: parseFloat(popupElem.style.left) || 0,
            top: parseFloat(popupElem.style.top) || 0
        };
        
        setDraggedPopupId(popupId);
        draggedPopupIdRef.current = popupId;
        setRecentPopupId(popupId);
        
        window.addEventListener('mousemove', handleMouseMove);
        window.addEventListener('mouseup', handleMouseUp);
    };

    // Global mousemove handler: updates popup position if dragging
    const handleMouseMove = useCallback((e: MouseEvent) => {
        if (!draggedPopupIdRef.current || !mapRef.current) {
            return;
        }
        
        const dx = e.clientX - dragStart.current.x;
        const dy = e.clientY - dragStart.current.y;
        let left = popupStart.current.left + dx;
        let top = popupStart.current.top + dy;
        
        // Constrain to map container
        const mapRect = mapRef.current.getBoundingClientRect();
        left = Math.max(0, Math.min(left, mapRect.width - 200)); // 200 = minWidth
        top = Math.max(0, Math.min(top, mapRect.height - 100)); // 100 = approx height
        
        setOpenPopups(prevPopups =>
            prevPopups.map(p =>
                p.id === draggedPopupIdRef.current
                    ? { ...p, position: { left, top } }
                    : p
            )
        );
    }, []);

    // Global mouseup handler: ends drag and removes listeners
    const handleMouseUp = useCallback(() => {
        setDraggedPopupId(null);
        draggedPopupIdRef.current = null;
        window.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('mouseup', handleMouseUp);
    }, [handleMouseMove]);

    // Cleanup event listeners on unmount
    useEffect(() => {
        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };
    }, [handleMouseMove]);

    // =======================
    // SECTION: Map Layer Management
    // =======================

    /**
     * Clears all map layers and resets visualization state
     */
    const clearMapLayers = () => {
        if (heatmapLayer1) heatmapLayer1.getSource()?.clear();
        if (heatmapLayer2) heatmapLayer2.getSource()?.clear();
        if (markerSource) markerSource.clear();
        if (gridSource) {
            gridSource.getFeatures().forEach(f => {
                if (f.get('kind') === 'density')
                    gridSource.removeFeature(f);
            });
        }

        // Clear coordinate registry
        setCoordinateRegistry({});

        // Reset event statistics
        setEventStatistics({
            totalEvents: 0,
            eventsByType: {}
        });

        // Hide layers
        if (heatmapLayer1) heatmapLayer1.setVisible(false);
        if (heatmapLayer2) heatmapLayer2.setVisible(false);
        if (gridLayer) gridLayer.setVisible(false);
    };


    // =======================
    // SECTION: Severity Filter Management
    // =======================

    /**
     * Handles minimum severity slider changes
     */
    const handleMinSeverityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const displayValue = Math.min(Number(e.target.value), displayMaxSeverity);
        setDisplayMinSeverity(displayValue);
        
        // Map display value to actual backend value
        // If user selects 0, we include all negative values (-10000 to displayValue)
        const actualValue = displayValue === 0 ? -10000 : displayValue;
        setMinSeverity(actualValue);
        
        if (displayValue > displayMaxSeverity) {
            setDisplayMaxSeverity(displayValue);
            setMaxSeverity(displayValue);
        }
    };

    /**
     * Handles maximum severity slider changes
     */
    const handleMaxSeverityChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const displayValue = Math.max(Number(e.target.value), displayMinSeverity);
        setDisplayMaxSeverity(displayValue);
        setMaxSeverity(displayValue);
        
        if (displayValue < displayMinSeverity) {
            setDisplayMinSeverity(displayValue);
            const actualValue = displayValue === 0 ? -10000 : displayValue;
            setMinSeverity(actualValue);
        }
    };

    /**
     * Severity slider component for the top menu
     */
    const severitySlider = (
        <div className="input-group">
            <div className="input-group-prepend">
                <div className="time-selector flex-col text-left px-3 gap-2">

                    {/* Severity Range Values Indicator */}
                    <span className="w-full flex flex-row justify-between items-center text-sm">

                        <div>Severity Range:</div>

                        <div className="flex flex-row items-center gap-1">
                            <span className="text-red-500">{displayMinSeverity}</span>
                            <span className="text-[var(--c_text)]">-</span>
                            <span className="text-red-500">{displayMaxSeverity}</span>
                        </div>
                    </span>
                        

                    <div style={{ display: 'flex', alignItems: 'center', gap: 4, minWidth: 120 }}>
                        <input
                            type="range"
                            name="minSeverity"
                            min={0}
                            max={displayMaxSeverity}
                            value={displayMinSeverity}
                            onChange={handleMinSeverityChange}
                            style={{ width: 120, accentColor: 'red' }}
                        />
                        <input
                            type="range"
                            name="maxSeverity"
                            min={displayMinSeverity}
                            max={1000}
                            value={displayMaxSeverity}
                            onChange={handleMaxSeverityChange}
                            style={{ width: 120, accentColor: 'red' }}
                        />
                    </div>


                </div>
            </div>
        </div>
    )

    // =======================
    // SECTION: Grid/Heatmap Toggle Management
    // =======================
    /**
     * Handles toggling between heatmap and grid visualizations for events on the map.
     * When toggled, reprocesses the current events with the new visualization mode.
     */

    const handleGridToggle = () => {
        const newShowGrid = !showGrid;
        setShowGrid(newShowGrid);
        
        // Reprocess events if we have any, using the new value
        if (proximityEventPoints.length > 0) {
            // Check if we have proximity events or regular events
            const hasProximityEvents = proximityEventPoints.some(event => event.otherFlightId !== 0 && event.otherFlightId !== null);
            if (hasProximityEvents) {
                processProximityEventCoordinates(proximityEventPoints, newShowGrid);
            } else {
                processSingleEventCoordinates(proximityEventPoints, newShowGrid);
            }
        }
    };

    /**
     * Grid/Heatmap toggle switch component with icons
     */
    const gridToggleSwitch = (
        <div
            className="bg-[var(--c_bg)]"
            style={{
                position: 'absolute',
                top: 16,
                right: 16,
                zIndex: 1000,
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '4px 10px',
                borderRadius: 5,
                boxShadow: '0 1px 4px rgba(0,0,0,0.10)',
                fontSize: 18,
                minWidth: 0
            }}
        >
            <span
                title="Heatmap"
                style={{
                    cursor: 'pointer',
                    color: !showGrid ? '#e74c3c' : '#bbb',
                    opacity: !showGrid ? 1 : 0.5,
                    fontWeight: !showGrid ? 700 : 400,
                    fontSize: 22,
                    transition: 'color 0.2s, opacity 0.2s',
                    display: 'flex',
                    alignItems: 'center',
                }}
                onClick={() => {
                    setShowGrid(false);
                    if (proximityEventPoints.length > 0) {
                        const hasProximityEvents = proximityEventPoints.some(event => event.otherFlightId !== 0 && event.otherFlightId !== null);
                        if (hasProximityEvents) {
                            processProximityEventCoordinates(proximityEventPoints, false);
                        } else {
                            processSingleEventCoordinates(proximityEventPoints, false);
                        }
                    }
                }}
            >
                <i className="fa fa-fire" />
            </span>
            <label className="switch mb-0 justify-self-center mx-auto" style={{ margin: 0 }}>
                <input type="checkbox" checked={showGrid} onChange={handleGridToggle} />
                <span className="slider round"></span>
            </label>
            <span
                title="Grid"
                style={{
                    cursor: 'pointer',
                    color: showGrid ? '#007bff' : '#bbb',
                    opacity: showGrid ? 1 : 0.5,
                    fontWeight: showGrid ? 700 : 400,
                    fontSize: 22,
                    transition: 'color 0.2s, opacity 0.2s',
                    display: 'flex',
                    alignItems: 'center',
                }}
                onClick={() => {
                    setShowGrid(true);
                    if (proximityEventPoints.length > 0) {
                        const hasProximityEvents = proximityEventPoints.some(event => event.otherFlightId !== 0 && event.otherFlightId !== null);
                        if (hasProximityEvents) {
                            processProximityEventCoordinates(proximityEventPoints, true);
                        } else {
                            processSingleEventCoordinates(proximityEventPoints, true);
                        }
                    }
                }}
            >
                <i className="fa fa-th" />
            </span>
            <style>{`
                .switch {
                    position: relative;
                    display: inline-block;
                    width: 36px;
                    height: 18px;
                }
                .switch input { display: none; }
                .slider {
                    position: absolute;
                    cursor: pointer;
                    top: 0; left: 0; right: 0; bottom: 0;
                    background-color: #ccc;
                    transition: .4s;
                    border-radius: 18px;
                }
                .slider:before {
                    position: absolute;
                    content: "";
                    height: 12px;
                    width: 12px;
                    left: 3px;
                    bottom: 3px;
                    background-color: white;
                    transition: .4s;
                    border-radius: 50%;
                }
                input:checked + .slider {
                    background-color: #2196F3;
                }
                input:checked + .slider:before {
                    transform: translateX(18px);
                }
            `}</style>
        </div>
    );

    /**
     * Renders proximity event points as either a heatmap or a grid overlay.
     * - If shouldShowGrid is true, renders a grid-based density map overlay.
     * - Otherwise, renders the default heatmap layers.
     *
     * @param events The proximity events to render
     * @param useShowGrid Optional override for the grid/heatmap toggle
     */
    const processProximityEventCoordinates = async (allEventPoints: ProximityEventPoints[], useShowGrid?: boolean) => {
        if (!heatmapLayer1 || !heatmapLayer2 || !markerSource) {
            console.error('Heatmap or marker layers not initialized');
            return;
        }
        // Use the passed showGrid value or fall back to state
        const shouldShowGrid = useShowGrid !== undefined ? useShowGrid : showGrid;


        // Clear heatmap sources
        heatmapLayer1.getSource()!.clear();
        heatmapLayer2.getSource()!.clear();
        if (markerSource) markerSource.clear();

        // Clear coordinate registry
        setCoordinateRegistry({});
        const newCoordinateRegistry: {[key: string]: CoordinateEventGroup} = {};

        // Track all points for grid
        const allPoints: { latitude: number, longitude: number }[] = [];

        for (const eventPoints of allEventPoints) {
            // Ensure mainFlightPoints is an array
            if (!Array.isArray(eventPoints.mainFlightPoints)) {
                console.warn(`mainFlightPoints is not an array for event ${eventPoints.eventId}:`, eventPoints.mainFlightPoints);
                continue;
            }
            // RED: mainFlightPoints
            for (const point of eventPoints.mainFlightPoints) {
                const olCoord = fromLonLat([
                    point.longitude + 0.0001,
                    point.latitude + 0.0001
                ]);
                const heatmapFeature = new Feature({ geometry: new Point(olCoord) });
                heatmapFeature.set('weight', 0.4);
                heatmapLayer1.getSource()!.addFeature(heatmapFeature);
                
                // Add to coordinate registry
                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                if (!newCoordinateRegistry[coordKey]) {
                    newCoordinateRegistry[coordKey] = {
                        coord: olCoord,
                        events: []
                    };
                }
                newCoordinateRegistry[coordKey].events.push({
                    eventId: eventPoints.eventId,
                    eventDefinitionId: eventPoints.eventDefinitionId,
                    flightId: eventPoints.mainFlightId,
                    otherFlightId: eventPoints.otherFlightId,
                    time: point.timestamp,
                    flightAirframe: eventPoints.airframe,
                    otherFlightAirframe: eventPoints.otherAirframe,
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
            // BLACK: otherFlightPoints

            if (!Array.isArray(eventPoints.otherFlightPoints)) {
                console.warn(`otherFlightPoints is not an array for event ${eventPoints.eventId}:`, eventPoints.otherFlightPoints);
                continue;
            }
            for (const point of eventPoints.otherFlightPoints) {
                const olCoord = fromLonLat([
                    point.longitude + 0.0001,
                    point.latitude + 0.0001
                ]);
                const heatmapFeature = new Feature({ geometry: new Point(olCoord) });
                heatmapFeature.set('weight', 0.4);
                heatmapLayer2.getSource()!.addFeature(heatmapFeature);
                
                // Add to coordinate registry
                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                if (!newCoordinateRegistry[coordKey]) {
                    newCoordinateRegistry[coordKey] = {
                        coord: olCoord,
                        events: []
                    };
                }
                newCoordinateRegistry[coordKey].events.push({
                    eventId: eventPoints.eventId,
                    eventDefinitionId: eventPoints.eventDefinitionId,
                    flightId: eventPoints.otherFlightId || 0,
                    otherFlightId: eventPoints.mainFlightId,
                    time: point.timestamp,
                    flightAirframe: eventPoints.otherAirframe,
                    otherFlightAirframe: eventPoints.airframe,
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
        }

        // Create markers from coordinate registry
        Object.values(newCoordinateRegistry).forEach((group) => {
            const marker = new Feature({ geometry: new Point(group.coord) });
            
            // All markers use blue style
            marker.setStyle(BLUE_POINT_STYLE);
            
            // Use the first event's properties for the marker
            const firstEvent = group.events[0];
            marker.setProperties({
                isMarker: true,
                coordKey: createCoordinateKey(firstEvent.latitude, firstEvent.longitude),
                events: group.events,
                // Legacy properties for backward compatibility
                eventId: firstEvent.eventId,
                eventDefinitionId: firstEvent.eventDefinitionId,
                flightId: firstEvent.flightId,
                otherFlightId: firstEvent.otherFlightId,
                time: firstEvent.time,
                flightAirframe: firstEvent.flightAirframe,
                otherFlightAirframe: firstEvent.otherFlightAirframe,
                severity: firstEvent.severity,
                altitudeAgl: firstEvent.altitudeAgl,
                latitude: firstEvent.latitude,
                longitude: firstEvent.longitude
            });
            markerSource.addFeature(marker);
        });

        // Update coordinate registry state
        setCoordinateRegistry(newCoordinateRegistry);

        // Fit map to extents
        const extent1 = heatmapLayer1.getSource()!.getExtent();
        const extent2 = heatmapLayer2.getSource()!.getExtent();
        const features1 = heatmapLayer1.getSource()!.getFeatures();
        const features2 = heatmapLayer2.getSource()!.getFeatures();

        const isValidExtent = (extent: number[]) => {
            return (
                Array.isArray(extent) &&
                extent.length === 4 &&
                extent.every((v) => typeof v === 'number' && isFinite(v)) &&
                (extent[0] !== extent[2]) && (extent[1] !== extent[3])
            );
        };

        if ((features1.length > 0 || features2.length > 0) && isValidExtent(extent1) && isValidExtent(extent2)) {
            const combinedExtent: [number, number, number, number] = [
                Math.min(extent1[0], extent2[0]),
                Math.min(extent1[1], extent2[1]),
                Math.max(extent1[2], extent2[2]),
                Math.max(extent1[3], extent2[3])
            ];
            map?.getView().fit(combinedExtent, { padding: [50, 50, 50, 50], maxZoom: 15 });
        }

        // Add grid-based density map if enabled
        if (shouldShowGrid && map && gridSource) {
            const gridSize = 0.05;
            const gridCounts: Record<string, number> = {};
            allPoints.forEach(pt => {
                const latKey = (Math.floor(pt.latitude / gridSize) * gridSize).toFixed(4);
                const lonKey = (Math.floor(pt.longitude / gridSize) * gridSize).toFixed(4);
                const key = `${latKey},${lonKey}`;
                gridCounts[key] = (gridCounts[key] || 0) + 1;
            });
            // Parse bounding box as numbers
            const minLatNum = parseFloat(boxCoords.minLat);
            const maxLatNum = parseFloat(boxCoords.maxLat);
            const minLonNum = parseFloat(boxCoords.minLon);
            const maxLonNum = parseFloat(boxCoords.maxLon);

            let latStart = minLatNum, latEnd = maxLatNum, lonStart = minLonNum, lonEnd = maxLonNum;
            if (isNaN(latStart) || isNaN(latEnd) || isNaN(lonStart) || isNaN(lonEnd)) {
                if (allPoints.length > 0) {
                    latStart = Math.min(...allPoints.map(pt => pt.latitude));
                    latEnd = Math.max(...allPoints.map(pt => pt.latitude));
                    lonStart = Math.min(...allPoints.map(pt => pt.longitude));
                    lonEnd = Math.max(...allPoints.map(pt => pt.longitude));
                } else {
                    latStart = latEnd = lonStart = lonEnd = 0;
                }
            }

            if (latStart > latEnd) [latStart, latEnd] = [latEnd, latStart];
            if (lonStart > lonEnd) [lonStart, lonEnd] = [lonEnd, lonStart];

            const features = [];
            let maxCount = 1;
            if (Object.values(gridCounts).length > 0) {
                maxCount = Math.max(1, ...Object.values(gridCounts));
            }

            const proj = getTransform('EPSG:4326', 'EPSG:3857');

            const xs: number[] = [];
            const ys: number[] = [];
            for (let lon = Math.floor(lonStart / gridSize) * gridSize; lon <= lonEnd + 1e-9; lon += gridSize) {
                xs.push(proj([lon, 0])[0]);
            }
            for (let lat = Math.floor(latStart / gridSize) * gridSize; lat <= latEnd + 1e-9; lat += gridSize) {
                ys.push(proj([0, lat])[1]);
            }

            const rows = ys.length - 1;
            const cols = xs.length - 1;

            for (let j = 0; j < rows; j++) {
                const y0 = ys[j];
                const y1 = ys[j + 1];
                const latVal = (Math.floor(latStart / gridSize) + j) * gridSize;

                for (let i = 0; i < cols; i++) {
                    const x0 = xs[i];
                    const x1 = xs[i + 1];
                    const lonVal = (Math.floor(lonStart / gridSize) + i) * gridSize;

                    const count = gridCounts[`${latVal.toFixed(4)},${lonVal.toFixed(4)}`] || 0;
                    const intensity = (maxCount > 0 ? Math.sqrt(count / maxCount) : 0);
                    const color = interpolateColor(intensity);

                    const polygon = new Polygon([[
                        [x0, y0],
                        [x1, y0],
                        [x1, y1],
                        [x0, y1],
                        [x0, y0]
                    ]]);

                    const cellFeature = new Feature(polygon);
                    cellFeature.set('kind', 'density');
                    cellFeature.set('color', color);

                    features.push(cellFeature);
                }
            }

            // Clear old density cells
            gridSource.getFeatures().forEach(f => {
                if (f.get('kind') === 'density')
                    gridSource.removeFeature(f);
            });

            // Add new features
            gridSource.addFeatures(features);

            // Hide heatmap layers
            heatmapLayer1.setVisible(false);
            heatmapLayer2.setVisible(false);

            // Show grid layer
            if (gridLayer) {
                gridLayer.setVisible(true);
            } else {
                console.warn('[processProximityEventCoordinates] gridLayer is null!');
            }
        } else {
            // Show heatmap layers
            if (heatmapLayer1) heatmapLayer1.setVisible(true);
            if (heatmapLayer2) heatmapLayer2.setVisible(true);
            // Hide grid layer if present
            if (gridLayer) {
                gridLayer.setVisible(false);
            }
        }
    };

    // to render mixed events (both proximity and single events)
    const processMixedEventCoordinates = async (proximityEventPoints: ProximityEventPoints[], singleEventPoints: any[], useShowGrid?: boolean) => {
        if (!heatmapLayer1 || !heatmapLayer2 || !markerSource) {
            console.error('Heatmap or marker layers not initialized');
            return;
        }
        
        // Use the passed showGrid value or fall back to state
        const shouldShowGrid = useShowGrid !== undefined ? useShowGrid : showGrid;

        
        // Clear heatmap and marker sources
        heatmapLayer1.getSource()!.clear();
        heatmapLayer2.getSource()!.clear();
        if (markerSource) markerSource.clear();

        // Clear coordinate registry
        setCoordinateRegistry({});
        const newCoordinateRegistry: {[key: string]: CoordinateEventGroup} = {};

        // Track all points for extent and grid
        const allPoints: { latitude: number, longitude: number }[] = [];

        // Process proximity events first
        console.log('[processMixedEventCoordinates] Processing proximity events...');
        for (const eventPoints of proximityEventPoints) {
            // Ensure mainFlightPoints is an array
            if (!Array.isArray(eventPoints.mainFlightPoints)) {
                console.warn(`mainFlightPoints is not an array for event ${eventPoints.eventId}:`, eventPoints.mainFlightPoints);
                continue;
            }
            // RED: mainFlightPoints
            for (const point of eventPoints.mainFlightPoints) {
                const olCoord = fromLonLat([
                    point.longitude + 0.0001,
                    point.latitude + 0.0001
                ]);
                const heatmapFeature = new Feature({ geometry: new Point(olCoord) });
                heatmapFeature.set('weight', 0.7);
                heatmapLayer1.getSource()!.addFeature(heatmapFeature);
                
                // Add to coordinate registry
                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                if (!newCoordinateRegistry[coordKey]) {
                    newCoordinateRegistry[coordKey] = {
                        coord: olCoord,
                        events: []
                    };
                }
                newCoordinateRegistry[coordKey].events.push({
                    eventId: eventPoints.eventId,
                    eventDefinitionId: eventPoints.eventDefinitionId,
                    flightId: eventPoints.mainFlightId,
                    otherFlightId: eventPoints.otherFlightId,
                    time: point.timestamp,
                    flightAirframe: eventPoints.airframe,
                    otherFlightAirframe: eventPoints.otherAirframe,
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
            // BLACK: otherFlightPoints
            if (!Array.isArray(eventPoints.otherFlightPoints)) {
                console.warn(`otherFlightPoints is not an array for event ${eventPoints.eventId}:`, eventPoints.otherFlightPoints);
                continue;
            }
            for (const point of eventPoints.otherFlightPoints) {
                const olCoord = fromLonLat([
                    point.longitude + 0.0001,
                    point.latitude + 0.0001
                ]);
                const heatmapFeature = new Feature({ geometry: new Point(olCoord) });
                heatmapFeature.set('weight', 0.7);
                heatmapLayer2.getSource()!.addFeature(heatmapFeature);
                
                // Add to coordinate registry
                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                if (!newCoordinateRegistry[coordKey]) {
                    newCoordinateRegistry[coordKey] = {
                        coord: olCoord,
                        events: []
                    };
                }
                newCoordinateRegistry[coordKey].events.push({
                    eventId: eventPoints.eventId,
                    eventDefinitionId: eventPoints.eventDefinitionId,
                    flightId: eventPoints.otherFlightId || 0,
                    otherFlightId: eventPoints.mainFlightId,
                    time: point.timestamp,
                    flightAirframe: eventPoints.otherAirframe,
                    otherFlightAirframe: eventPoints.airframe,
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
        }

        // Process single events
        for (const eventPoints of singleEventPoints) {
            // Ensure mainFlightPoints is an array
            if (!Array.isArray(eventPoints.mainFlightPoints)) {
                console.warn(`mainFlightPoints is not an array for event ${eventPoints.eventId}:`, eventPoints.mainFlightPoints);
                continue;
            }
            for (const point of eventPoints.mainFlightPoints) {
                const olCoord = fromLonLat([
                    point.longitude + 0.0001,
                    point.latitude + 0.0001
                ]);
                const heatmapFeature = new Feature({ geometry: new Point(olCoord) });
                heatmapFeature.set('weight', 0.7);
                heatmapLayer1.getSource()!.addFeature(heatmapFeature);
                
                // Add to coordinate registry
                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                if (!newCoordinateRegistry[coordKey]) {
                    newCoordinateRegistry[coordKey] = {
                        coord: olCoord,
                        events: []
                    };
                }
                newCoordinateRegistry[coordKey].events.push({
                    eventId: eventPoints.eventId,
                    eventDefinitionId: eventPoints.eventDefinitionId,
                    flightId: eventPoints.mainFlightId,
                    otherFlightId: null, // Single events don't have other flights
                    time: point.timestamp,
                    flightAirframe: eventPoints.airframe,
                    otherFlightAirframe: '',
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
        }

        // Create markers from coordinate registry
        Object.values(newCoordinateRegistry).forEach((group) => {
            const marker = new Feature({ geometry: new Point(group.coord) });
            
            // All markers use blue style
            marker.setStyle(BLUE_POINT_STYLE);
            
            // Use the first event's properties for the marker
            const firstEvent = group.events[0];
            marker.setProperties({
                isMarker: true,
                coordKey: createCoordinateKey(firstEvent.latitude, firstEvent.longitude),
                events: group.events,
                // Legacy properties for backward compatibility
                eventId: firstEvent.eventId,
                eventDefinitionId: firstEvent.eventDefinitionId,
                flightId: firstEvent.flightId,
                otherFlightId: firstEvent.otherFlightId,
                time: firstEvent.time,
                flightAirframe: firstEvent.flightAirframe,
                otherFlightAirframe: firstEvent.otherFlightAirframe,
                severity: firstEvent.severity,
                altitudeAgl: firstEvent.altitudeAgl,
                latitude: firstEvent.latitude,
                longitude: firstEvent.longitude
            });
            markerSource.addFeature(marker);
        });

        // Update coordinate registry state
        setCoordinateRegistry(newCoordinateRegistry);

        // Fit map to extents
        const extent1 = heatmapLayer1.getSource()!.getExtent();
        const extent2 = heatmapLayer2.getSource()!.getExtent();
        const features1 = heatmapLayer1.getSource()!.getFeatures();
        const features2 = heatmapLayer2.getSource()!.getFeatures();

        const isValidExtent = (extent: number[]) => {
            return (
                Array.isArray(extent) &&
                extent.length === 4 &&
                extent.every((v) => typeof v === 'number' && isFinite(v)) &&
                (extent[0] !== extent[2]) && (extent[1] !== extent[3])
            );
        };

        if ((features1.length > 0 || features2.length > 0) && isValidExtent(extent1) && isValidExtent(extent2)) {
            const combinedExtent: [number, number, number, number] = [
                Math.min(extent1[0], extent2[0]),
                Math.min(extent1[1], extent2[1]),
                Math.max(extent1[2], extent2[2]),
                Math.max(extent1[3], extent2[3])
            ];
            map?.getView().fit(combinedExtent, { padding: [50, 50, 50, 50], maxZoom: 15 });
        }

        // Add grid-based density map if enabled
        if (shouldShowGrid && map && gridSource) {
            const gridSize = 0.05;
            const gridCounts: Record<string, number> = {};
            allPoints.forEach(pt => {
                const latKey = (Math.floor(pt.latitude / gridSize) * gridSize).toFixed(4);
                const lonKey = (Math.floor(pt.longitude / gridSize) * gridSize).toFixed(4);
                const key = `${latKey},${lonKey}`;
                gridCounts[key] = (gridCounts[key] || 0) + 1;
            });
            
            // Parse bounding box as numbers
            const minLatNum = parseFloat(boxCoords.minLat);
            const maxLatNum = parseFloat(boxCoords.maxLat);
            const minLonNum = parseFloat(boxCoords.minLon);
            const maxLonNum = parseFloat(boxCoords.maxLon);

            let latStart = minLatNum, latEnd = maxLatNum, lonStart = minLonNum, lonEnd = maxLonNum;
            if (isNaN(latStart) || isNaN(latEnd) || isNaN(lonStart) || isNaN(lonEnd)) {
                if (allPoints.length > 0) {
                    latStart = Math.min(...allPoints.map(pt => pt.latitude));
                    latEnd = Math.max(...allPoints.map(pt => pt.latitude));
                    lonStart = Math.min(...allPoints.map(pt => pt.longitude));
                    lonEnd = Math.max(...allPoints.map(pt => pt.longitude));
                } else {
                    latStart = latEnd = lonStart = lonEnd = 0;
                }
            }

            if (latStart > latEnd) [latStart, latEnd] = [latEnd, latStart];
            if (lonStart > lonEnd) [lonStart, lonEnd] = [lonEnd, lonStart];

            const features = [];
            let maxCount = 1;
            if (Object.values(gridCounts).length > 0) {
                maxCount = Math.max(1, ...Object.values(gridCounts));
            }

            const proj = getTransform('EPSG:4326', 'EPSG:3857');

            const xs: number[] = [];
            const ys: number[] = [];
            for (let lon = Math.floor(lonStart / gridSize) * gridSize; lon <= lonEnd + 1e-9; lon += gridSize) {
                xs.push(proj([lon, 0])[0]);
            }
            for (let lat = Math.floor(latStart / gridSize) * gridSize; lat <= latEnd + 1e-9; lat += gridSize) {
                ys.push(proj([0, lat])[1]);
            }

            const rows = ys.length - 1;
            const cols = xs.length - 1;

            for (let j = 0; j < rows; j++) {
                const y0 = ys[j];
                const y1 = ys[j + 1];
                const latVal = (Math.floor(latStart / gridSize) + j) * gridSize;

                for (let i = 0; i < cols; i++) {
                    const x0 = xs[i];
                    const x1 = xs[i + 1];
                    const lonVal = (Math.floor(lonStart / gridSize) + i) * gridSize;

                    const count = gridCounts[`${latVal.toFixed(4)},${lonVal.toFixed(4)}`] || 0;
                    const intensity = (maxCount > 0 ? Math.sqrt(count / maxCount) : 0);
                    const color = interpolateColor(intensity);

                    const polygon = new Polygon([[
                        [x0, y0],
                        [x1, y0],
                        [x1, y1],
                        [x0, y1],
                        [x0, y0]
                    ]]);

                    const cellFeature = new Feature(polygon);
                    cellFeature.set('kind', 'density');
                    cellFeature.set('color', color);

                    features.push(cellFeature);
                }
            }

            // Clear old density cells
            gridSource.getFeatures().forEach(f => {
                if (f.get('kind') === 'density')
                    gridSource.removeFeature(f);
            });

            // Add new features
            gridSource.addFeatures(features);

            // Hide heatmap layers
            heatmapLayer1.setVisible(false);
            heatmapLayer2.setVisible(false);

            // Show grid layer
            if (gridLayer) {
                gridLayer.setVisible(true);
            } else {
                console.warn('[processMixedEventCoordinates] gridLayer is null!');
            }
        } else {
            // Show heatmap layers
            if (heatmapLayer1) heatmapLayer1.setVisible(true);
            if (heatmapLayer2) heatmapLayer2.setVisible(true);
            // Hide grid layer if present
            if (gridLayer) {
                gridLayer.setVisible(false);
            }
        }
    };

    // Dedicated function to render single-flight (non-proximity) events
    const processSingleEventCoordinates = async (singleEventPoints: any[], useShowGrid?: boolean) => {
        if (!heatmapLayer1 || !markerSource) {
            console.error('Heatmap or marker layers not initialized');
            return;
        }
        
        // Use the passed showGrid value or fall back to state
        const shouldShowGrid = useShowGrid !== undefined ? useShowGrid : showGrid;
        
        // Clear heatmap and marker sources
        heatmapLayer1.getSource()!.clear();
        if (markerSource) markerSource.clear();

        // Clear coordinate registry
        setCoordinateRegistry({});
        const newCoordinateRegistry: {[key: string]: CoordinateEventGroup} = {};

        // Track all points for extent and grid
        const allPoints: { latitude: number, longitude: number }[] = [];

        for (const eventPoints of singleEventPoints) {
            // Ensure mainFlightPoints is an array
            if (!Array.isArray(eventPoints.mainFlightPoints)) {
                console.warn(`mainFlightPoints is not an array for event ${eventPoints.eventId}:`, eventPoints.mainFlightPoints);
                continue;
            }
            for (const point of eventPoints.mainFlightPoints) {
                const olCoord = fromLonLat([
                    point.longitude + 0.0001,
                    point.latitude + 0.0001
                ]);
                const heatmapFeature = new Feature({ geometry: new Point(olCoord) });
                heatmapFeature.set('weight', 0.4);
                heatmapLayer1.getSource()!.addFeature(heatmapFeature);
                
                // Add to coordinate registry
                const coordKey = createCoordinateKey(point.latitude, point.longitude);
                if (!newCoordinateRegistry[coordKey]) {
                    newCoordinateRegistry[coordKey] = {
                        coord: olCoord,
                        events: []
                    };
                }
                newCoordinateRegistry[coordKey].events.push({
                    eventId: eventPoints.eventId,
                    eventDefinitionId: eventPoints.eventDefinitionId,
                    flightId: eventPoints.mainFlightId,
                    otherFlightId: null, // Single events don't have other flights
                    time: point.timestamp,
                    flightAirframe: eventPoints.airframe,
                    otherFlightAirframe: '',
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
        }

        // Create markers from coordinate registry
        Object.values(newCoordinateRegistry).forEach((group) => {
            const marker = new Feature({ geometry: new Point(group.coord) });
            marker.setStyle(BLUE_POINT_STYLE); // Single events now use blue for better visibility
            
            // Use the first event's properties for the marker
            const firstEvent = group.events[0];
            marker.setProperties({
                isMarker: true,
                coordKey: createCoordinateKey(firstEvent.latitude, firstEvent.longitude),
                events: group.events,
                // Legacy properties for backward compatibility
                eventId: firstEvent.eventId,
                eventDefinitionId: firstEvent.eventDefinitionId,
                flightId: firstEvent.flightId,
                otherFlightId: firstEvent.otherFlightId,
                time: firstEvent.time,
                flightAirframe: firstEvent.flightAirframe,
                otherFlightAirframe: firstEvent.otherFlightAirframe,
                severity: firstEvent.severity,
                altitudeAgl: firstEvent.altitudeAgl,
                latitude: firstEvent.latitude,
                longitude: firstEvent.longitude
            });
            markerSource.addFeature(marker);
        });

        // Update coordinate registry state
        setCoordinateRegistry(newCoordinateRegistry);

        // Fit map to extents
        const extent = heatmapLayer1.getSource()!.getExtent();
        const features = heatmapLayer1.getSource()!.getFeatures();
        const isValidExtent = (extent: number[]) => {
            return (
                Array.isArray(extent) &&
                extent.length === 4 &&
                extent.every((v) => typeof v === 'number' && isFinite(v)) &&
                (extent[0] !== extent[2]) && (extent[1] !== extent[3])
            );
        };
        if (features.length > 0 && isValidExtent(extent)) {
            map?.getView().fit(extent, { padding: [50, 50, 50, 50], maxZoom: 15 });
        }

        // Add grid-based density map if enabled
        if (shouldShowGrid && map && gridSource) {
            const gridSize = 0.05;
            const gridCounts: Record<string, number> = {};
            allPoints.forEach(pt => {
                const latKey = (Math.floor(pt.latitude / gridSize) * gridSize).toFixed(4);
                const lonKey = (Math.floor(pt.longitude / gridSize) * gridSize).toFixed(4);
                const key = `${latKey},${lonKey}`;
                gridCounts[key] = (gridCounts[key] || 0) + 1;
            });
            
            // Parse bounding box as numbers
            const minLatNum = parseFloat(boxCoords.minLat);
            const maxLatNum = parseFloat(boxCoords.maxLat);
            const minLonNum = parseFloat(boxCoords.minLon);
            const maxLonNum = parseFloat(boxCoords.maxLon);

            let latStart = minLatNum, latEnd = maxLatNum, lonStart = minLonNum, lonEnd = maxLonNum;
            if (isNaN(latStart) || isNaN(latEnd) || isNaN(lonStart) || isNaN(lonEnd)) {
                if (allPoints.length > 0) {
                    latStart = Math.min(...allPoints.map(pt => pt.latitude));
                    latEnd = Math.max(...allPoints.map(pt => pt.latitude));
                    lonStart = Math.min(...allPoints.map(pt => pt.longitude));
                    lonEnd = Math.max(...allPoints.map(pt => pt.longitude));
                } else {
                    latStart = latEnd = lonStart = lonEnd = 0;
                }
            }

            if (latStart > latEnd) [latStart, latEnd] = [latEnd, latStart];
            if (lonStart > lonEnd) [lonStart, lonEnd] = [lonEnd, lonStart];

            const features = [];
            let maxCount = 1;
            if (Object.values(gridCounts).length > 0) {
                maxCount = Math.max(1, ...Object.values(gridCounts));
            }

            const proj = getTransform('EPSG:4326', 'EPSG:3857');

            const xs: number[] = [];
            const ys: number[] = [];
            for (let lon = Math.floor(lonStart / gridSize) * gridSize; lon <= lonEnd + 1e-9; lon += gridSize) {
                xs.push(proj([lon, 0])[0]);
            }
            for (let lat = Math.floor(latStart / gridSize) * gridSize; lat <= latEnd + 1e-9; lat += gridSize) {
                ys.push(proj([0, lat])[1]);
            }

            const rows = ys.length - 1;
            const cols = xs.length - 1;

            for (let j = 0; j < rows; j++) {
                const y0 = ys[j];
                const y1 = ys[j + 1];
                const latVal = (Math.floor(latStart / gridSize) + j) * gridSize;

                for (let i = 0; i < cols; i++) {
                    const x0 = xs[i];
                    const x1 = xs[i + 1];
                    const lonVal = (Math.floor(lonStart / gridSize) + i) * gridSize;

                    const count = gridCounts[`${latVal.toFixed(4)},${lonVal.toFixed(4)}`] || 0;
                    const intensity = (maxCount > 0 ? Math.sqrt(count / maxCount) : 0);
                    const color = interpolateColor(intensity);

                    const polygon = new Polygon([[
                        [x0, y0],
                        [x1, y0],
                        [x1, y1],
                        [x0, y1],
                        [x0, y0]
                    ]]);

                    const cellFeature = new Feature(polygon);
                    cellFeature.set('kind', 'density');
                    cellFeature.set('color', color);

                    features.push(cellFeature);
                }
            }

            // Clear old density cells
            gridSource.getFeatures().forEach(f => {
                if (f.get('kind') === 'density')
                    gridSource.removeFeature(f);
            });

            // Add new features
            gridSource.addFeatures(features);

            // Hide heatmap layers
            heatmapLayer1.setVisible(false);

            // Show grid layer
            if (gridLayer) {
                gridLayer.setVisible(true);
            } else {
                console.warn('[processSingleEventCoordinates] gridLayer is null!');
            }
        } else {
            // Show heatmap layers
            if (heatmapLayer1) heatmapLayer1.setVisible(true);
            // Hide grid layer if present
            if (gridLayer) {
                gridLayer.setVisible(false);
            }

        }
    };

    // Map initialization and layer switching
    useEffect(() => {
        if (mapRef.current && !map) {
            console.log('[DEBUG] Initializing map...');
            // Create layers for each style
            const layers = mapLayerOptions.map(opt => {
                const url = opt.url();
                if (!url) {
                    console.warn(`Skipping layer ${opt.value} - no URL available (Azure Maps key may be missing)`);
                    return null;
                }
                return new TileLayer({
                    visible: opt.value === mapStyle,
                    source: new XYZ({ url })
                });
            }).filter((layer): layer is TileLayer<XYZ> => layer !== null);

            // Create heatmap sources and layers
            const heatmapSource1 = new VectorSource();
            const heatmapSource2 = new VectorSource();
            const markerSource = new VectorSource();

            const heatmapLayer1 = new Heatmap({
                source: heatmapSource1,
                blur: 3,
                radius: 4,
                opacity: 0.8,
                gradient: [
                    'rgba(0,255,0,0)',
                    'rgba(0,255,0,1)',
                    'rgba(255,255,0,1)',
                    'rgba(255,165,0,1)',
                    'rgba(255,0,0,1)'
                ]
            });
            heatmapLayer1.set('interactive', false);
            heatmapLayer1.setVisible(true); // Ensure it's visible by default

            const heatmapLayer2 = new Heatmap({
                source: heatmapSource2,
                blur: 3,
                radius: 4,
                opacity: 0.8,
                gradient: [
                    'rgba(0,255,0,0)',
                    'rgba(0,255,0,1)',
                    'rgba(255,255,0,1)',
                    'rgba(255,165,0,1)',
                    'rgba(255,0,0,1)'
                ]
            });
            heatmapLayer2.set('interactive', false);
            heatmapLayer2.setVisible(true); // Ensure it's visible by default

            // Create marker layer for individual points
            const markerLayer = new VectorLayer({
                source: markerSource,
                style: (feature) => {
                    const props = feature.getProperties();

                    if (!props.isMarker) return undefined;
                    
                    // All markers use blue style
                    return BLUE_POINT_STYLE;
                },
                zIndex: 1001
            });
            markerLayer.set('interactive', true); // Make markers interactive

            // Create grid source and layer
            const gridSource = new VectorSource({
                useSpatialIndex: false,
            });

            const gridLayer = new WebGLVectorLayer({
                source: gridSource,
                disableHitDetection: true,
                style: {
                    'fill-color': ['get', 'color'],
                    'stroke-color': 'rgba(0,0,0,0.1)',
                    'stroke-width': 1.00,
                },
                opacity: 0.7
            });
            gridLayer.set('interactive', false);
            gridLayer.setVisible(false); // Ensure grid is hidden by default

            // Create overlay vector layer for selection box
            const vectorSource = new VectorSource();
            const vectorLayer = new VectorLayer({
                source: vectorSource,
                style: new Style({
                    fill: new Fill({ color: 'rgba(128,128,128,0.3)' })
                }),
                zIndex: 1000
            });

            const olMap = new Map({
                target: mapRef.current,
                layers: [...layers, heatmapLayer1, heatmapLayer2, markerLayer, gridLayer, vectorLayer],
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
                // Create overlay polygon feature
                const poly = new Polygon([[
                    [extent[0], extent[1]],
                    [extent[0], extent[3]],
                    [extent[2], extent[3]],
                    [extent[2], extent[1]],
                    [extent[0], extent[1]]
                ]]);
                const feature = new Feature(poly);
                vectorSource.clear();
                vectorSource.addFeature(feature);
                setOverlayFeature(feature);
            });

            // =======================
            // SECTION: Restrict to 2 Selected Points/Popups & Distance Calculation
            // =======================
            /**
             * This section enforces that only two points/popups can be selected at a time on the map.
             *
             * - When a marker is clicked, if there are already two selected points, no additional point or popup is added.
             * - When a popup is closed, the corresponding point is removed, allowing another to be picked.
             * - When exactly two points are selected, the code calculates and displays:
             *     - Lateral (surface) distance
             *     - Euclidean (slant/3D) distance
             *     - Vertical separation
             *   These are shown at the bottom of both popups.
             *
             * This logic provides robust popup management and distance calculation functionality.
             *
             * Key state:
             *   - openPopups: Array of currently open popups (max 2)
             *   - selectedPoints: Array of selected points (max 2)
             *   - distances: Calculated distances between the two points
             *
             * Usage:
             *   - The marker click handler below enforces the 2-point restriction and triggers distance calculation.
             *   - The popup close handler removes the point and updates distances.
             */
            // Click handler for markers
            olMap.on('singleclick', (event) => {
                olMap.forEachFeatureAtPixel(event.pixel, (feature) => {
                    const geometry = feature.getGeometry() as Point | undefined;
                    if (!geometry) return;
                    const coord = geometry.getCoordinates();
                    const props = feature.getProperties();
                    if (!props.isMarker) return;
                    
                    // Use the events array stored directly in marker properties (more reliable than coordinate registry)
                    const coordKey = props.coordKey || createCoordinateKey(props.latitude, props.longitude);
                    
                    // Generate popup ID based on coordinates to avoid duplicates
                    const popupId = `coord-${coordKey}`;
                    // Prevent duplicate popups for the same marker
                    if (openPopups.some(p => p.id === popupId)) return;
                    // Use callback form to get latest selectedPoints
                    setSelectedPoints(prevSelectedPoints => {
                        if (prevSelectedPoints.length >= 2) {
                            return prevSelectedPoints;
                        }
                        // Add popup only if we can add a point
                        setOpenPopups(prevPopups => {
                            if (prevPopups.length >= 2) return prevPopups;
                            // Get all unique event types at this location
                            let allEventTypes: string[];
                            if (props.events && Array.isArray(props.events) && props.events.length > 0) {
                                // Use the events array stored in marker properties
                                allEventTypes = [...new Set(props.events.map((e: any) => getEventTypeName(e.eventDefinitionId)))];
                            } else {
                                // Fallback to single event
                                allEventTypes = [props.eventDefinitionId ? getEventTypeName(props.eventDefinitionId) : 'Unknown Event'];
                            }
                            
                            const popupContentData = {
                                time: new Date(props.time).toLocaleString(),
                                latitude: props.latitude !== undefined ? props.latitude : null,
                                longitude: props.longitude !== undefined ? props.longitude : null,
                                altitude: props.altitudeAgl !== undefined ? props.altitudeAgl : null,
                                flightId: props.flightId,
                                flightAirframe: props.flightAirframe || 'N/A',
                                otherFlightId: props.otherFlightId,
                                otherFlightAirframe: props.otherFlightAirframe || 'N/A',
                                severity: props.severity?.toFixed(2),
                                eventId: props.eventId || null,
                                eventType: allEventTypes.length === 1 ? allEventTypes[0] : `${allEventTypes.length} Events`,
                                eventTypes: allEventTypes
                            };
                            
                            // Fetch event columns values for this event(s)
                            if (props.flightId && props.time) {
                                if (props.events && Array.isArray(props.events) && props.events.length > 0) {
                                    // Multiple events - fetch columns and values for all events
                                    fetchMultipleEventColumnsValues(props.events, props.flightId, props.time, popupId);
                                } else if (props.eventId) {
                                    // Single event - fetch columns and values for this event
                                    fetchEventColumnsValues(props.eventId, props.flightId, props.time, popupId);
                                }
                            }
                            const pixel = olMap.getPixelFromCoordinate(coord);
                            const initialPosition = pixel ? { left: pixel[0], top: pixel[1] } : { left: 100, top: 100 };
                            const newOpenPopups = [
                                ...prevPopups,
                                { id: popupId, coord, data: popupContentData, position: initialPosition }
                            ];
                            return newOpenPopups;
                        });
                        const newSelectedPoints = [
                            ...prevSelectedPoints,
                            {
                                id: popupId,
                                latitude: props.latitude,
                                longitude: props.longitude,
                                altitude: props.altitudeAgl || 0
                            }
                        ];
                        // Calculate distances if we now have 2 points
                        if (newSelectedPoints.length === 2) {
                            const [point1, point2] = newSelectedPoints;
                            const result = calculateDistanceBetweenPoints(
                                point1.latitude, point1.longitude, point1.altitude,
                                point2.latitude, point2.longitude, point2.altitude
                            );
                            setDistances({ lateral: result.lateral, euclidean: result.euclidean });
                        } else {
                            setDistances({ lateral: null, euclidean: null });
                        }
                        return newSelectedPoints;
                    });
                }, {
                    layerFilter: (layer) => layer.get('interactive') === true
                });
            });
            // =================== END 2-POINT RESTRICTION & DISTANCE CALCULATION ===================

            // Zoom-based marker visibility
            olMap.on('moveend', () => {
                const mapView = olMap.getView();
                const mapZoom = mapView.getZoom();
                if (mapZoom === undefined) {
                    console.error('Map zoom level is undefined');
                    return;
                }
                if (markerLayer) {
                    markerLayer.setVisible(mapZoom >= MARKER_VISIBILITY_ZOOM_THRESHOLD);
                }
            });

            setMap(olMap);
            setOverlayLayer(vectorLayer);
            setHeatmapLayer1(heatmapLayer1);
            setHeatmapLayer2(heatmapLayer2);
            setMarkerSource(markerSource);
            setMarkerLayer(markerLayer);
            setGridLayer(gridLayer);
            setGridSource(gridSource);
        }
        // Layer switching
        if (map) {
            const tileLayers = map.getLayers().getArray().filter(layer => layer instanceof TileLayer);
            tileLayers.forEach((layer, idx) => {
                if (layer instanceof TileLayer && mapLayerOptions[idx]) {
                    const url = mapLayerOptions[idx].url();
                    if (url) {
                        layer.setVisible(mapLayerOptions[idx].value === mapStyle);
                    } else {
                        layer.setVisible(false);
                    }
                }
            });
        }
    }, [mapRef, map, mapStyle]);

    // =======================
    // EVENT HANDLERS (UI & FILTERS)
    // =======================
    // Handlers for event checkboxes, airframe selection, date pickers, etc.
    const handleCheckEvent = (eventName: string) => {
        setEventChecked(prev => ({ ...prev, [eventName]: !prev[eventName] }));
        // Clear previous parameters when Proximity or ANY Event is deselected
        if ((eventName === "Proximity" || eventName === "ANY Event") && eventChecked[eventName]) {
            clearMapLayers();
        }
    };
    const handleAirframeChange = (af: string) => {
        console.log('Airframe changing from', airframe, 'to', af);
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

    const toggleNavigationTips = () => {
        setNavigationTipsExpanded(!navigationTipsExpanded);
    };

    // --- Refactored Event Fetching and Points Aggregation ---
    // 1. Fetch events matching filters (all types)
    const fetchEvents = async (filters: {
        airframe: string;
        eventDefinitionIds: number[];
        startDate: string;
        endDate: string;
        minLat: number;
        maxLat: number;
        minLon: number;
        maxLon: number;
        minSeverity: number;
        maxSeverity: number;
    }) => {
        let url = `/protected/proximity_events_in_box?`;
        if (filters.airframe) url += `airframe=${encodeURIComponent(filters.airframe)}&`;
        url += `event_definition_ids=${filters.eventDefinitionIds.join(",")}&` +
            `start_date=${filters.startDate}&end_date=${filters.endDate}&` +
            `area_min_lat=${filters.minLat}&area_max_lat=${filters.maxLat}&` +
            `area_min_lon=${filters.minLon}&area_max_lon=${filters.maxLon}`;
        url += `&min_severity=${filters.minSeverity}&max_severity=${filters.maxSeverity}`;
        console.log('[DEBUG] Fetching events:', filters, url);
        const response = await fetch(url, { credentials: 'include', headers: { 'Accept': 'application/json' } });
        if (!response.ok) throw new Error('Failed to load events');
        const events = await response.json();
        console.log(`[DEBUG] Events fetched: count=${events.length}`);
        if (events.length > 0) {
            console.log('[DEBUG] First event:', events[0]);
            // Debug: Check what event definition IDs we actually received
            const receivedEventDefinitionIds = [...new Set(events.map((e: any) => e.event_definition_id))];
            
            // Check if we received any events that don't match our requested IDs
            const unexpectedEvents = events.filter((e: any) => !filters.eventDefinitionIds.includes(e.event_definition_id));
            if (unexpectedEvents.length > 0) {
                console.warn('[DEBUG] Received events with unexpected definition IDs:', unexpectedEvents.map((e: any) => e.event_definition_id));
            }
        }
        return events;
    };

    // Helper to check if event is a proximity event
    const isProximityEvent = (event: any) => {
        return event.event_definition_id === -1 || event.event_definition_id === -2 || event.event_definition_id === -3;
    };

    // Main orchestration: fetch events, then fetch points for each event (simple for loop, easy to extend)
    const processEventsAndPoints = async (filters: {
        airframe: string;
        eventDefinitionIds: number[];
        startDate: string;
        endDate: string;
        minLat: number;
        maxLat: number;
        minLon: number;
        maxLon: number;
    }) => {
        setLoading(true);
        setError(null);
        try {
            const events: any[] = await fetchEvents({
                ...filters,
                minSeverity,
                maxSeverity
            });
            if (!events.length) {
                setError("No events found with the given constraints!");
                setProximityEventPoints([]);
                setLoading(false);
                return [];
            }
            const allProximityEventPoints: ProximityEventPoints[] = [];
            const allSingleEventPoints: ProximityEventPoints[] = [];
            const processedPairs = new Set<string>();
            for (const event of events) {
                if (isProximityEvent(event)) {
                    // Deduplicate within the same event to prevent processing the same flight pair twice
                    // but allow different events with the same flight pair
                    const mainFlightId = event.flight_id;
                    const otherFlightId = event.other_flight_id;
                    const eventId = event.id;
                    const pairKey = `${eventId}-${Math.min(mainFlightId, otherFlightId)}-${Math.max(mainFlightId, otherFlightId)}`;
                    if (processedPairs.has(pairKey)) {
                        continue; // skip duplicate pair within the same event
                    }
                    processedPairs.add(pairKey);
                    const mainUrl = `/protected/heatmap_points_for_event_and_flight?event_id=${eventId}&flight_id=${mainFlightId}`;
                    const otherUrl = `/protected/heatmap_points_for_event_and_flight?event_id=${eventId}&flight_id=${otherFlightId}`;
                    
                    try {
                        const [mainResp, otherResp] = await Promise.all([
                            fetch(mainUrl, { credentials: 'include', headers: { 'Accept': 'application/json' } }),
                            fetch(otherUrl, { credentials: 'include', headers: { 'Accept': 'application/json' } })
                        ]);
                        
                        // Check if both responses are successful
                        if (!mainResp.ok || !otherResp.ok) {
                            console.warn(`Failed to fetch proximity points for event ${eventId}: main=${mainResp.status}, other=${otherResp.status}`);
                            continue; // Skip this event if we can't get its points
                        }
                        
                        const mainData = await mainResp.json();
                        const otherData = await otherResp.json();
                        allProximityEventPoints.push({
                            eventId: eventId,
                            eventDefinitionId: event.event_definition_id,
                            mainFlightId: mainFlightId,
                            otherFlightId: otherFlightId,
                            mainFlightPoints: mainData.points || mainData || [],
                            otherFlightPoints: otherData.points || otherData || [],
                            severity: event.severity,
                            airframe: event.airframe,
                            otherAirframe: event.otherAirframe
                        });
                    } catch (error) {
                        console.warn(`Error fetching proximity points for event ${eventId}:`, error);
                        continue; // Skip this event if there's an error
                    }
                } else {
                    // Regular event: fetch points for just the main flight
                    const mainFlightId = event.flight_id;
                    const eventId = event.id;
                    const mainUrl = `/protected/heatmap_points_for_event_and_flight?event_id=${eventId}&flight_id=${mainFlightId}`;

                    try {
                        const mainResp = await fetch(mainUrl, { credentials: 'include', headers: { 'Accept': 'application/json' } });
                        if (!mainResp.ok) {
                            console.warn(`Failed to fetch event points for event ${eventId}: ${mainResp.status} ${mainResp.statusText}`);
                            continue; // Skip this event if we can't get its points
                        }
                        const mainData = await mainResp.json();
                        allSingleEventPoints.push({
                            eventId: eventId,
                            eventDefinitionId: event.event_definition_id,
                            mainFlightId: mainFlightId,
                            otherFlightId: null, // Use null for regular events
                            mainFlightPoints: mainData.points || mainData || [],
                            otherFlightPoints: [],
                            severity: event.severity,
                            airframe: event.airframe,
                            otherAirframe: '' // Use empty string instead of null
                        });
                        console.log(`[processEventsAndPoints] Added to allSingleEventPoints. Total count:`, allSingleEventPoints.length);
                    } catch (error) {
                        console.warn(`Error fetching event points for event ${eventId}:`, error);
                        continue; // Skip this event if there's an error
                    }
                }
            }
            setProximityEventPoints([...allProximityEventPoints, ...allSingleEventPoints]);
            setLoading(false);
            
            // Calculate and set event statistics
            const allEvents = [...allProximityEventPoints, ...allSingleEventPoints];
            const statistics = calculateEventStatistics(allEvents);
            setEventStatistics(statistics);
            
            console.log('[processEventsAndPoints] Processing events. Proximity events:', allProximityEventPoints.length, 'Single events:', allSingleEventPoints.length);

            
            // Process both types of events together
            if (allProximityEventPoints.length > 0 && allSingleEventPoints.length > 0) {
                processMixedEventCoordinates(allProximityEventPoints, allSingleEventPoints, showGrid);
            } else if (allProximityEventPoints.length > 0) {
                // We have only proximity events - process them
                processProximityEventCoordinates(allProximityEventPoints, showGrid);
            } else if (allSingleEventPoints.length > 0) {
                // We have only regular events - process them
                processSingleEventCoordinates(allSingleEventPoints, showGrid);
            } else {
                console.log('[processEventsAndPoints] No events to process');
            }
            
            return [...allProximityEventPoints, ...allSingleEventPoints];
        } catch (error) {
            setError(error instanceof Error ? error.message : String(error));
            setLoading(false);
            setProximityEventPoints([]);
            return [];
        }
    };

    const handleDateChange = async () => {
        // Clear previous state immediately when starting a new search
        setProximityEventPoints([]);
        setError(null);
        clearMapLayers();
        
        // Gather checked events
        const selectedEvents = allEventNames.filter(e => eventChecked[e]);
        // Check for no events selected
        if (selectedEvents.length === 0) {
            setError('No events are selected. Please select at least one event.');
            return;
        }
        // Check for no area selected (all 0s or empty)
        const { minLat, maxLat, minLon, maxLon } = boxCoords;
        const areaNotSelected =
            (!minLat || !maxLat || !minLon || !maxLon) ||
            ([minLat, maxLat, minLon, maxLon].every(v => v === '0.000000'));
        if (areaNotSelected) {
            setError('No area is selected. Please select an area on the map.');
            return;
        }

        // Get event definition IDs for selected events
        const selectedDefinitionIds: number[] = [];
        for (const eventName of selectedEvents) {
            const definitionIds = eventNameToDefinitionIds[eventName] || [];
            selectedDefinitionIds.push(...definitionIds);
        }
        selectedDefinitionIds.sort();

        if (selectedDefinitionIds.length > 0) {
            await processEventsAndPoints({
                airframe,
                eventDefinitionIds: selectedDefinitionIds,
                startDate: `${startYear}-${startMonth.toString().padStart(2, '0')}-01`,
                endDate: `${endYear}-${endMonth.toString().padStart(2, '0')}-${new Date(endYear, endMonth, 0).getDate()}`,
                minLat: minLat ? parseFloat(minLat) : 0,
                maxLat: maxLat ? parseFloat(maxLat) : 0,
                minLon: minLon ? parseFloat(minLon) : 0,
                maxLon: maxLon ? parseFloat(maxLon) : 0
            });
        } else {
            setError('No valid event definition IDs selected.');
        }
        setDatesChanged(false);
        // Remove overlay feature (grayed out area) when updating
        if (overlayLayer && overlayFeature) {
            const source = overlayLayer.getSource();
            if (source) {
                source.removeFeature(overlayFeature);
            }
            setOverlayFeature(null);
        }
    };

    // Add coordinate input boxes to the TimeHeader row, with toggle above
    const latlonInputBoxes = (

        <div className="input-group">
            <div className="input-group-prepend">
                <div className="time-selector flex-col text-left px-3">

                    {/* Latitude Row */}
                    <div className="flex flex-row items-center justify-start text-sm gap-2 text-left">
                        
                        {/* Row Title */}
                        <span className="w-16">Latitude:</span>

                        {/* Min Lat. */}
                        <div className="flex flex-row gap-1">
                            <input
                                placeholder="Min"
                                className="
                                    w-16
                                    bg-transparent
                                    border-b-1 border-gray-400
                                    focus:ring-0 focus:border-gray-600 leading-7
                                "
                                type="text"
                                value={boxCoords.minLat}
                                readOnly
                            />
                            <span>°</span>
                        </div>

                        <span>-</span>

                        {/* Max Lat. */}
                        <div className="flex flex-row gap-1">
                            <input
                                placeholder="Max"
                                className="
                                    w-16
                                    bg-transparent
                                    border-b-1 border-gray-400
                                    focus:ring-0 focus:border-gray-600 leading-7
                                "
                                type="text"
                                value={boxCoords.maxLat}
                                readOnly
                            />
                            <span>°</span>
                        </div>

                    </div>


                    {/* Longitude Row */}
                    <div className="flex flex-row items-center justify-start text-sm gap-2 mt-1">
                        
                        {/* Row Title */}
                        <span className="w-16">Longitude:</span>

                        {/* Min Lon. */}
                        <div className="flex flex-row gap-1">
                            <input
                                placeholder="Min"
                                className="
                                    w-16
                                    bg-transparent
                                    border-b-1 border-gray-400
                                    focus:ring-0 focus:border-gray-600 leading-7
                                "
                                type="text"
                                value={boxCoords.minLon}
                                readOnly
                            />
                            <span>°</span>
                        </div>

                        <span>-</span>

                        {/* Max Lon. */}
                        <div className="flex flex-row gap-1">
                            <input
                                placeholder="Max"
                                className="
                                    w-16
                                    bg-transparent
                                    border-b-1 border-gray-400
                                    focus:ring-0 focus:border-gray-600 leading-7
                                "
                                type="text"
                                value={boxCoords.maxLon}
                                readOnly
                            />
                            <span>°</span>
                        </div>
                    </div>

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

    // =======================
    // SECTION: Event Columns Values Fetching
    // =======================
    /**
     * Fetches the relevant column names and their values for a given event ID, flight ID, and timestamp.
     * This will be used to show what data columns are relevant for this type of event and their values.
     */
    const fetchEventColumnsValues = async (eventId: number, flightId: number, timestamp: string, popupId?: string) => {
        try {
            const response = await fetch(`/protected/event_columns_values?event_id=${eventId}&flight_id=${flightId}&timestamp=${encodeURIComponent(timestamp)}`, {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            
            if (!response.ok) {
                console.warn(`Failed to fetch event columns values: ${response.status} ${response.statusText}`);
                return null;
            }
            
            const data = await response.json();
            
            // Update popup with column values if popupId is provided
            if (popupId && data.column_values) {
                const excludedColumns = new Set(["AltAGL", "AltMSL", "Latitude", "Longitude", "Lcl Date", "Lcl Time", "UTCOfst"]);
                const filteredValues: { [key: string]: any } = {};
                
                Object.entries(data.column_values).forEach(([col, value]) => {
                    if (!excludedColumns.has(col)) {
                        filteredValues[col] = value;
                    }
                });
                
                setOpenPopups(prevPopups => 
                    prevPopups.map(popup => 
                        popup.id === popupId 
                            ? { ...popup, data: { ...popup.data, columnValues: filteredValues } }
                            : popup
                    )
                );
            }
            
            return data;
        } catch (error) {
            console.error('Error fetching event columns values:', error);
            return null;
        }
    };

    /**
     * Fetches relevant column names and values for multiple events and combines them.
     * This handles cases where multiple events are associated with the same point.
     */
    const fetchMultipleEventColumnsValues = async (events: any[], flightId: number, timestamp: string, popupId: string) => {
        try {
            const allColumns = new Set<string>();
            const allValues: { [key: string]: any } = {};
            const eventDetails: any[] = [];
            
            // Columns to exclude (already shown in popup)
            const excludedColumns = new Set(["AltAGL", "AltMSL", "Latitude", "Longitude", "Lcl Date", "Lcl Time", "UTCOfst"]);
            
            // Fetch columns and values for each event
            for (const event of events) {
                if (event.eventId && flightId) {
                    const data = await fetchEventColumnsValues(event.eventId, flightId, timestamp);
                    if (data && data.column_names && Array.isArray(data.column_names) && data.column_values) {
                        // Add columns to the set to avoid duplicates, excluding specified columns
                        data.column_names.forEach((col: string) => {
                            if (!excludedColumns.has(col)) {
                                allColumns.add(col);
                            }
                        });
                        
                        // Store values (later events will override earlier ones for same column)
                        Object.entries(data.column_values).forEach(([col, value]) => {
                            if (!excludedColumns.has(col)) {
                                allValues[col] = value;
                            }
                        });
                        
                        eventDetails.push({
                            eventId: event.eventId,
                            eventType: getEventTypeName(event.eventDefinitionId),
                            columns: data.column_names,
                            values: data.column_values
                        });
                    }
                }
            }
            
            // Update the popup with column values
            if (allColumns.size > 0) {
                setOpenPopups(prevPopups => 
                    prevPopups.map(popup => 
                        popup.id === popupId 
                            ? { ...popup, data: { ...popup.data, columnValues: allValues } }
                            : popup
                    )
                );
            }
            
            return { allColumns: Array.from(allColumns), allValues: allValues, eventDetails };
        } catch (error) {
            console.error('Error fetching multiple event columns values:', error);
            return null;
        }
    };

    /**
     * Calculates event statistics from the current proximity event points
     */
    const calculateEventStatistics = (events: ProximityEventPoints[]): EventStatistics => {
        const eventsByType: { [eventType: string]: number } = {};
        let totalEvents = 0;

        events.forEach(event => {
            const eventTypeName = getEventTypeName(event.eventDefinitionId);
            eventsByType[eventTypeName] = (eventsByType[eventTypeName] || 0) + 1;
            totalEvents++;
        });

        return {
            totalEvents,
            eventsByType
        };
    };

    // =======================
    // SECTION: Information Window Component
    // =======================
    /**
     * Information window component that displays event statistics and user guidance
     */
    const selectAreaInstructions = (
        <span className='italic'>
            {
                (userOS==="Linux" || userOS==="Windows")
                ?
                (<span>Ctrl+Drag</span>)
                :
                (<span>⌘+Drag</span>)
            }
        </span>
    );
    const InformationWindow = () => (
        <div
            className="
                w-[280px]
                absolute top-4 left-4
                z-1
                p-4
                rounded-lg
                bg-[var(--c_card_header_bg_opaque)]
                shadow-md
                border-gray-200 border-1
            "
        >
            
            {/* Events Found */}
            <div
                className="
                    text-base font-semibold
                    text-[var(--c_text)]
                    border-gray-200 border-b-1
                    mb-3
                    pb-2 pr-2
                "
            >
                Events Found: {eventStatistics.totalEvents}
            </div>

            {/* Total Events by Type */}
            {eventStatistics.totalEvents > 0 && (
                <div className="mb-4">
                    <div className="max-h-[200px] overflow-y-auto">
                        {Object.entries(eventStatistics.eventsByType)
                            .sort(([,a], [,b]) => b - a) // Sort by count descending
                            .map(([eventType, count]) => (
                                <div key={eventType} style={{
                                    padding: '4px 0',
                                    fontSize: '13px',
                                    borderBottom: '1px solid #f0f0f0',
                                    paddingRight: '8px'
                                }}>
                                    <div style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center'
                                    }}>
                                        <span
                                            className="text-[var(--c_text_button)]"
                                            style={{
                                                flex: 1,
                                                marginRight: '12px',
                                                wordBreak: 'break-word'
                                            }}
                                        >
                                            {eventType}
                                        </span>
                                        <span style={{
                                            fontWeight: '600',
                                            color: '#007bff',
                                            minWidth: '30px',
                                            textAlign: 'right'
                                        }}>
                                            {count}
                                        </span>
                                    </div>
                                </div>
                            ))}
                    </div>
                </div>
            )}
            
            {/* Map Controls */}
            <div
                className="
                    text-xs
                    leading-4
                    p-3
                    bg-[var(--c_dropdown_bg)]
                    text-[var(--c_text_button)]
                "
            style={{
                borderRadius: '6px',
                border: '1px solid #e9ecef'
            }}>
                <button
                    className="
                        w-full
                        flex flex-row justify-between items-center
                        group
                        cursor-pointer
                        text-[var(--c_text_button_alt)]
                    " 
                    style={{ 
                        fontWeight: '500', 
                        cursor: 'pointer',
                        position: 'relative'
                    }}
                    onClick={toggleNavigationTips}
                >

                    {/* Map Controls Title */}
                    <span className="
                        text-base
                        flex flex-row justify-start items-center gap-2
                    ">
                        <span>💡</span>
                        <span>Map Controls</span>
                    </span>

                    {/* Toggle Button Icon */}
                    <i className={`fa ${navigationTipsExpanded ? 'fa-chevron-down' : 'fa-chevron-right'} p-1
                        scale-100 group-hover:scale-125
                        transition-transform duration-200 ease-in-out
                    `}/>

                </button>
                {navigationTipsExpanded && (
                    <ul
                        className="m-0 mt-3"
                        style={{
                        paddingLeft: '16px',
                        listStyleType: 'disc'
                    }}>
                        <li style={{ marginBottom: '2px' }}>
                            Use <b>{selectAreaInstructions}</b> to select map areas
                        </li>
                        <li style={{ marginBottom: '2px' }}>
                            Toggle switch in the right corner to change heatmap view to grid view
                        </li>
                        <li style={{ marginBottom: '2px' }}>
                            Zoom in to level 15+ to see individual event markers
                        </li>
                        <li style={{ marginBottom: '2px' }}>
                            Click markers to view detailed event information
                        </li>
                        <li style={{ marginBottom: '2px' }}>
                            Select 2 points to calculate distances
                        </li>
                    </ul>
                )}
            </div>
        </div>

    );

    const timeHeader = (
        <TimeHeader
            name="Event Heat Map"
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
            severitySliderComponent={null}
            datesOrAirframeChanged={datesOrAirframeChanged}
        >

            {/* Lat & Lon Selectors */}
            {latlonInputBoxes}

            {/* Severity Slider */}
            <div style={{ width: 'auto', margin: 0, padding: 0 }}>
                {severitySlider}
            </div>

        </TimeHeader>
    );

    // Wrap the main content in a full-height flex container
    return (
        <div style={{ overflowX: 'hidden', display: 'flex', flexDirection: 'column', height: '100vh' }}>

            {/* Scoped CSS override for Bootstrap grid spacing and dropdown z-index */}
            <style>
            {`
                #heat-map-top-menu .row,
                #heat-map-top-menu .form-row,
                #heat-map-top-menu .col-auto,
                #heat-map-top-menu .col-auto:not(:last-child) {
                    margin-left: 0 !important;
                    margin-right: 0 !important;
                    padding-left: 0 !important;
                    padding-right: 0 !important;
                }
                
                /* Ensure all dropdowns appear above the InformationWindow */
                .dropdown-menu {
                    z-index: 9999 !important;
                    position: fixed !important;
                }
                
                /* Also target Bootstrap's specific dropdown classes */
                .dropdown-menu.show {
                    z-index: 9999 !important;
                    position: fixed !important;
                }
                
                /* Ensure the dropdown backdrop doesn't interfere */
                .dropdown-backdrop {
                    z-index: 9998 !important;
                }
                
                /* Force dropdowns to be above everything */
                .dropdown {
                    position: relative !important;
                }
            `}
            </style>

            {/* Navbar */}
            <div style={{ flex: '0 0 auto' }}>
                <SignedInNavbar activePage="heat_map" mapLayerDropdown={mapLayerDropdown} />
            </div>

            {/* Main Content Container */}
            <div className="p-2" style={{overflowY: "auto", flex: "1 1 auto"}}>

                {/* Error Banner */}
                {error && (
                    <div className="alert alert-danger p-2 mx-3 mb-3 mt-2 flex flex-row items-center justify-start gap-2 z-2000">
                        <strong>Error:</strong>
                        <span>{error}</span>
                        <button className="ml-auto btn btn-link p-0" style={{fontSize: 18}} onClick={() => setError(null)}>
                            <i className="fa fa-times p-1 cursor-pointer"></i>
                        </button>
                    </div>
                )}


                {/* Loading Indicator */}
                {loading && (
                    <div className="alert alert-info p-2 mx-3 mb-3 mt-2 flex flex-row items-center justify-start gap-2 z-2000">
                        <div className="spinner-border spinner-border-sm" role="status">
                            <span className="sr-only">Loading...</span>
                        </div>
                        <span>Loading proximity events...</span>
                    </div>
                )}


                {/* Main Content Area */}
                <div style={{ flex: '1 1 auto', display: 'flex', flexDirection: 'column', height: '100%' }}>
                    <div className="container-fluid" style={{ flex: '1 1 auto', height: '100%' }}>
                        <div className="row" style={{ height: '100%' }}>
                            <div className="col-lg-12" style={{ height: '100%' }}>
                                <div className="card" style={{ height: '100%', display: 'flex', flexDirection: 'column', flex: '1 1 auto' }}>

                                    {/* Time Header */}
                                    {timeHeader}

                                    <div className="card-body" style={{ padding: 0, background: 'transparent', flex: '1 1 auto', minHeight: 0, display: 'flex', position: 'relative' }}>
                                        <div className="row" style={{ margin: 0, padding: 0, background: 'transparent', flex: 1, height: '100%' }}>

                                            {/* Event Checkboxes Column */}
                                            <div className="col-lg-2 overflow-y-auto" style={{ padding: 0, margin: 0, background: 'transparent', height: '100%', paddingLeft: 16, paddingTop: 16 }}>
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

                                            {/* Map and Information Window Column */}
                                            <div className="col-lg-10" style={{ padding: 0, margin: 0, background: 'transparent', height: '100%', position: 'relative' }}>
                                                {gridToggleSwitch}
                                                <div ref={mapRef} style={{ width: '100%', height: '100%', background: 'transparent', margin: 0, padding: 0, position: 'relative' }} />
                                                <InformationWindow />

                                                {/* Render all open popups as overlays */}
                                                <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, pointerEvents: 'none', zIndex: 1000 }}>
                                                    {openPopups.map((popup) => {
                                                        const isGrabbing = (draggedPopupId === popup.id);
                                                        const isRecent = (recentPopupId === popup.id);

                                                        return (
                                                            <div
                                                                key={popup.id}
                                                                className={`
                                                                    bg-[var(--c_card_header_bg_opaque)]
                                                                    ol-popup ${isRecent ? 'z-110' : 'z-100'}
                                                                    px-3 pb-3 pt-2
                                                                `}
                                                                style={{
                                                                    position: 'absolute',
                                                                    boxShadow: '0 1px 4px rgba(0,0,0,0.2)',
                                                                    borderRadius: '10px',
                                                                    border: '1px solid #cccccc',
                                                                    minWidth: '200px',
                                                                    left: popup.position?.left ?? 100,
                                                                    top: popup.position?.top ?? 100,
                                                                    transform: 'none',
                                                                    zIndex: isRecent ? 110 : 100,
                                                                    pointerEvents: 'auto',
                                                                }}
                                                            >
                                                                
                                                                {/* Grab & Drag Area */}
                                                                <div
                                                                    className={`
                                                                        group
                                                                        ${isGrabbing ? 'cursor-grabbing' : 'cursor-grab'}
                                                                        mb-2
                                                                    `}
                                                                    onMouseDown={e => { console.log('Popup mouse down', popup.id, e); handlePopupMouseDown(e, popup.id); }}
                                                                >
                                                                    <span className={`
                                                                        fa fa-arrows
                                                                        ${isGrabbing ? 'opacity-100 select-none' : 'opacity-25 group-hover:opacity-100 select-auto'}
                                                                        mr-2
                                                                        scale-100 group-hover:scale-125
                                                                        transition-all duration-200 ease-in-out
                                                                    `}/>
                                                                </div>

                                                                {/* Event Details */}
                                                                <div>

                                                                    {/* Event Type */}
                                                                    <div>{popup.data.eventType || 'Event'}</div>

                                                                    {/* Event Types */}
                                                                    {popup.data.eventTypes && popup.data.eventTypes.length > 1 && (
                                                                        <div style={{ fontSize: '0.7em', color: '#888', marginTop: '1px', fontStyle: 'italic' }}>
                                                                            {popup.data.eventTypes.join(', ')}
                                                                        </div>
                                                                    )}

                                                                    {/* Event ID */}
                                                                    <div className="text-[var(--c_text_button)]" style={{ fontSize: '0.8em', marginTop: '2px' }}>
                                                                        Event ID: {popup.data.eventId || 'N/A'}
                                                                    </div>

                                                                    {/* Event Date & Time */}
                                                                    <div className="text-[var(--c_text_button)]" style={{ fontSize: '0.7em', marginTop: '1px' }}>
                                                                        {popup.data.time ?? '...'}
                                                                    </div>
                                                                </div>

                                                                <a
                                                                    href="#"
                                                                    className="
                                                                        absolute top-2 right-3
                                                                        ol-popup-closer no-underline
                                                                        scale-100 hover:scale-125 transition-transform duration-200 ease-in-out
                                                                    "
                                                                    onClick={e => {
                                                                        e.preventDefault();
                                                                        setOpenPopups(prevPopups =>
                                                                            prevPopups.filter(p => p.id !== popup.id)
                                                                        );
                                                                        setSelectedPoints(prevPoints => {
                                                                            const newSelectedPoints = prevPoints.filter(p => p.id !== popup.id);

                                                                            // Recalculate distances after removing point
                                                                            if (newSelectedPoints.length === 2) {
                                                                                const [point1, point2] = newSelectedPoints;
                                                                                const result = calculateDistanceBetweenPoints(
                                                                                    point1.latitude, point1.longitude, point1.altitude,
                                                                                    point2.latitude, point2.longitude, point2.altitude
                                                                                );
                                                                                setDistances({ lateral: result.lateral, euclidean: result.euclidean });
                                                                            } else {
                                                                                setDistances({ lateral: null, euclidean: null });
                                                                            }

                                                                            return newSelectedPoints;
                                                                        });
                                                                    }}
                                                                >
                                                                    <i className="fa fa-times text-[var(--c_text)]" />
                                                                </a>
                                                                <div>
                                                                    <hr />
                                                                    <div><strong>Latitude: </strong> {popup.data.latitude !== null && popup.data.latitude !== undefined ? Number(popup.data.latitude).toFixed(5) : '...'}°</div>
                                                                    <div><strong>Longitude: </strong> {popup.data.longitude !== null && popup.data.longitude !== undefined ? Number(popup.data.longitude).toFixed(5) : '...'}°</div>
                                                                    <div><strong>Altitude (AGL): </strong> {popup.data.altitude !== null && popup.data.altitude !== undefined ? `${popup.data.altitude.toFixed(0)} ft` : '...'}</div>
                                                                    <hr />
                                                                    <div><strong>Flight ID: </strong>{popup.data.flightId ?? '...'}</div>
                                                                    <div><strong>Airframe: </strong>{popup.data.flightAirframe ?? '...'}</div>
                                                                    {popup.data.otherFlightId && popup.data.otherFlightId !== null && popup.data.otherFlightId !== 0 && popup.data.otherFlightId !== '0' && (
                                                                        <>
                                                                            <hr />
                                                                            <div><strong>Other Flight ID: </strong>{popup.data.otherFlightId ?? '...'}</div>
                                                                            <div><strong>Other Airframe: </strong>{popup.data.otherFlightAirframe ?? '...'}</div>
                                                                        </>
                                                                    )}
                                                                    <hr />
                                                                    <div><strong>Severity: </strong>{parseFloat(String(popup.data.severity)).toFixed(2)}</div>
                                                                    {popup.data.columnValues && Object.keys(popup.data.columnValues).length > 0 && (
                                                                        <>
                                                                            <hr />
                                                                            <div><strong>Event Data:</strong></div>
                                                                            {Object.entries(popup.data.columnValues).map(([columnName, value]) => (
                                                                                <div key={columnName}>
                                                                                    <strong>{columnName}: </strong>
                                                                                    {value !== null && value !== undefined ? 
                                                                                        (typeof value === 'number' ? value.toFixed(2) : String(value)) : 
                                                                                        'N/A'
                                                                                    }
                                                                                </div>
                                                                            ))}
                                                                        </>
                                                                    )}
                                                                    {distances.lateral !== null && distances.euclidean !== null && selectedPoints.length === 2 && (
                                                                        <>
                                                                            <hr />
                                                                            <div><strong>Lateral Distance: </strong>{distances.lateral.toFixed(0)} ft</div>
                                                                            <div><strong>Euclidean (Slant) Distance: </strong>{distances.euclidean.toFixed(0)} ft</div>
                                                                            <div><strong>Vertical Separation: </strong>{Math.abs(selectedPoints[0].altitude - selectedPoints[1].altitude).toFixed(0)} ft</div>
                                                                        </>
                                                                    )}
                                                                </div>
                                                            </div>
                                                        );
                                                    })}
                                                </div>
                                            </div>

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

// =======================
// SECTION: Mount React Component
// =======================
const container = document.querySelector("#heat-map-page");
if (container) {
    const root = createRoot(container);
    root.render(<HeatMapPage />);
} 