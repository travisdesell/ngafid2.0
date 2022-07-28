import React from "react";


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

    }

    componentDidMount() {
        updateCesiumButtonState();
    }

    render() {
        return (
            <div className="col form-row input-group m-0 p-0">
                <div className="input-group-prepend p-0">
                    <button id="cesiumViewButton" className="btn btn-sm btn-primary"
                            onClick={() => this.viewCesiumFlights()}>
                        View Selected Replays
                    </button>

                    <button id="cesiumClearButton" className="btn btn-sm btn-primary"
                            onClick={() => this.clearCesiumFlights()}>
                        Clear Selected Replays
                    </button>
                </div>
            </div>

        )
    }


}

export let cesiumFlightsSelected = [];

export function updateCesiumButtonState() {
    let cesiumButtonsDisabled = cesiumFlightsSelected.length <= 0;

    let viewButton = document.getElementById("cesiumViewButton");
    viewButton.disabled = cesiumButtonsDisabled;

    let clearButton = document.getElementById("cesiumClearButton")
    clearButton.disabled = cesiumButtonsDisabled;
}


export {CesiumButtons}

