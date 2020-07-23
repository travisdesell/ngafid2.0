import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class SelectAircraftModal extends React.Component {
    constructor(props) {
        super();
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

        let validationMessageStyle = {
            padding : '7 0 7 0',
            margin : '0',
            display: 'block',
            textAlign: 'left',
            color: 'red'
        };

		console.log("rendering select aircraft (xplane) modal");

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
                </div>
				<select class="form-control">
				  <option>/Aircraft/A320</option>
				</select>

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
