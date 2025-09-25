
import 'bootstrap';

import React from "react";
import { createRoot } from 'react-dom/client';

import $ from 'jquery';
window.jQuery = $;
window.$ = $;


const helpModalRef = React.createRef();


class HelpModal extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            title : "",
            message : ""
        };
    }

    show(title, message, closeMethod) {
        this.setState({
            title: title,
            message: message,
            closeMethod: closeMethod
        }, () => {
            $("#help-modal").modal('show');
        });
    }

    render() {

        console.log(
            "Rendering help modal with title: '", this.state.title,
            "' and message: ", this.state.message
        );

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

const container = document.querySelector("#help-modal-content");
const root = createRoot(container);
root.render(<HelpModal ref={helpModalRef}/>);


export function showHelpModal(title, message, closeMethod) {

    console.log("Showing help modal with title: '", title, "' and message: ", message);

    helpModalRef.current.show(title, message, closeMethod);
    
}