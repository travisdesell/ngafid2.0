import TrendsPage from "./trends";
import ReactDOM from "react-dom";
import React from "react";

var aggregateTrendsPage = ReactDOM.render(
    <TrendsPage
        aggregate_page ={true}
    />,
    document.querySelector('#trends-page')
);

aggregateTrendsPage.displayPlots("All Airframes");
// import 'bootstrap';
//
// import React, { Component } from "react";
// import ReactDOM from "react-dom";
//
// import { errorModal } from "./error_modal.js";
// import SignedInNavbar from "./signed_in_navbar.js";
//
// import TimeHeader from "./time_header.js";
// import GetDescription from "./get_description";
// // import TrendsPage from "./trends";
// import Plotly from 'plotly.js';
// import Tooltip from "react-bootstrap/Tooltip";
// import {OverlayTrigger} from "react-bootstrap";
//
// airframes.unshift("All Airframes");
// var index = airframes.indexOf("Garmin Flight Display");
// if (index !== -1) airframes.splice(index, 1);
//
// eventNames.sort();
//
// console.log(eventNames);
//
// /*
// var trace1 = {
//     name: 'test1',
//     x: [1, 2, 3, 4],
//     y: [10, 15, 13, 17],
//     type: 'scatter'
// };
//
// var trace2 = {
//     name: 'test2',
//     x: [1, 2, 3, 4],
//     y: [16, 5, 11, 9],
//     type: 'scatter'
// };
// */
//
// var countData = [];
// var percentData = [];
//
// var eventCounts = {};
//
// var eventFleetPercents = {};
// var eventNGAFIDPercents = {};
//
// class AggregateTrendsPage extends React.Component {
//     constructor(props) {
//         super(props);
//
//         let eventChecked = {};
//         for (let i = 0; i < eventNames.length; i++) {
//             eventChecked[eventNames[i]] = false;
//         }
//
//         var date = new Date();
//         this.state = {
//             airframe : "All Airframes",
//             startYear : 2020,
//             startMonth : 1,
//             endYear : date.getFullYear(),
//             endMonth : date.getMonth() + 1,
//             datesChanged : false,
//
//             eventChecked : eventChecked
//         };
//     }
//
//     exportCSV() {
//         let selectedAirframe = this.state.airframe;
//
//         console.log("selected airframe: '" + selectedAirframe + "'");
//
//         console.log(eventCounts);
//
//         let eventNames = [];
//         let airframeNames = [];
//         let dates = [];
//         let csvValues = {};
//
//
//         for (let [eventName, countsObject] of Object.entries(eventCounts)) {
//             //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
//             if (!this.state.eventChecked[eventName]) continue;
//
//             //make sure the eventNames array is unique names only
//             if (!eventNames.includes(eventName)) {
//                 eventNames.push(eventName);
//             }
//
//             for (let [airframe, value] of Object.entries(countsObject)) {
//                 if (value.airframeName === "Garmin Flight Display") continue;
//                 if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes") continue;
//
//                 let airframeName = value.airframeName;
//                 let valueDates = value.dates;
//
//
//                 //make sure the airframeNames array is unique names only
//                 if (!airframeNames.includes(airframeName)) {
//                     airframeNames.push(airframeName);
//                 }
//
//
//                 console.log(eventName + " - " + value.airframeName + " has dates: ");
//                 console.log(value.dates);
//
//                 for (let i = 0; i < value.dates.length; i++) {
//                     let date = value.dates[i];
//                     let eventCount = value.y[i];
//                     let flightsWithEventCount = value.flightsWithEventCounts[i];
//                     let totalFlights = value.totalFlightsCounts[i];
//
//
//                     //make sure the dates array is unique dates only
//                     if (!dates.includes(date)) {
//                         dates.push(date);
//                     }
//
//                     if (!(eventName in csvValues)) {
//                         csvValues[eventName] = {};
//                     }
//
//                     if (!(airframeName in csvValues[eventName])) {
//                         csvValues[eventName][airframeName] = {};
//                     }
//
//                     csvValues[eventName][airframeName][date] = {};
//                     csvValues[eventName][airframeName][date].eventCount = eventCount;
//                     csvValues[eventName][airframeName][date].flightsWithEventCount = flightsWithEventCount;
//                     csvValues[eventName][airframeName][date].totalFlights = totalFlights;
//                 }
//             }
//         }
//         eventNames.sort();
//         airframeNames.sort();
//         dates.sort();
//
//         console.log("eventNames:");
//         console.log(eventNames);
//         console.log("airframeNames:");
//         console.log(airframeNames);
//         console.log("dates:");
//         console.log(dates);
//
//         for (let eventName of eventNames) {
//             console.log(eventName + " has " + Object.keys(csvValues[eventName]).length + " entries!");
//             console.log(csvValues[eventName]);
//
//             for (let airframeName of airframeNames) {
//                 if (airframeName in csvValues[eventName]) {
//                     console.log("\t" + eventName + " - " + airframeName + " has " + Object.keys(csvValues[eventName][airframeName]).length + " entries!");
//                 }
//             }
//         }
//
//         let filetext = "";
//
//         let needsComma = false;
//         for (let eventName of eventNames) {
//             for (let airframeName of airframeNames) {
//                 if (airframeName in csvValues[eventName]) {
//                     if (needsComma) {
//                         filetext += ",";
//                     } else {
//                         needsComma = true;
//                     }
//
//                     filetext += eventName;
//                     filetext += "," + eventName;
//                     filetext += "," + eventName;
//                 }
//             }
//         }
//         filetext += "\n";
//
//         needsComma = false;
//         for (let eventName of eventNames) {
//             for (let airframeName of airframeNames) {
//                 if (airframeName in csvValues[eventName]) {
//                     if (needsComma) {
//                         filetext += ",";
//                     } else {
//                         needsComma = true;
//                     }
//                     filetext += airframeName;
//                     filetext += "," + airframeName;
//                     filetext += "," + airframeName;
//                 }
//             }
//         }
//         filetext += "\n";
//
//         needsComma = false;
//         for (let eventName of eventNames) {
//             for (let airframeName of airframeNames) {
//                 if (airframeName in csvValues[eventName]) {
//                     if (needsComma) {
//                         filetext += ",";
//                     } else {
//                         needsComma = true;
//                     }
//                     filetext += "Events";
//                     filetext += ",Flights With Event";
//                     filetext += ",Total Flights";
//                 }
//             }
//         }
//         filetext += "\n";
//
//         for (let i = 0; i < dates.length; i++) {
//             let date = dates[i];
//
//             needsComma = false;
//             for (let eventName of eventNames) {
//                 for (let airframeName of airframeNames) {
//                     if (airframeName in csvValues[eventName]) {
//                         if (needsComma) {
//                             filetext += ",";
//                         } else {
//                             needsComma = true;
//                         }
//
//                         if (date in csvValues[eventName][airframeName]) {
//                             filetext += csvValues[eventName][airframeName][date].eventCount;
//                             filetext += "," + csvValues[eventName][airframeName][date].flightsWithEventCount;
//                             filetext += "," + csvValues[eventName][airframeName][date].totalFlights;
//                         } else {
//                             filetext += ",,";
//                         }
//                     }
//                 }
//             }
//             filetext += "\n";
//         }
//
//         console.log("eventNames:");
//         console.log(eventNames);
//         console.log("airframeNames:");
//         console.log(airframeNames);
//         console.log("dates:");
//         console.log(dates);
//
//
//         let filename = "aggregate_trends.csv";
//
//         console.log("exporting csv!");
//
//         var element = document.createElement('a');
//         element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(filetext));
//         element.setAttribute('download', filename);
//
//         element.style.display = 'none';
//         document.body.appendChild(element);
//
//         element.click();
//
//         document.body.removeChild(element);
//
//     }
//
//     displayPlots(selectedAirframe) {
//         console.log("displaying plots with airframe: '" + selectedAirframe + "'");
//
//         eventFleetPercents = {};
//         eventNGAFIDPercents = {};
//
//         countData = [];
//         percentData = [];
//
//         for (let [eventName, countsObject] of Object.entries(eventCounts)) {
//
//             console.log("Event count ");
//             console.log(eventCounts);
//             //console.log("checking to plot event: '" + eventName + "', checked? '" + this.state.eventChecked[eventName] + "'");
//             if (!this.state.eventChecked[eventName]) continue;
//             let ngafidPercents = null;
//             if (eventName in eventNGAFIDPercents) {
//                 console.log('getting existing fleetPercents!');
//                 ngafidPercents = eventNGAFIDPercents[eventName];
//             } else {
//                 console.log('setting initial fleetPercents!');
//                 ngafidPercents = {
//                     name : eventName + ' - All Fleets',
//                     type : 'scatter',
//                     hoverinfo : 'x+text',
//                     hovertext : [],
//                     y : [],
//                     x : [],
//                     flightsWithEventCounts : {},
//                     totalFlightsCounts :{}
//                 }
//                 eventNGAFIDPercents[eventName] = ngafidPercents;
//             }
//             console.log("Counts object : ");
//             console.log(countsObject);
//
//
//             for (let [airframe, value] of Object.entries(countsObject)) {
//                 console.log("Value : ");
//                 console.log(value);
//                 if (value.airframeName === "Garmin Flight Display") continue;
//                 if (selectedAirframe !== value.airframeName && selectedAirframe !== "All Airframes") continue;
//
//                 /*
//                 console.log("airframes, airframeName, value:");
//                 console.log(airframes);
//                 console.log(airframe);
//                 console.log(value);
//                 */
//
//
//                 value.name = value.eventName + " - " + value.airframeName;
//                 value.x = value.dates;
//                 value.type = 'scatter';
//                 value.hoverinfo = 'x+text';
//
//                 console.log("Value update 1 : ");
//                 console.log(value);
//                 console.log("Airframes : " );
//                 console.log(airframes);
//                 if (airframes.indexOf(value.airframeName) >= 0) countData.push(value);
//                 //don't add airframes to the count plot that the fleet doesn't have
//                 value.y = value.aggregateTotalEventsCounts;
//                 value.hovertext = [];
//
//                 console.log("Count Data ");
//                 console.log(countData);
//                 for (let i = 0; i < value.dates.length; i++) {
//                     let date = value.dates[i];
//                     if (date in ngafidPercents.flightsWithEventCounts) {
//                         ngafidPercents.flightsWithEventCounts[date] += value.aggregateFlightsWithEventCounts[i];
//                         ngafidPercents.totalFlightsCounts[date] += value.aggregateTotalFlightsCounts[i];
//                     }
//                     else {
//                         ngafidPercents.flightsWithEventCounts[date] = value.aggregateFlightsWithEventCounts[i];
//                         ngafidPercents.totalFlightsCounts[date] = value.aggregateTotalFlightsCounts[i];
//                     }
//                 }
//
//                 for (let i = 0; i < value.dates.length; i++) {
//                     let date = value.dates[i];
//                     value.hovertext.push(value.y[i] + " events in " + value.flightsWithEventCounts[i] + " of " + value.totalFlightsCounts[i] + " flights : " + value.eventName + " - " + value.airframeName);
//                 }
//
//             }
//         }
//         console.log("Event NGAFID Percents ");
//         console.log(eventNGAFIDPercents);
//         for (let [eventName, ngafidValue] of Object.entries(eventNGAFIDPercents)) {
//             // let ngafidValue = eventNGAFIDPercents[eventName];
//             percentData.push(ngafidValue);
//             ngafidValue.x = [];
//             ngafidValue.y = [];
//
//             for (let date of Object.keys(ngafidValue.flightsWithEventCounts).sort()) {
//                 ngafidValue.x.push(date);
//                 let v = 100.0 * parseFloat(ngafidValue.flightsWithEventCounts[date]) / parseFloat(ngafidValue.totalFlightsCounts[date]);
//                 ngafidValue.y.push(v);
//                 var fixedText = "";
//                 if (v > 0 && v < 1) {
//                     fixedText = v.toFixed(-Math.ceil(Math.log10(v)) + 2) + "%";
//                 } else {
//                     fixedText = v.toFixed(2) + "%";
//                 }
//                 console.log("Hover text ");
//                 console.log(fixedText  + " (" + ngafidValue.flightsWithEventCounts[date] + " of " + ngafidValue.totalFlightsCounts[date] + " flights) : " + ngafidValue.name)
//                 ngafidValue.hovertext.push(fixedText  + " (" + ngafidValue.flightsWithEventCounts[date] + " of " + ngafidValue.totalFlightsCounts[date] + " flights) : " + ngafidValue.name);
//
//             }
//         }
//         console.log("Count Data");
//         console.log(countData);
//         var countLayout = {
//             title : 'Event Counts Over Time',
//             hovermode : "x unified",
//             //autosize: false,
//             //width: 500,
//             //height: 500,
//             margin: {
//                 l: 50,
//                 r: 50,
//                 b: 50,
//                 t: 50,
//                 pad: 4
//             }
//         };
//
//         var percentLayout = {
//             title : 'Percentage of Flights With Event Over Time',
//             hovermode : "x unified",
//             //autosize: false,
//             //width: 500,
//             //height: 500,
//             margin: {
//                 l: 50,
//                 r: 50,
//                 b: 50,
//                 t: 50,
//                 pad: 4
//             }
//         };
//
//         var config = {responsive: true};
//         Plotly.newPlot('count-trends-plot', countData, countLayout, config);
//         Plotly.newPlot('percent-trends-plot', percentData, percentLayout, config);
//     }
//
//
//     checkEvent(eventName) {
//         console.log("checking event: '" + eventName + "'");
//         this.state.eventChecked[eventName] = !this.state.eventChecked[eventName];
//         this.setState(this.state);
//
//         let startDate = this.state.startYear + "-";
//         let endDate = this.state.endYear + "-";
//
//         //0 pad the months on the front
//         if (parseInt(this.state.startMonth) < 10) startDate += "0" + parseInt(this.state.startMonth);
//         else startDate += this.state.startMonth;
//         if (parseInt(this.state.endMonth) < 10) endDate += "0" + parseInt(this.state.endMonth);
//         else endDate += this.state.endMonth;
//
//         var submission_data = {
//             startDate : startDate + "-01",
//             endDate : endDate + "-28",
//             eventName : eventName
//         };
//
//         if (eventName in eventCounts) {
//             console.log("already loaded counts for event: '" + eventName + "'");
//             aggregateTrendsPage.displayPlots(aggregateTrendsPage.state.airframe);
//
//         } else {
//             $('#loading').show();
//             console.log("showing loading spinner!");
//
//             let aggregateTrendsPage = this;
//
//             $.ajax({
//                 type: 'POST',
//                 url: '/protected/aggregate_monthly_event_counts',
//                 data : submission_data,
//                 dataType : 'json',
//                 success : function(response) {
//                     console.log("received response: ");
//                     console.log(response);
//
//                     $('#loading').hide();
//
//                     if (response.err_msg) {
//                         errorModal.show(response.err_title, response.err_msg);
//                         return;
//                     }
//
//                     eventCounts[eventName] = response;
//                     aggregateTrendsPage.displayPlots(aggregateTrendsPage.state.airframe);
//                 },
//                 error : function(jqXHR, textStatus, errorThrown) {
//                     errorModal.show("Error Loading Uploads", errorThrown);
//                 },
//                 async: true
//             });
//         }
//     }
//
//     updateStartYear(newStartYear) {
//         console.log("setting new start year to: " + newStartYear);
//         this.setState({startYear : newStartYear, datesChanged : true});
//         console.log(this.state);
//     }
//
//     updateStartMonth(newStartMonth) {
//         console.log("setting new start month to: " + newStartMonth);
//         this.setState({startMonth : newStartMonth, datesChanged : true});
//         console.log(this.state);
//     }
//
//     updateEndYear(newEndYear) {
//         console.log("setting new end year to: " + newEndYear);
//         this.setState({endYear : newEndYear, datesChanged : true});
//         console.log(this.state);
//     }
//
//     updateEndMonth(newEndMonth) {
//         console.log("setting new end month to: " + newEndMonth);
//         this.setState({endMonth : newEndMonth, datesChanged : true});
//         console.log(this.state);
//     }
//
//     dateChange() {
//         console.log("[trendscard] notifying date change 2, startYear: '" + this.state.startYear + "', startMonth: '" + this.state.startMonth + ", endYear: '" + this.state.endYear + "', endMonth: '" + this.state.endMonth + "'");
//
//         for (let [eventName, value] of Object.entries(this.state.eventChecked)) {
//             this.state.eventChecked[eventName] = false;
//         }
//         this.state.datesChanged = false;
//         this.setState(this.state);
//
//         eventCounts = {};
//         this.displayPlots(this.state.airframe);
//     }
//
//     airframeChange(airframe) {
//         this.setState({airframe});
//         this.displayPlots(airframe);
//     }
//
//
//     render() {
//         //console.log(systemIds);
//
//         const numberOptions = {
//             minimumFractionDigits: 2,
//             maximumFractionDigits: 2
//         };
//
//         return (
//             <div>
//                 <SignedInNavbar activePage={"trends"} waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>
//
//                 <div className="container-fluid">
//
//                     <div className="row">
//                         <div className="col-lg-12">
//                             <div className="card mb-2 m-2" style={{background : "rgba(248,259,250,0.8)"}}>
//                                 <TimeHeader
//                                     name="Event Trends"
//                                     airframes={airframes}
//                                     airframe={this.state.airframe}
//                                     startYear={this.state.startYear}
//                                     startMonth={this.state.startMonth}
//                                     endYear={this.state.endYear}
//                                     endMonth={this.state.endMonth}
//                                     datesChanged={this.state.datesChanged}
//                                     dateChange={() => this.dateChange()}
//                                     airframeChange={(airframe) => this.airframeChange(airframe)}
//                                     updateStartYear={(newStartYear) => this.updateStartYear(newStartYear)}
//                                     updateStartMonth={(newStartMonth) => this.updateStartMonth(newStartMonth)}
//                                     updateEndYear={(newEndYear) => this.updateEndYear(newEndYear)}
//                                     updateEndMonth={(newEndMonth) => this.updateEndMonth(newEndMonth)}
//                                     exportCSV={() => this.exportCSV()}
//                                 />
//
//                                 <div className="card-body" style={{padding:"0"}}>
//                                     <div className="row" style={{margin:"0"}}>
//                                         <div className="col-lg-2" style={{padding:"8 8 8 8"}}>
//
//                                             {
//                                                 eventNames.map((eventName, index) => {
//                                                     return (
//                                                         <div key={index} className="form-check">
//                                                             <input className="form-check-input" type="checkbox" value="" id={"event-check-" + index} checked={this.state.eventChecked[eventName]} onChange={() => this.checkEvent(eventName)}></input>
//
//                                                             <OverlayTrigger overlay={(props) => (
//                                                                 <Tooltip {...props}>{GetDescription(eventName)}</Tooltip>)}
//                                                                             placement="bottom">
//                                                                 <label className="form-check-label">
//                                                                     {eventName}
//                                                                 </label>
//
//
//                                                             </OverlayTrigger>
//
//
//                                                         </div>
//                                                     );
//                                                 })
//                                             }
//
//                                         </div>
//
//                                         <div className="col-lg-10" style={{padding:"0 0 0 8"}}>
//                                             <div id="count-trends-plot"></div>
//                                             <div id="percent-trends-plot"></div>
//                                         </div>
//                                     </div>
//                                 </div>
//                             </div>
//                         </div>
//                     </div>
//                 </div>
//             </div>
//         );
//     }
// }
//
//
// var aggregateTrendsPage = ReactDOM.render(
//     <AggregateTrendsPage />,
//     document.querySelector('#trends-page')
// );
//
// aggregateTrendsPage.displayPlots("All Airframes");
