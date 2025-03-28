// FlightComponent.tsx

/* Imports */
import 'bootstrap';
import React, { ReactElement } from "react";
import { errorModal } from "./error_modal.js";
import { MapPopup } from "./map_popup.js";
import { map, Colors } from "./map.js";
import { TileLayer } from 'ol/layer';

import {fromLonLat, toLonLat} from 'ol/proj.js';
import Overlay from 'ol/Overlay';
import {Vector as VectorSource} from 'ol/source.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';

import { Itinerary } from './itinerary_component.js';
import { TraceButtons } from './trace_buttons_component.js';
import { Tags } from './tags_component.js';
import { Events } from './events_component.js';
import { selectAircraftModal } from './select_acft_modal.js';
import { generateLOCILayer, generateStallLayer } from './map_utils.js';

// import Plotly from 'plotly.js';
import * as Plotly from 'plotly.js';
import {cesiumFlightsSelected, updateCesiumButtonState} from "./cesium_buttons.js";

import { createRoot } from 'react-dom/client';
import $ from 'jquery';


/* Declarations */
declare const global: any;

/* Components */
export interface FlightItinerary {
    startOfApproach: number;
    endOfApproach: number;
    startOfTakeoff: number;
    endOfTakeoff: number;
    airport: string;
}

export interface FlightTag {
    id?: number;
    name: string;
    description: string;
    color: string;
}

export interface FlightInfo {
    id: number; // or string, depending on your setup   [EX]
    tailNumber?: string;
    systemId?: string;
    airframeName?: string;
    startDateTime: string;
    endDateTime: string;
    itinerary: FlightItinerary[];
    tags?: FlightTag[];
    hasCoords: string; // "0" or "1"

    //... [EX]
    flightMetrics?: any;
    flightState?: any;
    flightPhases?: any;
    flightEvents?: any;
    flightPath?: any;

}

export interface FlightProps {
    flightInfo: FlightInfo;
    pageIndex: number;
    parent?: any;

    //Tag operations:
    addTag: Function;
    removeTag: Function;
    deleteTag: Function;
    getUnassociatedTags: Function;
    associateTag: Function;
    clearTags: Function;
    editTag: Function;

    //Displaying or updating the parent:
    showMap: () => void;
    showPlot: () => void;
    setAvailableLayers: (layers: any[]) => void;
    updateParentState: (flights: any[]) => void;

    tags: any;
}

export interface FlightState {
    pathVisible: boolean;
    pageIndex: number;
    mapLoaded: boolean;
    eventsLoaded: boolean;
    tagsLoaded: boolean;
    commonTraceNames: string[] | null;
    uncommonTraceNames: string[] | null;
    traceIndex: { [key: string]: number } | [];
    traceVisibility: { [key: string]: boolean } | [];
    traceNamesVisible: boolean;
    eventsVisible: boolean;
    tagsVisible: boolean;
    itineraryVisible: boolean;
    points: number[][];
    parent: any;
    selectedPlot: string | null;
    color: string;
    mapPopups: any[];              // Will hold references to popup components
    seriesData: Map<string, number[]>;

    eventsMapped: boolean[];
    eventPoints: Feature[];
    eventLayer: VectorLayer<any> | null;
    itineraryLayer: VectorLayer<any> | null;
    eventOutlines: Feature[];
    eventOutlineLayer: VectorLayer<any> | null;

    replayToggled: boolean;

    page?: number; // Added page property

    // Optional fields set at runtime:
    events?: any[];             // The flight’s events
    trackingPoint?: Feature;
    baseLayer?: VectorLayer<any>;
    coordinates?: number[][];   // Possibly the raw coordinate array
    nanOffset?: any;
    layers?: (Group | VectorLayer<any>)[];

    flightMetrics?: any;
    flightState?: any;
    flightPhases?: any;
    flightEvents?: any;
    flightPath?: any;

    tags?: any;                 // Added tags property
}




const moment = require('moment');


class Flight extends React.Component<FlightProps, FlightState> {

    constructor(props: FlightProps) {

        super(props);

        let color = Colors.randomValue();
        console.log("Flight Color: ", color);

        this.state = {
            pathVisible : false,
            pageIndex : props.pageIndex,
            mapLoaded : false,
            eventsLoaded : false,
            tagsLoaded : false,
            commonTraceNames : null,
            uncommonTraceNames : null,
            traceIndex : {},
            traceVisibility : {},
            traceNamesVisible : false,
            eventsVisible : false,
            tagsVisible : false,
            itineraryVisible : false,
            points : [],
            parent : props.parent,
            selectedPlot : null,
            color : color,
            mapPopups : [],
            seriesData : new Map(),

            eventsMapped : [],                              // Bool list to toggle event icons on map flightpath
            eventPoints : [],                               // list of event Features
            eventLayer : new VectorLayer(),
            itineraryLayer : null,
            eventOutlines : [],
            eventOutlineLayer : null,
            replayToggled: cesiumFlightsSelected.includes(this.props.flightInfo.id),

            flightMetrics : null,
        };

        this.submitXPlanePath = this.submitXPlanePath.bind(this);
        this.displayParameters = this.displayParameters.bind(this);
        this.closeParamDisplay = this.closeParamDisplay.bind(this);
        this.zoomChanged = this.zoomChanged.bind(this);
    }

    async fetchEvents() {

        const thisFlight = this;

        const submissionData = {
            flightId : this.props.flightInfo.id,
            eventDefinitionsLoaded : global.eventDefinitionsLoaded
        };

        console.log("Fetching events for flight: ", this.props.flightInfo.id);

        $.ajax({
            type: 'POST',
            url: '/protected/events',
            data : submissionData,
            dataType : 'json',
            async: false,//true,
            success : function(response) {

                console.log("fetchEvents -- Received Response:", response);

                //Event Definitions are not loaded yet, load them
                if (!global.eventDefinitionsLoaded) {
                    global.eventDefinitions = response.definitions;
                    global.eventDefinitionsLoaded = true;
                }

                const events = response.events;
                for (const event of events) {

                    for (let j = 0; j < global.eventDefinitions.length; j++) {

                        if (event.eventDefinitionId == global.eventDefinitions[j].id) {

                            event.eventDefinition = global.eventDefinitions[j];
                            console.log("Set Event Definition to: ", event.eventDefinition, " for event: ", event);

                        }

                    }

                }

                thisFlight.setState({ events: events });

            },
            error : function(jqXHR, textStatus, errorThrown) {
                thisFlight.setState({ mapLoaded: false });
                thisFlight.setState(thisFlight.state);

                errorModal.show("Error Loading Flight Events", errorThrown);
            },
        });

    }

    getActiveLayers(): TileLayer[] {

        let activeLayers:TileLayer[] = [];

        //Layers are not loaded yet, return
        if (this.state.layers == null)
            return activeLayers;

        for (const layer of this.state.layers) {

            //Target layer is visible, add to activeLayers
            if (layer.getVisible())
                activeLayers.push(layer);
            
        }

        return activeLayers;

    }

    componentWillUnmount() {

        console.log("Unmounting:", this.props.flightInfo);

        //Flight coordinate data not found, return
        if (this.props.flightInfo.hasCoords === "0")
            return;

        console.log("Hiding Flight Path");
        this.setState({
            pathVisible: false,
            itineraryVisible: false
        });

        //Hide active layers
        let layersActive = this.getActiveLayers();
        for (const layer of layersActive) {
            layer.setVisible(false);
        }

        //Event layer is loaded, hide contents
        if (this.state.eventLayer) {
            
            //Map
            this.state.eventLayer.setVisible(false);
            this.state.eventOutlineLayer.setVisible(false);

            //Plot
            let shapes = global.plotlyLayout.shapes;
            shapes.length = 0;

        }
        
        //Phases / Itinerary layer is loaded, hide it
        if (this.state.itineraryLayer)
            this.state.itineraryLayer.setVisible(false);

        console.log("Hiding plots...");

        //Trace Names are loaded, hide them
        if (this.state.commonTraceNames || this.state.uncommonTraceNames) {

            //Mark as hidden
            let visible = false;

            //Combine common and uncommon trace names
            const traceNamesCombined = (this.state.commonTraceNames || []).concat(this.state.uncommonTraceNames || []);

            for (let seriesName of traceNamesCombined) {

                //Series Name is loaded, hide it
                if (seriesName in this.state.traceIndex) {

                    /*
                        Makes this trace visible if it was formerly
                        set to visible and the visibility button was
                        clicked.
                    */
                    const visibleCurrent = (
                        visible
                        && !Array.isArray(this.state.traceVisibility)
                        && this.state.traceVisibility[seriesName]
                    );
                    const traceIndexCurrent = (
                        !Array.isArray(this.state.traceIndex)
                        ? this.state.traceIndex[seriesName]
                        : undefined
                    );

                    Plotly.restyle(
                        'plot',
                        { visible: visibleCurrent },
                        [ traceIndexCurrent ]
                    );

                }

            }

        }

        this.setState({ traceNamesVisible: false });

    }

    plotClicked() {

        //Trace names are not loaded, fetch them
        if (this.state.commonTraceNames == null) {
            var thisFlight = this;

            var submissionData = {
                flightId : this.props.flightInfo.id
            };

            $.ajax({
                type: 'POST',
                url: '/protected/double_series_names',
                data : submissionData,
                dataType : 'json',
                async: true,
                success : function(response) {

                    console.log("Received Response:", response);

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
                    var preferredNames = ["AltAGL", "AltMSL", "E1 MAP", "E2 MAP", "E1 RPM", "E2 RPM", "IAS", "NormAc", "Pitch", "Roll", "VSpd", "LOC-I Index", "Stall Index"];
                    var commonTraceNames = [];
                    var uncommonTraceNames = [];

                    for (const traceName of response.names) {

                        //Trace name has a preferred name, add to commonTraceNames
                        if (preferredNames.includes(traceName))
                            commonTraceNames.push(traceName);
                        
                        //Otherwise, add to uncommonTraceNames
                        else
                            uncommonTraceNames.push(traceName);
                        
                    }

                    //Set the trace number for this series
                    thisFlight.setState({
                        commonTraceNames: commonTraceNames,
                        uncommonTraceNames: uncommonTraceNames,
                        traceNamesVisible: true,
                    });

                },
                error : function(jqXHR, textStatus, errorThrown) {
                    this.state.commonTraceNames = null;
                    this.state.uncommonTraceNames = null;
                    errorModal.show("Error Getting Potential Plot Parameters", errorThrown);
                }
            });
        
        //Trace names are loaded, toggle visibility
        } else {

            //Toggle visibility
            let visible = !this.state.traceNamesVisible;

            //Combine common and uncommon trace names
            const traceNamesCombined = (this.state.commonTraceNames || []).concat(this.state.uncommonTraceNames || []);

            for (let seriesName of traceNamesCombined) {

                //Series Name is loaded, toggle visibility
                if (seriesName in this.state.traceIndex) {

                    /*
                        Makes this trace visible if it was formerly
                        set to visible and the visibility button was
                        clicked.
                    */
                    const visibleCurrent = (
                        visible
                        && !Array.isArray(this.state.traceVisibility)
                        && this.state.traceVisibility[seriesName]
                    );
                    const traceIndexCurrent = (
                        !Array.isArray(this.state.traceIndex)
                        ? this.state.traceIndex[seriesName]
                        : undefined
                    );

                    Plotly.restyle(
                        'plot',
                        { visible: visibleCurrent },
                        [ traceIndexCurrent ]
                    );

                }

            }

            this.setState({ traceNamesVisible: !this.state.traceNamesVisible });
            this.setState(this.state);

        }

    }

    flightColorChange(target: { state: { baseLayer: { setStyle: (arg0: any) => void; }; layers: any; }; setState: (arg0: { color: any; }) => void; }, event: { target: { value: any; }; }) {

        console.log("Trace color changed!");
        console.log(event);
        console.log(event.target);
        console.log(event.target.value);

        const stroke = new Stroke({
            color: event.target.value,
            width: 3
        });
        const style = new Style({
            stroke: stroke
        });

        target.state.baseLayer.setStyle(style);

        for (const layer of target.state.layers) {
            
            //Layer is a flight path, change color
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

        target.setState({color : event.target.value}); 

    }

    async exclamationClicked() {

        console.log ("Exclamation Clicked...");

        //Events are not loaded, fetch them
        if (!this.state.eventsLoaded) {

            console.log("...Loading events!");

            this.setState({
                eventsLoaded: true,
                eventsVisible: true,
            });

            let result = await this.fetchEvents();
            console.log("[EX] got events", result);

            let events = this.state.events || [];

            //Create list of event Features to display on map
            for (const event of events) {

                var points;
                var eventPoint;
                var eventOutline;

                //Map not loaded, create Feature with placeholder coordinates
                if (!this.state.mapLoaded){              //<-- Points/Coordinates have not been fetched

                    const linePlaceholder = new LineString( [0,0] );

                    //Create eventPoint with placeholder coordinates
                    eventPoint = new Feature({
                        geometry: linePlaceholder,
                        name: 'Event'
                    });

                    //Create Outlines
                    eventOutline = new Feature({
                        geometry: linePlaceholder,
                        name: 'EventOutline'
                    });

                //Map loaded, create Feature with fetched coordinates
                } else {

                    const line = new LineString( this.state.points.slice(event.startLine, event.endLine + 2) );

                    //Create eventPoint with preloaded coordinates
                    points = this.state.points;
                    eventPoint = new Feature({
                        geometry: line,
                        name: 'Event'
                    });

                    //Create outlines
                    eventOutline = new Feature({
                        geometry: line,
                        name: 'EventOutline'
                    });

                }

                //Add eventPoint to Flight
                this.state.eventsMapped.push(false);
                this.state.eventPoints.push(eventPoint);
                this.state.eventOutlines.push(eventOutline);

            }

            //Create eventLayer & add eventPoints
            const eventVectorLayer = new VectorLayer({
                
                style: new Style({
                    stroke: new Stroke({
                        color: [0,0,0,0],
                        width: 3
                    })
                }),

                source : new VectorSource({
                    features: this.state.eventPoints
                })

            });

            //Create outline eventLayer & add eventPoints
            const eventOutlineLayer = new VectorLayer({
                
                style: new Style({
                    stroke: new Stroke({
                        color: [0,0,0,0],
                        width: 4
                    })
                }),

                source : new VectorSource({
                    features: this.state.eventOutlines
                })

            });

            this.setState({
                eventOutlineLayer: eventOutlineLayer,
                eventLayer: eventVectorLayer,
            });

            eventOutlineLayer.setVisible(true);
            eventVectorLayer.setVisible(true);

            //flightPath loaded, add eventLayer to map
            if (this.state.mapLoaded){
                map.addLayer(eventOutlineLayer);
                map.addLayer(eventVectorLayer);
            }

            this.setState(this.state);

        //Events are loaded, toggle visibility
        } else {

            console.log("...Events have already loaded!");

            //Toggle visibility if already loaded
            this.setState({ eventsVisible: !this.state.eventsVisible });
            this.state.eventLayer.setVisible(this.state.eventsVisible);
            this.state.eventOutlineLayer.setVisible(this.state.eventsVisible);

            //Events are visible, clear Plotly
            if(!this.state.eventsVisible) {
                console.log("Clearing Plotly");
                global.plotlyLayout.shapes = [];
                Plotly.relayout('plot', global.plotlyLayout);
            }

            console.log(global.plotlyLayout);
            this.setState(this.state);

        }

    }

    /**
     * Handles when the download button was clicked
     * @param type which type of download (xplane, csv etc)
     */
    downloadClicked(type: string) {

        console.log("Download Type Clicked: ", type);

        switch (type) {

            case 'KML':
                window.open(`/protected/get_kml?flight_id=${this.props.flightInfo.id}`);
                break;

            case 'XPL10':
                selectAircraftModal.show('10', this.submitXPlanePath, this.props.flightInfo.id, false);
                break;

            case 'XPL11':
                selectAircraftModal.show('11', this.submitXPlanePath, this.props.flightInfo.id, false);
                break;

            case 'CSV-IMP':
                window.open(`/protected/get_csv?flight_id=${this.props.flightInfo.id}&generated=false`);
                break;

            case 'CSV-GEN':
                window.open(`/protected/get_csv?flight_id=${this.props.flightInfo.id}&generated=true`);
                break;

            default:
                window.open(`/protected/get_kml?flight_id=${this.props.flightInfo.id}`);

        }

    }

    /**
     * Gets the aircraft path from the submit aircraft modal
     * @param type the xplane version
     * @param path the selected path
     * @param flightId the flightId
     **/
    submitXPlanePath(type: any, path: any, useMSL: any){

        console.log(`Submitting the xplane path to server: ${type} ${path} ${useMSL}`);
        console.log(this.props);

        const xplaneURL = `/protected/get_xplane?flight_id=${this.props.flightInfo.id}&version=${type}&acft_path=${path}&use_msl=${useMSL}`;
        window.open(xplaneURL);

    }

    cesiumClicked() {

        let flightStoreIndex = cesiumFlightsSelected.indexOf(this.props.flightInfo.id);

        //If the flight is not in the store, add it
        if (flightStoreIndex === -1)
            cesiumFlightsSelected.push(this.props.flightInfo.id)

        //Otherwise, remove it
        else
            cesiumFlightsSelected.splice(flightStoreIndex, 1);

        updateCesiumButtonState();
        console.log(cesiumFlightsSelected);

    }

    replayClicked() {

        let replayURL = `/protected/ngafid_cesium?flight_id=${this.props.flightInfo.id}`;
        window.open(replayURL);

    }

    closeParamDisplay() {

        console.log("Popup Closed!");

    }

    zoomChanged(oldZoom: string) {

        let currZoom = map.getView().getZoom();
        console.log("Old Zoom: " + oldZoom);
        console.log("Current Zoom: " + currZoom);

        for(let i = 0; i < this.state.mapPopups.length; i++) {
            this.state.mapPopups[i].close();
        }

    }

    displayParameters(event: { pixel: any; }){

        const pixel = event.pixel;
        const features: any[] = [];

        //Get the features at the pixel
        map.forEachFeatureAtPixel(pixel, function(feature: any, layer: any) {
            features.push(feature)
        });

        let target = features[0];
        console.log("Populating new popup for metrics");

        //If the target is an event, set the target to the line segment
        if (target.get('name') === 'Event' && features[2] != null)
            target = features[2];

        console.log(this.state.events);

        const lociInfo = new Array();
        let info = null;
        let precision = 0;

        //Check if the target is a point on the flight path
        const doParamRender = (
            (target != null)
            && (target.parent === "LOC-I Index" || target.parent === "Stall Index")
        );

        //If the target is a point on the flight path, render the popup
        if (doParamRender) {

            const index = target.getId();
            console.log("Target Info and Index: ", target, index);

            console.log(this.state.flightMetrics);
            const submissionData = {
                flightId : this.props.flightInfo.id,
                timeIndex : index
            };

            lociInfo.push(index);

            const spData = this.state.seriesData.get('Stall Index');
            const lociData = this.state.seriesData.get('LOC-I Index');

            //Flight does not have Stall Index data, throw error
            if (spData == null)
                throw new Error("Flight has no Stall Index data");

            lociInfo.push(spData[index]); //<-- All flights should have SI data

            //Flight does not have LOC-I data, push null
            if (lociData == null)
                lociInfo.push(null);

            //Otherwise, push the LOC-I data
            else 
                lociInfo.push(lociData[index]);

            $.ajax({
                type: 'POST',
                url: '/protected/loci_metrics',
                data : submissionData,
                dataType : 'json',
                async: false,
                success : function(response) {

                    console.log("Got loci_metrics response: ", response);
                    info = response.values;
                    precision = response.precision;

                },
                error : function(jqXHR, textStatus, errorThrown) {

                    console.log("Error getting upset data:");
                    console.log(errorThrown);

                },
            });  
            
            const popupProps = {
                pixel : pixel,
                status : '',
                events : this.state.events,
                info : info,
                lociData : lociInfo,
                placement : pixel,
                precision : precision,
                lineSeg : target,
                closePopup : this.closeParamDisplay(),
                title : 'title'
            };

            //Render the popup
            this.renderNewPopup(this.state.mapPopups.length - 1, popupProps);

        } else {

            console.log("Won't render popup");

        }

    }

    /**
     * Recursively find a vacant (unpinned) popup or create a new one
     */
    renderNewPopup(index: number, props: any): HTMLElement | null | void {

        //Index is out of bounds, create a new popup
        if (index < 0 || this.state.mapPopups[index] == null) {

            //If we reach the bottom of the stack, we must allocate memory for a new popup component
            const outterHTM = document.createElement('div');
            document.body.appendChild(outterHTM);
            const root = createRoot(outterHTM);
            const popup = root.render(React.createElement(MapPopup, props));
            outterHTM.setAttribute("id", "popover" + this.state.mapPopups.length);
            this.state.mapPopups.push(popup);

            return popup;

        //Index is in bounds, check if the popup is pinned
        } else if (this.state.mapPopups[index].isPinned()) {

            //Skip reallocating an existing popup if it is pinned
            return this.renderNewPopup(index - 1, props);

        //Index is in bounds, return the popup
        } else {

            console.log("Using existing popup to render!");
            let element = "popover" + index;
            const container = document.getElementById(element);
            const root = createRoot(container!);
            root.render(React.createElement(MapPopup, props));
            const popupElement = document.getElementById("popover" + index);

            //Popup is not null, set display to block
            if (popupElement)
                popupElement.style.display = "block";

            return popupElement;

        }

    }

    tagClicked() {
        this.setState({ tagsVisible: !this.state.tagsVisible });
        this.setState(this.state);
    }

    mapClicked() {

        //Flight coordinate data not found, return
        if (this.props.flightInfo.hasCoords === "0")
            return;

        //Map is not loaded, load it
        if (!this.state.mapLoaded) {

            this.props.showMap();
            this.setState({ mapLoaded: true });

            let thisFlight = this;
            
            this.fetchEvents();
            console.log("Events Fetched: ", this.state.events);

            /*
                var lociSubmissionData = {
                    seriesName : "LOC-I Index",
                    flightId : this.props.flightInfo.id
                };
            */

            //TODO: get upset probability data here

            console.log("Getting upset probabilities...");

            const DOUBLE_SERIES_NAMES = [
                "Stall Index",
                "LOC-I Index",
            ];
            for (const name of DOUBLE_SERIES_NAMES) {

                console.log(name);

                const submissionData = {
                    seriesName : name,
                    flightId : this.props.flightInfo.id
                };

                $.ajax({
                    type: 'POST',
                    url: '/protected/double_series',
                    data : submissionData,
                    dataType : 'json',
                    async: false,
                    success : function(response) {
                        console.log("Got double_series response: ", thisFlight.state.seriesData);
                        thisFlight.state.seriesData.set(name, response.y);
                    },
                    error : function(jqXHR, textStatus, errorThrown) {
                        console.log("Error getting upset data:", errorThrown);
                    },
                });

            }

            /*
                TODO:

                Check if the actual user_id and id_token
                values are suppoed to be used here
            */
            const TEST_USER_ID = 1;
            const submissionData = {
                request : "GET_COORDINATES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id,
                user_id : TEST_USER_ID,
                flightId : this.props.flightInfo.id,
            };

            $.ajax({
                type: 'POST',
                url: '/protected/coordinates',
                data : submissionData,
                dataType : 'json',
                async: true,
                success : function(response) {

                    console.log("Received Response: ", response);

                    const coordinates = response.coordinates;

                    const points = thisFlight.state.points;
                    for (const coord of coordinates) {
                        const point = fromLonLat(coord);
                        points.push(point);
                    }

                    const color = thisFlight.state.color;
                    const trackingPoint = new Feature({
                        geometry : new Point(points[0]),
                        name: 'TrackingPoint'
                    });

                    trackingPoint.setId(points[0]);

                    thisFlight.setState({
                        layers: new Array(),
                        trackingPoint: trackingPoint
                    });

                    const layers = thisFlight.state.layers || [];

                    //Adding itinerary (approaches and takeoffs) to flightpath 
                    const itinerary = thisFlight.props.flightInfo.itinerary;
                    const flightPhases = [];

                    //Create flight phase styles
                    const takeoffStyle = new Style({
                        stroke: new Stroke({
                            color: "#34eb52",
                            width: 3
                        })
                    });

                    const approachStyle = new Style({
                        stroke: new Stroke({
                            color: "#347deb",
                            width: 3
                        })
                    });

                    //Create and add Features to flightPhases for each flight phase in itinerary
                    for (let i = 0; i < itinerary.length; i++) {

                        var stop = itinerary[i];
                        var approach = null;
                        var takeoff = null;

                        //Approach phase has a start and end, create a LineString
                        if (stop.startOfApproach != -1 && stop.endOfApproach != -1) {
                            const approachPoints = points.slice(stop.startOfApproach, stop.endOfApproach);
                            approach = new LineString(approachPoints);
                        }
                    
                        //Takeoff phase has a start and end, create a LineString
                        if (stop.startOfTakeoff != -1 && stop.endOfTakeoff != -1) {
                            const takeoffPoints = points.slice(stop.startOfTakeoff, stop.endOfTakeoff);
                            takeoff = new LineString(takeoffPoints);
                        }

                        //Appraoch phase exists, add to flightPhases
                        if (approach != null) {

                            const phase = new Feature({
                                geometry: approach,
                                name: 'Approach'
                            });
                            phase.setStyle(approachStyle);
                            flightPhases.push( phase );

                        }

                        //Takeoff phase exists, add to flightPhases
                        if (takeoff != null) {

                            const phase = new Feature({
                                geometry: takeoff,
                                name: 'Takeoff'
                            });
                            phase.setStyle(takeoffStyle);
                            flightPhases.push( phase );

                        }

                    }

                    console.log("[EX] Tracking Point: ", trackingPoint);

                    const baseLayer = new VectorLayer({
                        flightState : thisFlight,
                        name : 'Itinerary' ,
                        description : 'Itinerary with Phases',
                        nMap : false,
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
                        source : new VectorSource({
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
                        name : 'Itinerary Phases',
                        nMap : true,
                        style: new Style({
                            stroke: new Stroke({
                                color: [1,1,1,1],
                                width: 3
                            })
                        }),
                        source : new VectorSource({
                            features: flightPhases
                        })
                    }); 

                    thisFlight.setState({
                        pathVisible: true,
                        itineraryVisible: true,
                        nanOffset: response.nanOffset,
                        coordinates: response.coordinates,
                        points: points,
                        baseLayer: baseLayer,
                        layers: [baseLayer, phaseLayer],
                        trackingPoint: trackingPoint
                    });

                
                    //Toggle visibility of itinerary
                    layers.push(baseLayer, phaseLayer);
                    
                    const lociData = thisFlight.state.seriesData.get('LOC-I Index');
                    const spData = thisFlight.state.seriesData.get('Stall Index');

                    console.log("Loc-I Data: ", lociData);
                    console.log("Stall Data: ", spData);
                    console.log("Layers: ", layers);
                    console.log("Flight: ", thisFlight);

                    generateLOCILayer(lociData, layers, thisFlight, trackingPoint);
                    generateStallLayer(spData, layers, thisFlight, trackingPoint);
                    
                    console.log("Adding layers...");
                    for (const layer of layers) {

                        console.log("Layer: ", layer);

                        //Layer is an itinerary, set as default and visible
                        if (layer.get('name').includes('Itinerary')) {

                            //Itinerary will be the default layer
                            thisFlight.setState({ selectedPlot: layer.values_.name });
                            layer.setVisible(true);

                        //Layer is not an itinerary, set as invisible
                        } else {

                            layer.setVisible(false);

                        }

                        //Add layer to map
                        map.addLayer(layer);
                        
                    }

                    console.log(layers);
                    thisFlight.props.setAvailableLayers(layers);

                    console.log("Added Layers: ", map.getLayers());
                    map.on('click', thisFlight.displayParameters); 

                    const currZoom = map.getView().getZoom();
                    map.on('moveend', () => thisFlight.zoomChanged(currZoom));
                    
                    //Adding coordinates to events (if needed)
                    let events = [];
                    let eventPoints = [];
                    let eventOutlines = [];
                    if (thisFlight.state.eventsLoaded) {

                        events = thisFlight.state.events || [];
                        eventPoints = thisFlight.state.eventPoints;
                        eventOutlines = thisFlight.state.eventOutlines;
                        for (let i = 0; i < events.length; i++){

                            let line = new LineString(points.slice(events[i].startLine -1, events[i].endLine + 1));
                            eventPoints[i].setGeometry(line);       //<-- Set geometry of eventPoint Features
                            eventOutlines[i].setGeometry(line);

                        }

                        //Add eventLayer to front of map
                        const eventLayer = thisFlight.state.eventLayer;
                        const outlineLayer = thisFlight.state.eventOutlineLayer;
                        map.addLayer(outlineLayer);
                        map.addLayer(eventLayer);

                    }

                    const extent = baseLayer.getSource().getExtent();
                    console.log("Extent:", extent);
                    map.getView().fit(extent, map.getSize());

                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    
                    thisFlight.setState({ mapLoaded: false });
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Coordinates", errorThrown);

                },
            });

        
        //Map is loaded, toggle visibility
        } else {

            // this.state.pathVisible = !this.state.pathVisible;
            // this.state.itineraryVisible = !this.state.itineraryVisible;

            this.setState({
                pathVisible: !this.state.pathVisible,
                itineraryVisible: !this.state.itineraryVisible
            });

            console.log("Already Rendered: ", this.state.layers);

            const layersCurrent = this.state.layers || [];
            for (const layer of layersCurrent) {

                console.log("Layer: ", layer);

                //Layer values are visible but path is not, hide layer
                if (layer.values_.visible && !this.state.pathVisible) {

                    this.setState({ selectedPlot: layer.values_.name });
                    layer.setVisible(false);

                //Layer values are visible and path is visible, show layer
                } else if (layer.values_.name === this.state.selectedPlot && this.state.pathVisible) {

                    layer.setVisible(true);

                }

            }

            //Event layer is loaded, toggle visibility
            if (this.state.eventLayer != null) {
                this.state.eventLayer.setVisible(!this.state.eventLayer.getVisible());
                this.state.eventOutlineLayer.setVisible(!this.state.eventOutlineLayer.getVisible());
            }

            //Path is visible, fit map to extent
            if (this.state.pathVisible)
                this.props.showMap();

            this.setState(this.state);

            //Path is visible, fit map to extent
            if (this.state.pathVisible) {

                let extent = this.state.baseLayer.getSource().getExtent();
                console.log(extent);
                map.getView().fit(extent, map.getSize());

            }

        }

    }

    /**
     * Changes all the flights on a given page by calling the parent function
     */
    updateFlights(flights: any[]){
        this.props.updateParentState(flights);
    }

    /**
     * Changes the tags associated with this flight
     */
    invokeUpdate(tags: any){
        this.setState({ tags });
    }

    /**
     * Called when props are updated
     * changes state if props have in fact changed
     * @param oldProps the old props before the update
     */
    componentDidUpdate(oldProps: { tags: any; }) {

        console.log("Props Updated");
        const newProps = this.props;

        //Tags have changed, update state
        if(oldProps.tags !== newProps.tags)
            this.setState({ tags: this.props.tags });

    }

    render() {

        const buttonClasses = "p-1 expand-import-button btn btn-outline-secondary d-flex align-items-center justify-content-center";
        //const lastButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        const firstCellClasses = "p-1 card mr-1"
        const cellClasses = "p-1 card mr-1"

        const buttonSize = "1.75em";
        const styleButton = { minWidth:buttonSize, minHeight:buttonSize, width:buttonSize, height:buttonSize, display:"inlineBlock", justifyContent:"center", alignContent:"center", textAlign:"center" };
        const styleEmptyCell = {fontStyle:"italic", fontSize:"0.75em", opacity:"0.50", userSelect:"none"};

        const flightInfo = this.props.flightInfo;

        const startTime = moment(flightInfo.startDateTime);
        const endTime = moment(flightInfo.endDateTime);

        let globeClasses = "";
        let globeTooltip = "";
        let traceDisabled = false;

        const tagTooltip = "Click to tag a flight for future queries and grouping";

        //Flight coordinate data not found, disable globe
        if (!flightInfo.hasCoords) {

            console.log(`Failed to find coords for Flight: ${flightInfo.id}!`);

            globeClasses += " disabled";
            globeTooltip = "Cannot display flight on the map because the flight data did not have latitude/longitude.";
            traceDisabled = true;

        //Flight coordinate data found, enable globe
        } else {

            globeTooltip = "Click the globe to display the flight on the map.";

        }

        const visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {

            //Airport has not been visited, add to visitedAirports
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0)
                visitedAirports.push(flightInfo.itinerary[i].airport);
            
        }

        //Visited Airports are loaded, display them
        let visitedAirportsRow:string | ReactElement = "";
        if (visitedAirports.length > 0)
            visitedAirportsRow = visitedAirports.join(", ");
        
        //Visited Airports are not loaded, display placeholder
        else
            visitedAirportsRow = <div style={styleEmptyCell}>No Airports...</div>

        //Events are loaded, display them
        let eventsRow:string | ReactElement = "";
        if (this.state.eventsVisible) {

            console.log("Creating Events row with events: ", this.state.events);
            eventsRow = (<Events events={this.state.events} parent={this}/>);

        }
        
        //Tags are loaded, display them
        let tagsRow:string | ReactElement = "";
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

        //Itinerary is loaded, display it
        let itineraryRow:string | ReactElement = "";
        if (this.state.itineraryVisible) {

            itineraryRow = (
                <Itinerary
                    showMap={() => {this.props.showMap();}}
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

        //Traces are loaded, display them
        let tracesRow:string | ReactElement = "";
        if (this.state.traceNamesVisible) {

            tracesRow = (
                <TraceButtons
                    showPlot={() => {this.props.showPlot();}}
                    parentFlight={this}
                    flightId={flightInfo.id}
                />
            );

        }

        //Tags are loaded, display them
        let tagPills:string | ReactElement | ReactElement[] = "";
        if (this.props.flightInfo.tags != null && this.props.flightInfo.tags.length > 0) {

            tagPills = (

                this.props.flightInfo.tags.map((tag, index) => {
                    let style = {
                        backgroundColor : tag.color,
                        marginRight : '4px',
                        lineHeight : '2',
                        opacity : '75%'
                    }
                    return(
                        <span key={index} className="badge badge-primary" style={{lineHeight : '1.5', marginRight : '4px', backgroundColor : 'var(--c_tag_badge)', color : 'var(--c_text)'}} title={tag.description}>
                            <span className="badge badge-pill badge-primary" style={style} data-page={this.state.page}>
                                <i className="fa fa-tag" aria-hidden="true"/>
                            </span>
                            {tag.name}
                        </span>
                    );
                })

            );

        //Tags are not loaded, display placeholder
        } else {
            tagPills = <div style={styleEmptyCell}>No Tags...</div>
        }

        //Tooltip Initialization
        $(function () {
            //@ts-ignore
            $('[data-bs-toggle="tooltip"]').tooltip() 
        })

        console.log("Tail Number: " + flightInfo.tailNumber);

        return (
            <div className="card mb-1" style={{backgroundColor:"var(--c_entry_bg)"}}>
                <div className="">
                    <div className="d-flex flex-column">

                        <div className="d-flex flex-row p-1">

                            {/* FLIGHT INFO */}
                            <div style={{ flexBasis: "32.5%", whiteSpace: "nowrap" }}>
                                <div className={`${firstCellClasses} d-flex flex-row`} style={{ height: "100%" }}>
                                    <div className="d-flex flex-column" style={{ alignItems: "start" }}>
                                        <a href={'/protected/flight?flight_id=' + flightInfo.id}>
                                            <i className="p-1 fa fa-plane">
                                                &nbsp;{flightInfo.id}
                                            </i>
                                        </a>
                                        <div>
                                            ◦&nbsp;
                                            {
                                                (flightInfo.tailNumber!=null && flightInfo.tailNumber!="")
                                                ? <a>{flightInfo.tailNumber}</a>
                                                : <a style={styleEmptyCell}>No Tail Number...</a>
                                            }
                                        </div>
                                    </div>

                                    <div className="d-flex flex-column ml-3" style={{ alignItems: "start" }}>
                                        <div>
                                            ◦&nbsp;
                                            {
                                                (flightInfo.systemId!=null && flightInfo.systemId!="")
                                                ? <a>{flightInfo.systemId}</a>
                                                : <a style={styleEmptyCell}>No System ID...</a>
                                            }
                                        </div>
                                        <div>
                                            ◦&nbsp;
                                            {
                                                (flightInfo.airframeName!=null && flightInfo.airframeName!="")
                                                ? <a>{flightInfo.airframeName}</a>
                                                : <a style={styleEmptyCell}>No Airframe Name...</a>
                                            }
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* START - END DATES */}
                            <div style={{ flexBasis: "32.5%", whiteSpace: "nowrap" }}>
                                <div className={`${cellClasses} d-flex flex-row`} style={{ height: "100%" }}>
                                    <div className="d-flex flex-column" style={{ alignItems: "center" }}>
                                        <div>
                                            ◦ {flightInfo.startDateTime}
                                        </div>
                                        <div>
                                            ◦ {flightInfo.endDateTime}
                                        </div>
                                    </div>

                                    <div className="d-flex flex-column ml-3" style={{ alignItems: "center" }}>
                                        <div>
                                            ◦ {moment.utc(endTime.diff(startTime)).format("HH:mm:ss")}
                                        </div>
                                        <div style={{ visibility: "hidden" }}>
                                            &emsp;
                                        </div>
                                    </div>
                                </div>
                            </div>

                            {/* AIRPORTS */}
                            <div className={cellClasses} style={{flexBasis:"12.50%"}}>
                                {visitedAirportsRow}
                            </div>

                            {/* TAGS */}
                            <div className={cellClasses} style={{flexBasis:"22.50%"}}>
                                <div style={{overflow:"hidden"}}>

                                    <div style={{ position: "absolute", top: "1", right: "1", zIndex:"1", scale:"0.75"}} onClick={() => this.tagClicked()}>
                                        <button className={"p-1 btn btn-outline-secondary d-flex align-items-center justify-content-center"} data-bs-toggle="button" title={tagTooltip} aria-pressed="false" style={{...styleButton, border:"none"}}>
                                            <i className="p-1 fa fa-plus"/>
                                        </button>
                                    </div>

                                    {tagPills}
                                    
                                </div>
                            </div>


                            {/* BUTTONS */}
                            <div style={{flexBasis:"12.50%"}}>
                                <div className={"card mr-0"} style={{flexBasis:"100px", minHeight:"100%", backgroundColor:"transparent", borderColor:"transparent", margin:"0", padding:"0"}}>
                                    <div className={"d-flex flex-column"} style={{gap:"0.25em"}}>

                                        <div className={"d-flex flex-row ml-auto mr-auto"} style={{flexShrink:"1", gap:"0.25em"}}>
                                            <button className={buttonClasses} data-bs-toggle="button" aria-pressed="false" style={styleButton} onClick={() => this.exclamationClicked()}>
                                                <i className="p-1 fa fa-exclamation"></i>
                                            </button>

                                            <button className={buttonClasses} style={styleButton} data-bs-toggle="button" aria-pressed="false" onClick={() => this.plotClicked()}>
                                                <i className="p-1 fa fa-area-chart"></i>
                                            </button>

                                            <button className={buttonClasses} style={styleButton} data-bs-toggle="button" aria-pressed="false" onClick={() => this.mapClicked()}>
                                                <i className="p-1 fa fa-map-o"></i>
                                            </button>
                                        </div>

                                        <div className={"d-flex flex-row ml-auto mr-auto"} style={{flexShrink:"1", gap:"0.25em"}}>
                                            <button className={buttonClasses + globeClasses} style={styleButton} title={globeTooltip} id={"cesiumToggled" + this.props.flightInfo.id} data-bs-toggle="button" aria-pressed={this.state.replayToggled} onClick={() => this.cesiumClicked()}>
                                                <i className="p-1 fa fa-globe"></i>
                                            </button>

                                            <button className={buttonClasses} style={styleButton} onClick={() => this.replayClicked()}>
                                                <i className="p-1 fa fa-video-camera"></i>
                                            </button>

                                            <button className={buttonClasses} style={styleButton} type="button" id="dropdownMenu2" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                                <i className="p-1 fa fa-download"></i>
                                            </button>

                                            <div className="dropdown-menu" aria-labelledby="dropdownMenu2">
                                                <button className="dropdown-item" type="button" onClick={() => this.downloadClicked('CSV-IMP')}>
                                                    Export to CSV (Original)
                                                    <i className="fa fa-question-circle ml-1" data-bs-toggle="tooltip" data-bs-placement="top" title="The NGAFID stores original CSV files from the aircraft's flight data recorder. Select this option if you wish to view this flight's original CSV file."></i>
                                                </button>
                                                <button className="dropdown-item" type="button" onClick={() => this.downloadClicked('CSV-GEN')}>
                                                    Export to CSV (Generated)
                                                    <i className="fa fa-question-circle ml-1" data-bs-toggle="tooltip" data-bs-placement="top" title="The NGAFID adds additional calculated parameters for further flight analysis, such as angle of attack. Select this option if you wish for the CSV file to contain such parameters."></i>
                                                </button>
                                                <button className="dropdown-item" type="button" onClick={() => this.downloadClicked('KML')}>Export to KML</button>
                                                <button className="dropdown-item" type="button" onClick={() => this.downloadClicked('XPL10')}>Export to X-Plane 10</button>
                                                <button className="dropdown-item" type="button" onClick={() => this.downloadClicked('XPL11')}>Export to X-Plane 11</button>
                                            </div>
                                        </div>

                                    </div>

                                </div>
                            </div>

                        </div>

                    </div>

                    {tagsRow}
                    {eventsRow}
                    {tracesRow}
                    {itineraryRow}

                </div>
            </div>
        );
    }
}

export { Flight } 