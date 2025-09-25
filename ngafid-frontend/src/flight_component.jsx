import 'bootstrap';
import React from "react";
import { createRoot } from "react-dom/client";
import {showErrorModal} from "./error_modal";
import {MapPopup} from "./map_popup";

import {Colors, map} from "./map";

import {fromLonLat} from 'ol/proj';
import {Vector as VectorSource} from 'ol/source';
import {Vector as VectorLayer} from 'ol/layer';
import {Circle, Stroke, Style} from 'ol/style';
import Feature from 'ol/Feature';
import LineString from 'ol/geom/LineString';
import Point from 'ol/geom/Point';

import {Itinerary} from './itinerary_component';
import {TraceButtons} from './trace_buttons_component';
import {Tags} from './tags_component';
import {Events, eventDefinitions} from './events_component';
import {showSelectAircraftModal} from './select_acft_modal';
import {generateLOCILayer, generateStallLayer} from './map_utils';

import Plotly from 'plotly.js';
import {cesiumFlightsSelected} from "./cesium_buttons";

import { plotlyLayoutGlobal } from './flights';

import moment from 'moment';
import { rejects } from 'assert';


class Flight extends React.Component {

    constructor(props) {

        super(props);

        const color = Colors.randomValue();
        console.log("Flight color: ", color);

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
        };

        this.submitXPlanePath = this.submitXPlanePath.bind(this);
        this.displayParameters = this.displayParameters.bind(this);
        this.closeParamDisplay = this.closeParamDisplay.bind(this);
        this.zoomChanged = this.zoomChanged.bind(this);
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

                        //console.log(name);
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

    async mapClicked() {

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
                                new Feature({
                                    geometry: new LineString(points),
                                    name: 'Line'
                                }),
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
                        if (layer.get('name').includes('Itinerary')) {
                            //Itinerary will be the default layer
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

                    const currZoom = map.getView().getZoom();
                    map.on('moveend', () => this.zoomChanged(currZoom));
                    // adding coordinates to events, if needed //
                    let events = [];
                    let eventPoints = [];
                    let eventOutlines = [];
                    if (this.state.eventsLoaded) {
                        events = this.state.events;
                        eventPoints = this.state.eventPoints;
                        eventOutlines = this.state.eventOutlines;
                        for (let i = 0; i < events.length; i++) {
                            const line = new LineString(points.slice(events[i].startLine - 1, events[i].endLine + 1));
                            eventPoints[i].setGeometry(line);                   // set geometry of eventPoint Features
                            eventOutlines[i].setGeometry(line);
                        }

                        // add eventLayer to front of map
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
                        selectedPlot: 'Itinerary',
                        pathVisible: true,
                        itineraryVisible: true
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
                                    overflowX: "hidden",
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

