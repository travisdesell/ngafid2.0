import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import { errorModal } from "./error_modal.js";

export default class TimeHeader extends React.Component {
    constructor(props) {
        super(props);

        console.log(this.props.airframes);

        let years = [];
        for (let year = props.startYear; year <= props.endYear; year++) {
            years.push(year);
        }

        this.state = {
            years : years,
            months : ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
        };
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
        console.log(this.props.airframes);
        console.log(this.state);

        var airportsHTML;
        if (this.props.airports.length > 0) {
            airportsHTML = this.makeDropdown(this.props.airport, this.props.airports, this.props.airportChange);
        }

        var runwaysHTML;
        if (this.props.runways.length > 0) {
            runwaysHTML = this.makeDropdown(this.props.runway, this.props.runways, this.props.runwayChange);
        } else {
            runwaysHTML = '';
        }

        var airframesHTML;
        if (this.props.airframes.length > 0) {
            airframesHTML = this.makeDropdown(this.props.airframe, this.props.airframes, this.props.airframeChange);
        } else {
            airframesHTML = '';
        }

        return (
            <div className="row card-header d-flex" style={{color : "rgba(75,75,75,250)", padding:"7 20 7 20", margin:"0"}}>
                <h4 className="mr-auto" style={{margin:"4 0 4 0"}}>{this.props.name}</h4>

                <div className="form-row">
                    {airframesHTML}

                    {airportsHTML}

                    {runwaysHTML}

                    <div className="col-auto">
                        <div className="input-group">
                            <div className="input-group-prepend">
                                <div className="input-group-text">Start Date</div>

                                <select id="start-year-select" className="custom-select" value={this.props.startYear} onChange={event => this.props.updateStartYear(event.target.value)} style={{width:"85"}}>
                                    {
                                        this.state.years.map((year, index) => {
                                            return (
                                                <option key={index} value={year}>{year}</option>
                                            );
                                        })
                                    }
                                </select>
                                <select id="start-month-select" className="custom-select" value={this.props.startMonth} onChange={event => this.props.updateStartMonth(event.target.value)} style={{width:"70"}}>
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

                    <div className="col-auto">
                        <div className="input-group">
                            <div className="input-group-prepend">
                                <div className="input-group-text">End Date</div>
                                <select id="end-year-select" className="custom-select" value={this.props.endYear} onChange={event => this.props.updateEndYear(event.target.value)} style={{width:"85"}}>
                                    {
                                        this.state.years.map((year, index) => {
                                            return (
                                                <option key={index} value={year}>{year}</option>
                                            );
                                        })
                                    }
                                </select>
                                <select id="end-month-select" className="custom-select" value={this.props.endMonth} onChange={event => this.props.updateEndMonth(event.target.value)} style={{width:"70"}}>
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

                    <div className="col-auto">
                        <button className="btn btn-primary-outline" onClick={() => this.props.dateChange()} disabled={!this.props.datesChanged}>Update</button>
                    </div>
                </div>

            </div>
        );
    }
};
