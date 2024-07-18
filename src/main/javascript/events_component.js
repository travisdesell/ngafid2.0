import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

import Plotly from 'plotly.js';
import { map } from "./map.js";
import {Circle, Fill, Icon, Stroke, Style} from 'ol/style.js';
import GetDescription from "./get_description";
import {errorModal} from "./error_modal";
// establish set of RGB values to combine //
let BG_values = ["00", "55", "AA", "FF"];
let R_values = ["FF", "D6", "AB", "80"];                            // heavier on the red for "warmer" colors


// populate hashmap of event definition IDs to RGB values
var eventColorScheme = {};
const LOWEST_EVENT_ID = -7;
const HIGHEST_EVENT_ID = 70;
const ABS_EVENT_ID = Math.abs(LOWEST_EVENT_ID);

for (let d = LOWEST_EVENT_ID; d < HIGHEST_EVENT_ID; d++) {
    // iterate through RGB permutations (up to 64)
    let green = (d + ABS_EVENT_ID) % 4;
    let blue = Math.trunc((d + ABS_EVENT_ID) / 4) % 4;
    let red = Math.trunc((d + ABS_EVENT_ID) / 16) % 4;

    eventColorScheme[(d + 1)] = "#" + R_values[red] + BG_values[green] + BG_values[blue];
}


//save the event definitions after the first event load so we can reuse them and not
//have to keep sending them from the server
global.eventDefinitionsLoaded = false;
global.eventDefinitions = null;

class Events extends React.Component {
    constructor(props) {
        super(props);

        console.log("constructing Events, props.events:");
        console.log(props.events);

        let definitionsPresent = [];

        for (let i = 0; i < props.events.length; i++) {
            if (!definitionsPresent.includes(props.events[i].eventDefinition)) {
                definitionsPresent.push(props.events[i].eventDefinition);
            }

            // assign color scheme to events, based on definition ID
            props.events[i].color = eventColorScheme[props.events[i].eventDefinitionId];
        }

        this.state = {
            events : props.events,
            definitions : definitionsPresent
        };
    }

    async getUTCOffset(id){
        if(id != null){

            var submissionData = {
                flightId : id,
                seriesName : "UTCOfst"
            };   
    
            var res = $.ajax({
                async: false, 
                type: 'POST',
                url: '/protected/string_series',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("OFFSET IN FUNCTION (hopefully)");
                    console.log(response);
                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Flight Coordinates", errorThrown);
                },   
                
            });

            console.log("Res.responseText: ");
            console.log(res.responseText);
            return res.responseText;

        } else{
            return 0;
        }
    }

    convertUTC(time,offset){
        var newTime = time;
        var hour = Number(time.substring(time.length - 8, time.length - 6));
        hour -= offset;
        newTime =  time.substring(0, time.length - 8) + hour.toString() + time.substring(time.length - 6);
        console.log(newTime);
        return newTime;

        
    }

    offsetTime(time,amount){
        var dateTimeCombo;
        var parts = time.split(" ");
        var date = parts[0];
        var time = parts[1];
        time = time.split(":");
        var hour = Number(time[0]);
        var minute = Number(time[1]);
        var second = Number(time[2]);
        date = date.split("-");
        var year = Number(date[0]);
        var month = Number(date[1]);
        var day = Number(date[2]);

        second += amount;
        if(second >= 60){
            minute += 1;
            second = 0;
        } else if(second < 0){
            minute -= 1;
            second = 59;
        }

        if(minute >= 60){
            hour += 1;
            minute = 0;
        } else if(minute < 0){
            hour -= 1;
            minute = 59;
        }

        if(hour >= 24){
            day += 1;
            hour = 0;
        } else if(hour < 0){
            day -= 1;
            hour = 23;
        }



        if(hour < 10){
            time = "0" + hour + ":";
        } else{
            time = hour + ":";
        }

        if(minute < 10){
            time += "0" + minute + ":";
        } else{
            time += minute + ":";
        }

        if(second < 10){
            time += "0" + second;
        } else{
            time += second;
        }

        date = year + "-";
        if(month < 10){
            date += "0" + month + "-";
        } else{
            date += month + "-";
        }

        if(day < 10){
            date += "0" + day;
        } else{
            date += day;
        }


        dateTimeCombo = date + " " + time;
        return dateTimeCombo;

    }

    async updateEventDisplay(index, toggle) {
            // Draw rectangles on plot
        var event = this.state.events[index];
        console.log("EVENT DETAILS: ");
        console.log(event);
        
        console.log("Adjusting Times to be -/+ 1");
        var tempStart = this.offsetTime(event.startTime,-1);
        var tempEnd = this.offsetTime(event.endTime,1);
        console.log(tempStart);
        console.log(tempEnd);

        var res = await this.getUTCOffset(event.flightId);
        res = res.split(" ");
        var offset = res[res.length-1];
        offset = offset.substring(offset.length-9,offset.length-6);

        var startTime = await this.convertUTC(tempStart, offset);
        var endTime = await this.convertUTC(tempEnd, offset);

        

        console.log("drawing plotly rectangle from " + startTime + " to " + endTime);
        let shapes = global.plotlyLayout.shapes;
        //console.log("Type of event.startTime: ");
        //console.log(typeof(event.startTime));

        let update = {
            id: event.id,
            type: 'rect',
            // x-reference is assigned to the x-values
            xref: 'x',
            // y-reference is assigned to the plot paper [0,1]
            yref: 'paper',
            x0: startTime,
            y0: 0,
            x1: endTime,
            y1: 1,
            fillcolor: event.color,
            'opacity': 0.5,
            line: {
                'width': 0,
            }
        };

        let found = false;
        for (let i = 0; i < shapes.length; i++) {
            if (shapes[i].id == event.id) {
                if (toggle) {
                    shapes.splice(i, 1);
                    found = true;
                } else {
                    shapes[i] = update;
                    found = true;
                    break;
                }
            }
        }

        if (!found && toggle) {
            shapes.push(update);
        }

        Plotly.relayout('plot', global.plotlyLayout);


        // Toggle visibility of clicked event's Feature //

        // create eventStyle & hiddenStyle
        var eventStyle = new Style({                                                   // create style getter methods**
            stroke: new Stroke({
                color: event.color,
                width: 7
            })
        });

        var outlineStyle = new Style({                                                   // create style getter methods**
            stroke: new Stroke({
                color: "#000000",
                width: 8
            })
        });

        var hiddenStyle = new Style({
            stroke: new Stroke({
                color: [0,0,0,0],
                width: 3
            })
        });

        // get event info from flight
        let flight = this.props.parent;
        let eventMapped = flight.state.eventsMapped[index];
        let pathVisible = flight.state.pathVisible;
        let eventPoints = flight.state.eventPoints;
        let eventOutline = flight.state.eventOutlines[index];
        event = eventPoints[index];                                 //override event var w/ event Feature

        //toggle eventLayer style
        if (!eventMapped) {                             // if event hidden
            event.setStyle(eventStyle);
            eventOutline.setStyle(outlineStyle);
            flight.state.eventsMapped[index] = !eventMapped;

            // center map view on event location
            let coords = event.getGeometry().getFirstCoordinate();
            if (coords.length > 0 && pathVisible) {
                map.getView().setCenter(coords);
            }

        } else {                                        // if event displayed
            event.setStyle(hiddenStyle);
            eventOutline.setStyle(hiddenStyle);
            flight.state.eventsMapped[index] = !eventMapped;
        }
    }

    changeColor(e, index) {
        this.state.events[index].color = e.target.value;
        this.setState({
            events : this.state.events
        });
        this.updateEventDisplay(index, false);
    }
    

    eventClicked(index) {
        this.updateEventDisplay(index, true);
    }

    getEventMetaData(eventId) {

        var eventMetaData = null;
        var submissionData = {
            eventId : eventId
        };
        $.ajax({
            type: 'POST',
            url: '/protected/event_metadata',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                eventMetaData =  response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Event Metadata ", errorThrown);
            },
            async: false
        })
        console.log("Event MetaData : ");
        console.log(eventMetaData);

        return eventMetaData;

    }

   
    render() {
        let cellClasses = "d-flex flex-row p-1";
        let cellStyle = { "overflowX" : "auto" };
        let buttonClasses = "m-1 btn btn-outline-secondary";
        const styleButton = {
            flex : "0 0 10em"
        };

        let eventType = "type";

        let eventTypeSet = new Set();
        let eventTypeButtons = [];
        let thisFlight = this.props.parent;

        this.state.events.map((event, index) => {
            if (!eventTypeSet.has(event.eventDefinitionId)) {
                // add new eventDef to types set
                eventTypeSet.add(event.eventDefinitionId);

                // create new button for toggle
                let type =
                        (
                            <button className={buttonClasses} style={{flex : "0 0 10em", "backgroundColor": eventColorScheme[event.eventDefinitionId], "color" : "#000000"}} data-toggle="button" aria-pressed="false" key={index}
                                        onClick={() =>
                                            {
                                                let flight = this.props.parent;
                                                let eventsMapped = flight.state.eventsMapped;
                                                let displayStatus = false;
                                                let displayStatusSet = false;

                                                // update eventDisplay for every event concerned
                                                for (let e = 0; e < this.state.events.length; e++) {
                                                    if (this.state.events[e].eventDefinitionId == event.eventDefinitionId) {
                                                        // ensure unified display
                                                        if (!displayStatusSet) {
                                                            displayStatus = !eventsMapped[e];
                                                            displayStatusSet = true;
                                                        }
                                                        // eventsMapped[e] = displayStatus;
                                                        // this.updateEventDisplay(e);

                                                        if (eventsMapped[e] != displayStatus) {
                                                            document.getElementById("_" + flight.props.flightInfo.id + e).click();
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    title={GetDescription(event.eventDefinition.name)}>

                                <b>{event.eventDefinition.name}</b>
                            </button>
                        );
                eventTypeButtons.push(type);

                console.log(eventColorScheme);
            }
        })

        return (
            <div>
                <b className={"p-1"} style={{marginBottom:"0"}}>Events:</b>

                <div className={"eventTypes"}>
                    {
                        eventTypeButtons.map( (button) => {
                            return (
                                button
                            )
                        })
                    }
                </div>

                {
                    this.state.events.map((event, index) => {
                        let buttonID = "_" + this.props.parent.props.flightInfo.id + index;
                        let otherFlightText = "";
                        let otherFlightURL = "";
                        let rateOfClosureBtn = "";
                        let rocPlot = "";
                        let eventMetaDataText = "";
                        var eventMetaData = this.getEventMetaData(event.id);

                        if (event.eventDefinitionId == -1) {
                            var rocPlotData = this.getRateOfClosureData(event);
                            
                            otherFlightText = ", other flight id: ";
                            otherFlightURL = ( <a href={"./flight?flight_id=" + event.flightId + "&flight_id=" + event.otherFlightId}> {event.otherFlightId} </a> );

                            if (rocPlotData != null) {
                                rateOfClosureBtn = ( <button id="rocButton" data-toggle="button" className={buttonClasses} onClick={() => this.displayRateOfClosurePlot(rocPlotData, event)}>
                                    <i className="fa fa-area-chart p-1" ></i></button>   );
                                if (!event.rocPlotVisible) {
                                    rocPlot = (<div id={event.id + "-rocPlot"}></div>);
                                }
                            }

                        }

                        if (eventMetaData != null) {
                            eventMetaDataText = " , ";
                            eventMetaData.map((item) => {
                                eventMetaDataText += item.name + ": " +  (Math.round(item.value * 100) / 100).toFixed(2) + ", ";
                            })
                            eventMetaDataText = eventMetaDataText.substring(0, eventMetaDataText.length - 2);
                        } 

                        return (
                            <div className={cellClasses} style={cellStyle} key={index}>
                                <div style={{flex: "0 0"}}>
                                    <input type="color" name="eventColor" value={event.color} onChange={(e) => {this.changeColor(e, index); }} style={{padding:"3 2 3 2", border:"1", margin:"5 4 4 0", height:"36px", width:"36px"}}/>
                                </div>

                                    <button id={buttonID} className={buttonClasses} style={styleButton} data-toggle="button" aria-pressed="false" onClick={() => this.eventClicked(index)}>
                                        <b>{event.eventDefinition.name}</b> {" -- " + event.startTime + " to " + event.endTime + ", severity: " + (Math.round(event.severity * 100) / 100).toFixed(2)} {eventMetaDataText} { otherFlightText } { otherFlightURL } { rateOfClosureBtn }
                                        {rocPlot}
                                    </button>
                            </div>

                        );
                    })
                }

            </div>
        );

    }

    displayRateOfClosurePlot(data, event) {
        var id = event.id + "-rocPlot";
        if (!event.rocPlotVisible) {
            var trace = {
                x : data.x,
                y : data.y,
                type : "scatter",
            }
            var layout = {
                shapes: [
                    {
                        type: 'rect',
                        x0: 0,
                        y0: Math.min(...data.y),
                        x1: data.x.length - 10,
                        y1: Math.max(...data.y),
                        fillcolor: '#d3d3d3',
                        opacity: 0.3,
                        line: {
                            width: 0
                        }
                    }],
                yaxis : {
                    title:{
                        text: "Rate of closure (distance in ft)"
                    }
                },
                xaxis : {
                    title : {
                        text : "Proximity Event (seconds)"
                    }
                }
            }
            Plotly.newPlot(id, [trace], layout)
            event.rocPlotVisible = true
            $("#"+id).show();
        } else {
            event.rocPlotVisible = false;
            $("#"+id).hide();
        }
    };


    getRateOfClosureData(event) {
        var eventId = event.id;
        var rocPlotData = null;
        console.log("Calculating Rate of Closure")
        var submissionData = {
            eventId : eventId
        };
        $.ajax({
            type: 'POST',
            url: '/protected/rate_of_closure',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                rocPlotData =  response;
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Rate of closure ", errorThrown);
            },
            async: false
        })
        return rocPlotData;
    };

}

export { Events, eventColorScheme };
