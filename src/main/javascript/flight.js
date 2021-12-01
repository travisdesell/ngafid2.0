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

function invalidString(str) {
    return (str == null || str.length < 0 || /^\s*$/.test(str));
}

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

    setAvailableLayers(plotLayers) {
        console.log("changing selectable layers on navbar");
        console.log(plotLayers);

        this.setState({selectableLayers : plotLayers});
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

    addTag(flightId, name, description, color) {
        if (invalidString(name) || invalidString(description)) {
            errorModal.show("Error creating tag!",
                            "Please ensure the name and description fields are correctly filled out!");
            return;
        }

        var submissionData = {
            name : name,
            description : description,
            color : color,
            id : flightId
        };
        console.log("Creating a new tag for flight # " + this.state.flightId);

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/create_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
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
                    errorModal.show("Error creating tag", "A tag with that name already exists! Use the dropdown menu to associate it with this flight or give this tag another name");
                }
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

    /**
     * Calls the server using ajax-json to notify it of the new tag change
     */
    editTag(newTag, currentTag) {
        console.log("submitting edit for tag: "+currentTag.hashId);

        console.log("current tag");
        console.log(currentTag);

        console.log("new tag");
        console.log(newTag);

        var submissionData = {
            tag_id : currentTag.hashId,
            name : newTag.name,
            description : newTag.description,
            color : newTag.color
        };

        let thisFlight = this;

        $.ajax({
            type: 'POST',
            url: '/protected/edit_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
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
            error : function(jqXHR, textStatus, errorThrown) {
            },
            async: true
        });
    }

    getUnassociatedTags(flightId) {
        console.log("getting unassociated tags!")

        let tags = [];

        var submissionData = {
            id : flightId
        };

        $.ajax({
            type: 'POST',
            url: '/protected/get_unassociated_tags',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                tags = response;
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: false 
        });  

        return tags;
    }

    /**
     * Handles when the user presses the delete button, and prompts them with @module confirmModal
     */
    deleteTag(flightId, tagId) {
        console.log(tag);
        if (tagId != null) {
            console.log("delete tag invoked!");
            confirmModal.show("Confirm Delete Tag: '" + tag.name + "'",
                "Are you sure you wish to delete this tag?\n\nThis operation will remove it from this flight as well as all other flights that this tag is associated with. This operation cannot be undone!",
                () => {this.removeTag(flightId, tagId, true)}
            );
        } else {
            errorModal.show("Please select a tag to delete first!",
                            "You did not select a tag to delete");
        }

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

        $.ajax({
            type: 'POST',
            url: '/protected/remove_tag',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                if (isPermanent) {
                    console.log("permanent deletion of tag with id: " + tagId);
                    for (var i = 0; i < thisFlight.state.flights.length; i++) {
                        let flight = thisFlight.state.flights[i];
                        console.log(flight);
                        if (flight.tags != null) {
                            let tags = flight.tags;
                            for (var j = 0; j < tags.length; j++) {
                                let tag = tags[j];
                                if (tagId == response.tagId) {
                                    tags.splice(j, 1);
                                }
                            }
                        }
                    } 
                } else if (response.allTagsCleared) {
                    for (var i = 0; i < thisFlight.state.flights.length; i++) {
                        let flight = thisFlight.state.flights[i];
                        if (flight.id == flightId) {
                            flight.tags = [];
                        }
                    } 
                } else {
                    for (var i = 0; i < thisFlight.state.flights.length; i++) {
                        let flight = thisFlight.state.flights[i];
                        let tags = flight.tags;
                        if (flight.id == flightId) {
                            let tags = flight.tags;
                            tags.splice(tags.indexOf(tag), 1);
                        }
                    }
                } 
                thisFlight.setState(thisFlight.state);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: false 
        });  
    }

    /**
     * Associates a tag with this flight
     * @param id the tag id to associate
     */
    associateTag(tagId, flightId) {
        console.log("associating tag #"+tagId+" with flight #"+flightId);

        var submissionData = {
            id : flightId,
            tag_id : tagId
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
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }


    /**
     * Handles when the user presses the clear all tags button, and prompts them with @module confirmModal
     */
    clearTags(flightId) {
        confirmModal.show("Confirm action", "Are you sure you would like to remove all the tags from flight #" + flightId + "?",
                          () => {this.removeTag(flightId, -2, false)});
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
                        setAvailableLayers={(plotLayers) => {this.setAvailableLayers(plotLayers);}}
                        setFlights={(flights) => {
                            this.setState({
                                flights : flights
                            });
                        }}

                        addTag={(flightId, name, description, color) => this.addTag(flightId, name, description, color)}
                        removeTag={(flightId, tagId, perm) => this.removeTag(flightId, tagId, perm)}
                        deleteTag={(flightId, tagId) => this.deleteTag(flightId, tagId)}
                        getUnassociatedTags={(flightId) => this.getUnassociatedTags(flightId)}
                        associateTag={(tagId, flightId) => this.associateTag(tagId, flightId)}
                        clearTags={(flightId) => this.clearTags(flightId)}
                        editTag={(currentTag, newTag) => this.editTag(currentTag, newTag)}
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

        console.log("in hover!");
        console.log(data);
        let x = data.points[0].x;
        //let x = data.points[0].pointIndex;

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
