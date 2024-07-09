import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown'
import DropdownButton from 'react-bootstrap/DropdownButton'

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";

import { Paginator } from "./paginator_component.js";

class FlightWarning extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let warning = this.props.warning;

        const styleName = { flex : "0 0 25em", color: "#eb9515" };
        let filenameClasses = "p-1 mr-1 card border-warning";
        let filenameText = warning.filename;
        if (warning.sameFilename) {
            filenameClasses = "p-1 mr-1";
            filenameText = "";
        }

        return (
            <div className="d-flex flex-row p-0 mt-1">
                <div className={filenameClasses} style={styleName} >
                    {filenameText}
                </div>
                <div className="p-1 card border-warning flex-fill" style={{color: "#eb9515"}}>
                    {warning.message}
                </div>
            </div>
        );
    }
}

class FlightWarnings extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let flightWarnings = this.props.flightWarnings;

        return (
            <div className="m-0">
                {
                    flightWarnings.map((warning, index) => {
                        return (
                            <FlightWarning warning={warning} key={warning.id} />
                        );
                    })
                }
            </div>
        );
    }
}

class FlightError extends React.Component {
    constructor(props) {
        super(props);
    }


    render() {
        let error = this.props.error;

        const styleName = { flex : "0 0 25em" };
        let filenameClasses = "p-1 mr-1 card border-danger text-danger";
        let filenameText = error.filename;
        if (error.sameFilename) {
            filenameClasses = "p-1 mr-1";
            filenameText = "";
        }

        return (
            <div className="d-flex flex-row p-0 mt-1">
                <div className={filenameClasses} style={styleName} >
                    {filenameText}
                </div>
                <div className="p-1 card border-danger text-danger flex-fill">
                    {error.message}
                </div>
            </div>
        );
    }
}

class FlightErrors extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let flightErrors = this.props.flightErrors;

        return (
            <div className="m-0">
                {
                    flightErrors.map((error, index) => {
                        return (
                            <FlightError error={error} key={error.id} />
                        );
                    })
                }
            </div>
        );
    }
}

class UploadError extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div className="d-flex flex-row card p-1 border-danger text-danger">
                {this.props.error.message}
            </div>
        );
    }
}

class UploadErrors extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let uploadErrors = this.props.uploadErrors;

        return (
            <div className="m-0 mt-1">
                {
                    uploadErrors.map((error, index) => {
                        return (
                            <UploadError error={error} key={error.id} />
                        );
                    })
                }
            </div>
        );
    }
}

class Import extends React.Component {
    constructor(props) {
        super(props);

        let uploadErrors = props.uploadErrors;
        if (uploadErrors == undefined) uploadErrors = [];

        let flightWarnings = props.flightWarnings;
        if (flightWarnings == undefined) flightWarnings = [];

        let flightErrors = props.flightErrors;
        if (flightErrors == undefined) flightErrors = [];


        this.state = {
            expanded : false,
            loaded : false,
            uploadErrors : uploadErrors,
            flightWarnings: flightWarnings,
            flightErrors: flightErrors
        };
    }

    componentDidMount() {
        //console.log("import did mount for filename: '" + this.props.importInfo.filename + "'");
    }

    expandClicked() {
        var thisImport = this;

        var submissionData = {
            uploadId : this.props.importInfo.id
        };   

        if (this.state.loaded) {
            console.log("not fetching import information from the server, already loaded.");
            thisImport.state.expanded = !thisImport.state.expanded;
            thisImport.setState(thisImport.state);
        } else {
            console.log("fetching import information from the server.");

            $.ajax({
                type: 'POST',
                url: '/protected/upload_details',
                data : submissionData,
                dataType : 'json',
                success : function(response) {
                    console.log("received response: ");
                    console.log(response);

                    if (response.errorTitle !== undefined) {
                        errorModal.show(response.errorTitle, response.errorMessage);
                    } else {
                        thisImport.state.loaded = true;
                        thisImport.state.expanded = !thisImport.state.expanded;
                        console.log("expand clicked, now:" + thisImport.state.expanded);
                        thisImport.state.uploadErrors = response.uploadErrors;
                        thisImport.state.flightWarnings = response.flightWarnings;
                        thisImport.state.flightErrors = response.flightErrors;

                        thisImport.setState(thisImport.state);
                    }

                },   
                error : function(jqXHR, textStatus, errorThrown) {
                    errorModal.show("Error Loading Uploads", errorThrown);
                },   
                async: true 
            });  
        }

    }

    render() {
        let expanded = this.state.expanded;
        let uploadErrors = this.state.uploadErrors;
        let flightWarnings = this.state.flightWarnings;
        let flightErrors = this.state.flightErrors;

        for (var i = 1; i < flightWarnings.length; i++) {
            if (flightWarnings[i-1].filename == flightWarnings[i].filename) {
                flightWarnings[i].sameFilename = true;
            } else {
                flightWarnings[i].sameFilename = false;
            }
        }

        for (var i = 1; i < flightErrors.length; i++) {
            if (flightErrors[i-1].filename == flightErrors[i].filename) {
                flightErrors[i].sameFilename = true;
            } else {
                flightErrors[i].sameFilename = false;
            }
        }

        let importInfo = this.props.importInfo;

        /*
        console.log("rendering import for filename: '" + importInfo.filename + "'");
        console.log(this.props);
        console.log(this.state);
        */

        let progressSize = importInfo.progressSize;
        let totalSize = importInfo.totalSize;

        if (progressSize == undefined) progressSize = importInfo.bytesUploaded;
        if (totalSize == undefined) totalSize = importInfo.sizeBytes;

        const width = ((progressSize / totalSize) * 100).toFixed(2);
        const sizeText = (progressSize/1000).toFixed(2).toLocaleString() + "/" + (totalSize/1000).toFixed(2).toLocaleString()  + " kB (" + width + "%)";
        const progressSizeStyle = {
            width : width + "%",
            height : "24px",
            textAlign : "left",
            whiteSpace : "nowrap"
        };

        const styleName = { };
        const styleTime = { flex : "0 0 11em" };
        const styleCount = { marginRight: "5px", borderRadius: "8px", color: "white", textAlign: "center", alignContent: "center" };
        const styleStatus = { flex : "0 0 10em" };
        const styleButton = { };

        let statusText = "";
        let expandButtonClasses = "p-1 expand-import-button btn btn-outline-secondary";
        let expandIconClasses = "fa ";

        let expandDivClasses = "";
        if (expanded) {
            expandIconClasses += "fa-angle-double-up";
            expandDivClasses = "m-0 mt-1 mb-4";
        } else {
            expandIconClasses += "fa-angle-double-down";
            expandDivClasses = "m-0";
        }

        let progressBarClasses = "progress-bar";
        let status = importInfo.status;
        let colorClasses = "";

        if (status == "HASHING") {
            statusText = "Hashing";
            progressBarClasses += " bg-warning";
            colorClasses += " border-warning text-warning";
        } else if (status == "IMPORTED") {
            if (importInfo.errorFlights == 0 && importInfo.warningFlights == 0) {
                statusText = "Imported";
                progressBarClasses += " bg-success";
                colorClasses += " border-success text-success";

            } else if (importInfo.errorFlights != 0 && importInfo.warningFlights != 0) {
                statusText = "Imported With Errors and Warnings";
                progressBarClasses += " bg-danger";
                colorClasses += " border-danger text-danger ";

            } else if (importInfo.errorFlights != 0) {
                statusText = "Imported With Errors";
                progressBarClasses += " bg-danger";
                colorClasses += " border-danger text-danger ";

            } else if (importInfo.warningFlights != 0) {
                statusText = "Imported With Warnings";
                progressBarClasses += " bg-warning";
                colorClasses += " border-warning text-warning ";
            }

        } else if (status == "UPLOADED") {
            statusText = "Not Yet Imported";
            progressBarClasses += " bg-info";
            colorClasses += " border-info text-info";

        } else if (status == "UPLOADING") {
            statusText = "Uploading";

        } else if (status == "UPLOAD INCOMPLETE") {
            statusText = "Upload Incomplete";
            progressBarClasses += " bg-warning";
            colorClasses += " border-warning text-warning";

        } else if (status == "ERROR") {
            statusText = "Import Failed";
            progressBarClasses += " bg-danger";
            colorClasses += " border-danger text-danger";
        }

        let textClasses = "p-1 mr-1 card bg-light";
        let cardClasses = (textClasses + colorClasses);

        return (
            <div className="m-1">
                <div className="d-flex justify-content-between align-items-start" style={{ ...styleName, backgroundColor: 'white', padding: '10px', borderRadius: "10px" }}>
        
                    {/* LEFT ELEMENTS */}
                    <div className="d-flex justify-content-start flex-wrap" style={{ flexWrap: "wrap", minWidth:"30%" }}>
                        <div className={textClasses} style={{ ...styleTime, minWidth:"40%"}}>
                            {importInfo.filename}
                        </div>
                        <div className={textClasses} style={{...styleTime, minWidth:"40%"}}>
                            {importInfo.endTime}
                        </div>
                    </div>
        
                    {/* RIGHT ELEMENTS */}
                    <div className="d-flex justify-content-end flex-wrap" style={{ flexFlow:"row wrap", minWidth: "70%" }}>
                        <div
                            style={{ ...styleCount, flex: "0 0 6em", backgroundColor: "gray" }}
                        >
                            <i className="fa fa-upload" aria-hidden="true" /> Total: {importInfo.validFlights}
                        </div>
                        <div
                            style={{ ...styleCount, flex: "0 0 6em", backgroundColor: "green" }}
                        >
                            <i className="fa fa-check" aria-hidden="true" /> Valid: {importInfo.validFlights}
                        </div>
                        <div
                            style={{ ...styleCount, flex: "0 0 8em", backgroundColor: "#eb9515" }}
                        >
                            <i className="fa fa-exclamation-triangle" aria-hidden="true" /> Warnings: {importInfo.warningFlights}
                        </div>
                        <div
                            style={{ ...styleCount, flex: "0 0 6.5em", backgroundColor: "#dc3545" }}
                        >
                            <i className="fa fa-exclamation-circle" aria-hidden="true" /> Errors: {importInfo.errorFlights}
                        </div>
                        <div
                            className={cardClasses}
                            style={{ ...styleStatus, flex: "0 0 18em", marginLeft: "10px", marginRight: "10px" }}
                        >
                            {statusText}
                        </div>
                        <button className={expandButtonClasses + "d-flex justify-content-end flex-wrap"} style={{...styleButton, marginLeft: "10px"}} onClick={() => this.expandClicked()}>
                            <i className={expandIconClasses}/>
                        </button>
                    </div>
                </div>
                <div className={expandDivClasses} hidden={!expanded}>
                    <UploadErrors expanded={expanded} uploadErrors={uploadErrors} />
                    <FlightWarnings expanded={expanded} flightWarnings={flightWarnings} />
                    <FlightErrors expanded={expanded} flightErrors={flightErrors} />
                </div>
            </div>
        );
        

    }
}


class ImportsPage extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            imports : this.props.imports,

            //needed for paginator
            currentPage : this.props.currentPage,
            numberPages : this.props.numberPages, //this will be set globally in the javascript
            pageSize : 10
        };
    }

    submitFilter() {
        var submissionData = {
            currentPage : this.state.currentPage,
            pageSize : this.state.pageSize
        }

        var importsPage = this;

        $.ajax({
            type: 'POST',
            url: '/protected/get_imports',
            data : submissionData,
            dataType : 'json',
            success : function(response) {

                console.log(response);

                $("#loading").hide();

                if (response.errorTitle) {
                    console.log("displaying error modal!");
                    errorModal.show(response.errorTitle, response.errorMessage);
                    return false;
                }

                console.log("got response: "+response+" "+response.size);

                importsPage.setState({
                    imports : response.imports,
                    numberPages : response.numberPages
                });
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },
            async: true
        });
    }

    render() {
        return (
            <body style={{overflowY:"scroll"}}>
                <div>
                    <SignedInNavbar activePage="imports" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>

                    <div className="p-1">
                        <Paginator
                            submitFilter={() => {this.submitFilter();}}
                            items={this.state.imports}
                            itemName="uploads"
                            currentPage={this.state.currentPage}
                            numberPages={this.state.numberPages}
                            pageSize={this.state.pageSize}
                            updateCurrentPage={(currentPage) => {
                                this.state.currentPage = currentPage;
                            }}
                            updateItemsPerPage={(pageSize) => {
                                this.state.pageSize = pageSize;
                            }}
                        />

                        {
                            
                            this.state.imports.map((importInfo, index) => {
                                return (
                                    <Import importInfo={importInfo} key={importInfo.identifier} />
                                );
                            })
                            
                        }

                        <Paginator
                            submitFilter={() => {this.submitFilter();}}
                            items={this.state.imports}
                            itemName="uploads"
                            currentPage={this.state.currentPage}
                            numberPages={this.state.numberPages}
                            pageSize={this.state.pageSize}
                            updateCurrentPage={(currentPage) => {
                                this.state.currentPage = currentPage;
                            }}
                            updateItemsPerPage={(pageSize) => {
                                this.state.pageSize = pageSize;
                            }}
                        />
                    </div>
                </div>
            </body>
        );
    }
}

var importsPage = ReactDOM.render(
    <ImportsPage imports={imports} numberPages={numberPages} currentPage={currentPage} />,
    document.querySelector('#imports-page')
);
