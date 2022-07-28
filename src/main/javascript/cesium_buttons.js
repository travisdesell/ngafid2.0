import React from "react";
import {cesiumFlightsSelected} from "./flight_component";


class CesiumButtons extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            disabled: cesiumFlightsSelected.length <= 0
        };


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

    /**
     * Handles clearing all selected flights for multiple flight replays
     */
    clearCesiumFlights() {
        cesiumFlightsSelected.forEach((removedFlight) => {
            console.log("Removed " + removedFlight);
            let toggleButton = document.getElementById("cesiumToggled" + removedFlight);
            toggleButton.click();
        });

        if (cesiumFlightsSelected.length > 0) {
            this.clearCesiumFlights();
        }

        this.setState({
            disabled: true
        })
    }

    updateState() {
        this.setState({
            disabled: cesiumFlightsSelected.length <= 0
        })
    }

    render() {
        console.log("Disabled state: " + this.state.disabled);
        console.log("Arr status: " + cesiumFlightsSelected.length + " " + cesiumFlightsSelected.length > 0);
        return (
            <div className="col form-row input-group m-0 p-0">
                <div className="input-group-prepend p-0">
                    <button className="btn btn-sm btn-primary" disabled={this.state.disabled}
                            onClick={() => this.viewCesiumFlights()}>
                        View Selected Replays
                    </button>

                    <button className="btn btn-sm btn-primary" disabled={this.state.disabled}
                            onClick={() => this.clearCesiumFlights()}>
                        Clear Selected Replays
                    </button>
                </div>
            </div>
        )
    }

}

export {CesiumButtons}