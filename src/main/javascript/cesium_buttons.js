import React, { useState } from "react";


class CesiumButtons extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            disabled: cesiumFlightsSelected.length <= 0
        };


    }

    componentDidMount() {

        let cesiumButtonsDisabled = cesiumFlightsSelected.length <= 0;
        let viewButton = document.getElementById("cesiumViewButton");
        let clearButton = document.getElementById("cesiumClearButton");

        viewButton.disabled = cesiumButtonsDisabled;
        clearButton.disabled = cesiumButtonsDisabled;

        updateCesiumURL();
        
    }


    /**
     * View flights selected through the globe button
     */
    viewCesiumFlights() {
        let URL = "/protected/ngafid_cesium?flight_id=";

        if (cesiumFlightsSelected.length > 0) {
            URL += cesiumFlightsSelected.join("&flight_id=");
        } else {
            return;
        }

        window.open(URL);
    }

    createButtonStyle() {
        console.log("Creating style")
        return {
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 12,
            paddingHorizontal: 32,
            borderRadius: 4,
            elevation: 3,
            opacity_disabled: 0,
        }
    }

    /**
     * Handles clearing all selected flights for multiple flight replays
     */
    clearCesiumFlights() {

        console.log("Clearing selected flights: ", cesiumFlightsSelected);

        let cesiumFlightsSelectedCopy = [...cesiumFlightsSelected];
        for (let flightId of cesiumFlightsSelectedCopy) {

            let toggleButton = document.getElementById("cesiumToggled" + flightId);
            if (toggleButton)
                toggleButton.click();
            else
                console.warn("Toggle button for flight " + flightId + " not found.");
            
        }

        cesiumFlightsSelected = [];
        updateCesiumURL();
        updateCesiumButtonState();

        //Clear the URL hash
        if (window.location.hash.startsWith("#/replays"))
            window.location.hash = "";

    }

    render() {

        return (
            <div className="col form-row input-group m-0 p-0" style={{
                position: "center", display: 'flex',
                justifyContent: 'center',
                alignItems: 'flex-start',
                margin: "0"
            }}>
                <div className="input-group-prepend p-0">
                    <button id={"cesiumViewButton"} className="btn btn-sm btn-primary"
                            onClick={() => this.viewCesiumFlights()} style={this.createButtonStyle("blue")}>
                        View Selected Replays
                    </button>

                    &nbsp;

                    <button id={"cesiumClearButton"} className="btn btn-sm btn-primary"
                            onClick={() => this.clearCesiumFlights()} style={this.createButtonStyle("red")}>
                        Clear Selected Replays
                    </button>

                </div>
            </div>

        )
    }


}

export let cesiumFlightsSelected = [];

export function toggleCesiumFlightSelected(flightId) {

    let index = cesiumFlightsSelected.indexOf(flightId);

    //Flight is already selected, remove it
    if (index > -1)
        cesiumFlightsSelected.splice(index, 1);

    //Otherwise, add it
    else
        cesiumFlightsSelected.push(flightId);

    updateCesiumURL();
    updateCesiumButtonState();

}

export function cesiumFlightIsSelected(flightId) {

    return cesiumFlightsSelected.includes(flightId);

}

function updateCesiumURL() {

    //No flights selected, remove #/replays... from the URL
    if (cesiumFlightsSelected.length <= 0) {

        if (window.location.hash.startsWith("#/replays"))
            window.location.hash = "";

        return;
    }

    //Modify URL hash to display all of the selected flights
    let hash = window.location.hash;
    if (hash.startsWith("#/replays")) {
        let newHash = "#/replays?flight_id=" + cesiumFlightsSelected.join("&flight_id=");
        window.location.hash = newHash;
    }
    else {
        window.location.hash = "#/replays?flight_id=" + cesiumFlightsSelected.join("&flight_id=");
    }

}

export function updateCesiumButtonState() {
    
    let cesiumButtonsDisabled = cesiumFlightsSelected.length <= 0;

    let clearButton = document.getElementById("cesiumClearButton");
    let viewButton = document.getElementById("cesiumViewButton");

    if (!clearButton || !viewButton) {
        console.warn("Cesium buttons not found in the DOM. Ensure they are rendered before calling this function.");
        return;
    }

    clearButton.disabled = cesiumButtonsDisabled;
    viewButton.disabled = cesiumButtonsDisabled;
    
}


export {CesiumButtons}

