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
        let cesiumButtonsDisabled = cesiumFlightsSelected.length <= 0;
        let viewButton = document.getElementById("cesiumViewButton" + this.props.location);
        let clearButton = document.getElementById("cesiumClearButton" + this.props.location);

        viewButton.disabled = cesiumButtonsDisabled;
        clearButton.disabled = cesiumButtonsDisabled;
    }

    render() {
        return (
            <div className="col form-row input-group m-0 p-0">
                <div className="input-group-prepend p-0">
                    <button id={"cesiumViewButton" + this.props.location} className="btn btn-sm btn-primary"
                            onClick={() => this.viewCesiumFlights()}>
                        View Selected Replays
                    </button>

                    <button id={"cesiumClearButton" + this.props.location} className="btn btn-sm btn-primary"
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

    let viewButtonTop = document.getElementById("cesiumViewButtonTop");
    let clearButtonTop = document.getElementById("cesiumClearButtonTop");
    viewButtonTop.disabled = cesiumButtonsDisabled;
    clearButtonTop.disabled = cesiumButtonsDisabled;


    let clearButtonBot = document.getElementById("cesiumClearButtonBottom");
    let viewButtonBot = document.getElementById("cesiumViewButtonBottom");
    if (clearButtonBot != null || clearButtonBot !== null) {
        viewButtonBot.disabled = cesiumButtonsDisabled;
        clearButtonBot.disabled = cesiumButtonsDisabled;
    }
}


export {CesiumButtons}

