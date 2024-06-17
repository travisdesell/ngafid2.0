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

    getStartDate(){

        

        var submissionData2 = {
            flightId : this.props.flightId,
            seriesName : "Lcl Date"
        };

        $.ajax({
            type: 'POST',
            url: '/protected/string_series',
            data : submissionData2,
            dataType : 'json',
            success : function(response) {
                dates = response;
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Start Time", errorThrown);
            }
        });

        /*$.ajax({
            type: 'POST',
            url: '/protected/double_series',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response FOR TIME: ");
                console.log(response);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Start Time", errorThrown);
            }
        });*/



    }

    combineDateTime(datesObj,timesObj){
        var date_time_combo = [];
        var dates = Object.values(datesObj)[1];
        var times = Object.values(timesObj)[1];
        console.log("dates: ");
        console.log(dates);
        console.log("times: ");
        console.log(times);
        var size = Object.keys(dates).length;
        for (let i = 0; i < size; i++) {
                date_time_combo[i] = new Date(dates[i] +" "+ times[i]);
        }
        console.log("Timestamps: ");
        console.log(date_time_combo);

    }

    getFlightTimes(dates){
        var submissionData = {
            flightId : this.props.flightId,
            seriesName : "Lcl Time"
        };
        console.log("getting start date...");

        var self = this;
        $.ajax({
            type: 'POST',
            url: '/protected/string_series',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response FOR TIME: ");
                console.log(response);
                self.combineDateTime(dates,response);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Start Time", errorThrown);
                return(none);
            }
        });
    }

    getFlightDates(){
        var submissionData = {
            flightId : this.props.flightId,
            seriesName : "Lcl Date"
        };
        
        var self = this;
        $.ajax({
            type: 'POST',
            url: '/protected/string_series',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response FOR DATE: ");
                console.log(response);
                self.getFlightTimes(response);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Start Date", errorThrown);
                return(none);
            }
        });
    }

    traceClicked(seriesName) {
        this.props.showPlot();

        let parentFlight = this.state.parentFlight;

        //check to see if we've already loaded this time series
        if (!(seriesName in parentFlight.state.traceIndex)) {
            var thisTrace = this;

            console.log(seriesName);
            console.log("seriesName: " + seriesName + ", flightId: " + this.props.flightId);
            var date = this.getFlightDates();
            console.log("RETURNED DATE IS:");
            console.log(date);
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
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let parentFlight = this.state.parentFlight;

        return (
            <div>
                <b className={"p-1"} style={{marginBottom:"0"}}>Flight Parameters:</b>
                <div className={cellClasses} style={cellStyle}>
                    {
                        parentFlight.state.commonTraceNames.map((traceName, index) => {
                            let ariaPressed = parentFlight.state.traceVisibility[traceName];
                            let active = "";
                            if (ariaPressed) active = " active";

                            return (
                                <button className={buttonClasses + active} key={traceName} style={styleButton} data-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
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
                                <button className={buttonClasses + active} key={traceName} style={styleButton} data-toggle="button" aria-pressed={ariaPressed} onClick={() => this.traceClicked(traceName)}>
                                    {traceName}
                                </button>
                            );
                        })
                    }
                </div>
            </div>
        );
    }
}

export { TraceButtons };
