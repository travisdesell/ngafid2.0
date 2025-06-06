
import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class HelpModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            title : "",
            message : ""
        };
    }

    show(title, message, closeMethod) {
        this.state.title = title;
        this.state.message = message;
        this.state.closeMethod = closeMethod;
        this.setState(this.state);

        $("#help-modal").modal('show');
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

        console.log("rendering help modal with title: '" + this.state.title + "' and message: " + this.state.message);

        return (
            <div className='modal-content'>
                <div className='modal-header'>
                    <h5 id='help-modal-title' className='modal-title'>Help</h5>
                    <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='help-modal-body' className='modal-body'>
                    <h4>{this.state.title}</h4>

                    {this.state.message}
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal' onClick={() => this.state.closeMethod()}>Close</button>
                </div>
            </div>
        );
    }
}

var helpModal = ReactDOM.render(
    <HelpModal />,
    document.querySelector("#help-modal-content")
);

export { helpModal };
