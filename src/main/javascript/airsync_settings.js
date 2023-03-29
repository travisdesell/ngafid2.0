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


class AirSyncSettings extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            selectedMetrics : props.selectedMetrics,
            decimalPrecision : props.decimalPrecision
        }
    }

    updatePrecision() {
        console.log("Updating Precision to " + this.state.decimalPrecision);

        var submissionData = {
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

        this.updatePrecision();
    }

    render() {
        let exemptCols = ["LOC-I Index", "Stall Index"]; //put columns here that we dont want to show in the popup
        let selectedMetrics = this.state.selectedMetrics;
        let defSeriesNames = this.getAllDoubleSeriesNames();
        let serverMetrics = defSeriesNames.filter((e) => !selectedMetrics.includes(e));
        serverMetrics = serverMetrics.filter((e) => !exemptCols.includes(e));

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
            flex: '0 0 180px'
        };


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
                                Your AirSync Settings:
                            </h6>
                            <div className="form-group" style={formGroupStyle}>
                            <div className="d-flex">
                                <div className="p-2 d-flex" style={formHeaderStyle}>
                                    <label htmlFor="selectedMetricsNames" style={labelStyle}>Timeout</label>
                                </div>
                                <div className="p-2 d-flex">
                                    <select id="columnNames" className="form-control" onChange={this.changePrecision.bind(this)} value="1 Day">
                                        <option key={0}>12 Hours</option>
                                        <option key={1}>1440</option>
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


export { AirSyncSettings };
