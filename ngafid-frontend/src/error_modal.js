import { Modal } from 'bootstrap';

import React, {createRef} from 'react';
import { createRoot } from 'react-dom/client';


const errorModalRef = createRef();


class ErrorModal extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            title: "",
            message: ""
        };
    }

    componentDidMount() {
        //Initialize/retrieve the Bootstrap modal instance
        const modalElement = document.getElementById('error-modal');
        this.bsModal = Modal.getOrCreateInstance(modalElement);
    }

    show(title, message) {
        const t = title != null ? String(title) : "";
        const m = message != null ? String(message) : "";
        this.setState(
            { title: t, message: m },
            () => {
                // Don't open modal when both title and message are empty (avoids empty error popup)
                if (t.trim() || m.trim())
                    this.bsModal.show();
            }
        );
    }

    render() {
        const { title, message } = this.state;

        return (
            <div className='modal-content'>
                {/* Error Modal Header */}
                <div className='modal-header'>
                    {/* Main Title */}
                    <h5 id='error-modal-title' className='modal-title'>
                        Server Error
                    </h5>

                    {/* Top Close Button */}
                    <button type='button' className='close' data-bs-dismiss='modal' aria-label='Close'>
                        <span aria-hidden='true'>&times;</span>
                    </button>
                </div>

                {/* Error Modal Body */}
                <div id='error-modal-body' className='modal-body' style={{whiteSpace:"pre-line"}}>
                    {/* Error Title */}
                    <h4>
                        {title}
                    </h4>

                    {/* Error Message */}
                    {message}
                </div>

                {/* Error Modal Footer */}
                <div className='modal-footer'>
                    {/* Footer Close Button */}
                    <button type='button' className='btn btn-secondary' data-bs-dismiss='modal'>
                        Close
                    </button>
                </div>
            </div>
        );
    }
}


const container = document.querySelector("#error-modal-content");
const root = createRoot(container);
root.render(<ErrorModal ref={errorModalRef}/>);


export function showErrorModal(title, message) {
    if (!errorModalRef.current) {
        console.error("Error Modal reference is not set. Cannot show error modal.");
        return;
    }
    errorModalRef.current.show(title, message);
}