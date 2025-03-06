import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class ErrorModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            title : "",
            message : ""
        };
    }

    show(title, message) {
        this.state.title = String(title);
        this.state.message = String(message);
        this.setState(this.state);

        $("#error-modal").modal('show');
    }

    render() {

        console.log(`Rendering Error Modal with error title: '${this.state.title}' and message: '${this.state.message}'`);

        return (
            <div className='modal-content position-absolute top-100 start-50'>
                <div className='modal-header'>
                    <h5 id='error-modal-title' className='modal-title'>Server Error</h5>
                    <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='error-modal-body' className='modal-body' style={{whiteSpace:"pre-line"}}>
                    <h4>{this.state.title}</h4>

                    {this.state.message}
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>Close</button>
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
