import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

//import Dropdown from 'react-bootstrap/Dropdown';
//import DropdownButton from 'react-bootstrap/DropdownButton';
//import {Alert, Button, InputGroup, Form, Col} from 'react-bootstrap';

import { errorModal } from "./error_modal.jsx";
import SignedInNavbar from "./signed_in_navbar.jsx";
import { map, styles, layers, Colors } from "./map.jsx";
import { View } from 'ol'

import {fromLonLat, toLonLat} from 'ol/proj.jsx';
import {Group, Vector as VectorLayer} from 'ol/layer.jsx';
import {Vector as VectorSource} from 'ol/source.jsx';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.jsx';
//import Draw from 'ol/interaction/Draw.jsx';

import { Filter } from './filter.jsx';
import { Flight } from './flight_component.jsx';

import Plotly from 'plotly.jsx';

class FlightsCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            mapVisible : false,
            plotVisible : false,
            getFilterQuery : this.props.getFilterQuery
        };

    }

    mapSelectChanged(style) {
        //layers and styles from plots.jsx
        for (var i = 0, ii = layers.length; i < ii; ++i) {

            console.log("setting layer " + i + " to:" + (styles[i] === style));
            layers[i].setVisible(styles[i] === style);
        }   
    }

    setFlights(flights) {
        console.log("FlightsCard setting flights on parent!");
        this.props.parent.setState({
            flights : flights
        });
    }




    /**
     * Renders the flightsCard
     */
    render() {
        console.log("rendering flights!");

        let flights = this.props.flights;

        if (typeof flights != 'undefined') {
            return (
                <div>
                    {
                        flights.map((flightInfo, index) => {
                            if(flightInfo != null) {
                                return (
                                    <Flight showPlot={() => {this.props.showPlot();}} showMap={() => {this.props.showMap();}} flightInfo={flightInfo} navBar={this.props.navBar} pageIndex={index}
                                        updateParentState={(newFlights) => this.props.setFlights(newFlights)} setAvailableLayers={(plotLayers) => this.props.setAvailableLayers(plotLayers)}
                                        parent={this} layers={this.props.layers} key={flightInfo.id}
                                        addTag={this.props.addTag}
                                        removeTag={this.props.removeTag}
                                        deleteTag={this.props.deleteTag}
                                        getUnassociatedTags={this.props.getUnassociatedTags}
                                        associateTag={this.props.associateTag}
                                        clearTags={this.props.clearTags}
                                        editTag={this.props.editTag}
                                    />
                                );
                            }
                        })
                    }
                </div>
            );

        } else {
            return (
                <div/>
            );
        }
    }
}


export { FlightsCard };
