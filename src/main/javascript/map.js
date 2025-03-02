import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Overlay from 'ol/Overlay';
import {Map, View} from 'ol';
import BingMaps from 'ol/source/BingMaps.js';
import {fromLonLat, toLonLat} from 'ol/proj.js';
import TileLayer from 'ol/layer/Tile';
import XYZ from 'ol/source/XYZ.js';


import OSM from 'ol/source/OSM';

import { errorModal } from "./error_modal.js";

console.log("doing first load after setting state!");

var styles = [
    'Aerial',
    'AerialWithLabels',
    'Road',
    'RoadOnDemand'
];

var layers = [];
var i;
var ii = styles.length; //do the mapquest satellite differently

for (i = 0; i < ii; ++i) {
    layers.push(new TileLayer({
        visible: false,
        preload: Infinity,
        source: new BingMaps({
            key: 'At3fIeQ6GJzbuGZpfWYkVjMDuzTuhYeFjHTzkraUafn06HMY3Eq1hDvZUlqq5Ysf',
            imagerySet: styles[i]
        })
    }));
}

styles.push('SectionalCharts');
var tms_sec = new TileLayer({
    visible: false,
    preload: Infinity,
    source : new XYZ({
        url : "http://localhost:8187/sectional/{z}/{x}/{-y}.png"}),
    maxZoom: 10
});

layers.push(tms_sec);


styles.push('IFREnrouteLowCharts');
var tms_enrl = new TileLayer({
    visible: false,
    preload: Infinity,
    source : new XYZ({
        url : " http://localhost:8187/ifr-enroute-low/{z}/{x}/{-y}.png"}),
    maxZoom: 10
});

layers.push(tms_enrl);


styles.push('IFREnrouteHighCharts');
var tms_enrh = new TileLayer({
    visible: false,
    preload: Infinity,
    source : new XYZ({
        url : "http://localhost:8187/ifr-enroute-high/{z}/{x}/{-y}.png"}),
    maxZoom: 10
});

layers.push(tms_enrh);

styles.push('TerminalAreaCharts');
var tms_tac = new TileLayer({
    visible: false,
    preload: Infinity,
    source : new XYZ({
        url : "http://localhost:8187/terminal-area/{z}/{x}/{-y}.png"}),
    maxZoom: 10
});

layers.push(tms_tac);

var center = fromLonLat([-97.0329, 47.9253]);

layers[2].setVisible(true);

var map = null;

function initializeMap() {
    map = new Map({
        target: 'map',
        layers: layers,
        loadTilesWhileInteracting: true,
        view: new View({
            center: center,
            zoom: 3,       // Initial zoom level when the map loads
            maxZoom: 10,   // Maximum zoom level the user can zoom to
            minZoom: 0
        })
    });
}
var container = document.getElementById('popup');
var content = document.getElementById('popup-content');
var closer = document.getElementById('popup-closer');
let overlays;
var overlay;
if (container == null) {
    overlays = [];
} else {
    overlay = new Overlay({
        element: container,
        autoPan: true,
        autoPanAnimation: {
            duration: 250
        }
    });
    overlays = [overlay];
}

var Colors = {};
Colors.names = {
    aqua: "#00ffff",
    //azure: "#f0ffff",
    beige: "#f5f5dc",
    black: "#000000",
    blue: "#0000ff",
    brown: "#a52a2a",
    cyan: "#00ffff",
    darkblue: "#00008b",
    darkcyan: "#008b8b",
    darkgrey: "#a9a9a9",
    darkgreen: "#006400",
    darkkhaki: "#bdb76b",
    darkmagenta: "#8b008b",
    darkolivegreen: "#556b2f",
    darkorange: "#ff8c00",
    darkorchid: "#9932cc",
    darkred: "#8b0000",
    darksalmon: "#e9967a",
    darkviolet: "#9400d3",
    fuchsia: "#ff00ff",
    gold: "#ffd700",
    green: "#008000",
    indigo: "#4b0082",
    //khaki: "#f0e68c",
    //lightblue: "#add8e6",
    //lightcyan: "#e0ffff",
    //lightgreen: "#90ee90",
    //lightgrey: "#d3d3d3",
    //lightpink: "#ffb6c1",
    //lightyellow: "#ffffe0",
    lime: "#00ff00",
    magenta: "#ff00ff",
    maroon: "#800000",
    navy: "#000080",
    olive: "#808000",
    orange: "#ffa500",
    pink: "#ffc0cb",
    purple: "#800080",
    violet: "#800080",
    red: "#ff0000",
    silver: "#c0c0c0",
    //white: "#ffffff",
    yellow: "#ffff00"
};

Colors.random = function() {
    var result;
    var count = 0;
    for (var prop in this.names)
        if (Math.random() < 1/++count)
            result = prop;
    return result;
};

Colors.randomValue = function() {
    var result;
    var count = 0;
    for (var prop in this.names)
        if (Math.random() < 1/++count)
            result = this.names[prop];
    return result;
};


export { map, styles, layers, Colors, initializeMap, overlay, container, closer, content };