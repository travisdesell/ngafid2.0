import 'bootstrap';
import React from "react";
import { createRoot } from "react-dom/client";
import {showErrorModal} from "./error_modal.js";
import {MapPopup} from "./map_popup.js";
import {LabelingMapPopup, LabelingHoverTooltip} from "./labeling_map_popup.js";

import {Colors, map} from "./map.js";

import {fromLonLat} from 'ol/proj.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Vector as VectorLayer} from 'ol/layer.js';
import {Circle, Fill, Stroke, Style} from 'ol/style.js';
import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';

import {Itinerary} from './itinerary_component.js';
import {TraceButtons} from './trace_buttons_component.js';
import {Tags} from './tags_component.js';
import {Events, eventDefinitions} from './events_component.js';
import {showSelectAircraftModal} from './select_acft_modal.js';
import {generateLOCILayer, generateStallLayer} from './map_utils.js';

import Plotly from 'plotly.js';
import {cesiumFlightsSelected} from "./cesium_buttons";

import { plotlyLayoutGlobal } from './flights.js';

import moment from 'moment';

// --- Labeling tool (changes in this file) ---
// Adds: state (labeling*), map click handling for two-click sections, map path-only mode (forLabeling),
// pointermove hover tooltip, parameter list + range UI row, fa-tag button. See labeling_map_popup.js for popup/side panel.
// Labeling tool: colors for map-drawn sections (two-click selection)
const LABELING_SECTION_COLORS = [
    '#e6194b', '#3cb44b', '#4363d8', '#f58231', '#911eb4', '#42d4f4', '#f032e6', '#bfef45',
    '#fabed4', '#469990', '#dcbeff', '#9a6324', '#fffac8', '#800000', '#aaffc3', '#808000', '#ffd8b1', '#000075',
];

class Flight extends React.Component {

    constructor(props) {

        super(props);

        const color = Colors.randomValue();

        this.state = {

            filterAddButtonHovered: false,
            pathVisible: false,
            pageIndex: props.pageIndex,
            mapLoaded: false,
            eventsLoaded: false,
            tagsLoaded: false,
            commonTraceNames: null,
            uncommonTraceNames: null,
            traceIndex: [],
            traceVisibility: [],
            traceNamesVisible: false,
            eventsVisible: false,
            tagsVisible: false,
            itineraryVisible: false,
            points: [],
            parent: props.parent,
            selectedPlot: null,
            color: color,
            mapPopups: [],
            seriesData: new Map(),

            eventsMapped: [],                              // Bool list to toggle event icons on map flightpath
            eventPoints: [],                               // list of event Features
            eventLayer: null,
            itineraryLayer: null,
            eventOutlines: [],
            eventOutlineLayer: null,
            replayToggled: cesiumFlightsSelected.includes(this.props.flightInfo.id),
            cesiumFlightEnabled: false,

            mapButtonDisabled: false,

            // Labeling tool: parameter list, map path, two-click sections, popup/side panel, highlight layers
            labelingParametersVisible: false,
            labelingParameterNames: [],
            /** Multi-select: array of selected parameter names */
            labelingSelectedParameters: [],
            labelingPathVisible: false,
            /** Series data per parameter: { [paramName]: { x, y } } */
            labelingSeriesDataByParameter: {},
            labelingPopup: null,
            labelingPopupPinned: false,
            labelingHoverTooltip: null,
            /** Sections from two-click. Each: { id?, startIndex, endIndex, startTime, endTime, startValue, endValue, label?, parameterNames: string[] } */
            labelingClickSections: [],
            /** Pending first click: { index, time, value } or null */
            labelingSectionStart: null,
            labelingMarkerLayer: null,
            labelingClickSectionsLayer: null,
            labelingSections: [],
            labelingSectionRange: { low: '', high: '' },
            labelingHighlightLayer: null,
            labelingViewMode: 'chart',
            /** Trace index on plot per parameter: { [paramName]: number } */
            labelingTraceIndices: {},
            /** Per-parameter value sections only: { [paramName]: { valueSections: [{ min, max }] } }. DB-driven later. */
            labelingDataByParameter: {},
            /** Fleet label definitions for dropdown: [{ id, labelText, displayOrder }]. Fetched when opening labeling tool. */
            labelDefinitions: [],
        };

        this.submitXPlanePath = this.submitXPlanePath.bind(this);
        this.displayParameters = this.displayParameters.bind(this);
        this.closeParamDisplay = this.closeParamDisplay.bind(this);
        this.zoomChanged = this.zoomChanged.bind(this);
        this.labelingHover = this.labelingHover.bind(this);
        this.removeLabelingHighlightLayer = this.removeLabelingHighlightLayer.bind(this);
        this.removeLabelingClickSectionsLayer = this.removeLabelingClickSectionsLayer.bind(this);
        this.updateLabelingClickSectionsLayer = this.updateLabelingClickSectionsLayer.bind(this);
        this.addLabelingSection = this.addLabelingSection.bind(this);
        this.removeLabelingSection = this.removeLabelingSection.bind(this);
        this.updateLabelingHighlightLayer = this.updateLabelingHighlightLayer.bind(this);
        this._labelingHoverThrottle = null;
        this.labelingPlotClick = this.labelingPlotClick.bind(this);
        this.updateLabelingChartShapes = this.updateLabelingChartShapes.bind(this);
        this.fitMapToFlightWhenVisible = this.fitMapToFlightWhenVisible.bind(this);
    }

    async fetchEvents() {

        return await new Promise((resolve, reject) => {

            const submissionData = {
                eventDefinitionsLoaded: eventDefinitions.loaded
            };

            $.ajax({
                type: 'GET',
                url: `/api/flight/${this.props.flightInfo.id}/events`,
                data: submissionData,
                dataType: 'json',
                async: true,
                success: (response) => {

                    console.log("Received response events data:", response);

                    if (!eventDefinitions.loaded) {
                        eventDefinitions.content = response.definitions;
                        eventDefinitions.loaded = true;
                    }

                    const events = response.events;
                    for (let i = 0; i < events.length; i++) {
                        for (let j = 0; j < eventDefinitions.content.length; j++) {

                            if (events[i].eventDefinitionId == eventDefinitions.content[j].id)
                                events[i].eventDefinition = eventDefinitions.content[j];

                        }
                    }

                    this.setState({ events }, () => resolve(this.state.events));

                },
                error: (jqXHR, textStatus, errorThrown) => {
                    this.setState({ mapLoaded: false });
                    showErrorModal("Error Loading Flight Events", errorThrown);
                    reject(errorThrown);
                },
            });

        });

    }

    getActiveLayers() {

        const activeLayers = [];
        if (this.state.layers != null) {

            for (let i = 0; i < this.state.layers.length; i++) {

                const layer = this.state.layers[i];
                if (layer.getVisible())
                    activeLayers.push(layer);
                
            }

        }

        return activeLayers;

    }

    componentWillUnmount() {

        console.log("Unmounting:", this.props.flightInfo);

        if (this.props.flightInfo.has_coords === "0")
            return;

        console.log("Hiding flight path");
        this.setState({
            pathVisible: false,
            itineraryVisible: false
        });
        
        const activeLayers = this.getActiveLayers();
        if (activeLayers) {
            for (const layer of activeLayers) {
                layer.setVisible(false);
            }
        }

        //Hiding events...
        if (this.state.eventLayer) {

            //...Map
            this.state.eventLayer.setVisible(false);
            this.state.eventOutlineLayer.setVisible(false);

            //...Plot
            const shapes = plotlyLayoutGlobal.shapes;
            shapes.length = 0;
        }

        //Hiding phases
        if (this.state.itineraryLayer)
            this.state.itineraryLayer.setVisible(false);


        console.log("Hiding plots");
        if (this.state.commonTraceNames) {

            const visible = false;

            for (let i = 0; i < this.state.commonTraceNames.length; i++) {
                const seriesName = this.state.commonTraceNames[i];

                if (seriesName in this.state.traceIndex) {

                    /*
                        This will make make a trace visible if it was
                        formerly set to visible and the plot button
                        this flight is clicked on otherwise it will hide them
                    */
                    Plotly.restyle('plot', {visible: (visible && this.state.traceVisibility[seriesName])}, [this.state.traceIndex[seriesName]]);
                }
            }

            for (let i = 0; i < this.state.uncommonTraceNames.length; i++) {
                const seriesName = this.state.uncommonTraceNames[i];

                if (seriesName in this.state.traceIndex) {

                    /*
                        This will make make a trace visible if it was
                        formerly set to visible and the plot button
                        this flight is clicked on otherwise it will hide them
                    */
                    Plotly.restyle('plot', {visible: (visible && this.state.traceVisibility[seriesName])}, [this.state.traceIndex[seriesName]]);
                }
            }
        }
        this.setState({ traceNamesVisible: false });
    }

    plotClicked() {

        //if (this.state.commonTraceNames == null) {
        if (!this.state.traceNamesVisible) {

            this.props.showPlot();

            $.ajax({
                type: 'GET',
                url: `/api/flight/${this.props.flightInfo.id}/double-series`,
                dataType: 'json',
                async: true,
                success: (response) => {

                    console.log("Received response double series name: ", response);

                    /*
                     * Do these common trace parameters first:
                     * Altitiude AGL
                     * Altitude MSL
                     * E1 MAP
                     * E2 MAP
                     * E1 RPM
                     * E2 RPM
                     * IAS
                     * Normal Acceleration
                     * Pitch
                     * Roll
                     * Vertical Speed
                     * LOC-I Index
                     * Stall Index
                     */
                    const preferredNames = ["AltAGL", "AltMSL", "E1 MAP", "E2 MAP", "E1 RPM", "E2 RPM", "IAS", "NormAc", "Pitch", "Roll", "VSpd", "LOC-I Index", "Stall Index"];
                    const commonTraceNames = [];
                    const uncommonTraceNames = [];

                    for (let i = 0; i < response.names.length; i++) {
                        const name = response.names[i];
                        if (preferredNames.includes(name)) {
                            commonTraceNames.push(name);
                        } else {
                            uncommonTraceNames.push(name);
                        }
                    }

                    //set the trace number for this series
                    this.setState({
                        commonTraceNames: commonTraceNames,
                        uncommonTraceNames: uncommonTraceNames,
                        traceNamesVisible: true
                    });
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    this.setState({
                        commonTraceNames: null,
                        uncommonTraceNames: null
                    });
                    showErrorModal("Error Getting Potential Plot Parameters", errorThrown);
                },
            });
        } else {

            const visible = !this.state.traceNamesVisible;

            for (let i = 0; i < this.state.commonTraceNames.length; i++) {

                const seriesName = this.state.commonTraceNames[i];

                //Check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    /*
                        This will make make a trace visible if it was
                        formerly set to visible and the plot button
                        this flight is clicked on otherwise it will hide them
                    */
                    Plotly.restyle('plot', {visible: (visible && this.state.traceVisibility[seriesName])}, [this.state.traceIndex[seriesName]]);

                }

            }

            for (let i = 0; i < this.state.uncommonTraceNames.length; i++) {

                const seriesName = this.state.uncommonTraceNames[i];

                //Check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    /*
                        This will make make a trace visible if it was
                        formerly set to visible and the plot button
                        this flight is clicked on otherwise it will hide them
                    */
                    Plotly.restyle('plot', {visible: (visible && this.state.traceVisibility[seriesName])}, [this.state.traceIndex[seriesName]]);

                }

            }

            this.setState({ traceNamesVisible: !this.state.traceNamesVisible });

        }

    }

    flightColorChange(target, event) {

        console.log("Trace color changed! ", event, event.target, event.target.value);

        target.state.baseLayer.setStyle(new Style({
            stroke: new Stroke({
                color: event.target.value,
                width: 3
            })
        }));

        for (let i = 0; i < target.state.layers.length; i++) {
            const layer = target.state.layers[i];
            if (layer.get('nMap')) {
                layer.setStyle(new Style({
                        stroke: new Stroke({
                            color: event.target.value,
                            width: 12,
                        })
                    })
                );
            }
        }


        target.setState({color: event.target.value});
    }

    async exclamationClicked() {

        console.log("Exclamation clicked!");

        if (!this.state.eventsLoaded) {

            console.log("Loading events!");

            const events = await this.fetchEvents();
            console.log("Got Flight Events: ", events);


            //Failed to load events, exit
            if (!events) {
                console.error("Events are undefined, exiting!");
                return;
            }

            // this.setState({ eventsVisible: true });

            // create list of event Features to display on map //
            for (let i = 0; i < events.length; i++) {

                let points;
                let eventPoint;
                let eventOutline;
                const event = events[i];

                // Create Feature for event
                if (!this.state.mapLoaded) {              // if points (coordinates) have not been fetched
                    // create eventPoint with placeholder coordinates
                    eventPoint = new Feature({
                        geometry: new LineString([0, 0]),
                        name: 'Event'
                    });

                    // create outlines
                    eventOutline = new Feature({
                        geometry: new LineString([0, 0]),
                        name: 'EventOutline'
                    });

                } else {
                    // create eventPoint with preloaded coordinates
                    points = this.state.points;
                    eventPoint = new Feature({
                        geometry: new LineString(points.slice(event.startLine, event.endLine + 2)),
                        name: 'Event'
                    });

                    // create outlines
                    eventOutline = new Feature({
                        geometry: new LineString(points.slice(event.startLine, event.endLine + 2)),
                        name: 'EventOutline'
                    });
                }

                // add eventPoint to flight
                this.state.eventsMapped.push(false);
                this.state.eventPoints.push(eventPoint);
                this.state.eventOutlines.push(eventOutline);
            }

            // create eventLayer & add eventPoints
            const eventLayer = new VectorLayer({
                style: new Style({
                    stroke: new Stroke({
                        color: [0, 0, 0, 0],
                        width: 3
                    })
                }),

                source: new VectorSource({
                    features: this.state.eventPoints
                })
            });

            // create eventOutlineLayer & add eventOutlines
            const eventOutlineLayer = new VectorLayer({
                style: new Style({
                    stroke: new Stroke({
                        color: [0, 0, 0, 0],
                        width: 4
                    })
                }),

                source: new VectorSource({
                    features: this.state.eventOutlines
                })
            });

            eventOutlineLayer.setVisible(true);
            eventLayer.setVisible(true);

            // add to map only if flightPath loaded
            if (this.state.mapLoaded) {
                map.addLayer(eventOutlineLayer);
                map.addLayer(eventLayer);
            }

            this.setState({
                ...this.state,
                eventsLoaded: true,
                eventsVisible: true,
                eventLayer: eventLayer,
                eventOutlineLayer: eventOutlineLayer
            });

        } else {
            console.log("events already loaded!");

            //toggle visibility if already loaded
            const newEventsVisible = !this.state.eventsVisible;
            this.state.eventLayer.setVisible(newEventsVisible);
            this.state.eventOutlineLayer.setVisible(newEventsVisible);

            if (!newEventsVisible) {
                console.log("Clearing plotly");
                plotlyLayoutGlobal.shapes = [];
                Plotly.relayout('plot', plotlyLayoutGlobal);
            }

            console.log(plotlyLayoutGlobal);
            this.setState({ ...this.state, eventsVisible: newEventsVisible });
        }
    }

    /**
     * Handles when the download button was clicked
     * @param type which type of download (xplane, csv etc)
     */
    downloadClicked(type) {
        if (type === 'KML') {
            window.open(`/api/flight/${this.props.flightInfo.id}/kml`);
        } else if (type === 'XPL10') {
            showSelectAircraftModal('10', this.submitXPlanePath, this.props.flightInfo.id);
        } else if (type === 'XPL11') {
            showSelectAircraftModal('11', this.submitXPlanePath, this.props.flightInfo.id);
        } else if (type === 'CSV-IMP') {
            window.open(`/api/flight/${this.props.flightInfo.id}/csv`);
        } else if (type === 'CSV-GEN') {
            window.open(`/api/flight/${this.props.flightInfo.id}/csv/generated`);
        }

    }

    /**
     * Gets the aircraft path from the submit aircraft modal
     * @param type the xplane version
     * @param path the selected path
     * @param flightId the flightId
     **/
    submitXPlanePath(type, path, useMSL) {
        window.open(`/api/flight/${this.props.flightInfo.id}/xplane?version=${type}&actf_path=${path}&use_msl=${useMSL}`);
    }

    getCesiumData(flightId) {

        let cesiumData = null;
        const submissionData = {
            "flightId": flightId
        };

        $.ajax({
            type: 'POST',
            url: '/protected/cesium_data',
            traditional: true,
            data: submissionData,
            dataType: 'json',
            success: (response) => {
                console.log(response);
                cesiumData = response;
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log(errorThrown);
            },
            async: false
        });

        return cesiumData;
    }

    cesiumClicked() {

        const flightId = this.props.flightInfo.id;

        this.setState(prevState => ({
            cesiumFlightEnabled: !prevState.cesiumFlightEnabled,
            replayToggled: !prevState.replayToggled
        }), () => {
            this.props.showCesiumPage(flightId, this.state.color);
        });

    }

    addCesiumFlight() {

        console.log("Adding flight to cesium");

        this.setState({ cesiumFlightEnabled: true }, () => {
            this.props.showCesiumPage(this.props.flightInfo.id, this.state.color);
        });

    }

    removeCesiumFlight() {

        console.log("Removing Cesium flights");
        this.setState({ cesiumFlightEnabled: false }, () => {
            this.props.removeCesiumFlight(this.props.flightInfo.id);
        });

    }

    toggleCesiumFlight() {

        //Cesium map is not visible, add the flight
        if (!this.state.cesiumFlightEnabled)
            this.addCesiumFlight();

        //Cesium map is visible, remove the flight
        else
            this.removeCesiumFlight();

        console.log(cesiumFlightsSelected);
    }

    replayClicked() {

        /*
            This functionality is deprecated.
        */

        const URL = `/protected/ngafid_cesium_old?flight_id=${  (this.props.flightInfo.id).toString()}`;
        window.open(URL);
    }

    closeParamDisplay() {
        console.log("popup closed!");
    }

    zoomChanged(oldZoom) {
        const currZoom = map.getView().getZoom();
        console.log(`old zoom: ${  oldZoom}`);
        console.log(`current zoom: ${  currZoom}`);

        for (let i = 0; i < this.state.mapPopups.length; i++) {
            this.state.mapPopups[i].close();
        }
    }

    displayParameters(event) {

        const pixel = event.pixel;
        const features = [];

        map.forEachFeatureAtPixel(pixel, function (feature) {
            features.push(feature);
        });

        let target = features[0];
        if (!target) return; // no feature under cursor (e.g. empty map click); avoids using target below

        // Labeling mode: two-click section — use path coords so click counts even when hitting a section segment or marker
        if (this.state.labelingPathVisible && this.state.points && this.state.points.length > 0) {
            const coord = map.getCoordinateFromPixel(pixel);
            const points = this.state.points;
            let minDist = Infinity;
            let pathIdx = 0;
            for (let i = 0; i < points.length; i++) {
                const dx = points[i][0] - coord[0];
                const dy = points[i][1] - coord[1];
                const d = dx * dx + dy * dy;
                if (d < minDist) { minDist = d; pathIdx = i; }
            }
            const selected = this.state.labelingSelectedParameters || [];
            const byParam = this.state.labelingSeriesDataByParameter || {};
            const firstParam = selected[0];
            const firstSeries = firstParam && byParam[firstParam];
            if (selected.length === 0 || !firstSeries || !firstSeries.x) {
                const pathClickThresholdSq = 40000;
                if (minDist < pathClickThresholdSq) {
                    showErrorModal(
                        'Select a parameter first',
                        'Please choose at least one parameter from the list above before clicking on the flight path to add a section.'
                    );
                }
                return;
            }
            let idx = pathIdx;
            const sectionStart = this.state.labelingSectionStart;
            if (sectionStart != null) {
                const hitStartMarker = features.some(f => f.get && f.get('name') === 'LabelSectionStart');
                const samePointAsStart = (idx === sectionStart.index);
                if (hitStartMarker || samePointAsStart) {
                    idx = sectionStart.index;
                }
            }
            const { x, y } = firstSeries;
            const time = x[idx];
            const value = y[idx];
            const parameterNames = [...selected];
            if (sectionStart == null) {
                this.setState({ labelingSectionStart: { index: idx, time, value } }, () => {
                    this.updateLabelingMarkers();
                    this.renderLabelingPopup();
                });
            } else {
                const startIndex = Math.min(sectionStart.index, idx);
                const endIndex = Math.max(sectionStart.index, idx);
                const startTime = x[startIndex];
                const endTime = x[endIndex];
                const startValue = y[startIndex];
                const endValue = y[endIndex];
                const sectionEntity = { id: null, startIndex, endIndex, startTime, endTime, startValue, endValue, label: '', parameterNames, visibleOnChart: true };
                const nextSections = [...(this.state.labelingClickSections || []), sectionEntity];
                const newIndex = nextSections.length - 1;
                this.setState({ labelingSectionStart: null, labelingClickSections: nextSections }, () => {
                    this.updateLabelingMarkers();
                    this.updateLabelingClickSectionsLayer();
                    this.renderLabelingPopup();
                    this.saveNewLabelSection({
                        id: null, startIndex, endIndex, startTime, endTime,
                        startValue, endValue, labelText: '', parameterNames,
                    }, newIndex);
                });
            }
            return;
        }

        console.log("Populating new popup for metrics...");

        if (target.get('name') === 'Event' && features[2] != null)
            target = features[2];

        console.log(this.state.events);


        const lociInfo = [];

        let info, precision;
        if (target != null && (target.parent === "LOC-I Index" || target.parent === "Stall Index")) {

            const index = target.getId();
            console.log("Target info: ", target, index);

            console.log(this.state.flightMetrics);
            const submissionData = {
                time_index: index
            };

            lociInfo.push(index);

            const spData = this.state.seriesData.get('Stall Index');
            const lociData = this.state.seriesData.get('LOC-I Index');

            lociInfo.push(spData[index]); //All flights should have SI data
            if (lociData == null)
                lociInfo.push(null);
            else
                lociInfo.push(lociData[index]);

            $.ajax({
                type: 'GET',
                url: `/api/flight/${this.props.flightInfo.id}/loci-metrics`,
                data: submissionData,
                async: false,
                success: (response) => {

                    console.log("Got loci_metrics response:", response);
                    info = response.values;
                    precision = response.precision;

                    console.log("Info: ", info);
                    console.log("Precision: ", precision);
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    console.log("Error getting upset data:", errorThrown);
                },
                
            });

        } else {
            console.log("Won't render popup");
        }
    }

    /**
     * Recursively find a vacant (unpinned) popup or create a new one
     */
    renderNewPopup(index, props) {

        // if we reach the bottom of the stack, we must allocate memory for a new popup component
        if (index < 0 || this.state.mapPopups[index] == null) {
            
            const outterHTM = document.createElement('div');
            document.body.appendChild(outterHTM);
            outterHTM.setAttribute("id", `popover${  this.state.mapPopups.length}`);
            const root = createRoot(outterHTM);
            root.render(React.createElement(MapPopup, props));
            this.state.mapPopups.push({ root, element: outterHTM });
            return root;
            
        } else if (this.state.mapPopups[index].isPinned && this.state.mapPopups[index].isPinned()) {
            
            return this.renderNewPopup(index - 1, props);

        } else {

            console.log("using existing popup to render!");
            const popupObj = this.state.mapPopups[index];
            popupObj.root.render(React.createElement(MapPopup, props));
            // If MapPopup exposes a show method, you may need to trigger it via ref or props.
            return popupObj.root;

        }

    }

    tagClicked() {
        this.setState((prevState) => ({
            tagsVisible: !prevState.tagsVisible
        }));
    }

    fetchFleetLabelDefinitions() {
        $.ajax({
            type: 'GET',
            url: '/api/fleet/labels',
            dataType: 'json',
            async: true,
            success: (list) => this.setState({ labelDefinitions: list || [] }),
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("Error fetching fleet label definitions", errorThrown);
                this.setState({ labelDefinitions: [] });
            },
        });
    }

    labelingClicked() {
        const flightId = this.props.flightInfo.id;

        if (this.state.labelingParametersVisible) {
            this.hideLabelingMap();
            this.closeLabelingPopup();
            this.removeLabelingClickSectionsLayer();
            this.removeLabelingTraceFromPlot();
            this.removeLabelingPlotClickListener();
            if (this.props.onLabelingViewMode) this.props.onLabelingViewMode(null);
            const selected = this.state.labelingSelectedParameters || [];
            const currentValue = this.state.labelingSections || [];
            let byParam = this.state.labelingDataByParameter || {};
            if (selected.length === 1) {
                byParam = { ...byParam, [selected[0]]: { valueSections: currentValue.map((s) => ({ min: s.min, max: s.max })) } };
            }
            this.setState({
                labelingParametersVisible: false,
                labelingParameterNames: [],
                labelingSelectedParameters: [],
                labelingSeriesDataByParameter: {},
                labelingPopupPinned: false,
                labelingClickSections: [],
                labelingSectionStart: null,
                labelingSections: [],
                labelingSectionRange: { low: '', high: '' },
                labelingHighlightLayer: null,
                labelingViewMode: 'chart',
                labelingTraceIndices: {},
                labelingDataByParameter: byParam,
            }, () => this.updateLabelingChartShapes());
            return;
        }

        $.ajax({
            type: 'GET',
            url: `/api/flight/${flightId}/double-series`,
            dataType: 'json',
            async: true,
            success: (response) => {
                const names = response.names || [];
                this.setState({
                    labelingParametersVisible: true,
                    labelingParameterNames: names,
                    labelingSelectedParameters: [],
                });
                this.fetchFleetLabelDefinitions();
                // Load any existing label sections for this flight
                $.ajax({
                    type: 'GET',
                    url: `/api/flight/${flightId}/labels`,
                    dataType: 'json',
                    async: true,
                    success: (sections) => this.applyLabelSections(this.mapApiSectionsToClickSections(sections)),
                    error: (jqXHR, textStatus, errorThrown) => console.error("Error fetching flight labels", errorThrown),
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("Error fetching double-series names for flight", flightId, errorThrown);
            },
        });
    }

    labelingParameterClicked(name) {
        const selected = this.state.labelingSelectedParameters || [];
        const byParamData = this.state.labelingSeriesDataByParameter || {};
        const isCurrentlySelected = selected.includes(name);

        if (isCurrentlySelected) {
            const nextSelected = selected.filter((p) => p !== name);
            this.removeLabelingTraceFromPlot();
            const nextSeries = { ...byParamData };
            delete nextSeries[name];
            if (nextSelected.length === 0) {
                let byParam = this.state.labelingDataByParameter || {};
                if (selected.length === 1) {
                    byParam = { ...byParam, [selected[0]]: { valueSections: (this.state.labelingSections || []).map((s) => ({ min: s.min, max: s.max })) } };
                }
                this.removeLabelingHighlightLayer();
                const clickSections = this.state.labelingClickSections || [];
                const paramsInSections = [...new Set(clickSections.flatMap((s) => s.parameterNames || []))];
                const seriesToKeep = {};
                paramsInSections.forEach((p) => {
                    if (byParamData[p]) seriesToKeep[p] = byParamData[p];
                });
                this.setState({
                    labelingSelectedParameters: [],
                    labelingSeriesDataByParameter: seriesToKeep,
                    labelingTraceIndices: {},
                    labelingSections: [],
                    labelingSectionRange: { low: '', high: '' },
                    labelingSectionStart: null,
                    labelingDataByParameter: byParam,
                }, () => {
                    this.addLabelingTraceToPlot();
                    this.updateLabelingChartShapes();
                    this.updateLabelingClickSectionsLayer();
                    this.renderLabelingPopup();
                });
                return;
            }
            this.setState({
                labelingSelectedParameters: nextSelected,
                labelingSeriesDataByParameter: nextSeries,
                labelingSections: nextSelected.length === 1 ? (this.state.labelingDataByParameter?.[nextSelected[0]]?.valueSections || []) : [],
                labelingSectionRange: { low: '', high: '' },
            }, () => {
                this.addLabelingTraceToPlot();
                this.updateLabelingChartShapes();
                if (nextSelected.length === 1) this.updateLabelingHighlightLayer();
                this.updateLabelingClickSectionsLayer();
                this.renderLabelingPopup();
            });
            return;
        }

        const nextSelected = [...selected, name];
        const flightId = this.props.flightInfo.id;
        $.ajax({
            type: 'GET',
            url: `/api/flight/${flightId}/double-series/${encodeURIComponent(name)}`,
            dataType: 'json',
            async: true,
            success: (response) => {
                const viewMode = this.state.labelingViewMode || 'chart';
                const nextSeries = { ...(this.state.labelingSeriesDataByParameter || {}), [name]: { x: response.x || [], y: response.y || [] } };
                const isFirst = (this.state.labelingSelectedParameters || []).length === 0;
                this.setState({
                    labelingSelectedParameters: nextSelected,
                    labelingSeriesDataByParameter: nextSeries,
                    labelingViewMode: viewMode,
                    labelingSections: nextSelected.length === 1 ? (this.state.labelingDataByParameter?.[name]?.valueSections || []) : [],
                    labelingSectionRange: { low: '', high: '' },
                    labelingSectionStart: null,
                }, () => {
                    if (this.props.onLabelingViewMode) this.props.onLabelingViewMode(viewMode);
                    if (viewMode === 'chart') {
                        this.props.showPlot();
                        // Ensure plot container is visible before adding traces (parent re-renders async)
                        setTimeout(() => {
                            this.addLabelingTraceToPlot();
                            if (typeof Plotly !== 'undefined' && Plotly.Plots) Plotly.Plots.resize('plot');
                        }, 400);
                    } else {
                        this.props.showMap();
                        if (isFirst) this.showMapForLabeling();
                    }
                    this.updateLabelingChartShapes();
                    this.updateLabelingClickSectionsLayer();
                    if (nextSelected.length === 1) this.updateLabelingHighlightLayer();
                    this.renderLabelingPopup();
                    if (viewMode === 'map' && isFirst) this.fitMapToFlightWhenVisible();
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("Error fetching time series for", name, errorThrown);
            },
        });
    }

    /** Map API label sections to our click-section format. */
    mapApiSectionsToClickSections(sections) {
        return (sections || []).map((s) => ({
            id: s.id, startIndex: s.startIndex, endIndex: s.endIndex,
            startTime: s.startTime, endTime: s.endTime,
            startTimeDisplay: s.startTimeDisplay, endTimeDisplay: s.endTimeDisplay,
            startValue: s.startValue, endValue: s.endValue,
            label: s.labelText || '', parameterNames: s.parameterNames || [],
        }));
    }

    /** Apply label sections to state and update layers/popup. */
    applyLabelSections(clickSections) {
        this.setState({ labelingClickSections: clickSections }, () => {
            this.updateLabelingClickSectionsLayer();
            if (this.state.labelingViewMode === 'chart') this.updateLabelingChartShapes();
            this.renderLabelingPopup();
        });
    }

    /** POST a new section, then update state with saved id and display times. */
    saveNewLabelSection(sectionData, newIndex) {
        const flightId = this.props.flightInfo.id;
        $.ajax({
            type: 'POST',
            url: `/api/flight/${flightId}/labels`,
            contentType: 'application/json',
            data: JSON.stringify(sectionData),
            success: (saved) => {
                this.setState((prev) => {
                    const list = prev.labelingClickSections || [];
                    if (newIndex < 0 || newIndex >= list.length) return null;
                    const updated = list.map((s, i) =>
                        i === newIndex ? { ...s, id: saved.id, startTimeDisplay: saved.startTimeDisplay, endTimeDisplay: saved.endTimeDisplay } : s
                    );
                    return { labelingClickSections: updated };
                }, () => this.renderLabelingPopup());
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("Error saving label section", errorThrown);
            },
        });
    }

    closeLabelingPopup() {
        const popup = this.state.labelingPopup;
        if (popup) {
            const { root, element } = popup;
            try { root.render(null); } catch (e) { /* ignore */ }
            if (element && element.parentNode) element.parentNode.removeChild(element);
            this.setState({ labelingPopup: null, labelingPopupPinned: false });
        }
        this.hideLabelingHoverTooltip();
    }

    hideLabelingHoverTooltip() {
        const tip = this.state.labelingHoverTooltip;
        if (tip) {
            try { tip.root.render(null); } catch (e) { /* ignore */ }
            if (tip.element && tip.element.parentNode) tip.element.parentNode.removeChild(tip.element);
            this.setState({ labelingHoverTooltip: null });
        }
    }

    labelingHover(event) {
        const selected = this.state.labelingSelectedParameters || [];
        const byParam = this.state.labelingSeriesDataByParameter || {};
        const firstSeries = selected.length > 0 ? byParam[selected[0]] : null;
        if (!this.state.labelingPathVisible || !firstSeries || selected.length === 0) {
            this.hideLabelingHoverTooltip();
            return;
        }
        if (this.state.labelingPopupPinned) {
            this.hideLabelingHoverTooltip();
            return;
        }
        const now = Date.now();
        if (this._labelingHoverThrottle != null && now - this._labelingHoverThrottle < 50) {
            return;
        }
        this._labelingHoverThrottle = now;
        const pixel = event.pixel;
        const features = [];
        map.forEachFeatureAtPixel(pixel, (feature) => features.push(feature));
        const { x, y } = firstSeries;
        let idx = null;
        let lineFeature = features.find(f => f.get && f.get('name') === 'Line' && f.get('flightId') === this.props.flightInfo.id);
        const sectionFeature = features.find(f => f.get && f.get('name') === 'LabelingSection');
        const clickSectionFeature = features.find(f => f.get && f.get('name') === 'LabelingClickSection');
        const segmentFeature = sectionFeature || clickSectionFeature;
        if (segmentFeature) {
            const geom = segmentFeature.getGeometry();
            if (geom && geom.getType() === 'Point') {
                idx = segmentFeature.get('startIndex') ?? segmentFeature.get('endIndex');
            } else if (geom && geom.getType() === 'LineString') {
                const coord = map.getCoordinateFromPixel(pixel);
                const coords = geom.getCoordinates();
                let minDist = Infinity;
                let localIdx = 0;
                for (let i = 0; i < coords.length; i++) {
                    const dx = coords[i][0] - coord[0];
                    const dy = coords[i][1] - coord[1];
                    const d = dx * dx + dy * dy;
                    if (d < minDist) { minDist = d; localIdx = i; }
                }
                const startIdx = segmentFeature.get('startIndex') ?? 0;
                idx = startIdx + localIdx;
            }
        }
        if (idx == null && lineFeature) {
            const geom = lineFeature.getGeometry();
            if (geom && geom.getType() === 'LineString') {
                const coord = map.getCoordinateFromPixel(pixel);
                const coords = geom.getCoordinates();
                let minDist = Infinity;
                for (let i = 0; i < coords.length; i++) {
                    const dx = coords[i][0] - coord[0];
                    const dy = coords[i][1] - coord[1];
                    const d = dx * dx + dy * dy;
                    if (d < minDist) { minDist = d; idx = i; }
                }
            }
        }
        if (idx == null) {
            this.hideLabelingHoverTooltip();
            return;
        }
        const time = x[idx];
        const value = selected.length === 1 ? y[idx] : undefined;
        const offset = 10;
        const placement = [pixel[0] + offset, pixel[1] + offset];

        const startDateTime = this.props.flightInfo.startDateTime;
        if (this.state.labelingHoverTooltip) {
            this.state.labelingHoverTooltip.root.render(React.createElement(LabelingHoverTooltip, {
                placement,
                time,
                value,
                startDateTime,
            }));
        } else {
            const el = document.createElement('div');
            document.body.appendChild(el);
            const root = createRoot(el);
            root.render(React.createElement(LabelingHoverTooltip, {
                placement,
                time,
                value,
                startDateTime,
            }));
            this.setState({ labelingHoverTooltip: { root, element: el } });
        }
    }

    showMapForLabeling() {
        this.mapClicked(true);
    }

    /**
     * Return sections that include the given parameter (for display or DB export).
     */
    getSectionsForParameter(paramName) {
        const entry = (this.state.labelingDataByParameter || {})[paramName];
        const clickSections = (this.state.labelingClickSections || []).filter((s) => (s.parameterNames || []).includes(paramName));
        return {
            clickSections: [...clickSections],
            valueSections: (entry && entry.valueSections) ? entry.valueSections.map((s) => ({ min: s.min, max: s.max })) : [],
        };
    }

    /**
     * Fit the map view to this flight's path after the map container is visible.
     * Used when switching from Chart to Map in labeling so getSize() is non-zero.
     */
    fitMapToFlightWhenVisible() {
        let attempts = 0;
        const maxAttempts = 20;
        const tryFit = () => {
            const baseLayer = this.state.baseLayer;
            if (!baseLayer || attempts++ >= maxAttempts) return;
            try {
                map.updateSize();
                const size = map.getSize();
                if (size && size[0] > 0 && size[1] > 0) {
                    const extent = baseLayer.getSource().getExtent();
                    map.getView().fit(extent, { size: map.getSize(), padding: [20, 20, 20, 20], maxZoom: 15 });
                    return;
                }
            } catch (e) { /* ignore */ }
            setTimeout(tryFit, 50);
        };
        setTimeout(tryFit, 100);
    }

    /**
     * Add one trace per selected parameter on the chart (parameter buttons only).
     * Always removes any existing labeling traces first to avoid duplicates.
     */
    addLabelingTraceToPlot() {
        const plotEl = document.getElementById('plot');
        const selected = this.state.labelingSelectedParameters || [];
        const byParam = this.state.labelingSeriesDataByParameter || {};
        if (!plotEl) return;

        const prefix = `Labeling: ${this.props.flightInfo.id} – `;
        const toRemove = [];
        const dataArr = plotEl.data || [];
        for (let i = 0; i < dataArr.length; i++) {
            const n = dataArr[i].name;
            if (typeof n === 'string' && n.startsWith(prefix)) toRemove.push(i);
        }
        try {
            toRemove.sort((a, b) => b - a).forEach((idx) => Plotly.deleteTraces('plot', [idx]));
        } catch (e) {
            console.warn('Labeling: could not remove traces', e);
        }

        if (selected.length === 0) {
            this.setState({ labelingTraceIndices: {} });
            return;
        }

        const indices = {};
        selected.forEach((name) => {
            const data = byParam[name];
            if (!data || !data.x) return;
            const trace = {
                x: data.x,
                y: data.y,
                mode: 'lines',
                name: `${prefix}${name}`,
                line: { width: 2 },
            };
            Plotly.addTraces('plot', [trace]);
            indices[name] = (plotEl.data ? plotEl.data.length : 1) - 1;
        });
        this.setState({ labelingTraceIndices: indices }, () => this.attachLabelingPlotClickListener());
    }

    /**
     * Remove all labeling traces from the plot and clear trace indices.
     */
    removeLabelingTraceFromPlot() {
        const indices = this.state.labelingTraceIndices || {};
        const toRemove = Object.values(indices).sort((a, b) => b - a);
        if (toRemove.length === 0) {
            this.setState({ labelingTraceIndices: {} });
            return;
        }
        try {
            const plotEl = document.getElementById('plot');
            if (plotEl && plotEl.data) {
                toRemove.forEach((idx) => {
                    if (idx < plotEl.data.length) Plotly.deleteTraces('plot', [idx]);
                });
            }
        } catch (e) {
            console.warn('Labeling: could not remove traces', e);
        }
        this.setState({ labelingTraceIndices: {} });
    }

    attachLabelingPlotClickListener() {
        const plotEl = document.getElementById('plot');
        if (!plotEl) return;
        if (plotEl.removeAllListeners) plotEl.removeAllListeners('plotly_click');
        plotEl.on('plotly_click', this.labelingPlotClick);
    }

    removeLabelingPlotClickListener() {
        const plotEl = document.getElementById('plot');
        if (plotEl && plotEl.removeAllListeners) plotEl.removeAllListeners('plotly_click');
    }

    /**
     * Handle click on the Plotly chart when in labeling chart view: two-click section selection.
     * With multiple params, click on any trace creates a section tagged with all selected parameters.
     */
    labelingPlotClick(event) {
        const selected = this.state.labelingSelectedParameters || [];
        const byParam = this.state.labelingSeriesDataByParameter || {};
        const traceIndices = this.state.labelingTraceIndices || {};
        if (selected.length === 0 || this.state.labelingViewMode !== 'chart') return;
        if (!event || !event.points || event.points.length === 0) return;
        const pt = event.points[0];
        const curveNumber = pt.curveNumber;
        const paramForTrace = selected.find((p) => traceIndices[p] === curveNumber);
        if (paramForTrace == null) return;
        const data = byParam[paramForTrace];
        if (!data || !data.x) return;
        const { x, y } = data;
        const pointIndex = typeof pt.pointIndex !== 'undefined' ? pt.pointIndex : pt.pointNumber;
        if (pointIndex < 0 || pointIndex >= (x.length || 0)) return;
        const time = x[pointIndex];
        const value = y[pointIndex];
        const parameterNames = [...selected];
        const sectionStart = this.state.labelingSectionStart;
        if (sectionStart == null) {
            this.setState({ labelingSectionStart: { index: pointIndex, time, value } }, () => {
                this.updateLabelingMarkers();
                this.renderLabelingPopup();
                if (this.state.labelingViewMode === 'chart') this.updateLabelingChartShapes();
            });
        } else {
            const startIndex = Math.min(sectionStart.index, pointIndex);
            const endIndex = Math.max(sectionStart.index, pointIndex);
            const startTime = x[startIndex];
            const endTime = x[endIndex];
            const startValue = y[startIndex];
            const endValue = y[endIndex];
            const sectionEntity = { id: null, startIndex, endIndex, startTime, endTime, startValue, endValue, label: '', parameterNames, visibleOnChart: true };
            const nextSections = [...(this.state.labelingClickSections || []), sectionEntity];
            const newIndex = nextSections.length - 1;
            this.setState({ labelingSectionStart: null, labelingClickSections: nextSections }, () => {
                this.updateLabelingMarkers();
                this.updateLabelingClickSectionsLayer();
                this.renderLabelingPopup();
                if (this.state.labelingViewMode === 'chart') this.updateLabelingChartShapes();
                this.saveNewLabelSection({
                    id: null, startIndex, endIndex, startTime, endTime,
                    startValue, endValue, labelText: '', parameterNames,
                }, newIndex);
            });
        }
    }

    hideLabelingMap() {
        if (!this.state.labelingPathVisible) return;
        const { layers, baseLayer, eventLayer, eventOutlineLayer, labelingMarkerLayer } = this.state;
        if (layers && baseLayer) {
            for (const layer of layers) {
                layer.setVisible(false);
            }
            if (eventLayer) eventLayer.setVisible(false);
            if (eventOutlineLayer) eventOutlineLayer.setVisible(false);
        }
        if (labelingMarkerLayer && map.getLayers().getArray().includes(labelingMarkerLayer)) {
            map.removeLayer(labelingMarkerLayer);
        }
        this.removeLabelingHighlightLayer();
        this.removeLabelingClickSectionsLayer();
        this.setState({ labelingPathVisible: false, labelingClickSections: [], labelingSectionStart: null, labelingMarkerLayer: null, labelingSections: [], labelingHighlightLayer: null });
    }

    removeLabelingClickSectionsLayer() {
        const layer = this.state.labelingClickSectionsLayer;
        if (layer && map.getLayers().getArray().includes(layer)) {
            map.removeLayer(layer);
        }
        this.setState({ labelingClickSectionsLayer: null });
    }

    /** Draw red segments on the path for each click-defined section (two-click sections). */
    updateLabelingClickSectionsLayer() {
        const { points, labelingClickSections, labelingClickSectionsLayer } = this.state;
        if (!points || points.length === 0 || !labelingClickSections || labelingClickSections.length === 0) {
            this.removeLabelingClickSectionsLayer();
            return;
        }
        const features = [];
        labelingClickSections.forEach((sec, colorIndex) => {
            if (sec.visibleOnChart === false) return;
            const startIndex = sec.startIndex;
            const endIndex = sec.endIndex;
            const color = LABELING_SECTION_COLORS[colorIndex % LABELING_SECTION_COLORS.length];
            if (startIndex === endIndex) {
                const f = new Feature({ geometry: new Point(points[startIndex]), name: 'LabelingClickSection' });
                f.set('colorIndex', colorIndex % LABELING_SECTION_COLORS.length);
                f.set('startIndex', startIndex);
                f.set('endIndex', endIndex);
                f.set('isPointSection', true);
                features.push(f);
            } else {
                const slice = points.slice(startIndex, endIndex + 1);
                const f = new Feature({ geometry: new LineString(slice), name: 'LabelingClickSection' });
                f.set('colorIndex', colorIndex % LABELING_SECTION_COLORS.length);
                f.set('startIndex', startIndex);
                f.set('endIndex', endIndex);
                features.push(f);
            }
        });
        const styleFn = (feature) => {
            const i = feature.get('colorIndex') ?? 0;
            const color = LABELING_SECTION_COLORS[i] || '#e6194b';
            if (feature.get('isPointSection')) {
                return new Style({
                    image: new Circle({
                        radius: 6,
                        fill: new Fill({ color }),
                        stroke: new Stroke({ color: '#fff', width: 2 }),
                    }),
                });
            }
            return new Style({
                stroke: new Stroke({ color, width: 8 }),
            });
        };
        if (labelingClickSectionsLayer) {
            labelingClickSectionsLayer.getSource().clear();
            labelingClickSectionsLayer.getSource().addFeatures(features);
            labelingClickSectionsLayer.setStyle(styleFn);
            return;
        }
        const layer = new VectorLayer({
            name: 'Labeling click sections',
            source: new VectorSource({ features }),
            style: styleFn,
        });
        map.addLayer(layer);
        this.setState({ labelingClickSectionsLayer: layer });
    }

    removeLabelingHighlightLayer() {
        const { labelingHighlightLayer } = this.state;
        if (labelingHighlightLayer && map.getLayers().getArray().includes(labelingHighlightLayer)) {
            map.removeLayer(labelingHighlightLayer);
        }
        this.setState({ labelingHighlightLayer: null });
    }

    addLabelingSection() {
        const selected = this.state.labelingSelectedParameters || [];
        if (selected.length !== 1) return;
        const data = (this.state.labelingSeriesDataByParameter || {})[selected[0]];
        if (!data || !data.y || data.y.length === 0) return;
        const { labelingSectionRange } = this.state;
        const y = data.y;
        const numericY = y.filter((v) => v != null && typeof v === 'number' && !isNaN(v));
        const dataMin = numericY.length ? Math.min(...numericY) : 0;
        const dataMax = numericY.length ? Math.max(...numericY) : 1;
        // When user never touched the slider, range is '' -> Number('') is 0; use full data range instead
        const lowRaw = labelingSectionRange.low !== '' ? Number(labelingSectionRange.low) : dataMin;
        const highRaw = labelingSectionRange.high !== '' ? Number(labelingSectionRange.high) : dataMax;
        if (isNaN(lowRaw) || isNaN(highRaw)) return;
        const minVal = Math.min(lowRaw, highRaw);
        const maxVal = Math.max(lowRaw, highRaw);
        // Single segment only: new range replaces the previous one
        const next = [{ min: minVal, max: maxVal }];
        this.setState({ labelingSections: next }, () => {
            this.updateLabelingHighlightLayer();
            if (this.state.labelingViewMode === 'chart') this.updateLabelingChartShapes();
        });
    }

    removeLabelingSection(index) {
        const next = this.state.labelingSections.filter((_, i) => i !== index);
        this.setState({ labelingSections: next }, () => {
            this.updateLabelingHighlightLayer();
            if (this.state.labelingViewMode === 'chart') this.updateLabelingChartShapes();
        });
    }

    /**
     * Highlight on the path all segments where the series value falls within any of labelingSections.
     */
    updateLabelingHighlightLayer() {
        const selected = this.state.labelingSelectedParameters || [];
        if (selected.length !== 1) {
            this.removeLabelingHighlightLayer();
            return;
        }
        const { points, labelingSections, labelingHighlightLayer } = this.state;
        const data = (this.state.labelingSeriesDataByParameter || {})[selected[0]];
        if (!points || points.length === 0 || !data || !data.y || !labelingSections || labelingSections.length === 0) {
            this.removeLabelingHighlightLayer();
            return;
        }
        const y = data.y;
        const inRange = (val) => {
            if (val == null || (typeof val === 'number' && isNaN(val))) return false;
            const v = Number(val);
            if (isNaN(v)) return false;
            return labelingSections.some((s) => v >= s.min && v <= s.max);
        };
        const runs = [];
        let runStart = null;
        for (let i = 0; i < y.length; i++) {
            if (inRange(y[i])) {
                if (runStart == null) runStart = i;
            } else {
                if (runStart != null) {
                    runs.push([runStart, i - 1]);
                    runStart = null;
                }
            }
        }
        if (runStart != null) runs.push([runStart, y.length - 1]);

        const features = runs.map(([start, end]) => {
            const coords = points.slice(start, end + 1);
            if (coords.length < 2) return null;
            const f = new Feature({ geometry: new LineString(coords), name: 'LabelingSection' });
            f.set('startIndex', start);
            f.set('endIndex', end);
            return f;
        }).filter(Boolean);

        const highlightStyle = new Style({
            stroke: new Stroke({
                color: 'rgba(255, 165, 0, 0.9)',
                width: 8,
            }),
        });

        if (labelingHighlightLayer) {
            labelingHighlightLayer.getSource().clear();
            if (features.length > 0) labelingHighlightLayer.getSource().addFeatures(features);
            else {
                map.removeLayer(labelingHighlightLayer);
                this.setState({ labelingHighlightLayer: null });
            }
            return;
        }
        if (features.length === 0) return;
        const layer = new VectorLayer({
            name: 'Labeling value sections',
            source: new VectorSource({ features }),
            style: highlightStyle,
        });
        map.addLayer(layer);
        this.setState({ labelingHighlightLayer: layer });
    }

    /**
     * Update Plotly chart shapes for labeling: value-range highlight and two-click sections.
     * Only runs when labelingViewMode === 'chart'. Removes any existing shapes with id 'labeling-*'.
     */
    updateLabelingChartShapes() {
        const shapes = plotlyLayoutGlobal.shapes || [];
        const { labelingViewMode, labelingSections, labelingClickSections, labelingSectionStart } = this.state;
        const selected = this.state.labelingSelectedParameters || [];
        // Remove previous labeling shapes
        for (let i = shapes.length - 1; i >= 0; i--) {
            if (shapes[i].id && String(shapes[i].id).startsWith('labeling-')) {
                shapes.splice(i, 1);
            }
        }
        const byParam = this.state.labelingSeriesDataByParameter || {};
        const firstData = selected.length > 0 ? byParam[selected[0]] : null;
        if (labelingViewMode !== 'chart' || !firstData || !firstData.x) {
            try { Plotly.relayout('plot', plotlyLayoutGlobal); } catch (e) { /* plot may not exist */ }
            return;
        }
        const x = firstData.x;
        const y = firstData.y || [];

        // Value-range highlight: segments where y is within any of labelingSections
        if (labelingSections && labelingSections.length > 0) {
            const inRange = (val) => {
                if (val == null || (typeof val === 'number' && isNaN(val))) return false;
                const v = Number(val);
                return !isNaN(v) && labelingSections.some((s) => v >= s.min && v <= s.max);
            };
            let runStart = null;
            let valueRunId = 0;
            for (let i = 0; i < y.length; i++) {
                if (inRange(y[i])) {
                    if (runStart == null) runStart = i;
                } else {
                    if (runStart != null) {
                        const x0 = x[runStart];
                        const x1 = x[i - 1];
                        if (x0 != null && x1 != null) {
                            shapes.push({
                                id: `labeling-value-${valueRunId++}`,
                                type: 'rect',
                                xref: 'x', yref: 'paper',
                                x0, x1, y0: 0, y1: 1,
                                fillcolor: 'rgba(255, 165, 0, 0.35)',
                                line: { width: 0 },
                            });
                        }
                        runStart = null;
                    }
                }
            }
            if (runStart != null) {
                const x0 = x[runStart];
                const x1 = x[y.length - 1];
                if (x0 != null && x1 != null) {
                    shapes.push({
                        id: `labeling-value-${valueRunId}`,
                        type: 'rect',
                        xref: 'x', yref: 'paper',
                        x0, x1, y0: 0, y1: 1,
                        fillcolor: 'rgba(255, 165, 0, 0.35)',
                        line: { width: 0 },
                    });
                }
            }
        }

        // Two-click sections: vertical bands (only when section is visible on chart).
        // Use startIndex/endIndex to get x coords from the trace - chart x is seconds-from-flight-start,
        // not Unix seconds, so sec.startTime/endTime would squash the trace into a line.
        if (labelingClickSections && labelingClickSections.length > 0) {
            labelingClickSections.forEach((sec, i) => {
                if (sec.visibleOnChart === false) return;
                const si = sec.startIndex, ei = sec.endIndex;
                if (si == null || ei == null || si < 0 || ei >= x.length) return;
                const x0 = x[si], x1 = x[ei];
                if (x0 == null || x1 == null) return;
                const color = LABELING_SECTION_COLORS[i % LABELING_SECTION_COLORS.length];
                shapes.push({
                    id: `labeling-click-${i}`,
                    type: 'rect',
                    xref: 'x', yref: 'paper',
                    x0, x1, y0: 0, y1: 1,
                    fillcolor: color,
                    opacity: 0.4,
                    line: { width: 0 },
                });
            });
        }

        // Pending section start: vertical line
        if (labelingSectionStart != null) {
            const xVal = labelingSectionStart.time;
            if (xVal != null) {
                shapes.push({
                    id: 'labeling-pending-start',
                    type: 'line',
                    xref: 'x', yref: 'paper',
                    x0: xVal, x1: xVal, y0: 0, y1: 1,
                    line: { color: LABELING_SECTION_COLORS[(labelingClickSections || []).length % LABELING_SECTION_COLORS.length], width: 2, dash: 'dot' },
                });
            }
        }

        try { Plotly.relayout('plot', plotlyLayoutGlobal); } catch (e) { /* plot may not exist */ }
    }

    updateLabelingMarkers() {
        const { labelingSectionStart, labelingClickSections, points, labelingMarkerLayer } = this.state;
        if (!points || points.length === 0) return;
        const features = [];
        if (labelingSectionStart != null) {
            const idx = labelingSectionStart.index;
            if (idx >= 0 && idx < points.length) {
                const f = new Feature({ geometry: new Point(points[idx]), name: 'LabelSectionStart' });
                f.set('pointIndex', idx);
                features.push(f);
            }
        }
        const nextColorIndex = (labelingClickSections || []).length;
        const markerColor = LABELING_SECTION_COLORS[nextColorIndex % LABELING_SECTION_COLORS.length];
        const styleFn = () => new Style({
            image: new Circle({
                radius: 8,
                fill: new Fill({ color: markerColor }),
                stroke: new Stroke({ color: '#fff', width: 2 }),
            }),
        });
        if (labelingMarkerLayer) {
            labelingMarkerLayer.getSource().clear();
            if (features.length > 0) {
                labelingMarkerLayer.getSource().addFeatures(features);
            } else {
                map.removeLayer(labelingMarkerLayer);
                this.setState({ labelingMarkerLayer: null });
            }
            return;
        }
        if (features.length === 0) return;
        const layer = new VectorLayer({
            name: 'Labeling section start',
            source: new VectorSource({ features }),
            style: styleFn,
        });
        map.addLayer(layer);
        this.setState({ labelingMarkerLayer: layer });
    }

    /**
     * Show or update the single labeling popup (top-right of map) with sections table.
     * Sections are from two-click selection; each row: Start date/time, End date/time, Value start, Value end, Remove.
     */
    renderLabelingPopup() {
        const { labelingClickSections, labelingSectionStart, labelingSelectedParameters, labelingPopup } = this.state;
        const startDateTime = this.props.flightInfo.startDateTime;
        const showPopup = (labelingClickSections && labelingClickSections.length > 0) || labelingSectionStart != null;

        if (!showPopup) {
            if (labelingPopup) {
                this.closeLabelingPopup();
            }
            return;
        }

        const sections = (labelingClickSections || []).map((sec) => ({
            startIndex: sec.startIndex,
            endIndex: sec.endIndex,
            startTime: sec.startTime,
            endTime: sec.endTime,
            startTimeDisplay: sec.startTimeDisplay,
            endTimeDisplay: sec.endTimeDisplay,
            startValue: sec.startValue,
            endValue: sec.endValue,
            label: sec.label ?? '',
            parameterNames: sec.parameterNames || [],
            visibleOnChart: sec.visibleOnChart !== false,
        }));

        const paramDisplay = (labelingSelectedParameters || []).length > 0
            ? (labelingSelectedParameters || []).join(', ')
            : 'Sections';

        const popupProps = {
            paramName: paramDisplay,
            sections,
            startDateTime,
            labelDefinitions: this.state.labelDefinitions || [],
            onRefreshLabels: () => this.fetchFleetLabelDefinitions(),
            pendingSectionStart: labelingSectionStart != null,
            selectionHint: this.state.labelingViewMode === 'chart' ? 'chart' : 'path',
            onToggleVisibility: (i, visible) => {
                const next = (this.state.labelingClickSections || []).map((sec, idx) =>
                    idx === i ? { ...sec, visibleOnChart: visible } : sec
                );
                this.setState({ labelingClickSections: next }, () => {
                    this.updateLabelingClickSectionsLayer();
                    this.updateLabelingChartShapes();
                    this.renderLabelingPopup();
                });
            },
            onUpdateLabel: (i, value) => {
                const sections = this.state.labelingClickSections || [];
                const target = sections[i];
                const next = sections.map((sec, idx) =>
                    idx === i ? { ...sec, label: value } : sec
                );
                this.setState({ labelingClickSections: next }, () => this.renderLabelingPopup());

                if (target && target.id != null) {
                    const flightIdUpdate = this.props.flightInfo.id;
                    $.ajax({
                        type: 'PUT',
                        url: `/api/flight/${flightIdUpdate}/labels/${encodeURIComponent(target.id)}`,
                        contentType: 'application/json',
                        data: JSON.stringify({ labelText: value }),
                        error: (jqXHR, textStatus, errorThrown) => {
                            console.error("Error updating label text", errorThrown);
                        },
                    });
                }
            },
            onRemoveSection: (i) => {
                const sections = this.state.labelingClickSections || [];
                const target = sections[i];
                const next = sections.filter((_, idx) => idx !== i);
                this.setState({ labelingClickSections: next }, () => {
                    this.updateLabelingClickSectionsLayer();
                    this.renderLabelingPopup();
                    if (this.state.labelingViewMode === 'chart') this.updateLabelingChartShapes();
                });

                if (target && target.id != null) {
                    const flightIdDelete = this.props.flightInfo.id;
                    $.ajax({
                        type: 'DELETE',
                        url: `/api/flight/${flightIdDelete}/labels/${encodeURIComponent(target.id)}`,
                        error: (jqXHR, textStatus, errorThrown) => {
                            console.error("Error deleting label section", errorThrown);
                        },
                    });
                }
            },
            onClearAll: () => {
                this.setState({ labelingClickSections: [], labelingSectionStart: null }, () => {
                    this.updateLabelingMarkers();
                    this.updateLabelingClickSectionsLayer();
                    this.renderLabelingPopup();
                    if (this.state.labelingViewMode === 'chart') this.updateLabelingChartShapes();
                });
            },
            onClose: () => {
                this.closeLabelingPopup();
            },
        };

        if (labelingPopup) {
            const desiredContainer = this.state.labelingViewMode === 'chart'
                ? (document.getElementById('plot-container') || document.body)
                : (document.getElementById('map-container') || document.getElementById('map') || document.body);
            if (labelingPopup.element && labelingPopup.element.parentNode !== desiredContainer) {
                desiredContainer.appendChild(labelingPopup.element);
            }
            labelingPopup.root.render(React.createElement(LabelingMapPopup, popupProps));
            return;
        }

        const container = this.state.labelingViewMode === 'chart'
            ? (document.getElementById('plot-container') || document.body)
            : (document.getElementById('map-container') || document.getElementById('map') || document.body);
        const popupEl = document.createElement('div');
        popupEl.id = 'labeling-popup-root';
        popupEl.style.cssText = 'position:absolute;left:0;top:0;right:0;bottom:0;width:100%;height:100%;z-index:10000;pointer-events:none;overflow:visible;';
        container.appendChild(popupEl);
        const root = createRoot(popupEl);
        root.render(React.createElement(LabelingMapPopup, popupProps));
        this.setState({ labelingPopup: { root, element: popupEl } });
    }

    // forLabeling=true when opened from labeling tool: path-only map, no events/itinerary, sets labelingPathVisible
    async mapClicked(forLabeling = false) {

        //Flagged as not having coordinate info, exit
        if (this.props.flightInfo.has_coords === "0")
            return;

        //2D map layer not loaded for this flight...
        if (!this.state.mapLoaded) {

            this.props.showMap();
            this.setState({ mapLoaded: true });

            await this.fetchEvents();
            console.log("Events Fetched: ", this.state.events);

            //TODO: get upset probability data here
            console.log("Getting upset probabilities...");

            const names = [
                "Stall Index",
                "LOC-I Index",
            ];

            for (let i = 0; i < names.length; i++) {

                const name = names[i];
                console.log("Double series: ", name);

                $.ajax({
                    type: 'GET',
                    url: `/api/flight/${this.props.flightInfo.id}/double-series/${encodeURIComponent(name)}`,
                    dataType: 'json',
                    async: false,
                    success: (response) => {

                        console.log("Got double_series response: ", response);
                        this.state.seriesData.set(name, response.y);

                    },
                    error: (jqXHR, textStatus, errorThrown) => {
                        console.log("Error getting upset data:", errorThrown);
                    },
                });
            }

            $.ajax({
                type: 'GET',
                url: `/api/flight/${this.props.flightInfo.id}/coordinates`,
                async: true,
                success: (response) => {

                    const coordinates = response.coordinates;
                    const points = response.coordinates.map((lonLat) => fromLonLat(lonLat));

                    const color = this.state.color;

                    const trackingPoint = new Feature({
                        geometry: new Point(points[0]),
                        name: 'TrackingPoint'
                    });

                    trackingPoint.setId(points[0]);

                    // Initialize layers as a new array
                    const layers = [];

                    // adding itinerary (approaches and takeoffs) to flightpath 
                    const itinerary = this.props.flightInfo.itinerary;
                    const flight_phases = [];

                    // Create flight phase styles
                    const takeoff_style = new Style({
                        stroke: new Stroke({
                            color: "#34eb52",
                            width: 3
                        })
                    });

                    const approach_style = new Style({
                        stroke: new Stroke({
                            color: "#347deb",
                            width: 3
                        })
                    });

                    // create and add Features to flight_phases for each flight phase in itinerary
                    for (let i = 0; i < itinerary.length; i++) {

                        const stop = itinerary[i];
                        let approach = null;
                        let takeoff = null;

                        // creating Linestrings
                        if (stop.startOfApproach != -1 && stop.endOfApproach != -1) {
                            approach = new LineString(points.slice(stop.startOfApproach, stop.endOfApproach));
                        }
                        if (stop.startOfTakeoff != -1 && stop.endOfTakeoff != -1) {
                            takeoff = new LineString(points.slice(stop.startOfTakeoff, stop.endOfTakeoff));
                        }

                        // set styles and add phases to flight_phases list
                        if (approach != null) {
                            const phase = new Feature({
                                geometry: approach,
                                name: 'Approach'
                            });
                            phase.setStyle(approach_style);
                            flight_phases.push(phase);
                        }
                        if (takeoff != null) {
                            const phase = new Feature({
                                geometry: takeoff,
                                name: 'Takeoff'
                            });
                            phase.setStyle(takeoff_style);
                            flight_phases.push(phase);
                        }
                    }

                    console.log("[EX] Flight Points: ", points);
                    console.log("[EX] Flight Tracking Point: ", trackingPoint);
                    const lineFeature = new Feature({
                        geometry: new LineString(points),
                        name: 'Line'
                    });
                    lineFeature.set('flightId', this.props.flightInfo.id); // used by labeling hover to find this flight's path
                    const baseLayer = new VectorLayer({
                        name: 'Itinerary',
                        description: 'Itinerary with Phases',
                        nMap: false,
                        style: new Style({
                            stroke: new Stroke({
                                color: color,
                                width: 3
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

                        source: new VectorSource({
                            features: [
                                lineFeature,
                                trackingPoint,
                            ]
                        })
                    });

                    const phaseLayer = new VectorLayer({
                        name: 'Itinerary Phases',
                        nMap: true,
                        style: new Style({
                            stroke: new Stroke({
                                color: [1, 1, 1, 1],
                                width: 3
                            })
                        }),

                        source: new VectorSource({
                            features: flight_phases
                        })
                    });

                    baseLayer.flightState = this;

                    // toggle visibility of itinerary
                    layers.push(baseLayer, phaseLayer);

                    const lociData = this.state.seriesData.get('LOC-I Index');
                    const spData = this.state.seriesData.get('Stall Index');

                    generateStallLayer(spData, points, trackingPoint, layers, this);
                    generateLOCILayer(lociData, points, trackingPoint, layers, this);

                    console.log("adding layers!");
                    for (let i = 0; i < layers.length; i++) {
                        const layer = layers[i];
                        console.log(layer);
                        if (forLabeling) {
                            // Labeling: show only path (base) layer
                            layer.setVisible(layer === baseLayer);
                        } else if (layer.get('name').includes('Itinerary')) {
                            this.setState({ selectedPlot: layer.values_.name });
                            layer.setVisible(true);
                        } else {
                            layer.setVisible(false);
                        }
                        map.addLayer(layer);
                    }

                    console.log(layers);
                    this.props.setAvailableLayers(layers);

                    console.log("Added layers:", map.getLayers());
                    map.on('click', this.displayParameters);
                    map.on('pointermove', this.labelingHover);

                    const currZoom = map.getView().getZoom();
                    map.on('moveend', () => this.zoomChanged(currZoom));
                    // Add event/outline layers (skipped in labeling mode — path only)
                    if (!forLabeling && this.state.eventsLoaded) {
                        const events = this.state.events;
                        const eventPoints = this.state.eventPoints;
                        const eventOutlines = this.state.eventOutlines;
                        for (let i = 0; i < events.length; i++) {
                            const line = new LineString(points.slice(events[i].startLine - 1, events[i].endLine + 1));
                            eventPoints[i].setGeometry(line);
                            eventOutlines[i].setGeometry(line);
                        }
                        const eventLayer = this.state.eventLayer;
                        const outlineLayer = this.state.eventOutlineLayer;
                        map.addLayer(outlineLayer);
                        map.addLayer(eventLayer);
                    }

                    const extent = baseLayer.getSource().getExtent();
                    console.log(extent);
                    map.getView().fit(extent, map.getSize());

                    this.setState({
                        baseLayer,
                        layers: [...layers],
                        points,
                        coordinates: response.coordinates,
                        nanOffset: response.nanOffset,
                        selectedPlot: forLabeling ? this.state.selectedPlot : 'Itinerary',
                        pathVisible: !forLabeling,
                        itineraryVisible: !forLabeling,
                        labelingPathVisible: forLabeling
                    });
                },
                error: (jqXHR, textStatus, errorThrown) => {
                    this.setState({
                        mapLoaded: false,
                        mapButtonDisabled: true,
                    });

                    showErrorModal("Error Loading Flight Coordinates", errorThrown);
                },
            });

        //2D map layer already loaded for this flight...
        } else {
            // Map already loaded: labeling mode switches to path-only and returns
            if (forLabeling) {
                for (const layer of (this.state.layers || [])) {
                    layer.setVisible(layer === this.state.baseLayer);
                }
                if (this.state.eventLayer) this.state.eventLayer.setVisible(false);
                if (this.state.eventOutlineLayer) this.state.eventOutlineLayer.setVisible(false);
                this.props.showMap();
                if (this.state.baseLayer) {
                    const extent = this.state.baseLayer.getSource().getExtent();
                    map.getView().fit(extent, map.getSize());
                }
                this.setState({ labelingPathVisible: true, pathVisible: false });
                return;
            }

            const nextPathVisible = !this.state.pathVisible;
            const nextItineraryVisible = !this.state.itineraryVisible;

            let nextSelected = this.state.selectedPlot;

            //Path isn't visible yet...
            if (!nextPathVisible) {

                for (const layer of (this.state.layers || [])) {

                    if (layer.getVisible())
                        nextSelected = layer.get('name') ?? layer.values_?.name;

                    layer.setVisible(false);

                }

            //Otherwise...
            } else {

                for (const layer of (this.state.layers || [])) {
                    const name = layer.get('name') ?? layer.values_?.name;
                    layer.setVisible(name === nextSelected);
                }

            }

            //Ensure events follow the path's visibility
            if (this.state.eventLayer) {
                const showEvents = nextPathVisible && this.state.eventsVisible;
                this.state.eventLayer.setVisible(showEvents);
                this.state.eventOutlineLayer.setVisible(showEvents);
            }

            //Showing the path again, refit and show the map
            if (nextPathVisible && this.state.baseLayer) {
                this.props.showMap();
                const extent = this.state.baseLayer.getSource().getExtent();
                map.getView().fit(extent, map.getSize());
            }

            //Trigger state update
            this.setState({
                pathVisible: nextPathVisible,
                itineraryVisible: nextItineraryVisible,
                selectedPlot: nextSelected
            });

        }

    }

    /**
     * Changes all the flights on a given page by calling the parent function
     */
    updateFlights(flights) {
        this.props.updateParentState(flights);
    }

    /**
     * Changes the tags associated with this flight
     */
    invokeUpdate(tags) {
        this.setState({ tags: tags });
    }

    /**
     * Called when props are updated
     * changes state if props have in fact changed
     * @param oldProps the old props before the update
     */
    componentDidUpdate(oldProps) {
        console.log("props updated");
        const newProps = this.props;
        if (oldProps.tags !== newProps.tags) {
            this.setState({ tags: this.props.tags });
        }
        // Re-render labeling popup when expand/collapse changes so it stays visible in both layouts
        if (oldProps.containerExpanded !== newProps.containerExpanded && this.state.labelingPopup) {
            setTimeout(() => this.renderLabelingPopup(), 0);
        }
    }

    addCesiumFlightPhase(phase) {
        this.props.addCesiumFlightPhase(phase);
    }

    addCesiumEventEntity(event) {
        console.log("Adding event to Cesium: ", event);
        this.props.addCesiumEventEntity(event, this.props.flightInfo.id);
    }

    zoomToEventEntity(eventId, flightId) {
        this.props.zoomToEventEntity(eventId, flightId);
    }

    render() {

        const buttonClasses = "p-1 expand-import-button btn btn-outline-secondary d-flex align-items-center justify-content-center";
        const cesiumControlButtonClasses = "p-1 btn btn-primary d-flex align-items-center justify-content-center";
        //const styleButton = { minWidth:"2.25em", minHeight:"2.25em" };'

        const buttonSize = "1.75em";
        const styleButton = {
            minWidth: buttonSize,
            minHeight: buttonSize,
            width: buttonSize,
            height: buttonSize,
            display: "inlineBlock",
            justifyContent: "center",
            alignContent: "center",
            textAlign: "center"
        };
        const styleEmptyCell = {fontStyle: "italic", fontSize: "0.75em", opacity: "0.50", userSelect: "none"};

        const firstCellClasses = "p-1 card mr-1";
        const cellClasses = "p-1 card mr-1";

        const flightInfo = this.props.flightInfo;

        const startTime = moment(flightInfo.startDateTime);
        const endTime = moment(flightInfo.endDateTime);

        const tagTooltip = "Click to tag a flight for future queries and grouping";

        const visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0) {
                visitedAirports.push(flightInfo.itinerary[i].airport);
            }
        }
        let visitedAirportsRow = "";
        if (visitedAirports.length > 0)
            visitedAirportsRow = visitedAirports.join(", ");
        else
            visitedAirportsRow = <div style={styleEmptyCell}>No Airports...</div>;


        const FLIGHT_COMPONENT_ROW_HIDDEN = "";


        //Events Row
        const events = this.state.events ?? [];
        let eventsRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        if (this.state.eventsVisible) {
            eventsRow = (
                <Events className="w-100" events={events} parent={this}/>
            );
        }


        //Tags Row
        let tagsRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        if (this.state.tagsVisible) {

            tagsRow = (
                <Tags
                    flight={this.props.flightInfo}
                    flightIndex={this.state.pageIndex}
                    flightId={flightInfo.id}
                    parent={this}
                    addTag={this.props.addTag}
                    removeTag={this.props.removeTag}
                    deleteTag={this.props.deleteTag}
                    getUnassociatedTags={this.props.getUnassociatedTags}
                    associateTag={this.props.associateTag}
                    clearTags={this.props.clearTags}
                    editTag={this.props.editTag}
                />
            );

        }

        let tagPills = "";
        if (this.props.flightInfo.tags != null && this.props.flightInfo.tags.length > 0) {
            tagPills =
                this.props.flightInfo.tags.map((tag, index) => {
                    const style = {
                        backgroundColor: tag.color,
                        marginRight: '4px',
                        lineHeight: '2',
                        opacity: '75%'
                    };
                    return (
                        <span key={index} className="badge badge-primary" style={{
                            lineHeight: '1.5',
                            marginRight: '4px',
                            backgroundColor: 'var(--c_tag_badge)',
                            color: 'var(--c_text)'
                        }} title={tag.description}>
                        <span className="badge badge-pill badge-primary" style={style}>
                            <i className="fa fa-tag" aria-hidden="true"/>
                        </span> {tag.name}
                    </span>
                    );
                });
        } else {
            tagPills = <div style={styleEmptyCell}>No Tags...</div>;
        }


        //Itinerary Row
        let itineraryRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        const itineraryReady =
            this.state.itineraryVisible
            && Array.isArray(this.state.layers)
            && (this.state.layers.length > 0);
        if (itineraryReady) {

            itineraryRow = (
                <Itinerary
                    showMap={() => {
                        this.props.showMap();
                    }}
                    layers={this.state.layers}
                    itinerary={flightInfo.itinerary}
                    color={this.state.color}
                    coordinates={this.state.coordinates}
                    nanOffset={this.state.nanOffset}
                    parent={this}
                    flightColorChange={this.flightColorChange}
                />
            );

        }


        //Traces Row
        let tracesRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        if (this.state.traceNamesVisible) {

            tracesRow = (
                <TraceButtons showPlot={() => {
                    this.props.showPlot();
                }} parentFlight={this} flightId={flightInfo.id}/>
            );

        }

        // Labeling parameters row: scrollable list, multi-select toggle
        const labelingButtonClasses = "m-1 btn btn-outline-secondary";
        const labelingButtonStyle = { flex: "0 0 10em" };
        let labelingRow = FLIGHT_COMPONENT_ROW_HIDDEN;
            if (this.state.labelingParametersVisible && this.state.labelingParameterNames.length > 0) {
            const selectedParams = this.state.labelingSelectedParameters || [];
            const singleParamSeries = selectedParams.length === 1 ? (this.state.labelingSeriesDataByParameter || {})[selectedParams[0]] : null;
            const seriesData = singleParamSeries;
            const yArr = seriesData && seriesData.y ? seriesData.y : [];
            const numericY = yArr.filter((v) => v != null && typeof v === 'number' && !isNaN(v));
            const minVal = numericY.length ? Math.min(...numericY) : 0;
            const maxVal = numericY.length ? Math.max(...numericY) : 1;
            const rangeLow = this.state.labelingSectionRange.low;
            const rangeHigh = this.state.labelingSectionRange.high;

            labelingRow = (
                <div className="w-100">
                    <b className="p-1 d-flex flex-row justify-content-between align-items-center" style={{ marginBottom: "0" }}>
                        <div className="d-flex flex-row align-items-center">
                            <div className="d-flex flex-column mr-3" style={{ width: "16px", minWidth: "16px", maxWidth: "16px", height: "16px" }}>
                                <i className="fa fa-tag ml-2" style={{ fontSize: "12px", marginTop: "3px", opacity: "0.50" }} />
                            </div>
                            <div style={{ fontSize: "0.75em" }}>Parameters</div>
                        </div>
                        <div className="d-flex flex-row align-items-center gap-1 flex-wrap" style={{ fontSize: "0.75em" }}>
                            {selectedParams.length > 0 && (
                                <>
                                    <span className="text-muted mr-1">View:</span>
                                    <div className="btn-group btn-group-sm" role="group">
                                        <button
                                            type="button"
                                            className={`btn btn-outline-secondary ${this.state.labelingViewMode === 'chart' ? 'active' : ''}`}
                                            onClick={() => {
                                                this.setState({ labelingViewMode: 'chart' }, () => {
                                                    this.updateLabelingChartShapes();
                                                    this.renderLabelingPopup();
                                                });
                                                this.props.showPlot();
                                                if (this.props.onLabelingViewMode) this.props.onLabelingViewMode('chart');
                                                if (Object.keys(this.state.labelingTraceIndices || {}).length > 0) this.attachLabelingPlotClickListener();
                                            }}
                                            title="Select sections on the chart"
                                        >
                                            <i className="fa fa-area-chart mr-1" /> Chart
                                        </button>
                                        <button
                                            type="button"
                                            className={`btn btn-outline-secondary ${this.state.labelingViewMode === 'map' ? 'active' : ''}`}
                                            disabled={this.props.flightInfo.has_coords === "0"}
                                            onClick={() => {
                                                this.setState({ labelingViewMode: 'map' }, () => {
                                                    this.updateLabelingChartShapes();
                                                    this.renderLabelingPopup();
                                                });
                                                this.props.showMap();
                                                this.showMapForLabeling();
                                                if (this.props.onLabelingViewMode) this.props.onLabelingViewMode('map');
                                                this.removeLabelingPlotClickListener();
                                                this.fitMapToFlightWhenVisible();
                                            }}
                                            title="Select sections on the map"
                                        >
                                            <i className="fa fa-map-o mr-1" /> Map
                                        </button>
                                    </div>
                                </>
                            )}
                            <div className="btn-group btn-group-sm ml-2" role="group">
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    title="Download labels for this flight as CSV"
                                    onClick={() => { window.location.href = `/api/flight/${this.props.flightInfo.id}/labels/csv`; }}
                                >
                                    <i className="fa fa-download mr-1" /> Flight
                                </button>
                                <button
                                    type="button"
                                    className="btn btn-outline-secondary"
                                    title="Download all fleet labels as CSV"
                                    onClick={() => { window.location.href = '/api/fleet/labels/csv'; }}
                                >
                                    <i className="fa fa-download mr-1" /> Fleet
                                </button>
                                <label className="btn btn-outline-secondary mb-0" title="Import labels from CSV. Use pipe (|) to separate parameter names.">
                                    <input
                                        type="file"
                                        accept=".csv"
                                        className="d-none"
                                        onChange={(e) => {
                                            const file = e.target.files?.[0];
                                            if (!file) return;
                                            const flightId = this.props.flightInfo.id;
                                            const formData = new FormData();
                                            formData.append('file', file);
                                            $.ajax({
                                                url: `/api/flight/${flightId}/labels/import`,
                                                type: 'POST',
                                                data: formData,
                                                processData: false,
                                                contentType: false,
                                                success: (res) => {
                                                    if (res.imported != null && res.imported > 0) {
                                                        $.ajax({
                                                            type: 'GET',
                                                            url: `/api/flight/${flightId}/labels`,
                                                            dataType: 'json',
                                                            success: (sections) => this.applyLabelSections(this.mapApiSectionsToClickSections(sections)),
                                                        });
                                                    }
                                                },
                                                error: (xhr) => console.error('Import failed', xhr.responseText),
                                            });
                                            e.target.value = '';
                                        }}
                                    />
                                    <i className="fa fa-upload mr-1" /> Import
                                </label>
                            </div>
                        </div>
                    </b>
                    <div className="p-1">
                        <div className="d-flex flex-row pb-1" style={{ overflowX: "auto", overflowY: "visible", flexWrap: "nowrap", paddingTop: "1.25rem" }}>
                            {(() => {
                                const getCount = (n) => (this.state.labelingClickSections || []).filter((s) => (s.parameterNames || []).includes(n)).length;
                                const sorted = [...this.state.labelingParameterNames].sort((a, b) => getCount(b) - getCount(a));
                                return sorted.map((name) => {
                                    const isSelected = (this.state.labelingSelectedParameters || []).includes(name);
                                    const count = getCount(name);
                                    return (
                                        <div key={name} style={{ position: "relative", display: "inline-block", marginRight: "0.25rem", flexShrink: 0 }}>
                                            {count > 0 && (
                                                <span style={{
                                                    position: "absolute",
                                                    bottom: "100%",
                                                    left: 0,
                                                    right: 0,
                                                    marginBottom: 2,
                                                    fontSize: "0.7em",
                                                    color: "var(--c_text_muted, #6c757d)",
                                                    textAlign: "center",
                                                    whiteSpace: "nowrap",
                                                }}>
                                                    labels: {count}
                                                </span>
                                            )}
                                            <button
                                                type="button"
                                                className={labelingButtonClasses + (isSelected ? " active" : "")}
                                                style={labelingButtonStyle}
                                                onClick={() => this.labelingParameterClicked(name)}
                                            >
                                                {name}
                                            </button>
                                        </div>
                                    );
                                });
                            })()}
                        </div>
                        {selectedParams.length === 1 && seriesData && yArr.length > 0 && (
                            <div className="mt-2 p-2 border rounded" style={{ fontSize: "0.8em", background: "var(--c_row_bg_alt, #f8f9fa)" }}>
                                <div className="mb-1">
                                    <strong>Value range</strong>: {minVal.toFixed(3)} to {maxVal.toFixed(3)}
                                </div>
                                <div className="mb-2">
                                    <div className="d-flex justify-content-between small text-muted mb-1">
                                        <span>Low</span>
                                        <span>High</span>
                                    </div>
                                    <div
                                        className="labeling-dual-range"
                                        style={{ position: 'relative', height: 28, display: 'flex', alignItems: 'center' }}
                                        onMouseMove={(e) => {
                                            const rect = e.currentTarget.getBoundingClientRect();
                                            const x = (e.clientX - rect.left) / rect.width;
                                            const lowNum = rangeLow !== '' ? parseFloat(rangeLow) : minVal;
                                            const highNum = rangeHigh !== '' ? parseFloat(rangeHigh) : maxVal;
                                            const lowVal = Math.max(minVal, Math.min(maxVal, lowNum));
                                            const highVal = Math.max(minVal, Math.min(maxVal, highNum));
                                            const range = maxVal - minVal || 1;
                                            const lowNorm = (lowVal - minVal) / range;
                                            const highNorm = (highVal - minVal) / range;
                                            const mid = (lowNorm + highNorm) / 2;
                                            const preferLow = x < mid;
                                            if (this._labelingSliderPreferLow !== preferLow) {
                                                this._labelingSliderPreferLow = preferLow;
                                                this.forceUpdate();
                                            }
                                        }}
                                        onMouseLeave={() => {
                                            if (this._labelingSliderPreferLow !== null) {
                                                this._labelingSliderPreferLow = null;
                                                this.forceUpdate();
                                            }
                                        }}
                                    >
                                        {(() => {
                                            const lowNum = rangeLow !== '' ? parseFloat(rangeLow) : minVal;
                                            const highNum = rangeHigh !== '' ? parseFloat(rangeHigh) : maxVal;
                                            const lowVal = Math.max(minVal, Math.min(maxVal, lowNum));
                                            const highVal = Math.max(minVal, Math.min(maxVal, highNum));
                                            const range = maxVal - minVal || 1;
                                            const lowPct = ((lowVal - minVal) / range) * 100;
                                            const highPct = ((highVal - minVal) / range) * 100;
                                            const preferLow = this._labelingSliderPreferLow !== false;
                                            const leftZ = preferLow ? 3 : 2;
                                            const rightZ = preferLow ? 2 : 3;
                                            return (
                                                <>
                                                <div style={{
                                                    position: 'absolute', left: 0, right: 0, height: 6,
                                                    background: 'var(--c_border_alt, #dee2e6)', borderRadius: 3,
                                                }}/>
                                                <div style={{
                                                    position: 'absolute',
                                                    left: `${lowPct}%`,
                                                    right: `${100 - highPct}%`,
                                                    height: 6,
                                                    background: '#0d6efd',
                                                    borderRadius: 3,
                                                }}/>
                                                <input
                                                    type="range"
                                                    min={minVal}
                                                    max={maxVal}
                                                    step={(maxVal - minVal) / 500 || 0.01}
                                                    value={lowVal}
                                                    onChange={(e) => {
                                                        const v = parseFloat(e.target.value);
                                                        this.setState({ labelingSectionRange: { low: String(v), high: String(Math.max(v, highVal)) } });
                                                    }}
                                                    style={{ position: 'absolute', width: '100%', margin: 0, height: 28, zIndex: leftZ, pointerEvents: 'auto' }}
                                                />
                                                <input
                                                    type="range"
                                                    min={minVal}
                                                    max={maxVal}
                                                    step={(maxVal - minVal) / 500 || 0.01}
                                                    value={highVal}
                                                    onChange={(e) => {
                                                        const v = parseFloat(e.target.value);
                                                        this.setState({ labelingSectionRange: { low: String(Math.min(v, lowVal)), high: String(v) } });
                                                    }}
                                                    style={{ position: 'absolute', width: '100%', margin: 0, height: 28, zIndex: rightZ, pointerEvents: 'auto' }}
                                                />
                                                </>
                                            );
                                        })()}
                                    </div>
                                </div>
                                <div className="d-flex flex-row align-items-center flex-wrap gap-2 mb-2">
                                    <label className="d-flex align-items-center">
                                        <span className="mr-1">From</span>
                                        <input
                                            type="number"
                                            className="form-control form-control-sm"
                                            style={{ width: "6em" }}
                                            placeholder={String(minVal.toFixed(2))}
                                            value={rangeLow}
                                            onChange={(e) => this.setState({ labelingSectionRange: { ...this.state.labelingSectionRange, low: e.target.value } })}
                                            step={typeof minVal === 'number' && (maxVal - minVal) < 100 ? (maxVal - minVal) / 100 : undefined}
                                        />
                                    </label>
                                    <label className="d-flex align-items-center">
                                        <span className="mr-1">To</span>
                                        <input
                                            type="number"
                                            className="form-control form-control-sm"
                                            style={{ width: "6em" }}
                                            placeholder={String(maxVal.toFixed(2))}
                                            value={rangeHigh}
                                            onChange={(e) => this.setState({ labelingSectionRange: { ...this.state.labelingSectionRange, high: e.target.value } })}
                                            step={typeof maxVal === 'number' && (maxVal - minVal) < 100 ? (maxVal - minVal) / 100 : undefined}
                                        />
                                    </label>
                                    <button
                                        type="button"
                                        className="btn btn-sm btn-primary"
                                        onClick={() => this.addLabelingSection()}
                                    >
                                        Set section
                                    </button>
                                </div>
                                {this.state.labelingSections && this.state.labelingSections.length > 0 && (
                                    <div>
                                        <strong className="mr-2">Section:</strong>
                                        <span className="badge badge-secondary mr-1 mb-1 d-inline-flex align-items-center">
                                            [{this.state.labelingSections[0].min.toFixed(2)}, {this.state.labelingSections[0].max.toFixed(2)}]
                                            <button
                                                type="button"
                                                className="btn btn-link btn-sm p-0 ml-1"
                                                style={{ color: "inherit", fontSize: "0.9em" }}
                                                onClick={() => this.removeLabelingSection(0)}
                                                aria-label="Clear section"
                                            >
                                                <i className="fa fa-times"/>
                                            </button>
                                        </span>
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            );
        }

        //Cesium Row
        let cesiumRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        const flightId = flightInfo.id;
        const flightPhases = ["Taxiing", "Takeoff", "Climb", "Cruise to Final", "Full Flight"];
        if (this.state.cesiumFlightEnabled) {

            let cesiumHeader = "";
            let flightPhasesCheckBox = "";
            let toggleCameraButton = "";
            let jumpToStartButton = "";

            cesiumHeader = (
                <b className={"p-1 d-flex flex-row justify-content-start align-items-center"}
                   style={{marginBottom: "0"}}>
                    <div className="d-flex flex-column mr-3"
                         style={{width: "16px", minWidth: "16px", maxWidth: "16px", height: "16px"}}>
                        <i className='fa fa-globe ml-2' style={{fontSize: "12px", marginTop: "3px", opacity: "0.50"}}/>
                    </div>
                    <div style={{fontSize: "0.75em"}}>
                        Cesium Phases
                    </div>
                </b>
            );

            flightPhasesCheckBox = (
                <div>
                    <div className={"d-flex flex-row p-1"} style={{"overflowX": "auto"}}>
                        {
                            flightPhases.map((phase, index) => {
                                return (
                                    <button
                                        className={`${buttonClasses  } mr-1`}
                                        style={{flex: "0 0 10em"}}
                                        data-bs-toggle="button"
                                        key={index}
                                        onClick={() => this.props.addCesiumFlightPhase(phase, flightId)}
                                    >
                                        {phase}
                                    </button>
                                );
                            })
                        }
                    </div>
                </div>
            );
            toggleCameraButton = (
                <button className={`${cesiumControlButtonClasses} ml-1 mt-1 mb-1 mr-0`} style={{flex: "0 0 10em"}}
                        aria-pressed="false" onClick={() => this.props.cesiumFlightTrackedSet(flightId)}>
                    <i className="fa fa-camera mr-2"/>
                    Track Flight
                </button>

            );
            jumpToStartButton = (
                <button className={`${cesiumControlButtonClasses} ml-1 mt-1 mb-1 mr-0`} style={{flex: "0 0 10em"}}
                        aria-pressed="false" onClick={() => this.props.cesiumJumpToFlightStart(flightId)}>
                    <i className="fa fa-play mr-2"/>
                    Jump to Start
                </button>
            );

            cesiumRow = (
                <div id="cesium-row" className="d-flex flex-column w-100">
                    {cesiumHeader}
                    <div className="m-1">
                        {flightPhasesCheckBox}
                        <div className="d-flex flex-row">
                            {toggleCameraButton}
                            {jumpToStartButton}
                        </div>
                    </div>
                </div>
            );
        }


        //List of all rows
        const rowList = [
            tagsRow,
            tracesRow,
            labelingRow,
            cesiumRow,
            itineraryRow,
            eventsRow,
        ];

        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip();
        });

        return (
            <div className="card mb-1" style={{backgroundColor: "var(--c_entry_bg)"}}>
                <div className="">
                    <div className="d-flex flex-column">

                        <div className="d-flex flex-row p-1">

                            {/* FLIGHT INFO */}
                            <div style={{flexBasis: "32.5%", whiteSpace: "nowrap"}}>
                                <div className={`${firstCellClasses} d-flex flex-row`} style={{height: "100%"}}>
                                    <div className="d-flex flex-column" style={{alignItems: "start"}}>

                                        {/* Filter Add Button */}
                                        <a
                                            href={"#"}
                                            onMouseEnter={() => this.setState({filterAddButtonHovered: true})}
                                            onMouseLeave={() => this.setState({filterAddButtonHovered: false})}
                                            onClick={() => this.props.onAddFilter(this.props.flightInfo.id)}
                                        >
                                            <i className={`fa ${this.state.filterAddButtonHovered ? "fa-search" : "fa-plane"}  p-1`}>
                                                &nbsp;{flightInfo.id}
                                            </i>
                                        </a>

                                        <div>
                                            ◦&nbsp;
                                            {
                                                (flightInfo.tailNumber != null && flightInfo.tailNumber != "")
                                                    ? <a>{flightInfo.tailNumber}</a>
                                                    : <a style={styleEmptyCell}>No Tail Number...</a>
                                            }
                                        </div>
                                    </div>

                                    <div className="d-flex flex-column ml-3" style={{alignItems: "start"}}>
                                        <div>
                                            ◦&nbsp;
                                            {
                                                (flightInfo.systemId != null && flightInfo.systemId != "")
                                                    ? <a>{flightInfo.systemId}</a>
                                                    : <a style={styleEmptyCell}>No System ID...</a>
                                            }
                                        </div>
                                        <div>
                                            ◦&nbsp;
                                            {
                                                (flightInfo.airframe.name != null && flightInfo.airframe.name != "")
                                                ? <a>{flightInfo.airframe.name}</a>
                                                : <a style={styleEmptyCell}>No Airframe Name...</a>
                                            }
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* START - END DATES */}
                            <div style={{flexBasis: "32.5%", whiteSpace: "nowrap"}}>
                                <div className={`${cellClasses} d-flex flex-row`} style={{height: "100%"}}>
                                    <div className="d-flex flex-column" style={{alignItems: "center"}}>
                                        <div>
                                            ◦ {flightInfo.startDateTime}
                                        </div>
                                        <div>
                                            ◦ {flightInfo.endDateTime}
                                        </div>
                                    </div>

                                    <div className="d-flex flex-column ml-3" style={{alignItems: "center"}}>
                                        <div>
                                            ◦ {moment.utc(endTime.diff(startTime)).format("HH:mm:ss")}
                                        </div>
                                        <div style={{visibility: "hidden"}}>
                                            &emsp;
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* AIRPORTS */}
                            <div className={cellClasses} style={{flexBasis: "12.50%"}}>
                                {visitedAirportsRow}
                            </div>

                            {/* TAGS */}
                            <div className={cellClasses} style={{flexBasis: "22.50%"}}>
                                <div style={{overflow: "hidden"}}>

                                    <div
                                        style={{position: "absolute", top: "1", right: "1", zIndex: "1", scale: "0.75"}}
                                        onClick={() => this.tagClicked()}>
                                        <button
                                            className={"p-1 btn btn-outline-secondary d-flex align-items-center justify-content-center"}
                                            data-bs-toggle="button" title={tagTooltip} aria-pressed="false"
                                            style={{...styleButton, border: "none"}}>
                                            <i className="fa fa-plus p-1"/>
                                        </button>
                                    </div>

                                    {tagPills}


                                </div>
                            </div>


                            {/* BUTTONS */}
                            <div style={{flexBasis:"12.50%"}}>
                                <div className={"card mr-0"} style={{flexBasis:"100px", minHeight:"100%", backgroundColor:"transparent", borderColor:"transparent", margin:"0", padding:"0"}}>
                                    <div className={"d-flex flex-column"} style={{gap:"0.25em"}}>

                                        {/* BUTTON ROW -- TOP */}
                                        <div className={"d-flex flex-row ml-auto mr-auto"} style={{flexShrink:"1", gap:"0.25em"}}>

                                            {/* Plot Toggle */}
                                            <button
                                                className={buttonClasses}
                                                style={styleButton}
                                                id={`plotToggle-${this.props.flightInfo.id}`}
                                                data-bs-toggle="button"
                                                onClick={() => this.plotClicked()}
                                            >
                                                <i className="fa fa-area-chart p-1"/>
                                            </button>

                                            {/* Cesium Toggle */}
                                            <button
                                                className={buttonClasses}
                                                style={styleButton}
                                                id={`cesiumToggle-${this.props.flightInfo.id}`}
                                                data-bs-toggle="button"
                                                aria-pressed={this.state.replayToggled}
                                                onClick={() => this.toggleCesiumFlight()}
                                            >
                                                <i className="fa fa-globe p-1"/>
                                            </button>

                                            {/* Map Toggle */}
                                            <button
                                                className={`${buttonClasses} ${this.state.pathVisible ? 'active' : ''}`}
                                                style={styleButton}
                                                id={`mapToggle-${this.props.flightInfo.id}`}
                                                data-bs-toggle="button"
                                                aria-pressed={this.state.pathVisible}
                                                onClick={() => this.mapClicked()}
                                                disabled={this.state.mapButtonDisabled}
                                            >
                                                <i className="fa fa-map-o p-1"/>
                                            </button>
                                        </div>

                                        {/* BUTTON ROW -- BOTTOM */}
                                        <div className={"d-flex flex-row ml-auto mr-auto"} style={{flexShrink:"1", gap:"0.25em"}}>

                                            {/* Events Toggle */}
                                            <button
                                                className={buttonClasses}
                                                data-bs-toggle="button"
                                                aria-pressed="false"
                                                style={styleButton}
                                                onClick={() => this.exclamationClicked()}
                                            >
                                                <i className="fa fa-exclamation p-1"/>
                                            </button>

                                            {/* (OUTDATED / DISABLED!) Replay Button */}
                                            <button
                                                className={`${buttonClasses} cursor-not-allowed`}
                                                style={styleButton}
                                                disabled={true}
                                                title={"The external replay system is deprecated.\nCesium flight replays can now be viewed on this page with the globe buttons."}
                                            >
                                                <i className="fa fa-video-camera p-1"/>
                                            </button>

                                            {/* Download Button */}
                                            <button
                                                className={buttonClasses}
                                                style={styleButton}
                                                type="button"
                                                id="dropdownMenu2"
                                                data-bs-toggle="dropdown"
                                                aria-haspopup="true"
                                                aria-expanded="false"
                                            >
                                                <i className="fa fa-download p-1"/>
                                            </button>

                                            {/* Labeling Tool */}
                                            <button
                                                className={`${buttonClasses} ${this.state.labelingParametersVisible ? 'active' : ''}`}
                                                style={styleButton}
                                                id={`labelingToggle-${this.props.flightInfo.id}`}
                                                title="Open labeling tool: select a parameter and mark a time section as a label"
                                                onClick={() => this.labelingClicked()}
                                            >
                                                <i className="fa fa-tag p-1"/>
                                            </button>

                                            <div className="dropdown-menu" aria-labelledby="dropdownMenu2">
                                                <button className="dropdown-item" type="button"
                                                        onClick={() => this.downloadClicked('CSV-IMP')}>
                                                    Export to CSV (Original)
                                                    <i className="ml-1 fa fa-question-circle" data-bs-toggle="tooltip"
                                                       data-bs-placement="top"
                                                       title="The NGAFID stores original CSV files from the aircraft's flight data recorder. Select this option if you wish to view this flight's original CSV file."></i>
                                                </button>
                                                <button className="dropdown-item" type="button"
                                                        onClick={() => this.downloadClicked('CSV-GEN')}>
                                                    Export to CSV (Generated)
                                                    <i className="ml-1 fa fa-question-circle" data-bs-toggle="tooltip"
                                                       data-bs-placement="top"
                                                       title="The NGAFID adds additional calculated parameters for further flight analysis, such as angle of attack. Select this option if you wish for the CSV file to contain such parameters."></i>
                                                </button>
                                                <button className="dropdown-item" type="button"
                                                        onClick={() => this.downloadClicked('KML')}>Export to KML
                                                </button>
                                                <button className="dropdown-item" type="button"
                                                        onClick={() => this.downloadClicked('XPL10')}>Export to X-Plane
                                                    10
                                                </button>
                                                <button className="dropdown-item" type="button"
                                                        onClick={() => this.downloadClicked('XPL11')}>Export to X-Plane
                                                    11
                                                </button>
                                            </div>
                                        </div>

                                    </div>

                                </div>
                            </div>

                        </div>

                    </div>

                    {/* Render all Rows */}
                    {
                        rowList.map((row, index) => {

                            //Row is not visible, skip
                            if (row === FLIGHT_COMPONENT_ROW_HIDDEN)
                                return null;

                            console.log("Rendering row: ", row);

                            return (
                                <div key={index} className="d-flex flex-row m-1 p-1" style={{
                                    overflow: "visible",
                                    width: "99%",
                                    backgroundColor: "var(--c_row_bg_alt)",
                                    borderRadius: "0.5em"
                                }}>
                                    {row}
                                </div>
                            );
                        })
                    }

                </div>
            </div>
        );
    }
}


export {Flight};

