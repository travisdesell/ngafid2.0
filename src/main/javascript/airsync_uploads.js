import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

class AirsyncUploadsCard extends React.Component {
    constructor(props) {
        super(props);

        console.log("AirSync uploads init");
        console.log(props);

        let uploads = props.uploads;
        if (uploads == undefined) uploads = [];

        this.state = {
            uploads : uploads
        };
    }

    getUploadsCard() {
        return this;
    }

    removeUpload(file) {
        console.log("does nothing");
    }


    render() {
        const hidden = this.props.hidden;
        const hiddenStyle = {
            display : "none"
        };

        return (
            <div className="card-body" hidden={hidden}>
                {
                    this.state.uploads.map((uploadInfo, index) => {
                        return (
                            <Upload uploadInfo={uploadInfo} key={uploadInfo.identifier} />
                        );
                    })
                }
                <div className="d-flex justify-content-center mt-2">
                    <div className="p-0">
                        <input id ="upload-file-input" type="file" style={hiddenStyle} />
                        <button id="upload-flights-button" className="btn btn-primary" onClick={() => this.triggerInput()}>
                            <i className="fa fa-upload"></i> Upload Flights
                        </button>
                    </div>
                </div>

            </div>
        );
    }
}

var preferencesPage = ReactDOM.render(
    <AirsyncUploadsCard numberPages={numberPages} uploads={uploads} currentPage={currentPage}/>,
   document.querySelector('#airsync-uploads-page')
)
