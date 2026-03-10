import 'bootstrap';

import Overlay from 'ol/Overlay';
import { Map, View } from 'ol';
import { fromLonLat } from 'ol/proj.js';
import TileLayer from 'ol/layer/Tile';
import XYZ from 'ol/source/XYZ.js';

console.log("doing first load after setting state!");

/** Chart tile base URL (injected by backend or fallback for local dev). */
const getChartBase = () => (typeof chartTileBaseUrl !== 'undefined' ? chartTileBaseUrl : 'http://localhost:8187');

let map = null;
let styles = [];
let layers = [];

function initializeMap() {
    // Avoid creating a second map (e.g. DOMContentLoaded and FlightsPage componentDidMount both call this)
    if (map !== null) {
        console.warn("Map instance already exists, initialization aborted.");
        return;
    }

    // Azure Maps key is now injected from backend via template
    if (typeof azureMapsKey === 'undefined' || !azureMapsKey) {
        console.error("Azure Maps key is missing or undefined!");
        return;
    }

    // Target element may not exist yet when DOMContentLoaded runs before React has rendered (e.g. Flights page)
    if (!document.getElementById('map')) {
        return;
    }

    styles = [
        'Aerial',
        'Road',
        'RoadOnDemand'
    ];
    layers = [];
    let i;
    const ii = styles.length;
    for (i = 0; i < ii; ++i) {
        let url = '';
        if (styles[i] === 'Aerial') {
            url = `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.imagery&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}`;
        } else if (styles[i] === 'Road') {
            url = `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}`;
        } else if (styles[i] === 'RoadOnDemand') {
            url = `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.hybrid.road&zoom={z}&x={x}&y={y}&subscription-key=${azureMapsKey}`;
        }
        layers.push(new TileLayer({
            visible: false,
            preload: Infinity,
            source: new XYZ({ url })
        }));
    }

    const chartBase = getChartBase();
    styles.push('SectionalCharts');
    const tms_sec = new TileLayer({
        visible: false,
        preload: Infinity,
        source : new XYZ({
            url : chartBase + "/sectional/{z}/{x}/{-y}.png"})
    });
    layers.push(tms_sec);

    styles.push('IFREnrouteLowCharts');
    const tms_enrl = new TileLayer({
        visible: false,
        preload: Infinity,
        source : new XYZ({
            url : chartBase + "/ifr-enroute-low/{z}/{x}/{-y}.png"})
    });
    layers.push(tms_enrl);

    styles.push('IFREnrouteHighCharts');
    const tms_enrh = new TileLayer({
        visible: false,
        preload: Infinity,
        source : new XYZ({
            url : chartBase + "/ifr-enroute-high/{z}/{x}/{-y}.png"})
    });
    layers.push(tms_enrh);

    styles.push('TerminalAreaCharts');
    const tms_tac = new TileLayer({
        visible: false,
        preload: Infinity,
        source : new XYZ({
            url : chartBase + "/terminal-area/{z}/{x}/{-y}.png"})
    });
    layers.push(tms_tac);

    styles.push('HelicopterCharts');
    const heli = new TileLayer({
        visible: false,
        preload: Infinity,
        source : new XYZ({
            url : chartBase + "/helicopter/{z}/{x}/{-y}.png"})
    });
    layers.push(heli);

    const center = fromLonLat([-97.0329, 47.9253]);
    layers[2].setVisible(true);

    map = new Map({
        target: 'map',
        layers: layers,
        loadTilesWhileInteracting: true,
        view: new View({
            center: center,
            zoom: 1,       // Initial zoom level when the map loads
            maxZoom: 20,   // Maximum zoom level the user can zoom to
            minZoom: 0
        })
    });

    console.log("Initialized map instance: ", map);
}

function createBaseMapLayers(azureKey) {
    const styles = [
        'Aerial',
        'Road',
        'RoadOnDemand',
        'SectionalCharts',
        'IFREnrouteLowCharts',
        'IFREnrouteHighCharts',
        'TerminalAreaCharts',
        'HelicopterCharts'
    ];
    const layers = [];
    for (let i = 0; i < styles.length; ++i) {
        let url = '';
        const name = styles[i];
        if (name === 'Aerial') {
            url = `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.imagery&zoom={z}&x={x}&y={y}&subscription-key=${azureKey}`;
        } else if (name === 'Road') {
            url = `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.road&zoom={z}&x={x}&y={y}&subscription-key=${azureKey}`;
        } else if (name === 'RoadOnDemand') {
            url = `https://atlas.microsoft.com/map/tile?api-version=2.0&tilesetId=microsoft.base.hybrid.road&zoom={z}&x={x}&y={y}&subscription-key=${azureKey}`;
        } else if (name === 'SectionalCharts') {
            url = getChartBase() + "/sectional/{z}/{x}/{-y}.png";
        } else if (name === 'IFREnrouteLowCharts') {
            url = getChartBase() + "/ifr-enroute-low/{z}/{x}/{-y}.png";
        } else if (name === 'IFREnrouteHighCharts') {
            url = getChartBase() + "/ifr-enroute-high/{z}/{x}/{-y}.png";
        } else if (name === 'TerminalAreaCharts') {
            url = getChartBase() + "/terminal-area/{z}/{x}/{-y}.png";
        } else if (name === 'HelicopterCharts') {
            url = getChartBase() + "/helicopter/{z}/{x}/{-y}.png";
        }
        const layer = new TileLayer({
            visible: false,
            preload: Infinity,
            source: new XYZ({ url })
        });
        layer.set('name', name);
        layers.push(layer);
    }
    return { styles, layers };
}

// Do not auto-initialize on DOMContentLoaded: on the Flights page the #map div is inside
// React and may be hidden when DOMContentLoaded fires, so that init would create a
// zero-size map and cause componentDidMount to abort the correct init. Let the
// page (Flights, TTF, etc.) call initializeMap() from componentDidMount only.
// document.addEventListener('DOMContentLoaded', () => { initializeMap(); });

const container = document.getElementById('popup');
const content = document.getElementById('popup-content');
const closer = document.getElementById('popup-closer');
let overlay;

//Container is not null, create new overlay
if (container != null) {
    overlay = new Overlay({
        element: container,
        autoPan: true,
        autoPanAnimation: {
            duration: 250
        }
    });
}

const Colors = {};
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

Colors.random = function () {
    let result;
    let count = 0;
    for (const prop in this.names)
        if (Math.random() < 1 / ++count)
            result = prop;
    return result;
};

Colors.randomValue = function () {
    let result;
    let count = 0;
    for (const prop in this.names)
        if (Math.random() < 1 / ++count)
            result = this.names[prop];
    return result;
};

export { map, styles, layers, Colors, initializeMap, overlay, container, closer, content, createBaseMapLayers };