import 'bootstrap';

import React, { Children } from "react";

export default class TimeHeader extends React.Component {

    constructor(props) {

        super(props);

        let buttonContent = this.props.buttonContent;
        if (buttonContent === undefined) {
            buttonContent = "Update";
        }

        const years = [];
        for (let year = 2000; year <= props.endYear; year++) {
            years.push(year);
        }

        this.state = {
            years: years,
            months: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"],
            buttonContent: buttonContent,
        };

    }

    makeHeaderContents(additionalHeaderContents, additionalRowContents) {
        console.log(this.props.airframes);
        console.log(this.state);

        let exportButton = null;
        if ('exportCSV' in this.props) {
            exportButton = (
                <>
                    <button className="btn btn-secondary" onClick={() => this.props.exportCSV()}>
                        <i className="fa fa-cloud-download mr-2" aria-hidden="true"></i>
                        Export
                    </button>
                    <div className="vertical-separator" />
                </>
            );
        }

        let airframe = null;
        if ('airframe' in this.props) {
            airframe = (
                <div className="dropdown">
                    <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        {this.props.airframe}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                        {
                            this.props.airframes.map((airframeName, index) => {
                                return (
                                    <a
                                        key={index}
                                        className="dropdown-item"
                                        onClick={() => this.props.airframeChange(airframeName)}
                                    >
                                        {airframeName}
                                    </a>
                                );
                            })
                        }
                    </div>
                </div>
            );
        }
        let tags = null;
        if ('tagName' in this.props) {
            tags = (
                <div className="dropdown">
                    <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        {this.props.tagName}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                        {
                            this.props.tagNames.map((tagName, index) => {
                                return (
                                    <a key={index} className="dropdown-item" onClick={() => this.props.tagNameChange(tagName)} onChange={event => this.props.updateTags(event.target.value)}>{tagName}</a>
                                );
                            })
                        }
                    </div>
                </div>
            );
        }

        const updateButtonDisabled = !this.props.datesOrAirframeChanged;
        const updateButtonIcon = (() => {

            //Button disabled -> Ban 🚫
            if (updateButtonDisabled)
                return 'fa-ban';

            //All good -> Refresh 🔄
            return 'fa-refresh';

        })();

        return (
            <div className="flex flex-row items-center justify-start gap-4" style={{ textAlign: 'center' }}>

                {/* Export Button */}
                {exportButton}

                {/* Tags */}
                {tags}

                {/* Airframe Drop-down */}
                {airframe}

                <div className="vertical-separator" />

                {/* Start and End Date Selectors */}
                <div className="flex flex-row items-center justify-start gap-4">
                    <div className="input-group">
                        <div className="input-group-prepend">
                            <div className="time-selector">

                                <span className="mx-3">Start Date</span>

                                {/* Start Date -- Year Select */}
                                <select
                                    id="start-year-select"
                                    className="custom-select rounded-r-none! cursor-pointer"
                                    value={this.props.startYear}
                                    onChange={event => this.props.updateStartYear(event.target.value)}
                                    style={{ width: "fit-content", border: "1px solid var(--c_border_alt)" }}
                                >
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

                                {/* Start Date -- Month Select */}
                                <select
                                    id="start-month-select"
                                    className="custom-select rounded-l-none! cursor-pointer"
                                    value={this.props.startMonth}
                                    onChange={event => this.props.updateStartMonth(event.target.value)}
                                    style={{ width: "fit-content", border: "1px solid var(--c_border_alt)" }}
                                >
                                    {
                                        this.state.months.map((month, index) => {

                                            //Same year, can't select months after the end month
                                            if (this.props.startYear === this.props.endYear && (index + 1) > this.props.endMonth)
                                                return null;

                                            //Otherwise, all months are valid
                                            return (
                                                <option key={index} value={index + 1}>{month}</option>
                                            );
                                        })
                                    }
                                </select>
                            </div>
                        </div>
                    </div>

                    <div className="input-group">
                        <div className="input-group-prepend">
                            <div className="time-selector">

                                <span className="mx-3">End Date</span>

                                {/* End Date -- Year Select */}
                                <select
                                    id="end-year-select"
                                    className="custom-select rounded-r-none! cursor-pointer"
                                    value={this.props.endYear}
                                    onChange={event => this.props.updateEndYear(event.target.value)}
                                    style={{ width: "fit-content", border: "1px solid var(--c_border_alt)" }}
                                >
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

                                {/* End Date -- Month Select */}
                                <select
                                    id="end-month-select"
                                    className="custom-select rounded-l-none! cursor-pointer"
                                    value={this.props.endMonth}
                                    onChange={event => this.props.updateEndMonth(event.target.value)}
                                    style={{ width: "fit-content", border: "1px solid var(--c_border_alt)" }}
                                >
                                    {
                                        this.state.months.map((month, index) => {
                                            return (
                                                <option key={index} value={index + 1}>{month}</option>
                                            );
                                        })
                                    }
                                </select>
                            </div>
                        </div>
                    </div>
                </div>

                {additionalHeaderContents}

                <div className="vertical-separator" />

                {/* Child Components */}
                {Children.map(this.props.children, child => {
                    return (
                        <>
                            <div>
                                {child}
                            </div>
                            <div className="vertical-separator" />
                        </>
                    );
                })}

                {/* Update Button */}
                <button
                    className="
                        btn btn-primary
                        disabled:cursor-not-allowed disabled:grayscale
                    "
                    onClick={() => this.props.dateChange()}
                    disabled={updateButtonDisabled}
                >
                    <i className={`fa ${updateButtonIcon} mr-2`} aria-hidden="true"></i>
                    {this.state.buttonContent}
                </button>

                {additionalRowContents}
            </div>
        );
    }

    render() {

        return (
            <div className={`row card-header d-flex ${this.props.className}`} style={{ padding: "7 20 7 20", margin: "0" }}>
                <h4 className="mr-auto" style={{ margin: "4 0 4 0", alignContent: "center" }}>
                    {this.props.name}
                </h4>
                {this.makeHeaderContents(this.props.extraHeaderComponents, this.props.extraRowComponents)}
            </div>
        );
    }

};






class TurnToFinalHeaderComponents extends React.Component {

    constructor(props) {
        super(props);
    }

    makeDropdown(currentItem, items, onChange) {

        const dropdownStyle = {
            maxHeight: "200px",
            overflowY: "auto"
        };

        return (
            <div className="col-auto">
                <div className="dropdown">
                    <button className="btn btn-secondary-outline dropdown-toggle" type="button" id="dropdownMenuButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                        {currentItem}
                    </button>
                    <div className="dropdown-menu" aria-labelledby="dropdownMenuButton" style={dropdownStyle}>
                        {
                            items.map((itemName, index) => {
                                return (
                                    <a key={index} className="dropdown-item" onClick={() => onChange(itemName)}>{itemName}</a>
                                );
                            })
                        }
                    </div>
                </div>
            </div>
        );

    }

    render() {

        const airportsHTML = this.makeDropdown(this.props.airport, this.props.airports, this.props.airportChange);
        const runwaysHTML = this.makeDropdown(this.props.runway, this.props.runways, this.props.runwayChange);
        return (
            <div className='form-row'>
                {airportsHTML}
                {runwaysHTML}
            </div>
        );

    }

};

export { TimeHeader, TurnToFinalHeaderComponents };