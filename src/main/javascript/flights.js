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

import { Filter } from './filter.js';
import { Paginator } from './paginator_component.js';
import { FlightsCard } from './flights_card_component.js';
import Plotly from 'plotly.js';
import { timeZones } from "./time_zones.js";
import { linearRingLength } from "ol/geom/flat/length.js";
import { View } from "ol";
import CesiumPage from "./ngafid_cesium.js";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import { cesiumFlightsSelected } from "./cesium_buttons.js";

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

const rules = [

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

const CONTAINER_EXPANDED_NONE = undefined;

const CESIUM_RESOLUTION_PASSTHROUGH = "Default";
const CESIUM_RESOLUTION_SCALE_DEFAULT = 1.00;
const CESIUM_FLIGHT_TRACKED_NONE = undefined;

const PAGE_ORIENTATION = Object.freeze({
    COLUMN: Symbol("column"),
    ROW: Symbol("row"),
})

class FlightsPage extends React.Component {

    constructor(props) {

        super(props);

        //Define reference for the Plotly plot container
        this.plotContainerRef = React.createRef();

        this.state = {

            plotInitialized: false,
            containerExpanded: CONTAINER_EXPANDED_NONE,
            pageOrientation: PAGE_ORIENTATION.COLUMN,
            filterVisible: true,
            filterSelected: true,
            plotVisible: false,
            plotSelected: false,
            mapVisible: false,
            cesiumVisible: false,
            mapSelected: false,
            mapStyle: "Road",
            flightsRef: React.createRef(),
            layers: [],
            flights: undefined, //start out with no specified flights
            sortColumn: "Start Date and Time", //need to define a default here, flt# will alias to primary key server side
            sortingOrder: "Descending", //need to define a default here, descending is default
            storedFilters: this.getStoredFilters(),

            filters: {
                type: "GROUP",
                condition: "AND",
                filters: [],
            },

            //Needed for paginator
            currentPage: 0,
            numberPages: 1,
            pageSize: 10,

            //...
            cesiumResolutionScale: 1.00,
            cesiumResolutionUseDefault: true,
            cesiumFlightTracked: CESIUM_FLIGHT_TRACKED_NONE,
            cesiumFlightTrackerDisplayHovered: false,


        };

        this.cesiumRef = React.createRef();
        this.navRef = React.createRef();

    }


    componentDidMount() {

        console.log("Got Component Mount");

        //Initialize the map
        initializeMap();

        //Initialize Plotly plot
        this.initializePlot();

    }

    componentDidUpdate(prevProps, prevState) {

        console.log("Got Component Update");

        //Plot visibility updated...
        if (this.state.plotVisible !== prevState.plotVisible) {

            console.log("Plot visibility changed to: ", this.state.plotVisible);

            //Plot is visible, initialize it
            if (this.state.plotVisible)
                this.initializePlot();

            //Otherwise, purge the plot
            else
                this.cleanupPlot();

        }

        //Update map size
        map.updateSize();

    }

    componentWillUnmount() {

        //Clean up the Plotly plot
        this.cleanupPlot();

    }

    initializePlot() {

        const plotElement = document.getElementById("plot");

        console.log("Initializing plot...");
        console.log("Plot element: ", plotElement);

        //Plot not flagged with Plotly class, initialize it
        if (plotElement && !plotElement.classList.contains("has-plotly-plot"))
            this.displayPlot();

    }

    cleanupPlot() {

        try {

            //Plot is visible, purge it
            if (this.plotContainerRef.current && this.state.plotInitialized) {
                Plotly.purge(this.plotContainerRef.current);
                this.setState({ plotInitialized: false });
            }

        } catch (error) {
            console.error("Plot cleanup error:", error);
        }

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

    addCesiumFlightPhase(phase, flightId) {
        this.cesiumRef.current.addPhaseEntity(phase, flightId);
    }

    zoomToEventEntity(eventId, flightId) {
        this.cesiumRef.current.zoomToEventEntity(eventId, flightId);
    }

    removeCesiumFlight(flightId) {
        this.cesiumRef.current.removeFlightEntities(flightId);
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

        //Order is the same, do nothing
        if (order === this.state.sortingOrder)
            return;

        console.log("sorting in " + order + " order");
        this.state.sortingOrder = order;
        this.setState(this.state);
        this.submitFilter(true);

    }

    getSortingOrder() {
        return this.state.sortingOrder;
    }


    /*
        Cesium (Visibility) Methods
    */
    addCesiumEventEntity(event, flightId) {
        this.cesiumRef.current.addEventEntity(event, flightId);
    }

    removeCesiumEntity(flightId) {
        this.cesiumRef.current.removeEntity(flightId);
    }

    addCesiumFlight(flightId, color) {

        console.log("add cesium flight");
        console.log("in showCesium flight id from flight component " + flightId);

        this.cesiumRef.current.addFlightEntity(flightId, color);
        this.showCesiumMap();

    }

    toggleCesium() {

        //Minimize any expanded container
        this.expandContainer(CONTAINER_EXPANDED_NONE);

        if (this.state.cesiumVisible)
            this.hideCesiumMap();
        else
            this.showCesiumMap();

    }

    hideCesiumMap() {

        //Cesium is already hidden, exit
        if (!this.state.cesiumVisible)
            return;

        this.setState({ cesiumVisible: false });

    }

    showCesiumMap() {

        //Cesium is already visible, exit
        if (this.state.cesiumVisible)
            return;

        this.setState({ cesiumVisible: true });

    }



    /*
        Map Visibility Methods
    */
    showMap() {

        //Map is already visible, exit
        if (this.state.mapVisible)
            return;

        this.setState({ mapVisible: true });

    }

    hideMap() {

        //Map is already hidden, exit
        if (!this.state.mapVisible)
            return;

        this.setState({ mapVisible: false });

    }

    toggleMap() {

        console.log("Toggle Map triggered...");

        //Minimize any expanded container
        this.expandContainer(CONTAINER_EXPANDED_NONE);

        if (this.state.mapVisible)
            this.hideMap();
        else
            this.showMap();

    }


    /*
        Plot Visibility Methods
    */
    showPlot() {

        console.log("Show Plot triggered...");

        //Plot is already visible, exit
        if (this.state.plotVisible)
            return;

        this.setState({ plotVisible: true });

        //Resize the plot
        Plotly.Plots.resize("plot");

    }

    hidePlot() {

        console.log("Hide Plot triggered...");

        //Plot is already hidden, exit
        if (!this.state.plotVisible)
            return;

        this.setState({ plotVisible: false });

    }

    togglePlot() {

        //Minimize any expanded container
        this.expandContainer(CONTAINER_EXPANDED_NONE);

        if (this.state.plotVisible)
            this.hidePlot();
        else
            this.showPlot();

    }


    /*
        Misc. Visibility Methods
    */
    toggleOrientation() {

        //In column mode, switch to row mode
        if (this.state.pageOrientation === PAGE_ORIENTATION.COLUMN)
            this.state.pageOrientation = PAGE_ORIENTATION.ROW;

        //Otherwise, switch to column mode
        else
            this.state.pageOrientation = PAGE_ORIENTATION.COLUMN;

        console.log("Switching page orientation to: ", this.state.pageOrientation);

        //Update the state
        this.setState(this.state);

        //Perform display resolution
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
        if (this.state.containerExpanded !== CONTAINER_EXPANDED_NONE)
            newExpandedContainerValue = CONTAINER_EXPANDED_NONE;

        //Not expanded, so expand
        else
            newExpandedContainerValue = targetContainerName;

        this.state.containerExpanded = newExpandedContainerValue;
        this.setState(this.state);

        this.resolveDisplay();

    }

    resolveDisplayExpanded() {

        const { containerExpanded } = this.state;

        console.log(`Resolving display for expanded container: ${containerExpanded}`);

        //Hide all containers initially
        this.setState({
            plotVisible: false,
            cesiumVisible: false,
            mapVisible: false,
        }, () => {

            //Show only the expanded container, trigger resize methods
            switch (containerExpanded) {

                case "plot-container":
                    this.setState({ plotVisible: true }, () => {
                        Plotly.Plots.resize("plot");
                    });
                    break;

                case "cesium-container":
                    this.setState({ cesiumVisible: true }, () => {
                        /* ... */
                    });
                    break;

                case "map-container":
                    this.setState({ mapVisible: true }, () => {
                        map.updateSize();
                    });
                    break;
            }

        });
        
    }

    resolveDisplay() {

        const { containerExpanded } = this.state;

        //A container is expanded, perform expanded layout resolution instead
        if (containerExpanded) {
            this.resolveDisplayExpanded();
            return;
        }

        //Normal layout calculations
        const { plotVisible, cesiumVisible, mapVisible } = this.state;

        //Plot marked as visible, resize and show it
        if (plotVisible) {

            console.log("Plot is visible, resizing...");

            //...Display plot div
            $("plot").show();

            //...Resize plot
            Plotly.Plots.resize("plot");

        }

        this.setState(this.state);
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
            async: false,
            success: function (response) {
                console.log("received filters response: ");
                console.log(response);

                storedFilters = response;
            },
            error: function (jqXHR, textStatus, errorThrown) { /* ... */ },
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
            "Submitting filter! "
            + "currentPage: " + this.state.currentPage
            + ", pageSize: " + this.state.pageSize
            + ", sortByColumn: " + this.state.sortColumn
        );

        console.log("Submitting filters:");
        console.log(this.state.filters);

        $("#loading").show();

        //Reset the current page to 0 if the page size or filter have changed
        let currentPage = this.state.currentPage;
        if (resetCurrentPage === true)
            currentPage = 0;

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
            async: true,
            success: function (response) {

                console.log("'Get Flights' response:", response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("Error in 'Get Flights', displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                //Response is empty, show error modal
                if (response == "NO_RESULTS") {

                    console.log("'Get Flights' -- No flights found with the given parameters!");
                    errorModal.show(
                        "No flights found with the given parameters!",
                        "Please try a different query."
                    );

                    //Response is valid, update the flights
                } else {

                    flightsPage.setState({
                        flights: response.flights,
                        currentPage: currentPage,
                        numberPages: response.numberPages,
                    });
                }

            },
            error: function (jqXHR, textStatus, errorThrown) {

                console.log("Error loading flights: ", jqXHR, textStatus, errorThrown);

                errorModal.show("Error Loading Flights", errorThrown);
                $("#loading").hide();
            }
        });

    }

    setAvailableLayers(plotLayers) {

        console.log("changing selectable layers on navbar");
        console.log(plotLayers);

        this.setState({ selectableLayers: plotLayers });

    }

    /* Tag Methods */
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
            async: true,
            success: function (response) {

                console.log("received response: ");
                console.log(response);
                if (response != "ALREADY_EXISTS") {

                    for (var i = 0; i < thisFlight.state.flights.length; i++) {

                        let flight = thisFlight.state.flights[i];
                        if (flight.id == flightId) {

                            //If the flight already has tags, add the new tag to the list
                            if (flight.tags != null && flight.tags.length > 0)
                                flight.tags.push(response);

                            //Otherwise, create a new list with the new tag
                            else
                                flight.tags = [response];

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
            error: function (jqXHR, textStatus, errorThrown) { /* ... */ }
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
            async: true,
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
                                if (tag.hashId == currentTag.hashId)
                                    tags[j] = response;

                            }

                        }

                    }

                    thisFlight.setState(thisFlight.state);

                } else {

                    thisFlight.showNoEditError();
                }

                thisFlight.setState(thisFlight.state);

            },
            error: function (jqXHR, textStatus, errorThrown) { /* ... */ },
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
            async: false,
            success: function (response) {
                console.log("received response: ");
                console.log(response);

                tags = response;
            },
            error: function (jqXHR, textStatus, errorThrown) { /* ... */ }
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

            if (tag == null)
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
            flight_id: flightId,
            tag_id: tagId,
            permanent: isPermanent,
            all: (tagId == -2)
        };

        let thisFlight = this;
        console.log("calling deletion ajax");

        return new Promise((resolve, reject) => {

            $.ajax({
                type: "POST",
                url: "/protected/remove_tag",
                data: submissionData,
                dataType: "json",
                async: false,
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
                                tags.splice(tags.indexOf(response.tag) - 1, 1);
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
                                tags.splice(tags.indexOf(response.tag) - 1, 1);
                            }
                        }

                    }
                    thisFlight.setState(thisFlight.state);

                    resolve(response);
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    reject(errorThrown);
                },

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
            async: true,
            success: function (response) {

                console.log("received response: ");
                console.log(response);
                for (var i = 0; i < thisFlight.state.flights.length; i++) {

                    let flight = thisFlight.state.flights[i];
                    if (flight.id == flightId) {

                        //Flight already has tags, add the new tag to the list
                        if (flight.tags != null && flight.tags.length > 0)
                            flight.tags.push(response);

                        //Otherwise, create a new list with the new tag
                        else
                            flight.tags = [response];

                    }

                }
                thisFlight.setState(thisFlight.state);

            },
            error: function (jqXHR, textStatus, errorThrown) { /* ... */ }
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
            let toggleButton = document.getElementById("cesiumToggled" + removedFlight);
            toggleButton.click();

        });

        if (cesiumFlightsSelected.length > 0)
            this.clearCesiumFlights();

    }

    setCesiumResolution(newResolution) {

        console.log("Setting Cesium Resolution to: " + newResolution);

        //Default Resolution
        if (newResolution === CESIUM_RESOLUTION_PASSTHROUGH) {
            this.state.cesiumResolutionScale = CESIUM_RESOLUTION_SCALE_DEFAULT;
            this.state.cesiumResolutionUseDefault = true;
        }

        //Custom Resolution
        else {
            this.state.cesiumResolutionScale = newResolution;
            this.state.cesiumResolutionUseDefault = false;
        }

        //Trigger State Update
        this.setState(this.state);

    }

    displayPlot() {

        //Configure Plotly Styles & Config
        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();
        global.plotlyLayout = {
            shapes: [],
            plot_bgcolor: "transparent",
            paper_bgcolor: plotBgColor,
            font: {
                color: plotTextColor
            },
            xaxis: {
                gridcolor: plotGridColor
            },
            yaxis: {
                gridcolor: plotGridColor
            },
            margin: {
                l: 60,
                r: 40,
                b: 40,
                t: 40,
            }
        };
        const plotlyConfig = { responsive: true };


        //Get plot div element
        const plotElement = $("#plot");

        //Plot div already flagged with Plotly class, update it
        if (plotElement.hasClass("has-plotly-plot"))
            Plotly.update("plot", [], global.plotlyLayout);

        //Otherwise, create a new plot
        else {

            plotElement.addClass("has-plotly-plot");
            console.log("Creating new plot...");
            Plotly.newPlot("plot", [], global.plotlyLayout, plotlyConfig);

        }

    }

    cesiumFlightTrackedSet(newFlightId) {

        this.cesiumRef.current.cesiumFlightTrackedSet(newFlightId);

        console.log("Setting cesium flight to: " + newFlightId);

        this.state.cesiumFlightsSelected = newFlightId;

        this.setState(this.state);

    }

    cesiumFlightTrackedClear() {

        console.log("Clearing cesium flight selection");

        this.cesiumFlightTrackedSet(CESIUM_FLIGHT_TRACKED_NONE);

    }

    render() {

        let sortableColumnsHumanReadable = Array.from(sortableColumns.keys());
        const CESIUM_RESOLUTION_SCALE_OPTIONS = [CESIUM_RESOLUTION_PASSTHROUGH, 0.50, 1.00, 2.00, 4.00];


        //Oritentation Resolution
        const doGraphicsContainer = (this.state.plotVisible || this.state.cesiumVisible || this.state.mapVisible);

        const isColumnMode = (this.state.pageOrientation === PAGE_ORIENTATION.COLUMN);
        const MAIN_CONTENT_CONTAINER_FLEX_TYPE = (isColumnMode ? "row" : "column"); //<-- This is reversed on purpose

        const SEARCH_CONTAINER_SIZE_PERCENTAGE = 55;
        const GRAPHICS_CONTAINER_SIZE_PERCENTAGE = (100 - SEARCH_CONTAINER_SIZE_PERCENTAGE);

        //Layout debugging stuff
        const CONTAINER_DEBUGGING = false;
        const CONTAINER_BACKGROUND_COLOR_ALPHA = (CONTAINER_DEBUGGING ? 0.50 : 0.00);


        //Search Filter
        const searchFilters = (
            <div
                id="search-filters"
                style={{ flex: "0 0 auto" }}
            >
                <Filter
                    submitButtonName="Apply Current Filter"
                    submitFilter={(resetCurrentPage = true) => { this.submitFilter(resetCurrentPage); }}
                    rules={rules}
                    filters={this.state.filters}
                    getFilter={() => { return this.state.filters; }}
                    setFilter={(filter) => this.setFilter(filter)}
                    setCurrentSortingColumn={(sortColumn) => this.setCurrentSortingColumn(sortColumn)}
                    getCurrentSortingColumn={() => this.getCurrentSortingColumn()}
                    errorModal={errorModal}
                />
            </div>
        );

        //Search Results
        const searchResults = (
            <div
                id="flights-card-container"
                className="card d-flex"
                style={{ overflowY: "scroll", flex: "1 1 auto", border: "1px solid var(--c_border_alt)", borderRadius: "0.25em" }}
            >
                <FlightsCard
                    parent={this}
                    layers={this.state.layers}
                    flights={this.state.flights}
                    navBar={this.navRef}
                    ref={(elem) => (this.flightsRef = elem)}
                    showMap={() => { this.showMap(); }}
                    hideMap={() => this.hideMap()}
                    showPlot={() => { this.showPlot(); }}
                    setAvailableLayers={(plotLayers) => { this.setAvailableLayers(plotLayers); }}
                    setFlights={(flights) => { this.setState({ flights: flights, }); }}
                    updateNumberPages={(numberPages) => { this.setState({ numberPages: numberPages, }); }}
                    addTag={(flightId, name, description, color) => this.addTag(flightId, name, description, color)}
                    removeTag={(flightId, tagId, perm) => this.removeTag(flightId, tagId, perm)}
                    deleteTag={(flightId, tagId) => this.deleteTag(flightId, tagId)}
                    getUnassociatedTags={(flightId) => this.getUnassociatedTags(flightId)}
                    associateTag={(tagId, flightId) => this.associateTag(tagId, flightId)}
                    clearTags={(flightId) => this.clearTags(flightId)}
                    editTag={(currentTag, newTag) => this.editTag(currentTag, newTag)}
                    showCesium={(flightId, color) => { this.addCesiumFlight(flightId, color); }}
                    addCesiumFlightPhase={(phase, flightId) => { this.addCesiumFlightPhase(phase, flightId); }}
                    addCesiumEventEntity={(event, flightId) => { this.addCesiumEventEntity(event, flightId); }}
                    removeCesiumFlight={(flightId) => { this.removeCesiumFlight(flightId); }}
                    zoomToEventEntity={(eventId, flightId) => { this.zoomToEventEntity(eventId, flightId) }}
                    cesiumFlightTrackedSet={(flightId) => { this.cesiumFlightTrackedSet(flightId) }}
                    cesiumJumpToFlightStart={(flightId) => { this.cesiumRef.current.cesiumJumpToFlightStart(flightId) }}
                />
            </div>
        );

        //Search Paginator
        const searchPaginator = (
            <div
                id="search-paginator"
                style={{ flex: "0 0 auto" }}
            >
                <Paginator
                    submitFilter={(resetCurrentPage) => { this.submitFilter(resetCurrentPage); }}
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
                    updateCurrentPage={(currentPage) => { this.state.currentPage = currentPage; }}
                    updateItemsPerPage={(pageSize) => { this.state.pageSize = pageSize; }}
                    location="Bottom"
                />
            </div>
        );

        //Search Container
        const SEARCH_CONTAINER_WIDTH = doGraphicsContainer ? (isColumnMode ? `${SEARCH_CONTAINER_SIZE_PERCENTAGE}%` : "100%") : "100%";
        const SEARCH_CONTAINER_HEIGHT = doGraphicsContainer ? (isColumnMode ? "100%" : `${SEARCH_CONTAINER_SIZE_PERCENTAGE}%`) : "100%";
        const doSearchDisplay = (this.state.containerExpanded === CONTAINER_EXPANDED_NONE);
        console.log(`doSearchDisplay: ${doSearchDisplay}, ${this.state.containerExpanded}`);
        const searchContainer = (
            <div
                className={`${doSearchDisplay ? "d-flex" : "d-none"} flex-column`}
                style={{
                    padding: "0.5em", gap: "0.5em", flex: "1 0 0",
                    minWidth: SEARCH_CONTAINER_WIDTH, maxWidth: SEARCH_CONTAINER_WIDTH, width: SEARCH_CONTAINER_WIDTH,
                    minHeight: SEARCH_CONTAINER_HEIGHT, maxHeight: SEARCH_CONTAINER_HEIGHT, height: SEARCH_CONTAINER_HEIGHT,
                    backgroundColor: `rgba(0, 0, 255, ${CONTAINER_BACKGROUND_COLOR_ALPHA})`
                }}>
                {this.state.filterVisible && searchFilters}
                {searchResults}
                {searchPaginator}
            </div>
        )



        let plotGraphicItem, cesiumGraphicItem, mapGraphicItem;

        //Plot Graphic Item
        const plotDiv = (<div id="plot" className="h-100"></div>);
        plotGraphicItem = (
            <div
                id="plot-container"
                ref={this.plotContainerRef}
                className={`card ${this.state.plotVisible ? "d-flex" : "d-none"}`}
                style={{
                    overflow: "hidden",
                    borderColor: "var(--c_border_alt)",
                    position: "relative",
                    flex: "1 1 0",
                    maxHeight: "100%",
                }}
            >
                {plotDiv}
                <div
                    className="map-graph-expand-button btn btn-outline-secondary d-flex align-items-center justify-content-center"
                    style={{ position: "absolute", top: "0", left: "0" }}
                    onClick={() => this.expandContainer("plot-container")}
                >
                    <i className="fa fa-expand p-1" />
                </div>
            </div>
        );

        //Cesium Flight Tracker Button
        let cesiumFlightTrackerButton = (
            <div
                className="btn btn-outline-secondary"
                id="flightTracerDisplay"
                onClick={() => this.cesiumFlightTrackedClear()}
                onMouseEnter={() => this.setState({ flightTrackerHovered: true })}
                onMouseLeave={() => this.setState({ flightTrackerHovered: false })}
            >
                <i
                    className={this.state.flightTrackerHovered ? "fa fa-close" : "fa fa-camera"}
                    style={{ marginRight: "0.25em", minWidth: "1.25em", minHeight: "1.25em" }}
                />
                Tracked Flight: {this.state.cesiumFlightsSelected ?? "(None)"}
            </div>
        );

        //Cesium Graphic Item
        const ITEM_WIDTH = "100%";
        const ITEM_HEIGHT = "100%";
        cesiumGraphicItem = (
            <div
                id="cesium-container"
                className={`card ${this.state.cesiumVisible ? "d-flex" : "d-none"}`}
                style={{
                    overflow: "hidden",
                    borderColor: "var(--c_border_alt)",
                    position: "relative",
                    flex: "1 1 0",
                    width: ITEM_WIDTH,
                    height: ITEM_HEIGHT,
                }}
            >
                {/* Main Cesium Content */}
                <CesiumPage
                    parent={this}
                    setRef={this.cesiumRef}
                    flights={this.state.flights}
                    errorModal={errorModal}
                    cesiumResolutionScale={this.state.cesiumResolutionScale}
                    cesiumResolutionUseDefault={this.state.cesiumResolutionUseDefault}
                />

                {/* Additional Cesium Controls */}
                <div style={{ position: "absolute", top: "0", left: "0" }} className="d-flex align-items-center justify-content-center gap-1">

                    {/* Expand Cesium Button */}
                    <div
                        className="map-graph-expand-button btn btn-outline-secondary"
                        onClick={() => this.expandContainer("cesium-container")}
                    >
                        <i className="fa fa-expand p-1" />
                    </div>

                    {/* Dropdown Menu to Change Resolution Scale */}
                    <div className="dropdown mr-1">
                        <button
                            className="btn btn-outline-secondary dropdown-toggle"
                            type="button"
                            id="dropdownMenuButton"
                            data-bs-toggle="dropdown"
                            aria-expanded="false"
                        >
                            Resolution Scale: {this.state.cesiumResolutionScale * 100}%
                        </button>
                        <ul className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                            {
                                CESIUM_RESOLUTION_SCALE_OPTIONS.map((resolutionScale) => (
                                    <li key={resolutionScale}>
                                        <a
                                            className="dropdown-item"
                                            href="#"
                                            onClick={() => this.setCesiumResolution(resolutionScale)}
                                        >
                                            {resolutionScale === CESIUM_RESOLUTION_PASSTHROUGH ? CESIUM_RESOLUTION_PASSTHROUGH : `${resolutionScale * 100}%`}
                                        </a>
                                    </li>
                                ))
                            }
                        </ul>
                    </div>

                    {/* Tracked Flight Indicator Button */}
                    {cesiumFlightTrackerButton}

                </div>
            </div>
        );

        //Map Graphic Item
        const mapDiv = (<div id="map" className="map h-100" style={{minHeight: "500px"}}></div>);
        mapGraphicItem = (
            <div
                id="map-container"
                className={`card ${this.state.mapVisible ? "d-flex" : "d-none"}`}
                style={{
                    overflow: "hidden",
                    borderColor: "var(--c_border_alt)",
                    position: "relative",
                    flex: "1 1 0",
                    height: "100%",
                    width: "100%",
                }}
            >
                {mapDiv}
                <div className="map-graph-expand-button btn btn-outline-secondary"
                    style={{ position: "absolute", top: "0", left: "0" }}
                    onClick={() => this.expandContainer("map-container")}>
                    <i className="fa fa-expand p-1" />
                </div>
            </div>
        );

        //Fake "constants" for the graphics container expanded condition
        let GRAPHICS_CONTAINER_WIDTH, GRAPHICS_CONTAINER_HEIGHT, GRAPHICS_CONTAINER_FLEX_TYPE, GRAPHICS_CONTAINER_PADDING;

        //Graphics Container [Expanded]
        if (this.state.containerExpanded) {

            GRAPHICS_CONTAINER_WIDTH = "100%"
            GRAPHICS_CONTAINER_HEIGHT = "100%"
            GRAPHICS_CONTAINER_FLEX_TYPE = "column"
            GRAPHICS_CONTAINER_PADDING = "0.5em 0.5em 0.5em 0.5em"    //Padding on all sides

            //Graphics Container [Normal]
        } else {

            GRAPHICS_CONTAINER_WIDTH = (isColumnMode ? `${GRAPHICS_CONTAINER_SIZE_PERCENTAGE}%` : "100%");
            GRAPHICS_CONTAINER_HEIGHT = (isColumnMode ? "100%" : `${GRAPHICS_CONTAINER_SIZE_PERCENTAGE}%`);
            GRAPHICS_CONTAINER_FLEX_TYPE = (isColumnMode ? "column" : "row");
            GRAPHICS_CONTAINER_PADDING = (isColumnMode
                ? "0.5em 0.0em 0.5em 0.5em"     //Column View -> No padding on the right
                : "0.5em 0.5em 0.0em 0.5em"     //Row View -> No padding on the bottom
            );

        }

        const doGraphicsDisplay = (this.state.plotVisible || this.state.cesiumVisible || this.state.mapVisible);
        const graphicsContainer = (
            <div
                className={`${doGraphicsDisplay ? "d-flex" : "d-none"} flex-${GRAPHICS_CONTAINER_FLEX_TYPE} flex-fill`}
                style={{
                    flex: "1 0 0", gap: "0.5em", padding: GRAPHICS_CONTAINER_PADDING,
                    minWidth: GRAPHICS_CONTAINER_WIDTH, maxWidth: GRAPHICS_CONTAINER_WIDTH, width: GRAPHICS_CONTAINER_WIDTH,
                    minHeight: GRAPHICS_CONTAINER_HEIGHT, maxHeight: GRAPHICS_CONTAINER_HEIGHT, height: GRAPHICS_CONTAINER_HEIGHT,
                    backgroundColor: `rgba(0, 255, 0, ${CONTAINER_BACKGROUND_COLOR_ALPHA})`
                }}
            >
                {plotGraphicItem}
                {cesiumGraphicItem}
                {mapGraphicItem}
            </div>
        );


        return (
            <div style={{ overflowY: "hidden", overflowX: "hidden", display: "flex", flexDirection: "column", height: "100vh", maxHeight: "100vh", maxWidth: "100vw", width: "100vw" }}>

                {/* Navbar */}
                <div className="d-flex flex-shrink-1">
                    <SignedInNavbar
                        activePage="flights"
                        selectableLayers={this.state.selectableLayers}
                        showFlightPageOrientationButton={true}
                        filterVisible={true}
                        showMapButton={true}
                        showPlotButton={true}
                        plotVisible={this.state.plotVisible}
                        showCesiumButton={true}
                        cesiumVisible={this.state.cesiumVisible}
                        mapVisible={this.state.mapVisible}
                        filterSelected={this.state.filterSelected}
                        plotSelected={this.state.plotSelected}
                        mapSelected={this.state.mapSelected}
                        mapStyle={this.state.mapStyle}
                        toggleOrientation={() => this.toggleOrientation()}
                        togglePlot={() => this.togglePlot()}
                        toggleFilter={() => this.toggleFilter()}
                        toggleCesium={() => this.toggleCesium()}
                        toggleMap={() => this.toggleMap()}
                        mapSelectChanged={(style) => this.mapSelectChanged(style)}
                        mapLayerChanged={(style) => this.mapLayerChanged(style)}
                        waitingUserCount={waitingUserCount}
                        fleetManager={fleetManager}
                        ref={this.navRef}
                        unconfirmedTailsCount={unconfirmedTailsCount}
                        modifyTailsAccess={modifyTailsAccess}
                        darkModeOnClickAlt={() => { this.displayPlot(); }}
                    />
                </div>

                {/* Main Content Subcontainer */}
                <div className={`d-flex flex-${MAIN_CONTENT_CONTAINER_FLEX_TYPE}`} style={{ overflowY: "auto", overflowX: "hidden", flex: "1 1 auto", }}>

                    {graphicsContainer}
                    {searchContainer}

                </div>

            </div>
        );

    }
}



const flightsPage = ReactDOM.render(
    <FlightsPage />,
    document.querySelector("#flights-page")
);

console.log("rendered flightsCard!");