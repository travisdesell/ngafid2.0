import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import { errorModal } from "./error_modal.js";
import { MapPopup } from "./map_popup.js";

import { map, styles, layers, Colors } from "./map.js";

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
import {generateLOCILayer, generateStallLayer} from './map_utils.js';

import Plotly from 'plotly.js';
import {cesiumFlightsSelected, updateCesiumButtonState} from "./cesium_buttons";

var moment = require('moment');


class Flight extends React.Component {
    constructor(props) {
        super(props);

        let color = Colors.randomValue();
        console.log("flight color: " );
        console.log(color);

        this.state = {

            filterAddButtonHovered : false,
            pathVisible : false,
            pageIndex : props.pageIndex,
            mapLoaded : false,
            eventsLoaded : false,
            tagsLoaded : false,
            commonTraceNames : null,
            uncommonTraceNames : null,
            traceIndex : [],
            traceVisibility : [],
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
            eventLayer : null,
            itineraryLayer : null,
            eventOutlines : [],
            eventOutlineLayer : null,
            replayToggled: cesiumFlightsSelected.includes(this.props.flightInfo.id),
            cesiumFlightEnabled: false,
        }

        this.submitXPlanePath = this.submitXPlanePath.bind(this);
        this.displayParameters = this.displayParameters.bind(this);
        this.closeParamDisplay = this.closeParamDisplay.bind(this);
        this.zoomChanged = this.zoomChanged.bind(this);
    }

    fetchEvents() {
        var thisFlight = this;

        var submissionData = {
            flightId : this.props.flightInfo.id,
            eventDefinitionsLoaded : global.eventDefinitionsLoaded
        };

        $.ajax({
            type: 'POST',
            url: '/protected/events',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response events data :");
                console.log("received response events data :");
                console.log(response);

                if (!global.eventDefinitionsLoaded) {
                    global.eventDefinitions = response.definitions;
                    global.eventDefinitionsLoaded = true;
                }

                var events = response.events;
                for (let i = 0; i < events.length; i++) {
                    for (let j = 0; j < global.eventDefinitions.length; j++) {

                        if (events[i].eventDefinitionId == global.eventDefinitions[j].id) {
                            events[i].eventDefinition = global.eventDefinitions[j];
                            console.log("set events[" + i + "].eventDefinition to:");
                            console.log(events[i].eventDefinition);
                        }
                    }
                }

                thisFlight.state.events = events;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                thisFlight.state.mapLoaded = false;
                thisFlight.setState(thisFlight.state);

                errorModal.show("Error Loading Flight Events", errorThrown);
            },
            async: false
        });
    }

    getActiveLayers() {
        let activeLayers = [];
        if (this.state.layers != null) {
            for (var i = 0; i < this.state.layers.length; i++) {
                let layer = this.state.layers[i];
                if (layer.getVisible()) {
                    activeLayers.push(layer);
                }
            }
        }
    }

    componentWillUnmount() {
        console.log("unmounting:");
        console.log(this.props.flightInfo);

        if (this.props.flightInfo.has_coords === "0") return;

        console.log("hiding flight path");
        this.state.pathVisible = false;
        this.state.itineraryVisible = false;
        if (this.getActiveLayers()) {
            for (var layer in this.getActiveLayers()) {
                layer.setVisible(false);
            }
        }

        // hiding events
        if (this.state.eventLayer) {
            // map
            this.state.eventLayer.setVisible(false);
            this.state.eventOutlineLayer.setVisible(false);

            // plot
            let shapes = global.plotlyLayout.shapes;
            shapes.length = 0;
        }
        
        // hiding phases
        if (this.state.itineraryLayer) {
            this.state.itineraryLayer.setVisible(false);
        }


        console.log("hiding plots");
        if (this.state.commonTraceNames) {
            let visible = false;

            for (let i = 0; i < this.state.commonTraceNames.length; i++) {
                let seriesName = this.state.commonTraceNames[i];

                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }

            for (let i = 0; i < this.state.uncommonTraceNames.length; i++) {
                let seriesName = this.state.uncommonTraceNames[i];

                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }
        }
        this.state.traceNamesVisible = false;
    }

    plotClicked() {
        
        //if (this.state.commonTraceNames == null) {
        if (!this.state.traceNamesVisible) {

            this.props.showPlot();
            var thisFlight = this;

            var submissionData = {
                flightId : this.props.flightInfo.id
            };

            $.ajax({
                type: 'POST',
                url: '/protected/double_series_names',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response double series name : ");
                    console.log("received response double series name : ");
                    console.log(response);

                    var names = response.names;

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

                    for (let i = 0; i < response.names.length; i++) {
                        let name = response.names[i];

                        //console.log(name);
                        if (preferredNames.includes(name)) {
                            commonTraceNames.push(name);
                        } else {
                            uncommonTraceNames.push(name);
                        }
                    }

                    //set the trace number for this series
                    thisFlight.state.commonTraceNames = commonTraceNames;
                    thisFlight.state.uncommonTraceNames = uncommonTraceNames;
                    thisFlight.state.traceNamesVisible = true;
                    thisFlight.setState(thisFlight.state);
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    this.state.commonTraceNames = null;
                    this.state.uncommonTraceNames = null;
                    errorModal.show("Error Getting Potential Plot Parameters", errorThrown);
                },
                async: true
            });
        } else {
            let visible = !this.state.traceNamesVisible;

            for (let i = 0; i < this.state.commonTraceNames.length; i++) {
                let seriesName = this.state.commonTraceNames[i];

                //check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }

            for (let i = 0; i < this.state.uncommonTraceNames.length; i++) {
                let seriesName = this.state.uncommonTraceNames[i];

                //check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }


            this.state.traceNamesVisible = !this.state.traceNamesVisible;
            this.setState(this.state);
        }
    }

    flightColorChange(target, event) {
        console.log("trace color changed!");
        console.log(event);
        console.log(event.target);
        console.log(event.target.value);

        target.state.baseLayer.setStyle(new Style({
            stroke: new Stroke({
                color: event.target.value,
                width: 3
            })
        }));

        for (let i = 0; i < target.state.layers.length; i++) {
            let layer = target.state.layers[i];
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

    downloadClicked() {
        window.open("/protected/get_kml?flight_id=" + this.props.flightInfo.id);
    }

    exclamationClicked() {
        console.log ("exclamation clicked!");

        if (!this.state.eventsLoaded) {
            console.log("loading events!");

            this.state.eventsLoaded = true;
            this.state.eventsVisible = true;

            this.fetchEvents();


            console.log("got events");
            console.log(this.state.events);

            let events = this.state.events;

            // create list of event Features to display on map //
            for (let i = 0; i < events.length; i++) {
                var points;
                var eventPoint;
                var eventOutline;
                let event = events[i];

                // Create Feature for event
                if (!this.state.mapLoaded){              // if points (coordinates) have not been fetched
                    // create eventPoint with placeholder coordinates
                    eventPoint = new Feature({
                        geometry : new LineString( [0,0] ),
                        name: 'Event'
                    });

                    // create outlines
                    eventOutline = new Feature({
                        geometry : new LineString( [0,0] ),
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
            this.state.eventLayer = new VectorLayer({
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

            // create eventLayer & add eventPoints
            this.state.eventOutlineLayer = new VectorLayer({
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

            //this.state.eventLayer.flightState = this;
            this.state.eventOutlineLayer.setVisible(true);
            this.state.eventLayer.setVisible(true);

            // add to map only if flightPath loaded
            if (this.state.mapLoaded){
                map.addLayer(this.state.eventOutlineLayer);
                map.addLayer(this.state.eventLayer);
            }

            this.setState(this.state);

        } else {
            console.log("events already loaded!");

            //toggle visibility if already loaded
            this.state.eventsVisible = !this.state.eventsVisible;
            this.state.eventLayer.setVisible(this.state.eventsVisible);
            this.state.eventOutlineLayer.setVisible(this.state.eventsVisible);

            if(!this.state.eventsVisible) {
                console.log("clearing plotly");
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
    downloadClicked(type) {
        if (type === 'KML') {
            window.open("/protected/get_kml?flight_id=" + this.props.flightInfo.id);
        } else if (type === 'XPL10') {
            selectAircraftModal.show('10', this.submitXPlanePath, this.props.flightInfo.id);    
        } else if (type === 'XPL11') {
            selectAircraftModal.show('11', this.submitXPlanePath, this.props.flightInfo.id);    
        } else if(type === 'CSV-IMP') {
            window.open("/protected/get_csv?flight_id=" + this.props.flightInfo.id + "&generated=false");
        } else if(type === 'CSV-GEN') {
            window.open("/protected/get_csv?flight_id=" + this.props.flightInfo.id + "&generated=true");
        }

    }

    /**
     * Gets the aircraft path from the submit aircraft modal
     * @param type the xplane version
     * @param path the selected path
     * @param flightId the flightId
     **/
    submitXPlanePath(type, path, useMSL){
        console.log("submitting the xplane path to server"+type+" "+path+" "+useMSL);
        console.log(this.props);
        window.open("/protected/get_xplane?flight_id=" + this.props.flightInfo.id + "&version=" + type + "&acft_path=" + path + "&use_msl=" + useMSL);
    }

    getCesiumData(flightId) {

        var cesiumData = null;
        var submissionData = {
            "flightId" : flightId
        };

        $.ajax({
            type : 'POST',
            url : '/protected/cesium_data',
            traditional : true,
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log(response)
                cesiumData = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log(errorThrown);
            },
            async: false
        });

        return cesiumData;
    }

    getCesiumData(flightId) {

        var cesiumData = null;
        var submissionData = {
            "flightId" : flightId
        };

        $.ajax({
            type : 'POST',
            url : '/protected/cesium_data',
            traditional : true,
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log(response)
                cesiumData = response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log(errorThrown);
            },
            async: false
        });

        return cesiumData;
    }

    cesiumClicked() {        

        var flightId = this.props.flightInfo.id;
        
        this.state.cesiumFlightEnabled = !this.state.cesiumFlightEnabled;
        this.state.replayToggled = !this.state.replayToggled;
        
        this.setState(this.state);
        this.props.showCesiumPage(flightId, this.state.color);

   }

    addCesiumFlight() {

        console.log("Adding flight to cesium");

        this.state.cesiumFlightEnabled = true;

        this.setState(this.state);
        this.props.showCesiumPage(this.props.flightInfo.id, this.state.color);

    }
    removeCesiumFlight() {

        console.log("Removing Cesium flights");
        this.state.cesiumFlightEnabled = false;
        this.props.removeCesiumFlight(this.props.flightInfo.id);
        this.setState(this.state);

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

        const URL = "/protected/ngafid_cesium_old?flight_id=" + (this.props.flightInfo.id).toString();
        window.open(URL);
    }

    closeParamDisplay() {
        console.log("popup closed!");
    }

    zoomChanged(oldZoom) {
        let currZoom = map.getView().getZoom();
        console.log("old zoom: " + oldZoom);
        console.log("current zoom: " + currZoom);

        for(let i = 0; i < this.state.mapPopups.length; i++) {
            this.state.mapPopups[i].close();
        }
    }

    displayParameters(event){
        var pixel = event.pixel;
        var features = [];

        map.forEachFeatureAtPixel(pixel, function(feature, layer) {
            features.push(feature)
        });

        let target = features[0];
        console.log("populating new popup for metrics");

        if (target.get('name') === 'Event' && features[2] != null) {
            target = features[2];
        }

        console.log(this.state.events);
            

        var lociInfo = new Array(), info = null, precision = 0;

        if (target != null && (target.parent === "LOC-I Index" || target.parent === "Stall Index")) {
            let index = target.getId();
            console.log("target info:");
            console.log(index);
            console.log(target);    

            console.log(this.state.flightMetrics);
            let submissionData = {
                flight_id : this.props.flightInfo.id,
                time_index : index
            };

            lociInfo.push(index);

            let spData = this.state.seriesData.get('Stall Index');
            let lociData = this.state.seriesData.get('LOC-I Index');

            lociInfo.push(spData[index]); //All flights should have SI data
            if (lociData == null) {
                lociInfo.push(null);
            } else {
                lociInfo.push(lociData[index]);
            }

            $.ajax({
                type: 'POST',
                url: '/protected/loci_metrics',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("got loci_metrics response");
                    console.log(response);
                    info = response.values;
                    precision = response.precision;
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    console.log("Error getting upset data:");
                    console.log(errorThrown);
                },   
                async: false 
            });  

            var popupProps = {
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

            var popup = this.renderNewPopup(this.state.mapPopups.length - 1, popupProps);
            var visibleStyle = new Style({
                stroke: new Stroke({
                        color: this.state.color,
                        width: 1.5
                    }),
                    image: new Circle({
                        radius: 5,
                        stroke: new Stroke({
                            color: this.state.color,
                            width: 2
                        })
                    })
                });

            if (target != null) {
                //console.log("need to draw point at: " + this.state.points[index]);
                //this.state.trackingPoint.setStyle(visibleStyle);
                //this.state.trackingPoint.getGeometry().setCoordinates(index);
            }

        } else {
            console.log("wont render popup");
        }
    }

    /**
     * Recursively find a vacant (unpinned) popup or create a new one
     */
    renderNewPopup(index, props) {
        if (index < 0 || this.state.mapPopups[index] == null) {
            // if we reach the bottom of the stack, we must allocate memory for a new popup component
            var outterHTM = document.createElement('div');
            document.body.appendChild(outterHTM);
            var popup = ReactDOM.render(React.createElement(MapPopup, props), outterHTM);
            outterHTM.setAttribute("id", "popover" + this.state.mapPopups.length);
            this.state.mapPopups.push(popup);
            return popup;
        } else if (this.state.mapPopups[index].isPinned()) {
            // skip reallocating an existing popup if it is pinned
            return this.renderNewPopup(index - 1, props);
        } else {
            console.log("using existing popup to render!");
            let element = "popover" + index;
            var popup = ReactDOM.render(React.createElement(MapPopup, props), document.getElementById(element));
            popup.show(); // we must call show in case the popup was closed before
            return popup;
        }

    }

    tagClicked() {
        this.state.tagsVisible = !this.state.tagsVisible;
        this.setState(this.state);
    }

    mapClicked() {

        //Flagged as not having coordinate info, exit
        if (this.props.flightInfo.has_coords === "0")
            return;

        //2D map layer not loaded for this flight...
        if (!this.state.mapLoaded) {
            
            this.props.showMap();
            this.state.mapLoaded = true;

            const thisFlight = this;
            
            this.fetchEvents();
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

                const submissionDataDoubleSeries = {
                    seriesName : name,
                    flightId : this.props.flightInfo.id
                };

                $.ajax({
                    type: 'POST',
                    url: '/protected/double_series',
                    data : submissionDataDoubleSeries,
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

            const submissionDataCoordinates = {
                request : "GET_COORDINATES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id,
                user_id : 1,
                flightId : this.props.flightInfo.id,
            };

            $.ajax({
                type: 'POST',
                url: '/protected/coordinates',
                data : submissionDataCoordinates,
                dataType : 'json',
                success : function(response) {

                    var coordinates = response.coordinates;
                    let points = thisFlight.state.points;
                    for (var i = 0; i < coordinates.length; i++) {
                        var point = fromLonLat(coordinates[i]);
                        points.push(point);
                    }

                    var color = thisFlight.state.color;
                    //console.log(color);

                    thisFlight.state.trackingPoint = new Feature({
                        geometry : new Point(points[0]),
                        name: 'TrackingPoint'
                    });

                    thisFlight.state.trackingPoint.setId(points[0]);

                    thisFlight.state.layers = new Array();
                    let layers = thisFlight.state.layers;


                    // adding itinerary (approaches and takeoffs) to flightpath 
                    var itinerary = thisFlight.props.flightInfo.itinerary;
                    var flight_phases = [];

                    // Create flight phase styles
                    var takeoff_style = new Style({
                        stroke: new Stroke({
                            color: "#34eb52",
                            width: 3
                        })
                    });

                    var approach_style = new Style({
                        stroke: new Stroke({
                            color: "#347deb",
                            width: 3
                        })
                    });

                    // create and add Features to flight_phases for each flight phase in itinerary
                    for (let i = 0; i < itinerary.length; i++) {
                        var stop = itinerary[i];
                        var approach = null;
                        var takeoff = null;

                        // creating Linestrings
                        if (stop.startOfApproach != -1 && stop.endOfApproach != -1) {
                            approach = new LineString( points.slice( stop.startOfApproach, stop.endOfApproach ) );
                        }
                        if (stop.startOfTakeoff != -1 && stop.endOfTakeoff != -1) {
                            takeoff = new LineString( points.slice( stop.startOfTakeoff, stop.endOfTakeoff ) );
                        }

                        // set styles and add phases to flight_phases list
                        if (approach != null) {
                            let phase = new Feature({
                                             geometry: approach,
                                             name: 'Approach'
                                         });
                            phase.setStyle(approach_style);
                            flight_phases.push( phase );
                        }
                        if (takeoff != null) {
                            let phase = new Feature({
                                             geometry: takeoff,
                                             name: 'Takeoff'
                                         });
                            phase.setStyle(takeoff_style);
                            flight_phases.push( phase );
                        }
                    }

                    thisFlight.state.baseLayer = new VectorLayer({
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
                                thisFlight.state.trackingPoint,
                            ]
                        })
                    });

                    let phaseLayer = new VectorLayer({
                        name : 'Itinerary Phases',
                        nMap : true,
                        style: new Style({
                            stroke: new Stroke({
                                color: [1,1,1,1],
                                width: 3
                            })
                        }),

                        source : new VectorSource({
                            features: flight_phases
                        })
                    }); 

                    let baseLayer = thisFlight.state.baseLayer;

                    baseLayer.flightState = thisFlight;

                    thisFlight.state.pathVisible = true;
                    thisFlight.state.itineraryVisible = true;
                    thisFlight.state.nanOffset = response.nanOffset;
                    thisFlight.state.coordinates = response.coordinates;
                    thisFlight.state.points = points;

                    // toggle visibility of itinerary
                    layers.push(baseLayer, phaseLayer);
                    
                    const lociData = thisFlight.state.seriesData.get('LOC-I Index');
                    const spData = thisFlight.state.seriesData.get('Stall Index');

                    generateStallLayer(spData, layers, thisFlight);
                    generateLOCILayer(lociData, layers, thisFlight);
                    
                    console.log("adding layers!");
                    for(let i = 0; i < layers.length; i++){
                        let layer = layers[i];
                        console.log(layer);
                        if(layer.get('name').includes('Itinerary')) {
                            //Itinerary will be the default layer
                            thisFlight.state.selectedPlot = layer.values_.name;
                            layer.setVisible(true);
                        } else {
                            layer.setVisible(false);
                        }
                        map.addLayer(layer);
                    }

                    console.log(layers);
                    thisFlight.props.setAvailableLayers(layers);

                    console.log("added layers");
                    console.log(map.getLayers());
                    map.on('click', thisFlight.displayParameters); 

                    var currZoom = map.getView().getZoom();
                    map.on('moveend', () => thisFlight.zoomChanged(currZoom));
                    // adding coordinates to events, if needed //
                    var events = [];
                    var eventPoints = [];
                    var eventOutlines = [];
                    if (thisFlight.state.eventsLoaded) {
                        events = thisFlight.state.events;
                        eventPoints = thisFlight.state.eventPoints;
                        eventOutlines = thisFlight.state.eventOutlines;
                        for (let i = 0; i < events.length; i++){
                            let line = new LineString(points.slice(events[i].startLine -1, events[i].endLine + 1));
                            eventPoints[i].setGeometry(line);                   // set geometry of eventPoint Features
                            eventOutlines[i].setGeometry(line);
                        }

                        // add eventLayer to front of map
                        let eventLayer = thisFlight.state.eventLayer;
                        let outlineLayer = thisFlight.state.eventOutlineLayer;
                        map.addLayer(outlineLayer);
                        map.addLayer(eventLayer);
                    }

                    let extent = baseLayer.getSource().getExtent();
                    console.log(extent);
                    map.getView().fit(extent, map.getSize());

                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.mapLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        
        //2D map layer already loaded for this flight...
        } else {

            this.state.mapLoaded = false;

            //Toggle visibility if already loaded
            this.state.pathVisible = !this.state.pathVisible;
            this.state.itineraryVisible = !this.state.itineraryVisible;

            console.log("Already rendered: ", this.state.layers);

            for (let i = 0; i < this.state.layers.length; i++) {

                let layer = this.state.layers[i];
                console.log("Layer: ", layer);

                //Layer values are visible and the path is not, hide the layer
                if (layer.values_.visible && !this.state.pathVisible) {

                    this.state.selectedPlot = layer.values_.name;
                    layer.setVisible(false);

                //Layer values match selected plot and the path is visible, show the layer
                } else if (layer.values_.name === this.state.selectedPlot && this.state.pathVisible) {

                    layer.setVisible(true);

                }

            }

            //Event layer not found, visibility of events
            if (this.state.eventLayer != null) {
                this.state.eventLayer.setVisible(!this.state.eventLayer.getVisible());
                this.state.eventOutlineLayer.setVisible(!this.state.eventOutlineLayer.getVisible());
            }

            //Path is visible, display the map
            if (this.state.pathVisibile) {

                this.props.showMap();

                let extent = this.state.baseLayer.getSource().getExtent();
                console.log(extent);
                map.getView().fit(extent, map.getSize());

            }

            //Trigger state update
            this.setState(this.state);

        }
    }

    /**
     * Changes all the flights on a given page by calling the parent function
     */
    updateFlights(flights){
        this.props.updateParentState(flights);
    }

    /**
     * Changes the tags associated with this flight
     */
    invokeUpdate(tags){
        this.state.tags = tags;
        this.setState(this.state);
    }

    /**
     * Called when props are updated
     * changes state if props have in fact changed
     * @param oldProps the old props before the update
     */
    componentDidUpdate(oldProps) {
        console.log("props updated");
        const newProps = this.props;
          if(oldProps.tags !== newProps.tags) {
            this.state.tags = this.props.tags;
            this.setState(this.state);
          }
    }

    addCesiumFlightPhase(phase) {
        this.props.addCesiumFlightPhase(phase);
    }

    addCesiumEventEntity(event) {
        console.log("Adding event to Cesium");
        console.log(event);
        this.props.addCesiumEventEntity(event, this.props.flightInfo.id);
    }

    zoomToEventEntity(eventId, flightId) {
        this.props.zoomToEventEntity(eventId, flightId);
    }

    addCesiumFlightPhase(phase) {
        this.props.addCesiumFlightPhase(phase);
    }

    addCesiumEventEntity(event) {
        console.log("Adding event to Cesium");
        console.log(event);
        this.props.addCesiumEventEntity(event, this.props.flightInfo.id);
    }

    zoomToEventEntity(eventId, flightId) {
        this.props.zoomToEventEntity(eventId, flightId);
    }

    render() {

        let buttonClasses = "p-1 expand-import-button btn btn-outline-secondary d-flex align-items-center justify-content-center";
        let cesiumControlButtonClasses = "p-1 btn btn-primary d-flex align-items-center justify-content-center";
        //const styleButton = { minWidth:"2.25em", minHeight:"2.25em" };'

        const buttonSize = "1.75em";
        const styleButton = { minWidth:buttonSize, minHeight:buttonSize, width:buttonSize, height:buttonSize, display:"inlineBlock", justifyContent:"center", alignContent:"center", textAlign:"center" };
        const styleEmptyCell = {fontStyle:"italic", fontSize:"0.75em", opacity:"0.50", userSelect:"none"};

        let firstCellClasses = "p-1 card mr-1"
        let cellClasses = "p-1 card mr-1"

        let flightInfo = this.props.flightInfo;

        let startTime = moment(flightInfo.startDateTime);
        let endTime = moment(flightInfo.endDateTime);

        let globeClasses = "";
        let traceDisabled = false;
        let globeTooltip = "";

        let tagTooltip = "Click to tag a flight for future queries and grouping";

        /*
            //console.log(flightInfo);
            if (!flightInfo.hasCoords) {
                //console.log("flight " + flightInfo.id + " doesn't have coords!");
                globeTooltip = "Cannot display flight on the map because the flight data did not have latitude/longitude.";
                traceDisabled = true;
            } else {
                globeTooltip = "Click the globe to display the flight on the map.";
            }
        */

        let visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0) {
                visitedAirports.push(flightInfo.itinerary[i].airport);
            }
        }
        let visitedAirportsRow = "";
        if (visitedAirports.length > 0)
            visitedAirportsRow = visitedAirports.join(", ");
        else
            visitedAirportsRow = <div style={styleEmptyCell}>No Airports...</div>


        const FLIGHT_COMPONENT_ROW_HIDDEN = "";


        //Events Row
        let eventsRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        if (this.state.eventsVisible) {
            eventsRow = (
                <Events className="w-100" events={this.state.events} parent={this}/>
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
                let style = {
                    backgroundColor : tag.color,
                    marginRight : '4px',
                    lineHeight : '2',
                    opacity : '75%'
                }
                return(
                    <span key={index} className="badge badge-primary" style={{lineHeight : '1.5', marginRight : '4px', backgroundColor : 'var(--c_tag_badge)', color : 'var(--c_text)'}} title={tag.description}>
                        <span className="badge badge-pill badge-primary" style={style} page={this.state.page}>
                            <i className="fa fa-tag" aria-hidden="true"/>
                        </span>   {tag.name}
                    </span>
                );
            });
        } else {
            tagPills = <div style={styleEmptyCell}>No Tags...</div>
        }


        //Itinerary Row
        let itineraryRow = FLIGHT_COMPONENT_ROW_HIDDEN;
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


        //Traces Row
        let tracesRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        if (this.state.traceNamesVisible) {

            tracesRow = (
                <TraceButtons showPlot={() => {this.props.showPlot();}} parentFlight={this} flightId={flightInfo.id}/>
            );
            
        }


        //Cesium Row
        let cesiumRow = FLIGHT_COMPONENT_ROW_HIDDEN;
        const flightId = flightInfo.id;
        const flightPhases = ["Taxiing", "Takeoff", "Climb", "Cruise to Final", "Full Flight"]
        if (this.state.cesiumFlightEnabled) {

            let cesiumHeader = "";
            let flightPhasesCheckBox = "";
            let toggleCameraButton = "";
            let jumpToStartButton = "";

            cesiumHeader = (
                <b className={"p-1 d-flex flex-row justify-content-start align-items-center"} style={{marginBottom:"0"}}>
                    <div className="d-flex flex-column mr-3" style={{width: "16px", minWidth:"16px", maxWidth:"16px", height: "16px"}}>
                        <i className='fa fa-globe ml-2' style={{fontSize: "12px", marginTop: "3px", opacity: "0.50"}}/>
                    </div>
                    <div style={{fontSize: "0.75em"}}>
                        Cesium Phases
                    </div>
                </b>
            );

            flightPhasesCheckBox = (
                <div>
                    <div className={"d-flex flex-row p-1"} style={{"overflowX" : "auto"}}>
                        {
                            flightPhases.map((phase, index) => {
                                return (
                                    <button
                                        className={buttonClasses + " mr-1"}
                                        style={{flex : "0 0 10em"}}
                                        data-bs-toggle="button"
                                        key={index}
                                        onClick={() => this.props.addCesiumFlightPhase(phase, flightId)}
                                    >
                                        {phase}
                                    </button>
                                )
                            })
                        }
                    </div>
                </div>
            );
            toggleCameraButton = (
                <button className={`${cesiumControlButtonClasses} ml-1 mt-1 mb-1 mr-0`} style={{flex : "0 0 10em"}} aria-pressed="false" onClick={() => this.props.cesiumFlightTrackedSet(flightId)}>
                    <i className="fa fa-camera mr-2"/>
                    Track Flight
                </button>

            );
            jumpToStartButton = (
                <button className={`${cesiumControlButtonClasses} ml-1 mt-1 mb-1 mr-0`} style={{flex : "0 0 10em"}} aria-pressed="false" onClick={() => this.props.cesiumJumpToFlightStart(flightId)}>
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
        ]

        $(function () {
            $('[data-bs-toggle="tooltip"]').tooltip()
        })

        return (
            <div className="card mb-1" style={{backgroundColor:"var(--c_entry_bg)"}}>
                <div className="">
                    <div className="d-flex flex-column">

                        <div className="d-flex flex-row p-1">

                            {/* FLIGHT INFO */}
                            <div style={{ flexBasis: "32.5%", whiteSpace: "nowrap" }}>
                                <div className={`${firstCellClasses} d-flex flex-row`} style={{ height: "100%" }}>
                                    <div className="d-flex flex-column" style={{ alignItems: "start" }}>

                                        {/* Filter Add Button */}
                                        <a 
                                            href={"#"}
                                            onMouseEnter={() => this.setState({ filterAddButtonHovered: true })}
                                            onMouseLeave={() => this.setState({ filterAddButtonHovered: false })}
                                            onClick={() => this.props.onAddFilter(this.props.flightInfo.id)}
                                        >
                                            <i className={`fa ${this.state.filterAddButtonHovered ? "fa-search" : "fa-plane"}  p-1`}>
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

                                        <div className={"d-flex flex-row ml-auto mr-auto"} style={{flexShrink:"1", gap:"0.25em"}}>

                                            <button className={buttonClasses} style={styleButton} id={`plotToggle-${this.props.flightInfo.id}`} data-bs-toggle="button" onClick={() => this.plotClicked()}>
                                                <i className="fa fa-area-chart p-1"/>
                                            </button>

                                            <button className={buttonClasses + globeClasses} title={globeTooltip} id={`cesiumToggle-${this.props.flightInfo.id}`} aria-pressed={this.state.replayToggled} style={styleButton} onClick={() => this.toggleCesiumFlight()}>
                                                <i className="fa fa-globe p-1"/>
                                            </button>

                                            <button className={buttonClasses} style={styleButton} id={`mapToggle-${this.props.flightInfo.id}`} data-bs-toggle="button" onClick={() => this.mapClicked()}>
                                                <i className="fa fa-map-o p-1"/>
                                            </button>
                                        </div>

                                        <div className={"d-flex flex-row ml-auto mr-auto"} style={{flexShrink:"1", gap:"0.25em"}}>

                                            <button className={buttonClasses} data-bs-toggle="button" aria-pressed="false" style={styleButton} onClick={() => this.exclamationClicked()}>
                                                <i className="fa fa-exclamation p-1"/>
                                            </button>

                                            <button className={buttonClasses} style={styleButton} disabled={true} title={"The external replay system is deprecated.\nCesium flight replays can now be viewed on this page with the globe buttons."}>
                                                <i className="fa fa-video-camera p-1"/>
                                            </button>

                                            <button className={buttonClasses} style={styleButton} type="button" id="dropdownMenu2" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                                <i className="fa fa-download p-1"/>
                                            </button>

                                            <div className="dropdown-menu" aria-labelledby="dropdownMenu2">
                                                <button className="dropdown-item" type="button" onClick={() => this.downloadClicked('CSV-IMP')}>
                                                    Export to CSV (Original)
                                                    <i className="ml-1 fa fa-question-circle" data-bs-toggle="tooltip" data-bs-placement="top" title="The NGAFID stores original CSV files from the aircraft's flight data recorder. Select this option if you wish to view this flight's original CSV file."></i>
                                                </button>
                                                <button className="dropdown-item" type="button" onClick={() => this.downloadClicked('CSV-GEN')}>
                                                    Export to CSV (Generated)
                                                    <i className="ml-1 fa fa-question-circle" data-bs-toggle="tooltip" data-bs-placement="top" title="The NGAFID adds additional calculated parameters for further flight analysis, such as angle of attack. Select this option if you wish for the CSV file to contain such parameters."></i>
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

                    {/* Render all Rows */}
                    {
                        rowList.map((row, index) => {

                            //Row is not visible, skip
                            if (row === FLIGHT_COMPONENT_ROW_HIDDEN)
                                return null;
                            
                            console.log("Rendering row: ", row);

                            return (
                                <div key={index} className="d-flex flex-row m-1 p-1" style={{overflowX:"hidden", width:"99%", backgroundColor: "var(--c_row_bg_alt)", borderRadius:"0.5em"}}>
                                    {row}
                                </div>
                            )
                        })
                    }
                    
                </div>
            </div>
        );
    }
}


export { Flight };

