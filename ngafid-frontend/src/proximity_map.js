// At the top of your file
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

class ProximityMapPage extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            events: [],
            loading: false,
            error: null,
            showEventList: true,
            map: null,
            heatmapLayer1: null,
            heatmapLayer2: null,
            markerSource: null
        };
    }

    componentDidMount() {
        requestAnimationFrame(() => {
            const heatmapSource1 = new VectorSource();
            const heatmapSource2 = new VectorSource();
            const markerSource = new VectorSource();

            const heatmapLayer1 = new Heatmap({
                source: heatmapSource1,
                blur: 3,
                radius: 1.5,
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
                radius: 1.5,
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

            // --- MODIFIED SINGLECLICK HANDLER ---
            map.on('singleclick', function (event) {
                map.forEachFeatureAtPixel(event.pixel, function (feature) {
                    const geometry = feature.getGeometry();
                    const coord = geometry.getCoordinates();
                    const props = feature.getProperties();
                    if (!props.isMarker) return;

                    const isPrimaryFlight = props.flightId === props.eventFlightId;
                    const offset = isPrimaryFlight ? [-220, 0] : [20, 0];
                    const positioning = isPrimaryFlight ? 'center-right' : 'center-left';

                    const container = document.createElement('div');
                    container.className = 'ol-popup';
                    container.style.cssText = `
                        position: absolute;
                        background-color: white;
                        box-shadow: 0 1px 4px rgba(0,0,0,0.2);
                        padding: 15px;
                        border-radius: 10px;
                        border: 1px solid #cccccc;
                        min-width: 200px;
                        z-index: 100;
                    `;

                    const closer = document.createElement('a');
                    closer.href = '#';
                    closer.textContent = '✖';
                    closer.className = 'ol-popup-closer';
                    closer.style.cssText = `
                        position: absolute;
                        top: 2px;
                        right: 8px;
                        text-decoration: none;
                    `;
                    container.appendChild(closer);

                    const content = document.createElement('div');
                    content.innerHTML = `
                        <div class="popup-content">
                            <h3>Proximity Event Details</h3>
                            <p><strong>Time:</strong> ${new Date(props.time).toLocaleString()}</p>
                            <p><strong>Coordinates:</strong> ${coord.map(n => n.toFixed(5)).join(', ')}</p>
                            <p><strong>Flight ID:</strong> ${props.flightId}</p>
                            <p><strong>Airframe:</strong> ${props.flightAirframe || 'N/A'}</p>
                            <p><strong>Other Flight ID:</strong> ${props.otherFlightId}</p>
                            <p><strong>Other Airframe:</strong> ${props.otherFlightAirframe || 'N/A'}</p>
                            <p><strong>Severity:</strong> ${props.severity}</p>
                        </div>
                    `;
                    container.appendChild(content);

                    const overlay = new Overlay({
                        element: container,
                        positioning: positioning,
                        offset: offset,
                        autoPan: true,
                        autoPanAnimation: { duration: 250 }
                    });

                    map.addOverlay(overlay);
                    overlay.setPosition(coord);

                    closer.onclick = () => {
                        map.removeOverlay(overlay);
                        return false;
                    };
                });
            });

            map.on('moveend', () => {
                markerLayer.setVisible(map.getView().getZoom() >= 12);
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
        } catch (error) {
            console.error('Error loading events:', error);
            this.setState({ error: error.message, loading: false });
            return [];
        }
    }

    async processEventCoordinates(events) {
        const { map, heatmapLayer1, heatmapLayer2, markerSource } = this.state;
        const heatmapSource1 = heatmapLayer1.getSource();
        const heatmapSource2 = heatmapLayer2.getSource();
        heatmapSource1.clear();
        heatmapSource2.clear();
        markerSource.clear();

        const blackPointStyle = new Style({
            image: new Icon({
                src: '/images/black-point.png',
                scale: 0.05,
                anchor: [0.5, 0.5],
            })
        });

        const redPointStyle = new Style({
            image: new Icon({
                src: '/images/red-point.png',
                scale: 0.05,
                anchor: [0.5, 0.5],
            })
        });

        // Fetch all proximity points once
        const allProximityEvents = await this.loadProximityPoints();
        for (const event of allProximityEvents) {
            const eventPoints = event.points || [];
            if (eventPoints.length === 0) continue;
            // Use the last point for marker (or pick another logic if needed)
            const lastPoint = eventPoints[eventPoints.length - 1];
            const olCoord = fromLonLat([lastPoint.longitude, lastPoint.latitude]);
            const marker = new Feature({ geometry: new Point(olCoord) });
            marker.setStyle(redPointStyle); // Only one flight per event object now
            marker.setProperties({
                isMarker: true,
                eventFlightId: event.flight_id,
                flightId: event.flight_id,
                time: lastPoint.timestamp,
                lateralDistance: event.lateral_distance,
                verticalDistance: event.vertical_distance
            });
            markerSource.addFeature(marker);

            // Add all points to heatmap layer
            eventPoints.forEach(point => {
                const olCoord = fromLonLat([point.longitude, point.latitude]);
                const feature = new Feature({ geometry: new Point(olCoord) });
                feature.set('weight', 10);
                heatmapSource1.addFeature(feature);
            });
        }

        const extent1 = heatmapSource1.getExtent();
        const extent2 = heatmapSource2.getExtent();
        const combinedExtent = extent1.concat(extent2);
        if (heatmapSource1.getFeatures().length > 0 || heatmapSource2.getFeatures().length > 0) {
            map.getView().fit(combinedExtent, { padding: [50, 50, 50, 50], maxZoom: 15 });
        }
    }

    async loadProximityPoints() {
        try {
            const response = await fetch('http://localhost:8181/protected/all_proximity_points', {
                credentials: 'include',
                headers: { 'Accept': 'application/json' }
            });
            if (!response.ok) throw new Error('Failed to load proximity points');
            return await response.json(); // [ { event_id, flight_id, points, lateral_distance, vertical_distance }, ... ]
        } catch (error) {
            console.error('Error loading proximity points:', error);
            return [];
        }
    }

    toggleEventList = () => {
        this.setState(prevState => ({ showEventList: !prevState.showEventList }));
    };

    render() {
        const { loading, error, showEventList, events } = this.state;
        return (
            <div className="container-fluid mt-4">
                <div className="row">
                    <div className="col-12">
                        <h1>Proximity Events</h1>
                        {error && <div className="alert alert-danger">Error loading events: {error}</div>}
                        {loading ? (
                            <div className="text-center">
                                <div className="spinner-border" role="status">
                                    <span className="sr-only">Loading...</span>
                                </div>
                            </div>
                        ) : (
                            <>
                                <div className="mb-3">
                                    <button className="btn btn-secondary" onClick={this.toggleEventList}>
                                        {showEventList ? 'Hide Event List' : 'Show Event List'}
                                    </button>
                                </div>
                                <div id="map" style={{ height: '100vh', width: '100%', position: 'relative' }}></div>
                                {showEventList && events.length > 0 && (
                                    <div className="table-responsive mt-3">
                                        <table className="table table-striped table-hover">
                                            <thead>
                                            <tr>
                                                <th>Flight ID</th>
                                                <th>Other Flight ID</th>
                                                <th>Start Time</th>
                                                <th>End Time</th>
                                                <th>Severity</th>
                                            </tr>
                                            </thead>
                                            <tbody>
                                            {events.map(event => (
                                                <tr key={event.id}>
                                                    <td>{event.flightId}</td>
                                                    <td>{event.otherFlightId}</td>
                                                    <td>{new Date(event.startTime).toLocaleString()}</td>
                                                    <td>{new Date(event.endTime).toLocaleString()}</td>
                                                    <td>{event.severity.toFixed(2)}</td>
                                                </tr>
                                            ))}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </>
                        )}
                    </div>
                </div>
            </div>
        );
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const navbarContainer = document.querySelector('#navbar');
    if (navbarContainer) {
        const navbarRoot = createRoot(navbarContainer);
        navbarRoot.render(<SignedInNavbar activePage="proximity_map" />);
    }

    const pageContainer = document.querySelector('#proximity-map-page');
    if (pageContainer) {
        const pageRoot = createRoot(pageContainer);
        pageRoot.render(<ProximityMapPage />);
    }
});
