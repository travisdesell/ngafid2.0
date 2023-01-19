import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";

class AirSyncUpload extends React.Component {
    constructor(props) {
        super(props);
    }

    componentDidMount() {
        //console.log("upload did mount for filename: '" + this.props.uploadInfo.filename + "'");
    }

    render() {
        let uploadInfo = this.props.uploadInfo;

        let progressSize = uploadInfo.progressSize;
        let totalSize = uploadInfo.totalSize;

        if (progressSize == undefined) progressSize = uploadInfo.bytes_uploaded;
        if (totalSize == undefined) totalSize = uploadInfo.size_bytes;

        const width = ((progressSize / totalSize) * 100).toFixed(2);
        const sizeText = (uploadInfo.sizeBytes / 1000) + " K";
        const progressSizeStyle = {
            width : width + "%",
            height : "24px",
            textAlign : "left",
            whiteSpace : "nowrap"
        };

        const fixedFlexStyle1 = {
            flex : "0 0 15em"
        };

        const fixedFlexStyle2 = {
            //flex : "0 0 75em",
            height : "34px",
            padding : "4 0 4 0"
        };

        const fixedFlexStyle3 = {
            flex : "0 0 18em"
        };


        let statusText = "";

        let progressBarClasses = "progress-bar";
        let statusClasses = "p-1 pl-2 pr-2 ml-1 card bg-light";
        let status = uploadInfo.status;
        if (status == "HASHING") {
            statusText = "Hashing";
            progressBarClasses += " bg-warning";
            statusClasses += " border-warning text-warning";
        } else if (status == "UPLOADED") {
            statusText = "Uploaded";
            progressBarClasses += " bg-primary";
            statusClasses += " border-primary text-primary";
        } else if (status == "UPLOADING") {
            statusText = "Uploading";
        } else if (status == "UPLOAD INCOMPLETE") {
            statusText = "Upload Incomplete";
            progressBarClasses += " bg-warning";
            statusClasses += " border-warning text-warning";
        } else if (status == "ERROR") {
            statusText = "Import Failed";
            progressBarClasses += " bg-danger";
            statusClasses += " border-danger text-danger";
        } else if (status == "IMPORTED") {
            if (uploadInfo.errorFlights == 0 && uploadInfo.warningFlights == 0) {
                statusText = "Imported";
                progressBarClasses += " bg-success";
                statusClasses += " border-success text-success";

            } else if (uploadInfo.errorFlights != 0 && uploadInfo.errorFlights != 0) {
                statusText = "Imported With Errors and Warnings";
                progressBarClasses += " bg-danger";
                statusClasses += " border-danger text-danger ";

            } else if (uploadInfo.errorFlights != 0) {
                statusText = "Imported With Errors";
                progressBarClasses += " bg-danger";
                statusClasses += " border-danger text-danger ";

            } else if (uploadInfo.warningFlights != 0) {
                statusText = "Imported With Warnings";
                progressBarClasses += " bg-warning";
                statusClasses += " border-warning text-warning ";
            }
        }

        return (
            <div className="m-1">
                <div className="d-flex flex-row">
                    <div className="p-1 mr-1 card border-light bg-light" style={fixedFlexStyle1}>{uploadInfo.identifier}</div>
                    <div className="p-1 mr-1 card border-light bg-light" style={fixedFlexStyle1}>AirSync Ref #: {uploadInfo.airsyncId}</div>
                    <div className="p-1 flex-fill card progress" style={fixedFlexStyle2}>
                        <div className={progressBarClasses} role="progressbar" style={progressSizeStyle} aria-valuenow={width} aria-valuemin="0" aria-valuemax="100">{sizeText}</div>
                    </div>
                    <div className={statusClasses} style={fixedFlexStyle3}>{statusText}</div>
                </div>
            </div>
        );

    }
}

class AirSyncUploadsCard extends React.Component {
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
                            <AirSyncUpload uploadInfo={uploadInfo} key={uploadInfo.identifier} />
                        );
                    })
                }
                <div className="d-flex justify-content-center mt-2">
                    <div className="p-0">
                        <button id="upload-flights-button" className="btn btn-primary" onClick={() => this.triggerInput()}>
                            <i className="fa fa-refresh"></i> Sync with AirSync Server Now
                        </button>
                    </div>
                </div>

            </div>
        );
    }
}

var preferencesPage = ReactDOM.render(
    <AirSyncUploadsCard numberPages={numberPages} uploads={uploads} currentPage={currentPage}/>,
   document.querySelector('#airsync-uploads-page')
)
