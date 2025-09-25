import { Modal } from 'bootstrap';

import React, {createRef} from "react";
import { createRoot } from 'react-dom/client';


const confirmModalRef = createRef();


class ConfirmModal extends React.Component {

    constructor(props) {

        super(props);

        this.state = {
            title : "",
            message : "",
            submitMethod : null
        };

    }

    componentDidMount() {

        //Initialize/retrieve the Bootstrap modal instance
        const modalElement = document.getElementById('confirm-modal');
        this.bsModal = Modal.getOrCreateInstance(modalElement);

    }

    show(title, message, submitMethod=null) {

        this.setState(
            {
                title: String(title),
                message: String(message),
                submitMethod: submitMethod
            },
            () => this.bsModal.show()      //<-- Show the modal after state has updated
        );

    }

    modalClicked() {

        console.log("Confirm Modal submit clicked!");
        
        //Submit method exists, call it
        if (this.state.submitMethod != null)
            this.state.submitMethod();

    }

    render() {

        const { title, message, submitMethod } = this.state;

        console.log(`Rendering Confirm Modal with confirm title: '${title}' and message: '${message}'`);

        return (
            <div className='modal-content'>

                {/* Confirm Modal Header */}
                <div className='modal-header'>

                    {/* Main Title */}
                    <h5 id='confirm-modal-title' className='modal-title'>
                        Confirm Operation
                    </h5>

                    {/* Top Close Button */}
                    <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>

                </div>

                {/* Confirm Modal Body */}
                <div id='confirm-modal-body' className='modal-body'>

                    {/* Confirm Modal Title */}
                    <h4>
                        {title}
                    </h4>

                    {/* Confirm Modal Message */}
                    {message}

                </div>

                {/* Confirm Modal Footer */}
                <div className='modal-footer'>

                    {/* Footer Confirm Button (Only visible with a defined submission method) */}
                    {
                        submitMethod
                        &&
                        <button type='button' className='btn btn-primary' data-bs-dismiss='modal' onClick={() => this.modalClicked()}>
                            Confirm
                        </button>
                    }

                    {/* Footer Close Button */}
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>
                        Close
                    </button>

                </div>
            </div>
        );
    }
}

const container = document.querySelector("#confirm-modal-content");
const root = createRoot(container);
root.render(<ConfirmModal ref={confirmModalRef}/>);


export function showConfirmModal(title, message, submitMethod=null) {

    console.log(`Showing Confirm Modal with title: '${title}' and message: '${message}'`);

    //Show the modal with the given title, message, and optional submit method
    confirmModalRef.current.show(title, message, submitMethod);
    
}