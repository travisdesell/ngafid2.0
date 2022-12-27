import 'bootstrap';
import React, {Component} from "react";
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


class EmailPreferences extends React.Component {
    constructor(props) {
        super(props);

        this.state = {}
    }

    render() {
        let formGroupStyle = {
            marginBottom: '0px',
            padding: '0 4 0 4'
        };

        return (
            <div className="card-body">
                <div className="col" style={{padding: "0 0 0 0"}}>
                    <div className="card" style={{background: "rgba(248,259,250,0.8)"}}>
                        <h6 className="card-header">
                            Your Email Preferences:
                        </h6>
                        <div className="form-group" style={formGroupStyle}>
                            <div className="d-flex">
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}


export {EmailPreferences};
