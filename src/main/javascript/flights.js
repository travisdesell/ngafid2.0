import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";
import { navbar } from "./signed_in_navbar.js";
import { map, styles, layers, Colors } from "./map.js";

import {fromLonLat, toLonLat} from 'ol/proj.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Icon, Stroke, Style} from 'ol/style.js';
import Feature from 'ol/Feature.js';
import LineString from 'ol/geom/LineString.js';
import { Filter } from './filter.js';


var moment = require('moment');


import Plotly from 'plotly.js';

/*
var airframes = [ "PA-28-181", "Cessna 172S", "PA-44-180", "Cirrus SR20"  ];
var tailNumbers = [ "N765ND", "N744ND", "N771ND", "N731ND", "N714ND", "N766ND", "N743ND" , "N728ND" , "N768ND" , "N713ND" , "N732ND", "N718ND" , "N739ND" ];
var doubleTimeSeriesNames = [ "E1 CHT1", "E1 CHT2", "E1 CHT3" ];
var visitedAirports = [ "GFK", "FAR", "ALB", "ROC" ];
*/

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

];


class Itinerary extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            color : this.props.color
        }
    }

    itineraryClicked(index) {
        flightsCard.showMap();

        var stop = this.props.itinerary[index];
        let modifiedIndex = parseInt(stop.minAltitudeIndex) - this.props.nanOffset;
        //console.log("index: " + stop.minAltitudeIndex + ", nanOffset: " + this.props.nanOffset + ", modifeid_index: " + modifiedIndex);

        let latlon = this.props.coordinates[modifiedIndex];
        //console.log(latlon);

        const coords = fromLonLat(latlon);
        map.getView().animate({center: coords, zoom: 13});
    }

    changeColor(event) {
        this.setState({
            color : event.target.value
        });
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        /*
        let cellClasses = "p-1 card mr-1 flex-fill"
        let itinerary = this.props.itinerary;

        let result = "";
        for (let i = 0; i < itinerary.length; i++) {
            result += itinerary[i].airport + " (" + itinerary[i].runway + ")";
            if (i != (itinerary.length - 1)) result += " => ";
        }
        */

        return (
            <div className={cellClasses} style={cellStyle}>
                <div style={{flex: "0 0"}}>
                    <input type="color" name="itineraryColor" value={this.state.color} onChange={(event) => {this.changeColor(event); this.props.flightColorChange(this.props.parent, event)}} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                </div>

                {
                    this.props.itinerary.map((stop, index) => {
                        let identifier = stop.airport;
                        if (stop.runway != null) identifier += " (" + stop.runway + ")";

                        return (
                            <button className={buttonClasses} key={index} style={styleButton} onClick={() => this.itineraryClicked(index)}>
                                { identifier }
                            </button>
                        );
                    })
                }
            </div>
        );

    }
}

class TraceButtons extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            parentFlight : this.props.parentFlight
        };
    }

    traceClicked(seriesName) {
        flightsCard.showPlot();

        let parentFlight = this.state.parentFlight;

        //check to see if we've already loaded this time series
        if (!(seriesName in parentFlight.state.traceIndex)) {
            var thisTrace = this;

            console.log(seriesName);
            console.log("seriesName: " + seriesName + ", flightId: " + this.props.flightId);

            var submissionData = {
                request : "GET_DOUBLE_SERIES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightId,
                seriesName : seriesName
            };   

            $.ajax({
                type: 'POST',
                url: '/protected/double_series',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    var trace = {
                        x : response.x,
                        y : response.y,
                        mode : "lines",
                        //marker : { size: 1},
                        name : thisTrace.props.flightId + " - " + seriesName
                    }

                    //set the trace number for this series
                    parentFlight.state.traceIndex[seriesName] = $("#plot")[0].data.length;
                    parentFlight.state.traceVisibility[seriesName] = true;
                    parentFlight.setState(parentFlight.state);

                    Plotly.addTraces('plot', [trace]);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        } else {
            //toggle visibility for this series
            let visibility = !parentFlight.state.traceVisibility[seriesName];
            parentFlight.state.traceVisibility[seriesName] = visibility;
            parentFlight.setState(parentFlight.state);

            console.log("toggled visibility to: " + visibility);

            Plotly.restyle('plot', { visible: visibility }, [ parentFlight.state.traceIndex[seriesName] ])
        }
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let parentFlight = this.state.parentFlight;


        return (
            <div className={cellClasses} style={cellStyle}>
                {
                    parentFlight.state.traceNames.map((traceName, index) => {
                        let ariaPressed = parentFlight.state.traceVisibility[traceName];
                        let active = "";
                        if (ariaPressed) active = " active";

                        return (
                            <button className={buttonClasses + active} key={traceName} style={styleButton} data-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
                                {traceName}
                            </button>
                        );
                    })
                }
            </div>
        );
    }
}

class Flight extends React.Component {
    constructor(props) {
        super(props);

        let color = Colors.randomValue();
        console.log("flight color: " );
        console.log(color);

        this.state = {
            pathVisible : false,
            mapLoaded : false,
            traceNames : null,
            traceIndex : [],
            traceVisibility : [],
            traceNamesVisible : false,
            itineraryVisible : false,
            layer : null,
            color : color
        }
    }

    componentWillUnmount() {
        console.log("unmounting:");
        console.log(this.props.flightInfo);

        if (this.props.flightInfo.has_coords === "0") return;

        console.log("hiding flight path");
        this.state.pathVisible = false;
        this.state.itineraryVisible = false;
        if (this.state.layer) {
            this.state.layer.setVisible(false);
        }

        console.log("hiding plots");
        if (this.state.traceNames) {
            let visible = false;

            for (let i = 0; i < this.state.traceNames.length; i++) {
                let seriesName = this.state.traceNames[i];
                //check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }
        }
        this.state.traceNamesVisible = false;
    }

    plotClicked() {
        if (this.state.traceNames == null) {
            var thisFlight = this;

            var submissionData = {
                request : "GET_DOUBLE_SERIES_NAMES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightInfo.id
            };   

            $.ajax({
                type: 'POST',
                url: '/protected/double_series_names',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    var names = response.names;

                    //set the trace number for this series
                    thisFlight.state.traceNames = response.names;
                    thisFlight.state.traceNamesVisible = true;
                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    this.state.traceNames = null;
                    errorModal.show("Error Getting Potentail Plot Parameters", errorThrown);
                },   
                async: true 
            });  
        } else {
            let visible = !this.state.traceNamesVisible;

            for (let i = 0; i < this.state.traceNames.length; i++) {
                let seriesName = this.state.traceNames[i];
                //check and see if this series was loaded in the past
                if (seriesName in this.state.traceIndex) {

                    //this will make make a trace visible if it was formly set to visible and the plot button this flight is clicked on
                    //otherwise it will hide them
                    Plotly.restyle('plot', { visible: (visible && this.state.traceVisibility[seriesName]) }, [ this.state.traceIndex[seriesName] ])
                }
            }

            this.state.traceNamesVisible = !this.state.traceNamesVisible;
            this.setState(this.state);
        }

        /*
        var trace3 = {
            x: [1, 2, 3, 4, 5],
            y: [11, 8, 1, 7, 13],
            type: 'scatter',
            yaxis: 'y2'
        };
        var data_update = [trace3];
        var layout_update = {
            title : "new title!"
        }

        console.log($("#plot"));
        console.log($("#plot")[0].layout);
        var layout = $("#plot")[0].layout;
        layout.yaxis2 = {
            title: 'yaxis2 title',
            titlefont: {color: '#ff7f0e'},
            tickfont: {color: '#ff7f0e'},
            anchor: 'free',
            overlaying: 'y',
            side: 'left',
            position: 0.15
        };

        Plotly.update('plot', $("#plot").data, layout);

        Plotly.addTraces('plot', data_update);
        */
    }

    flightColorChange(target, event) {
        console.log("trace color changed!");
        console.log(event);
        console.log(event.target);
        console.log(event.target.value);

        let color = event.target.value;
        target.state.color = color;

        console.log(target);
        console.log(target.state);

        target.state.layer.setStyle(new Style({
            stroke: new Stroke({
                color: color,
                width: 1.5
            })
        }));
    }

    downloadClicked() {
        window.open("/protected/get_kml?flight_id=" + this.props.flightInfo.id);

        /*
        let filePath = 'https://ngafid.org/protected/get_kml?flight_id=' + this.props.flightInfo.id;
        let downloadName = "flight_" + this.props.flightInfo.id + ".kml";

        console.log("creating download ref: '" + filePath + "'");
        console.log("creating download name: '" + downloadName + "'");

        let a = document.createElement('kml-download');
        a.href = filePath;
        a.download = downloadName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        */
    }

    globeClicked() {
        if (this.props.flightInfo.has_coords === "0") return;

        if (!this.state.mapLoaded) {
            flightsCard.showMap();
            this.state.mapLoaded = true;

            var thisFlight = this;

            var submissionData = {
                request : "GET_COORDINATES",
                id_token : "TEST_ID_TOKEN",
                //id_token : id_token,
                //user_id : user_id
                user_id : 1,
                flightId : this.props.flightInfo.id
            };   

            $.ajax({
                type: 'POST',
                url: '/protected/coordinates',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    //console.log("received response: ");
                    //console.log(response);

                    var coordinates = response.coordinates;

                    var points = [];
                    for (var i = 0; i < coordinates.length; i++) {
                        var point = fromLonLat(coordinates[i]);
                        points.push(point);
                    }

                    var color = thisFlight.state.color;
                    console.log(color);

                    thisFlight.state.layer = new VectorLayer({
                        style: new Style({
                            stroke: new Stroke({
                                color: color,
                                width: 1.5
                            }),
                        }),

                        source : new VectorSource({
                            features: [new Feature({
                                geometry: new LineString(points),
                                name: 'Line'
                            })]
                        }),
                    });
                    thisFlight.state.layer.setVisible(true);
                    thisFlight.state.pathVisible = true;
                    thisFlight.state.itineraryVisible = true;
                    thisFlight.state.nanOffset = response.nanOffset;
                    thisFlight.state.coordinates = response.coordinates;

                    map.addLayer(thisFlight.state.layer);

                    let extent = thisFlight.state.layer.getSource().getExtent();
                    console.log(extent);
                    map.getView().fit(extent, map.getSize());

                    thisFlight.setState(thisFlight.state);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    thisFlight.state.mapLoaded = false;
                    thisFlight.setState(thisFlight.state);

                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        } else {
            //toggle visibility if already loaded
            this.state.pathVisible = !this.state.pathVisible;
            this.state.itineraryVisible = !this.state.itineraryVisible;
            this.state.layer.setVisible(this.state.pathVisible);

            if (this.state.pathVisibile) {
                flightsCard.showMap();
            }

            this.setState(this.state);

            if (this.state.pathVisible) {
                let extent = this.state.layer.getSource().getExtent();
                console.log(extent);
                map.getView().fit(extent, map.getSize());
            }
        }
    }

    render() {
        let buttonClasses = "p-1 mr-1 expand-import-button btn btn-outline-secondary";
        let lastButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        const styleButton = { };

        let firstCellClasses = "p-1 card mr-1"
        let cellClasses = "p-1 card mr-1"

        let flightInfo = this.props.flightInfo;

        let startTime = moment(flightInfo.startDateTime);
        let endTime = moment(flightInfo.endDateTime);

        let globeClasses = "";
        let globeTooltip = "";

        //console.log(flightInfo);
        if (!flightInfo.hasCoords) {
            //console.log("flight " + flightInfo.id + " doesn't have coords!");
            globeClasses += " disabled";
            globeTooltip = "Cannot display flight on the map because the flight data did not have latitude/longitude.";
        } else {
            globeTooltip = "Click the globe to display the flight on the map.";
        }

        let visitedAirports = [];
        for (let i = 0; i < flightInfo.itinerary.length; i++) {
            if ($.inArray(flightInfo.itinerary[i].airport, visitedAirports) < 0) {
                visitedAirports.push(flightInfo.itinerary[i].airport);
            }
        }

        let itineraryRow = "";
        if (this.state.itineraryVisible) {
            itineraryRow = (
                <Itinerary itinerary={flightInfo.itinerary} color={this.state.color} coordinates={this.state.coordinates} nanOffset={this.state.nanOffset} parent={this} flightColorChange={this.flightColorChange}/>
            );
        }

        let tracesRow = "";
        if (this.state.traceNamesVisible) {
            tracesRow = 
                (
                    <TraceButtons parentFlight={this} flightId={flightInfo.id}/>
                );
        }

        return (
            <div className="card mb-1">
                <div className="card-body m-0 p-0">
                    <div className="d-flex flex-row p-1">
                        <div className={firstCellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            <i className="fa fa-plane p-1"> {flightInfo.id}</i>
                        </div>

                        <div className={cellClasses} style={{flexBasis:"100px", flexShrink:0, flexGrow:0}}>
                            {flightInfo.tailNumber}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"120px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.airframeType}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.startDateTime}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"200px", flexShrink:0, flexGrow:0}}>

                            {flightInfo.endDateTime}
                        </div>

                        <div className={cellClasses} style={{flexBasis:"80px", flexShrink:0, flexGrow:0}}>

                            {moment.utc(endTime.diff(startTime)).format("HH:mm:ss")}
                        </div>

                        <div className={cellClasses} style={{flexGrow:1}}>
                            {visitedAirports.join(", ")}
                        </div>

                        <div className="p-0">
                            <button className={buttonClasses + " disabled"} style={styleButton} onClick={() => this.editClicked()}>
                                <i className="fa fa-pencil p-1"></i>
                            </button>

                            <button className={buttonClasses + globeClasses} data-toggle="button" title={globeTooltip} aria-pressed="false" style={styleButton} onClick={() => this.globeClicked()}>
                                <i className="fa fa-globe p-1"></i>
                            </button>

                            <button className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.plotClicked()}>
                                <i className="fa fa-area-chart p-1"></i>
                            </button>

                            <button className={buttonClasses + " disabled"} style={styleButton} onClick={() => this.replayClicked()}>
                                <i className="fa fa-video-camera p-1"></i>
                            </button>

                            <button className={lastButtonClasses + globeClasses} style={styleButton} onClick={() => this.downloadClicked()}>
                                <i className="fa fa-download p-1"></i>
                            </button>
                        </div>
                    </div>

                    {itineraryRow}

                    {tracesRow}
                </div>
            </div>
        );
    }
}


class FlightsCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            mapVisible : false,
            plotVisible : false,
            filterVisible : true,
        }

        this.filterRef = React.createRef();
    }

    setFlights(flights) {
        this.state.flights = flights;
        this.setState(this.state);
    }

    mapSelectChanged(style) {
        //layers and styles from plots.js
        for (var i = 0, ii = layers.length; i < ii; ++i) {

            console.log("setting layer " + i + " to:" + (styles[i] === style));
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

    showFilter() {
        if (this.state.filterVisible) return;

        if ( !$("#filter-toggle-button").hasClass("active") ) { 
            $("#filter-toggle-button").addClass("active");
            $("#filter-toggle-button").attr("aria-pressed", true);
        }

        this.state.filterVisible = true;
        this.setState(this.state);

        //$("#filter").show();
    }

    hideFilter() {
        if (!this.state.filterVisible) return;

        if ( $("#filter-toggle-button").hasClass("active") ) { 
            $("#filter-toggle-button").removeClass("active");
            $("#filter-toggle-button").attr("aria-pressed", false);
        }   

        this.state.filterVisible = false;
        this.setState(this.state);

        //$("#filter").hide();
    }

    toggleFilter() {
        if (this.state.filterVisible) {
            this.hideFilter();
        } else {
            this.showFilter();
        }
    }

    submitFilter() {
        //console.log( this.state.filters );

        let query = this.filterRef.current.getQuery();

        console.log("Submitting filters:");
        console.log( query );

        $("#loading").show();

        var submissionData = {
            filterQuery : JSON.stringify(query)
        };   
        console.log(submissionData);

        $.ajax({
            type: 'POST',
            url: '/protected/get_flights',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                flightsCard.setFlights(response);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
        });  
    }

    render() {
        console.log("rendering flights!");

        let flights = [];
        if (typeof this.state.flights != 'undefined') {
            flights = this.state.flights;
        }

        let style = null;
        if (this.state.mapVisible || this.state.plotVisible) {
            console.log("rendering half");
            style = { 
                overflow : "scroll",
                height : "calc(50% - 56px)"
            };  
        } else {
            style = { 
                overflow : "scroll",
                height : "calc(100% - 56px)"
            };  
        }   
        style.padding = "5";

        return (
            <div className="card-body" style={style}>
                <Filter ref={this.filterRef} hidden={!this.state.filterVisible} depth={0} baseIndex="[0-0]" key="[0-0]" parent={null} type="GROUP" submitFilter={() => {this.submitFilter()}} rules={rules} submitButtonName="Apply Filter"/>

                {
                    flights.map((flightInfo, index) => {
                        return (
                            <Flight flightInfo={flightInfo} key={flightInfo.id} />
                        );
                    })
                }

                <div id="load-more"></div>
            </div>
        );
    }
}

let flightsCard = null;
//check to see if flights has been defined already. unfortunately
//the navbar includes flights.js (bad design) for the navbar buttons
//to toggle flights, etc. So this is a bit of a hack.
if (typeof flights !== 'undefined') {
    flightsCard = ReactDOM.render(
        <FlightsCard />,
        document.querySelector('#flights-card')
    );
    navbar.setFlightsCard(flightsCard);

    console.log("rendered flightsCard!");

    $(document).ready(function() {
        var layout1 = { 
            yaxis: {
                rangemode: 'tozero',
                showline: true,
                zeroline: true
            }
        };
        layout1 = {};

        Plotly.newPlot('plot', [], layout1);
    });
}

export { flightsCard };
