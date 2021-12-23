//Static class to assist with generation of LOC-I Index and Stall Index 'heatmaps'
import {Vector as VectorSource} from 'ol/source.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import {fromLonLat, toLonLat} from 'ol/proj.js';
import Point from 'ol/geom/Point.js';


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

function mapLayerIndexOf(layers, layerName) {
    for (var i = 0; i < layers.length; i++) {
        if (layers[i].get('name') == layerName) {
            return i;
        }
    }
    return -1;
}


function paletteGenerator(colors, pos) {
    return function (p) {
        let length = colors.length;
        for (var i = 0; i < length - 1; i++) {
            if (p <= pos[i + 1]) {
                let diff = pos[i + 1] - pos[i];
                console.log(i);
                console.log(pos);
                console.log(diff);
                let w0 = 1 - (p - pos[i]) / diff;
                let w1 = 1 - (pos[i + 1] - p) / diff;
                console.log(w0);
                console.log(w1);
                return interpolateColors(colors[i], w0, colors[i + 1], w1);
            }
        }
        return colors[length - 1];
    }
}

function generateBaseLayer(flight, lowerConstraint, upperConstraint) {
    let coordinates = flight.coordinates;
    let nanOffset = flight.nanOffset;

    flight.state.points = new Array();
    let points = flight.state.points;
    for (var i = lowerConstraint; i < upperConstraint; i++) {
        var point = fromLonLat(coordinates[i]);
        points.push(point);
    }
    console.log("pts");
    console.log(points);

    var color = flight.state.color;
    //console.log(color);

    flight.state.trackingPoint = new Feature({
                    geometry : new Point(points[0]),
                    name: 'TrackingPoint'
                });

    flight.state.trackingPoint.setId(points[0]);

    flight.state.layers = new Array();
    let layers = flight.state.layers;


    // adding itinerary (approaches and takeoffs) to flightpath 
    var itinerary = flight.props.flightInfo.itinerary;
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

    flight.state.baseLayer = new VectorLayer({
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
                flight.state.trackingPoint,
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

    let baseLayer = flight.state.baseLayer;

    baseLayer.flightState = flight;
    let flightInfo = flight.props.flightInfo;

    flight.state.pathVisible = true;
    flight.state.itineraryVisible = true;
    flight.state.nanOffset = nanOffset;
    flight.state.coordinates = coordinates;
    flight.state.points = points;

    // toggle visibility of itinerary
    layers.push(baseLayer, phaseLayer);
}

/**
 * Generates the layer for Stall Index
 *
 * @param spData the DoubleTimeSeries with the stall index data
 * @param layers the collection of layers to add this new layer to
 * @param flight the flight object that has data pertaining to the flight
 */
function generateStallLayer(spData, layers, flight, lowerConstraint, upperConstraint) {
    console.log("generating LOCI layer");
    var spPhases = [], spOutlinePhases = [];
    if (spData != null) {
        for(let i = lowerConstraint; i < upperConstraint; i++){
            let val = spData[i];
            var feat = new Feature({
                geometry : new LineString(flight.state.points.slice(i, i+2)),
                name : "SP"
            });
            feat.setId(i);
            feat.parent = 'Stall Index';
            feat.setStyle([
              new Style({
                stroke: new Stroke({
                  color: paletteAt(val),
                  width: 8
                })
              })
            ]);

            let outFeat = new Feature({
                geometry : new LineString(flight.state.points.slice(i, i+2)),
                name : "Stall Index Outline"
            });

            outFeat.setId(i);
            outFeat.parent = 'Stall Index';

            spOutlinePhases.push(outFeat);
            spPhases.push(feat);
        }
    }

    spPhases.push(flight.state.trackingPoint);

    let spLayer = new VectorLayer({
        name : 'Stall Index' ,
        description : 'Stall Index',
        nMap : false,
        disabled : (spData == null),
        source : new VectorSource({
            features: spPhases                        
        })
    });

    let spLayerOutline = new VectorLayer({
        name : 'Stall Index' ,
        description : 'Stall Index Outline',
        nMap : true,
        disabled : (spData == null),
        style : new Style({
            stroke: new Stroke({
                color: flight.state.color,
                width : 12

            })
        }),
        source : new VectorSource({
            features: spOutlinePhases                        
        })
    });

    spLayer.flightState = flight;
    flight.state.layers.push(spLayerOutline, spLayer);
}

/**
 * Generates the layer for LOC-I Index
 *
 * @param spData the DoubleTimeSeries with the LOC-I index data
 * @param layers the collection of layers to add this new layer to
 * @param flight the flight object that has data pertaining to the flight
 */
function generateLOCILayer(lociData, layers, flight, lowerConstraint, upperConstraint) {
    console.log("generating LOCI layer from " + lowerConstraint + " to " + upperConstraint);
    var lociPhases = [], lociOutlinePhases = [];
    if (lociData != null) {
        for(let i = lowerConstraint; i < upperConstraint; i++){
            let val = lociData[i];
            var feat = new Feature({
                geometry : new LineString(flight.state.points.slice(i, i+2)),
                name : "LOC-I Index"
            });
            feat.setId(i);
            feat.parent = 'LOC-I Index';
            feat.setStyle([
              new Style({
                stroke: new Stroke({
                  color: paletteAt(val),
                  width: 8
                })
              })
            ]);

            let outFeat = new Feature({
                geometry : new LineString(flight.state.points.slice(i, i+2)),
                name : "LOC-I Index Outline"
            });

            outFeat.setId(i);
            outFeat.parent = 'LOC-I Index';

            lociPhases.push(feat);
            lociOutlinePhases.push(outFeat);
        }
    }


    lociPhases.push(flight.state.trackingPoint);

    let lociLayer = new VectorLayer({
        name : 'LOC-I Index' ,
        description : 'LOC-I Index' ,
        nMap : false,
        disabled : (lociData == null),

        source : new VectorSource({
            features: lociPhases
        })
    });

    let lociLayerOutline = new VectorLayer({
        name : 'LOC-I Index Outline' ,
        description : 'LOC-I Index' ,
        nMap : true,
        disabled : (lociData == null),
        style : new Style({
            stroke: new Stroke({
                color: flight.state.color,
                width : 12
            })
        }),
        source : new VectorSource({
            features: lociOutlinePhases                        
        })
    });


    lociLayer.flightState = flight;

    for (let i = 0; i < flight.state.layers.length; i++) {
        if (flight.state.layers[i].get('name').includes("LOC-I")) {
            flight.state.layers.splice(i, 1);
        }
    }
    // let lociOutlineIndex = mapLayerIndexOf(layers, lociLayerOutline.get('name'));
    // if (lociOutlineIndex >= 0) {
    //     layers[lociOutlineIndex] = lociLayerOutline;
    //     console.log("setting layer as it already exists!");
    // } else {
    //     layers.push(lociLayerOutline);
    // }

    // let lociLayerIndex = mapLayerIndexOf(layers, lociLayer.get('name'));
    // if (lociLayerIndex >= 0) {
    //     console.log(layers);
    //     layers[lociLayerIndex] = lociLayerOutline;
    //     console.log("setting layer as it already exists!");
    //     console.log(layers);
    // } else {
    //     flight.state.layers.push(lociLayer);
    // }
    flight.state.layers.push(lociLayerOutline);
    flight.state.layers.push(lociLayer);
}

export {generateBaseLayer, generateStallLayer, generateLOCILayer, paletteAt, paletteGenerator };
