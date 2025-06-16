import React from 'react';
import { createRoot } from 'react-dom/client';
import SignedInNavbar from './signed_in_navbar';
import 'ol/ol.css';

import Map from 'ol/Map';
import View from 'ol/View';
import { fromLonLat } from 'ol/proj';
import TileLayer from 'ol/layer/Tile';
import Heatmap from 'ol/layer/Heatmap';
import VectorLayer from 'ol/layer/Vector';
import OSM from 'ol/source/OSM';
import VectorSource from 'ol/source/Vector';
import Feature from 'ol/Feature';
import Point from 'ol/geom/Point';
import Style from 'ol/style/Style';
import Icon from 'ol/style/Icon';
import Overlay from 'ol/Overlay';
import { Coordinate } from 'ol/coordinate';

type ProximityMapPageState = {

    events: any[];
    loading: boolean;
    error: string | null;
    showEventList: boolean;
    map: Map | null;
    heatmapLayer1: Heatmap | null;
    heatmapLayer2: Heatmap | null;
    markerSource: VectorSource | null;

    popupContentData: {
        time: string | null;
        coordinates: string | null;
        flightId: string | null;
        flightAirframe: string | null;
        otherFlightId: string | null;
        otherFlightAirframe: string | null;
        severity: string | null;
    };
};

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

class ProximityMapPage extends React.Component<{}, ProximityMapPageState> {

    constructor(props: {}) {
        super(props);
        this.state = {
            events: [],
            loading: false,
            error: null,
            showEventList: true,
            map: null,
            heatmapLayer1: null,
            heatmapLayer2: null,
            markerSource: null,

            popupContentData: {
                time: null,
                coordinates: null,
                flightId: null,
                flightAirframe: null,
                otherFlightId: null,
                otherFlightAirframe: null,
                severity: null,
            }
        };
    }

    componentDidUpdate() {

        console.log("ProximityMapPage component updated, triggering map size update...");

        //Trigger map aspect ratio update
        this.state.map?.updateSize();

    }

    componentDidMount() {
        requestAnimationFrame(() => {
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

            const markerLayer = new VectorLayer({
                source: markerSource,
                visible: false
            });

            const map = new Map({
                target: 'map',
                layers: [
                    new TileLayer({ source: new OSM() }),
                    heatmapLayer1,
                    heatmapLayer2,
                    markerLayer
                ],
                view: new View({
                    center: fromLonLat([-95, 40]),
                    zoom: 4
                })
            });

            const container = document.getElementById('popup');
            const closer = document.getElementById('popup-closer');

            if (!container)
                throw new Error("Popup container element not found");
            
            const overlay = new Overlay({
                element: container,
                autoPan: true,
                autoPanAnimation: { duration: 250 }
            });
            map.addOverlay(overlay);

            if (!closer)
                throw new Error("Popup closer element not found");

            closer.onclick = () => {
                overlay.setPosition(undefined);
                container.style.display = 'none';
                closer.blur();
                return false;
            };

            const proximityMapPageRef = this;
            map.on('singleclick', function (event) {

                console.log("Got Click, checking for markers at cursor...");

                map.forEachFeatureAtPixel(event.pixel, function (feature) {

                    const geometry = feature.getGeometry() as Point | undefined;
                    if (!geometry)
                        return;
                    const coord = geometry!.getCoordinates();
                    const props = feature.getProperties();

                    console.log("Marker found at cursor, displaying popup...");

                    let popupContentData = {
                        time: new Date(props.time).toLocaleString(),
                        coordinates: coord.map((n: number) => n.toFixed(5)).join(', '),
                        flightId: props.flightId,
                        flightAirframe: props.flightAirframe || 'N/A',
                        otherFlightId: props.otherFlightId,
                        otherFlightAirframe: props.otherFlightAirframe || 'N/A',
                        severity: props.severity.toFixed(2)
                    };

                    proximityMapPageRef.setState({ popupContentData });

                    container.style.display = 'block';
                    overlay.setPosition(coord);
                });
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

            this.setState({ map, heatmapLayer1, heatmapLayer2, markerSource }, async () => {
                map.updateSize();
                const events = await this.loadEvents();
                await this.processEventCoordinates(events);
            });
        });
    }

    componentWillUnmount() {
        const { map } = this.state;
        if (map) map.setTarget(undefined);
    }

    async loadEvents() {
        try {
            const response = await fetch('/protected/all_proximity_events', {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error('Failed to load events');
            const events = await response.json();
            this.setState({ events, loading: false });
            return events;
        } catch (error: any) {
            console.error('Error loading events:', error);
            this.setState({ error: error.message, loading: false });
            return [];
        }
    }

    async processEventCoordinates(events: any) {
        const { map, heatmapLayer1, heatmapLayer2, markerSource } = this.state;

        if (!map || !heatmapLayer1 || !heatmapLayer2 || !markerSource) {
            console.error('Map or layers not initialized');
            return;
        }

        const heatmapSource1 = heatmapLayer1.getSource();
        const heatmapSource2 = heatmapLayer2.getSource();
        heatmapSource1.clear();
        heatmapSource2.clear();
        markerSource.clear();

        for (const event of events) {
            const coordinates1 = await this.loadCoordinates({ ...event, flightId: event.flightId });
            const coordinates2 = await this.loadCoordinates({ ...event, flightId: event.otherFlightId });

            if (coordinates1) {
                coordinates1.forEach((coord: Coordinate) => {
                    const olCoord = fromLonLat(coord);

                    const feature = new Feature({ geometry: new Point(olCoord) });
                    feature.set('weight', 0.8);
                    heatmapSource1.addFeature(feature);

                    const marker = new Feature({ geometry: new Point(olCoord) });
                    marker.setStyle(RED_POINT_STYLE);
                    marker.setProperties({
                        isMarker: true,
                        flightId: event.flightId,
                        otherFlightId: event.otherFlightId,
                        time: event.startTime,
                        flightAirframe: event.flightAirframe,
                        otherFlightAirframe: event.otherFlightAirframe,
                        severity: event.severity
                    });
                    markerSource.addFeature(marker);
                });
            }

            if (coordinates2) {
                coordinates2.forEach((coord: Coordinate) => {
                    const olCoord = fromLonLat(coord);

                    const feature = new Feature({ geometry: new Point(olCoord) });
                    feature.set('weight', 0.8);
                    heatmapSource2.addFeature(feature);

                    const marker = new Feature({ geometry: new Point(olCoord) });
                    marker.setStyle(BLACK_POINT_STYLE);
                    marker.setProperties({
                        isMarker: true,
                        flightId: event.otherFlightId,
                        otherFlightId: event.flightId,
                        time: event.endTime,
                        flightAirframe: event.otherFlightAirframe,
                        otherFlightAirframe: event.flightAirframe,
                        severity: event.severity
                    });
                    markerSource.addFeature(marker);
                });
            }
        }

        const extent1 = heatmapSource1.getExtent();
        const extent2 = heatmapSource2.getExtent();

        //Check if both extents are valid (length 4 and not infinite)
        const isValidExtent = (extent: number[]) => {
            return (
                Array.isArray(extent) &&
                extent.length === 4 &&
                extent.every((v) => typeof v === 'number' && isFinite(v))
            );
        }

        if (isValidExtent(extent1) && isValidExtent(extent2)) {

            //Calculate the union of both extents
            const combinedExtent: [number, number, number, number] = [
                Math.min(extent1[0], extent2[0]),
                Math.min(extent1[1], extent2[1]),
                Math.max(extent1[2], extent2[2]),
                Math.max(extent1[3], extent2[3])
            ];

            if (heatmapSource1.getFeatures().length > 0 || heatmapSource2.getFeatures().length > 0) {
                map.getView().fit(combinedExtent, { padding: [50, 50, 50, 50], maxZoom: 15 });
            }

        }

    }

    async loadCoordinates(event: { flightId: string; startTime: string | number | Date; endTime: string | number | Date; }) {
        const flightId = parseInt(event.flightId);
        const startDate = new Date(event.startTime);
        const endDate = new Date(event.endTime);
        const timezoneOffset = startDate.getTimezoneOffset() * 60 * 1000;
        const startTime = Math.floor((startDate.getTime() - timezoneOffset) / 1000);
        const endTime = Math.floor((endDate.getTime() - timezoneOffset) / 1000);

        try {
            const response = await fetch(`/protected/coordinates/time_range?start_time=${startTime}&end_time=${endTime}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ flightId })
            });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            const data = await response.json();
            return data.coordinates;
        } catch (error) {
            console.error('Detailed error in loadCoordinates:', error);
            return null;
        }
    }

    toggleEventList = () => {
        this.setState(prevState => ({ showEventList: !prevState.showEventList }));
    };

    render() {
        const { loading, error, showEventList, events } = this.state;

        const eventList = (events.length > 0) && (

            <div className="card flex flex-col w-[75%] h-full max-h-full overflow-clip"> 

                {/* Header */}
                <div className="text-2xl card-header">
                    Proximity Events
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
                                event: { id: React.Key | null | undefined; flightId: string | number | bigint | boolean | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | React.ReactPortal | Promise<string | number | bigint | boolean | React.ReactPortal | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | null | undefined> | null | undefined; otherFlightId: string | number | bigint | boolean | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | React.ReactPortal | Promise<string | number | bigint | boolean | React.ReactPortal | React.ReactElement<unknown, string | React.JSXElementConstructor<any>> | Iterable<React.ReactNode> | null | undefined> | null | undefined; startTime: string | number | Date; endTime: string | number | Date; severity: number; },
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
            <div className="alert alert-danger p-2! m-2!">
                <strong>Error:</strong> {error}
            </div>
        ) : null;

        const loadingDisplay = loading ? (
            <div className="text-center">
                <div className="spinner-border" role="status">
                    <span className="sr-only">Loading...</span>
                </div>
            </div>
        ) : null;

        const mapContent = (
            <div
                id="map"
                style={{ minHeight: "500px" }}
                className="map card overflow-clip m-0 flex border-[var(--c_border_alt)] w-full h-full! relative"
            ></div>
        );

        const { popupContentData } = this.state;
        const mapPopupContent = (
            <div>
                <div className="mt-2">Proximity Event Details</div>
                <hr/>
                <div><strong>Time: </strong>{popupContentData.time??'...'}</div>
                <div><strong>Coordinates: </strong> {popupContentData.coordinates??'...'}</div>
                <hr/>
                <div><strong>Flight ID: </strong>{popupContentData.flightId??'...'}</div>
                <div><strong>Airframe: </strong>{popupContentData.flightAirframe??'...'}</div>
                <hr/>
                <div><strong>Other Flight ID: </strong>{popupContentData.otherFlightId??'...'}</div>
                <div><strong>Other Airframe: </strong>{popupContentData.otherFlightAirframe??'...'}</div>
                <hr/>
                <div><strong>Severity: </strong>{popupContentData.severity??'...'}</div>
            </div>
        );


        const mapPopup = (
            <div id="popup" className="ol-popup" style={{
                position: 'absolute',
                boxShadow: '0 1px 4px rgba(0,0,0,0.2)',
                padding: '15px',
                borderRadius: '10px',
                border: '1px solid #cccccc',
                bottom: '12px',
                left: '-50px',
                minWidth: '200px',
                zIndex: 100,
                display: 'none'
            }}>
                <a href="#" id="popup-closer" className="ol-popup-closer"
                    style={{ position: 'absolute', top: 2, right: 8, textDecoration: 'none' }}>
                    ✖
                </a>
                {mapPopupContent}
            </div>
        );

        return (
            <div
                className="w-full h-full grow flex flex-col"
                id="proximity-map-page-content"
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

                <div className="flex flex-row flex-grow min-h-0 w-full overflow-hidden p-2 gap-2">

                    {/* Loading Indicator */}
                    {loadingDisplay}

                    {/* Map Content */}
                    {
                        (!loading)
                        &&
                        <>
                            {mapContent}
                            {mapPopup}
                        </>
                    }

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