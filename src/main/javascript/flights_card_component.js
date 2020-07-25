import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

//import Dropdown from 'react-bootstrap/Dropdown';
//import DropdownButton from 'react-bootstrap/DropdownButton';
//import {Alert, Button, InputGroup, Form, Col} from 'react-bootstrap';

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";
import { map, styles, layers, Colors } from "./map.js";
import { View } from 'ol'

import {fromLonLat, toLonLat} from 'ol/proj.js';
import {Group, Vector as VectorLayer} from 'ol/layer.js';
import {Vector as VectorSource} from 'ol/source.js';
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
//import Draw from 'ol/interaction/Draw.js';

import { Filter } from './filter.js';
import { Flight } from './flight_component.js';

import Plotly from 'plotly.js';

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
        //layers and styles from plots.js
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


    submitFilter(pageIndex, numberPerPage) {
        console.log(this.state);
        let query = this.state.getFilterQuery();

        console.log("Submitting filters:");
        console.log( query );


        $("#loading").show();

        var submissionData = {
            filterQuery : JSON.stringify(query),
            pageIndex : pageIndex,
            numPerPage : numberPerPage
        };

        console.log(submissionData);

        let flightsCard = this;

        $.ajax({
            type: 'POST',
            url: '/protected/get_flights',
            data : submissionData,
            dataType : 'json',
            success : function(response) {

                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                console.log("got response: "+response+" "+response.size);

                //get page data
				if (response == "NO_RESULTS") {
					errorModal.show("No flights found with the given parameters!", "Please try a different query.");
 				} else {
                    flightsCard.props.setFlights(response.data);
                    flightsCard.props.updateNumberPages(response.sizeAll);
				}
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },   
            async: true 
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
                                    <Flight showPlot={() => {this.props.showPlot();}} showMap={() => {this.props.showMap();}} flightInfo={flightInfo} pageIndex={index}
                                        updateParentState={(newFlights) => this.props.setFlights(newFlights)}
                                        parent={this} tags={flightInfo.tags.value} key={flightInfo.id}/>
                                );
                            }
                        })
                    }
                </div>
            );

        } else {
            return (
                <div className="card-body">
                </div>
            );
        }
    }
}


export { FlightsCard };
