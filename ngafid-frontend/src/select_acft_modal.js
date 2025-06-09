import 'bootstrap';

import React from "react";
import ReactDOM from "react-dom";

import ListGroup from 'react-bootstrap/ListGroup';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';

import {helpModal} from './help_modal.js';
import {showErrorModal} from './error_modal.js';

import $ from 'jquery';

window.jQuery = $;
window.$ = $;


class SelectAircraftModal extends React.Component {

    constructor() {
        super();
        this.state = {
            paths: [],
            activeId: 0
        };
        this.getSimAircraft();
        this.reOpen = this.reOpen.bind(this);
    }

    show(type, submitMethod) {
        //this.state.submitMethod = submitMethod;
        this.setState({
            version: type,
            submitMethod: submitMethod
        });

        $("#select_aircraft-modal").modal('show');
    }

    reOpen() {
        this.show(this.state.version, this.state.submitMethod, this.state.flightId);
    }

    modalClicked() {
        let selectedPath = this.state.paths[this.state.activeId - 1];
        const useMSL = $('#altCheck').is(':checked');
        console.log(`will use msl: ${  useMSL}`);

        if (this.state.activeId == 0) {
            selectedPath = $('#cust_path').val();
            if (selectedPath == null || selectedPath.match(/^ *$/) !== null) {
                console.log("selected path is not formatted correctly!");
                const title = "Format Error";
                const message = "Please make sure there is a path selected or there is a correctly formatted path in the custom path box. Press help for more information";
                helpModal.show(title, message, this.reOpen);
                return;
            }
            this.addFile(selectedPath); //cache the filepath in the server
        }

        console.log("modal submit clicked!");
        this.state.submitMethod(this.state.version, selectedPath, useMSL);
    }

    selectPathId(pathId) {
        this.setState({ activeId: pathId });
    }

    helpClicked() {
        const title = "X-Plane Aircraft Selector Help";
        const message = "The paths in the list are filepaths to aircraft from previous exports. To add a new aircraft, use the text " +
            "field at the top of the list. The path should follow the format (no quotes): \"Aircraft/Laminar Research/Cessna 172SP/Cessna172SP.acf\"." +
            " It is very important to exclude the leading '/' from the filepath right before the 'Aircraft' directory.";
        helpModal.show(title, message, this.reOpen);
    }

    getSimAircraft() {

        $.ajax({
            type: 'GET',
            url: '/api/aircraft/sim-aircraft',
            async: true,
            success: (response) => {
                console.log("Received Response: ", response);

                this.setState({ paths: response });
                if (response != null && response.length > 0) {
                    this.setState({ selectedPath: response[0] });
                }
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
                showErrorModal("Error Loading Aircraft", errorThrown);
            },
        });
    }

    addFile(filepath) {

        const submissionData = {
            "path": filepath
        };

        $.ajax({
            type: 'POST',
            url: '/api/aircraft/sim-aircraft',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {
                console.log("Received response: ", response);
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
                showErrorModal("Error Adding Aircraft", errorThrown);
            },
        });
    }

    removeFile(index) {

        const submissionData = {
            "path": this.state.paths[index]
        };

        $.ajax({
            type: 'DELETE',
            url: '/api/aircraft/sim-aircraft',
            data: submissionData,
            dataType: 'json',
            async: true,
            success: (response) => {

                console.log("Received response: ", response);
                this.setState({ paths: this.state.paths });
                
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.log(jqXHR);
                console.log(textStatus);
                console.log(errorThrown);
                showErrorModal("Error Removing Aircraft", errorThrown);
            },
        });
    }


    render() {
        const styleButtonSq = {
            flex: "right",
            float: "auto"
        };

        const listStyle = {
            maxHeight: "400px",
            overflowY: "scroll"
        };

        console.log("rendering select aircraft (xplane) modal");
        console.log(this.state.paths);
        const paths = this.state.paths;

        const selectRow = (
            <ListGroup id="listgrp" defaultActiveKey="#custom" style={listStyle}>
                <ListGroup.Item active={this.state.activeId == 0} onClick={() => this.selectPathId(0)}>
                    <input type="text" id="cust_path" className="form-control"
                           placeholder="Enter a new or custom path to a *.acf file"/>
                </ListGroup.Item>
                {paths != null && paths.length > 0 &&
                    paths.map((path, index) => {
                        const relIndex = index + 1;
                        const isActive = (this.state.activeId - 1 == index);
                        return (
                            <ListGroup.Item active={isActive} key={index} onClick={() => this.selectPathId(relIndex)}>
                                <Container>
                                    <Row className="justify-content-md-center">
                                        <Col xs lg="11">
                                            {path}
                                        </Col>
                                        <Col xs lg="1">
                                            <button className="m-1 btn btn-outline-secondary align-right"
                                                    style={styleButtonSq} onClick={() => this.removeFile(index)}
                                                    title="Permanently delete this cached aircraft">
                                                <i className="fa fa-trash" aria-hidden="true"></i>
                                            </button>
                                        </Col>
                                    </Row>
                                </Container>
                            </ListGroup.Item>
                        );
                    })
                }
            </ListGroup>
        );


        //have to use a plaintext form for the filepath here
        // see https://stackoverflow.com/questions/3489133/full-path-from-file-input-using-jquery
        return (
            <div className='modal-content'>

                <div className='modal-header'>
                    <h5 id='confirm-modal-title' className='modal-title'>Select Aircraft Filepath for X-Plane</h5>
                    <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='confirm-modal-body' className='modal-body'>
                    <h4>Select *.acf filepath for X-Plane</h4>
                    Please select (or add) a filepath for the *.acf file that matches the aircraft in your X-Plane
                    library that you would like simulated.

                    <div className="row p-2">
                        <div className="col">
                            {selectRow}
                        </div>
                    </div>
                </div>

                <div className="row p-3">
                    <div className="col">
                        <div className="form-check">
                            <input className="form-check-input" type="checkbox" id="altCheck"></input>
                            <label className="form-check-label" htmlFor="defaultCheck1">
                                Use altMSL instead of altAGL
                            </label>
                        </div>
                    </div>
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-primary' data-bs-dismiss='modal'
                            onClick={() => this.modalClicked()}>Submit
                    </button>
                    <button type='button' className='btn btn-success' data-bs-dismiss='modal'
                            onClick={() => this.helpClicked()}>Help
                    </button>
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>Cancel</button>
                </div>

            </div>
        );
    }
}

const container = document.querySelector("#select_aircraft-modal-content");
const root = ReactDOM.createRoot(container);
const selectAircraftModal = root.render(<SelectAircraftModal backdrop="static"/>);

export {selectAircraftModal};
