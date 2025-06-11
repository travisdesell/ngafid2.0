import 'bootstrap';
import {Paginator} from "./paginator_component.js";
import { showConfirmModal } from "./confirm_modal.js";
import { showErrorModal } from './error_modal.js';
import SignedInNavbar from "./signed_in_navbar.js";
import React from "react";
import ReactDOM from "react-dom";

class AirSyncUpload extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        const uploadInfo = this.props.uploadInfo;

        let progressSize = uploadInfo.progressSize;
        let totalSize = uploadInfo.totalSize;

        if (progressSize == undefined)
            progressSize = uploadInfo.bytes_uploaded;
        
        if (totalSize == undefined)
            totalSize = uploadInfo.size_bytes;

        const fixedFlexStyle1 = {
            flex: "0 0 15em"
        };

        const fixedFlexStyle2 = {
            //flex : "0 0 75em",
            height: "34px",
            padding: "4 0 4 0"
        };

        const fixedFlexStyle3 = {
            flex: "0 0 18em"
        };


        let statusText = "";


        let statusClasses = "p-1 pl-2 pr-2 ml-1 card bg-light";
        const status = uploadInfo.status;
        if (status == "HASHING") {
            statusText = "Hashing";
            statusClasses += " border-warning text-warning";
        } else if (status == "UPLOADED") {
            statusText = "Uploaded";
            statusClasses += " border-primary text-primary";
        } else if (status == "UPLOADING") {
            statusText = "Uploading";
        } else if (status == "UPLOAD INCOMPLETE") {
            statusText = "Upload Incomplete";
            statusClasses += " border-warning text-warning";
        } else if (status == "ERROR") {
            statusText = "Import Failed";
            statusClasses += " border-danger text-danger";
        } else if (status == "IMPORTED") {

            if (uploadInfo.errorFlights == 0 && uploadInfo.warningFlights == 0) {
                statusText = "All Flights Imported";
                statusClasses += " border-success text-success";

            } else if (uploadInfo.errorFlights != 0 && uploadInfo.warningFlights != 0) {
                statusText = "Imported With Some Errors and Warnings";
                statusClasses += " border-danger text-danger ";

            } else if (uploadInfo.errorFlights != 0) {
                statusText = "Imported With Some Errors";
                statusClasses += " border-danger text-danger ";

            } else if (uploadInfo.warningFlights != 0) {
                statusText = "Imported With Some Warnings";
                statusClasses += " border-warning text-warning ";
            }
            
        }

        statusClasses += " mr-1 bg-light flex-fill";

        return (
            <div className="m-1">
                <div className="d-flex flex-row">
                    <div className="p-1 mr-1 card border-light bg-light"
                         style={fixedFlexStyle2}>{uploadInfo.identifier}</div>
                    <div className="p-1 mr-1 card border-light bg-light"
                         style={fixedFlexStyle1}>Tail: {uploadInfo.tail}</div>
                    <div className="p-1 mr-1 card border-light bg-light"
                         style={fixedFlexStyle1}>{uploadInfo.groupString}</div>
                    <div className="p-1 mr-1 card border-light bg-light"
                         style={fixedFlexStyle1}>{uploadInfo.validFlights} valid flights.
                    </div>
                    <div className="p-1 mr-1 card border-light bg-light"
                         style={fixedFlexStyle1}>{uploadInfo.warningFlights} warning flights.
                    </div>
                    <div className="p-1 mr-1 card border-light bg-light"
                         style={fixedFlexStyle1}>{uploadInfo.errorFlights} error flights.
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
            uploads: uploads,
            numberPages: numberPages,
            currentPage: currentPage,
            pageSize: 10,
            lastUpdateTime: props.lastUpdateTime,
        };
    }

    submitFilter() {
    
        const submissionData = {
            currentPage: this.state.currentPage,
            pageSize: this.state.pageSize,
        };

        console.log(submissionData);

        $.ajax({
            type: 'GET',
            url: '/api/airsync/uploads',
            data: submissionData,
            async: true,
            success: (response) => {

                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("Displaying error modal!");
                    showErrorModal(response.errorTitle, response.errorMessage);
                    return false;
                }

                this.setState({
                    uploads: response.page,
                    numberPages: response.numberPages
                });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Loading Uploads", errorThrown);
            },
        });
    }

    manualSync() {
        console.log("Manual AirSync update requested!");
        showConfirmModal(
            "Confirm Operation",
            "Confirm that you would like to update with the AirSync servers. This operation can take a lot of time, especially if there are a lot of new flights! You will recieve and email once the process is complete.",
            () => { this.requestUpdate(); }
        );
    }

    requestUpdate() {

        $.ajax({
            type: 'PATCH',
            url: '/api/airsync/update',
            dataType: 'json',
            async: true,
            success: () => {
                this.setState({ lastUpdateTime: "Pending" });
            },
            error: (jqXHR, textStatus, errorThrown) => {
                showErrorModal("Error Updating:", errorThrown);
            },
        });
    }


    render() {

        const hidden = this.props.hidden;

        const updateTimeInfo = `Last Sync Time: ${  this.state.lastUpdateTime}`;

        return (
            <div>
                <SignedInNavbar activePage="uploads" waitingUserCount={waitingUserCount} fleetManager={fleetManager}
                                unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess}
                                plotMapHidden={plotMapHidden}/>

                <div className="card-body" hidden={hidden}>
                    <div className="card mb-1 border-secondary">
                        <div className="p-2">
                            <button id="upload-airsync-button" className="btn btn-info btn-sm float-left" disabled>
                                <i className="fa fa-cloud-download"></i> {updateTimeInfo}
                            </button>
                            <button id="upload-airsync-button" className="btn btn-primary btn-sm float-right"
                                    onClick={() => this.manualSync()}>
                                <i className="fa fa-refresh"></i> Sync with AirSync Server Now
                            </button>
                        </div>
                    </div>

                    <Paginator
                        submitFilter={() => {
                            this.submitFilter();
                        }}
                        items={this.state.uploads}
                        itemName="uploads"
                        currentPage={this.state.currentPage}
                        numberPages={this.state.numberPages}
                        pageSize={this.state.pageSize}
                        updateCurrentPage={(currentPage) => {
                            this.setState({ currentPage: currentPage });
                        }}
                        updateItemsPerPage={(pageSize) => {
                            this.setState({ pageSize: pageSize });
                        }}
                    />

                    {
                        this.state.uploads.map((uploadInfo) => {
                            return (
                                <AirSyncUpload uploadInfo={uploadInfo} key={uploadInfo.identifier}/>
                            );
                        })
                    }


                    <Paginator
                        submitFilter={() => {
                            this.submitFilter();
                        }}
                        items={this.state.uploads}
                        itemName="uploads"
                        currentPage={this.state.currentPage}
                        numberPages={this.state.numberPages}
                        pageSize={this.state.pageSize}
                        updateCurrentPage={(currentPage) => {
                            this.setState({ currentPage: currentPage });
                        }}
                        updateItemsPerPage={(pageSize) => {
                            this.setState({ pageSize: pageSize });
                        }}
                    />

                </div>
            </div>
        );
    }
}

const container = document.querySelector("#airsync-uploads-page");
const root = ReactDOM.createRoot(container);
root.render(
    <AirSyncUploadsCard
        numberPages={numberPages}
        uploads={uploads}
        lastUpdateTime={lastUpdateTime}
        currentPage={currentPage}
    />
);