import 'bootstrap';

import React, { Component } from "react";
import ReactDOM from "react-dom";

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


class ConfirmModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            title : "",
            message : ""
        };
    }

    show(title, message, submitMethod) {
        this.state.title = title;
        this.state.message = message;
        this.state.submitMethod = submitMethod;
        this.setState(this.state);

        $("#confirm-modal").modal('show');
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

        console.log("rendering confirm modal with confirm title: '" + this.state.title + "' and message: " + this.state.message);

        return (
            <div className='modal-content'>
                <div className='modal-header'>
                    <h5 id='confirm-modal-title' className='modal-title'>Confirm Operation</h5>
                    <button type='button' className='close' data-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                <div id='confirm-modal-body' className='modal-body'>
                    <h4>{this.state.title}</h4>

                    {this.state.message}
                </div>

                <div className='modal-footer'>
                    <button type='button' className='btn btn-primary' data-dismiss='modal' onClick={() => this.modalClicked()}>Confirm</button>
                    <button type='button' className='btn btn-secondary' data-dismiss='modal'>Close</button>
                </div>
            </div>
        );
    }
}

var confirmModal = ReactDOM.render(
    <ConfirmModal />,
    document.querySelector("#confirm-modal-content")
);

export { confirmModal };
