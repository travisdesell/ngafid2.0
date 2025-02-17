import "bootstrap";
import React, { Component } from "react";
import ReactDOM from "react-dom";

import SignedInNavbar from "./signed_in_navbar.js";
import { map, styles, layers, Colors, initializeMap } from "./map.js";

import { Group, Vector as VectorLayer } from "ol/layer.js";
import { Vector as VectorSource } from "ol/source.js";
import { Circle, Fill, Icon, Stroke, Style } from "ol/style.js";

import { errorModal } from "./error_modal.js";
import { confirmModal } from "./confirm_modal.js";

import { Filter } from "./filter.js";
import { Paginator } from "./paginator_component.js";
import { FlightsCard } from "./flights_card_component.js";

import Plotly from "plotly.js";

import { timeZones } from "./time_zones.js";
import CesiumPage from "./ngafid_cesium.js";
import { linearRingLength } from "ol/geom/flat/length.js";
import { View } from "ol";

function invalidString(str) {
  return str == null || str.length < 0 || /^\s*$/.test(str);
}

/*
var airframes = [ "PA-28-181", "Cessna 172S", "PA-44-180", "Cirrus SR20"  ];
var tailNumbers = [ "N765ND", "N744ND", "N771ND", "N731ND", "N714ND", "N766ND", "N743ND" , "N728ND" , "N768ND" , "N713ND" , "N732ND", "N718ND" , "N739ND" ];
var doubleTimeSeriesNames = [ "E1 CHT1", "E1 CHT2", "E1 CHT3" ];
var visitedAirports = [ "GFK", "FAR", "ALB", "ROC" ];
*/
// var tagNames = ["Tag A", "Tag B"];
var rules = [

	{
    	name: "Has Any Event(s)",
    	conditions: [
        	{
            	type: "select",
            	name: "airframes",
            	options: airframes,
        	}
    	]
	},

  {
	name: "Airframe",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["is", "is not"],
  	},
  	{
    	type: "select",
    	name: "airframes",
    	options: airframes,
  	},
	],
  },

  {
	name: "Tail Number",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["is", "is not"],
  	},
  	{
    	type: "select",
    	name: "tail numbers",
    	options: tailNumbers,
  	},
	],
  },

  {
	name: "System ID",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["is", "is not"],
  	},
  	{
    	type: "select",
    	name: "system id",
    	options: systemIds,
  	},
	],
  },

  {
	name: "Duration",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "number",
    	name: "hours",
  	},
  	{
    	type: "number",
    	name: "minutes",
  	},
  	{
    	type: "number",
    	name: "seconds",
  	},
	],
  },

  {
	name: "Start Date and Time",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "datetime-local",
    	name: "date and time",
  	},
  	{
    	type: "select",
    	name: "timezone",
    	options: timeZones,
  	},
	],
  },

  {
	name: "End Date and Time",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "datetime-local",
    	name: "date and time",
  	},
  	{
    	type: "select",
    	name: "timezone",
    	options: timeZones,
  	},
	],
  },

  {
	name: "Flight ID",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "number",
    	name: "number",
  	},
	],
  },

  {
	name: "Start Date",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "date",
    	name: "date",
  	},
	],
  },

  {
	name: "End Date",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "date",
    	name: "date",
  	},
	],
  },

  {
	name: "Start Time",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "time",
    	name: "time",
  	},
  	{
    	type: "select",
    	name: "timezone",
    	options: timeZones,
  	},
	],
  },

  {
	name: "End Time",
	conditions: [
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "time",
    	name: "time",
  	},
  	{
    	type: "select",
    	name: "timezone",
    	options: timeZones,
  	},
	],
  },

  {
	name: "Parameter",
	conditions: [
  	{
    	type: "select",
    	name: "statistic",
    	options: ["min", "avg", "max"],
  	},
  	{
    	type: "select",
    	name: "doubleSeries",
    	options: doubleTimeSeriesNames,
  	},
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "number",
    	name: "number",
  	},
	],
  },

  {
	name: "Airport",
	conditions: [
  	{
    	type: "select",
    	name: "airports",
    	options: visitedAirports,
  	},
  	{
    	type: "select",
    	name: "condition",
    	options: ["visited", "not visited"],
  	},
	],
  },

  {
	name: "Runway",
	conditions: [
  	{
    	type: "select",
    	name: "runways",
    	options: visitedRunways,
  	},
  	{
    	type: "select",
    	name: "condition",
    	options: ["visited", "not visited"],
  	},
	],
  },

  {
	name: "Event Count",
	conditions: [
  	{
    	type: "select",
    	name: "eventNames",
    	options: eventNames,
  	},
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "number",
    	name: "number",
  	},
	],
  },

  {
	name: "Event Severity",
	conditions: [
  	{
    	type: "select",
    	name: "eventNames",
    	options: eventNames,
  	},
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "number",
    	name: "number",
  	},
	],
  },

  {
	name: "Event Duration",
	conditions: [
  	{
    	type: "select",
    	name: "eventNames",
    	options: eventNames,
  	},
  	{
    	type: "select",
    	name: "condition",
    	options: ["<=", "<", "=", ">", ">="],
  	},
  	{
    	type: "number",
    	name: "number",
  	},
	],
  },

  {
	name: "Tag",
	conditions: [
  	{
    	type: "select",
    	name: "flight_tags",
    	options: tagNames,
  	},
  	{
    	type: "select",
    	name: "condition",
    	options: ["Is Associated", "Is Not Associated"],
  	},
	],
  },
];

const sortableColumns = new Map();

sortableColumns.set("Flight ID", "id");
sortableColumns.set(
  "Flight Length (Number of Valid Data Points)",
  "number_rows"
);
sortableColumns.set("Start Date and Time", "start_time");
sortableColumns.set("End Date and Time", "end_time");
sortableColumns.set("Number of Airports Visited", "airports_visited");
sortableColumns.set("Number of Tags Associated", "flight_tags");
sortableColumns.set("Total Event Count", "events");
sortableColumns.set("System ID", "system_id");
sortableColumns.set("Tail Number", "tail_number");
sortableColumns.set("Airframe", "airframe_id");
sortableColumns.set("Number of Takeoffs/Landings", "itinerary");

class FlightsPage extends React.Component {

  constructor(props) {

	super(props);

	this.state = {
  	containerExpanded : undefined,
  	filterVisible: true,
  	filterSelected: true,
  	plotVisible: false,
  	plotSelected: false,
  	mapVisible: false,
  	mapSelected: false,
  	mapStyle: "Road",
  	flightsRef: React.createRef(),
  	layers: [],
  	flights: undefined, //start out with no specified flights
  	sortColumn: "Start Date and Time", //need to define a default here, flt# will alias to primary key server side
  	sortingOrder: "Descending", //need to define a default here, descending is default
  	storedFilters: this.getStoredFilters(),
    cesiumVisible: false,
  	filters: {
    	type: "GROUP",
    	condition: "AND",
    	filters: [],
  	},

  	//needed for paginator
  	currentPage: 0,
  	numberPages: 1,
  	pageSize: 10,
	};

    this.cesiumRef = React.createRef();
	this.navRef = React.createRef();

  }

  mapSelectChanged(newMapStyle) {

	for (var i = 0, ii = layers.length; i < ii; ++i) {
  	console.log("Setting layer " + i + " to:" + (styles[i] === newMapStyle));
  	layers[i].setVisible(styles[i] === newMapStyle);
	}

	console.log("Map style changed to: '" + newMapStyle + "'!");
	this.setMapStyle(newMapStyle);

  }

  mapLayerChanged(newMapStyle) {

	console.log("changing path to: " + newMapStyle);
	console.log(this.state.selectableLayers);

	for (let i = 0; i < this.state.selectableLayers.length; i++) {

  	let layer = this.state.selectableLayers[i];
  	let name = layer.values_.name;

  	if (name == newMapStyle) {
    	layer.setVisible(true);
    	console.log("Setting layer " + name + " to visible");
  	} else {
    	layer.setVisible(false);
    	console.log("Setting layer " + name + " to not visible");
  	}

	}

	console.log("Map layer changed to: '" + newMapStyle + "'!");
	this.setMapStyle(newMapStyle);

  }

  setMapStyle(newMapStyle) {

    	this.state.mapStyle = newMapStyle;
    	this.setState(this.state);

  }

  setSortingColumn(column) {
	console.log("sorting by: " + column);
	this.state.sortColumn = column;
	this.setState(this.state);

	this.submitFilter(true);
  }

  getSortingColumn() {
	return this.state.sortColumn;
  }

  setSortingOrder(order) {

	if (order != this.state.sortingOrder) {
  	console.log("sorting in " + order + " order");
  	this.state.sortingOrder = order;
  	this.setState(this.state);
  	this.submitFilter(true);
	}

  }

  getSortingOrder() {
	return this.state.sortingOrder;
  }

    addCesiumFlightPhase(phase, flightId) {
        this.cesiumRef.current.addPhaseEntity(phase, flightId);
    }

    addCesiumEventEntity(event, flightId) {
        this.cesiumRef.current.addEventEntity(event, flightId);
    }

    zoomToEventEntity(eventId, flightId) {
        this.cesiumRef.current.zoomToEventEntity(eventId, flightId);
    }

    removeCesiumFlight(flightId) {
        this.cesiumRef.current.removeFlightEntities(flightId);
    }
    addCesiumFlight(flightId, color) {

        console.log("add cesium flight");

        if (this.state.plotVisible) {
            this.hidePlot();
        }
        if (this.state.mapVisible) {
            this.toggleMap();
        }

        console.log("in showCesium flight id from flight component " + flightId);

        this.cesiumRef.current.addFlightEntity(flightId, color);
        this.showCesiumMap();
    }


    setMapStyle(style) {
         this.setState({
            mapStyle : style
        });
    }

    setSortingColumn(column) {
        console.log("sorting by: " + column);
        this.state.sortColumn = column;
        this.setState(this.state);

        this.submitFilter(true);
    }

    getSortingColumn() {
        return this.state.sortColumn;
    }

    setSortingOrder(order) {
        if (order != this.state.sortingOrder) {
            console.log("sorting in " + order + " order");
            this.state.sortingOrder = order;
            this.setState(this.state);
            this.submitFilter(true);
        }
    }

    getSortingOrder() {
        return this.state.sortingOrder;
    }

    addCesiumFlightPhase(phase, flightId) {
        this.cesiumRef.current.addPhaseEntity(phase, flightId);
    }

    addCesiumEventEntity(event, flightId) {
        this.cesiumRef.current.addEventEntity(event, flightId);
    }

    zoomToEventEntity(eventId, flightId) {
        this.cesiumRef.current.zoomToEventEntity(eventId, flightId);
    }

    removeCesiumEntity(flightId) {
        this.cesiumRef.current.removeEntity(flightId);
    }

    addCesiumFlight(flightId, color) {

        console.log("add cesium flight");

        if (this.state.plotVisible) {
            this.hidePlot();
        }
        if (this.state.mapVisible) {
            this.toggleMap();
        }

        console.log("in showCesium flight id from flight component " + flightId);

        this.cesiumRef.current.addFlightEntity(flightId, color);
        this.showCesiumMap();
    }

    showCesiumMap() {

        if (!$("#cesium-toggle-button").hasClass("active")) {
            $("#cesium-toggle-button").addClass("active");
            $("#cesium-toggle-button").attr("aria-pressed", true);
        }
        this.state.cesiumVisible = true;
        this.setState(this.state);
        $("#cesium-div").css("height", "100%");
        $("#cesium-div").show();

    }

    showMap() {

        console.log("new show map implementation");

        if (this.state.mapVisible) return;

        if (this.state.cesiumVisible)
            console.log("hiding cesium (toggle)");
            this.hideCesiumMap();



        console.log("in flight.js showmap");

        if ( !$("#map-toggle-button").hasClass("active") ) {
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

	this.state.mapVisible = true;
	this.setState(this.state);

	this.resolveDisplay();
	this.resolveDisplay();  //TODO: Fix the map inexplicably freezing when toggling unless this is called twice

  }

  hideMap() {

	if (!this.state.mapVisible)
    	return;

	if ($("#map-toggle-button").hasClass("active")) {
  	$("#map-toggle-button").removeClass("active");
  	$("#map-toggle-button").attr("aria-pressed", false);
	}

	this.state.mapVisible = false;
	this.setState(this.state);

	this.resolveDisplay();

  }

    toggleCamera(flightId) {

        this.cesiumRef.current.toggleCamera(flightId);
    }
    hideCesiumMap() {
        if (!this.state.cesiumVisible) return;

        if ( $("#cesium-toggle-button").hasClass("active") ) {
            $("#cesium-toggle-button").removeClass("active");
            $("#cesium-toggle-button").attr("aria-pressed", false);
        }

        this.state.cesiumVisible = false;
        this.setState(this.state);

        $("#cesium-div").hide();

    }

  toggleMap() {

	//Map is expanded, collapse it
	if (this.state.containerExpanded === "map-container")
    	this.expandContainer(undefined);

	if (this.state.mapVisible)
    	this.hideMap();
	else
    	this.showMap();

  }

    toggleCesiumMap() {
        if (this.state.cesiumVisible) {
            this.hideCesiumMap();
        } else {

            this.showCesiumMap();
        }

    }

    showPlot() {
        if (this.state.plotVisible) return;

	if (!$("#plot-toggle-button").hasClass("active")) {
  	$("#plot-toggle-button").addClass("active");
  	$("#plot-toggle-button").attr("aria-pressed", true);
	}

	this.state.plotVisible = true;
	this.setState(this.state);

	this.resolveDisplay();

  }

  hidePlot() {

	//Plot is already hidden
	if (!this.state.plotVisible)
    	return;

	if ($("#plot-toggle-button").hasClass("active")) {
  	$("#plot-toggle-button").removeClass("active");
  	$("#plot-toggle-button").attr("aria-pressed", false);
	}

	this.state.plotVisible = false;
	this.setState(this.state);

	this.resolveDisplay();

  }

  togglePlot() {

	//Plot is expanded, collapse it
	if (this.state.containerExpanded === "plot-container")
        	this.expandContainer(undefined);

    	if (this.state.plotVisible)
        	this.hidePlot();
    	else
        	this.showPlot();

    	this.resolveDisplay();

	}

	toggleFilter() {

    	let newFilterState = (!this.state.filterVisible);

    	console.log("Toggling filterVisible to: " + newFilterState);

    	this.state.filterVisible = newFilterState;
    	this.setState(this.state);

    	this.resolveDisplay();

	}

	expandContainer(targetContainerName) {

    	let newExpandedContainerValue;

    	//Already expanded, so collapse
    	if (this.state.containerExpanded !== undefined)
        	newExpandedContainerValue = undefined;

    	//Not expanded, so expand
    	else
        	newExpandedContainerValue = targetContainerName;

    	this.state.containerExpanded = newExpandedContainerValue;
    	this.setState(this.state);

    	this.resolveDisplay();

	}

	resolveDisplayExpanded() {

    	console.log("Handling Expanded Display State...");

    	//...
    	if (true) {

        	$("#plot-map-div").css("padding", "0.5em");

        	$("#plot-map-div").css("height", "100%");
        	$("#plot-map-div").css("min-height", "100%");
        	$("#plot-map-div").css("max-height", "100%");

        	$("#plot-map-div").css("width", "100%");
        	$("#plot-map-div").css("min-width", "100%");
        	$("#plot-map-div").css("max-width", "100%");

    	}

    	//Check if the plot is expanded
    	let plotVisible = (this.state.containerExpanded === "plot-container");

    	//Check if the map is expanded
    	let mapVisible = (this.state.containerExpanded === "map-container");

    	//...
    	if (plotVisible) {

        	$("#map-container").hide();

        	$("#plot").show();
        	Plotly.Plots.resize("plot");

        	$("#plot-container").css("height", "100%");
        	$("#plot-container").css("min-height", "100%");
        	$("#plot-container").css("max-height", "100%");

    	}

    	//...
    	if (mapVisible) {

        	$("#plot-container").hide();


        	$("#map-container").css("margin-top", "0");

        	$("#map-container").css("height", "100%");
        	$("#map-container").css("min-height", "100%");
        	$("#map-container").css("max-height", "100%");


        	$("#map").show();
        	map.updateSize();

        	// let vw = Math.max(document.documentElement.clientWidth || 0, window.innerWidth || 0);
        	// let vh = Math.max(document.documentElement.clientHeight || 0, window.innerHeight || 0);
        	// map.setView({ width: vw, height: vh });

    	}

	}

	resolveDisplay() {

    	console.log(`Container Expanded: ${this.state.containerExpanded}`);

    	//Check for expanded windows
    	if (this.state.containerExpanded !== undefined) {
        	this.resolveDisplayExpanded();
        	return;
    	}

    	//Check if filter is visible
    	let filterVisible = this.state.filterVisible;

    	//Check if the plot is visible
    	let plotVisible = this.state.plotVisible;

    	//Check if the map is visible
    	let mapVisible = this.state.mapVisible;

    	//Check if the plot AND map are visible
    	let plotAndMapVisible = (plotVisible && mapVisible);

    	//Check if the plot OR map are visible
    	let plotOrMapVisible = (plotVisible || mapVisible);


    	console.log(`Resolving Display State... FILTER: ${filterVisible}, PLOT: ${plotVisible}, MAP: ${mapVisible}`);


    	//Either the plot or map are visible...
    	if (plotOrMapVisible) {

        	$("#plot-map-div").css("padding", "0.5em 0.0em 1.0em 0.5em");

        	$("#plot-map-div").css("height", "100%");

        	$("#plot-map-div").css("width", "45%");
        	$("#plot-map-div").css("min-width", "45%");
        	$("#plot-map-div").css("max-width", "45%");

    	} else {	//Neither the plot or map are visible...

        	$("#plot-map-div").css("padding", "0");

        	$("#plot-map-div").css("height", "0%");

        	$("#plot-map-div").css("width", "0%");
        	$("#plot-map-div").css("min-width", "0%");
        	$("#plot-map-div").css("max-width", "0%");
    	}

    	//If the plot is visible...
    	if (plotVisible) {

        	//...Show the plot element
        	$("#plot").show();

        	//...
        	$("#plot-container").css("height", "50%");
        	$("#plot-container").css("min-height", "50%");
        	$("#plot-container").css("max-height", "50%");

        	//...
        	Plotly.Plots.resize("plot");

    	} else {	//If the plot is not visible...

        	//...Hide the plot element
        	$("#plot").hide();

    	}

    	//If the map is visible...
    	if (mapVisible) {

        	//...Show the map element
			$("#plot-map-div").css("height", "50%");
        	$("#map").show();
			$("#map").css("height", "50%");

        	//...
        	$("#map-container").css("height", "50%");
        	$("#map-container").css("min-height", "50%");
        	$("#map-container").css("max-height", "50%");

        	//...Update the map size
        	map.updateSize();

        	//...Apply margin to the map container if the plot is visible
        	$("#map-container").css("margin-top", plotVisible ? "0.50em" : "0.00em");

    	} else {	//If the map is not visible...

        	//...Hide the map element
        	$("#map").hide();

    	}

    	//Apply margin to the flights card container if the filter is visible
    	$("#flights-card-container").css("margin-top", filterVisible ? "0.50em" : "0.00em");

    	//Display the plot and map containers if they are visible
    	$("#plot-container").css("display", plotVisible ? "block" : "none");
    	$("#map-container").css("display", mapVisible ? "block" : "none");

	}

  setFilter(filter) {
	this.setState({
  	filters: filter,
	});
  }

  getStoredFilters() {
	let storedFilters = [];

	$.ajax({
  	type: "GET",
  	url: "/protected/stored_filters",
  	dataType: "json",
  	success: function (response) {
    	console.log("received filters response: ");
    	console.log(response);

    	storedFilters = response;
  	},
  	error: function (jqXHR, textStatus, errorThrown) {},
  	async: false,
	});

	return storedFilters;
  }


	transformHasAnyEvent(filters) {

    	let newFilters = [];
    	filters.forEach((filter) => {

        	if (filter.inputs && filter.inputs[0] === "Has Any Event(s)") {

            	console.log("Rebuilding filter for 'Has Any Event(s)' as 'Event Count' > 0 for all events for the given airframe...");

            	let airframe = filter.inputs[1];
            	newFilters.push({
                	type: "GROUP",
                	condition: "AND",
                	filters: [
                    	{
                        	type: "RULE",
                        	inputs: ["Airframe", "is", airframe]
                    	},
                    	{
                        	type: "GROUP",
                        	condition: "OR",
                        	filters: eventNames.map((eventName) => ({
                            	type: "RULE",
                            	inputs: ["Event Count", eventName, ">", "0"]
                        	}))
                    	}
                	]
            	});

        	//Attempt to recursively transform nested filters...
        	} else if (filter.filters) {
            	newFilters.push({
                	...filter,
                	filters: transformAirframeEventsCountFilter(filter.filters)
            	});
        	} else {
            	newFilters.push(filter);
        	}

    	});

    	return newFilters;

	};

  submitFilter(resetCurrentPage = false) {
	console.log(
  	"submitting filter! currentPage: " +
    	this.state.currentPage +
    	", pageSize: " +
    	this.state.pageSize +
    	" sortByColumn: " +
    	this.state.sortColumn
	);

	console.log("Submitting filters:");
	console.log(this.state.filters);

	$("#loading").show();

	//reset the current page to 0 if the page size or filter
	//have changed
	let currentPage = this.state.currentPage;
	if (resetCurrentPage === true) {
  	currentPage = 0;
	}

	//Transform the 'Has Any Event(s)' filter
	let originalFilters = this.state.filters.filters;
	this.state.filters.filters = this.transformHasAnyEvent(this.state.filters.filters);

	var submissionData = {
  	filterQuery: JSON.stringify(this.state.filters),
  	currentPage: currentPage,
  	pageSize: this.state.pageSize,
  	sortingColumn: sortableColumns.get(this.state.sortColumn),
  	sortingOrder: this.state.sortingOrder,
	};

	console.log(submissionData);

	//Undo the transformation
	this.state.filters.filters = originalFilters;

	let flightsPage = this;

	$.ajax({
  	type: "POST",
  	url: "/protected/get_flights",
  	data: submissionData,
  	dataType: "json",
  	timeout: 0, //set timeout to be unlimited for slow queries
  	success: function (response) {
    	console.log(response);

    	$("#loading").hide();

    	if (response.errorTitle) {
      	console.log("displaying error modal!");
      	errorModal.show(response.errorTitle, response.errorMessage);
      	return false;
    	}

    	console.log("got response: " + response + " " + response.size);

    	//get page data
    	if (response == "NO_RESULTS") {
      	errorModal.show(
        	"No flights found with the given parameters!",
        	"Please try a different query."
      	);
    	} else {
      	flightsPage.setState({
        	flights: response.flights,
        	currentPage: currentPage,
        	numberPages: response.numberPages,
      	});
    	}
  	},
  	error: function (jqXHR, textStatus, errorThrown) {
    	errorModal.show("Error Loading Flights", errorThrown);
    	$("#loading").hide();
  	},
  	async: true,
	});
  }

  setAvailableLayers(plotLayers) {
	console.log("changing selectable layers on navbar");
	console.log(plotLayers);

	this.setState({ selectableLayers: plotLayers });
  }

  //Tag Methods:
  //

  addTag(flightId, name, description, color) {
	if (invalidString(name) || invalidString(description)) {
  	errorModal.show(
    	"Error creating tag!",
    	"Please ensure the name and description fields are correctly filled out!"
  	);
  	return;
	}

	var submissionData = {
  	name: name,
  	description: description,
  	color: color,
  	id: flightId,
	};
	console.log("Creating a new tag for flight # " + this.state.flightId);

	let thisFlight = this;

	$.ajax({
  	type: "POST",
  	url: "/protected/create_tag",
  	data: submissionData,
  	dataType: "json",
  	success: function (response) {
    	console.log("received response: ");
    	console.log(response);
    	if (response != "ALREADY_EXISTS") {
      	for (var i = 0; i < thisFlight.state.flights.length; i++) {
        	let flight = thisFlight.state.flights[i];
        	if (flight.id == flightId) {
          	if (flight.tags != null && flight.tags.length > 0) {
            	flight.tags.push(response);
          	} else {
            	flight.tags = [response];
          	}
        	}
      	}
      	thisFlight.setState(thisFlight.state);
    	} else {
      	errorModal.show(
        	"Error creating tag",
        	"A tag with that name already exists! Use the dropdown menu to associate it with this flight or give this tag another name"
      	);
    	}
  	},
  	error: function (jqXHR, textStatus, errorThrown) {},
  	async: true,
	});
  }

  /**
   * Calls the server using ajax-json to notify it of the new tag change
   */
  editTag(newTag, currentTag) {
	console.log("submitting edit for tag: " + currentTag.hashId);

	console.log("current tag");
	console.log(currentTag);

	console.log("new tag");
	console.log(newTag);

	var submissionData = {
  	tag_id: currentTag.hashId,
  	name: newTag.name,
  	description: newTag.description,
  	color: newTag.color,
	};

	let thisFlight = this;

	$.ajax({
  	type: "POST",
  	url: "/protected/edit_tag",
  	data: submissionData,
  	dataType: "json",
  	success: function (response) {
    	console.log("received response: ");
    	console.log(response);
    	if (response != "NOCHANGE") {
      	console.log("tag was edited!");

      	for (var i = 0; i < thisFlight.state.flights.length; i++) {
        	let flight = thisFlight.state.flights[i];
        	console.log(flight);
        	console.log(currentTag);
        	if (flight.tags != null && flight.tags.length > 0) {
          	let tags = flight.tags;
          	for (var j = 0; j < tags.length; j++) {
            	let tag = tags[j];
            	if (tag.hashId == currentTag.hashId) {
              	tags[j] = response;
            	}
          	}
        	}
      	}
      	thisFlight.setState(thisFlight.state);
    	} else {
      	thisFlight.showNoEditError();
    	}
    	thisFlight.setState(thisFlight.state);
  	},
  	error: function (jqXHR, textStatus, errorThrown) {},
  	async: true,
	});
  }

  getUnassociatedTags(flightId) {
	console.log("getting unassociated tags!");

	let tags = [];

	var submissionData = {
  	id: flightId,
	};

	$.ajax({
  	type: "POST",
  	url: "/protected/get_unassociated_tags",
  	data: submissionData,
  	dataType: "json",
  	success: function (response) {
    	console.log("received response: ");
    	console.log(response);

    	tags = response;
  	},
  	error: function (jqXHR, textStatus, errorThrown) {},
  	async: false,
	});

	return tags;
  }

  /**
   * Handles when the user presses the delete button, and prompts them with @module confirmModal
   */
  deleteTag(flightId, tagId) {

	return new Promise((resolve, reject) => {

    	let tag = this.state.flights.find(
        	(flight) => (flight.id == flightId)
    	).tags.find(
        	(tag) => (tag.hashId == tagId)
    	);

    	console.log(tag);
    	if (tag==null)
        	return resolve(null);

    	if (tagId == null) {
        	errorModal.show(
            	"Please select a tag to delete first!",
            	"You did not select a tag to delete"
        	);
        	return resolve(null);
    	}

    	console.log("delete tag invoked!");
    	confirmModal.show(
        	"Confirm Delete Tag: '" + tag.name + "'",
        	"Are you sure you wish to delete this tag?\n\nThis operation will remove it from this flight as well as all other flights that this tag is associated with. This operation cannot be undone!",
        	() => {
            	let confirmResult = this.removeTag(flightId, tagId, true);
            	return resolve(confirmResult);
        	}
    	);

	});

  }

  /**
   * removes a tag from a flight, either permanent or just from one flight
   * @param id the tagid of the tag being removed
   * @param tag is the tag being removed
   * @param isPermanent a bool representing whether or not the removal is permanent
   */
  removeTag(flightId, tagId, isPermanent) {

	console.log("un-associating tag #" + tagId + " with flight #" + flightId);

	if (tagId == null || tagId == -1) {
    	errorModal.show("Please select a flight to remove first!", "Cannot remove any flights!");
    	return;
	}

	var submissionData = {
    	flight_id : flightId,
    	tag_id : tagId,
    	permanent : isPermanent,
    	all : (tagId == -2)
	};

	let thisFlight = this;
	console.log("calling deletion ajax");

	return new Promise((resolve, reject) => {

    	$.ajax({
        	type: "POST",
        	url: "/protected/remove_tag",
        	data: submissionData,
        	dataType: "json",
        	success: function (response) {

            	console.log("received response: ");
            	console.log(response);

            	//Permanently deleting a tag
            	if (isPermanent) {

                	console.log("permanent deletion of tag with id: " + tagId);
                	for (var i = 0; i < thisFlight.state.flights.length; i++) {
                    	let flight = thisFlight.state.flights[i];
                    	if (flight.id == flightId) {
                        	let tags = flight.tags;
                        	tags.splice(tags.indexOf(response.tag)-1, 1);
                    	}
                	}

            	//Clearing all tags from a flight
            	} else if (response.allTagsCleared) {

                	for (var i = 0; i < thisFlight.state.flights.length; i++) {
                    	let flight = thisFlight.state.flights[i];
                    	if (flight.id == flightId) {
                        	flight.tags = [];
                    	}
                	}

            	//Removing a tag from a flight
            	} else {

                	for (var i = 0; i < thisFlight.state.flights.length; i++) {
                    	let flight = thisFlight.state.flights[i];
                    	if (flight.id == flightId) {
                        	let tags = flight.tags;
                        	tags.splice(tags.indexOf(response.tag)-1, 1);
                    	}
                	}

            	}
            	thisFlight.setState(thisFlight.state);

            	resolve(response);
        	},
        	error: function (jqXHR, textStatus, errorThrown) {
            	reject(errorThrown);
        	},
        	async: false,
    	});

	});

  }

  /**
   * Associates a tag with this flight
   * @param id the tag id to associate
   */
  associateTag(tagId, flightId) {
	console.log("associating tag #" + tagId + " with flight #" + flightId);

	var submissionData = {
  	id: flightId,
  	tag_id: tagId,
	};

	let thisFlight = this;

	$.ajax({
  	type: "POST",
  	url: "/protected/associate_tag",
  	data: submissionData,
  	dataType: "json",
  	success: function (response) {
    	console.log("received response: ");
    	console.log(response);
    	for (var i = 0; i < thisFlight.state.flights.length; i++) {
      	let flight = thisFlight.state.flights[i];
      	if (flight.id == flightId) {
        	if (flight.tags != null && flight.tags.length > 0) {
          	flight.tags.push(response);
        	} else {
          	flight.tags = [response];
        	}
      	}
    	}
    	thisFlight.setState(thisFlight.state);
  	},
  	error: function (jqXHR, textStatus, errorThrown) {},
  	async: true,
	});
  }

  /**
   * Handles when the user presses the clear all tags button, and prompts them with @module confirmModal
   */
  clearTags(flightId) {
	confirmModal.show(
  	"Confirm action",
  	"Are you sure you would like to remove all the tags from flight #" +
    	flightId +
    	"?",
  	() => {
    	this.removeTag(flightId, -2, false);
  	}
	);
  }

  /**
   * Handles clearing all selected flights for multiple flight replays
   */
  clearCesiumFlights() {
	cesiumFlightsSelected.forEach((removedFlight) => {
  	console.log("Removed " + removedFlight);
  	let toggleButton = document.getElementById(
    	"cesiumToggled" + removedFlight
  	);
  	toggleButton.click();
	});

	if (cesiumFlightsSelected.length > 0) {
  	this.clearCesiumFlights();
	}
  }

	displayPlot() {

    	let styles = getComputedStyle(document.documentElement);
    	let plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
    	let plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
    	let plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();
    	global.plotlyLayout = {
        	shapes: [],
        	plot_bgcolor : "transparent",
        	paper_bgcolor : plotBgColor,
        	font : {
            	color : plotTextColor
        	},
        	xaxis : {
            	gridcolor : plotGridColor
        	},
        	yaxis : {
            	gridcolor : plotGridColor
        	},
        	margin : {
            	l : 60,
            	r : 40,
            	b : 40,
            	t : 40,
        	}
    	};

    	const plotlyConfig = {responsive: true};

    	let plotElement = $("#plot");
    	console.log("Plot Element: ", plotElement, plotElement.children.length);

    	if (plotElement.hasClass("has-plotly-plot")) {
        	Plotly.update("plot", [], global.plotlyLayout);
    	} else {
        	plotElement.addClass("has-plotly-plot");
        	console.log("Creating new plot...");
        	Plotly.newPlot("plot", [], global.plotlyLayout, plotlyConfig);
    	}

    	if (map == null)
        	initializeMap();

    	var myPlot = document.getElementById("plot");
    	console.log("myPlot:");
    	console.log(myPlot);

    	myPlot.on("plotly_hover", function (data) {

        	var xaxis = data.points[0].xaxis,
            	yaxis = data.points[0].yaxis;

        	/*
        	var infotext = data.points.map(function(d) {
            	return ('width: '+xaxis.l2p(d.x)+', height: '+yaxis.l2p(d.y));
        	});
        	*/

        	//console.log("in hover!");
        	//console.log(data);

        	let x = data.points[0].x;
        	//console.log("x: " + x);

        	map.getLayers().forEach(function (layer) {

            	if (layer instanceof VectorLayer) {

                	if ("flightState" in layer) {
                    	//console.log("VECTOR layer:");

                    	var hiddenStyle = new Style({
                    	stroke: new Stroke({
                        	color: layer.flightState.state.color,
                        	width: 1.5,
                    	}),
                    	image: new Circle({
                        	radius: 5,
                        	stroke: new Stroke({
                        	color: [0, 0, 0, 0],
                        	width: 2,
                        	}),
                    	}),
                    	});

                    	var visibleStyle = new Style({
                    	stroke: new Stroke({
                        	color: layer.flightState.state.color,
                        	width: 1.5,
                    	}),
                    	image: new Circle({
                        	radius: 5,
                        	stroke: new Stroke({
                        	color: layer.flightState.state.color,
                        	width: 2,
                        	}),
                    	}),
                    	});

                    	if (layer.getVisible()) {
                    	if (x < layer.flightState.state.points.length) {
                        	console.log(
                        	"need to draw point at: " + layer.flightState.state.points[x]
                        	);
                        	layer.flightState.state.trackingPoint.setStyle(visibleStyle);
                        	layer.flightState.state.trackingPoint
                        	.getGeometry()
                        	.setCoordinates(layer.flightState.state.points[x]);
                    	} else {
                        	console.log(
                        	"not drawing point x: " +
                            	x +
                            	" >= points.length: " +
                            	layer.flightState.state.points.length
                        	);
                        	layer.flightState.state.trackingPoint.setStyle(hiddenStyle);
                    	}
                    	}
                	}

            	}

        	});

    	});

	}

  render() {

	let style = {};

	let sortableColumnsHumanReadable = Array.from(sortableColumns.keys());

	return (
    	<div style={{overflowY:"hidden", overflowX:"hidden", display:"flex", flexDirection:"column", height:"100vh"}}>

      	<div style={{flex:"0 0 auto"}}>
          	<SignedInNavbar
              	activePage="flights"
              	selectableLayers={this.state.selectableLayers}
              	filterVisible={true}
              	plotVisible={this.state.plotVisible}
              	mapVisible={this.state.mapVisible}
              	filterSelected={this.state.filterSelected}
              	plotSelected={this.state.plotSelected}
              	mapSelected={this.state.mapSelected}
              	mapStyle={this.state.mapStyle}
              	togglePlot={() => this.togglePlot()}
              	toggleFilter={() => this.toggleFilter()}
                toggleCesiumMap={() => this.toggleCesiumMap()}
              	toggleMap={() => this.toggleMap()}
              	mapSelectChanged={(style) => this.mapSelectChanged(style)}
              	mapLayerChanged={(style) => this.mapLayerChanged(style)}
              	waitingUserCount={waitingUserCount}
              	fleetManager={fleetManager}
              	ref={this.navRef}
              	unconfirmedTailsCount={unconfirmedTailsCount}
              	modifyTailsAccess={modifyTailsAccess}
              	darkModeOnClickAlt={()=>{this.displayPlot();}}
          	/>
      	</div>

			<div className="d-flex flex-row" style={{overflowY: "auto", overflowX: "hidden", flex: "1 1 auto"}}>

				<div
					id="plot-map-div"
					className="d-flex flex-column col m-0"
					style={{
						minWidth: "45%",
						maxWidth: "45%",
						minHeight: "100%",
						maxHeight: "100%",
						padding: "0.50em 0 1.00em 0.50em"
					}}
				>
					<div id="plot-container" className="card"
						 style={{width: "100%", minHeight: "50%", maxHeight: "50%", overflow: "hidden"}}>
						<div id="plot" style={{minHeight: "100%", maxHeight: "100%"}}/>
						<div
							className="map-graph-expand-button btn btn-outline-secondary d-flex align-items-center justify-content-center"
							style={{position: "absolute", top: "0", left: "0"}}
							onClick={() => this.expandContainer("plot-container")}>
							<i className="fa fa-expand p-1"/>
						</div>
					</div>

					<div id="map-container" className="card" style={{
						width: "100%",
						minHeight: "50%",
						maxHeight: "50%",
						marginTop: "0.50em",
						overflow: "hidden"
					}}>
						<div id="map" style={{minHeight: "100%", maxHeight: "100%"}}/>
						<div className="map-graph-expand-button btn btn-outline-secondary"
							 style={{position: "absolute", top: "0", left: "0"}}
							 onClick={() => this.expandContainer("map-container")}>
							<i className="fa fa-expand p-1"/>
						</div>
					</div>

				</div>
				<div id="cesium-div" className='row m-0' style={{width: "50%", height: "0%", display: "none"}}>
					<CesiumPage
						parent={this}
						setRef={this.cesiumRef}
						flights={this.state.flights}
					/>
				</div>
				<div className="p-0 m-2 d-flex flex-column col" style={{width: "100%", overflowX: "hidden"}}>

					<div>
						<Filter
							filterVisible={this.state.filterVisible}
							submitButtonName="Apply Filter"
							submitFilter={(resetCurrentPage = true) => {
								this.submitFilter(resetCurrentPage);
							}}
							rules={rules}
							filters={this.state.filters}
							getFilter={() => {
								return this.state.filters;
							}}
							setFilter={(filter) => this.setFilter(filter)}
							setCurrentSortingColumn={(sortColumn) => this.setCurrentSortingColumn(sortColumn)}
							getCurrentSortingColumn={() => this.getCurrentSortingColumn()}
						/>
					</div>

					<div id="flights-card-container" className="mb-2 card" style={{
						overflowY: "scroll",
						flex: "1 1 auto",
						border: "1px solid var(--c_border_alt)",
						borderRadius: "0.25em",
						marginTop: "0.50em"
					}}>
						<FlightsCard
							parent={this}
							layers={this.state.layers}
							flights={this.state.flights}
							navBar={this.navRef}
							ref={(elem) => (this.flightsRef = elem)}
							showMap={() => {
								this.showMap();
							}}
							showPlot={() => {
								this.showPlot();
							}}
							setAvailableLayers={(plotLayers) => {
								this.setAvailableLayers(plotLayers);
							}}
							setFlights={(flights) => {
								this.setState({
									flights: flights,
								});
							}}
							updateNumberPages={(numberPages) => {
								this.setState({
									numberPages: numberPages,
								});
							}}
							addTag={(flightId, name, description, color) =>
								this.addTag(flightId, name, description, color)
							}
							removeTag={(flightId, tagId, perm) =>
								this.removeTag(flightId, tagId, perm)
							}
							deleteTag={(flightId, tagId) => this.deleteTag(flightId, tagId)}
							getUnassociatedTags={(flightId) =>
								this.getUnassociatedTags(flightId)
							}
							associateTag={(tagId, flightId) =>
								this.associateTag(tagId, flightId)
							}
							clearTags={(flightId) => this.clearTags(flightId)}
							editTag={(currentTag, newTag) => this.editTag(currentTag, newTag)}
							showCesiumPage={(flightId, color) => {this.addCesiumFlight(flightId, color);}}
							addCesiumFlightPhase={(phase, flightId) => {this.addCesiumFlightPhase(phase, flightId);}}
							addCesiumEventEntity={(event, flightId) => {this.addCesiumEventEntity(event, flightId);}}
							removeCesiumFlight={(flightId) => {this.removeCesiumFlight(flightId);}}

						/>
					</div>

					<div style={{width: "100%", bottom: "0", alignSelf: "center"}}>
						<Paginator
							submitFilter={(resetCurrentPage) => {
								this.submitFilter(resetCurrentPage);
							}}
							items={this.state.flights}
							itemName="flights"
							rules={sortableColumns}
							currentPage={this.state.currentPage}
							numberPages={this.state.numberPages}
							pageSize={this.state.pageSize}
							setSortingColumn={(sortColumn) => this.setSortingColumn(sortColumn)}
							getSortingColumn={() => this.getSortingColumn()}
							setSortingOrder={(order) => this.setSortingOrder(order)}
							getSortingOrder={() => this.getSortingOrder()}
							sortOptions={sortableColumnsHumanReadable}
							updateCurrentPage={(currentPage) => {
								this.state.currentPage = currentPage;
							}}
							updateItemsPerPage={(pageSize) => {
								this.state.pageSize = pageSize;
							}}
							location="Bottom"
						/>
					</div>

				</div>

			</div>

		</div>
	);
  }
}


var flightsPage = ReactDOM.render(
	<FlightsPage/>,
	document.querySelector("#flights-page")
);

initializeMap();
flightsPage.displayPlot();
flightsPage.resolveDisplay();

console.log("rendered flightsCard!");