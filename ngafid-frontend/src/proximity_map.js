import React from 'react';
import { createRoot } from 'react-dom/client';
import SignedInNavbar from './signed_in_navbar';
import { errorModal } from './error_modal';
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
import Fill from 'ol/style/Fill';
import Stroke from 'ol/style/Stroke';
import RegularShape from 'ol/style/RegularShape';

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
                radius: 2.9,
                opacity: 0.5,
                gradient: ['#00f', '#0ff', '#0f0', '#ff0', '#f00']
            });

            const heatmapLayer2 = new Heatmap({
                source: heatmapSource2,
                blur: 3,
                radius: 2.9,
                opacity: 0.5,
                gradient: ['#00f', '#0ff', '#0f0', '#ff0', '#f00']
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

            map.on('moveend', () => {
                const zoom = map.getView().getZoom();
                markerLayer.setVisible(zoom >= 12);
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

        const squareStyle = new Style({
            image: new RegularShape({
                points: 4,
                radius: 8,
                angle: Math.PI / 4,
                fill: new Fill({ color: 'blue' }),
                stroke: new Stroke({ color: 'white', width: 1 })
            })
        });

        const starStyle = new Style({
            image: new RegularShape({
                points: 5,
                radius: 10,
                radius2: 4,
                angle: 0,
                fill: new Fill({ color: 'gold' }),
                stroke: new Stroke({ color: 'black', width: 1 })
            })
        });

        for (const event of events) {
            const coordinates1 = await this.loadCoordinates({ ...event, flightId: event.flightId });
            const coordinates2 = await this.loadCoordinates({ ...event, flightId: event.otherFlightId });

            if (coordinates1) {
                coordinates1.forEach(coord => {
                    const olCoord = fromLonLat(coord);
                    heatmapSource1.addFeature(new Feature({
                        geometry: new Point(olCoord),
                    }));
                    const marker = new Feature({ geometry: new Point(olCoord) });
                    marker.setStyle(squareStyle);
                    markerSource.addFeature(marker);
                });
            }

            if (coordinates2) {
                coordinates2.forEach(coord => {
                    const olCoord = fromLonLat(coord);
                    heatmapSource2.addFeature(new Feature({
                        geometry: new Point(olCoord),
                    }));
                    const marker = new Feature({ geometry: new Point(olCoord) });
                    marker.setStyle(starStyle);
                    markerSource.addFeature(marker);
                });
            }
        }

        const extent1 = heatmapSource1.getExtent();
        const extent2 = heatmapSource2.getExtent();
        const combinedExtent = extent1.concat(extent2);
        if (heatmapSource1.getFeatures().length > 0 || heatmapSource2.getFeatures().length > 0) {
            map.getView().fit(combinedExtent, { padding: [50, 50, 50, 50], maxZoom: 15 });
        }
    }

    async loadCoordinates(event) {
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

    renderEventList = () => {
        const { events } = this.state;
        if (events.length === 0) return <div className="alert alert-info">No proximity events found.</div>;
        return (
            <div className="table-responsive">
                <table className="table table-striped table-hover">
                    <thead>
                    <tr>
                        <th>Flight ID</th>
                        <th>Other Flight ID</th>
                        <th>Start Time</th>
                        <th>End Time</th>
                        <th>Severity</th>
                        <th>Lateral Distance (ft)</th>
                        <th>Vertical Distance (ft)</th>
                        <th>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {events.map(event => (
                        <tr key={event.id}>
                            <td>{event.flightId}</td>
                            <td>{event.otherFlightId}</td>
                            <td>{new Date(event.startTime).toLocaleString()}</td>
                            <td>{new Date(event.endTime).toLocaleString()}</td>
                            <td>
                                    <span className={`badge badge-${event.severity > 0.7 ? 'danger' : event.severity > 0.3 ? 'warning' : 'info'}`}>
                                        {event.severity.toFixed(2)}
                                    </span>
                            </td>
                            <td>{event.lateralDistance?.toFixed(2) || 'N/A'}</td>
                            <td>{event.verticalDistance?.toFixed(2) || 'N/A'}</td>
                            <td>
                                <button className="btn btn-sm btn-primary" onClick={() => this.showEventDetails?.(event.id)}>
                                    Details
                                </button>
                            </td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        );
    };

    render() {
        const { loading, error, showEventList } = this.state;
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
                                {showEventList && this.renderEventList()}
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
    } else {
        console.error("Could not find #proximity-map-page element!");
    }
});
