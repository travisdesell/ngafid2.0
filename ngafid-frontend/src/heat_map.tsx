// =======================
// HEAT MAP PAGE COMPONENT
// =======================
//
// This file implements the Heat Map page for the NGAFID frontend. It provides:
//  - Event selection and filtering UI
//  - OpenLayers map with multiple base layers
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
import VectorLayer from 'ol/layer/Vector';
import VectorSource from 'ol/source/Vector';
import Feature from 'ol/Feature';
import Polygon from 'ol/geom/Polygon';
import Style from 'ol/style/Style';
import Fill from 'ol/style/Fill';
import Heatmap from 'ol/layer/Heatmap';
import Point from 'ol/geom/Point';
import Icon from 'ol/style/Icon';
import { getTransform } from 'ol/proj';
import WebGLVectorLayer from 'ol/layer/WebGLVector';
import MultiPolygon from 'ol/geom/MultiPolygon';

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

// Proximity event interface
interface ProximityEvent {
    id: number;
    flightId: number;
    otherFlightId: number;
    startTime: string;
    endTime: string;
    severity: number;
    flightAirframe: string;
    otherFlightAirframe: string;
}

// Constants for proximity functionality
const ICON_IMAGE_BLACK = new Icon({
    src: '/images/black-point.png',
    scale: 0.05,
    anchor: [0.5, 0.5],
});

const ICON_IMAGE_RED = new Icon({
    src: '/images/red-point.png',
    scale: 0.05,
    anchor: [0.5, 0.5],
});

const BLACK_POINT_STYLE = new Style({
    image: ICON_IMAGE_BLACK
});

const RED_POINT_STYLE = new Style({
    image: ICON_IMAGE_RED
});

// Interpolate color from green to red
function interpolateColor(value: number) {
    // value: 0 (green) to 1 (red)
    const r = Math.round(255 * value);
    const g = Math.round(255 * (1 - value));
    return `rgba(${r},${g},0,0.6)`;
}

// Utility functions for distance calculation
function toRadians(degrees: number): number {
    return degrees * (Math.PI / 180);
}

/**
 * Calculates the lateral (surface) and euclidean (3D) distance between two points.
 * @param lat1 Latitude of point 1 in degrees
 * @param lon1 Longitude of point 1 in degrees
 * @param alt1 Altitude of point 1 in feet (optional, default 0)
 * @param lat2 Latitude of point 2 in degrees
 * @param lon2 Longitude of point 2 in degrees
 * @param alt2 Altitude of point 2 in feet (optional, default 0)
 * @returns { lateral: feet, euclidean: feet }
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

// Popup content data interface
interface PopupContentData {
    time: string | null;
    latitude: number | null;
    longitude: number | null;
    altitude: number | null;
    flightId: string | null;
    flightAirframe: string | null;
    otherFlightId: string | null;
    otherFlightAirframe: string | null;
    severity: string | null;
}

// Event type registry for handling different event types
interface EventTypeHandler {
    name: string;
    endpoint: string;
    processFunction: (events: any[], map: Map | null, layers: any) => Promise<void>;
}

const azureMapsKey = process.env.AZURE_MAPS_KEY;

// Marker visibility threshold (same as proximity map)
const MARKER_VISIBILITY_ZOOM_THRESHOLD = 12;

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

// Mapping from event display name to event definition IDs
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

// Use the global airframes variable injected by the backend, if available
let airframesList = (typeof airframes !== 'undefined' && Array.isArray(airframes)) ? [...airframes] : [];
// Ensure 'All Airframes' is at the front
if (!airframesList.includes('All Airframes')) {
    airframesList.unshift('All Airframes');
}
// Remove 'Garmin Flight Display' if present
const gfdIndex = airframesList.indexOf('Garmin Flight Display');
if (gfdIndex !== -1) airframesList.splice(gfdIndex, 1);

// =======================
// SECTION: Main HeatMapPage Component
// =======================
const HeatMapPage: React.FC = () => {
    // =======================
    // STATE HOOKS
    // =======================
    // All state variables for UI, map, events, popups, etc.


    const [airframes, setAirframes] = useState<string[]>(airframesList);
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
    const [showGrid, setShowGrid] = useState<boolean>(false);
    const [overlayLayer, setOverlayLayer] = useState<VectorLayer<VectorSource> | null>(null);
    const [overlayFeature, setOverlayFeature] = useState<Feature<Polygon> | null>(null);
    const [error, setError] = useState<string | null>(null);

    // Proximity functionality state
    const [proximityEvents, setProximityEvents] = useState<ProximityEvent[]>([]);
    const [loading, setLoading] = useState<boolean>(false);
    const [heatmapLayer1, setHeatmapLayer1] = useState<Heatmap | null>(null);
    const [heatmapLayer2, setHeatmapLayer2] = useState<Heatmap | null>(null);
    const [markerSource, setMarkerSource] = useState<VectorSource | null>(null);
    const [markerLayer, setMarkerLayer] = useState<VectorLayer | null>(null);
    const [gridLayer, setGridLayer] = useState<WebGLVectorLayer | null>(null);
    const [gridSource, setGridSource] = useState<VectorSource | null>(null);
    const [previousParams, setPreviousParams] = useState<{
        airframe: string;
        startYear: number;
        startMonth: number;
        endYear: number;
        endMonth: number;
        minSeverity: number;
        maxSeverity: number;
        boxCoords: { minLat: string, maxLat: string, minLon: string, maxLon: string };
    } | null>(null);

    // Popup and distance calculation state
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


    // Data structure for proximity event points
    interface ProximityEventPoints {
        eventId: number;
        mainFlightId: number;
        otherFlightId: number;
        mainFlightPoints: any[];
        otherFlightPoints: any[];
        severity: number;
        airframe: string;
        otherAirframe: string;
    }
    // Add a new state for proximity event points
    const [proximityEventPoints, setProximityEventPoints] = useState<ProximityEventPoints[]>([]);

    // =======================
    // POPUP DRAGGING FUNCTIONALITY
    // =======================
    // This section implements draggable popups (windows) for event details.
    // It uses refs to track drag state and ensure global event handlers always have the latest values.
    // This is necessary in React functional components to avoid stale closures when using global listeners.
    //
    // - dragStart: stores the initial mouse position when dragging starts
    // - popupStart: stores the initial popup position when dragging starts
    // - draggedPopupIdRef: always holds the current popup being dragged (for global handlers)
    // - draggedPopupId: state for re-rendering and UI feedback
    // - handlePopupMouseDown: starts the drag, attaches global listeners
    // - handleMouseMove: updates popup position as mouse moves
    // - handleMouseUp: ends the drag, removes listeners

    const dragStart = useRef<{ x: number, y: number }>({ x: 0, y: 0 });
    const popupStart = useRef<{ left: number, top: number }>({ left: 0, top: 0 });
    const [draggedPopupId, setDraggedPopupId] = useState<string | null>(null);
    const draggedPopupIdRef = useRef<string | null>(null);
    const [recentPopupId, setRecentPopupId] = useState<string | null>(null);

    // Called on popup header mousedown: starts drag and attaches listeners
    const handlePopupMouseDown = (e: React.MouseEvent, popupId: string) => {
        console.log('handlePopupMouseDown called', popupId, e);
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
        console.log('Added global listeners for mousemove and mouseup');
        console.log('Drag start set to', dragStart.current, 'Popup start set to', popupStart.current);
        console.log('draggedPopupIdRef set to', draggedPopupIdRef.current);
    };

    // Global mousemove handler: updates popup position if dragging
    const handleMouseMove = useCallback((e: MouseEvent) => {
        if (!draggedPopupIdRef.current || !mapRef.current) {
            console.log('handleMouseMove: not dragging or mapRef missing', { draggedPopupId: draggedPopupIdRef.current, mapRef: mapRef.current });
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
        console.log('handleMouseMove', { dx, dy, left, top, mapRect });
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
        console.log('handleMouseUp');
        setDraggedPopupId(null);
        draggedPopupIdRef.current = null;
        window.removeEventListener('mousemove', handleMouseMove);
        window.removeEventListener('mouseup', handleMouseUp);
    }, [handleMouseMove]);

    useEffect(() => {
        return () => {
            window.removeEventListener('mousemove', handleMouseMove);
            window.removeEventListener('mouseup', handleMouseUp);
        };
    }, []);
    // =================== END POPUP DRAGGING FUNCTIONALITY ===================

    // Map ref must be defined before using in handlers
    const mapRef = React.useRef<HTMLDivElement | null>(null);

    // Now define handlePopupMouseDown after mapRef and the useCallback hooks
    // Function to clear all map layers
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

        // Hide layers
        if (heatmapLayer1) heatmapLayer1.setVisible(false);
        if (heatmapLayer2) heatmapLayer2.setVisible(false);
        if (gridLayer) gridLayer.setVisible(false);
    };

    // Function to check if parameters have changed
    const haveParametersChanged = () => {
        if (!previousParams) {
            console.log('No previous params, considering changed');
            return true; // First time, so consider it changed
        }

        const currentParams = {
            airframe,
            startYear,
            startMonth,
            endYear,
            endMonth,
            minSeverity,
            maxSeverity,
            boxCoords
        };

        const changed = (
            previousParams.airframe !== currentParams.airframe ||
            previousParams.startYear !== currentParams.startYear ||
            previousParams.startMonth !== currentParams.startMonth ||
            previousParams.endYear !== currentParams.endYear ||
            previousParams.endMonth !== currentParams.endMonth ||
            previousParams.minSeverity !== currentParams.minSeverity ||
            previousParams.maxSeverity !== currentParams.maxSeverity ||
            previousParams.boxCoords.minLat !== currentParams.boxCoords.minLat ||
            previousParams.boxCoords.maxLat !== currentParams.boxCoords.maxLat ||
            previousParams.boxCoords.minLon !== currentParams.boxCoords.minLon ||
            previousParams.boxCoords.maxLon !== currentParams.boxCoords.maxLon
        );

        console.log('Parameter change check:', {
            previous: previousParams,
            current: currentParams,
            changed: changed
        });

        return changed;
    };

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

    // =======================
    // SECTION: Grid/Heatmap Toggle and Rendering
    // =======================
    /**
     * This section handles the logic for toggling between heatmap and grid visualizations
     * for proximity events on the map. It includes:
     *   - The showGrid state (true = grid, false = heatmap)
     *   - The handleGridToggle function, which toggles the state and triggers re-rendering
     *   - The processProximityEventCoordinates function, which renders either the heatmap or grid
     *     based on the current toggle state and the loaded proximity events.
     *
     * Usage:
     *   - When the user toggles the switch, handleGridToggle is called.
     *   - This updates showGrid and immediately calls processProximityEventCoordinates
     *     with the current proximityEvents and the new toggle value.
     *   - processProximityEventCoordinates clears previous map layers and renders either
     *     the heatmap (default) or a grid-based density map overlay, depending on showGrid.
     *   - The grid overlay is built using OpenLayers WebGLVectorLayer and colored cells.
     *
     * This logic is modeled after the working implementation in proximity_map.tsx.
     */

        // Handle grid/heatmap toggle for both proximity and regular events
    const handleGridToggle = () => {
            // Toggle the showGrid state and re-render the map overlay
            const newShowGrid = !showGrid;
            setShowGrid(newShowGrid);
            // Reprocess events if we have any, using the new value
            if (proximityEventPoints.length > 0) {
                // Check if we have proximity events or regular events
                const hasProximityEvents = proximityEventPoints.some(event => event.otherFlightId !== 0);
                if (hasProximityEvents) {
                    processProximityEventCoordinates(proximityEventPoints, newShowGrid);
                } else {
                    processSingleEventCoordinates(proximityEventPoints, newShowGrid);
                }
            }
        };

    // Grid/Heatmap icon-only toggle, highlight active icon
    const gridToggleSwitch = (
        <div style={{
            position: 'absolute',
            top: 16,
            right: 16,
            zIndex: 1000,
            display: 'flex',
            alignItems: 'center',
            gap: 10,
            background: 'white',
            padding: '4px 10px',
            borderRadius: 5,
            boxShadow: '0 1px 4px rgba(0,0,0,0.10)',
            fontSize: 18,
            minWidth: 0
        }}>
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
                        const hasProximityEvents = proximityEventPoints.some(event => event.otherFlightId !== 0);
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
                        const hasProximityEvents = proximityEventPoints.some(event => event.otherFlightId !== 0);
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
        console.log('[processProximityEventCoordinates] called. shouldShowGrid:', shouldShowGrid);

        // Clear heatmap sources
        heatmapLayer1.getSource()!.clear();
        heatmapLayer2.getSource()!.clear();
        if (markerSource) markerSource.clear();

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
                heatmapFeature.set('weight', 0.2);
                heatmapLayer1.getSource()!.addFeature(heatmapFeature);
                const marker = new Feature({ geometry: new Point(olCoord) });
                marker.setStyle(RED_POINT_STYLE);
                marker.setProperties({
                    isMarker: true,
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
                markerSource.addFeature(marker);
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
            // BLACK: otherFlightPoints
            // Ensure otherFlightPoints is an array
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
                heatmapFeature.set('weight', 0.2);
                heatmapLayer2.getSource()!.addFeature(heatmapFeature);
                const marker = new Feature({ geometry: new Point(olCoord) });
                marker.setStyle(BLACK_POINT_STYLE);
                marker.setProperties({
                    isMarker: true,
                    flightId: eventPoints.otherFlightId,
                    otherFlightId: eventPoints.mainFlightId,
                    time: point.timestamp,
                    flightAirframe: eventPoints.otherAirframe,
                    otherFlightAirframe: eventPoints.airframe,
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                markerSource.addFeature(marker);
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
        }

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
            console.log('[processProximityEventCoordinates] Rendering grid...');
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
                console.log('[processProximityEventCoordinates] gridLayer set to visible:', gridLayer.getVisible());
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
                console.log('[processProximityEventCoordinates] gridLayer set to visible:', gridLayer.getVisible());
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
        console.log('[processSingleEventCoordinates] called. shouldShowGrid:', shouldShowGrid);
        
        // Clear heatmap and marker sources
        heatmapLayer1.getSource()!.clear();
        if (markerSource) markerSource.clear();

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
                heatmapFeature.set('weight', 0.2);
                heatmapLayer1.getSource()!.addFeature(heatmapFeature);
                const marker = new Feature({ geometry: new Point(olCoord) });
                marker.setStyle(RED_POINT_STYLE);
                marker.setProperties({
                    isMarker: true,
                    flightId: eventPoints.mainFlightId,
                    otherFlightId: 0, // Single events don't have other flights
                    time: point.timestamp,
                    flightAirframe: eventPoints.airframe,
                    otherFlightAirframe: '',
                    severity: eventPoints.severity,
                    altitudeAgl: point.altitude_agl,
                    latitude: point.latitude,
                    longitude: point.longitude
                });
                markerSource.addFeature(marker);
                allPoints.push({ latitude: point.latitude, longitude: point.longitude });
            }
        }

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
            console.log('[processSingleEventCoordinates] Rendering grid...');
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

                    if (count > 0) {
                        console.log(`[Grid Debug] Cell (${latVal},${lonVal}) count:`, count, 'intensity:', intensity, 'color:', color);
                    }

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
            console.log('[processSingleEventCoordinates] Added grid features:', features.length, 'Grid source now has:', gridSource.getFeatures().length);

            // Hide heatmap layers
            heatmapLayer1.setVisible(false);

            // Show grid layer
            if (gridLayer) {
                gridLayer.setVisible(true);
                console.log('[processSingleEventCoordinates] gridLayer set to visible:', gridLayer.getVisible());
            } else {
                console.warn('[processSingleEventCoordinates] gridLayer is null!');
            }
        } else {
            // Show heatmap layers
            if (heatmapLayer1) heatmapLayer1.setVisible(true);
            // Hide grid layer if present
            if (gridLayer) {
                gridLayer.setVisible(false);
                console.log('[processSingleEventCoordinates] gridLayer set to visible:', gridLayer.getVisible());
            }
        }
    };

    // Map initialization and layer switching
    useEffect(() => {
        if (mapRef.current && !map) {
            // Create layers for each style
            const layers = mapLayerOptions.map(opt => new TileLayer({
                visible: opt.value === mapStyle,
                source: new XYZ({ url: opt.url() })
            }));

            // Create heatmap sources and layers
            const heatmapSource1 = new VectorSource();
            const heatmapSource2 = new VectorSource();
            const markerSource = new VectorSource();

            const heatmapLayer1 = new Heatmap({
                source: heatmapSource1,
                blur: 3,
                radius: 2,
                opacity: 0.5,
                gradient: [
                    'rgba(0,255,0,0)',
                    'rgba(0,255,0,1)',
                    'rgba(255,255,0,1)',
                    'rgba(255,165,0,1)',
                    'rgba(255,0,0,1)'
                ]
            });
            heatmapLayer1.set('interactive', false);

            const heatmapLayer2 = new Heatmap({
                source: heatmapSource2,
                blur: 3,
                radius: 2,
                opacity: 0.5,
                gradient: [
                    'rgba(0,255,0,0)',
                    'rgba(0,255,0,1)',
                    'rgba(255,255,0,1)',
                    'rgba(255,165,0,1)',
                    'rgba(255,0,0,1)'
                ]
            });
            heatmapLayer2.set('interactive', false);

            // Create marker layer for individual points
            const markerLayer = new VectorLayer({
                source: markerSource,
                style: (feature) => {
                    const props = feature.getProperties();

                    if (!props.isMarker) return undefined;
                    
                    // For proximity events: red for main flight, black for other flight
                    // For regular events: red for all points
                    if (props.otherFlightId) {
                        // Proximity event - use red/black based on which flight this point belongs to
                        return props.flightId === props.otherFlightId ? RED_POINT_STYLE : BLACK_POINT_STYLE;
                    } else {
                        // Regular event - use red for all points
                        return RED_POINT_STYLE;
                    }
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
             * This logic matches the robust approach from proximity_map.tsx and ensures consistent user experience.
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
                    console.log('props.severity:', props.severity, 'type:', typeof props.severity, 'props:', props);
                    // Generate popup ID based on whether it's a proximity event or regular event
                    const popupId = props.otherFlightId 
                        ? `${props.flightId}-${props.otherFlightId}-${props.time}` 
                        : `${props.flightId}-${props.time}`;
                    // Prevent duplicate popups for the same marker
                    if (openPopups.some(p => p.id === popupId)) return;
                    // Use callback form to get latest selectedPoints
                    setSelectedPoints(prevSelectedPoints => {
                        if (prevSelectedPoints.length >= 2) {
                            console.log('Maximum 2 points allowed. Close one to pick another.');
                            return prevSelectedPoints;
                        }
                        // Add popup only if we can add a point
                        setOpenPopups(prevPopups => {
                            if (prevPopups.length >= 2) return prevPopups;
                            const popupContentData = {
                                time: new Date(props.time).toLocaleString(),
                                latitude: props.latitude !== undefined ? props.latitude : null,
                                longitude: props.longitude !== undefined ? props.longitude : null,
                                altitude: props.altitudeAgl !== undefined ? props.altitudeAgl : null,
                                flightId: props.flightId,
                                flightAirframe: props.flightAirframe || 'N/A',
                                otherFlightId: props.otherFlightId,
                                otherFlightAirframe: props.otherFlightAirframe || 'N/A',
                                severity: props.severity?.toFixed(2)
                            };
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
            map.getLayers().forEach((layer, idx) => {
                if (layer instanceof TileLayer) {
                    layer.setVisible(mapLayerOptions[idx].value === mapStyle);
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
            setPreviousParams(null);
            setProximityEvents([]);
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
            console.log('[DEBUG] Received event definition IDs:', receivedEventDefinitionIds);
            console.log('[DEBUG] Requested event definition IDs:', filters.eventDefinitionIds);
            
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
                    // Deduplicate unordered flight pairs
                    const mainFlightId = event.flight_id;
                    const otherFlightId = event.other_flight_id;
                    const pairKey = [Math.min(mainFlightId, otherFlightId), Math.max(mainFlightId, otherFlightId)].join('-');
                    if (processedPairs.has(pairKey)) {
                        continue; // skip duplicate pair
                    }
                    processedPairs.add(pairKey);
                    const eventId = event.id;
                    const mainUrl = `/protected/proximity_points_for_event_and_flight?event_id=${eventId}&flight_id=${mainFlightId}`;
                    const otherUrl = `/protected/proximity_points_for_event_and_flight?event_id=${eventId}&flight_id=${otherFlightId}`;
                    
                    console.log(`Fetching proximity points for event ${eventId}: mainFlightId=${mainFlightId}, otherFlightId=${otherFlightId}`);
                    console.log(`Main URL: ${mainUrl}`);
                    console.log(`Other URL: ${otherUrl}`);
                    
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
                    const mainUrl = `/protected/proximity_points_for_event_and_flight?event_id=${eventId}&flight_id=${mainFlightId}`;
                    try {
                        const mainResp = await fetch(mainUrl, { credentials: 'include', headers: { 'Accept': 'application/json' } });
                        if (!mainResp.ok) {
                            console.warn(`Failed to fetch event points for event ${eventId}: ${mainResp.status} ${mainResp.statusText}`);
                            continue; // Skip this event if we can't get its points
                        }
                        const mainData = await mainResp.json();
                        allSingleEventPoints.push({
                            eventId: eventId,
                            mainFlightId: mainFlightId,
                            otherFlightId: 0, // Use 0 instead of null for regular events
                            mainFlightPoints: mainData.points || mainData || [],
                            otherFlightPoints: [],
                            severity: event.severity,
                            airframe: event.airframe,
                            otherAirframe: '' // Use empty string instead of null
                        });
                    } catch (error) {
                        console.warn(`Error fetching event points for event ${eventId}:`, error);
                        continue; // Skip this event if there's an error
                    }
                }
            }
            setProximityEventPoints([...allProximityEventPoints, ...allSingleEventPoints]);
            setLoading(false);
            
            // Clear all layers first
            clearMapLayers();
            
            // Process events based on type - only one type at a time
            if (allProximityEventPoints.length > 0) {
                // We have proximity events - process them
                processProximityEventCoordinates(allProximityEventPoints, showGrid);
            } else if (allSingleEventPoints.length > 0) {
                // We have regular events - process them
                processSingleEventCoordinates(allSingleEventPoints, showGrid);
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
        
        console.log('[DEBUG] Selected events:', selectedEvents);
        console.log('[DEBUG] Selected definition IDs:', selectedDefinitionIds);

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
            {/* Scoped CSS override for Bootstrap grid spacing */}
            <style>{`
                #heat-map-top-menu .row,
                #heat-map-top-menu .form-row,
                #heat-map-top-menu .col-auto,
                #heat-map-top-menu .col-auto:not(:last-child) {
                    margin-left: 0 !important;
                    margin-right: 0 !important;
                    padding-left: 0 !important;
                    padding-right: 0 !important;
                }
            `}</style>
            <div style={{ flex: '0 0 auto' }}>
                <SignedInNavbar activePage="heat_map" mapLayerDropdown={mapLayerDropdown} />
            </div>
            {/* Top menu row: Airframes dropdown, Severity slider, Date selector */}
            <div id="heat-map-top-menu" style={{
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '16px 32px 0 32px',
                background: 'white',
                zIndex: 10
            }}>
                {/* Left: Title */}
                <h4 style={{ margin: 0, padding: 0, lineHeight: 1.2 }}>Event Heat Map</h4>
                {/* Right: Controls group */}
                <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                    {/* Remove mapLayerDropdown from here */}
                    <div style={{ width: 'auto', margin: 0, padding: 0 }}>{severitySlider}</div>
                    <div style={{ width: 'auto', margin: 0, padding: 0 }}>
                        <TimeHeader
                            name=""
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
                            severitySliderComponent={null}
                        />
                    </div>
                </div>
            </div>
            {/* Error Banner */}
            {error && (
                <div className="alert alert-danger p-2 m-2 flex flex-row items-center justify-start gap-2" style={{zIndex: 2000}}>
                    <strong>Error:</strong>
                    <span>{error}</span>
                    <button className="ml-auto btn btn-link p-0" style={{fontSize: 18}} onClick={() => setError(null)}>
                        <i className="fa fa-times p-1 cursor-pointer"></i>
                    </button>
                </div>
            )}
            {/* Loading Indicator */}
            {loading && (
                <div className="alert alert-info p-2 m-2 flex flex-row items-center justify-start gap-2" style={{zIndex: 2000}}>
                    <div className="spinner-border spinner-border-sm" role="status">
                        <span className="sr-only">Loading...</span>
                    </div>
                    <span>Loading proximity events...</span>
                </div>
            )}
            <div style={{ flex: '1 1 auto', display: 'flex', flexDirection: 'column', height: '100%' }}>
                <div className="container-fluid" style={{ flex: '1 1 auto', height: '100%' }}>
                    <div className="row" style={{ height: '100%' }}>
                        <div className="col-lg-12" style={{ height: '100%' }}>
                            <div className="card m-2" style={{ height: '100%', display: 'flex', flexDirection: 'column', flex: '1 1 auto' }}>
                                <div className="card-body" style={{ padding: 0, background: 'transparent', flex: '1 1 auto', minHeight: 0, display: 'flex', position: 'relative' }}>
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
                                        <div className="col-lg-10" style={{ padding: 0, margin: 0, background: 'transparent', height: '100%', position: 'relative' }}>
                                            {gridToggleSwitch}
                                            <div ref={mapRef} style={{ width: '100%', height: '100%', background: 'transparent', margin: 0, padding: 0, position: 'relative' }} />

                                            {/* Render all open popups as overlays */}
                                            <div style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, pointerEvents: 'none', zIndex: 1000 }}>
                                                {openPopups.map((popup) => {
                                                    const isGrabbing = (draggedPopupId === popup.id);
                                                    const isRecent = (recentPopupId === popup.id);

                                                    return (
                                                        <div
                                                            key={popup.id}
                                                            className={`ol-popup ${isRecent ? 'z-110' : 'z-100'}`}
                                                            style={{
                                                                position: 'absolute',
                                                                boxShadow: '0 1px 4px rgba(0,0,0,0.2)',
                                                                padding: '15px',
                                                                borderRadius: '10px',
                                                                border: '1px solid #cccccc',
                                                                minWidth: '200px',
                                                                left: popup.position?.left ?? 100,
                                                                top: popup.position?.top ?? 100,
                                                                background: 'white',
                                                                transform: 'none',
                                                                cursor: isGrabbing ? 'grabbing' : '',
                                                                zIndex: isRecent ? 110 : 100,
                                                                pointerEvents: 'auto',
                                                            }}
                                                        >
                                                            <div
                                                                className="group"
                                                                style={{ fontWeight: 600, cursor: 'grab', marginBottom: 4, userSelect: isGrabbing ? 'none' : 'auto' }}
                                                                onMouseDown={e => { console.log('Popup mouse down', popup.id, e); handlePopupMouseDown(e, popup.id); }}
                                                            >
                                                                <span className={`fa fa-arrows ${isGrabbing ? 'opacity-100' : 'opacity-25 group-hover:opacity-100'} mr-2`}/>
                                                                {popup.data.otherFlightId ? 'Proximity Event Details' : 'Event Details'}
                                                            </div>
                                                            <a
                                                                href="#"
                                                                className="ol-popup-closer"
                                                                style={{ position: 'absolute', top: 2, right: 8, textDecoration: 'none' }}
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
                                                                ✖
                                                            </a>
                                                            <div>
                                                                <hr />
                                                                <div><strong>Time: </strong>{popup.data.time ?? '...'}</div>
                                                                <div><strong>Latitude: </strong> {popup.data.latitude !== null && popup.data.latitude !== undefined ? Number(popup.data.latitude).toFixed(5) : '...'}</div>
                                                                <div><strong>Longitude: </strong> {popup.data.longitude !== null && popup.data.longitude !== undefined ? Number(popup.data.longitude).toFixed(5) : '...'}</div>
                                                                <div><strong>Altitude (AGL): </strong> {popup.data.altitude !== null && popup.data.altitude !== undefined ? `${popup.data.altitude.toFixed(0)} ft` : '...'}</div>
                                                                <hr />
                                                                <div><strong>Flight ID: </strong>{popup.data.flightId ?? '...'}</div>
                                                                <div><strong>Airframe: </strong>{popup.data.flightAirframe ?? '...'}</div>
                                                                {popup.data.otherFlightId && (
                                                                    <>
                                                                        <hr />
                                                                        <div><strong>Other Flight ID: </strong>{popup.data.otherFlightId ?? '...'}</div>
                                                                        <div><strong>Other Airframe: </strong>{popup.data.otherFlightAirframe ?? '...'}</div>
                                                                    </>
                                                                )}
                                                                <hr />
                                                                <div><strong>Severity: </strong>{parseFloat(String(popup.data.severity)).toFixed(2)}</div>
                                                                {popup.data.otherFlightId && distances.lateral !== null && distances.euclidean !== null && selectedPoints.length === 2 && (
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