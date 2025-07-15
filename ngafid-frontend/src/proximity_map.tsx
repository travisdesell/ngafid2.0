import React from 'react';
import { createRoot } from 'react-dom/client';
import SignedInNavbar from './signed_in_navbar';
import 'ol/ol.css';


import Map from 'ol/Map';
import View from 'ol/View';
import { fromLonLat, getTransform, toLonLat } from 'ol/proj';
import Heatmap from 'ol/layer/Heatmap';
import VectorLayer from 'ol/layer/Vector';
import OSM from 'ol/source/OSM';
import VectorSource from 'ol/source/Vector';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import Style from 'ol/style/Style';
import Icon from 'ol/style/Icon';
import DragBox from 'ol/interaction/DragBox';
import { platformModifierKeyOnly } from 'ol/events/condition';
import Polygon from 'ol/geom/Polygon';
import WebGLVectorLayer from 'ol/layer/WebGLVector';
import MultiPolygon from 'ol/geom/MultiPolygon';
import WebGLTileLayer from 'ol/layer/WebGLTile';
import DataTileSource from 'ol/source/DataTile';


import type { Event } from './types';

type PopupContentData = {
    time: string | null;
    latitude: number | null;
    longitude: number | null;
    altitude: number | null;
    flightId: string | null;
    flightAirframe: string | null;
    otherFlightId: string | null;
    otherFlightAirframe: string | null;
    severity: string | null;
};

type ProximityMapPageState = {

    events: Event[];
    didFirstLoad: boolean;
    loading: boolean;
    error: string | null;
    showEventList: boolean;
    map: Map | null;
    heatmapLayer1: Heatmap | null;
    heatmapLayer2: Heatmap | null;
    markerSource: VectorSource | null;

    openPopups: Array<{ id: string, coord: number[], data: PopupContentData, position?: { left: number, top: number } }>;
    draggedPopupId: string | null,
    recentPopupId: string | null,
    popupContentData: PopupContentData;

    // Distance calculation state
    selectedPoints: Array<{
        id: string;
        latitude: number;
        longitude: number;
        altitude: number;
    }>;
    distances: {
        lateral: number | null;
        euclidean: number | null;
    };
};

const DEBUG_ENABLED = false;

const MARKER_VISIBILITY_ZOOM_THRESHOLD = 12;

const ICON_IMAGE_BLACK =  new Icon({
    src: '/images/black-point.png',
    scale: 0.05,
    anchor: [0.5, 0.5],
});

const ICON_IMAGE_RED =  new Icon({
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


function getOS():string {

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


// Interpolate color from green to red
function interpolateColor(value: number) {
    // value: 0 (green) to 1 (red)
    const r = Math.round(255 * value);
    const g = Math.round(255 * (1 - value));
    return `rgba(${r},${g},0,0.6)`;
}


export function toRadians(degrees: number): number {
    return degrees * Math.PI / 180;
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
export function calculateDistanceBetweenPoints(
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

class ProximityMapPage extends React.Component<object, ProximityMapPageState & {
    boxCoords: {
        minLat: number | null,
        maxLat: number | null,
        minLon: number | null,
        maxLon: number | null
    },
    boxInput: {
        minLat: string,
        maxLat: string,
        minLon: string,
        maxLon: string,
        startDate: string,
        endDate: string,
        minSeverity: number,
        maxSeverity: number
    },
    boxActive: boolean,
    showGrid: boolean,
    gridLayer: WebGLVectorLayer | null,
    heatmapLayer1: Heatmap | null,
    heatmapLayer2: Heatmap | null,
    markerLayer: VectorLayer | null,
}> {

    mapContainerRef: React.RefObject<HTMLDivElement | null>;
    dragStart: { x: number, y: number };
    popupStart: { left: number, top: number };

    gridSource: VectorSource | null;
    gridFeature: Feature | null;

    constructor(props: object) {
        super(props);
        this.mapContainerRef = React.createRef<HTMLDivElement>();
        this.dragStart = { x: 0, y: 0 };
        this.popupStart = { left: 0, top: 0 };
        this.gridSource = null;
        this.gridFeature = null;
        
        this.state = {
            events: [],
            didFirstLoad: false,
            loading: false,
            error: null,
            showEventList: true,
            map: null,
            heatmapLayer1: null,
            heatmapLayer2: null,
            markerSource: null,
            markerLayer: null,
            popupContentData: {
                time: null,
                latitude: null,
                longitude: null,
                altitude: null,
                flightId: null,
                flightAirframe: null,
                otherFlightId: null,
                otherFlightAirframe: null,
                severity: null,
            },
            openPopups: [],
            draggedPopupId: null,
            recentPopupId: null,
            boxCoords: {
                minLat: null,
                maxLat: null,
                minLon: null,
                maxLon: null
            },
            boxInput: {
                minLat: '',
                maxLat: '',
                minLon: '',
                maxLon: '',
                startDate: '2000-01-01',
                endDate: '',
                minSeverity: 0,
                maxSeverity: 1000
            },
            boxActive: false,
            showGrid: true,
            gridLayer: null,
            selectedPoints: [],
            distances: {
                lateral: null,
                euclidean: null
            },
        };
    }

    componentDidUpdate() {
        //Trigger map aspect ratio update
        this.state.map?.updateSize();
    }

    componentDidMount() {
        const initMap = (() => {
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
            heatmapLayer1.set('interactive', false); //<-- Flag as non-interactive layer

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
            heatmapLayer2.set('interactive', false); //<-- Flag as non-interactive layer

            const markerLayer = new VectorLayer({
                source: markerSource,
                visible: false
            });
            markerLayer.set('interactive', true); //<-- Flag as interactive layer

            const openStreetMapLayer = new WebGLTileLayer({
                source: new OSM() as unknown as DataTileSource,
                preload: Infinity
            });

            const map = new Map({
                target: 'map',
                layers: [
                    openStreetMapLayer,
                    heatmapLayer1,
                    heatmapLayer2,
                    markerLayer
                ],
                view: new View({
                    center: fromLonLat([-95, 40]),
                    zoom: 4
                })
            });

            // DRAG BOX INTERACTION
            const dragBox = new DragBox({
                condition: platformModifierKeyOnly
            });
            map.addInteraction(dragBox);

            dragBox.on('boxstart', () => {
                console.log("Box Drag Started...");
                this.setState({ boxActive: true });
            });

            dragBox.on('boxend', () => {
                const extent = dragBox.getGeometry().getExtent();
                const bottomLeft = toLonLat([extent[0], extent[1]]);
                const topRight = toLonLat([extent[2], extent[3]]);
                const minLat = Math.min(bottomLeft[1], topRight[1]);
                const maxLat = Math.max(bottomLeft[1], topRight[1]);
                const minLon = Math.min(bottomLeft[0], topRight[0]);
                const maxLon = Math.max(bottomLeft[0], topRight[0]);

                console.log("...Box Drag Finished!");
                console.log(`Updating Coord Ranges: Lat: [${minLat}, ${maxLat}], Lon: [${minLon}, ${maxLon}]`);

                this.setState(prevState => ({
                    ...prevState,
                    boxCoords: { minLat, maxLat, minLon, maxLon },
                    boxInput: {
                        ...prevState.boxInput,
                        minLat: minLat.toFixed(6),
                        maxLat: maxLat.toFixed(6),
                        minLon: minLon.toFixed(6),
                        maxLon: maxLon.toFixed(6)
                    },
                    boxActive: false
                }), () => {
                    // If grid mode is on, render empty grid overlay
                    if (this.state.showGrid) {
                        this.renderEmptyGridOverlay();
                    }
                });
            });

            map.on('singleclick', (event) => {

                map.forEachFeatureAtPixel(
                    event.pixel,
                    (feature) => {

                    const geometry = feature.getGeometry() as Point | undefined;
                    if (!geometry) return;
                    const coord = geometry.getCoordinates();
                    const props = feature.getProperties();

                    // Unique popup id based on coordinates and time
                    const popupId = `${props.flightId}-${props.otherFlightId}-${props.time}`;
                    // Prevent duplicate popups for the same marker
                    if (this.state.openPopups.some(p => p.id === popupId)) return;

                    // Limit to 2 popups maximum
                    if (this.state.openPopups.length >= 2) {
                        console.log('Maximum 2 popups allowed. Close one to open another.');
                        return;
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
                        severity: props.severity?.toFixed(2)
                    };

                    // Add new popup to openPopups
                    const pixel = map.getPixelFromCoordinate(coord);
                    const initialPosition = pixel ? { left: pixel[0], top: pixel[1] } : { left: 100, top: 100 };
                    
                    this.setState(prevState => {
                        const newOpenPopups = [
                            ...prevState.openPopups,
                            { id: popupId, coord, data: popupContentData, position: initialPosition }
                        ];

                        // Add point to selectedPoints for distance calculation
                        const newSelectedPoints = [
                            ...prevState.selectedPoints,
                            {
                                id: popupId,
                                latitude: props.latitude,
                                longitude: props.longitude,
                                altitude: props.altitudeAgl || 0 // Already in feet
                            }
                        ];

                        return {
                            openPopups: newOpenPopups,
                            selectedPoints: newSelectedPoints
                        };
                    }, () => {
                        // Calculate distances after state update
                        this.calculateDistancesBetweenPoints();
                    });
                    },
                    {
                        //Exclude non-interactive layers
                        layerFilter: (layer) => layer.get('interactive') === true
                    }
                );
            });

            map.on('moveend', () => {
                const mapView = map.getView();
                const mapZoom = mapView.getZoom();
                if (mapZoom === undefined) {
                    console.error('Map zoom level is undefined');
                    return;
                }
                markerLayer.setVisible(mapZoom >= MARKER_VISIBILITY_ZOOM_THRESHOLD);
            });

            this.setState({ map, heatmapLayer1, heatmapLayer2, markerSource, markerLayer }, async () => {
                map.updateSize();
            });
        });

        setTimeout(initMap, 0);
    }

    componentWillUnmount() {
        const { map } = this.state;
        if (map) map.setTarget(undefined);
        window.removeEventListener('mousemove', this.handleMouseMove);
        window.removeEventListener('mouseup', this.handleMouseUp);
    }

    async loadEvents() {
        try {
            const response = await fetch('/protected/proximity_events', {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error('Failed to load events');
            const events = await response.json();
            this.setState({ events, loading: false });
            return events;
        } catch (error) {
            console.error('Error loading events:', error);
            if (error instanceof Error)
                this.setState({ error: error.message, loading: false });
            else if (typeof error === 'string') 
                this.setState({ error, loading: false });
            return [];
        }
    }

    async loadProximityPoints() {
        try {
            const response = await fetch('/protected/proximity_points', {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error('Failed to load proximity points');
            return await response.json();
        } catch (error) {
            console.error('Error loading proximity points:', error);
            if (error instanceof Error)
                this.setState({ error: error.message, loading: false });
            else if (typeof error === 'string')
                this.setState({ error, loading: false });
            return [];
        }
    }

    async processEventCoordinates(events: Event[]) {
        const { map, heatmapLayer1, heatmapLayer2, markerSource, gridLayer, showGrid } = this.state;
        if (!map || !heatmapLayer1 || !heatmapLayer2 || !markerSource) {
            console.error('Map or layers not initialized');
            return;
        }

        //Grid layer exists, hide it
        if (gridLayer)
            gridLayer.setVisible(false);

        // clear heatmap sources
        heatmapLayer1.getSource()!.clear();
        heatmapLayer2.getSource()!.clear();
        if (markerSource) markerSource.clear();

        // Track processed unordered flight pairs
        const processedPairs = new Set<string>();
        const allPoints: { latitude: number, longitude: number }[] = [];
        for (const event of events) {
            const eventId = Number(event.id);
            const mainFlightId = Number(event.flightId);
            const otherFlightId = Number(event.otherFlightId);

            // Create an unordered key for the pair
            const pairKey = [Math.min(mainFlightId, otherFlightId), Math.max(mainFlightId, otherFlightId)].join('-');
            if (processedPairs.has(pairKey)) continue;
            processedPairs.add(pairKey);

            // Fetch points for main flight
            const mainFlightResp = await fetch(`/protected/proximity_points_for_flight?event_id=${eventId}&flight_id=${mainFlightId}`, {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            const mainFlightData = await mainFlightResp.json();

            // Fetch points for other flight
            const otherFlightResp = await fetch(`/protected/proximity_points_for_flight?event_id=${eventId}&flight_id=${otherFlightId}`, {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            const otherFlightData = await otherFlightResp.json();

            console.log("otherFlightData", otherFlightData);

            // Add points for main flight (RED)
            if (mainFlightData && mainFlightData.points && mainFlightData.points.length > 0) {
                for (const point of mainFlightData.points) {
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
                        flightId: mainFlightId,
                        otherFlightId: otherFlightId,
                        time: point.timestamp,
                        flightAirframe: mainFlightData.flight_airframe,
                        otherFlightAirframe: otherFlightData.flight_airframe || 'N/A',
                        severity: mainFlightData.lateral_distance,
                        altitudeAgl: point.altitude_agl,
                        lateralDistance: mainFlightData.lateral_distance,
                        verticalDistance: mainFlightData.vertical_distance,
                        latitude: point.latitude,
                        longitude: point.longitude
                    });
                    markerSource.addFeature(marker);
                    allPoints.push({ latitude: point.latitude, longitude: point.longitude });
                }
            }

            // Add points for other flight (BLACK)
            if (otherFlightData && otherFlightData.points && otherFlightData.points.length > 0) {
                for (const point of otherFlightData.points) {
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
                        flightId: otherFlightId,
                        otherFlightId: mainFlightId,
                        time: point.timestamp,
                        flightAirframe: otherFlightData.flight_airframe,
                        otherFlightAirframe: mainFlightData.flight_airframe || 'N/A',
                        severity: otherFlightData.lateral_distance,
                        altitudeAgl: point.altitude_agl,
                        lateralDistance: otherFlightData.lateral_distance,
                        verticalDistance: otherFlightData.vertical_distance,
                        latitude: point.latitude,
                        longitude: point.longitude
                    });
                    markerSource.addFeature(marker);
                    allPoints.push({ latitude: point.latitude, longitude: point.longitude });
                }
            }
        }

        // Fit map to extents as before
        const extent1 = heatmapLayer1.getSource()!.getExtent();
        const extent2 = heatmapLayer2.getSource()!.getExtent();
        const features1 = heatmapLayer1.getSource()!.getFeatures();
        const features2 = heatmapLayer2.getSource()!.getFeatures();
        console.log('[processEventCoordinates] extent1:', extent1, 'features1:', features1.length);
        console.log('[processEventCoordinates] extent2:', extent2, 'features2:', features2.length);
        const isValidExtent = (extent: number[]) => {
            return (
                Array.isArray(extent) &&
                extent.length === 4 &&
                extent.every((v) => typeof v === 'number' && isFinite(v)) &&
                (extent[0] !== extent[2]) && (extent[1] !== extent[3]) // not a single point
            );
        };
        if ((features1.length > 0 || features2.length > 0) && isValidExtent(extent1) && isValidExtent(extent2)) {
            const combinedExtent: [number, number, number, number] = [
                Math.min(extent1[0], extent2[0]),
                Math.min(extent1[1], extent2[1]),
                Math.max(extent1[2], extent2[2]),
                Math.max(extent1[3], extent2[3])
            ];
            console.log('[processEventCoordinates] fitting map to combinedExtent:', combinedExtent);
            map.getView().fit(combinedExtent, { padding: [50, 50, 50, 50], maxZoom: 15 });
        } else {
            console.log('[processEventCoordinates] Not fitting map: invalid or empty extents/features.');
        }

        // Add grid-based density map if enabled
        if (showGrid && map) {
            // Use bounding box from boxInput
            const { minLat, maxLat, minLon, maxLon } = this.state.boxInput;
            const gridSize = 0.05;
            const gridCounts: Record<string, number> = {};
            allPoints.forEach(pt => {
                const latKey = (Math.floor(pt.latitude / gridSize) * gridSize).toFixed(4);
                const lonKey = (Math.floor(pt.longitude / gridSize) * gridSize).toFixed(4);
                const key = `${latKey},${lonKey}`;
                gridCounts[key] = (gridCounts[key] || 0) + 1;
            });
            // Parse bounding box as numbers
            const minLatNum = parseFloat(minLat);
            const maxLatNum = parseFloat(maxLat);
            const minLonNum = parseFloat(minLon);
            const maxLonNum = parseFloat(maxLon);
            // Defensive: if bounding box is not valid, fallback to points extents
            let latStart = minLatNum, latEnd = maxLatNum, lonStart = minLonNum, lonEnd = maxLonNum;
            if (isNaN(latStart) || isNaN(latEnd) || isNaN(lonStart) || isNaN(lonEnd)) {
                // fallback to points extents
                if (allPoints.length > 0) {
                    latStart = Math.min(...allPoints.map(pt => pt.latitude));
                    latEnd = Math.max(...allPoints.map(pt => pt.latitude));
                    lonStart = Math.min(...allPoints.map(pt => pt.longitude));
                    lonEnd = Math.max(...allPoints.map(pt => pt.longitude));
                } else {
                    latStart = latEnd = lonStart = lonEnd = 0;
                }
            }
            // Ensure min < max
            if (latStart > latEnd) [latStart, latEnd] = [latEnd, latStart];
            if (lonStart > lonEnd) [lonStart, lonEnd] = [lonEnd, lonStart];
            // Compute grid cells for the entire bounding box
            const features = [];
            let maxCount = 1;
            if (Object.values(gridCounts).length > 0) {
                maxCount = Math.max(1, ...Object.values(gridCounts));
            }

            console.log('[Grid Debug] gridCounts:', gridCounts);
            console.log('[Grid Debug] maxCount:', maxCount);


            //Get transformation function from lon/lat to web mercator
            const proj = getTransform('EPSG:4326', 'EPSG:3857');

            //Populate grid coordinates
            const xs:number[] = [];
            const ys:number[] = [];
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

                const latVal = (Math.floor(latStart/gridSize)+j)*gridSize;

                for (let i = 0; i < cols; i++) {

                    const x0 = xs[i];
                    const x1 = xs[i + 1];

                    const lonVal = (Math.floor(lonStart/gridSize)+i)*gridSize;

                    const count = gridCounts[`${latVal.toFixed(4)},${lonVal.toFixed(4)}`]||0;
                    const intensity = (maxCount > 0 ? Math.sqrt(count / maxCount) : 0);
                    const color = interpolateColor(intensity);

                    if (count > 0)
                        console.log(`[Grid Debug] Cell (${latVal},${lonVal}) count:`, count, 'intensity:', intensity, 'color:', color);

                    const polygon = new Polygon([[
                        [x0,y0],
                        [x1,y0],
                        [x1,y1],
                        [x0,y1],
                        [x0,y0]
                    ]]);

                    const cellFeature = new Feature(polygon);
                    cellFeature.set('kind', 'density');     //<-- Flag this feature as a density cell so it can be removed later
                    cellFeature.set('color', color);

                    features.push(cellFeature);

                }

            }

            //Clear all of the old density cells from the grid source
            this.gridSource!.getFeatures().forEach(f => {

                if (f.get('kind') === 'density')
                    this.gridSource!.removeFeature(f);

            });

            //Add the new features to the grid source
            this.gridSource!.addFeatures(features);

            // Hide heatmap layers
            heatmapLayer1.setVisible(false);
            heatmapLayer2.setVisible(false);

            // Show grid layer
            if (gridLayer)
                gridLayer.setVisible(true);
;

            this.setState({ gridLayer: gridLayer });

        } else {
            // Show heatmap layers
            if (heatmapLayer1) heatmapLayer1.setVisible(true);
            if (heatmapLayer2) heatmapLayer2.setVisible(true);
            // Hide grid layer if present
            if (gridLayer) {
                gridLayer.setVisible(false);
                // this.setState({ gridLayer: null });
            }
        }
    }

    toggleEventList = () => {
        this.setState(prevState => ({ showEventList: !prevState.showEventList }));
    };

    handleBoxInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value, type } = e.target;
        this.setState(prevState => ({
            boxInput: {
                ...prevState.boxInput,
                [name]: type === 'range' ? Number(value) : value
            }
        }));
    };

    handleBoxSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const { minLat, maxLat, minLon, maxLon, startDate, endDate, minSeverity, maxSeverity } = this.state.boxInput;
        if (!minLat || !maxLat || !minLon || !maxLon) {
            this.setState({ error: 'Please select a box on the map first.' });
            return;
        }
        this.setState({ loading: true, didFirstLoad: true, error: null });
        try {
            let url = `/protected/proximity_events_in_box?min_latitude=${minLat}&max_latitude=${maxLat}&min_longitude=${minLon}&max_longitude=${maxLon}`;
            if (startDate) url += `&start_time=${encodeURIComponent(startDate)}`;
            if (endDate) url += `&end_time=${encodeURIComponent(endDate)}`;
            if (minSeverity !== 0) url += `&min_severity=${minSeverity}`;
            if (maxSeverity !== 1000) url += `&max_severity=${maxSeverity}`;
            const response = await fetch(url, {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error('Failed to load events in box');
            const events = await response.json();
            
            let error = null;

            //No events loaded, show error
            if (events.length === 0) {
                error = "No events found with the given constraints! Please try again.";
                console.log("No events found with the given constraints! Please try again.");
            }

            this.setState({ error, events, loading: false });
            await this.processEventCoordinates(events);
            console.log('[handleBoxSubmit] map after processEventCoordinates:', this.state.map);
        } catch (error) {

            if (error instanceof Error)
                this.setState({ error: error.message, loading: false });
            else if (typeof error === 'string')
                this.setState({ error, loading: false });

        }
    };

    handlePopupMouseDown = (e: React.MouseEvent, popupId: string) => {

        e.preventDefault();

        this.setState({
            draggedPopupId: popupId,
            recentPopupId: popupId,
        });

        const popupElem = (e.currentTarget.parentElement as HTMLElement);


        this.dragStart = { x: e.clientX, y: e.clientY };
        this.popupStart = {
                left: parseFloat(popupElem.style.left) || 0,
                top:  parseFloat(popupElem.style.top)  || 0
        };

        window.addEventListener('mousemove', this.handleMouseMove);
        window.addEventListener('mouseup', this.handleMouseUp);

    };

    handleMouseMove = (e: MouseEvent) => {

        if (!this.state.draggedPopupId || !this.mapContainerRef.current)
            return;

        const dx = e.clientX - this.dragStart.x;
        const dy = e.clientY - this.dragStart.y;

        let left = this.popupStart.left + dx;
        let top = this.popupStart.top + dy;

        // Constrain to map
        const mapRect = this.mapContainerRef.current.getBoundingClientRect();
        left = Math.max(0, Math.min(left, mapRect.width - 200)); // 200 = minWidth
        top = Math.max(0, Math.min(top, mapRect.height - 100)); // 100 = approx height
        
        this.setState(prevState => ({
            openPopups: prevState.openPopups.map(p =>
                p.id === this.state.draggedPopupId ? { ...p, position: { left, top } } : p
            )
        }));
    };

    handleMouseUp = () => {
        this.setState({ draggedPopupId: null });
        window.removeEventListener('mousemove', this.handleMouseMove);
        window.removeEventListener('mouseup', this.handleMouseUp);
    };

    // Calculate distances between two selected points
    calculateDistancesBetweenPoints = () => {
        const { selectedPoints } = this.state;
        if (selectedPoints.length !== 2) {
            this.setState({ distances: { lateral: null, euclidean: null } });
            return;
        }

        const [point1, point2] = selectedPoints;
        const result = calculateDistanceBetweenPoints(
            point1.latitude, point1.longitude, point1.altitude,
            point2.latitude, point2.longitude, point2.altitude
        );

        this.setState({ distances: { lateral: result.lateral, euclidean: result.euclidean } });
    };

    toggleGrid = () => {
        this.setState(
            prevState => ({ showGrid: !prevState.showGrid }),
            () => {
                // Remove grid layer if switching to heatmap
                const { map, gridLayer, heatmapLayer1, heatmapLayer2, showGrid } = this.state;
                if (map) {
                    if (showGrid) {
                        // Hide heatmap layers
                        if (heatmapLayer1) heatmapLayer1.setVisible(false);
                        if (heatmapLayer2) heatmapLayer2.setVisible(false);
                    } else {
                        // Hide grid layer
                        if (gridLayer) {
                            gridLayer.setVisible(false);
                        }
                        // Show heatmap layers
                        if (heatmapLayer1) heatmapLayer1.setVisible(true);
                        if (heatmapLayer2) heatmapLayer2.setVisible(true);
                    }
                }
                // Re-process events to update map layers
                this.processEventCoordinates(this.state.events);
            }
        );
    };

    // Helper to render an empty green grid overlay for the selected bounding box
    async renderEmptyGridOverlay() {
        const { map, gridLayer, markerLayer, heatmapLayer1, heatmapLayer2 } = this.state;
        
        if (!map) {
            console.warn('[renderEmptyGridOverlay] Map not initialized, exiting renderEmptyGridOverlay');
            return;
        }

        console.log('[renderEmptyGridOverlay] Rendering empty grid overlay...');

        // Grid layer exists, make it visible
        if (gridLayer)
            gridLayer.setVisible(true);

        // Use bounding box from boxInput
        const { minLat, maxLat, minLon, maxLon } = this.state.boxInput;
        const gridSize = 0.05; // degrees (should match main gridSize)
        // Parse bounding box as numbers
        const minLatNum = parseFloat(minLat);
        const maxLatNum = parseFloat(maxLat);
        const minLonNum = parseFloat(minLon);
        const maxLonNum = parseFloat(maxLon);
        let latStart = minLatNum, latEnd = maxLatNum, lonStart = minLonNum, lonEnd = maxLonNum;
        if (isNaN(latStart) || isNaN(latEnd) || isNaN(lonStart) || isNaN(lonEnd)) {
            console.warn('Invalid bounding box coordinates, exiting renderEmptyGridOverlay');
            return;
        }
        if (latStart > latEnd) [latStart, latEnd] = [latEnd, latStart];
        if (lonStart > lonEnd) [lonStart, lonEnd] = [lonEnd, lonStart];


        //Get transformation function from lon/lat to web mercator
        const proj = getTransform('EPSG:4326', 'EPSG:3857');

        //Populate grid coordinates
        const xs:number[] = [];
        const ys:number[] = [];
        for (let lon = Math.floor(lonStart / gridSize) * gridSize; lon <= lonEnd + 1e-9; lon += gridSize) {
            xs.push(proj([lon, 0])[0]);
        }
        for (let lat = Math.floor(latStart / gridSize) * gridSize; lat <= latEnd + 1e-9; lat += gridSize) {
            ys.push(proj([0, lat])[1]);
        }

        let itr:number = 0;
        const rows = ys.length - 1;
        const cols = xs.length - 1;
        const rings3857: number[][][][] = new Array(rows * cols);

        for (let j = 0; j < rows; j++) {

            const y0 = ys[j];
            const y1 = ys[j + 1];

            for (let i = 0; i < cols; i++) {

                const x0 = xs[i];
                const x1 = xs[i + 1];

                /*
                    Create a polygon for each grid cell in EPSG:3857 coordinates

                    Defined as a closed ring (i.e. explicit matching start & end points)
                    in clockwise order starting from the top left.
                */
                rings3857[itr++] = [[
                    [x0,y0],
                    [x1,y0],
                    [x1,y1],
                    [x0,y1],
                    [x0,y0]
                ]];

            }

        }

        let gridLayerCurrent = this.state.gridLayer;

        //Grid source doesn't exist yet, create it
        if (!this.gridSource) {

            this.gridSource = new VectorSource({
                useSpatialIndex: false,
            });

            const newGridLayer = new WebGLVectorLayer({
                source: this.gridSource,
                disableHitDetection: true,
                style: {
                    'fill-color': ['get', 'color'],     //<-- Get color attribute from feature (defaults to gray)
                    'stroke-color': 'rgba(0,0,0,0.1)',
                    'stroke-width': 1.00,
                },
                opacity: 0.7
            });
            newGridLayer.set('interactive', false); //<-- Flag as non-interactive layer

            //Update current grid layer
            gridLayerCurrent = newGridLayer;

            if (markerLayer) {

                const markerLayerIndex = map.getLayers().getArray().indexOf(markerLayer);
                if (gridLayerCurrent)
                    map.getLayers().insertAt(markerLayerIndex, gridLayerCurrent);
                
            } else if (gridLayerCurrent) {
                map.addLayer(gridLayerCurrent);
            }

        }

        //Feature doesn't exist yet, create it
        if (!this.gridFeature) {
            this.gridFeature = new Feature(new MultiPolygon(rings3857));
            this.gridSource.addFeature(this.gridFeature);

        //Otherwise, update existing feature coordinates
        } else {
            (this.gridFeature.getGeometry() as MultiPolygon).setCoordinates(rings3857);
        }

        // Hide heatmap layers
        if (heatmapLayer1) heatmapLayer1.setVisible(false);
        if (heatmapLayer2) heatmapLayer2.setVisible(false);
        this.setState({ gridLayer: gridLayerCurrent });

    }

    render() {
        const { loading, error, showEventList, events, openPopups, boxInput, boxActive, showGrid, gridLayer, didFirstLoad } = this.state;

        /*
            console.log('[Render] boxInput:', boxInput);
        */

        const debugContent = (DEBUG_ENABLED) && (
            <div className="font-mono flex flex-col items-start justify-start bg-fuchsia-500/50 p-2 mt-2 overflow-y-auto max-h-[200px]">
                <hr className='w-full mt-[8px] border-t border-orange-500'/>

                <span>boxActive: {boxActive ? 'true' : 'false'}</span>
                <span>showGrid: {showGrid ? 'true' : 'false'}</span>
                <span>gridLayer: {gridLayer ? 'active' : 'inactive'}</span>
                <span>showEventList: {showEventList ? 'true' : 'false'}</span>
                <span>|openPopups|: {openPopups.length}</span>
            </div>
        );

        const eventList = (events.length > 0) && (

            <div className="card flex flex-col w-[75%] h-full max-h-full overflow-clip">

                {/* Header */}
                <div className="text-2xl card-header flex flex-row items-center justify-between">

                    <span>Proximity Events</span>

                    <button
                        className="btn"
                        onClick={this.toggleEventList}
                    >
                        <i className="fa fa-times"/>
                    </button>

                </div>

                <div className="card-body grow overflow-y-auto text-center text-sm w-[100%]!">
                    <table className="table-hover table-fixed rounded-lg w-full">

                        <colgroup>
                            <col style={{ width: "24%" }} />
                            <col style={{ width: "24%" }} />
                            <col style={{ width: "19%" }} />
                            <col style={{ width: "19%" }} />
                            <col style={{ width: "14%" }} />
                        </colgroup>

                        <thead className="leading-16 text-[var(--c_text)] border-b-1 sticky">
                        <tr>
                            <th className="text-left">Start</th>
                            <th className="text-left">End</th>
                            <th className="text-left">Flight ID</th>
                            <th className="text-left">Other Flight ID</th>
                            <th className="text-left">Severity</th>
                        </tr>
                        </thead>

                        <tbody className="leading-8 before:content-['\A'] min-w-full max-h-[calc(100vh-800px)]!">

                        {/* Empty spacer row */}
                        <tr className="pointer-none bg-transparent">
                            <td colSpan={3} className="h-6"/>
                        </tr>

                        {events.map((
                            event: Event,
                            index: number
                        ) => (
                            <tr key={event.id} className={`${index%2 ? "bg-[var(--c_row_bg)]" : "bg-[var(--c_row_bg_alt)]"} text-[var(--c_text_alt)]`}>
                                <td className="text-left text-[var(--c_text)]/100">{new Date(event.startTime).toLocaleString()}</td>
                                <td className="text-left text-[var(--c_text)]/100">{new Date(event.endTime).toLocaleString()}</td>
                                <td className="text-left text-[var(--c_text)]/100">{event.flightId}</td>
                                <td className="text-left text-[var(--c_text)]/100">{event.otherFlightId}</td>
                                <td className="text-left text-[var(--c_text)]/100">{event.severity.toFixed(2)}</td>
                            </tr>
                        ))}

                        </tbody>

                    </table>
                </div>

            </div>
        );

        const errorDisplay = error ? (
            <div className="alert alert-danger p-2! m-2! flex flex-row items-center justify-start gap-2">

                {/* Display Error */}
                <strong>Error:</strong>
                <span>{error}</span>

                {/* Close Error */}
                <button className="ml-auto" onClick={() => this.setState({ error: null })}>
                    <i className="fa fa-times p-1 cursor-pointer"></i>
                </button>

            </div>
        ) : null;

        const loadingDisplay = loading ? (
            <div className="absolute top-10 left-1/2 transform -translate-x-1/2 bg-white p-2 rounded shadow z-[9999]">
                <div className="spinner-border" role="status">
                    <span className="sr-only">Loading...</span>
                </div>
            </div>
        ) : null;

        const mapContent = (
            <div
                id="map"
                ref={this.mapContainerRef}
                style={{ minHeight: "500px" }}
                className="map card overflow-clip m-0 flex border-[var(--c_border_alt)] w-full h-full! relative"
            ></div>
        );


        const eventsTableDisplayToggle = (
            <div>
                <hr/>
                <div className="flex items-center justify-between text-gray-500 ">
                    <div className="flex flex-row items-center justify-between w-full">

                        {/* Event Count Display */}
                        <span className="text-sm">
                            {events.length} Found{events.length ? '!' : '...'}
                        </span>

                        {/* Toggle Event List Button (Only visible when list is closed and we have events)*/}
                        <button
                            className={`btn btn-secondary ${showEventList||(events.length<=0) ? 'opacity-0 pointer-events-none' : ''}`}
                            onClick={this.toggleEventList}
                        >
                            <i className="fa fa-table mr-2"></i>
                            Reopen Event List
                        </button>
                    </div>
                </div>
            </div>
        );


        // Render all open popups as overlays
        const popups = openPopups.map((popup) => {

            const isGrabbing = (this.state.draggedPopupId === popup.id);
            const isRecent = (this.state.recentPopupId === popup.id);

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
                    }}
                >
                    <div
                        className="group"
                        style={{ fontWeight: 600, cursor: 'grab', marginBottom: 4 }}
                        onMouseDown={e => this.handlePopupMouseDown(e, popup.id)}
                    >
                        <span className={`fa fa-arrows ${isGrabbing ? 'opacity-100' : 'opacity-25 group-hover:opacity-100'} mr-2`}/>
                        Proximity Event Details
                    </div>
                    <a
                        href="#"
                        className="ol-popup-closer"
                        style={{ position: 'absolute', top: 2, right: 8, textDecoration: 'none' }}
                        onClick={e => {
                            e.preventDefault();
                            this.setState(prevState => ({
                                openPopups: prevState.openPopups.filter(p => p.id !== popup.id),
                                selectedPoints: prevState.selectedPoints.filter(p => p.id !== popup.id)
                            }), () => {
                                // Recalculate distances after removing point
                                this.calculateDistancesBetweenPoints();
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
                        <hr />
                        <div><strong>Other Flight ID: </strong>{popup.data.otherFlightId ?? '...'}</div>
                        <div><strong>Other Airframe: </strong>{popup.data.otherFlightAirframe ?? '...'}</div>
                        <hr />
                        <div><strong>Severity: </strong>{popup.data.severity ?? '...'}</div>
                        {this.state.distances.lateral !== null && this.state.distances.euclidean !== null && this.state.selectedPoints.length === 2 && (
                            <>
                                <hr />
                                <div><strong>Lateral Distance: </strong>{this.state.distances.lateral.toFixed(0)} ft</div>
                                <div><strong>Euclidean (Slant) Distance: </strong>{this.state.distances.euclidean.toFixed(0)} ft</div>
                                <div><strong>Vertical Separation: </strong>{Math.abs(this.state.selectedPoints[0].altitude - this.state.selectedPoints[1].altitude).toFixed(0)} ft</div>
                            </>
                        )}
                    </div>
                </div>
            );
        });

        // Toggle switch for grid/heatmap
        const gridToggleSwitch = (
            <div className="flex items-center justify-between gap-2">
                <span className={showGrid ? 'opacity-50' : ''}  style={{ fontWeight: 600, fontSize: 15 }}>
                    <i className="fa fa-fire mr-1"/>
                    Heatmap
                </span>
                <label className="switch mb-0 justify-self-center mx-auto">
                    <input type="checkbox" checked={showGrid} onChange={this.toggleGrid} />
                    <span className="slider round"></span>
                </label>
                <span className={`ml-auto ${showGrid ? '' : 'opacity-50'}`} style={{ fontWeight: 600, fontSize: 15 }}>
                    <i className="fa fa-th mr-1"/>
                    Grid
                </span>
                <style>{`
                    .switch {
                        position: relative;
                        display: inline-block;
                        width: 48px;
                        height: 24px;
                    }
                    .switch input { display: none; }
                    .slider {
                        position: absolute;
                        cursor: pointer;
                        top: 0; left: 0; right: 0; bottom: 0;
                        background-color: #ccc;
                        transition: .4s;
                        border-radius: 24px;
                    }
                    .slider:before {
                        position: absolute;
                        content: "";
                        height: 18px;
                        width: 18px;
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
                        transform: translateX(24px);
                    }
                `}</style>
            </div>
        );

        const selectAreaInstructions = (
            <span className='italic'>
                {
                (userOS==="Linux" || userOS==="Windows")
                ?
                (<span>Ctrl + Drag</span>)
                :
                (<span>⌘ + Drag</span>)
                }
            </span>
        );

        // Top left box for coordinates and submit
        const boxSelector = (
            <div
                className='w-[320px]'
                style={{
                position: 'absolute',
                top: 20,
                left: 20,
                zIndex: 200,
                background: 'white',
                padding: '10px',
                borderRadius: '8px',
                boxShadow: '0 1px 4px rgba(0,0,0,0.2)'
            }}>
                <form onSubmit={this.handleBoxSubmit}>
                    <div style={{ marginBottom: 4, fontWeight: 600 }}>Select Area ({selectAreaInstructions}):</div>
                    <hr className="mt-[8px]"/>

                    {gridToggleSwitch}

                    <hr className="mt-[8px]"/>

                    <div style={{display: 'flex', flexDirection: 'column', gap: 2}}>

                        {/* Min. Latitutde */}
                        <label className="flex! flex-row items-center justify-start gap-1" style={{fontSize: 12}}>
                            <span className="text-nowrap">Min Latitude:</span>
                            <input
                                className='bg-neutral-100 w-full text-right px-1'
                                type="text"
                                placeholder="Enter minimum latitude..."
                                name="minLat"
                                value={boxInput.minLat}
                                onChange={this.handleBoxInputChange}
                            />
                            <span className="w-2">°</span>
                        </label>

                        {/* Max. Latitude */}
                        <label className="flex! flex-row items-center justify-start gap-1" style={{fontSize: 12}}>
                            <span className="text-nowrap">Max Latitude:</span>
                            <input
                                className='bg-neutral-100 w-full text-right px-1'
                                type="text"
                                placeholder="Enter maximum latitude..."
                                name="maxLat"
                                value={boxInput.maxLat}
                                onChange={this.handleBoxInputChange}
                            />
                            <span className="w-2">°</span>
                        </label>

                        {/* Min. Longitude */}
                        <label className="flex! flex-row items-center justify-start gap-1" style={{fontSize: 12}}>
                            <span className="text-nowrap">Min Longitude:</span>
                            <input
                                className='bg-neutral-100 w-full text-right px-1'
                                type="text"
                                placeholder="Enter minimum longitude..."
                                name="minLon"
                                value={boxInput.minLon}
                                onChange={this.handleBoxInputChange}
                            />
                            <span className="w-2">°</span>
                        </label>

                        {/* Max. Longitude */}
                        <label className="flex! flex-row items-center justify-start gap-1" style={{fontSize: 12}}>
                            <span className="text-nowrap">Max Longitude:</span>
                            <input
                                className='bg-neutral-100 w-full text-right px-1'
                                type="text"
                                placeholder="Enter maximum longitude..."
                                name="maxLon"
                                value={boxInput.maxLon}
                                onChange={this.handleBoxInputChange}
                            />
                            <span className="w-2">°</span>
                        </label>

                        {/* Start Date */}
                        <label className="flex! flex-row items-center justify-start gap-1" style={{fontSize: 12}}>
                            <span className="text-nowrap">Start Date:</span>
                            <input
                                className='bg-neutral-100 w-full text-right px-1 cursor-text'
                                type="date" 
                                name="startDate"
                                value={boxInput.startDate}
                                onChange={this.handleBoxInputChange}
                            />
                            <span className="w-2">&nbsp;</span>
                        </label>

                        {/* End Date */}
                        <label className="flex! flex-row items-center justify-start gap-1" style={{fontSize: 12}}>
                            <span className="text-nowrap">End Date:</span>
                            <input
                                className='bg-neutral-100 w-full text-right px-1 cursor-text'
                                type="date" 
                                name="endDate"
                                value={boxInput.endDate}
                                onChange={this.handleBoxInputChange}
                            />
                            <span className="w-2">&nbsp;</span>
                        </label>

                    </div>

                    <hr/>

                    {/* Severity Range Selector */}
                    <label style={{fontSize: 12}}>
                        Severity Range: <span style={{color: 'red', fontWeight: 600}}>{boxInput.minSeverity} <span className="text-black">-</span> {boxInput.maxSeverity}</span>
                        <div style={{display: 'flex', alignItems: 'center', gap: 8}}>
                            <input
                                type="range"
                                name="minSeverity"
                                min={0}
                                max={boxInput.maxSeverity}
                                value={boxInput.minSeverity}
                                onChange={this.handleBoxInputChange}
                                style={{flex: 1, accentColor: 'red'}}
                            />
                            <input
                                type="range"
                                name="maxSeverity"
                                min={boxInput.minSeverity}
                                max={1_000}
                                value={boxInput.maxSeverity}
                                onChange={this.handleBoxInputChange}
                                style={{flex: 1, accentColor: 'red'}}
                            />
                        </div>
                    </label>

                    <button type="submit" 
                        style={{
                            marginTop: 8, 
                            width: '100%', 
                            backgroundColor: 'var(--c_confirm_bg)', 
                            color: 'white', 
                            border: 'none', 
                            borderRadius: 4, 
                            padding: '8px 0', 
                            fontWeight: 600, 
                            fontSize: 16, 
                            cursor: 'pointer',
                            boxShadow: '0 1px 4px rgba(0,0,0,0.1)'
                        }}
                    >
                        Show Events
                    </button>

                    {/* Event Table Toggle */}
                    {
                        (didFirstLoad)
                        &&
                        eventsTableDisplayToggle
                    }

                    {/* Debug Panel */}
                    {debugContent}

                </form>
            </div>
        );

        return (
            <div
                className="w-full h-full grow flex flex-col"
                id="proximity-map-page-content"
                style={{position: 'relative'}}
            >

                {/* Navbar */}
                <div style={{flex: "0 0 auto"}}>
                    <SignedInNavbar
                        activePage="proximity_map"
                        showProximityEventListButton={true}
                        toggleProximityEventList={this.toggleEventList}
                        plotMapHidden={false}
                    />
                </div>

                {/* Error Display */}
                {errorDisplay}

                <div className="flex flex-row flex-grow min-h-0 w-full overflow-hidden p-2 gap-2" style={{position: 'relative'}}>

                    {/* Always render boxSelector, mapContent, loadingDisplay, and popups */}
                    {boxSelector}
                    {mapContent}
                    {loadingDisplay}
                    {popups}

                    {/* Event List */}
                    {showEventList && eventList}

                </div>

            </div>
        );
    }
}

const pageContainer = document.querySelector('#proximity-map-page') as HTMLElement;
const pageRoot = createRoot(pageContainer);
pageRoot.render(<ProximityMapPage/>);