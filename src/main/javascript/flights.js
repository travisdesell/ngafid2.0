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
                options : doubleTimeSeriesNames
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
            filterRef : React.createRef(),
            flightsRef : React.createRef(),
            flights : undefined, //start out with no specified flights

            //needed for paginator
            currentPage : 0,
            numberPages : 1,
            pageSize : 10
        };

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

    submitFilter(resetCurrentPage = false) {
        console.log("submitting filter! currentPage: " + this.state.currentPage + ", pageSize: " + this.state.pageSize);

        let query = this.filterRef.getQuery();

        console.log("Submitting filters:");
        console.log( query );

        $("#loading").show();

        //reset the current page to 0 if the page size or filter
        //have changed
        let currentPage = this.state.currentPage;
        if (resetCurrentPage === true) {
            currentPage = 0;
        }

        var submissionData = {
            filterQuery : JSON.stringify(query),
            currentPage : currentPage,
            pageSize : this.state.pageSize
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

        return (
            <div>
                <SignedInNavbar 
                    activePage="flights"
                    filterVisible={this.state.filterVisible}
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
                    waitingUserCount={waitingUserCount}
                    fleetManager={fleetManager}
                    unconfirmedTailsCount={unconfirmedTailsCount}
                    modifyTailsAccess={modifyTailsAccess}
                />

                <div id="plot-map-div" className='row m-0' style={{width:"100%", height:"0%"}}>
                    <div id="map" className="map" style={{width:"50%", display:"none"}}></div> 
                    <div id="plot" style={{width:"50%", display:"none"}}></div>
                </div>

                <div style={style}>
                    <Filter
                        ref={elem => this.filterRef = elem}
                        submitFilter={(resetCurrentPage) => {this.submitFilter(resetCurrentPage);}}
                        filterVisible={this.state.filterVisible}
                        depth={0}
                        baseIndex="[0-0]"
                        key="[0-0]"
                        parent={null}
                        type="GROUP"
                        rules={rules}
                        submitButtonName="Apply Filter"
                    />

                    <Paginator
                        submitFilter={(resetCurrentPage) => {this.submitFilter(resetCurrentPage);}}
                        items={this.state.flights}
                        itemName="flights"
                        currentPage={this.state.currentPage}
                        numberPages={this.state.numberPages}
                        pageSize={this.state.pageSize}
                        updateCurrentPage={(currentPage) => {
                            this.state.currentPage = currentPage;
                        }}
                        updateItemsPerPage={(pageSize) => {
                            this.state.pageSize = pageSize;
                        }}
                    />

                    <FlightsCard
                        parent={this}
                        flights={this.state.flights} 
                        ref={elem => this.flightsRef = elem}
                        showMap={() => {this.showMap();}}
                        showPlot={() => {this.showPlot();}}
                        flights={this.state.flights}
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
                        currentPage={this.state.currentPage}
                        numberPages={this.state.numberPages}
                        pageSize={this.state.pageSize}
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
//<<<<<<< HEAD
//                return(
//					<span key={index} className="badge badge-primary" style={{lineHeight : '1.5', marginRight : '4px', backgroundColor : '#e3e3e3', color : '#000000'}} title={tag.description}>
//                        <span className="badge badge-pill badge-primary" style={style} page={this.state.page}>
//							<i className="fa fa-tag" aria-hidden="true"></i>
//						</span>   {tag.name}
//					</span>
//                );
//            });
//        }
//
//        return (
//            <div className="card mb-1">
//                <div className="card-body m-0 p-0">
//                    <div className="d-flex flex-row p-1">
//                        <div className={firstCellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
//                            <i className="fa fa-plane p-1"> {flightInfo.id}</i>
//                        </div>
//
//                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
//                            {flightInfo.tailNumber}
//                        </div>
//
//                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
//                            {flightInfo.systemId}
//                        </div>
//
//
//                        <div className={cellClasses} style={{flexBasis:"120px", flexShrink:0, flexGrow:0}}>
//
//                            {flightInfo.airframeType}
//                        </div>
//
//                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>
//
//                            {flightInfo.startDateTime}
//                        </div>
//
//                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>
//
//                            {flightInfo.endDateTime}
//                        </div>
//
//                        <div className={cellClasses} style={{flexBasis:"80px", flexShrink:0, flexGrow:0}}>
//
//                            {moment.utc(endTime.diff(startTime)).format("HH:mm:ss")}
//                        </div>
//
//                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>
//                            {visitedAirports.join(", ")}
//                        </div>
//
//                        <div className={cellClasses} style={{
//							flexGrow:1,
//							//textShadow: '-1px -1px 0 #000, 1px -1px 0 #000, -1px 1px 0 #000, 1px 1px 0 #000'
//						}}>
//
//                            <div>
//                                {tagPills}
//                            </div>
//                        </div>
//
//                        <div className="p-0">
//                            <button className={buttonClasses} data-toggle="button" aria-pressed="false" style={styleButton} onClick={() => this.exclamationClicked()}>
//                                <i className="fa fa-exclamation p-1"></i>
//                            </button>
//
//                            <button className={buttonClasses} data-toggle="button" title={tagTooltip} aria-pressed="false" style={styleButton} onClick={() => this.tagClicked()}>
//                                <i className="fa fa-tag p-1"></i>
//                            </button>
//
//                            <button className={buttonClasses + globeClasses} data-toggle="button" title={globeTooltip} aria-pressed="false" style={styleButton} onClick={() => this.globeClicked()}>
//                                <i className="fa fa-map-o p-1"></i>
//                            </button>
//
//                            <button className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.plotClicked()}>
//                                <i className="fa fa-area-chart p-1"></i>
//                            </button>
//
//                            <button className={buttonClasses + globeClasses} disabled={traceDisabled} style={styleButton} onClick={() => this.cesiumClicked()}>
//                                <i className="fa fa-globe p-1"></i>
//                            </button>
//
//                            <button className={buttonClasses + " disabled"} style={styleButton} onClick={() => this.replayClicked()}>
//                                <i className="fa fa-video-camera p-1"></i>
//                            </button>
//
//                            <button className={lastButtonClasses + globeClasses} disabled={traceDisabled} style={styleButton} onClick={() => this.downloadClicked()}>
//                                <i className="fa fa-download p-1"></i>
//                            </button>
//                        </div>
//                    </div>
//
//                    {itineraryRow}
//
//                    {tagsRow}
//
//                    {eventsRow}
//
//                    {tracesRow}
//                </div>
//            </div>
//        );
//    }
//}
//
//
//class FlightsCard extends React.Component {
//    constructor(props) {
//        super(props);
//
//        this.state = {
//            mapVisible : false,
//            plotVisible : false,
//            filterVisible : true,
//            page : 0,
//            buffSize : 10,   //def size of flights to show per page is 10
//            numPages : 0
//        };
//
//       this.previousPage = this.previousPage.bind(this);
//       this.nextPage = this.nextPage.bind(this);
//       this.repaginate = this.repaginate.bind(this);
//       this.updateState = this.updateState.bind(this);
//       this.filterRef = React.createRef();
//    }
//
//    setFlights(flights) {
//        this.state.flights = flights;
//        this.setState(this.state);
//    }
//
//	//used to update the state from a child component
//	updateState(newFlights){
//		this.setFlights(newFlights);
//	}
//
//    setIndex(index){
//        this.state.page = index;
//        this.setState(this.state);
//    }
//
//    setSize(size){
//        this.state.numPages = size;
//        this.setState(this.state);
//    }
//
//    mapSelectChanged(style) {
//        //layers and styles from plots.js
//        for (var i = 0, ii = layers.length; i < ii; ++i) {
//
//            console.log("setting layer " + i + " to:" + (styles[i] === style));
//            layers[i].setVisible(styles[i] === style);
//        }
//    }
//
//    showMap() {
//        if (this.state.mapVisible) return;
//
//        if ( !$("#map-toggle-button").hasClass("active") ) {
//            $("#map-toggle-button").addClass("active");
//            $("#map-toggle-button").attr("aria-pressed", true);
//        }
//
//        this.state.mapVisible = true;
//        this.setState(this.state);
//
//        $("#plot-map-div").css("height", "50%");
//        $("#map").show();
//
//        if (this.state.plotVisible) {
//            $("#map").css("width", "50%");
//            map.updateSize();
//            $("#plot").css("width", "50%");
//            Plotly.Plots.resize("plot");
//        } else {
//            $("#map").css("width", "100%");
//            map.updateSize();
//        }
//
//    }
//
//    hideMap() {
//        if (!this.state.mapVisible) return;
//
//        if ( $("#map-toggle-button").hasClass("active") ) {
//            $("#map-toggle-button").removeClass("active");
//            $("#map-toggle-button").attr("aria-pressed", false);
//        }
//
//        this.state.mapVisible = false;
//        this.setState(this.state);
//
//        $("#map").hide();
//
//        if (this.state.plotVisible) {
//            $("#plot").css("width", "100%");
//            var update = { width : "100%" };
//            Plotly.Plots.resize("plot");
//        } else {
//            $("#plot-map-div").css("height", "0%");
//        }
//    }
//
//    toggleMap() {
//        if (this.state.mapVisible) {
//            this.hideMap();
//        } else {
//            this.showMap();
//        }
//    }
//
//    showPlot() {
//        if (this.state.plotVisible) return;
//
//        if ( !$("#plot-toggle-button").hasClass("active") ) {
//            $("#plot-toggle-button").addClass("active");
//            $("#plot-toggle-button").attr("aria-pressed", true);
//        }
//
//        this.state.plotVisible = true;
//        this.setState(this.state);
//
//        $("#plot").show();
//        $("#plot-map-div").css("height", "50%");
//
//        if (this.state.mapVisible) {
//            $("#map").css("width", "50%");
//            map.updateSize();
//            $("#plot").css("width", "50%");
//            Plotly.Plots.resize("plot");
//        } else {
//            $("#plot").css("width", "100%");
//            Plotly.Plots.resize("plot");
//        }
//    }
//
//    hidePlot() {
//        if (!this.state.plotVisible) return;
//
//        if ( $("#plot-toggle-button").hasClass("active") ) {
//            $("#plot-toggle-button").removeClass("active");
//            $("#plot-toggle-button").attr("aria-pressed", false);
//        }
//
//        this.state.plotVisible = false;
//        this.setState(this.state);
//
//        $("#plot").hide();
//
//        if (this.state.mapVisible) {
//            $("#map").css("width", "100%");
//            map.updateSize();
//        } else {
//            $("#plot-map-div").css("height", "0%");
//        }
//    }
//
//    togglePlot() {
//        if (this.state.plotVisible) {
//            this.hidePlot();
//        } else {
//            this.showPlot();
//        }
//    }
//
//    showFilter() {
//        if (this.state.filterVisible) return;
//
//        if ( !$("#filter-toggle-button").hasClass("active") ) {
//            $("#filter-toggle-button").addClass("active");
//            $("#filter-toggle-button").attr("aria-pressed", true);
//        }
//
//        this.state.filterVisible = true;
//        this.setState(this.state);
//
//        //$("#filter").show();
//    }
//
//    hideFilter() {
//        if (!this.state.filterVisible) return;
//
//        if ( $("#filter-toggle-button").hasClass("active") ) {
//            $("#filter-toggle-button").removeClass("active");
//            $("#filter-toggle-button").attr("aria-pressed", false);
//        }
//
//        this.state.filterVisible = false;
//        this.setState(this.state);
//
//        //$("#filter").hide();
//    }
//
//    toggleFilter() {
//        if (this.state.filterVisible) {
//            this.hideFilter();
//        } else {
//            this.showFilter();
//        }
//    }
//
//	/**
//	 * Jumps to a page in this collection of queried flights
//	 * @param pg the page to jump to
//	 */
//    jumpPage(pg){
//        if(pg < this.state.numPages && pg >= 0){
//            this.state.page = pg;
//            this.submitFilter();
//        }
//    }
//
//	/**
//	 * jumps to the next page in this collection of queried flights
//	 */
//    nextPage(){
//        this.state.page++;
//        this.submitFilter();
//    }
//
//	/**
//	 * jumps to the previous page in this collection of queried flights
//	 */
//    previousPage(){
//        this.state.page--;
//        this.submitFilter();
//    }
//
//	/**
//	 * Repaginates the page configuration when the numPerPage field has been changed by the user
//	 */
//    repaginate(pag){
//        console.log("Re-Paginating");
//        this.state.buffSize = pag;
//        this.submitFilter();
//    }
//
//    submitFilter() {
//        //console.log( this.state.filters );
//
//        let query = this.filterRef.current.getQuery();
//
//        console.log("Submitting filters:");
//        console.log( query );
//
//        $("#loading").show();
//
//        var submissionData = {
//            filterQuery : JSON.stringify(query),
//            pageIndex : this.state.page,
//            numPerPage : this.state.buffSize
//        };
//
//        console.log(submissionData);
//
//        $.ajax({
//            type: 'POST',
//            url: '/protected/get_flights',
//            data : submissionData,
//            dataType : 'json',
//            success : function(response) {
//
//                console.log(response);
//
//                $("#loading").hide();
//
//                if (response.errorTitle) {
//                    console.log("displaying error modal!");
//                    errorModal.show(response.errorTitle, response.errorMessage);
//                    return false;
//                }
//
//                console.log("got response: "+response+" "+response.size);
//
//                //get page data
//                flightsCard.setFlights(response.data);
//                flightsCard.setIndex(response.index);
//                flightsCard.setSize(response.sizeAll);
//            },
//            error : function(jqXHR, textStatus, errorThrown) {
//                errorModal.show("Error Loading Flights", errorThrown);
//            },
//            async: true
//        });
//    }
//
//	/**
//	 * Generates an array representing all the pages in this collection of
//	 * queried flights
//	 * @return an array of String objects containing page names
//	 */
//    genPages(){
//        var page = [];
//        for(var i = 0; i<this.state.numPages; i++){
//            page.push({
//                value : i,
//                name : "Page "+(i+1)
//            });
//        }
//        return page;
//    }
//
//	/**
//	 * Renders the flightsCard
//	 */
//    render() {
//        console.log("rendering flights!");
//
//        let flights = [];
//        if (typeof this.state.flights != 'undefined') {
//            flights = this.state.flights;
//
//        }
//
//        let pages = this.genPages();
//
//        let style = null;
//        if (this.state.mapVisible || this.state.plotVisible) {
//            console.log("rendering half");
//            style = {
//                overflow : "scroll",
//                height : "calc(50% - 56px)"
//            };
//        } else {
//            style = {
//                overflow : "scroll",
//                height : "calc(100% - 56px)"
//            };
//        }
//
//        style.padding = "5";
//        if(flights == null || flights.length > 0){
//            var begin = this.state.page == 0;
//            var end = this.state.page == this.state.numPages-1;
//            var prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage}>Previous Page</button>
//            var next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage}>Next Page</button>
//
//            if(begin) {
//                prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage} disabled>Previous Page</button>
//            }
//            if(end){
//                next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage} disabled>Next Page</button>
//=======
//>>>>>>> 05d2a135ea53f8377cc823f9fcb06cf8cf22fd06
            }
        });
    });

});
