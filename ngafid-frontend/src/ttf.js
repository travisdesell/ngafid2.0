import 'bootstrap';
import React from "react";
import { createRoot } from 'react-dom/client';

import { Vector as VectorLayer } from 'ol/layer.js';
import { fromLonLat } from 'ol/proj.js';
import { Vector as VectorSource } from 'ol/source.js';
import { Circle, Stroke, Style } from 'ol/style.js';
import { closer, container, initializeMap, layers, map, overlay, styles } from "./map.js";
import { paletteGenerator } from "./map_utils.js";
import SignedInNavbar from "./signed_in_navbar.js";
import { TimeHeader, TurnToFinalHeaderComponents } from "./time_header.js";

import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';

import Plotly from 'plotly.js';
import { showErrorModal } from './error_modal.js';

import './index.css'; //<-- include Tailwind
import { showAjaxErrorModal } from './extract_ajax_error_message.js';

const ROLL_THRESHOLDS = {
    Min: 0,
    Default: 0,
    Dangerous: 26,
    MaxSoft: 30,
    MaxHard: 45
};

class TTFMapPopup extends React.Component {

    constructor(props) {
        super(props);
        this.popupRef = React.createRef();
        this.dragStart = { x: 0, y: 0 };
        this.popupStart = { left: props.initialLeft, top: props.initialTop };
        this.currentPosition = { left: props.initialLeft, top: props.initialTop };
        this.dragging = false;
        this.frameHandle = null;
        this.frameIsAnimationFrame = false;
        this.pendingPosition = null;

        this.handleMouseMove = this.handleMouseMove.bind(this);
        this.handleMouseUp = this.handleMouseUp.bind(this);
    }

    componentDidMount() {
        this.applyPosition(this.currentPosition.left, this.currentPosition.top);
    }

    componentWillUnmount() {
        window.removeEventListener('mousemove', this.handleMouseMove);
        window.removeEventListener('mouseup', this.handleMouseUp);
        if (this.frameHandle !== null) {
            if (this.frameIsAnimationFrame && typeof window.cancelAnimationFrame === 'function') {
                window.cancelAnimationFrame(this.frameHandle);
            } else {
                window.clearTimeout(this.frameHandle);
            }
            this.frameHandle = null;
            this.frameIsAnimationFrame = false;
        }
    }

    onDragStart(e) {
        e.preventDefault();
        e.stopPropagation();
        this.dragStart = { x: e.clientX, y: e.clientY };
        this.popupStart = { left: this.currentPosition.left, top: this.currentPosition.top };
        this.dragging = true;

        if (this.popupRef.current) {
            this.popupRef.current.style.cursor = 'grabbing';
        }

        window.addEventListener('mousemove', this.handleMouseMove);
        window.addEventListener('mouseup', this.handleMouseUp);
    }

    applyPosition(left, top) {
        if (!this.popupRef.current)
            return;

        this.popupRef.current.style.left = `${left}px`;
        this.popupRef.current.style.top = `${top}px`;
    }

    schedulePositionUpdate(left, top) {
        this.pendingPosition = { left, top };
        if (this.frameHandle !== null)
            return;

        const flush = () => {
            this.frameHandle = null;
            this.frameIsAnimationFrame = false;
            if (!this.pendingPosition)
                return;

            const { left: nextLeft, top: nextTop } = this.pendingPosition;
            this.currentPosition = { left: nextLeft, top: nextTop };
            this.applyPosition(nextLeft, nextTop);
            this.pendingPosition = null;
        };

        if (typeof window.requestAnimationFrame === 'function') {
            this.frameIsAnimationFrame = true;
            this.frameHandle = window.requestAnimationFrame(flush);
        } else {
            this.frameHandle = window.setTimeout(flush, 16);
        }
    }

    handleMouseMove(e) {

        // No container/not dragging, exit
        if (!this.props.mapContainer || !this.dragging)
            return;

        e.preventDefault();

        const dx = e.clientX - this.dragStart.x;
        const dy = e.clientY - this.dragStart.y;

        const mapRect = this.props.mapContainer.getBoundingClientRect();
        const popupWidth = this.popupRef.current?.offsetWidth ?? 300;
        const popupHeight = this.popupRef.current?.offsetHeight ?? 140;
        const maxLeft = Math.max(0, mapRect.width - popupWidth);
        const maxTop = Math.max(0, mapRect.height - popupHeight);

        const left = Math.max(0, Math.min(this.popupStart.left + dx, maxLeft));
        const top = Math.max(0, Math.min(this.popupStart.top + dy, maxTop));

        this.schedulePositionUpdate(left, top);
    }

    handleMouseUp() {
        this.dragging = false;
        window.removeEventListener('mousemove', this.handleMouseMove);
        window.removeEventListener('mouseup', this.handleMouseUp);

        if (this.popupRef.current)
            this.popupRef.current.style.cursor = 'default';
    }

    render() {
        const hasFlightId = !!this.props.flightId;
        return (
            <div
                ref={this.popupRef}
                className="ol-popup"
                style={{
                    position: 'absolute',
                    zIndex: 120,
                    minWidth: 260,
                    maxWidth: 340,
                    pointerEvents: 'auto',
                    boxShadow: '0 1px 4px rgba(0,0,0,0.2)',
                    borderRadius: '10px',
                    border: '1px solid #cccccc',
                    padding: '0.6rem 0.75rem 0.75rem',
                    background: 'var(--c_card_header_bg_opaque)'
                }}
            >
                <div
                    className="d-flex justify-content-between align-items-center mb-2"
                    style={{ userSelect: 'none', gap: '8px' }}
                >
                    {/* Grab & Drag Area */}
                    <div
                        className={`
                            group
                            ${this.dragging ? 'cursor-grabbing' : 'cursor-grab'}
                            mb-2
                        `}
                        onMouseDown={(e) => this.onDragStart(e)}
                    >
                        <span className={`
                            fa fa-arrows
                            ${this.dragging ? 'opacity-100 select-none' : 'opacity-25 group-hover:opacity-100 select-auto'}
                            mr-2
                            scale-100 group-hover:scale-125
                            transition-all duration-200 ease-in-out
                        `}/>
                    </div>
                    <div className="d-flex align-items-center" style={{ gap: '6px', marginLeft: 'auto' }}>
                        {
                            (hasFlightId)
                                ? (
                                    <a
                                        className="btn btn-sm btn-outline-info"
                                        href={`/protected/flight?flight_id=${this.props.flightId}`}
                                        target="_blank"
                                        rel="noreferrer"
                                        title="Open flight"
                                    >
                                        <i className="fa fa-plane" aria-hidden="true"></i>
                                    </a>
                                )
                                : (
                                    <button
                                        type="button"
                                        className="btn btn-sm btn-outline-info grayscale opacity-50"
                                        disabled
                                        title="Flight ID unavailable"
                                    >
                                        <i className="fa fa-plane" aria-hidden="true"></i>
                                    </button>
                                )
                        }
                        <button
                            type="button"
                            className="btn btn-sm btn-outline-danger"
                            onClick={() => this.props.onClose(this.props.popupId)}
                            title="Close popup"
                        >
                            <i className="fa fa-times" aria-hidden="true"></i>
                        </button>
                    </div>
                </div>
                <div>
                    <div><strong>Flight ID:</strong> {this.props.flightId || '(unknown)'}</div>
                    <div><strong>Approach:</strong> {this.props.approachN ?? '(unknown)'}</div>
                </div>
            </div>
        );
    }
}

class RollSlider extends React.Component {
    constructor(props) {
        super(props);
    }

    makeRollSlider(min, max, onChange, getValue) {
        return (
            <div className="col-auto min-w-72" style={{ textAlign: 'center', margin: 'auto' }}
            >

                {/* Minimum Roll Value */}
                <span>
                    Minimum Roll Value = {getValue()}&deg;
                </span>

                {/* Slider Range Values */}
                <div className="w-full flex-row flex gap-2 h-6">
                    <span>{min}&deg;</span>
                    <hr className="w-full bg-(--c_border_alt)" />
                    <span>{max}&deg;</span>
                </div>

                {/* Slider Input */}
                <input
                    id="rollSlider" type="range" min={min} max={max} value={getValue()} className="slider w-full"
                    onChange={(val) => onChange(val)}
                    style={{ margin: 'auto', verticalAlign: 'middle' }}
                />

                {/* Gradient Bar */}
                <div className="w-full" style={{
                    margin: "auto",
                    backgroundImage: "linear-gradient(90deg, rgb(0, 255, 0), 55%, rgb(255, 255, 0), 66%, rgb(255, 0, 0))",
                    height: "4px"
                }}></div>

            </div>
        );
    }

    render() {
        return this.makeRollSlider(this.props.rollSliderMin, this.props.rollSliderMax, this.props.rollSliderChanged, this.props.rollSliderValue);
    }
}

const rollPalette = paletteGenerator([[0, 255, 0], [255, 255, 0], [255, 0, 0]], [ROLL_THRESHOLDS.Min, ROLL_THRESHOLDS.Dangerous, ROLL_THRESHOLDS.MaxSoft]);


class TTFCard extends React.Component {

    constructor(props) {
        super(props);
        const date = new Date();

        // Missing airports data, exit
        if (!airports || airports.length === 0) {

            console.error("No airports data found!");
            showErrorModal("Missing Airports", "Airport data is required for this page to function correctly, but no airport data was found.");

        }

        const selectedAirportInitial = (airports?.[0] ?? null);
        this.state = {
            // The start of the date range that this.state.data corresponds to.
            // This will be null if and only if this.state.data is null
            dataStartDate: null,

            // The end of the date range that this.state.data corresponds to.
            // This will be null if and only if this.state.data is null
            dataEndDate: null,

            // Data is an object containing the following information:
            //     data = {
            //         airports: [ <list of airports that appear in the ttf list> ],
            //         ttfs: [ <list of turn to final objects> ],
            //     }
            // The turn to final objects are defined in the org.ngafid.flights.TurnToFinal::jsonify method.
            data: null,

            datesChanged: true,

            minRoll: ROLL_THRESHOLDS.Default,

            startYear: date.getFullYear(),
            startMonth: 1,
            endYear: date.getFullYear(),
            endMonth: date.getMonth() + 1,
            startDate: `${date.getFullYear()}-01-01`,
            startDateObject: this.parseDate(`${date.getFullYear()}-01-01`),
            endDate: `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(new Date(date.getFullYear(), date.getMonth(), 0).getDate()).padStart(2, '0')}`,
            endDateObject: this.parseDate(`${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(new Date(date.getFullYear(), date.getMonth(), 0).getDate()).padStart(2, '0')}`),

            selectedAirport: selectedAirportInitial,
            selectedRunway: "Any Runway",
            mapVisible: true,
            plotVisible: true,
            mapStyle: "Road",
            disableFetching: false,
            // Style object for ttf lines. This is just a thin green line style.
            ttfStyle:
                new Style({
                    stroke: new Stroke({
                        color: "#00ff00",
                        width: 2.5
                    }),
                    image: new Circle({
                        radius: 5,
                        //fill: new Fill({color: [0, 0, 0, 255]}),
                        stroke: new Stroke({
                            color: [0, 0, 0, 0],
                            width: 2
                        })
                    })
                }),
        };

            this.popupMounts = [];
            this.nextPopupId = 0;
            this.pendingFetchRequest = null;
            this.fetchId = 0;
            this.loadingEstimateInterval = null;

        const navbarContainer = document.querySelector('#navbar');
        const navbarRoot = createRoot(navbarContainer);
        navbarRoot.render(
            <SignedInNavbar
                filterVisible={false}
                showPlotButton={false}
                disableMapButton={true}
                mapVisible={this.state.mapVisible}
                activePage="ttf"
                filterSelected={false}
                mapStyle={this.state.mapStyle}
                togglePlot={() => this.togglePlot()}
                toggleFilter={() => this.toggleFilter()}
                toggleMap={() => this.toggleMap()}
                mapSelectChanged={(style) => this.mapSelectChanged(style)}
                waitingUserCount={waitingUserCount}
                fleetManager={fleetManager}
                unconfirmedTailsCount={unconfirmedTailsCount}
                modifyTailsAccess={modifyTailsAccess}
                darkModeOnClickAlt={() => {
                    this.displayPlots();
                }}
            />
        );

    }

    extractFlightId(ttf) {

        const candidates = [
            ttf?.popupFlightId,
            ttf?.flight_id,
            ttf?.flightId,
            ttf?.flightID,
            ttf?.id,
            ttf?.flight?.id,
            ttf?.flight?.flightId,
            ttf?.flight?.flight_id
        ];

        for (const value of candidates) {

            // Got empty/null/undefined value, skip
            if (value === null || value === undefined)
                continue;

            const str = String(value).trim();

            // Stringified value is empty, skip
            if (str.length === 0)
                continue;

            // "Approach N" is a display label, not the canonical flight ID, skip
            if (/^approach\s+\d+$/i.test(str))
                continue;

            return str;

        }

        return null;

    }

    normalizeFlightTimestamp(value) {

        // Got empty/null/undefined value -> null
        if (value === null || value === undefined)
            return null;

        const str = String(value).trim();

        // Stringified value is empty -> null
        if (str.length === 0)
            return null;

        // Attempt simplified parsing for common timestamp formats (e.g., "2024-01-01 12:00:00")
        const simpleMatch = str.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2}:\d{2})/);
        if (simpleMatch)
            return `${simpleMatch[1]} ${simpleMatch[2]}`;

        // Fallback to Date parsing
        const parsed = Date.parse(str);
        if (Number.isNaN(parsed))
            return null;

        return new Date(parsed).toISOString().slice(0, 19).replace('T', ' ');

    }

    buildFlightLookupIndex(flights) {
        const lookup = new Map();

        for (const flight of flights ?? []) {
            const timestampKey = this.normalizeFlightTimestamp(flight?.startDateTime);

            // Timestamp missing, skip
            if (!timestampKey)
                continue;

            // Target timestamp key doesn't exist in lookup, initialize with empty array
            if (!lookup.has(timestampKey))
                lookup.set(timestampKey, []);

            // Add current flight to lookup under the normalized timestamp key
            lookup.get(timestampKey).push(flight);

        }

        return lookup;

    }

    matchFlightIdFromLookup(ttf, flightLookup, airportIataCode) {

        // Flight lookup is not a valid Map instance, cannot perform lookup -> null
        if (!(flightLookup instanceof Map))
            return null;

        const timestampKey = this.normalizeFlightTimestamp(ttf?.flightStartDate);

        // Timestamp missing/invalid, cannot perform lookup -> null
        if (!timestampKey)
            return null;

        const candidates = flightLookup.get(timestampKey) ?? [];

        // No flights found matching the TTF's normalized flight start timestamp, cannot determine flight ID -> null
        if (candidates.length === 0)
            return null;

        const runwayName = ttf?.runway?.name;
        const matchingCandidates = candidates.filter((flight) => {
            const itinerary = Array.isArray(flight?.itinerary) ? flight.itinerary : [];
            return itinerary.some((stop) => {

                // Stop doesn't have an airport or runway -> false
                if (stop?.airport !== airportIataCode)
                    return false;

                // Runway name not provided, attmpt to match any stop at the airport
                if (!runwayName || stop?.runway === runwayName)
                    return true;

                return false;
            });
        });

        const resolvedCandidates = matchingCandidates.length > 0 ? matchingCandidates : candidates;
        const uniqueFlightIds = [...new Set(resolvedCandidates
            .map((flight) => flight?.id)
            .filter((flightId) => flightId !== null && flightId !== undefined))];

        if (uniqueFlightIds.length === 1)
            return String(uniqueFlightIds[0]);

        return null;
    }

    fetchFlightLookupForMissingIds(startDate, endDate, airportIataCode) {
        const overlapFilter = {
            type: 'GROUP',
            condition: 'OR',
            filters: [
                {
                    type: 'GROUP',
                    condition: 'AND',
                    filters: [
                        { type: 'RULE', inputs: ['Start Date', '>=', startDate] },
                        { type: 'RULE', inputs: ['Start Date', '<=', endDate] }
                    ]
                },
                {
                    type: 'GROUP',
                    condition: 'AND',
                    filters: [
                        { type: 'RULE', inputs: ['End Date', '>=', startDate] },
                        { type: 'RULE', inputs: ['End Date', '<=', endDate] }
                    ]
                }
            ]
        };

        const filters = {
            type: 'GROUP',
            condition: 'AND',
            filters: [
                { type: 'RULE', inputs: ['Airport', airportIataCode, 'visited'] },
                overlapFilter
            ]
        };

        return new Promise((resolve, reject) => {
            $.ajax({
                type: 'GET',
                url: '/api/flight',
                data: {
                    filterQuery: JSON.stringify(filters),
                    currentPage: 0,
                    pageSize: 10000,
                    sortingColumn: 'start_time',
                    sortingOrder: 'Ascending'
                },
                async: true,
                success: (response) => {

                    if (response?.errorTitle || response?.errorMessage) {
                        reject(new Error(response.errorMessage || response.errorTitle || 'Unable to fetch flights for TTF lookup.'));
                        showErrorModal(
                            response.errorTitle || 'Error Fetching Flights for TTF Lookup',
                            response.errorMessage || 'An unknown error occurred while fetching flights for TTF lookup.'
                        );
                        return;
                    }

                    resolve(this.buildFlightLookupIndex(response?.flights ?? []));
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    reject(new Error(errorThrown || textStatus || jqXHR?.responseText || 'Unable to fetch flights for TTF lookup.'));
                    showAjaxErrorModal(jqXHR, errorThrown, 'Unable to Fetch Flights for TTF Lookup');
                }
            });
        });
    }

    componentDidMount() {

        initializeMap();
        if (map && !map.get('ttfHandlersAttached')) {
            // https://embed.plnkr.co/plunk/hhEAWk
            map.on('click', (event => this.openMapPopup(event)));
            map.on('moveend', (() => this.zoomChanged()));
            map.set('ttfHandlersAttached', true);
        }

        //Display plots
        this.displayPlots();

        //Update map layers to use default style
        this.mapSelectChanged(this.state.mapStyle);

    }

    componentWillUnmount() {
        this.clearAllPopups();
        this.stopLoadingEstimateCountdown();
    }

    startLoadingEstimateCountdown(initialSeconds = 15) {
        this.stopLoadingEstimateCountdown();
        let remaining = initialSeconds;
        const updateAll = (text, visible) => {
            document.querySelectorAll('#loading-estimate').forEach((el) => {
                el.textContent = text;
                el.style.display = visible ? '' : 'none';
            });
        };
        updateAll(`Estimated loading time: ${remaining} s`, true);
        this.loadingEstimateInterval = setInterval(() => {
            remaining -= 1;
            if (remaining <= 0) {
                updateAll('', false);
                this.stopLoadingEstimateCountdown();
            } else {
                updateAll(`Estimated loading time: ${remaining} s`, true);
            }
        }, 1000);
    }

    stopLoadingEstimateCountdown() {
        if (this.loadingEstimateInterval) {
            clearInterval(this.loadingEstimateInterval);
            this.loadingEstimateInterval = null;
        }
    }

    removePopupById(popupId) {
        const index = this.popupMounts.findIndex((mount) => mount.id === popupId);
        if (index === -1)
            return;

        const [mount] = this.popupMounts.splice(index, 1);
        mount.root.unmount();
        mount.node.remove();
    }

    clearAllPopups() {
        for (const mount of this.popupMounts) {
            mount.root.unmount();
            mount.node.remove();
        }
        this.popupMounts = [];
    }

    openMapPopup(event) {
        closer.onclick = function () {
            overlay.setPosition(undefined);
            closer.blur();
            return false;
        };

        const f = map.forEachFeatureAtPixel(event.pixel, function (feature) {
            return feature;
        });

        if (f && f.get('type') == 'ttf') {
            const ttfObject = f.get('ttf');
            const mapElement = document.getElementById('map');
            if (!mapElement) {
                console.warn('Cannot open TTF popup: missing #map');
                return;
            }

            mapElement.style.position = 'relative';
            const popupId = `ttf-popup-${++this.nextPopupId}`;
            const popupNode = document.createElement('div');
            const popupRoot = createRoot(popupNode);
            const estimatedWidth = 320;
            const estimatedHeight = 140;
            const maxLeft = Math.max(0, mapElement.clientWidth - estimatedWidth);
            const maxTop = Math.max(0, mapElement.clientHeight - estimatedHeight);
            const initialLeft = Math.max(0, Math.min(event.pixel[0] + 12, maxLeft));
            const initialTop = Math.max(0, Math.min(event.pixel[1] + 12, maxTop));
            const flightId = this.extractFlightId(ttfObject) ?? f.get('flightId') ?? null;

            mapElement.appendChild(popupNode);
            this.popupMounts.push({ id: popupId, node: popupNode, root: popupRoot });

            popupRoot.render(
                <TTFMapPopup
                    popupId={popupId}
                    mapContainer={mapElement}
                    initialLeft={initialLeft}
                    initialTop={initialTop}
                    flightId={flightId}
                    approachN={ttfObject?.approachn}
                    onClose={(id) => this.removePopupById(id)}
                />
            );

        } else {

            container.style.display = 'none';

        }
    }

    zoomChanged() {
        this.clearAllPopups();
    }

    mapSelectChanged(style) {
        for (let i = 0, ii = layers.length; i < ii; ++i) {
            layers[i].setVisible(styles[i] === style);
        }

        this.setState({ mapStyle: style });
    }

    showMap() {
        if (this.state.mapVisible) return;

        if (!$("#map-toggle-button").hasClass("active")) {
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

        this.setState({ mapVisible: true });

        $("#map-div").css("height", "50%");
        $("#map").show();

        $("#map").css("width", "100%");
    }

    hideMap() {
        if (!this.state.mapVisible) return;

        if ($("#map-toggle-button").hasClass("active")) {
            $("#map-toggle-button").removeClass("active");
            $("#map-toggle-button").attr("aria-pressed", false);
        }

        this.setState({ mapVisible: false });

        $("#map").hide();

        $("#map").css("height", "0%");
    }

    toggleMap() {
        if (this.state.mapVisible) {
            this.hideMap();
        } else {
            this.showMap();
        }
    }

    toggleFilter() {
        if (this.state.filterVisible) {
            this.hideFilter();
        } else {
            this.showFilter();
        }
    }

    ttfToPoints(ttf) {
        // This will generate a list of points in the appropriate format and save it, so it is only computed once.
        // This should only ever get called once though so I guess this is unnecessary.
        if (typeof ttf.points !== 'undefined')
            return ttf.points;

        const lats = ttf.lat ?? ttf.latitude;
        const lons = ttf.lon ?? ttf.longitude;
        if (!Array.isArray(lats) || !Array.isArray(lons)) {
            console.warn("TTF missing lat/lon arrays", ttf);
            return [];
        }

        const points = [];
        ttf.points = points;

        for (let i = 0; i < lons.length; i++) {
            const lon = Number(lons[i]);
            const lat = Number(lats[i]);
            if (!Number.isFinite(lon) || !Number.isFinite(lat)) {
                continue;
            }
            const point = fromLonLat([lon, lat]);
            points.push(point);
        }

        if (points.length === 0) {
            console.warn("TTF has no valid points", {
                flightId: ttf.flightId,
                latsLength: lats.length,
                lonsLength: lons.length
            });
        }

        return points;
    }

    makeTTFLayers(ttf) {
        // This will generate a layer for this ttf if once hasn't been generated before, and add it to the map as a
        // hidden layer.
        if (typeof ttf.layer !== 'undefined')
            return ttf.layer;

        const points = this.ttfToPoints(ttf);
        if (points.length === 0) {
            return null;
        }

        // Create simple layer of the path.
        const trackingPoint =
            new Feature({
                geometry: new Point(points[0]),
                name: 'TrackingPoint'
            });
        const features = [
            new Feature({
                geometry: new LineString(points),
                name: ttf.popupFlightName ?? ttf.flightId ?? '(unknown)',
                flightId: ttf.popupFlightId,
                type: 'ttf',
                ttf: ttf
            }),
            trackingPoint
        ];

        const layer = new VectorLayer({
            style: this.getStyle(ttf),
            source: new VectorSource({
                features: features,
            }),
        });

        ttf.layer = layer;
        map.addLayer(layer);
        layer.setVisible(false);
        ttf.enabled = true;
        return ttf.layer;
    }

    getStyle(ttf) {
        return new Style({
            stroke: new Stroke({
                // Assuming anything past 45 is really bad, so color will get increasingly red as roll approaches 45
                color: rollPalette(ttf.maxRoll),
                width: 2.5
            }),
            image: new Circle({
                radius: 5,
                //fill: new Fill({color: [0, 0, 0, 255]}),
                stroke: new Stroke({
                    color: [0, 0, 0, 0],
                    width: 2
                })
            })
        });
    }

    updateDisplay() {
        if (this.state.data != null) {
            this.plotTTFs();
            this.plotCharts(this.state.data.ttfs);
        }
    }

    plotTTFs() {
        for (const ttf of this.state.data.ttfs) {
            if (this.shouldDisplay(ttf))
                this.plotTTF(ttf);
            else
                this.hideTTF(ttf);
        }
    }

    plotTTF(ttf) {
        const layer = this.makeTTFLayers(ttf);
        if (!layer) {
            return;
        }
        layer.setVisible(true);
    }

    hideTTF(ttf) {
        const layer = this.makeTTFLayers(ttf);
        if (!layer) {
            return;
        }
        layer.setVisible(false);
    }

    // setMaximumRoll will move the roll slider to the maximum roll found in the set of ttfs so that
    // all flights will be displayed
    plotCharts(ttfs, setMaximumRoll = false) {
        if (setMaximumRoll) {

            let minRoll = Math.min(...ttfs.map(ttf => ttf.maxRoll));
            minRoll = Math.max(minRoll, ROLL_THRESHOLDS.Min);
            if (!isFinite(minRoll)) {
                console.warn("No valid roll values found in ttfs, defaulting to 0");
                minRoll = 0;
            }

            this.onRollSliderChanged(minRoll, true);
        }

        let ttfIndex = -1;
        const curves = ttfs
            .map(ttf => {
                ttfIndex += 1;
                const glideAngle = ttf.selfDefinedGlideAngle;
                const alt = ttf.AltAGL;
                const traceName = ttf.popupTraceName ?? ttf.popupFlightName ?? ttf.flightId ?? ttf.flight_id ?? "(unknown)";

                // This is what applies the roll filter
                if (this.shouldDisplay(ttf)) {
                    return {
                        deviations: {
                            name: traceName,
                            x: ttf.distanceFromRunway,
                            y: ttf.selfDefinedGlidePathDeviations,
                            type: 'scatter',
                            mode: 'lines'
                        },
                        alt: { name: traceName, x: ttf.distanceFromRunway, y: alt, type: 'scatter', mode: 'lines' },
                        maxGlideAngle: glideAngle, _ttfIndex: ttfIndex
                    };
                } else
                    return null;
            })
            .filter(curve => curve != null);
        const curveMap = {};
        for (let i = 0; i < curves.length; i++) {
            curveMap[i] = curves[i]._ttfIndex;
        }
        const deviationsCurves = curves.map(x => x.deviations);

        Plotly.newPlot('deviations-plot', deviationsCurves, this.deviationsPlotlyLayout, this.state.plotlyConfig);

        const maxGlideAngles = curves.map(x => x.maxGlideAngle);
        const glideAngleTrace = {
            type: 'histogram',
            y: maxGlideAngles,
            ybins: {
                end: ROLL_THRESHOLDS.MaxSoft,
                size: 1.0,
                start: 0,
            }
        };

        Plotly.newPlot('glide-angle-hist', [glideAngleTrace], this.glideAngleHistLayout);

        const altCurves = curves.map(x => x.alt);
        Plotly.newPlot('alt-plot', altCurves, this.altitudePlotlyLayout, this.state.plotlyConfig);
    }

    updateMapViewForTTFs(ttfs, airports) {
        const view = map.getView();
        let extent = null;
        for (const ttf of ttfs) {
            const points = this.ttfToPoints(ttf);
            for (const point of points) {
                if (!extent) {
                    extent = [point[0], point[1], point[0], point[1]];
                } else {
                    extent[0] = Math.min(extent[0], point[0]);
                    extent[1] = Math.min(extent[1], point[1]);
                    extent[2] = Math.max(extent[2], point[0]);
                    extent[3] = Math.max(extent[3], point[1]);
                }
            }
        }

        if (extent) {
            view.fit(extent, { padding: [40, 40, 40, 40], maxZoom: 15 });
            return;
        }

        if (airports) {
            const airport = airports[this.state.selectedAirport];
            if (airport && Number.isFinite(airport.longitude) && Number.isFinite(airport.latitude)) {
                view.setCenter(fromLonLat([airport.longitude, airport.latitude]));
                view.setZoom(12);
            }
        }
    }

    getRunwayValue() {
        const runwayElement = this.state.selectedRunway;
        if (runwayElement == null)
            return null;
        else if (this.state.dataAirport != this.getAirportValue())
            return null;
        else
            return runwayElement.value;
    }

    getAirportValue() {
        return this.state.selectedAirport;
    }

    shouldDisplay(ttf) {
        const runway = this.state.selectedRunway;
        const flightDate = this.parseDate(ttf.flightStartDate);
        const withinDateRange = flightDate
            ? this.dateWithinRange(flightDate, this.state.startDateObject, this.state.endDateObject)
            : true;
        const should = ttf.enabled && (this.state.selectedRunway == null || ['Any Runway', ttf.runway.name].includes(runway))
            && withinDateRange
            && ttf.maxRoll >= this.state.minRoll;
        return should;
    }

    // For parsing dates in the format "yyyy-mm-dd hh:mm:ss" where the hh:mm:ss is optional
    parseDate(dateString) {

        if (dateString == null)
            return null;
        const parsed = new Date(dateString);

        // Date string already in a format that Date.parse can handle, use it
        if (!Number.isNaN(parsed.getTime()))
            return parsed;

        const pieces = dateString.split(" ");
        const yyyymmdd = pieces[0].split("-");
        const year = yyyymmdd[0];
        // Minus 1 because dates are zero indexed in javascript
        const month = parseInt(yyyymmdd[1]) - 1;
        const day = yyyymmdd[2];

        if (pieces.length > 1) {

            const hhmmss = pieces[1].split(":");
            const hour = hhmmss[0];
            const minutes = hhmmss[1];
            const seconds = hhmmss[2];

            return new Date(year, month, day, hour, minutes, seconds);

        }

        return new Date(year, month, day);
    }

    // These should all be date objects
    dateWithinRange(date, startDate, endDate) {
        return startDate <= date && date <= endDate;
    }

    computeDatesFromUI() {
        const pad = (n) => n < 10 ? `0${n}` : `${n}`;
        const lastDay = (y, m) => new Date(y, m, 0).getDate();
        const startDate = `${this.state.startYear}-${pad(this.state.startMonth)}-01`;
        const endDate = `${this.state.endYear}-${pad(this.state.endMonth)}-${lastDay(this.state.endYear, this.state.endMonth)}`;
        return { startDate, endDate };
    }

    onFetchClicked() {

        this.setDates();
        // Run fetch logic in setState callback so we read the latest UI state (startYear/startMonth etc.)
        // after any pending updates from dropdown changes have been applied.
        this.setState({}, () => this.doFetch());
    }

    doFetch() {
        const { startDate: startDateString, endDate: endDateString } = this.computeDatesFromUI();
        const airport = this.state.selectedAirport;

        // Invalid airport, exit
        if (!airport) {

            showErrorModal("Invalid Airport", "Please select a valid airport before fetching data.");
            return;

        }

        const submissionData = {
            startDate: startDateString,
            endDate: endDateString,
            airport: airport,
        };

        const startDate = this.parseDate(startDateString);
        const endDate = this.parseDate(endDateString);

        /*
            Start and end dates for the data we already have.

            This won't encounter an error even if either of dataStartDate or dataEndDate is null.
            If they're null then they won't get used because this.state.data is also null.
            
            This feels like bad practice though.
        */
        const dataStartDate = this.parseDate(this.state.dataStartDate);
        const dataEndDate = this.parseDate(this.state.dataEndDate);

        /*
            Process a batch of TTFs: normalize, plot each, merge into accumulated state.
            approachCounts is mutated across batches for consistent approach numbering.
        */
        const processBatch = (batchTtfs, responseAirports, flightLookup, approachCounts, accumulatedTtfs, accumulatedAirports) => {
            for (let i = 0; i < batchTtfs.length; i++) {
                const ttf = batchTtfs[i];
                const rawFlightId = this.extractFlightId(ttf);
                const resolvedFlightId = rawFlightId ?? this.matchFlightIdFromLookup(ttf, flightLookup, submissionData.airport);
                if (ttf.flightId === undefined && ttf.flight_id !== undefined) ttf.flightId = ttf.flight_id;
                if (resolvedFlightId != null) { ttf.flightId = resolvedFlightId; ttf.flight_id = resolvedFlightId; }
                if (ttf.lat === undefined && Array.isArray(ttf.latitude)) ttf.lat = ttf.latitude;
                if (ttf.lon === undefined && Array.isArray(ttf.longitude)) ttf.lon = ttf.longitude;
                const approachKey = resolvedFlightId ?? '__unknown_flight__';
                if (!(approachKey in approachCounts)) approachCounts[approachKey] = 0;
                ttf.approachn = ++approachCounts[approachKey];
                ttf.popupFlightId = resolvedFlightId;
                ttf.popupFlightName = `Approach ${ttf.approachn}`;
                ttf.popupTraceName = (resolvedFlightId != null && resolvedFlightId !== '')
                    ? `Flight ${resolvedFlightId} - Approach ${ttf.approachn}` : `Approach ${ttf.approachn}`;
                accumulatedTtfs.push(ttf);
                this.plotTTF(ttf);
            }
            Object.assign(accumulatedAirports, responseAirports || {});
        };

        // Batch sizes: 100, 500, 500, then 1000 each (faster first paint)
        const BATCH_SIZES = [100, 500, 500];
        const BATCH_SIZE_REST = 1000;

        const getBatchLimit = (offset) => {
            if (offset === 0) return BATCH_SIZES[0];
            if (offset === 100) return BATCH_SIZES[1];
            if (offset === 600) return BATCH_SIZES[2];
            return BATCH_SIZE_REST;
        };

        const LARGE_RANGE_THRESHOLD = 10000;
        const buildLoadingMessage = (ttfsLoaded, flightsProcessed, totalFlights) => {
            const ttfsStr = ttfsLoaded.toLocaleString();
            const flightsStr = flightsProcessed.toLocaleString();
            const totalStr = totalFlights != null ? totalFlights.toLocaleString() : '?';
            let msg = `${ttfsStr} approaches loaded (${flightsStr} of ${totalStr} flights)`;
            if (totalFlights != null && totalFlights > LARGE_RANGE_THRESHOLD) {
                msg += ' · Consider narrowing your date range for faster loading.';
            }
            return msg;
        };

        const buildCompleteMessage = (ttfsLoaded, flightsProcessed) => {
            const ttfsStr = ttfsLoaded.toLocaleString();
            const flightsStr = flightsProcessed.toLocaleString();
            return `Load complete · ${ttfsStr} approaches from ${flightsStr} flights`;
        };

        const COMPLETE_MESSAGE_DURATION_MS = 10000;

        const fetchBatch = (offset, accumulatedTtfs, accumulatedAirports, approachCounts, flightLookup, totalFlights, totalFlightsRaw, skipCount) => {
            if (currentFetchId !== this.fetchId) return;
            const batchLimit = getBatchLimit(offset);
            const effectiveTotal = totalFlights ?? null;
            $('#loading-progress-text').text(buildLoadingMessage(accumulatedTtfs.length, offset, effectiveTotal));
            $('#loading-progress-banner').show();

            this.pendingFetchRequest = $.ajax({
                type: 'GET',
                url: '/api/flight/turn-to-final',
                data: { ...submissionData, limit: batchLimit, offset, ...(skipCount ? { skipCount: 'true' } : {}) },
                async: true,
                success: (response) => {
                    this.pendingFetchRequest = null;
                    if (currentFetchId !== this.fetchId) return;
                    const batchTtfs = response?.ttfs ?? [];
                    const batchFlightCount = response?.batchFlightCount ?? batchTtfs.length;
                    const effectiveTotal = totalFlights ?? response?.totalFlights ?? null;
                    const flightsProcessed = offset + batchFlightCount;

                    processBatch(batchTtfs, response?.airports, flightLookup, approachCounts, accumulatedTtfs, accumulatedAirports);
                    this.plotCharts(accumulatedTtfs, offset === 0);
                    this.updateMapViewForTTFs(accumulatedTtfs, accumulatedAirports);
                    if (map && typeof map.updateSize === 'function') map.updateSize();

                    $('#loading-progress-text').text(buildLoadingMessage(accumulatedTtfs.length, flightsProcessed, effectiveTotal));

                    const nextOffset = offset + batchLimit;
                    const rawTotal = response?.totalFlightsRaw;
                    const ttfCapReached = response?.ttfCapReached === true;
                    const gotFullBatch = batchFlightCount >= batchLimit;
                    const hasMore = effectiveTotal == null ? gotFullBatch : nextOffset < effectiveTotal;
                    if (!ttfCapReached && gotFullBatch && hasMore) {
                        fetchBatch(nextOffset, accumulatedTtfs, accumulatedAirports, approachCounts, flightLookup, effectiveTotal ?? response?.totalFlights, rawTotal, false);
                    } else {
                        $('#loading-progress-text').text(buildCompleteMessage(accumulatedTtfs.length, flightsProcessed));
                        $('#loading-progress-banner').removeClass('alert-info').addClass('alert-success');
                        setTimeout(() => {
                            $('#loading-progress-banner').hide().removeClass('alert-success').addClass('alert-info');
                        }, COMPLETE_MESSAGE_DURATION_MS);
                        this.setState({
                            disableFetching: false,
                            dataAirport: submissionData.airport,
                            dataStartDate: submissionData.startDate,
                            dataEndDate: submissionData.endDate,
                            data: { ttfs: accumulatedTtfs, airports: accumulatedAirports }
                        });
                    }
                },
                error: (jqXHR, textStatus) => {
                    this.pendingFetchRequest = null;
                    if (textStatus === 'abort') return;
                    this.stopLoadingEstimateCountdown();
                    $('#loading').hide();
                    $('#loading-progress-banner').hide();
                    console.error("Error fetching TTF data: ", jqXHR.responseText);
                    this.setState({ disableFetching: false, datesChanged: false });
                }
            });
        };

        this.setState({ datesChanged: false });

        if (this.pendingFetchRequest) {
            this.pendingFetchRequest.abort();
            this.pendingFetchRequest = null;
        }
        this.fetchId += 1;
        const currentFetchId = this.fetchId;

        if (
            this.state.data != null
            && this.dateWithinRange(startDate, dataStartDate, dataEndDate)
            && this.dateWithinRange(endDate, dataStartDate, dataEndDate)
            && airport == this.state.dataAirport) {

            for (const ttf of this.state.data.ttfs) {
                ttf.enabled = true;
            }
            this.updateDisplay();

        } else {

            if (this.state.data != null) {
                for (const ttf of this.state.data.ttfs) {
                    map.removeLayer(ttf.layer);
                }
            }
            this.setState({ disableFetching: true });
            const firstBatchLimit = getBatchLimit(0);
            $('#loading-message').text('Loading flights...');
            $('#loading').show();
            this.startLoadingEstimateCountdown(15);
            $('#loading-progress-banner').hide();

            const accumulatedTtfs = [];
            const accumulatedAirports = {};
            const approachCounts = {};

            this.pendingFetchRequest = $.ajax({
                type: 'GET',
                url: '/api/flight/turn-to-final',
                data: { ...submissionData, limit: firstBatchLimit, offset: 0 },
                async: true,
                success: (response) => {
                    this.pendingFetchRequest = null;
                    if (currentFetchId !== this.fetchId) return;
                    const missingFlightIds = (response?.ttfs ?? []).some((ttf) => this.extractFlightId(ttf) == null);
                    const lookupPromise = missingFlightIds
                        ? this.fetchFlightLookupForMissingIds(submissionData.startDate, submissionData.endDate, submissionData.airport)
                        : Promise.resolve(null);

                    lookupPromise
                        .catch((error) => {
                            console.warn('Unable to recover missing TTF flight IDs from /api/flight.', error);
                            return null;
                        })
                        .then((flightLookup) => {
                            if (currentFetchId !== this.fetchId) return;
                            processBatch(
                                response?.ttfs ?? [],
                                response?.airports,
                                flightLookup,
                                approachCounts,
                                accumulatedTtfs,
                                accumulatedAirports
                            );
                            this.plotCharts(accumulatedTtfs, true);
                            this.updateMapViewForTTFs(accumulatedTtfs, accumulatedAirports);
                            if (map && typeof map.updateSize === 'function') map.updateSize();

                            const totalFlights = response?.totalFlights ?? null;
                            const batchFlightCount = response?.batchFlightCount ?? accumulatedTtfs.length;
                            const flightsProcessed = batchFlightCount;
                            $('#loading-progress-text').text(buildLoadingMessage(accumulatedTtfs.length, flightsProcessed, totalFlights));

                            const totalFlightsRaw = response?.totalFlightsRaw;
                            const ttfCapReached = response?.ttfCapReached === true;
                            const nextOffset = firstBatchLimit;
                            const gotFullBatch = batchFlightCount >= firstBatchLimit;
                            const hasMore = totalFlights == null ? gotFullBatch : nextOffset < totalFlights;
                            if (!ttfCapReached && gotFullBatch && hasMore) {
                                this.stopLoadingEstimateCountdown();
                                $('#loading').hide();
                                $('#loading-progress-banner').show();
                                fetchBatch(nextOffset, accumulatedTtfs, accumulatedAirports, approachCounts, flightLookup, totalFlights, totalFlightsRaw, false);
                            } else {
                                this.stopLoadingEstimateCountdown();
                                $('#loading').hide();
                                $('#loading-progress-text').text(buildCompleteMessage(accumulatedTtfs.length, flightsProcessed));
                                $('#loading-progress-banner').show().removeClass('alert-info').addClass('alert-success');
                                setTimeout(() => {
                                    $('#loading-progress-banner').hide().removeClass('alert-success').addClass('alert-info');
                                }, COMPLETE_MESSAGE_DURATION_MS);
                                this.setState({
                                    disableFetching: false,
                                    dataAirport: submissionData.airport,
                                    dataStartDate: submissionData.startDate,
                                    dataEndDate: submissionData.endDate,
                                    data: { ttfs: accumulatedTtfs, airports: accumulatedAirports }
                                });
                            }
                        });
                },
                error: (jqXHR, textStatus) => {
                    this.pendingFetchRequest = null;
                    if (textStatus === 'abort') return;
                    this.stopLoadingEstimateCountdown();
                    $('#loading').hide();
                    $('#loading-progress-banner').hide();
                    console.error("Error fetching TTF data: ", jqXHR.responseText);
                    this.setState({ disableFetching: false, datesChanged: false });
                }
            });
        }
    }

    getAirports() {
        if (this.state.data == null)
            return null;

        return this.state.data.airports.map(ap => ap.iataCode);

    }

    onAirportFilterChanged(airport) {
        const iataCode = airport;
        this.setState({ selectedAirport: iataCode, selectedRunway: "Any Runway", datesChanged: true, });
    }

    onUpdateStartYear(year) {

        this.setState({ startYear: year }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    onUpdateStartMonth(month) {

        this.setState({ startMonth: month }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    onUpdateEndYear(year) {

        this.setState({ endYear: year }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    onUpdateEndMonth(month) {

        this.setState({ endMonth: month }, () => {
            this.setDates();
            this.forceUpdate();
        });

    }

    setDates() {
        function getDaysInMonth(year, month) {
            return new Date(year, month, 0).getDate();
        }

        let endDate;
        let startDate;

        this.setState({ datesChanged: true });

        if (this.state.startMonth < 10)
            startDate = `${this.state.startYear}-0${this.state.startMonth}-01`;
        else
            startDate = `${this.state.startYear}-${this.state.startMonth}-01`;

        const startDateObject = this.parseDate(startDate);
        const finalDayOfMonth = getDaysInMonth(this.state.endYear, this.state.endMonth);

        if (this.state.endMonth < 10)
            endDate = `${this.state.endYear}-0${this.state.endMonth}-${finalDayOfMonth}`;
        else
            endDate = `${this.state.endYear}-${this.state.endMonth}-${finalDayOfMonth}`;

        const endDateObject = this.parseDate(endDate);

        this.setState({
            datesChanged: true,
            endDate: endDate,
            startDate: startDate,
            endDateObject: endDateObject,
            startDateObject: startDateObject
        });

    }

    onRunwayFilterChanged(runway) {

        if (runway == null)
            throw "getRunwayValue returned null even though the onRunwayFilterChanged event handler was called.";

        this.setState({ selectedRunway: runway }, () => {
            this.forceUpdate();
        });

        if (this.state.data != null)
            for (const ttf of this.state.data.ttfs) {
                if (this.shouldDisplay(ttf))
                    this.plotTTF(ttf);
                else
                    this.hideTTF(ttf);
            }

    }

    onRollSliderChanged(value, override = false) {

        const slider = document.getElementById('rollSlider');
        if (!override) {

            this.setState({ minRoll: slider.value });
            if (this.state.data != null)
                this.updateDisplay();

        } else {

            this.setState({ minRoll: value });

        }

    }

    displayPlots() {

        const docStyles = getComputedStyle(document.documentElement);
        const plotBgColor = docStyles.getPropertyValue("--c_plotly_bg").trim() || "transparent";
        const plotTextColor = docStyles.getPropertyValue("--c_plotly_text").trim() || "#e6e6e6";
        const plotGridColor = docStyles.getPropertyValue("--c_plotly_grid").trim() || "rgba(255,255,255,0.15)";
        const titleFont = { color: plotTextColor, size: 16 };
        const legendFont = { color: plotTextColor, size: 11 };

        this.glideAngleHistLayout = {
            title: { text: 'Histogram of Glide Path Angles', font: titleFont },
            bargap: 0.05,
            showlegend: false,
            autosize: true,
            margin: {
                t: 40,
                l: 60,
                r: 20,
                b: 40,
                pad: 10
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            legend: {
                font: legendFont
            },
            xaxis: {
                title: 'Frequency',
                gridcolor: plotGridColor
            },
            yaxis: {
                title: 'Glide Path Angle',
                gridcolor: plotGridColor
            }
        };
        this.deviationsPlotlyLayout = {
            title: { text: 'Glide Path Deviations', font: titleFont },
            showlegend: false,
            autosize: true,
            margin: {
                t: 40,
                l: 60,
                r: 20,
                b: 40,
                pad: 2
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            legend: {
                font: legendFont
            },
            xaxis: {
                title: 'Distance from Runway (ft.)',
                gridcolor: plotGridColor,
                autorange: "reversed"
            },
            yaxis: {
                title: 'Distance above Glidepath (ft.)',
                gridcolor: plotGridColor,
                range: [-100, 100]
            }
        };
        this.altitudePlotlyLayout = {
            title: { text: 'Altitude vs. Distance to Runway', font: titleFont },
            showlegend: false,
            autosize: true,
            margin: {
                t: 40,
                l: 60,
                r: 20,
                b: 40,
                pad: 2
            },
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            legend: {
                font: legendFont
            },
            xaxis: {
                title: 'Distance from Runway (ft.)',
                gridcolor: plotGridColor,
                autorange: "reversed"
            },
            yaxis: {
                title: 'Alitude (AGL) (ft.)',
                gridcolor: plotGridColor,
            }
        };
        const plotlyConfig = { responsive: true };


        Plotly.newPlot('deviations-plot', [], this.deviationsPlotlyLayout, plotlyConfig);
        Plotly.newPlot('alt-plot', [], this.altitudePlotlyLayout, plotlyConfig);
        Plotly.newPlot('glide-angle-hist', [], this.glideAngleHistLayout, plotlyConfig);

    }

    render() {

        const runwayList = (this.state.selectedAirport)
            ? runways[this.state.selectedAirport].map(runway => runway.name)
            : [];

        const rollSlider =
            <RollSlider
                rollSliderMin={0}
                rollSliderMax={ROLL_THRESHOLDS.MaxHard}
                rollSliderChanged={(val) => this.onRollSliderChanged(val)}
                rollSliderValue={() => this.state.minRoll}
            />;
        const turnToFinalHeaderComponents =
            <TurnToFinalHeaderComponents
                airframes={[]}
                airport={this.state.selectedAirport}
                airports={airports}
                airportChange={(airport) => this.onAirportFilterChanged(airport)}
                runway={this.state.selectedRunway}
                runways={runwayList}
                runwayChange={(runway) => this.onRunwayFilterChanged(runway)} />;

        const form = (
            <div>
                <TimeHeader
                    name="Flight Filters"
                    disabled={this.state.disableFetching}
                    buttonContent={'Fetch'}
                    extraHeaderComponents={turnToFinalHeaderComponents}
                    extraRowComponents={rollSlider}
                    startYear={this.state.startYear}
                    startMonth={this.state.startMonth}
                    endYear={this.state.endYear}
                    endMonth={this.state.endMonth}
                    datesChanged={this.state.datesChanged}
                    dateChange={() => this.onFetchClicked()}
                    updateStartYear={(newStartYear) => this.onUpdateStartYear(newStartYear)}
                    updateStartMonth={(newStartMonth) => this.onUpdateStartMonth(newStartMonth)}
                    updateEndYear={(newEndYear) => this.onUpdateEndYear(newEndYear)}
                    updateEndMonth={(newEndMonth) => this.onUpdateEndMonth(newEndMonth)}
                />
            </div>
        );

        return form;
    }
}

const ttfContainer = document.querySelector('#ttf-card');
const root = createRoot(ttfContainer);
root.render(<TTFCard />);

export { TTFCard };
