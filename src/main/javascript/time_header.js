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
                    <button className="btn btn-primary-outline" onClick={() => this.props.exportCSV()}>Export</button>
                </div>
            );
        }

        let all_months = ...;
        let airframe = null;
        if ('airframe' in this.props) 
            airframe = (
                <div className="input-group">
                    <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        {this.props.airframe}
                    </button>
                </div>
            );
        else
            airframe = ( );
        return (
            <div className="form-row" style={{textAlign: 'center', verticalAlign: 'center'}}>
                <div className="col-auto">
                    { airframe } 
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <div className="input-group-text">Start Date</div>

                            <select id="start-year-select" className="custom-select" value={this.props.startYear} onChange={event => this.props.updateStartYear(event.target.value)} style={{width:"fit-content"}}>
                                {
                                    this.state.years.map((year, index) => {
                                        if (year <= this.props.endYear)
                                            return (
                                                <option key={index} value={year}>{year}</option>
                                            );
                                        else
                                            return ( );
                                    })
                                }
                            </select>
                            <select id="start-month-select" className="custom-select" value={this.props.startMonth} onChange={event => this.props.updateStartMonth(event.target.value)} style={{width:"fit-content"}}>
                { exportButton }

                <div className="col-auto">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <div className="input-group-text">End Date</div>
                            <select id="end-year-select" className="custom-select" value={this.props.endYear} onChange={event => this.props.updateEndYear(event.target.value)} style={{width:"fit-content"}}>
                                {
                                    this.state.years.map((year, index) => {
                                        if (year >= this.props.startYear)
                                            return (
                                                <option key={index} value={year}>{year}</option>
                                            );
                                        else
                                            return ( );
                                    })
                                }
                            </select>
                            <select id="end-month-select" className="custom-select" value={this.props.endMonth} onChange={event => this.props.updateEndMonth(event.target.value)} style={{width:"fit-content"}}>
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

                { additionalHeaderContents }

                <div className="col-auto mr-5">
                    <button className="btn btn-primary btn-primary-outline" onClick={() => this.props.dateChange()} disabled={!this.props.datesChanged}>{this.state.buttonContent}</button>
                </div>

                { additionalRowContents }
            </div>
         );
    }

    render() {
        return (
            <div className="row card-header d-flex" style={{color : "rgba(75,75,75,250)", padding:"7 20 7 20", margin:"0"}}>
                <h4 className="mr-auto" style={{margin:"4 0 4 0"}}>{this.props.name}</h4>
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
        return (
            <div className="col-auto">
                <div className="dropdown">
                    <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        {currentItem}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
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
