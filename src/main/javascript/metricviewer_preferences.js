import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown';
import DropdownButton from 'react-bootstrap/DropdownButton';
import Pagination from 'react-bootstrap/Pagination';
import FormCheck from 'react-bootstrap/FormCheck';
import Form from 'react-bootstrap/Form';
import FormControl from 'react-bootstrap/FormControl';
import ListGroup from 'react-bootstrap/ListGroup';
import Button from 'react-bootstrap/Button';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';


class MetricViewerSettings extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            decimalPrecision : userPreferences.decimalPrecision,
            selectedMetrics : userPreferences.flightMetrics,
        }
    }

    updatePreferences() {
        console.log("updating preferences");
        console.log(this.state.selectedMetrics);

        var submissionData = {
            flight_metrics : JSON.stringify(this.state.selectedMetrics),
            decimal_precision : this.state.decimalPrecision
        };

        let prefsPage = this;

        $.ajax({
            type: 'POST',
            url: '/protected/preferences',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                prefsPage.setState({
                    selectedMetrics : response.flightMetrics,
                    decimalPrecision : response.decimalPrecision
                });
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Updating User Preferences", errorThrown);
            },   
            async: true 
        });  
    }

    getAllDoubleSeriesNames() {
        let prefsPage = this;
        let metrics = [];

        $.ajax({
            type: 'GET',
            url: '/protected/all_double_series_names',
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                metrics = response.names;
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Getting Column Names", errorThrown);
            },   
            async: false 
        });  

        return metrics;
    }

    addMetric(event) {
        console.log(event);

        let name = event.target.value;
        console.log("adding " + name + " to metric list.");

        this.modifyMetric(name, "addition");
    }

    removeMetric(index) {
        let name = this.state.selectedMetrics[index];

        console.log("removing " + name + " from metric list.");

        this.modifyMetric(name, "deletion");
    }

    modifyMetric(name, type) {
        let prefsPage = this;

        let submissionData = {
            metricName : name,
            modificationType : type
        }

        $.ajax({
            type: 'POST',
            url: '/protected/preferences_metric',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);

                prefsPage.setState({
                    selectedMetrics : response
                });
            },   
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Updating User Preferences", errorThrown);
            },   
            async: true 
        });  
    }
    
    changePrecision(precision) {
        this.state.decimalPrecision = event.target.value;

        this.updatePreferences();
    }

    render() {
        let selectedMetrics = this.state.selectedMetrics;
        let defSeriesNames = this.getAllDoubleSeriesNames();
        let serverMetrics = defSeriesNames.filter((e) => !selectedMetrics.includes(e));

        let styleButtonSq = {
            flex : "right",
            float : "auto"
        };

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left'
        };

        let listStyle = {
            maxHeight: "400px",
            overflowY: "scroll"
        };

        let formGroupStyle = {
            marginBottom: '0px',
            padding: '0 4 0 4'
        };

        let formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        let metricsRow = (
            <div className="d-flex">
                <div className="p-2" style={formHeaderStyle}>
                    <label htmlFor="selectedMetricsNames" style={labelStyle}>Selected Metrics:</label>
                </div>
                <div className="p-2">
                    {
                        selectedMetrics.map((columnName, index) => {
                            return (<button type="button" key={columnName} className="btn btn-primary mr-1" onClick={() => this.removeMetric(index)}>{columnName} <i className="fa fa-times p-1"></i></button>)
                        })
                    }
                </div>
                <div className="p-2 flex">
                    <select id="columnNames" className="form-control" onChange={this.addMetric.bind(this)} value="Select a new metric to add...">
                    <option key={0} value="Select a new metric to add..." disabled>Select a new metric to add...</option>
                    {
                        serverMetrics.map((seriesName, index) => {
                            return (
                                <option key={index} value={seriesName}>{seriesName}</option>
                            )
                        })
                    }
                    </select>               
                </div>
            </div>
        );


        //let listStyle = {
            //maxHeight: "400px",
            //overflowX: "scroll",
            //flexDirection: "row"
        //}

              return (
                <div className="card-body">
                    <div className="col" style={{padding:"0 0 0 0"}}>
                        <div className="card" style={{background : "rgba(248,259,250,0.8)"}}>
                            <h6 className="card-header">
                                Your Flight Metric Viewer Preferences:
                            </h6>
                            <div className="form-group" style={formGroupStyle}>
                            {metricsRow}
                            <div className="d-flex">
                                <div className="p-2" style={formHeaderStyle}>
                                    <label htmlFor="selectedMetricsNames" style={labelStyle}>Decimal Precision:</label>
                                </div>
                                <div className="p-2 flex">
                                    <select id="columnNames" className="form-control" onChange={this.changePrecision.bind(this)}>
                                        <option key={0}>0</option>
                                        <option key={1}>1</option>
                                        <option key={2}>2</option>
                                        <option key={3}>3</option>
                                        <option key={4}>4</option>
                                        <option key={5}>5</option>
                                        <option key={6}>6</option>
                                    </select>               
                                </div>
                            <hr style={{padding:"0", margin:"0 0 0 0"}}></hr>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}


export { MetricViewerSettings };
