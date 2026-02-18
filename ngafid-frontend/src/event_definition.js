import 'bootstrap';
import React from "react";

import {Filter, isValidFilter} from './filter.js';


class EventDefinitionCard extends React.Component {
    constructor(props) {
        super(props);

        this.exceedenceFilter = React.createRef();
    }

    componentDidMount() {
        this.setState({
            conditionText: ""
        });
    }

    render() {
        
        const formGroupStyle = {
            marginBottom: '0px',
            padding: '0 4 0 4'
        };

        const formHeaderStyle = {
            width: '200px',
            flex: '0 0 200px'
        };

        const labelStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'right'
        };

        const validationMessageStyle = {
            padding: '7 0 7 0',
            margin: '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

        let validationMessage = "";

        if (this.props.eventName == "") {
            validationMessage = "Please enter an event name.";
        } else if (this.props.startBuffer == "") {
            validationMessage = "Please enter a start buffer time.";
        } else if (parseInt(this.props.startBuffer) < 1) {
            validationMessage = "Start buffer time must be greater than 1 second.";
        } else if (this.props.stopBuffer == "") {
            validationMessage = "Please enter a stop buffer time.";
        } else if (parseInt(this.props.stopBuffer) < 1) {
            validationMessage = "Stop buffer time must be greater than 1 second.";

            //first time rendering this component exceedenceFilter will not be defined
        } else if (this.props.eventDefinitionID > 0 && !isValidFilter(this.props.filters, this.props.rules)) {
            validationMessage = "Correct the incomplete filter.";
        }

        const validationHidden = (validationMessage == "");
        const createEventDisabled = !validationHidden;

        console.log(`rendering with new severityColumnNames: ${  this.props.severityColumnNames}`);

        return (
            <div>
                <div className="form-group" style={formGroupStyle}>
                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="eventName" style={labelStyle}>Event name</label>
                        </div>
                        <div className="p-2 flex-fill">
                            <input type="text" className="form-control" id="eventName" aria-describedby="eventName"
                                   placeholder="Enter event name"
                                   onChange={(event) => this.props.validateEventName(event)}
                                   value={this.props.eventName}/>
                        </div>
                    </div>
                </div>


                <div className="form-group" style={formGroupStyle}>
                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="airframeSelect" style={labelStyle}>Event for</label>
                        </div>
                        <div className="p-2 flex-fill">

                            <select id="airframeSelect" className="form-control"
                                    onChange={(event) => this.props.validateAirframe(event)}
                                    value={this.props.airframe}>
                                {
                                    this.props.airframes.map((airframeInfo, index) => {
                                        return (
                                            <option key={index} value={airframeInfo}>{airframeInfo}</option>
                                        );
                                    })
                                }
                            </select>
                        </div>
                    </div>
                </div>


                <div className="form-group" style={formGroupStyle}>
                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="startBuffer" style={labelStyle}>Start buffer (seconds)</label>
                        </div>
                        <div className="p-2 flex-fill">
                            <input type="number" className="form-control" id="eventName" aria-describedby="startBuffer"
                                   placeholder="Enter seconds" min="1"
                                   onChange={(event) => this.props.validateStartBuffer(event)}
                                   value={this.props.startBuffer}/>
                        </div>
                    </div>
                </div>

                <div className="form-group" style={formGroupStyle}>
                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="stopBuffer" style={labelStyle}>Stop buffer (seconds)</label>
                        </div>
                        <div className="p-2 flex-fill">
                            <input type="number" className="form-control" id="eventName" aria-describedby="stopBuffer"
                                   placeholder="Enter seconds" min="1"
                                   onChange={(event) => this.props.validateStopBuffer(event)}
                                   value={this.props.stopBuffer}/>
                        </div>
                    </div>
                </div>

                <div className="form-group" style={formGroupStyle}>
                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="severityColumnNames" style={labelStyle}>Severity Columns</label>
                        </div>
                        <div className="p-2">

                            <select id="severityColumnNames" className="form-control"
                                    onChange={(event) => this.props.changeSeverityColumn(event)}
                                    value={this.props.severityColumn}>
                                {
                                    this.props.doubleTimeSeriesNames.map((seriesName, index) => {
                                        return (
                                            <option key={index} value={seriesName}>{seriesName}</option>
                                        );
                                    })
                                }
                            </select>
                        </div>
                        <div className="p-2 flex-fill">

                            {
                                this.props.severityColumnNames.map((columnName) => {
                                    return (<button type="button" key={columnName} className="btn btn-primary mr-1"
                                                    onClick={() => this.props.removeSeverityColumn(columnName)}>{columnName}
                                        <i className="fa fa-times p-1"></i></button>);
                                })
                            }

                        </div>
                        <div className="p-2">
                            <button type="button" className="btn btn-primary"
                                    onClick={() => this.props.addSeverityColumn()}>Add Column
                            </button>
                        </div>
                    </div>
                </div>


                <div className="form-group" style={formGroupStyle}>
                    <div className="d-flex">
                        <div className="p-2" style={formHeaderStyle}>
                            <label htmlFor="severityTypeSelect" style={labelStyle}>Severity Type</label>
                        </div>
                        <div className="p-2 flex-fill">

                            <select id="severityTypeSelect" className="form-control"
                                    onChange={(event) => this.props.validateSeverityType(event)}
                                    value={this.props.severityType}>
                                <option key="min" value="MIN">Minimum</option>
                                <option key="max" value="MAX">Maximum</option>
                                <option key="min abs" value="MIN_ABS">Minimum Absolute Value</option>
                                <option key="max abs" value="MAX_ABS">Maximum Absolute Value</option>
                            </select>
                        </div>
                    </div>
                </div>

                {this.props.eventID >= 0 ? (
                    <div className="form-group" style={formGroupStyle} hidden={this.props.eventDefinitionID < 0}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label style={labelStyle}>Exceedence Definition</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <Filter
                                    filterVisible={true}
                                    filters={this.props.filters}
                                    rules={this.props.rules}

                                    getFilter={() => {
                                        return this.props.getFilter();
                                    }}
                                    setFilter={(filter) => this.props.setFilter(filter)}
                                />
                            </div>
                        </div>
                    </div>
                ) : (
                    <div className="form-group" style={formGroupStyle}>
                        <div className="d-flex">
                            <div className="p-2" style={formHeaderStyle}>
                                <label htmlFor="conditionJsonText" style={labelStyle}>Condition JSON Text</label>
                            </div>
                            <div className="p-2 flex-fill">
                                <input type="text" className="form-control" id="conditionJsonText"
                                       aria-describedby="conditionJsonText"
                                       placeholder="Enter condition JSON text"
                                       onChange={(event) => {
                                           this.setState({conditionText: event.target.value});
                                           this.props.setFilter({
                                               "text": event.target.value,
                                           });

                                       }}
                                       value={this.props.filters.text}/>
                            </div>
                        </div>
                    </div>
                )}

                <div className="d-flex">
                    <div className="p-2" style={formHeaderStyle}>
                    </div>
                    <div className="p-2 flex-fill">
                        <span style={validationMessageStyle} hidden={validationHidden}>{validationMessage}</span>
                    </div>
                    <div className="p-2">
                        <button className="btn btn-primary float-right" onClick={() => {
                            this.props.submitFilter();
                        }} disabled={createEventDisabled}>{this.props.submitName}</button>
                    </div>
                </div>

            </div>
        );
    }
}

export {EventDefinitionCard};
