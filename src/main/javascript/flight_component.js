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

import Plotly from 'plotly.js';

var moment = require('moment');

// So the weights w0 and w1 are for the weighted average
// They should add to 1.0 so if one of them is 0, the resulting color
// will just be the other color (e.g. w0 is 0 then the resulting color will be the same as c1)
function interpolateColors(c0, w0, c1, w1) {
	var new_color = [0.0, 0.0, 0.0];
	// red = 0, green = 1, blue = 2
	for (var i = 0; i < 3; i++) {
		new_color[i] = Math.round(w0 * c0[i] + w1 * c1[i]);
	}
	return new_color;
}

// loc_percentage should be between 0 and 1.0
// This will get the color for a given p(LOC)
// This can probably be made cleaner / not use if statements and just use lists but im lazy
function paletteAt(loc_probability) {
	if (loc_probability < 0.8) {
		var c0 = [0, 255, 0]; // green
		var c1 = [255, 255, 0]; // yellow

		// This will be a proportion between 0 and 1 since the max value for loc_p = 0.8 and min is 0
		var weight = loc_probability / 0.8;
		var w0 = 1.0 - weight; // if weight is 1, we want there to be no green and all yellow
		var w1 = weight;

		return interpolateColors(c0, w0, c1, w1);
	} else if (loc_probability >= 0.8 && loc_probability < 1.0) {
		// Our range of loc_p values is 0.8 to 1.0, so a distance of 0.2
		var c0 = [255, 255, 0];//yellow
		var c1 = [255, 0, 0];//red

		// The minimum value of this will be 0.0 and max is 0.2
		var numerator = loc_probability - 0.8;

		// value range is 0.0 to 1.0
		var weight = numerator / 0.2;
		var w0 = 1.0 - weight;
		var w1 = weight;

		return interpolateColors(c0, w0, c1, w1);
	} else {
		// red
		return [255, 0, 0];
	}
}

class Flight extends React.Component {
    constructor(props) {
        super(props);

        let color = Colors.randomValue();
        console.log("flight color: " );
        console.log(color);

        this.state = {
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
            tags : props.tags,
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
            eventOutlineLayer : null
        }

		this.submitXPlanePath = this.submitXPlanePath.bind(this);
		this.displayParameters = this.displayParameters.bind(this);
		this.closeParamDisplay = this.closeParamDisplay.bind(this);
		this.zoomChanged = this.zoomChanged.bind(this);
    }
	

    componentWillUnmount() {
        console.log("unmounting:");
        console.log(this.props.flightInfo);

        if (this.props.flightInfo.has_coords === "0") return;

        console.log("hiding flight path");
        this.state.pathVisible = false;
        this.state.itineraryVisible = false;
        if (this.state.layer) {
            this.state.layer.setVisible(false);
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
                success : function(response) {
                    console.log("received response: ");
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
					 * LOCI
					 * StallProbability
                     */
                    var preferredNames = ["AltAGL", "AltMSL", "E1 MAP", "E2 MAP", "E1 RPM", "E2 RPM", "IAS", "NormAc", "Pitch", "Roll", "VSpd", "LOCI", "StallProbability"];
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
                    errorModal.show("Error Getting Potentail Plot Parameters", errorThrown);
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
                width: 1.5
            })
        }));

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
                    console.log("received response: ");
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

                    // create list of event Features to display on map //
                    for (let i = 0; i < events.length; i++) {
                        var points;
                        var eventPoint;
                        var eventOutline;
                        let event = events[i];

                        // Create Feature for event
                        if (!thisFlight.state.mapLoaded){              // if points (coordinates) have not been fetched
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
                            points = thisFlight.state.points;
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
                        thisFlight.state.eventsMapped.push(false);
                        thisFlight.state.eventPoints.push(eventPoint);
                        thisFlight.state.eventOutlines.push(eventOutline);
                    }

                    // create eventLayer & add eventPoints
                    thisFlight.state.eventLayer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: [0,0,0,0],
                                width: 3
                            })
                        }),

                        source : new VectorSource({
                            features: thisFlight.state.eventPoints
                        })
                    });

                    // create eventLayer & add eventPoints
                    thisFlight.state.eventOutlineLayer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: [0,0,0,0],
                                width: 4
                            })
                        }),

                        source : new VectorSource({
                            features: thisFlight.state.eventOutlines
                        })
                    });

                    //thisFlight.state.eventLayer.flightState = thisFlight;
                    thisFlight.state.eventOutlineLayer.setVisible(true);
                    thisFlight.state.eventLayer.setVisible(true);

                    // add to map only if flightPath loaded
                    if (thisFlight.state.mapLoaded){
                        map.addLayer(thisFlight.state.eventOutlineLayer);
                        map.addLayer(thisFlight.state.eventLayer);
                    }

                    thisFlight.setState(thisFlight.state);
                },
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.mapLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Events", errorThrown);
                },
                async: true
            });

        } else {
            console.log("events already loaded!");

            //toggle visibility if already loaded
            this.state.eventsVisible = !this.state.eventsVisible;
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
        } else if(type === 'CSV') {
            window.open("/protected/get_csv?flight_id=" + this.props.flightInfo.id);
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

    cesiumClicked() {
        window.open("/protected/ngafid_cesium?flight_id=" + this.props.flightInfo.id);
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
		console.log(pixel);

		var info = new Array();

		if (target != null && (target.parent === "PLOCI" || target.parent === "PStall")) {
			let index = target.getId();
			console.log("target info:");
			console.log(index);
			console.log(target);	

			info.push(index);
			info.push(this.state.seriesData.get('StallProbability')[index]);
			info.push(this.state.seriesData.get('LOCI')[index]);
			info.push(this.state.seriesData.get('Roll')[index]);
			info.push(this.state.seriesData.get('Pitch')[index]);
			info.push(this.state.seriesData.get('IAS')[index]);
			info.push(this.state.seriesData.get('AltMSL')[index]);
			info.push(this.state.seriesData.get('AltAGL')[index]);
			info.push(this.state.seriesData.get('AOASimple')[index]);
			info.push(this.state.seriesData.get('E1 RPM')[index]);

			var popupProps = {
				pixel : pixel,
				status : '',
				info : info,
				placement : pixel,
				lineSeg : target,
				closePopup : this.closeParamDisplay(),
				title : 'title'
			};

			var popup = this.renderNewPopup(this.state.mapPopups.length - 1, popupProps);
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
        console.log("tag clicked!");

        if (!this.state.tagsLoaded) {
            console.log("loading tags!");

            var thisFlight = this;

            var submissionData = {
                flightId : this.props.flightInfo.id,
            };

            $.ajax({
                type: 'POST',
                url: '/protected/flight_tags',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    if(response != null){
                        thisFlight.state.tags = response;
                    }

                    thisFlight.state.tagsLoaded = true;
                    thisFlight.state.tagsVisible = !thisFlight.state.tagsVisible;
                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.tagsLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Tags", errorThrown);
                },   
                async: true 
            });  

        } else {
            console.log("tags already loaded!");

            //toggle visibility if already loaded
            this.state.tagsVisible = !this.state.tagsVisible;
            this.setState(this.state);
        }
    }

    globeClicked() {
        if (this.props.flightInfo.has_coords === "0") return;

        if (!this.state.mapLoaded) {
            this.props.showMap();
            this.state.mapLoaded = true;

            var thisFlight = this;

            var lociSubmissionData = {
				seriesName : "LOCI",
                flightId : this.props.flightInfo.id
            };

			//TODO: get upset probability data here

			console.log("getting upset probabilities");

			var names = [
				"StallProbability",
				"LOCI",
				"Roll",
				"IAS",
				"Pitch", 
				"AltMSL",
				"AOASimple",
				"E1 RPM",
				"AltAGL",
			];

			for (let i = 0; i < names.length; i++) {
				const name = names[i];
				console.log(name);

				var submissionData = {
					seriesName : name,
					flightId : this.props.flightInfo.id
				};

				$.ajax({
					type: 'POST',
					url: '/protected/double_series',
					data : submissionData,
					dataType : 'json',
					success : function(response) {
						console.log("got double_series response");
						console.log(thisFlight.state.seriesData);
						thisFlight.state.seriesData.set(name, response.y);
					},
					error : function(jqXHR, textStatus, errorThrown) {
						console.log("Error getting upset data:");
						console.log(errorThrown);
					},   
					async: true 
				});  
			}

            var submissionData = {
                request : "GET_COORDINATES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightInfo.id,
            };

            $.ajax({
                type: 'POST',
                url: '/protected/coordinates',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    //console.log("received response: ");
                    //console.log(response);

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

					thisFlight.state.layers = new Array();
					let layers = thisFlight.state.layers;

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
                                thisFlight.state.trackingPoint
                            ]
                        })
                    });

					let baseLayer = thisFlight.state.baseLayer;


                    baseLayer.flightState = thisFlight;

                    thisFlight.state.pathVisible = true;
                    thisFlight.state.itineraryVisible = true;
                    thisFlight.state.nanOffset = response.nanOffset;
                    thisFlight.state.coordinates = response.coordinates;
                    thisFlight.state.points = points;

					layers.push(baseLayer);


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
					
					const lociData = thisFlight.state.seriesData.get('LOCI');
					const spData = thisFlight.state.seriesData.get('StallProbability');

					var lociPhases = [], lociOutlinePhases = [];
					if (lociData != null) {
						for(let i = 0; i < lociData.length; i++){
							let val = lociData[i];
							var feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : "LOCI"
							});
							let sval = val / 100.0;
							feat.setId(i);
							feat.parent = 'PLOCI';
							feat.setStyle([
							  new Style({
								stroke: new Stroke({
								  color: paletteAt(sval),
								  width: 5 + parseInt((val / 10.0).toFixed(0)),
								})
							  })
							]);

							let outFeat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : "LOCI Outline"
							});
							outFeat.setStyle(new Style({
									stroke: new Stroke({
										color: thisFlight.state.color,
										width: 9 + parseInt((val / 10.0).toFixed(0)),
									})
								})
							);
							outFeat.parent = 'PLOCI';

							lociPhases.push(feat);
							lociOutlinePhases.push(outFeat);
						}
					}

					var spPhases = [], spOutlinePhases = [];
					if (spData != null) {
						for(let i = 0; i < spData.length; i++){
							let val = spData[i];
							var feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : "SP"
							});
							let sval = val / 100.0;
							feat.setId(i);
							feat.parent = 'PStall';
							feat.setStyle([
							  new Style({
								stroke: new Stroke({
								  color: paletteAt(sval),
								  width: 5 + parseInt((val / 10.0).toFixed(0)),
								})
							  })
							]);

							let outFeat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : "SP Outline"
							});
							outFeat.setStyle(new Style({
									stroke: new Stroke({
										color: thisFlight.state.color,
										width: 9 + parseInt((val / 10.0).toFixed(0)),
									})
								})
							);
							outFeat.parent = 'PStall';

							spOutlinePhases.push(outFeat);
							spPhases.push(feat);
						}
					}


					let lociLayer = new VectorLayer({
						name : 'PLOCI' ,
						description : 'Loss of Control Probability' ,
						nMap : false,
						disabled : (lociData == null),
                        source : new VectorSource({
                            features: lociPhases                        
						})
                    });

					let lociLayerOutline = new VectorLayer({
						name : 'PLOCI Outline' ,
						description : 'Loss of Control Probability' ,
						nMap : true,
						disabled : (lociData == null),
                        source : new VectorSource({
                            features: lociOutlinePhases                        
						})
                    });

					let spLayer = new VectorLayer({
						name : 'PStall' ,
						description : 'Stall Probability',
						nMap : false,
						disabled : (spData == null),
						source : new VectorSource({
							features: spPhases                        
						})
                    });

					let spLayerOutline = new VectorLayer({
						name : 'PStall Outline' ,
						description : 'Stall Probability',
						nMap : true,
						disabled : (spData == null),
                        source : new VectorSource({
							features: spOutlinePhases                        
						})
                    });

					lociLayer.flightState = thisFlight;
					spLayer.flightState = thisFlight;

					layers.push(lociLayerOutline, lociLayer, spLayerOutline, spLayer);

					console.log("adding layers!");
					for(let i = 0; i < layers.length; i++){
						let layer = layers[i];
						console.log(layer);
						if(layer.values_.name == 'Itinerary') {
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
        } else {
            //toggle visibility if already loaded
            this.state.pathVisible = !this.state.pathVisible;
            this.state.itineraryVisible = !this.state.itineraryVisible;

			console.log("already rendered");
			console.log(this.state.layers);

			for (let i = 0; i < this.state.layers.length; i++) {
				let layer = this.state.layers[i];
				console.log(layer);
				if (layer.values_.visible && !this.state.pathVisible) {
					this.state.selectedPlot = layer.values_.name;
					layer.setVisible(false);
				} else if (layer.values_.name === this.state.selectedPlot && this.state.pathVisible) {
					layer.setVisible(true);
				}
			}

            // toggle visibility of events
            if (this.state.eventLayer != null) {
                this.state.eventLayer.setVisible(!this.state.eventLayer.getVisible());
                this.state.eventOutlineLayer.setVisible(!this.state.eventOutlineLayer.getVisible());
            }
            // toggle visibility of itinerary
            this.state.baseLayer.setVisible(this.state.pathVisible);

            if (this.state.pathVisibile) {
                this.props.showMap();
            }

            this.setState(this.state);

            if (this.state.pathVisible) {
                let extent = this.state.layer.getSource().getExtent();
                console.log(extent);
                map.getView().fit(extent, map.getSize());
            }
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

    render() {
        let buttonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        let lastButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        const styleButton = { };

        let firstCellClasses = "p-1 card mr-1"
        let cellClasses = "p-1 card mr-1"

        let flightInfo = this.props.flightInfo;

        let startTime = moment(flightInfo.startDateTime);
        let endTime = moment(flightInfo.endDateTime);

        let globeClasses = "";
        let traceDisabled = false;
        let globeTooltip = "";

        let tagTooltip = "Click to tag a flight for future queries and grouping";

        //console.log(flightInfo);
        if (!flightInfo.hasCoords) {
            //console.log("flight " + flightInfo.id + " doesn't have coords!");
            globeClasses += " disabled";
            globeTooltip = "Cannot display flight on the map because the flight data did not have latitude/longitude.";
            traceDisabled = true;
        } else {
            globeTooltip = "Click the globe to display the flight on the map.";
        }

        let visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0) {
                visitedAirports.push(flightInfo.itinerary[i].airport);
            }
        }

        let itineraryRow = "";
        if (this.state.itineraryVisible) {
            itineraryRow = (
                <Itinerary showMap={() => {this.props.showMap();}} layers={this.state.layers} itinerary={flightInfo.itinerary} color={this.state.color} coordinates={this.state.coordinates} nanOffset={this.state.nanOffset} parent={this} flightColorChange={this.flightColorChange}/>
            );
        }

        let eventsRow = "";
        if (this.state.eventsVisible) {
            eventsRow = (
                <Events events={this.state.events} parent={this} />
            );
        }

        let tagsRow = "";
        if (this.state.tagsVisible) {
            tagsRow = (
                    <Tags tags={this.state.tags} flightIndex={this.state.pageIndex} flightId={flightInfo.id} parent={this} />
            );
        }

        let tracesRow = "";
        if (this.state.traceNamesVisible) {
            tracesRow = 
                (
                    <TraceButtons showPlot={() => {this.props.showPlot();}} parentFlight={this} flightId={flightInfo.id}/>
                );
        }

        let tagPills = "";
        if(this.state.tags != null){
            tagPills = 
            this.state.tags.map((tag, index) => {
                let style = {
                    backgroundColor : tag.color,
                    marginRight : '4px',
                    lineHeight : '2',
					opacity : '75%'
                }
                return(
					<span key={index} className="badge badge-primary" style={{lineHeight : '1.5', marginRight : '4px', backgroundColor : '#e3e3e3', color : '#000000'}} title={tag.description}>
                        <span className="badge badge-pill badge-primary" style={style} page={this.state.page}>
							<i className="fa fa-tag" aria-hidden="true"></i>
						</span>   {tag.name}
					</span>
                );
            });
        }

        return (
            <div className="card mb-1">
                <div className="card-body m-0 p-0">
                    <div className="d-flex flex-row p-1">
                        <div className={firstCellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            <i className="fa fa-plane p-1"> <a href={'/protected/flight?flight_id=' + flightInfo.id}>{flightInfo.id}</a></i>
                        </div>

                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            {flightInfo.tailNumber}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            {flightInfo.systemId}
                        </div>


                        <div className={cellClasses} style={{flexBasis:"120px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.airframeType}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.startDateTime}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.endDateTime}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"80px", flexShrink:0, flexGrow:0}}>

                            {moment.utc(endTime.diff(startTime)).format("HH:mm:ss")}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>
                            {visitedAirports.join(", ")}
                        </div>

                        <div className={cellClasses} style={{
							flexGrow:1,
							//textShadow: '-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000'
						}}>

                            <div>
                                {tagPills}
                            </div>
                        </div>

                        <div className="p-0">
                            <button className={buttonClasses} data-toggle="button" aria-pressed="false" style={styleButton} onClick={() => this.exclamationClicked()}>
                                <i className="fa fa-exclamation p-1"></i>
                            </button>

                            <button className={buttonClasses} data-toggle="button" title={tagTooltip} aria-pressed="false" style={styleButton} onClick={() => this.tagClicked()}>
                                <i className="fa fa-tag p-1"></i>
                            </button>

                            <button className={buttonClasses + globeClasses} data-toggle="button" title={globeTooltip} aria-pressed="false" style={styleButton} onClick={() => this.globeClicked()}>
                                <i className="fa fa-map-o p-1"></i>
                            </button>

                            <button className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.plotClicked()}>
                                <i className="fa fa-area-chart p-1"></i>
                            </button>

                            <button className={buttonClasses + globeClasses} disabled={traceDisabled} style={styleButton} onClick={() => this.cesiumClicked()}>
                                <i className="fa fa-globe p-1"></i>
                            </button>

                            <button className={buttonClasses + " disabled"} style={styleButton} onClick={() => this.replayClicked()}>
                                <i className="fa fa-video-camera p-1"></i>
                            </button>

						    <button className={buttonClasses} type="button" id="dropdownMenu2" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                  <i className="fa fa-download p-1"></i>
						    </button>

							<div className="dropdown-menu" aria-labelledby="dropdownMenu2">
								<button className="dropdown-item" type="button" onClick={() => this.downloadClicked('CSV')}>Export to CSV</button>
								<button className="dropdown-item" type="button" onClick={() => this.downloadClicked('KML')}>Export to KML</button>
								<button className="dropdown-item" type="button" onClick={() => this.downloadClicked('XPL10')}>Export to X-Plane 10</button>
								<button className="dropdown-item" type="button" onClick={() => this.downloadClicked('XPL11')}>Export to X-Plane 11</button>
						   </div>

                        </div>
                    </div>

                    {itineraryRow}

                    {tagsRow}

                    {eventsRow}

                    {tracesRow}
                </div>
            </div>
        );
    }
}


export { Flight };
