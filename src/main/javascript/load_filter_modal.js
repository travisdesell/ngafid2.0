import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";
import ListGroup from 'react-bootstrap/ListGroup';
import Container from 'react-bootstrap/Container';
import Col from 'react-bootstrap/Col';
import Row from 'react-bootstrap/Row';

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class LoadFilterModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            activeId : 0,
            filters : [],
            submitMethod : undefined
        }
    }

    show(filters, submitMethod) {
        //this.state.submitMethod = submitMethod;
        this.setState({
            filters : filters,
            submitMethod : submitMethod
        });

        $("#load_filter-modal").modal('show');
    }

    selectPathId(pathId){
        this.state.activeId = pathId;
        this.setState(this.state);
    }

    modalClicked() {
        let pathId = this.state.activeId - 1;
        this.state.submitMethod(this.state.filters[pathId].filter);
    }

    getSelectedFilter() {
        return this.state.filters[this.state.activeId].filter;
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
        let filters = this.state.filters;

        let selectRow = (
            <ListGroup id="listgrp" defaultActiveKey="#custom" style={listStyle}>
            <ListGroup.Item active={this.state.activeId == 0} onClick={() => this.selectPathId(0)}>
            </ListGroup.Item>
            {filters != null && filters.length > 0 &&
                filters.map((filter, index) => {
                    let relIndex = index + 1;
                    let isActive = (this.state.activeId - 1 == index);
                    return(
                        <ListGroup.Item active={isActive} key={index} onClick={() => this.selectPathId(relIndex)}>
                            <Container>
                                <Row className="justify-content-md-center">
                                    <Col xs lg="12">
                                        {filter.name}
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
                    <h5 id='confirm-modal-title' className='modal-title'>Select Filter</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='confirm-modal-body' className='modal-body'>
                    <h4>Select the filter you wish to load</h4>
                    <div className="row p-2">
                        <div className="col">
                            {selectRow}
                        </div>
                    </div>
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-primary' data-dismiss='modal' onClick={() => this.modalClicked()}>Done</button>
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
