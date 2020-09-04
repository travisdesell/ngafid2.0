import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import { errorModal } from "./error_modal.js";

import { map, styles, layers, Colors } from "./map.js";

import {fromLonLat, toLonLat} from 'ol/proj.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';

import { Itinerary } from './itinerary_component.js';
import { TraceButtons } from './trace_buttons_component.js';
import { Tags } from './tags_component.js';
import { Events } from './events_component.js';
import { selectAircraftModal } from './select_acft_modal';

import Plotly from 'plotly.js';

var moment = require('moment');

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
            tags : props.tags,
            layer : null,
            parent : props.parent,
            color : color,
			lociData : [],
			spData : [],

            eventsMapped : [],                              // Bool list to toggle event icons on map flightpath
            eventPoints : [],                               // list of event Features
            eventLayer : null,
            itineraryLayer : null,
            eventOutlines : [],
            eventOutlineLayer : null
        }

		this.submitXPlanePath = this.submitXPlanePath.bind(this);
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
                     */
                    var preferredNames = ["AltAGL", "AltMSL", "E1 MAP", "E2 MAP", "E1 RPM", "E2 RPM", "IAS", "NormAc", "Pitch", "Roll", "VSpd"];
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

        let color = event.target.value;
        target.state.color = color;

        console.log(target);
        console.log(target.state);

        target.state.layer.setStyle(new Style({
            stroke: new Stroke({
                color: color,
                width: 1.5
            })
        }));
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
        if(type === 'KML'){
            window.open("/protected/get_kml?flight_id=" + this.props.flightInfo.id);
        }else if (type === 'XPL10'){
			selectAircraftModal.show('10', this.submitXPlanePath, this.props.flightInfo.id);	
        }else if (type === 'XPL11'){
			selectAircraftModal.show('11', this.submitXPlanePath, this.props.flightInfo.id);	
        }else if(type === 'CSV'){
            window.open("/protected/get_csv?flight_id=" + this.props.flightInfo.id);
		}
    }

	/**
	 * Gets the aircraft path from the submit aircraft modal
	 * @param type the xplane version
	 * @param path the selected path
	 * @param flightId the flightId
	 **/
	submitXPlanePath(type, path){
		console.log("submitting the xplane path to server"+type+" "+path);
		console.log(this.props);
		if (type === '10') {
            window.open("/protected/get_xplane?flight_id=" + this.props.flightInfo.id + "&version=10"+"&acft_path="+path);
        }else if (type === '11') {
            window.open("/protected/get_xplane?flight_id=" + this.props.flightInfo.id + "&version=11"+"&acft_path="+path);
        }
	}

    cesiumClicked() {
        window.open("/protected/ngafid_cesium?flight_id=" + this.props.flightInfo.id);
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

			$.ajax({
				type: 'POST',
				url: '/protected/double_series',
				data : lociSubmissionData,
				dataType : 'json',
				success : function(response) {
					console.log("got loci dts response");
					thisFlight.state.lociData = response;
					console.log(thisFlight.state.lociData);
				},   
				error : function(jqXHR, textStatus, errorThrown) {
					console.log("Error getting upset data:");
					console.log(errorThrown);
				},   
				async: true 
			});  

			var spSubmissionData = {
				seriesName : "StallProbability",
                flightId : this.props.flightInfo.id
            };

			$.ajax({
				type: 'POST',
				url: '/protected/double_series',
				data : spSubmissionData,
				dataType : 'json',
				success : function(response) {
					console.log("got stall prob. dts response");
					thisFlight.state.spData = response;
					console.log(thisFlight.state.spData);
				},   
				error : function(jqXHR, textStatus, errorThrown) {
					console.log("Error getting upset data:");
					console.log(errorThrown);
				},   
				async: true 
			});  


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

                    var points = [];
                    for (var i = 0; i < coordinates.length; i++) {
                        var point = fromLonLat(coordinates[i]);
                        points.push(point);
                    }

                    var color = thisFlight.state.color;
                    console.log(color);

                    thisFlight.state.trackingPoint = new Feature({
                                    geometry : new Point(points[0]),
                                    name: 'TrackingPoint'
                                });

                    thisFlight.state.layer = new VectorLayer({
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

                    thisFlight.state.layer.flightState = thisFlight;

                    thisFlight.state.layer.setVisible(true);
                    thisFlight.state.pathVisible = true;
                    thisFlight.state.itineraryVisible = true;
                    thisFlight.state.nanOffset = response.nanOffset;
                    thisFlight.state.coordinates = response.coordinates;
                    thisFlight.state.points = points;

					var lprobs = [];

					if(thisFlight.state.lociData != null){
						console.log("loci data is not null");
						for(let i = 0; i < thisFlight.state.lociData.y.length; i++){
							let val = thisFlight.state.lociData.y[i];
							if(val != null){
								lprobs[i] = val;
							}
						}
					}
					
					console.log("created lprobs:");
					console.log(lprobs);

                    map.addLayer(thisFlight.state.layer);

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

					var lociPhases = [];

					var stages = [];

					//stage 0
					stages.push(new Style({
						stroke: new Stroke({
							color : "#00ff00",
							width : 3
						})
					}));

					//stage 1
					stages.push(new Style({
						stroke: new Stroke({
							color : "#80ff00",
							width : 3
						})
					}));

					//stage 2
					stages.push(new Style({
						stroke: new Stroke({
							color : "#baff00",
							width : 3
						})
					}));

					//stage 3
					stages.push(new Style({
						stroke: new Stroke({
							color : "#ffff00",
							width : 3
						})
					}));

					//stage 4
					stages.push(new Style({
						stroke: new Stroke({
							color : "#ffcf00",
							width : 3
						})
					}));

					//stage 5
					stages.push(new Style({
						stroke: new Stroke({
							color : "#ffbf00",
							width : 3
						})
					}));

					//stage 6
					stages.push(new Style({
						stroke: new Stroke({
							color : "#ff9100",
							width : 3
						})
					}));

					//stage 7
					stages.push(new Style({
						stroke: new Stroke({
							color : "#ff6f00",
							width : 3
						})
					}));

					//stage 8
					stages.push(new Style({
						stroke: new Stroke({
							color : "#ff5500",
							width : 3
						})
					}));

					//stage 9
					stages.push(new Style({
						stroke: new Stroke({
							color : "#ff0000",
							width : 3
						})
					}));

					console.log("generating line strs");
					for(let i = 0; i < lprobs.length; i++){
						let val = lprobs[i];
						var feat;
						if(val > 0 && val < 10) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage0',
							});
							feat.setStyle(stages[0]);
						} else if (val >= 10 && val < 20) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage1',
							});
							feat.setStyle(stages[1]);
						} else if (val >= 20 && val < 30) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage2',
							});
							feat.setStyle(stages[2]);
						} else if (val >= 30 && val < 40) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage3',
							});
							feat.setStyle(stages[3]);
						} else if (val >= 40 && val < 50) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage4',
							});
							feat.setStyle(stages[4]);
						} else if (val >= 50 && val < 60) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage5',
							});
							feat.setStyle(stages[5]);
						} else if (val >= 60 && val < 70) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage6',
							});
							feat.setStyle(stages[6]);
						} else if (val >= 70 && val < 80) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage7',
							});
							feat.setStyle(stages[7]);
						} else if (val >= 80 && val < 90) {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage8',
							});
							feat.setStyle(stages[8]);
						} else {
							feat = new Feature({
								geometry : new LineString(points.slice(i, i+2)),
								name : 'stage9',
							});
							feat.setStyle(stages[9]);
						}
						lociPhases.push(feat);
					}

					for(let i = 0; i < flight_phases.length; i++){
						console.log(flight_phases[i]);
					}

                    // create itineraryLayer
                    thisFlight.state.itineraryLayer = new VectorLayer({
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

                    // add itineraryLayer to map
                    map.addLayer(thisFlight.state.itineraryLayer);

					thisFlight.state.lociLayer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: [2,2,2,2],
                                width: 5
                            })
                        }),

                        source : new VectorSource({
                            features: lociPhases                        
						})
                    });

                    map.addLayer(thisFlight.state.lociLayer);
					thisFlight.state.lociLayer.setVisible(true);
					thisFlight.state.layer.setVisible(false);

                    // adding coordinates to events, if needed //
                    var events = [];
                    var eventPoints = [];
                    var eventOutlines = [];
                    if (thisFlight.state.eventsLoaded) {
                        events = thisFlight.state.events;
                        eventPoints = thisFlight.state.eventPoints;
                        eventOutlines = thisFlight.state.eventOutlines;
                        for (let i = 0; i < events.length; i++){
                            let line = new LineString(points.slice(events[i].startLine, events[i].endLine + 2));
                            eventPoints[i].setGeometry(line);                   // set geometry of eventPoint Features
                            eventOutlines[i].setGeometry(line);
                        }

                        // add eventLayer to front of map
                        let eventLayer = thisFlight.state.eventLayer;
                        let outlineLayer = thisFlight.state.eventOutlineLayer;
                        map.addLayer(outlineLayer);
                        map.addLayer(eventLayer);
                    }

                    let extent = thisFlight.state.layer.getSource().getExtent();
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
            this.state.layer.setVisible(this.state.pathVisible);


            // toggle visibility of events
            if (this.state.eventLayer != null) {
                this.state.eventLayer.setVisible(!this.state.eventLayer.getVisible());
                this.state.eventOutlineLayer.setVisible(!this.state.eventOutlineLayer.getVisible());
            }
            // toggle visibility of itinerary
            this.state.itineraryLayer.setVisible(this.state.pathVisible);

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
                <Itinerary showMap={() => {this.props.showMap();}} itinerary={flightInfo.itinerary} color={this.state.color} coordinates={this.state.coordinates} nanOffset={this.state.nanOffset} parent={this} flightColorChange={this.flightColorChange}/>
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
