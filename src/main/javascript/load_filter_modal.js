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


class LoadFilterModal extends React.Component {
    constructor(props) {
        super(props);
    }

    show(filters, submitMethod) {
        //this.state.submitMethod = submitMethod;
        this.setState({
            filters : filters,
            submitMethod : submitMethod
        });

        $("#load_filter-modal").modal('show');
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

    render() {
        let styleButtonSq = {
            flex : "right",
            float : "auto"
        };

        let listStyle = {
            maxHeight: "400px",
            overflowY: "scroll"
        }

        console.log("loading filter select modal");
        console.log(this.state.paths);
        let filters = this.state.filters;

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
                                    <Col xs lg="12">
                                        {path}
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
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='confirm-modal-body' className='modal-body'>
                    <h4>Select *.acf filepath for X-Plane</h4>
                        Please select a previously saved filter

                    <div className="row p-2">
                        <div className="col">
                            {selectRow}
                        </div>
                    </div>
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-primary' data-dismiss='modal' onClick={() => this.modalClicked()}>Submit</button>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Cancel</button>
                </div>

            </div>
        );
    }
}

var loadFilterModal = ReactDOM.render(
    <LoadFilterModal backdrop="static" />,
    document.querySelector("#load_filter-modal-content")
);

export { loadFilterModal };
