//Static class to assist with generation of LOC-I Index and Stall Index 'heatmaps'
import {Vector as VectorSource} from 'ol/source.js';
import { Vector as VectorLayer} from 'ol/layer.js';
import { Stroke, Style} from 'ol/style.js';
import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';


// So the weights w0 and w1 are for the weighted average
// They should add to 1.0 so if one of them is 0, the resulting color
// will just be the other color (e.g. w0 is 0 then the resulting color will be the same as c1)
function interpolateColors(c0, w0, c1, w1) {
    const new_color = [0.0, 0.0, 0.0];
    // red = 0, green = 1, blue = 2
    for (let i = 0; i < 3; i++) {
        new_color[i] = Math.round(w0 * c0[i] + w1 * c1[i]);
    }
    return new_color;
}

// loc_percentage should be between 0 and 1.0
// This will get the color for a given p(LOC)
// This can probably be made cleaner / not use if statements and just use lists but im lazy
function paletteAt(loc_probability) {

    let c0, c1, weight, w0, w1;

    if (loc_probability < 0.8) {
        c0 = [0, 255, 0];   // green
        c1 = [255, 255, 0]; // yellow

        // This will be a proportion between 0 and 1 since the max value for loc_p = 0.8 and min is 0
        weight = loc_probability / 0.8;
        w0 = 1.0 - weight; // if weight is 1, we want there to be no green and all yellow
        w1 = weight;

        return interpolateColors(c0, w0, c1, w1);

    } else if (loc_probability >= 0.8 && loc_probability < 1.0) {

        // Our range of loc_p values is 0.8 to 1.0, so a distance of 0.2
        c0 = [255, 255, 0]; //yellow
        c1 = [255, 0, 0];   //red

        // The minimum value of this will be 0.0 and max is 0.2
        const numerator = loc_probability - 0.8;

        // value range is 0.0 to 1.0
        weight = numerator / 0.2;
        w0 = 1.0 - weight;
        w1 = weight;

        return interpolateColors(c0, w0, c1, w1);

    } else {

        return [255, 0, 0]; // red

    }
    
}


function paletteGenerator(colors, pos) {
    return function (p) {
        const length = colors.length;
        for (let i = 0; i < length - 1; i++) {
            if (p <= pos[i + 1]) {
                const diff = pos[i + 1] - pos[i];
                const w0 = 1 - (p - pos[i]) / diff;
                const w1 = 1 - (pos[i + 1] - p) / diff;
                return interpolateColors(colors[i], w0, colors[i + 1], w1);
            }
        }
        return colors[length - 1];
    };
}

/**
 * Generates the layer for Stall Index
 *
 * @param spData the DoubleTimeSeries with the stall index data
 * @param layers the collection of layers to add this new layer to
 * @param flight the flight object that has data pertaining to the flight
 */
function generateStallLayer(spData, layers, flight) {
    const spPhases = [], spOutlinePhases = [];
    if (spData != null) {
        for(let i = 0; i < spData.length; i++){
            const val = spData[i];
            const feat = new Feature({
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

            const outFeat = new Feature({
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

    const spLayer = new VectorLayer({
        name : 'Stall Index' ,
        description : 'Stall Index',
        nMap : false,
        disabled : (spData == null),
        source : new VectorSource({
            features: spPhases                        
        })
    });

    const spLayerOutline = new VectorLayer({
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
    layers.push(spLayerOutline, spLayer);
}

/**
 * Generates the layer for LOC-I Index
 *
 * @param spData the DoubleTimeSeries with the LOC-I index data
 * @param layers the collection of layers to add this new layer to
 * @param flight the flight object that has data pertaining to the flight
 */
function generateLOCILayer(lociData, layers, flight) {
    const lociPhases = [], lociOutlinePhases = [];
    if (lociData != null) {
        for(let i = 0; i < lociData.length; i++){
            const val = lociData[i];
            const feat = new Feature({
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

            const outFeat = new Feature({
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

    const lociLayer = new VectorLayer({
        name : 'LOC-I Index' ,
        description : 'LOC-I Index' ,
        nMap : false,
        disabled : (lociData == null),

        source : new VectorSource({
            features: lociPhases
        })
    });

    const lociLayerOutline = new VectorLayer({
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
    layers.push(lociLayerOutline, lociLayer);
}

export {generateStallLayer, generateLOCILayer, paletteAt, paletteGenerator };
