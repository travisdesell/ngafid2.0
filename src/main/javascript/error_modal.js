import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


function display_error_modal(title, message) {
    $("#error-modal-title").html(title);
    $("#error-modal-body").html(message);
    $("#error-modal").modal();
}

class ErrorModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            title : "",
            message : ""
        };
    }

    show(title, message) {
        this.state.title = title;
        this.state.message = message;
        this.setState(this.state);

        $("#error-modal").modal('show');
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

        console.log("rendering error modal with error title: '" + this.state.title + "' and message: " + this.state.message);


        return (
            <div className='modal-content'>
                <div className='modal-header'>
                    <h5 id='error-modal-title' className='modal-title'>Server Error</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='error-modal-body' className='modal-body'>
                    <h4>{this.state.title}</h4>

                    {this.state.message}
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                </div>
            </div>
        );
    }
}

var errorModal = ReactDOM.render(
    <ErrorModal />,
    document.querySelector("#error-modal-content")
);

export { errorModal };
