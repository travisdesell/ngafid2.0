import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import {fromLonLat, toLonLat} from 'ol/proj.js';
import { map, styles, layers, Colors } from "./map.js";

class Itinerary extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            color : this.props.color
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

    selectLayer(selectedLayer) {
        console.log("setting selected layer");
        for (let i = 0; i < this.props.layers.length; i++) {
            let layer = this.props.layers[i];
            layer.setVisible(layer.get('name').includes(selectedLayer));
            console.log("setting layer: ");
            console.log(layer);
        }
        this.setState(this.state);
    }

    setDefaultLayer() {
        let defaultLayerName = 'Itinerary'; //changeme if we want the default layer to be something different

        this.selectLayer(defaultLayerName);
    }

    getSelectedLayer() {
        for (let i = 0; i < this.props.layers.length; i++) {
            let layer = this.props.layers[i];
            console.log(layer);
            if (layer.get('visible') && !layer.get('nMap')) {
                return layer.get('description');
            }
        }

        this.setDefaultLayer();
        return 'Itinerary with Phases';
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
            <div className="m-1">
                <b className={"m-0 p-1"} style={{marginBottom:"0", overflowY:"auto"}}>Itinerary:</b>
                <div className={cellClasses} style={cellStyle}>
                    <div style={{flex: "0 0"}}>
                        <input type="color" name="itineraryColor" value={this.state.color} onChange={(event) => {this.changeColor(event); this.props.flightColorChange(this.props.parent, event)}} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                    </div>

                    <button className="m-1 btn btn-outline-dark dropdown-toggle" style={styleButton} type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        <i className="fa fa-map-o p-1"></i>
                        {this.getSelectedLayer()}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                    {
                        this.props.layers.map((layer, index) => {
                            if (!layer.get('nMap')) {
                                return (
                                    <button className="dropdown-item" type="button" key={index} onClick={() => this.selectLayer(layer.get('name'))} disabled={layer.get('disabled')}>{layer.get('description')}</button>
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
