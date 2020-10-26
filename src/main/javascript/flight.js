import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import SignedInNavbar from "./signed_in_navbar.js";
import { map, styles, layers, Colors, initializeMap } from "./map.js";

import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';

import { FlightsCard } from './flights_card_component.js';

import Plotly from 'plotly.js';

global.plotlyLayout = { 
    shapes : []
};

class FlightPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            filterVisible : false,
            //filterSelected : false,
            plotVisible : false,
            plotSelected : false,
            mapVisible : false,
            mapSelected : false,
            mapStyle : "Road",
            filterRef : React.createRef(),
            flightsRef : React.createRef(),
			navRef : React.createRef(),
            flights : flights, //start out with the provided flight
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

    submitFilter() {
        console.log("submitting filter! currentPage: " + this.state.currentPage + ", itemsPerPage: " + this.state.itemsPerPage);
        this.flightsRef.submitFilter(this.state.currentPage, this.state.itemsPerPage);
    }

    getQuery() {
        console.log("getting query!");
        return this.filterRef.getQuery();
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
					ref={elem => this.navRef = elem}
                    unconfirmedTailsCount={unconfirmedTailsCount}
                    modifyTailsAccess={modifyTailsAccess}
                />

                <div id="plot-map-div" className='row m-0' style={{width:"100%", height:"0%"}}>
                    <div id="map" className="map" style={{width:"50%", display:"none"}}></div> 
                    <div id="plot" style={{width:"50%", display:"none"}}></div>
                </div>

                <div style={style}>
                    <FlightsCard
                        parent={this}
                        flights={this.state.flights} 
						navBar={this.navRef}
                        ref={elem => this.flightsRef = elem}
                        showMap={() => {this.showMap();}}
                        showPlot={() => {this.showPlot();}}
                        getFilterQuery={() => {return this.getQuery();}}
                        flights={this.state.flights}
                        setFlights={(flights) => {
                            this.setState({
                                flights : flights
                            });
                        }}
                    />
                </div>
            </div>
        );
    }
}


var flightPage = ReactDOM.render(
    <FlightPage />,
    document.querySelector('#flight-page')
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
