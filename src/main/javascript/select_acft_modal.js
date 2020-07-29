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


class SelectAircraftModal extends React.Component {
    constructor(props) {
        super();
		console.log("getSimAircraft invoked");
		this.state = {
			paths : [],
			selectedPath : null
		}
		this.getSimAircraft();
	}

    show(type, submitMethod) {
        //this.state.submitMethod = submitMethod;
		this.state.version = type;
		this.state.submitMethod = submitMethod;
        this.setState(this.state);
        $("#select_aircraft-modal").modal('show');
    }

    modalClicked() {
        console.log("modal submit clicked!");
        this.state.submitMethod(this.state.version, this.state.selectedPath);
    }

	triggerInput(){
		$('#upload-file-input').trigger('click');
        $('#upload-file-input:not(.bound)').addClass('bound').change(function() {
            console.log("number files selected: " + this.files.length);
            console.log( this.files );
        });
		
		let filePath = $('#upload-file-input').val();
		console.log("selected path from input: "+filePath);
	}

	selectPath(path){
		this.state.selectedPath = path;
		this.setState(this.state);
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

	addFile(){
		let thisModal = this;

		let submissionData = {
			"type" : "CACHE"
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

	removeFile(){
		let thisModal = this;

		let submissionData = {
			"type" : "RMCACHE"
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


    render() {
        let formGroupStyle = {
            marginBottom: '8px'
        };

        let formHeaderStyle = {
            width: '150px',
            flex: '0 0 150px'
        };

        let labelStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'right'
        };

        let hiddenStyle = {
            display : "none"
        };

        let validationMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

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

		let selectRow = "";
		if(paths != null && paths.length > 0){
			selectRow = (
				<ListGroup id="listgrp" defaultActiveKey="#custom" style={listStyle}>
				<ListGroup.Item action href="#custom">
				  <input type="text" id="description" className="form-control" placeholder="Enter a new or custom path to a *.acf file"/>
				</ListGroup.Item>
				{
					paths.map((path, index) => {
						let key = "#" + index;
						return(
							<ListGroup.Item action href={key}>
								<Container>
									<Row className="justify-content-md-center">
										<Col xs lg="11">
											{path}
										</Col>
										<Col xs lg="1">
											<button className="m-1 btn btn-outline-secondary align-right" style={styleButtonSq} title="Permanently delete this cached aircraft">
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
		}


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

                    Please select (or create) a filepath for the *.acf file that matches the aircraft in your X-Plane library that you would like simulated.

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

var selectAircraftModal = ReactDOM.render(
    <SelectAircraftModal />,
    document.querySelector("#select_aircraft-modal-content")
);

export { selectAircraftModal };
