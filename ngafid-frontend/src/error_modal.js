import 'bootstrap';

import { Modal } from 'bootstrap';          // ← programmatic API (no jQuery plug‑in)
import React from 'react';
import ReactDOM from 'react-dom';



class ErrorModal extends React.Component {

    constructor(props) {

        super(props);

        this.state = {
            title : "",
            message : ""
        };

        this.bootstrapModal = null;

    }

    componentDidMount() {

        //Initialize/retrieve the Bootstrap modal instance
        const modalElement = document.getElementById('error-modal');
        this.bsModal = Modal.getOrCreateInstance(modalElement);

    }

    show(title, message) {

        this.setState(
            {
                title: String(title),
                message: String(message),
            },
            () => this.bsModal.show()      //<-- Show the modal after state has updated
        );

    }

    render() {

        const { title, message } = this.state;

        console.log(`Rendering Error Modal with error title: '${title}' and message: '${message}'`);

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
                    {this.state.message}

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

const errorModal = ReactDOM.render(
    <ErrorModal/>,
    document.querySelector("#error-modal-content")
);

export { errorModal };
