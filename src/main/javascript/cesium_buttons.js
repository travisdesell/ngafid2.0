import React from "react";


class CesiumButtons extends React.Component {
    constructor(props) {
        super(props);


    }

    render() {
        return (
            <div>
                <button className="btn btn-sm btn-primary" onClick={() => this.viewCesiumFlights()}>
                    View Selected Replays
                </button>
                <button className="btn btn-sm btn-primary" onClick={() => this.clearCesiumFlights()}>
                    Clear Selected Replays
                </button>
            </div>
        )
    }

}

export { CesiumButtons }