import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class SelectAircraftModal extends React.Component {
    constructor(props) {
        super();
		console.log("getSimAircraft invoked");
		this.getSimAircraft();
	}

    show() {
        //this.state.submitMethod = submitMethod;
        this.setState(this.state);

        $("#select_aircraft-modal").modal('show');
    }

    modalClicked() {
        console.log("modal submit clicked!");
        this.state.submitMethod();
    }

	triggerInput(){
		$('#upload-file-input').trigger('click');
        $('#upload-file-input:not(.bound)').addClass('bound').change(function() {
            console.log("number files selected: " + this.files.length);
            console.log( this.files );
        });
	}


	getSimAircraft(){
		let thisModal = this;

		let submissionData = {
			type : "INIT"
		}

		$.ajax({
			type: 'POST',
            data : submissionData,
			url: '/protected/sim_acft',
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

		console.log("rendering select aircraft (xplane) modal");
		this.getSimAircraft();

        return (
            <div className='modal-content'>
                <div className='modal-header'>
                    <h5 id='confirm-modal-title' className='modal-title'>Confirm Operation</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='confirm-modal-body' className='modal-body'>
                    <h4>Select *.acf filepath for X-Plane</h4>

                    Please select (or create) a filepath for the *.acf file that matches the aircraft in your X-Plane library that you would like simulated.

					<input id ="upload-file-input" type="file" style={hiddenStyle} />
					<button id="upload-flights-button"  className="btn btn-primary btn-sm float-right" onClick={() => this.triggerInput()}>
						<i className="fa fa-file"></i> Choose a new *.acf File
					</button>
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-primary' data-dismiss='modal' onClick={() => this.modalClicked()}>Submit</button>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
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
