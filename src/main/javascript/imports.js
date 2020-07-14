import 'bootstrap';
import React, { Component } from "react";
import ReactDOM from "react-dom";
import Dropdown from 'react-bootstrap/Dropdown'
import DropdownButton from 'react-bootstrap/DropdownButton'

import { errorModal } from "./error_modal.js";
import SignedInNavbar from "./signed_in_navbar.js";


var navbar = ReactDOM.render(
    <SignedInNavbar activePage="imports" waitingUserCount={waitingUserCount} fleetManager={fleetManager} unconfirmedTailsCount={unconfirmedTailsCount} modifyTailsAccess={modifyTailsAccess} plotMapHidden={plotMapHidden}/>,
    document.querySelector('#navbar')
);


class FlightWarning extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let warning = this.props.warning;

        const styleName = { flex : "0 0 25em" };
        let filenameClasses = "p-1 mr-1 card border-warning text-warning";
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
                <div className="p-1 card border-warning text-warning flex-fill">
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
        const styleCount = { flex : "0 0 8em" };
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
        let cardClasses = textClasses + colorClasses;

        return (
            <div className="m-1">
                <div className="d-flex flex-row">
                    <div className={textClasses + " flex-fill"} style={styleName}>{importInfo.filename}</div>
                    <div className={textClasses} style={styleTime}>{importInfo.endTime}</div>
                    <div className={textClasses + " text-success"} style={styleCount}>{importInfo.validFlights} valid</div>
                    <div className={textClasses + " text-warning"} style={styleCount}>{importInfo.warningFlights} warnings</div>
                    <div className={textClasses + " text-danger"} style={styleCount}>{importInfo.errorFlights} errors</div>
                    <div className={cardClasses} style={styleStatus}>{statusText}</div>
                    <button className={expandButtonClasses} style={styleButton} onClick={() => this.expandClicked()}><i className={expandIconClasses}></i></button>

                </div>
                <div className={expandDivClasses} hidden={!expanded}>
                    <UploadErrors expanded={expanded} uploadErrors={uploadErrors}/>
                    <FlightWarnings expanded={expanded} flightWarnings={flightWarnings}/>
                    <FlightErrors expanded={expanded} flightErrors={flightErrors}/>
                </div>
            </div>
        );

    }
}


class ImportsCard extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            imports : this.props.imports,
            page : this.props.page,
            numPages : this.props.numPages,
            buffSize : 10
        };

       this.previousPage = this.previousPage.bind(this);
       this.nextPage = this.nextPage.bind(this);
       this.repaginate = this.repaginate.bind(this);
    }

    setIndex(index){
        this.state.page = index;
        this.setState(this.state);
    }

    setSize(size){
        this.state.numPages = size;
        this.setState(this.state);
    }

    setData(data) {
        this.state.imports = data;
        this.setState(this.state);
    }

    nextPage(){
        this.state.page++;
        this.submitPagination();
        console.log(this.state.page+" pg");
    }

    previousPage(){
        this.state.page--;
        this.submitPagination();
    }

    repaginate(pag){
        console.log("Re-Paginating");
        this.state.buffSize = pag;
        this.submitPagination();
    }

    jumpPage(pg){
        if(pg < this.state.numPages && pg >= 0){
            this.state.page = pg;
            this.submitPagination();
        }
    }

    genPages(){
        var page = [];
        for(var i = 0; i<this.state.numPages; i++){
            page.push({
                value : i,
                name : "Page "+(i+1)
            });
        }
        return page;
    }

    submitPagination(){
        var submissionData = {
            index : this.state.page,
            buffSize : this.state.buffSize
        }

        var importsCard = this;

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

                //get page data
                importsCard.setData(response.data);
                importsCard.setIndex(response.index);
                importsCard.setSize(response.sizeAll);
            },
            error : function(jqXHR, textStatus, errorThrown) {
                errorModal.show("Error Loading Flights", errorThrown);
            },
            async: true
        });
    }

    render() {
        const hidden = this.props.hidden;
        const hiddenStyle = {
            display : "none"
        };

        let imports = [];
        let pages = this.genPages();

        if (typeof this.state.imports != 'undefined') {
            imports = this.state.imports;
        }

		let pageStatus = "Page "+(this.state.page + 1)+" of "+(this.state.numPages);
		let noImports = false;
		if(this.state.numPages == 0){
			pageStatus = "No imports yet!";
			noImports = true;
		}

        var begin = this.state.page == 0;
        var end = this.state.page == this.state.numPages-1 || noImports;

        var prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage}>Previous Page</button>
        var next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage}>Next Page</button>

        if(begin) {
            prev = <button className="btn btn-primary btn-sm" type="button" onClick={this.previousPage} disabled>Previous Page</button>
        }
        if(end){
            next = <button className="btn btn-primary btn-sm" type="button" onClick={this.nextPage} disabled>Next Page</button>
        }

        return (
            <div className="card-body" hidden={hidden}>
                <div className="card mb-1 m-1" style={{background : "rgba(248,259,250,0.8)"}}>
                    <div className="card mb-1 m-1 border-secondary">
                        <div className="p-2">
                            <button className="btn btn-sm btn-info pr-2" disabled>{pageStatus}</button>
                            <div className="btn-group mr-1 pl-1" role="group" aria-label="First group">
                                <DropdownButton className="pr-1" id="dropdown-item-button" title={this.state.buffSize + " uploads per page"} size="sm" disabled={noImports}>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(10)}>10 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(15)}>15 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(25)}>25 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(50)}>50 uploads per page</Dropdown.Item>
                                    <Dropdown.Item as="button" onClick={() => this.repaginate(100)}>100 uploads per page</Dropdown.Item>
                                </DropdownButton>
                                <Dropdown className="pr-1">
                                    <Dropdown.Toggle variant="primary" id="dropdown-basic" size="sm" disabled={noImports}>
                                        {"Page " + (this.state.page + 1)}
                                    </Dropdown.Toggle>
                                    <Dropdown.Menu  style={{ maxHeight: "256px", overflowY: 'scroll' }}>
                                        {
                                            pages.map((pages, index) => {
                                                return (
                                                    <Dropdown.Item key={index} as="button" onClick={() => this.jumpPage(pages.value)}>{pages.name}</Dropdown.Item>
                                                );
                                            })
                                        }
                                    </Dropdown.Menu>
                                </Dropdown>
                                {prev}
                                {next}
                            </div>
                        </div>
                    </div>
                    {
                        imports.map((importInfo, index) => {
                            return (
                                <Import importInfo={importInfo} key={importInfo.identifier} />
                            );
                        })
                    }
                    <div className="card mb-1 m-1 border-secondary" hidden={noImports}>
                        <div className="p-2">
                            <button className="btn btn-sm btn-info pr-2" disabled>{pageStatus}</button>
                            <div className="btn-group mr-2 pl-1" role="group" aria-label="First group">
                                {prev}
                                {next}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}


var importsCard = ReactDOM.render(
    <ImportsCard imports={imports} numPages={numPages} page={index} />,
    document.querySelector('#imports-card')
);
