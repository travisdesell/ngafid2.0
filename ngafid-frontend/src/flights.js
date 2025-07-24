import "bootstrap";
import React from "react";
import { createRoot } from 'react-dom/client';

import SignedInNavbar from "./signed_in_navbar.js";
import {initializeMap, layers, map, styles} from "./map.js";

import {showErrorModal} from "./error_modal.js";
import {showConfirmModal} from "./confirm_modal.js";

import {Filter, isValidFilter} from './filter.js';
import {Paginator} from './paginator_component.tsx';
import {FlightsCard} from './flights_card_component.js';
import Plotly from 'plotly.js';
import {timeZones} from "./time_zones.js";
import CesiumPage from "./ngafid_cesium.js";
import {cesiumFlightsSelected} from "./cesium_buttons.js";

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

let plotlyLayoutGlobal = {};

const rules = [

    /*
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
    */

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
});

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
            mapVisible: false,
            cesiumVisible: false,
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

        //Check for filter in URL and load if present
        const urlParams = new URLSearchParams(window.location.search);
        const filterParam = urlParams.get('filter');

        //Found a filter in the URL, try to load it
        if (filterParam) {

            try {

                //Double decode for URL components
                const decodedFilter = decodeURIComponent(filterParam);

                console.log("Decoded filter:", decodedFilter);

                // Strict validation with fallback
                let parsedFilter;
                try {

                    parsedFilter = JSON.parse(decodedFilter);

                } catch (e) {

                    throw new Error(`Malformed filter structure: ${  e.message}`);

                }

                //Did not get 'filters' key in the parsed filter
                if (!parsedFilter?.filters)
                    throw new Error("Missing 'filters' key in parsed filter");

                //Did not interpret 'filters' as an array
                if (!Array.isArray(parsedFilter.filters))
                    throw new Error("Loaded 'filters' value is not an array");

                //Validate filter
                const autoSubmitFilter = isValidFilter(parsedFilter, rules);

                this.setState({filters: parsedFilter}, () => {

                    //Flagged as valid, automatically submit the filter
                    if (autoSubmitFilter)
                        this.submitFilter(true);

                });

            } catch (error) {

                console.error("Filter load error:", error);

            }

        }

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
                this.setState({plotInitialized: false});
            }

        } catch (error) {
            console.error("Plot cleanup error:", error);
        }

    }

    mapSelectChanged(newMapStyle) {

        for (let i = 0, ii = layers.length; i < ii; ++i) {
            console.log(`Setting layer ${  i  } to:${  styles[i] === newMapStyle}`);
            layers[i].setVisible(styles[i] === newMapStyle);
        }

        console.log(`Map style changed to: '${  newMapStyle  }'!`);
        this.setMapStyle(newMapStyle);

    }

    mapLayerChanged(newMapStyle) {

        console.log(`changing path to: ${  newMapStyle}`);
        console.log(this.state.selectableLayers);

        for (let i = 0; i < this.state.selectableLayers.length; i++) {

            const layer = this.state.selectableLayers[i];
            const name = layer.values_.name;

            if (name == newMapStyle) {
                layer.setVisible(true);
                console.log(`Setting layer ${  name  } to visible`);
            } else {
                layer.setVisible(false);
                console.log(`Setting layer ${  name  } to not visible`);
            }

        }

        console.log(`Map layer changed to: '${  newMapStyle  }'!`);
        this.setMapStyle(newMapStyle);

    }

    setMapStyle(newMapStyle) {

        this.setState({ mapStyle: newMapStyle });

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

        console.log("Sorting by: ", column);
        this.setState({ sortColumn: column }, () => {
            this.submitFilter(true);
        });

    }

    getSortingColumn() {
        return this.state.sortColumn;
    }

    setSortingOrder(order) {

        //Order is the same, do nothing
        if (order === this.state.sortingOrder)
            return;

        console.log("Sorting in '", order, "' order");
        this.setState({ sortingOrder: order }, () => {
            this.submitFilter(true);
        });

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
        console.log(`in showCesium flight id from flight component ${  flightId}`);

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

        this.setState({cesiumVisible: false});

    }

    showCesiumMap() {

        //Cesium is already visible, exit
        if (this.state.cesiumVisible)
            return;

        this.setState({cesiumVisible: true});

    }


    /*
        Map Visibility Methods
    */
    showMap() {

        //Map is already visible, exit
        if (this.state.mapVisible)
            return;

        this.setState({mapVisible: true});

    }

    hideMap() {

        //Map is already hidden, exit
        if (!this.state.mapVisible)
            return;

        this.setState({mapVisible: false});

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

        this.setState({plotVisible: true});

        //Resize the plot
        Plotly.Plots.resize("plot");

    }

    hidePlot() {

        console.log("Hide Plot triggered...");

        //Plot is already hidden, exit
        if (!this.state.plotVisible)
            return;

        this.setState({plotVisible: false});

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
        if (this.state.pageOrientation === PAGE_ORIENTATION.COLUMN) {

            this.setState({ pageOrientation: PAGE_ORIENTATION.ROW }, () => {
                console.log("Switching page orientation to: ", this.state.pageOrientation);
                this.resolveDisplay();
            });

        }
        //Otherwise, switch to column mode
        else {

            this.setState({ pageOrientation: PAGE_ORIENTATION.COLUMN }, () => {
                console.log("Switching page orientation to: ", this.state.pageOrientation);
                this.resolveDisplay();
            });

        }

    }


    toggleFilter() {

        const newFilterState = (!this.state.filterVisible);

        console.log("Toggling filterVisible to: ", newFilterState);

        this.setState({ filterVisible: newFilterState }, () => {
            this.resolveDisplay();
        });

    }

    expandContainer(targetContainerName) {

        let newExpandedContainerValue;

        //Already expanded, so collapse
        if (this.state.containerExpanded !== CONTAINER_EXPANDED_NONE)
            newExpandedContainerValue = CONTAINER_EXPANDED_NONE;

        //Not expanded, so expand
        else
            newExpandedContainerValue = targetContainerName;

        this.setState({ containerExpanded: newExpandedContainerValue }, () => {
            this.resolveDisplay();
        });

    }

    resolveDisplayExpanded() {

        const {containerExpanded} = this.state;

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
                    this.setState({plotVisible: true}, () => {
                        Plotly.Plots.resize("plot");
                    });
                    break;

                case "cesium-container":
                    this.setState({cesiumVisible: true}, () => {
                        /* ... */
                    });
                    break;

                case "map-container":
                    this.setState({mapVisible: true}, () => {
                        map.updateSize();
                    });
                    break;
            }

        });

    }

    resolveDisplay() {

        const {containerExpanded} = this.state;

        //A container is expanded, perform expanded layout resolution instead
        if (containerExpanded) {
            this.resolveDisplayExpanded();
            return;
        }

        //Normal layout calculations
        const { plotVisible } = this.state;

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
            type: 'GET',
            url: "/api/filter",
            async: false,
            success: (response) => {

                console.log("Received filters response: ", response);
                storedFilters = response;

            },
            error: (jqXHR, textStatus, errorThrown) => {

                console.log("Error loading stored filters: ", jqXHR, textStatus, errorThrown);
                showErrorModal("Error Loading Filters", errorThrown);
                
            },
        });

        return storedFilters;

    }

    submitFilter(resetCurrentPage = false) {

        console.log(`Submitting filter!`,
            `currentPage: ${this.state.currentPage},
            pageSize: ${this.state.pageSize},
            sortByColumn: ${this.state.sortColumn},
            resetCurrentPage: ${resetCurrentPage},
            `
        );

        console.log("Submitting filters:", this.state.filters);

        $("#loading").show();

        //Page size or filter have changed, reset the current page to 0
        let currentPage = this.state.currentPage;
        if (resetCurrentPage === true)
            currentPage = 0;

        //Transform the 'Has Any Event(s)' filter
        const originalFilters = this.state.filters.filters;

        const submissionData = {
            filterQuery: JSON.stringify(this.state.filters),
            currentPage: currentPage,
            pageSize: this.state.pageSize,
            sortingColumn: sortableColumns.get(this.state.sortColumn),
            sortingOrder: this.state.sortingOrder,
        };

        console.log(submissionData);

        //Undo the transformation
        this.setState(prevState => ({
            filters: {
                ...prevState.filters,
                filters: originalFilters
            }
        }));

        $.ajax({
            type: 'GET',
            url: "/api/flight",
            data: submissionData,
            timeout: 0, //<-- Unlimited timeout for slow queries
            async: true,
            success: (response, jqXHR) => {

                console.log("'Get Flights' response:", response);
                console.log("'Get Flights' jqXHR:", jqXHR);

                //Response is empty, show error modal
                const JQXHR_NO_CONTENT = 'nocontent';
                if (jqXHR === JQXHR_NO_CONTENT) {

                    console.log("'Get Flights' -- No flights found with the given parameters!");
                    showErrorModal(
                        "No flights found with the given parameters!",
                        "Please try a different query."
                    );
                    return;

                }

                //Response is invalid, show error modal
                if (response.errorTitle) {
                    console.log("Error in 'Get Flights', displaying error modal!");
                    showErrorModal(response.errorTitle, response.errorMessage);
                    return;
                }

                //Response is valid, update the flights
                this.setState({
                    flights: response.flights,
                    currentPage: currentPage,
                    numberPages: response.numberPages,
                });

            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error loading flights: ", jqXHR, textStatus, errorThrown);
                showErrorModal("Error Loading Flights", errorThrown);
            },
            complete: () => {
                console.log("Flight loading complete!");
                $("#loading").hide();
            }
        });

    }

    setAvailableLayers(plotLayers) {

        console.log("Changing selectable layers on Navbar:", plotLayers);
        this.setState({selectableLayers: plotLayers});

    }

    /* Tag Methods */
    addTag(flightId, name, description, color) {

        if (invalidString(name) || invalidString(description)) {

            showErrorModal(
                "Error creating tag!",
                "Please ensure the name and description fields are correctly filled out!"
            );

            return;

        }

        const submissionData = {
            name: name,
            description: description,
            color: color,
            id: flightId,
        };

        console.log(`Creating a new tag for flight #${this.state.flightId}`);

        $.ajax({
            type: "POST",
            url: "/api/tag",
            data: submissionData,
            dataType: "json",
            async: true,
            success: (response) => {

                console.log("Received response: ", response);

                if (response != "ALREADY_EXISTS") {

                    for (let i = 0; i < this.state.flights.length; i++) {

                        const flight = this.state.flights[i];
                        if (flight.id == flightId) {

                            //If the flight already has tags, add the new tag to the list
                            if (flight.tags != null && flight.tags.length > 0)
                                flight.tags.push(response);

                            //Otherwise, create a new list with the new tag
                            else
                                flight.tags = [response];

                        }

                    }

                    this.setState(this.state);

                } else {

                    showErrorModal(
                        "Error creating tag",
                        "A tag with that name already exists! Use the dropdown menu to associate it with this flight or give this tag another name"
                    );

                }

            },
            error: (jqXHR, textStatus, errorThrown) => { 

                console.log("Error creating tag: ", jqXHR, textStatus, errorThrown);
                showErrorModal("Error Creating Tag", errorThrown);

            }
        });

    }

    /**
     * Calls the server using ajax-json to notify it of the new tag change
     */

    editTag(newTag, currentTag) {

        console.log("Submitting edit for tag: ", currentTag.hashId);

        console.log("Current tag: ", currentTag);

        console.log("New tag: ", newTag);

        const submissionData = {
            name: newTag.name,
            description: newTag.description,
            color: newTag.color,
        };

        $.ajax({
            type: "PATCH",
            url: `/api/tag/${currentTag.hashId}`,
            data: submissionData,
            dataType: "json",
            async: true,
            success: (response) => {

                console.log("Received response: ", response);

                if (response != "NOCHANGE") {

                    console.log("Tag was edited!");

                    for (let i = 0; i < this.state.flights.length; i++) {

                        const flight = this.state.flights[i];
                        console.log(flight);
                        console.log(currentTag);

                        if (flight.tags != null && flight.tags.length > 0) {

                            const tags = flight.tags;
                            for (let j = 0; j < tags.length; j++) {

                                const tag = tags[j];
                                if (tag.hashId == currentTag.hashId)
                                    tags[j] = response;

                            }

                        }

                    }

                    this.setState(this.state);

                } else {

                    this.showNoEditError();
                }

                this.setState(this.state);

            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error editing tag: ", jqXHR, textStatus, errorThrown);
                showErrorModal("Error Editing Tag", errorThrown);
            },
        });

    }

    getUnassociatedTags(flightId) {

        console.log("getting unassociated tags!");

        let tags = [];

        $.ajax({
            type: 'GET',
            url: `/api/flight/${flightId}/tag/unassociated`,
            async: false,
            success: (response) => {

                console.log("Received response: ", response);

                tags = response;
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log("Error loading unassociated tags: ", jqXHR, textStatus, errorThrown);
                showErrorModal("Error Loading Unassociated Tags", errorThrown);
            }
        });

        return tags;

    }

    /**
     * Handles when the user presses the delete button, and prompts them with @module confirmModal
     */
    deleteTag(flightId, tagId) {

        return new Promise((resolve) => {

            const tag = this.state.flights.find(
                (flight) => (flight.id == flightId)
            ).tags.find(
                (tag) => (tag.hashId == tagId)
            );

            console.log(tag);

            if (tag == null)
                return resolve(null);

            if (tagId == null) {

                showErrorModal(
                    "Please select a tag to delete first!",
                    "You did not select a tag to delete"
                );

                return resolve(null);

            }

            console.log("Delete tag invoked!");
            showConfirmModal(
                `Confirm Delete Tag: '${  tag.name  }'`,
                "Are you sure you wish to delete this tag?\n\nThis operation will remove it from this flight as well as all other flights that this tag is associated with. This operation cannot be undone!",
                () => {
                    const confirmResult = this.removeTag(flightId, tagId, true);
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

        console.log(`un-associating tag #${  tagId  } with flight #${  flightId}`);

        if (tagId == null || tagId == -1) {
            showErrorModal("Please select a flight to remove first!", "Cannot remove any flights!");
            return;
        }

        const all = (tagId == -2);
        const submissionData = {
            flight_id: flightId,
            tag_id: tagId,
            permanent: isPermanent,
            all: all,
        };

        // TODO: it is a little crazy that this is a single function. The routes have been broken up, the JS should mirror that.
        let route;
        if (isPermanent) {
            route = `/api/tag/${tagId}`;
        } else if (all) {
            route = `/api/flight/${flightId}/tag`;
        } else {
            route = `/api/flight/${flightId}/tag/${tagId}`;
        }

        return new Promise((resolve, reject) => {

            $.ajax({
                type: "DELETE",
                url: route,
                data: submissionData,
                dataType: "json",
                async: false,
                success: (response) => {

                    console.log("Received response: ", response);

                    //Permanently deleting a tag
                    if (isPermanent) {

                        console.log(`permanent deletion of tag with id: ${  tagId}`);
                        for (let i = 0; i < this.state.flights.length; i++) {
                            const flight = this.state.flights[i];
                            if (flight.id == flightId) {
                                const tags = flight.tags;
                                tags.splice(tags.indexOf(response.tag) - 1, 1);
                            }
                        }

                        //Clearing all tags from a flight
                    } else if (response.allTagsCleared) {

                        for (let i = 0; i < this.state.flights.length; i++) {
                            const flight = this.state.flights[i];
                            if (flight.id == flightId) {
                                flight.tags = [];
                            }
                        }

                        //Removing a tag from a flight
                    } else {

                        for (let i = 0; i < this.state.flights.length; i++) {
                            const flight = this.state.flights[i];
                            if (flight.id == flightId) {
                                const tags = flight.tags;
                                tags.splice(tags.indexOf(response.tag) - 1, 1);
                            }
                        }

                    }
                    this.setState(this.state);

                    resolve(response);
                },
                error: (jqXHR, textStatus, errorThrown) => {
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

        console.log(`Associating tag #${  tagId  } with flight #${  flightId}`);

        $.ajax({
            type: "PUT",
            url: `/api/flight/${flightId}/tag/${tagId}`,
            dataType: "json",
            async: true,
            success: (response) => {

                console.log("Received response: ", response);
                for (let i = 0; i < this.state.flights.length; i++) {

                    const flight = this.state.flights[i];
                    if (flight.id == flightId) {

                        //Flight already has tags, add the new tag to the list
                        if (flight.tags != null && flight.tags.length > 0)
                            flight.tags.push(response);

                        //Otherwise, create a new list with the new tag
                        else
                            flight.tags = [response];

                    }

                }
                this.setState(this.state);

            },
            error: (jqXHR, textStatus, errorThrown) => {

                console.log("Error associating tag: ", jqXHR, textStatus, errorThrown);
                showErrorModal("Error Associating Tag", errorThrown);

            }
        });

    }

    /**
     * Handles when the user presses the clear all tags button, and prompts them with @module confirmModal
     */
    clearTags(flightId) {

        showConfirmModal(
            "Confirm action",
            `Are you sure you would like to remove all the tags from flight #${ flightId  }?`,
            () => { this.removeTag(flightId, -2, false); }
        );

    }

    /**
     * Handles clearing all selected flights for multiple flight replays
     */
    clearCesiumFlights() {

        cesiumFlightsSelected.forEach((removedFlight) => {

            console.log(`Removed ${  removedFlight}`);
            const toggleButton = document.getElementById(`cesiumToggled${  removedFlight}`);
            toggleButton.click();

        });

        if (cesiumFlightsSelected.length > 0)
            this.clearCesiumFlights();

    }

    setCesiumResolution(newResolution) {

        console.log(`Setting Cesium Resolution to: ${  newResolution}`);

        //Default Resolution
        if (newResolution === CESIUM_RESOLUTION_PASSTHROUGH) {
            this.setState({
                cesiumResolutionScale: CESIUM_RESOLUTION_SCALE_DEFAULT,
                cesiumResolutionUseDefault: true
            });
        }

        //Custom Resolution
        else {
            this.setState({
                cesiumResolutionScale: newResolution,
                cesiumResolutionUseDefault: false
            });
        }

    }

    displayPlot() {

        //Configure Plotly Styles & Config
        const styles = getComputedStyle(document.documentElement);
        const plotBgColor = styles.getPropertyValue("--c_plotly_bg").trim();
        const plotTextColor = styles.getPropertyValue("--c_plotly_text").trim();
        const plotGridColor = styles.getPropertyValue("--c_plotly_grid").trim();
        plotlyLayoutGlobal = {
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
        const plotlyConfig = {responsive: true};


        //Get plot div element
        const plotElement = $("#plot");

        //Plot div already flagged with Plotly class, update it
        if (plotElement.hasClass("has-plotly-plot"))
            Plotly.update("plot", [], plotlyLayoutGlobal);

        //Otherwise, create a new plot
        else {

            plotElement.addClass("has-plotly-plot");
            console.log("Creating new plot...");
            Plotly.newPlot("plot", [], plotlyLayoutGlobal, plotlyConfig);

        }

    }

    cesiumFlightTrackedSet(newFlightId) {

        this.cesiumRef.current.cesiumFlightTrackedSet(newFlightId);

        console.log(`Setting cesium flight to: ${  newFlightId}`);

        this.setState({ cesiumFlightsSelected: newFlightId });

    }

    cesiumFlightTrackedClear() {

        console.log("Clearing cesium flight selection");

        this.cesiumFlightTrackedSet(CESIUM_FLIGHT_TRACKED_NONE);

    }

    handleAddFilter = (flightId) => {

        const newRule = {
            type: "RULE",
            inputs: ["Flight ID", "=", flightId.toString()]
        };

        //Find or create the special OR group for Flight IDs
        const filters = {...this.state.filters};
        let flightIdGroup = filters.filters.find(
            f => f.type === "GROUP" && f.condition === "OR" && f.isFlightIdGroup
        );

        //No OR group exists, create new one
        if (!flightIdGroup) {

            flightIdGroup = {
                type: "GROUP",
                condition: "OR",
                isFlightIdGroup: true, //<-- Identifier for the special group
                filters: [newRule]
            };
            filters.filters.push(flightIdGroup);

            //Otherwise, add to existing OR group
        } else {

            //Check if a rule for this flight ID already exists
            const FLIGHT_RULE_ID_INDEX = 2;
            const existingRule = flightIdGroup.filters.find(
                f => (f.type === "RULE") && (f.inputs[FLIGHT_RULE_ID_INDEX] === flightId.toString())
            );

            //No existing rule found, add the new rule
            if (!existingRule)
                flightIdGroup.filters.push(newRule);

        }

        //Clear the special group if it's empty
        if (flightIdGroup.filters.length === 0)
            filters.filters = filters.filters.filter(f => f !== flightIdGroup);

        this.setFilter(filters);

    };


    copyFilterURL = () => {

        //Create a deep copy of the filters
        const filtersIn = JSON.parse(JSON.stringify(this.state.filters));
        console.log("Filters In: ", filtersIn);

        //Recursively cull empty filter groups
        const filterCull = (filtersIn) => {

            filtersIn.filters = filtersIn.filters.filter((filter) => {

                //Filter is a group...
                if (filter.type === "GROUP") {

                    //Recursively cull empty groups
                    filterCull(filter);

                    //Remove empty groups (i.e. 'filters' array is empty)
                    return (filter.filters.length > 0);

                }

                //Keep all other filters
                return true;

            });

        };

        filterCull(filtersIn);

        //Convert the filters to a JSON string
        const filterJsonString = JSON.stringify(filtersIn);
        console.log("Filter JSON String: ", filterJsonString);

        //Encode the filter string
        const encodedFilter = encodeURIComponent(filterJsonString);
        console.log("Encoded Filter: ", encodedFilter);

        //Construct the full URL
        const fullURL = `${window.location.origin}${window.location.pathname}?filter=${encodedFilter}`;

        //Copy the URL to the clipboard
        navigator.clipboard.writeText(fullURL)
            .then(() => alert(`Copied current filter as a shareable URL!\n\n(URL Length: ${fullURL.length})`))
            .catch(console.error);

    };

    render() {

        const sortableColumnsHumanReadable = Array.from(sortableColumns.keys());
        const CESIUM_RESOLUTION_SCALE_OPTIONS = [CESIUM_RESOLUTION_PASSTHROUGH, 0.50, 1.00, 2.00, /*4.00*/];


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
                style={{flex: "0 0 auto"}}
            >
                <Filter
                    submitButtonName="Apply Current Filter"
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
                    copyFilterURL={() => this.copyFilterURL()}
                />
            </div>
        );

        //Search Results
        const searchResults = (
            <div
                id="flights-card-container"
                className="card d-flex"
                style={{
                    overflowY: "scroll",
                    flex: "1 1 auto",
                    border: "1px solid var(--c_border_alt)",
                    borderRadius: "0.25em"
                }}
            >
                <FlightsCard
                    parent={this}
                    layers={this.state.layers}
                    flights={this.state.flights}
                    navBar={this.navRef}
                    ref={(elem) => (this.flightsRef = elem)}
                    showMap={() => {
                        this.showMap();
                    }}
                    hideMap={() => this.hideMap()}
                    showPlot={() => {
                        this.showPlot();
                    }}
                    setAvailableLayers={(plotLayers) => {
                        this.setAvailableLayers(plotLayers);
                    }}
                    setFlights={(flights) => {
                        this.setState({flights: flights,});
                    }}
                    updateNumberPages={(numberPages) => {
                        this.setState({numberPages: numberPages,});
                    }}
                    addTag={(flightId, name, description, color) => this.addTag(flightId, name, description, color)}
                    removeTag={(flightId, tagId, perm) => this.removeTag(flightId, tagId, perm)}
                    deleteTag={(flightId, tagId) => this.deleteTag(flightId, tagId)}
                    getUnassociatedTags={(flightId) => this.getUnassociatedTags(flightId)}
                    associateTag={(tagId, flightId) => this.associateTag(tagId, flightId)}
                    clearTags={(flightId) => this.clearTags(flightId)}
                    editTag={(currentTag, newTag) => this.editTag(currentTag, newTag)}
                    showCesium={(flightId, color) => {
                        this.addCesiumFlight(flightId, color);
                    }}
                    addCesiumFlightPhase={(phase, flightId) => {
                        this.addCesiumFlightPhase(phase, flightId);
                    }}
                    addCesiumEventEntity={(event, flightId) => {
                        this.addCesiumEventEntity(event, flightId);
                    }}
                    removeCesiumFlight={(flightId) => {
                        this.removeCesiumFlight(flightId);
                    }}
                    zoomToEventEntity={(eventId, flightId) => {
                        this.zoomToEventEntity(eventId, flightId);
                    }}
                    cesiumFlightTrackedSet={(flightId) => {
                        this.cesiumFlightTrackedSet(flightId);
                    }}
                    cesiumJumpToFlightStart={(flightId) => {
                        this.cesiumRef.current.cesiumJumpToFlightStart(flightId);
                    }}
                    onAddFilter={this.handleAddFilter}
                />
            </div>
        );

        //Search Paginator
        const searchPaginator = (
            <div
                id="search-paginator"
                style={{flex: "0 0 auto"}}
            >
                <Paginator
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
                        this.setState({ currentPage: currentPage }, () => {
                            this.submitFilter(false);
                        });
                    }}
                    updateItemsPerPage={(pageSize) => {
                        this.setState({ pageSize: pageSize }, () => {
                            this.submitFilter(true);
                        });
                    }}
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
                    padding: "0.5em",
                    gap: "0.5em",
                    flex: "1 0 0",
                    minWidth: SEARCH_CONTAINER_WIDTH,
                    maxWidth: SEARCH_CONTAINER_WIDTH,
                    width: SEARCH_CONTAINER_WIDTH,
                    minHeight: SEARCH_CONTAINER_HEIGHT,
                    maxHeight: SEARCH_CONTAINER_HEIGHT,
                    height: SEARCH_CONTAINER_HEIGHT,
                    backgroundColor: `rgba(0, 0, 255, ${CONTAINER_BACKGROUND_COLOR_ALPHA})`
                }}>
                {/* {this.state.filterVisible && searchFilters} */}
                {searchFilters}
                {searchResults}
                {searchPaginator}
            </div>
        );

        //Plot Graphic Item
        const plotDiv = (<div id="plot" className="h-100"></div>);
        const plotGraphicItem = (
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
                    style={{position: "absolute", top: "0", left: "0"}}
                    onClick={() => this.expandContainer("plot-container")}
                >
                    <i className="fa fa-expand p-1"/>
                </div>
            </div>
        );

        //Cesium Flight Tracker Button
        const cesiumFlightTrackerButton = (
            <div
                className="btn btn-outline-secondary"
                id="flightTracerDisplay"
                onClick={() => this.cesiumFlightTrackedClear()}
                onMouseEnter={() => this.setState({flightTrackerHovered: true})}
                onMouseLeave={() => this.setState({flightTrackerHovered: false})}
            >
                <i
                    className={this.state.flightTrackerHovered ? "fa fa-close" : "fa fa-camera"}
                    style={{marginRight: "0.25em", minWidth: "1.25em", minHeight: "1.25em"}}
                />
                Tracked Flight: {this.state.cesiumFlightsSelected ?? "(None)"}
            </div>
        );

        //Cesium Graphic Item
        const ITEM_WIDTH = "100%";
        const ITEM_HEIGHT = "100%";
        const cesiumGraphicItem = (
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
                    cesiumResolutionScale={this.state.cesiumResolutionScale}
                    cesiumResolutionUseDefault={this.state.cesiumResolutionUseDefault}
                />

                {/* Additional Cesium Controls */}
                <div style={{position: "absolute", top: "0", left: "0"}}
                     className="d-flex align-items-center justify-content-center gap-1">

                    {/* Expand Cesium Button */}
                    <div
                        className="map-graph-expand-button btn btn-outline-secondary"
                        onClick={() => this.expandContainer("cesium-container")}
                    >
                        <i className="fa fa-expand p-1"/>
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
        const mapGraphicItem = (
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
                     style={{position: "absolute", top: "0", left: "0"}}
                     onClick={() => this.expandContainer("map-container")}>
                    <i className="fa fa-expand p-1"/>
                </div>
            </div>
        );

        //Fake "constants" for the graphics container expanded condition
        let GRAPHICS_CONTAINER_WIDTH, GRAPHICS_CONTAINER_HEIGHT, GRAPHICS_CONTAINER_FLEX_TYPE,
            GRAPHICS_CONTAINER_PADDING;

        //Graphics Container [Expanded]
        if (this.state.containerExpanded) {

            GRAPHICS_CONTAINER_WIDTH = "100%";
            GRAPHICS_CONTAINER_HEIGHT = "100%";
            GRAPHICS_CONTAINER_FLEX_TYPE = "column";
            GRAPHICS_CONTAINER_PADDING = "0.5em 0.5em 0.5em 0.5em";    //Padding on all sides

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
                    flex: "1 0 0",
                    gap: "0.5em",
                    padding: GRAPHICS_CONTAINER_PADDING,
                    minWidth: GRAPHICS_CONTAINER_WIDTH,
                    maxWidth: GRAPHICS_CONTAINER_WIDTH,
                    width: GRAPHICS_CONTAINER_WIDTH,
                    minHeight: GRAPHICS_CONTAINER_HEIGHT,
                    maxHeight: GRAPHICS_CONTAINER_HEIGHT,
                    height: GRAPHICS_CONTAINER_HEIGHT,
                    backgroundColor: `rgba(0, 255, 0, ${CONTAINER_BACKGROUND_COLOR_ALPHA})`
                }}
            >
                {plotGraphicItem}
                {cesiumGraphicItem}
                {mapGraphicItem}
            </div>
        );


        return (
            <div style={{
                overflowY: "hidden",
                overflowX: "hidden",
                display: "flex",
                flexDirection: "column",
                height: "100vh",
                maxHeight: "100vh",
                maxWidth: "100vw",
                width: "100vw"
            }}>

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
                        darkModeOnClickAlt={() => {
                            this.displayPlot();
                        }}
                    />
                </div>

                {/* Main Content Subcontainer */}
                <div className={`d-flex flex-${MAIN_CONTENT_CONTAINER_FLEX_TYPE}`}
                     style={{overflowY: "auto", overflowX: "hidden", flex: "1 1 auto",}}>

                    {graphicsContainer}
                    {searchContainer}

                </div>

            </div>
        );

    }
}


const container = document.querySelector("#flights-page");
const root = createRoot(container);
root.render(<FlightsPage />);


export {plotlyLayoutGlobal};