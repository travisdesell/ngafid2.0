import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";
import { errorModal } from "./error_modal.js";

import ListGroup from 'react-bootstrap/ListGroup';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';

import { helpModal } from './help_modal.js';

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class SelectAircraftModal extends React.Component {
    constructor(props) {
        super();
        this.state = {
            paths : [],
            activeId : 0
        }
        this.getSimAircraft();
        this.reOpen = this.reOpen.bind(this);
    }

    show(type, submitMethod) {
        //this.state.submitMethod = submitMethod;
        this.setState({
            version : type,
            submitMethod : submitMethod
        });

        $("#select_aircraft-modal").modal('show');
    }

    reOpen(){
        this.show(this.state.version, this.state.submitMethod, this.state.flightId);
    }

    modalClicked() {
        var selectedPath = this.state.paths[this.state.activeId - 1];
        var useMSL = $('#altCheck').is(':checked');
        console.log("will use msl: "+useMSL);

        if(this.state.activeId == 0){
            selectedPath = $('#cust_path').val();
            if(selectedPath == null || selectedPath.match(/^ *$/) !== null){
                console.log("selected path is not formatted correctly!");
                let title = "Format Error";
                let message = "Please make sure there is a path selected or there is a correctly formatted path in the custom path box. Press help for more information";
                helpModal.show(title, message, this.reOpen);
                return;
            }
            this.addFile(selectedPath); //cache the filepath in the server
        }
            
        console.log("modal submit clicked!");
        this.state.submitMethod(this.state.version, selectedPath, useMSL);
    }

    selectPathId(pathId){
        this.state.activeId = pathId;
        this.setState(this.state);
    }

    helpClicked(){
        let title = "X-Plane Aircraft Selector Help";
        let message = "The paths in the list are filepaths to aircraft from previous exports. To add a new aircraft, use the text "+
                      "field at the top of the list. The path should follow the format (no quotes): \"Aircraft/Laminar Research/Cessna 172SP/Cessna172SP.acf\"."+
                      " It is very important to exclude the leading '/' from the filepath right before the 'Aircraft' directory.";
        helpModal.show(title, message, this.reOpen);
    }

    getSimAircraft(){
        let thisModal = this;

        $.ajax({
            type: 'GET',
            url: '/protected/sim_acft',
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                
                thisModal.state.paths = response;
                if(thisModal.state.paths != null & thisModal.state.paths.length > 0){
                    thisModal.state.selectedPath = thisModal.state.paths[0];
                }
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

    addFile(filepath){
        let thisModal = this;

        let submissionData = {
            "type" : "cache",
            "path" : filepath
        };

        $.ajax({
            type: 'POST',
            url: '/protected/sim_acft',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }

    removeFile(index){
        let thisModal = this;

        let submissionData = {
            "type" : "rmcache",
            "path" : this.state.paths[index]
        };

        $.ajax({
            type: 'POST',
            url: '/protected/sim_acft',
            data : submissionData,
            dataType : 'json',
            success : function(response) {
                console.log("received response: ");
                console.log(response);
                thisModal.state.paths = response;
                thisModal.setState(thisModal.state);
            },   
            error : function(jqXHR, textStatus, errorThrown) {
            },   
            async: true 
        });  
    }


    render() {
        let styleButtonSq = {
            flex : "right",
            float : "auto"
        };

        let listStyle = {
            maxHeight: "400px",
            overflowY: "scroll"
        }

        console.log("rendering select aircraft (xplane) modal");
        console.log(this.state.paths);
        let paths = this.state.paths;

        let selectRow = (
            <ListGroup id="listgrp" defaultActiveKey="#custom" style={listStyle}>
            <ListGroup.Item active={this.state.activeId == 0} onClick={() => this.selectPathId(0)}>
              <input type="text" id="cust_path" className="form-control" placeholder="Enter a new or custom path to a *.acf file"/>
            </ListGroup.Item>
            {paths != null && paths.length > 0 &&
                paths.map((path, index) => {
                    let relIndex = index + 1;
                    let isActive = (this.state.activeId - 1 == index);
                    return(
                        <ListGroup.Item active={isActive} key={index} onClick={() => this.selectPathId(relIndex)}>
                            <Container>
                                <Row className="justify-content-md-center">
                                    <Col xs lg="11">
                                        {path}
                                    </Col>
                                    <Col xs lg="1">
                                        <button className="m-1 btn btn-outline-secondary align-right" style={styleButtonSq} onClick={() => this.removeFile(index)} title="Permanently delete this cached aircraft">
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
                        Please select (or add) a filepath for the *.acf file that matches the aircraft in your X-Plane library that you would like simulated.

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
                    <button type='button' className='btn btn-primary' data-bs-dismiss='modal' onClick={() => this.modalClicked()}>Submit</button>
                    <button type='button' className='btn btn-success' data-bs-dismiss='modal' onClick={() => this.helpClicked()}>Help</button>
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>Cancel</button>
                </div>

            </div>
        );
    }
}

var selectAircraftModal = ReactDOM.render(
    <SelectAircraftModal backdrop="static" />,
    document.querySelector("#select_aircraft-modal-content")
);

export { selectAircraftModal };
