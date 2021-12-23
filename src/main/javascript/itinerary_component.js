import 'bootstrap';
import 'bootstrap-slider';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import {fromLonLat, toLonLat} from 'ol/proj.js';
import { map, styles, layers, Colors } from "./map.js";
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import Geometry from 'ol/geom/Geometry';

class Itinerary extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            color : this.props.color,
            sliderData : {}
        }
    }

    itineraryClicked(index) {
        this.props.showMap();

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

    changeItineraryRange(el) {
        let min = el.value[0];
        let max = el.value[1];

        //console.log("Trimming flight to timestamp " + min + " through " + max);

        this.props.redrawFlightPath(min, max);
    }

    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn expand-import-button btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        const styleDropdown = {
            top : "38px"
        };

        const sliderStyle = {
            display : 'auto',
            width : '1200px'
        }

        /*
        let cellClasses = "p-1 card mr-1 flex-fill"
        let itinerary = this.props.itinerary;
        let result = "";
        for (let i = 0; i < itinerary.length; i++) {
            result += itinerary[i].airport + " (" + itinerary[i].runway + ")";
            if (i != (itinerary.length - 1)) result += " => ";
        }
        */

        let eventHighlights = [];
        for (var i = 0; i < this.props.events.length; i++) {
            let event = this.props.events[i];
            // eventHighlights.push({"start" : event.startLine, "end" : event.endLine, style: "background: #99CC00;"})
        }

        console.log("event highlights:");
        console.log(eventHighlights);

        //this.props.events.map((event, index) => {
            //if (!layer.get('nMap')) {
                //return (
                    //<div className="slider-rangeHighlight slider-selection"  key={index} style={{background : "#ff00ff"}}/>
                //);
            //}
                        //})

        $('#slider').slider({
            id: 'slider',
            min: 0,
            max: this.props.numberRows,
            step: 1,
            range: true,
            value: [0, this.props.numberRows],
        });

        

        const el = $('#slider').on('slide', (el) => this.changeItineraryRange(el)).data('slider');
        console.log(this.props.layers);

        return (
            <div>
                <b className={"m-0 p-1"} style={{marginBottom:"0", overflowY:"auto"}}>Itinerary:</b>
                <div className="row">
                    <div className="col mb-1 ml-3 mr-5">
                        <input id="slider" type="range" onMouseUp={() => this.changeItineraryRange()} style={{width : "100%"}}/>
                    </div>
                </div>
                <div className={cellClasses} style={cellStyle}>
                    <div style={{flex: "0 0"}}>
                        <input type="color" name="itineraryColor" value={this.state.color} onChange={(event) => {this.changeColor(event); this.props.flightColorChange(this.props.parent, event)}} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                    </div>

                    <button className="m-1 btn btn-outline-dark dropdown-toggle" style={styleButton} type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <i className="fa fa-map-o p-1"></i>
                        {this.props.getSelectedLayer()}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                    {
                        this.props.layers.map((layer, index) => {
                            if (!layer.get('nMap')) {
                                return (
                                    <button className="dropdown-item" type="button" key={index} onClick={() => this.props.selectLayer(layer.get('name'))} disabled={layer.get('disabled')}>{layer.get('description')}</button>
                                );
                            }
                        })
                    }
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
            </div>
        );

    }
}

export { Itinerary };
