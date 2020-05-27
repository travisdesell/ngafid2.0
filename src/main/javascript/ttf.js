import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";
import { map, styles, layers, Colors } from "./map.js";

import {fromLonLat, toLonLat} from 'ol/proj.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import Draw from 'ol/interaction/Draw.js';

import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import Point from 'ol/geom/Point.js';
import { Filter } from './filter.js';


var moment = require('moment');

class TTFCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            mapVisible : false
        };

    }

    mapSelectChanged(style) {
        for (var i = 0, ii = layers.length; i < ii; ++i) {
            layers[i].setVisible(styles[i] === style);
        }
    }

    showMap() {
        if (this.state.mapVisible) return;

        if ( !$("#map-toggle-button").hasClass("active") ) {
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

        this.state.mapVisible = true;
        this.setState(this.state);

        $("#map-div").css("height", "50%");
        $("#map").show();

        $("#map").css("width", "100%");

    }

    hideMap() {
        if (!this.state.mapVisible) return;

        if ( $("#map-toggle-button").hasClass("active") ) {
            $("#map-toggle-button").removeClass("active");
            $("#map-toggle-button").attr("aria-pressed", false);
        }

        this.state.mapVisible = false;
        this.setState(this.state);

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

    ttfToCoordinateSeries(ttf) {
        var coordinates = [];
        for (var i = 0; i < ttf.lon.length; i++) {
            coordinates.push([ttf.lon[i], ttf.lat[i]]);
        }
        console.log(coordinates);
        return coordinates;
    }

    plotTTF(ttf) {
        var coordinates = this.ttfToCoordinateSeries(ttf);

        var points = [];
        for (var i = 0; i < coordinates.length; i++) {
            var point = fromLonLat(coordinates[i]);
            points.push(point);
        }

        var color = "#00ff00";
        console.log(color);

        let trackingPoint = new Feature({
                        geometry : new Point(points[0]),
                        name: 'TrackingPoint'
                    });

        let layer = new VectorLayer({
            style: new Style({
                stroke: new Stroke({
                    color: color,
                    width: 1.5
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
                    trackingPoint
                ]
            })
        });

        map.addLayer(layer);

        let extent = layer.getSource().getExtent();
        //console.log(extent);
        //map.getView().fit(extent, map.getSize());
    }

    onFetchClicked() {
        var startDateElement = document.getElementById("start");
        var endDateElement = document.getElementById("end");

        var submissionData = { startDate: startDateElement.value, endDate: endDateElement.value };
        var thisTTF = this;

        $.ajax({
            type: 'POST',
            url: '/protected/ttf',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                for (var i = 0; i < response.ttfs.length; i++)
                    thisTTF.plotTTF(response.ttfs[i]);
            },
            error : function(jqXHR, textStatus, errorThrown) {
                console.log(textStatus);
                console.log(errorThrown);
            },
            async: true
         });
    }

    render() {
        return (
            <div>
                <label for="start">Start date:</label>

                <input type="date" id="start" name="date-start"
                       value="2000-01-01"
                       min="2000-01-01" max="3000-12-31" />

                <label for="start">End date:</label>

                <input type="date" id="end" name="date-end"
                       value="2020-01-01"
                       min="2000-01-01" max="3000-12-31" />

                <button onClick={()=>this.onFetchClicked()}>Fetch</button>
            </div>
        );
    }
}

let ttfCard = null;
//check to see if flights has been defined already. unfortunately
//the navbar includes flights.js (bad design) for the navbar buttons
//to toggle flights, etc. So this is a bit of a hack.
ttfCard = ReactDOM.render(
    <TTFCard />,
    document.querySelector('#ttf-card')
);
navbar.setFlightsCard(ttfCard);

console.log("rendered ttfCard!");

export { ttfCard };
