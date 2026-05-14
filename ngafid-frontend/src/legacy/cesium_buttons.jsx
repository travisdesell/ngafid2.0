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

    createButtonStyle() {
        console.log("Creating style");
        return {
            alignItems: 'center',
            justifyContent: 'center',
            paddingVertical: 12,
            paddingHorizontal: 32,
            borderRadius: 4,
            elevation: 3,
            opacity_disabled: 0,
        };
    }

    /**
     * Handles clearing all selected flights for multiple flight replays
     */
    clearCesiumFlights() {
        cesiumFlightsSelected.forEach((removedFlight) => {
            console.log(`Removed ${  removedFlight}`);
            const toggleButton = document.getElementById(`cesiumToggled${  removedFlight}`);
            toggleButton.click();
        });

        if (cesiumFlightsSelected.length > 0) {
            this.clearCesiumFlights();
        }

    }

    componentDidMount() {
        const cesiumButtonsDisabled = cesiumFlightsSelected.length <= 0;
        const viewButton = document.getElementById(`cesiumViewButton${  this.props.location}`);
        const clearButton = document.getElementById(`cesiumClearButton${  this.props.location}`);

        viewButton.disabled = cesiumButtonsDisabled;
        clearButton.disabled = cesiumButtonsDisabled;
    }

    render() {
        return (
            <div className="col form-row input-group m-0 p-0" style={{
                position: "center", display: 'flex',
                justifyContent: 'center',
                alignItems: 'flex-start',
                margin: "0",
            }}>
                <div className="input-group-prepend p-0">
                    <button id={`cesiumViewButton${  this.props.location}`} className="btn btn-sm btn-primary d-none"
                            onClick={() => this.viewCesiumFlights()} style={this.createButtonStyle("blue")}>
                        View Selected Replays
                    </button>

                    &nbsp;

                    <button id={`cesiumClearButton${  this.props.location}`} className="btn btn-sm btn-primary d-none"
                            onClick={() => this.clearCesiumFlights()} style={this.createButtonStyle("red")}>
                        <i className="fa fa-globe mr-2"/>
                        Clear Selected Replays
                    </button>
                </div>
            </div>

        );
    }


}

export const cesiumFlightsSelected = [];

export function updateCesiumButtonState() {
    const cesiumButtonsDisabled = cesiumFlightsSelected.length <= 0;

    const viewButtonTop = document.getElementById("cesiumViewButtonTop");
    const clearButtonTop = document.getElementById("cesiumClearButtonTop");
    viewButtonTop.disabled = cesiumButtonsDisabled;
    clearButtonTop.disabled = cesiumButtonsDisabled;


    const clearButtonBot = document.getElementById("cesiumClearButtonBottom");
    const viewButtonBot = document.getElementById("cesiumViewButtonBottom");
    if (clearButtonBot !== null || viewButtonBot !== null) {
        viewButtonBot.disabled = cesiumButtonsDisabled;
        clearButtonBot.disabled = cesiumButtonsDisabled;
    }
}

export function cesiumJumpToFlightStart(flightId) {

    /*
        Set the Cesium Viewer playhead position
        to the start of the selected flight
    */

    const flight = cesiumFlightsSelected[flightId];
    const startTime = flight.start_time;
    const endTime = flight.end_time;

    const viewer = this.props.viewer;
    const clock = viewer.clock;

    //Set the clock to the start time of the flight
    clock.currentTime = Cesium.JulianDate.fromDate(new Date(startTime));
    clock.startTime = Cesium.JulianDate.fromDate(new Date(startTime));
    clock.stopTime = Cesium.JulianDate.fromDate(new Date(endTime));
    clock.currentTime = Cesium.JulianDate.fromDate(new Date(startTime));
    clock.multiplier = 1;
    clock.clockRange = Cesium.ClockRange.LOOP_STOP;
    clock.shouldAnimate = true;


    viewer.timeline.zoomTo(startTime, endTime);

}

export {CesiumButtons};

