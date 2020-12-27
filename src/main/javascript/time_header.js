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
            months : ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
        };
    }

    render() {
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

        return (
            <div className="row card-header d-flex" style={{color : "rgba(75,75,75,250)", padding:"7 20 7 20", margin:"0"}}>
                <h4 className="mr-auto" style={{margin:"4 0 4 0"}}>{this.props.name}</h4>

                { exportButton }

                <div className="form-row">
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
