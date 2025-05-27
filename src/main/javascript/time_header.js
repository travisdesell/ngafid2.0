import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";

export default class TimeHeader extends React.Component {
    constructor(props) {
        super(props);

        let buttonContent = this.props.buttonContent;
        if (buttonContent === undefined) {
            buttonContent = "Update";
        }

        let years = [];
        for (let year = 2000; year <= props.endYear; year++) {
            years.push(year);
        }

        this.state = {
            years : years,
            months : ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
            buttonContent: buttonContent,
        };
    }

    makeHeaderContents(additionalHeaderContents, additionalRowContents) {
        console.log(this.props.airframes);
        console.log(this.state);

        let exportButton = null;
        if ('exportCSV' in this.props) {
            exportButton = (
                <div className="col-auto">
                    <button className="btn btn-primary" onClick={() => this.props.exportCSV()}>Export</button>
                </div>
            );
        }

        let airframe = null;
        if ('airframe' in this.props) {
            airframe = (
               <div className="col-auto">
                   <div className="dropdown">
                       <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                           {this.props.airframe}
                       </button>
                       <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                           {
                               this.props.airframes.map((airframeName, index) => {
                                   return (
                                       <a key={index} className="dropdown-item" onClick={event => this.props.airframeChange(airframeName)}>{airframeName}</a>
                                   );
                               })
                           }
                       </div>
                   </div>
               </div>
            );
        }
        let tags = null;
        if ('tagName' in this.props) {
            tags = (
                <div className="col-auto">
                    <div className="dropdown">
                        <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                            {this.props.tagName}
                        </button>
                        <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                            {
                                this.props.tagNames.map((tagName, index) => {
                                    return (
                                        <a key={index} className="dropdown-item" onClick={event => this.props.tagNameChange(tagName)} onChange={event => this.props.updateTags(event.target.value)}>{tagName}</a>
                                    );
                                })
                            }
                        </div>
                    </div>
                </div>
            );
        }

        return  (
            <div className="form-row justify-content-center d-flex align-items-center" style={{ textAlign: 'center' }}>
                { exportButton }

                <div className="col-auto">
                    { tags }
                </div>
                <div className="col-auto">
                    { airframe } 
                </div>
                <div className="col-auto">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <div className="time-selector">

                                &nbsp;Start Date&nbsp;

                                <select id="start-year-select" className="custom-select" value={this.props.startYear} onChange={event => this.props.updateStartYear(event.target.value)} style={{width:"fit-content", border:"1px solid var(--c_border_alt)"}}>
                                    {
                                        this.state.years.map((year, index) => {
                                            if (year <= this.props.endYear)
                                                return (
                                                    <option key={index} value={year}>{year}</option>
                                                );
                                            else
                                                return null;
                                        })
                                    }
                                </select>
                                &nbsp;
                                <select id="start-month-select" className="custom-select" value={this.props.startMonth} onChange={event => this.props.updateStartMonth(event.target.value)} style={{width:"fit-content", border:"1px solid var(--c_border_alt)"}}>
                                    {
                                        this.state.months.map((month, index) => {
                                            return (
                                                <option key={index} value={index+1}>{month}</option>
                                            );
                                        })
                                    }
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-auto">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <div className="time-selector">

                                &nbsp;End Date&nbsp;
                                
                                <select id="end-year-select" className="custom-select" value={this.props.endYear} onChange={event => this.props.updateEndYear(event.target.value)} style={{width:"fit-content", border:"1px solid var(--c_border_alt)"}}>
                                    {
                                        this.state.years.map((year, index) => {
                                            if (year >= this.props.startYear)
                                                return (
                                                    <option key={index} value={year}>{year}</option>
                                                );
                                            else
                                                return null;
                                        })
                                    }
                                </select>
                                &nbsp;
                                <select id="end-month-select" className="custom-select" value={this.props.endMonth} onChange={event => this.props.updateEndMonth(event.target.value)} style={{width:"fit-content", border:"1px solid var(--c_border_alt)"}}>
                                    {
                                        this.state.months.map((month, index) => {
                                            return (
                                                <option key={index} value={index+1}>{month}</option>
                                            );
                                        })
                                    }
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                { additionalHeaderContents }

                <div className="col-auto mr-5">
                    <button className="btn btn-primary" onClick={() => this.props.dateChange()} disabled={!this.props.datesChanged}>{this.state.buttonContent}</button>
                </div>

                { additionalRowContents }
            </div>
         );
    }

    render() {

        let descriptiveList = null;

        if (this.props.name === "Event Trends") {

            descriptiveList = (
                <ul style={{fontSize: "16px", alignContent: "center", alignItems: "center", justifyContent: "start", marginBottom: "0", fontStyle: "italic"}}>
                    <li>Select a date range, then use the Update button to scan for events</li>
                    <li>Events for the current calendar year will be loaded automatically</li>
                </ul>
            );
        }

        return (
            <div className="row card-header d-flex" style={{padding:"7 20 7 20", margin:"0"}}>
                <h4 className="mr-auto d-flex flex-row" style={{margin:"4 0 4 0", alignContent:"center", alignItems:"center", justifyContent:"start"}}>
                    {this.props.name}
                    {descriptiveList}
                </h4>

                { this.makeHeaderContents(this.props.extraHeaderComponents, this.props.extraRowComponents) }
            </div>
        );

    }

};

class TurnToFinalHeaderComponents extends React.Component {
    constructor(props) {
        super(props);
    }

    makeDropdown(currentItem, items, onChange) {
        let dropdownStyle = {
          maxHeight: "200px",
          overflowY: "auto"
        };
        return (
            <div className="col-auto">
                <div className="dropdown">
                    <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        {currentItem}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton" style={dropdownStyle}>
                        {
                            items.map((itemName, index) => {
                                return (
                                    <a key={index} className="dropdown-item" onClick={event => onChange(itemName)}>{itemName}</a>
                                );
                            })
                        }
                    </div>
                </div>
            </div>
        )
    }

    render() {
        var airportsHTML = this.makeDropdown(this.props.airport, this.props.airports, this.props.airportChange);
        var runwaysHTML = this.makeDropdown(this.props.runway, this.props.runways, this.props.runwayChange);
        // var airframesHTML = this.makeDropdown(this.props.airframe, this.props.airframes, this.props.airframeChange);
        return (
            <div className='form-row'>
                {airportsHTML}
                {runwaysHTML}
            </div>
        );
    }
};

export { TimeHeader, TurnToFinalHeaderComponents };