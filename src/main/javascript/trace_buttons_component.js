import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Plotly from 'plotly.js';
import { errorModal } from "./error_modal.js";

class TraceButtons extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            parentFlight : this.props.parentFlight
        };
    }

    traceClicked(seriesName) {
        this.props.showPlot();

        let parentFlight = this.state.parentFlight;

        //check to see if we've already loaded this time series
        if (!(seriesName in parentFlight.state.traceIndex)) {
            var thisTrace = this;

            console.log(seriesName);
            console.log("seriesName: " + seriesName + ", flightId: " + this.props.flightId);

            var submissionData = {
                flightId : this.props.flightId,
                seriesName : seriesName
            };   

            $.ajax({
                type: 'POST',
                url: '/protected/double_series',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    var trace = {
                        x : response.x,
                        y : response.y,
                        mode : "lines",
                        //marker : { size: 1},
                        name : thisTrace.props.flightId + " - " + seriesName
                    }

                    //set the trace number for this series
                    parentFlight.state.traceIndex[seriesName] = $("#plot")[0].data.length;
                    parentFlight.state.traceVisibility[seriesName] = true;
                    parentFlight.setState(parentFlight.state);

                    Plotly.addTraces('plot', [trace]);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
                },   
                async: true 
            });  
        } else {
            //toggle visibility for this series
            let visibility = !parentFlight.state.traceVisibility[seriesName];
            parentFlight.state.traceVisibility[seriesName] = visibility;
            parentFlight.setState(parentFlight.state);

            console.log("toggled visibility to: " + visibility);

            Plotly.restyle('plot', { visible: visibility }, [ parentFlight.state.traceIndex[seriesName] ])
        }
    }

    render() {
        let cellClasses = "d-flex flex-row pb-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let parentFlight = this.state.parentFlight;

        return (
            
            <div className="w-100">

                <b className={"p-1 d-flex flex-row justify-content-start align-items-center"} style={{marginBottom:"0"}}>
                    <div className="d-flex flex-column mr-3" style={{width: "16px", minWidth:"16px", maxWidth:"16px", height: "16px"}}>
                        <i className='fa fa-area-chart ml-2' style={{fontSize: "12px", marginTop: "3px", opacity: "0.50"}}/>
                    </div>
                    <div style={{fontSize: "0.75em"}}>
                        Parameters
                    </div>
                </b>

                <div className="p-1" style={{overflowX:"auto"}}>

                    <div className={cellClasses} style={cellStyle}>
                        {
                            parentFlight.state.commonTraceNames.map((traceName, index) => {
                                let ariaPressed = parentFlight.state.traceVisibility[traceName];
                                let active = "";
                                if (ariaPressed) active = " active";

                                return (
                                    <button className={buttonClasses + active} key={traceName} style={styleButton} data-bs-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
                                        {traceName}
                                    </button>
                                );
                            })
                        }
                    </div>
                    <div className={cellClasses} style={cellStyle}>
                        {
                            parentFlight.state.uncommonTraceNames.map((traceName, index) => {
                                let ariaPressed = parentFlight.state.traceVisibility[traceName];
                                let active = "";
                                if (ariaPressed) active = " active";

                                return (
                                    <button className={buttonClasses + active} key={traceName} style={styleButton} data-bs-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
                                        {traceName}
                                    </button>
                                );
                            })
                        }
                    </div>

                </div>
            </div>
        );
    }
}

export { TraceButtons };
