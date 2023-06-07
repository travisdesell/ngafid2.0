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
            decimalPrecision : props.decimalPrecision,
            timeout : props.timeout
        }
    }

    updateTimeout(precision) {
        let timeout = event.target.value;
        console.log("New timeout is: " + timeout);

        this.setState({timeout: timeout});
    }

    render() {

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

        $(function () {
            $('[data-toggle="tooltip"]').tooltip()
        })


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
                                <span className="badge badge-info mr-1">ADMIN</span>
                                Your AirSync Settings:
                            </h6>
                            <div className="form-group" style={formGroupStyle}>
                            <div className="d-flex">
                                <div className="p-2 d-flex" style={formHeaderStyle}>
                                    <label htmlFor="selectedMetricsNames" style={labelStyle}>
                                      Sync Frequency
                                    <i className="ml-1 fa fa-question-circle" data-toggle="tooltip" data-placement="top" title="This is the amount of time the NGAFID waits to check for new AirSync flights. For example, if this is set to 12 hours, the NGAFID will check with the AirSync servers once every 12 hours."></i>
                                    </label>
                                </div>
                                <div className="p-2 d-flex">
                                    <select id="columnNames" className="form-control" onChange={this.updateTimeout.bind(this)} value={this.state.timeout}>
                                        <option key={72}>72 Hours</option>
                                        <option key={48}>48 Hours</option>
                                        <option key={24}>24 Hours</option>
                                        <option key={12}>12 Hours</option>
                                        <option key={6}>6 Hours</option>
                                        <option key={3}>3 Hours</option>
                                        <option key={1}>1 Hour</option>
                                        <option key={0}>30 Minutes</option>
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
