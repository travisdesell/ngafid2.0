import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import SignedInNavbar from "./signed_in_navbar.js";
import { map, styles, layers, Colors, initializeMap } from "./map.js";

import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';

import { errorModal } from "./error_modal.js";
import { Filter } from './filter.js';
import { Paginator } from './paginator_component.js';
import { FlightsCard } from './flights_card_component.js';

import Plotly from 'plotly.js';

import { timeZones } from "./time_zones.js";


/*
var airframes = [ "PA-28-181", "Cessna 172S", "PA-44-180", "Cirrus SR20"  ];
var tailNumbers = [ "N765ND", "N744ND", "N771ND", "N731ND", "N714ND", "N766ND", "N743ND" , "N728ND" , "N768ND" , "N713ND" , "N732ND", "N718ND" , "N739ND" ];
var doubleTimeSeriesNames = [ "E1 CHT1", "E1 CHT2", "E1 CHT3" ];
var visitedAirports = [ "GFK", "FAR", "ALB", "ROC" ];
*/
// var tagNames = ["Tag A", "Tag B"];
var rules = [
    {
        name : "Airframe",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "is", "is not" ]
            },
            { 
                type : "select",
                name : "airframes",
                options : airframes
            }
        ]
    },

    {
        name : "Tail Number",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "is", "is not" ]
            },
            {
                type : "select",
                name : "tail numbers",
                options : tailNumbers
            }
        ]
    },

    {
        name : "System ID",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "is", "is not" ]
            },
            {
                type : "select",
                name : "system id",
                options : systemIds 
            }
        ]
    },


    {
        name : "Duration",
        conditions : [
            { 
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type : "number",
                name : "hours"
            },
            {
                type : "number",
                name : "minutes"
            },
            {
                type : "number",
                name : "seconds"
            }
        ]
    },

    {
        name : "Start Date and Time",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "datetime-local",
                name : "date and time"
            },
            {
                type : "select",
                name : "timezone", 
                options : timeZones
            }
        ]
    },

    {
        name : "End Date and Time",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "datetime-local",
                name : "date and time"
            },
            {
                type : "select",
                name : "timezone", 
                options : timeZones
            }
        ]
    },

    {
        name : "Start Date",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "date",
                name : "date"
            }
        ]
    },

    {
        name : "End Date",
        conditions : [
            {
                type : "select",
                name : "condition", 
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "date",
                name : "date"
            }
        ]
    },

    {
        name : "Start Time",
        conditions : [
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "time",
                name : "time"
            },
            {
                type : "select",
                name : "timezone", 
                options : timeZones
            }
        ]
    },

    {
        name : "End Time",
        conditions : [
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "time",
                name : "time"
            },
            {
                type : "select",
                name : "timezone", 
                options : timeZones
            }
        ]
    },


    {
        name : "Parameter",
        conditions : [
            {
                type : "select",
                name : "statistic",
                options : [ "min", "avg", "max" ]
            },
            {
                type : "select",
                name : "doubleSeries",
                oplufthansations : doubleTimeSeriesNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Airport",
        conditions : [
            { 
                type : "select",
                name : "airports",
                options : visitedAirports
            },
            { 
                type : "select",
                name : "condition",
                options : [ "visited", "not visited" ]
            }
        ]
    },

    {
        name : "Runway",
        conditions : [
            { 
                type : "select",
                name : "runways",
                options : visitedRunways
            },
            { 
                type : "select",
                name : "condition",
                options : [ "visited", "not visited" ]
            }
        ]
    },

    {
        name : "Event Count",
        conditions : [
            {
                type : "select",
                name : "eventNames",
                options : eventNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Event Severity",
        conditions : [
            {
                type : "select",
                name : "eventNames",
                options : eventNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Event Duration",
        conditions : [
            {
                type : "select",
                name : "eventNames",
                options : eventNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "<=", "<", "=", ">", ">=" ]
            },
            {
                type  : "number",
                name : "number"
            }
        ]
    },

    {
        name : "Tag",
        conditions : [
            {
                type : "select",
                name : "flight_tags",
                options : tagNames
            },
            {
                type : "select",
                name : "condition",
                options : [ "Is Associated", "Is Not Associated"]
            },
        ]
    },


];

const sortableColumns = new Map();

sortableColumns.set("Flight Number", "id");
sortableColumns.set("Flight Length (Number of Valid Data Points)", "number_rows");
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
            filterVisible : true,
            filterSelected : true,
            plotVisible : false,
            plotSelected : false,
            mapVisible : false,
            mapSelected : false,
            mapStyle : "Road",
            flightsRef : React.createRef(),
            layers : [],
            flights : undefined, //start out with no specified flights
            sortColumn : "Start Date and Time", //need to define a default here, flt# will alias to primary key server side
            sortingOrder : "Descending", //need to define a default here, descending is default
            storedFilters : this.getStoredFilters(),

            filters : {
                type : "GROUP",
                condition : "AND",
                filters : []
            }, 

            //needed for paginator
            currentPage : 0,
            numberPages : 1,
            pageSize : 10
        };

        this.navRef = React.createRef();
    }

    mapSelectChanged(style) {
        for (var i = 0, ii = layers.length; i < ii; ++i) {
            console.log("setting layer " + i + " to:" + (styles[i] === style));
            layers[i].setVisible(styles[i] === style);
        }   

        console.log("map style changed to: '" +  style + "'!");
        this.setState({
            mapStyle : style
        });
    }

    mapLayerChanged(style) {
        console.log("changing path to: "+style);
        console.log(this.state.selectableLayers);

        for(let i = 0; i < this.state.selectableLayers.length; i++){
            let layer = this.state.selectableLayers[i];
            let name = layer.values_.name;
            
            if(name == style) {
                layer.setVisible(true);
                console.log("setting layer " + name + " to visible");
            } else {
                layer.setVisible(false);
                console.log("setting layer " + name + " to not visible");
            }
        }

        console.log("map layer changed to: '" +  style + "'!");

        this.setMapStyle(style);
        
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

    showMap() {
        if (this.state.mapVisible) return;

        if ( !$("#map-toggle-button").hasClass("active") ) { 
            $("#map-toggle-button").addClass("active");
            $("#map-toggle-button").attr("aria-pressed", true);
        }

        this.state.mapVisible = true;
        this.setState(this.state);

        $("#plot-map-div").css("height", "50%");
        $("#map").show();

        if (this.state.plotVisible) {
            $("#map").css("width", "50%");
            map.updateSize();
            $("#plot").css("width", "50%");
            Plotly.Plots.resize("plot");
        } else {
            $("#map").css("width", "100%");
            map.updateSize();
        }

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

        if (this.state.plotVisible) {
            $("#plot").css("width", "100%");
            var update = { width : "100%" };
            Plotly.Plots.resize("plot");
        } else {
            $("#plot-map-div").css("height", "0%");
        }
    }

    toggleMap() {
        if (this.state.mapVisible) {
            this.hideMap();
        } else {
            this.showMap();
        }
    }

    showPlot() {
        if (this.state.plotVisible) return;

        if ( !$("#plot-toggle-button").hasClass("active") ) { 
            $("#plot-toggle-button").addClass("active");
            $("#plot-toggle-button").attr("aria-pressed", true);
        }

        this.state.plotVisible = true;
        this.setState(this.state);

        $("#plot").show();
        $("#plot-map-div").css("height", "50%");

        if (this.state.mapVisible) {
            $("#map").css("width", "50%");
            map.updateSize();
            $("#plot").css("width", "50%");
            Plotly.Plots.resize("plot");
        } else {
            $("#plot").css("width", "100%");
            Plotly.Plots.resize("plot");
        }
    }

    hidePlot() {
        if (!this.state.plotVisible) return;

        if ( $("#plot-toggle-button").hasClass("active") ) { 
            $("#plot-toggle-button").removeClass("active");
            $("#plot-toggle-button").attr("aria-pressed", false);
        }   

        this.state.plotVisible = false;
        this.setState(this.state);

        $("#plot").hide();

        if (this.state.mapVisible) {
            $("#map").css("width", "100%");
            map.updateSize();
        } else {
            $("#plot-map-div").css("height", "0%");
        }
    }

    /**
     * removes a tag from a flight, either permanent or just from one flight
     * @param id the tagid of the tag being removed
     * @param perm a bool representing whether or not the removal is permanent
     */
    removeTag(id, perm) {
        console.log("un-associating tag #" + id + " with flight #" + this.state.flightId);

        if (id==null || id == -1) {
            errorModal.show("Please select a flight to remove first!", "Cannot remove any flights!");
            return;
        }

        let allTags = (id == -2);

        var submissionData = {
            flight_id : this.state.flightId,
            tag_id : id,
            permanent : perm,
            all : allTags
        };
        
        this.state.activeTag = null;

        let thisFlight = this;
        console.log("calling deletion ajax");

        $.ajax({
            type: 'POST',
            url: '/protected/remove_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                if (perm) {
                    console.log("permanent deletion, refreshing all flights with: ");
                    console.log(response);
                    let allFlights = response.data;
                    thisFlight.state.tags = allFlights[flight_compthisFlight.state.flightIndex].tags.value;
                    thisFlight.state.addFormActive = false;
                    thisFlight.updateFlights(allFlights);
                } else {
                    thisFlight.state.tags = response;
                    thisFlight.setState(thisFlight.state);
                    thisFlight.getUnassociatedTags();
                    thisFlight.state.addFormActive = false;
                    thisFlight.state.addActive = false;
                    thisFlight.updateParent(thisFlight.state.tags);
                }
                thisFlight.setState(thisFlight.state);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

    /**
     * Associates a tag with this flight
     * @param id the tag id to associate
     */
    associateTag(id) {
        console.log("associating tag #"+id+" with flight #"+this.state.flightId);

        var submissionData = {
            id : this.state.flightId,
            tag_id : id
        };

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/associate_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                if (thisFlight.state.tags != null) {
                    thisFlight.state.tags.push(response);
                } else {
                    thisFlight.state.tags = new Array(response);
                    console.log(thisFlight.state.tags);
                }
                thisFlight.getUnassociatedTags();
                thisFlight.updateParent(thisFlight.state.tags);
                thisFlight.setState(thisFlight.state);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

    togglePlot() {
        if (this.state.plotVisible) {
            this.hidePlot();
        } else {
            this.showPlot();
        }
    }


    toggleFilter() {
        console.log("toggling filterVisible to: " + !this.state.filterVisible);
        this.setState({
            filterVisible : !this.state.filterVisible
        });
    }

    setFilter(filter) {
        this.setState({
            filters : filter
        });
    }


    getStoredFilters() {
        let storedFilters = [];

        $.ajax({
            type: 'GET',
            url: '/protected/stored_filters',
            dataType : 'json',
            success : function(response) {
                console.log("received filters response: ");
                console.log(response);
                
                storedFilters = response;
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: false 
        });  

        return storedFilters;
    }

    submitFilter(resetCurrentPage = false) {
        console.log("submitting filter! currentPage: " + this.state.currentPage + ", pageSize: " + this.state.pageSize + " sortByColumn: " + this.state.sortColumn);

        console.log("Submitting filters:");
        console.log( this.state.filters );

        $("#loading").show();

        //reset the current page to 0 if the page size or filter
        //have changed
        let currentPage = this.state.currentPage;
        if (resetCurrentPage === true) {
            currentPage = 0;
        }

        var submissionData = {
            filterQuery : JSON.stringify(this.state.filters),
            currentPage : currentPage,
            pageSize : this.state.pageSize,
            sortingColumn : sortableColumns.get(this.state.sortColumn),
            sortingOrder : this.state.sortingOrder
        };

        console.log(submissionData);

        let flightsPage = this;

        $.ajax({
            type: 'POST',
            url: '/protected/get_flights',
            data : submissionData,
            dataType : 'json',
            timeout : 0, //set timeout to be unlimited for slow queries
            success : function(response) {

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
                    errorModal.show("No flights found with the given parameters!", "Please try a different query.");
                 } else {
                    flightsPage.setState({
                        flights : response.flights,
                        currentPage : currentPage,
                        numberPages : response.numberPages  
                    });

                }
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
        });  
    }

    setAvailableLayers(plotLayers) {
        console.log("changing selectable layers on navbar");
        console.log(plotLayers);

        this.setState({selectableLayers : plotLayers});
    }

    render() {
        let style = null;
        if (this.state.mapVisible || this.state.plotVisible) {
            console.log("rendering half");
            style = { 
                overflow : "scroll",
                height : "calc(50%)"
            };  
        } else {
            style = { 
                overflow : "scroll",
                height : "calc(100%)"
            };  
        }

        style.padding = "5";

        let sortableColumnsHumanReadable = Array.from(sortableColumns.keys());

        return (
            <div>
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
                    toggleMap={() => this.toggleMap()}
                    mapSelectChanged={(style) => this.mapSelectChanged(style)}
                    mapLayerChanged={(style) => this.mapLayerChanged(style)}
                    waitingUserCount={waitingUserCount}
                    fleetManager={fleetManager}
                    ref={this.navRef}
                    unconfirmedTailsCount={unconfirmedTailsCount}
                    modifyTailsAccess={modifyTailsAccess}
                />

                <div id="plot-map-div" className='row m-0' style={{width:"100%", height:"0%"}}>
                    <div id="map" className="map" style={{width:"50%", display:"none"}}></div> 
                    <div id="plot" style={{width:"50%", display:"none"}}></div>
                </div>

                <div style={style}>
                    <Filter
                        filterVisible={this.state.filterVisible}

                        submitButtonName="Apply Filter"
                        submitFilter={(resetCurrentPage) => {this.submitFilter(resetCurrentPage);}}

                        rules={rules}
                        filters={this.state.filters}

                        getFilter={() => {return this.state.filters}}
                        setFilter={(filter) => this.setFilter(filter)}
                        setCurrentSortingColumn={(sortColumn) => this.setCurrentSortingColumn(sortColumn)}
                        getCurrentSortingColumn={() => this.getCurrentSortingColumn()}

                    />

                    <Paginator
                        submitFilter={(resetCurrentPage) => {this.submitFilter(resetCurrentPage);}}
                        items={this.state.flights}
                        itemName="flights"
                        currentPage={this.state.currentPage}
                        numberPages={this.state.numberPages}
                        pageSize={this.state.pageSize}
                        rules={sortableColumns}
                        setSortingColumn={(sortColumn) => this.setSortingColumn(sortColumn)}
                        getSortingColumn={() => this.getSortingColumn()}
                        setSortingOrder={(order) => this.setSortingOrder(order)}
                        getSortingOrder={() => this.getSortingOrder()}
                        sortOptions = {sortableColumnsHumanReadable}
                        updateCurrentPage={(currentPage) => {
                            this.state.currentPage = currentPage;
                        }}
                        updateItemsPerPage={(pageSize) => {
                            this.state.pageSize = pageSize;
                        }}
                    />

                    <FlightsCard
                        parent={this}
                        layers={this.state.layers}
                        flights={this.state.flights} 
                        navBar={this.navRef}
                        ref={elem => this.flightsRef = elem}
                        showMap={() => {this.showMap();}}
                        showPlot={() => {this.showPlot();}}
                        flights={this.state.flights}
                        setAvailableLayers={(plotLayers) => {this.setAvailableLayers(plotLayers);}}
                        setFlights={(flights) => {
                            this.setState({
                                flights : flights
                            });
                        }}
                        updateNumberPages={(numberPages) => {
                            this.setState({
                                numberPages : numberPages
                            });
                        }}
                    />

                    <Paginator
                        submitFilter={(resetCurrentPage) => {this.submitFilter(resetCurrentPage);}}
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
                        sortOptions = {sortableColumnsHumanReadable}
                        updateCurrentPage={(currentPage) => {
                            this.state.currentPage = currentPage;
                        }}
                        updateItemsPerPage={(pageSize) => {
                            this.state.pageSize = pageSize;
                        }}
                    />

                </div>
            </div>
        );
    }
}

global.plotlyLayout = { 
    shapes : []
};

var flightsPage = ReactDOM.render(
    <FlightsPage />,
    document.querySelector('#flights-page')
);


console.log("rendered flightsCard!");


//need to wait for the page to load before initializing maps
//TODO: this is the same as in flights.js, put it in a single spot
$(document).ready(function() {
    Plotly.newPlot('plot', [], global.plotlyLayout);

    initializeMap();

    var myPlot = document.getElementById("plot");
    console.log("myPlot:");
    console.log(myPlot);

    myPlot.on('plotly_hover', function(data) {
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

        map.getLayers().forEach(function(layer) {
            if (layer instanceof VectorLayer) {
                if ('flightState' in layer) {
                    //console.log("VECTOR layer:");

                    var hiddenStyle = new Style({
                        stroke: new Stroke({
                            color: layer.flightState.state.color,
                            width: 1.5
                        }),
                        image: new Circle({
                            radius: 5,
                            stroke: new Stroke({
                                color: [0,0,0,0],
                                width: 2
                            })
                        })
                    });

                    var visibleStyle = new Style({
                        stroke: new Stroke({
                            color: layer.flightState.state.color,
                            width: 1.5
                        }),
                        image: new Circle({
                            radius: 5,
                            stroke: new Stroke({
                                color: layer.flightState.state.color,
                                width: 2
                            })
                        })
                    });

                    if (layer.getVisible()) {
                        if (x < layer.flightState.state.points.length) {
                            console.log("need to draw point at: " + layer.flightState.state.points[x]);
                            layer.flightState.state.trackingPoint.setStyle(visibleStyle);
                            layer.flightState.state.trackingPoint.getGeometry().setCoordinates(layer.flightState.state.points[x]);
                        } else {
                            console.log("not drawing point x: " + x + " >= points.length: " + layer.flightState.state.points.length);
                            layer.flightState.state.trackingPoint.setStyle(hiddenStyle);
                        }
                    }
                }
            }
        });
    });

});
